package com.mira.miraai.data

import com.mira.miraai.memory.Fact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The one seam between the Room-coupled `data/` layer and the pure-Kotlin `memory/` layer —
 * everything upstream (the Memory Graph screen, a future `consolidate()` call site) talks to
 * [Fact], never [FactEntity].
 */
class FactRepository(private val database: MiraDatabase) {

    fun observeFacts(): Flow<List<Fact>> =
        database.factDao().observeAll().map { entities -> entities.map { it.toDomain() } }

    suspend fun allFacts(): List<Fact> = database.factDao().allFacts().map { it.toDomain() }

    suspend fun saveFacts(facts: List<Fact>) {
        database.factDao().upsertAll(facts.map { it.toEntity() })
    }
}
