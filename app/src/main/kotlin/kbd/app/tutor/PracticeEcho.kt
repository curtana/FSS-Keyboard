package kbd.app.tutor


/**
 * The tutor's "입력: …" line (FR-TUT-09, LLD-07 §2.3): practice strokes interpreted with the real
 * keyboard's decision at the thresholds the tutor started with. Display only; measurement does not
 * read it. One echo lives for one try of one step: [TutorFlow] makes a new one to clear the line.
 */
interface PracticeEcho {
    /** What the strokes so far type, the composing syllable included. */
    val text: String

    /** One practice stroke on a practice keyboard [widthDp] wide. */
    fun onStroke(raw: RawStroke, widthDp: Float)

    /** The last stroke was ignored with the error vibration (a cheonjiin element that cannot extend, FR-SWP-62). */
    val lastIgnored: Boolean get() = false

    /** The practice keyboard's center consonant and its place C (SWIPE 9.5); null = none composing, a bare vowel. */
    val center: kbd.app.view.CenterMark? get() = null

    /** The practice keyboard's place C once its geometry is known (null = none yet); the 3-1 judge uses it. */
    val consonantPlace: kbd.hangul.stroke.ConsonantBox? get() = null

    /** Vowel strokes of the open assembly (the practice keyboard's trail follows them, SWIPE 9.4). */
    val openStrokes: Int get() = 0

    /** What vowel stroke [s] would show now (the practice guide, SWIPE 9); null = ignored or no strokes here. */
    fun preview(s: kbd.hangul.stroke.VowelStroke): String? = null
}
