package org.crosswire.jsword.index.fts

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.driver.bundled.SQLITE_OPEN_READONLY
import androidx.sqlite.execSQL
import okio.FileSystem
import okio.Path
import org.crosswire.jsword.index.analysis.AnalyzedToken

/**
 * One book's search index in a SQLite FTS5 file (schema in [FtsSchema]).
 *
 * Build: [create], [beginBuild], [addEntry]*, [writeMetadata], [commitBuild] (one transaction). Query: [match],
 * [strongs], [vocabulary], [tokensOf], ... The FTS5 expressions passed to [match] are over encoded tokens
 * ([SearchTokenCodec.encode]); the query evaluator ([org.crosswire.jsword.index.fts.query.FtsQueryEvaluator]) builds them.
 *
 * Positions: [addEntry] takes analyzer tokens with absolute positions and stores a gap token for every missing
 * position, so FTS5 phrase and NEAR distances equal analyzer position distances (a phrase "a b" does not match
 * a, gap, b; NEAR with enough slop does). [tokensOf] therefore returns [AnalyzedToken]s whose `position` is the
 * stream position (gaps are simply absent from the list), not the list index. [vocabulary] never contains the gap.
 * The empty term is stored as `x` like any other term.
 */
internal class FtsIndexStore private constructor(private val db: SQLiteConnection) : AutoCloseable {
    private var insertFts: SQLiteStatement? = null
    private var insertEntry: SQLiteStatement? = null
    private var insertStrongs: SQLiteStatement? = null
    private var building = false

    /** Starts the single build transaction. */
    fun beginBuild() {
        check(!building) { "build already started" }
        db.execSQL("BEGIN")
        building = true
        insertFts = db.prepare("INSERT INTO entry_fts(rowid, tokens) VALUES(?, ?)")
        insertEntry = db.prepare("INSERT INTO entry(rowid, osis_ref) VALUES(?, ?)")
        insertStrongs = db.prepare("INSERT OR IGNORE INTO strongs(token, rowid) VALUES(?, ?)")
    }

    /**
     * Adds one entry. [tokens] need strictly increasing positions (all analyzers satisfy this); [strongs] are the
     * Strong's tokens of the entry (stored verbatim in table `strongs`, not in the FTS table).
     */
    // A huge position jump writes one gap token per missing position (no cap; unrealistic for the analyzers).
    fun addEntry(rowId: Long, osisRef: String, tokens: List<AnalyzedToken>, strongs: List<String>) {
        check(building) { "beginBuild() not called" }
        val sb = StringBuilder()
        var next = 0
        for (t in tokens) {
            require(t.position >= next) { "positions must be strictly increasing: ${t.position} after ${next - 1}" }
            while (next < t.position) { if (sb.isNotEmpty()) sb.append(' '); sb.append(FtsSchema.GAP); next++ }
            if (sb.isNotEmpty()) sb.append(' ')
            sb.append(SearchTokenCodec.encode(t.text))
            next++
        }
        exec(insertFts!!) { bindLong(1, rowId); bindText(2, sb.toString()) }
        exec(insertEntry!!) { bindLong(1, rowId); bindText(2, osisRef) }
        for (s in strongs) exec(insertStrongs!!) { bindText(1, s); bindLong(2, rowId) }
    }

    fun writeMetadata(meta: FtsIndexMetadata) {
        val values = mapOf(
            "schemaVersion" to meta.schemaVersion.toString(), "analyzerId" to meta.analyzerId, "codec" to meta.codec,
            "bookInitials" to meta.bookInitials, "bookVersion" to meta.bookVersion, "sourceFingerprint" to meta.sourceFingerprint,
        )
        db.prepare("INSERT OR REPLACE INTO meta(key, value) VALUES(?, ?)").use { st ->
            for ((k, v) in values) {
                if (v == null) continue
                st.reset(); st.bindText(1, k); st.bindText(2, v); st.step()
            }
        }
        if (meta.bookVersion == null) db.prepare("DELETE FROM meta WHERE key='bookVersion'").use { it.step() }
    }

    fun commitBuild() {
        check(building) { "beginBuild() not called" }
        insertFts?.close(); insertEntry?.close(); insertStrongs?.close()
        insertFts = null; insertEntry = null; insertStrongs = null
        db.execSQL("COMMIT")
        building = false
    }

    fun metadata(): FtsIndexMetadata = readMetadata(db) ?: error("index metadata missing or incomplete")

    /** Rowids of the entries matching the FTS5 [ftsExpression] over encoded tokens. */
    fun match(ftsExpression: String): Set<Long> = longs("SELECT rowid FROM entry_fts WHERE entry_fts MATCH ?", ftsExpression)

    /** Rowids of the entries containing Strong's [token]. */
    fun strongs(token: String): Set<Long> = longs("SELECT rowid FROM strongs WHERE token = ?", token)

