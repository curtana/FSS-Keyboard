package kbd.app.ime

import java.io.File
import kbd.app.view.BarCell
import kbd.suggest.Candidate
import kbd.suggest.EditCommand
import kbd.suggest.EditPlan
import kbd.suggest.KeyAnswer
import kbd.suggest.OnFail
import kbd.suggest.ResetReason
import kbd.suggest.SuggestKey
import kbd.suggest.SuggestionSource
import kbd.suggest.TextPeek
import kbd.suggest.en.EnglishWordSource
import kbd.suggest.en.WordDictionary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** The test dictionary of LLD-11 §7.12 (`core-suggest/src/test/resources/suggest/mini_words.txt`), read from its file. */
internal object MiniWords {
    private val file: File = listOf("../core-suggest", "core-suggest")
        .map { File(it, "src/test/resources/suggest/mini_words.txt") }
        .first { it.isFile }

    fun load(): WordDictionary = file.reader(Charsets.UTF_8).use { WordDictionary.read(it) }
}

/**
 * An editor whose text is a buffer with the cursor at its end: commits append, deletes take from the end, reads give
 * the end. [readable] false: every read is null while `canReadText` stays true (LLD-11 SU-16). Calls are recorded.
 */
internal class BufferEditor(text: String = "", private val readable: Boolean = true) : EditorPort {
    val text = StringBuilder(text)
    val calls = mutableListOf<String>()
    override val canReadText: Boolean get() = true
    override fun textBeforeCursor(n: Int): String? {
        calls += "before($n)"
        return if (readable) text.substring(maxOf(0, text.length - n)) else null
    }
    override fun textAfterCursor(n: Int): String? {
        calls += "after($n)"
        return if (readable) "" else null
    }
    override fun hasSelection(): Boolean? {
        calls += "selection"
        return if (readable) false else null
    }
    override fun deleteBeforeCursor(n: Int) {
        calls += "deleteBefore($n)"
        text.setLength(maxOf(0, text.length - n))
    }
    override fun commitText(text: String) {
        calls += "commit($text)"
        this.text.append(text)
    }
    override fun setComposingText(text: String) { calls += "compose($text)" }
    override fun finishComposingText() { calls += "finish" }
    override fun deleteBefore() {
        calls += "del"
        if (text.isNotEmpty()) text.setLength(text.length - 1)
    }
    override fun sendKey(key: EditorKey) { calls += "key($key)" }
    override fun performEditorAction(actionId: Int) { calls += "action($actionId)" }
    override fun setSelection(start: Int, end: Int) { calls += "sel($start,$end)" }
    override fun beginBatch() { calls += "begin" }
    override fun endBatch() { calls += "end" }
}

/**
 * LLD-11 §7.12 (타) SU-01..SU-16: [SuggestionController] (LLD-12 §12, §6.1) with a fake provider that answers what the
 * test sets and records its calls in one log with the editor's calls, `normal` and the bar's cells.
 */
class SuggestionControllerTest {

    private val log = mutableListOf<String>()

    /** Reads give what the test set; every call goes to [log]. */
    private inner class LogPort : EditorPort {
        var canRead = true
        var before: String? = ""
        var throwOnDelete = false
        override val canReadText: Boolean get() = canRead
        override fun textBeforeCursor(n: Int): String? {
            log += "before($n)"
            return before
        }
        override fun textAfterCursor(n: Int): String? {
            log += "after($n)"
            return ""
        }
        override fun hasSelection(): Boolean? {
            log += "selection"
            return false
        }
        override fun deleteBeforeCursor(n: Int) {
            if (throwOnDelete) throw IllegalStateException("delete")
            log += "deleteBefore($n)"
        }
        override fun commitText(text: String) { log += "commit($text)" }
        override fun setComposingText(text: String) { log += "compose($text)" }
        override fun finishComposingText() { log += "finish" }
        override fun deleteBefore() { log += "del" }
        override fun sendKey(key: EditorKey) { log += "key($key)" }
        override fun performEditorAction(actionId: Int) { log += "action($actionId)" }
        override fun setSelection(start: Int, end: Int) { log += "sel($start,$end)" }
        override fun beginBatch() { log += "begin" }
        override fun endBatch() { log += "end" }
    }

