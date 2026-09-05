#!/usr/bin/env bash
# The tests create/remove download tasks, local synthetic videos and history in this app.
set -euo pipefail
serial="${1:?Pass a disposable emulator serial, e.g. emulator-5580}"
[[ "$serial" =~ ^emulator-[0-9]+$ ]] || { echo 'Only disposable emulator-* targets are accepted.' >&2; exit 2; }
cd "$(dirname "$0")/.."
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest --console=plain
adb -s "$serial" install -r -g app/build/outputs/apk/debug/app-debug.apk
adb -s "$serial" install -r -g app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
mkdir -p app/build/verification/offline
adb -s "$serial" shell am instrument -w -r -e class com.app.chao.chaoapp.download.OfflineDownloadTest \
  com.app.chao.chaoapp.test/androidx.test.runner.AndroidJUnitRunner | tee app/build/verification/offline/instrumentation.txt
grep -q 'OK (2 tests)' app/build/verification/offline/instrumentation.txt
adb -s "$serial" pull /sdcard/Android/data/com.app.chao.chaoapp/files/verification app/build/verification/offline/
