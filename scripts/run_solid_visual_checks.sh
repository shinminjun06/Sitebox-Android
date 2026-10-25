#!/usr/bin/env bash
set -euo pipefail
mkdir -p solid-preview
adb logcat -c
adb logcat -v threadtime > solid-preview/logcat.txt &
log_pid=$!
trap 'kill "$log_pid" 2>/dev/null || true' EXIT
status=0
timeout 480s adb shell am instrument -w -e visualOnly true com.sitebox.app.test/com.sitebox.app.ModelSmokeInstrumentation > solid-preview.txt 2>&1 || status=$?
timeout 15s adb pull /sdcard/Android/data/com.sitebox.app/files/qa/ solid-preview/ || true
timeout 15s adb logcat -d >> solid-preview/logcat.txt || true
cat solid-preview.txt
if [ "$status" -ne 0 ]; then exit "$status"; fi
grep -q 'PASS solid geometry and natural window preview rendered on native GPU' solid-preview.txt
! grep -q 'FAIL ' solid-preview.txt
