package kbd.app.graffiti

import kbd.graffiti.ink.PadArea
import kbd.graffiti.ink.RectF2
import kbd.stroke.layout.FunctionKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** T35: graffiti pad layout (LLD-05 §8.1, FR-GRF-01). No LLD-11 ID; the view itself is checked on device (DV-18, DV-22). */
class GraffitiPadGeometryTest {

    private val eps = 1e-3f

    private fun assertRect(expected: RectF2, actual: RectF2) {
        assertEquals(expected.left, actual.left, eps)
        assertEquals(expected.top, actual.top, eps)
        assertEquals(expected.right, actual.right, eps)
        assertEquals(expected.bottom, actual.bottom, eps)
    }

    @Test
    fun `pad is the top 4 of 5 rows and the bar the bottom row`() {
        val g = GraffitiPadGeometry(360f, 240f, 2f / 3f)
        assertEquals(48f, g.rowHeightDp, eps)
        assertEquals(192f, g.padBottomDp, eps)
        g.keys.forEach {
            assertEquals(192f, it.rect.top, eps)
            assertEquals(240f, it.rect.bottom, eps)
        }
    }

    @Test
    fun `PI-08 the corner-tap zone is the top-left 15 percent of the letter area for every ratio`() {
        for ((ratio, split) in listOf(0.5f to 180f, 2f / 3f to 240f, 0.75f to 270f)) {
            val g = GraffitiPadGeometry(360f, 240f, ratio)
            assertRect(RectF2(0f, 0f, split * 0.15f, 192f * 0.15f), g.cornerRect)
        }
    }

    @Test
    fun `letter and number areas follow the ratio setting`() {
        for ((ratio, split) in listOf(0.5f to 180f, 2f / 3f to 240f, 0.75f to 270f)) {
            val g = GraffitiPadGeometry(360f, 240f, ratio)
            assertRect(RectF2(0f, 0f, split, 192f), g.letterRect)
            assertRect(RectF2(split, 0f, 360f, 192f), g.numberRect)
        }
    }

    @Test
    fun `bar is mode 72 dp at 360 dp then space, backspace, enter sharing the rest`() {
        val g = GraffitiPadGeometry(360f, 240f, 2f / 3f)
        assertEquals(listOf(FunctionKey.MODE, FunctionKey.SPACE, FunctionKey.BACKSPACE, FunctionKey.ENTER), g.keys.map { it.key })
        val edges = listOf(0f, 72f, 168f, 264f, 360f)
        g.keys.forEachIndexed { i, k ->
            assertEquals(edges[i], k.rect.left, eps)
            assertEquals(edges[i + 1], k.rect.right, eps)
        }
    }

    @Test
    fun `mode key scales as 72 over 360 of the width like the tap keyboards`() {
        val g = GraffitiPadGeometry(720f, 288f, 2f / 3f)
        assertEquals(144f, g.keys[0].rect.right, eps)
        assertEquals((720f - 144f) / 3, g.keys[1].rect.width(), eps)
        assertEquals(720f, g.keys[3].rect.right, eps)
    }

    @Test
    fun `areaAt uses x against the split and rectOf returns that area`() {
        val g = GraffitiPadGeometry(360f, 240f, 2f / 3f)
        assertEquals(PadArea.LETTER, g.areaAt(239.9f, 10f))
        assertEquals(PadArea.NUMBER, g.areaAt(240f, 10f))
        assertEquals(g.letterRect, g.rectOf(PadArea.LETTER))
        assertEquals(g.numberRect, g.rectOf(PadArea.NUMBER))
    }

    @Test
    fun `keyAt finds bar buttons and nothing over the pad`() {
        val g = GraffitiPadGeometry(360f, 240f, 2f / 3f)
        assertNull(g.keyAt(10f, 191.9f))
        assertEquals(FunctionKey.MODE, g.keyAt(71.9f, 192f)?.key)
        assertEquals(FunctionKey.SPACE, g.keyAt(72f, 200f)?.key)
        assertEquals(FunctionKey.BACKSPACE, g.keyAt(200f, 239f)?.key)
        assertEquals(FunctionKey.ENTER, g.keyAt(359f, 239f)?.key)
        assertEquals(FunctionKey.MODE, g.keyAt(-1f, 200f)?.key)
        assertEquals(FunctionKey.ENTER, g.keyAt(361f, 200f)?.key)
    }

    @Test
    fun `bar labels are the tap keyboards' function key labels`() {
        val g = GraffitiPadGeometry(360f, 240f, 2f / 3f)
        assertEquals(listOf("한/영", "", "⌫", "↵"), g.keys.map { it.label })
    }

    private fun RectF2.width() = right - left
}
