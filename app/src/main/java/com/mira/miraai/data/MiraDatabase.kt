package com.mira.miraai.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Local on-device database — build-architecture.md Section 4's Session + User memory layers.
 * `facts` (Phase 10) bumped [version] to 2 rather than silently changing Phase 9's shipped
 * schema. No migration path is written — [Room.databaseBuilder]'s
 * `fallbackToDestructiveMigration()` is used instead, acceptable pre-ship (no real user data
 * exists to preserve yet); replace with a real `Migration` before this ships with users on v1.
 */
@Database(
    entities = [SessionEntity::class, PoseAttemptEntity::class, CueEntity::class, FactEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class MiraDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun poseAttemptDao(): PoseAttemptDao
    abstract fun cueDao(): CueDao
    abstract fun factDao(): FactDao

    companion object {
        fun build(context: Context): MiraDatabase =
            Room.databaseBuilder(context, MiraDatabase::class.java, "mira.db")
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
