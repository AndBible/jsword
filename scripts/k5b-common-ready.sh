#!/usr/bin/env bash
# K5b guard (reuses the K5 guard): files marked "#done" in the move list must not use JVM-only APIs.
# Heuristic; the real proof is the iOS compile after the move (K5b). Deleted after Task 14 (M3).
set -euo pipefail
here="$(cd "$(dirname "$0")/.." && pwd)"
list="$here/docs/k5b-move-set.txt"
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
  '\bRandomAccessFile\b'
  '\bThread\b'
  '\bTimer(Task)?\b'
  '\bExecutors\b'
  '\bGregorianCalendar\b'
  '\bHttpsURLConnection\b'
  '\.toURL\(\)'
  '\bjava\.nio\.'
  '\borg\.apache\.commons\.'
  '\borg\.jdom2\.'
  '\borg\.xml\.sax\b'
  '\bjavax\.xml\b'
  '\bjava\.util\.concurrent\b'
  '\bBlockingQueue\b'
  '\bEventObject\b'
  '\bEventListener\b'
  '\bjava\.lang\.reflect\b'
  '\bInstantiationException\b'
)
hits=0
while IFS= read -r line; do
  case "$line" in ''|'#'*) continue ;; esac
  case "$line" in *'#done'*) ;; *) continue ;; esac
  f="${line%% *}"
  [ -f "$root/$f" ] || { echo "$f: listed file missing" ; hits=1; continue; }
  atomic=0
  if grep -q '^import kotlinx\.atomicfu\.locks\.synchronized' "$root/$f"; then
    atomic=1
  else
    grep_status=$?
    [ "$grep_status" -eq 1 ] || { echo "$f: grep failed while checking atomicfu import"; hits=1; continue; }
  fi
  for p in "${patterns[@]}"; do
    [ "$p" = '(^|[^.A-Za-z])synchronized\(' ] && [ "$atomic" = 1 ] && continue
    # Distinguish grep's no-match status from read/search errors before filtering comments.
    if matches=$(grep -nE "$p" "$root/$f"); then
      if out=$(grep -vE '^[0-9]+:\s*(\*|//|/\*)' <<< "$matches"); then
        while IFS= read -r m; do
          # This portable constant preserves TreeNode's frozen JVM stream contract.
          if [ "$p" = 'serialVersionUID' ] && [ "$f" = 'org/crosswire/jsword/book/sword/TreeNode.kt' ] &&
             [[ "$m" =~ ^[0-9]+:[[:space:]]*private[[:space:]]const[[:space:]]val[[:space:]]serialVersionUID[[:space:]]=[[:space:]]-2472601787934480762L$ ]]; then
            continue
          fi
          # Kotlin Exception is Serializable on JVM; preserve the frozen install exception UID.
          if [ "$p" = 'serialVersionUID' ] && [ "$f" = 'org/crosswire/jsword/book/install/InstallException.kt' ] &&
             [[ "$m" =~ ^[0-9]+:[[:space:]]*private[[:space:]]const[[:space:]]val[[:space:]]serialVersionUID[[:space:]]=[[:space:]]3258132440416335669L$ ]]; then
            continue
          fi
          # Portable constants retain event UIDs through the explicit JVM Serializable seam.
          if [ "$p" = 'serialVersionUID' ]; then
            case "$f:$m" in
              org/crosswire/common/progress/WorkEvent.kt:*) uid=3976736990807011378 ;;
              org/crosswire/common/util/ReporterEvent.kt:*) uid=4121978048640988213 ;;
              *) uid='' ;;
            esac
            if [ -n "$uid" ] && [[ "$m" =~ ^[0-9]+:[[:space:]]*private[[:space:]]const[[:space:]]val[[:space:]]serialVersionUID[[:space:]]=[[:space:]]${uid}L$ ]]; then
              continue
            fi
          fi
          echo "$f:$m   [$p]"; hits=1
        done <<< "$out"
      else
        grep_status=$?
        [ "$grep_status" -eq 1 ] || { echo "$f: grep failed while filtering comments"; hits=1; }
      fi
    else
      grep_status=$?
      [ "$grep_status" -eq 1 ] || { echo "$f: grep failed"; hits=1; }
    fi
  done
done < "$list"
exit $hits
