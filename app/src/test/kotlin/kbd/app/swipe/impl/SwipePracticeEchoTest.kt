package kbd.app.swipe.impl

import kbd.app.tutor.HowTo
import kbd.hangul.jamo.Jamo
import kbd.app.tutor.TutorSteps
import kbd.app.tutor.PracticeLayout
import kbd.app.tutor.PracticeLayouts
import kbd.app.tutor.RawStroke
import kbd.app.tutor.TutorFlow
import kbd.app.tutor.TutorSession
import kbd.stroke.geom.StrokePoint
import kbd.stroke.threshold.ThresholdDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The personal "입력: …" line (FR-TUT-09): practice strokes through the swipe keyboard's own chain (issue #168: a swipe = consonant + vowel). */
class SwipePracticeEchoTest {

    private val width = 350f   // column width 50 dp
    private val rowH = 50f
    private val geo = SwipeGeometry.build(width, rowH)
    private val g = geo.geoms[geo.keys.indexOfFirst { it.label == "ㄱ" }].rect   // (200, 100)–(250, 150)
    private var clock = 0L

    private fun pts(vararg xy: Pair<Float, Float>, step: Long = 20): List<StrokePoint> =
        xy.map { (x, y) -> clock += step; StrokePoint(x, y, clock) }

    /** A tap on ㄱ, [gap] ms after the last stroke. */
    private fun tap(gap: Long = 1000): RawStroke {
        clock += gap
        return RawStroke(g, pts(225f to 125f, 226f to 125f, 225f to 125f), null, key = "ㄱ")
    }

    /** A stroke from (x0, y0) to (x1, y1), keyboard dp; the key under its start is the key pressed. */
    private fun line(x0: Float, y0: Float, x1: Float, y1: Float, gap: Long = 1000): RawStroke {
        val i = geo.indexAt(x0, y0)
        clock += gap
        return RawStroke(geo.geoms[i].rect, pts(x0 to y0, (x0 + x1) / 2 to (y0 + y1) / 2, x1 to y1), null, key = geo.keys[i].label)
    }

    /** ㅣ beside C (x 230, y 70 → 190; C = (132.5, 54)–(217.5, 154) here, centred, issue #213) and ㅏ's dot right of it (SWIPE 3.6.3). */
    private fun i() = line(230f, 70f, 230f, 190f)
    private fun dotA(len: Float = 25f) = line(230f, 130f, 230f + len, 130f)

    /** A tap on the consonant key labelled [label]. */
    private fun tapOn(label: String, gap: Long = 1000): RawStroke {
        val r = geo.geoms[geo.keys.indexOfFirst { it.label == label }].rect
        val cx = (r.left + r.right) / 2; val cy = (r.top + r.bottom) / 2
        clock += gap
        return RawStroke(r, pts(cx to cy, cx + 1f to cy, cx to cy), null, key = label)
    }

    @Test
    fun `a tap types the consonant, strokes write the vowel, a tap after it is the final`() {
        val e = SwipePracticeEcho(ThresholdDefaults.VALUES, rowH)
        e.onStroke(tap(), width)
        assertEquals("ㄱ", e.text)
        e.onStroke(i(), width)
        assertEquals("기", e.text)
        assertEquals(1, e.openStrokes)
        e.onStroke(dotA(), width)
        assertEquals("가", e.text)
        assertEquals(2, e.openStrokes)
        e.onStroke(tap(), width)
        assertEquals("각", e.text)
        assertEquals(0, e.openStrokes)   // the tap closed the assembly (the trail goes, SWIPE 9.4)
    }

    /** #180 follow-up SWIPE 9.5: the practice keyboard's center consonant is the echo's last consonant, in C. */
    @Test
    fun `TU-62 the center consonant and its place C, judged against C`() {
        val e = SwipePracticeEcho(ThresholdDefaults.VALUES, rowH)
        assertNull(e.center)
        e.onStroke(tap(), width)
        val c = SwipeGeometry.build(width, rowH).consonantBox
        assertEquals(kbd.app.view.CenterMark(Jamo.G, c), e.center)
        e.onStroke(line(220f, 100f, 245f, 100f), width)   // a horizontal dot beside C: no vowel yet
        assertEquals("ㄱㆍ", e.text)
        assertEquals("ㅓ", e.preview(kbd.hangul.stroke.VowelStroke(kbd.hangul.stroke.StrokeAxis.VERTICAL, 255f, 70f, 255f, 190f)))
        e.onStroke(line(255f, 70f, 255f, 190f), width)    // ㅣ beside C = ㅓ (C's dot first)
        assertEquals("거", e.text)
        assertEquals(Jamo.G, e.center?.consonant)          // a vowel keeps it
        e.onStroke(tapOn("ㄴ"), width)
        assertEquals("건", e.text)
        assertEquals(Jamo.N, e.center?.consonant)          // the next consonant replaces it
        e.onStroke(line(160f, 70f, 160f, 190f), width)   // its middle (160, 130) is under-left: a dot, nothing dropped (SWIPE 0.43)
        assertFalse(e.lastIgnored)
        assertEquals("건ㆍ", e.text)
    }

