package kbd.app.swipe.impl

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log

/**
 * The `FSSStroke` debug stroke log (issue #217): `adb logcat -s FSSStroke`. On only in debuggable builds (the
 * release build type is not debuggable), so it never ships in a release. The swipe keyboard and the tutor's
 * swipe practice keyboard both write their lines ([StrokeLogLine]) through [sink]; whichever has a context
 * first calls [init] (the keyboard view, the practice guide).
 */
internal object StrokeDebugLog {
    const val TAG = "FSSStroke"

    @Volatile
    private var on = false

    fun init(context: Context) {
        on = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    /** The log line writer, null when off (nothing is built then). */
    val sink: ((String) -> Unit)? get() = if (on) ::write else null

    private fun write(line: String) {
        Log.d(TAG, line)
    }
}
