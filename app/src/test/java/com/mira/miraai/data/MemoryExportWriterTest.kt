package com.mira.miraai.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mira.miraai.memory.Fact
import com.mira.miraai.memory.MemoryGraphExport
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Phase 11 — confirms the JSON export works against real [FactEntity] data read through
 * [FactRepository], not just the pure-Kotlin fixtures in `memory/MemoryExportTest.kt`. Same
 * Robolectric/in-memory-DB pattern as [SessionRepositoryTest].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class MemoryExportWriterTest {

    private lateinit var writer: MemoryExportWriter

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val db = Room.inMemoryDatabaseBuilder(context, MiraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val factRepository = FactRepository(db)
        writer = MemoryExportWriter(context, factRepository)
    }

    @Test
    fun `exporting with no facts writes an empty graph`() = runTest {
        val file = writer.exportToFile()

        val export = MemoryGraphExport.fromJson(file.readText())
        assertTrue(export.nodes.isEmpty())
        assertTrue(export.links.isEmpty())
    }

    @Test
    fun `exporting real fact data writes a graph the laptop script can read back`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val db = Room.inMemoryDatabaseBuilder(context, MiraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val factRepository = FactRepository(db)
        factRepository.saveFacts(
            listOf(
                Fact("f1", "user.warrior_ii", "struggles_with", "front_knee_past_ankle", 0.8f, 1_000L, "s3"),
                Fact("f2", "user", "prefers", "shorter_holds", 0.4f, 2_000L, "s2"),
            ),
        )
        val writerForRealData = MemoryExportWriter(context, factRepository)

        val file = writerForRealData.exportToFile()

        val export = MemoryGraphExport.fromJson(file.readText())
        assertEquals(2, export.links.size)
        assertEquals(4, export.nodes.size)
    }
}
