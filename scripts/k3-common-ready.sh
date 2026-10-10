#!/usr/bin/env bash
# K3 guard (spec §4.1): files marked "#done" in the move list must not use JVM-only APIs.
# Heuristic; the real proof is the iOS compile after the move (Task 11). Deleted after the move.
set -euo pipefail
here="$(cd "$(dirname "$0")/.." && pwd)"
list="$here/docs/k3-move-set.txt"
root="$here/src/jvmMain/kotlin"
while [ $# -gt 0 ]; do
  case "$1" in
    --list) list="$2"; shift 2 ;;
    --root) root="$2"; shift 2 ;;
    *) echo "usage: $0 [--list FILE] [--root DIR]" >&2; exit 2 ;;
  esac
done
patterns=(
  '^import java\.'
  '^import javax\.'
  '\bCloneable\b'
  'super(<[A-Za-z]+>)?\.clone\('
  '@Synchronized'
  '(^|[^.A-Za-z])synchronized\('
  'serialVersionUID'
  '@Transient'
  '::class\.java\b'
  '\bjavaClass\b'
  '\bSystem\.'
  '\bCharacter\.'
  '\bString\.format\('
  '\b(to)?(lower|upper)[Cc]ase\([^)]*Locale'
  '\bjava\.(util|io|lang|text|net)\.'
)
hits=0
while IFS= read -r line; do
  case "$line" in ''|'#'*) continue ;; esac
  case "$line" in *'#done'*) ;; *) continue ;; esac
  f="${line%% *}"
  [ -f "$root/$f" ] || { echo "$f: listed file missing" ; hits=1; continue; }
  atomic=0; grep -q '^import kotlinx\.atomicfu\.locks\.synchronized' "$root/$f" && atomic=1
  for p in "${patterns[@]}"; do
    [ "$p" = '(^|[^.A-Za-z])synchronized\(' ] && [ "$atomic" = 1 ] && continue
    # strip comment lines (KDoc / line comments) before matching
    if out=$(grep -nE "$p" "$root/$f" | grep -vE '^[0-9]+:\s*(\*|//|/\*)'); then
      while IFS= read -r m; do echo "$f:$m   [$p]"; done <<< "$out"; hits=1
    fi
  done
done < "$list"
exit $hits
