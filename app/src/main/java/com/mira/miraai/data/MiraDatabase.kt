package com.mira.miraai.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Local session-history database — build-architecture.md Section 4's "Session" memory layer.
 * Phase 9 scope: `sessions`/`pose_attempts`/`cues` only. `facts` (User memory layer) is added in
 * Phase 10, which will bump [version] rather than replace this file's schema silently.
 */
@Database(
    entities = [SessionEntity::class, PoseAttemptEntity::class, CueEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class MiraDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun poseAttemptDao(): PoseAttemptDao
    abstract fun cueDao(): CueDao

    companion object {
        fun build(context: Context): MiraDatabase =
            Room.databaseBuilder(context, MiraDatabase::class.java, "mira.db").build()
    }
}