    @Test
    fun `a reversed stroke and an unfinished stroke are the error vibration`() {
        val e = SwipePracticeEcho(ThresholdDefaults.VALUES, rowH)
        e.onStroke(tap(), width)
        e.onStroke(line(230f, 190f, 230f, 70f), width)   // ↑ at ㅣ's place: ignored for its direction
        assertTrue(e.lastIgnored)
        e.onStroke(line(220f, 100f, 245f, 100f), width)   // a dot beside C: ㆍ
        assertEquals("ㄱㆍ", e.text)
        assertEquals(false, e.lastIgnored)
        e.onStroke(tapOn("ㄴ"), width)                    // drops it
        assertTrue(e.lastIgnored)
        assertEquals("ㄱㄴ", e.text)
    }

    @Test
    fun `length decides nothing - a bare first horizontal is ㅡ whatever longDp (issue 179)`() {
        val e = SwipePracticeEcho(ThresholdDefaults.VALUES, rowH)       // longDp 51
        e.onStroke(line(80f, 130f, 100f, 130f), width)
        assertEquals("ㅡ", e.text)
        val loose = SwipePracticeEcho(ThresholdDefaults.VALUES.copy(longDp = 80f), rowH)
        loose.onStroke(line(80f, 130f, 140f, 130f), width)
        assertEquals("ㅡ", loose.text)
    }

    @Test
    fun `the personal module gives the swipe echo for swipe steps`() {
        val e = PracticeLayouts.echo(PracticeLayout.SWIPE_CONSONANT_KEY, SwipeEngineModule, ThresholdDefaults.VALUES, rowH)
        assertTrue(e is SwipePracticeEcho)
    }

    @Test
    fun `TU-17 step 1 shows one large character, then 1-2 is judged by key, axis and direction`() {
        val flow = TutorFlow(TutorSession(TutorSteps.ALL, ThresholdDefaults.VALUES), {
            PracticeLayouts.echo(it, SwipeEngineModule, ThresholdDefaults.VALUES, rowH)
        }) {}
        flow.onStroke(tap(), width)                          // ignored while the lesson explanation shows
        assertEquals("", flow.echoChar)
        flow.startPractice()
        assertEquals(false, flow.echoAsLine)
        repeat(3) { flow.onStroke(tap(), width) }            // ㄱ ✓, then ㄱ again while ㄴ is asked: ✗, ✗
        assertEquals("ㄱ", flow.echoChar)
        assertEquals("ㄴ", flow.target!!.label)
        assertEquals(1, flow.progress.collected)
        assertEquals("ㄴ = ㄴ 탭", flow.howTo)
        TutorSteps.CONSONANTS.drop(1).forEach { flow.onStroke(tapOn(it), width) }
        assertEquals("1-2", flow.step!!.id)
        assertEquals("가", flow.target!!.label)
        flow.onStroke(tap(), width); flow.onStroke(i(), width); flow.onStroke(dotA(), width)
        assertEquals(kbd.app.tutor.Judgement.Right, flow.verdict)
        assertEquals("가", flow.echoChar)
        assertEquals("꺼", flow.target!!.label)              // the next consonant: ㄲ
        flow.onStroke(tapOn("ㄲ"), width); flow.onStroke(i(), width)   // ㅓ starts with the dot: ✗
        assertEquals(kbd.app.tutor.Judgement.Wrong, flow.verdict)
        assertEquals("꺼 = ㄲ 탭 → 자음 오른쪽에 오른쪽으로 → 점 오른쪽 끝에서 점이 위나 가운데에 오게 아래로", flow.howTo)
        flow.onStroke(tapOn("ㄲ"), width); flow.onStroke(line(230f, 190f, 230f, 70f), width)   // ↑: ✗ with the extra line
        assertEquals("꺼 = ㄲ 탭 → 자음 오른쪽에 오른쪽으로 → 점 오른쪽 끝에서 점이 위나 가운데에 오게 아래로\n획은 아래로, 오른쪽으로만 그어요", flow.howTo)
        assertEquals("꺼", flow.target!!.label)
    }

