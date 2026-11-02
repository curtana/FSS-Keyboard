package kbd.app.view

import android.graphics.PointF
import android.graphics.RectF
import kbd.app.ime.FieldClass
import kbd.app.ime.FieldInfo
import kbd.hangul.stroke.DotPlace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** LLD-11 §6.6 CS-01..CS-04: what the center syllable shows, how big and where its dots go (SWIPE 9.5, FR-DSP-10, issue #180, FSS#36). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ViewTestSdk.SDK])
class CenterConsonantPainterTest {

    private val text = FieldInfo(FieldClass.TEXT, isPassword = false, noLearning = false, multiLine = false, enterAction = null)

    @Test
    fun `CS-01 the host's center text as it is, nothing when none`() {
        val go = CenterText("ㄱ", listOf(DotPlace.UNDER))
        assertEquals(go, CenterConsonantPainter.textFor(go, text))
        assertEquals(CenterText("ㄲ"), CenterConsonantPainter.textFor(CenterText("ㄲ"), text))
        assertNull(CenterConsonantPainter.textFor(null, text))                    // nothing composing, or a bare vowel
        assertEquals(CenterText("ㄴ"), CenterConsonantPainter.textFor(CenterText("ㄴ"), null))   // no field
    }

    @Test
    fun `CS-02 only password fields draw nothing, no-learning fields draw it - FR-CMN-08`() {
        assertNull(CenterConsonantPainter.textFor(CenterText("각"), text.copy(isPassword = true)))
        assertEquals(CenterText("각"), CenterConsonantPainter.textFor(CenterText("각"), text.copy(noLearning = true)))
    }

    @Test
    fun `CS-03 the place's height, shrunk to its width`() {
        // C on a 360 dp portrait view at density 1: 81.6 × 96 px; one glyph 0.8 px wide at size 1 fits by height.
        assertEquals(96f, CenterConsonantPainter.sizeFor(81.6f, 96f, 0.8f), 0.01f)
        // A glyph 1.2 px wide at size 1 is shrunk to the width: 81.6 / 1.2.
        assertEquals(68f, CenterConsonantPainter.sizeFor(81.6f, 96f, 1.2f), 0.01f)
        assertEquals(96f, CenterConsonantPainter.sizeFor(81.6f, 96f, 0f), 0.01f)
    }

    @Test
    fun `CS-04 dots in place - under in a row, right in a column, the syllable beside them (FSS#36)`() {
        val c = RectF(139.2f, 76f, 220.8f, 172f)   // C on 360 × 48 dp at density 1: 81.6 × 96
        val r = RectF()
        val p = ArrayList<PointF>()
        CenterConsonantPainter.textBox(c, emptyList(), r)
        same(c, r)                                                           // no dots: all of C
        CenterConsonantPainter.textBox(c, listOf(DotPlace.UNDER, DotPlace.UNDER), r)
        same(RectF(139.2f, 76f, 220.8f, 76f + 96f * 0.7f), r)                // the top 70 %, as ㄱ in 고
        CenterConsonantPainter.textBox(c, listOf(DotPlace.RIGHT), r)
        same(RectF(139.2f, 76f, 139.2f + 81.6f * 0.7f, 172f), r)             // the left 70 %, as ㄱ in 거
        CenterConsonantPainter.dotCenters(c, listOf(DotPlace.UNDER), p)
        assertEquals(1, p.size)                                              // 고: one under the middle
        assertEquals(180f, p[0].x, 0.01f)
        assertEquals(76f + 96f * 0.85f, p[0].y, 0.01f)
        CenterConsonantPainter.dotCenters(c, listOf(DotPlace.UNDER, DotPlace.UNDER), p)
        assertEquals(p[0].y, p[1].y, 0.01f)                                  // 교: side by side (··)
        assertEquals(81.6f * 0.3f, p[1].x - p[0].x, 0.01f)
        assertEquals(180f, (p[0].x + p[1].x) / 2, 0.01f)
        CenterConsonantPainter.dotCenters(c, listOf(DotPlace.RIGHT, DotPlace.RIGHT), p)
        assertEquals(p[0].x, p[1].x, 0.01f)                                  // 겨: one above the other (:)
        assertEquals(139.2f + 81.6f * 0.85f, p[0].x, 0.01f)
        assertEquals(96f * 0.3f, p[1].y - p[0].y, 0.01f)
        assertEquals(124f, (p[0].y + p[1].y) / 2, 0.01f)
    }

    private fun same(want: RectF, got: RectF) {
        for ((w, g) in listOf(want.left to got.left, want.top to got.top, want.right to got.right, want.bottom to got.bottom)) assertEquals(w, g, 0.01f)
    }
}
