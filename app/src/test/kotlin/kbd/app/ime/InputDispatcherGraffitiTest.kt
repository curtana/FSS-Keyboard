package kbd.app.ime

import kbd.app.error.EngineGuard
import kbd.app.feedback.FeedbackPort
import kbd.app.graffiti.GraffitiInputHandler
import kbd.app.store.ErrorLog
import kbd.app.swipe.SwipeEngineState
import kbd.hangul.cheonjiin.CheonjiinAssembler
import kbd.hangul.cheonjiin.CjState
import kbd.graffiti.ink.GraffitiLang.EN
import kbd.graffiti.ink.GraffitiLang.KO
import kbd.graffiti.ink.PadArea
import kbd.graffiti.ink.PadArea.LETTER
import kbd.graffiti.recognize.Recognizer
import kbd.graffiti.recognize.SetName
import kbd.graffiti.recognize.StrokeToken
import kbd.graffiti.recognize.StrokeToken.CornerTap
import kbd.graffiti.recognize.StrokeToken.DiagDownRight
import kbd.graffiti.recognize.StrokeToken.LineDown
import kbd.graffiti.recognize.StrokeToken.LineUp
import kbd.graffiti.recognize.StrokeToken.Symbol
import kbd.graffiti.recognize.StrokeToken.Tap
import kbd.graffiti.recognize.StrokeToken.Unknown
import kbd.graffiti.recognize.Template
import kbd.graffiti.recognize.TemplateOutput
import kbd.graffiti.recognize.TemplateSet
import kbd.graffiti.state.GraffitiAction
import kbd.graffiti.state.GraffitiState
import kbd.hangul.compose.ComposePolicy
import kbd.hangul.compose.HangulComposer
import kbd.hangul.event.JamoEvent
import kbd.hangul.jamo.Jamo
import kbd.stroke.layout.FunctionKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * InputDispatcher graffiti branches and cross-mode cases (LLD-08 §6.1, §3.1; LLD-11 §7.4 ID-09, ID-10, ID-13,
 * ID-18, ID-21, ID-22, §2.1 BS-22, BS-25, §7.2a NF-03, §6.4 GH-20, GH-32, §7.5 EG-06). A real [GraffitiInputHandler] takes the
 * dispatcher as its `InputSink`; strokes go in as [StrokeToken]s through the real [GraffitiState] (LLD-11 §1.3).
 */
class InputDispatcherGraffitiTest {

    @get:Rule
    val tmp = TemporaryFolder()

    /** Editor text model: [committed] plus the composing span at its end; [sel] is a selection inside [committed]. */
    private class FakeEditor : EditorPort {
        var committed = ""
        var composing = ""
        var sel: IntRange? = null
        var throwOnCompose = false
        val calls = mutableListOf<String>()
        val text get() = committed + composing

        override fun commitText(text: String) { calls += "commit($text)"; committed += text; composing = "" }
        override fun setComposingText(text: String) {
            if (throwOnCompose) throw IllegalStateException("compose")
            calls += "compose($text)"
            composing = text
        }
        override fun finishComposingText() { calls += "finish"; committed += composing; composing = "" }
        override fun deleteBefore() {
            calls += "del"
            val s = sel
            // The editor's own DEL removes a selection (DCR-13).
            committed = if (s != null) committed.removeRange(s) else committed.dropLast(1)
            sel = null
        }
        override fun sendKey(key: EditorKey) { calls += "key($key)" }
        override fun performEditorAction(actionId: Int) { calls += "action($actionId)" }
        override fun setSelection(start: Int, end: Int) { calls += "sel($start,$end)" }
        override fun beginBatch() {}
        override fun endBatch() {}
    }

    private class FakeFeedback : FeedbackPort {
        var errors = 0
        override fun key() {}
        override fun error() { errors++ }
    }

    private class FakeSwipe : SwipeEngineState {
        var block = ""
        var resets = 0
        var backspaces = 0
        override fun reset(): Boolean { block = ""; resets++; return false }
        override fun popElement(): kbd.hangul.stroke.StrokePop { backspaces++; return kbd.hangul.stroke.StrokePop.Empty }
        override fun snapshot(): Any = block
        override fun restore(s: Any) { block = s as String }
    }

    private fun template(id: String, output: TemplateOutput, verts: List<Pair<Float, Float>>) =
        Template(id, output, corners = 1, strokes = listOf(verts))

