package kbd.app.ime

import kbd.app.error.GuardedState
import kbd.app.feedback.FeedbackPort
import kbd.app.graffiti.GraffitiInputHandler
import kbd.app.graffiti.GraffitiSnapshot
import kbd.app.graffiti.InputSink
import kbd.app.graffiti.ResetReason
import kbd.app.swipe.SwipeEngineState
import kbd.app.view.CenterText
import kbd.app.view.TapSink
import kbd.hangul.stroke.StrokePop
import kbd.hangul.compose.BackspaceResult
import kbd.hangul.compose.ComposerSnapshot
import kbd.hangul.compose.HangulComposer
import kbd.hangul.event.ComposeResult
import kbd.hangul.event.JamoEvent
import kbd.hangul.jamo.Jamo
import kbd.hangul.jamo.JamoKind
import kbd.stroke.geom.Dir
import kbd.stroke.layout.FunctionKey
import kbd.stroke.layout.TapKeyDef
import kbd.stroke.tap.TapOutput

/**
 * Applies view output to the composer, the editor and the mode state (LLD-08 §6). Android-free.
 */
class InputDispatcher(
    private val modes: ModeController,
    private val out: ComposingController,
    private val feedback: FeedbackPort,
    private val saveMode: (InputMode) -> Unit,
) : TapSink, InputSink, GuardedState {

    /** Per-mode policy (LLD-08 §3.1); null in QWERTY, where letters are committed at once. */
    var composer: HangulComposer? = null

    /** Swipe mode only: the wrapped `SwipeInputMapper` (LLD-03 §2.2). */
    var swipeState: SwipeEngineState? = null

    /** Graffiti mode only (LLD-08 §6); like [swipeState], set while the graffiti view is shown and null otherwise. */
    var graffiti: GraffitiInputHandler? = null

    /** Current field; set by `KeyboardService.onStartInputView` (LLD-08 §3.1). */
    override var field: FieldInfo = FieldInfo(FieldClass.TEXT, isPassword = false, noLearning = false, multiLine = false, enterAction = null)

    /** The service shows the view of the new mode (LLD-08 §6.1 `onModeKey`). */
    var onShowMode: (InputMode) -> Unit = {}

    /** The service opens the keyboard settings screen (an up stroke on the mode key, issue #205). */
    var onOpenSettings: () -> Unit = {}

    /**
     * Swipe `SwipeInputMapper` output (HLD 4.1). A vowel stroke or cheonjiin element may give no event
     * (ㆍ·‥ pending): the composition is shown again with the mapper's temporary glyph after it
     * (SWIPE 3.4, FR-SWP-64). [errorVibe]: the input closed a vowel assembly and dropped its unfinished
     * strokes (SWIPE 3.3).
     */
    fun onJamoEvents(ev: List<JamoEvent>, errorVibe: Boolean = false) {
        val c = composer ?: return
        if (errorVibe) feedback.error()
        out.show(c.applyAll(ev), swipeState?.pendingGlyph() ?: "", swipeState?.pendingReplacesVowel() == true)
        markInput()
    }

    /**
     * What the swipe view draws in C (SWIPE 9.5, FR-DSP-10, owner decisions 2026-11-02, FSS#36): the one composing
     * syllable, no candidates. Vowel strokes that make no vowel yet show as dots in place ([CenterText.dots]) instead
     * of the editor's ㆍ·‥; a cheonjiin ㆍ·‥ stays text as in the editor. Null when nothing composes or no consonant
     * does (a bare vowel). Read only; it changes only when the composer or the assemblies do (a tap, a released
     * stroke, a cheonjiin element, ⌫, a reset), never while a stroke is drawn.
     */
    fun centerText(): CenterText? {
        val c = composer ?: return null
        if (c.lastConsonant() == null) return null
        val sw = swipeState
        val dots = sw?.pendingDots().orEmpty()
        if (dots.isNotEmpty()) {
            // The ‥ that hides the composer's ㅣ / ㅡ: the syllable without that vowel (그 + a second line = ㄱ and two dots).
            val base = if (sw?.pendingReplacesVowel() == true) {
                HangulComposer(c.policy).also { it.restore(c.snapshot()); it.undoLast() }.composingText()
            } else {
                c.composingText()
            }
            return CenterText(base, dots)
        }
        return CenterText(c.composingText() + (sw?.pendingGlyph() ?: ""))   // the cheonjiin ㆍ·‥ as in the editor
    }

    /** Closes the swipe assemblies for a key the user pressed; unfinished vowel strokes dropped = the error vibration (SWIPE 3.3). */
    private fun closeSwipe() {
        if (swipeState?.reset() == true) feedback.error()
    }

    /** Swipe perimeter digits and symbols; the mapper has already closed its cheonjiin assembly (LLD-04 §6.2, D-30). */
    fun onSwipeText(s: String, vibe: Boolean) {
        finishAll()
        out.commitText(s)
        if (vibe) feedback.error()
        markInput()
    }

    override fun onTapOutput(key: TapKeyDef, out: TapOutput) {
        val jamo = out.jamo
        val text = out.text
        if (jamo != null) {
            val j = Jamo.ofCompat(jamo)
            val c = composer
            if (j != null && c != null) {
                this.out.show(c.apply(if (j.kind == JamoKind.CONSONANT) JamoEvent.Consonant(j) else JamoEvent.Vowel(j)))
            }
        } else if (text != null) {
            finishAll()   // FR-TAP-11a
            this.out.commitText(text)
        }
        if (out.errorVibe) feedback.error()
        markInput()
    }

    override fun onFunctionDown(key: FunctionKey) {
        when (key) {
            FunctionKey.BACKSPACE -> {
                backspace()
                markInput()
            }
            FunctionKey.SPACE -> {
                finishAll()
                out.commitText(" ")
                closeSwipe()
                markInput()
            }
            FunctionKey.ENTER -> {
                finishAll()
                out.enter(field)
                closeSwipe()
                markInput()
            }
            FunctionKey.TAB -> {
                finishAll()
                out.tab()
                closeSwipe()   // a forced commit: the assemblies close (FR-CMN-03d)
                markInput()
            }
            FunctionKey.ARROW_LEFT -> arrow(Dir.LEFT)
            FunctionKey.ARROW_UP -> arrow(Dir.UP)
            FunctionKey.ARROW_DOWN -> arrow(Dir.DOWN)
            FunctionKey.ARROW_RIGHT -> arrow(Dir.RIGHT)
            FunctionKey.ESC -> editKey(EditorKey.ESCAPE)
            FunctionKey.DELETE -> editKey(EditorKey.FORWARD_DEL)
            FunctionKey.MODE -> onModeKey()
        }
    }

    /** Release notification only; the view stops the repeat (FR-CMN-11a). */
    override fun onFunctionUp(key: FunctionKey) {}

    /** One ⌫; the repeat timer calls this too (FR-CMN-07, 07a, 07c). */
    fun backspace() {
        graffiti?.let { return it.backspace() }   // shift, assembler, composer order (LLD-05 §6)
        val sw = swipeState
        val c = composer
        if (sw != null && c != null) {
            // Swipe (SWIPE 3.5, FR-SWP-64, FR-CMN-07b): an open vowel assembly gives back one stroke or element first.
            val pop = sw.popElement()
            if (pop is StrokePop.Popped) {
                val r = if (pop.undoComposer) c.undoLast()   // the vowel's last change goes (its own Vowel / ReplaceVowel unit)
                else ComposeResult("", c.composingText())   // only ㆍ·‥ changed
                out.show(r, sw.pendingGlyph(), sw.pendingReplacesVowel())
                return
            }
        }
        val b = composer?.backspace()
        if (b is BackspaceResult.Handled) out.show(b.result) else out.deleteBefore()
    }

    /**
     * Mode key up stroke, in all four modes (issue #205, owner decision 2026-11-02): a composing syllable
     * is committed first and the assemblies close (unfinished strokes vibrate, SWIPE 3.3), like the
     * arrows and Esc; then the keyboard settings open. Not input (like the mode key). The swipe ⚙ key
     * and its one-syllable completion (issues #173, #180) are gone.
     */
    override fun onSettingsStroke() {
        finishAll()
        resetParsers(ResetReason.MODE_CHANGED)
        onOpenSettings()
    }

    /**
     * Mode key: not input (FR-CMN-03a). Modes changed inside numeric-like fields are not saved (FR-CMN-04b, 10).
     * A new entry into graffiti calls `enterMode()` after [onShowMode], where the service attaches [graffiti] (G6).
     * Since issue #205 the views call it on the mode key's release (a tap), not on its press.
     */
    fun onModeKey() {
        finishAll()
        resetParsers(ResetReason.MODE_CHANGED)
        val before = modes.current
        val next = modes.onModeKey()
        if (!modes.inNumericField) saveMode(next)
        onShowMode(next)
        if (next == InputMode.GRAFFITI && before != InputMode.GRAFFITI) graffiti?.enterMode()   // FR-GRF-05a
    }

    /**
     * After `ComposingController.onUpdateSelection` returned USER_MOVED. The verdict keeps the span
     * start, so [ComposingController.finishAfterUserMove] must be the first editor call after it (G13).
     */
    fun onCursorMoved(newS: Int, newE: Int, candS: Int, candE: Int) {
        composer?.finish()
        out.finishAfterUserMove(newS, newE, candS, candE)
        resetParsers(ResetReason.CURSOR_MOVED)
    }

    /** OTHER_MOVED: nothing is composing; the swipe cheonjiin assembly closes (FR-CMN-03d). */
    fun onCursorMovedNoComposing() {
        resetParsers(ResetReason.CURSOR_MOVED)
    }

    /** FR-CMN-03d. In graffiti the handler also clears the assembler and drops the temporary glyph (FR-HGL-04e). */
    fun finishAll() {
        val g = graffiti
        if (g != null) {
            g.finishAll()
        } else {
            composer?.finish()
            out.finishComposing()
        }
    }

    /** HLD 4.3, 5 initialisation. [reason] only labels the graffiti call (LLD-05 §6); the default is the field change. */
    fun resetParsers(reason: ResetReason = ResetReason.FIELD_CHANGED) {
        // The mode key (tap or up stroke) is a key the user pressed: dropped vowel strokes vibrate (SWIPE 3.3);
        // a cursor move or a new field drops them silently (LLD decision D-57).
        val dropped = swipeState?.reset() == true
        if (dropped && reason == ResetReason.MODE_CHANGED) feedback.error()
        graffiti?.reset(reason)
    }

    override fun markInput() {
        modes.onInput()
    }

    /** Graffiti recognition failure (FR-GRF-08). */
    override fun error() {
        feedback.error()
    }

    override val hidesDisplay: Boolean get() = this.field.hidesDisplay   // `this.`: bare `field` is the backing-field keyword here

    /** Composer input units, the `SwipeInputMapper` state and the graffiti assembler and state (LLD-09 §2, LLD-08 §6.1). */
    override fun snapshot(): Any = Snapshot(composer?.snapshot(), swipeState?.snapshot(), graffiti?.snapshot())

    override fun restore(s: Any) {
        val snap = s as Snapshot
        snap.composer?.let { composer?.restore(it) }
        snap.swipe?.let { swipeState?.restore(it) }
        snap.graffiti?.let { graffiti?.restore(it) }
    }

    /** Arrows are not input (FR-TAP-10, FR-CMN-03a). */
    private fun arrow(d: Dir) {
        finishAll()
        resetParsers(ResetReason.CURSOR_MOVED)
        out.arrow(d)
    }

    /** Esc and Del (issue #198): like an arrow key, a composing syllable is finished first. */
    private fun editKey(k: EditorKey) {
        finishAll()
        resetParsers(ResetReason.CURSOR_MOVED)
        out.editKey(k)
    }

    private data class Snapshot(val composer: ComposerSnapshot?, val swipe: Any?, val graffiti: GraffitiSnapshot?)
}
