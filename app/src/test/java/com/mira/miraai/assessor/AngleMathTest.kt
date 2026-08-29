package com.mira.miraai.assessor

import org.junit.Assert.assertEquals
import org.junit.Test

class AngleMathTest {

    @Test
    fun straightLineReadsAs180Degrees() {
        val angle = angleDegrees(Point2D(0f, 0f), Point2D(1f, 0f), Point2D(2f, 0f))
        assertEquals(180f, angle, 0.01f)
    }

    @Test
    fun perpendicularRaysReadAs90Degrees() {
        val angle = angleDegrees(Point2D(0f, 1f), Point2D(0f, 0f), Point2D(1f, 0f))
        assertEquals(90f, angle, 0.01f)
    }
}
