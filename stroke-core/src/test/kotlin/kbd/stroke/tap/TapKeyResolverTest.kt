package kbd.stroke.tap

import kbd.stroke.geom.Diag
import kbd.stroke.geom.Dir
import kbd.stroke.geom.Dir.DOWN
import kbd.stroke.geom.Dir.LEFT
import kbd.stroke.geom.Dir.RIGHT
import kbd.stroke.geom.Dir.UP
import kbd.stroke.geom.WAYS
import kbd.stroke.geom.Way
import kbd.stroke.layout.TapLayout
import kbd.stroke.layout.TapLayouts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * LLD-11 §5.2 TK-01..TK-24 (one per cell of the SRS tap action table) and §5.3 TA-01..TA-12
 * (symbol assignment table), with keys taken from [TapLayouts]. Key ids are QWERTY positions:
 * r = ㄱ, a = ㅁ, o = ㅐ, i = ㅑ, p = ㅔ, l = ㅣ, q = ㅂ, s = ㄴ, f = ㄹ, t = ㅅ, x = ㅌ, v = ㅍ.
 */
class TapKeyResolverTest {

    private val tap = TapGesture.Tap
    private fun exit(d: Way) = TapGesture.Exit(d)
    private fun rub(d: Way) = TapGesture.Rub(d)
    private fun back(d: Way) = TapGesture.ExitReturn(d)

    private fun q(id: String, g: TapGesture) = resolve(TapLayouts.QWERTY, id, g)
    private fun d(id: String, g: TapGesture) = resolve(TapLayouts.DUBEOLSIK, id, g)
    private fun resolve(layout: TapLayout, id: String, g: TapGesture) =
        TapKeyResolver.resolve(layout.keys.single { it.id == id }, g)

    private fun t(s: String) = TapOutput(s, null, errorVibe = false)
    private fun j(c: Char) = TapOutput(null, c, errorVibe = false)
    /** A consonant then a vowel, one stroke up/down/left/right on a consonant key (FSS#122, #124). */
    private fun cv(c: Char, v: Char) = TapOutput(null, c, errorVibe = false, vowel = v)

    // --- §5.2 action table: rows 1..8 × QWERTY / Dubeolsik / number row and symbol ---

    @Test fun `TK-01 tap QWERTY letter is lowercase`() = assertEquals(t("a"), q("a", tap))

    @Test fun `TK-02 tap Dubeolsik letter is base jamo`() = assertEquals(j('ㄱ'), d("r", tap))

    @Test fun `TK-03 tap number row and symbol is the tap symbol`() {
        assertEquals(t("3"), q("3", tap))
        assertEquals(t(","), q("comma", tap))
    }

    @Test fun `TK-04 up exit QWERTY is uppercase with no shift state`() {
        assertEquals(t("A"), q("a", exit(UP)))
        assertEquals(t("A"), q("a", exit(UP)))
    }

    @Test fun `TK-05 up exit Dubeolsik is shift jamo, paired vowel or consonant + ㅗ`() {
        assertEquals(cv('ㄱ', 'ㅗ'), d("r", exit(UP)))            // 고 (FSS#124; ㄲ until then, now 4 ↓)
        assertEquals(j('ㅒ'), d("o", exit(UP)))
        assertEquals(cv('ㅁ', 'ㅗ'), d("a", exit(UP)))            // 모 (FSS#122; the tap until then)
        assertEquals(cv('ㄴ', 'ㅗ'), d("s", exit(UP)))            // FSS#124; ㄵ in FSS#97..#122
        assertEquals(cv('ㄹ', 'ㅗ'), d("f", exit(UP)))            // FSS#124; ㄺ in FSS#109..#122
    }

    @Test fun `TK-06 up exit number row and symbol is the up symbol`() {
        assertEquals(t("#"), q("3", exit(UP)))
        assertEquals(t("<"), q("comma", exit(UP)))
    }

    @Test fun `TK-07 down exit QWERTY is down symbol or lowercase`() {
        assertEquals(t("a"), q("a", exit(DOWN)))
        assertEquals(t("i"), q("i", exit(DOWN)))   // i has no down symbol (#198)
        assertEquals(t("|"), q("p", exit(DOWN)))
    }

    @Test fun `TK-08 down exit Dubeolsik is consonant + ㅜ, paired vowel, down symbol or base jamo`() {
        assertEquals(cv('ㄱ', 'ㅜ'), d("r", exit(DOWN)))          // 구 (FSS#122; the tap in FSS#109, ㄳ in FSS#97)
        assertEquals(cv('ㄹ', 'ㅜ'), d("f", exit(DOWN)))          // FSS#124; ㄿ in FSS#109..#122
        assertEquals(cv('ㅁ', 'ㅜ'), d("a", exit(DOWN)))
        assertEquals(j('ㅐ'), d("o", exit(DOWN)))   // o has no down symbol (#198)
        assertEquals(t(":"), d("l", exit(DOWN)))
    }

