package com.mira.miraai.perception

/**
 * Indices into MediaPipe PoseLandmarker's 33-point BlazePose output — covers the 10
 * [BodyJoint]s the Warrior II Assessor tracks (build-architecture.md Section 2/7 Phase 4).
 */
object PoseLandmarkIndex {
    const val LEFT_EAR = 7
    const val RIGHT_EAR = 8
    const val LEFT_SHOULDER = 11
    const val RIGHT_SHOULDER = 12
    const val LEFT_ELBOW = 13
    const val RIGHT_ELBOW = 14
    const val LEFT_WRIST = 15
    const val RIGHT_WRIST = 16
    const val LEFT_HIP = 23
    const val RIGHT_HIP = 24
    const val LEFT_KNEE = 25
    const val RIGHT_KNEE = 26
    const val LEFT_ANKLE = 27
    const val RIGHT_ANKLE = 28
}
