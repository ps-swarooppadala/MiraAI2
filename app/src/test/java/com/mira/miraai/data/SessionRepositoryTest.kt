package com.mira.miraai.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Repository tests against an in-memory DB, per this phase's instruction. Runs on the JVM via
 * Robolectric rather than an instrumented (`androidTest`) suite, since this environment has no
 * attached device/emulator (same constraint noted in every earlier on-device-verification gap
 * in docs/PROGRESS.md). Pinned to `sdk = [33]` rather than the project's `compileSdk` 37 — a
 * fictional-future SDK level Robolectric has no framework jar for; Robolectric's simulated SDK
 * only needs to satisfy `minSdk` (26), so 33 is an arbitrary supported stand-in, not tied to any
 * real target.
 *
 * `application = Application::class` overrides Robolectric's manifest-driven default of the
 * real `MiraApplication` — whose `onCreate()` calls `startKoin()`. Left at the default, a
 * second Robolectric test class in the same JVM process hits
 * `KoinApplicationAlreadyStartedException` since Koin's context is a process-wide singleton.
 * These tests only exercise Room, not any Koin binding, so a bare `Application` is correct, not
 * a workaround.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class SessionRepositoryTest {

    private lateinit var repository: SessionRepository

    @Before
    fun setUp() {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), MiraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = SessionRepository(db)
    }

    @Test
    fun `recorded session round-trips through the repository`() = runTest {
        val session = SessionEntity(id = "s1", startedAt = 100L, endedAt = 200L, posesPracticed = "warrior_ii,tree_pose")

        repository.recordSession(session)

        assertEquals(session, repository.sessionById("s1"))
        assertEquals(listOf(session), repository.allSessions())
    }

    @Test
    fun `sessionById returns null for an unknown id`() = runTest {
        assertNull(repository.sessionById("does-not-exist"))
    }

    @Test
    fun `pose attempts are scoped to their session`() = runTest {
        val attempt1 = PoseAttemptEntity(
            id = "a1", sessionId = "s1", pose = "warrior_ii",
            holdSeconds = 18, maxAngleDeviation = 4.5f, issuesDetected = "FRONT_KNEE_PAST_ANKLE", improved = true,
        )
        val attempt2 = PoseAttemptEntity(
            id = "a2", sessionId = "s2", pose = "tree_pose",
            holdSeconds = 12, maxAngleDeviation = 2.0f, issuesDetected = "", improved = false,
        )

        repository.recordPoseAttempt(attempt1)
        repository.recordPoseAttempt(attempt2)

        assertEquals(listOf(attempt1), repository.attemptsForSession("s1"))
        assertEquals(listOf(attempt1, attempt2), repository.allAttempts())
    }

    @Test
    fun `cues for a session are returned in timestamp order`() = runTest {
        val later = CueEntity(id = "c2", sessionId = "s1", timestamp = 200L, intent = "SPEAK_CUE", text = "Bend deeper.", issueCode = "FRONT_KNEE_ANGLE")
        val earlier = CueEntity(id = "c1", sessionId = "s1", timestamp = 100L, intent = "SPEAK_CUE", text = "Widen your stance.", issueCode = "BACK_LEG_BENT")

        repository.recordCue(later)
        repository.recordCue(earlier)

        assertEquals(listOf(earlier, later), repository.cuesForSession("s1"))
    }
}