    /** Answers [answer]; [throwIn] names the calls that throw. */
    private inner class FakeSource : SuggestionSource {
        var answer: KeyAnswer = KeyAnswer.Pass
        var plan: EditPlan? = null
        var cells: List<Candidate> = emptyList()
        val throwIn = HashSet<String>()
        var resets = mutableListOf<ResetReason>()

        override val candidates: List<Candidate>
            get() {
                if ("candidates" in throwIn) throw IllegalStateException("candidates")
                return cells
            }

        override fun onKey(key: SuggestKey, peek: TextPeek): KeyAnswer {
            log += "onKey($key)"
            if ("onKey" in throwIn) throw IllegalStateException("onKey")
            return answer
        }

        override fun afterKey(key: SuggestKey) {
            log += "afterKey($key)"
            if ("afterKey" in throwIn) throw IllegalStateException("afterKey")
        }

        override fun pick(index: Int): EditPlan? {
            log += "pick($index)"
            if ("pick" in throwIn) throw IllegalStateException("pick")
            return plan
        }

        override fun onWriteFailed() {
            log += "onWriteFailed"
        }

        override fun reset(reason: ResetReason) {
            log += "reset($reason)"
            resets += reason
            if ("reset" in throwIn) throw IllegalStateException("reset")
        }
    }

    private val port = LogPort()
    private val cc = ComposingController(port)
    private val errors = mutableListOf<Pair<String, Exception>>()
    private val src = FakeSource()
    private val c = SuggestionController(cc) { site, e -> errors += site to e }.also {
        it.onCandidates = { cells, revision -> log += "cells(${cells.joinToString(",") { b -> b.text }})@$revision" }
    }

    /** All six conditions true, the fake provider attached, the log cleared. */
    private fun activate() {
        c.enabled = true
        c.fieldAllowed = true
        c.mode = InputMode.QWERTY
        c.chordOn = false
        c.attach(src)
        assertTrue(c.active)
        log.clear()
    }

    private val normal: () -> Unit = { log += "normal" }

    @Test
    fun `SU-01 not active - normal only, no provider call, no read, revision unchanged`() {
        val cases: List<Pair<String, () -> Unit>> = listOf(
            "enabled false" to { c.enabled = false },
            "field not allowed" to { c.fieldAllowed = false },
            "Dubeolsik" to { c.mode = InputMode.DUBEOLSIK },
            "graffiti" to { c.mode = InputMode.GRAFFITI },
            "chord on" to { c.chordOn = true },
            "no provider" to { c.attach(null) },
            "port cannot read" to { port.canRead = false },
        )
        for ((name, off) in cases) {
            activate()
            off()
            assertTrue(name, !c.active)
            log.clear()
            val before = c.revision
            c.key(SuggestKey.Text("a"), normal)
            assertEquals(name, listOf("normal"), log)
            assertEquals(name, before, c.revision)
            // back to the start for the next case
            port.canRead = true
            c.enabled = true; c.fieldAllowed = true; c.mode = InputMode.QWERTY; c.chordOn = false
        }
    }

    @Test
    fun `SU-02 Pass - onKey, normal, afterKey, then the cells`() {
        activate()
        c.key(SuggestKey.Text("a"), normal)
        assertEquals(listOf("onKey(Text(text=a))", "normal", "afterKey(Text(text=a))", "cells()@${c.revision}"), log)
    }

    @Test
    fun `SU-03 WriteThenPass, precondition met - read, batch with the delete and the normal write, then afterKey`() {
        activate()
        port.before = " "
        src.answer = KeyAnswer.WriteThenPass(EditPlan(" ", listOf(EditCommand.DeleteBefore(1))))
        c.key(SuggestKey.Text(".")) { cc.commitText(".") }
        assertEquals(
            listOf("onKey(Text(text=.))", "before(1)", "begin", "deleteBefore(1)", "commit(.)", "end", "afterKey(Text(text=.))", "cells()@${c.revision}"),
            log,
        )
    }

