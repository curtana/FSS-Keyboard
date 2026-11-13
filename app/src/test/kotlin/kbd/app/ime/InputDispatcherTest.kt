package kbd.app.ime

import kbd.app.error.EngineGuard
import kbd.app.feedback.FeedbackPort
import kbd.app.store.ErrorLog
import kbd.app.swipe.SwipeEngineState
import kbd.hangul.compose.ComposePolicy
import kbd.hangul.compose.HangulComposer
import kbd.hangul.event.ComposeResult
import kbd.hangul.event.JamoEvent
import kbd.hangul.jamo.Jamo
import kbd.stroke.geom.KeyGeom
import kbd.stroke.geom.RectDp
import kbd.stroke.geom.StrokePoint
import kbd.stroke.layout.FunctionKey
import kbd.stroke.layout.TapKeyDef
import kbd.stroke.layout.TapLayout
import kbd.stroke.layout.TapLayouts
import kbd.stroke.tap.TapGesture
import kbd.stroke.tap.TapGestureClassifier
import kbd.stroke.tap.TapKeyResolver
import kbd.stroke.tap.TapOutput
import kbd.stroke.threshold.ThresholdDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * LLD-11 §7.4 ID-01..ID-08, ID-11, ID-12, ID-14..ID-17, ID-19, ID-20, ID-23, ID-24, §5.4 RT-03, RT-04,
 * §7.5 EG-05 (LLD-08 §6). Graffiti IDs (ID-09, 10, 13, 18, 21, 22) belong to T31.
 */
class InputDispatcherTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private class FakePort : EditorPort {
        val calls = mutableListOf<String>()
        var throwOnFinish = false
        override fun commitText(text: String) { calls += "commit($text)" }
        override fun setComposingText(text: String) { calls += "compose($text)" }
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

    private class FakeFeedback : FeedbackPort {
        var keys = 0
        var errors = 0
        override fun key() { keys++ }
        override fun error() { errors++ }
    }

    /** Stands in for the wrapped `SwipeInputMapper`: [block] is its state, [backspaces] counts the ⌫ that asked it first. */
    private class FakeSwipe : SwipeEngineState {
        var block = ""
        var resets = 0
        var throwOnReset = false
        var throwOnBackspace = false
        var backspaces = 0
        /** Strokes "dropped" at the next reset (SWIPE 3.3: the caller vibrates). */
        var dropOnReset = false
        override fun reset(): Boolean {
            block = ""
            resets++
            if (throwOnReset) throw IllegalStateException("parser")
            return dropOnReset.also { dropOnReset = false }
        }
        override fun popElement(): kbd.hangul.stroke.StrokePop {
            if (throwOnBackspace) throw IllegalStateException("mapper")
            backspaces++
            return kbd.hangul.stroke.StrokePop.Empty
        }
        override fun snapshot(): Any = block
        override fun restore(s: Any) { block = s as String }
    }

    private val port = FakePort()
    private val feedback = FakeFeedback()
    private val cc = ComposingController(port)
    private val saved = mutableListOf<InputMode>()
    private val shown = mutableListOf<InputMode>()
    private var modes = ModeController(InputMode.entries.toList(), InputMode.DUBEOLSIK)
    private val d = newDispatcher()

    private fun newDispatcher() = InputDispatcher(modes, cc, feedback) { saved += it }.also { it.onShowMode = { m -> shown += m } }

    private fun field(
        fieldClass: FieldClass = FieldClass.TEXT,
        password: Boolean = false,
        multiLine: Boolean = false,
        enterAction: Int? = null,
    ) = FieldInfo(fieldClass, isPassword = password, noLearning = false, multiLine = multiLine, enterAction = enterAction)

    private fun jamo(c: Char) = Jamo.ofCompat(c)!!

    private fun key(layout: TapLayout, id: String): TapKeyDef = layout.keys.single { it.id == id }

    private fun dubeolsikKey(j: Char): TapKeyDef = TapLayouts.DUBEOLSIK.keys.single { it.jamo == j }

    private fun dubeolsik() {
        d.composer = HangulComposer(ComposePolicy.DUBEOLSIK)
    }

