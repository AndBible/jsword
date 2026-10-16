# Search Analyzer Resource Provenance

These files are runtime inputs to `BibleCore`; `Package.swift` copies the complete `Resources/search`
directory into the BibleCore resource bundle. Missing resources are an explicit
`SearchIndexError.analyzerResourceUnavailable` failure and never select a fallback analyzer.

## JSword and Lucene Contract

- Android authority: <https://github.com/AndBible/and-bible>
- JSword source revision: `0da7412d7716731f402c9002a0b92e4c00ef30eb`
- Analyzer selection source: `src/main/resources/AnalyzerFactory.properties`
- Query behavior source: JSword `LuceneQueryDecorator` plus Apache Lucene `QueryParser`
- Lucene artifacts: `org.apache.lucene:lucene-core:3.6.2` and
  `org.apache.lucene:lucene-analyzers:3.6.2`
- Artifact SHA-256:
  - `lucene-core-3.6.2.jar`: `cef4436bae85c31417443284f736e321511cd1615268103378a9bf00b1df036d`
  - `lucene-analyzers-3.6.2.jar`: `82f9f78ff2143f1895ac04500aa47fdac3c52632a08522dde7dbb0f0c082801f`
- License: Apache License 2.0, reproduced in `LICENSE.apache-2.0.txt`.

`lucene-letter-ranges.tsv`, `lucene-lowercase.tsv`, and `lucene-ascii-folding.tsv` were generated
by enumerating BMP UTF-16 values through the pinned Lucene/JVM APIs used by `LowerCaseTokenizer` and
`ASCIIFoldingFilter`. `lucene-character-categories.tsv` records the Java `Character.getType(char)`
categories consumed by the mmseg4j and Lucene tokenizer ports. Generation used Eclipse Temurin
17.0.19+10, Docker image digest
`sha256:068a8f9ae4b74d9a20de3ecca771ba1a6437f1d7a8a8ad6deaf9dbdd2274397a`.
Their SHA-256 values are:

- `lucene-letter-ranges.tsv`: `ec4628b3d774854032d487349d1f111fc985c2492e852cf67af2eae9d28f23fd`
- `lucene-lowercase.tsv`: `a7ae93204edcaaa82d79aec61225a49e1f881754c6d2d604cfe109b2e4b1849e`
- `lucene-ascii-folding.tsv`: `5a7b168a1ecb2240d459c1047aa1bdfa318f79d4989f0a14967f23efaaac48cc`
- `lucene-character-categories.tsv`: `136920d4d3aed398a573338b1afd92925ab471d2d4383ecf5c0ba1f29810d247`

JSword's Greek and Hebrew analyzers construct `StandardTokenizer(Version.LUCENE_29)`. Lucene 3.6.2
therefore selects its generated `ClassicTokenizerImpl`, rather than the newer standard scanner.
`lucene-classic-tokenizer-2.9.bin` is a lossless extraction of that class's `ZZ_CMAP`, `ZZ_ACTION`,
`ZZ_ROWMAP`, `ZZ_TRANS`, and `ZZ_ATTRIBUTE` arrays from the verified `lucene-core-3.6.2.jar` above.
It is consumed by a bounds-checked Swift DFA runner and is covered by the same Apache License 2.0.

- `lucene-classic-tokenizer-2.9.bin`: `6c1a9c85cae3d24644247aef57bf79353b43dbb131fb9ea30366d479940d9bd7`

## mmseg4j Dictionary

- Maven coordinate: `com.chenlb.mmseg4j:mmseg4j-dic:1.8.6`
- Source project declared by the parent POM: `https://github.com/chenlb/mmseg4j-from-googlecode`
- Dictionary JAR SHA-256: `b1d226e818b0f3d692668b585063852953ae58f20b04a1e5955b589f1fc5181c`
- License declared by the 1.8.6 parent POM: Apache License 2.0, reproduced in
  `LICENSE.apache-2.0.txt`.
- Extraction: the following files are byte-for-byte copies of the JAR's `data/` entries:
  - `mmseg4j/chars.dic`: `ef3353113808b01437fd10bcbc7e889715c650be6be7afec003e11ed55f24e9c`
  - `mmseg4j/words.dic`: `91c438f781771d6ac744ab1f205bc4650ee79593636d05bb96aa8d3c93f3022e`
  - `mmseg4j/units.dic`: `ed9abe99d933f5753827c822ef0218aecdd1187416526c1ba1cc46186142840f`

## Snowball C Sources

The generated C sources in `Sources/SearchStemmers/snowball` are copied from PostgreSQL 9.2.4's
vendored Snowball runtime and stemmers. The source archive SHA-256 is
`f0840edcfc7780c44079740921cf9b51b37ae2835580edf478e8f03841166a2f`.

License: the Snowball 3-clause BSD license is reproduced at
`Sources/SearchStemmers/LICENSE.snowball`. The adapter in `SearchStemmers.c` is project code.

The ten vendored stemmers were exhaustively compared with Lucene 3.6.2's official
`TestSnowballVocabData.zip`: all 336,125 input/output pairs matched (Danish 23,829; Dutch 45,669;
Finnish 50,000; French 20,403; Italian 35,494; Norwegian 20,628; Portuguese 32,016; Russian
49,673; Spanish 28,390; Swedish 30,623).

## Thai BreakIterator Resources

JSword's `ThaiLuceneAnalyzer` instantiates Lucene 3.6.2 `ThaiWordFilter`, which delegates word
boundaries to `BreakIterator.getWordInstance(new Locale("th"))`. The iOS implementation does not use
Foundation or the device ICU version. It directly ports the OpenJDK forward rule/dictionary state
machines and bundles the exact Eclipse Temurin 17.0.19+10 resources used by the Java oracle:

- Docker image digest:
  `eclipse-temurin@sha256:068a8f9ae4b74d9a20de3ecca771ba1a6437f1d7a8a8ad6deaf9dbdd2274397a`
- OpenJDK source tag: `jdk-17.0.19-ga` (`jdk17u`)
- `openjdk17/WordBreakIteratorData_th`:
  `49931ec22a95d0f9f18fda1ab80e68e3f0105b433e87a0be9312066f3e16b57f`
- `openjdk17/thai_dict`:
  `c5129c3875aecbdf860005efb4a720d94eddbd19e5ea63bbc1f0d378a9482e7a`

The OpenJDK state-machine source is GPLv2 with the Classpath Exception. The distributed GPL text and
Oracle additional-license notice are reproduced as `openjdk17/LICENSE.openjdk-gpl2.txt` and
`openjdk17/ADDITIONAL_LICENSE_INFO.openjdk.txt`. The Thai dictionary's permissive Royal Institute /
NECTEC license is reproduced verbatim as `openjdk17/LICENSE.thai-dictionary.md`.

## Oracle Fixture

`Sources/BibleCore/Tests/BibleCoreTests/Fixtures/search/jsword-lucene-3.6.2-oracle.json` was generated
from the pinned JSword classes and the exact Maven artifacts above under the JSword test locale
`en_US`. It records analyzer tokens and Lucene `Query.toString()` output. The fixture is test-only;
it does not replace runtime dictionaries or algorithms.
