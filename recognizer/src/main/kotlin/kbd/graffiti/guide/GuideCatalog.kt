package kbd.graffiti.guide

import kbd.graffiti.recognize.SetName
import kbd.graffiti.recognize.StrokeToken
import kbd.graffiti.recognize.TemplateSet

/** Sections of the stroke guide, in their fixed order (SRS FR-GRF-09b, GRAFFITI 6.6-4). */
enum class GuideSection { HANGUL, ENGLISH, NUMBER, CONTROL, SYMBOL }

/** One drawn part of a cell (LLD-05 §10.1). A cell draws its marks side by side, in order. */
sealed interface GuideMark {
    /** Stroke [stroke] of template [id] in [set]: the same points the recognizer matches. */
    data class Template(val set: SetName, val id: String, val stroke: Int = 0) : GuideMark

    /** A rule stroke with no template: a straight line, [StrokeToken.Tap] or [StrokeToken.CornerTap]. */
    data class Rule(val token: StrokeToken) : GuideMark
}

/**
 * One cell = one row of the canonical stroke table (① and, where the original has it, ②; LLD-05 §10.1).
 * [label] is the meaning written under the drawing, [alt] is 0 (no alternative), 1 (①) or 2 (②),
 * [note] a second line (area, language, a vowel's input order, "미정"). No [marks] = text only.
 * [interim] = the template is the T28 interim stroke (`TEMPLATES.md` interim), drawn dashed with "임시".
 */
data class GuideCell(
    val label: String,
    val marks: List<GuideMark>,
    val alt: Int = 0,
    val note: String? = null,
    val interim: Boolean = false,
)

/** A titled run of cells; [GuideCatalog.groups] keeps the section order. */
data class GuideGroup(val section: GuideSection, val title: String, val cells: List<GuideCell>)

/** What a mark draws, in 0..1 box coordinates; the first point is the start (●). */
sealed interface GuideShape {
    data class Line(val points: List<Pair<Float, Float>>) : GuideShape
    data object Tap : GuideShape
    data object CornerTap : GuideShape
}

/**
 * The stroke guide's content (SRS FR-GRF-09b, 09c, HLD 5장, LLD-05 §10.1): one list, the same for both
 * languages. Cells point at the built-in templates by id, so the guide draws the strokes the recognizer
 * matches; recognition-only variants (`ko_d2`, `ko_b2`, `ko_bb2` and second strokes of one row) are no cells.
 * Strokes with no template are not recognised and have no drawing (symbols: "미정").
 */
object GuideCatalog {

    private fun ko(id: String, stroke: Int = 0) = GuideMark.Template(SetName.KO_LETTER, id, stroke)
    private fun en(id: String, stroke: Int = 0) = GuideMark.Template(SetName.EN_LETTER, id, stroke)
    private fun num(id: String, stroke: Int = 0) = GuideMark.Template(SetName.NUMBER, id, stroke)
    private fun p(id: String) = GuideMark.Template(SetName.PUNCT, id)
    private fun x(id: String) = GuideMark.Template(SetName.EXTENDED, id)
    private fun rule(t: StrokeToken) = GuideMark.Rule(t)

    private fun cell(label: String, vararg marks: GuideMark, alt: Int = 0, note: String? = null, interim: Boolean = false) =
        GuideCell(label, marks.toList(), alt, note, interim)

    private val down = rule(StrokeToken.LineDown)
    private val up = rule(StrokeToken.LineUp)
    private val right = rule(StrokeToken.LineRight)
    private val left = rule(StrokeToken.LineLeft)
    private val downLeft = rule(StrokeToken.DiagDownLeft)
    private val downRight = rule(StrokeToken.DiagDownRight)
    private val tap = rule(StrokeToken.Tap)
    private val cornerTap = rule(StrokeToken.CornerTap)

    /** "미정": in the symbol table but not recognised yet (GRAFFITI 6.4, SRS TBD-11). */
    const val UNSETTLED = "미정"

    /** GRAFFITI 4.2 vowel table: vowel → input order of the cheonjiin elements. */
    private val VOWELS = listOf(
        "ㅏ" to "ㅣ ㆍ", "ㅐ" to "ㅣ ㆍ ㅣ", "ㅑ" to "ㅣ ㆍ ㆍ", "ㅒ" to "ㅣ ㆍ ㆍ ㅣ",
        "ㅓ" to "ㆍ ㅣ", "ㅔ" to "ㆍ ㅣ ㅣ", "ㅕ" to "ㆍ ㆍ ㅣ", "ㅖ" to "ㆍ ㆍ ㅣ ㅣ", "ㅣ" to "ㅣ",
        "ㅗ" to "ㆍ ㅡ", "ㅛ" to "ㆍ ㆍ ㅡ", "ㅜ" to "ㅡ ㆍ", "ㅠ" to "ㅡ ㆍ ㆍ", "ㅡ" to "ㅡ", "ㅢ" to "ㅡ ㅣ",
        "ㅘ" to "ㅗ + ㅏ", "ㅙ" to "ㅗ + ㅐ", "ㅚ" to "ㅗ + ㅣ", "ㅝ" to "ㅜ + ㅓ", "ㅞ" to "ㅜ + ㅔ", "ㅟ" to "ㅜ + ㅣ",
    )

