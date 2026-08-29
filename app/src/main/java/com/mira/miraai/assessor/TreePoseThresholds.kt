package com.mira.miraai.assessor

/**
 * Numeric tolerances for the Tree Pose rule engine. Same category of gap as
 * [WarriorIIThresholds]: feature-spec.md only names Tree Pose in the MVP content table (Section
 * 3.2), no per-joint angle targets — every constant below is a placeholder pending on-device
 * tuning (see docs/PROGRESS.md), not a value sourced from spec.
 */
object TreePoseThresholds {
    // placeholder — needs on-device tuning
    const val STANDING_LEG_STRAIGHT_MIN_DEG = 160f
    // placeholder — needs on-device tuning
    const val LIFTED_FOOT_MIN_HEIGHT_TOLERANCE = 0.03f
    // placeholder — needs on-device tuning
    const val HIPS_LEVEL_TOLERANCE = 0.04f
    // placeholder — needs on-device tuning
    const val ARMS_AT_CHEST_X_TOLERANCE = 0.08f
    // needs on-device tuning — added with ear tracking; flags a tilted head/gaze rather than
    // the level-eye-line focus feature-spec.md describes for Tree Pose but doesn't quantify
    const val HEAD_TILT_TOLERANCE = 0.03f
    // tuned down from 0.5, same on-device feedback as WarriorIIThresholds — still a placeholder.
    const val MIN_LANDMARK_VISIBILITY = 0.3f
}
