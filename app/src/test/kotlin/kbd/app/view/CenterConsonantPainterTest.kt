package kbd.app.view

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

/** LLD-11 §6.6 CS-01..CS-04: what the center syllable shows and how big (SWIPE 9.5, FR-DSP-10, issue #180, FSS#36). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ViewTestSdk.SDK])
class CenterConsonantPainterTest {

    private val text = FieldInfo(FieldClass.TEXT, isPassword = false, noLearning = false, multiLine = false, enterAction = null)

    @Test
    fun `CS-01 the host's center text as it is, nothing when none`() {
        val ga = CenterText("기", "ㄱ", DotPlace.UNDER)
        assertEquals(ga, CenterConsonantPainter.textFor(ga, text))
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
    fun `CS-04 the bracket sits right above C - half a row high, centred, 1_5 C wide (FSS#36)`() {
        val c = RectF(139.2f, 76f, 220.8f, 172f)   // C on 360 × 48 dp at density 1
        val r = RectF()
        CenterConsonantPainter.otherBox(c, r)
        assertEquals(52f, r.top, 0.01f)            // 24 px = half a row: the room above C since FSS#37 (row 1 starts at 48)
        assertEquals(76f, r.bottom, 0.01f)
        assertEquals(180f, r.centerX(), 0.01f)
        assertEquals(122.4f, r.width(), 0.01f)
    }
}
