package kbd.graffiti.state

import kbd.hangul.cheonjiin.CjElement
import kbd.graffiti.ink.GraffitiLang
import kbd.graffiti.ink.GraffitiLang.EN
import kbd.graffiti.ink.GraffitiLang.KO
import kbd.graffiti.ink.PadArea
import kbd.graffiti.ink.PadArea.LETTER
import kbd.graffiti.ink.PadArea.NUMBER
import kbd.graffiti.recognize.SetName
import kbd.graffiti.recognize.StrokeToken
import kbd.graffiti.recognize.StrokeToken.CornerTap
import kbd.graffiti.recognize.StrokeToken.DiagDownLeft
import kbd.graffiti.recognize.StrokeToken.DiagDownRight
import kbd.graffiti.recognize.StrokeToken.LineDown
import kbd.graffiti.recognize.StrokeToken.LineLeft
import kbd.graffiti.recognize.StrokeToken.LineRight
import kbd.graffiti.recognize.StrokeToken.LineUp
import kbd.graffiti.recognize.StrokeToken.Symbol
import kbd.graffiti.recognize.StrokeToken.Tap
import kbd.graffiti.recognize.StrokeToken.Unknown
import kbd.graffiti.recognize.Template
import kbd.graffiti.recognize.TemplateOutput
import kbd.graffiti.recognize.TemplateSet
import kbd.hangul.jamo.Jamo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** GraffitiState (LLD-05 §5, LLD-11 §6.3 GS-01..GS-38; GS-34 and GS-39..GS-45 are in GraffitiStrokeTest). */
class GraffitiStateTest {

    private fun template(id: String, output: TemplateOutput) =
        Template(id, output, corners = 0, strokes = listOf(listOf(0f to 0f, 1f to 1f)))

    private val templates = TemplateSet(
        mapOf(
            SetName.KO_LETTER to listOf(
                template("ko_g", TemplateOutput.Jamo(Jamo.G)),
                template("ko_k", TemplateOutput.Jamo(Jamo.K)),
                template("ko_gg", TemplateOutput.Jamo(Jamo.GG)),
                template("ko_space_v", TemplateOutput.Control("SPACE_V")),
                template("ko_finish_j", TemplateOutput.Control("FINISH_J")),
            ),
            SetName.EN_LETTER to listOf(template("en_a", TemplateOutput.Text("a"))),
            SetName.NUMBER to listOf(template("num_7", TemplateOutput.Text("7"))),
            SetName.PUNCT to listOf(template("p_dot", TemplateOutput.Text("."))),
            SetName.EXTENDED to listOf(template("x_at", TemplateOutput.Text("@"))),
        ),
    )

    private enum class Pending { SHIFT, EXT, PUNCT }

    private fun state(
        lang: GraffitiLang = KO,
        capsLock: Boolean = false,
        pending: Pending? = null,
    ) = GraffitiState(templates).apply {
        this.lang = lang
        this.capsLock = capsLock
        shiftPending = pending == Pending.SHIFT
        extPending = pending == Pending.EXT
        punctPending = pending == Pending.PUNCT
    }

    private fun GraffitiState.flags() = listOf(shiftPending, extPending, punctPending)

    private val none = listOf(false, false, false)

    private fun assertOneShotOff(s: GraffitiState, msg: String = "") = assertEquals(msg, none, s.flags())

    private fun text(t: String) = GraffitiAction.CommitText(t)

    @Test
    fun `GS-01 ext pending then Tap toggles lang and clears one-shot`() {
        for (lang in GraffitiLang.entries) for (area in PadArea.entries) {
            val s = state(lang, pending = Pending.EXT)
            assertEquals("$lang $area", GraffitiAction.ToggleLang, s.interpret(Tap, area))
            assertOneShotOff(s, "$lang $area")
            assertEquals("lang is switched by the handler, not here", lang, s.lang)
        }
    }

    @Test
    fun `GS-02 ext pending then extended symbol commits it`() {
        val s = state(EN, pending = Pending.EXT)
        assertEquals(text("@"), s.interpret(Symbol("x_at"), LETTER))
        assertOneShotOff(s)
    }

    @Test
    fun `GS-03 punct pending then punctuation symbol commits it`() {
        val s = state(KO, pending = Pending.PUNCT)
        assertEquals(text("."), s.interpret(Symbol("p_dot"), NUMBER))
        assertOneShotOff(s)
    }

    @Test
    fun `GS-04 LineLeft while shift pending is Backspace and keeps shift`() {
        val s = state(EN, pending = Pending.SHIFT)
        assertEquals(GraffitiAction.Backspace, s.interpret(LineLeft, LETTER))
        assertTrue(s.shiftPending)
    }

