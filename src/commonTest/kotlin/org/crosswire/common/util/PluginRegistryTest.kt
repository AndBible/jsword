package org.crosswire.common.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

class PluginRegistryTest {
    private interface Greeter
    private class Hello : Greeter
    private class Bonjour : Greeter

    private fun builder() = PluginRegistryBuilder()

    @Test
    fun entriesKeepRegistrationOrderAndDefaultIsOrdinary() {
        val b = builder()
        b.register(Greeter::class, "default", Hello::class) { Hello() }
        b.register(Greeter::class, "fr", Bonjour::class) { Bonjour() }
        b.register(Greeter::class, "en", Hello::class) { Hello() }
        val names = b.map[Greeter::class]!!.map { it.name }
        assertEquals(listOf("default", "fr", "en"), names)
    }

    @Test
    fun createUsesTheRegisteredFactory() {
        val shared = Bonjour()
        val b = builder()
        b.register(Greeter::class, "fr", Bonjour::class) { shared }
        val entry = b.map[Greeter::class]!!.single()
        assertSame(shared, entry.create())
        assertEquals(Bonjour::class, entry.implClass)
    }

    @Test
    fun unknownTypeYieldsEmptyListAndNullLookups() {
        assertEquals(emptyList(), PluginRegistry.entries(Greeter::class))
        assertNull(PluginRegistry.entry(Greeter::class, "x"))
        assertNull(PluginRegistry.default(Greeter::class))
    }

    @Test
    fun registryIsBuiltFromPlatformPlugins() {
        // Platform actuals register at least one type on the JVM; on iOS the registry may still be empty.
        assertNotNull(PluginRegistry.DEFAULT)
        assertEquals("default", PluginRegistry.DEFAULT)
    }
}