    @Test fun `TK-09 down exit number row and symbol is down symbol or tap`() {
        assertEquals(t("3"), q("3", exit(DOWN)))
        assertEquals(t("+"), q("minus", exit(DOWN)))
        assertEquals(t("."), q("period", exit(DOWN)))
        assertEquals(j('ㄸ'), d("3", exit(DOWN)))                // Dubeolsik 1..5: the tense consonant (FSS#124)
        assertEquals(t("7"), d("7", exit(DOWN)))
        assertEquals(t("1"), q("1", exit(DOWN)))                 // QWERTY 1 ↓ is empty since ~ went left (FSS#124)
    }

    @Test fun `TK-10 left and right exit QWERTY`() {
        assertEquals(t("a"), q("a", exit(LEFT)))
        assertEquals(t("a"), q("a", exit(RIGHT)))
        assertEquals(t("["), q("i", exit(LEFT)))
        assertEquals(t("]"), q("i", exit(RIGHT)))
    }

    @Test fun `TK-11 left and right exit Dubeolsik`() {
        assertEquals(cv('ㄱ', 'ㅓ'), d("r", exit(LEFT)))          // 거 (FSS#122)
        assertEquals(cv('ㄱ', 'ㅏ'), d("r", exit(RIGHT)))         // 가 (FSS#124; ㄳ in FSS#109..#122)
        assertEquals(cv('ㄹ', 'ㅓ'), d("f", exit(LEFT)))          // 러 (FSS#124; ㄻ until then)
        assertEquals(cv('ㄹ', 'ㅏ'), d("f", exit(RIGHT)))         // 라 (FSS#124; ㅀ until then)
        assertEquals(t("{"), d("o", exit(LEFT)))
        assertEquals(t("}"), d("o", exit(RIGHT)))
    }

    @Test fun `TK-12 left and right exit number row and symbol`() {
        assertEquals(t("3"), q("3", exit(LEFT)))
        assertEquals(t("3"), q("3", exit(RIGHT)))
        assertEquals(t("'"), q("comma", exit(LEFT)))
        assertEquals(t("\""), q("comma", exit(RIGHT)))
        assertEquals(t("."), q("period", exit(RIGHT)))
        assertEquals(t("="), q("minus", exit(RIGHT)))
        assertEquals(t("~"), q("1", exit(LEFT)))                 // ~ ← since FSS#124 (↓ until then)
        assertEquals(t("~"), d("1", exit(LEFT)))
    }

    @Test fun `TK-13 up rub QWERTY is superscript`() = assertEquals(t("ᵃ"), q("a", rub(UP)))

    @Test fun `TK-14 up rub Dubeolsik is base jamo`() {
        assertEquals(j('ㄱ'), d("r", rub(UP)))
        for ((id, base) in listOf("s" to 'ㄴ', "f" to 'ㄹ', "v" to 'ㅍ')) assertEquals(id, j(base), d(id, rub(UP)))   // no consonant + vowel
    }

    @Test fun `TK-15 up rub number row and symbol is the tap symbol`() {
        assertEquals(t("3"), q("3", rub(UP)))
        assertEquals(t(","), q("comma", rub(UP)))
    }

    @Test fun `TK-16 down rub QWERTY is subscript or letter with error vibration`() {
        assertEquals(t("ₐ"), q("a", rub(DOWN)))
        assertEquals(TapOutput("b", null, errorVibe = true), q("b", rub(DOWN)))
    }

    @Test fun `TK-17 down rub Dubeolsik is base jamo`() {
        // No consonant + vowel on a rub, though ↓ exit types one on each of these (FSS#122, #124).
        for ((id, base) in listOf("r" to 'ㄱ', "s" to 'ㄴ', "f" to 'ㄹ', "q" to 'ㅂ', "t" to 'ㅅ')) assertEquals(id, j(base), d(id, rub(DOWN)))
    }

    @Test fun `TK-18 down rub number row and symbol is the tap symbol`() {
        assertEquals(t("3"), q("3", rub(DOWN)))
        assertEquals(t(","), q("comma", rub(DOWN)))
        assertEquals(t("1"), d("1", rub(DOWN)))                  // no tense consonant on a rub (FSS#124)
    }

    @Test fun `TK-19 left and right rub QWERTY resolves like an exit`() {
        assertEquals(t("a"), q("a", rub(LEFT)))
        assertEquals(t("a"), q("a", rub(RIGHT)))
        assertEquals(t("["), q("i", rub(LEFT)))
        assertEquals(t("]"), q("i", rub(RIGHT)))
    }

    @Test fun `TK-20 left and right rub Dubeolsik resolves like an exit`() {
        assertEquals(cv('ㄱ', 'ㅓ'), d("r", rub(LEFT)))           // like the exit (FSS#122)
        assertEquals(cv('ㄱ', 'ㅏ'), d("r", rub(RIGHT)))          // like the exit (FSS#124)
        assertEquals(cv('ㄹ', 'ㅓ'), d("f", rub(LEFT)))
        assertEquals(cv('ㄹ', 'ㅏ'), d("f", rub(RIGHT)))
        assertEquals(t("{"), d("o", rub(LEFT)))
        assertEquals(t("}"), d("o", rub(RIGHT)))
    }

    @Test fun `TK-21 left and right rub number row and symbol resolves like an exit`() {
        assertEquals(t("3"), q("3", rub(LEFT)))
        assertEquals(t("3"), q("3", rub(RIGHT)))
        assertEquals(t("'"), q("comma", rub(LEFT)))
        assertEquals(t("\""), q("comma", rub(RIGHT)))
        assertEquals(t("~"), d("1", rub(LEFT)))                  // like the exit (FSS#124)
    }

