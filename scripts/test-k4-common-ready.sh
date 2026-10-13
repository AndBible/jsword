#!/usr/bin/env bash
# Planted-sample self-test for k4-common-ready.sh: one clean file passes, each planted token fails.
set -uo pipefail
guard="$(cd "$(dirname "$0")" && pwd)/k4-common-ready.sh"
tmp=$(mktemp -d); trap 'rm -rf "$tmp"' EXIT
mkdir -p "$tmp/root/p"
cat > "$tmp/root/p/Clean.kt" <<'EOF'
package p
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
// java.util.Locale in a comment is fine
class Clean { private val lock = SynchronizedObject(); fun f() = synchronized(lock) { referenceSystem.size } ; val referenceSystem = listOf(1) }
val workerThread = 1; val timerTask = 2; fun toURLString() = "x"
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
  'val f = RandomAccessFile(p, "r")'
  'val t = Thread { }'
  'val t = Timer()'
  'val e = Executors.newSingleThreadExecutor()'
  'val g = GregorianCalendar()'
  'val c = u.openConnection() as HttpsURLConnection'
  'val u = uri.toURL()'
  'val c = java.nio.charset.Charset.forName("UTF-8")'
  'val t = org.apache.commons.compress.archivers.tar.TarArchiveInputStream(s)'
  'val c: org.jdom2.Content? = null'
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
tree='org/crosswire/jsword/book/sword/TreeNode.kt'
mkdir -p "$tmp/root/$(dirname "$tree")"
printf 'package p\nprivate const val serialVersionUID = -2472601787934480762L\n' > "$tmp/root/$tree"
printf '%s #done\n' "$tree" > "$tmp/list"
"$guard" --list "$tmp/list" --root "$tmp/root" >/dev/null || { echo 'FAIL: exact TreeNode UID rejected'; fail=1; }
for sample in 'private const val serialVersionUID = 1L' 'private const val serialVersionUIDOther = -2472601787934480762L' 'private const val serialVersionUID = -2472601787934480762L; val x = java.lang.System.out'; do
  printf 'package p\n%s\n' "$sample" > "$tmp/root/$tree"
  output=$("$guard" --list "$tmp/list" --root "$tmp/root" 2>&1); status=$?
  if [ "$status" -ne 1 ] || ! grep -q "^$tree:2:" <<< "$output"; then
    echo "FAIL: TreeNode UID exception masked $sample"; fail=1
  fi
done
printf 'package p\nprivate const val serialVersionUID = -2472601787934480762L\n' > "$tmp/root/p/Wrong.kt"
echo 'p/Wrong.kt #done' > "$tmp/list"
output=$("$guard" --list "$tmp/list" --root "$tmp/root" 2>&1); status=$?
if [ "$status" -ne 1 ] || ! grep -q '^p/Wrong.kt:2:' <<< "$output"; then
  echo 'FAIL: TreeNode UID exception accepted wrong path'; fail=1
fi
install='org/crosswire/jsword/book/install/InstallException.kt'
mkdir -p "$tmp/root/$(dirname "$install")"
printf 'package p\nprivate const val serialVersionUID = 3258132440416335669L\n' > "$tmp/root/$install"
printf '%s #done\n' "$install" > "$tmp/list"
"$guard" --list "$tmp/list" --root "$tmp/root" >/dev/null || { echo 'FAIL: exact InstallException UID rejected'; fail=1; }
for sample in 'private const val serialVersionUID = 1L' 'private const val serialVersionUIDOther = 3258132440416335669L' 'private const val serialVersionUID = 3258132440416335669L; val x = java.lang.System.out'; do
  printf 'package p\n%s\n' "$sample" > "$tmp/root/$install"
  output=$("$guard" --list "$tmp/list" --root "$tmp/root" 2>&1); status=$?
  if [ "$status" -ne 1 ] || ! grep -q "^$install:2:" <<< "$output"; then
    echo "FAIL: InstallException UID exception masked $sample"; fail=1
  fi
done
printf 'package p\nprivate const val serialVersionUID = 3258132440416335669L\n' > "$tmp/root/p/Wrong.kt"
echo 'p/Wrong.kt #done' > "$tmp/list"
output=$("$guard" --list "$tmp/list" --root "$tmp/root" 2>&1); status=$?
if [ "$status" -ne 1 ] || ! grep -q '^p/Wrong\.kt:2:' <<< "$output"; then
  echo 'FAIL: InstallException UID exception accepted wrong path'; fail=1
fi
[ $fail = 0 ] && echo "k4-common-ready self-test: OK"
exit $fail
