package kbd.app.swipe.impl

import kbd.app.ime.FieldInfo
import kbd.app.store.Settings
import kbd.stroke.threshold.Thresholds

/**
 * Guide switches (LLD-06 §2). All three come from settings (FR-DSP-08, FR-SET-02); a password field
 * also turns [bigCurrentOn] off (FR-CMN-08 ①; a no-learning field shows it, ②). All three off = no guide at all.
 */
data class GuideFlags(val directionOn: Boolean, val nextCharOn: Boolean, val bigCurrentOn: Boolean) {
    companion object {
        /** [field] null = the tutor practice keyboard, which has no input field (FR-TUT-11, issue #135). */
        fun of(s: Settings, field: FieldInfo?): GuideFlags = GuideFlags(
            directionOn = s.showDirectionGuide,
            nextCharOn = s.showNextChar,
            bigCurrentOn = s.showCurrentChar && field?.hidesDisplay != true,
        )
    }
}

enum class GuidePhase { IDLE, PRESSED_WAITING, SHOWING }

/** What the overlay draws. */
data class GuideView(val showDirection: Boolean, val showCurrent: Boolean, val showNext: Boolean)

/** ① show delay (HLD 6). The dwell timer (issue #127) and the hold timer (issue #142) are gone. */
enum class TimerKind { SHOW_DELAY }

data class TimerRequest(val kind: TimerKind, val atMs: Long)

/**
 * Guide timing, pure (LLD-06 §2, HLD 6 timer ①). The caller posts the [TimerRequest]s from
 * [onDown] and feeds them back through [onTimer]; a cancelled or stale timer is
 * ignored, so the caller need not remove posted callbacks. View methods return the new
 * [GuideView], or null when nothing changed ([onUp] always returns the all-hidden view).
 *
 * [moveStartDp] = platform touch slop in dp (`ViewConfiguration.scaledTouchSlop`, D-17).
 */
class GuideTimerLogic(
    private val th: Thresholds,
    private val flags: GuideFlags,
    private val moveStartDp: Float,
) {
    var phase: GuidePhase = GuidePhase.IDLE
        private set

    private var showDelayAt: Long? = null

    fun onDown(t: Long): List<TimerRequest> {
        phase = GuidePhase.PRESSED_WAITING
        val delay = TimerRequest(TimerKind.SHOW_DELAY, t + th.guideDelayMs)
        showDelayAt = delay.atMs
        return listOf(delay)
    }

    /** Moving [moveStartDp] or more starts the guide at once and cancels the show delay (FR-DSP-01). */
    fun onMove(movedDp: Float, t: Long): GuideView? {
        if (phase == GuidePhase.IDLE) return null
        if (phase == GuidePhase.PRESSED_WAITING && movedDp >= moveStartDp) {
            phase = GuidePhase.SHOWING
            showDelayAt = null
            return view()
        }
        return null
    }

    /** The show-delay timer: shows the guide if it is still waiting. */
    fun onTimer(kind: TimerKind, t: Long): GuideView? {
        if (phase == GuidePhase.IDLE || kind != TimerKind.SHOW_DELAY) return null
        val at = showDelayAt ?: return null
        if (t < at) return null
        showDelayAt = null
        if (phase != GuidePhase.PRESSED_WAITING) return null
        phase = GuidePhase.SHOWING
        return view()
    }

    /** All hidden, timers cancelled. Released before the show delay without moving → never shown (FR-DSP-07). */
    fun onUp(): GuideView {
        phase = GuidePhase.IDLE
        showDelayAt = null
        return view()
    }

    private fun view(): GuideView {
        val showing = phase == GuidePhase.SHOWING
        return GuideView(
            showDirection = showing && flags.directionOn,
            showCurrent = showing && flags.bigCurrentOn,
            showNext = showing && flags.nextCharOn,
        )
    }
}
