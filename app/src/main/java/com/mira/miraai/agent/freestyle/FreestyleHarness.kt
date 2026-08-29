package com.mira.miraai.agent.freestyle

import com.mira.miraai.voice.LLMProvider

/**
 * Phase 8: the "harness routing" half of build-architecture.md Section 11.1 item 11 — sits
 * alongside the pose-correction perception->decision loop as a parallel harness responsibility.
 * Builds the [AgentPrompt] (always the full fixed [ActionSchema] set, per feature-spec.md
 * Section 4.5: "never expanded at runtime"), invokes the [LLMProvider], validates the response
 * defensively, and resolves START_WORKOUT/SUGGEST_WARMUP to a concrete routine id so callers
 * (the UI layer) don't need to know about [SessionContext] internals.
 *
 * The out-of-schema guard below is unreachable through [ActionSchemaClassifier] today (its
 * return type is the closed [ActionSchema] enum), but it's what feature-spec.md Section 4.5
 * actually asks for at the harness level — this is where a future SLM-backed [LLMProvider]
 * (Section 12.4) would get its raw output checked before it's allowed to touch app state.
 */
class FreestyleHarness(
    private val llmProvider: LLMProvider,
    private val defaultRoutineId: String = DEFAULT_ROUTINE_ID,
) {
    suspend fun resolve(userUtterance: String, context: SessionContext): AgentResponse {
        val prompt = AgentPrompt(
            userUtterance = userUtterance,
            allowedActions = ActionSchema.entries,
            context = context,
        )
        val response = llmProvider.complete(prompt)

        if (response.action !in prompt.allowedActions) {
            return fallbackResponse()
        }

        return if (response.action == ActionSchema.START_WORKOUT || response.action == ActionSchema.SUGGEST_WARMUP) {
            response.copy(resolvedRoutineId = response.resolvedRoutineId ?: context.lastRoutineId ?: defaultRoutineId)
        } else {
            response
        }
    }

    private fun fallbackResponse() = AgentResponse(
        action = ActionSchema.ANSWER_SMALLTALK,
        spokenLine = "I didn't quite catch that — want to start a workout, or hear what I've learned about you?",
        resolvedRoutineId = null,
    )

    private companion object {
        // The only routine with isCoachingSupported == true today (Phase 6 follow-up) —
        // update once a second coachable routine exists.
        const val DEFAULT_ROUTINE_ID = "warrior_ii_quick_practice"
    }
}
