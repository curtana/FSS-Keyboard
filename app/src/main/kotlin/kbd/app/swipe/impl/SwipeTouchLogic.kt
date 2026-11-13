package kbd.app.swipe.impl

import kbd.app.swipe.StrokeTrail
import kbd.gesture.classify.StrokeOutcome
import kbd.app.swipe.SwipeHost
import kbd.gesture.classify.StrokeClassifier
import kbd.gesture.classify.StrokeResult
import kbd.gesture.classify.SwipeKeyKind
import kbd.gesture.input.MapResult
import kbd.gesture.input.SwipeInputMapper
import kbd.gesture.layout.SwipeKeyDef
import kbd.gesture.layout.SwipeLayout
import kbd.gesture.progress.ProgressState
import kbd.hangul.stroke.ConsonantBox
import kbd.stroke.geom.KeyGeom
import kbd.stroke.geom.KeyHitTester
import kbd.stroke.geom.RectDp
import kbd.stroke.geom.StrokePoint
import kbd.stroke.layout.FunctionKey
import kbd.stroke.tap.ModeKeyGesture
import kbd.stroke.tap.TapGestureClassifier

/**
 * Key rectangles of the swipe layout in view dp (LLD-04 §8.1, §9.2): column width = width / 7,
 * row height = the row height setting. The view is the window's bottom 5 rows, so its bottom border is the
 * window's (LLD-08 §7); its top border never is. The window's left and right borders are [sideDp] outside the view,
 * the outer sides of the keyboard's side margins (SRS FR-CMN-14 ⑧, LLD-08 §7.2): an end key's outer edge is an
 * ordinary edge.
 */
class SwipeGeometry private constructor(val widthDp: Float, val rowHeightDp: Float, val sideDp: Float) {
    val keys: List<SwipeKeyDef> = SwipeLayout.KEYS
    val heightDp: Float = SwipeLayout.ROWS * rowHeightDp

    /** The center consonant's place C (SWIPE 9.5): drawn there and judged against (SWIPE 3.6.3). */
    val consonantBox: ConsonantBox = SwipeLayout.consonantBox(widthDp, rowHeightDp)
    val geoms: List<KeyGeom>
    private val hitTester: KeyHitTester

    init {
        val colW = widthDp / SwipeLayout.COLS
        geoms = keys.map { k ->
            val rect = RectDp(k.col * colW, k.row * rowHeightDp, (k.col + k.colSpan) * colW, (k.row + 1) * rowHeightDp)
            KeyGeom.build(k.id, rect, widthDp, heightDp, sideDp)
        }
        hitTester = KeyHitTester(geoms, widthDp, heightDp)
    }

    /** Index into [keys]/[geoms] of the key at (x, y) dp; gaps go to the nearest key (LLD-04 §2). */
    fun indexAt(x: Float, y: Float): Int {
        val g = hitTester.hit(x, y) ?: return -1
        return geoms.indexOf(g)
    }

    companion object {
        fun build(widthDp: Float, rowHeightDp: Float, sideDp: Float = 0f): SwipeGeometry {
            require(widthDp > 0f && rowHeightDp > 0f) { "empty swipe view" }
            return SwipeGeometry(widthDp, rowHeightDp, sideDp)
        }
    }
}

/** The guide as the touch logic drives it (LLD-06 §3); [GuideOverlay] behind the view. */
interface StrokeGuide {
    fun onDown(key: SwipeKeyDef, keyRect: RectDp, origin: StrokePoint)
    fun onProgress(ps: ProgressState, finger: StrokePoint)
    fun onUp()
}

/** ⌫ repeat (LLD-05A §6); `RepeatTimer` behind the view. */
interface KeyRepeat {
    fun start(pointerId: Int, key: FunctionKey)
    fun stop(pointerId: Int)
    fun stopAll()
}

/**
 * One-stroke touch handling of the swipe keyboard, free of Android (LLD-04 §9.3). The view turns
 * `MotionEvent`s into dp points and calls [down], [move], [up] and [cancel]. The stroke's result
 * comes only from the first-pressed key (FR-SWP-01); other pointers are ignored while a stroke is
 * in progress (FR-CMN-11). Every consonant key (the double keys too) shows the guide (FR-DSP-01, issue #168).
 * The mode key is judged on release like on the tap keyboards (issue #205): an up exit opens the settings
 * ([SwipeHost.onSettingsStroke]), anything else is the mode change.
 */
