package com.mira.miraai.agent.freestyle

import com.mira.miraai.memory.Fact

/**
 * Freestyle intent layer types — feature-spec.md Section 4.5/10.5. Full classifier/harness
 * routing is Phase 8 scope; these shapes are pulled forward to Phase 3 only because
 * `voice.LLMProvider` needs them to declare its interface contract.
 */
enum class ActionSchema {
    START_WORKOUT, SUGGEST_WARMUP, PAUSE, RESUME,
    SWITCH_POSE, END_SESSION, ANSWER_SMALLTALK, SHOW_MEMORY
}

data class SessionContext(
    val timeOfDay: String,
    val lastRoutineId: String?,
    val relevantFacts: List<Fact>,
)

data class AgentPrompt(
    val userUtterance: String,
    val allowedActions: List<ActionSchema>,
    val context: SessionContext,
)

data class AgentResponse(
    val action: ActionSchema,
    val spokenLine: String,
    val resolvedRoutineId: String?,
)
