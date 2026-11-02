package kbd.hangul.cheonjiin

import kbd.hangul.cheonjiin.CjElement.DOT
import kbd.hangul.cheonjiin.CjElement.EU
import kbd.hangul.cheonjiin.CjElement.I
import kbd.hangul.jamo.Jamo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** LLD-11 §6.2 CJ-01..CJ-41 (LLD-05 §4). */
class CheonjiinAssemblerTest {

    /** Builds an assembler by pushing [seq]; every push must be accepted. */
    private fun asm(vararg seq: CjElement) = CheonjiinAssembler().apply {
        for (e in seq) {
            val r = push(e)
            assertTrue("$e rejected: $r", r !is CjPush.Ignored && r !is CjPush.NotExtendable)
        }
    }

    private fun assertState(expected: CjState, vararg seq: CjElement) {
        val a = asm(*seq)
        assertEquals(expected, a.state)
        assertEquals(seq.toList(), a.elements)
    }

    private fun v(j: Jamo) = CjState.Vowel(j)

    @Test fun `CJ-01 I is ㅣ`() = assertState(v(Jamo.I), I)
    @Test fun `CJ-02 I DOT is ㅏ`() = assertState(v(Jamo.A), I, DOT)
    @Test fun `CJ-03 I DOT I is ㅐ`() = assertState(v(Jamo.AE), I, DOT, I)
    @Test fun `CJ-04 I DOT DOT is ㅑ`() = assertState(v(Jamo.YA), I, DOT, DOT)
    @Test fun `CJ-05 I DOT DOT I is ㅒ`() = assertState(v(Jamo.YAE), I, DOT, DOT, I)
    @Test fun `CJ-06 DOT is Pending(1)`() = assertState(CjState.Pending(1), DOT)
    @Test fun `CJ-07 DOT DOT is Pending(2)`() = assertState(CjState.Pending(2), DOT, DOT)
    @Test fun `CJ-08 DOT I is ㅓ`() = assertState(v(Jamo.EO), DOT, I)
    @Test fun `CJ-09 DOT I I is ㅔ`() = assertState(v(Jamo.E), DOT, I, I)
    @Test fun `CJ-10 DOT DOT I is ㅕ`() = assertState(v(Jamo.YEO), DOT, DOT, I)
    @Test fun `CJ-11 empty is Empty`() = assertState(CjState.Empty)
    @Test fun `CJ-12 EU DOT DOT I is ㅝ`() = assertState(v(Jamo.WEO), EU, DOT, DOT, I)
    @Test fun `CJ-13 DOT DOT I I is ㅖ`() = assertState(v(Jamo.YE), DOT, DOT, I, I)
    @Test fun `CJ-14 DOT EU is ㅗ`() = assertState(v(Jamo.O), DOT, EU)
    @Test fun `CJ-15 DOT DOT EU is ㅛ`() = assertState(v(Jamo.YO), DOT, DOT, EU)
    @Test fun `CJ-16 EU is ㅡ`() = assertState(v(Jamo.EU), EU)
    @Test fun `CJ-17 EU DOT is ㅜ`() = assertState(v(Jamo.U), EU, DOT)
    @Test fun `CJ-18 EU DOT DOT is ㅠ`() = assertState(v(Jamo.YU), EU, DOT, DOT)
    @Test fun `CJ-19 EU I is ㅢ`() = assertState(v(Jamo.UI), EU, I)
    @Test fun `CJ-20 DOT EU I is ㅚ`() = assertState(v(Jamo.OE), DOT, EU, I)
    @Test fun `CJ-21 DOT EU I DOT is ㅘ`() = assertState(v(Jamo.WA), DOT, EU, I, DOT)
    @Test fun `CJ-22 DOT EU I DOT I is ㅙ`() = assertState(v(Jamo.WAE), DOT, EU, I, DOT, I)
    @Test fun `CJ-23 EU DOT I is ㅟ`() = assertState(v(Jamo.WI), EU, DOT, I)
    @Test fun `CJ-24 EU DOT DOT I I is ㅞ`() = assertState(v(Jamo.WE), EU, DOT, DOT, I, I)

    @Test
    fun `CJ-25 third DOT after DOT DOT is Ignored and stays Pending(2)`() {
        val a = asm(DOT, DOT)
        assertEquals(CjPush.Ignored, a.push(DOT))
        assertEquals(CjState.Pending(2), a.state)
        assertEquals(listOf(DOT, DOT), a.elements)
    }

    @Test
    fun `CJ-26 DOT after ㅐ is NotExtendable and changes nothing`() {
        val a = asm(I, DOT, I)
        assertEquals(CjPush.NotExtendable, a.push(DOT))
        assertEquals(v(Jamo.AE), a.state)
        assertEquals(listOf(I, DOT, I), a.elements)
    }

