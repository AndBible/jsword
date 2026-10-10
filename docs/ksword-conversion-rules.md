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
- Kotlin `trim()` != Java `String.trim()` (`isWhitespace` vs `<= ' '`): Java strips every char up to U+0020 (control
  chars such as U+001A) and keeps NBSP; convert with `trim { it <= ' ' }`.
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

## 9. Member form for Kotlin consumers (K1)
Kotlin consumers (`:app`) read a Java getter either as a property (`book.initials`) or as a call
(`book.getInitials()`); after conversion only one form compiles. Rule: if any Kotlin consumer
**overrides** the getter (`override fun getX()`), it stays `fun getX()`; otherwise it becomes a property
(`val`/`var`/`open val`) with the same JVM names. Consumer call sites that then fail to compile are
rewritten to the surviving form — that is the only consumer edit allowed. A Kotlin class implementing a
still-Java interface overrides its getters with `fun`; convert interfaces before their implementers.
A Java interface a consumer implements with a lambda (SAM) becomes `fun interface`.
Protected fields that subclasses read stay fields: `@JvmField protected var x`.

## 10. API parity gate (K1)
`ApiParityTest` (in `:jsword:parityTest`) compares every public/protected member and the serialization
members of each baseline class with the current code. Removed/changed = failure; additions only for
`Companion`, enum `getEntries` and `$`-mangled `internal` methods, or via `oracle/api-allowlist.txt` with a reason. Read
`build/reports/ksword/api-generic.txt` (generic signature differences) at review.

## 11. K1 outcome
- 18 batches (B1-B18, Tasks 3-23) converted every JSword Java source except the Lucene packages
  (`index.lucene`, `index.lucene.analysis`: 35 files, left for a later phase).
- Consumer (`:app`) edits forced by Kotlin syntax: 15 and-bible commits touching `app/src`
  (`git log --oneline dd03ff696..HEAD -- app/src`), 142 files, +422/-418 lines; mostly `!!` on honest
  nullable returns, `getX()`/`setX()` for overridden getters, `Companion` constant imports.
- API parity allowlist: 173 entries (`grep -vc '^#' jsword/oracle/api-allowlist.txt`), all Kotlin
  artefacts (Cloneable direct supertype, protected -> public constructors of `internal` classes,
  `INSTANCE` fields, removed `equals`/`hashCode` redeclarations, `readObject` finality).
- Gate numbers at M4: `jvmTest` 279 classes / 3411 tests / 0 failures; `parityTest` 5 classes / 27 tests /
  0 failures; `:app` suite 561 classes / 3808 tests / 0 failures.
- Bugs noticed and left as is are listed in the commit bodies:
  `git -C jsword log b9490121..HEAD --grep="left as is"`.
- Lessons that became rules: ruling R7 (non-null returns where every path provably returns non-null,
  parameters conservative), R8 (`:app` overrides decide parameter types), caller-frame counting
  (`object` + `@JvmStatic` for `CallContext`/`ResourceUtil`/`CWClassLoader`), `getOsis` must return a
  mutable list, differential parity probes for hand-rewritten loops.

# Phase 2: commonization (K2)

Rules for moving converted JSword code from `jvmMain` to `commonMain`. Each row: the rule, the replacement, the
trap, and where the helper lives (all under `src/commonMain/kotlin`, package `org.crosswire.common.util` unless
stated). Phase-1 rules (§1-§10) still apply.

## 12. Resources
- Rule: `KswordResources.bytes(path)` / `.text(path)` read embedded files. `KswordResources` is generated at build
  time (see `build.gradle.kts`) from `src/embeddedResources/`. Bundles: `ResourceBundles.get(base, locale, charset)`
  (`ResourceBundles.kt`, returns `KBundle?`).
- Trap: keep the old charset. `ResourceBundle.getBundle` -> `PropertiesCharset.UTF8_THEN_LATIN1` (the default
  argument); `Properties.load(InputStream)` -> `PropertiesCharset.LATIN1` (`PropertiesParser.kt`).
- No `CallContext` / `CWClassLoader` in common code.

## 13. Messages
- Rule: `MsgBase(bundleName)`. `java.text.MessageFormat` ->
  `MessageFormatLite.format(pattern, locale, args)`. `NumberFormat`/`DecimalFormat` -> `NumberFormats.general`,
  `.integer`, `.pattern(value, pattern, locale)` (`expect object`, actuals per platform).
