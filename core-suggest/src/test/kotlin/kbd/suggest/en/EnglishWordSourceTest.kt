package kbd.suggest.en

import kbd.suggest.Candidate
import kbd.suggest.EditCommand.DeleteBefore
import kbd.suggest.EditCommand.Insert
import kbd.suggest.EditPlan
import kbd.suggest.KeyAnswer
import kbd.suggest.OnFail
import kbd.suggest.ResetReason
import kbd.suggest.SuggestKey
import kbd.suggest.TextPeek
import kbd.suggest.en.EnglishWordSource.State
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * LLD-11 §7.12 (다) ES-01..ES-36 (LLD-12 §7) with the test dictionary and a fake [TextPeek].
 *
 * Notation of LLD-11: N(t)/N(f) = None(prevKnown), T(w) = Typing, B = Blocked, J(p, t) = JustPicked.
 * One "event" = onKey, then afterKey when the answer is Pass or WriteThenPass.
 */
class EnglishWordSourceTest {

    /** Returns the given values and counts the calls. */
    class FakePeek(
        private val before: String? = "",
        private val after: String? = "",
        private val selection: Boolean? = false,
    ) : TextPeek {
        var beforeCalls = 0
        var afterCalls = 0
        var selectionCalls = 0
        val calls get() = beforeCalls + afterCalls + selectionCalls

        override fun before(n: Int): String? { beforeCalls++; return before }
        override fun after(n: Int): String? { afterCalls++; return after }
        override fun hasSelection(): Boolean? { selectionCalls++; return selection }
    }

    /** Drives one source and records everything it says, for ES-36. */
    internal class Driver(index: WordIndex) {
        val src = EnglishWordSource(index)
        val trace = ArrayList<String>()
        var peeks = 0

        internal val state: State get() = src.state
        val cells: List<String> get() = src.candidates.map { it.text }

        fun key(k: SuggestKey, peek: FakePeek = FakePeek()): KeyAnswer {
            val a = src.onKey(k, peek)
            if (a is KeyAnswer.Pass || a is KeyAnswer.WriteThenPass) src.afterKey(k)
            peeks += peek.calls
            trace += "key $k -> $a; ${peek.calls} peeks; ${src.state}; ${src.candidates}"
            return a
        }

        fun text(s: String, peek: FakePeek = FakePeek()) = key(SuggestKey.Text(s), peek)

        /** Each character as its own Text key. */
        fun type(s: String) = s.map { text(it.toString()) }

        fun pick(i: Int): EditPlan? = src.pick(i).also { trace += "pick $i -> $it; ${src.state}; ${src.candidates}" }

        fun failed() = src.onWriteFailed().also { trace += "failed; ${src.state}" }

        fun reset(r: ResetReason) = src.reset(r).also { trace += "reset $r; ${src.state}" }

        /** N(t): a space typed with no word. */
        fun nT() = apply { key(SuggestKey.Space); assertEquals(N_T, state) }

        /** T(w) typed after a space. */
        fun typing(w: String) = apply { nT(); type(w); assertEquals(State.Typing(w), state) }

        /** J(they, the). */
        fun justPicked() = apply {
            typing("the")
            assertEquals(EditPlan("the", listOf(Insert("y "))), pick(0))
            assertEquals(State.JustPicked("they", "the"), state)
        }
    }

    private fun d(index: WordIndex = DICT) = Driver(index)

    // ES-01..ES-34 bodies take a driver factory so ES-36 can run them twice and compare the traces.

    private fun es01(mk: () -> Driver) {
        val x = mk().nT()
        for (c in listOf("h", "e", "l")) {
            assertEquals(KeyAnswer.Pass, x.text(c))
            assertEquals(listOf("hello", "help", "held"), x.cells)
        }
        assertEquals(0, x.peeks)
    }

    private fun es02(mk: () -> Driver) {
        val x = mk()
        val p = FakePeek("", "", false)
        x.text("h", p)
        assertEquals(State.Typing("h"), x.state)
        assertEquals(listOf(1, 1, 1), listOf(p.beforeCalls, p.afterCalls, p.selectionCalls))
        val y = mk()
        y.text("h", FakePeek(" ", "\n", false))
        assertEquals(State.Typing("h"), y.state)
    }

    private fun es03(mk: () -> Driver) {
        val x = mk()
        x.text("h", FakePeek(before = "a"))
        assertEquals(State.Blocked, x.state)
        assertEquals(emptyList<String>(), x.cells)
        assertEquals(State.Blocked, mk().also { it.text("h", FakePeek(after = "x")) }.state)
        assertEquals(State.Typing("h"), mk().also { it.text("h", FakePeek(before = "'")) }.state)
        assertEquals(State.Typing("h"), mk().also { it.text("h", FakePeek(after = "'")) }.state)
    }

