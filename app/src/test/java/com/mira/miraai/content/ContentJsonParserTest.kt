package com.mira.miraai.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentJsonParserTest {

    private val sampleJson = """
        {
          "categories": [
            { "id": "full_body", "title": "Full Body", "subtitle": "Standing strength", "icon": "ic_full_body", "routineIds": ["r1"] },
            { "id": "empty_cat", "title": "Empty", "subtitle": "Nothing yet", "icon": "ic_empty", "routineIds": [] }
          ],
          "routines": [
            {
              "id": "r1",
              "title": "Foundations",
              "categoryIds": ["full_body"],
              "level": "BEGINNER",
              "estimatedDurationSec": 420,
              "coverImage": "cover_r1",
              "poseSequence": [
                { "poseId": "warrior_ii", "side": "BOTH", "targetHoldSec": 20, "order": 2 },
                { "poseId": "tree_pose", "side": "BOTH", "targetHoldSec": 15, "order": 1 }
              ]
            },
            {
              "id": "r2_unsupported",
              "title": "Not Yet Coachable",
              "categoryIds": ["full_body"],
              "level": "BEGINNER",
              "estimatedDurationSec": 300,
              "coverImage": "cover_r2",
              "poseSequence": [
                { "poseId": "tree_pose", "side": "NONE", "targetHoldSec": 15, "order": 1 }
              ]
            }
          ],
          "poses": [
            { "id": "warrior_ii", "displayName": "Warrior II", "sanskritName": "Virabhadrasana II", "thumbnail": "thumb_warrior", "instructionSteps": ["Step wide"], "defaultHoldSec": 20, "trackingMode": "HOLD", "hasAssessorRuleSet": true },
            { "id": "tree_pose", "displayName": "Tree Pose", "thumbnail": "thumb_tree", "instructionSteps": ["Balance"], "defaultHoldSec": 15, "trackingMode": "HOLD", "hasAssessorRuleSet": false }
          ]
        }
    """.trimIndent()

    private fun resolver(name: String): Int = when (name) {
        "ic_full_body" -> 101
        "cover_r1" -> 201
        "thumb_warrior" -> 301
        else -> 0
    }

    @Test
    fun `parses categories, routines, and poses with resolved drawable ids`() {
        val bundle = parseContentBundle(sampleJson, ::resolver)

        assertEquals(2, bundle.categories.size)
        assertEquals(2, bundle.routines.size)
        assertEquals(2, bundle.poses.size)

        val fullBody = bundle.categories.first { it.id == "full_body" }
        assertEquals(101, fullBody.iconRes)
        assertEquals(listOf("r1"), fullBody.routineIds)
    }

    @Test
    fun `pose sequence is sorted by order regardless of JSON order`() {
        val bundle = parseContentBundle(sampleJson, ::resolver)
        val r1 = bundle.routines.first { it.id == "r1" }

        assertEquals(listOf("tree_pose", "warrior_ii"), r1.poseSequence.map { it.poseId })
        assertEquals(1, r1.poseSequence[0].order)
        assertEquals(2, r1.poseSequence[1].order)
    }

    @Test
    fun `routine coaching support requires every pose to have an assessor rule set`() {
        val bundle = parseContentBundle(sampleJson, ::resolver)

        val r1 = bundle.routines.first { it.id == "r1" }
        assertFalse("r1 mixes warrior_ii (has rules) with tree_pose (no rules yet)", r1.isCoachingSupported)

        val r2 = bundle.routines.first { it.id == "r2_unsupported" }
        assertFalse(r2.isCoachingSupported)
    }

    @Test
    fun `routine is coaching-supported when every pose has a rule set`() {
        val allRuleSetsJson = sampleJson.replace(
            "\"id\": \"tree_pose\", \"displayName\": \"Tree Pose\", \"thumbnail\": \"thumb_tree\", \"instructionSteps\": [\"Balance\"], \"defaultHoldSec\": 15, \"trackingMode\": \"HOLD\", \"hasAssessorRuleSet\": false",
            "\"id\": \"tree_pose\", \"displayName\": \"Tree Pose\", \"thumbnail\": \"thumb_tree\", \"instructionSteps\": [\"Balance\"], \"defaultHoldSec\": 15, \"trackingMode\": \"HOLD\", \"hasAssessorRuleSet\": true",
        )
        val bundle = parseContentBundle(allRuleSetsJson, ::resolver)

        assertTrue(bundle.routines.first { it.id == "r1" }.isCoachingSupported)
    }

    @Test
    fun `optional pose fields default sensibly`() {
        val bundle = parseContentBundle(sampleJson, ::resolver)
        val treePose = bundle.poses.first { it.id == "tree_pose" }

        assertNull(treePose.sanskritName)
    }

    @Test
    fun `unresolved drawable name falls back to resolver's own default`() {
        val bundle = parseContentBundle(sampleJson) { 0 }
        assertEquals(0, bundle.categories.first().iconRes)
    }
}
