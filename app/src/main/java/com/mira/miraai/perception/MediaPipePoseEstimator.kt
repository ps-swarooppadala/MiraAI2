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

private const val TAG = "MiraPoseEstimator"
private const val MODEL_ASSET_PATH = "pose_landmarker_lite.task"

/**
 * Thin wrapper around MediaPipe's PoseLandmarker task, running LiteRT.
 * [delegate] is supplied by the flavor's DI module — `Delegate.CPU`/`Delegate.GPU` for
 * devPhone, and the NPU-routed delegate for iqoo once wired (build-architecture.md Section 2).
 *
 * **Known deviation (flagged, not silently patched — see docs/PROGRESS.md):** this class does
 * not implement the [PoseEstimator] interface. The interface's contract
 * (`estimate(frame): PoseFrame`, synchronous) doesn't fit MediaPipe's `LIVE_STREAM` running
 * mode this class uses (async, callback-based, needed for Phase 0's measured 25-31 FPS on
 * devPhone). Reconciling the two — either an async `PoseEstimator` contract or a synchronous
 * adapter — is Phase 4's job when the camera pipeline is rewired onto the Assessor/Coach Agent,
 * not Phase 3's.
 */
class MediaPipePoseEstimator(
    context: Context,
    private val isFrontCamera: Boolean,
    delegate: Delegate = Delegate.CPU,
    private val onResult: (PoseLandmarkerResult) -> Unit,
    private val onError: (String) -> Unit,
) {
    private val landmarker: PoseLandmarker

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
            .setResultListener { result, _ -> onResult(result) }
            .setErrorListener { error ->
                Log.e(TAG, "Pose landmarker error", error)
                onError(error.message ?: "unknown pose landmarker error")
            }
            .build()
        landmarker = PoseLandmarker.createFromOptions(context, options)
    }

    /** Consumes and closes [imageProxy]; must be called from ImageAnalysis's analyzer callback. */
    fun detect(imageProxy: ImageProxy) {
        val frameTimeMs = SystemClock.uptimeMillis()
        val bitmapBuffer = Bitmap.createBitmap(imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888)
        imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(it.planes[0].buffer) }

        val matrix = Matrix().apply {
            postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
            if (isFrontCamera) postScale(-1f, 1f, imageProxy.width.toFloat(), imageProxy.height.toFloat())
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
