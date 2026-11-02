package kbd.app.store

import org.junit.Assert.assertEquals
import org.junit.Test

/** `KeyboardWidth` (LLD-11 §7.5 KW-01~KW-08, SRS FR-CMN-14, LLD-08 §7.2). Density 1 unless said: px = dp. */
class KeyboardWidthTest {

    private val d = WidthSetting.DEFAULT

    private fun s(keyDp: Float?, align: KeyboardAlign = KeyboardAlign.CENTER) = WidthSetting(keyDp, align)

    private fun portrait(avail: Int, p: WidthSetting, density: Float = 1f) = KeyboardWidth.place(avail, avail, density, p, d, isLandscape = false)

    @Test
    fun `KW-01 portrait default fills the available width`() {
        assertEquals(KeyboardSpan(0, 711), portrait(711, d))
        assertEquals(KeyboardSpan(0, 360), portrait(360, d))
    }

    @Test
    fun `KW-02 landscape default is the portrait key size, centred, never wider than the available width`() {
        // Tablet 711 x 1138 dp, portrait full: the keyboard is 711 dp in the middle.
        assertEquals(KeyboardSpan(213, 711), KeyboardWidth.place(1138, 711, 1f, d, d, isLandscape = true))
        // Portrait key 40 dp: landscape 400 dp.
        assertEquals(KeyboardSpan(369, 400), KeyboardWidth.place(1138, 711, 1f, s(40f), d, isLandscape = true))
        // Side bar wider than the difference: the available width wins.
        assertEquals(KeyboardSpan(0, 700), KeyboardWidth.place(700, 711, 1f, d, d, isLandscape = true))
    }

    @Test
    fun `KW-03 ten keys of the chosen size, stopping at the available width`() {
        assertEquals(KeyboardSpan(155, 400), portrait(711, s(40f)))   // rest 311 / 2
        assertEquals(KeyboardSpan(0, 711), portrait(711, s(80f)))   // 800 > 711: full
        assertEquals(900, KeyboardWidth.place(2276, 1422, 2f, d, s(45f), isLandscape = true).width)   // px at density 2
    }

    @Test
    fun `KW-04 never narrower than 320 dp, unless the available width is`() {
        assertEquals(320, portrait(711, s(20f)).width)   // below the slider: still 320
        assertEquals(720, portrait(1600, s(32f), density = 2.25f).width)   // 320 dp = 720 px
        assertEquals(KeyboardSpan(0, 300), portrait(300, s(32f, KeyboardAlign.RIGHT)))
    }

    @Test
    fun `KW-05 left, centre and right leave the rest on the other side`() {
        assertEquals(KeyboardSpan(0, 400), portrait(800, s(40f, KeyboardAlign.LEFT)))
        assertEquals(KeyboardSpan(200, 400), portrait(800, s(40f, KeyboardAlign.CENTER)))
        assertEquals(KeyboardSpan(400, 400), portrait(800, s(40f, KeyboardAlign.RIGHT)))
        assertEquals(KeyboardSpan(0, 800), portrait(800, s(null, KeyboardAlign.RIGHT)))   // full: no rest
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
        assertEquals(KeyboardSpan(0, 0), portrait(0, s(40f)))
        assertEquals(KeyboardSpan(0, 1138), KeyboardWidth.place(1138, 711, 1f, d, s(KeyboardWidth.FULL), isLandscape = true))
        // A portrait full mark is the full portrait width for the landscape default.
        assertEquals(KeyboardSpan(213, 711), KeyboardWidth.place(1138, 711, 1f, s(KeyboardWidth.FULL), d, isLandscape = true))
    }

    @Test
    fun `KW-08 the slider top is the first whole key size that fills`() {
        assertEquals(72, KeyboardWidth.fullKeyDp(711f))
        assertEquals(36, KeyboardWidth.fullKeyDp(360f))
        assertEquals(114, KeyboardWidth.fullKeyDp(1138f))
        assertEquals(32, KeyboardWidth.fullKeyDp(300f))
    }
}
