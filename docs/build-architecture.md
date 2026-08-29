# Mira.ai — Build Architecture (Consolidated, Final)

Supersedes both `Mira-ai-Build-Architecture.md` and `Mira-ai-Foolproof-Build-Strategy.md`
— the former was native Kotlin but pre-dated the P0 elevation of Freestyle/Memory; the
latter was written for Flutter, which we moved away from. This doc keeps the native
Kotlin technical design from the first, and the tool-agnostic process practices from
the second (those were never actually Flutter-specific). Target path: `docs/build-architecture.md`.

---

## 1. The core principle: the repo is the source of truth, not the tool

No coding agent — Claude Code, Antigravity, a human — should need anything outside the
repo to know what to do next.

- **Docs hold the plan.** `docs/feature-spec.md` (v2.1, canonical) + this file.
- **Tests hold "what's done."** Any agent can run the suite and know exactly how far
  the build got, without trusting a prior tool's word for it.
- **Git history holds the trail.** One commit per completed phase, message = phase name.
- **`docs/PROGRESS.md` holds current state** — updated after every phase, in every tool,
  no exceptions. This is what makes switching tools or running out of credits a 5-minute
  onboarding instead of a rebuild. See Section 6.

---

## 2. Device Abstraction Layer (native Kotlin, devPhone → iQOO NPU)

**Principle: business logic never knows which phone it's on. Only a Gradle flavor +
its DI module does.**

### Interfaces (package: `com.mira.miraai.perception` / `com.mira.miraai.voice`)

```kotlin
interface PoseEstimator {
    fun estimate(frame: ImageProxy): PoseFrame
}

interface LLMProvider {
    suspend fun complete(prompt: AgentPrompt): AgentResponse
}

interface TTSProvider {
    fun speak(text: String, lang: Lang)
}

interface STTProvider {
    fun startListening(onResult: (String) -> Unit)
}
```

Everything downstream (Assessor, Coach Agent, UI) depends only on these — never on
MediaPipe, QNN, or a specific model file directly.

### Gradle product flavors

```
android {
    flavorDimensions += "device"
    productFlavors {
        create("devPhone") { dimension = "device" }
        create("iqoo")     { dimension = "device" }
    }
}
```

| Component | devPhone flavor | iqoo flavor |
|---|---|---|
| Pose model delegate | LiteRT GPU/CPU delegate | LiteRT + QNN delegate (Hexagon NPU) |
| Pose model | MoveNet Lightning or smaller MediaPipe | RTMPose-Body2d / MediaPipe-Pose via QAI Hub |
| LLM (Freestyle classifier) | Small on-device model, CPU, or rules-only degraded mode | Same model via QNN, larger context comfortable |
| STT | Whisper-Tiny, CPU | Whisper-Tiny, QNN/Hexagon |
| TTS | Piper (CPU) or system TTS fallback | Piper (QNN-accelerated if available) or system TTS fallback |

Each flavor gets its own Hilt/Koin module (`DevPhoneAiModule`, `IqooAiModule`) binding
the interfaces above. Logic in `assessor/`, `agent/`, `memory/` is identical in both
flavors — swapping phones on hackathon day means changing the build flavor, not the code.

**Why this matters for your timeline:** build and test everything on your current phone
using `devPhone` for the next N days. On hackathon morning, build `iqoo`, rerun the same
test suite, and only debug delegate/model-loading issues — never logic issues.

---

## 3. Agentic AI Architecture

### Two brains, one mouth

```
Camera → PoseEstimator → Assessor (deterministic) → Verdict
                                                         │
                                                         ▼
                                  Coach Agent (stateful, decides)
                                          │                    │
                              ┌───────────┘                    └──────────┐
                              ▼                                            ▼
                     Template phrase                          LLM (Freestyle only,
                     (default path)                           constrained ActionSchema)
                              │                                            │
                              └─────────────────┬──────────────────────────┘
                                                 ▼
                                            TTSProvider
```

### Agent loop (per frame, and per Freestyle turn)

1. **Perceive** — Assessor emits a `Verdict`, or STT emits a transcript.
2. **Update working memory** — Moment Memory ring buffer, or `SessionContext` build.
3. **Decide** — Coach Agent state machine (pose path) or ActionSchema classifier
   (Freestyle path) — both produce a bounded decision, never open generation.
4. **Act** — template phrase, or validated `AgentResponse`.
5. **Log** — Session Memory, feeding consolidation into User Memory (Section 4).

### Constraining the LLM

The LLM is never asked to judge biomechanics or free-associate. For Freestyle, it only
selects from the fixed `ActionSchema` set (`feature-spec.md` Section 4.5) and returns a
short `spokenLine`. Anything outside the schema is caught by the harness and downgraded
to `ANSWER_SMALLTALK` — this bounds hallucination risk and keeps latency predictable.

Coach Agent hard rules (unit-testable, per feature-spec.md Section 10.2):
one cue per cooldown window · escalate only on persistence · confirm improvement ·
safety overrides everything · never coach below confidence threshold · never diagnose.

---

## 4. Memory System

| Layer | Lifetime | Storage | Written by |
|---|---|---|---|
| Moment | seconds | In-memory ring buffer | Assessor, every frame |
| Session | one session | Room: `sessions`, `pose_attempts`, `cues` | Coach Agent |
| User | permanent | Room: `facts` (triples) | `consolidate()`, after each session |

