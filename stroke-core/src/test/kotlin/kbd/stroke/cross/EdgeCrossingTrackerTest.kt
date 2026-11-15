package kbd.stroke.cross

import kbd.stroke.geom.Edge
import kbd.stroke.geom.StrokePoint
import kbd.stroke.testing.Leg
import kbd.stroke.testing.Trajectories
import kbd.stroke.testing.Trajectory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** LLD-11 §3 EC-01..EC-12. Data: `src/test/resources/trajectories/EC-*.json`. */
class EdgeCrossingTrackerTest {

    private fun run(tr: Trajectory, points: List<StrokePoint> = tr.points): EdgeCrossingTracker =
        EdgeCrossingTracker(tr.key.geom(), tr.thresholds.minCrossDp).apply {
            begin(points.first())
            points.drop(1).forEach { add(it) }
        }

    private fun names(t: EdgeCrossingTracker) = t.crossings.map { it.edge.name to it.dir.name }

    private fun check(id: String) {
        val tr = Trajectories.load(id)
        assertEquals(id, tr.expectCrossings, names(run(tr)))
    }

    @Test fun `EC-01 R10 L10 records nothing`() = check("EC-01")

    @Test fun `EC-02 R45 records RIGHT OUT`() = check("EC-02")

    @Test fun `EC-03 R33_4 (7_9 beyond, minCrossDp 8) records nothing`() = check("EC-03")

    @Test fun `EC-04 R33_5 (8_0 beyond) records RIGHT OUT`() = check("EC-04")

    @Test
    fun `EC-05 R45 then L26 stays out, L28 comes back in`() {
        check("EC-05-a")
        check("EC-05-b")
    }

    @Test fun `EC-06 around the corner back in through TOP`() = check("EC-06")

    @Test fun `EC-07 diagonal V(45,-45) in one sample leaves through TOP`() = check("EC-07")

    @Test fun `EC-08 diagonal tie on a square key goes horizontal`() = check("EC-08")

    @Test fun `EC-09 outer LEFT edge, L45 reaches the window border - LEFT OUT (issue 237)`() = check("EC-09")

    @Test fun `EC-10 outer LEFT edge, L45 then R90 - LEFT OUT, LEFT IN, RIGHT OUT (issue 237)`() = check("EC-10")

    @Test
    fun `EC-11 outer BOTTOM D45 reaches the border - BOTTOM OUT (issue 237), an inner top is crossed as before`() {
        check("EC-11-a")
        check("EC-11-b")
    }

    @Test
    fun `EC-12 a NaN sample is ignored and the result is unchanged`() {
        for (id in listOf("EC-02", "EC-05-b", "EC-06")) {
            val tr = Trajectories.load(id)
            val clean = run(tr)
            for (i in 1..tr.points.size) {
                val withNaN = tr.points.toMutableList().apply {
                    add(i, StrokePoint(if (i % 2 == 0) Float.NaN else 30f, if (i % 2 == 0) 20f else Float.NaN, tr.points[i - 1].t))
                }
                val t = run(tr, withNaN)
                assertEquals("$id NaN at $i", clean.crossings, t.crossings)
                assertEquals("$id NaN at $i", clean.zone, t.zone)
            }
            val t = EdgeCrossingTracker(tr.key.geom(), 14f).apply { begin(tr.points.first()) }
            assertNull(t.add(StrokePoint(Float.NaN, Float.NaN, 1)))
        }
    }

    @Test
    fun `crossing carries the sample time and position, and add returns it`() {
        val tr = Trajectories.load("EC-02")
        val t = EdgeCrossingTracker(tr.key.geom(), 14f)
        t.begin(tr.points.first())
        val returned = tr.points.drop(1).mapNotNull { t.add(it) }
        assertEquals(t.crossings, returned)
        val c = returned.single()
        // First sample at x ≥ 65: start 25.5 + 40 = 65.5, after 40 dp at 2 ms/dp.
        assertEquals(Crossing(Edge.RIGHT, CrossDir.OUT, 80L, 65.5f, 24f), c)
        assertFalse(t.zone.inside)
        assertEquals(ZoneState(XZone.RIGHT_OUT, YZone.IN), t.zone)
    }

