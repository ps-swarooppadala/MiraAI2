package com.mira.miraai.agent

import com.mira.miraai.assessor.VerdictCode

/** What the Coach Agent decided to do this tick — feature-spec.md Section 10.2. */
enum class CoachIntent {
    SPEAK_CUE,
    CONFIRM_IMPROVEMENT,
    SAFETY_OVERRIDE,
    SILENT
}

/** Cue intensity — escalates only on persistence, per the "escalate only on persistence" guardrail. */
enum class CueEscalation {
    NORMAL,
    FIRM
}

data class CoachDecision(
    val intent: CoachIntent,
    val verdictCode: VerdictCode?,
    val escalation: CueEscalation = CueEscalation.NORMAL
)
