package kbd.stroke.layout

import kbd.stroke.geom.Edge
import kbd.stroke.geom.KeyGeom
import kbd.stroke.geom.RectDp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** LLD-11 §5.4 TL-01..TL-03, TL-07..TL-09 and §3 KH-05, plus transcription checks of LLD-05A §3.2, §3.3. */
class TapLayoutsTest {

    private val layouts = listOf(TapLayouts.QWERTY, TapLayouts.DUBEOLSIK)

    private val windowW = 360f
    private val rowH = 48f
    private val windowH = 288f

    private fun TapLayout.key(id: String) = keys.single { it.id == id }

    /** The keys taking [row] (a two-row key in both of its rows, FSS#105) as (x, w) spans, left to right. */
    private fun TapLayout.spans(row: Int): List<Pair<Float, Float>> =
        keys.filter { it.inRow(row) }.map { it.x to it.w }.sortedBy { it.first }

    private fun TapLayout.geoms(sideDp: Float = 0f): Map<String, KeyGeom> = keys.associate {
        it.id to KeyGeom.build(it.id, it.rect(1f, rowH), windowW, windowH, sideDp)
    }

    @Test
    fun `TL-01 every row of both layouts is filled 0 to 360 by its keys without gaps or overlaps, six rows`() {
        for (l in layouts) {
            assertEquals(l.name, 6, l.rows)
            assertEquals(l.name, (0 until 6).toSet(), l.keys.map { it.row }.toSet())
            assertTrue(l.name, l.keys.all { it.rowSpan >= 1 && it.row + it.rowSpan <= l.rows })
            for (row in 0 until l.rows) {
                var x = 0f
                for ((sx, w) in l.spans(row)) {
                    assertEquals("${l.name} row $row", x, sx, 0f)
                    x += w
                }
                assertEquals("${l.name} row $row", 360f, x, 0f)
            }
        }
    }

    @Test
    fun `TL-02 key sizes meet NFR-06 and the default row height is 48 dp`() {
        for (l in layouts) {
            for (k in l.keys) {
                // Edit row function keys are as wide as a letter key, an owner-decided NFR-06 exception (FSS#98, LLD-05A §3.2).
                val min = when {
                    k.kind != TapKeyKind.FUNCTION -> 36f
                    k.row == 0 -> 36f
                    k.function == FunctionKey.ENTER -> 36f   // FSS#105: one unit wide, two rows high (NFR-06 exception)
                    else -> 48f
                }
                assertTrue("${l.name} ${k.id} w=${k.w}", k.w >= min)
                if (k.function == FunctionKey.ENTER) assertEquals("${l.name} enter", 2, k.rowSpan)
            }
            assertEquals(l.name, rowH, windowH / l.rows, 0f)
        }
    }

    @Test
    fun `TL-03 the mode key is the first key of row 5`() {
        for (l in layouts) {
            val first = l.keys.filter { it.row == 5 }.minBy { it.x }
            assertEquals(l.name, FunctionKey.MODE, first.function)
            assertEquals(l.name, 0f, first.x, 0f)
            assertEquals(l.name, 1, l.keys.count { it.function == FunctionKey.MODE })
        }
    }

    @Test
    fun `KH-05 outer edges of the QWERTY layout on a 360x288 window`() {
        // #198: Esc starts and ⌫ ends the edit row. FSS#105: a and z start rows 3 and 4, ↵ (two rows) ends them,
        // `.` ends row 5; `/` in row 4 has no window edge.
        val left = setOf("esc", "1", "q", "a", "z", "mode")
        val right = setOf("backspace", "0", "p", "enter", "period")
        for (l in layouts) {
            for ((id, g) in l.geoms()) {
                val expected = buildSet {
                    if (id in left) add(Edge.LEFT)
                    if (id in right) add(Edge.RIGHT)
                    if (l.key(id).inRow(5)) add(Edge.BOTTOM)
                }
                assertEquals("${l.name} $id", expected, g.outerEdges)
            }
            val geoms = l.geoms()
            assertTrue(Edge.BOTTOM !in geoms.getValue("slash").outerEdges)   // FSS#105: `,` is under it now
            assertTrue(Edge.BOTTOM !in geoms.getValue("enter").outerEdges)   // its bottom is the top of `.`
            assertTrue(geoms.values.filter { it.rect.top == 0f }.all { Edge.TOP !in it.outerEdges })
        }
    }

