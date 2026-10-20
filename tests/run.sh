#!/bin/sh
set -eu
out="${TMPDIR:-/tmp}/sitebox-tests"
mkdir -p "$out"
javac -d "$out" app/src/main/java/com/sitebox/app/LibraryRules.java tests/LibraryRulesTest.java
java -cp "$out" com.sitebox.app.LibraryRulesTest
python3 scripts/validate_assets.py