- Trap: where Java used the default formatting locale, pass `KswordLocale.default()`.

## 14. Locale
- Rule: `KLocale` replaces `java.util.Locale` in common code. `KswordLocale.current()` replaces
  `LocaleProviderManager.getLocale()`; `KswordLocale.default()` replaces `Locale.getDefault()` (`KLocale.kt`).
- Trap: legacy language codes (`iw`, `in`, `ji`) are normalized by `KLocale.normalizeLanguage` /
  `legacyLanguage`; do not compare raw JDK codes.

## 15. Logging
- Rule: `org.crosswire.common.util.LoggerFactory.getLogger(X::class)` (defined in `Logger.kt`); slf4j `{}` placeholders.
- Trap: no `org.slf4j` imports in common code. The JVM actual keeps the same logger names.

## 16. Plugins
- Rule: consumers look implementations up through `PluginRegistry.entries/entry/default(type)`. A class that
  moves to common is registered with `builder.register(type, name, implClass) { ... }`.
- Where: there is no `registerCommonPlugins` yet. The only hook today is `expect fun registerPlatformPlugins(builder)`
  (`PluginRegistry.kt`, actuals in jvmMain/iosMain). Add `registerCommonPlugins` (called from the registry's
  `lazy`) when the first common class needs registering.
- Trap: registration order is the order of the old `.plugin` files; `default` is an ordinary name.

## 17. Files
- Rule: `okio.Path` + `KFiles` (`exists/isFile/readBytes/write/list/delete/move`, each taking an optional
  `FileSystem`). `java.io.File` / `URI` stay at the JVM API boundary until their class moves.
  `RandomAccessFile` -> `RandomAccessSource.open(path)` (`read(offset, size)`, `size()`).
- Trap: `KFiles` keeps `java.io.File`'s boolean/-1 return conventions; do not turn them into exceptions.

## 18. zlib
- Rule: `org.crosswire.common.compress.Zlib.inflate(input, expectedSize)` / `.deflate(input)`. gzip, bzip2, xz and
  tar are decided in K4.

## 19. `synchronized` / `@Synchronized`
- Rule: `kotlinx.atomicfu.locks.SynchronizedObject` + `synchronized(lock) { }` (import
  `kotlinx.atomicfu.locks.synchronized`). A `@Synchronized` method becomes a `synchronized(lock)` body on a private
  lock object.
- `SynchronizedObject` is a real lock on both JVM (a monitor) and native, not a no-op.
- Trap: it no longer locks on `this`; check that no caller synchronizes on the instance. `static synchronized`
  (`synchronized(Outer::class.java)`) becomes a lock object in the companion.
- Fields read without the lock and written under it: `kotlin.concurrent.Volatile`.

## 20. `java.util.concurrent`
- Rule: `CopyOnWriteArrayList` -> `CopyOnWriteList` (`CopyOnWriteList.kt`, K2 F5). It is a `MutableList`: iterators and
  `subList` are snapshots, iterator `remove/set/add` and `subList` mutators throw `UnsupportedOperationException`
  (as the JDK class; `subList` there is a view that throws on concurrent change, here a read-only copy);
  `removeAll { }` is one atomic step. The six existing `CopyOnWriteArrayList` uses move with their classes.
- Executors/futures stay JVM until `common.progress` moves (K4).

## 21. Regex
- Rule: `kotlin.text.Regex`. Every moved pattern gets a test that runs in `commonTest`.
- Trap: the native engine differs: lookbehind, `\p{..}` classes; `Pattern.LITERAL` -> `Regex.escape`.

## 22. Dates
- Rule: kotlinx-datetime, added when first needed. `0.7.1` is the target version, to be verified when it is first
  added (the build does not pin it yet).

## 23. Java serialization
- Rule: stays JVM. `Serializable` is dropped when a class moves to common unless a JVM caller is found.
- K3 (2026-10-10): `Converters.keyToStr/strToKey` were dead code (no `Key` column; never called from generated Room code) and were deleted; the reference model has no Java serialization.

## 24. Exceptions
- Rule: common code throws `okio.IOException` subtypes where Java threw `IOException`. `MissingResourceException`
  and other `java.util` exceptions stay at the JVM boundary.

## 25. Generated resources
- Rule: a new resource file goes to `src/embeddedResources/`; nothing new on the JVM classpath.

