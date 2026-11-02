package kbd.graffiti.eval

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * GR-36 (FSS#47): real tablet strokes in `strokes/device/` must all be recognized. They are raw touches
 * read off the device while "갸" (ㄱ, ㅣ, ㆍ, ㆍ) was written over and over: 84 strokes, 23 of them ㅣ. The long
 * ㅣ strokes end in a short move back up as the finger lifts; seven ㅣ failed before the lift-off tail rule.
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
