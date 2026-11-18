package kbd.app.ime

import kbd.app.error.EngineGuard
import kbd.app.feedback.FeedbackPort
import kbd.app.store.ErrorLog
import kbd.hangul.compose.ComposePolicy
import kbd.hangul.compose.HangulComposer
import kbd.hangul.event.ComposeResult
import kbd.hangul.event.JamoEvent
import kbd.hangul.jamo.Jamo
import kbd.stroke.geom.KeyGeom
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
        /** The editor fails at the start of the next composing write. */
        var throwOnBegin = false
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
        override fun beginBatch() {
            if (throwOnBegin) throw IllegalStateException("begin")
            calls += "begin"
        }
        override fun endBatch() { calls += "end" }
    }

    private class FakeFeedback : FeedbackPort {
        var keys = 0
        var errors = 0
        override fun key() { keys++ }
        override fun error() { errors++ }
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


    /** Dubeolsik letter taps, through the same resolver path as the view. */
    private fun type(text: String) {
        for (c in text) {
            val k = dubeolsikKey(c)
            d.onTapOutput(k, TapKeyResolver.resolve(k, TapGesture.Tap))
        }
    }

    /** Dubeolsik "고", composing. */
    private fun composeGo() {
        dubeolsik()
        type("ㄱㅗ")
    }

    /** One stroke on [key] of a 360×288 dp window with 48 dp rows, through [legs] (dx, dy) from the key centre. */
    private fun stroke(key: TapKeyDef, vararg legs: Pair<Float, Float>): TapOutput {
        val rect = key.rect(1f, 48f)
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
    fun `ID-40 Dubeolsik compound final by two taps - 다 ㄹ ㄱ is 닭, ⌫ gives 달, and ㄹ up on 다 is 다로 (issue 242, FSS 109, 124)`() {
        dubeolsik()
        type("ㄷㅏㄹㄱ")
        assertEquals("닭", cc.composingShown)
        d.onFunctionDown(FunctionKey.BACKSPACE)
        assertEquals("달", cc.composingShown)
        // No compound final in one stroke since FSS#124: ㄹ ↑ is ㄹ + ㅗ (ㄺ in FSS#109..#122).
        d.finishAll()
        port.calls.clear()
        type("ㄷㅏ")
        val r = dubeolsikKey('ㄹ')
        d.onTapOutput(r, TapKeyResolver.resolve(r, TapGesture.Exit(kbd.stroke.geom.Dir.UP)))
        assertEquals("다로", shownText(port.calls))
    }

    /** What the editor shows after [port]'s calls: committed text plus the composing text. */
    private fun shownText(calls: List<String>): String {
        val text = StringBuilder()
        var composing = ""
        for (c in calls) when {
            c.startsWith("compose(") -> composing = c.removePrefix("compose(").removeSuffix(")")
            c.startsWith("commit(") -> { text.append(c.removePrefix("commit(").removeSuffix(")")); composing = "" }
            c == "finish" -> { text.append(composing); composing = "" }
            c == "del" -> if (text.isNotEmpty()) text.setLength(text.length - 1)
        }
        return text.toString() + composing
    }

    /**
     * Dubeolsik input from [keys]: a jamo alone is a tap on its key, a jamo then an arrow (↑ ← → ↓, or a diagonal
     * ↖ ↗ ↙ ↘, FSS#109) a stroke that way, and ⌫ a backspace. Returns what the editor shows, on a fresh composer.
     */
    private fun shownAfter(keys: String): String {
        d.finishAll()                     // the case before is done
        dubeolsik()
        port.calls.clear()
        var i = 0
        while (i < keys.length) {
            val c = keys[i]
            if (c == '⌫') {
                d.onFunctionDown(FunctionKey.BACKSPACE)
                i++
                continue
            }
            val dir = when (keys.getOrNull(i + 1)) {
                '↑' -> kbd.stroke.geom.Dir.UP
                '←' -> kbd.stroke.geom.Dir.LEFT
                '→' -> kbd.stroke.geom.Dir.RIGHT
                '↓' -> kbd.stroke.geom.Dir.DOWN
                '↖' -> kbd.stroke.geom.Diag.UP_LEFT
                '↗' -> kbd.stroke.geom.Diag.UP_RIGHT
                '↙' -> kbd.stroke.geom.Diag.DOWN_LEFT
                '↘' -> kbd.stroke.geom.Diag.DOWN_RIGHT
                else -> null
            }
            val k = dubeolsikKey(c)
            // A ↺ after the arrow is an exit-and-return (FSS#128).
            val back = dir != null && keys.getOrNull(i + 2) == '↺'
            val g = when {
                dir == null -> TapGesture.Tap
                back -> TapGesture.ExitReturn(dir)
                else -> TapGesture.Exit(dir)
            }
            d.onTapOutput(k, TapKeyResolver.resolve(k, g))
            i += if (dir == null) 1 else if (back) 3 else 2
        }
        return shownText(port.calls)
    }

    @Test
    fun `ID-41 a paired vowel stroke composes as its key would - no vowel merge, dokkaebibul, ⌫ unit (FSS 104, FSS 107)`() {
        val cases = listOf(
            "ㄱㅗ↑" to "교", "ㄱㅓ←" to "겨", "ㄱㅏ→" to "갸", "ㄱㅜ↓" to "규",
            "ㄱㅗ↑ㅏ" to "교ㅏ",     // ㅛ + ㅏ does not merge, as ㅛ typed on its key
            "ㄱㅏㄱㅓ←" to "가겨",   // dokkaebibul: the final ㄱ goes to the next syllable
            "ㄱㅜ↓ㄹ" to "귤",
            "ㄱㅜㅓ←" to "구ㅕ",     // ㅜ + ㅕ does not merge
            "ㄱㅜㅓ" to "궈",        // the tap still merges
            "ㄱㅏㅌ↑" to "가토",     // ㅌ ↑ is ㅌ + ㅗ (FSS#122; ㅌ in FSS#109, ㄾ in FSS#107)
            "ㄱㅏㅌ←" to "가터",
            "ㄱㅏㅌ" to "같",
            "ㄱㅗ↑⌫" to "ㄱ",        // ⌫ takes the vowel, one unit
            "ㄱㅏㅌ↑⌫" to "가ㅌ",    // ⌫ takes the ㅗ, as 가토 typed (FSS#122; 가 in FSS#109, 갈 while ㅌ ↑ was ㄾ)
        )
        val taps = mapOf("ㅗ↑" to "ㅛ", "ㅓ←" to "ㅕ", "ㅏ→" to "ㅑ", "ㅜ↓" to "ㅠ", "ㅌ↑" to "ㅌㅗ", "ㅌ←" to "ㅌㅓ")
        for ((keys, expected) in cases) {
            assertEquals(keys, expected, shownAfter(keys))
            // The same as the keys typed by taps (ㅌ ↑ as ㅌ ㅗ).
            val tapped = taps.entries.fold(keys) { s, (stroke, jamo) -> s.replace(stroke, jamo) }
            assertEquals("$keys = $tapped", shownAfter(tapped), shownAfter(keys))
        }
    }

    @Test
    fun `ID-42 compound finals are typed as their two consonants, no stroke types one any more (FSS 109, 124)`() {
        // FSS#124: the one-stroke compound finals (FR-TAP-02c ①) are gone; each stroke below is a consonant + vowel,
        // composed as the two keys tapped, and the compound finals come from two taps (갉 = ㄱㅏㄹㄱ).
        val cases = listOf(
            "ㄱㅏㄹㄱ" to "갉", "ㄱㅏㄹㅂ" to "갋", "ㄱㅏㄹㅅ" to "갌", "ㄱㅏㄹㅁ" to "갊", "ㄱㅏㄹㅎ" to "갏",
            "ㄱㅏㄹㅌ" to "갍", "ㄱㅏㄹㅍ" to "갎", "ㄱㅏㄱㅅ" to "갃", "ㄱㅏㄴㅈ" to "갅", "ㄱㅏㄴㅎ" to "갆", "ㄱㅏㅂㅅ" to "값",
            "ㄱㅏㄹ↑" to "가로", "ㄱㅏㄹ←" to "가러", "ㄱㅏㄹ→" to "가라", "ㄱㅏㄹ↓" to "가루",
            "ㄱㅏㄱ→" to "가가", "ㄱㅏㄴ↑" to "가노", "ㄱㅏㄴ→" to "가나", "ㄱㅏㅂ→" to "가바",
            "ㄱㅏㄹ↖" to "갈", "ㄱㅏㄹ↘" to "갈",       // a diagonal passed for ㄹ (4-way now) is the tap
            "ㄱㅏㄹㅂㅏ" to "갈바",                     // dokkaebibul: ㅂ of ㄼ goes on
            "ㄱㅏㄹㅌ⌫" to "갈", "ㄱㅏㅂㅅ⌫" to "갑",
        )
        val taps = mapOf(
            "ㄹ↑" to "ㄹㅗ", "ㄹ←" to "ㄹㅓ", "ㄹ→" to "ㄹㅏ", "ㄹ↓" to "ㄹㅜ", "ㄹ↖" to "ㄹ", "ㄹ↘" to "ㄹ",
            "ㄱ→" to "ㄱㅏ", "ㄴ↑" to "ㄴㅗ", "ㄴ→" to "ㄴㅏ", "ㅂ→" to "ㅂㅏ",
        )
        for ((keys, expected) in cases) {
            assertEquals(keys, expected, shownAfter(keys))
            val tapped = taps.entries.fold(keys) { s, (stroke, jamo) -> s.replace(stroke, jamo) }
            assertEquals("$keys = $tapped", shownAfter(tapped), shownAfter(keys))
        }
    }

    @Test
    fun `ID-45 a number key's tense consonant composes as one jamo, as ㄲ ㄸ ㅃ ㅆ ㅉ did on the consonant keys (FSS 124)`() {
        // SRS FR-TAP-02i: 4 ↓ ㄲ then ㅗ = 꼬, ⌫ ⌫ takes ㅗ then ㄲ (one unit each); after a final as typed.
        fun down(n: String) {
            val k = TapLayouts.DUBEOLSIK.keys.single { it.id == n }
            d.onTapOutput(k, TapKeyResolver.resolve(k, TapGesture.Exit(kbd.stroke.geom.Dir.DOWN)))
        }
        val tense = mapOf("1" to "빠", "2" to "짜", "3" to "따", "4" to "까", "5" to "싸")
        for ((n, syllable) in tense) {
            d.finishAll()
            dubeolsik()
            port.calls.clear()
            down(n)
            type("ㅏ")
            assertEquals(n, syllable, shownText(port.calls))
        }
        d.finishAll()
        dubeolsik()
        port.calls.clear()
        down("4")
        type("ㅗ")
        assertEquals("꼬", cc.composingShown)
        d.onFunctionDown(FunctionKey.BACKSPACE)
        assertEquals("ㄲ", cc.composingShown)
        d.onFunctionDown(FunctionKey.BACKSPACE)
        assertEquals("", cc.composingShown)
        // After a syllable: ㄲ and ㅆ can close it, ㅃ ㄸ ㅉ cannot (the composer's rule, as typed before FSS#124).
        d.finishAll()
        port.calls.clear()
        type("ㄱㅏ")
        down("4")
        assertEquals("갂", cc.composingShown)
        d.finishAll()
        port.calls.clear()
        type("ㄱㅏ")
        down("1")
        type("ㅏ")
        assertEquals("가빠", shownText(port.calls))
    }

    // ---- RT ----    // ---- RT ----

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
     * opens the settings, also while composing.
     */
    @Test
    fun `ID-27 the mode key up stroke commits the composing syllable, then opens the settings`() {
        var opened = 0
        val atOpen = mutableListOf<String>()
        d.onOpenSettings = { opened++; atOpen += port.calls }
        composeGo()
        port.calls.clear()
        modes.typed = false
        val before = modes.current
        d.onSettingsStroke()
        assertEquals(listOf("finish"), port.calls)               // 고 committed as it is, no space
        assertEquals(listOf("finish"), atOpen)                    // committed before the settings open
        assertFalse(d.composer!!.isComposing())
        assertEquals(0, feedback.errors)
        assertEquals(1, opened)
        assertFalse(modes.typed)                                  // not input, like the mode key
        assertEquals(before, modes.current)                       // no mode change
        assertTrue(shown.isEmpty())
        d.onSettingsStroke()                                      // nothing composing: opens again, nothing committed
        assertEquals(2, opened)
        assertEquals(listOf("finish"), port.calls)
        assertTrue(FunctionKey.entries.none { it.name == "SETTINGS" })   // no settings key: the mode key's up stroke opens them
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
    fun `ID-08 mode key finishes, switches mode and saves it`() {
        dubeolsik()
        type("ㄱㅏㄱ")
        port.calls.clear()
        d.onFunctionDown(FunctionKey.MODE)
        assertEquals(listOf("finish"), port.calls)
        assertEquals(InputMode.QWERTY, modes.current)     // typed, no previous → next in order (Dubeolsik → QWERTY, FSS#95)
        assertEquals(InputMode.DUBEOLSIK, modes.previous)
        assertFalse(modes.typed)                           // the mode key is not input
        assertEquals(listOf(InputMode.QWERTY), saved)
        assertEquals(listOf(InputMode.QWERTY), shown)
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
    fun `ID-12 onStartInputView order finishes and clears typed`() {
        composeGo()
        assertTrue(modes.typed)
        port.calls.clear()
        // LLD-08 §3.1 call order.
        d.field = field()
        d.finishAll()
        d.resetParsers()
        modes.onFieldChanged()
        assertEquals(listOf("finish"), port.calls)
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
        composeGo()
        port.calls.clear()
        port.throwOnBegin = true   // ⌫ has already taken ㅗ off the composer when the editor write fails
        assertNull(guard.stroke("Tap.fn") { d.onFunctionDown(FunctionKey.BACKSPACE) })
        assertEquals(emptyList<String>(), port.calls)
        assertEquals("고", d.composer!!.composingText())
        assertEquals(1, log.readAll().lines().count { it.isNotEmpty() })
    }

    @Test
    fun `ID-20 engine exception in a password field writes no log line`() {
        val log = ErrorLog(tmp.newFile("err.log"))
        val guard = EngineGuard(log, d)
        d.field = field(password = true)
        log.enabled = !d.field.noRecord                       // KeyboardService.onStartInputView (LLD-08 §3.1)
        composeGo()
        port.throwOnBegin = true
        assertNull(guard.stroke("Tap.fn") { d.onFunctionDown(FunctionKey.BACKSPACE) })
        assertEquals("", log.readAll())
    }

    @Test
    fun `ID-23 mode key in a numeric-like field switches once and does not save`() {
        modes.enterNumericField()
        assertEquals(InputMode.QWERTY, modes.current)
        d.onFunctionDown(FunctionKey.MODE)
        assertEquals(InputMode.GRAFFITI, modes.current)    // one onModeKey: QWERTY → next in order (graffiti, FSS#95)
        assertEquals(InputMode.DUBEOLSIK, modes.returnMode)
        assertEquals(emptyList<InputMode>(), saved)
        assertEquals(listOf(InputMode.GRAFFITI), shown)
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
    fun `EG-05 exception during finish while composing 고 keeps 고`() {
        val log = ErrorLog(tmp.newFile("err.log"))
        val guard = EngineGuard(log, d)
        composeGo()
        port.throwOnFinish = true
        assertNull(guard.stroke("Tap.fn") { d.onFunctionDown(FunctionKey.SPACE) })
        assertEquals("고", d.composer!!.composingText())
        assertEquals("고", cc.composingShown)
        assertEquals(1, log.readAll().lines().count { it.isNotEmpty() })
    }

    @Test
    fun `EG-05 restore also undoes a composer finish done before the failure`() {
        composeGo()
        val before = d.snapshot()
        d.composer!!.finish()
        d.restore(before)
        assertEquals("고", d.composer!!.composingText())
    }

    // ---- cursor moves (LLD-08 §6.1, §4.1; G13) ----

    /** CC-16 numbers: "가" + temporary glyph "ㆍ" at 5..7; the glyph path tells finishAfterUserMove from finishComposing. */
    private fun composingWithGlyph() {
        d.composer = HangulComposer(ComposePolicy.GRAFFITI)
        cc.onStart(5, 5)
        d.composer!!.apply(JamoEvent.Consonant(jamo('ㄱ')))
        d.composer!!.apply(JamoEvent.Vowel(jamo('ㅏ')))
        cc.show(ComposeResult("", "가"), "ㆍ")
        port.calls.clear()
    }

    @Test
    fun `onCursorMoved right after USER_MOVED drops the glyph, finishes and keeps the user position`() {
        composingWithGlyph()
        assertEquals(SelectionVerdict.USER_MOVED, cc.onUpdateSelection(9, 9, 5, 7))
        d.onCursorMoved(9, 9, 5, 7)
        assertEquals(listOf("begin", "compose(가)", "finish", "sel(8,8)", "end"), port.calls)
        assertFalse(d.composer!!.isComposing())
    }

    @Test
    fun `onCursorMoved with no reported span uses the known span end (G13)`() {
        composingWithGlyph()
        assertEquals(SelectionVerdict.USER_MOVED, cc.onUpdateSelection(9, 9, -1, -1))
        d.onCursorMoved(9, 9, -1, -1)
        assertEquals(listOf("begin", "compose(가)", "finish", "sel(8,8)", "end"), port.calls)
    }

    @Test
    fun `onCursorMoved is not input and onCursorMovedNoComposing writes nothing`() {
        composingWithGlyph()
        modes.typed = false
        cc.onUpdateSelection(2, 2, 5, 7)
        d.onCursorMoved(2, 2, 5, 7)
        assertFalse(modes.typed)
        port.calls.clear()
        d.onCursorMovedNoComposing()
        assertEquals(emptyList<String>(), port.calls)
        assertFalse(modes.typed)
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

    @Test
    fun `ID-43 a compound vowel in one stroke composes as its keys typed and backspace takes one stroke step (FSS 110)`() {
        // SRS FR-TAP-02g table and FR-HGL-04f: the visible text is the keys typed one after the other.
        val same = listOf(
            "ㄱㅗ→" to "과", "ㄱㅗ↓" to "괴", "ㄱㅗ↘" to "괘", "ㄱㅜ←" to "궈", "ㄱㅜ↑" to "귀", "ㄱㅜ↙" to "궤", "ㅇㅡ↓" to "의",
            "ㄱㅗ←" to "고", "ㄱㅗ↖" to "고", "ㄱㅜ→" to "구", "ㄱㅜ↘" to "구", "ㅇㅡ↑" to "으",   // empty ways: the tap
            "ㄱㅏㄱㅗ→" to "가과", "ㄱㅗ→ㅣ" to "과ㅣ", "ㅗ→" to "ㅘ", "ㄱㅗ→⌫" to "고", "ㄱㅜ↑⌫" to "구", "ㅇㅡ↓⌫" to "으",
            // ㅙ ㅞ backspace as their keys (owner 2026-11-15 "괘는 지우면 고가 되어야해"): 괘 → 고 → ㄱ, 궤 → 구 → ㄱ.
            "ㄱㅗ↘⌫" to "고", "ㄱㅗ↘⌫⌫" to "ㄱ", "ㄱㅜ↙⌫" to "구", "ㄱㅜ↙⌫⌫" to "ㄱ",
            "ㄱㅗ↘ㄱ⌫" to "괘", "ㄱㅗ↘ㄱ⌫⌫" to "고",                                         // 괙 → 괘 → 고
            "ㄱㅏㄱㅗ↘" to "가괘", "ㄱㅏㄱㅗ↘⌫" to "가고", "ㄱㅏㄱㅜ↙⌫" to "가구",            // dokkaebibul
            "ㄱㅗ↘⌫ㅐ" to "괘", "ㅗ↘⌫" to "ㅗ",
        )
        val taps = mapOf("ㅗ→" to "ㅗㅏ", "ㅗ↓" to "ㅗㅣ", "ㅗ↘" to "ㅗㅐ", "ㅜ←" to "ㅜㅓ", "ㅜ↑" to "ㅜㅣ", "ㅜ↙" to "ㅜㅔ",
            "ㅡ↓" to "ㅡㅣ", "ㅗ←" to "ㅗ", "ㅗ↖" to "ㅗ", "ㅜ→" to "ㅜ", "ㅜ↘" to "ㅜ", "ㅡ↑" to "ㅡ")
        for ((keys, expected) in same) {
            assertEquals(keys, expected, shownAfter(keys))
            val tapped = taps.entries.fold(keys) { s, (stroke, jamo) -> s.replace(stroke, jamo) }
            assertEquals("$keys = $tapped", shownAfter(tapped), shownAfter(keys))
        }
        // ⌫ one step at a time (owner "한 단계씩").
        val steps = listOf(
            "ㄱㅏㄱㅗ→⌫" to "가고", "ㅗ→⌫" to "ㅗ", "ㄱㅗ↘⌫ㅣ" to "괴", "ㄱㅗ→⌫ㅏ" to "과", "ㄱㅗㅐ⌫" to "고",
        )
        for ((keys, expected) in steps) assertEquals(keys, expected, shownAfter(keys))
    }

    @Test
    fun `ID-44 a consonant key's up, down, left and right compose as the consonant then the vowel tapped, two backspace units (FSS 122, 124)`() {
        // SRS FR-TAP-02h ②: 거 ⌫ = ㄱ, ⌫ = nothing; after a final as the keys would (각거, 갈거).
        val cases = listOf(
            "ㄱ←" to "거", "ㄱ→" to "가", "ㄱ↑" to "고", "ㄱ↓" to "구", "ㅁ↑" to "모", "ㅁ→" to "마", "ㅇ←" to "어", "ㅎ↓" to "후",
            "ㄷ→" to "다", "ㄷ↑" to "도", "ㅅ←" to "서", "ㅈ↓" to "주", "ㄴ←" to "너", "ㅂ↓" to "부", "ㅋ→" to "카",
            "ㅇ↑" to "오", "ㅇ↓" to "우", "ㅇ→" to "아", "ㄹ→" to "라", "ㅂ↑" to "보", "ㅅ↑" to "소", "ㅈ↑" to "조",   // FSS#124
            "ㄱ←⌫" to "ㄱ", "ㄱ←⌫⌫" to "", "ㅁ↑⌫" to "ㅁ",
            "ㄱㅏㄱㄱ←" to "각거", "ㄱㅏㄹㄱ←" to "갈거",
            "ㄱ←ㄴ" to "건", "ㄱ←ㄴㅁ↑" to "건모", "ㄱ←ㄹ↑" to "거로", "ㄱ←ㅏ" to "거ㅏ", "ㅗㄱ←" to "ㅗ거",
            "ㄱㅗㅁ→" to "고마", "ㄱㅏㅁ→" to "가마",
        )
        // Each stroke as its two keys tapped (FR-TAP-02h ②): every up/down/left/right of every consonant key (FSS#124).
        val vowel = mapOf('↑' to 'ㅗ', '↓' to 'ㅜ', '←' to 'ㅓ', '→' to 'ㅏ')
        val taps = "ㅂㅈㄷㄱㅅㅁㄴㅇㄹㅎㅋㅌㅊㅍ".flatMap { c -> vowel.map { (a, v) -> "$c$a" to "$c$v" } }.toMap()
        val arrows = "↑←→↓"
        fun sameAsTaps(keys: String) {
            val tapped = taps.entries.fold(keys) { s, (stroke, jamo) -> s.replace(stroke, jamo) }
            if (tapped.none { it in arrows }) assertEquals("$keys = $tapped", shownAfter(tapped), shownAfter(keys))
        }
        for ((keys, expected) in cases) {
            assertEquals(keys, expected, shownAfter(keys))
            sameAsTaps(keys)
        }
        // ⌫ after a final and dokkaebibul: unit by unit as the keys typed (no literal: the composer's own rule).
        for (keys in listOf("ㄱㅏㄹㄱ←⌫", "ㄱㅏㄹㄱ←⌫⌫", "ㄱㅏㄱㄱ←⌫", "ㄱㅏㄱㄱ←⌫⌫", "ㄱㅏㅁ→⌫", "ㄱㅏㅁ→⌫⌫⌫")) sameAsTaps(keys)
    }

    @Test
    fun `ID-46 a ㅓ ㅏ diagonal composes as the ㅔ ㅐ key typed - merge, dokkaebibul, backspace (FSS 126)`() {
        // SRS FR-TAP-02j: each case equals the same keys typed (ㅖ ㅒ are the ㅔ ㅐ keys ↑).
        val cases = listOf(
            "ㄱㅓ↙" to "게", "ㄱㅓ↖" to "계", "ㄱㅏ↘" to "개", "ㄱㅏ↗" to "걔",
            "ㄱㅓ↗" to "거", "ㄱㅓ↘" to "거", "ㄱㅏ↖" to "가", "ㄱㅏ↙" to "가",     // empty diagonals: the tap
            "ㄱㅗㅏ↘" to "괘", "ㄱㅜㅓ↙" to "궤",                                   // merge as 고 + ㅐ, 구 + ㅔ
            "ㄱㅗㅏ↗" to "고ㅒ", "ㄱㅜㅓ↖" to "구ㅖ",                               // no (ㅗ, ㅒ) or (ㅜ, ㅖ) merge
            "ㄱㅏㄱㅏ↘" to "가개", "ㅏ↘" to "ㅐ", "ㄱㅓ↙ㄴ" to "겐",
            "ㄱㅓ↙⌫" to "ㄱ", "ㄱㅏ↗⌫" to "ㄱ", "ㄱㅗㅏ↘⌫" to "고", "ㄱㅗㅏ↘⌫⌫" to "ㄱ", "ㄱㅏㄱㅏ↘⌫" to "가ㄱ",
            // ㅕ ↓ ㅖ, ㅑ ↓ ㅒ (FSS#126 addition).
            "ㄱㅕ↓" to "계", "ㄱㅑ↓" to "걔", "ㅇㅕ↓" to "예", "ㄱㅕ↓⌫" to "ㄱ", "ㄱㅏㄱㅑ↓" to "가걔", "ㄱㅗㅑ↓" to "고ㅒ",
        )
        val taps = mapOf("ㅕ↓" to "ㅔ↑", "ㅑ↓" to "ㅐ↑", "ㅓ↙" to "ㅔ", "ㅓ↖" to "ㅔ↑", "ㅏ↘" to "ㅐ", "ㅏ↗" to "ㅐ↑",
            "ㅓ↗" to "ㅓ", "ㅓ↘" to "ㅓ", "ㅏ↖" to "ㅏ", "ㅏ↙" to "ㅏ")
        for ((keys, expected) in cases) {
            assertEquals(keys, expected, shownAfter(keys))
            val tapped = taps.entries.fold(keys) { s, (stroke, jamo) -> s.replace(stroke, jamo) }
            assertEquals("$keys = $tapped", shownAfter(tapped), shownAfter(keys))
        }
    }

    @Test
    fun `ID-47 a consonant key's exit-and-return composes as the consonant then the paired vowel tapped, two backspace units (FSS 128)`() {
        // SRS FR-TAP-02h ③: 교 ⌫ = ㄱ, ⌫ = nothing; ㅛ ㅠ ㅕ ㅑ do not merge (FR-HGL-04a), dokkaebibul as the keys.
        val cases = listOf(
            "ㄱ↑↺" to "교", "ㄱ↓↺" to "규", "ㄱ←↺" to "겨", "ㄱ→↺" to "갸",
            "ㅇ↑↺" to "요", "ㅇ↓↺" to "유", "ㅇ←↺" to "여", "ㅇ→↺" to "야", "ㄹ↓↺" to "류", "ㅎ←↺" to "혀",
            "ㄱ↑↺⌫" to "ㄱ", "ㄱ↑↺⌫⌫" to "", "ㄱ←↺⌫" to "ㄱ",
            "ㄱ↑↺ㅏ" to "교ㅏ", "ㄱ↓↺ㅓ" to "규ㅓ", "ㄱ↓↺ㄹ" to "귤", "ㄱ←↺ㅇ" to "경",
            "ㄱㅏㄱㄱ←↺" to "각겨", "ㄱㅏㅁ→↺" to "가먀", "ㄱ←ㄱ←↺" to "거겨", "ㄱ↑↺ㄱ↑" to "교고",
        )
        val vowel = mapOf('↑' to 'ㅛ', '↓' to 'ㅠ', '←' to 'ㅕ', '→' to 'ㅑ')
        val taps = "ㅂㅈㄷㄱㅅㅁㄴㅇㄹㅎㅋㅌㅊㅍ".flatMap { c -> vowel.map { (a, v) -> "$c$a↺" to "$c$v" } }.toMap() +
            "ㅂㅈㄷㄱㅅㅁㄴㅇㄹㅎㅋㅌㅊㅍ".flatMap { c ->
                mapOf('↑' to 'ㅗ', '↓' to 'ㅜ', '←' to 'ㅓ', '→' to 'ㅏ').map { (a, v) -> "$c$a" to "$c$v" }
            }.toMap()
        for ((keys, expected) in cases) {
            assertEquals(keys, expected, shownAfter(keys))
            // Exit-and-return strokes first, so "ㄱ↑↺" is not read as "ㄱ↑" then ↺.
            val tapped = taps.entries.fold(keys) { s, (stroke, jamo) -> s.replace(stroke, jamo) }
            assertEquals("$keys = $tapped", shownAfter(tapped), shownAfter(keys))
        }
    }
}
