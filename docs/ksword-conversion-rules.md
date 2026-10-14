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

## 27. K4 replacements

These rules extend the commonization replacements above. Keep the JVM implementation's observable behavior and API unless a narrowly documented platform limitation or approved K4 split says otherwise. `jsword/docs/k4-seams.md` is the detailed implementation inventory; this section is the portable conversion checklist.

| JVM implementation or contract | K4 replacement/rule | Traps and required behavior |
|---|---|---|
| `Charset`, `String` encoding/decoding | Use common `KCharset` for UTF-8, ISO-8859-1 and Windows-1252 (CP1252), by the existing supported charset names. | Preserve JDK replacement behavior for malformed input and undefined CP1252 bytes; differential JVM coverage compares all 256 bytes for Latin-1/CP1252 (including the five undefined CP1252 bytes) and malformed UTF-8 (truncation, overlongs, surrogates, bytes 0xF5-0xFF and buffer-end cuts); common tests pin the same expectations. Do not substitute Kotlin UTF-8 defaults or platform default charset where the old code specified a charset. Unknown charset names retain the characterized exception path. |
| `Charset.defaultCharset()` for cipher keys (`AbstractBackend`, `RawLDBackend`) | Encode cipher keys as explicit UTF-8. | Keep this explicit default distinct from `bookCharset`; a non-ASCII cipher-key test pins the resulting bytes. |
| `RandomAccessFile`, `File`, file streams | Use `RandomAccessSource`, `okio.Path`/`KFiles` for common operations and narrow lexical storage hooks / JVM holders for `File`-specific behavior. | Keep boolean/`-1` return conventions, read and EOF behavior, close ownership, and literal `StorageLocation` spelling authoritative for JVM File identity. A `Path` projection is not an interchangeable File identity; Unix backslashes and dot spelling can matter. Preserve state-pool reuse after return, close after idle timeout, and exclusive borrowing under concurrent JVM borrow/return. |
| `java.net.URI` | Use the inventory-limited `KUri` contract. JVM remains a `java.net.URI` typealias; iOS uses the common RFC 3986 subset implementation. Call explicit getter functions in common code. | Do not add `resolve`, `normalize` or `relativize` absent an actual common caller and reviewed contract update. Preserve raw component/equality semantics; `toURL()` is a JVM boundary, not a KUri member. |
| `TreeNode.clone`, backend `Object.clone`, `Cloneable` | Use explicit portable copy hooks for known common types, with JVM clone-base actuals where genuine JVM shallow-clone semantics are required. | Preserve exactly shallow/deep field sharing, unknown JVM subclass fields, runtime class and JVM `Cloneable` contract. TreeNode's private `userData` backing array remains shallow-aliased as in the old clone while public getters/setters retain defensive copies. Unknown Native subclasses must implement the portable hook or cloning throws `UnsupportedOperationException`; do not claim arbitrary Native clone equivalence. |
| `Serializable`, `ObjectStreamClass`, serialization hooks | Retain JVM serialization only where an actual JVM caller/oracle requires it; use the TreeNode JVM marker bridge and exact UID/field contracts. | Do not make the Native clone base Serializable or add a superclass descriptor. Verify exact old/new serialized bytes and descriptors separately; API parity alone does not prove serialization fidelity. |
| Java `Properties` and `LinkedHashMap` inheritance | Keep `PropertyMap` on a platform superclass seam: JVM actual is the genuine `java.util.LinkedHashMap`; Native uses an ordered backing map. Keep JDK stream load/store in bounded JVM/platform holders. | JVM preservation claims are scoped to Java Properties stream semantics and the observed JDK17 sequential non-null String-put ordering model: load includes parsed puts and duplicate updates; store uses a fresh Properties presized by putAll. The order model does not cover concurrency, removal, defaults, arbitrary Map operations or every JDK/Android version. JVM comments keep JDK escaping and generated timestamp. Native comment timestamps use `NSDateFormatter(en_US_POSIX, defaultTimeZone, "EEE MMM dd HH:mm:ss zzz yyyy")`; timezone abbreviations/DST remain unverified. Native store opens/truncates before `putAll`, writes then flushes before close; `IOUtil.close` suppresses cleanup-close IOException. Native `storageSave` closes an owned fd after charset-construction failure, unlike the legacy JVM leak. These are bounded contracts/differences; do not claim general Native or map-order equivalence from compilation. |
| `File`, `FileOutputStream`, `OutputStreamWriter`, `RandomAccessFile` | Use lexical storage and stream hooks for File-facing behavior; use common `Path`/`KFiles` only where Path is the intended contract. | Preserve default filename charset, formatter locale, record separators, write/truncate/create behavior, short reads, errors and resource ownership. JVM `RawFile` filename decoding uses `Charset.defaultCharset()` over exactly seven bytes; the iOS platform default is UTF-8 and its implementation explicitly decodes those seven bytes. JVM raw-file append opens its append stream before charset encoding; Native likewise opens before encoding. Native property-store opens/truncates before copying properties, explicitly flushes before close, and cleanup `IOUtil.close` suppresses a close IOException. Native storage-save closes its owned fd after charset-construction failure, unlike the JVM legacy path; state this bounded ownership difference, not universal equivalence. |
| `System.getProperty("user.home")`, `os.name` | Use host-provided home paths and the platform OS-name actual; retain explicit `CWProject.setHome(...)` ownership. | Preserve JVM system-property behavior and do not invent a Native class-loader resource home. iOS resources are embedded, so `setResourceHome` is a no-op. |
| `DateFormatter`, `GregorianCalendar`, locale formatters | Route platform-specific date/number behavior through the narrow formatter actuals. | JVM keeps `DateFormatter.getDateInstance()`/`GregorianCalendar`, default locale, leniency, current-year normalization and exception behavior. iOS uses `NSDateFormatter` medium style for devotion text with year 2000 for formatting; parsing uses current Foundation calendar and is lenient. Backend/raw-file numeric formats use `NSNumberFormatter` with `NSLocale.currentLocale`, no grouping, and the specified minimum integer width. These Foundation semantics compiled but were not runtime-verified on macOS; do not claim locale, timezone or parity results. |
| Executors, `ScheduledExecutorService`, `Timer` | Use the separate open-file scheduler and Job ticker interfaces; do not merge their policies. | Preserve fixed-delay/cancellation behavior for open-file state and the Job ticker's immediate tick, wall-clock START-relative scheduling, non-overlap, cancellation, non-daemon JVM lifetime and failure termination policy. Linux compilation is not Native runtime evidence. |
| `HttpsURLConnection`, `URLConnection`, `WebResource` | Use `HttpTransport`; JVM retains the URLConnection implementation. iOS has no default transport until the track-I host supplies one. | Characterize and preserve redirects, 404-before-output behavior, timeout behavior, absent `Content-Length`, `Last-Modified`, mid-body cancellation and proxy-selector behavior. Keep lazy GET order: read `contentLength` only when a meter exists (HEAD fallback only for zero), update meter, assign/check status, `prepareRead()`, then open destination and stream. Preserve input-before-output close/error order, cancellation and lexical sink output. Constructor timeout/proxy arguments remain ignored as characterized. The known `http:` to `HttpsURLConnection` `ClassCastException` remains unfixed; global proxy-selector tests do not prove CONNECT tunneling. D3 host GET/HEAD is not an output port. `:app` callers continue to compile unchanged. |
| commons-compress `TarArchiveInputStream`, `GzipCompressorInputStream` | Use the common tar reader (ustar, GNU long names, PAX paths) with the common gzip header/parser path. | Differential-check against commons-compress on saved repository `mods.d.tar.gz` fixtures and crafted archives. Reject `../` and absolute extraction paths; traversal rejection is stricter than the legacy extractor. Preserve installer-compatible short reads, padding boundaries, partial cache effects and lazy error timing. |
| commons-compress ZIP readers/writers and `IOUtil.unpackZip` | Use the common local-header ZIP reader (stored/deflated/data descriptors), stored cache writer and archive extraction hooks. | Read old commons-compress deflated cache entries and new stored entries; unpack module ZIP fixtures to identical trees. Preserve local-versus-central order, duplicate names, declared-size behavior, streaming/close/error timing and partial installation effects. Reject zip-slip traversal. |
| `BZIP2`, `XZ`, `LZSS` (`CompressorType`) | Keep JVM BZIP2/XZ on commons-compress; Native BZIP2/XZ report the typed D4 unsupported `BookException`. LZSS uses the pure common byte-array implementation. | Native unsupported BZIP2/XZ does not make GZIP unsupported or waive JVM codec coverage. LZSS is round-tripped against legacy/test-module data and generated inputs; retain its byte-level/truncated-token behavior. |
| `NetUtil`, `IOUtil`, `FileUtil` JVM-only members | Keep exact Java-facing File/URL members in approved JVM holders or bounded platform hooks; qualify actual callers mechanically. | Do not add speculative Path overloads, widen checked exceptions, round-trip lexical File identity through Path, or introduce a global Path-to-File registry. Every API removal/split must remain narrowly allowlisted and justified. |
| `java.io` input/output wrappers, `PrintStream` | Use common `String`/`Appendable` APIs for portable text paths, and lexical/platform stream seams for storage/archive operations. | Preserve old host newline where output must match `PrintStream`/`PrintWriter`; iOS INI record separators are LF. Preserve buffering, one-read/short-read, output timing and close semantics where observable. A common abstraction must not silently turn an old partial-write/error behavior into eager validation. |

