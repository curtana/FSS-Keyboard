package kbd.app.view

import android.graphics.Canvas
import android.graphics.Paint
import android.view.accessibility.AccessibilityNodeInfo
import java.io.File
import kbd.app.error.EngineGuard
import kbd.app.error.GuardedState
import kbd.app.feedback.FeedbackPort
import kbd.app.store.ErrorLog
import kbd.stroke.geom.Dir
import kbd.stroke.layout.FunctionKey
import kbd.stroke.layout.TapKeyDef
import kbd.stroke.layout.TapKeyKind
import kbd.stroke.layout.TapLayout
import kbd.stroke.layout.TapLayouts
import kbd.stroke.tap.ChordModifier
import kbd.stroke.tap.TapKeyResolver
import kbd.stroke.tap.TapOutput
import kbd.stroke.threshold.ThresholdDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/**
 * LLD-11 §7.5 CK-01..CK-04: Caps Lock on `TapKeyboardView` (LLD-05A §4, §7; SRS FR-TAP-19 ②, FSS#131) — the
 * letters drawn and named in upper case, the ↑ hint gone, the rest as it was. The sink stands in for
 * `InputDispatcher`: the test sets [Sink.capsLock] and tells the view, as `onCapsChanged` does.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ViewTestSdk.SDK])
@LooperMode(LooperMode.Mode.PAUSED)
class TapKeyboardCapsViewTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private class Sink : TapSink {
        override var capsLock = false
        override var chord: Set<ChordModifier> = emptySet()
        override fun onTapOutput(key: TapKeyDef, out: TapOutput) = Unit
        override fun onFunctionDown(key: FunctionKey) = Unit
        override fun onFunctionUp(key: FunctionKey) = Unit
    }

    private val sink = Sink()
    private lateinit var view: TapKeyboardView

    private val feedback = object : FeedbackPort {
        override fun key() = Unit
        override fun error() = Unit
    }

    private object NoState : GuardedState {
        override fun snapshot(): Any = Unit
        override fun restore(s: Any) {}
    }

    private fun show(layout: TapLayout = TapLayouts.QWERTY) {
        val guard = EngineGuard(ErrorLog(File(tmp.root, "errors.log")), NoState)
        view = TapKeyboardView(RuntimeEnvironment.getApplication(), sink, guard, feedback)
        view.setLayout(layout, ThresholdDefaults.VALUES, 48f)
        val density = view.resources.displayMetrics.density
        layoutExactly(view, Math.round(360f * density), Math.round(6 * 48f * density))
    }

    /** Every text drawn, labels and edge hints, in drawing order. */
    private class Texts : Canvas() {
        val texts = ArrayList<String>()
        override fun drawText(text: String, x: Float, y: Float, paint: Paint) {
            texts += text
        }
    }

    private fun drawn(): List<String> = Texts().also { view.draw(it) }.texts

    private fun index(layout: TapLayout, id: String) = layout.keys.indexOfFirst { it.id == id }

    private fun node(i: Int): AccessibilityNodeInfo = view.accessibilityNodeProvider.createAccessibilityNodeInfo(i)!!

    private fun names(layout: TapLayout) = layout.keys.indices.map { node(it).contentDescription.toString() }

    private fun caps(on: Boolean) {
        sink.capsLock = on
        view.capsChanged()
    }

    @Test
    fun `CK-01 on - every QWERTY letter drawn and named in upper case, other keys as they were`() {
        show()
        val q = TapLayouts.QWERTY
        val offNames = names(q)
        caps(true)
        val onNames = names(q)
        for ((i, k) in q.keys.withIndex()) {
            val want = if (k.kind == TapKeyKind.QWERTY_LETTER) offNames[i].uppercase() else offNames[i]
            assertEquals(k.id, want, onNames[i])
        }
        assertEquals("A", onNames[index(q, "a")])
        assertEquals("1", onNames[index(q, "1")])
        val texts = drawn()
        for (c in 'a'..'z') {
            assertTrue("$c", c.uppercase() in texts)
            assertFalse("$c", c.toString() in texts)   // no lower-case label and no lower-case hint is left
        }
        caps(false)
        assertEquals(offNames, names(q))
        assertTrue("a" in drawn())
    }

    @Test
    fun `CK-02 on - the up hint of a letter is gone, its other hints and the digit hints stay`() {
        show()
        val q = TapLayouts.QWERTY
        val i = q.keys.single { it.id == "i" }
        val one = q.keys.single { it.id == "1" }
        assertEquals("I", view.hintsOf(i)[Dir.UP])
        caps(true)
        assertEquals(null, view.hintsOf(i)[Dir.UP])   // that ↑ turns Caps Lock off (FR-TAP-19 ③)
        assertEquals(TapKeyResolver.edgeHints(i) - Dir.UP, view.hintsOf(i))   // [ and ] stay
        assertEquals(TapKeyResolver.edgeHints(one), view.hintsOf(one))
        assertEquals(1, drawn().count { it == "I" })   // drawn once, as the label
    }

    @Test
    fun `CK-03 the QWERTY drawing over Dubeolsik follows Caps Lock`() {
        show(TapLayouts.DUBEOLSIK)
        sink.chord = setOf(ChordModifier.CTRL)
        view.chordChanged()
        assertEquals("a", node(index(TapLayouts.QWERTY, "a")).contentDescription.toString())
        caps(true)   // not reachable from Dubeolsik today, but the drawing reads the one state (FR-TAP-15 ⑥)
        assertEquals("A", node(index(TapLayouts.QWERTY, "a")).contentDescription.toString())
        sink.chord = emptySet()
        view.chordChanged()
        assertEquals("ㅁ", node(index(TapLayouts.DUBEOLSIK, "a")).contentDescription.toString())   // jamo have no case
    }

    @Test
    fun `CK-04 a sink without Caps Lock draws lower case, as before`() {
        show()
        assertFalse(sink.capsLock)
        val texts = drawn()
        assertTrue("a" in texts)
        assertTrue("A" in texts)   // the a key's ↑ hint
    }
}