    private fun swipe(): FakeSwipe {
        d.composer = HangulComposer(ComposePolicy.SWIPE)
        return FakeSwipe().also { d.swipeState = it }
    }

    /** Dubeolsik letter taps, through the same resolver path as the view. */
    private fun type(text: String) {
        for (c in text) {
            val k = dubeolsikKey(c)
            d.onTapOutput(k, TapKeyResolver.resolve(k, TapGesture.Tap))
        }
    }

    /** Swipe "고" (ㄱ key ↑). */
    private fun swipeGo(): FakeSwipe {
        val s = swipe()
        d.onJamoEvents(listOf(JamoEvent.Consonant(jamo('ㄱ')), JamoEvent.Vowel(jamo('ㅗ'))))
        s.block = "ㄱㅗ"
        return s
    }

    /** One stroke on [key] of a 360×288 dp window with 48 dp rows, through [legs] (dx, dy) from the key centre. */
    private fun stroke(key: TapKeyDef, vararg legs: Pair<Float, Float>): TapOutput {
        val rect = RectDp(key.x, key.row * 48f, key.x + key.w, (key.row + 1) * 48f)
        val geom = KeyGeom.build(key.id, rect, 360f, 288f)
        val cls = TapGestureClassifier(geom, ThresholdDefaults.VALUES.minCrossDp)
        var x = (rect.left + rect.right) / 2
        var y = (rect.top + rect.bottom) / 2
        var t = 0L
        cls.begin(StrokePoint(x, y, t))
        for ((dx, dy) in legs) {
            val n = 20
            repeat(n) {
                x += dx / n
                y += dy / n
                t += 8
                cls.add(StrokePoint(x, y, t))
            }
        }
        return TapKeyResolver.resolve(key, cls.finish(StrokePoint(x, y, t + 8)))
    }

    @Test
    fun `ID-40 Dubeolsik compound final in one stroke - ㄹ left on 다 is 닭, ⌫ gives 달 as two taps would (issue 242)`() {
        dubeolsik()
        type("ㄷㅏ")
        val r = dubeolsikKey('ㄹ')
        d.onTapOutput(r, TapKeyResolver.resolve(r, TapGesture.Exit(kbd.stroke.geom.Dir.LEFT)))
        assertEquals("닭", cc.composingShown)
        d.onFunctionDown(FunctionKey.BACKSPACE)
        assertEquals("달", cc.composingShown)
    }

    // ---- RT ----

    @Test
    fun `RT-03 backspace press and two repeats on 닭 give 달 then 다 then ㄷ`() {
        dubeolsik()
        type("ㄷㅏㄹㄱ")
        assertEquals("닭", cc.composingShown)
        val after = mutableListOf<String>()
        repeat(3) {   // press, then two repeats: each repeat is one onFunctionDown (LLD-05A §6)
            d.onFunctionDown(FunctionKey.BACKSPACE)
            after += cc.composingShown
        }
        assertEquals(listOf("달", "다", "ㄷ"), after)
        assertFalse(port.calls.contains("del"))
    }

    @Test
    fun `RT-04 arrow press and two repeats send DPAD_RIGHT three times, finished before the first`() {
        dubeolsik()
        type("ㄱㅏㄱ")
        port.calls.clear()
        repeat(3) { d.onFunctionDown(FunctionKey.ARROW_RIGHT) }
        assertEquals(listOf("finish", "key(DPAD_RIGHT)", "key(DPAD_RIGHT)", "key(DPAD_RIGHT)"), port.calls)
    }

    // ---- ID ----

    @Test
    fun `ID-01 number row 2 up while composing 각 finishes then commits @`() {
        dubeolsik()
        type("ㄱㅏㄱ")
        port.calls.clear()
        val two = key(TapLayouts.DUBEOLSIK, "2")
        d.onTapOutput(two, stroke(two, 0f to -40f))
        assertEquals(listOf("finish", "commit(@)"), port.calls)
        assertFalse(d.composer!!.isComposing())
    }

    @Test
    fun `ID-02 tab key while composing 각 finishes then commits tab`() {
        dubeolsik()
        type("ㄱㅏㄱ")
        port.calls.clear()
        d.onFunctionDown(FunctionKey.TAB)
        assertEquals(listOf("finish", "commit(\t)"), port.calls)
    }

