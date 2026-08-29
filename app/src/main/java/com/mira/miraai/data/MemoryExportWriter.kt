package com.mira.miraai.data

import android.content.Context
import com.mira.miraai.memory.buildMemoryGraphExport
import java.io.File

/**
 * Phase 11 — Memory inspector, JSON export fallback (build-architecture.md Section 5). The one
 * Android-coupled edge for the export: reads real [FactRepository] data and writes the pure-Kotlin
 * `memory.MemoryGraphExport` JSON (built/serialized in `memory/MemoryExport.kt`, zero Android
 * imports there per CLAUDE.md) to a file the user can pull off-device or share.
 *
 * Written to the app's external files dir (no storage permission needed, visible to the user via
 * a file manager / `adb pull`) rather than internal storage, so it's actually reachable without
 * the live embedded-server path (Section 5's primary option, not built this phase).
 */
class MemoryExportWriter(private val context: Context, private val factRepository: FactRepository) {

    suspend fun exportToFile(fileName: String = "mira_memory_export.json"): File {
        val facts = factRepository.allFacts()
        val json = buildMemoryGraphExport(facts).toJson()
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        val file = File(dir, fileName)
        file.writeText(json)
        return file
    }
}
