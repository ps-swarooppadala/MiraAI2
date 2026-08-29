package com.mira.miraai.voice

import com.mira.miraai.agent.CueEscalation
import com.mira.miraai.assessor.VerdictCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CueTemplatesTest {

    @Test
    fun `given the same issue repeats, when forIssue is called with increasing repeatIndex, then the phrasing varies`() {
        val first = CueTemplates.forIssue(VerdictCode.INSUFFICIENT_VISIBILITY, CueEscalation.NORMAL, repeatIndex = 0)
        val second = CueTemplates.forIssue(VerdictCode.INSUFFICIENT_VISIBILITY, CueEscalation.NORMAL, repeatIndex = 1)

        assertNotEquals(first, second)
    }

    @Test
    fun `forIssue with the same repeatIndex is deterministic`() {
        val a = CueTemplates.forIssue(VerdictCode.TORSO_LEANING, CueEscalation.FIRM, repeatIndex = 2)
        val b = CueTemplates.forIssue(VerdictCode.TORSO_LEANING, CueEscalation.FIRM, repeatIndex = 2)

        assertEquals(a, b)
    }

    @Test
    fun `forIssue cycles back around once repeatIndex exceeds the variant count`() {
        val firstCycle = CueTemplates.forIssue(VerdictCode.BACK_LEG_BENT, CueEscalation.NORMAL, repeatIndex = 0)
        val secondCycle = CueTemplates.forIssue(VerdictCode.BACK_LEG_BENT, CueEscalation.NORMAL, repeatIndex = 3)

        assertEquals(firstCycle, secondCycle)
    }

    @Test
    fun `forStepComplete gives a different line for switching sides vs the next pose`() {
        val switching = CueTemplates.forStepComplete(isSwitchingSides = true)
        val nextPose = CueTemplates.forStepComplete(isSwitchingSides = false)

        assertNotEquals(switching, nextPose)
    }

    @Test
    fun `forConfirmImprovement never returns blank`() {
        val line = CueTemplates.forConfirmImprovement(repeatIndex = 5)

        assertTrue(line.isNotBlank())
    }
}