    @Test fun `TK-22 exit and return QWERTY resolves like an exit`() {
        assertEquals(t("A"), q("a", back(UP)))
        assertEquals(t("I"), q("i", back(UP)))
        assertEquals(t("a"), q("a", back(DOWN)))
        assertEquals(t("|"), q("p", back(DOWN)))
        assertEquals(t("a"), q("a", back(LEFT)))
        assertEquals(t("["), q("i", back(LEFT)))
        assertEquals(t("a"), q("a", back(RIGHT)))
        assertEquals(t("]"), q("i", back(RIGHT)))
    }

    @Test fun `TK-23 exit and return Dubeolsik resolves like an exit`() {
        assertEquals(cv('ㄱ', 'ㅗ'), d("r", back(UP)))            // like the exit (FSS#124)
        assertEquals(j('ㅒ'), d("o", back(UP)))
        assertEquals(cv('ㄴ', 'ㅗ'), d("s", back(UP)))
        assertEquals(cv('ㄱ', 'ㅜ'), d("r", back(DOWN)))          // like the exit: 구 (FSS#122)
        assertEquals(t(":"), d("l", back(DOWN)))
        assertEquals(cv('ㄱ', 'ㅓ'), d("r", back(LEFT)))
        assertEquals(cv('ㄹ', 'ㅓ'), d("f", back(LEFT)))
        assertEquals(t("{"), d("o", back(LEFT)))
        assertEquals(cv('ㄱ', 'ㅏ'), d("r", back(RIGHT)))
        assertEquals(t("}"), d("o", back(RIGHT)))
    }

    @Test fun `TK-24 exit and return number row and symbol resolves like an exit`() {
        assertEquals(t("#"), q("3", back(UP)))
        assertEquals(t("<"), q("comma", back(UP)))
        assertEquals(t("3"), q("3", back(DOWN)))
        assertEquals(t("+"), q("minus", back(DOWN)))
        assertEquals(t("3"), q("3", back(LEFT)))
        assertEquals(t("'"), q("comma", back(LEFT)))
        assertEquals(t("3"), q("3", back(RIGHT)))
        assertEquals(t("\""), q("comma", back(RIGHT)))
        assertEquals(j('ㅃ'), d("1", back(DOWN)))                // like the exit (FSS#124)
        assertEquals(t("~"), q("1", back(LEFT)))
    }

    // --- §5.3 symbol assignment table: outputs for tap / up / left / right / down ---

    private fun assertRow(layout: TapLayout, id: String, vararg expected: TapOutput) {
        val actual = listOf(tap, exit(UP), exit(LEFT), exit(RIGHT), exit(DOWN)).map { resolve(layout, id, it) }
        assertEquals("${layout.name} $id", expected.toList(), actual)
    }

    /** A symbol key gives the same outputs in both layouts. */
    private fun assertSymbolRow(id: String, vararg expected: String) {
        for (layout in listOf(TapLayouts.QWERTY, TapLayouts.DUBEOLSIK)) {
            assertRow(layout, id, *expected.map(::t).toTypedArray())
        }
    }

    @Test fun `TA-01 the key right of m, tap = and left -`() = assertSymbolRow("minus", "=", "_", "-", "=", "+")   // FSS#62

    @Test fun `TA-02 number 1`() {
        assertRow(TapLayouts.QWERTY, "1", t("1"), t("!"), t("~"), t("`"), t("1"))       // ~ ← since FSS#124 (↓ until then)
        assertRow(TapLayouts.DUBEOLSIK, "1", t("1"), t("!"), t("~"), t("`"), j('ㅃ'))   // ↓ ㅃ (FSS#124)
    }

    @Test fun `TA-03 numbers 2 to 9`() {
        "2345678".zip("@#\$%^&*").plus('9' to '(').forEach { (n, up) ->
            assertRow(TapLayouts.QWERTY, "$n", t("$n"), t("$up"), t("$n"), t("$n"), t("$n"))
            val down = "ㅉㄸㄲㅆ".getOrNull(n - '2')?.let(::j) ?: t("$n")   // 2..5 ↓ in Dubeolsik (FSS#124)
            assertRow(TapLayouts.DUBEOLSIK, "$n", t("$n"), t("$up"), t("$n"), t("$n"), down)
        }
    }

    @Test fun `TA-04 number 0`() = assertSymbolRow("0", "0", ")", "0", "0", "0")

    @Test fun `TA-05 i and ㅑ`() {
        assertRow(TapLayouts.QWERTY, "i", t("i"), t("I"), t("["), t("]"), t("i"))
        assertRow(TapLayouts.DUBEOLSIK, "i", j('ㅑ'), j('ㅑ'), t("["), t("]"), j('ㅑ'))
    }

    @Test fun `TA-06 o and ㅐ`() {
        assertRow(TapLayouts.QWERTY, "o", t("o"), t("O"), t("{"), t("}"), t("o"))
        assertRow(TapLayouts.DUBEOLSIK, "o", j('ㅐ'), j('ㅒ'), t("{"), t("}"), j('ㅐ'))
    }

