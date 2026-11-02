# LLD-06 안내 표시

- 대응 HLD 절: 6장(안내 표시)
- 버전 **0.24** (2026-11-02)
- 0.24(FSS#36, SRS 0.72 FR-DSP-01·04, SWIPE 0.52 9.1, HLD 0.65 6장): 획 안내의 **"다음 획 자리" 표시를 없앴다** — 가운데 자리의 조합 글자(LLD-04 9.5)가 대신한다. 이 표시는 0.18부터 그리지 않았으므로(아래 0.18) 코드는 바뀌지 않는다. 획 안내(↓·→와 결과)·현재 글자(모음만, FR-DSP-03)는 그대로다.
- 0.23(FSS#17: 튜터 → 입력 연습, SRS 0.68·HLD 0.61·SWIPE 0.48): 4절 — 연습 자판이 실제 긋기 뷰라 연습 전용 안내(`SwipePracticeGuide`)를 지웠다.
- 0.22(이슈 #251 오너 결정: 공개판·개인판을 하나로(빌드 하나, 긋기 엔진은 모든 빌드)): 안내는 모든 빌드에 있다. 아래 "personal"은 이 판부터 지운다.
- 0.21(이슈 #179, SRS 0.44 FR-CMN-08 ①/②): `GuideFlags.bigCurrentOn`은 비밀번호 입력창에서만 끈다(학습 금지 칸은 켬, 2절). 자음 키 획 안내의 미리보기는 매퍼의 C(LLD-04 6.1·9.3)를 쓴다.
- 0.18(이슈 #173, SWIPE 9.1·9.2): 한글 자음 키는 **획 안내**다: ↓·→ 점과 긴 획(`ProbeKind.DOT`·`LONG`, LLD-04 5.2)의 결과(조립기 미리보기: 이어질 모음 또는 ㆍ)를 보이고, 이어질 수 없는 획은 보이지 않는다. 현재 글자 = 이 획을 이었을 때 보일 모음(또는 ㆍ·‥), 반대 방향이면 빈 글자. 숫자·기호·천지인 키는 그대로. 튜터 연습 자판의 안내는 입력 표시의 미리보기를 받는다(`SwipePracticeGuide(preview)`, `PracticeLayouts.guide(..., preview)`). 다음 획 시작 자리 표시(SRS FR-DSP-01 "다음 획을 시작할 자리")는 이 판에서 하지 않았다(코드 후속, README 6.19. 0.24에서 요구 자체가 없어짐, FSS#36).
- 0.17(이슈 #168, SRS FR-DSP-01·03, SWIPE 9장): 안내는 **모든 한글 자음 키**(쌍자음 키 포함)에서 띄운다(기준 키 조건 없앰). 안내 글자는 다시 **그 키의 음절**이다(ㄱ 키: 가 거 고 구 / 그 기, ㄲ 키: 까 …, LLD-04 5.2). 실제 자판 `guided = true`(LLD-04 9.3), 연습 자판 `SwipePracticeGuide`의 `anchorKeyId` 인자를 지웠다(4절).
- 0.16(이슈 #160, SRS FR-SWP-65, SWIPE 9.1): 천지인 키는 기준 키와 상관없이 안내한다(실제 자판 `guided`, LLD-04 9.3 / 연습 자판 4절). 안내 글자는 탐침이 내는 그대로: 누를 때 위·아래 ㅣ, 오른쪽 ㅡ, 현재 글자 = 이번 획의 요소, 나간 뒤 다음 글자 없음(LLD-04 4.7).
- 0.15(이슈 #156): 나눠 쓰기가 없어져 현재 글자 설명의 예를 바꿨다(3절 표). 안내 동작은 그대로다.
- 0.14(이슈 #150, PM 결정): 한글 자음 키의 안내 글자는 **낱모음**(ㅏ ㅓ ㅗ ㅜ / ㅡ ㅣ, ㅏ 다음 ㅐ·ㅑ·ㅡ, LLD-04 5.2)이고, 한글 자음 키는 누른 키가 **기준 키**일 때만 안내를 띄운다(FR-DSP-01, HLD 6장). 실제 키보드는 `SwipeTouchLogic`이 `onDown`을 부르지 않고(LLD-04 9.3 `guided`), 튜터 연습 자판은 `SwipePracticeGuide`가 `anchorKeyId()`로 거른다(4절). 숫자·기호 키는 그대로.
- 0.12(이슈 #142 오너 결정): 누르고 있기(FR-SWP-35)를 없애며 타이머 ②(HOLD)·`onHoldTimer`·`onHold`·`holdCapable`와 쌍자음 안내를 지웠다. 타이머는 표시 지연 ① 하나다.
- 입력 데이터 `ProgressState`·`Candidate`는 [LLD-04-swipe.md#5](LLD-04-swipe.md#5-progressstate-gesture-engine)에 있다.
- 요구: FR-DSP-01~08, FR-CMN-08, FR-TUT-11, SWIPE 9장·10장. (FR-SWP-24 머물기 "새 글자"는 삭제됨, 이슈 #127)

## 1. 구성

| 클래스 | 소스 세트 | 안드로이드 | 책임 |
|---|---|---|---|
| `GuideTimerLogic` | app(`kbd.app.swipe.impl`) | 없음 | 표시 지연 타이머의 판단(언제 무엇을 보일지) |
| `GuideOverlay` | app(`kbd.app.swipe.impl`) | 있음 | `PopupWindow`와 그리기, 타이머 등록(`Handler`) |

- 안내는 판정에 관여하지 않는다(HLD 1장 "표시와 판정 분리"). `GuideOverlay`는 엔진 상태를 읽기만 한다.

## 2. GuideTimerLogic

```kotlin
data class GuideFlags(val directionOn: Boolean, val nextCharOn: Boolean, val bigCurrentOn: Boolean) {
    companion object { fun of(s: Settings, field: FieldInfo?): GuideFlags }   // field null = 입력창 없음(0.23: 연습 자판은 연습 호스트의 보통 입력창을 넘김)
}
enum class GuidePhase { IDLE, PRESSED_WAITING, SHOWING }
data class GuideView(val showDirection: Boolean, val showCurrent: Boolean, val showNext: Boolean)   // 그릴 것
class GuideTimerLogic(private val th: Thresholds, private val flags: GuideFlags, private val moveStartDp: Float) {
    fun onDown(t: Long): List<TimerRequest>                         // 등록할 타이머
    fun onMove(movedDp: Float, t: Long): GuideView?
    fun onTimer(kind: TimerKind, t: Long): GuideView?
    fun onUp(): GuideView                                           // 모두 숨김
}
enum class TimerKind { SHOW_DELAY }                                 // HOLD는 0.12에서 삭제
data class TimerRequest(val kind: TimerKind, val atMs: Long)
```

| 함수 | 처리 | 근거 |
|---|---|---|
| `onDown(t)` | phase = PRESSED_WAITING. `[SHOW_DELAY @ t + guideDelayMs]` 하나를 돌려준다(누르고 있기 타이머 ②는 0.12에서 삭제) | HLD 6장 타이머 ① |
| `onMove(d, t)` | `d ≥ MOVE_START_DP`이면 표시 시작(phase = SHOWING, 표시 지연 취소) | FR-DSP-01 "움직이기 시작하면" |
| `onTimer(SHOW_DELAY)` | phase가 PRESSED_WAITING이면 SHOWING | FR-DSP-01 |
| `onUp()` | phase = IDLE, 모든 표시 끔, 타이머 취소. 표시 지연 전에 움직이지 않고 떼면 한 번도 SHOWING이 되지 않으므로 아무것도 뜨지 않는다 | FR-DSP-07 |

- `MOVE_START_DP` = 플랫폼 터치 슬롭(`ViewConfiguration.scaledTouchSlop`)을 dp로 바꾼 값(D-17). 테스트에서는 생성자로 넣는다.
- `GuideView` 계산: `showDirection = SHOWING && flags.directionOn`, `showNext = SHOWING && flags.nextCharOn`, `showCurrent = SHOWING && flags.bigCurrentOn`.
- `GuideFlags.of(s, field)`(0.11, 이슈 #135): `directionOn = s.showDirectionGuide`, `nextCharOn = s.showNextChar`, `bigCurrentOn = s.showCurrentChar && field?.hidesDisplay != true`(0.21). 세 안내 모두 설정으로 끈다(FR-DSP-08, FR-SET-02). **비밀번호 입력창만** 설정과 상관없이 현재 글자 크게 보이기를 끈다(FR-CMN-08 ①). 학습 금지 입력창은 평소처럼 보인다(②, 오너 2026-11-01 "학습 금지이지 암호처럼 출력 금지가 아니잖아!"). 튜터 연습 자판은 입력창이 없어 `field = null`(설정만 따름, FR-TUT-11). 셋 다 꺼지면 `GuideView`가 늘 모두 false라 창을 띄우지도 그리지도 않는다.
- 머물기 타이머와 "새 글자" 표시는 없앴다(이슈 #127, FR-SWP-24 삭제). 쌍자음은 둘째 탭을 뗄 때 정해지므로 미리 알리는 표시가 없다(SWIPE 9.2, 이슈 #150).
- 안내를 띄울 키인지는 이 클래스 밖에서 정한다: 안내를 띄우지 않을 획(기능키·빈 칸)에는 `onDown`·`onProgress`가 오지 않으므로 phase는 IDLE 그대로다(0.17: 자음 키는 모두 띄움).
- 시험: GT-01~GT-04, GT-06~GT-08, GT-10(GT-05·GT-09는 0.12에서 삭제).

## 3. GuideOverlay

```kotlin
class GuideOverlay(private val anchor: View, private val logic: GuideTimerLogic, private val handler: Handler) {
    fun onDown(key: SwipeKeyDef, keyRectPx: Rect, origin: StrokePoint)   // origin = 누른 점(dp), t = origin.t (0.5, 이슈 #83)
    fun onProgress(ps: ProgressState, finger: StrokePoint)
    fun onUp()
    fun dismiss()
}
```

| 항목 | 설계 | 근거 |
|---|---|---|
| 창 | `PopupWindow(contentView = GuideCanvasView)`, `isTouchable = false`, `isFocusable = false`, `isClippingEnabled = false`, `inputMethodMode = INPUT_METHOD_NOT_NEEDED`. 입력 뷰를 기준으로 `showAtLocation`, 크기 = 입력 뷰 폭 × (입력 뷰 높이 + 위로 띄울 높이 2 × 줄 높이) | HLD 6장, FR-DSP-06 |
| 위치 | 손가락에 가리지 않게 키 위쪽. 맨 윗줄 키는 입력 뷰 위(앱 화면 쪽)까지 그린다 | SWIPE 9.1, R3 |
| 방향 안내(`showDirection`) | `e1 == null`일 때의 후보를 손가락 위치에서 `dir`로 `offsetDp`만큼 떨어진 곳에 그린다: RING = 키 변 바로 밖, OUTER = 변에서 `longDp`, 숫자 키 RUB(↑/↓)·기호 키 STAGE2 = OUTER와 같은 자리, STAGE3 = 그 바깥(변에서 2 × `longDp`) (LLD-04 5.2, D-37) | FR-DSP-01, 02, SWIPE 9.1 |
| 현재 글자(`showCurrent`) | `ps.current`를 키 위쪽에 크게(키 높이의 0.8배, D-15). 이번 획 하나의 판정 결과(그 키의 음절, 0.17)이고 조합 결과는 보이지 않는다(각 다음 ㄱ 키 → 획 중 "가", 뗀 뒤 입력창은 "각가", LLD-04 5.2, DCR-16 결정) | FR-DSP-03 |
| 다음 글자(`showNext`) | `e1 != null`이면 `ps.candidates`를 손가락 위치에서 `dir`로 `offsetDp`만큼 떨어진 곳에 작게. `dir`은 손가락이 다음에 움직일 방향이다(RUB는 손가락이 밖이면 되돌아오는 쪽, 안이면 e1 쪽, LLD-04 5.2) | FR-DSP-04, SWIPE 9.2 |
| 쓸 수 없는 방향 | 후보가 생기지 않으므로 그리지 않음(바깥 변은 탐침 결과가 바뀌지 않음) | HLD 6장 |
| 다시 그리기 | `ps`와 `GuideView`가 직전과 같으면 `invalidate`하지 않음 | HLD 6장 "바뀔 때만" |

#### `onDown(key, rect, origin)`
- 앞 획의 타이머를 취소하고(`removeCallbacksAndMessages(token)`), `logic.onDown(origin.t)`의 요청마다 `handler.postAtTime(…, token, atMs)`(`atMs`는 `MotionEvent.eventTime`과 같은 uptime 시계). 키 사각형과 누른 점 `origin`(dp)을 저장하고, 보이던 내용·직전 `ps`를 지운다.
- 긋기 뷰는 이 호출 바로 뒤에 누른 상태의 `ProgressState`로 `onProgress(ps, origin)`를 한 번 부른다(LLD-04 9.3). 그래서 움직이지 않고 누르고 있다가 표시 지연이 지나 SHOWING이 되어도 현재 글자·방향 안내를 그린다(FR-DSP-01, 03).

#### `onProgress(ps, finger)`
- `v = logic.onMove(hypot(finger − origin), finger.t)`(거리 dp). 방향 안내는 `ps.beforeExit`일 때, 다음 글자는 그 뒤에 그린다.
- `ps`를 저장하고, 바뀐 것이 있으면 다시 그린다. 타이머(`onTimer`)로 SHOWING이 되면 저장한 `ps`로 그린다.

#### 생성과 다시 만들기 (0.5, 이슈 #83)
- `GuideTimerLogic`의 `flags`(2절)와 기준값은 생성자에서 정해진다. 긋기 뷰가 overlay를 가지며, **누를 때마다** `GuideFlags.of(지금 설정, 입력창)`(2절)으로 직전 것과 다르면 이전 overlay를 `dismiss()`하고 새로 만든다. 같으면 그대로 쓴다.
- 기준값은 긋기 뷰를 만들 때 정해진다. 뷰는 `onStartInputView`마다 `showModeView`로 새로 만들고 그때 바뀐 기준값을 다시 읽으므로(LLD-08 3.1) overlay를 기준값 때문에 따로 다시 만들지 않는다.

#### `onUp()`, `dismiss()`
- 타이머 취소(`handler.removeCallbacksAndMessages(token)`), 내용 지움. `dismiss`는 창도 닫는다(`onFinishInputView`, `onWindowHidden`).
- `ACTION_CANCEL`에도 긋기 뷰가 `onUp()`을 불러 안내를 닫는다(HLD 4.1, FR-CMN-11a ③, LLD-04 9.3).

- 성능: 그리기는 `onDraw`에서 미리 만든 `Paint`만 쓴다(할당 없음). NFR-01은 실기기 DV-02로 잰다.
- 시험: 실기기 DV-01~DV-04, 단위 GT-*(2절).

## 4. 연습 자판의 안내 (0.23, FSS#17)

FR-TUT-11, HLD 7.1: 입력 연습의 긋기 연습 자판은 **실제 `SwipeKeyboardView`**다(LLD-07 2.3). 그래서 안내도 이 문서 2·3절 그대로 같은 `GuideOverlay`·`GuideTimerLogic`이 같은 시점에 띄운다. `GuideFlags.of(settings, field)`의 `field`는 연습 호스트의 보통 글자 입력창(비밀번호·학습 금지 아님)이라 설정 세 스위치만 안내를 끈다. 0.22까지의 연습 전용 안내(`PracticeGuide`, `SwipePracticeGuide`, `PracticeLayoutSource.practiceGuide`)와 시험 PG-01~PG-07은 지웠다.
