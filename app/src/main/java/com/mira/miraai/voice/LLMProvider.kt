package com.mira.miraai.voice

import com.mira.miraai.agent.freestyle.AgentPrompt
import com.mira.miraai.agent.freestyle.AgentResponse

/** Device-abstraction interface per build-architecture.md Section 2. See [PoseEstimator]. */
interface LLMProvider {
    suspend fun complete(prompt: AgentPrompt): AgentResponse
}
