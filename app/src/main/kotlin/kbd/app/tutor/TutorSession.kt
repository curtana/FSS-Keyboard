package kbd.app.tutor

import kbd.stroke.threshold.Thresholds

/**
 * A step whose target actions take more than one stroke (가 = ㄱ tap + swipe, ㄲ = tap tap,
 * LLD-07 §2.1). The session feeds the strokes of one action in order; [extract] gets the action's
 * strokes so far (the last one is the new stroke).
 */
interface MultiStrokeStep : TutorStep {
    fun strokesPerAction(target: TargetAction): Int

    fun extract(action: List<RawStroke>, target: TargetAction, current: Thresholds): List<Sample>

    override fun extract(raw: RawStroke, target: TargetAction, current: Thresholds): List<Sample> =
        extract(listOf(raw), target, current)
}

/**
 * Lesson 5 (LLD-07 §2.2 5-1): each target is a sentence typed freely and ended with [다음]. Nothing
 * is measured (typing speed and accuracy are shown only, SWIPE 10 5-1, issue #150).
 */
interface SentenceStep : TutorStep

/**
 * Runs the tutor steps in order (LLD-07 §2.1–2.2, HLD 7.1). Samples of finished steps stay in a
 * pool so later steps can use them (3-1 uses 1-2's strokes, 3-2 checks 1-1, D-19). Targets of a
 * step are asked in turn; practice-only targets ([TutorStep.practiceOnly]) are not counted.
 *
 * Judged items (LLD-07 §2.5, FR-TUT-12, issue #154): every released stroke is judged against its turn
 * of the item ([TutorItemJudge]). A wrong try ([Judgement.Wrong]) drops its samples and the same item
 * is asked again; a right one ([Judgement.Right]) keeps them and moves on, and only right tries count.
 * Tap or swipe and the first edge are decided at [start] — the values the echo uses (FR-TUT-09) — not
 * at the values the steps estimate.
 *
 * Practice consonants rotate (issue #168, SWIPE 10장): an item whose step [TutorStep.rotates] it gets
 * the next of [HowTo.ROTATION] each time it is asked (the same one when it pairs with the item before,
 * and when it is asked again after ✗); the count starts over at ㄱ for every step and every try of a
 * step. [currentTarget] is that concrete item ([TargetAction.template] names the step's item).
 */
class TutorSession(private val steps: List<TutorStep>, private val start: Thresholds) {

    /** How a field is estimated from the pooled samples. */
    private enum class Rule { BOUNDARY }

    var current: Thresholds = start
        private set

    private var stepIndex = 0
    private val pool = ArrayList<Sample>()
    private val attempt = ArrayList<Sample>()
    private val action = ArrayList<RawStroke>()
    private val records = ArrayList<ActionRecord>()
    private val trySamples = ArrayList<Sample>()
    private val sentence = ArrayList<RawStroke>()
    private var actions = 0
    private var targetIndex = 0
    private var keySize: Pair<Float, Float>? = null

    /** Rotating items asked so far in this try of the step, and the consonant of the item asked now. */
    private var rotation = 0
    private var consonant: String? = null

    /** True after the last step was accepted; only then is [current] saved (FR-TUT-04, 05). */
    val done: Boolean get() = stepIndex >= steps.size

    val currentStep: TutorStep? get() = steps.getOrNull(stepIndex)

    /** The action to ask for next: the step's item with this turn's practice consonant. */
    val currentTarget: TargetAction?
        get() {
            val step = currentStep ?: return null
            val base = step.targets[targetIndex]
            val c = consonant ?: return base
            return step.withConsonant(base, c)
        }

    val progress: StepProgress
        get() = StepProgress(actions, currentStep?.let { s -> s.targets.count { !s.practiceOnly(it) } * s.minSamplesPerTarget } ?: 0)

    /** Strokes of the sentence in progress (lesson 5), for its time span. */
    val sentenceStrokes: List<RawStroke> get() = sentence

    /** The judgement after the last stroke; null = not judged (sentences, items with no [ItemSpec]). */
    var judgement: Judgement? = null
        private set

    /** The item [judgement] is about (it stays the asked item after `Wrong`; after `Right` the next one is asked). */
    var judgedItem: ItemSpec? = null
        private set

    /** The last `Wrong` was a vowel stroke drawn ← or ↑ (the how-to gets [HowTo.REVERSE_HINT]). */
    var reversedLast = false
        private set

    /**
     * The 1-2 record (FR-TUT-13, HLD 7.1–7.2 `calibration`): set when 1-2 is accepted; never judged with.
     * Its `at` is empty; the activity stamps the time when it saves.
     */
    var calibration: kbd.app.store.Calibration? = null
        private set

