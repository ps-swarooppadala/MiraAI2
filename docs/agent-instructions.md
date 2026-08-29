# Mira.ai — Agent Instructions

Read docs/feature-spec.md (canonical, v2.1) and docs/build-architecture.md before any
work. Check docs/PROGRESS.md for what's done and what's next — do not skip phases.

Rules:
- Code in assessor/, agent/, and memory/ must have zero android.*, Compose, or
  CameraX imports. Pure Kotlin, JUnit-testable without a device or emulator.
- All CV/LLM/STT/TTS access goes through the interfaces in build-architecture.md
  Section 2 — never call a model API directly from UI or agent code.
- Two Gradle flavors: devPhone, iqoo. Never write flavor-specific logic outside the
  flavor's DI module.
- Follow TDD: write the test from feature-spec.md's Given/When/Then before implementing.
- Match UI to docs/ux/*.md exactly for tokens and layout — feature-spec.md Section 7
  is a fallback only if a Stitch export conflicts.
- Any numeric threshold not explicitly given in the spec (angle targets, tolerances,
  cooldown windows) must be added as a clearly named, isolated constant — flag it in
  docs/PROGRESS.md as "needs tuning," never bury it inline.
- Freestyle's ActionSchema is fixed (feature-spec.md Section 4.5) — never let the
  LLM/classifier drive app state with a value outside that set. Out-of-schema output
  falls back to ANSWER_SMALLTALK.
- After finishing a phase: run the full test suite, update docs/PROGRESS.md with
  what's done and what's next, commit with message "Phase N: <name>". Do not start
  the next phase in the same commit.
- If you are a new agent picking this project up mid-build: read docs/PROGRESS.md,
  run the test suite, confirm the last logged phase's tests pass, then continue from
  the next phase in build-architecture.md's Section 7 phase table. Do not re-architect
  completed phases.
