# K4 seams

| Seam | commonMain | jvmMain | iosMain | Removed by |
|---|---|---|---|---|
| `org.crosswire.common.kdom.Content`, `Document` | `expect abstract class Content` / `expect open class Document`, each declaring `final override fun equals(other: Any?): Boolean` and `final override fun hashCode(): Int` | `actual typealias` to `org.jdom2.Content` / `org.jdom2.Document` | opaque classes, `internal constructor()`, identity equality | K5 (real kdom) |
| `org.crosswire.common.util.KUri` | `expect class KUri` (members: Task 2 inventory) | `actual typealias KUri = java.net.URI` | own RFC 3986 subset on `KUriParts` | stays |
| `org.crosswire.jsword.book.Book`, `BookDriver` (`BookSeams.kt`), `org.crosswire.jsword.book.filter.SourceFilter` (`SourceFilterSeams.kt`) | `expect interface` with only the members common code calls | existing interface becomes `actual interface` with all members and supertypes unchanged | minimal `actual interface` | K5b (facade move) |
| facade hooks (`SwordFacadeHooks.kt`, package `org.crosswire.jsword.book.sword`) | `internal expect fun swordMetaDataToOsis(md: SwordBookMetaData): Document`, `internal expect fun sourceFilterFor(sourceType: String?): SourceFilter`, `internal expect fun createSwordBook(type: BookType, sbmd: SwordBookMetaData, backend: Backend<*>): Book`, `internal expect fun registerSwordDriver()` | today's code | throw `UnsupportedOperationException("K5b: <what>")`; `registerSwordDriver` is a no-op | K5b |
| `CWProject` class-loader hook (`ResourceHomeHook.kt`, package `org.crosswire.common.util`) | `internal expect fun setResourceHome(dirs: Array<KUri>)` | `CWClassLoader.setHome(dirs)` | no-op (resources are embedded, K2) | stays |
| `DevotionDateFormat` (package `org.crosswire.jsword.book.sword`) | `expect object DevotionDateFormat { fun format(month: Int, day: Int): String; fun parseMonthDay(text: String): Pair<Int, Int>? }` | today's `DateFormatter.getDateInstance()` + `GregorianCalendar` code from `RawLDBackend` | `NSDateFormatter`, medium date style, lenient | stays |
| `OpenFileStateManager` scheduler, `Job` ticker (`Scheduling.kt`, package `org.crosswire.common.util`) | `expect fun scheduleRepeating(periodMs: Long, task: () -> Unit): Cancellable` + `fun interface Cancellable { fun cancel() }` | daemon `ScheduledExecutorService` (one per call, as today) | `dispatch_source` timer on a global queue | stays |
| BZIP2/XZ (`CompressorPlatform.kt`, package `org.crosswire.common.compress`) | `internal expect fun platformUncompress(type: CompressorType, input: ByteArray, expectedLength: Int): ByteArray`, `internal expect fun platformCompress(type: CompressorType, input: ByteArray): ByteArray` | commons-compress (today's `BZip2`/`XZ` bodies) | throw `BookException` "unsupported compression on this platform: <type>" | stays (D4) |
| HTTP (`HttpTransport.kt`, package `org.crosswire.common.util`) | `interface HttpTransport`, `class HttpHead`, `interface HttpBody`, `class TransportCancelledException : okio.IOException` | `UrlConnectionHttpTransport` (today's `HttpsURLConnection` code) | none in K4 (track I); `WebResource.transport` must be set by the host | stays (D3) |

## Task status

Task 2 introduces Content, Document and KUri. Content/Document retain final identity equality on both platforms; native placeholders have no public constructor.

Task 3 introduces Book/BookDriver/SourceFilter forward declarations. Book currently extends only Comparable<Book> in common/iOS; add Activatable to the expect and iOS actual in Task 16 when Activatable moves. JVM retains Activatable and all existing members.

Task 3 behavior hooks, ResourceHomeHook and DevotionDateFormat are deliberately plain JVM declarations: SwordBookMetaData, BookType, Backend and CWProject are not visible to commonMain yet. In Task 16 add common expects and actual modifiers to the JVM declarations, then add the iOS bodies below. No prematurely uncompilable common/iOS behavior files are created here. The ResourceHomeHook signature must be `Array<KUri?>`, not the table's original `Array<KUri>`: HEAD's projectResourceDirs and CWClassLoader.setHome both accept nullable elements.

The only API parity allowance is BookType.getBook becoming protected open instead of protected abstract (K4 seam). Custom BookType subclasses can still override getBook. Built-ins retain the thirteen mappings characterized at d2759f58; HREFCom and RawFiles both create SwordBook. RawLD, RawLD4 and zLD each create SwordDailyDevotion only for DAILY_DEVOTIONS, otherwise SwordDictionary.

### Task 16 iOS implementation handoff

```kotlin
// SwordFacadeHooks.ios.kt, package org.crosswire.jsword.book.sword
internal actual fun swordMetaDataToOsis(md: SwordBookMetaData): Document =
    throw UnsupportedOperationException("K5b: SwordBookMetaData.toOSIS needs the OSIS facade")
internal actual fun sourceFilterFor(sourceType: String?): SourceFilter =
    throw UnsupportedOperationException("K5b: source filters")
internal actual fun createSwordBook(type: BookType, sbmd: SwordBookMetaData, backend: Backend<*>): Book =
    throw UnsupportedOperationException("K5b: book facade")
internal actual fun registerSwordDriver() {
    // No JVM SwordBookDriver registry on iOS until K5b.
}

// ResourceHomeHook.ios.kt, package org.crosswire.common.util
internal actual fun setResourceHome(dirs: Array<KUri?>) {
    // Resources are embedded; no class-loader home on iOS.
}
```

DevotionDateFormat's JVM body keeps DateFormatter's default-locale medium style, current year, leniency and exceptions verbatim. Formatting February 29 in a non-leap current year normalizes to March 1; tests compare parsed normalized reference results, not an impossible February 29 identity. DateFormatter currently asserts on its reflective parse failure when JVM assertions are enabled, and falls back to the current date when disabled; this is not changed to a null result or ParseException.

The Task 16 iOS body uses a fixed leap year 2000 for formatting (a deliberate platform difference), NSDateFormatterMediumStyle and lenient parsing. Use the Foundation imports and ExperimentalForeignApi opt-in as needed at that move's compile gate:

```kotlin
// DevotionDateFormat.ios.kt, package org.crosswire.jsword.book.sword
actual object DevotionDateFormat {
    actual fun format(month: Int, day: Int): String {
        val calendar = NSCalendar.currentCalendar
        val components = NSDateComponents().apply {
            year = 2000
            this.month = month.toLong()
            this.day = day.toLong()
        }
        val formatter = NSDateFormatter().apply {
            dateStyle = NSDateFormatterMediumStyle
            lenient = true
        }
        return formatter.stringFromDate(calendar.dateFromComponents(components)!!)
    }

    actual fun parseMonthDay(text: String): Pair<Int, Int>? {
        val formatter = NSDateFormatter().apply {
            dateStyle = NSDateFormatterMediumStyle
            lenient = true
        }
        val date = formatter.dateFromString(text) ?: return null
        val components = NSCalendar.currentCalendar.components(
            NSCalendarUnitMonth or NSCalendarUnitDay, date
        )
        return components.month.toInt() to components.day.toInt()
    }
}
```

Other rows remain planned seams for later tasks.

## KUri inventory

Inventory at jsword 30882a72 over the 82 move-set data paths (plus one header line). The plan matcher misses bare `uri` and `orig`; a supplementary identifier matcher yields the following counts, excluding comments.

| Member | Code occurrences | Seam treatment |
|---|---:|---|
| path | 19 | getPath() |
| scheme | 16 | getScheme() |
| toURL() | 10 | Tasks 8/14 JVM split or okio Path, not a KUri member |
| toString() | 6 | toString() |
| userInfo | 2 | getUserInfo() |
| host | 2 | getHost() |
| port | 2 | getPort() |
| query | 1 | getQuery() |
| fragment | 1 | getFragment() |

Comments add three toURL mentions (13 total). Unqualified receiver `path` in CWProject and `NetUtil.lengthenURI(...).path` are outside identifier counts but require no additional seam member. Direct constructors are URI(String) (NetUtil lines 147/940) and seven components (425/461); the four-component overload remains part of the required seam and is tested. getRawPath(), isAbsolute(), equals() and hashCode() are also declared for construction/behavior verification. No resolve/normalize/relativize call appeared in the move set. Common code must use explicit getter functions. Controller ruling: the approved Task 2 contract is inventory-limited and does not include resolve; the incidental resolve mention in spec section 4.2 will be aligned with this contract in Task 18, not implemented as an unrequested API expansion here.

## Native URI behavior

KUriModel is the common implementation core exercised against java.net.URI on JVM and delegated to by the iOS actual. Parsed text is retained, with UTF-8 percent decoding for getters; opaque URIs have null hierarchical components. Component builders quote percent signs and illegal characters separately for path, user info, query and fragment. Non-ASCII letters remain literal. Equality folds scheme/server-host case and percent-escape hex case, distinguishes opaque/hierarchical forms and retains raw component semantics (no path normalization). Hashing uses the same canonical components. Invalid server names in parsed input can remain registry authorities with a null host; component constructors require a valid server authority. JVM actuals remain typealiases, so existing Java-facing URI/JDOM signatures are unchanged.

### Task 6 util plain JVM boundaries (Task 16 expects deferred)

`ResourceHomeHook.jvm.kt` now also provides `systemProperty(String?): String?`,
`platformOsName(): String?`, `platformDisplayLanguage(String): String`,
`pathUri(Path): KUri`, and `setUriCachePath(Path?)`. Preserve JDK default DISPLAY
locale for the language fallback; it is not the JSword locale. iOS supplies host
home/null system properties, iOS OS name, and path/cache equivalents at the move.

`PropertyMapJvm` owns stream load/store. `legacyPropertyEntries` retains JDK
Properties key enumeration order, and `legacyPropertyBytes` retains escaping,
comments and the generated timestamp. These plain JVM helpers need platform
implementations at Task 16, not a new common parser with changed ordering.
`IniSectionJvm` owns File/InputStream/Writer overloads; IniSection consumes
Path/ByteArray/Appendable. Necessary JVM callers and BibleApplication's resource
stream call were qualified mechanically now rather than waiting for Task 14.

### Task 6 util review fixes

`IniSectionJvm.kt` also supplies plain JVM `platformIniLineSeparator(): String`,
backed by the cached `System.lineSeparator()` value (as PrintWriter uses). The
internal `iniLineSeparator` platform value can be forced in characterization;
Task 16 adds the expect/actual boundary, with iOS LF. The common serializer uses
this for record endings only; embedded continuation LF remains unchanged.

`unmodifiableIniKeys(Set<String>): Collection<String>` and
`unmodifiableIniValues(Collection<String>): Collection<String>` are plain JVM
helpers using Collections.unmodifiableSet/Collection. This preserves live views
and the Java removeIf default-method contract: always UOE, without evaluating a
predicate, even for an empty view or no match. Task 16 needs common declarations
and an iOS live read-only view actual. The former common wrappers were removed
because Kotlin collection implementations do not override Java removeIf.

### Task 6 final File boundaries

`MetaDataLocator` owns nullable Path locations; `MetaDataLocatorJvm` exposes File
getters. `validatedFileUriPath(KUri): Path` in ResourceHomeHook.jvm.kt deliberately
uses File(URI), preserving rejection of non-file, relative, authority, query and
fragment URIs. Task 16 adds the common declaration and equivalent iOS validation.
Directory creation failure remains ignored and the computed location is returned.

`SwordBookMetaData` accepts Path and exposes Path? configFile;
`SwordBookMetaDataJvm.create(File, KUri?)` and `getConfigFile(metadata)` are the File
factory/bridge. Its Writer normalize entry point moved intact to that holder.
IniSection Path and ByteArray parsing already uses KCharset, including the
UTF-8 first parse, filtered Encoding lookup, clear and CP1252 retry.

`SwordBookPathJvm` owns the seven File methods, delegating to the Path core and
checking an empty download File before conversion (okio maps empty to dot).
Empty File book-list requests also retain the old null result.

Plain JVM helpers in `SwordStorageHooks.jvm.kt` need common expect declarations
and actual bodies in Task 16: `metadataChildPath(Path?, String?): Path` preserves
File(parent, child), including null parent, absolute child and null-child NPE;
`metadataDefaultsView(Map<String,String>): Map<String,String>` retains Java
unmodifiable-map behavior; `storageCanRead/Write(Path): Boolean` preserve access
checks; `storageSeparator(): String`; `storageMkdirs(Path): Boolean` preserves
mkdirs failure; `storageListNames(Path): List<String>?` preserves native File.list
order (okio sorts). iOS uses its filesystem/access/child equivalents; do not
silently substitute sorted listing on the JVM. Facade registration hook remains
plain JVM until Task 16, with assignment before driver registration unchanged.

Mechanical real JVM/app File caller qualifications are advanced from Task 14:
SwordBookDriver, AbstractSwordInstaller, the dual-compiled CorpusProbe (reflective
old/new selection), File metadata tests, BackupControl, InstallDeps,
SwordEnvironmentInitialisation and the existing app download/backup tests.
Null-only configFile checks in TtfBook, BackgroundImageBook and installer tests
remain on the core and require no File conversion.

### Task 6 File-boundary review fix 1: lexical location

`StorageLocation` is an internal minimal String-backed value. Its spelling is
authoritative for legacy JVM File identity, names, child joins, discovery,
deduplication, migration and IO. Path getters/constructor APIs are projections
for common Path callers only: okio cannot represent Unix literal backslashes
or preserve dot spelling. JVM File holders never round-trip via those projections.
No normalized-key cache or global Path-to-File map exists.

`SwordStorageHooks.jvm.kt` now supplies lexical `storageChild`, `storageName`,
`storageParent`, `storageRead`, `storageExists`, `storageDirectory`,
`storageCanRead/Write`, `storageMkdirs`, `storageListNames`, `storageMove`,
`storageSave`, and `storageUriLocation` (File(URI) validation retained).
The earlier Path-based metadataChildPath/access/list helpers are superseded.
Task 16 must declare these hooks in common and implement iOS native literal-path
handling. `metadataDefaultsView` and `storageSeparator` remain as inventoried.
The internal lexical metadata constructor is JVM public and has one exact K4
seam allowance; locator Path projection getters are concrete (exact allowances
updated). No File signature is reintroduced into the common-ready classes.
