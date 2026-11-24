package kbd.suggest.en

/** Character classes of the English source (LLD-12 §7.1). Kept here so the module depends on nothing. */
object WordChars {
    /** Word length limit (SRS FR-SUG-04 ④); longer than the longest dictionary word. */
    const val MAX_WORD = 32

    /** The 10 marks that remove the automatic space (SRS FR-SUG-07 ①). */
    const val PUNCT_JOIN = ".,!?;:)]}'"

    /** 'a'..'z', 'A'..'Z'. */
    fun isLetter(c: Char): Boolean = c in 'a'..'z' || c in 'A'..'Z'
}
