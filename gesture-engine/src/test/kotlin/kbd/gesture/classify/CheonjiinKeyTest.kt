package kbd.gesture.classify

import kbd.gesture.classify.ClassifierTestSupport.D
import kbd.gesture.classify.ClassifierTestSupport.D_BACK
import kbd.gesture.classify.ClassifierTestSupport.L
import kbd.gesture.classify.ClassifierTestSupport.R
import kbd.gesture.classify.ClassifierTestSupport.R_BACK
import kbd.gesture.classify.ClassifierTestSupport.TAP
import kbd.gesture.classify.ClassifierTestSupport.U
import kbd.gesture.classify.ClassifierTestSupport.U_BACK
import kbd.gesture.classify.ClassifierTestSupport.U_RUB
import kbd.gesture.classify.ClassifierTestSupport.keyDef
import kbd.gesture.classify.ClassifierTestSupport.layoutGeom
import kbd.gesture.classify.ClassifierTestSupport.outcomeOf
import kbd.gesture.classify.ClassifierTestSupport.run
import kbd.gesture.classify.ClassifierTestSupport.stroke
import kbd.gesture.layout.SwipeLayout
import kbd.gesture.progress.ProgressState
import kbd.hangul.cheonjiin.CjElement
import kbd.stroke.geom.Dir
import kbd.stroke.testing.Leg
import kbd.stroke.threshold.ThresholdDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** LLD-11 §4.2 CK-01..CK-04: the swipe keyboard's cheonjiin key (SWIPE 5.4, FR-SWP-61, 65, issue #160). */
class CheonjiinKeyTest {

    private val id = SwipeLayout.CHEONJIIN_ID

    private fun el(e: CjElement) = StrokeOutcome.Element(e)

    @Test
    fun `CK-01 tap is dot, up and down are i, right is eu, left is the screen edge`() {
        assertEquals(el(CjElement.DOT), outcomeOf(id, TAP))
        assertEquals(el(CjElement.I), outcomeOf(id, U))
        assertEquals(el(CjElement.I), outcomeOf(id, D))   // ↓ = ㅣ (FR-SWP-61, design recommendation)
        assertEquals(el(CjElement.EU), outcomeOf(id, R))
        assertEquals(el(CjElement.DOT), outcomeOf(id, L))  // the left edge is outer: never crossed
    }

    @Test
    fun `CK-02 only the first exit counts - length, return, rub and turn are not looked at`() {
        assertEquals(el(CjElement.I), outcomeOf(id, listOf(Leg.U(120.0))))
        assertEquals(el(CjElement.I), outcomeOf(id, U_BACK))
        assertEquals(el(CjElement.I), outcomeOf(id, D_BACK))
        assertEquals(el(CjElement.I), outcomeOf(id, U_RUB))
        assertEquals(el(CjElement.EU), outcomeOf(id, R_BACK))
        assertEquals(el(CjElement.I), outcomeOf(id, listOf(Leg.U(42.0), Leg.R(60.0))))   // ↑ then a turn
        assertEquals(el(CjElement.EU), outcomeOf(id, listOf(Leg.R(45.0), Leg.U(80.0))))
        // Below the minimum crossing distance it is still a tap.
        assertEquals(el(CjElement.DOT), outcomeOf(id, listOf(Leg.U(30.0))))
        assertEquals(el(CjElement.DOT), outcomeOf(id, TAP, ThresholdDefaults.VALUES.copy(minCrossDp = 30f)))
    }

    @Test
    fun `CK-03 display text is the element`() {
        val k = keyDef(id)
        val c = StrokeClassifier(k, ThresholdDefaults.VALUES, layoutGeom(k))
        assertEquals("ㆍ", c.textOf(el(CjElement.DOT)))
        assertEquals("ㅣ", c.textOf(el(CjElement.I)))
        assertEquals("ㅡ", c.textOf(el(CjElement.EU)))
    }

    @Test
    fun `CK-04 guide on press - i up and down, eu right, nothing left, nothing after the exit`() {
        val k = keyDef(id)
        val g = layoutGeom(k)
        val c = StrokeClassifier(k, ThresholdDefaults.VALUES, g)
        c.begin(stroke(g, TAP).first())
        val ps = ProgressState.of(c)
        assertEquals("ㆍ", ps.current)
        assertTrue(ps.beforeExit)
        assertEquals(mapOf(Dir.UP to "ㅣ", Dir.DOWN to "ㅣ", Dir.RIGHT to "ㅡ"), ps.candidates.associate { it.dir to it.label })
        // After the exit the element is fixed: no next-character candidates.
        val c2 = StrokeClassifier(k, ThresholdDefaults.VALUES, g)
        val pts = stroke(g, U)
        c2.begin(pts.first())
        for (p in pts.drop(1)) c2.update(p)
        val after = ProgressState.of(c2)
        assertEquals("ㅣ", after.current)
        assertTrue(after.candidates.isEmpty())
        assertEquals(el(CjElement.I), run(StrokeClassifier(k, ThresholdDefaults.VALUES, g), pts).outcome)
    }
}
