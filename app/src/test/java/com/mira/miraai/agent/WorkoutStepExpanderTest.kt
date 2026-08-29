package com.mira.miraai.agent

import com.mira.miraai.content.PoseSide
import com.mira.miraai.content.Routine
import com.mira.miraai.content.RoutineLevel
import com.mira.miraai.content.RoutineStep
import com.mira.miraai.perception.Side
import org.junit.Assert.assertEquals
import org.junit.Test

private fun routineOf(vararg steps: RoutineStep) = Routine(
    id = "r", title = "R", categoryIds = emptyList(), level = RoutineLevel.BEGINNER,
    estimatedDurationSec = 0, coverImageRes = 0, poseSequence = steps.toList(), isCoachingSupported = true,
)

class WorkoutStepExpanderTest {

    @Test
    fun bothSideStep_expandsIntoLeftThenRight() {
        val routine = routineOf(RoutineStep("warrior_ii", PoseSide.BOTH, 20, null, order = 1))
        val steps = expandRoutineSteps(routine)
        assertEquals(2, steps.size)
        assertEquals(Side.LEFT, steps[0].side)
        assertEquals(Side.RIGHT, steps[1].side)
        assertEquals("warrior_ii", steps[1].poseId)
    }

    @Test
    fun noneSideStep_expandsIntoOneStepWithNullSide() {
        val routine = routineOf(RoutineStep("chair_pose", PoseSide.NONE, 20, 2, order = 1))
        val steps = expandRoutineSteps(routine)
        assertEquals(1, steps.size)
        assertEquals(null, steps[0].side)
    }

    @Test
    fun multipleSteps_expandInOrderRegardlessOfInputOrdering() {
        val routine = routineOf(
            RoutineStep("chair_pose", PoseSide.NONE, 20, 2, order = 2),
            RoutineStep("warrior_ii", PoseSide.BOTH, 20, null, order = 1),
        )
        val steps = expandRoutineSteps(routine)
        assertEquals(listOf("warrior_ii", "warrior_ii", "chair_pose"), steps.map { it.poseId })
    }
}
