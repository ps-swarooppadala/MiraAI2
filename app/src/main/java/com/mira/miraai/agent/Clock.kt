package com.mira.miraai.agent

/** Pure-Kotlin clock seam so CoachAgent is testable without a real timer (CLAUDE.md TDD rule). */
fun interface Clock {
    fun nowMs(): Long
}
