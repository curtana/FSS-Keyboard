package kbd.hangul.compose

import kbd.hangul.jamo.Jamo
import kbd.hangul.jamo.JamoTables

/**
 * One composing syllable (LLD-04 §7.1). A compound final is kept as its two consonants
 * [jong1] + [jong2] so that dokkaebibul can split it (FR-HGL-03).
 */
data class Syllable(val cho: Jamo?, val jung: Jamo?, val jong1: Jamo?, val jong2: Jamo?) {

    val isEmpty: Boolean get() = cho == null && jung == null

    val hasJong: Boolean get() = jong1 != null

    /** The final as one jamo: [jong1], or the compound final of [jong1] + [jong2]. */
    val jong: Jamo? get() = if (jong2 == null) jong1 else JamoTables.COMPOUND_JONG[jong1 to jong2]

    /**
     * Text of this syllable (LLD-04 §7.2 `render`): the U+AC00 formula when an initial and a
     * medial are present, the compatibility jamo when only one is, "" when empty.
     */
    fun render(): String {
        val cho = cho
        val jung = jung
        return when {
            cho != null && jung != null -> {
                val jongIndex = jong?.jongIndex ?: 0
                (SYLLABLE_BASE + (cho.choIndex * JUNG_COUNT + jung.jungIndex) * JONG_COUNT + jongIndex)
                    .toChar().toString()
            }
            cho != null -> cho.compat.toString()
            jung != null -> jung.compat.toString()
            else -> ""
        }
    }

    companion object {
        val EMPTY = Syllable(null, null, null, null)

        private const val SYLLABLE_BASE = 0xAC00
        private const val JUNG_COUNT = 21
        private const val JONG_COUNT = 28
    }
}