    /** Rowids of the entries containing any of the Strong's [tokens] (looked up in chunks, one statement each). */
    fun strongsAny(tokens: Collection<String>): Set<Long> {
        val out = LinkedHashSet<Long>()
        for (chunk in tokens.chunked(500)) {
            val sql = "SELECT rowid FROM strongs WHERE token IN (" + chunk.joinToString(",") { "?" } + ")"
            db.prepare(sql).use { st ->
                chunk.forEachIndexed { i, t -> st.bindText(i + 1, t) }
                while (st.step()) out += st.getLong(0)
            }
        }
        return out
    }

    /** Eager: all rows are read inside the call (memory O(vocabulary)) and only then wrapped as a [Sequence]. */
    fun strongsVocabulary(): Sequence<String> = strings("SELECT DISTINCT token FROM strongs ORDER BY token").asSequence()

    /**
     * Every distinct decoded term (never the gap token), via `fts5vocab`. Eager like [strongsVocabulary]
     * (memory O(vocabulary)); terms that fail to decode are silently dropped (mapNotNull).
     */
    fun vocabulary(): Sequence<String> =
        strings("SELECT term FROM entry_vocab ORDER BY term").asSequence().filter { it != FtsSchema.GAP }.mapNotNull { SearchTokenCodec.decode(it) }

    /** The decoded tokens of [rowId] with their stream positions (gaps are visible as jumps in `position`). */
    fun tokensOf(rowId: Long): List<AnalyzedToken> {
        val stored = db.prepare("SELECT tokens FROM entry_fts WHERE rowid = ?").use { st ->
            st.bindLong(1, rowId)
            if (st.step()) st.getText(0) else return emptyList()
        }
        if (stored.isEmpty()) return emptyList() // entry added with no tokens
        val out = ArrayList<AnalyzedToken>()
        stored.split(' ').forEachIndexed { pos, enc ->
            if (enc != FtsSchema.GAP) out += AnalyzedToken(SearchTokenCodec.decode(enc) ?: error("corrupt token '$enc'"), pos)
        }
        return out
    }

    fun osisRef(rowId: Long): String = db.prepare("SELECT osis_ref FROM entry WHERE rowid = ?").use { st ->
        st.bindLong(1, rowId)
        check(st.step()) { "no entry $rowId" }
        st.getText(0)
    }

    fun allRowIds(): Set<Long> = longs("SELECT rowid FROM entry")

    override fun close() {
        if (building) runCatching { db.execSQL("ROLLBACK") }
        insertFts?.close(); insertEntry?.close(); insertStrongs?.close()
        db.close()
    }

    private fun exec(st: SQLiteStatement, bind: SQLiteStatement.() -> Unit) {
        st.reset(); st.bind(); st.step()
    }

    private fun longs(sql: String, arg: String? = null): Set<Long> = db.prepare(sql).use { st ->
        if (arg != null) st.bindText(1, arg)
        val r = LinkedHashSet<Long>()
        while (st.step()) r += st.getLong(0)
        r
    }

    private fun strings(sql: String): List<String> = db.prepare(sql).use { st ->
        val r = ArrayList<String>()
        while (st.step()) r += st.getText(0)
        r
    }

    companion object {
        /** A fresh index file at [path] (an existing file is replaced) with the schema created. */
        fun create(path: Path): FtsIndexStore {
            FileSystem.SYSTEM.delete(path)
            val db = BundledSQLiteDriver().open(path.toString())
            try {
                for (s in FtsSchema.statements) db.execSQL(s)
            } catch (e: Throwable) { db.close(); throw e }
            return FtsIndexStore(db)
        }

        /**
         * The store at [path], or null when the file is missing, not an index, or its metadata is absent or has another
         * schema version. With [readOnly] the file is opened `SQLITE_OPEN_READONLY` (metadata-only checks must not be
         * able to write or create anything); the build methods then fail.
         */
        fun open(path: Path, readOnly: Boolean = false): FtsIndexStore? {
            if (!FileSystem.SYSTEM.exists(path)) return null
            val db = try {
                if (readOnly) BundledSQLiteDriver().open(path.toString(), SQLITE_OPEN_READONLY) else BundledSQLiteDriver().open(path.toString())
            } catch (e: Exception) { return null }
            val ok = try { readMetadata(db)?.schemaVersion == FTS_SCHEMA_VERSION } catch (e: Exception) { false }
            if (!ok) { db.close(); return null }
            return FtsIndexStore(db)
        }

        private fun readMetadata(db: SQLiteConnection): FtsIndexMetadata? {
            val m = HashMap<String, String>()
            db.prepare("SELECT key, value FROM meta").use { st -> while (st.step()) m[st.getText(0)] = st.getText(1) }
            return FtsIndexMetadata(
                schemaVersion = m["schemaVersion"]?.toIntOrNull() ?: return null,
                analyzerId = m["analyzerId"] ?: return null,
                codec = m["codec"] ?: return null,
                bookInitials = m["bookInitials"] ?: return null,
                bookVersion = m["bookVersion"],
                sourceFingerprint = m["sourceFingerprint"] ?: return null,
            )
        }
    }
}
