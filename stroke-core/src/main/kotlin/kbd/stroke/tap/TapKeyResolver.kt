package kbd.stroke.tap

import kbd.stroke.geom.Dir
import kbd.stroke.layout.KeyAssign
import kbd.stroke.layout.TapKeyDef
import kbd.stroke.layout.TapKeyKind

/**
 * The SRS tap keyboard action table, 8 rows × 3 key kinds (LLD-05A §2.2). Every gesture on a
 * character key yields exactly one output. Function keys are not in the table: the view handles
 * them before resolving (LLD-05A §4), so passing one is a caller error.
 */
object TapKeyResolver {
    /**
     * What a stroke out of each edge types, where it differs from the tap (owner #198: every stroked character is shown
     * in the middle of its edge): a QWERTY letter's ↑ capital, a Dubeolsik shift jamo, compound final or paired vowel, the
     * assigned symbols. Function keys have none. The view draws these; it never decides anything with them.
     */
    fun edgeHints(key: TapKeyDef): Map<Dir, String> {
        if (key.kind == TapKeyKind.FUNCTION) return emptyMap()
        fun shown(o: TapOutput): String? = o.text ?: o.jamo?.toString()
        val tap = shown(resolve(key, TapGesture.Tap))
        val out = LinkedHashMap<Dir, String>()
        for (d in Dir.entries) shown(resolve(key, TapGesture.Exit(d)))?.takeIf { it != tap }?.let { out[d] = it }
        return out
    }


    fun resolve(key: TapKeyDef, g: TapGesture): TapOutput {
        val a = key.assign
        return when (key.kind) {
            TapKeyKind.QWERTY_LETTER -> qwerty(key.label, a, normalize(g))
            TapKeyKind.DUBEOLSIK_LETTER -> dubeolsik(key, a, normalize(g))
            TapKeyKind.NUMBER_ROW, TapKeyKind.SYMBOL -> symbol(requireNotNull(a) { key.id }, normalize(g))
            TapKeyKind.FUNCTION -> throw IllegalArgumentException("function key ${key.id} has no tap output")
        }
    }

    /**
     * Step 1: exit-and-return is an exit in that direction (table row 8), and a left/right rub
     * is an exit in that direction (row 7). Only Tap, Exit and Rub(UP/DOWN) remain.
     */
    private fun normalize(g: TapGesture): TapGesture = when {
        g is TapGesture.ExitReturn -> TapGesture.Exit(g.dir)
        g is TapGesture.Rub && (g.dir == Dir.LEFT || g.dir == Dir.RIGHT) -> TapGesture.Exit(g.dir)
        else -> g
    }

    private fun qwerty(letter: String, a: KeyAssign?, g: TapGesture): TapOutput = when (g) {
        TapGesture.Tap -> text(letter)
        is TapGesture.Exit -> if (g.dir == Dir.UP) text(letter.uppercase()) else text(assigned(a, g.dir) ?: letter)
        is TapGesture.Rub -> script(letter, if (g.dir == Dir.UP) ScriptTables.SUPER else ScriptTables.SUB)
        is TapGesture.ExitReturn -> error("normalized")
    }

    private fun dubeolsik(key: TapKeyDef, a: KeyAssign?, g: TapGesture): TapOutput {
        val base = requireNotNull(key.jamo) { key.id }
        return when (g) {
            is TapGesture.Exit -> when (g.dir) {
                Dir.UP -> jamo(key.shiftJamo ?: key.dirJamo[Dir.UP] ?: base)   // ㄴ ↑ ㄵ, ㅌ ↑ ㄾ, ㅗ ↑ ㅛ; ㄱ·ㅂ·ㅅ keep ㄲ·ㅃ·ㅆ
                else -> key.dirJamo[g.dir]?.let(::jamo) ?: assigned(a, g.dir)?.let(::text) ?: jamo(base)   // ㄹ ← ㄺ, ㅓ ← ㅕ
            }
            // Tap and up/down rubs: no super/subscripts for jamo, and no compound final or paired vowel either (FSS#97, FSS#104).
            else -> jamo(base)
        }
    }

    private fun symbol(a: KeyAssign, g: TapGesture): TapOutput = when (g) {
        is TapGesture.Exit -> text(if (g.dir == Dir.UP) a.up ?: a.tap else assigned(a, g.dir) ?: a.tap)
        else -> text(a.tap)
    }

    /** The symbol assigned to a down/left/right exit, or null when the direction has none. */
    private fun assigned(a: KeyAssign?, dir: Dir): String? = when (dir) {
        Dir.DOWN -> a?.down
        Dir.LEFT -> a?.left
        Dir.RIGHT -> a?.right
        Dir.UP -> a?.up
    }

    /** Super/subscript of a letter; the plain letter plus the error vibration when Unicode has none (FR-TAP-01b). */
    private fun script(letter: String, table: Map<Char, String>): TapOutput =
        table[letter.single()]?.let(::text) ?: TapOutput(letter, null, errorVibe = true)

    private fun text(s: String) = TapOutput(s, null, errorVibe = false)

    private fun jamo(c: Char) = TapOutput(null, c, errorVibe = false)
}
