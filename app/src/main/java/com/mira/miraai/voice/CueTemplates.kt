package com.mira.miraai.voice

import com.mira.miraai.agent.CueEscalation
import com.mira.miraai.assessor.VerdictCode

/**
 * needs tuning — placeholder template phrases for Coach Agent cues, keyed by [VerdictCode].
 * feature-spec.md Section 9.2 calls for a real "template-first language" library (Phase 7);
 * this is a minimal stand-in so Phase 4's camera pipeline has something to speak, not the
 * final wording. Flagged in docs/PROGRESS.md — replace wholesale in Phase 7, don't tune in place.
 *
 * Each code maps to a *list* of phrasings per escalation tier, not a single line — a coaching
 * cue that repeats verbatim every cooldown window (e.g. "I still can't see you — step back into
 * frame." on loop) reads as robotic. [forIssue]/[forSafetyOverride] rotate through the list using
 * [CoachDecision.repeatIndex][com.mira.miraai.agent.CoachDecision], so the same underlying issue
 * still gets re-cued (guardrails unchanged) but in different words each time.
 */
object CueTemplates {
    private val NORMAL: Map<VerdictCode, List<String>> = mapOf(
        VerdictCode.FRONT_KNEE_TOO_STRAIGHT to listOf(
            "Bend your front knee a bit more.",
            "Sink a little deeper into that front knee.",
            "A touch more bend in the front knee.",
        ),
        VerdictCode.FRONT_KNEE_OVER_BENT to listOf(
            "Ease up on that front knee bend.",
            "Rise up just a little through the front knee.",
            "Back off the bend a touch.",
        ),
        VerdictCode.FRONT_KNEE_PAST_ANKLE to listOf(
            "Bring your front knee back over your ankle.",
            "Pull that knee back in line with your ankle.",
        ),
        VerdictCode.BACK_LEG_BENT to listOf(
            "Straighten your back leg.",
            "Lengthen through the back leg.",
            "Reach that back heel away and straighten the leg.",
        ),
        VerdictCode.ARMS_NOT_LEVEL to listOf(
            "Level your arms out with your shoulders.",
            "Stretch both arms out to the same height.",
            "Even out your arms — shoulder height on both sides.",
        ),
        VerdictCode.ARMS_NOT_STRAIGHT to listOf(
            "Straighten your arms out through the elbows.",
            "Lengthen both arms — no bend in the elbows.",
        ),
        VerdictCode.TORSO_LEANING to listOf(
            "Keep your torso upright over your hips.",
            "Stack your shoulders back over your hips.",
            "Lift tall through your spine.",
        ),
        VerdictCode.INSUFFICIENT_VISIBILITY to listOf(
            "I can't quite see your whole body — step back a little.",
            "Let's get you fully in frame, step back for me.",
            "Try backing up a step so I can see you head to toe.",
        ),
        VerdictCode.TREE_STANDING_LEG_BENT to listOf(
            "Straighten that standing leg a bit more.",
            "Press firmly through your standing leg — lock it in.",
        ),
        VerdictCode.TREE_LIFTED_FOOT_TOO_LOW to listOf(
            "Lift your foot a little higher against your standing leg.",
            "Press your foot up higher on your inner leg.",
        ),
        VerdictCode.TREE_HIPS_NOT_LEVEL to listOf(
            "Level out your hips.",
            "Square your hips back to center.",
        ),
        VerdictCode.TREE_ARMS_NOT_AT_CHEST to listOf(
            "Bring your palms together at your chest.",
            "Draw your hands in to meet at your heart.",
        ),
        VerdictCode.TREE_HEAD_TILTED to listOf(
            "Level your head and find a steady point to focus on.",
            "Straighten your head — pick a spot ahead and hold your gaze there.",
        ),
        VerdictCode.CHAIR_NOT_LOW_ENOUGH to listOf(
            "Sink your hips lower, like sitting into a chair.",
            "Bend your knees a bit more.",
        ),
        VerdictCode.CHAIR_KNEES_PAST_TOES to listOf(
            "Sit your hips back so your knees stay over your ankles.",
            "Shift your weight into your heels a touch.",
        ),
        VerdictCode.CHAIR_TORSO_TOO_FORWARD to listOf(
            "Lift your chest and keep your torso a bit more upright.",
            "Draw your chest up — less forward fold.",
        ),
        VerdictCode.CHAIR_ARMS_NOT_RAISED to listOf(
            "Reach your arms further overhead.",
            "Stretch both arms up toward the ceiling.",
        ),
        VerdictCode.CHAIR_ARMS_NOT_STRAIGHT to listOf(
            "Straighten your arms out through the elbows.",
            "Lengthen both arms overhead, no bend in the elbows.",
        ),
    )
    private val FIRM: Map<VerdictCode, List<String>> = mapOf(
        VerdictCode.FRONT_KNEE_TOO_STRAIGHT to listOf(
            "Bend that front knee — really commit to the angle.",
            "You'll need more bend than that — sink lower.",
        ),
        VerdictCode.FRONT_KNEE_OVER_BENT to listOf(
            "Come up out of that knee bend now.",
            "That's too deep — rise up a good amount.",
        ),
        VerdictCode.FRONT_KNEE_PAST_ANKLE to listOf(
            "Your knee is past your ankle — pull it back now.",
            "That knee's too far forward — bring it back over the ankle.",
        ),
        VerdictCode.BACK_LEG_BENT to listOf(
            "Lock that back leg out straight.",
            "Really straighten the back leg this time.",
        ),
        VerdictCode.ARMS_NOT_LEVEL to listOf(
            "Arms need to be level — reach out through both hands.",
            "Both arms all the way out, same height.",
        ),
        VerdictCode.ARMS_NOT_STRAIGHT to listOf(
            "Lock your arms straight — no bend in the elbows.",
        ),
        VerdictCode.TORSO_LEANING to listOf(
            "You're leaning — stack your shoulders over your hips.",
            "Straighten back up through the torso.",
        ),
        VerdictCode.INSUFFICIENT_VISIBILITY to listOf(
            "I still can't see you clearly — step back a bit more.",
            "Still out of frame — a couple more steps back should do it.",
            "Not quite — keep backing up until your whole body's in view.",
        ),
        VerdictCode.TREE_STANDING_LEG_BENT to listOf(
            "Your standing leg needs to be straight — lock it out now.",
            "Really press into that standing leg and straighten it.",
        ),
        VerdictCode.TREE_LIFTED_FOOT_TOO_LOW to listOf(
            "Your foot's slipping low — press it up higher, right now.",
        ),
        VerdictCode.TREE_HIPS_NOT_LEVEL to listOf(
            "Your hips are tilted — square them back to center now.",
        ),
        VerdictCode.TREE_ARMS_NOT_AT_CHEST to listOf(
            "Bring your hands all the way in to your chest.",
        ),
        VerdictCode.TREE_HEAD_TILTED to listOf(
            "Your head's tilted — level it out and fix your gaze on one spot.",
        ),
        VerdictCode.CHAIR_NOT_LOW_ENOUGH to listOf(
            "You need to sink lower — really bend those knees.",
        ),
        VerdictCode.CHAIR_KNEES_PAST_TOES to listOf(
            "Your knees are too far forward — sit your hips back now.",
        ),
        VerdictCode.CHAIR_TORSO_TOO_FORWARD to listOf(
            "You're folding too far forward — lift your chest up now.",
        ),
        VerdictCode.CHAIR_ARMS_NOT_RAISED to listOf(
            "Get those arms all the way overhead.",
        ),
        VerdictCode.CHAIR_ARMS_NOT_STRAIGHT to listOf(
            "Lock your arms straight overhead — no bend in the elbows.",
        ),
    )
    private val CONFIRM_IMPROVEMENT_LINES = listOf(
        "Nice, that's it.",
        "There you go, that's the shape.",
        "Beautiful, hold that.",
    )
    private const val FALLBACK_LINE = "Reset your position."

