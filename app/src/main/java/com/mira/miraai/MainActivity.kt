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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mira.miraai.agent.Clock
import com.mira.miraai.agent.CoachAgent
import com.mira.miraai.agent.CoachAgentThresholds
import com.mira.miraai.agent.CoachDecision
import com.mira.miraai.agent.CoachIntent
import com.mira.miraai.agent.FramingGate
import com.mira.miraai.agent.FramingGateState
import com.mira.miraai.agent.SessionPhase
import com.mira.miraai.agent.SessionSummary
import com.mira.miraai.agent.WorkoutSessionEngine
import com.mira.miraai.agent.WorkoutSessionState
import com.mira.miraai.assessor.WarriorIIAssessor
import com.mira.miraai.assessor.WarriorIIVerdict
import com.mira.miraai.capture.CameraXController
import com.mira.miraai.content.ContentRepository
import com.mira.miraai.content.loadContentRepositoryFromAssets
import com.mira.miraai.agent.ExpandedStep
import com.mira.miraai.perception.BodyJoint
import com.mira.miraai.perception.PoseEstimator
import com.mira.miraai.perception.PoseFrame
import com.mira.miraai.perception.Side
import com.mira.miraai.ui.category.CategoryBrowseScreen
import com.mira.miraai.ui.framing.FramingAssistantScreen
import com.mira.miraai.ui.home.HomeScreen
import com.mira.miraai.ui.player.WorkoutPlayerScreen
import com.mira.miraai.ui.routinedetail.RoutineDetailScreen
import com.mira.miraai.ui.setup.SetupTipsScreen
import com.mira.miraai.ui.summary.SessionSummaryScreen
import com.mira.miraai.ui.theme.MiraAITheme
import com.mira.miraai.voice.CueTemplates
import com.mira.miraai.voice.Lang
import com.mira.miraai.voice.TTSProvider
import com.mira.miraai.voice.TTSShutdown
import org.koin.android.ext.android.inject

/**
 * Navigation routes.
 *
 * Phase 6 replaces the Phase 5-addendum stub routes (which toasted instead of navigating past
 * Setup Tips) with the real Setup Tips -> Framing Assistant -> Player -> Summary chain per
 * feature-spec.md Section 3's IA diagram. The temporary camera-demo route/button from Phase 5
 * is removed now that Framing/Player give the app a real entry into the coaching flow.
 */
private object Routes {
    const val HOME = "home"
    const val CATEGORY = "category/{categoryId}"
    const val ROUTINE_DETAIL = "routineDetail/{routineId}"
    const val SETUP_TIPS = "setupTips/{routineId}"
    const val FRAMING = "framing/{routineId}"
    const val PLAYER = "player"
    const val SUMMARY = "summary"

    fun category(id: String) = "category/$id"
    fun routineDetail(id: String) = "routineDetail/$id"
    fun setupTips(id: String) = "setupTips/$id"
    fun framing(id: String) = "framing/$id"
}

/**
 * Phase 6: Framing Assistant (US-5), Workout Mode Player (US-6/Section 8), and Session Summary
 * (US-7), wired to the Phase 1 [WarriorIIAssessor] and Phase 2 [CoachAgent] via the Phase 3/4
 * [PoseEstimator] device abstraction. [WorkoutSessionEngine] (pure Kotlin, unit-tested) owns the
 * routine-sequencing/step-phase FSM; this Activity is the one Android-coupled place that wires a
 * live camera frame stream into it, mirroring Phase 4's `handlePoseFrame` pattern but per-screen
 * (Framing needs only a confidence signal; the Player needs the full Assessor+CoachAgent tick).
 */
class MainActivity : ComponentActivity() {

    private val poseEstimator: PoseEstimator by inject()
    private val ttsProvider: TTSProvider by inject()

    private lateinit var cameraController: CameraXController
    private lateinit var contentRepository: ContentRepository
    private val assessor = WarriorIIAssessor()
    private var coachAgent = CoachAgent(clock = Clock { SystemClock.uptimeMillis() })
    private val framingGate = FramingGate()

