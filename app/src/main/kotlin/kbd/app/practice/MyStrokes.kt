package kbd.app.practice

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kbd.app.graffiti.GraffitiRecognizers
import kbd.app.store.PersonalStrokeBook
import kbd.app.store.PersonalStrokeStore
import kbd.graffiti.guide.GuideCatalog
import kbd.graffiti.guide.GuideGroup
import kbd.graffiti.guide.GuideMark
import kbd.graffiti.ink.GraffitiLang
import kbd.graffiti.ink.InkStroke
import kbd.graffiti.ink.PadArea
import kbd.graffiti.recognize.Recognizer
import kbd.graffiti.recognize.SetName
import kbd.graffiti.recognize.StrokeRules
import kbd.graffiti.recognize.TemplateOutput
import kbd.graffiti.recognize.TemplateSet

/** A letter of "my strokes" (LLD-07 §8.1): its set, its `output.value`, and how the screen writes it (English in upper case, as the stroke guide). */
data class MyLetter(val set: SetName, val value: String, val label: String) {
    val lang: GraffitiLang get() = if (set == SetName.KO_LETTER) GraffitiLang.KO else GraffitiLang.EN
}

/** Whether a stroke written on the "my strokes" screen is stored (SRS FR-TUT-14 ④). */
enum class MyStrokeVerdict { SAVE, RULE_STROKE, NUMBER_AREA }

/** The rules of "my strokes" that need no screen (LLD-07 §8.1, HLD 7.4). Android-free. */
object MyStrokes {

    /**
     * The letters that can be taught (FR-TUT-14 ①, TBD-30 ⑦): the Korean consonants and the English letters the
     * templates recognize, in template order — 14 and 25 (i is a line rule) with the production templates. Control
     * strokes, digits and symbols are not among them.
     */
    fun letters(templates: TemplateSet): List<MyLetter> {
        val out = LinkedHashMap<Pair<SetName, String>, MyLetter>()
        for (set in listOf(SetName.KO_LETTER, SetName.EN_LETTER)) for (t in templates.sets[set].orEmpty()) {
            if (t.strokes.isEmpty()) continue
            val letter = when (val o = t.output) {
                is TemplateOutput.Jamo -> if (set == SetName.KO_LETTER) Recognizer.valueOf(o).let { MyLetter(set, it, it) } else null
                is TemplateOutput.Text -> if (set == SetName.EN_LETTER && o.value.length == 1 && o.value[0] in 'a'..'z') MyLetter(set, o.value, o.value.uppercase()) else null
                is TemplateOutput.Control -> null
            } ?: continue
            out.putIfAbsent(set to letter.value, letter)
        }
        return out.values.toList()
    }

    /**
     * A stroke begun in the number area, and one the rules take (tap, corner tap, straight line — the same functions
     * recognition asks first), are not stored: a rule stroke never reaches the letters. Anything else is stored, whatever it looks like (TBD-30 ⑥).
     */
    fun verdict(stroke: InkStroke): MyStrokeVerdict = when {
        stroke.area == PadArea.NUMBER -> MyStrokeVerdict.NUMBER_AREA
        StrokeRules.tap(stroke) != null || StrokeRules.line(stroke) != null -> MyStrokeVerdict.RULE_STROKE
        else -> MyStrokeVerdict.SAVE
    }

    /** The stroke guide cells that draw [letter]: one template mark whose output is the letter (both ① and ② where there are two). */
    fun guideGroups(templates: TemplateSet, letter: MyLetter): List<GuideGroup> = GuideCatalog.groups.mapNotNull { g ->
        val cells = g.cells.filter { c ->
            val m = c.marks.singleOrNull() as? GuideMark.Template
            m != null && m.set == letter.set && templates.outputOf(m.id)?.let(Recognizer::valueOf) == letter.value
        }
        if (cells.isEmpty()) null else g.copy(cells = cells)
    }
}

/** What the write screen says after a stroke or a button (LLD-07 §8.3). */
sealed interface MyStrokeMessage {
    /** Stored; [recognized] is the letter label read before storing, null = not recognized (FR-TUT-14 ③, TBD-30 ⑨). */
    data class Saved(val recognized: String?) : MyStrokeMessage
    data object RuleStroke : MyStrokeMessage
    data object NumberArea : MyStrokeMessage
    data object Cleared : MyStrokeMessage
    data object WriteFailed : MyStrokeMessage
}

