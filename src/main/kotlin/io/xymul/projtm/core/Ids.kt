package io.xymul.projtm.core

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.Locale
import java.util.UUID

// Normalized absolute path with forward slashes, lower cased because Windows paths are case insensitive.
fun normalizePath(path: String): String = runCatching {
    Path.of(path).toAbsolutePath().normalize().toString()
}.getOrElse { path }
    .replace('\\', '/')
    .lowercase(Locale.ROOT)
    .removeSuffix("/")

// Eight character hash that is stable for one normalized path.
fun pathHash8(normalizedPath: String): String =
    UUID.nameUUIDFromBytes("projtm:$normalizedPath".toByteArray(StandardCharsets.UTF_8))
        .toString().replace("-", "").take(8)

// Readable identifier built from the project name and the path hash, e.g. demo-1f3a9c7b.
fun readableId(projectName: String, path: String): String {
    val name = sanitizeSegment(projectName).lowercase(Locale.ROOT).take(24).ifBlank { "project" }
    return "$name-${pathHash8(normalizePath(path))}"
}

// Appends an index when a path hash collides with an identifier that is already in use.
fun deduplicateId(id: String, used: Set<String>): String {
    if (id !in used) return id
    var index = 2
    while ("$id-$index" in used) index++
    return "$id-$index"
}
