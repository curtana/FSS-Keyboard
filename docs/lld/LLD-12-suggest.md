# LLD-12 단어 후보 (core-suggest, 후보 줄)

- 대응 HLD 절: 5B(5B.1 언어 중립 경계 ~ 5B.6 차수), 7.3(설정·영문 사전 줄), 8장(단어 후보 줄), 9장(단어 후보 줄), 10장 R22~R27, 11장(단어 후보 줄), 12장(조합 구간 없이 기억하는 결정)
- 버전 **0.1** (2026-11-24)
- 0.1(FSS#73, REQUIREMENTS 0.36 UR-A14 · SRS 0.97 3.9 FR-SUG-01~12·FR-SET-11·NFR-05·NFR-14·NFR-15·TBD-40(확정, 오너 결정 2026-11-24 "#145는 권장안대로 진행해")·TBD-41~43 · HLD 0.90 5B): 처음 판. 영문 쿼티의 사전 단어 완성, 고른 뒤 자동 공백(구두점 앞 지움, 직후 ⌫ 되돌리기), 축약형. 정한 세부 D-78~D-89(README 5절). 시험 ID는 [LLD-11-tests.md#7.12](LLD-11-tests.md#712-단어-후보-070-fss73-lld-12) — 이 문서는 함수와 규칙만 두고 기대값은 LLD-11에 둔다(README 2절 "참조").
- 근거 분석(저장소 밖): `_workspace/analysis/en-autocomplete/`의 `code-integration.md`(코드 접점), `dictionary.md`(자료·라이선스·자료구조 측정), `scope.md`(동작 규칙). 이 문서의 파일:줄은 develop f1e8203 기준이다.
- 표기·함수 명세 형식: [README.md#2](README.md#2-공통-표기-규칙). 이 문서에서 "공급자" = `SuggestionSource`, "줄" = 후보 줄 `SuggestionBarView`, "평소 길" = 0.96까지의 키 처리(`InputDispatcher`가 지금 하는 것).

## 1. 범위와 놓는 자리

| 구성 요소 | 모듈·패키지(파일) | 절 | 구현 작업(HLD 5B.6) |
|---|---|---|---|
| 모듈 `core-suggest`(Gradle, 의존) | `core-suggest/` | 2 | T-b |
| 사전 자료 파일과 안내 파일 | `core-suggest/src/main/resources/suggest/` | 3 | T-a |
| `WordIndex`·`WordDictionary`·`EnglishDictionary` | `kbd.suggest.en` | 4 | T-b |
| `CaseRule`, `WordChars` | `kbd.suggest.en` | 5, 7.1 | T-b |
| 공급자 계약(언어 중립) | `kbd.suggest` | 6 | T-b |
| `EnglishWordSource` | `kbd.suggest.en` | 7 | T-b |
| `FieldInfo.autocompleteAllowed` | app `kbd.app.ime` `FieldInfo.kt` | 8.1 | T-c |
| `EditorPort` 확장, `AndroidEditorPort` | app `kbd.app.ime` | 8.2 | T-c |
| `ComposingController` 읽기·`applyEdits`, `EditorEdit` | app `kbd.app.ime` | 8.3 | T-c |
| `OwnEditLedger`(커서 장부) | app `kbd.app.ime` `OwnEditLedger.kt` | 8.4 | T-c |
| `SuggestionBarView`·`BarCell`·`SuggestionAccessHelper` | app `kbd.app.view` | 9 | T-d |
| `KeyboardRootView` 후보 줄 높이·놓기 | app `kbd.app.view` | 10 | T-d |
| 설정 "영문 단어 후보"(FR-SET-11) | app `kbd.app.store`·`kbd.app.settings` | 11 | T-d |
| `SuggestionController` | app `kbd.app.ime` | 12 | T-e |
| `InputDispatcher` 배선 | app `kbd.app.ime` | 13 | T-e |
| `KeyboardService` 배선·사전 따로 읽기 | app `kbd.app.ime` | 14 | T-e |
| 오류·개인정보 | – | 15 | 각 작업 |
| 사전 빌드 도구, `NOTICE` | `tools/en-dict/`, 저장소 루트 `NOTICE` | 16 | T-a |

- **설정이 꺼져 있으면 0.96과 모든 것이 같다**(SRS FR-SUG-01 ①, NFR-14 ③): `SuggestionController.enabled`가 거짓이면 공급자가 없고(사전도 읽지 않음) `key()`는 평소 길만 부르며, 루트에는 후보 줄 자식이 없다(10절). 커서 장부(8.4)는 설정과 상관없이 늘 돌지만 편집기를 읽거나 쓰지 않는 순수 계산이다.
- 1단계에서 만들지 않는 것은 SRS FR-SUG-12 그대로다(다음 단어, 학습, 오타 교정, 그래피티 영문 후보, 한글 후보, 문장 첫 글자 자동 대문자, `onDisplayCompletions`, `dont` 별칭, 대화체 보정, 4칸 이상). 계약이 그것들을 막지 않게 둔 자리는 6.3.

## 2. 모듈 `core-suggest`

| 항목 | 값 |
|---|---|
| Gradle 플러그인 | `org.jetbrains.kotlin.jvm`(`libs.plugins.kotlin.jvm`). 직렬화 플러그인 없음 |
| JVM | `sourceCompatibility`·`targetCompatibility` 17, `jvmTarget` 17(`stroke-core/build.gradle.kts`와 같은 모양) |
| 의존 | 없음(다른 모듈·라이브러리 없음). 시험만 `testImplementation(libs.junit)` |
| 루트 패키지 | `kbd.suggest`(계약), `kbd.suggest.en`(영문) |
| 리소스 | `src/main/resources/suggest/en_words.txt`, `en_words_NOTICE.txt`(3절) — 일반 자바 리소스라 jar에 들고 APK에 합쳐진다(그래피티 템플릿 `recognizer/src/main/resources/graffiti/`와 같은 방식) |
| 시험 리소스 | `src/test/resources/suggest/mini_words.txt`(LLD-11 7.12 머리의 작은 목록) |

- `settings.gradle.kts`: `include(":core-hangul", ":stroke-core", ":recognizer", ":core-suggest", ":app")`.
- 루트 `build.gradle.kts`의 시험 별칭 목록(D-02, `build.gradle.kts:10`)에 `":core-suggest"`를 더한다 — `./gradlew testDebugUnitTest`가 이 모듈 시험도 돈다.
- `app/build.gradle.kts`: `implementation(project(":core-suggest"))`(T-e — 앱에서 이 모듈을 처음 쓰는 작업이 넣는다. T-c·T-d는 이 모듈을 쓰지 않는다).
- 의존 방향(LLD-03 1.1): `app ──► core-suggest`, `core-suggest ──► (없음)`. 글자 분류는 자기 안(`WordChars`)에 두고 `stroke-core` 등을 보지 않는다(HLD 5B.2 "의존"). 시험 `MOD-05`가 `core-suggest/src/main`에 `import android.`·`import kbd.`(자기 `kbd.suggest` 밖)가 없음을 본다.
- 안드로이드 없는 클래스라 `app`의 `SuggestionController`·`OwnEditLedger`·`FieldInfo`도 JVM 단위 시험으로 본다(LLD-03 1.2 끝 문단에 더함).

## 3. 사전 자료 파일 형식 (D-78)

- **위치:** `core-suggest/src/main/resources/suggest/en_words.txt`. 클래스패스 이름 `/suggest/en_words.txt`(`EnglishDictionary.RESOURCE`). 옆에 `en_words_NOTICE.txt`(16.3 — 안내 파일, 앱 안에도 들어감).
- **글자:** UTF-8(BOM 없음), 줄 끝 LF, 마지막 줄 뒤에도 LF.
- **머리 줄:** 파일 맨 앞의 `#`로 시작하는 줄들. 읽기는 `#` 줄을 어디서든 건너뛴다(단어는 `#`로 시작할 수 없다). 빌드 도구가 쓰는 머리(순서 고정, 값 예):

```
#fss-words 1
#name en
#dict 2026.11.1
#tool tools/en-dict/build_en_words.py 1
#source ngram googlebooks-eng-all-1gram-20120701 years=1980-2008 min=2000 sha256-list=<26개 파일 검사합을 이은 문자열의 sha256>
#source scowl 2020.12.07 level=60 sha256=<압축 파일 sha256>
#source ldnoobw en commit=<커밋> sha256=<파일 sha256>
#contractions tools/en-dict/contractions.tsv sha256=<파일 sha256>
#words 50000
#sha256 <단어 줄 부분(머리 뒤 모든 줄, 줄마다 LF 포함)의 sha256>
```

  `#fss-words`의 숫자는 형식 판이다(이 절이 바뀌면 올림). `#dict`는 자료 판(같은 형식에서 자료를 다시 만들면 올림). 날짜·시각·만든 기계 이름은 넣지 않는다(같은 입력 = 같은 파일, 16.2 ⑨).
- **단어 줄:** 한 줄에 한 단어, 앞뒤 공백 없음, **사전 모양 그대로**(소문자 낱말, 대문자로 시작하는 고유명사·`I`, 축약형 `I'm`·`don't`). 줄 순서 = 순위(위가 자주 쓰임).
- **읽기가 받아들이는 줄:** 정규식 `^[A-Za-z]+(?:'[A-Za-z]+)*$`에 맞고 길이 ≤ `WordChars.MAX_WORD`(= 32)인 줄. 끝의 `\r` 하나는 지우고 본다(다른 도구가 CRLF로 바꿔도 읽힘). 그 밖의 줄(빈 줄, 공백 든 줄, 영문 글자 밖의 글자, `'`로 시작·끝나는 줄, 33자 이상)은 **건너뛴다**(HLD 5B.2 "깨진 줄"). 빌드 도구는 그런 줄을 쓰지 않는다(16.2 ⑧).
- **같은 소문자 키:** 소문자로 바꾼 글자가 앞의 줄과 같은 줄은 **건너뛴다**(뒤의 줄을 버림, HLD 5B.2 K 전제). 빌드 도구는 키 하나에 줄 하나만 쓴다.
- **순위 번호:** 받아들인 줄을 0부터 센 번호(건너뛴 줄은 번호를 먹지 않는다). 이 번호가 `WordIndex`의 단어 번호다.
- 까닭: 순위순 목록이 디스크에서 거의 최적이고(분석 dictionary.md 6.1 — 원문 447KB, 압축 211KB) 배열 트라이를 정렬 없이 한 번에 만들 수 있다. 머리를 파일 안에 둔 것은 자료와 판이 떨어지지 않게 하려는 것이다(HLD 5B.5 "파일 머리 또는 옆 파일").

## 4. 사전 읽기와 찾기 (`kbd.suggest.en`, D-79)

```kotlin
/** 접두어 찾기 모양 — 자료구조와 뗀 경계(HLD 5B.2: 직렬화 트라이로 바꿀 수 있게). */
interface WordIndex {
    /** [prefixLower](소문자, 단어 글자만)로 시작하는 단어 번호를 순위순으로 최대 [K]개. 그런 단어가 없으면 null. 빈 접두어는 null. */
    fun top(prefixLower: String): IntArray?
    /** 단어 번호의 사전 모양. */
    fun form(id: Int): String
    val size: Int
    companion object { const val K = 4 }   // 후보 칸 수 3 + 1 (HLD 5B.2, SRS FR-SUG-05 ②)
}

class WordDictionary private constructor(/* 배열들 */) : WordIndex {
    companion object {
        /** 3절의 줄 규칙으로 읽어 배열 트라이를 만든다. 입출력 예외는 호출자에게 간다. */
        fun read(reader: java.io.Reader): WordDictionary
    }
}

object EnglishDictionary {
    const val RESOURCE = "/suggest/en_words.txt"
    /** 리소스를 UTF-8로 읽어 [WordDictionary.read]. 리소스가 없으면 IllegalStateException(메시지에 단어 없음). */
    fun load(): WordDictionary
}
```

#### `WordDictionary.read`
- 자료구조(분석 b2): 노드 배열 넷 — `label: ByteArray`(0~25 = a~z, 26 = `'`), `firstChild: IntArray`, `nextSibling: IntArray`(−1 = 없음), `top: IntArray`(노드마다 K칸, −1 = 빈 칸) — 과 `forms: Array<String>`. 노드 0 = 뿌리. 처음 용량 2^17 노드, 모자라면 두 배로 늘림(배열 복사).
- 처리:
  1. 줄마다 3절 규칙으로 거른다(`\r` 하나 지움 → `#`로 시작하면 건너뜀 → 정규식·길이 → 소문자 키 `w.lowercase(Locale.ROOT)`가 이미 있으면 건너뜀. 키가 있는지는 트라이 끝 노드의 "단어 끝" 표시 `IntArray end`(−1 = 아님)로 안다).
  2. 받아들인 줄의 번호 = `forms.size`. `forms += w`.
  3. 뿌리에서 키 글자마다 자식을 찾고(형제 목록을 앞에서부터 훑음, 최대 27개) 없으면 만들어 **형제 목록 맨 앞**에 붙인다. 지나는 노드마다(뿌리 제외, 끝 노드 포함) `top`의 빈 칸이 있으면 첫 빈 칸에 번호를 넣는다 — 줄이 순위순으로 오므로 K칸은 늘 순위순이고 그 접두어의 위 K개다.
  4. 끝 노드에 `end = 번호`.
- 형제 순서는 찾기 결과에 영향이 없다(결과는 `top`뿐). 다 읽은 뒤 배열을 줄여 두지 않아도 된다(남는 칸은 버려도 되는 크기 — 측정 힙 약 5.3MB, NFR-14 ②).
- 사후 조건: 모든 노드 n에 대해 `top[n]`의 앞쪽 칸은 그 접두어로 시작하는 받아들인 단어의 번호를 작은 것부터 최대 K개 담고, 그 뒤 칸은 −1이다(LLD-11 WD-05의 전수 대조).

#### `WordDictionary.top(prefixLower)`
1. 빈 문자열이면 null.
2. 글자마다 자식을 찾는다(글자가 a~z·`'`가 아니면 null). 못 찾으면 null.
3. 끝 노드의 `top`에서 −1 앞까지를 새 `IntArray`로 돌려준다(호출자가 바꿔도 사전이 안 바뀜).
- 대소문자는 호출자가 소문자로 바꿔 준다(`EnglishWordSource`). 이 함수는 `prefixLower`를 다시 바꾸지 않는다.
- 시간: 접두어 길이 × 형제 훑기(≤ 27). 분석 측정 평균 1µs 미만(NFR-14 ①).

## 5. 대소문자 `CaseRule` (순수 함수, SRS FR-SUG-05 ⑤)

```kotlin
object CaseRule {
    enum class Shape { LOWER, FIRST_UPPER, ALL_UPPER, MIXED }
    /** 친 글자의 모양. 글자(a~z·A~Z)만 보고 `'`는 건너뛴다. 글자가 없으면 LOWER. */
    fun shapeOf(typed: String): Shape
    /** 칸에 보일 모양. MIXED이면 null. */
    fun apply(typed: String, form: String): String?
}
```

| 친 글자의 글자들 | `shapeOf` | `apply(typed, form)` |
|---|---|---|
| 모두 소문자 | LOWER | `form` 그대로(`i` → `I`, `i'm` → `I'm`, `mon` → `Monday`, `mc` → `McDonald`) |
| 첫 글자만 대문자(한 글자 대문자 포함) | FIRST_UPPER | `form`의 첫 글자만 대문자로, 나머지는 `form` 그대로(`Th` → `The`, `I` → `I`, `Mc` → `McDonald`) |
| 두 글자 이상이고 모두 대문자 | ALL_UPPER | `form.uppercase(Locale.ROOT)`(`MON` → `MONDAY`, `I'M` → `I'M`, `DON'` → `DON'T`) |
| 그 밖(`hELlo`, `tHe`) | MIXED | null(그 단어는 후보 없음, TBD-40 ④) |

- 대문자로 바꾸기는 ASCII만이다(사전 글자가 영문 글자뿐, 3절).

## 6. 공급자 계약 (`kbd.suggest`, 언어 중립 — HLD 5B.1, D-80)

```kotlin
package kbd.suggest

/** 후보 칸 하나. [spoken] = 화면 읽기 이름. */
data class Candidate(val text: String, val spoken: String = text, val kind: Kind = Kind.WORD) {
    enum class Kind { WORD, TYPED }   // TYPED = 친 글자 그대로 칸(SRS FR-SUG-05 ④)
}

/** 앱이 공급자에 알리는 키 사건(언어 중립 종류, HLD 5B.1). 닫힌 종류 — 늘릴 때는 이 목록에 더한다. */
sealed interface SuggestKey {
    data class Text(val text: String) : SuggestKey   // 글자·숫자·기호 키가 낸 글 그대로
    data object Space : SuggestKey
    data object Backspace : SuggestKey               // ⌫ 한 번(반복의 한 번 포함)
    data object Enter : SuggestKey
    data object Tab : SuggestKey
    data object Navigation : SuggestKey              // 방향키 넷, Esc, Del
    data object Function : SuggestKey                // 그 밖의 기능(1단계에는 보내는 곳 없음)
}

enum class ResetReason { FIELD, MODE, CHORD, CURSOR, SETTING, FINISHED, CONFIG, INACTIVE }

/** 편집 명령. 1단계는 둘(HLD 5B.1). */
sealed interface EditCommand {
    data class DeleteBefore(val count: Int) : EditCommand   // 커서 앞 count글자
    data class Insert(val text: String) : EditCommand        // 커서 자리에 넣음(확정 글)
}

/** [expectBefore] = 쓰기 전 커서 바로 앞이 이 글이어야 함(빈 글 = 확인 없음). */
data class EditPlan(val expectBefore: String, val commands: List<EditCommand>) {
    companion object { val NONE = EditPlan("", emptyList()) }
}

enum class OnFail { NOTHING, PASS }   // 계획을 쓰지 못했을 때: 아무것도 안 함 / 평소 길

sealed interface KeyAnswer {
    data object Pass : KeyAnswer                                   // 평소 길만
    data class WriteThenPass(val plan: EditPlan) : KeyAnswer       // 계획을 쓰고 같은 묶음 안에서 평소 길
    data class Consume(val plan: EditPlan, val onFail: OnFail) : KeyAnswer   // 계획만, 평소 길을 타지 않음
}

/** 공급자가 부탁하는 글자 엿보기. null = 읽지 못함. */
interface TextPeek {
    fun before(n: Int): String?
    fun after(n: Int): String?
    fun hasSelection(): Boolean?
}

interface SuggestionSource {
    /** 순위순 칸 내용. 길이 제한 없음(줄이 앞에서부터 보일 만큼 보인다). 후보가 없으면 빈 목록. */
    val candidates: List<Candidate>
    /** 평소 길 **전에** 묻는다. 엿보기는 여기서만 한다. */
    fun onKey(key: SuggestKey, peek: TextPeek): KeyAnswer
    /** 평소 길을 **탄 뒤에** 알린다(Pass, WriteThenPass, 그리고 OnFail.PASS로 평소 길을 탄 Consume). */
    fun afterKey(key: SuggestKey)
    /** 칸 [index]를 골랐다. 쓸 계획, 고를 것이 없으면 null(상태 그대로). 계획이 있으면 쓰기에 성공한 것으로 보고 상태를 옮긴다. */
    fun pick(index: Int): EditPlan?
    /** 바로 앞의 onKey·pick이 낸 계획을 쓰지 못했다(전제 어긋남·읽기 실패). */
    fun onWriteFailed()
    fun reset(reason: ResetReason)
}
```

### 6.1 부르는 순서 (앱 쪽 규약, 12.2가 지킨다)

| `onKey`의 답 | 앱이 하는 일 | 그 뒤 공급자 호출 |
|---|---|---|
| `Pass` | 평소 길 | `afterKey(key)` |
| `WriteThenPass(plan)` | 전제 확인 → 맞으면 **한 묶음**(`beginBatchEdit`~`endBatchEdit`) 안에서 계획의 명령 → 평소 길 / 어긋나거나 읽지 못하면 `onWriteFailed()` 뒤 평소 길만(묶음 없이) | `afterKey(key)` |
| `Consume(plan, onFail)` | 전제 확인 → 맞으면 한 묶음으로 명령(명령이 없으면 아무것도 안 씀) / 어긋나면 `onWriteFailed()`, `onFail == PASS`이면 평소 길 | 평소 길을 탔을 때만 `afterKey(key)` |

- 상태를 옮기는 때: `Pass`·`WriteThenPass`는 `afterKey`에서, `Consume`은 `onKey`에서(성공한 것으로 보고). `pick`은 `pick` 안에서. 실패는 `onWriteFailed()`가 되돌린다(영문 공급자는 "단어 없음, 앞 글자 모름"으로). 그래서 앱은 상태를 묻지 않고 이 표만 지키면 된다.
- 되돌리기의 전제가 어긋날 때 "평소 ⌫ 한 글자"(SRS FR-SUG-08 ①)는 **앱이 평소 길로 되돌리는 방식**(`OnFail.PASS`)으로 정했다(HLD 5B.2가 LLD에 맡긴 갈래). 공급자가 답을 다시 내는 방식은 `onKey`를 두 번 부르게 되어 엿보기·상태 규약이 복잡해진다.
- `WriteThenPass`의 평소 길을 묶음 **안**에 두는 것은 SRS FR-SUG-07 ① "지우기와 넣기는 한 묶음"을 지키려는 것이다(8.3 `applyEdits`의 `then`).
- 공급자·줄·배선은 모두 입력기 주 스레드에서 부른다(HLD 5B.1 "스레드"). 공급자는 스레드에 안전하지 않아도 된다.

### 6.2 결정성 (NFR-04)

공급자는 시계·난수·전역 상태를 쓰지 않는다. 같은 `WordIndex`, 같은 호출 열, 같은 엿보기 값에는 같은 `candidates`·답·계획을 낸다(LLD-11 ES-40).

### 6.3 뒤에 붙일 것이 막히지 않게 둔 자리 (만들지 않음)

변환 입력(FSS#144)은 `EditCommand`에 조합 글자 보이기·끝내기 명령을, `SuggestKey`에 필요한 키를 더하고 `Consume`으로 스페이스·엔터·⌫·방향키를 가로챈다. 후보 목록 길이가 계약에 없으므로 넘기는 목록·펼침 격자는 줄 쪽 일이다. 앱이 넘겨 주는 완성 목록(TBD-41)도 `SuggestionSource` 하나로 붙인다. 공급자는 모드마다 하나다(12.1).

## 7. 영문 공급자 `EnglishWordSource` (`kbd.suggest.en`, D-81)

```kotlin
class EnglishWordSource(private val index: WordIndex) : SuggestionSource
```

### 7.1 글자 분류 `WordChars`

```kotlin
object WordChars {
    const val MAX_WORD = 32                 // 단어 길이 상한(SRS FR-SUG-04 ④). 사전의 가장 긴 단어(23자, 분석 5만)보다 크다
    const val PUNCT_JOIN = ".,!?;:)]}'"      // 자동 공백을 지우는 구두점 10개(SRS FR-SUG-07 ①)
    fun isLetter(c: Char): Boolean          // 'a'..'z', 'A'..'Z'
}
```

| 키 사건 | 분류 |
|---|---|
| `Text(t)`, `t`가 한 글자이고 `isLetter` | **글자** |
| `Text("'")` | **아포스트로피** — 지금 단어의 마지막 글자가 글자이면 단어 글자, 아니면(단어 없음, `'` 뒤) 단어 끝(단어 맨 앞의 `'` = 따옴표, SRS FR-SUG-04 ①) |
| 그 밖의 `Text`(숫자, 기호, 윗첨자·아래첨자, 두 글자 이상) | **단어 끝 글자** |
| `Space` | 단어 끝(키보드가 넣은 공백) |
| `Backspace` | ⌫ |
| `Enter`, `Tab`, `Navigation`, `Function` | **초기화 키**(SRS FR-SUG-09 ②) |

- 엿보기 값의 분류: 받은 글자(앞 1·뒤 1)가 `isLetter`일 때만 "단어 글자"다. `'`는 출처(입력창에 원래 있던 글자인지 키보드가 넣은 글자인지)와 상관없이 단어 글자로 보지 않는다 — 한 글자만 읽어서는 단어 안의 `'`인지 알 수 없고, SRS FR-SUG-04 ①이 단어 맨 앞의 `'`를 따옴표로 보므로 키보드가 넣은 `'`(7.3 `None` 행)와 같게 둔다(PM 결정 2026-11-24, 검수 1회차 J2). 빈 글(입력창 맨 앞·끝)도 단어 글자가 아니다. **SRS와의 차이(검수 2회차, 기록):** 앞 글자가 단어 뒤의 `'`인 경우(입력창의 `don'|` 뒤에서 침)는 SRS FR-SUG-04 ③으로는 단어 가운데(후보 없음)이나, 이 규칙으로는 새 단어로 시작해 `T(h)`가 된다(ES-03). 해는 작다 — 고르면 전제 `h`가 맞아 `h`만 바꾸고 앞의 `don'`은 그대로 둔다. 두 글자를 읽어 가리지 않는 것은 "앞뒤 한 글자씩을 한 번 읽어"(같은 항)를 지키려는 것이다.

### 7.2 상태

| 상태 | 값 | 후보 |
|---|---|---|
| `None(prevKnown)` | 단어 없음. `prevKnown` = 커서 바로 앞이 **키보드가 넣은 단어 끝**(스페이스·숫자·기호·자동 공백·구두점)임을 안다 | 없음 |
| `Typing(word)` | 치는 중. `word` = 지금 단어(친 모양 그대로, 1~`MAX_WORD`자, 끝이 `'`일 수 있음) | 7.5 |
| `Blocked` | 이 단어는 후보 없음(단어 가운데에서 침·엿보기 실패·선택 영역·길이 상한 넘음) — 다음 단어 끝까지 | 없음 |
| `JustPicked(picked, typed)` | 방금 고름: `picked` = 넣은 후보(칸에 보인 모양, 공백 빼고), `typed` = 고르기 전의 지금 단어. 커서 바로 앞이 자동 공백 | 없음 |

- 처음 상태와 `reset(any)`·`onWriteFailed()` 뒤 = `None(false)`. 기다리는 결정(아래 "보류")도 버린다.
- **보류:** `None(false)` + 글자의 엿보기 결과는 `onKey`에서 정해 `afterKey`까지 들고 간다(엿보기는 평소 길 **전** 커서 앞뒤라야 하므로).

### 7.3 키 사건 표 (정본 SRS FR-SUG-04~09, HLD 5B.2)

`onKey`의 답이 `Pass`인 행은 상태를 `afterKey`에서 옮긴다. "→ 상태"는 사건 뒤의 상태다.

| 지금 상태 | 사건 | `onKey` 답 | → 상태 |
|---|---|---|---|
| `None(true)` | 글자 c | Pass(엿보기 없음) | `Typing(c)` |
| `None(false)` | 글자 c | Pass. 엿보기: `before(1)`, 값이 있으면 `after(1)`, 값이 있으면 `hasSelection()` — 하나라도 null이면 거기서 멈춤 | null이 있음 → `Blocked` / 선택 영역 참 → `Blocked` / 앞이나 뒤가 단어 글자(7.1) → `Blocked` / 그 밖 → `Typing(c)` |
| `None(_)` | 아포스트로피(단어 끝으로 분류) · 단어 끝 글자 · Space | Pass | `None(true)` |
| `None(_)` | ⌫ | Pass | `None(false)`(앞 단어 안으로 들어가도 다시 읽지 않음, TBD-40 ⑥) |
| 모든 상태 | 초기화 키 | Pass | `None(false)` |
| `Typing(w)` | 글자 c | Pass | `w.length < MAX_WORD`이면 `Typing(w + c)`, 아니면 `Blocked` |
| `Typing(w)` | 아포스트로피, `w`의 끝이 글자 | Pass | `Typing(w + "'")`(같은 길이 규칙) |
| `Typing(w)` | 아포스트로피, `w`의 끝이 `'` · 단어 끝 글자 · Space | Pass | `None(true)` |
| `Typing(w)` | ⌫ | Pass | `w.length > 1`이면 `Typing(w.dropLast(1))`, 아니면 `None(true)`(단어 앞 글자는 단어 끝이었음) |
| `Blocked` | 글자 · 아포스트로피 | Pass | `Blocked`(`Blocked`에서는 `'`도 단어 안으로 본다 — 끝까지 후보 없음 쪽) |
| `Blocked` | ⌫ | Pass | `Blocked`(어디까지 지웠는지 모름 — 다음 단어 끝까지 후보 없음) |
| `Blocked` | 단어 끝 글자 · Space | Pass | `None(true)` |
| `JustPicked(p, t)` | `Text(x)`, `x`가 `PUNCT_JOIN`의 한 글자 | `WriteThenPass(EditPlan(" ", [DeleteBefore(1)]))` | `x == "'"`이면 `Typing(p + "'")`, 아니면 `None(true)` |
| `JustPicked(p, t)` | Space | `Consume(EditPlan.NONE, NOTHING)` | 그대로(무동작 스페이스, SRS FR-SUG-07 ③ — 키 입력 피드백은 뷰가 누를 때 이미 냄, TBD-40 ⑫) |
| `JustPicked(p, t)` | ⌫ | `Consume(EditPlan(p + " ", [DeleteBefore(p.length + 1), Insert(t)]), PASS)` | `Typing(t)`(같은 후보를 다시 냄, SRS FR-SUG-08 ①) |
| `JustPicked(p, t)` | 글자 c | Pass | `Typing(c)`(앞이 자동 공백이라 읽지 않음, FR-SUG-07 ④) |
| `JustPicked(p, t)` | 그 밖의 `Text`(`(`·`"`·숫자·다른 기호·윗첨자) | Pass | `None(true)` |
| 모든 상태 | `onWriteFailed()` | – | `None(false)` |

- 구두점 표의 `'`는 `PUNCT_JOIN`에 들어 있어 `JustPicked`에서는 아포스트로피 분류보다 이 행이 먼저다(TBD-40 "구두점 `'`도 자동 공백을 지움").
- 되돌리기는 한 단계뿐이다: 되돌린 뒤는 `Typing(t)`이라 다음 ⌫는 한 글자다(FR-SUG-08 ②). ⌫ 반복의 첫 번이 되돌리기이면 다음 반복부터 한 글자씩이다(③ — 반복도 같은 `Backspace` 사건).
- 사용자가 스페이스로 친 공백은 `JustPicked`가 아니므로 지우지 않는다(`None(true)` + 구두점 = Pass).
- 무동작 스페이스 뒤에도 상태가 `JustPicked`라 그 뒤의 구두점·⌫는 여전히 "자동 공백 바로 뒤"다(FR-SUG-07 ③).
- 키보드에 선택 영역을 알 길이 엿보기뿐이라, `None(true)`·`JustPicked`에서 사용자가 선택한 뒤 곧바로 친 글자는 선택을 모른 채 `Typing`이 될 수 있다. 그 선택 변화 알림은 커서 장부가 사용자 변화로 보아 초기화하고(8.4), 늦게 와도 고르기는 전제 확인으로 막힌다(입력창을 망가뜨리지 않음, FR-SUG-06 ②).

### 7.4 고르기 `pick(index)`

1. 상태가 `Typing(w)`가 아니거나 `index`가 `candidates` 범위 밖이면 null(상태 그대로).
2. `c = candidates[index]`.
   - `c.kind == TYPED`(친 글자 그대로 칸): 계획 = `EditPlan(w, [Insert(" ")])`(남은 글자 없음 → 자동 공백만, TBD-40 ⑤).
   - `c.text.startsWith(w)`(대소문자까지 같음): 계획 = `EditPlan(w, [Insert(c.text.substring(w.length) + " ")])`.
   - 그 밖(대소문자를 바꿈 — `i` → `I`, `mon` → `Monday`): 계획 = `EditPlan(w, [DeleteBefore(w.length), Insert(c.text + " ")])`.
3. 상태 → `JustPicked(c.text, w)`. 계획을 돌려준다.
- 전제 `w`는 "쓰기 전 커서 앞 글자를 한 번 읽어 지금 단어와 같은지"(FR-SUG-06 ②)다. 읽기는 앱이 한다(8.3).

### 7.5 후보 내기 (`candidates`)

상태가 `Typing(w)`일 때만 계산하고, 그 밖에는 빈 목록이다. 같은 `w`에 대한 결과는 다시 계산하지 않고 들고 있는다(사건마다 `candidates`를 읽어도 찾기는 단어가 바뀔 때 한 번).

1. `shape = CaseRule.shapeOf(w)`. `MIXED`이면 빈 목록.
2. `ids = index.top(w.lowercase(Locale.ROOT))`. null(그 접두어로 시작하는 사전 단어가 없음)이면 `[Candidate(w, kind = TYPED)]`(FR-SUG-05 ④).
3. `ids`를 순서대로 `CaseRule.apply(w, index.form(id))`로 바꾸고, **결과가 `w`와 글자 그대로 같은 것을 뺀 뒤** 앞에서 3개(`EnglishWordSource.MAX_CANDIDATES = 3` — core-suggest 안의 상수. 앱 줄의 칸 수 `SuggestionController.BAR_CELLS`와는 따로 둔다)까지(FR-SUG-05 ②③). 빼고 남은 것이 없으면 빈 목록(사전 단어가 있으므로 친 글자 칸은 없다).
- 사전이 없을 때(읽기 전·실패)는 공급자 자체가 없다(12.5) — 빈 줄, 입력 그대로(FR-SUG-11 ③).
- 길이 상한을 넘는 단어는 `Blocked`라 여기 오지 않는다. 한 글자부터 낸다(TBD-40 ②), 길이로 빼는 규칙 없음(③).
- 다른 칸이 같은 글이 되는 일은 없다: 사전은 소문자 키 하나에 모양 하나이고(3절), `CaseRule.apply`는 키가 다른 두 모양을 같은 글로 만들지 않는다(소문자 키가 다르면 대문자로 바꾼 글도 다르다).

## 8. app 쪽 경계 (T-c)

### 8.1 `FieldInfo.autocompleteAllowed` (SRS FR-SUG-10, HLD 5B.3)

```kotlin
data class FieldInfo(
    val fieldClass: FieldClass, val isPassword: Boolean, val noLearning: Boolean, val multiLine: Boolean, val enterAction: Int?,
    /** 입력창 자신의 표시가 후보를 끔: NO_SUGGESTIONS 플래그, 또는 문자 클래스의 URI·이메일 주소·웹 이메일 주소 변형. */
    val noSuggest: Boolean = false,
) {
    /** SRS FR-SUG-10 표. */
    val autocompleteAllowed: Boolean get() = fieldClass == FieldClass.TEXT && !isPassword && !noSuggest
}
```

- 새 칸 `noSuggest`는 **기본값이 있는 마지막 매개변수**다 — 위치 인자로 `FieldInfo(…5개…)`를 만드는 지금 코드·시험(`InputDispatcher.kt:41` 등)이 그대로 컴파일된다.
- `from(inputType, imeOptions)`(`FieldInfo.kt:56-75`)에 더하는 것: 상수 `TYPE_TEXT_VARIATION_URI = 0x10`, `TYPE_TEXT_VARIATION_EMAIL_ADDRESS = 0x20`, `TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS = 0xD0`, `TYPE_TEXT_FLAG_NO_SUGGESTIONS = 0x80000`(모두 `android.text.InputType`의 공개 값). `noSuggest = cls == TYPE_CLASS_TEXT && (inputType and TYPE_TEXT_FLAG_NO_SUGGESTIONS != 0 || varn in setOf(URI, EMAIL_ADDRESS, WEB_EMAIL_ADDRESS))`. 클래스와 함께 보므로 숫자 비밀번호 변형(0x10)은 URI로 읽히지 않는다(지금 비밀번호 판별과 같은 방식, `FieldInfo.kt:66-68`).

| 입력창(SRS FR-SUG-10 표) | `fieldClass` | `isPassword` | `noSuggest` | `autocompleteAllowed` |
|---|---|---|---|---|
| 문자 비밀번호 변형 3종(0x80·0x90·0xE0) | TEXT | 참 | – | 거짓 |
| 숫자 비밀번호(NUMBER + 0x10) | NUMBER | 참 | 거짓(클래스가 TEXT 아님) | 거짓 |
| 숫자·전화·날짜시간 클래스 | NUMBER·PHONE·DATETIME | 거짓 | 거짓 | 거짓 |
| 문자 + `TYPE_TEXT_FLAG_NO_SUGGESTIONS` | TEXT | 거짓 | 참 | 거짓 |
| 문자 + URI(0x10)·이메일 주소(0x20)·웹 이메일 주소(0xD0) | TEXT | 거짓 | 참 | 거짓 |
| 입력 종류 없음(`TYPE_NULL` = 0) | OTHER | 거짓 | 거짓 | 거짓 |
| 문자 + `TYPE_TEXT_FLAG_AUTO_COMPLETE`(0x10000) | TEXT | 거짓 | 거짓 | **참**(TBD-40 ⑦) |
| 문자 + `IME_FLAG_NO_PERSONALIZED_LEARNING` | TEXT | 거짓 | 거짓 | **참**(학습 금지 ≠ 출력 금지) |
| 그 밖의 문자(일반·여러 줄·짧은·긴 메시지·이메일 제목·웹 편집창·사람 이름·주소·발음 변형 등) | TEXT | 거짓 | 거짓 | 참(TBD-40 ⑪) |

- 입력 연결이 없을 때의 "끔"(FR-SUG-10)은 이 칸이 아니라 읽기가 null을 주는 것으로 물러선다(8.2, 7.3 엿보기, 8.3 전제). **SRS 문구와의 차이(검수 1회차 J3, 기록):** `canReadText`는 포트의 성질이라 연결이 없어도 참이고, 앞 글자를 아는 상태(`None(true)`)에서는 엿보지 않으므로 연결이 없는 동안 후보가 보일 수 있다. 해는 없다 — 연결이 없으면 평소 길의 `commitText`부터 아무 일도 하지 않아 지금 단어가 생기지 않고(키보드가 넣은 글이 없음), 고르기는 전제 읽기가 null이라 쓰지 않으며, 연결이 다시 생기면 `onStartInputView`가 초기화한다.

### 8.2 `EditorPort` 확장 (D-83)

```kotlin
interface EditorPort {
    // … 지금 메서드(EditorPort.kt:13-30) 그대로 …

    /** 편집기 글을 읽을 수 있는 포트인가. 기본 거짓 — 읽기를 구현하지 않은 포트에서는 단어 후보가 켜지지 않는다. */
    val canReadText: Boolean get() = false
    /** 커서 앞 최대 [n]글자. 기본 null(읽지 못함). */
    fun textBeforeCursor(n: Int): String? = null
    /** 커서 뒤 최대 [n]글자. 기본 null. */
    fun textAfterCursor(n: Int): String? = null
    /** 선택 영역이 있는가. 기본 null(모름). */
    fun hasSelection(): Boolean? = null
    /** 커서 앞 [n]글자 지우기. 기본 = `deleteBefore()`를 n번(키 이벤트 — 읽지 못하는 포트는 계획이 쓰이지 않으므로 실제로 불리지 않는다). */
    fun deleteBeforeCursor(n: Int) { repeat(n) { deleteBefore() } }
}
```

- **기존 포트를 고치지 않는다:** 시험용 가짜 포트 7곳(`ComposingControllerTest.kt:15`, `InputDispatcherTest.kt:39`, `InputDispatcherChordTest.kt:35`, `InputDispatcherGraffitiTest.kt:51`, `GraffitiInputHandlerTest.kt:50`, `GraffitiGuideTest.kt:78`, `GraffitiPadViewTest.kt:77`)과 입력 연습의 `PracticeEditor`(`PracticeInput.kt:34`)는 기본 구현을 받아 그대로 컴파일·통과한다(`sendChord`와 같은 방식, `EditorPort.kt:26`).
- `canReadText`를 따로 둔 까닭: 읽기가 null이어도 `None(true)`에서는 엿보지 않고 `Typing`이 되므로, 읽기 null만으로는 후보가 "저절로 꺼지지" 않고 고를 수 없는 후보가 보인다. 그래서 `SuggestionController.active`가 이 값을 본다(12.1, HLD 5B.3 "읽기를 구현하지 않은 포트에서는 저절로 꺼지는 쪽"을 지키는 장치).
- `AndroidEditorPort`(`AndroidEditorPort.kt`): `canReadText = true`. `textBeforeCursor(n) = ic?.getTextBeforeCursor(n, 0)?.toString()`, `textAfterCursor(n) = ic?.getTextAfterCursor(n, 0)?.toString()`, `hasSelection() = ic?.let { c -> c.getSelectedText(0)?.isNotEmpty() ?: false }`(입력 연결이 없으면 null), `deleteBeforeCursor(n) = ic?.deleteSurroundingText(n, 0)`(돌려받는 값은 버림, D-32와 같음). 입력 연결이 없으면 읽기 null, 쓰기 아무 일 없음.
- `getSelectedText`의 null: 플랫폼 문서는 "선택된 글이 없으면 null"이라 적으므로 null을 "선택 없음"으로 읽는다. 연결이 죽어 null이 온 경우는 앞의 `getTextBeforeCursor`가 이미 null이라 엿보기가 거기서 멈춘다(7.3). 실기 확인 R22 ①(DV-45 ①).
- 읽는 때는 단어 시작에서 앞 글자를 모를 때(앞 1·뒤 1·선택)와 계획을 쓰기 직전(전제 길이만큼)뿐이다 — 키마다 읽지 않는다(NFR-14 ①, HLD 12장).

### 8.3 `ComposingController` — 읽기와 계획 쓰기 (D-84)

```kotlin
/** 계획의 편집 명령(app 쪽 모양 — core-suggest 형에 의존하지 않게, T-c와 T-b가 따로 감). */
sealed interface EditorEdit {
    data class DeleteBefore(val count: Int) : EditorEdit
    data class Insert(val text: String) : EditorEdit
}

class ComposingController(private val port: EditorPort) {
    // … 지금 그대로 …
    val canReadText: Boolean get() = port.canReadText
    fun textBefore(n: Int): String? = port.textBeforeCursor(n)
    fun textAfter(n: Int): String? = port.textAfterCursor(n)
    fun hasSelection(): Boolean? = port.hasSelection()

    /**
     * 전제 확인 → 한 묶음으로 [edits] → 같은 묶음 안에서 [then]. 전제가 어긋나거나 읽지 못하면 아무것도 쓰지 않고(then도 부르지 않음) false.
     * [edits]가 비면 묶음 없이 then만 부르고 true.
     */
    fun applyEdits(expectBefore: String, edits: List<EditorEdit>, then: () -> Unit = {}): Boolean

    /** 키보드 쓰기로 설명되는 커서 알림인가(8.4). onUpdateSelection마다 한 번, 판정 뒤에 부른다. */
    fun isOwnCursorChange(newS: Int, newE: Int): Boolean
}
```

#### `applyEdits(expectBefore, edits, then)`
1. `expectBefore`가 비지 않았으면 `got = port.textBeforeCursor(expectBefore.length)`. `got != expectBefore`(null 포함, 대소문자까지 같아야 함 — 앞이 짧아 덜 온 것도 어긋남)이면 **false**(아무 호출 없음).
2. `edits`가 비면 `then()` 뒤 true(빈 계획 = 무동작 스페이스).
3. `batch { … }`(지금의 `beginBatch`~`endBatch`, 예외에도 묶음을 닫음 — 이슈 #89 방식, `ComposingController.kt:194-201`) 안에서 명령마다:
   - `DeleteBefore(n)`: `port.deleteBeforeCursor(n)`, `predictedSel = -1`(지금 `deleteBefore`와 같은 뜻 — 커서를 모름, 기존 판정 `onUpdateSelection`은 바꾸지 않음), 장부 `deletedBefore(n)`.
   - `Insert(t)`: 지금의 `commitText(t)`(포트 `commitText` + `expectCommit` + 장부 `committed`).
   - 그 다음 `then()`(예: 구두점의 평소 길 `finishAll(); commitText(".")`).
4. true.
- 시험: PW-01~PW-08(호출 순서 = 전제 읽기 → `beginBatch` → 지우기 → 넣기 → (평소 길) → `endBatch`).
- 조합 중이 아닐 때만 부른다(쿼티는 조합이 없다). 그래서 `composingShown`을 건드리지 않는다.

### 8.4 커서 장부 `OwnEditLedger` (D-82, SRS FR-SUG-04 ②, HLD 5B.3)

**문제(분석 code-integration 1-1, 코드 읽기):** 지금 판정(`ComposingController.onUpdateSelection`, `:160-188`)은 `deleteBefore`·`arrow`·`editKey`·`chordKey`(`:102-132`)가 `predictedSel = -1`을 만들고, 그 뒤 첫 알림을 `OTHER_MOVED`로 본다. `predictedSel == -1`일 때의 `commitText`는 기대 위치를 쌓지 않아(`:225-233`) ⌫ 바로 뒤에 친 글자의 알림도 `OTHER_MOVED`다. 이것을 "사용자가 커서를 옮김"으로 쓰면 ⌫마다 후보가 사라진다.

**정한 방법:** 한글 조합 판정(FR-CMN-06)이 쓰는 `predictedSel`·`expected`·`SelectionVerdict`는 **그대로 두고**, 후보 쪽 판정만을 위한 장부를 따로 둔다. `ComposingController`의 모든 쓰기가 장부에 알린다(쓰기 길이 이 클래스 하나라 빠짐이 없다 — 그래피티의 쓰기도 같은 길). 지우기·바꾸기까지 예상하는 것은 장부만이고, 기존 판정의 기대값은 바뀌지 않으므로 기존 시험(CC-01~CC-22)이 그대로 통과한다.

```kotlin
internal class OwnEditLedger {
    fun start(selS: Int, selE: Int)     // onStart: 둘 다 ≥ 0이면 앎, 아니면 모름
    fun committed(len: Int)             // 확정 글 len글자(UTF-16 단위)를 넣음
    fun deletedKey()                    // ⌫ 키 이벤트 한 번
    fun deletedBefore(n: Int)           // deleteSurroundingText(n, 0)
    fun unknown()                       // 결과 커서를 예상할 수 없는 쓰기
    fun explains(newS: Int, newE: Int): Boolean
}
```

- 상태: 예상 선택 `(s, e)`(−1 = 모름), 예상 커서 줄 `expected: ArrayDeque<Int>`(오래된 것부터), **결과 기다림** `awaiting`(참 = `unknown()`의 결과 알림을 아직 받지 못함)과 그동안의 내 쓰기 `pending`(쓰기 종류와 값의 목록), `writesSinceUpdate`(마지막 알림 뒤의 내 쓰기 수 — 기다림이 아닌 모름에서만 씀).
- 쓰기(모두 `writesSinceUpdate += 1`). "새 커서 = c"는 늘 `(s, e) = (c, c)`로 두고 **`expected` 끝에 c를 더한다**는 뜻이다(세 쓰기 모두):
  - `committed(len)`: 앎이면 새 커서 = `s + len`(선택이 있었으면 선택을 바꿔 넣음 — 시작은 s). 기다림이면 `pending += committed(len)`. 그 밖의 모름이면 모름.
  - `deletedKey()`: 앎이고 `s != e`이면 새 커서 = s(선택 지움). 앎이고 `s == e ≥ 1`이면 `e − 1`. 앎이고 맨 앞(0)이면 모름(기다림 아님). 기다림이면 `pending += deletedKey`.
  - `deletedBefore(n)`: 앎이고 `s == e ≥ n`이면 `e − n`, 앎인데 그렇지 않으면 모름(기다림 아님). 기다림이면 `pending += deletedBefore(n)`.
  - `unknown()`: 모름, `expected`·`pending` 비움, **`awaiting = true`**.
  - `start(selS, selE)`: 둘 다 ≥ 0이면 앎 `(min, max)`, 아니면 모름(기다림 아님). `expected`·`pending` 비움, `writesSinceUpdate = 0`.
- `ComposingController`가 부르는 곳: `commitText`·`tab`·`enter`의 여러 줄 → `committed`; `deleteBefore` → `deletedKey`; `applyEdits` → `deletedBefore`·`committed`; `show`(조합 쓰기)·`finishComposing`의 임시 글자 빼기·`finishAfterUserMove`·`arrow`·`editKey`·`chordKey`·`enter`의 동작·키 → `unknown`; `onStart(s, e)` → `start(s, e)`.
- `explains(newS, newE)`:
  1. `w = writesSinceUpdate`, `writesSinceUpdate = 0`.
  2. **기다림**이면(PM 결정 2026-11-24, 검수 1회차 중요 3 (나)): 이 알림을 **`unknown()` 쓰기의 결과**로 본다 — `(s, e) = (min, max)(newS, newE)`, `awaiting = false`, 그 뒤 `pending`의 쓰기를 차례로 앎의 규칙으로 다시 적용(그때마다 `expected`에 새 커서를 더함)하고 `pending`을 비운다. **true**. 그래서 방향키 뒤 곧바로 친 글자들도 예상에 들어가고, 그 뒤의 사용자 터치는 6에서 초기화된다 — 모름이 입력으로 이어지지 않는다(SRS FR-SUG-04 ②·09 ② 그대로 — 남는 경우는 아래 한계 문단의 둘이고, 둘 다 입력창은 안전하다 — 전제 확인이 쓰기를 막음).
  3. **그 밖의 모름**(시작 값 없음, 맨 앞 ⌫, 범위 밖 지우기): **늘 true**(모름에서는 초기화하지 않음). 다시 맞추는 것은 `w == 0`일 때만이다 — `(s, e) = (min, max)(newS, newE)`. `w ≥ 1`이면 모름 그대로 둔다(날아가는 내 쓰기의 알림이 아직 남음).
  4. 앎이고 `newS == newE`이고 `expected`에 newE가 있으면: 그것까지 앞에서 지우고 true(늦게 오거나 묶여 온 알림, D-29와 같은 생각).
  5. 앎이고 `(newS, newE) == (s, e)`이면(쓰기 없는 같은 자리 알림): `expected` 비우고 true.
  6. 그 밖: `(s, e) = (min, max)`, `expected` 비우고 **false**(키보드 쓰기로 설명되지 않는 변화 → 초기화).
- **기다림의 한계:** 모름을 만든 쓰기(방향키 등)의 알림이 따로 오지 않고 뒤의 내 글자와 **한 알림으로 묶여** 오면, 그 알림을 방향키의 결과로 보아 예상이 묶인 글자 수만큼 어긋나고 다음 알림이 6이 되어 **초기화**한다(후보가 한 번 사라짐 — 쓰기는 일어나지 않으므로 입력창은 안전). 모름을 만든 쓰기가 **알림을 아예 내지 않으면**(커서가 움직이지 않는 가장자리 방향키, 글을 바꾸지 않는 조합 키 — 플랫폼 동작은 미확인) 기다림이 남아, 이어 친 글자의 알림을 그 쓰기의 결과로 받고 `pending`을 다시 적용해 예상 커서가 앞서간다. 그러면 그 자리와 겹친 사용자 터치 한 번이 4에서 묻힐 수 있다(드묾 — 다음 알림부터 맞음, 고르기 등은 전제 확인으로 막혀 입력창은 안전). 3의 모름에서도 사용자 터치를 한 번 놓칠 수 있으나, 시작 값 없음·맨 앞 ⌫·범위 밖 지우기 뒤에는 공급자가 이미 "단어 없음, 앞 글자 모름"이고 그동안의 고르기·되돌리기·공백 지우기는 전제 확인(8.3)으로 막힌다. 실기 DV-45 ③(방향키 → 단어 입력 → 다른 곳 터치).
- 한계(실기 R22 ③): 앱이 알림을 묶지 않고 중간 위치를 건너뛰어 보내면 3이 맞고, 앱이 자기 글을 바꾸면(자동 서식) 5가 되어 초기화한다(SRS 그대로 — 앱이 글을 바꿈). 이모지처럼 두 단위인 글자 하나를 ⌫ 하면 예상이 1 어긋나 5가 된다 — 그때는 단어가 없거나(이모지는 단어 끝) 초기화되어도 해가 없다.
- `isOwnCursorChange(newS, newE)` = `ledger.explains(newS, newE)`. 서비스가 `onUpdateSelection`에서 기존 판정 처리 **뒤에** 늘 부른다(14절).
- 시험: OL-01~OL-15.

## 9. 후보 줄 `SuggestionBarView` (app `kbd.app.view`, D-86·D-89)

```kotlin
data class BarCell(val text: String, val spoken: String = text)

class SuggestionBarView(context: Context, private val feedback: FeedbackPort) : View(context) {
    /** 칸 [index]를 그 칸 안에서 누르고 뗐다. [revision] = 누를 때 받은 판. */
    var onPick: (index: Int, revision: Int) -> Unit = { _, _ -> }
    /** 받은 칸들(앞에서 3개까지 그림)과 판. **판은 늘 새 값으로 바꾸고**, 칸이 같으면 다시 그리기(와 접근성 나무 다시 만들기)만 건너뛴다. */
    fun show(cells: List<BarCell>, revision: Int)
    internal val cellCount: Int                      // = 3
    internal fun cellAt(x: Float): Int               // 0~2, 밖이면 −1
    internal fun cellBounds(i: Int, out: Rect)
    internal fun click(i: Int)                       // 접근성 동작: 칸이 있으면 onPick(i, 지금 판)
}
```

- **줄은 영어를 모른다:** 받은 글을 그리고 번호만 알린다(HLD 5B.1·5B.4). `BarCell`은 app 쪽 모양이고 `SuggestionController`가 `Candidate`를 바꿔 준다(12.2).
- **칸:** 뷰 폭(= 자판 폭 W, 10절) 을 같은 폭 3칸으로 — 칸 i = `[round(i·w/3), round((i+1)·w/3))`(FR-SUG-03 ①, TBD-40 ⑨). 후보는 왼쪽 칸부터, 모자라면 오른쪽이 빈다.
- **그리기:** 배경을 칠하지 않는다(루트가 자판 판을 키보드 배경색으로 칠함, LLD-08 7.3). 칸 사이 구분선 2개 = 폭 1dp, 높이 = 뷰 높이의 50%, 세로 가운데, 색 `0xFF505050`. 글 = 흰색, 크기 = 뷰 높이 × 0.40(px), 칸 가운데. 칸 폭 − 2 × 8dp에 안 들어가면 크기를 0.70배까지 줄여 맞추고, 그래도 안 들어가면 그 크기에서 끝을 `…`로 자른다(`TextUtils.ellipsize(…, END)`) — FR-SUG-03 ①이 LLD에 맡긴 "줄이거나 자르는 방법". 눌린 칸은 칸 사각형을 `0xFFFFFFFF` 15% 알파로 덮는다.
- **터치(`onTouchEvent`, 늘 true — 받아 소비):** 이 뷰는 첫 손가락만 받는다(둘째부터는 루트가 떨굼, 10절). `ACTION_DOWN`: 칸 = `cellAt(x)`, 그 칸에 글이 있으면 `pressed = 칸`, `pressedRevision = 지금 판`, `feedback.key()`(누를 때, FR-CMN-11a·FR-SUG-03 ②), 다시 그림. 빈 칸·칸 밖이면 아무것도 안 함(피드백 없음). `ACTION_UP`: `pressed`가 있고 뗀 점이 **그 칸 사각형 안**(x는 칸 안, y는 0 ≤ y < 높이)이면 `onPick(pressed, pressedRevision)`. 그 밖은 아무 일 없음. 끝으로 `pressed = null`. `ACTION_CANCEL`: `pressed = null`. 길게 눌러도 반복·옮기기 없음(FR-SUG-03 ④).
- **누름 버리기(FR-SUG-03 ③, TBD-40 ⑩):** `onPick`은 누를 때의 판을 넘긴다. 누르는 동안 다른 손가락의 키 입력이 먼저 처리되면 `SuggestionController`의 판이 올라가 있어 그 고르기를 버린다(12.3). 줄 쪽에서도 `show`로 새 판이 오면 `pressed`를 지우지는 않고(그림만 다시) 뗄 때 판으로 가린다.
- **접근성 `SuggestionAccessHelper : ExploreByTouchHelper(bar)`(FR-SUG-03 ⑤, NFR-07, D-89):** 가상 항목 = 글이 있는 칸마다 하나(id = 칸 번호), 이름 = `BarCell.spoken`, 경계 = 칸 사각형, `className = "android.widget.Button"`, 동작 `ACTION_CLICK` → `click(i)` 뒤 `TYPE_VIEW_CLICKED`. 빈 칸·빈 줄은 항목이 아니다. 칸이 바뀌면(`show`에서 다름) `invalidateRoot()`로 항목을 다시 만든다. **바뀐 것을 소리로 알리지 않는다**(키마다 후보가 바뀌어 말이 쌓이므로 — 사용자가 줄을 더듬어 들음). `TapKeyAccessHelper`와 같은 방식(`dispatchHoverEvent`·`dispatchKeyEvent`를 도우미에 넘김).
- 시험: SB-01~SB-12.

## 10. `KeyboardRootView` — 후보 줄 높이와 놓기 (HLD 5B.4, SRS FR-SUG-01·02)

```kotlin
class KeyboardRootView … {
    /** 후보 줄 자식. null = 없음(설정 끔). 바꾼 뒤 relayout()은 부르는 쪽이 한다. */
    fun setSuggestionBar(bar: SuggestionBarView?)
    internal val suggestionBar: SuggestionBarView?
    /** 후보 줄 높이 H_s(px): 줄이 있으면 px(rowHeightDp), 없으면 0. */
    internal val barHeight: Int
}
```

- **자식 하나로 둔다**(손잡이 띠와 같은 까닭 — HLD 5A "손잡이 띠"): `setSuggestionBar(bar)`는 앞 줄을 빼고 `bar`를 `LayoutParams(MATCH_PARENT, px(rowHeightDp))`로 더한다. `relayout()`(`:256-267`)이 줄의 `layoutParams.height = px(rowHeightDp)`도 고친다(줄 높이 설정을 따라감, TBD-40 ①). `MATCH_PARENT` 폭이라 지금 `onMeasure` 고리(`:452-470`)가 폭 = `span.keysWidth`(W)로 잰다.
- **높이:** `H_s = barHeight`. `WINDOW_ROWS`(`:613`)는 그대로 6이다(기존 시험이 그대로 — HLD 5B.4).
  - 붙은 자판 `onMeasure`(`:442`): `h = H_s + px(WINDOW_ROWS × rowHeightDp) + paddingBottom`. 줄이 없으면 H_s = 0이라 0.96과 같은 식·같은 값.
  - 띄운 자판 `floatPlaceIn`(`:473-477`)의 `plateHeight = px(HANDLE_DP) + H_s + px(WINDOW_ROWS × rowHeightDp)` — 들어가는지·세로 범위 Y를 이 높이로 잰다(FR-SUG-02 ②). 저장된 fx·fy는 그대로 읽는다.
- **놓기 — 붙은 자판 `onLayout`(`:484-513`):** `rowsBottom = 높이 − paddingBottom`, `keysTop = rowsBottom − px(WINDOW_ROWS × rowHeightDp)`(줄이 없으면 0 = 지금 `paddingTop`과 같은 값 — 루트의 위 padding은 늘 0, `:181`). 후보 줄 = `(x, keysTop − H_s, x + W, keysTop)`(x = `keyboardLeft`, 줄이 있으면 위 = 0). **안내표 버튼의 위 = `keysTop`**(`:503-504`의 `paddingTop`을 바꿈 — 키 영역 기준, 분석 code-integration 2-2 ⓐ). 모드 뷰·덮개는 지금 그대로 `rowsBottom`에 붙는다.
- **놓기 — 띄운 자판 `layoutOnPlate`(`:515-527`):** `rowsTop = p.top + px(HANDLE_DP) + H_s`. 손잡이 띠 = `(p.left, p.top, p.right, p.top + px(HANDLE_DP))`, 후보 줄 = `(x, p.top + px(HANDLE_DP), x + W, rowsTop)`(x = `p.left + span.margin`), 안내표 버튼 = 위 `rowsTop`, 그 밖 = `p.bottom` 바닥 정렬(지금 그대로).
- **자판 판·칠하기·터치 영역·인셋:** 붙은 자판의 판은 `KeyboardPlate.rect`가 입력 뷰 높이 전체를 쓰므로(`KeyboardWidth.kt:133-143`) 고칠 것이 없다 — 후보 줄 자리가 판에 들고, 양쪽 여유 칸 자리(u/2씩)는 루트가 칠하는 판의 빈 자리다(FR-SUG-01 ②). 띄운 자판은 `floatPlace.plate`가 새 `plateHeight`로 나온다. `KeyboardInsets.compute`는 그대로다(앱이 후보 줄 높이만큼 더 밀림 — 붙은 자판, FR-SUG-02 ①).
- **둘째 손가락(FR-SUG-03 ③):** 루트의 `dispatchTouchEvent`(`:416-422`)가 `offKeyboard.filter(ev)`를 부르기 **전에** `firstDown = ev.actionMasked == ACTION_DOWN`을 둔다. `onChild(x, y)`(`:425-431`)는 자식이 후보 줄이고 `firstDown`이 거짓이면 그 자식을 "닿은 자식"으로 치지 않는다 → 다른 손가락이 닿아 있는 동안 후보 줄에 내려온 손가락은 `OffKeyboardPointers`가 떨궈 아무 데도 가지 않는다(입력·피드백 없음, 손을 뗄 때까지). `OffKeyboardPointers`는 고치지 않는다. 후보 줄을 첫 손가락으로 누른 뒤 키에 닿은 손가락은 보통의 자판 터치다(키에 닿았으므로 떨구지 않음). **한계(검수 1회차 J4, 근사 유지):** "다른 손가락이 키보드에 닿아 있는 동안"을 "제스처의 첫 손가락이 아닌 손가락"으로 근사한다 — 첫 손가락이 루트의 빈 자리(여유 칸·빈 띠·여백)에 닿아 있는 동안 후보 줄에 내려온 손가락도 떨군다. 그 경우 손가락을 모두 떼고 다시 누르면 된다(입력 손실 없음).
- **띄운 자판 옮기기:** 옮기기 상태 기계(`PlateMover`, LLD-08 7.4)는 떨구기 전의 원래 사건을 보므로 "기다림 + 다른 손가락 내려옴 → 버림"이 후보 줄 위 손가락에도 그대로다. 끄는 동안 `movePlate`가 모든 자식을 옮기므로 후보 줄도 함께 움직이고, 그 제스처는 루트가 가로채 후보 줄도 터치를 받지 않는다.
- **테두리·획:** 모드 뷰를 고치지 않는다 — 입력기 테두리는 자판 사각형의 네 변 그대로이고, 키에서 시작해 후보 줄 위로 나간 획은 모드 뷰가 음수 y로 계속 받는다(FR-SUG-02 ③, LLD-08 7절 "획이 시작된 뒤").
- **모드 전환·덮개:** `setModeView`·`enableGuide`·`openGuide`는 후보 줄을 건드리지 않는다. 덮개는 키 영역 높이(`rows × rowHeightDp`, 바닥 정렬)라 후보 줄을 덮지 않는다.
- **입력 연습:** 연습 화면의 `BoardFrame`(`PracticeActivity.kt:198`)은 `KeyboardRootView`가 아니고 이 설정을 읽지 않으므로 후보 줄이 없다(FR-SUG-01 ⑤). 고치지 않는다.
- 시험: BR-01~BR-16(Robolectric), 기존 KR·PL·FL·KI·OP 시험은 고치지 않고 통과.

## 11. 설정 "영문 단어 후보" (SRS FR-SET-11, D-87)

| 항목 | 값 |
|---|---|
| `Settings` 칸 | `val wordSuggest: Boolean = false`(기본값이 있는 칸 — `Settings.DEFAULT` 식 그대로, `Settings.kt:6-26`) |
| 저장 키 | `word_suggest`(`booleanPreferencesKey`). `SettingsStore.update`가 쓰고(`:47-63`에 한 줄), `decode`가 읽는다(없으면 기본 거짓). 옮겨 오기 없음(새 키) |
| 설정 화면 자리 | "입력 모드" 묶음 바로 뒤의 새 묶음 **"단어 후보"**(`settings_section_suggest`). 묶음 순서 = 입력 모드 → 단어 후보 → 입력기 → 피드백 → 자판 → 입력 연습 → 버전(LLD-07 7절) |
| 줄 | 스위치 한 줄 "영문 단어 후보"(`settings_word_suggest`, 시험 태그 `word_suggest`, 지금 `SwitchRow` 모양). 그 아래 안내 한 줄(`settings_word_suggest_summary`): "영문 쿼티 자판 위에 단어 후보 한 줄을 둡니다. 켜면 키보드가 한 줄 높아집니다." |
| 세로·가로 | 하나(방향별로 두지 않음, FR-SET-11) |

- 키보드 쪽 반영은 14절(설정 흐름 → 다시 놓기·사전 읽기/놓기).
- **끄면 사전을 놓는다**(HLD 5B.3이 LLD에 맡김): 끄면 공급자와 사전을 버리고(가비지 수집에 맡김, 약 5MB), 다시 켜면 다시 읽는다. 켜고 끄기는 드문 일이고 꺼진 사용자의 메모리를 붙잡지 않는 쪽을 골랐다(측정은 기기에서, R23).

## 12. `SuggestionController` (app `kbd.app.ime`, 안드로이드 없음, D-85)

```kotlin
class SuggestionController(
    private val out: ComposingController,
    private val onError: (site: String, e: Exception) -> Unit,
) {
    /** 줄에 보일 칸과 판. 서비스가 줄의 show를 잇는다. */
    var onCandidates: (cells: List<BarCell>, revision: Int) -> Unit = { _, _ -> }
    var enabled: Boolean          // 설정 "영문 단어 후보"
    var fieldAllowed: Boolean     // FieldInfo.autocompleteAllowed
    var mode: InputMode           // 지금 모드
    var chordOn: Boolean          // Ctrl·Alt가 하나라도 켜짐
    val revision: Int
    val active: Boolean
    /** 사전을 다 읽으면 공급자를 붙이고, 설정을 끄면 null. */
    fun attach(source: SuggestionSource?)
    /** 키 하나: 활성이 아니면 [normal]만. 6.1 표대로. */
    fun key(k: SuggestKey, normal: () -> Unit)
    /** 줄 칸을 골랐다. */
    fun pick(index: Int, revision: Int)
    fun reset(reason: ResetReason)
    companion object { const val BAR_CELLS = 3; const val SITE = "Suggest.source" }
}
```

### 12.1 켬 여부

`active = enabled && fieldAllowed && mode == InputMode.QWERTY && !chordOn && source != null && out.canReadText`.

- 영문 쿼티에만 공급자가 있다(그 밖의 모드 = 없음, HLD 5B "공급자 고르기"). 1단계는 공급자 하나라 "모드마다 하나"를 `mode == QWERTY` 조건으로 쓴다.
- `enabled`·`fieldAllowed`·`mode`·`chordOn`의 세터와 `attach`는 값을 넣은 뒤 `active`가 참 → 거짓으로 바뀌었으면 `source?.reset(INACTIVE)`(예외는 12.6) 하고 판을 올려 빈 칸을 알린다. 거짓 → 참은 따로 할 일이 없다(꺼질 때 이미 초기화).
- 활성이 아니면 공급자를 부르지 않고 편집기를 읽지 않는다(FR-SUG-10 "지금 단어를 추적하지 않으며 커서 앞뒤 글자도 읽지 않는다", NFR-14 ③).

### 12.2 `key(k, normal)`

1. `active`가 아니면 `normal()` 하고 끝(판도 올리지 않음).
2. `revision += 1`. `s = source`.
3. `answer = guarded { s.onKey(k, peek) } ?: KeyAnswer.Pass` — `peek`는 `out.textBefore`·`textAfter`·`hasSelection`을 부르는 `TextPeek`.
4. 6.1 표대로: `Pass` → `normal()`, `guarded { s.afterKey(k) }`. `WriteThenPass(p)` → `ok = out.applyEdits(p.expectBefore, edits(p), then = normal)`; `ok`가 거짓이면 `guarded { s.onWriteFailed() }` 뒤 `normal()`; 그다음 `guarded { s.afterKey(k) }`. `Consume(p, f)` → `ok = out.applyEdits(p.expectBefore, edits(p))`; 거짓이면 `guarded { s.onWriteFailed() }`, `f == PASS`이면 `normal()`, `guarded { s.afterKey(k) }`.
5. `publish()`.
- `edits(p)` = `p.commands`를 같은 이름의 `EditorEdit`로 1:1(8.3).
- `normal()`과 `applyEdits`의 포트 호출에서 난 예외는 잡지 않는다 — 0.96처럼 뷰의 `EngineGuard.stroke`까지 간다(키 처리 실패의 길이 바뀌지 않음).
- `publish()` = `cells = if (active) source.candidates.take(BAR_CELLS).map { BarCell(it.text, it.spoken) } else emptyList()`; `onCandidates(cells, revision)`(같은 칸이어도 판은 줄에 늘 넘어가고, 다시 그리기만 줄이 건너뜀, 9절).

### 12.3 `pick(index, revision)`

1. `active`가 아니거나 `revision != this.revision`(누르는 동안 다른 사건이 처리됨)이면 아무것도 안 함.
2. `this.revision += 1`. `plan = guarded { source.pick(index) }`. null이면 `publish()` 하고 끝.
3. `ok = out.applyEdits(plan.expectBefore, edits(plan))`. 거짓이면 `guarded { source.onWriteFailed() }`(FR-SUG-06 ② "아무것도 쓰지 않고 지금 단어를 버림").
4. `publish()`.
- `typed`(FR-CMN-03)는 따로 다루지 않는다(고르기는 그 모드에서 글자를 친 뒤에만 있다, HLD 5B.3).

### 12.4 `reset(reason)` · `attach(source)`

- `reset`: `revision += 1`; 공급자가 있으면 `guarded { source.reset(reason) }`; `publish()`. 활성이 아니어도 부를 수 있다(공급자가 있으면 초기화).
- `attach(s)`: 앞 공급자를 버리고 `source = s`, `revision += 1`, `publish()`. 새 공급자는 처음 상태(`None(false)`)라 따로 초기화하지 않는다.

### 12.5 사전이 없을 때

읽기 전·읽기 실패·설정 끔이면 `source == null` → 활성 아님 → 빈 줄, 입력은 평소대로(FR-SUG-11 ③). 다 읽으면 서비스가 주 스레드에서 `attach(EnglishWordSource(dict))`(14절).

### 12.6 예외 (HLD 9장, LLD-09)

`guarded { … }` = 공급자 호출을 `try`로 감싸고, `Exception`이면 `onError(SITE, e)`(서비스가 `errorLog.record` — 오류 종류와 코드 위치만, 15절), 이어 `source.reset(INACTIVE)`를 한 번 시도(거기서 또 나면 무시), 결과는 null(`onKey`는 `Pass`로 본다). 그래서 공급자 오류는 그 키를 평소 길로만 처리하고(가로채지 않음) 키 입력을 잃지 않는다. `Error`는 잡지 않는다(D-31과 같음).

## 13. `InputDispatcher` 배선 (T-e)

```kotlin
class InputDispatcher(…) {
    /** 단어 후보(12절). null = 없음(입력 연습 — 연습 자판은 후보가 없다, FR-SUG-01 ⑤). */
    var suggest: SuggestionController? = null
}
```

| 자리(`InputDispatcher.kt`) | 바꿈 |
|---|---|
| `onTapResult`의 글 갈래(`:90-93`) | `keyed(SuggestKey.Text(text)) { finishAll(); this.out.commitText(text) }`. 조합 키 갈래(`:74-81`)·자모 갈래(`:84-89`)는 그대로(조합 키가 켜져 있으면 `chordOn`으로 활성이 아님, 두벌식은 `mode`로 활성이 아님) |
| `targetKey`의 BACKSPACE(`:120-123`) | `keyed(Backspace) { backspace() }`(반복도 `onFunctionDown` → 여기, FR-SUG-08 ③) |
| SPACE(`:124-128`) | `keyed(Space) { finishAll(); out.commitText(" ") }` |
| ENTER(`:129-133`) · TAB(`:134-138`) | `keyed(Enter) { … }` · `keyed(Tab) { … }` |
| 방향키 넷(`:139-142`) · ESC · DELETE(`:143-144`) | `keyed(Navigation) { arrow(d) }` · `keyed(Navigation) { editKey(k) }` |
| 조합 키가 실린 `targetKey`(`:113-118`) | 그대로(공급자에 묻지 않음, FR-SUG-09 ①) |
| `setChord`(`:169-173`) | 값을 바꾼 뒤 `suggest?.chordOn = s.isNotEmpty()`(켜지면 12.1이 초기화하고 켜진 동안 활성 아님 — 켜지는 순간 버림, FR-SUG-09 ①) |
| `onSettingsStroke`(`:196-201`) · `onModeKey`(`:208-217`) | `resetParsers(MODE_CHANGED)` 옆에 `suggest?.reset(ResetReason.MODE)` |
| `markInput()` 부르는 자리 | 그대로(무동작 스페이스·되돌리기도 "입력"으로 친다 — 0.96의 스페이스·⌫와 같음) |

- `keyed(k, normal)` = `val s = suggest; if (s == null) normal() else s.key(k, normal)`.
- 그래피티(`GraffitiInputHandler`)의 쓰기는 이 길을 타지 않는다(그 모드에는 공급자가 없다). 커서 장부에는 `ComposingController`를 거쳐 알려진다(8.4).

## 14. `KeyboardService` 배선과 사전 따로 읽기 (T-e)

| 자리(`KeyboardService.kt`) | 바꿈 |
|---|---|
| 필드 | `suggestion: SuggestionController`, `bar: SuggestionBarView?`, `dictGen: Int`, `internal var dictionaryLoader: () -> WordIndex = EnglishDictionary::load`(시험이 작은 사전·실패를 넣는 자리, LLD-11 SV-02·SV-08) |
| `onCreate`(`:77-100`) | `composing` 다음에 `suggestion = SuggestionController(composing) { site, e -> errorLog.record(site, e) }`, `dispatcher.suggest = suggestion`, `suggestion.enabled = settings.wordSuggest`, `suggestion.mode = modes.current`. `settings.wordSuggest`이면 `loadDictionary()` — **`guard` 생성(`:95`) 뒤에** 부른다(`guard`는 `lateinit`이고 `loadDictionary`가 쓴다) |
| `onCreateInputView`(`:108-121`) | 루트를 만든 뒤 `settings.wordSuggest`이면 `root.setSuggestionBar(newBar())`, 그 다음 지금의 `relayout()` |
| `newBar()` | `SuggestionBarView(this, feedback)`; `onPick = { i, r -> guard.stroke(SITE_PICK) { suggestion.pick(i, r) } }`; `bar = 그것`; `suggestion.onCandidates = { c, r -> bar?.show(c, r) }` |
| `onStartInputView`(`:124-140`) | `composing.onStart(…)` 다음에 `suggestion.fieldAllowed = f.autocompleteAllowed`, `suggestion.reset(FIELD)`(입력창 바뀜·다시 뜸, FR-SUG-09 ②) |
| `showModeView`(`:232-261`) | 맨 앞에서 `suggestion.mode = mode`(바뀌면 12.1이 초기화 — 숫자류 입력창 자동 전환·꺼진 모드 전환 포함) |
| `onUpdateSelection`(`:142-149`) | 지금의 `when`(판정 처리) 뒤에 `if (!composing.isOwnCursorChange(newSelStart, newSelEnd)) suggestion.reset(CURSOR)` — 알림마다 한 번 |
| `onFinishInputView`(`:156-164`) · `onFinishInput`(`:166-170`) | `suggestion.reset(FINISHED)` |
| `onConfigurationChanged`(`:188-192`) | `suggestion.reset(CONFIG)` |
| `applySettings`(`:264-280`) | `suggestChanged = s.wordSuggest != settings.wordSuggest`. 바뀌었으면 `suggestion.reset(SETTING)`, `suggestion.enabled = s.wordSuggest`, 켬 → `loadDictionary()` · 루트가 있으면 `root.setSuggestionBar(newBar())`, 끔 → `releaseDictionary()` · 루트가 있으면 `root.setSuggestionBar(null)`, `bar = null`. 루트가 있으면 `relayout()`(지금 다시 놓는 조건에 `suggestChanged`를 더함 — 모드·조합 중 글자는 그대로, FR-SUG-01 ④). 줄 높이가 바뀌면 지금처럼 `relayout()`이 후보 줄 높이도 바꾼다 |
| `applyEnabledModes`(`:286-297`) | 지금 그대로(모드가 바뀌면 `showModeView` → `mode` 세터가 초기화) |

#### `loadDictionary()` · `releaseDictionary()`
```kotlin
private fun loadDictionary() {
    val gen = ++dictGen
    scope.launch {   // Dispatchers.Default — 입력을 막지 않음(FR-SUG-11 ③)
        val dict = guard.run(SITE_DICT) { dictionaryLoader() }   // 실패 = null, 오류 종류만 기록
        handler.post { if (gen == dictGen && settings.wordSuggest) suggestion.attach(dict?.let(::EnglishWordSource)) }
    }
}
private fun releaseDictionary() { dictGen++; suggestion.attach(null) }
```
- 읽는 동안 설정을 껐다 켜면 `dictGen`이 달라 앞 결과를 버린다(뒤의 읽기가 붙음).
- `ErrorLog.record`는 `@Synchronized`라 다른 스레드에서 불러도 된다(`ErrorLog.kt:17`). 읽기 실패 = 공급자 없음 = 빈 줄, 입력기는 멈추지 않는다(NFR-03).
- `SITE_DICT = "EnglishDictionary.load"`, `SITE_PICK = "Suggest.pick"`.
- `onDisplayCompletions`는 고치지 않는다(TBD-41).

## 15. 오류 처리와 개인정보 (NFR-05, HLD 9장)

- 기록하는 것: 사이트 이름(`EnglishDictionary.load`, `Suggest.source`, `Suggest.pick`)·예외 클래스 이름·첫 `kbd.` 코드 위치뿐이다(`ErrorLog` 형식 그대로, 메시지는 쓰지 않음). core-suggest가 던지는 예외 메시지에도 단어·읽은 글자를 넣지 않는다(`EnglishDictionary.load`의 "missing" 같은 고정 글만).
- 비밀번호·학습 금지 입력창에서는 `errorLog.enabled`가 거짓이라 기록 자체가 없다(지금 그대로). 비밀번호 입력창은 `autocompleteAllowed`가 거짓이라 공급자도 돌지 않는다.
- 아무것도 저장하지 않는다: 지금 단어·후보·고른 단어는 공급자 메모리에만 있고 `reset`(입력창 바뀜·입력 끝)에서 버린다. 네트워크 권한이 그대로 없다(CON-2).
- 편집 계획의 전제가 어긋나거나 읽기가 값을 주지 않으면 아무것도 쓰지 않는다(8.3). 공급자 예외는 12.6.

## 16. 사전 빌드 도구와 고지 (`tools/en-dict/`, HLD 5B.5, NFR-15, D-88)

### 16.1 파일

| 파일 | 내용 |
|---|---|
| `tools/en-dict/build_en_words.py` | 도구 하나(파이썬 표준 라이브러리만). 하위 명령 `fetch`·`aggregate`·`build`·`check` |
| `tools/en-dict/sources.json` | 출처 셋의 내려받는 곳·판·sha256(Ngram 26파일 각각, SCOWL 압축 파일, LDNOOBW `en` 파일과 커밋). 이 셋 말고는 읽지 않는다 |
| `tools/en-dict/contractions.tsv` | 축약형 표(16.2 ⑥) |
| `tools/en-dict/restore.txt` | 되살릴 소문자 키 목록(LDNOOBW가 거른 낱말 가운데 되살릴 것). 1단계는 빈 파일(HLD 5B.5 "되살릴 목록을 둘 수 있게만") |
| `tools/en-dict/README.md` | 다시 만드는 법·입력·판·검사합 |
| `tools/en-dict/test_build_en_words.py`, `tools/en-dict/testdata/` | 작은 입력으로 도구를 시험(DT-01~DT-09) |

### 16.2 처리 (같은 입력 = 같은 출력)

1. **`fetch --dl <빈 폴더>`**: `sources.json`의 파일을 **차례로** 내려받고(병렬 금지 — 분석에서 병렬 중복 쓰기로 파일이 깨짐) sha256을 맞춘다. 다르면 멈춘다. 내려받은 자료는 믿지 않는 자료로 다룬다(새 빈 폴더, 읽기만, 파이썬 `-I`). Ngram은 `googlebooks-eng-all-1gram-20120701-<a~z>.gz` 26개(숫자·기호·품사 파일 제외, 약 4.8GB) — 저장소에 넣지 않는다.
2. **`aggregate --dl <폴더> --work <폴더>`**: Ngram 줄 `토큰 \t 해 \t 횟수 \t 책 수`에서 해 1980~2008의 횟수를 토큰(대소문자 구분)마다 더한다. 토큰에 `_`(품사 표지)가 있거나 `^[A-Za-z]+$`가 아니면 버린다. 합 < 2000이면 버린다. 글자 파일마다 `<글자>.tsv`(`토큰\t합`, 토큰 순 정렬)로 쓴다.
3. **SCOWL 2020.12.07**(압축 파일의 `final/`과 `Copyright`만 푼다): 단계 ≤ 60의 `english-words.*`·`american-words.*`에서 `^[a-z]+$`인 낱말 = 소문자 낱말 L. `english-upper.*`·`american-upper.*`·`english-proper-names.*`·`american-proper-names.*`에서 `^[A-Z][A-Za-z]*$` = 대문자 낱말 U. `english-contractions.*` = 축약형 C(소문자로). 파일은 latin-1로 읽는다.
4. **키와 모양:** 집계 토큰 t마다 키 k = `t.lower()`, `total[k] += 합`, `forms[k][t] = 합`. 키마다 모양: `k == "i"` → `I`. k ∈ L → 소문자 k, 단 길이 > 1이고 `forms[k].get(k, 0) / total[k] < 0.2`이고 가장 많이 쓰인 모양(동점은 글자 순 앞)이 U에 있으면 그 모양(분석 dictionary.md 3.1 ③). k ∉ L → `forms[k]` 가운데 U에 있는 것 중 가장 많이 쓰인 모양(동점은 글자 순 앞), 없으면 버림. **키 하나에 모양 하나**(3절의 K 전제).
5. **한 글자:** `a`, `I`만 남긴다.
6. **축약형**(`contractions.tsv`, 열 = `form` · `host` · `permille` · `extra`): 한 줄이 축약형 하나. **표에 넣는 범위(고정):** C의 형태 가운데 꼬리가 `n't`·`'m`·`'re`·`'ve`·`'ll`·`'d`·`'s` 중 하나이고 `host`가 정해지는 것 — `host` = 꼬리를 뗀 앞 낱말, 불규칙 셋만 고정(`can't` → `can`, `won't` → `will`, `shan't` → `shall`) — 과 `extra = 1`의 `'s`형 11개(SCOWL에 없음 — `it's that's he's she's there's what's here's who's where's how's let's`). **`host`가 정해지지 않는 형태(`ain't`, `y'all`, `o'clock`, 꼬리가 둘인 형태 등)는 표에서 뺀다**(검수 1회차 J5). 도구는 `form.lower()`가 C에 있거나 `extra = 1`인 줄만 쓴다. 점수 = `total[host] × permille // 1000`(정수), `host` 키가 없으면 버림. `permille`은 꼬리마다 고정: `n't` 50, `'m` 50, `'re`·`'ve`·`'ll`·`'d` 20, `'s` 10. `host`는 축약 앞 낱말(`don't` → `do`, `can't` → `can`, `won't` → `will`, `I'm` → `i`, `it's` → `it`). **까닭:** 축약형 순위에 같은 조건 공개를 요구하는 자료(CC BY-SA)를 쓰지 않는다(NFR-15, HLD 5B.5 ⑥). 같은 출처의 2-gram 횟수로 세는 방법은 내려받을 양이 수십 GB이고 Ngram이 축약형을 `do n't`처럼 가르는 방식이 판마다 확인되지 않아 고르지 않았다. 이 계수는 어림이다 — 실사용 뒤 다시 본다(R26, TBD-43).
7. **거르기:** LDNOOBW `en`의 줄(소문자, 공백 없는 것)에 든 키는 버린다(축약형 포함). 단 `restore.txt`의 키는 버리지 않는다.
8. **고르기와 쓰기:** (모양, 점수)를 점수 큰 순, 같으면 모양의 글자 순(파이썬 문자열 비교)으로 줄 세워 앞 50,000개. 모든 모양이 3절의 정규식·길이 ≤ 32에 맞고, 소문자 키가 겹치지 않고, `a`·`I`가 들어 있는지 검사(아니면 멈춤). 3절의 머리 + 단어 줄을 `core-suggest/src/main/resources/suggest/en_words.txt`에 쓴다.
9. **결정성:** 날짜·시각·임의 순서(집합·사전 순회 순서)를 출력에 쓰지 않는다. 같은 입력으로 두 번 돌린 파일이 바이트까지 같다(`check`: 다시 만든 파일을 저장소의 파일과 견줌).

### 16.3 고지

- **저장소 루트 `NOTICE`(새 파일):** 첫머리에 이 저장소 이름과 README와 같은 저작권 줄, 이어서 "이 제품에 든 영문 단어 목록(`core-suggest/src/main/resources/suggest/en_words.txt`)은 `tools/en-dict`가 아래 자료로 만든 것" 다음에 ① **Google Books Ngram Viewer**(http://books.google.com/ngrams), 영어 1-gram 20120701판, Creative Commons Attribution 3.0 Unported, 가공 내용 — 1980~2008년 횟수 합산, 대소문자를 소문자 키로 합침, SCOWL·LDNOOBW로 거름, 순위만 남김 ② **SCOWL** 2020.12.07, 단계 60 이하로 철자를 거름 — 압축 파일의 `Copyright` 파일 **전문**(WordNet 고지 포함, 고치지 않고) ③ **LDNOOBW**(List of Dirty, Naughty, Obscene, and Otherwise Bad Words) `en`, CC BY 4.0, 단어를 빼는 데 씀. 고지 문구는 영어로 쓴다(원문 고지가 영어).
- **`core-suggest/src/main/resources/suggest/en_words_NOTICE.txt`:** 같은 ①~③(앱 안에도 들어가는 안내 파일 — NFR-15 "자료 옆 안내 파일"). 앱 안의 고지 화면은 만들지 않는다(HLD 5B.5).
- 걸러진 단어는 후보에만 안 나오고 직접 치는 것은 막지 않는다(FR-SUG-11 ②) — 이 문서의 어디에도 입력을 막는 길이 없다.

## 17. 시퀀스

### 17.1 `mon` → `Monday` 고름 → `.`

```
(None(true), 앞이 공백) m → key(Text m): onKey Pass → normal(commit "m") → afterKey → Typing("m") → publish
o, n → Typing("mon") → candidates [Monday, money, month]
칸 0 누름 → feedback.key() → 뗌 → onPick(0, r) → pick: r 같음 → source.pick(0) = EditPlan("mon", [Delete 3, Insert "Monday "])
   → applyEdits: textBeforeCursor(3) = "mon" → beginBatch, deleteSurroundingText(3, 0), commitText("Monday "), endBatch → JustPicked("Monday", "mon") → 빈 줄
onUpdateSelection(…) → 기존 판정 → isOwnCursorChange: 장부 expected에 있음 → 초기화 안 함
. → key(Text "."): onKey WriteThenPass(EditPlan(" ", [Delete 1]))
   → applyEdits: textBeforeCursor(1) = " " → beginBatch, deleteSurroundingText(1, 0), (평소 길) commitText("."), endBatch → afterKey → None(true)
```

### 17.2 `i` → `I` 고름 → ⌫ 되돌리기

```
(None(false)) i → onKey: before(1) = "", after(1) = "", hasSelection = false → Pass → normal → afterKey → Typing("i") → [in, is, I]
칸 2(I) 고름 → EditPlan("i", [Delete 1, Insert "I "]) → JustPicked("I", "i")
⌫ → onKey Consume(EditPlan("I ", [Delete 2, Insert "i"]), PASS) → Typing("i") (onKey에서 옮김)
   → applyEdits: textBeforeCursor(2) = "I " → 묶음으로 지우기·넣기 → 같은 후보 [in, is, I]
   (전제가 어긋나면 onWriteFailed → None(false) → 평소 ⌫ 한 글자 → afterKey(Backspace) → None(false))
```

## 18. 시험과 구현 차수

- 시험 ID와 기대값: [LLD-11-tests.md#7.12](LLD-11-tests.md#712-단어-후보-070-fss73-lld-12) — WD(사전), CR(`CaseRule`), ES(`EnglishWordSource`), MOD-05(모듈 경계), FI-13~FI-20(입력창 표), OL(커서 장부), PW(계획 쓰기), CP-07~CP-10(`AndroidEditorPort` 읽기·지우기), SB(줄 뷰), BR(루트), SS-16·SE-01·PR-38(설정·연습), SU(`SuggestionController`), SX(`InputDispatcher` 배선), SV(서비스), DT(빌드 도구), DV-45(실기).
- **설정 끔 = 0.96과 같음**은 기존 시험이 고치지 않고 통과하는 것과 BR-01·SV-01로 본다(SRS 6장 T ⑨).
- 구현 차수는 HLD 5B.6 그대로다: 1차수 T-a(16절) · T-b(2~7절) · T-c(8절), 2차수 T-d(9~11절), 3차수 T-e(12~14절). 같은 차수끼리 파일이 겹치지 않게 경계를 이렇게 그었다 — T-b만 `settings.gradle.kts`·루트 `build.gradle.kts`를, T-e만 `app/build.gradle.kts`를 고친다. T-a의 자료 파일은 `src/main/resources/`, T-b의 시험 사전은 `src/test/resources/`라 서로 기다리지 않는다(실제 자료로 하는 WD-13~WD-16은 둘 다 들어온 뒤의 T-e). T-c의 `EditorEdit`는 core-suggest 형을 쓰지 않아 T-b와 따로 간다. T-d의 줄 뷰는 `BarCell`만 알아 T-b·T-c를 기다리지 않는다(파일이 겹치지 않아 1차수로 당겨 돌려도 된다).
