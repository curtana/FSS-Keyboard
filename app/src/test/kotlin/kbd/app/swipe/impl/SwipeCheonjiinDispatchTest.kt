package kbd.app.swipe.impl

import kbd.app.feedback.FeedbackPort
import kbd.app.ime.ComposingController
import kbd.app.ime.EditorKey
import kbd.app.ime.EditorPort
import kbd.app.ime.InputDispatcher
import kbd.app.ime.InputMode
import kbd.app.ime.ModeController
import kbd.gesture.classify.StrokeOutcome
import kbd.gesture.classify.StrokeResult
import kbd.gesture.classify.SwipeKeyKind
import kbd.gesture.input.MapResult
import kbd.gesture.input.SwipeInputMapper
import kbd.gesture.layout.SwipeLayout
import kbd.hangul.cheonjiin.CjElement
import kbd.hangul.compose.ComposePolicy
import kbd.hangul.compose.HangulComposer
import kbd.hangul.jamo.Jamo
import kbd.stroke.layout.FunctionKey
import kbd.stroke.threshold.ThresholdDefaults
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * LLD-11 §7.4 ID-30..ID-33: the swipe cheonjiin key through the real `SwipeInputMapper`, `InputDispatcher`
 * and `ComposingController` (FR-SWP-62~64, FR-CMN-07b, issue #160): the temporary ㆍ·‥ on the editor,
 * ⌫ one element at a time, dropping the glyph on other input and commits.
 */
class SwipeCheonjiinDispatchTest {

    private class Port : EditorPort {
        val commits = ArrayList<String>()
        var composing = ""
        var deletes = 0
        override fun commitText(text: String) { commits += text }
        override fun setComposingText(text: String) { composing = text }
        override fun finishComposingText() { if (composing.isNotEmpty()) commits += composing; composing = "" }
        override fun deleteBefore() { deletes++ }
        override fun sendKey(key: EditorKey) = Unit
        override fun performEditorAction(actionId: Int) = Unit
        override fun setSelection(start: Int, end: Int) = Unit
        override fun beginBatch() = Unit
        override fun endBatch() = Unit
    }

    private val port = Port()
    private val cc = ComposingController(port)
    private var errors = 0
    private val feedback = object : FeedbackPort {
        override fun key() = Unit
        override fun error() { errors++ }
    }
    private val d = InputDispatcher(ModeController(InputMode.entries.toList(), InputMode.SWIPE), cc, feedback) {}
    private val mapper = SwipeInputMapper(ThresholdDefaults.VALUES)
    private var t = 0L

    init {
        d.composer = HangulComposer(ComposePolicy.SWIPE)
        d.swipeState = SwipeMapperState(mapper)
    }

    /** As `SwipeTouchLogic.release` does it. */
    private fun feed(r: StrokeResult) {
        when (val m = mapper.accept(r)) {
            is MapResult.Jamo -> d.onJamoEvents(m.events, m.errorVibe)
            MapResult.Ignored -> feedback.error()
            is MapResult.Text -> d.onSwipeText(m.text, m.errorVibe)
        }
        t += 300
    }

    private fun tap(key: String) = feed(StrokeResult(key, SwipeKeyKind.CONSONANT, StrokeOutcome.Tap, t, null, t + 50))

    /** A vowel stroke (issue #173): vertical at x from y0 to y1, or horizontal at y from x0 to x1. */
    private fun stroke(s: kbd.hangul.stroke.VowelStroke) = feed(StrokeResult("ㄱ", SwipeKeyKind.CONSONANT, StrokeOutcome.Stroke(s), t, null, t + 100))
    private fun vert(x: Float, y0: Float, y1: Float) = stroke(kbd.hangul.stroke.VowelStroke(kbd.hangul.stroke.StrokeAxis.VERTICAL, x, y0, x, y1))
    private fun horiz(y: Float, x0: Float, x1: Float) = stroke(kbd.hangul.stroke.VowelStroke(kbd.hangul.stroke.StrokeAxis.HORIZONTAL, x0, y, x1, y))

    private fun cj(vararg es: CjElement) = es.forEach {
        feed(StrokeResult(SwipeLayout.CHEONJIIN_ID, SwipeKeyKind.CHEONJIIN, StrokeOutcome.Element(it), t, null, t + 50))
    }

    @Test
    fun `ID-30 the pending dot shows after the composition and goes on the next input`() {
        tap("ㄱ")
        cj(CjElement.DOT)
        assertEquals("ㄱㆍ", port.composing)                  // FR-SWP-64: temporary glyph U+318D
        cj(CjElement.DOT)
        assertEquals("ㄱ‥", port.composing)                   // U+2025
        cj(CjElement.I)
        assertEquals("겨", port.composing)
        tap("ㄴ")
        cj(CjElement.DOT)
        assertEquals("견ㆍ", port.composing)
        tap("ㄱ")                                              // the dot is dropped silently (FR-HGL-04e)
        assertEquals("견ㄱ", port.commits.joinToString("") + port.composing)
    }

    @Test
    fun `ID-31 backspace takes back one element, then the composer, the closed vowel too`() {
        tap("ㄱ")
        cj(CjElement.I, CjElement.DOT)                          // 가
        d.backspace()
        assertEquals("기", port.composing)
        cj(CjElement.DOT)
        assertEquals("가", port.composing)
        tap("ㄴ")                                              // 간: the assembly closes
        d.backspace()
        assertEquals("가", port.composing)
        d.backspace()
        assertEquals("기", port.composing)                     // FR-SWP-64: 간 ⌫ = 가, ⌫ = 기
        d.backspace()
        assertEquals("ㄱ", port.composing)
        cj(CjElement.DOT, CjElement.I)                          // 거
        d.backspace()
        assertEquals("ㄱㆍ", port.composing)                   // FR-CMN-07b: 거 ⌫ = ㄱㆍ
        d.backspace()
        assertEquals("ㄱ", port.composing)
        assertEquals(0, port.deletes)
    }

    @Test
    fun `ID-32 space and enter commit without the dot, the next element starts a lone vowel`() {
        tap("ㄱ")
        cj(CjElement.I, CjElement.DOT)
        cj(CjElement.DOT, CjElement.DOT)                        // 갸, then ㅑ + ㆍ cannot extend
        assertEquals(1, errors)
        assertEquals("갸", port.composing)
        d.onFunctionDown(FunctionKey.SPACE)
        assertEquals(listOf("갸", " "), port.commits)
        cj(CjElement.DOT)
        assertEquals("ㆍ", port.composing)
        d.onFunctionDown(FunctionKey.ENTER)
        assertEquals(listOf("갸", " "), port.commits.take(2))
        assertEquals("", mapper.pendingGlyph())               // the pending dot is gone, not committed
        assertEquals(false, port.commits.any { it.contains('ㆍ') })
    }

    @Test
    fun `ID-33 vowel strokes and cheonjiin elements do not join`() {
        tap("ㄱ")
        vert(100f, 40f, 160f)
        horiz(100f, 100f, 125f)                                 // 가 by strokes (issue #173)
        cj(CjElement.DOT, CjElement.I)
        assertEquals("가ㅓ", port.commits.joinToString("") + port.composing)
        vert(100f, 40f, 160f)                                   // a stroke closes the cheonjiin assembly: a new vowel
        assertEquals("가ㅓㅣ", port.commits.joinToString("") + port.composing)
    }
}
