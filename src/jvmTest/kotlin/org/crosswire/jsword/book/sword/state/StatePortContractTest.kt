package org.crosswire.jsword.book.sword.state

import org.crosswire.common.util.RandomAccessSource
import org.crosswire.jsword.book.sword.SwordUtil
import org.junit.Assert.assertEquals
import org.junit.Test

class StatePortContractTest {
    @Test fun stateHandlesAndPublicHelpersUseRandomAccessSource() {
        val getters = listOf(
            RawBackendState::class.java to "getOtTextRaf",
            RawFileBackendState::class.java to "getNtTextRaf",
            RawLDBackendState::class.java to "getIdxRaf",
            ZLDBackendState::class.java to "getZdxRaf",
            GenBookBackendState::class.java to "getBdtRaf",
            ZVerseBackendState::class.java to "getCompRaf"
        )
        getters.forEach { (type, name) ->
            assertEquals(type.name, RandomAccessSource::class.java,
                type.methods.single { it.name == name }.returnType)
        }
        assertEquals(RandomAccessSource::class.java,
            SwordUtil::class.java.methods.single { it.name == "readRAF" }.parameterTypes[0])
    }
}