    @Test
    fun `SU-04 WriteThenPass, precondition not met - onWriteFailed, normal outside a batch, afterKey, no delete`() {
        activate()
        port.before = "x"
        src.answer = KeyAnswer.WriteThenPass(EditPlan(" ", listOf(EditCommand.DeleteBefore(1))))
        c.key(SuggestKey.Text(".")) { cc.commitText(".") }
        assertEquals(
            listOf("onKey(Text(text=.))", "before(1)", "onWriteFailed", "commit(.)", "afterKey(Text(text=.))", "cells()@${c.revision}"),
            log,
        )
    }

    @Test
    fun `SU-05 Consume met - delete and insert in one batch, no normal, no afterKey - Consume(NONE) writes nothing`() {
        activate()
        port.before = "I "
        src.answer = KeyAnswer.Consume(EditPlan("I ", listOf(EditCommand.DeleteBefore(2), EditCommand.Insert("i"))), OnFail.PASS)
        c.key(SuggestKey.Backspace, normal)
        assertEquals(listOf("onKey(Backspace)", "before(2)", "begin", "deleteBefore(2)", "commit(i)", "end", "cells()@${c.revision}"), log)

        log.clear()
        src.answer = KeyAnswer.Consume(EditPlan.NONE, OnFail.NOTHING)
        c.key(SuggestKey.Space, normal)
        assertEquals(listOf("onKey(Space)", "cells()@${c.revision}"), log)
    }

    @Test
    fun `SU-06 Consume not met - PASS goes the normal way and afterKey, NOTHING only onWriteFailed`() {
        activate()
        port.before = "x "
        src.answer = KeyAnswer.Consume(EditPlan("I ", listOf(EditCommand.DeleteBefore(2), EditCommand.Insert("i"))), OnFail.PASS)
        c.key(SuggestKey.Backspace, normal)
        assertEquals(listOf("onKey(Backspace)", "before(2)", "onWriteFailed", "normal", "afterKey(Backspace)", "cells()@${c.revision}"), log)

        log.clear()
        src.answer = KeyAnswer.Consume(EditPlan("I ", listOf(EditCommand.DeleteBefore(2))), OnFail.NOTHING)
        c.key(SuggestKey.Backspace, normal)
        assertEquals(listOf("onKey(Backspace)", "before(2)", "onWriteFailed", "cells()@${c.revision}"), log)
    }

    @Test
    fun `SU-07 pick at the current revision writes, at an old one does nothing, a null plan writes nothing`() {
        activate()
        port.before = "mon"
        src.plan = EditPlan("mon", listOf(EditCommand.DeleteBefore(3), EditCommand.Insert("Monday ")))
        val old = c.revision
        c.key(SuggestKey.Text("n"), normal)
        log.clear()
        c.pick(0, old)   // pressed before the key above was handled (FR-SUG-03 ③)
        assertEquals(emptyList<String>(), log)

        c.pick(0, c.revision)
        assertEquals(listOf("pick(0)", "before(3)", "begin", "deleteBefore(3)", "commit(Monday )", "end", "cells()@${c.revision}"), log)

        log.clear()
        src.plan = null
        c.pick(1, c.revision)
        assertEquals(listOf("pick(1)", "cells()@${c.revision}"), log)
    }

    @Test
    fun `SU-08 pick with the text before not the word - nothing written, onWriteFailed`() {
        activate()
        port.before = "mOn"
        src.plan = EditPlan("mon", listOf(EditCommand.DeleteBefore(3), EditCommand.Insert("Monday ")))
        c.pick(0, c.revision)
        assertEquals(listOf("pick(0)", "before(3)", "onWriteFailed", "cells()@${c.revision}"), log)
    }