    @Test
    fun `ID-03 arrow left while composing 각 finishes, sends DPAD_LEFT and keeps typed`() {
        dubeolsik()
        type("ㄱㅏㄱ")
        port.calls.clear()
        modes.typed = false
        d.onFunctionDown(FunctionKey.ARROW_LEFT)
        assertEquals(listOf("finish", "key(DPAD_LEFT)"), port.calls)
        assertFalse(modes.typed)
        modes.typed = true
        d.onFunctionDown(FunctionKey.ARROW_LEFT)
        assertTrue(modes.typed)
    }

    @Test
    fun `ID-35 Esc while composing 각 finishes, then sends ESCAPE like an arrow (#198)`() {
        dubeolsik()
        type("ㄱㅏㄱ")
        port.calls.clear()
        modes.typed = false
        d.onFunctionDown(FunctionKey.ESC)
        assertEquals(listOf("finish", "key(ESCAPE)"), port.calls)
        assertFalse(modes.typed)
        port.calls.clear()  // nothing composing now: no finish
        d.onFunctionDown(FunctionKey.ESC)
        assertEquals(listOf("key(ESCAPE)"), port.calls)
    }

    @Test
    fun `ID-36 Del while composing 각 finishes, then sends FORWARD_DEL, not a backspace (#198)`() {
        dubeolsik()
        type("ㄱㅏㄱ")
        port.calls.clear()
        d.onFunctionDown(FunctionKey.DELETE)
        assertEquals(listOf("finish", "key(FORWARD_DEL)"), port.calls)
        assertFalse(port.calls.contains("del"))
        port.calls.clear()
        d.onFunctionDown(FunctionKey.DELETE)
        assertEquals(listOf("key(FORWARD_DEL)"), port.calls)
    }

    @Test
    fun `ID-04 space while composing 각 finishes, commits space and sets typed`() {
        dubeolsik()
        type("ㄱㅏㄱ")
        port.calls.clear()
        modes.typed = false
        d.onFunctionDown(FunctionKey.SPACE)
        assertEquals(listOf("finish", "commit( )"), port.calls)
        assertTrue(modes.typed)
    }

    /**
     * Issue #205 (owner decision 2026-11-02): the mode key's up stroke commits what is composing and then
     * opens the settings, also while composing; the ⚙ key's one-syllable completion (issues #173, #180) is gone.
     */
    @Test
    fun `ID-27 the mode key up stroke commits the composing syllable, then opens the settings`() {
        var opened = 0
        val atOpen = mutableListOf<String>()
        d.onOpenSettings = { opened++; atOpen += port.calls }
        val s = swipeGo()
        port.calls.clear()
        modes.typed = false
        val before = modes.current
        s.dropOnReset = true                                      // an unfinished stroke was open: it vibrates (SWIPE 3.3)
        d.onSettingsStroke()
        assertEquals(listOf("finish"), port.calls)               // 고 committed as it is, no space
        assertEquals(listOf("finish"), atOpen)                    // committed before the settings open
        assertFalse(d.composer!!.isComposing())
        assertEquals(1, s.resets)
        assertEquals(1, feedback.errors)
        assertEquals(1, opened)
        assertFalse(modes.typed)                                  // not input, like the mode key
        assertEquals(before, modes.current)                       // no mode change
        assertTrue(shown.isEmpty())
        d.onSettingsStroke()                                      // nothing composing: opens again, nothing committed
        assertEquals(2, opened)
        assertEquals(listOf("finish"), port.calls)
        assertTrue(FunctionKey.entries.none { it.name == "SETTINGS" })   // the swipe ⚙ key is gone
    }

    @Test
    fun `ID-05 swipe perimeter 1 while composing 고 finishes and commits 1`() {
        swipeGo()
        port.calls.clear()
        modes.typed = false
        d.onSwipeText("1", vibe = false)
        assertEquals(listOf("finish", "commit(1)"), port.calls)
        assertFalse(d.composer!!.isComposing())
        assertTrue(modes.typed)
        assertEquals(0, feedback.errors)
        // The mapper has already cleared its anchor (LLD-04 §9.3, LLD-08 §6.1); D-30 vibration on request.
        d.onSwipeText("¹", vibe = true)
        assertEquals(1, feedback.errors)
    }

