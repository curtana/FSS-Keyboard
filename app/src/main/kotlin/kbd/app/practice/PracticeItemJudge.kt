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
     * Swipe and graffiti items: action [index] (0-based) of the try left [text] in the editor; [error] = it gave the
     * error vibration (an ignored stroke or element, dropped strokes, a failed recognition — SWIPE 10.2 오류 진동).
     * ✗ when it vibrated, when the item has no such action, or when [text] is not that action's step.
     */
    fun step(item: PracticeItem, index: Int, text: String, error: Boolean): Verdict {
        val steps = requireNotNull(item.steps) { "${item.target}: a tap keyboard item is judged by text" }
        if (error || index !in steps.indices || text != steps[index]) return Verdict.WRONG
        return if (index == steps.lastIndex) Verdict.RIGHT else Verdict.PENDING
    }

    /** The last code point of [s], or "" (the input display, FR-TUT-09: one character, large). */
    fun lastChar(s: String): String = if (s.isEmpty()) "" else s.substring(s.offsetByCodePoints(s.length, -1))
}
