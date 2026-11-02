package kbd.hangul.jamo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Vowel merge tables (LLD-03 §3.1; LLD-11 §2 HC-01..HC-05). */
class MergeTablesTest {

    private fun j(c: Char): Jamo = Jamo.ofCompat(c)!!

    /** "앞뒤결과" strings → ((prev, next), result). */
    private fun pairs(vararg s: String): Map<Pair<Jamo, Jamo>, Jamo> =
        s.associate { (j(it[0]) to j(it[1])) to j(it[2]) }

    private val dubeolsik = pairs("ㅗㅏㅘ", "ㅗㅐㅙ", "ㅗㅣㅚ", "ㅜㅓㅝ", "ㅜㅔㅞ", "ㅜㅣㅟ", "ㅡㅣㅢ")

    /** Every other (prev, next) over all 51×51 jamo pairs gives null. */
    private fun assertOnly(table: VowelMergeTable, expected: Map<Pair<Jamo, Jamo>, Jamo>) {
        for (prev in Jamo.entries) for (next in Jamo.entries) {
            assertEquals("$prev+$next", expected[prev to next], table.merge(prev, next))
        }
    }

    @Test
    fun `HC-01 DUBEOLSIK merges its 7 pairs`() {
        assertEquals(7, dubeolsik.size)
        assertEquals("ㅘㅙㅚㅝㅞㅟㅢ", dubeolsik.values.map { it.compat }.joinToString(""))
        for ((k, v) in dubeolsik) assertEquals("$k", v, MergeTables.DUBEOLSIK.merge(k.first, k.second))
    }

    @Test
    fun `HC-02 DUBEOLSIK does not merge other pairs`() {
        for (s in listOf("ㅏㅣ", "ㅓㅣ", "ㅘㅣ")) assertNull(s, MergeTables.DUBEOLSIK.merge(j(s[0]), j(s[1])))
        assertOnly(MergeTables.DUBEOLSIK, dubeolsik)
    }

    @Test
    fun `HC-05 NONE never merges (graffiti, swipe since issue 156)`() {
        assertOnly(MergeTables.NONE, emptyMap())
    }
}
