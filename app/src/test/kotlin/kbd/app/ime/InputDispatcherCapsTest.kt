package kbd.app.ime

import kbd.app.feedback.FeedbackPort
import kbd.stroke.geom.Dir
import kbd.stroke.layout.FunctionKey
import kbd.stroke.layout.TapKeyDef
import kbd.stroke.layout.TapLayout
import kbd.stroke.layout.TapLayouts
import kbd.stroke.tap.ChordKey
import kbd.stroke.tap.ChordModifier
import kbd.stroke.tap.TapGesture
import kbd.stroke.tap.TapKeyResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * LLD-11 §7.4b CS-01..CS-18: Caps Lock of the QWERTY letter keys in `InputDispatcher` (LLD-08 §6.4, SRS FR-TAP-19,
 * FSS#131). The editor is a text buffer, so a take-back that deletes the wrong character shows in [FakePort.text].
 */
class InputDispatcherCapsTest {

    /** An editor holding its text, the cursor always at the end. */
    private class FakePort : EditorPort {
        val text = StringBuilder()
        val calls = mutableListOf<String>()
        /** The editor drops what is committed (an input filter, a full field). */
        var rejects = false
        /** The editor does not tell what is before the cursor. */
        var unreadable = false
        override fun commitText(text: String) {
            calls += "commit($text)"
            if (!rejects) this.text.append(text)
        }
        override fun setComposingText(text: String) { calls += "compose($text)" }
        override fun finishComposingText() { calls += "finish" }
        override fun deleteBefore() {
            calls += "del"
            if (text.isNotEmpty()) text.setLength(text.length - 1)
        }
        override fun textBeforeCursor(n: Int): String? =
            if (unreadable) null else text.substring(maxOf(0, text.length - n))
        override fun sendKey(key: EditorKey) { calls += "key($key)" }
        override fun sendChord(key: ChordKey, mods: Set<ChordModifier>) { calls += "chord($key,${mods.sorted()})" }
        override fun performEditorAction(actionId: Int) { calls += "action($actionId)" }
        override fun setSelection(start: Int, end: Int) { calls += "sel($start,$end)" }
        override fun beginBatch() = Unit
        override fun endBatch() = Unit
    }

    private class FakeFeedback : FeedbackPort {
        var errors = 0
        override fun key() = Unit
        override fun error() { errors++ }
    }

    private val port = FakePort()
    private val feedback = FakeFeedback()
    private val cc = ComposingController(port)
    private val modes = ModeController(listOf(InputMode.DUBEOLSIK, InputMode.QWERTY), InputMode.QWERTY)
    private var now = 1_000L
    private var capsChanges = 0
    private val d = InputDispatcher(modes, cc, feedback) {}.also {
        it.clock = { now }
        it.onCapsChanged = { capsChanges++ }
    }

    private val q = TapLayouts.QWERTY
    private val up = TapGesture.Exit(Dir.UP)
    private val win = InputDispatcher.CAPS_DOUBLE_MS

    private fun key(layout: TapLayout, id: String): TapKeyDef = layout.keys.single { it.id == id }
    private fun hit(id: String, g: TapGesture = TapGesture.Tap, layout: TapLayout = q) =
        key(layout, id).let { d.onTapResult(it, TapKeyResolver.resolve(it, g), g) }

    /** Turns Caps Lock on with two ↑ at once. */
    private fun capsOn() {
        hit("a", up)
        hit("s", up)
        assertTrue(d.capsLock)
    }

    @Test
    fun `CS-01 one up exit types one capital and leaves Caps Lock off`() {
        hit("a", up)
        assertEquals("A", port.text.toString())
        assertFalse(d.capsLock)
        assertEquals(0, capsChanges)
    }

    @Test
    fun `CS-02 two up exits within the window turn Caps Lock on and leave no character`() {
        port.text.append("x")
        hit("a", up)
        now += win   // the limit itself still counts
        hit("s", up)   // another key is fine
        assertTrue(d.capsLock)
        assertEquals("x", port.text.toString())   // the first capital taken back, nothing else
        assertEquals(listOf("commit(A)", "del"), port.calls)
        assertEquals(1, capsChanges)
    }

    @Test
    fun `CS-03 a second up exit after the window is just another capital`() {
        hit("a", up)
        now += win + 1
        hit("s", up)
        assertEquals("AS", port.text.toString())
        assertFalse(d.capsLock)
        now += 10
        hit("d", up)   // but this one is quick after S: on, S taken back
        assertTrue(d.capsLock)
        assertEquals("A", port.text.toString())
    }

    @Test
    fun `CS-04 Caps Lock on - a tap and a direction with nothing assigned type the capital`() {
        capsOn()
        hit("a")
        hit("a", TapGesture.Exit(Dir.DOWN))   // a has nothing below: the letter
        hit("l", TapGesture.Exit(Dir.RIGHT))   // l has nothing right
        hit("q", TapGesture.Rub(Dir.LEFT))   // a left rub is the left exit
        assertEquals("AALQ", port.text.toString())
        assertTrue(d.capsLock)
    }

    @Test
    fun `CS-05 Caps Lock on - symbols, digits and rubs stay as they are`() {
        capsOn()
        hit("i", TapGesture.Exit(Dir.LEFT))   // [
        hit("l", TapGesture.Exit(Dir.DOWN))   // :
        hit("1")
        hit("a", TapGesture.Rub(Dir.UP))   // superscript a
        hit("b", TapGesture.Rub(Dir.DOWN))   // no subscript b: the plain letter, with the error vibration
        assertEquals("[:1" + TapKeyResolver.resolve(key(q, "a"), TapGesture.Rub(Dir.UP)).text + "b", port.text.toString())
        assertEquals(1, feedback.errors)
    }

    @Test
    fun `CS-17 a digit or symbol key's up is its US Shift character with Caps Lock on or off, and never counts`() {
        val shifted = linkedMapOf("1" to "!", "2" to "@", "0" to ")", "comma" to "<", "period" to ">", "minus" to "_", "slash" to "?")
        for (id in shifted.keys) hit(id, up)   // off, all quick: no toggle
        assertFalse(d.capsLock)
        val all = shifted.values.joinToString("")
        assertEquals(all, port.text.toString())
        capsOn()   // a, s: nothing left
        for (id in shifted.keys) hit(id, up)   // on: the same characters, and quick ups on them never turn it off
        assertTrue(d.capsLock)
        assertEquals(all + all, port.text.toString())
        assertEquals(1, capsChanges)
    }

    @Test
    fun `CS-06 Caps Lock on - one up types the lower case, two quick ups turn it off and leave nothing`() {
        capsOn()
        now += 5_000
        hit("z", up)   // Shift with Caps Lock on
        assertEquals("z", port.text.toString())
        assertTrue(d.capsLock)
        now += 5_000
        hit("x", up)
        now += win
        hit("c", up)   // the second quick up: off, x taken back
        assertFalse(d.capsLock)
        assertEquals("z", port.text.toString())
        assertEquals(2, capsChanges)
        hit("a")
        hit("b", up)
        assertEquals("zaB", port.text.toString())
    }

    @Test
    fun `CS-07 quick ups go in pairs - the third types, the fourth turns it off`() {
        hit("a", up)
        hit("s", up)   // pair 1: on
        hit("d", up)   // the toggling up starts no pair: a lower-case d
        assertTrue(d.capsLock)
        assertEquals("d", port.text.toString())
        hit("f", up)   // pair 2: off, d taken back
        assertFalse(d.capsLock)
        assertEquals("", port.text.toString())
    }

    @Test
    fun `CS-08 exit-and-return up counts as an up exit`() {
        hit("a", TapGesture.ExitReturn(Dir.UP))
        hit("s", TapGesture.ExitReturn(Dir.UP))
        assertTrue(d.capsLock)
        assertEquals("", port.text.toString())
    }

    @Test
    fun `CS-09 another key or a function key in between ends the wait`() {
        hit("a", up)
        hit("b")
        hit("s", up)
        assertEquals("AbS", port.text.toString())
        d.onFunctionDown(FunctionKey.SPACE)
        d.onFunctionUp(FunctionKey.SPACE)
        assertEquals("AbS ", port.text.toString())
        hit("d", up)
        assertEquals("AbS D", port.text.toString())   // the space came between S and D
        d.onFunctionDown(FunctionKey.BACKSPACE)
        hit("e", up)
        assertEquals("AbS E", port.text.toString())
        assertFalse(d.capsLock)
    }

    @Test
    fun `CS-10 take-back deletes nothing when the capital is not before the cursor`() {
        port.text.append("xy")
        port.rejects = true   // the field dropped the A
        hit("a", up)
        hit("s", up)
        assertTrue(d.capsLock)   // still on
        assertEquals("xy", port.text.toString())   // y is not deleted
        assertFalse("del" in port.calls)

        d.clearCaps()
        port.rejects = false
        port.unreadable = true   // the editor does not tell
        hit("a", up)
        hit("s", up)
        assertTrue(d.capsLock)
        assertEquals("xyA", port.text.toString())   // the capital stays rather than risk another character
        assertFalse("del" in port.calls)
    }

    @Test
    fun `CS-11 the mode key and clearCaps turn it off once, nothing is sent`() {
        capsOn()
        val before = port.calls.toList()
        d.clearCaps()
        assertFalse(d.capsLock)
        assertEquals(2, capsChanges)
        d.clearCaps()
        assertEquals(2, capsChanges)   // already off: no call
        capsOn()
        d.onModeKey()
        assertFalse(d.capsLock)
        assertEquals(4, capsChanges)
        assertEquals(before + listOf("commit(A)", "del"), port.calls)
    }

    @Test
    fun `CS-12 clearCaps also ends the wait for a second up`() {
        hit("a", up)
        d.clearCaps()
        hit("s", up)
        assertEquals("AS", port.text.toString())
        assertFalse(d.capsLock)
    }

    @Test
    fun `CS-13 with Ctrl on, an up exit is the chord and Caps Lock stays as it is`() {
        capsOn()
        d.onFunctionDown(FunctionKey.CTRL)
        d.onFunctionUp(FunctionKey.CTRL)
        hit("a", up)   // Ctrl + Shift + A (FR-TAP-16), not the off stroke
        assertTrue(d.capsLock)
        d.onFunctionDown(FunctionKey.CTRL)
        d.onFunctionUp(FunctionKey.CTRL)
        hit("a")   // Ctrl + A, whatever Caps Lock is
        assertEquals(
            listOf("chord(${ChordKey.A},${listOf(ChordModifier.CTRL, ChordModifier.SHIFT)})", "chord(${ChordKey.A},${listOf(ChordModifier.CTRL)})"),
            port.calls.filter { it.startsWith("chord") },
        )
        assertEquals("", port.text.toString())
        // A chord between two up exits ends the wait as any key does.
        d.clearCaps()
        hit("a", up)
        d.onFunctionDown(FunctionKey.CTRL)
        hit("b")
        hit("s", up)
        assertFalse(d.capsLock)
        assertEquals("AS", port.text.toString())
    }

    @Test
    fun `CS-14 Dubeolsik keys and QWERTY digit keys never turn Caps Lock on`() {
        d.composer = kbd.hangul.compose.HangulComposer(kbd.hangul.compose.ComposePolicy.DUBEOLSIK)
        val dub = TapLayouts.DUBEOLSIK
        hit("q", up, dub)   // ㅂ key ↑ = ㅂ + ㅗ (FR-TAP-02h)
        hit("q", up, dub)
        assertFalse(d.capsLock)
        d.composer = null
        d.finishAll()
        hit("1", up)
        hit("2", up)
        assertFalse(d.capsLock)
        assertEquals(0, capsChanges)
    }

    @Test
    fun `CS-15 a cursor moved by the user ends the wait`() {
        hit("a", up)
        d.onCursorMoved(0, 0, -1, -1)
        hit("s", up)
        assertFalse(d.capsLock)
        assertEquals("AS", port.text.toString())
    }

    @Test
    fun `CS-16 an accessibility click is a tap - upper case while on, never a toggle`() {
        capsOn()
        d.onTapOutput(key(q, "a"), TapKeyResolver.resolve(key(q, "a"), TapGesture.Tap))
        assertEquals("A", port.text.toString())
        assertTrue(d.capsLock)
    }

    @Test
    fun `CS-18 a selection reported between the two ups - nothing is deleted, the toggle still happens`() {
        port.text.append("xyz")
        hit("a", up)
        cc.onUpdateSelection(1, 3, -1, -1)   // the user selected "yz" (⌫ would delete it all)
        hit("s", up)
        assertTrue(d.capsLock)
        assertEquals("xyzA", port.text.toString())
        assertFalse("del" in port.calls)
        cc.onUpdateSelection(4, 4, -1, -1)   // a cursor again: the next pair takes back as before
        hit("d", up)
        hit("f", up)
        assertFalse(d.capsLock)
        assertEquals("xyzA", port.text.toString())   // d typed and taken back
    }
}
