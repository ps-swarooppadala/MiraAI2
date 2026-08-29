package com.mira.miraai.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mira.miraai.memory.Fact

/** Room row for the User memory layer — build-architecture.md Section 4, matches [Fact] 1:1. */
@Entity(tableName = "facts")
data class FactEntity(
    @PrimaryKey val id: String,
    val subject: String,
    val predicate: String,
    val objectValue: String,
    val confidence: Float,
    val lastUpdated: Long,
    val sourceSessionId: String,
)

fun FactEntity.toDomain() = Fact(
    id = id,
    subject = subject,
    predicate = predicate,
    objectValue = objectValue,
    confidence = confidence,
    lastUpdated = lastUpdated,
    sourceSessionId = sourceSessionId,
)

fun Fact.toEntity() = FactEntity(
    id = id,
    subject = subject,
    predicate = predicate,
    objectValue = objectValue,
    confidence = confidence,
    lastUpdated = lastUpdated,
    sourceSessionId = sourceSessionId,
)
