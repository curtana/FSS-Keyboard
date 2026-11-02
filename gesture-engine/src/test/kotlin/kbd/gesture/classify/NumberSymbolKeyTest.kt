package kbd.gesture.classify

import kbd.gesture.classify.ClassifierTestSupport.D
import kbd.gesture.classify.ClassifierTestSupport.D_BACK
import kbd.gesture.classify.ClassifierTestSupport.D_RUB
import kbd.gesture.classify.ClassifierTestSupport.L
import kbd.gesture.classify.ClassifierTestSupport.L_BACK
import kbd.gesture.classify.ClassifierTestSupport.L_RUB
import kbd.gesture.classify.ClassifierTestSupport.R
import kbd.gesture.classify.ClassifierTestSupport.R_BACK
import kbd.gesture.classify.ClassifierTestSupport.R_RUB
import kbd.gesture.classify.ClassifierTestSupport.TAP
import kbd.gesture.classify.ClassifierTestSupport.U
import kbd.gesture.classify.ClassifierTestSupport.U_BACK
import kbd.gesture.classify.ClassifierTestSupport.U_RUB
import kbd.gesture.classify.ClassifierTestSupport.keyDef
import kbd.gesture.classify.ClassifierTestSupport.layoutGeom
import kbd.gesture.classify.ClassifierTestSupport.outcomeOf
import kbd.gesture.classify.ClassifierTestSupport.run
import kbd.gesture.classify.ClassifierTestSupport.stroke
import kbd.gesture.classify.ClassifierTestSupport.text
import kbd.stroke.testing.Leg
import kbd.stroke.threshold.ThresholdDefaults
import org.junit.Assert.assertEquals
import org.junit.Test

/** LLD-11 §4.2 NK-01..NK-15 and SK-01..SK-06, on the keys' real layout positions (outer edges included). */
class NumberSymbolKeyTest {

    private fun expect(id: String, vararg cases: Pair<List<Leg>, String>) {
        for ((legs, want) in cases) assertEquals("key $id, $legs", text(want), outcomeOf(id, legs))
    }

    @Test fun `NK-01 key 1 tap U R D, L is the outer edge`() =
        expect("1", TAP to "1", U to "!", R to "`", D to "·", L to "1")

    @Test fun `NK-02 key 2 brackets on left and right (issue #208)`() = expect("2", TAP to "2", U to "@", L to "[", R to "]", D to "※")

    @Test fun `NK-03 key 3`() = expect("3", TAP to "3", U to "#", L to "{", R to "}", D to "±")

    @Test fun `NK-04 key 4`() = expect("4", TAP to "4", U to "$", L to ";", R to ":", D to "€")   // ← ; (owner #208 swap)

    @Test fun `NK-05 key 5`() = expect("5", TAP to "5", U to "%", L to "_", R to "+", D to "×")

    @Test fun `NK-06 key 6, left has no symbol and is the tap (issue #208)`() = expect("6", TAP to "6", U to "^", L to "6", R to "~", D to "÷")

    @Test fun `NK-07 key 7, R is the outer edge`() =
        expect("7", TAP to "7", U to "&", L to "\\", D to "≠", R to "7")

    @Test fun `NK-08 key 8, L is the outer edge`() =
        expect("8", TAP to "8", U to "*", R to "|", D to "∞", L to "8")

    @Test fun `NK-09 key 9, R is the outer edge`() =
        expect("9", TAP to "9", U to "(", L to "₩", D to "≈", R to "9")

    @Test fun `NK-10 key 0, L is the outer edge`() =
        expect("0", TAP to "0", U to ")", R to "…", D to "°", L to "0")

    @Test fun `NK-11 key 3 up rub is superscript`() = expect("3", U_RUB to "³")

    @Test fun `NK-12 key 3 down rub is subscript`() = expect("3", D_RUB to "₃")

    @Test fun `NK-13 key 3 side rubs and returns equal the exit`() =
        expect("3", R_RUB to "}", L_RUB to "{", U_BACK to "#", D_BACK to "±")

    @Test fun `NK-14 key 5 long up is the exit, length not judged`() = expect("5", listOf(Leg.U(85.0)) to "%")

    @Test fun `NK-15 key 2 return and rub, key 1 left rub on the outer edge`() {
        expect("2", U_BACK to "@", L_RUB to "[")
        expect("1", L_RUB to "1")
    }

    @Test fun `NK-01 result fields of a number key`() {
        val k = keyDef("1")
        val g = layoutGeom(k)
        val r = run(StrokeClassifier(k, ThresholdDefaults.VALUES, g), stroke(g, U))
        assertEquals(SwipeKeyKind.NUMBER, r.kind)
        assertEquals("1", r.keyId)
    }

    @Test fun `SK-01 symbol tap`() = expect("symbol", TAP to ".")

    // Issue #208: no return or rub stages; they equal the exit in that direction (DCR-02), ↓ included.
    @Test fun `SK-02 symbol left exit, return and rub are all the exit`() = expect("symbol", L to "(", L_BACK to "(", L_RUB to "(")

    @Test fun `SK-03 symbol right exit, return and rub are all the exit`() = expect("symbol", R to ")", R_BACK to ")", R_RUB to ")")

    @Test fun `SK-04 symbol up exit, return and rub are all the exit`() = expect("symbol", U to ">", U_BACK to ">", U_RUB to ">")

    @Test fun `SK-05 symbol down is the hyphen`() = expect("symbol", D to "-", D_BACK to "-", D_RUB to "-")

    @Test fun `SK-06 symbol2 tap and its directions, the right one is the screen edge`() =
        expect("symbol2", TAP to ",", L to "\"", U to "<", D to "'", R to ",", L_BACK to "\"", U_RUB to "<", D_BACK to "'")   // ← " (owner #208 swap)

    @Test fun `SK-07 the result comes from the first-pressed key - symbol right over symbol2 is still )`() =
        expect("symbol", listOf(Leg.R(80.0)) to ")")

    @Test fun `SK-08 symbol3 tap and its directions, left is the screen edge and down has no symbol`() =
        expect("symbol3", TAP to "/", R to "=", U to "?", L to "/", D to "/", R_BACK to "=", U_RUB to "?", D_BACK to "/")
}
