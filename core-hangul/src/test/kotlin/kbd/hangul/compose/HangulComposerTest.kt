package kbd.hangul.compose

import kbd.hangul.event.ComposeResult
import kbd.hangul.event.JamoEvent
import kbd.hangul.event.JamoEvent.Consonant
import kbd.hangul.event.JamoEvent.DoubleConsonant
import kbd.hangul.event.JamoEvent.ReplaceVowel
import kbd.hangul.event.JamoEvent.UndoLast
import kbd.hangul.event.JamoEvent.Vowel
import kbd.hangul.jamo.Jamo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** HangulComposer (LLD-04 §7; LLD-11 §2 HC-06..HC-31, HC-41..HC-44, HC-50..HC-53 (swipe double tap, last consonant), §2.1 BS-01..BS-05, BS-20, BS-21). */
class HangulComposerTest {

    private fun j(c: Char): Jamo = Jamo.ofCompat(c)!!

    /** Plain events from compatibility jamo: consonants → `Consonant`, vowels → `Vowel`. */
    private fun ev(s: String): List<JamoEvent> =
        s.map { c -> j(c).let { if (it.jungIndex >= 0) Vowel(it) else Consonant(it) } }

    private val reports = mutableListOf<String>()

    private fun composer(policy: ComposePolicy = ComposePolicy.DUBEOLSIK) =
        HangulComposer(policy) { reports += it }

    /** Feeds [events] one by one; returns committed text + composing text (what the editor shows). */
    private fun HangulComposer.type(events: List<JamoEvent>): String {
        val sb = StringBuilder()
        var last = ComposeResult("", "")
        for (e in events) {
            last = apply(e)
            sb.append(last.committed)
        }
        return sb.toString() + last.composing
    }

    private fun typed(s: String, policy: ComposePolicy = ComposePolicy.DUBEOLSIK) = composer(policy).type(ev(s))

    private fun HangulComposer.bs(): ComposeResult = (backspace() as BackspaceResult.Handled).result

    @Test
    fun `HC-06 g a composes ga`() = assertEquals("가", typed("ㄱㅏ"))

    @Test
    fun `HC-07 g a g composes gak`() = assertEquals("각", typed("ㄱㅏㄱ"))

    @Test
    fun `HC-08 d a r g composes compound final dalk`() = assertEquals("닭", typed("ㄷㅏㄹㄱ"))

    @Test
    fun `HC-09 all 11 compound finals`() {
        val pairs = listOf("ㄱㅅ", "ㄴㅈ", "ㄴㅎ", "ㄹㄱ", "ㄹㅁ", "ㄹㅂ", "ㄹㅅ", "ㄹㅌ", "ㄹㅍ", "ㄹㅎ", "ㅂㅅ")
        val got = pairs.joinToString(" ") { typed("ㄱㅏ$it") }
        assertEquals("갃 갅 갆 갉 갊 갋 갌 갍 갎 갏 값", got)
    }

    @Test
    fun `HC-10 dokkaebibul splits a compound final`() {
        val c = composer()
        c.type(ev("ㄷㅏㄹㄱ"))
        assertEquals(ComposeResult("달", "가"), c.apply(Vowel(j('ㅏ'))))
        assertEquals(listOf(Consonant(j('ㄱ')), Vowel(j('ㅏ'))), c.snapshot().units)
    }

    @Test
    fun `HC-11 dokkaebibul moves a single final`() {
        val c = composer()
        c.type(ev("ㄱㅏㄱ"))
        assertEquals(ComposeResult("가", "가"), c.apply(Vowel(j('ㅏ'))))
    }

    @Test
    fun `HC-12 dubeolsik merges o ae into wae`() = assertEquals("왜", typed("ㅇㅗㅐ"))

    @Test
    fun `HC-13 dubeolsik merges u e into we`() = assertEquals("뒈", typed("ㄷㅜㅔ"))

    @Test
    fun `HC-14 dubeolsik does not merge a i`() = assertEquals("가ㅣ", typed("ㄱㅏㅣ"))

    @Test
    fun `HC-15 vowel alone`() = assertEquals("ㅏ", typed("ㅏ"))

    @Test
    fun `HC-16 vowel then consonant commits the vowel`() {
        val c = composer()
        assertEquals(ComposeResult("", "ㅏ"), c.apply(Vowel(j('ㅏ'))))
        assertEquals(ComposeResult("ㅏ", "ㄱ"), c.apply(Consonant(j('ㄱ'))))
    }

    @Test
    fun `HC-17 two initials`() {
        val c = composer()
        c.apply(Consonant(j('ㄱ')))
        assertEquals(ComposeResult("ㄱ", "ㄱ"), c.apply(Consonant(j('ㄱ'))))
    }