    private val STEP_COMPLETE_SWITCH_SIDES = listOf(
        "Great hold — relax and switch sides.",
        "Nicely done, ease out and let's do the other side.",
        "That's it, come out of the pose and switch sides.",
    )
    private val STEP_COMPLETE_NEXT_POSE = listOf(
        "Great work — take a breath, next pose coming up.",
        "Well held, relax for a moment before the next one.",
        "Nice job, take a quick breather.",
    )

    fun forIssue(code: VerdictCode, escalation: CueEscalation, repeatIndex: Int = 0): String =
        pick((if (escalation == CueEscalation.FIRM) FIRM else NORMAL)[code], repeatIndex) ?: FALLBACK_LINE

    fun forConfirmImprovement(repeatIndex: Int = 0): String = pick(CONFIRM_IMPROVEMENT_LINES, repeatIndex)!!

    fun forSafetyOverride(code: VerdictCode, repeatIndex: Int = 0): String =
        pick(FIRM[code], repeatIndex) ?: FALLBACK_LINE

    /** Spoken line for the rest transition between steps — feature-spec.md Section 8.6. */
    fun forStepComplete(isSwitchingSides: Boolean, variantSeed: Int = 0): String =
        pick(if (isSwitchingSides) STEP_COMPLETE_SWITCH_SIDES else STEP_COMPLETE_NEXT_POSE, variantSeed)!!

    private fun pick(variants: List<String>?, repeatIndex: Int): String? {
        if (variants.isNullOrEmpty()) return null
        val index = repeatIndex.mod(variants.size)
        return variants[index]
    }
}
