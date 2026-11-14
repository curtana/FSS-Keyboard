package kbd.app.view

import kbd.app.error.EngineGuard
import kbd.app.feedback.FeedbackPort
import kbd.stroke.geom.KeyGeom
import kbd.stroke.geom.KeyHitTester
import kbd.stroke.geom.RectDp
import kbd.stroke.geom.StrokePoint
import kbd.stroke.layout.FunctionKey
import kbd.stroke.layout.TapKeyDef
import kbd.stroke.layout.TapKeyKind
import kbd.stroke.layout.TapLayout
import kbd.stroke.tap.TapGesture
import kbd.stroke.tap.ModeKeyGesture
import kbd.stroke.tap.TapGestureClassifier
import kbd.stroke.tap.TapKeyResolver
import kbd.stroke.tap.TapOutput

/**
 * Key rectangles of a tap layout in view dp (LLD-05A §3.1): `x_dp = x / 360 × width`, row y =
 * `row × rowHeight`. The view is the whole 6-row key area, so its bottom border is the window's (LLD-08 §7); its
 * top border never is (LLD-03 §3.3). The window's left and right borders are [sideDp] outside the view, the outer
 * sides of the keyboard's side margins (SRS FR-CMN-14 ⑧, LLD-08 §7.2): an end key's outer edge is an ordinary edge.
 */
class TapGeometry private constructor(val layout: TapLayout, val widthDp: Float, val rowHeightDp: Float, val sideDp: Float) {
    val keys: List<TapKeyDef> = layout.keys
    val heightDp: Float = layout.rows * rowHeightDp
    val geoms: List<KeyGeom>
    private val blankRects: List<Pair<RectDp, Int>>
    private val hitTester: KeyHitTester

    init {
        val unit = widthDp / LAYOUT_WIDTH_DP
        geoms = keys.map { k ->
            val rect = RectDp(k.x * unit, k.row * rowHeightDp, (k.x + k.w) * unit, (k.row + 1) * rowHeightDp)
            KeyGeom.build(k.id, rect, widthDp, heightDp, sideDp)
        }
        blankRects = layout.blanks.map { b ->
            val rect = RectDp(b.x * unit, b.row * rowHeightDp, (b.x + b.w) * unit, (b.row + 1) * rowHeightDp)
            rect to keys.indexOfFirst { it.id == b.nearestKeyId }
        }
        hitTester = KeyHitTester(geoms, widthDp, heightDp)
    }

    /**
     * Index into [keys]/[geoms] of the key at (x, y) dp (LLD-05A §4 `hit`): ① inside an indent blank →
     * its same-row key, even when the row above is nearer; ② otherwise [KeyHitTester]. −1 for no keys.
     */
    fun indexAt(x: Float, y: Float): Int {
        for ((r, i) in blankRects) {
            if (x >= r.left && x < r.right && y >= r.top && y < r.bottom) return i
        }
        val g = hitTester.hit(x, y) ?: return -1
        return geoms.indexOf(g)
    }

    companion object {
        /** Tap layout row width in dp units (LLD-05A §3.1). */
        const val LAYOUT_WIDTH_DP = 360f

        fun build(layout: TapLayout, widthDp: Float, rowHeightDp: Float, sideDp: Float = 0f): TapGeometry {
            require(widthDp > 0f && rowHeightDp > 0f) { "empty tap view" }
            return TapGeometry(layout, widthDp, rowHeightDp, sideDp)
        }
    }
}

/** ⌫ and arrow repeat (LLD-05A §6); `RepeatTimer` behind the view. */
interface TapKeyRepeat {
    fun start(pointerId: Int, key: FunctionKey)
    fun stop(pointerId: Int)
    fun stopAll()
}

/**
 * Multi-pointer touch handling of the tap keyboard, free of Android (LLD-05A §4 `onTouchEvent`).
 * The view turns `MotionEvent`s into dp points per pointer and calls [down], [move], [up] and
 * [cancel]. Function keys act on press, outside the queue (FR-CMN-11, 11a ①); character keys are
 * resolved on release from the first-pressed key only and leave in press order ([MultiPointerQueue]).
 * The mode key is judged on release, also outside the queue (issue #205): a first crossed edge up opens the
 * settings ([TapSink.onSettingsStroke]), anything else is the mode change (`MODE` down then up).
 */
