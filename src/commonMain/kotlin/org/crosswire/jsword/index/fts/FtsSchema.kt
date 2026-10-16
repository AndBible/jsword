package org.crosswire.jsword.index.fts

/**
 * Bumped on any incompatible change of [FtsSchema]; an index file with another value is rebuilt.
 *
 * It must also be bumped on ANY change of analyzer output (token text or positions): [FtsIndexMetadata.analyzerId]
 * names the analyzer chain but does not version its code, so an analyzer change without a bump leaves existing indexes
 * silently stale (indexed with the old tokens, queried with the new ones). Guard: `PdFixtureSearchTest` pins the
 * analyzer's token output for the public-domain fixtures and fails when that output changes; when it does, bump this
 * value in the same commit.
 */
const val FTS_SCHEMA_VERSION = 1

/**
 * Identity of an index file, stored in table `meta`; used to decide whether it is still valid. [analyzerId] names the
 * analyzer chain only, it does not version analyzer code: see [FTS_SCHEMA_VERSION].
 */
data class FtsIndexMetadata(
    val schemaVersion: Int,
    val analyzerId: String,
    val codec: String,
    val bookInitials: String,
    val bookVersion: String?,
    val sourceFingerprint: String,
)

/**
 * The SQLite schema of one book's index.
 *
 * `entry_fts.tokens` holds the encoded tokens ([SearchTokenCodec]) joined by single spaces, one FTS5 position per
 * token. Analyzer positions are absolute and not always consecutive (ClassicTokenizer-based Greek/Hebrew/Thai skip
 * positions after a dropped over-long token; the first position can be above 0). To keep FTS5 phrase/NEAR distances
 * equal to analyzer position distances, each missing position (including leading ones) is stored as the reserved
 * gap token [GAP]. [GAP] cannot equal an encoded token (those start with `x`).
 */
internal object FtsSchema {
    const val GAP = "g"

    val statements = listOf(
        "CREATE VIRTUAL TABLE entry_fts USING fts5(tokens, tokenize='ascii')",
        "CREATE TABLE entry(rowid INTEGER PRIMARY KEY, osis_ref TEXT NOT NULL)",
        "CREATE TABLE strongs(token TEXT NOT NULL, rowid INTEGER NOT NULL, PRIMARY KEY(token, rowid)) WITHOUT ROWID",
        "CREATE VIRTUAL TABLE entry_vocab USING fts5vocab(entry_fts, 'row')",
        "CREATE TABLE meta(key TEXT PRIMARY KEY, value TEXT)",
    )
}
