// The phone-side language model, with no JNI in it so it can be built and run on a PC too.
// One Engine holds one model and one conversation. Not thread-safe: use it from one thread at a time.
#pragma once

#include <atomic>
#include <functional>
#include <string>
#include <vector>

struct llama_model;
struct llama_context;
struct llama_vocab;

struct ChatTurn {
    std::string role;     // "system", "user" or "assistant"
    std::string content;
};

enum EngineResult {
    ENGINE_OK = 0,
    ENGINE_BAD_HANDLE = -1,
    ENGINE_PROMPT_TOO_LONG = -2,
    ENGINE_DECODE_FAILED = -3,
    ENGINE_TEMPLATE_FAILED = -4,
    ENGINE_TOKENIZE_FAILED = -5,
    ENGINE_SINK_FAILED = -6,
    ENGINE_CANCELLED = -7,
};

struct Engine {
    llama_model *model = nullptr;
    llama_context *ctx = nullptr;
    const llama_vocab *vocab = nullptr;
    int n_ctx = 0;
    int n_batch = 0;
    // The tokens now in the model's memory, in order. The next prompt only has to read what comes
    // after the part it shares with these, which is what makes a second question fast.
    std::vector<int> cache;
    // Filled in by engine_generate, for logging.
    int last_prompt_tokens = 0;
    int last_reused_tokens = 0;
    // Set from another thread to stop the current engine_generate, even while it is still reading the prompt.
    std::atomic<bool> cancel{false};
};

// Returns null if the file can't be loaded as a model or the memory can't be had.
Engine *engine_load(const char *model_path, int n_ctx, int n_threads, int n_threads_batch);

void engine_free(Engine *engine);

// Safe to call from any thread. The running engine_generate returns ENGINE_CANCELLED (or the
// token count so far, if it had already started answering).
void engine_cancel(Engine *engine);

// Forget everything remembered, so the next prompt is read from the start.
void engine_reset(Engine *engine);

// Reads [turns] (the model's own chat format is applied), then writes the reply, calling [sink]
// with each piece as it comes. A piece always ends on a whole character. [sink] returns false to
// stop. Returns the number of reply tokens (>= 0) or a negative EngineResult.
// temperature <= 0 picks the most likely word every time.
int engine_generate(Engine *engine,
                    const std::vector<ChatTurn> &turns,
                    int max_tokens,
                    float temperature,
                    const std::function<bool(const std::string &)> &sink);

// How many bytes at the start of [text] are whole UTF-8 characters (the rest is a half-finished one).
size_t utf8_whole_prefix(const std::string &text);

const char *engine_system_info();
