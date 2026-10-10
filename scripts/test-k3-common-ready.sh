#!/usr/bin/env bash
# Planted-sample self-test for k3-common-ready.sh: one clean file passes, each planted token fails.
set -uo pipefail
guard="$(cd "$(dirname "$0")" && pwd)/k3-common-ready.sh"
tmp=$(mktemp -d); trap 'rm -rf "$tmp"' EXIT
mkdir -p "$tmp/root/p"
cat > "$tmp/root/p/Clean.kt" <<'EOF'
package p
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
// java.util.Locale in a comment is fine
class Clean { private val lock = SynchronizedObject(); fun f() = synchronized(lock) { referenceSystem.size } ; val referenceSystem = listOf(1) }
EOF
echo "p/Clean.kt #done" > "$tmp/list"
"$guard" --list "$tmp/list" --root "$tmp/root" >/dev/null || { echo "FAIL: clean file rejected"; exit 1; }
fail=0
planted=(
  'import java.util.Locale'
  'class A : Cloneable'
  'fun c() = super.clone()'
  'fun c() = super<Cloneable>.clone()'
  '@Synchronized fun s() {}'
  'fun s() = synchronized(this) { }'
  'private const val serialVersionUID = 1L'
  '@Transient var t = 0'
  'val l = LoggerFactory.getLogger(A::class.java)'
  'val n = javaClass.name'
  'val t = System.currentTimeMillis()'
  'val w = Character.isWhitespace(c)'
  'val s = String.format("%d", 1)'
  'val s = x.lowercase(Locale.ENGLISH)'
  'val s = x.toLowerCase(java.util.Locale.ROOT)'
)
for i in "${!planted[@]}"; do
  printf 'package p\n%s\n' "${planted[$i]}" > "$tmp/root/p/Bad$i.kt"
  echo "p/Bad$i.kt #done" > "$tmp/list"
  if "$guard" --list "$tmp/list" --root "$tmp/root" >/dev/null; then
    echo "FAIL: not detected: ${planted[$i]}"; fail=1
  fi
done
echo "p/Bad0.kt" > "$tmp/list"   # not #done -> ignored
"$guard" --list "$tmp/list" --root "$tmp/root" >/dev/null || { echo "FAIL: non-done file checked"; fail=1; }
[ $fail = 0 ] && echo "k3-common-ready self-test: OK"
exit $fail
