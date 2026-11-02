package kbd.app.practice

import kbd.graffiti.ink.GraffitiLang

/** The practice keyboard of an item (SWIPE 10.1, HLD 7.1): the real view, the real engine, the fixed thresholds. */
enum class PracticeBoard {
    /** The English QWERTY tap keyboard as it is, edit row and ⌫ included (owner decision A, FSS#17). */
    TAP_QWERTY,

    /** The swipe keyboard with its guides (FR-TUT-11). */
    SWIPE,

    /** The graffiti pad with the stroke guide always open above it (SWIPE 10.1 ③). */
    GRAFFITI,
}

/**
 * One practice item (LLD-07 §2.2). [target] is what the item types. Tap keyboard items ([steps] null) are judged by
 * the text after ⌫ (SWIPE 10.2); the others by [steps], the editor text the real engine shows after each action
 * (touch, or graffiti stroke) of the item's stroke order — the result of that action, temporary ㆍ / ‥ included
 * (SWIPE 3.4). [howTo] is the line shown after ✗ (SWIPE 10.2 문구 규칙); [note] a short addition to the target line;
 * [guideCells] the stroke-guide cell labels highlighted after ✗ and [lang] the pad's language for graffiti items.
 */
data class PracticeItem(
    val target: String,
    val board: PracticeBoard,
    val howTo: String,
    val steps: List<String>? = null,
    val note: String? = null,
    val guideCells: List<String> = emptyList(),
    val lang: GraffitiLang? = null,
) {
    init {
        require(board == PracticeBoard.TAP_QWERTY || !steps.isNullOrEmpty()) { "$target: steps are needed off the tap keyboard" }
        require(steps == null || steps.last() == target) { "$target: the last step is the target" }
    }
}

/** One topic of the topic list (SWIPE 10.0, 10.1): its name, one-line summary, rough time, explanation and items. */
data class PracticeTopic(
    val id: String,
    val title: String,
    val summary: String,
    val time: String,
    val intro: List<String>,
    val items: List<PracticeItem>,
)

/** The four topics and 19 items of SWIPE 0.48 10.1 (the items are the design's recommendation). */
object PracticeTopics {

    private fun tap(target: String, howTo: String) = PracticeItem(target, PracticeBoard.TAP_QWERTY, howTo)

    private fun swipe(target: String, howTo: String, vararg steps: String, note: String? = null) =
        PracticeItem(target, PracticeBoard.SWIPE, howTo, steps.toList(), note)

    private fun graffiti(target: String, lang: GraffitiLang, howTo: String, cells: List<String>, vararg steps: String, note: String? = null) =
        PracticeItem(target, PracticeBoard.GRAFFITI, howTo, steps.toList(), note, cells, lang)

    val UPPER = PracticeTopic(
        "upper", "영문 대문자", "글자 키를 위로 그어 대문자 쓰기", "약 30초",
        listOf(
            "영문 쿼티 자판에서 글자 키를 위로 그으면 대문자예요.",
            "시프트 키와 캡스록은 없어요.",
            "키 윗변 가운데의 작은 글자가 위로 그어 나오는 글자예요.",
            "틀리면 ⌫로 지우고 이어 쓰면 돼요.",
        ),
        listOf(
            tap("A", "A = a 키에서 위로"),
            tap("Hi", "Hi = h 키에서 위로 → i 탭"),
            tap("OK", "OK = o 키에서 위로 → k 키에서 위로"),
        ),
    )

    val SYMBOL = PracticeTopic(
        "symbol", "기호 입력", "숫자·기호 키를 그어 짝 기호 쓰기", "약 35초",
        listOf(
            "숫자 키와 기호 키를 위로 그으면 짝 기호예요(물리 키보드의 시프트 글자).",
            "키 네 변 가운데의 작은 글자는 그쪽(↑ ← → ↓)으로 그으면 나와요.",
            "앞의 셋은 영문 쿼티 자판, 뒤의 셋은 긋기 자판에서 해요.",
        ),
        listOf(
            tap("!", "! = 1 키에서 위로"),
            tap("?", "? = / 키에서 위로"),
            tap("[", "[ = i 키에서 왼쪽으로"),
            swipe("@", "@ = 2 키에서 위로", "@", note = "긋기 자판"),
            swipe("(", "( = . 키에서 왼쪽으로", "(", note = "긋기 자판"),
            swipe("?", "? = / 키에서 위로", "?", note = "긋기 자판"),
        ),
    )

