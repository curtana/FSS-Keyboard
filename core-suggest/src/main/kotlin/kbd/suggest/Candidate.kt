package kbd.suggest

/** One suggestion cell (LLD-12 §6). [spoken] = the screen-reader name. */
data class Candidate(val text: String, val spoken: String = text, val kind: Kind = Kind.WORD) {
    /** TYPED = the cell that shows the typed letters as they are (SRS FR-SUG-05 ④). */
    enum class Kind { WORD, TYPED }
}
