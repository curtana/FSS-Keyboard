package kbd.stroke.layout

import kbd.stroke.geom.Edge
import kbd.stroke.geom.KeyGeom
import kbd.stroke.geom.RectDp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** LLD-11 §5.4 TL-01..TL-03 and §3 KH-05, plus transcription checks of LLD-05A §3.2, §3.3. */
class TapLayoutsTest {

    private val layouts = listOf(TapLayouts.QWERTY, TapLayouts.DUBEOLSIK)

    private val windowW = 360f
    private val rowH = 48f
    private val windowH = 288f

    private fun TapLayout.key(id: String) = keys.single { it.id == id }

    /** Keys and blanks of one row as (x, w) spans, left to right. */
    private fun TapLayout.spans(row: Int): List<Pair<Float, Float>> =
        (keys.filter { it.row == row }.map { it.x to it.w } + blanks.filter { it.row == row }.map { it.x to it.w })
            .sortedBy { it.first }

    private fun TapLayout.geoms(): Map<String, KeyGeom> = keys.associate {
        it.id to KeyGeom.build(it.id, RectDp(it.x, it.row * rowH, it.x + it.w, (it.row + 1) * rowH), windowW, windowH)
    }

    @Test
    fun `TL-01 every row of both layouts sums to 360 dp without gaps, six rows`() {
        for (l in layouts) {
            assertEquals(l.name, 6, l.rows)
            assertEquals(l.name, (0 until 6).toSet(), l.keys.map { it.row }.toSet())
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
                    else -> 48f
                }
                assertTrue("${l.name} ${k.id} w=${k.w}", k.w >= min)
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
        // #198: Esc starts and ⌫ ends the edit row, ↵ ends row 4, / ends row 5.
        val left = setOf("esc", "1", "q", "mode")
        val right = setOf("backspace", "0", "p", "enter", "slash")
        for (l in layouts) {
            for ((id, g) in l.geoms()) {
                val expected = buildSet {
                    if (id in left) add(Edge.LEFT)
                    if (id in right) add(Edge.RIGHT)
                    if (l.key(id).row == 5) add(Edge.BOTTOM)
                }
                assertEquals("${l.name} $id", expected, g.outerEdges)
            }
            val geoms = l.geoms()
            assertTrue(Edge.LEFT !in geoms.getValue("a").outerEdges)
            assertTrue(Edge.RIGHT !in geoms.getValue("l").outerEdges)
            assertTrue(geoms.values.filter { it.rect.top == 0f }.all { Edge.TOP !in it.outerEdges })
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
        assertEquals("asdfghjkl".map { it.toString() }, ids(3))
        assertEquals("zxcvbnm".map { it.toString() } + listOf("minus", "enter"), ids(4))
        assertEquals(listOf("mode", "space", "comma", "period", "slash"), ids(5))
        assertEquals(listOf(36f, 54f, 72f, 180f, 36f, 36f, 36f),
            listOf("minus", "enter", "mode", "space", "comma", "period", "slash").map { l.key(it).w })
        assertEquals(270f, l.key("minus").x, 0f)
        assertEquals(306f, l.key("enter").x, 0f)
        assertEquals(listOf(0f, 72f, 252f, 288f, 324f), ids(5).map { l.key(it).x })   // #198: space 180, , . / 36 each
        assertTrue(l.keys.none { it.id == "quote" })
        assertEquals(
            listOf(
                BlankDef(3, 0f, 18f, "a"), BlankDef(3, 342f, 18f, "l"),
                BlankDef(4, 0f, 18f, "z"),
            ),
            l.blanks,
        )
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
            assertTrue(l.name, l.blanks.none { it.row == 0 })
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
        assertEquals(q.keys.filter { it.row !in 2..4 || it.id.length > 1 }, d.keys.filter { it.row !in 2..4 || it.id.length > 1 })
        assertEquals(q.blanks, d.blanks)
        assertTrue(q.keys.all { it.jamo == null && it.shiftJamo == null })
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
}