    @Test
    fun `ID-06 backspace while composing 고 in swipe removes one jamo, the cheonjiin assembler asked first`() {
        val s = swipeGo()
        port.calls.clear()
        d.onFunctionDown(FunctionKey.BACKSPACE)
        assertEquals("ㄱ", cc.composingShown)                         // FR-CMN-07: one jamo (고 ⌫ = ㄱ, issue #168)
        assertEquals(1, s.backspaces)
        d.onFunctionDown(FunctionKey.BACKSPACE)
        assertEquals("", cc.composingShown)
        d.onFunctionDown(FunctionKey.BACKSPACE)                          // nothing composing: one character deleted
        assertEquals(3, s.backspaces)
        assertEquals(0, s.resets)
    }

    @Test
    fun `ID-07 qwerty a commits a and sets typed`() {
        modes = ModeController(InputMode.entries.toList(), InputMode.QWERTY)
        val q = newDispatcher()
        val a = key(TapLayouts.QWERTY, "a")
        q.onTapOutput(a, stroke(a))
        assertEquals(listOf("commit(a)"), port.calls)
        assertTrue(modes.typed)
    }

    @Test
    fun `ID-08 mode key finishes, resets parsers, switches mode and saves it`() {
        dubeolsik()
        val s = FakeSwipe().also { d.swipeState = it }
        type("ㄱㅏㄱ")
        port.calls.clear()
        d.onFunctionDown(FunctionKey.MODE)
        assertEquals(listOf("finish"), port.calls)
        assertEquals(1, s.resets)
        assertEquals(InputMode.GRAFFITI, modes.current)   // typed, no previous → next in order
        assertEquals(InputMode.DUBEOLSIK, modes.previous)
        assertFalse(modes.typed)                           // the mode key is not input
        assertEquals(listOf(InputMode.GRAFFITI), saved)
        assertEquals(listOf(InputMode.GRAFFITI), shown)
    }

    @Test
    fun `ID-11 number row 3 in a numeric-like field commits 3 and sets typed`() {
        modes.enterNumericField()
        d.field = field(FieldClass.NUMBER)
        modes.onFieldChanged()
        val three = key(TapLayouts.QWERTY, "3")
        d.onTapOutput(three, stroke(three))
        assertEquals(listOf("commit(3)"), port.calls)
        assertTrue(modes.typed)
    }

    @Test
    fun `ID-12 onStartInputView order finishes, resets parsers and clears typed`() {
        val s = swipeGo()
        assertTrue(modes.typed)
        port.calls.clear()
        // LLD-08 §3.1 call order.
        d.field = field()
        d.finishAll()
        d.resetParsers()
        modes.onFieldChanged()
        assertEquals(listOf("finish"), port.calls)
        assertEquals(1, s.resets)
        assertFalse(modes.typed)
        assertFalse(d.composer!!.isComposing())
    }

    @Test
    fun `ID-14 qwerty a up and back gives A, i rubbed left gives bracket`() {
        modes = ModeController(InputMode.entries.toList(), InputMode.QWERTY)
        val q = newDispatcher()
        val a = key(TapLayouts.QWERTY, "a")
        val outA = stroke(a, 0f to -40f, 0f to 40f)
        assertEquals(TapOutput("A", null, errorVibe = false), outA)
        q.onTapOutput(a, outA)
        val i = key(TapLayouts.QWERTY, "i")
        val outI = stroke(i, -35f to 0f, 35f to 0f, -35f to 0f)
        assertEquals(TapOutput("[", null, errorVibe = false), outI)
        q.onTapOutput(i, outI)
        assertEquals(listOf("commit(A)", "commit([)"), port.calls)
        assertEquals(0, feedback.errors)
        assertTrue(modes.typed)
    }

