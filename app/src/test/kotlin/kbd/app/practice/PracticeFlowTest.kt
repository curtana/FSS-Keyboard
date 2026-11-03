package kbd.app.practice

import kbd.app.feedback.FeedbackPort
import kbd.app.store.ErrorLog
import kbd.app.store.Settings
import kbd.stroke.geom.Dir
import kbd.stroke.layout.FunctionKey
import kbd.stroke.layout.TapLayouts
import kbd.stroke.tap.TapGesture
import kbd.stroke.tap.TapKeyResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** LLD-11 PR-01..PR-09, PR-14, PR-22 (FSS#17, FSS#43, FSS#54, SWIPE 10.0–10.2, FR-TUT-01·02·09·12): topics, judgement, flow, first open. */
class PracticeFlowTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `PR-01 four topics, 19 items, in the SWIPE 10_1 order`() {
        val t = PracticeTopics.ALL
        assertEquals(listOf("upper", "symbol", "graffiti", "swipe"), t.map { it.id })
        assertEquals(listOf(3, 6, 4, 6), t.map { it.items.size })
        assertEquals(19, t.sumOf { it.items.size })
        assertEquals(listOf("A", "Hi", "OK"), PracticeTopics.UPPER.items.map { it.target })
        assertEquals(listOf("!", "?", "[", "@", "(", "?"), PracticeTopics.SYMBOL.items.map { it.target })
        assertEquals(listOf(PracticeBoard.TAP_QWERTY, PracticeBoard.TAP_QWERTY, PracticeBoard.TAP_QWERTY,
            PracticeBoard.SWIPE, PracticeBoard.SWIPE, PracticeBoard.SWIPE), PracticeTopics.SYMBOL.items.map { it.board })
        assertTrue(PracticeTopics.GRAFFITI.items.all { it.board == PracticeBoard.GRAFFITI && it.guideCells.isNotEmpty() })
        assertTrue(t.all { it.intro.isNotEmpty() && it.items.all { i -> i.howTo.startsWith(i.target + " = ") } })
    }

    @Test
    fun `PR-14 graffiti and swipe topics show off in keyboard when their mode is off`() {
        val on = Settings.DEFAULT.enabledModes
        assertTrue(PracticeTopics.ALL.none { it.offInKeyboard(on) })
        val off = Settings.DEFAULT.copy(graffitiEnabled = false, swipeEnabled = false).enabledModes
        assertEquals(listOf("graffiti", "swipe"), PracticeTopics.ALL.filter { it.offInKeyboard(off) }.map { it.id })
        val noSwipe = Settings.DEFAULT.copy(swipeEnabled = false).enabledModes
        assertEquals(listOf("swipe"), PracticeTopics.ALL.filter { it.offInKeyboard(noSwipe) }.map { it.id })   // symbol's swipe items stay
    }

    @Test
    fun `PR-02 tap judgement by the text after backspace`() {
        val ok = PracticeTopics.UPPER.items[2]   // OK
        assertEquals(Verdict.PENDING, PracticeItemJudge.text(ok, ""))
        assertEquals(Verdict.PENDING, PracticeItemJudge.text(ok, "O"))
        assertEquals(Verdict.WRONG, PracticeItemJudge.text(ok, "Ok"))
        assertEquals(Verdict.RIGHT, PracticeItemJudge.text(ok, "OK"))
        assertEquals(Verdict.WRONG, PracticeItemJudge.text(ok, "OKK"))
    }

    @Test
    fun `PR-03 commit judgement - a composing syllable is no result, the space or the next syllable commits (FSS#43)`() {
        val ga = PracticeTopics.SWIPE.items[0]
        val go = PracticeTopics.SWIPE.items[1]
        @Suppress("UNUSED_PARAMETER") fun j(i: PracticeItem, committed: String, composing: String, error: Boolean = false) = PracticeItemJudge.commit(i, committed, error)   // composing: shown, never judged
        assertEquals(Verdict.PENDING, j(ga, "", "ㄱ"))
        assertEquals(Verdict.PENDING, j(ga, "", "기"))                 // 기 may still become 가
        assertEquals(Verdict.PENDING, j(ga, "", "가"))                 // not finished: it may become 각
        assertEquals(Verdict.RIGHT, j(ga, "가 ", ""))                  // the space commits; it is not judged
        assertEquals(Verdict.WRONG, j(ga, "기 ", ""))
        assertEquals(Verdict.WRONG, j(ga, "", "ㄱ", error = true))     // the error vibration is ✗ at once
        assertEquals(Verdict.PENDING, j(go, "", "기"))                 // the owner's 고 written as ㄱ ㅣ: no ✗ yet
        assertEquals(Verdict.PENDING, j(go, "", "ㄱㆍ"))
        assertEquals(Verdict.WRONG, j(go, "기", "가"))                  // the next syllable committed 기
        assertEquals(Verdict.RIGHT, j(ga, "가", "ㄸ"))                  // 가 committed by a consonant that is no final (ㄸ): ✓ as by the space
        assertEquals(Verdict.PENDING, j(ga, " ", ""))                  // a space alone commits nothing
        val gaga = PracticeItem("가가", PracticeBoard.SWIPE, "가가 = …", listOf("가가"))
        assertEquals(Verdict.PENDING, j(gaga, "", "가"))
        assertEquals(Verdict.PENDING, j(gaga, "", "각"))               // 가 → 각 → 가가: each step is still on the way
        assertEquals(Verdict.PENDING, j(gaga, "가", "가"))             // the first 가 committed by the second
        assertEquals(Verdict.RIGHT, j(gaga, "가가 ", ""))
        assertEquals(Verdict.WRONG, j(gaga, "각", "ㄱ"))               // 각 committed: no beginning of 가가
    }

    /** The tap keyboard as the practice wires it: the real resolver, the real dispatcher, ⌫ through the tap sink. */
    private fun tapInput() = PracticeInput(PracticeBoard.TAP_QWERTY, object : FeedbackPort {
        override fun key() = Unit
        override fun error() = Unit
    }, ErrorLog(tmp.newFile()), Settings.DEFAULT)

    private fun PracticeInput.key(label: String, g: TapGesture) {
        val k = TapLayouts.QWERTY.keys.first { it.label == label }
        tapSink.onTapOutput(k, TapKeyResolver.resolve(k, g))
    }

    @Test
    fun `PR-04 tap items on the real QWERTY tables, backspace fixes a wrong letter`() {
        val f = PracticeFlow(listOf(PracticeTopics.UPPER))
        f.open(PracticeTopics.UPPER)
        f.start()
        var i = tapInput()
        fun act() = f.onAction(i.editor.text, i.editor.display)
        i.key("a", TapGesture.Exit(Dir.UP)); act()
        assertEquals(Verdict.RIGHT, f.verdict)
        assertEquals(0, f.index)                                // A stays on screen with its ✓ (FSS#54)
        f.advance()                                             // the screen, RIGHT_HOLD_MS later
        assertEquals(1, f.index)
        i = tapInput()   // the screen builds a fresh keyboard for the next item
        f.onPress()
        i.key("h", TapGesture.Exit(Dir.UP)); act()
        i.key("o", TapGesture.Tap); act()                      // "Ho": ✗, the item is not cleared
        assertEquals(Verdict.WRONG, f.verdict)
        assertEquals("Hi = h 키에서 위로 → i 탭", f.howTo)
        val tryBefore = f.tryNo
        f.onPress()
        assertEquals(Verdict.WRONG, f.verdict)                  // stays while the text is wrong
        i.tapSink.onFunctionDown(FunctionKey.BACKSPACE); act()  // "H": the ✗ goes
        assertNull(f.verdict)
        assertEquals(tryBefore, f.tryNo)
        i.tapSink.onFunctionDown(FunctionKey.SPACE); act()      // other edit keys do nothing here
        assertEquals("H", i.editor.text)
        i.key("i", TapGesture.Tap); act()
        assertEquals(Verdict.RIGHT, f.verdict)
        assertNull(f.howTo)
    }

    @Test
    fun `PR-05 tap symbol items resolve as SWIPE 10_1 says`() {
        for ((label, g, out) in listOf(
            Triple("1", TapGesture.Exit(Dir.UP), "!"), Triple("/", TapGesture.Exit(Dir.UP), "?"), Triple("i", TapGesture.Exit(Dir.LEFT), "["),
        )) {
            val i = tapInput()
            i.key(label, g)
            assertEquals(label, out, i.editor.text)
        }
    }

    @Test
    fun `PR-06 a wrong swipe action shows the how-to and starts a new try, the next press clears the marks`() {
        val f = PracticeFlow(listOf(PracticeTopics.SWIPE))
        f.open(PracticeTopics.SWIPE)
        f.start()
        val t0 = f.tryNo
        f.onAction("ㄴ", "ㄴ")                                           // a committed ㄴ (no composing part)
        assertEquals(Verdict.WRONG, f.verdict)
        assertEquals("ㄴ", f.echo)
        assertEquals(t0 + 1, f.tryNo)
        f.onPress()
        assertNull(f.verdict)
        assertEquals("", f.echo)
        assertEquals(PracticeTopics.SWIPE.items[0].howTo, f.howTo)   // kept until the item is right
        f.onAction("ㄱ", "ㄱ", composing = "ㄱ")
        f.onAction("기", "기", error = true, reversed = true, composing = "기")
        assertEquals(PracticeTopics.REVERSE_HINT, f.hint)
        f.onPress()
        f.onAction("ㄱ", "ㄱ", composing = "ㄱ"); f.onAction("기", "기", composing = "기"); f.onAction("가", "가", composing = "가")
        assertNull(f.verdict)                                            // composing 가: not judged yet
        f.onAction("가 ", "가")                                          // the space
        assertEquals(Verdict.RIGHT, f.verdict)
        assertNull(f.howTo)
        assertNull(f.hint)
        f.advance()
        assertEquals(1, f.index)
    }

    @Test
    fun `PR-07 graffiti wrong highlights the target cells, right clears them`() {
        val f = PracticeFlow(listOf(PracticeTopics.GRAFFITI))
        f.open(PracticeTopics.GRAFFITI)
        f.start()
        f.onAction("ㄴ", "ㄴ")
        assertEquals(listOf("ㄱ", "ㅏ"), f.highlight)
        f.onAction("ㄱ", "ㄱ", composing = "ㄱ"); f.onAction("기", "기", composing = "기"); f.onAction("가", "가", composing = "가")
        f.onAction("가 ", "가")
        assertTrue(f.highlight.isEmpty())
    }

    @Test
    fun `PR-08 the last item ends the topic and marks it done, stop goes back to the list`() {
        val done = ArrayList<String>()
        val f = PracticeFlow(PracticeTopics.ALL) { done += it }
        assertEquals(PracticeFlow.Screen.LIST, f.screen)
        f.open(PracticeTopics.UPPER)
        assertEquals(PracticeFlow.Screen.INTRO, f.screen)
        f.start()
        for (target in listOf("A", "Hi", "OK")) { f.onAction(target, target); f.advance() }
        assertEquals(PracticeFlow.Screen.DONE, f.screen)
        assertEquals(listOf("upper"), done)
        f.stop()
        assertEquals(PracticeFlow.Screen.LIST, f.screen)
        f.open(PracticeTopics.SWIPE)
        f.start()
        f.stop()   // [그만] midway: nothing marked
        assertEquals(PracticeFlow.Screen.LIST, f.screen)
        assertEquals(listOf("upper"), done)
        assertNull(f.item)
    }

    @Test
    fun `PR-22 a right item stays with its check, takes no input, then the next one comes (FSS#54)`() {
        val f = PracticeFlow(listOf(PracticeTopics.SWIPE))
        f.open(PracticeTopics.SWIPE)
        f.start()
        f.onAction("가", "가", composing = "가")
        f.onAction("가 ", "가")                                          // the space: ✓
        val t0 = f.tryNo
        assertTrue(f.holding)
        assertEquals(Verdict.RIGHT, f.verdict)
        assertEquals("가", f.item?.target)                               // the solved target is still shown
        assertEquals("가", f.echo)
        f.onPress()                                                      // no input while it is shown
        f.onAction("ㄱ", "ㄱ", composing = "ㄱ")
        assertEquals(Verdict.RIGHT, f.verdict)
        assertEquals("가", f.echo)
        assertEquals(t0, f.tryNo)                                        // same keyboard, nothing new
        f.advance()
        assertFalse(f.holding)
        assertEquals("고", f.item?.target)
        assertNull(f.verdict)
        assertEquals("", f.echo)
        assertEquals(t0 + 1, f.tryNo)                                    // a fresh keyboard for 고
        f.advance()                                                      // a late second call does nothing
        assertEquals(1, f.index)
        f.onAction("가 ", "가")
        assertEquals(Verdict.WRONG, f.verdict)                           // 가 on 고 is ✗, with 고's how-to
        assertEquals(PracticeTopics.SWIPE.items[1].howTo, f.howTo)
        f.onAction("고 ", "고")
        assertTrue(f.holding)
        f.stop()                                                         // [그만] while it is shown: the hold ends
        assertFalse(f.holding)
        f.advance()
        assertEquals(PracticeFlow.Screen.LIST, f.screen)
    }

    @Test
    fun `PR-09 the first app open shows the topic list once`() {
        var marked = 0
        var opened = 0
        PracticeLaunchPolicy.onAppOpen(Settings.DEFAULT, { marked++ }, { opened++ })
        assertEquals(1 to 1, marked to opened)
        PracticeLaunchPolicy.onAppOpen(Settings.DEFAULT.copy(practiceListShown = true), { marked++ }, { opened++ })
        assertEquals(1 to 1, marked to opened)
        assertFalse(PracticeLaunchPolicy.shouldShowList(Settings.DEFAULT.copy(practiceListShown = true)))
    }
}
