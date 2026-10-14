package org.crosswire.common.compress

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ZipTest {
    @Test fun testCompression() {
        val compressed = Zip(compressionTestCorpus()).compress()
        val result = Zip(compressed).uncompress().decodeToString()
        assertTrue(Regex("          \r?\nThe First Book of Moses, called Genesis").containsMatchIn(result), "round trip ZIP uncompression")
    }
}