    private fun es04(mk: () -> Driver) {
        for ((peek, calls) in listOf(
            FakePeek(before = null) to 1,
            FakePeek(before = "", after = null) to 2,
            FakePeek(before = "", after = "", selection = null) to 3,
            FakePeek(selection = true) to 3,
        )) {
            val x = mk()
            x.text("h", peek)
            assertEquals(State.Blocked, x.state)
            assertEquals(calls, peek.calls)
        }
    }

    private fun es05(mk: () -> Driver) {
        val x = mk()
        x.text("h", FakePeek(before = "a"))
        for (c in listOf("e", "l", "l")) {
            x.text(c)
            assertEquals(State.Blocked, x.state)
            assertEquals(emptyList<String>(), x.cells)
        }
        x.key(SuggestKey.Space)
        assertEquals(N_T, x.state)
        val before = x.peeks
        x.text("h")
        assertEquals(State.Typing("h"), x.state)
        assertEquals(before, x.peeks)
    }

    private fun es06(mk: () -> Driver) {
        val x = mk().nT()
        x.type("don'")
        assertEquals(State.Typing("don'"), x.state)
        assertEquals(listOf("don't"), x.cells)
        x.text("'")
        assertEquals(N_T, x.state)
    }

    private fun es07(mk: () -> Driver) {
        val x = mk()
        x.text("'")
        assertEquals(N_T, x.state)
        x.text("t")
        assertEquals(State.Typing("t"), x.state)
        assertEquals(0, x.peeks)
    }

    private fun es08(mk: () -> Driver) {
        for (k in listOf<SuggestKey>(
            SuggestKey.Text("1"), SuggestKey.Text("-"), SuggestKey.Text("¹"), SuggestKey.Text("ab"), SuggestKey.Space,
        )) {
            val x = mk().typing("hel")
            assertEquals(KeyAnswer.Pass, x.key(k))
            assertEquals(N_T, x.state)
            assertEquals(emptyList<String>(), x.cells)
        }
    }

    private fun es09(mk: () -> Driver) {
        for (k in listOf(SuggestKey.Enter, SuggestKey.Tab, SuggestKey.Navigation, SuggestKey.Function)) {
            val x = mk().typing("hel")
            assertEquals(KeyAnswer.Pass, x.key(k))
            assertEquals(N_F, x.state)
            val p = FakePeek()
            x.text("h", p)
            assertEquals(3, p.calls)
            assertEquals(State.Typing("h"), x.state)
        }
    }

    private fun es10(mk: () -> Driver) {
        val x = mk().typing("hel")
        x.key(SuggestKey.Backspace)
        assertEquals(State.Typing("he"), x.state)
        assertEquals(listOf("hello", "help", "held"), x.cells)
        val y = mk().typing("hel")
        repeat(3) { y.key(SuggestKey.Backspace) }
        assertEquals(N_T, y.state)
        val p = FakePeek()
        y.text("h", p)
        assertEquals(State.Typing("h"), y.state)
        assertEquals(0, p.calls)
    }

    private fun es11(mk: () -> Driver) {
        val x = mk().nT()
        x.key(SuggestKey.Backspace)
        assertEquals(N_F, x.state)
        x.text("o", FakePeek(before = "d"))
        assertEquals(State.Blocked, x.state)
    }

    private fun es12(mk: () -> Driver) {
        val x = mk().nT()
        val w = "abcdefghijklmnopqrstuvwxyzabcdef"
        x.type(w)
        assertEquals(State.Typing(w), x.state)
        x.text("g")
        assertEquals(State.Blocked, x.state)
        assertEquals(emptyList<String>(), x.cells)
        x.key(SuggestKey.Backspace)
        assertEquals(State.Blocked, x.state)
    }

    private fun es13(mk: () -> Driver) {
        val x = mk().nT()
        x.type("hEl")
        assertEquals(emptyList<String>(), x.cells)
        repeat(2) { x.key(SuggestKey.Backspace) }
        assertEquals(State.Typing("h"), x.state)
        assertEquals(listOf("hello", "help", "held"), x.cells)
    }

    private fun es14(mk: () -> Driver) {
        val expect = linkedMapOf(
            "t" to listOf("the", "to", "that"), "th" to listOf("the", "that", "they"),
            "the" to listOf("they", "there", "then"), "i" to listOf("in", "is", "I"), "I" to listOf("In", "Is", "It"),
            "i'" to listOf("I'm"), "do" to listOf("don't", "done", "don"), "don" to listOf("don't", "done"),
        )
        for ((w, cells) in expect) assertEquals(w, cells, mk().typing(w).cells)
    }

