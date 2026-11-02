package kbd.stroke.tap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** LLD-11 §5.3 ST-01..ST-03: ScriptTables against the LLD-05A §2.3 code points. */
class ScriptTablesTest {

    private fun cp(s: String?): Int? = s?.let { if (it.codePointCount(0, it.length) == 1) it.codePointAt(0) else -1 }

    private val superLetters = mapOf(
        'a' to 0x1D43, 'b' to 0x1D47, 'c' to 0x1D9C, 'd' to 0x1D48, 'e' to 0x1D49, 'f' to 0x1DA0,
        'g' to 0x1D4D, 'h' to 0x02B0, 'i' to 0x2071, 'j' to 0x02B2, 'k' to 0x1D4F, 'l' to 0x02E1,
        'm' to 0x1D50, 'n' to 0x207F, 'o' to 0x1D52, 'p' to 0x1D56, 'q' to 0x107A5, 'r' to 0x02B3,
        's' to 0x02E2, 't' to 0x1D57, 'u' to 0x1D58, 'v' to 0x1D5B, 'w' to 0x02B7, 'x' to 0x02E3,
        'y' to 0x02B8, 'z' to 0x1DBB,
    )

    private val subLetters = mapOf(
        'a' to 0x2090, 'e' to 0x2091, 'h' to 0x2095, 'i' to 0x1D62, 'j' to 0x2C7C, 'k' to 0x2096,
        'l' to 0x2097, 'm' to 0x2098, 'n' to 0x2099, 'o' to 0x2092, 'p' to 0x209A, 'r' to 0x1D63,
        's' to 0x209B, 't' to 0x209C, 'u' to 0x1D64, 'v' to 0x1D65, 'x' to 0x2093,
    )

    @Test
    fun `ST-01 SUPER has all 26 letters as in the table`() {
        assertEquals(26, superLetters.size)
        for (c in 'a'..'z') assertEquals("$c", superLetters[c], cp(ScriptTables.SUPER[c]))
        // q is a supplementary-plane character: two UTF-16 units in one code point.
        assertEquals(2, ScriptTables.SUPER.getValue('q').length)
    }

    @Test
    fun `ST-02 SUB has 17 letters as in the table, b c d f g q w y z have none`() {
        assertEquals(17, subLetters.size)
        for (c in 'a'..'z') assertEquals("$c", subLetters[c], cp(ScriptTables.SUB[c]))
        for (c in "bcdfgqwyz") assertFalse("$c", c in ScriptTables.SUB)
        assertEquals(17 + 10, ScriptTables.SUB.size)
    }

    @Test
    fun `ST-03 digits 0-9 superscript and subscript code points`() {
        val sup = listOf(0x2070, 0x00B9, 0x00B2, 0x00B3) + (0x2074..0x2079)
        for (d in 0..9) {
            val c = '0' + d
            assertEquals("^$c", sup[d], cp(ScriptTables.SUPER[c]))
            assertEquals("_$c", 0x2080 + d, cp(ScriptTables.SUB[c]))
        }
        assertEquals(26 + 10, ScriptTables.SUPER.size)
    }
}