    @Test fun `TA-07 p and ㅔ`() {
        assertRow(TapLayouts.QWERTY, "p", t("p"), t("P"), t("\\"), t("p"), t("|"))
        assertRow(TapLayouts.DUBEOLSIK, "p", j('ㅔ'), j('ㅖ'), t("\\"), j('ㅔ'), t("|"))
    }

    @Test fun `TA-08 l and ㅣ`() {
        assertRow(TapLayouts.QWERTY, "l", t("l"), t("L"), t(";"), t("l"), t(":"))
        assertRow(TapLayouts.DUBEOLSIK, "l", j('ㅣ'), j('ㅣ'), t(";"), j('ㅣ'), t(":"))
    }

    @Test fun `TA-09 the quote key is gone (#198)`() {
        for (layout in listOf(TapLayouts.QWERTY, TapLayouts.DUBEOLSIK)) {
            assertTrue(layout.name, layout.keys.none { it.id == "quote" })
        }
    }

    @Test fun `TA-10 comma`() = assertSymbolRow("comma", ",", "<", "'", "\"", ",")

    @Test fun `TA-11 period`() = assertSymbolRow("period", ".", ">", ".", ".", ".")

    @Test fun `TA-13 slash key (#198)`() = assertSymbolRow("slash", "/", "?", "/", "/", "/")

    @Test fun `TA-12 other letter keys q ㅂ and s ㄴ`() {
        assertRow(TapLayouts.QWERTY, "q", t("q"), t("Q"), t("q"), t("q"), t("q"))
        assertRow(TapLayouts.DUBEOLSIK, "q", j('ㅂ'), cv('ㅂ', 'ㅗ'), cv('ㅂ', 'ㅓ'), cv('ㅂ', 'ㅏ'), cv('ㅂ', 'ㅜ'))   // 보 버 바 부 (FSS#124)
        assertRow(TapLayouts.QWERTY, "s", t("s"), t("S"), t("s"), t("s"), t("s"))
        assertRow(TapLayouts.DUBEOLSIK, "s", j('ㄴ'), cv('ㄴ', 'ㅗ'), cv('ㄴ', 'ㅓ'), cv('ㄴ', 'ㅏ'), cv('ㄴ', 'ㅜ'))   // 노 너 나 누
    }

    @Test
    fun `TA-20 edge hints - every stroked character that differs from the tap, the capital and the shift jamo too (owner 198)`() {
        fun h(layout: TapLayout, id: String) = TapKeyResolver.edgeHints(layout.keys.single { it.id == id })
        assertEquals(mapOf(Dir.UP to "I", Dir.LEFT to "[", Dir.RIGHT to "]"), h(TapLayouts.QWERTY, "i"))
        assertEquals(mapOf(Dir.UP to "S"), h(TapLayouts.QWERTY, "s"))
        // Consonant keys: nothing drawn, a consonant + vowel is not shown (owner; FSS#124 took ㅃ ㅄ ㄵ ㄶ off).
        for (id in listOf("q", "s", "a", "f")) assertEquals(id, emptyMap<Dir, String>(), h(TapLayouts.DUBEOLSIK, id))
        // Number row: ~ on the left edge of 1 in both layouts, the tense consonants on Dubeolsik 1..5's bottom edge.
        assertEquals(mapOf(Dir.UP to "!", Dir.LEFT to "~", Dir.RIGHT to "`"), h(TapLayouts.QWERTY, "1"))
        assertEquals(mapOf(Dir.UP to "!", Dir.LEFT to "~", Dir.RIGHT to "`", Dir.DOWN to "ㅃ"), h(TapLayouts.DUBEOLSIK, "1"))
        assertEquals(mapOf(Dir.UP to "@", Dir.DOWN to "ㅉ"), h(TapLayouts.DUBEOLSIK, "2"))
        assertEquals(mapOf(Dir.UP to "@"), h(TapLayouts.QWERTY, "2"))
        assertEquals(mapOf(Dir.UP to "^"), h(TapLayouts.DUBEOLSIK, "6"))
        assertEquals(mapOf(Dir.LEFT to "[", Dir.RIGHT to "]"), h(TapLayouts.DUBEOLSIK, "i"))   // ㅑ: ↑ is ㅑ again
        for (layout in listOf(TapLayouts.QWERTY, TapLayouts.DUBEOLSIK)) {
            for (k in layout.keys.filter { it.kind == kbd.stroke.layout.TapKeyKind.FUNCTION }) assertEquals(k.id, emptyMap<Dir, String>(), TapKeyResolver.edgeHints(k))
        }
    }

