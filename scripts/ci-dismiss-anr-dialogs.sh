#!/usr/bin/env bash
# Clears "Application Not Responding" dialogs that slow CI emulators show after boot. They hold
# window focus, which instrumented tests that need the IME depend on.
set -u

# Hide future crash/ANR dialogs; the system proceeds as if they were accepted.
adb shell settings put global hide_error_dialogs 1

for attempt in $(seq 1 10); do
  focus=$(adb shell dumpsys window | grep mCurrentFocus | tr -s " \r\n" " ")
  echo "Focus: $focus"
  if ! echo "$focus" | grep -q "Not Responding"; then
    adb shell cmd statusbar collapse
    adb shell input keyevent KEYCODE_HOME
    exit 0
  fi
  adb shell uiautomator dump /sdcard/anr.xml >/dev/null 2>&1
  bounds=$(adb shell cat /sdcard/anr.xml | grep -o 'text="Wait"[^>]*bounds="\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]"' | grep -o '\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]' | head -1)
  if [ -n "$bounds" ]; then
    read -r left top right bottom <<<"$(echo "$bounds" | tr -c '0-9' ' ')"
    echo "Tapping Wait at $(( (left + right) / 2 )),$(( (top + bottom) / 2 ))"
    adb shell input tap $(( (left + right) / 2 )) $(( (top + bottom) / 2 ))
  else
    echo "No Wait button found (attempt $attempt)"
  fi
  sleep 2
done

echo "ANR dialog still holds focus"
adb shell dumpsys window | grep -E "mCurrentFocus|mFocusedApp" || true
exit 1
