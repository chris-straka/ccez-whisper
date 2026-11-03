# ccez-whisper

Whisper on the phone, for any app on it. One Android app holds
the model (Whisper large-v3-turbo, 8-bit, 874 MB, fetched on first open
from whisper.cpp's model page) and hears speech for ccez-tube's AI
subtitles, line checks and speaking practice, and later ccez-llm.

- Other apps bind `uk.ccez.whisper/.WhisperService` (action
  `uk.ccez.whisper.HEAR`) and call `IWhisper.hear` with 16 kHz mono
  16-bit sound (`android/app/src/main/aidl`). Any app on the phone may
  (owner, 2026-11-02).
- whisper.cpp is a submodule (`third_party/whisper.cpp`), on the CPU: on
  the S24 a minute of speech takes about 35 s (8-bit, greedy, 6 threads).
  The GPU doesn't help (tried 2026-11-02, whisper.cpp 482279b):
  - Vulkan: Qualcomm's shader compiler (libllvm-qgl.so) crashes on a ggml
    shader; with integer dot, fp16 and coopmat off it runs, slower than
    the CPU and hearing garbage.
  - OpenCL: crashes asking a buffer's alignment before the backend
    exists (`ggml_backend_opencl_buffer_type_get_alignment`); calling
    `ggml_cl_init` there fixes it, but the encoder runs no faster and
    each token takes 270-300 ms against the CPU's 40 ms: 104 s a minute.
  - The real speed-up would be the Hexagon NPU (Qualcomm's QNN builds of
    Whisper), a different stack.
- Release: `fleet run --on y8 --snapshot -- scripts/release.sh` (ccez-tube's key).

## Licence

MIT (LICENSE). whisper.cpp and the model are MIT too.
