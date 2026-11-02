package kbd.app.practice

/** Result of judging an item so far (FR-TUT-12, SWIPE 10.2). */
enum class Verdict {
    /** Everything so far matches; more is needed. */
    PENDING,

    /** The item is typed (✓). */
    RIGHT,

    /** The last action does not match (✗). */
    WRONG,
}

/**
 * Item judgement of the input practice (HLD 7.1, SWIPE 10.2, LLD-07 §2.4). Pure Kotlin: the practice screen passes
 * what the real engine left in the practice editor; nothing is measured.
 */
object PracticeItemJudge {

    /**
     * Tap keyboard items (owner decision A, FSS#17): [text] is the item's text after ⌫. It is ✓ when it is the target,
     * pending while it is a beginning of the target (empty included: everything erased), else ✗ — the item is not
     * cleared, so ⌫ back to a beginning takes the ✗ back.
     */
    fun text(item: PracticeItem, text: String): Verdict = when {
        text == item.target -> Verdict.RIGHT
        item.target.startsWith(text) -> Verdict.PENDING
        else -> Verdict.WRONG
    }

    /**
     * Swipe and graffiti items (owner decision 2026-11-02, FSS#43): judged by what is committed only — a syllable still
     * composing is not finished (기 may still become 가, and 가 becomes 각 on its way to 가가). [committed] is the try's
     * committed text (what still composes is left out); its spaces are dropped (the space is the closing action, PM
     * decision A). [error] = the action gave the error vibration (an ignored stroke or element, dropped strokes, a
     * failed recognition — SWIPE 10.2 오류 진동). ✗ when it vibrated or the committed text is no beginning of the
     * target, ✓ when it is the target (closed by the space, or by the next syllable's consonant), else pending.
     */
    fun commit(item: PracticeItem, committed: String, error: Boolean): Verdict {
        val done = committed.replace(" ", "")
        return when {
            error || !item.target.startsWith(done) -> Verdict.WRONG
            done == item.target -> Verdict.RIGHT
            else -> Verdict.PENDING
        }
    }

    /** The last code point of [s], or "" (the input display, FR-TUT-09: one character, large). */
    fun lastChar(s: String): String = if (s.isEmpty()) "" else s.substring(s.offsetByCodePoints(s.length, -1))
}
