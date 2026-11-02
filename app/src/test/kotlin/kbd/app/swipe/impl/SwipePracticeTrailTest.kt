package kbd.app.swipe.impl

import kbd.app.tutor.TutorSteps
import kbd.app.tutor.PracticeKeyboardView
import kbd.app.tutor.PracticeLayout
import kbd.app.tutor.PracticeLayouts
import kbd.app.tutor.TutorFlow
import kbd.app.tutor.TutorSession
import kbd.app.view.Touches
import kbd.app.view.ViewTestSdk
import kbd.app.view.layoutExactly
import kbd.gesture.layout.SwipeLayout
import kbd.stroke.threshold.ThresholdDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/**
 * Issue #217 (owner, tablet SM-T725N 711 dp wide, 360 dpi = 2.25 px/dp): the tutor's swipe practice keyboard wired as
 * `TutorActivity` wires it (the flow's echo, its open strokes as the trail, SWIPE 9.4) with synthetic touches in px.
 * Every stroke that leaves a not-yet-vowel state (ㆍ / ‥: ㅗ·ㅛ dots, ㅓ·ㅕ horizontal dots, ㅝ's dot) keeps its trail;
 * a stroke whose press → release displacement is under `minCrossDp` (8 dp, fixed since issue #217, compared in dp, not px) is a tap of the
 * key under it: no trail, a consonant. The practice echo writes the keyboard's `FSSStroke` line.
 *
 * C at 1600 px = 711.1 dp, 48 dp rows: (314.8, 52)–(396.4, 148), b = 138, t = 20. Strokes start on ㄷ (col 3 row 2), ㅇ (col 3 row 3), ㄲ (col 4 row 1).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ViewTestSdk.SDK], qualifiers = "w711dp-h1138dp-360dpi")
@LooperMode(LooperMode.Mode.PAUSED)
class SwipePracticeTrailTest {

    private val th = ThresholdDefaults.VALUES
    private val lines = ArrayList<String>()
    private val flow = TutorFlow(TutorSession(listOf(TutorSteps.STEP_5_1), th), {
        when (it) {
            PracticeLayout.SWIPE_CONSONANT_KEY, PracticeLayout.SWIPE_SENTENCE -> SwipePracticeEcho(th, ROW) { { l: String -> lines += l } }
            else -> PracticeLayouts.echo(it, SwipeEngineModule, th, ROW)
        }
    }) {}
    private val view = PracticeKeyboardView(RuntimeEnvironment.getApplication(), PracticeLayouts.of(PracticeLayout.SWIPE_SENTENCE, SwipeEngineModule),
        ROW, flow::onStroke, flow::onPress, trailOpen = { flow.openStrokes }, center = { flow.center })
    private val density = view.resources.displayMetrics.density
    private val touch = Touches(view)

    init {
        flow.startPractice()
        layoutExactly(view, WIDTH_PX, Math.round(SwipeLayout.ROWS * ROW * density))
    }

    /** A straight stroke in dp, 6 moves; returns the trail's path count after release. */
    private fun stroke(x0: Float, y0: Float, x1: Float, y1: Float): Int {
        touch.down(0, x0 * density, y0 * density)
        touch.move(0, x1 * density, y1 * density, steps = 6)
        touch.up(0)
        return view.trail.paths.size
    }

    private fun tapG(): Int = stroke(457f, 120f, 457f, 120f)   // ㄱ, col 4 row 2

    private fun echo(): String = flow.echoText

    @Test
    fun `the view works in dp at 2_25 px per dp`() {
        assertEquals(2.25f, density, 0.001f)
        assertEquals(711.1f, view.width / density, 0.05f)   // the owner's 1600 px
    }

    @Test
    fun `ㅗ - a short 15 dp dot keeps its trail (ㆍ), the line after it makes 고 with both trails`() {
        tapG()
        assertEquals(1, stroke(355f, 150f, 355f, 165f))
        assertTrue(echo(), echo().endsWith("ㄱㆍ"))
        assertEquals(2, stroke(314f, 185f, 396f, 185f))
        assertTrue(echo(), echo().endsWith("고"))
    }

    @Test
    fun `ㅛ - two short dots keep their trails (‥), the line makes 교`() {
        tapG()
        assertEquals(1, stroke(340f, 150f, 340f, 165f))
        assertEquals(2, stroke(372f, 150f, 372f, 165f))
        assertTrue(echo(), echo().endsWith("‥"))
        assertEquals(3, stroke(314f, 185f, 400f, 185f))
        assertTrue(echo(), echo().endsWith("교"))
    }

    @Test
    fun `ㅓ and ㅕ - horizontal dots keep their trails, the ㅣ makes 거 and 겨`() {
        tapG()
        assertEquals(1, stroke(400f, 100f, 418f, 100f))
        assertEquals(2, stroke(430f, 60f, 430f, 140f))
        assertTrue(echo(), echo().endsWith("거"))
        tapG()
        assertEquals(0, view.trail.paths.size)   // the tap closed the assembly
        assertEquals(1, stroke(400f, 100f, 418f, 100f))
        assertEquals(2, stroke(400f, 120f, 418f, 120f))
        assertEquals(3, stroke(430f, 60f, 430f, 140f))
        assertTrue(echo(), echo().endsWith("겨"))
    }

    @Test
    fun `ㅝ - ㅡ, dot and horizontal dot keep three trails, the ㅣ makes 궈`() {
        tapG()
        assertEquals(1, stroke(314f, 170f, 396f, 170f))
        assertEquals(2, stroke(355f, 180f, 355f, 196f))
        assertEquals(3, stroke(365f, 190f, 383f, 190f))
        assertEquals(4, stroke(400f, 60f, 400f, 210f))
        assertTrue(echo(), echo().endsWith("궈"))
    }

    @Test
    fun `a 7 dp dot is under minCrossDp 8 - a tap of the key under it (ㅇ), no trail, the line after it is ㅡ on that ㅇ`() {
        tapG()
        assertEquals(0, stroke(355f, 150f, 355f, 157f))
        assertTrue(echo(), echo().endsWith("ㄱㅇ"))
        assertTrue(lines.last(), lines.last().contains("practice tap key=ㅇ") && lines.last().contains("len=7.0 minCross=8.0"))
        assertEquals(1, stroke(314f, 185f, 396f, 185f))
        assertTrue(echo(), echo().endsWith("ㄱ으"))
    }

    @Test
    fun `the practice echo writes the FSSStroke line with the role by C`() {
        tapG()
        stroke(355f, 150f, 355f, 165f)
        val l = lines.last()
        assertTrue(l, l.startsWith("practice stroke key=ㅇ view=711.1x288.0"))
        assertTrue(l, l.contains("C=(314.8,52.0,396.4,148.0) t=20.0 center=ㄱ shape=vertical len=15.0 minCross=8.0 role=first vertical under-left: shows a dot, both roles kept (P (355.0,157.5), d 50.9;"))
        assertTrue(l, l.endsWith("assembly=V1[ㅣㅏㅑㅐㅒㅗㅛㅚㅘㅙ] vowel=- pending=ㆍ"))
    }

    private companion object {
        const val WIDTH_PX = 1600   // SM-T725N portrait at 2.25 px/dp = 711.1 dp
        const val ROW = 48f
    }
}
