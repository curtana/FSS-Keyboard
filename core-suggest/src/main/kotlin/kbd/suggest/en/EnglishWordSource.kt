package kbd.suggest.en

import java.util.Locale
import kbd.suggest.Candidate
import kbd.suggest.EditCommand
import kbd.suggest.EditPlan
import kbd.suggest.KeyAnswer
import kbd.suggest.OnFail
import kbd.suggest.ResetReason
import kbd.suggest.SuggestKey
import kbd.suggest.SuggestionSource
import kbd.suggest.TextPeek

/**
 * English dictionary completion (LLD-12 §7, D-81): the state machine of the key-event table §7.3,
 * picking §7.4 and candidates §7.5. No clock, no randomness, no global state (§6.2).
 *
 * State moves in [afterKey] for Pass / WriteThenPass answers, in [onKey] for Consume, in [pick] for picks;
 * [onWriteFailed] and [reset] go back to `None(false)` (§6.1).
 */
class EnglishWordSource(private val index: WordIndex) : SuggestionSource {

    /** The states of LLD-12 §7.2. */
    internal sealed interface State {
        /** No word. [prevKnown] = the text right before the cursor is a word end the keyboard put there. */
        data class None(val prevKnown: Boolean) : State

        /** Typing [word] (as typed, 1..MAX_WORD characters, may end in '). */
        data class Typing(val word: String) : State

        /** No candidates for this word, until the next word end. */
        data object Blocked : State

        /** Just picked [picked] (as shown, without the space) while the word was [typed]; an automatic space is before the cursor. */
        data class JustPicked(val picked: String, val typed: String) : State
    }

    internal var state: State = State.None(false)
        private set

    /** Peek decision of `None(false)` + letter, made in [onKey] and applied in [afterKey]: true = Blocked. */
    private var pendingBlocked: Boolean? = null

    private var cachedWord: String? = null
    private var cachedCandidates: List<Candidate> = emptyList()

    override val candidates: List<Candidate>
        get() {
            val s = state as? State.Typing ?: return emptyList()
            if (s.word != cachedWord) {
                cachedCandidates = compute(s.word)
                cachedWord = s.word
            }
            return cachedCandidates
        }

    override fun onKey(key: SuggestKey, peek: TextPeek): KeyAnswer {
        pendingBlocked = null
        val s = state
        if (s is State.JustPicked) {
            when (key) {
                is SuggestKey.Text -> if (key.text.length == 1 && key.text[0] in WordChars.PUNCT_JOIN) {
                    return KeyAnswer.WriteThenPass(EditPlan(" ", listOf(EditCommand.DeleteBefore(1))))
                }
                SuggestKey.Space -> return KeyAnswer.Consume(EditPlan.NONE, OnFail.NOTHING)
                SuggestKey.Backspace -> {
                    val plan = EditPlan(
                        s.picked + " ",
                        listOf(EditCommand.DeleteBefore(s.picked.length + 1), EditCommand.Insert(s.typed)),
                    )
                    state = State.Typing(s.typed)
                    return KeyAnswer.Consume(plan, OnFail.PASS)
                }
                else -> Unit
            }
            return KeyAnswer.Pass
        }
        if (s == State.None(false) && isLetterKey(key)) pendingBlocked = peekBlocks(peek)
        return KeyAnswer.Pass
    }

    override fun afterKey(key: SuggestKey) {
        val pending = pendingBlocked
        pendingBlocked = null
        state = next(state, key, pending)
    }

    override fun pick(index: Int): EditPlan? {
        val s = state as? State.Typing ?: return null
        val cells = candidates
        if (index !in cells.indices) return null
        val c = cells[index]
        val w = s.word
        val plan = when {
            c.kind == Candidate.Kind.TYPED -> EditPlan(w, listOf(EditCommand.Insert(" ")))
            c.text.startsWith(w) -> EditPlan(w, listOf(EditCommand.Insert(c.text.substring(w.length) + " ")))
            else -> EditPlan(w, listOf(EditCommand.DeleteBefore(w.length), EditCommand.Insert(c.text + " ")))
        }
        state = State.JustPicked(c.text, w)
        return plan
    }

