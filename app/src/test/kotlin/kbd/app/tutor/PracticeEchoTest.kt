package kbd.app.tutor

import kbd.app.view.TapGeometry
import kbd.stroke.geom.RectDp
import kbd.stroke.geom.StrokePoint
import kbd.stroke.layout.TapLayouts
import kbd.stroke.threshold.ThresholdDefaults
import kbd.stroke.threshold.Thresholds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The tutor's "입력: …" line (FR-TUT-09, LLD-07 §2.3): when the line is emptied (the public QWERTY echo went with issue #251). */
class PracticeEchoTest {

    private val width = 360f
    private val rowH = 48f
    private val geo = TapGeometry.build(TapLayouts.QWERTY, width, rowH)

    private fun rectOf(label: String): RectDp = geo.geoms[geo.keys.indexOfFirst { it.label == label }].rect

    private fun stroke(label: String, t0: Long, dx: Float = 0f, dy: Float = 0f): RawStroke {
        val r = rectOf(label)
        val cx = (r.left + r.right) / 2; val cy = (r.top + r.bottom) / 2
        return RawStroke(r, listOf(StrokePoint(cx, cy, t0), StrokePoint(cx + dx / 2, cy + dy / 2, t0 + 30), StrokePoint(cx + dx, cy + dy, t0 + 60)), null, key = label)
    }

    /** Counts strokes, so a fresh echo shows as an empty line. */
    private class CountingEcho : PracticeEcho {
        val strokes = ArrayList<Float>()
        override val text: String get() = "x".repeat(strokes.size)
        override fun onStroke(raw: RawStroke, widthDp: Float) { strokes += widthDp }
    }

    /** Step [id] with targets A and B; A gives SHORT, B LONG samples of [field] with the value [valueOf]. */
    private class FakeStep(
        override val id: String, private val field: ThresholdField, override val minSamplesPerTarget: Int = 10, private val valueOf: (String) -> Float,
    ) : TutorStep {
        override val prompt = 0
        override val practiceLayout = PracticeLayout.SWIPE_CONSONANT_KEY
        override val targets = listOf(TargetAction("A"), TargetAction("B"))
        override fun extract(raw: RawStroke, target: TargetAction, current: Thresholds): List<Sample> {
            val side = if (target.label == "A") SampleSide.SHORT else SampleSide.LONG
            // 1-2 also sets minCrossDp (taps ↔ dots, issue #173): well apart here, so only [field] decides.
            val minCross = if (field == ThresholdField.LONG) listOf(Sample(ThresholdField.MIN_CROSS, side, if (side == SampleSide.SHORT) 1f else 40f)) else emptyList()
            return listOf(Sample(field, side, valueOf(target.label))) + minCross
        }
    }

    private val anyStroke = RawStroke(RectDp(0f, 0f, 36f, 48f), listOf(StrokePoint(18f, 24f, 0), StrokePoint(18f, 24f, 50)), null)

    @Test
    fun `echo line shows strokes of the try and empties on Overlap and on the next step`() {
        val echoes = ArrayList<CountingEcho>()
        val overlapping = FakeStep("1-2", ThresholdField.LONG) { 30f }                        // A and B overlap
        val separate = FakeStep("1-2", ThresholdField.LONG) { if (it == "A") 20f else 60f }
        var tries = 0
        // First try of the step overlaps, the second is accepted.
        val step = object : TutorStep by overlapping {
            override fun extract(raw: RawStroke, target: TargetAction, current: Thresholds) =
                (if (tries == 0) overlapping else separate).extract(raw, target, current)
        }
        val flow = TutorFlow(TutorSession(listOf(step, separate), ThresholdDefaults.VALUES), { CountingEcho().also { echoes += it } }) {}

        repeat(19) { flow.onStroke(anyStroke, width) }
        assertEquals("x".repeat(19), flow.echoText)
        assertEquals(1, echoes.size)
        assertEquals(19, echoes[0].strokes.size)
        assertTrue(echoes[0].strokes.all { it == width })

        flow.onStroke(anyStroke, width)                                                  // 20th: Overlap
        assertEquals(TutorFlow.Message.OVERLAP_LENGTH, flow.message)
        assertEquals("", flow.echoText)                                                  // emptied for the new try
        assertEquals(2, echoes.size)

        tries = 1
        repeat(3) { flow.onStroke(anyStroke, width) }
        assertEquals("xxx", flow.echoText)
        repeat(17) { flow.onStroke(anyStroke, width) }                                   // accepted → step 2 of 2
        assertEquals(TutorFlow.Message.NONE, flow.message)
        assertEquals("1-2", flow.step!!.id)
        assertEquals("", flow.echoText)
        assertEquals(3, echoes.size)
    }

    @Test
    fun `echo line keeps its text when the step-end button comes too early`() {
        val flow = TutorFlow(TutorSession(listOf(FakeStep("1-2", ThresholdField.LONG) { 30f }), ThresholdDefaults.VALUES), { CountingEcho() }) {}
        repeat(2) { flow.onStroke(anyStroke, width) }
        flow.finishStep()
        assertEquals(TutorFlow.Message.NOT_ENOUGH, flow.message)
        assertEquals("xx", flow.echoText)
    }

