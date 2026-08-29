package com.mira.miraai.data

/**
 * Thin wrapper over the three Phase 9 DAOs — the one seam `MainActivity`/the future workout
 * session-logging wiring is meant to depend on, rather than each DAO directly. No mapping to a
 * pure-Kotlin domain type here (unlike [com.mira.miraai.memory.Fact]/`FactRepository` in Phase
 * 10) because nothing in `assessor/`/`agent/` needs to read session history back yet — writing
 * it is the only consumer so far.
 */
class SessionRepository(private val database: MiraDatabase) {

    suspend fun recordSession(session: SessionEntity) {
        database.sessionDao().insertSession(session)
    }

    suspend fun recordPoseAttempt(attempt: PoseAttemptEntity) {
        database.poseAttemptDao().insertPoseAttempt(attempt)
    }

    suspend fun recordCue(cue: CueEntity) {
        database.cueDao().insertCue(cue)
    }

    suspend fun allSessions(): List<SessionEntity> = database.sessionDao().allSessions()

    suspend fun sessionById(sessionId: String): SessionEntity? = database.sessionDao().sessionById(sessionId)

    suspend fun attemptsForSession(sessionId: String): List<PoseAttemptEntity> =
        database.poseAttemptDao().attemptsForSession(sessionId)

    suspend fun allAttempts(): List<PoseAttemptEntity> = database.poseAttemptDao().allAttempts()

    suspend fun cuesForSession(sessionId: String): List<CueEntity> = database.cueDao().cuesForSession(sessionId)
}
