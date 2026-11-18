# LLD-04 방향 긋기 판정과 한글 조합

- 대응 HLD 절: 4장(4.2 변 통과 판정, 4.4 HangulComposer)
- 버전 **0.40** (2026-11-18)
- 0.40(FSS#129, SRS 0.99 FR-HGL-04a·04f ②, HLD 0.92 4.4): 7.2 전이 규칙은 그대로이고 병합 표 데이터만 넓어졌다(LLD-03 3.1 열일곱 줄). 병합한 중성도 다음 `Vowel`과 같은 `merge`로 본다(접을 때 fold가 이미 그렇게 함 — ㅣ ㅏ ㅣ = ㅒ). 단위는 키 하나씩이라 ⌫ = 키 하나(걔 → 갸 → 기). 7.4의 "(ㅘ, ㅣ)를 더하지 않는다" 근거를 고쳤다 — 이제 과 + ㅣ = 괘(키로 친 것과 같음), 획 단계는 여전히 `ReplaceVowel`(D-77 그대로)
- 0.39(FSS#110 후속 오너 결정 "괘는 지우면 고가 되어야해", SRS 0.93 FR-HGL-04f ③·TBD-39 ②, HLD 0.86 4.4): 7.4 — `VOWEL_STEPS`의 ㅙ·ㅞ를 두 단계로(ㅙ [ㅗ, ㅙ], ㅞ [ㅜ, ㅞ]). 괘 → 고 → ㄱ, 궤 → 구 → ㄱ로 키로 친 괘·궤와 단계마다 같다. 코드 형식·`ReplaceVowel` 쌓기(D-77)는 그대로이고 표 두 줄만 바뀐다
- 0.38(FSS#110 오너 결정 ⌫ "한 단계씩", SRS 0.92 FR-HGL-04f·FR-CMN-07·TBD-39 ②③, HLD 0.85 4.4 "겹모음 자모 한 번에"): 7절 — 두벌식 정책에서 겹모음 `Vowel` 이벤트(ㅘ ㅙ ㅚ ㅝ ㅞ ㅟ ㅢ — 한 번 긋기로 온 것)를 **획 단계 단위**로 쌓는다: 첫 단계 `Vowel`, 이어지는 단계마다 `ReplaceVowel`(ㅙ = `Vowel(ㅗ)`, `ReplaceVowel(ㅘ)`, `ReplaceVowel(ㅙ)`). 단계 표 `JamoTables.VOWEL_STEPS`(LLD-03 3.1), 정책 칸 `ComposePolicy.splitCompoundVowel`(두벌식 참, 그래피티 거짓), `StepOut.then`을 목록으로(겹받침 두 자음도 같은 칸). 새 7.4. 보이는 글자·상태 전이·다음 모음 병합은 그대로이고 `MergeTables.DUBEOLSIK`도 그대로다((ㅘ, ㅣ)·(ㅝ, ㅣ)를 더하지 않음). 그래피티 정책·`ReplaceVowel` 단계·`backspace`·`undoLast`·`snapshot`·`restore`는 코드 그대로다(D-77). 시험 HC-70~HC-75. 7.2의 `ReplaceVowel` 줄에 0.16 이전 문구("모음 단위를 `Vowel(v)` 하나로 바꾼다")가 남아 있던 것을 7.1과 맞게 고쳤다(동작 변화 없음)
- 0.37(FSS#105, SRS 0.89 FR-TAP-03d, HLD 0.82 5A): 2절 `KeyHitTester` 근거 문구만 고쳤다 — 탭 자판에 들여쓰기 빈칸이 없어져(LLD-05A 0.21 3.2) 탭 자판 안의 점은 모두 처리 1(사각형 포함)로 정해진다. 처리 2(가장 가까운 키)는 그대로 둔다(키 영역 밖 좌표·부동소수 경계용). 함수 동작은 바뀌지 않았다.
- 0.36(FSS#95, SRS 0.86·HLD 0.79: 긋기 입력 모드 삭제): 파일 이름을 `LLD-04-swipe.md`에서 바꾸고, 탭 자판·모드 키·그래피티가 쓰는 **2절(키 찾기)·3절(변 통과 추적)·7절(조합기)만 남긴다. 절 번호는 그대로**라 다른 문서·코드의 "LLD-04 §2·§3·§7" 참조는 그대로 맞다. 4절(`StrokeClassifier`)·5절(`ProgressState`)·6절(`SwipeInputMapper`·`VowelStrokeAssembler`)·8절(긋기 자판 배치 데이터)·9절(긋기 자판 뷰·획 자국·가운데 글자)·10절(시퀀스)은 그 코드(`gesture-engine` 모듈, `kbd.app.swipe`, `kbd.hangul.stroke`)와 함께 지웠다(번호 재사용 없음). 7절: `ComposePolicy.SWIPE`와 `lastConsonant()`(쓰는 곳이 긋기 자판뿐)를 지웠다(정책은 두벌식·그래피티 둘, 두 정책의 표·동작은 그대로). `JamoEvent.DoubleConsonant`·`UndoLast`와 `EdgeCrossingTracker.copy()`·`nthCrossingOf`는 모듈의 공용 형식으로 남긴다. 0.35까지의 긋기 쪽 변경 이력은 git 이력에 있다.
- 0.35(FSS#84, SRS 0.82 FR-CMN-14 ⑧·FR-SWP-45, HLD 0.75 4.2·5A): `KeyGeom.build`에 `sideDp`(테두리의 왼쪽·오른쪽이 자판에서 여유 칸만큼 밖, 2절). 양 끝 열의 바깥쪽 획은 보통 변 통과다. `EdgeCrossingTracker`는 그대로.
- 0.29(이슈 #242 오너 결정(두벌식 겹받침 한 번에), 조합기 7절): `HangulComposer`가 겹받침 자모 하나(`Consonant(ㄺ)` 등)를 받는다 — 초성·중성만 있는 글자면 그 받침으로 두 자음 단위(`Consonant(ㄹ)`, `Consonant(ㄱ)`)를 저장해 두 번 치기와 같다(갉 + ㅏ = 갈가, ⌫ = 갈 — 둘째 자음만, PM 채택 2026-11-02; 낱자 ㄺ ⌫ = 통째). 그 밖(조합 없음, 받침 있음, 초성만)은 지금 글자를 확정하고 겹받침 낱자(ㄺ)로 선다. 낱자 뒤 모음은 둘째 자음이 가져간다(ㄺ + ㅏ = ㄹ가).
- 0.28(이슈 #237 후속, SRS 0.60 FR-SWP-45 설계 결정): 테두리 닿음은 **그 방향으로 누른 점에서 `minCrossDp` 이상 움직였을 때만** 센다(3.2, `EdgeCrossingTracker.start`) — 테두리 근처 탭·흔들림, 반대쪽으로 긋다 옆 테두리를 스친 대각선은 나감이 아니다.
- 0.27(이슈 #237): 맨 윗줄 키(`KeyGeom.topAtWindow`)에서 창 위 끝(y ≤ 키 위 + 0.5dp)에 닿은 점은 위 변을 `minCrossDp`만큼 넘은 것으로 친다(3.2). 돌아옴은 실제 점으로.
- 공통 타입(`Jamo`, `JamoEvent`, `ComposeResult`, `Dir`, `Edge`, `RectDp`, `StrokePoint`, `KeyGeom`, `Thresholds`)은 [LLD-03-modules.md#3](LLD-03-modules.md#3-모듈-사이-공통-타입)에 있다.
- 규칙의 정본은 SRS 3.4(방향 긋기 판정 공통, FR-SWP-01~05·44·45)와 SRS 3.2(한글 조합, FR-HGL)다. 이 문서는 그 규칙을 함수로 옮긴다.

## 1. 범위

| 절 | 구성 요소 | 모듈 |
|---|---|---|
| 2 | `KeyHitTester` (키 찾기, 탭 자판) | stroke-core |
| 3 | `EdgeCrossingTracker` (변 통과 기록, 탭 자판·모드 키) | stroke-core |
| 4~6 | 삭제(FSS#95) | – |
| 7 | `HangulComposer` (조합기, 모드 정책 2종) | core-hangul |
| 8~10 | 삭제(FSS#95) | – |

## 2. 키 찾기 (KeyHitTester, stroke-core)

```kotlin
class KeyHitTester(val keys: List<KeyGeom>, val windowW: Float, val windowH: Float)
```

#### `KeyHitTester.hit(x: Float, y: Float): KeyGeom?`
- 입력: 누른 점(dp, 입력 뷰 좌표). 출력: 키, 없으면 null.
- 처리:
  1. `rect`가 점을 포함하는 키(왼쪽·위 변 포함, 오른쪽·아래 변 제외)가 있으면 그 키.
  2. 없으면 점이 어느 키에도 속하지 않는 틈이다. 점과 각 키 사각형 사이 거리(사각형 밖 최단 거리)가 가장 작은 키를 고른다. 같으면 목록 앞 키.
  3. 단, 뷰가 "입력 없음 영역"으로 둔 곳(자판의 좌우 여유 칸에서 시작한 터치, LLD-08 7.2)은 이 함수를 부르기 전에 뷰가 걸러 낸다.
- 오류: 키 목록이 비면 null.
- 근거: HLD 5A("이웃 틈은 가까운 키로"), SRS 3.3. 0.37(FSS#105): 탭 자판은 빈칸이 없어 모든 줄이 키로 차 있으므로(두 줄 높이 ↵는 사각형 하나) 뷰 안의 점은 처리 1로 정해진다. 0.36까지의 "들여쓰기 빈칸은 가장 가까운 키"는 뷰가 빈칸 사각형을 먼저 보던 규칙이었고 LLD-05A 0.21에서 지웠다. 시험: KH-01~KH-04, BE-01.

#### `KeyGeom.build(id, rect, windowW, windowH, sideDp = 0): KeyGeom` (companion)
- `windowW`·`windowH` = 키 영역(모드 뷰)의 폭·높이. `sideDp`(0.35, FSS#84) = 테두리의 왼쪽·오른쪽이 키 영역보다 밖에 있는 거리, 곧 자판의 여유 칸 폭(LLD-08 7.2). 테두리의 왼쪽 = −`sideDp`, 오른쪽 = `windowW + sideDp`.
- `outerEdges` = { LEFT if `rect.left ≤ −sideDp + 0.5`, RIGHT if `rect.right ≥ windowW + sideDp − 0.5`, BOTTOM if `rect.bottom ≥ windowH − 0.5` }. 0.5dp는 부동소수 오차 여유다. TOP은 넣지 않는다(LLD-03 3.3). `topAtWindow`는 그대로(`rect.top ≤ 0.5`).
- 실제 자판은 늘 `sideDp` = 여유 칸 폭(> 0.5dp)으로 부르므로 끝 키(`rect.left` = 0, `rect.right` = `windowW`)의 LEFT·RIGHT가 들지 않는다. `sideDp` = 0은 0.34와 같은 결과이고 순수 함수 시험(KH-05)이 쓴다.
- 시험: KH-05(쿼티 1·0·q·p·⌫·↵·맨 아랫줄) — `sideDp` 0. KH-07(`sideDp` 8: 끝 키 LEFT·RIGHT 없음, 아래·위 그대로), SM-02·SM-04(실제 기하).

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
- ~~**바깥 변 제외(FR-SWP-45)**~~(0.27, 이슈 #237로 대체): 0.26까지는 바깥 변 쪽 OUT 구역으로 전이하지 않아 바깥으로만 나간 획은 기록이 없었다. 이제 바깥 변도 아래 "창 테두리에 붙은 점"으로 나감이 된다. 그 방향에 배정이 없으면 결과는 탭과 같다(모드 키 ←·↓ = 모드 전환. 새 기호는 배정하지 않음).
- **여유 칸 쪽 변(0.35, FSS#84):** 자판 왼쪽·오른쪽 끝 키의 바깥쪽 변은 테두리에 붙은 변이 아니다(2절 `sideDp`). 그래서 위 표의 X 전이 그대로, 실제 점이 `rect.left − m`·`rect.right + m`에 닿을 때 나감이다 — 아래 "테두리에 붙은 점" 규칙(닿으면 나감, 누른 점에서 `minCrossDp` 이동 조건)은 이 변에 걸리지 않는다. 손가락은 여유 칸(모드 뷰 밖, x < 0 또는 x > 폭) 위에 있고 그 좌표가 모드 뷰에 그대로 온다(LLD-08 7.2). 시험 EC-16, SM-03·SM-04.
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
    fun copy(): EdgeCrossingTracker             // 지금까지의 기록을 가진 복사본(획 도중 상태를 따로 이어 볼 때)
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
- `countOf(edge): Int` — 그 변의 OUT·IN 기록 수(같은 변 통과 횟수, SRS 1.3 변 통과 용어).
- `nthCrossingOf(edge, n): Crossing?` — 그 변의 n번째 기록.

## 4. ~~StrokeClassifier~~ — 삭제 (FSS#95)

## 5. ~~ProgressState~~ — 삭제 (FSS#95)

## 6. ~~SwipeInputMapper·VowelStrokeAssembler~~ — 삭제 (FSS#95)

긋기 자판의 판정·안내용 진행 상태·탭과 획을 자모로 바꾸는 부분은 긋기 입력 모드와 함께 없앴다(HLD 4.3). 천지인 조립(`CheonjiinAssembler`)은 LLD-05 4절이다.

## 7. HangulComposer (core-hangul)

### 7.1 타입

```kotlin
data class ComposePolicy(val name: String, val merge: VowelMergeTable, val mergeConsecutiveVowel: Boolean,
                         val splitCompoundVowel: Boolean = false) {   // 0.38: 겹모음 Vowel 이벤트를 획 단계로 쌓음(7.4)
    companion object {
        val DUBEOLSIK = ComposePolicy("dubeolsik", MergeTables.DUBEOLSIK, true, splitCompoundVowel = true)
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
- `ReplaceVowel`은 **자기 자신을 단위로 쌓는다**(0.16, 이슈 #160, SRS FR-HGL-04e, HLD 4.4 "마지막 입력 단위(자모 하나 또는 천지인 요소 하나) 되돌림"). 접을 때 그 시점 글자의 중성을 v로 바꾸므로(`step` 그대로) 병합된 중성(과)에서도 확정이 일어나지 않아 불변식을 지킨다. 그래서 천지인 요소(ㅣ = `Vowel`, 이어지는 요소마다 `ReplaceVowel`)가 하나씩 단위가 되고, 조립기가 닫힌 뒤의 ⌫도 요소 하나씩이다(간 ⌫ = 가, ⌫ = 기, ⌫ = ㄱ). 0.15까지의 "모든 모음 단위를 `Vowel(v)` 하나로 바꿈"(G8)은 지웠다. 조립기가 열려 있을 때의 ⌫는 조립기가 먼저 빼고 조합기는 `undoLast`로 따라간다(LLD-05 6절).
- `UndoLast`는 단위로 쌓지 않는다.
- **0.14에서 지운 것(이슈 #150):** 연속 입력 run(`RunStart`, `base`·`events`·`pending`, `RunSnapshot`), `MergeVowel`(모음 병합은 `mergeConsecutiveVowel` + 정책 병합 표가 정함), `BackspaceMode`(`WHOLE_SYLLABLE`이 없어져 모든 정책이 입력 단위 하나씩 지운다). 확정된 글자를 보관하거나 확정 경계를 넘어 되돌릴 일이 없다(HLD 4.4).

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

- 그래피티 정책은 같은 표를 쓰고 모음 병합만 없다(`MergeTables.NONE`, `mergeConsecutiveVowel` = false): `Vowel` 뒤 `Vowel`은 늘 "확정, 새 중성만 v"다. 그래피티의 모음은 천지인 조립 결과라 첫 요소가 `Vowel`, 이어지는 요소가 `ReplaceVowel`로 온다(LLD-05 4절). 받침 뒤 모음은 도깨비불이다(각 + ㅣ = 가기, 닭 + ㅣ = 달기).
- 쌍자음은 `Consonant(ㄲ)`처럼 자음 이벤트 하나로 온다(두벌식 ↑, 그래피티 쌍자음 획). 쌍받침 ㄲ·ㅆ은 받침이 되고, ㄸ·ㅃ·ㅉ(받침 불가)은 앞 글자를 확정하고 새 초성이 된다(아ㅃ). 받침 ㄱ 뒤 `Consonant(ㄱ)`은 겹받침이 아니라(COMPOUND_JONG에 없음) 확정, 새 초성이다(곡 + ㄱ = 곡ㄱ).
- `DoubleConsonant(b)` = `Consonant(DOUBLE_OF[b])`로 처리하고 단위에는 그대로 쌓는다. `DOUBLE_OF[b]`가 없으면 무시하고 오류를 보고한다(D-09, G9). 앱에는 이 이벤트와 `UndoLast` 이벤트를 내는 곳이 없다(core-hangul 이벤트 형식으로 남김, HC 시험 그대로. 그래피티는 `undoLast()` 함수를 직접 부른다).
- `ReplaceVowel(v)`: 중성이 있고 받침이 없으면 중성을 v로 바꾸고 자기 자신을 단위로 쌓는다(7.1, 0.16). 병합 표는 보지 않는다. 아니면 무시하고 오류 기록(D-09). 두벌식에서는 앱이 이 이벤트를 보내지 않고 조합기가 겹모음의 획 단계를 쌓을 때만 생긴다(7.4).
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
- 근거: HLD 4.4 `UndoLast`. 쓰는 곳: 그래피티 ⌫(천지인 요소 하나 되돌림, LLD-05 6절). 시험: HC-50, HC-51, HC-53.

#### `backspace(): BackspaceResult`
- `units`가 비면 `NothingComposing`. 아니면 마지막 단위를 빼고 `Handled(ComposeResult("", render(fold(units))))`(모든 정책).
- 확정된 글자는 다시 열지 않는다(목록이 없으므로, FR-CMN-07). 도깨비불 뒤의 단위는 `[Consonant(j), Vowel(v)]`라 가기 ⌫ = 가ㄱ(두벌식과 같음).
- 근거: FR-CMN-07, 07a, HLD 4.4. 시험: BS-01~BS-05, BS-20~BS-22.


#### `finish(): String`
- 조합 텍스트를 돌려주고 `units`를 비운다. 앱이 `finishComposingText`와 함께 부른다.

#### 조회·복구
- `composingText(): String`, `isComposing(): Boolean`(`units`가 있음), `hasJong(): Boolean`(지금 상태에 받침이 있음, 그래피티가 씀), `currentVowel(): Jamo?`.
- `snapshot()` / `restore(s)`: `units` 복사(LLD-09). 시험: HC-53.

- 앱에는 조합기가 하나다. 모드가 바뀌면 확정 뒤 새 정책으로 다시 만든다(HLD 2장 "조합 상태는 앱이 가진 조합기 하나", D-10).

### 7.4 겹모음 자모 한 번에 — 획 단계 단위 (0.38, 0.39, SRS FR-HGL-04f, HLD 4.4, FSS#110)

두벌식 탭 자판은 ㅗ·ㅜ·ㅡ 키를 덧붙는 획의 방향으로 한 번 그으면 겹모음 자모 하나(`Vowel(ㅘ)` 등)를 보낸다(LLD-05A 3.3 겹모음 배정표, SRS FR-TAP-02g). **모음은 덧붙는 획이 뻗는 방향, 자음 겹받침은 뒤 자음 키의 위치**가 배정 원리이고(LLD-05A 3.3), ⌫는 같은 글자를 키로 친 것과 같은 단계로 하나씩 되돌린다(오너 결정 "한 단계씩", "괘는 지우면 고가 되어야해").

```kotlin
// JamoTables (LLD-03 3.1)
val VOWEL_STEPS: Map<Jamo, List<Jamo>>   // ㅘ [ㅗ, ㅘ], ㅙ [ㅗ, ㅙ], ㅚ [ㅗ, ㅚ], ㅝ [ㅜ, ㅝ], ㅞ [ㅜ, ㅞ], ㅟ [ㅜ, ㅟ], ㅢ [ㅡ, ㅢ]
// HangulComposer (private)
private fun vowelUnits(v: Jamo, unit: JamoEvent): List<JamoEvent>
private class StepOut(…, val then: List<JamoEvent> = emptyList())   // 0.38: 0.37까지 JamoEvent?(겹받침 둘째 자음)
```

- **`vowelUnits(v, unit)`:** `policy.splitCompoundVowel`이고 `VOWEL_STEPS[v]`가 있으면 `[Vowel(단계 1), ReplaceVowel(단계 2), …]`, 아니면 `[unit]`(지금까지와 같음). 단계는 SRS FR-HGL-04f ③과 같다 — 모두 두 단계(앞 모음, 그 겹모음)로 같은 글자를 키로 친 단위와 수가 같다(0.39: ㅙ·ㅞ도 ㅗ·ㅜ 다음 바로 그 겹모음, 오너 결정 "괘는 지우면 고가 되어야해").
- **`vowel(s, v, unit)`이 쌓는 것**(상태 전이 `next`·`commit`은 0.37과 같음 — 7.2 표):

| 지금 상태 | 0.37까지 단위 | 0.38 단위(겹모음, 두벌식) | 보이는 글자 |
|---|---|---|---|
| 빔 | `[Vowel(ㅘ)]` | `[Vowel ㅗ, Replace ㅘ]` | ㅘ |
| 초성만(ㄱ) | `+ Vowel(ㅘ)` | `+ Vowel ㅗ, Replace ㅘ` | 과 |
| 낱자 겹받침(ㄺ) | 확정 ㄹ, `[ㄱ, Vowel ㅘ]` | 확정 ㄹ, `[ㄱ, Vowel ㅗ, Replace ㅘ]` | ㄹ과 |
| 중성 있음·받침 없음(고), 병합 없음 | 확정 고, `[Vowel ㅘ]` | 확정 고, `[Vowel ㅗ, Replace ㅘ]` | 고ㅘ |
| 받침 하나(각) | 확정 가, `[ㄱ, Vowel ㅘ]` | 확정 가, `[ㄱ, Vowel ㅗ, Replace ㅘ]` | 가과 |
| 겹받침(갉) | 확정 갈, `[ㄱ, Vowel ㅘ]` | 확정 갈, `[ㄱ, Vowel ㅗ, Replace ㅘ]` | 갈과 |

- 병합이 일어나는 칸(중성 u + v, merge(u, v) = m)은 겹모음 v에서 생기지 않는다(두벌식 표의 둘째 모음은 모두 홑모음). 생겨도 단위는 `unit` 그대로다.
- **왜 `ReplaceVowel`인가(D-77):** 이어지는 단계를 `Vowel`로 쌓으면 접을 때 병합 표를 보므로 (ㅗ, ㅘ)·(ㅗ, ㅙ)가 표에 없어 확정이 일어나(고ㅘ) 불변식(접을 때 확정 없음, 7.1)이 깨진다. (ㅗ, ㅘ)·(ㅗ, ㅙ) 같은 줄은 키로 친 ㅗ ㅘ가 없으므로 표에 둘 까닭이 없다. 0.40(FSS#129)에 더한 (ㅘ, ㅣ) = ㅙ · (ㅝ, ㅣ) = ㅞ는 단계가 아니라 **다음 키**의 병합이라 D-77과 겹치지 않는다(긋기 ㅙ는 여전히 [ㅗ, ㅙ] 두 단계, ㅗ ㅏ ㅣ 세 단위로 나누지 않음 — SRS FR-HGL-04f ④). `ReplaceVowel`은 중성을 표 없이 바꾸고 자기 자신이 단위라(7.1, 0.16) 새 이벤트 없이 단계를 나타낸다. 단계의 앞(초성 + `Vowel`)은 중성이 있고 받침이 없으므로 `ReplaceVowel`의 조건이 늘 맞다(오류 기록 없음, 시험 HC-70~HC-75의 `reports` 비어 있음).
- **⌫·되돌림:** `backspace()`·`undoLast()`는 그대로(목록 끝 하나를 빼고 접음)라 한 단계씩이다 — 괘 → 고 → ㄱ, 궤 → 구 → ㄱ(0.39, 키로 친 괘·궤와 같음), 괙 → 괘 → 고, 가괘 → 가고, 괴 → 고, 귀 → 구, 의 → 으, 홀로 ㅘ → ㅗ → 빔, 가과 → 가고 → 가ㄱ(넘어간 ㄱ 뒤부터), 고ㅘ → 고ㅗ, 곽 → 과 → 고. `snapshot`·`restore`(LLD-09)는 같은 목록을 복사하므로 오류 복구도 같은 단계다.
- **되돌린 뒤 이어 치기:** 남은 목록의 상태는 그 글자를 키로 친 상태와 같다 — 괘 ⌫(= `[ㄱ, Vowel ㅗ]`, 0.39) + ㅐ 키 = 괘, + ㅣ 키 = 괴, 과 + ㅣ 키 = (ㅘ, ㅣ) = ㅙ → 괘(0.40, FSS#129. 0.39까지 과ㅣ) ⌫ = 과 ⌫ = 고, 과 ⌫(= `[ㄱ, Vowel ㅗ]`) + ㅏ 키 = 과, + ㅐ = 괘.
- **바뀌지 않는 것:** 다음 모음과의 병합은 병합 표 그대로(0.40부터 과 + ㅣ = 괘 · 궈 + ㅣ = 궤, 괘 + ㅣ = 괘ㅣ — HC-71), 키로 친 겹모음(ㄱ ㅗ ㅏ, ㄱ ㅗ ㅐ — 키 하나가 단위 하나, ⌫ = 고), 짝 모음 ㅛ·ㅕ·ㅑ·ㅠ와 시프트 자모 ㅒ·ㅖ(`VOWEL_STEPS`에 없음 — 한 키·한 획으로 받으면 단위 하나, 0.40부터 두 키 병합으로 만들면 키 하나씩, HC-78·HC-80), **그래피티 정책**(`splitCompoundVowel` 거짓 — 천지인 조립기가 `Vowel`·`ReplaceVowel`을 보내는 지금 길, `Vowel(ㅘ)`도 단위 하나, BS-20·HC-75), 쿼티(조합기 없음), 확정 경계.
- 근거: SRS FR-HGL-04f ①~④, FR-CMN-07, FR-HGL-04d 시험 사례, HLD 4.4. 시험: HC-70(보이는 글자 고정), HC-71(다음 모음 고정), HC-72(⌫ 단계), HC-73(되돌린 뒤 이어 치기), HC-74(`undoLast`·`snapshot`), HC-75(키 입력·그래피티·한 키 모음 그대로), ID-43(앱 길).

## 8. ~~긋기 자판 배치 데이터~~ — 삭제 (FSS#95)

## 9. ~~SwipeKeyboardView와 TouchTracker~~ — 삭제 (FSS#95)

## 10. ~~시퀀스~~ — 삭제 (FSS#95)