    @Test
    fun `a long echo line shows only its tail`() {
        val flow = TutorFlow(TutorSession(listOf(FakeStep("1-2", ThresholdField.LONG, 50) { 30f }), ThresholdDefaults.VALUES), {
            object : PracticeEcho {
                var n = 0
                override val text: String get() = (0 until n).joinToString("") { ('a' + it % 26).toString() }
                override fun onStroke(raw: RawStroke, widthDp: Float) { n++ }
            }
        }) {}
        repeat(TutorFlow.ECHO_MAX) { flow.onStroke(anyStroke, width) }
        assertEquals("abcdefghijklmnopqrst", flow.echoText)                              // exactly ECHO_MAX: whole text
        repeat(5) { flow.onStroke(anyStroke, width) }                                     // 25 chars "a..y"
        assertEquals(TutorFlow.ECHO_MAX, flow.echoText.length)
        assertEquals("…ghijklmnopqrstuvwxy", flow.echoText)
    }

    @Test
    fun `without a width the echo is not fed and measurement goes on`() {
        val echo = CountingEcho()
        val flow = TutorFlow(TutorSession(listOf(FakeStep("1-2", ThresholdField.LONG) { 30f }), ThresholdDefaults.VALUES), { echo }) {}
        flow.onStroke(anyStroke)
        assertEquals("", flow.echoText)
        assertEquals(1, flow.progress.collected)
    }

    // --- #143: lessons 1–4 show the last character large, lesson 5 the line ----------------

    /** Shows the texts in [texts] one per stroke (a composing syllable may change or commit). */
    private class ScriptedEcho(private val texts: List<String>) : PracticeEcho {
        var n = 0
        override val text: String get() = if (n == 0) "" else texts[n - 1]
        override fun onStroke(raw: RawStroke, widthDp: Float) { n++ }
    }

    private class FakeSentences : SentenceStep {
        override val id = "9-1"   // no threshold rules
        override val prompt = 0
        override val practiceLayout = PracticeLayout.SWIPE_SENTENCE
        override val targets = listOf(TargetAction("곡기"))
        override val minSamplesPerTarget = 1
        override fun extract(raw: RawStroke, target: TargetAction, current: Thresholds) = emptyList<Sample>()
    }

    @Test
    fun `TU-27 a step that is not sentences shows only the last character, composing or committed`() {
        val texts = listOf("ㄱ", "가", "각", "가가", "가가ㄴ")   // composing changes, then dokkaebibul commits 가
        val flow = TutorFlow(TutorSession(listOf(FakeStep("1-2", ThresholdField.LONG) { 30f }), ThresholdDefaults.VALUES), { ScriptedEcho(texts) }) {}
        assertEquals(false, flow.echoAsLine)
        assertEquals("", flow.echoChar)
        val shown = (1..texts.size).map { flow.onStroke(anyStroke, width); flow.echoChar }
        assertEquals(listOf("ㄱ", "가", "각", "가", "ㄴ"), shown)
    }

    @Test
    fun `TU-28 the large character stays on NotEnough and is emptied when the step changes`() {
        val texts = listOf("가", "가거")
        val tooFew = TutorFlow(TutorSession(listOf(FakeStep("1-2", ThresholdField.LONG, 1) { 30f }), ThresholdDefaults.VALUES), { ScriptedEcho(texts) }) {}
        repeat(2) { tooFew.onStroke(anyStroke, width) }   // both targets once: NotEnough, the try stays
        assertEquals(TutorFlow.Message.NOT_ENOUGH, tooFew.message)
        assertEquals("거", tooFew.echoChar)

        // 2-2 sets nothing, so it is accepted after one round; lesson 5 follows with a fresh echo.
        val flow = TutorFlow(TutorSession(listOf(FakeStep("2-2", ThresholdField.LONG, 1) { 30f }, FakeSentences()), ThresholdDefaults.VALUES), {
            ScriptedEcho(texts)
        }) {}
        flow.onStroke(anyStroke, width)
        assertEquals("가", flow.echoChar)
        flow.onStroke(anyStroke, width)
        assertEquals("9-1", flow.step!!.id)
        assertEquals(true, flow.echoAsLine)
        assertEquals("", flow.echoChar)
        assertEquals("", flow.echoText)
    }

    @Test
    fun `TU-29 lesson 5 keeps the line and is emptied per sentence`() {
        val flow = TutorFlow(TutorSession(listOf(FakeSentences()), ThresholdDefaults.VALUES), { ScriptedEcho(listOf("곡", "곡기")) }) {}
        assertEquals(true, flow.echoAsLine)
        repeat(2) { flow.onStroke(anyStroke, width) }
        assertEquals("곡기", flow.echoText)
        flow.nextSentence()
        assertEquals("", flow.echoText)
    }

    @Test
    fun `TU-31 lastChar takes one code point`() {
        assertEquals("", TutorFlow.lastChar(""))
        assertEquals("각", TutorFlow.lastChar("가각"))
        val smile = String(Character.toChars(0x1F600))
        assertEquals(smile, TutorFlow.lastChar("a" + smile))   // a surrogate pair stays whole
    }
}
