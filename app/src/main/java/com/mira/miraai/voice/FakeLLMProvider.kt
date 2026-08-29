package com.mira.miraai.voice

import com.mira.miraai.agent.freestyle.AgentPrompt
import com.mira.miraai.agent.freestyle.AgentResponse

/** Fake [LLMProvider] for contract tests — returns [nextResponse], records the last prompt seen. */
class FakeLLMProvider(var nextResponse: AgentResponse) : LLMProvider {
    var lastPromptSeen: AgentPrompt? = null
        private set

    override suspend fun complete(prompt: AgentPrompt): AgentResponse {
        lastPromptSeen = prompt
        return nextResponse
    }
}
