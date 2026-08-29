package com.mira.miraai.voice

import com.mira.miraai.agent.CueEscalation
import com.mira.miraai.assessor.VerdictCode

/**
 * needs tuning — placeholder template phrases for Coach Agent cues, keyed by [VerdictCode].
 * feature-spec.md Section 9.2 calls for a real "template-first language" library (Phase 7);
 * this is a minimal stand-in so Phase 4's camera pipeline has something to speak, not the
 * final wording. Flagged in docs/PROGRESS.md — replace wholesale in Phase 7, don't tune in place.
 */
object CueTemplates {
    private val NORMAL = mapOf(
        VerdictCode.FRONT_KNEE_TOO_STRAIGHT to "Bend your front knee a bit more.",
        VerdictCode.FRONT_KNEE_OVER_BENT to "Ease up on that front knee bend.",
        VerdictCode.FRONT_KNEE_PAST_ANKLE to "Bring your front knee back over your ankle.",
        VerdictCode.BACK_LEG_BENT to "Straighten your back leg.",
        VerdictCode.ARMS_NOT_LEVEL to "Level your arms out with your shoulders.",
        VerdictCode.TORSO_LEANING to "Keep your torso upright over your hips.",
        VerdictCode.INSUFFICIENT_VISIBILITY to "Step back so I can see your whole body.",
    )
    private val FIRM = mapOf(
        VerdictCode.FRONT_KNEE_TOO_STRAIGHT to "Bend that front knee — really commit to the angle.",
        VerdictCode.FRONT_KNEE_OVER_BENT to "Come up out of that knee bend now.",
        VerdictCode.FRONT_KNEE_PAST_ANKLE to "Your knee is past your ankle — pull it back now.",
        VerdictCode.BACK_LEG_BENT to "Lock that back leg out straight.",
        VerdictCode.ARMS_NOT_LEVEL to "Arms need to be level — reach out through both hands.",
        VerdictCode.TORSO_LEANING to "You're leaning — stack your shoulders over your hips.",
        VerdictCode.INSUFFICIENT_VISIBILITY to "I still can't see you — step back into frame.",
    )
    private const val CONFIRM_IMPROVEMENT_LINE = "Nice, that's it."
    private const val FALLBACK_LINE = "Reset your position."

    fun forIssue(code: VerdictCode, escalation: CueEscalation): String =
        (if (escalation == CueEscalation.FIRM) FIRM else NORMAL)[code] ?: FALLBACK_LINE

    fun forConfirmImprovement(): String = CONFIRM_IMPROVEMENT_LINE

    fun forSafetyOverride(code: VerdictCode): String = FIRM[code] ?: FALLBACK_LINE
}
