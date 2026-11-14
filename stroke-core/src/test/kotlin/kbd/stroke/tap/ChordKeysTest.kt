package kbd.stroke.tap

import kbd.stroke.geom.Dir
import kbd.stroke.geom.Dir.DOWN
import kbd.stroke.geom.Dir.LEFT
import kbd.stroke.geom.Dir.RIGHT
import kbd.stroke.geom.Dir.UP
import kbd.stroke.layout.FunctionKey
import kbd.stroke.layout.TapKeyKind
import kbd.stroke.layout.TapLayouts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * LLD-11 §5.6 CM-01..CM-12: the SRS chord table (FR-TAP-16, LLD-05A §2.4). Key ids are QWERTY positions, the same
 * in the Dubeolsik layout: q = ㅂ, a = ㅁ, f = ㄹ, l = ㅣ.
 */
class ChordKeysTest {

    private val tap = TapGesture.Tap
    private fun exit(d: Dir) = TapGesture.Exit(d)
    private fun rub(d: Dir) = TapGesture.Rub(d)
    private fun back(d: Dir) = TapGesture.ExitReturn(d)

    private fun plain(k: ChordKey) = ChordStroke(k, shift = false)
    private fun shift(k: ChordKey) = ChordStroke(k, shift = true)

    private val letterKeys = ChordKey.entries.subList(ChordKey.A.ordinal, ChordKey.Z.ordinal + 1)
    private val digitKeys = ChordKey.entries.subList(ChordKey.D0.ordinal, ChordKey.D9.ordinal + 1)

    @Test
    fun `CM-01 the character table is the 94 printable ASCII characters`() {
        assertEquals(('!'..'~').toSet(), ChordKeys.CHARS.keys)
        assertEquals(26, letterKeys.size)
        assertEquals(10, digitKeys.size)
        assertNull(ChordKeys.ofChar(' '))
        assertNull(ChordKeys.ofChar('ㄱ'))
        assertNull(ChordKeys.ofChar('ᵃ'))
    }

    @Test
    fun `CM-02 small letters are the letter keys, capitals add Shift`() {
        for (i in 0 until 26) {
            assertEquals(plain(letterKeys[i]), ChordKeys.ofChar('a' + i))
            assertEquals(shift(letterKeys[i]), ChordKeys.ofChar('A' + i))
            assertEquals(('A' + i).toString(), letterKeys[i].name)
        }
    }

    @Test
    fun `CM-03 digits are the digit keys, their Shift symbols add Shift`() {
        ")!@#$%^&*(".forEachIndexed { i, s ->
            assertEquals(plain(digitKeys[i]), ChordKeys.ofChar('0' + i))
            assertEquals(shift(digitKeys[i]), ChordKeys.ofChar(s))
            assertEquals("D$i", digitKeys[i].name)
        }
    }

    @Test
    fun `CM-04 the 22 other symbols are the US layout keys`() {
        val expected = mapOf(
            '`' to plain(ChordKey.GRAVE), '~' to shift(ChordKey.GRAVE),
            '-' to plain(ChordKey.MINUS), '_' to shift(ChordKey.MINUS),
            '=' to plain(ChordKey.EQUALS), '+' to shift(ChordKey.EQUALS),
            '[' to plain(ChordKey.LEFT_BRACKET), '{' to shift(ChordKey.LEFT_BRACKET),
            ']' to plain(ChordKey.RIGHT_BRACKET), '}' to shift(ChordKey.RIGHT_BRACKET),
            '\\' to plain(ChordKey.BACKSLASH), '|' to shift(ChordKey.BACKSLASH),
            ';' to plain(ChordKey.SEMICOLON), ':' to shift(ChordKey.SEMICOLON),
            '\'' to plain(ChordKey.APOSTROPHE), '"' to shift(ChordKey.APOSTROPHE),
            ',' to plain(ChordKey.COMMA), '<' to shift(ChordKey.COMMA),
            '.' to plain(ChordKey.PERIOD), '>' to shift(ChordKey.PERIOD),
            '/' to plain(ChordKey.SLASH), '?' to shift(ChordKey.SLASH),
        )
        for ((c, s) in expected) assertEquals("$c", s, ChordKeys.ofChar(c))
        // Shift is carried exactly by the capitals and the SRS list (FR-TAP-16 ②).
        val shifted = ChordKeys.CHARS.filterValues { it.shift }.keys
        assertEquals((('A'..'Z') + "!@#$%^&*()~_+{}|:\"<>?".toList()).toSet(), shifted)
    }

