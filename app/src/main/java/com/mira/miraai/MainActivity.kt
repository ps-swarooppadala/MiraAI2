package com.mira.miraai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.mira.miraai.assessor.ElbowCheck
import com.mira.miraai.assessor.Point2D
import com.mira.miraai.capture.CameraXController
import com.mira.miraai.perception.MediaPipePoseEstimator
import com.mira.miraai.perception.PoseLandmarkIndex
import com.mira.miraai.ui.theme.MiraAITheme
import com.mira.miraai.voice.SpeechCoach

private const val TAG = "MiraPhase0"
private const val COACH_LINE = "Straighten your arm."

// needs tuning — placeholder, gates which landmarks are trusted for the angle check (see docs/PROGRESS.md)
private const val LANDMARK_VISIBILITY_THRESHOLD = 0.5f

/**
 * Phase 0 walking skeleton: CameraX -> MediaPipe PoseLandmarker (CPU delegate) -> one
 * hardcoded right-elbow angle check -> one hardcoded line via system TextToSpeech.
 * No state machine, no rule engine, no other poses — see docs/build-architecture.md Section 7.
 */
class MainActivity : ComponentActivity() {

    private lateinit var cameraController: CameraXController
    private lateinit var speechCoach: SpeechCoach
    private var poseEstimator: MediaPipePoseEstimator? = null

    private var frameCountInWindow = 0
    private var fpsWindowStartMs = 0L
    private val fpsState = mutableIntStateOf(0)
    private val hasPermissionState = mutableStateOf(false)

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasPermissionState.value = granted
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        cameraController = CameraXController(this)
        speechCoach = SpeechCoach(this)

        hasPermissionState.value = ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        setContent {
            MiraAITheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Phase0Screen(
                        hasPermission = hasPermissionState.value,
                        fps = fpsState.intValue,
                        onGrantPermission = { requestPermissionLauncher.launch(Manifest.permission.CAMERA) },
                        onPreviewViewReady = { previewView -> bindCameraAndPipeline(previewView) },
                    )
                }
            }
        }
    }

    private fun bindCameraAndPipeline(previewView: PreviewView) {
        poseEstimator = MediaPipePoseEstimator(
            context = this,
            isFrontCamera = true,
            onResult = { result -> handlePoseResult(result) },
            onError = { message -> Log.e(TAG, "Pose estimator error: $message") },
        )
        cameraController.start(
            lifecycleOwner = this,
            previewView = previewView,
            onFrame = { imageProxy -> poseEstimator?.detect(imageProxy) },
        )
    }

    private fun handlePoseResult(result: PoseLandmarkerResult) {
        recordFrameForFps()

        val landmarks = result.landmarks().firstOrNull() ?: return
        val shoulder = landmarks.getOrNull(PoseLandmarkIndex.RIGHT_SHOULDER) ?: return
        val elbow = landmarks.getOrNull(PoseLandmarkIndex.RIGHT_ELBOW) ?: return
        val wrist = landmarks.getOrNull(PoseLandmarkIndex.RIGHT_WRIST) ?: return

        val allVisible = listOf(shoulder, elbow, wrist).all {
            it.visibility().orElse(0f) >= LANDMARK_VISIBILITY_THRESHOLD
        }
        if (!allVisible) return

        val isBent = ElbowCheck.isRightArmBent(
            Point2D(shoulder.x(), shoulder.y()),
            Point2D(elbow.x(), elbow.y()),
            Point2D(wrist.x(), wrist.y()),
        )
        if (isBent) {
            speechCoach.speak(COACH_LINE)
        }
    }

    private fun recordFrameForFps() {
        frameCountInWindow++
        val now = SystemClock.uptimeMillis()
        if (fpsWindowStartMs == 0L) fpsWindowStartMs = now
        if (now - fpsWindowStartMs >= 1000L) {
            val fps = frameCountInWindow
            Log.i(TAG, "sustained fps: $fps")
            runOnUiThread { fpsState.intValue = fps }
            frameCountInWindow = 0
            fpsWindowStartMs = now
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        poseEstimator?.close()
        speechCoach.shutdown()
        cameraController.shutdown()
    }
}

@Composable
private fun Phase0Screen(
    hasPermission: Boolean,
    fps: Int,
    onGrantPermission: () -> Unit,
    onPreviewViewReady: (PreviewView) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (hasPermission) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx -> PreviewView(ctx).also { onPreviewViewReady(it) } },
            )
            Text(
                text = "Phase 0 walking skeleton — $fps fps",
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(8.dp),
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Mira needs camera access to see your pose.")
                Spacer(Modifier.height(16.dp))
                Button(onClick = onGrantPermission) { Text("Grant camera permission") }
            }
        }
    }
}
