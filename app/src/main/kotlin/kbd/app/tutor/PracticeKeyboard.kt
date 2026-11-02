package kbd.app.tutor

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kbd.app.store.Settings
import kbd.app.swipe.SwipeModule
import kbd.app.view.KeyboardRootView
import kbd.stroke.geom.RectDp
import kbd.stroke.geom.StrokePoint
import kbd.stroke.threshold.Thresholds

/**
 * One key of a practice keyboard in dp; only [practised] keys take practice strokes. [space] = the space key,
 * [backspace] = ⌫ (lesson 5; ⌫ since issue #165).
 */
data class PracticeKey(val label: String, val rect: RectDp, val practised: Boolean, val space: Boolean = false, val backspace: Boolean = false)

/** Key rectangles of a practice keyboard of [rows] rows for a width and row height (HLD 7.1, FR-CMN-12). */
class PracticeKeys(val rows: Int, private val build: (widthDp: Float, rowHeightDp: Float) -> List<PracticeKey>) {
    fun keys(widthDp: Float, rowHeightDp: Float): List<PracticeKey> = build(widthDp, rowHeightDp)
}

/**
 * The swipe module's practice layouts, implemented next to [SwipeModule] (the swipe consonant keys from the real
 * swipe geometry, LLD-07 §2.3).
 */
interface PracticeLayoutSource {
    fun practiceKeys(layout: PracticeLayout): PracticeKeys?

    /** The "입력: …" interpreter of [layout] (FR-TUT-09); null = not this module's layout. */
    fun practiceEcho(layout: PracticeLayout, th: Thresholds, rowHeightDp: Float): PracticeEcho?

    /**
     * The real keyboard's stroke guide on the practice keyboard [anchor] (FR-TUT-11); null = no guide
     * for [layout]. The guides follow [settings] (FR-DSP-08); every consonant key shows it (issue #168).
     */
    /** [preview]: what a vowel stroke would show with the echo's open assembly (the stroke guide, SWIPE 9). */
    fun practiceGuide(
        layout: PracticeLayout, anchor: View, th: Thresholds, settings: Settings,
        preview: (kbd.hangul.stroke.VowelStroke) -> String? = { null },
    ): PracticeGuide?
}

/**
 * The stroke guide of a practice keyboard (FR-TUT-11, HLD 7.1), fed the same touches as the
 * [PracticeStrokeCollector]. Display only: samples and the "입력: …" line do not depend on it.
 */
interface PracticeGuide {
    /** A stroke starts at [p] on a keyboard [widthDp] wide (the key geometry). */
    fun down(p: StrokePoint, widthDp: Float)

    fun move(ps: List<StrokePoint>)

    /** Released or cancelled: the guide hides, its timers stop. */
    fun up()

    /** The keyboard goes away: also closes the guide window. */
    fun dismiss()
}

/** The practice keyboard of a step uses the same layout data as the real keyboard (LLD-07 §2.3, HLD 7.1). */
object PracticeLayouts {

    private fun source(module: SwipeModule): PracticeLayoutSource =
        checkNotNull(module as? PracticeLayoutSource) { "the swipe module has no practice layouts" }

    fun of(layout: PracticeLayout, module: SwipeModule): PracticeKeys = checkNotNull(source(module).practiceKeys(layout)) { "$layout" }

    /** The guide of [layout] on [anchor] (FR-TUT-11). */
    fun guide(
        layout: PracticeLayout, module: SwipeModule, anchor: View, th: Thresholds, settings: Settings,
        preview: (kbd.hangul.stroke.VowelStroke) -> String? = { null },
    ): PracticeGuide? = source(module).practiceGuide(layout, anchor, th, settings, preview)

    /** A fresh "입력: …" interpreter for [layout] with the real keyboard's decision at [th] (FR-TUT-09). */
    fun echo(layout: PracticeLayout, module: SwipeModule, th: Thresholds, rowHeightDp: Float): PracticeEcho =
        checkNotNull(source(module).practiceEcho(layout, th, rowHeightDp)) { "$layout" }
    // The QWERTY 1-1 practice keys and their echo (the public edition's tutor) went with issue #251.
}

/**
 * Turns one pointer's touches into [RawStroke]s (LLD-07 §2.3). A stroke belongs to the practised key it
 * starts on (its label is [RawStroke.key]); touches that start elsewhere are ignored.
 * [RawStroke.prevUpTime] is the UP time of the previous stroke handed out.
 */
class PracticeStrokeCollector(private val onStroke: (RawStroke) -> Unit) {
    var keys: List<PracticeKey> = emptyList()