    @Test
    fun `HC-18 dd is not a final`() {
        val c = composer()
        c.type(ev("ㄷㅏ"))
        assertEquals(ComposeResult("다", "ㄸ"), c.apply(Consonant(j('ㄸ'))))
    }

    @Test
    fun `HC-19 gg as final then vowel moves it`() {
        val c = composer()
        c.type(ev("ㄱㅏ"))
        assertEquals(ComposeResult("", "갂"), c.apply(Consonant(j('ㄲ'))))
        assertEquals(ComposeResult("가", "까"), c.apply(Vowel(j('ㅏ'))))
    }

    @Test
    fun `HC-20 ss as final`() = assertEquals("했", typed("ㅎㅐㅆ"))

    @Test
    fun `HC-21 swipe consecutive vowels never merge`() {
        assertEquals("가ㅣ", typed("ㄱㅏㅣ", ComposePolicy.SWIPE))    // FR-SWP-21, issue #156: 가 + ㅣ (was 개)
        assertEquals("가ㅏ", typed("ㄱㅏㅏ", ComposePolicy.SWIPE))
        assertEquals("고ㅜ", typed("ㄱㅗㅜ", ComposePolicy.SWIPE))    // FR-SWP-21: no merge, a new syllable
        assertEquals("가ㅣ", typed("ㄱㅏㅣ"))                          // dubeolsik table has no ㅏ + ㅣ
        assertTrue(reports.isEmpty())
    }

    @Test
    fun `HC-22 swipe does not merge dubeolsik pairs either`() {
        assertEquals("고ㅏ", typed("ㄱㅗㅏ", ComposePolicy.SWIPE))    // TBD-18 = B, issue #156 (dubeolsik: 과)
        assertEquals("과", typed("ㄱㅗㅏ"))
        assertEquals("그ㅣ", typed("ㄱㅡㅣ", ComposePolicy.SWIPE))
        assertEquals("과ㅣ", typed("ㄱㅘㅣ", ComposePolicy.SWIPE))    // the one-stroke ㅘ, then ㅣ: no 괘
        assertEquals("과", typed("ㄱㅘ", ComposePolicy.SWIPE))        // the one-stroke ㅘ is one vowel event
        assertEquals("가기", typed("ㄱㅏㄱㅣ", ComposePolicy.SWIPE))   // after a final: dokkaebibul, as dubeolsik
        assertEquals("갈기", typed("ㄱㅏㄹㄱㅣ", ComposePolicy.SWIPE)) // compound final splits, as dubeolsik
    }

    @Test
    fun `HC-24 ReplaceVowel replaces the vowel and is its own unit`() {
        val c = composer(ComposePolicy.GRAFFITI)
        c.apply(Consonant(j('ㄱ')))
        assertEquals("기", c.apply(Vowel(j('ㅣ'))).composing)
        assertEquals("가", c.apply(ReplaceVowel(j('ㅏ'))).composing)
        assertEquals("갸", c.apply(ReplaceVowel(j('ㅑ'))).composing)
        // One unit per cheonjiin element (issue #160): ⌫ takes back one element even after the assembler closed.
        assertEquals(listOf(Consonant(j('ㄱ')), Vowel(j('ㅣ')), ReplaceVowel(j('ㅏ')), ReplaceVowel(j('ㅑ'))), c.snapshot().units)
        c.apply(Consonant(j('ㄴ')))                                   // 갼
        assertEquals("갸", c.bs().composing)
        assertEquals("가", c.bs().composing)
        assertEquals("기", c.bs().composing)
        assertEquals("ㄱ", c.bs().composing)
        assertTrue(reports.isEmpty())
    }

    @Test
    fun `HC-24c ReplaceVowel after dokkaebibul stays in the new syllable`() {
        val c = composer(ComposePolicy.SWIPE)
        c.type(ev("ㄱㅏㄱ"))                                           // 각
        assertEquals(ComposeResult("가", "기"), c.apply(Vowel(j('ㅣ'))))
        assertEquals(ComposeResult("", "가"), c.apply(ReplaceVowel(j('ㅏ'))))
        assertEquals("기", c.bs().composing)
        assertEquals("ㄱ", c.bs().composing)
    }

    @Test
    fun `HC-25 ReplaceVowel with a final is ignored and reported once`() {
        val c = composer(ComposePolicy.GRAFFITI)
        c.type(ev("ㄱㅏㄱ"))
        val before = c.snapshot()
        assertEquals(ComposeResult("", "각"), c.apply(ReplaceVowel(j('ㅓ'))))
        assertEquals(before, c.snapshot())
        assertEquals(1, reports.size)
    }

