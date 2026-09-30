package io.xymul.projtm.core

import io.xymul.projtm.model.PluginSettings
import io.xymul.projtm.model.ProjectEntry
import io.xymul.projtm.model.ProjectSceneConfig
import io.xymul.projtm.model.ProjectsDocument
import io.xymul.projtm.model.Scene
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class XmlRoundTripTest {

    @Test
    fun projectsDocumentSurvivesAWriteAndReadCycle() {
        val document = ProjectsDocument(
            pluginVersion = "1.0.0",
            ideName = "IntelliJ IDEA",
            ideVersion = "2026.2.3",
            settings = PluginSettings(confirmOnDelete = false),
            projects = listOf(ProjectEntry("Demo", "D:/IdeaProjects/Demo", "demo-1f3a9c7b", "Demo_ws.xml")),
        )
        val file = Files.createTempDirectory("ptm").resolve("projects.xml")
        writeDocumentAtomically(serializeProjects(document), file)

        val parsed = parseProjects(assertNotNull(parseDocument(file)))
        assertEquals(document, parsed)
    }

    @Test
    fun workScenesDocumentSurvivesAWriteAndReadCycle() {
        val config = ProjectSceneConfig(
            name = "Demo",
            path = "D:/IdeaProjects/Demo",
            uuid = "demo-1f3a9c7b",
            defaultScene = "write",
            scenes = listOf(
                Scene("default", "Darcula"),
                Scene("write", "falcon-relax-light-green"),
            ),
        )
        val file = Files.createTempDirectory("ptm").resolve("Demo_ws.xml")
        writeDocumentAtomically(serializeWorkScenes(config, "1.0.0"), file)

        val parsed = parseWorkScenes(assertNotNull(parseDocument(file)))
        assertEquals(config, parsed)
    }

    @Test
    fun legacyThemeIdAttributeIsMigratedToTheThemeName() {
        val file = Files.createTempDirectory("ptm").resolve("Legacy_ws.xml")
        Files.writeString(
            file,
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <workScenes schema="1">
                <plugin_version>1.0.0</plugin_version>
                <project>
                    <name>Legacy</name>
                    <path>D:/Legacy</path>
                    <uuid>legacy-1</uuid>
                </project>
                <scenes default="default">
                    <scene name="default" themeId="Darcula" dark="true"/>
                </scenes>
            </workScenes>
            """.trimIndent(),
        )

        val parsed = parseWorkScenes(assertNotNull(parseDocument(file)))
        assertEquals(listOf(Scene("default", "Darcula")), parsed?.scenes)
    }

    @Test
    fun missingAndBrokenFilesAreReportedAsNull() {
        val directory = Files.createTempDirectory("ptm")
        assertNull(parseDocument(directory.resolve("missing.xml")))

        val broken = directory.resolve("broken.xml")
        Files.writeString(broken, "<projectThemeManager")
        assertNull(parseDocument(broken))
    }
}
