package com.mira.miraai.assessor

/**
 * Assessor verdict codes — feature-spec.md Section 10.1. Originally Warrior-II-only (Phase 1);
 * extended in Phase 13 with Tree Pose and Chair Pose codes once those poses got real Assessor
 * rule sets, per the [Verdict] interface extraction. `GOOD_FORM`/`INSUFFICIENT_VISIBILITY` are
 * shared across every pose rather than duplicated per-pose.
 */
enum class VerdictCode {
    GOOD_FORM,
    INSUFFICIENT_VISIBILITY,

    // Warrior II
    FRONT_KNEE_TOO_STRAIGHT,
    FRONT_KNEE_OVER_BENT,
    FRONT_KNEE_PAST_ANKLE,
    BACK_LEG_BENT,
    ARMS_NOT_LEVEL,
    ARMS_NOT_STRAIGHT,
    TORSO_LEANING,

    // Tree Pose
    TREE_STANDING_LEG_BENT,
    TREE_LIFTED_FOOT_TOO_LOW,
    TREE_HIPS_NOT_LEVEL,
    TREE_ARMS_NOT_AT_CHEST,
    TREE_HEAD_TILTED,

    // Chair Pose
    CHAIR_NOT_LOW_ENOUGH,
    CHAIR_KNEES_PAST_TOES,
    CHAIR_TORSO_TOO_FORWARD,
    CHAIR_ARMS_NOT_RAISED,
    CHAIR_ARMS_NOT_STRAIGHT,
}
