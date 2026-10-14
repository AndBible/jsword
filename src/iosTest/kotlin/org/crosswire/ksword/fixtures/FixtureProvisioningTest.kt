package org.crosswire.ksword.fixtures

import okio.FileSystem
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class FixtureProvisioningTest {
    @Test fun independentClaimsProvisionExactResourcesWithoutSharingFiles() {
        val first = provisionFixtureDirectory()
        val second = provisionFixtureDirectory()
        val fs = FileSystem.SYSTEM
        assertNotEquals(first, second)
        assertTrue(fs.metadata(first).isDirectory)
        assertTrue(fs.metadata(second).isDirectory)
        for ((name, bytes) in K4FixtureResources.files()) {
            assertContentEquals(bytes, fs.read(first / name) { readByteArray() }, "first $name")
            assertContentEquals(bytes, fs.read(second / name) { readByteArray() }, "second $name")
        }
        fs.write(first / "ownership-probe") { writeUtf8("first only") }
        assertTrue(!fs.exists(second / "ownership-probe"))
        assertEquals(fixtureRoot(), fixtureRoot(), "runner keeps its provisioned root")
    }
}
