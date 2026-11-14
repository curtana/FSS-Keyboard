package kbd.stroke.tap

import kbd.stroke.geom.Dir
import kbd.stroke.layout.FunctionKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [ModeKeyGesture], shared by the tap, swipe and graffiti views (SRS FR-CMN-13, HLD 8.2, issue #205,
 * LLD-11 MK-00): a first crossed edge up opens the settings, everything else is the tap's mode change.
 */
class ModeKeyStrokeTest {

    private fun opens(g: TapGesture) = ModeKeyGesture.of(g) == ModeKeyGesture.OPEN_SETTINGS

    @Test
    fun `MK-00 only a first crossed edge up opens the settings, coming back or rubbing included`() {
        assertTrue(opens(TapGesture.Exit(Dir.UP)))
        assertTrue(opens(TapGesture.ExitReturn(Dir.UP)))
        assertTrue(opens(TapGesture.Rub(Dir.UP)))   // only the first edge counts (FR-CMN-13)
        assertFalse(opens(TapGesture.Tap))
        for (d in listOf(Dir.DOWN, Dir.LEFT, Dir.RIGHT)) {
            assertFalse("$d", opens(TapGesture.Exit(d)))
            assertFalse("$d", opens(TapGesture.ExitReturn(d)))
            assertFalse("$d", opens(TapGesture.Rub(d)))
        }
    }

    @Test
    fun `MK-00b the settings function key is gone`() {
        assertEquals(
            listOf("BACKSPACE", "SPACE", "ENTER", "TAB", "MODE", "ARROW_LEFT", "ARROW_UP", "ARROW_DOWN", "ARROW_RIGHT", "ESC", "DELETE", "CTRL", "ALT"),   // CTRL, ALT: FSS#98
            FunctionKey.entries.map { it.name },
        )
    }
}