    private val templates = TemplateSet(
        mapOf(
            SetName.KO_LETTER to listOf(
                template("ko_g", TemplateOutput.Jamo(Jamo.G), listOf(0f to 0f, 1f to 0.05f, 0.95f to 1.05f)),
                template("ko_finish_j", TemplateOutput.Control("FINISH_J"), listOf(1f to 0f, 1f to 1f, 0f to 0.8f)),
            ),
            SetName.EXTENDED to listOf(template("x_at", TemplateOutput.Text("@"), listOf(1f to 0f, 0f to 0.5f, 1f to 1f))),
        ),
    )

    private val editor = FakeEditor()
    private val feedback = FakeFeedback()
    private val cc = ComposingController(editor)
    private val saved = mutableListOf<InputMode>()
    private var modes = ModeController(InputMode.entries.toList(), InputMode.GRAFFITI)
    private val state = GraffitiState(templates)
    private val asm = CheonjiinAssembler()
    private var d = newDispatcher()
    private var h = handlerFor(d)

    /** `KeyboardService.onStartInputView` calls of `GraffitiInputHandler.enterMode()` (LLD-08 §3.1, last line). */
    private var serviceEnterModes = 0

    init {
        show(modes.current)
    }

    private fun newDispatcher() = InputDispatcher(modes, cc, feedback) { saved += it }.also { it.onShowMode = ::show }

    private fun handlerFor(x: InputDispatcher) =
        GraffitiInputHandler(Recognizer(templates), state, asm, { x.composer!! }, cc, x, {})

    /** `showModeView` stand-in (LLD-08 §3.1): composer policy per mode, the graffiti handler only in graffiti. */
    private fun show(m: InputMode) {
        d.composer = when (m) {
            InputMode.SWIPE -> HangulComposer(ComposePolicy.SWIPE)
            InputMode.DUBEOLSIK -> HangulComposer(ComposePolicy.DUBEOLSIK)
            InputMode.GRAFFITI -> HangulComposer(ComposePolicy.GRAFFITI)
            InputMode.QWERTY -> null
        }
        d.graffiti = if (m == InputMode.GRAFFITI) h else null
    }

    /** `KeyboardService.onStartInputView` in the LLD-08 §3.1 order. */
    private fun startInput(f: FieldInfo) {
        d.field = f
        d.finishAll()
        d.resetParsers()
        modes.onFieldChanged()
        val before = modes.current
        if (f.numericLike) modes.enterNumericField() else modes.leaveNumericField()
        cc.onStart(editor.committed.length, editor.committed.length)
        show(modes.current)
        if (modes.current == InputMode.GRAFFITI && before != InputMode.GRAFFITI) {
            serviceEnterModes++
            d.graffiti?.enterMode()
        }
    }

    private fun field(fieldClass: FieldClass = FieldClass.TEXT) =
        FieldInfo(fieldClass, isPassword = false, noLearning = false, multiLine = false, enterAction = null)

    /** Puts the keyboard in [m] as the service would; typed is cleared. */
    private fun mode(m: InputMode) {
        modes = ModeController(InputMode.entries.toList(), m)
        d = newDispatcher()
        h = handlerFor(d)
        show(m)
    }

    private fun input(vararg tokens: StrokeToken, area: PadArea = LETTER) {
        for (t in tokens) h.handle(state.interpret(t, area))
    }

    private val G = Symbol("ko_g")
    private val I = LineDown
    private val J = Symbol("ko_finish_j")

    /** "가ㆍ": 각 ⌫ leaves the vowel closed, so the tap shows a temporary glyph after 가 (BS-17). */
    private fun gaWithGlyph() {
        input(G, I, Tap, G)
        d.backspace()
        input(Tap)
        assertEquals("가ㆍ", editor.text)
        assertEquals(CjState.Pending::class, asm.state::class)
    }

    // ---- BS: "all modes" rows through InputDispatcher.backspace ----

    @Test
    fun `BS-22 nothing composing after 한 deletes one character in all four modes`() {
        for (m in InputMode.entries) {
            mode(m)
            val s = if (m == InputMode.SWIPE) FakeSwipe().also { d.swipeState = it } else null
            editor.committed = "한"
            editor.composing = ""
            editor.calls.clear()
            d.onFunctionDown(FunctionKey.BACKSPACE)
            assertEquals(m.name, listOf("del"), editor.calls)
            assertEquals(m.name, "", editor.text)
            assertTrue(m.name, modes.typed)
            s?.let { assertEquals(m.name, 1, it.backspaces) }   // FR-SWP-64: ⌫ asks the cheonjiin assembler first
        }
    }

