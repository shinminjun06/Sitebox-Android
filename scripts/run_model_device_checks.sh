#!/usr/bin/env bash
set -euo pipefail
mkdir -p room-device-qa
adb logcat -c
adb logcat -v threadtime > room-device-qa/logcat.txt &
log_pid=$!
(
  while adb get-state >/dev/null 2>&1; do
    adb pull /sdcard/Android/data/com.sitebox.app/files/qa/ room-device-qa/ >/dev/null 2>&1 || true
    free -m >> room-device-qa/host-memory.txt
    sleep 3
  done
) &
pull_pid=$!
(
  sleep 150
  if ! grep -q 'PASS actual supplied GLB rendered with furniture and walls' room-device-tests.txt; then
    model_pid=$(adb shell pidof -s com.sitebox.app | tr -d '\r' || true)
    if [ -n "$model_pid" ]; then
      timeout 20s adb shell debuggerd -b "$model_pid" > room-device-qa/native-thread-dump.txt 2>&1 || true
      adb shell dumpsys activity activities > room-device-qa/startup-activity-state.txt || true
      echo 'FAIL native startup watchdog: no displayed model within 150 seconds' >> room-device-tests.txt
      adb shell am force-stop com.sitebox.app || true
    fi
  fi
) &
watchdog_pid=$!
trap 'kill "$log_pid" "$pull_pid" "$watchdog_pid" 2>/dev/null || true' EXIT
status=0
timeout 1200s adb shell am instrument -w com.sitebox.app.test/com.sitebox.app.ModelSmokeInstrumentation | tee room-device-tests.txt || status=$?
adb pull /sdcard/Android/data/com.sitebox.app/files/qa/ room-device-qa/ || true
adb logcat -d >> room-device-qa/logcat.txt || true
adb shell dumpsys gfxinfo com.sitebox.app framestats > room-device-qa/emulator-frame-stats.txt || true
if [ "$status" -ne 0 ]; then exit "$status"; fi
grep -q 'PASS actual supplied GLB rendered with furniture and walls' room-device-tests.txt
grep -q 'PASS automatic sync merges, deletions, conflicts, account persistence and automatic restore verified' room-device-tests.txt
grep -q 'PASS actual 3D room, shared camera, native book management, gestures and lifecycle verified' room-device-tests.txt
! grep -q 'FAIL ' room-device-tests.txt
