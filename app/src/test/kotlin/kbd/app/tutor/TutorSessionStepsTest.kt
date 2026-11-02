package kbd.app.tutor

import kbd.hangul.cheonjiin.CjElement
import kbd.stroke.geom.RectDp
import kbd.stroke.geom.StrokePoint
import kbd.stroke.threshold.ThresholdDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * LLD-11 §7.5 personal tutor steps (issue #173): 1-1 gathers taps; 1-2 asks the basic vowels in stroke
 * order, judged by key, axis and direction, and sets minCrossDp (taps ↔ dots) and longDp (dots ↔ long
 * strokes) with the calibration record; 3-1 is judged by result; the practice consonant rotates.
 */
class TutorSessionStepsTest {

    private val eps = 1e-3f
    private val rect = RectDp(0f, 0f, 50f, 50f)
    private var clock = 0L

    /** A stroke on [key] from (x0, y0) moving (dx, dy) dp. */
    private fun raw(key: String, dx: Float, dy: Float, x0: Float = 25f, y0: Float = 25f): RawStroke {
        val prevUp = clock
        clock += 500
        val pts = listOf(StrokePoint(x0, y0, clock), StrokePoint(x0 + dx / 2, y0 + dy / 2, clock + 20), StrokePoint(x0 + dx, y0 + dy, clock + 40))
        clock += 40
        return RawStroke(rect, pts, prevUp, key = key)
    }

    private fun tap(key: String, jitter: Float = 1f) = raw(key, jitter, 0f)

    /**
     * The strokes of [label] (its consonant tap with [tap] jitter, then each vowel stroke: a dot of [dot]
     * dp, a long one of [long] dp), in the canonical places of SWIPE 3.6.
     */
    private fun item(label: String, tap: Float = 1f, dot: Float = 25f, long: Float = 90f): List<RawStroke> {
        val (c, v) = HowTo.split(label)
        val steps = HowTo.STROKES.getValue(v)
        return listOf(tap(c, tap)) + steps.map { s ->
            val d = if (s.long) long else dot
            if (s.vertical) raw(c, 0f, d) else raw(c, d, 0f)
        }
    }

    private fun write(s: TutorSession, strokes: List<RawStroke>) = strokes.forEach { s.onStroke(it) }

    /** Feeds whole items until the step's counted progress is full. */
    private fun performUntilDone(s: TutorSession, strokes: (TargetAction) -> List<RawStroke>) {
        var guard = 0
        while (s.progress.collected < s.progress.needed) {
            check(guard++ < 500) { "the items are not being judged right" }
            write(s, strokes(s.currentTarget!!))
        }
    }

    private fun session(vararg steps: TutorStep) = TutorSession(steps.toList(), ThresholdDefaults.VALUES)

    @Test
    fun `TU-32 step 1-1 asks the 14 basic consonants once each and, personal, sets nothing yet`() {
        val s = session(TutorSteps.STEP_1_1, TutorSteps.STEP_1_2)
        val asked = ArrayList<String>()
        (1..14).forEach { j ->
            asked += s.currentTarget!!.label
            s.onStroke(tap(s.currentTarget!!.label, jitter = j.toFloat()))
        }
        assertEquals(listOf("ㄱ", "ㄴ", "ㄷ", "ㄹ", "ㅁ", "ㅂ", "ㅅ", "ㅇ", "ㅈ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ"), asked)
        val out = s.finishStep()
        assertTrue("got $out", out is StepOutcome.Accepted)
        assertEquals(ThresholdDefaults.VALUES.minCrossDp, s.current.minCrossDp, eps)   // set with 1-2's dots (issue #173)
    }

    @Test
    fun `TU-34 step 1-2 - items, rotation, pairs, practice-only vowel-first items`() {
        val step = TutorSteps.STEP_1_2
        assertEquals(listOf("가", "거", "고", "구", "그", "기", "아", "오", "이"), step.targets.map { it.label })
        assertEquals(listOf("아", "오", "이"), step.targets.filter { step.practiceOnly(it) }.map { it.label })
        assertEquals(3, step.strokesPerAction(TargetAction("가")))
        assertEquals(2, step.strokesPerAction(TargetAction("그")))
        val s = session(step)
        assertEquals(StepProgress(0, 30), s.progress)
        val asked = ArrayList<String>()
        repeat(9) {
            val t = s.currentTarget!!
            asked += t.label
            write(s, item(t.label))
            assertEquals(t.label, Judgement.Right, s.judgement)
        }
        assertEquals(listOf("가", "꺼", "노", "두", "뜨", "띠", "아", "오", "이"), asked)
        assertEquals(6, s.progress.collected)
    }

    @Test
    fun `TU-03 step 1-2 records taps and dots (minCrossDp stays fixed, issue 217) and sets longDp from dots and long strokes`() {
        val s = session(TutorSteps.STEP_1_1, TutorSteps.STEP_1_2)
        (1..14).forEach { s.onStroke(tap(s.currentTarget!!.label, jitter = 2f + it / 7f)) }
        assertTrue(s.finishStep() is StepOutcome.Accepted)
        var n = 0
        performUntilDone(s) { t -> item(t.label, tap = 2f + (n % 7) / 7f, dot = 22f + (n % 9), long = 80f + (n++ % 11)) }
        val out = s.finishStep()
        assertTrue("got $out", out is StepOutcome.Accepted)
        val cal = s.calibration!!
        val tapDot = cal.tapDot!!
        val dotLong = cal.dotLong!!
        assertEquals(ThresholdDefaults.MIN_CROSS_DP, s.current.minCrossDp, eps)   // recorded only
        assertEquals((dotLong.shortP90 + dotLong.longP10) / 2, s.current.longDp, eps)
        assertEquals(14 + 30, tapDot.nShort)   // 1-1's taps and 1-2's consonant taps
        assertEquals(20, tapDot.nLong)         // the dots of 가 거 고 구, 5 rounds
        assertEquals(30, dotLong.nLong)        // a long stroke per item
        assertTrue(tapDot.gap > 0f && dotLong.gap > 0f)
        assertEquals(0, dotLong.extraRounds)
        assertEquals(20, cal.position!!.n)     // the second stroke of 가 거 고 구 (FR-TUT-13)
    }

    @Test
    fun `TU-04 overlap - the step again twice at most, then the formula's value with the overlap recorded`() {
        val s = session(TutorSteps.STEP_1_2)
        repeat(2) { k ->
            performUntilDone(s) { t -> item(t.label, dot = 50f, long = 45f) }   // dots and long strokes overlap
            assertEquals("round $k", StepOutcome.Overlap(ThresholdField.LONG), s.finishStep())
            assertEquals("1-2", s.currentStep!!.id)
            assertEquals(0, s.progress.collected)
            assertEquals("가", s.currentTarget!!.label)   // the rotation starts over (D-56)
        }
        performUntilDone(s) { t -> item(t.label, dot = 50f, long = 45f) }
        val out = s.finishStep()
        assertTrue("got $out", out is StepOutcome.Accepted)
        assertEquals(2, s.calibration!!.dotLong!!.extraRounds)
        assertTrue(s.calibration!!.dotLong!!.gap <= 0f)
    }

    @Test
    fun `TU-43 1-2 wrong actions - another key, the wrong axis, a reversed stroke with the hint, no movement`() {
        val s = session(TutorSteps.STEP_1_2)
        s.onStroke(tap("ㄴ"))
        assertEquals(Judgement.Wrong, s.judgement)
        s.onStroke(tap("ㄱ")); s.onStroke(raw("ㄱ", 60f, 0f))   // → for ㅣ
        assertEquals(Judgement.Wrong, s.judgement)
        assertFalse(s.reversedLast)
        s.onStroke(tap("ㄱ")); s.onStroke(raw("ㄱ", 0f, -60f))  // ↑
        assertEquals(Judgement.Wrong, s.judgement)
        assertTrue(s.reversedLast)
        s.onStroke(tap("ㄱ")); s.onStroke(raw("ㄱ", 0f, 0f))    // no movement (D-60)
        assertEquals(Judgement.Wrong, s.judgement)
        assertEquals("가", s.currentTarget!!.label)
        assertEquals(0, s.progress.collected)
        write(s, item("가", dot = 70f, long = 20f))   // lengths are not judged in 1-2
        assertEquals(Judgement.Right, s.judgement)
    }

    @Test
    fun `TU-48 3-1 - one round of the compound and y-vowels judged by result, rotating`() {
        val step = TutorSteps.STEP_3_1
        assertEquals(listOf("갸", "겨", "교", "규", "개", "게", "걔", "계", "과", "괘", "괴", "궈", "궤", "귀", "긔"), step.targets.map { it.label })
        val s = session(step)
        assertEquals(StepProgress(0, 15), s.progress)
        val shapes = shapes()
        repeat(15) {
            val t = s.currentTarget!!
            val (c, v) = HowTo.split(t.label)
            s.onStroke(tap(c))
            shapes.getValue(v).forEach { (a, b) -> s.onStroke(raw(c, b.first - a.first, b.second - a.second, a.first, a.second)) }
            assertEquals(t.label, Judgement.Right, s.judgement)
        }
        assertTrue(s.finishStep() is StepOutcome.Accepted)
        assertTrue(s.done)
    }

    /** Canonical strokes (start, end) of the vowels 3-1 asks for (SWIPE 3.6, t = 20). */
    private fun shapes(): Map<Char, List<Pair<Pair<Float, Float>, Pair<Float, Float>>>> {
        fun v(x: Float, y0: Float, y1: Float) = (x to y0) to (x to y1)
        fun h(y: Float, x0: Float, x1: Float) = (x0 to y) to (x1 to y)
        val i = v(100f, 40f, 160f)
        return mapOf(
            'ㅑ' to listOf(i, h(80f, 100f, 125f), h(120f, 100f, 125f)),
            'ㅕ' to listOf(h(80f, 60f, 85f), h(120f, 60f, 85f), v(87f, 40f, 160f)),
            'ㅛ' to listOf(v(80f, 60f, 85f), v(120f, 60f, 85f), h(90f, 50f, 150f)),
            'ㅠ' to listOf(h(100f, 50f, 150f), v(80f, 100f, 125f), v(120f, 100f, 125f)),
            'ㅐ' to listOf(i, h(100f, 100f, 125f), v(130f, 40f, 160f)),
            'ㅔ' to listOf(h(100f, 60f, 85f), v(87f, 40f, 160f), v(120f, 40f, 160f)),
            'ㅒ' to listOf(i, h(80f, 100f, 125f), h(120f, 100f, 125f), v(130f, 40f, 160f)),
            'ㅖ' to listOf(h(80f, 60f, 85f), h(120f, 60f, 85f), v(87f, 40f, 160f), v(120f, 40f, 160f)),
            'ㅘ' to listOf(v(100f, 60f, 85f), h(90f, 50f, 150f), v(160f, 40f, 160f), h(100f, 160f, 185f)),
            'ㅙ' to listOf(v(100f, 60f, 85f), h(90f, 50f, 150f), v(160f, 40f, 160f), h(100f, 160f, 185f), v(190f, 40f, 160f)),
            'ㅚ' to listOf(v(100f, 60f, 85f), h(90f, 50f, 150f), v(160f, 40f, 160f)),
            'ㅝ' to listOf(h(100f, 50f, 150f), v(100f, 100f, 125f), h(130f, 110f, 135f), v(137f, 60f, 170f)),
            'ㅞ' to listOf(h(100f, 50f, 150f), v(100f, 100f, 125f), h(130f, 110f, 135f), v(137f, 60f, 170f), v(170f, 60f, 170f)),
            'ㅟ' to listOf(h(100f, 50f, 150f), v(100f, 100f, 125f), v(160f, 60f, 170f)),
            'ㅢ' to listOf(h(100f, 50f, 150f), v(160f, 20f, 130f)),   // ㅡ on the ㅣ's lower third (SWIPE 0.33)
        )
    }

    @Test
    fun `TU-49 3-1 wrong - a stroke out of place, a vertical for a dot`() {
        val s = session(TutorSteps.STEP_3_1)   // 갸 first
        s.onStroke(tap("ㄱ")); s.onStroke(raw("ㄱ", 0f, 120f, 100f, 40f))
        s.onStroke(raw("ㄱ", 25f, 0f, 40f, 100f))   // left of the ㅣ: not ㅑ's dot
        assertEquals(Judgement.Wrong, s.judgement)
        assertEquals("갸", s.currentTarget!!.label)
    }

    @Test
    fun `TU-18b steps and screen numbers - no lessons 2 and 4, no 3-2 or 3-3`() {
        assertEquals(listOf("1-1", "1-2", "3-1", "3-4", "5-1"), TutorSteps.ALL.map { it.id })
        assertEquals(listOf("1-1", "1-2", "2-1", "2-4", "3-1"), TutorSteps.ALL.map { TutorFlow.screenId(it.id) })
    }

    @Test
    fun `TU-50 how-to lines follow SWIPE 10_1 and every item but the sentences is judged`() {
        assertEquals("ㄱ = ㄱ 탭", TutorSteps.STEP_1_1.itemSpec(TargetAction("ㄱ"))!!.howTo)
        assertEquals("가 = ㄱ 탭 → 자음 오른쪽에서 아래로 → ㅣ 오른쪽에 오른쪽으로", TutorSteps.STEP_1_2.itemSpec(TargetAction("가"))!!.howTo)
        assertEquals("구 = ㄱ 탭 → 자음 아래에 오른쪽으로 → ㅡ 아래에서 아래로", TutorSteps.STEP_1_2.itemSpec(TargetAction("구"))!!.howTo)
        val all = TutorSteps.ALL.flatMap { st -> st.targets.mapNotNull { st.itemSpec(it)?.howTo } }
        assertTrue(all.none { "측정" in it || "재기" in it || "기준값" in it })
        assertEquals(TutorSteps.ALL.dropLast(1).sumOf { it.targets.size }, all.size)
    }

    @Test
    fun `TU-51 outlier - a 1-1 touch far past the key is right but gives no sample`() {
        val one = TutorSteps.STEP_1_1
        assertTrue(one.extract(listOf(tap("ㄱ", 36f)), TargetAction("ㄱ"), ThresholdDefaults.VALUES).isEmpty())
        assertEquals(1, one.extract(listOf(tap("ㄱ", 35f)), TargetAction("ㄱ"), ThresholdDefaults.VALUES).size)
    }

    @Test
    fun `TU-52 3-4 cheonjiin key - one item judged by result, nothing measured`() {
        val step = TutorSteps.STEP_3_4
        assertEquals(listOf("가 — 천지인 키"), step.targets.map { it.label })
        val s = session(step)
        s.onStroke(tap("ㄱ")); s.onStroke(raw(HowTo.CHEONJIIN_KEY, 0f, -40f)); s.onStroke(tap(HowTo.CHEONJIIN_KEY))
        assertEquals(Judgement.Right, s.judgement)
        assertTrue(s.finishStep() is StepOutcome.Accepted)
        assertTrue(s.done)
        assertEquals(CjElement.I, TutorItemJudge.record(raw(HowTo.CHEONJIIN_KEY, 0f, -40f), ThresholdDefaults.VALUES).element)
    }

    @Test
    fun `TU-54 a lesson-5 stroke has no judgement even right after a judged step (issue 163)`() {
        val s = session(TutorSteps.STEP_1_1, TutorSteps.STEP_5_1)
        TutorSteps.CONSONANTS.forEach { s.onStroke(tap(it)) }
        assertTrue(s.finishStep() is StepOutcome.Accepted)
        s.onStroke(tap("ㅇ"))
        assertNull(s.judgement)
        assertNull(s.judgedItem)
    }

    @Test
    fun `TU-59 the next step after an accepted one starts the rotation at ㄱ`() {
        val s = session(TutorSteps.STEP_1_2, TutorSteps.STEP_3_1)
        performUntilDone(s) { t -> item(t.label, dot = 20f, long = 80f) }
        assertTrue(s.finishStep() is StepOutcome.Accepted)
        assertEquals("갸", s.currentTarget!!.label)
    }
}
