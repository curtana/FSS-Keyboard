package kbd.app.store

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `FloatingKeyboard`: where a floating keyboard's plate is, dragging it, and its shares (LLD-11 §7.5 FK-01~FK-08,
 * SRS FR-CMN-15 ④~⑨, HLD 5A "띄운 자판", LLD-08 §7.4, FSS#86). Pure, px.
 *
 * The view is 1000 × 2000 unless a case says otherwise; the plate is 600 high (a 24 px handle and 576 px of rows).
 */
class FloatingKeyboardTest {

    private val on = FloatSetting(true, null, null)

    private fun span(avail: Int, keyDp: Float?, align: KeyboardAlign): KeyboardSpan =
        KeyboardWidth.place(avail, avail, 1f, WidthSetting(keyDp, align), WidthSetting.DEFAULT, false)

    private fun place(
        s: FloatSetting,
        span: KeyboardSpan = span(1000, 40f, KeyboardAlign.CENTER),
        w: Int = 1000,
        h: Int = 2000,
        l: Int = 0,
        t: Int = 0,
        r: Int = 0,
        b: Int = 0,
    ): FloatPlace? = FloatingKeyboard.place(w, h, l, t, r, b, span, PLATE_H, s)

    private fun inside(p: FloatPlace) = p.plate.left >= p.area.left && p.plate.top >= p.area.top &&
        p.plate.right <= p.area.right && p.plate.bottom <= p.area.bottom

    @Test
    fun `FK-01 off, or an area lower than the plate, is docked - an area as high as the plate floats with no range down`() {
        assertNull(place(FloatSetting(false, 0.3f, 0.3f)))
        assertNull(place(FloatSetting.DEFAULT))
        // 60 at the top and 40 at the bottom: the area is h − 100 high.
        assertNull(place(on, h = PLATE_H + 99, t = 60, b = 40))
        val exact = place(on, h = PLATE_H + 100, t = 60, b = 40)
        assertNotNull(exact)
        assertEquals(0, exact!!.rangeY)
        assertEquals(60, exact.plate.top)
        assertEquals(PxRect(280, 60, 720, 60 + PLATE_H), exact.plate)
        assertEquals(1, place(on, h = PLATE_H + 101, t = 60, b = 40)!!.rangeY)
        // No keyboard rectangle (no width yet): docked.
        assertNull(place(on, span = KeyboardSpan(0, 0, 0)))
    }

    @Test
    fun `FK-02 nothing stored - the bottom end, and across where the alignment puts the docked keyboard`() {
        for (align in KeyboardAlign.values()) {
            val sp = span(900, 40f, align)   // 440 wide in 900: the available width of a 1000 px view with side bars
            val p = place(on, sp, l = 70, r = 30, t = 50, b = 20)!!
            assertEquals("$align", PxRect(70, 50, 970, 1980), p.area)
            assertEquals("$align", 900 - 440, p.rangeX)
            assertEquals("$align", 1930 - PLATE_H, p.rangeY)
            assertEquals("$align", 70 + sp.left, p.plate.left)   // the docked keyboard rectangle's left, to the px
            assertEquals("$align", 440, p.plate.width)
            assertEquals("$align", 1980, p.plate.bottom)   // the bottom end: the rows end where the docked rows end
            assertEquals("$align", PLATE_H, p.plate.height)
        }
        // The alignment as a share: 0, a half (the docked keyboard's own rounding), 1.
        assertEquals(0, place(on, span(1000, 40f, KeyboardAlign.LEFT))!!.plate.left)
        assertEquals(280, place(on, span(1000, 40f, KeyboardAlign.CENTER))!!.plate.left)
        assertEquals(560, place(on, span(1000, 40f, KeyboardAlign.RIGHT))!!.plate.left)
        assertEquals(span(1001, 40f, KeyboardAlign.CENTER).left, place(on, span(1001, 40f, KeyboardAlign.CENTER), w = 1001)!!.plate.left)
    }

