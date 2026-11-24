package kbd.suggest.en

import kbd.suggest.en.CaseRule.Shape
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** LLD-11 §7.12 (나) CR-01..CR-05 (LLD-12 §5). */
class CaseRuleTest {

    @Test
    fun `CR-01 shapeOf`() {
        val cases = listOf(
            "the" to Shape.LOWER, "The" to Shape.FIRST_UPPER, "T" to Shape.FIRST_UPPER, "TH" to Shape.ALL_UPPER,
            "tHe" to Shape.MIXED, "hELlo" to Shape.MIXED, "don'" to Shape.LOWER, "DON'" to Shape.ALL_UPPER,
            "I'm" to Shape.FIRST_UPPER, "i" to Shape.LOWER,
        )
        for ((typed, shape) in cases) assertEquals(typed, shape, CaseRule.shapeOf(typed))
    }

    @Test
    fun `CR-02 LOWER keeps the dictionary form`() {
        check("i" to "I", "I", "i'm" to "I'm", "I'm", "mon" to "Monday", "Monday", "mc" to "McDonald", "McDonald", "the" to "the", "the")
    }

    @Test
    fun `CR-03 FIRST_UPPER raises the first letter only`() {
        check("Th" to "the", "The", "I" to "in", "In", "Mc" to "McDonald", "McDonald", "M" to "money", "Money")
    }

    @Test
    fun `CR-04 ALL_UPPER raises every letter`() {
        check("MON" to "Monday", "MONDAY", "I'M" to "I'm", "I'M", "DON'" to "don't", "DON'T", "MC" to "McDonald", "MCDONALD")
    }

    @Test
    fun `CR-05 MIXED gives null`() {
        assertNull(CaseRule.apply("hELlo", "hello"))
        assertNull(CaseRule.apply("tHe", "the"))
    }

    /** Triples written as (typed to form), expected. */
    private fun check(vararg v: Any) {
        for (i in v.indices step 2) {
            @Suppress("UNCHECKED_CAST")
            val (typed, form) = v[i] as Pair<String, String>
            assertEquals("$typed/$form", v[i + 1], CaseRule.apply(typed, form))
        }
    }
}
