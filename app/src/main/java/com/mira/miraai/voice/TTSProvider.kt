package com.mira.miraai.voice

/** Device-abstraction interface per build-architecture.md Section 2. See [PoseEstimator]. */
interface TTSProvider {
    fun speak(text: String, lang: Lang)
}