    @Test
    fun `TL-08 with side margins no edge of the two-row enter is a window edge (FSS#105)`() {
        for (l in layouts) {
            val g = l.geoms(sideDp = 16f).getValue("enter")
            assertEquals(l.name, RectDp(324f, 3 * rowH, 360f, 5 * rowH), g.rect)
            assertEquals(l.name, emptySet<Edge>(), g.outerEdges)
            assertEquals(l.name, emptySet<Edge>(), g.windowEdges)
            // The ends of rows 3, 4 and 5 are ordinary edges inside the margins too.
            for (id in listOf("a", "z", "period", "slash")) {
                assertTrue("${l.name} $id", l.geoms(sideDp = 16f).getValue(id).outerEdges.none { it == Edge.LEFT || it == Edge.RIGHT })
            }
        }
    }

    @Test
    fun `QWERTY rows and widths follow LLD-05A 3_2`() {
        val l = TapLayouts.QWERTY
        fun ids(row: Int) = l.keys.filter { it.row == row }.sortedBy { it.x }.map { it.id }
        val edit = listOf("esc", "ctrl", "alt", "tab", "left", "up", "down", "right", "delete", "backspace")   // FSS#98
        assertEquals(edit, ids(0))
        assertTrue(ids(0).all { l.key(it).w == 36f })
        assertEquals(324f, l.key("backspace").x, 0f)
        assertEquals(288f, l.key("delete").x, 0f)
        assertEquals(listOf("Esc", "Ctrl", "Alt", "⇥", "←", "↑", "↓", "→", "Del", "⌫"), edit.map { l.key(it).label })
        assertEquals("1234567890".map { it.toString() }, ids(1))
        assertEquals("qwertyuiop".map { it.toString() }, ids(2))
        assertEquals("asdfghjkl".map { it.toString() } + "enter", ids(3))
        assertEquals("zxcvbnm".map { it.toString() } + listOf("minus", "slash"), ids(4))
        assertEquals(listOf("mode", "space", "comma", "period"), ids(5))
        assertEquals(listOf(36f, 36f, 36f, 72f, 216f, 36f, 36f),
            listOf("minus", "slash", "enter", "mode", "space", "comma", "period").map { l.key(it).w })
        // FSS#105: rows 3 and 4 from x 0, `=` 252, `/` 288, ↵ 324; row 5 72 · 216 · 36 · 36.
        assertEquals(List(9) { it * 36f } + 324f, ids(3).map { l.key(it).x })
        assertEquals(List(9) { it * 36f }, ids(4).map { l.key(it).x })
        assertEquals(252f, l.key("minus").x, 0f)
        assertEquals(288f, l.key("slash").x, 0f)
        assertEquals(listOf(0f, 72f, 288f, 324f), ids(5).map { l.key(it).x })
        assertTrue(l.keys.none { it.id == "quote" })
        assertEquals(
            listOf(FunctionKey.ESC, FunctionKey.CTRL, FunctionKey.ALT, FunctionKey.TAB, FunctionKey.ARROW_LEFT, FunctionKey.ARROW_UP,
                FunctionKey.ARROW_DOWN, FunctionKey.ARROW_RIGHT, FunctionKey.DELETE, FunctionKey.BACKSPACE),
            edit.map { l.key(it).function },
        )
        assertEquals(TapKeyKind.SYMBOL, l.key("minus").kind)
        assertTrue(l.keys.filter { it.row in 2..4 && it.id.length == 1 }.all { it.kind == TapKeyKind.QWERTY_LETTER })
    }

