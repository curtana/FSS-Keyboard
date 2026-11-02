package kbd.app.tutor

import kbd.hangul.cheonjiin.CjElement
import kbd.hangul.jamo.Jamo
import kbd.stroke.geom.RectDp
import kbd.stroke.geom.StrokePoint
import kbd.stroke.threshold.ThresholdDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** LLD-11 §7.5 TJ-01..TJ-08: `TutorItemJudge` and `HowTo` (LLD-07 §2.5, SWIPE 10.1, FR-TUT-12, issue #173). */
class TutorItemJudgeTest {

    private val th = ThresholdDefaults.VALUES   // minCross 8 (fixed, issue #217), longDp 51, t 20
    private val rect = RectDp(0f, 0f, 50f, 50f)

    /** A stroke on [key] from (x0, y0) to (x1, y1), screen dp. */
    private fun raw(key: String, x0: Float, y0: Float, x1: Float = x0, y1: Float = y0) =
        RawStroke(rect, listOf(StrokePoint(x0, y0, 0), StrokePoint((x0 + x1) / 2, (y0 + y1) / 2, 10), StrokePoint(x1, y1, 20)), null, key = key)

    private fun rec(r: RawStroke) = TutorItemJudge.record(r, th)

    private fun judge(spec: ItemSpec, vararg r: RawStroke): List<Judgement> {
        val recs = ArrayList<ActionRecord>()
        return r.map { recs += rec(it); TutorItemJudge.judge(spec, recs, th) }
    }

    @Test
    fun `TJ-01 record - displacement, tap under minCrossDp, axis and direction`() {
        val tap = rec(raw("ㄱ", 25f, 25f, 32.9f, 25f))
        assertTrue(tap.tap)
        assertEquals(7.9f, tap.length, 1e-4f)
        assertFalse(rec(raw("ㄱ", 25f, 25f, 33f, 25f)).tap)          // 8 dp: a stroke
        val s = rec(raw("ㄲ", 25f, 25f, 25f, 105f))
        assertFalse(s.tap); assertEquals("ㄲ", s.key); assertEquals(true, s.vertical); assertTrue(s.forward)
        assertFalse(rec(raw("ㄱ", 25f, 25f, 5f, 25f)).forward)     // ←
        assertNull(rec(raw("ㄱ", 25f, 25f)).vertical)               // no movement: no axis (D-60)
        assertFalse(rec(raw("ㄱ", 25f, 25f)).forward)
    }

    @Test
    fun `TJ-03 1-2 - the key, then each stroke's axis and direction, whatever its length`() {
        val ga = HowTo.measured("가")
        assertEquals(listOf(ExpectedAction.Press("ㄱ"), ExpectedAction.Stroke(true), ExpectedAction.Stroke(false)), ga.actions)
        assertEquals(listOf(Judgement.Pending, Judgement.Pending, Judgement.Right),
            judge(ga, raw("ㄱ", 10f, 10f, 30f, 10f), raw("ㄱ", 100f, 40f, 100f, 50f), raw("ㅎ", 100f, 100f, 300f, 100f)))   // a "long dot" and a short ㅣ are right
        assertEquals(Judgement.Wrong, judge(ga, raw("ㄴ", 10f, 10f)).last())                                           // another key
        assertEquals(Judgement.Wrong, judge(ga, raw("ㄱ", 10f, 10f), raw("ㄱ", 100f, 40f, 140f, 40f)).last())          // → for ㅣ
        assertEquals(Judgement.Wrong, judge(ga, raw("ㄱ", 10f, 10f), raw("ㄱ", 100f, 140f, 100f, 40f)).last())         // ↑
        assertEquals(Judgement.Wrong, judge(ga, raw("ㄱ", 10f, 10f), raw("ㄱ", 100f, 40f)).last())                     // no movement (D-60)
        assertEquals(Judgement.Pending, judge(ga, raw("ㄱ", 10f, 10f), raw("ㄱ", 100f, 40f, 100f, 42f)).last())        // any movement counts
    }

    @Test
    fun `TJ-04 3-1 - judged by result - a real tap, strokes the assembler takes, the vowel at the end`() {
        val gwa = HowTo.written("과")
        assertEquals(Jamo.WA, gwa.vowel)
        val dot = raw("ㄱ", 100f, 60f, 100f, 85f)
        val eu = raw("ㄱ", 50f, 90f, 150f, 90f)
        val i = raw("ㄱ", 160f, 40f, 160f, 160f)
        val dotA = raw("ㄱ", 160f, 100f, 185f, 100f)
        assertEquals(listOf(Judgement.Pending, Judgement.Pending, Judgement.Pending, Judgement.Pending, Judgement.Right),
            judge(gwa, raw("ㄱ", 10f, 10f), dot, eu, i, dotA))
        assertEquals(Judgement.Wrong, judge(gwa, raw("ㄱ", 10f, 10f, 40f, 10f)).last())                 // not a tap
        assertEquals(Judgement.Wrong, judge(gwa, raw("ㄱ", 10f, 10f), dot, raw("ㄱ", 130f, 90f, 230f, 90f)).last())   // ㅡ not under the dot (L > dot.x + t)
        assertEquals(Judgement.Wrong, judge(gwa, raw("ㄱ", 10f, 10f), dot, eu, i, raw("ㄱ", 160f, 100f, 160f, 110f)).last())   // a vertical stroke for the dot
    }

    @Test
    fun `TJ-09 3-1 with the practice keyboard's C is judged as the echo reads it - TBD-24`() {
        val c = kbd.hangul.stroke.ConsonantBox(64f, 52f, 144f, 148f)   // SWIPE 3.6.5 (가)
        fun judgeC(spec: ItemSpec, vararg r: RawStroke): List<Judgement> {
            val recs = ArrayList<ActionRecord>()
            return r.map { recs += rec(it); TutorItemJudge.judge(spec, recs, th, c) }
        }
        val gi = HowTo.written("기")
        val over = raw("ㄱ", 80f, 56f, 80f, 150f)                      // over C's left part: under-left = a dot, so not 기 (SWIPE 0.43)
        assertEquals(Judgement.Wrong, judgeC(gi, raw("ㄱ", 10f, 10f), over).last())
        assertEquals(Judgement.Right, judge(gi, raw("ㄱ", 10f, 10f), over).last())     // bare: any vertical is ㅣ
        assertEquals(Judgement.Right, judgeC(gi, raw("ㄱ", 10f, 10f), raw("ㄱ", 160f, 56f, 160f, 150f)).last())
        // 고 beside C: a long dot under C, then a shorter line (C beats the relative size, SWIPE 3.6.5 (가)).
        assertEquals(Judgement.Right, judgeC(HowTo.written("고"), raw("ㄱ", 10f, 10f), raw("ㄱ", 100f, 150f, 100f, 200f), raw("ㄱ", 70f, 205f, 110f, 205f)).last())
    }

    @Test
    fun `TJ-05 the stroke order of every vowel`() {
        assertEquals(21, HowTo.STROKES.size)
        assertEquals(listOf(true, false), HowTo.STROKES.getValue('ㅏ').map { it.vertical })
        assertEquals(listOf(true, false, true, false, true), HowTo.STROKES.getValue('ㅙ').map { it.vertical })
        assertEquals(listOf(false, true, false, true, true), HowTo.STROKES.getValue('ㅞ').map { it.vertical })
        assertEquals("ㅇ" to 'ㅢ', HowTo.split("의"))
    }

    @Test
    fun `TJ-06 how-to lines - SWIPE 10_1 words with the place words`() {
        assertEquals("q = q 탭", HowTo.press("q").howTo)
        assertEquals("가 = ㄱ 탭 → 자음 오른쪽에서 아래로 → ㅣ 오른쪽에 오른쪽으로", HowTo.measured("가").howTo)   // SWIPE 0.35 10.1 보기
        assertEquals("고 = ㄱ 탭 → 자음 아래에 아래로 → 점 아래에 오른쪽으로", HowTo.measured("고").howTo)
        assertEquals("과 = ㄱ 탭 → 자음 아래에 아래로 → 점 아래에 오른쪽으로 → ㅡ 오른쪽 끝에서 아래로 → ㅣ 오른쪽에 오른쪽으로",
            HowTo.written("과").howTo)
        assertEquals("까 = ㄲ 탭 → 자음 오른쪽에서 아래로 → ㅣ 오른쪽에 오른쪽으로", HowTo.measured("까").howTo)   // D-59: place words on every stroke
        assertEquals("의 = ㅇ 탭 → 자음 아래에 오른쪽으로 → ㅡ 오른쪽 끝에서 ㅡ가 아래쪽에 오게 아래로", HowTo.written("의").howTo)
        assertEquals("거 = ㄱ 탭 → 자음 오른쪽에 오른쪽으로 → 점 오른쪽 끝에서 점이 위나 가운데에 오게 아래로", HowTo.written("거").howTo)
        assertFalse(HowTo.STROKES.values.flatten().any { HowTo.stroke(it).contains("길게") || HowTo.stroke(it).contains("짧게") })   // issue #179
        assertEquals("획은 아래로, 오른쪽으로만 그어요", HowTo.REVERSE_HINT)
    }

    @Test
    fun `TJ-07 cheonjiin key records give elements and 3-4 is judged by them`() {
        val cjKey = HowTo.CHEONJIIN_KEY
        assertEquals(CjElement.DOT, rec(raw(cjKey, 25f, 25f)).element)
        assertEquals(CjElement.I, rec(raw(cjKey, 25f, 25f, 25f, -20f)).element)
        assertEquals(CjElement.I, rec(raw(cjKey, 25f, 25f, 25f, 70f)).element)
        assertEquals(CjElement.EU, rec(raw(cjKey, 25f, 25f, 70f, 25f)).element)
        val ga = HowTo.cheonjiin("가", listOf(CjElement.I, CjElement.DOT))
        assertEquals("가 = ㄱ 탭 → 천지인 키에서 위로 긋기 → 천지인 키 탭", ga.howTo)
        assertEquals(listOf(Judgement.Pending, Judgement.Pending, Judgement.Right),
            judge(ga, raw("ㄱ", 25f, 25f), raw(cjKey, 25f, 25f, 25f, 70f), raw(cjKey, 25f, 25f)))
        assertEquals(Judgement.Wrong, judge(ga, raw("ㄱ", 25f, 25f), raw(cjKey, 25f, 25f, 70f, 25f)).last())
    }

    @Test
    fun `TJ-08 rotation - 19 consonants in dictionary order and the initial swapped`() {
        assertEquals("ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ".map { it.toString() }, HowTo.ROTATION)
        assertEquals("나", HowTo.withInitial("가", "ㄴ"))
        assertEquals("꽤", HowTo.withInitial("괘", "ㄲ"))
        assertEquals("늬", HowTo.withInitial("의", "ㄴ"))
    }
}