### Schema

```kotlin
@Entity data class SessionEntity(
    @PrimaryKey val id: String, val startedAt: Long, val endedAt: Long?,
    val posesPracticed: String
)

@Entity data class PoseAttemptEntity(
    @PrimaryKey val id: String, val sessionId: String, val pose: String,
    val holdSeconds: Int, val maxAngleDeviation: Float,
    val issuesDetected: String, val improved: Boolean
)

@Entity data class CueEntity(
    @PrimaryKey val id: String, val sessionId: String, val timestamp: Long,
    val intent: String, val text: String, val issueCode: String?
)

@Entity data class FactEntity(
    @PrimaryKey val id: String,
    val subject: String, val predicate: String, val objectValue: String,
    val confidence: Float, val lastUpdated: Long, val sourceSessionId: String
)
```

Matches `Fact` in `feature-spec.md` Section 4.6 exactly — one schema, read by both the
Freestyle proactive suggestion and the Memory Graph screen.

### Consolidation

```kotlin
fun consolidate(session: SessionData, existingFacts: List<Fact>): List<Fact>
```

Pure function, unit-testable against fixtures. Example rules: same issue on same pose
across ≥3 sessions → raise/insert `(user.left_knee, struggles_with, alignment)`; hold
time trending down → `(user, prefers, shorter_holds)`.

---

## 5. Laptop Memory Inspector

**Primary (demo-grade):** embedded local HTTP server (NanoHTTPD/Ktor) started on-device,
serving a bundled D3.js force-directed graph page reading `FactEntity` over the local
network — no cloud round trip, still fully on-device.

**Fallback (reliability — build this first):** "Export memory" button → dumps
`FactEntity` to JSON → a small pre-built laptop script (Python/networkx or an HTML
artifact) renders the same graph offline. Use as the recorded backup if stage WiFi is
unreliable.

---

## 6. Tool-agnostic process practices

### `docs/agent-instructions.md`
The single canonical instruction file. `CLAUDE.md` and `AGENTS.md` are direct copies
of it, not separate documents — never let them drift. See the two files delivered
alongside this doc.

### `docs/PROGRESS.md`
Updated after every phase, every tool, no exceptions:

```markdown
# Build Progress

## Done
- Phase 0: Walking skeleton. Manual smoke test passed on devPhone, 2026-08-29.
- Phase 1: Assessor core. Tests: 14/14 passing. Commit: a1b2c3d.

## In progress
- Phase 2: Coach Agent state machine. ~60% done. Next: escalation logic, see
  failing test coach_agent_test.kt::escalates_after_second_uncorrected_cue.

## Not started
- Phase 3 onward per Section 7's phase table below.

## Known friction points for the next agent
- (fill in as they come up)
```

### If you run out of credits mid-phase
1. Commit whatever's there, even red tests — `git commit -m "WIP: Phase N"`.
2. Update PROGRESS.md's friction-points section with exactly what's broken.
3. Open the repo in whatever tool has credits left, point it at
   `docs/agent-instructions.md` and `docs/PROGRESS.md` first, nothing else.
4. Antigravity runs Claude models alongside Gemini — a provider-specific credits
   problem may not require a full tool switch. Check which limit you actually hit
   before assuming a full migration is needed.

---

## 7. Phase table (final, native Kotlin, aligned to feature-spec.md v2.1 priorities)

| Phase | Deliverable | Test strategy |
|---|---|---|
| 0 | Walking skeleton: CameraX → pose (default delegate) → one hardcoded angle check → one hardcoded TTS line, `devPhone` flavor | Manual on-device smoke test |
| 1 | Assessor core: Warrior II rules, pure Kotlin, `com.mira.miraai.assessor` | JUnit, from feature-spec.md Given/When/Then |
| 2 | Coach Agent state machine: pure Kotlin, `com.mira.miraai.agent` | JUnit, fake clock + scripted verdict stream |
| 3 | Device abstraction: interfaces + `devPhone`/`iqoo` flavors + DI modules | Contract tests against fakes |
| 4 | Real camera pipeline wired to Phase 1+2 on `devPhone` | On-device integration test, golden landmark fixtures |
| 5 | Content model + Browse IA: Home/Category/Routine Detail | Compose UI tests for navigation |
| 6 | Workout Mode player: pause/resume, hold timer, rep counter | Widget/state tests + manual device check |
| 7 | Voice output (Piper + system TTS fallback) | Unit test: intent/verdict → correct template |
| 8 | **Freestyle: STT wiring, ActionSchema classifier, harness routing, orb/caption UI** | Unit test per intent phrase; out-of-schema fallback test |
| 9 | Session memory (Room: sessions/pose_attempts/cues) | Repository tests, in-memory DB |
| 10 | **User memory + consolidation + Memory Graph screen** | Pure function tests against fixture session histories |
| 11 | Memory inspector: JSON export fallback first, live server second | Manual check against real fact data |
| 12 | `iqoo` flavor swap: QNN delegates | Full existing suite rerun unchanged, fix only native-loader issues |
| 13+ | Second pose, Hindi TTS, polish — feature-flagged | New tests only, never edit merged phase tests |

Phases 8 and 10 are pulled forward relative to the original plan — feature-spec.md v2.1
elevated Freestyle and Memory to P0, so they should not be left until the end.
