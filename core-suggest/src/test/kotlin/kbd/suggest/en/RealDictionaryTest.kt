package kbd.suggest.en

import java.security.MessageDigest
import java.util.Random
import kbd.suggest.SuggestKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * LLD-11 §7.12 (가) WD-10..WD-13 on the real word list (`src/main/resources/suggest/en_words.txt`, built by
 * `tools/en-dict`). The WD-12 lists are the ones the committed file gives; rebuilding the file means updating them
 * (HLD 11).
 */
class RealDictionaryTest {

    private val raw: ByteArray = checkNotNull(EnglishDictionary::class.java.getResourceAsStream(EnglishDictionary.RESOURCE)).use { it.readBytes() }
    private val text = String(raw, Charsets.UTF_8)
    private val header = text.lineSequence().takeWhile { it.startsWith("#") }.toList()
    private val dict = EnglishDictionary.load()

    @Test
    fun `WD-10 the real list loads - 50,000 words, its header, and the sha256 of the word lines`() {
        assertEquals(50_000, dict.size)
        assertEquals("#fss-words 1", header.first())
        assertTrue(header.contains("#words 50000"))
        val sha = header.single { it.startsWith("#sha256 ") }.removePrefix("#sha256 ")
        val headerBytes = header.sumOf { it.toByteArray(Charsets.UTF_8).size + 1 }   // each with its LF
        val words = raw.copyOfRange(headerBytes, raw.size)
        val hex = MessageDigest.getInstance("SHA-256").digest(words).joinToString("") { "%02x".format(it) }
        assertEquals(sha, hex)
    }

    @Test
    fun `WD-11 every form fits the line rule, keys are unique, a, I, I'm and don't are in, one letter only a and I`() {
        val rule = Regex("^[A-Za-z]+(?:'[A-Za-z]+)*$")
        val forms = (0 until dict.size).map(dict::form)
        assertTrue(forms.all { rule.matches(it) && it.length <= WordChars.MAX_WORD })
        assertEquals(forms.size, forms.map { it.lowercase() }.toSet().size)
        assertTrue(forms.containsAll(listOf("a", "I", "I'm", "don't")))
        assertEquals(setOf("a", "I"), forms.filter { it.length == 1 }.toSet())
        // The lines of the file are the forms, in rank order (nothing skipped by the reader).
        assertEquals(forms, text.lineSequence().filter { it.isNotEmpty() && !it.startsWith("#") }.toList())
    }

    /** The cells for [w] typed after a space. */
    private fun cells(w: String): List<String> {
        val d = EnglishWordSourceTest.Driver(dict)
        d.key(SuggestKey.Space)
        d.type(w)
        return d.cells
    }

    @Test
    fun `WD-12 cells of the real list for fixed prefixes`() {
        val golden = linkedMapOf(
            "t" to listOf("the", "to", "that"),
            "th" to listOf("the", "that", "this"),
            "i" to listOf("in", "is", "it"),
            "i'" to listOf("I'm", "I'd", "I'll"),
            "don" to listOf("done", "don't", "Don"),
            "mon" to listOf("money", "months", "month"),
            "hel" to listOf("help", "held", "helped"),
            "Th" to listOf("The", "That", "This"),
            "MON" to listOf("MONEY", "MONTHS", "MONTH"),
        )
        val got = golden.keys.associateWith(::cells)
        println("WD-12 " + got)
        assertEquals(golden, got)
        assertEquals("I'm", got.getValue("i'").first())
        assertTrue(got.getValue("don").contains("don't"))
        assertTrue(got.values.all { it.size <= 3 })
    }

    @Test
    fun `WD-13 candidates for 10,000 fixed prefixes take less than 1 ms on average`() {
        val rnd = Random(73)
        val prefixes = ArrayList<String>(10_000)
        while (prefixes.size < 10_000) {
            val form = dict.form(rnd.nextInt(dict.size))
            val n = 1 + rnd.nextInt(minOf(6, form.length))
            val p = form.substring(0, n)
            if (p.all(WordChars::isLetter)) prefixes += p.lowercase()
        }
        // One typed source per prefix, built outside the timing; the first read of candidates computes them.
        fun sources() = prefixes.map { p ->
            EnglishWordSource(dict).also { s ->
                s.onKey(SuggestKey.Space, EnglishWordSourceTest.FakePeek())
                s.afterKey(SuggestKey.Space)
                for (c in p) {
                    val k = SuggestKey.Text(c.toString())
                    s.onKey(k, EnglishWordSourceTest.FakePeek())
                    s.afterKey(k)
                }
            }
        }
        for (s in sources()) s.candidates   // warm up
        val timed = sources()
        val t0 = System.nanoTime()
        var n = 0
        for (s in timed) n += s.candidates.size
        val avgMs = (System.nanoTime() - t0) / 1e6 / timed.size
        println("WD-13 average candidates time: %.4f ms over %d prefixes (%d cells)".format(avgMs, timed.size, n))
        assertTrue("average $avgMs ms", avgMs < 1.0)
    }
}
