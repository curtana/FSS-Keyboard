package kbd.stroke.tap

import kbd.stroke.geom.Dir

/**
 * The mode key's stroke, judged on release (SRS FR-CMN-13, HLD 8.2, issue #205), shared by the tap
 * and graffiti views. Only the first edge crossed counts: up =
 * [OPEN_SETTINGS], whether the finger came back or rubbed; no crossing, ← or → = [MODE_KEY_TAP]
 * (the mode change). Outer edges record nothing, so leaving through them is a tap.
 */
enum class ModeKeyGesture {
    MODE_KEY_TAP,
    OPEN_SETTINGS;

    companion object {
        fun of(g: TapGesture): ModeKeyGesture {
            val dir = when (g) {
                TapGesture.Tap -> null
                is TapGesture.Exit -> g.dir
                is TapGesture.ExitReturn -> g.dir
                is TapGesture.Rub -> g.dir
            }
            return if (dir == Dir.UP) OPEN_SETTINGS else MODE_KEY_TAP
        }
    }
}