    private var key: PracticeKey? = null
    private val points = ArrayList<StrokePoint>()
    private var prevUpTime: Long? = null

    /** False when [p] is not on a practised key. */
    fun down(p: StrokePoint): Boolean {
        key = keys.firstOrNull { it.practised && p.x >= it.rect.left && p.x < it.rect.right && p.y >= it.rect.top && p.y < it.rect.bottom }
        points.clear()
        if (key == null) return false
        points += p
        return true
    }

    fun move(ps: List<StrokePoint>) {
        if (key != null) points += ps
    }

    fun up(p: StrokePoint) {
        val k = key ?: return
        points += p
        key = null
        onStroke(RawStroke(k.rect, points.toList(), prevUpTime, k.space, k.label, k.backspace))
        prevUpTime = p.t
        points.clear()
    }

    /** `ACTION_CANCEL`: the stroke in progress gives no sample. */
    fun cancel() {
        key = null
        points.clear()
    }
}

/**
 * Draws [practiceKeys] at the row height setting and feeds one pointer to a [PracticeStrokeCollector].
 * [onStroke] also gets the keyboard width in dp, which the "입력: …" line needs for the key geometry.
 * [guideOf] makes the stroke guide on this view (FR-TUT-11); it sees the same touches as the collector.
 * [onPress] runs when a practice stroke starts, before the guide starts: the tutor
 * clears its ✓/✗ and the echo then (FR-TUT-12, LLD-07 §2.5).
 */
