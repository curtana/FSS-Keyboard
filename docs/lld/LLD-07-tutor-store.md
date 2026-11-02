# LLD-07 입력 연습과 저장소

- 대응 HLD 절: 7장(7.1 입력 연습, 7.2 기준값, 7.3 저장 데이터)
- 버전 **0.25** (2026-11-02)
- 0.25(FSS#25, SRS 0.71 FR-CMN-14·FR-SET-09, HLD 0.64 5A): 설정 `portraitWidth`·`landscapeWidth`(`WidthSetting`, 키 `width_portrait`·`align_portrait`·`width_landscape`·`align_landscape`, 6.1), 설정 화면 "자판" 묶음의 세로·가로 폭·정렬(7절), 연습 자판 `BoardFrame`의 폭·정렬·여백(2.3). 계산 `KeyboardWidth`는 LLD-08 7.2.
- 0.24(FSS#17 오너 결정 "권장안대로"·오너 결정 A·PM 결정 B, SRS 0.68·HLD 0.61·SWIPE 0.48): **튜터를 입력 연습으로 다시 짰다.** 측정하는 튜터(1·3·5과, `TutorSession`·`TutorStep`·`TutorSteps`, `TutorFlow`, `TutorItemJudge`·`HowTo`, `PracticeEcho`·`PracticeKeyboard`·`PracticeLayouts`, `TypingStats`, `TutorMeasure`, `ThresholdEstimator`)와 기준값 저장(`ThresholdStore`·`ThresholdCodec`, 기준값 JSON 형식, 내보내기·가져오기·초기화 메뉴, `calibration` 쓰기)을 지웠다. 새 구성은 2절: 주제 넷·19항목(`PracticeTopics`), 실제 자판 뷰를 그대로 쓰는 연습 자판(`PracticeInput`이 실제 `InputDispatcher`를 메모리 편집기에 연결), 결과 판정 `PracticeItemJudge`(탭 자판 항목은 ⌫ 반영 글자열), 화면 `PracticeActivity`·`PracticeFlow`, 첫 실행 `PracticeLaunchPolicy`(주제 목록만). 기준값은 코드 상수(3절), 위치 허용 오차는 실행 때 긋기 키 폭 × 0.4. 옛 저장 파일 `thresholds.json`은 읽지 않고 지우지도 않는다(4절). 0.23까지의 기록은 아래 이력과 git에 남는다.
- 0.23(이슈 #251 오너 결정: 공개판·개인판을 하나로(빌드 하나, 긋기 엔진은 모든 빌드)): 공개판 튜터를 지웠다 — 1-1 쿼티(`MinCrossOnQwertyStep`), `QwertyPracticeEcho`, `PracticeLayout.QWERTY_LETTER_KEY`, `PracticeLayouts.QWERTY`, `TutorSession`의 `personal` 구분과 1-1 한쪽 분포 규칙(`ONE_SIDED_DISTANCE`), `TutorMeasure.jitter`·`jitterSample`. `PersonalTutorSteps` → `TutorSteps`. 이 문서의 "public …" 설명은 0.22 이전 기록이고, "personal"은 모든 빌드로 읽는다. `ThresholdEstimator.oneSidedDistance`는 남겼다(쓰는 곳 없음, 튜터 정리 이슈 #224에서).
- 0.22(이슈 #217 오너 결정): **`minCrossDp` = 8dp 고정.** 탭 겹침 안내(`Message.OVERLAP_TAP`, `tutor_overlap_tap`) 삭제(2.3). 튜터는 이 값을 바꾸지 않는다 — public 1-1·personal 1-2의 `MIN_CROSS` 규칙은 표본 수(`NotEnough`)만 보고 값은 그대로, 탭 ↔ 점 겹침은 1-2를 다시 하게 하지 않는다(기록 `calibration.tapDot`에는 남음, 2.2). 저장 파일의 `minCrossDp`는 읽지 않고(없어도·범위 밖이어도 됨) 8을 쓰며, 쓰기는 8을 그대로 적는다(옛 판 호환, 4절).
- 0.21(이슈 #179·#180 후속, SRS 0.44·HLD 0.40·SWIPE 0.35): 연습 자판 **가운데 자음**(`PracticeEcho.center`/`TutorFlow.center`/`PracticeKeyboard(center)`, 0.20의 `composing` 대신, 2.3). 연습 자판의 판정도 C 기준(`SwipePracticeEcho`가 매퍼에 C, 3-1 판정 `judge(…, consonantBox)`, 2.5). **쓰는 법 문구에서 "길게·짧게"를 없애고** SWIPE 10.1의 자음 기준 위치 말로(2.5, D-59 개정). 1과·2과 설명·1-2 지시문·탭 겹침 안내 문구. `ActionRecord.stroke()`는 길이를 넘기지 않는다. `show_current_char`·`ErrorLog.enabled`는 FR-CMN-08 ①/② 분리대로(6.1·6.2).
- 0.20(이슈 #180, SWIPE 0.33 9.5, FR-DSP-10): 연습 자판 **가운데 음절** — `PracticeEcho.composing`(조합 글자 + 임시 ㆍ·‥), `TutorFlow.composing`, `PracticeKeyboard(centerText, redraw)`(2.3, LLD-04 9.5). 표준 ㅢ 모양(시험)은 ㅡ가 ㅣ 아래 1/3(이슈 #179).
- 0.19(이슈 #173 오너 결정, SRS 0.41 FR-TUT-02·03·12·13, HLD 0.37 7.1·7.2, SWIPE 0.32 10장·10.1): 글자 항목은 **자음 탭 + 모음 필순 획**(가 = ㄱ 탭, ㅣ, 가로 점). **1-2**는 가 거 고 구 / 그 기(짝) / 아 오 이(ㅇ, 연습만): 자음 키 누름 + 획마다 축·방향만 판정(길이 안 봄, 변위 0 = ✗, D-60). 1-2가 **탭(1-1 + 1-2 자음 탭) ↔ 점 = `minCrossDp`**, **점 ↔ 긴 획 = `longDp`** 두 경계를 정하고(personal 1-1은 표본만), 겹치면 1-2를 다시(최대 두 번, 그 뒤 공식 값 저장), **`calibration` 기록**(두 경계의 P90·P10·간격·표본 수·더 한 바퀴, 둘째 획 위치 오차 P90·P95, FR-TUT-13). **3-1**은 ㅑㅕㅛㅠ ㅐㅔㅒㅖ ㅘㅙㅚ ㅝㅞㅟ ㅢ 한 바퀴, 결과로 판정(조립기가 받은 획과 남은 모음). **3-2 삭제.** 쓰는 법 안내 = 필순 동작 말 + 위치 말(D-59), 반대 방향 획 뒤 덧붙임 "획은 아래로, 오른쪽으로만 그어요". 오류 진동 = 무시된 획·요소, 조립을 닫으며 버린 획. 겹침 안내 두 가지(탭 ↔ 점, 점 ↔ 긴 획). 연습 자판의 획 자국(`openStrokes`)과 안내 미리보기(`preview`). 기준값 형식 5(`posTolDp`, `calibration`, 형식 1~4의 `longDp`는 기본값).
- 0.18(이슈 #168 오너 결정, SRS 0.38 FR-TUT-02·12, HLD 0.34 7.1·7.2, SWIPE 0.29 10장·10.1): **2과(2-1) 삭제**, 글자 항목은 모두 **그 자음 키에서 긋기 한 번**(가 = ㄱ 키 →, 탭 없음. 3-4만 탭 + 천지인 획 둘). **연습 자음 돌림**: 돌리는 항목은 ㄱ ㄲ ㄴ ㄷ ㄸ ㄹ ㅁ ㅂ ㅃ ㅅ ㅆ ㅇ ㅈ ㅉ ㅊ ㅋ ㅌ ㅍ ㅎ(19개)를 차례로 쓰고, 짝(가 / 그)은 같은 자음, ✗ 뒤에는 같은 자음, 아·오·이는 ㅇ 그대로(순번 안 셈), 1-1은 기본 14자음 그대로(PM 결정 #168), 3-4는 첫 자음 ㄱ 그대로, 단계가 바뀌거나 같은 단계를 다시 하면 ㄱ부터(2.2, D-56). 판정은 `Swipe(key, edges)`·`Pass(key, edges)`(목표 자음 키에서 시작), `Judgement.Wrong`은 값 없음(기준 키 밖·덧붙임 안내 없음), 튜터의 오류 진동은 무시된 천지인 요소만(`PracticeEcho.lastIgnored`, 2.3·2.5). 화면 과 번호 1 기본 / 2 복모음 / 3 문장, 단계 줄도 화면 번호(`TutorFlow.screenId`, 2.2). 1과 설명(긋기 = 자음 + 모음, 받침은 탭, 쌍자음은 위 줄 전용 키, 모음 시작은 ㅇ 키). 기준값 형식 4(`doubleTapWindowMs` 없음, 형식 1~3 읽기, 4·5절). `ThresholdField.DOUBLE_TAP_WINDOW`, `TutorMeasure.interval`·`intervalSample`, `ThresholdEstimator.GAP_OUTLIER_MS`, `HowTo.doubleTap`·`offAnchorHint`, `TutorItemJudge.anchorAfter`, `ItemSpec.tapKey`, `TutorFlow.hint`·`anchorKeyId`, `PracticeEcho.anchorKeyId`를 지웠다.
- 0.17(이슈 #165, PM 결정, SRS 0.37 FR-TUT-09·10, HLD 0.33 7.1, SWIPE 0.28 10장): **5과 연습 자판에 ⌫**(`PracticeKey.backspace`·`RawStroke.backspace`, `SwipePracticeEcho`가 실제 `InputDispatcher.backspace`와 같은 순서로 처리, 2.2·2.3). 타수·정확도는 [다음] 때의 최종 입력 줄(지운 것 제외), 걸린 시간 끝 = 마지막 동작(⌫ 포함) 뗌(2.4, 바뀐 코드 없음: 문장 획에 ⌫ 획도 들어감). 1~3과·public은 ⌫ 없음. "틀린 입력 뒤 다음 글자 안 들어감"은 지금 코드로 재현되지 않음(이슈 #163 수정 뒤, 6.17).
- 0.16(이슈 #160, SRS 0.36 FR-TUT-02, HLD 0.32 7.1·7.2, SWIPE 0.27 10장·10.1): 3과에 **3-4 천지인 키** 한 항목(가 = ㄱ 탭 → 천지인 ↑ → 천지인 탭, 결과로 판정, 측정 없음, 한 번 맞히면 끝, 2.2·2.5). 판정에 `ExpectedAction.Element`, 동작 기록에 `element`. 긋기 연습 자판의 연습 키에 천지인 키가 들어가고(5과 포함) 입력 표시도 천지인 키를 해석한다(ㆍ·‥ 임시 글자 포함, 2.3). 3과 설명에 천지인 키 소개.
- 0.15(이슈 #154, SRS 0.34~0.35 FR-TUT-12·TBD-17, HLD 0.30~0.31 7.1, SWIPE 0.25~0.26 10.1절): **항목 판정**(2.5 `TutorItemJudge`·`HowTo`). 1~3과 항목과 public 1-1은 동작을 뗄 때마다 목표의 같은 차례 동작과 비교해 다르면 바로 ✗(빨강) + 쓰는 법 안내 + 같은 항목 다시, 모두 맞으면 ✓(초록) + 다음 항목. ✗ 시도의 표본은 버리고 진행 수는 ✓만 센다. 측정 단계는 키·탭/긋기·기준 키·처음 통과한 변으로만 판정하고 ✓이면 입력 표시를 목표 글자로(TBD-17 = A). ✓·✗와 입력 표시는 다음 동작을 누를 때 비운다(항목마다 새 `PracticeEcho`). ✗인 기준 키 밖 긋기는 오류 진동(D-48 바뀜). 이상값 처리(3절, D-51). 이슈 #156: 3-3 삭제, 3과 설명에서 나눠 쓰기 문장을 뺌.
- 0.14(이슈 #150, SRS 0.33·HLD 0.29·SWIPE 0.24): 4과(4-1)·3-3 측정·5-1 다시 계산·`SwipeSentenceJudge`를 지웠다. 2-1은 빠른 ㄲ ↔ 보통 ㄱㄱ 두 분포 경계, 연습 항목은 모두 "자음 탭 + 그 키에서 모음 긋기", 1-2에 아·오·이(연습만, 한 바퀴의 마지막, PM 결정), 안내는 기준 키에서만(`anchorKeyId`). 기준값 JSON 형식 3(형식 1·2는 빠진 칸과 `doubleTapWindowMs`를 버리고 읽음, PM 결정).
- `Thresholds`·`ThresholdDefaults`는 [LLD-03-modules.md#34-기준값-kbdstrokethreshold](LLD-03-modules.md#34-기준값-kbdstrokethreshold)에 있다.
- 요구: FR-TUT-01·02·05·06·09·11·12, FR-DSP-08, FR-SET-02~06·09, NFR-05, FR-CMN-12·14.

## 1. 범위

| 절 | 구성 요소 |
|---|---|
| 2 | 입력 연습: `PracticeTopics`(2.2), `PracticeInput`·`PracticeEditor`·`PracticeActivity`·`PracticeFlow`(2.3), `PracticeItemJudge`(2.4), `PracticeLaunchPolicy`(2.5) |
| 3 | 고정 기준값 |
| 4 | (삭제) 기준값 저장 — 옛 파일 처리만 |
| 5 | (삭제) 기준값 JSON 형식 |
| 6 | `SettingsStore`, `ErrorLog` |
| 7 | `SettingsActivity` |

## 2. 입력 연습 (HLD 7.1, SWIPE 10장, FSS#17)

`app` 패키지 `kbd.app.practice`. 화면(`PracticeActivity`)과 연습 자판의 뷰를 빼면 모두 안드로이드 없는 Kotlin이고 JVM으로 시험한다. **아무것도 재지 않는다.** 판정은 실제 엔진이 남긴 결과로만 한다.

### 2.1 타입

```kotlin
enum class PracticeBoard { TAP_QWERTY, SWIPE, GRAFFITI }      // 항목의 연습 자판
data class PracticeItem(
    val target: String, val board: PracticeBoard, val howTo: String,
    val steps: List<String>? = null,       // 탭 자판이 아닌 항목: 동작마다 편집기에 남아야 할 글자열(마지막 = target)
    val note: String? = null,              // 목표 줄에 붙이는 짧은 말("긋기 자판", "천지인 키", "숫자 영역")
    val guideCells: List<String> = emptyList(),   // 그래피티 ✗ 때 강조할 안내표 칸 이름
    val lang: GraffitiLang? = null,        // 그래피티 항목의 패드 언어
)
data class PracticeTopic(val id: String, val title: String, val summary: String, val time: String,
                         val intro: List<String>, val items: List<PracticeItem>)
enum class Verdict { PENDING, RIGHT, WRONG }
```

- 화면 문구 가운데 주제 이름·한 줄 설명·어림 시간·설명·쓰는 법은 `PracticeTopics`의 Kotlin 상수다(시험이 그대로 읽음). 단추·제목은 `strings_settings.xml`의 `practice_*`.
- **`steps`(LLD 결정 D-65):** 탭 자판이 아닌 항목은 "동작마다 실제 판정 결과를 목표의 같은 차례와 비교"(SWIPE 10.2)를 **그 동작 뒤 편집기의 글자열**로 한다. 글자열은 조합 중 글자와 임시 ㆍ·‥까지 포함한 실제 표시다(예: 고 = `ㄱ` → `ㄱㆍ` → `고`). 이 값은 손으로 정한 것이 아니라 실제 엔진으로 확인한 값이고, 시험 PR-10~PR-12·PR-20이 항목의 필순대로 실제 엔진에 넣어 같음을 지킨다.

### 2.2 주제와 항목 (SWIPE 10.1, 설계 권장 글자 그대로)

| 주제(id) | 연습 자판 | 항목: 목표(쓰는 법 / `steps`) |
|---|---|---|
| ① 영문 대문자(`upper`) | 탭 자판 | `A`(a 키에서 위로), `Hi`(h 키에서 위로 → i 탭), `OK`(o 키에서 위로 → k 키에서 위로) |
| ② 기호 입력(`symbol`) | 탭 자판 3, 긋기 자판 3 | `!`(1 키에서 위로), `?`(/ 키에서 위로), `[`(i 키에서 왼쪽으로) / 긋기: `@`(2 키에서 위로, `@`), `(`(. 키에서 왼쪽으로, `(`), `?`(/ 키에서 위로, `?`) |
| ③ 그래피티(`graffiti`) | 패드 + 안내표 | `가`(ㄱ → ㅣ → 탭 / `ㄱ`,`기`,`가`, 칸 ㄱ·ㅏ), `고`(ㄱ → 탭 → ㅡ / `ㄱ`,`ㄱㆍ`,`고`, 칸 ㄱ·ㅗ), `a`(영문, 칸 A), `1`(숫자 영역, 칸 1) |
| ④ 긋기(`swipe`) | 긋기 자판 | `가`(`ㄱ`,`기`,`가`), `고`(`ㄱ`,`ㄱㆍ`,`고`), `아`(`ㅇ`,`이`,`아`), `과`(`ㄱ`,`ㄱㆍ`,`고`,`괴`,`과`), `의`(`ㅇ`,`으`,`의`), 천지인 키 `가`(`ㄱ`,`기`,`가`) |

- 긋기 자판의 기호 키 이름은 키에 적힌 탭 기호로 부른다(기호 키 = `.`, 기호3 키 = `/`). 긋기 항목의 쓰는 법은 SWIPE 10.2 위치 말 그대로다(예: 과 = `ㄱ 탭 → 자음 아래에 아래로 → 점 아래에 오른쪽으로 → ㅡ 오른쪽 끝에서 아래로 → ㅣ 오른쪽에 오른쪽으로`).
- 그래피티 한글 쓰는 법은 자모 차례(`가 = ㄱ → ㅣ → 탭`), 영문·숫자는 `<목표> = 안내표의 <글자> 칸처럼`이다(SWIPE 10.2).
- 설명(주제마다 3~5줄)은 SWIPE 10.1 "설명(핵심)" 칸을 말로 풀어 쓴 것이고 한 화면을 넘지 않는다. ④에는 "떨어져도 돼요"를 둔다.
- 시험: PR-01.

### 2.3 화면과 연습 자판 (`PracticeActivity`, `PracticeFlow`, `PracticeInput`)

**흐름 `PracticeFlow`(안드로이드 없음):** `Screen { LIST, INTRO, ITEM, DONE }`. `open(topic)` → INTRO, `start()`([시작]) → ITEM 첫 항목, `stop()`([그만]·[주제 목록으로]) → LIST. 마지막 항목이 ✓이면 DONE으로 가고 `markDone(id)`(→ `SettingsStore.markPracticeDone`)를 부른다. 중간에 그만두면 표시하지 않는다.

| 값 | 뜻 | 바뀌는 때 |
|---|---|---|
| `verdict` | ✓·✗ | 판정 때 정함. 다음 터치를 누를 때(`onPress`) 지움. 탭 자판 항목의 ✗는 글자열이 틀린 동안 남고, ⌫로 앞부분이 되면 지움 |
| `echo` | 입력 표시(FR-TUT-09) | 동작마다 `PracticeEditor.display`. `onPress`에서 지움(탭 자판 항목의 ✗ 동안은 남김) |
| `howTo` | 쓰는 법 한 줄 | ✗ 때 항목의 `howTo`, 그 항목을 맞히면 지움 |
| `hint` | 덧붙임 | 긋기 항목의 ✗가 위나 왼쪽으로 그은 터치에서 났을 때 "획은 아래로, 오른쪽으로만 그어요"(SWIPE 10.2) |
| `highlight` | 안내표 강조 칸 | 그래피티 ✗ 때 `guideCells`, ✓ 때 지움 |
| `tryNo` | 시도 번호 | 항목 시작, 탭 자판이 아닌 항목의 ✗ 뒤. 바뀌면 화면이 연습 자판을 새로 만든다(= 판정·조합 상태를 비움, SWIPE 10.2) |

**`onAction(text, display, error, reversed)`:** 탭 자판 항목은 `PracticeItemJudge.text`, 그 밖은 `PracticeItemJudge.step(item, 이번 시도의 동작 번호, text, error)`. ✓ → 다음 항목(새 시도), ✗ → 위 표대로(탭 자판 항목은 비우지 않음).

**연습 편집기 `PracticeEditor : EditorPort`:** 입력창 대신 메모리 글자열. `commitText`는 조합 글자를 바꿔 넣고(InputConnection과 같음), `deleteBefore`는 끝 한 글자를 지운다. `text` = 확정 + 조합(판정용), `display` = 조합 글자가 있으면 그것(`ㄱㆍ`처럼 그대로), 없으면 확정 글자의 마지막 한 글자.

**한 시도 `PracticeInput(board, feedback, errorLog, settings) : SwipeHost`:** 실제 `InputDispatcher`·`ComposingController`·`ModeController(모드 하나)`와 모드의 조합기(긋기 `ComposePolicy.SWIPE`, 그래피티 `GRAFFITI`, 쿼티 없음)를 `PracticeEditor`에 잇는다. 기준값은 `ThresholdDefaults.VALUES`(3절). 오류 진동(`FeedbackPort.error`)을 지나며 `errored`를 켠다 — SWIPE 10.2의 ✗ 조건(무시된 획·천지인 요소, 버린 임시 획, 인식 실패)이 모두 이 진동이다.
- `tapSink`(탭 자판·그래피티 패드에 줌): 글자·숫자·기호는 그대로, 기능 키는 **탭 자판의 ⌫만** 넘긴다. 스페이스·↵·화살표·Esc·Del·Tab·모드 키·설정 획은 보이되 연습에서는 아무 일도 하지 않는다(SWIPE 10.2 "편집 줄의 다른 키는 판정에 들어가지 않는다", LLD 결정 D-66).
- `SwipeHost` 구현(긋기 자판에 줌): `onSwipeEvents`·`onSwipeText` → dispatcher. 긋기 자판의 기능 키(⌫·스페이스·↵·모드 키)는 아무 일도 하지 않는다(SWIPE 10.2: 긋기 연습 자판에는 편집 키가 없음 — 키는 실제처럼 보이되 쓰이지 않음, D-66). `centerConsonant` = 조합기의 `lastConsonant()`라 가운데 자음·C 기준 판정이 실제와 같다.
- `graffiti(templates, lang, flash, redraw)`: 실제 `GraffitiInputHandler`(인식기·`GraffitiState`·천지인 조립기)를 만들고 패드 언어를 항목의 `lang`으로 둔다(한영 전환 획은 연습하지 않음, SWIPE 10.1 ③).

**연습 자판(`BoardFactory.build`, 시도마다 새로):** 실제 뷰를 `BoardFrame` 안에 둔다.
- 탭 자판: `TapKeyboardView` + `TapLayouts.QWERTY`(편집 줄 포함, 오너 결정 A), 높이 = 줄 수 × 설정 줄 높이.
- 긋기 자판: `SwipeModuleProvider.get().createSwipeView(context, input)` — 같은 `SwipeKeyboardView`라 획 안내·현재 글자·다음 글자·획 자국·가운데 자음이 같은 시점·설정으로 나온다(FR-TUT-11, 입력창이 없어 비밀번호 판별은 해당 없음).
- 그래피티: 위에 `GraffitiGuidePanel`(높이 200dp, ✕ 없음 `showClose = false`, 그 항목의 구획만 — 한글 / 영문 / 숫자, `highlight` = `PracticeFlow.highlight`), 아래에 `GraffitiPadView`(실제 비율 설정). 안내표는 늘 펼친 상태이고 패드를 덮지 않는다. **구획은 화면 크기와 상관없이 늘 그 항목의 것만** 보인다(LLD 결정 D-67: 안내표는 길어서 어차피 스크롤한다).
- **`BoardFrame`:** 뷰의 터치를 가로채지 않고 지켜본다. 다른 손가락이 없을 때 누르면 `errored`를 끄고 그때의 글자열을 기억한 뒤 `PracticeFlow.onPress()`. 뗄 때는 뷰가 처리한 **뒤에** 판정을 `post`한다(엔진이 스스로 `post`한 일도 끝난 뒤). 탭 자판이 아니면 글자열이 바뀌었거나 오류 진동이 있었을 때만 동작으로 센다(아무 일도 하지 않는 키·안내표 스크롤은 동작이 아님). `reversed` = 그 터치가 8dp를 넘게 위나 왼쪽으로 갔는가(덧붙임용).
- **폭·정렬(0.25, FSS#25, SRS FR-CMN-14 ⑥):** `BoardFrame`은 화면 폭(가용 폭)을 받아 그 화면 방향의 설정(`portraitWidth`·`landscapeWidth`)으로 `KeyboardWidth.place`(LLD-08 7.2, 짧은 변 = `displayMetrics`의 작은 쪽)를 구해 자식(연습 자판, 그래피티면 안내표 + 패드 묶음)을 그 폭으로 재고 `span.left`에 둔다. 남는 자리는 키보드 배경색이고 터치를 소비한다. 자판 밖에서 내려온 포인터는 `OffKeyboardPointers`로 걸러 자판에 가지 않고 `onPress`·`onAction`도 부르지 않는다(연습 동작이 아님). 설정은 화면을 열 때 한 번 읽은 값이다(2.3 처음)

**화면(Compose):** 위에 제목, 주제 목록(이름·✓·어림 시간·한 줄 설명, [닫기]) / 설명([그만], [시작]) / 항목(주제 이름 n / N, `쓸 것: <목표> (<note>)`, 입력 표시 64sp + ✓ 초록·✗ 빨강(Material green 700·red 700), 쓰는 법, 덧붙임, [그만]) / 끝("다 했어요", [주제 목록으로]). 아래에 연습 자판. 회전해도 다시 만들지 않는다(매니페스트 `configChanges`).
- 시험: PR-02~PR-08, PR-10~PR-13, PR-20·PR-21, 실기기 DV-11·DV-24.

### 2.4 항목 판정 `PracticeItemJudge` (FR-TUT-12, SWIPE 10.2, 안드로이드 없음)

```kotlin
object PracticeItemJudge {
    fun text(item: PracticeItem, text: String): Verdict      // 탭 자판 항목
    fun step(item: PracticeItem, index: Int, text: String, error: Boolean): Verdict
    fun lastChar(s: String): String
}
```

- `text`: 글자열 = 목표 → RIGHT, 목표의 앞부분(빈 글자열 포함) → PENDING, 그 밖 → WRONG. ⌫는 판정 동작이 아니고, ⌫ 뒤의 글자열을 다시 보는 것이 "⌫로 고쳐 앞부분이 되면 ✗를 거둠"이다(오너 결정 A).
- `step`: 오류 진동이 있었거나, 항목에 그 차례가 없거나(동작이 남음), 글자열이 그 차례의 `steps`와 다르면 WRONG. 마지막 차례와 같으면 RIGHT, 아니면 PENDING.
- 0.23까지의 1-1·1-2 특별 판정(누른 키만, 축·방향만)과 "✓이면 목표 글자로 바꿔 보임"은 없다(SRS TBD-17 해당 없음). 입력 표시는 늘 실제 글자라 ✓이면 그것이 곧 목표다.

### 2.5 첫 실행 `PracticeLaunchPolicy` (FR-TUT-01, 안드로이드 없음)

- `SettingsActivity.onCreate`에서 `onAppOpen(settings, markShown, openList)`: `practiceListShown`이 거짓이면 **먼저** `setPracticeListShown(true)`를 저장하고(D-34) `PracticeActivity`를 연다. 화면은 주제 목록으로 시작하고 연습은 시작하지 않으며 [닫기] 한 번으로 나간다(오너 결정 ②). 그 뒤에는 설정의 "입력 연습"으로만 연다(FR-TUT-06).
- 시험: PR-09.

## 3. 기준값 (HLD 7.2, SRS 1.3)

- 모두 **고정값이고 코드 상수**다: `ThresholdDefaults.VALUES`(`minCrossDp` = 8dp, `guideDelayMs` = 120ms). `KeyboardService.thresholds`와 연습 자판이 같은 값을 쓴다. 저장 파일·측정·다시 읽기(옛 D-22 파일 시각 비교)는 없다.
- **위치 허용 오차 `posTolDp` = 실행 때의 실제 긋기 키 폭 × 0.4**(PM 결정 B): `SwipeKeyboardView.rebuildGeometry`가 폭이 정해질 때마다 `SwipeInputMapper.setKeyWidth(폭 / 7)`을 불러 `ThresholdDefaults.posTolFor(키 폭)`을 모음 조립기에 넣는다. 그 전(뷰가 폭을 알기 전)에는 `VALUES.posTolDp` = 20dp(키 폭 51dp × 0.4). 다른 기준값은 키 크기와 상관없는 dp 절댓값이다(FR-CMN-12).
- `longDp`·`keySizeWDp`·`keySizeHDp`는 형식만 남았고 판정에 쓰지 않는다(디버그 기록·시험용).
- 시험: TH-10(0.24).

## 4. ~~ThresholdStore~~ (0.24 삭제, FSS#17)

기준값 저장 파일을 쓰지 않는다. 0.23까지 `filesDir`에 쓰던 `thresholds.json`(형식 1~6)은 **읽지 않고 지우지도 않는다**(LLD 결정 D-68: 쓰는 곳이 없고, 앱을 지우면 함께 지워진다. 남겨도 동작에 영향이 없다).

## 5. ~~기준값 JSON 형식, 내보내기·가져오기~~ (0.24 삭제, FSS#17 오너 결정 ①)

설정의 내보내기·가져오기·초기화 메뉴와 형식은 없다. 0.23까지의 형식 설명은 이력(git)에 남긴다.

## 6. SettingsStore와 ErrorLog

### 6.1 SettingsStore (Jetpack DataStore Preferences)

```kotlin
data class Settings(val lastMode: InputMode?, val showDirectionGuide: Boolean, val showNextChar: Boolean,
                    val showCurrentChar: Boolean,   // 0.11, 이슈 #135
                    val haptic: Boolean, val sound: Boolean, val rowHeightDp: Float,
                    val graffitiLetterRatio: Float, val tutorAutoStarted: Boolean,
                    val portraitWidth: WidthSetting = WidthSetting.PORTRAIT_DEFAULT,     // 0.25, FSS#25 (LLD-08 7.2)
                    val landscapeWidth: WidthSetting = WidthSetting.LANDSCAPE_DEFAULT)
class SettingsStore(private val ds: DataStore<Preferences>) {
    val flow: Flow<Settings>
    fun loadBlocking(): Settings                    // runBlocking { flow.first() } — 서비스 시작 때 한 번(D-24)
    suspend fun setLastMode(m: InputMode); suspend fun update(f: (Settings) -> Settings)
    suspend fun setPracticeListShown(v: Boolean)     // FR-TUT-01, 2.5
    suspend fun markPracticeDone(id: String)          // 주제 목록의 ✓(2.3)
}
object RowHeight { const val MIN_DP = 40f; const val MAX_DP = 64f; const val DEFAULT_DP = 48f
                   fun clamp(v: Float) = v.coerceIn(MIN_DP, MAX_DP) }
```

| 키 | 기본값 | 근거 |
|---|---|---|
| `last_mode` | 없음 | FR-CMN-10. 숫자류 입력창 안에서 바꾼 모드는 저장하지 않는다(FR-CMN-04b, LLD-08 6.1 `onModeKey`) |
| `show_direction_guide` | true | SWIPE 9.3 |
| `show_next_char` | true | SWIPE 9.3 |
| `show_current_char` | true | SWIPE 9.3, FR-DSP-08(0.11, 이슈 #135). 비밀번호 입력창만 이 값과 상관없이 끔(FR-CMN-08 ①, LLD-06 2절). 학습 금지 칸은 이 값대로(②, 0.21) |
| `haptic` | **true**(진동 켬) | FR-SET-03(DCR-14 결정) |
| `sound` | **false**(소리 끔) | FR-SET-03(DCR-14 결정) |
| `row_height_dp` | 48, 고를 수 있는 범위 40~64 | FR-SET-06, NFR-06, TBD-16(DCR-14 결정) |
| `graffiti_letter_ratio` | 2/3 (문자 2 : 숫자 1). 고를 수 있는 값은 1/2(1 : 1)·2/3(2 : 1)·3/4(3 : 1) 셋뿐 | FR-GRF-01, FR-SET-05(오너 결정 #108 A), GRAFFITI 6.3 |
| `tutor_auto_started` | false | FR-TUT-01: 첫 실행에 주제 목록을 보였는가(2.5). 값 이름은 `practiceListShown`, 키 이름은 옛 것 그대로라 옛 튜터를 이미 연 설치본은 목록을 다시 열지 않는다 |
| `practice_done` | 빈 집합 | 끝까지 한 주제 id(`upper`·`symbol`·`graffiti`·`swipe`), 주제 목록의 ✓(SWIPE 10.0). 판정에 쓰지 않는다 |
| `width_portrait`, `align_portrait` | 1.0, `CENTER` | FR-CMN-14 ③, FR-SET-09(0.25). 폭 비율(Float)과 정렬 이름(`LEFT`·`CENTER`·`RIGHT`). 읽을 때 비율은 `KeyboardWidth.snap`(50~100 안 가까운 5% 칸, D-33과 같은 생각), 모르는 정렬 이름은 `CENTER` |
| `width_landscape`, `align_landscape` | 없음(= 세로와 같은 크기), `CENTER` | FR-CMN-14 ③, FR-SET-09(0.25). 비율이 없으면 `ratio = null`이고 `KeyboardWidth.place`가 화면 짧은 변을 폭으로 쓴다. `update`에서 `ratio = null`이면 키를 지운다 |

- 저장된 `lastMode` 이름이 모르는 값이면 없음으로 본다(FR-CMN-10; 0.23: 모든 빌드가 네 모드라 FR-CMN-03b의 "빌드에 없는 모드" 경우는 없음, 이슈 #251).
- `graffiti_letter_ratio`는 Float 하나로 저장한다(`floatPreferencesKey`). 값은 패드 폭 가운데 **문자 영역의 몫**이다: 1 : 1 = 1/2, 2 : 1 = 2/3, 3 : 1 = 3/4(문자 : 숫자 = r : 1 − r). 설정 화면은 세 값만 쓴다(`GRAFFITI_RATIOS`, 7절). 읽을 때는 검사하지 않는다: 저장값이 없으면 기본값 2/3, 있으면 목록 밖 값이라도 그대로 `GraffitiPadGeometry`에 넘겨 그 몫으로 패드를 나누고, 설정 화면은 어느 선택지도 고르지 않은 상태(세 값과의 차가 모두 0.001 이상)로 보인다. 사용자가 하나를 고르면 그 값으로 덮어쓴다. 앱이 목록 밖 값을 쓰는 길은 없으므로(설정 화면이 유일한 쓰기) 이 경우는 저장 파일이 손상되거나 바깥에서 고친 때뿐이다.
- `row_height_dp`는 읽을 때 `RowHeight.clamp`로 40~64 안에 둔다(범위 밖 저장값은 가까운 끝값, D-33). 48dp 미만(40~47dp)도 사용자가 고른 값이면 그대로 쓴다(NFR-06은 기본 줄 높이 기준, TBD-16).
- 0.1의 `tutor_done`은 없앴다. 연습을 했는지는 키보드 동작을 바꾸지 않는다(안내 없음, FR-TUT-05).
- 시험: SS-01~SS-05, SS-07(0.24).

### 6.2 ErrorLog

```kotlin
class ErrorLog(private val file: File, private val maxBytes: Int = 64 * 1024) {
    var enabled: Boolean                         // FieldInfo.noRecord(비밀번호·학습 금지 입력창)이면 false(FR-CMN-08 ①②, NFR-05)
    fun record(site: String, e: Throwable)
    fun readAll(): String                        // 시험·개발용(사용자 화면 없음)
}
```

#### `record(site, e)`
- `enabled == false`면 아무것도 하지 않는다(NFR-05).
- 한 줄 = `site` + `\t` + `e.javaClass.simpleName` + `\t` + 우리 패키지(`kbd.`) 안 첫 스택 프레임의 `클래스.메서드:줄` + `\n`. **예외 메시지·입력 글자·좌표·시각은 쓰지 않는다**(NFR-05 "오류 종류·위치(코드)만").
- 파일이 `maxBytes`를 넘으면 앞쪽 절반을 버린다(크기 제한 순환, HLD 7.3, D-26).
- 쓰기 실패는 삼킨다(기록이 입력기를 죽이지 않음, NFR-03).
- 시험: EL-01~EL-05.

## 7. SettingsActivity

| 항목 | 저장 | 근거 |
|---|---|---|
| 방향 안내 켜기·끄기 | `show_direction_guide` | FR-DSP-08, FR-SET-02 |
| 다음 글자 안내 켜기·끄기 | `show_next_char` | 〃 |
| 현재 글자 크게 보기 켜기·끄기(0.11) | `show_current_char` | FR-DSP-08, FR-SET-02(이슈 #135). 셋 다 끄면 안내가 전혀 보이지 않음(실제 키보드·연습 자판) |
| 진동·소리 | `haptic`, `sound`(기본 진동 켬·소리 끔) | FR-SET-03 |
| 입력 연습(0.24) | `PracticeActivity` 열기(설명 "영문 대문자 · 기호 · 그래피티 · 긋기를 골라서 짧게 연습"). 첫 실행의 주제 목록은 2.5 | FR-TUT-06, FR-SET-04 |
| 그래피티 영역 비율 | `graffiti_letter_ratio`. 라디오 단추 셋: 1 : 1 / 2 : 1(기본) / 3 : 1 → 1/2 / 2/3 / 3/4 저장(6.1) | FR-SET-05(오너 결정 #108 A), GRAFFITI 6.3 |
| 줄 높이 | `row_height_dp`(40~64dp, 기본 48dp) | FR-SET-06, TBD-16 |
| 세로·가로 화면 폭·정렬(0.25) | `width_portrait`·`align_portrait`·`width_landscape`·`align_landscape`(6.1). 줄 높이 바로 다음에 넷: "세로 화면 폭"(슬라이더, `KeyboardWidth.RATIOS` 11칸, 값 "75%"), "세로 화면 정렬"(라디오 셋 한 줄: 왼손모드 · 중앙모드 · 오른손모드), "가로 화면 폭"(슬라이더, `landscapeChoices(approx)` 12칸 — null 칸 값은 "세로와 같은 크기 (약 62%)"), "가로 화면 정렬". approx = 화면 짧은 변 ÷ 긴 변(`displayMetrics`, 옆 막대를 빼지 않은 대략값; 키보드는 정확한 폭을 셈). 슬라이더는 뗄 때 저장(줄 높이와 같음). 정렬은 그 방향 폭이 100%(null이면 approx ≥ 1)일 때 흐리게·고를 수 없음. 저장값이 칸에 없으면(바깥에서 고친 값) 첫 칸을 보임 | FR-SET-09, FR-CMN-14 |
| 입력기 켜기 안내 | 시스템 입력기 설정 화면 열기(`Settings.ACTION_INPUT_METHOD_SETTINGS`) | FR-CMN-01 |

- 0.24: "튜터 다시 실행"은 "입력 연습"으로, 기준값 초기화·내보내기·가져오기는 삭제(FSS#17 오너 결정 ①, FR-TUT-07 삭제).
- 안내 표시 항목(방향 안내·다음 글자·현재 글자)은 늘 보인다(0.23, 이슈 #251). 줄 높이는 40~64dp 범위에서 고른다(FR-SET-06, DCR-14 결정). 설정 화면에는 기준값 상태 안내를 두지 않는다(FR-TUT-05).
- 시험: 실기기 DV-11.
