package kbd.app.settings

import kbd.app.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** FSS#140, LLD-11 BI-01~BI-04: the settings screen's version text. */
class BuildInfoTest {

    @Test fun bi01_release_shows_version_and_release_only() {
        assertEquals("0.1.0-beta (4)\n릴리스", BuildInfo.text("0.1.0-beta", 4, "release", "", ""))
    }

    @Test fun bi01b_release_ignores_hash_and_date_even_if_given() {
        assertEquals("0.1.0-beta (4)\n릴리스", BuildInfo.text("0.1.0-beta", 4, "release", "47a8398", "2026-11-18"))
    }

    @Test fun bi02_debug_shows_hash_and_date() {
        assertEquals("0.1.0-beta (4)\n개발 · 47a8398 · 2026-11-18", BuildInfo.text("0.1.0-beta", 4, "debug", "47a8398", "2026-11-18"))
    }

    @Test fun bi03_debug_without_git_shows_unknown() {
        assertEquals("0.1.0-beta (4)\n개발 · unknown · 2026-11-18", BuildInfo.text("0.1.0-beta", 4, "debug", "unknown", "2026-11-18"))
    }

    @Test fun bi03b_empty_fields_leave_no_gap() {
        assertEquals("0.1.0-beta (4)\n개발", BuildInfo.text("0.1.0-beta", 4, "debug", "", ""))
    }

    /** The unit tests run against the debug variant: its fields are filled by app/build.gradle.kts. */
    @Test fun bi04_current_debug_build_has_hash_and_date() {
        val text = BuildInfo.current()
        assertEquals("debug", BuildConfig.BUILD_TYPE)
        assertTrue(text, text.startsWith("${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n개발 · "))
        assertTrue(BuildConfig.GIT_HASH, BuildConfig.GIT_HASH == "unknown" || Regex("[0-9a-f]{7,}").matches(BuildConfig.GIT_HASH))
        assertTrue(BuildConfig.BUILD_DATE, Regex("\\d{4}-\\d{2}-\\d{2}").matches(BuildConfig.BUILD_DATE))
    }
}
