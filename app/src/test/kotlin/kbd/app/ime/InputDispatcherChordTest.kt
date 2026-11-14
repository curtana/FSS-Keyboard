package kbd.app.ime

import kbd.app.error.EngineGuard
import kbd.app.feedback.FeedbackPort
import kbd.app.store.ErrorLog
import kbd.hangul.compose.ComposePolicy
import kbd.hangul.compose.HangulComposer
import kbd.stroke.geom.Dir
import kbd.stroke.layout.FunctionKey
import kbd.stroke.layout.TapKeyDef
import kbd.stroke.layout.TapLayout
import kbd.stroke.layout.TapLayouts
import kbd.stroke.tap.ChordKey
import kbd.stroke.tap.ChordModifier
import kbd.stroke.tap.ChordModifier.ALT
import kbd.stroke.tap.ChordModifier.CTRL
import kbd.stroke.tap.TapGesture
import kbd.stroke.tap.TapKeyResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * LLD-11 §7.4a CD-01..CD-18: Ctrl and Alt in `InputDispatcher` (LLD-08 §6.3, SRS FR-TAP-15..17, FSS#98). The
 * behaviour with both off is what `InputDispatcherTest` already pins; CD-17 adds the new entry point to it.
 */
class InputDispatcherChordTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private class FakePort : EditorPort {
        val calls = mutableListOf<String>()
        var throwOnChord = false
        override fun commitText(text: String) { calls += "commit($text)" }
        override fun setComposingText(text: String) { calls += "compose($text)" }
        override fun finishComposingText() { calls += "finish" }
        override fun deleteBefore() { calls += "del" }
        override fun sendKey(key: EditorKey) { calls += "key($key)" }
        override fun sendChord(key: ChordKey, mods: Set<ChordModifier>) {
            if (throwOnChord) throw IllegalStateException("chord")
            calls += "chord($key,${mods.sorted()})"   // modifiers in the order they are sent
        }
        override fun performEditorAction(actionId: Int) { calls += "action($actionId)" }
        override fun setSelection(start: Int, end: Int) { calls += "sel($start,$end)" }
        override fun beginBatch() { calls += "begin" }
        override fun endBatch() { calls += "end" }
    }

    private class FakeFeedback : FeedbackPort {
        var errors = 0
        override fun key() = Unit
        override fun error() { errors++ }
    }

    private val port = FakePort()
    private val feedback = FakeFeedback()
    private val cc = ComposingController(port)
    private val saved = mutableListOf<InputMode>()
    private val shown = mutableListOf<InputMode>()
    private val modes = ModeController(listOf(InputMode.QWERTY, InputMode.DUBEOLSIK), InputMode.QWERTY)
    private var changes = 0
    private var settingsOpened = 0
    private val d = InputDispatcher(modes, cc, feedback) { saved += it }.also {
        it.onShowMode = { m -> shown += m }
        it.onChordChanged = { changes++ }
        it.onOpenSettings = { settingsOpened++ }
    }

    private val q = TapLayouts.QWERTY
    private val dub = TapLayouts.DUBEOLSIK

    private fun key(layout: TapLayout, id: String): TapKeyDef = layout.keys.single { it.id == id }
    private fun jamoKey(j: Char): TapKeyDef = dub.keys.single { it.jamo == j }

    /** One key as the view sends it: the resolved output and the judged gesture. */
    private fun hit(k: TapKeyDef, g: TapGesture = TapGesture.Tap) = d.onTapResult(k, TapKeyResolver.resolve(k, g), g)
    private fun hit(id: String, g: TapGesture = TapGesture.Tap) = hit(key(q, id), g)

    private fun down(f: FunctionKey) = d.onFunctionDown(f)
    private fun up(f: FunctionKey) = d.onFunctionUp(f)

    /** Press and release, as a tap on the key. */
    private fun press(f: FunctionKey) {
        down(f)
        up(f)
    }

    private fun dubeolsik() {
        d.composer = HangulComposer(ComposePolicy.DUBEOLSIK)
    }

    private fun type(text: String) {
        for (c in text) hit(jamoKey(c))
    }

    private fun exit(dir: Dir) = TapGesture.Exit(dir)

    @Test
    fun `CD-01 Ctrl and Alt toggle on press - nothing sent, not input, what is composing stays`() {
        dubeolsik()
        type("ㄱㅏ")
        port.calls.clear()
        modes.typed = false
        down(FunctionKey.CTRL)
        assertEquals(setOf(CTRL), d.chord)
        up(FunctionKey.CTRL)
        assertEquals(setOf(CTRL), d.chord)          // the release changes nothing
        down(FunctionKey.CTRL)                      // pressed again: off
        assertEquals(emptySet<ChordModifier>(), d.chord)
        up(FunctionKey.CTRL)
        press(FunctionKey.ALT)
        assertEquals(setOf(ALT), d.chord)
        press(FunctionKey.ALT)
        assertEquals(emptySet<ChordModifier>(), d.chord)
        assertEquals(emptyList<String>(), port.calls)
        assertEquals("가", cc.composingShown)
        assertTrue(d.composer!!.isComposing())
        assertFalse(modes.typed)
        assertEquals(4, changes)
    }

    @Test
    fun `CD-02 Ctrl and Alt both on ride on one key and both go off`() {
        press(FunctionKey.CTRL)
        press(FunctionKey.ALT)
        assertEquals(setOf(CTRL, ALT), d.chord)
        hit("c")
        assertEquals(listOf("chord(C,[CTRL, ALT])"), port.calls)
        assertEquals(emptySet<ChordModifier>(), d.chord)
        assertEquals(3, changes)   // on, on, taken together
        // Alt first, then Ctrl: the same set, sent in the same order.
        port.calls.clear()
        press(FunctionKey.ALT)
        press(FunctionKey.CTRL)
        press(FunctionKey.ARROW_LEFT)
        assertEquals(listOf("chord(DPAD_LEFT,[CTRL, ALT])"), port.calls)
    }

    @Test
    fun `CD-03 a chord rides on one key only - the key is not typed, the next one is`() {
        modes.typed = false
        press(FunctionKey.CTRL)
        hit("c")
        assertEquals(listOf("chord(C,[CTRL])"), port.calls)
        assertEquals(emptySet<ChordModifier>(), d.chord)
        assertTrue(modes.typed)   // a letter key is input with a chord too (FR-TAP-16 ⑦)
        hit("c")
        assertEquals(listOf("chord(C,[CTRL])", "commit(c)"), port.calls)
        port.calls.clear()
        press(FunctionKey.ALT)
        hit("x")
        assertEquals(listOf("chord(X,[ALT])"), port.calls)
    }

    @Test
    fun `CD-04 the gesture picks the key - up is Shift, assigned symbols are their PC keys, rubs are the letter`() {
        fun chord(id: String, g: TapGesture): String {
            port.calls.clear()
            press(FunctionKey.CTRL)
            hit(id, g)
            return port.calls.single()
        }
        assertEquals("chord(A,[CTRL, SHIFT])", chord("a", exit(Dir.UP)))
        assertEquals("chord(A,[CTRL])", chord("a", exit(Dir.LEFT)))
        assertEquals("chord(MINUS,[CTRL])", chord("minus", exit(Dir.LEFT)))
        assertEquals("chord(EQUALS,[CTRL])", chord("minus", TapGesture.Tap))
        assertEquals("chord(EQUALS,[CTRL, SHIFT])", chord("minus", exit(Dir.DOWN)))
        assertEquals("chord(LEFT_BRACKET,[CTRL])", chord("i", exit(Dir.LEFT)))
        assertEquals("chord(BACKSLASH,[CTRL, SHIFT])", chord("p", exit(Dir.DOWN)))
        assertEquals("chord(D1,[CTRL])", chord("1", TapGesture.Tap))
        assertEquals("chord(D2,[CTRL, SHIFT])", chord("2", exit(Dir.UP)))
        assertEquals("chord(SLASH,[CTRL, SHIFT])", chord("slash", exit(Dir.UP)))
        assertEquals("chord(APOSTROPHE,[CTRL])", chord("comma", exit(Dir.LEFT)))
        // b has no subscript: without a chord the plain letter and the error vibration, with one neither.
        assertEquals("chord(B,[CTRL])", chord("b", TapGesture.Rub(Dir.DOWN)))
        assertEquals("chord(A,[CTRL])", chord("a", TapGesture.Rub(Dir.UP)))
        assertEquals(0, feedback.errors)
        // Alt and Shift.
        port.calls.clear()
        press(FunctionKey.ALT)
        hit("f", exit(Dir.UP))
        assertEquals(listOf("chord(F,[ALT, SHIFT])"), port.calls)
    }

    @Test
    fun `CD-05 Dubeolsik - what is composing is committed first, then the English key of that place`() {
        dubeolsik()
        type("ㄱㅏㄱ")
        port.calls.clear()
        press(FunctionKey.CTRL)
        assertEquals("각", cc.composingShown)   // turning Ctrl on commits nothing
        hit(jamoKey('ㅊ'))                      // the c key
        assertEquals(listOf("finish", "chord(C,[CTRL])"), port.calls)
        assertFalse(d.composer!!.isComposing())
        assertEquals("", cc.composingShown)

        fun chord(k: TapKeyDef, g: TapGesture): String {
            port.calls.clear()
            press(FunctionKey.CTRL)
            hit(k, g)
            return port.calls.single()
        }
        assertEquals("chord(Q,[CTRL])", chord(jamoKey('ㅂ'), TapGesture.Tap))
        assertEquals("chord(Q,[CTRL, SHIFT])", chord(jamoKey('ㅂ'), exit(Dir.UP)))      // ㅃ
        assertEquals("chord(Q,[CTRL])", chord(jamoKey('ㅂ'), exit(Dir.DOWN)))           // ㅄ: nothing on QWERTY q ↓
        assertEquals("chord(F,[CTRL])", chord(jamoKey('ㄹ'), exit(Dir.LEFT)))           // ㄺ
        assertEquals("chord(A,[CTRL, SHIFT])", chord(jamoKey('ㅁ'), exit(Dir.UP)))      // ㅁ ↑ types ㅁ; still Shift + A
        assertEquals("chord(SEMICOLON,[CTRL])", chord(jamoKey('ㅣ'), exit(Dir.LEFT)))
        assertEquals("chord(D5,[CTRL, SHIFT])", chord(key(dub, "5"), exit(Dir.UP)))
        assertFalse(d.composer!!.isComposing())   // no jamo reached the composer
    }

    @Test
    fun `CD-06 function keys with a chord are key events - no space, tab, editor action or undo of the syllable`() {
        dubeolsik()
        d.field = FieldInfo(FieldClass.TEXT, isPassword = false, noLearning = false, multiLine = true, enterAction = 3)
        fun chord(f: FunctionKey): List<String> {
            type("ㄷㅏㄹㄱ")
            port.calls.clear()
            press(FunctionKey.CTRL)
            press(f)
            assertEquals("$f", "", cc.composingShown)
            return port.calls.toList()
        }
        assertEquals(listOf("finish", "chord(SPACE,[CTRL])"), chord(FunctionKey.SPACE))
        assertEquals(listOf("finish", "chord(ENTER,[CTRL])"), chord(FunctionKey.ENTER))
        assertEquals(listOf("finish", "chord(TAB,[CTRL])"), chord(FunctionKey.TAB))
        assertEquals(listOf("finish", "chord(DEL,[CTRL])"), chord(FunctionKey.BACKSPACE))   // 닭 is committed, not undone to 달
        assertEquals(listOf("finish", "chord(ESCAPE,[CTRL])"), chord(FunctionKey.ESC))
        assertEquals(listOf("finish", "chord(FORWARD_DEL,[CTRL])"), chord(FunctionKey.DELETE))
        assertEquals(listOf("finish", "chord(DPAD_LEFT,[CTRL])"), chord(FunctionKey.ARROW_LEFT))
        assertEquals(listOf("finish", "chord(DPAD_UP,[CTRL])"), chord(FunctionKey.ARROW_UP))
        assertEquals(listOf("finish", "chord(DPAD_DOWN,[CTRL])"), chord(FunctionKey.ARROW_DOWN))
        assertEquals(listOf("finish", "chord(DPAD_RIGHT,[CTRL])"), chord(FunctionKey.ARROW_RIGHT))
    }

    @Test
    fun `CD-07 input or not is the key's own class, with a chord as without`() {
        fun typedAfter(f: FunctionKey): Boolean {
            modes.typed = false
            press(FunctionKey.ALT)
            press(f)
            return modes.typed
        }
        for (f in listOf(FunctionKey.SPACE, FunctionKey.ENTER, FunctionKey.TAB, FunctionKey.BACKSPACE)) assertTrue("$f", typedAfter(f))
        for (f in listOf(FunctionKey.ARROW_LEFT, FunctionKey.ARROW_UP, FunctionKey.ARROW_DOWN, FunctionKey.ARROW_RIGHT,
            FunctionKey.ESC, FunctionKey.DELETE)) assertFalse("$f", typedAfter(f))
        modes.typed = false
        press(FunctionKey.ALT)
        hit("1")
        assertTrue(modes.typed)
    }

    @Test
    fun `CD-08 a held key repeats with the chord its press took, and no other key gets it meanwhile`() {
        press(FunctionKey.CTRL)
        down(FunctionKey.ARROW_LEFT)                     // the press
        assertEquals(emptySet<ChordModifier>(), d.chord)   // off from the press on
        down(FunctionKey.ARROW_LEFT)                     // repeat 1 (RepeatTimer: a down with no up)
        hit("a")                                         // another finger meanwhile: a plain letter
        press(FunctionKey.ARROW_UP)                      // and a plain arrow
        down(FunctionKey.ARROW_LEFT)                     // repeat 2
        up(FunctionKey.ARROW_LEFT)
        assertEquals(
            listOf("chord(DPAD_LEFT,[CTRL])", "chord(DPAD_LEFT,[CTRL])", "commit(a)", "key(DPAD_UP)", "chord(DPAD_LEFT,[CTRL])"),
            port.calls,
        )
        port.calls.clear()
        press(FunctionKey.ARROW_LEFT)                    // released: the chord is forgotten
        assertEquals(listOf("key(DPAD_LEFT)"), port.calls)
        // ⌫ and Del repeat the same way.
        port.calls.clear()
        press(FunctionKey.ALT)
        repeat(3) { down(FunctionKey.BACKSPACE) }
        up(FunctionKey.BACKSPACE)
        press(FunctionKey.CTRL)
        repeat(2) { down(FunctionKey.DELETE) }
        up(FunctionKey.DELETE)
        assertEquals(List(3) { "chord(DEL,[ALT])" } + List(2) { "chord(FORWARD_DEL,[CTRL])" }, port.calls)
    }

    @Test
    fun `CD-09 Ctrl turned on while a plain arrow repeats does not join it and rides on the next key`() {
        down(FunctionKey.ARROW_LEFT)
        press(FunctionKey.CTRL)                          // another finger, mid-hold
        down(FunctionKey.ARROW_LEFT)                     // repeat: still a plain ←
        down(FunctionKey.ARROW_LEFT)
        assertEquals(List(3) { "key(DPAD_LEFT)" }, port.calls)
        assertEquals(setOf(CTRL), d.chord)               // still on
        port.calls.clear()
        hit("c")                                         // ← still held: the next target key takes it
        down(FunctionKey.ARROW_LEFT)
        up(FunctionKey.ARROW_LEFT)
        assertEquals(listOf("chord(C,[CTRL])", "key(DPAD_LEFT)"), port.calls)
        assertEquals(emptySet<ChordModifier>(), d.chord)
    }

    @Test
    fun `CD-10 the mode key takes no chord - both go off, nothing is sent, the mode changes`() {
        press(FunctionKey.CTRL)
        press(FunctionKey.ALT)
        press(FunctionKey.MODE)
        assertEquals(emptySet<ChordModifier>(), d.chord)
        assertEquals(emptyList<String>(), port.calls)
        assertEquals(listOf(InputMode.DUBEOLSIK), shown)
        assertEquals(3, changes)
        hit("c")
        assertEquals(listOf("commit(c)"), port.calls)
    }

    @Test
    fun `CD-11 the mode key's up stroke turns the chord off and opens the settings`() {
        press(FunctionKey.CTRL)
        d.onSettingsStroke()
        assertEquals(emptySet<ChordModifier>(), d.chord)
        assertEquals(1, settingsOpened)
        assertEquals(emptyList<String>(), port.calls)
    }

    @Test
    fun `CD-12 clearChord turns both off without sending, tells once, and not at all when nothing was on`() {
        d.clearChord()
        assertEquals(0, changes)
        press(FunctionKey.CTRL)
        press(FunctionKey.ALT)
        d.clearChord()
        assertEquals(emptySet<ChordModifier>(), d.chord)
        assertEquals(3, changes)
        d.clearChord()
        assertEquals(3, changes)
        assertEquals(emptyList<String>(), port.calls)
        hit("v")
        assertEquals(listOf("commit(v)"), port.calls)
    }

    @Test
    fun `CD-13 clearChord forgets what a held key took - its next repeat is plain`() {
        press(FunctionKey.CTRL)
        down(FunctionKey.ARROW_RIGHT)
        d.clearChord()                                   // a new field while the key is held
        down(FunctionKey.ARROW_RIGHT)
        assertEquals(listOf("chord(DPAD_RIGHT,[CTRL])", "key(DPAD_RIGHT)"), port.calls)
    }

    @Test
    fun `CD-14 a cursor move does not turn the chord off`() {
        dubeolsik()
        press(FunctionKey.CTRL)
        d.onCursorMovedNoComposing()
        assertEquals(setOf(CTRL), d.chord)
        type("ㄱ")                                       // takes the chord: Ctrl + R
        press(FunctionKey.ALT)
        type("ㄱ")
        press(FunctionKey.CTRL)
        cc.onStart(0, 0)
        d.onCursorMoved(3, 3, -1, -1)                    // the user selected text, then Ctrl + C must still work
        assertEquals(setOf(CTRL), d.chord)
        port.calls.clear()
        hit(jamoKey('ㅊ'))
        assertEquals(listOf("chord(C,[CTRL])"), port.calls)
    }

    @Test
    fun `CD-15 the change callback comes once per change`() {
        press(FunctionKey.CTRL)        // 1: on
        press(FunctionKey.ALT)         // 2: on
        press(FunctionKey.ALT)         // 3: off
        hit("a")                       // 4: taken
        hit("a")                       // none
        press(FunctionKey.ARROW_LEFT)  // none: nothing was on
        press(FunctionKey.CTRL)        // 5
        down(FunctionKey.ARROW_LEFT)   // 6: taken by the press
        down(FunctionKey.ARROW_LEFT)   // none: a repeat
        up(FunctionKey.ARROW_LEFT)     // none
        assertEquals(6, changes)
    }

    @Test
    fun `CD-16 a chord that fails stays taken - it does not ride on the next key (HLD 9)`() {
        val guard = EngineGuard(ErrorLog(tmp.newFile("err.log")), d)
        dubeolsik()
        type("ㄱㅏ")
        press(FunctionKey.CTRL)
        port.throwOnChord = true
        guard.stroke("Tap.output") { hit(jamoKey('ㅊ')) }
        assertEquals(emptySet<ChordModifier>(), d.chord)
        port.throwOnChord = false
        port.calls.clear()
        guard.stroke("Tap.output") { hit(jamoKey('ㅊ')) }
        assertFalse(port.calls.any { it.startsWith("chord") })
        // The snapshot has no chord in it: restoring one leaves a chord turned on afterwards alone.
        val snap = d.snapshot()
        press(FunctionKey.ALT)
        d.restore(snap)
        assertEquals(setOf(ALT), d.chord)
    }

    @Test
    fun `CD-17 with Ctrl and Alt off the new entry point is the old one, whatever the gesture`() {
        dubeolsik()
        val a = key(q, "a")
        d.onTapResult(a, TapKeyResolver.resolve(a, exit(Dir.UP)), exit(Dir.UP))
        val b = key(q, "b")
        d.onTapResult(b, TapKeyResolver.resolve(b, TapGesture.Rub(Dir.DOWN)), TapGesture.Rub(Dir.DOWN))
        assertEquals(1, feedback.errors)   // b has no subscript (FR-TAP-01b)
        hit(jamoKey('ㄱ'))
        d.onTapOutput(jamoKey('ㅏ'), TapKeyResolver.resolve(jamoKey('ㅏ'), TapGesture.Tap))   // the two-argument form
        press(FunctionKey.TAB)
        press(FunctionKey.BACKSPACE)
        assertEquals(
            listOf("commit(A)", "commit(b)", "compose(ㄱ)", "compose(가)", "finish", "commit(\t)", "del"),
            port.calls.filter { it != "begin" && it != "end" },
        )
        assertEquals(0, changes)
        assertEquals(emptySet<ChordModifier>(), d.chord)
    }

    @Test
    fun `CD-18 a result without its gesture is a tap - the accessibility click and a password field work the same`() {
        d.field = FieldInfo(FieldClass.TEXT, isPassword = true, noLearning = true, multiLine = false, enterAction = null)
        press(FunctionKey.CTRL)
        val a = key(q, "a")
        d.onTapOutput(a, TapKeyResolver.resolve(a, TapGesture.Tap))
        assertEquals(listOf("chord(A,[CTRL])"), port.calls)
    }
}
