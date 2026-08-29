package com.mira.miraai.voice

import android.content.Context

/**
 * [TTSProvider] backed by Piper (VITS/ONNX) — the primary voice per build-architecture.md
 * Section 2.1 / feature-spec.md Section 12.4 ("pick a warm, mid-pitch voice").
 *
 * **Not a real synthesis path yet — flagged, not silently faked.** Piper needs a bundled
 * `.onnx` voice model asset plus an ONNX Runtime (or `sherpa-onnx`) native dependency, neither
 * of which exists in this repo: no voice model was fetchable from this sandboxed environment,
 * and picking a specific runtime binding is a real decision (model size vs. quality vs. APK
 * size) that shouldn't be guessed here. [isAvailable] is hardcoded `false` so [speak] always
 * throws [PiperUnavailableException] rather than pretending to speak — [FallbackTTSProvider]
 * catches that and falls through to [SystemTTSProvider] every time, which is the explicit,
 * non-silent fallback build-architecture.md Section 2.1 calls for.
 *
 * Whoever wires the real model: replace [isAvailable] and the body of [speak] with actual
 * inference; nothing else in the app should need to change since callers only see [TTSProvider].
 */
class PiperTTSProvider(private val context: Context) : TTSProvider {
    val isAvailable: Boolean = false

    override fun speak(text: String, lang: Lang) {
        if (!isAvailable) {
            throw PiperUnavailableException("Piper voice model/runtime not bundled in this build yet")
        }
        TODO("Real Piper ONNX synthesis — needs a bundled voice model asset, not built yet")
    }
}

class PiperUnavailableException(message: String) : Exception(message)
