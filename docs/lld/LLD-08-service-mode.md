# LLD-08 입력기 생명주기·플랫폼 처리·모드 키

- 대응 HLD 절: 8장(입력기 생명주기와 플랫폼 처리), 8.1(ModeController), 5A의 창 높이·바닥 정렬·처리 시점
- 버전 **0.33** (2026-11-14)
- 0.33(FSS#95, SRS 0.85 FR-CMN-02·03 ②·03f·10·10a, HLD 0.78 8장·8.1: **긋기 입력 모드 삭제, 모드 키 순환 순서 한글 두벌식 → 영문 쿼티 → 그래피티**): ① `InputMode { DUBEOLSIK, QWERTY, GRAFFITI }`(선언 순서 = 순환 순서, LLD-03 3.5). 저장이 없을 때 시작 모드 = `available[0]` = 한글 두벌식(5절). `ModeController`의 코드는 그대로이고 순서만 바뀐다. ② **옮겨 오기:** 저장된 마지막 모드 이름이 지금 없는 이름(`"SWIPE"`)이면 `SettingsStore`가 없음으로 읽어 `ModeController(…, saved = null, …)`이 된다(5절, LLD-07 6.1, 시험 MC-40~MC-42). ③ `KeyboardService`는 `SwipeHost`가 아니고 `SwipeModule`을 쓰지 않는다. `showModeView`는 탭 자판 뷰 아니면 그래피티 패드다(3절). ④ `InputDispatcher`에서 `swipeState`·`onJamoEvents`·`onSwipeText`·`centerText`·`closeSwipe`를 지웠다. `resetParsers`는 그래피티만 비우고, ⌫는 그래피티 아니면 조합기다(6.1). ⑤ `ComposingController.show(r, g)`의 `replacesLast`(긋기 낱모음 ‥)를 지웠다(4절). ⑥ 7절: 모드 뷰는 탭 자판(6줄)·그래피티(5줄) 둘. 탭 자판·그래피티·모드 키·좌우 여유 칸·자판 판·띄운 자판의 동작은 그대로다. 아래 0.32까지의 이력에 나오는 긋기 자판·`Swipe…`·SWIPE n.n은 이 판에서 없어졌다.
- 0.32(FSS#86, SRS 0.84 FR-CMN-15·1.3·TBD-33, HLD 0.77 5A "띄운 자판"·8장·R12): **띄운 자판**(새 7.4) — 순수 함수 `FloatingKeyboard.place`·`drag`·`ratios`(이동 영역 S, 자판 판 = 손잡이 띠 + 자판 사각형, 비율 fx·fy, 들어가지 않으면 붙은 자판), 순수 상태 기계 `PlateMover`(HLD ⓐ~ⓚ), `KeyboardRootView`의 `portraitFloat`·`landscapeFloat`·손잡이 띠 뷰 `MoveHandle`·`movePlate`·이동 영역의 위 t(상태 막대·컷아웃, 붙은 동안은 큰 값만), `KeyboardInsets`의 띄운 자판 인셋(내용·보이는 인셋 = 입력 뷰 아래 끝, 늘 `REGION`), `FeedbackPort.grab()`(8절), 서비스의 `saveFloatPosition`·설정 흐름의 띄우기 변경(3절). 플랫폼 동작 ⑨~⑭(문서 근거·미확인). 띄우기 끔이면 0.31과 모든 길이 같다.
- 0.31(FSS#85, SRS 0.83 FR-CMN-14 ⑤⑫·3.3, HLD 0.76 5A·8장·R11): **자판 판과 투명한 자리**(새 7.3) — 순수 함수 `KeyboardPlate.rect`(입력 뷰 좌표의 사각형 `PxRect`), `KeyboardRootView`는 배경을 두지 않고 `plate`만 칠한다(`onLayout`에서 정함, `PlatePainter`), `KeyboardService.onComputeInsets`는 `KeyboardInsets.compute`로 줄인 자판에서 `TOUCHABLE_INSETS_REGION` + `touchableRegion` = 자판 판(창 좌표), 꽉 채움·첫 배치 전에는 `TOUCHABLE_INSETS_VISIBLE` 그대로. 내용·보이는 인셋은 그대로. 플랫폼 동작의 근거와 미확인 항목 표. 3절 `onComputeInsets` 행(코드와 달랐던 옛 값도 함께 고침), 7절 여백 색·`onTouchEvent`, 7.2 `onLayout`·"자판 사각형을 아는 곳"·"여백·여유 칸 터치" 행. `OffKeyboardPointers`·`KeyboardWidth.place`·판정은 그대로.
- 0.30(FSS#84, SRS 0.82 FR-CMN-14 ④⑧⑪·FR-SET-09, HLD 0.75 5A): **자판 좌우 여유 칸**(7.2) — `KeyboardWidth.place`가 자판 사각형(11칸)과 여유 칸 폭을 낸다(`KeyboardSpan(left, width, margin)`, `RECT_UNITS` = 11, `MIN_DP` = 352, `fullKeyDp` = 가용 폭 / 11 올림, 픽셀 반올림 규칙). `KeyboardRootView`는 모드 뷰·덮개·버튼을 자판(`keysLeft`·`keysWidth`)에 놓고 여유 칸은 자식 없는 제 자리로 둔다. 모드 뷰는 `SideMarginView.sideMarginPx`로 여유 칸 폭을 받아 키 기하의 테두리 왼쪽·오른쪽을 (−m, W + m)으로 준다(`KeyGeom.build`의 `sideDp`). `OffKeyboardPointers`·`EdgeCrossingTracker`는 그대로.
- 0.29(FSS#25, SRS 0.71 FR-CMN-14·FR-SET-09, HLD 0.64 5A): **자판 폭과 정렬**(새 7.2) — `KeyboardWidth.place`(키 한 칸 폭 × 10, 가용 폭에서 멈춤, 하한 320dp, 왼손/중앙/오른손), `KeyboardRootView`의 `portraitWidth`·`landscapeWidth`, `onMeasure`·`onLayout`(모드 뷰·덮개·버튼을 줄인 자판에), 자식 밖에서 내려온 포인터를 거르는 `OffKeyboardPointers`, 다시 놓기(화면 방향·설정 흐름).
- 0.28(FSS#36, SWIPE 0.52 9.5, SRS 0.72 FR-DSP-10): `InputDispatcher.centerText()`(가운데 글자, 6.1). `KeyboardService.centerText = dispatcher.centerText()`(3절 표).
- 0.27(FSS#31, SRS 0.69 FR-SET-08·FR-CMN-03f, HLD 0.62 8.1·8장): **켜진 모드.** `ModeController`에 켜진 모드 집합 `enabled`(영문 쿼티·한글 두벌식은 늘 켬), `nextEnabled`(`nextInOrder` 대신), `applyEnabled`(이전 모드 비움, 돌아갈 모드·지금 모드를 다음 켜진 모드로, 저장할 모드를 돌려줌), `fixedStart`(시작 때 고친 모드)(5절). 서비스: 생성 때 켜진 모드를 넘기고 `fixedStart`를 저장, `onStartInputView`에서 `applyEnabled`(3.1), 설정 `flow`가 바뀌면 `applyEnabledModes` — 키보드가 떠 있으면 곧바로 확정하고 새 모드의 뷰로 바꿈(3.2).
- 0.26(FSS#17: 튜터 → 입력 연습, SRS 0.68·HLD 0.61·SWIPE 0.48): 기준값은 코드 상수(`thresholds = ThresholdDefaults.VALUES`), `ThresholdStore` 읽기와 `onStartInputView`의 다시 읽기 삭제.
- 0.25(이슈 #251 오너 결정: 공개판·개인판을 하나로(빌드 하나, 긋기 엔진은 모든 빌드)): `KeyboardService`는 `ModeController`에 모든 모드를 넘긴다(긋기를 빼는 공개판 순서 없음, 5절). `createSwipeView`는 늘 뷰를 준다.
- 0.24(이슈 #189, SRS 0.45 FR-GRF-09·09a, HLD 0.41 5장·5A): **그래피티 획 안내표의 버튼과 덮개는 `KeyboardRootView`가 가진다**(새 7.1). 빈 띠 오른쪽 끝 1줄 × 1줄 버튼(누를 때 피드백·토글), 패드 + 보조 줄 위 덮개(`GraffitiGuidePanel`, LLD-05 10절), 열 때 모드 뷰의 진행 중 터치 취소와 그 제스처의 나머지 가로채기. `setModeView`는 모드 뷰만 바꾼다(버튼·덮개는 남음). 서비스: `showModeView`에서 그래피티면 `enableGuide`, 아니면 `disableGuide`, `onWindowHidden`·`onFinishInputView(false)`에서 `closeGuide`(3절). 창 높이·인셋은 그대로.
- 0.23(이슈 #205, 오너 결정 2026-11-02): **모드 키 ↑ = 설정**(네 모드 모두). 뷰가 모드 키를 뗄 때 판정해(LLD-05A 4절, LLD-04 9.3, LLD-05 8절) 위로 나갔으면 `onSettingsStroke()`: `finishAll()`(조합 중이면 먼저 확정) → `resetParsers(MODE_CHANGED)`(버린 획 = 진동) → `onOpenSettings()`. 입력 아님, 모드 그대로. 그 밖(탭·다른 방향)은 지금처럼 `onFunctionDown(MODE)` = `onModeKey()`, 다만 누를 때가 아니라 뗄 때 온다. 긋기 ⚙ 키(`FunctionKey.SETTINGS`)와 `onSettingsKey()`의 조합 중 한 글자 완성(0.18, 0.21)은 지웠다(6.1).
- 0.22(이슈 #198): 탭 자판 편집 줄의 Esc·Del. `EditorKey.ESCAPE`(`KEYCODE_ESCAPE`)·`FORWARD_DEL`(`KEYCODE_FORWARD_DEL`), `ComposingController.editKey(k)`(4절), `onFunctionDown(ESC/DELETE)`는 방향키와 같이 확정 → `resetParsers()` → 키 보냄, 입력 아님(6.1).
- 0.21(이슈 #179·#180 후속, SRS 0.44 FR-CMN-08 ①/②, SWIPE 0.35 9.5): `FieldInfo.restricted` → `hidesDisplay`(비밀번호만, 표시 끔)·`noRecord`(비밀번호·학습 금지, 기록 안 함)(2절). `errorLog.enabled = !noRecord`(3.1). **조합 중 설정 키 = 한 글자 완성**(설정 화면 안 엶, 6.1). `SwipeHost.centerConsonant`(2.2).
- 0.18(이슈 #173): 긋기 **설정 키**(⚙, `FunctionKey.SETTINGS`): 누를 때 조합 확정·조립 닫기(모드 전환과 같음) 뒤 키보드 설정 화면을 연다(`onOpenSettings`, 서비스가 `SettingsActivity`를 새 작업으로 시작). 입력 아님. `onJamoEvents(ev, errorVibe)`, 스페이스·엔터·탭·모드·설정 키의 조립 닫기에서 모음이 못 된 획을 버리면 오류 진동(D-57: 커서 이동·입력창 바뀜은 진동 없음). `backspace()`는 `StrokePop`으로 바뀜(6.1).
- 0.17(이슈 #168): 긋기 자판이 6줄(쌍자음 줄)이 되어 창을 채운다(`setModeView` 긋기 6줄, 위 빈 띠 없음, 7절). `backspace()`에서 `swipeState.onBackspace`(⌫ 뒤 기준 키)를 지웠다(6.1). `resetParsers()`는 긋기 매퍼의 천지인 조립기만 비운다. 무시된 긋기(기준 키 밖)는 없다(6.2).
- 0.16(이슈 #160): 긋기 천지인 키. `onJamoEvents`는 매퍼의 임시 글자(ㆍ·‥)를 조합 글자 뒤에 보이고(빈 이벤트 목록도 보임), `backspace()`는 긋기면 매퍼의 조립기에 먼저 묻는다(6.1). 천지인 키 요소는 입력이다(6.2).
- 0.14(이슈 #150): `BlockParser` → `SwipeInputMapper`(`SwipeEngineState`). `SwipeHost.hasJong` 삭제. `backspace()`는 조합기 다음 `swipeState.onBackspace(composer.lastConsonant())`로 ⌫ 뒤 기준 키를 정하고 편집기를 마지막에 쓴다(FR-SWP-39). 기준 키 밖 긋기의 오류 진동은 뷰가 `feedback.error()`로 낸다(8절 진동 설정을 따름).
- `InputMode`·`FunctionKey`는 [LLD-03-modules.md#35-모드와-기능키](LLD-03-modules.md#35-모드와-기능키), 조합기는 [LLD-04-stroke-hangul.md#7](LLD-04-stroke-hangul.md#7-hangulcomposer-core-hangul)에 있다.
- 요구: FR-CMN-01~12(03f·04·04b·11a 포함), FR-CMN-14, FR-SET-08, FR-TAP-04, 10, 11a, FR-CMN-09, FR-GRF-08, NFR-05.

## 1. 범위

| 절 | 구성 요소 | 안드로이드 |
|---|---|---|
| 2 | `FieldInfo` (입력창 판별, 숫자류 입력창 판별) | 없음 |
| 3 | `KeyboardService` (생명주기) | 있음 |
| 4 | `ComposingController`, `EditorPort`, `AndroidEditorPort` | 앞 둘은 없음 |
| 5 | `ModeController` (모드 키, 숫자류 입력창 진입·이탈) | 없음 |
| 6 | `InputDispatcher` (뷰 출력 → 조합기·편집기·모드) | 없음 |
| 7 | `KeyboardRootView` (창 높이 고정·바닥 정렬·빈 띠) | 있음 |
| 8 | `Feedback` | 있음 |
| 9 | 시퀀스: 모드 키, 입력창 시작, 숫자류 입력창 진입·이탈, 커서 이동 | – |

## 2. FieldInfo

```kotlin
enum class FieldClass { TEXT, NUMBER, PHONE, DATETIME, OTHER }
data class FieldInfo(val fieldClass: FieldClass, val isPassword: Boolean, val noLearning: Boolean,
                     val multiLine: Boolean, val enterAction: Int?) {     // enterAction null = 동작 없음
    companion object { fun from(inputType: Int, imeOptions: Int): FieldInfo }
    val hidesDisplay get() = isPassword                     // 0.21 FR-CMN-08 ①: 입력 글자를 드러내는 표시(현재 글자, 가운데 자음, 그래피티 인식 글자)를 끔
    val noRecord get() = isPassword || noLearning           // 0.21 FR-CMN-08 ①②: 학습·통계·오류 기록 안 함. 학습 금지 칸은 표시는 평소대로(오너 2026-11-01)
    val numericLike get() = fieldClass in setOf(NUMBER, PHONE, DATETIME) || isPassword   // FR-CMN-04 "숫자류 입력창"
}
```

#### `FieldInfo.from(inputType, imeOptions)` — HLD 8장 표의 판별을 그대로 옮긴다
```
cls = inputType and TYPE_MASK_CLASS (0x0F)
varn = inputType and TYPE_MASK_VARIATION (0xFF0)
fieldClass = 1→TEXT, 2→NUMBER, 3→PHONE, 4→DATETIME, 그 밖→OTHER
isPassword = (cls == TEXT  && varn ∈ {0x80 PASSWORD, 0x90 VISIBLE_PASSWORD, 0xE0 WEB_PASSWORD})
          || (cls == NUMBER && varn == 0x10 NUMBER_VARIATION_PASSWORD)          // 클래스와 변형을 함께(URI 0x10과 구분)
noLearning = imeOptions and IME_FLAG_NO_PERSONALIZED_LEARNING (0x1000000) != 0
multiLine = cls == TEXT && (inputType and TYPE_TEXT_FLAG_MULTI_LINE (0x20000)) != 0
action = imeOptions and IME_MASK_ACTION (0xFF)
enterAction = if (imeOptions and IME_FLAG_NO_ENTER_ACTION (0x40000000)) != 0 || action ∈ {0 UNSPECIFIED, 1 NONE} then null else action
```
- `numericLike`(숫자류 입력창) = 클래스가 숫자·전화·날짜시간이거나 **비밀번호 변형**(`isPassword`)이다. 학습 금지 플래그(`noLearning`)만 있는 칸은 숫자류가 아니다(일반 칸). `restricted`(표시 제한)와 범위가 다르다(SRS FR-CMN-04·04b, HLD 8장 자판 선택, PM 확인 2026-10-27 PR #4).
- 상수 값은 안드로이드 `InputType`·`EditorInfo`의 공개 값이다. 시험에서 같은 값을 쓴다.
- 근거: FR-CMN-04, 04b, 08, 09, HLD 8장. 시험: FI-01~FI-12.

## 3. KeyboardService

```kotlin
class KeyboardService : InputMethodService() {
    lateinit var dispatcher: InputDispatcher
    lateinit var modes: ModeController
    lateinit var root: KeyboardRootView
}
```

| 함수 | 처리 | 근거 |
|---|---|---|
| `onCreate()` | `settings = SettingsStore.loadBlocking()`, `thresholds = ThresholdDefaults.VALUES`(0.26, 고정 상수, LLD-07 3절), `ErrorLog` 준비. `modes = ModeController(InputMode.entries.toList(), settings.lastMode, settings.enabledModes)`(0.27: 켜진 모드. 0.33: `lastMode`가 null이면 — 처음 설치했거나 저장된 이름이 지금 없는 모드 — 한글 두벌식으로 시작, FR-CMN-10·10a). `modes.fixedStart`가 있으면 마지막 모드로 저장(0.27, FR-CMN-03f ③). `dispatcher = InputDispatcher(…, saveLastMode)`. 설정 `flow`를 모아 바뀌면 `applySettings()`(끝에 3.2 `applyEnabledModes`) | FR-CMN-10, 03f, HLD 7.3 |
| `onCreateInputView()` | `root = KeyboardRootView(…)`(줄 높이·두 방향 폭, 0.32: 두 방향 띄우기, `moveFeedback = feedback`, `onFloatMoved = ::saveFloatPosition`, 그리고 `relayout()`). `showModeView(modes.current)` | FR-CMN-10, FR-CMN-15 (0.32) |
| `onStartInputView(info, restarting)` | 아래 3.1 | FR-CMN-03e, 04, 04b, 08 |
| `onUpdateSelection(oldS, oldE, newS, newE, candS, candE)` | `r = composing.onUpdateSelection(newS, newE, candS, candE)`. `USER_MOVED` → `dispatcher.onCursorMoved(newS, newE, candS, candE)`. `OTHER_MOVED` → `dispatcher.onCursorMovedNoComposing()` | FR-CMN-06, HLD 8장, R4 |
| `onFinishInputView(finishing)` | `dispatcher.finishAll()`, `guide?.dismiss()`, 반복 타이머 중지. `finishing == false`(키보드가 내려감)면 `root.closeGuide()`. `true`(입력창만 바뀜, 이어서 `onStartInputView`)면 획 안내표는 열린 채(0.24) | FR-CMN-06, FR-GRF-09 |
| `onFinishInput()` | `dispatcher.finishAll()` | HLD 8장 |
| `onWindowHidden()` | 진행 중 터치·타이머 버림(`root.cancelTouches()` — 0.32: 띄운 자판의 옮기기·기다림도 취소, 7.4), `root.closeGuide()`(0.24) | D-27, FR-GRF-09, FR-CMN-15 ⑩ |
| `onEvaluateFullscreenMode()` | `false` | FR-CMN-12 |
| `onConfigurationChanged(c)` | `super`, `root.relayout()`(같은 배치를 그 방향의 자판 폭·정렬로, 줄 높이 유지, 7.2. 0.32: 그 방향의 띄우기·자리로, 옮기기 중이면 먼저 취소, 7.4) | FR-CMN-12, FR-CMN-14 ⑩, FR-CMN-15 ⑩⑬ |
| `onComputeInsets(out)` | `super` 뒤, 루트가 창에 붙어 있으면 `KeyboardInsets.compute(root, out)`(7.3): `contentTopInsets = visibleTopInsets` = 입력 뷰 위 끝(창 좌표), 터치를 받는 자리 = **자판 판**(줄인 자판: `TOUCHABLE_INSETS_REGION` + `touchableRegion`, 꽉 채움: `TOUCHABLE_INSETS_VISIBLE` = 입력 뷰 전체). 빈 띠는 자판 판 안이라 터치를 받는다. 0.32: 띄운 자판이면 내용·보이는 인셋 = 입력 뷰 아래 끝, 터치를 받는 자리 = 띄운 자판의 자판 판(늘 `REGION`, 7.4) | SRS 3.3 빈 띠, FR-CMN-14 ⑫ (0.31), FR-CMN-15 ⑫ (0.32) |

### 3.1 `onStartInputView(info, restarting)`
```
f = FieldInfo.from(info.inputType, info.imeOptions)
dispatcher.field = f
dispatcher.finishAll()                     // 조합 확정(HLD 8장)
dispatcher.resetParsers()                  // 그래피티 조립기·일회성 상태(HLD 8장, 5장)
modes.onFieldChanged()                     // typed = false, previous 유지(FR-CMN-03e)
before = modes.current
if f.numericLike: modes.enterNumericField()   // 영문 쿼티로 시작, 돌아갈 모드 기억(5절, FR-CMN-04·04b)
else:             modes.leaveNumericField()   // 돌아갈 모드가 있으면 그 모드로(5절)
modes.applyEnabled(settings.enabledModes)?.let(saveLastMode)   // 0.27: 꺼진 모드를 고침(FR-CMN-03f ②~④). 확정은 위에서 함
errorLog.enabled = !f.noRecord             // NFR-05, FR-CMN-08 ①② (0.21)
composing.onStart(info.initialSelStart, info.initialSelEnd)
showModeView(modes.current, f)
if modes.current == GRAFFITI && before != GRAFFITI: dispatcher.graffiti?.enterMode()   // → GraffitiState.enterMode()(LLD-05 6절, G6)   // 그래피티에 새로 들어옴(HLD 5장 초기화, FR-GRF-05a)
```
- 같은 숫자류 입력창에서 키보드가 다시 떠도(`onStartInputView` 다시 호출) 이 순서를 그대로 탄다. 그래서 다시 영문 쿼티로 시작하고 돌아갈 모드는 그대로다(FR-CMN-04b).
- 자동으로 바뀌는 모드(쿼티로 시작, 돌아가기)는 `previous`와 마지막 모드 저장을 바꾸지 않는다(5절, FR-CMN-04b).

- 0.27: `applyEnabled`는 enter·leave 다음이다. 그래서 숫자류 입력창 안이면 `current`(= QWERTY, 늘 켬)는 그대로이고 돌아갈 모드만 고쳐지며, 문자 입력창으로 돌아온 뒤 돌아갈 모드가 꺼져 있었으면 그 다음 켜진 모드가 된다(사례 표 8행).

### 3.2 `applyEnabledModes(on)` (0.27, FSS#31)
설정 `flow`의 값이 올 때마다(`applySettings`의 끝, 메인 스레드) 부른다. `on = settings.enabledModes`.
```
before = modes.current
if before ∉ on && isInputViewShown:  dispatcher.finishAll(); dispatcher.resetParsers()   // 바꾸기 전에 확정(FR-CMN-03d)
modes.applyEnabled(on)?.let(saveLastMode)
if modes.current == before || !isInputViewShown: return      // 떠 있지 않으면 다음 onStartInputView의 showModeView가 보임
showModeView(modes.current, dispatcher.field)                   // 모드 키와 같은 길(뷰 교체)
if modes.current == GRAFFITI: dispatcher.graffiti?.enterMode()   // (꺼진 모드의 다음 켜진 모드는 그래피티가 될 수 없지만 같은 규칙을 둠)
```
- `resetParsers()`는 기본 이유(`FIELD_CHANGED`)다: 입력창이 바뀐 것이라 그래피티 조립기·일회성 상태만 비운다(LLD-05 6절).
- 자동 전환이라 `typed`·`previous`는 바꾸지 않는다(5절). 저장은 `applyEnabled`가 돌려준 모드만 한다(숫자류 입력창 안이면 고친 돌아갈 모드, 밖이면 새 모드).
- `Settings.enabledModes` = {DUBEOLSIK, QWERTY} ∪ (`graffitiEnabled`면 GRAFFITI)(LLD-07 6.1).

#### `showModeView(mode, f)`
| 조건 | 보이는 뷰 |
|---|---|
| `mode == DUBEOLSIK` / `QWERTY` | `TapKeyboardView(TapLayouts.DUBEOLSIK / QWERTY)` |
| `mode == GRAFFITI` | `GraffitiPadView` |

- 숫자류 입력창이라고 따로 고르는 뷰는 없다. `ModeController`가 이미 `current = QWERTY`로 바꿨으므로 쿼티 자판이 보이고, 사용자가 모드 키로 바꾸면 그 모드의 뷰가 보인다(FR-CMN-04b "모드 키는 평소처럼").
- 뷰를 넣기 전에 `mode == GRAFFITI`면 `root.enableGuide(templates, feedback)`, 아니면 `root.disableGuide()`(획 안내표 닫힘·버튼 숨김, 0.24, 7.1). `templates`는 `createGraffiti()`가 읽은 `TemplateSet`(인식기와 같은 것).
- 뷰는 `root.setModeView(view, rowsOfView)`로 넣는다(7절). 조합기 정책: DUBEOLSIK → `DUBEOLSIK`, GRAFFITI → `GRAFFITI`, QWERTY → 조합기 없음(글자는 바로 확정).
- `restarting`은 따로 다루지 않는다(HLD 8장에 구분이 없음, D-28).

## 4. ComposingController와 EditorPort

```kotlin
interface EditorPort {                     // 안드로이드 없는 경계. AndroidEditorPort가 InputConnection으로 구현
    fun commitText(text: String); fun setComposingText(text: String); fun finishComposingText()
    fun deleteBefore()                       // KEYCODE_DEL 내림·올림
    fun sendKey(key: EditorKey)              // DPAD_LEFT/RIGHT/UP/DOWN, ENTER, ESCAPE, FORWARD_DEL(0.22)
    fun performEditorAction(actionId: Int)
    fun setSelection(start: Int, end: Int)
    fun beginBatch(); fun endBatch()
}
enum class SelectionVerdict { OWN, USER_MOVED, OTHER_MOVED }
class ComposingController(private val port: EditorPort) {
    fun show(r: ComposeResult, pendingGlyph: String = "")   // 0.33: replacesLast(긋기 낱모음 ‥) 삭제
    fun finishComposing()
    fun finishAfterUserMove(newS: Int, newE: Int, candS: Int, candE: Int)
    fun commitText(text: String)
    fun deleteBefore(); fun arrow(d: Dir); fun tab(); fun enter(f: FieldInfo)
    fun onStart(selStart: Int, selEnd: Int)
    fun onUpdateSelection(newS: Int, newE: Int, candS: Int, candE: Int): SelectionVerdict
    val composingShown: String
}
```

`AndroidEditorPort`는 부를 때마다 `service.currentInputConnection`을 얻고 null이면 아무것도 하지 않는다(HLD 9장). `InputConnection` 호출의 반환값(`false` = 실패)은 쓰지 않으며 예외로 바꾸지 않는다(LLD-09 2절 D-32).

- `ComposingController`는 포트 호출이 돌아온 뒤에만 자기 상태(`composingShown`·`glyph`·`composeStart`·기대 커서)를 바꾼다(호출 하나 단위). 그래서 포트 호출이 예외를 내면 돌아온 호출까지의 상태가 남는다. `EngineGuard` 스냅숏에는 들어가지 않는다(LLD-09 2절, 이슈 #75).
- 포트 호출이 던져도(0.6, PR #91): ① `beginBatch`로 연 배치는 `endBatch`로 항상 닫는다(`try`/`finally`, `show`·`finishAfterUserMove`). ② 임시 글자를 빼는 `setComposingText(composingShown − glyph)`가 돌아오면 그 자리에서 `composingShown`·`glyph`에서도 임시 글자를 뺀다(`finishComposing`·`finishAfterUserMove`). 그래서 뒤이은 `finishComposingText`가 던져도 `composingShown`은 편집기 조합 구간과 같다(임시 글자 없음). 예외는 그대로 호출자(`EngineGuard.stroke`)에게 간다.

| 함수 | 처리 | 근거 |
|---|---|---|
| `show(r, g)` | `beginBatch`. `r.committed`가 있으면 `commitText(r.committed)`(조합 구간을 바꿔 확정). `d = r.composing + g`(g = 그래피티 천지인의 임시 글자 ㆍ·‥, SRS FR-HGL-04e). `bare = r.composing`(임시 글자를 뺀 조합 글자). 임시 글자를 뺄 때(`finishComposing`의 `dropGlyph`, `finishAfterUserMove`)는 `bare`로 되돌리고, 커서 보정은 `composingShown.length − bare.length`. `d`가 있으면 `setComposingText(d)`, 없고 `composingShown`이 있었으면 `setComposingText("")`(조합 구간 지움). `endBatch`(던져도 항상). 배치 안 호출이 모두 돌아오면 `composingShown = d`, `glyph = g`(던지면 그 앞 값 그대로). 기대 커서는 돌아온 쓰기마다 큐에 넣음(4.1) | FR-CMN-05, HLD 5장 임시 글자 |
| `finishComposing()` | `composingShown`이 비면 아무것도 안 함. `glyph`가 있으면 먼저 `setComposingText(composingShown − glyph)`(임시 글자 뺌, 기대 커서 큐에 넣음)하고, 돌아오면 `composingShown`·`glyph`에서도 임시 글자를 뺌. 그다음 `finishComposingText()`(배치 없음). `composingShown = ""`, `glyph = ""`, `composeStart = −1` | FR-CMN-03d, 06, FR-HGL-04e |
| `finishAfterUserMove(newS, newE, candS, candE)` | 사용자가 커서를 옮긴 뒤의 확정. `glyph`가 없으면 `finishComposingText()`만. 있으면 `beginBatch`; `setComposingText(composingShown − glyph)`(이때 커서가 조합 끝으로 감. 돌아오면 `composingShown`·`glyph`에서도 임시 글자를 뺌); `finishComposingText()`; 사용자 위치를 되돌림: 조합 구간 끝 `spanEnd` = `candE`(−1이 아니면), 아니면 마지막으로 아는 끝 `composeStart + composingShown.length`(`composeStart`가 있으면), 둘 다 없으면 −1. 각 값 `x`에 대해 `spanEnd ≠ −1`이고 `x ≥ spanEnd`면 `x − glyph.length`, 아니면 `x`로 `setSelection(newS', newE')`; `endBatch`(던져도 항상). 끝을 모르면 옮기지 않는다 — 사용자가 고른 위치를 지킨다(FR-CMN-06, G13, D-36). 상태 비움은 `finishComposing`과 같고, `predictedSel = newE'`(보정한 사용자 위치), `expected.clear()` | FR-CMN-06, FR-HGL-04e |
| `commitText(s)` | (호출자가 먼저 확정) `commitText(s)`. 기대 커서 큐에 넣음 | FR-TAP-11a |
| `deleteBefore()` | `port.deleteBefore()`. 확정 글자 한 글자를 지우고, 입력창에 선택 영역이 있으면 편집기 기본 동작대로 선택한 부분을 지운다(`KEYCODE_DEL`이 그렇게 동작함, SRS FR-CMN-07, HLD 8장, DCR-13 결정). 결과 커서를 알 수 없으므로 `predictedSel = −1` | FR-CMN-07 |
| `arrow(d)` | (호출자가 먼저 확정) `sendKey(DPAD_d)`. `predictedSel = −1` | FR-TAP-10 |
| `editKey(k)`(0.22) | (호출자가 먼저 확정) `sendKey(k)`(`ESCAPE`, 또는 `FORWARD_DEL` = 커서 오른쪽 한 글자·선택 영역은 편집기 기본 동작). `predictedSel = −1` | 이슈 #198 |
| `tab()` | (호출자가 먼저 확정) `commitText("\t")` | FR-TAP-04 |
| `enter(f)` | (호출자가 먼저 확정) `f.enterAction != null` → `performEditorAction(it)`, 아니고 `f.multiLine` → `commitText("\n")`, 그 밖(동작 없는 한 줄 입력창) → `sendKey(ENTER)`(`KEYCODE_ENTER`, SRS FR-CMN-09, HLD 8장, DCR-13 결정). 키 보냄·동작이면 `predictedSel = −1` | FR-CMN-09, HLD 8장 |
| `onStart(selStart, selEnd)` | `predictedSel = selEnd`(−1이면 모름), `composeStart = −1`, `expected.clear()`, `composingShown = ""`, `glyph = ""` | HLD 8장 |

### 4.1 커서 이동 판정 `onUpdateSelection`

상태: `predictedSel`(우리가 마지막으로 쓴 뒤 기대하는 커서, 모르면 −1), `composeStart`(조합 구간 시작, 없으면 −1), `expected: ArrayDeque<Int>`(아직 확인 안 된 우리 쓰기의 기대 커서들).

- 우리 쓰기의 기대 커서: `setComposingText(d)` → 조합 구간이 없었으면 `composeStart = predictedSel`, 기대값 `composeStart + d.length`. `commitText(t)` → `(composeStart 있으면 그 값, 없으면 predictedSel) + t.length` 뒤 `composeStart = −1`. 각 값을 `expected` 끝에 넣고 `predictedSel`에 저장한다. `predictedSel == −1`(모름)이면 계산하지 않고 넣지 않는다(`composeStart`도 −1로 둔다).
- `composeStart`는 조합 구간이 끝날 때마다 −1로 돌린다: `finishComposing`·`finishAfterUserMove`·`onStart`·`commitText`, 그리고 `show`가 조합 구간을 비웠을 때(`d == ""`). 그래서 확정 뒤 새 조합은 그때의 `predictedSel`에서 시작한다. USER_MOVED 판정은 `composeStart`를 지우지 않는다 — 조합 구간은 호출자가 곧 부르는 `finishAfterUserMove`까지 편집기에 그대로 있고, 그 함수가 `candE == −1`일 때 마지막으로 아는 구간 끝을 쓰기 때문이다(G13).
- **다시 맞춤(resync):** `predictedSel == −1`(방향키·⌫·키 보낸 Enter 뒤)인 채 조합을 시작하면 `composeStart`도 −1로 남는다. 그 조합의 첫 갱신 알림이 조합 구간 끝(`newE == candE`)이라 OWN으로 판정할 때, `composeStart`가 −1이면 `composeStart = candS`로 맞춘다. 맞추지 않으면 다음 조합 갱신의 기대값이 `predictedSel`(구간 끝)에서 계산돼 한 칸 밀리고, 자기 갱신을 USER_MOVED로 오판해 조합이 끊긴다(FR-CMN-06 위반, G12).
- `predictedSel == −1`인 동안 첫 갱신 알림은 아래 규칙에 따라 OWN 또는 OTHER_MOVED로 판정하고 `predictedSel = newE`로 다시 맞춘다.

```
collapsed = newS == newE
if collapsed && newE in expected: expected에서 그 값까지 버림; predictedSel 유지; return OWN
if composingShown != "":
    if !collapsed || candE == -1 || newE != candE: → USER_MOVED
    if expected.isEmpty() && (predictedSel == -1 || newE == predictedSel):
        predictedSel = newE; if composeStart == -1: composeStart = candS   // 다시 맞춤(G12)
        return OWN
    → USER_MOVED                                     // 조합 구간 끝이지만 기대 위치와 다름(HLD 8장)
else: predictedSel = newE; expected.clear(); return OTHER_MOVED (newE != 직전 predictedSel일 때), 같으면 OWN
USER_MOVED 일 때: predictedSel = newE; expected.clear()   // composeStart는 finishAfterUserMove가 지움(G13)
```
- HLD 8장 조건("조합 구간 끝과 다르거나 마지막으로 기대한 위치와 다를 때만")을 따르되, 갱신 알림이 쓰기보다 늦게 여러 번 올 수 있어 "기대한 위치"를 하나가 아니라 확인 안 된 기대값 목록으로 둔다(D-29). 실기기 R4(DV-06)로 확인한다.
- 시험: CC-01~CC-22(다시 맞춤 CC-18, 구간 보고 없는 사용자 이동 CC-19, 포트 예외 CC-20~CC-22).

## 5. ModeController

```kotlin
class ModeController(private val available: List<InputMode>, saved: InputMode?, enabled: Set<InputMode> = available.toSet()) {
    var enabled: Set<InputMode>; private set           // 0.27: 켜진 모드(정규화, 아래)
    var current: InputMode; var previous: InputMode? = null; var typed = false
    var returnMode: InputMode? = null                  // 숫자류 입력창에 들어가기 직전의 모드(돌아갈 모드). null = 숫자류 입력창 밖
    val inNumericField: Boolean get() = returnMode != null
    val fixedStart: InputMode?                         // 0.27: 저장된 모드가 꺼져 있어 고친 시작 모드(저장할 값). 아니면 null
    fun onInput()
    fun onFieldChanged()
    fun onModeKey(): InputMode
    fun enterNumericField()
    fun leaveNumericField()
    fun applyEnabled(modes: Set<InputMode>): InputMode?   // 0.27: 저장할 마지막 모드 또는 null
    fun nextEnabled(m: InputMode): InputMode
    companion object { val ALWAYS_ON = setOf(DUBEOLSIK, QWERTY) }
}
```

| 함수 | 처리 | 근거 |
|---|---|---|
| 생성 | `enabled = 정규화(enabled)`. `current = saved`가 `available`에 있으면 그 값(꺼져 있으면 `nextEnabled(saved)`), 없으면 `available[0]`(= DUBEOLSIK, 0.33. 늘 켜진 모드라 `nextEnabled`로 넘어갈 일이 없다). `saved`가 null인 경우는 둘이다: 처음 설치했을 때와, 저장된 이름이 지금 없는 모드일 때(옛 `"SWIPE"`를 `SettingsStore`가 없음으로 읽음, FR-CMN-10a ①, LLD-07 6.1). `fixedStart = saved != null && saved != current`면 `current`, 아니면 null(저장된 모드가 없을 때 고친 첫 모드는 저장하지 않음). `previous = null`, `typed = false`, `returnMode = null`(프로세스 시작) | FR-CMN-10, 10a, 03b, 03e, 03f ③, HLD 8.1 |
| `onInput()` | `typed = true` | FR-CMN-03a |
| `onFieldChanged()` | `typed = false`(previous·returnMode 유지) | FR-CMN-03e |
| `onModeKey()` | HLD 8.1 그대로: `next = if (typed && previous != null && previous != current) previous else nextEnabled(current)`; `if (typed) previous = current`; `current = next`; `typed = false`; `return next`. 숫자류 입력창 안에서도 같다(`returnMode`는 바꾸지 않음). `previous`는 늘 켜진 모드다(`applyEnabled`가 꺼진 것을 비움) | FR-CMN-03, 03c, 03f ①②, 04b |
| `enterNumericField()` | `if (returnMode == null) returnMode = current`; `current = QWERTY`. `previous`·`typed`는 바꾸지 않는다 | FR-CMN-04, 04b, HLD 8.1 |
| `leaveNumericField()` | `if (returnMode != null) { current = returnMode; returnMode = null }`. `previous`·`typed`는 바꾸지 않는다. 숫자류 입력창 밖에서 부르면 아무것도 하지 않는다 | FR-CMN-04b, HLD 8.1 |
| `applyEnabled(modes)` (0.27) | `enabled = 정규화(modes)`; `if (previous ∉ enabled) previous = null`; `if (returnMode != null && returnMode ∉ enabled) { returnMode = nextEnabled(returnMode); 저장 = returnMode }`; `if (current ∉ enabled) { current = nextEnabled(current); if (returnMode == null) 저장 = current }`. `typed`는 바꾸지 않는다. 돌려주는 값 = 저장(없으면 null). 확정·뷰 교체는 서비스(3.1, 3.2) | FR-CMN-03f ②~④, HLD 8.1 |
| `nextEnabled(m)` (0.27, `nextInOrder` 대신) | `available`에서 m 다음부터 돌며(끝이면 처음) 처음 만나는 `enabled`의 모드(DUBEOLSIK → QWERTY → GRAFFITI → DUBEOLSIK → …, 꺼진 모드 건너뜀. 그래피티가 꺼지면 DUBEOLSIK ↔ QWERTY). m 말고 켜진 것이 없으면 m | FR-CMN-03, 03b, 03f ① |
| 정규화 (0.27) | `available` 가운데 `modes`에 있거나 `ALWAYS_ON`(DUBEOLSIK·QWERTY)인 것. 비면 `available` 전부(연습 자판의 한 모드짜리 `ModeController(listOf(mode), mode)`가 그대로 동작) | FR-SET-08, FR-CMN-03f ⑤ |

- `QWERTY`는 늘 `available`에 있다. 그래서 `enterNumericField`는 늘 쿼티로 간다.
- **돌아갈 모드의 수명:** 숫자류 입력창에서 다른 숫자류 입력창으로 옮기면 `enterNumericField`가 다시 불려도 `returnMode`는 처음 값 그대로이고 `current`는 다시 QWERTY다(사례 표 4행). 같은 숫자류 입력창에서 키보드가 다시 떠도 같다(FR-CMN-04b). 숫자류 입력창이 아닌 입력창으로 옮길 때만 `returnMode`로 돌아가고 비운다(사례 3·5행).
- **마지막 모드 저장:** `inNumericField`인 동안 모드 키로 바꾼 모드는 저장하지 않는다(6.1 `onModeKey`). 자동 전환(enter·leave)도 저장하지 않는다. 그래서 저장된 마지막 모드는 숫자류 입력창 안에서는 늘 돌아갈 모드와 같고, 숫자류 입력창 안에서 프로세스가 다시 시작되면 생성 때 `current = 저장된 마지막 모드`가 되고 곧 이은 `onStartInputView`의 `enterNumericField`가 그 값을 `returnMode`로 삼아 쿼티로 시작한다(사례 7행, HLD 8.1). 옮겨 오기도 같다: 저장된 이름이 옛 `"SWIPE"`면 `current` = 한글 두벌식으로 시작하고, 숫자류 입력창이면 `enterNumericField`가 한글 두벌식을 `returnMode`로 삼아 쿼티로 시작한다(SRS 옮겨 오기 사례 2행, MC-41).
- `returnMode`는 저장하지 않는다(프로세스 시작 때 null, HLD 8.1).
- 확정·초기화·저장 같은 부수 효과는 `InputDispatcher.onModeKey()`와 `KeyboardService.onStartInputView`가 한다(6절, 3.1). 이 클래스는 상태 기계만 둔다(HLD 8.1 "순수 Kotlin 상태 기계").
- `onInput()`을 부르는 곳과 부르지 않는 곳은 6.2 표.
- **켜진 모드(0.27):** 켜진 모드 집합은 밖(`SettingsStore`)에서 받는다. 이 클래스는 저장하지 않고 저장할 값만 돌려준다(`applyEnabled`, `fixedStart`). 영문 쿼티·한글 두벌식은 끌 수 없으므로 순환에는 늘 둘 이상이 있고, `enterNumericField`의 QWERTY도 늘 켜져 있다.
- 시험: MC-01~MC-10(SRS 모드 키 예시 10행), MC-11~MC-13, NF-01~NF-09(SRS 숫자류 입력창 사례 7행 + 재표시·저장), OM-01~OM-07(SRS 꺼진 모드 사례 6행 + 정규화, 0.33), MC-40~MC-42(SRS 옮겨 오기 사례 4행, 0.33). 기대값은 SRS 3.1의 새 사례 표 넷을 따른다(LLD-11 7.3).

## 6. InputDispatcher

뷰가 내는 결과를 조합기·편집기·모드 상태에 반영한다. 안드로이드 없는 클래스이고 `TapSink`, 그래피티 `InputSink`, `EngineGuard`의 `GuardedState`(LLD-09 2절)를 구현한다.

```kotlin
class InputDispatcher(private val modes: ModeController, private val out: ComposingController,
                      private val feedback: FeedbackPort, private val saveMode: (InputMode) -> Unit) {
    var composer: HangulComposer?                     // 모드 정책(3절)
    var graffiti: GraffitiInputHandler?                // 그래피티 모드만
    var field: FieldInfo
}
```

### 6.1 함수

| 함수 | 처리 | 근거 |
|---|---|---|
| `onTapOutput(key, o)` | `o.jamo != null`(두벌식 글자) → `j = Jamo.ofCompat(o.jamo)`, `composer.apply(if j.kind == CONSONANT Consonant(j) else Vowel(j))` → `out.show`. `o.text != null` → `finishAll()` 뒤 `out.commitText(o.text)`(쿼티 글자도 이 길, FR-TAP-11a). `o.errorVibe` → `feedback.error()`. `markInput()` | FR-TAP-01a~03a, 11a |
| `onFunctionDown(BACKSPACE)` | `backspace()`; `markInput()` | FR-CMN-07 |
| `onFunctionDown(SPACE)` | `finishAll()`; `out.commitText(" ")`; `markInput()` | FR-CMN-03a |
| `onFunctionDown(ENTER)` | `finishAll()`; `out.enter(field)`; `markInput()` | FR-CMN-09 |
| `onFunctionDown(TAB)` | `finishAll()`; `out.tab()`; `markInput()` | FR-TAP-04 |
| `onFunctionDown(ARROW_d)` | `finishAll()`; `resetParsers()`; `out.arrow(d)` (입력 아님) | FR-TAP-10, FR-CMN-03a |
| `onFunctionDown(ESC)`, `onFunctionDown(DELETE)`(0.22) | 방향키와 같음: `finishAll()`; `resetParsers()`; `out.editKey(ESCAPE / FORWARD_DEL)` (입력 아님). Del 반복은 반복 한 번 = 이 호출 한 번(LLD-05A 6절) | 이슈 #198 |
| `onFunctionDown(MODE)` | `onModeKey()`. 0.23부터 뷰는 모드 키를 뗄 때(탭이면) 부른다 | FR-CMN-03 |
| `onSettingsStroke()`(0.23, `TapSink` 경유) | 모드 키 ↑: `finishAll()`(조합 중이면 확정, 공백 없음); `resetParsers(MODE_CHANGED)`; `onOpenSettings()`(서비스가 `SettingsActivity`를 새 작업으로 시작). 입력 아님(`markInput` 없음), 모드 바꾸지 않음. 0.18·0.21의 `onFunctionDown(SETTINGS)`·`onSettingsKey()`(조합 중 한 글자 완성만)는 삭제 | 이슈 #205 오너 결정 |
| `onFunctionUp(key)` | 입력 없음(뗌 알림. 반복 중지는 뷰가 함) | FR-CMN-11a |
| `backspace()` | 그래피티면 `graffiti.backspace()`(LLD-05 6절). 아니면 `b = composer?.backspace()`(입력 단위 하나); `Handled(r)` → `out.show(r)`, 그 밖 → `out.deleteBefore()`. 반복 타이머도 이 함수를 부른다 | FR-CMN-07, 07a, 07b, 07c |
| `onModeKey()` | `finishAll()`; `resetParsers()`; `next = modes.onModeKey()`; `if (!modes.inNumericField) saveMode(next)`(숫자류 입력창 안에서 바꾼 모드는 마지막 모드로 저장하지 않음, FR-CMN-04b·10, HLD 8.1); 서비스가 `showModeView(next)`. 그래피티로 새로 들어오면 `graffiti?.enterMode()`(→ `GraffitiState.enterMode()`, LLD-05 6절, G6) | FR-CMN-03d, 04b, HLD 8.1 |
| `onCursorMoved(newS, newE, candS, candE)` | `composer?.finish()` + `out.finishAfterUserMove(newS, newE, candS, candE)`(임시 글자가 있으면 빼고 확정, 사용자 커서 위치 유지, 4절); `resetParsers()` | FR-CMN-06, FR-HGL-04e |
| `onCursorMovedNoComposing()` | `resetParsers()` | FR-CMN-06, FR-HGL-04e |
| `finishAll()` | `graffiti?.finishAll()` 또는 `composer?.finish()` + `out.finishComposing()` | FR-CMN-03d |
| `resetParsers(reason)` | `graffiti?.reset(reason)`(그래피티 조립기·일회성 상태, LLD-05 6절). 그래피티가 아니면 비울 것이 없다(0.33: 긋기 조립과 그 오류 진동 D-57 삭제) | HLD 5장 초기화 |
| `markInput()` | `modes.onInput()` | FR-CMN-03a |
| `error()` (그래피티 `InputSink`) | `feedback.error()`(인식 실패 오류 진동) | FR-GRF-08 |
| `hidesDisplay` (그래피티 `InputSink`, 0.21) | `field.hidesDisplay`(비밀번호만) | FR-CMN-08 ①, FR-GRF-07 |
| `field` (그래피티 `InputSink`) | 지금 `field`(그래피티 Enter → `out.enter(field)`, G2) | FR-CMN-09 |
| `snapshot()` (`GuardedState`) | `Snapshot(composer?.snapshot(), graffiti?.snapshot())` — 엔진 상태만. `ComposingController`는 넣지 않는다(LLD-09 2절 스냅숏 범위, 이슈 #75) | HLD 9장, NFR-03 |
| `restore(s)` (`GuardedState`) | 스냅숏에 든 것만 각 객체의 `restore`로 되돌림(null 칸은 건너뜀). 편집기·`ComposingController`는 건드리지 않는다(D-32) | HLD 9장 |

- 숫자류 입력창에서도 입력 분류(6.2)와 `markInput()`은 같다(SRS FR-CMN-03a "숫자류 입력창(FR-CMN-04)에서도 이 구분은 같다"). 0.1의 `keypadActive`(숫자 자판 입력은 typed를 바꾸지 않음)는 숫자 키패드가 없어져 없앴다.

### 6.2 "입력" 분류 (FR-CMN-03a)

| 동작 | `markInput()` |
|---|---|
| 글자·숫자·기호(탭 자판, 그래피티 글자·숫자·기호 획) | 예 |
| 그래피티 천지인 요소 획(ㅣ·ㅡ·탭, 모음이 성립하기 전의 ㆍ·‥ 포함) | 예(SRS FR-CMN-03a, DCR-17 결정) |
| 스페이스, 엔터, 탭, ⌫(어느 모드든) | 예 |
| 방향키, 모드 키 | 아니오 |
| 그래피티 한영 전환·시프트·캡스록·확장 시프트·구두점 시프트 획 | 아니오 |
| 그래피티 조합 강제 확정('J') 획 | 아니오(SRS FR-CMN-03a, DCR-17 결정) |
| 그래피티 인식 실패 | 아니오(입력 없음, FR-GRF-08) |

- 시험: ID-01~ID-24.

## 7. KeyboardRootView (창 높이 고정·바닥 정렬)

```kotlin
class KeyboardRootView(context: Context) : FrameLayout(context) {
    var rowHeightDp: Float
    fun setModeView(v: View, rows: Int)
    fun relayout(); fun cancelTouches()
    // 획 안내표(0.24, 7.1)
    val guideOpen: Boolean
    fun enableGuide(templates: TemplateSet, feedback: FeedbackPort); fun disableGuide()
    fun openGuide(); fun closeGuide()
    // 띄운 자판(0.32, 7.4)
    var portraitFloat: FloatSetting; var landscapeFloat: FloatSetting
    var moveFeedback: FeedbackPort?; var onFloatMoved: ((Boolean, Float?, Float?) -> Unit)?
}
```

| 함수 | 처리 | 근거 |
|---|---|---|
| 시스템 막대 여백(0.7) | 창 인셋 `navigationBars() ∪ tappableElement()`(두 종류 중 **변마다 큰 값**)를 아래·왼쪽·오른쪽 padding으로 둔다(위는 0). `ViewCompat.setOnApplyWindowInsetsListener`로 받고 값이 바뀌면 padding을 고쳐 `requestLayout`, 붙을 때 `requestApplyInsets`. 바닥 = 시스템 막대 위. 가로 화면 3버튼 내비게이션처럼 막대가 옆이면 그 폭만큼 옆 여백이 생기고 자판은 남은 폭에 같은 배치로 놓인다 | SRS 3.3 "시스템 막대와의 관계", FR-CMN-12, HLD 5A, 오너 결정 #93 OD-1·OD-3 A |
| 여백 색(0.7, 0.31) | 루트는 배경을 두지 않고 **자판 판**(7.3)만 `BACKGROUND_COLOR`(키보드 배경색, 지금 검정)로 칠한다(0.30까지는 루트 배경). 모드 뷰도 같은 상수를 배경으로 쓴다(그래서 자판 판 안의 여백과 키 영역이 한 색으로 이어짐). 꽉 채움이면 자판 판 = 루트 전체라 0.30과 같은 모습이다 | 오너 결정 #93 OD-2 A, FR-CMN-14 ⑫ |
| `onMeasure` | 높이 = `6 × rowHeightDp`(쿼티·두벌식 6줄이 가장 높음, 기본 288dp) **+ 아래 padding**(시스템 막대 여백). 0.32: 띄운 자판이면 부모가 허용하는 가장 큰 높이(7.4). 폭 = 부모. 자식 폭 = 폭 − 왼쪽 padding − 오른쪽 padding(안내표 버튼처럼 폭이 정해진 자식은 그 폭), 자식 높이 = 그 자식 `layoutParams.height`. `rowHeightDp`는 설정값(40~64dp, LLD-07 6.1) | SRS 3.3, HLD 5A, FR-SET-06 |
| `setModeView(v, rows)` | 모드 뷰 자식을 바꾼다(앞 모드 뷰만 빼고, 새 뷰는 맨 아래 z 순서 = 버튼·덮개 밑. 0.24부터 버튼·덮개는 남음). 자식 높이 = `rows × rowHeightDp`, `gravity = BOTTOM`. 그래피티 5줄, 쿼티·두벌식 6줄 | HLD 5A |
| `onTouchEvent` | 자식 밖(위 빈 띠와 시스템 막대 여백)의 터치는 `true`를 돌려 소비하고 아무 입력도 하지 않는다(빈 띠는 키가 아니므로 키 입력 피드백도 없음, FR-CMN-11a ②). 0.31: 자판 판 밖(투명한 자리)에서 시작한 터치는 보통 창에 오지 않고, 오더라도 같은 길로 소비한다(7.3) | SRS 3.3 "빈 띠는 터치를 받되 입력은 없다" |
| 자식에게 주는 창 크기 | 바깥 변 계산(LLD-03 3.3)은 자식 좌표에서 창 왼쪽·오른쪽·아래가 바깥, 자식 위 끝은 바깥이 아님(빈 띠로 이어짐) | SRS 3.3 창 높이 규칙 |

- 획이 시작된 뒤 손가락이 빈 띠로 넘어가도 이벤트는 그 획을 시작한 자식 뷰가 계속 받는다(안드로이드 터치 대상 유지). 그래서 맨 윗줄 키의 ↑ 나감은 보통 변 통과다. 탭 자판은 6줄이라 빈 띠가 없고 숫자 줄 위는 창 밖(앱 화면)이다. 창 밖으로 나간 이동도 DOWN을 받은 뷰가 계속 받아야 한다(HLD 10장 R2, 에뮬레이터 확인 DV-26, LLD-11 8절). 빈 띠는 그래피티(5줄)에만 남는다.
- 모드 키는 모든 모드에서 맨 아랫줄 왼쪽이다(각 배치 데이터, SL-02·TL-03, 그래피티 보조 줄 LLD-05 8.1).
- 시험: 실기기 DV-07(높이·바닥 정렬·빈 띠 소비, 시스템 막대 여백 색·여백 터치 입력 없음 — 제스처·3버튼 내비게이션), DV-08(가로 화면, 옆 막대 여백). JVM 단위 시험은 없다(안드로이드 뷰, LLD-11 9절 G1).

### 7.1 그래피티 획 안내표 버튼·덮개 (0.24, 이슈 #189)

근거: SRS 0.45 FR-GRF-09·09a, FR-CMN-11a, HLD 0.41 5장 "소유·열기·닫기"·"배치와 터치", GRAFFITI 0.16 6.6-1~3·7. 덮개 그림·스크롤·✕는 LLD-05 10.2.

| 항목 | 처리 |
|---|---|
| 자식과 z 순서 | 아래부터 모드 뷰(`setModeView`가 index 0에 넣음) → 덮개 `GraffitiGuidePanel`(`MATCH_PARENT` × `rows × rowHeightDp`, `BOTTOM` = 모드 뷰와 같은 자리) → 버튼(`rowHeightDp` × `rowHeightDp`, `TOP|END` = 빈 띠 오른쪽 끝). 덮개·버튼은 `enableGuide`에서 한 번 만들고, 모드 뷰를 바꿔도 남는다. `relayout`이 세 자식의 크기를 새 줄 높이로 고친다 |
| `enableGuide(templates, feedback)` | 덮개(처음 한 번, `INVISIBLE`)와 버튼을 만들고 버튼을 `VISIBLE`로. 닫힌 덮개는 `INVISIBLE`이라 터치를 받지 않지만 배치는 되어 있어 연 즉시 제 크기다 |
| `disableGuide()` | `closeGuide()`, 버튼 `GONE`(그래피티를 떠남: 쿼티·두벌식, 숫자류 입력창의 쿼티 시작) |
| 버튼 터치 | 키다(빈 띠의 예외). `ACTION_DOWN`에서 `feedback.key()` 뒤 열려 있으면 `closeGuide()`, 아니면 `openGuide()`(누를 때 처리, FR-CMN-11a ①②). 그 뒤 이동·뗌은 아무 일 없음. 열린 동안 켜진 모양(밝은 배경·흰 "?"), 닫히면 흐린 모양. 접근성 이름 "획 안내표" |
| `openGuide()` | 버튼이 보일 때만. ① 모드 뷰에 합성 `ACTION_CANCEL`을 바로 보낸다(`GraffitiPadView`: 진행 중 획 버림 = 입력·오류 진동 없음, ⌫ 반복 멈춤, 떼지 않은 모드 키는 입력 없음, LLD-05 8.3 CANCEL). ② 덮개 `VISIBLE`. ③ `swallowGesture = true` |
| `closeGuide()` | 열려 있을 때만. 덮개 `INVISIBLE`, `rewind()`(맨 위, 따르는 포인터 없음), `swallowGesture = true`. 덮개를 빼지 않는다(✕를 뗄 때 덮개 자신의 터치 처리 안에서 불리므로) |
| `onInterceptTouchEvent` | `ACTION_DOWN`(새 제스처)이면 `swallowGesture = false`. `swallowGesture`면 `true`: 안내표를 열거나 닫은 **그 제스처의 나머지**는 안드로이드가 이미 잡은 자식들(모드 뷰·버튼·덮개)에 `ACTION_CANCEL`을 보내고 루트가 받아 버린다(`onTouchEvent` = 소비, 입력 없음). 그래서 열기 전에 패드를 누르고 있던 손가락이 덮개 아래 패드로 획을 마치지 못하고, 그 제스처 동안 새로 누른 손가락도 덮개·패드에 가지 않는다(모든 손가락을 뗀 뒤 다시 누르면 덮개가 받음) |
| 열린 동안 터치 | 덮개 자리(패드 + 보조 줄, 모드 키 포함)의 새 터치는 맨 위의 덮개가 받아 소비한다(LLD-05 10.2). 모드 뷰는 받지 않는다 → 패드 획·탭·보조 줄 키·모드 키(탭·↑ 설정) 모두 입력과 피드백이 없다(FR-GRF-09a). 빈 띠의 버튼만 그대로 받는다 |
| 상태 | 열림 = 덮개 `VISIBLE`. 뷰 상태일 뿐 `GraffitiState`·조합기·조립기에 아무것도 넘기지 않는다(HLD 5장). 새 루트(`onCreateInputView`, 화면 회전 등 설정 변경)는 닫힌 채 시작 |
| 닫히는 때 | 버튼, ✕(LLD-05 10.2), `disableGuide`(그래피티를 떠남), 서비스의 `onWindowHidden`·`onFinishInputView(false)`(키보드가 내려감, 3절). 입력창만 바뀌면(`onFinishInputView(true)` → `onStartInputView` → `showModeView(GRAFFITI)`: 새 패드 뷰가 덮개 밑에 들어감) 열린 채 |
| 창 크기 | 바꾸지 않는다(높이·인셋·`onComputeInsets` 그대로, 세로·가로 같음) |

- 시험: GD-01·GD-02·GD-06~GD-09(LLD-11 6.6, Robolectric). 실기기 DV-28(LLD-11 8절).

### 7.2 자판 폭과 정렬, 좌우 여유 칸 (0.29, FSS#25; 0.30, FSS#84)

근거: SRS 0.82 FR-CMN-14·FR-SET-09·3.3 창 규칙 ④·FR-SWP-03·45, HLD 0.75 5A "자판 폭과 정렬, 좌우 여유 칸". 설정값과 저장은 LLD-07 6.1·7절.

낱말(HLD 5A): **자판** = 키 10칸(폭 W). **여유 칸** = 자판 양쪽 바깥의 띠(폭 m씩). **자판 사각형** = 여유 칸 + 자판 + 여유 칸(폭 R = W + 2m). **남는 자리** = 가용 폭 A − R. 아래에서 px 값은 모두 정수다.

```kotlin
// kbd.app.store (안드로이드 없음)
enum class KeyboardAlign { LEFT, CENTER, RIGHT }                 // 왼손모드 · 중앙모드 · 오른손모드
data class WidthSetting(val keyDp: Float?, val align: KeyboardAlign)  // 키 한 칸 폭, null = 기본값, FULL = 꽉 채움
data class KeyboardSpan(val left: Int, val width: Int, val margin: Int) {   // 자판 사각형(가용 폭 왼쪽 끝에서 px)과 여유 칸 폭 (0.30)
    val keysLeft: Int   // = left + margin, 자판(모드 뷰) 왼쪽 끝
    val keysWidth: Int  // = width − 2 × margin, 자판 폭 W
}
object KeyboardWidth {
    const val UNITS = 10                                          // 배치 단위 360 = 키 10칸
    const val RECT_UNITS = 11                                     // 자판 사각형 = 10칸 + 반 칸 둘 (0.30)
    const val MIN_KEY_DP = 32f; const val MAX_KEY_DP = 160f; const val MIN_DP = 352f   // 자판 사각형 하한 (0.30, 0.29는 320)
    const val FULL = Float.POSITIVE_INFINITY                      // "꽉 채움" 표식(가로 슬라이더 위 끝)
    fun snap(k: Float): Float                                     // 정수 dp, 32~160 안(FULL은 그대로)
    fun fullKeyDp(availDp: Float): Int                            // 슬라이더 위 끝(설정 화면, LLD-07 7절)
    fun place(availPx: Int, portraitAvailPx: Int, density: Float,
              portrait: WidthSetting, landscape: WidthSetting, isLandscape: Boolean): KeyboardSpan
}
// KeyboardRootView
var portraitWidth: WidthSetting; var landscapeWidth: WidthSetting   // 바꾼 뒤 relayout()
internal val rectLeft: Int; internal val rectWidth: Int; internal val sideMargin: Int   // 자판 사각형과 여유 칸(px, 0.30). 남는 자리 = 그 밖
internal val keyboardLeft: Int; internal val keyboardWidth: Int                         // 자판(모드 뷰)의 왼쪽 끝과 폭
// kbd.app.view (0.30)
interface SideMarginView { var sideMarginPx: Int }              // 모드 뷰가 구현: 부모가 재기 전에 여유 칸 폭을 알림
internal fun View.applySideMargin(margin: Int)                   // 이 뷰와 그 안의 SideMarginView 모두에(연습 화면의 안내표 + 패드 묶음)
internal class OffKeyboardPointers(onKeyboard: (x: Float, y: Float) -> Boolean) { fun filter(ev): MotionEvent?; val active: Boolean }
```

| 항목 | 처리 |
|---|---|
| `KeyboardWidth.place` | `availPx ≤ 0`이면 (0, 0, 0). s = 그 방향의 설정. **자판 사각형**의 원하는 폭 = s.keyDp가 `FULL`이면 `availPx`, 숫자면 `round(keyDp × 11 × density)`, null이면 — 세로: `availPx`(꽉 채움), 가로: 세로 설정의 keyDp(숫자면 × 11, `FULL`이면 `portraitAvailPx`), 그것도 null이면 `portraitAvailPx`(세로 꽉 채움의 사각형 폭 = 짧은 변, 곧 u = A_v / 11). `width` R = `min(availPx, max(원하는 폭, round(352 × density)))` — 하한이 먼저, A에서 멈춤이 그 뒤(A_v가 352dp보다 작으면 가로 기본은 `min(A, 352dp)`). 남는 자리 rest = `availPx − R`, `left` = 0(LEFT) / `rest / 2`(CENTER, 정수 나눗셈) / rest(RIGHT) (FR-CMN-14 ②③④⑤). 0.29까지 저장된 keyDp는 **바꾸지 않고** 같은 식에 넣는다: 자판 폭은 같고 사각형이 한 칸 넓어지며 A를 넘으면 A에서 멈춘다(예: A 360dp·keyDp 36 → 396 > 360이라 R = 360, 꽉 채움과 같은 모양. A 411dp·keyDp 34 → R 374, W 340) |
| 픽셀 반올림(0.30) | `margin` m = `round(R / 22)`(px, R의 1/22 = 반 칸), 자판 폭 W = `R − 2m`. 여유 칸 폭을 먼저 정수로 정하고 자판이 나머지를 받으므로 **두 여유 칸의 폭이 늘 같고 m + W + m = R이 px까지 정확히 맞는다**. 반올림 몫(1px 미만 × 2)은 자판 쪽에 들어가 키 한 칸이 0.1px 안쪽으로만 달라진다(예: R 360px → m 16, W 328; R 1080px → m 49, W 982; R 711px → m 32, W 647). 꽉 채움이면 R = `availPx` 그대로다(R을 22의 배수로 줄이지 않는다 — 사각형이 A와 같아야 함, FR-CMN-14 ③) |
| `onMeasure` | 가용 폭 = 잰 폭 − 왼쪽·오른쪽 padding(시스템 막대, 7절). 방향 = `resources.configuration.orientation`(가로인가). `portraitAvailPx` = `min(displayMetrics.widthPixels, heightPixels)`(화면 짧은 변 = 세로 화면 가용 폭, FR-CMN-14 ③). `span = place(…, portraitWidth, landscapeWidth, 가로인가)`. 자식마다 재기 **전에** `applySideMargin(span.margin)`(모드 뷰가 `SideMarginView`면 여유 칸 폭을 받는다). `MATCH_PARENT` 폭 자식(모드 뷰·안내표 덮개)은 폭 `span.keysWidth`(W), 폭이 정해진 자식(안내표 버튼)은 그 폭. 높이는 7절 그대로 |
| `onLayout` | 0.26까지의 `FrameLayout` gravity 대신 직접 놓는다. x = 왼쪽 padding + `span.keysLeft`(= 옆 여백 + 사각형 왼쪽 + m). 안내표 버튼 = (x + W − 버튼 폭, 위 padding), 그 밖의 자식 = (x, 줄 바닥 − 자식 높이) — 줄 바닥 = 높이 − 아래 padding. `GONE` 자식은 건너뜀. **여유 칸에는 자식을 놓지 않는다**: 루트가 칠한 자판 판(`BACKGROUND_COLOR`, 7.3)이 보이고 따로 그리는 것이 없다. 놓기 전에 `plate`를 정한다(0.31, 7.3) |
| 자판 사각형을 아는 곳(0.30) | 루트의 `rectLeft`(= 왼쪽 padding + `span.left`)·`rectWidth`(R)·`sideMargin`(m). 남는 자리는 `[paddingLeft, rectLeft)`와 `[rectLeft + rectWidth, width − paddingRight)`다. 0.31(FSS#85): 남는 자리는 칠하지 않고 터치 영역에서 뺀다 — 같은 `span`에서 만든 자판 판(`plate`, 7.3) 하나를 칠하기와 터치 영역이 함께 쓴다 |
| 다시 놓기 | `relayout()`(7절)이 `requestLayout`. 서비스가 `onConfigurationChanged`(화면 방향)와 설정 흐름의 폭·정렬 변경(`applySettings`, 줄 높이와 같은 길)에서 부른다. `onCreateInputView`가 새 루트에 두 값을 넣는다. 모드·조합 상태는 건드리지 않는다(FR-CMN-14 ⑩) |
| 여백·여유 칸 터치 | 자식 밖(빈 띠·**여유 칸**·남는 자리·시스템 막대 여백)의 터치는 7절 `onTouchEvent` = 소비 그대로(0.31: 남는 자리와 그 아래 여백에서 시작한 터치는 터치 영역 밖이라 보통 창에 오지 않는다, 7.3. 오면 같은 길이다). 여유 칸은 모드 뷰 밖이라 `onKeyboard`가 false이고, 거기서 내려온 포인터는 아래 `OffKeyboardPointers`가 버린다 — 탭 자판의 "틈·빈칸은 가까운 키로"(`TapGeometry.indexAt`, LLD-05A 4절)는 모드 뷰가 받은 내려옴에만 쓰이므로 여유 칸으로 번지지 않는다(FR-CMN-14 ⑪ "가까운 끝 키로 치지 않는다"). 자판에서 내려온 포인터는 여유 칸·남는 자리로 나가도 안드로이드가 그 모드 뷰에 계속 보낸다(모드 뷰 좌표로 음수 또는 W 초과) |
| `dispatchTouchEvent` | `OffKeyboardPointers.filter`를 거친 이벤트만 `super`로 보낸다. 판정 `onKeyboard(x, y)` = 보이는(`VISIBLE`) 자식의 사각형 안. 결과가 null이면(자식 밖에서 내려온 포인터만의 이벤트) `true`(소비, 입력 없음). 돌려준 값 = `super`의 결과 또는 버린 포인터가 남아 있음 |
| `OffKeyboardPointers.filter` | `ACTION_DOWN`이면 버린 집합을 비우고, `ACTION_CANCEL`이면 비운 뒤 원래 이벤트(모든 자식이 멈춰야 함). `ACTION_DOWN`·`POINTER_DOWN`의 점이 자식 밖이면 그 포인터 id를 버린다. 버린 것이 없으면 원래 이벤트. 있으면: 버린 포인터의 내려옴·올라옴은 null, 그 밖은 버린 포인터를 뺀 새 이벤트 — 남은 포인터가 하나뿐인 내려옴은 `ACTION_DOWN`(그 때를 `downTime`으로), 남은 하나의 올라옴은 `ACTION_UP`, 그 밖은 새 색인의 `POINTER_DOWN`·`POINTER_UP`, 이동은 묶인 표본(history)까지 옮긴다(획이 history를 읽음). 올라온 포인터는 집합에서 뺀다. 새 이벤트는 부른 쪽이 `recycle` |
| 왜 거르나 | 안드로이드는 다른 손가락이 자식을 누르고 있으면 자식 밖에 내려온 새 손가락도 그 자식에 붙인다(탭 자판은 틈을 가장 가까운 키로 침 → 여백 입력). 또 첫 손가락이 자식 밖이면 그 제스처의 뒤 손가락은 자식에 가지 않는다(`ViewGroup`이 가로챔) — 여백에 손을 얹으면 자판이 막힌다. 거르면 둘 다 없다(SRS FR-CMN-14 ⑤ 둘째 손가락, HLD 5A). 그래피티 빈 띠도 같다 |
| 안내표 | 덮개는 모드 뷰와 같은 x·폭(W), 버튼은 자판 오른쪽 끝(x + W − 버튼 폭. 7.1 표 "자식과 z 순서"의 `END`는 이 x로 바뀜). 둘 다 여유 칸에 들어가지 않는다(FR-CMN-14 ⑥). 열기·닫기·삼키기는 7.1 그대로(삼키기 `onInterceptTouchEvent`는 거른 이벤트를 본다) |
| 판정 | 모드 뷰는 받은 폭 W로 기하를 만든다: 탭 자판 `TapGeometry.build(layout, widthDp, rowDp, sideDp)`, 그래피티 패드 `GraffitiPadGeometry`. `minCrossDp` = 8dp 그대로 |
| 테두리의 왼쪽·오른쪽(0.30, FR-CMN-14 ⑧) | `sideDp` = `sideMarginPx / density`. 모드 뷰가 `KeyGeom.build(id, rect, widthDp, heightDp, sideDp)`(LLD-04 2절)로 키 기하를 만든다 — 테두리의 왼쪽 = −`sideDp`, 오른쪽 = `widthDp + sideDp`(자판 사각형의 바깥 변), 위·아래는 그대로. 그러면 끝 키의 왼쪽·오른쪽 변은 `outerEdges`에 들지 않고, `EdgeCrossingTracker`(고치지 않음)가 다른 변처럼 변 너머 `minCrossDp`에서 나감으로 기록한다. `TapKeyboardView`: `sideMarginPx`가 바뀌면 크기가 같아도 기하를 다시 만든다(`rebuildGeometry`의 같은지 비교에 `sideDp`를 더함). `GraffitiPadView`: 키 기하를 쓰는 곳이 보조 줄 모드 키뿐이라 누를 때 그 값을 읽는다(패드·보조 줄 배치는 그대로 W 기준, LLD-05 8절). `KeyHitTester(keys, windowW, windowH)`는 창 크기를 판정에 쓰지 않으므로 그대로다. `sideDp` 기본값 0은 여유 칸을 모르는 부름(순수 함수 시험)에서 0.29와 같은 결과다 |
| 꽉 채움에서 닿는가 | 꽉 채움에서는 여유 칸의 바깥 변이 화면 끝이라 그 너머 좌표는 오지 않는다. m ≥ `minCrossDp`이면(A ≥ 176dp) 화면 끝에 닿기 전에 통과가 선다(360dp: m 약 16.4dp) |

- 시험: KW-01~KW-10(`KeyboardWidthTest`, JVM), SM-01~SM-05(`SideMarginStrokeTest`, JVM), KR-01~KR-11(`KeyboardRootViewTest`)·OP-01~OP-05(`OffKeyboardPointersTest`, Robolectric), SS-10·SS-11(LLD-11 7.5·7.7). 실기기 DV-30·DV-35(LLD-11 8절).

### 7.3 자판 판과 투명한 자리 (0.31, FSS#85)

근거: SRS 0.83 FR-CMN-14 ⑤·⑫·1.3 용어·3.3 창 규칙·TBD-32, HLD 0.76 5A "자판 판과 투명한 자리"·8장 `onComputeInsets`·10장 R11.

낱말(HLD 5A): **자판 판** = 키보드가 칠하고 터치를 받는 사각형 하나. **투명한 자리** = 입력 뷰에서 자판 판 밖(남는 자리와 그 아래 시스템 막대 여백, 자판 사각형이 닿지 않은 쪽의 옆 여백).

```kotlin
// kbd.app.store (안드로이드 없음)
data class PxRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {   // px, right·bottom은 밖의 첫 px
    val isEmpty: Boolean; fun contains(x: Int, y: Int): Boolean
    companion object { val EMPTY = PxRect(0, 0, 0, 0) }
}
object KeyboardPlate {
    fun rect(viewWidth: Int, viewHeight: Int, padLeft: Int, padRight: Int, span: KeyboardSpan): PxRect
}
// kbd.app.view
class KeyboardRootView { internal var plate: PxRect /* private set */; internal val plateFillsView: Boolean }
internal class PlatePainter { fun draw(canvas: Canvas, plate: PxRect) }   // 자판 판을 BACKGROUND_COLOR로 채움
internal interface PlateView { val plate: PxRect }                         // 연습 화면의 BoardFrame이 구현
// kbd.app.ime
internal object KeyboardInsets { fun compute(root: KeyboardRootView, out: InputMethodService.Insets) }
```

| 항목 | 처리 |
|---|---|
| `KeyboardPlate.rect` | 입력 뷰 좌표의 사각형(px). `viewWidth ≤ 0` 또는 `viewHeight ≤ 0`이면 `EMPTY`. 가용 폭 A = `viewWidth − padLeft − padRight`, x = `span.left`, R = `span.width`. **왼쪽** = x가 0 이하이면 0, 아니면 `padLeft + x`. **오른쪽** = `A − x − R`이 0 이하이면 `viewWidth`, 아니면 `padLeft + x + R`. **위** = 0, **아래** = `viewHeight`(키 영역 6줄 + 아래 padding — 아래 여백은 뷰 높이에 들어 있어 따로 받지 않는다). "닿음"은 `KeyboardWidth.place`가 낸 정수 px(놓고 칠하는 데 쓰는 바로 그 값)로 판정하고 다시 반올림하지 않는다. 그래서 중앙모드에서 A − R = 1px이면 `left = 0`(정수 나눗셈)이라 왼쪽만 닿고 오른쪽은 1px이 남는다. 꽉 채움(R = A)이면 양쪽이 다 닿아 뷰 전체다. `span`이 (0, 0, 0)(A ≤ 0)이어도 뷰 전체가 나온다(남는 자리가 없음) |
| 사각형 값은 한 곳에서 | 위·아래를 포함한 사각형을 만드는 곳은 `KeyboardPlate.rect` 하나다. 칠하기(`onDraw`)와 터치 영역(`KeyboardInsets`)은 `root.plate`의 `left·top·right·bottom`을 그대로 읽고 "위 = 0, 아래 = 뷰 높이"를 따로 쓰지 않는다. 자판 사각형의 세로 자리를 바꾸는 뒤 작업(FSS#86)은 이 함수의 위·아래만 바꾸면 된다 |
| `KeyboardRootView.plate` | `onLayout`에서 `KeyboardPlate.rect(폭, 높이, paddingLeft, paddingRight, span)`으로 정한다(자식을 놓는 것과 같은 `span`, 같은 배치). 값이 바뀌면 `invalidate()`. 첫 배치 전에는 `EMPTY`. `plateFillsView` = `plate`가 (0, 0, 폭, 높이)와 같음 |
| 칠하기 | 루트는 배경을 두지 않는다(0.30까지의 `setBackgroundColor` 삭제). `setWillNotDraw(false)`(배경 없는 `ViewGroup`은 기본으로 `onDraw`를 건너뜀) + `onDraw`에서 `PlatePainter.draw(canvas, plate)` — 사각형 하나를 `BACKGROUND_COLOR`로 채운다. 자식(모드 뷰·덮개·버튼)은 그 위에 그려지고 자판 폭 W 안이라 그대로다. 여유 칸·그래피티 빈 띠·자판 판 안의 시스템 막대 여백은 이 사각형의 색이 보이고, 그 밖에는 루트가 아무것도 그리지 않는다 |
| 터치 영역 | `KeyboardService.onComputeInsets` → `super` 뒤 루트가 창에 붙어 있으면 `KeyboardInsets.compute(root, out)`. `loc = root.getLocationInWindow()`. `contentTopInsets = visibleTopInsets = loc[1]`(0.30과 같음 — 앱 화면이 밀리는 양은 그대로). `plate`가 비었거나 `plateFillsView`이면 `touchableInsets = TOUCHABLE_INSETS_VISIBLE`, `touchableRegion.setEmpty()`. 아니면 `touchableInsets = TOUCHABLE_INSETS_REGION`, `touchableRegion.set(loc[0] + plate.left, loc[1] + plate.top, loc[0] + plate.right, loc[1] + plate.bottom)`(창 좌표) |
| 꽉 채움에서는 `VISIBLE`을 그대로(결정) | 꽉 채움과 첫 배치 전에는 0.30이 알리던 값(`TOUCHABLE_INSETS_VISIBLE`)을 그대로 알린다. 받는 자리가 0.30과 "같은 값"이 아니라 **같은 보고**라서 실기 없이도 꽉 채움 회귀가 없다. 줄인 자판에서만 `REGION`을 쓴다. 두 길을 오갈 때 앞 값이 남지 않도록 `VISIBLE` 쪽에서 영역을 비운다(KI-04). 늘 `REGION`을 쓰는 것은 창 높이가 입력 뷰보다 커지는 작업(FSS#86)에서 정한다 — 0.32: 띄운 자판만 늘 `REGION`, 붙은 자판은 이 행 그대로(7.4) |
| 다시 알리는 시점 | 플랫폼은 창을 순회할 때마다(배치가 끝난 뒤) 입력기에 `onComputeInsets`를 다시 묻고, 값이 앞과 다르면 창 관리자에 알린다(아래 "플랫폼 동작" ③). 자판 판을 바꾸는 일은 모두 루트의 `requestLayout`을 거친다: 폭·정렬·줄 높이 설정(`relayout()`), 화면 방향(`onConfigurationChanged` → `relayout()`, 또는 플랫폼이 입력 뷰를 새로 만들면 새 루트의 첫 배치), 시스템 막대 여백(`setPadding`), 창 폭. 그래서 같은 순회에서 `onLayout`(새 `plate`) → `onDraw`(새 자리) → `onComputeInsets`(새 영역) 순서로 함께 새 값이 되고, 따로 갱신을 요청하는 코드는 두지 않는다. 모드 전환(`setModeView`)·덮개 여닫기는 `span`을 바꾸지 않아 `plate`가 그대로다(PL-07) |
| 터치 길 | 자판 판 안의 자식 밖 자리(여유 칸·빈 띠·판 안의 시스템 막대 여백)는 7절·7.2 그대로 루트가 소비한다. 투명한 자리에서 시작한 터치는 창에 오지 않는 것이 보통이다. 창에 오는 경우(아래 "플랫폼 동작" ⑤·⑥)에도 자식 밖에서 내려온 포인터라 `OffKeyboardPointers`가 자식에 넘기지 않고 루트가 소비한다 — 입력·피드백 없음(코드 그대로, PL-08) |
| 판에서 시작한 획이 판 밖으로 | 뷰 쪽: 자판에서 내려온 포인터는 남는 자리 좌표로 나가도 안드로이드가 그 모드 뷰에 계속 보낸다(7.2 "여백·여유 칸 터치", PL-08: 끝 키에서 남는 자리까지 그은 획이 꽉 채움과 같은 결과). 창 쪽: 터치 영역은 누를 때 어느 창이 받을지를 고르는 데 쓰이고, 이어지는 이동은 누른 창이 계속 받는 것으로 본다(창 위로 나간 획과 같은 성질, HLD R2·DV-26). **실기 미확인(HLD R11 ⑦, DV-36 ⑦).** 끊기면 획이 이어지는 동안만 영역을 입력 뷰 전체로 넓히는 길(누를 때 `requestLayout` 없이 영역만 다시 알리는 방법이 공개 API에 없어, 루트가 `requestLayout`을 불러 순회를 일으키는 방법)을 그때 검토한다 — 지금은 넣지 않는다 |
| 입력 연습 | `BoardFrame`(LLD-07 2.3)도 배경을 두지 않고 `KeyboardPlate.rect(폭, 높이, 0, 0, span)`(시스템 막대 여백 없음 → 자판 판 = 자판 사각형 × 틀 높이)을 `PlatePainter`로 칠한다. 남는 자리에는 연습 화면의 배경이 보인다. 아래에 다른 앱이 없어 터치 영역은 다루지 않는다: 남는 자리의 터치는 0.30처럼 **틀이 소비**하고(`onTouchEvent` = true, `OffKeyboardPointers`) 자판에 넘기지 않으며 연습 동작이 아니다(PR-33) |

**플랫폼 동작 — 확인한 것과 못 한 것.** 기기가 없어 실기는 하지 않았다. "확인"은 문서·소스를 읽은 것이고 실기 확인이 아니다.

| # | 내용 | 근거 | 상태 |
|---|---|---|---|
| ① | `TOUCHABLE_INSETS_REGION`이면 `touchableRegion`이 가리킨 자리만 터치를 받는다(API 11부터). 영역은 창 틀의 원점 기준 좌표다. `contentTopInsets`·`visibleTopInsets`는 입력기 창 위 끝 기준이고 앱을 줄이거나 미는 데 쓰인다 | 공식 문서 `InputMethodService.Insets` | 확인(문서) |
| ② | `InputMethodService.onComputeInsets` 기본 구현은 `touchableInsets = TOUCHABLE_INSETS_VISIBLE`로 두고 `touchableRegion`을 비운다 | AOSP `InputMethodService.java`(main) | 확인(소스) |
| ③ | 입력기는 제 뷰 뿌리의 `ViewTreeObserver`에 내부 인셋 계산 리스너를 걸고 거기서 `onComputeInsets`를 부른다. `ViewRootImpl.performTraversals`는 그 리스너가 있으면 순회마다(배치 블록 밖, 배치 뒤) 인셋을 다시 계산하고, 앞 값과 다르면 창 관리자에 알린다 | AOSP `InputMethodService.java`(main), `ViewRootImpl.java`(공개 미러의 master — main과 다를 수 있음) | 확인(소스), 판마다 같은지는 미확인 |
| ④ | 입력기 창의 배경: 플랫폼 기본 주제 `Theme.InputMethod`는 `Theme.Panel`을 물려받고 `Theme.Panel`은 `windowBackground` = 투명, `windowIsTranslucent` = true다. 대상 SDK가 높으면 `Theme.DeviceDefault.InputMethod`(→ `Theme.Material.InputMethod`)가 골라진다. `InputMethodService`는 전체 화면 모드에서만 제 영역에 배경을 넣는다(이 앱은 전체 화면 모드를 쓰지 않음, `onEvaluateFullscreenMode` = false). 이 앱은 서비스에 주제를 지정하지 않고 창 배경을 건드리지 않는다(`AndroidManifest.xml`) | AOSP `themes.xml`·`InputMethodService.java`(main, 파일 끝 일부는 읽지 못함) | 기본 주제는 확인(소스). `Theme.Material.InputMethod` 단계의 정의와 제조사 주제는 **미확인** → 실기 DV-36 ① |
| ⑤ | 입력기가 제 창 안에 내비게이션 막대(키보드 내리기·입력기 전환 버튼)를 그리는 구성에서는, 플랫폼이 입력기가 알린 영역에 **창 맨 아래의 시스템 막대 높이만큼의 띠(창 폭 전체)를 더한다.** 그 구성에서는 남는 자리 아래의 여백에서 시작한 터치가 아래 앱이 아니라 이 창으로 온다(루트가 입력 없이 소비). 칠하지 않는 것은 그대로다 | AOSP `NavigationBarController.java`(main) | 확인(소스). 어느 기기·내비게이션 방식이 이 구성인지는 **미확인** → 실기 DV-36 ④, 설계질의 FSS#89 |
| ⑥ | 다른 손가락이 자판을 누르고 있는 동안 투명한 자리에 닿은 손가락, 아래 앱을 누르고 있는 동안 자판에 닿은 손가락이 어느 창으로 가는지 | 읽은 근거 없음 | **미확인** → 실기 DV-36 ③ (창에 와도 입력 없음: 위 "터치 길") |
| ⑦ | 자판 판에서 시작한 획이 터치 영역 밖으로 나가도 이 창이 끝까지 받는지 | 읽은 근거 없음(같은 성질의 R2는 DV-26으로 확인됨) | **미확인** → 실기 DV-36 ⑦ |
| ⑧ | 창이 칠하지 않은 자리로 아래 앱이 실제로 보이는지, 터치 영역 밖 터치를 아래 앱이 실제로 받는지, 회전·설정 변경 바로 뒤의 영역, 시스템 막대 자리의 모습 | – | **미확인** → 실기 DV-36 ①②④⑤⑥ |

- ⑤는 설계(SRS FR-CMN-14 ⑫ "남는 자리 아래 여백도 투명한 자리")와 플랫폼이 어긋날 수 있는 곳이다. 공개 API로 그 띠를 빼는 방법은 찾지 못했다. 임시 처리: 코드는 설계대로 자판 판만 알리고, 플랫폼이 더한 띠로 온 터치는 입력 없이 소비한다. 설계팀에 설계질의로 알렸다(FSS#89, 막히지 않음).
- 시험: KP-01~KP-07(`KeyboardPlateTest`, JVM, LLD-11 7.5), PL-01~PL-08(`KeyboardPlateViewTest`)·KI-01~KI-04(`KeyboardInsetsTest`, Robolectric, LLD-11 7.7), PR-33(LLD-11 7.6). 실기기 DV-36(LLD-11 8절).

### 7.4 띄운 자판 — 손잡이 띠를 길게 눌러 옮기기 (0.32, FSS#86)

근거: SRS 0.84 FR-CMN-15 ①~⑭·1.3 용어·기준값(손잡이 띠 높이 24dp, 잡기 허용 거리 8dp, 길게 누름 시간)·TBD-33·FR-SET-09, HLD 0.77 5A "띄운 자판"·7.3·8장(`onComputeInsets`, 키보드 내려감)·10장 R12. 저장 키는 LLD-07 6.1, 설정 화면은 LLD-07 7절.

낱말(HLD 5A): **붙은 자판**(띄우기 끔, 또는 켬이지만 들어가지 않음 — 7.2·7.3 그대로) / **띄운 자판**(띄우기 켬이고 들어감). **손잡이 띠** = 띄운 자판의 자판 사각형 바로 위 띠(폭 R, 높이 24dp, 키가 아님). 띄운 자판의 **자판 판** = 손잡이 띠 + 자판 사각형(시스템 막대 여백 없음). **이동 영역 S** = 입력 뷰에서 아래·옆의 시스템 막대 여백과 위의 상태 막대·컷아웃 자리를 뺀 사각형.

```kotlin
// kbd.app.store (안드로이드 없음)
data class PxRect(…) { val width: Int; val height: Int }                 // 0.32: 폭·높이 더함
data class FloatSetting(val on: Boolean, val fx: Float?, val fy: Float?)  // 한 방향의 띄우기와 놓은 자리 비율(null = 그 축은 놓은 적 없음). DEFAULT = (false, null, null)
data class FloatPlace(val area: PxRect, val plate: PxRect) { val rangeX: Int; val rangeY: Int }   // S와 자판 판(입력 뷰 좌표 px). X = S 폭 − 판 폭, Y = S 높이 − 판 높이
data class FloatRatio(val fx: Float?, val fy: Float?)                    // 저장할 비율(null = 그 축은 저장하지 않음)
object FloatingKeyboard {
    const val HANDLE_DP = 24f; const val GRAB_SLOP_DP = 8f
    fun ratio(v: Float?): Float?                                          // 0~1로 자름, NaN은 null
    fun place(viewWidth: Int, viewHeight: Int, left: Int, top: Int, right: Int, bottom: Int,
              span: KeyboardSpan, plateHeight: Int, s: FloatSetting): FloatPlace?   // null = 붙은 자판
    fun drag(start: PxRect, dx: Int, dy: Int, area: PxRect): PxRect
    fun ratios(plate: PxRect, area: PxRect): FloatRatio
}
// kbd.app.view (안드로이드 없음 — 시각·좌표·기준값을 값으로 받음)
internal class PlateMover(longPressMs: Long, slopPx: Float) {
    sealed interface Effect   // Started, Moved(plate), Put(plate, save: FloatRatio), Cancelled(plate)
    val waiting: Boolean; val moving: Boolean; val pointer: Int?; val waitEnd: Long?
    fun down(first: Boolean, id: Int, x: Float, y: Float, time: Long, onHandle: Boolean, place: FloatPlace?): List<Effect>
    fun move(x: Float, y: Float, time: Long, place: FloatPlace?): List<Effect>
    fun up(id: Int, time: Long, place: FloatPlace?): List<Effect>
    fun timeout(time: Long, place: FloatPlace?): List<Effect>; fun cancel(): List<Effect>
}
// KeyboardRootView (0.32)
var portraitFloat: FloatSetting; var landscapeFloat: FloatSetting      // 바꾼 뒤 relayout()
var moveFeedback: FeedbackPort?                                          // 옮기기 시작 진동(8절 grab)
var onFloatMoved: ((landscape: Boolean, fx: Float?, fy: Float?) -> Unit)?   // 놓은 자리의 저장할 비율
internal val floatPlace: FloatPlace? /* private set */; internal val floated: Boolean
const val HANDLE_NAME = "자판 옮기기 손잡이"                             // 손잡이 띠의 접근성 이름
// FeedbackPort (0.32): fun grab() {} — 기본은 아무것도 안 함(8절)
```

| 항목 | 처리 |
|---|---|
| `FloatingKeyboard.place` | `s.on`이 거짓이거나 `span.width ≤ 0`이거나 `plateHeight ≤ 0`이면 null. S = (left, top, viewWidth − right, viewHeight − bottom). X = S 폭 − R, Y = S 높이 − `plateHeight`. **X < 0 또는 Y < 0이면 null**(들어가지 않음 → 붙은 자판, FR-CMN-15 ④). 가로: `ratio(fx)`가 있으면 `at(fx, X)`, 없으면 `span.left`를 [0, X]로 자른 값(정렬 설정의 자리 — `KeyboardWidth.place`의 정수 나눗셈까지 붙은 자판과 같은 px. 0.5 × X를 반올림해 쓰지 않는 까닭: 중앙모드에서 X가 홀수일 때 붙은 자판과 1px 어긋나지 않게, FR-CMN-15 ⑥ "자판이 뛰지 않는다"). 세로: `at(ratio(fy) ?: 1, Y)`. `at(f, r)` = `round(f × r)`(Double, 반올림)을 [0, r]로 자름 → 자판 판 전체가 늘 S 안(FK-05). 자판 판 = (left + x, top + y, left + x + R, top + y + `plateHeight`) |
| 들어가는지의 높이 | 루트가 `plateHeight` = `px(24dp)` + `px(6 × rowHeightDp)`로 부른다. viewHeight는 **부모가 허용하는 가장 큰 높이**(`onMeasure`의 높이 `MeasureSpec` 크기, 아래)이고 지금 놓인 높이가 아니다(FR-CMN-15 ④). 폭은 S 폭 = 가용 폭 A라 X = A − R ≥ 0이 늘 성립한다 |
| `drag` | x = `start.left + dx`를 [S 왼쪽, max(S 왼쪽, S 오른쪽 − 판 폭)]으로, y도 같은 식으로 **따로** 자른다. 변위 합을 자르므로 한계 너머로 갔다 돌아오면 손가락 자리에서 다시 따라온다(FR-CMN-15 ⑧, FK-06). 범위 0인 축은 S 끝에 머문다 |
| `ratios` | X > 0이면 fx = (판 왼쪽 − S 왼쪽) / X를 [0, 1]로, 아니면 null. fy도 같다(FR-CMN-15 ⑤ "범위 0인 축은 저장하지 않음", FK-07). 왕복 `place(ratios(p)) == p`는 px까지 같다: Float 몫의 상대 오차(약 6 × 10⁻⁸) × 화면 범위(수천 px)가 0.5px보다 훨씬 작다(FK-08) |
| 기준값 | 손잡이 띠 높이 `HANDLE_DP` = 24dp(SRS 1.3, TBD-33). 잡기 허용 거리 `GRAB_SLOP_DP` = 8dp(px = × density, 반올림하지 않은 Float로 비교). 길게 누름 시간 = `ViewConfiguration.getLongPressTimeout()`(⌫ 반복 첫 지연과 같은 값, HLD 5A) |
| 상태 기계 `PlateMover` | HLD 5A ⓐ~ⓚ 그대로. 상태 = 쉼 / 기다림(포인터, 누른 점, 누른 시각, 마지막 위치) / 끄는 중(포인터, 시작 때의 판, 시작 때의 손가락 위치, 지금 판) / 버림(포인터). `down(first = ACTION_DOWN인가)`: ⓐ 쉼 + first + `onHandle` → 기다림. ⓑ 쉼에서 first가 아님 → 그대로. ⓔ 기다림 + 다른 손가락(어디든) → 버림. ⓗ 끄는 중·버림 → 그대로. 새 제스처(first)인데 쉼이 아니면 남은 것을 먼저 `cancel()`(앞 제스처의 끝을 못 받은 경우). `move`: ⓒ 기다림에서 누른 점과 가로 또는 세로 차가 slop **이상** → 버림(미만이면 마지막 위치만 기억, PM-03). ⓖ 끄는 중 → `drag`(변위는 `roundToInt`), 판이 바뀌었을 때만 `Moved`. `up`: ⓓ 기다림의 그 포인터 → 쉼. ⓚ 버림의 그 포인터 → 쉼. ⓘ 끄는 중의 그 포인터 → 쉼 + `Put(판, save)`, save = `ratios(판, S)`에서 **판의 왼쪽(위)이 시작 때와 px로 같은 축은 null**(FR-CMN-15 ⑨, TBD-33 ⑯, PM-09). 다른 포인터의 올라옴은 아무 일 없음(PM-08). `cancel()`(ⓙ): 끄는 중이면 `Cancelled(시작 때의 판)`, 어느 상태든 쉼(PM-11) |
| 시간 | ⓕ는 사건마다 먼저 따진다(`catchUp`): 기다림이고 `time − 누른 시각 ≥ longPressMs`이면(같으면 지남, PM-01) 그 사건을 다루기 **전에** 끄는 중으로 가며 `Started`를 내고(PM-07), 시작 때의 손가락 위치 = 마지막으로 본 위치, 시작 때의 판 = 그때의 `place.plate`. 그때 `place`가 null(띄운 자판이 아님)이면 쉼으로 가고 아무것도 내지 않는다(PM-12). 루트는 기다림의 끝(`waitEnd`)에 `Handler.postDelayed` 하나를 걸어 `timeout(uptimeMillis)`를 부른다 — 움직이지 않는 손가락의 시작을 그 때로 당길 뿐이다. 효과를 처리할 때마다 타이머를 지우고 기다림이면 다시 건다(`armLongPress`). 사건 시각은 `MotionEvent.eventTime`(uptime 기준, 타이머와 같은 시계) |
| 터치 길(루트) | `dispatchTouchEvent`가 맨 먼저 `onMoveEvent(ev)`로 상태 기계에 알린 뒤 0.31 길(`OffKeyboardPointers.filter` → `super`)로 간다. `onMoveEvent`: 손잡이 띠 뷰가 없고 따르는 포인터도 없으면 아무것도 하지 않는다(붙은 자판 = 상태 기계가 쉼을 떠나지 않음). `ACTION_DOWN`·`POINTER_DOWN` → `down(first = ACTION_DOWN, onHandle = floated이고 점이 손잡이 띠 뷰의 사각형 안)`, `MOVE` → 따르는 포인터의 점으로 `move`(그 포인터가 이벤트에 없으면 무시), `UP`·`POINTER_UP` → `up`, `CANCEL` → `cancel`. 좌표는 루트(입력 뷰) 좌표 — 끄는 동안 루트는 움직이지 않는다(HLD 5A "좌표") |
| 효과 처리 | `Started`: `moveFeedback.grab()`(진동 한 번, 8절), 손잡이 표시 `held = true`, `swallowGesture = true` — 7.1과 같은 장치로 **그 제스처의 나머지 전부**를 루트가 가로챈다: 이미 자식이 받은 포인터(손잡이 띠)는 `ACTION_CANCEL`, 그 뒤 새로 닿은 손가락은 자식에 가지 않고, 놓은 뒤에도 모든 손가락을 뗄 때까지(다음 `ACTION_DOWN`까지) 그대로다(FR-CMN-15 ⑧, FL-13). 옮기기가 시작될 때 모드 뷰가 받은 포인터는 없다(⑪ 마 — 시작은 첫 손가락이 띠에 있고 그동안 다른 손가락이 없을 때뿐). `Moved`: `movePlate`. `Put`: `held = false`, `movePlate`, save에 값이 있으면 그 방향(잰 때의 방향)의 `FloatSetting`에 넣고 `onFloatMoved(방향, fx, fy)`를 한 번 부른 뒤 `requestLayout()`. `Cancelled`: `held = false`, `movePlate(시작 때의 판)`, `requestLayout()` |
| `movePlate(to)` | 다시 재거나 놓지 않고 모든 자식을 `offsetLeftAndRight(dx)`·`offsetTopAndBottom(dy)`로 같은 변위만큼 옮기고 `plate`·`floatPlace.plate`를 바꾼 뒤 `invalidate()`(손잡이 띠·모드 뷰·덮개·버튼이 한 덩어리, FR-CMN-15 ⑧, FL-06). 재기·놓기를 거치지 않아 프레임마다의 일은 자식 옮김과 다시 그리기뿐이다(NFR-01 실측은 실기 R12). 모드 뷰의 키 기하는 모드 뷰 좌표라 다시 만들 일이 없다 |
| 끄는 동안의 터치 영역(결정) | 놓을 때와 취소할 때 `requestLayout()`으로 순회를 일으켜 `onComputeInsets`가 **새 자판 판**을 알리게 한다(HLD 5A "터치 영역은 놓을 때와 취소할 때 반드시", FL-18 `isLayoutRequested`). 끄는 동안에는 따로 요청하지 않는다: `invalidate`도 창 순회를 일으키고 플랫폼은 순회마다 인셋을 다시 묻는 것으로 읽었으나(7.3 ③, 판마다는 미확인 — 아래 ⑫) 그것에 기대지 않는다. 끄는 동안 영역이 시작 때의 판으로 남아 있어도 잡는 손가락의 이동은 누른 창이 계속 받는 것으로 보고(7.3 ⑦과 같은 성질, 실기 미확인), 새로 닿은 손가락은 어디서든 입력이 없다(⑧) |
| `onMeasure` | 따르는 포인터가 있으면 먼저 `dropMove()`(다시 재면 옮기기·기다림 취소, ⓙ). span은 7.2 그대로. maxH = 높이 `MeasureSpec`이 `UNSPECIFIED`이면 0, 아니면 그 크기(`AT_MOST`·`EXACTLY`의 크기 = 부모가 허용하는 가장 큰 높이로 본다). 잰 방향을 기억하고(`measuredLandscape`) `measuredFloat` = 손잡이 띠 뷰가 있음(그 방향 띄우기 켬) && `floatPlaceIn(폭, maxH) != null`. 손잡이 띠 뷰는 띄운 자판이면 R × 24dp, 아니면 0 × 0으로 잰다. 그 밖의 자식은 7.2 그대로(같은 크기 — 폭 W, 6줄 안의 높이). 높이 = 띄운 자판이면 **maxH**, 아니면 7절 그대로(6줄 + 아래 padding) |
| `onLayout` | `measuredFloat`이면 `floatPlace = floatPlaceIn(폭, 높이)`(같은 방향 설정, 같은 span), 아니면 null. `plate` = `floatPlace.plate` 또는 7.3 `KeyboardPlate.rect`. 붙은 자판이면 7.2 놓기 그대로(손잡이 띠 뷰가 있으면 (0, 0, 0, 0)에 놓아 보이지도 닿지도 않게 — 켬이지만 들어가지 않을 때, FL-02). 띄운 자판이면 `layoutOnPlate(p)`: 손잡이 띠 = (p.left, p.top, p.right, p.top + 24dp), 안내표 버튼 = (x + W − 버튼 폭, 띠 아래 끝), 그 밖의 자식 = (x, p.bottom − 자식 높이)(x = p.left + m) — 키 영역 안의 놓기(낮은 자판 바닥 정렬, 그래피티 빈 띠)는 붙은 자판과 같다(HLD 5A "입력 뷰 높이와 놓기", FL-03) |
| 이동 영역의 위 t | `OnApplyWindowInsetsListener`에서 `statusBars() ∪ displayCutout()`의 위 값(두 종류 중 큰 값)을 받는다. 띄운 자판으로 놓여 있으면 받은 값을 그대로 쓰고(작아져도), 붙은 자판이면 **더 큰 값만** 받아들인다 — 낮은 붙은 자판의 입력 뷰는 위쪽 인셋을 받지 못해 0이 올 수 있는데, 그 0으로 S가 높아져 다시 띄우고 띄운 높은 뷰가 인셋을 받아 다시 붙는 것이 되풀이되지 않게 한다(FR-CMN-15 ④ "판정이 하나로", HLD 5A, FL-20). 값이 바뀌고 그 방향 띄우기가 켜져 있으면 `requestLayout`. 화면 방향이 바뀌면(루트의 `onConfigurationChanged`에서 지금 방향 ≠ 잰 방향) t를 0으로 지우고 `requestApplyInsets`로 다시 받는다(다른 방향의 값이 남지 않게). 새 루트는 t = 0에서 시작하므로 경계 높이에서는 한 번 띄웠다가 인셋을 받고 붙은 자판으로 자리 잡을 수 있다(그 뒤로는 되풀이 없음, FL-20) |
| 손잡이 띠 뷰 `MoveHandle` | 루트의 자식(HLD 5A "손잡이 띠" — 띠를 누르고 기다리는 동안 키에 닿은 둘째 손가락이 모드 뷰로 가게, FL-14). 그 방향 띄우기가 켜져 있을 때만 있다(`relayout`의 `syncHandle`: 켜면 `addView`(맨 위 z), 끄면 `removeView`). 끈 방향에서는 뷰가 없다(FL-01). `onTouchEvent` = 늘 true(받아 소비, 입력·피드백 없음 — 키가 아님, FL-12). 다른 손가락이 키를 누르고 있는 동안 띠에 닿은 손가락은 `ViewGroup`이 손잡이 띠에 보내고 거기서 소비된다(모드 뷰로 가지 않음, HLD ⓑ, FL-15). 그리기: 배경은 루트가 칠한 자판 판(키보드 배경색), 가운데에 둥근 막대 하나 — 보통 32 × 4dp 회색(`0xFF808080`), 잡힌 동안(`held`) 48 × 4dp 흰색. 접근성 이름 `HANDLE_NAME` |
| 다시 놓기·취소 | `relayout()`이 맨 먼저 `dropMove()`(상태 기계 `cancel`, 타이머 지움, `held = false` — 판을 되돌려 옮기지 않고 이어지는 배치가 저장값으로 놓음) 뒤 `syncHandle()`, 그 다음 7절 그대로. 서비스가 부르는 곳: 설정 흐름(줄 높이·폭·정렬, 그리고 0.32부터 **띄우기 설정이 루트의 것과 다를 때** — 루트가 놓을 때 이미 넣은 비율이 흐름으로 돌아오면 같으므로 다시 놓지 않는다, FR-CMN-15 ⑨), `onCreateInputView`(새 루트에 두 방향 값·`moveFeedback`·`onFloatMoved`를 넣고 `relayout` — 손잡이 띠가 생김). 루트 자신의 `onConfigurationChanged`도 `relayout()`(서비스의 `onConfigurationChanged` → `relayout()`과 둘 다 불려도 같은 결과, FL-11). 키보드 내려감(`onFinishInputView`·`onWindowHidden` → `cancelTouches()`의 합성 `ACTION_CANCEL`)·플랫폼의 `ACTION_CANCEL`은 `onMoveEvent` → `cancel()` → `Cancelled`(시작 때의 자리로 되돌림, 저장 없음, FL-18). `onDetachedFromWindow`도 `dropMove()`(타이머가 떠난 뷰를 부르지 않게) |
| 저장 | 루트의 `onFloatMoved` = 서비스의 `saveFloatPosition(landscape, fx, fy)`: 서비스의 `settings` 사본에 그 방향의 비율을 먼저 넣고(null 축은 그대로) `SettingsStore.setFloatPosition`(LLD-07 6.1)을 코루틴으로 부른다. 놓을 때 한 번만 쓴다(끄는 동안 쓰지 않음). 같은 값이 흐름으로 돌아와도 루트의 값과 같아 다시 놓지 않고, 놓더라도 `ratios` → `place` 왕복이 같은 px라 자리가 같다(FK-08, FL-08) |
| 인셋(`KeyboardInsets.compute`) | 띄운 자판(`root.floated`)이면 `contentTopInsets = visibleTopInsets = loc[1] + root.height`(입력 뷰 아래 끝 = 앱이 내줄 자리 0, FR-CMN-15 ⑫), `touchableInsets = TOUCHABLE_INSETS_REGION`, `touchableRegion` = 창 좌표의 자판 판 — 늘 `REGION`이다(7.3에서 미룬 결정. `VISIBLE`이면 보이는 인셋이 아래 끝이라 터치를 받을 자리가 없다, KI-05). 붙은 자판이면 7.3 그대로(KI-01~KI-04, KI-06의 끔) |
| 입력 연습 | `BoardFrame`은 띄우기 값을 읽지 않는다(LLD-07 2.3, FR-CMN-15 ⑭, PR-34) |
| 테두리·획 | 모드 뷰는 고치지 않는다(HLD 5A). 테두리는 자판 사각형의 네 변이고 손잡이 띠는 모드 뷰 밖 — 맨 윗줄에서 위로 그은 획은 모드 뷰가 음수 y로 계속 받아 붙은 자판과 같은 결과다(FL-16). 모드 전환·덮개 여닫기는 span을 바꾸지 않아 판이 그대로이고, 덮개가 열린 채 옮겨진다(FL-17) |

**플랫폼 동작 — 확인한 것과 못 한 것(0.32).** 기기가 없어 실기는 하지 않았다. 7.3 표의 ①~⑧은 그대로 적용된다(창 높이 전체로 넓어졌을 뿐 같은 장치다).

| # | 내용 | 근거 | 상태 |
|---|---|---|---|
| ⑨ | `contentTopInsets`는 뒤의 앱을 크기 조절·밀기 하는 데 쓰는 기본 자리를, `visibleTopInsets`는 뒤의 앱을 보이게 덮는 위 부분을 정하고, 둘 다 **입력기 창 위 끝 기준**이다. 그래서 창 높이(입력 뷰 아래 끝)를 주면 앱이 내줄 자리가 0이다 | 공식 문서 `InputMethodService.Insets`(`contentTopInsets`·`visibleTopInsets` 설명) | 값의 뜻은 확인(문서). **앱이 실제로 밀리지 않는지, 앱이 키보드 보임을 어떻게 받는지(API 30 이상의 창 인셋 포함)는 미확인** → 실기 DV-37 ①, R12 ① |
| ⑩ | `touchableRegion`은 `TOUCHABLE_INSETS_REGION`일 때 쓰이고 창 틀의 원점 기준이다 | 공식 문서 `InputMethodService.Insets` | 확인(문서) |
| ⑪ | 입력 뷰를 키우는 길: 루트가 높이 `MeasureSpec`의 크기를 제 높이로 잰다. 입력기 창의 높이가 내용을 따라 정해지는지, 루트가 받는 `MeasureSpec`의 종류·크기(상태 막대·컷아웃 아래까지인지), 창 위쪽 인셋(상태 막대·컷아웃)을 받는지 | 공식 문서 `InputMethodService`는 입력 뷰가 화면 아래에 놓이고 전체 화면이 아니면 앱이 그 위로 옮겨지거나 줄어든다고만 적고 창 크기·재기 조건은 적지 않는다(`getInputMethodWindowRecommendedHeight`는 API 29에서 동작이 잘 정의된 적 없다며 폐기). 소스는 이번에 읽지 않았다 | **미확인** → 실기 DV-37 ③, R12 ③. `UNSPECIFIED`가 오면 붙은 자판으로 잰다(위 `onMeasure`). 위쪽 인셋이 오지 않으면 t = 0(S의 위 = 입력 뷰 위 끝) |
| ⑫ | 끄는 동안 `invalidate`만으로 일어난 순회에서도 `onComputeInsets`가 다시 불려 영역이 따라오는지 | 7.3 ③(소스, 판마다 미확인) | **미확인**. 기대지 않는다(위 "끄는 동안의 터치 영역") → 실기 DV-37 ⑦ |
| ⑬ | 길게 누름 시간 `ViewConfiguration.getLongPressTimeout()`의 기기 값 | 공식 문서 `ViewConfiguration.getLongPressTimeout`(누름이 길게 누름이 되기까지의 시간). 기기마다의 값은 문서에 없음 | 기기 값은 **미확인** → 실기 DV-37 ⑧ |
| ⑭ | 화면만 한 투명 입력기 창의 부작용(뜨고 내릴 때의 애니메이션, 그리기 비용, 제조사 기능), 시스템의 키보드 내리기·입력기 전환 버튼, 분할 화면·팝업 창 | 읽은 근거 없음 | **미확인** → 실기 DV-37 ②④⑥ |

- 시험: FK-01~FK-08(`FloatingKeyboardTest`, JVM), PM-01~PM-12(`PlateMoverTest`, JVM), FL-01~FL-20(`KeyboardFloatViewTest`)·KI-05·KI-06(`KeyboardInsetsTest`)·FB-05(`FeedbackTest`)·OB-06·OB-07(`KeyboardSizeBoxesTest`)·PR-34(`PracticeScreenTest`)(Robolectric), SS-12~SS-14(`SettingsStoreTest`). LLD-11 7.5·7.6·7.7. 실기기 DV-37(LLD-11 8절).

## 8. Feedback

```kotlin
interface FeedbackPort { fun key(); fun error(); fun grab() {} }   // 뷰와 InputDispatcher가 쓰는 경계. grab(0.32)은 기본 빈 구현
class Feedback(private val view: () -> View?, private val audio: AudioManager,
               private val settings: () -> Settings) : FeedbackPort
```

| 함수 | 처리 | 근거 |
|---|---|---|
| `key()` | `settings.haptic`이면 `view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)`; `settings.sound`이면 `audio.playSoundEffect(AudioManager.FX_KEY_CLICK)` | HLD 8장, FR-SET-03 |
| `error()` | `settings.haptic`이면 `performHapticFeedback(HapticFeedbackConstants.REJECT)`(API 30 미만은 `LONG_PRESS`) | FR-TAP-01b, FR-GRF-08 |
| `grab()`(0.32) | 띄운 자판의 옮기기 시작(7.4). `settings.haptic`이면 `performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)` 한 번. 소리는 없다. 키 입력 피드백이 아니다 | FR-CMN-15 ⑦ 나 |

- **`key()`는 키를 누를 때 부른다**(SRS FR-CMN-11a ②, FR-SET-03, HLD 5A·8장, DCR-11 결정). 부르는 곳: `TapKeyboardView` DOWN(모든 키, LLD-05A 4절), `GraffitiPadView` 보조 줄 DOWN(LLD-05 8.3). 손을 뗄 때·결과가 나올 때·반복 때는 부르지 않는다.
- 키 입력 피드백을 내지 않는 곳(SRS FR-CMN-11a ②, HLD 8장 피드백, GRAFFITI 6.2-9, 설계질의 #6 Q1·Q3 확정): 그래피티 패드 획(키가 아님, 인식 실패만 `error()`), 창 위 빈 띠(키가 아님), ⌫·방향키 반복(누를 때 한 번만).
- `error()`를 부르는 곳: 유니코드에 없는 윗첨자·아래첨자(FR-TAP-01b), 그래피티 인식 실패(FR-GRF-08).
- 기본값은 진동 켬·소리 끔이다(`Settings.haptic = true`, `sound = false`, LLD-07 6.1, DCR-14 결정).
- 시험: FB-01~FB-05(FB-05: `grab` 진동 한 번·소리 없음·진동 끔이면 없음, 0.32).

## 9. 시퀀스

### 9.1 모드 키 (두벌식 "각" 조합 중, 입력함, 이전 모드 = 그래피티)

```
TapKeyboardView DOWN(모드 키) → Feedback.key(); UP(탭, 0.23) → TapSink.onFunctionDown(MODE) → InputDispatcher.onModeKey
  → finishAll: HangulComposer.finish() → ComposingController.finishComposing → EditorPort.finishComposingText
  → resetParsers
  → ModeController.onModeKey: typed && previous(GRAFFITI) != current(DUBEOLSIK) → next = GRAFFITI, previous = DUBEOLSIK
  → inNumericField == false → saveMode(GRAFFITI) → SettingsStore.setLastMode
  → KeyboardService.showModeView(GRAFFITI) → GraffitiPadView → KeyboardRootView.setModeView(…, 5)
     조합기 = HangulComposer(ComposePolicy.GRAFFITI), dispatcher.graffiti = 그래피티 처리기 → GraffitiInputHandler.enterMode()
```

### 9.2 입력창 시작 (한글 두벌식으로 쓰다가 숫자 비밀번호 입력창으로)

```
InputMethodService → KeyboardService.onStartInputView(info)
  → FieldInfo.from → (숫자 비밀번호 → fieldClass NUMBER, isPassword, numericLike)
  → InputDispatcher.finishAll, resetParsers
  → ModeController.onFieldChanged (typed = false)
  → ModeController.enterNumericField: returnMode = DUBEOLSIK, current = QWERTY (previous·마지막 모드 저장 그대로)
  → ErrorLog.enabled = false
  → ComposingController.onStart(initialSel)
  → showModeView(QWERTY, f) → TapKeyboardView(TapLayouts.QWERTY) (6줄, 숫자 줄 포함, 모드 키 있음)
```

### 9.3 숫자류 입력창을 떠남 (9.2에서 모드 키로 그래피티로 바꾼 뒤 문자 입력창으로)

```
(9.2 상태) 모드 키 → InputDispatcher.onModeKey → ModeController.onModeKey: typed false → next = GRAFFITI(쿼티 다음)
  → inNumericField == true → saveMode 부르지 않음(저장된 마지막 모드 = DUBEOLSIK 그대로)
  → showModeView(GRAFFITI), GraffitiInputHandler.enterMode()
문자 입력창 → onStartInputView → FieldInfo(numericLike = false)
  → finishAll, resetParsers, onFieldChanged
  → ModeController.leaveNumericField: current = DUBEOLSIK, returnMode = null
  → showModeView(DUBEOLSIK) → TapKeyboardView(TapLayouts.DUBEOLSIK)
```

### 9.4 커서 이동 (그래피티 "가" 조합 중 사용자가 화면을 눌러 커서를 옮김)

```
편집기 → KeyboardService.onUpdateSelection(…, newS=newE=3, candS=7, candE=8)
  → ComposingController.onUpdateSelection: 조합 중, newE != candE → USER_MOVED
  → InputDispatcher.onCursorMoved(newS, newE, candS, candE)
      → HangulComposer.finish(), ComposingController.finishAfterUserMove (임시 글자를 빼고 확정, 사용자 커서 유지)
      → resetParsers(CURSOR_MOVED) → GraffitiInputHandler.reset (천지인 조립기·일회성 상태 비움)
```
