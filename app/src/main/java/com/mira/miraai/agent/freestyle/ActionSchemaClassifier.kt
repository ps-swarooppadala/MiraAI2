package com.mira.miraai.agent.freestyle

import com.mira.miraai.voice.LLMProvider

/**
 * Phase 8: STT transcript -> [ActionSchema] classifier, feature-spec.md Section 4.5/10.5/12.5.
 *
 * A "lightweight separate classifier" (Section 12.5's fallback option, not the SLM path) —
 * keyword matching over the fixed 8-value [ActionSchema] enum. Because the enum is closed at
 * the Kotlin type level, this classifier structurally *cannot* return a value outside the
 * schema; unmatched utterances fall back to [ActionSchema.ANSWER_SMALLTALK] by construction,
 * satisfying feature-spec.md Section 4.5's fallback rule without needing runtime validation
 * here (the runtime double-check still happens in [FreestyleHarness], defensively, for
 * whatever [LLMProvider] a future SLM-backed implementation plugs in).
 *
 * `resolvedRoutineId` is left null here — resolving an action to an actual `Routine` id is
 * [FreestyleHarness]'s job, since only it knows [SessionContext.lastRoutineId].
 */
class ActionSchemaClassifier : LLMProvider {

    override suspend fun complete(prompt: AgentPrompt): AgentResponse {
        val action = classify(prompt.userUtterance)
        return AgentResponse(
            action = action,
            spokenLine = spokenLineFor(action),
            resolvedRoutineId = null,
        )
    }

    internal fun classify(utterance: String): ActionSchema {
        val text = utterance.lowercase()
        return when {
            PAUSE_PHRASES.any { text.contains(it) } -> ActionSchema.PAUSE
            RESUME_PHRASES.any { text.contains(it) } -> ActionSchema.RESUME
            END_SESSION_PHRASES.any { text.contains(it) } -> ActionSchema.END_SESSION
            SWITCH_POSE_PHRASES.any { text.contains(it) } -> ActionSchema.SWITCH_POSE
            SHOW_MEMORY_PHRASES.any { text.contains(it) } -> ActionSchema.SHOW_MEMORY
            SUGGEST_WARMUP_PHRASES.any { text.contains(it) } -> ActionSchema.SUGGEST_WARMUP
            START_WORKOUT_PHRASES.any { text.contains(it) } -> ActionSchema.START_WORKOUT
            else -> ActionSchema.ANSWER_SMALLTALK
        }
    }

    private fun spokenLineFor(action: ActionSchema): String = when (action) {
        ActionSchema.START_WORKOUT -> "Let's get started."
        ActionSchema.SUGGEST_WARMUP -> "A quick warm-up sounds good — let's ease in."
        ActionSchema.PAUSE -> "Pausing for you."
        ActionSchema.RESUME -> "Picking back up."
        ActionSchema.SWITCH_POSE -> "Let's try something else."
        ActionSchema.END_SESSION -> "Okay, wrapping up for today."
        ActionSchema.SHOW_MEMORY -> "Here's what I've picked up about you so far."
        ActionSchema.ANSWER_SMALLTALK -> "I'm still learning to have that conversation."
    }

    private companion object {
        val START_WORKOUT_PHRASES = listOf("start my workout", "let's start", "lets start", "begin the workout", "let's do this", "lets do this")
        val SUGGEST_WARMUP_PHRASES = listOf("warm up", "warmup", "something easy", "ease into it")
        val PAUSE_PHRASES = listOf("pause", "hold on", "give me a second", "wait a second")
        val RESUME_PHRASES = listOf("resume", "keep going", "let's continue", "lets continue", "continue please")
        val SWITCH_POSE_PHRASES = listOf("switch pose", "different pose", "change the pose", "something else instead")
        val END_SESSION_PHRASES = listOf("end session", "end workout", "i'm done", "im done", "that's enough", "thats enough", "stop for today")
        val SHOW_MEMORY_PHRASES = listOf("what do you know about me", "show me my progress", "what have you learned", "what have you learnt")
    }
}
