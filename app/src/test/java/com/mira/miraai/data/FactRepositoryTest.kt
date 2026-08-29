package com.mira.miraai.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mira.miraai.memory.Fact
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** See [SessionRepositoryTest]'s class doc for why `application = Application::class` is needed. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class FactRepositoryTest {

    private lateinit var repository: FactRepository

    @Before
    fun setUp() {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), MiraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = FactRepository(db)
    }

    private fun fact(id: String, confidence: Float) = Fact(
        id = id, subject = "user.warrior_ii", predicate = "struggles_with",
        objectValue = "front_knee_past_ankle", confidence = confidence, lastUpdated = 1000L, sourceSessionId = "s1",
    )

    @Test
    fun `saved facts round-trip through the repository`() = runTest {
        repository.saveFacts(listOf(fact("f1", 0.5f)))

        assertEquals(listOf(fact("f1", 0.5f)), repository.allFacts())
    }

    @Test
    fun `saving a fact with the same id replaces it, not duplicates it`() = runTest {
        repository.saveFacts(listOf(fact("f1", 0.3f)))
        repository.saveFacts(listOf(fact("f1", 0.55f)))

        val all = repository.allFacts()
        assertEquals(1, all.size)
        assertEquals(0.55f, all.single().confidence)
    }

    @Test
    fun `allFacts returns rows ordered by descending confidence`() = runTest {
        repository.saveFacts(listOf(fact("low", 0.2f), fact("high", 0.9f)))

        assertEquals(listOf("high", "low"), repository.allFacts().map { it.id })
    }
}
