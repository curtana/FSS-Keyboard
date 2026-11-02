package kbd.app.graffiti

import kbd.graffiti.ink.InkPoint
import kbd.graffiti.ink.PadArea
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** T35: single-pointer ink collection in dp (LLD-05 §8.2, FR-CMN-11, GRAFFITI 6.3). No LLD-11 ID. */
class InkCollectorTest {

    private val g = GraffitiPadGeometry(360f, 240f, 2f / 3f)
    private val ink = InkCollector(density = 2f)

    private fun down(id: Int, x: Float, y: Float, t: Long = 0) = ink.down(id, x, y, t, g::areaAt, g::rectOf)

    @Test
    fun `a stroke keeps its points in order and ends on up`() {
        assertTrue(down(0, 10f, 10f, 0))
        ink.move(0, listOf(InkPoint(20f, 12f, 10), InkPoint(30f, 14f, 20)))
        val s = ink.up(0, 40f, 16f, 30)!!
        assertEquals(listOf(InkPoint(10f, 10f, 0), InkPoint(20f, 12f, 10), InkPoint(30f, 14f, 20), InkPoint(40f, 16f, 30)), s.points)
        assertFalse(ink.active)
        assertTrue(ink.points.isEmpty())
    }

    @Test
    fun `area is decided by the start point`() {
        down(0, 230f, 50f)
        ink.move(0, listOf(InkPoint(300f, 60f, 10)))
        val s = ink.up(0, 330f, 70f, 20)!!
        assertEquals(PadArea.LETTER, s.area)
        assertEquals(g.letterRect, s.areaRect)

        down(1, 250f, 50f)
        val n = ink.up(1, 100f, 50f, 20)!!
        assertEquals(PadArea.NUMBER, n.area)
        assertEquals(g.numberRect, n.areaRect)
    }

    @Test
    fun `other pointers are ignored while a stroke is in progress`() {
        down(0, 10f, 10f)
        assertFalse(down(1, 300f, 10f))
        ink.move(1, listOf(InkPoint(310f, 20f, 5)))
        assertNull(ink.up(1, 320f, 30f, 10))
        val s = ink.up(0, 20f, 10f, 20)!!
        assertEquals(PadArea.LETTER, s.area)
        assertEquals(listOf(10f, 20f), s.points.map { it.x })
    }

    @Test
    fun `a tap is one point and up at the same place adds none`() {
        down(0, 10f, 10f, 5)
        val s = ink.up(0, 10f, 10f, 5)!!
        assertEquals(listOf(InkPoint(10f, 10f, 5)), s.points)
    }

    @Test
    fun `cancel drops the stroke so up gives nothing`() {
        down(0, 10f, 10f)
        ink.move(0, listOf(InkPoint(20f, 20f, 10)))
        ink.cancel()
        assertFalse(ink.active)
        assertNull(ink.up(0, 30f, 30f, 20))
        assertTrue(down(2, 10f, 10f))
    }

    @Test
    fun `up without a stroke gives nothing`() {
        assertNull(ink.up(0, 1f, 1f, 0))
    }
}
