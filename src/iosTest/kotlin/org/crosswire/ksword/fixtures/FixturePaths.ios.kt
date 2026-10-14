@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.crosswire.ksword.fixtures

import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSTemporaryDirectory
import platform.posix.getenv
import platform.posix.mkdtemp

// Retain the exclusively owned root for the runner lifetime; never mutate a shared fixed directory.
private val provisionedRoot: Path by lazy { provisionFixtureDirectory() }

// The CLI Native runner has no resource bundle. Materialize test-only embedded bytes as real files.
internal fun provisionFixtureDirectory(): Path {
    val root = memScoped {
        val template = (NSTemporaryDirectory() + "ksword-k4-modules-XXXXXX").cstr.getPointer(this)
        checkNotNull(mkdtemp(template)) { "Cannot create an exclusive k4 fixture directory" }.toKString().toPath()
    }
    val fs = FileSystem.SYSTEM
    for ((name, bytes) in K4FixtureResources.files()) {
        val file = root / name
        fs.createDirectories(file.parent!!)
        fs.write(file) { write(bytes) }
    }
    check(fs.exists(root / "manifest.tsv")) { "Missing fixture manifest at $root" }
    return root
}

actual fun fixtureRoot(): Path = provisionedRoot
internal actual val isJvm: Boolean = false
internal actual fun fixtureEnvironment(name: String): String? = getenv(name)?.toKString()
