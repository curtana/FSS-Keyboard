package kbd.stroke.tap

import kbd.stroke.geom.Dir
import kbd.stroke.geom.Dir.DOWN
import kbd.stroke.geom.Dir.LEFT
import kbd.stroke.geom.Dir.RIGHT
import kbd.stroke.geom.Dir.UP
import kbd.stroke.layout.TapLayout
import kbd.stroke.layout.TapLayouts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * LLD-11 §5.2 TK-01..TK-24 (one per cell of the SRS tap action table) and §5.3 TA-01..TA-12
 * (symbol assignment table), with keys taken from [TapLayouts]. Key ids are QWERTY positions:
 * r = ㄱ, a = ㅁ, o = ㅐ, i = ㅑ, p = ㅔ, l = ㅣ, q = ㅂ, s = ㄴ.
 */
class TapKeyResolverTest {

    private val tap = TapGesture.Tap
    private fun exit(d: Dir) = TapGesture.Exit(d)
    private fun rub(d: Dir) = TapGesture.Rub(d)
    private fun back(d: Dir) = TapGesture.ExitReturn(d)

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

    @Test fun `TK-05 up exit Dubeolsik is shift jamo or base`() {
        assertEquals(j('ㄲ'), d("r", exit(UP)))
        assertEquals(j('ㅒ'), d("o", exit(UP)))
        assertEquals(j('ㅁ'), d("a", exit(UP)))
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

    @Test fun `TK-08 down exit Dubeolsik is down symbol or base jamo`() {
        assertEquals(j('ㄱ'), d("r", exit(DOWN)))
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
        assertEquals(j('ㄳ'), d("r", exit(LEFT)))                // a compound final (issue #242)
        assertEquals(j('ㄱ'), d("r", exit(RIGHT)))
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

    @Test fun `TK-14 up rub Dubeolsik is base jamo`() = assertEquals(j('ㄱ'), d("r", rub(UP)))

    @Test fun `TK-15 up rub number row and symbol is the tap symbol`() {
        assertEquals(t("3"), q("3", rub(UP)))
        assertEquals(t(","), q("comma", rub(UP)))
    }

    @Test fun `TK-16 down rub QWERTY is subscript or letter with error vibration`() {
        assertEquals(t("ₐ"), q("a", rub(DOWN)))
        assertEquals(TapOutput("b", null, errorVibe = true), q("b", rub(DOWN)))
    }

    @Test fun `TK-17 down rub Dubeolsik is base jamo`() = assertEquals(j('ㄱ'), d("r", rub(DOWN)))

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
        assertEquals(j('ㄳ'), d("r", rub(LEFT)))                 // like the exit (issue #242)
        assertEquals(j('ㄱ'), d("r", rub(RIGHT)))
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
        assertEquals(j('ㄱ'), d("r", back(DOWN)))
        assertEquals(t(":"), d("l", back(DOWN)))
        assertEquals(j('ㄳ'), d("r", back(LEFT)))                // like the exit (issue #242)
        assertEquals(t("{"), d("o", back(LEFT)))
        assertEquals(j('ㄱ'), d("r", back(RIGHT)))
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
        assertRow(TapLayouts.DUBEOLSIK, "q", j('ㅂ'), j('ㅃ'), j('ㅂ'), j('ㅄ'), j('ㅂ'))   // → ㅄ (issue #246; #242 had ←)
        assertRow(TapLayouts.QWERTY, "s", t("s"), t("S"), t("s"), t("s"), t("s"))
        assertRow(TapLayouts.DUBEOLSIK, "s", j('ㄴ'), j('ㄴ'), j('ㄵ'), j('ㄶ'), j('ㄴ'))   // ← ㄵ → ㄶ (issue #242)
    }

    @Test
    fun `TA-20 edge hints - every stroked character that differs from the tap, the capital and the shift jamo too (owner 198)`() {
        fun h(layout: TapLayout, id: String) = TapKeyResolver.edgeHints(layout.keys.single { it.id == id })
        assertEquals(mapOf(Dir.UP to "I", Dir.LEFT to "[", Dir.RIGHT to "]"), h(TapLayouts.QWERTY, "i"))
        assertEquals(mapOf(Dir.UP to "S"), h(TapLayouts.QWERTY, "s"))
        assertEquals(mapOf(Dir.UP to "ㅃ", Dir.RIGHT to "ㅄ"), h(TapLayouts.DUBEOLSIK, "q"))   // a shift jamo, a compound final
        assertEquals(emptyMap<Dir, String>(), h(TapLayouts.DUBEOLSIK, "a"))          // ㅁ: no shift jamo, nothing assigned
        assertEquals(mapOf(Dir.LEFT to "[", Dir.RIGHT to "]"), h(TapLayouts.DUBEOLSIK, "i"))   // ㅑ: ↑ is ㅑ again
        for (layout in listOf(TapLayouts.QWERTY, TapLayouts.DUBEOLSIK)) {
            for (k in layout.keys.filter { it.kind == kbd.stroke.layout.TapKeyKind.FUNCTION }) assertEquals(k.id, emptyMap<Dir, String>(), TapKeyResolver.edgeHints(k))
        }
    }

    @Test
    fun `TA-21 Dubeolsik compound finals - one stroke each, the shift jamo stays on up, QWERTY unchanged (issue 242)`() {
        fun jo(c: Char) = TapOutput(null, c, errorVibe = false)
        assertEquals(jo('ㄳ'), d("r", TapGesture.Exit(LEFT)))
        assertEquals(jo('ㄲ'), d("r", TapGesture.Exit(UP)))                    // ㄱ ↑ is still ㄲ
        assertEquals(jo('ㄵ'), d("s", TapGesture.Exit(LEFT)))
        assertEquals(jo('ㄶ'), d("s", TapGesture.Exit(RIGHT)))
        assertEquals(jo('ㅀ'), d("f", TapGesture.Exit(UP)))
        assertEquals(jo('ㄺ'), d("f", TapGesture.Exit(LEFT)))
        assertEquals(jo('ㄻ'), d("f", TapGesture.Exit(RIGHT)))
        assertEquals(jo('ㄼ'), d("f", TapGesture.Exit(DOWN)))
        assertEquals(jo('ㅄ'), d("q", TapGesture.Exit(RIGHT)))                 // → (issue #246)
        assertEquals(jo('ㅂ'), d("q", TapGesture.Exit(LEFT)))                  // ← is the plain ㅂ
        assertEquals(jo('ㅃ'), d("q", TapGesture.Exit(UP)))
        assertEquals(jo('ㄽ'), d("t", TapGesture.Exit(LEFT)))
        assertEquals(jo('ㅆ'), d("t", TapGesture.Exit(UP)))
        assertEquals(jo('ㄾ'), d("x", TapGesture.Exit(LEFT)))
        assertEquals(jo('ㄿ'), d("v", TapGesture.Exit(LEFT)))
        assertEquals(jo('ㄹ'), d("f", TapGesture.Tap))
        // QWERTY keeps its letters.
        assertEquals(t("f"), q("f", TapGesture.Exit(LEFT)))
        assertEquals(t("F"), q("f", TapGesture.Exit(UP)))
        // Edge hints show them (owner #198).
        fun h(id: String) = TapKeyResolver.edgeHints(TapLayouts.DUBEOLSIK.keys.single { it.id == id })
        assertEquals(mapOf(UP to "ㅀ", LEFT to "ㄺ", RIGHT to "ㄻ", DOWN to "ㄼ"), h("f"))
        assertEquals(mapOf(UP to "ㅃ", RIGHT to "ㅄ"), h("q"))
        assertEquals(mapOf(LEFT to "ㄵ", RIGHT to "ㄶ"), h("s"))
    }
}
