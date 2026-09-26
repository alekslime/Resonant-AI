# Resonant proguard rules

# JNI — keep the native bridge so R8 doesn't rename or strip the methods
# that whisper.cpp calls back into via JNI.
-keep class com.resonant.app.speech.whisper.WhisperNative {
    native <methods>;
    <init>();
}

# org.json — used directly by OllamaClient and ChatHistoryStore via string keys;
# R8 can't see those usages as type-safe references so it would otherwise strip
# or rename the constructors and methods.
-keep class org.json.** { *; }

# Kotlin coroutines — R8 needs these to correctly handle continuation objects
# and debug metadata; without them, stack traces in release builds can be
# misleading and some coroutine internals can be incorrectly removed.
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.** {
    volatile <fields>;
}

# Compose — keep lambda and composable function metadata that the runtime
# reflects on for recomposition, slot table management, and tooling.
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# Keep line numbers in release stack traces so crash reports are readable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
