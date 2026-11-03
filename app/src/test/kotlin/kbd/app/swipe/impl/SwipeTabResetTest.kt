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
import kbd.hangul.event.JamoEvent
import kbd.hangul.jamo.Jamo
import kbd.stroke.layout.FunctionKey
import kbd.stroke.threshold.ThresholdDefaults
import org.junit.Assert.assertEquals
import org.junit.Test

/** LLD-11 §7.4 ID-26: TAB resets the real SwipeInputMapper too: its cheonjiin assembly closes (PM decision, PR #132; issue #168). */
class SwipeTabResetTest {

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

    private val feedback = object : FeedbackPort {
        override fun key() = Unit
        override fun error() = Unit
    }

    @Test
    fun `ID-26 TAB commits and closes both assemblies, unfinished strokes dropped with the error vibration`() {
        val port = Port()
        var errors = 0
        val fb = object : FeedbackPort { override fun key() = Unit; override fun error() { errors++ } }
        val d = InputDispatcher(ModeController(InputMode.entries.toList(), InputMode.SWIPE), ComposingController(port), fb) {}
        val mapper = SwipeInputMapper(ThresholdDefaults.VALUES)
        d.composer = HangulComposer(ComposePolicy.SWIPE)
        d.swipeState = SwipeMapperState(mapper)
        fun tap(down: Long) = mapper.accept(StrokeResult("ㄱ", SwipeKeyKind.CONSONANT, StrokeOutcome.Tap, down, null, down + 50))
        fun stroke(down: Long, s: kbd.hangul.stroke.VowelStroke) = mapper.accept(StrokeResult("ㄱ", SwipeKeyKind.CONSONANT, StrokeOutcome.Stroke(s), down, null, down + 100))
        // Against C = consonantBox(360, 48) ≈ (139, 76)–(221, 172) (centred, issue #213, FSS#37), t ≈ 20.6, b ≈ 161.7 (SWIPE 3.6.3, 9.5):
        // ㅣ right of C at C's height (x ≥ C.R − t/2, T < b), C's horizontal dot beside it (no vowel yet).
        val i = kbd.hangul.stroke.VowelStroke(kbd.hangul.stroke.StrokeAxis.VERTICAL, 235f, 56f, 235f, 176f)
        val dot = kbd.hangul.stroke.VowelStroke(kbd.hangul.stroke.StrokeAxis.HORIZONTAL, 225f, 100f, 245f, 100f)

        d.onJamoEvents((tap(0) as MapResult.Jamo).events)
        mapper.consonant = SwipeLayout.consonantBox(360f, 48f)        // as SwipeTouchLogic.down: a consonant composes
        d.onJamoEvents((stroke(100, i) as MapResult.Jamo).events)   // 기
        mapper.accept(StrokeResult(SwipeLayout.CHEONJIIN_ID, SwipeKeyKind.CHEONJIIN, StrokeOutcome.Element(CjElement.DOT), 200, null, 250))
        assertEquals("ㆍ", mapper.pendingGlyph())
        d.onFunctionDown(FunctionKey.TAB)
        assertEquals("", mapper.pendingGlyph())                          // the cheonjiin dot goes silently
        assertEquals(listOf("기", "	"), port.commits)
        assertEquals(0, errors)
        d.onJamoEvents((tap(280) as MapResult.Jamo).events)
        mapper.consonant = SwipeLayout.consonantBox(360f, 48f)        // as SwipeTouchLogic.down: a consonant composes
        d.onJamoEvents((stroke(300, dot) as MapResult.Jamo).events)  // an unfinished vowel stroke (ㄱㆍ)
        assertEquals("ㆍ", mapper.pendingGlyph())
        d.onFunctionDown(FunctionKey.TAB)
        assertEquals(1, errors)                                       // dropped with the error vibration (SWIPE 3.3)
        assertEquals("", mapper.pendingGlyph())
        assertEquals(MapResult.Jamo(listOf(JamoEvent.Consonant(Jamo.G))), tap(400))
    }
}