    @Test
    fun `ID-15 qwerty b rubbed down commits b with the error vibration`() {
        modes = ModeController(InputMode.entries.toList(), InputMode.QWERTY)
        val q = newDispatcher()
        val b = key(TapLayouts.QWERTY, "b")
        val out = stroke(b, 0f to 40f, 0f to -40f, 0f to 40f)
        assertEquals(TapOutput("b", null, errorVibe = true), out)
        q.onTapOutput(b, out)
        assertEquals(listOf("commit(b)"), port.calls)
        assertEquals(1, feedback.errors)
        assertTrue(modes.typed)
    }

    @Test
    fun `ID-16 enter with action, multi-line and plain single-line fields`() {
        val cases = listOf(
            field(enterAction = 6) to "action(6)",
            field(multiLine = true) to "commit(\n)",
            field() to "key(ENTER)",
        )
        for ((f, expected) in cases) {
            dubeolsik()
            type("ㄱㅏㄱ")
            port.calls.clear()
            modes.typed = false
            d.field = f
            d.onFunctionDown(FunctionKey.ENTER)
            assertEquals(listOf("finish", expected), port.calls)
            assertTrue(modes.typed)
        }
    }

    @Test
    fun `ID-16 enter resets the swipe mapper`() {
        val s = swipeGo()
        d.onFunctionDown(FunctionKey.ENTER)
        assertEquals(1, s.resets)
    }

    @Test
    fun `ID-25 tab resets the swipe mapper`() {
        val s = swipeGo()
        d.onFunctionDown(FunctionKey.TAB)
        assertEquals(1, s.resets)
        assertEquals("", s.block)
    }

    @Test
    fun `ID-17 backspace with nothing composing deletes one character`() {
        d.onFunctionDown(FunctionKey.BACKSPACE)          // QWERTY: no composer
        dubeolsik()
        d.onFunctionDown(FunctionKey.BACKSPACE)          // composer with nothing composing
        assertEquals(listOf("del", "del"), port.calls)
        assertTrue(modes.typed)
    }

    @Test
    fun `ID-19 engine exception leaves the editor and the composer untouched and is logged once`() {
        val log = ErrorLog(tmp.newFile("err.log"))
        val guard = EngineGuard(log, d)
        val s = swipeGo()
        port.calls.clear()
        s.throwOnBackspace = true
        assertNull(guard.stroke("Tap.fn") { d.onFunctionDown(FunctionKey.BACKSPACE) })
        assertEquals(emptyList<String>(), port.calls)
        assertEquals("고", d.composer!!.composingText())
        assertEquals("ㄱㅗ", s.block)
        assertEquals(1, log.readAll().lines().count { it.isNotEmpty() })
    }

    @Test
    fun `ID-20 engine exception in a password field writes no log line`() {
        val log = ErrorLog(tmp.newFile("err.log"))
        val guard = EngineGuard(log, d)
        d.field = field(password = true)
        log.enabled = !d.field.noRecord                       // KeyboardService.onStartInputView (LLD-08 §3.1)
        val s = swipeGo()
        s.throwOnBackspace = true
        assertNull(guard.stroke("Tap.fn") { d.onFunctionDown(FunctionKey.BACKSPACE) })
        assertEquals("", log.readAll())
    }

    @Test
    fun `ID-23 mode key in a numeric-like field switches once and does not save`() {
        modes.enterNumericField()
        assertEquals(InputMode.QWERTY, modes.current)
        d.onFunctionDown(FunctionKey.MODE)
        assertEquals(InputMode.SWIPE, modes.current)   // one onModeKey: QWERTY → next in order (FSS#93: Dubeolsik → QWERTY → swipe → graffiti)
        assertEquals(InputMode.DUBEOLSIK, modes.returnMode)
        assertEquals(emptyList<InputMode>(), saved)
        assertEquals(listOf(InputMode.SWIPE), shown)
    }

    @Test
    fun `ID-24 function key up writes nothing and keeps typed`() {
        dubeolsik()
        type("ㄱ")
        port.calls.clear()
        modes.typed = false
        for (k in FunctionKey.entries) d.onFunctionUp(k)
        assertEquals(emptyList<String>(), port.calls)
        assertFalse(modes.typed)
        assertEquals("ㄱ", cc.composingShown)
    }

