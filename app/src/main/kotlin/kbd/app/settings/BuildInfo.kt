package kbd.app.settings

import kbd.app.BuildConfig

/**
 * The settings screen's "버전" item (FSS#140, FR-SET-10, LLD-07 §7.1): which build is installed. Pure so it can be
 * tested for both build types without building both.
 */
internal object BuildInfo {

    const val RELEASE = "릴리스"
    const val DEBUG = "개발"
    private const val SEP = " · "

    /**
     * Two lines: `0.1.0-beta (4)`, then `릴리스` for the release build type and `개발 · <hash> · <date>` for any other.
     * An empty hash or date (never in a debug build, see app/build.gradle.kts) is left out instead of leaving a gap.
     */
    fun text(versionName: String, versionCode: Int, buildType: String, gitHash: String, buildDate: String): String {
        val kind = if (buildType == "release") RELEASE else listOf(DEBUG, gitHash, buildDate).filter { it.isNotEmpty() }.joinToString(SEP)
        return "$versionName ($versionCode)\n$kind"
    }

    /** This installed build. */
    fun current(): String =
        text(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, BuildConfig.BUILD_TYPE, BuildConfig.GIT_HASH, BuildConfig.BUILD_DATE)
}
