package kbd.graffiti.nn

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** LLD-11 §6.7 SF-01..SF-08: the feature calculation of LLD-05 §3.4 on strokes small enough to work out by hand. */
class StrokeFeaturesTest {

    private fun f(vararg p: Pair<Double, Double>) = StrokeFeatures.compute(DoubleArray(p.size) { p[it].first }, DoubleArray(p.size) { p[it].second })

    private fun at(f: FloatArray, channel: Int, row: Int, col: Int) = f[(channel * 8 + row) * 8 + col]

    private fun channelSum(f: FloatArray, channel: Int) = (0 until 64).sumOf { f[channel * 64 + it].toDouble() }

    /** An L: 100 dp to the right, then 100 dp down, a point every 5 dp. */
    private val ell = (0..20).map { 100.0 + 5 * it to 50.0 } + (1..20).map { 200.0 to 50.0 + 5 * it }

    @Test
    fun `SF-01 576 values in 0 to 1, laid out channel, row, column`() {
        val v = f(*ell.toTypedArray())
        assertEquals(576, v.size)
        assertEquals(StrokeFeatures.SIZE, v.size)
        assertTrue(v.all { it in 0f..1f })
        // The top bar runs right (channel 0) along the row holding grid y = 0.5, the right bar down (channel 2) along the column holding x = 7.5.
        for (c in 1..6) assertTrue("right $c", at(v, 0, 0, c) > 0.5f)
        for (r in 1..6) assertTrue("down $r", at(v, 2, r, 7) > 0.5f)
        // The resampled step that cuts the corner points between right and down: a little of it lands in channel 1.
        assertTrue(channelSum(v, 1) < 0.2)
        for (ch in listOf(3, 4, 5, 6, 7, 8)) assertEquals("channel $ch", 0.0, channelSum(v, ch), 1e-6)
    }

    @Test
    fun `SF-02 the long side of the bounding box covers seven of the eight cells, an even margin on both sides`() {
        // A line to the right: grid x 0.5 to 7.5 in the middle row, all of it in channel 0.
        val v = f(*(0..40).map { 10.0 + it * 2.5 to 300.0 }.toTypedArray())
        val total = channelSum(v, 0)
        assertTrue("total $total", total > 7.0 - CELL_SLACK * 8 && total <= 7.0 + 1e-4)
        assertEquals(0.0, (0 until 576).sumOf { v[it].toDouble() } - total, 1e-6)
        // A step goes to the cell of its middle, so a cell holds its share give or take one resampled step (7 / 95).
        assertEquals(0.5, at(v, 0, 4, 0).toDouble(), CELL_SLACK)
        assertEquals(0.5, at(v, 0, 4, 7).toDouble(), CELL_SLACK)
        for (c in 1..6) assertEquals(1.0, at(v, 0, 4, c).toDouble(), CELL_SLACK)
    }

    @Test
    fun `SF-03 position and size are gone`() {
        val a = f(*ell.toTypedArray())
        val moved = f(*ell.map { (x, y) -> x + 123.0 to y - 31.0 }.toTypedArray())
        val bigger = f(*ell.map { (x, y) -> x * 2 to y * 2 }.toTypedArray())
        for (i in a.indices) {
            assertEquals("moved $i", a[i], moved[i], 1e-5f)
            assertEquals("bigger $i", a[i], bigger[i], 1e-5f)
        }
    }

    @Test
    fun `SF-04 the aspect ratio is kept - a flat stroke stays in the middle rows`() {
        val v = f(*(0..40).map { 5.0 * it to 100.0 + (if (it % 2 == 0) 0.0 else 10.0) }.toTypedArray())   // 200 wide, 10 high
        for (ch in 0 until 9) for (r in listOf(0, 1, 2, 5, 6, 7)) for (c in 0 until 8) assertEquals("ch $ch row $r", 0f, at(v, ch, r, c))
    }