class SwipeTouchLogic(
    private val host: SwipeHost,
    private val mapper: SwipeInputMapper,
    private val guide: StrokeGuide,
    private val repeat: KeyRepeat,
    /** One line per released key stroke for the debug build's logcat (issue #217); null = off (release builds). */
    private val strokeLog: ((String) -> Unit)? = null,
) {
    /** Built by the view on layout; null before the first layout (touches are then ignored). */
    var geometry: SwipeGeometry? = null

    var activePointerId: Int = TouchTracker.NONE
        private set

    private var key: SwipeKeyDef? = null
    private var classifier: StrokeClassifier? = null

    /** The mode key's stroke, judged on release (issue #205); same edge-crossing judgement as the tap keyboards. */
    private var modeStroke: TapGestureClassifier? = null

    /** The guide follows this stroke: a number or symbol key, or the anchor consonant key (HLD 6). */
    private var guided = false

    /** The vowel strokes' trail (SWIPE 9.4, FR-DSP-09): the view draws it; display only. */
    val trail = StrokeTrail()

    /** Bounding box of the stroke's path (left, top, right, bottom dp) for [strokeLog]; kept only while it is on. */
    private val pathBox = FloatArray(4)

    /** ACTION_DOWN / POINTER_DOWN. False when ignored (a stroke is in progress, or no layout yet). */
    fun down(pointerId: Int, p: StrokePoint): Boolean {
        if (activePointerId != TouchTracker.NONE) return false
        val geo = geometry ?: return false
        val i = geo.indexAt(p.x, p.y)
        if (i < 0) return false
        val k = geo.keys[i]
        activePointerId = pointerId
        key = k
        if (k.kind != SwipeKeyKind.EMPTY) host.feedback.key() // FR-CMN-11a ②: on press, not for empty cells
        when (k.kind) {
            SwipeKeyKind.FUNCTION -> if (k.function == FunctionKey.MODE) {
                modeStroke = TapGestureClassifier(geo.geoms[i], host.thresholds.minCrossDp).also { it.begin(p) }   // nothing on press (issue #205)
            } else {
                val f = checkNotNull(k.function)
                host.guard.stroke(SITE_FN) { host.onFunctionKey(f) } // FR-CMN-11a ①: on press
                trail.sync(mapper.strokes.size)   // ⌫ took back one stroke, the others closed the assembly (SWIPE 9.4)
                if (f == FunctionKey.BACKSPACE) repeat.start(pointerId, f)
            }
            SwipeKeyKind.EMPTY -> Unit // touch taken, no input (FR-SWP-50)
            else -> {
                val g = geo.geoms[i]
                // TBD-24: vowel strokes are judged against C while a consonant composes (the guide's preview too).
                mapper.consonant = if (host.centerConsonant != null) geo.consonantBox else null
                classifier = host.guard.run(SITE_BEGIN) {
                    StrokeClassifier(k, host.thresholds, g, mapper::preview).also { it.begin(p) }
                }
                val c = classifier ?: return true
                if (strokeLog != null) { pathBox[0] = p.x; pathBox[1] = p.y; pathBox[2] = p.x; pathBox[3] = p.y }
                if (k.kind == SwipeKeyKind.CONSONANT) trail.begin(p)   // only vowel strokes leave a trail (SWIPE 9.4)
                guided = true   // every key that strokes shows the guide (FR-DSP-01; the anchor-key rule went with issue #168)
                if (guided) {
                    guide.onDown(k, g.rect, p)
                    // The guide may show after the delay without any move (FR-DSP-01): give it the press state.
                    host.guard.run(SITE_UPDATE) { ProgressState.of(c) }?.let { guide.onProgress(it, p) }
                }
            }
        }
        return true
    }

    /** ACTION_MOVE with the active pointer's history and current points. */
    fun move(points: List<StrokePoint>) {
        modeStroke?.let { m -> points.forEach(m::add) }
        val c = classifier ?: return
        for (p in points) {
            val ps = host.guard.run(SITE_UPDATE) { c.update(p) }
            if (ps == null) { // the stroke is dropped: nothing on release
                dropStroke()
                return
            }
            if (guided) guide.onProgress(ps, p)
            trail.add(p)
            if (strokeLog != null) grow(p)
        }
    }

    private fun grow(p: StrokePoint) {
        pathBox[0] = minOf(pathBox[0], p.x); pathBox[1] = minOf(pathBox[1], p.y)
        pathBox[2] = maxOf(pathBox[2], p.x); pathBox[3] = maxOf(pathBox[3], p.y)
    }

    /** ACTION_UP / POINTER_UP; ignored unless [pointerId] is the active pointer. */
    fun up(pointerId: Int, p: StrokePoint) {
        if (pointerId != activePointerId) return
        val k = checkNotNull(key)
        guide.onUp()
        val m = modeStroke
        when {
            m != null -> modeUp(m, p)
            k.kind == SwipeKeyKind.FUNCTION -> {
                val f = checkNotNull(k.function)
                repeat.stop(pointerId)
                host.onFunctionKeyUp(f)
            }
            k.kind == SwipeKeyKind.EMPTY -> Unit
            else -> classifier?.let { c ->
                // snapshot → run → restore on failure; nothing is written on failure (LLD-09 §2)
                host.guard.stroke(SITE_RELEASE) { release(c, p) }
            }
        }
        clear()
    }

    /**
     * ACTION_CANCEL: the unreleased stroke gives nothing. A function key's press input is kept and
     * only its repeat stops (FR-CMN-11a ③, DCR-11). An unreleased mode key gives nothing (issue #205).
     */
    fun cancel() {
        if (activePointerId == TouchTracker.NONE) return
        val k = key
        if (classifier != null) dropStroke() else guide.onUp()
        if (k?.kind == SwipeKeyKind.FUNCTION && modeStroke == null) {
            repeat.stop(activePointerId)
            host.onFunctionKeyUp(checkNotNull(k.function))
        }
        clear()
    }

    /** The stroke gives nothing; the mapper never saw it (LLD-04 §9.3). */
    private fun dropStroke() {
        classifier = null
        guide.onUp()
        trail.drop()
    }

    private fun release(c: StrokeClassifier, p: StrokePoint) {
        trail.add(p)
        val r = c.finish(p)
        val log = strokeLog?.let { grow(p); StrokeLogLine(r, mapper) }
        val m = mapper.accept(r)
        if (log != null) strokeLog?.invoke(log.finish(m, geometry, c, host.centerConsonant?.compat, pathBox, host.fieldInfo.hidesDisplay))
        // The trail keeps a vowel stroke the assembly took; the rest goes, and it follows the assembly (SWIPE 9.4).
        if (r.outcome is StrokeOutcome.Stroke && m is MapResult.Jamo) trail.keep() else trail.drop()
        trail.sync(mapper.strokes.size)
        when (m) {
            is MapResult.Jamo -> host.onSwipeEvents(m.events, m.errorVibe) // composer and editor are updated last, inside
            MapResult.Ignored -> host.feedback.error()          // an ignored stroke or cheonjiin element (FR-SWP-12, 14, 62); follows the vibration setting
            is MapResult.Text -> host.onSwipeText(m.text, m.errorVibe)
        }
    }

    /** Mode key release (issue #205): first edge up = the settings, else the mode change (FR-CMN-13); a failed judgement gives nothing (LLD-09 §2). */
    private fun modeUp(m: TapGestureClassifier, p: StrokePoint) {
        val settings = host.guard.run(SITE_FN) { ModeKeyGesture.of(m.finish(p)) == ModeKeyGesture.OPEN_SETTINGS } ?: return
        if (settings) {
            host.guard.stroke(SITE_FN) { host.onSettingsStroke() }
        } else {
            host.guard.stroke(SITE_FN) { host.onFunctionKey(FunctionKey.MODE) }
            host.onFunctionKeyUp(FunctionKey.MODE)
        }
        trail.sync(mapper.strokes.size)   // the assemblies closed (SWIPE 9.4)
    }

    private fun clear() {
        activePointerId = TouchTracker.NONE
        key = null
        classifier = null
        modeStroke = null
        guided = false
    }

    companion object {
        const val SITE_FN = "Swipe.fn"
        const val SITE_BEGIN = "StrokeClassifier.begin"
        const val SITE_UPDATE = "StrokeClassifier.update"
        const val SITE_RELEASE = "Swipe.release"
    }
}

