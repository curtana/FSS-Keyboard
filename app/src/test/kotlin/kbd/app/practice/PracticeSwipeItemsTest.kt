package kbd.app.practice

import kbd.app.feedback.FeedbackPort
import kbd.app.store.ErrorLog
import kbd.app.store.Settings
import kbd.app.swipe.impl.SwipeGeometry
import kbd.app.swipe.impl.SwipeMapperState
import kbd.gesture.classify.StrokeClassifier
import kbd.gesture.input.MapResult
import kbd.gesture.input.SwipeInputMapper
import kbd.gesture.layout.SwipeLayout
import kbd.stroke.geom.StrokePoint
import kbd.stroke.threshold.ThresholdDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * LLD-11 PR-10..PR-12 (FSS#17): the swipe items of topics ② and ④ written in their stroke order on the real swipe
 * engine (`SwipeInputMapper` + `InputDispatcher` + composer, as the practice keyboard wires them) leave exactly the
 * item's steps in the practice editor, so each action is judged ✓ in turn. Keyboard 350 × 6 × 50 dp: keys 50 dp wide,
 * t = 20 dp at run time, C = (132.5, 54)–(217.5, 154).
 */
class PracticeSwipeItemsTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val width = 350f
    private val rowH = 50f
    private val geo = SwipeGeometry.build(width, rowH)
    private var clock = 0L

    private class Run(val input: PracticeInput, val mapper: SwipeInputMapper)

    private fun run(): Run {
        val noFeedback = object : FeedbackPort {
            override fun key() = Unit
            override fun error() = Unit
        }
        val input = PracticeInput(PracticeBoard.SWIPE, noFeedback, ErrorLog(tmp.newFile()), Settings.DEFAULT)
        val mapper = SwipeInputMapper(ThresholdDefaults.VALUES)
        mapper.setKeyWidth(width / SwipeLayout.COLS)   // as SwipeKeyboardView.rebuildGeometry
        input.registerSwipeState(SwipeMapperState(mapper))
        return Run(input, mapper)
    }

    /** One touch from (x0, y0) to (x1, y1) as `SwipeTouchLogic` hands it to the mapper (cf. SwipeErrorRecoveryTest ID-34). */
    private fun Run.touch(x0: Float, y0: Float, x1: Float = x0, y1: Float = y0) {
        clock += 1000
        val pts = listOf(StrokePoint(x0, y0, clock), StrokePoint((x0 + x1) / 2 + 0.5f, (y0 + y1) / 2, clock + 20), StrokePoint(x1, y1, clock + 40))
        val i = geo.indexAt(x0, y0)
        val k = geo.keys[i]
        mapper.consonant = if (input.centerConsonant != null) geo.consonantBox else null
        val c = StrokeClassifier(k, input.thresholds, geo.geoms[i])
        c.begin(pts.first()); c.update(pts[1])
        when (val m = mapper.accept(c.finish(pts.last()))) {
            is MapResult.Jamo -> input.onSwipeEvents(m.events, m.errorVibe)
            MapResult.Ignored -> input.feedback.error()
            is MapResult.Text -> input.onSwipeText(m.text, m.errorVibe)
        }
    }

    private fun center(label: String): Pair<Float, Float> {
        val r = geo.geoms[geo.keys.indexOfFirst { it.label == label }].rect
        return (r.left + r.right) / 2 to (r.top + r.bottom) / 2
    }

    private fun Run.tap(label: String) = center(label).let { (x, y) -> touch(x, y) }
    private fun Run.swipe(label: String, dx: Float, dy: Float) = center(label).let { (x, y) -> touch(x, y, x + dx, y + dy) }

    /** Plays [actions] and checks the editor text after each against [item]'s steps, and the judge's ✓ at the end. */
    private fun check(item: PracticeItem, vararg actions: Run.() -> Unit) {
        val r = run()
        val seen = ArrayList<String>()
        var v = Verdict.PENDING
        actions.forEachIndexed { i, a ->
            r.input.errored = false
            r.a()
            seen += r.input.editor.text
            v = PracticeItemJudge.step(item, i, r.input.editor.text, r.input.errored)
        }
        assertEquals(item.target, item.steps, seen)
        assertEquals(item.target, Verdict.RIGHT, v)
    }

    private fun item(topic: PracticeTopic, target: String, note: String? = null) =
        topic.items.single { it.target == target && it.board == PracticeBoard.SWIPE && it.note == note }

    // Vowel strokes by C (SWIPE 3.6.3) at this size; every one starts on a Hangul consonant key.
    private val iRight: Run.() -> Unit = { touch(230f, 60f, 230f, 140f) }        // ㅣ right of C
    private val aDot: Run.() -> Unit = { touch(230f, 110f, 255f, 110f) }        // ㅏ's dot right of the ㅣ
    private val oDot: Run.() -> Unit = { touch(175f, 160f, 175f, 182f) }        // ㅗ's dot under C
    private val oEu: Run.() -> Unit = { touch(140f, 195f, 210f, 195f) }         // ㅡ under the dot
    private val oiI: Run.() -> Unit = { touch(255f, 130f, 255f, 220f) }         // ㅚ's ㅣ at ㅡ's right end, its middle right of C's 45° line
    private val waDot: Run.() -> Unit = { touch(262f, 175f, 285f, 175f) }       // ㅘ's dot right of that ㅣ
    private val euUnder: Run.() -> Unit = { touch(140f, 165f, 210f, 165f) }     // ㅡ under C
    private val uiI: Run.() -> Unit = { touch(218f, 110f, 218f, 175f) }         // ㅢ's ㅣ, the ㅡ in its lower part

    @Test
    fun `PR-10 swipe items of topic 4 leave their steps`() {
        val t = PracticeTopics.SWIPE
        check(item(t, "가"), { tap("ㄱ") }, iRight, aDot)
        check(item(t, "고"), { tap("ㄱ") }, oDot, oEu)
        check(item(t, "아"), { tap("ㅇ") }, iRight, aDot)
        check(item(t, "과"), { tap("ㄱ") }, oDot, oEu, oiI, waDot)
        check(item(t, "의"), { tap("ㅇ") }, euUnder, uiI)
    }

    @Test
    fun `PR-11 the cheonjiin item - consonant tap, cheonjiin key up, cheonjiin key tap`() {
        check(item(PracticeTopics.SWIPE, "가", "천지인 키"), { tap("ㄱ") }, { swipe(CHEONJIIN, 0f, -50f) }, { tap(CHEONJIIN) })
    }

    @Test
    fun `PR-12 swipe symbol items of topic 2`() {
        val t = PracticeTopics.SYMBOL
        check(item(t, "@", "긋기 자판"), { swipe("2", 0f, -40f) })
        check(item(t, "(", "긋기 자판"), { swipe(".", -40f, 0f) })
        check(item(t, "?", "긋기 자판"), { swipe("/", 0f, -40f) })
    }

    @Test
    fun `PR-13 a wrong consonant and a reversed stroke are wrong at once`() {
        val it = item(PracticeTopics.SWIPE, "가")
        val r = run()
        r.tap("ㄴ")
        assertEquals(Verdict.WRONG, PracticeItemJudge.step(it, 0, r.input.editor.text, r.input.errored))
        val r2 = run()
        r2.tap("ㄱ")
        r2.input.errored = false
        r2.touch(230f, 140f, 230f, 60f)   // ㅣ drawn upwards: ignored with the error vibration (SWIPE 3.2)
        assertTrue(r2.input.errored)
        assertEquals(Verdict.WRONG, PracticeItemJudge.step(it, 1, r2.input.editor.text, r2.input.errored))
    }

    private companion object {
        const val CHEONJIIN = "ㆍ ㅡ ㅣ"
    }
}
