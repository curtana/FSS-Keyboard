# LLD-09 오류 처리

- 대응 HLD 절: 9장(오류 처리)
- 버전 **0.18** (2026-11-01)
- 0.18(이슈 #173): 긋기 스냅숏(`SwipeMapperSnapshot`)에 모음 획 조립기의 획 열(`strokes`)이 더해짐(천지인 요소 `cj`와 함께).
- 0.17(이슈 #168): 긋기 스냅숏(`SwipeMapperSnapshot`)은 천지인 조립기 요소(`cj`)뿐이다(기준 키·쌍자음 후보 없음).
- 0.16(이슈 #160): 긋기 스냅숏(`SwipeMapperSnapshot`)에 천지인 조립기 요소(`cj`)가 들어간다.
- 0.14(이슈 #150): 긋기 스냅숏의 엔진 상태가 `BlockParser`에서 `SwipeInputMapper`(기준 키·쌍자음 후보)로 바뀌었다.
- `ErrorLog`는 [LLD-07-tutor-store.md#62-errorlog](LLD-07-tutor-store.md#62-errorlog), `ThresholdStore.load`는 [LLD-07-tutor-store.md#4](LLD-07-tutor-store.md#4-thresholdstore)에 있다.
- 요구: NFR-03, NFR-05, FR-TUT-05.

## 1. 원칙

- 엔진 호출은 **획(동작) 단위**로 감싼다. 예외가 나면 그 획은 결과 없이 버리고, 조합·긋기 매퍼(천지인 조립기, 0.17)·조립 상태를 획 시작 전으로 되돌린다(HLD 9장).
- 편집기에 쓰는 일은 엔진 계산이 모두 끝난 뒤에 한다. 그래서 예외가 나면 편집기에는 아무것도 쓰이지 않는다.
- 오류 기록은 종류·코드 위치만(NFR-05, LLD-07 6.2).
- `InputConnection`이 없으면 쓰지 않는다(`AndroidEditorPort`, LLD-08 4절).

## 2. EngineGuard (app, 안드로이드 없음)

```kotlin
class EngineGuard(private val log: ErrorLog, private val state: GuardedState) {
    fun <T> run(site: String, block: () -> T): T?          // 예외면 null
    fun <T> stroke(site: String, block: () -> T): T?       // 동작 하나: 스냅숏 → 실행 → 실패 시 되돌림
}
interface GuardedState { fun snapshot(): Any; fun restore(s: Any) }   // InputDispatcher가 구현
```

#### `run(site, block)`
- 처리: `try { block() } catch (e: Exception) { log.record(site, e); null }`. `Error`(OOM 등)는 잡지 않는다(D-31).
- 쓰는 곳: 상태를 바꾸지 않는 계산 — 진행 중 판정(`StrokeClassifier.update`, null이면 그 획을 버림), 탭 자판 판정(`TapKeyResolver.resolve`, null이면 대기열 칸을 비움). 탐침(`ProgressState.of`)은 자기 안에서 탐침별로 예외를 버린다(LLD-04 5.2).

#### `stroke(site, block)`
```
s = state.snapshot()          // HangulComposer.snapshot(), SwipeEngineState.snapshot()(= SwipeInputMapper.snapshot(), LLD-03 2.2), GraffitiInputHandler.snapshot()(= CheonjiinAssembler·GraffitiState 값 복사, LLD-05 6절)
try   { return block() }      // block 안에서 편집기 쓰기는 마지막에만
catch (e: Exception) { state.restore(s); log.record(site, e); return null }
```
- 쓰는 곳(상태를 바꾸는 동작 하나마다): 긋기 손 뗄 때 처리 전체(`StrokeClassifier.finish` → `SwipeInputMapper.accept` → `HangulComposer.applyAll` → `ComposingController.show`, LLD-04 9.3), 탭 자판 출력 하나(`InputDispatcher.onTapOutput`, LLD-05A 4절), 그래피티 획 하나(`GraffitiInputHandler.onStroke` — 인식 포함, LLD-05 8.3), 기능키 누름과 반복 한 번(세 뷰 모두, LLD-05A 6절). `null`(실패)이면 그 동작은 입력 없이 끝난다.
- 되돌림은 각 클래스의 `snapshot`/`restore`(LLD-04 6.2·7.3, LLD-05 4·5절)를 쓴다. 조합기 상태는 입력 단위 목록 복사로 되돌린다(HLD 9장 "입력 단위 목록 이용").
- **스냅숏 범위(0.5, 이슈 #75):** 엔진 상태(조합기 입력 단위, 긋기 매퍼, 그래피티 조립기·상태)만 넣는다. `ComposingController`(기대 커서 `expected`·`predictedSel`, `composingShown`·`glyph`·`composeStart`)는 넣지 않는다. block 안에서 편집기 쓰기는 마지막이라 그 전에 실패하면 `ComposingController`는 바뀌지 않았고, `ComposingController`는 포트 호출이 돌아온 뒤에만 자기 상태를 바꾸기 때문이다(LLD-08 4절). 구현: `InputDispatcher.snapshot()`·`restore()`(LLD-08 6.1).
- **편집기 쓰기 도중 예외(D-32, 0.5에서 바꿈, 0.6에서 PR #91 동작에 맞춤):** 다른 예외와 똑같이 처리한다. 엔진 상태를 동작 시작 전으로 되돌리고 기록한다(HLD 9장 "조합 상태는 획 시작 전으로 되돌린다"). 조합기 강제 확정·표시 상태 비움 같은 별도 처리는 없고, 맡는 곳도 없다(`EngineGuard.stroke`의 되돌림뿐). `ComposingController`의 표시 상태(`composingShown`·`glyph`)는 돌아온 포트 호출까지만 바뀌고 나머지는 그 동작 전 값으로 남는다(LLD-08 4절). 임시 글자가 없으면 이것이 되돌린 조합기와 맞는다(EG-05). 임시 글자가 있는 `finishComposing`·`finishAfterUserMove`(그래피티)에서 `setComposingText(임시 글자 뺀 것)`은 돌아왔고 `finishComposingText`가 던졌다면, `composingShown`·`glyph`에서도 임시 글자가 이미 빠져 있어 편집기 조합 구간(임시 글자 없음)과 같다(0.6, PR #91 — 0.5까지는 `composingShown`에 임시 글자가 남는다고 적었다). 이때 조립기는 동작 전 상태(임시 글자 대기)로 되돌아가므로 그 사이 화면에만 임시 글자가 없고, 다음 `show`(`asm.pendingGlyph()`를 넘김, LLD-05 6절)가 다시 그린다. `show`·`finishAfterUserMove`에서 `beginBatch` 뒤에 던져도 `endBatch`는 `finally`로 불린다(0.6, PR #91 — 0.5까지는 불리지 않는다고 적었다. 이 경로가 생기는 조건은 아래). 던진 호출 전에 큐에 넣은 기대 커서 일부가 `expected`에 남을 수 있으나 다음 USER_MOVED·OTHER_MOVED 판정이나 `onStart`에서 비워진다(LLD-08 4.1). 시험: CC-20~CC-22.
  - 편집기가 그 쓰기를 실제로 반영한 뒤에 예외가 났다면 편집기와 조합기가 어긋날 수 있다. `InputConnection`은 실패를 반환값(`false`)으로 알리고 예외를 내지 않으므로(`AndroidEditorPort`는 반환값을 쓰지 않는다, LLD-08 4절) 이 경로는 우리 코드 결함에서만 생기며, 오류 기록으로 찾는다. 0.4의 "조합기 `finish()` + 표시 상태 비움"은 EG-05(되돌림)와 반대 결과였고 HLD 9장과도 맞지 않아 없앴다.
- 근거: NFR-03, HLD 9장. 시험: EG-01~EG-06, CC-20~CC-22.

## 3. 기준값 파일 손상

| 경우 | 동작 | 근거 |
|---|---|---|
| 파일 없음 | 기본값(`ThresholdDefaults`), `LoadStatus.DEFAULT_MISSING` | FR-TUT-05 |
| 해독 실패·`version` 모름·값 범위 밖 | 기본값, `DEFAULT_CORRUPT`, `ErrorLog.record("ThresholdStore.load", e)`. 손상 파일은 지우지 않고 그대로 둔다(다음 저장이 덮어씀) | HLD 9장 |
| 가져오기 파일이 잘못됨 | 저장하지 않고 `ImportResult.Invalid` | FR-TUT-07 |
| 저장 실패 | 이전 파일 유지(임시 파일 + 이름 바꾸기), 기록 | D-21 |

- `DEFAULT_*`면 기본값으로 동작하고 따로 안내하지 않는다(HLD 9장, SRS FR-TUT-05. DCR-10은 오너 결정으로 대체됨).

## 4. 그 밖의 실패

| 경우 | 동작 |
|---|---|
| 템플릿 파일 읽기 실패 | `TemplateSet`을 빈 집합으로. 규칙 판정(탭·직선·제어 획)은 동작하고 템플릿 획은 `Unknown` → 인식 실패(입력 없음 + 오류 진동, FR-GRF-08). 기록 |
| 설정 DataStore 읽기 실패 | 기본 `Settings`, 기록 |
| 배치 데이터 오류(폭 합이 360이 아님 등) | 개발 중 시험(TL-01, SL-01)으로 막는다. 실행 중 검사는 하지 않는다 |
| `onUpdateSelection` 값이 −1(조합 구간 사라짐) | USER_MOVED로 보고 조합기 확정(LLD-08 4.1) |
