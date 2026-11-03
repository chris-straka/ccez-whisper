# ccez-whisper

Read `~/AGENTS.md` and `~/SWE/AGENTS.md` first. README says what this is.

- Kotlin + a small JNI file over whisper.cpp; no Rust here.
- Changing the AIDL changes ccez-tube's copy
  (`ccez-tube/android/app/src/main/aidl`): keep them the same.
- Check on the owner's phone (adb -s, never a bare adb); the emulator
  isn't built for (arm64 only, too slow on x86).
- Wording: pithy, no helper text.