    private val hasPermissionState = mutableStateOf(false)

    // --- Framing Assistant state ---
    private var framingGateState = FramingGateState()
    private var framingLastFrameMs = 0L
    private val framingConfidenceState = mutableFloatStateOf(0f)
    private val framingReadyState = mutableStateOf(false)
    private val framingPoseFrameState = mutableStateOf<PoseFrame?>(null)

    // --- Workout Mode Player state ---
    private var engine: WorkoutSessionEngine? = null
    private var playerLastFrameMs = 0L
    private val playerUiState = mutableStateOf<WorkoutSessionState?>(null)
    private val cueLineState = mutableStateOf<String?>(null)
    private val sessionCompleteState = mutableStateOf(false)
    private var pendingSummary: SessionSummary? = null
    private val playerPoseFrameState = mutableStateOf<PoseFrame?>(null)
    private val playerAngleDegState = mutableFloatStateOf(Float.NaN)

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasPermissionState.value = granted
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        cameraController = CameraXController(this)
        contentRepository = loadContentRepositoryFromAssets(this)

        hasPermissionState.value = ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        setContent {
            MiraAITheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    MiraNavHost(
                        activity = this,
                        navController = navController,
                        contentRepository = contentRepository,
                        hasPermission = hasPermissionState.value,
                        onGrantPermission = { requestPermissionLauncher.launch(Manifest.permission.CAMERA) },
                    )
                }
            }
        }
    }

    fun resetFramingGate() {
        framingGateState = FramingGateState()
        framingLastFrameMs = 0L
        framingConfidenceState.floatValue = 0f
        framingReadyState.value = false
    }

    fun framingConfidence() = framingConfidenceState.floatValue
    fun framingReady() = framingReadyState.value
    fun framingPoseFrame(): PoseFrame? = framingPoseFrameState.value

    fun bindCameraForFraming(previewView: PreviewView) {
        cameraController.start(
            lifecycleOwner = this,
            previewView = previewView,
            onFrame = { imageProxy -> poseEstimator.estimate(imageProxy) { frame -> handleFramingFrame(frame) } },
        )
    }

    private fun handleFramingFrame(frame: PoseFrame) {
        val now = SystemClock.uptimeMillis()
        val deltaMs = if (framingLastFrameMs == 0L) 0L else now - framingLastFrameMs
        framingLastFrameMs = now

        val visibilities = frame.landmarks.values.map { it.visibility }
        val confidence = if (visibilities.isEmpty()) 0f else visibilities.average().toFloat()
        framingGateState = framingGate.tick(framingGateState, confidence, deltaMs)

        runOnUiThread {
            framingConfidenceState.floatValue = confidence
            framingReadyState.value = framingGateState.isReady
            framingPoseFrameState.value = frame
        }
    }

    fun startWorkout(routine: com.mira.miraai.content.Routine) {
        engine = WorkoutSessionEngine(routine)
        coachAgent = CoachAgent(clock = Clock { SystemClock.uptimeMillis() })
        playerLastFrameMs = 0L
        sessionCompleteState.value = false
        cueLineState.value = null
        playerUiState.value = null
        playerPoseFrameState.value = null
        playerAngleDegState.floatValue = Float.NaN
    }

    fun playerState(): WorkoutSessionState? = playerUiState.value
    fun playerCueLine(): String? = cueLineState.value
    fun sessionComplete() = sessionCompleteState.value
    fun currentEngine(): WorkoutSessionEngine? = engine
    fun playerPoseFrame(): PoseFrame? = playerPoseFrameState.value
    fun playerAngleDeg(): Float? = playerAngleDegState.floatValue.takeUnless { it.isNaN() }

    fun bindCameraForPlayer(previewView: PreviewView) {
        cameraController.start(
            lifecycleOwner = this,
            previewView = previewView,
            onFrame = { imageProxy -> poseEstimator.estimate(imageProxy) { frame -> handlePlayerFrame(frame) } },
        )
    }

    private fun handlePlayerFrame(frame: PoseFrame) {
        val activeEngine = engine ?: return
        val now = SystemClock.uptimeMillis()
        val deltaMs = if (playerLastFrameMs == 0L) 0L else now - playerLastFrameMs
        playerLastFrameMs = now

        val step = activeEngine.currentStep()
        val isWarriorII = step?.poseId == "warrior_ii" && step.side != null
        val verdict: WarriorIIVerdict? = if (isWarriorII) assessor.assess(frame, step!!.side!!) else null
        val angleDeg: Float? = if (isWarriorII) assessor.frontKneeAngleDeg(frame, step!!.side!!) else null
        val decision: CoachDecision? = verdict?.let { coachAgent.tick(it) }

        val newState = activeEngine.tick(deltaMs, verdict, decision)

        val line = when (decision?.intent) {
            CoachIntent.SPEAK_CUE -> CueTemplates.forIssue(decision.verdictCode!!, decision.escalation)
            CoachIntent.CONFIRM_IMPROVEMENT -> CueTemplates.forConfirmImprovement()
            CoachIntent.SAFETY_OVERRIDE -> CueTemplates.forSafetyOverride(decision.verdictCode!!)
            CoachIntent.SILENT, null -> null
        }
        line?.let { ttsProvider.speak(it, Lang.EN) }

        runOnUiThread {
            playerUiState.value = newState
            playerPoseFrameState.value = frame
            playerAngleDegState.floatValue = angleDeg ?: Float.NaN
            if (line != null) cueLineState.value = line
            if (newState.phase == SessionPhase.SUMMARY) {
                pendingSummary = activeEngine.buildSummary()
                sessionCompleteState.value = true
            }
        }
    }

    fun togglePause() {
        val activeEngine = engine ?: return
        val isPaused = playerUiState.value?.isPaused ?: false
        if (isPaused) {
            activeEngine.resume()
            coachAgent.resume()
        } else {
            activeEngine.pause()
            coachAgent.pause()
        }
    }

    fun endWorkoutEarly(): SessionSummary {
        val summary = engine?.endEarly() ?: SessionSummary(0L, emptyList(), null)
        pendingSummary = summary
        return summary
    }

    fun pendingSummary(): SessionSummary = pendingSummary ?: SessionSummary(0L, emptyList(), null)

    override fun onDestroy() {
        super.onDestroy()
        (ttsProvider as? TTSShutdown)?.shutdown()
        cameraController.shutdown()
    }
}

