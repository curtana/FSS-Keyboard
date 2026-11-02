package kbd.app.tutor

import android.media.AudioManager
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kbd.app.R
import kbd.app.feedback.Feedback
import kbd.app.settings.AppStores
import kbd.app.settings.KeyboardAppTheme
import kbd.app.swipe.SwipeModuleProvider
import kbd.stroke.threshold.Thresholds

/**
 * Drives a [TutorSession] for the tutor screen (LLD-07 §2.3). A lesson starts with its explanation
 * ([intro], until [startPractice]); a step ends when its samples are collected or on the step-end
 * button; `Overlap` shows the overlap message and repeats the step (FR-TUT-03); after the last step
 * the values are saved once (FR-TUT-04). Skipping saves nothing (FR-TUT-05).
 * The echo comes from a [PracticeEcho] made by [echoOf] for each try of a step, and for each
 * sentence of lesson 5, so it is empty again then (FR-TUT-09): lesson 5 shows it as the "입력: …"
 * line, the other steps show only its last character ([echoChar]). Lesson 5 sentences end with
 * [nextSentence], which shows the sentence's typing speed and accuracy (FR-TUT-10).
 *
 * Item judgement (FR-TUT-12, LLD-07 §2.5, issue #154): after a judged item's last action the flow
 * shows ✓ ([verdict] `Right`, the echo becomes the item's glyph: every judged step is a measurement
 * step, SRS TBD-17 = A) or ✗ ([verdict] `Wrong`, the [howTo] line, the same item again). ✓/✗ and
 * the echo stay until the next practice stroke is pressed ([onPress]); then the echo starts empty, so
 * a try never continues the previous one. [howTo] stays until the item is right. [errorVibe] runs
 * when the echo ignored the stroke (a cheonjiin element that cannot extend, FR-SWP-62; issue #168).
 */
