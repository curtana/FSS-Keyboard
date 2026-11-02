package kbd.app.swipe.impl

import kbd.hangul.stroke.StrokePop
import kbd.app.tutor.PracticeEcho
import kbd.app.tutor.RawStroke
import kbd.gesture.classify.StrokeClassifier
import kbd.gesture.classify.SwipeKeyKind
import kbd.gesture.input.MapResult
import kbd.gesture.input.SwipeInputMapper
import kbd.hangul.compose.BackspaceResult
import kbd.hangul.compose.ComposePolicy
import kbd.hangul.compose.HangulComposer
import kbd.stroke.layout.FunctionKey
import kbd.stroke.threshold.Thresholds

/**
 * Swipe consonant keys (lessons 1, 3 and 5): each practice stroke goes through the swipe
 * keyboard's own chain — [StrokeClassifier] on the real swipe geometry, [SwipeInputMapper] (a tap =
 * the consonant, a swipe = the key's consonant + the vowel, issue #168), the swipe [HangulComposer] — as [SwipeTouchLogic] does (LLD-04 §9.3), so the echo
 * shows what the keyboard would type at [th] (FR-TUT-09). A stroke on the space key (lesson 5) commits and types a space, as the
 * keyboard's space key does; ⌫ (lesson 5, issue #165) as the keyboard's ⌫.
 */
class SwipePracticeEcho(
    private val th: Thresholds,
    private val rowHeightDp: Float,
    /** The `FSSStroke` line writer, read at each stroke (issue #217): as the keyboard's, null = off (release builds). */
    private val strokeLog: () -> ((String) -> Unit)? = { StrokeDebugLog.sink },
) : PracticeEcho {
    private val mapper = SwipeInputMapper(th)
    private val composer = HangulComposer(ComposePolicy.SWIPE)
    private val committed = StringBuilder()
    private var geometry: SwipeGeometry? = null

    override val text: String get() = committed.toString() + shownComposing()

    /** As `ComposingController.show`: the bare ‥ glyph stands in place of the composer's ㅣ / ㅡ (SWIPE 0.36 3.4). */
    private fun shownComposing(): String {
        val c = composer.composingText()
        return (if (mapper.pendingReplacesVowel()) c.dropLast(1) else c) + mapper.pendingGlyph()
    }

    /** As on the keyboard (SWIPE 9.5): the composer's last consonant in the place C of the real geometry. */
    override val center: kbd.app.view.CenterMark?
        get() {
            val c = composer.lastConsonant() ?: return null
            val g = geometry ?: return null
            return kbd.app.view.CenterMark(c, g.consonantBox)
        }

    override val consonantPlace: kbd.hangul.stroke.ConsonantBox? get() = geometry?.consonantBox

    override var lastIgnored: Boolean = false
        private set

    override val openStrokes: Int get() = mapper.strokes.size

    override fun preview(s: kbd.hangul.stroke.VowelStroke): String? {
        mapper.consonant = consonantBox()
        return mapper.preview(s)
    }

    /** C while a consonant composes (TBD-24), as `SwipeTouchLogic.down` gives it to the mapper. */
    private fun consonantBox() = if (composer.lastConsonant() != null) geometry?.consonantBox else null

    override fun onStroke(raw: RawStroke, widthDp: Float) {
        lastIgnored = false
        val pts = raw.points
        if (pts.size < 2) return
        val g = geometry?.takeIf { it.widthDp == widthDp }
            ?: SwipeGeometry.build(widthDp, rowHeightDp).also { geometry = it }
        val i = g.indexAt(pts.first().x, pts.first().y)
        val hit = g.keys.getOrNull(i) ?: return
        if (hit.kind == SwipeKeyKind.FUNCTION && hit.function == FunctionKey.SPACE) {   // InputDispatcher: finish, " ", reset
            committed.append(composer.finish()).append(' ')
            lastIgnored = mapper.reset()   // dropped unfinished vowel strokes vibrate, as on the keyboard (SWIPE 3.3)
            return
        }
        if (hit.kind == SwipeKeyKind.FUNCTION && hit.function == FunctionKey.BACKSPACE) {
            backspace()
            return
        }
        val key = hit.takeIf { it.kind == SwipeKeyKind.CONSONANT || it.kind == SwipeKeyKind.CHEONJIIN } ?: return
        mapper.consonant = consonantBox()
        val c = StrokeClassifier(key, th, g.geoms[i], mapper::preview)
        c.begin(pts.first())
        for (p in pts.subList(1, pts.size - 1)) c.update(p)
        val r = c.finish(pts.last())
        val sink = strokeLog()
        val log = sink?.let { StrokeLogLine(r, mapper) }
        val center = composer.lastConsonant()?.compat
        val m = mapper.accept(r)
        if (log != null) sink("practice " + log.finish(m, g, c, center, boxOf(pts)))   // practice fields are never password fields
        if (m is MapResult.Jamo) committed.append(composer.applyAll(m.events).committed)
        // The keyboard's error vibration: an ignored stroke or element, or unfinished strokes dropped (SWIPE 10.1).
        lastIgnored = m == MapResult.Ignored || (m is MapResult.Jamo && m.errorVibe)
    }

    private fun boxOf(pts: List<kbd.stroke.geom.StrokePoint>) =
        floatArrayOf(pts.minOf { it.x }, pts.minOf { it.y }, pts.maxOf { it.x }, pts.maxOf { it.y })

    /**
     * Lesson 5 ⌫ (PM decision, issue #165), as `InputDispatcher.backspace` does it on the keyboard: an
     * open vowel assembly gives back one stroke or element first, else the composer one unit, else the
     * last committed character (SWIPE 3.5, FR-CMN-07, 07b).
     */
    private fun backspace() {
        when (val pop = mapper.popElement()) {
            is StrokePop.Popped -> if (pop.undoComposer) composer.undoLast()
            StrokePop.Empty -> when (composer.backspace()) {
                is BackspaceResult.Handled -> Unit
                BackspaceResult.NothingComposing -> if (committed.isNotEmpty()) {
                    committed.setLength(committed.offsetByCodePoints(committed.length, -1))
                }
            }
        }
    }
}