    val GRAFFITI = PracticeTopic(
        "graffiti", "그래피티", "한 획에 글자 하나, 안내표 보며 쓰기", "약 45초",
        listOf(
            "획 하나에 글자 하나예요.",
            "한글 모음은 ㅣ, ㅡ, ㆍ(탭)를 이어 써요.",
            "숫자는 오른쪽 숫자 영역에 써요.",
            "모르는 글자는 위의 안내표에서 찾아요. 점(●)이 획의 시작이에요.",
        ),
        listOf(
            graffiti("가", GraffitiLang.KO, "가 = ㄱ → ㅣ → 탭", listOf("ㄱ", "ㅏ"), "ㄱ", "기", "가"),
            graffiti("고", GraffitiLang.KO, "고 = ㄱ → 탭 → ㅡ", listOf("ㄱ", "ㅗ"), "ㄱ", "ㄱㆍ", "고"),
            graffiti("a", GraffitiLang.EN, "a = 안내표의 A 칸처럼", listOf("A"), "a", note = "영문"),
            graffiti("1", GraffitiLang.EN, "1 = 안내표의 1 칸처럼, 숫자 영역에", listOf("1"), "1", note = "숫자 영역"),
        ),
    )

    val SWIPE = PracticeTopic(
        "swipe", "긋기", "자음은 탭, 모음은 획으로", "약 1분",
        listOf(
            "자음은 탭이에요.",
            "모음은 위쪽 가운데에 흐리게 보이는 자음 옆에 필순대로 한 획씩 그어요(ㅣ는 오른쪽, ㅡ와 점은 아래).",
            "길이는 상관없고 아래로, 오른쪽으로만 그어요. 떨어져도 돼요.",
            "모음으로 시작하는 글자는 ㅇ 탭 뒤에 그어요. 쌍자음은 위 줄의 전용 키예요.",
            "복모음은 쓰는 그대로 이어 붙여요. 천지인 키로도 모음을 넣을 수 있어요.",
        ),
        listOf(
            swipe("가", "가 = ㄱ 탭 → 자음 오른쪽에서 아래로 → ㅣ 오른쪽에 오른쪽으로", "ㄱ", "기", "가"),
            swipe("고", "고 = ㄱ 탭 → 자음 아래에 아래로 → 점 아래에 오른쪽으로", "ㄱ", "ㄱㆍ", "고"),
            swipe("아", "아 = ㅇ 탭 → 자음 오른쪽에서 아래로 → ㅣ 오른쪽에 오른쪽으로", "ㅇ", "이", "아"),
            swipe(
                "과", "과 = ㄱ 탭 → 자음 아래에 아래로 → 점 아래에 오른쪽으로 → ㅡ 오른쪽 끝에서 아래로 → ㅣ 오른쪽에 오른쪽으로",
                "ㄱ", "ㄱㆍ", "고", "괴", "과",
            ),
            swipe("의", "의 = ㅇ 탭 → 자음 아래에 오른쪽으로 → ㅡ 오른쪽 끝에서 ㅡ가 아래쪽에 오게 아래로", "ㅇ", "으", "의"),
            swipe("가", "가 = ㄱ 탭 → 천지인 키에서 위로 긋기 → 천지인 키 탭", "ㄱ", "기", "가", note = "천지인 키"),
        ),
    )

    /** In the order of SWIPE 10.1; any can be done first. */
    val ALL: List<PracticeTopic> = listOf(UPPER, SYMBOL, GRAFFITI, SWIPE)

    /** The extra line after a reversed swipe stroke (SWIPE 10.2 덧붙임, 3.2). */
    const val REVERSE_HINT = "획은 아래로, 오른쪽으로만 그어요"
}
