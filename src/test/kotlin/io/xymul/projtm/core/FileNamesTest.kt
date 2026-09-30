package io.xymul.projtm.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FileNamesTest {

    @Test
    fun sanitizeSegmentReplacesIllegalCharacters() {
        assertEquals("a-b-c", sanitizeSegment("a<>b:c"))
    }

    @Test
    fun sanitizeSegmentTrimsAndCollapses() {
        assertEquals("demo", sanitizeSegment("  demo  "))
        assertEquals("a-b", sanitizeSegment("a??b"))
    }

    @Test
    fun versionSegmentConvertsDotsToDashes() {
        assertEquals("2026-2-3", versionSegment("2026.2.3"))
    }

    @Test
    fun scenesFileNameFollowsTheRequiredPattern() {
        assertEquals(
            "Demo_IntelliJ-IDEA_2026-2-3_demo-1f3a9c7b_ws.xml",
            scenesFileName("Demo", "IntelliJ IDEA", "2026.2.3", "demo-1f3a9c7b"),
        )
    }

    @Test
    fun scenesFileNameStaysInsideTheLengthLimit() {
        val name = scenesFileName("x".repeat(400), "IntelliJ IDEA", "2026.2.3", "demo-1f3a9c7b")
        assertTrue(name.length <= MAX_FILE_NAME_LENGTH, "length was ${name.length}")
    }

    @Test
    fun scenesFileNameFallsBackForUnusableNames() {
        assertEquals("project_ide_0-0-0_id_ws.xml", scenesFileName("***", "***", "", "id"))
    }
}
