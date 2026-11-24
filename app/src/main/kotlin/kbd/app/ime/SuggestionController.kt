package kbd.app.ime

import kbd.app.view.BarCell
import kbd.suggest.EditCommand
import kbd.suggest.EditPlan
import kbd.suggest.KeyAnswer
import kbd.suggest.OnFail
import kbd.suggest.ResetReason
import kbd.suggest.SuggestKey
import kbd.suggest.SuggestionSource
import kbd.suggest.TextPeek

/**
 * Word suggestions between the keys and the editor (LLD-12 §12, D-85, SRS FR-SUG). Android-free. It decides when the
 * provider is active, runs each key through it in the order of LLD-12 §6.1, writes its plans through
 * [ComposingController.applyEdits] and gives the bar its cells with a revision. All calls on the input method's main
 * thread (HLD 5B.1). Nothing typed is kept here or logged (NFR-05): the provider holds the word, [onError] gets only a
 * site and the exception.
 */
class SuggestionController(
    private val out: ComposingController,
    private val onError: (site: String, e: Exception) -> Unit,
) {
    /** Cells for the bar and their revision; the service joins the bar's `show`. Called on every event, same cells too (§9). */
    var onCandidates: (cells: List<BarCell>, revision: Int) -> Unit = { _, _ -> }

    /** The setting "English word suggestions" (FR-SET-11). */
    var enabled: Boolean = false
        set(v) {
            val was = active
            field = v
            changed(was)
        }

    /** `FieldInfo.autocompleteAllowed` of the current field (FR-SUG-10). */
    var fieldAllowed: Boolean = false
        set(v) {
            val was = active
            field = v
            changed(was)
        }

    /** The mode on screen; only QWERTY has a provider (§12.1). */
    var mode: InputMode = InputMode.QWERTY
        set(v) {
            val was = active
            field = v
            changed(was)
        }

    /** Ctrl or Alt on (FR-SUG-09 ①). */
    var chordOn: Boolean = false
        set(v) {
            val was = active
            field = v
            changed(was)
        }

    /** Goes up on each active key, each pick, reset and attach; a pick carrying an older one is dropped (FR-SUG-03 ③). */
    var revision: Int = 0
        private set

    private var source: SuggestionSource? = null

    /** §12.1: the six conditions. While false, no provider call and no editor read (FR-SUG-10, NFR-14 ③). */
    val active: Boolean
        get() = enabled && fieldAllowed && mode == InputMode.QWERTY && !chordOn && source != null && out.canReadText

    private val peek = object : TextPeek {
        override fun before(n: Int): String? = out.textBefore(n)
        override fun after(n: Int): String? = out.textAfter(n)
        override fun hasSelection(): Boolean? = out.hasSelection()
    }

    /** The dictionary was read: its provider, fresh (`None(false)`); null when the setting goes off (§12.4, §12.5). */
    fun attach(source: SuggestionSource?) {
        this.source = source   // the one before is dropped as it is; a null one turns active off with nothing to reset
        revision += 1
        publish()
    }

    /** One key (§12.2, §6.1). Not active: [normal] only. Exceptions of [normal] and of the editor are not caught. */
    fun key(k: SuggestKey, normal: () -> Unit) {
        val s = source
        if (!active || s == null) {
            normal()
            return
        }
        revision += 1
        when (val answer = guarded { s.onKey(k, peek) } ?: KeyAnswer.Pass) {
            KeyAnswer.Pass -> {
                normal()
                guarded { s.afterKey(k) }
            }
            is KeyAnswer.WriteThenPass -> {
                // The plan and the normal path in one batch (FR-SUG-07 ①); on a failed precondition the normal path alone.
                if (!out.applyEdits(answer.plan.expectBefore, edits(answer.plan), then = normal)) {
                    guarded { s.onWriteFailed() }
                    normal()
                }
                guarded { s.afterKey(k) }
            }
            is KeyAnswer.Consume -> {
                if (!out.applyEdits(answer.plan.expectBefore, edits(answer.plan))) {
                    guarded { s.onWriteFailed() }
                    if (answer.onFail == OnFail.PASS) {
                        normal()   // e.g. the undo of a pick could not be checked: a plain ⌫ (FR-SUG-08 ①)
                        guarded { s.afterKey(k) }
                    }
                }
            }
        }
        publish()
    }

    /** Cell [index] of the bar was picked under [revision] (§12.3). An older revision is dropped (FR-SUG-03 ③). */
    fun pick(index: Int, revision: Int) {
        val s = source
        if (!active || s == null || revision != this.revision) return
        this.revision += 1
        val plan = guarded { s.pick(index) }
        if (plan != null && !out.applyEdits(plan.expectBefore, edits(plan))) {
            guarded { s.onWriteFailed() }   // nothing written, the word is dropped (FR-SUG-06 ②)
        }
        publish()
    }

    /** FR-SUG-09 ②: the provider forgets its word; called active or not (§12.4). */
    fun reset(reason: ResetReason) {
        revision += 1
        source?.let { s -> guarded { s.reset(reason) } }
        publish()
    }

    /** After a setter: active → not active resets the provider and shows empty cells (§12.1). */
    private fun changed(was: Boolean) {
        if (was && !active) {
            source?.let { s -> guarded { s.reset(ResetReason.INACTIVE) } }
            revision += 1
            publish()
        }
    }

    private fun publish() {
        val s = source
        val cells = if (active && s != null) {
            guarded { s.candidates.take(BAR_CELLS).map { BarCell(it.text, it.spoken) } } ?: emptyList()
        } else {
            emptyList()
        }
        onCandidates(cells, revision)
    }

    /**
     * A provider call (§12.6): an [Exception] is reported with [SITE] only, the provider is reset once (a second
     * failure is ignored) and the result is null, so the key goes the normal way and is not lost. [Error] passes.
     */
    private inline fun <T> guarded(block: () -> T): T? = try {
        block()
    } catch (e: Exception) {
        onError(SITE, e)
        try {
            source?.reset(ResetReason.INACTIVE)
        } catch (_: Exception) {
        }
        null
    }

    companion object {
        /** Cells on the bar; the provider's own count is apart (LLD-12 §7.5). */
        const val BAR_CELLS = 3
        const val SITE = "Suggest.source"

        /** [EditPlan] commands to the app's [EditorEdit], one to one (§8.3). */
        private fun edits(p: EditPlan): List<EditorEdit> = p.commands.map {
            when (it) {
                is EditCommand.DeleteBefore -> EditorEdit.DeleteBefore(it.count)
                is EditCommand.Insert -> EditorEdit.Insert(it.text)
            }
        }
    }
}
