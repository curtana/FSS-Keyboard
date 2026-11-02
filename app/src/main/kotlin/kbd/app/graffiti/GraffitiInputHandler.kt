package kbd.app.graffiti

import kbd.app.ime.ComposingController
import kbd.hangul.cheonjiin.CheonjiinAssembler
import kbd.hangul.cheonjiin.CjElement
import kbd.hangul.cheonjiin.CjPop
import kbd.hangul.cheonjiin.CjPush
import kbd.hangul.cheonjiin.CjSnapshot
import kbd.hangul.cheonjiin.CjState
import kbd.graffiti.ink.GraffitiLang
import kbd.graffiti.ink.InkStroke
import kbd.graffiti.recognize.Expect
import kbd.graffiti.recognize.Recognizer
import kbd.graffiti.state.GraffitiAction
import kbd.graffiti.state.GraffitiState
import kbd.graffiti.state.GraffitiStateSnapshot
import kbd.hangul.compose.BackspaceResult
import kbd.hangul.compose.HangulComposer
import kbd.hangul.event.ComposeResult
import kbd.hangul.event.JamoEvent

/** Callers of [GraffitiInputHandler.reset] (LLD-05 §6); all three do the same thing. */
enum class ResetReason { CURSOR_MOVED, FIELD_CHANGED, MODE_CHANGED }

/** Assembler and state values for EngineGuard rollback (LLD-05 §6, LLD-09 §2). */
data class GraffitiSnapshot(val asm: CjSnapshot, val state: GraffitiStateSnapshot)

/**
 * Ties the cheonjiin assembler, the composer and the editor together (LLD-05 §6). Android-free.
 * Every commit goes through [finishAll], which drops the temporary glyph (FR-HGL-04e, DCR-21).
 */
