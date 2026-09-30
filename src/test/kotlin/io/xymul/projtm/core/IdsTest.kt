package io.xymul.projtm.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class IdsTest {

    @Test
    fun normalizePathUsesForwardSlashesAndLowerCase() {
        assertEquals("d:/ideaprojects/demo", normalizePath("D:\\IdeaProjects\\Demo"))
    }

    @Test
    fun readableIdIsStableForTheSamePath() {
        assertEquals(readableId("Demo", "D:/IdeaProjects/Demo"), readableId("Demo", "D:\\IdeaProjects\\Demo\\"))
    }

    @Test
    fun readableIdStartsWithTheProjectName() {
        assertTrue(readableId("Demo", "D:/work/demo").startsWith("demo-"))
    }

    @Test
    fun readableIdDiffersForDifferentPaths() {
        assertNotEquals(readableId("Demo", "D:/a"), readableId("Demo", "D:/b"))
    }

    @Test
    fun deduplicateIdAppendsAnIndexOnCollision() {
        assertEquals("demo-1f3a9c7b", deduplicateId("demo-1f3a9c7b", emptySet()))
        assertEquals("demo-1f3a9c7b-2", deduplicateId("demo-1f3a9c7b", setOf("demo-1f3a9c7b")))
        assertEquals(
            "demo-1f3a9c7b-3",
            deduplicateId("demo-1f3a9c7b", setOf("demo-1f3a9c7b", "demo-1f3a9c7b-2")),
        )
    }
}
