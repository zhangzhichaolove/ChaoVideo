#!/usr/bin/env bash
# Uses synthetic videos and changes settings/history; never run on a personal-data device.
set -euo pipefail
serial="${1:?Pass a disposable emulator serial, e.g. emulator-5580}"
[[ "$serial" =~ ^emulator-[0-9]+$ ]] || { echo 'Only disposable emulator-* targets are accepted.' >&2; exit 2; }
[[ "$(adb -s "$serial" shell getprop ro.build.version.sdk | tr -d '\r')" -ge 31 ]] || {
  echo 'Playback/PiP tests require API 31+.' >&2; exit 2;
}
cd "$(dirname "$0")/.."
mkdir -p app/build/verification/playback
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease \
  :app:assembleDebugAndroidTest --console=plain | tee app/build/verification/playback/build.txt
adb -s "$serial" install -r -g app/build/outputs/apk/debug/app-debug.apk
adb -s "$serial" install -r -g app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s "$serial" shell am instrument -w -r \
  -e class com.app.chao.chaoapp.playback.PlaybackSettingsExperienceTest,com.app.chao.chaoapp.download.OfflineDownloadTest \
  com.app.chao.chaoapp.test/androidx.test.runner.AndroidJUnitRunner \
  | tee app/build/verification/playback/instrumentation.txt
grep -q 'OK (8 tests)' app/build/verification/playback/instrumentation.txt
adb -s "$serial" pull /sdcard/Android/data/com.app.chao.chaoapp/files/verification-playback app/build/verification/playback/
adb -s "$serial" pull /sdcard/Android/data/com.app.chao.chaoapp/files/verification app/build/verification/playback/
