package com.mira.miraai.agent

/** Progress of the Framing Assistant's confidence gate — feature-spec.md US-5. */
data class FramingGateState(
    val sustainedGoodMs: Long = 0L,
    val isReady: Boolean = false,
)

/**
 * Pre-coaching confidence gate (US-5): "auto-advances when green for 1 sustained second."
 * Below [WorkoutThresholdsConfig.framingConfidenceThreshold], the sustained-good streak resets
 * to zero every frame — the UI reads that as the "Fix your framing" guided-fix variant.
 */
class FramingGate(private val config: WorkoutThresholdsConfig = WorkoutThresholdsConfig()) {

    fun tick(state: FramingGateState, confidence: Float, deltaMs: Long): FramingGateState {
        val isGoodFrame = confidence >= config.framingConfidenceThreshold
        val sustained = if (isGoodFrame) state.sustainedGoodMs + deltaMs else 0L
        return FramingGateState(sustained, sustained >= config.framingSustainMs)
    }
}
