# LLD-08 입력기 생명주기·플랫폼 처리·모드 키

- 대응 HLD 절: 8장(입력기 생명주기와 플랫폼 처리), 8.1(ModeController), 5A의 창 높이·바닥 정렬·처리 시점
- 버전 **0.26** (2026-11-02)
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
- `InputMode`·`FunctionKey`는 [LLD-03-modules.md#35-모드와-기능키](LLD-03-modules.md#35-모드와-기능키), 조합기는 [LLD-04-swipe.md#7](LLD-04-swipe.md#7-hangulcomposer-core-hangul)에 있다.
- 요구: FR-CMN-01~12(04·04b·11a 포함), FR-TAP-04, 10, 11a, FR-CMN-09, FR-GRF-08, NFR-05.

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
class KeyboardService : InputMethodService(), SwipeHost {
    lateinit var dispatcher: InputDispatcher
    lateinit var modes: ModeController
    lateinit var root: KeyboardRootView
}
```

| 함수 | 처리 | 근거 |
|---|---|---|
| `onCreate()` | `settings = SettingsStore.loadBlocking()`, `thresholds = ThresholdDefaults.VALUES`(0.26, 고정 상수, LLD-07 3절), `ErrorLog` 준비, `swipe = SwipeModuleProvider.get()`. `modes = ModeController(InputMode.entries.toList(), settings.lastMode)`(0.25: 모든 모드, 이슈 #251). `dispatcher = InputDispatcher(…)`. 설정 `flow`를 모아 바뀌면 `applySettings()` | FR-CMN-10, HLD 7.3 |
| `onCreateInputView()` | `root = KeyboardRootView(…)`. `showModeView(modes.current)` | FR-CMN-10 |
| `onStartInputView(info, restarting)` | 아래 3.1 | FR-CMN-03e, 04, 04b, 08 |
| `onUpdateSelection(oldS, oldE, newS, newE, candS, candE)` | `r = composing.onUpdateSelection(newS, newE, candS, candE)`. `USER_MOVED` → `dispatcher.onCursorMoved(newS, newE, candS, candE)`. `OTHER_MOVED` → `dispatcher.onCursorMovedNoComposing()` | FR-CMN-06, HLD 8장, R4 |
| `onFinishInputView(finishing)` | `dispatcher.finishAll()`, `guide?.dismiss()`, 반복 타이머 중지. `finishing == false`(키보드가 내려감)면 `root.closeGuide()`. `true`(입력창만 바뀜, 이어서 `onStartInputView`)면 획 안내표는 열린 채(0.24) | FR-CMN-06, FR-GRF-09 |
| `onFinishInput()` | `dispatcher.finishAll()` | HLD 8장 |
| `onWindowHidden()` | 진행 중 터치·타이머 버림(`root.cancelTouches()`), `root.closeGuide()`(0.24) | D-27, FR-GRF-09 |
| `onEvaluateFullscreenMode()` | `false` | FR-CMN-12 |
| `onConfigurationChanged(c)` | `super`, `root.relayout()`(같은 배치를 새 폭에, 줄 높이 유지) | FR-CMN-12 |
| `onComputeInsets(out)` | `contentTopInsets = visibleTopInsets = 0`(입력 뷰 위 끝), `touchableInsets = TOUCHABLE_INSETS_FRAME`. 창 전체(빈 띠 포함)가 터치를 받는다 | SRS 3.3 빈 띠 |
| `SwipeHost` 구현 | `onSwipeEvents(ev, errorVibe) = dispatcher.onJamoEvents(ev, errorVibe)`(0.18), `feedback` = 8절 `Feedback`, `centerConsonant = dispatcher.composer?.lastConsonant()`(0.21, LLD-04 9.5) 등 | LLD-03 2.2 |

### 3.1 `onStartInputView(info, restarting)`
```
f = FieldInfo.from(info.inputType, info.imeOptions)
dispatcher.field = f
dispatcher.finishAll()                     // 조합 확정(HLD 8장)
dispatcher.resetParsers()                  // 긋기 천지인 조립기(0.17: 기준 키·쌍자음 후보 없음), 그래피티 일회성(HLD 8장, 5장)
modes.onFieldChanged()                     // typed = false, previous 유지(FR-CMN-03e)
before = modes.current
if f.numericLike: modes.enterNumericField()   // 영문 쿼티로 시작, 돌아갈 모드 기억(5절, FR-CMN-04·04b)
else:             modes.leaveNumericField()   // 돌아갈 모드가 있으면 그 모드로(5절)
errorLog.enabled = !f.noRecord             // NFR-05, FR-CMN-08 ①② (0.21)
composing.onStart(info.initialSelStart, info.initialSelEnd)
showModeView(modes.current, f)
if modes.current == GRAFFITI && before != GRAFFITI: dispatcher.graffiti?.enterMode()   // → GraffitiState.enterMode()(LLD-05 6절, G6)   // 그래피티에 새로 들어옴(HLD 5장 초기화, FR-GRF-05a)
```
- 같은 숫자류 입력창에서 키보드가 다시 떠도(`onStartInputView` 다시 호출) 이 순서를 그대로 탄다. 그래서 다시 영문 쿼티로 시작하고 돌아갈 모드는 그대로다(FR-CMN-04b).
- 자동으로 바뀌는 모드(쿼티로 시작, 돌아가기)는 `previous`와 마지막 모드 저장을 바꾸지 않는다(5절, FR-CMN-04b).

#### `showModeView(mode, f)`
| 조건 | 보이는 뷰 |
|---|---|
| `mode == SWIPE` | `swipe.createSwipeView(this, this)` |
| `mode == QWERTY` / `DUBEOLSIK` | `TapKeyboardView(TapLayouts.QWERTY / DUBEOLSIK)` |
| `mode == GRAFFITI` | `GraffitiPadView` |

- 숫자류 입력창이라고 따로 고르는 뷰는 없다. `ModeController`가 이미 `current = QWERTY`로 바꿨으므로 쿼티 자판이 보이고, 사용자가 모드 키로 바꾸면 그 모드의 뷰가 보인다(FR-CMN-04b "모드 키는 평소처럼").
- 뷰를 넣기 전에 `mode == GRAFFITI`면 `root.enableGuide(templates, feedback)`, 아니면 `root.disableGuide()`(획 안내표 닫힘·버튼 숨김, 0.24, 7.1). `templates`는 `createGraffiti()`가 읽은 `TemplateSet`(인식기와 같은 것).
- 뷰는 `root.setModeView(view, rowsOfView)`로 넣는다(7절). 조합기 정책: SWIPE → `ComposePolicy.SWIPE`, DUBEOLSIK → `DUBEOLSIK`, GRAFFITI → `GRAFFITI`, QWERTY → 조합기 없음(글자는 바로 확정).
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
    fun show(r: ComposeResult, pendingGlyph: String = "", replacesLast: Boolean = false)   // replacesLast: 0.21 2회차, 긋기 낱모음 ‥
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
| `show(r, g)` | `beginBatch`. `r.committed`가 있으면 `commitText(r.committed)`(조합 구간을 바꿔 확정). `d = r.composing + g`(0.21 2회차: `replacesLast`이고 g가 있으면 `r.composing.dropLast(1) + g` — 긋기 낱모음 ‥가 조합기의 ㅣ / ㅡ 자리에 보임, SWIPE 0.36 3.4, D-64). `bare = r.composing`(글자를 뺀 조합 글자). 글자를 뺄 때(`finishComposing`의 `dropGlyph`, `finishAfterUserMove`)는 `bare`로 되돌리고, 커서 보정은 `composingShown.length − bare.length`(‥가 ㅣ 자리면 0). `d`가 있으면 `setComposingText(d)`, 없고 `composingShown`이 있었으면 `setComposingText("")`(조합 구간 지움). `endBatch`(던져도 항상). 배치 안 호출이 모두 돌아오면 `composingShown = d`, `glyph = g`(던지면 그 앞 값 그대로). 기대 커서는 돌아온 쓰기마다 큐에 넣음(4.1) | FR-CMN-05, HLD 5장 임시 글자 |
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
class ModeController(private val available: List<InputMode>, saved: InputMode?) {
    var current: InputMode; var previous: InputMode? = null; var typed = false
    var returnMode: InputMode? = null                  // 숫자류 입력창에 들어가기 직전의 모드(돌아갈 모드). null = 숫자류 입력창 밖
    val inNumericField: Boolean get() = returnMode != null
    fun onInput()
    fun onFieldChanged()
    fun onModeKey(): InputMode
    fun enterNumericField()
    fun leaveNumericField()
}
```

| 함수 | 처리 | 근거 |
|---|---|---|
| 생성 | `current = saved`가 `available`에 있으면 그 값, 아니면 `available[0]`(= SWIPE, 0.25: 모든 빌드가 모든 모드). `previous = null`, `typed = false`, `returnMode = null`(프로세스 시작) | FR-CMN-10, 03b, 03e, HLD 8.1 |
| `onInput()` | `typed = true` | FR-CMN-03a |
| `onFieldChanged()` | `typed = false`(previous·returnMode 유지) | FR-CMN-03e |
| `onModeKey()` | HLD 8.1 그대로: `next = if (typed && previous != null && previous != current) previous else nextInOrder(current)`; `if (typed) previous = current`; `current = next`; `typed = false`; `return next`. 숫자류 입력창 안에서도 같다(`returnMode`는 바꾸지 않음) | FR-CMN-03, 03c, 04b |
| `enterNumericField()` | `if (returnMode == null) returnMode = current`; `current = QWERTY`. `previous`·`typed`는 바꾸지 않는다 | FR-CMN-04, 04b, HLD 8.1 |
| `leaveNumericField()` | `if (returnMode != null) { current = returnMode; returnMode = null }`. `previous`·`typed`는 바꾸지 않는다. 숫자류 입력창 밖에서 부르면 아무것도 하지 않는다 | FR-CMN-04b, HLD 8.1 |
| `nextInOrder(m)` | `available`에서 m 다음, 끝이면 처음(SWIPE → QWERTY → DUBEOLSIK → GRAFFITI → …) | FR-CMN-03, 03b |

- `QWERTY`는 늘 `available`에 있다. 그래서 `enterNumericField`는 늘 쿼티로 간다.
- **돌아갈 모드의 수명:** 숫자류 입력창에서 다른 숫자류 입력창으로 옮기면 `enterNumericField`가 다시 불려도 `returnMode`는 처음 값 그대로이고 `current`는 다시 QWERTY다(사례 표 4행). 같은 숫자류 입력창에서 키보드가 다시 떠도 같다(FR-CMN-04b). 숫자류 입력창이 아닌 입력창으로 옮길 때만 `returnMode`로 돌아가고 비운다(사례 3·5행).
- **마지막 모드 저장:** `inNumericField`인 동안 모드 키로 바꾼 모드는 저장하지 않는다(6.1 `onModeKey`). 자동 전환(enter·leave)도 저장하지 않는다. 그래서 저장된 마지막 모드는 숫자류 입력창 안에서는 늘 돌아갈 모드와 같고, 숫자류 입력창 안에서 프로세스가 다시 시작되면 생성 때 `current = 저장된 마지막 모드`가 되고 곧 이은 `onStartInputView`의 `enterNumericField`가 그 값을 `returnMode`로 삼아 쿼티로 시작한다(사례 7행, HLD 8.1).
- `returnMode`는 저장하지 않는다(프로세스 시작 때 null, HLD 8.1).
- 확정·초기화·저장 같은 부수 효과는 `InputDispatcher.onModeKey()`와 `KeyboardService.onStartInputView`가 한다(6절, 3.1). 이 클래스는 상태 기계만 둔다(HLD 8.1 "순수 Kotlin 상태 기계").
- `onInput()`을 부르는 곳과 부르지 않는 곳은 6.2 표.
- 시험: MC-01~MC-10(SRS 모드 키 예시 10행), MC-11~MC-13, NF-01~NF-09(SRS 숫자류 입력창 사례 7행 + 재표시·저장).

## 6. InputDispatcher

뷰가 내는 결과를 조합기·편집기·모드 상태에 반영한다. 안드로이드 없는 클래스이고 `TapSink`, `SwipeHost`의 입력 부분, 그래피티 `InputSink`, `EngineGuard`의 `GuardedState`(LLD-09 2절)를 구현한다.

```kotlin
class InputDispatcher(private val modes: ModeController, private val out: ComposingController,
                      private val feedback: FeedbackPort, private val saveMode: (InputMode) -> Unit) {
    var composer: HangulComposer?                     // 모드 정책(3절)
    var swipeState: SwipeEngineState?                  // 긋기 모드만(SwipeInputMapper를 감싼 것, LLD-03 2.2)
    var graffiti: GraffitiInputHandler?                // 그래피티 모드만
    var field: FieldInfo
}
```

### 6.1 함수

| 함수 | 처리 | 근거 |
|---|---|---|
| `onJamoEvents(ev, errorVibe)` (긋기) | `if (errorVibe) feedback.error()`(0.18: 조립을 닫으며 모음이 못 된 획을 버림); `r = composer.applyAll(ev)`; `out.show(r, swipeState?.pendingGlyph() ?: "", swipeState?.pendingReplacesVowel() == true)`(ㆍ·‥ 임시 글자를 조합 글자 뒤에, 빈 이벤트도 보임); `markInput()` | HLD 4.1, SWIPE 3.3·3.4 |
| `onSwipeText(s, vibe)` (둘레 숫자·기호) | `finishAll()`; `out.commitText(s)`; `if vibe feedback.error()`; `markInput()`. 조립기 비움은 매퍼가 이미 함(LLD-04 6.2) | FR-SWP-42, 43, D-30 |
| `onTapOutput(key, o)` | `o.jamo != null`(두벌식 글자) → `j = Jamo.ofCompat(o.jamo)`, `composer.apply(if j.kind == CONSONANT Consonant(j) else Vowel(j))` → `out.show`. `o.text != null` → `finishAll()` 뒤 `out.commitText(o.text)`(쿼티 글자도 이 길, FR-TAP-11a). `o.errorVibe` → `feedback.error()`. `markInput()` | FR-TAP-01a~03a, 11a |
| `onFunctionDown(BACKSPACE)` | `backspace()`; `markInput()` | FR-CMN-07 |
| `onFunctionDown(SPACE)` | `finishAll()`; `out.commitText(" ")`; `closeSwipe()`; `markInput()`. `closeSwipe()` = `if (swipeState?.reset() == true) feedback.error()`(0.18) | FR-CMN-03a |
| `onFunctionDown(ENTER)` | `finishAll()`; `out.enter(field)`; `closeSwipe()`; `markInput()` | FR-CMN-09 |
| `onFunctionDown(TAB)` | `finishAll()`; `out.tab()`; `closeSwipe()`; `markInput()` | FR-TAP-04 |
| `onFunctionDown(ARROW_d)` | `finishAll()`; `resetParsers()`; `out.arrow(d)` (입력 아님) | FR-TAP-10, FR-CMN-03a |
| `onFunctionDown(ESC)`, `onFunctionDown(DELETE)`(0.22) | 방향키와 같음: `finishAll()`; `resetParsers()`; `out.editKey(ESCAPE / FORWARD_DEL)` (입력 아님). Del 반복은 반복 한 번 = 이 호출 한 번(LLD-05A 6절) | 이슈 #198 |
| `onFunctionDown(MODE)` | `onModeKey()`. 0.23부터 뷰는 모드 키를 뗄 때(탭이면) 부른다 | FR-CMN-03 |
| `onSettingsStroke()`(0.23, `TapSink`·`SwipeHost` 경유) | 모드 키 ↑: `finishAll()`(조합 중이면 확정, 공백 없음); `resetParsers(MODE_CHANGED)`(버린 획 = 진동); `onOpenSettings()`(서비스가 `SettingsActivity`를 새 작업으로 시작). 입력 아님(`markInput` 없음), 모드 바꾸지 않음. 0.18·0.21의 `onFunctionDown(SETTINGS)`·`onSettingsKey()`(조합 중 한 글자 완성만)는 삭제 | 이슈 #205 오너 결정 |
| `onFunctionUp(key)` | 입력 없음(뗌 알림. 반복 중지는 뷰가 함) | FR-CMN-11a |
| `backspace()` | 그래피티면 `graffiti.backspace()`(LLD-05 6절). 긋기면 먼저 `p = swipeState.popElement()`(0.18: 열린 모음 획 조립이나 천지인 조립에서 하나, LLD-04 6.2); `p is Popped`면 `r = if (p.undoComposer) composer.undoLast() else ComposeResult("", composer.composingText())`; `out.show(r, swipeState.pendingGlyph(), swipeState.pendingReplacesVowel())`로 끝. `p == Empty`면 `b = composer?.backspace()`(입력 단위 하나: 자음·모음이 바뀐 단계 하나, SWIPE 3.5); `Handled(r)` → `out.show(r)`, 그 밖 → `out.deleteBefore()`. 반복 타이머도 이 함수를 부른다 | FR-CMN-07, 07a, 07b, 07c |
| `onModeKey()` | `finishAll()`; `resetParsers()`; `next = modes.onModeKey()`; `if (!modes.inNumericField) saveMode(next)`(숫자류 입력창 안에서 바꾼 모드는 마지막 모드로 저장하지 않음, FR-CMN-04b·10, HLD 8.1); 서비스가 `showModeView(next)`. 그래피티로 새로 들어오면 `graffiti?.enterMode()`(→ `GraffitiState.enterMode()`, LLD-05 6절, G6) | FR-CMN-03d, 04b, HLD 8.1 |
| `onCursorMoved(newS, newE, candS, candE)` | `composer?.finish()` + `out.finishAfterUserMove(newS, newE, candS, candE)`(임시 글자가 있으면 빼고 확정, 사용자 커서 위치 유지, 4절); `resetParsers()` | FR-CMN-06, FR-HGL-04e, FR-SWP-37 |
| `onCursorMovedNoComposing()` | `resetParsers()` | FR-SWP-37 |
| `finishAll()` | `graffiti?.finishAll()` 또는 `composer?.finish()` + `out.finishComposing()` | FR-CMN-03d |
| `resetParsers(reason)` | `dropped = swipeState?.reset() == true`; `if (dropped && reason == MODE_CHANGED) feedback.error()`(모드 키(탭·↑)만 진동, 커서·입력창은 조용히, D-57); `graffiti?.reset()` | HLD 4.3, 5장 초기화 |
| `markInput()` | `modes.onInput()` | FR-CMN-03a |
| `error()` (그래피티 `InputSink`) | `feedback.error()`(인식 실패 오류 진동) | FR-GRF-08 |
| `hidesDisplay` (그래피티 `InputSink`, 0.21) | `field.hidesDisplay`(비밀번호만) | FR-CMN-08 ①, FR-GRF-07 |
| `field` (그래피티 `InputSink`) | 지금 `field`(그래피티 Enter → `out.enter(field)`, G2) | FR-CMN-09 |
| `snapshot()` (`GuardedState`) | `Snapshot(composer?.snapshot(), swipeState?.snapshot(), graffiti?.snapshot())` — 엔진 상태만. `ComposingController`는 넣지 않는다(LLD-09 2절 스냅숏 범위, 이슈 #75) | HLD 9장, NFR-03 |
| `restore(s)` (`GuardedState`) | 스냅숏에 든 것만 각 객체의 `restore`로 되돌림(null 칸은 건너뜀). 편집기·`ComposingController`는 건드리지 않는다(D-32) | HLD 9장 |

- 숫자류 입력창에서도 입력 분류(6.2)와 `markInput()`은 같다(SRS FR-CMN-03a "숫자류 입력창(FR-CMN-04)에서도 이 구분은 같다"). 0.1의 `keypadActive`(숫자 자판 입력은 typed를 바꾸지 않음)는 숫자 키패드가 없어져 없앴다.

### 6.2 "입력" 분류 (FR-CMN-03a)

| 동작 | `markInput()` |
|---|---|
| 글자·숫자·기호(탭 자판, 긋기 자판, 그래피티 글자·숫자·기호 획) | 예 |
| 그래피티 천지인 요소 획(ㅣ·ㅡ·탭, 모음이 성립하기 전의 ㆍ·‥ 포함) | 예(SRS FR-CMN-03a, DCR-17 결정) |
| 긋기 천지인 키 요소(0.16, ㆍ·‥ 대기 포함) | 예(`onJamoEvents`). 이어질 수 없어 무시된 요소는 오류 진동만이고 `markInput()` 없음(입력 없음, FR-SWP-62. 0.17부터 긋기 매퍼의 `Ignored`는 이것뿐) |
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
}
```

| 함수 | 처리 | 근거 |
|---|---|---|
| 시스템 막대 여백(0.7) | 창 인셋 `navigationBars() ∪ tappableElement()`(두 종류 중 **변마다 큰 값**)를 아래·왼쪽·오른쪽 padding으로 둔다(위는 0). `ViewCompat.setOnApplyWindowInsetsListener`로 받고 값이 바뀌면 padding을 고쳐 `requestLayout`, 붙을 때 `requestApplyInsets`. 바닥 = 시스템 막대 위. 가로 화면 3버튼 내비게이션처럼 막대가 옆이면 그 폭만큼 옆 여백이 생기고 자판은 남은 폭에 같은 배치로 놓인다 | SRS 3.3 "시스템 막대와의 관계", FR-CMN-12, HLD 5A, SWIPE 2장, 오너 결정 #93 OD-1·OD-3 A |
| 여백 색(0.7) | 루트 배경 = `BACKGROUND_COLOR`(키보드 배경색, 지금 검정). 모드 뷰도 같은 상수를 배경으로 쓴다(그래서 여백과 키 영역이 한 색으로 이어짐) | 오너 결정 #93 OD-2 A |
| `onMeasure` | 높이 = `6 × rowHeightDp`(쿼티·두벌식 6줄이 가장 높음, 기본 288dp) **+ 아래 padding**(시스템 막대 여백). 폭 = 부모. 자식 폭 = 폭 − 왼쪽 padding − 오른쪽 padding(안내표 버튼처럼 폭이 정해진 자식은 그 폭), 자식 높이 = 그 자식 `layoutParams.height`. `rowHeightDp`는 설정값(40~64dp, LLD-07 6.1) | SRS 3.3, HLD 5A, FR-SET-06 |
| `setModeView(v, rows)` | 모드 뷰 자식을 바꾼다(앞 모드 뷰만 빼고, 새 뷰는 맨 아래 z 순서 = 버튼·덮개 밑. 0.24부터 버튼·덮개는 남음). 자식 높이 = `rows × rowHeightDp`, `gravity = BOTTOM`. 긋기 자판 6줄(0.17, 쌍자음 줄), 그래피티 5줄, 쿼티·두벌식 6줄 | HLD 5A |
| `onTouchEvent` | 자식 밖(위 빈 띠와 시스템 막대 여백)의 터치는 `true`를 돌려 소비하고 아무 입력도 하지 않는다(빈 띠는 키가 아니므로 키 입력 피드백도 없음, FR-CMN-11a ②) | SRS 3.3 "빈 띠는 터치를 받되 입력은 없다" |
| 자식에게 주는 창 크기 | 바깥 변 계산(LLD-03 3.3)은 자식 좌표에서 창 왼쪽·오른쪽·아래가 바깥, 자식 위 끝은 바깥이 아님(빈 띠로 이어짐) | SWIPE 2.1 |

- 획이 시작된 뒤 손가락이 빈 띠로 넘어가도 이벤트는 그 획을 시작한 자식 뷰가 계속 받는다(안드로이드 터치 대상 유지). 그래서 맨 윗줄 키의 ↑ 나감은 보통 변 통과다. 0.17부터 긋기 자판은 6줄이라 빈 띠가 없고 숫자 줄 위는 창 밖(앱 화면)이다. 창 밖으로 나간 이동도 DOWN을 받은 뷰가 계속 받아야 한다(HLD 10장 R2, 에뮬레이터 확인 DV-26, LLD-11 8절). 빈 띠는 그래피티(5줄)에만 남는다.
- 모드 키는 모든 모드에서 맨 아랫줄 왼쪽이다(각 배치 데이터, SL-02·TL-03, 그래피티 보조 줄 LLD-05 8.1).
- 시험: 실기기 DV-07(높이·바닥 정렬·빈 띠 소비, 시스템 막대 여백 색·여백 터치 입력 없음 — 제스처·3버튼 내비게이션), DV-08(가로 화면, 옆 막대 여백). JVM 단위 시험은 없다(안드로이드 뷰, LLD-11 9절 G1).

### 7.1 그래피티 획 안내표 버튼·덮개 (0.24, 이슈 #189)

근거: SRS 0.45 FR-GRF-09·09a, FR-CMN-11a, HLD 0.41 5장 "소유·열기·닫기"·"배치와 터치", GRAFFITI 0.16 6.6-1~3·7. 덮개 그림·스크롤·✕는 LLD-05 10.2.

| 항목 | 처리 |
|---|---|
| 자식과 z 순서 | 아래부터 모드 뷰(`setModeView`가 index 0에 넣음) → 덮개 `GraffitiGuidePanel`(`MATCH_PARENT` × `rows × rowHeightDp`, `BOTTOM` = 모드 뷰와 같은 자리) → 버튼(`rowHeightDp` × `rowHeightDp`, `TOP|END` = 빈 띠 오른쪽 끝). 덮개·버튼은 `enableGuide`에서 한 번 만들고, 모드 뷰를 바꿔도 남는다. `relayout`이 세 자식의 크기를 새 줄 높이로 고친다 |
| `enableGuide(templates, feedback)` | 덮개(처음 한 번, `INVISIBLE`)와 버튼을 만들고 버튼을 `VISIBLE`로. 닫힌 덮개는 `INVISIBLE`이라 터치를 받지 않지만 배치는 되어 있어 연 즉시 제 크기다 |
| `disableGuide()` | `closeGuide()`, 버튼 `GONE`(그래피티를 떠남: 쿼티·두벌식·긋기, 숫자류 입력창의 쿼티 시작) |
| 버튼 터치 | 키다(빈 띠의 예외). `ACTION_DOWN`에서 `feedback.key()` 뒤 열려 있으면 `closeGuide()`, 아니면 `openGuide()`(누를 때 처리, FR-CMN-11a ①②). 그 뒤 이동·뗌은 아무 일 없음. 열린 동안 켜진 모양(밝은 배경·흰 "?"), 닫히면 흐린 모양. 접근성 이름 "획 안내표" |
| `openGuide()` | 버튼이 보일 때만. ① 모드 뷰에 합성 `ACTION_CANCEL`을 바로 보낸다(`GraffitiPadView`: 진행 중 획 버림 = 입력·오류 진동 없음, ⌫ 반복 멈춤, 떼지 않은 모드 키는 입력 없음, LLD-05 8.3 CANCEL). ② 덮개 `VISIBLE`. ③ `swallowGesture = true` |
| `closeGuide()` | 열려 있을 때만. 덮개 `INVISIBLE`, `rewind()`(맨 위, 따르는 포인터 없음), `swallowGesture = true`. 덮개를 빼지 않는다(✕를 뗄 때 덮개 자신의 터치 처리 안에서 불리므로) |
| `onInterceptTouchEvent` | `ACTION_DOWN`(새 제스처)이면 `swallowGesture = false`. `swallowGesture`면 `true`: 안내표를 열거나 닫은 **그 제스처의 나머지**는 안드로이드가 이미 잡은 자식들(모드 뷰·버튼·덮개)에 `ACTION_CANCEL`을 보내고 루트가 받아 버린다(`onTouchEvent` = 소비, 입력 없음). 그래서 열기 전에 패드를 누르고 있던 손가락이 덮개 아래 패드로 획을 마치지 못하고, 그 제스처 동안 새로 누른 손가락도 덮개·패드에 가지 않는다(모든 손가락을 뗀 뒤 다시 누르면 덮개가 받음) |
| 열린 동안 터치 | 덮개 자리(패드 + 보조 줄, 모드 키 포함)의 새 터치는 맨 위의 덮개가 받아 소비한다(LLD-05 10.2). 모드 뷰는 받지 않는다 → 패드 획·탭·보조 줄 키·모드 키(탭·↑ 설정) 모두 입력과 피드백이 없다(FR-GRF-09a). 빈 띠의 버튼만 그대로 받는다 |
| 상태 | 열림 = 덮개 `VISIBLE`. 뷰 상태일 뿐 `GraffitiState`·조합기·조립기에 아무것도 넘기지 않는다(HLD 5장). 새 루트(`onCreateInputView`, 화면 회전 등 설정 변경)는 닫힌 채 시작 |
| 닫히는 때 | 버튼, ✕(LLD-05 10.2), `disableGuide`(그래피티를 떠남), 서비스의 `onWindowHidden`·`onFinishInputView(false)`(키보드가 내려감, 3절). 입력창만 바뀌면(`onFinishInputView(true)` → `onStartInputView` → `showModeView(GRAFFITI)`: 새 패드 뷰가 덮개 밑에 들어감) 열린 채 |
| 창 크기 | 바꾸지 않는다(높이·인셋·`onComputeInsets` 그대로, 세로·가로 같음) |

- 시험: GD-01·GD-02·GD-06~GD-09(LLD-11 6.6, Robolectric). 실기기 DV-28(LLD-11 8절).

## 8. Feedback

```kotlin
interface FeedbackPort { fun key(); fun error() }        // 뷰와 InputDispatcher가 쓰는 경계
class Feedback(private val view: () -> View?, private val audio: AudioManager,
               private val settings: () -> Settings) : FeedbackPort
```

| 함수 | 처리 | 근거 |
|---|---|---|
| `key()` | `settings.haptic`이면 `view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)`; `settings.sound`이면 `audio.playSoundEffect(AudioManager.FX_KEY_CLICK)` | HLD 8장, FR-SET-03 |
| `error()` | `settings.haptic`이면 `performHapticFeedback(HapticFeedbackConstants.REJECT)`(API 30 미만은 `LONG_PRESS`) | FR-TAP-01b, FR-GRF-08 |

- **`key()`는 키를 누를 때 부른다**(SRS FR-CMN-11a ②, FR-SET-03, HLD 5A·8장, DCR-11 결정). 부르는 곳: `TapKeyboardView` DOWN(모든 키, LLD-05A 4절), `SwipeKeyboardView` DOWN(빈 칸 제외, LLD-04 9.3), `GraffitiPadView` 보조 줄 DOWN(LLD-05 8.3). 손을 뗄 때·결과가 나올 때·반복 때는 부르지 않는다.
- 키 입력 피드백을 내지 않는 곳(SRS FR-CMN-11a ②, HLD 8장 피드백, GRAFFITI 6.2-9, 설계질의 #6 Q1·Q3 확정): 그래피티 패드 획(키가 아님, 인식 실패만 `error()`), 긋기 자판 빈 칸·창 위 빈 띠(키가 아님), ⌫·방향키 반복(누를 때 한 번만).
- `error()`를 부르는 곳: 유니코드에 없는 윗첨자·아래첨자(FR-TAP-01b), 그래피티 인식 실패(FR-GRF-08).
- 기본값은 진동 켬·소리 끔이다(`Settings.haptic = true`, `sound = false`, LLD-07 6.1, DCR-14 결정).
- 시험: FB-01~FB-04.

## 9. 시퀀스

### 9.1 모드 키 (두벌식 "각" 조합 중, 입력함, 이전 모드 = 긋기)

```
TapKeyboardView DOWN(모드 키) → Feedback.key(); UP(탭, 0.23) → TapSink.onFunctionDown(MODE) → InputDispatcher.onModeKey
  → finishAll: HangulComposer.finish() → ComposingController.finishComposing → EditorPort.finishComposingText
  → resetParsers
  → ModeController.onModeKey: typed && previous(SWIPE) != current(DUBEOLSIK) → next = SWIPE, previous = DUBEOLSIK
  → inNumericField == false → saveMode(SWIPE) → SettingsStore.setLastMode
  → KeyboardService.showModeView(SWIPE) → SwipeModule.createSwipeView → KeyboardRootView.setModeView(…, 5)
     조합기 = HangulComposer(ComposePolicy.SWIPE), 긋기 뷰가 SwipeInputMapper를 만들어 `registerSwipeState`로 swipeState 등록(LLD-04 9.3)
