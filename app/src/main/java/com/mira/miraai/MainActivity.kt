package com.mira.miraai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mira.miraai.agent.Clock
import com.mira.miraai.agent.CoachAgent
import com.mira.miraai.agent.CoachIntent
import com.mira.miraai.assessor.WarriorIIAssessor
import com.mira.miraai.capture.CameraXController
import com.mira.miraai.content.ContentRepository
import com.mira.miraai.content.loadContentRepositoryFromAssets
import com.mira.miraai.perception.PoseEstimator
import com.mira.miraai.perception.PoseFrame
import com.mira.miraai.perception.Side
import com.mira.miraai.ui.category.CategoryBrowseScreen
import com.mira.miraai.ui.home.HomeScreen
import com.mira.miraai.ui.routinedetail.RoutineDetailScreen
import com.mira.miraai.ui.setup.SetupTipsScreen
import com.mira.miraai.ui.theme.MiraAITheme
import com.mira.miraai.voice.CueTemplates
import com.mira.miraai.voice.SpeechCoach
import org.koin.android.ext.android.inject

// TODO(needs wiring): the routine/step data that should choose the practicing leg doesn't exist
// yet (Phase 5+, Browse IA / Workout Mode). Hardcoded until then — see docs/PROGRESS.md.
private val HARDCODED_FRONT_LEG = Side.LEFT

/**
 * Navigation routes.
 *
 * TEMPORARY (Phase 5 on-device review only — see docs/PROGRESS.md): this NavHost only wires
 * Home -> Category Browse -> Routine Detail -> Setup Tips, plus a scaffolding-only route into
 * the Phase 4 camera-pipeline demo. It stops at Setup Tips on purpose — the real
 * Setup Tips -> Framing Assistant -> Player -> Summary chain, Language Selection, and the
 * "hasSeenSetupTips" skip flag are all Phase 6+ per build-architecture.md Section 7's phase
 * table. This whole NavHost is expected to be replaced (not extended in place) once that real
 * graph exists.
 */
private object Routes {
    const val HOME = "home"
    const val CATEGORY = "category/{categoryId}"
    const val ROUTINE_DETAIL = "routineDetail/{routineId}"
    const val SETUP_TIPS = "setupTips"
    const val CAMERA_DEMO_TEMP = "cameraDemoTemp"

    fun category(id: String) = "category/$id"
    fun routineDetail(id: String) = "routineDetail/$id"
}

/**
 * Phase 4: real camera pipeline wired to the Warrior II Assessor (Phase 1) and Coach Agent
 * (Phase 2) via the [PoseEstimator] device-abstraction interface (Phase 3). Phase 5 added the
 * content-backed Browse screens; this Activity now boots into Home and reaches the camera
 * pipeline only via the temporary debug route described in [Routes] above.
 */
class MainActivity : ComponentActivity() {

    private val poseEstimator: PoseEstimator by inject()

    private lateinit var cameraController: CameraXController
    private lateinit var speechCoach: SpeechCoach
    private lateinit var contentRepository: ContentRepository
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
        contentRepository = loadContentRepositoryFromAssets(this)

        hasPermissionState.value = ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        setContent {
            MiraAITheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    MiraNavHost(
                        navController = navController,
                        contentRepository = contentRepository,
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
private fun MiraNavHost(
    navController: NavHostController,
    contentRepository: ContentRepository,
    hasPermission: Boolean,
    fps: Int,
    onGrantPermission: () -> Unit,
    onPreviewViewReady: (PreviewView) -> Unit,
) {
    val context = LocalContext.current

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            Box(modifier = Modifier.fillMaxSize()) {
                HomeScreen(
                    greeting = "Good evening —\nready to unwind?",
                    categories = contentRepository.categories,
                    recommendedRoutines = contentRepository.routines,
                    // No session history yet (Room lands in Phase 9) — see docs/PROGRESS.md.
                    lastRoutine = null,
                    onFreestyleClick = {
                        Toast.makeText(context, "Freestyle isn't built yet — Phase 8", Toast.LENGTH_SHORT).show()
                    },
                    onContinueClick = { routine -> navController.navigate(Routes.routineDetail(routine.id)) },
                    onCategoryClick = { category -> navController.navigate(Routes.category(category.id)) },
                    onRoutineClick = { routine -> navController.navigate(Routes.routineDetail(routine.id)) },
                )
                TempCameraDemoButton(
                    onClick = { navController.navigate(Routes.CAMERA_DEMO_TEMP) },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
                )
            }
        }

        composable(Routes.CATEGORY) { backStackEntry ->
            val category = contentRepository.categoryById(requireCategoryId(backStackEntry))
            if (category != null) {
                CategoryBrowseScreen(
                    category = category,
                    routines = contentRepository.routinesForCategory(category.id),
                    onBackClick = { navController.popBackStack() },
                    onRoutineClick = { routine -> navController.navigate(Routes.routineDetail(routine.id)) },
                )
            }
        }

        composable(Routes.ROUTINE_DETAIL) { backStackEntry ->
            val routine = contentRepository.routineById(requireRoutineId(backStackEntry))
            if (routine != null) {
                RoutineDetailScreen(
                    routine = routine,
                    posesById = contentRepository.poses.associateBy { it.id },
                    onBackClick = { navController.popBackStack() },
                    onStartWorkoutClick = { navController.navigate(Routes.SETUP_TIPS) },
                )
            }
        }

        composable(Routes.SETUP_TIPS) {
            SetupTipsScreen(
                onBackClick = { navController.popBackStack() },
                onReadyClick = {
                    Toast.makeText(
                        context,
                        "Framing Assistant + Player aren't built yet — Phase 6",
                        Toast.LENGTH_SHORT,
                    ).show()
                },
            )
        }

        composable(Routes.CAMERA_DEMO_TEMP) {
            Phase0Screen(
                hasPermission = hasPermission,
                fps = fps,
                onGrantPermission = onGrantPermission,
                onPreviewViewReady = onPreviewViewReady,
            )
        }
    }
}

private fun requireCategoryId(backStackEntry: androidx.navigation.NavBackStackEntry): String =
    backStackEntry.arguments?.getString("categoryId").orEmpty()

private fun requireRoutineId(backStackEntry: androidx.navigation.NavBackStackEntry): String =
    backStackEntry.arguments?.getString("routineId").orEmpty()

/**
 * TEMPORARY scaffolding (see [Routes] doc comment) — deliberately unstyled (plain Material
 * defaults, not [com.mira.miraai.ui.theme.MiraColors]) so it reads as debug tooling, not a
 * real product surface. Delete this composable and its call site once Phase 6 gives the app a
 * real entry into the coaching flow and this stops being the only way to see the camera demo.
 */
@Composable
private fun TempCameraDemoButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White),
        modifier = modifier,
    ) {
        Text("Camera Demo (temp)")
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