    @Test
    fun `TL-06 the edit row is ten keys of one unit in both layouts, Ctrl and Alt right of Esc (FSS#98)`() {
        for (l in layouts) {
            val row = l.keys.filter { it.row == 0 }.sortedBy { it.x }
            assertEquals(
                l.name,
                listOf(FunctionKey.ESC, FunctionKey.CTRL, FunctionKey.ALT, FunctionKey.TAB, FunctionKey.ARROW_LEFT, FunctionKey.ARROW_UP,
                    FunctionKey.ARROW_DOWN, FunctionKey.ARROW_RIGHT, FunctionKey.DELETE, FunctionKey.BACKSPACE),
                row.map { it.function },
            )
            assertEquals(l.name, List(10) { it * 36f }, row.map { it.x })
            assertTrue(l.name, row.all { it.w == 36f && it.kind == TapKeyKind.FUNCTION })
            assertEquals(l.name, 360f, row.sumOf { it.w.toDouble() }.toFloat(), 0f)
            // As wide as the number row keys under them.
            assertEquals(l.name, l.keys.filter { it.row == 1 }.sortedBy { it.x }.map { it.x to it.w }, row.map { it.x to it.w })
        }
    }

    @Test
    fun `DUBEOLSIK letters, jamo and shift jamo follow LLD-05A 3_2`() {
        val d = TapLayouts.DUBEOLSIK
        val q = TapLayouts.QWERTY
        val jamo = "ㅂㅈㄷㄱㅅㅛㅕㅑㅐㅔ" + "ㅁㄴㅇㄹㅎㅗㅓㅏㅣ" + "ㅋㅌㅊㅍㅠㅜㅡ"
        val letters = d.keys.filter { it.kind == TapKeyKind.DUBEOLSIK_LETTER }.sortedWith(compareBy({ it.row }, { it.x }))
        assertEquals(jamo.toList(), letters.map { it.jamo })
        assertEquals(jamo.map { it.toString() }, letters.map { it.label })
        val shift = mapOf('ㅂ' to 'ㅃ', 'ㅈ' to 'ㅉ', 'ㄷ' to 'ㄸ', 'ㄱ' to 'ㄲ', 'ㅅ' to 'ㅆ', 'ㅐ' to 'ㅒ', 'ㅔ' to 'ㅖ')
        letters.forEach { assertEquals("${it.jamo}", shift[it.jamo], it.shiftJamo) }
        // Same positions and ids as QWERTY; non-letter keys are identical.
        assertEquals(q.keys.map { Triple(it.id, it.row, it.x) }, d.keys.map { Triple(it.id, it.row, it.x) })
        assertEquals(q.keys.map { it.w to it.rowSpan }, d.keys.map { it.w to it.rowSpan })
        assertEquals(q.keys.filter { it.row !in 2..4 || it.id.length > 1 }, d.keys.filter { it.row !in 2..4 || it.id.length > 1 })
        assertTrue(q.keys.all { it.jamo == null && it.shiftJamo == null })
    }

    @Test
    fun `TL-07 enter is one key two rows high at x 324, listed right after l, the only multi-row key (FSS#105)`() {
        for (l in layouts) {
            val enters = l.keys.filter { it.function == FunctionKey.ENTER }
            assertEquals(l.name, 1, enters.size)
            val e = enters.single()
            assertEquals(l.name, listOf(3, 2), listOf(e.row, e.rowSpan))
            assertEquals(l.name, 324f to 36f, e.x to e.w)
            assertEquals(l.name, "↵", e.label)
            assertTrue(l.name, l.keys.filter { it !== e }.all { it.rowSpan == 1 })
            assertEquals(l.name, "l", l.keys[l.keys.indexOf(e) - 1].id)
            assertEquals(l.name, "z", l.keys[l.keys.indexOf(e) + 1].id)
            assertEquals(l.name, RectDp(324f, 3 * rowH, 360f, 5 * rowH), e.rect(1f, rowH))
            assertTrue(l.name, e.inRow(3) && e.inRow(4) && !e.inRow(2) && !e.inRow(5))
            assertEquals(l.name, 1, l.keys.count { it.id == "slash" })
        }
    }