    private fun es15(mk: () -> Driver) {
        val expect = linkedMapOf(
            "mon" to listOf("Monday", "money", "month"), "Mon" to listOf("Monday", "Money", "Month"),
            "MON" to listOf("MONDAY", "MONEY", "MONTH"), "mOn" to emptyList(), "mc" to listOf("McDonald"),
            "Mc" to listOf("McDonald"), "MC" to listOf("MCDONALD"), "rock'" to listOf("rock'n'roll"),
        )
        for ((w, cells) in expect) assertEquals(w, cells, mk().typing(w).cells)
    }

    private fun es16(mk: () -> Driver) {
        assertEquals(listOf(Candidate("xyz", kind = Candidate.Kind.TYPED)), mk().typing("xyz").src.candidates)
        assertEquals(listOf(Candidate("Xyz", kind = Candidate.Kind.TYPED)), mk().typing("Xyz").src.candidates)
        assertEquals(emptyList<Candidate>(), mk().typing("helm").src.candidates)
        assertTrue(mk().typing("the").src.candidates.all { it.kind == Candidate.Kind.WORD })
    }

    private fun es17(mk: () -> Driver) {
        val x = mk().typing("the")
        assertEquals(EditPlan("the", listOf(Insert("y "))), x.pick(0))
        assertEquals(State.JustPicked("they", "the"), x.state)
        assertEquals(emptyList<String>(), x.cells)
    }

    private fun es18(mk: () -> Driver) {
        assertEquals(EditPlan("mon", listOf(DeleteBefore(3), Insert("Monday "))), mk().typing("mon").pick(0))
        assertEquals(EditPlan("i", listOf(DeleteBefore(1), Insert("I "))), mk().typing("i").pick(2))
        assertEquals(EditPlan("Mon", listOf(Insert("day "))), mk().typing("Mon").pick(0))
    }

    private fun es19(mk: () -> Driver) {
        val x = mk().typing("xyz")
        assertEquals(EditPlan("xyz", listOf(Insert(" "))), x.pick(0))
        assertEquals(State.JustPicked("xyz", "xyz"), x.state)
    }

    private fun es20(mk: () -> Driver) {
        val x = mk().typing("the")
        assertNull(x.pick(3))
        assertEquals(State.Typing("the"), x.state)
        val y = mk().nT()
        assertNull(y.pick(0))
        assertEquals(N_T, y.state)
        val z = mk().justPicked()
        assertNull(z.pick(0))
        assertEquals(State.JustPicked("they", "the"), z.state)
        assertNull(mk().typing("the").pick(-1))
    }

    private fun es21(mk: () -> Driver) {
        for (p in WordChars.PUNCT_JOIN) {
            val x = mk().justPicked()
            assertEquals("$p", KeyAnswer.WriteThenPass(EditPlan(" ", listOf(DeleteBefore(1)))), x.text("$p"))
            assertEquals("$p", if (p == '\'') State.Typing("they'") else N_T, x.state)
        }
        assertEquals(10, WordChars.PUNCT_JOIN.length)
    }

    private fun es22(mk: () -> Driver) {
        val x = mk().typing("do")
        assertEquals(EditPlan("do", listOf(Insert("n "))), x.pick(2))
        assertEquals(KeyAnswer.WriteThenPass(EditPlan(" ", listOf(DeleteBefore(1)))), x.text("'"))
        assertEquals(State.Typing("don'"), x.state)
        assertEquals(listOf("don't"), x.cells)
    }

    private fun es23(mk: () -> Driver) {
        for (s in listOf("(", "\"", "1", "-", "¹")) {
            val x = mk().justPicked()
            assertEquals(s, KeyAnswer.Pass, x.text(s))
            assertEquals(s, N_T, x.state)
        }
    }

    private fun es24(mk: () -> Driver) {
        val x = mk().justPicked()
        repeat(2) {
            assertEquals(KeyAnswer.Consume(EditPlan.NONE, OnFail.NOTHING), x.key(SuggestKey.Space))
            assertEquals(State.JustPicked("they", "the"), x.state)
        }
        assertEquals(KeyAnswer.WriteThenPass(EditPlan(" ", listOf(DeleteBefore(1)))), x.text("."))
    }

