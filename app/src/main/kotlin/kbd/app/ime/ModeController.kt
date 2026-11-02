package kbd.app.ime

/**
 * Pure mode state machine (LLD-08 §5, HLD 8.1). Side effects (commit, reset, save) belong to
 * `InputDispatcher.onModeKey` and `KeyboardService.onStartInputView`.
 *
 * [available] is in mode-key cycle order; [saved] is the last saved mode, used when available (FR-CMN-10, 03b).
 */
class ModeController(private val available: List<InputMode>, saved: InputMode?) {

    var current: InputMode = if (saved != null && saved in available) saved else available[0]
    var previous: InputMode? = null
    var typed = false

    /** Mode just before entering numeric-like fields (the mode to return to). null = outside numeric-like fields. */
    var returnMode: InputMode? = null

    val inNumericField: Boolean get() = returnMode != null

    /** FR-CMN-03a. */
    fun onInput() {
        typed = true
    }

    /** FR-CMN-03e: previous and returnMode are kept. */
    fun onFieldChanged() {
        typed = false
    }

    /** HLD 8.1. Same inside numeric-like fields; returnMode is not changed (FR-CMN-04b). */
    fun onModeKey(): InputMode {
        val prev = previous
        val next = if (typed && prev != null && prev != current) prev else nextInOrder(current)
        if (typed) previous = current
        current = next
        typed = false
        return next
    }

    /** FR-CMN-04, 04b: start in QWERTY; the first return mode is kept across numeric-like fields. */
    fun enterNumericField() {
        if (returnMode == null) returnMode = current
        current = InputMode.QWERTY
    }

    /** FR-CMN-04b: back to the return mode; no-op outside numeric-like fields. */
    fun leaveNumericField() {
        val ret = returnMode ?: return
        current = ret
        returnMode = null
    }

    /** Next mode in [available] after [m], wrapping to the first (FR-CMN-03, 03b). */
    fun nextInOrder(m: InputMode): InputMode = available[(available.indexOf(m) + 1) % available.size]
}