    @Test
    fun `TU-42 the error vibration - a reversed stroke, an ignored cheonjiin element`() {
        var vibes = 0
        val flow = TutorFlow(TutorSession(TutorSteps.ALL.filter { it.id == "1-2" }, ThresholdDefaults.VALUES), {
            PracticeLayouts.echo(it, SwipeEngineModule, ThresholdDefaults.VALUES, rowH)
        }, { vibes++ }) {}
        flow.onStroke(tapOn("ㄴ"), width)                    // 가 asked: another key, ✗ without vibration
        assertEquals(kbd.app.tutor.Judgement.Wrong, flow.verdict)
        assertEquals(0, vibes)
        flow.onStroke(tap(), width); flow.onStroke(line(230f, 190f, 230f, 70f), width)   // ↑: ✗ and the keyboard's vibration
        assertEquals(1, vibes)
        flow.onStroke(tap(), width); flow.onStroke(i(), width); flow.onStroke(dotA(), width)
        assertEquals(kbd.app.tutor.Judgement.Right, flow.verdict)
        val f2 = TutorFlow(TutorSession(TutorSteps.ALL.filter { it.id == "3-4" }, ThresholdDefaults.VALUES), {
            PracticeLayouts.echo(it, SwipeEngineModule, ThresholdDefaults.VALUES, rowH)
        }, { vibes++ }) {}
        f2.onStroke(tapOn("ㄱ"), width)
        f2.onStroke(swipeOn(HowTo.CHEONJIIN_KEY, 0f, -50f), width)
        f2.onStroke(swipeOn(HowTo.CHEONJIIN_KEY, 0f, -50f), width)
        assertEquals(kbd.app.tutor.Judgement.Wrong, f2.verdict)
        assertEquals(2, vibes)
    }

    @Test
    fun `TU-29 lesson 5 of the personal tutor shows the line`() {
        val steps = TutorSteps.ALL.filter { it.id == "5-1" }
        val flow = TutorFlow(TutorSession(steps, ThresholdDefaults.VALUES), {
            PracticeLayouts.echo(it, SwipeEngineModule, ThresholdDefaults.VALUES, rowH)
        }) {}
        flow.startPractice()
        assertEquals(true, flow.echoAsLine)
        flow.onStroke(tap(), width); flow.onStroke(i(), width); flow.onStroke(dotA(), width)
        flow.onStroke(tap(), width); flow.onStroke(i(), width); flow.onStroke(dotA(), width)
        assertEquals("가가", flow.echoText)
        assertEquals(2, flow.openStrokes)
    }

    /** A stroke from the centre of the key labelled [label] by (dx, dy) dp. */
    private fun swipeOn(label: String, dx: Float, dy: Float, gap: Long = 1000): RawStroke {
        val r = geo.geoms[geo.keys.indexOfFirst { it.label == label }].rect
        val cx = (r.left + r.right) / 2; val cy = (r.top + r.bottom) / 2
        clock += gap
        return RawStroke(r, pts(cx to cy, cx + dx / 2 to cy + dy / 2, cx + dx to cy + dy), null, key = label)
    }

    @Test
    fun `TU-53 lesson 5 after a judged step - 아이 types, the last judgement does not empty the sentence line (issue 163)`() {
        val steps = TutorSteps.ALL.filter { it.id == "3-4" || it.id == "5-1" }
        val flow = TutorFlow(TutorSession(steps, ThresholdDefaults.VALUES), {
            PracticeLayouts.echo(it, SwipeEngineModule, ThresholdDefaults.VALUES, rowH)
        }) {}
        flow.startPractice()
        flow.onStroke(tapOn("ㄱ"), width)                                    // 3-4: 가 by the cheonjiin key
        flow.onStroke(swipeOn(HowTo.CHEONJIIN_KEY, 0f, -50f), width)
        flow.onStroke(tapOn(HowTo.CHEONJIIN_KEY), width)
        assertEquals("5-1", flow.step!!.id)
        flow.startPractice()
        flow.onStroke(tapOn("ㅇ"), width); flow.onStroke(i(), width); flow.onStroke(dotA(), width)
        assertEquals("아", flow.echoText)
        flow.onStroke(tapOn("ㅇ"), width); flow.onStroke(i(), width)
        assertEquals("아이", flow.echoText)
        assertEquals(null, flow.verdict)                                     // lesson 5 is not judged
    }
}
