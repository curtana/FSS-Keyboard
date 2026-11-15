package kbd.app.view

import kbd.hangul.jamo.Jamo
import kbd.hangul.jamo.JamoTables
import kbd.stroke.layout.TapKeyDef
import kbd.stroke.layout.TapKeyKind

/**
 * The compound final of a press on one Dubeolsik consonant key released over another (LLD-05A §4.1, SRS FR-TAP-02f,
 * owner FSS#109 "더해줘"): the pressed key's jamo then the released key's, both as tapped, as a pair of the
 * composer's own table (`JamoTables.COMPOUND_JONG`, the 11 compound finals) — no table of its own. Null when the
 * release is not on such a pair: no key (side margin, outside the keyboard), the pressed key itself, the reverse
 * order (ㅂ then ㄹ), a vowel, a QWERTY, number, symbol or function key. Pure; the direction and the crossing count
 * are not looked at. Lives in the app because stroke-core does not depend on core-hangul (LLD-03 §1.2).
 */
internal object ReleaseKeyCompound {

    fun of(pressed: TapKeyDef, released: TapKeyDef?): Char? {
        if (released == null || released.id == pressed.id) return null
        if (pressed.kind != TapKeyKind.DUBEOLSIK_LETTER || released.kind != TapKeyKind.DUBEOLSIK_LETTER) return null
        val first = pressed.jamo?.let(Jamo::ofCompat) ?: return null
        val second = released.jamo?.let(Jamo::ofCompat) ?: return null
        return JamoTables.COMPOUND_JONG[first to second]?.compat
    }
}