class TutorFlow(
    private val session: TutorSession,
    private val echoOf: (PracticeLayout) -> PracticeEcho? = { null },
    private val errorVibe: () -> Unit = {},
    private val save: (Thresholds) -> Unit,
) {

    /** Vowel strokes the echo holds open: the practice keyboard's trail (SWIPE 9.4) keeps that many. */
    val openStrokes: Int get() = echo?.openStrokes ?: 0

    /** The echo's center consonant and its place: the practice keyboard's center consonant (SWIPE 9.5). */
    val center: kbd.app.view.CenterMark? get() = echo?.center

    /** The echo's assembler preview for the practice guide (SWIPE 9). */
    fun preview(s: kbd.hangul.stroke.VowelStroke): String? = echo?.preview(s)

    /**
     * OVERLAP_LENGTH: the 1-2 dot ↔ long-stroke record overlapped (its hint only says to write by the drawn consonant's
     * places; the measurement itself goes with format 6, issue #194). The taps ↔ strokes overlap message went with the
     * fixed minimum crossing distance (issue #217: it never overlaps any more).
     */
    enum class Message { NONE, OVERLAP_LENGTH, NOT_ENOUGH }

    var message = Message.NONE
        private set

    val done: Boolean get() = session.done
    val step: TutorStep? get() = session.currentStep
    val target: TargetAction? get() = session.currentTarget
    val progress: StepProgress get() = session.progress

    /** Steps whose lesson explanation was already shown (a repeated step does not show it again). */
    private val introShown = HashSet<String>()

    /** The lesson explanation to show now, or null: practice is on. */
    val intro: Int?
        get() = step?.takeIf { it.intro != 0 && it.id !in introShown }?.intro

    /** The lesson-5 sentence results so far (cleared when lesson 5 is done again). */
    val sentenceStats: List<SentenceStats> get() = stats

    /** The last finished sentence's result, shown until the next stroke. */
    var lastSentence: SentenceStats? = null
        private set

    /** Overall lesson-5 result; null when no sentence was finished. */
    val overall: OverallStats? get() = if (stats.isEmpty()) null else TypingStats.overall(stats)

    private val stats = ArrayList<SentenceStats>()
    private var echo: PracticeEcho? = newEcho()

    /** ✓ (`Right`) or ✗ (`Wrong`) of the last judged item, until the next stroke is pressed; null = none. */
    var verdict: Judgement? = null
        private set

    /** "<목표> = <동작> → <동작>" after a ✗, until the item is right (SWIPE 10.1); null = none. */
    var howTo: String? = null
        private set

    /** The item's glyph shown in place of the echo after ✓ (SRS TBD-17 = A). */
    private var shownGlyph: String? = null

    /** The "입력: …" text; only its last [ECHO_MAX] characters when longer (FR-TUT-09). */
    val echoText: String
        get() {
            val s = echo?.text ?: return ""
            return if (s.length <= ECHO_MAX) s else "…" + s.takeLast(ECHO_MAX - 1)
        }

    /** Lesson 5 (sentences) shows the "입력: …" line ([echoText]); the other steps show [echoChar] large (FR-TUT-09, #143). */
    val echoAsLine: Boolean get() = session.currentStep is SentenceStep

    /**
     * The last character the practice strokes typed, for steps that are not sentences: the syllable
     * being composed, else the last committed one;
     * "" when nothing was typed in this try. Not joined to earlier ones (FR-TUT-09, #143).
     */
    val echoChar: String get() = shownGlyph ?: lastChar(echo?.text ?: "")

    /** The explanation was read: practice starts. */
    fun startPractice() {
        step?.let { introShown += it.id }
    }

    /**
     * A practice stroke is pressed: a shown ✓/✗ and its echo go away and the echo starts empty, so the
     * next item or the retry starts with no cheonjiin assembly or syllable (FR-TUT-12).
     */
    fun onPress() {
        if (verdict == null) return
        verdict = null
        shownGlyph = null
        echo = newEcho()
    }

    /** [widthDp] = practice keyboard width, for the echo's key geometry; measurement uses [raw] only. */
    fun onStroke(raw: RawStroke, widthDp: Float = 0f) {
        if (session.done || intro != null) return
        onPress()   // when the press was not reported (tests)
        lastSentence = null
        // Display only: a stroke the echo cannot read shows nothing, as a failed stroke types nothing.
        if (widthDp > 0f) echo?.let { e -> runCatching { e.onStroke(raw, widthDp) } }
        // As the keyboard: an ignored stroke or cheonjiin element, or unfinished strokes dropped (SWIPE 10.1 오류 진동).
        if (echo?.lastIgnored == true) errorVibe()
        val before = session.progress.collected
        session.consonantBox = echo?.consonantPlace   // 3-1 is judged as the echo reads it (against C, SWIPE 10.1)
        val p = session.onStroke(raw)
        val item = session.judgedItem
        when (val j = session.judgement) {
            Judgement.Right -> {
                verdict = j
                shownGlyph = item?.glyph
                howTo = null
            }
            Judgement.Wrong -> {
                verdict = j
                // SWIPE 10.1 덧붙임: after a ← / ↑ stroke, the "down and right only" line.
                howTo = item?.howTo?.let { if (session.reversedLast) "$it\n${HowTo.REVERSE_HINT}" else it }
            }
            Judgement.Pending, null -> Unit
        }
        if (p.collected != before && p.collected >= p.needed) finishStep()
    }

    /** Lesson 5 [다음]: the sentence's speed and accuracy, then the next sentence (or the end of the step). */
    fun nextSentence() {
        if (session.currentStep !is SentenceStep) return
        val strokes = session.sentenceStrokes
        val millis = if (strokes.isEmpty()) 0L else strokes.last().points.last().t - strokes.first().points.first().t
        val s = TypingStats.sentence(checkNotNull(session.currentTarget).label, echo?.text ?: "", millis)
        stats += s
        lastSentence = s
        val p = session.endSentence()
        echo = newEcho()   // FR-TUT-09: the line is emptied for each sentence
        if (p.collected >= p.needed) finishStep()
    }

    fun finishStep() {
        if (session.done) return
        val wasSentences = session.currentStep is SentenceStep
        val outcome = session.finishStep()
        if (outcome !is StepOutcome.NotEnough) {   // a new try of a step: no how-to line from the old one
            howTo = null
        }
        message = when (outcome) {
            is StepOutcome.Overlap -> {
                if (wasSentences) stats.clear()
                echo = newEcho()   // the step is done again
                Message.OVERLAP_LENGTH   // only LONG can overlap (MIN_CROSS is fixed, issue #217)
            }
            StepOutcome.NotEnough -> Message.NOT_ENOUGH
            is StepOutcome.Accepted -> {
                if (session.done) save(session.current)
                echo = newEcho()   // next step (none after the last)
                Message.NONE
            }
        }
    }

    private fun newEcho(): PracticeEcho? = session.currentStep?.let { echoOf(it.practiceLayout) }

    companion object {
        /** Characters of the "입력: …" line kept when it gets long (FR-TUT-09: only the tail shows). */
        const val ECHO_MAX = 20

        /** A step id with its lesson in screen numbering (issue #168): 1-2 → 1-2, 3-1 → 2-1, 5-1 → 3-1. */
        fun screenId(id: String): String {
            val lesson = id.substringBefore('-').toInt()
            val shown = when (lesson) { 3 -> 2; 5 -> 3; else -> lesson }
            return "$shown-${id.substringAfter('-')}"
        }

        /** The last code point of [s], or "" (a Hangul syllable or jamo is one code point). */
        fun lastChar(s: String): String =
            if (s.isEmpty()) "" else s.substring(s.offsetByCodePoints(s.length, -1))
    }
}

