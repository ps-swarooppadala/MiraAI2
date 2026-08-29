package com.mira.miraai.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity)

    @Query("SELECT * FROM sessions ORDER BY startedAt DESC")
    suspend fun allSessions(): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    suspend fun sessionById(sessionId: String): SessionEntity?
}

@Dao
interface PoseAttemptDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoseAttempt(attempt: PoseAttemptEntity)

    @Query("SELECT * FROM pose_attempts WHERE sessionId = :sessionId")
    suspend fun attemptsForSession(sessionId: String): List<PoseAttemptEntity>

    @Query("SELECT * FROM pose_attempts")
    suspend fun allAttempts(): List<PoseAttemptEntity>
}

@Dao
interface CueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCue(cue: CueEntity)

    @Query("SELECT * FROM cues WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun cuesForSession(sessionId: String): List<CueEntity>
}
