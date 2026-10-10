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
  output=$("$guard" --list "$tmp/list" --root "$tmp/root" 2>&1); status=$?
  if [ "$status" -ne 1 ] || ! grep -q '^p/Bad'"$i"'\.kt:2:' <<< "$output"; then
    echo "FAIL: missing exit 1 or path:line for: ${planted[$i]}"
    echo "$output"
    fail=1
  fi
done
echo "p/Bad0.kt" > "$tmp/list"   # not #done -> ignored
"$guard" --list "$tmp/list" --root "$tmp/root" >/dev/null || { echo "FAIL: non-done file checked"; fail=1; }
echo 'p/Missing.kt #done' > "$tmp/list"
output=$("$guard" --list "$tmp/list" --root "$tmp/root" 2>&1); status=$?
if [ "$status" -ne 1 ] || ! grep -q '^p/Missing\.kt: listed file missing$' <<< "$output"; then
  echo "FAIL: missing marked file was not reported with exit 1"
  echo "$output"
  fail=1
fi
mkdir -p "$tmp/bin"
cat > "$tmp/bin/grep" <<'EOF'
#!/usr/bin/env bash
if [ "${1-}" = -nE ] && [[ " $* " == *" $UNREADABLE_FIXTURE "* ]]; then
  exit 2
fi
exec /usr/bin/grep "$@"
EOF
chmod +x "$tmp/bin/grep"
printf 'package p\nclass ReadFailure\n' > "$tmp/root/p/Unreadable.kt"
echo "p/Unreadable.kt #done" > "$tmp/list"
output=$(PATH="$tmp/bin:$PATH" UNREADABLE_FIXTURE="$tmp/root/p/Unreadable.kt" "$guard" --list "$tmp/list" --root "$tmp/root" 2>&1); status=$?
if [ "$status" -ne 1 ] || ! grep -q '^p/Unreadable\.kt: grep failed' <<< "$output"; then
  echo "FAIL: grep read error was not reported with exit 1"
  echo "$output"
  fail=1
fi
[ $fail = 0 ] && echo "k3-common-ready self-test: OK"
exit $fail
