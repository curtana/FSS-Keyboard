package kbd.stroke.tap

import kbd.stroke.geom.Dir
import kbd.stroke.geom.Edge
import kbd.stroke.geom.KeyGeom
import kbd.stroke.geom.RectDp
import kbd.stroke.testing.Leg
import kbd.stroke.testing.Trajectories
import kbd.stroke.threshold.ThresholdDefaults
import org.junit.Assert.assertEquals
import org.junit.Test

/** LLD-11 §5.1 TG-01..TG-10: key 36×48, default thresholds, strokes start at the key centre. */
class TapGestureClassifierTest {

    private val rect = RectDp(0f, 0f, 36f, 48f)
    private val inner = KeyGeom("k", rect, emptySet())
    /** A bottom-row key: its BOTTOM edge lies on the window border (TG-08). */
    private val bottomRow = KeyGeom("k", rect, setOf(Edge.BOTTOM))

    private fun classify(vararg legs: Leg, key: KeyGeom = inner): TapGesture {
        val points = Trajectories.expand(18.0, 24.0, 0L, legs.toList())
        return TapGestureClassifier(key, ThresholdDefaults.VALUES.minCrossDp).run {
            begin(points.first())
            points.drop(1).dropLast(1).forEach { add(it) }
            finish(points.last())
        }
    }

    @Test fun `TG-01 no movement is Tap`() = assertEquals(TapGesture.Tap, classify())

    @Test fun `TG-02 U40 is Exit(UP)`() = assertEquals(TapGesture.Exit(Dir.UP), classify(Leg.U(40.0)))

    @Test fun `TG-03 D40 is Exit(DOWN)`() = assertEquals(TapGesture.Exit(Dir.DOWN), classify(Leg.D(40.0)))

    @Test
    fun `TG-04 L35 is Exit(LEFT), R35 is Exit(RIGHT)`() {
        assertEquals(TapGesture.Exit(Dir.LEFT), classify(Leg.L(35.0)))
        assertEquals(TapGesture.Exit(Dir.RIGHT), classify(Leg.R(35.0)))
    }

    @Test
    fun `TG-05 U40 D40 U40 is Rub(UP)`() =
        assertEquals(TapGesture.Rub(Dir.UP), classify(Leg.U(40.0), Leg.D(40.0), Leg.U(40.0)))

    @Test
    fun `TG-06 D40 U40 D40 is Rub(DOWN)`() =
        assertEquals(TapGesture.Rub(Dir.DOWN), classify(Leg.D(40.0), Leg.U(40.0), Leg.D(40.0)))

    @Test
    fun `TG-07 U40 D40 is ExitReturn(UP)`() =
        assertEquals(TapGesture.ExitReturn(Dir.UP), classify(Leg.U(40.0), Leg.D(40.0)))

    @Test
    fun `TG-08 bottom-row key D40 to the window border is an exit down (issue 237)`() =
        assertEquals(TapGesture.Exit(Dir.DOWN), classify(Leg.D(40.0), key = bottomRow))

    @Test
    fun `TG-09 L35 R35 L35 is Rub(LEFT), R35 L35 R35 is Rub(RIGHT)`() {
        assertEquals(TapGesture.Rub(Dir.LEFT), classify(Leg.L(35.0), Leg.R(35.0), Leg.L(35.0)))
        assertEquals(TapGesture.Rub(Dir.RIGHT), classify(Leg.R(35.0), Leg.L(35.0), Leg.R(35.0)))
    }

    @Test
    fun `TG-10 L35 R35 is ExitReturn(LEFT), D40 U40 is ExitReturn(DOWN)`() {
        assertEquals(TapGesture.ExitReturn(Dir.LEFT), classify(Leg.L(35.0), Leg.R(35.0)))
        assertEquals(TapGesture.ExitReturn(Dir.DOWN), classify(Leg.D(40.0), Leg.U(40.0)))
    }
}
