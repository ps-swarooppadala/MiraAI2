package com.mira.miraai.voice

/** Device-abstraction interface per build-architecture.md Section 2. See [PoseEstimator]. */
interface STTProvider {
    fun startListening(onResult: (String) -> Unit)
}
