package kbd.app.ime

import kbd.hangul.event.ComposeResult
import kbd.stroke.geom.Dir
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * OL-01..OL-15 (LLD-11 §7.12 (바), LLD-12 §8.4): the cursor ledger on its own (OL-01..OL-12) and through a
 * [ComposingController] with a recording fake port (OL-13..OL-15).
 */
class OwnEditLedgerTest {

    private val l = OwnEditLedger()

    /** [explains] for each collapsed cursor in [cursors]. */
    private fun notices(vararg cursors: Int) = cursors.map { l.explains(it, it) }

    @Test
    fun `OL-01 three commits, three notices - all own`() {
        l.start(5, 5)
        repeat(3) { l.committed(1) }
        assertEquals(listOf(true, true, true), notices(6, 7, 8))
    }

    @Test
    fun `OL-02 three commits in one notice, then the same place again - own, own`() {
        l.start(5, 5)
        repeat(3) { l.committed(1) }
        assertEquals(listOf(true, true), notices(8, 8))
    }

    @Test
    fun `OL-03 a letter typed right after a backspace is own, notices one by one or only the last`() {
        l.start(5, 5)
        l.committed(1); l.deletedKey(); l.committed(1)
        assertEquals(listOf(true, true, true), notices(6, 5, 6))

        val m = OwnEditLedger()
        m.start(5, 5)
        m.committed(1); m.deletedKey(); m.committed(1)
        assertTrue(m.explains(6, 6))
    }

    @Test
    fun `OL-04 repeated backspace - own, notices one by one or only the last`() {
        l.start(10, 10)
        repeat(4) { l.deletedKey() }
        assertEquals(listOf(true, true, true, true), notices(9, 8, 7, 6))

        val m = OwnEditLedger()
        m.start(10, 10)
        repeat(4) { m.deletedKey() }
        assertTrue(m.explains(6, 6))
    }

    @Test
    fun `OL-05 pick - delete the word and commit the whole word, one notice or two`() {
        l.start(3, 3)
        l.deletedBefore(3); l.committed(7)
        assertEquals(listOf(true), notices(7))

        val m = OwnEditLedger()
        m.start(3, 3)
        m.deletedBefore(3); m.committed(7)
        assertTrue(m.explains(0, 0))
        assertTrue(m.explains(7, 7))
    }

    @Test
    fun `OL-06 a user touch is not own and resyncs the ledger`() {
        l.start(5, 5)
        l.committed(1)
        assertTrue(l.explains(6, 6))
        assertFalse(l.explains(2, 2))
        l.committed(1)
        assertTrue(l.explains(3, 3))
    }

    @Test
    fun `OL-07 a selection is not own, backspace over it goes to its start, then a commit`() {
        l.start(5, 5)
        assertFalse(l.explains(2, 5))
        l.deletedKey()
        assertTrue(l.explains(2, 2))
        l.committed(1)
        assertTrue(l.explains(3, 3))
    }

    @Test
    fun `OL-08 after an arrow the first notice is its result and the commits made meanwhile are replayed`() {
        l.start(5, 5)
        l.unknown()
        l.committed(1)
        assertTrue(l.explains(4, 4))
        assertTrue(l.explains(5, 5))
        assertFalse(l.explains(1, 1))
    }

    @Test
    fun `OL-09 arrow, typing, touch - unknown does not carry into typing`() {
        l.start(5, 5)
        l.unknown()
        repeat(3) { l.committed(1) }
        assertEquals(listOf(true, true, true, true), notices(2, 3, 4, 5))
        assertFalse(l.explains(0, 0))

        val m = OwnEditLedger()
        m.unknown()
        assertTrue(m.explains(7, 7))
        m.committed(1)
        assertTrue(m.explains(8, 8))
        assertFalse(m.explains(3, 3))
    }

    @Test
    fun `OL-10 no start value - the first notice resyncs, then commits are known`() {
        l.start(-1, -1)
        assertTrue(l.explains(4, 4))
        l.committed(1)
        assertTrue(l.explains(5, 5))
    }

    @Test
    fun `OL-11 backspace at the start and a delete past the start - unknown, not awaiting, kept with a write in flight`() {
        l.start(0, 0)
        l.deletedKey()
        assertTrue(l.explains(0, 0))

        val m = OwnEditLedger()
        m.start(2, 2)
        m.deletedBefore(3)
        assertTrue(m.explains(0, 0))
    }

    @Test
    fun `OL-12 a notice off the expected cursor resets, and the late expected one is no longer own`() {
        l.start(5, 5)
        l.committed(1)
        assertFalse(l.explains(9, 9))
        assertFalse(l.explains(6, 6))
    }

    /** Records port calls; enough of a port for the controller. */
    private class FakePort : EditorPort {
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

    private val port = FakePort()
    private val cc = ComposingController(port)

    /** The existing verdict first, then the ledger, as the service calls them. */
    private fun notice(s: Int, e: Int, candS: Int = -1, candE: Int = -1): Pair<SelectionVerdict, Boolean> {
        val v = cc.onUpdateSelection(s, e, candS, candE)
        return v to cc.isOwnCursorChange(s, e)
    }

    @Test
    fun `OL-13 controller - a letter after backspace is own to the ledger, the old verdict is unchanged`() {
        cc.onStart(5, 5)
        cc.commitText("a")
        cc.deleteBefore()
        cc.commitText("b")
        assertEquals(
            listOf(
                SelectionVerdict.OWN to true,
                SelectionVerdict.OTHER_MOVED to true,
                SelectionVerdict.OTHER_MOVED to true,
            ),
            listOf(notice(6, 6), notice(5, 5), notice(6, 6)),
        )
    }

    @Test
    fun `OL-14 controller - composing and an arrow are unknown writes whose next notice is their result`() {
        cc.show(ComposeResult("", "가"))
        assertTrue(notice(6, 6, 5, 6).second)

        val other = ComposingController(FakePort())
        other.arrow(Dir.LEFT)
        assertTrue(other.isOwnCursorChange(4, 4))
        other.commitText("a")
        assertTrue(other.isOwnCursorChange(5, 5))
        assertFalse(other.isOwnCursorChange(9, 9))
    }

    @Test
    fun `OL-15 controller - a plan insert is own, an enter action is unknown`() {
        cc.onStart(5, 5)
        assertTrue(cc.applyEdits("", listOf(EditorEdit.Insert("ab"))))
        assertTrue(notice(7, 7).second)

        cc.enter(FieldInfo(FieldClass.TEXT, isPassword = false, noLearning = false, multiLine = false, enterAction = 3))
        assertTrue(notice(0, 0).second)
    }
}