    @Test
    fun `GS-05 DiagDownLeft is Enter`() {
        val s = state(KO)
        assertEquals(GraffitiAction.Enter, s.interpret(DiagDownLeft, LETTER))
        assertOneShotOff(s)
    }

    @Test
    fun `GS-06 DiagDownRight sets ext`() {
        val s = state(EN)
        assertEquals(GraffitiAction.StateOnly, s.interpret(DiagDownRight, LETTER))
        assertEquals(listOf(false, true, false), s.flags())
    }

    @Test
    fun `GS-07 LineUp sets shift in EN and KO`() {
        for (lang in GraffitiLang.entries) {
            val s = state(lang)
            assertEquals("$lang", GraffitiAction.StateOnly, s.interpret(LineUp, LETTER))
            assertEquals("$lang", listOf(true, false, false), s.flags())
            assertFalse("$lang", s.capsLock)
        }
    }

    @Test
    fun `GS-08 EN LineUp twice is caps lock`() {
        val s = state(EN)
        s.interpret(LineUp, LETTER)
        assertEquals(GraffitiAction.StateOnly, s.interpret(LineUp, LETTER))
        assertTrue(s.capsLock)
        assertFalse(s.shiftPending)
    }

    @Test
    fun `GS-09 CornerTap toggles lang`() {
        for (lang in GraffitiLang.entries) {
            val s = state(lang)
            assertEquals("$lang", GraffitiAction.ToggleLang, s.interpret(CornerTap, LETTER))
            assertOneShotOff(s, "$lang")
        }
    }

    @Test
    fun `GS-10 Tap in EN either area or KO number area sets punct`() {
        for ((lang, area) in listOf(EN to LETTER, EN to NUMBER, KO to NUMBER)) {
            val s = state(lang)
            assertEquals("$lang $area", GraffitiAction.StateOnly, s.interpret(Tap, area))
            assertEquals("$lang $area", listOf(false, false, true), s.flags())
        }
    }

    @Test
    fun `GS-11 KO Tap in letter area is DOT`() {
        val s = state(KO)
        assertEquals(GraffitiAction.Element(CjElement.DOT), s.interpret(Tap, LETTER))
        assertOneShotOff(s)
    }

    @Test
    fun `GS-12 KO LineRight in letter area is EU`() {
        assertEquals(GraffitiAction.Element(CjElement.EU), state(KO).interpret(LineRight, LETTER))
    }

    @Test
    fun `GS-13 EN LineRight and KO number-area LineRight are Space`() {
        assertEquals(GraffitiAction.Space, state(EN).interpret(LineRight, LETTER))
        assertEquals(GraffitiAction.Space, state(EN).interpret(LineRight, NUMBER))
        assertEquals(GraffitiAction.Space, state(KO).interpret(LineRight, NUMBER))
    }

    @Test
    fun `GS-14 KO LineDown in letter area is I`() {
        assertEquals(GraffitiAction.Element(CjElement.I), state(KO).interpret(LineDown, LETTER))
    }

    @Test
    fun `GS-15 LineDown in number area is 1`() {
        for (lang in GraffitiLang.entries) {
            assertEquals("$lang", text("1"), state(lang).interpret(LineDown, NUMBER))
        }
    }

    @Test
    fun `GS-16 EN LineDown in letter area is i, I after shift, I with caps lock`() {
        assertEquals(text("i"), state(EN).interpret(LineDown, LETTER))
        val shifted = state(EN, pending = Pending.SHIFT)
        assertEquals(text("I"), shifted.interpret(LineDown, LETTER))
        assertOneShotOff(shifted)
        val caps = state(EN, capsLock = true)
        assertEquals(text("I"), caps.interpret(LineDown, LETTER))
        assertTrue("caps lock stays", caps.capsLock)
        assertEquals(text("I"), caps.interpret(LineDown, LETTER))
    }

    @Test
    fun `GS-17 shift then ko_g is double consonant`() {
        val s = state(KO, pending = Pending.SHIFT)
        assertEquals(GraffitiAction.Consonant(Jamo.GG), s.interpret(Symbol("ko_g"), LETTER))
        assertOneShotOff(s)
    }

    @Test
    fun `GS-18 one-stroke double consonant`() {
        assertEquals(GraffitiAction.Consonant(Jamo.GG), state(KO).interpret(Symbol("ko_gg"), LETTER))
        val shifted = state(KO, pending = Pending.SHIFT)
        assertEquals(GraffitiAction.Consonant(Jamo.GG), shifted.interpret(Symbol("ko_gg"), LETTER))
        assertOneShotOff(shifted)
    }

    @Test
    fun `GS-19 KO SPACE_V is Space`() {
        assertEquals(GraffitiAction.Space, state(KO).interpret(Symbol("ko_space_v"), LETTER))
    }

