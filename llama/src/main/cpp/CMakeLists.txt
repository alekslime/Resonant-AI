cmake_minimum_required(VERSION 3.22.1)
project(resonant_llama)

# llama.cpp is not copied into git. Clone this exact tag into this folder before building:
#
#   cd llama/src/main/cpp
#   git clone --depth 1 --branch b11904 https://github.com/ggml-org/llama.cpp.git
#
# This is its own Gradle module (and so its own CMake project) on purpose: whisper.cpp in the app
# module and llama.cpp both define a target called "ggml", and one CMake project cannot have two.
set(LLAMA_DIR ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp)

if(NOT EXISTS ${LLAMA_DIR}/CMakeLists.txt)
    message(FATAL_ERROR
        "llama.cpp not found at ${LLAMA_DIR}: see the comment at the top of this file for the clone command.")
endif()

set(LLAMA_BUILD_COMMON OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TOOLS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(LLAMA_CURL OFF CACHE BOOL "" FORCE)
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(GGML_CPU_KLEIDIAI OFF CACHE BOOL "" FORCE)

if(ANDROID_ABI STREQUAL "arm64-v8a")
    # Dot-product and half-float instructions: every phone chip since about 2018 has them (the
    # Helio G99's Cortex-A76 cores do). The app checks the phone before it loads a model, so an
    # older chip never reaches this code.
    set(GGML_CPU_ARM_ARCH "armv8.2-a+dotprod+fp16" CACHE STRING "" FORCE)
endif()

add_subdirectory(${LLAMA_DIR} llama_build)

add_library(llama_jni SHARED llama_jni.cpp engine.cpp)

target_include_directories(llama_jni PRIVATE
    ${LLAMA_DIR}/include
    ${LLAMA_DIR}/ggml/include
)

# Keep this library's own copy of ggml private. whisper_jni carries an older ggml, and two
# versions answering to the same names inside one app is a crash waiting to happen.
target_link_options(llama_jni PRIVATE -Wl,--exclude-libs,ALL)

find_library(log-lib log)
target_link_libraries(llama_jni llama ${log-lib})