    @Test
    fun `HC-25 ReplaceVowel with no vowel is ignored and reported once`() {
        val c = composer(ComposePolicy.GRAFFITI)
        assertEquals(ComposeResult("", ""), c.apply(ReplaceVowel(j('ㅓ'))))
        c.apply(Consonant(j('ㄱ')))
        assertEquals(ComposeResult("", "ㄱ"), c.apply(ReplaceVowel(j('ㅓ'))))
        assertEquals(listOf(Consonant(j('ㄱ'))), c.snapshot().units)
        assertEquals(2, reports.size)
    }

    @Test
    fun `HC-24 ReplaceVowel after a merged medial folds back the same`() {
        val c = composer()   // dubeolsik: the only policy that merges (issue #156)
        c.type(ev("ㄱㅗㅏ"))
        assertEquals(ComposeResult("", "거"), c.apply(ReplaceVowel(j('ㅓ'))))
        assertEquals(listOf(Consonant(j('ㄱ')), Vowel(j('ㅗ')), Vowel(j('ㅏ')), ReplaceVowel(j('ㅓ'))), c.snapshot().units)
        assertEquals("거", c.composingText())
        assertEquals("과", c.bs().composing)
    }

    @Test
    fun `HC-28 DoubleConsonant with no double is ignored and reported once`() {
        val c = composer()
        c.type(ev("ㄱㅏ"))
        assertEquals(ComposeResult("", "가"), c.apply(DoubleConsonant(j('ㄴ'))))
        assertEquals(2, c.snapshot().units.size)
        assertEquals(1, reports.size)
    }

    @Test
    fun `HC-26 applyAll returns one result without the intermediate syllable`() {
        val c = composer(ComposePolicy.SWIPE)
        c.type(ev("ㄱㅗ"))
        assertEquals(ComposeResult("고", "구"), c.applyAll(listOf(Consonant(j('ㄱ')), Vowel(j('ㅜ')))))
    }

    @Test
    fun `HC-27 render hih is U+D7A3`() {
        val s = typed("ㅎㅣㅎ")
        assertEquals("힣", s)
        assertEquals(0xD7A3, s.single().code)
    }

    @Test
    fun `HC-28 DoubleConsonant is the double consonant and stored as is`() {
        val c = composer()
        assertEquals(ComposeResult("", "ㄲ"), c.apply(DoubleConsonant(j('ㄱ'))))
        assertEquals(listOf<JamoEvent>(DoubleConsonant(j('ㄱ'))), c.snapshot().units)
        assertEquals("까", c.apply(Vowel(j('ㅏ'))).composing)
    }

    @Test
    fun `HC-29 finish returns the syllable and clears`() {
        val c = composer()
        c.type(ev("ㄱㅏㄱ"))
        assertEquals("각", c.finish())
        assertFalse(c.isComposing())
        assertEquals("", c.composingText())
    }

    @Test
    fun `HC-30 hasJong`() {
        val c = composer()
        c.type(ev("ㄱㅏ"))
        assertFalse(c.hasJong())
        c.apply(Consonant(j('ㄱ')))
        assertTrue(c.hasJong())
        c.finish()
        c.type(ev("ㄷㅏㄹㄱ"))
        assertTrue(c.hasJong())
        assertEquals(j('ㅏ'), c.currentVowel())
    }

    @Test
    fun `HC-31 snapshot and restore`() {
        val c = composer()
        c.type(ev("ㄷㅏㄹㄱ"))
        val snap = c.snapshot()
        c.apply(Vowel(j('ㅏ')))
        c.restore(snap)
        assertEquals("닭", c.composingText())
        assertEquals(4, c.snapshot().units.size)
    }

    @Test
    fun `HC-41 undoLast removes the final`() {
        val c = composer()
        c.type(ev("ㄱㅏㄱ"))
        assertEquals(ComposeResult("", "가"), c.undoLast())
        assertEquals(ComposeResult("", "ㄱ"), c.apply(UndoLast))
    }

    @Test
    fun `HC-42 undoLast after a new syllable does not reopen the committed one`() {
        val c = composer()
        c.type(ev("ㅂㅏㄲ"))
        assertEquals(ComposeResult("밖", "ㄱ"), c.apply(Consonant(j('ㄱ'))))
        assertEquals(ComposeResult("", ""), c.apply(UndoLast))
        assertFalse(c.isComposing())
    }

    @Test
    fun `HC-43 undoLast on empty does nothing`() {
        val c = composer()
        assertEquals(ComposeResult("", ""), c.undoLast())
        assertFalse(c.isComposing())
        assertTrue(reports.isEmpty())
    }