    @Test
    fun `CJ-27 push result kinds`() {
        assertEquals(CjPush.Started(Jamo.I), CheonjiinAssembler().push(I))
        assertEquals(CjPush.PendingShown(1), CheonjiinAssembler().push(DOT))
        assertEquals(CjPush.Changed(Jamo.A), asm(I).push(DOT))
        assertEquals(CjPush.Started(Jamo.EO), asm(DOT).push(I))
    }

    @Test
    fun `CJ-28 pendingGlyph`() {
        assertEquals("ㆍ", asm(DOT).pendingGlyph())
        assertEquals("‥", asm(DOT, DOT).pendingGlyph())
        assertEquals("", asm(I, DOT).pendingGlyph())
    }

    @Test
    fun `CJ-29 close from ㅏ is Empty`() {
        val a = asm(I, DOT)
        a.close()
        assertEquals(CjState.Empty, a.state)
        assertEquals(emptyList<CjElement>(), a.elements)
    }

    @Test
    fun `CJ-30 clear from Pending(2) is Empty`() {
        val a = asm(DOT, DOT)
        a.clear()
        assertEquals(CjState.Empty, a.state)
        assertEquals("", a.pendingGlyph())
    }

    @Test fun `CJ-31 pop I DOT is ToVowel(ㅣ)`() = assertEquals(CjPop.ToVowel(Jamo.I), asm(I, DOT).pop())
    @Test fun `CJ-32 pop I is ToNone(wasVowel = true)`() = assertEquals(CjPop.ToNone(wasVowel = true), asm(I).pop())
    @Test fun `CJ-33 pop DOT I is ToPending(1)`() = assertEquals(CjPop.ToPending(1), asm(DOT, I).pop())
    @Test fun `CJ-34 pop DOT DOT is ToPending(1)`() = assertEquals(CjPop.ToPending(1), asm(DOT, DOT).pop())
    @Test fun `CJ-35 pop DOT is ToNone(wasVowel = false)`() = assertEquals(CjPop.ToNone(wasVowel = false), asm(DOT).pop())
    @Test fun `CJ-36 pop DOT EU I DOT is ToVowel(ㅚ)`() = assertEquals(CjPop.ToVowel(Jamo.OE), asm(DOT, EU, I, DOT).pop())
    @Test fun `CJ-37 pop DOT DOT I is ToPending(2)`() = assertEquals(CjPop.ToPending(2), asm(DOT, DOT, I).pop())

    @Test
    fun `CJ-38 pop empty is Empty`() {
        val a = CheonjiinAssembler()
        assertEquals(CjPop.Empty, a.pop())
        assertEquals(CjState.Empty, a.state)
    }

    @Test
    fun `CJ-39 pop then push DOT gives Changed(ㅏ)`() {
        val a = asm(I, DOT)
        a.pop()
        assertEquals(CjPush.Changed(Jamo.A), a.push(DOT))
    }

    @Test
    fun `CJ-40 pop to Pending(1) then push EU gives Started(ㅗ)`() {
        val a = asm(DOT, I)
        assertEquals(CjPop.ToPending(1), a.pop())
        assertEquals(CjPush.Started(Jamo.O), a.push(EU))
    }

    @Test
    fun `CJ-41 elements that cannot extend a vowel are dropped`() {
        val cases = listOf(
            Triple(listOf(I), I, Jamo.I),
            Triple(listOf(DOT, EU), DOT, Jamo.O),
            Triple(listOf(EU), EU, Jamo.EU),
            Triple(listOf(EU, DOT, DOT), DOT, Jamo.YU),
        )
        for ((seq, e, vowel) in cases) {
            val a = asm(*seq.toTypedArray())
            assertEquals("$seq + $e", CjPush.NotExtendable, a.push(e))
            assertEquals("$seq + $e", v(vowel), a.state)
            assertEquals("$seq + $e", seq, a.elements)
        }
        // The dropped ㅣ is not a ⌫ target: one pop removes the first ㅣ.
        val a = asm(I)
        a.push(I)
        assertEquals(CjPop.ToNone(wasVowel = true), a.pop())
        assertEquals(CjState.Empty, a.state)
    }

    @Test
    fun `TABLE has the 23 sequences and every prefix is in it`() {
        assertEquals(23, CheonjiinAssembler.TABLE.size)
        for (seq in CheonjiinAssembler.TABLE.keys) for (n in 1 until seq.size) {
            assertTrue("prefix of $seq", seq.take(n) in CheonjiinAssembler.TABLE)
        }
    }

    @Test
    fun `snapshot and restore copy the elements by value`() {
        val a = asm(DOT, EU, I)
        val s = a.snapshot()
        a.push(DOT)
        a.clear()
        a.push(I)
        a.restore(s)
        assertEquals(listOf(DOT, EU, I), a.elements)
        assertEquals(v(Jamo.OE), a.state)
        a.push(DOT)
        assertEquals(listOf(DOT, EU, I), s.elements)
    }
}
