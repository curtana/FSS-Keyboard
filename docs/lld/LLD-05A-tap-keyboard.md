# LLD-05A 탭 자판 (쿼티·두벌식)

- 대응 HLD 절: 5A장(탭 자판)
- 버전 **0.19** (2026-11-14)
- 0.19(FSS#95, 긋기 입력 모드 삭제): 긋기 자판 참조를 지웠다(2.3 윗첨자·아래첨자 표, 6절 반복 타이머). 변 통과 추적·키 찾기 명세는 `LLD-04-stroke-hangul.md` 2·3절(절 번호 그대로). 없는 아래첨자 목록의 근거는 SRS FR-TAP-01b, 문지르기·나갔다 돌아옴 정규화의 근거는 SRS 3.3 탭 자판 동작 표다. 탭 자판의 동작·배치·판정은 그대로다.
- 0.18(FSS#97 오너 지시 2026-11-14, SRS 0.85 FR-TAP-02c·두벌식 겹받침 배정표, HLD 0.78 5A): 두벌식 겹받침 여섯 개의 방향을 옮겼다 — ㄱ(r) ↓ ㄳ, ㄴ(s) ↑ ㄵ · ↓ ㄶ, ㅂ(q) ↓ ㅄ, ㅅ(t) ↓ ㄽ, ㅍ(v) ↑ ㄿ(0.17까지 ㄱ ← ㄳ, ㄴ ← ㄵ · → ㄶ, ㅂ → ㅄ, ㅅ ← ㄽ, ㅍ ← ㄿ). 비게 된 방향(ㄱ ←, ㄴ ← · →, ㅂ ← · →, ㅅ ←, ㅍ ←)은 기본 글자다. ㄹ(f)·ㅌ(x)·↑ 쌍자음·쿼티는 그대로. 바뀐 것은 `TapLayouts.COMPOUND_JAMO` 자료뿐이고 `TapKeyResolver`의 찾는 순서는 0.14 그대로다. 3.3에 두벌식 겹받침 배정표를 두었고, 2.2 표의 `DUBEOLSIK_LETTER` 행에 0.14부터 빠져 있던 겹받침 단계를 적었다(↑·↓ 문지르기는 기본 글자). 3.1 타입에 `compoundJamo`, 3.4에 겹받침 표시. 아래 0.14·0.15 줄의 방향은 그때의 것이다.
- 0.17(FSS#84, SRS 0.82 FR-CMN-14 ⑧⑪, HLD 0.75 5A): `TapGeometry.build(layout, widthDp, rowHeightDp, sideDp)`, `TapKeyboardView.sideMarginPx` — 테두리의 왼쪽·오른쪽이 자판에서 여유 칸만큼 밖이라 끝 키(Esc·1·q·한/영, ⌫·0·p·↵·`/`)의 바깥쪽 변은 보통 변이다(3.1). 배정표는 그대로다(새 배정 없음 — 그 방향은 여전히 탭과 같은 결과).
- 0.16(FSS#62 오너 요청): m 오른쪽 키의 탭과 ←를 맞바꿨다 — 탭 `=`, ← `-`(3.2·3.3). ↑ `_`, ↓ `+`는 그대로. 키 id `minus`는 그대로 둔다.
- 0.15(이슈 #246 오너 결정): ㅄ을 ㅂ(q) ←에서 **ㅂ →**로 옮겼다 — ㅂ은 창 왼쪽 끝 키라 실기기에서 ←가 잘 안 된다. ㅂ ←는 기본 ㅂ(배정 없음).
- 0.14(이슈 #242 오너 결정(두벌식 겹받침 한 번에)): 두벌식 글자 키의 겹받침 11개(3.3): ㄱ(r) ← ㄳ, ㄴ(s) ← ㄵ → ㄶ, ㄹ(f) ↑ ㅀ ← ㄺ → ㄻ ↓ ㄼ, ㅂ(q) ← ㅄ(0.15: → ㅄ로 옮김), ㅅ(t) ← ㄽ, ㅌ(x) ← ㄾ, ㅍ(v) ← ㄿ(`TapLayouts.COMPOUND_JAMO` → `TapKeyDef.compoundJamo`). `TapKeyResolver` 두벌식: ↑ = 시프트 자모 ?: 겹받침 ?: 기본(ㄱ·ㅂ·ㅅ ↑는 ㄲ·ㅃ·ㅆ 그대로), ←→↓ = 겹받침 ?: 배정 기호 ?: 기본. 출력은 자모(`TapOutput.jamo`)라 조합기로 간다(LLD-04 7절). 쿼티는 그대로. 변 가운데 표시(`edgeHints`)에 나온다. ㄱ(r) ←는 창 테두리가 아니라 보통 변 통과다(0.15: 0.14의 "ㅂ ←·ㄱ ←는 테두리 판정"은 틀렸다 — ㄱ은 왼쪽 끝 키가 아니고, ㅂ ←는 이슈 #246으로 →에 옮겼다).
- 0.13(이슈 #208·#198 오너 지시, #237): 그어서 나오는 모든 글자(쿼티 대문자·두벌식 시프트 자모 포함)를 네 변의 가운데에 표시(3.4, `TapKeyResolver.edgeHints`). 창 테두리에 닿은 점은 그 변의 나감(이슈 #237, LLD-04 3.2) — 탭 자판에도 같다: 맨 아랫줄 ↓·양 끝 열 ←→도 나감이 되고, 배정이 없으면 결과는 탭과 같다(TG-08).
- 0.12(이슈 #205, 오너 결정 2026-11-02 "모든 모드에서 모드 키를 위로 올리면 설정"): 모드 키는 **뗄 때** 판정한다(4절). 처음 통과한 변이 위(`Exit`·`ExitReturn`·`Rub`의 방향이 UP)면 `sink.onSettingsStroke()`(조합 확정 뒤 설정 화면, LLD-08 6.1), 그 밖(탭·좌우가 첫 변)은 지금처럼 모드 전환(`onFunctionDown(MODE)` → `onFunctionUp(MODE)`). 판정은 stroke-core `ModeKeyGesture.of(g)`(HLD 8.2 공용 판정, `OPEN_SETTINGS` / `MODE_KEY_TAP`)이고 긋기·그래피티 뷰도 같이 쓴다. 근거 SRS 0.52 FR-CMN-13·FR-CMN-11a ①. 누를 때는 진동·소리만, 떼기 전 취소는 입력 없음. 시험 MK-00~MK-05(LLD-11 5.5).
- 0.11(이슈 #198 추가 오너 지시 두 번): 맨 아랫줄을 한/영 72 · 스페이스 180 · `,` · `.` · `/` 각 36으로(3.2, 배정 그대로). i(ㅑ) = ← `[` → `]`, o(ㅐ) = ← `{` → `}`, 둘 다 ↓ 배정 없음(3.3). p·l은 그대로. 글자 키 `assign`이 `right`도 받는다. SRS는 설계팀이 고친다.
- 0.10(이슈 #198, 오너 결정 2026-11-02 + 같은 날 추가 지시 두 번): 기호 키(`'` ↑`"` →`/` ↓`?`)를 없애고 `,` 키에 ← `'` → `"`를 넣었고, 맨 아랫줄 끝에 새 기호 키 `/`(탭 `/` ↑ `?`)를 두었다(3.3). 편집 줄은 Esc · ⇥ · ← · ↑ · ↓ · → · Del · ⌫ 8개(각 45dp, NFR-06 예외), `-` 키는 넷째 줄 m 오른쪽, ↵는 넷째 줄 오른쪽 끝, 맨 아랫줄은 한/영 72 · 스페이스 144 · `,` · `.` · `/` 각 48(빈칸 없음)이다(3.2). 새 기능키 `ESC`(KEYCODE_ESCAPE)·`DELETE`(KEYCODE_FORWARD_DEL)는 방향키처럼 조합을 먼저 확정하고 보낸다(LLD-08 4·6절). Del은 ⌫처럼 누르고 있으면 반복한다(PM 권장안, 오너 확정 전, 6절). SRS 3.3은 설계팀이 고친다(SRS 0.49).
- 공통 타입은 [LLD-03-modules.md#3](LLD-03-modules.md#3-모듈-사이-공통-타입), 변 통과 추적은 [LLD-04-stroke-hangul.md#3](LLD-04-stroke-hangul.md#3-변-통과-추적-edgecrossingtracker-stroke-core), 키 찾기는 [LLD-04-stroke-hangul.md#2](LLD-04-stroke-hangul.md#2-키-찾기-keyhittester-stroke-core)에 있다.
- 요구: SRS 3.3(FR-TAP), 탭 자판 동작 표, PC식 배치 그림, 기호 배정표, FR-CMN-11, 11a, FR-CMN-07c.
- 따로 만든 숫자 키패드는 없다. 숫자류 입력창(숫자·전화·날짜시간·비밀번호)도 이 문서의 영문 쿼티 자판으로 시작한다(SRS FR-CMN-04, HLD 5A, 진입·이탈은 LLD-08 5절).

## 1. 범위

| 절 | 구성 요소 | 모듈 |
|---|---|---|
| 2 | `TapGestureClassifier`, `TapKeyResolver`, `ScriptTables` (탭 자판 동작 표 판정) | stroke-core |
| 3 | 자판 배치 데이터(`TapLayout`, 쿼티·두벌식), 모서리 표시 데이터 | stroke-core |
| 4 | `TapKeyboardView` (그리기·터치) | app |
| 5 | `MultiPointerQueue` (여러 손가락 대기열) | app |
| 6 | `RepeatTimer`, `RepeatSchedule` (방향키·⌫ 반복) | app |
| 7 | `TapKeyAccessHelper` (접근성) | app |
| 8 | 시퀀스: 탭 자판 키 입력 | – |

## 2. 탭 자판 동작 표 판정 (stroke-core)

### 2.1 TapGestureClassifier

```kotlin
sealed interface TapGesture {
    data object Tap : TapGesture
    data class Exit(val dir: Dir) : TapGesture          // 같은 변 1회 통과
    data class ExitReturn(val dir: Dir) : TapGesture    // 2회(나갔다 돌아옴) — 결과는 Exit(dir)과 같음(2.2)
    data class Rub(val dir: Dir) : TapGesture           // 3회 이상(문지르기)
}
class TapGestureClassifier(key: KeyGeom, minCrossDp: Float) {
    fun begin(p: StrokePoint); fun add(p: StrokePoint); fun finish(p: StrokePoint): TapGesture
}
```

#### `finish(p): TapGesture`
- 처리: `EdgeCrossingTracker`(LLD-04 3절)로 기록을 얻는다. 기록 없음 → `Tap`. 첫 나감 변 `e1`, `n = countOf(e1)`: 1 → `Exit`, 2 → `ExitReturn`, ≥3 → `Rub`.
- 길이·꺾기는 보지 않는다(탭 자판 동작 표에 없음). 같은 `minCrossDp`를 쓴다(FR-TAP-02b).
- 근거: FR-TAP-01a, 02a, 02b. 시험: TG-01~TG-08.

### 2.2 TapKeyResolver

```kotlin
data class TapOutput(val text: String?, val jamo: Char?,        // jamo: 두벌식 호환 자모(stroke-core는 core-hangul에 의존하지 않음, LLD-03 1.2)
                     val errorVibe: Boolean)
object TapKeyResolver {
    fun resolve(key: TapKeyDef, g: TapGesture): TapOutput
}
```

`resolve` — SRS 탭 자판 동작 표(8행 × 3열)를 그대로 옮긴다. `a` = 키의 `KeyAssign`(3.1).

```
1. g를 정규화: ExitReturn(d) → Exit(d)                       // 나갔다 돌아옴 = 그 방향 나감(표 8행)
             Rub(LEFT) → Exit(LEFT), Rub(RIGHT) → Exit(RIGHT)  // 좌우 문지르기 = 그 방향 나감(표 7행)
2. 아래 표로 출력을 찾는다.
```

| 키 종류 \ 동작 | Tap | Exit(UP) | Exit(DOWN) | Exit(LEFT/RIGHT) | Rub(UP) | Rub(DOWN) |
|---|---|---|---|---|---|---|
| `QWERTY_LETTER` | 소문자 | 대문자 | `a.down` ?: 소문자 | `a.left/right` ?: 소문자 | 윗첨자 | 아래첨자 |
| `DUBEOLSIK_LETTER` | 기본 자모 | 시프트 자모(ㄲ ㄸ ㅃ ㅆ ㅉ ㅒ ㅖ) ?: `compoundJamo[UP]`(ㄵ ㅀ ㄿ) ?: 기본 | `compoundJamo[DOWN]` ?: `a.down` ?: 기본 | `compoundJamo[LEFT/RIGHT]` ?: `a.left/right` ?: 기본 | 기본 | 기본 |
| `NUMBER_ROW`, `SYMBOL` | `a.tap` | `a.up` | `a.down` ?: `a.tap` | `a.left/right` ?: `a.tap` | `a.tap` | `a.tap` |

- 윗첨자·아래첨자가 유니코드에 없으면 일반 글자 + `errorVibe = true`(FR-TAP-01b).
- 두벌식 겹받침(`compoundJamo`, 3.3 두벌식 겹받침 배정표, FR-TAP-02c)은 **나감**에만 있다. 1번 정규화로 나갔다 돌아옴과 좌우 문지르기는 그 방향 나감과 같고, ↑·↓ 문지르기(`Rub(UP)`·`Rub(DOWN)`)는 겹받침이 있는 키에서도 기본 자모다(0.18, FSS#97: ㄴ ↑ ㄵ·ㅍ ↑ ㄿ·ㄱ ↓ ㄳ 등도 ㄹ ↑ ㅀ · ↓ ㄼ과 같은 규칙). 겹받침과 시프트 자모가 같은 키의 ↑에 함께 있는 일은 없고(ㄴ·ㄹ·ㅍ은 시프트 자모가 없다), 겹받침과 배정 기호가 같은 방향에 함께 있는 일도 없다(겹받침 키 r·s·f·q·t·x·v는 `assign`이 없다).
- 1번 정규화는 SRS 동작 표의 "← · → 문지르기"·"나갔다 돌아옴(같은 변 2번 통과)" 행이다(SRS 3.3 탭 자판 동작 표, DCR-02 결정). 예: a ↑ 나갔다 돌아옴 = A, i ← 문지르기 = `[`, i → 나갔다 돌아옴 = i(→ 배정 없음). 그래서 모든 획이 출력을 하나 낸다(입력 없는 결과가 없다).
- `TapGestureClassifier`는 물리 동작을 그대로 구분하고(시험 TG), 결과 규칙은 이 함수에만 둔다.
- 배정 기호(`text`)를 넣는 경로와 자모(`jamo`)를 넣는 경로는 호출자(`InputDispatcher`, LLD-08 6절)가 나눈다. 두벌식 조합 중 기호·숫자면 먼저 확정한다(FR-TAP-11a).
- 근거: FR-TAP-01a, 01b, 02a, 03a, 11, FR-SWP-44, SRS 탭 자판 동작 표. 시험: TK-01~TK-24(표의 칸마다 하나), TA-01~TA-12.

### 2.3 ScriptTables (윗첨자·아래첨자)

```kotlin
object ScriptTables { val SUPER: Map<Char, String>; val SUB: Map<Char, String> }   // 값이 보조 평면 문자일 수 있어 String
```

| 글자 | 윗첨자 | 아래첨자 | 글자 | 윗첨자 | 아래첨자 |
|---|---|---|---|---|---|
| a | ᵃ U+1D43 | ₐ U+2090 | n | ⁿ U+207F | ₙ U+2099 |
| b | ᵇ U+1D47 | 없음 | o | ᵒ U+1D52 | ₒ U+2092 |
| c | ᶜ U+1D9C | 없음 | p | ᵖ U+1D56 | ₚ U+209A |
| d | ᵈ U+1D48 | 없음 | q | 𐞥 U+107A5 | 없음 |
| e | ᵉ U+1D49 | ₑ U+2091 | r | ʳ U+02B3 | ᵣ U+1D63 |
| f | ᶠ U+1DA0 | 없음 | s | ˢ U+02E2 | ₛ U+209B |
| g | ᵍ U+1D4D | 없음 | t | ᵗ U+1D57 | ₜ U+209C |
| h | ʰ U+02B0 | ₕ U+2095 | u | ᵘ U+1D58 | ᵤ U+1D64 |
| i | ⁱ U+2071 | ᵢ U+1D62 | v | ᵛ U+1D5B | ᵥ U+1D65 |
| j | ʲ U+02B2 | ⱼ U+2C7C | w | ʷ U+02B7 | 없음 |
| k | ᵏ U+1D4F | ₖ U+2096 | x | ˣ U+02E3 | ₓ U+2093 |
| l | ˡ U+02E1 | ₗ U+2097 | y | ʸ U+02B8 | 없음 |
| m | ᵐ U+1D50 | ₘ U+2098 | z | ᶻ U+1DBB | 없음 |

- 숫자: 윗첨자 ⁰ U+2070, ¹ U+00B9, ² U+00B2, ³ U+00B3, ⁴~⁹ U+2074~U+2079 / 아래첨자 ₀~₉ U+2080~U+2089.
- 아래첨자 없음 9자(b c d f g q w y z)는 SRS FR-TAP-01b와 같다.
- q 윗첨자는 유니코드 14에 들어간 보조 평면 문자라 글꼴에 없을 수 있다(D-14, 실기기 확인 DV-12). 그래서 표의 값 타입이 `String`이다.

## 3. 자판 배치 데이터

### 3.1 타입

```kotlin
enum class TapKeyKind { QWERTY_LETTER, DUBEOLSIK_LETTER, NUMBER_ROW, SYMBOL, FUNCTION }
data class KeyAssign(val tap: String, val up: String? = null, val left: String? = null,
                     val right: String? = null, val down: String? = null)
data class TapKeyDef(val id: String, val row: Int, val x: Float, val w: Float,    // x·w는 360dp 기준 dp
                     val kind: TapKeyKind, val label: String,
                     val assign: KeyAssign? = null,
                     val jamo: Char? = null, val shiftJamo: Char? = null,      // 두벌식 글자 키(호환 자모 문자)
                     val function: FunctionKey? = null,
                     val compoundJamo: Map<Dir, Char> = emptyMap())            // 두벌식 글자 키: 방향별 겹받침(3.3)
data class TapLayout(val name: String, val rows: Int, val keys: List<TapKeyDef>,
                     val blanks: List<BlankDef>)                                  // 들여쓰기 빈칸
data class BlankDef(val row: Int, val x: Float, val w: Float, val nearestKeyId: String)
object TapLayouts { val QWERTY: TapLayout; val DUBEOLSIK: TapLayout }
```

- 화면 좌표: `x_px = x / 360 × 폭`, 줄 y = 창 위에서 `row × 줄 높이`(쿼티·두벌식은 6줄로 창 전체). 가로 화면에서는 폭만 늘고 줄 높이는 그대로(FR-CMN-12).
- "불가" 방향은 데이터에 넣지 않는다. 그 방향은 바깥 변이라 기록이 생기지 않는다(LLD-03 3.3).
- **여유 칸(0.17, FSS#84):** `TapGeometry.build(layout, widthDp, rowHeightDp, sideDp)`가 키마다 `KeyGeom.build(id, rect, widthDp, heightDp, sideDp)`를 부른다(LLD-04 2절). `sideDp` = 뷰의 `sideMarginPx / density`(부모가 재기 전에 알림, LLD-08 7.2). 그래서 왼쪽·오른쪽 끝 키의 바깥쪽 변은 바깥 변이 아니고, 그 방향 획은 변 너머 `minCrossDp`에서 `Exit`이다(안쪽 키와 같은 기준). 그 방향에 배정이 없는 키는 결과가 탭과 같다(`TapKeyResolver`, 그대로). `TapKeyboardView.rebuildGeometry`는 배치·폭·줄 높이·`sideDp`가 모두 같을 때만 다시 만들지 않는다. 들여쓰기 빈칸(░)의 "같은 줄 키로" 규칙(4절 `hit`)은 뷰가 받은 내려옴에만 쓰이고, 여유 칸은 뷰 밖이라 거기서 내려온 터치는 뷰에 오지 않는다(입력 없음).

### 3.2 쿼티 배치 (SRS 3.3 그림, FR-TAP-03)

| 줄 | 키 (폭 dp) |
|---|---|
| 0 편집 줄 | Esc ESC(45) · ⇥ TAB(45) · ← (45) · ↑ (45) · ↓ (45) · → (45) · Del DELETE(45) · ⌫ BACKSPACE(45) |
| 1 숫자 줄 | 1 2 3 4 5 6 7 8 9 0 NUMBER_ROW(각 36) |
| 2 | q w e r t y u i o p (각 36) |
| 3 | 빈칸 18(→ a) · a s d f g h j k l (각 36) · 빈칸 18(→ l) |
| 4 | 빈칸 18(→ z) · z x c v b n m (각 36) · `=` SYMBOL(36) · ↵ ENTER(54) |
| 5 기능키 줄 | 한/영 MODE(72) · 스페이스(180) · `,` SYMBOL(36) · `.` SYMBOL(36) · `/` SYMBOL(36) |

- 두벌식은 같은 자리에 2줄 ㅂㅈㄷㄱㅅㅛㅕㅑㅐㅔ, 3줄 ㅁㄴㅇㄹㅎㅗㅓㅏㅣ, 4줄 ㅋㅌㅊㅍㅠㅜㅡ. `shiftJamo`: ㅂ→ㅃ, ㅈ→ㅉ, ㄷ→ㄸ, ㄱ→ㄲ, ㅅ→ㅆ, ㅐ→ㅒ, ㅔ→ㅖ, 나머지 null(기본 자모).
- 모드 키는 맨 아랫줄 왼쪽(SRS 3.3, 모든 모드 공통). 시험 TL-01(폭 합 = 360, 모든 줄), TL-02(NFR-06 크기), TL-03(모드 키 위치).
- ⌫는 편집 줄 오른쪽 끝, ↵는 아랫글자 줄(z 줄) 오른쪽 끝이다(0.10, 이슈 #198). 키 id는 `esc`·`delete`·`minus`·`slash`, 키 라벨은 `Esc`·`Del`.
- **NFR-06 예외(0.10, 이슈 #198):** 편집 줄 기능키 8개는 45dp로 NFR-06의 기능키 48dp보다 좁다. 오너 지시로 Esc·Del을 더한 결과다(PM 결정, 글자 키 36dp보다는 넓음). TL-02는 편집 줄 기능키에 45dp를 하한으로 쓴다. SRS 문구는 설계팀이 고친다.
- 빈칸은 들여쓰기 3개(3줄 양쪽, 4줄 왼쪽)뿐이다. 0.10에서 맨 아랫줄의 빈칸은 없앴다(`/` 키와 넓힌 스페이스가 채움).

### 3.3 기호 배정 (SRS 기호 배정표, FR-TAP-03a)

| 키 | tap | up | left | right | down |
|---|---|---|---|---|---|
| `=`(4줄 m 오른쪽, id `minus`) | `=` | `_` | `-` | – | `+` |
| 숫자 1 | `1` | `!` | (불가) | `` ` `` | `~` |
| 숫자 2~9 | `2`~`9` | `@ # $ % ^ & * (` | – | – | – |
| 숫자 0 | `0` | `)` | – | (불가) | – |
| i(ㅑ) | (글자) | (대문자) | `[` | `]` | – |
| o(ㅐ) | (글자) | (대문자, ㅒ) | `{` | `}` | – |
| p(ㅔ) | (글자) | (대문자, ㅖ) | `\` | (불가) | `\|` |
| l(ㅣ) | (글자) | (대문자) | `;` | – | `:` |
| `,` | `,` | `<` | `'` | `"` | (불가) |
| `.` | `.` | `>` | – | – | (불가) |
| `/`(맨 아랫줄 끝) | `/` | `?` | – | (불가) | (불가) |

- 글자 키의 `assign`에는 `left`·`right`·`down`만 넣고(0.11: i·o는 `left`·`right`, p·l은 `left`·`down`) `tap`·`up`은 `TapKeyResolver`가 키 종류로 정한다. `→` 배정이 있으면 → 나감·문지르기·나갔다 돌아옴도 그 기호다(동작 표 4·7·8행).
- 두 자판은 키 자리 단위로 같은 배정을 쓴다(ㅣ 자리 = l 자리).
- 0.10(이슈 #198): 기호 키(`'` 키, id `quote`)는 없다. `'`·`"`는 `,`의 ←·→, `/`·`?`는 새 `/` 키의 탭·↑다. `=` 키(m 오른쪽)의 →는 이웃이 ↵라 바깥 변이 아니지만 배정이 없다(탭과 같음). 시험 TL-05: 특수 문자 32개가 각각 정확히 한 키·한 방향에만 있다.
- 시험 TL-04: 배정표의 숫자 10 + 특수 문자 32가 모두 한 동작으로 나온다(FR-TAP-11).

**두벌식 겹받침 배정표 (0.18, SRS FR-TAP-02c의 표와 같다. 정본은 SRS)** — `TapLayouts.COMPOUND_JAMO`(자모 → 방향 → 겹받침)가 두벌식 글자 키의 `TapKeyDef.compoundJamo`가 된다. 쿼티에는 없다(빈 맵). "–"는 배정 없음(기본 자모), ↑의 ㄲ·ㅃ·ㅆ는 `shiftJamo`(겹받침 아님), 굵은 글자가 겹받침이다.

| 키(id) | tap | up | left | right | down |
|---|---|---|---|---|---|
| ㄱ (`r`) | ㄱ | ㄲ | – | – | **ㄳ** |
| ㄴ (`s`) | ㄴ | **ㄵ** | – | – | **ㄶ** |
| ㄹ (`f`) | ㄹ | **ㅀ** | **ㄺ** | **ㄻ** | **ㄼ** |
| ㅂ (`q`) | ㅂ | ㅃ | – | – | **ㅄ** |
| ㅅ (`t`) | ㅅ | ㅆ | – | – | **ㄽ** |
| ㅌ (`x`) | ㅌ | – | **ㄾ** | – | – |
| ㅍ (`v`) | ㅍ | **ㄿ** | – | – | – |

- 0.17까지는 ㄱ ← ㄳ, ㄴ ← ㄵ · → ㄶ, ㅂ → ㅄ, ㅅ ← ㄽ, ㅍ ← ㄿ였다(FSS#97로 옮김). 옮겨서 비게 된 방향은 다른 배정이 없으므로 기본 자모다.
- 겹받침 11개는 각각 한 키·한 방향에만 있다. 출력은 자모(`TapOutput.jamo`)라 조합기로 간다(LLD-04 7절, FR-HGL-02a). 시험 TA-12·TA-20·TA-21, TK-05·TK-08·TK-11·TK-14·TK-17·TK-20·TK-23(LLD-11 5.2·5.3).

### 3.4 모서리 표시 (FR-TAP-03b)

- `TapKeyResolver.edgeHints(key)`로 그린다(0.13, 오너 #198·#208: 그어서 나오는 모든 글자 중 탭과 다른 것 — 쿼티 ↑ 대문자, 두벌식 시프트 자모(ㅃ ㅉ ㄸ ㄲ ㅆ ㅒ ㅖ)·겹받침(0.14; 0.18 FSS#97부터 ㄴ ↑ ㄵ·ㅍ ↑ ㄿ은 위 변, ㄱ ↓ ㄳ·ㄴ ↓ ㄶ·ㅂ ↓ ㅄ·ㅅ ↓ ㄽ은 아래 변 — `edgeHints`가 `resolve(Exit(d))`의 결과를 그대로 쓰므로 배정 자료만 바뀌면 표시가 따라온다), 숫자 줄·기호 키·글자 키의 배정 기호. 기능 키는 없음. 모서리 자리는 쓰지 않음): ↑ → 위 변 가운데, ← → 왼쪽 변 가운데(세로 가운데), ↓ → 아래 변 가운데, → → 오른쪽 변 가운데. 글자 크기는 키 라벨의 40 %(`HINT_SCALE`), 변에서 2dp(`HINT_PAD_DP`) — 36dp 키(라벨 약 19dp)에서 ←·→ 기호(약 8dp)가 가운데 라벨과 겹치지 않는다. 0.12까지는 `up` 오른쪽 위, `left` 왼쪽 아래, `right` 오른쪽 아래. 0.12까지는 글자 키의 `up`(대문자·쌍자음)을 표시하지 않았다.
- 글자 크기는 키 글자의 40%(D-15, 레이아웃 세부).

### 3.5 숫자류 입력창

- 숫자류 입력창은 `TapLayouts.QWERTY`(6줄, 숫자 줄 포함)를 그대로 쓴다. 숫자는 숫자 줄로 넣는다(FR-CMN-04, FR-TAP-11). 모드 키도 그대로 있다(FR-CMN-04b).
- 0.1의 숫자 자판 데이터(`NUMPAD_*`, `TapKeyKind.NUMPAD`)는 없앴다(SRS FR-CMN-04a 삭제·TBD-14 폐기, 오너 결정 2026-10-27 이슈 #3).

## 4. TapKeyboardView (app)

```kotlin
class TapKeyboardView(context: Context, private val sink: TapSink, private val guard: EngineGuard,
                      private val feedback: FeedbackPort) : View(context) {
    fun setLayout(layout: TapLayout, th: Thresholds, rowHeightDp: Float)
}
interface TapSink {                              // InputDispatcher가 구현(LLD-08 6절)
    fun onTapOutput(key: TapKeyDef, out: TapOutput)
    fun onFunctionDown(key: FunctionKey); fun onFunctionUp(key: FunctionKey)
}
```

| 함수 | 처리 | 근거 |
|---|---|---|
| `setLayout` | `KeyGeom`(dp)·`KeyHitTester`를 다시 만든다 | FR-TAP-03 |
| `hit(p)` | ① 점이 `BlankDef` 사각형 안이면 그 `nearestKeyId` 키(같은 줄의 a·l·z). ② 아니면 `KeyHitTester.hit(p)`. ①을 먼저 보므로 빈칸 위쪽에서 윗줄 키가 더 가까워도 같은 줄 키가 된다 | SRS 3.3 빈칸 |
| `onMeasure` | 폭 = 부모, 높이 = `layout.rows × rowHeight` | 5A |
| `onDraw` | 키 배경·글자·모서리 표시(3.4). 누른 키 강조 | FR-TAP-03b |
| `onTouchEvent` | 아래 | FR-CMN-11 |

#### `onTouchEvent(e)`
```
DOWN / POINTER_DOWN (index i, id):
  p = dp(e, i); key = hit(p)
  feedback.key()                                            // 키 입력 진동·소리는 누를 때(FR-CMN-11a ②). 빈칸도 가까운 키라 같음
  if key.function == MODE:                                  // 0.12(#205): 누를 때는 입력 없음, 뗄 때 판정
      fnPointers[id] = (key, TapGestureClassifier(key.geom, th.minCrossDp).begin(p))
  else if key.kind == FUNCTION:
      guard.stroke("Tap.fn") { sink.onFunctionDown(key.function) }   // 누르는 즉시, 대기열 밖(FR-CMN-11, 11a ①)
      if key.function ∈ {BACKSPACE, ARROW_*, DELETE}: repeat.start(id, key.function)   // FR-CMN-07c, FR-TAP-10, DELETE 0.10(#198)
      fnPointers[id] = key
  else:
      cls[id] = TapGestureClassifier(key.geom, th.minCrossDp); cls[id].begin(p)
      queue.onDown(id)                                      // 5절
MOVE: 각 포인터의 히스토리 점을 cls[id].add (모드 키 포인터면 그 분류기에)
UP / POINTER_UP (id):
  if id = 모드 키 포인터: s = guard.run("Tap.classify") { ModeKeyGesture.of(c.finish(p)) == OPEN_SETTINGS }   // 실패면 입력 없음
        s == true  → guard.stroke("Tap.fn") { sink.onSettingsStroke() }                      // 위로 나감 = 설정(#205)
        s == false → guard.stroke("Tap.fn") { sink.onFunctionDown(MODE) }; guard.stroke("Tap.fn") { sink.onFunctionUp(MODE) }   // 탭 = 모드 전환
  else if id ∈ fnPointers: repeat.stop(id); sink.onFunctionUp(fn); remove
  else: r = guard.run("Tap.classify") { TapKeyResolver.resolve(key, cls[id].finish(p)) }   // 순수 계산, 상태 없음
        outs = if r == null then queue.onCancel(id) else queue.onUp(id, key, r)            // 실패한 칸만 비우고 뒤 결과는 흘려보냄
        for (k, o) in outs: guard.stroke("Tap.output") { sink.onTapOutput(k, o) }        // 출력 하나 = 보호 단위 하나(LLD-09 2절), 누른 순서대로
CANCEL: outs = queue.onCancelAll(); repeat.stopAll(); fnPointers 각각(떼지 않은 모드 키 빼고) sink.onFunctionUp(fn); fnPointers.clear(); cls.clear()
        for (k, o) in outs: guard.stroke("Tap.output") { sink.onTapOutput(k, o) }   // 이미 뗀 대기 결과만, 누른 순서대로
```
- `ACTION_CANCEL`(진행 중인 모든 터치가 함께 취소됨): 아직 떼지 않은 모든 포인터는 결과 없이 대기열에서 빠진다. 이미 뗐지만 앞 키를 기다리던 결과는 취소된 칸을 빼고 누른 순서대로 낸다. 이미 누를 때 처리한 기능키 입력은 되돌리지 않고 반복만 멈춘다(`onFunctionUp`은 입력을 만들지 않는 뗌 알림이다). 근거: SRS FR-CMN-11a ③, HLD 5A 처리 시점(DCR-11 결정).
- 결과는 처음 누른 키로만 정한다(SRS 동작 표 아래 문단). `,` 키를 →로 그어 `.` 위를 지나도 `"`, `=` 키를 ←로 그어 m 위를 지나도 `-`.
- Esc·탭 키(⇥)·방향키·Del·⌫·스페이스·↵·모드 키 처리 내용은 `InputDispatcher`(LLD-08 6절)에 있다.
- **모드 키 ↑ = 설정(0.12, 이슈 #205):** 모드 키만 누를 때가 아니라 뗄 때 처리한다(다른 기능키는 그대로 누를 때). 판정은 글자 키와 같은 변 넘음(`TapGestureClassifier`, `minCrossDp`)이고 `ModeKeyGesture.of(g)` = 처음 통과한 변이 위면(`Exit`·`ExitReturn`·`Rub` 모두, 되돌아옴·문지르기는 보지 않음 — SRS FR-CMN-13 "처음 통과한 변만") `OPEN_SETTINGS`, 그 밖(탭, 첫 변 ←·→)은 `MODE_KEY_TAP` = 모드 전환. 맨 아랫줄이라 아래 나감은 바깥 변(기록 없음 = 탭). 대기열 밖이라 잡고 있는 글자 키를 기다리지 않고, 글자 키도 모드 키를 기다리지 않는다. 떼기 전 `ACTION_CANCEL`이면 입력 없음(모드 전환도 설정도 아님). 접근성 클릭(7절)은 그대로 탭 = 모드 전환이고 설명 문구에 "위로 밀면 설정"을 붙였다(`a11y_key_mode`).

## 5. MultiPointerQueue (app)

```kotlin
class MultiPointerQueue {
    fun onDown(pointerId: Int)
    fun onUp(pointerId: Int, key: TapKeyDef, out: TapOutput): List<Pair<TapKeyDef, TapOutput>>
    fun onCancel(pointerId: Int): List<Pair<TapKeyDef, TapOutput>>
    fun onCancelAll(): List<Pair<TapKeyDef, TapOutput>>
    fun clear()
}
```
- 상태: 누른 순서대로의 칸 목록 `[(pointerId, result?)]`.
- `onDown`: 끝에 빈 칸을 붙인다.
- `onUp`: 그 포인터 칸에 결과를 채우고, **앞에서부터 결과가 찬 칸을 연속으로** 꺼내 돌려준다. 앞 칸이 아직 비어 있으면(앞 키가 눌려 있음) 아무것도 돌려주지 않는다.
- `onCancel(id)`: 그 칸을 결과 없음으로 채우고 같은 방식으로 흘려보낸다(결과 없음 칸은 출력에서 뺀다). 판정 실패(`guard.run`이 null) 때 쓴다.
- `onCancelAll()`: `ACTION_CANCEL` 때. 아직 결과가 없는 칸(떼지 않은 포인터)을 모두 결과 없음으로 채우고, 남은 칸 가운데 결과가 있는 것을 누른 순서대로 모두 돌려준 뒤 목록을 비운다(SRS FR-CMN-11a ③).
- 기능키는 이 큐에 들어가지 않는다.
- 근거: FR-CMN-11, 11a, HLD 5A. 시험: MQ-01~MQ-08.

## 6. RepeatTimer와 RepeatSchedule (app)

```kotlin
class RepeatSchedule(val initialDelayMs: Long, val intervalMs: Long) {
    fun firesBetween(pressAt: Long, from: Long, to: Long): Int     // 순수 계산(시험용)
}
class RepeatTimer(private val handler: Handler, private val schedule: RepeatSchedule,
                  private val fire: (FunctionKey) -> Unit) {
    fun start(pointerId: Int, key: FunctionKey); fun stop(pointerId: Int); fun stopAll()
}
```
- 누름 자체가 한 번이다(`onFunctionDown`에서 이미 처리). `start`는 `initialDelayMs` 뒤 첫 반복, 그 뒤 `intervalMs`마다 `fire(key)`. 반복 한 번 = 그 키 한 번(⌫면 `InputDispatcher.backspace()`, FR-CMN-07c).
- 값: `ViewConfiguration.getKeyRepeatTimeout()`(첫 지연)과 `getKeyRepeatDelay()`(간격) — 플랫폼 기본값을 쓴다(D-16). 방향키와 ⌫가 같은 타이머(HLD 5A). 0.10부터 Del도 같은 타이머로 반복한다(이슈 #198, PM 권장안). 진동·소리는 누를 때 한 번(반복에는 없음, ⌫와 같음). Esc는 반복하지 않는다.
- 그래피티 보조 줄 ⌫도 이 클래스를 쓴다. 각 뷰가 `fire`를 `{ k -> guard.stroke("Repeat") { sink.onFunctionDown(k) } }`로 넘긴다(반복 한 번 = 보호 단위 하나, LLD-09 2절).
- 반복 한 번에는 키 입력 피드백(`feedback.key()`)을 내지 않는다. 피드백은 누를 때 한 번이다(FR-CMN-11a ②: 반복 중에는 내지 않음, HLD 8장 피드백, 오너 결정 #6 OD-3).
- `ACTION_CANCEL`·뗌·창 숨김 때 멈춘다(`stop`, `stopAll`, FR-CMN-11a ③).
- 근거: FR-CMN-07c, FR-TAP-10. 시험: RT-01~RT-05.

## 7. TapKeyAccessHelper (app, NFR-07)

```kotlin
class TapKeyAccessHelper(private val view: TapKeyboardView) : ExploreByTouchHelper(view)
```
| 함수 | 처리 |
|---|---|
| `getVirtualViewAt(x, y)` | `hit(x, y)`의 키 번호(빈칸은 가까운 키) |
| `getVisibleVirtualViews(list)` | 키 번호 전부 |
| `onPopulateNodeForVirtualView(id, node)` | 경계 = 키 사각형(px), `contentDescription` = 키 글자(기능키는 이름 문자열 리소스), 동작 = `ACTION_CLICK` |
| `onPerformActionForVirtualView(id, CLICK)` | 그 키의 `Tap` 결과를 `sink`로(기능키는 down+up) |

- 쿼티·두벌식에만 붙인다(NFR-07). 읽어 주는 문구는 UI 문구라 문자열 리소스로 둔다.
- 시험: 실기기 DV-10.

## 8. 시퀀스 — 탭 자판 키 입력 (두벌식 "각" 조합 중 숫자 줄 2를 ↑)

```
TapKeyboardView.onTouchEvent(DOWN) → hit → 숫자 2 키 → TapGestureClassifier.begin, MultiPointerQueue.onDown
MOVE → EdgeCrossingTracker.add → OUT(TOP)
UP   → finish → Exit(UP) → TapKeyResolver.resolve → TapOutput(text="@")
     → MultiPointerQueue.onUp → [(2 키, "@")]
     → InputDispatcher.onTapOutput (LLD-08 6절)
          → HangulComposer.finish() + ComposingController.finishComposing()   // FR-TAP-11a
          → ComposingController.commitText("@")
          → ModeController.onInput()
```
