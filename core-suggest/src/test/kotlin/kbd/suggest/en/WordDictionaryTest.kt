package kbd.suggest.en

import java.io.StringReader
import java.util.Locale
import java.util.Random
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** LLD-11 §7.12 (가) WD-01..WD-09. Data: `src/test/resources/suggest/mini_words.txt`. */
class WordDictionaryTest {

    private val dict = mini()

    private fun ids(vararg v: Int) = v

    @Test
    fun `WD-01 mini dictionary size and forms by rank`() {
        assertEquals(33, dict.size)
        assertEquals("the", dict.form(0))
        assertEquals("I", dict.form(7))
        assertEquals("I'm", dict.form(10))
        assertEquals("Monday", dict.form(22))
        assertEquals("rock'n'roll", dict.form(31))
    }

    @Test
    fun `WD-02 skipped lines take no number and one trailing CR is stripped`() {
        val expected = listOf(
            "the", "of", "and", "to", "in", "a", "is", "I", "that", "it", "I'm", "for", "don't", "it's", "do",
            "they", "there", "then", "them", "these", "done", "don", "Monday", "money",
            "month", "monster", "hello", "help", "held", "helm", "McDonald", "rock'n'roll",
            "abcdefghijklmnopqrstuvwxyzabcdef",
        )
        assertEquals(expected, (0 until dict.size).map { dict.form(it) })
        val crlf = WordDictionary.read(StringReader("go\r\nup\r\n"))
        assertEquals(listOf("go", "up"), (0 until crlf.size).map { crlf.form(it) })
        assertArrayEquals(ids(0), crlf.top("go"))
    }

    @Test
    fun `WD-03 top K = 4 in rank order`() {
        assertArrayEquals(ids(0, 3, 8, 15), dict.top("t"))
        assertArrayEquals(ids(0, 8, 15, 16), dict.top("th"))
        assertArrayEquals(ids(0, 15, 16, 17), dict.top("the"))
    }

    @Test
    fun `WD-04 lowercase keys with apostrophes`() {
        assertArrayEquals(ids(4, 6, 7, 9), dict.top("i"))
        assertArrayEquals(ids(10), dict.top("i'"))
        assertArrayEquals(ids(12, 14, 20, 21), dict.top("do"))
        assertArrayEquals(ids(30), dict.top("mc"))
        assertArrayEquals(ids(31), dict.top("rock'"))
    }

    @Test
    fun `WD-05 top equals brute force for every prefix (mini and 2000 generated words)`() {
        checkBruteForce(dict, (0 until dict.size).map { dict.form(it) })

        val rnd = Random(20261124L) // fixed seed: the same list every run
        val seen = HashSet<String>()
        val words = ArrayList<String>()
        while (words.size < 2000) {
            val len = 1 + rnd.nextInt(8)
            val w = buildString { repeat(len) { append('a' + rnd.nextInt(5)) } }
            if (seen.add(w)) words += w
        }
        val gen = WordDictionary.read(StringReader(words.joinToString("\n", postfix = "\n")))
        assertEquals(2000, gen.size)
        checkBruteForce(gen, words)
    }

    private fun checkBruteForce(d: WordDictionary, forms: List<String>) {
        val keys = forms.map { it.lowercase(Locale.ROOT) }
        val prefixes = keys.flatMap { k -> (1..k.length).map { k.substring(0, it) } }.toSortedSet()
        var checked = 0
        for (p in prefixes) {
            val expect = keys.indices.filter { keys[it].startsWith(p) }.take(WordIndex.K)
            val got = d.top(p)
            if (expect.isEmpty()) assertNull(p, got) else assertArrayEquals(p, expect.toIntArray(), got)
            checked++
        }
        // Prefixes that no word has: one more letter after each full key.
        for (k in keys) for (c in "abcdez'") {
            val p = k + c
            if (keys.none { it.startsWith(p) }) assertNull(p, d.top(p))
        }
        assertEquals(prefixes.size, checked)
    }

    @Test
    fun `WD-06 unknown, empty, uppercase and foreign characters give null`() {
        assertNull(dict.top("xyz"))
        assertNull(dict.top(""))
        assertNull(dict.top("T"))
        assertNull(dict.top("a-"))
    }

    @Test
    fun `WD-07 32 characters accepted, 33 skipped`() {
        assertEquals("abcdefghijklmnopqrstuvwxyzabcdef", dict.form(32))
        assertArrayEquals(ids(32), dict.top("abcdefghijklmnopqrstuvwxyzabcdef"))
        assertNull(dict.top("abcdefghijklmnopqrstuvwxyzabcdefg"))
        val one = WordDictionary.read(StringReader("abcdefghijklmnopqrstuvwxyzabcdefg\n"))
        assertEquals(0, one.size)
    }

    @Test
    fun `WD-08 top returns a copy`() {
        val first = dict.top("t")!!
        first[0] = 99
        first[1] = -5
        assertArrayEquals(ids(0, 3, 8, 15), dict.top("t"))
    }

    @Test
    fun `WD-09 empty input and header only`() {
        for (text in listOf("", "#fss-words 1\n#name none\n")) {
            val d = WordDictionary.read(StringReader(text))
            assertEquals(0, d.size)
            assertNull(d.top("a"))
        }
    }

    companion object {
        fun mini(): WordDictionary {
            val stream = WordDictionaryTest::class.java.getResourceAsStream("/suggest/mini_words.txt")
                ?: error("test resource missing")
            return stream.reader(Charsets.UTF_8).use { WordDictionary.read(it) }
        }
    }
}