    val groups: List<GuideGroup> = listOf(
        GuideGroup(
            GuideSection.HANGUL, "한글 · 문자 영역",
            listOf(
                cell("ㄱ", ko("ko_g")), cell("ㄴ", ko("ko_n")), cell("ㄷ", ko("ko_d")), cell("ㄹ", ko("ko_r")),
                cell("ㅁ", ko("ko_m"), alt = 1), cell("ㅁ", ko("ko_m2"), alt = 2),
                cell("ㅂ", ko("ko_b")), cell("ㅅ", ko("ko_s")), cell("ㅇ", ko("ko_ieung")), cell("ㅈ", ko("ko_j")),
                cell("ㅊ", ko("ko_ch")), cell("ㅋ", ko("ko_k")), cell("ㅌ", ko("ko_t")), cell("ㅍ", ko("ko_p")),
                cell("ㅎ", ko("ko_h"), alt = 1), cell("ㅎ", ko("ko_h6"), alt = 2),
                cell("ㄲ", ko("ko_gg")), cell("ㄸ", ko("ko_dd")), cell("ㅃ", ko("ko_bb")), cell("ㅆ", ko("ko_ss")), cell("ㅉ", ko("ko_jj")),
                cell("ㄲ", up, ko("ko_g"), note = "시프트 + 자음"),
                cell("ㅣ", down, note = "모음 요소"), cell("ㅡ", right, note = "모음 요소"), cell("ㆍ", tap, note = "모음 요소"),
            ),
        ),
        GuideGroup(GuideSection.HANGUL, "한글 · 모음 입력 순서", VOWELS.map { (v, order) -> cell(v, note = order) }),
        GuideGroup(
            GuideSection.ENGLISH, "영문 · 문자 영역",
            listOf(
                cell("A", en("en_a")),
                cell("B", en("en_b"), alt = 1, interim = true), cell("B", en("en_b2"), alt = 2),
                cell("C", en("en_c")),
                cell("D", en("en_d"), alt = 1, interim = true), cell("D", en("en_d2"), alt = 2),
                cell("E", en("en_e")),
                cell("F", en("en_f"), alt = 1), cell("F", en("en_f2"), alt = 2),
                cell("G", en("en_g"), alt = 1, interim = true), cell("G", en("en_g2"), alt = 2),
                cell("H", en("en_h"), interim = true),
                cell("I", down), cell("J", en("en_j")), cell("K", en("en_k")), cell("L", en("en_l")),
                cell("M", en("en_m"), alt = 1), cell("M", en("en_m2"), alt = 2),
                cell("N", en("en_n")),
                cell("O", en("en_o", 0), alt = 1, interim = true), cell("O", en("en_o", 1), alt = 2, interim = true),
                cell("P", en("en_p"), alt = 2), cell("Q", en("en_q")), cell("R", en("en_r"), alt = 2),
                cell("S", en("en_s")), cell("T", en("en_t")), cell("U", en("en_u")),
                cell("V", en("en_v"), alt = 1), cell("V", en("en_v2"), alt = 2),
                cell("W", en("en_w")),
                cell("X", en("en_x"), alt = 1, interim = true), cell("X", en("en_x2"), alt = 2),
                cell("Y", en("en_y"), alt = 1), cell("Y", en("en_y2"), alt = 2),
                cell("Z", en("en_z")),
            ),
        ),
        GuideGroup(
            GuideSection.NUMBER, "숫자 영역",
            listOf(
                cell("0", num("num_0", 0), alt = 1, interim = true), cell("0", num("num_0", 1), alt = 2, interim = true),
                cell("1", down), cell("2", num("num_2")), cell("3", num("num_3")), cell("4", num("num_4")),
                cell("5", num("num_5"), alt = 1), cell("5", num("num_5b"), alt = 2),
                cell("6", num("num_6")), cell("7", num("num_7")), cell("8", num("num_8"), alt = 1, interim = true),
                cell("9", num("num_9")),
            ),
        ),
        GuideGroup(
            GuideSection.CONTROL, "제어 획",
            listOf(
                cell("⌫", left), cell("↵", downLeft), cell("시프트", up, note = "대문자 · 쌍자음"),
                cell("한영 전환", cornerTap, alt = 1, note = "왼쪽 위 구석 탭"), cell("한영 전환", downRight, tap, alt = 2),
                cell("구두점 시프트", tap, note = "한글은 숫자 영역"), cell("확장 시프트", downRight),
                cell("스페이스", right, note = "영문"),
                cell("스페이스", ko("ko_space_v"), note = "한글 · 문자 영역"),
                cell("스페이스", right, note = "한글 · 숫자 영역"),
                cell("캡스록", up, up, note = "영문"),
                cell("조합 확정", ko("ko_finish_j"), note = "한글"),
            ),
        ),
        GuideGroup(
            GuideSection.SYMBOL, "기호 · 구두점 (탭 뒤)",
            listOf(
                cell(".", tap), cell(",", downLeft), cell("'", down), cell("-", right),
                cell("(", p("p_lparen")), cell(")", p("p_rparen")), cell("?", p("p_question")), cell("/", p("p_slash")),
                cell("$", p("p_dollar")), cell("!", up), cell("@", p("p_at")), cell("#", p("p_hash")),
                cell("%", p("p_percent")), cell("^", p("p_caret")), cell("&", p("p_amp")), cell("*", p("p_star")),
                cell("<", p("p_lt")), cell(">", p("p_gt")), cell("_", note = UNSETTLED), cell("+", p("p_plus")),
                cell("=", p("p_equals")), cell("|", note = UNSETTLED), cell("\\", downRight),
                cell("{", p("p_lbrace")), cell("}", p("p_rbrace")), cell("[", p("p_lbracket")), cell("]", p("p_rbracket")),
                cell("~", p("p_tilde")), cell("`", note = UNSETTLED), cell(";", note = UNSETTLED), cell(":", note = UNSETTLED),
                cell("\"", p("p_quote")), cell(TAB_LABEL, p("p_tab")),
            ),
        ),
        GuideGroup(
            GuideSection.SYMBOL, "기호 · 확장 (↘ 뒤)",
            listOf(
                cell("™", x("x_trademark")), cell("®", x("x_registered")), cell("©", x("x_copyright")),
                cell("‘", x("x_lsquo")), cell("’", x("x_rsquo")), cell("“", x("x_ldquo")), cell("”", x("x_rdquo")),
                cell("§", x("x_section")), cell("°", x("x_degree"), interim = true), cell("+", x("x_plus")),
                cell("±", right), cell("×", downLeft), cell("÷", x("x_divide")), cell("=", x("x_equals")),
                cell("¢", x("x_cent")), cell("¥", x("x_yen")), cell("£", x("x_pound")), cell("¿", x("x_iquest")),
                cell("¡", up), cell("ß", x("x_sz")), cell("µ", x("x_micro")),
                cell("∫", note = UNSETTLED), cell("Ø", note = UNSETTLED), cell("€", x("x_euro"), interim = true),
            ),
        ),
    )

