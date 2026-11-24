package kbd.app.ime

import kbd.app.feedback.FeedbackPort
import kbd.hangul.compose.ComposePolicy
import kbd.hangul.compose.HangulComposer
import kbd.stroke.geom.Dir
import kbd.stroke.layout.FunctionKey
import kbd.stroke.layout.TapKeyDef
import kbd.stroke.layout.TapLayout
import kbd.stroke.layout.TapLayouts
import kbd.stroke.tap.ChordModifier
import kbd.stroke.tap.TapGesture
import kbd.stroke.tap.TapKeyResolver
import kbd.suggest.Candidate
import kbd.suggest.EditPlan
import kbd.suggest.KeyAnswer
import kbd.suggest.ResetReason
import kbd.suggest.SuggestKey
import kbd.suggest.SuggestionSource
import kbd.suggest.TextPeek
import kbd.suggest.en.EnglishWordSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * LLD-11 §7.12 (파) SX-01..SX-09: the dispatcher asks [SuggestionController] around the normal key paths (LLD-12 §13).
 * A recording fake port, a real controller and a provider that records the keys it is asked about.
 */
class InputDispatcherSuggestTest {

    private class FakePort : EditorPort {
        val calls = mutableListOf<String>()
        override val canReadText: Boolean get() = true
        override fun textBeforeCursor(n: Int): String? { calls += "before($n)"; return "" }
        override fun textAfterCursor(n: Int): String? { calls += "after($n)"; return "" }
        override fun hasSelection(): Boolean? { calls += "selection"; return false }
        override fun deleteBeforeCursor(n: Int) { calls += "deleteBefore($n)" }
        override fun commitText(text: String) { calls += "commit($text)" }
        override fun setComposingText(text: String) { calls += "compose($text)" }
        override fun finishComposingText() { calls += "finish" }
        override fun deleteBefore() { calls += "del" }
        override fun sendKey(key: EditorKey) { calls += "key($key)" }
        override fun sendChord(key: kbd.stroke.tap.ChordKey, mods: Set<ChordModifier>) { calls += "chord($key,$mods)" }
        override fun performEditorAction(actionId: Int) { calls += "action($actionId)" }
        override fun setSelection(start: Int, end: Int) { calls += "sel($start,$end)" }
        override fun beginBatch() { calls += "begin" }
        override fun endBatch() { calls += "end" }
    }

    /** Answers Pass; records every call. */
    private class RecordingSource : SuggestionSource {
        val calls = mutableListOf<String>()
        val keys = mutableListOf<SuggestKey>()
        override val candidates: List<Candidate> get() = emptyList()
        override fun onKey(key: SuggestKey, peek: TextPeek): KeyAnswer {
            calls += "onKey"
            keys += key
            return KeyAnswer.Pass
        }
        override fun afterKey(key: SuggestKey) { calls += "afterKey" }
        override fun pick(index: Int): EditPlan? { calls += "pick"; return null }
        override fun onWriteFailed() { calls += "onWriteFailed" }
        override fun reset(reason: ResetReason) { calls += "reset($reason)" }
    }

    private val noFeedback = object : FeedbackPort {
        override fun key() {}
        override fun error() {}
    }

    private class Rig(port: EditorPort, start: InputMode = InputMode.QWERTY, withSuggest: Boolean = true, feedback: FeedbackPort) {
        val modes = ModeController(InputMode.entries.toList(), start)
        val cc = ComposingController(port)
        val d = InputDispatcher(modes, cc, feedback) {}
        val c = SuggestionController(cc) { _, e -> throw AssertionError(e) }.also {
            it.enabled = true
            it.fieldAllowed = true
            it.mode = start
        }

        init {
            if (withSuggest) d.suggest = c
            d.onShowMode = { m -> c.mode = m }   // as the service's showModeView
        }
    }