    @Test
    fun `BS-25 nothing composing with a selection sends one delete in all four modes`() {
        for (m in InputMode.entries) {
            mode(m)
            editor.committed = "안녕하세요요"
            editor.composing = ""
            editor.sel = 3..4   // characters 3 to 5 selected
            editor.calls.clear()
            d.backspace()
            assertEquals(m.name, listOf("del"), editor.calls)
            assertEquals(m.name, "안녕하요", editor.text)
        }
    }

    // ---- GH rows that need InputDispatcher ----

    @Test
    fun `GH-20 cursor move in 가ㆍ commits 가, drops the dot and empties the assembler`() {
        cc.onStart(0, 0)
        gaWithGlyph()
        state.shiftPending = true
        assertEquals(SelectionVerdict.USER_MOVED, cc.onUpdateSelection(0, 0, 0, 2))
        d.onCursorMoved(0, 0, 0, 2)
        assertEquals("가", editor.text)
        assertEquals("가", editor.committed)
        assertEquals(CjState.Empty, asm.state)
        assertFalse(d.composer!!.isComposing())
        assertFalse(state.shiftPending)   // reset(CURSOR_MOVED) clears one-shots (LLD-05 §6)
    }

    @Test
    fun `GH-32 g tap then field change through finishAll commits g with no dot in the editor`() {
        input(G, Tap)
        assertEquals("ㄱㆍ", editor.text)
        startInput(field())
        assertEquals("ㄱ", editor.text)
        assertEquals("ㄱ", editor.committed)
        assertFalse(editor.committed.contains('ㆍ'))
        assertEquals(CjState.Empty, asm.state)
        assertEquals(0, serviceEnterModes)   // graffiti before and after: not a new entry
    }

    @Test
    fun `GH-32 g tap then extended shift and symbol commits g then the symbol, dot dropped`() {
        input(G, Tap)
        input(DiagDownRight, Symbol("x_at"))
        assertEquals("ㄱ@", editor.committed)
        assertEquals("", editor.composing)
        assertEquals(CjState.Empty, asm.state)
    }

    // ---- NF: numeric-like field round trip (NF-01, NF-02 replayed) ----

    @Test
    fun `NF-03 back to a text field returns to graffiti and calls enterMode exactly once`() {
        state.lang = EN
        state.capsLock = true
        // NF-01: graffiti → numeric field.
        startInput(field(FieldClass.NUMBER))
        assertEquals(InputMode.QWERTY, modes.current)
        assertEquals(InputMode.GRAFFITI, modes.returnMode)
        assertNull(modes.previous)
        assertNull(d.graffiti)
        // NF-02: mode key without input goes in order to swipe (SRS numeric-like table row 2, FSS#93).
        d.onModeKey()
        assertEquals(InputMode.SWIPE, modes.current)
        assertEquals(InputMode.GRAFFITI, modes.returnMode)
        assertNull(modes.previous)
        assertEquals(emptyList<InputMode>(), saved)
        assertEquals(0, serviceEnterModes)
        assertEquals(EN, state.lang)   // nothing has called enterMode yet
        // NF-03: text field. The §3.1 replay in startInput counts the service's enterMode call.
        startInput(field())
        assertEquals(InputMode.GRAFFITI, modes.current)
        assertNull(modes.returnMode)
        assertNull(modes.previous)
        assertEquals(emptyList<InputMode>(), saved)
        assertEquals(1, serviceEnterModes)
        assertEquals(KO, state.lang)
        assertFalse(state.capsLock)
    }

    // ---- ID: graffiti rows of the input classification ----

    @Test
    fun `ID-09 language toggle stroke keeps typed`() {
        input(CornerTap)
        assertEquals(EN, state.lang)
        assertFalse(modes.typed)
        input(G)
        assertTrue(modes.typed)
        input(CornerTap)
        assertEquals(KO, state.lang)
        assertTrue(modes.typed)
    }

    @Test
    fun `ID-10 shift, caps lock and extended shift strokes keep typed`() {
        input(LineUp)
        assertTrue(state.shiftPending)
        assertFalse(modes.typed)
        input(LineUp)   // KO: the second shift cancels
        input(CornerTap)
        input(LineUp, LineUp)
        assertTrue(state.capsLock)
        assertFalse(modes.typed)
        input(DiagDownRight)
        assertTrue(state.extPending)
        assertFalse(modes.typed)
        assertEquals(emptyList<String>(), editor.calls)
    }

