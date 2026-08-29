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
import com.mira.miraai.assessor.VerdictCode
import com.mira.miraai.assessor.WarriorIIVerdict
import com.mira.miraai.capture.CameraXController
import com.mira.miraai.content.ContentRepository
import com.mira.miraai.content.loadContentRepositoryFromAssets
import com.mira.miraai.agent.ExpandedStep
import com.mira.miraai.agent.freestyle.ActionSchema
import com.mira.miraai.agent.freestyle.AgentResponse
import com.mira.miraai.agent.freestyle.FreestyleHarness
import com.mira.miraai.agent.freestyle.SessionContext
import com.mira.miraai.content.Routine
import com.mira.miraai.data.FactRepository
import com.mira.miraai.memory.Fact
import com.mira.miraai.perception.BodyJoint
import com.mira.miraai.perception.PoseEstimator
import com.mira.miraai.perception.PoseFrame
import com.mira.miraai.perception.Side
import com.mira.miraai.ui.category.CategoryBrowseScreen
import com.mira.miraai.ui.framing.FramingAssistantScreen
import com.mira.miraai.ui.freestyle.FreestyleScreen
import com.mira.miraai.ui.freestyle.OrbState
import com.mira.miraai.ui.home.HomeScreen
import com.mira.miraai.ui.memorygraph.MemoryGraphScreen
import com.mira.miraai.ui.player.WorkoutPlayerScreen
import com.mira.miraai.ui.routinedetail.RoutineDetailScreen
import com.mira.miraai.ui.setup.SetupTipsScreen
import com.mira.miraai.ui.summary.SessionSummaryScreen
import com.mira.miraai.ui.theme.MiraAITheme
import com.mira.miraai.voice.CueTemplates
import com.mira.miraai.voice.Lang
import com.mira.miraai.voice.LLMProvider
import com.mira.miraai.voice.STTProvider
import com.mira.miraai.voice.TTSProvider
import com.mira.miraai.voice.TTSShutdown
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
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
    const val FREESTYLE = "freestyle"
    const val MEMORY_GRAPH = "memoryGraph"

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
    private val llmProvider: LLMProvider by inject()
    private val sttProvider: STTProvider by inject()
    private val factRepository: FactRepository by inject()

    private lateinit var cameraController: CameraXController
    private lateinit var contentRepository: ContentRepository
    private val assessor = WarriorIIAssessor()
    private var coachAgent = CoachAgent(clock = Clock { SystemClock.uptimeMillis() })
    private val framingGate = FramingGate()
    private val freestyleHarness by lazy { FreestyleHarness(llmProvider) }

    private val hasPermissionState = mutableStateOf(false)

    // --- Freestyle Conversation state (US-8) ---
    private val freestyleCaptionState = mutableStateOf("")
    private val freestyleOrbState = mutableStateOf(OrbState.GREETING)
    private val freestyleMicMutedState = mutableStateOf(false)
    private val freestyleResolvedRoutineState = mutableStateOf<Routine?>(null)

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
    private val playerIsGoodFormState = mutableStateOf(false)

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
        playerIsGoodFormState.value = false
    }

    fun playerState(): WorkoutSessionState? = playerUiState.value
    fun playerCueLine(): String? = cueLineState.value
    fun sessionComplete() = sessionCompleteState.value
    fun currentEngine(): WorkoutSessionEngine? = engine
    fun playerPoseFrame(): PoseFrame? = playerPoseFrameState.value
    fun playerAngleDeg(): Float? = playerAngleDegState.floatValue.takeUnless { it.isNaN() }
    fun playerIsGoodForm(): Boolean = playerIsGoodFormState.value

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

        val previousPhase = playerUiState.value?.phase
        val step = activeEngine.currentStep()
        val isWarriorII = step?.poseId == "warrior_ii" && step.side != null
        val verdict: WarriorIIVerdict? = if (isWarriorII) assessor.assess(frame, step!!.side!!) else null
        val angleDeg: Float? = if (isWarriorII) assessor.frontKneeAngleDeg(frame, step!!.side!!) else null
        val decision: CoachDecision? = verdict?.let { coachAgent.tick(it) }

        val newState = activeEngine.tick(deltaMs, verdict, decision)

        val line = when (decision?.intent) {
            CoachIntent.SPEAK_CUE -> CueTemplates.forIssue(decision.verdictCode!!, decision.escalation, decision.repeatIndex)
            CoachIntent.CONFIRM_IMPROVEMENT -> CueTemplates.forConfirmImprovement(decision.repeatIndex)
            CoachIntent.SAFETY_OVERRIDE -> CueTemplates.forSafetyOverride(decision.verdictCode!!, decision.repeatIndex)
            CoachIntent.SILENT, null -> null
        }
        line?.let { ttsProvider.speak(it, Lang.EN) }

        // Just finished a hold clean (Section 8.6 rest transition) — tell the user to relax and
        // what's next, not just show it silently on screen.
        if (previousPhase != SessionPhase.REST && newState.phase == SessionPhase.REST) {
            val nextStep = activeEngine.currentStep()
            val isSwitchingSides = nextStep != null && step != null &&
                nextStep.poseId == step.poseId && nextStep.side != step.side
            ttsProvider.speak(
                CueTemplates.forStepComplete(isSwitchingSides, variantSeed = newState.currentStepIndex),
                Lang.EN,
            )
        }

        val isGoodForm = verdict?.verdictCode == VerdictCode.GOOD_FORM

        runOnUiThread {
            playerUiState.value = newState
            playerPoseFrameState.value = frame
            playerAngleDegState.floatValue = angleDeg ?: Float.NaN
            playerIsGoodFormState.value = isGoodForm
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

    fun factRepository(): FactRepository = factRepository

    // --- Freestyle Conversation (US-8) ---

    fun freestyleCaption(): String = freestyleCaptionState.value
    fun freestyleOrb(): OrbState = freestyleOrbState.value
    fun freestyleMicMuted(): Boolean = freestyleMicMutedState.value
    fun freestyleResolvedRoutine(): Routine? = freestyleResolvedRoutineState.value
    fun clearFreestyleResolvedRoutine() {
        freestyleResolvedRoutineState.value = null
    }

    fun toggleFreestyleMicMute() {
        freestyleMicMutedState.value = !freestyleMicMutedState.value
    }

    /**
     * Mira greets first, unprompted, per US-8's Given/When/Then — the greeting line is built
     * from [SessionContext] (time of day + last routine) rather than a bare template. Real
     * time-of-day/last-routine history isn't wired yet (no session history until Phase 9, per
     * Phase 5's HomeScreen note), so this uses the same "evening" + no-last-routine defaults
     * HomeScreen already hardcodes — swap both together once Phase 9 lands.
     */
    fun startFreestyle() {
        freestyleResolvedRoutineState.value = null
        freestyleMicMutedState.value = false
        val greeting = "Good evening — ready to unwind? Want a quick warm-up before we move into a full routine?"
        freestyleCaptionState.value = greeting
        freestyleOrbState.value = OrbState.SPEAKING
        ttsProvider.speak(greeting, Lang.EN)
        freestyleOrbState.value = OrbState.LISTENING
        sttProvider.startListening { transcript -> handleFreestyleTranscript(transcript) }
    }

    private fun handleFreestyleTranscript(transcript: String) {
        freestyleOrbState.value = OrbState.THINKING
        val context = SessionContext(timeOfDay = "evening", lastRoutineId = null, relevantFacts = emptyList())
        lifecycleScope.launch {
            val response: AgentResponse = freestyleHarness.resolve(transcript, context)
            freestyleCaptionState.value = response.spokenLine
            freestyleOrbState.value = OrbState.SPEAKING
            ttsProvider.speak(response.spokenLine, Lang.EN)

            when (response.action) {
                ActionSchema.START_WORKOUT, ActionSchema.SUGGEST_WARMUP -> {
                    val routine = response.resolvedRoutineId?.let { contentRepository.routineById(it) }
                    freestyleResolvedRoutineState.value = routine
                }
                else -> {
                    freestyleOrbState.value = OrbState.LISTENING
                }
            }
        }
    }

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
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                greeting = "Good evening —\nready to unwind?",
                categories = contentRepository.categories,
                recommendedRoutines = contentRepository.routines,
                // No session history yet (Room lands in Phase 9) — see docs/PROGRESS.md.
                lastRoutine = null,
                onFreestyleClick = {
                    activity.startFreestyle()
                    navController.navigate(Routes.FREESTYLE)
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
                    isGoodForm = activity.playerIsGoodForm(),
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

        composable(Routes.FREESTYLE) {
            val resolvedRoutine = activity.freestyleResolvedRoutine()

            // Section 8.8: Freestyle hands off into Setup Tips -> Framing exactly as Browse
            // does — it does not skip the Framing gate.
            LaunchedEffect(resolvedRoutine) {
                if (resolvedRoutine != null) {
                    activity.clearFreestyleResolvedRoutine()
                    navController.navigate(Routes.setupTips(resolvedRoutine.id))
                }
            }

            FreestyleScreen(
                captionText = activity.freestyleCaption(),
                orbState = activity.freestyleOrb(),
                isMicMuted = activity.freestyleMicMuted(),
                onMicMuteToggle = { activity.toggleFreestyleMicMute() },
                onBackClick = { navController.popBackStack(Routes.HOME, inclusive = false) },
            )
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
                onSeeWhatMiraLearnedClick = { navController.navigate(Routes.MEMORY_GRAPH) },
            )
        }

        composable(Routes.MEMORY_GRAPH) {
            val facts by activity.factRepository().observeFacts().collectAsState(initial = emptyList<Fact>())
            MemoryGraphScreen(
                facts = facts,
                onBackClick = { navController.popBackStack() },
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
