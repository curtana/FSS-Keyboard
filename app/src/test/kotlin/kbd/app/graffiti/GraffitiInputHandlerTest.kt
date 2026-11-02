package kbd.app.graffiti

import kbd.app.ime.ComposingController
import kbd.app.ime.EditorKey
import kbd.app.ime.EditorPort
import kbd.app.ime.FieldClass
import kbd.app.ime.FieldInfo
import kbd.hangul.cheonjiin.CheonjiinAssembler
import kbd.hangul.cheonjiin.CjElement
import kbd.hangul.cheonjiin.CjState
import kbd.graffiti.ink.GraffitiLang.EN
import kbd.graffiti.ink.GraffitiLang.KO
import kbd.graffiti.ink.InkPoint
import kbd.graffiti.ink.InkStroke
import kbd.graffiti.ink.PadArea
import kbd.graffiti.ink.PadArea.LETTER
import kbd.graffiti.ink.PadArea.NUMBER
import kbd.graffiti.ink.RectF2
import kbd.graffiti.recognize.Recognizer
import kbd.graffiti.recognize.SetName
import kbd.graffiti.recognize.StrokeToken
import kbd.graffiti.recognize.StrokeToken.CornerTap
import kbd.graffiti.recognize.StrokeToken.DiagDownLeft
import kbd.graffiti.recognize.StrokeToken.LineDown
import kbd.graffiti.recognize.StrokeToken.LineRight
import kbd.graffiti.recognize.StrokeToken.LineUp
import kbd.graffiti.recognize.StrokeToken.Symbol
import kbd.graffiti.recognize.StrokeToken.Tap
import kbd.graffiti.recognize.StrokeToken.Unknown
import kbd.graffiti.recognize.Template
import kbd.graffiti.recognize.TemplateOutput
import kbd.graffiti.recognize.TemplateSet
import kbd.graffiti.state.GraffitiState
import kbd.hangul.compose.ComposePolicy
import kbd.hangul.compose.HangulComposer
import kbd.hangul.jamo.Jamo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GraffitiInputHandler (LLD-05 §6; LLD-11 §6.4 GH, §2.1 BS graffiti rows). Strokes go in as [StrokeToken]s through
 * the real [GraffitiState] (LLD-11 §1.3), output through a real [ComposingController] into a fake editor that keeps
 * committed and composing text. GH-20 and GH-32 need InputDispatcher (T31).
 */
class GraffitiInputHandlerTest {

    /** Editor text model: [committed] plus the composing span at its end (the cursor never moves here). */
    private class FakeEditor : EditorPort {
        var committed = ""
        var composing = ""
        val calls = mutableListOf<String>()
        val text get() = committed + composing

        override fun commitText(text: String) { calls += "commit($text)"; committed += text; composing = "" }
        override fun setComposingText(text: String) { calls += "compose($text)"; composing = text }
        override fun finishComposingText() { calls += "finish"; committed += composing; composing = "" }
        override fun deleteBefore() { calls += "del"; committed = committed.dropLast(1) }
        override fun sendKey(key: EditorKey) { calls += "key($key)" }
        override fun performEditorAction(actionId: Int) { calls += "action($actionId)" }
        override fun setSelection(start: Int, end: Int) { calls += "sel($start,$end)" }
        override fun beginBatch() {}
        override fun endBatch() {}
    }

    private class FakeSink : InputSink {
        var marks = 0
        var errors = 0
        override fun markInput() { marks++ }
        override fun error() { errors++ }
        override var hidesDisplay = false
        override val field = FieldInfo(FieldClass.TEXT, isPassword = false, noLearning = false, multiLine = false, enterAction = null)
    }

    private fun template(id: String, output: TemplateOutput, verts: List<Pair<Float, Float>> = G_SHAPE) =
        Template(id, output, corners = 1, strokes = listOf(verts))

