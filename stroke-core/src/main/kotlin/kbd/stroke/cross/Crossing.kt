package kbd.stroke.cross

import kbd.stroke.geom.Edge

/** Edge-crossing record types (LLD-04 §3.1). */
enum class CrossDir { OUT, IN }

/** One crossing: the edge, its direction, and the sample (time and position) that met the condition. */
data class Crossing(val edge: Edge, val dir: CrossDir, val t: Long, val x: Float, val y: Float)

enum class XZone { LEFT_OUT, IN, RIGHT_OUT }

enum class YZone { TOP_OUT, IN, BOTTOM_OUT }

data class ZoneState(val x: XZone, val y: YZone) {
    val inside get() = x == XZone.IN && y == YZone.IN

    companion object {
        val INSIDE = ZoneState(XZone.IN, YZone.IN)
    }
}
