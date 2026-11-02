package kbd.app.swipe

import kbd.app.swipe.impl.SwipeEngineModule
import kbd.app.swipe.impl.SwipeGeometry
import kbd.app.tutor.TutorSteps
import kbd.app.tutor.PracticeLayout
import kbd.app.tutor.HowTo
import kbd.app.tutor.PracticeLayouts
import kbd.gesture.classify.SwipeKeyKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** LLD-11 §7.5 TU-01 and the tutor wiring of T33 (LLD-07 §2.3). */
class TutorWiringTest {

    @Test
    fun `TU-01 tutorSteps are the lesson steps in SWIPE 10 order`() {
        val steps = SwipeEngineModule.tutorSteps()
        assertEquals(listOf("1-1", "1-2", "3-1", "3-4", "5-1"), steps.map { it.id })   // lessons 4 (#150) and 2 (#168), 3-3 (#156) and 3-2 (#173) removed; 3-4 (#160)
        assertEquals(TutorSteps.ALL, steps)
        assertTrue(steps.dropLast(1).all { it.practiceLayout == PracticeLayout.SWIPE_CONSONANT_KEY })
        assertEquals(PracticeLayout.SWIPE_SENTENCE, steps.last().practiceLayout)
        // Each lesson's first step carries the lesson explanation, the others none.
        assertEquals(listOf("1-1", "3-1", "5-1"), steps.filter { it.intro != 0 }.map { it.id })
        assertTrue(SwipeModuleProvider.get().tutorSteps() == steps)
    }

    @Test
    fun `the practice keyboard uses the real swipe geometry and practises the consonant keys and the cheonjiin key`() {
        val pk = PracticeLayouts.of(PracticeLayout.SWIPE_CONSONANT_KEY, SwipeEngineModule)
        val keys = pk.keys(widthDp = 392f, rowHeightDp = 56f)
        val g = SwipeGeometry.build(392f, 56f)
        val consonants = g.keys.indices.filter { g.keys[it].kind == SwipeKeyKind.CONSONANT }
        val practised = g.keys.indices.filter { g.keys[it].kind == SwipeKeyKind.CONSONANT || g.keys[it].kind == SwipeKeyKind.CHEONJIIN }
        assertEquals(6, pk.rows)
        assertEquals(practised.map { g.keys[it].label to g.geoms[it].rect }, keys.filter { it.practised }.map { it.label to it.rect })
        assertEquals(19, consonants.size)   // the double keys too (issue #168)
        assertEquals(HowTo.CHEONJIIN_KEY, keys.single { it.label.startsWith("ㆍ") }.label)   // the label the 3-4 judgement reads (issue #160)
        assertTrue(keys.none { it.label.isEmpty() })   // empty cells are not drawn
    }

    @Test
    fun `lesson 5 practises the consonant keys, the cheonjiin key, the space key and backspace`() {
        val keys = PracticeLayouts.of(PracticeLayout.SWIPE_SENTENCE, SwipeEngineModule).keys(392f, 56f)
        assertEquals(22, keys.count { it.practised })   // 19 consonants (issue #168), cheonjiin, space, ⌫
        assertEquals(listOf("⌫"), keys.filter { it.backspace }.map { it.label })   // issue #165
        assertTrue(keys.single { it.backspace }.practised)
        assertTrue(PracticeLayouts.of(PracticeLayout.SWIPE_CONSONANT_KEY, SwipeEngineModule).keys(392f, 56f).none { it.practised && it.backspace })
        assertEquals(listOf("스페이스"), keys.filter { it.space }.map { it.label })
        assertTrue(keys.single { it.space }.practised)
    }

}
