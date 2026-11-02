package kbd.app.swipe.impl

import kbd.app.feedback.FeedbackPort
import kbd.app.ime.ComposingController
import kbd.app.ime.EditorKey
import kbd.app.ime.EditorPort
import kbd.app.ime.InputDispatcher
import kbd.app.ime.InputMode
import kbd.app.ime.ModeController
import kbd.app.view.CenterText
import kbd.gesture.classify.StrokeOutcome
import kbd.gesture.classify.StrokeResult
import kbd.gesture.classify.SwipeKeyKind
import kbd.gesture.input.MapResult
import kbd.gesture.input.SwipeInputMapper
import kbd.gesture.layout.SwipeLayout
import kbd.hangul.cheonjiin.CjElement
import kbd.hangul.compose.ComposePolicy
import kbd.hangul.compose.HangulComposer
import kbd.hangul.stroke.DotPlace
import kbd.hangul.stroke.StrokeAxis
import kbd.hangul.stroke.VowelStroke
import kbd.stroke.layout.FunctionKey
import kbd.stroke.threshold.ThresholdDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * LLD-11 §7.4 CT-01..CT-07: the center syllable `InputDispatcher.centerText` with the real `SwipeInputMapper` and
 * composer (SWIPE 9.5, FR-DSP-10, owner decisions 2026-11-02, FSS#36): always the one composing syllable, changed by a
 * tap, a released stroke, a cheonjiin element and ⌫; a lone first stroke's other role in brackets. Strokes are SWIPE
 * 3.6.5 (가) rows against C = consonantBox(360, 48) ≈ (139, 76)–(221, 172) (FSS#37).
 */
class SwipeCenterSyllableTest {

    private class Port : EditorPort {
        val commits = ArrayList<String>()
        var composing = ""
        override fun commitText(text: String) { commits += text }
        override fun setComposingText(text: String) { composing = text }
        override fun finishComposingText() { if (composing.isNotEmpty()) commits += composing; composing = "" }
        override fun deleteBefore() = Unit
        override fun sendKey(key: EditorKey) = Unit
        override fun performEditorAction(actionId: Int) = Unit
        override fun setSelection(start: Int, end: Int) = Unit
        override fun beginBatch() = Unit
        override fun endBatch() = Unit
    }

    private inner class Board {
        var errors = 0
        val port = Port()
        val mapper = SwipeInputMapper(ThresholdDefaults.VALUES)
        val d = InputDispatcher(ModeController(InputMode.entries.toList(), InputMode.SWIPE), ComposingController(port),
            object : FeedbackPort { override fun key() = Unit; override fun error() { errors++ } }) {}
        private var t = 0L

        init {
            d.composer = HangulComposer(ComposePolicy.SWIPE)
            d.swipeState = SwipeMapperState(mapper)
        }

        val center: CenterText? get() = d.centerText()

        private fun send(m: MapResult) {
            when (m) {
                is MapResult.Jamo -> d.onJamoEvents(m.events, m.errorVibe)
                MapResult.Ignored -> errors++
                is MapResult.Text -> d.onSwipeText(m.text, m.errorVibe)
            }
        }

        /** A tap on consonant key [id]. */
        fun tap(id: String) = also {
            send(mapper.accept(StrokeResult(id, SwipeKeyKind.CONSONANT, StrokeOutcome.Tap, t, null, t + 50)))
            t += 100
        }

        /** A released vowel stroke; C as `SwipeTouchLogic.down` sets it (a consonant composing = C). */
        fun stroke(s: VowelStroke) = also {
            mapper.consonant = if (d.composer?.lastConsonant() != null) C else null
            send(mapper.accept(StrokeResult("ㄱ", SwipeKeyKind.CONSONANT, StrokeOutcome.Stroke(s), t, null, t + 100)))
            t += 200
        }

        fun element(e: CjElement) = also {
            send(mapper.accept(StrokeResult(SwipeLayout.CHEONJIIN_ID, SwipeKeyKind.CHEONJIIN, StrokeOutcome.Element(e), t, null, t + 50)))
            t += 100
        }

        fun backspace() = also { d.onFunctionDown(FunctionKey.BACKSPACE) }
    }

    private fun v(x: Int, y0: Int, y1: Int) = VowelStroke(StrokeAxis.VERTICAL, x.toFloat(), y0.toFloat(), x.toFloat(), y1.toFloat())
    private fun h(y: Int, x0: Int, x1: Int) = VowelStroke(StrokeAxis.HORIZONTAL, x0.toFloat(), y.toFloat(), x1.toFloat(), y.toFloat())

    private val i = v(237, 80, 174)       // right-up: shows ㅣ, could be ㅗ·ㅛ's dot
    private val dotA = h(124, 237, 262)   // ㅏ's dot right of it

    @Test
    fun `CT-01 the owner's ㄱ ㅏ ㄱ ㅏ - ㄱ, 가, 각, 가 (도깨비불 leaves the new syllable only)`() {
        val b = Board().tap("ㄱ")
        assertEquals(CenterText("ㄱ"), b.center)
        b.stroke(i)
        assertEquals(CenterText("기", "ㄱ", DotPlace.UNDER), b.center)   // one stroke: both roles, the dot under ㄱ in brackets
        b.stroke(dotA)
        assertEquals(CenterText("가"), b.center)                        // the second stroke settles it
        b.tap("ㄱ")
        assertEquals(CenterText("각"), b.center)                        // the tap is a final: shown on the syllable
        b.stroke(i)
        assertEquals(listOf("가"), b.port.commits)                      // 각 + ㅣ = 가기
        assertEquals(CenterText("기", "각", DotPlace.UNDER), b.center)   // as a dot nothing would have committed: 각 with the dot
        b.stroke(dotA)
        assertEquals(CenterText("가"), b.center)
        assertEquals(0, b.errors)
    }

    @Test
    fun `CT-02 the other role of each lone first stroke - vowel or dot, under or right`() {
        assertEquals(CenterText("그", "ㄱ", DotPlace.RIGHT), Board().tap("ㄱ").stroke(h(134, 137, 197)).center)   // ㅡ / ㅓ·ㅕ's dot
        assertEquals(CenterText("ㄱㆍ", "그"), Board().tap("ㄱ").stroke(h(114, 137, 197)).center)                // a horizontal dot / ㅡ
        assertEquals(CenterText("ㄱㆍ", "기"), Board().tap("ㄱ").stroke(v(80, 80, 174)).center)                  // a vertical dot / ㅣ
        assertEquals(CenterText("ㄱㆍ", "기"), Board().tap("ㄱ").stroke(v(177, 176, 199)).center)
        // After a final the other vowel is what the composer would make of it: 각 + ㅣ = 가기, the syllable 기.
        assertEquals(CenterText("각ㆍ", "기"), Board().tap("ㄱ").stroke(i).stroke(dotA).tap("ㄱ").stroke(v(177, 176, 199)).center)
        // A double consonant key's consonant composes as itself.
        assertEquals(CenterText("끼", "ㄲ", DotPlace.UNDER), Board().tap("ㄲ").stroke(i).center)
    }

    @Test
    fun `CT-03 an ignored stroke changes nothing, the second stroke drops the bracket`() {
        val b = Board().tap("ㄱ").stroke(i)
        b.stroke(v(262, 80, 174))                                         // a second ㅣ: no candidate
        assertEquals(1, b.errors)
        assertEquals(CenterText("기", "ㄱ", DotPlace.UNDER), b.center)
        val o = Board().tap("ㄱ").stroke(v(177, 176, 199)).stroke(h(209, 137, 247))
        assertEquals(CenterText("고"), o.center)
    }

    @Test
    fun `CT-04 backspace shows what is left - 가 ⌫ 기 with its bracket again, ⌫ ㄱ, ⌫ nothing`() {
        val b = Board().tap("ㄱ").stroke(i).stroke(dotA)
        b.backspace()
        assertEquals(CenterText("기", "ㄱ", DotPlace.UNDER), b.center)   // the lone first stroke keeps both roles again
        b.backspace()
        assertEquals(CenterText("ㄱ"), b.center)
        b.backspace()
        assertNull(b.center)
        val k = Board().tap("ㄱ").stroke(i).stroke(dotA).tap("ㄴ")
        assertEquals(CenterText("간"), k.center)
        k.backspace()
        assertEquals(CenterText("가"), k.center)                         // SWIPE 9.5: 간 ⌫ = 가
    }

    @Test
    fun `CT-05 a bare vowel draws nothing, a cheonjiin element shows the syllable it makes`() {
        assertNull(Board().stroke(v(100, 40, 160)).center)               // no consonant: no C, nothing drawn
        assertNull(Board().center)
        val c = Board().tap("ㄱ").element(CjElement.I)
        assertEquals(CenterText("기"), c.center)                          // no stroke roles: no bracket
        c.element(CjElement.DOT)
        assertEquals(CenterText("가"), c.center)
    }

    @Test
    fun `CT-06 finishing clears it - space commits, nothing composing`() {
        val b = Board().tap("ㄱ").stroke(i)
        b.d.onFunctionDown(FunctionKey.SPACE)
        assertNull(b.center)
        assertEquals(listOf("기", " "), b.port.commits)
    }

    @Test
    fun `CT-07 error recovery restores the bracket's base with the composer`() {
        val b = Board().tap("ㄱ").stroke(i).stroke(dotA).tap("ㄱ").stroke(i)   // 기(각·), 가 committed
        val s = b.d.snapshot()
        b.backspace()
        b.backspace()
        b.d.restore(s)
        assertEquals(CenterText("기", "각", DotPlace.UNDER), b.center)
    }

    private companion object {
        val C = SwipeLayout.consonantBox(360f, 48f)
    }
}
