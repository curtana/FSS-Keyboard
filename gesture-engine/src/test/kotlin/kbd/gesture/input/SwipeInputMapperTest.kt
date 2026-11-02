package kbd.gesture.input

import kbd.gesture.classify.StrokeOutcome
import kbd.gesture.classify.StrokeResult
import kbd.gesture.classify.SwipeKeyKind
import kbd.gesture.input.SwipeTypist.Companion.h
import kbd.gesture.input.SwipeTypist.Companion.v
import kbd.hangul.cheonjiin.CjElement
import kbd.hangul.event.JamoEvent
import kbd.hangul.jamo.Jamo
import kbd.stroke.threshold.ThresholdDefaults
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * LLD-11 §4.4: SwipeInputMapper → HangulComposer(SWIPE) through [SwipeTypist] (HLD 4.3, SWIPE 3 and 5,
 * issue #173): a tap = the key's consonant (double keys = ordinary keys, no time judgement); vowels
 * are handwriting strokes assembled by stroke order and position; finals are taps again.
 */
class SwipeInputMapperTest {

    private fun t() = SwipeTypist()

    private fun check(expect: String, typist: SwipeTypist) = assertEquals(expect, typist.shown)

    // --- SWIPE 5.3 input examples ------------------------------------------------------------

    @Test fun `SM-01 ga and geo, go and gu in stroke order`() {
        check("가", t().tap("ㄱ").vowel("ㅏ"))
        check("거", t().tap("ㄱ").vowel("ㅓ"))
        check("고", t().tap("ㄱ").vowel("ㅗ"))
        check("구", t().tap("ㄱ").vowel("ㅜ"))
    }
    @Test fun `SM-02 gak - a tap after a syllable is the final`() = check("각", t().tap("ㄱ").vowel("ㅏ").tap("ㄱ"))
    @Test fun `SM-03 gagi - a vowel after a final moves it by dokkaebibul`() = check("가기", t().tap("ㄱ").vowel("ㅏ").tap("ㄱ").vowel("ㅣ"))
    @Test fun `SM-04 gokgi and gokki - the key pressed decides, no time judgement`() {
        check("곡기", t().tap("ㄱ").vowel("ㅗ").tap("ㄱ").tap("ㄱ").vowel("ㅣ"))
        check("고끼", t().tap("ㄱ").vowel("ㅗ").tap("ㄲ").vowel("ㅣ"))
    }
    @Test fun `SM-06 kka on the double key`() = check("까", t().tap("ㄲ").vowel("ㅏ"))
    @Test fun `SM-07 bakk, bakga, bakka, bakkka`() {
        check("밖", t().tap("ㅂ").vowel("ㅏ").tap("ㄲ"))
        check("박가", t().tap("ㅂ").vowel("ㅏ").tap("ㄱ").tap("ㄱ").vowel("ㅏ"))
        check("바까", t().tap("ㅂ").vowel("ㅏ").tap("ㄲ").vowel("ㅏ"))
        check("박까", t().tap("ㅂ").vowel("ㅏ").tap("ㄱ").tap("ㄲ").vowel("ㅏ"))
    }
    @Test fun `SM-11 appa - pp cannot be a final`() = check("아빠", t().tap("ㅇ").vowel("ㅏ").tap("ㅃ").vowel("ㅏ"))
    @Test fun `SM-12 annyeong`() = check("안녕", t().tap("ㅇ").vowel("ㅏ").tap("ㄴ").tap("ㄴ").vowel("ㅕ").tap("ㅇ"))
    @Test fun `SM-13 haetda`() = check("했다", t().tap("ㅎ").vowel("ㅐ").tap("ㅆ").tap("ㄷ").vowel("ㅏ"))
    @Test fun `SM-14 dalk - compound finals are taps in a row`() = check("닭", t().tap("ㄷ").vowel("ㅏ").tap("ㄹ").tap("ㄱ"))
    @Test fun `SM-15 gwa, goe, ui, dwaeji, kkwae - compound vowels as written`() {
        check("과", t().tap("ㄱ").vowel("ㅘ"))
        check("괴", t().tap("ㄱ").vowel("ㅚ"))
        check("의", t().tap("ㅇ").vowel("ㅢ"))
        check("돼지", t().tap("ㄷ").vowel("ㅙ").tap("ㅈ").vowel("ㅣ"))
        check("꽤", t().tap("ㄲ").vowel("ㅙ"))
    }
    @Test fun `SM-16 two taps are two consonants whatever the speed`() {
        check("ㄱㄱ", t().tap("ㄱ").tap("ㄱ"))
        check("ㅋㅋ", t().tap("ㅋ").tap("ㅋ"))
        check("곡ㄱ", t().tap("ㄱ").vowel("ㅗ").tap("ㄱ").tap("ㄱ"))
    }
    @Test fun `SM-17 every vowel after a consonant`() {
        for ((vw, _) in SwipeTypist.SHAPES) {
            val j = checkNotNull(Jamo.ofCompat(vw.single()))
            val syllable = (0xAC00 + j.jungIndex * 28).toChar().toString()   // ㄱ + vowel
            check(syllable, t().tap("ㄱ").vowel(vw))
        }
    }
    @Test fun `SM-18 lone vowels - no consonant, closed by a space`() = check("ㅠ ㅠ", t().vowel("ㅠ").space().vowel("ㅠ"))
    @Test fun `SM-19 the key a stroke starts on does not matter`() {
        val x = t().tap("ㄱ")
        SwipeTypist.SHAPES_C.getValue("ㅏ").forEachIndexed { i, s -> x.stroke(s, key = listOf("ㅎ", "ㅃ")[i]) }
        check("가", x)
    }

    @Test fun `SM-20 accept events - tap, double key, strokes, text`() {
        val m = SwipeInputMapper(ThresholdDefaults.VALUES)
        fun r(key: String, o: StrokeOutcome) = StrokeResult(key, SwipeKeyKind.CONSONANT, o, 0, null, 50)
        assertEquals(MapResult.Jamo(listOf(JamoEvent.Consonant(Jamo.G))), m.accept(r("ㄱ", StrokeOutcome.Tap)))
        assertEquals(MapResult.Jamo(listOf(JamoEvent.Consonant(Jamo.GG))), m.accept(r("ㄲ", StrokeOutcome.Tap)))
        assertEquals(MapResult.Jamo(listOf(JamoEvent.Vowel(Jamo.I))), m.accept(r("ㅎ", StrokeOutcome.Stroke(v(100f, 40f, 160f)))))
        assertEquals(MapResult.Jamo(listOf(JamoEvent.ReplaceVowel(Jamo.A))), m.accept(r("ㄱ", StrokeOutcome.Stroke(h(100f, 100f, 125f)))))
        assertEquals(MapResult.Ignored, m.accept(r("ㄱ", StrokeOutcome.Reverse)))
        assertEquals(MapResult.Text("1", false), m.accept(StrokeResult("1", SwipeKeyKind.NUMBER, StrokeOutcome.Text("1", false), 0, null, 50)))
    }

    // --- ignored strokes and closing (SWIPE 3.2, 3.3) ---------------------------------------------

    @Test fun `SM-21 a reversed or misplaced stroke is ignored, the assembly stays`() {
        val x = t().tap("ㄱ").vowel("ㅣ").reverse()
        check("기", x)
        assertEquals(1, x.errors)
        x.stroke(v(235f, 56f, 150f))   // a second ㅣ beside C: no candidate
        assertEquals(2, x.errors)
        check("가", x.stroke(h(100f, 235f, 260f)))
    }
    @Test fun `SM-22 a tap closing unfinished strokes drops them with the error vibration`() {
        val x = t().tap("ㄱ").stroke(SwipeTypist.DOT_C)   // one dot: no vowel yet
        check("ㄱㆍ", x)
        x.tap("ㄴ")
        check("ㄱㄴ", x)
        assertEquals(1, x.errors)
        val y = t().tap("ㄱ").vowel("ㅜ").stroke(h(185f, 185f, 210f)).tap("ㄴ")   // ㅜ + dot, then ㄴ
        check("군", y)
        assertEquals(1, y.errors)
        val z = t().tap("ㄱ").vowel("ㅏ").tap("ㄴ")   // a vowel closes quietly
        assertEquals(0, z.errors)
        val w = t().tap("ㄱ").stroke(SwipeTypist.DOT_C).space()
        check("ㄱ ", w)
        assertEquals(1, w.errors)
    }

    @Test fun `SM-23 after a consonant strokes are judged against C, without one they are bare - TBD-24`() {
        // The same two strokes: a vertical, then a shorter horizontal at its bottom (SWIPE 3.6.5 (가) row 4).
        val strokes = listOf(v(175f, 150f, 200f), h(205f, 145f, 185f))
        check("고", t().tap("ㄱ").also { x -> strokes.forEach { x.stroke(it) } })   // C: a dot under C, then the underline
        check("ㅏ", t().also { x -> strokes.forEach { x.stroke(it) } })              // bare: the shorter line is ㅏ's dot
        // A vertical through C's left part is under-left = a dot with C (SWIPE 0.43: nothing is dropped); bare it is ㅣ.
        val over = t().tap("ㄱ").stroke(v(170f, 56f, 150f))
        check("ㄱㆍ", over)
        assertEquals(0, over.errors)
        check("ㅣ", t().stroke(v(170f, 56f, 150f)))
    }

    @Test fun `SM-24 bare two verticals show only the dots - SWIPE 0_36 3_4, D-64`() {
        val x = t().stroke(v(90f, 40f, 70f))
        check("ㅣ", x)
        check("‥", x.stroke(v(110f, 40f, 70f)))          // the first ㅣ became ㅛ's dot: "‥" alone
        check("ㅣ", x.backspace())                        // ⌫ brings the ㅣ back
        check("‥", x.stroke(v(110f, 40f, 70f)))
        check("ㅛ", x.stroke(h(100f, 60f, 140f)))
        val y = t().stroke(h(80f, 60f, 80f)).stroke(h(120f, 60f, 80f))
        check("‥", y)
        check("ㅡ ", y.space())                           // closing leaves the first stroke's ㅡ, with the vibration
        assertEquals(1, y.errors)
    }

    // --- ⌫ (SWIPE 3.5) ---------------------------------------------------------------------------

    @Test fun `SM-40 open assembly - one stroke at a time`() {
        val x = t().tap("ㄱ").vowel("ㅏ")
        check("기", x.backspace())
        check("ㄱ", x.backspace())
        val y = t().tap("ㄱ").vowel("ㅗ")
        check("ㄱㆍ", y.backspace())
        check("ㄱ", y.backspace())
    }
    @Test fun `SM-41 closed assembly - one change at a time, never reopened`() {
        val x = t().tap("ㄱ").vowel("ㅏ").tap("ㄴ")   // 간
        check("가", x.backspace())
        check("기", x.backspace())
        check("ㄱ", x.backspace())
        val y = t().tap("ㄱ").vowel("ㅗ").tap("ㄱ")   // 곡: ㅗ was one change
        check("고", y.backspace())
        check("ㄱ", y.backspace())
        val z = t().tap("ㄱ").vowel("ㅘ").space().backspace()
        check("과", z)
        val w = t().tap("ㄱ").vowel("ㅏ").tap("ㄴ").backspace().vowel("ㅣ")   // 간 ⌫ = 가, then ㅣ: a new vowel
        check("가ㅣ", w)
    }
    @Test fun `SM-42 dalk backspace thrice`() {
        val x = t().tap("ㄷ").vowel("ㅏ").tap("ㄹ").tap("ㄱ")
        check("달", x.backspace())
        check("다", x.backspace())
        check("디", x.backspace())
    }

    @Test fun `SM-45 snapshot and restore keep both assemblies`() {
        val x = t().tap("ㄱ").stroke(SwipeTypist.DOT_C)
        val s = x.mapper.snapshot()
        x.mapper.reset()
        x.mapper.restore(s)
        assertEquals("ㆍ", x.mapper.pendingGlyph())
    }

    // --- Cheonjiin key (SWIPE 5.4, issue #160, kept by #173) -------------------------------------

    @Test fun `CJ-50 ga by the cheonjiin key after a consonant tap`() {
        check("가", t().tap("ㄱ").cj("ㅣㆍ"))
        check("과", t().tap("ㄱ").cj("ㆍㅡㅣㆍ"))
        check("까", t().tap("ㄲ").cj("ㅣㆍ"))
    }
    @Test fun `CJ-52 vowel strokes and cheonjiin elements never join`() {
        check("기ㅡ", t().tap("ㄱ").vowel("ㅣ").cj("ㅡ"))
        check("기ㅣ", t().tap("ㄱ").cj("ㅣ").vowel("ㅣ"))
        val x = t().tap("ㄱ").stroke(SwipeTypist.DOT_C).cj("ㅣ")   // an unfinished stroke dropped by the element
        check("기", x)
        assertEquals(1, x.errors)
    }
    @Test fun `CJ-56 backspace takes back one element`() {
        val x = t().tap("ㄱ").cj("ㅣㆍ")
        check("기", x.backspace())
        check("ㄱ", x.backspace())
    }
    @Test fun `CJ-60 the element event per push`() {
        val m = SwipeInputMapper(ThresholdDefaults.VALUES)
        fun e(x: CjElement) = m.accept(StrokeResult("cheonjiin", SwipeKeyKind.CHEONJIIN, StrokeOutcome.Element(x), 0, null, 50))
        assertEquals(MapResult.Jamo(emptyList()), e(CjElement.DOT))
        assertEquals("ㆍ", m.pendingGlyph())
        assertEquals(MapResult.Jamo(listOf(JamoEvent.Vowel(Jamo.EO))), e(CjElement.I))
        assertEquals(MapResult.Ignored, e(CjElement.DOT))
    }

    @Test
    fun `TH-10 the position tolerance follows the drawn key width`() {
        assertEquals(20.4f, ThresholdDefaults.posTolFor(51f), 1e-4f)
        val m = SwipeInputMapper(ThresholdDefaults.VALUES)
        assertEquals(20f, m.th.posTolDp, 0f)                // until the view knows its width
        m.setKeyWidth(50f)
        assertEquals(20f, m.th.posTolDp, 1e-4f)
        m.setKeyWidth(101.6f)                                // a 711 dp tablet: t follows the key (PM decision, FSS#17)
        assertEquals(40.64f, m.th.posTolDp, 1e-4f)
        assertEquals(ThresholdDefaults.VALUES.minCrossDp, m.th.minCrossDp, 0f)   // the other values stay absolute dp
    }
}
