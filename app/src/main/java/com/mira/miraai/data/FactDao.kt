package com.mira.miraai.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FactDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(facts: List<FactEntity>)

    @Query("SELECT * FROM facts ORDER BY confidence DESC")
    fun observeAll(): Flow<List<FactEntity>>

    @Query("SELECT * FROM facts ORDER BY confidence DESC")
    suspend fun allFacts(): List<FactEntity>
}
