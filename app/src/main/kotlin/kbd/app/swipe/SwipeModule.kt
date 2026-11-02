package kbd.app.swipe

import kbd.hangul.stroke.DotPlace
import kbd.hangul.stroke.StrokePop
import android.content.Context
import android.view.View
import kbd.app.error.EngineGuard
import kbd.app.feedback.FeedbackPort
import kbd.app.ime.FieldInfo
import kbd.app.store.Settings
import kbd.app.view.CenterText
import kbd.hangul.event.JamoEvent
import kbd.hangul.jamo.Jamo
import kbd.stroke.layout.FunctionKey
import kbd.stroke.threshold.Thresholds

/**
 * The swipe engine as the rest of the app reaches it (LLD-03 §2.2): the swipe view. One edition since issue #251
 * (owner): every build has it (`SwipeModuleProvider`). The input practice (FSS#17) shows the same view through its
 * own [SwipeHost]; the old tutor lessons (`tutorSteps`) are gone.
 */
interface SwipeModule {
    /** Builds the swipe keyboard view. */
    fun createSwipeView(context: Context, host: SwipeHost): View
}

/** Implemented by `KeyboardService`, called by the swipe view (LLD-03 §2.2). */
interface SwipeHost {
    val thresholds: Thresholds
    val settings: Settings
    val fieldInfo: FieldInfo

    /**
     * The consonant the next vowel attaches to (the composer's `lastConsonant()`: the final's last consonant,
     * else the initial; null = nothing composing or a bare vowel). When it is not null the swipe view judges
     * vowel strokes against C (SWIPE 3.6.3, TBD-24).
     */
    val centerConsonant: Jamo? get() = null

    /**
     * What the swipe view draws in the place C (SWIPE 9.5, FR-DSP-10, FSS#36): the composing syllable, its pending
     * dots drawn in place (`InputDispatcher.centerText`); null = nothing. The default draws [centerConsonant] alone
     * (hosts without a composer).
     */
    val centerText: CenterText? get() = centerConsonant?.let { CenterText(it.compat.toString()) }

    /** `SwipeInputMapper` output for a consonant-key stroke (HLD 4.1). */
    fun onSwipeEvents(events: List<JamoEvent>, errorVibe: Boolean = false)   // errorVibe: unfinished vowel strokes dropped (SWIPE 3.3)

    /** Result of the perimeter number and symbol keys. */
    fun onSwipeText(text: String, errorVibe: Boolean)

    /** ⌫, space, ↵ on press; the mode key on release when it was a tap (issue #205). */
    fun onFunctionKey(key: FunctionKey)

    /** End of ⌫ repeat. */
    fun onFunctionKeyUp(key: FunctionKey)

    /** Up stroke on the mode key: commit what is composing, then open the keyboard settings (issue #205). */
    fun onSettingsStroke() {}

    /** Per-stroke protection (LLD-09). */
    val guard: EngineGuard

    /** Vibration and sound on key press (FR-CMN-11a ②, LLD-08 §8). */
    val feedback: FeedbackPort

    /** The swipe view hands over its wrapped `SwipeInputMapper` when created, and null when it goes away. */
    fun registerSwipeState(state: SwipeEngineState?)
}

/** Boundary through which main handles `SwipeInputMapper` without seeing gesture-engine types (LLD-03 §2.2). */
interface SwipeEngineState {
    /**
     * = `SwipeInputMapper.reset()`: both vowel assemblies close (FR-CMN-03d); true when vowel strokes that
     * made no vowel yet were dropped (the caller vibrates, SWIPE 3.3, issue #173).
     */
    fun reset(): Boolean

    /** = `SwipeInputMapper.pendingGlyph()`: ㆍ·‥ of a vowel not made yet (SWIPE 3.4, FR-SWP-64). */
    fun pendingGlyph(): String = ""

    /** = `SwipeInputMapper.pendingReplacesVowel()`: the glyph is shown in place of the composer's last character (bare ‥, SWIPE 0.36 3.4). */
    fun pendingReplacesVowel(): Boolean = false

    /** = `SwipeInputMapper.pendingDots()`: the pending vowel-stroke dots and their places, drawn in the center place (SWIPE 9.5, FSS#36). */
    fun pendingDots(): List<DotPlace> = emptyList()

    /** = `SwipeInputMapper.popElement()`: ⌫ takes back one vowel stroke or cheonjiin element first (SWIPE 3.5, FR-SWP-64). */
    fun popElement(): StrokePop = StrokePop.Empty

    /** = `SwipeInputMapper.snapshot()`. */
    fun snapshot(): Any

    fun restore(s: Any)
}
