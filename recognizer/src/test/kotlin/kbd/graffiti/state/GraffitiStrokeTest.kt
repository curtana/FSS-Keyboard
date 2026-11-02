package kbd.graffiti.state

import kbd.graffiti.ink.GraffitiLang
import kbd.graffiti.ink.GraffitiLang.EN
import kbd.graffiti.ink.GraffitiLang.KO
import kbd.graffiti.ink.InkPoint
import kbd.graffiti.ink.InkStroke
import kbd.graffiti.ink.PadArea
import kbd.graffiti.ink.PadArea.LETTER
import kbd.graffiti.ink.PadArea.NUMBER
import kbd.graffiti.recognize.Expect
import kbd.graffiti.recognize.Recognizer
import kbd.graffiti.recognize.TestStrokes
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Stroke-level GraffitiState tests on the production templates (LLD-11 §6.3 GS-34, GS-39..GS-45): the
 * recognizer turns a tap or line into its symbol while that table's shift is pending (LLD-05 §3.1 step 3),
 * so it reaches rows 2–3 instead of the control-stroke rows. `expect` follows the pending flag as in
 * GraffitiInputHandler.onStroke. Lines are 40 dp, taps 2 dp away from the letter area's corner.
 */
class GraffitiStrokeTest {

    private val templates = TestStrokes.production
    private val rec = Recognizer(templates)

    private enum class Pending { EXT, PUNCT }

    private fun state(lang: GraffitiLang, pending: Pending) = GraffitiState(templates).apply {
        this.lang = lang
        extPending = pending == Pending.EXT
        punctPending = pending == Pending.PUNCT
    }

    private fun GraffitiState.flags() = listOf(shiftPending, extPending, punctPending)

    /** Recognize [stroke] in the state's pending context, then interpret it. */
    private fun GraffitiState.stroke(stroke: InkStroke): GraffitiAction {
        val expect = when {
            extPending -> Expect.EXTENDED
            punctPending -> Expect.PUNCT
            else -> Expect.NORMAL
        }
        return interpret(rec.recognize(stroke, lang, expect), stroke.area)
    }

    private fun centre(area: PadArea) = if (area == LETTER) 150f to 120f else 375f to 120f

    private fun line(ux: Float, uy: Float, area: PadArea, len: Float = 40f): InkStroke {
        val (cx, cy) = centre(area)
        return TestStrokes.line(cx - ux * len / 2, cy - uy * len / 2, ux * len, uy * len, area)
    }

    private fun tap(area: PadArea): InkStroke {
        val (cx, cy) = centre(area)
        return TestStrokes.line(cx, cy, 2f, 1f, area)
    }

    private val d = 0.7071f
    private fun text(t: String) = GraffitiAction.CommitText(t)
    private val off = listOf(false, false, false)

    private fun check(lang: GraffitiLang, pending: Pending, stroke: InkStroke, expected: GraffitiAction, msg: String) {
        val s = state(lang, pending)
        assertEquals(msg, expected, s.stroke(stroke))
        assertEquals(msg, off, s.flags())
    }

    @Test
    fun `GS-34 punct or ext pending then the up line is the exclamation mark, not shift`() {
        for (lang in GraffitiLang.entries) for (area in PadArea.entries) {
            check(lang, Pending.PUNCT, line(0f, -1f, area), text("!"), "$lang $area punct")
            check(lang, Pending.EXT, line(0f, -1f, area), text("¡"), "$lang $area ext")
        }
    }

    @Test
    fun `GS-39 punct or ext pending then a short or long down-left line is comma or times, not Enter`() {
        for (lang in GraffitiLang.entries) for (len in listOf(15f, 120f)) {
            check(lang, Pending.PUNCT, line(-d, d, LETTER, len), text(","), "$lang $len punct")
            check(lang, Pending.EXT, line(-d, d, LETTER, len), text("×"), "$lang $len ext")
        }
    }

    @Test
    fun `GS-40 punct or ext pending then the right line is hyphen or plus-minus, not Space or the eu element`() {
        check(EN, Pending.PUNCT, line(1f, 0f, LETTER), text("-"), "EN letter")
        check(KO, Pending.PUNCT, line(1f, 0f, NUMBER), text("-"), "KO number")
        check(KO, Pending.PUNCT, line(1f, 0f, LETTER), text("-"), "KO letter (OD-5 A)")
        check(EN, Pending.EXT, line(1f, 0f, LETTER), text("±"), "EN ext")
        check(KO, Pending.EXT, line(1f, 0f, LETTER), text("±"), "KO ext")
    }

    @Test
    fun `GS-41 punct pending then a tap is the period, in any area`() {
        check(EN, Pending.PUNCT, tap(LETTER), text("."), "EN letter")
        check(KO, Pending.PUNCT, tap(NUMBER), text("."), "KO number")
        check(KO, Pending.PUNCT, tap(LETTER), text("."), "KO letter (OD-5 A)")
    }

    @Test
    fun `GS-42 punct pending then the down line is the apostrophe, not i, 1 or the i element`() {
        check(EN, Pending.PUNCT, line(0f, 1f, LETTER), text("'"), "EN letter")
        check(EN, Pending.PUNCT, line(0f, 1f, NUMBER), text("'"), "number")
        check(KO, Pending.PUNCT, line(0f, 1f, LETTER), text("'"), "KO letter (OD-5 A)")
    }

    @Test
    fun `GS-43 punct pending then the down-right line is backslash, ext pending stays ext pending`() {
        for (lang in GraffitiLang.entries) {
            check(lang, Pending.PUNCT, line(d, d, LETTER), text("\\"), "$lang punct (OD-4 A)")
            val s = state(lang, Pending.EXT)
            assertEquals("$lang ext", GraffitiAction.StateOnly, s.stroke(line(d, d, LETTER)))
            assertEquals("$lang ext", listOf(false, true, false), s.flags())
        }
    }

    @Test
    fun `GS-44 punct pending then the corner tap toggles the language, ext pending then a tap toggles too`() {
        val corner = InkStroke(listOf(InkPoint(10f, 10f, 0L), InkPoint(11f, 10f, 8L)), LETTER, TestStrokes.LETTER_RECT)
        for (lang in GraffitiLang.entries) {
            check(lang, Pending.PUNCT, corner, GraffitiAction.ToggleLang, "$lang punct corner")
            check(lang, Pending.EXT, tap(LETTER), GraffitiAction.ToggleLang, "$lang ext tap (OD-7 A)")
        }
    }

    @Test
    fun `GS-45 KO punct pending then consonant shapes in the letter area are symbols`() {
        val circle = TestStrokes.letter(TestStrokes.points(TestStrokes.CIRCLE))
        check(KO, Pending.PUNCT, circle, text("@"), "ㅇ shape")
        check(KO, Pending.PUNCT, TestStrokes.letter(TestStrokes.points(ALPHA)), text("+"), "ㅈ shape, punct")
        check(KO, Pending.EXT, TestStrokes.letter(TestStrokes.points(ALPHA)), text("+"), "ㅈ shape, ext")
    }

    private companion object {
        // 'α' loop from the top-right tail (ㅈ, K, +), drawn for this test.
        val ALPHA = listOf(1.0 to 0.02, 0.62 to 0.52, 0.3 to 0.97, 0.03 to 0.72, 0.06 to 0.27, 0.32 to 0.03, 0.58 to 0.47, 0.98 to 1.0)
    }
}
