#!/bin/sh
set -eu
out="${TMPDIR:-/tmp}/sitebox-tests"
mkdir -p "$out"
javac -d "$out" app/src/main/java/com/sitebox/app/LibraryRules.java app/src/main/java/com/sitebox/app/BookLayout.java tests/LibraryRulesTest.java tests/BookLayoutTest.java
java -cp "$out" com.sitebox.app.LibraryRulesTest
java -cp "$out" com.sitebox.app.BookLayoutTest
python3 scripts/validate_assets.py