@Composable
private fun MiraNavHost(
    activity: MainActivity,
    navController: NavHostController,
    contentRepository: ContentRepository,
    hasPermission: Boolean,
    onGrantPermission: () -> Unit,
) {
    val context = LocalContext.current

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
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
        }

        composable(Routes.CATEGORY) { backStackEntry ->
            val category = contentRepository.categoryById(requireArg(backStackEntry, "categoryId"))
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
            val routine = contentRepository.routineById(requireArg(backStackEntry, "routineId"))
            if (routine != null) {
                RoutineDetailScreen(
                    routine = routine,
                    posesById = contentRepository.poses.associateBy { it.id },
                    onBackClick = { navController.popBackStack() },
                    onStartWorkoutClick = { navController.navigate(Routes.setupTips(routine.id)) },
                )
            }
        }

        composable(Routes.SETUP_TIPS) { backStackEntry ->
            val routineId = requireArg(backStackEntry, "routineId")
            SetupTipsScreen(
                onBackClick = { navController.popBackStack() },
                onReadyClick = {
                    activity.resetFramingGate()
                    navController.navigate(Routes.framing(routineId))
                },
            )
        }

        composable(Routes.FRAMING) { backStackEntry ->
            val routineId = requireArg(backStackEntry, "routineId")
            val routine = contentRepository.routineById(routineId)
            var hasAdvanced by remember(routineId) { mutableStateOf(false) }
            val confidence = activity.framingConfidence()
            val isReady = activity.framingReady()

            LaunchedEffect(isReady) {
                if (isReady && !hasAdvanced && routine != null) {
                    hasAdvanced = true
                    activity.startWorkout(routine)
                    navController.navigate(Routes.PLAYER) { popUpTo(Routes.HOME) }
                }
            }

            FramingAssistantScreen(
                hasPermission = hasPermission,
                confidence = confidence,
                poseFrame = activity.framingPoseFrame(),
                onGrantPermission = onGrantPermission,
                onPreviewViewReady = { previewView -> activity.bindCameraForFraming(previewView) },
                onEndSession = { navController.popBackStack(Routes.HOME, inclusive = false) },
            )
        }

        composable(Routes.PLAYER) {
            val engine = activity.currentEngine()
            val state = activity.playerState()
            val cueLine = activity.playerCueLine()
            val sessionComplete = activity.sessionComplete()

            LaunchedEffect(sessionComplete) {
                if (sessionComplete) {
                    navController.navigate(Routes.SUMMARY) { popUpTo(Routes.HOME) }
                }
            }

            if (engine == null) {
                navController.popBackStack(Routes.HOME, inclusive = false)
            } else {
                val step = engine.currentStep()
                val pose = contentRepository.poseById(step?.poseId.orEmpty())
                WorkoutPlayerScreen(
                    hasPermission = hasPermission,
                    routineTitle = engine.routine.title,
                    stepNumber = (state?.currentStepIndex ?: 0) + 1,
                    totalSteps = engine.steps.size,
                    poseDisplayName = pose?.displayName ?: step?.poseId.orEmpty(),
                    sideLabel = step?.side?.let { if (it == Side.LEFT) "Left side" else "Right side" },
                    targetHoldSec = step?.targetHoldSec ?: 0,
                    elapsedHoldSec = state?.elapsedHoldSec ?: 0,
                    confidenceScore = state?.confidenceScore ?: 1f,
                    isPaused = state?.isPaused ?: false,
                    isResting = state?.phase == SessionPhase.REST,
                    restLabel = "Switch sides",
                    cueCaption = cueLine,
                    showConfidenceRecoveryBanner = (state?.confidenceScore ?: 1f) < CoachAgentThresholds.MIN_CONFIDENCE_TO_COACH,
                    poseFrame = activity.playerPoseFrame(),
                    highlightJoint = frontKneeJointFor(step),
                    currentAngleDeg = activity.playerAngleDeg(),
                    onGrantPermission = onGrantPermission,
                    onPreviewViewReady = { previewView -> activity.bindCameraForPlayer(previewView) },
                    onPauseToggle = { activity.togglePause() },
                    onEndWorkoutClick = {
                        activity.endWorkoutEarly()
                        navController.navigate(Routes.SUMMARY) { popUpTo(Routes.HOME) }
                    },
                )
            }
        }

        composable(Routes.SUMMARY) {
            val summary = activity.pendingSummary()
            val engine = activity.currentEngine()
            SessionSummaryScreen(
                routineTitle = engine?.routine?.title ?: "",
                totalElapsedMs = summary.totalElapsedMs,
                stepResults = summary.stepResults,
                posesById = contentRepository.poses.associateBy { it.id },
                nextFocusVerdict = summary.nextFocusVerdict,
                onDoneClick = { navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } } },
                onSeeWhatMiraLearnedClick = null, // Memory Graph is Phase 10 — no-op per this phase's instruction.
            )
        }
    }
}

private fun requireArg(backStackEntry: androidx.navigation.NavBackStackEntry, key: String): String =
    backStackEntry.arguments?.getString(key).orEmpty()

/** The joint the overlay highlights while a step is being live-assessed — only Warrior II's front knee today. */
private fun frontKneeJointFor(step: ExpandedStep?): BodyJoint? {
    if (step?.poseId != "warrior_ii") return null
    return when (step.side) {
        Side.LEFT -> BodyJoint.LEFT_KNEE
        Side.RIGHT -> BodyJoint.RIGHT_KNEE
        null -> null
    }
}
