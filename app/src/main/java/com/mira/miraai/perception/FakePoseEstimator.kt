package com.mira.miraai.perception

import androidx.camera.core.ImageProxy

/** Fake [PoseEstimator] for contract tests and previews — ignores [frame], returns [nextFrame]. */
class FakePoseEstimator(var nextFrame: PoseFrame = PoseFrame(emptyMap())) : PoseEstimator {
    var lastFrameSeen: ImageProxy? = null
        private set

    override fun estimate(frame: ImageProxy, onResult: (PoseFrame) -> Unit) {
        lastFrameSeen = frame
        onResult(nextFrame)
    }
}
