package org.crosswire.jsword.book.install

import okio.Path.Companion.toPath
import org.crosswire.common.util.IniSection
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/** Opt-in, actual downloaded catalog census; no network access or synthetic fallback. */
class CompressTypeCensus {
    @Test fun configuredCatalogs() {
        assumeTrue(System.getProperty("k4.census") == "true")
        val manifest = File(System.getenv("K4_CENSUS_MANIFEST") ?: error("K4_CENSUS_MANIFEST required"))
        val seen = mutableMapOf<String, MutableSet<String>>()
        for (row in manifest.readLines()) {
            val fields = row.split('\t')
            val repo = fields[0]
            if (fields[5] != "0") {
                println("K4CENSUS $repo UNAVAILABLE ${fields[6]}")
                continue
            }
            val output = File.createTempFile("k4-census-", ".zip")
            try {
                SwordArchive.tarGzConfsToZip(fields[4].toPath(), output.path.toPath())
                val records = mutableListOf<Pair<String, String>>()
                for ((_, bytes) in SwordArchive.readConfZip(output.path.toPath())) {
                    val ini = IniSection()
                    ini.load(bytes, "UTF-8")
                    val initial = ini.name ?: error("Missing module section")
                    val type = ini.get("CompressType") ?: "ABSENT"
                    records.add(initial to type)
                    seen.getOrPut(initial) { mutableSetOf() }.add(repo)
                }
                for ((type, modules) in records.groupBy { it.second }.toSortedMap()) {
                    println("K4CENSUS $repo $type ${modules.size}")
                    if (type.equals("BZIP2", true) || type.equals("XZ", true))
                        println("K4CENSUS_INITIALS $repo $type ${modules.map { it.first }.sorted().joinToString(",")}")
                }
                println("K4CENSUS_RECORDS $repo ${records.size} UNIQUE_INITIALS ${records.map { it.first }.toSet().size}")
                for ((initial, duplicates) in records.groupBy { it.first }.filterValues { it.size > 1 })
                    println("K4CENSUS_DUPLICATE $repo $initial ${duplicates.size}")
            } finally {
                output.delete()
            }
        }
        for ((initial, repos) in seen.filterValues { it.size > 1 }.toSortedMap())
            println("K4CENSUS_SHARED $initial ${repos.sorted().joinToString("|")}")
        println("K4CENSUS_UNIQUE_INITIALS ${seen.size}")
    }
}
