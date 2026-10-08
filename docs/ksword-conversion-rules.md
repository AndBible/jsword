# KSword conversion rules (phase 1: Java → Kotlin)

Source: docs/superpowers/specs/2026-10-08-ksword-k0-conversion-foundations-design.md §7 (and-bible).
Every agent converting JSword Java to Kotlin follows these rules. Phase 2 (commonization) has its own rules.

## 1. Translate, don't improve
Behaviour stays identical, including odd behaviour. No refactoring, renaming, API cleanup or "fixes".
A real bug found while converting is noted in the commit message and left as is.

## 2. Same FQN, same Java-visible API
- Same package and class name; the `.java` file is `git mv`-ed to `.kt` in the matching `kotlin` directory
  first, then edited, so history follows. Do the `git mv` in its OWN commit (the .kt file still holds the
  Java text and may not compile) and edit the content in the next one; a mixed rename + heavy edit commit
  loses `git log --follow` (this happened for diff/Commonality, DiffCleanup, Distance).
- Static members → `companion object` with `@JvmStatic` (methods) and `@JvmField` / `const val` (fields).
  A companion holding only PRIVATE statics is declared `private companion object` (no extra public JVM
  members); public statics need a normal companion with `@JvmStatic`/`@JvmField`/`const`.
  A `private static` method that tests reach by reflection keeps `@JvmStatic` so it stays on the outer class.
- Overloads → `@JvmOverloads` only where Java had exactly those overloads; otherwise write each overload.
- Checked exceptions → `@Throws(...)` with the same exception list.
- Getter/setter pairs → properties only when the JVM names stay identical (`getX`/`setX`, `isX` for booleans
  declared `isX`); otherwise keep functions. Use `@JvmName` where needed.
- Non-final classes and non-final methods stay `open`; `final` Java classes become plain Kotlin classes.
  `:app` subclasses `AbstractBookDriver`, `AbstractKeyBackend`, `OpenFileState`, `Installer`,
  `AbstractBookList`, `DefaultKeyList` and implements `BookFilter`, `LocaleProvider`, `WorkListener`,
  `ReporterListener` — their overridable surface must not shrink.
- Visibility: `public` → public; `protected` → `protected`; package-private → `internal` only if no Java
  caller in another package needs it, else public with a `// was package-private` comment.

## 3. Conservative nullability
A type is non-null only when the code proves it (assigned non-null on every path, never compared to null,
never set from a nullable source). Parameters of public/protected methods are nullable unless every caller
in jsword and `:app` passes non-null **and** the Java code dereferenced it unconditionally. A Kotlin
non-null parameter turns a Java caller's `null` into an immediate exception. Once an interface that Java
classes implement is converted (e.g. `Activatable.activate(lock: Lock?)`), later Kotlin overriders must use
the same nullability.

## 4. Known traps
- Integer division and `char`/`int` arithmetic: keep the Java types (`Char.code`, `toChar()`), no implicit
  widening differences; `(int) someDouble` → `toInt()` (saturating, like Java).
- `==` in Java is identity for objects → `===`; `.equals` → `==`.
- `switch` fall-through → explicit duplicate branches; `switch` on strings → `when`.
- Static initialization order: companion properties initialize in declaration order when the class
  initializes — keep the Java order.
- `static synchronized` → `synchronized(Outer::class.java) { }` (same monitor as Java); instance
  `synchronized` methods → `@Synchronized`.
- `Cloneable`/`clone()`, `Serializable`, `readObject`/`writeObject`/`readResolve`, `transient` are kept.
- `assert cond : msg` → `jswordAssert(cond) { msg }` (`org.crosswire.common.util.jswordAssert`, created
  in K0 Task 7; enabled like Java `-ea` on the JVM, no-op on native). It always evaluates `cond` (Java `assert` skips
  evaluation when assertions are disabled), so a condition with side effects or real cost needs an explicit
  guard. Kotlin's `assert` is governed by `-ea` for the `kotlin` package, not per Java package.
- `String.split(regex)` / `Pattern.split` drop trailing empty strings and return `[input]` when nothing
  matches → `splitLikeJava(Regex(..))` (`org.crosswire.common.util`, created in K0 Task 7; not for
  zero-width patterns).
- `Matcher.group(n)` returns `null` for a non-participating group; `MatchResult.groupValues[n]` returns
  `""` — use `groups[n]?.value` where null mattered.
- `new String(bytes, ISO_8859_1)` / `getBytes(ISO_8859_1)`: in commonMain use
  `org.crosswire.common.util.latin1String()` / `latin1Bytes()` (created in K0 Task 7; unmappable chars
  → `?`, as Java).
- `String.format` → keep on the JVM with an explicit `Locale` exactly as Java had (default locale if Java
  used the default).
- `Math.x` → `kotlin.math.x` (or `Math.x` in jvmMain); `Math.pow(2, n)` → `2.0.pow(n)`, not `1 shl n`.
- `System.currentTimeMillis()` in commonMain → `kotlin.time.TimeSource.Monotonic` (note the change in the
  KDoc); in jvmMain keep it.
- `java.util.Stack` → `kotlin.collections.ArrayDeque` (`addLast`/`removeLast`/`last()`), keeping LIFO order.
- Varargs → `vararg`; Java callers passing arrays still compile.
- Boxed vs primitive overloads (`remove(int)` vs `remove(Object)` on lists) — check every call.

## 5. Tests
Tests are **not** converted in the batch that converts their subject; the Java tests stay an independent
check. They are converted in phase 2 when they move to `commonTest`.

## 6. Comments
Keep comments and Javadoc as KDoc; keep the licence header.

## 7. Placement
`commonMain` only if the converted file uses no JVM API (after the replacements in §4 that have common
equivalents); otherwise `jvmMain`. When in doubt, `jvmMain` — commonization is phase 2.
Converted commonMain files need no file-level `@file:OptIn(ExperimentalMultiplatform)`: the module opts in
globally (`build.gradle.kts`: `compilerOptions { optIn.add("kotlin.ExperimentalMultiplatform") }`).
`@JvmStatic`/`@JvmField` in commonMain need `import kotlin.jvm.JvmStatic` / `import kotlin.jvm.JvmField`.

## 8. Every batch
`jvmTest` and `parityTest` green, `:app` full unit suite and assemble green, iOS compile green, a review.