    private val port = FakePort()
    private val src = RecordingSource()
    private val rig = Rig(port, feedback = noFeedback).also { it.c.attach(src) }
    private val d = rig.d
    private val q = TapLayouts.QWERTY

    private fun key(layout: TapLayout, id: String): TapKeyDef = layout.keys.single { it.id == id }
    private fun hit(d: InputDispatcher, k: TapKeyDef, g: TapGesture = TapGesture.Tap) = d.onTapResult(k, TapKeyResolver.resolve(k, g), g)
    private fun press(d: InputDispatcher, f: FunctionKey) {
        d.onFunctionDown(f)
        d.onFunctionUp(f)
    }

    private val functionKeys = listOf(
        FunctionKey.BACKSPACE, FunctionKey.SPACE, FunctionKey.ENTER, FunctionKey.TAB,
        FunctionKey.ARROW_LEFT, FunctionKey.ARROW_UP, FunctionKey.ARROW_DOWN, FunctionKey.ARROW_RIGHT,
        FunctionKey.ESC, FunctionKey.DELETE,
    )

    @Test
    fun `SX-01 QWERTY a, A, 1 and apostrophe - one Text each, the commit inside the key's normal path`() {
        hit(d, key(q, "a"))
        hit(d, key(q, "a"), TapGesture.Exit(Dir.UP))
        hit(d, key(q, "1"))
        hit(d, key(q, "comma"), TapGesture.Exit(Dir.LEFT))
        assertEquals(listOf("a", "A", "1", "'").map { SuggestKey.Text(it) }, src.keys)
        assertEquals(List(4) { listOf("onKey", "afterKey") }.flatten(), src.calls)
        assertEquals(listOf("commit(a)", "commit(A)", "commit(1)", "commit(')"), port.calls)
    }

    @Test
    fun `SX-02 function keys go as Backspace, Space, Enter, Tab and six Navigation, with the 0_96 editor calls`() {
        for (f in functionKeys) press(d, f)
        assertEquals(
            listOf(SuggestKey.Backspace, SuggestKey.Space, SuggestKey.Enter, SuggestKey.Tab) + List(6) { SuggestKey.Navigation },
            src.keys,
        )
        val plainPort = FakePort()
        val plain = Rig(plainPort, withSuggest = false, feedback = noFeedback).d
        for (f in functionKeys) press(plain, f)
        assertEquals(plainPort.calls, port.calls)
    }

    @Test
    fun `SX-03 Ctrl on drops the word and the chord path never asks the provider - off again after the key`() {
        press(d, FunctionKey.CTRL)
        assertTrue(rig.c.chordOn)
        assertEquals(listOf("reset(INACTIVE)"), src.calls)
        assertFalse(rig.c.active)
        hit(d, key(q, "c"))
        assertEquals(listOf("reset(INACTIVE)"), src.calls)   // nothing asked for the chord key
        assertEquals(emptySet<ChordModifier>(), d.chord)
        assertFalse(rig.c.chordOn)
        assertTrue(rig.c.active)
        assertTrue(port.calls.any { it.startsWith("chord(") })
        // A function key with a chord: not asked either.
        src.calls.clear()
        press(d, FunctionKey.ALT)
        press(d, FunctionKey.BACKSPACE)
        assertEquals(listOf("reset(INACTIVE)"), src.calls)
    }

    @Test
    fun `SX-04 the mode key tap and its up stroke reset the provider with MODE`() {
        d.onModeKey()
        assertTrue(src.calls.contains("reset(MODE)"))
        src.calls.clear()
        // Back to QWERTY (Dubeolsik → … the cycle), then the up stroke.
        while (rig.modes.current != InputMode.QWERTY) d.onModeKey()
        src.calls.clear()
        d.onSettingsStroke()
        assertEquals(listOf("reset(MODE)"), src.calls)
    }

