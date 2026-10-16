#!/usr/bin/env bash
# Fetches the KSword K6 search-parity corpus into <repo>/.local/k6-corpus (SWORD layout: mods.d + modules).
# Modules come from CrossWire's rawzip packages, verified against parity/k6-corpus.sha256.
#   fetch-k6-corpus.sh            verify against the hash file (fails on mismatch or missing entry)
#   fetch-k6-corpus.sh --record   write the hash file (first run, or after deliberately adopting new versions)
# KJV and FinPR are copied from <repo>/.local/testmods.zip. Idempotent: existing downloads are reused.
# No `unzip` in the container: extraction uses Python's zipfile.
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JSWORD="$(dirname "$HERE")"
REPO="$(dirname "$JSWORD")"
OUT="$REPO/.local/k6-corpus"
CACHE="$OUT/.zips"
HASHES="$JSWORD/parity/k6-corpus.sha256"
BASE="https://crosswire.org/ftpmirror/pub/sword/packages/rawzip"
MODULES="ChiUns ChiUn JapKougo ThaiKJV GerBoLut GreVamvas Byz HebModern WLC AraSVD FarOPV CzeBKR RusSynodal FreCrampon MHC"
LOCAL_MODULES="KJV FinPR"
RECORD=0
[ "${1:-}" = "--record" ] && RECORD=1

mkdir -p "$OUT" "$CACHE"
TMP_HASHES="$(mktemp)"
trap 'rm -f "$TMP_HASHES"' EXIT

for m in $MODULES; do
  zip="$CACHE/$m.zip"
  if [ ! -s "$zip" ]; then
    curl -fsS -o "$zip.part" "$BASE/$m.zip"
    mv "$zip.part" "$zip"
  fi
  echo "$(sha256sum "$zip" | cut -d' ' -f1)  $m.zip" >> "$TMP_HASHES"
done

if [ "$RECORD" = 1 ]; then
  sort -k2 "$TMP_HASHES" > "$HASHES"
  echo "recorded $HASHES"
else
  [ -f "$HASHES" ] || { echo "missing $HASHES (run with --record once)" >&2; exit 1; }
  sort -k2 "$TMP_HASHES" | diff - <(sort -k2 "$HASHES") >&2 || { echo "corpus hash mismatch" >&2; exit 1; }
fi

python3 -I - "$OUT" "$CACHE" "$REPO/.local/testmods.zip" "$MODULES" "$LOCAL_MODULES" <<'PY'
import os, sys, zipfile, re
out, cache, testmods, modules, local = sys.argv[1:6]

def extract(z, out, only=None):
    for info in z.infolist():
        name = info.filename
        if only and not only(name):
            continue
        target = os.path.realpath(os.path.join(out, name))
        if not target.startswith(os.path.realpath(out) + os.sep):
            raise SystemExit("unsafe zip entry: " + name)
        if info.is_dir():
            os.makedirs(target, exist_ok=True)
        else:
            os.makedirs(os.path.dirname(target), exist_ok=True)
            with z.open(info) as src, open(target, "wb") as dst:
                dst.write(src.read())

for m in modules.split():
    extract(zipfile.ZipFile(os.path.join(cache, m + ".zip")), out)

# KJV and FinPR from the existing test modules: their conf, plus data directory.
names = [l.lower() for l in local.split()]
def wanted(n):
    low = n.lower()
    return any(low == "mods.d/%s.conf" % k or ("/%s/" % k) in low for k in names)
extract(zipfile.ZipFile(testmods), out, wanted)
PY

echo "modules: $(ls "$OUT/mods.d" | wc -l) in $OUT/mods.d"
