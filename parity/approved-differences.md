# Approved parity differences

Intentional differences between the KSword baseline (oracle) and the current code. Each entry: parity
test and case, the difference, the reason, the approving review. Anything not listed here fails
`parityTest`.

## Search (K6, `SearchCorpusParityTest`)

`SearchCorpusParityTest` compares, for every corpus module and every query of `SearchQuerySets.kt`, the verse
set (or the failure kind) of the baseline Lucene 3.6.2 search with the FTS5 search. An entry here is either a
rule (`RULE <id>`, recognized by `approvedRule` in the test, which states the exact shape of both outcomes) or one
case (`DIFF <Module> <query as a JSON string>`, a difference of that single query). Approved differences are
printed (`K6 APPROVED ...`) and pass; nothing else may differ. Searches that the UI sends (all / any / phrase,
trailing `*`, `strong:`, the `+[range]` suffix) have no entry here; any such difference is a defect or goes to the
user first (spec 6.3).

### Rules

- RULE K6-3-unsupported-syntax: `field:x` for any field other than `strong:` (`heading:x`, `note:x`, `a:b`), `^` boosts
  (`a^2`, `(a b)^2`, `"a b"^3`) and Lucene term ranges (`{a TO b}`, `[a TO b]` when not taken by the JSword range
  layer). Baseline: accepts and returns results (or fails in Lucene's parser). Here: `UnsupportedSearchSyntaxException`.
  Reason: decision K6-3 (the dropped Lucene syntax fails with a typed, user-visible error, never a silent wrong
  result; no UI or docs path uses these forms). Approved: user, 2026-10-16 (spec K6-3).
- RULE strong-phrase-or-fuzzy: `strong:"a b c"` (a quoted multi-number phrase) and `strong:term~` (fuzzy on the
  Strong's field). Baseline: accepts. Here: `UnsupportedSearchSyntaxException`. Reason: the Strong's field is an
  exact-number field in the new index; a phrase over several numbers and a fuzzy Strong's number have no UI or docs
  path (the "find all" link of a Strong's number sends `strong:<number>`). Approved: with K6-3, spec 6.3.
- RULE malformed-range-bracket: a `[` or `{` that JSword's range layer leaves in the Lucene text, e.g. a second range
  `+[Gen-Mal] +[Mat-Rev] a` (the layer removes only the first) or an unbalanced `[a`. Baseline: Lucene's parser fails
  (`BookException`, cause `ParseException`). Here: `UnsupportedSearchSyntaxException`, because the bracket starts the
  dropped Lucene range form (K6-3). Reason: both fail with a typed `BookException`; only the message differs. The UI
  never sends a second range, and a single `[Book]` range is handled by the unchanged JSword layer.
- RULE fuzzy-similarity-at-least-1: `term~1`, `term~2`, `term~1.5` (similarity of 1 or more). Baseline: Lucene's
  `FuzzyQuery` throws a raw `IllegalArgumentException` (minimumSimilarity >= 1). Here: the usual typed
  `BookException` ("Search failed.", cause `QueryParseException`). Reason: both fail, the baseline with an untyped
  exception that escapes JSword's `BookException` contract; review focus 5 requires a typed failure. The user
  sees the same "search failed" either way.
- RULE nesting-over-200: parenthesis nesting deeper than 200 levels. Baseline: works down to about 2,000 levels
  and overflows its stack at about 3,000. Here: a typed `BookException` (cause `QueryParseException`) above 200
  levels, so a pasted pathological query cannot overflow the stack on any platform (the iOS stack is smaller).
  Reason: robustness; no real query nests beyond a handful of levels.

### Known differences outside the query matrix (documented, not exercised by it)

- Ranked search (`SearchModifier.isRanked == true`): baseline ranks by Lucene score; here `UnsupportedSearchSyntaxException("ranked")`.
  Reason: K6-3; no UI path sets it.
- General books and genbook / tree-keyed books are indexed non-recursively. The baseline's recursive tree walk
  overflowed the stack on real modules; searching them still fails like the baseline (`VerseCollector` cannot
  collect a non-verse key; review focus 3).
- Thai word breaking: Android and the desktop JVM keep the platform word breaker (`java.text.BreakIterator` for `th`:
  ICU data on Android, JDK data on the JVM, as the baseline did), iOS ships the ported OpenJDK 17 dictionary data
  (`ThaiBreakPort`). The JVM build equals the baseline on the corpus (`ThaiPortVsJdkTest`, the ThaiKJV matrix);
  Android versus iOS may break rare words differently. Platform note (spec: Thai, builder call), not a code defect.

### Cases

_None yet._
