package kbd.app.tutor

import kbd.stroke.geom.RectDp
import kbd.stroke.geom.StrokePoint
import kbd.stroke.threshold.Thresholds

/** Short or long side of a boundary (LLD-07 §2.1). */
enum class SampleSide { SHORT, LONG }

/** One measured value for a threshold field (LLD-07 §2.1). */
data class Sample(val field: ThresholdField, val side: SampleSide, val value: Float)

/** The stored thresholds (LLD-07 §2.1, HLD 7.2 format 3). */
enum class ThresholdField {
    MIN_CROSS, LONG, GUIDE_DELAY,   // issue #173: ADD_I, RETURN_LONG, TURN_ANGLE went with the one-stroke vowels
    /** 1-2's second-stroke position error (FR-TUT-13): recorded in `calibration`, never estimated. */
    POSITION,
}

/** Layout a tutor step practises on. [SWIPE_SENTENCE] = the swipe consonant keys plus the space and ⌫ keys (lesson 5, ⌫ since issue #165). */
enum class PracticeLayout { SWIPE_CONSONANT_KEY, SWIPE_SENTENCE }   // QWERTY_LETTER_KEY (public 1-1) went with issue #251

/**
 * Action the user is asked to perform, e.g. "가", "그", or a sentence in lesson 5. [template] names the
 * step's item it was made from: with a rotating practice consonant (issue #168) the label is "나"
 * while the template stays "가".
 */
data class TargetAction(val label: String, val template: String = label)

/** Actions done so far in the current step / actions needed. */
data class StepProgress(val collected: Int, val needed: Int)

/** Result of finishing a tutor step. */
sealed interface StepOutcome {
    data class Accepted(val thresholds: Thresholds) : StepOutcome
    data class Overlap(val field: ThresholdField) : StepOutcome
    data object NotEnough : StepOutcome
}

/** One step of a tutor lesson (LLD-07 §2.1–2.2, SWIPE 10). */
interface TutorStep {
    /** SWIPE 10 step, "lesson-step" ("1-1", "5-1"). */
    val id: String

    /** The lesson number, the part of [id] before the dash. */
    val lesson: Int get() = id.substringBefore('-').toInt()

    /** String resource of the lesson explanation shown before this step (the first step of a lesson); 0 = none. */
    val intro: Int get() = 0

    /** String resource describing the action to perform. */
    val prompt: Int

    /** Swipe consonant keys, swipe sentence keys or QWERTY letter keys. */
    val practiceLayout: PracticeLayout

    /** Actions the user is asked to perform. */
    val targets: List<TargetAction>

    /** Default 10 (D-18). */
    val minSamplesPerTarget: Int

    /**
     * A practice-only item (1-2 아·오·이, issue #150): asked in its turn but neither measured nor
     * counted towards the step's progress, so the step ends as it would without it.
     */
    fun practiceOnly(target: TargetAction): Boolean = false

    /** The item takes the rotating practice consonant (issue #168, SWIPE 10장); see [TutorSession]. */
    fun rotates(target: TargetAction): Boolean = false

    /** The item is the second of a pair (가 / 그) and keeps the consonant of the item before it. */
    fun pairedWithPrevious(target: TargetAction): Boolean = false

    /** [target] written with practice consonant [consonant] (가 + ㄴ = 나), its template kept. */
    fun withConsonant(target: TargetAction, consonant: String): TargetAction = target

    /**
     * How [target] is judged (FR-TUT-12, LLD-07 §2.5); null = not judged (lesson 5 sentences): the
     * action then ends after its strokes and its samples are always kept.
     */
    fun itemSpec(target: TargetAction): ItemSpec? = null

    fun extract(raw: RawStroke, target: TargetAction, current: Thresholds): List<Sample>
}

/**
 * One practice stroke on a key. [prevUpTime] = previous stroke's UP time, null if none.
 * [space] = the stroke was on the space key, [backspace] on ⌫ (lesson 5 sentences only, issue #165). [key] = the label of the key
 * it starts on ("ㄱ", "q"), for the item judgement (LLD-07 §2.5).
 */
data class RawStroke(
    val keyRect: RectDp,
    val points: List<StrokePoint>,
    val prevUpTime: Long?,
    val space: Boolean = false,
    val key: String = "",
    val backspace: Boolean = false,
)
