package uk.ccez.whisper

/** whisper.cpp (src/main/cpp/whisper_jni.cpp). */
object Native {
    init {
        System.loadLibrary("ccez_whisper")
    }

    /** A loaded model, or 0 when it couldn't be read. */
    @JvmStatic external fun load(path: String): Long

    @JvmStatic external fun free(ctx: Long)

    /** What's said in 16 kHz mono [samples], as JSON in UTF-8; null when it failed. */
    @JvmStatic external fun hear(ctx: Long, samples: FloatArray, lang: String): ByteArray?
}
