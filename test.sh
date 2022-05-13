#!/usr/bin/env bash

set -euo pipefail

javac *.java

run() {
  cat "$1" \
    | java Lexer samples/final/lexer.txt \
    | java FirstFirst \
    | java Parser samples/final/parser.txt \
    | java Semantic
}

for f in ./samples/final/test_fail/*; do
  set +e
  OUTPUT=$(run "$f" 2>&1)
  EXIT_CODE=$?
  set -e

  if (( $EXIT_CODE != 0 )); then
    printf '\e[1;32m[OK]\e[0m %s\n' "$f"
  else
    printf '\e[1;31m[NO]\e[0m \e[2mShould fail\e[0m %s\n' "$f"
    printf '%s\n' "$OUTPUT"
    exit 1
  fi
done

