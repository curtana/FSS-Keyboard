# LLD-05A 탭 자판 (쿼티·두벌식)

- 대응 HLD 절: 5A장(탭 자판)
- 버전 **0.26** (2026-11-18)
- 0.26(FSS#141, SRS 0.95 FR-TAP-02d·02g ①, HLD 0.88 5A): **두벌식 짝 모음 긋기 빼기** — `TapLayouts.PAIR_VOWEL`을 지우고 `dirJamo = COMPOUND_JAMO[jamo] + COMPOUND_VOWEL[jamo]`(3.3). ㅗ ↑ · ㅓ ← · ㅏ → · ㅜ ↓는 배정 없음 = 탭(2.2 빈 방향), ㅓ·ㅏ는 `dirJamo`가 비어 4방향, ㅗ·ㅜ는 대각선 겹모음으로 8방향 그대로. 3.3 짝 모음 배정표 삭제, 겹모음 원리의 세로 뒤집기 조건을 "키 모음 자신의 짧은 획 방향(ㅗ ↑, ㅜ ↓)"으로(결과 같음, `COMPOUND_VOWEL` 그대로), 2.2 동작 표 두벌식 줄의 예, 3.4 `edgeHints` 예. 판정·표시·Ctrl·Alt 코드는 그대로
- 0.25(FSS#117 오너 결정 "차라리 ctrl, alt가 눌리면 순간 자반을 영문으로 바꾸자 키 선택이되면 다시 한글로 돌아오고"·보탬, SRS 0.94 FR-TAP-15 ⑥, HLD 0.87 5A "켜진 동안 두벌식을 쿼티로 그림"): **Ctrl·Alt가 켜진 동안 두벌식을 쿼티로 그림** — `TapLayouts.shown(layout, chordOn)`(3.1), `TapKeyboardView`의 `onDraw`·`chordChanged()`·`shownKeyAt`(4절), 접근성 이름(7절). 시험 TL-13, CV-10~CV-14
- 0.24(FSS#110 오너 결정 "모음은 획의 방향으로 해야지"·"좋아 대각선 써보자"·⌫ "한 단계씩", SRS 0.92 FR-TAP-02g·FR-HGL-04f·TBD-39, HLD 0.85 4.2·5A "두벌식 겹모음 한 번 긋기"): **두벌식 겹모음 한 번 긋기** — `TapLayouts.COMPOUND_VOWEL`(ㅗ → ㅘ · ↓ ㅚ · ↘ ㅙ, ㅜ ← ㅝ · ↑ ㅟ · ↙ ㅞ, ㅡ ↓ ㅢ)을 겹받침 표·짝 모음 표와 따로 두고 셋을 `TapKeyDef.dirJamo`로 합친다(3.3 "두벌식 겹모음 배정표"). **원리 둘을 나란히 적는다: 모음은 덧붙는 획이 뻗는 방향, 자음 겹받침은 뒤 자음 키의 위치.** ㅙ·ㅞ가 대각선이라 ㅗ(h)·ㅜ(n)는 데이터만으로 8방향 키가 되고(3.1 `eightWay`, 2.1 판정 그대로), ㅡ(m)는 4방향 그대로다. **판정·결과(`TapKeyResolver`)·표시(`edgeHints`·`drawHints`)·Ctrl·Alt(`ChordKeys`)·손 뗀 키 우선(`ReleaseKeyCompound`) 코드는 바꾸지 않았다** — 모두 데이터로 정해지고, 손 뗀 키 우선은 모음 키가 `COMPOUND_JONG`에 없어 이미 건너뛴다(4.1). 바뀐 코드는 `TapLayouts`의 표 하나와 `dirJamo` 합치기(셋, 한 방향에 둘이면 `require` 실패)다. 입력 단위를 획 단계로 나누는 것은 조합기 일이다(LLD-04 7.4). 꺾기(키를 벗어난 뒤 꺾음)는 SRS에서 폐기돼 분류기에 더하지 않았다. 시험 TL-09·TL-10(새 기대)·TL-12, TA-21·TA-22(새 기대)·TA-25, CM-15, GJ-02(새 기대), GV-01~GV-03, ID-43(LLD-11 0.65)
- 0.23(FSS#109 오너 결정 "8방향으로 해"·"만드는 원리가 일정해야지"·"비어있는 방향으로 긋는 것은 그냥 키를 누른 것과 같은 효과", PM 결정(문지르기), SRS 0.91 FR-TAP-02c ①~⑤·02e·03b·16 ③·TBD-38, HLD 0.84 4.2·5A "겹받침 방향 규칙과 8방향 키"): **겹받침 방향 규칙 하나와 8방향 키** — 방향 타입에 대각선을 더했다: `sealed interface Way { dx; dy }`를 `Dir`(그대로 네 값, 부호 `dx`·`dy`만 더함)과 새 `Diag`(↖ ↗ ↙ ↘)가 구현한다(LLD-03 3.3, D-75). `TapGesture`의 방향은 `Way`(2.1). `TapGestureClassifier(key, minCrossDp, eightWay)`는 `eightWay`이면 처음 나감 순간의 점과 누른 점의 각도로 8방향을 정한다(위아래좌우 60°·대각선 30°, 경계는 위아래좌우 쪽 — `wayOfAngle`·`wayOfVector`, 2.1). 나감·나갔다 돌아옴·문지르기는 처음 나간 변의 통과 횟수 그대로. `TapKeyResolver`는 대각선 나감·나갔다 돌아옴·문지르기를 그 대각선의 `dirJamo`로, 없으면 탭 결과로 낸다(2.2 1·2번). `TapKeyDef.dirJamo`는 `Map<Way, Char>`, **`TapKeyDef.eightWay` = 대각선 배정이 있는가**(데이터로 정함, 키 이름 갈래 없음 — 3.1). 겹받침 표 `COMPOUND_JAMO`(3.3)는 규칙 (sign 열 차, sign 줄 차)의 결과: ㄹ ↖ ㄼ ↑ ㄺ ↗ ㄽ ← ㄻ → ㅀ ↙ ㄾ ↓ ㄿ, ㄱ → ㄳ, ㄴ ↑ ㄵ → ㄶ, ㅂ → ㅄ(ㅅ·ㅌ·ㅍ 키의 겹받침은 없앰). 빈 방향 = 탭(2.2). `edgeHints`가 대각선도 내고 `TapKeyboardView`가 모서리에 그린다(3.4, 4절). `ChordKeys`는 고치지 않았다 — 쿼티 f에 대각선이 없어 F(2.4). **같은 판 덧붙임(SRS FR-TAP-02f, 오너 결정 "더해줘"): 손 뗀 키 우선** — 두벌식 자음 키에서 나감이 세어진 획을 겹받침 짝 자음 키 위에서 떼면 방향과 관계없이 그 겹받침(4.1). 뗀 점의 키는 `TapGeometry.releasedKey(p)`(뷰 안이면 누름과 같은 키 찾기, 뷰 밖·여유 칸은 없음), 짝 판정은 app의 순수 함수 `ReleaseKeyCompound.of(누른 키, 뗀 키)`가 `core-hangul`의 `JamoTables.COMPOUND_JONG`(조합기가 겹받침을 나누는 표와 같은 표)으로 한다 — stroke-core는 core-hangul에 의존하지 않으므로(LLD-03 1.2) 이 단계는 `TapTouchLogic.up`에 있고 `TapGestureClassifier`·`TapKeyResolver`는 뗀 키를 모른다(D-76). 동작(`TapGesture`)은 그대로 실어 Ctrl·Alt가 켜져 있으면 방향 규칙의 키 입력이 나간다. 시험 TG-11~14, TA-23·24, TL-10·11, CM-14, ID-42, GJ-01~03, CD-20(LLD-11 0.64).
- 0.22(FSS#104 오너 결정·FSS#107 오너 지시, SRS 0.90 FR-TAP-02c·02d·두벌식 짝 모음 배정표·TBD-37, HLD 0.83 5A): **두벌식 짝 모음 긋기와 ㅌ ↑ ㄾ** — `TapLayouts.PAIR_VOWEL`(ㅗ ↑ ㅛ, ㅓ ← ㅕ, ㅏ → ㅑ, ㅜ ↓ ㅠ)을 겹받침 표 `COMPOUND_JAMO`와 나란히 두고, 두 표를 키 정의의 방향별 자모 **`TapKeyDef.dirJamo`**(0.21까지 `compoundJamo`, 이름만 바꿈 — D-74)로 합친다(3.1·3.3). 겹받침 표의 ㅌ(x)를 ← ㄾ에서 ↑ ㄾ으로(←는 기본 자모 ㅌ). `TapKeyResolver`의 찾는 순서는 그대로이고(2.2, 짝 모음은 겹받침 자리), `edgeHints`(3.4)·`ChordKeys`(2.4)·`HangulComposer`도 고치지 않았다. 한 키·한 방향에 시프트 자모·방향별 자모·배정 기호가 둘 이상 없음을 배치 시험 TL-09로 지킨다.
- 0.21(FSS#105 오너 요청·추가 지시, SRS 0.89 FR-TAP-03·03d·NFR-06·TBD-36, HLD 0.82 5A "두 줄 높이 키 — ↵"): **두 줄 높이 ↵와 빈칸 없는 글자 줄** — `TapKeyDef.rowSpan`(차지하는 줄 수, 기본 1)·`inRow(r)`·`rect(unit, rowHeight)`(키 사각형 식 하나, 3.1), 배치(3.2): 3줄 a~l 0부터 + ↵(x 324 · 폭 36 · 줄 3부터 두 줄, 키 목록에서 l 다음), 4줄 z~m 0부터 · `=` 252 · `/` 288(배정 그대로), 5줄 한/영 72 · 스페이스 216 · `,` 288 · `.` 324. **빈칸 형 `BlankDef`·`TapLayout.blanks`와 `hit`의 빈칸 단계를 지웠다**(D-73, 4절). 그리기·접근성·조합 키·연습 화면은 키 사각형 하나를 그대로 쓰므로 식만 바뀌었다(4·7절). 판정·배정표·`TapKeyResolver`·`TapGestureClassifier`는 고치지 않았다.
- 0.20(FSS#98 오너 지시 "제일위 라인 esc 키 옆에 Ctrl, Alt키를 넣어줘", SRS 0.87 FR-TAP-03·03c·04·13·15~18·조합 키 대응표·NFR-06, HLD 0.80 5A "조합 키 Ctrl·Alt"): **편집 줄 10키**(Esc · Ctrl · Alt · ⇥ · ← · ↑ · ↓ · → · Del · ⌫, 각 36 — 0.19까지 8 × 45. 3.2), 기능키 `FunctionKey.CTRL`·`ALT`(반복 없음, 4·6절), 새 2.4 **조합 키 대응**(`ChordModifier`·`ChordKey`·`ChordStroke`·`ChordKeys`: 글자 94자 → (키, Shift), 키 id + 판정된 동작 → 같은 자리 쿼티 키의 결과, 기능키 → 키), `TapSink.onTapResult(key, out, gesture)`·`TapSink.chord`(4절), `MultiPointerQueue<R>`(판정된 동작을 결과와 함께 실음, 5절), `TapKeyboardView`의 켜짐 바탕·낱말 글자 크기 `WordLabelFit`·`chordChanged()`(4절), 접근성 노드의 켜고 끄는 항목(7절), 시퀀스 8.2. 조합 키 상태와 보내기는 LLD-08 4절·6.3. Ctrl·Alt가 꺼져 있으면 0.19와 모든 길이 같다(`TapKeyResolver`·`TapGestureClassifier`·배정표는 고치지 않았다).
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
| 2 | `TapGestureClassifier`, `TapKeyResolver`, `ScriptTables` (탭 자판 동작 표 판정), `ChordKeys`(조합 키 대응, 2.4) | stroke-core |
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
    data class Exit(val dir: Way) : TapGesture          // 같은 변 1회 통과
    data class ExitReturn(val dir: Way) : TapGesture    // 2회(나갔다 돌아옴) — 결과는 Exit(dir)과 같음(2.2)
    data class Rub(val dir: Way) : TapGesture           // 3회 이상(문지르기)
}   // dir: 4방향 키는 처음 나간 변의 Dir, 8방향 키는 각도로 정한 Dir 또는 Diag(0.23. 0.22까지 Dir)
class TapGestureClassifier(key: KeyGeom, minCrossDp: Float, eightWay: Boolean = false) {   // 0.23: eightWay
    fun begin(p: StrokePoint); fun add(p: StrokePoint); fun finish(p: StrokePoint): TapGesture
    companion object {
        fun wayOfVector(dx: Float, dy: Float): Way      // dp 벡터(y 아래로 증가)의 각도 → wayOfAngle
        fun wayOfAngle(deg: Double): Way                 // 0 ≤ deg < 360, 0° = →, 90° = ↑(시계 반대 방향)
    }
}
```

#### `finish(p): TapGesture`
- 처리: `EdgeCrossingTracker`(LLD-04 3절)로 기록을 얻는다. 기록 없음 → `Tap`. 첫 나감 기록 `out`(변 `e1`), `n = countOf(e1)`: 1 → `Exit`, 2 → `ExitReturn`, ≥3 → `Rub`. 방향 `dir`은 `eightWay`가 아니면 `e1.dir`(0.22까지와 같음), `eightWay`면 `wayOfVector(out.x − 누른 점.x, out.y − 누른 점.y)`다(0.23). `out.x`·`out.y`는 그 나감을 센 표본점이다(`Crossing`이 이미 담고 있음 — LLD-04 3.1, 판정용으로 창 테두리에 맞춘 점이 아니라 받은 점). 누른 점은 `begin(p)`의 `p`.
- **8방향 각도 구간(0.23, SRS FR-TAP-02e ②·③):** `wayOfAngle(deg)` — `deg ≤ 30` → `RIGHT`, `< 60` → `UP_RIGHT`, `≤ 120` → `UP`, `< 150` → `UP_LEFT`, `≤ 210` → `LEFT`, `< 240` → `DOWN_LEFT`, `≤ 300` → `DOWN`, `< 330` → `DOWN_RIGHT`, 그 밖(330 이상) → `RIGHT`. 위아래좌우 구간은 양 끝을 넣고 대각선 구간은 양 끝을 뺀다(경계 = 위아래좌우 쪽). `wayOfVector(dx, dy)` = `wayOfAngle(atan2(−dy, dx)`를 도로 바꿔 음수면 360을 더한 값`)`. 영 벡터는 생기지 않는다(나감은 누른 점에서 `minCrossDp` 이상 움직인 뒤에만 셈).
- **언제 정하나:** 각도는 처음 나감을 센 순간 한 번만 잰다. 그 뒤의 움직임(다른 변 통과, 다른 키 위로 넘어감)은 방향을 바꾸지 않는다(FR-TAP-02e ①). 통과 횟수는 처음 나간 **변**의 것이라 대각선으로 나갔다 돌아오면 `ExitReturn(대각선)`, 같은 대각선으로 문지르면 `Rub(대각선)`이다. 8방향 키는 누른 위치를 판정에 쓴다(FR-SWP-02·04의 예외, SRS FR-TAP-02e ①) — 왼쪽 변 가까이 눌러 위로(115°) 그어 왼쪽 변을 먼저 지나도 ↑다.
- **누가 `eightWay`를 정하나:** 호출자(`TapTouchLogic.down`, 4절)가 키 데이터의 `TapKeyDef.eightWay`(3.1)를 넘긴다. 이 클래스는 키를 모른다. 모드 키·그래피티 뷰의 모드 키는 기본값(4방향)이다.
- 길이·꺾기는 보지 않는다(탭 자판 동작 표에 없음). 같은 `minCrossDp`를 쓴다(FR-TAP-02b).
- 근거: FR-TAP-01a, 02a, 02b, 02e. 시험: TG-01~TG-14(TG-11~14는 8방향, 0.23).

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
             Rub(대각선) → Exit(대각선)                        // 0.23: 처음 나감의 방향을 따름(SRS FR-TAP-02e ④)
2. 정규화한 것이 Exit(대각선 d)면 key.dirJamo[d]의 자모, 없으면 resolve(key, Tap)   // 0.23: 빈 방향 = 탭
3. 아래 표로 출력을 찾는다(남은 것은 Tap, Exit(Dir), Rub(UP), Rub(DOWN)).
```

| 키 종류 \ 동작 | Tap | Exit(UP) | Exit(DOWN) | Exit(LEFT/RIGHT) | Rub(UP) | Rub(DOWN) |
|---|---|---|---|---|---|---|
| `QWERTY_LETTER` | 소문자 | 대문자 | `a.down` ?: 소문자 | `a.left/right` ?: 소문자 | 윗첨자 | 아래첨자 |
| `DUBEOLSIK_LETTER` | 기본 자모 | 시프트 자모(ㄲ ㄸ ㅃ ㅆ ㅉ ㅒ ㅖ) ?: `dirJamo[UP]`(ㄵ ㄺ, ㅟ) ?: 기본 | `dirJamo[DOWN]`(ㄿ, ㅚ ㅢ) ?: `a.down` ?: 기본 | `dirJamo[LEFT/RIGHT]`(← ㄻ · → ㄳ ㄶ ㅀ ㅄ, → ㅘ · ← ㅝ) ?: `a.left/right` ?: 기본 | 기본 | 기본 |
| `NUMBER_ROW`, `SYMBOL` | `a.tap` | `a.up` | `a.down` ?: `a.tap` | `a.left/right` ?: `a.tap` | `a.tap` | `a.tap` |

- 윗첨자·아래첨자가 유니코드에 없으면 일반 글자 + `errorVibe = true`(FR-TAP-01b).
- 두벌식 방향별 자모(`dirJamo` = 겹받침 + 겹모음, 3.3 두벌식 겹받침 배정표·겹모음 배정표, FR-TAP-02c·02g. 0.22~0.25의 짝 모음은 0.26에서 뺌, FSS#141)는 **나감**에만 있다. 1번 정규화로 나갔다 돌아옴과 좌우 문지르기는 그 방향 나감과 같고, ↑·↓ 문지르기(`Rub(UP)`·`Rub(DOWN)`)는 겹받침이 있는 키에서도 기본 자모다(ㄴ ↑ ㄵ, ㄹ ↑ ㄺ · ↓ ㄿ도 같은 규칙. ㄹ ← · → 문지르기는 ㄻ · ㅀ — 0.23, SRS FR-TAP-02e ④, PM 결정). 겹받침과 시프트 자모가 같은 키의 ↑에 함께 있는 일은 없고(ㄴ·ㄹ은 시프트 자모가 없고 ㄱ·ㅂ의 겹받침은 →), 겹받침과 배정 기호가 같은 방향에 함께 있는 일도 없다(겹받침 키 r·s·f·q는 `assign`이 없다).
- **대각선(0.23, FSS#109):** 대각선 방향은 8방향 키(`eightWay`, 지금은 두벌식 f·h·n — 0.24에 ㅗ·ㅜ, FSS#110)의 획에서만 나온다. 2번 단계는 키 종류를 보지 않는다 — 대각선 `dirJamo`가 있으면 그 자모, 없으면 탭과 같은 결과다(ㄹ ↘ = ㄹ). 그래서 대각선을 받은 4방향 키(Ctrl·Alt가 쿼티 f에 넘기는 ㄹ의 대각선, 2.4)도 탭 결과(f)다. 대각선 문지르기는 1번에서 대각선 나감이 된다(대각선 문지르기를 따로 두지 않음, SRS FR-TAP-02e ④).
- **빈 방향 = 탭(0.23, SRS FR-TAP-02c ④):** 배정이 없는 방향의 나감·나갔다 돌아옴은 모든 탭 자판 글자 키에서 탭과 같은 출력이고 `errorVibe = false`다(위 표의 "?: 기본"·"?: 소문자"·"?: `a.tap`", 2번 단계). 0.22까지도 네 방향은 그랬고(동작 표 "배정 없음 = 탭과 같음") 0.23은 대각선에도 같게 했다. 오류 진동은 없는 윗첨자·아래첨자(FR-TAP-01b)에만 있다. 시험 TA-24. 짝 모음(0.22, FSS#104)도 같은 판정 길이다: ㅗ ↑ ㅛ · ㅓ ← ㅕ · ㅏ → ㅑ · ㅜ ↓ ㅠ는 나감·나갔다 돌아옴에서 나오고, ㅓ ← · ㅏ → 문지르기는 1번 정규화로 그 방향 나감이라 ㅕ · ㅑ, ㅗ ↑ · ㅜ ↓ 문지르기는 기본 모음 ㅗ · ㅜ다(SRS TBD-37 ① 잠정). 짝 모음 키 h·j·k·n에는 시프트 자모·겹받침·`assign`이 없다. 그래서 찾는 순서(↑ = 시프트 자모 → `dirJamo` → 기본, 그 밖 = `dirJamo` → 배정 기호 → 기본)는 지금 데이터에서 결과를 바꾸지 않는다(시험 TL-09).
- 1번 정규화는 SRS 동작 표의 "← · → 문지르기"·"나갔다 돌아옴(같은 변 2번 통과)" 행이다(SRS 3.3 탭 자판 동작 표, DCR-02 결정). 예: a ↑ 나갔다 돌아옴 = A, i ← 문지르기 = `[`, i → 나갔다 돌아옴 = i(→ 배정 없음). 그래서 모든 획이 출력을 하나 낸다(입력 없는 결과가 없다).
- `TapGestureClassifier`는 물리 동작을 그대로 구분하고(시험 TG), 결과 규칙은 이 함수에만 둔다.
- 배정 기호(`text`)를 넣는 경로와 자모(`jamo`)를 넣는 경로는 호출자(`InputDispatcher`, LLD-08 6절)가 나눈다. 두벌식 조합 중 기호·숫자면 먼저 확정한다(FR-TAP-11a).
- 근거: FR-TAP-01a, 01b, 02a, 02c, 02e, 02g, 03a, 11, FR-SWP-44, SRS 탭 자판 동작 표. 시험: TK-01~TK-24(표의 칸마다 하나), TA-01~TA-12, TA-21~TA-25.

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

### 2.4 조합 키 대응 ChordKeys (0.20, FSS#98)

Ctrl·Alt가 켜진 채 누른 키가 보내는 키(SRS 조합 키 대응표, FR-TAP-16)를 정하는 순수 함수다. 안드로이드가 없다 — 키는 PC 자판(미국 배열)의 키 이름이고, 안드로이드 키 코드로 바꾸는 것은 `AndroidEditorPort`가 한다(LLD-08 4절).

```kotlin
// kbd.stroke.tap
enum class ChordModifier { CTRL, ALT, SHIFT }        // 선언 순서 = 누름 입력을 보내는 순서(FR-TAP-16 ④)
enum class ChordKey {                                // PC 자판의 키. 이름은 안드로이드 키 코드 이름과 같다(D0~D9 = KEYCODE_0~9)
    A, B, …, Z,  D0, D1, …, D9,
    GRAVE, MINUS, EQUALS, LEFT_BRACKET, RIGHT_BRACKET, BACKSLASH, SEMICOLON, APOSTROPHE, COMMA, PERIOD, SLASH,
    SPACE, ENTER, TAB, DPAD_LEFT, DPAD_UP, DPAD_DOWN, DPAD_RIGHT, ESCAPE, FORWARD_DEL, DEL,
}
data class ChordStroke(val key: ChordKey, val shift: Boolean)   // 그 글자를 치는 키와 Shift 여부
object ChordKeys {
    val CHARS: Map<Char, ChordStroke>                 // 글자 94자
    fun ofChar(c: Char): ChordStroke?
    fun ofTap(keyId: String, g: TapGesture): ChordStroke
    fun ofFunction(f: FunctionKey): ChordKey?
}
```

- `ChordModifier.SHIFT`에는 키가 없다. Ctrl·Alt에 딸려서만 실린다(FR-TAP-16 ②).
- **`CHARS`** — 공백을 뺀 인쇄 가능 ASCII 94자(`!`~`~`) 전부다.

| 글자 | `ChordStroke` |
|---|---|
| `a`~`z` | (`A`~`Z`, Shift 없음) |
| `A`~`Z` | (`A`~`Z`, Shift) |
| `0`~`9` | (`D0`~`D9`, Shift 없음) |
| `) ! @ # $ % ^ & * (` | (`D0`~`D9` 차례로, Shift) |
| `` ` `` `-` `=` `[` `]` `\` `;` `'` `,` `.` `/` | (`GRAVE` `MINUS` `EQUALS` `LEFT_BRACKET` `RIGHT_BRACKET` `BACKSLASH` `SEMICOLON` `APOSTROPHE` `COMMA` `PERIOD` `SLASH`, Shift 없음) |
| `~` `_` `+` `{` `}` `\|` `:` `"` `<` `>` `?` | (위와 같은 차례의 키, Shift) |

- **`ofChar(c)`** — `CHARS[c]`. 표에 없는 글자(공백, 한글 자모, 윗첨자·아래첨자)는 null.
- **`ofTap(keyId, g)`** — 글자·숫자·기호 키.
  ```
  key = TapLayouts.QWERTY에서 id가 keyId이고 FUNCTION이 아닌 키        // 없으면 IllegalArgumentException(호출자 잘못)
  g' = if g가 Rub(UP) 또는 Rub(DOWN) then Tap else g                     // 윗첨자·아래첨자는 PC 자판에 없다 → 그 글자 키(FR-TAP-16 ②)
  text = TapKeyResolver.resolve(key, g').text                           // 2.2 동작 표의 쿼티 결과(나갔다 돌아옴·좌우 문지르기 정규화 포함)
  return CHARS[text의 한 글자]                                           // 없으면 IllegalStateException
  ```
  - **두벌식도 같은 함수다.** 쿼티·두벌식 배치는 키 자리마다 id가 같으므로(3.2, 시험 "DUBEOLSIK letters…") 키 id만 받으면 "같은 자리의 쿼티 키"가 된다. 두벌식 키의 자모·시프트 자모·겹받침은 보지 않는다 — ㅂ(q) ↑ = Shift + Q, ㄹ(f) ←(겹받침 ㄻ 자리) = 쿼티 f ←는 배정이 없어 F, ㄹ(f) ↑(ㄺ 자리) = Shift + F, ㅁ(a) ↑(두벌식에서는 탭과 같은 ㅁ) = Shift + A(FR-TAP-16 ③).
  - **ㄹ의 대각선(0.23, FSS#109, SRS FR-TAP-02e ⑧):** 뷰는 두벌식 f의 획을 8방향으로 판정해 `Exit/ExitReturn/Rub(Diag)`를 넘긴다. 쿼티 f에는 대각선 배정이 없으므로 `resolve`의 2번 단계(2.2)가 탭 결과 f를 내고 → `F`(Shift 없음)다. 대각선 `Rub`는 위 `g'` 단계에서 바뀌지 않고(↑·↓가 아님) `resolve`에서 대각선 나감 → f → `F`. ↑ 구간 나감 = Shift + `F`, ←·→·↓ 구간 = `F`, ↑·↓ 구간 문지르기 = `F`(위 `g'`). 이 함수는 고치지 않았다(시험 CM-14).
  - 동작의 판정은 다시 하지 않는다. 뷰가 한 번 판정한 `TapGesture`(2.1)를 받아 쿼티 키의 결과 표를 한 번 더 찾을 뿐이다(순수 표 찾기).
  - 쿼티 배치의 모든 글자·숫자·기호 키(40개) × 모든 동작(탭 + 네 방향의 `Exit`·`ExitReturn`·`Rub` = 13가지)의 결과가 `CHARS`에 있다(대각선 넷의 세 동작도 있다 — 탭 결과, CM-14). 기호 배정(3.3)이 바뀌어 표에 없는 글자가 나오게 되면 시험 CM-10이 알린다. 실행 중에 없으면 예외이고 `EngineGuard`가 그 키 하나를 입력 없이 끝낸다(LLD-09 2절. 조합 키는 이미 꺼진 채 — LLD-08 6.3).
- **`ofFunction(f)`** — 기능키는 표 없이 키 하나씩이고 Shift가 없다(FR-TAP-16 ②): `SPACE` → `SPACE`, `ENTER` → `ENTER`, `TAB` → `TAB`, `ARROW_LEFT/UP/DOWN/RIGHT` → `DPAD_LEFT/UP/DOWN/RIGHT`, `ESC` → `ESCAPE`, `DELETE` → `FORWARD_DEL`, `BACKSPACE` → `DEL`. `MODE`·`CTRL`·`ALT` → null(대상 키가 아님, FR-TAP-16 ①).
- 근거: SRS FR-TAP-16 ①~③, FR-TAP-02e ⑧, FR-TAP-02g ⑦, 조합 키 대응표, HLD 5A "키 대응". 시험: CM-01~CM-15(LLD-11 5.6).

## 3. 자판 배치 데이터

### 3.1 타입

```kotlin
enum class TapKeyKind { QWERTY_LETTER, DUBEOLSIK_LETTER, NUMBER_ROW, SYMBOL, FUNCTION }
data class KeyAssign(val tap: String, val up: String? = null, val left: String? = null,
                     val right: String? = null, val down: String? = null)
data class TapKeyDef(val id: String, val row: Int, val x: Float, val w: Float,    // x·w는 360dp 기준 dp, row = 맨 위 줄
                     val kind: TapKeyKind, val label: String,
                     val assign: KeyAssign? = null,
                     val jamo: Char? = null, val shiftJamo: Char? = null,      // 두벌식 글자 키(호환 자모 문자)
                     val function: FunctionKey? = null,
                     val dirJamo: Map<Way, Char> = emptyMap(),                 // 두벌식 글자 키: 방향별 자모 = 겹받침 + 겹모음(3.3, 0.22. 0.21까지 compoundJamo. 0.24: 겹모음. 0.26: 짝 모음 뺌). 0.23: 대각선도(Way)
                     val rowSpan: Int = 1) {                                   // 0.21: 차지하는 줄 수(↵ 2, 그 밖 1). 1 미만이면 require 실패
    val eightWay: Boolean get() = dirJamo.keys.any { it is Diag }              // 0.23: 대각선 배정이 있는 키 = 8방향 판정(2.1)
    fun inRow(r: Int): Boolean = r >= row && r < row + rowSpan                 // 줄 r에 드는가
    fun rect(unit: Float, rowHeight: Float): RectDp =                          // 키 사각형 — 모든 키·모든 곳의 식 하나
        RectDp(x * unit, row * rowHeight, (x + w) * unit, (row + rowSpan) * rowHeight)
}
data class TapLayout(val name: String, val rows: Int, val keys: List<TapKeyDef>)   // 0.21: 빈칸 목록 없음(D-73)
object TapLayouts { val QWERTY: TapLayout; val DUBEOLSIK: TapLayout }
```

- 화면 좌표: `x_px = x / 360 × 폭`, 키의 위 끝 = 창 위에서 `row × 줄 높이`, 아래 끝 = `(row + rowSpan) × 줄 높이`(쿼티·두벌식은 6줄로 창 전체). 가로 화면에서는 폭만 늘고 줄 높이는 그대로(FR-CMN-12).
- **키 사각형 식 하나(0.21, FSS#105, HLD 5A "기하"):** 키 사각형은 `TapKeyDef.rect(unit, rowHeight)` 하나로만 만든다(`unit` = 뷰 폭 / 360). `TapGeometry`(4절), 시험 도우미(키 가운데 y = `(row + rowSpan / 2) × 줄 높이`)가 이 식을 쓴다. 0.20까지 아래 끝을 `row + 1`로 쓰던 곳(`TapGeometry`, 빈칸 사각형, 시험 도우미 5곳)을 모두 바꿨다 — 남으면 ↵가 한 줄로 줄어든다. 연습 화면·`KeyboardRootView`는 키 사각형을 따로 만들지 않고(`TapKeyboardView`를 그대로 씀) 줄 수 합(`layout.rows` = 6)만 쓰므로 바뀌지 않는다. 키 사각형 하나가 `KeyGeom` 하나라 두 줄 키의 줄 경계는 변이 아니다.
- **8방향 키는 데이터로 정한다(0.23, FSS#109, HLD 5A "겹받침 방향 규칙과 8방향 키"):** `eightWay`는 `dirJamo`에 대각선(`Diag`)이 하나라도 있는가다. 판정(2.1)·결과(2.2)·표시(3.4)·Ctrl·Alt(2.4) 코드는 키 id·자모·자모 종류(겹받침인지)로 갈래를 두지 않는다. 그래서 배치 데이터에 대각선 배정을 더하면(예: FSS#110의 ㅗ ↘ · ㅜ ↙ 겹모음 — `COMPOUND_VOWEL` 표의 대각선 칸) 코드 변경 없이 그 키도 8방향으로 판정되고 모서리에 표시된다(시험 TL-10). 지금 8방향 키는 두벌식 f(ㄹ)·h(ㅗ)·n(ㅜ)이고(0.24 — FSS#110의 `COMPOUND_VOWEL` 대각선 ㅙ·ㅞ 때문, 코드는 바꾸지 않음. 0.26에 짝 모음을 빼도 그대로), ㅡ(m)와 ㅓ(j)·ㅏ(k)는 대각선이 없어 4방향, 쿼티에는 없다.
- "불가" 방향은 데이터에 넣지 않는다. 그 방향은 바깥 변이라 기록이 생기지 않는다(LLD-03 3.3).
- **여유 칸(0.17, FSS#84):** `TapGeometry.build(layout, widthDp, rowHeightDp, sideDp)`가 키마다 `KeyGeom.build(id, rect, widthDp, heightDp, sideDp)`를 부른다(LLD-04 2절). `sideDp` = 뷰의 `sideMarginPx / density`(부모가 재기 전에 알림, LLD-08 7.2). 그래서 왼쪽·오른쪽 끝 키의 바깥쪽 변은 바깥 변이 아니고, 그 방향 획은 변 너머 `minCrossDp`에서 `Exit`이다(안쪽 키와 같은 기준). 그 방향에 배정이 없는 키는 결과가 탭과 같다(`TapKeyResolver`, 그대로). `TapKeyboardView.rebuildGeometry`는 배치·폭·줄 높이·`sideDp`가 모두 같을 때만 다시 만들지 않는다. 여유 칸은 뷰 밖이라 거기서 내려온 터치는 뷰에 오지 않는다(입력 없음). 0.21부터 들여쓰기 빈칸이 없어(FSS#105) a·z도 q·p처럼 여유 칸에 바로 닿고, 끝 키의 바깥쪽 획은 여유 칸 안의 보통 변 통과다.

### 3.2 쿼티 배치 (SRS 3.3 그림, FR-TAP-03)

| 줄 | 키 (폭 dp) |
|---|---|
| 0 편집 줄 | Esc ESC(36) · Ctrl CTRL(36) · Alt ALT(36) · ⇥ TAB(36) · ← (36) · ↑ (36) · ↓ (36) · → (36) · Del DELETE(36) · ⌫ BACKSPACE(36) |
| 1 숫자 줄 | 1 2 3 4 5 6 7 8 9 0 NUMBER_ROW(각 36) |
| 2 | q w e r t y u i o p (각 36) |
| 3 | a s d f g h j k l (각 36, x 0~288) · ↵ ENTER(36, x 324, **줄 3·4 두 줄**, `rowSpan` 2) |
| 4 | z x c v b n m (각 36, x 0~216) · `=` SYMBOL(36, x 252) · `/` SYMBOL(36, x 288) · (↵가 x 324~360을 차지) |
| 5 기능키 줄 | 한/영 MODE(72) · 스페이스(216) · `,` SYMBOL(36, x 288) · `.` SYMBOL(36, x 324) |

- 키 목록 순서(접근성 노드 순서, 7절): 편집 줄 → 숫자 줄 → q~p → a~l → **↵** → z~m → `=` → `/` → 한/영 → 스페이스 → `,` → `.`(0.21: ↵는 맨 위 줄인 3줄의 끝, SRS FR-TAP-03d ⑦·TBD-36 ④). 0.20까지는 3줄 빈칸 18 + 9 × 36 + 빈칸 18, 4줄 빈칸 18 + 7 × 36 + `=` 36 + ↵ 54, 5줄 72 · 180 · 36 · 36 · `/` 36.

- 두벌식은 같은 자리에 2줄 ㅂㅈㄷㄱㅅㅛㅕㅑㅐㅔ, 3줄 ㅁㄴㅇㄹㅎㅗㅓㅏㅣ, 4줄 ㅋㅌㅊㅍㅠㅜㅡ. `shiftJamo`: ㅂ→ㅃ, ㅈ→ㅉ, ㄷ→ㄸ, ㄱ→ㄲ, ㅅ→ㅆ, ㅐ→ㅒ, ㅔ→ㅖ, 나머지 null(기본 자모).
- 모드 키는 맨 아랫줄 왼쪽(SRS 3.3, 모든 모드 공통). 시험 TL-01(폭 합 = 360, 모든 줄), TL-02(NFR-06 크기), TL-03(모드 키 위치).
- ⌫는 편집 줄 오른쪽 끝, ↵는 가운데 글자 줄·아랫글자 줄의 오른쪽 끝에 걸친 두 줄 높이 키다(0.10, 이슈 #198 — 그때는 아랫글자 줄 한 줄, 폭 54. 0.21 FSS#105로 두 줄 · 폭 36). 키 id는 `esc`·`ctrl`·`alt`·`delete`·`minus`·`slash`, 키 라벨은 `Esc`·`Ctrl`·`Alt`·`Del`.
- **편집 줄 10키(0.20, FSS#98):** Esc 바로 오른쪽에 Ctrl, 그 오른쪽에 Alt를 두고 열 키 모두 글자 키와 같은 폭 36이다(한 줄 10칸, 빈칸 없음. 0.19까지 8 × 45). 쿼티·두벌식이 같은 데이터다. 편집 줄 키의 x는 바로 아래 숫자 줄 키의 x와 같다. 한 줄 10칸이라 여유 칸(LLD-08 7.2), 줄인 자판, 띄운 자판(7.4)에서 따로 바뀌는 것이 없다(SRS FR-TAP-03c). 시험 TL-06.
- **NFR-06 예외(0.20, FSS#98. 0.10의 45dp 예외를 바꿈):** 편집 줄 기능키 10개는 글자 키와 같은 폭(배치 단위 36, 360dp 화면 꽉 채움에서 여유 칸 때문에 약 32.7dp)으로 NFR-06의 기능키 48dp보다 좁다. 오너 지시로 Ctrl·Alt를 더한 결과이고 SRS NFR-06이 이 예외를 적는다(잠정, 오너 확인 전 — SRS TBD-34 ⑤). TL-02는 편집 줄 기능키에 36을 하한으로 쓴다.
- **빈칸 없음(0.21, FSS#105, D-73):** 모든 줄이 0~360을 키로 틈·겹침 없이 채운다 — 줄 r에 드는 키(`inRow(r)`, ↵는 3·4줄 모두)의 (x, 폭)을 x 차례로 이으면 0에서 360까지 빈틈이 없다(시험 TL-01). `rowSpan` ≥ 1, `row + rowSpan` ≤ `rows`. 0.20까지의 들여쓰기 빈칸 3개(3줄 양쪽, 4줄 왼쪽, 누르면 같은 줄 가장 가까운 키)는 없어졌고 그 자리는 a·z(반 칸 왼쪽으로 옮겨 옴)와 ↵다. 0.10에서 맨 아랫줄의 빈칸은 이미 없앴다.
- **두 줄 높이 ↵(0.21, FSS#105, SRS FR-TAP-03d):** ↵ 키 정의는 **하나**(`id` "enter", row 3, `rowSpan` 2, x 324, 폭 36)다. 같은 동작의 키 둘(3줄·4줄)로 나누지 않으므로 누름·그리기·눌림 표시·접근성 노드가 하나다(HLD 12장 결정). 두 줄 키는 ↵뿐이고 다른 키는 모두 `rowSpan` 1이다(시험 TL-07). 쿼티·두벌식이 같은 데이터다.
- **`/` 키 자리(0.21):** `/`(`id` "slash", 배정 탭 `/` · ↑ `?`, 0.10 그대로)는 4줄 x 288(`=`와 ↵ 사이)로 옮겼다. 맨 아랫줄의 빈 36은 스페이스를 216으로 넓혀 채운다(PM 기본안, 잠정 — SRS TBD-36 ①). 오너가 다른 안을 고르면 이 표의 데이터만 바뀐다.
- **NFR-06 예외(0.21, FSS#105):** ↵는 폭 36(꽉 채움에서 약 32.7dp) · 두 줄 높이로 기능키 폭 48 기준보다 좁다(SRS NFR-06 예외, 잠정 TBD-36 ②). TL-02는 ↵에 폭 36 하한과 `rowSpan` 2를 본다.

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

**두벌식 겹받침 배정표 (0.23, FSS#109, SRS FR-TAP-02c ①의 규칙과 배정표·규칙 대조표. 정본은 SRS의 규칙)** — `TapLayouts.COMPOUND_JAMO: Map<Char, Map<Way, Char>>`(앞 자음 → 방향 → 겹받침, `internal` — 시험 TL-11이 읽음)가 아래 짝 모음 표와 합쳐 두벌식 글자 키의 `TapKeyDef.dirJamo`가 된다. 쿼티에는 없다(빈 맵).

- **규칙(정본, 예외 없음):** 겹받침은 **앞 자음 키에만** 하나 두고, 방향 = `(sign(뒤 자음 키 열 − 앞 자음 키 열), sign(뒤 자음 키 줄 − 앞 자음 키 줄))`, 열 = `TapKeyDef.x ÷ 36`(정수, 왼쪽 끝 0), 줄 = `TapKeyDef.row`. 이 부호 쌍이 `Way.dx`·`Way.dy`이고 `wayOfSigns(dx, dy)`(LLD-03 3.3)가 방향을 돌려준다. 표는 이 규칙을 지금 배치(3.2, 빈칸 없음 — FSS#105)에 적용한 결과이고, 시험 TL-11이 `TapLayouts.DUBEOLSIK`의 좌표에서 규칙으로 다시 계산해 `COMPOUND_JAMO`와 같은지 본다(표를 시험에 다시 적지 않음). 배치가 바뀌면 TL-11이 어긋남을 알린다.
- 계산(앞 자음 키 (열, 줄) → 뒤 자음 키 (열, 줄)): ㄹ f(3, 3) → ㄱ r(3, 2) ↑ ㄺ, ㅁ a(0, 3) ← ㄻ, ㅂ q(0, 2) ↖ ㄼ, ㅅ t(4, 2) ↗ ㄽ, ㅌ x(1, 4) ↙ ㄾ, ㅍ v(3, 4) ↓ ㄿ, ㅎ g(4, 3) → ㅀ / ㄱ r(3, 2) → ㅅ t(4, 2) → ㄳ / ㄴ s(1, 3) → ㅈ w(1, 2) ↑ ㄵ, ㅎ g(4, 3) → ㄶ / ㅂ q(0, 2) → ㅅ t(4, 2) → ㅄ.

"–"는 배정 없음(탭과 같은 기본 자모, 2.2), ↑의 ㄲ·ㅃ·ㅆ는 `shiftJamo`(겹받침 아님), 굵은 글자가 겹받침이다. ㅅ(t)·ㅌ(x)·ㅍ(v)는 0.23부터 겹받침이 없다(↑ ㅆ만 남은 ㅅ, 모든 방향이 기본인 ㅌ·ㅍ).

| 키(id) | tap | ↑ | ← | → | ↓ | ↖ | ↗ | ↙ | ↘ |
|---|---|---|---|---|---|---|---|---|---|
| ㄱ (`r`) | ㄱ | ㄲ | – | **ㄳ** | – | | | | |
| ㄴ (`s`) | ㄴ | **ㄵ** | – | **ㄶ** | – | | | | |
| ㄹ (`f`) | ㄹ | **ㄺ** | **ㄻ** | **ㅀ** | **ㄿ** | **ㄼ** | **ㄽ** | **ㄾ** | – |
| ㅂ (`q`) | ㅂ | ㅃ | – | **ㅄ** | – | | | | |

- 대각선 칸은 8방향 키(ㄹ)에만 있다. 다른 키는 4방향 판정이라 대각선이 나오지 않는다(빈칸).
- 지난 배정: 0.17까지 ㄱ ← ㄳ, ㄴ ← ㄵ · → ㄶ, ㄹ ↑ ㅀ · ← ㄺ · → ㄻ · ↓ ㄼ, ㅂ → ㅄ(0.14 ←), ㅅ ← ㄽ, ㅌ ← ㄾ, ㅍ ← ㄿ. 0.18~0.22 ㄱ ↓ ㄳ, ㄴ ↑ ㄵ · ↓ ㄶ, ㅂ ↓ ㅄ, ㅅ ↓ ㄽ, ㅍ ↑ ㄿ, ㅌ ← ㄾ(0.22 ↑ ㄾ), ㄹ은 0.17과 같음(FSS#97·#107). 0.23에서 ㄵ(ㄴ ↑)만 그대로이고 나머지 열 개는 키나 방향이 바뀌었다. 옮겨서 비게 된 방향(ㄱ ↓, ㄴ ↓, ㅂ ↓, ㅅ ↓, ㅌ ↑, ㅍ ↑)은 기본 자모다.
- 겹받침 11개는 각각 한 키·한 방향에만 있다(시험 TL-09·TL-11·TA-21). 출력은 자모(`TapOutput.jamo`)라 조합기로 간다(LLD-04 7절, FR-HGL-02a, 시험 ID-40·ID-42). 시험 TA-12·TA-20·TA-21·TA-23·TA-24, TK-05·TK-08·TK-11·TK-20·TK-23(LLD-11 5.2·5.3).

**두벌식 짝 모음 배정표** — 0.26에서 뺐다(FSS#141, SRS FR-TAP-02d). `TapLayouts.PAIR_VOWEL`(0.22~0.25)은 없다. ㅗ ↑ · ㅓ ← · ㅏ → · ㅜ ↓는 배정 없음이라 탭과 같은 기본 모음이고 진동이 없다(2.2 빈 방향). ㅛ·ㅕ·ㅑ·ㅠ 키(y·u·i·b)와 i의 ← `[` · → `]`는 그대로다.

- **합치기:** 두벌식 글자 키를 만들 때 `dirJamo = COMPOUND_JAMO[jamo] + COMPOUND_VOWEL[jamo]`(없으면 빈 맵, 0.24에 겹모음 표, 0.26에 짝 모음 표를 뺌)이다. 두 표가 같은 자모의 같은 방향을 가지면 배치를 만들 때 `require`로 실패한다(지금 겹받침 표와 겹모음 표의 자모는 겹치지 않는다). 두 표를 따로 둔 것은 뜻이 다른 배정을 한 표에 섞지 않으려는 것이다(HLD 5A ①).
- **겹침 없음(시험 TL-09):** 두벌식 글자 키 26개 × 여덟 방향(0.23. 0.22까지 네 방향)마다 시프트 자모(↑만)·`dirJamo`·`assign`의 기호가 많아야 하나이고, `dirJamo` 값 전체는 겹받침 11개 + 겹모음 7개(0.24. 0.25까지 짝 모음 4개도)가 한 번씩이다. 어느 키의 `dirJamo`에도 ㅛ·ㅕ·ㅑ·ㅠ가 없다(시험 TA-22). 쿼티 키의 `dirJamo`는 모두 비어 있다.
- **옛 짝 모음 자리:** ㅓ·ㅏ는 `dirJamo`가 비어 모든 방향이 탭이고 4방향 키다. ㅗ ↑ · ㅜ ↓는 겹모음 표에도 없어 탭과 같은 기본 모음이다. 출력은 키로 친 ㅗ·ㅓ·ㅏ·ㅜ와 같은 자모라 조합도 같다(ㄱ ㅗ↑ ㅏ = 과, 각 + ㅓ← = 가거 — LLD-04 7절). 시험 TA-21, TA-22, TA-24, TL-09, CM-13, ID-41, PV-01(LLD-11 5.3·5.4·5.6·5.8·7.4).
- **처음 누른 키:** ㅜ(n)는 아랫글자 줄이라 ↓로 그으면 손가락이 맨 아랫줄 스페이스 위로 넘어가지만, 결과는 처음 누른 키의 ↓ = ㅜ다(4절 판정 그대로, 시험 PV-01. 0.25까지 ㅠ).
- 0.24(FSS#110): ㅗ·ㅜ 키의 "–" 가운데 ㅗ → · ↓, ㅜ ← · ↑는 아래 겹모음 배정표의 겹모음이다.

**두벌식 겹모음 배정표 (0.24, FSS#110, SRS FR-TAP-02g ①의 원리와 두벌식 겹모음 배정표·겹모음 원리 대조표. 정본은 SRS의 원리)** — `TapLayouts.COMPOUND_VOWEL: Map<Char, Map<Way, Char>>`(키의 모음 → 방향 → 겹모음, `internal` — 시험 TL-12가 읽음). 쿼티에는 없다.

- **원리 둘(나란히, HLD 5A):** **모음은 덧붙는 획이 뻗는 방향으로 긋는다. 자음 겹받침은 뒤 자음 키의 위치를 따른다**(위 겹받침 배정표의 규칙). 모음 키는 자판에서 획 모양과 맞는 자리에 있지 않으므로(ㅜ(n, 열 5·줄 4)에서 ㅓ(j, 열 6·줄 3)는 ↗) 겹받침 규칙을 모음에 쓰지 않는다(시험 TL-12가 이 차이도 확인).
- **원리(정본, 예외 없음):** 겹모음 = 키의 모음 + 덧붙는 획(ㅘ = ㅗ + ㅏ, ㅚ = ㅗ + ㅣ, ㅙ = ㅗ + ㅏ + ㅣ, ㅝ = ㅜ + ㅓ, ㅟ = ㅜ + ㅣ, ㅞ = ㅜ + ㅓ + ㅣ, ㅢ = ㅡ + ㅣ). 획 방향 벡터 ㅏ = (1, 0), ㅓ = (−1, 0), ㅣ = (0, 1)(오른쪽·아래가 +, 겹받침 규칙과 같은 부호)를 더해 `(sign(합 x), sign(합 y))`를 `wayOfSigns`로 방향으로 바꾼다. **그 방향이 그 키 모음 자신의 짧은 획 방향(ㅗ ↑, ㅜ ↓ — ㅡ는 없음)이면 세로 부호를 뒤집는다**(ㅜ + ㅣ = ↓는 ㅜ의 획 방향 → ↑ ㅟ. 0.25까지 "짝 모음 자리(`PAIR_VOWEL[키]`의 방향)"로 적었고 같은 방향이라 결과 같음, 0.26 FSS#141). 표는 이 원리의 결과이고, 시험 TL-12가 원리로 다시 계산해 `COMPOUND_VOWEL`과 같은지 본다(표를 시험에 다시 적지 않음 — TL-11과 같은 방식).
- 계산: ㅘ ㅗ (1, 0) → / ㅚ ㅗ (0, 1) ↓(ㅗ의 획은 ↑라 그대로) / ㅙ ㅗ (1, 1) ↘ / ㅝ ㅜ (−1, 0) ← / ㅟ ㅜ (0, 1) = ㅜ의 획 방향 → (0, −1) ↑ / ㅞ ㅜ (−1, 1) ↙ / ㅢ ㅡ (0, 1) ↓.

| 키(id) | tap | ↑ | ← | → | ↓ | ↖ | ↗ | ↙ | ↘ |
|---|---|---|---|---|---|---|---|---|---|
| ㅗ (`h`) | ㅗ | – | – | **ㅘ** | **ㅚ** | – | – | – | **ㅙ** |
| ㅜ (`n`) | ㅜ | **ㅟ** | **ㅝ** | – | – | – | – | **ㅞ** | – |
| ㅡ (`m`) | ㅡ | – | – | – | **ㅢ** | | | | |

- "–"는 배정 없음(탭과 같은 기본 모음, 진동 없음 — 2.2 빈 방향. ㅗ ↑ · ㅜ ↓는 0.25까지 괄호 짝 모음 ㅛ · ㅠ), 굵은 글자가 겹모음이다. ㅡ는 대각선 배정이 없어 4방향 키라 대각선 칸이 비어 있다. 일곱 겹모음은 저마다 한 키·한 방향이다(시험 TL-09·TL-12).
- **판정은 데이터로:** ㅙ(↘)·ㅞ(↙)가 대각선이라 ㅗ·ㅜ의 `eightWay`가 참이 되고 `TapTouchLogic.down`이 그 값으로 분류기를 만든다(2.1, 4절 — 코드 그대로). 그래서 ㅗ·ㅜ의 ↑·↓·←·→(ㅗ·ㅚ·ㅘ, ㅟ·ㅜ·ㅝ)도 0.24부터 각도 구간(위아래좌우 60°, 대각선 30°, 경계는 위아래좌우 쪽)이다. 키 이름·자모 종류(모음인지)로 갈래를 두는 코드는 없다.
- **결과·문지르기:** `TapKeyResolver`(2.2) 그대로 — ↑는 `shiftJamo ?: dirJamo[UP] ?: 기본`(ㅜ ↑ ㅟ), ←·→·↓는 `dirJamo`, 대각선은 2번 단계, ↑·↓ 문지르기 = 기본 모음, ←·→·대각선 문지르기 = 그 나감(ㅗ → 문지르기 = ㅘ, ㅗ ↘ 문지르기 = ㅙ). 시험 TA-25.
- **처음 누른 키, 손 뗀 키 우선 없음:** ㅗ에서 →로 그어 ㅓ·ㅏ 위에서 떼도 ㅘ, ㅜ·ㅡ를 ↓로 그어 스페이스 위로 넘어가도 ㅠ·ㅢ다(4.1, 시험 GV-02).
- **출력:** 자모 하나(`TapOutput.jamo` = ㅘ 등)다. 뷰·판정은 자모 하나만 넘기고, 입력 단위를 획 단계로 나누는 것은 조합기의 두벌식 정책이다(LLD-04 7.4, SRS FR-HGL-04f). 시험 ID-43.
- **Ctrl·Alt:** `ChordKeys.ofTap(id, gesture)`(2.4)가 같은 id의 쿼티 키(h·n·m)로 판정하므로 바꿀 것이 없다 — ↑ 구간 나감·돌아옴 = Shift + `H`·`N`·`M`, 나머지 방향·대각선·문지르기 = `H`·`N`·`M`(SRS FR-TAP-02g ⑦). 시험 CM-15.

### 3.4 모서리 표시 (FR-TAP-03b)

- `TapKeyResolver.edgeHints(key): Map<Way, String>`(0.23. 0.22까지 `Map<Dir, String>`)으로 그린다. 여덟 방향(`WAYS` = `Dir` 넷 + `Diag` 넷) 각각의 `resolve(Exit(w))` 가운데 **탭과 다른 것**만 담는다(0.13, 오너 #198·#208: 그어서 나오는 모든 글자 — 쿼티 ↑ 대문자, 두벌식 시프트 자모(ㅃ ㅉ ㄸ ㄲ ㅆ ㅒ ㅖ)·겹받침·겹모음(0.25까지 짝 모음도), 숫자 줄·기호 키·글자 키의 배정 기호. 기능 키는 없음). 대각선은 대각선 배정이 있는 키에서만 탭과 달라 담기고(2.2 2번), 탭과 같은 ↘ = ㄹ은 담기지 않는다. 배정 자료만 바뀌면 표시가 따라온다.
- 자리(`TapKeyboardView.drawHints`, 4절): ↑ → 위 변 가운데, ← → 왼쪽 변 가운데(세로 가운데), ↓ → 아래 변 가운데, → → 오른쪽 변 가운데. **대각선(0.23, FR-TAP-02e ⑥)은 그 모서리:** ↖ = 위 줄 왼쪽(왼쪽 정렬, x = 왼쪽 + 2dp), ↗ = 위 줄 오른쪽(오른쪽 정렬), ↙ = 아래 줄 왼쪽, ↘ = 아래 줄 오른쪽. 위·아래 줄의 y는 ↑·↓ 표시와 같다. 지금은 ㄹ의 ㄼ(왼쪽 위)·ㄽ(오른쪽 위)·ㄾ(왼쪽 아래)과 0.24의 ㅗ ㅙ(오른쪽 아래)·ㅜ ㅞ(왼쪽 아래)다(FR-TAP-02g ⑧, 그리는 코드 그대로). 36dp 키에서 위 줄에 ㄼ · ㄺ · ㄽ 셋이 놓인다(각 약 8dp, 실기 확인 DV-43).
- 글자 크기는 키 라벨의 40 %(`HINT_SCALE`), 변에서 2dp(`HINT_PAD_DP`) — 36dp 키(라벨 약 19dp)에서 ←·→ 기호(약 8dp)가 가운데 라벨과 겹치지 않는다. 0.12까지는 `up` 오른쪽 위, `left` 왼쪽 아래, `right` 오른쪽 아래. 0.12까지는 글자 키의 `up`(대문자·쌍자음)을 표시하지 않았다. 0.13~0.22는 모서리 자리를 쓰지 않았다.
- 시험: TA-20·TA-21·TA-22·TA-25(내용), GJ-02·GV-03(그리는 자리, 0.23·0.24).
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
    fun onTapResult(key: TapKeyDef, out: TapOutput, gesture: TapGesture) = onTapOutput(key, out)   // 0.20: 뷰가 부르는 입구
    val chord: Set<ChordModifier> get() = emptySet()                                               // 0.20: 켜진 조합 키(읽기만)
    fun onFunctionDown(key: FunctionKey); fun onFunctionUp(key: FunctionKey)
}
```
- **`onTapResult`(0.20, FSS#98):** 뷰는 글자·숫자·기호 키의 결과를 **판정된 동작과 함께** 이 함수로 넘긴다. 0.19까지의 `onTapOutput(key, out)`은 두벌식에서 자모만 넘겨, 조합 키가 켜졌을 때 필요한 "같은 자리 쿼티 키의 결과"(2.4)를 알 수 없었다(HLD 5A "키 대응"). 판정은 지금처럼 한 번이다. 기본 구현은 `onTapOutput`으로 넘기므로 조합 키를 다루지 않는 싱크(입력 연습 `PracticeInput.tapSink`, 시험의 가짜 싱크)는 고치지 않는다. `InputDispatcher`는 `onTapResult`를 구현하고 `onTapOutput(key, out)`은 `onTapResult(key, out, Tap)`이다(LLD-08 6.1).
- **`chord`(0.20):** 켜진 조합 키 집합(Ctrl, Alt). 상태는 싱크(`InputDispatcher`)가 갖고 뷰는 그릴 때와 접근성 노드를 채울 때 읽기만 한다(HLD 5A "상태의 자리"). 기본값은 빈 집합이라 입력 연습 자판에는 켜짐 표시가 없다(FR-TAP-17 ④).

| 함수 | 처리 | 근거 |
|---|---|---|
| `setLayout` | `KeyGeom`(dp)·`KeyHitTester`를 다시 만든다 | FR-TAP-03 |
| `hit(p)` | `KeyHitTester.hit(p)`(LLD-04 2절: 사각형 포함 → 가장 가까운 키). 0.21: 빈칸이 없어 뷰 안의 모든 점이 어느 키 사각형 안이다 — ↵ 사각형(왼쪽·위 변 포함, 오른쪽·아래 변 제외) 안은 어느 줄이든 ↵. 0.20까지의 ① 빈칸 단계(`BlankDef` → 같은 줄 키)는 지웠다(D-73) | SRS 3.3, FR-TAP-03d ② |
| `onMeasure` | 폭 = 부모, 높이 = `layout.rows × rowHeight` | 5A |
| `onDraw` | 키 배경·글자·모서리 표시(3.4). 누른 키 강조. 0.20: 켜진 조합 키는 켜짐 바탕, Esc·Ctrl·Alt·Del은 낱말 글자 크기(아래 "조합 키 Ctrl·Alt") | FR-TAP-03b, 03c, 15 ④ |
| `chordChanged()`(0.20) | `invalidate()` + Ctrl·Alt 두 키의 접근성 노드 `invalidateVirtualView`. 서비스가 `InputDispatcher.onChordChanged`에서 부른다(LLD-08 3절). 0.25: 보이는 배치(`TapLayouts.shown`)가 마지막으로 그린 것(`lastShown`)과 다르면 `invalidateRoot()`도 — 모든 키의 이름이 바뀌었다 | FR-TAP-15 ④, 18 |
| `onTouchEvent` | 아래 | FR-CMN-11 |

#### `onTouchEvent(e)`
```
DOWN / POINTER_DOWN (index i, id):
  p = dp(e, i); key = hit(p)
  feedback.key()                                            // 키 입력 진동·소리는 누를 때(FR-CMN-11a ②)
  if key.function == MODE:                                  // 0.12(#205): 누를 때는 입력 없음, 뗄 때 판정
      fnPointers[id] = (key, TapGestureClassifier(key.geom, th.minCrossDp).begin(p))
  else if key.kind == FUNCTION:
      guard.stroke("Tap.fn") { sink.onFunctionDown(key.function) }   // 누르는 즉시, 대기열 밖(FR-CMN-11, 11a ①). Ctrl·Alt도 이 길(0.20)
      if key.function ∈ {BACKSPACE, ARROW_*, DELETE}: repeat.start(id, key.function)   // FR-CMN-07c, FR-TAP-10, DELETE 0.10(#198)
      fnPointers[id] = key
  else:
      cls[id] = TapGestureClassifier(key.geom, th.minCrossDp, eightWay = key.eightWay); cls[id].begin(p)   // 0.23: 8방향은 키 데이터로(2.1·3.1)
      queue.onDown(id)                                      // 5절
MOVE: 각 포인터의 히스토리 점을 cls[id].add (모드 키 포인터면 그 분류기에)
UP / POINTER_UP (id):
  if id = 모드 키 포인터: s = guard.run("Tap.classify") { ModeKeyGesture.of(c.finish(p)) == OPEN_SETTINGS }   // 실패면 입력 없음
        s == true  → guard.stroke("Tap.fn") { sink.onSettingsStroke() }                      // 위로 나감 = 설정(#205)
        s == false → guard.stroke("Tap.fn") { sink.onFunctionDown(MODE) }; guard.stroke("Tap.fn") { sink.onFunctionUp(MODE) }   // 탭 = 모드 전환
  else if id ∈ fnPointers: repeat.stop(id); sink.onFunctionUp(fn); remove
  else: r = guard.run("Tap.classify") {                    // 순수 계산, 상태 없음. 판정은 한 번(0.20: 동작 g를 결과에 실음)
            g = cls[id].finish(p)
            rel = if g == Tap then null else geometry.releasedKey(p)                       // 0.23: 손 뗀 키(4.1)
            out = ReleaseKeyCompound.of(key, rel)?.let { TapOutput(null, it, false) } ?: TapKeyResolver.resolve(key, g)
            Result(key, out, g) }
        outs = if r == null then queue.onCancel(id) else queue.onUp(id, r)                 // 실패한 칸만 비우고 뒤 결과는 흘려보냄
        for r in outs: guard.stroke("Tap.output") { sink.onTapResult(r.key, r.out, r.gesture) }   // 출력 하나 = 보호 단위 하나(LLD-09 2절), 누른 순서대로
CANCEL: outs = queue.onCancelAll(); repeat.stopAll(); fnPointers 각각(떼지 않은 모드 키 빼고) sink.onFunctionUp(fn); fnPointers.clear(); cls.clear()
        for r in outs: guard.stroke("Tap.output") { sink.onTapResult(r.key, r.out, r.gesture) }   // 이미 뗀 대기 결과만, 누른 순서대로
```
- `ACTION_CANCEL`(진행 중인 모든 터치가 함께 취소됨): 아직 떼지 않은 모든 포인터는 결과 없이 대기열에서 빠진다. 이미 뗐지만 앞 키를 기다리던 결과는 취소된 칸을 빼고 누른 순서대로 낸다. 이미 누를 때 처리한 기능키 입력은 되돌리지 않고 반복만 멈춘다(`onFunctionUp`은 입력을 만들지 않는 뗌 알림이다). 근거: SRS FR-CMN-11a ③, HLD 5A 처리 시점(DCR-11 결정).
- 결과는 처음 누른 키로만 정한다(SRS 동작 표 아래 문단). `,` 키를 →로 그어 `.` 위를 지나도 `"`, `=` 키를 ←로 그어 m 위를 지나도 `-`.
- Esc·탭 키(⇥)·방향키·Del·⌫·스페이스·↵·모드 키 처리 내용은 `InputDispatcher`(LLD-08 6절)에 있다.
- **두 줄 높이 ↵(0.21, FSS#105, SRS FR-TAP-03d ②~⑥, HLD 5A):** 코드 길은 0.20과 같고 키 사각형만 두 줄이다. ① 누름: ↵ 사각형 어디든 `onFunctionDown(ENTER)` 한 번(기능키 길, 누를 때). `REPEATING`에 없어 반복하지 않는다. 기능키 포인터의 이동은 보지 않으므로 ↵ 안에서 3줄 ↔ 4줄로 움직여도, ↵ 밖으로 나가도 아무 일 없고 뗄 때 `onFunctionUp(ENTER)`(입력 없는 알림)뿐이다. ② 이웃 획: p·l·`/`·`.`의 분류기는 자기 키 사각형만 보므로 ↵ 쪽으로 나간 획은 그 키의 그 방향 결과다 — p ↓ `|`, l → l, `/` → `/`, `.` ↑ `>`(처음 누른 키로만). `/`의 아랫변은 5줄 `,`의 윗변이라 테두리가 아니고 ↓는 보통 변 통과 `Exit(DOWN)` = `/`(↓ 배정 없음). ↵의 네 변은 여유 칸이 있으면 어느 것도 테두리 변이 아니다(`KeyGeom.build` 그대로). ③ 그리기(`onDraw`): 키마다 `geoms`의 사각형 하나에 바탕·글자를 그리므로 ↵는 두 줄 높이 사각형 하나(안쪽 여백 `KEY_INSET_DP`, 모서리, 기능 키 바탕, 눌림 바탕 모두 사각형 전체)이고 두 줄 사이에 선이 없다. 글자는 `keyRect.centerX()`·`centerY()` = 사각형 가운데(세로는 두 줄 경계). 글자 크기는 `rebuildGeometry`가 **줄 높이** × `LABEL_SCALE`(0.4)로 정하므로 ↵ 글자도 다른 키와 같다(키 높이에서 정하지 않음). 기능키라 변 가운데 표시(`drawHints`)가 없다. 줄 단위로 키를 걸러 그리는 곳은 없다. ④ 조합 키: ↵는 키가 하나라 Ctrl·Alt가 켜진 채 어느 줄을 눌러도 조합 키를 실은 `ENTER` 하나다(LLD-08 6.3 그대로). 시험 BE-01~BE-07, BV-01~BV-04, CD-19(LLD-11).
- **모드 키 ↑ = 설정(0.12, 이슈 #205):** 모드 키만 누를 때가 아니라 뗄 때 처리한다(다른 기능키는 그대로 누를 때). 판정은 글자 키와 같은 변 넘음(`TapGestureClassifier`, `minCrossDp`)이고 `ModeKeyGesture.of(g)` = 처음 통과한 변이 위면(`Exit`·`ExitReturn`·`Rub` 모두, 되돌아옴·문지르기는 보지 않음 — SRS FR-CMN-13 "처음 통과한 변만") `OPEN_SETTINGS`, 그 밖(탭, 첫 변 ←·→)은 `MODE_KEY_TAP` = 모드 전환. 맨 아랫줄이라 아래 나감은 바깥 변(기록 없음 = 탭). 대기열 밖이라 잡고 있는 글자 키를 기다리지 않고, 글자 키도 모드 키를 기다리지 않는다. 떼기 전 `ACTION_CANCEL`이면 입력 없음(모드 전환도 설정도 아님). 접근성 클릭(7절)은 그대로 탭 = 모드 전환이고 설명 문구에 "위로 밀면 설정"을 붙였다(`a11y_key_mode`).

#### 4.1 손 뗀 키 우선 (0.23, SRS FR-TAP-02f, HLD 5A "손 뗀 키 우선", FSS#109)

```kotlin
// kbd.app.view
class TapGeometry { fun releasedKey(p: StrokePoint): TapKeyDef? }      // 뗀 점의 키, 뷰 밖이면 null
internal object ReleaseKeyCompound { fun of(pressed: TapKeyDef, released: TapKeyDef?): Char? }   // 겹받침 호환 자모 또는 null
```

- **`releasedKey(p)`:** `p`가 뷰 안(0 ≤ x ≤ 폭, 0 ≤ y ≤ 높이)이면 누를 때와 같은 키 찾기(`indexAt` → `KeyHitTester.hit`, 사각형 경계도 탭과 같음, FR-TAP-02f ③)의 키, 뷰 밖이면 null이다. `KeyHitTester`는 뷰 밖 점에 가장 가까운 키를 주므로(LLD-04 2절) 그대로 쓰면 왼쪽 여유 칸에서 뗀 ㄹ 획이 ㅂ(가장 가까운 키)과 짝이 되어 버린다 — 여유 칸·자판 밖은 "키 없음"이다(FR-TAP-02f ②, 시험 GJ-03).
- **`ReleaseKeyCompound.of(pressed, released)`:** 다음이 모두 참일 때만 겹받침이다 — `released`가 있고, `released.id ≠ pressed.id`, 두 키 모두 `DUBEOLSIK_LETTER`, `JamoTables.COMPOUND_JONG[(Jamo.ofCompat(pressed.jamo), Jamo.ofCompat(released.jamo))]`가 있음(앞 = 누른 키, 뒤 = 뗀 키의 **탭 기본 자모** — 시프트 자모가 아님, 차례 그대로라 ㅂ → ㄹ 키는 짝이 아님, FR-TAP-02f ①). 표는 조합기가 겹받침을 두 자음으로 나누는 것(`SPLIT_JONG`)과 같은 표 하나다(HLD 5A — 새 표를 두지 않음). 모음 키는 표에 없어 늘 null이고(⑥ — 0.24의 겹모음 키 ㅗ·ㅜ·ㅡ도 같다: 누른 키가 모음이면 뗀 키가 무엇이든 방향 규칙, SRS FR-TAP-02g ⑤, 코드 그대로, 시험 GV-02), 쿼티·숫자·기호·기능 키는 종류에서 걸린다. 순수 함수다.
- **어디서:** `TapTouchLogic.up`(4절 의사 코드)이 분류(`finish`) 뒤, `TapKeyResolver` 앞에서 부른다 — 결과 해석의 맨 앞 단계(HLD 5A). **나감이 세어진 획에만:** `g == Tap`이면 부르지 않는다 — 키 가장자리를 눌러 최소 통과 거리 안에서 짝 키 위로 조금 밀려 떼어도 탭이다(SRS 0.91 검수 반영 8fe0e99, 시험 GJ-03). 나감·나갔다 돌아옴·문지르기는 통과 횟수와 관계없이 뗀 곳으로 정한다(④ — 나갔다 돌아옴은 누른 키 안에서 떼므로 `released.id == pressed.id`로 방향 규칙). 짝이면 `TapOutput(text = null, jamo = 겹받침, errorVibe = false)`이고 `Result`의 `gesture`는 분류 결과 그대로다.
- **Ctrl·Alt(⑤):** 이 단계는 조합 키 상태를 보지 않는다. 켜져 있으면 `InputDispatcher.onTapResult`가 `out`을 버리고 `ChordKeys.ofTap(key.id, gesture)`(2.4)를 보내므로 방향 규칙의 키 입력이 된다(ㄹ → ㅂ 키에서 뗌 = 쿼티 f ← = F, 시험 CD-20). 뷰 쪽에 조합 키 갈래를 두지 않으려는 것이다.
- **뗀 키는 동작하지 않는다:** 결과의 키는 처음 누른 키(`Result.key = key`)이고 뗀 키에는 아무것도 보내지 않는다(FR-SWP-01). 표시(`edgeHints`)·입력 연습(쿼티뿐)·그래피티는 바뀌지 않는다.
- **왜 stroke-core가 아닌가:** 짝 표가 `core-hangul`에 있고 stroke-core는 core-hangul에 의존하지 않는다(LLD-03 1.2). stroke-core에 같은 표를 하나 더 두면 HLD 5A의 "표 하나"에 어긋난다. 그래서 `TapGestureClassifier.finish(p)`·`TapKeyResolver.resolve(key, g)`의 모양은 그대로이고, 뗀 점은 `TapTouchLogic`이 이미 가진 `p`다(D-76).
- 근거: SRS FR-TAP-02f ①~⑥, 탭 자판 동작 표 "다른 키 위에서 뗌" 줄, FR-SWP-01. 시험: GJ-03(11개 곧은 획, 짝 아닌 키·거꾸로 차례·나갔다 돌아옴·문지르기·여유 칸·가장자리 탭·쿼티), CD-20(Ctrl 켜짐), 실기 DV-43 ⑤.

#### 조합 키 Ctrl·Alt (0.20, FSS#98)

- **누름:** Ctrl·Alt는 다른 기능키처럼 누를 때 `sink.onFunctionDown(CTRL / ALT)`, 뗄 때 `onFunctionUp`이다(대기열 밖, 진동·소리는 누를 때). 반복 대상(`TapTouchLogic.REPEATING`)이 아니다(FR-TAP-15 ①). 켜고 끄기·싣기·끄기는 모두 `InputDispatcher`가 한다(LLD-08 6.3) — 뷰와 `TapTouchLogic`에는 조합 키 상태가 없다.
- **글자 키와의 순서:** 글자·숫자·기호 키는 결과가 대기열에서 나와 싱크에 닿을 때 조합 키가 실린다(손을 뗀 때가 아니다, FR-TAP-15 ②). 뷰는 0.19와 같은 순서로 `onTapResult`를 부를 뿐이다.
- **켜짐 표시(FR-TAP-15 ④):** `onDraw`의 키 바탕은 ① 켜진 조합 키(`isChordOn(key)` = 그 키가 Ctrl·Alt이고 `sink.chord`에 있음) → `CHORD_ON_COLOR`(0xFF2E6FBF, 파랑) ② 누르고 있는 키 → `PRESSED_COLOR`(0xFF5A5A5A) ③ 기능키 → `FUNCTION_COLOR`(0xFF2A2A2A) ④ 그 밖 → `KEY_COLOR`의 차례로 고른다. ①이 ②보다 앞이라 눌러 켜는 순간부터(손가락 아래에서도) 켜짐 바탕이고, 눌러 끄면 손을 뗄 때까지 눌림 바탕이었다가 보통으로 돌아간다. 탭 자판에는 시프트 키가 없어 따를 표시가 없으므로 새로 정한 색이다(LLD 결정 D-71, 실기 확인 DV-39 ⑧).
- **낱말 글자 크기(FR-TAP-03c):** Esc · Ctrl · Alt · Del 네 키(`WORD_KEYS`)의 글자는 `wordPaint` 하나로 그린다(네 낱말이 같은 크기). 크기는 `rebuildGeometry`(배치·폭·줄 높이·여유 칸이 바뀔 때) 끝의 `fitWordLabels()`가 정한다:
  ```
  안쪽 폭 = 네 키 가운데 가장 좁은 키의 폭(px) − 2 × (KEY_INSET_DP 2 + WORD_PAD_DP 2) × density
  size = labelPaint.textSize                                   // 다른 키 글자 크기(줄 높이 × 0.4)
  WORD_FIT_STEPS(8)번까지: widest = 네 낱말 가운데 가장 넓은 것의 measureText(size)
                           widest ≤ 안쪽 폭이면 끝
                           size = WordLabelFit.size(size, widest, 안쪽 폭)     // = size × 안쪽 폭 / widest
  ```
  `WordLabelFit.size(base, widestAtBase, innerWidth)`는 순수 함수다: 들어가면(`widestAtBase ≤ innerWidth`) `base`, 아니면 `base × max(innerWidth, 0) / widestAtBase`(`widestAtBase ≤ 0`이면 `base`). 글자 폭은 반올림되어 크기에 정확히 비례하지 않으므로 줄인 뒤 다시 재어 들어갈 때까지 줄인다(보통 한두 번). 그래서 가장 긴 낱말이 키 한 칸 32dp(안쪽 폭 24dp)에서도 들어가고, 키가 넓으면(태블릿) 다른 키와 같은 크기다. Robolectric 실제 글꼴 측정(시험 CV-04): 줄 높이 48dp(글자 19.2)에서 키 36dp → 약 17.3, 약 32.7dp → 약 15.3, 32dp → 약 14.9. ⇥·방향키·⌫와 글자·숫자·기호 키의 글자는 그대로 `labelPaint`다.
- **켜진 동안 두벌식을 쿼티로 그림(0.25, FSS#117, SRS FR-TAP-15 ⑥):** stroke-core에 순수 함수 `TapLayouts.shown(layout: TapLayout, chordOn: Boolean): TapLayout` = `chordOn && layout === DUBEOLSIK`이면 `QWERTY`, 아니면 `layout`. `onDraw`는 `shown(geometry.layout, sink.chord.isNotEmpty())`의 키 i로 글자·모서리 표시(3.4 `edgeHints`)를 그리고, 바탕·자리는 `geometry`의 키 i(설정된 배치)로 정한다. 두 배치는 키 순서·id·줄·x·폭·줄 수·기능키가 같다(TL-13) — 그래서 자리·크기가 그대로고, Esc·Ctrl·Alt·Del 낱말 크기(`fitWordLabels`)도 그대로다. `shownKeyAt(i)` = 보이는 배치의 키 i(접근성 이름용). `keyAt(i)`·누름 판정·`onTapResult`의 키는 설정된 배치 그대로라 보내는 키(2.4 `ChordKeys.ofTap`은 id로 쿼티 키를 찾는다)·조합·모드가 바뀌지 않는다. 하나만 끄면 집합이 비지 않아 쿼티 그대로다. 디스패처는 싣는 순간 집합을 한 번에 비우므로 그 사이에 두벌식이 그려지지 않는다. 쿼티 배치와 조합 키를 다루지 않는 싱크(연습)는 바뀌지 않는다.
- 근거: SRS FR-TAP-03c·15·17 ④, HLD 5A "그리기". 시험: CV-01~CV-14(LLD-11 7.5), TL-13.

## 5. MultiPointerQueue (app)

```kotlin
class MultiPointerQueue<R : Any> {               // 0.20: R = 뗀 키 하나의 결과(TapTouchLogic의 (키, 출력, 판정된 동작))
    fun onDown(pointerId: Int)
    fun onUp(pointerId: Int, result: R): List<R>
    fun onCancel(pointerId: Int): List<R>
    fun onCancelAll(): List<R>
    fun clear()
}
```
- 0.20(FSS#98): 결과의 형을 타입 인자로 했다. 뷰가 판정된 동작을 결과와 함께 싱크에 넘겨야 해서다(4절 `onTapResult`). 대기 규칙은 그대로다.
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
- **조합 키(0.20, FSS#98):** Ctrl·Alt는 반복하지 않는다(누르고 있어도 한 번, FR-TAP-15 ①). 조합 키가 실린 방향키·Del·⌫의 반복은 지금처럼 `fire(key)` = `sink.onFunctionDown(key)`이고, 반복마다 같은 조합 키를 싣는 것은 `InputDispatcher`가 그 누름에 실린 집합을 기억해서 한다(LLD-08 6.3). 타이머는 바뀌지 않았다.
- 근거: FR-CMN-07c, FR-TAP-10. 시험: RT-01~RT-05.

## 7. TapKeyAccessHelper (app, NFR-07)

```kotlin
class TapKeyAccessHelper(private val view: TapKeyboardView) : ExploreByTouchHelper(view)
```
| 함수 | 처리 |
|---|---|
| `getVirtualViewAt(x, y)` | `hit(x, y)`의 키 번호(0.21: ↵ 사각형의 두 줄 모두 ↵ 노드) |
| `getVisibleVirtualViews(list)` | 키 번호 전부 |
| `onPopulateNodeForVirtualView(id, node)` | 경계 = 키 사각형(px), `contentDescription` = 키 글자(기능키는 이름 문자열 리소스. 0.25: `shownKeyAt(id)`의 키 — Ctrl·Alt가 켜진 두벌식에서는 쿼티 키 이름), 동작 = `ACTION_CLICK`. 0.20: Ctrl·Alt 키는 `className = "android.widget.ToggleButton"`, `isCheckable = true`, `isChecked = view.isChordOn(key)` |
| `onPerformActionForVirtualView(id, CLICK)` | 그 키의 `Tap` 결과를 `sink`로(`onTapResult(key, out, Tap)`. 기능키는 down+up) |

- 쿼티·두벌식에만 붙인다(NFR-07). 읽어 주는 문구는 UI 문구라 문자열 리소스로 둔다.
- **두 줄 높이 ↵(0.21, FSS#105, SRS FR-TAP-03d ⑦):** 노드는 키마다 하나이고 경계는 키 사각형(`keyBoundsPx`)이므로 ↵는 노드 하나, 경계 = 두 줄 사각형이다. 이름은 그대로(`a11y_key_enter`). 노드 번호 = 키 목록 순서라 훑어 넘기는 순서에서 ↵는 l 다음, z 앞이다(3.2 키 목록 순서). 시험 BV-03.
- **조합 키(0.20, FR-TAP-18):** Ctrl·Alt 노드는 켜고 끄는 항목이다. 이름은 `a11y_key_ctrl` = "Ctrl", `a11y_key_alt` = "Alt"(화면 읽기가 켜짐·꺼짐을 덧붙여 읽는다). 조합 키 집합이 바뀌면(`chordChanged()`) 두 노드를 `invalidateVirtualView`로 무효화해 바뀐 상태가 읽히게 한다 — 눌러 켜고 끔, 대상 키에 실려 꺼짐, 모드 키·입력창 변경 등으로 꺼짐 모두 `InputDispatcher.onChordChanged` 하나로 온다. 누르기 동작은 터치와 같이 down+up(켜고 끄기)이고, 켜진 채 다른 키를 누르기 동작으로 누르면 그 키의 탭 결과에 실린다(글자 키는 `Tap`, 기능키는 그 키).
- 시험: CV-06·CV-07(LLD-11 7.5), 실기기 DV-10·DV-39 ⑨.

## 8. 시퀀스

### 8.1 탭 자판 키 입력 (두벌식 "각" 조합 중 숫자 줄 2를 ↑)

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

### 8.2 조합 키 (0.20, FSS#98 — 두벌식 "각" 조합 중 Ctrl을 켜고 ㅂ 키를 ↑)

```
DOWN(Ctrl 키) → feedback.key() → sink.onFunctionDown(CTRL)
     → InputDispatcher: chord = {CTRL}, onChordChanged() → TapKeyboardView.chordChanged()(다시 그림, 노드 무효화)   // "각"은 조합 중 그대로
UP   → sink.onFunctionUp(CTRL)                                                                                        // 아무 일 없음
DOWN(ㅂ 키) → TapGestureClassifier.begin, MultiPointerQueue.onDown
MOVE → OUT(TOP)
UP   → finish → Exit(UP) → TapKeyResolver.resolve → TapOutput(jamo = 'ㅃ') → queue.onUp → [(ㅂ 키, ㅃ, Exit(UP))]
     → InputDispatcher.onTapResult(ㅂ 키, ㅃ, Exit(UP))            // chord가 비어 있지 않음 → 조합 키 길(LLD-08 6.3)
          → mods = {CTRL}; chord = {}; onChordChanged()            // 꺼냄
          → HangulComposer.finish() + ComposingController.finishComposing()   // "각" 확정
          → ChordKeys.ofTap("q", Exit(UP)) = (Q, Shift)            // 같은 자리 쿼티 키 q의 ↑ = "Q"
          → ComposingController.chordKey(Q, {CTRL, SHIFT}) → EditorPort.sendChord
               → Ctrl↓ Shift↓ Q↓ Q↑ Shift↑ Ctrl↑(LLD-08 4절)
          → ModeController.onInput()
```