    private val templates = TemplateSet(
        mapOf(
            SetName.KO_LETTER to listOf(
                template("ko_g", TemplateOutput.Jamo(Jamo.G)),
                template("ko_n", TemplateOutput.Jamo(Jamo.N), listOf(0f to 0f, 0.05f to 1f, 1f to 0.95f)),
                template("ko_ng", TemplateOutput.Jamo(Jamo.NG), listOf(0.5f to 0f, 0f to 0.5f, 0.5f to 1f, 1f to 0.5f, 0.5f to 0f)),
                template("ko_gg", TemplateOutput.Jamo(Jamo.GG), listOf(0f to 0f, 0.45f to 0f, 0.5f to 1f, 0.55f to 0f, 1f to 0f, 1f to 1f)),
                template("ko_space_v", TemplateOutput.Control("SPACE_V"), listOf(0f to 0f, 0.5f to 1f, 1f to 0f)),
                template("ko_finish_j", TemplateOutput.Control("FINISH_J"), listOf(1f to 0f, 1f to 1f, 0f to 0.8f)),
            ),
            SetName.EN_LETTER to listOf(template("en_a", TemplateOutput.Text("a"), listOf(0f to 1f, 0.5f to 0f, 1f to 1f))),
            SetName.NUMBER to listOf(
                template("num_3", TemplateOutput.Text("3"), listOf(0f to 0f, 1f to 0.25f, 0.3f to 0.5f, 1f to 0.75f, 0f to 1f)),
            ),
            SetName.EXTENDED to listOf(template("x_at", TemplateOutput.Text("@"), listOf(1f to 0f, 0f to 0.5f, 1f to 1f))),
        ),
    )

    private val editor = FakeEditor()
    private val sink = FakeSink()
    private val state = GraffitiState(templates)
    private val asm = CheonjiinAssembler()
    private val composer = HangulComposer(ComposePolicy.GRAFFITI)
    private val flashes = mutableListOf<String>()
    private val h = GraffitiInputHandler(
        Recognizer(templates), state, asm, { composer }, ComposingController(editor), sink, { flashes += it },
    )

    // Token names for the tables (KO letter area unless noted).
    private val G = Symbol("ko_g")
    private val N = Symbol("ko_n")
    private val NG = Symbol("ko_ng")
    private val GG = Symbol("ko_gg")
    private val I = LineDown
    private val EU = LineRight
    private val V = Symbol("ko_space_v")
    private val J = Symbol("ko_finish_j")
    private val NUM3 = Symbol("num_3")

    private fun input(vararg tokens: StrokeToken, area: PadArea = LETTER) {
        for (t in tokens) h.handle(state.interpret(t, area))
    }

    private fun bs(n: Int = 1) = repeat(n) { h.backspace() }

    // ---- FR-HGL-04d / 04e sequences ----

    @Test
    fun `GH-01 g i tap gives ga`() {
        input(G, I, Tap)
        assertEquals("가", editor.text)
    }

    @Test
    fun `GH-02 g tap shows g with the dot glyph, composer holds g only`() {
        input(G, Tap)
        assertEquals("ㄱㆍ", editor.text)
        assertEquals("ㄱ", composer.composingText())
    }

    @Test
    fun `GH-03 g tap tap shows the double dot glyph`() {
        input(G, Tap, Tap)
        assertEquals("ㄱ‥", editor.text)
    }

    @Test
    fun `GH-04 gak tap i gives ga geo through gak with glyph`() {
        input(G, I, Tap, G, Tap)
        assertEquals("각ㆍ", editor.text)
        input(I)
        assertEquals("가거", editor.text)
        assertEquals("가", editor.committed)
    }

    @Test
    fun `GH-05 g eu tap tap i gives gwo`() {
        input(G, EU, Tap, Tap, I)
        assertEquals("궈", editor.text)
    }

    @Test
    fun `GH-06 g tap n drops the glyph`() {
        input(G, Tap, N)
        assertEquals("ㄱㄴ", editor.text)
    }

    @Test
    fun `GH-07 g tap V space drops the glyph`() {
        input(G, Tap, V)
        assertEquals("ㄱ ", editor.text)
        assertEquals("", editor.composing)
    }

    @Test
    fun `GH-08 third tap after double dot is ignored`() {
        input(G, Tap, Tap, Tap)
        assertEquals("ㄱ‥", editor.text)
        assertEquals(listOf(CjElement.DOT, CjElement.DOT), asm.elements)
    }

    @Test
    fun `GH-09 tap then i with nothing composing gives eo alone`() {
        input(Tap)
        assertEquals("ㆍ", editor.text)
        assertEquals("", composer.composingText())
        input(I)
        assertEquals("ㅓ", editor.text)
    }

