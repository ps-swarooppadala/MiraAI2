package com.mira.miraai.voice

/** Fake [TTSProvider] for contract tests — records every spoken line instead of speaking it. */
class FakeTTSProvider : TTSProvider {
    data class SpokenLine(val text: String, val lang: Lang)

    private val _spokenLines = mutableListOf<SpokenLine>()
    val spokenLines: List<SpokenLine> get() = _spokenLines

    override fun speak(text: String, lang: Lang) {
        _spokenLines += SpokenLine(text, lang)
    }
}
