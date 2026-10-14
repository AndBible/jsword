package org.crosswire.ksword.fixtures

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

actual fun fixtureRoot(): Path {
    val root = checkNotNull(System.getProperty("ksword.fixturesDir")) {
        "ksword.fixturesDir must point to the frozen k4-modules corpus"
    }.toPath()
    check(FileSystem.SYSTEM.exists(root / "manifest.tsv")) { "Missing fixture manifest at $root" }
    return root
}
internal actual val isJvm: Boolean = true
internal actual fun fixtureEnvironment(name: String): String? = System.getenv(name)