    @Test
    fun `GH-10 g tap enter commits g and drops the glyph`() {
        input(G, Tap, DiagDownLeft)
        assertEquals("ㄱ", editor.committed)
        assertEquals("", editor.composing)
        assertEquals("key(ENTER)", editor.calls.last())
        assertFalse(editor.calls.any { it == "commit(ㄱㆍ)" })
    }

    @Test
    fun `GH-11 g tap J commits g and drops the glyph`() {
        input(G, Tap, J)
        assertEquals("ㄱ", editor.committed)
        assertEquals("", editor.composing)
        assertEquals(CjState.Empty, asm.state)
    }

    @Test
    fun `GH-12 g tap mode key finishAll commits g and drops the glyph`() {
        input(G, Tap)
        h.finishAll()   // what InputDispatcher.onModeKey calls (LLD-08 §6)
        assertEquals("ㄱ", editor.committed)
        assertEquals("", editor.composing)
        assertEquals(CjState.Empty, asm.state)
        assertFalse(composer.isComposing())
    }

    @Test
    fun `GH-13 ga then number area 3 commits ga first`() {
        input(G, I, Tap)
        input(NUM3, area = NUMBER)
        assertEquals("가3", editor.committed)
        assertEquals("", editor.composing)
    }

    @Test
    fun `GH-14 ga then an English letter commits ga first`() {
        input(G, I, Tap)
        state.lang = EN
        input(Symbol("en_a"))
        assertEquals("가a", editor.committed)
        assertEquals("", editor.composing)
    }

    @Test
    fun `GH-15 g i g gives gik and closes the assembler`() {
        input(G, I, G)
        assertEquals("긱", editor.text)
        assertEquals(CjState.Empty, asm.state)
    }

    @Test
    fun `GH-16 ng tap eu i tap gives wa`() {
        input(NG, Tap, EU, I, Tap)
        assertEquals("와", editor.text)
    }

    @Test
    fun `GH-17 g tap tap eu gives gyo`() {
        input(G, Tap, Tap, EU)
        assertEquals("교", editor.text)
    }

    @Test
    fun `GH-18 shift g i tap gives kka`() {
        input(LineUp, G, I, Tap)
        assertEquals("까", editor.text)
    }

    @Test
    fun `GH-19 ga then g g gives gak then initial g, not a double`() {
        input(G, I, Tap, G, G)
        assertEquals("각ㄱ", editor.text)
        assertEquals("각", editor.committed)
    }

    @Test
    fun `GH-21 entering graffiti mode again resets to KO and caps off`() {
        state.lang = EN
        state.capsLock = true
        state.shiftPending = true
        h.enterMode()
        assertEquals(KO, state.lang)
        assertFalse(state.capsLock)
        assertFalse(state.shiftPending)
    }

    @Test
    fun `GH-22 reset keeps EN and caps lock, clears one-shots and the assembler`() {
        for (reason in ResetReason.entries) {
            state.lang = KO
            input(G, Tap)   // assembler Pending
            state.lang = EN
            state.capsLock = true
            state.shiftPending = true
            state.extPending = true
            state.punctPending = true
            h.reset(reason)
            assertEquals(reason.name, EN, state.lang)
            assertTrue(reason.name, state.capsLock)
            assertFalse(reason.name, state.shiftPending || state.extPending || state.punctPending)
            assertEquals(reason.name, CjState.Empty, asm.state)
        }
    }

    @Test
    fun `GH-23 ga backspace repeated twice gives gi then g`() {
        input(G, I, Tap)
        bs()
        assertEquals("기", editor.text)
        bs()
        assertEquals("ㄱ", editor.text)
    }

    @Test
    fun `GH-24 ga geo backspace gives ga g with glyph, not crossing the commit`() {
        input(G, I, Tap, G, Tap, I)
        bs()
        assertEquals("가ㄱㆍ", editor.text)
        assertEquals("가", editor.committed)
    }

    @Test
    fun `GH-26 g tap number area 3 drops the glyph and commits first`() {
        input(G, Tap)
        input(NUM3, area = NUMBER)
        assertEquals("ㄱ3", editor.committed)
        assertEquals("", editor.composing)
    }