    // ---- EG ----

    @Test
    fun `EG-05 exception during finish while composing 고 keeps 고 and the swipe mapper state`() {
        val log = ErrorLog(tmp.newFile("err.log"))
        val guard = EngineGuard(log, d)
        val s = swipeGo()
        port.throwOnFinish = true
        assertNull(guard.stroke("Swipe.fn") { d.onFunctionDown(FunctionKey.SPACE) })
        assertEquals("고", d.composer!!.composingText())
        assertEquals("고", cc.composingShown)
        assertEquals("ㄱㅗ", s.block)
        assertEquals(0, s.resets)
        assertEquals(1, log.readAll().lines().count { it.isNotEmpty() })
    }

    @Test
    fun `EG-05 restore also undoes a swipe mapper reset and a composer finish done before the failure`() {
        val s = swipeGo()
        val before = d.snapshot()
        s.reset()
        d.composer!!.finish()
        d.restore(before)
        assertEquals("ㄱㅗ", s.block)
        assertEquals("고", d.composer!!.composingText())
    }

    // ---- cursor moves (LLD-08 §6.1, §4.1; G13) ----

    /** CC-16 numbers: "가" + temporary glyph "ㆍ" at 5..7; the glyph path tells finishAfterUserMove from finishComposing. */
    private fun composingWithGlyph(): FakeSwipe {
        val s = swipe()
        cc.onStart(5, 5)
        d.composer!!.apply(JamoEvent.Consonant(jamo('ㄱ')))
        d.composer!!.apply(JamoEvent.Vowel(jamo('ㅏ')))
        cc.show(ComposeResult("", "가"), "ㆍ")
        s.block = "ㄱㅏ"
        port.calls.clear()
        return s
    }

    @Test
    fun `onCursorMoved right after USER_MOVED drops the glyph, finishes and keeps the user position`() {
        val s = composingWithGlyph()
        assertEquals(SelectionVerdict.USER_MOVED, cc.onUpdateSelection(9, 9, 5, 7))
        d.onCursorMoved(9, 9, 5, 7)
        assertEquals(listOf("begin", "compose(가)", "finish", "sel(8,8)", "end"), port.calls)
        assertFalse(d.composer!!.isComposing())
        assertEquals("", s.block)
        assertEquals(1, s.resets)
    }

    @Test
    fun `onCursorMoved with no reported span uses the known span end (G13)`() {
        composingWithGlyph()
        assertEquals(SelectionVerdict.USER_MOVED, cc.onUpdateSelection(9, 9, -1, -1))
        d.onCursorMoved(9, 9, -1, -1)
        assertEquals(listOf("begin", "compose(가)", "finish", "sel(8,8)", "end"), port.calls)
    }

    @Test
    fun `onCursorMoved is not input and onCursorMovedNoComposing only resets parsers`() {
        val s = composingWithGlyph()
        modes.typed = false
        cc.onUpdateSelection(2, 2, 5, 7)
        d.onCursorMoved(2, 2, 5, 7)
        assertFalse(modes.typed)
        port.calls.clear()
        d.onCursorMovedNoComposing()
        assertEquals(emptyList<String>(), port.calls)
        assertEquals(2, s.resets)
        assertFalse(modes.typed)
    }

    @Test
    fun `swipe jamo events show the composer result and set typed`() {
        swipe()
        d.onJamoEvents(listOf(JamoEvent.Consonant(jamo('ㄱ')), JamoEvent.Vowel(jamo('ㅗ'))))
        assertEquals(listOf("begin", "compose(고)", "end"), port.calls)
        assertTrue(modes.typed)
    }

    @Test
    fun `InputSink hidesDisplay and error follow the field and the feedback port - FR-CMN-08 split`() {
        assertFalse(d.hidesDisplay)
        d.field = field(password = true)
        assertTrue(d.hidesDisplay)
        d.field = FieldInfo(FieldClass.TEXT, isPassword = false, noLearning = true, multiLine = false, enterAction = null)
        assertFalse(d.hidesDisplay)   // ② a no-learning field shows the glyph flash (owner 2026-11-01)
        d.error()
        assertEquals(1, feedback.errors)
    }
}