    @Test
    fun `begin restarts, copy is independent, derived functions`() {
        val key = Trajectories.load("EC-01").key.geom()
        // Right, back in, right again, back in: four RIGHT records, then out through TOP.
        val pts = Trajectories.expand(
            25.5, 24.0, 0L,
            listOf(Leg.R(45.0), Leg.L(45.0), Leg.R(45.0), Leg.L(45.0), Leg.U(45.0)),
        )
        val t = EdgeCrossingTracker(key, 14f)
        t.begin(pts.first())
        pts.drop(1).take(pts.size / 2).forEach { t.add(it) }
        val probe = t.copy()
        pts.drop(1 + pts.size / 2).forEach { t.add(it) }

        assertTrue(probe.crossings.size < t.crossings.size)
        assertEquals(
            listOf("RIGHT" to "OUT", "RIGHT" to "IN", "RIGHT" to "OUT", "RIGHT" to "IN", "TOP" to "OUT"),
            names(t),
        )
        assertEquals(Crossing(Edge.RIGHT, CrossDir.OUT, t.crossings[0].t, t.crossings[0].x, t.crossings[0].y), t.firstOut())
        assertEquals(4, t.countOf(Edge.RIGHT))
        assertEquals(1, t.countOf(Edge.TOP))
        assertEquals(0, t.countOf(Edge.LEFT))
        assertEquals(t.crossings[2], t.nthCrossingOf(Edge.RIGHT, 3))
        assertNull(t.nthCrossingOf(Edge.RIGHT, 5))
        assertNull(t.nthCrossingOf(Edge.RIGHT, 0))

        // Feeding the probe the rest gives the same result.
        pts.drop(1 + pts.size / 2).forEach { probe.add(it) }
        assertEquals(t.crossings, probe.crossings)

        t.begin(pts.first())
        assertTrue(t.crossings.isEmpty())
        assertEquals(ZoneState.INSIDE, t.zone)
        assertNull(t.firstOut())
    }

    @Test
    fun `diagonal entry picks the axis that became IN last`() {
        val key = Trajectories.load("EC-01").key.geom()
        val t = EdgeCrossingTracker(key, 14f)
        t.begin(StrokePoint(25.5f, 24f, 0))
        // Out through RIGHT, then up beyond TOP: outside on both axes at (70.5, -21).
        t.add(StrokePoint(70.5f, 24f, 10))
        t.add(StrokePoint(70.5f, -21f, 20))
        // Back to the centre in one sample: X reaches 37 at f = 0.746, Y reaches 14 at f = 0.778 → TOP is last.
        val c = t.add(StrokePoint(25.5f, 24f, 30))
        assertEquals(Edge.TOP, c?.edge)
        assertEquals(CrossDir.IN, c?.dir)
    }

    @Test
    fun `EC-13 a top-row key - a point pinned to the window top counts as past the top edge, coming back is judged as before (issue 237)`() {
        val rect = kbd.stroke.geom.RectDp(0f, 0f, 101.6f, 48f)
        val top = kbd.stroke.geom.KeyGeom.build("1", rect, 711f, 288f)
        assertTrue(top.topAtWindow)
        val t = EdgeCrossingTracker(top, 8f)
        t.begin(StrokePoint(68f, 39.6f, 0))
        t.add(StrokePoint(68f, 10f, 10))
        assertTrue(t.crossings.isEmpty())
        val out = t.add(StrokePoint(68f, 0f, 20))                     // pinned at y 0: the platform's border
        assertEquals(Edge.TOP, out?.edge)
        assertEquals(CrossDir.OUT, out?.dir)
        t.add(StrokePoint(68f, 0.5f, 30))                              // still on the border
        t.add(StrokePoint(68f, 7.9f, 40))                              // not yet minCross inside
        assertEquals(1, t.crossings.size)
        assertEquals(CrossDir.IN, t.add(StrokePoint(68f, 8f, 50))?.dir)   // back in at top + minCross, as before
        // A key below the top row, and a top-row stroke that stays below the border: nothing.
        val below = kbd.stroke.geom.KeyGeom.build("8", kbd.stroke.geom.RectDp(0f, 48f, 101.6f, 96f), 711f, 288f)
        assertFalse(below.topAtWindow)
        val u = EdgeCrossingTracker(top, 8f).apply { begin(StrokePoint(68f, 20f, 0)) }
        assertNull(u.add(StrokePoint(68f, 0.6f, 10)))
    }

