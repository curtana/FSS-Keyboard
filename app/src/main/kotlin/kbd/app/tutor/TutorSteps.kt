package kbd.app.tutor

import kbd.app.R
import kbd.app.tutor.ThresholdField.LONG
import kotlin.math.abs
import kbd.app.tutor.ThresholdField.MIN_CROSS
import kbd.hangul.cheonjiin.CjElement
import kbd.stroke.threshold.Thresholds

/**
 * Tutor lessons 1, 3 and 5 on the swipe keyboard (LLD-07 §2.2, SWIPE 10, issues #127,
 * #150, #168; shown as lessons 1, 2 and 3). Each lesson starts with an explanation (the step's [TutorStep.intro]); its items are written
 * in a fixed alternating order, pair partner next to each other, every way of writing as its own
 * item, and cycled until the step has enough samples. Prompts only teach how to write; samples are
 * taken silently. Samples carry the side the user **intended** (SHORT = normal / fast, LONG =
 * intended long / normal pace).
 *
 * Every syllable is written as on the keyboard (issue #168): one swipe on its consonant's key types
 * that consonant and the vowel (every vowel is one stroke, issue #156). The practice consonant
 * rotates through the 19 consonants ([HowTo.ROTATION]); a pair (가 / 그) keeps one consonant; 아·오·이
 * stay on ㅇ and 1-1 keeps the 14 basic consonants. Every item is judged by its spec (SWIPE 10.1,
 * LLD-07 §2.5): the measurement steps by key, tap or swipe and the first edge, 3-4 by result.
 */
object TutorSteps {

    /** One raw value a stroke contributes, with its field and intended side. */
    enum class Measure {
        /** 1-1: tap jitter. */
        JITTER,
        /** 1-2 (issue #173): a dot's length (tap ↔ dot and dot ↔ long), a long stroke's length (dot ↔ long). */
        DOT, LONG_STROKE,
    }

    /**
     * One practice item: the [label] (its template, e.g. "가"), how a concrete item is judged ([specOf],
     * one expected action per stroke) and one measure list per stroke; [practiceOnly] items are not
     * counted. [rotates]: the practice consonant rotates; [paired]: keeps the previous item's consonant.
     */
    class Script(
        val label: String,
        val specOf: (String) -> ItemSpec,
        vararg strokes: List<Measure>,
        val practiceOnly: Boolean = false,
        val rotates: Boolean = false,
        val paired: Boolean = false,
        /** 1-2's position error (FR-TUT-13): the second stroke's start from its reference place, given the two strokes. */
        val position: ((RawStroke, RawStroke) -> Float)? = null,
    ) {
        val strokes: List<List<Measure>> = strokes.toList()

        init {
            val n = specOf(label).actions.size
            require(n == this.strokes.size) { "$label: $n judged actions for ${this.strokes.size} strokes" }
        }
    }

    /** A step built from scripts; items are asked in list order, cycling (LLD-07 §2.2). */
    class ScriptedStep(
        override val id: String,
        override val prompt: Int,
        private val scripts: List<Script>,
        override val minSamplesPerTarget: Int = ThresholdEstimator.MIN_SAMPLES,
        override val intro: Int = 0,
    ) : MultiStrokeStep {
        override val practiceLayout = PracticeLayout.SWIPE_CONSONANT_KEY
        override val targets = scripts.map { TargetAction(it.label) }

        private fun script(target: TargetAction) = scripts.first { it.label == target.template }

        override fun strokesPerAction(target: TargetAction): Int = script(target).strokes.size

        override fun practiceOnly(target: TargetAction): Boolean = script(target).practiceOnly

        override fun itemSpec(target: TargetAction): ItemSpec = script(target).specOf(target.label)

        override fun rotates(target: TargetAction): Boolean = script(target).rotates

        override fun pairedWithPrevious(target: TargetAction): Boolean = script(target).paired

        override fun withConsonant(target: TargetAction, consonant: String): TargetAction =
            TargetAction(HowTo.withInitial(target.template, consonant), target.template)

        override fun extract(action: List<RawStroke>, target: TargetAction, current: Thresholds): List<Sample> {
            val raw = action.last()
            val sc = script(target)
            val pos = if (action.size == 3) sc.position?.invoke(action[1], action[2])?.let { Sample(ThresholdField.POSITION, SampleSide.SHORT, it) } else null
            return sc.strokes.getOrNull(action.size - 1).orEmpty().flatMap { measure(it, raw, current) } + listOfNotNull(pos)
        }
    }