    @Test
    fun `TA-21 Dubeolsik compound finals and the up tense consonants are off the consonant keys, QWERTY unchanged (FSS 124)`() {
        // The keys that carried them until FSS#124 (#242, FSS#97, #107, #109): every side a consonant + vowel now.
        for ((id, c) in listOf("r" to 'ㄱ', "s" to 'ㄴ', "f" to 'ㄹ', "q" to 'ㅂ', "w" to 'ㅈ', "e" to 'ㄷ', "t" to 'ㅅ', "x" to 'ㅌ', "v" to 'ㅍ')) {
            assertRow(TapLayouts.DUBEOLSIK, id, j(c), cv(c, 'ㅗ'), cv(c, 'ㅓ'), cv(c, 'ㅏ'), cv(c, 'ㅜ'))
        }
        // No consonant key has a direction jamo; no key at all has a compound final.
        assertTrue(TapLayouts.DUBEOLSIK.keys.filter { it.jamo != null && it.jamo !in VOWEL_KEYS }.all { it.dirJamo.isEmpty() })
        val placed = TapLayouts.DUBEOLSIK.keys.flatMap { it.dirJamo.values }
        assertTrue(placed.toString(), placed.none { it in "ㄳㄵㄶㄺㄻㄼㄽㄾㄿㅀㅄ" })
        // QWERTY keeps its letters on the same keys.
        for (id in "rsfqtxv".map { it.toString() }) assertRow(TapLayouts.QWERTY, id, t(id), t(id.uppercase()), t(id), t(id), t(id))
    }

    @Test
    fun `TA-22 Dubeolsik paired vowels - ㅗ up ㅛ, ㅓ left ㅕ, ㅏ right ㅑ, ㅜ down ㅠ, everything else unchanged (FSS 104)`() {
        // The whole table (SRS FR-TAP-02d): tap / ↑ / ← / → / ↓ of the four keys; the other directions type the vowel,
        // except the compound vowels of ㅗ and ㅜ since FSS#110 (ㅗ ↓ ㅚ → ㅘ, ㅜ ← ㅝ ↑ ㅟ, TA-25).
        assertRow(TapLayouts.DUBEOLSIK, "h", j('ㅗ'), j('ㅛ'), j('ㅗ'), j('ㅘ'), j('ㅚ'))
        assertRow(TapLayouts.DUBEOLSIK, "j", j('ㅓ'), j('ㅓ'), j('ㅕ'), j('ㅓ'), j('ㅓ'))
        assertRow(TapLayouts.DUBEOLSIK, "k", j('ㅏ'), j('ㅏ'), j('ㅏ'), j('ㅑ'), j('ㅏ'))
        assertRow(TapLayouts.DUBEOLSIK, "n", j('ㅜ'), j('ㅟ'), j('ㅝ'), j('ㅜ'), j('ㅠ'))
        // Exit-and-return is the exit; a left/right rub is the exit (ㅓ ← rub = ㅕ); an up/down rub is the plain vowel.
        for (id in listOf("h", "j", "k", "n")) {
            val key = TapLayouts.DUBEOLSIK.keys.single { it.id == id }
            for (dir in Dir.entries) assertEquals("$id $dir", TapKeyResolver.resolve(key, exit(dir)), TapKeyResolver.resolve(key, back(dir)))
            for (dir in listOf(LEFT, RIGHT)) assertEquals("$id rub $dir", TapKeyResolver.resolve(key, exit(dir)), TapKeyResolver.resolve(key, rub(dir)))
            for (dir in listOf(UP, DOWN)) assertEquals("$id rub $dir", j(key.jamo!!), TapKeyResolver.resolve(key, rub(dir)))
        }
        assertEquals(j('ㅕ'), d("j", rub(LEFT)))
        assertEquals(j('ㅑ'), d("k", rub(RIGHT)))
        assertEquals(j('ㅗ'), d("h", rub(UP)))
        assertEquals(j('ㅜ'), d("n", rub(DOWN)))
        // The ㅛ ㅕ ㅑ ㅠ keys stay, ㅑ with its brackets.
        assertRow(TapLayouts.DUBEOLSIK, "y", j('ㅛ'), j('ㅛ'), j('ㅛ'), j('ㅛ'), j('ㅛ'))
        assertRow(TapLayouts.DUBEOLSIK, "u", j('ㅕ'), j('ㅕ'), j('ㅕ'), j('ㅕ'), j('ㅕ'))
        assertRow(TapLayouts.DUBEOLSIK, "i", j('ㅑ'), j('ㅑ'), t("["), t("]"), j('ㅑ'))
        assertRow(TapLayouts.DUBEOLSIK, "b", j('ㅠ'), j('ㅠ'), j('ㅠ'), j('ㅠ'), j('ㅠ'))
        // Only the four keys carry paired vowels, one each.
        val vowels = TapLayouts.DUBEOLSIK.keys.filter { it.jamo in PAIR_KEYS }
            .associate { k -> k.jamo!! to k.dirJamo.filterValues { it in "ㅛㅕㅑㅠ" } }
        assertEquals(mapOf('ㅗ' to mapOf(UP to 'ㅛ'), 'ㅓ' to mapOf(LEFT to 'ㅕ'), 'ㅏ' to mapOf(RIGHT to 'ㅑ'), 'ㅜ' to mapOf(DOWN to 'ㅠ')), vowels)
        // QWERTY h j k n unchanged.
        for (id in listOf("h", "j", "k", "n")) assertRow(TapLayouts.QWERTY, id, t(id), t(id.uppercase()), t(id), t(id), t(id))
        // Edge hints: the pair on its own edge, nothing on the others (owner #198).
        fun h(id: String) = TapKeyResolver.edgeHints(TapLayouts.DUBEOLSIK.keys.single { it.id == id })
        assertEquals(mapOf(UP to "ㅛ", RIGHT to "ㅘ", DOWN to "ㅚ", Diag.DOWN_RIGHT to "ㅙ"), h("h"))   // + compound vowels (FSS#110)
        assertEquals(mapOf(LEFT to "ㅕ", Diag.DOWN_LEFT to "ㅔ", Diag.UP_LEFT to "ㅖ"), h("j"))     // + diagonals (FSS#126)
        assertEquals(mapOf(RIGHT to "ㅑ", Diag.DOWN_RIGHT to "ㅐ", Diag.UP_RIGHT to "ㅒ"), h("k"))
        assertEquals(mapOf(UP to "ㅟ", LEFT to "ㅝ", DOWN to "ㅠ", Diag.DOWN_LEFT to "ㅞ"), h("n"))
        assertEquals(mapOf(LEFT to "[", RIGHT to "]"), h("i"))
        for (id in listOf("y", "u", "b")) assertEquals(id, emptyMap<Dir, String>(), h(id))
    }

