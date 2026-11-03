// The app's one native entry: load a model, hear 16 kHz mono samples.
// Whatever language the caller names (or Whisper's own guess), the
// answer is JSON: {"lang":"fr","segments":[{"t0":ms,"t1":ms,"text":"…"}]}.

#include <jni.h>
#include <android/log.h>
#include <string>
#include <thread>
#include <vector>

#include "whisper.h"

#define LOG(...) __android_log_print(ANDROID_LOG_INFO, "ccez-whisper", __VA_ARGS__)

namespace {

std::string escape(const char *s) {
    std::string out;
    for (const char *p = s; *p; ++p) {
        switch (*p) {
            case '"': out += "\\\""; break;
            case '\\': out += "\\\\"; break;
            case '\n': out += "\\n"; break;
            case '\r': out += "\\r"; break;
            case '\t': out += "\\t"; break;
            default:
                if (static_cast<unsigned char>(*p) < 0x20) {
                    char buf[8];
                    snprintf(buf, sizeof buf, "\\u%04x", *p);
                    out += buf;
                } else {
                    out += *p;
                }
        }
    }
    return out;
}

// The big cores: the S24 has one X4 and five A720s worth using; past
// six threads the little cores slow it down (measured: 6 beat 4 and 8).
int threads() {
    unsigned n = std::thread::hardware_concurrency();
    return n >= 8 ? 6 : (n > 2 ? static_cast<int>(n) - 1 : 1);
}

}  // namespace

extern "C" JNIEXPORT jlong JNICALL
Java_uk_ccez_whisper_Native_load(JNIEnv *env, jclass, jstring path) {
    const char *p = env->GetStringUTFChars(path, nullptr);
    whisper_context_params cp = whisper_context_default_params();
    cp.use_gpu = false;
    whisper_context *ctx = whisper_init_from_file_with_params(p, cp);
    env->ReleaseStringUTFChars(path, p);
    return reinterpret_cast<jlong>(ctx);
}

extern "C" JNIEXPORT void JNICALL
Java_uk_ccez_whisper_Native_free(JNIEnv *, jclass, jlong ctx) {
    if (ctx) whisper_free(reinterpret_cast<whisper_context *>(ctx));
}

// UTF-8 bytes, not a jstring: NewStringUTF wants modified UTF-8 and
// aborts on an emoji or a token cut mid-character.
extern "C" JNIEXPORT jbyteArray JNICALL
Java_uk_ccez_whisper_Native_hear(JNIEnv *env, jclass, jlong handle, jfloatArray samples, jstring lang) {
    auto *ctx = reinterpret_cast<whisper_context *>(handle);
    if (!ctx) return nullptr;
    const jsize n = env->GetArrayLength(samples);
    std::vector<float> pcm(n);
    env->GetFloatArrayRegion(samples, 0, n, pcm.data());
    const char *l = env->GetStringUTFChars(lang, nullptr);
    std::string language = l;
    env->ReleaseStringUTFChars(lang, l);

    // Greedy: as good as beam search on news and talk here, and faster.
    whisper_full_params wp = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    wp.n_threads = threads();
    wp.language = language.empty() ? "auto" : language.c_str();
    wp.translate = false;
    wp.print_progress = false;
    wp.print_realtime = false;
    wp.print_timestamps = false;
    wp.print_special = false;
    wp.suppress_blank = true;
    wp.no_context = true;

    if (whisper_full(ctx, wp, pcm.data(), n) != 0) return nullptr;

    std::string out = "{\"lang\":\"";
    out += whisper_lang_str(whisper_full_lang_id(ctx));
    out += "\",\"segments\":[";
    const int segs = whisper_full_n_segments(ctx);
    for (int i = 0; i < segs; ++i) {
        if (i) out += ",";
        // Whisper's times are in 10 ms steps.
        out += "{\"t0\":" + std::to_string(whisper_full_get_segment_t0(ctx, i) * 10);
        out += ",\"t1\":" + std::to_string(whisper_full_get_segment_t1(ctx, i) * 10);
        out += ",\"text\":\"" + escape(whisper_full_get_segment_text(ctx, i)) + "\"}";
    }
    out += "]}";
    LOG("heard %d samples: %d segments", n, segs);
    jbyteArray bytes = env->NewByteArray(static_cast<jsize>(out.size()));
    env->SetByteArrayRegion(bytes, 0, static_cast<jsize>(out.size()), reinterpret_cast<const jbyte *>(out.data()));
    return bytes;
}
