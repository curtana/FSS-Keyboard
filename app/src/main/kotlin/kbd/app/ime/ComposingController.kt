package kbd.app.ime

import kbd.hangul.event.ComposeResult
import kbd.stroke.geom.Dir
import kbd.stroke.tap.ChordKey
import kbd.stroke.tap.ChordModifier

/** Cursor-move verdict for `onUpdateSelection` (LLD-08 §4.1). */
enum class SelectionVerdict { OWN, USER_MOVED, OTHER_MOVED }

/**
 * Writes composer output to the editor and judges cursor-move notifications (LLD-08 §4, §4.1).
 * Callers finish composing before [commitText], [arrow], [tab], [enter] and [chordKey].
 * If a port call throws, an open batch is still closed and state changes only for the calls that returned (issue #89).
 */
class ComposingController(private val port: EditorPort) {

    /** Composing text currently shown in the editor, including the temporary glyph. */
    var composingShown: String = ""
        private set

    /** Temporary glyph at the end of [composingShown] (HLD 5, FR-HGL-04e). */
    private var glyph = ""

    /** [composingShown] without the glyph: the composer's text, restored when the glyph goes (FR-HGL-04e). */
    private var bare = ""

    /** Cursor expected after our last write, −1 = unknown. */
    private var predictedSel = -1

    /** Start of the composing span, −1 = none. */
    private var composeStart = -1

    /** Expected cursors of our writes not yet confirmed by an update (D-29). */
    private val expected = ArrayDeque<Int>()

    /** Cursor ledger for word suggestions (LLD-12 §8.4); every write below reports to it. The verdict above never reads it. */
    private val ledger = OwnEditLedger()

    /** Whether the port can read the editor's text (LLD-12 §8.3). */
    val canReadText: Boolean get() = port.canReadText

    fun textBefore(n: Int): String? = port.textBeforeCursor(n)

    fun textAfter(n: Int): String? = port.textAfterCursor(n)

    fun hasSelection(): Boolean? = port.hasSelection()

    /** [pendingGlyph]: the graffiti cheonjiin's temporary ㆍ / ‥ after the composition (FR-HGL-04e); dropping it restores the text without it. */
    fun show(r: ComposeResult, pendingGlyph: String = "") {
        val base = r.composing
        val d = base + pendingGlyph
        val writes = r.committed.isNotEmpty() || d.isNotEmpty() || composingShown.isNotEmpty()
        if (writes) ledger.unknown()
        batch {
            if (r.committed.isNotEmpty()) {
                port.commitText(r.committed)
                expectCommit(r.committed)
            }
            if (d.isNotEmpty()) {
                port.setComposingText(d)
                expectComposing(d)
            } else if (composingShown.isNotEmpty()) {
                port.setComposingText("")
                expectComposing("")
            }
        }
        composingShown = d
        glyph = pendingGlyph
        bare = base
        if (d.isEmpty()) composeStart = -1
    }

    fun finishComposing() {
        if (composingShown.isEmpty()) return
        if (glyph.isNotEmpty()) dropGlyph()
        port.finishComposingText()
        clearComposing()
    }

    /**
     * Finishes composing after the user moved the cursor to ([newS], [newE]) while the span was [candS]..[candE].
     * With no reported span ([candE] = −1) the last known span end is used; if none is known, nothing shifts (G13).
     */
    fun finishAfterUserMove(newS: Int, newE: Int, candS: Int, candE: Int) {
        val spanEnd = when {
            candE != -1 -> candE
            composeStart != -1 -> composeStart + composingShown.length
            else -> -1
        }
        ledger.unknown()
        val g = composingShown.length - bare.length   // the glyph's share of the span (0 when "‥" stands for ㅣ / ㅡ)
        val restoredS = restore(newS, spanEnd, g)
        val restoredE = restore(newE, spanEnd, g)
        if (glyph.isEmpty()) {
            port.finishComposingText()
        } else {
            batch {
                port.setComposingText(bare)   // cursor goes to the span end
                composingShown = bare
                glyph = ""
                port.finishComposingText()
                port.setSelection(restoredS, restoredE)
            }
        }
        clearComposing()
        predictedSel = restoredE
        expected.clear()
    }

    fun commitText(text: String) {
        port.commitText(text)
        expectCommit(text)
        ledger.committed(text.length)
    }

    /** Deletes one character, or the selection (FR-CMN-07). The resulting cursor is unknown. */
    fun deleteBefore() {
        port.deleteBefore()
        predictedSel = -1
        ledger.deletedKey()
    }

    fun arrow(d: Dir) {
        port.sendKey(
            when (d) {
                Dir.LEFT -> EditorKey.DPAD_LEFT
                Dir.RIGHT -> EditorKey.DPAD_RIGHT
                Dir.UP -> EditorKey.DPAD_UP
                Dir.DOWN -> EditorKey.DPAD_DOWN
            },
        )
        predictedSel = -1
        ledger.unknown()
    }

    /** Esc or forward Del key event (issue #198); like an arrow, the resulting cursor is unknown. */
    fun editKey(k: EditorKey) {
        port.sendKey(k)
        predictedSel = -1
        ledger.unknown()
    }