    @Test
    fun `GS-20 KO FINISH_J is Finish`() {
        assertEquals(GraffitiAction.Finish, state(KO).interpret(Symbol("ko_finish_j"), LETTER))
    }

    @Test
    fun `GS-21 EN letter is a, A after shift, A with caps lock`() {
        assertEquals(text("a"), state(EN).interpret(Symbol("en_a"), LETTER))
        val shifted = state(EN, pending = Pending.SHIFT)
        assertEquals(text("A"), shifted.interpret(Symbol("en_a"), LETTER))
        assertOneShotOff(shifted)
        assertEquals("shift is one-shot", text("a"), shifted.interpret(Symbol("en_a"), LETTER))
        val caps = state(EN, capsLock = true)
        assertEquals(text("A"), caps.interpret(Symbol("en_a"), LETTER))
        assertEquals(text("A"), caps.interpret(Symbol("en_a"), LETTER))
    }

    @Test
    fun `GS-22 Unknown while shift pending fails and keeps shift`() {
        val s = state(EN, capsLock = false, pending = Pending.SHIFT)
        val before = s.snapshot()
        assertEquals(GraffitiAction.RecognitionFailed, s.interpret(Unknown, LETTER))
        assertEquals(before, s.snapshot())
    }

    @Test
    fun `GS-23 cancelPending clears any pending and reports it`() {
        for (p in Pending.entries) {
            val s = state(EN, capsLock = true, pending = p)
            assertTrue("$p", s.cancelPending())
            assertOneShotOff(s, "$p")
            assertTrue("$p caps lock is not a pending", s.capsLock)
        }
        val idle = state(EN, capsLock = true)
        assertFalse(idle.cancelPending())
        assertOneShotOff(idle)
    }

    @Test
    fun `GS-24 enterMode resets caps lock, lang and one-shot`() {
        for (p in Pending.entries) {
            val s = state(EN, capsLock = true, pending = p)
            s.enterMode()
            assertFalse("$p", s.capsLock)
            assertEquals("$p", KO, s.lang)
            assertOneShotOff(s, "$p")
        }
    }

    @Test
    fun `GS-25 EN caps lock then LineUp turns caps lock off without shift`() {
        val s = state(EN, capsLock = true)
        assertEquals(GraffitiAction.StateOnly, s.interpret(LineUp, LETTER))
        assertFalse(s.capsLock)
        assertFalse(s.shiftPending)
        assertEquals(text("a"), s.interpret(Symbol("en_a"), LETTER))
    }

    @Test
    fun `GS-26 KO LineUp twice turns shift off and no caps lock`() {
        val s = state(KO)
        s.interpret(LineUp, LETTER)
        assertEquals(GraffitiAction.StateOnly, s.interpret(LineUp, LETTER))
        assertFalse(s.shiftPending)
        assertFalse(s.capsLock)
        assertEquals(GraffitiAction.Consonant(Jamo.G), s.interpret(Symbol("ko_g"), LETTER))
    }

    @Test
    fun `GS-27 KO shift then ko_k stays K`() {
        val s = state(KO, pending = Pending.SHIFT)
        assertEquals(GraffitiAction.Consonant(Jamo.K), s.interpret(Symbol("ko_k"), LETTER))
        assertOneShotOff(s)
    }

    @Test
    fun `GS-28 KO shift then vowel element strokes ignore shift`() {
        val cases = listOf(
            Tap to GraffitiAction.Element(CjElement.DOT),
            LineDown to GraffitiAction.Element(CjElement.I),
            LineRight to GraffitiAction.Element(CjElement.EU),
        )
        for ((t, expected) in cases) {
            val s = state(KO, pending = Pending.SHIFT)
            assertEquals("$t", expected, s.interpret(t, LETTER))
            assertOneShotOff(s, "$t")
        }
    }

    @Test
    fun `GS-29 shift then number-area strokes ignore shift`() {
        for (lang in GraffitiLang.entries) {
            val a = state(lang, pending = Pending.SHIFT)
            assertEquals("$lang", text("1"), a.interpret(LineDown, NUMBER))
            assertOneShotOff(a, "$lang")
            val b = state(lang, pending = Pending.SHIFT)
            assertEquals("$lang", text("7"), b.interpret(Symbol("num_7"), NUMBER))
            assertOneShotOff(b, "$lang")
        }
    }

    @Test
    fun `GS-30 shift pending then EN LineRight is Space and clears shift`() {
        // Ext / punct pending + → is a symbol (GS-40, GraffitiStrokeTest).
        val s = state(EN, pending = Pending.SHIFT)
        assertEquals(GraffitiAction.Space, s.interpret(LineRight, LETTER))
        assertOneShotOff(s)
    }