    @Test
    fun `EC-14 the four window borders - a point pinned to a border at a window edge is that exit, one dp short is not (issue 237)`() {
        val w = 360f
        val h = 288f
        fun key(l: Float, t: Float, r: Float, b: Float) = kbd.stroke.geom.KeyGeom.build("k", kbd.stroke.geom.RectDp(l, t, r, b), w, h)
        fun first(k: kbd.stroke.geom.KeyGeom, vararg p: Pair<Float, Float>): String? {
            val tr = EdgeCrossingTracker(k, 8f)
            tr.begin(StrokePoint((k.rect.left + k.rect.right) / 2, (k.rect.top + k.rect.bottom) / 2, 0))
            p.forEachIndexed { i, (x, y) -> tr.add(StrokePoint(x, y, i + 1L)) }
            return tr.crossings.firstOrNull()?.let { "${it.edge} ${it.dir}" }
        }
        val leftKey = key(0f, 48f, 51f, 96f)
        assertEquals("LEFT OUT", first(leftKey, 10f to 72f, 0f to 72f))
        assertNull(first(leftKey, 10f to 72f, 1f to 72f))
        val rightKey = key(309f, 48f, 360f, 96f)
        assertEquals("RIGHT OUT", first(rightKey, 350f to 72f, 360f to 72f))
        assertNull(first(rightKey, 350f to 72f, 359f to 72f))
        val bottomKey = key(102f, 240f, 153f, 288f)
        assertEquals("BOTTOM OUT", first(bottomKey, 128f to 280f, 128f to 288f))
        assertNull(first(bottomKey, 128f to 280f, 128f to 287f))
    }

    @Test
    fun `EC-15 a border counts only after minCrossDp of movement towards it from the press (SRS 0_60 FR-SWP-45)`() {
        fun first(k: kbd.stroke.geom.KeyGeom, start: Pair<Float, Float>, vararg p: Pair<Float, Float>): String? {
            val tr = EdgeCrossingTracker(k, 8f)
            tr.begin(StrokePoint(start.first, start.second, 0))
            p.forEachIndexed { i, (x, y) -> tr.add(StrokePoint(x, y, i + 1L)) }
            return tr.crossings.firstOrNull()?.let { "${it.edge} ${it.dir}" }
        }
        val left = kbd.stroke.geom.KeyGeom.build("k", kbd.stroke.geom.RectDp(0f, 48f, 51f, 96f), 360f, 288f)
        assertNull(first(left, 4f to 72f, 2f to 72f, 0f to 72f))              // a press near the border: moved 4 dp
        assertEquals("LEFT OUT", first(left, 12f to 72f, 6f to 72f, 0f to 72f))
        assertNull(first(left, 7.9f to 72f, 0f to 72f))                         // 7.9 dp: not yet
        assertEquals("LEFT OUT", first(left, 8f to 72f, 0f to 72f))             // 8 dp = minCrossDp
        // Top-left key: an up stroke that brushes the left border is ↑, not ←.
        val corner = kbd.stroke.geom.KeyGeom.build("1", kbd.stroke.geom.RectDp(0f, 0f, 51f, 48f), 360f, 288f)
        assertEquals("TOP OUT", first(corner, 6f to 40f, 0.3f to 20f, 0f to 0f))
    }

    @Test
    fun `EC-16 with a side margin an end key's outer edge is an ordinary edge - out at minCrossDp past it (FSS#84)`() {
        fun first(k: kbd.stroke.geom.KeyGeom, start: Pair<Float, Float>, vararg p: Pair<Float, Float>): String? {
            val tr = EdgeCrossingTracker(k, 8f)
            tr.begin(StrokePoint(start.first, start.second, 0))
            p.forEachIndexed { i, (x, y) -> tr.add(StrokePoint(x, y, i + 1L)) }
            return tr.crossings.firstOrNull()?.let { "${it.edge} ${it.dir}" }
        }
        // Keys 328 dp wide, a 16 dp margin on each side: the border is at -16 and 344.
        fun key(l: Float, r: Float) = kbd.stroke.geom.KeyGeom.build("k", kbd.stroke.geom.RectDp(l, 48f, r, 96f), 328f, 288f, 16f)
        val left = key(0f, 32.8f)
        assertNull(first(left, 16f to 72f, 0f to 72f))                          // on its edge: no longer an exit
        assertNull(first(left, 16f to 72f, -7.9f to 72f))
        assertEquals("LEFT OUT", first(left, 16f to 72f, -8f to 72f))
        assertEquals("LEFT OUT", first(left, 2f to 72f, -8f to 72f))            // pressed near the edge: the same point
        val right = key(295.2f, 328f)
        assertNull(first(right, 310f to 72f, 328f to 72f))
        assertNull(first(right, 310f to 72f, 335.9f to 72f))
        assertEquals("RIGHT OUT", first(right, 310f to 72f, 336f to 72f))
        // An inner key of the same row: the same distances.
        val inner = key(32.8f, 65.6f)
        assertNull(first(inner, 49f to 72f, 24.9f to 72f))
        assertEquals("LEFT OUT", first(inner, 49f to 72f, 24.8f to 72f))
    }
}
