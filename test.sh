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

print_file_error() {
  printf '\e[1;31m[NO]\e[0m \e[2m%s\e[0m %s\n' "$2" "$1"
}

print_file_warning() {
  printf '\e[1;33m[HM]\e[0m \e[2m%s\e[0m %s\n' "$2" "$1"
}

print_file_ok() {
  printf '\e[1;32m[OK]\e[0m %s\n' "$1"
}

for f in ./samples/final/test_fail/*.txt; do
  set +e
  OUTPUT=$(run "$f" 2>&1)
  EXIT_CODE=$?
  set -e

  if (( $EXIT_CODE != 0 )); then
    print_file_ok "$f"
  else
    print_file_error "$f" "Should fail"
    printf '%s\n' "$OUTPUT"
    exit 1
  fi
done


for f in ./samples/final/test_ok/*.txt; do
  TMP=$(mktemp)
  EXPECTED="${f%.txt}.out"

  set +e
  run "$f" &> "$TMP"
  EXIT_CODE=$?
  set -e

  if (( $EXIT_CODE == 0 )); then
    if git ls-files --error-unmatch "$EXPECTED" &> /dev/null; then
      if diff "$EXPECTED" "$TMP" &> /dev/null; then
        print_file_ok "$f"
      else
        print_file_error "$f" "Should match previous file"
        cat "$TMP"
        exit 1
      fi
    else
      print_file_warning "$f" "Output is not tracked by git"
      cat "$TMP"
      mv "$TMP" "$EXPECTED"
    fi
  else
    print_file_error "$f" "Should compile correctly"
    cat "$TMP"
    exit 1
  fi
done

