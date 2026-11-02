package kbd.app.swipe.impl

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log

/**
 * The `FSSStroke` debug stroke log (issue #217): `adb logcat -s FSSStroke`. On only in debuggable builds (the
 * release build type is not debuggable), so it never ships in a release. The swipe keyboard (also the input
 * practice's, FSS#17) writes its lines ([StrokeLogLine]) through [sink]; the view calls [init] with its context.
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
