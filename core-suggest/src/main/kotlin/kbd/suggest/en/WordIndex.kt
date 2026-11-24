package kbd.suggest.en

/** Prefix lookup, kept apart from the data structure (HLD 5B.2, LLD-12 §4). */
interface WordIndex {
    /** Word ids starting with [prefixLower] (lowercase, word characters only), rank order, at most [K]. null if none or empty. */
    fun top(prefixLower: String): IntArray?

    /** The dictionary form of word [id]. */
    fun form(id: Int): String

    val size: Int

    companion object {
        /** Cells (3) + 1, so one dropped exact match still leaves three (HLD 5B.2, SRS FR-SUG-05 ②). */
        const val K = 4
    }
}
