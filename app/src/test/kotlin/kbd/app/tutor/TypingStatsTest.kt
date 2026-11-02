package kbd.app.tutor

import kbd.stroke.geom.RectDp
import kbd.stroke.geom.StrokePoint
import kbd.stroke.threshold.ThresholdDefaults
import kbd.stroke.threshold.Thresholds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** LLD-11 §7.5 TY-01..TY-04 (typing speed and accuracy, FR-TUT-10) and TU-22..TU-23 (lesson flow). */
class TypingStatsTest {

    private val eps = 1e-6

    @Test
    fun `TY-01 dubeolsik keystrokes`() {
        assertEquals(5, TypingStats.keystrokes("곡기"))       // ㄱ ㅗ ㄱ ㄱ ㅣ
        assertEquals(6, TypingStats.keystrokes("과일"))       // ㄱ ㅘ(2) ㅇ ㅣ ㄹ
        assertEquals(2, TypingStats.keystrokes("까"))         // shift not counted: ㄲ 1
        assertEquals(3, TypingStats.keystrokes("밖"))         // ㄲ final 1
        assertEquals(4, TypingStats.keystrokes("닭"))         // ㄺ 2
        assertEquals(2, TypingStats.keystrokes("얘"))         // ㅒ 1
        assertEquals(3, TypingStats.keystrokes("의"))         // ㅢ 2
        assertEquals(3, TypingStats.keystrokes("a 1"))        // space, digit, letter 1 each
        assertEquals(1, TypingStats.keystrokes("ㄱ"))
        assertEquals(2, TypingStats.keystrokes("ㅘ"))
        assertEquals(2, TypingStats.keystrokes("ㄳ"))
        assertEquals(0, TypingStats.keystrokes(""))
    }

    @Test
    fun `TY-02 accuracy by character edit distance, never below zero`() {
        assertEquals(100.0, TypingStats.accuracy("고기", "고기"), eps)
        assertEquals(0.0, TypingStats.accuracy("고기", "괴"), eps)               // 2 edits on 2 characters
        assertEquals(50.0, TypingStats.accuracy("고기", "고"), eps)
        assertEquals(80.0, TypingStats.accuracy("아이 고기", "아이 괴기"), eps)   // the space counts as a character
        assertEquals(0.0, TypingStats.accuracy("가", "나다라"), eps)            // 3 edits on 1: not below 0
        assertEquals(3, TypingStats.editDistance("kitten", "sitting"))
    }

    @Test
    fun `TY-03 per-sentence speed and the dash rules`() {
        val s = TypingStats.sentence("곡기", "곡기", 6_000L)       // 5 keys in 0.1 min
        assertEquals(50.0, s.perMinute!!, eps)
        assertEquals(100.0, s.accuracy, eps)
        assertNull(TypingStats.sentence("곡기", "", 6_000L).perMinute)     // nothing typed: –
        assertNull(TypingStats.sentence("곡기", "곡기", 0L).perMinute)     // no time: –
        assertEquals(0.0, TypingStats.sentence("곡기", "", 6_000L).accuracy, eps)
    }

    @Test
    fun `TY-04 overall speed is keys over time of timed sentences, accuracy the mean of all`() {
        val a = TypingStats.sentence("곡기", "곡기", 6_000L)     // 5 keys
        val b = TypingStats.sentence("과일", "과일", 18_000L)    // 6 keys
        val c = TypingStats.sentence("고기", "", 0L)             // –, accuracy 0
        val o = TypingStats.overall(listOf(a, b, c))
        assertEquals(11 * 60_000.0 / 24_000, o.perMinute!!, eps)
        assertEquals(200.0 / 3, o.accuracy!!, eps)
        assertNull(TypingStats.overall(listOf(c)).perMinute)
        assertNull(TypingStats.overall(emptyList()).accuracy)
    }

    // --- TutorFlow lessons (LLD-07 §2.3) ----------------------------------------------------

    private class EchoOf(val typed: String) : PracticeEcho {
        var n = 0
        override val text: String get() = typed.take(n)
        override fun onStroke(raw: RawStroke, widthDp: Float) { n++ }
    }

    private class Sentences(override val intro: Int) : SentenceStep {
        override val id = "9-1"   // no threshold rules: the flow alone is tested
        override val prompt = 0
        override val practiceLayout = PracticeLayout.SWIPE_SENTENCE
        override val targets = listOf(TargetAction("곡기"), TargetAction("과일"))
        override val minSamplesPerTarget = 1
        override fun extract(raw: RawStroke, target: TargetAction, current: Thresholds) = emptyList<Sample>()
    }

    private var t = 0L
    private fun stroke(): RawStroke {
        t += 1_000
        return RawStroke(RectDp(0f, 0f, 50f, 50f), listOf(StrokePoint(25f, 25f, t), StrokePoint(26f, 25f, t + 500)), null)
    }

    @Test
    fun `TU-22 the lesson explanation comes first and strokes wait for it`() {
        val step = Sentences(intro = 42)
        val flow = TutorFlow(TutorSession(listOf(step), ThresholdDefaults.VALUES), { EchoOf("곡기과일") }) {}
        assertEquals(42, flow.intro)
        flow.onStroke(stroke(), 350f)
        assertEquals("", flow.echoText)
        flow.startPractice()
        assertNull(flow.intro)
        flow.onStroke(stroke(), 350f)
        assertEquals("곡", flow.echoText)
    }

    @Test
    fun `TU-23 each sentence ends with next, shows its speed and accuracy, empties the line, then the overall`() {
        val step = Sentences(intro = 0)
        var saved = 0
        val flow = TutorFlow(TutorSession(listOf(step), ThresholdDefaults.VALUES), { EchoOf("곡기") }) { saved++ }
        flow.onStroke(stroke(), 350f)              // t 1000..1500
        flow.onStroke(stroke(), 350f)              // t 2000..2500
        assertEquals("곡기", flow.echoText)
        flow.nextSentence()
        val first = flow.lastSentence!!
        assertEquals(5, first.keystrokes)
        assertEquals(1_500L, first.millis)
        assertEquals(100.0, first.accuracy, eps)
        assertEquals("", flow.echoText)            // emptied per sentence (FR-TUT-09)
        assertEquals("과일", flow.target!!.label)
        flow.nextSentence()                        // nothing typed: –, accuracy 0
        assertNull(flow.sentenceStats[1].perMinute)
        assertTrue(flow.done)
        assertEquals(1, saved)
        val o = flow.overall!!
        assertEquals(5 * 60_000.0 / 1_500, o.perMinute!!, eps)
        assertEquals(50.0, o.accuracy!!, eps)
    }
}
