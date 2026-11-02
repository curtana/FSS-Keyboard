package kbd.app.settings

import android.content.res.Configuration
import kbd.app.R
import kbd.app.view.ViewTestSdk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * The settings screen marks the key size and alignment rows of the present orientation (LLD-07 §7, FSS#44): on a
 * portrait screen the landscape alignment does not move the keyboard on screen, so the rows must say which is which.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ViewTestSdk.SDK])
class WidthRowsNowTest {

    @Test
    fun `WN-01 the present orientation's rows are the marked ones`() {
        assertTrue(isPresentOrientation(landscapeRows = false, Configuration.ORIENTATION_PORTRAIT))
        assertFalse(isPresentOrientation(landscapeRows = true, Configuration.ORIENTATION_PORTRAIT))
        assertTrue(isPresentOrientation(landscapeRows = true, Configuration.ORIENTATION_LANDSCAPE))
        assertFalse(isPresentOrientation(landscapeRows = false, Configuration.ORIENTATION_LANDSCAPE))
        // An undefined orientation is not landscape: the portrait rows are marked, as the keyboard places it.
        assertTrue(isPresentOrientation(landscapeRows = false, Configuration.ORIENTATION_UNDEFINED))
    }

    @Test
    fun `WN-02 the mark under the present orientation's titles reads "지금 화면"`() {
        assertEquals("지금 화면", RuntimeEnvironment.getApplication().getString(R.string.settings_now))
    }
}