/** The tutor screen (LLD-07 §2.3): practice keyboard, step prompt, overlap message, save at the end, skip. */
class TutorActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val stores = AppStores(this)
        val module = SwipeModuleProvider.get()
        val steps = module.tutorSteps()   // FR-TUT-02, 08
        val start = stores.thresholds.load().thresholds
        val settings = stores.settings.loadBlocking()
        val rowHeightDp = settings.rowHeightDp   // same row height as the keyboard (HLD 7.1)
        // FR-TUT-09: the "입력: …" line judges with the stored values; the tutor saves only at its end, so these stay current.
        // FR-TUT-12: the error vibration of an ignored cheonjiin element, as the keyboard's (FR-SWP-62), with the vibration setting.
        val feedback = Feedback({ window?.decorView }, getSystemService(AudioManager::class.java), { settings })
        val session = TutorSession(steps, start)
        val flow = TutorFlow(session, { PracticeLayouts.echo(it, module, start, rowHeightDp) }, feedback::error) { t ->
            // FR-TUT-13: the 1-2 record goes with the values, stamped now (ISO-8601 with the offset).
            stores.thresholds.save(t, session.calibration?.copy(at = java.time.OffsetDateTime.now().withNano(0).toString()))
        }
        setContent {
            KeyboardAppTheme {
                TutorScreen(flow, steps, rowHeightDp, { PracticeLayouts.of(it, module) }, ::finish) { layout, anchor ->
                    PracticeLayouts.guide(layout, module, anchor, start, settings, flow::preview)   // FR-TUT-11: the keyboard's guide, its settings
                }
            }
        }
    }
}

@Composable
private fun TutorScreen(
    flow: TutorFlow,
    steps: List<TutorStep>,
    rowHeightDp: Float,
    keysOf: (PracticeLayout) -> PracticeKeys,
    close: () -> Unit,
    guideOf: (PracticeLayout, View) -> PracticeGuide?,
) {
    var tick by remember { mutableIntStateOf(0) }   // TutorFlow is plain state; bump to redraw
    val onStroke: (RawStroke, Float) -> Unit = remember(flow) { { raw, widthDp -> flow.onStroke(raw, widthDp); tick++ } }
    val onPress: () -> Unit = remember(flow) { { flow.onPress(); tick++ } }
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            val step = remember(tick) { flow.step }
            val intro = remember(tick) { flow.intro }
            Column(Modifier.weight(1f).fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.tutor_title), style = MaterialTheme.typography.headlineSmall)
                if (step == null) {
                    Text(stringResource(R.string.tutor_done), style = MaterialTheme.typography.titleLarge)
                    remember(tick) { flow.overall }?.let { o ->   // FR-TUT-10 overall
                        Text(stringResource(R.string.tutor_overall_stats, perMinute(o.perMinute), percent(o.accuracy)))
                    }
                    Button(onClick = close) { Text(stringResource(R.string.tutor_close)) }
                } else if (intro != null) {
                    ScrollingTexts {   // a short screen scrolls the text; the buttons stay below it
                        Text(stringResource(lessonTitle(step.lesson)), style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(intro), style = MaterialTheme.typography.bodyLarge)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = close) { Text(stringResource(R.string.tutor_skip)) }   // nothing saved (FR-TUT-05)
                        Button(onClick = { flow.startPractice(); tick++ }) { Text(stringResource(R.string.tutor_start)) }
                    }
                } else {
                    ScrollingTexts {   // the large character makes the screen taller (#143): a short screen scrolls, the buttons stay
                        Text(stringResource(R.string.tutor_step, TutorFlow.screenId(step.id), steps.indexOf(step) + 1, steps.size), style = MaterialTheme.typography.labelLarge)
                        Text(stringResource(promptOf(step)), style = MaterialTheme.typography.titleMedium)
                        remember(tick) { flow.target }?.let { Text(stringResource(R.string.tutor_target, it.label), style = MaterialTheme.typography.headlineMedium) }
                        val p = remember(tick) { flow.progress }
                        Text(stringResource(R.string.tutor_progress, p.collected, p.needed))
                        if (remember(tick) { flow.echoAsLine }) {
                            Text(stringResource(R.string.tutor_echo, remember(tick) { flow.echoText }), maxLines = 1)   // FR-TUT-09
                        } else {
                            EchoChar(remember(tick) { flow.echoChar }, remember(tick) { flow.verdict })   // FR-TUT-09 lessons 1–3 (#143), FR-TUT-12
                            remember(tick) { flow.howTo }?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }   // SWIPE 10.1
                        }
                        remember(tick) { flow.lastSentence }?.let { s ->   // FR-TUT-10 per sentence
                            Text(stringResource(R.string.tutor_sentence_stats, perMinute(s.perMinute), percent(s.accuracy)))
                        }
                        when (remember(tick) { flow.message }) {
                            TutorFlow.Message.OVERLAP_LENGTH -> Text(stringResource(R.string.tutor_overlap_length), color = MaterialTheme.colorScheme.error)
                            TutorFlow.Message.NOT_ENOUGH -> Text(stringResource(R.string.tutor_not_enough), color = MaterialTheme.colorScheme.error)
                            TutorFlow.Message.NONE -> Unit
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = close) { Text(stringResource(R.string.tutor_skip)) }   // nothing saved (FR-TUT-05)
                        if (step is SentenceStep) {
                            Button(onClick = { flow.nextSentence(); tick++ }) { Text(stringResource(R.string.tutor_next)) }
                        } else {
                            Button(onClick = { flow.finishStep(); tick++ }) { Text(stringResource(R.string.tutor_finish_step)) }
                        }
                    }
                }
            }
            if (step != null && intro == null) {
                key(step.practiceLayout) {
                    PracticeKeyboard(keysOf(step.practiceLayout), rowHeightDp, onStroke, Modifier.navigationBarsPadding(), onPress,
                        trailOpen = { flow.openStrokes }, center = { flow.center }, redraw = tick) { guideOf(step.practiceLayout, it) }
                }
            }
        }
    }
}

