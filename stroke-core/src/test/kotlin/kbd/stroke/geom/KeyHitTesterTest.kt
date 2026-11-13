package kbd.stroke.geom

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** LLD-11 §3 KH-01, KH-02, KH-04 (KH-03, KH-05, KH-06 need layout data and belong to later tasks). */
class KeyHitTesterTest {

    private val windowW = 153f
    private val windowH = 96f
    private val a = KeyGeom.build("a", RectDp(0f, 0f, 51f, 48f), windowW, windowH)
    private val b = KeyGeom.build("b", RectDp(51f, 0f, 102f, 48f), windowW, windowH)
    private val c = KeyGeom.build("c", RectDp(110f, 0f, 153f, 48f), windowW, windowH)
    private val d = KeyGeom.build("d", RectDp(0f, 56f, 153f, 96f), windowW, windowH)
    private val tester = KeyHitTester(listOf(a, b, c, d), windowW, windowH)

    @Test
    fun `KH-01 point inside a key hits that key`() {
        assertSame(a, tester.hit(25.5f, 24f))
        assertSame(b, tester.hit(70f, 10f))
        assertSame(d, tester.hit(140f, 90f))
        // Left and top edges are inclusive.
        assertSame(c, tester.hit(110f, 0f))
    }

    @Test
    fun `KH-02 point on the shared edge goes to the right key`() {
        assertSame(b, tester.hit(51f, 24f))
        // Bottom edge is exclusive: on a's bottom edge the point is in the gap, nearest is a (distance 0).
        assertSame(a, tester.hit(20f, 48f))
    }

    @Test
    fun `KH-04 empty key list gives null`() {
        assertNull(KeyHitTester(emptyList(), windowW, windowH).hit(10f, 10f))
    }

    @Test
    fun `gap point goes to the nearest key, ties to the earlier key`() {
        assertSame(c, tester.hit(108f, 24f))   // 6 from b, 2 from c
        assertSame(b, tester.hit(104f, 24f))   // 2 from b, 6 from c
        assertSame(b, tester.hit(106f, 24f))   // 4 from both: b is earlier
        assertSame(a, tester.hit(25f, 50f))    // 2 from a, 6 from d
        assertSame(d, tester.hit(25f, 54f))    // 6 from a, 2 from d
    }

    @Test
    fun `build marks left, right and bottom window borders as outer, never top`() {
        assertEquals(setOf(Edge.LEFT), a.outerEdges)
        assertEquals(emptySet<Edge>(), b.outerEdges)
        assertEquals(setOf(Edge.RIGHT), c.outerEdges)
        assertEquals(setOf(Edge.LEFT, Edge.RIGHT, Edge.BOTTOM), d.outerEdges)
        // 0.5 dp tolerance.
        val near = KeyGeom.build("n", RectDp(0.5f, 0f, 152.5f, 95.5f), windowW, windowH)
        assertEquals(setOf(Edge.LEFT, Edge.RIGHT, Edge.BOTTOM), near.outerEdges)
        val far = KeyGeom.build("f", RectDp(0.6f, 0f, 152.4f, 95.4f), windowW, windowH)
        assertEquals(emptySet<Edge>(), far.outerEdges)
    }

    @Test
    fun `dir extensions`() {
        assertEquals(Axis.HORIZONTAL, Dir.LEFT.axis)
        assertEquals(Axis.VERTICAL, Dir.UP.axis)
        assertEquals(Axis.VERTICAL, Edge.BOTTOM.axis)
        assertEquals(Dir.DOWN, Dir.UP.opposite)
        assertEquals(Dir.LEFT, Dir.RIGHT.opposite)
        assertEquals(0f to -1f, Dir.UP.unit)
        assertEquals(1f to 0f, Dir.RIGHT.unit)
        assertEquals(Dir.UP, Edge.TOP.dir)
    }

    /** FSS#84: the border's left and right sides lie a side margin outside the key area. */
    @Test
    fun `KG-02 with a side margin the end keys' left and right edges are not outer, bottom and top stay`() {
        fun build(r: RectDp, side: Float) = KeyGeom.build("k", r, windowW, windowH, side)
        assertEquals(emptySet<Edge>(), build(a.rect, 8f).outerEdges)
        assertTrue(build(a.rect, 8f).topAtWindow)
        assertEquals(emptySet<Edge>(), build(c.rect, 8f).outerEdges)
        assertEquals(setOf(Edge.BOTTOM), build(d.rect, 8f).outerEdges)
        // A rect that reaches the border itself is still outer there (within the tolerance).
        assertEquals(setOf(Edge.LEFT, Edge.RIGHT), build(RectDp(-7.5f, 0f, 160.5f, 48f), 8f).outerEdges)
        assertEquals(emptySet<Edge>(), build(RectDp(-7.4f, 0f, 160.4f, 48f), 8f).outerEdges)
        // No margin: as before.
        assertEquals(a, build(a.rect, 0f).copy(id = "a"))
    }
}
