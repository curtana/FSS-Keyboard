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
 * LLD-11 §7.4 CT-01..CT-06: the center syllable `InputDispatcher.centerText` with the real `SwipeInputMapper` and
 * composer (SWIPE 9.5, FR-DSP-10, owner decisions 2026-11-02, FSS#36): always the one composing syllable, no candidates,
 * changed by a tap, a released stroke, a cheonjiin element and ⌫; strokes that make no vowel yet as dots in place. Strokes are SWIPE
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

    private val i = v(237, 80, 174)       // right-up: ㅣ
    private val dotA = h(124, 237, 262)   // ㅏ's dot right of it
    private val U = DotPlace.UNDER
    private val R = DotPlace.RIGHT

    @Test
    fun `CT-01 the owner's ㄱ ㅏ ㄱ ㅏ - ㄱ, 가, 각, 가 (도깨비불 leaves the new syllable only)`() {
        val b = Board().tap("ㄱ")
        assertEquals(CenterText("ㄱ"), b.center)
        b.stroke(i)
        assertEquals(CenterText("기"), b.center)                        // one syllable, no candidates
        b.stroke(dotA)
        assertEquals(CenterText("가"), b.center)
        b.tap("ㄱ")
        assertEquals(CenterText("각"), b.center)                        // the tap is a final: shown on the syllable
        b.stroke(i)
        assertEquals(listOf("가"), b.port.commits)                      // 각 + ㅣ = 가기
        assertEquals(CenterText("기"), b.center)
        b.stroke(dotA)
        assertEquals(CenterText("가"), b.center)
        assertEquals(0, b.errors)
    }

    @Test
    fun `CT-02 dots in place until a vowel is made - 고 교 under, 거 겨 right (owner 2026-11-02)`() {
        val go = Board().tap("ㄱ").stroke(v(177, 176, 199))
        assertEquals(CenterText("ㄱ", listOf(U)), go.center)          // the editor shows ㄱㆍ; C shows ㄱ with a dot under it
        assertEquals(CenterText("고"), go.stroke(h(209, 137, 247)).center)
        val gyo = Board().tap("ㄱ").stroke(v(167, 176, 196)).stroke(v(192, 176, 196))
        assertEquals(CenterText("ㄱ", listOf(U, U)), gyo.center)       // two side by side
        assertEquals(CenterText("교"), gyo.stroke(h(209, 137, 227)).center)
        val geo = Board().tap("ㄱ").stroke(h(124, 227, 247))
        assertEquals(CenterText("ㄱ", listOf(R)), geo.center)         // a dot right of it
        assertEquals(CenterText("거"), geo.stroke(v(252, 80, 184)).center)
        val gyeo = Board().tap("ㄱ").stroke(h(114, 227, 252)).stroke(h(144, 227, 252))
        assertEquals(CenterText("ㄱ", listOf(R, R)), gyeo.center)      // two, one above the other
        assertEquals(CenterText("겨"), gyeo.stroke(v(257, 80, 184)).center)
        assertEquals(0, go.errors + gyo.errors + geo.errors + gyeo.errors)
    }

    @Test
    fun `CT-03 the first stroke shows its likeliest syllable, the second stroke changes it - 그 → 고, 그 → ㄱ two dots → 겨`() {
        val b = Board().tap("ㄱ").stroke(h(184, 137, 227))
        assertEquals(CenterText("그"), b.center)                        // shown as ㅡ, no bracket
        b.stroke(h(204, 137, 227))                                       // ㅕ ②: the first line becomes a dot
        assertEquals(CenterText("ㄱ", listOf(R, R)), b.center)          // the ‥ hides the composer's ㅡ: ㄱ and two dots
        assertEquals(CenterText("겨"), b.stroke(v(240, 80, 224)).center)
        val o = Board().tap("ㄱ").stroke(i).stroke(h(194, 200, 300))     // ㅣ re-read as ㅗ's dot by the underline
        assertEquals(CenterText("고"), o.center)
        // After a final: 각 + ㅡ = 가그 (가 committed), then the second line → ㄱ and two dots, not 각.
        val k = Board().tap("ㄱ").stroke(i).stroke(dotA).tap("ㄱ").stroke(h(184, 137, 227))
        assertEquals(CenterText("그"), k.center)
        assertEquals(CenterText("ㄱ", listOf(R, R)), k.stroke(h(204, 137, 227)).center)
        assertEquals(CenterText("구", listOf(R)), Board().tap("ㄱ").stroke(h(184, 137, 247)).stroke(v(177, 192, 219)).stroke(h(209, 187, 212)).center)   // ㅝ on its way
    }

    @Test
    fun `CT-04 an ignored stroke changes nothing, backspace shows what is left`() {
        val b = Board().tap("ㄱ").stroke(i)
        b.stroke(v(262, 80, 174))                                        // a second ㅣ: no candidate
        assertEquals(1, b.errors)
        assertEquals(CenterText("기"), b.center)
        val g = Board().tap("ㄱ").stroke(v(167, 176, 196)).stroke(v(192, 176, 196))
        g.backspace()
        assertEquals(CenterText("ㄱ", listOf(U)), g.center)            // one dot taken back
        g.backspace()
        assertEquals(CenterText("ㄱ"), g.center)
        g.backspace()
        assertNull(g.center)
        val k = Board().tap("ㄱ").stroke(i).stroke(dotA).tap("ㄴ")
        assertEquals(CenterText("간"), k.center)
        k.backspace()
        assertEquals(CenterText("가"), k.center)                         // SWIPE 9.5: 간 ⌫ = 가
        k.backspace()
        assertEquals(CenterText("기"), k.center)                         // a closed vowel: one composer step (SWIPE 3.5)
    }

    @Test
    fun `CT-05 a bare vowel draws nothing, a cheonjiin element shows the syllable it makes, its ㆍ as text`() {
        assertNull(Board().stroke(v(100, 40, 160)).center)               // no consonant: no C, nothing drawn
        assertNull(Board().center)
        val c = Board().tap("ㄱ").element(CjElement.DOT)
        assertEquals(CenterText("ㄱㆍ"), c.center)                        // a cheonjiin dot is no stroke: as in the editor
        c.element(CjElement.I)
        assertEquals(CenterText("거"), c.center)
    }

    @Test
    fun `CT-06 finishing clears it - space commits, nothing composing`() {
        val b = Board().tap("ㄱ").stroke(v(177, 176, 199))
        b.d.onFunctionDown(FunctionKey.SPACE)
        assertNull(b.center)
        assertEquals(1, b.errors)                                        // the unfinished dot is dropped (SWIPE 3.3)
    }

    private companion object {
        val C = SwipeLayout.consonantBox(360f, 48f)
    }
}