    /** Lesson 5: sentences ended with [다음]; typing speed and accuracy only, nothing measured (issue #150). */
    class SentencesStep(override val id: String, override val prompt: Int, sentences: List<String>, override val intro: Int = 0) : SentenceStep {
        override val practiceLayout = PracticeLayout.SWIPE_SENTENCE
        override val targets = sentences.map { TargetAction(it) }
        override val minSamplesPerTarget = 1

        override fun extract(raw: RawStroke, target: TargetAction, current: Thresholds): List<Sample> = emptyList()
    }

    private fun measure(m: Measure, raw: RawStroke, th: Thresholds): List<Sample> {
        fun s(field: ThresholdField, side: SampleSide, v: Float?) = listOfNotNull(v?.let { Sample(field, side, it) })
        val exit by lazy { TutorMeasure.firstExit(raw, th.minCrossDp) }
        return when (m) {
            Measure.JITTER -> s(MIN_CROSS, SampleSide.SHORT, TutorMeasure.tapSample(raw))   // displacement, as the keyboard decides (HLD 7.1)
            Measure.DOT -> TutorMeasure.length(raw).let { listOf(Sample(MIN_CROSS, SampleSide.LONG, it), Sample(LONG, SampleSide.SHORT, it)) }
            Measure.LONG_STROKE -> s(LONG, SampleSide.LONG, TutorMeasure.length(raw))
        }
    }

    private fun strokes(vararg m: Measure) = listOf(*m)

    /** A stroke that gives no sample (3-4's taps and elements). */
    private val TAP = emptyList<Measure>()

    /** The 14 basic consonant keys in the order 1-1 asks for them (issue #144; the doubles are not added, PM decision #168). */
    val CONSONANTS = listOf("ㄱ", "ㄴ", "ㄷ", "ㄹ", "ㅁ", "ㅂ", "ㅅ", "ㅇ", "ㅈ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ")

    // 1-1 (issue #144): every consonant key tapped once, in turn; each tap is one jitter sample (14 ≥ MIN_SAMPLES).
    val STEP_1_1 = ScriptedStep("1-1", R.string.tutor_prompt_1_1, CONSONANTS.map { Script(it, HowTo::press, strokes(Measure.JITTER)) },
        minSamplesPerTarget = 1, intro = R.string.tutor_lesson_1_intro)

    /** The measures of each stroke of [glyph]'s vowel: its tap (jitter), then a dot or a long stroke per stroke (issue #173). */
    private fun strokeMeasures(glyph: String): Array<List<Measure>> {
        val steps = checkNotNull(HowTo.STROKES[HowTo.split(glyph).second])
        return arrayOf(strokes(Measure.JITTER)) + steps.map { strokes(if (it.long) Measure.LONG_STROKE else Measure.DOT) }.toTypedArray()
    }

    private fun RawStroke.start() = points.first()
    private fun RawStroke.end() = points.last()

    /** 1-2's reference places of the second stroke (SWIPE 10, FR-TUT-13): ㅏ ㅣ's line, ㅓ the dot's end, ㅗ the dot's bottom, ㅜ ㅡ's line. */
    private val POSITION: Map<String, (RawStroke, RawStroke) -> Float> = mapOf(
        "가" to { a, b -> abs(b.start().x - a.start().x) },
        "거" to { a, b -> abs(b.start().x - a.end().x) },
        "고" to { a, b -> abs(b.start().y - a.end().y) },
        "구" to { a, b -> abs(b.start().y - a.start().y) },
    )

