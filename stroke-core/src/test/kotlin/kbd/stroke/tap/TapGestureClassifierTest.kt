package kbd.stroke.tap

import kbd.stroke.geom.Diag
import kbd.stroke.geom.Dir
import kbd.stroke.geom.Edge
import kbd.stroke.geom.KeyGeom
import kbd.stroke.geom.RectDp
import kbd.stroke.geom.StrokePoint
import kbd.stroke.geom.Way
import kbd.stroke.testing.Leg
import kbd.stroke.testing.Trajectories
import kbd.stroke.threshold.ThresholdDefaults
import org.junit.Assert.assertEquals
import org.junit.Test

/** LLD-11 §5.1 TG-01..TG-14: key 36×48, default thresholds, strokes start at the key centre (TG-11..TG-14: FSS#109). */
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
    // --- 8-way keys (FSS#109, SRS FR-TAP-02e): the angle at the first exit decides the direction ---

    /** A stroke through [corners] (dp offsets from the key centre), sampled every 1 dp; the last point is the release. */
    private fun stroke(vararg corners: Pair<Float, Float>, eightWay: Boolean = true, from: Pair<Float, Float> = 18f to 24f): TapGesture {
        val pts = ArrayList<StrokePoint>()
        var (x, y) = from
        var t = 0L
        pts += StrokePoint(x, y, t)
        for ((dx, dy) in corners) {
            val n = maxOf(1, kotlin.math.ceil(kotlin.math.hypot(dx, dy)).toInt())
            repeat(n) { pts += StrokePoint(x + dx * (it + 1) / n, y + dy * (it + 1) / n, ++t) }
            x += dx
            y += dy
        }
        return TapGestureClassifier(inner, ThresholdDefaults.VALUES.minCrossDp, eightWay).run {
            begin(pts.first())
            pts.drop(1).dropLast(1).forEach { add(it) }
            finish(pts.last())
        }
    }

    /** A point [len] dp from the origin at [deg] (0° right, 90° up). */
    private fun polar(deg: Double, len: Float = 40f): Pair<Float, Float> {
        val r = Math.toRadians(deg)
        return (len * kotlin.math.cos(r)).toFloat() to (-len * kotlin.math.sin(r)).toFloat()
    }

    @Test
    fun `TG-11 angle sectors - up down left right 60 degrees, diagonals 30, boundaries on the up-down-left-right side`() {
        val w = TapGestureClassifier.Companion
        // One angle inside each sector.
        val inside = listOf(
            0.0 to Dir.RIGHT, 45.0 to Diag.UP_RIGHT, 90.0 to Dir.UP, 135.0 to Diag.UP_LEFT,
            180.0 to Dir.LEFT, 225.0 to Diag.DOWN_LEFT, 270.0 to Dir.DOWN, 315.0 to Diag.DOWN_RIGHT, 359.0 to Dir.RIGHT,
        )
        for ((deg, way) in inside) assertEquals("$deg", way, w.wayOfAngle(deg))
        // Exactly on a boundary: the up/down/left/right side (SRS FR-TAP-02e ③).
        val bounds = listOf(
            30.0 to Dir.RIGHT, 60.0 to Dir.UP, 120.0 to Dir.UP, 150.0 to Dir.LEFT,
            210.0 to Dir.LEFT, 240.0 to Dir.DOWN, 300.0 to Dir.DOWN, 330.0 to Dir.RIGHT,
        )
        for ((deg, way) in bounds) assertEquals("$deg", way, w.wayOfAngle(deg))
        // Just inside a boundary: the diagonal.
        val e = 1e-6
        val near = listOf(
            30.0 + e to Diag.UP_RIGHT, 60.0 - e to Diag.UP_RIGHT, 120.0 + e to Diag.UP_LEFT, 150.0 - e to Diag.UP_LEFT,
            210.0 + e to Diag.DOWN_LEFT, 240.0 - e to Diag.DOWN_LEFT, 300.0 + e to Diag.DOWN_RIGHT, 330.0 - e to Diag.DOWN_RIGHT,
        )
        for ((deg, way) in near) assertEquals("$deg", way, w.wayOfAngle(deg))
        // Vectors in dp, y down: the angle is measured with up = 90°.
        assertEquals(Dir.UP, w.wayOfVector(0f, -10f))
        assertEquals(Diag.UP_LEFT, w.wayOfVector(-10f, -10f))
        assertEquals(Diag.DOWN_RIGHT, w.wayOfVector(10f, 10f))
        assertEquals(Dir.LEFT, w.wayOfVector(-10f, 0f))
    }

    @Test
    fun `TG-12 an 8-way key - a stroke in each sector exits in that direction, decided at the first exit`() {
        val expect = listOf(
            0.0 to Dir.RIGHT, 45.0 to Diag.UP_RIGHT, 90.0 to Dir.UP, 135.0 to Diag.UP_LEFT,
            180.0 to Dir.LEFT, 225.0 to Diag.DOWN_LEFT, 270.0 to Dir.DOWN, 315.0 to Diag.DOWN_RIGHT,
            20.0 to Dir.RIGHT, 70.0 to Dir.UP, 110.0 to Dir.UP, 160.0 to Dir.LEFT, 250.0 to Dir.DOWN, 340.0 to Dir.RIGHT,
        )
        for ((deg, way) in expect) assertEquals("$deg", TapGesture.Exit(way), stroke(polar(deg)))
        // Up from a press near the left edge crosses the left edge first, yet the angle says ↑ (115°).
        assertEquals(TapGesture.Exit(Dir.UP), stroke(polar(115.0, 60f), from = 4f to 24f))
        // What comes after the first exit does not change the direction: ↖, then on to the right over the top.
        assertEquals(TapGesture.Exit(Diag.UP_LEFT), stroke(polar(135.0, 40f), 50f to -30f))
        // The same diagonal stroke on a 4-way key is the edge crossed first (FR-SWP-04, unchanged).
        assertEquals(TapGesture.Exit(Dir.LEFT), stroke(polar(135.0), eightWay = false))
        assertEquals(TapGesture.Exit(Dir.RIGHT), stroke(polar(45.0), eightWay = false))
        // No exit is a tap on an 8-way key too.
        assertEquals(TapGesture.Tap, stroke(3f to -3f))
    }

    @Test
    fun `TG-13 an 8-way key - out and back along a diagonal is the diagonal's exit-and-return`() {
        for ((deg, way) in listOf(135.0 to Diag.UP_LEFT, 45.0 to Diag.UP_RIGHT, 225.0 to Diag.DOWN_LEFT, 315.0 to Diag.DOWN_RIGHT)) {
            val (dx, dy) = polar(deg)
            assertEquals("$deg", TapGesture.ExitReturn(way), stroke(dx to dy, -dx to -dy))
        }
    }

    @Test
    fun `TG-14 an 8-way key - a rub keeps the first exit's direction, diagonal or not`() {
        val (dx, dy) = polar(135.0)
        assertEquals(TapGesture.Rub(Diag.UP_LEFT), stroke(dx to dy, -dx to -dy, dx to dy))
        assertEquals(TapGesture.Rub(Dir.LEFT), stroke(-35f to 0f, 35f to 0f, -35f to 0f))
        assertEquals(TapGesture.Rub(Dir.RIGHT), stroke(35f to 0f, -35f to 0f, 35f to 0f))
        assertEquals(TapGesture.Rub(Dir.UP), stroke(0f to -40f, 0f to 40f, 0f to -40f))
        assertEquals(TapGesture.Rub(Dir.DOWN), stroke(0f to 40f, 0f to -40f, 0f to 40f))
    }
}
