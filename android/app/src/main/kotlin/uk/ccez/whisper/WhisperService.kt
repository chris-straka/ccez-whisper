package uk.ccez.whisper

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelFileDescriptor
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Whisper for any app on the phone (owner, 2026-11-02: any app may use
 * it). The model is loaded on the first request and
 * let go two minutes after the last, so it holds no memory between uses.
 */
class WhisperService : Service() {
    private val model by lazy { Model(this) }
    private val lock = Any()
    private var ctx = 0L
    private val main = Handler(Looper.getMainLooper())
    private val unload = Runnable { synchronized(lock) { release() } }

    private val binder = object : IWhisper.Stub() {
        override fun state(): Int = when {
            model.ready -> 0
            model.progress() != null -> 2
            else -> 1
        }

        override fun hear(sound: ParcelFileDescriptor, lang: String?): String? {
            val bytes = ParcelFileDescriptor.AutoCloseInputStream(sound).use { it.readBytes() }
            val samples = samples(bytes)
            if (samples.isEmpty() || !model.ready) return null
            synchronized(lock) {
                main.removeCallbacks(unload)
                try {
                    if (ctx == 0L) ctx = Native.load(model.file.path)
                    if (ctx == 0L) return null
                    return Native.hear(ctx, samples, lang.orEmpty())?.toString(Charsets.UTF_8)
                } finally {
                    main.postDelayed(unload, IDLE_MS)
                }
            }
        }
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onDestroy() {
        main.removeCallbacks(unload)
        synchronized(lock) { release() }
        super.onDestroy()
    }

    private fun release() {
        if (ctx != 0L) Native.free(ctx)
        ctx = 0L
    }

    companion object {
        const val IDLE_MS = 2 * 60 * 1000L
    }
}

/** 16-bit little-endian samples (a WAV's data chunk, or raw) as floats. */
fun samples(bytes: ByteArray): FloatArray {
    var from = 0
    var until = bytes.size
    if (bytes.size >= 12 && String(bytes, 0, 4, Charsets.US_ASCII) == "RIFF" && String(bytes, 8, 4, Charsets.US_ASCII) == "WAVE") {
        var at = 12
        from = bytes.size
        while (at + 8 <= bytes.size) {
            val id = String(bytes, at, 4, Charsets.US_ASCII)
            val size = ByteBuffer.wrap(bytes, at + 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
            if (id == "data") {
                from = at + 8
                until = (from + size).coerceAtMost(bytes.size)
                break
            }
            at += 8 + size + (size and 1)
        }
    }
    val shorts = ByteBuffer.wrap(bytes, from, until - from).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
    return FloatArray(shorts.remaining()) { shorts.get(it) / 32768f }
}
