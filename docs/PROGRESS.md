# Build Progress

## Done
- **Phase 0: Walking skeleton.** CameraX → MediaPipe PoseLandmarker (CPU delegate) → one hardcoded right-elbow angle check → hardcoded TTS line via system TextToSpeech. Manual on-device smoke test passed 2026-08-29 on vivo I2501 (Android 16). **Sustained FPS: 25–31 (avg 29–30), exceeds 15–20 FPS target.** Commit: `phase-0-walking-skeleton`.

## In progress
(none)

## Not started
Everything else — see docs/feature-spec.md Section 15 build priority. Next: Phase 1 (Assessor core: Warrior II rules, pure Kotlin JUnit tests).