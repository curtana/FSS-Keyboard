# LLD-07 튜터 모드와 저장소

- 대응 HLD 절: 7장(7.1 튜터 모드, 7.2 기준값, 7.3 저장 데이터)
- 버전 **0.23** (2026-11-02)
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
- `Thresholds`·`ThresholdDefaults`는 [LLD-03-modules.md#34-기준값-kbdstrokethreshold](LLD-03-modules.md#34-기준값-kbdstrokethreshold), `SwipeModule.tutorSteps()`는 [LLD-03-modules.md#22-swipemodule-인터페이스-app-main](LLD-03-modules.md#22-swipemodule-인터페이스-app-main)에 있다.
- 요구: FR-TUT-01~12, FR-DSP-08, FR-SET-02~06, NFR-05, FR-CMN-12.

## 1. 범위

| 절 | 구성 요소 |
|---|---|
| 2 | `TutorActivity`·`TutorFlow`, `TutorSession`, `TutorStep`, `TypingStats`(2.4), `TutorItemJudge`·`HowTo`(2.5) |
| 3 | `ThresholdEstimator` |
| 4 | `ThresholdStore`, `ThresholdCodec` |
| 5 | 기준값 JSON 형식, 내보내기·가져오기 |
| 6 | `SettingsStore`, `ErrorLog` |
| 7 | `SettingsActivity` |

## 2. 튜터 모드

### 2.1 타입

```kotlin
enum class SampleSide { SHORT, LONG }             // 경계의 짧은 쪽(붙인·보통) · 긴 쪽(끊은·의도)
data class Sample(val field: ThresholdField, val side: SampleSide, val value: Float)
enum class ThresholdField { MIN_CROSS, LONG, GUIDE_DELAY, POSITION }   // 0.19: ADD_I·RETURN_LONG·TURN_ANGLE 지움, POSITION = 위치 오차 기록(추정 안 함)
interface TutorStep {
    val id: String                                // SWIPE 10장 "과-단계"("1-1", "5-1")
    val lesson: Int                               // id의 앞 숫자(기본 구현)
    val intro: Int                                // 과 설명 문자열 리소스(그 과의 첫 단계만, 없으면 0)
    val prompt: Int                               // 단계 안내 문구(쓰는 법) 문자열 리소스
    val practiceLayout: PracticeLayout
    val targets: List<TargetAction>               // 연습 항목(정해진 번갈아 순서, 2.2)
    val minSamplesPerTarget: Int                  // 항목마다 되풀이 횟수(D-18 기본 10, 2.2 표)
    fun practiceOnly(target: TargetAction): Boolean = false   // 연습만 하는 항목: 차례에는 나오지만 측정·진행 수에 넣지 않음(0.14, 1-2 아·오·이)
    fun itemSpec(target: TargetAction): ItemSpec? = null      // 판정 방식(0.15, 2.5). null = 판정 없음(5-1 문장)
    fun rotates(target: TargetAction): Boolean = false        // 연습 자음 돌림을 받는 항목(0.18, 2.2)
    fun pairedWithPrevious(target: TargetAction): Boolean = false   // 짝의 둘째(그): 앞 항목의 자음을 그대로(0.18)
    fun withConsonant(target: TargetAction, consonant: String): TargetAction = target   // 가 + ㄴ = 나(template 유지, 0.18)
    fun extract(raw: RawStroke, target: TargetAction, current: Thresholds): List<Sample>
}
interface MultiStrokeStep : TutorStep {           // 항목 하나가 획 여럿일 수 있음(0.18: 3-4 = 탭 + 천지인 획 둘, 그 밖은 1)
    fun strokesPerAction(target: TargetAction): Int
    fun extract(action: List<RawStroke>, target: TargetAction, current: Thresholds): List<Sample>   // action = 그 항목의 지금까지 획(끝이 새 획)
}
interface SentenceStep : TutorStep                // 5-1: 문장을 쓰고 [다음]. 측정 없음(0.14, extractSentence 삭제)
data class RawStroke(val keyRect: RectDp, val points: List<StrokePoint>, val prevUpTime: Long?, val space: Boolean = false,
                     val key: String = "")       // key = 누른 연습 키의 글자("ㄱ", "q"), 판정용(0.15)
enum class PracticeLayout { SWIPE_CONSONANT_KEY, SWIPE_SENTENCE, QWERTY_LETTER_KEY }   // SWIPE_SENTENCE = 자음 키 + 천지인 키 + 스페이스 + ⌫(5과, ⌫는 0.17)
data class TargetAction(val label: String, val template: String = label)   // label = 화면의 "지금 쓸 것"(돌림 자음을 넣은 글자 "나"), template = 단계 목록의 항목("가", 0.18)
data class StepProgress(val collected: Int, val needed: Int)   // 이 단계에서 마친 항목 수 / 필요한 수
sealed interface StepOutcome {
    data class Accepted(val thresholds: Thresholds) : StepOutcome
    data class Overlap(val field: ThresholdField) : StepOutcome   // 두 분포가 겹침(HLD 7.1)
    data object NotEnough : StepOutcome
}
class TutorSession(steps: List<TutorStep>, start: Thresholds) {
    fun onStroke(raw: RawStroke): StepProgress
    val judgement: Judgement?                      // 이 획 뒤 판정(Pending / Right / Wrong), null = 판정 없음(0.15, 2.5)
    val judgedItem: ItemSpec?                      // judgement의 항목
    fun endSentence(): StepProgress                // 5-1 [다음](0.14: 측정이 없어 자판 폭을 받지 않음)
    val sentenceStrokes: List<RawStroke>           // 쓰는 중인 문장의 획(걸린 시간 계산용)
    fun finishStep(): StepOutcome
    val current: Thresholds
}
interface PracticeEcho {                           // 입력 표시(FR-TUT-09, 2.3): 1~3과 큰 한 글자, 5과 "입력: …" 줄. 표시 전용
    val text: String
    fun onStroke(raw: RawStroke, widthDp: Float)
    val lastIgnored: Boolean get() = false         // 마지막 획에 실제 자판이 오류 진동을 낼 일(무시된 획·요소, 버린 획, 0.19)이 있었나. 쿼티는 false
    val openStrokes: Int get() = 0                 // 열린 모음 획 수(연습 자판 획 자국, 0.19)
    val center: CenterMark? get() = null           // 0.21: 연습 자판 가운데 자음 = (조합기 lastConsonant, C), LLD-04 9.5. 0.20의 composing(가운데 음절) 대신
    val consonantPlace: ConsonantBox? get() = null // 0.21: 연습 자판의 C(기하가 정해지면). 3-1 판정이 씀(2.5)
    fun preview(s: VowelStroke): String? = null    // 이 획을 이었을 때 보일 글자(연습 자판 안내, 0.19)
}
```

### 2.2 과와 단계 (HLD 7.1·7.2, SWIPE 10장 0.29, 이슈 #127, #150, #154, #156, #168)

튜터는 **가르치는 과정**이다. 과마다 설명(쓰는 법을 모두, "이렇게도 쓸 수 있어요")을 먼저 보이고(`intro`), 연습 항목을 **정해진 순서로 번갈아**(짝을 나란히) 되풀이한다. 쓰는 법마다 항목이 따로 있다(이슈 #156로 모음이 한 획뿐이라 지금은 쓰는 법이 여럿인 글자가 없다). 문구에는 측정·재기라는 말을 쓰지 않는다. 표본은 연습 획에서 조용히 모은다. 항목은 목록 순서로 돌고(`targetIndex`가 끝에서 처음으로), **맞힌(✓) 세는 항목마다 `minSamplesPerTarget`번** 하면 단계가 끝난다(D-41, 2.5). 틀리면(✗) 같은 항목을 다시 한다(`targetIndex` 그대로). 누르고 있기(쌍자음) 항목은 0.13에서 없앴다(이슈 #142).

**연습 방식(0.19, 이슈 #173):** 연습은 실제 자판과 같다 — 자음은 탭, 모음은 필순대로 획마다 손을 떼며 긋는다(가 = ㄱ 탭, ㅣ, 가로 점). 동작 수 = 자음 탭 1 + 모음 획 수(가 3, 그 2, 과 5, 3-4는 3). `PersonalTutorSteps.Script`는 획 수와 판정 동작 수가 같아야 한다(생성 시 검사). 0.18의 "자음 키에서 긋기 = 자음 + 모음"(한 획)은 없앴다.

**연습 자음 돌림(0.18, 이슈 #168, SWIPE 10장 "연습 자음을 돌려 쓴다"):** 항목 목록(아래 표)은 ㄱ으로 쓴 **틀**(`TargetAction.template`)이고, 화면의 항목(`label`)은 틀의 초성을 돌림 자음으로 바꾼 글자다(`withConsonant`, `HowTo.withInitial("가", "ㄴ")` = 나). `TutorSession`이 정한다.
- 순서 `HowTo.ROTATION` = ㄱ ㄲ ㄴ ㄷ ㄸ ㄹ ㅁ ㅂ ㅃ ㅅ ㅆ ㅇ ㅈ ㅉ ㅊ ㅋ ㅌ ㅍ ㅎ(사전 순 19개). 돌리는 항목(`rotates`)이 하나 나올 때마다 다음 자음을 쓰고(`rotation` 순번), 끝까지 가면 ㄱ으로 돌아가며 바퀴가 바뀌어도 이어서 센다.
- 짝의 둘째(`pairedWithPrevious`: 1-2의 둘째 그·둘째 기, 3-2의 교, 0.19: 1-2의 기)는 앞 항목의 자음을 그대로 쓰고 순번을 쓰지 않는다(짝을 한 번으로 셈). 1-2 한 바퀴(0.19) = 가(ㄱ) / 꺼(ㄲ) / 노(ㄴ) / 두(ㄷ) / 뜨 띠(ㄸ, 짝) / 아 오 이, 다음 바퀴는 ㄹ부터.
- ✗ 뒤에는 `targetIndex`와 자음이 그대로다(같은 항목 다시). 자음은 다음 항목으로 넘어갈 때(`nextAction`)만 고른다(`pickConsonant`).
- 고정(`rotates` = false, 순번 안 셈): 1-2의 아·오·이(ㅇ), 1-1(기본 자음 14개를 차례로, PM 결정 #168 — 돌림 대상이 아님), 3-4(가, "돌림의 첫 자음" = ㄱ), 5-1 문장. 3-1의 의는 돌린다(긔, 늬 …).
- **다시 시작(D-56):** 다음 단계로 넘어갈 때(`Accepted`)와 겹쳐서 같은 단계를 다시 할 때(`Overlap`, `restartStep`) 순번을 0으로 돌려 ㄱ부터 쓴다. SWIPE 10장은 "단계가 바뀌면 처음(ㄱ)부터"만 정한다. 다시 하는 단계도 처음 하는 단계와 같은 차례로 보이게 했다(`targetIndex`·진행 수도 처음으로 돌아가는 것과 맞춤).
- 판정·쓰는 법 안내·✓ 뒤 목표 글자는 `label`(돌림 자음을 넣은 글자)로 만든다(`itemSpec(label)`). 표본 추출(`extract`)은 틀의 측정 규칙(`Script.strokes`)을 쓴다(자음과 무관, SWIPE 10장 설계자 확인).

`extract`는 판정 전 원시값을 낸다. 거리는 `stroke-core`의 변 위치 기준(LLD-04 3절), 문턱으로 자르지 않는다. 0.18부터 시간 간격 표본은 없다(`TutorMeasure.interval` 지움).

| 단계 | 연습 항목(번갈아 순서) | 동작 | 항목당 | 표본 | 정하는 칸(공식) |
|---|---|---|---|---|---|
| 1-1 | 자음 14개를 차례로 한 번씩 탭(0.23: 공개판 쿼티 1-1 삭제) | 탭 | 1 | 탭 = 누른 점 → 뗀 점 변위의 큰 성분(`TutorMeasure.length`, D-61) → `MIN_CROSS` SHORT(0.23: public `jitter` 삭제) | 없음(0.22: `minCrossDp` 고정) |
| 1-2 | 가 거 고 구, 그 / 기(돌림, 기는 짝), **아 오 이**(ㅇ 고정, 연습만) | 자음 탭 + 획 1~2(필순) | 5 | 자음 탭 → `MIN_CROSS` SHORT. 점 획 → `MIN_CROSS` LONG과 `LONG` SHORT. 긴 획 → `LONG` LONG(길이 = 축 성분). 가 거 고 구의 둘째 획 시작점 ↔ 기준 자리 → `POSITION` | `longDp` = `boundary(점, 긴 획)`, `calibration`(0.22: `minCrossDp`는 고정, 탭 ↔ 점은 기록만) |
| 3-1 | 갸 겨 교 규 개 게 걔 계 과 괘 괴 궈 궤 귀 긔(돌림) | 자음 탭 + 필순 획 | 1 | – | 없음(연습만, 한 바퀴) |
| 3-4 | 가 — 천지인 키(ㄱ 고정) | 탭, 천지인 긋기, 천지인 탭 | 1 | – | 없음 |
| 5-1 | 문장 4개, [다음] | 자유 | 1 | – | 없음(타수·정확도 표시만) |

- **1-2 획의 역할**은 항목의 필순(`HowTo.STROKES`)이 정한다(가의 둘째 획 = 점, 첫째 = 긴 획). 1-2는 길이를 판정하지 않으므로 "길게 그은 점"도 ✓이고 점 표본에 들어간다(SWIPE 10.1). 표본 수: 탭 14 + 30, 점 20(가 거 고 구 × 5), 긴 획 30.
- **위치 오차(`POSITION`, FR-TUT-13, 판정·추정에 안 씀):** 가 = |점 시작 x − ㅣ 시작 x|, 거 = |ㅣ 시작 x − 점 끝 x|, 고 = |ㅡ 시작 y − 점 끝 y|, 구 = |점 시작 y − ㅡ 시작 y|. `Script.position`.
- **겹침과 다시 하기(SWIPE 10, FR-TUT-03):** 1-2를 마칠 때 `LONG` 경계가 겹치면(`s90 ≥ l10`. 0.22: `MIN_CROSS`는 고정이라 겹쳐도 다시 하지 않고 기록에만 남는다) 1-2를 처음부터 다시 한다(`restartStep`: 이 시도의 표본을 버림, 1-1 탭은 `pool`에 남음). 더 하기는 `MAX_EXTRA_ROUNDS` = 2번까지이고, 셋째에도 겹치면 공식 값(`ThresholdEstimator.midpoint`, 겹쳐도 (P90 + P10) / 2)을 쓰고 `Accepted`(기록에 간격 ≤ 0과 `extraRounds` 2가 남음). 안내 문구는 `tutor_overlap_length` 하나다(0.22: `MIN_CROSS`는 겹침을 내지 않으므로 탭 겹침 안내 `tutor_overlap_tap`과 `Message.OVERLAP_TAP`을 지웠다, 오너 "튜터에 불필요한 내용은 제거해"). 0.21까지: `MIN_CROSS` → `tutor_overlap_tap`("자음은 손가락을 움직이지 말고 톡 누르고, 점은 짧게 그어 보세요"), `LONG` → `tutor_overlap_length`("점은 짧게, ㅡ·ㅣ는 길게 그어 보세요").
- **`calibration`(FR-TUT-13):** 1-2가 받아들여질 때 `TutorSession.calibration` = `Calibration(tapDot = 경계 기록(MIN_CROSS), dotLong = 경계 기록(LONG), position = PositionRecord(P90, P95, n), at = "")`. 경계 기록 = `BoundaryRecord(shortP90, longP10, gap = longP10 − shortP90, nShort, nLong, extraRounds)`. 마지막 단계 뒤 `TutorActivity`가 `ThresholdStore.save(값, calibration.copy(at = 지금 ISO-8601))`. public은 기록 없음.
- **지운 것(0.19):** 3-2(문지르기)와 3-1의 측정(ㅣ 추가·되돌아오기·꺾기), `Measure.RUB`·`VERT_*`·`RETURN_*`·`TURN_*`·`EXIT_*`, `TutorMeasure.longIndex`·`rubExcursions`·`vertAfter`·`maxReturn`·`maxTurn`·`SEG_DP`, `Rule.MIN_CROSS_RECHECK`, `tutor_prompt_3_2`, `tutor_overlap`.

**5-1 문장**(`SentencesStep`): "아이가 고기를 먹어요", "밖까지 과일을 사러 가요", "박가가 곡기를 끊었다", "안녕하세요 학교에 갔다 왔어요"(0.14: SWIPE 10장 예의 안녕하세요·학교·했다 계열을 넣음). 연습 자판은 `SWIPE_SENTENCE`(자음 키 + 천지인 키 + 스페이스 + ⌫, 0.17). 획은 ⌫ 획까지 `sentenceStrokes`에 모으고(걸린 시간 = 첫 획 누름 → 마지막 획(⌫ 포함) 뗌, FR-TUT-10) [다음]에서 `endSentence()`. 키 크기 기록은 스페이스·⌫ 획을 뺀다.
- **5과 ⌫(0.17, 이슈 #165, PM 결정, SWIPE 10장):** `PracticeKey.backspace`(SWIPE_SENTENCE의 ⌫ 칸만 연습 키) → `RawStroke.backspace`. `SwipePracticeEcho`가 ⌫ 칸의 획을 받으면 `InputDispatcher.backspace`(LLD-08 6.1)와 같이: 조립기가 열려 있으면 `popElement` → `ToVowel`이면 `undoLast`, 모음을 만든 요소였으면 `undoLast` / 아니면 조합기 `backspace()` → `NothingComposing`이면 확정 글자 맨 끝 한 글자(코드 포인트)를 지움 / 끝에 (0.18: 기준 키 복원 `mapper.onBackspace`는 지움). 입력 줄은 그 결과를 그대로 보인다. 연습 자판은 뗄 때 한 번만 처리하고 누르고 있어도 되풀이하지 않는다(D-55). 타수·정확도는 [다음] 때의 입력 줄로 계산하므로 지운 글자는 들어가지 않는다(지운 횟수는 세지 않음). 측정하지 않으므로 `finishStep`은 늘 `Accepted`(값 그대로)다. `minSamplesPerTarget` = 1.

**화면의 과 번호(0.18, SWIPE 10장 "화면 번호는 연속, 화면 문구는 LLD"):** 단계 id·`lesson`은 문서 번호 그대로(1-1, 1-2, 3-1, 3-4, 5-1, 0.19: 3-2 없음). 화면은 차례로 매긴다: 과 제목 `tutor_lesson_1_title` = "1과 기본", `tutor_lesson_3_title` = "2과 복모음", `tutor_lesson_5_title` = "3과 문장"(`lessonTitle(lesson)`: 1 → 1과, 3 → 3과 리소스, 그 밖 → 5과 리소스). 단계 줄의 단계 번호도 화면 번호다: `TutorFlow.screenId(id)` = 앞 숫자를 1 → 1, 3 → 2, 5 → 3으로 바꿈(3-4 → 2-4, 5-1 → 3-1). 2과·4과 문자열은 지웠다.
- **1과 설명(`tutor_lesson_1_intro`, 0.18, SWIPE 10장 1과 설명):** ① 자음 키에서 그으면 그 자음 + 모음(가 = ㄱ 키에서 오른쪽으로) ② 받침은 그 자음 키 탭(각 = 가 다음 ㄱ 탭) ②-1 쌍자음은 위 줄 전용 키(까 = ㄲ 키에서 긋기, 밖 = 바 다음 ㄲ 탭) ③ 모음으로 시작하는 글자는 ㅇ 키에서 긋기(아 = ㅇ 키 →). "측정"·"재기" 같은 말은 쓰지 않는다.

- 단계 순서대로 진행하고, 앞 단계에서 정한 값(최소 통과 거리)을 뒤 단계 측정에 쓴다. 앞 단계의 획을 뒤 단계 SHORT 표본으로 쓰는 것(3-1의 1-2 표본)은 LLD가 정한 측정 방법이다(D-19).
- ~~public은 1-1만(`MinCrossOnQwertyStep`, 쿼티 글자 키)~~ — 0.23에서 삭제(이슈 #251).
- 1-1 목표(0.13, 이슈 #144 PM 결정): `TutorSteps.CONSONANTS` 14개를 차례로 한 번씩(`minSamplesPerTarget` = 1, 진행 1/14 … 14/14; 0.23: public q~p 10개 삭제). 0.15(이슈 #154): **누른 키만** 판정한다. 목표가 아닌 키를 누르면 ✗ 후 같은 목표를 다시 탭하고 그 터치는 표본이 아니다. 움직임(탭/긋기)은 보지 않으므로 흔들려 실제로는 긋기가 된 터치도 ✓다(표본은 3절 이상값 처리를 따름). 저장 형식은 그대로다.
- **3-3(나눠 쓰기)은 0.15에서 지웠다**(이슈 #156: 모든 모음은 한 획). `STEP_3_3`, 문자열 `tutor_prompt_3_3`, 3과 설명(`tutor_lesson_3_intro`)의 "두 번에 나눠 쓸 수도 있습니다" 문장을 지우고, 설명 끝에 "모음은 언제나 한 획, 손을 뗐다 다시 그으면 모음이 따로 들어감(고 다음 오른쪽 = 고ㅏ)"을 넣었다.
- 기준값 `guideDelayMs`는 측정하지 않는다. 측정 당시 키 크기를 함께 저장한다(스페이스 획은 제외).

### 2.3 TutorActivity (Compose)

| 함수 | 처리 | 근거 |
|---|---|---|
| `onCreate` | `steps = SwipeModuleProvider.get().tutorSteps()`, `TutorSession` 생성, 첫 단계 화면 | FR-TUT-02, 08 |
| 과 설명(`TutorFlow.intro`) | 단계의 `intro`가 있고 아직 보이지 않았으면 과 제목·설명과 [연습 시작]만 보이고 연습 자판은 숨긴다. [연습 시작] = `startPractice()`. 같은 단계를 다시 할 때(겹침)는 다시 보이지 않는다. 설명 중의 획은 무시 | FR-TUT-02, SWIPE 10장 |
| 단계 줄 | `tutor_step` = "2-1단계 (3 / 6)"(화면 단계 번호 `screenId(id)`, 순번, 단계 수, 0.18) | SWIPE 10장 |
| 5과 [다음](`nextSentence`) | `SentenceStep`이면 [단계 끝] 대신 [다음]. 그 문장의 타수·정확도를 계산해 보이고(`lastSentence`, 다음 획까지), `session.endSentence()`, 입력 줄을 비움. 마지막 문장 뒤에는 `finishStep()` | FR-TUT-10, 09 |
| 끝 화면 | 5과를 했으면 전체 타수·전체 정확도(`overall`) | FR-TUT-10 |
| 연습 자판(`PracticeKeyboard` 컴포저블 안의 `AndroidView`) | 실제 키보드와 같은 키 크기(같은 배치 데이터, 같은 줄 높이 설정)로 그리고 터치 점을 `RawStroke`로 모은다. 획마다 `RawStroke`와 자판 폭(dp)을 넘긴다. 같은 터치를 `PracticeGuide`(있으면)에도 넘긴다(down·move·up, 취소·크기 바뀜 = up, 창에서 떨어짐 = dismiss) | HLD 7.1 |
| 연습 자판의 안내(0.11, 이슈 #135) | `PracticeLayouts.guide(layout, module, 자판 뷰, th, settings)` = `PracticeLayoutSource.practiceGuide(...)`. 긋기 배치(모든 과)는 실제 긋기 키보드와 같은 안내(LLD-06 4절). 0.18: 모든 자음 키(쌍자음 키 포함)·천지인 키에서 띄운다(기준 키 조건과 `anchorKeyId` 인자 지움). `th`·`settings`는 `onCreate`에서 읽은 값("입력: …" 줄과 같은 기준값). 표시 전용 | FR-TUT-11, FR-DSP-08 |
| 연습 자판의 누름(0.15) | `PracticeKeyboard(..., onPress)` → `PracticeKeyboardView`가 연습 키에서 터치가 시작될 때 `onPress()`를 부른다. **안내의 `down`보다 먼저** 불러서, ✓·✗ 뒤 첫 누름에 새 `PracticeEcho`를 만든다 | FR-TUT-12 |
| 오류 진동(0.15, D-48 바뀜) | `TutorFlow(..., errorVibe)` = `Feedback({ window.decorView }, AudioManager, { settings }).error`(LLD-08 8절, 진동 설정을 따름). 0.18: 입력 표시가 그 획을 **이어질 수 없어 무시된 천지인 요소**로 해석했을 때만 낸다(`echo.lastIgnored`, 실제 키보드의 오류 진동과 같음, SWIPE 10.1 "튜터에서 나는 오류 진동은 무시된 천지인 요소뿐"). 그 동작은 ✗다(3-4) — 5과에서는 판정 없이 진동만. 기준 키 밖 긋기 진동(0.15~0.17)은 없어졌다. `PracticeEcho`는 스스로 진동하지 않는다 | FR-TUT-12, FR-SWP-62 |
| 입력 표시(`TutorFlow.echoAsLine`·`echoChar`·`echoText`) | 아래 "입력 표시". 5과는 "입력: …" 줄, 1~3과는 마지막 한 글자를 크게(0.13, 이슈 #143) | FR-TUT-09 |
| 단계 끝 버튼 / 표본 수 충족 | `finishStep()`: `Overlap`이면 "한 번 더 연습해 볼게요" 문구 뒤 같은 단계 다시(FR-TUT-03, 5-1이면 문장 결과도 비움), `Accepted`면 다음 단계 | FR-TUT-03 |
| 마지막 단계 끝 | `ThresholdStore.save(session.current)` | FR-TUT-04 |
| 건너뛰기 | 저장하지 않고 닫음. 키보드는 기본값(또는 이전에 저장한 값)으로 동작 | FR-TUT-05 |

#### 입력 표시 `PracticeEcho` (FR-TUT-09, 안드로이드 없음)

- `TutorFlow(session, echoOf, errorVibe, save)`: `echoOf(layout)`가 새 `PracticeEcho`를 만든다. 처음, `finishStep()`이 `Accepted`(다음 단계, 마지막 단계 뒤에는 없음)·`Overlap`(같은 단계 다시)일 때, 그리고 0.15부터 **판정(✓·✗)이 보이는 동안 다음 동작을 누를 때**(`onPress`, 2.5: 다음 항목 또는 같은 항목 다시) 새로 만들어 줄을 비운다. 그래서 판정하는 항목은 시도마다 천지인 조립·조합 중 글자가 비어서 시작한다(앞 항목의 모음 뒤에 붙지 않고, 도깨비불로 앞 받침이 넘어오지 않음). `NotEnough`는 단계가 그대로이므로 비우지 않는다.
- `TutorFlow.onStroke(raw, widthDp)`: 먼저 `onPress()`(누름이 오지 않은 경우, 시험), `echo.onStroke(raw, widthDp)`, 그다음 `session.onStroke(raw)`(측정·판정)와 판정 표시(2.5). 표본 수가 차서 단계가 끝나면 그 자리에서 줄을 비운다(✓ 뒤 목표 글자는 다음 누름까지 남음). 해석 중 예외는 잡고 그 획만 표시하지 않는다(측정은 계속, 실제 키보드에서도 실패한 획은 아무것도 치지 않음). `widthDp ≤ 0`(아직 배치 전)이면 해석하지 않는다.
- 보이는 꼴(0.13, 이슈 #143 PM 결정): `echoAsLine` = 지금 단계가 `SentenceStep`(5과)인가.
  - **5과(`echoAsLine` = true)**: `echoText`를 `tutor_progress` 아래 한 줄(`입력: %1$s`)에 보인다. `text`가 `ECHO_MAX`(= 20자, D-38)보다 길면 `"…"` + 끝 19자. 그 아래 문장 타수·정확도(`lastSentence`).
  - **그 밖(1~3과)**: 줄 대신 `echoChar`만 `tutor_progress` 아래에 크게(64sp, 높이 80dp 상자 가운데, 비어도 상자 높이는 그대로라 화면이 흔들리지 않음) 보인다. `echoChar` = `text`의 마지막 코드 포인트(`TutorFlow.lastChar`), 없으면 "". `text` = 확정 글자 + 조합 중 글자이므로 조합 중이면 그 글자, 아니면 마지막 확정 글자다(도깨비불로 새 글자가 되면 그 글자). 앞 글자와 이어 붙이지 않는다. 앞머리 "입력:"은 붙이지 않는다.
  - 화면 높이(0.13, #143 검수 PM 결정): 단계 줄부터 메시지까지의 글은 버튼 위 남은 칸을 차지하고 모자라면 세로로 스크롤한다(`ScrollingTexts`, 과 설명 화면도 같음). [건너뛰기]·[단계 끝]/[다음] 줄과 연습 자판은 스크롤하지 않아 늘 보인다.
  - 둘 다 같은 `PracticeEcho`(같은 엔진·같은 기준값)에서 읽고, 비우는 때(아래)도 같다. 측정·표본은 읽지 않는다. 0.15: 1~3과는 큰 글자 오른쪽에 판정 표시(✓/✗, 2.5)를 두고, ✓ 뒤에는 `echoChar` = 항목의 `glyph`(목표 글자, TBD-17 = A)다. 5과는 판정이 없다.
- 판정 기준값: `onCreate`에서 읽은 저장값(`ThresholdStore.load()`, 없으면 기본값)을 `TutorSession` 시작 값과 같이 넘긴다. 튜터는 마지막 단계 뒤에만 저장하므로 화면이 보이는 동안 이것이 "그 시점의 현재 기준값"이다. 튜터가 단계마다 고치는 `session.current`는 쓰지 않는다(측정·추정 불변).
- 만드는 곳: `PracticeLayouts.echo(layout, module, th, rowHeightDp)` — 모듈이 `PracticeLayoutSource.practiceEcho(...)`로 낸 것, 없으면 쿼티(`QwertyPracticeEcho`).

| 구현 | 연습 자판 | 해석(실제 키보드와 같은 경로) |
|---|---|---|
| ~~`QwertyPracticeEcho`~~ (0.23 삭제, 이슈 #251) | 쿼티 글자 키(public 1-1) | `TapGeometry.build(TapLayouts.QWERTY, widthDp, rowHeightDp)`에서 첫 점의 키(`indexAt`), `TapGestureClassifier(geom, minCrossDp)`에 점을 넣고 `TapKeyResolver.resolve`(LLD-05A 2.2·4절)의 `text`를 붙임. 탭 = 소문자, ↑ 나감 = 대문자 등 동작 표 그대로 |
| `SwipePracticeEcho(th, rowHeightDp)` (`swipe.impl`) | 긋기 자판 자음 키·천지인 키(1~3과, 0.16), 자음 키·천지인 키 + 스페이스(5과) | `SwipeGeometry.build(widthDp, rowHeightDp)`에서 첫 점의 키. 스페이스면 조합을 끝내고 " "를 붙이고 `mapper.reset()`(실제 스페이스 키와 같음). 자음 키면 `StrokeClassifier(key, th, geom)` begin → update(가운데 점) → finish(마지막 점) → `SwipeInputMapper.accept(r)`(긋기 = 그 키의 자음 + 모음, 0.18) → `Jamo`면 `HangulComposer(ComposePolicy.SWIPE).applyAll` (LLD-04 9.3과 같음). `text` = 확정된 글자 + `composingText()` + `mapper.pendingGlyph()`(0.16: 천지인 ㆍ·‥), `lastIgnored` = 이 획의 결과가 `Ignored`(이어질 수 없는 천지인 요소, 0.18). 천지인 키 획도 같은 길(`StrokeClassifier` → `accept`의 `Element`, LLD-04 6.2) |

- 5-1에서는 문장마다 새 `PracticeEcho`를 만든다(줄을 비움, FR-TUT-09).
- 시험: TU-13~TU-17, TU-22, TU-23, TU-27~TU-31(0.13 입력 표시), TU-30·TU-17·TU-42(0.15 판정 표시).

### 2.4 TypingStats (FR-TUT-10, 안드로이드 없음)

```kotlin
data class SentenceStats(val keystrokes: Int, val millis: Long, val accuracy: Double, val perMinute: Double?)
data class OverallStats(val perMinute: Double?, val accuracy: Double?)
object TypingStats {
    fun keystrokes(text: String): Int
    fun editDistance(a: String, b: String): Int
    fun accuracy(target: String, typed: String): Double
    fun sentence(target: String, typed: String, millis: Long): SentenceStats
    fun overall(list: List<SentenceStats>): OverallStats
}
```
- `keystrokes`: 글자마다 두벌식 키 수. 완성 음절은 초성 1 + 중성(ㅘ ㅙ ㅚ ㅝ ㅞ ㅟ ㅢ = 2, 나머지 1) + 종성(없음 0, 겹받침 ㄳ ㄵ ㄶ ㄺ ㄻ ㄼ ㄽ ㄾ ㄿ ㅀ ㅄ = 2, 나머지 1 — 쌍받침 ㄲ ㅆ도 1). 호환 자모 하나는 같은 규칙(겹받침·두 키 복모음 2, 나머지 1). 그 밖(띄어쓰기·숫자·기호·영문) 1. 시프트는 세지 않는다. 곡기 = 5, 과일 = 6.
- `editDistance`: `String`의 문자(음절·공백·기호) 단위 레벤슈타인 거리.
- `accuracy` = max(0, (보인 문장 글자 수 − 편집 거리) ÷ 보인 문장 글자 수 × 100).
- `sentence`: 걸린 시간 = 그 문장 첫 획의 첫 점 → 마지막 획의 마지막 점(`TutorFlow`가 셈). 친 글자가 비었거나 시간이 0 이하면 `perMinute = null`("–"). 정확도는 그대로 계산(빈 줄 = 0%).
- `overall`: 전체 타수 = `perMinute`가 있는 문장들의 키 수 합 ÷ 그 시간 합(분), 없으면 null. 전체 정확도 = 모든 문장 정확도의 평균, 문장이 없으면 null.
- 화면 표시는 반올림한 정수, null은 "–"(`tutor_sentence_stats`, `tutor_overall_stats`). 표시만 하고 기준값 계산에 쓰지 않는다.
- 시험: TY-01~TY-04.

#### 첫 실행 자동 시작 `TutorLaunchPolicy` (안드로이드 없음)

```kotlin
object TutorLaunchPolicy {
    fun shouldAutoStart(s: Settings): Boolean = !s.tutorAutoStarted
}
```
- 앱을 열 때(런처 화면 = `SettingsActivity.onCreate`) `shouldAutoStart(settings)`이면 **먼저** `SettingsStore.setTutorAutoStarted(true)`를 저장하고(D-34) `TutorActivity`를 띄운다. 그래서 처음 설치 후 처음 열 때 **한 번만** 자동으로 시작하고, 사용자가 끝까지 하든 건너뛰든 중간에 앱을 닫든 그 뒤에는 자동으로 시작하지 않는다(SRS FR-TUT-01, SWIPE 10장 실행 시점).
- 그 뒤 튜터는 설정의 "튜터 다시 실행"으로만 시작한다(FR-TUT-06, FR-SET-04, 7절).
- 앱을 열기 전에 키보드부터 켜서 써도 키보드 쪽에서는 튜터를 띄우지 않는다. 기본값으로 동작한다(FR-TUT-05).
- **기본값으로 동작 중이라는 안내는 두지 않는다**(키보드 문구·설정 화면 안내 모두 없음, SRS FR-TUT-05, 오너 결정 2026-10-27 이슈 #3 — DCR-10을 대체). 그래서 기준값 상태(`LoadStatus`)를 화면에 보이는 곳이 없다.
- 시험: TU-01~TU-12.

### 2.5 항목 판정 `TutorItemJudge`·`HowTo` (FR-TUT-12, HLD 7.1, SWIPE 10.1절, 0.19)

`app` main(`kbd.app.tutor`)의 순수 Kotlin. core-hangul의 `VowelStrokeAssembler`를 쓴다(3-1).

```kotlin
sealed interface ExpectedAction {
    data class Press(val key: String)            // 1-1, 1-2 ①: 그 키에서 시작한 터치(움직임 무관)
    data class Tap(val key: String)              // 3-1 ①, 3-4 ①: 실제 판정의 탭(변위 큰 성분 < minCrossDp)
    data class Stroke(val vertical: Boolean)     // 1-2: 그 축이고 → / ↓인 획(길이 무관, 변위 0 = ✗, D-60)
    data class Written(val vertical: Boolean)    // 3-1: 실제 판정의 획(≥ minCrossDp), 축·방향이 맞고 조립기가 받음, 마지막이면 남은 모음 = 목표
    data class Element(val e: CjElement)         // 3-4 ②③
}
data class ActionRecord(val key: String, val x0: Float, val y0: Float, val x1: Float, val y1: Float, val tap: Boolean, val firstEdge: Edge?) {
    val length: Float; val vertical: Boolean?; val forward: Boolean; fun stroke(): VowelStroke; val element: CjElement   // 0.21: 길이 표시 없는 획
}
data class ItemSpec(val glyph: String, val actions: List<ExpectedAction>, val howTo: String, val vowel: Jamo? = null)
object TutorItemJudge { fun record(raw, th: Thresholds): ActionRecord; fun judge(spec, records, th, consonantBox: ConsonantBox? = null): Judgement }
object HowTo {
    data class VowelStep(val vertical: Boolean, val long: Boolean, val place: String)
    val STROKES: Map<Char, List<VowelStep>>      // 21모음 필순(SWIPE 3.3) + 위치 말(SWIPE 10.1)
    fun measured(glyph): ItemSpec                // 1-2: [Press(c)] + Stroke(축)…
    fun written(glyph): ItemSpec                 // 3-1: [Tap(c)] + Written(축)…, vowel = 목표 모음
    fun press(key); fun cheonjiin(glyph, elements); fun stroke(step): String; const val REVERSE_HINT
}
```

- **동작 기록:** 누른 점·뗀 점, `tap` = 큰 성분 < 결정 `minCrossDp`(세션 시작 값 = 입력 표시와 같은 값, FR-TUT-09), 천지인 키는 `firstEdge`(LLD-04 4.7과 같은 규칙). `vertical` = |dy| > |dx|(같으면 가로, LLD-04 4.3 ②), 변위 0이면 null.
- **판정 표:** `Press(k)` = `key == k` / `Tap(k)` = `tap && key == k` / `Stroke(v)` = `vertical == v && forward` / `Written(v)` = `!tap && vertical == v && forward`이고, 시도의 `Written` 기록들을 차례로 `VowelStrokeAssembler(posTolDp)`에 `push(stroke(), consonantBox)`로 넣어(0.21: 연습 자판의 C로, 입력 표시와 같은 판정, SWIPE 10.1) 모두 받아들여지고(마지막이면) `vowel == spec.vowel` / `Element(e)` = 천지인 키 && `element == e`.
- **변위 0 = ✗(LLD 결정 D-60):** SWIPE 10.1 "변위가 0이라 축을 정할 수 없는 동작은 방향이 다른 것으로 보아 ✗(세부는 LLD)". 조금이라도 움직였으면 축을 정한다(1-2는 거리와 상관없이 획으로 봄).
- **반대 방향 덧붙임:** 세션이 `Stroke`·`Written` 차례에 ← / ↑ 기록으로 ✗이면 `reversedLast = true`. `TutorFlow`는 안내 줄 뒤에 줄을 바꿔 `REVERSE_HINT`("획은 아래로, 오른쪽으로만 그어요")를 붙인다.
- **쓰는 법 문구(SWIPE 0.35 10.1, 0.21):** `<글자> = <자음> 탭 → <획> → …`. 획 = 위치 말 + 동작 말(세로 `아래로`, 가로 `오른쪽으로`). **"길게·짧게"는 쓰지 않는다**(이슈 #179). 위치 말: 첫 획은 자음 기준 — ㅣ "자음 오른쪽에서 ", ㅡ "자음 아래에 ", ㅗ·ㅛ의 점 "자음 아래에 ", ㅓ·ㅕ의 점 "자음 오른쪽에 ". 이어지는 획 — ㅏ·ㅑ·ㅘ의 점 "ㅣ 오른쪽에 ", ㅓ·ㅔ의 ㅣ "점 오른쪽 끝에서 점이 위나 가운데에 오게 ", ㅢ의 ㅣ "ㅡ 오른쪽 끝에서 ㅡ가 아래쪽에 오게 ", ㅕ·ㅖ·ㅝ·ㅞ·ㅐ·ㅒ·ㅙ의 그 ㅣ "점 오른쪽 끝에서 ", ㅗ·ㅛ·ㅚ·ㅘ·ㅙ의 ㅡ "점 아래에 ", ㅜ·ㅠ·ㅟ·ㅝ·ㅞ의 세로 점 "ㅡ 아래에서 ", ㅚ·ㅟ·ㅘ·ㅙ의 ㅣ "ㅡ 오른쪽 끝에서 ", ㅕ·ㅖ의 둘째 점 "첫 점 아래에 ", ㅛ의 둘째 점 "첫 점 오른쪽 옆에 ", ㅝ·ㅞ의 가로 점 "ㅜ의 점 오른쪽에 ", ㅔ·ㅖ·ㅞ의 마지막 ㅣ "앞 ㅣ 오른쪽 옆에 ". 보기(SWIPE 10.1과 같음): 가 = `ㄱ 탭 → 자음 오른쪽에서 아래로 → ㅣ 오른쪽에 오른쪽으로`, 과 = `ㄱ 탭 → 자음 아래에 아래로 → 점 아래에 오른쪽으로 → ㅡ 오른쪽 끝에서 아래로 → ㅣ 오른쪽에 오른쪽으로`. `VowelStep.long`(ㅣ·ㅡ / 점)은 1-2 표본 종류에만 쓰고 문구에 나오지 않는다.
- **위치 말을 늘 붙임(LLD 결정 D-59, 0.21 개정):** 모든 획에 위치 말을 붙인다(첫 획은 자음 기준, SWIPE 10.1 "첫 획에는 자음 기준 자리를 붙인다"). 아·까도 같다(아 = `ㅇ 탭 → 자음 오른쪽에서 아래로 → ㅣ 오른쪽에 오른쪽으로`).
- **3-1과 C(0.21, TBD-24):** `TutorFlow.onStroke`가 `session.consonantBox = echo.consonantPlace`를 넣고 세션이 `judge(…, consonantBox)`로 넘긴다. 3-1 항목은 늘 자음 탭 뒤라 C로 판정하고, 입력 표시(`SwipePracticeEcho`)도 같은 C로 읽으므로 둘이 같다(SWIPE 10.1 "판정이 쓰는 기준값은 입력 표시와 같다"). 세션만 따로 돌리는 시험(입력 표시 없음)은 C가 null이라 낱모음 규칙으로 판정한다. 시험 TJ-09.
- **문구(strings, 0.21):** 1과 설명(`tutor_lesson_1_intro`)은 SWIPE 10 "1과 설명에 넣을 것"대로 흐린 자음 옆에 쓰기, 정해진 길이 없음, 떨어져도 됨을 넣고 길이 말을 뺐다. 2과(문서 3과) 설명, 1-2 지시문(`tutor_prompt_1_2`), 탭 ↔ 획 겹침 안내(`tutor_overlap_tap`, 0.22에 삭제 = "자음은 손가락을 움직이지 말고 톡 누르고, 모음은 손가락을 움직여 그어 보세요", SWIPE 10)도 맞췄다. 점 ↔ 긴 획 겹침 안내(`tutor_overlap_length`)는 0.21 2회차에서 길이 말 없이 고쳤다. 그 측정(`dotLong`)·다시 하기는 HLD 0.40 형식 6에서 없어지는 것이라 이슈 #194로 넘겼다(README 6.21 남은 일).
- 시험: TJ-01~TJ-08, TU-43, TU-48~TU-50, TU-60.

#### 세션 `TutorSession.onStroke`(0.15~0.19)
1. 획을 `action`에 넣고 표본을 시도 표본에 넣는다. 2. `itemSpec`이 null이면 획 수로 다음 항목(문장 제외). 3. 아니면 `record(raw, start)`를 쌓고 `judge(spec, records, start)`: `Pending` 기다림 / `Right` 표본을 단계 표본으로, 다음 항목 / `Wrong` 시도를 버리고 같은 항목(같은 자음). 4. `judgement`·`judgedItem`·`reversedLast`는 그 획의 것(이슈 #163).

#### 화면 `TutorFlow`
- `verdict`, `howTo`(✗이면 항목 문구 + 반대 방향 덧붙임), 목표 글자 표시. `message` = `NONE`·`OVERLAP_LENGTH`·`NOT_ENOUGH`(0.22: `OVERLAP_TAP` 삭제). 0.21 2회차: `OVERLAP_LENGTH` 문구는 "한 번 더 연습해 볼게요. 흐린 자음을 기준으로 자리를 맞춰 다시 써 보세요"(길이 말 없음). 그 측정·다시 하기는 이슈 #194.
- 오류 진동: 입력 표시가 이 획에 실제 자판의 오류 진동을 낼 일(`echo.lastIgnored`: 무시된 획·천지인 요소, 조립을 닫으며 버린 획)이 있었으면 `errorVibe()`.
- 연습 자판: `openStrokes`(획 자국 수, LLD-04 9.4), `preview(s)`(안내, `SwipePracticeEcho`가 미리보기 전에 매퍼에 C를 넣음), `center`(가운데 자음, LLD-04 9.5, 0.21: `TutorFlow.center` → `PracticeKeyboard(center, redraw = tick)`, 터치 없이 입력 표시가 새로 되면 `AndroidView.update`가 다시 그림).

## 3. ThresholdEstimator

```kotlin
object ThresholdEstimator {
    fun boundary(short: List<Float>, long: List<Float>): EstimateResult   // Ok(value) | Overlap | NotEnough
    fun oneSidedDistance(jitter: List<Float>, marginDp: Float = JITTER_MARGIN_DP): EstimateResult   // Ok | NotEnough
    fun percentile(xs: List<Float>, p: Float): Float
    fun midpoint(short: List<Float>, long: List<Float>): Float   // 0.19: 겹쳐도 (P90 + P10) / 2(더 하기 두 번 뒤, SWIPE 10)
    fun toStoredMs(ms: Float): Long      // ms 칸을 반올림해 저장(쓰는 칸은 guideDelayMs뿐, 측정하지 않음)
    const val JITTER_MARGIN_DP = 4f      // 흔들림 여유 초기값(HLD 7.1)
}                                        // 0.14: oneSidedWindow·median·WINDOW_FACTOR 삭제(HLD 0.28 7.1 "시간 창 계수 없앰")
```

#### `oneSidedDistance(jitter, marginDp)` — (0.23: 쓰는 곳 없음. public 1-1과 함께 쓰임이 사라졌고 코드는 #224에서 정리)
- 사전: 크기 ≥ `MIN_SAMPLES`. 아니면 `NotEnough`.
- 처리: `Ok(percentile(jitter, 99) + marginDp)`. 분포가 하나라 겹침 검사는 없다(HLD 7.1, DCR-09 결정).

- **이상값 처리(0.15, D-51, SWIPE 10.1 "추정의 이상값 처리는 LLD", PM 결정 #154 Q3):** 측정 단계의 너그러운 판정으로 들어온 표본 중 분명히 벗어난 것은 표본으로 쓰지 않는다(시도는 ✓ 그대로, 진행 수에도 들어감). ① 1-1: 터치가 키 사각형 밖으로 `JITTER_OUTLIER_DP`(= 10dp, 재는 값이 아닌 고정 상수) 넘게 나갔으면 흔들림 표본 없음(`TutorMeasure.jitterSample`; 탭 흔들림은 키 안에서 끝나므로 10dp 밖은 탭이 아님). (② 2-1 간격 이상값은 2-1과 함께 0.18에서 지움.) 그 밖(길게 그은 가, 꺾지 않은 과)은 거르지 않고 겹침 검사(FR-TUT-03)와 백분위수 공식(90·10백분위)에 맡긴다. 표본이 빠져 `NotEnough`이면 그 단계를 이어서 한다(2.3).
- `JITTER_MARGIN_DP`는 측정값이 아니라 튜터 계산의 매개변수다. 저장 JSON에 넣지 않고, 구현 후 튜터를 실제로 돌려 기본값을 정할 때 함께 조정한다(HLD 7.1, FR-TUT-05).

#### `boundary(short, long)`
- 사전: 각 목록 크기 ≥ `MIN_SAMPLES`(= `minSamplesPerTarget`). 아니면 `NotEnough`.
- 처리: `s90 = percentile(short, 90)`, `l10 = percentile(long, 10)`. `s90 ≥ l10` → `Overlap`. 아니면 `Ok((s90 + l10) / 2)`(HLD 7.1).
- 쓰는 곳: 1-2의 두 경계(0.19).
- **탭 흔들림 값(LLD 결정 D-61):** 탭 표본은 누른 점 → 뗀 점 변위의 큰 성분(`TutorMeasure.length`, HLD 7.1 "4.2 판정과 같은 값")이다(0.23: public 1-1의 `jitter` 삭제, 이슈 #251).
- 근거: HLD 7.1, FR-TUT-03. 시험: TE-01~TE-10.

#### `percentile(xs, p)`
- 오름차순 정렬 뒤 선형 보간: 위치 `h = (n − 1) × p / 100`, `x[⌊h⌋] + (h − ⌊h⌋) × (x[⌊h⌋+1] − x[⌊h⌋])`(D-20). `n = 1`이면 그 값.

## 4. ThresholdStore

```kotlin
enum class LoadStatus { OK, DEFAULT_MISSING, DEFAULT_CORRUPT }
data class Loaded(val thresholds: Thresholds, val status: LoadStatus)
data class BoundaryRecord(val shortP90: Float, val longP10: Float, val gap: Float, val nShort: Int, val nLong: Int, val extraRounds: Int)
data class PositionRecord(val p90: Float, val p95: Float, val n: Int)
data class Calibration(val tapDot: BoundaryRecord?, val dotLong: BoundaryRecord?, val position: PositionRecord?, val at: String)   // FR-TUT-13, 0.19
object ThresholdCodec {                                  // 안드로이드 없음, JVM 시험
    fun encode(t: Thresholds, calibration: Calibration? = null): String
    fun decode(json: String): Thresholds                 // 실패 시 ThresholdFormatException. calibration은 보지 않음
    fun decodeCalibration(json: String): Calibration?    // 기록만, 못 읽으면 null(판정에 안 씀)
}
class ThresholdStore(private val filesDir: File) {
    fun load(): Loaded
    fun save(t: Thresholds, calibration: Calibration? = null)   // 0.19
    fun exportTo(out: OutputStream); fun importFrom(inp: InputStream): ImportResult   // Ok | Invalid
    fun resetToDefault()                                                              // FR-SET-04
}
```

#### `load()`
- 처리: `filesDir/thresholds.json`이 없으면 `Loaded(DEFAULTS, DEFAULT_MISSING)`. 읽기·해독 실패, `version` 모름, 값 범위 밖이면 `Loaded(DEFAULTS, DEFAULT_CORRUPT)`와 `ErrorLog.record("ThresholdStore.load", e)`(LLD-09 3절). 아니면 `OK`.
- `DEFAULT_*`면 기본값으로 동작한다. 따로 안내하지 않는다(FR-TUT-05, HLD 9장). `status`는 시험·오류 기록용이다.
- 부르는 곳: `KeyboardService.onCreate`, `TutorActivity` 시작, 가져오기 뒤.

#### `save(t)`
- 같은 폴더 임시 파일에 쓰고 `renameTo`로 바꾼다(쓰다가 죽어도 이전 파일이 남음, D-21).
- 사후: 키보드는 다음 입력부터 새 값(FR-TUT-04). `KeyboardService`는 `FileObserver` 대신 `onStartInputView`마다 수정 시각을 보고 다시 읽는다(D-22).

#### `importFrom(inp)`
- `decode` 성공이면 `save(값, decodeCalibration(글))`(기록도 함께, 0.19). 실패면 저장 안 함 + `Invalid`(설정 화면이 문구를 보임).

#### 값 범위 (`decode`의 검사, D-23)

| 칸 | 범위 |
|---|---|
| `minCrossDp` | 읽지 않음(0.22, 고정 8) |
| `longDp` | 10 ~ 300 (형식 5만) |
| `posTolDp` | 1 ~ 100 (형식 5만) |
| `guideDelayMs` | 0 ~ 2000 |
| `keySizeDp.w/h` | 10 ~ 300 |

- 시험: TS-01~TS-11.

## 5. 기준값 JSON 형식

HLD 7.2 형식 5(0.19, 이슈 #173)를 그대로 쓴다.

```json
{
  "version": 5,
  "keySizeDp": { "w": 51, "h": 48 },
  "minCrossDp": 14,
  "longDp": 51,
  "guideDelayMs": 120,
  "posTolDp": 20,
  "calibration": {
    "tapDot": { "shortP90": 6.1, "longP10": 18.4, "gap": 12.3, "n": [24, 30], "extraRounds": 0 },
    "dotLong": { "shortP90": 31, "longP10": 62.5, "gap": 31.5, "n": [30, 30], "extraRounds": 1 },
    "position": { "p90": 11.2, "p95": 14.8, "n": 40 },
    "at": "2026-11-01T10:00:00+09:00"
  }
}
```

- **형식 5**: `calibration`을 뺀 칸이 모두 필수다. `encode`는 늘 형식 5(`VERSION` = 5). `calibration`은 있을 때만 쓰고, `decode`는 읽지 않는다(판정에 안 씀). 잘못된 `calibration`은 `decodeCalibration`이 null로 넘긴다(파일은 OK).
- **형식 1~4 읽기**(`OLD_VERSIONS`): `keySizeDp`·`guideDelayMs`는 필수이고 범위를 검사한다(`minCrossDp`는 0.22부터 어느 형식에서도 읽지 않는다). `longDp`는 뜻이 달라(첫 나감의 변 너머 거리) **읽지 않고 기본값**, `posTolDp`도 기본값. `dwellMs`·끊는 간격 칸들·`doubleTapWindowMs`·`addIDp`·`returnLongDp`·`turnAngleDeg`는 읽지 않는다. 상태 OK, 다음 `save`가 형식 5로 바꿔 쓴다. 6 이상은 `DEFAULT_CORRUPT`.
- 시험: TS-01~TS-11.

- 모든 칸 필수. 모르는 칸은 무시한다(뒤 버전 호환). 숫자는 JSON number.
- 파일 이름 `thresholds.json`, 위치 `Context.filesDir`(HLD 7.3).
- 내보내기: `SettingsActivity`가 `ACTION_CREATE_DOCUMENT`(MIME `application/json`, 기본 이름 `keyboard-thresholds.json`)로 받은 URI에 `exportTo`. 가져오기: `ACTION_OPEN_DOCUMENT`(MIME `application/json`, `text/plain`) → `importFrom`(FR-TUT-07).

## 6. SettingsStore와 ErrorLog

### 6.1 SettingsStore (Jetpack DataStore Preferences)

```kotlin
data class Settings(val lastMode: InputMode?, val showDirectionGuide: Boolean, val showNextChar: Boolean,
                    val showCurrentChar: Boolean,   // 0.11, 이슈 #135
                    val haptic: Boolean, val sound: Boolean, val rowHeightDp: Float,
                    val graffitiLetterRatio: Float, val tutorAutoStarted: Boolean)
class SettingsStore(private val ds: DataStore<Preferences>) {
    val flow: Flow<Settings>
    fun loadBlocking(): Settings                    // runBlocking { flow.first() } — 서비스 시작 때 한 번(D-24)
    suspend fun setLastMode(m: InputMode); suspend fun update(f: (Settings) -> Settings)
    suspend fun setTutorAutoStarted(v: Boolean)
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
| `tutor_auto_started` | false | FR-TUT-01(첫 실행 자동 시작을 했는가, 2.3) |

- 저장된 `lastMode` 이름이 모르는 값이면 없음으로 본다(FR-CMN-10; 0.23: 모든 빌드가 네 모드라 FR-CMN-03b의 "빌드에 없는 모드" 경우는 없음, 이슈 #251).
- `graffiti_letter_ratio`는 Float 하나로 저장한다(`floatPreferencesKey`). 값은 패드 폭 가운데 **문자 영역의 몫**이다: 1 : 1 = 1/2, 2 : 1 = 2/3, 3 : 1 = 3/4(문자 : 숫자 = r : 1 − r). 설정 화면은 세 값만 쓴다(`GRAFFITI_RATIOS`, 7절). 읽을 때는 검사하지 않는다: 저장값이 없으면 기본값 2/3, 있으면 목록 밖 값이라도 그대로 `GraffitiPadGeometry`에 넘겨 그 몫으로 패드를 나누고, 설정 화면은 어느 선택지도 고르지 않은 상태(세 값과의 차가 모두 0.001 이상)로 보인다. 사용자가 하나를 고르면 그 값으로 덮어쓴다. 앱이 목록 밖 값을 쓰는 길은 없으므로(설정 화면이 유일한 쓰기) 이 경우는 저장 파일이 손상되거나 바깥에서 고친 때뿐이다.
- `row_height_dp`는 읽을 때 `RowHeight.clamp`로 40~64 안에 둔다(범위 밖 저장값은 가까운 끝값, D-33). 48dp 미만(40~47dp)도 사용자가 고른 값이면 그대로 쓴다(NFR-06은 기본 줄 높이 기준, TBD-16).
- 0.1의 `tutor_done`은 없앴다. 튜터를 마쳤는지는 동작을 바꾸지 않는다(안내 없음, FR-TUT-05).
- 시험: SS-01~SS-05.

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
| 현재 글자 크게 보기 켜기·끄기(0.11) | `show_current_char` | FR-DSP-08, FR-SET-02(이슈 #135). 셋 다 끄면 안내가 전혀 보이지 않음(실제 키보드·튜터) |
| 진동·소리 | `haptic`, `sound`(기본 진동 켬·소리 끔) | FR-SET-03 |
| 튜터 다시 실행 | `TutorActivity` 시작(튜터를 부르는 유일한 길. 첫 실행 자동 시작은 2.3) | FR-TUT-06, FR-SET-04 |
| 기준값 초기화 | `ThresholdStore.resetToDefault()` | FR-SET-04 |
| 그래피티 영역 비율 | `graffiti_letter_ratio`. 라디오 단추 셋: 1 : 1 / 2 : 1(기본) / 3 : 1 → 1/2 / 2/3 / 3/4 저장(6.1) | FR-SET-05(오너 결정 #108 A), GRAFFITI 6.3 |
| 줄 높이 | `row_height_dp`(40~64dp, 기본 48dp) | FR-SET-06, TBD-16 |
| 기준값 내보내기·가져오기 | 5절 | FR-TUT-07 |
| 입력기 켜기 안내 | 시스템 입력기 설정 화면 열기(`Settings.ACTION_INPUT_METHOD_SETTINGS`) | FR-CMN-01 |

- 안내 표시 항목(방향 안내·다음 글자·현재 글자)은 늘 보인다(0.23, 이슈 #251: 공개판의 숨김 없음). 줄 높이는 40~64dp 범위에서 고른다(FR-SET-06, DCR-14 결정). 설정 화면에는 튜터·기준값 상태 안내를 두지 않는다(FR-TUT-05).
- 시험: 실기기 DV-11.
