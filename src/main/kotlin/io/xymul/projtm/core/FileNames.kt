package io.xymul.projtm.core

import java.util.Locale

private val ILLEGAL_CHARS = Regex("[^A-Za-z0-9._-]+")
private val REPEATED_DASHES = Regex("-{2,}")

const val MAX_FILE_NAME_LENGTH = 180

// Replaces everything that is not allowed inside a file name with a single dash.
fun sanitizeSegment(raw: String): String =
    ILLEGAL_CHARS.replace(raw, "-").replace(REPEATED_DASHES, "-").trim('-', '.', ' ')

// 2026.2.3 -> 2026-2-3
fun versionSegment(fullVersion: String): String =
    sanitizeSegment(fullVersion.replace('.', '-')).ifBlank { "0-0-0" }

// <project>_<ide>_<version>_<uuid>_ws.xml, shortened so the whole file name stays below the limit.
fun scenesFileName(projectName: String, ideName: String, ideVersion: String, uuid: String): String {
    val ide = sanitizeSegment(ideName).take(32).ifBlank { "ide" }
    val version = versionSegment(ideVersion).take(16)
    val id = sanitizeSegment(uuid).take(64)
    val suffix = "_${ide}_${version}_${id}_ws.xml"
    val project = sanitizeSegment(projectName).take(48).ifBlank { "project" }
    val room = (MAX_FILE_NAME_LENGTH - suffix.length).coerceAtLeast(1)
    return project.take(room) + suffix
}

// Lower cased name that is safe to use inside a generic file name.
fun fileNameSafeName(raw: String): String =
    sanitizeSegment(raw).lowercase(Locale.ROOT).ifBlank { "project" }
