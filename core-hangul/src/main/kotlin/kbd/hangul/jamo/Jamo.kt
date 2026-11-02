package kbd.hangul.jamo

enum class JamoKind { CONSONANT, VOWEL }

/**
 * Hangul jamo (LLD-03 §3.1): 30 consonants (19 initials + 11 compound finals) and 21 vowels,
 * declared in Hangul Compatibility Jamo order (U+3131..U+3163).
 *
 * Indices follow the syllable formula U+AC00 + (cho × 21 + jung) × 28 + jong; -1 means "none".
 * Compound finals (`GS` = ㄳ etc.) have `choIndex = -1` and are never used as events.
 */
enum class Jamo(
    val compat: Char,
    val kind: JamoKind,
    val choIndex: Int,   // initial 0..18, -1 if none
    val jungIndex: Int,  // medial 0..20, -1 if none
    val jongIndex: Int,  // final 1..27, -1 if none
) {
    G('ㄱ', JamoKind.CONSONANT, 0, -1, 1),
    GG('ㄲ', JamoKind.CONSONANT, 1, -1, 2),
    GS('ㄳ', JamoKind.CONSONANT, -1, -1, 3),
    N('ㄴ', JamoKind.CONSONANT, 2, -1, 4),
    NJ('ㄵ', JamoKind.CONSONANT, -1, -1, 5),
    NH('ㄶ', JamoKind.CONSONANT, -1, -1, 6),
    D('ㄷ', JamoKind.CONSONANT, 3, -1, 7),
    DD('ㄸ', JamoKind.CONSONANT, 4, -1, -1),
    R('ㄹ', JamoKind.CONSONANT, 5, -1, 8),
    RG('ㄺ', JamoKind.CONSONANT, -1, -1, 9),
    RM('ㄻ', JamoKind.CONSONANT, -1, -1, 10),
    RB('ㄼ', JamoKind.CONSONANT, -1, -1, 11),
    RS('ㄽ', JamoKind.CONSONANT, -1, -1, 12),
    RT('ㄾ', JamoKind.CONSONANT, -1, -1, 13),
    RP('ㄿ', JamoKind.CONSONANT, -1, -1, 14),
    RH('ㅀ', JamoKind.CONSONANT, -1, -1, 15),
    M('ㅁ', JamoKind.CONSONANT, 6, -1, 16),
    B('ㅂ', JamoKind.CONSONANT, 7, -1, 17),
    BB('ㅃ', JamoKind.CONSONANT, 8, -1, -1),
    BS('ㅄ', JamoKind.CONSONANT, -1, -1, 18),
    S('ㅅ', JamoKind.CONSONANT, 9, -1, 19),
    SS('ㅆ', JamoKind.CONSONANT, 10, -1, 20),
    NG('ㅇ', JamoKind.CONSONANT, 11, -1, 21),
    J('ㅈ', JamoKind.CONSONANT, 12, -1, 22),
    JJ('ㅉ', JamoKind.CONSONANT, 13, -1, -1),
    C('ㅊ', JamoKind.CONSONANT, 14, -1, 23),
    K('ㅋ', JamoKind.CONSONANT, 15, -1, 24),
    T('ㅌ', JamoKind.CONSONANT, 16, -1, 25),
    P('ㅍ', JamoKind.CONSONANT, 17, -1, 26),
    H('ㅎ', JamoKind.CONSONANT, 18, -1, 27),

    A('ㅏ', JamoKind.VOWEL, -1, 0, -1),
    AE('ㅐ', JamoKind.VOWEL, -1, 1, -1),
    YA('ㅑ', JamoKind.VOWEL, -1, 2, -1),
    YAE('ㅒ', JamoKind.VOWEL, -1, 3, -1),
    EO('ㅓ', JamoKind.VOWEL, -1, 4, -1),
    E('ㅔ', JamoKind.VOWEL, -1, 5, -1),
    YEO('ㅕ', JamoKind.VOWEL, -1, 6, -1),
    YE('ㅖ', JamoKind.VOWEL, -1, 7, -1),
    O('ㅗ', JamoKind.VOWEL, -1, 8, -1),
    WA('ㅘ', JamoKind.VOWEL, -1, 9, -1),
    WAE('ㅙ', JamoKind.VOWEL, -1, 10, -1),
    OE('ㅚ', JamoKind.VOWEL, -1, 11, -1),
    YO('ㅛ', JamoKind.VOWEL, -1, 12, -1),
    U('ㅜ', JamoKind.VOWEL, -1, 13, -1),
    WEO('ㅝ', JamoKind.VOWEL, -1, 14, -1),
    WE('ㅞ', JamoKind.VOWEL, -1, 15, -1),
    WI('ㅟ', JamoKind.VOWEL, -1, 16, -1),
    YU('ㅠ', JamoKind.VOWEL, -1, 17, -1),
    EU('ㅡ', JamoKind.VOWEL, -1, 18, -1),
    UI('ㅢ', JamoKind.VOWEL, -1, 19, -1),
    I('ㅣ', JamoKind.VOWEL, -1, 20, -1);

    companion object {
        private val BY_COMPAT: Map<Char, Jamo> = entries.associateBy { it.compat }

        /** Finds the jamo for a compatibility jamo character (LLD-03 §3.5); null if none. */
        fun ofCompat(c: Char): Jamo? = BY_COMPAT[c]
    }
}
