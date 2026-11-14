# LLD-06 안내 표시 — 삭제 (FSS#95)

- 대응 HLD 절: 6장(안내 표시, HLD 0.78에서 삭제)
- 버전 **0.25** (2026-11-14)
- 0.25(FSS#95, SRS 0.85 3.5·HLD 0.78 6장): 안내 팝업(방향 안내·획 안내·현재 글자·다음 글자)은 긋기 자판에만 있던 표시라 긋기 입력 모드와 함께 없앴다. `GuideTimerLogic`·`GuideOverlay`(`kbd.app.swipe.impl`)와 그 시험(GT·GO), 표시 지연 기준값 `guideDelayMs`, 세 안내 설정(`showDirectionGuide`·`showNextChar`·`showCurrentChar`)이 함께 없어졌다. 0.24까지의 명세는 git 이력에 있다.

남은 표시의 명세는 다음에 있다.

| 표시 | 명세 |
|---|---|
| 탭 자판의 키 변 글자 표시 | [LLD-05A-tap-keyboard.md](LLD-05A-tap-keyboard.md) 3.4 |
| 그래피티의 인식 글자 표시·패드 상태 표시 | [LLD-05-graffiti.md](LLD-05-graffiti.md) 8절 |
| 그래피티 획 안내표 | [LLD-05-graffiti.md](LLD-05-graffiti.md) 10절 |
| 입력 연습 화면의 입력 표시·✓·✗ | [LLD-07-tutor-store.md](LLD-07-tutor-store.md) 2.3 |
