package kbd.app.ime

/**
 * Pure mode state machine (LLD-08 §5, HLD 8.1). Side effects (commit, reset, save) belong to
 * `InputDispatcher.onModeKey` and `KeyboardService`.
 *
 * [available] is in mode-key cycle order; [saved] is the last saved mode, used when available (FR-CMN-10, 03b).
 * [enabled] is the modes turned on in the settings (FR-SET-08); QWERTY and Dubeolsik are always on (FR-CMN-03f ⑤).
 */
class ModeController(private val available: List<InputMode>, saved: InputMode?, enabled: Set<InputMode> = available.toSet()) {

    /** The modes the mode key cycles through; set by [applyEnabled]. */
    var enabled: Set<InputMode> = normalize(enabled)
        private set

    var current: InputMode = start(saved)
    var previous: InputMode? = null
    var typed = false

    /** Mode just before entering numeric-like fields (the mode to return to). null = outside numeric-like fields. */
    var returnMode: InputMode? = null

    val inNumericField: Boolean get() = returnMode != null

    /** The saved mode was off, so [current] was moved on: save it as the last mode (FR-CMN-03f ③, SRS case 7). null = keep. */
    val fixedStart: InputMode? = if (saved != null && saved != current) current else null

    /** FR-CMN-03a. */
    fun onInput() {
        typed = true
    }

    /** FR-CMN-03e: previous and returnMode are kept. */
    fun onFieldChanged() {
        typed = false
    }

    /** HLD 8.1. Same inside numeric-like fields; returnMode is not changed (FR-CMN-04b). previous is always on ([applyEnabled]). */
    fun onModeKey(): InputMode {
        val prev = previous
        val next = if (typed && prev != null && prev != current) prev else nextEnabled(current)
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

    /**
     * FR-CMN-03f ②–④, HLD 8.1: on a settings change and at every `onStartInputView`. A switch of [current] is an
     * automatic one: typed and previous stay (the caller commits first and shows the new view). Returns the mode to
     * save as the last mode, or null.
     */
    fun applyEnabled(modes: Set<InputMode>): InputMode? {
        enabled = normalize(modes)
        var save: InputMode? = null
        if (previous?.let { it !in enabled } == true) previous = null
        val ret = returnMode
        if (ret != null && ret !in enabled) {
            returnMode = nextEnabled(ret)
            save = returnMode
        }
        if (current !in enabled) {
            current = nextEnabled(current)
            if (returnMode == null) save = current
        }
        return save
    }

    /** The first mode on in [available] after [m], wrapping (FR-CMN-03 ②, 03f ①); [m] itself when nothing else is on. */
    fun nextEnabled(m: InputMode): InputMode {
        val i = available.indexOf(m)
        for (k in 1..available.size) {
            val c = available[(i + k).mod(available.size)]
            if (c in enabled) return c
        }
        return m
    }

    /** Saved mode if available and on, else the first mode on from it (or from the start of the order) (FR-CMN-10, 03f ③). */
    private fun start(saved: InputMode?): InputMode {
        if (saved != null && saved in available) return if (saved in enabled) saved else nextEnabled(saved)
        val first = available[0]
        return if (first in enabled) first else nextEnabled(first)
    }

    /** Only modes of [available]; the always-on ones are added (FR-CMN-03f ⑤). Empty means all of [available]. */
    private fun normalize(modes: Set<InputMode>): Set<InputMode> {
        val on = available.filter { it in modes || it in ALWAYS_ON }.toSet()
        return on.ifEmpty { available.toSet() }
    }

    companion object {
        /** Cannot be turned off (FR-SET-08). */
        val ALWAYS_ON: Set<InputMode> = setOf(InputMode.QWERTY, InputMode.DUBEOLSIK)
    }
}
