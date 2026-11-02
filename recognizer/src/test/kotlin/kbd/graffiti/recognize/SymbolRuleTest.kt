package kbd.graffiti.recognize

import kbd.graffiti.ink.GraffitiLang
import kbd.graffiti.ink.InkPoint
import kbd.graffiti.ink.InkStroke
import kbd.graffiti.ink.PadArea
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Symbol-table rule entries and retracing strokes on the production templates (LLD-05 §3.1 step 3, §7;
 * LLD-11 §6.1 GR-27..GR-32). Taps sit away from the letter area's corner (CORNER_RATIO).
 */
class SymbolRuleTest {

    private val rec = Recognizer(TestStrokes.production)

    private fun outputOf(t: StrokeToken): String? =
        ((t as? StrokeToken.Symbol)?.id?.let { TestStrokes.production.outputOf(it) } as? TemplateOutput.Text)?.value

    private fun tap(area: PadArea) =
        if (area == PadArea.LETTER) TestStrokes.line(150f, 120f, 2f, 1f, area) else TestStrokes.line(375f, 120f, 2f, 1f, area)

    /** Straight stroke of [len] dp in screen direction ([ux], [uy]), centred in [area]. */
    private fun line(ux: Float, uy: Float, len: Float, area: PadArea): InkStroke {
        val (cx, cy) = if (area == PadArea.LETTER) 150f to 120f else 375f to 120f
        return TestStrokes.line(cx - ux * len / 2, cy - uy * len / 2, ux * len, uy * len, area)
    }

    private val d = 0.7071f
    private val down = 0f to 1f
    private val up = 0f to -1f
    private val right = 1f to 0f
    private val downLeft = -d to d
    private val downRight = d to d

    private fun forAll(block: (PadArea, GraffitiLang) -> Unit) {
        for (area in PadArea.entries) for (lang in GraffitiLang.entries) block(area, lang)
    }

    @Test
    fun `GR-27 punctuation pending turns tap and straight lines into their symbols, any length, area and language`() {
        forAll { area, lang ->
            val c = "$area $lang"
            assertEquals(c, ".", outputOf(rec.recognize(tap(area), lang, Expect.PUNCT)))
            for (len in listOf(15f, 120f)) {
                assertEquals("$c $len", ",", outputOf(rec.recognize(line(downLeft.first, downLeft.second, len, area), lang, Expect.PUNCT)))
            }
            assertEquals(c, "'", outputOf(rec.recognize(line(down.first, down.second, 40f, area), lang, Expect.PUNCT)))
            assertEquals(c, "-", outputOf(rec.recognize(line(right.first, right.second, 40f, area), lang, Expect.PUNCT)))
            assertEquals(c, "!", outputOf(rec.recognize(line(up.first, up.second, 40f, area), lang, Expect.PUNCT)))
            assertEquals(c, "\\", outputOf(rec.recognize(line(downRight.first, downRight.second, 40f, area), lang, Expect.PUNCT)))
        }
    }

    @Test
    fun `GR-28 extended pending has plus-minus, times and inverted exclamation, but no tap, down-right or down line`() {
        forAll { area, lang ->
            val c = "$area $lang"
            assertEquals(c, "±", outputOf(rec.recognize(line(right.first, right.second, 40f, area), lang, Expect.EXTENDED)))
            for (len in listOf(15f, 120f)) {
                assertEquals("$c $len", "×", outputOf(rec.recognize(line(downLeft.first, downLeft.second, len, area), lang, Expect.EXTENDED)))
            }
            assertEquals(c, "¡", outputOf(rec.recognize(line(up.first, up.second, 40f, area), lang, Expect.EXTENDED)))
            assertEquals(c, StrokeToken.Tap, rec.recognize(tap(area), lang, Expect.EXTENDED))
            assertEquals(c, StrokeToken.DiagDownRight, rec.recognize(line(downRight.first, downRight.second, 40f, area), lang, Expect.EXTENDED))
            assertEquals(c, StrokeToken.LineDown, rec.recognize(line(down.first, down.second, 40f, area), lang, Expect.EXTENDED))
        }
    }

    @Test
    fun `GR-29 corner tap and the backspace line are never symbols`() {
        val corner = InkStroke(listOf(InkPoint(10f, 10f, 0L), InkPoint(11f, 10f, 8L)), PadArea.LETTER, TestStrokes.LETTER_RECT)
        for (lang in GraffitiLang.entries) for (expect in listOf(Expect.PUNCT, Expect.EXTENDED)) {
            assertEquals("$lang $expect", StrokeToken.CornerTap, rec.recognize(corner, lang, expect))
            assertEquals("$lang $expect", StrokeToken.LineLeft, rec.recognize(line(-1f, 0f, 40f, PadArea.LETTER), lang, expect))
        }
    }

