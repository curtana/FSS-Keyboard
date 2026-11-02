package kbd.app.swipe.impl

import kbd.app.feedback.FeedbackPort
import kbd.app.ime.ComposingController
import kbd.app.ime.EditorKey
import kbd.app.ime.EditorPort
import kbd.app.ime.InputDispatcher
import kbd.app.ime.InputMode
import kbd.app.ime.ModeController
import kbd.app.tutor.HowTo
import kbd.app.tutor.TutorSteps
import kbd.app.tutor.PracticeLayouts
import kbd.app.tutor.RawStroke
import kbd.app.tutor.TutorFlow
import kbd.app.tutor.TutorSession
import kbd.app.tutor.TypingStats
import kbd.gesture.classify.StrokeClassifier
import kbd.gesture.input.MapResult
import kbd.gesture.input.SwipeInputMapper
import kbd.hangul.compose.ComposePolicy
import kbd.hangul.compose.HangulComposer
import kbd.stroke.geom.StrokePoint
import kbd.stroke.layout.FunctionKey
import kbd.stroke.threshold.ThresholdDefaults
import kbd.gesture.classify.SwipeKeyKind
import kbd.gesture.layout.SwipeLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * LLD-11 §7.5 TU-55..TU-57, ID-34 (issues #165, #168): after a wrong input — a stray tap, a swipe on another
 * key, a cheonjiin element that cannot extend — the next swipe types its syllable, in the tutor's lesson 5
 * and on the real keyboard; lesson 5 has a working ⌫ (one jamo at a time).
 */
class SwipeErrorRecoveryTest {

    private val width = 350f   // column width 50 dp
    private val rowH = 50f
    private val geo = SwipeGeometry.build(width, rowH)
    private var clock = 0L

    private fun rectOf(label: String) = geo.geoms[geo.keys.indexOfFirst { it.label == label }].rect

    /** A stroke from the centre of [label] by (dx, dy) dp; a tap when both are 0. */
    private fun stroke(label: String, dx: Float = 0f, dy: Float = 0f): RawStroke {
        val r = rectOf(label)
        val cx = (r.left + r.right) / 2; val cy = (r.top + r.bottom) / 2
        clock += 1000
        val pts = listOf(StrokePoint(cx, cy, clock), StrokePoint(cx + dx / 2 + 0.5f, cy + dy / 2, clock + 20), StrokePoint(cx + dx, cy + dy, clock + 40))
        clock += 40
        return RawStroke(r, pts, null, space = label == "스페이스", key = label, backspace = label == "⌫")
    }

    /** A stroke from (x0, y0) to (x1, y1) in keyboard dp; the key under its start is the key pressed. */
    private fun line(x0: Float, y0: Float, x1: Float, y1: Float): RawStroke {
        val i = geo.indexAt(x0, y0)
        clock += 1000
        val pts = listOf(StrokePoint(x0, y0, clock), StrokePoint((x0 + x1) / 2, (y0 + y1) / 2, clock + 20), StrokePoint(x1, y1, clock + 40))
        clock += 40
        return RawStroke(geo.geoms[i].rect, pts, null, key = geo.keys[i].label)
    }

    /*
     * Vowel strokes are judged against the drawn consonant C while a consonant composes (SWIPE 3.6.3, TBD-24).
     * Here C = consonantBox(350, 50) = (132.5, 54)–(217.5, 154) (centred, issue #213), t = 20, b = C.B − t/2 = 144
     * (SWIPE 9.5). So: ㅣ starts right of C at C's height (x 230 ≥ C.R − t/2, T 60 < b) on the ㄲ key; a dot (ㅏ's,
     * or a lone first dot) lies right of C at C's height (L 230 ≥ C.R − t, y 110 < b) on the ㄱ key.
     */
    private val iStroke = floatArrayOf(230f, 60f, 230f, 140f)
    private val dotStroke = floatArrayOf(230f, 110f, 255f, 110f)

    private fun line(s: FloatArray) = line(s[0], s[1], s[2], s[3])

    /** [s] drawn backwards (↑ or ←): ignored for its direction only (SWIPE 3.2), its place is a vowel's. */
    private fun reversed(s: FloatArray) = line(s[2], s[3], s[0], s[1])

    /** The canonical strokes of [v] (SWIPE 3.3, 3.6) at C's right (above): ㅣ, then ㅏ's dot from its middle. */
    private fun strokes(v: Char): List<RawStroke> = when (v) {
        'ㅣ' -> listOf(line(iStroke))
        'ㅏ' -> listOf(line(iStroke), line(dotStroke))
        else -> error("no shape for $v")
    }

    @Test
    fun `the strokes sit where SWIPE 3_6_3 reads them against C, on Hangul consonant keys`() {
        val c = geo.consonantBox
        val t = width / 7 * 0.4f
        val b = c.bottom - t / 2
        assertEquals(SwipeLayout.consonantBox(width, rowH), c)
        assertTrue("ㅣ: right of C", iStroke[0] >= c.right - t / 2)
        assertTrue("ㅣ: starts at C's height", iStroke[1] < b)
        assertTrue("dot: right of C", dotStroke[0] >= c.right - t)
        assertTrue("dot: at C's height", dotStroke[1] < b)
        for ((x, y) in listOf(iStroke[0] to iStroke[1], dotStroke[0] to dotStroke[1], iStroke[2] to iStroke[3]))
            assertEquals(SwipeKeyKind.CONSONANT, geo.keys[geo.indexAt(x, y)].kind)
    }

    private fun lesson5(): TutorFlow {
        val flow = TutorFlow(TutorSession(TutorSteps.ALL.filter { it.id == "5-1" }, ThresholdDefaults.VALUES), {
            PracticeLayouts.echo(it, SwipeEngineModule, ThresholdDefaults.VALUES, rowH)
        }) {}
        flow.startPractice()
        return flow
    }

    private fun TutorFlow.type(label: String) = onStroke(stroke(label), width)

    private fun TutorFlow.write(v: Char) = strokes(v).forEach { onStroke(it, width) }

    @Test
    fun `TU-55 lesson 5 - a stray tap and a reversed stroke do not stop the next syllable`() {
        val f = lesson5()
        f.type("ㄴ"); f.write('ㅏ')
        assertEquals("나", f.echoText)
        f.type("ㅇ")                                             // a stray tap: a final consonant
        assertEquals("낭", f.echoText)
        f.onStroke(reversed(iStroke), width)                     // ↑ at ㅣ's place: ignored for its direction (SWIPE 3.2)
        assertEquals("낭", f.echoText)
        f.write('ㅏ')                                             // ㅏ after the final: dokkaebibul
        assertEquals("나아", f.echoText)
        f.type("ㄱ"); f.write('ㅣ')
        assertEquals("나아기", f.echoText)   // the final moves to the new vowel (dokkaebibul, SWIPE 5.3 가기)
    }

    @Test
    fun `TU-56 lesson 5 - a dropped cheonjiin element and an unfinished stroke do not stop the next syllable, a double key types`() {
        val f = lesson5()
        f.type("ㅇ"); f.write('ㅏ')
        f.onStroke(stroke(HowTo.CHEONJIIN_KEY, 0f, -50f), width)   // cheonjiin ㅣ after a vowel: a lone ㅣ
        f.onStroke(stroke(HowTo.CHEONJIIN_KEY, 0f, -50f), width)   // ㅣ + ㅣ cannot extend: dropped
        f.type("ㄴ"); f.write('ㅏ')
        assertEquals("아ㅣ나", f.echoText)
        f.onStroke(line(dotStroke), width)                       // a first dot right of C: no vowel yet (ㅓ's ①)
        f.type("ㄲ"); f.write('ㅏ')                               // the tap drops it
        assertEquals("아ㅣ나까", f.echoText)
    }

    @Test
    fun `TU-57 lesson 5 backspace - one stroke while open, the stats use the final text`() {
        val f = lesson5()
        f.type("ㅇ"); f.write('ㅏ'); f.type("ㄱ"); f.write('ㅏ')     // 아가
        assertEquals("아가", f.echoText)
        f.type("⌫")                                             // the open ㅏ: one stroke back → 기
        assertEquals("아기", f.echoText)
        f.type("⌫")                                             // ㅣ → ㄱ
        assertEquals("아ㄱ", f.echoText)
        f.type("⌫"); f.type("⌫")                                // ㄱ, then the committed 아
        assertEquals("", f.echoText)
        f.type("ㅇ"); f.write('ㅏ'); f.type("ㅇ"); f.write('ㅣ')
        assertEquals("아이", f.echoText)
        f.nextSentence()
        val s = f.sentenceStats.last()
        // Speed and accuracy count the final line only (PM decision, issue #165): 아이 = 4 keystrokes.
        assertEquals(TypingStats.accuracy("아이가 고기를 먹어요", "아이"), s.accuracy, 1e-9)
        assertEquals(4, s.keystrokes)
    }

    // --- the real keyboard (InputDispatcher + SwipeInputMapper) ------------------------------------

    private class Port : EditorPort {
        val text = StringBuilder()
        var composing = ""
        override fun commitText(text: String) { this.text.append(text) }
        override fun setComposingText(text: String) { composing = text }
        override fun finishComposingText() { text.append(composing); composing = "" }
        override fun deleteBefore() { if (text.isNotEmpty()) text.setLength(text.length - 1) }
        override fun sendKey(key: EditorKey) = Unit
        override fun performEditorAction(actionId: Int) = Unit
        override fun setSelection(start: Int, end: Int) = Unit
        override fun beginBatch() = Unit
        override fun endBatch() = Unit
        val shown get() = text.toString() + composing
    }

    @Test
    fun `ID-34 the real keyboard types the next syllable after each kind of wrong input`() {
        val port = Port()
        val errors = intArrayOf(0)
        val feedback = object : FeedbackPort {
            override fun key() = Unit
            override fun error() { errors[0]++ }
        }
        val d = InputDispatcher(ModeController(InputMode.entries.toList(), InputMode.SWIPE), ComposingController(port), feedback) {}
        val mapper = SwipeInputMapper(ThresholdDefaults.VALUES)
        d.composer = HangulComposer(ComposePolicy.SWIPE)
        d.swipeState = SwipeMapperState(mapper)
        fun feed(raw: RawStroke) {
            val i = geo.indexAt(raw.points.first().x, raw.points.first().y)
            val k = geo.keys[i]
            if (k.function == FunctionKey.BACKSPACE) return d.backspace()
            // As SwipeTouchLogic.down: C while a consonant composes (the center consonant shows, TBD-24).
            mapper.consonant = if (d.composer?.lastConsonant() != null) geo.consonantBox else null
            val c = StrokeClassifier(k, ThresholdDefaults.VALUES, geo.geoms[i])
            c.begin(raw.points.first()); for (p in raw.points.subList(1, raw.points.size - 1)) c.update(p)
            when (val m = mapper.accept(c.finish(raw.points.last()))) {
                is MapResult.Jamo -> d.onJamoEvents(m.events, m.errorVibe)
                MapResult.Ignored -> feedback.error()
                is MapResult.Text -> d.onSwipeText(m.text, m.errorVibe)
            }
        }
        fun type(label: String) = feed(stroke(label))
        fun write(v: Char) = strokes(v).forEach(::feed)
        type("ㄴ"); write('ㅏ'); type("ㅇ")                                // 낭: a stray tap is a final consonant
        feed(reversed(iStroke))                                           // ↑ at ㅣ's place: ignored, error 1
        write('ㅏ')                                                        // 나아
        feed(stroke(HowTo.CHEONJIIN_KEY, 0f, -50f)); feed(stroke(HowTo.CHEONJIIN_KEY, 0f, -50f))   // ㅣ, ㅣ dropped: error 2
        type("ㄱ"); write('ㅏ')
        assertEquals("나아ㅣ가", port.shown)
        assertEquals(2, errors[0])
        feed(line(dotStroke)); type("ㄴ")                                 // an unfinished dot dropped by the tap: error 3
        assertEquals("나아ㅣ간", port.shown)
        assertEquals(3, errors[0])
    }
}
