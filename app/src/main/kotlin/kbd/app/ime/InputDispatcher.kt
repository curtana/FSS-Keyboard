package kbd.app.ime

import kbd.app.error.GuardedState
import kbd.app.feedback.FeedbackPort
import kbd.app.graffiti.GraffitiInputHandler
import kbd.app.graffiti.GraffitiSnapshot
import kbd.app.graffiti.InputSink
import kbd.app.graffiti.ResetReason
import kbd.app.view.TapSink
import kbd.hangul.compose.BackspaceResult
import kbd.hangul.compose.ComposerSnapshot
import kbd.hangul.compose.HangulComposer
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

    /** Graffiti mode only (LLD-08 §6): set while the graffiti view is shown and null otherwise. */
    var graffiti: GraffitiInputHandler? = null

    /** Current field; set by `KeyboardService.onStartInputView` (LLD-08 §3.1). */
    override var field: FieldInfo = FieldInfo(FieldClass.TEXT, isPassword = false, noLearning = false, multiLine = false, enterAction = null)

    /** The service shows the view of the new mode (LLD-08 §6.1 `onModeKey`). */
    var onShowMode: (InputMode) -> Unit = {}

    /** The service opens the keyboard settings screen (an up stroke on the mode key, issue #205). */
    var onOpenSettings: () -> Unit = {}

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
                markInput()
            }
            FunctionKey.ENTER -> {
                finishAll()
                out.enter(field)
                markInput()
            }
            FunctionKey.TAB -> {
                finishAll()
                out.tab()
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
        val b = composer?.backspace()
        if (b is BackspaceResult.Handled) out.show(b.result) else out.deleteBefore()
    }

    /**
     * Mode key up stroke, in every mode (issue #205, owner decision 2026-11-02): a composing syllable
     * is committed first, like the arrows and Esc; then the keyboard settings open. Not input (like
     * the mode key).
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

    /** OTHER_MOVED: nothing is composing; the graffiti assembler closes (FR-CMN-03d). */
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

    /** HLD 5 initialisation: the graffiti assembler and one-shot state (LLD-05 §6). The default [reason] is the field change. */
    fun resetParsers(reason: ResetReason = ResetReason.FIELD_CHANGED) {
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

    /** Composer input units and the graffiti assembler and state (LLD-09 §2, LLD-08 §6.1). */
    override fun snapshot(): Any = Snapshot(composer?.snapshot(), graffiti?.snapshot())

    override fun restore(s: Any) {
        val snap = s as Snapshot
        snap.composer?.let { composer?.restore(it) }
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

    private data class Snapshot(val composer: ComposerSnapshot?, val graffiti: GraffitiSnapshot?)
}