    @Test
    fun `TA-23 ㄹ has no diagonals any more - a diagonal passed for it, or for any 4-way key, is its tap (FSS 109, 124)`() {
        val f = TapLayouts.DUBEOLSIK.keys.single { it.id == "f" }
        assertFalse(f.eightWay)   // 4-way since FSS#124
        for (dg in Diag.entries) {
            assertEquals(j('ㄹ'), d("f", exit(dg)))
            assertEquals(j('ㄹ'), d("f", back(dg)))
            assertEquals(t("f"), q("f", exit(dg)))
            assertEquals(t("f"), q("f", rub(dg)))
            assertEquals(t("3"), q("3", exit(dg)))
            assertEquals(t("3"), d("3", exit(dg)))
            assertEquals(j('ㄱ'), d("r", exit(dg)))
        }
        // ㄹ's rubs follow the other consonant keys: left and right the exit, up and down the plain ㄹ.
        assertEquals(cv('ㄹ', 'ㅓ'), d("f", rub(LEFT)))
        assertEquals(cv('ㄹ', 'ㅏ'), d("f", rub(RIGHT)))
        assertEquals(j('ㄹ'), d("f", rub(UP)))
        assertEquals(j('ㄹ'), d("f", rub(DOWN)))
    }

    @Test
    fun `TA-24 a stroke in a direction with nothing types what the tap types, with no error vibration (FSS 109)`() {
        // SRS FR-TAP-02c ④: every letter key of both layouts, every one of the eight directions without an assignment.
        val letterKinds = setOf(kbd.stroke.layout.TapKeyKind.DUBEOLSIK_LETTER, kbd.stroke.layout.TapKeyKind.QWERTY_LETTER)
        var checked = 0
        for (layout in listOf(TapLayouts.QWERTY, TapLayouts.DUBEOLSIK)) {
            for (key in layout.keys.filter { it.kind in letterKinds }) {
                val tapOut = TapKeyResolver.resolve(key, tap)
                assertFalse(tapOut.errorVibe)
                // Assigned from the data, not from the resolver: the ↑ capital or shift jamo, direction jamo, symbols.
                val a = key.assign
                val assigned = buildSet<Way> {
                    if (key.kind == kbd.stroke.layout.TapKeyKind.QWERTY_LETTER || key.shiftJamo != null) add(UP)
                    addAll(key.dirJamo.keys)
                    addAll(key.dirVowel.keys)   // a consonant + vowel since FSS#122 (TA-26)
                    if (a?.up != null) add(UP)
                    if (a?.left != null) add(LEFT)
                    if (a?.right != null) add(RIGHT)
                    if (a?.down != null) add(DOWN)
                }
                for (w in WAYS.filter { it !in assigned }) {
                    for (g in listOf(exit(w), back(w))) {
                        assertEquals("${layout.name} ${key.id} $g", tapOut, TapKeyResolver.resolve(key, g))
                        checked++
                    }
                }
            }
        }
        assertTrue(checked > 200)
        // The case the owner named: ㄹ ↘ (4-way since FSS#124, so only a caller could pass it). Up/down/left/right of
        // every consonant key are a consonant + vowel since FSS#124 (TA-26).
        assertEquals(j('ㄹ'), d("f", exit(Diag.DOWN_RIGHT)))
    }

