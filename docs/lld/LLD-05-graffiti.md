# LLD-05 그래피티

- 대응 HLD 절: 5장(그래피티 처리 흐름)
- 버전 **0.38** (2026-11-14)
- 0.38(FSS#96, SRS 0.88 FR-TUT-14·17, LLD-07 0.39 8절): **내 획 학습에 잇기** — 8.3 `GraffitiPadView.onInk`(쓰기 화면의 획을 입력하지 않고 받음), 11절 `revision`·`personal`에 개인 획 저장소(`PersonalStrokeStore`)를 잇는 곳. 인식·특징·모델은 그대로다.
- 0.37(FSS#96, SRS 0.88 FR-GRF-10~14·NFR-13·TBD-35, HLD 0.81 5.1·10.1 R14~R19): **글자 획 = 템플릿 점수와 신경망 점수의 합산.** 3.1 4번(집합마다 `고르기`), 3.2(`Recognizer(templates, model, personal)`·`withPersonal`·`score`), 새 3.4(특징 `StrokeFeatures`: 8 × 8 격자 × 방향 8 + 가운데 1 = 576), 3.5(순전파 `NnBody`: 합성곱 둘·풀링·완전연결, 임베딩 32), 3.6(합산 a = 0.5, 문턱 0.80·0.78과 출처, 돌려줄 id = 고른 글자의 내장 템플릿 최고, 개인 획 = 개인 템플릿 + 예시, 모델 없는 구성), 새 7.1(모델 파일 형식·항목별 크기·줄일 선택지와 잰 영향·APK 증가분), 새 11절(`GraffitiRecognizers`: 모델을 뒤 스레드에서 읽음, 개인 획의 판과 목록을 인자로 받음 — 저장소는 내 획 학습에서 잇는다). 학습·내보내기 스크립트 `tools/graffiti-nn/`. 규칙 획, 집합 순서, `TemplateMatcher`의 점수 계산, `StrokeToken`, `GraffitiState`는 그대로다. **설계 문서에 없어 여기서 임시로 정한 것:** 모델을 읽지 못하면 템플릿 점수만으로 인식(3.6 "모델 없는 구성").
- 0.36(FSS#95, 긋기 입력 모드 삭제): 긋기 참조만 지웠다 — `CheonjiinAssembler`를 가지는 곳은 그래피티뿐이다(4절). 조합기 명세는 `LLD-04-stroke-hangul.md` 7절(절 번호 그대로). 동작·표는 그대로. 아래 이력의 "긋기 천지인 키"·"SWIPE n.n"은 그때의 일이다(SWIPE 10.1 ③은 지금 SRS 부록 A.2).
- 0.35(FSS#84, SRS 0.82 FR-CMN-14 ⑥⑧, HLD 0.75 5A): 패드·보조 줄·덮개는 자판 폭 W 그대로이고 여유 칸은 비운다(배치 코드 그대로). `GraffitiPadView.sideMarginPx`(`SideMarginView`) — 보조 줄 모드 키의 `KeyGeom`을 만들 때 테두리의 왼쪽·오른쪽을 여유 칸만큼 밖으로 준다(8절). 인식·영역 판정은 그대로다.
- 0.34(FSS#80, SRS 0.81): 영문 오너 획 템플릿 156개를 `EN_LETTER`에 더했다(7절). 판정 코드와 안내표 칸은 그대로다.
- 0.33(FSS#72, SWIPE 0.56 10.1 ③, LLD-07 0.32): `GraffitiGuidePanel.reveal`(10.2). 강조 없이 그 칸이 보이게 넘긴다. 키보드의 덮개는 쓰지 않는다.
- 0.32(FSS#70, SRS 0.78): 인식 전용 변형 `en_q2`를 더했다(7절). Q의 꼬리가 고리 폭 밖으로 나가면 `en_q`보다 `en_v`에 가까워져 v가 되던 문제다. 안내표 칸은 그대로다.
- 0.31(FSS#68, SRS 0.78): 문구 정리. 7절 `output.type`의 `jamo`에서 쌍자음은 시험용 템플릿에만 있음을 적었다. 형식과 동작은 그대로다. 아래 변경 이력의 칸 수·변형 수는 그 판의 값이므로 고치지 않는다(지금 값은 10.1).
- 0.30(FSS#52·FSS#64, 오너 결정 2026-11-05): **한 획 쌍자음 템플릿 삭제**(`ko_gg`·`ko_dd`·`ko_bb`·`ko_bb2`·`ko_ss`·`ko_jj`, 안내표 칸 5개 포함). 쌍자음은 시프트 + 자음(5.2 19행)이다. **오너 획 템플릿 추가**(7절): 그린 템플릿은 정사각형 비율이라 수평획이 긴 ㄴ이 `ko_bb2`에 지고(FSS#52), 둥근 고리의 ㅍ이 코너 감점으로 SPACE_V에 졌다.
- 0.29(FSS#47, 오너 실기 신고): **손 떼는 꼬리**(3.1 2번). 태블릿에서 ㅣ를 그으면 손을 떼는 순간 끝점이 5~13dp 되돌아와 직선도가 0.84~0.91로 떨어져(실기 ㅣ 23획 중 7획, 길게 그은 획일수록) 인식 실패였다. 직선 규칙은 시작점에서 가장 먼 점 뒤의 경로가 그 앞 경로의 `LIFT_TAIL_RATIO`(0.2) 이하면 그 점에서 획이 끝난 것으로 본다. 템플릿 비교와 되짚는 획(절반 되짚음 이상)은 그대로다. 시험 GR-36(실기 획)·GR-37.
- 0.28(FSS#48 오너 요청, SRS 0.75 FR-GRF-01a, HLD 0.68 5장): **인식 영역 경계.** 8.1에 문자·숫자 영역 경계 사각형(영역 사각형을 `AREA_INSET_DP` = 4dp 안으로, 둥근 모서리 6dp, 선 1dp, `0xFF505050`)을 더하고 영역 사이·패드 아래 구분선을 뺐다. 구석 점·언어 글자·대기 표시를 테두리 안으로 옮겼다(점 중심 (10, 10)dp, 대기 표시 안쪽 여백 10dp). 영역 판정·크기·`cornerRect`는 그대로다. 시험 PI-01 기대값, PI-09·PI-10(LLD-11 0.43).
- 0.27(FSS#33, 오너 실기 신고): **닫힌 고리 비교**(3.2 4a단계). 원 계열 템플릿(ㅇ·o·0·@·°)과는, 입력이 거의 닫힌 고리면 정확히 한 바퀴로 자르거나 닫은 뒤 시작점을 돌려 가장 잘 맞는 자리에서 점수를 낸다. 시작 위치·덜 닫힘·더 돎에 강해진다. 판정 규칙(SRS·HLD)은 그대로이고 회전 정규화도 하지 않는다(방향은 그대로 가름). 시험 GR-33~GR-35.
- 0.26(이슈 #258 오너 결정): 패드 상태 표시의 다른 제품 이름 언급을 뺐다. 동작은 그대로다.
- 0.25(이슈 #254 오너 결정): 다른 제품 이름이 든 출처 표현을 중립 표현으로 바꿨다(7절 획 데이터·패드 상태 표시). 획 규칙·템플릿은 그대로다.
- 0.24(이슈 #249·#251): 획 체계 문서의 파일 이름·링크와 원본 출처 문구를 머리말에서 뺐다(문서는 비공개로 옮김). 본문의 "GRAFFITI n.n" 절 번호는 그대로다.
- 0.23(이슈 #189, SRS 0.45 FR-GRF-09~09d, HLD 0.41 5장, GRAFFITI 0.16 6.6): **획 안내표**. 새 10절 — 칸 목록 `GuideCatalog`(recognizer `kbd.graffiti.guide`, 템플릿 id·규칙 획을 가리킴, 162칸 = 한글 25 + 모음 입력 순서 21 + 영문 35 + 숫자 12 + 제어 획 12 + 기호 57), 배치 `GraffitiGuideLayout`, 덮개 `GraffitiGuidePanel`(app). 버튼·열고 닫기·터치 막기는 `KeyboardRootView`(LLD-08 7.1). 패드 뷰(8절)는 바뀌지 않는다. 시험 GC-01~GC-09, GD-01~GD-11(LLD-11 6.6).
- 0.22(이슈 #205): 보조 줄 모드 키는 **뗄 때** 판정한다(8절 터치). 위로 나감(패드 쪽) = `sink.onSettingsStroke()`(조합 확정 뒤 설정 화면), 그 밖 = 모드 전환. 판정은 탭 자판과 같은 `TapGestureClassifier` + `ModeKeyGesture.of`(LLD-05A 4절, HLD 8.2), 문턱 `minCrossDp`는 서비스가 기준값에서 넣는다. 모드 키에서 시작한 손가락은 패드로 올라가도 패드 획이 아니다. 시험 MK-08(LLD-11 5.5).
- 0.21(이슈 #179, SRS 0.44 FR-CMN-08 ①/② 분리·FR-GRF-07): `InputSink.restricted` → `hidesDisplay`(비밀번호 입력창만). 학습 금지 입력창에서도 인식 글자를 잠깐 보인다(6절 `onStroke`, 8.3 `flash`). 기록(오류 기록)은 그대로 끈다(LLD-08 3.1 `noRecord`).
- 0.16(이슈 #160, HLD 0.32 3장·5장, SRS 0.36 FR-HGL-04c·FR-CMN-07b): `CheonjiinAssembler`·`CjElement`·`CjState`·`CjPush`·`CjPop`·`CjSnapshot`을 recognizer `kbd.graffiti.cheonjiin`에서 **core-hangul `kbd.hangul.cheonjiin`**으로 옮겼다(긋기 천지인 키와 함께 씀, 4절). 동작·표는 그대로. 조합기의 `ReplaceVowel`이 자기 단위로 쌓이므로(LLD-04 7.1) ⌫의 `ToVowel`은 `undoLast()`로 바꿨다(6절). 그 결과 조립기가 닫힌 뒤의 ⌫도 요소 하나씩이다(간 ⌫ = 가, ⌫ = 기).
- 공통 타입은 [LLD-03-modules.md#3](LLD-03-modules.md#3-모듈-사이-공통-타입), 조합기는 [LLD-04-stroke-hangul.md#7](LLD-04-stroke-hangul.md#7-hangulcomposer-core-hangul)에 있다.
- 획 체계의 정본은 그래피티 규칙 문서(비공개, 저장소에 두지 않음, 이슈 #249) 0.15(이하 GRAFFITI) 4.2·6장(6.1 영문 글자·6.3 숫자·6.4 구두점·확장 기호 획 표, 6.2 "이 앱에서 정한 세부", 6.5 겹치는 모양 포함), 요구는 SRS 0.28 FR-GRF-01~08(05a~05d 포함), FR-HGL-04c~04e, FR-CMN-07b다. 영문·숫자·기호 획은 오너 결정 #79 OD-1·OD-2 A, 겹치는 모양은 오너 결정 #79 OD-3~OD-7 A대로다(0.7).
- 숫자류 입력창(숫자·전화·날짜시간·비밀번호)에서는 그래피티가 아니라 영문 쿼티로 시작하므로(SRS FR-CMN-04, LLD-08 5절) 숫자 입력창 전용 패드는 없다.

## 1. 범위

| 절 | 구성 요소 | 모듈 |
|---|---|---|
| 2 | 타입 | recognizer |
| 3 | `Recognizer`, `StrokeRules`, `TemplateMatcher`, `TemplateSet`, 0.36: `StrokeFeatures`(3.4)·`NnBody`(3.5)·합산과 개인 획(3.6) | recognizer |
| 4 | `CheonjiinAssembler` | recognizer |
| 5 | `GraffitiState` | recognizer |
| 6 | `GraffitiInputHandler` (조립기·조합기 동기화, ⌫ 순서) | app |
| 7 | 템플릿 JSON 형식, 0.36: 모델 파일 형식(7.1) | recognizer 리소스 |
| 8 | `GraffitiPadView`, `InkCollector` | app |
| 9 | 시퀀스: 그래피티 한 획, ⌫ | – |
| 10 | 획 안내표: `GuideCatalog`(recognizer), `GraffitiGuideLayout`·`GraffitiGuidePanel`(app) (0.23) | recognizer, app |
| 11 | 인식기 공급 `GraffitiRecognizers`: 모델 읽기, 개인 획 반영 (0.37) | app |

## 2. 타입

```kotlin
data class InkPoint(val x: Float, val y: Float, val t: Long)     // dp, ms (패드 좌표)
data class InkStroke(val points: List<InkPoint>, val area: PadArea,   // 시작점이 있는 영역(GRAFFITI 6.3)
                     val areaRect: RectF2)                             // 그 영역 사각형(dp), 구석 판정용
enum class PadArea { LETTER, NUMBER }
enum class GraffitiLang { KO, EN }
data class RectF2(val left: Float, val top: Float, val right: Float, val bottom: Float)   // recognizer는 stroke-core에 의존하지 않음(HLD 2장)

sealed interface StrokeToken {                 // 인식 결과(상태와 무관한 모양 판정)
    data object Tap : StrokeToken
    data object CornerTap : StrokeToken        // 문자 영역 왼쪽 위 구석 탭(FR-GRF-05a)
    data object LineDown : StrokeToken         // 위 → 아래 직선
    data object LineUp : StrokeToken           // 아래 → 위 직선(시프트)
    data object LineRight : StrokeToken        // 왼쪽 → 오른쪽
    data object LineLeft : StrokeToken         // 오른쪽 → 왼쪽(백스페이스)
    data object DiagDownLeft : StrokeToken     // 오른쪽 위 → 왼쪽 아래(리턴)
    data object DiagDownRight : StrokeToken    // 왼쪽 위 → 오른쪽 아래(확장 시프트)
    data class Symbol(val id: String) : StrokeToken   // 템플릿 결과(템플릿 JSON의 id)
    data object Unknown : StrokeToken          // 어느 것에도 맞지 않음(인식 실패, FR-GRF-08)
}

sealed interface GraffitiAction {              // GraffitiState가 해석한 동작
    data class CommitText(val text: String) : GraffitiAction              // 영문·숫자·기호(한글 조합 중이면 먼저 확정)
    data class Consonant(val c: Jamo) : GraffitiAction                    // 쌍자음 포함
    data class Element(val e: CjElement) : GraffitiAction                 // 천지인 요소
    data object Space : GraffitiAction
    data object Backspace : GraffitiAction
    data object Enter : GraffitiAction
    data object Finish : GraffitiAction                                   // 'J' 획
    data object ToggleLang : GraffitiAction
    data object StateOnly : GraffitiAction     // 시프트·캡스록·확장 시프트·구두점 시프트(입력 아님, FR-CMN-03a)
    data object RecognitionFailed : GraffitiAction   // 인식 실패: 입력 없음 + 오류 진동(FR-GRF-08)
}
```

## 3. Recognizer

### 3.1 판정 순서 (GRAFFITI 6.2 3번, 4.4)

```
recognize(stroke, lang, expect):
 1. 길이 L = 경로 길이, B = 경계 상자 대각선
    L < TAP_MAX_DP 그리고 B < TAP_MAX_DP                        → 탭
        stroke.area == LETTER 이고 시작점이 구석(왼쪽 위, x − left < 폭 × CORNER_RATIO 그리고 y − top < 높이 × CORNER_RATIO) → CornerTap
        그 밖 → Tap
 2. 손 떼는 꼬리 빼기(0.29): F = 시작점에서 가장 먼 점. F 뒤 경로 ≤ F까지 경로 × LIFT_TAIL_RATIO 이면 획을 F에서 끊는다(2번에서만)
    직선도 S = |끝 − 시작| / L ≥ LINE_STRAIGHTNESS 이면 직선. 방향 각 θ = atan2(dy, dx)
        |θ − 90°| ≤ LINE_SECTOR → LineDown,  |θ + 90°| ≤ LINE_SECTOR → LineUp
        |θ| ≤ LINE_SECTOR → LineRight,       |θ − 180°| ≤ LINE_SECTOR → LineLeft
        |θ − 135°| ≤ LINE_SECTOR → DiagDownLeft, |θ − 45°| ≤ LINE_SECTOR → DiagDownRight
        그 밖 직선 → 4번으로
 3. 1·2번이 규칙 토큰 r(Tap·LineDown·LineUp·LineRight·LineLeft·DiagDownLeft·DiagDownRight·CornerTap)을 냈으면:
       expect가 PUNCT·EXTENDED이고 id = TemplateSet.ruleSymbol(expect, r) 가 있으면 → Symbol(id)   // 기호표의 직선·탭 획(FR-GRF-05c ⑥ 단서)
       그 밖 → r
 4. (규칙 토큰이 아닌 획) expect가 PUNCT·EXTENDED면 먼저 기호표 집합 = TemplateSet.setNameFor(area, lang, expect)  (3.3)
       고르기(stroke, 기호표 집합)가 받아들이면 → Symbol(id)                // 기호표 획이 먼저(FR-GRF-05c ⑥ 단서)
    평소 집합 = TemplateSet.setNameFor(area, lang, NORMAL)               // 기호표에 없는 획은 평소대로(05c ⑦)
    고르기(stroke, 평소 집합)가 받아들이면 Symbol(id) 아니면 Unknown
       // 고르기 = 글자별 합산 점수의 최고 글자와 두 문턱(0.37, FSS#96, 3.6). 모델이 없으면 템플릿 점수 ≥ MATCH_MIN(0.35까지의 판정)
```

- 매개변수 초기값(D-11): `TAP_MAX_DP = 8`, `CORNER_RATIO = 0.15`(SRS FR-GRF-05a, 구현 시 조정), `LINE_STRAIGHTNESS = 0.92`, `LINE_SECTOR = 20°`, `MATCH_MIN = 0.80`(0.36부터 모델 없는 구성에서만 쓴다. 합산의 a와 두 문턱은 3.6), `LIFT_TAIL_RATIO = 0.2`(0.29). 모두 실측으로 조정하는 인식 매개변수다.
- **손 떼는 꼬리(0.29, FSS#47):** 정전식 화면은 손가락을 뗄 때 닿는 면이 줄며 마지막 점들이 획 반대쪽으로 몇 dp 되돌아온다(태블릿 실측 5~13dp, 획의 9% 이하). 직선 규칙만 이 꼬리를 뺀다. 한계 0.2는 실측 꼬리의 두 배쯤이고, 되짚는 획(절반 되짚음 = 0.5 이상)과 J 갈고리(약 0.45)보다 작아 그 획들은 그대로 템플릿으로 간다. 템플릿 비교에는 획 전체를 쓴다.
- 탭·짧은 선을 템플릿보다 먼저 규칙으로 가른다(GRAFFITI 4.4 "길이와 방향 규칙으로 판정").
- **기호표의 직선·탭 획(3번, 0.7):** 원본 구두점·확장 기호표에는 제어 획과 같은 직선·탭 모양 기호가 있다(GRAFFITI 6.4·6.5). 대기 중 그 표에 실린 획이면 직선·탭 모양이어도 기호이고 제어 획보다 먼저다(SRS FR-GRF-05c ⑥, GRAFFITI 6.2-8 ①). 그래서 규칙이 정한 토큰을 대기 중이면 기호표에서 한 번 더 찾는다. 기호표의 규칙 항목은 템플릿 JSON의 `rule` 칸이다(7절).
  - 규칙 항목(제품 템플릿): 구두점 `.` = Tap, `,` = DiagDownLeft, `'` = LineDown, `-` = LineRight, `!` = LineUp, `\` = DiagDownRight(OD-4 A) / 확장 `±` = LineRight, `×` = DiagDownLeft, `¡` = LineUp. 확장 기호표에 탭은 없다(확장 대기 + 탭 = 한영 전환, 5.2 1행. `•`는 배정하지 않음, OD-7 A). 확장 기호표에 ↘·↓도 없다(확장 대기 + ↘ = 확장 대기 그대로, GRAFFITI 6.2-8).
  - **길이로 가르지 않는다.** 1번 탭 판정 뒤 직선이면 길이와 상관없이 같은 토큰이다. 그래서 구두점 대기 + 짧은 ↙·긴 ↙ 모두 `,`, 확장 대기 + ↙ = `×`이고 리턴이 아니다(SRS FR-GRF-05c ⑥ "모양으로 찾고 길이로 가르지 않는다", PM 판단 #79).
  - **영역도 가르지 않는다.** 기호표 집합은 area·lang과 무관하므로(3.3 표) 한글 모드 문자 영역의 ↓·→·탭(ㅣ·ㅡ·ㆍ 모양)도 구두점 대기 중이면 `'`·`-`·`.`이다(오너 결정 #79 OD-5 A). 자음 모양 획(ㅇ → `@`, ㅈ → `+` 등)은 4번에서 같은 원리로 기호표가 먼저다.
  - **CornerTap과 LineLeft는 기호표에 넣을 수 없다**(`TemplateLoader`가 거부, 7절). 구석 탭은 기호표의 탭과 다른 획이라 대기 중에도 한영 전환이고(FR-GRF-05c ⑥, 5.2 11행), ⌫ 획(←)은 대기 중에도 늘 대기 취소다(오너 결정 #79 OD-3 A, 5.2 4행·6절 ⌫ 순서 1번).
  - expect가 NORMAL이면 규칙 항목은 보지 않는다(대기가 없으면 직선·탭은 평소 뜻, 5.2 4~18행).
- **되짚는 획(0.7):** 원본의 쐐기형 기호 `_` `|` `` ` `` `;` `:`와 B① D① P① R① H는 같은 길을 되짚는 구간이 있다(GRAFFITI 6.1·6.4, 되짚는 지점은 미확인). 판정 방법(GRAFFITI 6.4 "LLD 몫"): 되짚으면 끝점이 시작점 쪽으로 돌아오므로 직선도 S = |끝 − 시작| / L이 작아(한 다리를 끝까지 되짚으면 S ≈ 0, 절반 되짚어도 S ≈ 1/3) 2번 직선 규칙(S ≥ 0.92)에 걸리지 않고 4번 템플릿으로 간다. 따로 "되짚음" 특징은 두지 않는다. 템플릿은 되짚는 두 다리를 조금 벌린(가는 V) 모양으로 그려 방향과 시작점이 갈리게 한다. 지금 템플릿이 있는 것은 B ①·D ①·H이고, 쐐기형과 P ①·R ①은 획이 확정되면 넣는다(7절). 시험 GR-30.
- 한영 전환 ②(사선 + 탭)의 "가운데 탭"은 위치를 보지 않는다. GRAFFITI 4.2 "사선 다음이 탭이면 한영 전환"을 따른다.

### 3.2 함수

```kotlin
class Recognizer(templates: TemplateSet, model: GraffitiModel? = null, personal: List<PersonalStroke> = emptyList()) {   // 0.36: model·personal(3.6)
    fun recognize(stroke: InkStroke, lang: GraffitiLang, expect: Expect): StrokeToken
    fun withPersonal(personal: List<PersonalStroke>): Recognizer      // 0.36: 개인 획 목록을 바꾼 새 인식기(3.6)
    fun score(stroke: InkStroke, set: SetName): LetterScore?          // 0.36: 한 집합의 고르기 결과(문턱을 넘지 못해도). 시험·측정·학습 화면용, 규칙은 보지 않음
}
class PersonalStroke(val set: SetName, val value: String, val points: List<Pair<Float, Float>>)   // 0.36: 개인 획(원 표본 dp, 글자 = output.value)
class LetterScore(val letter: String, val id: String, val template: Float, val network: Float,     // 0.36: 고른 글자, 돌려줄 id, 그 글자의 두 점수,
                  val sum: Float, val setTemplate: Float, val accepted: Boolean)                    //       합산, 집합의 템플릿 최고, 두 문턱을 넘었는가
enum class Expect { NORMAL, PUNCT, EXTENDED }   // 구두점 시프트·확장 시프트 대기면 그 집합에서 먼저 찾음(3.1 3·4번)
```

#### `recognize`
- 사전: `stroke.points.size ≥ 1`.
- 처리: 3.1 순서.
- 오류: 점이 하나면 탭. 예외는 호출자의 `EngineGuard`가 잡는다.
- 결정성: 시계·난수를 쓰지 않는다(NFR-04).
- 근거: FR-GRF-02, 03, 05, 05b, 05c ⑥⑦, 06, 10, 11, 14, HLD 5장·5.1. 시험: GR-01~GR-20, GR-25~GR-35, 0.36: LLD-11 6.7.

#### `TemplateMatcher.best(stroke, set): Pair<String, Float>?`
- 처리(직접 구현, GPL 코드 미사용):
  1. 경로를 같은 간격 `N = 32`점으로 재표본화.
  2. 중심(재표본화한 점들의 **무게중심**, 산술 평균)을 원점으로 옮기고, 경계 상자의 긴 변을 1로 하는 **같은 비율** 축척(가로세로 비 보존 — ㅡ/ㅣ 류 왜곡 방지). 긴 변이 0이면 축척하지 않는다.
  3. **회전 정규화 안 함**(방향이 의미를 가르므로, GRAFFITI 7장).
  4. 템플릿마다 평균 점 거리 `d`를 구하고 점수 = `1 − d / √2`. 같은 템플릿 id의 대체 획 중 최댓값.
  4a. **닫힌 고리(0.27, FSS#33):** 손으로 그린 원은 맨 위에서 시작하지도, 정확히 닫히지도 않는다. 템플릿 획이 **고리 템플릿**(끝 간격 ≤ 0.05로 닫혀 있고 `corners` = 0: ㅇ `ko_ieung`, o `en_o`, 0 `num_0`, @ `p_at`, ° `x_degree`)이고 입력이 **거의 닫힌 고리**면, 4번 점수와 아래 고리 점수 가운데 큰 값을 쓴다.
     - 거의 닫힌 고리(`loopOf`): 입력의 코너 수 < 2(코너가 많은 둥근 ㅁ은 4번 그대로, 5번 보조가 가름), 입력을 64점으로 재표본화해 더한 부호 있는 방향 변화의 크기가 300°~540°. 360°를 넘으면 360°에 이른 점에서 자른다 — 그 뒤 점은 모두 자른 고리에서 긴 변의 0.15 안이어야 한다(넘으면 꼬리: g·q·6·9). 자른(또는 그대로인) 고리의 끝이 시작에서 긴 변의 0.35 안이어야 한다(돌기만 하고 돌아오지 않는 6·9·e 제외). 마지막에 시작점을 붙여 닫는다(덜 닫힌 틈은 곧은 다리).
     - 고리 점수(`cyclicScore`): 닫은 고리를 1·2번대로 32점(처음 = 끝)으로 만들고, 서로 다른 31점의 시작을 한 칸씩 돌려 가며 템플릿 31점과 평균 거리를 재 가장 작은 값으로 `1 − d / √2`. 방향은 바꾸지 않는다(템플릿이 두 방향을 따로 가짐. @는 한 방향뿐).
     - 한계: 세로로 긴 고리를 왼쪽 아래에서 시계 방향으로 시작하면 D(`en_d2`, 왼쪽 아래 모서리에서 위로 올라 시계 방향)와 같은 모양이라 D로 읽힐 수 있다(GR-33이 허용).
  5. 코너 수가 템플릿의 `corners`와 2 이상 차이 나면 점수에서 0.1을 뺀다(ㅁ/ㅇ, U/V 구분 보조). 코너 수는 2번까지 마친 32점에서 센다(G11):
     - 점 `i`(`CORNER_WINDOW ≤ i < N − CORNER_WINDOW`)의 방향 변화 = 현 `p[i − W] → p[i]`와 현 `p[i] → p[i + W]` 사이 각, `W = CORNER_WINDOW = 2`. 길이 0인 현이면 변화 0.
     - 변화 ≥ 60°인 점을 표시하고, **연달아 표시된 점들은 코너 하나**로 센다.
     - 이웃한 구간(W = 1)으로 재면 두 표본 사이에 떨어진 90° 모서리가 45° 두 개로 갈라져 놓친다. W = 2면 90° 모서리는 어디에 떨어져도 약 70° 이상이고, 32점 원은 약 23°에 머문다.
  6. 가장 높은 점수의 id. 같으면 템플릿 파일 순서가 앞인 것.
- 시험: GR-21~GR-24(정규화·같은 비율·회전 구분·코너 보조. GR-24는 모서리가 둥근 사각형처럼 코너 세기에 기대는 경우를 포함), GR-33~GR-35(닫힌 고리, 0.27).

### 3.3 TemplateSet

```kotlin
class TemplateSet(val sets: Map<SetName, List<Template>>) {
    fun forContext(area: PadArea, lang: GraffitiLang, expect: Expect): List<Template>
    fun outputOf(id: String): TemplateOutput?     // 모든 집합에서 id의 출력, 없으면 null(G3, 5.2 19~24행)
    companion object { fun setNameFor(area: PadArea, lang: GraffitiLang, expect: Expect): SetName }   // 0.36: 아래 표의 집합 이름
    fun ruleSymbol(expect: Expect, rule: StrokeToken): String?   // 0.7: expect의 기호표(PUNCT·EXTENDED)에서 rule 칸이 rule인 템플릿 id, 없으면 null. NORMAL이면 늘 null(3.1 3번)
}
enum class SetName { KO_LETTER, EN_LETTER, NUMBER, PUNCT, EXTENDED }
data class Template(val id: String, val output: TemplateOutput, val corners: Int, val strokes: List<List<Pair<Float, Float>>>,
                    val rule: StrokeToken? = null)   // 0.7: 규칙 항목이면 그 규칙 토큰(7절 `rule`), 이때 strokes는 빈 목록
```

| area | lang | expect | 집합 |
|---|---|---|---|
| 아무 | 아무 | PUNCT | PUNCT |
| 아무 | 아무 | EXTENDED | EXTENDED |
| NUMBER | 아무 | NORMAL | NUMBER(FR-GRF-04) |
| LETTER | KO | NORMAL | KO_LETTER |
| LETTER | EN | NORMAL | EN_LETTER |

- 규칙 항목(`rule`이 있는 템플릿)은 `strokes`가 비어 있으므로 `TemplateMatcher.best`가 건너뛴다(맞출 획이 없는 템플릿). 그래서 규칙 항목은 3.1 3번에서만 쓰인다.
- PUNCT·EXTENDED 집합에서 맞는 것이 없으면 `recognize`가 같은 area·lang의 NORMAL 집합으로 다시 찾는다(3.1 4번). 템플릿 id는 집합 사이에 겹치지 않으므로 5.2 2·3행은 id가 어느 집합에 속하는지로 가른다.

### 3.4 신경망 입력 특징 `StrokeFeatures` (0.37, FSS#96, SRS FR-GRF-12)

`kbd.graffiti.nn.StrokeFeatures.compute(xs: DoubleArray, ys: DoubleArray): FloatArray` — 획의 **원 표본 점 열**(dp, 재표본·정규화 전)에서 값 576개(`SIZE`)를 낸다. 놓이는 순서는 `[채널][행][열]`(채널 0~7 = 방향, 8 = 가운데. 행 = 격자 y, 열 = 격자 x)이다. **학습 스크립트(`tools/graffiti-nn/features.py`)와 연산 순서까지 같게** 계산한다 — 출고 가중치가 그 값으로 학습됐기 때문이다(FR-GRF-12 ⑤). 중간 계산은 64비트 부동소수, 칸에 쌓는 값은 32비트다.

```
입력: 원 표본 n점 (x_i, y_i), n ≥ 1
1. 둘레 상자: side = max(max x − min x, max y − min y), side = 0이면 1.  중심 c = ((max x + min x) / 2, (max y + min y) / 2)
   격자 좌표 g(p) = (p − c) / side × (FILL × GRID) + GRID / 2          // GRID = 8, FILL = 0.875 → 긴 변이 격자 0.5 ~ 7.5 (가로세로비 유지, 짧은 변은 가운데)
2. 재표본: 경로 길이로 같은 간격 RESAMPLE = 96점(= 12 × GRID). 자리 t_k = k × (전체 길이 / 95), 마지막은 전체 길이.
   t_k가 놓인 구간(누적 길이가 t_k 이하인 마지막 꼭짓점 j와 j + 1)에서 선형 보간. 전체 길이 0이면 96점 모두 첫 점
3. 방향 8채널: 재표본한 점을 격자 좌표로 옮긴 q_0..q_95의 걸음 k = 0..94마다
     길이 ln = |q_{k+1} − q_k|,  칸 = (⌊중점 y⌋, ⌊중점 x⌋)를 0..7로 자름(걸음은 **중점이 있는 칸 하나**에 통째로 들어간다)
     각 a = atan2(dy, dx) (음수면 + 2π) / (2π / 8),  b0 = ⌊a⌋ mod 8,  fr = a − ⌊a⌋
     채널 b0 에 ln × (1 − fr),  채널 (b0 + 1) mod 8 에 ln × fr       // 화면 좌표(y 아래): 0 = →, 2 = ↓, 4 = ←, 6 = ↑
   더하는 순서: 모든 걸음의 첫 몫을 차례로 더한 뒤 둘째 몫을 더한다(32비트 덧셈 순서를 학습 쪽과 같게).  끝나면 칸마다 min(값, 1)
4. 가운데 1채널: **원 표본**의 걸음 i = 0..n−2 가운데 |p_{i+1} − p_i| < CENTRE_STEP_DP(1.0dp)인 것마다
     칸 = 두 점의 격자 좌표 중점이 있는 칸,  그 칸에 CENTRE_GAIN(0.25)을 더한다.  끝나면 칸마다 min(값, 1)   // 4걸음 = 1
```

- 상수: `GRID = 8`, `DIRECTIONS = 8`, `CHANNELS = 9`, `SIZE = 576`, `RESAMPLE = 96`, `FILL = 0.875`, `CENTRE_STEP_DP = 1.0`, `CENTRE_GAIN = 0.25f`. 모델 파일 머리의 값과 다르면 모델을 읽지 않는다(7.1).
- **경계에 걸친 걸음:** 칸 경계에서 나누지 않는다. 재표본 한 걸음은 격자 단위로 7 / 95 ≈ 0.074(긴 변 기준)라 한 칸의 값은 실제 지나간 길이와 한 걸음만큼 다를 수 있다(시험 SF-02의 허용 폭).
- 자리와 크기는 1번에서 지워진다. 그래서 탭·직선·영역으로 갈리는 획은 규칙(3.1 1~3번)에 남는다(FR-GRF-12 ④).
- 점이 하나뿐이면 모든 값이 0이다(규칙이 먼저 탭으로 받으므로 인식 경로에서는 일어나지 않는다).
- 결정성: 시계·난수 없음(NFR-04). 시험: SF-01~SF-08, NN-02(학습 쪽 값과 대조, LLD-11 6.7).

### 3.5 순전파 `NnBody.embed` (0.37, FSS#96, SRS FR-GRF-11·13)

`kbd.graffiti.nn.NnBody.embed(features: FloatArray): FloatArray` — 특징 576개 → 길이 1인 임베딩 32개. 라이브러리 없이 32비트 부동소수 곱셈·덧셈만 쓴다.

| 층 | 입력 → 출력 | 가중치 수 |
|---|---|---|
| 합성곱 1 | 9 × 8 × 8 → 16 × 8 × 8, 3 × 3, 둘레 0 채움 1, 뒤에 ReLU | 16 × 9 × 9 + 16 = 1,312 |
| 합성곱 2 | 16 × 8 × 8 → 32 × 8 × 8, 3 × 3, 둘레 0 채움 1, 뒤에 ReLU | 32 × 16 × 9 + 32 = 4,640 |
| 최댓값 풀링 | 2 × 2 → 32 × 4 × 4, `[채널][행][열]`로 펴서 512 | – |
| 완전연결 | 512 → 32, 뒤에 ReLU | 32 × 512 + 32 = 16,416 |
| 정규화 | 벡터 / max(‖벡터‖, 1e-12) | – |

- 몸통 하나 = 가중치 22,368개 = 89,472바이트. 가중치 순서는 학습 틀의 것 그대로다(합성곱 `[출력][입력][행][열]`, 완전연결 `[출력][입력]`).
- **임베딩 차원 32**, 마지막 분류 층은 파일에도 없고 쓰지 않는다(FR-GRF-11 ③). 학습 때의 드롭아웃은 추론에서 아무 일도 하지 않는다.
- ReLU 뒤라 임베딩의 모든 값이 0 이상이다. 그래서 두 임베딩의 코사인은 0~1이다. 모든 값이 0이면 임베딩은 0 벡터이고 코사인은 0이다.
- 덧셈 순서가 학습 틀과 같지 않아 값이 마지막 자리에서 다르다. 고정 표본 440획에서 잰 차이는 임베딩 7.7 × 10⁻⁷ 이하다(허용 오차 10⁻⁵, NN-03).
- 기기에서 가중치를 바꾸지 않는다. 시험: NN-03, GM-01.

### 3.6 합산·문턱·개인 획 (0.37, FSS#96, SRS FR-GRF-10·11·14)

집합 하나에서 글자를 고르는 방법이다(3.1 4번의 `고르기`).

```
고르기(획, 집합):                                   // 집합 = SetName 하나. 템플릿 획이 하나도 없으면 "없음"
  글자 = 그 집합에서 출력(output.value)이 같은 템플릿들. 순서는 모델 파일의 글자 목록 순서(7.1)
  t[글자]  = 그 글자의 내장 템플릿과 개인 템플릿 가운데 TemplateMatcher 점수(3.2의 4·4a·5번 그대로) 최고
  T       = 이 집합의 모든 템플릿(내장 + 개인) 점수 최고                     // 글자를 가리지 않음
  z       = 그 집합 몸통.embed(StrokeFeatures.compute(획의 원 표본))          // 3.4, 3.5. 특징은 획마다 한 번만 계산
  n[글자]  = z · e 의 최고, e = 그 글자의 예시(모델 파일의 출고 예시 + 개인 획의 임베딩). 예시는 길이 1이라 내적 = 코사인
  w[글자]  = WEIGHT × t[글자] + (1 − WEIGHT) × n[글자]                      // 32비트. 코사인은 **그대로** 쓴다(범위를 바꾸지 않음)
  고른 글자 = w 최고(같으면 글자 목록에서 앞)
  받아들임 = w[고른 글자] ≥ SUM_MIN 그리고 T ≥ TEMPLATE_MIN               // 둘 다 64비트로 올려 비교
  돌려줄 id = 고른 글자의 **내장 템플릿 가운데 템플릿 점수가 가장 높은 것**(같으면 파일 순서가 앞)
```

| 상수 | 값 | 뜻 | 출처 |
|---|---|---|---|
| `Recognizer.WEIGHT` | 0.5 | FR-GRF-10 ④의 a | FSS#96 작업지시서 v2 3절(PM 측정 2026-11-14): 가르치는 날 안에서 고른 값이 30개 작업 가운데 19개에서 0.5 |
| `Recognizer.SUM_MIN` | 0.80 | 합산 문턱 (가) | 같은 문서 3절. 결과를 보고한 자료 자체로 고른 값이다(SRS TBD-35 ①) |
| `Recognizer.TEMPLATE_MIN` | 0.78 | 템플릿 문턱 (나) | 같은 문서 3절. 합산 문턱 하나로는 영문 합성 낙서의 45 %가 들어왔다 |
| `Recognizer.MATCH_MIN` | 0.80 | 모델 없는 구성의 템플릿 문턱(아래) | D-11(0.35까지의 값) |

- 값 한 쌍을 다섯 집합에 모두 쓴다(집합별로 고르지 않았다). 한 사람의 획으로 고른 값이다(HLD 5.1 한계).
- **코사인을 그대로 쓰는 까닭:** a와 문턱을 고른 측정이 코사인을 그대로 더했다(FR-GRF-11 ④). 범위를 바꾸면 문턱의 뜻이 달라진다.
- **돌려줄 id:** `StrokeToken.Symbol(id)`의 id는 늘 `TemplateSet`에 있는 내장 템플릿 id다. `GraffitiState`가 `TemplateSet.outputOf(id)`로 출력을 찾고(5.2), id는 집합마다 겹치지 않으므로 대기 표의 판정(5.2 2·3행)도 그대로다. 개인 템플릿은 id가 밖으로 나가지 않는다. 합산이 템플릿 단독과 다른 글자를 고르면 그 글자의 내장 템플릿 가운데 가장 가까운 것이 나간다(예: 오너 획 `en_u_o6`). 같은 글자의 id가 0.35와 달라질 수 있으나 출력은 같다.
- **글자 목록 일치:** 모델이 있을 때, 템플릿 획이 있는 템플릿의 출력이 그 집합의 모델 글자 목록에 없으면 `Recognizer`를 만들 때 `IllegalStateException`이다(HLD R18. 앱은 아래 "모델 없는 구성"으로 내려간다, 11절). 모델에만 있고 템플릿이 없는 글자는 고르지 않는다. 제품 템플릿과 출고 모델은 목록이 같다(GM-02).
- **개인 획(`PersonalStroke(set, value, points)`):** `points`는 원 표본 점 열(dp), `value`는 글자(`output.value`), `set`은 그 글자의 기본 집합이다.
  - 들어가는 곳: `KO_LETTER`·`EN_LETTER`뿐이다(`Recognizer.PERSONAL_SETS`, FR-GRF-14 ①). 다른 집합의 것, 그 집합에 없는 글자의 것, 점이 없는 것은 **버리고 넘어간다**(템플릿 구성이 바뀌어 글자가 없어진 뒤에도 파일을 읽을 수 있게).
  - 개인 템플릿 = 내장 오너 획 템플릿을 만드는 것과 같은 처리(`Recognizer.personalTemplate`): 32점 재표본(3.2 1번) → 둘레 상자의 왼쪽 위를 원점으로, 긴 변을 1로(같은 비율) → 소수 셋째 자리로 반올림(짝수 쪽) → **그 점 열에서** 3.2 5번대로 센 코너 수가 `corners`. 닫힌 고리 여부(끝 간격 ≤ 0.05)와 고리 점수(4a)도 내장 템플릿과 같게 적용된다.
  - 예시 = 그 집합 몸통으로 낸 그 획의 임베딩.
  - 내장 템플릿과 출고 예시는 그대로 둔다. 개인 획 목록이 비면 출고 상태와 같다(PS-03).
- **구성을 바꾸는 방법:** `Recognizer`는 바뀌지 않는 값이다. `withPersonal(목록)`이 **새 `Recognizer`**를 만들어 돌려준다(내장 템플릿의 준비와 모델은 함께 쓰고, 같은 `PersonalStroke` 객체의 개인 템플릿·임베딩은 다시 계산하지 않는다). 쓰는 쪽은 참조 하나를 바꿔 끼우므로 획 하나를 인식하는 도중에 구성이 바뀌지 않는다(HLD 5.1). 누가 언제 바꾸는지는 11절.
- **모델 없는 구성(`Recognizer(templates)`, 모델 `null`):** 템플릿 점수만으로 고른다 — 집합의 템플릿(내장 + 개인) 점수 최고가 `MATCH_MIN` 이상이면 그 글자, id는 위와 같다. 0.35까지의 판정과 같다. 쓰는 곳: ① 모델 파일을 읽지 못했거나 글자 목록이 어긋났을 때의 대체 동작(11절, LLD-09 4절 — **설계 문서에 없는 임시 처리**, 변경 이력), ② 시험용 템플릿(글자가 모델과 다름)으로 규칙·순서를 보는 시험, ③ 시험의 "템플릿 단독" 기준값(LLD-11 6.7).
- **템플릿 준비:** 내장 템플릿의 재표본·정규화(3.2 1·2번)는 `Recognizer`를 만들 때 한 번 해 둔다(`TemplateMatcher.prepare`). 점수 계산식은 그대로다.
- 시험: NN-04·NN-05, PS-01~PS-09, CE-01, SR-01~SR-03, GM-02.

## 4. CheonjiinAssembler

**소속(0.16, 이슈 #160, HLD 3장):** core-hangul, 패키지 `kbd.hangul.cheonjiin`(0.15까지 recognizer `kbd.graffiti.cheonjiin`). 그래피티(`GraffitiInputHandler`, 6절)가 객체를 하나 가진다. recognizer는 `CjElement`(획 → 요소)만 쓴다. recognizer는 core-hangul의 `kbd.hangul.cheonjiin`을 참조할 수 있다(MOD-03은 `kbd.hangul.compose`만 막음).

### 4.1 타입과 전이 표 (GRAFFITI 4.2)

```kotlin
enum class CjElement { I, EU, DOT }                  // 세로선 ㅣ, 가로선 ㅡ, 탭 ㆍ
sealed interface CjState {
    data object Empty : CjState
    data class Pending(val dots: Int) : CjState      // 1 = ㆍ, 2 = ‥ (모음 미성립)
    data class Vowel(val v: Jamo) : CjState
}
class CheonjiinAssembler {
    val elements: List<CjElement>          // 지금 조립 중인 요소(그은 순서)
    val state: CjState                     // = TABLE[elements]
}
```

요소 열 → 상태(`TABLE`). 표에 없는 열은 만들지 않는다.

| 요소 열 | 상태 | 요소 열 | 상태 |
|---|---|---|---|
| ㅣ | ㅣ | ㆍ | Pending(1) |
| ㅣㆍ | ㅏ | ㆍㆍ | Pending(2) |
| ㅣㆍㅣ | ㅐ | ㆍㅣ | ㅓ |
| ㅣㆍㆍ | ㅑ | ㆍㅣㅣ | ㅔ |
| ㅣㆍㆍㅣ | ㅒ | ㆍㆍㅣ | ㅕ |
| ㅡ | ㅡ | ㆍㆍㅣㅣ | ㅖ |
| ㅡㆍ | ㅜ | ㆍㅡ | ㅗ |
| ㅡㆍㆍ | ㅠ | ㆍㆍㅡ | ㅛ |
| ㅡㅣ | ㅢ | ㆍㅡㅣ | ㅚ |
| ㅡㆍㅣ | ㅟ | ㆍㅡㅣㆍ | ㅘ |
| ㅡㆍㆍㅣ | ㅝ | ㆍㅡㅣㆍㅣ | ㅙ |
| ㅡㆍㆍㅣㅣ | ㅞ | | |

- ㅠ(ㅡㆍㆍ) + ㅣ = ㅝ, ㅜ(ㅡㆍ) + ㅣ = ㅟ로 표 안에서 겹치지 않는다(FR-HGL-04d "ㄱ, ㅡ, 탭, 탭, ㅣ = 궈").

### 4.2 함수

#### `push(e: CjElement): CjPush`
`sealed CjPush { Started(Vowel) | Changed(Vowel) | PendingShown(dots) | Ignored | NotExtendable }`
- 처리:
  1. `next = elements + e`.
  2. `next ∈ TABLE` → `elements = next`, 결과: 이전 상태가 Vowel이 아니고 새 상태가 Vowel → `Started(v)`, 둘 다 Vowel → `Changed(v)`, 새 상태가 Pending → `PendingShown(dots)`.
  3. `elements == [ㆍ,ㆍ]`이고 `e == ㆍ` → `Ignored`(세 번째 탭 무시, FR-HGL-04e).
  4. 그 밖(이미 성립한 모음 뒤에 이어질 수 없는 요소: ㅐ + 탭, ㅣ + ㅣ, ㅗ + 탭, ㅡ + ㅡ, ㅠ + 탭 등) → `NotExtendable`. 요소를 **무시**한다: `elements`·상태·표시를 바꾸지 않고 입력 단위에도 넣지 않는다(그래서 ⌫ 대상이 아님). SRS FR-HGL-04c, GRAFFITI 6.2-7, HLD 5장(DCR-04 결정).
- 근거: FR-HGL-04c, 04e. 시험: CJ-01~CJ-30, CJ-41.

#### `pop(): CjPop`
`sealed CjPop { Empty | ToVowel(v) | ToPending(dots) | ToNone(wasVowel: Boolean) }`
- 사전: 없음(비어 있으면 `Empty`).
- 처리: 마지막 요소를 빼고 남은 열의 상태로 결과를 낸다. 남은 열이 비면 `ToNone(이전 상태가 Vowel이었는가)`.
- 근거: FR-CMN-07b, HLD 5장 ⌫ 순서. 시험: CJ-31~CJ-40.

#### `close()`, `clear()`
- `close()`: 모음이 성립한 뒤 자음(받침)이 오면 부른다. 요소를 버린다. 닫힌 모음은 다시 열지 않는다(HLD 5장).
- `clear()`: 확정·초기화 때. 요소를 버린다. Pending이었으면 임시 글자도 버려진다(앱이 표시를 지움).
- 두 함수 모두 사후 `state == Empty`. 이름을 나눈 것은 호출 이유를 기록에 남기려는 것이다.

#### `pendingGlyph(): String`
- `Pending(1)` → "ㆍ"(U+318D), `Pending(2)` → "‥"(U+2025), 그 밖 → "". (FR-HGL-04e)

## 5. GraffitiState

```kotlin
class GraffitiState(private val templates: TemplateSet)   // Symbol(id)의 출력은 templates.outputOf(id)로 찾는다(G3)
```

### 5.1 상태

| 칸 | 값 | 수명 |
|---|---|---|
| `lang` | KO / EN | 그래피티 모드에 새로 들어올 때 KO(FR-GRF-05a). 커서 이동·확정에서는 유지 |
| `capsLock` | Boolean | 모드에 새로 들어올 때 false. 커서 이동·확정에서는 유지 |
| `shiftPending` | Boolean | 일회성. 다음 글자 획 하나에 적용 |
| `extPending` | Boolean | 일회성(확장 시프트 사선 뒤) |
| `punctPending` | Boolean | 일회성(구두점 시프트 탭 뒤) |

### 5.2 해석 `interpret(t: StrokeToken, area: PadArea): GraffitiAction`

HLD 5장·GRAFFITI 4.2·6.2·6.3, SRS FR-GRF-05·05c를 표로 옮긴다. 위에서부터 처음 맞는 행. "일회성"은 `shiftPending`·`extPending`·`punctPending` 셋이다. "일회성 끔"은 그 행의 결과를 정한 **뒤에** 끈다(그래서 16·20행은 끄기 전의 shift를 본다).

| # | 조건 | 결과 | 상태 변화 | 근거 |
|---|---|---|---|---|
| 1 | `extPending` 이고 `t == Tap` | ToggleLang | 일회성 끔 | GRAFFITI 4.2 한영 전환 ② |
| 2 | `extPending` 이고 `t == Symbol(id)` (EXTENDED 집합, 규칙 항목 `±`·`×`·`¡` 포함) | CommitText(기호) | 일회성 끔 | FR-GRF-05, 05c ⑥ 단서 |
| 3 | `punctPending` 이고 `t == Symbol(id)` (PUNCT 집합, 규칙 항목 `.`·`,`·`'`·`-`·`!`·`\` 포함) | CommitText(기호) | 일회성 끔 | FR-GRF-05, 05c ⑥ 단서, OD-4·OD-5 A |
| 4 | `t == LineLeft` | Backspace | 없음(대기 취소는 ⌫ 처리 순서 1번, 6절) | FR-GRF-05b, 05c ⑤ |
| 5 | `t == DiagDownLeft` | Enter | 일회성 끔 | FR-GRF-05b, 05c ④ |
| 6 | `t == DiagDownRight` | StateOnly | 일회성 끔 뒤 ext 켬 | FR-GRF-05, 05c ⑥ |
| 7 | `t == LineUp`, EN, `capsLock` | StateOnly | capsLock 끔, 일회성 끔(시프트 대기는 생기지 않음) | FR-GRF-05c ① |
| 8 | `t == LineUp`, EN, `shiftPending` | StateOnly | shift 끔, capsLock 켬(두 번 = 캡스록) | FR-GRF-05 |
| 9 | `t == LineUp`, KO, `shiftPending` | StateOnly | shift 끔(두 번 = 시프트 끔) | FR-GRF-05c ② |
| 10 | `t == LineUp`, 그 밖 | StateOnly | 일회성 끔 뒤 shift 켬 | FR-GRF-05, 05c ⑥ (구두점·확장 대기 중 ↑는 3.1 3번이 `!`·`¡`로 바꿔 3·2행에 먼저 닿는다) |
| 11 | `t == CornerTap` | ToggleLang | 일회성 끔 | FR-GRF-05, 05a, 05c ④⑥ |
| 12 | `t == Tap`, EN(두 영역) 또는 KO의 NUMBER 영역 | StateOnly | 일회성 끔 뒤 punct 켬 | FR-GRF-05 구두점 시프트, 05c ③(숫자 영역), ⑥ |
| 13 | `t == Tap`, KO의 LETTER 영역 | Element(DOT) | 일회성 끔 | FR-GRF-03, 05c ③(모음 요소) |
| 14 | `t == LineRight`, KO의 LETTER 영역 | Element(EU) | 일회성 끔 | GRAFFITI 6.3, 05c ③ |
| 15 | `t == LineRight`, 그 밖 | Space | 일회성 끔 | FR-GRF-06, 05c ④ |
| 16 | `t == LineDown`, KO의 LETTER 영역 | Element(I) | 일회성 끔 | GRAFFITI 6.3, 05c ③ |
| 17 | `t == LineDown`, NUMBER 영역 | CommitText("1") | 일회성 끔 | GRAFFITI 6.3, 05c ③(숫자 영역 획) |
| 18 | `t == LineDown`, EN의 LETTER 영역 | CommitText(`capsLock` 또는 shift면 "I", 아니면 "i") | 일회성 끔 | GRAFFITI 6.3, FR-GRF-02 |
| 19 | `t == Symbol`, 출력이 한글 자음 c | Consonant(shift이고 c에 쌍자음이 있으면 쌍자음, 아니면 c) | 일회성 끔 | FR-GRF-03a, 05c ③(시프트 + ㅋ = ㅋ) |
| 20 | `t == Symbol`, 출력이 한글 쌍자음(0.30부터 제품 템플릿에는 없음. 시험용 템플릿만 해당) | Consonant(쌍자음) | 일회성 끔 | FR-GRF-03a |
| 21 | `t == Symbol`, 출력이 SPACE_V(KO 문자 영역 V 획) | Space | 일회성 끔 | FR-GRF-06, 05c ④ |
| 22 | `t == Symbol`, 출력이 FINISH_J(KO 문자 영역 J 획) | Finish | 일회성 끔 | FR-GRF-05b, 05c ④ |
| 23 | `t == Symbol`, 출력이 영문 글자 | CommitText(`capsLock` 또는 shift면 대문자, 아니면 소문자) | 일회성 끔 | FR-GRF-02 |
| 24 | `t == Symbol`, 출력이 숫자·기호 | CommitText | 일회성 끔 | FR-GRF-04, 05c ③ |
| 25 | `t == Unknown` | RecognitionFailed | 없음(입력 없음) | FR-GRF-08 |

- 시프트·캡스록 세부는 SRS FR-GRF-05c·GRAFFITI 6.2-8(DCR-05 결정)대로다: 영문 캡스록 중 시프트 획 = 캡스록 끔(7행), 한글 시프트 두 번 = 시프트 끔(9행), 시프트가 쓰일 곳이 없는 획(쌍자음 없는 자음, 모음 요소, 숫자 영역 획)은 시프트 없이 해석하고 시프트는 사라짐(13·14·16·17·19·24행), 대기 중 다른 제어 획은 그 동작 + 대기 소멸(5·15·21·22행).
- 영문 대문자 = `capsLock` 또는 shift. 7행 때문에 영문에서 둘이 함께 켜진 상태는 생기지 않는다(HLD 5장).
- **대기 획끼리 겹칠 때(SRS FR-GRF-05c ⑥, HLD 5장, GRAFFITI 6.2-8, 설계질의 #6 Q2 확정):** 대기를 만드는 획(6·10·12행)과 한영 전환 획(11행)도 05c ④의 제어 획이라 앞 대기를 지운 뒤 자기 동작을 한다. 먼저인 것은 확장·구두점 기호표에 실린 획(2·3행, 확장 기호표에 탭은 없다), FR-GRF-05의 조합 1행(사선 + 탭 = 한영 전환)·8행(영문 시프트 두 번 = 캡스록), 05c ①②(7·9행)이다. 표 순서가 이 우선순위다.
- **기호표의 직선·탭 획(0.7, SRS FR-GRF-05c ⑥ 단서, GRAFFITI 6.2-8 ①·6.5, 오너 결정 #79 OD-4·OD-5 A):** 표는 바뀌지 않는다. 대기 중 기호표에 실린 직선·탭 획은 `Recognizer`가 규칙 토큰 대신 `Symbol(id)`를 내므로(3.1 3번) 2·3행에 닿는다. 그래서 제품 템플릿에서 구두점 대기 + 탭 = `.`(12·13행 아님), + ↙ = `,`(5행 아님, 길이 무관), + ↓ = `'`(16~18행 아님, 영역 무관), + → = `-`(14·15행 아님), + ↑ = `!`(10행 아님), + ↘ = `\`(6행 아님) / 확장 대기 + → = `±`, + ↙ = `×`, + ↑ = `¡`이다. 기호표에 없는 규칙 토큰은 그대로 와서 평소 행을 탄다: 확장 대기 + 탭 = 한영 전환(1행), 확장 대기 + ↘ = 확장 대기 다시 켬(6행, 결과적으로 확장 대기 그대로), 확장 대기 + ↓ = 그 영역의 평소 뜻(16~18행), 구석 탭 = 한영 전환(11행), ← = ⌫(4행 → 6절 순서 1번이 대기만 취소). 이 행들의 시험은 토큰 수준(GS)과 획 수준(GS-39~GS-45, `Recognizer` + 제품 템플릿)으로 나눈다(LLD-11 6.3).
- **확장·구두점 대기 뒤 기호표에 없는 획(SRS FR-GRF-05c ⑦, GRAFFITI 6.2-8, 오너 결정 #6 OD-2):** 제어 획이 아니면 대기를 지우고 평소대로 입력한다(13·14·16~20·23·24행, 예: 영문 구두점 대기 + 'a' 획 = a). 템플릿 획은 `recognize`가 기호표에서 못 찾으면 평소 집합으로 다시 찾으므로(3.1 4번) 이 행들에 닿는다. 평소 집합에서도 못 찾으면 25행(인식 실패, 대기 그대로).
- 인식 실패(25행)는 입력이 없으므로 대기·상태를 바꾸지 않는다(FR-GRF-08 "입력이 없고").

#### `cancelPending(): Boolean`
- 시프트·확장 시프트·구두점 시프트 대기 가운데 하나라도 있으면 셋 다 끄고 true. ⌫가 먼저 부른다(SRS 백스페이스 사례 "가 + 시프트 획"·"구두점 시프트 대기 중", FR-GRF-05c ⑤, HLD 5장, DCR-05 결정).

#### `clearOneShot()`, `enterMode()`
- `clearOneShot()`: shift·ext·punct 끔(모드 전환, 커서 이동, `onStartInputView`, 확정, HLD 5장 초기화).
- `enterMode()`: `clearOneShot()` + `capsLock = false`, `lang = KO`.

- 근거: FR-GRF-02~06, 05a~05c, 08, HLD 5장. 시험: GS-01~GS-45.

## 6. GraffitiInputHandler (app)

조립기·조합기·편집기를 묶는다. 안드로이드 없는 클래스다.

```kotlin
class GraffitiInputHandler(
    private val recognizer: () -> Recognizer,             // 0.36: 획마다 불러 지금 인식기를 받는다(11절). Recognizer를 바로 받는 보조 생성자도 있다
    private val state: GraffitiState, private val asm: CheonjiinAssembler,
    private val composer: () -> HangulComposer,           // 그래피티 정책 조합기(LLD-08 6절)
    private val out: ComposingController, private val sink: InputSink,
    private val flash: (String) -> Unit,                  // GraffitiPadView.flash(8.3)
    private val redraw: () -> Unit = {})                  // 패드 상태 표시 다시 그리기(8.1, 0.11). 서비스가 { pad?.invalidate() }를 넘김
interface InputSink {                                      // InputDispatcher가 구현(LLD-08 6절)
    fun markInput(); fun error()
    val hidesDisplay: Boolean                              // FieldInfo.hidesDisplay(비밀번호 입력창만, FR-CMN-08 ①). 학습 금지 칸은 false(②, 0.21)
    val field: FieldInfo                                   // 지금 입력창. Enter가 ComposingController.enter(f)에 넘김(G2)
}
```
- 아래 "typed" 열이 "예"인 동작 뒤에 `sink.markInput()`을 부른다.

#### `onStroke(stroke: InkStroke)`
```
expect = when { state.extPending → EXTENDED; state.punctPending → PUNCT; else → NORMAL }
t = recognizer.recognize(stroke, state.lang, expect)
a = state.interpret(t, stroke.area)
handle(a)                                   // Element의 push가 Ignored·NotExtendable이면 dropped = true(handle 표)
g = glyph(a)
if g != "" && !dropped && !sink.hidesDisplay: flash(g)   // FR-GRF-07, FR-CMN-08 ①(0.21: 학습 금지 칸에서는 보임)
```
- `glyph(a)` = 인식한 글자: Consonant(c) → c(쌍자음 포함), Element(e) → "ㅣ"·"ㅡ"·"ㆍ", CommitText(s) → s(영문·숫자·기호). 그 밖(Space·Enter·Backspace·Finish·ToggleLang·StateOnly·RecognitionFailed)은 ""(0.7, PR #104).
- flash하지 않는 경우(0.7, 이슈 #105): ① 글자가 없는 획 — 시프트·대기 획·⌫·스페이스·리턴·확정·한영 전환·인식 실패(SRS FR-GRF-07 "인식한 글자만", 이슈 #74). ② 조립기가 무시한 요소 — `push`가 `Ignored`(‥ 뒤 세 번째 탭)·`NotExtendable`(ㅐ + 탭, ㅣ + ㅣ 등)(SRS FR-HGL-04c·FR-GRF-07, 오너 결정 #74 A). 무시한 요소도 typed는 참이다(FR-CMN-03a). ③ 비밀번호 입력창(FR-CMN-08 ①, DCR-19 결정. 0.21: 학습 금지 칸은 FR-CMN-08 ②로 보인다).
- 시험: GH-35, GH-36, GH-37(LLD-11 6.4).

#### `handle(a: GraffitiAction)`
| 동작 | 처리 | typed |
|---|---|---|
| Consonant(c) | 조립기가 Vowel이면 `asm.close()`, Pending이면 `asm.clear()`(임시 글자 버림). `composer.apply(Consonant(c))` → `show` | 예 |
| Element(e) | `r = asm.push(e)`: Started(v) → `composer.apply(Vowel(v))`(도깨비불도 이때, FR-HGL-04e) / Changed(v) → `apply(ReplaceVowel(v))` / PendingShown → 조합기 그대로 / Ignored·NotExtendable → 아무것도 안 함(무시, FR-HGL-04c·04e), `dropped = true`(onStroke가 flash하지 않음). 그다음 `show`. `handle`은 시작할 때 `dropped = false`로 둔다 | 예(천지인 요소 획, 무시된 요소 포함 — FR-CMN-03a) |
| CommitText(s) | `finishAll()` 뒤 `out.commitText(s)` (FR-GRF-04 "먼저 확정") | 예 |
| Space | `finishAll()` 뒤 `out.commitText(" ")` | 예 |
| Enter | `finishAll()` 뒤 `out.enter(sink.field)`(LLD-08 4절, G2) | 예 |
| Finish | `finishAll()` | 아니오(조합 강제 확정 'J' 획, FR-CMN-03a, DCR-17 결정) |
| Backspace | `backspace()` | 예 |
| ToggleLang | `finishAll()`(조합 확정, 임시 글자 버림) 뒤 `state.lang` 전환(SRS FR-GRF-05a, GRAFFITI 6.2-10, HLD 5장, DCR-06 결정) | 아니오 |
| StateOnly | 없음 | 아니오(FR-CMN-03a) |
| RecognitionFailed | `sink.error()`(오류 진동). 편집기·조합기·조립기 그대로 | 아니오(입력 없음, FR-GRF-08, DCR-07 결정) |

- `show` = `out.show(ComposeResult, asm.pendingGlyph())` — 조합 텍스트 뒤에 임시 글자를 붙여 `setComposingText`(HLD 5장).
- `finishAll()` = `asm.clear()`; `composer.finish()`; `out.finishComposing()`. 편집기 조합 구간에 임시 글자가 있으면 `ComposingController.finishComposing`이 먼저 임시 글자를 뺀 조합 텍스트로 바꾼 뒤 확정한다(LLD-08 4절). 그래서 임시 글자는 편집기에 남지 않는다.
- 임시 글자(ㆍ·‥)는 **모든 확정에서** 버린다: 자음·스페이스·엔터·확정 획·모드 전환과, 그 밖의 이유로 조합이 확정될 때(숫자 영역 획, 영문·기호 입력, 그래피티 안의 한영 전환, 커서 이동, 입력창 변경). 조합기에 넘기지 않은 요소라 확정할 모음이 없다(SRS FR-HGL-04e, HLD 5장, DCR-21 결정). 모든 경로가 `finishAll()`(또는 LLD-08의 `finishAll`·`onCursorMoved`)을 거치므로 따로 처리하지 않는다.

#### `backspace()` — ⌫ 순서 (FR-CMN-07b, HLD 5장)
```
1. if state.cancelPending(): return                       // 대기만 취소, 글자 안 지움. ⌫ 획(←)도 대기 중 기호표에서 찾지 않으므로(3.1 3번) 늘 여기에 닿는다(OD-3 A, GH-37)
2. r = asm.pop()
   when r:
     ToVowel(v)            → composer.undoLast(); show                       // 0.16: 그 요소의 ReplaceVowel 단위(LLD-04 7.1). 결과 중성 = v
     ToPending(d)          → if (pop 전 상태가 Vowel) composer.undoLast()   // 거 → ㄱㆍ
                             show
     ToNone(wasVowel)      → if wasVowel composer.undoLast(); show          // 기 → ㄱ
     Empty                 → 3번으로
3. b = composer.backspace()
   Handled(res)            → show(res)
   NothingComposing        → out.deleteBefore()                              // 확정 글자 한 글자
```
- 조합기 단위 목록에는 조립기가 성립시킨 `Vowel` 하나와 그 뒤 요소마다 `ReplaceVowel`이 쌓인다(0.16, LLD-04 7.1). 그래서 `ToVowel`은 마지막 `ReplaceVowel` 하나를, 모음이 성립하지 않게 되면(`ToPending`·`ToNone`) `Vowel` 하나(와 도깨비불)를 `undoLast()` 한 번으로 되돌린다. 조립기가 닫힌 뒤(받침)에는 `Empty`라 조합기 ⌫로 가고, 그때도 요소 하나씩 지운다(간 ⌫ = 가, ⌫ = 기, ⌫ = ㄱ, SRS FR-CMN-07b). 닫힌 모음은 다시 열지 않는다(BS-17).
- 도깨비불이 일어난 뒤의 되돌림은 확정 경계를 넘지 않는다: 각 + 탭 + ㅣ(= 가거) ⌫ = 가ㄱㆍ(앞 가는 확정됨, FR-CMN-07 확정 경계와 두벌식 "가가 ⌫ = 가ㄱ" 사례와 같은 규칙). 시험 GH-24.
- 반복: 보조 줄 ⌫를 누르고 있으면 `RepeatTimer`(LLD-05A 6절)가 이 함수를 되풀이한다(FR-CMN-07c).

#### `enterMode()`
- `state.enterMode()`를 부르고 `redraw()`(8.1 패드 상태 표시, 0.11). 상태는 이 클래스 안에 있으므로 `InputDispatcher.onModeKey`·`KeyboardService.onStartInputView`는 그래피티에 새로 들어올 때 이 함수를 부른다(LLD-08 3.1, 6.1, G6).

#### `reset(reason)`
- 커서 이동·입력창 변경·모드 전환: `asm.clear()`, `state.clearOneShot()`, `redraw()`(조합 확정은 호출자가 먼저 함, LLD-08 6절). `redraw()`는 패드 밖에서 온 초기화(커서 이동 등)로 대기가 사라질 때 대기 표시를 지우려고 부른다(8.1, 0.11).
- 근거: HLD 5장 초기화. 시험: GH-01~GH-25.

#### `snapshot()`, `restore(s)` (0.5, 이슈 #75)
```kotlin
data class GraffitiSnapshot(val asm: CjSnapshot, val state: GraffitiStateSnapshot)
fun snapshot(): GraffitiSnapshot = GraffitiSnapshot(asm.snapshot(), state.snapshot())
fun restore(s: GraffitiSnapshot) { asm.restore(s.asm); state.restore(s.state); redraw() }   // redraw: 8.1(0.11)
```
- 조립기·상태는 이 클래스 안에 있으므로 `InputDispatcher.snapshot()`·`restore()`(LLD-08 6.1)가 이 함수를 부른다(LLD-09 2절). 조합기는 `InputDispatcher`가 따로 되돌린다(같은 조합기를 두 번 넣지 않음).
- 근거: HLD 9장, NFR-03, LLD-09 2절. 시험: EG-06.

## 7. 템플릿 JSON 형식

위치: `recognizer/src/main/resources/graffiti/templates.json`. 시험·평가 샘플은 따로 둔다(LLD-11 1절, NFR-09).

**오너 획 템플릿(0.30, FSS#64):** `KO_LETTER`의 그린 템플릿 뒤에 오너가 손으로 쓴 자음 획 117개를 더한다(원본 `strokes/tools/owner_strokes.json`, 13글자, 글자당 8~10획). id는 `<그린 템플릿 id>_oN`이고, 획마다 코너 수가 달라 한 획에 id 하나다. 인식 전용이라 안내표 칸이 아니다. ㅋ은 넣지 않는다(오너 ㅋ이 실기 ㄱ을 가져간다). 점수 계산은 3.2절 그대로다.

**영문 오너 획 템플릿(0.34, FSS#80, FR-GRF-03b):** `EN_LETTER`의 그린 템플릿 뒤에 오너가 손으로 쓴 글자 획 156개를 더한다(i를 뺀 25글자, 글자당 4~10획). 원본은 시험이 함께 읽는 `strokes/owner_en/day1`(2026-11-05)·`day2`(2026-11-06)의 파일이고, 파일 이름이 템플릿과의 관계를 정한다. `en_<글자>_oN` = 그 id의 템플릿. `en_i_rN` = 직선 규칙(↓ = i)이라 템플릿이 아니다. `en_<글자>_xN` = 측정으로 뺀 획이다: u 2획(`en_u_x1`·`x2`, 실기 V `device/en/en_v_01`이 u 0.955 > v 0.935로 읽혔다, GR-40)과 d 4획(`en_d_x1`·`x3`·`x4`·`x7`, 아래쪽에서 시계 방향으로 시작한 그린 고리 5개가 d로 읽혔다, GR-33). 뺀 획도 시험 표본으로는 남고, 번호는 다시 매기지 않는다. 인식 전용이라 안내표 칸이 아니다. 점수 계산은 3.2절 그대로다. 인식은 획이 끝날 때 한 번 돌아 NFR-02(이동 이벤트당)의 대상이 아니다. 참고로 영문 획 하나의 인식 시간은 개발 PC JVM에서 0.105 ms → 0.532 ms로 늘었다(손 획 168개 평균, 폰은 재지 않음).

**Q 변형 `en_q2`(0.32, FSS#70):** `en_q`는 꼬리가 고리 폭 안에서 끝나(끝 x = 0.95) 고리가 상자를 채운다. 꼬리가 고리 오른쪽 밖으로 나가는 Q는 정규화 뒤 고리가 상자 폭의 일부로 좁아져 `en_q`에서 멀어지고 `en_v`(√)에 가까워진다. `en_q2`는 고리 폭 0.45에 꼬리가 x = 1까지 나가는 그린 획이다. 오너 획이 아니다. 인식 전용이라 안내표 칸이 아니고, `gen_strokes.py`의 샘플 순서를 흔들지 않도록 `TEMPLATES` 밖의 `EXTRA_TEMPLATES`에 둔다.

```json
{
  "version": 1,
  "sets": {
    "KO_LETTER": [
      { "id": "ko_g",   "output": { "type": "jamo", "value": "ㄱ" }, "corners": 1,
        "strokes": [ [[0,0],[1,0],[1,1]] ] },
      { "id": "ko_space_v", "output": { "type": "control", "value": "SPACE_V" }, "corners": 1, "strokes": [] },
      { "id": "ko_finish_j", "output": { "type": "control", "value": "FINISH_J" }, "corners": 1, "strokes": [] }
    ],
    "EN_LETTER": [ { "id": "en_a", "output": { "type": "text", "value": "a" }, "corners": 1, "strokes": [] } ],
    "NUMBER":    [],
    "PUNCT":     [ { "id": "p_period", "output": { "type": "text", "value": "." }, "corners": 0, "rule": "TAP", "strokes": [] } ],
    "EXTENDED":  []
  }
}
```

| 칸 | 뜻 |
|---|---|
| `id` | 템플릿 이름(파일 안에서 유일) |
| `output.type` | `jamo`(한글 자음. 쌍자음은 형식상 가능하지만 0.30부터 시험용 템플릿에만 있음) / `text`(영문·숫자·기호, 영문은 소문자) / `control`(`SPACE_V`, `FINISH_J`) |
| `corners` | 기대 코너 수(3.2 5단계) |
| `strokes` | 대체 획 목록. 획 하나 = 0~1 정규화 좌표 열, 시작점이 첫 점(GRAFFITI 표의 ●) |
| `rule` | (선택, 0.7) 규칙 항목: 이 기호가 직선·탭 규칙 토큰(3.1 1·2번) 하나와 같은 모양임. 값 `TAP`·`LINE_DOWN`·`LINE_UP`·`LINE_RIGHT`·`DIAG_DOWN_LEFT`·`DIAG_DOWN_RIGHT`. 이때 `strokes`는 `[]`, `corners`는 0 |

- `rule` 규칙(어기면 `IllegalStateException`): PUNCT·EXTENDED 집합에만 둔다(평소 집합의 직선·탭은 GraffitiState 표가 뜻을 정함, 5.2). `LINE_LEFT`(⌫, OD-3 A)·`CORNER_TAP`(한영 전환)과 그 밖의 값은 거부. 한 집합에 같은 `rule`은 하나. `rule`이 있으면 `strokes`가 비어 있어야 한다. `rule` 칸이 없는 파일(0.6 형식)도 그대로 읽히므로 `version`은 1 그대로다.
- `strokes`가 빈 템플릿은 `TemplateMatcher`가 건너뛴다. 대체 획마다 코너 수가 다르면 `corners`가 하나로 맞지 않으므로 다른 id로 나눈다(예: B ①과 ② = `en_b`·`en_b2`, 같은 출력).
- 획 모양의 실제 데이터는 GRAFFITI 6.1(영문 글자 26, 대체 획 포함)·6.3(숫자 10)·6.4(구두점 33·확장 기호)의 말로 적은 원본 획 표를 보고 템플릿 작업에서 직접 점 열로 그린다(GPL 코드·다른 제품 그림 복제 금지, CON-6, 오너 결정 #79 OD-1·OD-2 A). 위 좌표는 형식 예시다. 한글 자음은 GRAFFITI 4.2 그대로다.
- 표가 **미확인** 칸을 채운 획(GRAFFITI 6.1·6.3·6.4, SRS TBD-11)의 처리는 GRAFFITI 6장 머리·6.4 끝을 따른다(실기기 획 수집 뒤 확정, GRAFFITI 7장):
  - **T28 임시 템플릿에 이미 있는 것**은 그 임시 획을 유지하고 `TEMPLATES.md`에 `interim`으로 표시한다: B ①(세로선 되짚음), D ①, H(되짚는 구간), G ①(막대 방향), O·0(회전 방향, 두 방향 모두 둠), 8 ①(진행 방향), X ①(한 획 경로), °(Ø와 구분), €(`x_euro`, E 모양 — GRAFFITI 6.4 "획 미확인. 그때까지 T28 임시").
  - **T28 임시 템플릿에 없는 것**은 넣지 않는다(GRAFFITI 6.4 끝 "획이 확정되어 템플릿이 생기기 전까지 인식 대상이 아니다"): 쐐기형 `_` `|` `` ` `` `;` `:`, P ①·R ①(되짚는 구간), 8 ②(진행 방향), ∫(기호 자체 미확인), Ø(°와 같은 모양). 기존 `en_p`·`en_r`(줄기 아래에서 시작하는 ②)은 그대로 두고, 되짚는 ①만 더하지 않는다.
  - 미확인 칸이 빈 획은 원본 표 설명대로 그린다(없던 것은 더하고, 모양이 다른 것은 바꾼다).
- 원본 표에도 4.2 제어 획 표에도 없는 템플릿은 두지 않는다(예: 닫힌 4 모양 `num_4b`). 원본 표의 `•`(확장 + 탭)는 한영 전환과 겹쳐 배정하지 않는다(OD-7 A). 영문 K는 원본 α 고리다(OD-6 A, 한글 ㅈ와 같은 모양이지만 집합이 달라 부딪히지 않음).
- 직선 규칙으로 처리하는 글자(영문 I = 문자 영역 ↓, 숫자 1 = 숫자 영역 ↓, 5.2 17·18행)는 템플릿을 두지 않는다. 기호표의 직선·탭 기호는 `rule` 항목으로 둔다(3.1 3번). 쐐기형 기호가 템플릿에 없는 동안 구두점 대기 + 쐐기 획은 3.1 4번에서 기호표에 맞는 것이 없어 평소 집합으로 간다(05c ⑦).
- 읽기: `TemplateLoader.load(stream): TemplateSet`. `version != 1`이거나 `id` 중복이거나 위 `rule` 규칙을 어기면 `IllegalStateException`(앱 시작 시 한 번, 실패하면 그래피티 패드는 인식 실패만 낸다, LLD-09 4절). 시험 GR-31.

### 7.1 모델 파일 형식 (0.37, FSS#96, SRS FR-GRF-13)

위치: `recognizer/src/main/resources/graffiti/graffiti-nn.bin`(템플릿 JSON과 같은 자리, `GraffitiModelLoader.RESOURCE`). 만드는 스크립트는 `tools/graffiti-nn/`(절차는 그 폴더의 README). **모델 파일과 시험용 대조 표본 `recognizer/src/test/resources/graffiti/nn-parity.json`은 한 번의 내보내기에서 나온 짝이다** — 다시 학습하면 가중치가 비트 단위로 같지 않을 수 있으므로 둘을 함께 간다.

모두 리틀 엔디언이다. `f32` = IEEE 754 32비트.

| 순서 | 내용 | 형식 |
|---|---|---|
| 1 | 표지 | 4바이트 `"FSGN"` |
| 2 | 판 번호 | `u32` = 1 |
| 3 | 특징: 격자, 방향 수, 가운데 채널 수 | `u32` × 3 = 8, 8, 1 |
| 4 | 가운데 채널: 걸음 문턱(dp), 걸음당 값 | `f32` × 2 = 1.0, 0.25 |
| 5 | 임베딩 차원 | `u32` = 32 |
| 6 | 몸통 수 B | `u32` = 5 |
| 7 | 몸통 B개, 하나마다 | `f32` × 22,368: 합성곱 1 가중치 16 × 9 × 3 × 3, 편향 16, 합성곱 2 가중치 32 × 16 × 3 × 3, 편향 32, 완전연결 가중치 32 × 512, 편향 32 (3.5의 순서) |
| 8 | 집합 수 S | `u32` = 5 |
| 9 | 집합 S개, 하나마다 | 이름(`u8` 길이 + ASCII, `SetName`의 이름), 몸통 번호 `u32`, 글자 수 `u32`, 글자마다(`u16` 길이 + UTF-8 = 템플릿 `output.value`), 예시 수 `u32`, 예시마다(`u16` 글자 번호 + `f32` × 32 = 길이 1인 임베딩) |

- 읽기: `GraffitiModelLoader.load(stream): GraffitiModel`. 표지·판 번호·3~5번 값이 `StrokeFeatures`·`NnBody`의 상수와 다르거나, 몸통 수·집합 수가 1~5 밖이거나, 집합 이름이 `SetName`이 아니거나 두 번 나오거나, 몸통 번호·글자 번호가 범위 밖이거나, 글자가 겹치거나, **예시가 없는 글자**가 있거나, 파일이 중간에 끝나거나 마지막 집합 뒤에 바이트가 남으면 `IllegalStateException`이다. 스트림은 닫지 않는다. 시험 GM-07·GM-08.
- 형식을 바꾸면 판 번호를 올린다. 앱은 자기 리소스의 파일만 읽으므로 옛 판을 읽는 코드는 두지 않는다.
- **출고 파일의 구성(이 판, 861,889바이트):**

| 항목 | 바이트 | 내용 |
|---|---|---|
| 머리(1~6번) | 36 | |
| 몸통 5개 | 447,360 | 하나 89,472 = 합성곱 1 5,248 + 합성곱 2 18,560 + 완전연결 65,664. 집합마다 하나(FR-GRF-13 ②) |
| 집합 수 | 4 | |
| `EN_LETTER` | 124,377 | 글자 25, 예시 956 = 합성 32 × 25 + 오너 획 156 (예시 124,280) |
| `KO_LETTER` | 81,881 | 글자 16(자음 14 + `SPACE_V` + `FINISH_J`), 예시 629 = 32 × 16 + 오너 획 117 (예시 81,770) |
| `NUMBER` | 37,486 | 글자 9, 예시 288 (37,440) |
| `PUNCT` | 91,604 | 글자 22, 예시 704 (91,520) |
| `EXTENDED` | 79,141 | 글자 19, 예시 608 (79,040) |
| 합 | 861,889 | 몸통 447,360(52 %) + 예시 3,185개 × 130 = 414,050(48 %) + 이름·글자 목록·머리 479 |

- **줄일 수 있는 선택지(이 PR은 32비트 그대로 낸다, FSS#96 작업지시서 v3 3절).** 영향은 출고 파일을 그대로 줄인 값으로 파이썬에서 잰 것이다 — 대조 표본 440획(NN-02의 고정 표본)과 GR-33 고리 864획의 최종 답(규칙 제외, 두 문턱, 대기 집합 먼저)이 32비트와 몇 개 달라지는가. 다시 학습하지 않았다.

| 선택지 | 크기(계산) | 대조 440획 답 변화 | 고리 864획 답 변화 | 임베딩 최대 차이 |
|---|---|---|---|---|
| 몸통 가중치 16비트 부동소수 | 몸통 447 → 224 kB | 0 | 0 | 1.7 × 10⁻⁴ |
| 몸통 가중치 8비트 정수(텐서마다 눈금 하나) | 447 → 112 kB | 0 | 0 | 9.2 × 10⁻³ |
| 몸통 가중치 8비트 정수(출력 채널마다 눈금) | 447 → 약 113 kB | 0 | 0 | 6.4 × 10⁻³ |
| 예시 16비트 부동소수(읽을 때 길이 1로) | 예시 414 → 210 kB(예시당 130 → 66바이트) | 0 | 0 | – |
| 예시 8비트(값이 0~1이라 눈금 1/255 고정, 읽을 때 길이 1로) | 414 → 108 kB(예시당 34바이트) | 0 | 0 | – |
| 합성 예시 글자당 32 → 16 | 예시 1,456개 × 130 = 189 kB 줄어듦 | 0 | 1(862 / 864) | – |
| 합성 예시 글자당 32 → 8 | 2,184개 × 130 = 284 kB 줄어듦 | 0 | 1(862 / 864) | – |
| 몸통 16비트 + 예시 16비트 | 862 → 약 434 kB | 0 | 0 | 1.7 × 10⁻⁴ |
| 몸통 8비트(채널) + 예시 8비트 | 862 → 약 221 kB | 0 | 0 | 6.4 × 10⁻³ |
| 몸통 하나를 다섯 집합이 함께 | 몸통 4개 = 약 358 kB 줄어듦, 대기 중 순전파 1번 | 다시 학습해야 해서 재지 않음 | – | 영문은 같고 한글 손가락 출고 −2.4 %p(범위 −5.4 ~ −0.6, 유의한 시드 5개 중 1개 — 구분되지 않음. FSS#96 작업지시서 v2 2절) |

  - 32비트(지금)에서 고리 864획 가운데 863획이 맞다(틀린 하나는 GR-33의 알려진 한계, LLD-11 6.7). 합성 예시를 줄이면 고리 하나가 더 틀린다.
  - 16비트·8비트로 바꾸면 Kotlin 대조(NN-03, 허용 오차 10⁻⁵)는 지금 값 그대로는 맞지 않는다 — 대조 표본도 그 형식으로 다시 내고 허용 오차를 정해야 한다. 이 판에서는 바꾸지 않는다.
  - **APK에 더해지는 크기(실측, 깨끗이 다시 빌드한 debug APK):** develop(a87ddd8) 13,049,722바이트 → 이 PR 13,612,247바이트, **+562,525바이트(약 549 kB)**. 그 가운데 모델 파일이 APK 안에서 529,574바이트(원 861,889바이트, deflate 압축 61 %)이고 dex가 +28,638바이트다. 부동소수 값도 압축으로 39 % 줄었다.
- **글자 목록:** 집합의 글자 목록은 그 집합에서 템플릿 획이 있는 템플릿의 출력을 파일 순서로 모은 것과 같아야 한다(GM-02). 템플릿의 글자 구성이나 내장 오너 획을 바꾸면 모델과 대조 표본을 다시 만든다(HLD 5.1 "모델 자산").
- **출고 예시:** 합성 예시는 학습에 쓴 합성 획 풀에서 글자마다 32개를 고른 것(고정 시드)이고, 오너 획 예시는 내장 템플릿의 `_oN` 획과 같은 획(`strokes/owner_en/`의 `en_<글자>_oN` 156개, 한글 117개)의 임베딩이다. 한글 117개는 기록 원본에서 냈고, 저장소의 `strokes/tools/owner_strokes.json`은 그 원본을 0.1 dp로 반올림한 사본이라 `tools/graffiti-nn/export.py`로 다시 내면 값이 최대 0.021 다르다(대조 표본 440획의 답은 같음, 그 폴더의 README). 가중치 학습에는 합성 획만 쓴다(FR-GRF-13 ⑤). 시험 GM-03~GM-06.

## 8. GraffitiPadView와 InkCollector (app)

### 8.1 배치 (FR-GRF-01)

- 전체 높이 = 5 × 줄 높이(기본 240dp), 창 바닥 정렬(LLD-08 7절).
- 패드 = 위 4줄 높이, 보조 버튼 줄 = 아래 1줄(왼쪽부터 모드 키, 스페이스, ⌫, ↵).
- 보조 줄 버튼 폭: 모드 키 = 72dp(탭 자판 모드 키와 같은 폭, 360dp 기준 값을 탭 자판과 같이 `72 / 360 × 창 폭`으로 늘림), 스페이스·⌫·↵ = 남은 폭을 셋이 같게 나눔(SRS FR-GRF-01, TBD-11 확정, HLD 5장 보조 줄, DCR-12 결정). 그래서 모드 키의 화면 위치·폭이 탭 자판과 같다.
- 패드 가로: 문자 영역 : 숫자 영역 = 설정 비율. 고를 수 있는 값은 1 : 1 / 2 : 1(기본) / 3 : 1 셋이다(SRS FR-SET-05, 오너 결정 #108 A, GRAFFITI 6.3). 저장값은 문자 영역의 몫 1/2 / 2/3 / 3/4이고 `GraffitiPadGeometry(letterRatio)`가 폭 × 그 몫을 문자 영역으로 쓴다(저장 형식·목록 밖 값 처리는 LLD-07 6.1).
- 숫자 영역 위에 흐린 "123"(GRAFFITI 6.3).

#### 인식 영역 경계 (0.28, SRS FR-GRF-01a, HLD 5장, FSS#48)

- 테두리: `letterRect`·`numberRect`를 네 변 모두 `AREA_INSET_DP` = 4dp 안으로 줄인 사각형(px로 바꾼 뒤 선 두께의 반만큼 더 안으로 — 선이 그 사각형 안쪽에 그려지게), 둥근 모서리 반지름 `AREA_RADIUS_DP` = 6dp, `Paint.Style.STROKE`, 선 두께 `AREA_STROKE_DP` = 1dp, 색 `0xFF505050`("123"과 같은 흐린 회색, 검은 배경 `KeyboardRootView.BACKGROUND_COLOR`). 자판은 어두운 모양 하나뿐이라 테마별 색은 없다.
- 그래서 두 영역 사이에 8dp 틈, 패드 위·양옆·아래(보조 줄 쪽)에 4dp 틈이 생긴다. 0.27까지의 구분선(영역 사이 세로선, 패드 아래 가로선)은 이 틈이 대신하므로 그리지 않는다.
- 영역 판정(`areaAt` = 시작점 x가 `letterRect.right`보다 작으면 문자 영역)과 영역 크기는 바꾸지 않는다. 테두리 밖 틈에서 시작한 획도 그 틈이 속한 영역의 획이다. 패드 밖(보조 줄)에서 시작한 손가락은 지금처럼 버튼이다(8.3). 영역 밖 시작 획 처리를 바꾸는 일은 FSS#47에서 정한다.
- 늘 그린다: 상태와 상관없고 입력 글자가 아니므로 `sink.hidesDisplay`(비밀번호)·학습 금지와 상관없다. 비율을 바꾸면(FR-SET-05) `rebuildGeometry`가 새 사각형으로 다시 그린다. 입력 연습(LLD-07)은 같은 `GraffitiPadView`라 같다.
- 그리는 순서: 테두리 → "123" → 상태 표시 → 보조 줄 버튼 → 인식 글자 → 획 자국(획 자국이 맨 위).

#### 패드 상태 표시 (0.11, SRS FR-GRF-05d, GRAFFITI 4.2 패드 상태 표시, HLD 5장, 이슈 #136)

상태의 정본은 `GraffitiState`다. 뷰는 `onDraw`마다 `handler.snapshot().state`를 읽어 그리기만 한다(따로 저장하지 않음). 그림은 점·글자·화살표 문자뿐이고 다른 제품의 그림을 옮기지 않는다.

| 표시 | 자리 | 그림 | 조건 |
|---|---|---|---|
| 구석 탭 점 | `GraffitiPadGeometry.cornerRect`: 문자 영역 왼쪽 위, 폭·높이 × `StrokeRules.CORNER_RATIO`(= 0.15). 인식기(`StrokeRules.inCorner`)와 같은 상수·같은 사각형(`letterRect`)이라 표시와 판정 영역이 어긋나지 않는다 | 반지름 `CORNER_DOT_DP` = 2dp 원, 중심 = 영역 왼쪽 위에서 (`AREA_INSET_DP` + 3r, `AREA_INSET_DP` + 3r) = (10, 10)dp — 테두리 선(4dp)과 둥근 모서리 안쪽(0.28; 0.27까지 (1.5r, 1.5r)) | 늘 |
| 언어 글자 | 같은 영역에서 점 오른쪽(점 오른쪽 끝 + r)부터 영역 오른쪽 끝까지의 가운데, 세로는 영역 가운데 | `langLabel(lang)` = KO → "한", EN → "A". 글자 크기 = min(줄 높이 × 0.4, 영역 높이 × 0.6) | 늘 |
| 대기 표시 | 패드 오른쪽 위 구석(숫자 영역 오른쪽 위, 오른쪽 정렬, 안쪽 여백 `AREA_INSET_DP` + 6dp = 10dp — 테두리 안, 0.28; 0.27까지 6dp) | `pendingLabel(state)` = 켜진 것만 ⇪ ⇧ • ↘ 순서로, 사이에 공백 하나. 글자 크기 = 줄 높이 × 0.4 | 켜진 것이 있을 때 |

- 색: 점·언어 글자는 흐린 회색 `0xFF5A5A5A`(숫자 영역 "123" `0xFF505050`과 비슷한 밝기), 대기 표시는 밝은 회색 `0xFFB0B0B0`(0.10의 ↘와 같음).
- 대기 표시 순서는 캡스록(계속 남음)을 앞에 둔다. 함께 켜지는 것은 ⇪ + •, ⇪ + ↘뿐이다(GRAFFITI 4.2 2번, 6.2-8). 캡스록은 한글 언어에서도 켜져 있으면 보인다(GRAFFITI 4.2 2번 "켜져 있는 동안 계속", SRS FR-GRF-05d 문구 그대로).
- 비밀번호·학습 금지 입력창에서도 그린다. `sink.hidesDisplay`를 보지 않는다(FR-GRF-05d. FR-CMN-08 ①이 끄는 것은 `flash`뿐).
- 다시 그리는 때: 패드 획·보조 줄 버튼은 `onTouchEvent`가 이미 `invalidate()`한다(한영 전환·대기 켬·⌫ 대기 취소 포함). 패드 밖에서 바뀌는 때(`enterMode`·`reset`·`restore`)는 `GraffitiInputHandler`가 `redraw()`를 부른다(6절). 그래서 표시는 그 획(또는 초기화) 직후 프레임에 바뀐다.
- 비율을 바꾸면(FR-SET-05) `letterRect`와 함께 `cornerRect`도 바뀐다.
- 시험: PI-01~PI-08(LLD-11 6.5), 실기기 DV-25.

### 8.2 InkCollector

```kotlin
class InkCollector(private val density: Float) {
    fun onDown(e: MotionEvent, idx: Int, areaOf: (Float, Float) -> PadArea)
    fun onMove(e: MotionEvent)        // 히스토리 점까지
    fun onUp(e: MotionEvent): InkStroke?
    fun cancel()
}
```
- 포인터 하나만 받는다. 획 진행 중 다른 포인터는 무시(FR-CMN-11). 영역은 **시작점**으로 정한다(GRAFFITI 6.3).

### 8.3 `GraffitiPadView.onTouchEvent`

```kotlin
class GraffitiPadView(context: Context, private val sink: TapSink,          // 보조 줄 기능키(InputDispatcher, LLD-05A 4절과 같은 경계)
                      private val handler: GraffitiInputHandler, private val guard: EngineGuard,
                      private val feedback: FeedbackPort) : View(context)
```

```
DOWN: 보조 줄 안 → feedback.key()(키 입력 피드백은 누를 때, FR-CMN-11a ②)
                   모드 키(0.22, #205): 입력 없음. KeyGeom(키 사각형, 창 = 패드 뷰, 0.35: sideDp = sideMarginPx / density — LLD-08 7.2) + TapGestureClassifier(minCrossDp).begin(p)
                   그 밖 기능키는 누를 때 처리(FR-CMN-11a ①): guard.stroke("Graffiti.fn") { sink.onFunctionDown(key) }
      (⌫는 InputDispatcher.backspace → handler.backspace, LLD-08 6절). ⌫면 RepeatTimer 시작
      패드 안 → InkCollector.onDown, 잉크 그리기 시작. 키가 아니므로 키 입력 피드백 없음(FR-CMN-11a ②, GRAFFITI 6.2-9)
MOVE: 모드 키를 잡고 있으면 그 포인터의 히스토리 점·현재 점을 분류기에. InkCollector.onMove, invalidate(잉크)
UP:   패드 획 → stroke = onUp → guard.stroke("Graffiti.onStroke") { handler.onStroke(stroke) }; 잉크 지움   // 실패면 상태 되돌림, 입력 없음(LLD-09 2절)
      모드 키 → s = guard.run("Graffiti.fn") { ModeKeyGesture.of(c.finish(p)) == OPEN_SETTINGS }; s == true → sink.onSettingsStroke(), false → sink.onFunctionDown(MODE) → onFunctionUp(MODE)
      보조 줄 기능키 → sink.onFunctionUp(key); ⌫면 RepeatTimer 중지
CANCEL: 진행 중 획 버림(InkCollector.cancel, 잉크 지움, 입력 없음), 반복 중지, 이미 처리한 기능키는 되돌리지 않음(FR-CMN-11a ③, HLD 4.1). 떼지 않은 모드 키는 입력 없음(0.22)
```

- **`onInk: ((InkStroke) -> Unit)?`(0.38, FSS#96):** 값이 있으면 패드 획을 `handler.onStroke` 대신 여기로 넘긴다(`guard.stroke("Graffiti.onInk")` 안에서). 내 획 학습의 쓰기 화면만 쓴다(LLD-07 8.3) — 그 화면의 획은 글자로 입력되지 않는다. 키보드와 연습 주제는 `null`이라 지금과 같다. 패드의 상태 표시(언어 글자)는 그대로 `handler`의 상태에서 그린다. 시험 MV-02·MV-03(LLD-11 7.10).

#### `flash(label: String)`
- 인식한 글자를 패드 위에 `FLASH_MS = 600ms` 보인다(FR-GRF-07, 표시 시간 D-13). 부르는 쪽(`onStroke`)이 비밀번호 입력창(`hidesDisplay`, FR-CMN-08 ①. 학습 금지 칸은 보임, ②, 0.21)과 인식 실패에서는 부르지 않는다(SRS FR-CMN-08, FR-GRF-07, GRAFFITI 6.2-6, HLD 5장, DCR-19 결정).

## 9. 시퀀스

### 9.1 그래피티 한 획 (각 + 탭 + ㅣ = 가거)

```
(각 조합 중, units=[ㄱ, Vowel ㅏ, ㄱ], 조립기 Empty — ㄱ 받침이 와서 닫힘)
탭: InkCollector.onUp → Recognizer.recognize → Tap → GraffitiState.interpret → Element(DOT)
    GraffitiInputHandler.handle → asm.push(DOT) → PendingShown(1)
    out.show(("", "각"), "ㆍ") → setComposingText("각ㆍ")
ㅣ: recognize → LineDown → Element(I) → asm.push(I) → [ㆍ,ㅣ] = ㅓ → Started(ㅓ)
    composer.apply(Vowel ㅓ) → 도깨비불 → ComposeResult("가", "거")
    out.show(…, "") → commitText("가"), setComposingText("거")
```

### 9.2 그래피티 ⌫ (와 ⌫ ⌫ = 외 → 오)

```
(와: units=[ㅇ, Vowel ㅗ, ReplaceVowel ㅚ, ReplaceVowel ㅘ](0.16), 조립기 [ㆍ,ㅡ,ㅣ,ㆍ])
⌫ 획: recognize → LineLeft → interpret → Backspace → handler.backspace()
   cancelPending() = false
   asm.pop() → [ㆍ,ㅡ,ㅣ] → ToVowel(ㅚ) → composer.undoLast()(마지막 ReplaceVowel ㅘ 단위) → "외"   // 0.16
⌫: asm.pop() → [ㆍ,ㅡ] → ToVowel(ㅗ) → "오"
```

## 10. 획 안내표 (0.23, 이슈 #189)

근거: SRS 0.45 FR-GRF-09~09d(TBD-25 오너 정정 = 불투명 덮개, TBD-26 = 기호 넣음), GRAFFITI 0.16 6.6, HLD 0.41 5장 "획 안내표 팁". 열고 닫기·버튼·터치 막기는 `KeyboardRootView`가 맡는다(LLD-08 7.1). 패드 뷰(8절)는 그대로다. 화면 문구에는 일반 이름("획 안내표")만 쓴다(상표, GRAFFITI 1장).

### 10.1 `GuideCatalog` (recognizer `kbd.graffiti.guide`)

```kotlin
enum class GuideSection { HANGUL, ENGLISH, NUMBER, CONTROL, SYMBOL }
sealed interface GuideMark {
    data class Template(val set: SetName, val id: String, val stroke: Int = 0) : GuideMark   // 템플릿의 몇째 획
    data class Rule(val token: StrokeToken) : GuideMark                                     // 직선 6, Tap, CornerTap
}
data class GuideCell(val label: String, val marks: List<GuideMark>, val alt: Int = 0, val note: String? = null, val interim: Boolean = false)
data class GuideGroup(val section: GuideSection, val title: String, val cells: List<GuideCell>)
sealed interface GuideShape { data class Line(val points: List<Pair<Float, Float>>); data object Tap; data object CornerTap }
object GuideCatalog {
    val groups: List<GuideGroup>
    fun shape(mark: GuideMark, templates: TemplateSet): GuideShape?
    const val UNSETTLED = "미정"; const val TAB_LABEL = "탭 문자"
}
```

- **행 ↔ 템플릿 id 목록은 코드 안의 표 하나다**(HLD "런타임에 읽는 방법은 LLD-05"). 칸이 템플릿 **id와 그 안의 획 번호**를 가리키고, 점 열은 실행 중에 인식기와 같은 `TemplateSet`에서 꺼낸다(`shape`). 그래서 템플릿 점을 고치면 그림이 따라 바뀐다. 템플릿 JSON 형식(7절)은 바꾸지 않는다.
- **임시 표시는 칸에 적는다**(`interim`). 정본은 `TEMPLATES.md`의 interim 열이고, 시험 GC-07이 둘이 같은지 본다(템플릿 확정 때 `tools/build_templates.py`가 interim을 지우면 GC-07이 깨져 칸 표시도 고치게 된다).
- `marks`가 둘 이상이면 차례대로 옆에 그린다(시프트 + 자음, 한영 전환 ② ↘ + 탭, 캡스록 ↑ ↑). `marks`가 비면 글자만 그린다(모음 입력 순서, 미정 기호).
- `shape`: 템플릿 표시 → 그 템플릿의 `strokes[stroke]`(첫 점 = 시작점), 템플릿이나 그 획이 없으면 null(템플릿 읽기 실패 시 빈 집합, LLD-09 4절 → 칸은 글자만). 규칙 표시 → `LineDown` (0.5,0)→(0.5,1), `LineUp` 반대, `LineRight` (0,0.5)→(1,0.5), `LineLeft` 반대, `DiagDownLeft` (1,0)→(0,1), `DiagDownRight` (0,0)→(1,1), `Tap`, `CornerTap`. `Symbol`·`Unknown`은 null.

칸 목록(순서 고정, FR-GRF-09b. 구획 순서 = 한글 → 영문 → 숫자 → 제어 획 → 기호, 지금 언어를 위로 올리지 않음):

| 묶음(제목) | 칸 | 수 |
|---|---|---|
| 한글 · 문자 영역 | ㄱ ㄴ ㄷ ㄹ ㅁ①(`ko_m`) ㅁ②(`ko_m2`) ㅂ ㅅ ㅇ ㅈ ㅊ ㅋ ㅌ ㅍ ㅎ①(`ko_h`) ㅎ②(`ko_h6`), "ㄲ = ↑ + ㄱ"(시프트 + 자음), ㅣ ↓ · ㅡ → · ㆍ 탭(모음 요소) | 20 |
| 한글 · 모음 입력 순서 | GRAFFITI 4.2 모음 표 21행을 글자로(예 ㅏ "ㅣ ㆍ", ㅘ "ㅗ + ㅏ"), 그림 없음 | 21 |
| 영문 · 문자 영역 | A~Z, ①②가 있는 글자는 두 칸(B D F G M O V X Y). P·R은 ②만(① 템플릿 없음). I = ↓ 규칙. O ①② = `en_o`의 획 0·1 | 35 |
| 숫자 영역 | 0①②(`num_0` 획 0·1) 1(↓) 2 3 4 5①② 6 7 8① 9(8②는 템플릿 없음) | 12 |
| 제어 획 | ⌫ ←, ↵ ↙, 시프트 ↑, 한영 전환 ①(구석 탭) ②(↘ + 탭), 구두점 시프트 탭, 확장 시프트 ↘, 스페이스(영문 →, 한글 문자 영역 `ko_space_v`, 한글 숫자 영역 →), 캡스록(영문, ↑ ↑), 조합 확정(한글, `ko_finish_j`) | 12 |
| 기호 · 구두점 (탭 뒤) | GRAFFITI 6.4 구두점 표 33행 순서. 규칙 항목(`.` `,` `'` `-` `!` `\`)은 그 규칙 획, 쐐기형 `_` `\|` `` ` `` `;` `:`는 "미정" | 33 |
| 기호 · 확장 (↘ 뒤) | 확장 기호표 24행 순서. 규칙 항목(`± × ¡`)은 그 규칙 획, ∫·Ø는 "미정" | 24 |

- 칸이 아닌 템플릿 = 인식 여유용 변형 `ko_d2`·`ko_b2`·`en_q2`, 오너 획 `*_oN`(7절)과, 한 행의 둘째 획(`ko_ieung`·`num_7`·`x_degree`의 획 1). 시험 GC-06.
- 임시 칸: B① D① G① H O①② X① 0①② 8① ° €(`TEMPLATES.md` interim = `en_b en_d en_g en_h en_o en_x num_0 num_8 x_degree x_euro`).
- 같은 모양 다른 뜻(K와 ㅈ, T와 ㄱ 등)은 각 묶음의 칸으로 따로 둔다(각 집합의 템플릿을 가리킴).

### 10.2 `GraffitiGuideLayout`·`GraffitiGuidePanel` (app `kbd.app.graffiti`)

```kotlin
class GraffitiGuideLayout(val widthDp: Float, groups: List<GuideGroup>) {   // 순수, dp
    val columns: Int; val cellWidthDp: Float; val titles: List<Title>; val cells: List<Cell>; val contentHeightDp: Float
    fun maxScrollDp(panelHeightDp: Float): Float; fun closeRect(panelWidthDp: Float): RectF2
}
class GraffitiGuidePanel(context: Context, templates: TemplateSet, feedback: FeedbackPort, onClose: () -> Unit) : View(context) {
    val scrollDp: Float; fun rewind()
}
```

**배치(dp, 세로·가로 화면 같음):** 안쪽 여백 `PAD` 8. 묶음마다 새 줄에 제목(높이 `TITLE` 32, 오른쪽 끝은 ✕ 폭만큼 비움) 뒤 칸. 칸 폭 = (폭 − 16) / `columns`, `columns` = max(1, ⌊(폭 − 16) / 64⌋)(360dp → 5, 640dp → 9). 칸 높이 = 84(그림 있는 묶음) / 44(모음 입력 순서처럼 모든 칸이 글자뿐인 묶음). 묶음 사이 8. ✕ = 덮개 오른쪽 위 40 × 40 고정(스크롤과 무관). 첫 칸은 8 + 32 = 40부터라 ✕와 겹치지 않는다. 덮개(5줄 240dp)보다 내용이 길므로 세로로 넘긴다.

**그리기(`onDraw`):** 덮개 전체를 불투명 배경 `0xFF161616`로 칠한다(FR-GRF-09a). 제목, 칸(둥근 네모 `0xFF262626`). 그림 칸: 그림 상자 = 칸 위 4dp부터 한 변 min(칸 폭 / 표시 수, 48) − 10의 정사각형, 표시마다 옆으로. 템플릿·직선 = 점 열을 상자에 맞춘 선(2dp, 임시면 점선 4·3dp), 시작점 ● 반지름 2.5dp, 끝 화살표(끝점에서 6dp 이상 떨어진 앞 점과의 방향, 양쪽 0.5rad, 길이 6dp). 탭 = 가운데 ●. 구석 탭 = 상자 테두리 + 왼쪽 위 ●. 그 아래 뜻(`label` + " ①"/" ②"), 그 아래 작은 글씨로 `note`와 "임시"(둘 다면 " · "로 이음). 글자 칸은 뜻과 `note`만. ✕는 맨 나중에(내용 위). 비밀번호 칸도 같다(`hidesDisplay`를 보지 않음, FR-GRF-09d). 아무것도 저장하지 않는다.

**터치(`onTouchEvent`, 늘 `true` = 소비, 입력 없음):**
```
DOWN/POINTER_DOWN: 따르는 포인터가 없을 때만 그 포인터를 따른다(다른 포인터는 소비만)
                   ✕ 안이면 onClosePressed = true, feedback.key()(✕는 키: 피드백은 누를 때, FR-CMN-11a ②)
MOVE: 따르는 포인터, onClosePressed가 아니면 scrollDp -= Δy, 0..maxScrollDp로 자름(✕를 누른 손가락은 스크롤하지 않음)
UP/POINTER_UP(따르는 포인터): onClosePressed && 뗀 점이 ✕ 안 → onClose()(손을 뗄 때 ✕ 안이면 닫음, FR-GRF-09). 그 밖 아무 일 없음
CANCEL: 따르기 끝
```
- 덮개의 다른 곳 탭은 닫지 않는다. 칸을 눌러도 아무 일이 없다(FR-GRF-09a).
- `rewind()`: 따르기 끝, `scrollDp = 0`(닫을 때 루트가 부름: 다음에 열면 맨 위부터).
- 시험: GD-03~GD-05, GD-10, GD-11(LLD-11 6.6).
- **입력 연습용 값(0.33, LLD-07 2.3):** `highlight`(칸 이름 집합)는 ✗ 때 그 칸을 강조하고 첫 칸을 위로 넘긴다. `reveal`(칸 이름 집합)은 `highlight`가 비었을 때 같은 자리로 넘기기만 하고 그리지는 않는다. 넘기는 칸은 레이아웃 순서로 첫 칸이고(쌍자음 항목은 자음 칸), 크기가 정해지기 전에 넣어도 `onSizeChanged`에서 적용한다. 그 뒤의 끌기는 막지 않는다.

## 11. 인식기 공급 `GraffitiRecognizers` (app, 0.37, FSS#96)

`kbd.app.graffiti.GraffitiRecognizers` — 모델을 읽고, 개인 획을 넣은 `Recognizer`를 만들어 주는 곳이다. 키보드(`KeyboardService`)와 입력 연습(`PracticeActivity`)이 하나씩 갖는다. 안드로이드 클래스를 쓰지 않는다(JVM 시험).

```kotlin
class GraffitiRecognizers(templates: TemplateSet, log: ErrorLog,
                          revision: () -> Int = { 0 },                          // 개인 획의 판. 바뀌면 다시 만든다
                          personal: () -> List<PersonalStroke> = { emptyList() }, // 그 판의 개인 획
                          model: () -> GraffitiModel? = { sharedModel(log) },
                          executor: Executor = GraffitiRecognizers.background) {
    fun warm()               // 지금 판의 인식기가 없으면 뒤 스레드에서 만들기 시작한다. 기다리지 않는다
    fun get(): Recognizer    // 지금 판의 인식기. 아직 만드는 중이면 끝날 때까지 기다린다
    companion object {
        fun sharedModel(log: ErrorLog): GraffitiModel?            // 프로세스에 한 번 읽음
        internal fun readModel(log: ErrorLog, resource: String = GraffitiModelLoader.RESOURCE): GraffitiModel?
    }
}
```

- **읽는 때(HLD 5.1 "읽는 때", R15):** 모델(862 kB)을 읽고 `Recognizer`를 꾸미는 일은 **뒤 스레드 하나**(`background`, 데몬 스레드 하나짜리 실행기)에서 한다. `KeyboardService.onCreate`가 `warm()`을 부르므로 키보드의 첫 표시(`onCreateInputView`·`onStartInputView`)는 이 일을 기다리지 않는다. 첫 획이 그보다 먼저 오면 `get()`이 남은 시간만 기다린다. 태블릿에서 잰 값(LLD-11 6.7): 모델 첫 읽기 44 ms, 인식기 첫 구성 68 ms, 개인 획 328개(한도 가까이) 반영 427 ms.
- **모델은 프로세스에 하나:** `sharedModel()`이 리소스를 한 번 읽어 간직한다(키보드와 입력 연습이 같은 프로세스다). 읽지 못하면(리소스 없음, 형식 오류) `ErrorLog`에 `GraffitiModelLoader.load`로 한 번 적고 `null`을 간직한다 → 3.6 "모델 없는 구성".
- **판과 개인 획:** `revision()`이 간직한 인식기의 판과 다르면 `get()`·`warm()`이 새로 만든다 — 앞의 인식기에 `withPersonal(personal())`을 불러 바뀐 획만 계산한다. 같은 판 안에서는 같은 인식기 객체를 돌려준다. 한 판에 만드는 일은 하나뿐이다(`warm()`을 여러 번 불러도). 만든 결과는 판이 더 새것일 때만 바꿔 끼운다. **쓰는 쪽은 개인 획 저장소를 잇는다(0.38, SRS FR-TUT-17 ③):** 키보드와 입력 연습 모두 `revision = { store.revision }`(쓰기마다 1 증가, LLD-07 8.2), `personal = { store.book().strokes }`로 만든다(`store`는 프로세스에 하나인 `AppStores.personalStrokes`). `KeyboardService.onStartInputView`가 `warm()`을 부르므로 **키보드가 뜰 때** 뒤에서 다시 만들기 시작하고, 획을 인식할 때의 `get()`이 늘 지금 판을 돌려준다(늦어도 다음에 키보드가 뜰 때 반영). 학습 화면은 저장·삭제 바로 뒤 `warm()`을 부른다(LLD-07 8.3). 키보드는 저장소를 읽기만 한다(FR-TUT-17 ①).
- **쓰는 쪽:** `GraffitiInputHandler`의 첫 인자는 `recognizer: () -> Recognizer`다(획마다 불러 그 획을 끝까지 그 인식기로 본다). `Recognizer`를 바로 받는 보조 생성자도 둔다(시험). 입력 연습의 `BoardFactory`·`PracticeInput.graffiti`도 같은 `() -> Recognizer`를 받는다(기본값 = 템플릿만, 시험용).
- **실패:** `personal()`이 예외를 내면 `ErrorLog`(`GraffitiRecognizers.build`)에 적고 개인 획 없이 만든다. 만들다 예외가 나면(`IllegalStateException` = 글자 목록 어긋남, 모델 읽기 예외 등) `ErrorLog`에 적고 `Recognizer(templates)`(모델 없는 구성)로 내려간다. 그것도 안 되면 빈 템플릿의 인식기(규칙 획만, LLD-09 4절과 같은 결과)다. `build`는 예외를 내지 않으므로 입력기는 멈추지 않는다(NFR-03). 획 좌표는 기록하지 않는다(NFR-05).
- 한 획의 특징 계산·순전파에서 난 예외는 지금처럼 `GraffitiPadView`의 `EngineGuard.stroke`가 잡아 그 획을 버린다(8.3, LLD-09 2절).
- 시험: RP-01~RP-06(LLD-11 7.9).
