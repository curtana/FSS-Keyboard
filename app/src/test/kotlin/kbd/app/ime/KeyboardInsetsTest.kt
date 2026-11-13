package kbd.app.ime

import android.app.Activity
import android.graphics.Rect
import android.inputmethodservice.InputMethodService
import android.os.Looper
import android.widget.FrameLayout
import kbd.app.store.FloatSetting
import kbd.app.store.FloatingKeyboard
import kbd.app.store.KeyboardAlign
import kbd.app.store.KeyboardWidth
import kbd.app.store.WidthSetting
import kbd.app.view.KeyboardRootView
import kbd.app.view.ViewTestSdk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

/**
 * `KeyboardInsets.compute`, what `KeyboardService.onComputeInsets` reports (LLD-11 §7.7 KI-01~KI-04, SRS FR-CMN-14 ⑫,
 * HLD 5A·8장, LLD-08 §7.3, FSS#85).
 *
 * The root is in an activity's window, [LEFT] px from the left and [TOP] px down in its parent, so that window
 * coordinates differ from the root's own. Key 40 dp in the root's width: the keyboard rectangle is 440 dp.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ViewTestSdk.SDK], qualifiers = "w800dp-h1280dp-port")
class KeyboardInsetsTest {

    private lateinit var root: KeyboardRootView
    private val insets = InputMethodService.Insets()

    @Before
    fun setUp() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        root = KeyboardRootView(activity)
        val parent = FrameLayout(activity)
        parent.addView(root, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
            leftMargin = LEFT
            topMargin = TOP
        })
        activity.setContentView(parent)
        traverse()
    }

    /** The window's traversal: measure and layout of what asked for it. */
    private fun traverse() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    private fun set(w: WidthSetting) {
        root.portraitWidth = w
        root.relayout()   // as the service does (applySettings, onConfigurationChanged)
        traverse()
    }

    private fun location(): IntArray = IntArray(2).also { root.getLocationInWindow(it) }

    private fun compute() {
        // What the platform's own onComputeInsets leaves before the service's (InputMethodService).
        insets.touchableInsets = InputMethodService.Insets.TOUCHABLE_INSETS_VISIBLE
        insets.touchableRegion.setEmpty()
        KeyboardInsets.compute(root, insets)
    }

    @Test
    fun `KI-01 a narrowed keyboard - the touchable region is the plate in window coordinates`() {
        for (align in KeyboardAlign.values()) {
            set(WidthSetting(40f, align))
            val loc = location()
            assertTrue(root.isAttachedToWindow && loc[0] >= LEFT && loc[1] >= TOP)
            val plate = root.plate
            assertFalse("$align", root.plateFillsView)
            compute()
            assertEquals("$align", InputMethodService.Insets.TOUCHABLE_INSETS_REGION, insets.touchableInsets)
            assertTrue("$align", insets.touchableRegion.isRect)
            assertEquals(
                "$align",
                Rect(loc[0] + plate.left, loc[1] + plate.top, loc[0] + plate.right, loc[1] + plate.bottom),
                insets.touchableRegion.bounds,
            )
            // The region is the keyboard rectangle: as wide, and as high as the root.
            assertEquals("$align", root.rectWidth, insets.touchableRegion.bounds.width())
            assertEquals("$align", loc[0] + root.rectLeft, insets.touchableRegion.bounds.left)
            assertEquals("$align", root.height, insets.touchableRegion.bounds.height())
        }
    }

    @Test
    fun `KI-02 content and visible insets are the top of the input view whatever the keyboard's width`() {
        for (w in listOf(WidthSetting.DEFAULT, WidthSetting(40f, KeyboardAlign.LEFT), WidthSetting(40f, KeyboardAlign.CENTER))) {
            set(w)
            compute()
            assertEquals("$w", location()[1], insets.contentTopInsets)
            assertEquals("$w", location()[1], insets.visibleTopInsets)
        }
    }

    @Test
    fun `KI-03 full width - the touchable insets stay the visible ones, as before there was a plate`() {
        for (w in listOf(WidthSetting.DEFAULT, WidthSetting(KeyboardWidth.FULL, KeyboardAlign.RIGHT))) {
            set(w)
            assertTrue("$w", root.plateFillsView)
            compute()
            assertEquals("$w", InputMethodService.Insets.TOUCHABLE_INSETS_VISIBLE, insets.touchableInsets)
            assertTrue("$w", insets.touchableRegion.isEmpty)
        }
    }

    @Test
    fun `KI-04 the region follows the layout - narrowed, moved, full and back`() {
        set(WidthSetting(40f, KeyboardAlign.LEFT))
        compute()
        val left = Rect(insets.touchableRegion.bounds)
        set(WidthSetting(40f, KeyboardAlign.RIGHT))
        compute()
        val right = Rect(insets.touchableRegion.bounds)
        assertEquals(left.width(), right.width())
        assertEquals(location()[0] + root.width, right.right)
        assertTrue(right.left > left.left)
        set(WidthSetting(KeyboardWidth.FULL, KeyboardAlign.RIGHT))
        compute()
        assertEquals(InputMethodService.Insets.TOUCHABLE_INSETS_VISIBLE, insets.touchableInsets)
        assertTrue(insets.touchableRegion.isEmpty)   // nothing of the old region is left
        set(WidthSetting(40f, KeyboardAlign.LEFT))
        compute()
        assertEquals(InputMethodService.Insets.TOUCHABLE_INSETS_REGION, insets.touchableInsets)
        assertEquals(left, insets.touchableRegion.bounds)
        // A root that is not laid out yet reports the visible insets.
        val fresh = KeyboardRootView(root.context)
        (root.parent as FrameLayout).addView(fresh)   // attached, no traversal yet
        assertTrue(fresh.plate.isEmpty)
        insets.touchableRegion.set(1, 2, 3, 4)
        KeyboardInsets.compute(fresh, insets)
        assertEquals(InputMethodService.Insets.TOUCHABLE_INSETS_VISIBLE, insets.touchableInsets)
        assertTrue(insets.touchableRegion.isEmpty)
    }

    /** FSS#86: the parent here lets the root be as high as the window's content, as the input method's window does. */
    private fun float(on: Boolean, fx: Float? = null, fy: Float? = null) {
        root.portraitFloat = FloatSetting(on, fx, fy)
        root.relayout()
        traverse()
    }

    @Test
    fun `KI-05 floating - the insets start at the input view's bottom, the touchable region is the plate`() {
        set(WidthSetting(40f, KeyboardAlign.CENTER))
        val dockedHeight = root.height
        float(true, 0.25f, 0.5f)
        assertTrue(root.floated)
        assertTrue(root.height > dockedHeight)
        val loc = location()
        val plate = root.plate
        assertTrue(plate.top > 0 && plate.bottom < root.height)   // in the middle: not at an end
        compute()
        assertEquals(loc[1] + root.height, insets.contentTopInsets)   // the app is left no less than it had
        assertEquals(loc[1] + root.height, insets.visibleTopInsets)
        assertEquals(InputMethodService.Insets.TOUCHABLE_INSETS_REGION, insets.touchableInsets)
        assertTrue(insets.touchableRegion.isRect)
        assertEquals(Rect(loc[0] + plate.left, loc[1] + plate.top, loc[0] + plate.right, loc[1] + plate.bottom), insets.touchableRegion.bounds)
        // Handle and keyboard rectangle: 24 dp and six rows high, the rectangle's width.
        val d = root.resources.displayMetrics.density
        assertEquals(root.rectWidth, insets.touchableRegion.bounds.width())
        assertEquals(Math.round(FloatingKeyboard.HANDLE_DP * d) + Math.round(KeyboardRootView.WINDOW_ROWS * root.rowHeightDp * d), insets.touchableRegion.bounds.height())
        // A full-width floating keyboard: still the region, never the visible insets (they leave nothing to touch).
        set(WidthSetting.DEFAULT)
        assertTrue(root.floated)
        compute()
        assertEquals(InputMethodService.Insets.TOUCHABLE_INSETS_REGION, insets.touchableInsets)
        assertEquals(Rect(location()[0], location()[1] + root.plate.top, location()[0] + root.width, location()[1] + root.plate.bottom), insets.touchableRegion.bounds)
        assertEquals(location()[1] + root.height, insets.visibleTopInsets)
    }

    @Test
    fun `KI-06 the region and the insets follow - moved, put, switched off and on`() {
        set(WidthSetting(40f, KeyboardAlign.CENTER))
        compute()
        val dockedRegion = Rect(insets.touchableRegion.bounds)
        val dockedTop = insets.contentTopInsets
        float(true)
        compute()
        val first = Rect(insets.touchableRegion.bounds)
        // Moved by its handle and put: after the traversal the put asks for, the region is the new plate.
        val t = kbd.app.view.Touches(root)
        val d = root.resources.displayMetrics.density
        val x = root.plate.left + root.plate.width / 2f
        val y = root.plate.top + 12 * d
        t.down(0, x, y)
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(android.view.ViewConfiguration.getLongPressTimeout().toLong()))
        t.move(0, x - 50 * d, y - 70 * d)
        t.up(0)
        traverse()
        compute()
        val moved = Rect(insets.touchableRegion.bounds)
        assertEquals(Rect(first.left - Math.round(50 * d), first.top - Math.round(70 * d), first.right - Math.round(50 * d), first.bottom - Math.round(70 * d)), moved)
        assertEquals(location()[1] + root.height, insets.contentTopInsets)
        // Off: what the docked keyboard reports, nothing of the floating one left.
        float(false, root.portraitFloat.fx, root.portraitFloat.fy)
        assertFalse(root.floated)
        compute()
        assertEquals(dockedRegion, insets.touchableRegion.bounds)
        assertEquals(dockedTop, insets.contentTopInsets)
        assertEquals(dockedTop, insets.visibleTopInsets)
        set(WidthSetting.DEFAULT)
        compute()
        assertEquals(InputMethodService.Insets.TOUCHABLE_INSETS_VISIBLE, insets.touchableInsets)   // KI-03, as before
        assertTrue(insets.touchableRegion.isEmpty)
        // On again: the place it was put at.
        set(WidthSetting(40f, KeyboardAlign.CENTER))
        float(true, root.portraitFloat.fx, root.portraitFloat.fy)
        compute()
        assertEquals(moved, insets.touchableRegion.bounds)
        assertEquals(location()[1] + root.height, insets.visibleTopInsets)
    }

    private companion object {
        const val LEFT = 30
        const val TOP = 50
    }
}
