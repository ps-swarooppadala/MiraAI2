package com.mira.miraai.perception

import androidx.camera.core.ImageProxy

/**
 * Device-abstraction interface per build-architecture.md Section 2. Business logic downstream
 * (Assessor, Coach Agent, UI) depends only on this — never on MediaPipe/QNN/a specific model
 * file directly. Bound per-flavor by `DevPhoneAiModule`/`IqooAiModule`.
 *
 * See [MediaPipePoseEstimator] for the current concrete implementation and its documented
 * deviation from this synchronous contract, pending Phase 4's camera pipeline rewiring.
 */
interface PoseEstimator {
    fun estimate(frame: ImageProxy): PoseFrame
}