    @Test
    fun `FK-03 a stored share places the plate whatever the alignment - 0, a half, 1, and cut to 0 to 1`() {
        for (align in KeyboardAlign.values()) {
            val sp = span(1000, 40f, align)
            assertEquals("$align", 0, place(FloatSetting(true, 0f, 0f), sp)!!.plate.left)
            assertEquals("$align", 0, place(FloatSetting(true, 0f, 0f), sp)!!.plate.top)
            assertEquals("$align", 280, place(FloatSetting(true, 0.5f, 0.5f), sp)!!.plate.left)
            assertEquals("$align", 700, place(FloatSetting(true, 0.5f, 0.5f), sp)!!.plate.top)
            assertEquals("$align", 560, place(FloatSetting(true, 1f, 1f), sp)!!.plate.left)
            assertEquals("$align", 1400, place(FloatSetting(true, 1f, 1f), sp)!!.plate.top)
            assertEquals("$align", 140, place(FloatSetting(true, 0.25f, null), sp)!!.plate.left)
            assertEquals("$align", 1400, place(FloatSetting(true, 0.25f, null), sp)!!.plate.top)   // down: none stored
        }
        val sp = span(1000, 40f, KeyboardAlign.LEFT)
        assertEquals(PxRect(0, 0, 440, PLATE_H), place(FloatSetting(true, -3f, -0.1f), sp)!!.plate)
        assertEquals(PxRect(560, 1400, 1000, 2000), place(FloatSetting(true, 1.5f, 40f), sp)!!.plate)
        // Not a number reads as none stored: the alignment's place, the bottom end.
        assertEquals(PxRect(0, 1400, 440, 2000), place(FloatSetting(true, Float.NaN, Float.NaN), sp)!!.plate)
        assertEquals(0.3f, FloatingKeyboard.ratio(0.3f))
        assertNull(FloatingKeyboard.ratio(null))
        assertNull(FloatingKeyboard.ratio(Float.NaN))
        assertEquals(1f, FloatingKeyboard.ratio(Float.POSITIVE_INFINITY))
    }

    @Test
    fun `FK-04 a full-width keyboard has no range across - it is placed up and down only`() {
        val full = span(1000, null, KeyboardAlign.RIGHT)
        assertEquals(1000, full.width)
        for (fx in listOf(null, 0f, 0.5f, 1f)) {
            val p = place(FloatSetting(true, fx, 0.25f), full, l = 0, t = 80, b = 20)!!
            assertEquals("$fx", 0, p.rangeX)
            assertEquals("$fx", 0, p.plate.left)
            assertEquals("$fx", 1000, p.plate.right)
            assertEquals("$fx", 80 + Math.round(0.25f * (1900 - PLATE_H)), p.plate.top)
        }
    }

    @Test
    fun `FK-05 the whole plate is in the area whatever the insets, the size and the shares`() {
        val shares = listOf(null, 0f, 0.001f, 0.333f, 0.5f, 0.999f, 1f, 7f, -7f)
        for (w in listOf(360, 411, 1000, 1280, 2560)) for (h in listOf(700, 1333, 2000)) for (l in listOf(0, 48)) for (r in listOf(0, 126)) {
            for (t in listOf(0, 63)) for (b in listOf(0, 24, 48)) for (keyDp in listOf(null, 32f, 40f)) {
                val sp = span(w - l - r, keyDp, KeyboardAlign.CENTER)
                for (fx in shares) for (fy in shares) {
                    val p = FloatingKeyboard.place(w, h, l, t, r, b, sp, PLATE_H, FloatSetting(true, fx, fy))
                    val case = "$w×$h l$l r$r t$t b$b key $keyDp fx $fx fy $fy"
                    if (h - t - b < PLATE_H) {
                        assertNull(case, p)
                        continue
                    }
                    assertTrue(case, inside(p!!))
                    assertEquals(case, sp.width, p.plate.width)
                    assertEquals(case, PLATE_H, p.plate.height)
                    assertEquals(case, PxRect(l, t, w - r, h - b), p.area)
                }
            }
        }
    }

