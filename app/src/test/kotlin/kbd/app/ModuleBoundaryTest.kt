package kbd.app

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Source-scan guards for the module boundaries (LLD-03 §1.1, §2.1; LLD-11 §7.5).
 *
 * MOD-01 is the build itself (`./gradlew assembleDebug`, one edition since issue #251).
 */
class ModuleBoundaryTest {

    /** Repository root. Gradle runs app unit tests with the `app` project dir as working dir. */
    private val root: File = File(System.getProperty("user.dir") ?: ".").absoluteFile.let { dir ->
        if (File(dir, "settings.gradle.kts").isFile) dir else dir.parentFile
    }

    @Test
    fun `MOD-03 recognizer does not import kbd_hangul_compose`() {
        assertNoImport(listOf("recognizer/src/main"), "kbd.hangul.compose")
    }

    @Test
    fun `MOD-03 scanner detects a forbidden import`() {
        val src = "package x\n\nimport kbd.hangul.compose.HangulComposer\nimport kbd.hangul.composex.Other\n"
        assertEquals(listOf(3), forbiddenLines(src, "kbd.hangul.compose"))
        assertEquals(listOf(1), forbiddenLines("import kbd.hangul.compose.*", "kbd.hangul.compose"))
        assertTrue(forbiddenLines("import kbd.hangul.jamo.Jamo", "kbd.hangul.compose").isEmpty())
    }

    private fun assertNoImport(dirs: List<String>, pkg: String) {
        assertTrue("repository root not found from ${System.getProperty("user.dir")}", File(root, "settings.gradle.kts").isFile)
        val hits = dirs.flatMap { dir ->
            File(root, dir).walkTopDown()
                .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
                .flatMap { file -> forbiddenLines(file.readText(), pkg).map { "${file.relativeTo(root)}:$it" } }
                .toList()
        }
        assertTrue("forbidden import of $pkg:\n" + hits.joinToString("\n"), hits.isEmpty())
    }

    /** 1-based line numbers that import [pkg] or one of its subpackages. */
    private fun forbiddenLines(source: String, pkg: String): List<Int> {
        val pattern = Regex("""^\s*import\s+${Regex.escape(pkg)}(\.|\s|$)""")
        return source.lines().mapIndexedNotNull { i, line -> if (pattern.containsMatchIn(line)) i + 1 else null }
    }
}
