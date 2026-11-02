package kbd.hangul.stroke

import kbd.hangul.jamo.Jamo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * LLD-11 §2.3 VS-01..VS-14: the handwriting vowel assembler (SWIPE 0.35 3.3 stroke order, 3.6 candidates,
 * C roles and zones, 3.4 pending glyph, 3.5 ⌫, issues #173, #179). Coordinates are screen dp, t = 20.
 * No stroke carries a length flag: length never decides (issue #179).
 */
class VowelStrokeAssemblerTest {

    private fun v(x: Float, y0: Float, y1: Float) = VowelStroke(StrokeAxis.VERTICAL, x, y0, x, y1)
    private fun h(y: Float, x0: Float, x1: Float) = VowelStroke(StrokeAxis.HORIZONTAL, x0, y, x1, y)
    private fun v(x: Int, y0: Int, y1: Int) = v(x.toFloat(), y0.toFloat(), y1.toFloat())
    private fun h(y: Int, x0: Int, x1: Int) = h(y.toFloat(), x0.toFloat(), x1.toFloat())

    /** SWIPE 3.6.5 (가) and 9.5: C of the 360 × 288 dp portrait layout, centred (issue #213), half a row lower (FSS#37), rounded (C.L = 139, C.R = 221). */
    private val C = ConsonantBox(139f, 76f, 221f, 172f)

    private fun asm() = VowelStrokeAssembler(20f)

    private fun write(c: ConsonantBox?, vararg s: VowelStroke): VowelStrokeAssembler =
        asm().also { a -> s.forEach { assertTrue("$it", a.push(it, c) != StrokePush.Ignored) } }

    private fun write(vararg s: VowelStroke) = write(null, *s)

    /** One row of SWIPE 3.6.5: the strokes and what they leave. */
    private sealed interface Want
    /** All taken, [v] shown, nothing pending. */
    private data class Is(val v: Jamo) : Want
    /** All but the last taken; the last is ignored and [before] stays. */
    private data class LastIgnored(val before: Jamo?) : Want
    /** All taken, then closed by a tap: [v] stays, [dropped] = the error vibration. */
    private data class Closed(val v: Jamo?, val dropped: Boolean) : Want

    private fun check(c: ConsonantBox?, row: String, strokes: List<VowelStroke>, want: Want) {
        val a = asm()
        val taken = if (want is LastIgnored) strokes.dropLast(1) else strokes
        for (s in taken) assertTrue("$row: $s taken", a.push(s, c) != StrokePush.Ignored)
        when (want) {
            is Is -> {
                assertEquals(row, want.v, a.vowel)
                assertEquals(row, "", a.pendingGlyph())
            }
            is LastIgnored -> {
                assertEquals(row, StrokePush.Ignored, a.push(strokes.last(), c))
                assertEquals(row, want.before, a.vowel)
            }
            is Closed -> {
                assertEquals(row, want.v, a.vowel)
                assertEquals(row, want.dropped, a.close())
            }
        }
    }

    // --- SWIPE 3.6.5 (가): with C --------------------------------------------------------------

    private fun line(x0: Float, y0: Float, x1: Float, y1: Float) = VowelStroke(StrokeAxis.HORIZONTAL, x0, y0, x1, y1)

    private val withC: List<Triple<String, List<VowelStroke>, Want>> = listOf(   // SWIPE 0.43 (issue #217: two zones, first stroke two roles)
        Triple("가 ① ㅣ ② right", listOf(v(237, 80, 174), h(124, 237, 262)), Is(Jamo.A)),
        Triple("고 ① dot under C ② underline", listOf(v(177, 176, 199), h(209, 137, 247)), Is(Jamo.O)),
        Triple("고 dot starting inside C (the device's 노)", listOf(v(177, 157, 192), h(209, 137, 247)), Is(Jamo.O)),
        Triple("가 short ㅣ, longer line (zone breaks the tie)", listOf(v(237, 80, 124), h(104, 237, 337)), Is(Jamo.A)),
        Triple("고 long dot, shorter line (zone breaks the tie)", listOf(v(177, 174, 224), h(229, 147, 187)), Is(Jamo.O)),
        Triple("가 dot re-read as ㅣ: the line fits only ㅏ's right", listOf(v(177, 176, 199), h(184, 177, 200)), Is(Jamo.A)),
        Triple("고 ㅣ re-read as a dot: the line fits only the underline", listOf(v(237, 80, 174), h(194, 200, 300)), Is(Jamo.O)),
        Triple("구 dot under C near ㅡ's right end", listOf(h(184, 137, 247), v(227, 192, 224)), Is(Jamo.U)),
        Triple("의 ㅣ right of C, r 0.84", listOf(h(184, 137, 227), v(242, 80, 204)), Is(Jamo.UI)),
        Triple("의 ㅣ over C's right part", listOf(h(184, 137, 227), v(200, 80, 204)), Is(Jamo.UI)),
        Triple("의 ㅣ crosses ㅡ", listOf(h(184, 137, 277), v(227, 80, 204)), Is(Jamo.UI)),
        Triple("거 ㅣ crosses C's dot", listOf(h(124, 227, 277), v(247, 80, 184)), Is(Jamo.EO)),
        Triple("거 ㅣ from just below the dot", listOf(h(124, 227, 277), v(247, 134, 184)), Is(Jamo.EO)),
        Triple("거 C's dot then ㅣ", listOf(h(124, 227, 247), v(252, 80, 184)), Is(Jamo.EO)),
        Triple("거 dot over C's right part", listOf(h(124, 170, 230), v(235, 80, 184)), Is(Jamo.EO)),
        Triple("거 low short dot, 0.5 exception", listOf(h(169, 227, 242), v(247, 80, 174)), Is(Jamo.EO)),
        Triple("거 boundary right-up", listOf(h(184, 137, 227), v(227, 154, 202)), Is(Jamo.EO)),
        Triple("구 boundary under-left", listOf(h(184, 137, 227), v(227, 155, 203)), Is(Jamo.U)),
        Triple("거 long ㅣ, P right-up", listOf(h(184, 137, 227), v(227, 81, 244)), Is(Jamo.EO)),
        Triple("의 ㅣ from C's middle height, P right-up", listOf(h(184, 137, 227), v(227, 124, 201)), Is(Jamo.UI)),
        Triple("구 vertical through C is under-left = a dot (nothing dropped)", listOf(h(184, 137, 227), v(170, 80, 204)), Is(Jamo.U)),
        Triple("구 low ㅡ, dot from above it", listOf(h(209, 137, 247), v(187, 174, 224)), Is(Jamo.U)),
        Triple("규 low ㅡ", listOf(h(209, 137, 247), v(177, 174, 224), v(202, 174, 224)), Is(Jamo.YU)),
        Triple("구 ㅡ a little high and slanted (the device's 뚜)", listOf(line(129f, 163f, 225f, 159f), v(180, 174, 199)), Is(Jamo.U)),
        Triple("그 horizontal over C's left part, under-left", listOf(h(134, 137, 197)), Is(Jamo.EU)),
        Triple("ㄱㆍ horizontal in C's upper left, right-up: a dot, dropped on closing", listOf(h(114, 137, 197)), Closed(null, true)),
        Triple("ㄱㆍ vertical left of C, under-left: a dot, dropped on closing", listOf(v(80, 80, 174)), Closed(null, true)),
        Triple("겨 the second horizontal re-reads ㅡ as a dot", listOf(h(184, 137, 227), h(204, 137, 227), v(240, 80, 224)), Is(Jamo.YEO)),
        Triple("기 second vertical ignored", listOf(v(237, 80, 174), v(262, 80, 174)), LastIgnored(Jamo.I)),
        Triple("교", listOf(v(167, 176, 196), v(192, 176, 196), h(209, 137, 227)), Is(Jamo.YO)),
        Triple("귀", listOf(h(184, 137, 247), v(187, 192, 219), v(262, 80, 224)), Is(Jamo.WI)),
        Triple("규", listOf(h(184, 137, 247), v(177, 192, 219), v(202, 192, 219)), Is(Jamo.YU)),
        Triple("긴 ㅣ closed", listOf(v(237, 80, 174)), Closed(Jamo.I, false)),
        Triple("ㄱㄴ dot under C closed", listOf(v(177, 176, 199)), Closed(null, true)),
        Triple("근 ㅡ closed", listOf(h(184, 137, 247)), Closed(Jamo.EU, false)),
        Triple("ㄱㄴ dot beside C closed", listOf(h(124, 227, 247)), Closed(null, true)),
    )

    @Test
    fun `VS-14 SWIPE 3_6_5 (ga) with the consonant place C`() {
        assertEquals(36, withC.size)
        for ((row, strokes, want) in withC) check(C, row, strokes, want)
    }

    // --- SWIPE 3.6.5 (나): bare vowels (no C) --------------------------------------------------

    private val ga = listOf(v(100, 40, 160), h(100, 100, 120))
    private val goe = listOf(v(100, 60, 95), h(100, 50, 150), v(160, 40, 170))
    private val gwoUpTo3 = listOf(h(100, 50, 150), v(100, 105, 135), h(120, 100, 130))

    private val bare: List<Triple<String, List<VowelStroke>, Want>> = listOf(
        Triple("구", listOf(h(100, 50, 150), v(100, 100, 190)), Is(Jamo.U)),
        Triple("구 short dot", listOf(h(100, 50, 150), v(100, 100, 125)), Is(Jamo.U)),
        Triple("구 dot apart below", listOf(h(100, 50, 150), v(100, 130, 160)), Is(Jamo.U)),
        Triple("구 short ㅡ", listOf(h(100, 100, 140), v(115, 100, 180)), Is(Jamo.U)),
        Triple("의 r 0.73", listOf(h(100, 50, 150), v(160, 20, 130)), Is(Jamo.UI)),
        Triple("의 ㅣ ends above", listOf(h(100, 50, 150), v(160, 20, 80)), Is(Jamo.UI)),
        Triple("고 longer line", listOf(v(100, 40, 98), h(100, 50, 150)), Is(Jamo.O)),
        Triple("고 dot apart above", listOf(v(100, 20, 60), h(100, 50, 150)), Is(Jamo.O)),
        Triple("가 shorter line at ㅣ's bottom", listOf(v(100, 40, 160), h(162, 40, 150)), Is(Jamo.A)),
        Triple("고 longer line", listOf(v(100, 40, 100), h(102, 40, 150)), Is(Jamo.O)),
        Triple("고 line across the dot's lower half", listOf(v(100, 60, 90), h(80, 60, 140)), Is(Jamo.O)),
        Triple("line across the dot's upper half ignored", listOf(v(100, 60, 90), h(65, 60, 140)), LastIgnored(Jamo.I)),
        Triple("가 equal lengths", listOf(v(100, 40, 100), h(70, 100, 160)), Is(Jamo.A)),
        Triple("가", listOf(v(100, 40, 160), h(100, 100, 120)), Is(Jamo.A)),
        Triple("가 longer dot", listOf(v(100, 40, 160), h(100, 100, 180)), Is(Jamo.A)),
        Triple("가 dot apart", listOf(v(100, 40, 160), h(100, 130, 150)), Is(Jamo.A)),
        Triple("가 dot high", listOf(v(100, 40, 160), h(60, 100, 120)), Is(Jamo.A)),
        Triple("dot left of ㅣ ignored", listOf(v(100, 40, 160), h(100, 60, 98)), LastIgnored(Jamo.I)),
        Triple("가 line across ㅣ", listOf(v(100, 40, 160), h(100, 60, 140)), Is(Jamo.A)),
        Triple("가 ㄴ shape", listOf(v(100, 40, 160), h(160, 100, 140)), Is(Jamo.A)),
        Triple("가 dot at ㅣ's start", listOf(v(100, 40, 160), h(42, 100, 120)), Is(Jamo.A)),
        Triple("가 both ways at the bottom", listOf(v(100, 40, 160), h(158, 60, 140)), Is(Jamo.A)),
        Triple("가 across", listOf(v(100, 40, 160), h(140, 60, 140)), Is(Jamo.A)),
        Triple("dot below ㅣ ignored", listOf(v(100, 40, 160), h(190, 100, 130)), LastIgnored(Jamo.I)),
        Triple("고 dot far above", listOf(v(100, 20, 50), h(120, 60, 160)), Is(Jamo.O)),
        Triple("거 r 0.4", listOf(h(100, 60, 80), v(82, 60, 160)), Is(Jamo.EO)),
        Triple("거 long line r 0.4", listOf(h(100, 50, 150), v(155, 60, 160)), Is(Jamo.EO)),
        Triple("거 ㅣ apart right", listOf(h(100, 40, 60), v(90, 60, 160)), Is(Jamo.EO)),
        Triple("거 ㅇㄱ", listOf(h(100, 40, 100), v(100, 110, 190)), Is(Jamo.EO)),
        Triple("거 upper third", listOf(h(64, 60, 80), v(82, 60, 160)), Is(Jamo.EO)),
        Triple("의 lower third, line ≥ half", listOf(h(140, 10, 80), v(82, 40, 160)), Is(Jamo.UI)),
        Triple("거 lower third, line < half", listOf(h(140, 60, 80), v(82, 40, 160)), Is(Jamo.EO)),
        Triple("의 short ㅣ", listOf(h(140, 10, 80), v(82, 100, 150)), Is(Jamo.UI)),
        Triple("의 ㅣ ends above the line", listOf(h(140, 10, 80), v(82, 60, 120)), Is(Jamo.UI)),
        Triple("게 after a ㅢ-looking ②", listOf(h(140, 10, 80), v(82, 40, 160), v(110, 40, 160)), Is(Jamo.E)),
        Triple("겨", listOf(h(80, 60, 80), h(120, 60, 80), v(90, 40, 160)), Is(Jamo.YEO)),
        Triple("교", listOf(v(90, 40, 70), v(110, 40, 70), h(100, 60, 140)), Is(Jamo.YO)),
        Triple("개", ga + v(125, 60, 170), Is(Jamo.AE)),
        Triple("ㅣ over ㅏ's ㅣ ignored", ga + v(100, 40, 160), LastIgnored(Jamo.A)),
        Triple("ㅣ left of ㅏ ignored", ga + v(78, 60, 170), LastIgnored(Jamo.A)),
        Triple("거 across ㅡ's left part", listOf(h(100, 50, 150), v(80, 60, 180)), Is(Jamo.EO)),
        Triple("left of ㅡ ignored", listOf(h(100, 50, 150), v(20, 60, 180)), LastIgnored(Jamo.EU)),
        Triple("의 across, lower third", listOf(h(140, 10, 80), v(40, 40, 160)), Is(Jamo.UI)),
        Triple("거 across a dot", listOf(h(100, 40, 100), v(60, 60, 160)), Is(Jamo.EO)),
        Triple("겨 across two dots", listOf(h(80, 60, 80), h(120, 60, 80), v(70, 40, 160)), Is(Jamo.YEO)),
        Triple("거 line past ㅣ", listOf(h(100, 50, 150), v(120, 60, 160)), Is(Jamo.EO)),
        Triple("거 dot 25 past ㅣ", listOf(h(100, 40, 110), v(85, 60, 160)), Is(Jamo.EO)),
        Triple("구 same x from ㅡ's height", listOf(h(100, 50, 150), v(120, 100, 180)), Is(Jamo.U)),
        Triple("귀", listOf(h(100, 50, 150), v(100, 102, 130), v(165, 70, 190)), Is(Jamo.WI)),
        Triple("규", listOf(h(100, 50, 150), v(90, 102, 130), v(115, 102, 130)), Is(Jamo.YU)),
        Triple("궈", gwoUpTo3 + v(135, 90, 180), Is(Jamo.WEO)),
        Triple("괴", goe, Is(Jamo.OE)),
        Triple("과", goe + h(100, 160, 180), Is(Jamo.WA)),
        Triple("괘", goe + h(100, 160, 180) + v(190, 40, 170), Is(Jamo.WAE)),
        Triple("긴 one vertical closed", listOf(v(100, 60, 90)), Closed(Jamo.I, false)),
        Triple("근 one horizontal closed", listOf(h(100, 100, 130)), Closed(Jamo.EU, false)),
        Triple("군 구ㆍ closed", gwoUpTo3, Closed(Jamo.U, true)),
        Triple("긴 ‥ closed", listOf(v(90, 40, 70), v(110, 40, 70)), Closed(Jamo.I, true)),
    )

    @Test
    fun `VS-02 SWIPE 3_6_5 (na) bare vowels`() {
        assertEquals(58, bare.size)
        for ((row, strokes, want) in bare) check(null, row, strokes, want)
    }

    // --- shapes --------------------------------------------------------------------------------

    private val I = v(100, 40, 160)
    private val dotA = h(100, 100, 125)

    private val shapes: Map<Jamo, List<VowelStroke>> = mapOf(
        Jamo.I to listOf(I),
        Jamo.EU to listOf(h(100, 50, 150)),
        Jamo.A to listOf(I, dotA),
        Jamo.YA to listOf(I, h(80, 100, 125), h(120, 100, 125)),
        Jamo.AE to listOf(I, dotA, v(130, 40, 160)),
        Jamo.YAE to listOf(I, h(80, 100, 125), h(120, 100, 125), v(130, 40, 160)),
        Jamo.EO to listOf(h(100, 60, 85), v(87, 40, 160)),
        Jamo.YEO to listOf(h(80, 60, 85), h(120, 60, 85), v(87, 40, 160)),
        Jamo.E to listOf(h(100, 60, 85), v(87, 40, 160), v(120, 40, 160)),
        Jamo.YE to listOf(h(80, 60, 85), h(120, 60, 85), v(87, 40, 160), v(120, 40, 160)),
        Jamo.O to listOf(v(100, 60, 85), h(90, 50, 150)),
        Jamo.YO to listOf(v(80, 60, 85), v(120, 60, 85), h(90, 50, 150)),
        Jamo.U to listOf(h(100, 50, 150), v(100, 100, 125)),
        Jamo.YU to listOf(h(100, 50, 150), v(80, 100, 125), v(120, 100, 125)),
        Jamo.OE to listOf(v(100, 60, 85), h(90, 50, 150), v(160, 40, 160)),
        Jamo.WA to listOf(v(100, 60, 85), h(90, 50, 150), v(160, 40, 160), h(100, 160, 185)),
        Jamo.WAE to listOf(v(100, 60, 85), h(90, 50, 150), v(160, 40, 160), h(100, 160, 185), v(190, 40, 160)),
        Jamo.WEO to listOf(h(100, 50, 150), v(100, 100, 125), h(130, 110, 135), v(137, 60, 170)),
        Jamo.WE to listOf(h(100, 50, 150), v(100, 100, 125), h(130, 110, 135), v(137, 60, 170), v(170, 60, 170)),
        Jamo.WI to listOf(h(100, 50, 150), v(100, 100, 125), v(160, 60, 170)),
        Jamo.UI to listOf(h(100, 50, 150), v(160, 20, 130)),
    )

    /** The same 21 vowels written beside C (SWIPE 9.5 space check). */
    private val shapesC: Map<Jamo, List<VowelStroke>> = mapOf(
        Jamo.I to listOf(v(237, 80, 174)),
        Jamo.EU to listOf(h(184, 137, 247)),
        Jamo.A to listOf(v(237, 80, 174), h(124, 237, 262)),
        Jamo.YA to listOf(v(237, 80, 174), h(114, 237, 262), h(144, 237, 262)),
        Jamo.AE to listOf(v(237, 80, 174), h(124, 237, 262), v(272, 80, 174)),
        Jamo.YAE to listOf(v(237, 80, 174), h(114, 237, 262), h(144, 237, 262), v(272, 80, 174)),
        Jamo.EO to listOf(h(124, 227, 252), v(257, 80, 184)),
        Jamo.YEO to listOf(h(114, 227, 252), h(144, 227, 252), v(257, 80, 184)),
        Jamo.E to listOf(h(124, 227, 252), v(257, 80, 184), v(282, 80, 184)),
        Jamo.YE to listOf(h(114, 227, 252), h(144, 227, 252), v(257, 80, 184), v(282, 80, 184)),
        Jamo.O to listOf(v(177, 176, 199), h(209, 137, 247)),
        Jamo.YO to listOf(v(167, 176, 196), v(192, 176, 196), h(209, 137, 227)),
        Jamo.OE to listOf(v(177, 176, 199), h(209, 137, 247), v(262, 80, 224)),
        Jamo.WA to listOf(v(177, 176, 199), h(209, 137, 247), v(262, 80, 224), h(134, 262, 287)),
        Jamo.WAE to listOf(v(177, 176, 199), h(209, 137, 247), v(262, 80, 224), h(134, 262, 287), v(302, 80, 224)),
        Jamo.U to listOf(h(184, 137, 247), v(177, 192, 219)),
        Jamo.YU to listOf(h(184, 137, 247), v(177, 192, 219), v(202, 192, 219)),
        Jamo.WI to listOf(h(184, 137, 247), v(187, 192, 219), v(262, 80, 224)),
        Jamo.WEO to listOf(h(184, 137, 247), v(177, 192, 219), h(209, 187, 212), v(227, 80, 244)),
        Jamo.WE to listOf(h(184, 137, 247), v(177, 192, 219), h(209, 187, 212), v(227, 80, 244), v(257, 80, 244)),
        Jamo.UI to listOf(h(184, 137, 227), v(242, 80, 204)),
    )

    @Test
    fun `VS-01 all 21 vowels in stroke order, bare and beside C`() {
        for ((c, table) in listOf(null to shapes, C to shapesC)) {
            assertEquals(21, table.size)
            for ((vowel, strokes) in table) {
                val a = write(c, *strokes.toTypedArray())
                assertEquals("$c $vowel", vowel, a.vowel)
                assertEquals("$c $vowel", "", a.pendingGlyph())
            }
        }
    }

    @Test
    fun `VS-03 composer events - started, changed, pending - and a bare second stroke re-reads the first`() {
        val a = asm()
        assertEquals(StrokePush.Pending, a.push(v(177, 176, 199), C))             // C's vertical dot: no vowel yet
        assertEquals("ㆍ", a.pendingGlyph())
        assertEquals(StrokePush.Started(Jamo.O), a.push(h(209, 137, 247), C))
        assertEquals(StrokePush.Changed(Jamo.OE), a.push(v(262, 80, 224), C))
        assertEquals(StrokePush.Changed(Jamo.WA), a.push(h(134, 262, 287), C))
        assertEquals(StrokePush.Changed(Jamo.WAE), a.push(v(302, 80, 224), C))
        // Bare 기 → 고 and 그 → 거: one stroke shows ㅣ / ㅡ; the second makes it a dot.
        val b = asm()
        assertEquals(StrokePush.Started(Jamo.I), b.push(v(100, 40, 98)))
        assertEquals(StrokePush.Changed(Jamo.O), b.push(h(100, 50, 150)))
        val g = asm()
        assertEquals(StrokePush.Started(Jamo.EU), g.push(h(100, 60, 80)))
        assertEquals(StrokePush.Changed(Jamo.EO), g.push(v(82, 60, 160)))
        // With C the first stroke is a dot from the start: no re-reading.
        val c = asm()
        assertEquals(StrokePush.Pending, c.push(h(124, 227, 247), C))
        assertEquals(StrokePush.Started(Jamo.EO), c.push(v(252, 80, 184), C))
    }

    @Test
    fun `VS-04 pending glyphs - C's dots, the dot after ㅜ, bare two strokes keep the first vowel`() {
        assertEquals("ㆍ", write(C, v(177, 176, 199)).pendingGlyph())
        assertEquals("ㆍ", write(C, h(124, 227, 247)).pendingGlyph())
        val vv = write(C, v(167, 176, 196), v(192, 176, 196))
        assertEquals("‥", vv.pendingGlyph())
        assertNull(vv.vowel)
        val hh = write(C, h(114, 227, 252), h(144, 227, 252))
        assertEquals("‥", hh.pendingGlyph())
        assertNull(hh.vowel)
        val wuDot = write(*gwoUpTo3.toTypedArray())   // ㅜ + dot
        assertEquals(Jamo.U, wuDot.vowel)
        assertEquals("ㆍ", wuDot.pendingGlyph())
        // Bare (SWIPE 0.36 3.4, #192, LLD D-64): two verticals / two horizontals show "‥" alone in place of the
        // first stroke's ㅣ / ㅡ, which the composer keeps; ⌫ shows it again, closing leaves it (with the vibration).
        val bareVv = write(v(90, 40, 70), v(110, 40, 70))
        assertEquals(Jamo.I, bareVv.vowel)
        assertEquals("‥", bareVv.pendingGlyph())
        assertTrue(bareVv.pendingReplacesVowel)
        assertEquals(StrokePop.Popped(false), bareVv.pop())
        assertEquals("", bareVv.pendingGlyph())
        assertFalse(bareVv.pendingReplacesVowel)
        assertEquals(Jamo.I, bareVv.vowel)
        val bareHh = write(h(80, 60, 80), h(120, 60, 80))
        assertEquals(Jamo.EU, bareHh.vowel)
        assertEquals("‥", bareHh.pendingGlyph())
        assertTrue(bareHh.pendingReplacesVowel)
        assertEquals("‥", write(h(80, 60, 80)).preview(h(120, 60, 80)))
        assertTrue(bareHh.close())
        assertFalse(write(C, v(167, 176, 196), v(192, 176, 196)).pendingReplacesVowel)   // with C: no vowel to hide, plain ‥
    }

    @Test
    fun `VS-05 strokes that fit no candidate are ignored and change nothing`() {
        val i = write(I)
        assertEquals(StrokePush.Ignored, i.push(v(100, 40, 160)))            // ㅣ then ㅣ on it
        val eu = write(h(100, 50, 150))
        assertEquals(StrokePush.Ignored, eu.push(h(100, 50, 150)))           // ㅡ then ㅡ at its height
        val o = write(*shapes.getValue(Jamo.O).toTypedArray())
        assertEquals(StrokePush.Ignored, o.push(v(100, 60, 85)))             // ㅗ then a vertical dot
        val ae = write(*shapes.getValue(Jamo.AE).toTypedArray())
        assertEquals(StrokePush.Ignored, ae.push(h(100, 130, 150)))          // ㅐ then a dot
        assertEquals(Jamo.AE, ae.vowel)
        assertEquals(StrokePush.Ignored, write(h(100, 50, 150)).push(v(20, 60, 85)))   // left of ㅡ
        // With C: a later vertical whose zone is not the role wanted (SWIPE 0.43: ㅛ ② must be an under-left dot, ㅚ ③ a right-up ㅣ).
        assertEquals(StrokePush.Ignored, write(C, v(177, 176, 199)).push(v(237, 80, 174)))     // ㅛ ② must be a dot
        assertEquals(StrokePush.Ignored, write(C, h(124, 227, 247)).push(v(150, 80, 174)))     // left of the first horizontal (x < L − t)
        assertEquals(StrokePush.Ignored, write(C, *shapesC.getValue(Jamo.O).toTypedArray()).push(v(240, 214, 264)))   // ㅚ ③ must be ㅣ: P (240, 239), d 48 = under-left
        assertEquals(StrokePush.Pending, write(C, h(184, 137, 247)).push(h(209, 137, 247)))      // SWIPE 0.43: ㅕ ② re-reads ㅡ as a dot (‥)
        assertEquals(StrokePush.Ignored, write(C, h(184, 137, 247)).push(h(184, 137, 247)))      // a second ㅡ at the same height: no zone
    }

    @Test
    fun `VS-06 backspace in an open assembly takes back one stroke, undoing the composer only when the vowel changed`() {
        val a = write(C, *shapesC.getValue(Jamo.WA).toTypedArray())
        assertEquals(StrokePop.Popped(true), a.pop())   // ㅘ → ㅚ
        assertEquals(Jamo.OE, a.vowel)
        assertEquals(StrokePop.Popped(true), a.pop())   // ㅚ → ㅗ
        assertEquals(StrokePop.Popped(true), a.pop())   // ㅗ → ㄱㆍ (the vowel unit goes, SWIPE 3.5 고 ⌫)
        assertEquals("ㆍ", a.pendingGlyph())
        assertEquals(StrokePop.Popped(false), a.pop())  // the dot alone: the composer had nothing of it
        assertEquals(StrokePop.Empty, a.pop())
        assertNull(a.consonant)
        val bareO = write(v(100, 60, 85), h(90, 50, 150))
        assertEquals(StrokePop.Popped(true), bareO.pop())   // bare ㅗ ⌫ = ㅣ (SWIPE 3.5)
        assertEquals(Jamo.I, bareO.vowel)
        val wuDot = write(*gwoUpTo3.toTypedArray())
        assertEquals(StrokePop.Popped(false), wuDot.pop())   // ㅜ + ㆍ → ㅜ: only the glyph
        assertEquals(Jamo.U, wuDot.vowel)
    }

    @Test
    fun `VS-07 close drops unfinished strokes and says so, a vowel closes quietly`() {
        assertTrue(write(C, h(124, 227, 247)).close())
        assertTrue(write(*gwoUpTo3.toTypedArray()).close())
        assertFalse(write(I, dotA).close())
        val a = write(C, v(237, 80, 174))
        a.close()
        assertTrue(a.isEmpty)
        assertNull(a.consonant)
        assertEquals(StrokePush.Started(Jamo.I), a.push(I))   // a closed vowel never reopens; the new one is bare here
    }

    @Test
    fun `VS-08 C role and zone boundaries at 1 dp`() {
        // SWIPE 0.43 3.6.3 (issue #217): the middle point P against the 45° line through (C.R, C.B) = (221, 172),
        // d = (P.y − 172) − (P.x − 221); d > 0 = under-left (dot / ㅡ), d ≤ 0 = right-up (ㅣ / horizontal dot).
        assertEquals(Jamo.I, write(C, v(221f, 162f, 182f)).vowel)                       // P (221, 172), d 0: right-up
        assertEquals("ㆍ", write(C, v(221f, 162.2f, 182.2f)).pendingGlyph())           // d 0.2: under-left
        assertEquals(Jamo.I, write(C, v(200f, 131f, 171f)).vowel)                       // P (200, 151), d 0
        assertEquals("ㆍ", write(C, v(200f, 131.2f, 171.2f)).pendingGlyph())
        assertEquals("ㆍ", write(C, h(151f, 190f, 210f)).pendingGlyph())               // P (200, 151), d 0: a horizontal dot
        assertEquals(Jamo.EU, write(C, h(151.2f, 190f, 210f)).vowel)                    // d 0.2: ㅡ
        // No first stroke is dropped for its place: far left, inside C, high up.
        for (first in listOf(v(60, 80, 174), v(180, 84, 164), h(84, 140, 200), h(124, 60, 100))) assertTrue("$first", asm().push(first, C) != StrokePush.Ignored)
        // Bare ㅏ: the dot reaches S.x + t/2 = 110 and lies within S's height ± t/4.
        assertEquals(Jamo.A, write(I, h(100, 60, 110)).vowel)
        assertEquals(StrokePush.Ignored, write(I).push(h(100, 60, 109)))
        assertEquals(Jamo.A, write(I, h(165, 100, 120)).vowel)
        assertEquals(StrokePush.Ignored, write(I).push(h(166, 100, 120)))
        // Bare ㅜ / ㅓ: x < R − t = 130 below is ㅜ; from 130 on the end (ㅓ / ㅢ); T vs y − t/2 = 90 splits the crossing.
        val eu = h(100, 50, 150)
        assertEquals(Jamo.U, write(eu, v(129, 100, 140)).vowel)
        assertEquals(Jamo.EO, write(eu, v(130, 60, 140)).vowel)
        assertEquals(Jamo.U, write(eu, v(100, 90, 140)).vowel)
        assertEquals(Jamo.EO, write(eu, v(100f, 89f, 140f)).vowel)
        assertEquals(Jamo.EO, write(eu, v(30, 60, 160)).vowel)
        assertEquals(StrokePush.Ignored, write(eu).push(v(29, 60, 160)))
    }

    @Test
    fun `VS-11 length decides nothing outside the two relative-size rules`() {
        // Shortening or lengthening the second stroke keeps the result (SWIPE 3.6.5 (나) pairs).
        for (len in listOf(15, 25, 60, 150)) {
            assertEquals("$len", Jamo.U, write(h(100, 50, 150), v(100, 100, 100 + len)).vowel)
            assertEquals("$len", Jamo.YA, write(I, h(80, 100, 125), h(120, 100, 100 + len)).vowel)
            assertEquals("$len", Jamo.OE, write(C, v(177, 176, 199), h(209, 137, 247), v(262, 80, 80 + len)).vowel)
        }
        // With C the first stroke's own length does not matter either: a short ㅣ, a long dot.
        assertEquals(Jamo.I, write(C, v(237, 80, 94)).vowel)
        assertEquals("ㆍ", write(C, v(177, 174, 304)).pendingGlyph())
    }

    @Test
    fun `VS-12 eo and ui by height thirds and the 0_5 exception - boundaries at 1 dp`() {
        val i = v(87, 40, 160)                                                     // T 40, B 160: the split is at y = 120
        assertEquals(Jamo.EO, write(h(119, 20, 85), i).vowel)                      // r 0.658
        assertEquals(Jamo.UI, write(h(120, 20, 85), i).vowel)                      // r = 2/3 exactly, line 65 ≥ 60: ㅢ
        assertEquals(Jamo.UI, write(h(200, 20, 85), i).vowel)                      // ㅣ ends above the line (r 1.33)
        assertEquals(Jamo.EO, write(h(10, 20, 85), i).vowel)                       // line above the ㅣ (r < 0)
        // TBD-22: line < ㅣ × 0.5 (60) is ㅓ even on the lower third.
        assertEquals(Jamo.UI, write(h(140, 25, 85), i).vowel)                      // 60: not shorter
        assertEquals(Jamo.EO, write(h(140, 26, 85), i).vowel)                      // 59
        // ㅔ's ② at either height; ③ beside it settles ㅔ.
        val e = write(h(140, 10, 80), v(82, 40, 160))
        assertEquals(Jamo.UI, e.vowel)
        assertEquals(StrokePush.Changed(Jamo.E), e.push(v(110, 40, 160)))
        // With C both go by the thirds, whatever the first horizontal's zone (SWIPE 0.43: "가로 점이면 ㅓ뿐" is gone).
        assertEquals(Jamo.UI, write(C, h(124, 227, 277), v(247, 80, 129)).vowel)  // right-up dot, r 0.9, line 50 ≥ 24.5
        assertEquals(Jamo.UI, write(C, h(184, 137, 227), v(242, 80, 204)).vowel)
    }

    @Test
    fun `VS-13 C is fixed by an assembly's first stroke`() {
        val a = asm()
        assertEquals(StrokePush.Started(Jamo.I), a.push(v(237, 80, 174), C))
        assertEquals(C, a.consonant)
        assertEquals(StrokePush.Changed(Jamo.A), a.push(h(124, 237, 262), null))   // C stays for the open assembly
        val b = asm()
        b.push(v(100, 40, 160))                                                     // bare
        assertNull(b.consonant)
        assertEquals("ㅏ", b.preview(h(100, 100, 120), C))                          // a later C does not apply
    }

    @Test
    fun `VS-09 preview shows what a stroke would give without taking it`() {
        val a = write(I)
        assertEquals("ㅏ", a.preview(dotA))
        assertNull(a.preview(v(100, 40, 160)))
        assertEquals(listOf(I), a.strokes)
        assertEquals("ㆍ", asm().preview(h(124, 227, 247), C))
        assertEquals("ㅡ", asm().preview(h(100, 150, 170)))
        assertEquals("ㆍ", asm().preview(v(137, 80, 174), C))                      // over C's left part: under-left, a dot (nothing is dropped)
    }

    @Test
    fun `VS-10 snapshot and restore keep C`() {
        val a = write(C, v(177, 176, 199))
        val s = a.snapshot()
        a.close()
        a.restore(s)
        assertEquals(C, a.consonant)
        assertEquals(StrokePush.Started(Jamo.O), a.push(h(209, 137, 247)))
    }

    @Test
    fun `debug text (issue 217) - role by C with its numbers, why a stroke would be ignored, the state and candidates`() {
        val a = asm()
        assertEquals("empty vowel=- pending=-", a.stateText())
        // SWIPE 0.43 two zones: P = the middle point, d = (P.y − C.B) − (P.x − C.R) with C.R 221, C.B 172.
        val through = v(170, 80, 204)                                 // P (170, 142), d = −30 + 51 = 21: under-left
        assertEquals("first vertical under-left: shows a dot, both roles kept (P (170.0,142.0), d 21.0; C.R 221.0, C.B 172.0)",
            a.roleText(through, C))
        assertNull(a.ignoreReason(through, C))                       // nothing is dropped for its place
        val overlap = v(177, 157, 192)                                // the device's 노 dot moved to 360 dp: P (177, 174.5), d 46.5
        assertTrue(a.roleText(overlap, C).startsWith("first vertical under-left: shows a dot"))
        assertTrue(a.roleText(v(237, 80, 174), C).startsWith("first vertical right-up: shows ㅣ, both roles kept (P (237.0,127.0), d -61.0"))
        assertTrue(a.roleText(h(184, 137, 247), C).startsWith("first horizontal under-left: shows ㅡ"))
        assertTrue(a.roleText(h(124, 227, 247), C).startsWith("first horizontal right-up: shows a dot"))
        val dot = v(180, 174, 194)
        assertEquals("no C (bare vowel)", a.roleText(dot))
        a.push(dot, C)
        assertEquals("V1[ㅣㅏㅑㅐㅒㅗㅛㅚㅘㅙ] vowel=- pending=ㆍ", a.stateText())
        assertEquals("not by C (a later horizontal stroke)", a.roleText(h(209, 140, 220)))
        assertEquals("vertical under-left = dot (P (150.0,184.0), d 83.0; C.R 221.0, C.B 172.0)", a.roleText(v(150, 174, 194)))
        assertEquals("no vertical zone of the next candidates fits", a.ignoreReason(v(150, 174, 194)))   // left of the dot: not beside
        assertNull(a.ignoreReason(h(209, 140, 220)))
        a.push(h(209, 140, 220))
        assertEquals("O[ㅗㅚㅘㅙ] vowel=ㅗ pending=-", a.stateText())
    }
}
