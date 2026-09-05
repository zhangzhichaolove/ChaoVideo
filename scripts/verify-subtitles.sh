#!/usr/bin/env bash
# Synthetic media, disposable API 31+ emulator only. Leaves test history, not external media.
set -euo pipefail
serial="${1:?Pass a disposable emulator serial}"
[[ "$serial" =~ ^emulator-[0-9]+$ ]] || { echo 'Only disposable emulator-* targets are accepted.' >&2; exit 2; }
[[ "$(adb -s "$serial" shell getprop ro.build.version.sdk | tr -d '\r')" -ge 31 ]] || {
  echo 'Subtitle/PiP tests require API 31+.' >&2; exit 2;
}
cd "$(dirname "$0")/.."
scripts/generate-subtitle-fixtures.sh
out=app/build/verification/subtitles
mkdir -p "$out"
./gradlew :app:testDebugUnitTest :app:lintDebug :app:lintRelease :app:assembleDebug :app:assembleRelease :app:assembleDebugAndroidTest --console=plain | tee "$out/build.txt"
adb -s "$serial" install -r app/build/outputs/apk/debug/app-debug.apk
adb -s "$serial" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
trap 'adb -s "$serial" shell am force-stop com.app.chao.chaoapp >/dev/null 2>&1 || true' EXIT
adb -s "$serial" shell am instrument -w -r -e class com.app.chao.chaoapp.playback.OnlineSubtitleExperienceTest \
  com.app.chao.chaoapp.test/androidx.test.runner.AndroidJUnitRunner | tee "$out/instrumentation.txt"
grep -q 'OK (3 tests)' "$out/instrumentation.txt"
adb -s "$serial" pull /sdcard/Android/data/com.app.chao.chaoapp/files/verification-subtitles "$out/"
