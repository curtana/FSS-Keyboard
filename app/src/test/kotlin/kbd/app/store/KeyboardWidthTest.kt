package kbd.app.store

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `KeyboardWidth` (LLD-11 §7.5 KW-01~KW-10, SRS FR-CMN-14, LLD-08 §7.2). Density 1 unless said: px = dp.
 * A span is (left, width, margin) of the keyboard rectangle: 11 key cells, the keys and half a cell on each side
 * (FSS#84).
 */
class KeyboardWidthTest {

    private val d = WidthSetting.DEFAULT

    private fun s(keyDp: Float?, align: KeyboardAlign = KeyboardAlign.CENTER) = WidthSetting(keyDp, align)

    private fun portrait(avail: Int, p: WidthSetting, density: Float = 1f) = KeyboardWidth.place(avail, avail, density, p, d, isLandscape = false)

    @Test
    fun `KW-01 portrait default fills the available width, margins of half a key`() {
        assertEquals(KeyboardSpan(0, 711, 32), portrait(711, d))   // 711 / 22 = 32.3
        assertEquals(KeyboardSpan(0, 360, 16), portrait(360, d))   // 360 / 22 = 16.4
        assertEquals(328, portrait(360, d).keysWidth)   // ten keys of about 32.7 dp
        assertEquals(16, portrait(360, d).keysLeft)
    }

    @Test
    fun `KW-02 landscape default is the portrait key size, centred, never wider than the available width`() {
        // Tablet 711 x 1138 dp, portrait full: the rectangle is 711 dp in the middle.
        assertEquals(KeyboardSpan(213, 711, 32), KeyboardWidth.place(1138, 711, 1f, d, d, isLandscape = true))
        // Portrait key 40 dp: landscape rectangle 440 dp, keys 400 dp.
        assertEquals(KeyboardSpan(349, 440, 20), KeyboardWidth.place(1138, 711, 1f, s(40f), d, isLandscape = true))
        // Side bar wider than the difference: the available width wins.
        assertEquals(KeyboardSpan(0, 700, 32), KeyboardWidth.place(700, 711, 1f, d, d, isLandscape = true))
    }

    @Test
    fun `KW-03 eleven cells of the chosen size, stopping at the available width`() {
        assertEquals(KeyboardSpan(135, 440, 20), portrait(711, s(40f)))   // rest 271 / 2
        assertEquals(400, portrait(711, s(40f)).keysWidth)   // ten keys of 40 dp
        assertEquals(KeyboardSpan(0, 711, 32), portrait(711, s(80f)))   // 880 > 711: full
        val px = KeyboardWidth.place(2276, 1422, 2f, d, s(45f), isLandscape = true)   // px at density 2
        assertEquals(990, px.width)
        assertEquals(45, px.margin)
        assertEquals(900, px.keysWidth)
    }

    @Test
    fun `KW-04 never narrower than 352 dp, unless the available width is`() {
        assertEquals(KeyboardSpan(179, 352, 16), portrait(711, s(20f)))   // below the slider: still 32 dp keys
        assertEquals(320, portrait(711, s(20f)).keysWidth)
        assertEquals(792, portrait(1600, s(32f), density = 2.25f).width)   // 352 dp = 792 px
        assertEquals(KeyboardSpan(0, 300, 14), portrait(300, s(32f, KeyboardAlign.RIGHT)))   // 300 / 22 = 13.6
        // The portrait full width below 352 dp: the landscape default is the lower limit, not the short side.
        assertEquals(KeyboardSpan(144, 352, 16), KeyboardWidth.place(640, 320, 1f, d, d, isLandscape = true))
    }

    @Test
    fun `KW-05 left, centre and right leave the rest on the other side`() {
        assertEquals(KeyboardSpan(0, 440, 20), portrait(800, s(40f, KeyboardAlign.LEFT)))
        assertEquals(KeyboardSpan(180, 440, 20), portrait(800, s(40f, KeyboardAlign.CENTER)))
        assertEquals(KeyboardSpan(360, 440, 20), portrait(800, s(40f, KeyboardAlign.RIGHT)))
        assertEquals(KeyboardSpan(0, 800, 36), portrait(800, s(null, KeyboardAlign.RIGHT)))   // full: no rest
    }

    @Test
    fun `KW-06 snap goes to whole dp inside 32 to 160`() {
        assertEquals(32f, KeyboardWidth.snap(10f))
        assertEquals(160f, KeyboardWidth.snap(400f))
        assertEquals(36f, KeyboardWidth.snap(36.4f))
        assertEquals(37f, KeyboardWidth.snap(36.6f))
        assertEquals(KeyboardWidth.FULL, KeyboardWidth.snap(KeyboardWidth.FULL))
    }

    @Test
    fun `KW-07 no available width gives an empty span, the full mark fills`() {
        assertEquals(KeyboardSpan(0, 0, 0), portrait(0, s(40f)))
        assertEquals(KeyboardSpan(0, 1138, 52), KeyboardWidth.place(1138, 711, 1f, d, s(KeyboardWidth.FULL), isLandscape = true))
        // A portrait full mark is the full portrait width for the landscape default.
        assertEquals(KeyboardSpan(213, 711, 32), KeyboardWidth.place(1138, 711, 1f, s(KeyboardWidth.FULL), d, isLandscape = true))
    }

    @Test
    fun `KW-08 the slider top is the first whole key size whose eleven cells fill`() {
        assertEquals(65, KeyboardWidth.fullKeyDp(711f))
        assertEquals(33, KeyboardWidth.fullKeyDp(360f))
        assertEquals(104, KeyboardWidth.fullKeyDp(1138f))
        assertEquals(32, KeyboardWidth.fullKeyDp(300f))
        assertEquals(32, KeyboardWidth.fullKeyDp(352f))   // exactly eleven 32 dp cells
        assertEquals(352f, KeyboardWidth.MIN_DP, 0f)
    }

    @Test
    fun `KW-09 the two margins are equal and margin + keys + margin is the rectangle to the px`() {
        for (density in listOf(1f, 1.5f, 2.625f, 3f)) {
            for (avail in 1..2400) {
                for (setting in listOf(d, s(32f), s(41f, KeyboardAlign.RIGHT), s(57f, KeyboardAlign.LEFT))) {
                    val sp = KeyboardWidth.place(avail, avail, density, setting, d, isLandscape = false)
                    val at = "avail $avail density $density $setting: $sp"
                    assertTrue(at, sp.left >= 0 && sp.left + sp.width <= avail)   // inside the available width
                    assertEquals(at, sp.width, sp.margin + sp.keysWidth + sp.margin)
                    assertEquals(at, sp.left + sp.width, sp.keysLeft + sp.keysWidth + sp.margin)   // the right margin = the left
                    assertTrue(at, abs(sp.margin - sp.width / 22f) <= 0.5f)   // half a key cell, to the px
                    assertTrue(at, sp.keysWidth > 0)
                }
            }
        }
        // Full width: the rectangle is the available width whatever the px count.
        for (avail in listOf(359, 360, 361, 1079, 1080, 1081)) assertEquals(avail, portrait(avail, d).width)
    }

    @Test
    fun `KW-10 a key size stored before the side margins is read as it is`() {
        // 360 dp screen, 36 dp stored (ten keys filled it): eleven cells do not fit, so it fills like the full mark.
        assertEquals(portrait(360, d), portrait(360, s(36f)))
        // 411 dp screen, 34 dp: the keys keep 340 dp and the rectangle is one cell wider.
        val sp = portrait(411, s(34f))
        assertEquals(KeyboardSpan(18, 374, 17), sp)
        assertEquals(340, sp.keysWidth)
    }
}
