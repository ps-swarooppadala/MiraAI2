package com.mira.miraai.assessor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HoldTimerTest {

    private val timer = HoldTimer(targetHoldSec = 20)
    private val initial = HoldTimerState(elapsedHoldMs = 0L, cleanStreakMs = 0L, isComplete = false)

    @Test
    fun tick_advancesElapsedByDelta_whenCleanAndNotPaused() {
        val next = timer.tick(initial, deltaMs = 500L, isPaused = false, hasCriticalIssue = false)
        assertEquals(500L, next.elapsedHoldMs)
        assertEquals(500L, next.cleanStreakMs)
    }

    @Test
    fun tick_doesNotAdvanceElapsed_whenHasCriticalIssue() {
        val state = initial.copy(elapsedHoldMs = 5_000L, cleanStreakMs = 5_000L)
        val next = timer.tick(state, deltaMs = 500L, isPaused = false, hasCriticalIssue = true)
        assertEquals(5_000L, next.elapsedHoldMs)
    }

    @Test
    fun tick_resetsCleanStreakToZero_whenCriticalIssueOccurs() {
        val state = initial.copy(elapsedHoldMs = 5_000L, cleanStreakMs = 5_000L)
        val next = timer.tick(state, deltaMs = 500L, isPaused = false, hasCriticalIssue = true)
        assertEquals(0L, next.cleanStreakMs)
    }

    @Test
    fun tick_resumesAdvancingAndAccumulatingCleanStreak_afterCriticalIssueClears() {
        val duringIssue = timer.tick(initial.copy(elapsedHoldMs = 5_000L), deltaMs = 500L, isPaused = false, hasCriticalIssue = true)
        val afterClears = timer.tick(duringIssue, deltaMs = 300L, isPaused = false, hasCriticalIssue = false)
        assertEquals(5_300L, afterClears.elapsedHoldMs)
        assertEquals(300L, afterClears.cleanStreakMs)
    }

    @Test
    fun tick_leavesStateUnchanged_whenPaused_regardlessOfCriticalIssueFlag() {
        val state = initial.copy(elapsedHoldMs = 5_000L, cleanStreakMs = 3_000L)
        val next = timer.tick(state, deltaMs = 500L, isPaused = true, hasCriticalIssue = true)
        assertEquals(state, next)
    }

    @Test
    fun isComplete_isFalse_whenElapsedBelowTarget_evenIfEntirelyClean() {
        val state = initial.copy(elapsedHoldMs = 19_000L, cleanStreakMs = 19_000L)
        val next = timer.tick(state, deltaMs = 500L, isPaused = false, hasCriticalIssue = false)
        assertFalse(next.isComplete)
    }

    @Test
    fun isComplete_isTrue_whenElapsedMeetsTargetAndLastTwoSecondsClean() {
        val state = initial.copy(elapsedHoldMs = 19_800L, cleanStreakMs = 3_000L)
        val next = timer.tick(state, deltaMs = 500L, isPaused = false, hasCriticalIssue = false)
        assertTrue(next.isComplete)
    }

    @Test
    fun isComplete_isFalse_whenElapsedMeetsTarget_butCleanStreakUnderTwoSeconds() {
        val state = initial.copy(elapsedHoldMs = 20_500L, cleanStreakMs = 1_000L)
        val next = timer.tick(state, deltaMs = 500L, isPaused = false, hasCriticalIssue = false)
        assertFalse(next.isComplete)
    }

    @Test
    fun isComplete_becomesTrue_onceCleanStreakRebuildsPastTwoSeconds_afterLateCriticalIssue() {
        var state = initial.copy(elapsedHoldMs = 21_000L, cleanStreakMs = 0L)
        state = timer.tick(state, deltaMs = 1_000L, isPaused = false, hasCriticalIssue = false)
        assertFalse(state.isComplete)
        state = timer.tick(state, deltaMs = 1_100L, isPaused = false, hasCriticalIssue = false)
        assertTrue(state.isComplete)
    }
}