```

### 9.2 입력창 시작 (그래피티로 쓰다가 숫자 비밀번호 입력창으로)

```
InputMethodService → KeyboardService.onStartInputView(info)
  → FieldInfo.from → (숫자 비밀번호 → fieldClass NUMBER, isPassword, numericLike)
  → InputDispatcher.finishAll, resetParsers
  → ModeController.onFieldChanged (typed = false)
  → ModeController.enterNumericField: returnMode = GRAFFITI, current = QWERTY (previous·마지막 모드 저장 그대로)
  → ErrorLog.enabled = false
  → ComposingController.onStart(initialSel)
  → showModeView(QWERTY, f) → TapKeyboardView(TapLayouts.QWERTY) (6줄, 숫자 줄 포함, 모드 키 있음)
```

### 9.3 숫자류 입력창을 떠남 (9.2에서 모드 키로 두벌식으로 바꾼 뒤 문자 입력창으로)

```
(9.2 상태) 모드 키 → InputDispatcher.onModeKey → ModeController.onModeKey: typed false → next = DUBEOLSIK
  → inNumericField == true → saveMode 부르지 않음(저장된 마지막 모드 = GRAFFITI 그대로)
  → showModeView(DUBEOLSIK)
문자 입력창 → onStartInputView → FieldInfo(numericLike = false)
  → finishAll, resetParsers, onFieldChanged
  → ModeController.leaveNumericField: current = GRAFFITI, returnMode = null
  → showModeView(GRAFFITI), GraffitiInputHandler.enterMode() → GraffitiState.enterMode() (KO, 캡스록 끔)
```

### 9.4 커서 이동 (긋기 "가" 조합 중 사용자가 화면을 눌러 커서를 옮김)

```
편집기 → KeyboardService.onUpdateSelection(…, newS=newE=3, candS=7, candE=8)
  → ComposingController.onUpdateSelection: 조합 중, newE != candE → USER_MOVED
  → InputDispatcher.onCursorMoved(newS, newE, candS, candE)
      → HangulComposer.finish(), ComposingController.finishComposing (finishComposingText)
      → SwipeInputMapper.reset()   (천지인 조립기 비움. 0.17: 다음 긋기는 어느 자음 키에서나 그 키의 음절)
```
