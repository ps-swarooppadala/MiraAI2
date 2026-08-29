package com.mira.miraai.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val GOOD = WorkoutThresholds.FRAMING_CONFIDENCE_THRESHOLD + 0.1f
private const val BAD = WorkoutThresholds.FRAMING_CONFIDENCE_THRESHOLD - 0.1f
private const val SUSTAIN_MS = WorkoutThresholds.FRAMING_SUSTAIN_MS

class FramingGateTest {

    private val gate = FramingGate()

    @Test
    fun belowThreshold_neverReady() {
        val state = gate.tick(FramingGateState(), BAD, SUSTAIN_MS * 2)
        assertFalse(state.isReady)
        assertEquals(0L, state.sustainedGoodMs)
    }

    @Test
    fun goodFrame_accumulatesSustainedTime() {
        val state = gate.tick(FramingGateState(), GOOD, SUSTAIN_MS / 2)
        assertEquals(SUSTAIN_MS / 2, state.sustainedGoodMs)
        assertFalse(state.isReady)
    }

    @Test
    fun goodFrame_forFullSustainWindow_becomesReady() {
        var state = FramingGateState()
        state = gate.tick(state, GOOD, SUSTAIN_MS / 2)
        state = gate.tick(state, GOOD, SUSTAIN_MS / 2)
        assertTrue(state.isReady)
    }

    @Test
    fun badFrame_afterGoodStreak_resetsSustainedTime() {
        var state = FramingGateState()
        state = gate.tick(state, GOOD, SUSTAIN_MS / 2)
        state = gate.tick(state, BAD, 100L)
        assertEquals(0L, state.sustainedGoodMs)
        assertFalse(state.isReady)
    }
}