    @Test
    fun `FK-06 dragging - the start plus the finger's way, cut into the area on each axis by itself`() {
        val area = PxRect(40, 60, 960, 1900)
        val start = PxRect(300, 700, 740, 700 + PLATE_H)
        fun at(dx: Int, dy: Int) = FloatingKeyboard.drag(start, dx, dy, area)
        assertEquals(start, at(0, 0))
        assertEquals(PxRect(310, 680, 750, 680 + PLATE_H), at(10, -20))
        // Past each of the four ends: it stops at the end, the other axis goes on.
        assertEquals(PxRect(40, 705, 480, 705 + PLATE_H), at(-5000, 5))
        assertEquals(PxRect(520, 705, 960, 705 + PLATE_H), at(5000, 5))
        assertEquals(PxRect(305, 60, 745, 60 + PLATE_H), at(5, -5000))
        assertEquals(PxRect(305, 1300, 745, 1900), at(5, 5000))
        assertEquals(PxRect(40, 60, 480, 60 + PLATE_H), at(-261, -641))   // one px past both
        assertEquals(PxRect(40, 60, 480, 60 + PLATE_H), at(-260, -640))   // exactly at both
        // Past an end and back: the place is the start plus the way now, not less by what went past.
        assertEquals(PxRect(40, 700, 480, 700 + PLATE_H), at(-900, 0))
        assertEquals(PxRect(200, 700, 640, 700 + PLATE_H), at(-100, 0))
        // No range on an axis: it stays there.
        val wide = PxRect(40, 700, 960, 700 + PLATE_H)
        assertEquals(PxRect(40, 730, 960, 730 + PLATE_H), FloatingKeyboard.drag(wide, 300, 30, area))
        assertEquals(PxRect(40, 730, 960, 730 + PLATE_H), FloatingKeyboard.drag(wide, -300, 30, area))
        val tall = PxRect(300, 60, 740, 1900)
        assertEquals(PxRect(330, 60, 770, 1900), FloatingKeyboard.drag(tall, 30, 300, area))
    }

    @Test
    fun `FK-07 shares - 0 at the left and top ends, 1 at the right and bottom ends, none on an axis with no range`() {
        val area = PxRect(40, 60, 960, 1900)
        assertEquals(FloatRatio(0f, 0f), FloatingKeyboard.ratios(PxRect(40, 60, 480, 60 + PLATE_H), area))
        assertEquals(FloatRatio(1f, 1f), FloatingKeyboard.ratios(PxRect(520, 1300, 960, 1900), area))
        assertEquals(FloatRatio(0.5f, 0.5f), FloatingKeyboard.ratios(PxRect(280, 680, 720, 680 + PLATE_H), area))
        assertEquals(FloatRatio(null, 0.5f), FloatingKeyboard.ratios(PxRect(40, 680, 960, 680 + PLATE_H), area))   // full width
        assertEquals(FloatRatio(0.5f, null), FloatingKeyboard.ratios(PxRect(280, 60, 720, 1900), area))           // as high as the area
        assertEquals(FloatRatio(null, null), FloatingKeyboard.ratios(area, area))
    }

    @Test
    fun `FK-08 place to shares to place gives the same px, for every place of every range`() {
        // Every range a screen can have (to 4000 px) and every place in it, on both axes at once.
        for (range in 1..4000) {
            val area = PxRect(7, 11, 7 + 440 + range, 11 + PLATE_H + range)
            val sp = KeyboardSpan(0, 440, 20)
            for (k in 0..range) {
                val plate = PxRect(7 + k, 11 + (range - k), 7 + k + 440, 11 + (range - k) + PLATE_H)
                val r = FloatingKeyboard.ratios(plate, area)
                val back = FloatingKeyboard.place(area.right, area.bottom, 7, 11, 0, 0, sp, PLATE_H, FloatSetting(true, r.fx, r.fy))!!
                if (back.plate != plate) assertEquals("range $range at $k", plate, back.plate)
            }
        }
    }

    private companion object {
        const val PLATE_H = 600
    }
}