    /** The practice keyboard's place C (TBD-24): the flow sets it from the echo; 3-1 is judged against it (SWIPE 10.1). */
    var consonantBox: kbd.hangul.stroke.ConsonantBox? = null

    /** Extra rounds of the current step asked for by overlaps (at most [MAX_EXTRA_ROUNDS], SWIPE 10). */
    private var extraRounds = 0


    fun onStroke(raw: RawStroke): StepProgress {
        val step = checkNotNull(currentStep) { "tutor is finished" }
        val target = checkNotNull(currentTarget)
        // The judgement is about this stroke only: a lesson-5 stroke must not carry the last judged
        // step's ✓, or the flow empties the echo on every press (issue #163).
        judgement = null
        judgedItem = null
        reversedLast = false
        // HLD 7.1: the key size at measurement time is stored with the values (consonant keys only).
        if (!raw.space && !raw.backspace) keySize = (raw.keyRect.right - raw.keyRect.left) to (raw.keyRect.bottom - raw.keyRect.top)
        if (step is SentenceStep) {
            sentence += raw
            return progress
        }
        action += raw
        trySamples += if (step is MultiStrokeStep) step.extract(action.toList(), target, current)
        else step.extract(raw, target, current)
        val spec = step.itemSpec(target)
        judgedItem = spec
        if (spec == null) {
            val perAction = if (step is MultiStrokeStep) step.strokesPerAction(target) else 1
            if (action.size >= perAction) rightAction(step)
            return progress
        }
        val rec = TutorItemJudge.record(raw, start)
        records += rec
        val j = TutorItemJudge.judge(spec, records, start, consonantBox)
        judgement = j
        // SWIPE 10.1 덧붙임: a vowel stroke turn answered ← / ↑ gets the "down and right only" line.
        val turn = spec.actions[records.size - 1]
        reversedLast = j == Judgement.Wrong && (turn is ExpectedAction.Stroke || turn is ExpectedAction.Written) &&
            rec.vertical != null && !rec.forward
        when (j) {
            Judgement.Pending -> Unit
            Judgement.Right -> rightAction(step)
            Judgement.Wrong -> clearTry()   // FR-TUT-12: the samples are dropped, the same item (and consonant) again
        }
        return progress
    }

    /** The try's samples are kept and the next item is asked (counted unless practice only). */
    private fun rightAction(step: TutorStep) {
        attempt += trySamples
        nextAction(step)
    }

    private fun clearTry() {
        action.clear()
        records.clear()
        trySamples.clear()
    }

    /** Lesson 5 [다음]: the next sentence is asked. */
    fun endSentence(): StepProgress {
        val step = checkNotNull(currentStep as? SentenceStep) { "not a sentence step" }
        nextAction(step)
        return progress
    }

    private fun nextAction(step: TutorStep) {
        clearTry()
        sentence.clear()
        if (!step.practiceOnly(step.targets[targetIndex])) actions++
        targetIndex = (targetIndex + 1) % step.targets.size
        pickConsonant()
    }

    /** The practice consonant of the item asked now (issue #168): the next one, the pair's, or none. */
    private fun pickConsonant() {
        val step = currentStep ?: return run { consonant = null }
        val base = step.targets[targetIndex]
        consonant = when {
            !step.rotates(base) -> null
            step.pairedWithPrevious(base) && consonant != null -> consonant
            else -> HowTo.ROTATION[rotation++ % HowTo.ROTATION.size]
        }
    }

    init {
        pickConsonant()
    }

    /**
     * Estimates the fields this step sets. `NotEnough` keeps the step and its samples; `Overlap` drops this
     * attempt's samples so the step is done again (FR-TUT-03) — at most [MAX_EXTRA_ROUNDS] times; after
     * that the formula's value is taken anyway and the overlap stays in [calibration] (SWIPE 10 "두 바퀴까지").
     * `Accepted` moves on. A step that sets nothing (3-1, 3-4, 5-1: practice only) is always accepted.
     */
    fun finishStep(): StepOutcome {
        val step = checkNotNull(currentStep) { "tutor is finished" }
        val samples = pool + attempt
        var next = current
        var overlap: ThresholdField? = null
        for ((field, rule) in rulesOf(step.id)) {
            val r = estimate(field, rule, samples)
            // Fixed minimum crossing distance (issue #217): enough samples are still asked for, but the value never
            // changes and an overlap of taps and dots does not repeat the step (it stays in the record).
            if (field == ThresholdField.MIN_CROSS) {
                if (r == EstimateResult.NotEnough) return StepOutcome.NotEnough
                continue
            }
            when (r) {
                EstimateResult.NotEnough -> return StepOutcome.NotEnough
                EstimateResult.Overlap -> {
                    if (overlap == null) overlap = field
                    next = next.with(field, ThresholdEstimator.midpoint(side(samples, field, SampleSide.SHORT), side(samples, field, SampleSide.LONG)))
                }
                is EstimateResult.Ok -> next = next.with(field, r.value)
            }
        }
        if (overlap != null && extraRounds < MAX_EXTRA_ROUNDS) {
            extraRounds++
            restartStep()
            return StepOutcome.Overlap(overlap)
        }
        if (step.id == "1-2") calibration = calibrationOf(samples)
        keySize?.let { (w, h) -> next = next.copy(keySizeWDp = w, keySizeHDp = h) }
        current = next
        pool += attempt
        restartStep()
        stepIndex++
        extraRounds = 0
        rotation = 0   // the next step starts its rotation at ㄱ (SWIPE 10장); restartStep counted on the old step
        consonant = null
        pickConsonant()
        return StepOutcome.Accepted(current)
    }

