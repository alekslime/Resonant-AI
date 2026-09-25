cmake_minimum_required(VERSION 3.22.1)
project(resonant_whisper)

# ---------------------------------------------------------------------------
# This does NOT vendor whisper.cpp itself — that's upstream C/C++ source
# (https://github.com/ggerganov/whisper.cpp), not something to hand-copy.
# Clone it into this exact directory before building:
#
#   cd app/src/main/cpp
#   git clone --depth 1 --branch v1.7.6 https://github.com/ggerganov/whisper.cpp.git
#
# Pin a tag rather than tracking main — whisper.cpp's internal layout and API
# have shifted before (e.g. ggml moving under its own subdirectory), and
# whisper_jni.cpp below is written against a specific, long-stable slice of
# whisper.h. If your checkout's layout differs, whisper.h and the top-level
# CMakeLists in your clone are the source of truth — adjust the include paths
# and library target name below to match.
# ---------------------------------------------------------------------------
set(WHISPER_DIR ${CMAKE_CURRENT_SOURCE_DIR}/whisper.cpp)

if(NOT EXISTS ${WHISPER_DIR}/CMakeLists.txt)
    message(FATAL_ERROR
        "whisper.cpp not found at ${WHISPER_DIR} — see the comment at the top "
        "of this file for the clone command.")
endif()

# Only need the library itself, not whisper.cpp's own examples/tests/server.
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
add_subdirectory(${WHISPER_DIR} whisper_build)

add_library(whisper_jni SHARED whisper_jni.cpp)

# whisper_jni.cpp only calls whisper.cpp's public API (whisper_*), never
# ggml internals directly, so it only needs whisper.h's directory. Do NOT
# add ggml's internal src/ include paths here on the assumption that fixes
# a missing-header error — that's a real, documented problem (see
# ggml-org/whisper.cpp#2624 and #2647), but it hits people who hand-list
# ggml's individual .c files as their own build target, bypassing whisper.cpp's
# own CMake. Building it correctly through add_subdirectory() above means
# ggml's internal includes are that target's problem, already solved by its
# own CMakeLists — not something a caller of the public API should need to
# know about. If you DO hit a "fatal error: ... not found" pointing at a
# ggml-internal header (ggml-cpu-impl.h, ggml-aarch64.c, etc.), it means
# something about that assumption didn't hold for whatever version you
# cloned — that's the CMake issue to search for by exact filename, not a
# sign to start guessing extra include paths here.
target_include_directories(whisper_jni PRIVATE
    ${WHISPER_DIR}/include
)

find_library(log-lib log)

target_link_libraries(whisper_jni
    whisper
    ${log-lib}
)
