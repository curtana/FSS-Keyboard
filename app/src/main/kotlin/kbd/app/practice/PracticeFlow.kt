package kbd.app.practice

/**
 * The input practice as a plain state machine (LLD-07 §2.3, SWIPE 10.0, 10.2): topic list → explanation → items, each
 * once → end → topic list; [stop] goes back to the list from anywhere. The screen redraws from these values after
 * every call. Nothing is measured or stored except the topic's ✓ ([markDone]).
 */
class PracticeFlow(
    val topics: List<PracticeTopic> = PracticeTopics.ALL,
    private val markDone: (String) -> Unit = {},
) {
    enum class Screen { LIST, INTRO, ITEM, DONE }

    var screen: Screen = Screen.LIST
        private set

    var topic: PracticeTopic? = null
        private set

    /** The item's place in [topic]. */
    var index: Int = 0
        private set

    val item: PracticeItem? get() = topic?.items?.getOrNull(index)

    /** A new try (item start, or after ✗ off the tap keyboard) bumps this: the screen builds a fresh practice keyboard. */
    var tryNo: Int = 0
        private set

    /** ✓ / ✗ of the last judged action, shown until the next touch is pressed; null = none (SWIPE 10.2 표시 순서). */
    var verdict: Verdict? = null
        private set

    /** The input display (FR-TUT-09): the last character, large; emptied when the next touch is pressed. */
    var echo: String = ""
        private set

    /** The how-to line after ✗, kept until the item is right (SWIPE 10.2). */
    var howTo: String? = null
        private set

    /** The extra line after a reversed swipe stroke, with [howTo] (SWIPE 10.2 덧붙임). */
    var hint: String? = null
        private set

    /** Stroke-guide cells highlighted after a graffiti ✗ (SWIPE 10.1 ③). */
    var highlight: List<String> = emptyList()
        private set

    /** Actions of the current try judged so far (swipe and graffiti items). */
    private var actions = 0

    fun open(t: PracticeTopic) {
        topic = t
        index = 0
        screen = Screen.INTRO
        clearItem()
    }

    /** [시작] on the explanation. */
    fun start() {
        check(screen == Screen.INTRO)
        screen = Screen.ITEM
        newTry()
    }

    /** [그만] at any time, and [주제 목록으로] on the end screen. */
    fun stop() {
        screen = Screen.LIST
        topic = null
        index = 0
        clearItem()
    }

    /** A touch is pressed on the practice keyboard: the last ✓ / ✗ and the input display go (SWIPE 10.2). */
    fun onPress() {
        if (verdict == Verdict.RIGHT || item?.board != PracticeBoard.TAP_QWERTY) {
            verdict = null
            echo = ""
        }
    }

    /**
     * An action ended (touch released, the real engine done with it). [text] is the item's text, [display] the input
     * display, [error] = the action vibrated as an error, [reversed] = it went up or left (swipe 덧붙임).
     * The screen calls this off the tap keyboard only when the action changed the text or vibrated.
     */
    fun onAction(text: String, display: String, error: Boolean = false, reversed: Boolean = false) {
        val it = item ?: return
        if (screen != Screen.ITEM) return
        echo = display
        val v = if (it.board == PracticeBoard.TAP_QWERTY) PracticeItemJudge.text(it, text)
        else PracticeItemJudge.step(it, actions++, text, error)
        when (v) {
            Verdict.RIGHT -> right()
            Verdict.WRONG -> {
                verdict = Verdict.WRONG
                howTo = it.howTo
                hint = if (reversed && it.board == PracticeBoard.SWIPE) PracticeTopics.REVERSE_HINT else null
                highlight = it.guideCells
                if (it.board != PracticeBoard.TAP_QWERTY) newTry()   // the tap keyboard keeps the text: ⌫ fixes it
            }
            Verdict.PENDING -> if (it.board == PracticeBoard.TAP_QWERTY) verdict = null   // fixed with ⌫: the ✗ goes
        }
    }

    private fun right() {
        verdict = Verdict.RIGHT
        howTo = null
        hint = null
        highlight = emptyList()
        index++
        if (index >= (topic?.items?.size ?: 0)) {
            screen = Screen.DONE
            topic?.let { markDone(it.id) }
        }
        newTry()
    }

    private fun newTry() {
        actions = 0
        tryNo++
    }

    private fun clearItem() {
        verdict = null
        echo = ""
        howTo = null
        hint = null
        highlight = emptyList()
        newTry()
    }
}