    @Test
    fun `GR-30 retracing strokes are not straight lines - B 1, D 1 and H match their templates, wedges are no line`() {
        for (id in listOf("en_b", "en_d", "en_h")) {
            val t = TestStrokes.production.sets.getValue(SetName.EN_LETTER).first { it.id == id }
            val verts = t.strokes[0].map { (x, y) -> x.toDouble() to y.toDouble() }
            val s = TestStrokes.letter(TestStrokes.points(verts, amp = 0.5))
            assertNull(id, StrokeRules.line(s))
            assertEquals(id, StrokeToken.Symbol(id), rec.recognize(s, GraffitiLang.EN, Expect.NORMAL))
        }
        // One leg fully retraced, slightly apart (a thin wedge): up and back down, right and back left.
        val wedges = listOf(
            listOf(0.0 to 1.0, 0.05 to 0.0, 0.1 to 1.0),
            listOf(0.0 to 0.0, 1.0 to 0.05, 0.0 to 0.1),
        )
        for (w in wedges) assertNull("$w", StrokeRules.line(TestStrokes.letter(TestStrokes.points(w, amp = 0.0))))
    }

    @Test
    fun `GR-32 without a pending shift taps and lines stay rule tokens though the tables have rule entries`() {
        val cases = listOf(
            tap(PadArea.LETTER) to StrokeToken.Tap,
            line(down.first, down.second, 40f, PadArea.LETTER) to StrokeToken.LineDown,
            line(right.first, right.second, 40f, PadArea.LETTER) to StrokeToken.LineRight,
            line(up.first, up.second, 40f, PadArea.LETTER) to StrokeToken.LineUp,
            line(downLeft.first, downLeft.second, 40f, PadArea.LETTER) to StrokeToken.DiagDownLeft,
            line(downRight.first, downRight.second, 40f, PadArea.LETTER) to StrokeToken.DiagDownRight,
        )
        for (lang in GraffitiLang.entries) for ((s, token) in cases) assertEquals("$lang $token", token, rec.recognize(s, lang, Expect.NORMAL))
    }

    @Test
    fun `ruleSymbol looks only in the pending table and never for NORMAL`() {
        val t = TestStrokes.production
        assertEquals("p_excl", t.ruleSymbol(Expect.PUNCT, StrokeToken.LineUp))
        assertEquals("x_iexcl", t.ruleSymbol(Expect.EXTENDED, StrokeToken.LineUp))
        assertNull(t.ruleSymbol(Expect.NORMAL, StrokeToken.LineUp))
        assertNull(t.ruleSymbol(Expect.EXTENDED, StrokeToken.Tap))
        assertNull(t.ruleSymbol(Expect.PUNCT, StrokeToken.LineLeft))
        assertNull(t.ruleSymbol(Expect.PUNCT, StrokeToken.CornerTap))
    }

    // ---- GR-31: TemplateLoader checks on `rule` (LLD-05 §7) ----

    private fun load(punct: String, number: String = ""): TemplateSet =
        TemplateLoader.load(
            """{"version": 1, "sets": {"NUMBER": [$number], "PUNCT": [$punct]}}""".byteInputStream(),
        )

    private fun entry(id: String, rule: String?, strokes: String = "[]") =
        """{"id": "$id", "output": {"type": "text", "value": "."}, "corners": 0,""" +
            (rule?.let { """ "rule": "$it",""" } ?: "") + """ "strokes": $strokes}"""

    @Test
    fun `GR-31 rule entries outside a symbol table, unknown or forbidden rules, with strokes or twice are rejected`() {
        val bad = listOf(
            "NUMBER set" to { load("", number = entry("n", "TAP")) },
            "LINE_LEFT" to { load(entry("p", "LINE_LEFT")) },
            "CORNER_TAP" to { load(entry("p", "CORNER_TAP")) },
            "unknown" to { load(entry("p", "CIRCLE")) },
            "strokes" to { load(entry("p", "TAP", "[[[0, 0], [1, 1]]]")) },
            "twice" to { load(entry("p", "TAP") + ", " + entry("q", "TAP")) },
        )
        for ((name, f) in bad) assertThrows(name, IllegalStateException::class.java) { f() }
    }

    @Test
    fun `GR-31 a file without rule entries still loads, and a rule entry is read`() {
        val old = load(entry("p", null, "[[[0, 0], [1, 1]]]"))
        assertNull(old.sets.getValue(SetName.PUNCT).single().rule)
        val withRule = load(entry("p", "DIAG_DOWN_LEFT"))
        assertEquals(StrokeToken.DiagDownLeft, withRule.sets.getValue(SetName.PUNCT).single().rule)
        assertEquals("p", withRule.ruleSymbol(Expect.PUNCT, StrokeToken.DiagDownLeft))
    }
}