    @Test
    fun `GH-27 g i i gives gi, the second i ignored but typed`() {
        input(G, I, I)
        assertEquals("기", editor.text)
        assertEquals(3, sink.marks)
    }

    @Test
    fun `GH-28 g eu eu gives geu`() {
        input(G, EU, EU)
        assertEquals("그", editor.text)
    }

    @Test
    fun `GH-29 g i tap i tap gives gae, the last tap ignored`() {
        input(G, I, Tap, I, Tap)
        assertEquals("개", editor.text)
    }

    @Test
    fun `GH-30 g tap corner tap commits g, drops the glyph, switches to EN, not typed`() {
        input(G, Tap)
        input(CornerTap)
        assertEquals("ㄱ", editor.committed)
        assertEquals("", editor.composing)
        assertEquals(EN, state.lang)
        assertEquals(2, sink.marks)
    }

    @Test
    fun `GH-31 ga corner tap a gives ga a`() {
        input(G, I, Tap, CornerTap, Symbol("en_a"))
        assertEquals("가a", editor.committed)
        assertEquals("", editor.composing)
    }

    @Test
    fun `GH-33 unknown stroke while composing ga changes nothing`() {
        input(G, I, Tap)
        val marks = sink.marks
        val calls = editor.calls.size
        h.onStroke(gStroke(NUMBER))   // matches no NUMBER template: Unknown
        assertEquals("가", editor.text)
        assertEquals(listOf(CjElement.I, CjElement.DOT), asm.elements)
        assertEquals(1, sink.errors)
        assertEquals(marks, sink.marks)
        assertEquals(calls, editor.calls.size)
        assertTrue(flashes.isEmpty())
    }

    @Test
    fun `GH-33 Unknown token is a recognition failure with an error`() {
        input(G, I, Tap)
        input(Unknown)
        assertEquals("가", editor.text)
        assertEquals(1, sink.errors)
        assertEquals(3, sink.marks)
    }

    @Test
    fun `GH-34 J stroke is not typed, g and tap are`() {
        input(G)
        assertEquals(1, sink.marks)
        input(Tap)
        assertEquals(2, sink.marks)
        input(J)
        assertEquals(2, sink.marks)
    }

    @Test
    fun `GH-35 a password field shows no flash`() {
        sink.hidesDisplay = true
        h.onStroke(gStroke(LETTER))
        assertEquals("ㄱ", editor.text)
        assertTrue(flashes.isEmpty())
    }

    @Test
    fun `GH-35 normal field flashes the recognized glyph once`() {
        h.onStroke(gStroke(LETTER))
        assertEquals("ㄱ", editor.text)
        assertEquals(listOf("ㄱ"), flashes)
        assertEquals(1, sink.marks)
    }

    // ---- Owner decisions #74 A and #79 OD-3 A (issue #101) ----

    @Test
    fun `GH-36 an element that cannot follow is not flashed - g i tap i tap gives gae, the last tap typed but not shown`() {
        for (s in listOf(gStroke(LETTER), downLine(), tapStroke(), downLine(), tapStroke())) h.onStroke(s)
        assertEquals("개", editor.text)
        assertEquals(listOf("ㄱ", "ㅣ", "ㆍ", "ㅣ"), flashes)
        assertEquals(5, sink.marks)   // the ignored tap is still typed (FR-CMN-03a)
    }

    @Test
    fun `GH-36 an element that cannot follow is not flashed - g i i, the second i not shown`() {
        for (s in listOf(gStroke(LETTER), downLine(), downLine())) h.onStroke(s)
        assertEquals("기", editor.text)
        assertEquals(listOf("ㄱ", "ㅣ"), flashes)
        assertEquals(3, sink.marks)
    }

    @Test
    fun `GH-36 the third tap after the double dot is not flashed`() {
        for (s in listOf(gStroke(LETTER), tapStroke(), tapStroke(), tapStroke())) h.onStroke(s)
        assertEquals("ㄱ‥", editor.text)
        assertEquals(listOf("ㄱ", "ㆍ", "ㆍ"), flashes)
    }

    @Test
    fun `GH-36 strokes with no glyph flash nothing`() {
        // Shift, punctuation-shift tap and extended shift, each cancelled by backspace: FR-GRF-07 shows nothing.
        for (s in listOf(upLine(), leftLine(), tapStroke(NUMBER), leftLine(), diagDownRight(), leftLine())) h.onStroke(s)
        assertEquals("", editor.text)
        assertTrue(flashes.isEmpty())
    }

