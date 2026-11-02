package kbd.app.swipe

import kbd.stroke.geom.StrokePoint

/**
 * The vowel strokes' trail (SWIPE 9.4, SRS FR-DSP-09, issue #173): the path of each vowel stroke as
 * the finger drew it, kept after release, one kept path per stroke of the open vowel assembly.
 * Display only (never judged with); Android-free so the swipe keyboard and the tutor's practice
 * keyboard share it.
 *
 * The kept paths always follow the assembly: [sync] cuts them to the number of open strokes, so an
 * input that closes the assembly (a tap, a number or symbol, a function key, the cheonjiin key, a
 * cursor move…) clears them, and ⌫ in an open assembly removes exactly the stroke it took back. A
 * stroke the assembly did not take (reversed, out of order or place) and a cancelled touch are
 * dropped on release ([drop]).
 */
class StrokeTrail {

    private val kept = ArrayList<List<StrokePoint>>()
    private var current: ArrayList<StrokePoint>? = null

    /** Kept paths, oldest first, and the one being drawn (if any). */
    val paths: List<List<StrokePoint>> get() = kept + listOfNotNull(current?.toList())

    /** A vowel stroke may start: a consonant key was pressed at [p]. */
    fun begin(p: StrokePoint) {
        current = arrayListOf(p)
    }

    fun add(p: StrokePoint) {
        current?.add(p)
    }

    /** Released and taken by the assembly: the path stays. */
    fun keep() {
        current?.let { if (it.size > 1) kept += it.toList() }
        current = null
    }

    /** Released as a tap, ignored or cancelled: the path goes. */
    fun drop() {
        current = null
    }

    /** Cuts the kept paths to the [open] strokes of the assembly (0 = closed: none). */
    fun sync(open: Int) {
        while (kept.size > open) kept.removeAt(kept.lastIndex)
    }

    fun clear() {
        kept.clear()
        current = null
    }
}
