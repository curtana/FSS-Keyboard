package kbd.graffiti.nn

import kbd.graffiti.eval.GraffitiEval
import kbd.graffiti.ink.GraffitiLang
import kbd.graffiti.recognize.Expect
import kbd.graffiti.recognize.Recognizer
import kbd.graffiti.recognize.SetName
import kbd.graffiti.recognize.StrokeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * LLD-11 §6.7 SR-01..SR-03 (HLD 11.1 라, 10.1 R18): the weighted sum must not let in clearly more scribbles than
 * the templates alone did. The scribbles are synthetic ([ScribbleStrokes]); the limits are the measured values with
 * a margin, not a target.
 */
class ScribbleRejectTest {

    private fun accepted(rec: Recognizer, lang: GraffitiLang): Int =
        ScribbleStrokes.all.count { (_, s) -> rec.recognize(s, lang, Expect.NORMAL) != StrokeToken.Unknown }

    @Test
    fun `SR-01 the scribbles are the same on every run and none is a rule stroke`() {
        assertEquals(SCRIBBLES, ScribbleStrokes.all.size)
        val first = ScribbleStrokes.all.first().second.points.first()
        assertEquals(FIRST_X, first.x, 0f)
        assertEquals(FIRST_Y, first.y, 0f)
    }

    @Test
    fun `SR-02 scribbles accepted by the weighted sum against the templates alone, per language`() {
        for ((lang, limit) in listOf(GraffitiLang.EN to EN_MAX, GraffitiLang.KO to KO_MAX)) {
            val alone = accepted(GraffitiEval.templateOnly, lang)
            val sum = accepted(GraffitiEval.recognizer, lang)
            println("SR-02 $lang scribbles accepted: templates alone $alone / ${ScribbleStrokes.all.size}, weighted sum $sum / ${ScribbleStrokes.all.size}")
            assertTrue("$lang weighted sum $sum, templates alone $alone", sum <= alone + OVER_ALONE_MAX)
            assertTrue("$lang weighted sum $sum over $limit", sum <= limit)
        }
    }

    @Test
    fun `SR-03 a sum over its threshold is refused when the set's best template score is under the template threshold`() {
        var refused = 0
        for ((name, s) in ScribbleStrokes.all) for (set in listOf(SetName.EN_LETTER, SetName.KO_LETTER)) {
            val got = GraffitiEval.recognizer.score(s, set)!!
            if (got.sum >= Recognizer.SUM_MIN && got.setTemplate < Recognizer.TEMPLATE_MIN) {
                refused++
                assertTrue(name, !got.accepted)
                val lang = if (set == SetName.EN_LETTER) GraffitiLang.EN else GraffitiLang.KO
                assertEquals(name, StrokeToken.Unknown, GraffitiEval.recognizer.recognize(s, lang, Expect.NORMAL))
            }
        }
        println("SR-03 scribbles refused by the template threshold alone: $refused")
        assertTrue(refused >= REFUSED_MIN)
    }

    private companion object {
        /** Eight families of 100; none of this seed's scribbles is a rule stroke. */
        const val SCRIBBLES = 800
        const val FIRST_X = 50.1f
        const val FIRST_Y = 49.7f

        /**
         * Measured (LLD-11 6.7): English 97 of 800 (templates alone 120), Korean 38 of 800 (templates alone 72). The limits
         * are the measured counts plus 2 % of the scribbles (16).
         */
        const val EN_MAX = 113
        const val KO_MAX = 54

        /** The weighted sum must not accept more scribbles than the templates alone (measured 23 and 34 fewer). */
        const val OVER_ALONE_MAX = 0
        const val REFUSED_MIN = 1
    }
}
