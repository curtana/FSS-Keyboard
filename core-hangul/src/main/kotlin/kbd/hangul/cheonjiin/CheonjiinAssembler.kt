package kbd.hangul.cheonjiin

import kbd.hangul.cheonjiin.CjElement.DOT
import kbd.hangul.cheonjiin.CjElement.EU
import kbd.hangul.cheonjiin.CjElement.I
import kbd.hangul.jamo.Jamo

/** Assembler state (LLD-05 §4.1): `TABLE[elements]`. */
sealed interface CjState {
    data object Empty : CjState
    data class Pending(val dots: Int) : CjState      // 1 = ㆍ, 2 = ‥ (no vowel yet)
    data class Vowel(val v: Jamo) : CjState
}

/** Result of [CheonjiinAssembler.push] (LLD-05 §4.2). */
sealed interface CjPush {
    data class Started(val v: Jamo) : CjPush         // non-vowel → vowel
    data class Changed(val v: Jamo) : CjPush         // vowel → vowel
    data class PendingShown(val dots: Int) : CjPush
    data object Ignored : CjPush                     // third tap after ‥ (FR-HGL-04e)
    data object NotExtendable : CjPush               // element dropped, nothing changes (FR-HGL-04c, DCR-04)
}

/** Result of [CheonjiinAssembler.pop] (LLD-05 §4.2). */
sealed interface CjPop {
    data object Empty : CjPop                        // nothing to pop
    data class ToVowel(val v: Jamo) : CjPop
    data class ToPending(val dots: Int) : CjPop
    data class ToNone(val wasVowel: Boolean) : CjPop
}

/** Value copy of the assembler for EngineGuard (LLD-09 §2). */
data class CjSnapshot(val elements: List<CjElement>)

/** Cheonjiin vowel assembler: element sequence → vowel by [TABLE] (LLD-05 §4, GRAFFITI 4.2). */
class CheonjiinAssembler {

    private val seq = mutableListOf<CjElement>()

    /** Elements being assembled, in drawing order. */
    val elements: List<CjElement> get() = seq.toList()

    val state: CjState get() = stateOf(seq)

    fun push(e: CjElement): CjPush {
        val before = state
        val next = seq + e
        val after = TABLE[next]
        if (after == null) {
            return if (seq == listOf(DOT, DOT) && e == DOT) CjPush.Ignored else CjPush.NotExtendable
        }
        seq += e
        return when (after) {
            is CjState.Vowel -> if (before is CjState.Vowel) CjPush.Changed(after.v) else CjPush.Started(after.v)
            is CjState.Pending -> CjPush.PendingShown(after.dots)
            CjState.Empty -> error("TABLE has no empty sequence")
        }
    }

    fun pop(): CjPop {
        if (seq.isEmpty()) return CjPop.Empty
        val before = state
        seq.removeAt(seq.lastIndex)
        return when (val after = state) {
            CjState.Empty -> CjPop.ToNone(wasVowel = before is CjState.Vowel)
            is CjState.Vowel -> CjPop.ToVowel(after.v)
            is CjState.Pending -> CjPop.ToPending(after.dots)
        }
    }

    /** A final consonant followed the vowel: drop the elements; a closed vowel never reopens (HLD 5). */
    fun close() = seq.clear()

    /** Commit or reset: drop the elements (a pending glyph goes with them). */
    fun clear() = seq.clear()

    /** Temporary glyph for a pending tap sequence (FR-HGL-04e). */
    fun pendingGlyph(): String = when (val s = state) {
        is CjState.Pending -> if (s.dots == 1) "ㆍ" else "‥"
        else -> ""
    }

    fun snapshot(): CjSnapshot = CjSnapshot(seq.toList())

    fun restore(s: CjSnapshot) {
        require(s.elements.isEmpty() || s.elements in TABLE) { "not a TABLE sequence: ${s.elements}" }
        seq.clear()
        seq += s.elements
    }

    companion object {
        /** The 23 sequences of LLD-05 §4.1. Sequences not listed are never built. */
        val TABLE: Map<List<CjElement>, CjState> = mapOf(
            listOf(I) to CjState.Vowel(Jamo.I),
            listOf(I, DOT) to CjState.Vowel(Jamo.A),
            listOf(I, DOT, I) to CjState.Vowel(Jamo.AE),
            listOf(I, DOT, DOT) to CjState.Vowel(Jamo.YA),
            listOf(I, DOT, DOT, I) to CjState.Vowel(Jamo.YAE),
            listOf(EU) to CjState.Vowel(Jamo.EU),
            listOf(EU, DOT) to CjState.Vowel(Jamo.U),
            listOf(EU, DOT, DOT) to CjState.Vowel(Jamo.YU),
            listOf(EU, I) to CjState.Vowel(Jamo.UI),
            listOf(EU, DOT, I) to CjState.Vowel(Jamo.WI),
            listOf(EU, DOT, DOT, I) to CjState.Vowel(Jamo.WEO),
            listOf(EU, DOT, DOT, I, I) to CjState.Vowel(Jamo.WE),
            listOf(DOT) to CjState.Pending(1),
            listOf(DOT, DOT) to CjState.Pending(2),
            listOf(DOT, I) to CjState.Vowel(Jamo.EO),
            listOf(DOT, I, I) to CjState.Vowel(Jamo.E),
            listOf(DOT, DOT, I) to CjState.Vowel(Jamo.YEO),
            listOf(DOT, DOT, I, I) to CjState.Vowel(Jamo.YE),
            listOf(DOT, EU) to CjState.Vowel(Jamo.O),
            listOf(DOT, DOT, EU) to CjState.Vowel(Jamo.YO),
            listOf(DOT, EU, I) to CjState.Vowel(Jamo.OE),
            listOf(DOT, EU, I, DOT) to CjState.Vowel(Jamo.WA),
            listOf(DOT, EU, I, DOT, I) to CjState.Vowel(Jamo.WAE),
        )

        private fun stateOf(seq: List<CjElement>): CjState =
            if (seq.isEmpty()) CjState.Empty else TABLE.getValue(seq)
    }
}
