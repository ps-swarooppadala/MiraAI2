package com.mira.miraai.voice

/**
 * Optional companion contract for [TTSProvider] implementations that hold onto engine
 * resources (e.g. Android's `TextToSpeech`) and need an explicit release. Not part of
 * [TTSProvider] itself — that interface is fixed by build-architecture.md Section 2 — so
 * callers check for this via `(provider as? TTSShutdown)?.shutdown()`.
 */
interface TTSShutdown {
    fun shutdown()
}
