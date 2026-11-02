package kbd.graffiti.eval

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * GR-36 (FSS#47): real tablet strokes in `strokes/device/` must all be recognized. They are raw touches
 * read off the device while "갸" was written ten times (ㄱ, ㅣ, ㆍ, ㆍ); six of the ten ㅣ strokes end in a
 * short move back up as the finger lifts.
 */
class DeviceStrokeTest {

    @Test
    fun `GR-36 device strokes ga - every stroke recognized, lift-off tails included`() {
        val sets = GraffitiEval.load("device")
        for ((folder, list) in sets) {
            val s = GraffitiEval.score(folder, list)
            println(s)
            assertEquals("misses ${s.misses}", s.total, s.correct)
        }
    }
}
