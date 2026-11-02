package kbd.app.ime

/** Editor field class from `InputType.TYPE_MASK_CLASS` (LLD-08 §2). */
enum class FieldClass { TEXT, NUMBER, PHONE, DATETIME, OTHER }

/**
 * What the keyboard needs to know about the focused field (LLD-08 §2, HLD 8).
 * [enterAction] null = no editor action.
 */
data class FieldInfo(
    val fieldClass: FieldClass,
    val isPassword: Boolean,
    val noLearning: Boolean,
    val multiLine: Boolean,
    val enterAction: Int?,
) {
    /**
     * FR-CMN-08 ①: a password field hides what shows the typed text (the big current character, the center
     * consonant, the graffiti glyph flash). A no-learning field shows everything as usual (owner 2026-11-01:
     * "학습 금지이지 암호처럼 출력 금지가 아니잖아!").
     */
    val hidesDisplay: Boolean get() = isPassword

    /** FR-CMN-08 ① and ②, NFR-05: nothing typed here goes to learning, statistics or the error log. */
    val noRecord: Boolean get() = isPassword || noLearning

    /** FR-CMN-04 "numeric-like field". A field with only the no-learning flag is a normal field (FR-CMN-04b). */
    val numericLike: Boolean
        get() = fieldClass == FieldClass.NUMBER || fieldClass == FieldClass.PHONE ||
            fieldClass == FieldClass.DATETIME || isPassword

    companion object {
        // Public values of android.text.InputType and android.view.inputmethod.EditorInfo.
        private const val TYPE_MASK_CLASS = 0x0F
        private const val TYPE_MASK_VARIATION = 0xFF0
        private const val TYPE_CLASS_TEXT = 1
        private const val TYPE_CLASS_NUMBER = 2
        private const val TYPE_CLASS_PHONE = 3
        private const val TYPE_CLASS_DATETIME = 4
        private const val TYPE_TEXT_VARIATION_PASSWORD = 0x80
        private const val TYPE_TEXT_VARIATION_VISIBLE_PASSWORD = 0x90
        private const val TYPE_TEXT_VARIATION_WEB_PASSWORD = 0xE0
        private const val TYPE_NUMBER_VARIATION_PASSWORD = 0x10
        private const val TYPE_TEXT_FLAG_MULTI_LINE = 0x20000
        private const val IME_MASK_ACTION = 0xFF
        private const val IME_ACTION_UNSPECIFIED = 0
        private const val IME_ACTION_NONE = 1
        private const val IME_FLAG_NO_PERSONALIZED_LEARNING = 0x1000000
        private const val IME_FLAG_NO_ENTER_ACTION = 0x40000000

        private val TEXT_PASSWORD_VARIATIONS = setOf(
            TYPE_TEXT_VARIATION_PASSWORD, TYPE_TEXT_VARIATION_VISIBLE_PASSWORD, TYPE_TEXT_VARIATION_WEB_PASSWORD,
        )

        /** HLD 8 field table, as written in LLD-08 §2. */
        fun from(inputType: Int, imeOptions: Int): FieldInfo {
            val cls = inputType and TYPE_MASK_CLASS
            val varn = inputType and TYPE_MASK_VARIATION
            val fieldClass = when (cls) {
                TYPE_CLASS_TEXT -> FieldClass.TEXT
                TYPE_CLASS_NUMBER -> FieldClass.NUMBER
                TYPE_CLASS_PHONE -> FieldClass.PHONE
                TYPE_CLASS_DATETIME -> FieldClass.DATETIME
                else -> FieldClass.OTHER
            }
            // Class and variation together: number password 0x10 has the same value as text URI 0x10.
            val isPassword = (cls == TYPE_CLASS_TEXT && varn in TEXT_PASSWORD_VARIATIONS) ||
                (cls == TYPE_CLASS_NUMBER && varn == TYPE_NUMBER_VARIATION_PASSWORD)
            val noLearning = imeOptions and IME_FLAG_NO_PERSONALIZED_LEARNING != 0
            val multiLine = cls == TYPE_CLASS_TEXT && inputType and TYPE_TEXT_FLAG_MULTI_LINE != 0
            val action = imeOptions and IME_MASK_ACTION
            val noAction = imeOptions and IME_FLAG_NO_ENTER_ACTION != 0 ||
                action == IME_ACTION_UNSPECIFIED || action == IME_ACTION_NONE
            return FieldInfo(fieldClass, isPassword, noLearning, multiLine, if (noAction) null else action)
        }
    }
}
