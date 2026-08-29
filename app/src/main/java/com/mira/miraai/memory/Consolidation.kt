package com.mira.miraai.memory

import com.mira.miraai.memory.ConsolidationThresholds.CONFIDENCE_INCREMENT
import com.mira.miraai.memory.ConsolidationThresholds.MAX_CONFIDENCE
import com.mira.miraai.memory.ConsolidationThresholds.NEW_FACT_CONFIDENCE
import com.mira.miraai.memory.ConsolidationThresholds.SHORTER_HOLDS_TREND_THRESHOLD_SEC

private const val STRUGGLES_WITH = "struggles_with"
private const val AVG_HOLD_TIME = "avg_hold_time"
private const val PREFERS = "prefers"
private const val SHORTER_HOLDS = "shorter_holds"
private const val USER_SUBJECT = "user"

/**
 * build-architecture.md Section 4: "Pure function, unit-testable against fixtures." One
 * [SessionData] in, an updated [Fact] list out — [consolidate] never touches Room/a database
 * directly, so the same evidence-accumulation logic that runs after a real session (future
 * wiring) is exercised in tests via nothing but fixture values.
 *
 * Implements the two example rules from build-architecture.md Section 4 / feature-spec.md
 * Section 10.4:
 * 1. **struggles_with** — one `Fact` per (pose, issue) pair seen this session. A pair not seen
 *    before is inserted at [NEW_FACT_CONFIDENCE]; a pair matching an existing Fact has its
 *    confidence bumped by [CONFIDENCE_INCREMENT] (capped at [MAX_CONFIDENCE]) — repeated
 *    evidence across sessions is what "increases with repeated evidence" (feature-spec.md
 *    Section 4.6's `Fact.confidence` doc) actually means in practice, since this function only
 *    ever sees one session at a time and relies on the caller feeding its own output back in as
 *    `existingFacts` for the next session.
 * 2. **avg_hold_time / prefers shorter_holds** — the session's average hold time updates (or
 *    inserts) a running `(user, avg_hold_time, "<n>s")` Fact; if it dropped by at least
 *    [SHORTER_HOLDS_TREND_THRESHOLD_SEC] seconds versus the previous known average, a
 *    `(user, prefers, shorter_holds)` Fact is inserted/reinforced.
 */
fun consolidate(session: SessionData, existingFacts: List<Fact>): List<Fact> {
    val updated = existingFacts.toMutableList()

    applyStrugglesWithRule(session, updated)
    applyHoldTimeTrendRule(session, updated)

    return updated
}

private fun applyStrugglesWithRule(session: SessionData, facts: MutableList<Fact>) {
    val issuesThisSession: Set<Pair<String, String>> = session.poseAttempts
        .flatMap { attempt -> attempt.issuesDetected.map { issue -> attempt.pose to issue } }
        .toSet()

    for ((pose, issue) in issuesThisSession) {
        upsertFact(
            facts = facts,
            subject = "$USER_SUBJECT.$pose",
            predicate = STRUGGLES_WITH,
            objectValue = issue.lowercase(),
            session = session,
        )
    }
}

private fun applyHoldTimeTrendRule(session: SessionData, facts: MutableList<Fact>) {
    val holds = session.poseAttempts.filter { it.holdSeconds > 0 }
    if (holds.isEmpty()) return

    val avgHoldSec = holds.map { it.holdSeconds }.average()
    val previousAvg = facts
        .firstOrNull { it.subject == USER_SUBJECT && it.predicate == AVG_HOLD_TIME }
        ?.objectValue
        ?.removeSuffix("s")
        ?.toFloatOrNull()

    upsertFact(
        facts = facts,
        subject = USER_SUBJECT,
        predicate = AVG_HOLD_TIME,
        objectValue = "${avgHoldSec.toInt()}s",
        session = session,
    )

    if (previousAvg != null && previousAvg - avgHoldSec >= SHORTER_HOLDS_TREND_THRESHOLD_SEC) {
        upsertFact(
            facts = facts,
            subject = USER_SUBJECT,
            predicate = PREFERS,
            objectValue = SHORTER_HOLDS,
            session = session,
        )
    }
}

private fun upsertFact(facts: MutableList<Fact>, subject: String, predicate: String, objectValue: String, session: SessionData) {
    val index = facts.indexOfFirst { it.subject == subject && it.predicate == predicate && matchesObjectFamily(it, predicate, objectValue) }
    if (index >= 0) {
        val existing = facts[index]
        facts[index] = existing.copy(
            objectValue = objectValue,
            confidence = (existing.confidence + CONFIDENCE_INCREMENT).coerceAtMost(MAX_CONFIDENCE),
            lastUpdated = session.endedAtMs,
            sourceSessionId = session.sessionId,
        )
    } else {
        facts += Fact(
            id = "${subject}_${predicate}_$objectValue",
            subject = subject,
            predicate = predicate,
            objectValue = objectValue,
            confidence = NEW_FACT_CONFIDENCE,
            lastUpdated = session.endedAtMs,
            sourceSessionId = session.sessionId,
        )
    }
}

/**
 * `avg_hold_time` is a running value (its `objectValue` legitimately changes session to
 * session), so it matches on (subject, predicate) alone. Every other predicate here
 * (`struggles_with`, `prefers`) identifies a distinct Fact per exact `objectValue` — matching
 * only by (subject, predicate) for those would wrongly merge e.g. two different struggled-with
 * issues on the same pose into one Fact.
 */
private fun matchesObjectFamily(fact: Fact, predicate: String, newObjectValue: String): Boolean =
    if (predicate == AVG_HOLD_TIME) true else fact.objectValue == newObjectValue
