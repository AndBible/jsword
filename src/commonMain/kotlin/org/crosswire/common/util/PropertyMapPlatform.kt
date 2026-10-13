package org.crosswire.common.util

internal expect fun legacyPropertyEntries(bytes: ByteArray): List<Pair<String, String>>
internal expect fun legacyPropertyBytes(map: PropertyMap, comments: String?): ByteArray
