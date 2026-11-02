package kbd.app.view

import kbd.app.ime.FieldClass
import kbd.app.ime.FieldInfo
import kbd.hangul.jamo.Jamo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** LLD-11 §6.6 CS-01..CS-03: what the center consonant shows and how big (SWIPE 9.5, FR-DSP-10, issue #180). */
class CenterConsonantPainterTest {

    private val text = FieldInfo(FieldClass.TEXT, isPassword = false, noLearning = false, multiLine = false, enterAction = null)

    @Test
    fun `CS-01 the consonant the next vowel attaches to, nothing when none`() {
        assertEquals("ㄱ", CenterConsonantPainter.textFor(Jamo.G, text))
        assertEquals("ㄲ", CenterConsonantPainter.textFor(Jamo.GG, text))   // a double key's consonant as itself
        assertNull(CenterConsonantPainter.textFor(null, text))              // nothing composing, or a bare vowel
        assertEquals("ㄴ", CenterConsonantPainter.textFor(Jamo.N, null))    // the tutor: no field
    }

    @Test
    fun `CS-02 only password fields draw nothing, no-learning fields draw it - FR-CMN-08`() {
        assertNull(CenterConsonantPainter.textFor(Jamo.G, text.copy(isPassword = true)))
        assertEquals("ㄱ", CenterConsonantPainter.textFor(Jamo.G, text.copy(noLearning = true)))
    }

    @Test
    fun `CS-03 the place's height, shrunk to its width`() {
        // C on a 360 dp portrait view at density 1: 81.6 × 96 px; one glyph 0.8 px wide at size 1 fits by height.
        assertEquals(96f, CenterConsonantPainter.sizeFor(81.6f, 96f, 0.8f), 0.01f)
        // A glyph 1.2 px wide at size 1 is shrunk to the width: 81.6 / 1.2.
        assertEquals(68f, CenterConsonantPainter.sizeFor(81.6f, 96f, 1.2f), 0.01f)
        assertEquals(96f, CenterConsonantPainter.sizeFor(81.6f, 96f, 0f), 0.01f)
    }
}
