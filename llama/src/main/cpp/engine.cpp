#include "engine.h"

#include <algorithm>
#include <cstdint>
#include <cstdio>
#include <ctime>

#include "llama.h"

#ifdef __ANDROID__
#include <android/log.h>
#endif

namespace {

bool g_backend_ready = false;

void log_callback(enum ggml_log_level level, const char *text, void *) {
#ifdef __ANDROID__
    int prio = level == GGML_LOG_LEVEL_ERROR ? ANDROID_LOG_ERROR
             : level == GGML_LOG_LEVEL_WARN  ? ANDROID_LOG_WARN
                                             : ANDROID_LOG_DEBUG;
    __android_log_print(prio, "ResonantLlama", "%s", text);
#else
    if (level >= GGML_LOG_LEVEL_WARN) fputs(text, stderr);
#endif
}

void ensure_backend() {
    if (g_backend_ready) return;
    llama_log_set(log_callback, nullptr);
    llama_backend_init();
    g_backend_ready = true;
}

}  // namespace

size_t utf8_whole_prefix(const std::string &text) {
    size_t n = text.size();
    // Look back at most 3 bytes for the start of the last character.
    for (size_t back = 1; back <= 4 && back <= n; ++back) {
        unsigned char c = static_cast<unsigned char>(text[n - back]);
        if ((c & 0xC0) == 0x80) continue;  // a continuation byte: keep looking for its start
        size_t need = (c & 0x80) == 0x00 ? 1
                    : (c & 0xE0) == 0xC0 ? 2
                    : (c & 0xF0) == 0xE0 ? 3
                    : (c & 0xF8) == 0xF0 ? 4
                                         : 1;  // stray byte: let it through, the Kotlin side replaces it
        return back >= need ? n : n - back;
    }
    return n;
}

const char *engine_system_info() {
    ensure_backend();
    return llama_print_system_info();
}

Engine *engine_load(const char *model_path, int n_ctx, int n_threads, int n_threads_batch) {
    ensure_backend();

    llama_model_params mparams = llama_model_default_params();
    mparams.n_gpu_layers = 0;  // CPU only: Mali GPUs are slower than the CPU for this

    llama_model *model = llama_model_load_from_file(model_path, mparams);
    if (!model) return nullptr;

    llama_context_params cparams = llama_context_default_params();
    cparams.n_ctx = static_cast<uint32_t>(n_ctx);
    cparams.n_batch = 512;
    cparams.n_ubatch = 512;
    cparams.n_threads = n_threads;
    cparams.n_threads_batch = n_threads_batch;

    llama_context *ctx = llama_init_from_model(model, cparams);
    if (!ctx) {
        llama_model_free(model);
        return nullptr;
    }

    auto *engine = new Engine();
    engine->model = model;
    engine->ctx = ctx;
    engine->vocab = llama_model_get_vocab(model);
    engine->n_ctx = static_cast<int>(llama_n_ctx(ctx));
    engine->n_batch = static_cast<int>(llama_n_batch(ctx));
    llama_set_abort_callback(ctx, [](void *data) { return static_cast<Engine *>(data)->cancel.load(); }, engine);
    return engine;
}

void engine_free(Engine *engine) {
    if (!engine) return;
    if (engine->ctx) llama_free(engine->ctx);
    if (engine->model) llama_model_free(engine->model);
    delete engine;
}

void engine_cancel(Engine *engine) {
    if (engine) engine->cancel.store(true);
}

void engine_reset(Engine *engine) {
    if (!engine || !engine->ctx) return;
    llama_memory_clear(llama_get_memory(engine->ctx), true);
    engine->cache.clear();
}

static bool format_prompt(Engine *engine, const std::vector<ChatTurn> &turns, std::string &out) {
    std::vector<llama_chat_message> msgs;
    msgs.reserve(turns.size());
    size_t chars = 0;
    for (const auto &t : turns) {
        msgs.push_back({t.role.c_str(), t.content.c_str()});
        chars += t.role.size() + t.content.size();
    }
    // A model file with no template of its own gets ChatML, which most small models understand.
    const char *tmpl = llama_model_chat_template(engine->model, nullptr);
    if (!tmpl) tmpl = "chatml";

    std::vector<char> buf(chars * 2 + 512);
    int n = llama_chat_apply_template(tmpl, msgs.data(), msgs.size(), true, buf.data(), static_cast<int32_t>(buf.size()));
    if (n > static_cast<int>(buf.size())) {
        buf.resize(static_cast<size_t>(n));
        n = llama_chat_apply_template(tmpl, msgs.data(), msgs.size(), true, buf.data(), static_cast<int32_t>(buf.size()));
    }
    if (n < 0) return false;
    out.assign(buf.data(), static_cast<size_t>(n));
    return true;
}

