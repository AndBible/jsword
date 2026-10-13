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

### Task 6 File-boundary review fix 2

StorageLocation is now inline in the already-listed SwordBookPath.kt, so it moves
with that file in Task 16; no unlisted JVM dependency or move-count increase.
New plain JVM hooks `storageFileSpelling(String): String`,
`storageFileEquals(StorageLocation, StorageLocation): Boolean` and
`storageFileHash(StorageLocation): Int` use File construction/equality/hash:
redundant/trailing separators collapse, dot segments and Unix backslashes remain.
Task 16 supplies native literal-path construction/equality/hash equivalents.
`storageSave` now uses FileOutputStream/OutputStreamWriter and the existing INI
Writer serializer directly, bypassing every Path conversion. Charset selection,
record newline/continuation formatting, writer flush and owned-stream closure
remain JVM behavior. File-open failures still reach metadata's IOException log.

### Task 8 lexical random access boundary

`storageOpenRandomAccess(StorageLocation, writable: Boolean): RandomAccessSource`
is a plain internal JVM hook until Task 16. It opens `RandomAccessFile(File(spelling),
"r"/"rw")` directly and owns it through an okio FileHandle; authoritative spelling
never passes through Path. Task 16 adds its common expect and an iOS literal-path
FileHandle actual, retaining writable creation, zero-filled extension, read-only
write failure and owned close. FileHandle resize/flush methods are required adapter
methods, not new caller-facing operations.

RandomAccessSource's internal cursor compatibility operations (`position`, `seek`,
`readAtCursor`, `readNext`, `readUntil`, `writeAtCursor`, `writeNext`) serve existing
TreeKeyIndex and RawFiles callers only. Cursor reads seek first (negative offsets
throw even for zero/negative sizes), shorten past EOF, and consume the delimiter.
The original public positional `read` remains unchanged, including its documented
zero-size-before-offset difference. Task 16 needs no platform hooks for this cursor
logic; only the lexical open hook is platform-specific.

`Scheduling.jvm.kt` is deliberately plain JVM now; Task 16 moves Cancellable to
common, adds the scheduleRepeating expect and actual modifiers. Its iOS actual
uses a global dispatch queue and a dispatch source timer. To preserve fixed delay
rather than fixed rate, use a one-shot timer and re-arm after each completed task:

```kotlin
// Scheduling.ios.kt (Task 16; imports/foreign opt-in verified at that gate)
actual fun scheduleRepeating(periodMs: Long, task: () -> Unit): Cancellable {
    val queue = dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u)
    val timer = dispatch_source_create(DISPATCH_SOURCE_TYPE_TIMER, 0u, 0u, queue)!!
    val lock = SynchronizedObject()
    var cancelled = false
    fun arm(delayNs: Long) {
        dispatch_source_set_timer(timer, dispatch_time(DISPATCH_TIME_NOW, delayNs),
            DISPATCH_TIME_FOREVER, 0u)
    }
    dispatch_source_set_event_handler(timer) {
        // Do not hold the cancellation lock while user code runs.
        if (synchronized(lock) { !cancelled }) {
            task()
            synchronized(lock) {
                if (!cancelled) arm(periodMs * 1_000_000L)
            }
        }
    }
    arm(0L)
    dispatch_resume(timer)
    return Cancellable {
        synchronized(lock) {
            if (!cancelled) {
                cancelled = true
                dispatch_source_cancel(timer)
            }
        }
    }
}
```

Task 16 must verify dispatch constant/unsigned types and source ownership under
Kotlin/Native. Cancellation must not interrupt a running task; no further re-arm
occurs afterwards. JVM scheduleWithFixedDelay also suppresses future runs when
the task throws; the Native actual must cancel rather than allow an exception to
escape into the C callback. This snippet is handoff design, not Native runtime
evidence.

## Task 8 state port closure

All six state implementations retain lexical `StorageLocation` values and own
`RandomAccessSource` handles opened through `storageOpenRandomAccess`, not Path.
SwordUtil's six public RAF overloads use internal cursor operations preserving
seek-before-size, delimiter inclusion, EOF shortening, null/empty writes and close
ownership. RawBackend, ZVerseBackend, RawFileBackend and TreeKeyIndex received
handle substitutions only, not Task 9 algorithm replacements.