/**
 * State and actions of the "my strokes" screen (LLD-07 §8.3, SRS FR-TUT-14~16): the one place that stores personal
 * strokes (FR-TUT-17 ①). A stroke types nothing: no graffiti state, no composer, no ✓ / ✗. The values the screen shows
 * are Compose state, like `PracticeFlow`'s.
 */
class MyStrokesModel(
    private val templates: TemplateSet,
    private val store: PersonalStrokeStore,
    private val recognizers: GraffitiRecognizers,
) {
    enum class Screen { CLOSED, LETTERS, WRITE }

    val letters: List<MyLetter> = MyStrokes.letters(templates)

    var screen: Screen by mutableStateOf(Screen.CLOSED)
        private set
    var letter: MyLetter? by mutableStateOf(null)
        private set
    var book: PersonalStrokeBook by mutableStateOf(store.book())
        private set
    var message: MyStrokeMessage? by mutableStateOf(null)
        private set

    /** The "clear everything?" question is on show (TBD-30 ⑫). */
    var askingClearAll: Boolean by mutableStateOf(false)
        private set

    fun open() {
        book = store.book()
        letter = null
        message = null
        screen = Screen.LETTERS
    }

    fun close() {
        askingClearAll = false
        screen = Screen.CLOSED
    }

    fun pick(l: MyLetter) {
        letter = l
        message = null
        screen = Screen.WRITE
    }

    fun backToLetters() {
        letter = null
        message = null
        screen = Screen.LETTERS
    }

    fun count(l: MyLetter): Int = book.count(l.set, l.value)

    /**
     * One stroke of the write screen, on release (FR-TUT-14 ③④): not stored when a rule takes it or it began in the
     * number area; else read by the recognizer as it is before storing, then stored at once (no question, TBD-30 ⑧).
     */
    fun onStroke(stroke: InkStroke) {
        val l = letter ?: return
        message = when (MyStrokes.verdict(stroke)) {
            MyStrokeVerdict.NUMBER_AREA -> MyStrokeMessage.NumberArea
            MyStrokeVerdict.RULE_STROKE -> MyStrokeMessage.RuleStroke
            MyStrokeVerdict.SAVE -> {
                val before = recognizers.get().score(stroke, l.set)?.takeIf { it.accepted }?.letter
                if (store.add(l.set, l.value, stroke.points.map { it.x to it.y })) {
                    refresh()
                    MyStrokeMessage.Saved(before?.let { v -> letters.firstOrNull { it.set == l.set && it.value == v }?.label ?: v })
                } else {
                    MyStrokeMessage.WriteFailed
                }
            }
        }
    }

    /** "다시 학습" (FR-TUT-16 ①): the letter's strokes go and the write screen stays for the new ones. */
    fun relearn() {
        val l = letter ?: return
        message = if (store.clear(l.set, l.value)) MyStrokeMessage.Cleared else MyStrokeMessage.WriteFailed
        refresh()
    }

    /** "초기화" (FR-TUT-16 ②): the letter's strokes go, back to the letter list. */
    fun reset() {
        val l = letter ?: return
        if (store.clear(l.set, l.value)) {
            refresh()
            backToLetters()
        } else {
            message = MyStrokeMessage.WriteFailed
        }
    }

    fun askClearAll() {
        askingClearAll = true
    }

    /** The answer to "clear everything?" (FR-TUT-16 ③). */
    fun answerClearAll(yes: Boolean) {
        askingClearAll = false
        if (yes) {
            message = if (store.clearAll()) null else MyStrokeMessage.WriteFailed
            refresh()
        }
    }

    /** The store's book on screen and in the recognizer of this screen and of the practice topics (FR-TUT-17 ③). */
    private fun refresh() {
        book = store.book()
        recognizers.warm()
    }

    companion object {
        const val MAX_PER_LETTER = PersonalStrokeBook.MAX_PER_LETTER
    }
}
