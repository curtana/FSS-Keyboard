package kbd.app.store

import org.junit.Assert.assertEquals
import org.junit.Test

/** `KeyboardWidth` (LLD-11 §7.5 KW-01~KW-07, SRS FR-CMN-14, LLD-08 §7.2). Density 1 unless said: px = dp. */
class KeyboardWidthTest {

    private fun s(ratio: Float?, align: KeyboardAlign = KeyboardAlign.CENTER) = WidthSetting(ratio, align)

    @Test
    fun `KW-01 portrait default fills the available width`() {
        assertEquals(KeyboardSpan(0, 711), KeyboardWidth.place(711, 711, 1f, WidthSetting.PORTRAIT_DEFAULT))
        assertEquals(KeyboardSpan(0, 360), KeyboardWidth.place(360, 360, 1f, WidthSetting.PORTRAIT_DEFAULT))
    }

    @Test
    fun `KW-02 landscape default is the portrait width, centred, never wider than the available width`() {
        // Tablet 711 x 1138 dp: the keyboard is 711 dp in the middle.
        assertEquals(KeyboardSpan(213, 711), KeyboardWidth.place(1138, 711, 1f, WidthSetting.LANDSCAPE_DEFAULT))
        // A phone (about 45 %, below the selectable range, kept: FR-CMN-14 ③).
        assertEquals(KeyboardSpan(220, 360), KeyboardWidth.place(800, 360, 1f, WidthSetting.LANDSCAPE_DEFAULT))
        // Side bar wider than the difference: the available width wins.
        assertEquals(KeyboardSpan(0, 700), KeyboardWidth.place(700, 711, 1f, WidthSetting.LANDSCAPE_DEFAULT))
    }

    @Test
    fun `KW-03 the ratio share of the available width`() {
        assertEquals(KeyboardSpan(89, 533), KeyboardWidth.place(711, 711, 1f, s(0.75f)))   // 533.25 → 533, rest 178 / 2
        assertEquals(569, KeyboardWidth.place(1138, 711, 1f, s(0.5f)).width)
    }

    @Test
    fun `KW-04 never narrower than 320 dp, unless the available width is`() {
        assertEquals(320, KeyboardWidth.place(400, 400, 1f, s(0.5f)).width)   // 200 → 320
        assertEquals(720, KeyboardWidth.place(1000, 1000, 2.25f, s(0.5f)).width)   // 500 px < 320 dp = 720 px
        assertEquals(KeyboardSpan(0, 300), KeyboardWidth.place(300, 300, 1f, s(0.5f, KeyboardAlign.RIGHT)))
    }

    @Test
    fun `KW-05 left, centre and right leave the rest on the other side`() {
        assertEquals(KeyboardSpan(0, 400), KeyboardWidth.place(800, 800, 1f, s(0.5f, KeyboardAlign.LEFT)))
        assertEquals(KeyboardSpan(200, 400), KeyboardWidth.place(800, 800, 1f, s(0.5f, KeyboardAlign.CENTER)))
        assertEquals(KeyboardSpan(400, 400), KeyboardWidth.place(800, 800, 1f, s(0.5f, KeyboardAlign.RIGHT)))
        assertEquals(KeyboardSpan(0, 800), KeyboardWidth.place(800, 800, 1f, s(1f, KeyboardAlign.RIGHT)))   // full: no rest
    }

    @Test
    fun `KW-06 snap goes to the nearest 5 percent step inside 50 to 100`() {
        assertEquals(0.5f, KeyboardWidth.snap(0.3f))
        assertEquals(1f, KeyboardWidth.snap(1.4f))
        assertEquals(0.65f, KeyboardWidth.snap(0.66f))
        assertEquals(0.7f, KeyboardWidth.snap(0.68f))
        assertEquals(11, KeyboardWidth.RATIOS.size)
        assertEquals(0.55f, KeyboardWidth.RATIOS[1])
    }

    @Test
    fun `KW-08 the landscape default sits among the ratios by its share, first below 50 percent`() {
        val tablet = KeyboardWidth.landscapeChoices(711f / 1138f)   // about 62 %
        assertEquals(12, tablet.size)
        assertEquals(listOf(0.5f, 0.55f, 0.6f, null, 0.65f), tablet.subList(0, 5))
        val phone = KeyboardWidth.landscapeChoices(360f / 800f)   // 45 %
        assertEquals(null, phone[0])
        assertEquals(0.5f, phone[1])
        assertEquals(null, KeyboardWidth.landscapeChoices(1.2f).last())
    }

    @Test
    fun `KW-07 no available width gives an empty span`() {
        assertEquals(KeyboardSpan(0, 0), KeyboardWidth.place(0, 711, 1f, s(0.5f)))
    }
}
