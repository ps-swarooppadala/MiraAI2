package com.mira.miraai.voice

import com.mira.miraai.agent.freestyle.ActionSchema
import com.mira.miraai.agent.freestyle.AgentPrompt
import com.mira.miraai.agent.freestyle.AgentResponse

/**
 * Placeholder [LLMProvider] — the real ActionSchema classifier/SLM bridge is Phase 8 scope
 * (build-architecture.md Section 7). Always falls back to `ANSWER_SMALLTALK`, matching the
 * harness's documented out-of-schema fallback behavior (feature-spec.md Section 4.5) so any
 * code wired against this provider today already exercises the correct failure path.
 */
class StubLLMProvider : LLMProvider {
    override suspend fun complete(prompt: AgentPrompt): AgentResponse =
        AgentResponse(
            action = ActionSchema.ANSWER_SMALLTALK,
            spokenLine = "I'm still learning to have that conversation.",
            resolvedRoutineId = null,
        )
}
