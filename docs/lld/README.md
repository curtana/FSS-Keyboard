# 상세 설계 (LLD)

- 문서: LLD, 버전 **0.50** (2026-11-14)
- 상태: 초안. 함수 단위 상세 설계. 현재 설계 판(REQUIREMENTS 0.17·SRS 0.58·HLD 0.53·SWIPE 0.43·GRAFFITI 0.16)에 맞춤. 0.50은 FSS#93(모드 키 순환 순서 한글 두벌식 → 영문 쿼티 → 긋기 입력 → 그래피티, 처음 설치 시작 모드 = 한글 두벌식, 설정 "입력 모드" 목록도 같은 순서, SRS 0.85·HLD 0.78)을 넣은 판이다(LLD-03 0.31 3.5·LLD-08 0.33 5절·LLD-07 0.37 7절·LLD-11 0.57 7.2~7.2b). 0.49는 FSS#86(띄운 자판: 방향별 띄우기, 손잡이 띠를 길게 눌러 옮기기, 이동 영역 안의 자판 판, 비율 fx·fy 저장, 앱을 밀지 않는 인셋, SRS 0.84 FR-CMN-15·HLD 0.77)을 넣은 판이다(LLD-08 0.32 7.4·LLD-07 0.36·LLD-11 0.56). 0.48은 FSS#85(자판 옆 남는 자리를 투명하게: 자판 판 `KeyboardPlate.rect`, 루트는 자판 판만 칠함, 터치 영역 = 자판 판, SRS 0.83·HLD 0.76)를 넣은 판이다(LLD-08 0.31·LLD-07 0.35·LLD-11 0.55). 0.47은 FSS#84(자판 좌우 여유 칸: 자판 사각형 11칸, 테두리의 왼쪽·오른쪽 = 자판 사각형 바깥 변, SRS 0.82·HLD 0.75·SWIPE 0.58)를 넣은 판이다(LLD-08 0.30·LLD-03 0.30·LLD-04 0.35·LLD-05 0.35·LLD-05A 0.17·LLD-07 0.34·LLD-11 0.54). 0.46은 FSS#80(영문 오너 획 템플릿, SRS 0.81·HLD 0.74)을 넣은 판이다(LLD-05 0.34·LLD-11 0.53). 0.45는 FSS#75(긋기 기호 재배치, SRS 0.80·SWIPE 0.57)를 넣은 판이다(LLD-04 0.34·LLD-07 0.33·LLD-11 0.52). 0.44는 FSS#72(그래피티 연습을 자음·쌍자음과 알파벳 전체로, 주제 다섯)를 넣은 판이다(LLD-07 0.32·LLD-05 0.33·LLD-11 0.51). 0.43은 FSS#70(Q 인식 변형 `en_q2`, 안내표 칸 그대로)을 넣은 판이다(LLD-05 0.32·LLD-11 0.50). 0.42는 FSS#68(한 획 쌍자음 삭제 뒤 남은 표현 정리, 안내표 157칸, SRS 0.78·HLD 0.71, 동작 그대로)을 넣은 판이다(LLD-05 0.31·LLD-11 0.49). 0.41은 FSS#62(탭 자판 m 오른쪽 키의 탭과 ← 맞바꿈, 탭 `=` ← `-`, SRS 0.77·HLD 0.70)를 넣은 판이다(LLD-05A 0.16·LLD-11 0.47). 0.40은 FSS#57(긋기 획 자국 설정 — 끄면 획 선만 안 그림, 기본 켬, SRS 0.76 FR-DSP-09·FR-SET-02·HLD 0.69·SWIPE 0.55)을 넣은 판이다(LLD-04 0.33·LLD-07 0.31·LLD-11 0.46). 0.39는 FSS#48(그래피티 패드 인식 영역 경계 사각형, SRS 0.75 FR-GRF-01a·HLD 0.68)을 넣은 판이다(LLD-05 0.28 8.1·LLD-11 0.43). 0.38은 FSS#44(설정 화면의 키 크기·정렬을 "세로 화면"·"가로 화면" 상자로 나누고 지금 화면 방향 상자에 "지금 화면" 표시, SRS 0.74·HLD 0.67)를 넣은 판이다(LLD-07 0.29·LLD-11 0.42). 0.37은 FSS#43(입력 연습의 긋기·그래피티 항목을 확정된 글자로 판정, 끝 동작 = 스페이스, SRS 0.73·HLD 0.66·SWIPE 0.53)을 넣은 판이다(LLD-07 0.28·LLD-11 0.41, D-65·D-66 바뀜). 0.36은 FSS#25(자판 폭과 왼손·중앙·오른손모드, 폭은 키 한 칸 크기로, SRS 0.71 FR-CMN-14·FR-SET-09·HLD 0.64 5A)를 넣은 판이다(LLD-07 0.27·LLD-08 0.29 7.2·LLD-11 0.40). 0.21은 이슈 #179(길이 판정 폐지, 자음 기준 C·필순·후보로 모음 판정, TBD-22·24)·이슈 #180 후속(가운데 자음, 조합 중 설정 키 = 한 글자 완성)·FR-CMN-08 ①/② 분리를 넣은 판이다(6.21). 0.22는 이슈 #198(탭 자판: 기호 키 삭제·`,` ←→·`/` 키, 편집 줄 Esc·Del·⌫ 45dp, `-` 넷째 줄, ↵ 넷째 줄 끝)을 넣은 판이다(6.22, LLD-05A 0.10). 같은 6.22에 이슈 #198 추가 지시(i ← `[` → `]`, o ← `{` → `}`)를 넣었다(LLD-05A 0.11, LLD-11 0.23). 0.34는 FSS#17(튜터 → 입력 연습, SRS 0.68·HLD 0.61·SWIPE 0.48)을 넣은 판이다(LLD-03·06·07·08·11, D-65~D-68). 0.35는 FSS#36(가운데 자리에 지금 조합 중인 한 글자, 후보 표시 없음, 아직 모음이 아닌 점은 자리에)·FSS#37(C를 반 키 높이 아래로)을 넣은 판이다(SRS 0.72·HLD 0.65·SWIPE 0.52·REQUIREMENTS 0.24, 6.30, LLD-03 0.29·04 0.32·06 0.24·07 0.26·08 0.28·11 0.39). 0.33은 설계질의 표의 저장소 이슈 링크를 "이슈 #N" 글로 바꾼 판이다(이슈 #249, 공개 준비). 0.32은 이슈 #251 오너 결정: 공개판·개인판을 하나로(빌드 하나, 긋기 엔진은 모든 빌드)를 넣은 판이다(LLD-03·04·06·07·08·11). 이 판부터 문서의 "personal / public" 구분은 기록이고, 코드·빌드는 하나다. 0.31은 이슈 #242 오너 결정(두벌식 겹받침 한 번에)을 넣은 판이다(LLD-03 0.26·LLD-04 0.29·LLD-05A 0.14·LLD-11 0.33). 0.30은 이슈 #237(맨 윗줄 숫자 키 ↑: 창 위 끝에 붙은 점 = 위 변 통과)과 이슈 #208 오너 지시(4 ← ; ↔ 기호2 ← ", 배정 기호를 네 변 가운데에)를 넣은 판이다(LLD-03 0.25·LLD-04 0.27·LLD-05A 0.13·LLD-11 0.31). 0.29는 이슈 #217 오너 결정(C 기준 두 구역 — 가운데 점이 C 오른쪽 아래 모서리의 45° 선 아래 왼쪽 / 오른쪽 위, 첫 획 두 후보, 버리는 획 없음, SWIPE 0.43)을 넣은 판이다(6.29, LLD-04 0.26·LLD-11 0.30). 0.28은 이슈 #217 오너 결정(모음 획이 C에 겹쳐도 버리지 않음, C 역할을 획의 가운데로, SWIPE 0.42)을 넣은 판이다(6.28, LLD-04 0.25·LLD-11 0.29). 0.27은 이슈 #217 오너 결정(`minCrossDp` 8dp 고정, 튜터가 재지 않음, 저장 값 무시)을 넣은 판이다(6.27, LLD-03 0.24·LLD-07 0.22·LLD-11 0.28). 0.26은 이슈 #213(가운데 자음 자리 C를 가로 가운데로, SWIPE 0.40)을 넣은 판이다(6.26). 0.25는 이슈 #189(그래피티 획 안내표: 칸 목록 `GuideCatalog`·불투명 덮개·빈 띠 버튼, SRS 0.45 FR-GRF-09~09d·HLD 0.41·GRAFFITI 0.16 6.6)를 넣은 판이다(6.25). 0.24는 이슈 #208(긋기 기호 키 단계 삭제·기호 키 셋, SRS 0.53·SWIPE 0.38)을 넣은 판이다(6.24). 0.23은 이슈 #205(모드 키 ↑ = 설정, 네 모드 공통, 긋기 ⚙ 키·한 글자 완성 삭제, SRS 0.52·HLD 0.47·SWIPE 0.37)를 넣은 판이다(6.23). 0.20은 이슈 #179(ㅓ/ㅢ 높이 3등분, 길이는 혼자 있는 첫 획에만)와 이슈 #180(가운데 음절)을 넣은 판이다(6.20). 0.19는 이슈 #173(자음은 탭, 모음은 손글씨 필순 획 + 획 사이 위치 관계, 종성은 다시 탭; 양 끝 열 이동·설정 키; 획 자국; 튜터 1-2 세 분포·calibration, 3-1 결과 판정, 3-2 삭제; 기준값 형식 5)을 넣은 판이다(6.19). 0.18은 이슈 #168(긋기 = 그 키의 자음 + 한 획 모음, 탭 = 자음, 쌍자음 전용 키, 기준 키·쌍자음 시간 창 삭제, 자판 7×6, 기준값 형식 4, 튜터 2과 삭제·연습 자음 19개 돌림·화면 과 번호)을 넣은 판이다(6.18). 0.17은 이슈 #165(튜터 5과 연습 자판 ⌫, 틀린 입력 뒤 입력 확인)를 넣은 판이다(6.17). 0.16은 이슈 #160(긋기 자판 천지인 키, `CheonjiinAssembler` core-hangul로 이동, `ReplaceVowel` 자기 단위, 튜터 3-4)을 넣은 판이다(6.16). 0.15는 이슈 #154(튜터 항목 판정: ✓·✗, 쓰는 법 안내, 같은 항목 다시, 측정 단계는 동작 종류로 판정)와 이슈 #156(모든 모음은 한 획, 긋기 병합 표·나눠 쓰기·튜터 3-3 삭제)을 넣은 판이다(6.15). 0.14는 이슈 #150(자음(탭)·모음(긋기) 분리, 기준 키, 짧은 두 번 탭만 쌍자음, 긋기 ⌫ 자모 단위, 튜터 4과 삭제·2-1 양쪽 측정, 기준값 형식 3)을 넣은 판이다(6.14). 0.3은 설계 0.16 판(DCR-01~21 결정, 숫자류 입력창 = 영문 쿼티, 튜터 첫 실행 한 번, 설계질의 #6 답·오너 결정)에 맞춘 판이다. 0.4는 SWIPE 0.12(설계질의 #55)와 구현 중 찾은 LLD 빈틈 G1~G13(이슈 #13, 6.3)을, 0.5는 T27·T30에서 찾은 빈틈(이슈 #75·#83, 6.4)을, 0.6은 T25 후속(이슈 #73)과 PR #91 동작(이슈 #92)을 본문에 넣은 판이다(6.5). 0.7은 설계 0.20 판(SRS 0.17~0.20, 오너 결정 #74 A·#93 OD-1~3 A·#79 OD-1~7 A)의 그래피티 원본 획 표·기호표의 직선·탭 획과 PR #104 동작(이슈 #105)을 넣은 판이다(6.6). 0.8은 설계 0.21 판(SRS 0.21, 오너 결정 #108 A)의 그래피티 영역 비율 선택지와 낡은 문구를 맞춘 판이다(6.7). 0.9는 설계 0.22 판(SRS 0.22 FR-TUT-09, 이슈 #124)의 튜터 "입력: …" 줄을 넣은 판이다(6.8). 0.10은 이슈 #127(SRS 0.27·HLD 0.23·SWIPE 0.19)의 머물기 삭제·끊는 간격·받침 우선·들어오는 기준 간격(R5 B안)·누르고 있기·다시 나누기·튜터 과·타수를 넣은 판이다(6.9). 0.11은 이슈 #135(SRS 0.29 FR-TUT-11·FR-DSP-08·FR-SET-02, HLD 0.25 6장·7.1, SWIPE 0.20 9.3·10장)의 현재 글자 설정과 튜터 연습 자판의 안내를 넣은 판이다(LLD-06 2·4절, LLD-07 2.3·6.1·7절, LLD-11 9.1). 같은 0.11에 이슈 #136(SRS 0.28 FR-GRF-05d, HLD 0.24 5장, GRAFFITI 0.15 4.2)의 그래피티 패드 상태 표시를 넣었다(LLD-05 6절·8.1, LLD-11 6.5, 6.10). 0.13은 이슈 #143(PM 결정, FR-TUT-09)의 튜터 입력 표시(1~4과 큰 한 글자, 5과 줄, 화면 세로 스크롤)와 이슈 #144(1-1 자음 14개 한 번씩)·이슈 #142 튜터 부분(누르고 있기 항목 없앰)을 넣은 판이다(LLD-07 2.2·2.3, LLD-11, 6.12, 6.13).
- 근거(읽기 전용): [REQUIREMENTS.md](../REQUIREMENTS.md) 0.11, [SRS.md](../SRS.md) 0.43, [HLD.md](../HLD.md) 0.39, [SWIPE_INPUT_DESIGN.md](../SWIPE_INPUT_DESIGN.md) 0.34(설계 PR #181, #182), 그래피티 규칙 문서(비공개, 이슈 #249) 0.15, 분석 `_workspace/analysis/split_consonant_vowel.md`(이슈 #150), 오너 결정(이슈 #3 코멘트, PR #4, 이슈 #6, #74, #93, #79, #108, #124, #127, #135(설계 PR #138), #136 PM 결정(설계 PR #137)), 설계 답(설계질의 #55, #57, #79 Q3), 작업지시서(이슈 #60 v1), LLD 빈틈 이슈 #75·#83(T27 PR #78, T30 PR #84의 임시 처리), #73(T25 PR #77), #92(PR #91), #105(PR #104), 분석 `_workspace/analysis/graffiti1_strokes.md`(Graffiti 1 획 조사)
- 구현 작업지시서는 [`docs/tasks/`](../tasks/README.md)에 있다(LLD 0.3 기준, 이슈 #12). 0.4는 지시서가 정한 임시 처리(G2~G11)를 그대로 본문에 확정했으므로 지시서 개정은 없다. G12·G13은 이미 머지된 T18 코드의 버그라 이 판과 같은 PR에서 코드를 고쳤다. 0.5는 머지된 T27·T30 코드의 임시 처리를 확정한 판이고, 코드가 아직 따라오지 않은 곳은 6.4 끝 "코드 후속"에 적었다. `docs/tasks/` 파일은 이 판에서 고치지 않았으므로 코드 후속은 해당 작업(T31 #85 등)의 작업지시서에 따로 넣어야 한다. 0.6도 `docs/tasks/`는 고치지 않았고, 코드 후속은 이슈 #96에 있다(6.5). 0.7의 코드 후속은 새 작업지시서 [T38](../tasks/T38-graffiti1-templates.md)(이슈 #103)이다(6.6). 0.8은 코드 후속이 없다(코드가 이미 0.8 본문과 같음, 6.7). 0.9는 구현과 같은 PR(이슈 #124)이라 지시서를 따로 두지 않았다(6.8). 0.10도 구현과 같은 PR(이슈 #127)이라 지시서를 따로 두지 않았다(6.9). 0.11(이슈 #135·#136)도 구현과 같은 PR이다(PR #139, #140). 0.13(이슈 #143, #144)도 구현과 같은 PR이다. 0.14(이슈 #150)도 구현과 같은 PR이다(지시서 없음, 6.14). 0.15(이슈 #154, #156)도 구현과 같은 PR이다(지시서 없음, 6.15). 0.16(이슈 #160)도 구현과 같은 PR이다(지시서 없음, T22·T12의 파일 위치만 고침, 6.16). 0.17(이슈 #165)도 구현과 같은 PR이다(6.17). 0.18(이슈 #168)도 구현과 같은 PR이다(지시서 없음, 6.18). 0.19(이슈 #173)도 구현과 같은 PR이다(지시서 없음, 6.19). 0.20(이슈 #179, #180)도 구현과 같은 PR이다(지시서 없음, 6.20). 0.21(이슈 #179, #180 후속)도 구현과 같은 PR이다(PR #183, 지시서 없음, 6.21).

## 1. 목차

### 1.1 파일과 HLD 절

| 파일 | HLD 절 | 내용 |
|---|---|---|
| [README.md](README.md) | 전체, 13장 | 목차, 표기 규칙, 추적표, LLD에서 정한 세부, 설계 변경 요청(결정 기록)·설계 질의 |
| [LLD-03-modules.md](LLD-03-modules.md) | 1~3장, 3.1 | Gradle 모듈·패키지·의존 방향, 빌드 변형·`SwipeModule`, 공통 타입(자모·이벤트·기하·기준값·모드), 필요 라이브러리 |
| [LLD-04-swipe.md](LLD-04-swipe.md) | 4장(4.1~4.4) | `KeyHitTester`, `EdgeCrossingTracker`, `StrokeClassifier`, `ProgressState`, `SwipeInputMapper`, `HangulComposer`, 긋기 자판·둘레 숫자·기호 배치표, `SwipeKeyboardView`·`TouchTracker`, 시퀀스(긋기 한 획, 블록 정정) |
| [LLD-05-graffiti.md](LLD-05-graffiti.md) | 5장 | `Recognizer`, `CheonjiinAssembler`, `GraffitiState`, `GraffitiInputHandler`, 템플릿 JSON, `GraffitiPadView`·`InkCollector`, 시퀀스(한 획, ⌫), 획 안내표(`GuideCatalog`·`GraffitiGuidePanel`, 0.25) |
| [LLD-05A-tap-keyboard.md](LLD-05A-tap-keyboard.md) | 5A장 | 탭 자판 동작 표 판정, 윗첨자·아래첨자 표, 배치 데이터(쿼티·두벌식)·기호 배정·모서리 표시, `TapKeyboardView`, `MultiPointerQueue`, `RepeatTimer`, 접근성, 시퀀스(키 입력) |
| [LLD-06-guide.md](LLD-06-guide.md) | 6장 | `GuideTimerLogic`, `GuideOverlay` |
| [LLD-07-tutor-store.md](LLD-07-tutor-store.md) | 7장 | 입력 연습(주제·항목 `PracticeTopics`, 실제 자판 뷰를 잇는 `PracticeInput`, 판정 `PracticeItemJudge`, 화면 `PracticeActivity`·`PracticeFlow`, 첫 실행 `PracticeLaunchPolicy`), 고정 기준값, `SettingsStore`, `ErrorLog`, `SettingsActivity` (0.34: 튜터·기준값 저장 삭제) |
| [LLD-08-service-mode.md](LLD-08-service-mode.md) | 8장, 8.1, 5A(창 높이·처리 시점) | `FieldInfo`(숫자류 입력창 판별), `KeyboardService`, `ComposingController`·`EditorPort`, `ModeController`(숫자류 입력창 진입·이탈), `InputDispatcher`, `KeyboardRootView`(획 안내표 버튼·덮개, 0.25; 자판 폭과 정렬, 0.36), `Feedback`, 시퀀스(모드 키, 입력창 시작, 숫자류 입력창 이탈, 커서 이동) |
| [LLD-09-errors.md](LLD-09-errors.md) | 9장 | `EngineGuard`, 상태 되돌리기, 기준값 파일 손상 |
| [LLD-11-tests.md](LLD-11-tests.md) | 11장 | 시험 데이터 형식, 함수별 시험 ID와 기대값, SRS 사례 표 ↔ 시험 ID, 실기기 시험 |

- HLD 10장(위험) R1~R5의 대응: R1 → LLD-11 DV-09, R2 → LLD-08 7절·DV-05, R3 → LLD-06 3절·DV-04, R4 → LLD-08 4.1·DV-06, R5 → LLD-04 6.2·LLD-07 2.2·DV-23. HLD 12장(설계 결정)은 각 파일이 따른다. HLD 13장은 이 문서 3·4절이 대신한다.

### 1.2 읽는 순서

LLD-03(공통 타입) → LLD-04(엔진 중심) → LLD-05A·05 → LLD-08(앱이 엔진을 묶는 방법) → LLD-06·07·09 → LLD-11.

### 1.3 "미정"·설계 변경 요청·설계 질의를 다루는 규칙

- 사용자에게 보이는 동작을 SRS·HLD·규칙 문서가 정하지 않았으면 LLD가 정하지 않는다. 설계팀에 `설계질의` 이슈로 묻는다(PROCESS_ROLES 0-1). 0.1에서는 6절 "설계 변경 요청(DCR)"으로 올렸고, **DCR-01~21은 모두 결정되어 설계 문서(SRS 0.15 이후)에 반영됐다.** LLD 본문은 결정된 규칙을 따르고 `(DCR-nn 결정)`으로 출처를 남긴다. 결정을 기다리는 DCR은 없다.
- 막히지 않는 질의는 이슈로 남기고, 답이 올 때까지 본문에 "설계질의 #n 임시 처리"를 적어 둔다. 임시 처리는 되도록 "입력 없음·변화 없음"으로 두고, 그 시험의 근거 칸에 `#n`을 적는다. 답이 설계 문서에 반영되면 본문의 근거를 그 SRS·HLD·규칙 문서 절로 바꾸고 6.2절에 답한 질의로 남긴다. 0.3 현재 열린 질의는 없다.
- 판정 매개변수·내부 자료구조·측정 방법처럼 동작 규칙을 바꾸지 않는 값은 LLD가 초기값을 정한다(5절 "LLD에서 정한 세부"). 판정 경계값의 기본값은 SRS가 튜터 실측으로 정하도록 했으므로(FR-TUT-05) LLD는 HLD 7.2 예시값을 임시 기본값으로만 쓴다.

## 2. 공통 표기 규칙

- 언어: Kotlin. 코드 블록은 시그니처와 의사 코드까지 쓴다(전체 구현은 쓰지 않는다).
- 단위: 엔진 좌표·거리 dp, 시각 ms(`MotionEvent.getEventTime()` 기준). 앱이 px를 dp로 바꿔 넘긴다.
- 방향 표기: ↑ UP, ↓ DOWN, ← LEFT, → RIGHT. 화면 y는 아래로 증가한다.
- 경계: "이상"·"이하"·"안"은 `≥`·`≤`, "초과"는 `>`(SRS 1.3, HLD 4.3).
- 참조: 다른 파일의 정의는 `파일명#절`로 가리키고 같은 정의를 다시 쓰지 않는다.
- ID: 요구사항은 SRS ID(`FR-…`, `NFR-…`), 시험은 LLD-11의 ID(`HC-01` 등), 정한 세부는 `D-nn`, 설계 변경 요청(결정 기록)은 `DCR-nn`, 설계 질의는 이슈 번호(`#n`).

### 2.1 함수 명세 형식

함수마다 아래 항목을 표나 블록으로 적는다. 해당 없는 항목은 뺀다.

```
#### `클래스.함수`
- 시그니처: (코드 블록)
- 입력·출력
- 사전 조건 / 사후 조건
- 처리: 번호 붙인 의사 코드(판정 순서·경계값 포함)
- 오류: 예외·잘못된 입력 처리
- 근거: SRS·HLD·SWIPE·GRAFFITI ID/절
- 시험: LLD-11 시험 ID
```

## 3. 추적표 — SRS → LLD

| SRS | LLD 파일#절 · 함수 | 시험 |
|---|---|---|
| FR-CMN-01 | LLD-08 3절 `KeyboardService`, LLD-07 7절 | DV-16 |
| FR-CMN-02 | LLD-03 3.5 `InputMode`, LLD-08 3.1 `showModeView` | MOD-01, DV-07 |
| FR-CMN-03, 03c | LLD-03 3.5 `InputMode` 선언 순서(= 순환 순서), LLD-08 5절 `ModeController.onModeKey`·`nextEnabled` | MC-01~MC-10, MC-16 |
| FR-CMN-03a | LLD-08 6.2 "입력" 분류, `InputDispatcher.markInput`, LLD-05 6절 typed 열 | ID-03, ID-04, ID-09~ID-11, ID-21, ID-22, GH-34 |
| ~~FR-CMN-03b~~ | SRS 0.65에서 삭제(이슈 #251) | ~~MC-11, MC-13~~ |
| FR-CMN-03d | LLD-08 6.1 `onModeKey`·`finishAll`·`resetParsers` | ID-08, GH-12 |
| FR-CMN-03e | LLD-08 5절 `onFieldChanged`, 3.1 | MC-10, MC-12, ID-12 |
| FR-CMN-03f | LLD-08 5절 `applyEnabled`·`nextEnabled`·`fixedStart`, 3.1, 3.2 `applyEnabledModes`, LLD-07 2.3(주제 목록 표시) | OM-01~OM-12, PR-14, DV-20 |
| FR-CMN-04 | LLD-08 2절 `FieldInfo.numericLike`, 3.1, 5절 `enterNumericField`, LLD-05A 3.5 | FI-01~FI-07, FI-11, FI-12, NF-01, NF-04, DV-20 |
| FR-CMN-04a | (SRS에서 삭제: 숫자 키패드 없음) | – |
| FR-CMN-04b | LLD-08 5절 `returnMode`·`enterNumericField`·`leaveNumericField`, 6.1 `onModeKey`(저장 제외), 3.1(같은 칸 재표시) | NF-01~NF-09, ID-11, ID-23, FI-06 |
| FR-CMN-05 | LLD-08 4절 `ComposingController.show` | CC-01~CC-05 |
| FR-CMN-06 | LLD-08 4절 `finishAfterUserMove`, 4.1 `onUpdateSelection`, 6.1 `onCursorMoved` | CC-09~CC-19, CC-22, ID-13, DV-06 |
| FR-CMN-07 | LLD-04 7.3 `HangulComposer.backspace`, LLD-05 6절 `backspace`, LLD-08 6.1 `backspace`, 4절 `deleteBefore`(선택 영역) | BS-01~BS-25 |
| FR-CMN-07a | LLD-04 6.2 `SwipeInputMapper.onBackspace`, 7.3, LLD-08 6.1 `backspace` | BS-02, SM-23, SM-40~SM-44, ID-06 |
| FR-CMN-07b | LLD-05 4.2 `pop`, 6절 `backspace`, LLD-04 6.2 `popElement`·7.1 `ReplaceVowel` 단위, LLD-08 6.1 `backspace`(긋기, 0.16) | BS-06~BS-19, BS-23, BS-24, CJ-31~CJ-41, HC-24, CJ-56, CJ-57, ID-31 |
| FR-CMN-07c | LLD-05A 6절 `RepeatTimer` | RT-01~RT-03, GH-23 |
| FR-CMN-08 (0.21 ①/②) | LLD-08 2절 `FieldInfo.hidesDisplay`(①, 표시)·`noRecord`(①②, 기록), LLD-06 2절 `bigCurrentOn`, LLD-05 6절 `onStroke`(인식 글자 표시), LLD-04 9.5 가운데 자음, LLD-07 6.2 `ErrorLog.enabled` | FI-01~FI-06, FI-12, GT-07, GT-10, GH-35, CS-02, SW-16, EL-02, ID-20, DV-14 |
| FR-CMN-09 | LLD-08 4절 `enter` | CC-07, ID-16, DV-17 |
| FR-CMN-10 | LLD-08 5절 생성, 6.1 `onModeKey`(숫자류 입력창 안 저장 제외), LLD-07 6.1 `last_mode` | MC-12, MC-15, NF-07, NF-08, SS-02 |
| FR-CMN-11 | LLD-05A 4절·5절 `MultiPointerQueue`, LLD-04 9.3, LLD-05 8.2 | MQ-01~MQ-06, DV-13 |
| FR-CMN-11a | ① LLD-05A 4절, LLD-04 9.3, LLD-05 8.3(기능키 누를 때) ② LLD-08 8절 `Feedback.key` 부르는 곳 ③ LLD-05A 4절 CANCEL·5절 `onCancelAll`, LLD-04 9.3, LLD-05 8.3 | MQ-07, MQ-08, FB-01~FB-03, SW-02~SW-04, RT-05, ID-24, DV-21 |
| FR-CMN-12 | LLD-08 3절 `onEvaluateFullscreenMode`·`onConfigurationChanged`, 7절(시스템 막대 여백), LLD-03 3.4 | TS-08, DV-07, DV-08 |
| FR-CMN-14 (0.36, FSS#25; 0.47, FSS#84 여유 칸; 0.48, FSS#85 ⑫ 자판 판) | LLD-08 7.2 `KeyboardWidth.place`(`KeyboardSpan` 사각형·여유 칸)·`KeyboardRootView` `onMeasure`·`onLayout`·`SideMarginView`·`OffKeyboardPointers`, LLD-08 7.3 `KeyboardPlate.rect`·`KeyboardRootView.plate`·`PlatePainter`·`KeyboardInsets.compute`(⑫), LLD-04 2절 `KeyGeom.build`의 `sideDp`, LLD-07 2.3 `BoardFrame` | KW-01~KW-05, KW-07, KW-09, KW-10, SM-01~SM-05, KH-07, EC-16, KR-01~KR-06, KR-08~KR-11, SW-20, MK-09, PR-31, PR-32, DV-30, DV-35, KP-01~KP-07, PL-01~PL-08, KI-01~KI-04, PR-33, DV-36(⑫) |
| FR-CMN-15 (0.49, FSS#86 띄운 자판) | LLD-08 7.4 `FloatingKeyboard.place`·`drag`·`ratios`, `PlateMover`, `KeyboardRootView`의 `portraitFloat`·`landscapeFloat`·`MoveHandle`·`movePlate`·`onMeasure`·`onLayout`·이동 영역의 위, `KeyboardInsets.compute`(⑫), LLD-08 3절 `saveFloatPosition`·설정 흐름, 8절 `Feedback.grab`, LLD-07 6.1 `FloatSetting`·`setFloatPosition`, 2.3 `BoardFrame`(⑭) | FK-01~FK-08, PM-01~PM-12, FL-01~FL-20, KI-05, KI-06, FB-05, SS-12~SS-14, PR-34, DV-37 |
| FR-HGL-01~03 | LLD-04 7.2 `step`, 7.3 `apply` | HC-06~HC-20 |
| FR-HGL-04, 04a, 04b | LLD-03 3.1 `MergeTables`(0.15: 긋기 = `NONE`), LLD-04 7.1 `ComposePolicy` | HC-01, HC-02, HC-05, HC-12~HC-14, HC-21, HC-22 |
| FR-HGL-04c | LLD-05 4절 `CheonjiinAssembler`(이어질 수 없는 요소 무시 포함), 6절 `onStroke`(무시 요소 flash 없음), LLD-04 7.2 `ReplaceVowel` | CJ-01~CJ-24, CJ-26, CJ-41, HC-24, GH-27~GH-29, GH-36 |
| FR-HGL-04d | LLD-04 7절, LLD-05 6절 | HC-12~HC-14, HC-21, HC-22, SM-14, SM-14b, GH-01~GH-07, GH-26~GH-29 |
| FR-HGL-04e | LLD-05 4.2 `push`·`pendingGlyph`, 6절 `handle`·`finishAll`(모든 확정에서 버림), LLD-08 4절 `finishComposing` | CJ-25, CJ-28, GH-02, GH-03, GH-06~GH-12, GH-20, GH-26, GH-30, GH-32, CC-15, CC-16, CC-21, CC-22 |
| FR-HGL-05 | LLD-03 3.1 `DOUBLE_OF`·`JONG_CAPABLE` | HC-18~HC-20, HC-28 |
| FR-HGL-06 | LLD-03 1절(core-hangul JVM 모듈) | HC 전체 |
| FR-TAP-01, 01a | LLD-05A 2.2 `TapKeyResolver` | TK-01~TK-24, ID-07, ID-14 |
| FR-TAP-01b | LLD-05A 2.3 `ScriptTables` | TK-16, ST-01~ST-03, ID-15 |
| FR-TAP-02, 02a | LLD-05A 2.2, 3.2(두벌식 `shiftJamo`) | TK-02, TK-05, TA-05~TA-08 |
| FR-TAP-02b | LLD-05A 2.1 `TapGestureClassifier` | TG-01~TG-10 |
| FR-TAP-03 | LLD-05A 3.2 | TL-01~TL-03 |
| FR-TAP-03a | LLD-05A 3.3 | TA-01~TA-13, TL-05 |
| FR-TAP-03b | LLD-05A 3.4 | (I 검사) |
| FR-TAP-04 | LLD-08 4절 `tab`, 6.1 | CC-08, ID-02 |
| FR-TAP-10 | LLD-08 4절 `arrow`, LLD-05A 6절 | ID-03, RT-04 |
| FR-TAP-11 | LLD-05A 3.3, 3.5(숫자류 입력창도 숫자 줄) | TL-04, ID-11 |
| FR-TAP-11a | LLD-08 6.1 `onTapOutput` | ID-01 |
| FR-TAP-12 | (차기 버전) | – |
| 탭 자판 동작 표(8행 × 3열) | LLD-05A 2.2(7·8행 = 정규화) | TK-01~TK-24, ID-14 |
| FR-SWP-01, 02 | LLD-04 9.3, 3.2 | EC-01, SC-02 |
| FR-SWP-03 | LLD-04 3.2 | EC-02~EC-05 |
| FR-SWP-04 | LLD-04 3.2 대각선 | EC-07, EC-08, SC-37 |
| FR-SWP-05 | LLD-04 4.3 1단계 | SC-01 |
| FR-SWP-10~17, 19 | LLD-04 4.3 `classifyConsonant`(판정 순서, 0.21 길이 단계 없음), 6.3 `VowelStrokeAssembler`(다음 후보 표 상태 기계, C 역할·앞 획 구역, 0.21), 6.1·9.3 매퍼 `consonant` | SC-01~SC-09, VS-01~VS-14, SM-01~SM-23, SW-17 |
| FR-SWP-18 | (0.19 해당 없음: 꺾기 규칙이 없어짐) | – |
| FR-SWP-20, 21 (22~28 삭제, 이슈 #150) | LLD-04 7.1·7.2 `ComposePolicy.SWIPE`(0.15: 병합 없음, 모음 긋기 연속 = 새 글자, 한 획 복모음 = 이벤트 하나) | HC-21, HC-22, SM-14, SM-14b, SM-15, SM-16, SM-40b, SM-42, BS-20 |
| FR-SWP-30, 34 (31~33 삭제) | LLD-04 6.2 `accept`(탭 → `Consonant`, 긋기 → `Consonant` + `Vowel`, 쌍자음 키 = 일반 자음 키, 0.18), 8.1 쌍자음 줄 | SM-01~SM-20 |
| FR-SWP-36 (37~39 삭제, 0.18) | LLD-04 6.2 `accept`(탭 = 자음 하나, 앞 동작과 합치지 않음), 9.3(안내 `guided = true`) | SM-16, SM-19, SW-05, SW-08b |
| FR-CMN-07a(긋기 ⌫) | LLD-04 6.2 `popElement`·7.3 `backspace`(자모 하나씩), LLD-08 6.1 `backspace`(0.18: `onBackspace` 없음) | SM-40~SM-42, ID-06 |
| FR-SWP-35(삭제, 0.12 이슈 #142 오너 결정) | LLD-04 4.6 "누른 시간"(누르고 있기 없음), LLD-06 2절(타이머 ② 없음) | SC-47~SC-50, BH-05, GT-04, SW-07, PG-02, DV-03 |
| FR-SWP-40 | LLD-04 8.1(영문 키 없음) | SL-01 |
| FR-SWP-42 | LLD-04 4.4, 8.2 | NK-01~NK-15 |
| FR-SWP-43 | LLD-04 4.5, 8.3 | SK-01~SK-06 |
| FR-SWP-44 | LLD-05A 2.2(탭 자판 ↑), LLD-04 4.4(둘레 숫자 ↑) | TK-04~TK-06, NK-01~NK-10 |
| FR-SWP-45 | LLD-04 2절 `KeyGeom.build`, 3.2(0.47: 왼쪽·오른쪽 테두리는 자판 사각형 바깥 변, 끝 키 바깥쪽은 보통 변 통과) | KH-05, KH-06, EC-09~EC-11, NK-15, KH-07, EC-16, SM-02~SM-05 |
| FR-SWP-50, 51 | LLD-04 8.1, 9.3(빈 칸) | SL-01, SL-02, SW-01 |
| FR-SWP-60~65 (0.16, 이슈 #160) | LLD-04 4.7 `classifyCheonjiin`, 6절 매퍼의 조립기(기준 키 예외·조립 닫기·`popElement`·`pendingGlyph`), 7.1 `ReplaceVowel` 단위, 8.1 천지인 키, 9.3 `guided`; LLD-05 4절(core-hangul 소속); LLD-06 4절; LLD-08 6.1 | SL-01, CK-01~CK-04, CJ-50~CJ-59, SW-10, SW-11, ID-30~ID-33 |
| FR-DSP-01~08 | LLD-04 5절·9.3(그 키의 음절, 모든 자음 키, 0.18), LLD-06 | PS-01~PS-18, GT-01~GT-10, SW-05, SW-08, PG-04, PG-05, PG-07, DV-01~DV-04 |
| FR-DSP-09 | LLD-04 9.4(획 자국) | SW-08b, SW-08c, SW-12, SW-13, KR-07, TU-61 |
| FR-DSP-10 (0.21, 이슈 #180 후속, 0.35 FSS#36·#37) | LLD-04 9.5 `CenterConsonantPainter`·`CenterText`, `SwipeHost.centerText`, LLD-08 6.1 `InputDispatcher.centerText`, LLD-04 6.3 `pendingDots`, 8.1 `SwipeLayout.consonantBox`, LLD-07 2.3 `PracticeInput.centerText` | VS-15, CS-01~CS-04, SW-14~SW-17·SW-14b, CT-01~CT-06, SL-08, TU-62, DV-29 |
| FR-TUT-01 | LLD-07 2.5 `PracticeLaunchPolicy`(처음 한 번 주제 목록만) | PR-09, SS-05, DV-11 |
| FR-TUT-02 (04·07·08 삭제) | LLD-07 2.2·2.3 | PR-01, PR-06~PR-08, PR-10~PR-12, PR-20, PR-24~PR-27, DV-24 |
| FR-TUT-05 | LLD-07 3절(고정값), 7절(안내 없음) | DV-11 |
| FR-TUT-06 | LLD-07 7절 | DV-11 |
| FR-TUT-09 | LLD-07 2.3 `PracticeEditor.display`·`PracticeFlow.echo` | PR-04, PR-06 |
| ~~FR-TUT-10~~ | SRS 0.68에서 삭제(FSS#17) | – |
| FR-TUT-11 | LLD-07 2.3 연습 자판(실제 `SwipeKeyboardView`, 늘 펼친 `GraffitiGuidePanel`, 0.44 `reveal`), LLD-05 10.2 | SW-08, GT-10, PR-07, PR-28, PR-29, DV-24 |
| FR-TUT-12 | LLD-07 2.4 `PracticeItemJudge`, 2.3 `PracticeFlow` 표시·`BoardFrame` | PR-02~PR-08, PR-10~PR-13, PR-20, PR-21, PR-24, PR-26, PR-28, PR-29 |
| FR-GRF-01 | LLD-05 8.1(보조 줄 모드 키 72dp) | DV-07, DV-22 |
| FR-GRF-02, 03, 03a | LLD-05 3절, 5.2, 7절(원본 획 표·미확인 처리) | GR-01~GR-24, GR-30, GS-16~GS-21, GH-18, GR-EVAL |
| FR-GRF-04 | LLD-05 3.3, 6절 | GR-11, GH-13 |
| FR-GRF-05, 05a, 05b | LLD-05 3.1, 5.2, 6절 ToggleLang | GR-02, GR-05~GR-09, GS-01~GS-11, GS-20, GS-24, GH-30, GH-31 |
| FR-GRF-05c | LLD-05 3.1 3번(⑥ 단서: 기호표의 직선·탭 획, 길이·영역 무관)·4번(⑦), 3.3 `ruleSymbol`, 5.2(7·9행, 일회성 끔, 대기 획 겹침 ⑥), 7절 `rule`, `cancelPending` | GR-25~GR-29, GR-31, GR-32, GS-23, GS-25~GS-45, BS-19, BS-23, GH-37 |
| FR-GRF-05d | LLD-05 8.1 패드 상태 표시, 6절 `redraw` | PI-01~PI-08, DV-25 |
| FR-GRF-06 | LLD-05 5.2 행 15·21 | GS-13, GS-19 |
| FR-GRF-07 | LLD-05 6절 `onStroke`(`glyph`, 무시 요소·글자 없는 획 제외), 8.3 `flash` | GH-35, GH-36, DV-18 |
| FR-GRF-08 | LLD-05 5.2 행 25, 6절 RecognitionFailed, LLD-08 8절 `error` | GR-16, GS-22, GH-33, ID-22, DV-22 |
| FR-GRF-09, 09a | LLD-08 7.1(버튼·덮개·터치 막기·닫는 때), LLD-05 10.2(✕·스크롤), LLD-08 3절(`onWindowHidden`·`onFinishInputView`) | GD-01~GD-09, DV-28 |
| FR-GRF-09b, 09c | LLD-05 10.1 `GuideCatalog`(칸 목록·`shape`), 10.2 그리기 | GC-01~GC-09, GD-10, GD-11 |
| FR-GRF-09d | LLD-05 10.2(`hidesDisplay`를 보지 않음, 저장 없음) | GD-01~GD-10(비밀번호 칸) |
| FR-SET-02, 04, 05 | LLD-07 6.1, 7절 | SS-01, GT-10, DV-11 |
| FR-SET-03 | LLD-07 6.1(진동 켬·소리 끔), LLD-08 8절(누를 때) | SS-01, FB-04, DV-21 |
| FR-SET-06 | LLD-07 6.1 `RowHeight`(40~64dp), LLD-08 7절 | SS-04, DV-11 |
| FR-SET-08 | LLD-07 6.1 `graffiti_enabled`·`swipe_enabled`, 7절 "입력 모드" | SS-08, OM-07, DV-11 |
| FR-SET-09 (0.36, FSS#25) | LLD-07 6.1 `WidthSetting`(`key_portrait_dp`·`align_portrait`·`key_landscape_dp`·`align_landscape`), 7절 설정 화면(0.38 방향별 상자 `KeyboardSizeBoxes`), LLD-08 7.2 `fullKeyDp`(0.47: / 11)·`FULL` | SS-10, SS-11, KW-06~KW-08, OB-01~OB-05, DV-30; 0.49 띄우기 스위치(LLD-07 7절, FSS#86): OB-06, OB-07, SS-12 |
| NFR-01 | LLD-06 3절 | DV-02 |
| NFR-02 | LLD-04 4.6 | PF-01, DV-15 |
| NFR-03 | LLD-09 | EG-01~EG-06, CC-20~CC-22, ID-19, EL-04 |
| NFR-04 | LLD-03 3.3(시각 주입), LLD-05 3.2 | GR-17, SC 전체 |
| NFR-05 | LLD-07 6.2, LLD-09 | EL-01~EL-05, ID-20 |
| NFR-06 | LLD-05A 3.2, LLD-04 8.1(기본 줄 높이 48dp 기준, TBD-16) | TL-02 |
| NFR-07 | LLD-05A 7절 | DV-10 |
| NFR-08 | LLD-03 1절 | MOD-03 |
| NFR-09 | LLD-11 1절 | (I) |
| NFR-10 | LLD-03 1절(minSdk 26) | DV-19 |
| ~~NFR-11~~ | SRS 0.65에서 삭제(이슈 #251). 모듈 경계 시험은 남음: LLD-03 2절 | MOD-01, MOD-02, MOD-04 |
| NFR-12 | LLD-05 3절, LLD-11 6.1 | GR-EVAL |

- SRS 사례 표 ↔ 시험 ID 대응(백스페이스 사례, 모드 키 예시, 숫자류 입력창 사례, 탭 자판 동작 표, 기호 배정표, FR-HGL-04d)과 SWIPE 5.1·5.2 대응은 [LLD-11-tests.md](LLD-11-tests.md) 머리말과 각 절에 있다.

## 4. 추적표 — HLD → LLD

### 4.1 HLD 절 → LLD 파일·절

| HLD 절 | LLD |
|---|---|
| 1 설계 원칙 | LLD-03 전체, README 1.3 |
| 2 시스템 구조(의존 방향: 병합 표 데이터 포함) | LLD-03 1절 |
| 3 모듈(3.1 빌드 변형은 HLD 0.59에서 삭제) | LLD-03 1·2절 |
| 4.1 한 획의 흐름(ACTION_CANCEL, 기능키·빈 칸) | LLD-04 9·10절 |
| 4.2 StrokeClassifier | LLD-04 3·4·5절 |
| 4.3 SwipeInputMapper | LLD-04 6절 |
| 4.4 HangulComposer | LLD-04 7절 |
| 5 그래피티 | LLD-05 |
| 5A 탭 자판 | LLD-05A, LLD-08 7절(창 높이), 8절(처리 시점의 피드백) |
| 6 안내 표시 | LLD-06 |
| 7.1 입력 연습 | LLD-07 2절(판정 `PracticeItemJudge` 2.4) |
| 7.2 기준값 | LLD-03 3.4, LLD-07 3절 |
| 7.3 저장 데이터 | LLD-07 4·6절 |
| 8 생명주기·플랫폼 | LLD-08 2~4·6~8절 |
| 8.1 ModeController | LLD-08 5절 |
| 9 오류 처리 | LLD-09 |
| 10 위험 | README 1.1 대응 목록 |
| 11 시험 설계 | LLD-11 |
| 12 설계 결정 | 각 파일 |
| 13 요구사항 → 구성 요소 | 이 문서 3절·4.2 |

### 4.2 HLD 13장 구성 요소 → LLD

| 구성 요소(HLD) | LLD 파일#절 |
|---|---|
| `KeyboardService`(숫자류 입력창 판별 포함) | LLD-08 3절 |
| `ModeController`(`enterNumericField`·`leaveNumericField`·`returnMode`) | LLD-08 5절 |
| `TapKeyboardView` | LLD-05A 3·4절 |
| `GraffitiPadView`(보조 줄) | LLD-05 8절 |
| `ComposingController` | LLD-08 4절 |
| `onUpdateSelection` 조건 | LLD-08 4.1 |
| `HangulComposer`(백스페이스 정책·입력 단위 목록·모드 정책) | LLD-04 7절 |
| `SwipeInputMapper`(탭·긋기 → 자모, 천지인 조립, 초기화) | LLD-04 6절 |
| `CheonjiinAssembler`(⌫ 순서) | LLD-05 4절, 6절 |
| ⌫ 반복 | LLD-05A 6절 |
| `onStartInputView` 비밀번호 판별 | LLD-08 2절·3.1 |
| `GuideOverlay` | LLD-06 3절 |
| `ErrorLog` | LLD-07 6.2 |
| `TouchTracker` | LLD-04 9.1 |
| `InkCollector` | LLD-05 8.2 |
| `onEvaluateFullscreenMode`, `onConfigurationChanged` | LLD-08 3절 |
| `core-hangul` 모드 정책 | LLD-04 7.1 |
| `stroke-core`(탭 자판 동작 표) | LLD-05A 2절 |
| `stroke-core`(변 통과·키 기하) | LLD-04 2·3절 |
| `StrokeClassifier`, 결과 해석 표(둘레 숫자·기호) | LLD-04 4절, 8.2·8.3 |
| `SwipeKeyboardView`, 자판 배치 데이터 | LLD-04 8·9절 |
| `ProgressState` | LLD-04 5절 |
| `PracticeActivity`, `PracticeItemJudge` | LLD-07 2절 |
| `Recognizer`, `GraffitiState` | LLD-05 3절, 5절 |
| `SettingsActivity`, `SettingsStore` | LLD-07 7절, 6.1 |
| `Feedback`(누를 때, 인식 실패 오류 진동) | LLD-08 8절 |
| `ACTION_CANCEL` 처리 | LLD-05A 4절, LLD-04 9.3, LLD-05 8.3 |
| `ExploreByTouchHelper` | LLD-05A 7절 |
| 빌드, `SwipeModule` | LLD-03 2절 |

LLD가 더한 내부 구성 요소(HLD 구성 요소를 나눈 것): `KeyHitTester`, `EdgeCrossingTracker`, `TapGestureClassifier`, `TapKeyResolver`, `ScriptTables`, `InputDispatcher`, `EditorPort`, `FieldInfo`, `KeyboardRootView`, `MultiPointerQueue`, `RepeatTimer`, `GuideTimerLogic`, `GraffitiInputHandler`, `TemplateMatcher`, `TutorSession`, `TutorLaunchPolicy`, `ThresholdCodec`, `RowHeight`, `EngineGuard`, `SwipeEngineState`, `SwipeTouchLogic`(0.5).

## 5. LLD에서 정한 세부

동작 규칙을 바꾸지 않는 구현 선택과 매개변수 초기값이다(1.3). 0.2에서 설계가 정하게 된 것은 지우거나 범위를 줄였다(D-19).

| ID | 내용 | 위치 | 이유 |
|---|---|---|---|
| D-01 | 패키지 루트 `kbd`(임시), 앱 ID `kbd.app` 하나(0.32: `.personal` 접미사 없음, 이슈 #251) | LLD-03 1절, 2.1 | 0.31 전에는 두 변형을 한 기기에 같이 설치해 비교 |
| D-02 | `testDebugUnitTest` 별칭 태스크를 JVM 모듈과 app에 등록 | LLD-03 2.1 | 프로젝트 시험 명령(CLAUDE.md)이 변형·JVM 모듈에서 그대로 돌게 |
| D-03 | 엔진 단위 dp·ms, 앱이 px → dp 변환 | LLD-03 3.3 | 기준값이 dp 절댓값(FR-CMN-12), 결정성(NFR-04) |
| D-04 | 대각선에서 두 축이 같은 비율이면 가로 변 | LLD-04 3.2 | 결정성. 실제로 같을 확률은 거의 없음 |
| D-05 | 통과 시각 = 조건을 만족한 표본 시각(보간 안 함) | LLD-04 3.2 | 표본 간격(약 8ms)이 쌍자음 시간 창(약 350ms)보다 훨씬 짧음 |
| D-06 | NaN 좌표 표본은 버림 | LLD-04 3.3 | 플랫폼 이상값 방어 |
| D-07 | 꺾기 판정 구간 길이 `SEG_DP` = 6dp | LLD-04 4.2 | 손 떨림 방향 잡음 제거 |
| D-08 | 시간 창 "안" = `≤`(0.14: 쌍자음 시간 창 하나, 끊는 간격은 이슈 #150로 없어짐) | LLD-04 6.2 | HLD 4.3 "이하" 문구 |
| D-09 | 잘못된 이벤트(중성 없는 `ReplaceVowel` → 무시, 쌍자음 없는 `DoubleConsonant` → 무시)는 안전하게 무시하고 한 번 보고(0.14: `MergeVowel`이 없어짐) | LLD-04 7.2 | 앱이 죽지 않게(HLD 9장) |
| D-10 | 모드가 바뀌면 조합기를 새 정책으로 다시 만듦 | LLD-04 7.3 | 모드 전환 전 확정(FR-CMN-03d)이라 상태 손실 없음 |
| D-11 | 인식 매개변수 초기값(탭 8dp, 직선도 0.92, 방향 ±20°, 점수 0.80, 재표본 32점, 손 떼는 꼬리 0.2(FSS#47)) | LLD-05 3절 | 템플릿 인식기 관례값. 평가 샘플로 조정 |
| D-12 | (삭제 — 사용자에게 보이는 동작이라 DCR-21로 옮김, 검수 1회차 m5. DCR-21은 결정됨) | – | – |
| D-13 | 인식 글자 표시 600ms | LLD-05 8.3 | FR-GRF-07 "잠깐"의 초기값 |
| D-14 | q 윗첨자는 U+107A5(유니코드 14) 사용, 글꼴 지원은 실기기 확인 | LLD-05A 2.3 | SWIPE 6장이 윗첨자 없는 글자를 두지 않음 |
| D-15 | 모서리 표시 글자 40%, 안내 현재 글자 키 높이 0.8배 | LLD-05A 3.4, LLD-06 3절 | 레이아웃 초기값 |
| D-16 | 반복 첫 지연·간격 = `ViewConfiguration` 키 반복 값 | LLD-05A 6절 | FR-CMN-07c "첫 지연 후 일정 간격"의 값으로 플랫폼 기본값 |
| D-17 | "움직이기 시작" = 플랫폼 터치 슬롭 | LLD-06 2절 | FR-DSP-01 문턱값 |
| D-18 | 튜터 대상 동작마다 표본 10개 | LLD-07 2.1 | 백분위수 계산에 필요한 최소 수 |
| D-19 | 튜터 경계의 비교 표본 짝 가운데 6단계 보통 누름(2·4·5단계에서 얻음)과 7단계 SHORT 표본 출처. 1·3단계 짝과 1·4·5단계 공식은 이제 HLD 7.1이 정함(DCR-09 결정) | LLD-07 2.2 | HLD 7.1 공식에 넣을 두 분포를 정함. 사용자가 하는 동작은 SWIPE 10장 그대로 |
| D-20 | 백분위수 선형 보간 | LLD-07 3절 | 표본 수가 적을 때 안정 |
| D-21 | 기준값 저장은 임시 파일 + 이름 바꾸기 | LLD-07 4절 | 저장 중 종료에도 이전 값 보존 |
| D-22 | 기준값 다시 읽기 = `onStartInputView`에서 파일 수정 시각 비교 | LLD-07 4절 | 튜터 뒤 "다음 입력부터"(FR-TUT-04) |
| D-23 | 기준값 값 범위 검사 | LLD-07 4절 | 손상 판단(HLD 9장) |
| D-24 | 설정은 서비스 시작 때 한 번 막고 읽은 뒤 Flow로 갱신 | LLD-07 6.1 | 입력 뷰를 만들 때 값이 필요 |
| D-25 | 탭 자판 배치 데이터는 `stroke-core`, 긋기 자판 배치 데이터는 `gesture-engine`에 둠 | LLD-03 1.2 | JVM 시험(TL·SL)과 모듈 분리(0.32: 변형 없음) |
| D-26 | 오류 기록 64KB, 넘으면 앞 절반 버림 | LLD-07 6.2 | HLD 7.3 "크기 제한 순환" |
| D-27 | `onWindowHidden`에서 진행 중 터치·타이머 버림 | LLD-08 3절 | 창이 사라진 뒤 늦은 입력 방지 |
| D-28 | `onStartInputView`의 `restarting`을 구분하지 않음 | LLD-08 3.1 | HLD 8장에 구분 없음. 같은 숫자류 칸 재표시도 쿼티로 다시 시작(FR-CMN-04b)과 맞음 |
| D-29 | 커서 기대값을 하나가 아닌 확인 전 목록으로 둠 | LLD-08 4.1 | 갱신 알림 지연. R4 실기기 확인 |
| D-30 | 긋기 자판 둘레 숫자·기호를 넣기 전에도 조합 확정 | LLD-08 6.1 | FR-TAP-11a·FR-GRF-04와 같은 규칙(조합 중 글자 뒤에 기호가 붙음). 화면 결과는 확정 여부와 같음 |
| D-31 | `EngineGuard`는 `Exception`만 잡고 `Error`는 잡지 않음 | LLD-09 2절 | OOM 등은 복구 불가 |
| D-32 | 편집기 쓰기 도중 예외도 다른 예외와 같이 엔진 상태만 되돌림(조합기 강제 확정·표시 상태 비움 없음). `ComposingController`는 스냅숏에 넣지 않고 포트 호출이 돌아온 뒤에만 자기 상태를 바꿈. `AndroidEditorPort`는 `InputConnection` 반환값을 예외로 바꾸지 않음 (0.5에서 바꿈). 포트 호출이 던져도 연 배치는 닫고(`endBatch`는 `finally`), 임시 글자를 빼는 쓰기가 돌아오면 `composingShown`에서도 바로 뺌 (0.6, PR #91) | LLD-09 2절, LLD-08 4절 | HLD 9장 "조합 상태는 획 시작 전으로", EG-05와 맞춤(이슈 #75). 0.4 규칙은 EG-05와 반대 결과였음. 0.6은 PR #91 동작에 문구를 맞춤(이슈 #92, CC-20~CC-22) |
| D-33 | 저장된 줄 높이가 40~64dp 밖이면 읽을 때 가까운 끝값으로 | LLD-07 6.1 `RowHeight.clamp` | 범위(FR-SET-06)는 설정 화면이 지키고, 손상·이전 판 값 방어만 함 |
| D-34 | 첫 실행 자동 시작 표시(`tutor_auto_started`)는 튜터를 띄우기 **전에** 저장 | LLD-07 2.3 | "한 번만"(FR-TUT-01)이 튜터 도중 종료에도 지켜지게 |
| D-35 | 템플릿 정규화의 중심 = 재표본 점의 무게중심, 코너 = 앞뒤 2점 현 사이 방향 변화 ≥ 60°, 연달아 표시된 점은 코너 하나, 구석 탭 경계는 미포함(`<`) | LLD-05 3.1, 3.2 | 이웃 구간으로 재면 표본 사이 90° 모서리를 놓침(G11). 인식 매개변수라 평가 샘플로 조정 |
| D-36 | 사용자 커서 이동 확정에서 편집기가 조합 구간을 알리지 않으면(`candE = −1`) 마지막으로 아는 구간 끝을 쓰고, 그것도 없으면 위치를 옮기지 않음 | LLD-08 4절 `finishAfterUserMove` | 사용자가 고른 위치를 지킴(FR-CMN-06, 설계질의 #57 Q2 "세부 방식은 개발팀이 정함", G13) |
| D-37 | 긋기 안내 후보의 표시 거리는 손가락에서 잼. 숫자 키 RUB(↑/↓)·기호 키 STAGE2 = 변까지 + `longDp`(자음 키 OUTER와 같은 자리), 기호 키 STAGE3 = 변까지 + 2 × `longDp` | LLD-04 5.2, LLD-06 3절 | SWIPE 9.1 "같은 자리"·"고리 바깥". STAGE3 자리는 레이아웃 초기값(T25 PR #77 구현, 이슈 #73) |
| D-38 | 튜터 "입력: …" 줄은 20자까지 그대로, 넘으면 `"…"` + 끝 19자 | LLD-07 2.3 입력 표시 | FR-TUT-09 "길어지면 끝부분만". 글자 수는 화면 폭에 맞춘 초기값 |
| D-39 | (0.12 삭제: 누르고 있기가 없어짐, 이슈 #142 오너 결정) | – | – |
| D-40 | (0.14 삭제: 다시 나누기·run이 없어짐, 이슈 #150) | – | – |
| D-41 | (0.19: 1-1 1, 1-2 5, 3-1·3-4·5-1 1, 3-2 삭제) 튜터 항목은 목록 순서로 돌고 세는 항목마다 `minSamplesPerTarget`번(1-1 1, 1-2·3-1·3-2 5, 2-1 10, 5-1 1. 0.15: ✓만 셈, 3-3 삭제). 한쪽 표본 10개 이상이 되게 고른 수(0.14: 2-1은 두 분포라 10, 4-1 삭제) | LLD-07 2.2 | SWIPE 10장 "횟수는 LLD" |
| D-42 | (0.19 삭제: 3-1의 되돌아오기 측정과 3-2가 없어짐, 이슈 #173) | – | – |
| D-43 | (0.14 삭제: 4-1이 없어짐, 이슈 #150) | – | – |
| D-44 | 5-1 문장 4개("아이가 고기를 먹어요", "밖까지 과일을 사러 가요", "박가가 곡기를 끊었다", "안녕하세요 학교에 갔다 왔어요"). 측정 없음(0.14: 오판정 다시 보기 삭제) | LLD-07 2.2 | SWIPE 10장 5-1 예(곡기, 학교, 아까, 과일, 했다, 안녕하세요) |
| D-45 | (0.18 삭제: `doubleTapWindowMs`가 없어짐, 이슈 #168) | – | – |
| D-46 | 1-2 아·오·이는 `TutorStep.practiceOnly`: 차례에는 나오나 표본·진행 수에 넣지 않음 | LLD-07 2.2 | SWIPE 10장 "측정·끝나는 조건 불변"(PM 결정 #150) |
| D-47 | (0.18 삭제: 두 번 탭 쌍자음이 없어짐, 이슈 #168) | – | – |
| D-48 | (0.18 바뀜) 튜터 연습 자판의 오류 진동은 입력 표시가 그 획을 이어질 수 없어 무시된 천지인 요소로 해석했을 때만 낸다(`PracticeEcho.lastIgnored`, `TutorFlow` `errorVibe` = `Feedback.error`, 진동 설정을 따름). 0.15~0.17은 기준 키 밖 긋기 | LLD-07 2.3·2.5 | SWIPE 10.1 "튜터에서 나는 오류 진동은 무시된 천지인 요소뿐"(이슈 #168) |
| D-49 | (0.18 바뀜) 화면 과 번호: 1과 기본 / 2과 복모음(문서 3과) / 3과 문장(문서 5과), 단계 줄도 화면 번호(`TutorFlow.screenId`), 단계 id는 그대로 | LLD-07 2.2 | SWIPE 10장 "화면 번호는 연속, 화면 문구는 LLD" |
| D-50 | 튜터 ✓·✗는 시간으로 지우지 않고 다음 동작을 누를 때까지 보인다. ✓ 초록 `#388E3C`, ✗ 빨강 `#D32F2F`(48sp, 큰 글자 오른쪽). 쓰는 법 안내는 그 항목이 ✓가 될 때까지 | LLD-07 2.5 | SWIPE 10.1 "보이는 시간, 색 값, 화면 배치는 LLD". 누름에서 비우므로 따로 시간이 필요 없음 |
| D-51 | 튜터 이상값: 1-1 터치가 키 사각형 밖 10dp 넘게 나가면 표본으로 쓰지 않음(시도는 ✓). 0.18: 2-1 간격 조건은 2-1과 함께 지움 | LLD-07 3절 | SWIPE 10.1 "이상값 처리는 추정의 일이며 LLD"(PM 결정 #154 Q3) |
| D-52 | (0.18 삭제: 기준 키 밖 긋기가 없어짐. 이어질 수 없어 무시된 천지인 요소는 그대로 `markInput` 없음, LLD-08 6.2) | – | – |
| D-53 | 튜터 3-4는 세는 항목 1개 × `minSamplesPerTarget` 1(practiceOnly 아님): 한 번 ✓면 단계가 저절로 끝남. 측정 칸 없음(`rulesOf` 빈 목록) | LLD-07 2.2 | SWIPE 10장 "한 번 맞히면 끝" |
| D-55 | 튜터 5과 연습 자판의 ⌫는 뗄 때 한 번 처리하고 누르고 있어도 되풀이하지 않음 | LLD-07 2.2 | 연습 자판은 획을 뗄 때 받는 구조(`PracticeStrokeCollector`)이고 문장 한 개 고치는 데는 한 번씩이면 충분. 실제 키보드의 누를 때 처리·반복(FR-CMN-07c)과 지우는 결과는 같다 |
| D-56 | 튜터 연습 자음 돌림은 다음 단계로 넘어갈 때와 겹쳐서 같은 단계를 다시 할 때(`Overlap`) ㄱ부터 다시 센다. 짝의 둘째·고정 항목은 순번을 쓰지 않는다 | LLD-07 2.2 | SWIPE 10장은 "단계가 바뀌면 ㄱ부터"만 정함. 다시 하는 단계는 `targetIndex`·진행 수도 처음으로 돌아가므로 같은 차례로 보이게 함 |
| D-57 | 조립을 닫으며 모음이 못 된 획을 버릴 때 오류 진동은 사용자가 누른 입력(자음 탭·숫자·기호·천지인 키·스페이스·엔터·탭·모드·설정 키)일 때만. 커서 이동·입력창 바뀜·입력 시작/끝은 조용히 버림 | LLD-04 6.2, LLD-08 6.1 | SWIPE 3.3 "다른 입력이 오면 … 짧은 오류 진동"은 사용자 입력에 대한 알림. 앱 쪽 사건에 진동하면 사용자가 원인을 모름 |
| D-58 | (0.21 삭제: 오너 결정 "길이에 비례하는 여유 없음", 이슈 #179) 0.20까지: 모음 획 위치 관계의 끝 여백 m = min(t, 선 길이 / 3). ㅜ·ㅠ·ㅏ의 "가운데"와 가로 획 뒤 "가운데/끝 나누기"를 m으로. ㅗ의 "ㅡ가 점보다 t 이상 왼쪽"은 t 그대로(ㅏ와 겹치지 않게) | LLD-04 6.3 | SWIPE 3.6 "확인할 것": 선이 2t보다 짧으면 가운데가 비어 ㅜ·ㅏ의 점을 받지 못함. 3t 이상이면 설계 값과 같음 |
| D-59 | (0.21 개정: 첫 획 포함 모든 획에 SWIPE 0.35 10.1 위치 말, 길게·짧게 없음, LLD-07 2.5) 0.20까지: 튜터 쓰는 법 안내의 위치 말은 SWIPE 10.1 목록의 자리에 늘 붙임(아·까도 "그 가운데에서 오른쪽으로 짧게") | LLD-07 2.5 | SWIPE 10.1 보기에서 가는 붙이고 아·까는 안 붙여 같은 모음이 자음에 따라 달라짐. 위치 말 규칙("이어지는 획에는 자리를 붙인다")을 따름 |
| D-60 | 튜터 1-2의 획 차례에 변위가 0인 동작은 ✗(축 없음). 조금이라도 움직였으면 축·방향으로 판정 | LLD-07 2.5 | SWIPE 10.1 "변위가 0이라 축을 정할 수 없는 동작은 … ✗(세부는 LLD)" |
| D-61 | 탭 흔들림 표본 = 누른 점 → 뗀 점 변위의 큰 성분(0.32: public 1-1 = 가장 먼 거리는 삭제, 이슈 #251) | LLD-07 3절 | HLD 7.1 "4.2 판정과 같은 값"은 긋기 자판의 탭/획 경계 |
| D-62 | (0.21 개정) ㅓ②·ㅢ②(가로 획 하나 뒤 세로 획)는 끝 자리 하나 — 낱모음은 `x ≥ R − t` 또는 `L − t ≤ x < R − t`이고 `T < y − t/2`(위에서 가로지름), C가 있으면 역할 ㅣ + `x ≥ L − t` — 로 받고, 그 안에서 높이 몫 r(< 2/3 = ㅓ)과 0.5 특례(가로 < ㅣ × 0.5 = ㅓ)로만 가름. C의 가로 점이 첫 획이면 ㅓ뿐. ㅔ③은 보임과 상관없이 옆이면 ㅔ. 0.20의 `R − m ~ R + 키 폭`·닿음 ±t/4는 없앰 | LLD-04 6.3 | SWIPE 0.35 3.6.3 "끝"·"끝 넓힘"과 같아짐(0.20에서 LLD가 정한 "자리 하나"는 오너 결정으로 설계에 들어감, 이슈 #179 PM 기록). 길이 비례 여유·거리 상한·닿음은 오너 결정으로 없앰 |
| D-63 | (설계 결정 #192, SWIPE 0.36 3.6.3·3.6.5) C가 있을 때 ㅜ ②·ㅠ ③의 "아래"는 시작 높이 조건(`T ≥ H.y − t/2`)을 보지 않음: ㅜ ② = 역할 세로 점 + `x ≥ H.L − t`, ㅠ ③ = 역할 세로 점 + `x ≥ ②.x + t/2` | LLD-04 6.3 | 0.21 첫 판은 임시 처리였다(SWIPE 0.35 3.6.3 C 절과 3.6.5 (가) "구 T 140" 행이 엇갈림). 설계 PR #193(SWIPE 0.36)이 같은 결론(C가 있으면 역할로 나뉘고 T 조건 없음)으로 정함. 코드 변경 없음 |
| D-64 | (설계 결정 #192, SWIPE 0.36 3.4, 0.21 2회차 개정) 낱모음의 ‥ 상태(세로 둘 = ㅛ 앞, 가로 둘 = ㅕ 앞)는 화면에 **"‥"만** 보임(C가 있으면 "ㄱ‥"). 조합기는 첫 획의 ㅣ / ㅡ를 그대로 갖고, 조립기의 `pendingReplacesVowel`로 앱이 그 글자 자리에 ‥를 그림(`ComposingController.show(…, replacesLast)`, `SwipePracticeEcho.text`). ⌫로 둘째 획을 빼면 ㅣ / ㅡ, 닫으면 ㅣ / ㅡ가 남음(진동) | LLD-04 6.3, LLD-08 4절 | SWIPE 0.36 3.4 "표시와 조합기가 들고 있는 모음 상태를 어떻게 맞출지는 LLD". 조합기에 모음 빼기·되돌리기 사건을 두면 스페이스·엔터·커서 등 "확정 뒤 조립 닫기" 경로마다 되돌리기를 끼워야 해서, 표시만 바꿈. 0.21 첫 판의 "ㅣㆍ"·"ㅡㆍ"는 버림 |
| D-65 | (0.34, FSS#17; 0.37 FSS#43 바뀜) 입력 연습의 긋기·그래피티 항목의 `steps`는 필순대로 넣었을 때 **각 동작 뒤 연습 편집기의 글자열**(조합 글자·임시 ㆍ·‥ 포함)이고 실제 엔진으로 확인해 시험이 지킨다. 0.36까지는 판정이 동작마다 이것과 비교했으나, 0.37부터 판정은 **확정된 글자열**(스페이스 뺌)로만 한다(`PracticeItemJudge.commit`) | LLD-07 2.1·2.4 | SWIPE 10.2 "확정된 글자로 판정"(오너 결정 FSS#43) — 조합 중인 글자는 결과가 아니다. 확정 글자는 실제 편집기 표시라 실제 키보드와 어긋날 수 없다 |
| D-66 | (0.34; 0.37 FSS#43 바뀜) 연습 자판에서 탭 자판의 ⌫와 **긋기 자판·그래피티 패드의 스페이스**(글자 확정, 끝 동작) 말고는 기능 키(↵·화살표·Esc·Del·Tab·모드 키·탭 자판의 스페이스·긋기 자판과 패드의 ⌫ 등)가 보이되 아무 일도 하지 않는다 | LLD-07 2.3 | 실제 뷰를 그대로 쓰면서 SWIPE 10.2 "긋기 연습 자판·패드에는 편집 키가 없다", "편집 줄의 다른 키는 판정에 들어가지 않는다"를 지킴 |
| D-67 | (0.34) 그래피티 연습의 안내표는 화면 크기와 상관없이 그 항목의 구획만(한글 / 영문 / 숫자) 보인다, 높이 200dp | LLD-07 2.3 | SWIPE 10.1 ③ "화면이 작으면 필요한 구획만" — 안내표는 길어 어느 화면에서도 스크롤하므로 늘 줄임 |
| D-68 | (0.34) 옛 기준값 파일 `thresholds.json`은 읽지도 지우지도 않는다 | LLD-07 4절 | HLD 7.2 "남은 파일을 지울지는 LLD" — 쓰는 곳이 없고 앱 삭제 때 지워짐 |
| D-54 | 긋기 자판·연습 자판은 키 폭의 0.8보다 넓은 가운데 라벨을 줄여 그림(천지인 키 "ㆍ ㅡ ㅣ") | LLD-04 8.1·9절, LLD-07 2.3 | 에뮬레이터에서 라벨이 키 밖으로 잘림을 확인(이슈 #160) |

## 6. 설계 변경 요청과 설계 질의

### 6.1 설계 변경 요청(DCR) — 결정 기록

LLD 0.1이 올린 DCR-01~21은 오너가 2026-10-27 권장안대로 승인했고(이슈 #3), 그 뒤 오너 결정으로 **DCR-08·DCR-20은 취소**, **DCR-10은 대체**됐다(숫자류 입력창 = 영문 쿼티, 튜터는 처음 한 번 자동). 설계 반영은 PR #4에서 했고(SRS 0.15부터), 현재 근거 판(머리말)에도 그대로 있다. 결정을 기다리는 항목은 없다.

| ID | 내용(0.1의 질문) | 결정 | 설계 반영 위치 | LLD 0.2 반영 위치 |
|---|---|---|---|---|
| DCR-01 | 길이 기준이 축마다 다른가 | `longDp` 하나를 가로·세로에 씀 | SRS 1.3, FR-SWP-11 / SWIPE 1.1·1.2·3.1·10장 / HLD 7.2 | LLD-03 3.4, LLD-04 4.3 R1, 시험 SC-46 |
| DCR-02 | 탭 자판 키·둘레 숫자 키의 나갔다 돌아옴·좌우 문지르기, 기호 키 ↓ | 나갔다 돌아옴·좌우 문지르기 = 그 방향 나감, 기호 키 ↓로 시작한 획 = 탭 | SRS 탭 자판 동작 표 7·8행, FR-SWP-42·43 / SWIPE 6·7·8장 / HLD 4.2, 5A | LLD-05A 2.2(정규화), LLD-04 4.4·4.5·8.3, `Undefined` 삭제, 시험 TK-19~TK-24, NK-13, NK-15, SK-05, SK-06, TG-09, TG-10, PS-16, ID-14 |
| DCR-03 | 긋기 자판 빈 칸을 누르면 | 터치만 받고 입력 없음(빈 띠와 같음) | SRS FR-SWP-50 / SWIPE 2.1 / HLD 4.1 | LLD-04 8.1, 9.3, 시험 SW-01 |
| DCR-04 | 성립한 모음 뒤 이어질 수 없는 천지인 요소 | 무시(상태·표시·입력 단위 그대로) | SRS FR-HGL-04c·04d, 백스페이스 사례 21행 / GRAFFITI 6.2-7 / HLD 5장 | LLD-05 4.2 `push` 4번, 6절, 시험 CJ-26, CJ-41, GH-27~GH-29, BS-24 |
| DCR-05 | 그래피티 시프트·캡스록·대기 세부 | 캡스록 중 시프트 = 캡스록 끔, 한글 시프트 두 번 = 끔, 그 밖 시프트 소멸, 대기 중 다른 제어 획 = 동작 + 대기 소멸, 구두점 대기도 ⌫로 취소 | SRS FR-GRF-05c, 백스페이스 사례 20행 / GRAFFITI 6.2-8 / HLD 5장 | LLD-05 5.2 표(7·9행, 일회성 끔), `cancelPending`, 시험 GS-23, GS-25~GS-32, BS-23 |
| DCR-06 | 그래피티 안 한영 전환 때 조합 중 글자·임시 글자 | 확정하고 임시 글자 버림 | SRS FR-GRF-05a, FR-HGL-04e / GRAFFITI 6.2-10 / HLD 5장 | LLD-05 6절 ToggleLang, 시험 GH-30, GH-31 |
| DCR-07 | 인식 실패 획 | 입력 없음 + 오류 진동 | SRS FR-GRF-08(신설) / GRAFFITI 6.2-9 / HLD 5장, 8장 피드백 | LLD-05 2절 `RecognitionFailed`, 5.2 25행, 6절, LLD-08 6.1 `error`, 8절, LLD-09 4절, 시험 GR-16, GS-22, GH-33, ID-22 |
| DCR-08 | 숫자 입력창 그래피티 패드의 모드 키·typed | **취소**(오너 결정: 숫자류 입력창은 영문 쿼티로 시작, 숫자 입력창 전용 패드 없음) | SRS FR-CMN-04·04b, TBD-11 / GRAFFITI 6.3·8장 / HLD 5장·8.1(반영 되돌림) | LLD-05 머리말·8.1(숫자 전용 패드 삭제), LLD-08 3.1(`numberOnly` 삭제), 시험 GH-25 삭제 |
| DCR-09 | 비교할 "긴 쪽"이 없는 튜터 단계의 경계 공식 | 1단계 = 탭 흔들림 P99 + 흔들림 여유(초기 4dp), 4·5단계 = 의도 간격 P90 × 시간 창 계수(초기 1.5), 겹침 검사 없음. personal 3단계는 1단계 값 확인 | HLD 7.1·7.2 / SWIPE 10장 | LLD-07 2.2, 3절 `oneSidedDistance`·`oneSidedWindow`, 시험 TE-07~TE-10, TU-07~TU-09 |
| DCR-10 | 기본값 동작 중 튜터 안내 방식 | **대체**(오너 결정: 안내 없음. 튜터는 처음 설치 후 한 번 자동, 그 뒤 설정에서만) | SRS FR-TUT-01·05, TBD-15 / SWIPE 10장 / HLD 5A·9장 | LLD-07 2.3 `TutorLaunchPolicy`, 6.1 `tutor_auto_started`(`tutor_done` 삭제), 7절, LLD-09 3절, 시험 TU-10~TU-12, SS-05 |
| DCR-11 | 처리 시점(기능키, 진동·소리, `ACTION_CANCEL`) | 기능키 누를 때, 진동·소리 누를 때, `ACTION_CANCEL` = 떼지 않은 모든 터치 입력 없음(뗀 대기 결과는 순서대로, 반복 멈춤) | SRS FR-CMN-11a(신설), FR-SET-03 / HLD 4.1, 5장, 5A, 8장 | LLD-05A 4·5절(`onCancelAll`), 6절, LLD-04 9.3, LLD-05 8.3, LLD-08 8절, 시험 MQ-07, MQ-08, FB-01~FB-03, SW-02~SW-04, RT-05 |
| DCR-12 | 그래피티 보조 줄 버튼 폭 | 모드 키 72dp, 나머지 세 버튼 같은 폭 | SRS FR-GRF-01, TBD-11 / HLD 5장 | LLD-05 8.1, 시험 DV-22 |
| DCR-13 | 동작 없는 한 줄 입력창 엔터, 선택 영역 ⌫ | `KEYCODE_ENTER` 보냄, 편집기 기본(선택 삭제) | SRS FR-CMN-09, 07 / HLD 8장 | LLD-08 4절 `enter`·`deleteBefore`, 시험 CC-07, ID-16, BS-25, DV-17 |
| DCR-14 | 진동·소리 기본값, 줄 높이 범위 | 진동 켬·소리 끔, 줄 높이 40~64dp(NFR-06은 기본 줄 높이 기준, TBD-16) | SRS FR-SET-03, 06, NFR-06, TBD-16 / HLD 8장 | LLD-07 6.1(`RowHeight`), 7절, LLD-08 7·8절, 시험 SS-01, SS-04, FB-04 |
| DCR-15 | `BlockParser`의 병합 표 참조가 의존 방향에 드는가 | 든다(병합 표 데이터는 정의 쪽). 0.14부터 병합 표는 조합기 정책만 씀(이슈 #150) | HLD 2장 | LLD-03 1.1 |
| DCR-16 | 안내 "현재 글자"에 나눠 쓰기 병합 결과를 보이나 | 이번 획 결과만 | SRS FR-DSP-03 / SWIPE 9.2 / HLD 4.2 | LLD-04 5.1·5.2, LLD-06 3절, 시험 PS-15 |
| DCR-17 | typed 분류 누락('J' 획, 천지인 요소 획) | 'J' 획 = 입력 아님, 천지인 요소 획 = 입력 | SRS FR-CMN-03a / HLD 8.1 | LLD-08 6.2, LLD-05 6절, 시험 ID-21, GH-34 |
| DCR-18 | 꺾기 뒤 세로 움직임의 판정 순서 | 첫 나감 길이·되돌아온 거리는 꺾기 전까지만 잼 | SWIPE 3.4 / SRS FR-SWP-18 / HLD 4.2 | LLD-04 4.3 R3·R8, 시험 SC-23~SC-26, SC-44, SC-45 |
| DCR-19 | 비밀번호 입력창에서 그래피티 인식 글자 표시 | 끔. (0.21: SRS 0.44 FR-CMN-08 ①/② 분리로 **학습 금지 입력창에서는 보임**. 0.20까지는 학습 금지 칸도 끔) | SRS FR-CMN-08, FR-GRF-07 / GRAFFITI 6.2-6 / HLD 5장 | LLD-05 6절 `onStroke`·`InputSink.hidesDisplay`(0.21), 8.3, 시험 GH-35, DV-14 |
| DCR-20 | TBD-14(숫자 자판 배치) 결정 전 보일 자판 | **취소**(TBD-14 폐기, 숫자 키패드 없음) | SRS FR-CMN-04, TBD-14 / HLD 8장·10장(반영 되돌림) | LLD-05A 3.5(숫자 자판 데이터 삭제), LLD-08 2절(`Keypad` 삭제)·3.1·6절(`keypadActive` 삭제) |
| DCR-21 | 임시 글자(ㆍ·‥)를 버리는 확정의 범위 | 모든 확정에서 버림 | SRS FR-HGL-04e·04d / HLD 5장 | LLD-05 6절, 시험 GH-26, GH-30, GH-32 |

- 숫자류 입력창 결정(오너 2026-10-27)의 LLD 반영: LLD-08 2절 `FieldInfo.numericLike`(비밀번호는 비밀번호 변형만, 학습 금지 플래그만 있는 칸 제외), 3.1(진입·이탈·같은 칸 재표시), 5절 `returnMode`·`enterNumericField`·`leaveNumericField`, 6.1 `onModeKey`(숫자류 입력창 안 저장 제외), LLD-05A 3.5, 시험 NF-01~NF-09, FI-01~FI-12, ID-11, ID-23, DV-20.

### 6.2 설계 질의(답함)

설계질의 #6(Q1~Q3)은 설계팀 명확화(PR #8)와 오너 결정(이슈 #6 OD-1~OD-3, PR #9)으로 답이 났고 SRS 0.16·HLD 0.12·GRAFFITI 0.9에 반영됐다. 세 답 모두 LLD 0.2의 임시 처리와 같아 동작은 바뀌지 않았다. 0.3에서 본문 메모를 확정으로 바꾸고 근거를 설계 문서 절로 옮겼다. 설계질의 #55·#57(구현 중 질의)은 0.4에서 반영했다. 열린 질의는 없다.

| 이슈 | 질문 | 확정(설계 근거) | LLD 반영 위치 |
|---|---|---|---|
| 이슈 #6 Q1 | 그래피티 패드 획, 긋기 자판 빈 칸·빈 띠에 키 입력 피드백을 내는가 | 내지 않음. 보조 줄 버튼은 누를 때 냄(SRS FR-CMN-11a ②, HLD 8장 피드백, GRAFFITI 6.2-9, 오너 결정 OD-1) | LLD-04 9.3, LLD-05 8.3, LLD-08 7·8절, 시험 SW-01, FB-03 |
| 이슈 #6 Q2 | 그래피티 대기 중 대기를 만드는 다른 획·한영 전환 획, 기호표에 없는 획이 오면 | 대기를 만드는 획·한영 전환 획 = 앞 대기를 지우고 자기 동작(FR-GRF-05c ⑥). 확장·구두점 대기 뒤 기호표에 없고 제어 획도 아닌 획 = 대기를 지우고 평소대로 입력(05c ⑦, 오너 결정 OD-2). 기호표 획·FR-GRF-05 조합·05c ①②가 먼저(SRS FR-GRF-05c ⑥⑦, HLD 5장, GRAFFITI 6.2-8) | LLD-05 3.1 3번(기호표 다음 평소 집합), 3.3, 5.2 6·10·11·12행과 그 아래 메모, 시험 GR-14, GR-25, GR-26, GS-33~GS-38 |
| 이슈 #6 Q3 | ⌫·방향키 반복 한 번마다 키 입력 피드백을 내는가 | 내지 않음, 누를 때 한 번(SRS FR-CMN-11a ②, HLD 8장 피드백, 오너 결정 OD-3) | LLD-05A 6절, LLD-08 8절, 시험 FB-02, SW-04 |
| 이슈 #55 | BP-37·BP-40의 크기·보기는 시간 창 충돌인가 | 아님. 자음이 다르면 언제나 새 글자, 같은 키 머묾 = 새 블록이라 크키·보비(SWIPE 0.12 4.3, PR #59) | LLD-11 4.4 BP-37·BP-40 (G10) |
| 이슈 #57 Q1·Q2 | resync 뒤 `composeStart`가 −1로 남음 / `candE = −1`인데 사용자 위치를 당김 | 둘 다 LLD 오류(FR-CMN-06, HLD 8장): 조합 중 resync-OWN이면 `composeStart = candS`, 끝을 모르면 옮기지 않음(세부는 개발팀) | LLD-08 4절·4.1, D-36, 시험 CC-18·CC-19 (G12, G13) |

### 6.3 LLD 빈틈(G1~G13) 처리 — 이슈 #13

구현·작업지시서 작성 중 찾은 LLD 내부 빈틈이다. 사용자에게 보이는 동작을 새로 정한 것은 없다(G10·G12·G13은 설계 답대로 고침). G2~G11은 작업지시서·PR의 임시 처리를 그대로 확정했다. 번호 겹침: T21(PR #58)이 G10으로 올린 LLD-05 3.2 코너 빈틈은 **G11**로 부른다(설계팀 제안, 이슈 #13).

| ID | 빈틈 | 0.4 처리 | 반영 위치 | 출처 |
|---|---|---|---|---|
| – | `app`에 `org.jetbrains.kotlin.android`를 적었으나 AGP 9.x는 Kotlin을 내장해 이 플러그인을 거부함 | AGP 내장 Kotlin, 이 플러그인 적용 안 함 | LLD-03 1절 | T01, PR #16 |
| G1 | 안드로이드 클래스가 대상인 단위 시험은 승인된 라이브러리로 JVM에서 못 돌림 | Robolectric(시험 전용, MIT) 승인 요청 → 승인됨(이슈 #12, T36 PR #120) | LLD-03 4절, LLD-11 9절 | 작업지시서 README 8절, T36 |
| G2 | 그래피티 Enter가 `out.enter()`인데 `enter(f)`에 필요한 입력창을 `InputSink`가 모름 | `InputSink.field`, `out.enter(sink.field)` | LLD-05 6절, LLD-08 6.1 | T10, T29 |
| G3 | `GraffitiState.interpret` 19~24행이 `Symbol(id)`의 출력을 찾을 길이 없음 | `TemplateSet.outputOf(id)`, `GraffitiState(templates)` | LLD-05 3.3, 5절 | T12, T23 |
| G4 | `PracticeLayout`·`TargetAction`·`StepProgress`·`StepOutcome`에 필드 없음 | T10의 최소 형태 | LLD-07 2.1 | T10 |
| G5 | D-09 "오류 기록"인데 core-hangul은 `ErrorLog`를 못 봄 | 생성자 콜백 `onInvalid: (String) -> Unit` | LLD-04 7.1, D-09 | T06 |
| G6 | `onModeKey`·`onStartInputView`가 부르는 `GraffitiState.enterMode()`가 `GraffitiInputHandler` 안에 숨음 | `GraffitiInputHandler.enterMode()` 위임 | LLD-05 6절, LLD-08 3.1·6.1·9.3, LLD-11 NF-03·ID-18 | T29, T31, T37 |
| G7 | 병합 못 하는 `MergeVowel`의 대체 처리를 다시 접을 때 되풀이·재보고 | 단위에 `Vowel(v)`로 쌓음 | LLD-04 7.2, D-09 | T06, PR #37 |
| G8 | `ReplaceVowel`이 마지막 모음 단위만 바꾸면 병합 중성(ㄱ ㅗ ㅏ)에서 접기 불변식이 깨짐 | 모든 모음 단위를 `Vowel(v)` 하나로 바꿈 | LLD-04 7.1 | T06, PR #37 |
| G9 | 쌍자음이 없는 자음의 `DoubleConsonant(b)` 처리 없음 | 무시하고 오류 보고 | LLD-04 7.2, D-09 | T06, PR #37 |
| G10 | BP-37·BP-40 기대값 크기·보기가 LLD-04 6.2(같은 키 머묾 = 새 블록)와 다름 | 크키·보비, 근거 표기 정정 | LLD-11 4.4 | T16, 설계질의 #55, PR #59 |
| G11 | 코너 "구간 방향 변화 ≥ 60°"의 구간이 정해지지 않음(이웃 구간이면 표본 사이 90° 모서리를 놓침), "중심" 정의 없음 | 앞뒤 2점 현(`CORNER_WINDOW = 2`), 연속 표시점 = 코너 하나, 무게중심 | LLD-05 3.2 2·5번, 3.1(구석 경계), D-35 | T21, PR #58 |
| G12 | resync로 OWN 판정할 때 `composeStart`가 −1로 남아 다음 자기 갱신을 USER_MOVED로 오판(방향키·⌫ 뒤 첫 한글 조합이 끊김) | 조합 중 resync-OWN이면 `composeStart = candS`. **T18 코드 수정** | LLD-08 4.1, LLD-11 CC-18 | T18, 설계질의 #57 Q1 |
| G13 | `finishAfterUserMove`가 `candE = −1`일 때 모든 위치를 임시 글자 길이만큼 당김(사용자 커서가 밀림) | 마지막으로 아는 구간 끝 기준, 없으면 옮기지 않음. USER_MOVED가 `composeStart`를 지우지 않음. **T18 코드 수정** | LLD-08 4절·4.1, D-36, LLD-11 CC-19 | T18, 설계질의 #57 Q2 |

- #57 Minor(같은 기대 커서 두 번 큐잉)는 설계 사항이 아니고 실기기 R4 확인(DV-06)에서 본다.

### 6.4 LLD 빈틈 처리(0.5) — 이슈 #75, #83

T27(PR #78)·T30(PR #84) 구현 중 찾은 LLD 0.4의 빈틈이다. 두 PR이 고른 임시 처리가 SRS 0.16·HLD 0.12·SWIPE 0.12와 맞아 그대로 확정했다. 사용자에게 보이는 동작을 새로 정한 것은 없다(오류 경로는 HLD 9장 그대로, 안내 표시는 FR-DSP-01·03 그대로).

| 이슈 | 빈틈 | 0.5 처리 | 반영 위치 |
|---|---|---|---|
| #75-1 | `EngineGuard.stroke` 스냅숏에 `ComposingController` 기대 커서가 있으나 API가 없음 | 스냅숏은 엔진 상태(조합기 입력 단위, `SwipeEngineState`, `GraffitiInputHandler.snapshot()`)만. `ComposingController`는 포트 호출이 돌아온 뒤에만 상태를 바꾸므로 넣지 않음(T27 처리) | LLD-09 2절, LLD-08 4절·6.1, LLD-05 6절 |
| #75-2 | EG-05(되돌림)와 D-32(편집기 쓰기 중 예외면 확정·비움)가 반대 결과, D-32 처리 위치 없음 | 편집기 쓰기 중 예외도 되돌림만(D-32 바꿈). EG-05 예외 지점 = `finishAll` 안의 `EditorPort.finishComposingText`. 그래피티 되돌림 EG-06 추가 | LLD-09 2절, D-32, LLD-11 EG-05·EG-06 |
| #83-1 | `GuideOverlay.onDown`에 누른 점이 없음, LLD-04 9.3과 LLD-06 3절 인자가 다름 | `onDown(key, keyRectPx, origin: StrokePoint)`, `t = origin.t`(T30 처리) | LLD-06 3절, LLD-04 9.3 |
| #83-2 | 움직이지 않고 누른 채 표시 지연이 지나면 그릴 `ProgressState`가 없음 | DOWN에서 `begin` 뒤 `guard.run { ProgressState.of(classifier) }`를 `guide.onProgress(ps, p)`로 한 번(T30 처리). 시험 SW-05 | LLD-04 9.3, LLD-06 3절, LLD-11 SW-05 |
| #83-3 | `GuideTimerLogic.flags`가 생성자 고정인데 overlay는 입력창·설정이 바뀌어도 남음 | 누를 때마다 flags를 계산해 다르면 overlay를 다시 만듦(T30 처리). 기준값은 뷰를 `onStartInputView`마다 새로 만들 때 반영 | LLD-06 3절 |
| – | LLD-04 4.6 `StrokeClassifier` 생성자에 키 사각형(`KeyGeom`)이 없음(T15 코드 주석이 지적한 이슈 #13 때의 누락) | `StrokeClassifier(key, th, geom)`, DOWN에서 `guard.run("StrokeClassifier.begin")`으로 만듦(T15·T30 코드) | LLD-04 4.6, 9.3 |

- 코드 후속(0.5 본문과 다르거나 아직 없는 코드): ① `GraffitiInputHandler.snapshot()`·`restore()`(LLD-05 6절)가 없음 — T29 파일. ② `InputDispatcher`의 `Snapshot`에 graffiti 필드가 없고 `snapshot()`·`restore()`에 graffiti 칸이 없음 — 그래피티 분기를 넣는 T31(이슈 #85)에서 함께, 시험 EG-06. ③ `AndroidEditorPort`가 `InputConnection` 반환값을 예외로 바꾸지 않음(LLD-08 4절) — T32에서 지킴. ④ (선택) T30 `SwipeTouchLogicTest`의 `consonant tap ...` 시험에 SW-05 ID를 붙이고 `onDown`의 키 사각형·누른 점 인자와 `onDown` → `onProgress` 순서를 단언으로 보강. T27·T30의 나머지 코드는 0.5와 같다.

### 6.5 T25 후속·PR #91 문구 맞춤(0.6) — 이슈 #73, #92, #88

사용자에게 보이는 동작을 새로 정한 것은 없다. #73 RUB 방향은 SWIPE 9.2 "다음 동작을 하면 만들어질 글자를 그 동작 방향에"를 그대로 따른 LLD 정정이고, #92는 머지된 코드(PR #91)가 고른 처리가 HLD 9장·NFR-03과 맞아 문구를 코드에 맞췄다.

| 이슈 | 빈틈 | 0.6 처리 | 반영 위치 |
|---|---|---|---|
| #73-1 | RUB 후보 표시 방향이 언제나 "e1.dir의 반대". 손가락이 이미 키 안으로 돌아왔으면(R45 L45 = 가) 다음 동작은 바깥(e1 쪽)으로 나가는 것인데 반대쪽에 그림 | 표시 방향 = 첫 다리의 방향(밖이면 e1.dir 반대, 안이면 e1.dir). 거리는 그대로(e1 변까지). 표의 RUB 행 칸 어긋남(다리가 "켜지는 상태" 칸에 있던 것)도 바로잡음 | LLD-04 5.2, LLD-06 3절, LLD-11 PS-17 |
| #73-2 | "고리 바깥"(숫자 키 RUB ↑/↓, 기호 키 STAGE2·STAGE3)의 표시 거리 미정 | T25 구현 확정: 손가락에서 STAGE2·숫자 RUB = 변까지 + longDp, STAGE3 = 변까지 + 2 × longDp. 표시 거리의 기준점(손가락)도 표 아래에 적음 | LLD-04 5.2, LLD-06 3절, D-37, LLD-11 PS-18 |
| #92 | D-32·LLD-08 4절·LLD-09 2절이 PR #91 앞 동작(임시 글자가 `composingShown`에 남음, `beginBatch` 뒤 예외면 `endBatch` 안 불림)을 적음 | `endBatch`는 `finally`로 항상, 임시 글자를 빼는 `setComposingText`가 돌아오면 `composingShown`·`glyph`에서도 바로 뺌. "포트 호출이 돌아온 뒤에만 상태를 바꾼다"는 호출 하나 단위로 유지 | LLD-08 4절, LLD-09 2절, D-32, LLD-11 CC-20~CC-22 |
| #88 | 그래피티 조립기·`GraffitiState` 스냅숏이 `GuardedState`에 없음 | 이미 해결: LLD 0.5(PR #86, LLD-05 6절·LLD-08 6.1·LLD-09 2절)와 코드 PR #91(#89, 커밋 d2f93e6, 시험 EG-06). 이 판에서 고친 것 없음 | – |

- 6.4 코드 후속 ①②④는 PR #91(#89)에서 끝났다. ③(`AndroidEditorPort` 반환값)은 T32(#90)에서 지킨다.
- 코드 후속(이슈 #96): ① `Probes.kt` RUB 표시 방향(손가락이 안이면 `u`) + 시험 PS-17. ② 시험 PS-18(코드는 이미 0.6과 같음). ③ (선택) `ComposingControllerTest`의 PR #91 시험 세 개에 CC-20~CC-22 ID.

### 6.6 그래피티 원본 획 표·기호표의 직선·탭 획·PR #104 동작(0.7) — 이슈 #105, #103, #79

사용자에게 보이는 동작은 LLD가 새로 정하지 않았다. 모두 설계 문서(SRS 0.17~0.20, HLD 0.13~0.16, GRAFFITI 0.10~0.13, SWIPE 0.13)에 적힌 오너 결정·명확화를 함수 단위로 옮긴 것이다.

| 근거 | 빈틈 | 0.7 처리 | 반영 위치 |
|---|---|---|---|
| SRS FR-GRF-05c ⑥ 단서, GRAFFITI 6.2-8 ①·6.5, 설계질의 #79 Q3, OD-4·OD-5 A | 3.1이 탭·직선을 규칙으로 먼저 정하고 바로 돌려줘서, 기호표에 실린 직선·탭 획(`.` `,` `'` `-` `!` `\`, `±` `×` `¡`)이 기호가 되지 못함. 5.2 행 3·10과 GS-34(대기 + ↑ = 시프트)가 새 설계와 부딪힘 | 3.1 3번 "규칙 토큰을 기호표에서 먼저 찾음"(`TemplateSet.ruleSymbol`), 템플릿 JSON `rule` 칸. 5.2 표는 그대로이고 2·3행 설명·10행 주석·표 아래 메모를 고침. GS-30·31·34·38을 고치고 획 수준 GS-39~GS-45, GR-27~GR-32 추가 | LLD-05 3.1·3.3·5.2·7절, LLD-11 1.3·6.1·6.3 |
| PM 판단 #79(명확화), SRS 0.19 | 길이로 가르지 않는 판정 | 3.1 3번 메모: 직선 토큰은 길이와 무관, 기호표 항목은 토큰 단위 | LLD-05 3.1, GR-27·GR-28·GS-39 |
| 오너 결정 #79 OD-6·OD-7 A, OD-1·OD-2 A, GRAFFITI 6.1·6.3·6.4 | 7절이 "원본 설명서와 대조 전"이라 적음. 미확인·원본에 없는 획의 처리 없음 | 7절: 원본 획 표를 보고 직접 그림. 미확인 칸이 있는 획은 GRAFFITI 6장 머리·6.4 끝대로 T28 임시에 있으면 유지(`interim`: B ①·D ①·H·G ①·O·0·8 ①·X ①·°·€ `x_euro`), 없으면 넣지 않음(쐐기형 `_` `\|` `` ` `` `;` `:`, P ①·R ①, 8 ②, ∫, Ø). 원본에도 4.2에도 없는 `num_4b` 없음, K = α 고리, `•` 없음, 대체 획은 코너 수가 다르면 다른 id | LLD-05 7절 |
| GRAFFITI 6.1·6.4(되짚는 획), 이슈 #103 3항 | 되짚는 획(`_` `\|` `` ` `` `;` `:`, B ① D ① P ① R ① H)이 직선 규칙에 걸리지 않는지 정함 없음 | 3.1 메모(직선도가 작아 템플릿으로 감, 따로 되짚음 특징 없음), 시험 GR-30(B ①·D ①·H 템플릿, 쐐기 모양 획이 직선이 아님) | LLD-05 3.1, LLD-11 6.1 |
| #105-1, SRS FR-GRF-07·FR-HGL-04c, #74 A, PR #104 | 6절 `onStroke` flash 조건이 "RecognitionFailed가 아니면" | `glyph(a)`가 빈 문자열이거나 조립기가 요소를 무시했으면(`dropped`) flash 없음 | LLD-05 6절, GH-36 |
| #105-2, SRS 3.3, HLD 5A, SWIPE 2장, #93 OD-1~3 A, PR #104 | 7절에 시스템 막대 인셋·여백 색·창 높이 없음 | `navigationBars ∪ tappableElement`(변마다 큰 값)를 아래·옆 padding, 여백 색 = `BACKGROUND_COLOR`, 창 높이 = 6줄 + 아래 여백, 자식 폭 = 남은 폭, 여백 터치 소비 | LLD-08 7절, DV-07·DV-08 |
| #105-3, OD-3 A, PR #104 | 6.4에 무시 요소 flash 없음·대기 중 ← = 대기만 취소 시험 ID 없음 | GH-36, GH-37 | LLD-11 6.4 |

- 코드 후속(이슈 #103, 작업지시서 [T38](../tasks/T38-graffiti1-templates.md)): `TemplateSet.ruleSymbol`·`Template.rule`·`TemplateLoader` `rule` 검사, `Recognizer` 3번 단계, 원본 획 표대로 템플릿 교체(`build_templates.py`), dev·eval 합성 획 다시 만들기, GR-27~GR-32·GS-30·31·34·38~45 시험. GH-36·GH-37은 PR #104의 시험에 ID만 붙인다. LLD-08 7절은 PR #104 코드와 같다(코드 후속 없음).

### 6.7 그래피티 영역 비율 선택지·낡은 문구(0.8) — 이슈 #121, #108

사용자에게 보이는 동작은 LLD가 새로 정하지 않았다. 설계 0.21 판(SRS 0.21·HLD 0.17·GRAFFITI 0.14)의 오너 결정 #108 A와 이미 머지된 코드(T26 `SettingsStore`, T33 설정 화면, T35·T37 그래피티 패드)를 본문에 맞췄다.

| 근거 | 빈틈 | 0.8 처리 | 반영 위치 |
|---|---|---|---|
| SRS FR-SET-05, GRAFFITI 6.3, 오너 결정 #108 A | 6.1 표가 기본값 2/3만 적고 고를 수 있는 값·저장 형식이 없음. LLD-05 8.1이 기본 2:1만 적음 | 선택지 1 : 1 / 2 : 1(기본) / 3 : 1, 저장값 = 문자 영역의 몫 1/2·2/3·3/4(Float). 목록 밖 저장값은 코드대로 검사 없이 그대로 쓰고 설정 화면은 아무것도 고르지 않은 상태 | LLD-07 6.1·7절, LLD-05 8.1 |
| 이슈 #12(Robolectric 승인), T36 PR #120 | LLD-03 4절·LLD-11 9절·6.3 G1이 "승인 요청 중" | 승인됨으로 | LLD-03 4절, LLD-11 9절, 6.3 G1 |
| SWIPE 0.13, GRAFFITI 0.14, SRS 0.21 | LLD-04·LLD-05 머리의 근거 판이 SWIPE 0.12·GRAFFITI 0.13·SRS 0.20 | 판 번호 갱신(SWIPE 0.13·GRAFFITI 0.14의 바뀐 곳은 각각 LLD-08 7절·이 표 첫 행이 이미 다룸) | LLD-04·LLD-05 머리, 이 문서 머리 |

- 코드 후속 없음. 같은 PR에서 쓰이지 않는 `placeholder_*` 문자열(T37에서 `PlaceholderModeView` 삭제)을 지웠다(이슈 #121).

### 6.8 튜터 "입력: …" 줄(0.9) — 이슈 #124

오너 요청(이슈 #124)을 설계팀이 SRS 0.22 FR-TUT-09·SWIPE 0.14 10장·HLD 0.18 13장에 넣었고(PR #125), LLD는 그 판에 맞춰 모듈·함수만 정했다. 사용자에게 보이는 동작은 LLD가 새로 정하지 않았다(줄 길이 20자만 D-38 초기값).

| 근거 | 0.9 처리 | 반영 위치 |
|---|---|---|
| SRS FR-TUT-09 | `PracticeEcho`(단계 시도마다 새로 만듦 = 비움), `QwertyPracticeEcho`(탭 자판 경로), `SwipePracticeEcho`(긋기 경로, personal), `PracticeLayoutSource.practiceEcho`, `PracticeKeyboard`가 자판 폭을 같이 넘김, 판정 기준값 = `onCreate`에서 읽은 저장값 | LLD-07 2.1·2.3, 시험 TU-13~TU-17 |
| SRS 0.22·HLD 0.18·SWIPE 0.14 | 근거 판 갱신 | 이 문서 머리, LLD-04 머리 |

- 측정·추정(`TutorSession`, `ThresholdEstimator`)은 바꾸지 않았다. 코드는 같은 PR에 있다.

### 6.9 머물기 삭제·끊는 간격·튜터 과(0.10) — 이슈 #127

설계팀이 SWIPE 0.15~0.18·SRS 0.23~0.26·HLD 0.19~0.22에 오너 결정(머물기 삭제, J1 받침 우선, J2·J3 A, M6 A′, 누르고 있기, R5 B안, 튜터 과·번갈아 연습·쓰는 법 모두, 타수)을 넣었고(PR #128~#131), LLD는 그 판에 맞춰 모듈·함수·시험을 정했다. 사용자에게 보이는 동작은 LLD가 새로 정하지 않았다(D-39~D-44는 설계가 LLD에 맡긴 것과 정본 읽기).

| 근거 | 0.10 처리 | 반영 위치 |
|---|---|---|
| FR-SWP-22·23·26~28·30~35, HLD 4.3 | `BlockParser` 다시 씀: 연속 입력, `press`/`cancelPress`, 끊는 간격(같은 키 간격끼리, 하나면 기준 간격, 들어오는 간격은 `incomingRefGapMs`), 받침 우선(동점은 받침), 누르고 있다 뗌·긋기 블록, `UndoLast` × n 정정, `RunStart` | LLD-04 6절, LLD-03 3.2, D-39 |
| HLD 4.4 | 조합기 run: 확정 경계를 넘는 `UndoLast`, 입력창 반영 방법 | LLD-04 7절, D-40 |
| FR-SWP-35, HLD 4.2·6 | `StrokeClassifier.held`·`tick`, `ProgressState.beforeExit`·`held`, 누르고 있기 타이머, "새 글자"·머물기 타이머 삭제 | LLD-04 4.6·5절·9.3, LLD-06 |
| HLD 7.2 형식 2 | `Thresholds`·`ThresholdCodec`(형식 1 읽기: 기준 간격 = 창 ÷ 3, 들어오는 = 병합 창 ÷ 2(PM 결정 PR #132)) | LLD-03 3.4, LLD-07 4·5절 |
| FR-TUT-02·03·10, HLD 7.1 | 튜터 과(설명 → 번갈아 연습), 2-1·3-3 기준 간격(중앙값), 4-1 항목·표본, 5-1 문장·오판정 다시 보기, 타수·정확도 | LLD-07 2.1~2.4·3절, D-41~D-44 |
| 위 전부 | 시험 다시 씀·더함 | LLD-11 0.10 머리 문단 |

- PM 결정(PR #132): 형식 1의 `incomingRefGapMs` = 병합 시간 창 ÷ 2("애매하면 받침"), 들어오는 간격 경계가 병합 시간 창을 넘는지 검사하지 않음(넘으면 어차피 끊으므로 일관), 쉬고 쓴 밖가는 끊은 표본이 아님(연습만), TAB도 BlockParser를 비움(ID-25·26). HLD 7.2의 ÷ 2 한 줄은 설계 문서 PR로 고침.
- 코드는 같은 PR에 있다.

### 6.10 그래피티 패드 상태 표시(0.11) — 이슈 #136

설계팀이 PM 결정(이슈 #136 1~3, 오너 요청 방향)을 GRAFFITI 0.15 4.2 "패드 상태 표시"·SRS 0.28 FR-GRF-05d·HLD 0.24 5장에 넣었고(PR #137), 위치·크기·색·겹칠 때 배치를 LLD에 맡겼다. LLD는 LLD-05 8.1에 정했다.

| 근거 | 0.11 처리 | 반영 위치 |
|---|---|---|
| FR-GRF-05d ①, FR-GRF-05a | `GraffitiPadGeometry.cornerRect`(인식기와 같은 `CORNER_RATIO`·`letterRect`), 반지름 2dp 점 + 언어 글자 "한"/"A" | LLD-05 8.1 |
| FR-GRF-05d ② | 대기 표시를 패드 오른쪽 위 한 자리에 ⇪ ⇧ • ↘ 순서로(0.10의 ↘ 자리) | LLD-05 8.1 |
| FR-GRF-05d ③ | `hidesDisplay`(0.21, 0.20까지 `restricted`)를 보지 않음 | LLD-05 8.1 |
| HLD 5장(초기화 뒤 다시 그림) | `GraffitiInputHandler(redraw)` — `enterMode`·`reset`·`restore`에서 부름 | LLD-05 6절 |
| 위 전부 | 시험 PI-01~PI-08, DV-25 | LLD-11 6.5, 8절, 9절 |

- 남은 PM 판단(설계 검수 PR #137의 경미 지적, 막지 않음): 캡스록은 한영 전환 뒤에도 남아 한글 언어에서는 효과가 없다. 문서 문구("켜져 있는 동안 계속")대로 한글 언어에서도 ⇪를 그린다. "영문일 때만 ⇪"로 바꾸면 `pendingLabel` 한 줄과 PI-04가 바뀐다.
- 코드는 같은 PR에 있다.

### 6.11 누르고 있기 삭제(0.12) — 이슈 #142

오너 베타 신고(ㅑㅛㅠㅕ 문지르는 중 쌍자음, 갸 → 꺄)를 PM이 "멈춰 있기" 규칙으로 고치려 했으나, 오너가 방향을 바꿨다: "일단 누르면 복자음 만드는 것은 빼자. 버그를 만든다." 누르고 있기 = 쌍자음(FR-SWP-35)을 엔진·안내에서 없앤다. 설계 문서(SRS FR-SWP-35·30·32·FR-TUT-11·1.3, HLD 4.2·4.3·6장 타이머 ②, SWIPE 5.1·9.2·10장·12.1·13장)는 설계팀이 따로 고친다. 그 판이 나오기 전이라 이 판의 근거는 이슈 #142 오너 결정 코멘트다.

| 근거 | 0.12 처리 | 반영 위치 |
|---|---|---|
| 이슈 #142 오너 결정(엔진) | `StrokeClassifier.held`·`holdCapable`·`tick`, `StrokeResult.held`, `ProgressState.held` 삭제. `textOf`는 늘 키의 자음 | LLD-04 4.1·4.6·5.1·5.2 |
| 〃(블록 해석) | `SwipeAction.held`, `ActionKind.HOLD_LIFT`·`HOLD_SWIPE`, `BlockType.HOLD_LIFT`·`HOLD_SWIPE` 삭제. 쌍자음은 두 번 탭·탭 + 긋기로만 | LLD-04 6.1·6.2, D-39 삭제 |
| 〃(안내) | 타이머 ②(`TimerKind.HOLD`, `onHoldTimer`, `onHold`, `holdCapable`, `holdTick`) 삭제, `onMove`의 `crossedEdge` 인자 삭제 | LLD-06 2·3·4절, LLD-04 9.3 |
| 〃 | 시험: SC-47~SC-50 다시 씀(오래 눌렀다 뗌 = ㄱ, 오래 눌렀다 문지르기 = 갸, 키 안 작은 왕복 뒤 문지르기 = 갸), BH-05·GT-04·SW-07·PG-02·DV-03 바꿈, BP-15·19·45·BH-01~04·GT-05·09 삭제 | LLD-11 0.12 머리 문단 |

- 튜터 2-1의 누르고 있기 연습 항목·설명 문구와 LLD-07 2.1의 과 표, TU-* 시험은 튜터 작업(#143·#144)이 고친다. 이 판은 LLD-07을 고치지 않았다.
- `doubleTapWindowMs`는 쌍자음 시간 창으로만 쓴다(LLD-03 3.4).
- 코드는 같은 PR에 있다.

### 6.12 튜터 입력 표시: 1~4과 큰 한 글자(0.13) — 이슈 #143

오너 요청("문장이 아닌 이상 크게 한 자")에 PM이 정한 것(이슈 #143 본문)을 LLD에 넣었다. SRS FR-TUT-09 문구는 설계팀이 따로 고친다(이 판의 근거 판 표기는 SRS 0.29 그대로).

| 근거 | 0.13 처리 | 반영 위치 |
|---|---|---|
| #143 1~4과·public 1-1 | `TutorFlow.echoChar`(= `PracticeEcho.text`의 마지막 코드 포인트, 조합 중이면 그 글자, 다시 나누면 그 결과의 끝 글자)를 64sp로, 80dp 상자 가운데. 앞머리·정오 색 없음 | LLD-07 2.3 |
| #143 5과 | `echoAsLine`(= `SentenceStep`)이면 지금처럼 "입력: …" 줄 + 타수·정확도 | LLD-07 2.3 |
| #143 표시 전용 | 같은 `PracticeEcho`·같은 기준값·같은 비우는 때. 측정·표본 불변 | LLD-07 2.3 |
| 위 전부 | TU-17 바뀜, TU-27~TU-31 | LLD-11 |

- 코드는 같은 PR에 있다.

### 6.13 튜터 1-1 자음 모두·누르고 있기 항목 없앰(0.13) — 이슈 #144, #142

오너 요청(이슈 #144 "자음을 다 쳐보라고", 이슈 #142 오너 결정 "누르고 있기 = 쌍자음 삭제")에 PM이 정한 것을 넣었다. SWIPE 10장·SRS 문구는 설계팀이 따로 고친다.

| 근거 | 0.13 처리 | 반영 위치 |
|---|---|---|
| #144 personal 1-1 | 목표 = 자음 14개 차례로, 항목당 1(표본 14개 ≥ `MIN_SAMPLES`) | LLD-07 2.2 |
| #144 public 1-1 | 목표 = q~p 10개 차례로, 항목당 1(이슈 예시는 8개 이상이나 `MIN_SAMPLES` = 10이라 10개) | LLD-07 2.2 |
| #144 목표가 아닌 키 | 다른 단계와 같이 키를 보지 않음: 그 탭이 목표의 표본이 되고 다음 목표로 | LLD-07 2.2 |
| #142 튜터 | 2-1 누르고 있기 2항목·4-1 누르고 있기 6항목·2과·4과 설명의 누르고 있기 문장 없앰(그 항목들은 표본이 없었으므로 측정 불변) | LLD-07 2.2 |
| 위 전부 | TU-07·TU-09·TU-17·TU-18 바뀜, TU-18b·TU-32·TU-33 | LLD-11 |

- 공식·저장 형식은 그대로다. 코드는 같은 PR에 있다.

### 6.14 자음(탭)·모음(긋기) 분리(0.14) — 이슈 #150

설계팀이 SWIPE 0.23~0.24·SRS 0.32~0.33·HLD 0.28~0.29에 오너 결정(기준 키, 기준 키 밖 긋기 = 무시 + 짧은 진동, 짧은 두 번 탭만 쌍자음, 모음 시작 = ㅇ 탭, ⌫ 자모 하나씩)과 PM 결정(⌫ 뒤 기준 키 복원, 2-1 양쪽 측정, 낱모음 안내, 나눠 쓰기 병합 유지, 형식 1·2의 `doubleTapWindowMs` 버림, 1-2 아·오·이)을 넣었다(PR #151, #152). LLD는 이를 함수로 옮겼다.

| 근거 | 0.14 처리 | 반영 위치 |
|---|---|---|
| HLD 4.3, FR-SWP-30·36~39 | `BlockParser` 삭제 → `SwipeInputMapper`(기준 키, 쌍자음 후보, accept 표, `onBackspace`, `keyIdOf`) | LLD-04 6절, LLD-03 1.2·2.2 |
| HLD 4.4, FR-CMN-07, FR-SWP-20·34 | `ComposePolicy.SWIPE` = 긋기 병합 표 + `mergeConsecutiveVowel` + 자모 단위 ⌫. run·`RunStart`·`MergeVowel`·`BackspaceMode` 삭제, `lastConsonant()` 추가 | LLD-04 7절, LLD-03 3.2 |
| HLD 4.2·6, FR-DSP-01·03·04 | `textOf` = 낱모음, 안내는 기준 키에서만(`guided`, `SwipePracticeGuide.anchorKeyId`) | LLD-04 5.2·9.3, LLD-06 |
| HLD 7.1·7.2, FR-TUT-02 | 4-1·3-3 측정·5-1 다시 계산 삭제, 2-1 = `boundary`, 글자 항목 = 탭 + 긋기, 1-2 아·오·이(D-46), 과 번호(D-49) | LLD-07 2.1·2.2·3절 |
| HLD 7.2 형식 3 | 6칸 삭제, 형식 1·2는 공통 칸만 읽고 `doubleTapWindowMs`는 기본값 | LLD-07 4·5절, LLD-03 3.4, D-45 |
| HLD 8·9장 | ⌫: 조합기 → `swipeState.onBackspace(lastConsonant)` → 편집기(편집기 쓰기가 마지막), 매퍼 상태도 스냅숏 | LLD-08 6.1, LLD-09 |
| 시험 | BP·BB·RS·BH·SJ 삭제, SM-01~SM-45 신설, HC-21~HC-24·HC-50~HC-53·BS-20·BS-21, TU-03·TU-04·TU-09·TU-20·TU-34, TS-01~TS-09, SW-05·SW-08, PG-07 | LLD-11 |

- 설계와 다른 처리 없음. `ComposePolicy.DUBEOLSIK`을 그대로 쓰지 않고 긋기 정책을 두벌식 오토마타와 같게 만든 것은 SWIPE 4.1·HLD 4.4(긋기 병합 표 유지, 가 + ㅣ = 개)를 따른 것이다.
- 코드는 같은 PR에 있다.

### 6.15 튜터 항목 판정·모든 모음 한 획(0.15) — 이슈 #154, #156

설계팀이 SWIPE 0.25~0.26·SRS 0.34~0.35·HLD 0.30~0.31·REQUIREMENTS 0.7에 오너 요청(이슈 #154: 맞음·틀림 판정, 틀리면 쓰는 법 안내 + 다시, 다음으로 넘어가지 않음)과 오너 결정(이슈 #156: 모든 모음은 한 획, 조합은 일반 두벌식과 같고 차이는 쌍자음 = 짧은 두 번 탭과 기준 키뿐), PM 결정(TBD-17 = A 측정 단계 ✓면 목표 글자, #154 Q2 겹침 검사로 막음, Q3 이상값은 LLD, TBD-18 = B 고 + ㅏ = 고ㅏ, 한 획 ㅘ ⌫ 한 번)을 넣었다(PR #155, #157). LLD는 그 판에 맞췄다.

| 근거 | 0.15 처리 | 반영 위치 |
|---|---|---|
| FR-HGL-04b, FR-SWP-20·21, TBD-18 = B, HLD 4.4 | `MergeTables.SWIPE` 삭제, `ComposePolicy.SWIPE` = (`NONE`, `mergeConsecutiveVowel` false). 받침·겹받침·도깨비불·자모 단위 ⌫ 그대로 | LLD-03 1·3.1, LLD-04 7.1·7.2 |
| SWIPE 5.2 ⌫ 예 | 과(한 획 ㅘ) ⌫ = ㄱ, 기준 키 ㄱ(모음 이벤트 하나 = 입력 단위 하나) | LLD-04 6절, SM-40b |
| FR-TUT-02, SWIPE 10장 3-3 삭제 | `STEP_3_3`·`tutor_prompt_3_3` 삭제, 3과 설명의 나눠 쓰기 문장을 한 획 안내로 | LLD-07 2.2, LLD-03 2.2 |
| FR-TUT-12, HLD 7.1, SWIPE 10.1 | `TutorItemJudge`(동작 기록·판정), `HowTo`(문구·항목), `TutorStep.itemSpec`, `TutorSession` 시도 표본·✗이면 같은 항목, `TutorFlow` ✓·✗·안내·덧붙임·누름에서 비움·오류 진동, `RawStroke.key` | LLD-07 2.1·2.3·2.5 |
| TBD-17 = A | ✓ 뒤 `echoChar` = 항목 `glyph` | LLD-07 2.5 |
| SWIPE 10.1 "이상값 처리는 LLD" | D-51 | LLD-07 3절 |
| SWIPE 10.1 "표시 시간, 색, 배치는 LLD" | D-50 | LLD-07 2.5 |
| 시험 | HC-03·04 삭제, HC-21·22·BS-20·SM-14·14b·16·40·40b·42 바뀜/신설, TJ-01~06, TU-40~51, TU-01·17·20·30 바뀜 | LLD-11 |

- **설계와 다른 처리(PM 승인 #158):** HLD 7.1은 personal의 동작 기록을 `SwipeModule`이 만든다고 하나, LLD는 main이 `RawStroke` + stroke-core로 만든다(LLD-07 2.5). 결과는 같다(같은 `EdgeCrossingTracker`·같은 `minCrossDp`, 시도 안 기준 키 = 마지막 탭의 키). PM이 이 방식을 승인했고 HLD 문구는 설계팀이 나중에 맞춘다.
- 코드는 같은 PR에 있다.

### 6.30 가운데 자리에 조합 중인 글자, C를 반 키 높이 아래로(0.35) — FSS#36, FSS#37

오너 요청·결정(2026-11-02): FSS#36 "선을 그으면 그 글자를 보여 주는 것이 어떨까? … 지금 쓴 것을 사람이 인지할 수도 있고", 확인 ㄱ + ㅏ + ㄱ + ㅏ = ㄱ → 가 → 각 → 가. 최종 결정: **후보 표시 없이 글자 하나만**("위치를 보면 그냥 알 수 있는 것 아닌가", 앞서 정한 괄호 "그(ㄱㆍ)"·점 후보를 대신함). 보충·확인: 아직 모음이 아닌 점 상태는 점을 획 수만큼 그 자리에 — 고·교는 자음 아래에 수평으로(··), 거·겨는 자음 오른쪽에 수직으로(:). FSS#37 "자음을 찍는 영역을 조금 아래로 내려 줘. 반 키 높이만큼 내려 줘." 근거 판 SWIPE 0.52(9.5·3.6.5)·SRS 0.72(FR-DSP-01·04·10, TBD-24)·HLD 0.65 6장·REQUIREMENTS 0.24(설계 PR #38과 이 PR의 설계 문서 고침). 구현과 같은 PR.

| 근거 | 0.35 처리 | 반영 위치 |
|---|---|---|
| SWIPE 9.5 자리와 크기, FSS#37 | `consonantBox` 위 끝 = `(1 + C_DROP_ROWS) × 줄 높이 + 4dp`, `C_DROP_ROWS = 0.5`(줄 높이를 따라감). 그림·판정이 같은 함수라 판정 C도 함께 내려감 | LLD-04 0.32 8.1 |
| SWIPE 9.5 그리는 글자, FR-DSP-10 | 지금 조합 중인 한 글자(도깨비불 뒤 지금 글자만, 낱모음 없음, 후보 없음) = `InputDispatcher.centerText()` → `SwipeHost.centerText` → `CenterConsonantPainter` | LLD-04 9.5, LLD-08 6.1, LLD-03 2.2, LLD-07 2.3 |
| SWIPE 9.5 점 상태 | 조립기 `pendingDots`(남은 획마다 세로 = 아래, 가로 = 오른쪽), 글자 칸을 위·왼쪽 0.7로 줄이고 점을 ㅛ·ㅕ 배열로 찍음. 편집기의 ㆍ·‥는 그대로, 천지인 ㆍ·‥는 글자 | LLD-04 6.3·9.5, LLD-08 6.1 |
| SWIPE 9.5 바뀌는 때 | 조합기·매퍼는 뗄 때·기능키·초기화에서만 바뀌고 뷰는 그릴 때 읽기만 하므로 긋는 동안·무시된 획에서는 그대로(새 타이머·상태 없음) | LLD-04 9.5 |
| SRS FR-DSP-01·04, SWIPE 9.1 | 다음 획 자리 표시 삭제. 코드는 0.18부터 그리지 않았으므로 바뀜 없음 | LLD-06 0.24, LLD-04 5.2 |
| SRS FR-DSP-03 | 현재 글자는 모음만 그대로(바뀜 없음) | LLD-06 |
| SWIPE 3.6.5 (가) | 시험 C = (139, 76)–(221, 172), 사례 y + 24(결과 그대로). C를 쓰는 다른 시험 획도 y + 24 | LLD-11 0.39 |
| 시험 | VS-15, CS-01 새 뜻·CS-04, SW-14b, CT-01~CT-06, DV-29 | LLD-11 0.39 |

- **확인할 것(실기기, DV-29):** 점의 크기·간격과 글자 칸 0.7(글자가 너무 작아 보이지 않는지), 줄 높이 64dp에서 마지막 ㅣ의 오른쪽 여유(SWIPE 9.5 공간 점검). 수치는 상수 한곳(`CenterConsonantPainter.TEXT_SHARE`·`DOT_*`, `SwipeLayout.C_*`).

### 6.29 C 기준 두 구역, 첫 획 두 후보(0.29) — 이슈 #217

- 오너 결정(2026-11-02): "자음 기준으로 아래 왼쪽, 오른쪽 위 이 두 가지만 살펴보면 쉽게 구분이 될 건데", "처음 입력이 모호해도 두 번째 획을 입력받으면 확정되잖아." 설계 PR #231(SWIPE 0.43 3.6.3)의 개발자 몫을 `VowelStrokeAssembler`에 옮겼다(LLD-04 6.3 0.26).
- 첫 획은 C가 있어도 낱모음 상태 `V1`/`H1` 하나로 합쳤다(C 전용 상태 넷 삭제). C의 구역은 보임·ㅏ / ㅗ 동점·한 획 닫기와 둘째 이후 세로 획의 역할에만 쓴다. ㅓ / ㅢ는 늘 3등분 + 0.5.
- 0.28·PR #230(닫음)의 ①~④ 규칙은 모두 대체됐다. 실기기 사례(노 점 d 38.8, 뚜 ㅡ d 32.1, 긴 ㅣ d −57.7·−81.7)는 시험으로 고정했다.
- `FSSStroke` 줄: 역할 이유는 "first vertical under-left: shows a dot, both roles kept (P (x,y), d …)" 꼴이고, C 뒤의 `b=`는 뺐다.

### 6.28 C에 겹친 모음 획, 역할은 획의 가운데(0.28) — 이슈 #217

- 오너 결정(2026-11-02, 실기기 "노"의 점 (362,133)→(358,168)이 T < b라 무시되던 기록): "자음 칸에 겹칠 수 있지, 겹치면 안 되는 이유가 있어?" 설계 PR #226(SWIPE 0.42 3.6.3)의 역할 표 ①~④를 `VowelStrokeAssembler.vRole`·`hRole`에 그대로 옮겼다(LLD-04 6.3).
- 영향: ㄱ 키(C 아래쪽 절반 높이)에서 시작한 ↓는 짧으면 ㅣ(③), 길면 세로 점(②)이라 획 안내의 ↓ 힌트가 둘이 된다(PS-01b). C 위쪽 절반에서 시작한 ㅣ는 길이와 상관없이 ㅣ(①). 설계자가 적은 위험(C 가운데쯤에서 시작해 길게 내린 ㅣ가 점이 됨)은 실기기 `FSSStroke`로 지켜본다.

### 6.27 탭/획 최소 거리 8dp 고정(0.27) — 이슈 #217

- 오너 결정(이슈 #217, 2026-11-02): "길이와 상관없이 자음 아래 점이 두 개여도 인식해야 한다", "물론 점(탭)보다는 길어야겠지만". 탭과 획을 가르는 `minCrossDp`를 8dp(안드로이드 touch slop 수준)로 고정한다.
- 코드: `ThresholdDefaults.MIN_CROSS_DP` = 8. `ThresholdCodec.decode`는 `minCrossDp`를 읽지 않는다(없어도·범위 밖이어도 OK, 쓰기는 8을 그대로 적어 옛 판이 읽을 수 있음). `TutorSession.finishStep`은 `MIN_CROSS` 규칙의 `NotEnough`만 쓰고 값은 바꾸지 않으며 탭 ↔ 점 겹침으로 1-2를 다시 하지 않는다(`calibration.tapDot` 기록은 그대로).
- 같은 값을 쓰는 곳(숫자·기호·천지인 키 변 통과, 탭 자판 ↑↓, 그래피티 모드 키)도 8dp가 된다. 시험 EC-03~05·SC-01·02의 경계값을 8로 옮겼다(LLD-11 0.28).
- 설계 문서(SRS 1.3 기준값 표·FR-TUT-02·03·13, HLD 7.1·7.2, SWIPE 3.1·10장)는 설계자가 `feature/217-mincross-srs`에서 고친다. 이 판은 그 결정에 맞춘 임시 근거로 이슈 #217 코멘트를 쓴다.

### 6.26 가운데 자음 자리 C를 가로 가운데로(0.26) — 이슈 #213

오너 결정(2026-11-02, 실기기 사용 뒤): "자음 글자를 ㅃ이 아니라 우측 ㄸ에 두었으면 하는데, 중앙 정렬을 해야지". 근거 판 SWIPE 0.40 9.5·3.6.5 (가), SRS 0.55, HLD 0.50(설계 PR #216). 구현과 같은 PR(지시서 없음).

| 근거 | 0.26 처리 | 반영 위치 |
|---|---|---|
| SWIPE 9.5 자리와 크기 | `SwipeLayout.consonantBox` 왼쪽 끝 = 폭 / 2 − 너비 / 2(가로 중심 = 자판 가로 중심), 위 끝·높이·너비 그대로 | LLD-04 0.24 8.1 |
| SWIPE 3.6.3 C 기준 판정 | 바뀜 없음(C에 대한 상대 조건이라 C와 함께 옮겨짐) | LLD-04 6.3 |
| SWIPE 3.6.5 (가) | 시험 C = (139, 52)–(221, 148), 사례 x + 77, C 왼쪽 사례 = ㅃ 키 (80,56) | LLD-11 2.3 |
| SWIPE 10장 1과 설명 | 튜터 1과 문구 "자판 왼쪽 위에" → "자판 위쪽 가운데에"(`tutor_lesson_1_intro`) | 화면 문구 |
| 시험 | SL-08, VS-*, C 옆 시험 획은 좌표만 옮김(기대값 그대로) | LLD-11 0.27 |

### 6.25 그래피티 획 안내표(0.25) — 이슈 #189

오너 요청·결정(이슈 #189): 획 안내표 팁, OD-2(기호 넣음, TBD-26), 정정 "tip은 이미지처럼 입력기 위에 불투명으로 보여줘. 참고하고 닫으면 그냥 사라지게."(TBD-25), 보충 "영문과 한글 숫자를 한번에 표시해도 돼. 참고니까." 근거 판 SRS 0.45 FR-GRF-09~09d, HLD 0.41 5장·5A, GRAFFITI 0.16 6.6(설계 PR #190). 구현과 같은 PR(지시서 없음, 0.21까지와 같음).

| 근거 | 0.25 처리 | 반영 위치 |
|---|---|---|
| FR-GRF-09b, HLD 5장 "런타임에 읽는 방법은 LLD-05" | 행 ↔ 템플릿 id·획 번호 목록은 recognizer 코드의 표 하나(`GuideCatalog.groups`, 157칸 = 한글 20 + 모음 입력 순서 21 + 영문 35 + 숫자 12 + 제어 획 12 + 기호 57. 0.25 당시 162칸(한글 25), FSS#64에서 쌍자음 칸 5개를 뺌). 점 열은 실행 중 `TemplateSet`에서 꺼냄. 템플릿 JSON 형식 그대로 | LLD-05 10.1 |
| FR-GRF-09c 임시 표시 | 칸의 `interim`. `TEMPLATES.md` interim과 같은지 시험(GC-07) | LLD-05 10.1 |
| FR-GRF-09c 칸 = 한 행 | 변형 `ko_d2`·`ko_b2`·`en_q2`, 오너 획 `*_oN`과 한 행의 둘째 획은 칸이 아님. O·0 ①② = 한 템플릿의 획 0·1 | LLD-05 10.1 |
| FR-GRF-09b 모음 입력 순서 | 4.2 모음 표 21행을 글자 칸으로 | LLD-05 10.1 |
| GRAFFITI 6.6-7 "칸 크기·색·한 줄 칸 수·✕ 크기는 LLD" | 칸 최소 폭 64dp(360dp → 5열), 높이 84/44dp, ✕ 40dp, 배경 `0xFF161616` | LLD-05 10.2 |
| FR-GRF-09 "빈 띠 버튼은 누를 때 처리(세부는 LLD)" | `ACTION_DOWN`에서 피드백·토글. 버튼 표시 "?", 접근성 이름 "획 안내표"(상품명 쓰지 않음) | LLD-08 7.1 |
| HLD 5장 "열 때 진행 중 패드 포인터 버림" | 모드 뷰에 합성 `ACTION_CANCEL`을 바로 보냄 + 열거나 닫은 제스처의 나머지를 루트가 가로챔(`onInterceptTouchEvent`) | LLD-08 7.1 |
| FR-GRF-09 "뷰 재생성 시 닫힘(세부는 LLD)" | 열림 = 루트의 덮개 `VISIBLE`. 새 루트는 닫힌 채 | LLD-08 7.1 |
| FR-GRF-09 "키보드가 내려갈 때" | `onWindowHidden`, `onFinishInputView(finishingInput = false)`. `true`(입력창만 바뀜)는 열린 채 | LLD-08 3절 |
| 시험 | GC-01~GC-09, GD-01~GD-11, DV-28 | LLD-11 0.26 |

### 6.24 긋기 기호 키 단계 삭제·기호 키 셋(0.24) — 이슈 #208

오너 결정(2026-11-02): "나갔다 돌아오는 방식을 제거하려고 기호 키를 없애는 거야" → "기호 키 옆에 두 번째 기호 키를 두고 나갔다 들어오는 것을 배치하자", 같은 날 추가 지시 "기호3 키를 만들고 기호를 적당히 분배하자", 오너 확정(이슈 #208 마지막 코멘트: PR #210 배치 그대로, TBD-28 = A). 근거 판 SRS 0.53(FR-SWP-42·43·43a·44·50, TBD-28), SWIPE 0.38 7·8장, HLD 0.48(설계 PR #210). 구현과 같은 PR(#211).

| 근거 | 0.24 처리 | 반영 위치 |
|---|---|---|
| FR-SWP-43 | 단계(`Stage`) 삭제, 첫 나감 방향만(돌아옴·문지르기 = 나감), ↓ 배정 허용, 배정 없는 방향 = 탭(진동 없음) | LLD-04 4.4·4.5 |
| FR-SWP-43, 50 | 기호3(줄 4 열 0) = 탭 `/` → `=` ↑ `?`, 기호(줄 4 열 5) = 탭 `.` ← `(` → `)` ↑ `>` ↓ `-`, 기호2(줄 4 열 6) = 탭 `,` ← `"` ↑ `<` ↓ `'`(0.24 당시 배정, 지금은 FSS#75로 LLD-04 8.3). 빈 칸은 맨 아랫줄 오른쪽 하나 | LLD-04 8.1·8.3 |
| FR-SWP-42, SWIPE 7장 | 숫자 2 = ← `[` → `]`, 6 ← 배정 없음. 9·0 ↑ `( )` 그대로(그때 TBD-28 A, FSS#75로 `( )`는 9·0 ↑에만) | LLD-04 8.2 |
| FR-SWP-43a | 라벨 = 가운데 탭 기호, 모서리에 배정 기호 | LLD-04 8.3·9절 |
| SWIPE 9.2 | 탐침 STAGE2·STAGE3 삭제 | LLD-04 5.2 |
| 시험 | NK-02·06·15, SK-01~SK-08, SL-01·09·10, PS-14·18 | LLD-11 0.25 |

### 6.23 모드 키 ↑ = 설정, 긋기 ⚙ 키·한 글자 완성 삭제(0.23) — 이슈 #205

오너 결정(2026-11-02): "모든 모드에서 모드 키를 위로 올리면 설정이 나오게 해줘. 만약 설정 키가 있다면 제거해", ⚙의 한 글자 완성에 대해 "완성 기능도 없앰". 근거 판 SRS 0.52(FR-CMN-11a ①·FR-CMN-13·FR-SWP-50·66), HLD 0.47(8.2 `ModeKeyGesture`), SWIPE 0.37(설계 PR #207). 구현과 같은 PR.

| 근거 | 0.23 처리 | 반영 위치 |
|---|---|---|
| FR-CMN-13, HLD 8.2 | stroke-core `ModeKeyGesture.of(TapGesture)`: 처음 통과한 변이 위면 `OPEN_SETTINGS`(되돌아옴·문지르기도), 그 밖 `MODE_KEY_TAP`. 탭·긋기·그래피티 세 뷰가 같은 `TapGestureClassifier`(`minCrossDp`)로 모드 키를 따라가 뗄 때 판정 | LLD-05A 4절(0.12), LLD-04 9.3(0.22), LLD-05 8절(0.22) |
| FR-CMN-11a ① | 모드 키만 뗄 때 처리(누를 때는 진동·소리만), 떼기 전 취소는 입력 없음. 탭 자판에서는 대기열 밖 | LLD-05A 4절, LLD-08 9.1 |
| FR-CMN-13 설정 열기 | `TapSink.onSettingsStroke()`·`SwipeHost.onSettingsStroke()` → `InputDispatcher.onSettingsStroke()`: `finishAll()`(조합 확정) → `resetParsers(MODE_CHANGED)`(조립 닫기, 버린 획 진동) → `onOpenSettings()`. 모드·`typed`·저장 모드 그대로 | LLD-08 6.1(0.23), LLD-03(0.23) |
| FR-SWP-66 삭제, FR-SWP-50 | 긋기 ⚙ 키 → 빈 칸 `empty_r4c0`(터치만, 입력 없음). `FunctionKey.SETTINGS`, `onSettingsKey()`(조합 중 한 글자 완성), `a11y_key_settings` 삭제. `a11y_key_mode` = "입력 모드 전환, 위로 밀면 설정" | LLD-04 8.1·9.6, LLD-03 3.5 |
| 시험 | MK-00~MK-08 새로, ID-27 대체, SL-01·KH-06 바뀜 | LLD-11 5.5(0.24) |

### 6.22 탭 자판 배치: 기호 키 삭제·편집 줄 Esc·Del(0.22) — 이슈 #198

오너 결정(2026-11-02, 실기기 사용 후)과 같은 날 추가 지시 두 번(Esc·Del, `,`·`.`·`/` 세 키와 넓힌 스페이스)을 이슈 #198 본문·코멘트 그대로 옮겼다. SRS 3.3 그림·배정표·⌫ 위치 문구는 설계팀이 고친다(SRS 0.49). 구현과 같은 PR이다(지시서 없음).

| 근거 | 0.22 처리 | 반영 위치 |
|---|---|---|
| #198 1절 기호 배정, 마지막 코멘트 | 기호 키(`quote`) 삭제, `,` ← `'` → `"`, 새 `/` 키(탭 `/` ↑ `?`), `.`은 탭 `.` ↑ `>`만. `-`의 →는 배정 없음 | LLD-05A 3.3, LLD-11 TK·TA, TL-05 |
| #198 추가 지시 편집 줄 | Esc · ⇥ · ← · ↑ · ↓ · → · Del · ⌫ 각 45dp(NFR-06 예외), `FunctionKey.ESC`·`DELETE`, `EditorKey.ESCAPE`·`FORWARD_DEL`, 조합 중이면 방향키처럼 먼저 확정. Del은 ⌫처럼 반복(PM 권장안) | LLD-05A 3.2·4·6절, LLD-03 3.5, LLD-08 4·6.1, LLD-11 TL-02·ID-35~37 |
| #198 추가 지시(i·o 괄호) | i ← `[` → `]`, o ← `{` → `}`, ↓ 배정 없음. 글자 키 `assign`이 `right`도 받음 | LLD-05A 3.3(0.11), LLD-11 TK-07·08·10·11·19·20·22·23, TA-05·06(0.23) |
| #198 배치 | 넷째 줄 빈칸 18 · z~m · `-` 36 · ↵ 54, 맨 아랫줄 한/영 72 · 스페이스 180 · `,` · `.` · `/` 각 36(빈칸 없음, 0.23 추가 지시. 그 앞 판은 144 · 48) | LLD-05A 3.2, LLD-11 KH-03b·KH-05 |

- **확인할 것(오너):** Del 누르고 있기 반복은 PM 권장안으로 넣었다(진동·소리는 누를 때 한 번). 오너가 다르게 정하면 `TapTouchLogic.REPEATING`에서 뺀다.

### 6.21 길이 판정 폐지·자음 기준 C·가운데 자음(0.21) — 이슈 #179, #180 후속

오너 결정(#179: "이게 길이로 인식하는 것은 폐지해야 해.", 붙음 안 봄, 가로지름, ㅏ / ㅗ 상대 크기, TBD-22 = 0.5, TBD-24 = 화면에 그려진 자음 C 기준, 학습 금지 = 표시는 평소대로)와 오너 요청(#180 후속: 자음을 흐리게 보이고 그 옆에 모음, 조합 중 설정 키 = 한 글자 완성)을 설계팀이 SWIPE 0.35·SRS 0.44·HLD 0.40·REQUIREMENTS 0.12에 넣었다(PR #184). LLD는 그 판에 맞췄다.

| 근거 | 0.21 처리 | 반영 위치 |
|---|---|---|
| SWIPE 3.1·3.6.1(길이 폐지), HLD 4.2 | `VowelStroke.long`·분류기 ④ 삭제. `span`은 상대 크기 두 곳에만 | LLD-04 4.3, 6.3 |
| SWIPE 3.6.2 다음 후보 표(정본), 3.6.3 C 역할 표·앞 획 구역, 3.6.4 | 조립기를 후보 표의 상태 기계로 다시 씀(상태 27개). C가 있으면 역할을 먼저, 낱모음은 상대 크기(같으면 ㅏ). 쪽·높이만(닿음·거리 상한·길이 비례 여유 D-58 삭제). D-62 개정 | LLD-04 6.3 |
| SWIPE 9.5 자리와 크기, HLD 4.2 입력 C·6장 | `ConsonantBox`, `SwipeLayout.consonantBox`(그림·판정 같은 함수), `SwipeInputMapper.consonant`, 터치 로직·연습 입력 표시가 획마다 넣음, 조립기는 첫 획 때 고정 | LLD-04 6.1, 8.1, 9.3, LLD-07 2.3 |
| SWIPE 9.5 가운데 자음, FR-DSP-10 | `CenterConsonantPainter`(조합기 `lastConsonant()`를 C에, 흰색 40%), `SwipeHost.centerConsonant`, 튜터 `PracticeEcho.center`. 0.20의 가운데 음절 삭제 | LLD-04 9.5, LLD-03 2.2, LLD-07 2.3 |
| SWIPE 9.5 조합 중 키 표, FR-SWP-66 | 조합 중 설정 키 = 한 글자 완성(설정 화면 안 엶), 스페이스·천지인 키는 그대로 | LLD-04 9.6, LLD-08 6.1 |
| SRS FR-CMN-08 ①/②, FR-GRF-07, FR-DSP-08·10 | `FieldInfo.hidesDisplay`(비밀번호)·`noRecord`(비밀번호·학습 금지). 현재 글자·가운데 자음·그래피티 인식 글자는 비밀번호에서만 끔. 오류 기록은 둘 다 끔. DCR-19 결과 바뀜 | LLD-08 2·3.1절, LLD-06 2절, LLD-05 6·8.3절, LLD-07 6.1·6.2 |
| SWIPE 10.1 쓰는 법 문구, SWIPE 10 1과 설명 | "길게·짧게" 삭제, 자음 기준 첫 획 위치 말, 3-1 판정을 연습 자판의 C로. 문구(strings) | LLD-07 2.5, D-59 개정 |
| SWIPE 3.6.5 (가)·(나) | VS-14(가) 25행, VS-02(나) 58행을 표 그대로 단위 시험으로. 고치기 전 코드에서 두 시험 모두 실패함을 확인(11-1) | LLD-11 2.3 |

- **설계질의 #192(답함, 설계 PR #193 = SWIPE 0.36, 머지 전):** ① C가 있을 때 ㅜ ② "아래"의 시작 높이 조건 없음 — 0.21 첫 판의 임시 처리와 같음(D-63, 근거만 바꿈). ② 낱모음 ‥ 상태는 "‥"(D-64 개정, 0.21 2회차). PR #193이 머지되면 근거 판을 SWIPE 0.36으로 올린다.
- **남은 일(이 판 밖):** ① HLD 0.40 7.1·7.2 형식 6(`longDp`·`dotLong` 삭제, 1-2 점 ↔ 긴 획 경계 재지 않음, `tapDot` → `tapStroke`)과 1-2 점 ↔ 긴 획 겹침 다시 하기 — 측정·저장 형식을 바꾸는 일이라 **이슈 #194**로 따로 한다(지금도 판정에는 `longDp`를 쓰지 않음). 그 안내 문구 `tutor_overlap_length`는 0.21 2회차에서 길이 말 없이 "흐린 자음을 기준으로 자리를 맞춰 다시 써 보세요"로 바꿨다. ②의 안내 말풍선 겹침(같은 방향 점·긴 획 탐침이 같은 글자)은 0.21 2회차에서 (방향, 글자) 중복 제거로 고쳤다(LLD-04 5.2). ② SWIPE 9.1 획 안내의 "다음 획 자리 표시"와 위치 기반 탐침(LLD-06) — 지금 안내는 0.18의 ↓·→ 점/긴 획 탐침에 미리보기 글자를 붙이는 방식 그대로다. ③ 넓은 화면 3구역 배치의 C(SWIPE 2.2, 미정).
- **확인할 것:** 실기기에서 C 자리(세로 화면 약 (64,52)–(146,148), 0.26부터 가로 가운데 (139,52)–(221,148), 6.26, 0.35부터 반 키 높이 아래 (139,76)–(221,172), 6.30)와 기준선 b = C.B − t/2가 손에 맞는지, C 위를 지나는 ㅣ를 무시하는 것이 거슬리지 않는지. 수치는 설계 기본값(상수 한곳).

### 6.20 ㅓ/ㅢ 높이 3등분·가운데 음절(0.20) — 이슈 #179, #180

오너 결정(#179: 가로 획이 ㅣ의 위·가운데 1/3에 붙으면 ㅓ, 아래 1/3이면 ㅢ), PM 결정(ㅣ 아래 끝 조건 삭제, 닿음 ±t/4, 짧은 ㅣ도 같음), 오너 요청(#180: 쓰는 음절을 자판 가운데에 크고 흐리게)을 설계팀이 SWIPE 0.33~0.34·SRS 0.42~0.43·HLD 0.38~0.39에 넣었다(PR #181, #182). LLD는 그 판에 맞췄다.

| 근거 | 0.20 처리 | 반영 위치 |
|---|---|---|
| SWIPE 3.6 "ㅓ / ㅢ는 높이로 가른다"·"같은 원리의 점검", FR-SWP-19 | 가로 획 뒤 오른쪽 끝 ㅣ: 자리 하나 + 닿음 ±t/4, r = (y − T)/(B − T) < 2/3 = ㅓ, ≥ 2/3 = ㅢ. ㅔ②는 r 안 봄(③으로 ㅔ). 0.19의 "ㅓ/ㅢ 동점은 길이" 삭제, 길이는 혼자 있는 첫 획에만. D-62 | LLD-04 6.3 |
| SWIPE 3.6 시험 사례(0.34, 17개) | VS-02 고침, VS-12(1dp 경계), VS-13(완성 모음 하나). 표준 ㅢ 모양(시험 도구 `SwipeTypist`, 튜터 시험)은 ㅡ가 ㅣ 아래 1/3에 오게 고침 | LLD-11 2.3 |
| SWIPE 9.5, FR-DSP-10, FR-CMN-08, HLD 6장 | `CenterSyllablePainter`(3줄 높이, 흰색 40%, 키 < 가운데 음절 < 획 자국), `SwipeHost.composingText`, 제한 입력창에서 끔, 안내 설정 무관, ⌫ 되풀이·앱 초기화에서 다시 그림, 튜터 `PracticeEcho.composing` | LLD-04 9.5, LLD-07 2.3 |

- **확인할 것:** 3등분 경계(2/3)와 닿음 여유(t/4)는 설계 기본값(상수 한곳). 실기기에서 ㅓ를 ㅣ 아래쪽에 붙여 쓰는 사용자가 많으면 PM이 조정.

### 6.19 손글씨 모음 획·위치 관계·획 자국(0.19) — 이슈 #173

오너 결정(2026-11-01: "자음은 탭, 모음은 손글씨 쓰는 방법, 종성은 다시 탭", "거리 말고 위치가 중요해", 획 자국)과 PM 결정을 설계팀이 SWIPE 0.30~0.32·SRS 0.39~0.41·HLD 0.35~0.37·REQUIREMENTS 0.10~0.11에 넣었다(PR #174, #175, #176). LLD는 그 판에 맞췄다.

| 근거 | 0.19 처리 | 반영 위치 |
|---|---|---|
| SWIPE 3.1·3.2, HLD 4.2 | 한글 자음 키 = 변위로 ① 탭/획 ② 축 ③ 방향(반대 = Reverse) ④ 길이. 변 통과 모음 규칙·상태·탐침 삭제 | LLD-04 4.2·4.3·5.2 |
| SWIPE 3.3~3.6, HLD 4.3 | core-hangul `VowelStrokeAssembler`: 획 열, 21모음 역할 표, 위치 관계, 길이는 첫 획·동점에만, 첫 획 다시 정하기, 임시 ㆍ·‥, ⌫ 획 하나·닫힌 뒤 단계 하나, 닫기 = 버림 알림. D-58 | LLD-04 6.3 |
| HLD 4.3, SWIPE 5.4 | 매퍼: 탭 → 자음(쌍자음 키 = 일반), 획 → 조립기, 천지인과 섞지 않음, `errorVibe`, `reset(): Boolean`, `StrokePop` | LLD-04 6절, LLD-03 2.2, LLD-08 6절, D-57 |
| SWIPE 2.1, FR-SWP-66 | 양 끝 열 한 칸 올림, 설정 키 ⚙(`FunctionKey.SETTINGS`, 누를 때 확정·닫기 후 설정 화면) | LLD-04 8.1, LLD-08 6.1 |
| SWIPE 9.1·9.2 | 획 안내(`DOT`·`LONG` 탐침 + 조립기 미리보기), 연습 자판 안내에 미리보기 | LLD-04 5.2, LLD-06 |
| SWIPE 9.4, FR-DSP-09 | `StrokeTrail`·`TrailPainter`(4dp, 40%), 열린 획 수에 맞춤 | LLD-04 9.4 |
| SWIPE 10·10.1, FR-TUT-02·03·12·13, HLD 7.1·7.2 | 1-2 세 분포(탭·점·긴 획) → minCrossDp·longDp, 겹침 두 바퀴까지, `calibration`(위치 포함), 1-2 축·방향 판정(D-60), 3-1 결과 판정, 3-2 삭제, 안내 문구(D-59)·덧붙임, D-61 | LLD-07 2.1~2.5·3절 |
| HLD 7.2 형식 5 | `posTolDp`, `calibration`, 형식 1~4의 `longDp`는 기본값 | LLD-07 4·5절, LLD-03 3.4 |

- **코드 후속(이 판에서 하지 않음):** SRS FR-DSP-01 "다음 획을 시작할 자리를 작은 표시로"(SWIPE 9.1 설계 권장)의 자리 표시는 그리지 않았다. 획 안내의 결과 글자는 보인다. (0.35: 이 표시는 요구에서 빠졌다 — 가운데 글자가 대신함, FSS#36, 6.30.)
- **확인할 것:** 위치 허용 오차·배수는 설계 기본값(상수 한곳). 실기기·튜터 기록(calibration.position)으로 PM이 조정.

### 6.18 긋기 = 자음 + 모음·쌍자음 키·튜터 2과 삭제(0.18) — 이슈 #168

오너 결정(2026-10-30, 이슈 #168: "쌍자음을 한 키로 만들어 입력받고 키를 두 번 두드리는 것을 제거하자", 연습 자음 돌림)을 설계팀이 SWIPE 0.29·SRS 0.38·HLD 0.34·REQUIREMENTS 0.9에 넣었다(PR #169). LLD는 그 판에 맞췄다.

| 근거 | 0.18 처리 | 반영 위치 |
|---|---|---|
| SWIPE 2.1 배치 안 A, FR-SWP-50·51 | `SwipeLayout` 7×6, 줄 1 = (빈) ㅃ ㅉ ㄸ ㄲ ㅆ (빈), 천지인 키 줄 4, 창을 채움(빈 띠 없음), `SWIPE_ROWS` 6 | LLD-04 8.1, LLD-08 7절, LLD-03 3.3 |
| FR-SWP-30·34·36, HLD 4.3 | `SwipeInputMapper` = 탭 → `[Consonant]`, 긋기 → `[Consonant, Vowel]`, 상태 = 천지인 조립기뿐. 기준 키·쌍자음 후보·`onBackspace`·`anchorKeyId`·`keyIdOf` 삭제 | LLD-04 6절, LLD-03 2.2, LLD-08 6절, LLD-09 |
| FR-DSP-01·03, SWIPE 9장 | 안내 글자 = 그 키의 음절(`StrokeRules.syllable`), 모든 자음 키에서 안내 | LLD-04 5.2·9.3, LLD-06 |
| HLD 7.2 형식 4 | `doubleTapWindowMs`·`DOUBLE_TAP_WINDOW` 삭제, 형식 1~3 읽기(뺀 칸 버림) | LLD-03 3.4, LLD-07 4·5절 |
| FR-TUT-02·12, SWIPE 10장·10.1 | 2-1 삭제, 글자 항목 = 긋기 한 번(`Swipe(key, edges)`·`Pass(key, edges)`), 연습 자음 19개 돌림(짝 같은 자음, ✗ 뒤 같은 자음, 아·오·이 ㅇ, 1-1 14자음 PM 결정, 3-4 ㄱ), 오류 진동 = 무시된 천지인 요소, 화면 과 번호·`screenId`, 1과 설명 | LLD-07 2.1~2.5, D-48·D-49·D-56 |
| HLD 10장 R2(맨 윗줄 위 긋기) | 에뮬레이터 확인: 숫자 2 ↑ 창 밖 = @, ㄲ 키 ㅙ 창 밖 = 꽤. 투명 여백 줄 필요 없음 | LLD-04 8.1, LLD-08 7절, LLD-11 DV-26 |
| 시험 | 4.4 다시 씀, TJ·TU·TS·SL·SW·PG·ID 행 바뀜 | LLD-11 |

- **LLD가 정한 것:** D-56(돌림을 겹침 재시도에서도 ㄱ부터). SWIPE 10장에는 "단계가 바뀌면 ㄱ부터"만 있다.
- **남긴 것:** core-hangul의 `JamoEvent.DoubleConsonant`·`UndoLast`, `HangulComposer.lastConsonant()`는 긋기가 더 쓰지 않지만 공용 형식·조회라 남겼다(HC 시험 그대로). 지울지는 따로 정한다.
- 설계와 다른 처리 없음. 코드는 같은 PR에 있다.

### 6.17 튜터 5과 ⌫·틀린 입력 뒤 입력(0.17) — 이슈 #165

오너 베타 신고(R6 탭, 튜터 문장 단원): "치다가 오류가 나면 수정도 안 되고 이어서 다음 글자도 입력이 안 돼." PM 결정(5과 연습 자판에 실제와 같은 ⌫, 타수·정확도는 최종 입력 줄)을 설계팀이 SRS 0.37 FR-TUT-09·10, HLD 0.33 7.1, SWIPE 0.28 10장에 넣었다(PR #166).

| 근거 | 0.17 처리 | 반영 위치 |
|---|---|---|
| FR-TUT-09(5과 ⌫) | `PracticeKey.backspace`, `RawStroke.backspace`, `SwipePracticeEcho.backspace()`(실제 `InputDispatcher.backspace`와 같은 순서), D-55 | LLD-07 2.2 |
| FR-TUT-10(최종 줄, 시간 끝 = 마지막 동작) | 코드 변경 없음: [다음] 때 입력 줄로 계산, `sentenceStrokes`에 ⌫ 획 포함 | LLD-07 2.2·2.4 |
| SWIPE 10장 "틀린 입력 뒤 다음 자음 탭부터 정상" | 시험으로 확인(TU-55·TU-56·ID-34). 지금 develop에서 꼬임 재현 없음 | LLD-11 |

- **"다음 글자 안 들어감" 원인 조사:** 기준 키 밖 긋기, 모음 뒤 모음 긋기, 이어질 수 없는 천지인 요소 뒤의 자음 탭 → 긋기를 튜터 5-1(`TutorFlow` + `SwipePracticeEcho`)과 실제 키보드(`SwipeInputMapper` + `InputDispatcher`) 모두에서 시험했고 모두 정상이다. 상태를 비우는 경로(`TutorFlow.onPress`·`finishStep`·`nextSentence`의 새 echo, `TutorSession.onStroke`의 판정 비움, 매퍼 `reset`·`onBackspace`·조립 닫기)를 점검했고 남은 꼬임은 없다. 신고 증상은 이슈 #163(5-1에 앞 단계 ✓가 남아 획마다 입력 표시가 새로 만들어져 모음 긋기가 모두 무시됨, PR #164로 수정)과 같다. R6 빌드가 #164 앞이면 그것이다(미확인: R6 빌드의 커밋).
- 설계와 다른 처리 없음. 코드는 같은 PR에 있다.

### 6.16 긋기 천지인 키(0.16) — 이슈 #160

설계팀이 SWIPE 0.27 5.4절·SRS 0.36 FR-SWP-60~65·FR-HGL-04c·FR-CMN-07b·FR-TUT-02·HLD 0.32 3장·4.3·7.1·REQUIREMENTS 0.8 UR-B24에 오너 요청(넷째 줄 왼쪽 빈 칸에 천지인 키, 탭 ㆍ·위 ㅣ·오른쪽 ㅡ)과 PM 위임 사항의 설계 권장(↓ = ㅣ, 천지인 입력끼리만 합침, 임시 ㆍ·‥, ⌫ 요소 하나씩, 기준 키 예외, 무시 = 오류 진동, 튜터 3-4 한 항목)을 넣었다(PR #161). LLD는 그 판에 맞췄다.

| 근거 | 0.16 처리 | 반영 위치 |
|---|---|---|
| HLD 3장 의존 방향, 조립기 상태는 쓰는 쪽 | `CheonjiinAssembler` 등 → core-hangul `kbd.hangul.cheonjiin`. 그래피티 처리부와 `SwipeInputMapper`가 각자 가짐 | LLD-03 1·1.2, LLD-05 4절, tasks T12·T22 |
| FR-SWP-60·61, HLD 4.2 | `SwipeKeyKind.CHEONJIIN`, 줄 3 열 0, 라벨 "ㆍ ㅡ ㅣ", `classifyCheonjiin`(처음 변만, 왼쪽은 바깥 변), `StrokeOutcome.Element` | LLD-04 4.1·4.7·8.1 |
| FR-SWP-62·63, HLD 4.3 | 매퍼가 요소 조립: `Started` → `Vowel`, `Changed` → `ReplaceVowel`, 대기 → 빈 이벤트, 무시 → `Ignored`(진동). 기준 키 그대로, 후보 끊음, 다른 입력이면 닫음 | LLD-04 6절 |
| FR-SWP-64, FR-CMN-07b, HLD 4.3·4.4 | 임시 ㆍ·‥(`pendingGlyph`, 앱이 조합 글자 뒤에), ⌫ 조립기 먼저(`popElement`), **`ReplaceVowel` 자기 단위**(닫힌 뒤에도 요소 하나씩: 간 ⌫ = 가, ⌫ = 기) | LLD-04 6·7절, LLD-08 6.1, LLD-05 6절 |
| FR-SWP-65, FR-DSP-01 | 천지인 키는 늘 안내(`guided`, 연습 자판 `SwipePracticeGuide`) | LLD-04 9.3, LLD-06 4절 |
| FR-TUT-02, HLD 7.1·7.2, SWIPE 10.1 | 3-4 한 항목(결과로 판정: `Element`), 연습 키에 천지인 키, 입력 표시에 임시 글자 | LLD-07 2.2·2.3·2.5 |
| 시험 | CK-01~04, CJ-50~59, SW-10·11, ID-30~33, TJ-07, TU-52, HC-24·24c, SL-01·SW-01·TU-01 바뀜 | LLD-11 |

- **그래피티 동작 변화:** `ReplaceVowel`이 자기 단위가 되어 그래피티도 조립기가 닫힌 뒤의 ⌫가 요소 하나씩이다(간 ⌫ = 가, ⌫ = 기, ⌫ = ㄱ, 0.15까지는 가 ⌫ = ㄱ). SRS FR-SWP-64·SWIPE 5.4가 "그래피티와 같다"로 적은 것과 HLD 4.3 "그 요소는 조합기 입력 단위 목록에서 하나씩"을 따른 것이다. 조립기가 열려 있을 때의 결과(BS-06~BS-19, CJ 사례)는 그대로다.
- 설계와 다른 처리 없음. 코드는 같은 PR에 있다.

## 변경 이력

| 버전 | 날짜 | 내용 |
|---|---|---|
| 0.1 | 2026-10-27 | 최초 작성(작업지시서 #1 v2: HLD 절에 따라 파일 분리). 검수 1회차(PR #2) 반영: 그래피티 임시 글자가 확정·커서 이동 때 편집기에 남던 문제(`finishComposing`·`finishAfterUserMove`), `composeStart` 초기화, main의 gesture-engine 타입 참조 제거(`SwipeEngineState`), `FunctionKey`를 stroke-core로·탭 자판 자모를 `Char`로(모듈 의존 방향), 손 뗄 때 처리 전체를 `EngineGuard.stroke`로, `keypadActive` 조건, 빈칸 hit 순서, R5 범위, 긋기 ⌫ 반복 소유, D-12 → DCR-21, 시험 보강(TA-12, ST-01~03, CC-15~17, DV-18·19) |
| 0.2 | 2026-10-27 | 설계 0.15 판에 맞춤(작업지시서 이슈 #5 v1). DCR-01~21 결정 반영(6.1 표: 결정·설계 반영 위치·LLD 반영 위치, DCR-08·20 취소, DCR-10 대체), 결정 전 처리와 `Undefined`·`undefinedDcr` 삭제. 숫자 키패드 삭제(`Keypad`, `NUMPAD_*`, `keypadActive`, 숫자 전용 그래피티 패드, 시험 MC-14·GH-25·숫자 자판 열) 후 숫자류 입력창 진입·이탈(`FieldInfo.numericLike`, `ModeController.returnMode`·`enterNumericField`·`leaveNumericField`, 마지막 모드 저장 제외, 같은 칸 재표시). 튜터: 안내 삭제, 첫 실행 한 번 자동(`TutorLaunchPolicy`, `tutor_auto_started`), 한쪽 분포 공식(`oneSidedDistance`·`oneSidedWindow`). 처리 시점(`Feedback.key` 누를 때, `ACTION_CANCEL`·`onCancelAll`), 그래피티 시프트 규칙·인식 실패·한영 전환·인식 표시·보조 줄 폭, 진동·소리 기본값, 줄 높이 40~64dp. 시험: SRS 0.15 사례 표 ↔ 시험 ID 연결(탭 자판 동작 표 8행 × 3열로 TK 번호 다시 매김, 숫자류 입력창 사례 NF-01~09, 백스페이스 사례 24행 BS-23~25), SWIPE 5.1·5.2 대응표, 새 시험 SC-44~46, NK-15, SK-06, PS-15·16, SW-01~04, TG-09·10, MQ-07·08, FB-01~04, RT-05, CJ-41, GS-25~36, GH-26~35, FI-11·12, ID-21~24, TE-07~10, TU-07~12, SS-04·05, DV-20~22. 설계질의 #6(Q1~Q3) 열고 임시 처리. D-19 범위 축소, D-33·D-34 추가 |
| 0.3 | 2026-10-27 | 설계 0.16 판에 맞춤(작업지시서 이슈 #10 v1): 근거 판 SRS 0.16·HLD 0.12·GRAFFITI 0.9, 모든 파일 버전 0.3. 설계질의 #6(Q1~Q3) 답 반영 — 본문의 설계질의 임시 처리 메모를 확정으로 바꾸고 근거를 FR-CMN-11a ②, FR-GRF-05c ⑥⑦, HLD 5·8장, GRAFFITI 6.2-8·9로 옮김(6.2 "답함"). 동작 변화 없음. FR-GRF-05c ⑦(기호표에 없는 획 = 평소대로)이 템플릿 획에도 성립하도록 LLD-05 3.1 3번에서 확장·구두점 집합에 맞는 것이 없으면 평소 집합으로 다시 찾게 함(3.3 보충). 시험 GR-14 보충, 새 시험 GR-25·26, GS-37·38 |
| 0.4 | 2026-10-28 | 구현 중 찾은 LLD 빈틈 반영(작업지시서 이슈 #60 v1, 이슈 #13 G1~G13, 6.3): 근거 판 SWIPE 0.12, 모든 파일 버전 0.4. AGP 내장 Kotlin(LLD-03 1절). G1 Robolectric 승인 요청과 LLD-11 9절. G2 `InputSink.field`, G3 `TemplateSet.outputOf`·`GraffitiState(templates)`, G4 튜터 타입 필드, G5 `HangulComposer(onInvalid)`, G6 `GraffitiInputHandler.enterMode()`, G7~G9 조합기 잘못된 이벤트 처리, G10 BP-37·BP-40 크키·보비(설계질의 #55), G11 코너 세기·무게중심(D-35), G12·G13 `ComposingController` 다시 맞춤·구간 끝 모를 때 위치 유지(설계질의 #57, D-36, 시험 CC-18·CC-19, T18 코드 수정 포함). 6.2에 #55·#57 |
| 0.5 | 2026-10-28 | T27·T30에서 찾은 LLD 빈틈 반영(이슈 #75·#83, 6.4): 모든 파일 버전 0.5, 근거 판 그대로(SRS 0.16·HLD 0.12·SWIPE 0.12·GRAFFITI 0.9). #75 — `EngineGuard` 스냅숏 범위에서 `ComposingController` 뺌, D-32를 "편집기 쓰기 중 예외도 되돌림만"으로 바꿈(EG-05와 맞춤), `InputDispatcher.snapshot/restore`(LLD-08 6.1), `GraffitiInputHandler.snapshot/restore`(LLD-05 6절), `AndroidEditorPort` 반환값 규칙(LLD-08 4절), 시험 EG-05 예외 지점 명시·EG-06 추가. #83 — `GuideOverlay.onDown(key, rect, origin)`, DOWN에서 누른 상태 `ProgressState`를 안내에 한 번, flags가 바뀌면 overlay 다시 만듦(LLD-04 9.3, LLD-06 3절), 시험 SW-05. LLD-04 4.6 `StrokeClassifier(key, th, geom)`를 코드에 맞춤. 동작 변화 없음 |
| 0.6 | 2026-10-28 | T25 후속과 PR #91 문구 맞춤(이슈 #73·#92, 6.5): 모든 파일 버전 0.6, 근거 판 그대로(SRS 0.16·HLD 0.12·SWIPE 0.12·GRAFFITI 0.9). #73 — RUB 후보 표시 방향 = 첫 다리 방향(손가락이 안이면 e1 쪽), 고리 바깥 표시 거리(D-37), 표시 거리 기준점 = 손가락(LLD-04 5.2, LLD-06 3절), 시험 PS-17·PS-18. #92 — 포트 예외 때 `endBatch` 항상·임시 글자 바로 뺌(LLD-08 4절, LLD-09 2절 D-32), 시험 CC-20~CC-22. #88은 0.5·PR #91에서 이미 해결(닫음). 코드 후속 이슈 #96 |
| 0.7 | 2026-10-28 | 설계 0.20 판에 맞춤(이슈 #105·#103, 6.6): 근거 판 SRS 0.20·HLD 0.16·SWIPE 0.13·GRAFFITI 0.13, 모든 파일 버전 0.7. LLD-05 — 3.1에 "규칙 토큰을 대기 중 기호표에서 먼저 찾음" 단계(길이·영역 무관, CornerTap·LineLeft 제외, OD-3~OD-5 A), 되짚는 획 메모, 3.3 `ruleSymbol`·`Template.rule`, 5.2 행 2·3·10 설명, 6절 `onStroke` flash 조건(`glyph`, 무시 요소, PR #104), 7절 `rule` 칸·원본 획 표·미확인 처리(OD-1·2·6·7 A). LLD-08 7절 시스템 막대 여백·여백 색·창 높이(#93 OD-1~3 A). LLD-11 — 1.3 `shift` 칸, GR-27~GR-32, GS-30·31·34·38 고침, GS-39~GS-45, GH-36·GH-37, DV-07·DV-08 보충. 작업지시서 T38(#103) 추가 |
| 0.8 | 2026-10-28 | 설계 0.21 판에 맞춤(이슈 #121, 6.7): 근거 판 SRS 0.21·HLD 0.17·SWIPE 0.13·GRAFFITI 0.14, 모든 파일 버전 0.8. LLD-07 6.1·7절 `graffiti_letter_ratio` 선택지 1 : 1 / 2 : 1(기본) / 3 : 1(오너 결정 #108 A), 저장 형식(문자 영역의 몫 1/2·2/3·3/4, Float)과 목록 밖 저장값 처리(검사 없음, 코드와 같음). LLD-05 8.1 비율 선택지. LLD-04·05 머리 근거 판 갱신. Robolectric 승인 반영(LLD-03 4절, LLD-11 9절, 6.3 G1). 동작 변화 없음 |
| 0.9 | 2026-10-28 | 설계 0.22 판에 맞춤(이슈 #124, 6.8): 근거 판 SRS 0.22·HLD 0.18·SWIPE 0.14·GRAFFITI 0.14, 모든 파일 버전 0.9. LLD-07 2.1 `PracticeEcho`, 2.3 "입력: …" 줄(FR-TUT-09: 쿼티·긋기 해석 경로, 단계가 바뀌거나 다시 할 때 비움, 판정 기준값 = 시작 때 저장값), D-38(줄 20자), 시험 TU-13~TU-17. 측정·추정 동작 변화 없음 |
| 0.10 | 2026-10-28 | 설계 SRS 0.27·HLD 0.23·SWIPE 0.19에 맞춤(이슈 #127, 6.9): 근거 판 갱신, LLD-03·04·06·07·11 버전 0.10. 머물기 삭제, `BlockParser` 연속 입력·끊는 간격·받침 우선·들어오는 기준 간격(R5 B안)·누르고 있기·다시 나누기, `RunStart`와 조합기 run, 누르고 있기 분류·타이머, 기준값 형식 2, 튜터 과(설명 → 번갈아 연습, 쓰는 법 모두)·2-1/3-3 기준 간격·4-1·5-1·타수와 정확도. D-08 고침, D-39~D-44. 시험 BP·BB·RS·BH, HC-50~55, SC-47~50, GT-09, SW-06·07, TS-09, TU-18~23, TY-01~04, DV-03 바뀜·DV-23·24 |
| 0.11 | 2026-10-29 | 설계 SRS 0.29·HLD 0.25·SWIPE 0.20에 맞춤(이슈 #135, 설계 PR #138): LLD-06·07·11 버전 0.11. `Settings.showCurrentChar`(`show_current_char`, 기본 true)와 설정 화면 줄, `GuideFlags.of(settings, field)`(세 안내 모두 설정, 비밀번호·학습 금지는 현재 글자 끔), 튜터 연습 자판의 안내 `PracticeGuide`·`SwipePracticeGuide`(실제 키보드와 같은 `GuideOverlay`·`GuideTimerLogic`, 표시 전용). 시험 GT-10, SW-08, PG-01~PG-06, SS-01 기본값. 같은 판에 설계 SRS 0.28·HLD 0.24·GRAFFITI 0.15에 맞춤(이슈 #136, 6.10): 근거 판 갱신, LLD-05·11 버전 0.11. LLD-05 8.1 패드 상태 표시(구석 탭 영역 `cornerRect`의 점과 언어 글자 "한"/"A", 오른쪽 위 대기 표시 ⇪ ⇧ • ↘, 비밀번호 칸에서도 그림), 6절 `redraw`(`enterMode`·`reset`·`restore`). 시험 PI-01~PI-08, DV-25 |
| 0.12 | 2026-10-30 | 누르고 있기 = 쌍자음(FR-SWP-35) 삭제(이슈 #142 오너 결정, 6.11): LLD-03·04·06·11 버전 0.12. 엔진 `held`·`holdCapable`·`tick`, `StrokeResult`·`ProgressState`·`SwipeAction`의 `held`, `HOLD_LIFT`·`HOLD_SWIPE`, 안내 타이머 ② 삭제. 시험 SC-47~50 다시 씀, BH-05·GT-04·SW-07·PG-02·DV-03 바꿈, BP-15·19·45·BH-01~04·GT-05·09 삭제. LLD-07(튜터)은 #143·#144가 고침 |
| 0.13 | 2026-10-30 | 튜터 입력 표시(이슈 #143, PM 결정, 6.12): LLD-07·11 버전 0.13. 1~4과·public 1-1은 마지막 한 글자를 크게(`TutorFlow.echoChar`·`echoAsLine`·`lastChar`), 5과는 "입력: …" 줄 그대로. 측정·표본 불변. 시험 TU-17 바뀜, TU-27~TU-31. 같은 판에 1-1 목표 자음 14개·public q~p 한 번씩(이슈 #144), 2-1·4-1 누르고 있기 항목과 설명 문장 없앰(이슈 #142 튜터 부분), 6.13. 시험 TU-07·09·17·18 바뀜, TU-18b·TU-32·TU-33 |
| 0.14 | 2026-10-30 | 자음(탭)·모음(긋기) 분리(이슈 #150, 6.14): 근거 판 REQUIREMENTS 0.5·SRS 0.33·HLD 0.29·SWIPE 0.24, LLD-03·04·06·07·08·09·11 버전 0.14. `SwipeInputMapper`(기준 키, 짧은 두 번 탭 쌍자음, ⌫ 뒤 기준 키), 긋기 조합 = 두벌식 + 긋기 병합 표·자모 단위 ⌫, 낱모음 안내·기준 키에서만, 튜터 4과·3-3 측정·5-1 다시 계산 삭제·2-1 양쪽 경계·1-2 아·오·이, 기준값 형식 3. D-08·09·41·44 고침, D-39·40·43 삭제, D-45~D-49 |
| 0.15 | 2026-10-30 | 튜터 항목 판정(이슈 #154)·모든 모음 한 획(이슈 #156), 6.15: 근거 판 REQUIREMENTS 0.7·SRS 0.35·HLD 0.31·SWIPE 0.26, LLD-03·04·06·07·11 버전 0.15. `MergeTables.SWIPE` 삭제·`ComposePolicy.SWIPE` 병합 없음, 3-3 삭제, `TutorItemJudge`·`HowTo`(LLD-07 2.5), D-48 바뀜, D-50·D-51, 설계질의 #158 |
| 0.16 | 2026-10-30 | 긋기 천지인 키(이슈 #160), 6.16: 근거 판 REQUIREMENTS 0.8·SRS 0.36·HLD 0.32·SWIPE 0.27, LLD-03·04·05·06·07·08·09·11 버전 0.16. `CheonjiinAssembler` core-hangul로 이동, 천지인 키 판정·매퍼 조립·⌫·임시 글자, `ReplaceVowel` 자기 단위, 튜터 3-4, D-52~D-54 |
| 0.17 | 2026-10-30 | 튜터 5과 ⌫·틀린 입력 뒤 입력(이슈 #165), 6.17: 근거 판 SRS 0.37·HLD 0.33·SWIPE 0.28, LLD-07·11 버전 0.17. D-55 |
| 0.18 | 2026-10-31 | 긋기 = 자음 + 모음·쌍자음 키·튜터 2과 삭제(이슈 #168), 6.18: 근거 판 REQUIREMENTS 0.9·SRS 0.38·HLD 0.34·SWIPE 0.29, LLD-03·04·06·08·09 버전 0.17, LLD-07·11 버전 0.18. 매퍼 단순화, 자판 7×6, 기준값 형식 4, 연습 자음 돌림, D-45·D-47·D-52 삭제, D-48·D-49·D-51 바뀜, D-56 |
| 0.19 | 2026-11-01 | 손글씨 모음 획·위치 관계·획 자국·설정 키(이슈 #173), 6.19: 근거 판 REQUIREMENTS 0.11·SRS 0.41·HLD 0.37·SWIPE 0.32, LLD-03·04·06·08·09 버전 0.18, LLD-07·11 버전 0.19. D-57~D-61 |
| 0.20 | 2026-11-01 | ㅓ/ㅢ 높이 3등분(이슈 #179)·가운데 음절(이슈 #180), 6.20: 근거 판 SRS 0.43·HLD 0.39·SWIPE 0.34, LLD-04·07·11 버전 0.20. D-62 |
| 0.21 | 2026-11-01 | 길이 판정 폐지·자음 기준 C(이슈 #179)·가운데 자음과 조합 중 설정 키(이슈 #180 후속)·FR-CMN-08 ①/② 분리, 6.21: 근거 판 REQUIREMENTS 0.12·SRS 0.44·HLD 0.40·SWIPE 0.35, LLD-03·04·05·06·07·08·11 버전 0.21. D-58 삭제, D-59·D-62 개정, D-63·D-64(설계질의 #192 임시 처리), DCR-19 결과 바뀜 |
| 0.22 | 2026-11-02 | 탭 자판 배치(이슈 #198), 6.22: 기호 키 삭제·`,` ←→·`/` 키, 편집 줄 Esc·Del·⌫ 45dp, `-` 넷째 줄, ↵ 넷째 줄 끝, 스페이스 144. LLD-05A 0.10, LLD-03·08·11 0.22. 같은 날 i·o 괄호(←→): LLD-05A 0.11, LLD-11 0.23(맨 아랫줄 스페이스 180 · `,` `.` `/` 각 36 포함) |
| 0.23 | 2026-11-02 | 모드 키 ↑ = 설정·긋기 ⚙ 키와 한 글자 완성 삭제(이슈 #205), 6.23: 근거 판 SRS 0.52·HLD 0.47·SWIPE 0.37. `ModeKeyGesture`(stroke-core), 모드 키는 뗄 때 판정. LLD-05A 0.12, LLD-04·05 0.22, LLD-03·08 0.23, LLD-11 0.24 |
| 0.24 | 2026-11-02 | 긋기 기호 키 단계 삭제·기호 키 셋(기호3·기호·기호2)·숫자 2·6 재배치(이슈 #208, 오너 확정), 6.24: 근거 판 SRS 0.53·HLD 0.48·SWIPE 0.38. ↓ 배정, TBD-28 A. LLD-04 0.23, LLD-11 0.25 |
| 0.25 | 2026-11-02 | 그래피티 획 안내표(이슈 #189, 오너 결정 OD-2·정정·보충), 6.25: 근거 판 SRS 0.45·HLD 0.41·GRAFFITI 0.16. `GuideCatalog`(recognizer)·`GraffitiGuideLayout`·`GraffitiGuidePanel`(app), `KeyboardRootView` 버튼·덮개. LLD-05 0.23, LLD-08 0.24, LLD-11 0.26 |
| 0.26 | 2026-11-02 | 가운데 자음 자리 C를 가로 가운데로(이슈 #213, 오너 결정), 6.26: 근거 판 SRS 0.55·HLD 0.50·SWIPE 0.40. LLD-04 0.24, LLD-11 0.27 |