    @Test
    fun `CM-05 letter keys - tap, unassigned directions, up = Shift, rubs`() {
        for (id in "abcdefghijklmnopqrstuvwxyz") {
            val k = ChordKey.valueOf(id.uppercase())
            assertEquals("$id tap", plain(k), ChordKeys.ofTap(id.toString(), tap))
            assertEquals("$id up", shift(k), ChordKeys.ofTap(id.toString(), exit(UP)))
            assertEquals("$id up and back", shift(k), ChordKeys.ofTap(id.toString(), back(UP)))
            // Super- and subscripts are not on a PC keyboard: the letter key, no Shift (also where Unicode has none, b ↓).
            assertEquals("$id rub up", plain(k), ChordKeys.ofTap(id.toString(), rub(UP)))
            assertEquals("$id rub down", plain(k), ChordKeys.ofTap(id.toString(), rub(DOWN)))
        }
        for (d in listOf(LEFT, RIGHT, DOWN)) {
            assertEquals(plain(ChordKey.A), ChordKeys.ofTap("a", exit(d)))
            assertEquals(plain(ChordKey.A), ChordKeys.ofTap("a", back(d)))
        }
        assertEquals(plain(ChordKey.A), ChordKeys.ofTap("a", rub(LEFT)))
    }

    @Test
    fun `CM-06 symbols assigned to letter keys`() {
        assertEquals(plain(ChordKey.LEFT_BRACKET), ChordKeys.ofTap("i", exit(LEFT)))
        assertEquals(plain(ChordKey.RIGHT_BRACKET), ChordKeys.ofTap("i", exit(RIGHT)))
        assertEquals(shift(ChordKey.LEFT_BRACKET), ChordKeys.ofTap("o", exit(LEFT)))
        assertEquals(shift(ChordKey.RIGHT_BRACKET), ChordKeys.ofTap("o", exit(RIGHT)))
        assertEquals(plain(ChordKey.BACKSLASH), ChordKeys.ofTap("p", exit(LEFT)))
        assertEquals(shift(ChordKey.BACKSLASH), ChordKeys.ofTap("p", exit(DOWN)))
        assertEquals(plain(ChordKey.SEMICOLON), ChordKeys.ofTap("l", exit(LEFT)))
        assertEquals(shift(ChordKey.SEMICOLON), ChordKeys.ofTap("l", exit(DOWN)))
        // A left or right rub and an exit-and-return are that exit (the tap action table).
        assertEquals(plain(ChordKey.LEFT_BRACKET), ChordKeys.ofTap("i", rub(LEFT)))
        assertEquals(plain(ChordKey.SEMICOLON), ChordKeys.ofTap("l", back(LEFT)))
        assertEquals(plain(ChordKey.I), ChordKeys.ofTap("i", exit(DOWN)))
    }

    @Test
    fun `CM-07 number row`() {
        "1234567890".forEach { n ->
            val k = ChordKey.valueOf("D$n")
            assertEquals("$n tap", plain(k), ChordKeys.ofTap(n.toString(), tap))
            assertEquals("$n up", shift(k), ChordKeys.ofTap(n.toString(), exit(UP)))
            assertEquals("$n rub up", plain(k), ChordKeys.ofTap(n.toString(), rub(UP)))
            assertEquals("$n rub down", plain(k), ChordKeys.ofTap(n.toString(), rub(DOWN)))
        }
        assertEquals(plain(ChordKey.GRAVE), ChordKeys.ofTap("1", exit(RIGHT)))
        assertEquals(shift(ChordKey.GRAVE), ChordKeys.ofTap("1", exit(DOWN)))
        assertEquals(plain(ChordKey.D2), ChordKeys.ofTap("2", exit(LEFT)))
        assertEquals(plain(ChordKey.D2), ChordKeys.ofTap("2", exit(DOWN)))
    }

    @Test
    fun `CM-08 symbol keys`() {
        assertEquals(plain(ChordKey.EQUALS), ChordKeys.ofTap("minus", tap))
        assertEquals(shift(ChordKey.MINUS), ChordKeys.ofTap("minus", exit(UP)))
        assertEquals(plain(ChordKey.MINUS), ChordKeys.ofTap("minus", exit(LEFT)))
        assertEquals(shift(ChordKey.EQUALS), ChordKeys.ofTap("minus", exit(DOWN)))
        assertEquals(plain(ChordKey.EQUALS), ChordKeys.ofTap("minus", exit(RIGHT)))
        assertEquals(plain(ChordKey.COMMA), ChordKeys.ofTap("comma", tap))
        assertEquals(shift(ChordKey.COMMA), ChordKeys.ofTap("comma", exit(UP)))
        assertEquals(plain(ChordKey.APOSTROPHE), ChordKeys.ofTap("comma", exit(LEFT)))
        assertEquals(shift(ChordKey.APOSTROPHE), ChordKeys.ofTap("comma", exit(RIGHT)))
        assertEquals(plain(ChordKey.PERIOD), ChordKeys.ofTap("period", tap))
        assertEquals(shift(ChordKey.PERIOD), ChordKeys.ofTap("period", exit(UP)))
        assertEquals(plain(ChordKey.SLASH), ChordKeys.ofTap("slash", tap))
        assertEquals(shift(ChordKey.SLASH), ChordKeys.ofTap("slash", exit(UP)))
        assertEquals(plain(ChordKey.COMMA), ChordKeys.ofTap("comma", rub(UP)))
        assertEquals(plain(ChordKey.PERIOD), ChordKeys.ofTap("period", exit(LEFT)))
    }

