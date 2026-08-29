package com.mira.miraai.voice

/**
 * Placeholder [STTProvider] — real Whisper-Tiny wiring is Phase 8 scope
 * (build-architecture.md Section 7). Never invokes [onResult]; exists so DI modules and
 * Freestyle-adjacent code can compile against the real interface ahead of that phase.
 */
class StubSTTProvider : STTProvider {
    override fun startListening(onResult: (String) -> Unit) {
        // no-op until Phase 8
    }
}
