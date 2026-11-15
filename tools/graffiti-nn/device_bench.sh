#!/usr/bin/env bash
# Times graffiti recognition on a connected Android device without touching the installed keyboard
# (LLD-11 6.7 DV-NN, SRS NFR-13): the recognizer's classes, its resources and RecognitionBench go into one dex jar
# under /data/local/tmp, app_process runs it, and the files are removed afterwards.
#
# Run from the repository root:   bash tools/graffiti-nn/device_bench.sh [adb serial]
# Needs: adb on PATH, the Android SDK build-tools (d8), Python 3 (PYTHON, default "python").
# SDK: ANDROID_HOME, ANDROID_SDK_ROOT or sdk.dir of local.properties. Dependency jars: the Gradle cache (GRADLE_USER_HOME, default ~/.gradle).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"
PYTHON="${PYTHON:-python}"
SERIAL="${1:-}"
ADB=(adb)
[ -n "$SERIAL" ] && ADB=(adb -s "$SERIAL")

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [ -z "$SDK" ] && [ -f local.properties ]; then
    SDK="$(sed -n 's/^sdk\.dir=//p' local.properties | sed 's/\\:/:/g; s/\\\\/\//g')"
fi
[ -n "$SDK" ] || { echo "Android SDK not found (ANDROID_HOME or local.properties)"; exit 1; }
D8="$(ls "$SDK"/build-tools/*/d8* | sort | tail -n 1)"
ANDROID_JAR="$(ls "$SDK"/platforms/android-*/android.jar | sort | tail -n 1)"

OUT="recognizer/build/device-bench"
REMOTE="/data/local/tmp/graffiti-bench"
rm -rf "$OUT"
mkdir -p "$OUT"

./gradlew -q :core-hangul:jar :recognizer:jar :recognizer:testClasses
CACHE="${GRADLE_USER_HOME:-$HOME/.gradle}/caches/modules-2/files-2.1"
dep() { find "$CACHE/$1" -name "$2" | sort | tail -n 1; }
JARS=(
    recognizer/build/libs/recognizer.jar
    core-hangul/build/libs/core-hangul.jar
    "$(dep org.jetbrains.kotlin/kotlin-stdlib 'kotlin-stdlib-[0-9]*.jar')"
    "$(dep org.jetbrains.kotlinx/kotlinx-serialization-core-jvm 'kotlinx-serialization-core-jvm-[0-9]*.jar')"
    "$(dep org.jetbrains.kotlinx/kotlinx-serialization-json-jvm 'kotlinx-serialization-json-jvm-[0-9]*.jar')"
)
for j in "${JARS[@]}"; do [ -f "$j" ] || { echo "missing jar: $j"; exit 1; }; done

# d8 keeps no resources: the dex goes into a jar together with graffiti/templates.json and graffiti/graffiti-nn.bin.
"$D8" --release --min-api 26 --lib "$ANDROID_JAR" --output "$OUT/dex" "${JARS[@]}" recognizer/build/classes/kotlin/test/kbd/graffiti/bench/*.class 2>/dev/null \
    || { mkdir -p "$OUT/dex"; "$D8" --release --min-api 26 --lib "$ANDROID_JAR" --output "$OUT/dex" "${JARS[@]}" recognizer/build/classes/kotlin/test/kbd/graffiti/bench/*.class; }
"$PYTHON" tools/graffiti-nn/bench_pack.py "$OUT/dex/classes.dex" "$OUT/bench.jar" "$OUT/strokes.jsonl"

# Git Bash on Windows would rewrite the device paths; harmless elsewhere.
export MSYS_NO_PATHCONV=1

"${ADB[@]}" shell "rm -rf $REMOTE && mkdir -p $REMOTE"
"${ADB[@]}" push "$OUT/bench.jar" "$OUT/strokes.jsonl" "$REMOTE/" > /dev/null
cleanup() { "${ADB[@]}" shell "rm -rf $REMOTE"; }
trap cleanup EXIT
"${ADB[@]}" shell "getprop ro.product.model; getprop ro.build.version.release; getprop ro.product.cpu.abi"
"${ADB[@]}" shell "CLASSPATH=$REMOTE/bench.jar app_process $REMOTE kbd.graffiti.bench.RecognitionBench $REMOTE/strokes.jsonl" | tee "$OUT/result.txt"