/**
 * The tutor's texts above the buttons: takes the room left in the column and scrolls when the
 * screen is too short for them, so the buttons below always show (#143 review).
 */
@Composable
private fun ColumnScope.ScrollingTexts(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}

/**
 * The last typed character, large (lessons 1–3, FR-TUT-09, #143), with the ✓
 * (green) or ✗ (red) of the item beside it (FR-TUT-12). The box keeps its height when empty so the
 * screen does not jump.
 */
@Composable
private fun EchoChar(c: String, verdict: Judgement?) {
    Row(Modifier.fillMaxWidth().height(ECHO_CHAR_BOX), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Text(c, fontSize = ECHO_CHAR_SIZE, lineHeight = ECHO_CHAR_SIZE, maxLines = 1)
        when (verdict) {
            Judgement.Right -> Text(MARK_RIGHT, fontSize = MARK_SIZE, color = RIGHT_COLOR, modifier = Modifier.padding(start = 16.dp))
            Judgement.Wrong -> Text(MARK_WRONG, fontSize = MARK_SIZE, color = WRONG_COLOR, modifier = Modifier.padding(start = 16.dp))
            else -> Unit
        }
    }
}

/** ✓ / ✗ and their colours (LLD-07 §2.5: Material green 700 / red 700, readable on light and dark). */
private const val MARK_RIGHT = "✓"
private const val MARK_WRONG = "✗"
private val RIGHT_COLOR = Color(0xFF388E3C)
private val WRONG_COLOR = Color(0xFFD32F2F)
private val MARK_SIZE = 48.sp

/** The large echo character: clearly larger than the target line (headlineMedium), still small enough not to cover the screen (#143). */
private val ECHO_CHAR_SIZE = 64.sp
private val ECHO_CHAR_BOX = 80.dp

/** Typing speed for display: whole keystrokes per minute, or "–" (FR-TUT-10). */
private fun perMinute(v: Double?): String = v?.let { Math.round(it).toString() } ?: "–"

/** Accuracy for display: whole percent, or "–". */
private fun percent(v: Double?): String = v?.let { Math.round(it).toString() } ?: "–"

/**
 * Lessons 2 (issue #168) and 4 (issue #150) were removed: the step ids keep the document numbers and
 * the screen numbers the lessons in a row — 1 basic, 2 compound vowels (doc 3), 3 sentences (doc 5)
 * (SWIPE 10장, LLD-07 §2.2).
 */
private fun lessonTitle(lesson: Int): Int = when (lesson) {
    1 -> R.string.tutor_lesson_1_title
    3 -> R.string.tutor_lesson_3_title
    else -> R.string.tutor_lesson_5_title
}

/** The step's prompt; a step without its own falls back to the 1-1 prompt string. */
private fun promptOf(step: TutorStep): Int = if (step.prompt != 0) step.prompt else R.string.tutor_prompt_qwerty_1
