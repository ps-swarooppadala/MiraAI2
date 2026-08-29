package com.mira.miraai.assessor

/**
 * Phase 0 walking-skeleton check only: a single hardcoded joint/threshold to prove the
 * camera -> pose -> voice pipeline runs end-to-end. Replaced by the real per-pose rule
 * engine in Phase 1 (feature-spec.md Section 8.3; build-architecture.md Section 7 Phase 1).
 */
object ElbowCheck {
    // needs tuning — placeholder, not sourced from feature-spec.md (see docs/PROGRESS.md)
    const val ARM_BENT_THRESHOLD_DEG = 150f

    fun isRightArmBent(shoulder: Point2D, elbow: Point2D, wrist: Point2D): Boolean {
        return angleDegrees(shoulder, elbow, wrist) < ARM_BENT_THRESHOLD_DEG
    }
}
