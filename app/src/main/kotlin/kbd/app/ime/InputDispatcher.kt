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
import kbd.stroke.tap.ChordKeys
import kbd.stroke.tap.ChordModifier
import kbd.stroke.tap.TapGesture
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

    /**
     * Ctrl and Alt turned on and waiting for the next target key (FSS#98, FR-TAP-15, LLD-08 §6.3). Kept here, not in
     * a view: every key and every reason to turn them off passes through the dispatcher. Never stored, and not part
     * of [snapshot]: a chord that failed stays taken (HLD 9).
     */
    override var chord: Set<ChordModifier> = emptySet()
        private set

    /** Called once each time [chord] changes: the tap view redraws the two keys and renews their accessibility nodes. */
    var onChordChanged: () -> Unit = {}

    /**
     * What each function key held down took from [chord] when it was pressed, an empty set too, until its
     * [onFunctionUp]: a repeat sends what its press took, whatever is on by then (FR-TAP-15 ③).
     */
    private val heldChords = HashMap<FunctionKey, Set<ChordModifier>>()

    /** A resolved key without its gesture (the practice sink, an accessibility click): a tap. */
    override fun onTapOutput(key: TapKeyDef, out: TapOutput) = onTapResult(key, out, TapGesture.Tap)

    /**
     * One letter, digit or symbol key as it leaves the view's queue. With Ctrl or Alt on, the key of that gesture
     * goes out as a chord instead of the character (FR-TAP-16 ②③), and no error vibration for a missing script.
     */
    override fun onTapResult(key: TapKeyDef, out: TapOutput, gesture: TapGesture) {
        if (chord.isNotEmpty()) {
            val mods = takeChord()
            finishForChord()
            val stroke = ChordKeys.ofTap(key.id, gesture)
            this.out.chordKey(stroke.key, if (stroke.shift) mods + ChordModifier.SHIFT else mods)
            markInput()
            return
        }
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
            FunctionKey.CTRL -> toggleChord(ChordModifier.CTRL)
            FunctionKey.ALT -> toggleChord(ChordModifier.ALT)
            FunctionKey.MODE -> onModeKey()
            else -> targetKey(key)
        }
    }

    /**
     * A function key that a chord can ride on (FR-TAP-16 ①). The press takes the modifiers that are on; a repeat (a
     * down with no up since the last one) reuses what its press took.
     */
    private fun targetKey(key: FunctionKey) {
        val mods = heldChords.getOrPut(key) { takeChord() }
        if (mods.isNotEmpty()) {
            finishForChord()
            out.chordKey(checkNotNull(ChordKeys.ofFunction(key)) { key }, mods)
            if (key in CHORD_INPUT_KEYS) markInput()   // ⌫, space, ↵, ⇥; arrows, Esc and Del are not input (FR-TAP-16 ⑦)
            return
        }
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
            FunctionKey.MODE, FunctionKey.CTRL, FunctionKey.ALT -> error("not a target key: $key")
        }
    }

    /** Release notification, no input; the view stops the repeat (FR-CMN-11a). What the press took from the chord goes. */
    override fun onFunctionUp(key: FunctionKey) {
        heldChords.remove(key)
    }

    /** Ctrl or Alt pressed: on ↔ off (FR-TAP-15 ①). Not input, and what is composing stays (FR-TAP-15 ⑤). */
    private fun toggleChord(m: ChordModifier) = setChord(if (m in chord) chord - m else chord + m)

    /**
     * Turns Ctrl and Alt off and forgets what the held keys took; nothing is sent (FR-TAP-17 ①, LLD-08 §6.3). For the
     * mode key, a new field, the keyboard going down, a rotation. Not for a cursor move (FR-TAP-17 ②).
     */
    fun clearChord() {
        heldChords.clear()
        setChord(emptySet())
    }

    /** The modifiers that are on, now off: they ride on one key only (FR-TAP-15 ②). */
    private fun takeChord(): Set<ChordModifier> = chord.also { setChord(emptySet()) }

    private fun setChord(s: Set<ChordModifier>) {
        if (s == chord) return
        chord = s
        onChordChanged()
    }

    /**
     * Start of the chord path (HLD 5A, FR-TAP-16 ③): what is composing is committed first, like an arrow or Esc. The
     * key then goes out as key events only; the keyboard itself types, deletes and undoes nothing.
     */
    private fun finishForChord() {
        finishAll()
        resetParsers(ResetReason.CURSOR_MOVED)
    }

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
        clearChord()   // FR-TAP-17 ① (라)
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
        clearChord()   // not a target key: the chord goes and nothing is sent (FR-TAP-17 ① (다))
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

    private companion object {
        /** Function keys that count as input, with a chord as without (FR-CMN-03a, FR-TAP-16 ⑦). */
        val CHORD_INPUT_KEYS = setOf(FunctionKey.BACKSPACE, FunctionKey.SPACE, FunctionKey.ENTER, FunctionKey.TAB)
    }
}