    @Test
    fun `SU-09 the first three cells with text and spoken name, the revision up by one on key, pick, reset and attach`() {
        val shown = mutableListOf<Pair<List<BarCell>, Int>>()
        c.onCandidates = { cells, r -> shown += cells to r }
        activate()
        src.cells = listOf(
            Candidate("they"), Candidate("there", spoken = "there!"), Candidate("then"), Candidate("them"), Candidate("these"),
        )
        var r = c.revision
        c.key(SuggestKey.Text("e"), normal)
        assertEquals(r + 1, c.revision)
        assertEquals(listOf(BarCell("they"), BarCell("there", "there!"), BarCell("then")) to c.revision, shown.last())
        r = c.revision
        c.pick(0, r)
        assertEquals(r + 1, c.revision)
        assertEquals(c.revision, shown.last().second)
        r = c.revision
        c.reset(ResetReason.CURSOR)
        assertEquals(r + 1, c.revision)
        assertEquals(c.revision, shown.last().second)
        r = c.revision
        c.attach(FakeSource())
        assertEquals(r + 1, c.revision)
        assertEquals(c.revision, shown.last().second)
        // Same cells again: still handed on with the new revision (LLD-12 §9).
        c.attach(src)
        val n = shown.size
        c.key(SuggestKey.Text("e"), normal)
        c.key(SuggestKey.Text("e"), normal)
        assertEquals(n + 2, shown.size)
        assertEquals(shown[n].first, shown[n + 1].first)
        assertEquals(shown[n].second + 1, shown[n + 1].second)
    }

    @Test
    fun `SU-10 going inactive resets the provider once with INACTIVE and shows empty cells - attach(null) drops it`() {
        val cases: List<Pair<String, () -> Unit>> = listOf(
            "enabled" to { c.enabled = false },
            "fieldAllowed" to { c.fieldAllowed = false },
            "mode" to { c.mode = InputMode.DUBEOLSIK },
            "chordOn" to { c.chordOn = true },
        )
        for ((name, off) in cases) {
            c.enabled = true; c.fieldAllowed = true; c.mode = InputMode.QWERTY; c.chordOn = false
            activate()
            src.resets.clear()
            val r = c.revision
            off()
            assertEquals(name, listOf(ResetReason.INACTIVE), src.resets)
            assertEquals(name, listOf("reset(INACTIVE)", "cells()@${r + 1}"), log)
            off()   // the same value again: nothing more
            assertEquals(name, listOf(ResetReason.INACTIVE), src.resets)
        }
        c.enabled = true; c.fieldAllowed = true; c.mode = InputMode.QWERTY; c.chordOn = false
        activate()
        src.resets.clear()
        val r = c.revision
        c.attach(null)
        assertEquals(emptyList<ResetReason>(), src.resets)   // dropped as it is: never called again
        assertEquals(listOf("cells()@${r + 1}"), log)
        // Coming back on needs nothing (it was reset going off).
        log.clear()
        c.attach(src)
        assertEquals(listOf("cells()@${r + 2}"), log)
    }

    @Test
    fun `SU-11 onKey throws - reported with the site, provider reset tried, normal once and the key is not lost`() {
        activate()
        src.throwIn += "onKey"
        c.key(SuggestKey.Text("a"), normal)
        assertEquals(1, errors.size)
        assertEquals(SuggestionController.SITE, errors[0].first)
        assertEquals("Suggest.source", errors[0].first)
        assertEquals("onKey", errors[0].second.message)
        assertEquals(1, log.count { it == "normal" })
        assertEquals(listOf("onKey(Text(text=a))", "reset(INACTIVE)", "normal", "afterKey(Text(text=a))"), log.dropLast(1))
    }

    @Test
    fun `SU-12 afterKey, pick and candidates throwing are reported, reset, and stay inside`() {
        activate()
        src.throwIn += "afterKey"
        c.key(SuggestKey.Text("a"), normal)
        assertEquals("afterKey", errors.single().second.message)
        assertTrue(log.contains("reset(INACTIVE)"))

        errors.clear(); log.clear(); src.throwIn.clear()
        src.throwIn += "pick"
        c.pick(0, c.revision)
        assertEquals("pick", errors.single().second.message)
        assertEquals(listOf("pick(0)", "reset(INACTIVE)", "cells()@${c.revision}"), log)

        errors.clear(); log.clear(); src.throwIn.clear()
        src.throwIn += "candidates"
        c.key(SuggestKey.Text("a"), normal)
        assertEquals("candidates", errors.single().second.message)
        assertEquals(listOf("onKey(Text(text=a))", "normal", "afterKey(Text(text=a))", "reset(INACTIVE)", "cells()@${c.revision}"), log)

        // A reset that throws again while recovering is ignored.
        errors.clear(); log.clear()
        src.throwIn += "reset"
        c.reset(ResetReason.FIELD)
        assertEquals(1 + 1, errors.size)   // the reset itself, then candidates; each recovery reset is swallowed
        assertTrue(errors.all { it.first == SuggestionController.SITE })
    }

