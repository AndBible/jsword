#!/usr/bin/env bash
# Rebuilds oracle/jsword-baseline.jar from the KSword K0 baseline commit.
# The commit hash below is the source of truth; the tag ksword-base is only a convenience
# (tags can be lost in the jailbee host<->container git bridge). See
# docs/superpowers/specs/2026-10-08-ksword-k0-conversion-foundations-design.md §6.1.
set -euo pipefail

BASELINE_COMMIT=50b241187ceb289495e16c1c94a59699159481e7
BASELINE_TAG=ksword-base

here="$(cd "$(dirname "$0")" && pwd)"
repo="$(cd "$here/.." && pwd)"

git -C "$repo" cat-file -e "${BASELINE_COMMIT}^{commit}"
if git -C "$repo" rev-parse -q --verify "refs/tags/${BASELINE_TAG}" >/dev/null; then
    tagged="$(git -C "$repo" rev-parse "${BASELINE_TAG}^{commit}")"
    if [ "$tagged" != "$BASELINE_COMMIT" ]; then
        echo "Tag ${BASELINE_TAG} points to ${tagged}, expected ${BASELINE_COMMIT}" >&2
        exit 1
    fi
fi

command -v jar >/dev/null || { echo "JDK 'jar' tool not on PATH" >&2; exit 1; }

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
git -C "$repo" archive "$BASELINE_COMMIT" | tar -x -C "$work"
(cd "$work" && ./gradlew --no-daemon -Dorg.gradle.native=false jvmJar)

built="$(ls "$work"/build/libs/jsword-jvm-*.jar | grep -v -- '-sources' | head -1)"
printf 'Baseline-Commit: %s\n' "$BASELINE_COMMIT" > "$work/baseline.mf"
cp "$built" "$here/jsword-baseline.jar"
jar --update --file "$here/jsword-baseline.jar" --manifest "$work/baseline.mf"
echo "Wrote $here/jsword-baseline.jar from $BASELINE_COMMIT"
