package kbd.app.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PW-01..PW-08 (LLD-11 §7.12 (사), LLD-12 §8.2, §8.3): plan writing and reads through [ComposingController], with a
 * fake port that returns a given text before the cursor and records calls.
 */
class ComposingEditsTest {

    /** Reads give what the test set; every call is recorded. */
    private class ReadingPort : EditorPort {
        val calls = mutableListOf<String>()
        var before: String? = null
        var after: String? = "x"
        var selection: Boolean? = false
        var throwOnDelete = false
        override val canReadText: Boolean get() = true
        override fun textBeforeCursor(n: Int): String? {
            calls += "before($n)"
            return before
        }
        override fun textAfterCursor(n: Int): String? {
            calls += "after($n)"
            return after
        }
        override fun hasSelection(): Boolean? {
            calls += "selection"
            return selection
        }
        override fun deleteBeforeCursor(n: Int) {
            if (throwOnDelete) throw IllegalStateException("delete")
            calls += "deleteBefore($n)"
        }
        override fun commitText(text: String) { calls += "commit($text)" }
        override fun setComposingText(text: String) { calls += "compose($text)" }
        override fun finishComposingText() { calls += "finish" }
        override fun deleteBefore() { calls += "del" }
        override fun sendKey(key: EditorKey) { calls += "key($key)" }
        override fun performEditorAction(actionId: Int) { calls += "action($actionId)" }
        override fun setSelection(start: Int, end: Int) { calls += "sel($start,$end)" }
        override fun beginBatch() { calls += "begin" }
        override fun endBatch() { calls += "end" }
    }

    /** Only the members a port had before the reads (the defaults stay). */
    private class OldPort : EditorPort {
        val calls = mutableListOf<String>()
        override fun commitText(text: String) { calls += "commit($text)" }
        override fun setComposingText(text: String) { calls += "compose($text)" }
        override fun finishComposingText() { calls += "finish" }
        override fun deleteBefore() { calls += "del" }
        override fun sendKey(key: EditorKey) { calls += "key($key)" }
        override fun performEditorAction(actionId: Int) { calls += "action($actionId)" }
        override fun setSelection(start: Int, end: Int) { calls += "sel($start,$end)" }
        override fun beginBatch() { calls += "begin" }
        override fun endBatch() { calls += "end" }
    }

    private val port = ReadingPort()
    private val cc = ComposingController(port)

    private val pick = listOf(EditorEdit.DeleteBefore(3), EditorEdit.Insert("Monday "))

    @Test
    fun `PW-01 pick - precondition read, then delete and insert in one batch`() {
        port.before = "mon"
        assertTrue(cc.applyEdits("mon", pick))
        assertEquals(listOf("before(3)", "begin", "deleteBefore(3)", "commit(Monday )", "end"), port.calls)
    }

    @Test
    fun `PW-02 precondition off in case, too short or unreadable - only the read, nothing written`() {
        for (b in listOf("mOn", "on", null)) {
            port.calls.clear()
            port.before = b
            assertFalse("$b", cc.applyEdits("mon", pick))
            assertEquals("$b", listOf("before(3)"), port.calls)
        }
    }

    @Test
    fun `PW-03 empty plan - no port call, then once`() {
        var then = 0
        assertTrue(cc.applyEdits("", emptyList()) { then++ })
        assertEquals(emptyList<String>(), port.calls)
        assertEquals(1, then)
    }

    @Test
    fun `PW-04 space removal before a period - delete and the usual path in one batch`() {
        port.before = " "
        assertTrue(cc.applyEdits(" ", listOf(EditorEdit.DeleteBefore(1))) { cc.commitText(".") })
        assertEquals(listOf("before(1)", "begin", "deleteBefore(1)", "commit(.)", "end"), port.calls)
    }

    @Test
    fun `PW-05 precondition off - then is not called`() {
        port.before = "x"
        var then = 0
        assertFalse(cc.applyEdits(" ", listOf(EditorEdit.DeleteBefore(1))) { then++ })
        assertEquals(listOf("before(1)"), port.calls)
        assertEquals(0, then)
    }

    @Test
    fun `PW-06 a throwing delete reaches the caller and the batch is closed`() {
        port.before = "mon"
        port.throwOnDelete = true
        assertThrows(IllegalStateException::class.java) { cc.applyEdits("mon", pick) }
        assertEquals(listOf("before(3)", "begin", "end"), port.calls)
    }

    @Test
    fun `PW-07 a port without reads - cannot read, reads null, delete of n is n backspace keys`() {
        val old = OldPort()
        assertFalse(old.canReadText)
        assertNull(old.textBeforeCursor(1))
        assertNull(old.textAfterCursor(1))
        assertNull(old.hasSelection())
        old.deleteBeforeCursor(3)
        assertEquals(listOf("del", "del", "del"), old.calls)
    }

    @Test
    fun `PW-08 controller reads pass the port's values through`() {
        port.before = "a"
        port.after = "b"
        port.selection = true
        assertTrue(cc.canReadText)
        assertEquals("a", cc.textBefore(1))
        assertEquals("b", cc.textAfter(1))
        assertEquals(true, cc.hasSelection())
        assertEquals(listOf("before(1)", "after(1)", "selection"), port.calls)
        assertFalse(ComposingController(OldPort()).canReadText)
    }
}
