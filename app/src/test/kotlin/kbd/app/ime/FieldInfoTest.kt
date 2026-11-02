package kbd.app.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** FI-01..FI-12 (LLD-11 §7.1, LLD-08 §2). Literal Android `InputType`/`EditorInfo` values. */
class FieldInfoTest {

    private companion object {
        const val TEXT = 0x1
        const val NUMBER = 0x2
        const val PHONE = 0x3
        const val DATETIME = 0x4
        const val TEXT_PASSWORD = 0x81
        const val TEXT_VISIBLE_PASSWORD = 0x91
        const val TEXT_WEB_PASSWORD = 0xE1
        const val NUMBER_PASSWORD = 0x12
        const val TEXT_URI = 0x11
        const val MULTI_LINE = 0x20000
        const val NO_PERSONALIZED_LEARNING = 0x1000000
        const val NO_ENTER_ACTION = 0x40000000
        const val ACTION_UNSPECIFIED = 0
        const val ACTION_NONE = 1
        const val ACTION_SEARCH = 3
    }

    @Test
    fun `FI-01 text password is password and numeric-like`() {
        val f = FieldInfo.from(TEXT_PASSWORD, 0)
        assertTrue(f.isPassword)
        assertTrue(f.numericLike)
    }

    @Test
    fun `FI-02 text visible password is password and numeric-like`() {
        val f = FieldInfo.from(TEXT_VISIBLE_PASSWORD, 0)
        assertTrue(f.isPassword)
        assertTrue(f.numericLike)
    }

    @Test
    fun `FI-03 text web password is password and numeric-like`() {
        val f = FieldInfo.from(TEXT_WEB_PASSWORD, 0)
        assertTrue(f.isPassword)
        assertTrue(f.numericLike)
    }

    @Test
    fun `FI-04 number password is password, NUMBER class, numeric-like`() {
        val f = FieldInfo.from(NUMBER_PASSWORD, 0)
        assertTrue(f.isPassword)
        assertEquals(FieldClass.NUMBER, f.fieldClass)
        assertTrue(f.numericLike)
    }

    @Test
    fun `FI-05 text URI shares variation 0x10 but is not a password`() {
        val f = FieldInfo.from(TEXT_URI, 0)
        assertFalse(f.isPassword)
        assertFalse(f.numericLike)
    }

    @Test
    fun `FI-06 no-learning flag alone records nothing but hides no display, not numeric-like - FR-CMN-08 ②`() {
        val f = FieldInfo.from(TEXT, NO_PERSONALIZED_LEARNING)
        assertTrue(f.noLearning)
        assertTrue(f.noRecord)
        assertFalse(f.hidesDisplay)
        assertFalse(f.numericLike)
    }

    @Test
    fun `FI-07 phone, datetime and number classes are numeric-like, not password`() {
        for ((type, cls) in listOf(PHONE to FieldClass.PHONE, DATETIME to FieldClass.DATETIME, NUMBER to FieldClass.NUMBER)) {
            val f = FieldInfo.from(type, 0)
            assertEquals(cls, f.fieldClass)
            assertTrue("$cls numericLike", f.numericLike)
            assertFalse("$cls isPassword", f.isPassword)
        }
    }

    @Test
    fun `FI-08 text multi-line flag sets multiLine`() {
        assertTrue(FieldInfo.from(TEXT or MULTI_LINE, 0).multiLine)
    }

    @Test
    fun `FI-09 search action, suppressed by NO_ENTER_ACTION`() {
        assertEquals(ACTION_SEARCH, FieldInfo.from(TEXT, ACTION_SEARCH).enterAction)
        assertNull(FieldInfo.from(TEXT, ACTION_SEARCH or NO_ENTER_ACTION).enterAction)
    }

    @Test
    fun `FI-10 NONE and UNSPECIFIED actions give no enter action`() {
        assertNull(FieldInfo.from(TEXT, ACTION_NONE).enterAction)
        assertNull(FieldInfo.from(TEXT, ACTION_UNSPECIFIED).enterAction)
    }

    @Test
    fun `FI-11 plain text and multi-line text are not numeric-like`() {
        assertFalse(FieldInfo.from(TEXT, 0).numericLike)
        assertFalse(FieldInfo.from(TEXT or MULTI_LINE, 0).numericLike)
    }

    @Test
    fun `FI-12 text password with no-learning is password, no-learning, hides display, records nothing, numeric-like`() {
        val f = FieldInfo.from(TEXT_PASSWORD, NO_PERSONALIZED_LEARNING)
        assertTrue(f.isPassword)
        assertTrue(f.noLearning)
        assertTrue(f.noRecord)
        assertTrue(f.hidesDisplay)
        assertTrue(f.numericLike)
    }
}
