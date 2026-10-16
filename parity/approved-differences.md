# Approved parity differences

Intentional differences between the KSword baseline (oracle) and the current code. Each entry: parity
test and case, the difference, the reason, the approving review. Anything not listed here fails
`parityTest`.

## Search (K6, `SearchCorpusParityTest`)

`SearchCorpusParityTest` compares, for every corpus module and every query of `SearchQuerySets.kt`, the verse
set (or the failure kind) of the baseline Lucene 3.6.2 search with the FTS5 search. An entry here is either a
rule (`RULE <id>`, recognized by `approvedRule` in the test, which states the exact shape of both outcomes) or one
case (`DIFF <Module> <query as a JSON string>`, a difference of that single query). Approved differences are
printed (`K6 APPROVED ...`) and pass; nothing else may differ. File format (read by the test): a `DIFF <Module> "<query as a JSON string>"` entry must start the line (no bullet, no
indent); `RULE <id>` entries are substring-matched, so keep the `RULE <id>:` wording. Rules are recognized by the
parser's reported syntax (`UnsupportedSearchSyntaxException.syntax`) or the failure kinds, not by query text. Bracket
input that is not a complete `[a TO b]` range is a parse error in both implementations and needs no entry. The test
caches indexes in `jsword/build/k6-indexes` (keyed by module hash and index-shaping classes); run
`rm -rf jsword/build/k6-indexes` before a gate run. Searches that the UI sends (all / any / phrase,
trailing `*`, `strong:`, the `+[range]` suffix) have no entry here; any such difference is a defect or goes to the
user first (spec 6.3).

### Rules

- RULE K6-3-unsupported-syntax: `field:x` for any field other than `strong:` (`heading:x`, `note:x`, `a:b`), `^` boosts
  (`a^2`, `(a b)^2`, `"a b"^3`) and Lucene term ranges (`{a TO b}`, `[a TO b]` when not taken by the JSword range
  layer). Baseline: accepts and returns results (or fails in Lucene's parser). Here: `UnsupportedSearchSyntaxException`.
  Reason: decision K6-3 (the dropped Lucene syntax fails with a typed, user-visible error, never a silent wrong
  result; no UI or docs path uses these forms). Approved: spec K6-3 (user decision, 2026-10-16).
- RULE strong-phrase-or-fuzzy: `strong:"a b c"` (a quoted multi-number phrase) and `strong:term~` (fuzzy on the
  Strong's field). Baseline: accepts. Here: `UnsupportedSearchSyntaxException`. Reason: the Strong's field is an
  exact-number field in the new index; a phrase over several numbers and a fuzzy Strong's number have no UI or docs
  path (the "find all" link of a Strong's number sends `strong:<number>`). Approved: Task 12/14 review; follows spec K6-3; no ordinary UI search affected.
- RULE fuzzy-similarity-at-least-1: `term~1` and `term~1.0` (a similarity of exactly 1). Baseline: Lucene's `FuzzyQuery`
  throws a raw `IllegalArgumentException` (minimumSimilarity >= 1) from the parser. Here: the usual typed
  `BookException` ("Search failed.", cause `QueryParseException`). Reason: both fail, the baseline with an untyped
  exception that escapes JSword's `BookException` contract; review focus 5 requires a typed failure. The user
  sees the same "search failed" either way. `term~2` and `term~1.5` (similarity above 1) are not a difference: the
  baseline already rejects them with a `ParseException`, and the port mirrors that (`LuceneSyntaxParser`, fuzzy
  similarity check). Approved: Task 11/14 review; no ordinary UI search affected.
- RULE nesting-over-200: parenthesis nesting deeper than 200 levels. Baseline: works down to about 2,000 levels
  and overflows its stack at about 3,000. Here: a typed `BookException` (cause `QueryParseException`) above 200
  levels, so a pasted pathological query cannot overflow the stack on any platform (the iOS stack is smaller). Approved: Task 11/14 review; no ordinary UI search affected.
  Reason: robustness; no real query nests beyond a handful of levels.
- RULE strongs-wildcard-case: `strong:H72*`, `strong:G*`, `strong:h*` (a wildcard or prefix on the Strong's field).
  Baseline: the parser lowercases the pattern while the indexed Strong's tokens are uppercase, so it returns 0
  verses. Here: the pattern is uppercased and matches the tokens case-insensitively. Reason: the baseline result is
  an evident defect, not a behavior to keep. Approved: user, 2026-10-17 ("cheap, so do it").

### Approximations (no difference observed)

- An unquoted `strong:` term that analyzes to several tokens is evaluated by intersecting the result sets of its
  tokens (`FtsQueryEvaluator`), an approximation of the baseline's phrase-like handling. No difference was observed on
  the matrix.

### Reproduced baseline quirks (not differences)


### Known differences outside the query matrix (documented, not exercised by it)

- Ranked search (`SearchModifier.isRanked == true`): baseline ranks by Lucene score; here `UnsupportedSearchSyntaxException("ranked")`.
  Reason: K6-3; no UI path sets it.
- General books and genbook / tree-keyed books are indexed non-recursively. The baseline's recursive tree walk
  overflowed the stack on real modules; searching them still fails like the baseline (`VerseCollector` cannot
  collect a non-verse key; review focus 3).
- Thai word breaking: Android and the desktop JVM keep the platform word breaker (`java.text.BreakIterator` for `th`:
  ICU data on Android, JDK data on the JVM, as the baseline did), iOS ships the ported OpenJDK 17 dictionary data
  (`ThaiBreakPort`, compiled into iOS binaries only). The JVM build equals the baseline on the corpus (`ThaiPortVsJdkTest`, the ThaiKJV matrix);
  Android versus iOS may break rare words differently. Platform note (spec: Thai, builder call), not a code defect.

### Outside the matrix

- Turkish and Azerbaijani device locales: the baseline lowercases wildcard and prefix terms with
  `String.toLowerCase()` in the default locale, so a dotless-i mismatch makes `ISA*` match nothing on tr/az devices.
  K6 lowercases locale-invariantly and finds results (an improvement). The matrix runs in en_US and cannot see it.
  Approved by controller ruling.

### Cases

_None yet._