### K4 seam rules

- The common `Content` and `Document` declarations each explicitly declare `final override fun equals(other: Any?): Boolean` and `final override fun hashCode(): Int`. JDOM's final methods require these declarations for compatible expect/actual scope; preserve final identity behavior rather than removing or loosening it.
- An `actual interface` may retain additional members and supertypes beyond its `expect`. Keep the complete JVM `Book`, `BookDriver` and `SourceFilter` actual API and supertypes unchanged; the common expect is only the subset common code needs. Do not prune the JVM interface to match the forward declaration.
- Name platform seam holders consistently with the platform operation, using the `*Seams.kt` naming pattern for declarations such as `BookSeams.kt`, `SourceFilterSeams.kt` and `PropertyMapSuperclassSeams.kt`. Keep platform implementations in their source-set-specific files.
- KUri Java getters are explicit functions in common (`getPath()`, `getScheme()`, etc.), not Kotlin properties. Its exact constructors/functions are inventoried in `jsword/docs/k4-seams.md`; no `resolve` property or function is part of K4.
- Separate permanent platform operations (lexical File IO, formatting, clocks/schedulers, HTTP host access, archive/compression, JVM cloning/serialization and Java ABI bridges) from the temporary facade declarations/hooks. K5 removes the JDOM `Content`/`Document` placeholders; K5b removes the `Book`/`BookDriver`/`SourceFilter` forward declarations and `SwordFacadeHooks` after K5's `kdom` is available.
- Record evidence at the scope it proves: JVM tests establish JVM behavior; Native source compilation establishes source-set/type compatibility only. It does not prove macOS link/runtime, filesystem semantics, locale behavior, GCD lifecycle, inflater behavior, fixture provisioning or emulator behavior.