    @Test
    fun `GH-37 backspace stroke while punctuation shift is pending only cancels it`() {
        editor.committed = "한"
        h.onStroke(tapStroke(NUMBER))
        assertTrue(state.punctPending)
        val calls = editor.calls.size
        h.onStroke(leftLine())   // recognized as LineLeft even though PUNCT is expected: ← is in no symbol table
        assertFalse(state.punctPending)
        assertEquals("한", editor.text)
        assertEquals(calls, editor.calls.size)
    }

    @Test
    fun `GH-37 backspace stroke while extended shift is pending only cancels it, the composing syllable stays`() {
        for (s in listOf(gStroke(LETTER), downLine(), tapStroke(), diagDownRight())) h.onStroke(s)
        assertTrue(state.extPending)
        h.onStroke(leftLine())
        assertFalse(state.extPending)
        assertEquals("가", editor.text)
        assertEquals(listOf(CjElement.I, CjElement.DOT), asm.elements)
    }

    // ---- Backspace cases (LLD-11 §2.1, graffiti rows) ----

    @Test
    fun `BS-06 ga backspace gives gi`() {
        input(G, I, Tap)
        bs()
        assertEquals("기", editor.text)
    }

    @Test
    fun `BS-07 ga backspace backspace gives gi then g`() {
        input(G, I, Tap)
        bs()
        assertEquals("기", editor.text)
        bs()
        assertEquals("ㄱ", editor.text)
        assertEquals(CjState.Empty, asm.state)
    }

    @Test
    fun `BS-08 ga backspace then tap gives ga again`() {
        input(G, I, Tap)
        bs()
        input(Tap)
        assertEquals("가", editor.text)
    }

    @Test
    fun `BS-09 g tap backspace gives g`() {
        input(G, Tap)
        bs()
        assertEquals("ㄱ", editor.text)
    }

    @Test
    fun `BS-10 g tap tap backspace gives g with dot`() {
        input(G, Tap, Tap)
        bs()
        assertEquals("ㄱㆍ", editor.text)
    }

    @Test
    fun `BS-11 geo backspace backspace gives g dot then g`() {
        input(G, Tap, I)
        assertEquals("거", editor.text)
        bs()
        assertEquals("ㄱㆍ", editor.text)
        bs()
        assertEquals("ㄱ", editor.text)
    }

    @Test
    fun `BS-12 gyeo backspace gives g double dot`() {
        input(G, Tap, Tap, I)
        assertEquals("겨", editor.text)
        bs()
        assertEquals("ㄱ‥", editor.text)
    }

    @Test
    fun `BS-13 wa backspace backspace gives oe then o`() {
        input(NG, Tap, EU, I, Tap)
        bs()
        assertEquals("외", editor.text)
        bs()
        assertEquals("오", editor.text)
    }

    @Test
    fun `BS-14 gak tap backspace gives gak`() {
        input(G, I, Tap, G, Tap)
        bs()
        assertEquals("각", editor.text)
    }

    @Test
    fun `BS-15 ga ga backspace gives ga gi`() {
        input(G, I, Tap, G, I, Tap)
        assertEquals("가가", editor.text)
        bs()
        assertEquals("가기", editor.text)
    }

    @Test
    fun `BS-16 gak backspace gives ga`() {
        input(G, I, Tap, G)
        bs()
        assertEquals("가", editor.text)
    }

    @Test
    fun `BS-17 gak backspace then tap gives ga with dot, the closed vowel stays closed`() {
        input(G, I, Tap, G)
        bs()
        input(Tap)
        assertEquals("가ㆍ", editor.text)
    }

    @Test
    fun `BS-18 kk by shift plus g backspace gives empty`() {
        input(LineUp, G)
        assertEquals("ㄲ", editor.text)
        bs()
        assertEquals("", editor.text)
    }

    @Test
    fun `BS-18 kk in one stroke backspace gives empty`() {
        input(GG)
        assertEquals("ㄲ", editor.text)
        bs()
        assertEquals("", editor.text)
    }

