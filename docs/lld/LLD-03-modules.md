# LLD-03 모듈 구조와 공통 타입

- 대응 HLD 절: 1장(설계 원칙), 2장(시스템 구조), 3장(모듈)(3.1 빌드 변형은 HLD 0.59에서 삭제, 이슈 #251)
- 버전 **0.31** (2026-11-14)
- 0.31(FSS#95, SRS 0.85·HLD 0.78: 긋기 입력 모드 삭제): `gesture-engine` 모듈(`kbd.gesture`), core-hangul `kbd.hangul.stroke`, app `kbd.app.swipe`·`kbd.app.swipe.impl`을 지웠다(1절, 1.2). 2절은 "빌드"만 남기고 2.2(`SwipeModule`·`SwipeHost`·`SwipeEngineState`)를 지웠다 — 앱은 탭 자판 뷰와 그래피티 패드를 직접 만든다(LLD-08 3.1). 3.1 `MergeTables.NONE`은 그래피티만, 3.4 `Thresholds`는 `minCrossDp` 한 칸만(`longDp`·`guideDelayMs`·`posTolDp`·`keySizeWDp`·`keySizeHDp` 삭제), 3.5 `InputMode { DUBEOLSIK, QWERTY, GRAFFITI }`(순환 순서 한글 두벌식 → 영문 쿼티 → 그래피티). 시험 MOD-02(긋기 경계)·MOD-04 삭제, MOD-03은 recognizer만 본다. 아래 0.30까지의 이력에 나오는 `Swipe…`·`gesture-engine`·`kbd.hangul.stroke`·LLD-04 4~6·8~10절·LLD-06은 이 판에서 없어진 것이다.
- 0.30(FSS#84, SRS 0.82 FR-CMN-14 ⑧, HLD 0.75 3장·5A): 3.3 바깥 변 — 테두리의 왼쪽·오른쪽은 자판 사각형의 바깥 변(자판에서 여유 칸만큼 밖)이라 끝 키의 왼쪽·오른쪽 변은 바깥 변이 아니다(`KeyGeom.build`의 `sideDp`, LLD-04 2절).
- 0.29(FSS#36, SWIPE 0.52 9.5, SRS 0.72 FR-DSP-10): `SwipeHost.centerText`(가운데 글자 = 지금 조합 중인 한 글자, 후보 표시 없음, 아직 모음이 아닌 점은 자리에, LLD-04 9.5), `SwipeEngineState.pendingDots()`(2.2). `centerConsonant`는 판정 C를 줄지 정하는 데만 쓴다.
- 0.28(FSS#17: 튜터 → 입력 연습, SRS 0.68·HLD 0.61·SWIPE 0.48): `kbd.app.tutor` → `kbd.app.practice`(LLD-07 2절), `ThresholdStore` 삭제. `SwipeModule.tutorSteps()` 삭제(연습은 같은 `createSwipeView`를 연습용 `SwipeHost`로 부름). 기준값은 고정 상수(3.4), `posTolDp`는 실행 때 키 폭 × 0.4(`ThresholdDefaults.posTolFor`, `SwipeInputMapper.setKeyWidth`).
- 0.27(이슈 #251 오너 결정: 공개판·개인판을 하나로, SRS 0.65·HLD 0.59 — CON-5·NFR-11·FR-CMN-03b·FR-TUT-08·HLD 3.1 삭제): 2절 제목을 "빌드와 SwipeModule"로. HLD가 `SwipeModule`을 LLD 표기로 따르므로 인터페이스 이름은 그대로 둔다. 빌드 변형(productFlavors) 삭제, 빌드 하나(`assembleDebug`, 앱 ID `kbd.app`), `gesture-engine`은 일반 `implementation`. `app/src/personal`·`public`·`testPersonal`·`testPublic`를 `main`·`test`로 합침. `SwipeModule`에서 `available`·`thresholdFields()`와 public `NoSwipeModule`을 지움, `PersonalSwipeModule` → `SwipeEngineModule`, `PersonalTutorSteps` → `TutorSteps`. 공개판 튜터 1-1(`MinCrossOnQwertyStep`, `QwertyPracticeEcho`, `PracticeLayout.QWERTY_LETTER_KEY`)과 긋기를 빼는 모드 순서도 지움. 아래 표·코드는 이 판 기준이고, 0.26까지의 변형 설명은 이 줄로 대신한다.
- 0.26(이슈 #242 오너 결정(두벌식 겹받침 한 번에)): `TapKeyDef.compoundJamo: Map<Dir, Char>`(두벌식 글자 키의 방향별 겹받침, 기본 빈 맵), core-hangul `JamoTables.SPLIT_JONG`(겹받침 → 두 자음). 겹받침 자모(`GS` 등)도 `JamoEvent.Consonant`로 들어올 수 있다(조합기가 나눔, LLD-04 7절).
- 0.25(이슈 #237): `KeyGeom.topAtWindow`(위 변이 창 위 끝과 같은 맨 윗줄 키, `build`가 정함)와 `windowEdges`(= `outerEdges` + 위). 플랫폼이 창 밖으로 나간 터치를 창 테두리에 붙여 보내므로 `EdgeCrossingTracker`가 테두리에 닿은 점을 그 변을 최소 통과 거리만큼 넘은 것으로 친다 — **네 테두리 모두**(바깥 변도 테두리에 닿으면 나감, 0.24까지의 "바깥 변은 기록 없음" 대체, LLD-04 3.2). `TapKeyResolver.edgeHints(key)`(그어서 나오는 글자 중 탭과 다른 것, 자판 표시용, LLD-05A 3.4).
- 0.24(이슈 #217 오너 결정): `minCrossDp` = **8dp 고정**(`ThresholdDefaults.MIN_CROSS_DP`, 안드로이드 touch slop 수준). 튜터가 재지 않고 저장 파일의 값도 읽지 않는다(LLD-07 2.2·4절). 제자리 탭 = 탭, 그보다 조금이라도 그은 획 = 획(길이와 상관없음).
- 0.23(이슈 #205): `FunctionKey.SETTINGS`(긋기 ⚙ 키, 0.18) 삭제. `SwipeHost.onSettingsStroke()`·`TapSink.onSettingsStroke()`(모드 키 ↑ = 설정, 기본 구현은 아무것도 하지 않음) 추가. 모드 키는 네 모드 모두 뗄 때 판정(stroke-core `ModeKeyGesture`, HLD 8.2, LLD-05A 4절).
- 0.22(이슈 #198): `FunctionKey.ESC`·`DELETE`(탭 자판 편집 줄 Esc·Del, LLD-05A 3.2).
- 0.21(이슈 #179·#180 후속, HLD 0.40): core-hangul `kbd.hangul.stroke`에 `ConsonantBox`(C), `VowelStroke.long` 삭제. `SwipeHost.centerConsonant`(2.2).
- 0.18(이슈 #173, HLD 0.37): core-hangul에 `kbd.hangul.stroke`(`VowelStrokeAssembler`, `VowelStroke`, `StrokeAxis`, `StrokePush`, `StrokePop`, `StrokeSnapshot`, LLD-04 6.3). `Thresholds`에서 `addIDp`·`returnLongDp`·`turnAngleDeg`를 지우고 `posTolDp`를 더함(형식 5). `FunctionKey.SETTINGS`(긋기 설정 키). `SwipeEngineState.reset(): Boolean`(버린 획 = 진동), `popElement(): StrokePop`, `cheonjiinState` 지움. `SwipeHost.onSwipeEvents(events, errorVibe)`. app main `kbd.app.swipe.StrokeTrail`, `kbd.app.view.TrailPainter`(획 자국). personal 튜터 단계에서 3-2 삭제.
- 0.17(이슈 #168, HLD 0.34 3장·7.2): `SwipeEngineState.onBackspace` 삭제(⌫ 뒤 기준 키 없음), `reset()`은 천지인 조립기만 비움. `Thresholds.doubleTapWindowMs` 삭제(시간 기준값 없음, 기준값 형식 4, LLD-07 4절). personal 튜터 단계에서 2-1 삭제, `thresholdFields()` 6칸. `JamoEvent.DoubleConsonant`·`UndoLast`는 긋기가 더 내지 않는다(형식은 남김).
- 0.16(이슈 #160, HLD 0.32 2·3장): `CheonjiinAssembler` 등을 recognizer `kbd.graffiti.cheonjiin`에서 core-hangul `kbd.hangul.cheonjiin`으로 옮김(그래피티·긋기 천지인 키 공용). gesture-engine·recognizer가 core-hangul에서 쓰는 패키지에 `kbd.hangul.cheonjiin` 추가. `SwipeEngineState`에 `pendingGlyph`·`cheonjiinState`·`popElement`. personal 튜터 단계에 3-4(천지인 키).
- 0.15(이슈 #156, SRS 0.35·HLD 0.31·SWIPE 0.26, TBD-18 = B): 긋기 병합 표 `MergeTables.SWIPE`를 지웠다. `ComposePolicy.SWIPE`는 `MergeTables.NONE`(병합 없음)을 쓴다. personal 튜터 단계에서 3-3 삭제. 이슈 #154: `TutorStep.itemSpec`(항목 판정, LLD-07 2.5), `RawStroke.key`.
- 0.14(이슈 #150): `BlockParser` → `SwipeInputMapper`(`kbd.gesture.input`), `SwipeHost.hasJong` 삭제, `SwipeEngineState.onBackspace` 추가, `JamoEvent.MergeVowel`·`RunStart` 삭제, 기준값 6칸 삭제(형식 3), personal 튜터 단계에서 4-1 삭제.
- 표기·함수 명세 형식: [README.md#2](README.md#2-공통-표기-규칙)

## 1. Gradle 모듈

| 모듈 | Gradle 플러그인 | 루트 패키지 | 변형 | 안드로이드 의존 |
|---|---|---|---|---|
| `core-hangul` | `org.jetbrains.kotlin.jvm` | `kbd.hangul` | 모두 | 없음 |
| `stroke-core` | `org.jetbrains.kotlin.jvm` | `kbd.stroke` | 모두 | 없음 |
| `recognizer` | `org.jetbrains.kotlin.jvm` | `kbd.graffiti` | 모두 | 없음 |
| `app` | `com.android.application`(AGP 내장 Kotlin) | `kbd.app` | 하나(0.27, 변형 없음) | 있음 |

- `settings.gradle.kts`: `include(":core-hangul", ":stroke-core", ":recognizer", ":app")`(0.31: `gesture-engine` 삭제).
- **AGP 내장 Kotlin:** AGP 9.x는 Kotlin 컴파일을 내장하고 `org.jetbrains.kotlin.android` 플러그인을 적용하면 빌드를 거부한다. 그래서 `app`에는 이 플러그인을 적용하지 않는다. Kotlin 컴파일러 플러그인(`org.jetbrains.kotlin.plugin.serialization`, `org.jetbrains.kotlin.plugin.compose`)은 그대로 적용한다(T01, PR #16, 이슈 #13).
- 빌드 스크립트는 Kotlin DSL(`*.gradle.kts`)로 쓴다.
- 패키지 루트 `kbd`는 임시 이름이다(앱 ID는 [README.md#5](README.md#5-lld에서-정한-세부) D-01).

### 1.1 의존 방향 (HLD 2장)

```
app ──► core-hangul, stroke-core, recognizer
recognizer ──► core-hangul
stroke-core ──► (없음)
core-hangul ──► (없음)
```

- `recognizer`가 `core-hangul`에서 쓰는 것은 `kbd.hangul.jamo`(자모 정의·병합 표 데이터), `kbd.hangul.event`(이벤트 형식), `kbd.hangul.cheonjiin`(천지인 조립, 0.16, 이슈 #160, HLD 3장) 세 패키지뿐이다. `kbd.hangul.compose`(조합 상태 기계)는 `app`만 쓴다. 이 경계는 시험 `MOD-03`(패키지 의존 검사)으로 지킨다.
- 병합 표 데이터(`kbd.hangul.jamo.MergeTables`)는 조합기 정책(`ComposePolicy`)이 쓴다. 병합 표는 상태가 없는 데이터라 정의 패키지에 둔다(DCR-15 결정).

### 1.2 패키지 목록

| 모듈 | 패키지 | 내용 | 명세 위치 |
|---|---|---|---|
| core-hangul | `kbd.hangul.jamo` | `Jamo`, `JamoTables`, `VowelMergeTable`, `MergeTables` | 이 문서 3.1, [LLD-04-stroke-hangul.md#7](LLD-04-stroke-hangul.md#7-hangulcomposer-core-hangul) |
| core-hangul | `kbd.hangul.event` | `JamoEvent`, `ComposeResult` | 이 문서 3.2 |
| core-hangul | `kbd.hangul.compose` | `HangulComposer`, `ComposePolicy`, `Syllable`, `ComposerSnapshot` | [LLD-04-stroke-hangul.md#7](LLD-04-stroke-hangul.md#7-hangulcomposer-core-hangul) |
| core-hangul | `kbd.hangul.cheonjiin` | `CheonjiinAssembler`, `CjElement`, `CjState`, `CjPush`, `CjPop`, `CjSnapshot`(0.16, 이슈 #160: recognizer에서 옮김) | [LLD-05-graffiti.md#4](LLD-05-graffiti.md#4-cheonjiinassembler) |
| stroke-core | `kbd.stroke.geom` | `Dir`, `Edge`, `Axis`, `RectDp`, `StrokePoint`, `KeyGeom`, `KeyHitTester` | 이 문서 3.3, [LLD-04-stroke-hangul.md#2](LLD-04-stroke-hangul.md#2-키-찾기-keyhittester-stroke-core) |
| stroke-core | `kbd.stroke.cross` | `EdgeCrossingTracker`, `Crossing`, `ZoneState` | [LLD-04-stroke-hangul.md#3](LLD-04-stroke-hangul.md#3-변-통과-추적-edgecrossingtracker-stroke-core) |
| stroke-core | `kbd.stroke.threshold` | `Thresholds`, `ThresholdDefaults` | 이 문서 3.4 |
| stroke-core | `kbd.stroke.tap` | `TapGesture`, `TapGestureClassifier`, `TapKeyResolver`, `ScriptTables` | [LLD-05A-tap-keyboard.md#2](LLD-05A-tap-keyboard.md#2-탭-자판-동작-표-판정-stroke-core) |
| stroke-core | `kbd.stroke.layout` | `TapLayout`, `TapKeyDef`, `KeyAssign`, `TapLayouts`(쿼티·두벌식 데이터), `FunctionKey` | [LLD-05A-tap-keyboard.md#3](LLD-05A-tap-keyboard.md#3-자판-배치-데이터) |
| recognizer | `kbd.graffiti.ink` | `InkPoint`, `InkStroke`, `PadArea`, `GraffitiLang` | [LLD-05-graffiti.md#2](LLD-05-graffiti.md#2-타입) |
| recognizer | `kbd.graffiti.recognize` | `Recognizer`, `StrokeRules`, `TemplateMatcher`, `TemplateSet`, `TemplateLoader`, `StrokeToken` | [LLD-05-graffiti.md#3](LLD-05-graffiti.md#3-recognizer) |
| recognizer | `kbd.graffiti.state` | `GraffitiState`, `GraffitiAction` | [LLD-05-graffiti.md#5](LLD-05-graffiti.md#5-graffitistate) |
| app | `kbd.app.ime` | `KeyboardService`, `FieldInfo`, `ModeController`, `InputMode`, `ComposingController`, `EditorPort`, `AndroidEditorPort`, `InputDispatcher` | [LLD-08-service-mode.md](LLD-08-service-mode.md) |
| app | `kbd.app.graffiti` | `GraffitiInputHandler`, `GraffitiPadView`, `InkCollector` | [LLD-05-graffiti.md#6](LLD-05-graffiti.md#6-graffitiinputhandler-app) |
| app | `kbd.app.view` | `KeyboardRootView`, `TapKeyboardView`, `TapKeyAccessHelper`, `MultiPointerQueue`, `RepeatTimer`, `RepeatSchedule` | [LLD-05A-tap-keyboard.md](LLD-05A-tap-keyboard.md), [LLD-08-service-mode.md#7](LLD-08-service-mode.md#7-keyboardrootview-창-높이-고정바닥-정렬) |
| app | `kbd.app.feedback` | `Feedback` | [LLD-08-service-mode.md#8](LLD-08-service-mode.md#8-feedback) |
| app | `kbd.app.store` | `SettingsStore`, `Settings`, `RowHeight`, `ErrorLog`(0.28: `ThresholdStore` 삭제) | [LLD-07-tutor-store.md](LLD-07-tutor-store.md) |
| app | `kbd.app.practice`(0.28) | `PracticeActivity`, `PracticeFlow`, `PracticeInput`, `PracticeItemJudge`, `PracticeTopics`, `PracticeLaunchPolicy` | [LLD-07-tutor-store.md#2](LLD-07-tutor-store.md#2-입력-연습-hld-71-srs-부록-a-fss17) |
| app | `kbd.app.settings` | `SettingsActivity` | [LLD-07-tutor-store.md#7](LLD-07-tutor-store.md#7-settingsactivity) |
| app | `kbd.app.error` | `EngineGuard` | [LLD-09-errors.md](LLD-09-errors.md) |

- **배치 데이터의 위치(D-25):** 탭 자판 배치(쿼티·두벌식)는 `stroke-core`의 `kbd.stroke.layout`에 둔다. 배치 검사 시험(TL)을 JVM에서 돌리려는 것이다. `stroke-core`는 다른 모듈에 의존하지 않으므로 탭 자판 데이터의 두벌식 자모는 `Char`(호환 자모)로 두고, 앱이 `Jamo.ofCompat(c)`로 바꾼다(LLD-05A 3.1).
- `FunctionKey`(기능키 종류)는 `stroke-core`(`kbd.stroke.layout`)에 둔다. 탭 자판 데이터와 그래피티 보조 줄(app)이 함께 쓴다.
- `app`의 안드로이드 없는 클래스(`ModeController`, `ComposingController`, `InputDispatcher`, `FieldInfo`, `GraffitiInputHandler`, `MultiPointerQueue`, `RepeatSchedule`, `PracticeFlow`, `PracticeItemJudge`, `PracticeLaunchPolicy`, `RowHeight`, `EngineGuard`)는 `android.*`를 import하지 않는다. 그래서 `app/src/test`의 JVM 단위 시험으로 시험한다.

## 2. 빌드

### 2.1 Gradle 구성

```kotlin
// app/build.gradle.kts (요점만)
android {
    // 0.27(이슈 #251): 빌드 변형 없음, 앱 ID kbd.app 하나
}
dependencies {
    implementation(project(":core-hangul"))
    implementation(project(":stroke-core"))
    implementation(project(":recognizer"))
}
```

| 소스 세트 | 경로 | 담는 것 |
|---|---|---|
| main | `app/src/main/kotlin` | 모든 코드(0.27) |
| test | `app/src/test/kotlin` | 모든 앱 시험(0.27: `testPersonal`·`testPublic` 합침) |

- 시험 `MOD-01`은 `assembleDebug`가 성공하는지 검사한다. 0.30까지의 `MOD-02`(app이 긋기 엔진을 `kbd.app.swipe.impl`에서만 import)는 긋기 엔진과 함께 없앴다(0.31).
- **시험 태스크 별칭:** 프로젝트 시험 명령은 `./gradlew testDebugUnitTest`다. JVM 모듈에는 이 이름의 태스크가 없어서 루트 빌드 스크립트에서 JVM 모듈마다 `tasks.register("testDebugUnitTest") { dependsOn("test") }`를 둔다(D-02). `app`은 변형이 없어 AGP가 만드는 `testDebugUnitTest`를 그대로 쓴다(0.27).

### 2.2 ~~SwipeModule 인터페이스~~ — 삭제 (0.31, FSS#95)

긋기 엔진을 앱의 나머지와 가르던 `SwipeModule`·`SwipeHost`·`SwipeEngineState`·`SwipeModuleProvider`는 긋기 입력 모드와 함께 없앴다. 모드 순서는 [LLD-08-service-mode.md#5](LLD-08-service-mode.md#5-modecontroller)다.

## 3. 모듈 사이 공통 타입

여기 정의는 이 문서에만 둔다. 다른 파일은 이 절을 참조한다.

### 3.1 자모 (`kbd.hangul.jamo`)

```kotlin
enum class JamoKind { CONSONANT, VOWEL }

enum class Jamo(val compat: Char, val kind: JamoKind,
                val choIndex: Int,   // 초성 인덱스 0..18, 없으면 -1
                val jungIndex: Int,  // 중성 인덱스 0..20, 없으면 -1
                val jongIndex: Int)  // 종성 인덱스 1..27, 없으면 -1
{ G('ㄱ',…), GG('ㄲ',…), … , A('ㅏ',…), … , I('ㅣ',…) }
```

- 자음 30개(초성 19 + 겹받침 11)와 모음 21개를 enum 값으로 둔다. 겹받침 값(`GS`=ㄳ 등)은 `kind = CONSONANT`, `choIndex = -1`이고 이벤트로는 쓰지 않는다(조합기 내부 표시용).
- 인덱스는 유니코드 한글 음절 공식(U+AC00 + (초성×21 + 중성)×28 + 종성)의 순서를 따른다.

| 표 (`JamoTables`) | 내용 |
|---|---|
| `DOUBLE_OF` | ㄱ→ㄲ, ㄷ→ㄸ, ㅂ→ㅃ, ㅅ→ㅆ, ㅈ→ㅉ (FR-HGL-05) |
| `JONG_CAPABLE` | 받침이 될 수 있는 자음: ㄸ ㅃ ㅉ를 뺀 16개(ㄲ ㅆ 포함, FR-HGL-05) |
| `COMPOUND_JONG` | (ㄱ,ㅅ)ㄳ (ㄴ,ㅈ)ㄵ (ㄴ,ㅎ)ㄶ (ㄹ,ㄱ)ㄺ (ㄹ,ㅁ)ㄻ (ㄹ,ㅂ)ㄼ (ㄹ,ㅅ)ㄽ (ㄹ,ㅌ)ㄾ (ㄹ,ㅍ)ㄿ (ㄹ,ㅎ)ㅀ (ㅂ,ㅅ)ㅄ (FR-HGL-02) |

```kotlin
fun interface VowelMergeTable { fun merge(prev: Jamo, next: Jamo): Jamo? }   // 없으면 null
object MergeTables {
    val DUBEOLSIK: VowelMergeTable   // FR-HGL-04a
    val NONE: VowelMergeTable        // 그래피티: 늘 null
}
```

| 표 | 앞 모음 + 다음 모음 = 결과 |
|---|---|
| `DUBEOLSIK` | ㅗ+ㅏ=ㅘ, ㅗ+ㅐ=ㅙ, ㅗ+ㅣ=ㅚ, ㅜ+ㅓ=ㅝ, ㅜ+ㅔ=ㅞ, ㅜ+ㅣ=ㅟ, ㅡ+ㅣ=ㅢ |
| `NONE` | 없음(그래피티) |

- 시험: HC-01, HC-02, HC-05(표 전수, HC-03·HC-04는 0.15에서 지움), [LLD-11-tests.md#2](LLD-11-tests.md#2-core-hangul).

### 3.2 이벤트 형식 (`kbd.hangul.event`)

```kotlin
sealed interface JamoEvent {
    data class Consonant(val c: Jamo) : JamoEvent        // c.kind == CONSONANT, 쌍자음도 이것으로 가능
    data class DoubleConsonant(val base: Jamo) : JamoEvent  // = Consonant(DOUBLE_OF[base]). 앱에는 내는 곳이 없다(형식만 남김, LLD-04 7.2)
    data class Vowel(val v: Jamo) : JamoEvent
    data class ReplaceVowel(val v: Jamo) : JamoEvent     // 천지인(그래피티): 지금 중성을 v로 바꿈. 자기 단위로 쌓임(0.16, LLD-04 7.1)
    data object UndoLast : JamoEvent                     // 조합 중 글자의 입력 단위 하나 되돌림(확정 경계를 넘지 않음, LLD-04 7.3)
}

data class ComposeResult(val committed: String,   // 이번 호출로 새로 확정된 글자(없으면 "")
                         val composing: String)   // 호출 뒤 조합 중 글자 전체(없으면 "")
```

- 근거: HLD 4.4(입력 `JamoEvent`, `ReplaceVowel`·`UndoLast`)·5장.
- 0.14(이슈 #150): 0.10의 `RunStart`(연속 입력 시작)와 `MergeVowel`을 지웠다. 모음 병합은 조합기 정책이 정한다(LLD-04 7.2).

### 3.3 기하 (`kbd.stroke.geom`)

모든 엔진 좌표·거리는 **dp**, 시각은 **ms(`Long`, `MotionEvent.getEventTime()` 기준)**다. 앱이 px를 dp로 바꿔 넘긴다(D-03).

```kotlin
enum class Dir { UP, DOWN, LEFT, RIGHT }               // 화면 기준, y는 아래로 증가
enum class Axis { HORIZONTAL, VERTICAL }
enum class Edge(val dir: Dir) { TOP(UP), BOTTOM(DOWN), LEFT(LEFT), RIGHT(RIGHT) }   // 변 = 그 변으로 나가는 방향
data class RectDp(val left: Float, val top: Float, val right: Float, val bottom: Float)
data class StrokePoint(val x: Float, val y: Float, val t: Long)
data class KeyGeom(val id: String, val rect: RectDp, val outerEdges: Set<Edge>)
```

- `Dir.axis`: LEFT·RIGHT = HORIZONTAL, UP·DOWN = VERTICAL. `Dir.opposite`, `Dir.unit`(UP = (0,−1) 등)을 확장 속성으로 둔다.
- **바깥 변(`outerEdges`):** 키의 변이 창의 왼쪽(x = 0), 오른쪽(x = 창 폭), 아래쪽(y = 창 높이) 경계와 겹치면 바깥 변이다. 창의 **위쪽 경계는 바깥 변이 아니다**(탭 자판은 6줄이 창을 채우고 위는 앱 화면으로 이어진다, SRS 3.3 창 높이 규칙). 빈칸과 맞닿은 변은 바깥 변이 아니다(SRS 3.3 들여쓰기 빈칸 문단). 계산은 배치 데이터를 만들 때 한 번 한다(`KeyGeom` 생성 함수, LLD-04 2절).
  - **0.30(FSS#84):** 위 문장의 "창"은 입력기 테두리이고, 그 왼쪽·오른쪽 변은 자판(키 10칸, 모드 뷰)의 끝이 아니라 **자판 사각형의 바깥 변**이다 — 모드 뷰 좌표로 x = −m과 x = 폭 + m(m = 여유 칸 폭, SRS FR-CMN-14 ⑧, LLD-08 7.2). 그래서 실제 자판에서는 끝 키의 왼쪽·오른쪽 변이 바깥 변이 아니고(변 너머 최소 통과 거리에서 나감, 다른 변과 같음), 바깥 변은 맨 아랫줄의 아래 변뿐이다. 맨 윗줄의 위 변(`topAtWindow`, 0.25)은 그대로다. m = 0으로 만들면(순수 함수 시험) 0.29와 같다.

### 3.4 기준값 (`kbd.stroke.threshold`)

```kotlin
data class Thresholds(val minCrossDp: Float)             // 0.31: 한 칸만(HLD 7.2)
object ThresholdDefaults { const val MIN_CROSS_DP = 8f; val VALUES: Thresholds }   // HLD 7.2, minCrossDp는 고정(0.24)
```

| 칸 | 값 | 판정에 쓰는 곳 |
|---|---|---|
| `minCrossDp` | **8, 고정**(0.24, 이슈 #217 오너 결정) | 탭 자판 키의 변 통과(LLD-04 3절, LLD-05A 2절), 모든 자판의 모드 키(LLD-05A 4절, 그래피티 보조 줄 포함) |

- 값은 **코드 상수**(`ThresholdDefaults.VALUES`)이고 측정·저장·다시 읽기가 없다(HLD 7.2, FSS#17 오너 결정 ①). 0.31(FSS#95): 긋기 자판·안내 팝업만 쓰던 칸(`longDp`, `guideDelayMs`, `posTolDp`, `keySizeWDp`·`keySizeHDp`)과 `ThresholdDefaults.posTolFor`를 지웠다.
- 거리 기준값은 dp 절댓값 그대로 쓰고 키 크기로 비례 변환하지 않는다(FR-CMN-12).

### 3.5 모드와 기능키

```kotlin
// kbd.app.ime
enum class InputMode { DUBEOLSIK, QWERTY, GRAFFITI }    // 선언 순서 = 순환 순서(FR-CMN-03 ②, 0.31: 한글 두벌식 → 영문 쿼티 → 그래피티)
// kbd.stroke.layout (stroke-core, 1.2절)
enum class FunctionKey { BACKSPACE, SPACE, ENTER, TAB, MODE, ARROW_LEFT, ARROW_UP, ARROW_DOWN, ARROW_RIGHT, ESC, DELETE }   // ESC·DELETE: 탭 편집 줄(0.22, 이슈 #198)
```

- `Jamo.ofCompat(c: Char): Jamo?` — 호환 자모 문자로 `Jamo`를 찾는다(`kbd.hangul.jamo`, 3.1). 탭 자판 출력의 `Char`를 앱이 바꿀 때 쓴다.

## 4. 필요 라이브러리 (승인 요청)

PROCESS_ROLES 10장에 따라 설치하지 않고 요청만 한다. 구현 착수 전 매니저 결정이 필요하다.

| 라이브러리 | 쓰는 곳 | 근거 | 라이선스 |
|---|---|---|---|
| Kotlin 표준 라이브러리, Kotlin Gradle 플러그인 | 전체 | CON-1 | Apache 2.0 |
| Android Gradle Plugin, AndroidX Core | app | CON-1 | Apache 2.0 |
| AndroidX DataStore Preferences | `SettingsStore` | HLD 7.3 | Apache 2.0 |
| Jetpack Compose(UI, Material3), Activity Compose | `SettingsActivity`, `PracticeActivity` | HLD 2장, 12장 | Apache 2.0 |
| AndroidX CustomView (`ExploreByTouchHelper`) | `TapKeyAccessHelper` | HLD 5A, NFR-07 | Apache 2.0 |
| kotlinx-serialization-json | 기준값 JSON, 템플릿 JSON, 시험 궤적 JSON(JVM 모듈에서도 써야 해서 `org.json` 불가) | HLD 7.3, 5장 | Apache 2.0 |
| JUnit 4.13 | 모든 모듈 단위 시험 | NFR-08 | EPL 1.0 |
| Robolectric(시험 전용, `testImplementation`) — **승인됨(G1, 이슈 #12)** | 안드로이드 클래스가 대상인 단위 시험(LLD-11 9절) | NFR-08 | MIT |

- Robolectric(G1)은 PM이 승인했다(이슈 #12). LLD-11 9절의 시험은 app 모듈 JVM 단위 시험으로 있다(작업지시서 T36, PR #120).
- kotlinx-serialization이 승인되지 않으면 세 JSON 형식(LLD-05 7절, LLD-07 5절, LLD-11 1절)을 읽는 작은 파서를 직접 쓴다. 형식은 바뀌지 않는다.