    private fun es25(mk: () -> Driver): Driver {
        val x = mk().justPicked()
        assertEquals(UNDO_THEY, x.key(SuggestKey.Backspace))
        assertEquals(State.Typing("the"), x.state)
        assertEquals(listOf("they", "there", "then"), x.cells)
        return x
    }

    private fun es26(mk: () -> Driver) {
        val x = es25(mk)
        assertEquals(KeyAnswer.Pass, x.key(SuggestKey.Backspace))
        assertEquals(State.Typing("th"), x.state)
        val y = mk().justPicked()
        repeat(2) { y.key(SuggestKey.Space) }
        assertEquals(UNDO_THEY, y.key(SuggestKey.Backspace))
        assertEquals(State.Typing("the"), y.state)
    }

    private fun es27(mk: () -> Driver) {
        val x = mk().justPicked()
        x.text(".")
        assertEquals(N_T, x.state)
        assertEquals(KeyAnswer.Pass, x.key(SuggestKey.Backspace))
        assertEquals(N_F, x.state)
        val y = mk().justPicked()
        val p = FakePeek()
        y.text("x", p)
        assertEquals(State.Typing("x"), y.state)
        assertEquals(0, p.calls)
        assertEquals(KeyAnswer.Pass, y.key(SuggestKey.Backspace))
        assertEquals(N_T, y.state)
    }

    private fun es28(mk: () -> Driver) {
        val x = mk().typing("Mon")
        x.pick(0)
        assertEquals(
            KeyAnswer.Consume(EditPlan("Monday ", listOf(DeleteBefore(7), Insert("Mon"))), OnFail.PASS),
            x.key(SuggestKey.Backspace),
        )
        assertEquals(State.Typing("Mon"), x.state)
        assertEquals(listOf("Monday", "Money", "Month"), x.cells)
        val y = mk().typing("i")
        y.pick(2)
        assertEquals(
            KeyAnswer.Consume(EditPlan("I ", listOf(DeleteBefore(2), Insert("i"))), OnFail.PASS),
            y.key(SuggestKey.Backspace),
        )
        assertEquals(State.Typing("i"), y.state)
    }

    private fun es29(mk: () -> Driver) {
        val x = mk().justPicked()
        assertEquals(UNDO_THEY, x.key(SuggestKey.Backspace))
        assertEquals(KeyAnswer.Pass, x.key(SuggestKey.Backspace))
        assertEquals(KeyAnswer.Pass, x.key(SuggestKey.Backspace))
        assertEquals(State.Typing("t"), x.state)
    }

    private fun es30(mk: () -> Driver) {
        for (k in listOf(SuggestKey.Enter, SuggestKey.Navigation)) {
            val x = mk().justPicked()
            assertEquals(KeyAnswer.Pass, x.key(k))
            assertEquals(N_F, x.state)
        }
    }

    private fun es31(mk: () -> Driver) {
        val x = mk().nT()
        assertEquals(KeyAnswer.Pass, x.text("."))
        assertEquals(N_T, x.state)
    }

    private fun es32(mk: () -> Driver) {
        for (x in listOf(mk().typing("the"), mk().justPicked())) {
            x.failed()
            assertEquals(N_F, x.state)
            val p = FakePeek()
            x.text("h", p)
            assertEquals(3, p.calls)
        }
    }

    private fun es33(mk: () -> Driver) {
        for (s in listOf(".", "'")) {
            val x = mk().justPicked()
            val k = SuggestKey.Text(s)
            assertTrue(x.src.onKey(k, FakePeek()) is KeyAnswer.WriteThenPass)
            x.failed()
            x.src.afterKey(k)
            assertEquals(s, N_T, x.state)
        }
    }

    private fun es34(mk: () -> Driver) {
        val x = mk().justPicked()
        assertEquals(UNDO_THEY, x.src.onKey(SuggestKey.Backspace, FakePeek()))
        x.failed()
        x.src.afterKey(SuggestKey.Backspace) // OnFail.PASS took the normal path
        assertEquals(N_F, x.state)
    }

    @Test fun `ES-01 N(t) h e l - no peek, cells hello help held, all Pass`() = es01(::d)

    @Test fun `ES-02 N(f) h with empty or blank neighbours - T(h), three peeks`() = es02(::d)

    @Test fun `ES-03 N(f) h inside a word is Blocked, peeked apostrophe is a quote`() = es03(::d)

    @Test fun `ES-04 N(f) h with a failed read or a selection is Blocked, peeks stop at the first null`() = es04(::d)

    @Test fun `ES-05 Blocked until a word end, then no peek`() = es05(::d)

    @Test fun `ES-06 apostrophe after a letter joins, after an apostrophe ends`() = es06(::d)