    @Test
    fun `BS-19 ga plus shift backspace cancels the shift only`() {
        input(G, I, Tap, LineUp)
        assertTrue(state.shiftPending)
        bs()
        assertEquals("가", editor.text)
        assertFalse(state.shiftPending)
    }

    @Test
    fun `BS-23 punct shift pending backspace cancels it without touching the editor`() {
        editor.committed = "한"
        input(Tap, area = NUMBER)
        assertTrue(state.punctPending)
        val calls = editor.calls.size
        bs()
        assertEquals("한", editor.text)
        assertEquals(calls, editor.calls.size)
        assertFalse(state.punctPending)
    }

    @Test
    fun `BS-24 gi plus ignored i backspace gives g`() {
        input(G, I, I)
        bs()
        assertEquals("ㄱ", editor.text)
        assertEquals(CjState.Empty, asm.state)
    }

    // ---- Extra boundaries of the §6 table ----

    @Test
    fun `backspace with nothing composing deletes one committed character`() {
        editor.committed = "한"
        bs()
        assertEquals("", editor.text)
        assertEquals(listOf("del"), editor.calls)
    }

    @Test
    fun `extended shift then symbol drops the glyph and commits the symbol`() {
        input(G, Tap)
        input(StrokeToken.DiagDownRight, Symbol("x_at"))
        assertEquals("ㄱ@", editor.committed)
        assertEquals("", editor.composing)
    }

    @Test
    fun `backspace action is typed, state-only strokes are not`() {
        input(LineUp)
        assertEquals(0, sink.marks)
        input(G, StrokeToken.LineLeft)
        assertEquals(2, sink.marks)
        assertEquals("", editor.text)
    }

    // ---- snapshot / restore (LLD-05 §6, LLD-09 §2; dispatcher-level EG-06 is in InputDispatcherGraffitiTest) ----

    @Test
    fun `restore brings back the assembler and the state but not the composer`() {
        input(G, Tap, LineUp)
        val s = h.snapshot()
        asm.clear()
        state.enterMode()
        state.lang = EN
        composer.finish()
        h.restore(s)
        assertEquals("ㆍ", asm.pendingGlyph())
        assertTrue(state.shiftPending)
        assertEquals(KO, state.lang)
        assertEquals("", composer.composingText())   // the composer is restored by InputDispatcher
    }

    private fun gStroke(area: PadArea): InkStroke {
        val pts = mutableListOf<InkPoint>()
        var t = 0L
        for (k in 0 until G_SHAPE.size - 1) {
            val (x0, y0) = G_SHAPE[k]
            val (x1, y1) = G_SHAPE[k + 1]
            for (i in 0 until 20) {
                val f = i / 20f
                pts += InkPoint(100f + 60f * (x0 + (x1 - x0) * f), 80f + 60f * (y0 + (y1 - y0) * f), t)
                t += 8
            }
        }
        pts += InkPoint(100f + 60f * G_SHAPE.last().first, 80f + 60f * G_SHAPE.last().second, t)
        return InkStroke(pts, area, if (area == LETTER) LETTER_RECT else NUMBER_RECT)
    }

    /** A single point in the middle of [area], away from the corner (a tap, LLD-05 §3.1 step 1). */
    private fun tapStroke(area: PadArea = LETTER): InkStroke {
        val r = if (area == LETTER) LETTER_RECT else NUMBER_RECT
        return InkStroke(listOf(InkPoint((r.left + r.right) / 2, (r.top + r.bottom) / 2, 0L)), area, r)
    }

    /** A straight line of 100 dp from the letter area's middle (LLD-05 §3.1 step 2). */
    private fun line(dx: Float, dy: Float): InkStroke {
        val pts = (0..50).map { i -> InkPoint(150f + dx * i / 50, 120f + dy * i / 50, i * 8L) }
        return InkStroke(pts, LETTER, LETTER_RECT)
    }

    private fun downLine() = line(0f, 100f)
    private fun upLine() = line(0f, -100f)
    private fun leftLine() = line(-100f, 0f)
    private fun diagDownRight() = line(70f, 70f)

    private companion object {
        val G_SHAPE = listOf(0f to 0f, 1f to 0.05f, 0.95f to 1.05f)
        val LETTER_RECT = RectF2(0f, 0f, 300f, 240f)
        val NUMBER_RECT = RectF2(300f, 0f, 450f, 240f)
    }
}