    @Test
    fun `TA-26 Dubeolsik consonant keys - up, down, left and right are the consonant then ㅗ ㅜ ㅓ ㅏ on all 14 keys (FSS 122, 124)`() {
        // SRS FR-TAP-02h table, written out by hand from the issue: all four ways of every consonant key (FSS#124;
        // FSS#122 had only the ways without a tense consonant or compound final).
        val vowel = mapOf(LEFT to 'ㅓ', RIGHT to 'ㅏ', UP to 'ㅗ', DOWN to 'ㅜ')
        val consonants = "ㅂㅈㄷㄱㅅㅁㄴㅇㄹㅎㅋㅌㅊㅍ"
        assertEquals(14, consonants.length)
        for (c in consonants) {
            val key = TapLayouts.DUBEOLSIK.keys.single { it.jamo == c }
            assertEquals("$c", vowel, key.dirVowel)
            for (w in listOf(UP, DOWN, LEFT, RIGHT)) {
                val out = TapKeyResolver.resolve(key, exit(w))
                assertEquals("$c $w", cv(c, vowel.getValue(w)), out)
                assertEquals("$c back $w", out, TapKeyResolver.resolve(key, back(w)))   // exit-and-return = exit
            }
            // Rubs: left/right the exit, up/down the plain consonant (as the paired vowels).
            assertEquals(TapKeyResolver.resolve(key, exit(LEFT)), TapKeyResolver.resolve(key, rub(LEFT)))
            assertEquals(TapKeyResolver.resolve(key, exit(RIGHT)), TapKeyResolver.resolve(key, rub(RIGHT)))
            assertEquals(j(c), TapKeyResolver.resolve(key, rub(UP)))
            assertEquals(j(c), TapKeyResolver.resolve(key, rub(DOWN)))
            // 4-way keys: a diagonal stays the tap; nothing is drawn on the key (owner).
            assertFalse("$c", key.eightWay)
            for (dg in Diag.entries) assertEquals("$c $dg", j(c), TapKeyResolver.resolve(key, exit(dg)))
            assertEquals("$c hints", emptyMap<Way, String>(), TapKeyResolver.edgeHints(key))
        }
        assertEquals(cv('ㄱ', 'ㅓ'), d("r", exit(LEFT)))   // 거
        assertEquals(cv('ㅁ', 'ㅗ'), d("a", exit(UP)))     // 모
        assertEquals(cv('ㅇ', 'ㅏ'), d("d", exit(RIGHT)))  // 아 (the owner's example)
        assertEquals(cv('ㄱ', 'ㅗ'), d("r", exit(UP)))     // 고, no longer ㄲ
        // Vowel keys, number and symbol keys, and QWERTY: no vowel after anything.
        for (layout in listOf(TapLayouts.QWERTY, TapLayouts.DUBEOLSIK)) for (key in layout.keys) {
            if (key.kind == kbd.stroke.layout.TapKeyKind.FUNCTION || (key.jamo != null && key.jamo in 'ㄱ'..'ㅎ')) continue
            assertEquals(key.id, emptyMap<Dir, Char>(), key.dirVowel)
            for (w in WAYS) assertEquals("${key.id} $w", null, TapKeyResolver.resolve(key, exit(w)).vowel)
        }
    }

    @Test
    fun `TA-27 number row - Dubeolsik 1 to 5 down type ㅃ ㅉ ㄸ ㄲ ㅆ as one jamo, 1 left is tilde in both layouts, Ctrl and Alt keep QWERTY (FSS 124)`() {
        // SRS FR-TAP-02i, by hand from the issue: the tense form of the consonant key right below.
        val tense = mapOf("1" to 'ㅃ', "2" to 'ㅉ', "3" to 'ㄸ', "4" to 'ㄲ', "5" to 'ㅆ')
        for (n in "1234567890".map { it.toString() }) {
            val want = tense[n]?.let(::j) ?: t(n)
            assertEquals(n, want, d(n, exit(DOWN)))
            assertEquals(n, want, d(n, back(DOWN)))
            assertEquals(n, t(n), d(n, rub(DOWN)))       // an up/down rub is the tap, as on every number key
            assertEquals(n, t(n), q(n, exit(DOWN)))      // QWERTY's number row ↓ is empty
            assertFalse(d(n, exit(DOWN)).errorVibe)
            assertEquals(n, null, d(n, exit(DOWN)).vowel)
        }
        assertEquals(t("~"), q("1", exit(LEFT)))
        assertEquals(t("~"), d("1", exit(LEFT)))
        assertEquals(t("~"), d("1", rub(LEFT)))
        // Ctrl·Alt send the QWERTY key of the place (FR-TAP-16 ③): 2 ↓ is D2, 1 ← is Shift + `.
        assertEquals(ChordStroke(ChordKey.D2, shift = false), ChordKeys.ofTap("2", exit(DOWN)))
        assertEquals(ChordStroke(ChordKey.D1, shift = false), ChordKeys.ofTap("1", exit(DOWN)))
        assertEquals(ChordStroke(ChordKey.GRAVE, shift = true), ChordKeys.ofTap("1", exit(LEFT)))
    }