/**
 * One `FSSStroke` logcat line of a released key stroke (issue #217, debug builds only; never judged
 * with). Made in two steps around `SwipeInputMapper.accept`: [StrokeLogLine] reads the stroke's role by C and
 * the assembly before, [finish] adds what the mapper did and the assembly after. All coordinates in view dp.
 * In a password field (`FieldInfo.hidesDisplay`, FR-CMN-08 ①) every typed character is `*`: the key, the center
 * consonant, the text, the cheonjiin element, the events and the shown vowel; places and roles stay.
 */
internal class StrokeLogLine(private val r: StrokeResult, private val mapper: SwipeInputMapper) {
    private val c: ConsonantBox? = mapper.debugConsonant()
    private val before: Int = mapper.strokes.size
    private val stroke = (r.outcome as? StrokeOutcome.Stroke)?.s
    private val role: String? = stroke?.let(mapper::debugRole)
    private val reason: String? = stroke?.let(mapper::debugIgnoreReason)

    fun finish(m: MapResult, geo: SwipeGeometry?, cl: StrokeClassifier, center: Char?, box: FloatArray, hide: Boolean = false): String {
        val s = cl.state()
        val b = StringBuilder()
        val shape = when (val o = r.outcome) {
            StrokeOutcome.Tap -> "tap"
            is StrokeOutcome.Stroke -> if (o.s.vertical) "vertical" else "horizontal"
            StrokeOutcome.Reverse -> "reversed (ignored)"
            is StrokeOutcome.Element -> "cheonjiin ${if (hide) MASK else o.e}"
            is StrokeOutcome.Text -> "text \"${if (hide) MASK else o.text}\""
        }
        b.append(if (r.outcome == StrokeOutcome.Tap) "tap" else "stroke").append(" key=").append(if (hide) MASK else r.keyId)
        if (geo != null) b.append(" view=").append(f(geo.widthDp)).append('x').append(f(geo.heightDp))
        b.append(" from=").append(pt(s.first.x, s.first.y)).append(" to=").append(pt(s.last.x, s.last.y))
        b.append(" bbox=").append(pt(box[0], box[1])).append('-').append(pt(box[2], box[3]))
        val shown = c ?: geo?.consonantBox
        if (shown != null) {
            b.append(" C=(").append(f(shown.left)).append(',').append(f(shown.top)).append(',')
                .append(f(shown.right)).append(',').append(f(shown.bottom)).append(')')
            if (c == null) b.append("(not judged)")
        }
        b.append(" t=").append(f(mapper.th.posTolDp)).append(" center=").append(if (center == null) "-" else if (hide) MASK else center.toString())
        b.append(" shape=").append(shape)
        // The one length threshold left (issue #179): press → release displacement's larger component vs minCrossDp = tap / stroke.
        val len = maxOf(kotlin.math.abs(s.last.x - s.first.x), kotlin.math.abs(s.last.y - s.first.y))
        b.append(" len=").append(f(len)).append(" minCross=").append(f(mapper.th.minCrossDp))
        // Number, symbol and cheonjiin keys decide by the edges crossed (issue #237): the key, its outer edges, the crossings.
        if (r.kind != SwipeKeyKind.CONSONANT) {
            val k = cl.geom
            b.append(" keyRect=(").append(f(k.rect.left)).append(',').append(f(k.rect.top)).append(',')
                .append(f(k.rect.right)).append(',').append(f(k.rect.bottom)).append(')')
            b.append(" outer=").append(k.outerEdges.sortedBy { it.ordinal })
            b.append(" crossings=").append(s.tracker.crossings.map { "${it.edge} ${it.dir}" })
            b.append(" e1=").append(s.e1 ?: "-")
        }
        if (role != null) b.append(" role=").append(role)
        b.append(" result=").append(resultText(m, hide))
        if (reason != null && m == MapResult.Ignored) b.append(" ignored=").append(reason)
        val dropped = before - mapper.strokes.size
        if (dropped > 0 && stroke == null) b.append(" closed: dropped ").append(dropped).append(" open stroke(s)")
        val state = mapper.debugState()
        b.append(" assembly=").append(if (hide) state.substringBefore(" vowel=") else state)   // the state name stays
        return b.toString()
    }

    private fun resultText(m: MapResult, hide: Boolean): String = when (m) {
        is MapResult.Jamo -> "jamo" + (if (hide) "[${m.events.size} × $MASK]" else m.events.toString()) +
            (if (m.errorVibe) " errorVibe" else "")
        MapResult.Ignored -> "Ignored"
        is MapResult.Text -> "text \"${if (hide) MASK else m.text}\"" + (if (m.errorVibe) " errorVibe" else "")
    }

    private companion object {
        const val MASK = "*"

        fun f(v: Float) = String.format(java.util.Locale.ROOT, "%.1f", v)
        fun pt(x: Float, y: Float) = "(${f(x)},${f(y)})"
    }
}
