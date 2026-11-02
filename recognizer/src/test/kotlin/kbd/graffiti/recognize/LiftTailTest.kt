package kbd.graffiti.recognize

import kbd.graffiti.ink.InkPoint
import kbd.graffiti.ink.InkStroke
import kbd.graffiti.ink.PadArea
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Lift-off tail of the line rule (LLD-05 §3.1 step 2, FSS#47, LLD-11 §6.1 GR-37). */
class LiftTailTest {

    /** Polyline through [ys] at x = 100 (y down), points every 2 dp. */
    private fun vertical(vararg ys: Float): InkStroke {
        val pts = ArrayList<InkPoint>()
        var t = 0L
        for (k in 1 until ys.size) {
            val a = ys[k - 1]
            val b = ys[k]
            val n = (kotlin.math.abs(b - a) / 2f).toInt().coerceAtLeast(1)
            for (i in (if (k == 1) 0 else 1)..n) pts += InkPoint(100f, a + (b - a) * i / n, t++)
        }
        return InkStroke(pts, PadArea.LETTER, TestStrokes.LETTER_RECT)
    }

    @Test
    fun `GR-37 a down line with a short move back at the end is LineDown`() {
        // 140 dp down, 13 dp back up: S = 127 / 153 = 0.83 without the tail rule.
        assertEquals(StrokeToken.LineDown, StrokeRules.line(vertical(20f, 160f, 147f)))
        // Tail at the 20 % limit still counts as a lift-off tail.
        assertEquals(StrokeToken.LineDown, StrokeRules.line(vertical(20f, 120f, 100f)))
        // Same for → (ㅡ).
        val right = InkStroke(
            (0..70).map { InkPoint(20f + 2f * it, 100f, it.toLong()) } + (1..5).map { InkPoint(160f - 2f * it, 100f, 70L + it) },
            PadArea.LETTER, TestStrokes.LETTER_RECT,
        )
        assertEquals(StrokeToken.LineRight, StrokeRules.line(right))
    }

    @Test
    fun `GR-37 a longer move back is no lift-off tail - retracing strokes stay off the line rule`() {
        // 25 % back, half back (S ≈ 1/3) and fully back (a wedge).
        assertNull(StrokeRules.line(vertical(20f, 120f, 95f)))
        assertNull(StrokeRules.line(vertical(20f, 120f, 70f)))
        assertNull(StrokeRules.line(vertical(20f, 120f, 22f)))
    }
}
