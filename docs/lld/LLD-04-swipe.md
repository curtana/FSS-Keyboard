# LLD-04 긋기 입력 처리

- 대응 HLD 절: 4장(4.1 한 획의 흐름, 4.2 StrokeClassifier, 4.3 SwipeInputMapper, 4.4 HangulComposer)
- 버전 **0.30** (2026-11-02)
- 0.30(이슈 #251 오너 결정: 공개판·개인판을 하나로(빌드 하나, 긋기 엔진은 모든 빌드)): 긋기 뷰·`SwipeTouchLogic`·`SwipeMapperState`·연습 부분(`kbd.app.swipe.impl`)은 app main 소스 세트에 있다(9절의 "personal"은 이 판부터 "app"으로 읽는다).
- 0.29(이슈 #242 오너 결정(두벌식 겹받침 한 번에), 조합기 7절): `HangulComposer`가 겹받침 자모 하나(`Consonant(ㄺ)` 등)를 받는다 — 초성·중성만 있는 글자면 그 받침으로 두 자음 단위(`Consonant(ㄹ)`, `Consonant(ㄱ)`)를 저장해 두 번 치기와 같다(갉 + ㅏ = 갈가, ⌫ = 갈 — 둘째 자음만, PM 채택 2026-11-02; 낱자 ㄺ ⌫ = 통째). 그 밖(조합 없음, 받침 있음, 초성만)은 지금 글자를 확정하고 겹받침 낱자(ㄺ)로 선다. 낱자 뒤 모음은 둘째 자음이 가져간다(ㄺ + ㅏ = ㄹ가). 긋기 자판은 겹받침 자모를 내지 않는다.
- 0.28(이슈 #237 후속, SRS 0.60 FR-SWP-45 설계 결정): 테두리 닿음은 **그 방향으로 누른 점에서 `minCrossDp` 이상 움직였을 때만** 센다(3.2, `EdgeCrossingTracker.start`) — 테두리 근처 탭·흔들림, 반대쪽으로 긋다 옆 테두리를 스친 대각선은 나감이 아니다. "바깥 변 = 기록 없음" 낡은 문장을 새 규칙으로 고침.
- 0.27(이슈 #237·#208): ① 맨 윗줄 키(`KeyGeom.topAtWindow`)에서 창 위 끝(y ≤ 키 위 + 0.5dp)에 닿은 점은 위 변을 `minCrossDp`만큼 넘은 것으로 친다(3.2) — 실기기(태블릿)에서 창 밖 점이 y = 0으로 와서 숫자 키 ↑(! @ # …)가 안 되던 원인. 돌아옴은 실제 점으로. ② 숫자 4 ← `;` ↔ 기호2 ← `"` 맞바꿈(8.2·8.3, 오너 #208). ③ 기호·기호2·기호3 키의 배정 기호도 숫자 키처럼 네 변의 가운데에 그린다(↑ 위, ↓ 아래, ← 왼쪽, → 오른쪽, 9.2). ③b 천지인 키는 가운데 ㆍ(탭)와 변 가운데 ↑·↓ ㅣ, → ㅡ를 그린다(`cheonjiinEdgeHints`, 오너 #198 "그어서 나오는 모든 글자"). 바깥 변(← ↓ 등)도 테두리에 닿으면 나감(3.2). ④ `FSSStroke` 줄에 숫자·기호·천지인 키의 키 사각형·바깥 변·통과 기록·첫 통과 변.
- 0.26(이슈 #217 오너 결정 "아래 왼쪽, 오른쪽 위 이 두 가지만"·"두 번째 획을 입력받으면 확정", SWIPE 0.43·SRS 0.58·HLD 0.53, 설계 PR #231): C 기준 역할을 **두 구역**으로 바꾸고 **첫 획은 늘 두 역할 후보**로 둔다(6.3). 0.25의 b·cy·cx·①~④와 C 전용 첫 획 상태(`IC`·`VDOT`·`EUC`·`HDOT`)는 지웠다.
- 0.25(이슈 #217 오너 결정 "자음 칸에 겹칠 수 있지", SWIPE 0.42 3.6.3·SRS 0.57·HLD 0.52, 설계 PR #226): C 역할을 **획의 가운데**로 정한다. `VowelStroke.midY` = (y0 + y1)/2, `midX` = (x0 + x1)/2, C 가운데 cy = C.T + (C.B − C.T)/2, cx = (C.L + C.R)/2. 세로 획 ① T < cy이고 x ≥ cx = ㅣ(길이 무관) ② M ≥ b = 세로 점 ③ M < b이고 x ≥ cx = ㅣ ④ 그 밖 = 무시. 첫 가로 획 y(가운데) ≥ b = ㅡ, y < b이고 가운데 x ≥ cx = 가로 점, 그 밖 = 무시(6.3). 앞 획 구역의 "높이 y"·"자리 x"는 그대로 시작점이다. `FSSStroke` 역할 문구도 이 행 번호로(`roleText`).
- 0.24(이슈 #213, 오너 결정 2026-11-02 "자음 글자를 ㅃ이 아니라 우측 ㄸ에 두었으면 하는데, 중앙 정렬을 해야지", SWIPE 0.40 9.5·3.6.5, SRS 0.55, HLD 0.50): 가운데 자음 자리 C의 **가로 중심 = 자판 가로 중심**(가운데 ㄸ 열 중심). `consonantBox`의 왼쪽 끝 = 폭 / 2 − 너비 / 2(0.23까지 1.25 × 열 폭). 세로 자리·크기 그대로, 세로 화면 ≈ (139.2, 52)–(220.8, 148). 판정 규칙(3.6.3 C 기준)은 그대로라 C와 함께 옮겨진다(8.1).
- 0.23(이슈 #208, 오너 결정 2026-11-02 "기호 키 옆에 두 번째 기호 키를 두고 나갔다 들어오는 것을 배치하자" + 같은 날 추가 지시 "기호3 키를 만들고 기호를 적당히 분배하자", 오너 확정 = PR #210 배치·TBD-28 A, SRS 0.53 FR-SWP-42·43·43a·44·50, SWIPE 0.38 7·8장): 기호 키의 나갔다 돌아옴·문지르기 단계(`Stage`)를 지웠다 — 첫 나감 방향만 본다(4.5). 기호 키 셋(8.3): **기호3**(줄 4 열 0, 0.22의 ⚙ 빈 칸 자리, id "symbol3") = 탭 `/` → `=` ↑ `?`(← 화면 끝, ↓ 배정 없음 = 탭), **기호**(줄 4 열 5) = 탭 `.` ← `(` → `)` ↑ `>` ↓ `-`, **기호2**(줄 4 열 6, 빈 칸이던 자리, id "symbol2") = 탭 `,` ← `;` ↑ `<` ↓ `'`(→ 화면 끝). 숫자 키(8.2): 2 = ← `[` → `]`(`< >`는 기호 키 ↑로), 6 ← 배정 없음(`/`는 기호3 탭으로). 9·0 ↑ `( )`는 그대로(TBD-28 A: `( )` 두 곳). 배정 없는 방향은 오류 진동 없이 탭(4.4·4.5). ↓ 배정 허용. 빈 칸은 맨 아랫줄 오른쪽 하나. 탐침 STAGE2·STAGE3 삭제(5.2), 라벨 = 가운데 탭 기호 + 모서리 배정 기호(8.3, 9절 `onDraw`). 시험 NK-02·06·15, SK-01~SK-08, SL-01·09·10, PS-14·18(LLD-11).
- 0.22(이슈 #205, 오너 결정 2026-11-02 "설정 키가 있다면 제거해", "완성 기능도 없앰"): 긋기 자판의 ⚙ 키(줄 4 열 0, FR-SWP-66)를 지우고 그 자리는 **빈 칸**(`empty_r4c0`, 터치만 받고 입력 없음, 8.1). `FunctionKey.SETTINGS`도 지웠다. 조합 중 ⚙의 한 글자 완성(9.6, 이슈 #173·#180)은 없어졌다 — 글자 완성은 스페이스·다음 자음·모드 전환 등 기존 경로로만. 설정은 모드 키 ↑(뗄 때 판정, 9.3, LLD-05A 4절과 같은 판정)로 연다. 시험 SL-01·KH-06 바뀜, MK-06·MK-07(LLD-11 5.5).
- 0.21(이슈 #179 오너 결정 "길이 판정 폐지"·TBD-24 "자음 기준 C", 이슈 #180 후속, SRS 0.44·HLD 0.40·SWIPE 0.35): **길이로 판정하지 않는다.** `VowelStroke`의 `long`과 분류기의 길이 단계 ④를 지웠다(4.3). 조립기(6.3)는 **다음 후보 표(SWIPE 3.6.2)의 상태 기계**다: C가 있으면 획의 역할(ㅣ / 세로 점, ㅡ / 가로 점)을 C로 먼저 정하고(역할 표, b = C.B − t/2), 그 역할의 후보만 앞 획 기준 구역(쪽·높이만, 닿음·거리 상한·길이 비례 여유 없음)으로 고른다. 낱모음(C 없음)은 앞 획 기준 구역만 쓰고 ㅏ / ㅗ는 두 획의 상대 크기(같으면 ㅏ)로 고른다. 길이는 ㅓ / ㅢ 0.5 특례(TBD-22)와 낱모음 ㅏ / ㅗ에만 쓴다. ㅓ / ㅢ 끝 자리는 x ≥ R − t 또는 가로지름(D-62 개정). C 상자는 `SwipeLayout.consonantBox`(8.1)이고 매퍼가 `consonant`로 받는다(6.1, 9.3). **가운데 자음**(9.5): 0.20의 가운데 음절을 버리고 조합기의 마지막 자음 하나를 C 자리(자음 키 영역 왼쪽 위, 키 줄 2개 높이)에 그린다. 비밀번호 입력창에서만 끈다(FR-CMN-08 ①, 학습 금지 칸은 그림). 설계질의 #192(C가 있을 때 ㅜ ② 시작 높이, 낱모음 ‥ 보임)의 임시 처리 D-63·D-64.
- 0.20(이슈 #179 오너·PM 결정, #180 오너 요청, SRS 0.43·HLD 0.39·SWIPE 0.34): **ㅓ/ㅢ는 높이 3등분**(앞 가로 획이 ㅣ의 위·가운데 1/3 = ㅓ, 아래 1/3 = ㅢ, 길이 안 씀, ㅣ 아래 끝 조건 없이 위아래 t/4 닿음만, 6.3, D-62). 길이는 혼자 있는 첫 획에만(둘째 획부터 위치만, 동점 규칙 삭제). **가운데 음절**(9.5, SWIPE 9.5, FR-DSP-10): 조합 중 글자 + 임시 ㆍ·‥를 자판 가운데에 키 줄 약 3개 높이로 흐리게, 키 < 가운데 음절 < 획 자국.
- 0.18(이슈 #173 오너 결정, SRS 0.41·HLD 0.37·SWIPE 0.32): **"자음은 탭, 모음은 손글씨 필순 획, 종성은 다시 탭."** 한글 자음 키의 판정은 키 변을 보지 않고 누른 점 → 뗀 점 변위로 ① 탭/획(`minCrossDp`) ② 축(큰 성분) ③ 방향(→ ↓만, 반대 = `Reverse`) ④ 길이(`longDp`, 점/긴 획) 순서로 정한다(4.3). 0.25~0.29의 변 통과 모음 규칙(첫 나감·길게·되돌아오기·꺾기·문지르기·+ㅣ)과 그 상태·탐침은 지웠다. 모음은 새 조립기 `VowelStrokeAssembler`(core-hangul `kbd.hangul.stroke`, 6.3)가 필순과 **획 사이 위치 관계**(SWIPE 3.6, 허용 오차 `posTolDp`)로 21모음을 만든다. 매퍼는 탭 → 자음, 획 → 조립기, 천지인 키는 따로(6절). 조립을 닫으며 모음이 되지 못한 획을 버리면 오류 진동(`MapResult.Jamo.errorVibe`, `reset(): Boolean`). 배치: 왼쪽 열 1·8·0·천지인·⚙(설정 키), 오른쪽 열 7·9·⌫·↵·빈 칸(8.1). 안내는 획 안내(5.2 `DOT`·`LONG` 탐침, 조립기 미리보기). 획 자국(9.4, SWIPE 9.4).
- 0.17(이슈 #168 오너 결정, SRS 0.38·HLD 0.34·SWIPE 0.29): **긋기 = 그 키의 자음 + 한 획 모음(늘 새 음절), 탭 = 자음 하나, 쌍자음 = 전용 키.** 자판은 7×6(숫자 줄 아래 쌍자음 줄 (빈) ㅃ ㅉ ㄸ ㄲ ㅆ (빈), 0.16까지의 위 48dp 빈 띠를 이 줄이 씀, 8.1). 쌍자음 키는 일반 자음 키와 같은 경로다(`kind = CONSONANT`, `consonant` = 쌍자음). `SwipeInputMapper`(6절)는 기준 키·쌍자음 후보·시간 비교·`onBackspace`·`anchorKeyId`·`keyIdOf`를 지우고 상태는 천지인 조립기 하나다: 탭 → `[Consonant(c)]`, 긋기 → `[Consonant(c), Vowel(v)]`. 결과 문자열은 다시 음절(`StrokeRules.syllable`, 5.2). 안내는 모든 자음 키(9.3 `guided = true`). 무시된 긋기(옛 FR-SWP-38)는 없고 `Ignored`는 이어질 수 없는 천지인 요소뿐이다. 조합기의 `DoubleConsonant`·`UndoLast`·`lastConsonant()`는 긋기가 더 쓰지 않는다(core-hangul에 남김, 7절). 시간 기준값 `doubleTapWindowMs`는 없다(LLD-03 3.4, 형식 4).
- 0.16(이슈 #160 오너 요청, SRS 0.36·HLD 0.32·SWIPE 0.27 5.4절): **천지인 키**. 넷째 줄 맨 왼쪽(0.15까지 빈 칸) = `SwipeKeyKind.CHEONJIIN`(라벨 "ㆍ ㅡ ㅣ", 8.1). 한 획 = 요소 하나(`StrokeOutcome.Element`, 탭 ㆍ·↑↓ ㅣ·→ ㅡ, 처음 통과한 변만, 4.7). `SwipeInputMapper`가 `CheonjiinAssembler`(core-hangul로 옮김, LLD-05 4절)를 가지고 요소를 조립한다: 기준 키 예외(기준 키 그대로·없어도 됨), 쌍자음 후보 끊음, 다른 입력이 오면 조립을 닫음, 이어질 수 없는 요소 = `Ignored`(오류 진동), ⌫는 조립기 먼저(`popElement`), 임시 글자 ㆍ·‥(`pendingGlyph`)(6절). 조합기의 `ReplaceVowel`은 **자기 단위로 쌓는다**(7.1, G8 바꿈): 닫힌 모음도 ⌫가 요소 하나씩 지운다(간 ⌫ = 가, ⌫ = 기).
- 0.15(이슈 #156 오너·PM 결정, SRS 0.35·HLD 0.31·SWIPE 0.26): 모든 모음은 한 획이다. 나눠 쓰기와 긋기 병합 표를 지웠다 — `ComposePolicy.SWIPE` = `MergeTables.NONE`, `mergeConsecutiveVowel` = false(7.1). 모음 긋기를 잇달아 하면 두벌식처럼 새 글자(가 + ㅏ = 가ㅏ, 가 + ㅣ = 가ㅣ, 고 + ㅏ = 고ㅏ, TBD-18 = B, 7.2). 받침·겹받침·도깨비불·자모 단위 ⌫는 그대로이고, 한 획 ㅘ는 모음 이벤트 하나라 ⌫ 한 번에 지운다(과 ⌫ = ㄱ, 6절 `onBackspace`).
- 0.14(이슈 #150 오너·PM 결정, SRS 0.33·HLD 0.29·SWIPE 0.24): 자음(탭)과 모음(긋기)을 나눴다. `BlockParser`(6절)를 지우고 `SwipeInputMapper`(6절, 기준 키·쌍자음 후보)를 새로 썼다. 긋기 결과 `StrokeOutcome.Vowel`은 모음만이고 안내 글자는 낱모음이다(5절). 조합기의 run(`RunStart`·`pending`)과 `MergeVowel`을 지우고 긋기 정책을 두벌식 오토마타 + 긋기 병합 표 + 자모 단위 ⌫로 바꿨다(7절). 시간 기준값은 `doubleTapWindowMs` 하나만 남는다(LLD-03 3.4). 10.2(다시 나누기) 시퀀스는 지웠다.
- 0.12(이슈 #142 오너 결정): 누르고 있기 = 쌍자음(FR-SWP-35)을 없앴다. `StrokeClassifier.held`·`holdCapable`·`tick`, `StrokeResult.held`, `ProgressState.held`, `SwipeAction.held`, `ActionKind.HOLD_LIFT`·`HOLD_SWIPE`, `BlockType.HOLD_LIFT`·`HOLD_SWIPE`, `holdTick`을 지웠다. 오래 누른 탭은 보통 탭, 오래 누르다 그은 획은 보통 긋기다. 쌍자음은 두 번 탭·탭 + 긋기로만 만든다(FR-SWP-30·31). 설계 문서(SRS·HLD·SWIPE)의 해당 문구는 설계팀이 따로 고친다.
- 공통 타입(`Jamo`, `JamoEvent`, `ComposeResult`, `Dir`, `Edge`, `RectDp`, `StrokePoint`, `KeyGeom`, `Thresholds`)은 [LLD-03-modules.md#3](LLD-03-modules.md#3-모듈-사이-공통-타입)에 있다.
- 규칙의 정본은 SWIPE_INPUT_DESIGN.md 0.35(이하 SWIPE)이다. 이 문서는 그 규칙을 함수로 옮긴다.

## 1. 범위

| 절 | 구성 요소 | 모듈 |
|---|---|---|
| 2 | `KeyHitTester` (키 찾기, 모든 자판 공용) | stroke-core |
| 3 | `EdgeCrossingTracker` (변 통과 기록, 탭 자판과 공용) | stroke-core |
| 4 | `StrokeClassifier` (한글 자음 키 탭/모음 획, 둘레 숫자·기호·천지인 판정) | gesture-engine |
| 5 | `ProgressState` (안내용 진행 상태) | gesture-engine |
| 6 | `SwipeInputMapper` (탭 → 자음, 모음 획·천지인 조립, 0.18), `VowelStrokeAssembler` (6.3) | gesture-engine, core-hangul |
| 7 | `HangulComposer` (조합기, 모드 정책 3종) | core-hangul |
| 8 | 긋기 자판 배치·둘레 숫자 배치표·기호 배치표 데이터 | gesture-engine |
| 9 | `SwipeKeyboardView`, `TouchTracker` | app(`kbd.app.swipe.impl`) |
| 10 | 시퀀스: 긋기와 탭(각), 쌍자음 키(밖·바까·박까·밖가) | – |

## 2. 키 찾기 (KeyHitTester, stroke-core)

```kotlin
class KeyHitTester(val keys: List<KeyGeom>, val windowW: Float, val windowH: Float)
```

#### `KeyHitTester.hit(x: Float, y: Float): KeyGeom?`
- 입력: 누른 점(dp, 입력 뷰 좌표). 출력: 키, 없으면 null.
- 처리:
  1. `rect`가 점을 포함하는 키(왼쪽·위 변 포함, 오른쪽·아래 변 제외)가 있으면 그 키.
  2. 없으면 점이 어느 키에도 속하지 않는 틈이다. 점과 각 키 사각형 사이 거리(사각형 밖 최단 거리)가 가장 작은 키를 고른다. 같으면 목록 앞 키.
  3. 단, 배치 데이터가 "입력 없음 영역"으로 둔 곳(긋기 자판 빈 띠, LLD-08 7절)은 이 함수를 부르기 전에 뷰가 걸러 낸다.
- 오류: 키 목록이 비면 null.
- 근거: HLD 4.1("이웃 틈은 가까운 키로"), SRS 3.3(들여쓰기 빈칸은 가장 가까운 키). 시험: KH-01~KH-04.

#### `KeyGeom.build(id, rect, windowW, windowH): KeyGeom` (companion)
- `outerEdges` = { LEFT if `rect.left ≤ 0.5`, RIGHT if `rect.right ≥ windowW − 0.5`, BOTTOM if `rect.bottom ≥ windowH − 0.5` }. 0.5dp는 부동소수 오차 여유다. TOP은 넣지 않는다(LLD-03 3.3).
- 시험: KH-05(쿼티 1·0·q·p·⌫·↵·맨 아랫줄), KH-06(긋기 자판 1·8·0·7·9·⌫·↵·맨 아랫줄, ㅋ의 왼쪽 = 빈 칸이라 바깥 변 아님).

## 3. 변 통과 추적 (EdgeCrossingTracker, stroke-core)

### 3.1 타입

```kotlin
enum class CrossDir { OUT, IN }
data class Crossing(val edge: Edge, val dir: CrossDir, val t: Long, val x: Float, val y: Float)
enum class XZone { LEFT_OUT, IN, RIGHT_OUT }
enum class YZone { TOP_OUT, IN, BOTTOM_OUT }
data class ZoneState(val x: XZone, val y: YZone) { val inside get() = x == XZone.IN && y == YZone.IN }
```

### 3.2 판정 모형

축마다 이력(hysteresis) 구역을 둔다. `m` = `minCrossDp`.

| 구역 전이 | 조건 |
|---|---|
| X: IN → RIGHT_OUT | `x ≥ rect.right + m`(0.27: 바깥 변도 — 테두리 점은 아래 "창 테두리에 붙은 점"으로 이 값이 됨) |
| X: RIGHT_OUT → IN | `x ≤ rect.right − m` |
| X: IN → LEFT_OUT | `x ≤ rect.left − m`(0.27: 바깥 변도) |
| X: LEFT_OUT → IN | `x ≥ rect.left + m` |
| X: RIGHT_OUT ↔ LEFT_OUT | 위 두 전이를 차례로 적용(한 표본에서 IN을 거침) |
| Y: 같은 방식 | TOP·BOTTOM, `y`는 아래로 증가 |

- 키 안에 있음 = 두 축 모두 IN. 처음 누른 점은 정의상 안(`ZoneState(IN, IN)`)이다. 누른 위치가 변에 가까워도 같다(FR-SWP-02).
- **나감(OUT):** 안 → 밖으로 바뀐 표본에서 기록한다. 바뀐 축의 변이 나감 변이다.
- **들어옴(IN):** 밖 → 안으로 바뀐 표본에서 기록한다. 마지막으로 IN이 된 축의 변이 들어옴 변이다.
- 밖에서 밖으로(모서리를 돌아감)는 기록하지 않는다. 키 위쪽 바깥에서 아래로 내려와 위 변으로 들어오면 IN(TOP)이다.
- ~~**바깥 변 제외(FR-SWP-45)**~~(0.27, 이슈 #237로 대체): 0.26까지는 바깥 변 쪽 OUT 구역으로 전이하지 않아 바깥으로만 나간 획은 기록이 없었다. 이제 바깥 변도 아래 "창 테두리에 붙은 점"으로 나감이 된다. 그 방향에 배정이 없으면 결과는 탭과 같다(숫자 1 ←·7 →, 기호2 →, 기호3 ←, 천지인 ← = ㆍ, 모드 키 ←·↓ = 모드 전환. 새 기호는 배정하지 않음).
- **창 테두리에 붙은 점(0.27, 이슈 #237):** 플랫폼은 창 밖으로 나간 터치를 창 테두리에 붙여 보낸다(위 y = 0, 왼쪽 x = 0 …). 그래서 키 변이 테두리에 있으면(`KeyGeom.windowEdges` = 바깥 변 + 맨 윗줄의 위 변) 그 테두리에서 0.5dp(`OUTER_TOLERANCE_DP`) 안의 점을 그 변을 `minCrossDp`만큼 넘은 점으로 보고 판정한다(나감). 단 **그 방향으로 누른 점에서 `minCrossDp` 이상 움직였을 때만**이다(0.28, SRS 0.60 FR-SWP-45 설계 결정: 왼쪽 테두리면 `누른 x − x ≥ m`, 오른쪽 `x − 누른 x ≥ m`, 위 `누른 y − y ≥ m`, 아래 `y − 누른 y ≥ m`) — 테두리에 붙여 누른 탭, 흔들림, 위로 긋다 옆 테두리를 스친 대각선은 나감이 아니다. 다시 키 안으로 들어오는 판정(변 안쪽 `minCrossDp`)은 실제 점으로 한다. 0.26까지는 이런 획이 탭(숫자)이 되었다(실기기 `FSSStroke`: key=1 (68,39.6)→(68,0.0) = "1").
- **대각선(FR-SWP-04):** 한 표본 사이에 두 축이 함께 OUT이 되면, 직전 표본과 이번 표본을 잇는 선분에서 각 축이 문턱(`변 ± m`)에 닿는 비율 `f`를 구해 작은 쪽을 나감 변으로 한다. 같으면 가로 변(LEFT·RIGHT)을 고른다(D-04). 들어옴도 같은 방식(나중에 IN이 된 축)으로 정한다.
- 기록 시각 `t`는 조건을 만족한 표본의 시각이다(보간하지 않는다, D-05).

### 3.3 함수

```kotlin
class EdgeCrossingTracker(val key: KeyGeom, val minCrossDp: Float) {
    fun begin(p: StrokePoint)
    fun add(p: StrokePoint): Crossing?          // 이번 표본에서 생긴 기록(최대 1개), 없으면 null
    val crossings: List<Crossing>
    val zone: ZoneState
    fun copy(): EdgeCrossingTracker             // 진행 상태 탐침용(LLD-04 5절)
}
```

#### `begin(p)`
- 사후: `zone = (IN, IN)`, `crossings` 빔, 직전 점 = p.
- 오류: 두 번 부르면 새 획으로 다시 시작한다.

#### `add(p): Crossing?`
- 사전: `begin` 뒤. `p.t ≥` 직전 `t`(같은 시각 허용).
- 처리:
  1. 새 X 구역, 새 Y 구역을 3.2 표로 계산한다(현재 구역에서 한 단계씩).
  2. `was = zone.inside`, `now = 새 zone.inside`.
  3. `was && !now` → OUT 기록. 바뀐 축이 하나면 그 변, 둘이면 3.2 대각선 규칙.
  4. `!was && now` → IN 기록. 마지막에 IN이 된 축의 변(둘이 함께면 대각선 규칙과 같은 비율 비교, 큰 쪽이 나중).
  5. 기록이 있으면 `crossings`에 붙이고 돌려준다.
- 오류: 좌표가 NaN이면 그 표본을 버리고 null(D-06).
- 근거: FR-SWP-03, 04, 05, 45, FR-TAP-02b. 시험: EC-01~EC-12.

#### 파생 함수 (확장 함수)
- `firstOut(): Crossing?` — 첫 OUT.
- `countOf(edge): Int` — 그 변의 OUT·IN 기록 수(같은 변 통과 횟수, SWIPE 1.2).
- `nthCrossingOf(edge, n): Crossing?` — 그 변의 n번째 기록.

## 4. StrokeClassifier (gesture-engine)

### 4.1 타입

```kotlin
enum class SwipeKeyKind { CONSONANT, NUMBER, SYMBOL, FUNCTION, EMPTY, CHEONJIIN }   // CHEONJIIN = 천지인 키(0.16)

sealed interface StrokeOutcome {
    data object Tap : StrokeOutcome                                   // 자음 키 탭: 그 키의 자음(FR-SWP-36)
    data class Stroke(val s: VowelStroke) : StrokeOutcome             // 자음 키: 모음 획 하나(→ ↓, 축과 시작·끝 점. 길이 표시 없음, 0.21). 시작 키는 상관없음(0.18)
    data object Reverse : StrokeOutcome                               // 자음 키: ← ↑ 획 = 무시 + 오류 진동(SWIPE 3.2, 0.18)
    data class Element(val e: CjElement) : StrokeOutcome              // 천지인 키: 요소 하나(0.16, 4.7). 조립은 6절
    data class Text(val text: String, val errorVibe: Boolean) : StrokeOutcome   // 숫자·기호 키
}

data class StrokeResult(
    val keyId: String, val kind: SwipeKeyKind, val outcome: StrokeOutcome,
    val downTime: Long,
    val firstCrossTime: Long?,   // 첫 변 통과 시각, 탭이면 null
    val upTime: Long)            // 누른 시간은 판정에 쓰지 않는다(0.12, 누르고 있기 삭제)
```

- 모든 획이 네 결과 중 하나가 된다(0.16: 천지인 키의 `Element`). 0.1의 `Undefined`(DCR-02 미정 동작)는 DCR-02 결정(나갔다 돌아옴·좌우 문지르기 = 그 방향 나감, 기호 키 ↓ = 탭, SRS FR-SWP-42·43)으로 없앴다.

### 4.2 내부 상태 `StrokeState`

0.18: 고정 크기 상태. 점 목록을 갖지 않아 탐침 복사가 싸다.

| 필드 | 뜻 |
|---|---|
| `tracker` | `EdgeCrossingTracker`(숫자·기호·천지인 키의 변 통과) |
| `e1`, `exitPt` | 처음 나간 변과 그 표본 |
| `first`, `last` | 누른 점, 마지막 점(자음 키 변위) |

- 0.17까지의 `firstExcursionOpen`·`maxBeyond`·`longPt`·`extreme`·`maxReturn`·`returnFrozen`·`segStart`·`turnPt`·`thirdPt`·`vAfter*`와 `SEG_DP`(D-07)는 지웠다(변 통과 모음 규칙이 없어짐).

### 4.3 한글 자음 키 판정 (SWIPE 3.1·3.2·3.6 판정 순서, HLD 4.2, 0.18)

`classifyConsonant(s: StrokeState, th: Thresholds): StrokeOutcome` — 순수 함수. 손을 뗄 때와 진행 중에 같은 함수를 쓴다. 누른 점 `first`와 마지막 점 `last`만 본다(중간 경로·키 변·시작 키 무관).

```
dx = last.x − first.x, dy = last.y − first.y
① 탭/획:  max(|dx|, |dy|) < minCrossDp                → Tap
② 축:     |dx| ≥ |dy| ? 가로 : 세로                     (대각선 = 가로)
③ 방향:   가로이고 dx ≤ 0, 또는 세로이고 dy ≤ 0          → Reverse
그 밖:                                                 → Stroke(VowelStroke(축, first, last))   (0.21: 길이 단계 ④ 삭제)
```

- 0.21(이슈 #179): 획의 길이는 판정하지 않는다(SWIPE 3.6.1: 0.34의 5단계 삭제). 0.20까지의 ④ "축 성분 ≥ longDp = 긴 획"과 `VowelStroke.long`은 없앴다. `longDp`는 기준값 파일·튜터 1-2 표본과 안내 탐침 거리(5.2)에만 남는다(HLD 0.40 형식 6의 정리는 따로). 판정 순서 각 경계의 시험: SC-01~SC-05(LLD-11 4.1, SC-05 = 길이를 바꿔도 같은 획).
- 오래 누르고 있어도 결과는 같다(이슈 #142).

### 4.4 둘레 숫자 키 판정 (SWIPE 7장, FR-SWP-42)

`classifyNumber(s, digit: Char, table: NumberKeyTable): StrokeOutcome`

```
if s.e1 == null                    → Text(digit)
n = countOf(e1)
if n ≥ 3 and e1 == TOP             → Text(ScriptTables.SUPER[digit])    // ↑ 문지르기 = 윗첨자(¹ 등, 숫자는 모두 있음)
if n ≥ 3 and e1 == BOTTOM          → Text(ScriptTables.SUB[digit])      // ↓ 문지르기 = 아래첨자
→ Text(table[digit][e1.dir] ?: digit)   // n = 1(나감), n = 2(나갔다 돌아옴), n ≥ 3이고 ←·→(좌우 문지르기) — 모두 그 방향 나감과 같음
                                    // ↑ = 윗글자(FR-SWP-44), ← → ↓ = 배치표. 칸 없음(6 ←, 0.23) = 탭, 오류 진동 없음
```

- 나갔다 돌아옴과 ←·→ 문지르기는 그 방향 나감과 같다(SWIPE 7장, SRS FR-SWP-42, DCR-02 결정). 예: 2 키 ↑ 나갔다 돌아옴 = @, 2 키 ← 문지르기 = `[`(0.23). ↑·↓ 문지르기만 윗첨자·아래첨자다.
- 길게·꺾기·되돌아오기는 숫자 키에서 보지 않는다(SWIPE 7장 표에 단계가 나감·문지르기뿐).
- 화면 끝 방향은 테두리에 닿으면 나감이 되지만(0.27, FR-SWP-45) 배정하지 않으므로 표에 칸이 없다 — 결과는 탭과 같다.
- 윗첨자·아래첨자 표 `ScriptTables`는 [LLD-05A-tap-keyboard.md#2](LLD-05A-tap-keyboard.md#2-탭-자판-동작-표-판정-stroke-core)에 있다(공용).

### 4.5 기호 키 셋(기호3·기호·기호2) 판정 (SWIPE 8장, FR-SWP-43, 0.23)

`classifySymbol(s, table: SymbolKeyTable): StrokeOutcome` — `table = SymbolKeyTable.of(key.id)`

```
if s.e1 == null                  → Text(table.tap)
t = table[e1.dir]                                            // 첫 나감 방향만. 나갔다 돌아옴·문지르기 = 그 방향 나감(DCR-02)
→ Text(t ?: table.tap)                                       // 칸 없음(기호3 ↓) = 탭, 오류 진동 없음. 기호3 ←·기호2 →는 테두리 쪽이라 배정 없음 = 탭(0.27)
```

- 0.22까지의 단계(나감·나갔다 돌아옴·문지르기)와 "↓로 시작한 획 = 탭"은 이슈 #208로 없앴다. ↓는 배정 기호(기호 `-`, 기호2 `'`)다 — 세 키 아래는 모드 키·빈 칸이라 바깥 변이 아니다(SWIPE 0.38 8장 "↓ 허용"). 기호3 ↓는 배정이 없어 탭 `/`.
- 결과는 처음 누른 키로만(FR-SWP-01): 기호 키에서 →로 그어 기호2 키 위로 가도 `)`.

### 4.7 천지인 키 판정 (SWIPE 5.4, FR-SWP-61, 0.16)

`classifyCheonjiin(s): StrokeOutcome`

```
if s.e1 == null                  → Element(DOT)    // ㆍ
if s.e1 == TOP or BOTTOM         → Element(I)      // ㅣ (↓ = ㅣ, 설계 권장)
if s.e1 == RIGHT                 → Element(EU)     // ㅡ
if s.e1 == LEFT                  → Element(DOT)    // 0.27: 왼쪽 테두리에 닿으면 나감(FR-SWP-45), 결과는 탭의 ㆍ
```

- 처음 통과한 변만 본다. 길이·되돌아오기·문지르기·꺾기·세로 이동은 보지 않는다. 탭·긋기 구분과 변 통과 인정은 자음 키와 같은 `EdgeCrossingTracker`(최소 통과 거리, 3절). 바깥 변은 `KeyGeom.build`가 정한다(왼쪽만, 9.2).
- 표시 문자열(`textOf`) = `elementText(e)`: ㆍ / ㅣ / ㅡ. 안내(5절)의 탐침도 같은 판정을 쓰므로 누를 때 위·아래 ㅣ, 오른쪽 ㅡ가 뜨고(왼쪽은 결과가 ㆍ 그대로라 없음), 나간 뒤에는 결과가 바뀌지 않아 다음 글자 후보가 없다(SWIPE 9.1, FR-SWP-65).
- 시험: CK-01~CK-04.

### 4.6 함수

```kotlin
class StrokeClassifier(val key: SwipeKeyDef, val th: Thresholds, val geom: KeyGeom) {   // geom = 뷰가 만든 그 키의 사각형·바깥 변(9.2). 배치 칸에는 사각형이 없음(0.5, T15 코드에 맞춤)
    fun begin(p: StrokePoint)
    fun update(p: StrokePoint): ProgressState        // 5절
    fun finish(p: StrokePoint): StrokeResult
    fun state(): StrokeState                          // 시험·탐침용 읽기
}
```

#### 누른 시간 (0.12, 이슈 #142 오너 결정)
- 누르고 있기(0.10 `held`·`holdCapable`·`tick`)는 없다. 판정은 획의 모양만 보고, 누른 시간·첫 변 통과까지의 시간은 보지 않는다. 오래 누르다 떼면 `Tap`(자음), 오래 누르다 그으면 그 모음이다. `textOf`(5.2 결과 문자열)는 탭이면 키의 자음, 긋기면 그 키의 자음 + 모음 음절이다(0.17. 쌍자음은 쌍자음 키의 자음, 8.1).
- 0.10의 오판(베타 신고: 키 안에서 작게 왕복하다 늦게 문지르면 꺄)은 이 삭제로 사라진다.
- 시험: SC-47~SC-50(0.12에서 다시 씀).

#### `begin(p)`
- 사전: `key.kind ∈ {CONSONANT, NUMBER, SYMBOL}`(기능키·빈 칸은 뷰가 분류기를 만들지 않는다).
- 사후: `StrokeState` 초기화, `downTime = p.t`, `segStart = p`.

#### `update(p): ProgressState`
- 처리:
  1. `c = tracker.add(p)`.
  2. `c`가 첫 OUT이면 `e1`, `exitPt`, `firstCrossTime`, `firstExcursionOpen = true`, `extreme`을 정한다.
  3. `firstExcursionOpen && turnPt == null`이면 `maxBeyond`를 갱신하고 `longPt`를 정한다. `c`가 IN이면 `firstExcursionOpen = false`.
  4. `e1 != null && !returnFrozen`이면 `extreme`, `maxReturn`을 갱신한다.
  5. 구간: `|p − segStart| ≥ SEG_DP`이면 `d = 단위벡터(p − segStart)`로 R5를 보고, 유효하면 `turnPt = segStart`, `returnFrozen = true`. 그다음 `segStart = p`. (`e1`이 TOP·BOTTOM이 아니거나 `turnPt != null`이면 R5를 보지 않는다.)
  6. `c`가 `e1`의 세 번째 기록이면 `thirdPt`.
  7. 각 기준점 뒤 세로 변위 칸을 갱신한다.
  8. `ProgressState.of(this)`(5절)를 돌려준다.
- 오류: 예외는 던지고 뷰의 `EngineGuard`(LLD-09)가 잡는다.
- 성능: 점마다 O(1). 탐침 포함 NFR-02 목표(개발 PC 0.1ms)는 시험 PF-01로 잰다.

#### `finish(p): StrokeResult`
- 처리: `update(p)` 뒤 `outcome` = 키 종류에 따라 `classifyConsonant`(0.18) / `classifyNumber` / `classifySymbol` / `classifyCheonjiin`. `upTime = p.t`.
- 근거: FR-SWP-01~05, 10~18, 42~45. 시험: SC-01~SC-46, NK-01~NK-15, SK-01~SK-06.

## 5. ProgressState (gesture-engine)

### 5.1 타입

```kotlin
data class Candidate(val dir: Dir, val offsetDp: Float, val label: String, val kind: ProbeKind)
enum class ProbeKind { RING, RUB, DOT, LONG }   // 0.23: STAGE2·STAGE3(기호 키 단계) 삭제. 0.18: 자음 키 변 통과 모음 탐침(OUTER·CONTINUE·VERTICAL·RETURN_LONG·TURN) 삭제, 획 안내 DOT·LONG
data class ProgressState(
    val current: String,            // 지금 떼면 나올 이번 획 하나의 판정 결과(자음 키는 그 키의 음절, 탭이면 자음, 0.17). 조합 결과는 넣지 않음
    val candidates: List<Candidate>,
    val beforeExit: Boolean)        // 아직 변을 통과하지 않음(방향 안내를 그림, 지나면 다음 글자 안내, LLD-06)
```

### 5.2 탐침(probe) 방식

다음 동작별 결과를 **판정과 같은 코드로** 얻기 위해, 현재 상태의 복사본에 가상의 이동(다리 목록)을 넣어 `classify*`를 다시 부른다. 결과가 현재와 다르고 성립하면 후보로 낸다. 결과가 그대로면 내지 않는다 — 그래서 없는 조합 방향에는 아무것도 뜨지 않는다(FR-DSP-05).

가상 점은 2dp 간격, 시각은 `last.t`로 둔다(시각은 판정에 쓰지 않음). `ε` = 1dp.

- **0.18 자음 키(획 안내, SWIPE 9.1·9.2):** 자음 키에는 아래 표 대신 `DOT`·`LONG` 탐침 넷만 켠다. ↓·→마다 누른 점에서의 변위가 점 길이((minCrossDp + longDp) / 2)·긴 획 길이(longDp + ε)가 되도록 다리를 더하고, 결과 글자(미리보기)가 비었거나(무시될 획) 지금과 같으면 내지 않는다. 표시 거리 = 손가락에서 그 길이까지 남은 거리. ← ↑는 없다(SWIPE 3.2). 시험 PS-01·PS-02. 아래 표의 RING·RUB·STAGE는 숫자·기호 키만, OUTER·CONTINUE·VERTICAL·RETURN_LONG·TURN 행은 0.18에서 지웠다. 0.21: 길이로 결과가 갈리지 않으므로 두 탐침은 대개 같은 글자를 내고(같으면 지금 글자와 비교해 걸러짐), 결과가 달라지는 곳은 낱모음 세로 하나 뒤 가로 획(ㅏ → ㅗ, 상대 크기)뿐이다. 미리보기는 매퍼의 `consonant`(9.3 down에서 넣음)를 쓴다. 같은 방향에서 점·긴 획 탐침이 같은 글자를 내면 가까운 쪽(점) 하나만 남긴다(`probeCandidates`의 (방향, 글자) 중복 제거, SWIPE 9.1 "dp 거리로 결과를 나누지 않는다", 검수 PR #183 2회차, 시험 PS-01·PS-01b). 탐침 표 자체를 위치(다음 획 자리) 기반으로 바꾸는 일은 LLD-06·SWIPE 9.1 "다음 획 자리 표시"와 함께 따로 한다(6.21 남은 일).

| 탐침 | 켜지는 상태 | 다리 | 표시 방향 | 표시 거리 |
|---|---|---|---|---|
| RING(d) | `e1 == null`, 네 방향 | d로 (변까지 + m + ε) | d | 키 변 바로 밖 |
| RUB | `e1 != null` | 손가락이 밖이면 (−u로 안쪽 m + ε까지) 뒤 (u로 밖 m + ε까지), 안이면 (u로 밖 m + ε까지) | **첫 다리의 방향**: 밖이면 e1.dir의 반대("되돌아오는 쪽"), 안이면 e1.dir(0.6, 이슈 #73) | e1 변까지 거리(`abs(beyond(e1))`) |
| RUB(↑/↓) | 숫자 키, `e1 == null` | ↑·↓로 나감·들어옴·나감(d 변 통과 3회) | ↑ / ↓ | 고리 바깥: 변까지 + longDp (D-37) |

- 표시 거리는 모두 **손가락(마지막 점)에서** 잰다. 화면에는 손가락 위치에서 표시 방향으로 그 거리만큼 떨어진 곳에 그린다(`Candidate.offsetDp`, LLD-06 3절). 그래서 RING = 변까지 + m(키 변 바로 밖), OUTER = 변까지 + longDp(변에서 longDp)다.
- 표시 방향은 그 결과를 내려고 **손가락이 다음에 움직일 방향**이다(SWIPE 9.2 "그 동작 방향"). RUB는 손가락이 밖이면 먼저 되돌아와야 하므로 e1.dir의 반대("되돌아오는 쪽", SWIPE 9.2 표), 이미 안으로 돌아왔으면(예: R45 L45 = 가, 다음에 나가면 갸) 다시 나가는 e1.dir이다. 거리는 두 경우 모두 e1 변까지다.
- 고리 바깥 거리(D-37)는 SWIPE 9.1 "영어·숫자·기호 키는 각자의 배치표 글자를 같은 자리에 표시"에 따라 자음 키 OUTER(변에서 longDp)와 같은 자리에 둔다. 0.23부터 기호 키에는 단계 탐침이 없어 RING만 뜨고, 나간 뒤에는 RUB 탐침 결과가 현재와 같아 다음 글자 안내가 없다(SWIPE 9.2).
- 결과 표시 문자열(`StrokeClassifier.textOf`): 자음 키 `Tap` = 그 키의 자음, `Stroke` = 조립기에 이 획을 이었을 때 보일 글자(모음 또는 ㆍ·‥, 생성자의 `preview`, 0.18), `Reverse`·무시될 획 = "". 숫자·기호 키는 `Text.text`, 천지인 키는 요소.
  - `StrokeClassifier(key, th, geom, preview: (VowelStroke) -> String? = { null })`: 긋기 자판은 `mapper::preview`, 튜터 연습 자판은 입력 표시(`PracticeEcho.preview`)를 넘긴다.
- `current`와 후보는 **이번 획 하나의 `classify*` 결과**다. 조합 결과는 넣지 않는다(각 다음 ㄱ 키 → 획 중 `current` = "가", 뗀 뒤 편집기도 "각가", 0.17. SRS FR-DSP-03, SWIPE 9.2, HLD 4.2, DCR-16 결정. 0.15부터 긋기는 병합하지 않는다). `ProgressState`는 조합기를 보지 않고, 자음 키의 글자만 매퍼의 미리보기(조립 결과)로 얻는다(0.18). 안내는 모든 자음 키에서 띄운다(9.3, LLD-06).
- 숫자 키의 ←·→ 문지르기·나갔다 돌아옴과 기호 키·기호2 키의 나갔다 돌아옴·문지르기는 결과가 그 방향 나감과 같으므로(4.4·4.5) 탐침 결과가 현재와 같아 후보로 뜨지 않는다.
- SWIPE 9.2 표의 9행이 이 탐침으로 나오는지를 PS-01~PS-09로 시험한다.

#### `ProgressState.of(c: StrokeClassifier): ProgressState`
- 처리: 1) `current` = 지금 상태의 `classify*` 결과 문자열(`textOf`). 2) 위 표에서 켜진 탐침마다 `c.state().copy()`에 다리를 넣고 결과를 비교. 3) `beforeExit` = 자음 키는 결과가 `Tap`인 동안(0.18: 변위가 minCrossDp 미만), 그 밖의 키는 `e1 == null`.
- 오류: 탐침 중 예외는 그 탐침만 버린다(안내는 판정에 관여하지 않음, HLD 1장).
- 근거: HLD 4.2, FR-DSP-02~05. 시험: PS-01~PS-18(안으로 돌아온 뒤 RUB 방향 PS-17, 고리 바깥 거리 PS-18).

## 6. SwipeInputMapper (gesture-engine)

0.18(이슈 #173, HLD 4.3, SWIPE 3·5장): **탭 = 그 키의 자음, 모음 = 필순 획 조립.** 쌍자음 키는 일반 자음 키와 같은 경로(그 키의 자음이 쌍자음), 두 번 탭 = 자음 둘, 시간 판정 없음(이슈 #168 그대로). 상태 = 모음 획 조립기(`VowelStrokeAssembler`, 6.3) + 천지인 조립기(`CheonjiinAssembler`). 두 조립은 섞이지 않는다(한쪽 입력이 오면 다른 쪽을 닫음, SWIPE 5.4). 패키지 `kbd.gesture.input`.

### 6.1 타입

```kotlin
sealed interface MapResult {
    data class Jamo(val events: List<JamoEvent>, val errorVibe: Boolean = false) : MapResult   // errorVibe: 이 입력이 조립을 닫으며 모음이 못 된 획을 버림(SWIPE 3.3)
    data object Ignored : MapResult                                   // 반대 방향·이어질 수 없는·위치가 맞지 않는 획, 이어질 수 없는 천지인 요소: 입력 없음 + 오류 진동
    data class Text(val text: String, val errorVibe: Boolean) : MapResult
}
data class SwipeMapperSnapshot(val cj: CjSnapshot = CjSnapshot(emptyList()), val strokes: StrokeSnapshot = StrokeSnapshot(emptyList()))
class SwipeInputMapper(val th: Thresholds) {
    var consonant: ConsonantBox?               // 0.21: 조합 중 자음이 있으면 C(`SwipeLayout.consonantBox`), 없으면 null. 부르는 쪽이 획마다 넣음(9.3, LLD-07 2.3)
    fun accept(r: StrokeResult): MapResult
    fun reset(): Boolean                       // 두 조립 닫기. true = 모음이 못 된 획을 버림(부르는 쪽이 진동, D-57)
    fun pendingGlyph(): String                 // ㆍ·‥(모음 획 조립 또는 천지인 조립)
    fun pendingReplacesVowel(): Boolean        // 0.21 2회차: 낱모음 ‥ 상태면 true — 앱이 조합기 글자의 마지막 글자(ㅣ / ㅡ) 자리에 ‥를 보임(LLD-08 4절 `show(…, replacesLast)`, SWIPE 0.36 3.4)
    fun preview(s: VowelStroke): String?       // 획 안내·현재 글자(5.2): 이 획을 이었을 때 보일 글자, null = 무시될 획
    val strokes: List<VowelStroke>             // 열린 모음 획(획 자국 9.4가 그 수에 맞춤)
    fun popElement(): StrokePop                // ⌫: 열린 조립에서 하나 빼기
    fun snapshot(): SwipeMapperSnapshot; fun restore(s: SwipeMapperSnapshot)
}
```

### 6.2 함수

#### `accept(r: StrokeResult): MapResult`

| `r.outcome` | 처리 | 결과 |
|---|---|---|
| 자음 키 `Tap` | 두 조립 닫기(모음 획 조립이 획을 버렸으면 `errorVibe`) | `Jamo([Consonant(c)], errorVibe)` |
| 자음 키 `Stroke(s)` | 천지인 조립 닫기(조용히), `vs.push(s, consonant)`(0.21) | `Started(v)` → `Jamo([Vowel(v)])`, `Changed(v)` → `Jamo([ReplaceVowel(v)])`, `Pending` → `Jamo([])`(앱이 `pendingGlyph()` 표시), `Ignored` → `Ignored` |
| 자음 키 `Reverse` | 조립 그대로(SWIPE 3.2) | `Ignored` |
| 숫자·기호 키 `Text` | 두 조립 닫기 | `Text(text, errorVibe ∨ 버림)` |
| 천지인 키 `Element(e)` | 모음 획 조립 닫기(버리면 진동), `cj.push(e)` | 0.16과 같음. 버림이 있었으면 `errorVibe`(무시된 요소면 `Jamo([], true)`) |

- `c` = 그 키의 자음(쌍자음 키면 쌍자음). 시작 키는 모음 획에 영향이 없다.
- 천지인 조립의 ㆍ·‥는 닫힐 때 말없이 버린다(FR-HGL-04e, 0.16 그대로). 오류 진동은 모음 획 조립만(SWIPE 3.3 "조립 끝").
- 시험: SM-01~SM-22, SM-40~SM-45, CJ-50~CJ-60(LLD-11 4.4).

#### `popElement(): StrokePop`
- 모음 획 조립이 열려 있으면 `vs.pop()`(획 하나), 아니면 천지인 조립 `cj.pop()`(요소 하나, `undoComposer` = 빼기 전 상태가 모음). 둘 다 비면 `Empty`(조합기의 ⌫, 닫힌 모음은 바뀐 단계 하나씩 = 조합기 단위, SWIPE 3.5).
- 앱(LLD-08 6.1)은 `Popped(undoComposer = true)`면 `composer.undoLast()`, `false`면 조합기 그대로 + 임시 글자만 다시 보인다.

#### `reset(): Boolean`
- 두 조립을 닫고, 모음이 되지 못한 획을 버렸으면 true. 부르는 곳: 스페이스·엔터·탭·모드 키(탭·↑, 0.22)·커서·입력창(LLD-08 6절). 사용자가 누른 키(스페이스·엔터·탭·모드 키)일 때만 진동하고, 커서 이동·입력창 바뀜은 조용히 버린다(LLD 결정 D-57).

#### `snapshot()`, `restore(s)`
- 두 조립기의 값(LLD-09). 시험: SM-45.

### 6.3 VowelStrokeAssembler (core-hangul `kbd.hangul.stroke`, SWIPE 3.3·3.4·3.5·3.6)

```kotlin
enum class StrokeAxis { HORIZONTAL, VERTICAL }
data class VowelStroke(val axis: StrokeAxis, val x0: Float, val y0: Float, val x1: Float, val y1: Float) {   // 화면 dp. 0.21: long 삭제
    val span: Float                              // 축 방향 길이. 상대 크기 규칙 두 곳(낱모음 ㅏ / ㅗ, ㅓ / ㅢ 0.5)에만 씀
}
data class ConsonantBox(val left: Float, val top: Float, val right: Float, val bottom: Float)   // 0.21: C, 획과 같은 화면 dp
sealed interface StrokePush { Started(v); Changed(v); Pending; Ignored }
sealed interface StrokePop { Empty; Popped(undoComposer: Boolean) }
data class StrokeSnapshot(val strokes: List<VowelStroke>, val consonant: ConsonantBox? = null)
class VowelStrokeAssembler(var tol: Float) {   // tol = posTolDp(SRS 1.3, 기본 20dp = 키 폭 51 × 0.4)
    val consonant: ConsonantBox?                 // 열린 조립의 C(첫 획 때 정해져 닫힐 때까지 그대로)
    fun push(s, c: ConsonantBox? = null): StrokePush; fun preview(s, c: ConsonantBox? = null): String?
    fun pop(): StrokePop; fun close(): Boolean
    val vowel: Jamo?; val pendingStrokes: Int; fun pendingGlyph(): String; val strokes; snapshot/restore
}
```

- **상태 = 획 열 + C**(받아들인 획만, C는 조립의 첫 획 때 받은 값). 화면 글자는 이 둘의 순수 함수다(⌫·복구가 쉬움). C는 자음 탭·⌫·조립 닫기에서만 바뀌므로(SWIPE 9.5) 열린 조립 동안 고정한다. 획이 다 빠지거나 닫으면 C도 비운다.
- **상태 기계(SWIPE 3.6.2 다음 후보 표 정본, 0.21):** 획마다 지금 상태 + 새 획 → 다음 상태 또는 없음(= `Ignored`). 상태 하나에 후보 집합이 하나이고, 같은 획 열이 둘 이상의 상태가 될 수 없으므로 보이는 모음은 늘 많아야 하나다(0.20의 "완성 모음 하나" 검사 VS-13은 구조로 대신함). 상태와 보이는 모음:

| 상태 | 보임 | 다음 세로 획(↓) | 다음 가로 획(→) |
|---|---|---|---|
| (빈) | – | → **V1**(C가 있어도, 0.26: 두 역할 후보) | → **H1**(C가 있어도) |
| V1 (세로 하나) | 낱모음 ㅣ. C: 오른쪽 위 ㅣ, 아래 왼쪽 ㆍ | 역할 점 + 옆(①) → VV | C 있음: 오른쪽(①)만 → A, 밑줄 ㅗ(①)만 → O, 둘 다 → ①의 구역(오른쪽 위 A, 아래 왼쪽 O), 둘 다 아님 → 무시. 낱모음: 상대 크기 `h.span ≤ ①.span` → 오른쪽(①)이면 A, `>` → 밑줄 ㅗ(①)이면 O. 위치가 안 맞으면 다른 쪽으로 넘기지 않고 무시 |
| H1 (가로 하나) | 낱모음 ㅡ. C: 아래 왼쪽 ㅡ, 오른쪽 위 ㆍ | C 있음: `x < ①.L − t` → 무시, 아래 왼쪽(점) → U, 오른쪽 위(ㅣ) → EOUI(x 경계 `R − t`도, 시작 높이 조건도 안 봄, D-63). 낱모음: `x ≥ ①.R − t` → EOUI. `①.L − t ≤ x < ①.R − t`: `T ≥ ①.y − t/2` → U, 아니면(위에서 가로지름) EOUI. `x < ①.L − t` → 무시 | 밑(①) + 좌우 겹침(`L ≤ ①.R + t/4, R ≥ ①.L − t/4`) → HH |
| A | ㅏ | 역할 ㅣ + 끝(②) + `x ≥ ①.x + t` → AE | 오른쪽(①) + `y ≥ ②.y + t/2` → YA |
| YA | ㅑ | 역할 ㅣ + `x ≥ max(②.R, ③.R) − t` + `x ≥ ①.x + t` → YAE | – |
| O | ㅗ | 역할 ㅣ + 끝(②) → OE | – |
| OE | ㅚ | – | 오른쪽(③) → WA |
| WA | ㅘ | 역할 ㅣ + 끝(④) + `x ≥ ③.x + t` → WAE | – |
| VV | ‥ (①이 ㅣ로 보였으면 — 낱모음, C의 오른쪽 위 — 조합기는 그 ㅣ를 갖고 화면만 ‥, `pendingReplacesVowel`, D-64. ①이 ㆍ로 보였으면 ‥만) | – | 밑줄 ㅛ(①, ②) → YO |
| U | ㅜ | C 있음: 역할 점 + `x ≥ ②.x + t/2` → YU, 역할 ㅣ + 끝(①) → WI. 낱모음: 끝(①) → WI, 아래(①) + `x ≥ ②.x + t/2` → YU | 밑(①) + `L ≥ ②.x − t/2` → UDOT |
| UDOT | ㅜ + ㆍ | 역할 ㅣ + `x ≥ ③.L − t`(끝 또는 가로지름) → WEO | – |
| WEO | ㅝ | 역할 ㅣ + 옆(④) → WE | – |
| EOUI | ㅓ 또는 ㅢ(아래) | 역할 ㅣ + 옆(②) → E | – |
| HH | ‥ (①이 ㅡ로 보였으면 ㅡ를 가린 채, D-64. ㆍ로 보였으면 ‥만) | 역할 ㅣ + `x ≥ min(①.L, ②.L) − t` → YEO | – |
| YEO | ㅕ | 역할 ㅣ + 옆(③) → YE | – |
| AE YAE WAE YO YU WI WE E YE | 그대로 | – | – |

  ("역할 ㅣ / 점"은 C가 있을 때만 그 획의 구역으로 보고(오른쪽 위 = ㅣ, 아래 왼쪽 = 점), 낱모음은 늘 맞음. ①②…는 획 순번. 0.26: C 전용 첫 획 상태 IC·VDOT·EUC·HDOT 삭제.)
- **C 역할(SWIPE 0.43 3.6.3 두 구역, 0.26):** 가운데 점 P = 시작·끝 평균(`midX`, `midY`), `d = (P.y − C.B) − (P.x − C.R)`(C 오른쪽 아래 모서리를 지나는 45° 선). `d > 0` = 아래 왼쪽(세로 획 = 세로 점, 가로 획 = ㅡ), `d ≤ 0` = 오른쪽 위(세로 획 = ㅣ, 가로 획 = 가로 점). **버리는 획은 없다.** 첫 획은 C가 있어도 낱모음과 같은 상태 `V1`/`H1`이고(두 역할 후보), C의 구역은 ① 첫 획 하나의 보임(`vowelOf`: `V1` 오른쪽 위 = ㅣ, 아래 왼쪽 = ㆍ; `H1` 아래 왼쪽 = ㅡ, 오른쪽 위 = ㆍ) ② ㅏ / ㅗ 동점(`V1` + 가로 획: 오른쪽만 맞으면 ㅏ, 밑줄만 맞으면 ㅗ, 둘 다 맞으면 첫 획의 구역) ③ 한 획 닫기(보임이 ㆍ면 버림 + 진동)에만 쓴다. 둘째 이후 세로 획은 그 획의 구역으로 역할을 정한다(`role`: ㅛ ② = 아래 왼쪽, ㅐ·ㅚ·ㅟ … = 오른쪽 위). `H1` + 세로 획(C 있음): x ≥ H.L − t이면 아래 왼쪽 = ㅜ, 오른쪽 위 = ㅓ·ㅢ(끝·가로지름). ㅓ / ㅢ는 첫 가로 획의 구역과 상관없이 3등분 + 0.5(`eoOrUi`). `V1`/`H1` + 같은 축 둘째 획(ㅛ ②·ㅕ ②)은 첫 획을 점으로 다시 정한다 — 첫 획이 ㅣ / ㅡ로 보였으면 낱모음처럼 조합기는 그 모음을 두고 화면만 ‥(`hidesVowel`), 점으로 보였으면 ‥만.
- **앞 획 기준 구역(SWIPE 3.6.3, 쪽·높이만, 0.21):** S = 세로 획(x, T, B), H = 가로 획(L, R, y). 오른쪽(S, h): `S.T − t/4 ≤ h.y ≤ S.B + t/4`, `h.R ≥ S.x + t/2`(L은 안 봄). 밑줄 ㅗ(점 d, h): `h.L ≤ d.x + t`, `h.R ≥ d.x − t`, `h.y ≥ (d.T + d.B)/2`. 밑줄 ㅛ(d₁, d₂, h): `h.y ≥ max(d₁.B, d₂.B) − t/2`, `h.R ≥ d₁.x − t`, `h.L ≤ d₂.x + t`. 옆(P, v): `v.x ≥ P.x + t/2`, `v.T ≤ P.B + t/4`, `v.B ≥ P.T − t/4`(겹치는 양은 안 봄). 끝(H, v): `v.x ≥ H.R − t`(높이·거리 상한 안 봄). 밑(H, h): `h.y ≥ H.y + t/2`. 아래(낱모음, H, v): `H.L − t ≤ v.x < H.R − t`, `v.T ≥ H.y − t/2`.
- **ㅓ / ㅢ (EOUI의 보임, D-62 개정):** C가 있어도 첫 가로 획의 구역과 상관없이(0.26, SWIPE 0.43) `r = (①.y − ②.T) / (②.B − ②.T)`가 `r < 2/3`이면 ㅓ, 아니면 `①.span < ②.span × 0.5`이면 ㅓ(TBD-22), 그 밖 ㅢ. 경계 r = 2/3은 ㅢ 쪽(`≥`). ㅔ③(EOUI + 옆)은 보임과 상관없이 받는다(ㅢ로 보였던 ②도 ㅔ).
- **받기(`push`):** 위 표로 다음 상태가 있으면 받고, 없으면 `Ignored`(SWIPE 3.6.1 4단계, 3.3 "이어질 수 없는 획", TBD-21 = 쪽·순번·역할 어긋남도 여기서 무시).
- **보이는 모음:** 획 열의 가장 긴 접두 중 모음인 상태(EOUI는 위 규칙)의 모음을 조합 글자로 두고, 그 뒤 획 수만큼 ㆍ(1)·‥(2 이상)를 붙인다(ㅜ + 가로 점 = "구ㆍ", C의 세로 점 둘 = "ㄱ‥"). 낱모음 첫 획은 ㅣ / ㅡ로 보이고 둘째 획이 역할을 다시 정한다(기 → 고, 그 → 거). C가 있으면 첫 획부터 역할이 정해져 다시 정할 일이 없다. **낱모음 ‥(0.21 2회차, SWIPE 0.36 3.4, 설계 결정 #192, D-64):** 낱모음의 세로 둘(VV)·가로 둘(HH)은 화면에 "‥"만 보인다. 조합기는 첫 획 때 받은 ㅣ / ㅡ를 그대로 갖고(사건 없음, `Pending`), 조립기가 `pendingGlyph() = "‥"`·`pendingReplacesVowel = true`를 내면 앱이 조합 글자의 마지막 글자(그 ㅣ / ㅡ) 자리에 ‥를 그린다. ⌫로 둘째 획을 빼면 다시 ㅣ / ㅡ가 보이고(`Popped(false)`), 조립이 닫히면(탭·스페이스·커서 등) ‥가 빠지고 ㅣ / ㅡ가 남는다(진동, SWIPE 3.3). 조합기에 "모음 빼기" 사건을 두지 않은 이유: 스페이스 등은 조합을 확정한 뒤 조립을 닫으므로(LLD-08 6.1), 빼 둔 모음을 닫을 때 되돌리려면 모든 닫는 경로의 순서를 바꿔야 한다.
- **조합기 이벤트:** 조합기에 보인 모음이 없다 → 생김 = `Started`, 바뀜 = `Changed`(`ReplaceVowel`), 같음 = `Pending`. ⌫(`pop`)은 마지막 획을 빼고 보인 모음이 바뀌었으면 `undoComposer = true`. `close()`는 모음이 못 된 획이 있었으면 true(C의 점 하나·둘, 구ㆍ, 낱모음 VV·HH).
- **상수(`VowelStrokeAssembler` companion, SWIPE 3.6.3 "상수 한곳"):** t의 배수 1, 1/2(`HALF`), 1/4(`QUARTER`), `EO_UI_SPLIT = 2/3`, `EO_SHORT_RATIO = 0.5`. 길이에 따라 줄거나 느는 여유(0.20의 D-58 `m = min(t, len/3)`), 키 폭 거리 상한(`R + 키 폭`, `키 폭 × 1.5`), 닿음 조건(`ㅣ.T ≤ y + t/4` 등), 절반 겹침은 모두 없앴다(오너 결정 2026-11-01, 이슈 #179).
- **C가 있을 때 쓰지 않는 것(SWIPE 3.6.3):** ㅏ / ㅗ 상대 크기와 동점 규칙, ㅜ / ㅓ·ㅢ·ㅠ / ㅟ의 x 경계(`H.R − t`), 낱모음 가로지름의 시작 높이 조건(`T < H.y − t/2`). C 규칙과 상대 크기가 부딪치는 두 경우(C 옆 짧은 ㅣ + 긴 가로 = ㅏ, C 아래 긴 점 + 짧은 가로 = ㅗ)는 C로 정한다(SWIPE 3.6.5 (가)).
- 시험: VS-01~VS-14(LLD-11 2.3): 21모음 낱모음·C 각각, SWIPE 3.6.5 (나) 58행(VS-02)·(가) 36행(VS-14, 0.26), 사건·임시 글자·무시·⌫·닫기, C 두 구역의 대각선 경계·낱모음 구역 경계 1dp(VS-08), 길이를 바꿔도 같음(VS-11), ㅓ/ㅢ 3등분·0.5 경계(VS-12), C 고정(VS-13), 미리보기·스냅숏.

## 7. HangulComposer (core-hangul)

### 7.1 타입

```kotlin
data class ComposePolicy(val name: String, val merge: VowelMergeTable, val mergeConsecutiveVowel: Boolean) {
    companion object {
        val DUBEOLSIK = ComposePolicy("dubeolsik", MergeTables.DUBEOLSIK, true)
        val SWIPE     = ComposePolicy("swipe",     MergeTables.NONE,      false)   // 0.15: 두벌식 오토마타, 모음 병합 없음(FR-SWP-20·21·34, TBD-18 = B)
        val GRAFFITI  = ComposePolicy("graffiti",  MergeTables.NONE,      false)
    }
}
data class Syllable(val cho: Jamo?, val jung: Jamo?, val jong1: Jamo?, val jong2: Jamo?)
sealed interface BackspaceResult {
    data class Handled(val result: ComposeResult) : BackspaceResult
    data object NothingComposing : BackspaceResult      // 앱이 확정 글자 한 글자를 지운다
}
data class ComposerSnapshot(val units: List<JamoEvent>)
class HangulComposer(val policy: ComposePolicy,
                     private val onInvalid: (String) -> Unit = {})   // 잘못된 이벤트 보고(D-09). 앱이 ErrorLog로 넘김(G5)
```

- **입력 단위 목록 `units`:** 조합 중 글자를 만든 이벤트를 차례로 담는다. 지금 상태는 늘 `fold(units)`다(빈 상태에서 `units`를 차례로 `step`). **불변식:** `fold(units)`는 도중에 확정을 일으키지 않는다(확정이 일어나면 그 순간 목록을 새로 시작하기 때문).
- `ReplaceVowel`은 **자기 자신을 단위로 쌓는다**(0.16, 이슈 #160, SRS FR-SWP-64 "닫힌 뒤에도 요소 하나씩", HLD 4.3 "그 요소는 조합기 입력 단위 목록에서 하나씩"). 접을 때 그 시점 글자의 중성을 v로 바꾸므로(`step` 그대로) 병합된 중성(과)에서도 확정이 일어나지 않아 불변식을 지킨다. 그래서 천지인 요소(ㅣ = `Vowel`, 이어지는 요소마다 `ReplaceVowel`)가 하나씩 단위가 되고, 조립기가 닫힌 뒤의 ⌫도 요소 하나씩이다(간 ⌫ = 가, ⌫ = 기, ⌫ = ㄱ). 0.15까지의 "모든 모음 단위를 `Vowel(v)` 하나로 바꿈"(G8)은 지웠다. 조립기가 열려 있을 때의 ⌫는 조립기가 먼저 빼고 조합기는 `undoLast`로 따라간다(6절 `popElement`, LLD-05 6절).
- `UndoLast`는 단위로 쌓지 않는다.
- **0.14에서 지운 것(이슈 #150):** 긋기 연속 입력 run(`RunStart`, `base`·`events`·`pending`, `RunSnapshot`), `MergeVowel`(나눠 쓰기 병합은 `mergeConsecutiveVowel` + 정책 병합 표가 정함), `BackspaceMode`(`WHOLE_SYLLABLE`이 없어져 모든 정책이 입력 단위 하나씩 지운다). 확정된 글자를 보관하거나 확정 경계를 넘어 되돌릴 일이 없다(HLD 4.4).

### 7.2 상태 전이 `step(s: Syllable, e: JamoEvent): StepOut`

`StepOut(next: Syllable, commit: Syllable?, newUnits: List<JamoEvent>?)` — `commit`이 있으면 그 글자를 확정하고, `newUnits`로 목록을 새로 시작한다.

| 지금 상태 | Consonant(c) | Vowel(v) |
|---|---|---|
| 빔 | 초성 c | 중성만 v |
| 초성 x | 확정 x, 새 초성 c | x + v |
| 초성 x + 중성 u (받침 없음) | c ∈ JONG_CAPABLE → 받침 c / 아니면 확정, 새 초성 c | `mergeConsecutiveVowel`이고 merge(u,v)=m → 중성 m / 아니면 확정, 새 중성만 v |
| 중성만 u | 확정 u, 새 초성 c | merge 규칙 같음(m이면 교체), 아니면 확정, 새 중성만 v |
| 받침 j 하나 | COMPOUND_JONG(j,c) 있음 → 겹받침 / 아니면 확정, 새 초성 c | 도깨비불: 확정 (x,u), 새 (j, v). `newUnits = [Consonant(j), Vowel(v)]` |
| 겹받침 j1 j2 | 확정, 새 초성 c | 도깨비불: 확정 (x,u,j1), 새 (j2, v). `newUnits = [Consonant(j2), Vowel(v)]` |

- 긋기(0.15, 이슈 #156)는 두벌식과 같은 표를 쓰고 모음 병합만 없다(`MergeTables.NONE`, `mergeConsecutiveVowel` = false): 모음 뒤 모음은 늘 "확정, 새 중성만 v"다(가 + ㅏ = 가ㅏ, 가 + ㅣ = 가ㅣ, 고 + ㅏ = 고ㅏ, 과 + ㅣ = 과ㅣ, 고 + ㅜ = 고ㅜ). 복모음은 한 획 하나라 `Vowel(ㅘ)`처럼 이벤트 하나로 온다. 받침 뒤 모음은 도깨비불이다(각 + ㅣ = 가기, 밖 + ㅏ = 바까, 닭 + ㅣ = 달기). 0.14의 `MergeTables.SWIPE`(고 + ㅏ = 과, 가 + ㅣ = 개)는 지웠다.
- 쌍자음은 0.17부터 쌍자음 키의 `Consonant(ㄲ)`처럼 자음 이벤트 하나로 온다. 쌍받침 ㄲ·ㅆ은 받침이 되고, ㄸ·ㅃ·ㅉ(받침 불가)은 앞 글자를 확정하고 새 초성이 된다(아ㅃ). 받침 ㄱ 뒤 `Consonant(ㄱ)`은 겹받침이 아니라(COMPOUND_JONG에 없음) 확정, 새 초성이다(곡 + ㄱ = 곡ㄱ).
- `DoubleConsonant(b)` = `Consonant(DOUBLE_OF[b])`로 처리하고 단위에는 그대로 쌓는다. `DOUBLE_OF[b]`가 없으면 무시하고 오류를 보고한다(D-09, G9). 0.17부터 긋기 매퍼는 이 이벤트와 `UndoLast`를 내지 않는다(core-hangul 이벤트 형식으로 남김, HC 시험 그대로).
- `ReplaceVowel(v)`: 중성이 있고 받침이 없으면 중성을 v로 바꾸고 모음 단위를 `Vowel(v)` 하나로 바꾼다. 아니면 무시하고 오류 기록(D-09).
- 오류 기록(D-09)은 생성자의 `onInvalid(메시지)`로 한 번 보고한다. core-hangul은 `ErrorLog`(app)를 볼 수 없어서 앱이 조합기를 만들 때 오류 기록 함수를 넘긴다(G5).
- 글자 문자열(`render`): 초성·중성이 있으면 U+AC00 공식, 초성만·중성만이면 호환 자모(`Jamo.compat`), 빔이면 "". 겹받침은 `COMPOUND_JONG`의 결과 자모로 종성 인덱스를 얻는다.

### 7.3 함수

#### `apply(e: JamoEvent): ComposeResult`
- 처리:
  1. `e == UndoLast` → `undoLast()` 결과를 돌려준다.
  2. `out = step(fold(units), e)`.
  3. `out.commit != null` → `committed = render(out.commit)`, `units = out.newUnits ?: [e]`. 아니면 `units += e`(ReplaceVowel은 7.1 규칙).
  4. `ComposeResult(committed, composingText())`.
- 근거: FR-HGL-01~05, HLD 4.4. 시험: HC-06~HC-31.

#### `applyAll(events: List<JamoEvent>): ComposeResult`
- 차례로 `apply`하고 `committed`는 이어 붙인다. 앱은 동작 하나의 이벤트 열을 이 함수로 한 번에 넣고 결과를 한 번 쓴다(`UndoLast` + `DoubleConsonant`의 중간 상태가 화면에 나오지 않음).

#### `undoLast(): ComposeResult`
- `units`가 비면 아무것도 하지 않고, 아니면 마지막 단위를 빼고 `ComposeResult("", render(fold(units)))`. 확정 경계를 넘지 않는다.
- 근거: HLD 4.3 쌍자음 바꾸기. 시험: HC-50, HC-51, HC-53.

#### `backspace(): BackspaceResult`
- `units`가 비면 `NothingComposing`. 아니면 마지막 단위를 빼고 `Handled(ComposeResult("", render(fold(units))))`(모든 정책, 긋기도 자모 하나씩, 오너 결정 2026-10-30, 이슈 #150).
- 확정된 글자는 다시 열지 않는다(목록이 없으므로, FR-CMN-07). 도깨비불 뒤의 단위는 `[Consonant(j), Vowel(v)]`라 가기 ⌫ = 가ㄱ(두벌식과 같음).
- 근거: FR-CMN-07, 07a, HLD 4.4. 시험: BS-01~BS-05, BS-20~BS-22.

#### `lastConsonant(): Jamo?` (0.14)
- 지금 글자 `fold(units)`의 `jong2 ?: jong1 ?: cho`. 겹받침은 뒤 자음, 받침이 없으면 초성, 쌍자음은 그대로(ㄲ). 비었거나 중성만이면 `null`.
- 쓰는 곳: 없음(0.17부터, core-hangul의 공용 조회로 남김). 시험: HC-52.

#### `finish(): String`
- 조합 텍스트를 돌려주고 `units`를 비운다. 앱이 `finishComposingText`와 함께 부른다.

#### 조회·복구
- `composingText(): String`, `isComposing(): Boolean`(`units`가 있음), `hasJong(): Boolean`(지금 상태에 받침이 있음, 그래피티가 씀), `currentVowel(): Jamo?`.
- `snapshot()` / `restore(s)`: `units` 복사(LLD-09). 시험: HC-53.

- 앱에는 조합기가 하나다. 모드가 바뀌면 확정 뒤 새 정책으로 다시 만든다(HLD 2장 "조합 상태는 앱이 가진 조합기 하나", D-10).

## 8. 긋기 자판 배치 데이터 (gesture-engine)

### 8.1 자판 (SWIPE 2.1, FR-SWP-50·51)

```kotlin
data class SwipeKeyDef(val id: String, val col: Int, val row: Int, val colSpan: Int,
                       val kind: SwipeKeyKind, val label: String,
                       val consonant: Jamo? = null, val digit: Char? = null, val function: FunctionKey? = null)
object SwipeLayout { val KEYS: List<SwipeKeyDef>; const val COLS = 7; const val ROWS = 6
    fun consonantBox(widthDp: Float, rowHeightDp: Float): ConsonantBox }   // 0.21: C(SWIPE 9.5)
```

| 줄\열 | 0 | 1 | 2 | 3 | 4 | 5 | 6 |
|---|---|---|---|---|---|---|---|
| 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 |
| 1 | 8 | ㅃ | ㅉ | ㄸ | ㄲ | ㅆ | 9 |
| 2 | 0 | ㅂ | ㅈ | ㄷ | ㄱ | ㅅ | ⌫ |
| 3 | 천지인 "ㆍ ㅡ ㅣ" | ㅁ | ㄴ | ㅇ | ㄹ | ㅎ | ↵ |
| 4 | 기호3 (0.23, 0.22의 ⚙ 빈 칸 자리) | ㅋ | ㅌ | ㅊ | ㅍ | 기호 | 기호2 (0.23) |
| 5 | 한/영 (열 0~1) | | 스페이스 (열 2~4) | | | (빈 칸, 열 5~6) | |

- **쌍자음 줄(0.17, 이슈 #168, SWIPE 2.1 배치안 A):** 줄 1에 ㅃ ㅉ ㄸ ㄲ ㅆ를 그 바탕 자음 바로 위에 둔다(id = 자음 글자 "ㄲ" 등, `kind = CONSONANT`, `consonant = Jamo.GG` 등). 양 끝(열 0·6)은 빈 칸(id `empty_r1c0`·`empty_r1c6`). 쌍자음 키는 위(숫자 줄)·아래(ㅂ 줄)·양옆에 칸이 있어 네 변 모두 안쪽 변이다.
- 키 사각형: 열 폭 = 창 폭 / 7, 줄 높이 = 설정 줄 높이(FR-SET-06, 기본 48dp). 자판 6줄(기본 288dp)은 창 바닥에 맞춘다(바깥 사각형 원점은 LLD-08 7절의 바닥 정렬 위치). 창(가장 높은 자판 6줄)을 꽉 채우므로 0.16까지의 위 빈 띠는 없다. ㅂ 줄 ~ 맨 아랫줄의 화면 위치는 0.16과 같고 숫자 줄만 한 줄 위로 갔다.
- **숫자 줄의 위쪽 긋기(HLD 10장 R2):** 숫자 줄 위는 창 밖(앱 화면)이다. 손가락이 창 위로 나가도 뷰는 그 획의 MOVE를 계속 받아야 한다(DOWN을 받은 뷰에 같은 포인터의 이동이 창 밖 좌표로도 옴, SWIPE 11장 터치 추적). 에뮬레이터 확인(LLD-11 8절 DV-26, 2026-10-31): 숫자 2 ↑ 창 밖까지 = @, ㄲ 키 ㅙ 창 밖까지 = 꽤. 투명 여백 줄은 필요 없다.
- **양 끝 열 한 칸 올림(0.18, 이슈 #173):** 왼쪽 열 1·8·0·천지인·빈 칸, 오른쪽 열 7·9·⌫·↵·빈 칸. 0.18의 설정 키 ⚙(id "settings", `FunctionKey.SETTINGS`)는 0.22(이슈 #205)에서 지웠고 그 칸은 빈 칸 `empty_r4c0`(왼쪽 변 = 창 왼쪽). 설정은 모드 키 ↑로 연다(9.3).
- 천지인 키(0.16, FR-SWP-60): 줄 3 열 0(0.18), id `SwipeLayout.CHEONJIIN_ID` = "cheonjiin", `kind = CHEONJIIN`, 라벨 "ㆍ ㅡ ㅣ". 왼쪽 변은 창 테두리다(닿으면 나감 = ㆍ, 0.27). 뷰는 키 폭의 `LABEL_FIT`(0.8)보다 넓은 라벨을 줄여 그린다(9절, 튜터 연습 자판도 같음).
- 빈 칸(0.18: 줄 4 열 6과 맨 아랫줄 열 5~6, 둘)은 `kind = EMPTY`. 누르면 터치만 받고 입력은 없다. 빈 칸에서 시작한 획은 어디로 움직여도 입력이 없다(빈 띠와 같음, SRS FR-SWP-50, SWIPE 2.1, HLD 4.1, DCR-03 결정). 처리는 9.3.
- **가운데 자음 자리 C(0.21, SWIPE 9.5 "자리와 크기", HLD 6장):** `consonantBox(폭, 줄 높이)` = 위 끝 `1 × 줄 높이 + 4dp`(쌍자음 줄 위 끝 + 4), 높이 `2 × 줄 높이`(기본 96dp, 줄 높이 설정 FR-SET-06에 따라 함께 줄어듦), 너비 `높이 × 0.85`, 왼쪽 끝 `폭 / 2 − 너비 / 2`(**가로 중심 = 자판 가로 중심** = 가운데 ㄸ 열 중심, 0.24, 이슈 #213. 0.23까지는 `1.25 × 열 폭` = ㅃ 열 왼쪽 끝 + 키 폭 × 0.25). 360dp·48dp 줄에서 ≈ (139.2, 52)–(220.8, 148)(SWIPE의 계산은 C.L = 139, C.R = 221로 반올림). 가로 화면도 같은 식(키 폭만 넓어지고 C 너비는 그대로, 가운데). 뷰가 그리는 자리(9.5)와 조립기 판정 기준(6.3)이 이 함수 하나를 쓴다(`SwipeGeometry.consonantBox`). 상수는 `SwipeLayout` 안 한곳. 시험 `SL-08`: 세로·가로·낮은 줄(셋 다 가로 중심 = 폭 / 2), 네 모서리가 자음 키 위.
- 시험 `SL-01`: 7×6, 자음 키 19개(쌍자음 키 포함) 모두 네 방향 이웃이 있고 바깥 변이 없다(FR-SWP-51), 쌍자음 키는 바탕 자음 바로 위, 빈 칸 셋(0.22: (0, 4) ⚙ 자리 포함), 천지인 키 (0, 3), ⚙ 없음, 양 끝 열(0.18). `SL-02`: 모드 키가 맨 아랫줄(줄 5) 왼쪽(SRS 3.3). `KH-06`: 7열 창의 바깥 변.

### 8.2 둘레 숫자 배치표 `NumberKeyTable` (SWIPE 7장)

데이터 형식: `Map<Char, Map<Dir, String>>`. 값은 SWIPE 7장 표를 그대로 옮긴다(빈칸 = 화면 끝, 키 없음).

| 숫자 | ↑ | ← | → | ↓ |
|---|---|---|---|---|
| 1 | ! | – | ` | · |
| 2 | @ | [ | ] | ※ |
| 3 | # | { | } | ± |
| 4 | $ | ; | : | € |
| 5 | % | _ | + | × |
| 6 | ^ | (배정 없음 = 탭, 0.23) | ~ | ÷ |
| 7 | & | \ | – | ≠ |
| 8 | * | – | \| | ∞ |
| 9 | ( | ₩ | – | ≈ |
| 0 | ) | – | … | ° |

### 8.3 기호 배치표 `SymbolKeyTable` (SWIPE 8장, 0.23)

형식: `data class SymbolKeyTable(tap: String, exits: Map<Dir, String>)`, 키마다 하나. `SymbolKeyTable.of(id)`: "symbol3" → `SYMBOL3`, "symbol2" → `SYMBOL2`, 그 밖 `SYMBOL`. 0.22까지의 `enum Stage { EXIT, RETURN, RUB }`는 삭제.

| 키 (id, 자리) | 탭 | ← | → | ↑ | ↓ |
|---|---|---|---|---|---|
| 기호3 ("symbol3", 줄 4 열 0, 한/영 키 위) | / | (화면 끝) | = | ? | (배정 없음 = 탭) |
| 기호 ("symbol", 줄 4 열 5) | . | ( | ) | > | - |
| 기호2 ("symbol2", 줄 4 열 6) | , | " | (화면 끝) | < | ' |

- 라벨(FR-SWP-43a): `SwipeKeyDef.label` = 탭 기호(가운데 크게). 뷰가 `exits`를 네 변의 가운데에 작게 그린다(0.27, 오너 #208) — 위 변 가운데 ↑, 왼쪽 변 가운데 ←, 아래 변 가운데 ↓, 오른쪽 변 가운데 →(숫자 키·탭 자판 FR-TAP-03b와 같은 자리, `drawSides`). 없는 방향은 그리지 않는다. 0.26까지는 모서리(오른쪽 위·왼쪽 아래·오른쪽 아래)였다.
- PC 특수 문자 32개가 모두 한 동작이다: 숫자 키 ↑ 10개 + 숫자 키 ←·→ 12개 + 기호 키 셋 12개. 겹침은 `( )`뿐(9·0 ↑와 기호 키 ←·→, SRS TBD-28 A 확정). 시험 SL-10.

## 9. SwipeKeyboardView와 TouchTracker (app)

### 9.1 TouchTracker

```kotlin
class TouchTracker(private val density: Float) {
    var activePointerId: Int = NONE
    fun onDown(e: MotionEvent, index: Int): StrokePoint      // px → dp
    fun pointsOf(e: MotionEvent): List<StrokePoint>          // 활성 포인터의 히스토리 점 + 현재 점
}
```

#### `pointsOf(e)`
- 처리: `idx = e.findPointerIndex(activePointerId)`. `idx < 0`이면 빈 목록. `0 until e.historySize`에 대해 `getHistoricalX/Y(idx, h)`, `getHistoricalEventTime(h)`, 그다음 `getX/Y(idx)`, `eventTime`. 모두 `/density`.
- 근거: HLD 4.1(히스토리 점까지 누적). 시험: 실기기 DV-05.

### 9.2 SwipeKeyboardView

```kotlin
class SwipeKeyboardView(context: Context, private val host: SwipeHost) : View(context)
```

| 함수 | 처리 | 근거 |
|---|---|---|
| `onMeasure` | 폭 = 부모 폭, 높이 = `SwipeLayout.ROWS`(6, 0.17) × 줄 높이 | SRS 3.3 |
| `onLayout`/`rebuildGeometry()` | `SwipeLayout.KEYS`로 `KeyGeom`(dp)과 `KeyHitTester`를 만든다. 창 크기는 루트 뷰 크기(LLD-08 7절)로 바깥 변을 계산 | FR-SWP-45, FR-CMN-12 |
| `onDraw` | 키 배경·글자. 둘레 숫자 키 글자는 배치표 그대로(변 가운데), 기호·기호2 키는 가운데 탭 기호 + 변 가운데 배정 기호(8.3, 0.27) | FR-SWP-50, 43a |
| `onTouchEvent` | 아래 9.3 | FR-CMN-11 |

### 9.3 `onTouchEvent(e)` — 한 획

```
ACTION_DOWN / POINTER_DOWN:
  if tracker.activePointerId != NONE: return true                // 진행 중 획이 있으면 다른 손가락 무시(FR-CMN-11)
  p = tracker.onDown(e, actionIndex); key = hit(p)
  activePointerId = pointerId
  if key.kind != EMPTY: host.feedback.key()                       // 키 입력 진동·소리는 누를 때(FR-CMN-11a ②). 빈 칸은 키가 아니라 내지 않음(FR-CMN-11a ②)
  when key.kind:
    FUNCTION, MODE → modeStroke = TapGestureClassifier(geom, host.thresholds.minCrossDp).begin(p); guide 없음   // 0.22(#205): 누를 때 입력 없음, 뗄 때 판정
    FUNCTION → host.guard.stroke("Swipe.fn") { host.onFunctionKey(key.function) }; guide 없음   // 기능키는 누를 때 처리(FR-CMN-11a ①)
               if key.function == BACKSPACE: repeat.start(pointerId, BACKSPACE)   // RepeatTimer(LLD-05A 6절), fire = host.onFunctionKey(BACKSPACE)
    EMPTY    → 터치만 받음(activePointerId는 잡아 둠), 분류기·안내 없음, 뗄 때도 입력 없음(FR-SWP-50)
    else     → mapper.consonant = if (host.centerConsonant != null) geometry.consonantBox else null   // 0.21: TBD-24, 미리보기(안내)도 이 C로
               classifier = host.guard.run("StrokeClassifier.begin") { StrokeClassifier(key, host.thresholds, geom).also { it.begin(p) } }
               if classifier == null: return true                 // 만들기 실패 = 그 획은 결과 없음(뗄 때도 입력 없음)
               guided = true                                   // 모든 자음 키(쌍자음 키 포함)·천지인 키·숫자·기호 키에서 안내(FR-DSP-01)
               if key.kind == CONSONANT: trail.begin(p)        // 획 자국(9.4, 0.18)
               if guided:
                   guide.onDown(key, keyRect, p)                   // 누른 점 p(dp, t 포함)를 넘김 — onProgress의 이동 거리 기준(LLD-06 3절, 0.5)
                   ps0 = host.guard.run("StrokeClassifier.update") { ProgressState.of(classifier) }   // 상태를 바꾸지 않음(5절)
                   if ps0 != null: guide.onProgress(ps0, p)        // 움직이지 않고 누르고 있다가 표시 지연이 지나도 그릴 현재 글자·방향 안내(FR-DSP-01, 0.5)
ACTION_MOVE:
  modeStroke?.add(각 점)                                              // 0.22: 모드 키 판정
  for p in tracker.pointsOf(e):
      ps = host.guard.run("StrokeClassifier.update") { classifier.update(p) }
      if ps == null: dropStroke(); break                             // 그 획은 버림(손 뗄 때 결과 없음)
      if guided: guide.onProgress(ps, p)
ACTION_UP / POINTER_UP (활성 포인터):
  guide.onUp()
  when key.kind:
    FUNCTION, MODE → s = host.guard.run("Swipe.fn") { ModeKeyGesture.of(modeStroke.finish(p)) == OPEN_SETTINGS }   // 0.22(#205), 실패면 입력 없음
               s == true → host.guard.stroke("Swipe.fn") { host.onSettingsStroke() }     // ↑ 나감 = 설정
               s == false → host.guard.stroke("Swipe.fn") { host.onFunctionKey(MODE) }; host.onFunctionKeyUp(MODE)   // 탭 = 모드 전환
               trail.sync(mapper.strokes.size)
    FUNCTION → repeat.stop(pointerId); host.onFunctionKeyUp(key.function)
    CONSONANT, NUMBER, SYMBOL → if classifier != null:
        host.guard.stroke("Swipe.release") {                          // 스냅숏 → 실행 → 실패면 되돌림(LLD-09 2절)
            when m = mapper.accept(classifier.finish(p)):             // 6.2
                Jamo    → host.onSwipeEvents(m.events)                // 조합기 반영·편집기 쓰기는 이 안에서 마지막
                Ignored → host.feedback.error()                       // 무시된 획·천지인 요소(FR-SWP-12, 14, 62). 진동 설정을 따름(LLD-08 8절)
            // 0.18: 받아들인 모음 획이면 trail.keep(), 아니면 trail.drop(); 이어 trail.sync(mapper.strokes.size). Jamo는 host.onSwipeEvents(events, errorVibe)
                Text    → host.onSwipeText(m.text, m.errorVibe)       // 숫자·기호 키(4.4·4.5). 매퍼는 이미 조립기를 비움
        }                                                             // null(실패)이면 아무것도 쓰지 않고 매퍼도 되돌림
    EMPTY → 없음
  activePointerId = NONE; classifier = null; guided = false
ACTION_CANCEL: 획이 있으면 dropStroke() 아니면 guide.onUp(), 기능키면(떼지 않은 모드 키 빼고, 0.22) repeat.stop + onFunctionKeyUp, activePointerId = NONE
dropStroke(): classifier = null; guide.onUp()                        // 매퍼는 그 획을 본 적이 없음(0.14: press·cancelPress 없음)
```

- `geom`·`keyRect`는 `rebuildGeometry()`(9.2)가 만든 그 키의 `KeyGeom`과 사각형(dp)이다. `guide`(`GuideOverlay`)에는 뷰가 px로 바꾼 사각형을 넘긴다(LLD-06 3절). `guide`는 누를 때마다 뷰가 지금 표시 설정·입력창 제한으로 고른 overlay다(LLD-06 3절 "생성과 다시 만들기", 0.5).
- DOWN의 `ProgressState.of`는 분류기 상태를 바꾸지 않는 탐침이라(5.2) `run`으로 감싼다. 이동 거리 0이라 `GuideTimerLogic.onMove`는 표시를 시작하지 않고, 표시 시작은 표시 지연 타이머가 정한다(LLD-06 2절). 실패(null)면 첫 MOVE까지 그릴 내용 없이 기다린다(이슈 #83).
- 구현 나눔(T30): 위 흐름은 안드로이드 없는 `SwipeTouchLogic`(`kbd.app.swipe.impl`)이 맡고, 뷰는 `MotionEvent`를 `TouchTracker`로 dp 점으로 바꿔 넘긴다. 안내·반복은 `StrokeGuide`·`KeyRepeat` 포트로 받는다(JVM 시험 SW-05, LLD-11 4.5).

- `ACTION_CANCEL`(플랫폼이 진행 중인 모든 터치를 취소): 아직 떼지 않은 획은 입력이 없다. 이미 누를 때 처리한 기능키 입력은 되돌리지 않고 반복만 멈춘다(SRS FR-CMN-11a ③, HLD 4.1, DCR-11 결정). 긋기 자판은 한 획만 받으므로 대기 중인 결과가 없다.

- `mapper`(`SwipeInputMapper`)는 이 뷰가 만들고, 만들 때 `host.registerSwipeState(SwipeMapperState(mapper))`로 앱에 넘긴다(앱 main은 `SwipeEngineState` 인터페이스만 본다, LLD-03 2.2). 뷰가 떨어질 때(`onDetachedFromWindow`) `registerSwipeState(null)`.
  - `class SwipeMapperState(private val m: SwipeInputMapper) : SwipeEngineState` (`kbd.app.swipe.impl`): `reset() = m.reset()`, `pendingGlyph()`·`cheonjiinState`·`popElement()`(0.16), `snapshot() = m.snapshot()`, `restore(s) = m.restore(s as SwipeMapperSnapshot)`(6.2). 0.17에서 `onBackspace`는 지웠다.
- `host.guard.stroke`가 예외를 잡으면 그 획은 결과 없이 버리고 매퍼·조합기 상태를 되돌린다(LLD-09). 스냅숏에는 `SwipeEngineState`가 들어간다.
- 긋기 자판 ⌫ 반복은 이 뷰가 가진 `RepeatTimer`가 맡는다(누를 때 시작, 뗄 때·취소 때 중지).
- 획 결과는 처음 누른 키로만 정한다(FR-SWP-01). 이동 중 다른 키 위를 지나도 hit를 다시 부르지 않는다.

### 9.4 획 자국 `StrokeTrail`·`TrailPainter` (app main, SWIPE 9.4, FR-DSP-09, 0.18)

- `StrokeTrail`(안드로이드 없음): 남긴 경로 목록 + 그리는 중인 경로. `begin(p)`(자음 키 누름), `add(p)`(MOVE·뗌), `keep()`(조립기가 받은 모음 획), `drop()`(탭·무시된 획·취소), `sync(open)`(남긴 경로를 열린 모음 획 수로 자름), `paths`.
- **지우는 때 = 조립이 닫히는 때:** 남긴 경로 수는 늘 매퍼의 열린 획 수(`mapper.strokes.size`)를 따른다. 뗌·기능키 누름 뒤와 그릴 때마다 `sync`하므로 자음 탭·숫자·기호·천지인 키·스페이스·엔터·모드·설정 키는 0으로, 조립이 열린 ⌫는 그 획 하나만 지운다. 커서 이동·입력창 바뀜은 뷰가 감싼 `SwipeEngineState.reset()`이 `postInvalidate()`로 곧 다시 그리게 한다(검수 PR #177).
- `TrailPainter`: 굵기 4dp, 둥근 끝·이음, 흰색(라벨색) 40%(0x66FFFFFF), 키를 그린 뒤 그림(키 위, 안내 팝업 창 아래). 뷰 밖은 잘림. 설정 없음(늘 그림).
- 긋기 자판(`SwipeTouchLogic.trail`, `SwipeKeyboardView.onDraw`)과 튜터 연습 자판(`PracticeKeyboardView.trail`, 입력 표시의 `openStrokes`로 맞춤, LLD-07 2.3)이 함께 쓴다. 숫자·기호·천지인·기능키·빈 칸에서 시작한 획은 `begin`하지 않는다.
- 시험: SW-12·SW-13(SwipeTouchLogic: 남김·지움·⌫), TU-61(연습 자판 `openStrokes`).

### 9.5 가운데 자음 `CenterConsonantPainter` (app main, SWIPE 9.5, FR-DSP-10, 0.21)

0.20의 가운데 음절(`CenterSyllablePainter`, 조합 중 글자 전체를 자판 가운데에 3줄 높이)은 SWIPE 0.35 9.5(이슈 #180 후속)로 버렸다.

- **그릴 글자:** `SwipeHost.centerConsonant`(기본 null) = 조합기의 `lastConsonant()`(겹받침이면 둘째, 받침이면 받침, 없으면 초성. 쌍자음은 그대로, 낱모음·조합 없음 = null). `KeyboardService`는 `dispatcher.composer?.lastConsonant()`. `CenterConsonantPainter.textFor(consonant, fieldInfo)` = 자음이 없거나 `fieldInfo.hidesDisplay`(비밀번호 입력창, FR-CMN-08 ①)면 null. **학습 금지 입력창에서는 그린다**(②, 오너 2026-11-01). 안내 설정(FR-DSP-08)은 보지 않는다. 임시 ㆍ·‥는 그리지 않는다(모음 획은 획 자국이 보여 줌).
- **자리·모양:** C = `SwipeGeometry.consonantBox`(8.1, dp) × 밀도. 글자 크기 = `min(C 높이, C 너비 / 글자 폭(크기 1))`(`sizeFor`), C 가운데 정렬. 색은 획 자국과 같은 흰색 40%(`TrailPainter.COLOR`).
- **층:** `onDraw`에서 키(배경·라벨·둘레 글자) → 가운데 자음 → 획 자국. 안내 팝업은 뷰 위의 창이라 그 위.
- **바뀌는 때:** 뷰는 그릴 때마다 `centerConsonant`를 읽는다. 조합기는 손을 뗄 때(탭·받아들인 획·천지인 키)와 기능키 누름에서만 바뀌므로 긋는 동안에는 그대로다. 모음 획·천지인 키는 마지막 자음을 바꾸지 않는다(가 → "ㄱ" 그대로). 다시 그리는 때는 0.20과 같다(모든 터치 사건, ⌫ 되풀이 콜백, 감싼 `reset()`의 `postInvalidate`). 조합이 비면 null이라 지워진다.
- **판정 C:** 같은 9.3 down에서 `centerConsonant != null`이면 매퍼에 C를 준다(비밀번호 입력창에서도 — 그리지만 않음, SWIPE 9.5 "판정 C는 그대로").
- **튜터 연습 자판:** `PracticeEcho.center: CenterMark?`(`CenterMark(consonant, box)`, 기본 null; `SwipePracticeEcho` = 조합기 `lastConsonant()` + 실제 긋기 기하의 C)를 `TutorFlow.center` → `PracticeKeyboard(center = …)`로 넘긴다. 연습 자판은 입력창이 없어 `textFor(…, null)`. 판정도 같은 C를 쓴다(`SwipePracticeEcho`가 획마다·미리보기마다 `mapper.consonant`를 넣음, 3-1 판정은 `PracticeEcho.consonantPlace`, LLD-07 2.3·2.5). 터치 없이 바뀌는 때는 `redraw = tick`(0.20과 같음).
- 표시 전용: 터치·조립에 영향 없음. 시험: CS-01~CS-03(글자·비밀번호/학습 금지·크기), SW-14~SW-17(뷰: C 자리·키 뒤·자국 아래, 모음 획에 그대로·다음 자음에서 바뀜, 비밀번호에서만 끔, 터치 로직이 C를 매퍼에 줌), TU-62(연습 자판의 C와 판정).

### 9.6 조합 중 모드 키 ↑·스페이스·천지인 키 (SWIPE 9.5 표, 0.21, 0.22)

| 키 | 조합 중(`composer.isComposing()` 또는 보인 조합 글자가 있음) | 조합이 없을 때 |
|---|---|---|
| 모드 키 ↑ (0.22, #205) | `InputDispatcher.onSettingsStroke`: `finishAll()`(확정, 공백 없음) → `resetParsers(MODE_CHANGED)`(조립 닫기, 버린 획이 있으면 진동) → `onOpenSettings()` | 같음(확정할 것 없이 설정 화면) |
| 스페이스 | 완성 + 공백(지금과 같음) | 공백 |
| 천지인 키 | 매퍼가 모음 획 조립을 닫고(버리면 진동) 천지인 요소로 모음(6.2). 가운데 자음은 조합기 그대로라 유지, 획 자국은 조립이 닫혀 지움(9.4) | 낱모음 요소 |

- 0.21의 ⚙ 키 "조합 중 = 한 글자 완성만, 설정 안 엶"은 0.22(이슈 #205, 오너 "완성 기능도 없앰")에서 키와 함께 없앴다.
- 시험: ID-27(LLD-11 7.4, 0.22 새 뜻: 조합 중 모드 키 ↑ = 확정·진동 뒤 설정 엶, 모드 그대로), CJ-50(천지인 키는 그대로).

## 10. 시퀀스

### 10.1 탭과 획: 가 (ㄱ 탭, ㅣ, 가로 점)

```
ㄱ 탭:  StrokeClassifier.finish → Tap → mapper.accept → Jamo([Consonant ㄱ]) → 조합기 "ㄱ", 가운데 자음 "ㄱ"(C 자리)
자음 키 아무 데서 C 오른쪽·C 높이로 ↓: down에서 mapper.consonant = C → classifyConsonant → Stroke(세로) → vs.push(s, C): V1, 가운데 점이 오른쪽 위 → 보임 ㅣ → Started(ㅣ) → "기", trail.keep
ㅣ 오른쪽으로 →(길이 무관): Stroke(가로) → V1 + 오른쪽(밑줄은 안 맞음) → A → Changed(ㅏ) → "가", trail 2개. 가운데 자음 "ㄱ" 그대로
ㄴ 탭:  vs.close(모음 성립, 진동 없음), Jamo([Consonant ㄴ]) → "간", trail.sync(0), 가운데 자음 "ㄴ"
```

### 10.2 역할과 다시 정하기

```
(C) C 아래에서 ↓ → V1, 아래 왼쪽 → 보임 "ㄱㆍ" → 그 아래를 지나는 → → O "고"(밑줄만 맞음) / 그 오른쪽 짧은 → → A "가"(오른쪽만 맞음, 첫 획을 ㅣ로 다시 정함)
(C) C 아래 ㅡ → H1, 아래 왼쪽 → "그" → 가운데가 아래 왼쪽인 ↓ = 점 → U "구" / 오른쪽 위인 ↓ = ㅣ → EOUI → 3등분 + 0.5로 "거" 또는 "의". 그 아래 둘째 가로 획 → HH "‥"(ㅕ ②)
(낱모음) ↓ → V1 "ㅣ" → 그보다 긴 →가 그 아래를 지남 → O: ReplaceVowel(ㅗ) (첫 획을 ㅗ의 점으로 다시 읽음)
가로 → 아래 1/3의 ↓ (ㅢ로 보임) → 그 오른쪽 옆 ↓ → E: Changed(ㅔ)
```
