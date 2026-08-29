package com.mira.miraai.assessor

/** Named joints tracked by the Assessor layer — a minimal pure-Kotlin pose model. */
enum class BodyJoint {
    LEFT_SHOULDER, RIGHT_SHOULDER,
    LEFT_WRIST, RIGHT_WRIST,
    LEFT_HIP, RIGHT_HIP,
    LEFT_KNEE, RIGHT_KNEE,
    LEFT_ANKLE, RIGHT_ANKLE
}

/** A single tracked point plus MediaPipe-style visibility/presence confidence (0..1). */
data class Landmark(val position: Point2D, val visibility: Float)

/** One frame's worth of detected landmarks, keyed by joint. A missing joint is simply absent. */
data class PoseFrame(val landmarks: Map<BodyJoint, Landmark>) {
    fun landmark(joint: BodyJoint): Landmark? = landmarks[joint]
}

/** Which leg is forward in an asymmetric standing pose (e.g. Warrior II's bent front knee). */
enum class Side { LEFT, RIGHT }
