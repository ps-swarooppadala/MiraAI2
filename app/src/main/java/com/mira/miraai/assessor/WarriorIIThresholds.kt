package com.mira.miraai.assessor

/**
 * Numeric tolerances for the Warrior II rule engine. feature-spec.md Section 8.3 explicitly
 * leaves per-pose angle targets and tolerance bands unspecified — every constant below is a
 * placeholder pending on-device tuning (see docs/PROGRESS.md), not a value sourced from spec.
 */
object WarriorIIThresholds {
    // placeholder — needs on-device tuning
    const val FRONT_KNEE_TARGET_DEG = 90f
    // widened from 15 after 2026-08-29 feedback: Warrior II is the routine's closing pose and
    // shouldn't demand near-perfect knee bend after the user is already tired — still a
    // placeholder, may need further tuning.
    const val FRONT_KNEE_TOLERANCE_DEG = 22f
    // relaxed from 160 for the same reason — a straighter-than-ideal back leg still counts as
    // good form here rather than triggering a correction cue.
    const val BACK_LEG_STRAIGHT_MIN_DEG = 145f
    // placeholder — needs on-device tuning
    const val ARM_LEVEL_TOLERANCE = 0.08f
    // placeholder — needs on-device tuning
    const val TORSO_UPRIGHT_TOLERANCE = 0.08f
    // placeholder — needs on-device tuning
    const val FRONT_KNEE_PAST_ANKLE_TOLERANCE = 0.12f
    // needs on-device tuning — added with elbow tracking, feature-spec.md doesn't set an arm
    // straightness target for Warrior II
    const val ARM_STRAIGHT_MIN_DEG = 155f
    // tuned down from 0.5 after 2026-08-29 on-device feedback: 0.5 triggered
    // INSUFFICIENT_VISIBILITY/"can't see you" cues on minor tracking dips, not real
    // out-of-frame cases — still a placeholder, may need further tuning.
    const val MIN_LANDMARK_VISIBILITY = 0.3f
}
