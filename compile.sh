#!/usr/bin/env bash

set -euo pipefail

javac *.java && cat "$1" \
  | java Lexer samples/final/lexer.txt \
  | java FirstFirst \
  | java Parser samples/final/parser.txt \
  | java Semantic

