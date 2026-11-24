package kbd.suggest.en

import java.util.Locale

/** Case of a cell from the typed letters (LLD-12 §5, SRS FR-SUG-05 ⑤). Pure functions. */
object CaseRule {
    enum class Shape { LOWER, FIRST_UPPER, ALL_UPPER, MIXED }

    /** Shape of [typed]. Looks at a..z / A..Z only (skips '); no letters = LOWER. */
    fun shapeOf(typed: String): Shape {
        var letters = 0
        var uppers = 0
        var firstUpper = false
        for (c in typed) {
            if (!WordChars.isLetter(c)) continue
            val upper = c in 'A'..'Z'
            if (letters == 0) firstUpper = upper
            if (upper) uppers++
            letters++
        }
        return when {
            uppers == 0 -> Shape.LOWER
            uppers == 1 && firstUpper -> Shape.FIRST_UPPER
            letters >= 2 && uppers == letters -> Shape.ALL_UPPER
            else -> Shape.MIXED
        }
    }

    /** [form] in the shape of [typed]; null when the shape is MIXED. ASCII only (dictionary letters are a..z / A..Z). */
    fun apply(typed: String, form: String): String? = when (shapeOf(typed)) {
        Shape.LOWER -> form
        Shape.FIRST_UPPER -> if (form.isEmpty()) form else form.substring(0, 1).uppercase(Locale.ROOT) + form.substring(1)
        Shape.ALL_UPPER -> form.uppercase(Locale.ROOT)
        Shape.MIXED -> null
    }
}
