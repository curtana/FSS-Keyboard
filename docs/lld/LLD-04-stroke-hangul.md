# LLD-04 방향 긋기 판정과 한글 조합

- 대응 HLD 절: 4장(4.2 변 통과 판정, 4.4 HangulComposer)
- 버전 **0.36** (2026-11-14)
- 0.36(FSS#95, SRS 0.85·HLD 0.78: 긋기 입력 모드 삭제): 파일 이름을 `LLD-04-swipe.md`에서 바꾸고, 탭 자판·모드 키·그래피티가 쓰는 **2절(키 찾기)·3절(변 통과 추적)·7절(조합기)만 남긴다. 절 번호는 그대로**라 다른 문서·코드의 "LLD-04 §2·§3·§7" 참조는 그대로 맞다. 4절(`StrokeClassifier`)·5절(`ProgressState`)·6절(`SwipeInputMapper`·`VowelStrokeAssembler`)·8절(긋기 자판 배치 데이터)·9절(긋기 자판 뷰·획 자국·가운데 글자)·10절(시퀀스)은 그 코드(`gesture-engine` 모듈, `kbd.app.swipe`, `kbd.hangul.stroke`)와 함께 지웠다(번호 재사용 없음). 7절: `ComposePolicy.SWIPE`와 `lastConsonant()`(쓰는 곳이 긋기 자판뿐)를 지웠다(정책은 두벌식·그래피티 둘, 두 정책의 표·동작은 그대로). `JamoEvent.DoubleConsonant`·`UndoLast`와 `EdgeCrossingTracker.copy()`·`nthCrossingOf`는 모듈의 공용 형식으로 남긴다. 0.35까지의 긋기 쪽 변경 이력은 git 이력에 있다.
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
- 근거: HLD 5A("이웃 틈은 가까운 키로"), SRS 3.3(들여쓰기 빈칸은 가장 가까운 키). 시험: KH-01~KH-04.

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
data class ComposePolicy(val name: String, val merge: VowelMergeTable, val mergeConsecutiveVowel: Boolean) {
    companion object {
        val DUBEOLSIK = ComposePolicy("dubeolsik", MergeTables.DUBEOLSIK, true)
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

## 8. ~~긋기 자판 배치 데이터~~ — 삭제 (FSS#95)

## 9. ~~SwipeKeyboardView와 TouchTracker~~ — 삭제 (FSS#95)

## 10. ~~시퀀스~~ — 삭제 (FSS#95)
