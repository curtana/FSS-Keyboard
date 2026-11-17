package kbd.stroke.tap

import kbd.stroke.geom.Diag
import kbd.stroke.geom.Dir
import kbd.stroke.geom.WAYS
import kbd.stroke.geom.Way
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
     * A diagonal ([Diag], FSS#109) is there only where the key has a diagonal assignment, and the view draws it in
     * that corner (FR-TAP-02e ⑥); a direction that types the tap, like ㄹ ↘, is not there.
     */
    fun edgeHints(key: TapKeyDef): Map<Way, String> {
        if (key.kind == TapKeyKind.FUNCTION) return emptyMap()
        fun shown(o: TapOutput): String? = o.text ?: o.jamo?.toString()
        val tap = shown(resolve(key, TapGesture.Tap))
        val out = LinkedHashMap<Way, String>()
        for (d in WAYS) shown(resolve(key, TapGesture.Exit(d)))?.takeIf { it != tap }?.let { out[d] = it }
        return out
    }


    fun resolve(key: TapKeyDef, g: TapGesture): TapOutput {
        val a = key.assign
        val n = normalize(g)
        if (n is TapGesture.Exit && n.dir is Diag) return diagonal(key, n.dir)
        return when (key.kind) {
            TapKeyKind.QWERTY_LETTER -> qwerty(key.label, a, n)
            TapKeyKind.DUBEOLSIK_LETTER -> dubeolsik(key, a, n)
            TapKeyKind.NUMBER_ROW, TapKeyKind.SYMBOL -> symbol(requireNotNull(a) { key.id }, n)
            TapKeyKind.FUNCTION -> throw IllegalArgumentException("function key ${key.id} has no tap output")
        }
    }

    /**
     * Step 1: exit-and-return is an exit in that direction (table row 8), and a left/right rub
     * is an exit in that direction (row 7). A diagonal rub (only on an 8-way key) is the diagonal's exit too: the
     * first exit's direction decides, as on every key (SRS FR-TAP-02e ④, FSS#109). Only Tap, Exit and Rub(UP/DOWN)
     * remain.
     */
    private fun normalize(g: TapGesture): TapGesture = when {
        g is TapGesture.ExitReturn -> TapGesture.Exit(g.dir)
        g is TapGesture.Rub && g.dir != Dir.UP && g.dir != Dir.DOWN -> TapGesture.Exit(g.dir)
        else -> g
    }

    /**
     * A diagonal exit (FSS#109): the key's diagonal jamo, else the tap's output — a direction with nothing is the tap
     * (SRS FR-TAP-02c ④: ㄹ ↘ = ㄹ, no error vibration). Keys without diagonals are never judged diagonally, but a
     * caller passing one (Ctrl·Alt with QWERTY f for ㄹ, FR-TAP-02e ⑧) gets the tap as well.
     */
    private fun diagonal(key: TapKeyDef, d: Diag): TapOutput =
        key.dirJamo[d]?.let(::jamo) ?: resolve(key, TapGesture.Tap)

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
                Dir.UP -> key.shiftJamo?.let(::jamo) ?: key.dirJamo[Dir.UP]?.let(::jamo)   // ㄴ ↑ ㄵ, ㄹ ↑ ㄺ, ㅗ ↑ ㅛ; ㄱ·ㅂ·ㅅ keep ㄲ·ㅃ·ㅆ
                    ?: syllable(key, base, Dir.UP)
                else -> key.dirJamo[g.dir]?.let(::jamo) ?: assigned(a, g.dir)?.let(::text)   // ㄹ ← ㄻ, ㄱ → ㄳ, ㅓ ← ㅕ
                    ?: syllable(key, base, g.dir)
            }
            // Tap and up/down rubs: no super/subscripts for jamo, and no compound final or paired vowel either (FSS#97, FSS#104).
            else -> jamo(base)
        }
    }

    /**
     * A Dubeolsik up/down/left/right exit with nothing else assigned: on a consonant key its jamo then the direction's
     * vowel (FSS#122, SRS FR-TAP-02h: ㄱ ↓ = 구, ㅁ ↑ = 모), else the tap (vowel keys, FR-TAP-02c ④). A diagonal never
     * gets here.
     */
    private fun syllable(key: TapKeyDef, base: Char, dir: Way): TapOutput =
        TapOutput(null, base, errorVibe = false, vowel = key.dirVowel[dir])

    private fun symbol(a: KeyAssign, g: TapGesture): TapOutput = when (g) {
        is TapGesture.Exit -> text(if (g.dir == Dir.UP) a.up ?: a.tap else assigned(a, g.dir) ?: a.tap)
        else -> text(a.tap)
    }

    /** The symbol assigned to a down/left/right exit, or null when the direction has none. */
    private fun assigned(a: KeyAssign?, dir: Way): String? = when (dir) {
        Dir.DOWN -> a?.down
        Dir.LEFT -> a?.left
        Dir.RIGHT -> a?.right
        Dir.UP -> a?.up
        is Diag -> null   // symbols have no diagonals
    }

    /** Super/subscript of a letter; the plain letter plus the error vibration when Unicode has none (FR-TAP-01b). */
    private fun script(letter: String, table: Map<Char, String>): TapOutput =
        table[letter.single()]?.let(::text) ?: TapOutput(letter, null, errorVibe = true)

    private fun text(s: String) = TapOutput(s, null, errorVibe = false)

    private fun jamo(c: Char) = TapOutput(null, c, errorVibe = false)
}