    @Test
    fun `SX-05 no controller - the same calls as before suggestions`() {
        val p = FakePort()
        val plain = Rig(p, withSuggest = false, feedback = noFeedback).d
        hit(plain, key(q, "h"))
        hit(plain, key(q, "i"))
        press(plain, FunctionKey.SPACE)
        press(plain, FunctionKey.BACKSPACE)
        press(plain, FunctionKey.ARROW_LEFT)
        assertEquals(listOf("commit(h)", "commit(i)", "commit( )", "del", "key(DPAD_LEFT)"), p.calls)
    }

    @Test
    fun `SX-06 a symbol key in Dubeolsik - the controller is not active, normal only`() {
        val p = FakePort()
        val s = RecordingSource()
        val dub = Rig(p, start = InputMode.DUBEOLSIK, feedback = noFeedback).also { it.c.attach(s) }
        dub.d.composer = HangulComposer(ComposePolicy.DUBEOLSIK)
        hit(dub.d, key(TapLayouts.DUBEOLSIK, "minus"), TapGesture.Exit(Dir.LEFT))
        assertEquals(emptyList<String>(), s.calls)
        assertEquals(listOf("commit(-)"), p.calls.filter { it.startsWith("commit") })
        assertFalse(p.calls.any { it.startsWith("before") || it.startsWith("after") || it == "selection" })
    }

    @Test
    fun `SX-07 real provider - the, pick, then backspace held - the first undoes, the next two delete one each`() {
        val editor = BufferEditor()
        val r = Rig(editor, feedback = noFeedback).also { it.c.attach(EnglishWordSource(MiniWords.load())) }
        press(r.d, FunctionKey.SPACE)
        for (ch in "the") hit(r.d, key(q, ch.toString()))
        r.c.pick(0, r.c.revision)
        assertEquals(" they ", editor.text.toString())
        repeat(3) { r.d.onFunctionDown(FunctionKey.BACKSPACE) }   // a repeat: downs with no up
        r.d.onFunctionUp(FunctionKey.BACKSPACE)
        assertEquals(" t", editor.text.toString())
        // Step by step on a fresh rig.
        val e2 = BufferEditor()
        val r2 = Rig(e2, feedback = noFeedback).also { it.c.attach(EnglishWordSource(MiniWords.load())) }
        press(r2.d, FunctionKey.SPACE)
        for (ch in "the") hit(r2.d, key(q, ch.toString()))
        r2.c.pick(0, r2.c.revision)
        val seen = mutableListOf<String>()
        repeat(3) {
            r2.d.onFunctionDown(FunctionKey.BACKSPACE)
            seen += e2.text.toString()
        }
        assertEquals(listOf(" the", " th", " t"), seen)
    }

    @Test
    fun `SX-08 graffiti mode - space and backspace ask no provider`() {
        val p = FakePort()
        val s = RecordingSource()
        val g = Rig(p, start = InputMode.GRAFFITI, feedback = noFeedback).also { it.c.attach(s) }
        g.d.composer = HangulComposer(ComposePolicy.GRAFFITI)
        press(g.d, FunctionKey.SPACE)
        press(g.d, FunctionKey.BACKSPACE)
        assertEquals(emptyList<String>(), s.calls)
        assertEquals(listOf("commit( )", "del"), p.calls.filter { it.startsWith("commit") || it == "del" })
    }

    @Test
    fun `SX-09 the no-op space after a pick - marked as input, no editor write`() {
        val editor = BufferEditor()
        val r = Rig(editor, feedback = noFeedback).also { it.c.attach(EnglishWordSource(MiniWords.load())) }
        press(r.d, FunctionKey.SPACE)
        for (ch in "the") hit(r.d, key(q, ch.toString()))
        r.c.pick(0, r.c.revision)
        r.modes.typed = false
        editor.calls.clear()
        press(r.d, FunctionKey.SPACE)
        assertTrue(r.modes.typed)
        assertEquals(emptyList<String>(), editor.calls.filter { !it.startsWith("before") })
        assertEquals(" they ", editor.text.toString())
    }
}