    override fun onWriteFailed() = toStart()

    override fun reset(reason: ResetReason) = toStart()

    private fun toStart() {
        state = State.None(false)
        pendingBlocked = null
    }

    /** The "→ state" column of LLD-12 §7.3 for answers that move state in [afterKey]. */
    private fun next(s: State, key: SuggestKey, pendingBlocked: Boolean?): State {
        if (isResetKey(key)) return State.None(false)
        return when (s) {
            is State.None -> when {
                isLetterKey(key) -> if (s.prevKnown) typing(key) else {
                    // No decision (onKey not asked in this state) counts as an unknown neighbour.
                    if (pendingBlocked == false) typing(key) else State.Blocked
                }
                key == SuggestKey.Backspace -> State.None(false)
                else -> State.None(true) // apostrophe (no word to join), word-end text, Space
            }
            is State.Typing -> {
                val w = s.word
                when {
                    isLetterKey(key) || (isApostrophe(key) && WordChars.isLetter(w.last())) -> {
                        val c = (key as SuggestKey.Text).text
                        if (w.length < WordChars.MAX_WORD) State.Typing(w + c) else State.Blocked
                    }
                    key == SuggestKey.Backspace -> if (w.length > 1) State.Typing(w.dropLast(1)) else State.None(true)
                    else -> State.None(true) // ' after ', word-end text, Space
                }
            }
            State.Blocked -> when {
                isLetterKey(key) || isApostrophe(key) || key == SuggestKey.Backspace -> State.Blocked
                else -> State.None(true)
            }
            is State.JustPicked -> when {
                // Same length rule as Typing + apostrophe (§7.2: a word is 1..MAX_WORD characters).
                isApostrophe(key) -> if (s.picked.length < WordChars.MAX_WORD) State.Typing(s.picked + "'") else State.Blocked
                isLetterKey(key) -> typing(key)
                // Other text (PUNCT_JOIN or not) and, after a fallback, anything else ends the word.
                else -> State.None(true)
            }
        }
    }

    /** §7.3 `None(false)` + letter: before(1), then after(1), then hasSelection(), stopping at the first null. */
    private fun peekBlocks(peek: TextPeek): Boolean {
        val before = peek.before(1) ?: return true
        val after = peek.after(1) ?: return true
        val selection = peek.hasSelection() ?: return true
        return selection || isWordChar(before.lastOrNull()) || isWordChar(after.firstOrNull())
    }

    /** A peeked character is a word character only when it is a letter (§7.1; ' and empty text are not). */
    private fun isWordChar(c: Char?): Boolean = c != null && WordChars.isLetter(c)

    private fun compute(w: String): List<Candidate> {
        if (CaseRule.shapeOf(w) == CaseRule.Shape.MIXED) return emptyList()
        val ids = index.top(w.lowercase(Locale.ROOT)) ?: return listOf(Candidate(w, kind = Candidate.Kind.TYPED))
        val out = ArrayList<Candidate>(MAX_CANDIDATES)
        for (id in ids) {
            val text = CaseRule.apply(w, index.form(id)) ?: continue
            if (text == w) continue
            out += Candidate(text)
            if (out.size == MAX_CANDIDATES) break
        }
        return out
    }

    companion object {
        /** Cells this source fills at most (SRS FR-SUG-05 ②). Separate from the app bar's cell count. */
        const val MAX_CANDIDATES = 3

        private fun typing(key: SuggestKey): State = State.Typing((key as SuggestKey.Text).text)

        private fun isLetterKey(key: SuggestKey): Boolean =
            key is SuggestKey.Text && key.text.length == 1 && WordChars.isLetter(key.text[0])

        private fun isApostrophe(key: SuggestKey): Boolean = key is SuggestKey.Text && key.text == "'"

        private fun isResetKey(key: SuggestKey): Boolean =
            key == SuggestKey.Enter || key == SuggestKey.Tab || key == SuggestKey.Navigation || key == SuggestKey.Function
    }
}