    @Test
    fun `TA-25 Dubeolsik compound vowels - ㅗ → ㅘ ↓ ㅚ ↘ ㅙ, ㅜ ← ㅝ ↑ ㅟ ↙ ㅞ, ㅡ ↓ ㅢ, every other way the vowel (FSS 110)`() {
        // The whole table (SRS FR-TAP-02g), all eight directions: exit, exit-and-return and rub.
        val table = mapOf(
            "h" to mapOf<Way, Char>(UP to 'ㅛ', RIGHT to 'ㅘ', DOWN to 'ㅚ', Diag.DOWN_RIGHT to 'ㅙ'),
            "n" to mapOf<Way, Char>(UP to 'ㅟ', LEFT to 'ㅝ', DOWN to 'ㅠ', Diag.DOWN_LEFT to 'ㅞ'),
            "m" to mapOf<Way, Char>(DOWN to 'ㅢ'),
        )
        for ((id, ways) in table) {
            val base = TapLayouts.DUBEOLSIK.keys.single { it.id == id }.jamo!!
            assertEquals(j(base), d(id, tap))
            for (w in WAYS) {
                val want = j(ways[w] ?: base)                                // an empty way is the tap (FR-TAP-02g ③)
                assertEquals("$id $w", want, d(id, exit(w)))
                assertEquals("$id back $w", want, d(id, back(w)))
                assertFalse(d(id, exit(w)).errorVibe)
                // Rubs as on every key (FR-TAP-02g ④): left/right and diagonals the exit, up/down the plain vowel.
                val rubWant = if (w == UP || w == DOWN) j(base) else want
                assertEquals("$id rub $w", rubWant, d(id, rub(w)))
            }
        }
        assertEquals(j('ㅘ'), d("h", rub(RIGHT)))
        assertEquals(j('ㅝ'), d("n", rub(LEFT)))
        assertEquals(j('ㅙ'), d("h", rub(Diag.DOWN_RIGHT)))
        assertEquals(j('ㅞ'), d("n", rub(Diag.DOWN_LEFT)))
        assertEquals(j('ㅡ'), d("m", rub(DOWN)))
        // Edge hints: the middle of the edges and the corners (FR-TAP-02g ⑧); ㅡ only its bottom edge.
        fun h(id: String) = TapKeyResolver.edgeHints(TapLayouts.DUBEOLSIK.keys.single { it.id == id })
        assertEquals(mapOf(UP to "ㅛ", RIGHT to "ㅘ", DOWN to "ㅚ", Diag.DOWN_RIGHT to "ㅙ"), h("h"))
        assertEquals(mapOf(UP to "ㅟ", LEFT to "ㅝ", DOWN to "ㅠ", Diag.DOWN_LEFT to "ㅞ"), h("n"))
        assertEquals(mapOf(DOWN to "ㅢ"), h("m"))
        // QWERTY h n m unchanged.
        for (id in listOf("h", "n", "m")) {
            assertRow(TapLayouts.QWERTY, id, t(id), t(id.uppercase()), t(id), t(id), t(id))
            for (dg in Diag.entries) assertEquals(t(id), q(id, exit(dg)))
        }
    }

    @Test
    fun `TA-28 Dubeolsik ㅓ ㅏ diagonals - ㅓ ↙ ㅔ ↖ ㅖ, ㅏ ↘ ㅐ ↗ ㅒ, the other diagonals the vowel, Ctrl and Alt QWERTY j k (FSS 126)`() {
        // The whole of both keys (SRS FR-TAP-02j), all eight directions: exit, exit-and-return and rub.
        val table = mapOf(
            "j" to mapOf<Way, Char>(LEFT to 'ㅕ', Diag.DOWN_LEFT to 'ㅔ', Diag.UP_LEFT to 'ㅖ'),
            "k" to mapOf<Way, Char>(RIGHT to 'ㅑ', Diag.DOWN_RIGHT to 'ㅐ', Diag.UP_RIGHT to 'ㅒ'),
        )
        for ((id, ways) in table) {
            val base = TapLayouts.DUBEOLSIK.keys.single { it.id == id }.jamo!!
            assertEquals(j(base), d(id, tap))
            for (w in WAYS) {
                val want = j(ways[w] ?: base)                                // an empty way is the tap (FR-TAP-02c ④)
                assertEquals("$id $w", want, d(id, exit(w)))
                assertEquals("$id back $w", want, d(id, back(w)))
                assertFalse(d(id, exit(w)).errorVibe)
                assertEquals("$id $w", null, d(id, exit(w)).vowel)            // one jamo, never a consonant + vowel
                val rubWant = if (w == UP || w == DOWN) j(base) else want     // rubs as on every key (FR-TAP-02e ④)
                assertEquals("$id rub $w", rubWant, d(id, rub(w)))
            }
        }
        // The same output as the ㅔ ㅐ keys: tap and ↑.
        assertEquals(d("p", tap), d("j", exit(Diag.DOWN_LEFT)))
        assertEquals(d("p", exit(UP)), d("j", exit(Diag.UP_LEFT)))
        assertEquals(d("o", tap), d("k", exit(Diag.DOWN_RIGHT)))
        assertEquals(d("o", exit(UP)), d("k", exit(Diag.UP_RIGHT)))
        // Ctrl·Alt (FR-TAP-16 ③): the QWERTY key of the place, j and k have no diagonal, so J and K without Shift.
        for (dg in Diag.entries) {
            assertEquals(ChordStroke(ChordKey.J, shift = false), ChordKeys.ofTap("j", exit(dg)))
            assertEquals(ChordStroke(ChordKey.K, shift = false), ChordKeys.ofTap("k", exit(dg)))
            assertEquals(t("j"), q("j", exit(dg)))
            assertEquals(t("k"), q("k", exit(dg)))
        }
    }

    private companion object {
        /** The four Dubeolsik vowel keys with a paired vowel (FSS#104). */
        val PAIR_KEYS = setOf('ㅗ', 'ㅓ', 'ㅏ', 'ㅜ')

        /** The Dubeolsik vowel keys with a direction jamo: the paired vowels and the compound vowels (FSS#110). */
        val VOWEL_KEYS = PAIR_KEYS + 'ㅡ'
    }
}
