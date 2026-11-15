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

    @Test fun `TK-05 up exit Dubeolsik is shift jamo, compound final or base`() {
        assertEquals(j('ㄲ'), d("r", exit(UP)))
        assertEquals(j('ㅒ'), d("o", exit(UP)))
        assertEquals(j('ㅁ'), d("a", exit(UP)))
        assertEquals(j('ㄵ'), d("s", exit(UP)))                  // a compound final (FSS#97)
        assertEquals(j('ㄺ'), d("f", exit(UP)))                  // ㄱ (r) is above ㄹ (FSS#109; ㅀ until then)
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

    @Test fun `TK-08 down exit Dubeolsik is compound final, down symbol or base jamo`() {
        assertEquals(j('ㄱ'), d("r", exit(DOWN)))                // nothing below ㄱ: the tap (FSS#109; ㄳ in FSS#97)
        assertEquals(j('ㄿ'), d("f", exit(DOWN)))                // ㅍ (v) is below ㄹ (FSS#109; ㄼ until then)
        assertEquals(j('ㅁ'), d("a", exit(DOWN)))
        assertEquals(j('ㅐ'), d("o", exit(DOWN)))   // o has no down symbol (#198)
        assertEquals(t(":"), d("l", exit(DOWN)))
    }

    @Test fun `TK-09 down exit number row and symbol is down symbol or tap`() {
        assertEquals(t("3"), q("3", exit(DOWN)))
        assertEquals(t("+"), q("minus", exit(DOWN)))
        assertEquals(t("."), q("period", exit(DOWN)))
    }

    @Test fun `TK-10 left and right exit QWERTY`() {
        assertEquals(t("a"), q("a", exit(LEFT)))
        assertEquals(t("a"), q("a", exit(RIGHT)))
        assertEquals(t("["), q("i", exit(LEFT)))
        assertEquals(t("]"), q("i", exit(RIGHT)))
    }

    @Test fun `TK-11 left and right exit Dubeolsik`() {
        assertEquals(j('ㄱ'), d("r", exit(LEFT)))
        assertEquals(j('ㄳ'), d("r", exit(RIGHT)))               // ㅅ (t) is right of ㄱ (FSS#109; ↓ in FSS#97)
        assertEquals(j('ㄻ'), d("f", exit(LEFT)))                // ㅁ (a) is left of ㄹ (FSS#109; ㄺ until then)
        assertEquals(j('ㅀ'), d("f", exit(RIGHT)))               // ㅎ (g) is right of ㄹ (FSS#109; ㄻ until then)
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
    }

    @Test fun `TK-13 up rub QWERTY is superscript`() = assertEquals(t("ᵃ"), q("a", rub(UP)))

    @Test fun `TK-14 up rub Dubeolsik is base jamo`() {
        assertEquals(j('ㄱ'), d("r", rub(UP)))
        for ((id, base) in listOf("s" to 'ㄴ', "f" to 'ㄹ', "v" to 'ㅍ')) assertEquals(id, j(base), d(id, rub(UP)))   // no compound final
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
        // No compound final on a rub, though ↓ exit types one on each of these (FSS#97).
        for ((id, base) in listOf("r" to 'ㄱ', "s" to 'ㄴ', "f" to 'ㄹ', "q" to 'ㅂ', "t" to 'ㅅ')) assertEquals(id, j(base), d(id, rub(DOWN)))
    }

    @Test fun `TK-18 down rub number row and symbol is the tap symbol`() {
        assertEquals(t("3"), q("3", rub(DOWN)))
        assertEquals(t(","), q("comma", rub(DOWN)))
    }

    @Test fun `TK-19 left and right rub QWERTY resolves like an exit`() {
        assertEquals(t("a"), q("a", rub(LEFT)))
        assertEquals(t("a"), q("a", rub(RIGHT)))
        assertEquals(t("["), q("i", rub(LEFT)))
        assertEquals(t("]"), q("i", rub(RIGHT)))
    }

    @Test fun `TK-20 left and right rub Dubeolsik resolves like an exit`() {
        assertEquals(j('ㄱ'), d("r", rub(LEFT)))
        assertEquals(j('ㄳ'), d("r", rub(RIGHT)))                // like the exit (FSS#109)
        assertEquals(j('ㄻ'), d("f", rub(LEFT)))                 // like the exit (FSS#109)
        assertEquals(j('ㅀ'), d("f", rub(RIGHT)))
        assertEquals(t("{"), d("o", rub(LEFT)))
        assertEquals(t("}"), d("o", rub(RIGHT)))
    }

    @Test fun `TK-21 left and right rub number row and symbol resolves like an exit`() {
        assertEquals(t("3"), q("3", rub(LEFT)))
        assertEquals(t("3"), q("3", rub(RIGHT)))
        assertEquals(t("'"), q("comma", rub(LEFT)))
        assertEquals(t("\""), q("comma", rub(RIGHT)))
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
        assertEquals(j('ㄲ'), d("r", back(UP)))
        assertEquals(j('ㅒ'), d("o", back(UP)))
        assertEquals(j('ㄵ'), d("s", back(UP)))                  // like the exit (FSS#97)
        assertEquals(j('ㄱ'), d("r", back(DOWN)))                // like the exit: nothing below ㄱ (FSS#109)
        assertEquals(t(":"), d("l", back(DOWN)))
        assertEquals(j('ㄱ'), d("r", back(LEFT)))
        assertEquals(j('ㄻ'), d("f", back(LEFT)))                // like the exit (FSS#109)
        assertEquals(t("{"), d("o", back(LEFT)))
        assertEquals(j('ㄳ'), d("r", back(RIGHT)))               // like the exit (FSS#109)
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

    @Test fun `TA-02 number 1`() = assertSymbolRow("1", "1", "!", "1", "`", "~")

    @Test fun `TA-03 numbers 2 to 9`() {
        "2345678".zip("@#\$%^&*").plus('9' to '(').forEach { (n, up) ->
            assertSymbolRow("$n", "$n", "$up", "$n", "$n", "$n")
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
        assertRow(TapLayouts.DUBEOLSIK, "q", j('ㅂ'), j('ㅃ'), j('ㅂ'), j('ㅄ'), j('ㅂ'))   // → ㅄ (FSS#109; #242 ←, #246 →, FSS#97 ↓)
        assertRow(TapLayouts.QWERTY, "s", t("s"), t("S"), t("s"), t("s"), t("s"))
        assertRow(TapLayouts.DUBEOLSIK, "s", j('ㄴ'), j('ㄵ'), j('ㄴ'), j('ㄶ'), j('ㄴ'))   // ↑ ㄵ → ㄶ (FSS#109; FSS#97 ↓ ㄶ)
    }

    @Test
    fun `TA-20 edge hints - every stroked character that differs from the tap, the capital and the shift jamo too (owner 198)`() {
        fun h(layout: TapLayout, id: String) = TapKeyResolver.edgeHints(layout.keys.single { it.id == id })
        assertEquals(mapOf(Dir.UP to "I", Dir.LEFT to "[", Dir.RIGHT to "]"), h(TapLayouts.QWERTY, "i"))
        assertEquals(mapOf(Dir.UP to "S"), h(TapLayouts.QWERTY, "s"))
        assertEquals(mapOf(Dir.UP to "ㅃ", Dir.RIGHT to "ㅄ"), h(TapLayouts.DUBEOLSIK, "q"))   // a shift jamo, a compound final
        assertEquals(mapOf(Dir.UP to "ㄵ", Dir.RIGHT to "ㄶ"), h(TapLayouts.DUBEOLSIK, "s"))   // ㄴ: compound finals on ↑ and → (FSS#109)
        assertEquals(emptyMap<Dir, String>(), h(TapLayouts.DUBEOLSIK, "a"))          // ㅁ: no shift jamo, nothing assigned
        assertEquals(mapOf(Dir.LEFT to "[", Dir.RIGHT to "]"), h(TapLayouts.DUBEOLSIK, "i"))   // ㅑ: ↑ is ㅑ again
        for (layout in listOf(TapLayouts.QWERTY, TapLayouts.DUBEOLSIK)) {
            for (k in layout.keys.filter { it.kind == kbd.stroke.layout.TapKeyKind.FUNCTION }) assertEquals(k.id, emptyMap<Dir, String>(), TapKeyResolver.edgeHints(k))
        }
    }

    @Test
    fun `TA-21 Dubeolsik compound finals - one stroke each on the first consonant's key, the shift jamo stays on up, QWERTY unchanged (issue 242, FSS 109)`() {
        // The whole table (SRS FR-TAP-02c, 0.91): tap / ↑ / ← / → / ↓ of the keys that had or have compound finals.
        assertRow(TapLayouts.DUBEOLSIK, "r", j('ㄱ'), j('ㄲ'), j('ㄱ'), j('ㄳ'), j('ㄱ'))   // ㄱ ↑ is still ㄲ; ↓ is the plain ㄱ again
        assertRow(TapLayouts.DUBEOLSIK, "s", j('ㄴ'), j('ㄵ'), j('ㄴ'), j('ㄶ'), j('ㄴ'))   // ↓ is the plain ㄴ again
        assertRow(TapLayouts.DUBEOLSIK, "f", j('ㄹ'), j('ㄺ'), j('ㄻ'), j('ㅀ'), j('ㄿ'))   // and the diagonals, TA-23
        assertRow(TapLayouts.DUBEOLSIK, "q", j('ㅂ'), j('ㅃ'), j('ㅂ'), j('ㅄ'), j('ㅂ'))   // ↓ is the plain ㅂ again
        assertRow(TapLayouts.DUBEOLSIK, "t", j('ㅅ'), j('ㅆ'), j('ㅅ'), j('ㅅ'), j('ㅅ'))   // no compound finals any more (FSS#109)
        assertRow(TapLayouts.DUBEOLSIK, "x", j('ㅌ'), j('ㅌ'), j('ㅌ'), j('ㅌ'), j('ㅌ'))
        assertRow(TapLayouts.DUBEOLSIK, "v", j('ㅍ'), j('ㅍ'), j('ㅍ'), j('ㅍ'), j('ㅍ'))
        // Each of the 11 compound finals is on exactly one key and one direction, on the four keys r s f q.
        val placed = TapLayouts.DUBEOLSIK.keys.filter { it.jamo !in PAIR_KEYS }.flatMap { it.dirJamo.values }
        assertEquals("ㄳㄵㄶㄺㄻㄼㄽㄾㄿㅀㅄ".toList(), placed.sorted())
        val carriers = TapLayouts.DUBEOLSIK.keys.filter { it.dirJamo.isNotEmpty() && it.jamo !in PAIR_KEYS }
        assertEquals(setOf("r", "s", "f", "q"), carriers.map { it.id }.toSet())
        // Exit-and-return is the exit; an up/down rub is the plain jamo (SRS tap action table rows 5, 6, 8).
        for (key in carriers) {
            for (dir in WAYS) assertEquals("${key.id} $dir", TapKeyResolver.resolve(key, exit(dir)), TapKeyResolver.resolve(key, back(dir)))
            for (dir in listOf(UP, DOWN)) assertEquals("${key.id} rub $dir", j(key.jamo!!), TapKeyResolver.resolve(key, rub(dir)))
        }
        // QWERTY keeps its letters on the same keys.
        for (id in "rsfqtxv".map { it.toString() }) assertRow(TapLayouts.QWERTY, id, t(id), t(id.uppercase()), t(id), t(id), t(id))
        // Edge hints show them on the new edges only (owner #198), ㄹ's diagonals in its corners (FR-TAP-02e ⑥).
        fun h(id: String) = TapKeyResolver.edgeHints(TapLayouts.DUBEOLSIK.keys.single { it.id == id })
        assertEquals(mapOf(UP to "ㄲ", RIGHT to "ㄳ"), h("r"))
        assertEquals(mapOf(UP to "ㄵ", RIGHT to "ㄶ"), h("s"))
        assertEquals(
            mapOf(UP to "ㄺ", LEFT to "ㄻ", RIGHT to "ㅀ", DOWN to "ㄿ", Diag.UP_LEFT to "ㄼ", Diag.UP_RIGHT to "ㄽ", Diag.DOWN_LEFT to "ㄾ"),
            h("f"),
        )
        assertEquals(mapOf(UP to "ㅃ", RIGHT to "ㅄ"), h("q"))
        assertEquals(mapOf(UP to "ㅆ"), h("t"))
        assertEquals(emptyMap<Way, String>(), h("x"))                                     // ㅌ ↑ ㄾ went (FSS#109)
        assertEquals(emptyMap<Way, String>(), h("v"))
    }

    @Test
    fun `TA-22 Dubeolsik paired vowels - ㅗ up ㅛ, ㅓ left ㅕ, ㅏ right ㅑ, ㅜ down ㅠ, everything else unchanged (FSS 104)`() {
        // The whole table (SRS FR-TAP-02d): tap / ↑ / ← / → / ↓ of the four keys; the other directions type the vowel.
        assertRow(TapLayouts.DUBEOLSIK, "h", j('ㅗ'), j('ㅛ'), j('ㅗ'), j('ㅗ'), j('ㅗ'))
        assertRow(TapLayouts.DUBEOLSIK, "j", j('ㅓ'), j('ㅓ'), j('ㅕ'), j('ㅓ'), j('ㅓ'))
        assertRow(TapLayouts.DUBEOLSIK, "k", j('ㅏ'), j('ㅏ'), j('ㅏ'), j('ㅑ'), j('ㅏ'))
        assertRow(TapLayouts.DUBEOLSIK, "n", j('ㅜ'), j('ㅜ'), j('ㅜ'), j('ㅜ'), j('ㅠ'))
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
        val vowels = TapLayouts.DUBEOLSIK.keys.filter { it.jamo in PAIR_KEYS }.associate { it.jamo!! to it.dirJamo }
        assertEquals(mapOf('ㅗ' to mapOf(UP to 'ㅛ'), 'ㅓ' to mapOf(LEFT to 'ㅕ'), 'ㅏ' to mapOf(RIGHT to 'ㅑ'), 'ㅜ' to mapOf(DOWN to 'ㅠ')), vowels)
        // QWERTY h j k n unchanged.
        for (id in listOf("h", "j", "k", "n")) assertRow(TapLayouts.QWERTY, id, t(id), t(id.uppercase()), t(id), t(id), t(id))
        // Edge hints: the pair on its own edge, nothing on the others (owner #198).
        fun h(id: String) = TapKeyResolver.edgeHints(TapLayouts.DUBEOLSIK.keys.single { it.id == id })
        assertEquals(mapOf(UP to "ㅛ"), h("h"))
        assertEquals(mapOf(LEFT to "ㅕ"), h("j"))
        assertEquals(mapOf(RIGHT to "ㅑ"), h("k"))
        assertEquals(mapOf(DOWN to "ㅠ"), h("n"))
        assertEquals(mapOf(LEFT to "[", RIGHT to "]"), h("i"))
        for (id in listOf("y", "u", "b")) assertEquals(id, emptyMap<Dir, String>(), h(id))
    }

    @Test
    fun `TA-23 ㄹ diagonals - exit, exit-and-return and rub give the diagonal's jamo, down-right is the tap (FSS 109)`() {
        assertEquals(j('ㄼ'), d("f", exit(Diag.UP_LEFT)))        // ㅂ (q) is up-left of ㄹ
        assertEquals(j('ㄽ'), d("f", exit(Diag.UP_RIGHT)))       // ㅅ (t) up-right
        assertEquals(j('ㄾ'), d("f", exit(Diag.DOWN_LEFT)))      // ㅌ (x) down-left
        assertEquals(j('ㄹ'), d("f", exit(Diag.DOWN_RIGHT)))     // ㅊ (c) makes no compound final: the tap
        for (dg in Diag.entries) {
            assertEquals("back $dg", d("f", exit(dg)), d("f", back(dg)))
            assertEquals("rub $dg", d("f", exit(dg)), d("f", rub(dg)))   // the first exit's direction, as on every key
        }
        // ㄹ's rubs follow the other keys (SRS FR-TAP-02e ④): left and right the exit, up and down the plain ㄹ.
        assertEquals(j('ㄻ'), d("f", rub(LEFT)))
        assertEquals(j('ㅀ'), d("f", rub(RIGHT)))
        assertEquals(j('ㄹ'), d("f", rub(UP)))
        assertEquals(j('ㄹ'), d("f", rub(DOWN)))
        // A diagonal passed for a key without diagonals is its tap: QWERTY f (Ctrl·Alt, FR-TAP-02e ⑧), others.
        for (dg in Diag.entries) {
            assertEquals(t("f"), q("f", exit(dg)))
            assertEquals(t("f"), q("f", rub(dg)))
            assertEquals(t("3"), q("3", exit(dg)))
            assertEquals(j('ㄱ'), d("r", exit(dg)))
        }
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
        assertTrue(checked > 300)
        // The cases the owner named: ㄹ ↘, ㄱ ↓, ㄴ ↓, ㅂ ↓, ㅅ ↓, ㅌ ↑, ㅍ ↑.
        assertEquals(j('ㄹ'), d("f", exit(Diag.DOWN_RIGHT)))
        for (id in listOf("r", "s", "q", "t")) assertEquals(id, d(id, tap), d(id, exit(DOWN)))
        for (id in listOf("x", "v")) assertEquals(id, d(id, tap), d(id, exit(UP)))
    }

    private companion object {
        /** The four Dubeolsik vowel keys with a paired vowel (FSS#104). */
        val PAIR_KEYS = setOf('ㅗ', 'ㅓ', 'ㅏ', 'ㅜ')
    }
}