static bool tokenize(Engine *engine, const std::string &text, std::vector<llama_token> &out) {
    int n = -llama_tokenize(engine->vocab, text.c_str(), static_cast<int32_t>(text.size()), nullptr, 0, true, true);
    if (n <= 0) return false;
    out.resize(static_cast<size_t>(n));
    int got = llama_tokenize(engine->vocab, text.c_str(), static_cast<int32_t>(text.size()), out.data(), n, true, true);
    if (got < 0) return false;
    out.resize(static_cast<size_t>(got));
    return true;
}

int engine_generate(Engine *engine,
                    const std::vector<ChatTurn> &turns,
                    int max_tokens,
                    float temperature,
                    const std::function<bool(const std::string &)> &sink) {
    if (!engine || !engine->ctx) return ENGINE_BAD_HANDLE;
    engine->cancel.store(false);

    std::string prompt;
    if (!format_prompt(engine, turns, prompt)) return ENGINE_TEMPLATE_FAILED;

    std::vector<llama_token> tokens;
    if (!tokenize(engine, prompt, tokens)) return ENGINE_TOKENIZE_FAILED;
    if (static_cast<int>(tokens.size()) + max_tokens + 8 > engine->n_ctx) return ENGINE_PROMPT_TOO_LONG;

    llama_memory_t mem = llama_get_memory(engine->ctx);

    // Keep what the model has already read: only the part after the shared start is new work.
    size_t common = 0;
    const size_t limit = std::min(engine->cache.size(), tokens.size());
    while (common < limit && engine->cache[common] == tokens[common]) ++common;
    if (common == tokens.size()) --common;  // the last token must be read again to get a next word
    if (!llama_memory_seq_rm(mem, 0, static_cast<llama_pos>(common), -1)) {
        llama_memory_clear(mem, true);
        common = 0;
    }
    engine->cache.resize(common);
    engine->last_prompt_tokens = static_cast<int>(tokens.size());
    engine->last_reused_tokens = static_cast<int>(common);

    for (size_t i = common; i < tokens.size(); i += static_cast<size_t>(engine->n_batch)) {
        int count = static_cast<int>(std::min(static_cast<size_t>(engine->n_batch), tokens.size() - i));
        llama_batch batch = llama_batch_get_one(&tokens[i], count);
        int rc = llama_decode(engine->ctx, batch);
        if (rc == 2 || engine->cancel.load()) {
            // Stopped part-way. What was read stays in memory, and the cache list only counts
            // whole chunks, so the next prompt trims back to what is really known.
            return ENGINE_CANCELLED;
        }
        if (rc != 0) {
            engine_reset(engine);
            return ENGINE_DECODE_FAILED;
        }
        engine->cache.insert(engine->cache.end(), tokens.begin() + static_cast<long>(i), tokens.begin() + static_cast<long>(i) + count);
    }

    llama_sampler *smpl = llama_sampler_chain_init(llama_sampler_chain_default_params());
    if (temperature <= 0.0f) {
        llama_sampler_chain_add(smpl, llama_sampler_init_greedy());
    } else {
        // Small models repeat themselves, so the penalty matters more here than on a big one.
        llama_sampler_chain_add(smpl, llama_sampler_init_penalties(llama_vocab_n_tokens(engine->vocab), 64, 1.1f, 0.0f, 0.0f));
        llama_sampler_chain_add(smpl, llama_sampler_init_top_k(40));
        llama_sampler_chain_add(smpl, llama_sampler_init_top_p(0.9f, 1));
        llama_sampler_chain_add(smpl, llama_sampler_init_temp(temperature));
        llama_sampler_chain_add(smpl, llama_sampler_init_dist(static_cast<uint32_t>(time(nullptr))));
    }

    int produced = 0;
    int result = ENGINE_OK;
    std::string pending;
    char piece[256];

    while (produced < max_tokens && static_cast<int>(engine->cache.size()) < engine->n_ctx - 1) {
        llama_token id = llama_sampler_sample(smpl, engine->ctx, -1);
        if (llama_vocab_is_eog(engine->vocab, id)) break;

        int n = llama_token_to_piece(engine->vocab, id, piece, sizeof(piece), 0, false);
        if (n > 0) pending.append(piece, static_cast<size_t>(n));
        ++produced;

        size_t whole = utf8_whole_prefix(pending);
        if (whole > 0) {
            bool keep_going = sink(pending.substr(0, whole));
            pending.erase(0, whole);
            if (!keep_going) break;
        }
        if (engine->cancel.load()) break;

        llama_batch batch = llama_batch_get_one(&id, 1);
        int rc = llama_decode(engine->ctx, batch);
        if (rc == 2) break;  // cancelled mid-step: this token never joined the cache
        if (rc != 0) {
            engine_reset(engine);
            result = ENGINE_DECODE_FAILED;
            break;
        }
        engine->cache.push_back(id);
    }

    llama_sampler_free(smpl);
    return result == ENGINE_OK ? produced : result;
}
