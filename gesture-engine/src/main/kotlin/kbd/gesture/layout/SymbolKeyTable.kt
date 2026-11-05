package kbd.gesture.layout

import kbd.stroke.geom.Dir

/**
 * One swipe symbol key's table (LLD-04 §8.3, SWIPE 8, FR-SWP-43, issue #208): the [tap] symbol and one
 * symbol per exit direction. There are no stages: exit-and-return and rubs equal the exit in that
 * direction (DCR-02). A direction without an entry is the tap: the symbol3 key's ← and the symbol2 key's →
 * are the screen edge (an exit once the stroke reaches the border, FR-SWP-45, issue #237), the symbol3 key's ↓ has
 * no symbol.
 */
data class SymbolKeyTable(val tap: String, val exits: Map<Dir, String>) {

    /** The symbol of an exit towards [dir]; null when the direction has none. */
    operator fun get(dir: Dir): String? = exits[dir]

    companion object {
        const val SYMBOL3_ID = "symbol3"
        const val SYMBOL_ID = "symbol"
        const val SYMBOL2_ID = "symbol2"

        /** Row 4 col 0, above the mode key (the ⚙ cell, issue #205): tap / → = ↑ ?; ← screen edge, ↓ none. */
        val SYMBOL3 = SymbolKeyTable("/", mapOf(Dir.RIGHT to "=", Dir.UP to "?"))

        /** Row 4 col 5, right of ㅍ: tap . ← < → > ↑ ' ↓ _ (owner FSS#75; ↓ _ swapped with 5's -). */
        val SYMBOL = SymbolKeyTable(".", mapOf(Dir.LEFT to "<", Dir.RIGHT to ">", Dir.UP to "'", Dir.DOWN to "_"))

        /** Row 4 col 6, the right end column: tap , ← ♥ ↑ " ↓ ★ (owner FSS#75); → screen edge. */
        val SYMBOL2 = SymbolKeyTable(",", mapOf(Dir.LEFT to "♥", Dir.UP to "\"", Dir.DOWN to "★"))

        /** The table of the symbol key [keyId]. */
        fun of(keyId: String): SymbolKeyTable = when (keyId) {
            SYMBOL3_ID -> SYMBOL3
            SYMBOL2_ID -> SYMBOL2
            else -> SYMBOL
        }
    }
}
