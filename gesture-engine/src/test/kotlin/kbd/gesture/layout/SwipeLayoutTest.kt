package kbd.gesture.layout

import kbd.gesture.classify.SwipeKeyKind
import kbd.hangul.jamo.Jamo
import kbd.stroke.geom.Dir
import kbd.stroke.geom.Edge
import kbd.stroke.geom.KeyGeom
import kbd.stroke.geom.RectDp
import kbd.stroke.layout.FunctionKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** LLD-11 §4.5 SL-01, SL-02 and §3 KH-06, plus the §8.2–8.3 tables. */
class SwipeLayoutTest {

    // 360 dp window, 288 dp high; the 6 rows of 48 dp fill it (SWIPE 2.1, issue #168: no band above).
    private val windowW = 360f
    private val windowH = 288f
    private val rowH = 48f
    private val top = windowH - SwipeLayout.ROWS * rowH

    private fun geomOf(k: SwipeKeyDef): KeyGeom {
        val colW = windowW / SwipeLayout.COLS
        val rect = RectDp(k.col * colW, top + k.row * rowH, (k.col + k.colSpan) * colW, top + (k.row + 1) * rowH)
        return KeyGeom.build(k.id, rect, windowW, windowH)
    }

    private val covered: List<Pair<Int, Int>> =
        SwipeLayout.KEYS.flatMap { k -> (k.col until k.col + k.colSpan).map { c -> c to k.row } }

    /** Grid cell (col, row) → the key covering it. */
    private val cells: Map<Pair<Int, Int>, SwipeKeyDef> =
        SwipeLayout.KEYS.flatMap { k -> (k.col until k.col + k.colSpan).map { c -> (c to k.row) to k } }.toMap()

    private fun key(label: String) = SwipeLayout.KEYS.single { it.label == label }

    @Test
    fun `SL-01 7 by 6 grid, 19 consonants with four neighbours, the cheonjiin key and one empty cell`() {
        assertEquals(7, SwipeLayout.COLS)
        assertEquals(6, SwipeLayout.ROWS)
        // Every cell is covered exactly once; ids are unique.
        assertEquals(SwipeLayout.COLS * SwipeLayout.ROWS, covered.size)
        assertEquals(covered.size, covered.toSet().size)
        assertTrue(covered.all { (c, r) -> c in 0 until SwipeLayout.COLS && r in 0 until SwipeLayout.ROWS })
        assertEquals(SwipeLayout.KEYS.size, SwipeLayout.KEYS.map { it.id }.toSet().size)

        val consonants = SwipeLayout.KEYS.filter { it.kind == SwipeKeyKind.CONSONANT }
        assertEquals(
            listOf(Jamo.BB, Jamo.JJ, Jamo.DD, Jamo.GG, Jamo.SS,   // row 1, each above its pair (layout A, issue #168)
                   Jamo.B, Jamo.J, Jamo.D, Jamo.G, Jamo.S, Jamo.M, Jamo.N, Jamo.NG, Jamo.R, Jamo.H,
                   Jamo.K, Jamo.T, Jamo.C, Jamo.P),
            consonants.map { it.consonant })
        for (k in consonants) {
            assertEquals(1, k.colSpan)
            for ((dc, dr) in listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)) {
                assertNotNull("${k.label} neighbour ($dc,$dr)", cells[(k.col + dc) to (k.row + dr)])
            }
            assertEquals("${k.label} outer edges", emptySet<Edge>(), geomOf(k).outerEdges)
        }

