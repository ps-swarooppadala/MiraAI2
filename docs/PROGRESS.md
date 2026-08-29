# Build Progress

## Done
- **Phase 0: Walking skeleton.** CameraX → MediaPipe PoseLandmarker (CPU delegate) → one hardcoded right-elbow angle check → hardcoded TTS line via system TextToSpeech. Manual on-device smoke test passed 2026-08-29 on vivo I2501 (Android 16). **Sustained FPS: 25–31 (avg 29–30), exceeds 15–20 FPS target.** Commit: `phase-0-walking-skeleton`.
- **Phase 1: Assessor core (Warrior II rules), pure Kotlin.** `WarriorIIAssessor.assess(frame, frontLeg)` implements the Section 10.1 verdict contract (single `verdictCode`, `hasCriticalIssue`, `confidence`) against a minimal pure-Kotlin pose model (`PoseModel.kt`: `BodyJoint`, `Landmark`, `PoseFrame`, `Side`) built on the existing `Point2D`/`angleDegrees` primitives from Phase 0's `AngleMath.kt`. `HoldTimer`/`HoldTimerState` implement the Section 8.3 hold-timer + Section 8.2 pause contract, including the explicit (non-placeholder) 2-second clean-tail. 24/24 JUnit tests pass (4 existing `AngleMathTest` + 9 `HoldTimerTest` + 11 `WarriorIIAssessorTest`). Commit: `Phase 1: Warrior II assessor core`.

  **Known deviation from feature-spec.md/build-architecture.md, flagged for a future doc pass:** both docs specify package `com.mira.ai`, but Phase 0 (already committed, on-device tested) used `com.mira.miraai` throughout (`app/build.gradle.kts` namespace/applicationId + every existing source file). Phase 1 followed the existing `com.mira.miraai` to avoid forking the package tree or renaming already-shipped Phase 0 code outside this phase's scope. Docs should be corrected to `com.mira.miraai`, or the app renamed to `com.mira.ai`, in a dedicated pass — not silently by whichever phase notices next.

  **Placeholder constants needing on-device tuning** (`WarriorIIThresholds.kt`, all explicitly commented as placeholders): `FRONT_KNEE_TARGET_DEG` (90°), `FRONT_KNEE_TOLERANCE_DEG` (15°), `BACK_LEG_STRAIGHT_MIN_DEG` (160°), `ARM_LEVEL_TOLERANCE` (0.05, normalized y), `TORSO_UPRIGHT_TOLERANCE` (0.05, normalized x), `FRONT_KNEE_PAST_ANKLE_TOLERANCE` (0.08, normalized x), `MIN_LANDMARK_VISIBILITY` (0.5). None are sourced from feature-spec.md — Section 8.3 explicitly leaves these open pending real camera data.

  **Scope notes for later phases:**
  - `PoseModel.kt` (`BodyJoint`/`Landmark`/`PoseFrame`) lives in `assessor/` for now since Phase 3's real `PoseEstimator` interface (build-architecture.md Section 2: `interface PoseEstimator { fun estimate(frame: ImageProxy): PoseFrame }`) doesn't exist yet — only a concrete MediaPipe-coupled `PoseEstimator` class does (`perception/PoseEstimator.kt`, Android-coupled, not the pure-Kotlin contract). Phase 3 may want to relocate/adapt this pose model into `perception/`.
  - `ElbowCheck.kt` (Phase 0's placeholder single-joint check) is left untouched — `MainActivity.kt` still calls it, and rewiring the camera pipeline onto `WarriorIIAssessor` is explicitly Phase 4's job ("Real camera pipeline wired to Phase 1+2"), not Phase 1's. Remove `ElbowCheck.kt` when Phase 4 does that rewiring.
  - `BOTH`-side sequencing and rep-counting are Coach Agent/Routine Sequencer territory (Phase 2), not covered here.

## In progress
(none)

## Not started
Everything else — see docs/feature-spec.md Section 15 build priority. Next: Phase 2 (Coach Agent state machine, pure Kotlin, `com.mira.miraai.agent`).