class GraffitiInputHandler(
    private val recognizer: Recognizer,
    private val state: GraffitiState,
    private val asm: CheonjiinAssembler,
    private val composer: () -> HangulComposer,   // graffiti-policy composer (LLD-08 §6)
    private val out: ComposingController,
    private val sink: InputSink,
    private val flash: (String) -> Unit,           // GraffitiPadView.flash (LLD-05 §8.3)
    private val redraw: () -> Unit = {},           // pad status redraw after changes from outside the pad (LLD-05 §8.1)
) {

    /** The last [handle] was an element the assembler dropped ([CjPush.Ignored] or [CjPush.NotExtendable]). */
    private var lastElementDropped = false

    fun onStroke(stroke: InkStroke) {
        val expect = when {
            state.extPending -> Expect.EXTENDED
            state.punctPending -> Expect.PUNCT
            else -> Expect.NORMAL
        }
        val t = recognizer.recognize(stroke, state.lang, expect)
        val a = state.interpret(t, stroke.area)
        handle(a)
        // FR-GRF-07: only a stroke that means a glyph is shown, never in password fields (FR-CMN-08 ①;
        // no-learning fields flash, ②, DCR-19). An element the assembler dropped changes nothing and is not shown (FR-HGL-04c, #74 A).
        val glyph = label(a)
        if (glyph.isNotEmpty() && !lastElementDropped && !sink.hidesDisplay) flash(glyph)
    }

    /** The §6 action table; "typed" actions end with `sink.markInput()` (FR-CMN-03a). */
    fun handle(a: GraffitiAction) {
        lastElementDropped = false
        val typed = when (a) {
            is GraffitiAction.Consonant -> {
                when (asm.state) {
                    is CjState.Vowel -> asm.close()
                    is CjState.Pending -> asm.clear()   // the temporary glyph is dropped
                    CjState.Empty -> Unit
                }
                show(composer().apply(JamoEvent.Consonant(a.c)))
                true
            }
            is GraffitiAction.Element -> {
                // Ignored and NotExtendable elements change nothing but are still typed (FR-HGL-04c, 04e, FR-CMN-03a).
                val r = when (val p = asm.push(a.e)) {
                    is CjPush.Started -> composer().apply(JamoEvent.Vowel(p.v))   // dokkaebibul happens here
                    is CjPush.Changed -> composer().apply(JamoEvent.ReplaceVowel(p.v))
                    is CjPush.PendingShown -> composing()
                    CjPush.Ignored, CjPush.NotExtendable -> composing().also { lastElementDropped = true }
                }
                show(r)
                true
            }
            is GraffitiAction.CommitText -> {
                finishAll()
                out.commitText(a.text)
                true
            }
            GraffitiAction.Space -> {
                finishAll()
                out.commitText(" ")
                true
            }
            GraffitiAction.Enter -> {
                finishAll()
                out.enter(sink.field)
                true
            }
            GraffitiAction.Finish -> {
                finishAll()
                false   // the 'J' stroke is not input (DCR-17)
            }
            GraffitiAction.Backspace -> {
                backspace()
                true
            }
            GraffitiAction.ToggleLang -> {
                finishAll()
                state.lang = if (state.lang == GraffitiLang.KO) GraffitiLang.EN else GraffitiLang.KO
                false
            }
            GraffitiAction.StateOnly -> false
            GraffitiAction.RecognitionFailed -> {
                sink.error()   // nothing else changes (FR-GRF-08, DCR-07)
                false
            }
        }
        if (typed) sink.markInput()
    }

    /** ⌫ order (FR-CMN-07b, HLD 5): cancel a pending shift, then the assembler, then the composer. */
    fun backspace() {
        if (state.cancelPending()) return
        val wasVowel = asm.state is CjState.Vowel
        when (val r = asm.pop()) {
            // The element's own ReplaceVowel unit goes (issue #160): the composer is back at the assembler's vowel.
            is CjPop.ToVowel -> show(composer().undoLast())
            is CjPop.ToPending -> {
                // The vowel unit goes, dokkaebibul included; the committed syllable stays (거 → ㄱㆍ, 가거 → 가ㄱㆍ).
                show(if (wasVowel) composer().undoLast() else composing())
            }
            is CjPop.ToNone -> show(if (r.wasVowel) composer().undoLast() else composing())
            CjPop.Empty -> when (val b = composer().backspace()) {
                is BackspaceResult.Handled -> show(b.result)
                BackspaceResult.NothingComposing -> out.deleteBefore()
            }
        }
    }

    fun finishAll() {
        asm.clear()
        composer().finish()
        out.finishComposing()   // takes the temporary glyph out before committing (LLD-08 §4)
    }

    /** New entry into graffiti mode (gap G6, LLD-08 §3.1, §6.1). */
    fun enterMode() {
        state.enterMode()
        redraw()
    }

    /** Cursor move, field change or mode switch; the caller has already finished composing (LLD-08 §6). */
    fun reset(reason: ResetReason) {
        asm.clear()
        state.clearOneShot()
        redraw()   // a pending indicator goes with the pending shift (FR-GRF-05d)
    }

    /** The composer is not included: InputDispatcher restores it on its own (LLD-08 §6.1). */
    fun snapshot(): GraffitiSnapshot = GraffitiSnapshot(asm.snapshot(), state.snapshot())

    fun restore(s: GraffitiSnapshot) {
        asm.restore(s.asm)
        state.restore(s.state)
        redraw()
    }

    private fun show(r: ComposeResult) = out.show(r, asm.pendingGlyph())

    /** The composer as it is, for a show that only changes the temporary glyph. */
    private fun composing() = ComposeResult("", composer().composingText())

    private companion object {
        /** Text for `flash`: the recognized glyph, "" for actions without one (FR-GRF-07, recognition failure included). */
        fun label(a: GraffitiAction): String = when (a) {
            is GraffitiAction.Consonant -> a.c.compat.toString()
            is GraffitiAction.Element -> when (a.e) {
                CjElement.I -> "ㅣ"
                CjElement.EU -> "ㅡ"
                CjElement.DOT -> "ㆍ"
            }
            is GraffitiAction.CommitText -> a.text
            else -> ""
        }
    }
}
