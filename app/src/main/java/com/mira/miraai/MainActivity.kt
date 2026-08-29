package com.mira.miraai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
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
import com.mira.miraai.agent.Clock
import com.mira.miraai.agent.CoachAgent
import com.mira.miraai.agent.CoachIntent
import com.mira.miraai.assessor.WarriorIIAssessor
import com.mira.miraai.capture.CameraXController
import com.mira.miraai.perception.PoseEstimator
import com.mira.miraai.perception.PoseFrame
import com.mira.miraai.perception.Side
import com.mira.miraai.ui.theme.MiraAITheme
import com.mira.miraai.voice.CueTemplates
import com.mira.miraai.voice.SpeechCoach
import org.koin.android.ext.android.inject

// TODO(needs wiring): the routine/step data that should choose the practicing leg doesn't exist
// yet (Phase 5+, Browse IA / Workout Mode). Hardcoded until then — see docs/PROGRESS.md.
private val HARDCODED_FRONT_LEG = Side.LEFT

/**
 * Phase 4: real camera pipeline wired to the Warrior II Assessor (Phase 1) and Coach Agent
 * (Phase 2) via the [PoseEstimator] device-abstraction interface (Phase 3), replacing Phase 0's
 * hardcoded single-joint `ElbowCheck` logic — build-architecture.md Section 7.
 */
class MainActivity : ComponentActivity() {

    private val poseEstimator: PoseEstimator by inject()

    private lateinit var cameraController: CameraXController
    private lateinit var speechCoach: SpeechCoach
    private val assessor = WarriorIIAssessor()
    private val coachAgent = CoachAgent(clock = Clock { SystemClock.uptimeMillis() })

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
        cameraController.start(
            lifecycleOwner = this,
            previewView = previewView,
            onFrame = { imageProxy -> poseEstimator.estimate(imageProxy) { frame -> handlePoseFrame(frame) } },
        )
    }

    private fun handlePoseFrame(frame: PoseFrame) {
        recordFrameForFps()

        val verdict = assessor.assess(frame, HARDCODED_FRONT_LEG)
        val decision = coachAgent.tick(verdict)

        val line = when (decision.intent) {
            CoachIntent.SPEAK_CUE -> CueTemplates.forIssue(decision.verdictCode!!, decision.escalation)
            CoachIntent.CONFIRM_IMPROVEMENT -> CueTemplates.forConfirmImprovement()
            CoachIntent.SAFETY_OVERRIDE -> CueTemplates.forSafetyOverride(decision.verdictCode!!)
            CoachIntent.SILENT -> null
        }
        line?.let { speechCoach.speak(it) }
    }

    private fun recordFrameForFps() {
        frameCountInWindow++
        val now = SystemClock.uptimeMillis()
        if (fpsWindowStartMs == 0L) fpsWindowStartMs = now
        if (now - fpsWindowStartMs >= 1000L) {
            val fps = frameCountInWindow
            runOnUiThread { fpsState.intValue = fps }
            frameCountInWindow = 0
            fpsWindowStartMs = now
        }
    }

    override fun onDestroy() {
        super.onDestroy()
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
                text = "Phase 4 — real pipeline — $fps fps",
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
