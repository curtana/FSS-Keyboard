package kbd.app.swipe.impl

import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewConfiguration
import kbd.app.tutor.PracticeGuide
import kbd.gesture.classify.StrokeClassifier
import kbd.gesture.classify.SwipeKeyKind
import kbd.gesture.progress.ProgressState
import kbd.stroke.geom.StrokePoint
import kbd.stroke.threshold.Thresholds

/**
 * The swipe keyboard's guide on the tutor practice keyboard (FR-TUT-11, HLD 7.1, issue #135): the same
 * [GuideOverlay] and [GuideTimerLogic] as [SwipeKeyboardView], fed by a [StrokeClassifier] on the real
 * swipe geometry as [SwipeTouchLogic] does (press state, moves).
 * Consonant keys (double keys too) and the cheonjiin key, every one of them (FR-TUT-11, FR-DSP-01;
 * the anchor-key rule went with issue #168). Display only: a failing classifier just hides the guide.
 */
class SwipePracticeGuide(
    anchor: View,
    private val th: Thresholds,
    flags: GuideFlags,
    private val rowHeightDp: Float,
    handler: Handler = Handler(Looper.getMainLooper()),
    /** The echo's assembler preview, so the guide shows what a vowel stroke gives (SWIPE 9.1–9.2). */
    private val preview: (kbd.hangul.stroke.VowelStroke) -> String? = { null },
) : PracticeGuide {
    private val density = anchor.resources.displayMetrics.density
    private val slopDp = ViewConfiguration.get(anchor.context).scaledTouchSlop / density // D-17

    /** Read by tests. */
    internal val overlay = GuideOverlay(anchor, GuideTimerLogic(th, flags, slopDp), handler)
    private var geometry: SwipeGeometry? = null
    private var classifier: StrokeClassifier? = null

    override fun down(p: StrokePoint, widthDp: Float) {
        up()
        if (widthDp <= 0f) return
        val g = geometry?.takeIf { it.widthDp == widthDp && it.rowHeightDp == rowHeightDp }
            ?: SwipeGeometry.build(widthDp, rowHeightDp).also { geometry = it }
        val i = g.indexAt(p.x, p.y)
        val key = g.keys.getOrNull(i)?.takeIf { it.kind == SwipeKeyKind.CONSONANT || it.kind == SwipeKeyKind.CHEONJIIN } ?: return
        val geo = g.geoms[i]
        val c = runCatching { StrokeClassifier(key, th, geo, preview).also { it.begin(p) } }.getOrNull() ?: return
        classifier = c
        val r = geo.rect
        overlay.onDown(key, Rect(px(r.left), px(r.top), px(r.right), px(r.bottom)), p)
        // As SwipeTouchLogic.down: the guide may show after the delay without any move (FR-DSP-01).
        runCatching { ProgressState.of(c) }.getOrNull()?.let { overlay.onProgress(it, p) }
    }

    override fun move(ps: List<StrokePoint>) {
        val c = classifier ?: return
        for (p in ps) {
            val s = runCatching { c.update(p) }.getOrNull()
            if (s == null) { // the keyboard drops such a stroke: no guide either
                up()
                return
            }
            overlay.onProgress(s, p)
        }
    }

    override fun up() {
        classifier = null
        overlay.onUp()
    }

    override fun dismiss() {
        classifier = null
        overlay.dismiss()
    }

    private fun px(dp: Float): Int = Math.round(dp * density)
}
