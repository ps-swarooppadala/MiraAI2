package com.mira.miraai.perception

import androidx.camera.core.ImageProxy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.mockito.kotlin.mock

class FakePoseEstimatorTest {

    @Test
    fun `returns the configured frame regardless of input`() {
        val frame = PoseFrame(mapOf(BodyJoint.LEFT_HIP to Landmark(com.mira.miraai.assessor.Point2D(0f, 0f), 1f)))
        val estimator: PoseEstimator = FakePoseEstimator(frame)
        val image: ImageProxy = mock()
        var result: PoseFrame? = null

        estimator.estimate(image) { result = it }

        assertEquals(frame, result)
    }

    @Test
    fun `records the last frame passed in`() {
        val fake = FakePoseEstimator()
        val image: ImageProxy = mock()

        fake.estimate(image) {}

        assertSame(image, fake.lastFrameSeen)
    }
}
