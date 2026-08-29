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

/**
 * @param repeatIndex how many consecutive times (0-based) this same [verdictCode] has now been
 * cued back-to-back — lets the Mouth ([com.mira.miraai.voice.CueTemplates]) rotate through
 * different phrasings for the same code instead of repeating one exact line verbatim on every
 * cooldown tick, which reads as robotic (feature-spec.md Section 10.3's "template-first
 * language" doesn't mean "one template per code forever").
 */
data class CoachDecision(
    val intent: CoachIntent,
    val verdictCode: VerdictCode?,
    val escalation: CueEscalation = CueEscalation.NORMAL,
    val repeatIndex: Int = 0,
)
