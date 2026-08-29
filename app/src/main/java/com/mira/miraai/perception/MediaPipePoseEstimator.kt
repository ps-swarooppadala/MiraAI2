package com.mira.miraai.perception

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.mira.miraai.assessor.Point2D

private const val TAG = "MiraPoseEstimator"
private const val MODEL_ASSET_PATH = "pose_landmarker_lite.task"

/** Maps the 10 [BodyJoint]s the Assessor layer tracks to BlazePose landmark indices. */
private val JOINT_INDEX: Map<BodyJoint, Int> = mapOf(
    BodyJoint.LEFT_EAR to PoseLandmarkIndex.LEFT_EAR,
    BodyJoint.RIGHT_EAR to PoseLandmarkIndex.RIGHT_EAR,
    BodyJoint.LEFT_SHOULDER to PoseLandmarkIndex.LEFT_SHOULDER,
    BodyJoint.RIGHT_SHOULDER to PoseLandmarkIndex.RIGHT_SHOULDER,
    BodyJoint.LEFT_ELBOW to PoseLandmarkIndex.LEFT_ELBOW,
    BodyJoint.RIGHT_ELBOW to PoseLandmarkIndex.RIGHT_ELBOW,
    BodyJoint.LEFT_WRIST to PoseLandmarkIndex.LEFT_WRIST,
    BodyJoint.RIGHT_WRIST to PoseLandmarkIndex.RIGHT_WRIST,
    BodyJoint.LEFT_HIP to PoseLandmarkIndex.LEFT_HIP,
    BodyJoint.RIGHT_HIP to PoseLandmarkIndex.RIGHT_HIP,
    BodyJoint.LEFT_KNEE to PoseLandmarkIndex.LEFT_KNEE,
    BodyJoint.RIGHT_KNEE to PoseLandmarkIndex.RIGHT_KNEE,
    BodyJoint.LEFT_ANKLE to PoseLandmarkIndex.LEFT_ANKLE,
    BodyJoint.RIGHT_ANKLE to PoseLandmarkIndex.RIGHT_ANKLE,
)

/** Converts MediaPipe's raw result into the pure-Kotlin [PoseFrame] the Assessor consumes. */
private fun PoseLandmarkerResult.toPoseFrame(): PoseFrame {
    val detected = landmarks().firstOrNull() ?: return PoseFrame(emptyMap())
    val landmarkMap = JOINT_INDEX.mapNotNull { (joint, index) ->
        detected.getOrNull(index)?.let { lm ->
            joint to Landmark(Point2D(lm.x(), lm.y()), lm.visibility().orElse(0f))
        }
    }.toMap()
    return PoseFrame(landmarkMap)
}

/**
 * Thin wrapper around MediaPipe's PoseLandmarker task, running LiteRT.
 * [delegate] is supplied by the flavor's DI module — `Delegate.CPU`/`Delegate.GPU` for
 * devPhone, and the NPU-routed delegate for iqoo once wired (build-architecture.md Section 2).
 *
 * Implements [PoseEstimator] per Phase 4's reconciliation of the interface's async contract
 * with MediaPipe's `LIVE_STREAM` running mode (see [PoseEstimator]'s doc comment).
 */
class MediaPipePoseEstimator(
    context: Context,
    private val isFrontCamera: Boolean,
    delegate: Delegate = Delegate.CPU,
) : PoseEstimator {
    private val landmarker: PoseLandmarker

    @Volatile
    private var pendingResultCallback: ((PoseFrame) -> Unit)? = null

    init {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath(MODEL_ASSET_PATH)
            .setDelegate(delegate)
            .build()
        val options = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setMinPoseDetectionConfidence(0.5f)
            .setMinPosePresenceConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .setResultListener { result, _ -> pendingResultCallback?.invoke(result.toPoseFrame()) }
            .setErrorListener { error -> Log.e(TAG, "Pose landmarker error", error) }
            .build()
        landmarker = PoseLandmarker.createFromOptions(context, options)
    }

    /** Consumes and closes [frame]; must be called from ImageAnalysis's analyzer callback. */
    override fun estimate(frame: ImageProxy, onResult: (PoseFrame) -> Unit) {
        pendingResultCallback = onResult
        val frameTimeMs = SystemClock.uptimeMillis()
        val bitmapBuffer = Bitmap.createBitmap(frame.width, frame.height, Bitmap.Config.ARGB_8888)
        frame.use { bitmapBuffer.copyPixelsFromBuffer(it.planes[0].buffer) }

        val matrix = Matrix().apply {
            postRotate(frame.imageInfo.rotationDegrees.toFloat())
            if (isFrontCamera) postScale(-1f, 1f, frame.width.toFloat(), frame.height.toFloat())
        }
        val rotatedBitmap = Bitmap.createBitmap(
            bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height, matrix, true
        )
        val mpImage = BitmapImageBuilder(rotatedBitmap).build()
        landmarker.detectAsync(mpImage, frameTimeMs)
    }

    fun close() {
        landmarker.close()
    }
}
