# JNI: the native code looks these up by name.
-keep class com.resonant.llama.LlamaNative { native <methods>; <init>(); }
-keep interface com.resonant.llama.LlamaPieceSink { *; }
