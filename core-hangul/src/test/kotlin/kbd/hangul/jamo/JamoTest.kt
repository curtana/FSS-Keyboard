package kbd.hangul.jamo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** `Jamo` layout and `JamoTables` (LLD-03 §3.1, §3.5). */
class JamoTest {

    @Test
    fun `layout has 30 consonants including 11 compound finals and 21 vowels in compat order`() {
        val consonants = Jamo.entries.filter { it.kind == JamoKind.CONSONANT }
        val vowels = Jamo.entries.filter { it.kind == JamoKind.VOWEL }
        assertEquals(30, consonants.size)
        assertEquals(11, consonants.count { it.choIndex == -1 })
        assertEquals(21, vowels.size)
        assertEquals((0x3131..0x3163).map { it.toChar() }, Jamo.entries.map { it.compat })
    }

    @Test
    fun `indices match Unicode conjoining jamo order`() {
        // Conjoining jamo (U+1100 initials, U+1161 medials, U+11A7+n finals) share the letter name with
        // the compat jamo: "HANGUL CHOSEONG KIYEOK" / "HANGUL LETTER KIYEOK".
        fun letter(code: Int): String = Character.getName(code).substringAfter(' ').substringAfter(' ')
        for (j in Jamo.entries) {
            val name = letter(j.compat.code)
            if (j.choIndex >= 0) assertEquals("$j cho", letter(0x1100 + j.choIndex), name)
            if (j.jungIndex >= 0) assertEquals("$j jung", letter(0x1161 + j.jungIndex), name)
            if (j.jongIndex >= 0) assertEquals("$j jong", letter(0x11A7 + j.jongIndex), name)
        }
        assertEquals((0..18).toList(), Jamo.entries.map { it.choIndex }.filter { it >= 0 }.sorted())
        assertEquals((0..20).toList(), Jamo.entries.map { it.jungIndex }.filter { it >= 0 }.sorted())
        assertEquals((1..27).toList(), Jamo.entries.map { it.jongIndex }.filter { it >= 0 }.sorted())
        for (j in Jamo.entries) {
            if (j.kind == JamoKind.VOWEL) {
                assertTrue("$j", j.choIndex == -1 && j.jongIndex == -1 && j.jungIndex >= 0)
            } else {
                assertTrue("$j", j.jungIndex == -1)
            }
        }
    }

    @Test
    fun `syllable formula yields the expected syllable`() {
        // 닭 = ㄷ ㅏ ㄺ
        val code = 0xAC00 + (Jamo.D.choIndex * 21 + Jamo.A.jungIndex) * 28 + Jamo.RG.jongIndex
        assertEquals('닭', code.toChar())
    }

    @Test
    fun `ofCompat finds every jamo and rejects other chars`() {
        for (j in Jamo.entries) assertSame(j, Jamo.ofCompat(j.compat))
        for (c in listOf('a', ' ', '가', 'ᄀ', '㄰', 'ㅤ')) assertNull(Jamo.ofCompat(c))
    }

    @Test
    fun `DOUBLE_OF maps the five base consonants`() {
        assertEquals(
            mapOf(Jamo.G to Jamo.GG, Jamo.D to Jamo.DD, Jamo.B to Jamo.BB, Jamo.S to Jamo.SS, Jamo.J to Jamo.JJ),
            JamoTables.DOUBLE_OF,
        )
    }

    @Test
    fun `JONG_CAPABLE is the 16 initials without DD BB JJ`() {
        val expected = "ㄱㄲㄴㄷㄹㅁㅂㅅㅆㅇㅈㅊㅋㅌㅍㅎ".map { Jamo.ofCompat(it)!! }.toSet()
        assertEquals(16, JamoTables.JONG_CAPABLE.size)
        assertEquals(expected, JamoTables.JONG_CAPABLE)
    }

    @Test
    fun `COMPOUND_JONG has the 11 pairs and covers every compound final`() {
        val expected = listOf(
            "ㄱㅅㄳ", "ㄴㅈㄵ", "ㄴㅎㄶ", "ㄹㄱㄺ", "ㄹㅁㄻ", "ㄹㅂㄼ",
            "ㄹㅅㄽ", "ㄹㅌㄾ", "ㄹㅍㄿ", "ㄹㅎㅀ", "ㅂㅅㅄ",
        ).associate { s -> (Jamo.ofCompat(s[0])!! to Jamo.ofCompat(s[1])!!) to Jamo.ofCompat(s[2])!! }
        assertEquals(expected, JamoTables.COMPOUND_JONG)
        assertEquals(Jamo.entries.filter { it.kind == JamoKind.CONSONANT && it.choIndex == -1 }.toSet(),
            JamoTables.COMPOUND_JONG.values.toSet())
    }
}