    /** Label of the tab character cell (`\t` cannot be shown). */
    const val TAB_LABEL = "탭 문자"

    /**
     * What [mark] draws, from [templates] for a template mark, or null when the template or the stroke is
     * missing (an empty set after a load failure, LLD-09 §4): the cell then shows its text only.
     */
    fun shape(mark: GuideMark, templates: TemplateSet): GuideShape? = when (mark) {
        is GuideMark.Template -> templates.sets[mark.set].orEmpty().firstOrNull { it.id == mark.id }
            ?.strokes?.getOrNull(mark.stroke)?.let { GuideShape.Line(it) }
        is GuideMark.Rule -> when (mark.token) {
            StrokeToken.Tap -> GuideShape.Tap
            StrokeToken.CornerTap -> GuideShape.CornerTap
            StrokeToken.LineDown -> GuideShape.Line(listOf(0.5f to 0f, 0.5f to 1f))
            StrokeToken.LineUp -> GuideShape.Line(listOf(0.5f to 1f, 0.5f to 0f))
            StrokeToken.LineRight -> GuideShape.Line(listOf(0f to 0.5f, 1f to 0.5f))
            StrokeToken.LineLeft -> GuideShape.Line(listOf(1f to 0.5f, 0f to 0.5f))
            StrokeToken.DiagDownLeft -> GuideShape.Line(listOf(1f to 0f, 0f to 1f))
            StrokeToken.DiagDownRight -> GuideShape.Line(listOf(0f to 0f, 1f to 1f))
            is StrokeToken.Symbol, StrokeToken.Unknown -> null
        }
    }
}