New JVM hook `storageAbsolute(StorageLocation): String` delegates to
`File(location.spelling).absolutePath`, used only for unchanged diagnostics.
Task 16 must declare a common expect and supply Native absolute spelling against
the platform working directory without canonicalizing dot segments or backslashes.
Existing permission/existence/separator and lexical open hooks are reused.
`closeStateSource` is in listed AbstractOpenFileState.kt (standalone StateSource.kt
removed), portable Kotlin/okio suppressing IOException under the legacy IOUtil
logger. It moves with that listed file in Task 16 and needs no platform actual.

File accessors moved to `RawBackendStateJvm`, `RawLDBackendStateJvm` and
`RawFileBackendStateJvm` (StateFilesJvm.kt); the latter also adapts the incfile
setter. Holders stay JVM-only, internal location accessors stay with state code.
No app caller of these state File accessors or RAF helpers exists. RawFileBackend
is the sole production File-accessor caller. Protected File fields were removed;
source fields retain protected visibility. Exact API removals/replacements have
K4 split/stream entries in api-allowlist.txt.

### Task 8 fix-round closure

Plain JVM `currentTimeMillis(): Long` in common/util/Time.jvm.kt returns
System.currentTimeMillis. All state wall-clock calls consume it. Task 16 adds
common `expect fun currentTimeMillis(): Long` in Time.kt and actual modifiers on
Time.jvm.kt, plus Time.ios.kt returning NSDate.timeIntervalSince1970 * 1000
converted to Long (epoch milliseconds, NOT monotonic elapsed time). Tasks 11/12
may consume this same hook. All nine listed state files are now #done; no new
move-set entry was added (82 retained). JVM File holders remain JVM-only.

RandomAccessSource internal readNext now advances its cursor after each successful
read chunk, including progress before EOF/IOException. Public positional read
still does not mutate cursor. Internal readStream(ByteArray): Int is one direct
FileHandle read with no Sword record EOF logging, used only by incfile to retain
FileInputStream's single-read byte count and old diagnostic events. Both are
portable Kotlin/okio and introduce no Native platform hook.

### Task 9 backend preservation hooks (Task 16 obligations)

`BackendNumberFormat.jvm.kt` defines internal `backendZeroPad(Int, Int): String`
using a fresh JDK DecimalFormat of the requested zero width and the default FORMAT
locale. Task 16 introduces the common expect in BackendNumberFormat.kt and actual
modifiers here, plus BackendNumberFormat.ios.kt. Native must preserve localized
zero digits, minimum integer width, no grouping and locale minus sign; it must not
silently use ASCII padStart. JVM reference coverage includes Arabic, Persian,
Bengali, Thai numbering extension, zero, max Int and regex rejection/overflow.

`BackendDebug.jvm.kt` defines internal `backendDebugFailure(Throwable): Unit` using
Throwable.printStackTrace() to stderr. Task 16 introduces the common expect in
BackendDebug.kt, JVM actual modifier, and BackendDebug.ios.kt emitting the failure
and available stack trace to stderr. RawLD and ZLD debug methods catch IO and
BookException as before; Appendable failures enter that same diagnostic path.

`BackendCloneBase.jvm.kt` is a permanent platform clone base, not a temporary
inherited JVM masking workaround. Task 16 declares common expect abstract
BackendCloneBase with protected shallowBackendClone(): Any and protected open
portableBackendClone(): Any. JVM shallowBackendClone is final and directly
invokes Object.clone (javap confirms invokespecial, no virtual recursion), retaining
unknown subclasses, all extra fields, metadata identity and shallow references.
The iOS actual shallowBackendClone delegates to portableBackendClone. AbstractKeyBackend
calls that bridge; unknown host subclasses must override portableBackendClone or
get an explicit UnsupportedOperationException on Native only. Known RawLD and ZLD
copies are already supplied and tested on JVM by invoking the portable hook: base
bmd reference and ignoreReadErrors value, RawLD datasize and derived entrysize;
ZLD adds no instance fields. Task 16 runs those tests on Native after moving them
out of JDK reflection. JVM clone retains Cloneable transitively. The exact
AbstractBackend superclass/copy helper and protected copy constructors/overrides
are K4 seam API allowances. No Unsafe, reflection or module-open dependency.

