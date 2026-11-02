# FSS Keyboard

An Android keyboard (IME) written in Kotlin with four ways to type:

- **QWERTY**: an English tap keyboard
- **Dubeolsik**: the standard Korean two-set tap keyboard
- **Single-stroke handwriting pad**: draw one stroke per letter, for English and Korean
- **Swipe input**: tap a Korean consonant, then draw the vowel with short strokes

## Modules

| Module | Kind | Contents |
|---|---|---|
| `core-hangul` | Kotlin/JVM | Hangul composition: jamo, syllable assembly, composition policies, input events, vowel stroke and Cheonjiin vowel assembly |
| `stroke-core` | Kotlin/JVM | Shared touch geometry, tap keyboard layouts, tap/stroke judgement |
| `gesture-engine` | Kotlin/JVM | Swipe keyboard layout and the swipe input engine |
| `recognizer` | Kotlin/JVM | Single-stroke handwriting recognizer and its stroke templates |
| `app` | Android | Input method service, keyboard views, settings, input practice |

## Build and test

Requires JDK 17 and the Android SDK (minSdk 26).

```sh
./gradlew :app:assembleDebug     # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # unit tests of every module
```

A release build is signed only when all four Gradle properties `FSS_KEYSTORE` (path to the keystore file),
`FSS_KEYSTORE_PASSWORD`, `FSS_KEY_ALIAS` and `FSS_KEY_PASSWORD` are set, for example in your own
`~/.gradle/gradle.properties` (never in this repository). Without them `./gradlew :app:assembleRelease` builds an
unsigned APK; debug builds and tests do not need them.

## Design documents

Requirements, software requirements (SRS), high-level design (HLD) and low-level design (LLD) are in [`docs/`](docs/) (in Korean). The LLD index is [`docs/lld/README.md`](docs/lld/README.md).

## License

[Apache License 2.0](LICENSE). Copyright 2026 Joobyoung Kang.

---

# FSS Keyboard (한국어)

Kotlin으로 만든 안드로이드 키보드(IME)다. 입력 방식은 넷이다.

- **쿼티**: 영문 탭 자판
- **두벌식**: 한글 두벌식 탭 자판
- **한 획 손글씨 패드**: 글자마다 한 획으로 그려 쓴다(영문·한글)
- **긋기 입력**: 한글 자음을 탭하고 모음을 짧은 획으로 긋는다

## 모듈

| 모듈 | 종류 | 내용 |
|---|---|---|
| `core-hangul` | Kotlin/JVM | 한글 조합(자모, 음절 조립, 조합 방식, 입력 이벤트, 모음 획·천지인 모음 조립) |
| `stroke-core` | Kotlin/JVM | 공통 터치 기하, 탭 자판 배치, 탭/획 판정 |
| `gesture-engine` | Kotlin/JVM | 긋기 자판 배치와 긋기 입력 엔진 |
| `recognizer` | Kotlin/JVM | 한 획 손글씨 인식기와 획 템플릿 |
| `app` | Android | 입력기 서비스, 자판 화면, 설정, 입력 연습 |

## 빌드와 시험

JDK 17과 Android SDK(minSdk 26)가 필요하다.

```sh
./gradlew :app:assembleDebug     # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # 모든 모듈의 단위 시험
```

release 빌드는 Gradle 속성 네 개 `FSS_KEYSTORE`(키 저장소 파일 경로), `FSS_KEYSTORE_PASSWORD`, `FSS_KEY_ALIAS`,
`FSS_KEY_PASSWORD`가 모두 있을 때만 서명된다. 이 값은 저장소가 아니라 자기 `~/.gradle/gradle.properties`에 둔다.
없으면 `./gradlew :app:assembleRelease`는 서명 안 된 APK를 만들고, 디버그 빌드와 시험에는 필요 없다.

## 설계 문서

요구사항, SRS, HLD, LLD는 [`docs/`](docs/)에 있다. LLD 목차는 [`docs/lld/README.md`](docs/lld/README.md)다.

## 라이선스

[Apache License 2.0](LICENSE). Copyright 2026 Joobyoung Kang.
