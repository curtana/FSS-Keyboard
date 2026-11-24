package kbd.suggest

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** LLD-11 §7.12 (라) MOD-05: core-suggest depends on nothing (LLD-12 §2, HLD 5B.2). */
class ModuleBoundaryTest {

    /** The `core-suggest` directory. Gradle runs the tests with the module dir as working dir. */
    private val module: File = File(System.getProperty("user.dir") ?: ".").absoluteFile.let { dir ->
        if (File(dir, "settings.gradle.kts").isFile) File(dir, "core-suggest") else dir
    }

    @Test
    fun `MOD-05 core-suggest has no android, foreign kbd or project imports`() {
        val buildFile = File(module, "build.gradle.kts")
        assertTrue("core-suggest not found from ${System.getProperty("user.dir")}", buildFile.isFile)
        val sources = File(module, "src/main/kotlin").walkTopDown()
            .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
            .toList()
        assertTrue("no sources scanned", sources.isNotEmpty())
        val hits = sources.flatMap { file -> forbiddenLines(file.readText()).map { "${file.relativeTo(module)}:$it" } } +
            projectLines(buildFile.readText()).map { "build.gradle.kts:$it" }
        assertTrue("forbidden dependency:\n" + hits.joinToString("\n"), hits.isEmpty())
    }

    @Test
    fun `MOD-05 scanner detects forbidden lines`() {
        val src = "package kbd.suggest.en\n\nimport android.view.View\nimport kbd.suggest.Candidate\n" +
            "import kbd.app.ime.FieldInfo\nimport kbd.suggestx.Other\n  import kbd.stroke.geom.Edge\nimport java.util.Locale\n"
        assertEquals(listOf(3, 5, 6, 7), forbiddenLines(src))
        assertTrue(forbiddenLines("import kbd.suggest.en.CaseRule\nimport kbd.suggest.*").isEmpty())
        assertEquals(listOf(2), projectLines("dependencies {\n    implementation(project(\":core-hangul\"))\n}"))
        assertTrue(projectLines("dependencies {\n    testImplementation(libs.junit)\n}").isEmpty())
    }

    /** 1-based lines importing android.* or a kbd.* package outside kbd.suggest. */
    private fun forbiddenLines(source: String): List<Int> {
        val android = Regex("""^\s*import\s+android\.""")
        val kbd = Regex("""^\s*import\s+kbd\.""")
        val own = Regex("""^\s*import\s+kbd\.suggest(\.|\s|$)""")
        return source.lines().mapIndexedNotNull { i, line ->
            if (android.containsMatchIn(line) || (kbd.containsMatchIn(line) && !own.containsMatchIn(line))) i + 1 else null
        }
    }

    /** 1-based lines with a `project(` dependency. */
    private fun projectLines(source: String): List<Int> =
        source.lines().mapIndexedNotNull { i, line -> if ("project(" in line) i + 1 else null }
}
