package kbd.app.swipe.impl

import kbd.app.swipe.SwipeEngineState
import kbd.gesture.input.SwipeInputMapper
import kbd.gesture.input.SwipeMapperSnapshot
import kbd.hangul.stroke.OtherRole
import kbd.hangul.stroke.StrokePop

/** [SwipeInputMapper] as app main sees it (LLD-04 §9.3, LLD-03 §2.2). */
class SwipeMapperState(private val m: SwipeInputMapper) : SwipeEngineState {
    override fun reset(): Boolean = m.reset()

    override fun pendingGlyph(): String = m.pendingGlyph()

    override fun pendingReplacesVowel(): Boolean = m.pendingReplacesVowel()

    override fun vowelStrokes(): Int = m.strokes.size

    override fun otherRole(): OtherRole? = m.otherRole()

    override fun popElement(): StrokePop = m.popElement()

    override fun snapshot(): Any = m.snapshot()

    override fun restore(s: Any) = m.restore(s as SwipeMapperSnapshot)
}
