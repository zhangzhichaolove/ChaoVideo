#!/usr/bin/env bash
# Disposable emulator only. Test-signs a COPY of the minified APK with the public debug key.
set -euo pipefail
serial="${1:?Pass a disposable emulator serial}"
[[ "$serial" =~ ^emulator-[0-9]+$ ]] || { echo 'Only disposable emulator-* targets are accepted.' >&2; exit 2; }
cd "$(dirname "$0")/.."
: "${ANDROID_HOME:?Set ANDROID_HOME to the Android SDK}"
apksigner="$(find "$ANDROID_HOME/build-tools" -name apksigner -type f | sort | tail -1)"
[[ -x "$apksigner" ]] || { echo 'Android SDK apksigner is required.' >&2; exit 2; }
./gradlew :app:assembleDebug :app:assembleRelease :app:assembleDebugAndroidTest --console=plain
out=app/build/verification/playback-release
mkdir -p "$out"
adb -s "$serial" install -r app/build/outputs/apk/debug/app-debug.apk
adb -s "$serial" shell am force-stop com.app.chao.chaoapp
# Preserve just the preferences the platform-only runner edits, even if the Release process crashes.
for pref in api_address video_playback_settings; do
  rm -f "$out/$pref.xml" "$out/$pref.absent"
  if adb -s "$serial" shell run-as com.app.chao.chaoapp test -f "shared_prefs/$pref.xml"; then
    adb -s "$serial" exec-out run-as com.app.chao.chaoapp cat "shared_prefs/$pref.xml" > "$out/$pref.xml"
  else
    touch "$out/$pref.absent"
  fi
done
server_pid=""
port=""
restore_debug() {
  if [[ -n "$server_pid" ]]; then
    kill "$server_pid" 2>/dev/null || true
    wait "$server_pid" 2>/dev/null || true
  fi
  if [[ -n "$port" ]]; then adb -s "$serial" reverse --remove "tcp:$port" >/dev/null 2>&1 || true; fi
  adb -s "$serial" logcat -d -s AndroidRuntime:E > "$out/runtime.txt" 2>/dev/null || true
  adb -s "$serial" shell am force-stop com.app.chao.chaoapp >/dev/null 2>&1 || true
  adb -s "$serial" install -r app/build/outputs/apk/debug/app-debug.apk >/dev/null 2>&1 || true
  for pref in api_address video_playback_settings; do
    if [[ -f "$out/$pref.xml" ]]; then
      adb -s "$serial" exec-in run-as com.app.chao.chaoapp sh -c "cat > shared_prefs/$pref.xml" < "$out/$pref.xml" || true
    elif [[ -f "$out/$pref.absent" ]]; then
      adb -s "$serial" shell run-as com.app.chao.chaoapp rm -f "shared_prefs/$pref.xml" || true
    fi
  done
  if [[ -f app/build/verification/playback-release/standard-test.apk ]]; then
    adb -s "$serial" install -r app/build/verification/playback-release/standard-test.apk >/dev/null 2>&1 || true
  fi
}
trap restore_debug EXIT
cp app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk "$out/standard-test.apk"
cp app/build/outputs/mapping/release/mapping.txt "$out/mapping.txt"
rm -f "$out/api-port.txt"
python3 scripts/playback-release-api.py "$out/api-port.txt" > "$out/api-server.txt" 2>&1 &
server_pid=$!
for ((attempt=0; attempt<600; attempt++)); do
  [[ -s "$out/api-port.txt" ]] && break
  kill -0 "$server_pid"
  sleep 0.05
done
port="$(cat "$out/api-port.txt")"
adb -s "$serial" reverse "tcp:$port" "tcp:$port"
# This is a test fixture copy, never the production release signing configuration.
"$apksigner" sign --ks "$HOME/.android/debug.keystore" --ks-pass pass:android --key-pass pass:android \
  --out "$out/app-release-test-signed.apk" app/build/outputs/apk/release/app-release-unsigned.apk
adb -s "$serial" install -r "$out/app-release-test-signed.apk"
./gradlew :app:assembleDebugAndroidTest -PplatformReleaseSmoke=true --console=plain
adb -s "$serial" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s "$serial" shell am instrument -w -r \
  -e api_url "http://127.0.0.1:$port/" \
  com.app.chao.chaoapp.test/com.app.chao.chaoapp.playback.ReleasePlaybackInstrumentation | tee "$out/release-test.txt"
grep -q 'RELEASE_PLAYBACK_OK' "$out/release-test.txt"
grep -q 'RELEASE_SUBTITLES_OK' "$out/release-test.txt"
grep -q 'RELEASE_DIAGNOSTIC_PRIVACY_OK' "$out/release-test.txt"
adb -s "$serial" pull /sdcard/Android/data/com.app.chao.chaoapp/files/verification-release-playback "$out/"
# EXIT trap stops synthetic audio and restores the standard debug app, including on failure.
echo 'Minified Release displayed API data, decoded synthetic video and restored/changed playback settings and rendered/toggled subtitles; diagnostic capture stays disabled.'
