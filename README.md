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
  the S24 a minute of speech takes about 35 s. Its OpenCL and Vulkan
  backends crash on the Adreno 750 (2026-11-02).
- Release: `fleet run --on y8 --snapshot -- scripts/release.sh` (ccez-tube's key).