    @Test
    fun `ID-13 onCursorMoved finishes the composer and empties the swipe mapper and the assembler`() {
        // Graffiti.
        cc.onStart(0, 0)
        input(G, Tap)
        cc.onUpdateSelection(0, 0, 0, 2)
        d.onCursorMoved(0, 0, 0, 2)
        assertFalse(d.composer!!.isComposing())
        assertEquals(CjState.Empty, asm.state)
        assertEquals("ㄱ", editor.committed)
        // Swipe.
        mode(InputMode.SWIPE)
        val s = FakeSwipe().also { d.swipeState = it }
        cc.onStart(1, 1)
        d.onJamoEvents(listOf(JamoEvent.Consonant(Jamo.G), JamoEvent.Vowel(Jamo.ofCompat('ㅗ')!!)))
        s.block = "ㄱㅗ"
        cc.onUpdateSelection(0, 0, 1, 2)
        d.onCursorMoved(0, 0, 1, 2)
        assertFalse(d.composer!!.isComposing())
        assertEquals("", s.block)
        assertEquals("ㄱ고", editor.committed)
    }

    @Test
    fun `ID-18 mode key into graffiti calls enterMode, KO and caps off`() {
        mode(InputMode.SWIPE)   // FSS#93: graffiti follows swipe in the cycle
        d.swipeState = FakeSwipe()
        state.lang = EN
        state.capsLock = true
        state.shiftPending = true
        modes.onInput()   // typed, no previous → next in order is GRAFFITI
        d.onModeKey()
        assertEquals(InputMode.GRAFFITI, modes.current)
        assertTrue(d.graffiti === h)
        assertEquals(KO, state.lang)
        assertFalse(state.capsLock)
        assertFalse(state.shiftPending)
        assertEquals(listOf(InputMode.GRAFFITI), saved)
    }

    @Test
    fun `ID-18 mode key out of graffiti finishes through the handler and does not call enterMode`() {
        input(CornerTap, LineUp, LineUp)   // EN, caps lock
        input(CornerTap, G, Tap)           // back to KO for ㄱㆍ, caps stays
        assertTrue(state.capsLock)
        d.onModeKey()
        assertEquals("ㄱ", editor.text)
        assertEquals(CjState.Empty, asm.state)
        assertTrue(state.capsLock)          // reset(MODE_CHANGED) keeps caps; enterMode was not called
        assertTrue(modes.current != InputMode.GRAFFITI)
    }

    @Test
    fun `ID-21 J finish stroke keeps typed, one cheonjiin element sets it`() {
        input(G)
        modes.onFieldChanged()   // typed false
        input(J)
        assertEquals("ㄱ", editor.committed)
        assertFalse(modes.typed)
        input(Tap)
        assertEquals("ㆍ", editor.composing)
        assertTrue(modes.typed)
    }

    @Test
    fun `ID-22 recognition failure vibrates once, keeps typed and touches no editor`() {
        input(G, I, Tap)
        modes.onFieldChanged()
        editor.calls.clear()
        input(Unknown)
        assertEquals(1, feedback.errors)
        assertFalse(modes.typed)
        assertEquals(emptyList<String>(), editor.calls)
        assertEquals("가", editor.text)
    }

    // ---- EG: rollback of the graffiti assembler and state (LLD-09 §2, LLD-08 §6.1, LLD-05 §6) ----

    @Test
    fun `EG-06 editor write failure for ㄴ after ㄱㆍ restores the assembler, the state and the composer`() {
        val log = ErrorLog(tmp.newFile("err.log"))
        val guard = EngineGuard(log, d)
        input(G, Tap)
        assertEquals("ㄱㆍ", editor.text)
        val stateBefore = state.snapshot()
        editor.throwOnCompose = true
        // The failure is in show, after asm.clear() and composer.apply(ㄴ).
        assertNull(guard.stroke("Graffiti.stroke") { h.handle(GraffitiAction.Consonant(Jamo.N)) })
        assertEquals("ㆍ", asm.pendingGlyph())
        assertEquals(CjState.Pending::class, asm.state::class)
        assertEquals(stateBefore, state.snapshot())
        assertEquals("ㄱ", d.composer!!.composingText())
        assertEquals("ㄱㆍ", cc.composingShown)
        assertEquals(1, log.readAll().lines().count { it.isNotEmpty() })
    }

    @Test
    fun `EG-06 a one-shot shift consumed by the failed stroke comes back`() {
        val guard = EngineGuard(ErrorLog(tmp.newFile("err.log")), d)
        input(G, Tap, LineUp)
        assertTrue(state.shiftPending)
        val before = state.snapshot()
        editor.throwOnCompose = true
        // interpret turns the shift off (row 19) before the editor write fails.
        assertNull(guard.stroke("Graffiti.stroke") { h.handle(state.interpret(G, LETTER)) })
        assertEquals(before, state.snapshot())
        assertEquals("ㆍ", asm.pendingGlyph())
        assertEquals("ㄱ", d.composer!!.composingText())
    }
}