    @Test fun `ES-07 leading apostrophe is a word end without peeking`() = es07(::d)

    @Test fun `ES-08 word-end text and space end the word`() = es08(::d)

    @Test fun `ES-09 reset keys go to N(f)`() = es09(::d)

    @Test fun `ES-10 backspace shortens the word and leaves it as N(t)`() = es10(::d)

    @Test fun `ES-11 backspace from N(t) goes to N(f) and does not reread`() = es11(::d)

    @Test fun `ES-12 32 letters type, the 33rd blocks, backspace stays Blocked`() = es12(::d)

    @Test fun `ES-13 mixed case has no cells until it is not mixed`() = es13(::d)

    @Test fun `ES-14 cells drop the exact match and keep three`() = es14(::d)

    @Test fun `ES-15 cells follow the case rule`() = es15(::d)

    @Test fun `ES-16 typed cell only when no dictionary word starts with the word`() = es16(::d)

    @Test fun `ES-17 pick appends the rest and a space`() = es17(::d)

    @Test fun `ES-18 pick that changes case deletes and inserts`() = es18(::d)

    @Test fun `ES-19 pick of the typed cell inserts only the space`() = es19(::d)

    @Test fun `ES-20 pick out of range or outside Typing gives null and keeps the state`() = es20(::d)

    @Test fun `ES-21 joining punctuation after a pick removes the automatic space`() = es21(::d)

    @Test fun `ES-22 apostrophe after a pick joins the picked word`() = es22(::d)

    @Test fun `ES-23 other text after a pick keeps the space`() = es23(::d)

    @Test fun `ES-24 space after a pick does nothing and keeps JustPicked`() = es24(::d)

    @Test fun `ES-25 backspace after a pick undoes it`() { es25(::d) }

    @Test fun `ES-26 undo is one step, still available after idle spaces`() = es26(::d)

    @Test fun `ES-27 after punctuation or a letter, backspace is the normal one`() = es27(::d)

    @Test fun `ES-28 undo restores the typed shape`() = es28(::d)

    @Test fun `ES-29 backspace repeat - first undoes, the rest delete one each`() = es29(::d)

    @Test fun `ES-30 reset keys after a pick go to N(f)`() = es30(::d)

    @Test fun `ES-31 a user space is not removed by punctuation`() = es31(::d)

    @Test fun `ES-32 onWriteFailed goes to N(f) and the next letter peeks`() = es32(::d)

    @Test fun `ES-33 failed WriteThenPass then afterKey ends in N(t)`() = es33(::d)

    @Test fun `ES-34 failed undo then afterKey ends in N(f)`() = es34(::d)

    @Test
    fun `ES-35 every reset reason goes to N(f) with no cells`() {
        for (r in ResetReason.entries) {
            val blocked = d().also { it.text("h", FakePeek(before = "a")) }
            for (x in listOf(d().typing("the"), d().justPicked(), blocked)) {
                x.reset(r)
                assertEquals("$r", N_F, x.state)
                assertEquals("$r", emptyList<String>(), x.cells)
            }
        }
        assertEquals(8, ResetReason.entries.size)
    }

    @Test
    fun `ES-36 two instances give the same cells, answers and plans for ES-01 to ES-34`() {
        val bodies: List<(() -> Driver) -> Unit> = listOf(
            ::es01, ::es02, ::es03, ::es04, ::es05, ::es06, ::es07, ::es08, ::es09, ::es10, ::es11, ::es12,
            ::es13, ::es14, ::es15, ::es16, ::es17, ::es18, ::es19, ::es20, ::es21, ::es22, ::es23, ::es24,
            { mk -> es25(mk) }, ::es26, ::es27, ::es28, ::es29, ::es30, ::es31, ::es32, ::es33, ::es34,
        )
        val other = WordDictionaryTest.mini() // a second dictionary read from the same file
        for ((i, body) in bodies.withIndex()) {
            val a = ArrayList<Driver>()
            val b = ArrayList<Driver>()
            body { Driver(DICT).also { a += it } }
            body { Driver(other).also { b += it } }
            val ta = a.flatMap { it.trace }
            assertTrue("ES-${i + 1} recorded nothing", ta.isNotEmpty())
            assertEquals("ES-${i + 1}", ta, b.flatMap { it.trace })
        }
    }

    companion object {
        private val DICT: WordIndex = WordDictionaryTest.mini()
        private val N_T = State.None(true)
        private val N_F = State.None(false)
        private val UNDO_THEY = KeyAnswer.Consume(EditPlan("they ", listOf(DeleteBefore(5), Insert("the"))), OnFail.PASS)
    }
}