    @Test
    fun `HC-44 undoLast after dokkaebibul stops at the commit boundary`() {
        val c = composer()
        assertEquals("가까", c.type(ev("ㄱㅏㄲㅏ")))
        assertEquals(ComposeResult("", "ㄲ"), c.undoLast())
    }

    @Test
    fun `BS-01 dubeolsik dalk backspace`() {
        val c = composer()
        c.type(ev("ㄷㅏㄹㄱ"))
        assertEquals(ComposeResult("", "달"), c.bs())
    }

    @Test
    fun `BS-02 dubeolsik dalk backspace three times`() {
        val c = composer()
        c.type(ev("ㄷㅏㄹㄱ"))
        assertEquals(listOf("달", "다", "ㄷ"), List(3) { c.bs().composing })
    }

    @Test
    fun `BS-03 dubeolsik gwa backspace`() {
        val c = composer()
        assertEquals("과", c.type(ev("ㄱㅗㅏ")))
        assertEquals("고", c.bs().composing)
    }

    @Test
    fun `BS-04 dubeolsik gak backspace`() {
        val c = composer()
        c.type(ev("ㄱㅏㄱ"))
        assertEquals("가", c.bs().composing)
    }

    @Test
    fun `BS-05 dubeolsik gaga backspace`() {
        val c = composer()
        val shown = c.type(ev("ㄱㅏㄱㅏ"))
        assertEquals("가가", shown)
        assertEquals("ㄱ", c.bs().composing)   // the committed 가 stays: 가ㄱ
        assertEquals("", c.bs().composing)
        assertEquals(BackspaceResult.NothingComposing, c.backspace())
    }

    @Test
    fun `BS-20 swipe backspace removes one jamo at a time`() {
        val c = composer(ComposePolicy.SWIPE)
        c.type(ev("ㄱㅐ"))                              // 개 = ㄱ tap + one-stroke ㅐ (issue #156)
        assertEquals("개", c.composingText())
        assertEquals("ㄱ", c.bs().composing)          // one vowel swipe = one unit (FR-CMN-07)
        assertEquals("", c.bs().composing)
        assertFalse(c.isComposing())

        c.type(ev("ㄱㅘ"))                              // 과 = one-stroke ㅘ: one ⌫ removes it (SWIPE 5.2)
        assertEquals("ㄱ", c.bs().composing)
        assertEquals(j('ㄱ'), c.lastConsonant())
        c.bs()

        c.type(ev("ㄱㅏㅣ"))                            // 가ㅣ: ⌫ removes the lone ㅣ, the committed 가 stays
        assertEquals("", c.bs().composing)
        assertEquals(BackspaceResult.NothingComposing, c.backspace())

        c.type(ev("ㄷㅏㄹㄱ"))
        assertEquals("닭", c.composingText())
        assertEquals("달", c.bs().composing)          // SRS 3.1 swipe row: 닭 ⌫⌫⌫ = 달, 다, ㄷ
        assertEquals("다", c.bs().composing)
        assertEquals("ㄷ", c.bs().composing)
    }

    @Test
    fun `BS-21 swipe backspace leaves the committed syllable`() {
        val c = composer(ComposePolicy.SWIPE)
        assertEquals("가가", c.type(ev("ㄱㅏㄱㅏ")))
        assertEquals(ComposeResult("", "ㄱ"), c.bs())   // 가ㄱ: the committed 가 is not reopened
        assertEquals(ComposeResult("", ""), c.bs())
        assertEquals(BackspaceResult.NothingComposing, c.backspace())
        assertNull(c.currentVowel())
    }

    // --- Swipe double tap and last consonant (HLD 4.3, issue #150) ----------------------------

    @Test
    fun `HC-50 UndoLast then DoubleConsonant turns the tap just typed into a double`() {
        val c = composer(ComposePolicy.SWIPE)
        c.type(ev("ㅂㅏㄱ"))                                                              // 박
        assertEquals(ComposeResult("", "밖"), c.applyAll(listOf(UndoLast, DoubleConsonant(j('ㄱ')))))
        assertEquals(ComposeResult("바", "까"), c.apply(Vowel(j('ㅏ'))))                   // 바까: dokkaebibul moves ㄲ

        val d = composer(ComposePolicy.SWIPE)
        d.type(ev("ㄷㅏㄹㄱ"))                                                            // 닭
        assertEquals(ComposeResult("달", "ㄲ"), d.applyAll(listOf(UndoLast, DoubleConsonant(j('ㄱ')))))   // ㄹ + ㄲ is no compound

        val e = composer(ComposePolicy.SWIPE)
        e.type(ev("ㅇㅏㅂ"))                                                              // 압
        assertEquals(ComposeResult("아", "ㅃ"), e.applyAll(listOf(UndoLast, DoubleConsonant(j('ㅂ')))))   // ㅃ cannot be a final
        assertEquals("빠", e.apply(Vowel(j('ㅏ'))).composing)
        assertTrue(reports.isEmpty())
    }