    @Test
    fun `SU-13 normal throwing goes to the caller of key`() {
        activate()
        val e = assertThrows(IllegalStateException::class.java) { c.key(SuggestKey.Text("a")) { throw IllegalStateException("normal") } }
        assertEquals("normal", e.message)
        assertEquals(emptyList<Pair<String, Exception>>(), errors)
        // In a WriteThenPass batch too, and the batch is closed (issue #89 way).
        log.clear()
        port.before = " "
        src.answer = KeyAnswer.WriteThenPass(EditPlan(" ", listOf(EditCommand.DeleteBefore(1))))
        assertThrows(IllegalStateException::class.java) { c.key(SuggestKey.Text(".")) { throw IllegalStateException("normal") } }
        assertTrue(log.contains("end"))
        // And an editor failure while writing a plan.
        port.throwOnDelete = true
        assertThrows(IllegalStateException::class.java) { c.key(SuggestKey.Text("."), normal) }
        assertTrue(errors.isEmpty())
    }

    // SU-14..SU-16: the real provider on the test dictionary and an editor that applies the writes to a buffer.

    private class Real(readable: Boolean = true) {
        val editor = BufferEditor(readable = readable)
        val cc = ComposingController(editor)
        val cells = mutableListOf<List<String>>()
        val c = SuggestionController(cc) { _, e -> throw AssertionError(e) }.also {
            it.onCandidates = { cs, _ -> cells += cs.map(BarCell::text) }
            it.enabled = true
            it.fieldAllowed = true
            it.mode = InputMode.QWERTY
            it.attach(EnglishWordSource(MiniWords.load()))
        }

        /** Keys as the dispatcher sends them in QWERTY: ' ' = space, '⌫' = backspace, others = text. */
        fun type(keys: String) {
            for (ch in keys) when (ch) {
                ' ' -> c.key(SuggestKey.Space) { cc.finishComposing(); cc.commitText(" ") }
                '⌫' -> c.key(SuggestKey.Backspace) { cc.deleteBefore() }
                else -> c.key(SuggestKey.Text(ch.toString())) { cc.finishComposing(); cc.commitText(ch.toString()) }
            }
        }

        fun pick(i: Int) = c.pick(i, c.revision)
        val text get() = editor.text.toString()
    }

    @Test
    fun `SU-14 mon, pick Monday, then a period - the auto space goes`() {
        val r = Real()
        r.type(" mon")
        assertEquals(listOf("Monday", "money", "month"), r.cells.last())
        r.pick(0)
        assertEquals(" Monday ", r.text)
        assertEquals(emptyList<String>(), r.cells.last())
        r.type(".")
        assertEquals(" Monday.", r.text)
    }

    @Test
    fun `SU-15 the undo of a pick by backspace gives the word and its cells back`() {
        val r = Real()
        r.type(" i")
        assertEquals(listOf("in", "is", "I"), r.cells.last())
        r.pick(2)
        assertEquals(" I ", r.text)
        r.type("⌫")
        assertEquals(" i", r.text)
        assertEquals(listOf("in", "is", "I"), r.cells.last())

        val t = Real()
        t.type(" the")
        assertEquals(listOf("they", "there", "then"), t.cells.last())
        t.pick(0)
        assertEquals(" they ", t.text)
        t.type(" ")   // the no-op space after a pick (FR-SUG-07 ③)
        assertEquals(" they ", t.text)
        t.type("⌫")
        assertEquals(" the", t.text)
        assertEquals(listOf("they", "there", "then"), t.cells.last())
    }

    @Test
    fun `SU-16 reads give null - cells show but the pick writes nothing and the word goes`() {
        val r = Real(readable = false)
        r.type(" the")
        assertEquals(listOf("they", "there", "then"), r.cells.last())
        r.editor.calls.clear()
        r.pick(0)
        assertEquals(listOf("before(3)"), r.editor.calls)   // the precondition read, no write
        assertEquals(" the", r.text)
        assertEquals(emptyList<String>(), r.cells.last())
    }
}