`TreeNodePlatform.jvm.kt` holds internal typealias TreeNodeSerializable =
java.io.Serializable and a NON-Serializable abstract TreeNodeCloneBase : Cloneable.
Task 16 introduces common expect marker interface TreeNodeSerializable, actual
JVM typealias and empty iOS actual marker. Common expect TreeNodeCloneBase declares
protected shallowTreeClone(): Any and protected open portableTreeClone(): Any;
JVM final bridge calls Object.clone, iOS bridge delegates to portableTreeClone.
TreeNode's explicit known copy aliases the PRIVATE userData array exactly as old
shallow clone and copies offset/name/firstChild/nextSibling/parent. Its public open
getUserData/setUserData retain defensive copying; three internal Kotlin callers
use explicit methods. Unknown JVM subclass extra references remain shallow;
unknown Native subclasses must supply a portable hook. The base has an effect-free
public no-arg constructor, is not Serializable, and introduces no serialized
superclass descriptor. Frozen oracle cross-loader old-to-new/new-to-old test also
compares serialized bytes byte-exactly (six fields, same userData name/type and
serialVersionUID); same-version roundtrip and backing identity tests complement it.
Guard permits only this exact TreeNode.kt private const UID declaration and value;
changed value/name/path and adjacent Java tokens remain rejected by planted tests.
The constant is portable Kotlin; Task 16 compiler and serialization gates still
verify the actual marker/base obligation, not this heuristic exemption.
Task 16 supplies Native actuals and runs portable-copy tests; no Native runtime
claim is made here. TreeKeyIndex uses existing storageCanRead/storageAbsolute and
storageOpenRandomAccess hooks on lexical StorageLocation, never Path normalization.

`RawFileIo.jvm.kt` contains bounded lexical stream hooks: rawFileCreate(Location):
Boolean = File.createNewFile; rawFileNumber(Long, Boolean): String = default FORMAT
String.format(%07d with optional CRLF); rawFileDecodeName(ByteArray): String =
JDK default-charset seven bytes; rawFileAppend(Location, Long, String?): Unit opens
append BufferedOutputStream BEFORE named charset encoding (including legacy
UnsupportedEncodingException/file creation); rawFileWrite(Location, ByteArray):
Unit truncates buffered output; rawFileRead(Location, Logger): ByteArray uses File
length.toInt and ONE BufferedInputStream read, preserving short-read diagnostic,
empty input and FileNotFoundException logging; rawFileWriteInc(Location?, ByteArray,
Logger): Unit uses truncating FileOutputStream and catches FileNotFoundException;
rawFilePopulate(Location, Int): Unit truncates and writes count six-byte zero records.
Task 16 introduces matching common expects and JVM actuals in RawFileIo.kt/jvm.kt
and RawFileIo.ios.kt implementing the same lexical/permission/open-before-encoding,
close, locale, one-read and diagnostic semantics. Do NOT substitute storageRead's
read-all semantics or normalize paths. These hooks are internal, not protected APIs.
Legacy protected updateDataFile(Long, File) is split to public static
RawFileBackendJvm.updateDataFile(RawFileBackend, Long, File), with the one exact K4
split removal allowance. No jsword or app subclass caller exists; portable backend
calls internal rawFileAppend on state's lexical text location. Existing overloaded
setRawText/setAliasKey signatures and protected updateIndexFile are unchanged.

ZVerse debug methods consume Appendable; exact existing Task 8 stream allowances
are adjusted only for that extra argument. No app debug caller exists. Existing
backendDebugFailure handles caught source IOException; Appendable exceptions
propagate (these old methods did not catch output writes). Arraycopy becomes
copyInto on identical byte ranges. Compressor uncompress(...).toByteArray remains
unchanged for Task 10.