        val empties = SwipeLayout.KEYS.filter { it.kind == SwipeKeyKind.EMPTY }
        assertEquals(listOf(Triple(5, 5, 2)), empties.map { Triple(it.col, it.row, it.colSpan) })   // issues #173, #205, #208
        for ((dbl, base) in listOf(Jamo.BB to Jamo.B, Jamo.JJ to Jamo.J, Jamo.DD to Jamo.D, Jamo.GG to Jamo.G, Jamo.SS to Jamo.S)) {
            val d = SwipeLayout.KEYS.single { it.consonant == dbl }
            val b = SwipeLayout.KEYS.single { it.consonant == base }
            assertEquals(dbl.toString(), d.col to d.row + 1, b.col to b.row)   // right above its pair
        }
        // FR-SWP-60, issue #173: row 3 col 0 (below 0, left of ㅁ) is the cheonjiin key, its left edge the window's.
        val cj = SwipeLayout.KEYS.single { it.kind == SwipeKeyKind.CHEONJIIN }
        assertEquals(Triple(0, 3, 1), Triple(cj.col, cj.row, cj.colSpan))
        assertEquals("ㆍ ㅡ ㅣ", cj.label)
        assertEquals(SwipeLayout.CHEONJIIN_ID, cj.id)
        assertEquals(setOf(Edge.LEFT), geomOf(cj).outerEdges)
        assertTrue(empties.all { it.consonant == null && it.digit == null && it.function == null })
        // Issue #173 moved the end columns up a cell; issue #205 removed the ⚙ key above the mode key, issue #208 put symbol3 there.
        assertTrue(SwipeLayout.KEYS.none { it.label == "⚙" || it.id == "settings" })
        assertEquals("symbol3", cells.getValue(0 to 4).id)
        assertEquals(listOf("1", "8", "0", "ㆍ ㅡ ㅣ", "/", "한/영"), (0..5).map { cells.getValue(0 to it).label })
        assertEquals(listOf("7", "9", "⌫", "↵", ",", ""), (0..5).map { cells.getValue(6 to it).label })   // symbol2 at row 4 (issue #208)
    }

    @Test
    fun `SL-02 mode key is row 5 cols 0-1`() {
        val mode = SwipeLayout.KEYS.single { it.function == FunctionKey.MODE }
        assertEquals(SwipeKeyKind.FUNCTION, mode.kind)
        assertEquals(0, mode.col)
        assertEquals(5, mode.row)
        assertEquals(2, mode.colSpan)
        assertEquals(mode, cells[0 to 5])
        assertEquals(mode, cells[1 to 5])
        assertEquals("한/영", mode.label)
    }

    @Test
    fun `KH-06 outer edges of the swipe layout in a 7-column window`() {
        val outer = SwipeLayout.KEYS.associate { it.id to geomOf(it).outerEdges }
        for (d in "180") assertEquals("$d", setOf(Edge.LEFT), outer[key("$d").id])
        for (d in "79") assertEquals("$d", setOf(Edge.RIGHT), outer[key("$d").id])
        assertEquals(setOf(Edge.RIGHT), outer[key("⌫").id])
        assertEquals(setOf(Edge.RIGHT), outer[key("↵").id])
        for (d in "23456") assertEquals("$d", emptySet<Edge>(), outer[key("$d").id])
        assertEquals(emptySet<Edge>(), outer["symbol"])
        // Bottom row.
        assertEquals(setOf(Edge.LEFT, Edge.BOTTOM), outer[key("한/영").id])
        assertEquals(setOf(Edge.BOTTOM), outer[key("스페이스").id])
        assertEquals(setOf(Edge.RIGHT, Edge.BOTTOM), outer[cells.getValue(5 to 5).id])
        assertEquals(setOf(Edge.RIGHT), outer["symbol2"])   // beside the symbol key, was an empty cell (issue #208)
        assertEquals(setOf(Edge.LEFT), outer["symbol3"])   // above the mode key, the ⚙ cell (issues #205, #208)
        // No consonant key has an outer edge; ㅋ's LEFT borders the empty cell, not the window.
        for (k in SwipeLayout.KEYS.filter { it.kind == SwipeKeyKind.CONSONANT }) {
            assertEquals(k.label, emptySet<Edge>(), outer[k.id])
        }
        assertTrue(Edge.LEFT !in outer.getValue(key("ㅋ").id))
        assertEquals(setOf(Edge.LEFT), outer[cells.getValue(0 to 3).id])
        // No TOP even though row 0 is the window's top (HLD 10 R2: moves past it still arrive).
        assertTrue(outer.values.none { Edge.TOP in it })
    }

    @Test
    fun `key payloads match the kind`() {
        for (k in SwipeLayout.KEYS) {
            when (k.kind) {
                SwipeKeyKind.CONSONANT -> assertEquals(k.label, k.consonant!!.compat.toString())
                SwipeKeyKind.NUMBER -> assertEquals(k.label, k.digit!!.toString())
                SwipeKeyKind.FUNCTION -> assertNotNull(k.function)
                SwipeKeyKind.SYMBOL, SwipeKeyKind.EMPTY, SwipeKeyKind.CHEONJIIN -> Unit
            }
        }
        assertEquals(setOf(FunctionKey.BACKSPACE, FunctionKey.ENTER, FunctionKey.MODE, FunctionKey.SPACE),
            SwipeLayout.KEYS.mapNotNull { it.function }.toSet())
        assertEquals(listOf("symbol3", "symbol", "symbol2"), SwipeLayout.KEYS.filter { it.kind == SwipeKeyKind.SYMBOL }.map { it.id })
        assertEquals("0123456789".toSet(), SwipeLayout.KEYS.mapNotNull { it.digit }.toSet())
    }

    @Test
    fun `number table matches SWIPE 7 and has no entry toward the screen border`() {
        assertEquals("0123456789".toSet(), NumberKeyTable.MAP.keys)
        assertEquals("@", NumberKeyTable['2'][Dir.UP])
        assertEquals("[", NumberKeyTable['2'][Dir.LEFT])
        assertEquals("]", NumberKeyTable['2'][Dir.RIGHT])
        assertNull(NumberKeyTable['6'][Dir.LEFT])   // issue #208: / moved to the symbol3 key
        assertEquals("\\", NumberKeyTable['7'][Dir.LEFT])
        assertEquals("|", NumberKeyTable['8'][Dir.RIGHT])
        assertEquals("₩", NumberKeyTable['9'][Dir.LEFT])
        assertEquals("…", NumberKeyTable['0'][Dir.RIGHT])
        assertEquals("·", NumberKeyTable['1'][Dir.DOWN])
        // Outer directions are missing exactly where KH-06 marks them; every other direction is set.
        for (k in SwipeLayout.KEYS.filter { it.kind == SwipeKeyKind.NUMBER }) {
            val dirs = NumberKeyTable[k.digit!!].keys
            val outerDirs = geomOf(k).outerEdges.map { it.dir }.toSet()
            val unassigned = if (k.digit == '6') setOf(Dir.LEFT) else emptySet()
            assertEquals("${k.digit}", Dir.entries.toSet() - outerDirs - unassigned, dirs)
        }
        assertTrue(NumberKeyTable['a'].isEmpty())
    }

    @Test
    fun `SL-09 symbol3, symbol and symbol2 tables match SWIPE 8 (issue #208)`() {
        val s3 = SymbolKeyTable.SYMBOL3
        assertEquals("/", s3.tap)
        assertEquals(mapOf(Dir.RIGHT to "=", Dir.UP to "?"), s3.exits)   // ← screen edge, ↓ none
        val s = SymbolKeyTable.SYMBOL
        assertEquals(".", s.tap)
        assertEquals(mapOf(Dir.LEFT to "(", Dir.RIGHT to ")", Dir.UP to ">", Dir.DOWN to "-"), s.exits)
        val s2 = SymbolKeyTable.SYMBOL2
        assertEquals(",", s2.tap)
        assertEquals(mapOf(Dir.LEFT to "\"", Dir.UP to "<", Dir.DOWN to "'"), s2.exits)   // → screen edge
        assertEquals(s3, SymbolKeyTable.of("symbol3"))
        assertEquals(s, SymbolKeyTable.of("symbol"))
        assertEquals(s2, SymbolKeyTable.of("symbol2"))
        // Positions and labels: the tap symbol in the centre (FR-SWP-43a).
        fun at(id: String) = SwipeLayout.KEYS.single { it.id == id }.let { Triple(it.col, it.row, it.label) }
        assertEquals(Triple(0, 4, "/"), at("symbol3"))
        assertEquals(Triple(5, 4, "."), at("symbol"))
        assertEquals(Triple(6, 4, ","), at("symbol2"))
        // No symbol key direction toward the screen border: those never cross (FR-SWP-45).
        for (id in listOf("symbol3", "symbol", "symbol2")) {
            val k = SwipeLayout.KEYS.single { it.id == id }
            assertTrue(id, geomOf(k).outerEdges.none { it.dir in SymbolKeyTable.of(id).exits })
        }
    }

    /**
     * SL-10 (SWIPE 8 "PC 특수 문자 32개가 모두 한 동작", issue #208): every PC special character is one move on the
     * swipe keyboard (a number-key exit or a symbol-key tap/exit), and only ( ) appear twice (9·0 ↑ and the symbol key ← →).
     */
    @Test
    fun `SL-10 the 32 PC special characters are all one move, only parentheses twice`() {
        val pc = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~".map { it.toString() }
        assertEquals(32, pc.toSet().size)
        val moves = ArrayList<String>()
        for (k in SwipeLayout.KEYS) {
            when (k.kind) {
                SwipeKeyKind.NUMBER -> moves += NumberKeyTable[k.digit!!].values
                SwipeKeyKind.SYMBOL -> SymbolKeyTable.of(k.id).let { t -> moves += t.tap; moves += t.exits.values }
                else -> Unit
            }
        }
        val counts = moves.filter { it in pc }.groupingBy { it }.eachCount()
        assertEquals(pc.toSet(), counts.keys)
        assertEquals(mapOf("(" to 2, ")" to 2), counts.filterValues { it > 1 })
    }

    /** LLD-11 SL-08 (SWIPE 9.5 "자리와 크기", issue #213, FSS#37): C at the top of the consonant area half a row down, centred across, two rows high, 0.85 wide. */
    @Test
    fun `SL-08 the consonant place C - portrait, landscape, low rows`() {
        val p = SwipeLayout.consonantBox(360f, 48f)
        assertEquals(180f - 40.8f, p.left, 1e-3f)               // 139.2: centre x = 180 (the ㄸ column's centre), width 81.6
        assertEquals(76f, p.top, 1e-3f)                         // double row top + 4 + half a row (FSS#37)
        assertEquals(172f, p.bottom, 1e-3f)                     // 96 dp high
        assertEquals(180f + 40.8f, p.right, 1e-3f)              // 220.8
        assertEquals(3.5f * 360f / 7, (p.left + p.right) / 2, 1e-3f)   // = the middle column's centre
        val land = SwipeLayout.consonantBox(720f, 48f)          // wider keys, same size, still centred (SWIPE 9.5 가로 화면)
        assertEquals(76f, land.top, 1e-3f)
        assertEquals(96f, land.bottom - land.top, 1e-3f)
        assertEquals(81.6f, land.right - land.left, 1e-3f)
        assertEquals(360f, (land.left + land.right) / 2, 1e-3f)
        val low = SwipeLayout.consonantBox(360f, 40f)           // FR-SET-06: lower rows shrink C with them
        assertEquals(64f, low.top, 1e-3f)                       // 40 + 4 + 20: the half row follows the rows too
        assertEquals(80f, low.bottom - low.top, 1e-3f)
        assertEquals(180f, (low.left + low.right) / 2, 1e-3f)
        // C sits on consonant keys: its corners are inside the double row … the ㅁ row, columns ㅉ … ㄲ.
        val keys = SwipeLayout.KEYS
        fun keyAt(x: Float, y: Float) = keys.single { x >= it.col * 360f / 7 && x < (it.col + it.colSpan) * 360f / 7 && y >= it.row * 48f && y < (it.row + 1) * 48f }
        for ((x, y) in listOf(p.left to p.top, p.right to p.top, p.left to p.bottom - 1f, p.right to p.bottom - 1f)) {
            assertEquals("($x, $y)", SwipeKeyKind.CONSONANT, keyAt(x, y).kind)
        }
    }
}
