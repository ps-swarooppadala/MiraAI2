package com.mira.miraai.assessor

/**
 * Numeric tolerances for the Chair Pose rule engine. Same category of gap as
 * [WarriorIIThresholds]/[TreePoseThresholds] — feature-spec.md only names Chair Pose in the MVP
 * content table (Section 3.2), no per-joint angle targets. Every constant below is a placeholder
 * pending on-device tuning (see docs/PROGRESS.md), not a value sourced from spec.
 */
object ChairPoseThresholds {
    // placeholder — needs on-device tuning
    const val KNEE_TARGET_DEG = 110f
    // placeholder — needs on-device tuning
    const val KNEE_TOLERANCE_DEG = 20f
    // placeholder — needs on-device tuning
    const val KNEES_PAST_TOES_TOLERANCE = 0.08f
    // placeholder — needs on-device tuning
    const val TORSO_FORWARD_TOLERANCE = 0.12f
    // placeholder — needs on-device tuning
    const val ARMS_RAISED_TOLERANCE = 0.05f
    // needs on-device tuning — added with elbow tracking, feature-spec.md doesn't set an arm
    // straightness target for Chair Pose's overhead reach
    const val ARM_STRAIGHT_MIN_DEG = 155f
    // tuned down from 0.5, same on-device feedback as WarriorIIThresholds — still a placeholder.
    const val MIN_LANDMARK_VISIBILITY = 0.3f
}