## 26. K3 replacements
- `Cloneable` / `super.clone()` -> protected copy constructors and explicit `Key.clone()`; copy exactly the old shallow/deep fields and fail loudly for an open subclass without its own override; JVM-only inheritors retain `Cloneable` and `super<Cloneable>.clone()`.
- `TreeSet` / `SortedSet` -> `SortedArraySet` (`SortedArraySet.kt`); keep sorted iteration and iterator removal, differential-test against the JDK, and report timings honestly: the early warmed ~1.51x Ranged remove slowdown was not the final M2 result (M2 medians 817.5626065 ms old / 2576.456209 ms new, 3.151x; later samples 2.07-2.67x). See the appended Task 14 review-fix evidence; old measurements remain historical, not closure evidence.
- `BitSet` -> `KBitSet` (`KBitSet.kt`, `copy()` replaces clone); preserve bit operations and passage equality/hashCode, including capacity-independent logical equality.
- Java mapped collections / `Iterable` / `Comparable` / `Object` -> Kotlin collections / types; preserve JVM types and behavior.
- `Collections.*` / `Arrays.*` / `System.arraycopy` -> Kotlin stdlib / wrappers (`copyOf`, `copyInto`); an unmodifiable live view must remain a view, not become a snapshot.
- `Character.isLetter/isDigit/isUpperCase/toUpperCase` -> `Char.isLetter()/isDigit()/isUpperCase()/uppercaseChar()`; preserve character semantics.
- `Character.isWhitespace` -> `Char.isJavaWhitespace()` (`CharCompat.kt`); exclude U+00A0, U+2007 and U+202F as Java does, unlike Kotlin whitespace.
- `System.getProperty("line.separator")` -> `"\n"` (`StringUtil`); matches Linux/Android's existing output.
- Locale-sensitive lower/upper case -> `String.lowercase/uppercase(KLocale)` (`CaseMapping.kt`, platform actuals); preserve Turkish/Azeri dotted i and Greek final sigma.
- `NoSuchElementException` / `ClassCastException` -> Kotlin exceptions; JVM typealiases preserve catch behavior.
- `Reader` / `Writer` / buffered variants -> `String` input / `Appendable` output (`Passage`, `PassageKeyFactory`, `StringUtil`); preserve description round trips (D3).
- `PrintStream` -> `Appendable` (`Versification`, `VersificationToKJVMapper`); preserve dev-tool output text (D3).
- `EventObject` / `EventListener` -> plain `PassageEvent` / `PassageListener`; keep Java `getSource()` and non-null source (null is rejected) and nullable start/end bounds for empty events (D3).
- `javaClass` / `X::class.java` -> KClass comparisons / `this::class.simpleName`; preserve pinned class-name output and `MsgBase` bundle names.
- `@JvmStatic` / `@JvmField` / `@JvmOverloads` / `@Throws` stay in common with explicit imports; `@Throws` uses `okio.IOException::class`.

### K3 Task 14 review-fix evidence (2026-10-10)

The early ~1.51x figure above was superseded by the M2 3.151x finding; neither is erased.
At frozen b63d4745, the unchanged faithful NT probe (two warmups, eight alternating-order samples,
4121 remaining verses) measured Ranged remove medians 809.240615 ms old / 1641.8442145 ms new (2.029x).
Temporary diagnostic instrumentation measured 87,794,175 new snapshot-copy comparisons versus zero
for the old empty TreeSet sorted-source bulk build; normalization had 87,839,557 new comparisons
versus 145,430,524 old. Instrumented stage timings are attribution evidence, not uninstrumented
performance ratios. A second frozen allocation probe measured 248,690,664 new versus 681,716,472
old allocated bytes in round 7, with 1/2 collections and 1/5 ms GC respectively: allocation volume
alone does not explain the slowdown.

The scoped fix preserves sorted order and avoids re-comparing an already sorted source only when
an empty SortedArraySet receives another SortedArraySet with an equal comparator (TreeSet's
analogous bulk-build condition). Two unchanged uninstrumented post-fix probe runs measured
929.7893575 / 1287.543108 ms (1.385x) and 811.0789035 / 1078.655247 ms (1.330x).
This is not a Native timing claim or a guarantee for arbitrary workloads. Distinct remove still
measured 2.182x and 2.139x at roughly 0.5/1.1 ms; retain this residual finding for review.
Full raw samples and XML are in the controller's Task 14 fix workspace; no earlier history was rewritten.
