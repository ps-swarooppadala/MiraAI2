package com.mira.miraai.voice

/** Fake [STTProvider] for contract tests — [emit] triggers the last-registered listener. */
class FakeSTTProvider : STTProvider {
    private var listener: ((String) -> Unit)? = null

    override fun startListening(onResult: (String) -> Unit) {
        listener = onResult
    }

    fun emit(transcript: String) {
        listener?.invoke(transcript)
    }
}