    @Test
    fun `SF-05 an angle between two directions is shared between them by how near it is`() {
        // 22.5 degrees: half in channel 0 (0 degrees) and half in channel 1 (45 degrees).
        val t = Math.tan(Math.toRadians(22.5))
        val v = f(*(0..40).map { 5.0 * it to 5.0 * it * t }.toTypedArray())
        assertEquals(channelSum(v, 0), channelSum(v, 1), 1e-4)
        assertTrue(channelSum(v, 0) > 1.0)
        // Straight up on screen is 270 degrees: channel 6 alone. Leftwards is channel 4.
        val up = f(*(0..40).map { 0.0 to -5.0 * it }.toTypedArray())
        assertEquals(7.0, channelSum(up, 5) + channelSum(up, 6), CELL_SLACK * 8)
        assertTrue(channelSum(up, 6) > 6.0)
        val left = f(*(0..40).map { -5.0 * it to 0.0 }.toTypedArray())
        assertEquals(7.0, channelSum(left, 4), CELL_SLACK * 8)
    }

    @Test
    fun `SF-06 a cell's value stops at 1`() {
        // Back and forth over the same line four times: every cell on it would hold 4 without the cut.
        val once = (0..40).map { 5.0 * it to 0.0 }
        val v = f(*(once + once.reversed() + once + once.reversed()).toTypedArray())
        assertEquals(1f, (0 until 576).maxOf { v[it] })
        for (c in 1..6) {
            assertEquals(1f, at(v, 0, 4, c))
            assertEquals(1f, at(v, 4, 4, c))
        }
    }

    @Test
    fun `SF-07 centre channel - raw steps shorter than 1 dp, four of them fill a cell`() {
        fun centre(vararg p: Pair<Double, Double>) = f(*p).copyOfRange(512, 576)
        val start = arrayOf(0.0 to 0.0)
        val rest = arrayOf(100.0 to 0.0, 100.0 to 100.0)
        // One short step at the start: its cell (row of grid y 0.5, column of grid x 0.5) gets 0.25.
        val one = centre(*start, 0.5 to 0.0, *rest)
        assertEquals(0.25f, one[0])
        assertEquals(0.25, one.sumOf { it.toDouble() }, 0.0)
        // A step of exactly 1 dp does not count, one of 0.99 dp does.
        assertEquals(0.0, centre(*start, 1.0 to 0.0, *rest).sumOf { it.toDouble() }, 0.0)
        assertEquals(0.25, centre(*start, 0.99 to 0.0, *rest).sumOf { it.toDouble() }, 0.0)
        // Four short steps = 1, a fifth adds nothing.
        val four = centre(*start, 0.2 to 0.0, 0.4 to 0.0, 0.6 to 0.0, 0.8 to 0.0, *rest)
        assertEquals(1f, four[0])
        val six = centre(*start, 0.1 to 0.0, 0.2 to 0.0, 0.3 to 0.0, 0.4 to 0.0, 0.5 to 0.0, 0.6 to 0.0, *rest)
        assertEquals(1f, six[0])
        assertEquals(1.0, six.sumOf { it.toDouble() }, 0.0)
        // Evenly sampled strokes (no short step) leave the channel empty.
        assertEquals(0.0, centre(*ell.toTypedArray()).sumOf { it.toDouble() }, 0.0)
    }

    @Test
    fun `SF-08 the same stroke gives the same values, and a single point gives none`() {
        assertArrayEquals(f(*ell.toTypedArray()), f(*ell.toTypedArray()), 0f)
        val dot = f(3.0 to 4.0)
        assertEquals(576, dot.size)
        assertTrue(dot.all { it == 0f })
        val twice = f(3.0 to 4.0, 3.0 to 4.0)   // a zero step is a short step: the centre cell of the grid
        assertEquals(0.25, twice.sumOf { it.toDouble() }, 0.0)
        assertEquals(0.25f, at(twice, 8, 4, 4))
    }

    private companion object {
        /** One step of the resampled path in grid units (7 / 95), rounded up: how far a cell's value may be off its exact share. */
        const val CELL_SLACK = 0.08
    }
}
