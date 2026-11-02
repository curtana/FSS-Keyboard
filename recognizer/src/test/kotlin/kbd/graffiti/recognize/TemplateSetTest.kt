package kbd.graffiti.recognize

import kbd.graffiti.ink.GraffitiLang
import kbd.graffiti.ink.PadArea
import kbd.hangul.jamo.Jamo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** `TemplateSet.forContext` table and `outputOf` (LLD-05 §3.3, §7; no LLD-11 ID). */
class TemplateSetTest {

    private fun template(id: String, output: TemplateOutput) =
        Template(id, output, corners = 0, strokes = listOf(listOf(0f to 0f, 1f to 1f)))

    // One distinct template per set, so the returned list identifies the set.
    private val bySet: Map<SetName, List<Template>> = mapOf(
        SetName.KO_LETTER to listOf(template("ko_g", TemplateOutput.Jamo(Jamo.G))),
        SetName.EN_LETTER to listOf(template("en_a", TemplateOutput.Text("a"))),
        SetName.NUMBER to listOf(template("num_1", TemplateOutput.Text("1"))),
        SetName.PUNCT to listOf(template("p_dot", TemplateOutput.Text("."))),
        SetName.EXTENDED to listOf(template("x_at", TemplateOutput.Text("@"))),
    )
    private val set = TemplateSet(bySet)

    @Test
    fun `PUNCT and EXTENDED win for any area and lang`() {
        for (area in PadArea.entries) for (lang in GraffitiLang.entries) {
            assertEquals("$area $lang", bySet[SetName.PUNCT], set.forContext(area, lang, Expect.PUNCT))
            assertEquals("$area $lang", bySet[SetName.EXTENDED], set.forContext(area, lang, Expect.EXTENDED))
        }
    }

    @Test
    fun `NORMAL in the number area is NUMBER for either lang`() {
        for (lang in GraffitiLang.entries) {
            assertEquals("$lang", bySet[SetName.NUMBER], set.forContext(PadArea.NUMBER, lang, Expect.NORMAL))
        }
    }

    @Test
    fun `NORMAL in the letter area follows lang`() {
        assertEquals(bySet[SetName.KO_LETTER], set.forContext(PadArea.LETTER, GraffitiLang.KO, Expect.NORMAL))
        assertEquals(bySet[SetName.EN_LETTER], set.forContext(PadArea.LETTER, GraffitiLang.EN, Expect.NORMAL))
    }

    @Test
    fun `a set missing from the map is empty`() {
        val partial = TemplateSet(mapOf(SetName.KO_LETTER to bySet.getValue(SetName.KO_LETTER)))
        assertTrue(partial.forContext(PadArea.LETTER, GraffitiLang.EN, Expect.NORMAL).isEmpty())
        assertTrue(partial.forContext(PadArea.NUMBER, GraffitiLang.KO, Expect.PUNCT).isEmpty())
    }

    @Test
    fun `outputOf looks up ids across all sets`() {
        assertEquals(TemplateOutput.Jamo(Jamo.G), set.outputOf("ko_g"))
        assertEquals(TemplateOutput.Text("1"), set.outputOf("num_1"))
        assertEquals(TemplateOutput.Text("@"), set.outputOf("x_at"))
        val control = TemplateSet(mapOf(SetName.KO_LETTER to listOf(template("ko_finish_j", TemplateOutput.Control("FINISH_J")))))
        assertEquals(TemplateOutput.Control("FINISH_J"), control.outputOf("ko_finish_j"))
        assertNull(set.outputOf("nope"))
        assertNull(TemplateSet(emptyMap()).outputOf("ko_g"))
    }
}
