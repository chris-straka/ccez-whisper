package uk.ccez.whisper

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import java.io.File
import java.security.MessageDigest

/**
 * The model: Whisper large-v3-turbo, 8-bit (874 MB), from whisper.cpp's
 * own model page (MIT). On the S24 it hears a minute of speech in about
 * 35 s with near-perfect French; the 4-bit one is no faster and the
 * small model garbles. Fetched by Android's download manager, so it goes
 * on with the app closed, then checked against its hash.
 */
class Model(private val context: Context) {
    val file: File get() = File(context.getExternalFilesDir(null), NAME)
    private val part: File get() = File(context.getExternalFilesDir(null), "$NAME.part")
    private val prefs = context.getSharedPreferences("model", Context.MODE_PRIVATE)
    private val downloads = context.getSystemService(DownloadManager::class.java)

    val ready: Boolean get() = file.exists()

    /** The download under way: its bytes so far and in all; null when none. */
    fun progress(): Pair<Long, Long>? {
        val id = prefs.getLong("download", -1L).takeIf { it >= 0 } ?: return null
        downloads.query(DownloadManager.Query().setFilterById(id)).use { c ->
            if (!c.moveToFirst()) return null
            val status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            if (status == DownloadManager.STATUS_FAILED) {
                prefs.edit().remove("download").apply()
                return null
            }
            val done = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val total = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            if (status == DownloadManager.STATUS_SUCCESSFUL) finish()
            return done to total.coerceAtLeast(SIZE)
        }
    }

    fun download() {
        if (ready || progress() != null) return
        part.delete()
        val request = DownloadManager.Request(Uri.parse(URL))
            .setTitle("Whisper model")
            .setDestinationUri(Uri.fromFile(part))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
        prefs.edit().putLong("download", downloads.enqueue(request)).apply()
    }

    /** A finished download becomes the model once its hash matches. */
    @Synchronized
    private fun finish() {
        if (!part.exists()) return
        prefs.edit().remove("download").apply()
        if (sha256(part) == SHA256) part.renameTo(file) else part.delete()
    }

    private fun sha256(f: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        f.inputStream().use { input ->
            val buf = ByteArray(1 shl 20)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val NAME = "ggml-large-v3-turbo-q8_0.bin"
        const val URL = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/$NAME"
        const val SHA256 = "317eb69c11673c9de1e1f0d459b253999804ec71ac4c23c17ecf5fbe24e259a1"
        const val SIZE = 874_188_075L
    }
}
