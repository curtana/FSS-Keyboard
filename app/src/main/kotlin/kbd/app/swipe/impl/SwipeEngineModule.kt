package kbd.app.swipe.impl

import android.content.Context
import android.view.View
import kbd.app.store.Settings
import kbd.app.swipe.SwipeHost
import kbd.app.swipe.SwipeModule
import kbd.app.tutor.TutorSteps
import kbd.app.tutor.PracticeEcho
import kbd.app.tutor.PracticeGuide
import kbd.app.tutor.PracticeKey
import kbd.app.tutor.PracticeKeys
import kbd.app.tutor.PracticeLayout
import kbd.app.tutor.PracticeLayoutSource
import kbd.app.tutor.TutorStep
import kbd.gesture.classify.SwipeKeyKind
import kbd.gesture.layout.SwipeLayout
import kbd.stroke.layout.FunctionKey
import kbd.stroke.threshold.Thresholds

/** The swipe module (LLD-03 §2.2): the swipe keyboard view, the tutor's lessons and its practice keyboards. */
object SwipeEngineModule : SwipeModule, PracticeLayoutSource {
    override fun createSwipeView(context: Context, host: SwipeHost): View = SwipeKeyboardView(context, host)

    /** Lessons 1, 2, 3 and 5 (lesson 4 removed, issue #150; LLD-07 §2.2, FR-TUT-02). */
    override fun tutorSteps(): List<TutorStep> = TutorSteps.ALL

    /** The swipe consonant keys of the real swipe geometry (LLD-07 §2.3, HLD 7.1); empty cells are left out. */
    override fun practiceKeys(layout: PracticeLayout): PracticeKeys = when (layout) {
        PracticeLayout.SWIPE_CONSONANT_KEY -> SWIPE_PRACTICE
        PracticeLayout.SWIPE_SENTENCE -> SWIPE_SENTENCE
    }

    /** The swipe keyboard's own decision for the "입력: …" line (FR-TUT-09). */
    override fun practiceEcho(layout: PracticeLayout, th: Thresholds, rowHeightDp: Float): PracticeEcho = SwipePracticeEcho(th, rowHeightDp)

    /**
     * The swipe keyboard's guide on the swipe practice keyboards (FR-TUT-11); the tutor has no input
     * field, so only the settings switch the guides (FR-DSP-08).
     */
    override fun practiceGuide(
        layout: PracticeLayout, anchor: View, th: Thresholds, settings: Settings,
        preview: (kbd.hangul.stroke.VowelStroke) -> String?,
    ): PracticeGuide {
        StrokeDebugLog.init(anchor.context)   // the practice echo's FSSStroke lines (issue #217)
        return SwipePracticeGuide(anchor, th, GuideFlags.of(settings, field = null), settings.rowHeightDp, preview = preview)
    }

    private val SWIPE_PRACTICE = PracticeKeys(SwipeLayout.ROWS) { widthDp, rowHeightDp ->
        val g = SwipeGeometry.build(widthDp, rowHeightDp)
        g.keys.indices.filter { g.keys[it].kind != SwipeKeyKind.EMPTY }.map { i ->
            PracticeKey(g.keys[i].label, g.geoms[i].rect, g.keys[i].kind == SwipeKeyKind.CONSONANT || g.keys[i].kind == SwipeKeyKind.CHEONJIIN)
        }
    }

    /**
     * Lesson 5: the consonant keys and the cheonjiin key plus the space key, so sentences can have
     * spaces, and ⌫, so a mistake can be corrected as on the keyboard (LLD-07 §2.3, PM decision, issue #165).
     */
    private val SWIPE_SENTENCE = PracticeKeys(SwipeLayout.ROWS) { widthDp, rowHeightDp ->
        val g = SwipeGeometry.build(widthDp, rowHeightDp)
        g.keys.indices.filter { g.keys[it].kind != SwipeKeyKind.EMPTY }.map { i ->
            val k = g.keys[i]
            val space = k.kind == SwipeKeyKind.FUNCTION && k.function == FunctionKey.SPACE
            val backspace = k.kind == SwipeKeyKind.FUNCTION && k.function == FunctionKey.BACKSPACE
            PracticeKey(k.label, g.geoms[i].rect, k.kind == SwipeKeyKind.CONSONANT || k.kind == SwipeKeyKind.CHEONJIIN || space || backspace,
                space, backspace)
        }
    }
}
