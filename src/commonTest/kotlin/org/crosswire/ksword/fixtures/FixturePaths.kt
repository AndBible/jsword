package org.crosswire.ksword.fixtures

import okio.Path

/** Root of the frozen, on-disk synthetic module corpus; missing resources are an error. */
expect fun fixtureRoot(): Path
internal expect val isJvm: Boolean
internal expect fun fixtureEnvironment(name: String): String?
