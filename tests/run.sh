#!/bin/sh
set -eu
out="${TMPDIR:-/tmp}/sitebox-tests"
mkdir -p "$out"
javac -d "$out" app/src/main/java/com/sitebox/app/LibraryRules.java app/src/main/java/com/sitebox/app/BookLayout.java app/src/main/java/com/sitebox/app/SceneTheme.java tests/LibraryRulesTest.java tests/BookLayoutTest.java tests/SceneThemeTest.java app/src/main/java/com/sitebox/app/SceneProjection.java tests/SceneProjectionTest.java tests/RoomPerspectiveTest.java app/src/main/java/com/sitebox/app/WeatherState.java tests/WeatherStateTest.java
java -cp "$out" com.sitebox.app.LibraryRulesTest
java -cp "$out" com.sitebox.app.BookLayoutTest
java -cp "$out" com.sitebox.app.SceneThemeTest
java -cp "$out" com.sitebox.app.RoomPerspectiveTest
java -cp "$out" com.sitebox.app.SceneProjectionTest
java -cp "$out" com.sitebox.app.WeatherStateTest
if [ "${SKIP_ASSET_VALIDATION:-0}" != 1 ]; then python3 scripts/validate_assets.py; fi
