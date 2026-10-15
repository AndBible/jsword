# K4 seams

| Seam | commonMain | jvmMain | iosMain | Removed by |
|---|---|---|---|---|
| `org.crosswire.common.kdom.Content`, `Document` | Removed by K5 Task 18; real `fi.sykero.kdom` types | Removed by K5 Task 18 | Removed by K5 Task 18 | K5 Task 18 |
| `XMLUtil.getDocument` validating resource loader | `internal expect fun loadValidatedResource(subject: String?): Document` (Task 20); XMLUtil moves in Task 21 | Explicit `ResourceUtil.getResourceAsStream(XMLUtil::class.java, subject + ".xml")` preserves caller-relative lookup, missing-resource identity and source/no-main-classpath behavior. Logging retains XMLUtil category and timing. Unmodified `ValidatingResourceBuilder.kt` retains validating JAXP, DTD defaults/entities, ordered augmented attributes and legacy exceptions/partial documents. | Throws explicit `UnsupportedOperationException`; no classpath/DTD validation capability claimed | JVM implementation retained |
| ThML JAXP SAX island | Removed in Task 20; namespace-unaware `XmlEventParser(false)` and kdom callbacks, existing entities -> characters -> close -> tags chain | No ThML JAXP callbacks remain; FontTag uses XMLUtil diagnostics | Source move is Task 21, no Native runtime acceptance | Task 20 |
| Filter cloning (`FilterCloneBase`) | Shallow-copy hook plus portable-copy override | Genuine Object.clone preserves unknown host subclass runtime type and fields | Known filters create portable copies; inherited known implementations reject unknown subclass slicing unless the host overrides the portable hook | Permanent platform seam |
| SAX serializer (`XmlWriter`) | Writer operations used by serializer, transformer exception marker, failure-reporting and indentation hooks | Writer and TransformerException typealiases preserve constructor/throws signatures. StringWriter remains default. Supplied writers are not flushed/closed. IOException still prints to System.err; indentation retains String bounds exception and interning. | StringBuilder-backed default writer; diagnostic prints stack trace; indentation uses common bounds exception (not JVM StringIndexOutOfBoundsException) | Permanent platform seam |
| `org.crosswire.common.util.KUri` | `expect class KUri` with the inventoried constructors and function members; see below | `actual typealias KUri = java.net.URI` | own RFC 3986 subset on `KUriParts` | stays |
| `org.crosswire.jsword.book.Book`, `BookDriver` (`BookSeams.kt`), `org.crosswire.jsword.book.filter.SourceFilter` (`SourceFilterSeams.kt`) | `expect interface` with only the members common code calls | existing interface becomes `actual interface` with all members and supertypes unchanged | minimal `actual interface` | K5b (facade move) |
| facade hooks (`SwordFacadeHooks.kt`, package `org.crosswire.jsword.book.sword`) | `internal expect fun swordMetaDataToOsis(md: SwordBookMetaData): Document`, `internal expect fun sourceFilterFor(sourceType: String?): SourceFilter`, `internal expect fun createSwordBook(type: BookType, sbmd: SwordBookMetaData, backend: Backend<*>): Book`, `internal expect fun registerSwordDriver()` | today's code | throw `UnsupportedOperationException("K5b: <what>")`; `registerSwordDriver` is a no-op | K5b |
| `CWProject` class-loader hook (`ResourceHomeHook.kt`, package `org.crosswire.common.util`) | `internal expect fun setResourceHome(dirs: Array<KUri?>)` | `CWClassLoader.setHome(dirs)` | no-op (resources are embedded, K2) | stays |
| `DevotionDateFormat` (package `org.crosswire.jsword.book.sword`) | `expect object DevotionDateFormat { fun format(month: Int, day: Int): String; fun parseMonthDay(text: String): Pair<Int, Int>? }` | today's `DateFormatter.getDateInstance()` + `GregorianCalendar` code from `RawLDBackend` | `NSDateFormatter`, medium date style, lenient | stays |
| `OpenFileStateManager` scheduler (`Scheduling.kt`, package `org.crosswire.common.util`; Job has separate Task 12 ticker below) | `expect fun scheduleRepeating(periodMs: Long, task: () -> Unit): Cancellable` + `fun interface Cancellable { fun cancel() }` | daemon `ScheduledExecutorService` (one per call, as today) | `dispatch_source` timer on a global queue | stays |
| BZIP2/XZ (`CompressorPlatform.kt`, package `org.crosswire.common.compress`) | `internal expect fun platformUncompress(type: CompressorType, input: ByteArray, expectedLength: Int): ByteArray`, `internal expect fun platformCompress(type: CompressorType, input: ByteArray): ByteArray` | commons-compress (today's `BZip2`/`XZ` bodies) | throw `BookException` "unsupported compression on this platform: <type>" | stays (D4) |
| HTTP (`HttpTransport.kt`, package `org.crosswire.common.util`) | `interface HttpTransport`, `class HttpHead`, `interface HttpBody`, `class TransportCancelledException : okio.IOException` | `UrlConnectionHttpTransport` (today's `HttpsURLConnection` code) | no default transport in K4; host must set `WebResource.transport` for HTTP(S) operations | stays (D3; iOS transport is track I) |
| platform storage and encoding hooks (`*Platform.kt`, `*Jvm.kt`, `*Seams.kt`) | `expect` declarations for lexical filesystem, charset/formatting, archive/compression and platform-specific operations | JDK-backed actuals or JVM-only holders, as inventoried below | Foundation/POSIX actuals; unsupported capabilities are typed and explicit | permanent platform seams |

## K4 implementation state

K4's 83-file move set is in `commonMain`; JVM and iOS actuals now compile against it. This is implementation/compiler closure, not the Task 19 M3 acceptance. JVM/common fixture suites passed independently, but this document does not claim M3's full application gates, timing or emulator checks, or macOS Native link/runtime verification. Linux Native compilation does not establish runtime behavior.

### Facade boundary: temporary seams

The common forward declarations and hooks deliberately keep the book facade outside K4 until K5's `kdom` work is complete:

- `Book`'s common declaration includes `Activatable` and `Comparable<Book>`; `BookDriver` exposes only `getDriverName()`, and `SourceFilter` is a marker declaration. The JVM `actual interface` declarations retain their complete existing members and supertypes, even where they exceed the `expect` declaration. Do not narrow or copy the JVM facade API into common prematurely.
- `SwordFacadeHooks` bridges OSIS conversion, source-filter lookup, book construction and driver registration. iOS throws the named `K5b` unsupported-operation errors for the first three and leaves driver registration a no-op. These are temporary facade seams, not a claim that the book/install facade works on iOS.
- K5b moves the facade after K5's `kdom` switch and removes these forward declarations and hooks. K5 removes the `Content`/`Document` placeholders when the real common kdom types replace them.

### Permanent platform seams

These boundaries remain after K5b because their behavior is platform-specific or host-owned. Task 16 implemented them; Task 17 accepted the JVM/common test migration. The iOS actuals compiled for Linux Native targets but have not had macOS link/runtime acceptance:

- **KUri** remains an expect/actual type. Its exact inventoried constructors and members are the `String`, 7-component, and 4-component constructors; getters `getScheme`, `getUserInfo`, `getHost`, `getPort`, `getPath`, `getRawPath`, `getQuery`, `getFragment`; `isAbsolute`; and `toString`, `equals`, `hashCode`. JVM remains a `java.net.URI` typealias; iOS uses the RFC 3986 subset in `KUriParts`. Common callers use getter functions, not Kotlin properties. There is no `resolve`, `normalize` or `relativize` member: the actual K4 storage callers do not use them, and the contract is intentionally inventory-limited.
- **Class-loader/resource home** keeps the JVM `CWClassLoader.setHome` behavior; iOS is a no-op because resources are embedded. `DevotionDateFormat` keeps JVM default-locale `DateFormatter`/`GregorianCalendar` behavior; the iOS medium-date formatter uses Foundation and has a documented fixed-leap-year formatting difference.
- **Scheduling and time** retain separate platform implementations. The open-file scheduler is a JVM daemon scheduled executor and an iOS dispatch-source timer. Job prediction uses its own ticker; its immediate first tick, epoch-wall-clock basis, non-overlap, cancellation and failure policy are not interchangeable with the open-file scheduler. Time is provided through platform hooks.
- **Storage and files** keep lexical `StorageLocation` spellings authoritative for JVM File identity, names, joins, discovery and IO. Path APIs are projections and must not round-trip JVM File behavior through normalized paths. Narrow `*Jvm.kt` holders preserve File/stream APIs and JDK-specific operations for callers. iOS uses lexical native filesystem helpers. The three explicitly reviewed JVM File split holders remain: `SwordBookMetaDataJvm.kt`, `SwordBookPathJvm.kt` and `state/StateFilesJvm.kt`; these are approved boundaries, not unconverted move-set leftovers. Native output through a non-file URL is a precisely typed unsupported-capability `IOException`: the D3 host transport exposes GET/HEAD only and has no output port. This is an explicit platform limitation, not non-file URL output parity; JVM `URLConnection` output behavior remains unchanged. Installed file/ZIP archive IO and supported GZIP remain supported.
- **Formatting, properties and charset** retain platform hooks where JDK behavior is observable: default FORMAT locale/number formatting, default filename charset decoding, host line separators, Java properties escaping/order/generated timestamp, and File-based serialization/open/close ordering. Native limitations or implementation differences in locale, ownership and formatting are not blanket parity claims.
- **Compression and archives** keep JVM commons-compress/JDK actuals and Native supported implementations where provided. Native BZIP2 and XZ throw the exact typed unsupported-compression `BookException` required by D4; this does not establish JVM codec behavior or justify widening unsupported status to GZIP. Archive hooks preserve lexical names, streaming/ownership and legacy error timing; Native runtime behavior still requires host verification.
- **Clone and serialization bridges** preserve genuine JVM `Object.clone()` behavior, `Cloneable` and serialization contracts while enabling portable known copies. Native known copies use portable hooks; unknown host subclasses must implement the documented portable clone hook or cloning throws an explicit unsupported-operation exception. `TreeNode`'s marker/base do not add a serialized superclass descriptor; the frozen byte-exact cross-loader oracle is separate evidence for that contract.
- **PropertyMap superclass** uses a common expect superclass, a JVM actual typealias to `java.util.LinkedHashMap`, and an iOS ordered backing implementation. The bridge preserves the actual JVM superclass and inherited map behavior; Native backing views are not asserted to have JVM runtime behavior.
- **HTTP transport** stays a host boundary. JVM uses the legacy URLConnection implementation. K4 provides no default iOS transport; the host sets the transport in track I. The K4 port does not add ktor to JSword or invent an upload/output backend.

## KUri inventory

Inventory at jsword 30882a72 over the 82 move-set data paths (plus one header line). The plan matcher misses bare `uri` and `orig`; a supplementary identifier matcher yields the following counts, excluding comments.

| Member | Code occurrences | Seam treatment |
|---|---:|---|
| path | 19 | `getPath()` |
| scheme | 16 | `getScheme()` |
| `toURL()` | 10 | Tasks 8/14 JVM split or okio Path, not a KUri member |
| `toString()` | 6 | `toString()` |
| userInfo | 2 | `getUserInfo()` |
| host | 2 | `getHost()` |
| port | 2 | `getPort()` |
| query | 1 | `getQuery()` |
| fragment | 1 | `getFragment()` |

Comments add three `toURL` mentions (13 total). Unqualified receiver `path` in CWProject and `NetUtil.lengthenURI(...).path` are outside identifier counts but require no additional seam member. Direct constructors are `URI(String)` (NetUtil lines 147/940) and seven components (425/461); the four-component overload remains part of the required seam and is tested. `getRawPath()`, `isAbsolute()`, `equals()` and `hashCode()` are also declared for construction/behavior verification. No resolve/normalize/relativize call appeared in the move set. Common code must use explicit getter functions. The approved Task 2 contract is inventory-limited and does not include `resolve`; no unrequested member was added.

## Native URI behavior

KUriModel is the common implementation core exercised against java.net.URI on JVM and delegated to by the iOS actual. Parsed text is retained, with UTF-8 percent decoding for getters; opaque URIs have null hierarchical components. Component builders quote percent signs and illegal characters separately for path, user info, query and fragment. Non-ASCII letters remain literal. Equality folds scheme/server-host case and percent-escape hex case, distinguishes opaque/hierarchical forms and retains raw component semantics (no path normalization). Hashing uses the same canonical components. Invalid server names in parsed input can remain registry authorities with a null host; component constructors require a valid server authority. JVM `KUri`, `Content` and `Document` actuals remain typealiases, so existing Java-facing URI/JDOM signatures are unchanged.

### Task-by-task seam evidence

The sections below preserve contemporaneous task inventories and evidence; their future-tense handoff instructions describe the state before Tasks 16-17. Final dispositions supersede those historical obligations: the Task 8 lexical random-access, Task 9 backend/clone/raw-file, Task 10 compression, Task 12 ticker, Task 13 archive, Task 14 utility, and Task 11 transport actuals are now declared and implemented in common/JVM/iOS source sets. They compiled for the specified Linux Native targets; that is not macOS link/runtime evidence. The accepted JVM XZ valid-payload gap was closed in Task 17. The old generic Native `IllegalArgumentException` URI-classification concern was superseded by common `KUriSyntaxException`, which the Native classifier recognizes precisely. The following final-policy details qualify any broader historical wording.

### Task 6 util JVM boundaries and common actuals

`ResourceHomeHook.jvm.kt` provides `systemProperty(String?): String?`,
`platformOsName(): String?`, `platformDisplayLanguage(String): String`,
`pathUri(Path): KUri`, and `setUriCachePath(Path?)`. The JVM language fallback
uses JDK default DISPLAY locale; it is not the JSword locale. iOS provides host
home/null system properties, iOS OS name, and path/cache equivalents.

`PropertyMapJvm` owns stream load/store. On the JVM, `legacyPropertyEntries`
retains JDK17 `Properties` key enumeration for sequential non-null String puts
only: load models each parsed put, including duplicate-key updates; store models
fresh `Properties` plus `putAll`. This is not a guarantee for concurrent access,
removals, defaults, arbitrary Map operations, or all JDK/Android implementations.
Native uses a matching bounded CHM-order model for that contract. Native comments
use `NSDateFormatter` with `en_US_POSIX`, the default time zone and
`EEE MMM dd HH:mm:ss zzz yyyy` in place of the JDK-generated timestamp; Darwin
abbreviation/DST runtime parity remains unverified. Native property store opens
and truncates the sink before copying properties, writes then explicitly flushes
before close; cleanup close failures are suppressed by `IOUtil.close`. The Native
storage-save helper likewise closes its owned fd if charset construction fails,
unlike the characterized JVM legacy leak; this is a deliberate bounded cleanup
difference, not universal ownership equivalence. `IniSectionJvm` owns File,
InputStream and Writer overloads; IniSection consumes Path, ByteArray and Appendable.
JVM callers and BibleApplication's resource stream call were qualified mechanically.

### Task 6 util review fixes

`IniSectionJvm.kt` also supplies `platformIniLineSeparator(): String`, backed by
the cached `System.lineSeparator()` value (as PrintWriter uses). The internal
`iniLineSeparator` platform value can be forced in characterization; iOS uses LF.
The common serializer uses this for record endings only; embedded continuation
LF remains unchanged.

`unmodifiableIniKeys(Set<String>): Collection<String>` and
`unmodifiableIniValues(Collection<String>): Collection<String>` are platform
helpers preserving live read-only views and the JVM Java `removeIf` default-method
contract: always UOE, without evaluating a predicate, even for an empty view or no
match. The iOS actual provides a live read-only view. The former common wrappers
were removed because Kotlin collection implementations do not override Java
`removeIf`.

### Task 6 final File boundaries

`MetaDataLocator` owns nullable Path locations; `MetaDataLocatorJvm` exposes File
getters. `validatedFileUriPath(KUri): Path` in ResourceHomeHook.jvm.kt deliberately
uses File(URI), preserving rejection of non-file, relative, authority, query and
fragment URIs. The iOS implementation validates the corresponding URI cases.
Directory creation failure remains ignored and the computed location is returned.

`SwordBookMetaData` accepts Path and exposes Path? configFile;
`SwordBookMetaDataJvm.create(File, KUri?)` and `getConfigFile(metadata)` are the File
factory/bridge. Its Writer normalize entry point moved intact to that holder.
IniSection Path and ByteArray parsing already uses KCharset, including the
UTF-8 first parse, filtered Encoding lookup, clear and CP1252 retry.

`SwordBookPathJvm` owns the seven File methods, delegating to the Path core and
checking an empty download File before conversion (okio maps empty to dot).
Empty File book-list requests also retain the old null result. This and the
metadata/state holders listed above are the three authorized JVM File splits; all
other move-set implementations are in commonMain with platform actuals.

The platform storage hooks preserve the JVM contracts for child-path joining,
`metadataDefaultsView(Map<String,String>): Map<String,String>` unmodifiable-map
behavior, access checks, separators, `mkdirs` failure and native `File.list`
ordering (which differs from sorted okio listing). iOS uses its filesystem,
access and child-path equivalents; do not silently substitute sorted listing on
the JVM. Driver registration uses the facade hook below, with assignment before
registration unchanged.

Mechanical real JVM/app File caller qualifications are advanced from Task 14:
SwordBookDriver, AbstractSwordInstaller, the dual-compiled CorpusProbe (reflective
old/new selection), File metadata tests, BackupControl, InstallDeps,
SwordEnvironmentInitialisation and the existing app download/backup tests.
Null-only configFile checks in TtfBook, BackgroundImageBook and installer tests
remain on the core and require no File conversion.

### Task 6 File-boundary review fix 1: lexical location (final implementation)

`StorageLocation` is an internal minimal String-backed value. Its spelling is
authoritative for legacy JVM File identity, names, child joins, discovery,
deduplication, migration and IO. Path getters/constructor APIs are projections
for common Path callers only: okio cannot represent Unix literal backslashes
or preserve dot spelling. JVM File holders never round-trip via those projections.
No normalized-key cache or global Path-to-File map exists.

`SwordStorageHooks.kt` declares lexical `storageChild`, `storageName`,
`storageParent`, `storageRead`, `storageExists`, `storageDirectory`,
`storageCanRead/Write`, `storageMkdirs`, `storageListNames`, `storageMove`,
`storageSave`, and `storageUriLocation` (File(URI) validation retained). The
JVM and iOS actuals use lexical spellings; iOS implements native filesystem
operations rather than normalizing through Path. The earlier Path-based
metadataChildPath/access/list helpers are superseded. `metadataDefaultsView` and
`storageSeparator` remain as inventoried.
The internal lexical metadata constructor is JVM public and has one exact K4
seam allowance; locator Path projection getters are concrete (exact allowances
updated). No File signature is reintroduced into the common-ready classes.

### Task 6 File-boundary review fix 2

`StorageLocation` remains inline in the already-listed `SwordBookPath.kt`; it was not an unlisted JVM dependency or move-count increase.
JVM hooks `storageFileSpelling(String): String`,
`storageFileEquals(StorageLocation, StorageLocation): Boolean` and
`storageFileHash(StorageLocation): Int` use File construction/equality/hash:
redundant/trailing separators collapse, dot segments and Unix backslashes remain.
The Native actuals provide POSIX lexical-path construction/equality/hash, rather
than routing File spellings through Path. `storageSave` uses JVM
FileOutputStream/OutputStreamWriter and the existing INI Writer serializer
without Path conversion; charset selection, record separators/continuations,
flush, owned-stream closure and IOException logging remain JVM behavior. The
Native helper opens/truncates before charset construction and closes its owned fd
on charset failure; JVM legacy code can leak the stream when writer construction
fails. This deliberate Native cleanup difference is bounded, not universal
ownership equivalence.

### Task 8 lexical random access boundary (common/JVM/iOS implementation)

`storageOpenRandomAccess(StorageLocation, writable: Boolean): RandomAccessSource`
is a common expect with JVM and iOS actuals. JVM opens
`RandomAccessFile(File(spelling), "r"/"rw")` directly and owns it through an okio
FileHandle; authoritative spelling never passes through Path. The iOS literal-path
FileHandle actual retains writable creation, zero-filled extension, read-only
write failure and owned close. FileHandle resize/flush methods are adapter
operations, not new caller-facing operations.

RandomAccessSource internal cursor compatibility operations (`position`, `seek`,
`readAtCursor`, `readNext`, `readUntil`, `writeAtCursor`, `writeNext`) serve existing
TreeKeyIndex and RawFiles callers only. Cursor reads seek first (negative offsets
throw even for zero/negative sizes), shorten past EOF, and consume the delimiter.
The original public positional `read` remains unchanged, including its documented
zero-size-before-offset difference. Cursor operations are common Kotlin logic and
need no platform hooks; only opening the lexical source is platform-specific.

`Scheduling.kt` declares the common `Cancellable` and `scheduleRepeating` seam;
JVM uses its daemon `ScheduledExecutorService`, while iOS uses a one-shot dispatch
source timer on a global queue. The Native implementation rearms after each task
to retain fixed delay rather than fixed rate. This implementation compiled for
Linux Native; macOS runtime behavior is not established. The design is:

```kotlin
// Scheduling.ios.kt (illustrative implementation shape)
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

The implemented contract cancels rather than allowing a task exception to escape
into a C callback. Cancellation does not interrupt a running task and prevents
later rearming. Compilation validated the dispatch API types, but the snippet and
compiler gate are not Native runtime evidence.

## Task 8 state port closure

All six state implementations retain lexical `StorageLocation` values and own
`RandomAccessSource` handles opened through `storageOpenRandomAccess`, not Path.
SwordUtil's six public RAF overloads use internal cursor operations preserving
seek-before-size, delimiter inclusion, EOF shortening, null/empty writes and close
ownership. RawBackend, ZVerseBackend, RawFileBackend and TreeKeyIndex received
handle substitutions only, not Task 9 algorithm replacements.

`storageAbsolute(StorageLocation): String` delegates to
`File(location.spelling).absolutePath` on JVM and uses Native working-directory
spelling on iOS without canonicalizing dot segments or backslashes. Existing
permission/existence/separator and lexical-open hooks are reused.
`closeStateSource` is in common `AbstractOpenFileState.kt` (standalone
`StateSource.kt` was removed), portable Kotlin/okio suppressing IOException under
the legacy IOUtil logger; it needs no platform actual.

File accessors moved to `RawBackendStateJvm`, `RawLDBackendStateJvm` and
`RawFileBackendStateJvm` (StateFilesJvm.kt); the latter also adapts the incfile
setter. Holders stay JVM-only, internal location accessors stay with state code.
No app caller of these state File accessors or RAF helpers exists. RawFileBackend
is the sole production File-accessor caller. Protected File fields were removed;
source fields retain protected visibility. Exact API removals/replacements have
K4 split/stream entries in api-allowlist.txt.

### Task 8 fix-round closure

Common `currentTimeMillis(): Long` is backed by `System.currentTimeMillis()` on
JVM and `NSDate.timeIntervalSince1970 * 1000` converted to `Long` on iOS (epoch
milliseconds, not monotonic elapsed time). Tasks 11/12 reuse this hook. The nine
listed state files are common; no move-set entry was added (83 retained). JVM
File holders remain JVM-only.

RandomAccessSource internal `readNext` now advances its cursor after each successful
read chunk, including progress before EOF/IOException. Public positional read
still does not mutate cursor. Internal `readStream(ByteArray): Int` is one direct
FileHandle read with no Sword record EOF logging, used only by incfile to retain
FileInputStream's single-read byte count and old diagnostic events. Both are
portable Kotlin/okio and introduce no Native platform hook.

### Task 9 backend preservation seams (final implementations)

`BackendNumberFormat.kt` declares internal `backendZeroPad(Int, Int): String`.
The JVM actual uses a fresh JDK `DecimalFormat` at the default FORMAT locale; the
iOS actual uses `NSNumberFormatter` at `NSLocale.currentLocale`, without grouping
and with the requested minimum integer width. It must not silently use ASCII
`padStart`. JVM reference coverage includes Arabic, Persian, Bengali, Thai
numbering extension, zero, max Int and regex rejection/overflow. The Native locale
format behavior was source-reviewed/compiled, not runtime-verified.

`BackendDebug.kt` declares internal `backendDebugFailure(Throwable): Unit`. JVM
uses `Throwable.printStackTrace()` to stderr; iOS emits the failure and available
stack trace to stderr. RawLD and ZLD debug methods catch IO and `BookException`
as before; source `Appendable` failures enter the same diagnostic path.

`BackendCloneBase` is a permanent platform clone base, not a temporary JVM
masking workaround. Its common expect exposes protected `shallowBackendClone():
Any` and protected open `portableBackendClone(): Any`. JVM `shallowBackendClone`
is final and directly invokes `Object.clone` (javap confirms `invokespecial`, no
virtual recursion), retaining unknown subclasses, extra fields, metadata identity
and shallow references. The iOS actual delegates `shallowBackendClone` to
`portableBackendClone`; `AbstractKeyBackend` calls that bridge. Known RawLD/ZLD
copies are portable and Task 17 covers JVM backend behavior. An unknown Native host
subclass must override `portableBackendClone`; otherwise clone throws an explicit
`UnsupportedOperationException`. Portable RawLD/ZLD copies preserve the base metadata reference and
`ignoreReadErrors`, RawLD `datasize` and derived `entrysize`; ZLD adds no instance
fields. JVM clone retains `Cloneable` transitively. The exact
AbstractBackend superclass/copy helper and protected copy constructors/overrides
are K4 seam API allowances. No Unsafe, reflection or module-open dependency.

`TreeNodePlatform.jvm.kt` defines internal typealias `TreeNodeSerializable =
java.io.Serializable` and a NON-Serializable abstract `TreeNodeCloneBase : Cloneable`.
The common expect marker has the JVM typealias actual and an empty iOS marker actual.
The common clone base exposes protected `shallowTreeClone(): Any` and protected
open `portableTreeClone(): Any`; JVM's final bridge calls `Object.clone`, while
iOS delegates to `portableTreeClone`. TreeNode's explicit known copy aliases the
private `userData` array exactly as the old shallow clone and copies
`offset`/`name`/`firstChild`/`nextSibling`/`parent`. Public open
`getUserData`/`setUserData` retain defensive copying; internal callers use explicit
methods. Unknown JVM subclass references remain shallow; unknown Native subclasses
must supply a portable hook or receive `UnsupportedOperationException`. The base
has an effect-free public no-arg constructor, is not Serializable, and adds no
serialized superclass descriptor. The frozen oracle compares old/new serialized
bytes byte-exactly in both classloader directions (six fields, same userData
name/type and serialVersionUID); round-trip and backing-identity tests complement
it. This exact serial oracle is distinct from API parity, which does not cover the
old package-private `TreeNode` surface. TreeKeyIndex uses lexical
`storageCanRead`/`storageAbsolute` and `storageOpenRandomAccess`, never Path
normalization.

`RawFileIo.jvm.kt` contains bounded lexical stream hooks: `rawFileCreate(Location):
Boolean = File.createNewFile`; `rawFileNumber(Long, Boolean): String = default FORMAT
String.format(%07d with optional CRLF)`; `rawFileDecodeName(ByteArray): String =
JDK Charset.defaultCharset()` decoding of seven bytes, explicitly supplied to
avoid Kotlin's UTF-8 constructor default; `rawFileAppend(Location, Long, String?): Unit` opens
append BufferedOutputStream BEFORE named charset encoding (including legacy
UnsupportedEncodingException/file creation); `rawFileWrite(Location, ByteArray):
Unit` truncates buffered output; `rawFileRead(Location, Logger): ByteArray` uses File
length.toInt and ONE BufferedInputStream read, preserving short-read diagnostic,
empty input and FileNotFoundException logging; `rawFileWriteInc(Location?, ByteArray,
Logger): Unit` uses truncating FileOutputStream and catches FileNotFoundException;
`rawFilePopulate(Location, Int): Unit` truncates and writes count six-byte zero records.
`RawFileIo.kt` declares the common hooks with JVM and iOS actuals. The iOS actual
uses UTF-8 for default filename bytes, localized `NSNumberFormatter` with seven
minimum integer digits (six plus a minus sign for negative values), lexical IO,
append-open-before-charset-encoding and a single buffered-style read (8192-byte
fill for small requests, direct read for large requests, no retry of a short read).
It preserves the FileNotFound diagnostic paths and six-byte record population.
Native locale/IO runtime behavior is not established by Linux compilation. Do not
substitute `storageRead`'s read-all behavior or normalize paths. These hooks are
internal, not protected APIs.
Legacy protected `updateDataFile(Long, File)` is split to public static
`RawFileBackendJvm.updateDataFile(RawFileBackend, Long, File)`, with the one exact K4
split removal allowance. No jsword or app subclass caller exists; portable backend
calls internal rawFileAppend on state's lexical text location. Existing overloaded
setRawText/setAliasKey signatures and protected updateIndexFile are unchanged.

RawFile default decoding is verified against the old Java constructor in a fresh
JDK17 subprocess with ISO-8859-1 default charset, including an actual non-ASCII
lexical filename read. iOS uses UTF-8 for the platform-default filename charset;
the Native actual explicitly decodes the first seven bytes with `KCharset` rather
than inheriting a Kotlin constructor default. Native filename/read/locale behavior
was compiled but remains unverified on macOS runtime.

ZVerse debug methods retain host println separators through the existing
`platformIniLineSeparator()` cached System.lineSeparator seam. Both populated dump
methods match independent PrintStream output in a fresh CRLF-host JDK17 subprocess.
The common platform separator remains LF on iOS and the cached host `System.lineSeparator()` on JVM; no helper changes prior RawLD debug behavior.
ZVerse debug methods consume Appendable; exact existing Task 8 stream allowances
are adjusted only for that extra argument. No app debug caller exists. Existing
backendDebugFailure handles caught source IOException; Appendable exceptions
propagate (these old methods did not catch output writes). Arraycopy becomes
copyInto on identical byte ranges. Compressor uncompress(...).toByteArray remains
unchanged for Task 10.

## Task 10 compression inventory (2026-10-13; final implementation in Tasks 16-17)

All eight compressor move-set files are in commonMain, and `CompressorRoundTripTest`
is in commonTest. Frozen JVM-stream differential tests remain JVM-only. JVM
BZIP2/XZ keep commons-compress; Native BZIP2/XZ throw the typed unsupported
`BookException` for both compress and decompress. The valid-payload JVM XZ
dependency gap recorded during Task 10 was subsequently closed with working test
dependency coverage in Task 17. Native compilation is not Native runtime evidence.

The common platform contract and source-set actuals are:

```kotlin
internal expect fun platformCompress(type: CompressorType, input: ByteArray): ByteArray
internal expect fun platformUncompress(type: CompressorType, input: ByteArray, expectedLength: Int): ByteArray
internal expect fun platformGzipUncompress(input: ByteArray, expectedLength: Int): ByteArray
```

BZIP2/XZ JVM bodies retain BufferedInputStream, ByteArrayOutputStream, the default
commons-compress constructors, IOUtils.copy and original close/flush ordering.
The iOS BZIP2/XZ actuals must both throw
`BookException("unsupported compression on this platform: $type")`, including
compression. Typed Native error expectations are covered in common tests. Task 17 added and ran
valid-payload JVM XZ decoding/round-trip coverage with the required runtime
dependency; the earlier optional tukaani-xz gap is closed. This does not imply
that Native XZ is supported.

GZIP is supported, not an unsupported Native codec. Independent characterization
proved Okio GzipSource is not a compatible decoder: concatenated members and
trailing bytes are rejected rather than returning the first member; malformed
short headers throw EOFException rather than legacy IOException. The minimal gzip decoding seam keeps the original commons-compress JVM body.
The Native implementation supports first-member-only decode, acceptance of
trailing bytes/extra members, length-as-capacity (zero accepted, negative
rejected), CRC and truncated-input errors matching the frozen reference. Native
runtime error timing remains a macOS verification item, not a reason to replace
GZIP with an unsupported error.

GZIP compression uses Okio GzipSink over Buffer with OS header byte 9 patched to
255 (commons default unknown OS instead of Okio FAT=0). Independent byte-exact
corpus checks cover all remaining header/body/trailer bytes. ZIP uses the existing
Zlib unchanged, including IllegalArgumentException on uncompress(0). Therefore
empty ZIP is tested via no-arg decode plus exact-length-zero failure rather than
changing the legacy inflater to satisfy the plan's guessed roundtrip example.
LZSS preserves constants/tree/ring behavior with indexed unsigned byte reads and
Buffer output, including truncated token acceptance and negative-capacity error.

## Task 12 progress inventory (Steps 3-5; common/JVM/iOS implementation)

`JobManager.createJob(String, String, (() -> Unit)?): Progress` replaces the
Thread factory in the common-ready class. `JobManagerJvm.createJob(String,
String, Thread?): Progress` retains the Thread-taking signature in the authorized
JVM holder and wraps interrupt. All three-argument old callers (including app's
two production null callers and its download-cache test) qualify that holder;
there is no ambiguous nullable Thread/lambda overload. The one-argument factory
remains unchanged. Cancel still completes/notifies/removes before calling the host
callback, ignores a false cancelable flag, and skips sequential repeats/finished
jobs. A thrown completion listener leaves the finished job registered and skips
cancellation, as the real old Thread characterization proves. No new idempotency
is imposed on simultaneous cancellation. Manager delivery uses CopyOnWriteList
snapshots; jobs use a narrow atomicfu lock, insertion ordering and a snapshot iterator
whose remove always throws. Callbacks run outside the manager lock.

`ProgressPlatform.kt` declares the platform hooks for random job IDs, reporter
source names/logger types and AWT exception registration. JVM preserves
`UUID.randomUUID`, binary simple names and the nested public AWT handler's original
binary name; iOS supplies UUID/class-name/KClass equivalents and no-op AWT
registration. Reuse the common epoch-millisecond clock and `Cancellable`; Job's
separate ticker is inventoried below.

Fix round 1 superseded the original event/ticker waiver: events extend
`ProgressEventBase`, a genuine JVM typealias to `java.util.EventObject`, with no
duplicate stored source. Frozen WorkEvent/ReporterEvent descriptors, UIDs and
byte-exact serialized streams match the isolated oracle in both loader directions,
including non-Serializable Job and Any sources; deserialized source remains null
as before. No event superclass/getSource/toString/source-field allowance remains.
The common `ProgressEventBase(source: Any?)` preserves inherited `getSource()` and
`toString()` semantics; JVM actual is EventObject, Native is a plain base rejecting
a null constructor source. ReporterEvent uses `getSource()!!` because construction
guarantees a source; JVM deserialization retains the legacy null-source
dereference failure. Native Java serialization is not a contract.

Job uses the separate `JobTicker.kt` seam, not the pool's open-file
`scheduleRepeating`. JVM retains the original non-daemon Timer/TimerTask behavior.
The Native actual implements an immediate first tick, epoch-wall-clock
START-relative cadence (next start after `max(period, callback duration)`), no
overlap, cancellation without interrupting a running callback, and uncaught
failure termination/visible host error rather than suppression or a leaked worker.
The JVM characterization confirms non-daemon lifetime, uncaught-handler identity
and worker termination; slow-callback tests use a 3-second period with a 3.5-second
overrun and 1.5-second slack, rejecting the former completion-relative scheduler.
The Native actual compiled but runtime clock/GCD/lifetime behavior remains a host
macOS verification obligation.

Job prediction I/O calls `NetUtil.loadProperties`/`storeProperties` in legacy
order through KUri (JVM URI alias); this preserves lexical URI/File spelling and
the bounded properties contract above, not a general Map-order guarantee. Do not
replace it with normalized Path IO. `JobCancellerTest` and portable listener tests
are in commonTest; real Thread, reflection/ObjectStreamClass and blocked-read
characterization remain jvmTest.

## Task 13 archive compatibility (final implementation)

`TarReader`, `StoredZipWriter` and `SwordArchive` are in commonMain. TarReader's internal
`consumeLegacyBlockPadding()` consumes up to the next 10240-byte tar block boundary
like commons-compress 1.12 (short EOF accepted), not the rest of the gzip member.
A 24-case frozen legacy differential covers termination before/at/after that
boundary, extra tar-tail bytes, truncated gzip trailers and corrupt CRCs.
Internal `readInstallerEntry(): ByteArray` permits the original short body write;
`nextInstaller(): Entry?` preserves the later IOException for an incomplete body.
Public `readEntry()` and `next()` remain strict EOFException/two-zero terminator.
Installer `nextInstaller()` independently permits absent/short next records and optional,
nonvalidating second EOF records and short padding after a completed body, retaining
actual consumed counts and 10240-byte consumption. Internal Source constructor leaves
public BufferedSource constructor/strict policy unchanged. GNU L/PAX x extension
bodies and padding use that same raw installer policy, never the public buffered
reader (actual old installer long-name/path plus subsequent/filtered entries covered).
Unlike ordinary selected bodies, incomplete declared GNU/PAX extension bodies fail
with IOException before padding/parsing or same-call next-header EOF; frozen old
installer differentials cover empty/partial extension bodies with/without an earlier
cache entry. Fully consumed GNU bodies still permit short padding (0/1/100 bytes).
A terminal fully consumed local PAX x header with no following entry fails with
portable stdlib NullPointerException in the internal installer path, preserving
commons' recursive next-entry dereference even for ignored comment records.
A 24-case actual frozen installer differential pins complete path/comment bodies,
absent/1/100/full padding and one/two zero terminators, with/without first.conf[1];
both caches finalize empty or with that earlier name/payload despite the failure.
On a zero terminator, the existing 10240-byte block-padding consumption precedes
this failure: twelve further cases pin NPE for valid gzip, EOFException for a
missing trailer, and IOException for corrupt CRC. Raw missing-header EOF does not
add a block drain. Normal PAX with a following entry, completed GNU short-padding,
incomplete-extension IOException and public strict EOF/terminator remain unchanged.
The old JVM-generated setName/isOldGNUSparse dereference messages are recorded
separately, not emulated or claimed identical; no platform error seam is added.
Native must retain this internal failure class/cache/error-priority policy and
raw consumed-count bounds; these JVM cases do not establish Native runtime parity.
This is legacy preservation, not generalized parser hardening.
Partial-cache differential pins
first entry plus 4300 bytes of a truncated later entry and finalized directory.

`ArchivePlatform.kt` declares common hooks with JVM and iOS actuals for supported
operations. Native source compiled on Linux; archive/file ownership, inflater behavior,
locale and fixture-provisioning runtime still require macOS verification.

- `archiveRead(String): ByteArray`, `archiveSink(String): BufferedSink` preserve
  lexical File spelling, owned output close and filesystem errors.
- `archiveGzipSource(String): Source` owns File input plus the original
  first-member-only commons gzip wrapper. Native must support gzip (not throw
  unsupported), trailing/concatenated-member acceptance, incremental validation
  and read-consumption/error timing. Installer reads use raw requested-count bounds,
  never BufferedSource 8192-byte read-ahead across body/header/padding boundaries;
  truncated trailer/CRC failures retain the preceding completed cache entry.
  Do not drain past legacy tar block padding.
- `archiveLowercase(String): String` uses JVM Locale.getDefault(), NOT JSword
  locale. Filter strings/output names remain unchanged; the Boolean is include.
- `archiveZipName(ByteArray, Boolean): String` retains commons' default UTF-8
  decoding (including unflagged UTF-8 names), not CP437 by assumption.
- `archiveDescriptorSource(ByteArray, Int, (Int) -> Unit): Source` streams raw
  DEFLATE from an offset without a suffix copy; at EOF its callback reports compressed
  bytes consumed. Selected cache bodies decode once; skipped descriptor entries drain
  through a 4096-byte discard buffer as the original sequential reader does (malformed
  skipped seekable entries are ignored, malformed skipped descriptors throw ZipException).
  Native requires negative-windowBits zlib, consumed-input accounting and matching
  streaming EOF/errors. Unused bounded `archiveInflate` and intermediate scan helper removed.
- `archiveCacheSource(ByteArray, Int, Int): Pair<Source, () -> Unit>` takes whole
  archive bytes, local body offset and compressed size. Sequential raw inflater fills
  512 compressed bytes (may include following headers); bounded caller reads expose
  short output before suffix failure. Advance callback drains only when fed compressed
  bytes exceed declared compressed size, otherwise skips without inflation, matching
  commons closeEntry. Invoke only after successful visitor callback, never in finally.
  Native requires equivalent raw zlib input-fed accounting/conditional advance/errors.
- `archiveInflateSource(ByteArray): Source` uses InflaterInputStream with raw
  Inflater(true), preserving ZipException and successful chunks before late failure.
  Native uses negative-windowBits zlib with equivalent streaming/error behavior.
- `archiveUnsupportedZipFeature(String, Int): Nothing` takes archive literal spelling
  and central enumeration index. Error-only JVM reopen uses the actual commons ZipFile
  entry/getInputStream exception, preserving all attached central/local metadata,
  feature and message (including duplicate names). No successful-body delegation.
  It closes the reopened archive/any unexpectedly returned stream; primary exception
  survives close failure. Extra error-path reopen has a concurrent-file-change/snapshot
  limitation. Native needs a typed IOException-facing unsupported-feature outcome with
  actual central/local metadata and equivalent timing, not a commons dependency.
  Skipped central entries/directories do not validate features.
- `archiveExtract(String, String, Boolean, () -> Source): Unit` performs lexical File
  child joins, canonical containment check against existing symlinks, legacy
  parent mkdir/error text, output case preservation and owned output close.
  Native must preserve literal filename spelling and enforce equivalent containment.

Task 13 continuation separates readConfZip sequential local-header order from
unpackZip central-directory order, retaining duplicates and nonalphabetical order.
Internal `visitConfZipSpelling(String, (String, ByteArray) -> Unit)` preserves the
actual installer declared-size loop (zero skip, undersized prefix, oversized read
failure, descriptor -1 NegativeArraySizeException), metadata processing and earlier
registrations before later failures. Raw Source reads request only the remaining
prefix; seekable unread compressed suffixes skip or drain according to the original
512-byte input-fed closeEntry condition. Both 200-byte (drain/error) and 5000-byte
(skip/success) malformed suffix cases retain the first book. A throwing callback
occurs before any suffix advance.
The internal ownership-test overload adds `(Source) -> Source` decorator before
that visitor; production delegates with identity. Tests wrap the actual source to
observe exactly-one close and suppressed close errors, not a synthetic reader or
inflater-GC counter. Raw source closes if decoration throws; owned use closes on
read/callback/advance failure. Close releases inflater without advancing; advancement
remains only after successful callback.
Public readConfZip List remains the generic
body API, not the installer oracle. Logger category uses the exact FQN string with
no dependency on the JVM-only installer class.
Cache streams ignore central CRC/sizes; extraction ignores local names/flags/method/
CRC/sizes and descriptor consistency as independently characterized against old
commons readers. CRC correctness remains a writer obligation, not new reader strictness.
Single-disk STORED/DEFLATED and small ZIP64 metadata/descriptors are covered;
compressed payloads must fit Int/ByteArray. Encrypted/unsupported methods and
multidisk archives remain limitations. Malformed length safety checks are retained.
Traversal/drive/UNC/dotdot names are rejected (tar/conf cache skipped, module ZIP
throws); existing symlink destination escapes are rejected. These are deliberate
security fixes over the frozen legacy extractor, which permitted contained zip-slip.
ZIP symlink entries remain ordinary files as in the old extractor. No global
Path-to-File map or IOUtil-wide split is introduced. File-facing callers pass
literal spelling directly to internal adapter entry points; Path APIs are projections.

## Task 14 member classification (before edits, base cdce9399)

Controller rulings: the actual `getURI(File): URI` moves to `NetUtilJvm`;
no unused Path overload is added. Reviewed HEAD Path operations stay Path-based;
lexical stream/property behavior never round-trips through Path. Preserve Java
MalformedURLException throws lists with a precise exception alias, not IOException
widening. URI syntax classification must rethrow unrelated failures.

| Members (all overloads unless stated) | Classification / actual callers outside move set |
|---|---|
| NetUtil private constructor; constants PROTOCOL_FILE/HTTP/HTTPS/FTP/JAR, INDEX_FILE, SEPARATOR, AUTH_SEPERATOR_USERNAME/PASSWORD; private isSeparator, LOGGER | common-ready unchanged; HTTP(S) installer constants, LuceneIndexManager separator; other constants have no external production caller |
| NetUtil.copy, shortenURI, lengthenURI; IsDirectoryURIFilter constructor/accept/parent | KUri core; lengthenURI outside callers: AbstractSwordInstaller, CWClassLoader, LuceneIndexManager; copy/shortenURI/nested filter have no callers |
| NetUtil.toPath | existing validatedFileUriPath projection; WebResourceCharacterizationTest and NetUtilPathTest only; never authoritative stream identity |
| NetUtil.makeDirectory/makeFile/isDirectory/move/delete/listByFile; checkFileURI | retain HEAD KFiles/Path behavior and explicit KUri getters; external isDirectory LuceneIndexManager; delete GenericFileDownloader, LuceneIndexManagerTest; other production callers only CWProject/internal core |
| NetUtil.isFile/canRead/canWrite | KUri core with bounded non-file existence/lexical access hooks; outside isFile AbstractSwordInstaller, canRead CWClassLoader and InstalledIndex; canWrite only CWProject |
| NetUtil.list/listByIndexFile | KUri core, Source boundary for exact URI input; outside production none; IndexFilesDifferentialTest verifies listing |
| NetUtil.loadProperties/storeProperties | KUri core; exact lexical property IO boundary preserving load explicit close then finally-close, store open-before-null/copy, Properties escaping/order/timestamp; outside callers InstallManager, CollectionUtil, Config, InstalledIndex (Java); Task12 Job retains these methods |
| NetUtil.getSize/fileSizeToInt/getLastModified/isNewer | KUri core retains HEAD Path fast path, HTTP WebResource branch and exact URL/Jar fallback through bounded helpers; outside getSize MyBibleInstaller, HttpSwordInstaller, HttpsSwordInstaller; isNewer AbstractSwordInstaller; other overload callers none |
| NetUtil.getTemporaryURI/TEMP_SUFFIX | KUri core delegates exact File.createTempFile boundary; external GenericFileDownloader, AbstractSwordInstaller |
| NetUtil.getAsFile/getInputStream/getOutputStream(URI)/(URI,Boolean), toURI(URL), toURL(URI) | exact signatures move to object NetUtilJvm `@JvmStatic`; getAsFile outside AbstractSwordInstaller, LuceneIndex/Manager (Java), GenericFileDownloader; stream callers PropertyMapTest (Java), LegacyArchiveReference, WebResourceCharacterizationTest, Task11 WebResourceIo.jvm; toURL CWClassLoader; toURI no caller |
| NetUtil.getURI(File)/getURICacheDir/setURICacheDir/private cachedir | JVM holder retains actual File spelling/state; external getURI SwordBookDriver, AbstractSwordInstaller, BackupControl, ModuleBackupRoundTripTest, CurrentPageManagerNoActivityTest, LegacyArchiveReference, NetUtilFileCharacterizationTest; cache ResourceHomeHook.jvm, installer characterization Java tests, MetadataUtilCharacterizationTest |
| NetUtil.URIFilterFilenameFilter constructor/accept/filter | unused JVM FilenameFilter adapter moves as nested NetUtilJvm class (no facade leak); IsDirectoryURIFilter stays core |
| IOUtil private constructor/log/close(Closeable) | common-ready okio.Closeable alias, exact IOException suppression/logger; outside CommonUtils, FileManager, LuceneIndex (Java), PropertyMapTest, RawFileBackendTest, LegacyArchiveReference |
| IOUtil.unpackZip(File,File)/(File,File,Boolean,String...) | exact static signatures move to IOUtilJvm; outside AbstractSwordInstaller, LuceneIndexManager (Java), SwordArchiveTest, ArchiveLegacyCharacterizationTest; lexical SwordArchive.unpackZipSpelling unchanged |
| IOUtil.getZipEntry(String) | only move-set SwordBookMetaData caller; common method delegates bounded platform zip-entry hook retaining actual commons lookup/read/close and exception behavior (no guessed generic SwordArchive semantics) |
| FileUtil private constructor; EXTENSION_JAVA/PROPERTIES/PLUGIN/XSLT/XML; MODE_READ/WRITE | common constants unchanged; outside PropertyMapTest/InstalledIndex, InstallManager, ResourceUtil, XMLUtil; unused constants remain |
| FileUtil.delete(File):MutableList<File> | exact static method moves FileUtilJvm, preserves HEAD KFiles projection algorithm; outside SwordBookDriver, AbstractSwordInstaller, LuceneIndex/Manager (Java), NetUtilFileCharacterizationTest; no unused core Path API added |
| CWProject all members; URIFilter.accept; DefaultURIFilter.accept | already common-ready paths/hooks at HEAD; retain reviewed property/cache/migration Path projection behavior; no replay of Task6/8/9 holders |

### Task 14 exact platform inventory (common/JVM/iOS implementation)

`NetUtilPlatform.kt` declares the common URI/file/URL/property helpers with JVM
and iOS actuals: `netUriSyntaxFailure(Throwable): Boolean`
(exact `URISyntaxException` only), `netCanRead(KUri): Boolean`,
`netCanWrite(KUri): Boolean` (File(decoded path) access),
`netCheckResource(KUri): Unit` (open and close non-file URL input),
`netUrlSize(KUri): Int`, `netUrlLastModified(KUri): Long` (URLConnection,
including JarURLConnection entry time), `netLoadProperties(KUri): PropertyMap`,
`netStoreProperties(PropertyMap?, KUri, String?): Unit`,
`netTemporaryUri(String, String?): KUri`, and
`netListByIndexFile(KUri, URIFilter?): Array<String>`.
Property hooks retain lexical URI stream input/output, explicit load-close plus
finally-close, output-open before null/copy/store, JDK Properties format/order and
owned IOUtil close suppression. Index hook retains UTF-8 decode and IndexFiles
parsing, closing the actual stream even when the filter throws. Temporary URI
retains prefix validation, null suffix default and real creation.
`IOUtilPlatform.jvm.kt` contains `ioZipEntry(String): ByteArray`, retaining exact
commons ZipFile lookup, size allocation, declared-size read loop and closure.
Native must implement those actual behaviors without a JVM/commons dependency;
non-file transport behavior is not silently replaced by Path filesystem IO.

`NetMalformedURLException` is an internal common IOException subtype; its JVM
actual is a `java.net.MalformedURLException` typealias and its iOS actual is an
IOException subclass. Java throws lists remain byte-for-byte unchanged (ApiParity
requires no exception allowance). `KUriParts` throws the precise internal
`KUriSyntaxException` for parser/constructor syntax failures; Native
`netUriSyntaxFailure` classifies only this subtype and rethrows unrelated
`IllegalArgumentException` rather than blanket-swallowing it.

Task11 WebResourceIo qualifies `NetUtilJvm.getOutputStream` directly, preserving
its special non-interrupting sink adapter. `ResourceHomeHook.setUriCachePath`
qualifies the exact JVM cache holder; its existing Path projection is unchanged.
The common declarations and JVM/iOS actuals are in place. The exception alias is
permanent; URI input/output/property/info operations retain non-file URL behavior
on JVM. Native host HTTP remains track I, and no Native runtime claim is made by
JVM characterization or Linux Native compilation.
All caller inventories include production/tests and existing holders; imports,
comments and reference-only documentation are not calls.

## Task 11 HTTP transport handoff

`HttpTransport` is common; the JVM URLConnection transport and WebResource/File output holder remain JVM-specific.
Controller corrected the eager GET promise after real local HTTPS characterization:
`get(uri: KUri, timeoutMs: Int): HttpBody` returns a lazy request. Access
`contentLength` only when a meter exists (HEAD fallback only for zero, not -1),
then `status`, then `prepareRead(): Unit` before destination open. A dropped
connection's contentLength suppresses IOException and yields -1; meter is updated
before status throws, mapping to LucidException with status still zero.
`prepareRead` establishes inputStream and preserves 404-before-output behavior.
Read/metadata/open InterruptedIOException maps to TransportCancelledException.
Input closes before output, with IOException suppressed/logged as before; no new
explicit disconnect of the URLConnection is introduced.

Plain JVM `internal fun webResourceOutputSink(uri: KUri): okio.Sink` in
WebResourceIo.jvm.kt reuses NetUtil.getOutputStream's lexical FileOutputStream
boundary (including non-file URL behavior). NetUtil.toPath is only a projection:
a legal Unix backslash filename changes into a separator there. The sink adapter
also deliberately avoids okio OutputStream.sink's interrupt check: that adapter
lost the partial file on cancellation in a real blocked-read test. WebResource
emits each write immediately, retaining legacy output-error timing. An internal common expect and iOS actual use exact lexical filesystem spelling;
there is no Path/File identity registry.

`defaultHttpTransport(): HttpTransport?` is a common expect (JVM
`UrlConnectionHttpTransport`, iOS `null`); the nullable transport holder throws
`IllegalStateException("No HttpTransport set")` when unset. Fake transport tests
are in commonTest. Native host HTTP implementation remains track I. The common
clock is reused for timeout behavior.
JVM constructor timeout/proxy arguments remain ignored; companion timeout and
platform default ProxySelector retain authority. Production TLS is unchanged.

The JVM HTTP implementation deliberately retains the known `http:` scheme
bug: the legacy `HttpsURLConnection` cast throws `ClassCastException` for an
HTTP URL. This was characterized and left unchanged; it is a backlog item, not a
K4 regression fix or evidence that plain HTTP transport works. Constructor timeout
and proxy arguments remain ignored as characterized; global proxy-selector tests
do not prove proxy CONNECT tunneling behavior.

The iOS URL/property helpers implement bounded Native behavior, compiled but not
runtime-verified here. `KUriSyntaxException` is the precise URI parser/constructor
syntax subtype used by `netUriSyntaxFailure`; unrelated `IllegalArgumentException`
continues to propagate. Native non-file output throws a typed unsupported-capability
`IOException`: D3 host transport supplies GET/HEAD only and no output port. Native
property store opens/truncates before copying entries, writes then flushes before
close; `IOUtil.close` suppresses close `IOException`. Native property order models
only sequential non-null String puts using the bounded JDK17 order algorithm,
not concurrent/removal/defaults/general-map behavior. Its timestamp uses
`NSDateFormatter` locale `en_US_POSIX`, the default time zone and
`EEE MMM dd HH:mm:ss zzz yyyy`. Neither Darwin timestamp/locale/zone behavior nor
Native HTTP, resource ownership, or filesystem runtime semantics was run on macOS.
JVM non-file `URLConnection` output and HTTP behavior are unchanged.
