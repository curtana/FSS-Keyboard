package kbd.app.ime

import kbd.hangul.event.ComposeResult
import kbd.stroke.geom.Dir
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * CC-01..CC-22 (LLD-11 §7.3, LLD-08 §4, §4.1) with a recording fake [EditorPort], and port-exception cleanup
 * (CC-20..CC-22, issue #89: the batch is always closed, a glyph already taken out of the editor is not left in composingShown).
 */
class ComposingControllerTest {

    private class FakePort : EditorPort {
        val calls = mutableListOf<String>()
        var throwOnCompose = false
        var throwOnFinish = false
        override fun commitText(text: String) { calls += "commit($text)" }
        override fun setComposingText(text: String) {
            if (throwOnCompose) throw IllegalStateException("compose")
            calls += "compose($text)"
        }
        override fun finishComposingText() {
            if (throwOnFinish) throw IllegalStateException("finish")
            calls += "finish"
        }
        override fun deleteBefore() { calls += "del" }
        override fun sendKey(key: EditorKey) { calls += "key($key)" }
        override fun performEditorAction(actionId: Int) { calls += "action($actionId)" }
        override fun setSelection(start: Int, end: Int) { calls += "sel($start,$end)" }
        override fun beginBatch() { calls += "begin" }
        override fun endBatch() { calls += "end" }
    }

    private val port = FakePort()
    private val cc = ComposingController(port)

    private fun res(committed: String, composing: String) = ComposeResult(committed, composing)

    private fun field(multiLine: Boolean = false, enterAction: Int? = null) =
        FieldInfo(FieldClass.TEXT, isPassword = false, noLearning = false, multiLine = multiLine, enterAction = enterAction)

    @Test
    fun `CC-01 show composing only sets composing text`() {
        cc.show(res("", "가"))
        assertEquals(listOf("begin", "compose(가)", "end"), port.calls)
        assertEquals("가", cc.composingShown)
    }

    @Test
    fun `CC-02 show committed and composing in one batch`() {
        cc.show(res("가", "까"))
        assertEquals(listOf("begin", "commit(가)", "compose(까)", "end"), port.calls)
        assertEquals("까", cc.composingShown)
    }

    @Test
    fun `CC-03 show empty after composing clears the span`() {
        cc.show(res("", "가"))
        port.calls.clear()
        cc.show(res("", ""))
        assertEquals(listOf("begin", "compose()", "end"), port.calls)
        assertEquals("", cc.composingShown)
    }

    @Test
    fun `CC-03 show empty with nothing composing writes nothing`() {
        cc.show(res("", ""))
        assertEquals(listOf("begin", "end"), port.calls)
    }

    @Test
    fun `CC-04 show appends the temporary glyph`() {
        cc.show(res("", "ㄱ"), "ㆍ")
        assertEquals(listOf("begin", "compose(ㄱㆍ)", "end"), port.calls)
        assertEquals("ㄱㆍ", cc.composingShown)
    }

    @Test
    fun `CC-05 show glyph alone`() {
        cc.show(res("", ""), "ㆍ")
        assertEquals(listOf("begin", "compose(ㆍ)", "end"), port.calls)
        assertEquals("ㆍ", cc.composingShown)
    }

    @Test
    fun `CC-06 finishComposing while composing finishes once`() {
        cc.show(res("", "가"))
        port.calls.clear()
        cc.finishComposing()
        assertEquals(listOf("finish"), port.calls)
        assertEquals("", cc.composingShown)
    }

    @Test
    fun `CC-06 finishComposing with nothing composing does nothing`() {
        cc.finishComposing()
        cc.show(res("", "가"))
        cc.finishComposing()
        port.calls.clear()
        cc.finishComposing()
        assertEquals(emptyList<String>(), port.calls)
    }

    @Test
    fun `CC-07 enter with action performs the editor action`() {
        cc.enter(field(enterAction = 3))
        assertEquals(listOf("action(3)"), port.calls)
    }

    @Test
    fun `CC-07 enter in multi-line field commits newline`() {
        cc.enter(field(multiLine = true))
        assertEquals(listOf("commit(\n)"), port.calls)
    }

    @Test
    fun `CC-07 enter with action and multi-line prefers the action`() {
        cc.enter(field(multiLine = true, enterAction = 6))
        assertEquals(listOf("action(6)"), port.calls)
    }

    @Test
    fun `CC-07 enter in single-line field without action sends ENTER key`() {
        cc.enter(field())
        assertEquals(listOf("key(ENTER)"), port.calls)
    }

    @Test
    fun `CC-08 tab commits a tab character`() {
        cc.tab()
        assertEquals(listOf("commit(\t)"), port.calls)
    }

    @Test
    fun `CC-09 update at the expected cursor after show is OWN`() {
        cc.onStart(5, 5)
        cc.show(res("", "가"))
        assertEquals(SelectionVerdict.OWN, cc.onUpdateSelection(6, 6, 5, 6))
    }

    @Test
    fun `CC-10 two late updates after two writes are both OWN`() {
        cc.onStart(5, 5)
        cc.show(res("", "가"))
        cc.show(res("", "각"))
        assertEquals(SelectionVerdict.OWN, cc.onUpdateSelection(6, 6, 5, 6))
        assertEquals(SelectionVerdict.OWN, cc.onUpdateSelection(6, 6, 5, 6))
    }

    @Test
    fun `CC-10 late updates after commit and new composing are OWN`() {
        cc.onStart(5, 5)
        cc.show(res("", "가"))
        cc.show(res("가", "까"))   // commit 5..6, new span 6..7
        assertEquals(SelectionVerdict.OWN, cc.onUpdateSelection(6, 6, 5, 6))
        assertEquals(SelectionVerdict.OWN, cc.onUpdateSelection(7, 7, 6, 7))
    }

    @Test
    fun `CC-10 a later expected cursor drops the earlier ones`() {
        cc.onStart(5, 5)
        cc.show(res("", "가"))
        cc.show(res("가", "까"))   // expected [6, 6, 7]
        assertEquals(SelectionVerdict.OWN, cc.onUpdateSelection(7, 7, 6, 7))
        // The skipped 6s are gone: a report at 6 now is the user's move.
        assertEquals(SelectionVerdict.USER_MOVED, cc.onUpdateSelection(6, 6, 6, 7))
    }

    @Test
    fun `CC-11 cursor moved off the span while composing is USER_MOVED`() {
        cc.onStart(5, 5)
        cc.show(res("", "가"))
        assertEquals(SelectionVerdict.USER_MOVED, cc.onUpdateSelection(2, 2, 5, 6))
    }

    @Test
    fun `CC-12 selection while composing is USER_MOVED`() {
        cc.onStart(5, 5)
        cc.show(res("", "가"))
        assertEquals(SelectionVerdict.USER_MOVED, cc.onUpdateSelection(3, 5, 5, 6))
    }

    @Test
    fun `CC-12 selection ending at the expected cursor is still USER_MOVED`() {
        cc.onStart(5, 5)
        cc.show(res("", "가"))
        assertEquals(SelectionVerdict.USER_MOVED, cc.onUpdateSelection(3, 6, 5, 6))
    }

    @Test
    fun `CC-13 update without a composing span while composing is USER_MOVED`() {
        cc.onStart(5, 5)
        cc.show(res("", "가"))
        assertEquals(SelectionVerdict.OWN, cc.onUpdateSelection(6, 6, 5, 6))
        assertEquals(SelectionVerdict.USER_MOVED, cc.onUpdateSelection(6, 6, -1, -1))
    }

    @Test
    fun `CC-14 cursor moved elsewhere with nothing composing is OTHER_MOVED`() {
        cc.onStart(5, 5)
        assertEquals(SelectionVerdict.OTHER_MOVED, cc.onUpdateSelection(2, 2, -1, -1))
        assertEquals(SelectionVerdict.OWN, cc.onUpdateSelection(2, 2, -1, -1))
    }

    @Test
    fun `CC-15 finishComposing removes the temporary glyph first`() {
        cc.show(res("", "ㄱ"), "ㆍ")
        port.calls.clear()
        cc.finishComposing()
        assertEquals(listOf("compose(ㄱ)", "finish"), port.calls)
        assertEquals("", cc.composingShown)
    }

    @Test
    fun `CC-15 finishComposing of a glyph alone clears the span`() {
        cc.show(res("", ""), "ㆍ")
        port.calls.clear()
        cc.finishComposing()
        assertEquals(listOf("compose()", "finish"), port.calls)
    }

    @Test
    fun `CC-16 user moved after the span shifts back by the glyph length`() {
        cc.onStart(5, 5)
        cc.show(res("", "가"), "ㆍ")
        assertEquals(SelectionVerdict.USER_MOVED, cc.onUpdateSelection(9, 9, 5, 7))
        port.calls.clear()
        cc.finishAfterUserMove(9, 9, 5, 7)
        assertEquals(listOf("begin", "compose(가)", "finish", "sel(8,8)", "end"), port.calls)
        assertEquals("", cc.composingShown)
        // predictedSel = 8, so the editor's report of the restored cursor is not a new move.
        assertEquals(SelectionVerdict.OWN, cc.onUpdateSelection(8, 8, -1, -1))
    }

    @Test
    fun `CC-16 user moved before the span keeps the position`() {
        cc.onStart(5, 5)
        cc.show(res("", "가"), "ㆍ")
        assertEquals(SelectionVerdict.USER_MOVED, cc.onUpdateSelection(2, 2, 5, 7))
        port.calls.clear()
        cc.finishAfterUserMove(2, 2, 5, 7)
        assertEquals(listOf("begin", "compose(가)", "finish", "sel(2,2)", "end"), port.calls)
    }

    @Test
    fun `CC-16 user selection across the span end shifts only the end`() {
        cc.show(res("", "가"), "ㆍ")
        port.calls.clear()
        cc.finishAfterUserMove(3, 9, 5, 7)
        assertEquals(listOf("begin", "compose(가)", "finish", "sel(3,8)", "end"), port.calls)
    }

    /** SWIPE 0.36 3.4 (#192, LLD D-64): the swipe bare ‥ is shown in place of the composer's ㅣ, which finishing restores. */
    @Test
    fun `CC-23 a glyph that replaces the last character - shown in its place, restored on finish and on a user move`() {
        cc.show(res("", "ㅣ"), "‥", replacesLast = true)
        assertEquals(listOf("begin", "compose(‥)", "end"), port.calls)
        assertEquals("‥", cc.composingShown)
        port.calls.clear()
        cc.finishComposing()
        assertEquals(listOf("compose(ㅣ)", "finish"), port.calls)
        cc.onStart(5, 5)
        cc.show(res("", "ㅣ"), "‥", replacesLast = true)
        port.calls.clear()
        cc.finishAfterUserMove(9, 9, 5, 6)
        assertEquals(listOf("begin", "compose(ㅣ)", "finish", "sel(9,9)", "end"), port.calls)   // same length: no shift
        cc.show(res("", "ㅣ"), "", replacesLast = true)                                         // no glyph: nothing replaced
        assertEquals("ㅣ", cc.composingShown)
    }

    @Test
    fun `CC-16 user moved without glyph only finishes`() {
        cc.show(res("", "가"))
        port.calls.clear()
        cc.finishAfterUserMove(2, 2, 5, 6)
        assertEquals(listOf("finish"), port.calls)
        assertEquals("", cc.composingShown)
    }

    @Test
    fun `CC-17 new composing after finishComposing starts at the finished end`() {
        cc.onStart(5, 5)
        cc.show(res("", "가"))
        assertEquals(SelectionVerdict.OWN, cc.onUpdateSelection(6, 6, 5, 6))
        cc.finishComposing()
        cc.show(res("", "나"))
        assertEquals(SelectionVerdict.OWN, cc.onUpdateSelection(7, 7, 6, 7))
    }

    @Test
    fun `CC-18 first composing after an arrow resyncs the span start`() {
        cc.onStart(5, 5)
        cc.arrow(Dir.LEFT)                                   // cursor unknown
        cc.show(res("", "가"))
        assertEquals(SelectionVerdict.OWN, cc.onUpdateSelection(5, 5, 4, 5))
        cc.show(res("", "각"))
        assertEquals(SelectionVerdict.OWN, cc.onUpdateSelection(5, 5, 4, 5))
        cc.show(res("각", "ㅅ"))
        assertEquals(SelectionVerdict.OWN, cc.onUpdateSelection(6, 6, 5, 6))
    }

    @Test
    fun `CC-18 first composing after deleteBefore resyncs the span start`() {
        cc.onStart(5, 5)
        cc.deleteBefore()
        cc.show(res("", "가"))
        assertEquals(SelectionVerdict.OWN, cc.onUpdateSelection(5, 5, 4, 5))
        cc.show(res("", "각"))
        assertEquals(SelectionVerdict.OWN, cc.onUpdateSelection(5, 5, 4, 5))
        // A real move after the resync is still caught.
        assertEquals(SelectionVerdict.USER_MOVED, cc.onUpdateSelection(1, 1, 4, 5))
    }

    @Test
    fun `CC-19 user moved before the span with no reported span keeps the position`() {
        cc.onStart(5, 5)
        cc.show(res("", "가"), "ㆍ")                     // span 5..7 known
        assertEquals(SelectionVerdict.USER_MOVED, cc.onUpdateSelection(2, 2, -1, -1))
        port.calls.clear()
        cc.finishAfterUserMove(2, 2, -1, -1)
        assertEquals(listOf("begin", "compose(가)", "finish", "sel(2,2)", "end"), port.calls)
    }

    @Test
    fun `CC-19 user moved after the span with no reported span uses the known span end`() {
        cc.onStart(5, 5)
        cc.show(res("", "가"), "ㆍ")                     // span 5..7 known
        assertEquals(SelectionVerdict.USER_MOVED, cc.onUpdateSelection(9, 9, -1, -1))
        port.calls.clear()
        cc.finishAfterUserMove(9, 9, -1, -1)
        assertEquals(listOf("begin", "compose(가)", "finish", "sel(8,8)", "end"), port.calls)
        assertEquals(SelectionVerdict.OWN, cc.onUpdateSelection(8, 8, -1, -1))
    }

    @Test
    fun `CC-19 user moved with no reported and no known span does not shift`() {
        cc.onStart(5, 5)
        cc.arrow(Dir.RIGHT)                                  // cursor unknown, span start unknown
        cc.show(res("", "가"), "ㆍ")
        assertEquals(SelectionVerdict.USER_MOVED, cc.onUpdateSelection(9, 9, -1, -1))
        port.calls.clear()
        cc.finishAfterUserMove(9, 9, -1, -1)
        assertEquals(listOf("begin", "compose(가)", "finish", "sel(9,9)", "end"), port.calls)
    }

    @Test
    fun `arrow sends the DPAD key and forgets the cursor`() {
        cc.onStart(5, 5)
        cc.arrow(Dir.LEFT)
        cc.arrow(Dir.RIGHT)
        cc.arrow(Dir.UP)
        cc.arrow(Dir.DOWN)
        assertEquals(
            listOf("key(DPAD_LEFT)", "key(DPAD_RIGHT)", "key(DPAD_UP)", "key(DPAD_DOWN)"),
            port.calls,
        )
        assertEquals(SelectionVerdict.OTHER_MOVED, cc.onUpdateSelection(4, 4, -1, -1))
    }

    @Test
    fun `deleteBefore sends DEL and forgets the cursor`() {
        cc.onStart(5, 5)
        cc.deleteBefore()
        assertEquals(listOf("del"), port.calls)
        assertEquals(SelectionVerdict.OTHER_MOVED, cc.onUpdateSelection(4, 4, -1, -1))
    }

    @Test
    fun `commitText queues its expected cursor`() {
        cc.onStart(5, 5)
        cc.commitText("ab")
        assertEquals(listOf("commit(ab)"), port.calls)
        assertEquals(SelectionVerdict.OWN, cc.onUpdateSelection(7, 7, -1, -1))
    }

    @Test
    fun `onStart clears the shown composing text`() {
        cc.show(res("", "가"), "ㆍ")
        cc.onStart(0, 0)
        assertEquals("", cc.composingShown)
        port.calls.clear()
        cc.finishComposing()
        assertEquals(emptyList<String>(), port.calls)
    }

    // ---- port exceptions (issue #89, NFR-03) ----

    @Test
    fun `CC-20 show closes the batch when a write throws`() {
        cc.show(res("", "가"))
        port.calls.clear()
        port.throwOnCompose = true
        assertThrows(IllegalStateException::class.java) { cc.show(res("", "각")) }
        assertEquals(listOf("begin", "end"), port.calls)
        assertEquals("가", cc.composingShown)   // state changes only after the port calls return
    }

    @Test
    fun `CC-21 finishComposing failure after the glyph is taken out leaves no glyph in composingShown`() {
        cc.show(res("", "가"), "ㆍ")
        port.calls.clear()
        port.throwOnFinish = true
        assertThrows(IllegalStateException::class.java) { cc.finishComposing() }
        assertEquals(listOf("compose(가)"), port.calls)
        assertEquals("가", cc.composingShown)
        // The next finish does not shorten the span again: 가 is committed as it is.
        port.throwOnFinish = false
        port.calls.clear()
        cc.finishComposing()
        assertEquals(listOf("finish"), port.calls)
        assertEquals("", cc.composingShown)
    }

    @Test
    fun `CC-22 finishAfterUserMove failure closes the batch and leaves no glyph in composingShown`() {
        cc.onStart(5, 5)
        cc.show(res("", "가"), "ㆍ")
        port.calls.clear()
        port.throwOnFinish = true
        assertThrows(IllegalStateException::class.java) { cc.finishAfterUserMove(2, 2, 5, 7) }
        assertEquals(listOf("begin", "compose(가)", "end"), port.calls)
        assertEquals("가", cc.composingShown)
    }
}