    @Test
    fun `GS-31 shift pending then DiagDownLeft is Enter and clears shift`() {
        // Ext / punct pending + ↙ is a symbol (GS-39, GraffitiStrokeTest).
        for (lang in GraffitiLang.entries) {
            val s = state(lang, pending = Pending.SHIFT)
            assertEquals("$lang", GraffitiAction.Enter, s.interpret(DiagDownLeft, LETTER))
            assertOneShotOff(s, "$lang")
        }
    }

    @Test
    fun `GS-32 KO shift then SPACE_V or FINISH_J clears shift`() {
        val a = state(KO, pending = Pending.SHIFT)
        assertEquals(GraffitiAction.Space, a.interpret(Symbol("ko_space_v"), LETTER))
        assertOneShotOff(a)
        val b = state(KO, pending = Pending.SHIFT)
        assertEquals(GraffitiAction.Finish, b.interpret(Symbol("ko_finish_j"), LETTER))
        assertOneShotOff(b)
    }

    @Test
    fun `GS-33 shift pending then DiagDownRight swaps to ext`() {
        val s = state(EN, pending = Pending.SHIFT)
        assertEquals(GraffitiAction.StateOnly, s.interpret(DiagDownRight, LETTER))
        assertEquals(listOf(false, true, false), s.flags())
    }

    // GS-34 (ext / punct pending + ↑ = ¡ / !) is stroke level since LLD 0.7: GraffitiStrokeTest.

    @Test
    fun `GS-35 shift pending then CornerTap toggles and clears shift`() {
        for (lang in GraffitiLang.entries) {
            val s = state(lang, pending = Pending.SHIFT)
            assertEquals("$lang", GraffitiAction.ToggleLang, s.interpret(CornerTap, LETTER))
            assertOneShotOff(s, "$lang")
        }
    }

    @Test
    fun `GS-36 EN shift pending then Tap swaps to punct`() {
        val s = state(EN, pending = Pending.SHIFT)
        assertEquals(GraffitiAction.StateOnly, s.interpret(Tap, LETTER))
        assertEquals(listOf(false, false, true), s.flags())
    }

    @Test
    fun `GS-37 pending ext or punct then a stroke outside its table inputs normally`() {
        val a = state(EN, pending = Pending.PUNCT)
        assertEquals(text("a"), a.interpret(Symbol("en_a"), LETTER))
        assertOneShotOff(a)
        val b = state(KO, pending = Pending.PUNCT)
        assertEquals(GraffitiAction.Consonant(Jamo.G), b.interpret(Symbol("ko_g"), LETTER))
        assertOneShotOff(b)
        val c = state(KO, pending = Pending.EXT)
        assertEquals(text("7"), c.interpret(Symbol("num_7"), NUMBER))
        assertOneShotOff(c)
    }

    @Test
    fun `GS-38 KO ext pending then the down line inputs the vowel element normally`() {
        // The extended table has no ↓; punct pending + ↓ / → is ' / - (GS-40, GS-42, OD-5 A).
        val a = state(KO, pending = Pending.EXT)
        assertEquals(GraffitiAction.Element(CjElement.I), a.interpret(LineDown, LETTER))
        assertOneShotOff(a)
    }

    // ---- beyond LLD-11 IDs ----

    @Test
    fun `unknown symbol id is a recognition failure and changes nothing`() {
        val s = state(EN, pending = Pending.EXT)
        val before = s.snapshot()
        assertEquals(GraffitiAction.RecognitionFailed, s.interpret(Symbol("nope"), LETTER))
        assertEquals(before, s.snapshot())
    }

    @Test
    fun `ext pending then punctuation-set symbol is not row 2 but still commits`() {
        val s = state(EN, pending = Pending.EXT)
        assertEquals(text("."), s.interpret(Symbol("p_dot"), LETTER))
        assertOneShotOff(s)
    }

    @Test
    fun `new state starts in KO with nothing pending`() {
        val s = GraffitiState(templates)
        assertEquals(KO, s.lang)
        assertFalse(s.capsLock)
        assertOneShotOff(s)
    }

    @Test
    fun `clearOneShot keeps lang and caps lock`() {
        val s = state(EN, capsLock = true, pending = Pending.PUNCT)
        s.clearOneShot()
        assertOneShotOff(s)
        assertEquals(EN, s.lang)
        assertTrue(s.capsLock)
    }

    @Test
    fun `snapshot and restore round-trip a value copy`() {
        val s = state(EN, capsLock = true, pending = Pending.SHIFT)
        val snap = s.snapshot()
        s.enterMode()
        s.interpret(DiagDownRight, LETTER)
        s.restore(snap)
        assertEquals(snap, s.snapshot())
        assertEquals(EN, s.lang)
        assertTrue(s.capsLock)
        assertEquals(listOf(true, false, false), s.flags())
    }
}
