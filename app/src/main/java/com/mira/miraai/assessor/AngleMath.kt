package com.mira.miraai.assessor

import kotlin.math.acos
import kotlin.math.sqrt

/** A plain 2D point — no Android graphics types, so this package stays JUnit-testable. */
data class Point2D(val x: Float, val y: Float)

/** Angle at [vertex] formed by rays toward [a] and [c], in degrees (0..180). */
fun angleDegrees(a: Point2D, vertex: Point2D, c: Point2D): Float {
    val v1x = a.x - vertex.x
    val v1y = a.y - vertex.y
    val v2x = c.x - vertex.x
    val v2y = c.y - vertex.y
    val mag1 = sqrt(v1x * v1x + v1y * v1y)
    val mag2 = sqrt(v2x * v2x + v2y * v2y)
    if (mag1 == 0f || mag2 == 0f) return 0f
    val cos = ((v1x * v2x + v1y * v2y) / (mag1 * mag2)).coerceIn(-1f, 1f)
    return Math.toDegrees(acos(cos).toDouble()).toFloat()
}
