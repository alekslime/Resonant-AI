plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

// llama.cpp on the phone (the on-phone AI). Its own module so its CMake project does not meet
// whisper.cpp's: both define a target called "ggml". See src/main/cpp/CMakeLists.txt.
android {
    namespace = "com.resonant.llama"
    compileSdk = 34
    ndkVersion = "26.1.10909125"

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")

        externalNativeBuild {
            cmake {
                cppFlags += listOf("-std=c++17", "-O3")
                arguments += listOf("-DANDROID_STL=c++_shared", "-DCMAKE_BUILD_TYPE=Release")
            }
        }
        ndk {
            // Only 64-bit ARM: every phone that can run a model this size is one.
            abiFilters += listOf("arm64-v8a")
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}
