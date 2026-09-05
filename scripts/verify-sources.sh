#!/usr/bin/env bash
# Source-switch fixtures create their own favorites/history; disposable emulator only.
set -euo pipefail
serial="${1:?Pass a disposable emulator serial, e.g. emulator-5580}"
[[ "$serial" =~ ^emulator-[0-9]+$ ]] || { echo 'Only disposable emulator-* targets are accepted.' >&2; exit 2; }
cd "$(dirname "$0")/.."
mkdir -p app/build/verification/sources
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease \
  :app:assembleDebugAndroidTest --console=plain | tee app/build/verification/sources/build.txt
adb -s "$serial" install -r -g app/build/outputs/apk/debug/app-debug.apk
adb -s "$serial" install -r -g app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s "$serial" shell am force-stop com.app.chao.chaoapp
out=app/build/verification/sources
rm -f "$out/api_address.xml"
if adb -s "$serial" shell run-as com.app.chao.chaoapp test -f shared_prefs/api_address.xml; then
  adb -s "$serial" exec-out run-as com.app.chao.chaoapp cat shared_prefs/api_address.xml > "$out/api_address.xml"
fi
restore_api() {
  adb -s "$serial" shell am force-stop com.app.chao.chaoapp >/dev/null 2>&1 || true
  if [[ -f "$out/api_address.xml" ]]; then
    adb -s "$serial" exec-in run-as com.app.chao.chaoapp sh -c "cat > shared_prefs/api_address.xml" < "$out/api_address.xml" || true
  else
    adb -s "$serial" shell run-as com.app.chao.chaoapp rm -f shared_prefs/api_address.xml || true
  fi
}
trap restore_api EXIT
adb -s "$serial" shell am instrument -w -r -e class com.app.chao.chaoapp.data.SourceIsolationExperienceTest \
  com.app.chao.chaoapp.test/androidx.test.runner.AndroidJUnitRunner | tee app/build/verification/sources/instrumentation.txt
grep -q 'OK (1 test)' app/build/verification/sources/instrumentation.txt
adb -s "$serial" pull /sdcard/Android/data/com.app.chao.chaoapp/files/verification-sources app/build/verification/sources/