    /**
     * One key with Ctrl / Alt (and Shift) as key events (FSS#98, FR-TAP-16); the caller finished composing. What the
     * app does with it is unknown, so like an arrow the resulting cursor is unknown.
     */
    fun chordKey(key: ChordKey, mods: Set<ChordModifier>) {
        port.sendChord(key, mods)
        predictedSel = -1
        ledger.unknown()
    }

    fun tab() = commitText("\t")

    fun enter(f: FieldInfo) {
        val action = f.enterAction
        when {
            action != null -> {
                port.performEditorAction(action)
                predictedSel = -1
                ledger.unknown()
            }
            f.multiLine -> commitText("\n")
            else -> {
                port.sendKey(EditorKey.ENTER)
                predictedSel = -1
                ledger.unknown()
            }
        }
    }

    fun onStart(selStart: Int, selEnd: Int) {
        predictedSel = selEnd
        composeStart = -1
        expected.clear()
        composingShown = ""
        glyph = ""
        bare = ""
        ledger.start(selStart, selEnd)
    }

    fun onUpdateSelection(newS: Int, newE: Int, candS: Int, candE: Int): SelectionVerdict {
        val collapsed = newS == newE
        if (collapsed) {
            val i = expected.indexOf(newE)
            if (i >= 0) {
                repeat(i + 1) { expected.removeFirst() }
                return SelectionVerdict.OWN
            }
        }
        if (composingShown.isNotEmpty()) {
            if (collapsed && candE != -1 && newE == candE &&
                expected.isEmpty() && (predictedSel == -1 || newE == predictedSel)
            ) {
                predictedSel = newE
                // Resync after an unknown cursor: the span start was never set (G12).
                if (composeStart == -1) composeStart = candS
                return SelectionVerdict.OWN
            }
            // Off the span end, a selection, or the span end but not where we expected (HLD 8).
            // composeStart is kept: finishAfterUserMove needs the known span end when candE is -1 (G13).
            predictedSel = newE
            expected.clear()
            return SelectionVerdict.USER_MOVED
        }
        val before = predictedSel
        predictedSel = newE
        expected.clear()
        return if (newE != before) SelectionVerdict.OTHER_MOVED else SelectionVerdict.OWN
    }

    /**
     * Precondition check, then [edits] and [then] in one batch (LLD-12 §8.3, D-84). If the text before the cursor is
     * not exactly [expectBefore] (or cannot be read), nothing is written, [then] is not called, and the result is
     * false. Empty [edits]: just [then], no batch, true. Called only while nothing is composing.
     */
    fun applyEdits(expectBefore: String, edits: List<EditorEdit>, then: () -> Unit = {}): Boolean {
        if (expectBefore.isNotEmpty() && port.textBeforeCursor(expectBefore.length) != expectBefore) return false
        if (edits.isEmpty()) {
            then()
            return true
        }
        batch {
            for (ed in edits) {
                when (ed) {
                    is EditorEdit.DeleteBefore -> {
                        port.deleteBeforeCursor(ed.count)
                        predictedSel = -1   // like deleteBefore: the cursor is unknown to the verdict
                        ledger.deletedBefore(ed.count)
                    }
                    is EditorEdit.Insert -> commitText(ed.text)
                }
            }
            then()
        }
        return true
    }

    /** Whether the cursor notice is explained by the keyboard's writes (LLD-12 §8.4). Once per notice, after [onUpdateSelection]. */
    fun isOwnCursorChange(newS: Int, newE: Int): Boolean = ledger.explains(newS, newE)

    /** A user position after the temporary glyph ([g] chars) is gone: positions at or past the span end shift left; −1 = unknown end, no shift. */
    private fun restore(x: Int, spanEnd: Int, g: Int) = if (spanEnd != -1 && x >= spanEnd) x - g else x

    /** Runs [block] between beginBatch and endBatch; endBatch runs even if a port call throws (issue #89). */
    private inline fun batch(block: () -> Unit) {
        port.beginBatch()
        try {
            block()
        } finally {
            port.endBatch()
        }
    }

    /** Takes the temporary glyph out of the editor's span; once that write returns, the glyph is gone from our state too. */
    private fun dropGlyph() {
        ledger.unknown()
        val withoutGlyph = bare
        port.setComposingText(withoutGlyph)
        expectComposing(withoutGlyph)
        composingShown = withoutGlyph
        glyph = ""
    }

    private fun clearComposing() {
        composingShown = ""
        glyph = ""
        bare = ""
        composeStart = -1
    }

    private fun expectComposing(d: String) {
        if (predictedSel == -1) return
        if (composeStart == -1) composeStart = predictedSel
        push(composeStart + d.length)
    }

    private fun expectCommit(t: String) {
        if (predictedSel == -1) {
            composeStart = -1
            return
        }
        val base = if (composeStart != -1) composeStart else predictedSel
        composeStart = -1
        push(base + t.length)
    }

    private fun push(cursor: Int) {
        expected.addLast(cursor)
        predictedSel = cursor
    }
}
