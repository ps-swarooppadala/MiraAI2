package com.mira.miraai.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentRepositoryTest {

    private val warriorPose = Pose(
        id = "warrior_ii", displayName = "Warrior II", sanskritName = "Virabhadrasana II",
        thumbnailRes = 1, instructionSteps = listOf("step"), defaultHoldSec = 20,
        trackingMode = TrackingMode.HOLD, hasAssessorRuleSet = true,
    )
    private val routine = Routine(
        id = "foundations_full_body", title = "Foundations", categoryIds = listOf("full_body"),
        level = RoutineLevel.BEGINNER, estimatedDurationSec = 420, coverImageRes = 2,
        poseSequence = listOf(RoutineStep("warrior_ii", PoseSide.BOTH, 20, null, 1)),
        isCoachingSupported = true,
    )
    private val fullBody = Category(
        id = "full_body", title = "Full Body", subtitle = "Strength", iconRes = 3,
        routineIds = listOf("foundations_full_body"),
    )
    private val emptyCategory = Category(
        id = "empty_cat", title = "Empty", subtitle = "Nothing", iconRes = 4, routineIds = emptyList(),
    )

    private val repository = InMemoryContentRepository(
        ContentBundle(categories = listOf(fullBody, emptyCategory), routines = listOf(routine), poses = listOf(warriorPose)),
    )

    @Test
    fun `looks up category, routine, and pose by id`() {
        assertEquals(fullBody, repository.categoryById("full_body"))
        assertEquals(routine, repository.routineById("foundations_full_body"))
        assertEquals(warriorPose, repository.poseById("warrior_ii"))
    }

    @Test
    fun `unknown id lookups return null`() {
        assertNull(repository.categoryById("nope"))
        assertNull(repository.routineById("nope"))
        assertNull(repository.poseById("nope"))
    }

    @Test
    fun `routines for category resolves routineIds to full Routine objects`() {
        val routines = repository.routinesForCategory("full_body")
        assertEquals(listOf(routine), routines)
    }

    @Test
    fun `category with no routineIds yields an empty list, not an error`() {
        assertTrue(repository.routinesForCategory("empty_cat").isEmpty())
    }

    @Test
    fun `unknown category id yields an empty list`() {
        assertTrue(repository.routinesForCategory("does_not_exist").isEmpty())
    }
}
