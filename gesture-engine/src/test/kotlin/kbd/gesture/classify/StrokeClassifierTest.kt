package kbd.gesture.classify

import kbd.gesture.classify.ClassifierTestSupport.keyDef
import kbd.gesture.classify.ClassifierTestSupport.run
import kbd.hangul.stroke.StrokeAxis
import kbd.hangul.stroke.VowelStroke
import kbd.stroke.geom.KeyGeom
import kbd.stroke.geom.RectDp
import kbd.stroke.geom.StrokePoint
import kbd.stroke.threshold.ThresholdDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * LLD-11 §4.1 SC-01..SC-12: the consonant key's stroke judgement in its order (SWIPE 3.1–3.2, 3.6,
 * HLD 4.2, issue #173) — ① tap or stroke (minCrossDp 8, fixed, issue #217), ② axis (larger component), ③ direction
 * (→ ↓ only); no length step (issue #179) — with a test at each boundary, and §7.5 TS-08.
 */
class StrokeClassifierTest {

    private val gKey = keyDef("ㄱ")
    private val geom = KeyGeom("ㄱ", RectDp(100f, 100f, 151f, 148f), emptySet())
    private val x0 = 125f
    private val y0 = 124f

    /** Press at the key's centre, a wiggle, release at (dx, dy) from the press. */
    private fun outcome(dx: Float, dy: Float, wiggle: Boolean = true): StrokeOutcome {
        val pts = buildList {
            add(StrokePoint(x0, y0, 0L))
            if (wiggle) add(StrokePoint(x0 - 30f, y0 + 40f, 20L))   // the path in between does not count
            add(StrokePoint(x0 + dx, y0 + dy, 40L))
        }
        return run(StrokeClassifier(gKey, ThresholdDefaults.VALUES, geom), pts).outcome
    }

    private fun stroke(axis: StrokeAxis, dx: Float, dy: Float) =
        StrokeOutcome.Stroke(VowelStroke(axis, x0, y0, x0 + dx, y0 + dy))

    @Test fun `SC-01 tap - no movement, a wiggle back to the start, under minCrossDp`() {
        assertEquals(StrokeOutcome.Tap, outcome(0f, 0f, wiggle = false))
        assertEquals(StrokeOutcome.Tap, outcome(0f, 0f))
        assertEquals(StrokeOutcome.Tap, outcome(7.9f, 7.9f))
    }

    @Test fun `SC-02 step 1 boundary - 8 dp is a stroke, 7_9 a tap`() {
        assertEquals(StrokeOutcome.Tap, outcome(7.9f, 0f))
        assertEquals(stroke(StrokeAxis.HORIZONTAL, 8f, 0f), outcome(8f, 0f))
        assertEquals(StrokeOutcome.Tap, outcome(0f, 7.9f))
        assertEquals(stroke(StrokeAxis.VERTICAL, 0f, 8f), outcome(0f, 8f))
    }

    @Test fun `SC-03 step 2 axis - the larger component, a diagonal is horizontal`() {
        assertEquals(stroke(StrokeAxis.HORIZONTAL, 30f, 29f), outcome(30f, 29f))
        assertEquals(stroke(StrokeAxis.VERTICAL, 29f, 30f), outcome(29f, 30f))
        assertEquals(stroke(StrokeAxis.HORIZONTAL, 30f, 30f), outcome(30f, 30f))
    }

    @Test fun `SC-04 step 3 direction - left and up are reversed`() {
        assertEquals(StrokeOutcome.Reverse, outcome(-30f, 0f))
        assertEquals(StrokeOutcome.Reverse, outcome(0f, -30f))
        assertEquals(StrokeOutcome.Reverse, outcome(-30f, 29f))   // horizontal axis, leftward
        assertEquals(StrokeOutcome.Reverse, outcome(29f, -30f))   // vertical axis, upward
        assertEquals(StrokeOutcome.Reverse, outcome(-14f, 0f))
    }

    @Test fun `SC-05 no length step - any stroke length gives the same plain stroke (issue 179)`() {
        assertEquals(stroke(StrokeAxis.HORIZONTAL, 51f, 0f), outcome(51f, 0f))
        assertEquals(stroke(StrokeAxis.HORIZONTAL, 50.9f, 0f), outcome(50.9f, 0f))
        assertEquals(stroke(StrokeAxis.VERTICAL, 0f, 51f), outcome(0f, 51f))
        assertEquals(stroke(StrokeAxis.VERTICAL, 0f, 50.9f), outcome(0f, 50.9f))
        assertEquals(stroke(StrokeAxis.VERTICAL, 10f, 120f), outcome(10f, 120f))   // far off the key: the key does not matter
    }

    @Test fun `SC-06 the key it starts on does not matter, a double key too`() {
        val kk = keyDef("ㄲ")
        val r = run(StrokeClassifier(kk, ThresholdDefaults.VALUES, geom), listOf(StrokePoint(x0, y0, 0L), StrokePoint(x0, y0 + 80f, 40L)))
        assertEquals(stroke(StrokeAxis.VERTICAL, 0f, 80f), r.outcome)
        assertEquals("ㄲ", r.keyId)
    }

    @Test fun `SC-07 progress - current is the consonant before the stroke, the preview after, beforeExit until minCrossDp`() {
        val c = StrokeClassifier(gKey, ThresholdDefaults.VALUES, geom) { s -> if (s.vertical && s.span >= 51f) "ㅣ" else "ㆍ" }
        c.begin(StrokePoint(x0, y0, 0L))
        val still = c.update(StrokePoint(x0, y0 + 5f, 10L))
        assertEquals("ㄱ", still.current)
        assertTrue(still.beforeExit)
        assertEquals(setOf("ㅣ", "ㆍ"), still.candidates.map { it.label }.toSet())   // ↓ / → dot and long strokes
        val dot = c.update(StrokePoint(x0, y0 + 20f, 20L))
        assertEquals("ㆍ", dot.current)
        assertFalse(dot.beforeExit)
        assertEquals("ㅣ", c.update(StrokePoint(x0, y0 + 60f, 30L)).current)
        val back = c.update(StrokePoint(x0, y0 - 20f, 40L))   // reversed: nothing to show
        assertEquals("", back.current)
    }

    @Test fun `SC-08 a long press then lift is a plain tap (issue 142)`() {
        val r = run(StrokeClassifier(gKey, ThresholdDefaults.VALUES, geom),
            listOf(StrokePoint(x0, y0, 0L), StrokePoint(x0, y0, 900L), StrokePoint(x0, y0, 950L)))
        assertEquals(StrokeOutcome.Tap, r.outcome)
        assertEquals("ㄱ", StrokeClassifier(gKey, ThresholdDefaults.VALUES, geom).textOf(r.outcome))
    }

    @Test fun `SC-09 records down and up time`() {
        val r = run(StrokeClassifier(gKey, ThresholdDefaults.VALUES, geom), listOf(StrokePoint(x0, y0, 5L), StrokePoint(x0 + 30f, y0, 45L)))
        assertEquals(5L, r.downTime)
        assertEquals(45L, r.upTime)
    }

    /** TS-08: thresholds are absolute dp — the same movement gives the same result on a 51 and a 70 dp key. */
    @Test fun `TS-08 same movement on 51 and 70 dp wide keys gives the same result`() {
        for (w in listOf(51f, 70f)) {
            val g = KeyGeom("ㄱ", RectDp(0f, 0f, w, 48f), emptySet())
            for (d in listOf(14f, 50.9f, 51f, 200f)) {
                val r = run(StrokeClassifier(gKey, ThresholdDefaults.VALUES.copy(keySizeWDp = w), g),
                    listOf(StrokePoint(10f, 10f, 0L), StrokePoint(10f + d, 10f, 40L)))
                assertEquals("$d on $w", StrokeOutcome.Stroke(VowelStroke(StrokeAxis.HORIZONTAL, 10f, 10f, 10f + d, 10f)), r.outcome)
            }
        }
    }
}