    @Test
    fun `CM-09 a Dubeolsik key sends the English key of its place`() {
        // The key id is all the function takes, so every Dubeolsik key is covered by its QWERTY twin (CM-05..CM-08).
        val q = TapLayouts.QWERTY.keys
        val d = TapLayouts.DUBEOLSIK.keys
        assertEquals(q.map { it.id }, d.map { it.id })
        fun dub(jamo: Char) = d.single { it.jamo == jamo }.id
        assertEquals(plain(ChordKey.Q), ChordKeys.ofTap(dub('ㅂ'), tap))
        assertEquals(shift(ChordKey.Q), ChordKeys.ofTap(dub('ㅂ'), exit(UP)))             // ㅃ
        assertEquals(plain(ChordKey.Q), ChordKeys.ofTap(dub('ㅂ'), exit(DOWN)))           // ㅄ: QWERTY q ↓ has nothing
        assertEquals(plain(ChordKey.F), ChordKeys.ofTap(dub('ㄹ'), exit(LEFT)))           // ㄺ: QWERTY f ← has nothing
        assertEquals(shift(ChordKey.F), ChordKeys.ofTap(dub('ㄹ'), exit(UP)))             // ㅀ
        assertEquals(shift(ChordKey.M), ChordKeys.ofTap("m", exit(UP)))                   // ㅡ ↑ types ㅡ, still Shift + M
        assertEquals(shift(ChordKey.A), ChordKeys.ofTap(dub('ㅁ'), exit(UP)))             // ㅁ ↑ types ㅁ, still Shift + A
        assertEquals(plain(ChordKey.SEMICOLON), ChordKeys.ofTap(dub('ㅣ'), exit(LEFT)))
        assertEquals(plain(ChordKey.A), ChordKeys.ofTap(dub('ㅁ'), rub(UP)))
    }

    @Test
    fun `CM-10 every gesture on every character key of the QWERTY layout has a chord key`() {
        val gestures = listOf(tap) + Dir.entries.flatMap { listOf(exit(it), back(it), rub(it)) }
        val keys = TapLayouts.QWERTY.keys.filter { it.kind != TapKeyKind.FUNCTION }
        assertEquals(26 + 10 + 4, keys.size)
        val seen = HashSet<ChordStroke>()
        for (k in keys) {
            for (g in gestures) {
                val s = ChordKeys.ofTap(k.id, g)   // throws when the output has no PC key
                seen += s
                // The same rule written out: the gesture's QWERTY character, except that an up or down rub is the tap.
                val plainGesture = if (g is TapGesture.Rub && (g.dir == UP || g.dir == DOWN)) tap else g
                val text = TapKeyResolver.resolve(k, plainGesture).text!!
                assertEquals("${k.id} $g", ChordKeys.ofChar(text.single()), s)
            }
        }
        // Every character of the table can be sent from the keyboard: 94 strokes, 47 keys with and without Shift.
        assertEquals(ChordKeys.CHARS.values.toSet(), seen)
        assertEquals(94, seen.size)
    }

    @Test
    fun `CM-11 function keys - one key each, no Shift, none for the mode key and the modifiers`() {
        val expected = mapOf(
            FunctionKey.SPACE to ChordKey.SPACE, FunctionKey.ENTER to ChordKey.ENTER, FunctionKey.TAB to ChordKey.TAB,
            FunctionKey.ARROW_LEFT to ChordKey.DPAD_LEFT, FunctionKey.ARROW_UP to ChordKey.DPAD_UP,
            FunctionKey.ARROW_DOWN to ChordKey.DPAD_DOWN, FunctionKey.ARROW_RIGHT to ChordKey.DPAD_RIGHT,
            FunctionKey.ESC to ChordKey.ESCAPE, FunctionKey.DELETE to ChordKey.FORWARD_DEL, FunctionKey.BACKSPACE to ChordKey.DEL,
            FunctionKey.MODE to null, FunctionKey.CTRL to null, FunctionKey.ALT to null,
        )
        assertEquals(FunctionKey.entries.toSet(), expected.keys)
        for ((f, k) in expected) assertEquals("$f", k, ChordKeys.ofFunction(f))
        // Function keys of the tap layouts that are chord targets all have a key.
        val onLayout = TapLayouts.QWERTY.keys.mapNotNull { it.function }.toSet()
        assertTrue(onLayout.containsAll(expected.filterValues { it != null }.keys))
    }

    @Test
    fun `CM-12 a function key id or an unknown id is a caller error`() {
        for (id in listOf("esc", "ctrl", "space", "nope")) {
            val thrown = runCatching { ChordKeys.ofTap(id, tap) }.exceptionOrNull()
            assertTrue(id, thrown is IllegalArgumentException)
        }
        assertEquals(listOf(ChordModifier.CTRL, ChordModifier.ALT, ChordModifier.SHIFT), ChordModifier.entries.toList())
    }
}
