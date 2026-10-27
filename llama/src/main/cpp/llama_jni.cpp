// Thin JNI bridge to engine.cpp. Kotlin side: LlamaNative.kt.
#include <jni.h>

#include <string>
#include <vector>

#include "engine.h"

namespace {

std::string to_string(JNIEnv *env, jstring s) {
    const char *chars = env->GetStringUTFChars(s, nullptr);
    std::string out(chars ? chars : "");
    if (chars) env->ReleaseStringUTFChars(s, chars);
    return out;
}

std::string to_string(JNIEnv *env, jbyteArray bytes) {
    const jsize n = env->GetArrayLength(bytes);
    std::string out(static_cast<size_t>(n), '\0');
    if (n > 0) env->GetByteArrayRegion(bytes, 0, n, reinterpret_cast<jbyte *>(&out[0]));
    return out;
}

}  // namespace

extern "C" JNIEXPORT jlong JNICALL
Java_com_resonant_llama_LlamaNative_nativeLoad(JNIEnv *env, jobject, jstring path, jint nCtx, jint nThreads, jint nThreadsBatch) {
    Engine *engine = engine_load(to_string(env, path).c_str(), nCtx, nThreads, nThreadsBatch);
    return reinterpret_cast<jlong>(engine);
}

extern "C" JNIEXPORT void JNICALL
Java_com_resonant_llama_LlamaNative_nativeFree(JNIEnv *, jobject, jlong handle) {
    engine_free(reinterpret_cast<Engine *>(handle));
}

extern "C" JNIEXPORT void JNICALL
Java_com_resonant_llama_LlamaNative_nativeCancel(JNIEnv *, jobject, jlong handle) {
    engine_cancel(reinterpret_cast<Engine *>(handle));
}

extern "C" JNIEXPORT void JNICALL
Java_com_resonant_llama_LlamaNative_nativeReset(JNIEnv *, jobject, jlong handle) {
    engine_reset(reinterpret_cast<Engine *>(handle));
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_resonant_llama_LlamaNative_nativeSystemInfo(JNIEnv *env, jobject) {
    return env->NewStringUTF(engine_system_info());
}

// Reused by the last nativeGenerate: [promptTokens, reusedTokens].
extern "C" JNIEXPORT jintArray JNICALL
Java_com_resonant_llama_LlamaNative_nativeLastPromptStats(JNIEnv *env, jobject, jlong handle) {
    auto *engine = reinterpret_cast<Engine *>(handle);
    jint values[2] = {engine ? engine->last_prompt_tokens : 0, engine ? engine->last_reused_tokens : 0};
    jintArray out = env->NewIntArray(2);
    if (out) env->SetIntArrayRegion(out, 0, 2, values);
    return out;
}

// Pieces go to the sink as UTF-8 bytes, not a Java string: NewStringUTF only takes "modified" UTF-8
// and aborts under CheckJNI on an emoji.
extern "C" JNIEXPORT jint JNICALL
Java_com_resonant_llama_LlamaNative_nativeGenerate(JNIEnv *env, jobject, jlong handle,
                                                   jobjectArray roles, jobjectArray contents, // contents: byte[][] (UTF-8)
                                                   jint maxTokens, jfloat temperature, jobject sink) {
    auto *engine = reinterpret_cast<Engine *>(handle);
    if (!engine) return ENGINE_BAD_HANDLE;

    std::vector<ChatTurn> turns;
    const jsize count = env->GetArrayLength(roles);
    for (jsize i = 0; i < count; ++i) {
        auto role = static_cast<jstring>(env->GetObjectArrayElement(roles, i));
        auto content = static_cast<jbyteArray>(env->GetObjectArrayElement(contents, i));
        turns.push_back({to_string(env, role), to_string(env, content)});
        env->DeleteLocalRef(role);
        env->DeleteLocalRef(content);
    }

    // Looked up on the interface, not the object's own class, so it survives R8 renaming the lambda class.
    jclass sinkClass = env->FindClass("com/resonant/llama/LlamaPieceSink");
    if (!sinkClass) return ENGINE_SINK_FAILED;
    jmethodID onPiece = env->GetMethodID(sinkClass, "onPiece", "([B)Z");
    if (!onPiece) return ENGINE_SINK_FAILED;

    bool sinkFailed = false;
    int result = engine_generate(engine, turns, maxTokens, temperature, [&](const std::string &piece) {
        jbyteArray bytes = env->NewByteArray(static_cast<jsize>(piece.size()));
        if (!bytes) { sinkFailed = true; return false; }
        env->SetByteArrayRegion(bytes, 0, static_cast<jsize>(piece.size()), reinterpret_cast<const jbyte *>(piece.data()));
        jboolean keepGoing = env->CallBooleanMethod(sink, onPiece, bytes);
        env->DeleteLocalRef(bytes);
        if (env->ExceptionCheck()) {
            env->ExceptionClear();
            sinkFailed = true;
            return false;
        }
        return keepGoing == JNI_TRUE;
    });
    return sinkFailed ? ENGINE_SINK_FAILED : result;
}
