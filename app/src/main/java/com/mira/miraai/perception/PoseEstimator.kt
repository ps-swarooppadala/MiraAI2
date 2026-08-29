package com.mira.miraai.perception

import androidx.camera.core.ImageProxy

/**
 * Device-abstraction interface per build-architecture.md Section 2. Business logic downstream
 * (Assessor, Coach Agent, UI) depends only on this — never on MediaPipe/QNN/a specific model
 * file directly. Bound per-flavor by the `devPhone`/`iqoo` `aiModule`.
 *
 * **Phase 4 reconciliation (was flagged in Phase 3):** the contract is async, not the originally
 * sketched synchronous `estimate(frame): PoseFrame`. [MediaPipePoseEstimator] runs MediaPipe's
 * `LIVE_STREAM` mode (needed for Phase 0's measured 25-31 FPS on devPhone), which only supports
 * a registered result listener, not a return value. [onResult] is delivered off the calling
 * thread. Callers own exactly one active [onResult] per estimator instance — passing a new one
 * on the next call replaces the previous, which is fine for the single-consumer camera pipeline
 * this drives, but would need revisiting for a multi-consumer use case.
 */
interface PoseEstimator {
    fun estimate(frame: ImageProxy, onResult: (PoseFrame) -> Unit)
}
