package kbd.app.store

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `KeyboardPlate.rect` (LLD-11 §7.5 KP-01~KP-07, SRS FR-CMN-14 ⑫, HLD 5A, LLD-08 §7.3, FSS#85). Density 1: px = dp.
 * The view is [H] high (rows and bottom padding); the span comes from `KeyboardWidth.place`, as the views get it.
 */
class KeyboardPlateTest {

    private val d = WidthSetting.DEFAULT

    private fun span(avail: Int, keyDp: Float?, align: KeyboardAlign) =
        KeyboardWidth.place(avail, avail, 1f, WidthSetting(keyDp, align), d, isLandscape = false)

    private fun plate(viewW: Int, padL: Int, padR: Int, keyDp: Float?, align: KeyboardAlign) =
        KeyboardPlate.rect(viewW, H, padL, padR, span(viewW - padL - padR, keyDp, align))

    @Test
    fun `KP-01 a full-width keyboard's plate is the whole view`() {
        assertEquals(PxRect(0, 0, 800, H), plate(800, 0, 0, null, KeyboardAlign.CENTER))
        assertEquals(PxRect(0, 0, 800, H), plate(800, 0, 0, KeyboardWidth.FULL, KeyboardAlign.LEFT))
        // With side bars too: the rectangle reaches both ends of the available width, so both paddings are in.
        assertEquals(PxRect(0, 0, 800, H), plate(800, 48, 0, null, KeyboardAlign.CENTER))
        assertEquals(PxRect(0, 0, 800, H), plate(800, 0, 48, null, KeyboardAlign.RIGHT))
        assertEquals(PxRect(0, 0, 800, H), plate(800, 30, 20, KeyboardWidth.FULL, KeyboardAlign.CENTER))
    }

    @Test
    fun `KP-02 a narrowed keyboard's plate is the keyboard rectangle - left, centre, right`() {
        // Key 40: the rectangle is 440 wide, in 800.
        assertEquals(PxRect(0, 0, 440, H), plate(800, 0, 0, 40f, KeyboardAlign.LEFT))
        assertEquals(PxRect(180, 0, 620, H), plate(800, 0, 0, 40f, KeyboardAlign.CENTER))
        assertEquals(PxRect(360, 0, 800, H), plate(800, 0, 0, 40f, KeyboardAlign.RIGHT))
    }

    @Test
    fun `KP-03 top is 0 and bottom is the view's height, the bottom padding included`() {
        for (h in listOf(288, 288 + 24, 288 + 48)) {
            val p = KeyboardPlate.rect(800, h, 0, 0, span(800, 40f, KeyboardAlign.CENTER))
            assertEquals(0, p.top)
            assertEquals(h, p.bottom)
        }
    }

    @Test
    fun `KP-04 a side padding is in the plate only on the side the rectangle reaches`() {
        // View 848 with a 48 px bar: available width 800, rectangle 440.
        // Bar on the left.
        assertEquals(PxRect(0, 0, 48 + 440, H), plate(848, 48, 0, 40f, KeyboardAlign.LEFT))   // reaches the bar: one plate with it
        assertEquals(PxRect(48 + 180, 0, 48 + 620, H), plate(848, 48, 0, 40f, KeyboardAlign.CENTER))   // the bar stays clear
        assertEquals(PxRect(48 + 360, 0, 848, H), plate(848, 48, 0, 40f, KeyboardAlign.RIGHT))
        // Bar on the right.
        assertEquals(PxRect(0, 0, 440, H), plate(848, 0, 48, 40f, KeyboardAlign.LEFT))
        assertEquals(PxRect(180, 0, 620, H), plate(848, 0, 48, 40f, KeyboardAlign.CENTER))
        assertEquals(PxRect(360, 0, 848, H), plate(848, 0, 48, 40f, KeyboardAlign.RIGHT))   // reaches the bar
    }

    @Test
    fun `KP-05 an available width under the lower limit - the rectangle is that width and the plate the whole view`() {
        // 300 < 352: R = A whatever the key size and the alignment.
        for (align in KeyboardAlign.values()) {
            assertEquals(PxRect(0, 0, 300, H), plate(300, 0, 0, 32f, align))
            assertEquals(PxRect(0, 0, 348, H), plate(348, 48, 0, 32f, align))
        }
    }

    @Test
    fun `KP-06 centred with one px left over - only the side with no px of the rest is reached`() {
        // Available 441, rectangle 440: the one px goes to the right (441 - 440 = 1, 1 / 2 = 0 on the left).
        assertEquals(KeyboardSpan(0, 440, 20), span(441, 40f, KeyboardAlign.CENTER))
        assertEquals(PxRect(0, 0, 440, H), plate(441, 0, 0, 40f, KeyboardAlign.CENTER))
        // With bars on both sides: the left one is in the plate, the right one is not.
        assertEquals(PxRect(0, 0, 30 + 440, H), plate(30 + 441 + 20, 30, 20, 40f, KeyboardAlign.CENTER))
        // Two px left over: one on each side, neither side is reached.
        assertEquals(PxRect(30 + 1, 0, 30 + 441, H), plate(30 + 442 + 20, 30, 20, 40f, KeyboardAlign.CENTER))
    }

    @Test
    fun `KP-07 no size yet - an empty plate, and what a px rectangle holds`() {
        assertTrue(KeyboardPlate.rect(0, 0, 0, 0, KeyboardSpan(0, 0, 0)).isEmpty)
        assertTrue(KeyboardPlate.rect(800, 0, 0, 0, span(800, 40f, KeyboardAlign.CENTER)).isEmpty)
        val p = plate(800, 0, 0, 40f, KeyboardAlign.CENTER)
        assertFalse(p.isEmpty)
        assertTrue(p.contains(180, 0) && p.contains(619, H - 1))
        assertFalse(p.contains(179, 10) || p.contains(620, 10) || p.contains(300, H))
    }

    private companion object {
        const val H = 312
    }
}