    @Test
    fun `HC-51 a double final moves whole on dokkaebibul`() {
        val c = composer(ComposePolicy.SWIPE)
        c.type(ev("ㄱㅗㄱ"))                                                              // 곡
        assertEquals(ComposeResult("", "곢"), c.applyAll(listOf(UndoLast, DoubleConsonant(j('ㄱ')))))
        assertEquals(ComposeResult("고", "끼"), c.apply(Vowel(j('ㅣ'))))                   // 고끼
    }

    @Test
    fun `HC-52 lastConsonant is the last consonant of the composing syllable`() {
        val c = composer(ComposePolicy.SWIPE)
        assertNull(c.lastConsonant())
        c.type(ev("ㄷㅏㄹㄱ"))
        assertEquals(j('ㄱ'), c.lastConsonant())   // compound final: the second consonant
        c.bs()
        assertEquals(j('ㄹ'), c.lastConsonant())
        c.bs()
        assertEquals(j('ㄷ'), c.lastConsonant())   // no final: the initial
        c.finish()
        c.type(ev("ㅂㅏ"))
        c.applyAll(listOf(Consonant(j('ㄱ')), UndoLast, DoubleConsonant(j('ㄱ'))))
        assertEquals(j('ㄲ'), c.lastConsonant())   // a double as itself
        c.finish()
        c.apply(Vowel(j('ㅏ')))
        assertNull(c.lastConsonant())              // only a vowel
    }

    @Test
    fun `HC-53 UndoLast on an empty list does nothing and snapshot restores the units`() {
        val c = composer(ComposePolicy.SWIPE)
        assertEquals(ComposeResult("", ""), c.apply(UndoLast))
        c.type(ev("ㄱㅏㄱ"))
        val s = c.snapshot()
        c.finish()
        c.restore(s)
        assertEquals("각", c.composingText())
        assertEquals(ComposeResult("", "가"), c.apply(UndoLast))
        assertTrue(reports.isEmpty())
    }

    @Test
    fun `HC-60 a compound final typed at once equals its two consonants after a CV syllable (issue 242)`() {
        val pairs = mapOf('ㄳ' to "ㄱㅅ", 'ㄵ' to "ㄴㅈ", 'ㄶ' to "ㄴㅎ", 'ㄺ' to "ㄹㄱ", 'ㄻ' to "ㄹㅁ", 'ㄼ' to "ㄹㅂ",
            'ㄽ' to "ㄹㅅ", 'ㄾ' to "ㄹㅌ", 'ㄿ' to "ㄹㅍ", 'ㅀ' to "ㄹㅎ", 'ㅄ' to "ㅂㅅ")
        assertEquals(11, pairs.size)
        for ((c, two) in pairs) {
            assertEquals("$c", typed("ㄱㅏ$two"), typed("ㄱㅏ$c"))                     // 갃 … 값
            assertEquals("$c + ㅏ", typed("ㄱㅏ${two}ㅏ"), typed("ㄱㅏ${c}ㅏ"))        // dokkaebibul: 갈가 …
        }
        assertEquals("갉", typed("ㄱㅏㄺ"))
        assertEquals("갈가", typed("ㄱㅏㄺㅏ"))
        val c = composer()
        c.type(ev("ㄱㅏㄺ"))
        assertEquals("갈", c.bs().composing)                                              // ⌫ takes the second consonant, as two taps
    }

    @Test
    fun `HC-61 a compound final where it cannot be a final stands alone and a vowel splits it (issue 242)`() {
        assertEquals("ㄺ", typed("ㄺ"))                     // nothing composing
        assertEquals("ㄹ가", typed("ㄺㅏ"))                 // ㄺ + ㅏ: the second consonant takes the vowel
        assertEquals("각ㄺ", typed("ㄱㅏㄱㄺ"))             // a final already: commit, ㄺ alone
        assertEquals("ㄱㄳ", typed("ㄱㄳ"))                 // an initial alone: commit, ㄳ alone
        assertEquals("ㄺㄱ", typed("ㄺㄱ"))                 // a consonant after it commits it
        val c = composer()
        c.type(ev("ㄺ"))
        assertEquals("", c.bs().composing)                  // ⌫ takes the lone compound away
    }
}
