package kbd.app.practice

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * The input practice as a plain state machine (LLD-07 §2.3, SWIPE 10.0, 10.2): topic list → explanation → items, each
 * once → end → topic list; [stop] goes back to the list from anywhere. Nothing is measured or stored except the topic's
 * ✓ ([markDone]). Its values are snapshot state, so the screen redraws whatever reads them when they change (FSS#54:
 * a plain value read in a skipped composable kept the old target on screen); it needs no Android.
 */
class PracticeFlow(
    val topics: List<PracticeTopic> = PracticeTopics.ALL,
    private val markDone: (String) -> Unit = {},
) {
    enum class Screen { LIST, INTRO, ITEM, DONE }

    var screen: Screen by mutableStateOf(Screen.LIST)
        private set

    var topic: PracticeTopic? by mutableStateOf(null)
        private set

    /** The item's place in [topic]. */
    var index: Int by mutableIntStateOf(0)
        private set

    val item: PracticeItem? get() = topic?.items?.getOrNull(index)

    /** A new try (item start, or after ✗ off the tap keyboard) bumps this: the screen builds a fresh practice keyboard. */
    var tryNo: Int by mutableIntStateOf(0)
        private set

    /** ✓ / ✗ of the last judged action, shown until the next touch is pressed; null = none (SWIPE 10.2 표시 순서). */
    var verdict: Verdict? by mutableStateOf(null)
        private set

    /** The input display (FR-TUT-09): the last character, large; emptied when the next touch is pressed. */
    var echo: String by mutableStateOf("")
        private set

    /** The how-to line after ✗, kept until the item is right (SWIPE 10.2). */
    var howTo: String? by mutableStateOf(null)
        private set

    /** The extra line after a reversed swipe stroke, with [howTo] (SWIPE 10.2 덧붙임). */
    var hint: String? by mutableStateOf(null)
        private set

    /** Stroke-guide cells highlighted after a graffiti ✗ (SWIPE 10.1 ③). */
    var highlight: List<String> by mutableStateOf(emptyList())
        private set

    /**
     * The item is right and stays on screen with its ✓ for [RIGHT_HOLD_MS], taking no input; then the screen calls
     * [advance] (SWIPE 10.2 표시 순서, FSS#54: the next target must not come unnoticed).
     */
    var holding: Boolean by mutableStateOf(false)
        private set

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
        if (holding) return
        if (item?.board != PracticeBoard.TAP_QWERTY) {
            verdict = null
            echo = ""
        }
    }

    /**
     * An action ended (touch released, the real engine done with it). [text] is the item's text, [composing] its part
     * still composing (at its end; the rest is committed), [display] the input display, [error] = the action vibrated
     * as an error, [reversed] = it went up or left (swipe 덧붙임). Off the tap keyboard only the committed text is
     * judged (FSS#43): the space or the next syllable commits. The screen calls this off the tap keyboard only when the
     * action changed the text or what is committed, or vibrated. Nothing is judged while [holding].
     */
    fun onAction(text: String, display: String, error: Boolean = false, reversed: Boolean = false, composing: String = "") {
        val it = item ?: return
        if (screen != Screen.ITEM || holding) return
        echo = display
        val v = if (it.board == PracticeBoard.TAP_QWERTY) PracticeItemJudge.text(it, text)
        else PracticeItemJudge.commit(it, text.dropLast(composing.length), error)
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
        holding = true
    }

    /** [RIGHT_HOLD_MS] after ✓: the next item with a fresh keyboard, or the end screen after the last one. */
    fun advance() {
        if (!holding) return
        holding = false
        verdict = null
        echo = ""
        index++
        if (index >= (topic?.items?.size ?: 0)) {
            screen = Screen.DONE
            topic?.let { markDone(it.id) }
        }
        newTry()
    }

    private fun newTry() {
        tryNo++
    }

    private fun clearItem() {
        verdict = null
        echo = ""
        howTo = null
        hint = null
        highlight = emptyList()
        holding = false
        newTry()
    }

    companion object {
        /** How long a right item stays on screen with its ✓ before the next one (FSS#54, PM decision A). */
        const val RIGHT_HOLD_MS = 1000L
    }
}
