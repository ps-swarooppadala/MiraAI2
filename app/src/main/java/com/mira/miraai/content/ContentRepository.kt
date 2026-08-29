package com.mira.miraai.content

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

interface ContentRepository {
    val categories: List<Category>
    val routines: List<Routine>
    val poses: List<Pose>

    fun categoryById(id: String): Category?
    fun routineById(id: String): Routine?
    fun poseById(id: String): Pose?
    fun routinesForCategory(categoryId: String): List<Routine>
}

/** Pure in-memory implementation over an already-parsed [ContentBundle] — trivially testable. */
class InMemoryContentRepository(bundle: ContentBundle) : ContentRepository {
    override val categories: List<Category> = bundle.categories
    override val routines: List<Routine> = bundle.routines
    override val poses: List<Pose> = bundle.poses

    private val categoriesById = categories.associateBy { it.id }
    private val routinesById = routines.associateBy { it.id }
    private val posesById = poses.associateBy { it.id }

    override fun categoryById(id: String): Category? = categoriesById[id]
    override fun routineById(id: String): Routine? = routinesById[id]
    override fun poseById(id: String): Pose? = posesById[id]

    override fun routinesForCategory(categoryId: String): List<Routine> {
        val category = categoriesById[categoryId] ?: return emptyList()
        return category.routineIds.mapNotNull { routinesById[it] }
    }
}

/**
 * Loads `assets/content.json` and resolves its resource-name strings against the app's
 * `drawable` resources. The only Android-coupled piece of the content pipeline — everything
 * else (parsing, lookups) is pure Kotlin per [InMemoryContentRepository]/[parseContentBundle].
 */
fun loadContentRepositoryFromAssets(context: Context, assetName: String = "content.json"): ContentRepository {
    val rawJson = context.assets.open(assetName).use { stream ->
        BufferedReader(InputStreamReader(stream)).readText()
    }
    val bundle = parseContentBundle(rawJson) { drawableName ->
        context.resources.getIdentifier(drawableName, "drawable", context.packageName)
    }
    return InMemoryContentRepository(bundle)
}