    @Test
    fun `symbol assignments follow LLD-05A 3_3 in both layouts`() {
        for (l in layouts) {
            assertEquals(KeyAssign("=", up = "_", left = "-", down = "+"), l.key("minus").assign)   // FSS#62
            assertEquals("=", l.key("minus").label)
            assertEquals(KeyAssign("1", up = "!", right = "`", down = "~"), l.key("1").assign)
            "234567890".zip("@#$%^&*()").forEach { (n, s) ->
                assertEquals(KeyAssign(n.toString(), up = s.toString()), l.key(n.toString()).assign)
            }
            assertEquals(KeyAssign(",", up = "<", left = "'", right = "\""), l.key("comma").assign)
            assertEquals(KeyAssign(".", up = ">"), l.key("period").assign)
            assertEquals(KeyAssign("/", up = "?"), l.key("slash").assign)
            assertEquals(TapKeyKind.SYMBOL, l.key("slash").kind)
            // Letter keys: left, right and down only, never up; i and o carry brackets left/right (#198).
            mapOf(
                "i" to Triple("[", "]", null), "o" to Triple("{", "}", null),
                "p" to Triple("\\", null, "|"), "l" to Triple(";", null, ":"),
            ).forEach { (id, s) ->
                val a = l.key(id).assign!!
                assertEquals("${l.name} $id", s, Triple(a.left, a.right, a.down))
                assertNull(a.up)
            }
            l.keys.filter { it.kind == TapKeyKind.QWERTY_LETTER || it.kind == TapKeyKind.DUBEOLSIK_LETTER }
                .filter { it.id !in setOf("i", "o", "p", "l") }
                .forEach { assertNull("${l.name} ${it.id}", it.assign) }
            l.keys.filter { it.kind == TapKeyKind.FUNCTION }.forEach { assertNull(it.assign) }
        }
    }

    @Test
    fun `TL-05 every PC special character is assigned to exactly one key and direction (#198)`() {
        val pc = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~"
        assertEquals(32, pc.length)
        for (l in layouts) {
            val symbols = l.keys.mapNotNull { it.assign }
                .flatMap { listOf(it.up, it.left, it.right, it.down) + if (it.tap.length == 1 && it.tap[0] in pc) listOf(it.tap) else emptyList() }
                .filterNotNull()
            assertEquals(l.name, pc.map { it.toString() }.sorted(), symbols.sorted())
        }
    }

    @Test
    fun `TL-09 no Dubeolsik letter key has two assignments in one direction, QWERTY letters have no direction jamo (FSS#104)`() {
        // Shift jamo (↑ only), direction jamo (compound finals, paired vowels) and assigned symbols: at most one per
        // key and direction, so the resolver's order never changes a result (HLD 5A).
        val letters = TapLayouts.DUBEOLSIK.keys.filter { it.kind == TapKeyKind.DUBEOLSIK_LETTER }
        assertEquals(26, letters.size)
        for (k in letters) {
            for (d in kbd.stroke.geom.Dir.entries) {
                val symbol = when (d) {
                    kbd.stroke.geom.Dir.UP -> k.assign?.up
                    kbd.stroke.geom.Dir.LEFT -> k.assign?.left
                    kbd.stroke.geom.Dir.RIGHT -> k.assign?.right
                    kbd.stroke.geom.Dir.DOWN -> k.assign?.down
                }
                val placed = listOfNotNull(if (d == kbd.stroke.geom.Dir.UP) k.shiftJamo else null, k.dirJamo[d], symbol)
                assertTrue("${k.id} $d $placed", placed.size <= 1)
            }
        }
        // 11 compound finals and 4 paired vowels, each once.
        assertEquals("ㄳㄵㄶㄺㄻㄼㄽㄾㄿㅀㅄㅑㅕㅛㅠ".toList(), letters.flatMap { it.dirJamo.values }.sorted())
        assertTrue(TapLayouts.QWERTY.keys.all { it.dirJamo.isEmpty() })
    }
}
