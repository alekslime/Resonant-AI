// Thin JNI bridge to whisper.cpp. Kotlin side: WhisperNative.kt.
//
// NOTE ON VERIFICATION: this was written by hand against whisper.h's public
// API from memory, in a sandbox with no Android NDK and no network access to
// actually fetch or build whisper.cpp — it has not been compiled. The
// functions used here (whisper_init_from_file_with_params, whisper_full,
// whisper_full_n_segments, whisper_full_get_segment_text) have been stable
// for a long time, but if your vendored version's whisper.h differs, this is
// the file most likely to need small fixes — check field/function names
// against your actual checkout first.

#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include "whisper.h"

#define LOG_TAG "WhisperJNI"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT jlong JNICALL
Java_com_resonant_app_speech_whisper_WhisperNative_nativeInit(
        JNIEnv *env, jobject /*thiz*/, jstring modelPath) {
    const char *path = env->GetStringUTFChars(modelPath, nullptr);

    struct whisper_context_params cparams = whisper_context_default_params();
    struct whisper_context *ctx = whisper_init_from_file_with_params(path, cparams);
    // If your whisper.cpp checkout predates the _with_params variant, use
    // instead:  struct whisper_context *ctx = whisper_init_from_file(path);

    env->ReleaseStringUTFChars(modelPath, path);

    if (ctx == nullptr) {
        LOGE("whisper_init_from_file_with_params failed to load model");
        return 0;
    }
    return reinterpret_cast<jlong>(ctx);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_resonant_app_speech_whisper_WhisperNative_nativeTranscribe(
        JNIEnv *env, jobject /*thiz*/, jlong handle, jfloatArray pcm, jint /*sampleRate*/) {
    auto *ctx = reinterpret_cast<struct whisper_context *>(handle);
    if (ctx == nullptr) {
        LOGE("nativeTranscribe called with a null context");
        return env->NewStringUTF("");
    }

    jsize n = env->GetArrayLength(pcm);
    std::vector<float> samples(static_cast<size_t>(n));
    env->GetFloatArrayRegion(pcm, 0, n, samples.data());

    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.language = "en";
    params.translate = false;
    params.print_progress = false;
    params.print_realtime = false;
    params.print_special = false;
    params.print_timestamps = false;
    params.no_timestamps = true;
    params.single_segment = false;
    // 4 threads is a reasonable default for a one-shot short command on a
    // modern phone; tune down if this contends badly with the UI thread.
    params.n_threads = 4;

    if (whisper_full(ctx, params, samples.data(), static_cast<int>(samples.size())) != 0) {
        LOGE("whisper_full failed");
        return env->NewStringUTF("");
    }

    std::string result;
    const int n_segments = whisper_full_n_segments(ctx);
    for (int i = 0; i < n_segments; ++i) {
        result += whisper_full_get_segment_text(ctx, i);
    }
    return env->NewStringUTF(result.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_resonant_app_speech_whisper_WhisperNative_nativeFree(
        JNIEnv * /*env*/, jobject /*thiz*/, jlong handle) {
    auto *ctx = reinterpret_cast<struct whisper_context *>(handle);
    if (ctx != nullptr) {
        whisper_free(ctx);
    }
}
