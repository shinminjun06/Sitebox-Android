#!/bin/sh
set -eu
out="${TMPDIR:-/tmp}/sitebox-tests"
mkdir -p "$out"
javac -d "$out" app/src/main/java/com/sitebox/app/LibraryRules.java app/src/main/java/com/sitebox/app/BookLayout.java app/src/main/java/com/sitebox/app/SceneTheme.java tests/LibraryRulesTest.java tests/BookLayoutTest.java tests/SceneThemeTest.java
java -cp "$out" com.sitebox.app.LibraryRulesTest
java -cp "$out" com.sitebox.app.BookLayoutTest
java -cp "$out" com.sitebox.app.SceneThemeTest
if [ "${SKIP_ASSET_VALIDATION:-0}" != 1 ]; then python3 scripts/validate_assets.py; fi
