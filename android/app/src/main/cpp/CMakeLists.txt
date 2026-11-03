cmake_minimum_required(VERSION 3.22)
project(ccez_whisper CXX C)

# whisper.cpp, pinned as a submodule. The CPU only: on the S24 the 8-bit
# model runs about twice as fast as speech with ggml's ARM repacked
# kernels, and its OpenCL and Vulkan backends crash on the Adreno 750
# (checked 2026-11-02, whisper.cpp 482279b).
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_CPU_ARM_ARCH "armv8.2-a+dotprod+fp16+i8mm" CACHE STRING "" FORCE)
set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_SERVER OFF CACHE BOOL "" FORCE)
add_subdirectory(${CMAKE_CURRENT_SOURCE_DIR}/../../../../../third_party/whisper.cpp whisper)

add_library(ccez_whisper SHARED whisper_jni.cpp)
target_link_libraries(ccez_whisper whisper log)
