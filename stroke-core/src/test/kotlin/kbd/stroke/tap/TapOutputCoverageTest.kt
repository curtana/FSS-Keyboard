package kbd.stroke.tap

import kbd.stroke.geom.Dir
import kbd.stroke.layout.TapKeyKind
import kbd.stroke.layout.TapLayouts
import org.junit.Assert.assertEquals
import org.junit.Test

/** LLD-11 §5.4 TL-04: every digit and PC special character is one gesture away (FR-TAP-11). */
class TapOutputCoverageTest {

    private val gestures: List<TapGesture> = listOf(TapGesture.Tap) +
        Dir.entries.flatMap { listOf(TapGesture.Exit(it), TapGesture.ExitReturn(it), TapGesture.Rub(it)) }

    /** The 32 printable ASCII characters that are neither letters, digits nor space. */
    private val pcSpecials = "!\"#\$%&'()*+,-./:;<=>?@[\\]^_`{|}~"

    @Test fun `TL-04 all keys x all gestures cover 10 digits and 32 PC specials`() {
        assertEquals(32, pcSpecials.toSet().size)
        for (layout in listOf(TapLayouts.QWERTY, TapLayouts.DUBEOLSIK)) {
            val outputs = layout.keys.filter { it.kind != TapKeyKind.FUNCTION }
                .flatMap { key -> gestures.mapNotNull { TapKeyResolver.resolve(key, it).text } }
                .toSet()
            val missing = ("0123456789" + pcSpecials).map { it.toString() }.filter { it !in outputs }
            assertEquals(layout.name, emptyList<String>(), missing)
        }
    }
}