    /** FR-TUT-13: both boundaries' P90 / P10 / gap / counts with this step's extra rounds, and the position errors. */
    private fun calibrationOf(samples: List<Sample>): kbd.app.store.Calibration {
        fun record(field: ThresholdField): kbd.app.store.BoundaryRecord? {
            val short = side(samples, field, SampleSide.SHORT)
            val long = side(samples, field, SampleSide.LONG)
            if (short.isEmpty() || long.isEmpty()) return null
            val s90 = ThresholdEstimator.percentile(short, 90f)
            val l10 = ThresholdEstimator.percentile(long, 10f)
            return kbd.app.store.BoundaryRecord(s90, l10, l10 - s90, short.size, long.size, extraRounds)
        }
        val pos = samples.filter { it.field == ThresholdField.POSITION }.map { it.value }
        return kbd.app.store.Calibration(
            tapDot = record(ThresholdField.MIN_CROSS),
            dotLong = record(ThresholdField.LONG),
            position = if (pos.isEmpty()) null else kbd.app.store.PositionRecord(
                ThresholdEstimator.percentile(pos, 90f), ThresholdEstimator.percentile(pos, 95f), pos.size),
            at = "",
        )
    }

    private fun side(samples: List<Sample>, field: ThresholdField, s: SampleSide) =
        samples.filter { it.field == field && it.side == s }.map { it.value }

    private fun restartStep() {
        attempt.clear()
        clearTry()
        sentence.clear()
        actions = 0
        targetIndex = 0
        rotation = 0
        consonant = null
        pickConsonant()
    }

    private fun estimate(field: ThresholdField, rule: Rule, samples: List<Sample>): EstimateResult {
        fun side(s: SampleSide) = samples.filter { it.field == field && it.side == s }.map { it.value }
        return when (rule) {
            Rule.BOUNDARY -> ThresholdEstimator.boundary(side(SampleSide.SHORT), side(SampleSide.LONG))
        }
    }

    /**
     * Fields each SWIPE 10 step measures and the formula (LLD-07 §2.2 table, HLD 7.1–7.2, issue #173): 1-1 only
     * gathers taps; 1-2 the minimum crossing distance (taps ↔ dots) and the length threshold (dots ↔ long strokes).
     * The public 1-1 one-sided formula went with the public edition (issue #251). The minimum crossing distance is fixed (owner decision, issue #217):
     * its rule only asks for enough samples and feeds the record (FR-TUT-13); see [finishStep].
     */
    private fun rulesOf(stepId: String): List<Pair<ThresholdField, Rule>> = when (stepId) {
        "1-2" -> listOf(ThresholdField.MIN_CROSS to Rule.BOUNDARY, ThresholdField.LONG to Rule.BOUNDARY)
        else -> emptyList()   // 1-1 (taps only), 3-1, 3-4, 5-1: practice only (issue #173: no one-stroke vowel measures; 3-2 removed)
    }

    private fun Thresholds.with(field: ThresholdField, v: Float): Thresholds = when (field) {
        ThresholdField.MIN_CROSS -> this   // fixed (issue #217)
        ThresholdField.LONG -> copy(longDp = v)
        // LLD-07 §3: ms values are stored rounded to Long, inside the stored range (§4).
        ThresholdField.GUIDE_DELAY -> copy(guideDelayMs = ThresholdEstimator.toStoredMs(v))
        ThresholdField.POSITION -> this   // recorded only (FR-TUT-13)
    }

    companion object {
        /** SWIPE 10: an overlap asks for the step again at most twice. */
        const val MAX_EXTRA_ROUNDS = 2
    }
}