class TapTouchLogic(
    private val sink: TapSink,
    private val guard: EngineGuard,
    private val feedback: FeedbackPort,
    private val repeat: TapKeyRepeat,
) {
    /** Built by the view on layout; null before the first layout (touches are then ignored). */
    var geometry: TapGeometry? = null

    /** The minimum crossing distance (FR-TAP-02b). */
    var minCrossDp: Float = 0f

    private class Stroke(val index: Int, val key: TapKeyDef, val classifier: TapGestureClassifier)

    private val queue = MultiPointerQueue()
    private val strokes = HashMap<Int, Stroke>()
    /** [classifier]: the mode key's stroke, judged on release (issue #205); null for keys that act on press. */
    private class FnPress(val index: Int, val key: FunctionKey, val classifier: TapGestureClassifier? = null)

    private val fnPointers = HashMap<Int, FnPress>()

    /** Indices of keys held down now, for the pressed state (LLD-05A §4 `onDraw`). */
    val pressed: Collection<Int> get() = strokes.values.map { it.index } + fnPointers.values.map { it.index }

    /**
     * [first] = `ACTION_DOWN`: a new gesture starts, so anything left from the last one (its end
     * never arrived) is ended as [cancel] first.
     */
    fun down(pointerId: Int, p: StrokePoint, first: Boolean = false) {
        if (first && (strokes.isNotEmpty() || fnPointers.isNotEmpty())) cancel()
        val g = geometry ?: return
        val i = g.indexAt(p.x, p.y)
        if (i < 0) return
        val key = g.keys[i]
        feedback.key()   // on press, blanks included (FR-CMN-11a ②)
        if (key.kind == TapKeyKind.FUNCTION && key.function == FunctionKey.MODE) {
            val c = TapGestureClassifier(g.geoms[i], minCrossDp)
            c.begin(p)
            fnPointers[pointerId] = FnPress(i, FunctionKey.MODE, c)   // nothing on press (issue #205)
        } else if (key.kind == TapKeyKind.FUNCTION) {
            val fn = checkNotNull(key.function) { key.id }
            fnPointers[pointerId] = FnPress(i, fn)
            guard.stroke(SITE_FN) { sink.onFunctionDown(fn) }   // on press, outside the queue (FR-CMN-11, 11a ①)
            if (fn in REPEATING) repeat.start(pointerId, fn)   // FR-CMN-07c, FR-TAP-10
        } else {
            val c = TapGestureClassifier(g.geoms[i], minCrossDp)
            c.begin(p)
            strokes[pointerId] = Stroke(i, key, c)
            queue.onDown(pointerId)
        }
    }

    /** History points then the current point of one pointer. */
    fun move(pointerId: Int, points: List<StrokePoint>) {
        fnPointers[pointerId]?.classifier?.let { c -> points.forEach(c::add) }
        val s = strokes[pointerId] ?: return
        points.forEach(s.classifier::add)
    }

    fun up(pointerId: Int, p: StrokePoint) {
        fnPointers.remove(pointerId)?.let { f ->
            if (f.classifier != null) return modeUp(f.classifier, p)
            repeat.stop(pointerId)
            guard.stroke(SITE_FN) { sink.onFunctionUp(f.key) }
            return
        }
        val s = strokes.remove(pointerId) ?: return
        val r = guard.run(SITE_CLASSIFY) { TapKeyResolver.resolve(s.key, s.classifier.finish(p)) }
        val outs = if (r == null) queue.onCancel(pointerId) else queue.onUp(pointerId, s.key, r)   // a failed slot only
        emit(outs)
    }

    /**
     * `ACTION_CANCEL` (FR-CMN-11a ③, DCR-11): pointers not yet released give nothing, results already
     * released leave in press order, function keys only stop repeating (their press stays done). A mode
     * key not yet released gives nothing (issue #205).
     */
    fun cancel() {
        val outs = queue.onCancelAll()
        repeat.stopAll()
        releaseFunctionKeys()
        strokes.clear()
        emit(outs)
    }

    /**
     * The view left the window mid-touch (mode key → another mode view): ended like [cancel], so a
     * released key waiting for a held one is not lost (FR-CMN-11; PM decision on PR #114).
     */
    fun detach() = cancel()

    /**
     * Accessibility click on key [index] (LLD-05A §7): the key's [TapGesture.Tap] result to the sink,
     * function keys as down then up. Outside the queue; it does not touch the pointers in progress.
     */
    fun click(index: Int) {
        val key = geometry?.keys?.getOrNull(index) ?: return
        val fn = key.function
        if (key.kind == TapKeyKind.FUNCTION && fn != null) {
            guard.stroke(SITE_FN) { sink.onFunctionDown(fn) }
            guard.stroke(SITE_FN) { sink.onFunctionUp(fn) }
            return
        }
        val r = guard.run(SITE_CLASSIFY) { TapKeyResolver.resolve(key, TapGesture.Tap) } ?: return
        emit(listOf(key to r))
    }

    /** Mode key release (issue #205): first edge up = the settings, else the mode change (FR-CMN-13); a failed judgement gives nothing (LLD-09 §2). */
    private fun modeUp(c: TapGestureClassifier, p: StrokePoint) {
        val settings = guard.run(SITE_CLASSIFY) { ModeKeyGesture.of(c.finish(p)) == ModeKeyGesture.OPEN_SETTINGS } ?: return
        if (settings) {
            guard.stroke(SITE_FN) { sink.onSettingsStroke() }
        } else {
            guard.stroke(SITE_FN) { sink.onFunctionDown(FunctionKey.MODE) }
            guard.stroke(SITE_FN) { sink.onFunctionUp(FunctionKey.MODE) }
        }
    }

    private fun releaseFunctionKeys() {
        val fns = fnPointers.values.filter { it.classifier == null }.map { it.key }   // an unreleased mode key was never pressed for the sink
        fnPointers.clear()
        for (fn in fns) guard.stroke(SITE_FN) { sink.onFunctionUp(fn) }
    }

    /** One output = one guarded unit (LLD-09 §2), in press order. */
    private fun emit(outs: List<Pair<TapKeyDef, TapOutput>>) {
        for ((k, o) in outs) guard.stroke(SITE_OUTPUT) { sink.onTapOutput(k, o) }
    }

    companion object {
        const val SITE_FN = "Tap.fn"
        const val SITE_CLASSIFY = "Tap.classify"
        const val SITE_OUTPUT = "Tap.output"
        const val SITE_REPEAT = "Repeat"

        /** Keys that repeat while held (FR-CMN-07c, FR-TAP-10). */
        val REPEATING = setOf(
            FunctionKey.BACKSPACE, FunctionKey.ARROW_LEFT, FunctionKey.ARROW_UP, FunctionKey.ARROW_DOWN, FunctionKey.ARROW_RIGHT,
            FunctionKey.DELETE,   // Del repeats like ⌫ (issue #198, PM recommendation)
        )
    }
}
