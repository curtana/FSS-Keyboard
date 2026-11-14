package kbd.hangul.compose

import kbd.hangul.event.ComposeResult
import kbd.hangul.event.JamoEvent
import kbd.hangul.jamo.Jamo
import kbd.hangul.jamo.JamoTables

/**
 * Hangul syllable composer (LLD-04 §7, FR-HGL-01..05). The app owns one per input mode.
 *
 * State is the input-unit list `units`; the composing syllable is always `fold(units)`, and folding
 * never commits (a commit restarts the list). Invalid events (D-09) are reported once through
 * [onInvalid]; the app passes the error log (task README §8 G5).
 */
class HangulComposer(
    val policy: ComposePolicy,
    private val onInvalid: (String) -> Unit = {},
) {
    private val units = mutableListOf<JamoEvent>()

    fun apply(e: JamoEvent): ComposeResult {
        if (e == JamoEvent.UndoLast) return undoLast()
        return ComposeResult(applyUnit(e), composingText())
    }

    /** Applies one action's events and returns one result, so intermediate text is never shown. */
    fun applyAll(events: List<JamoEvent>): ComposeResult {
        val committed = StringBuilder()
        for (e in events) committed.append(apply(e).committed)
        return ComposeResult(committed.toString(), composingText())
    }

    /**
     * Drops the last input unit of the composing syllable; does nothing when the list is empty. It
     * never crosses a commit (HLD 4.4). Graffiti ⌫ takes back one cheonjiin element with it (LLD-05 §6).
     */
    fun undoLast(): ComposeResult {
        units.removeLastOrNull()
        return ComposeResult("", composingText())
    }

    /** Removes the last input unit (FR-CMN-07, 07a): one key, one jamo or one cheonjiin element. */
    fun backspace(): BackspaceResult {
        if (units.isEmpty()) return BackspaceResult.NothingComposing
        units.removeAt(units.lastIndex)
        return BackspaceResult.Handled(ComposeResult("", composingText()))
    }

    /** Returns the composing text and clears the composer; called with `finishComposingText`. */
    fun finish(): String {
        val text = composingText()
        units.clear()
        return text
    }

    fun composingText(): String = fold().render()

    fun isComposing(): Boolean = units.isNotEmpty()

    fun hasJong(): Boolean = fold().hasJong

    fun currentVowel(): Jamo? = fold().jung

    fun snapshot(): ComposerSnapshot = ComposerSnapshot(units.toList())

    fun restore(s: ComposerSnapshot) {
        units.clear()
        units += s.units
    }

    /** One event on [units] (LLD-04 §7.3 steps 2–3); returns the text it commits. */
    private fun applyUnit(e: JamoEvent): String {
        val out = step(fold(), e)
        out.invalid?.let(onInvalid)
        if (out.commit != null) {
            units.clear()
            units += out.newUnits ?: listOfNotNull(out.unit)
            return out.commit.render()
        }
        if (out.unit != null) units += out.unit
        out.then?.let { units += it }
        return ""
    }

    /**
     * Result of one transition (LLD-04 §7.2). [unit] is what the event stores in `units`
     * (null: nothing), [invalid] a D-09 report.
     */
    private class StepOut(
        val next: Syllable,
        val commit: Syllable? = null,
        val newUnits: List<JamoEvent>? = null,
        val unit: JamoEvent? = null,
        val invalid: String? = null,
        /** A second unit stored after [unit] (a compound final typed at once is its two consonants, issue #242). */
        val then: JamoEvent? = null,
    )

    private fun fold(): Syllable = units.fold(Syllable.EMPTY) { s, u -> step(s, u).next }

    private fun step(s: Syllable, e: JamoEvent): StepOut = when (e) {
        is JamoEvent.Consonant -> if (e.c in JamoTables.SPLIT_JONG) compound(s, e.c, e) else consonant(s, e.c, e)
        is JamoEvent.DoubleConsonant -> {
            val double = JamoTables.DOUBLE_OF[e.base]
            if (double != null) consonant(s, double, e)
            else StepOut(s, invalid = "DoubleConsonant(${e.base.compat}): no double consonant, ignored")
        }
        is JamoEvent.Vowel -> vowel(s, e.v, e)
        is JamoEvent.ReplaceVowel ->
            if (s.jung != null && !s.hasJong) {
                StepOut(s.copy(jung = e.v), unit = e)   // its own unit: ⌫ takes back one cheonjiin element (FR-CMN-07b, issue #160)
            } else {
                StepOut(s, invalid = "ReplaceVowel(${e.v.compat}) on ${s.render().ifEmpty { "empty" }}, ignored")
            }
        JamoEvent.UndoLast -> StepOut(s)
    }

    /**
     * A compound final typed at once (issue #242, Dubeolsik ← → ↑ ↓ on ㄱ ㄴ ㄹ ㅂ ㅅ ㅌ ㅍ): on a syllable with an initial
     * and a medial but no final it is that syllable's final, stored as its two consonants — the same as typing them one
     * after the other (갉 + ㅏ = 갈가 by dokkaebibul, ⌫ takes the second back). Anywhere else it stands alone as the
     * compound jamo (ㄺ), committing what was composing; a vowel after it splits it (ㄺ + ㅏ = ㄹ가).
     */
    private fun compound(s: Syllable, c: Jamo, unit: JamoEvent): StepOut {
        val (a, b) = JamoTables.SPLIT_JONG.getValue(c)
        val alone = Syllable(c, null, null, null)
        return when {
            s.cho != null && s.jung != null && s.jong1 == null ->
                StepOut(s.copy(jong1 = a, jong2 = b), unit = JamoEvent.Consonant(a), then = JamoEvent.Consonant(b))
            s.isEmpty -> StepOut(alone, unit = unit)
            else -> StepOut(alone, commit = s, unit = unit)
        }
    }

    private fun consonant(s: Syllable, c: Jamo, unit: JamoEvent): StepOut {
        val fresh = Syllable(c, null, null, null)
        return when {
            s.isEmpty -> StepOut(fresh, unit = unit)
            s.cho == null || s.jung == null -> StepOut(fresh, commit = s, unit = unit)
            s.jong1 == null ->
                if (c in JamoTables.JONG_CAPABLE) StepOut(s.copy(jong1 = c), unit = unit)
                else StepOut(fresh, commit = s, unit = unit)
            s.jong2 == null && JamoTables.COMPOUND_JONG.containsKey(s.jong1 to c) ->
                StepOut(s.copy(jong2 = c), unit = unit)
            else -> StepOut(fresh, commit = s, unit = unit)
        }
    }

    private fun vowel(s: Syllable, v: Jamo, unit: JamoEvent): StepOut {
        val fresh = Syllable(null, v, null, null)
        val jung = s.jung
        return when {
            s.isEmpty -> StepOut(fresh, unit = unit)
            // A compound final standing alone (issue #242): its second consonant takes the vowel, as dokkaebibul.
            jung == null && s.cho in JamoTables.SPLIT_JONG -> {
                val (a, b) = JamoTables.SPLIT_JONG.getValue(checkNotNull(s.cho))
                StepOut(Syllable(b, v, null, null), commit = Syllable(a, null, null, null),
                    newUnits = listOf(JamoEvent.Consonant(b), JamoEvent.Vowel(v)))
            }
            jung == null -> StepOut(s.copy(jung = v), unit = unit)
            s.jong1 == null -> {
                val m = if (policy.mergeConsecutiveVowel) policy.merge.merge(jung, v) else null
                if (m != null) StepOut(s.copy(jung = m), unit = unit)
                else StepOut(fresh, commit = s, unit = unit)
            }
            // Dokkaebibul (FR-HGL-03): the last final moves to the next syllable.
            s.jong2 == null -> StepOut(
                Syllable(s.jong1, v, null, null), commit = s.copy(jong1 = null),
                newUnits = listOf(JamoEvent.Consonant(s.jong1), JamoEvent.Vowel(v)),
            )
            else -> StepOut(
                Syllable(s.jong2, v, null, null), commit = s.copy(jong2 = null),
                newUnits = listOf(JamoEvent.Consonant(s.jong2), JamoEvent.Vowel(v)),
            )
        }
    }
}