    /**
     * A 1-2 item (issue #173): press the consonant key, then the vowel's strokes, judged by axis and
     * direction only; rotating unless [fixed] (아·오·이 stay on ㅇ); [paired] keeps the previous item's consonant.
     */
    private fun measured(label: String, practiceOnly: Boolean = false, paired: Boolean = false, fixed: Boolean = false) =
        Script(label, { HowTo.measured(it) }, *(if (practiceOnly) strokeMeasures(label).map { emptyList<Measure>() }.toTypedArray() else strokeMeasures(label)),
            practiceOnly = practiceOnly, rotates = !fixed, paired = paired, position = if (practiceOnly) null else POSITION[label])

    // 1-2 (SWIPE 10): 가 / 거 / 고 / 구, then the pair 그 / 기 (기 keeps 그's consonant).
    private val STEP_1_2_ITEMS = listOf(measured("가"), measured("거"), measured("고"), measured("구"), measured("그"), measured("기", paired = true))

    // 아·오·이 (PM decision, issue #150): the ㅇ tap and the strokes; practice only, the last items of each round, never rotated.
    private val VOWEL_FIRST = listOf("아", "오", "이").map { measured(it, practiceOnly = true, fixed = true) }
    val STEP_1_2 = ScriptedStep("1-2", R.string.tutor_prompt_1_2, STEP_1_2_ITEMS + VOWEL_FIRST, minSamplesPerTarget = 5)

    // 2-1 (double tap) was removed with the double tap: the double consonants have their own keys (issue #168).

    /**
     * 3-1 (issue #173, SWIPE 10): the compound and y-vowels in stroke order, measuring nothing and judged
     * by result (the strokes the assembler takes and the vowel they leave); one round of right items ends it.
     */
    val STEP_3_1 = ScriptedStep("3-1", R.string.tutor_prompt_3_1,
        listOf("갸", "겨", "교", "규", "개", "게", "걔", "계", "과", "괘", "괴", "궈", "궤", "귀", "긔").map { g ->
            Script(g, { HowTo.written(it) }, *strokeMeasures(g).map { emptyList<Measure>() }.toTypedArray(), rotates = true)
        },
        minSamplesPerTarget = 1, intro = R.string.tutor_lesson_3_intro)

    // 3-2 (rubbing) was removed with the rubs: vowels are written stroke by stroke (issue #173).

    // 3-4 (issue #160, PM): the cheonjiin key, one item — 가 = ㄱ tap, cheonjiin ↑ (ㅣ), cheonjiin tap (ㆍ).
    // Practice only in the sense of measuring nothing, but counted: one right try ends the step.
    val STEP_3_4 = ScriptedStep("3-4", R.string.tutor_prompt_3_4, listOf(
        Script("가 — 천지인 키", { HowTo.cheonjiin("가", listOf(CjElement.I, CjElement.DOT)) }, TAP, TAP, TAP),
    ), minSamplesPerTarget = 1)

    val STEP_5_1 = SentencesStep("5-1", R.string.tutor_prompt_5_1, listOf(
        "아이가 고기를 먹어요",
        "밖까지 과일을 사러 가요",
        "박가가 곡기를 끊었다",
        "안녕하세요 학교에 갔다 왔어요",
    ), intro = R.string.tutor_lesson_5_intro)

    /** Lessons 1, 3 and 5 in SWIPE 10 order, shown as 1, 2, 3 (lesson 4 removed, issue #150; 2 removed, issue #168; 3-3 removed, issue #156; 3-4 added, issue #160; LLD-03 §2.2 `tutorSteps()`). */
    val ALL: List<TutorStep> = listOf(STEP_1_1, STEP_1_2, STEP_3_1, STEP_3_4, STEP_5_1)   // 3-2 removed, issue #173
}
