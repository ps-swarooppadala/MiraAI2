package com.mira.miraai.assessor

/**
 * Numeric tolerances for the Warrior II rule engine. feature-spec.md Section 8.3 explicitly
 * leaves per-pose angle targets and tolerance bands unspecified — every constant below is a
 * placeholder pending on-device tuning (see docs/PROGRESS.md), not a value sourced from spec.
 */
object WarriorIIThresholds {
    // placeholder — needs on-device tuning
    const val FRONT_KNEE_TARGET_DEG = 90f
    // placeholder — needs on-device tuning
    const val FRONT_KNEE_TOLERANCE_DEG = 15f
    // placeholder — needs on-device tuning
    const val BACK_LEG_STRAIGHT_MIN_DEG = 160f
    // placeholder — needs on-device tuning
    const val ARM_LEVEL_TOLERANCE = 0.05f
    // placeholder — needs on-device tuning
    const val TORSO_UPRIGHT_TOLERANCE = 0.05f
    // placeholder — needs on-device tuning
    const val FRONT_KNEE_PAST_ANKLE_TOLERANCE = 0.08f
    // placeholder — needs on-device tuning
    const val MIN_LANDMARK_VISIBILITY = 0.5f
}
