package kbd.graffiti.eval

import kbd.graffiti.ink.GraffitiLang
import kbd.graffiti.ink.InkPoint
import kbd.graffiti.ink.InkStroke
import kbd.graffiti.ink.PadArea
import kbd.graffiti.recognize.Expect
import kbd.graffiti.recognize.Recognizer
import kbd.graffiti.recognize.SetName
import kbd.graffiti.recognize.StrokeRules
import kbd.graffiti.recognize.StrokeToken
import kbd.graffiti.recognize.TemplateLoader
import kbd.graffiti.recognize.TemplateMatcher
import kbd.graffiti.recognize.TemplateOutput
import kbd.hangul.jamo.Jamo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The production `graffiti/templates.json` (LLD-05 §7) covers the stroke set and is consistent with the matcher. */
class TemplateFileTest {

    private val templates = GraffitiEval.templates

    private fun outputs(set: SetName): Set<TemplateOutput> = templates.sets[set].orEmpty().map { it.output }.toSet()

    @Test
    fun `KO_LETTER has the 14 consonants, the 5 one-stroke doubles, SPACE_V and FINISH_J`() {
        val jamo = "ㄱㄴㄷㄹㅁㅂㅅㅇㅈㅊㅋㅌㅍㅎ" + "ㄲㄸㅃㅆㅉ"     // GRAFFITI 4.2
        val expected = jamo.map { TemplateOutput.Jamo(Jamo.ofCompat(it)!!) }.toSet() +
            TemplateLoader.CONTROL_VALUES.map { TemplateOutput.Control(it) }
        assertEquals(expected, outputs(SetName.KO_LETTER))
    }

    @Test
    fun `EN_LETTER has every letter but i, NUMBER every digit but 1 (those are the down line rule)`() {
        assertEquals(('a'..'z').filter { it != 'i' }.map { TemplateOutput.Text("$it") }.toSet(), outputs(SetName.EN_LETTER))
        assertEquals(('0'..'9').filter { it != '1' }.map { TemplateOutput.Text("$it") }.toSet(), outputs(SetName.NUMBER))
        assertTrue(outputs(SetName.PUNCT).isNotEmpty())
        assertTrue(outputs(SetName.EXTENDED).isNotEmpty())
    }

    @Test
    fun `PUNCT and EXTENDED follow the Graffiti 1 tables, with the tap and line symbols as rule entries`() {
        // GRAFFITI 6.4 (owner decisions #79 OD-2, OD-4, OD-7 A); wedge symbols and ∫, Ø are left out until settled.
        val punct = ". , ' - ( ) ? / $ ! @ # % ^ & * < > + = \\ { } [ ] ~ \"".split(" ").toSet() + "\t"
        assertEquals(punct.map { TemplateOutput.Text(it) }.toSet(), outputs(SetName.PUNCT))
        val ext = "™ ® © ‘ ’ “ ” § ° + ± × ÷ = ¢ ¥ £ ¿ ¡ ß µ €".split(" ")
        assertEquals(ext.map { TemplateOutput.Text(it) }.toSet(), outputs(SetName.EXTENDED))
        fun rules(set: SetName) = templates.sets[set].orEmpty().filter { it.rule != null }
            .associate { it.rule!! to (it.output as TemplateOutput.Text).value }
        assertEquals(
            mapOf(
                StrokeToken.Tap to ".", StrokeToken.DiagDownLeft to ",", StrokeToken.LineDown to "'",
                StrokeToken.LineRight to "-", StrokeToken.LineUp to "!", StrokeToken.DiagDownRight to "\\",
            ),
            rules(SetName.PUNCT),
        )
        assertEquals(
            mapOf(StrokeToken.LineRight to "±", StrokeToken.DiagDownLeft to "×", StrokeToken.LineUp to "¡"),
            rules(SetName.EXTENDED),
        )
    }

    @Test
    fun `templates missing from Graffiti 1 are gone - no closed 4, no bullet`() {
        assertTrue(templates.sets.values.flatten().none { it.id == "num_4b" })
        assertTrue(outputs(SetName.EXTENDED).none { it == TemplateOutput.Text("•") })
    }

    @Test
    fun `corners of every template is the corner count the matcher finds on each of its strokes`() {
        for ((_, list) in templates.sets) for (t in list) {
            if (t.rule != null) {   // a rule entry has no strokes to match (LLD-05 §7)
                assertTrue(t.id, t.strokes.isEmpty())
                continue
            }
            assertTrue(t.id, t.strokes.isNotEmpty())
            for (s in t.strokes) {
                val counted = TemplateMatcher.cornerCount(TemplateMatcher.normalize(TemplateMatcher.resample(s)))
                assertEquals(t.id, t.corners, counted)
            }
        }
    }

    @Test
    fun `the backspace stroke is LineLeft while punctuation or extended shift is pending, in no symbol table`() {
        // FR-GRF-05c ⑥, GRAFFITI 6.2-8 ①: ← is never a symbol (owner decision #79 OD-3 A, issue #101).
        val recognizer = Recognizer(templates)
        val left = InkStroke((0..50).map { i -> InkPoint(200f - 2f * i, 120f, i * 8L) }, PadArea.LETTER, GraffitiEval.LETTER_RECT)
        for (lang in GraffitiLang.entries) for (expect in listOf(Expect.PUNCT, Expect.EXTENDED)) {
            assertEquals("$lang $expect", StrokeToken.LineLeft, recognizer.recognize(left, lang, expect))
        }
    }

    @Test
    fun `no template stroke is a tap or a rule line, which recognize decides before the templates`() {
        for ((_, list) in templates.sets) for (t in list) for (s in t.strokes) {
            // Drawn 60 dp wide, as a stroke of the pad.
            val ink = InkStroke(s.mapIndexed { i, (x, y) -> InkPoint(x * 60f, y * 60f, i * 8L) }, PadArea.LETTER, GraffitiEval.LETTER_RECT)
            assertNull(t.id, StrokeRules.tap(ink))
            assertNull(t.id, StrokeRules.line(ink))
        }
    }
}