@SuppressLint("ViewConstructor")
class PracticeKeyboardView(
    context: Context,
    private val practiceKeys: PracticeKeys,
    private val rowHeightDp: Float,
    onStroke: (RawStroke, Float) -> Unit,
    private val onPress: () -> Unit = {},
    /** Open vowel strokes of the echo after each stroke (SWIPE 9.4 trail); null = no trail (the QWERTY 1-1). */
    private val trailOpen: () -> Int? = { null },
    /** The echo's center consonant and its place C in dp (SWIPE 9.5, the tutor too); null = none. */
    private val center: () -> kbd.app.view.CenterMark? = { null },
    guideOf: (View) -> PracticeGuide? = { null },
) : View(context) {

    private val density = resources.displayMetrics.density
    private var widthDp = 0f
    private val collector = PracticeStrokeCollector { onStroke(it, widthDp) }
    private var activePointerId = NONE
    private val keyRect = RectF()
    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = KEY_COLOR }
    private val otherPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = OTHER_KEY_COLOR }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = LABEL_COLOR; textAlign = Paint.Align.CENTER }
    private val otherLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = OTHER_LABEL_COLOR; textAlign = Paint.Align.CENTER }
    /** The vowel strokes' trail, as on the keyboard (SWIPE 9.4: the tutor's practice keyboard too). */
    val trail = kbd.app.swipe.StrokeTrail()
    private val trailPainter = kbd.app.view.TrailPainter(density)
    private val centerPainter = kbd.app.view.CenterConsonantPainter()
    private val centerBox = RectF()

    init {
        setBackgroundColor(KeyboardRootView.BACKGROUND_COLOR)
    }

    /** Null = no guide for this layout. */
    val guide: PracticeGuide? = guideOf(this)

    override fun onDetachedFromWindow() {
        activePointerId = NONE
        collector.cancel()
        guide?.dismiss()
        super.onDetachedFromWindow()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), Math.round(practiceKeys.rows * rowHeightDp * density))
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        collector.cancel()
        guide?.up()
        widthDp = w / density
        collector.keys = if (w > 0) practiceKeys.keys(w / density, rowHeightDp) else emptyList()
        labelPaint.textSize = rowHeightDp * density * LABEL_SCALE
        otherLabelPaint.textSize = labelPaint.textSize
    }

    override fun onDraw(canvas: Canvas) {
        val inset = KEY_INSET_DP * density
        for (k in collector.keys) {
            val r = k.rect
            keyRect.set(r.left * density + inset, r.top * density + inset, r.right * density - inset, r.bottom * density - inset)
            canvas.drawRoundRect(keyRect, inset * 2, inset * 2, if (k.practised) keyPaint else otherPaint)
            val paint = if (k.practised) labelPaint else otherLabelPaint
            // A label wider than the key (the cheonjiin key's "ㆍ ㅡ ㅣ", issue #160) is drawn smaller to fit.
            val size = paint.textSize
            val w = paint.measureText(k.label)
            if (w > keyRect.width() * LABEL_FIT) paint.textSize = size * keyRect.width() * LABEL_FIT / w
            canvas.drawText(k.label, keyRect.centerX(), keyRect.centerY() - (paint.ascent() + paint.descent()) / 2, paint)
            paint.textSize = size
        }
        // SWIPE 9.5: in the place C, under the trail. Practice fields are never password fields; no guide setting turns it off.
        center()?.let { m ->
            centerBox.set(m.box.left * density, m.box.top * density, m.box.right * density, m.box.bottom * density)
            centerPainter.draw(canvas, kbd.app.view.CenterConsonantPainter.textFor(m.consonant, null), centerBox)
        }
        trailOpen()?.let { trail.sync(it) }
        trailPainter.draw(canvas, trail.paths)
    }

    /** A Hangul consonant key: where vowel strokes start (SWIPE 9.4 draws only those). */
    private fun consonantAt(p: StrokePoint): Boolean = collector.keys.firstOrNull { k ->
        p.x >= k.rect.left && p.x < k.rect.right && p.y >= k.rect.top && p.y < k.rect.bottom
    }?.label?.singleOrNull()?.let { it in 'ㄱ'..'ㅎ' } == true

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        invalidate()   // the trail follows the finger
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                if (activePointerId != NONE) return true   // one stroke at a time
                val i = e.actionIndex
                val p = pointAt(e, i)
                if (collector.down(p)) {
                    activePointerId = e.getPointerId(i)
                    onPress()
                    guide?.down(p, widthDp)
                    if (trailOpen() != null && consonantAt(p)) trail.begin(p)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val i = e.findPointerIndex(activePointerId)
                if (i >= 0) {
                    val ps = ArrayList<StrokePoint>(e.historySize + 1)
                    for (h in 0 until e.historySize) {
                        ps += StrokePoint(e.getHistoricalX(i, h) / density, e.getHistoricalY(i, h) / density, e.getHistoricalEventTime(h))
                    }
                    ps += pointAt(e, i)
                    collector.move(ps)
                    guide?.move(ps)
                    ps.forEach(trail::add)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val i = e.actionIndex
                if (e.getPointerId(i) == activePointerId) {
                    guide?.up()
                    val before = trailOpen() ?: 0
                    trail.add(pointAt(e, i))
                    collector.up(pointAt(e, i))
                    val open = trailOpen()
                    // Kept when the echo's assembly took the stroke (one more open stroke), dropped otherwise.
                    if (open != null && open > before) trail.keep() else trail.drop()
                    open?.let { trail.sync(it) }
                    activePointerId = NONE
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                collector.cancel()
                trail.drop()
                guide?.up()
                activePointerId = NONE
            }
        }
        return true
    }

    private fun pointAt(e: MotionEvent, i: Int) = StrokePoint(e.getX(i) / density, e.getY(i) / density, e.eventTime)

    private companion object {
        const val NONE = -1
        const val KEY_INSET_DP = 2f
        const val LABEL_SCALE = 0.4f
        const val LABEL_FIT = 0.8f
        const val KEY_COLOR = 0xFF3C3C3C.toInt()
        const val OTHER_KEY_COLOR = 0xFF1E1E1E.toInt()
        const val LABEL_COLOR = 0xFFFFFFFF.toInt()
        const val OTHER_LABEL_COLOR = 0xFF5A5A5A.toInt()
    }
}

/** The practice keyboard composable (LLD-07 §2.3): the real layout's key size at the row height setting. */
@Composable
fun PracticeKeyboard(
    practiceKeys: PracticeKeys,
    rowHeightDp: Float,
    onStroke: (RawStroke, Float) -> Unit,
    modifier: Modifier = Modifier,
    onPress: () -> Unit = {},
    trailOpen: () -> Int? = { null },
    center: () -> kbd.app.view.CenterMark? = { null },
    /** Bumped when the echo changes without a touch (a new sentence or step): redraws the center consonant. */
    redraw: Int = 0,
    guideOf: (View) -> PracticeGuide? = { null },
) {
    AndroidView(
        factory = { PracticeKeyboardView(it, practiceKeys, rowHeightDp, onStroke, onPress, trailOpen, center, guideOf) },
        modifier = modifier.fillMaxWidth().height((practiceKeys.rows * rowHeightDp).dp),
        // The lambda captures [redraw]: a new tick recomposes with a new lambda, which AndroidView runs, so the view redraws.
        update = { v ->
            @Suppress("UNUSED_VARIABLE") val tick = redraw
            v.invalidate()
        },
    )
}
