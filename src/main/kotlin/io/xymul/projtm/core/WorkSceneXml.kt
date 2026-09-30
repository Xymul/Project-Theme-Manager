package io.xymul.projtm.core

import io.xymul.projtm.model.ProjectSceneConfig
import io.xymul.projtm.model.Scene
import org.w3c.dom.Document

const val WORK_SCENES_SCHEMA_VERSION = 1

// Builds one WorkScenes/*_ws.xml document.
fun serializeWorkScenes(config: ProjectSceneConfig, pluginVersion: String): Document {
    val xml = newDocument(ROOT_NAME, mapOf(SCHEMA_ATTRIBUTE to WORK_SCENES_SCHEMA_VERSION.toString()))
    val root = xml.rootElement()
    xml.childElement(root, "plugin_version", pluginVersion)
    val project = xml.childElement(root, "project")
    xml.childElement(project, "name", config.name)
    xml.childElement(project, "path", config.path)
    xml.childElement(project, "uuid", config.uuid)
    val scenes = xml.childElement(
        root,
        SCENES_NAME,
        attributes = config.defaultScene?.let { mapOf(DEFAULT_ATTRIBUTE to it) } ?: emptyMap(),
    )
    config.scenes.forEach { scene ->
        xml.childElement(
            scenes, "scene",
            attributes = mapOf(
                "name" to scene.name,
                "themeId" to scene.themeId,
                "dark" to scene.dark.toString(),
            ),
        )
    }
    return xml
}

// Reads one WorkScenes/*_ws.xml document, returns null when the root element is not ours.
fun parseWorkScenes(xml: Document): ProjectSceneConfig? {
    val root = xml.rootElement()
    if (root.nodeName != ROOT_NAME) return null
    val project = root.firstChildElement("project") ?: return null
    val name = project.childText("name") ?: return null
    val path = project.childText("path") ?: return null
    val uuid = project.childText("uuid") ?: return null
    val scenesElement = root.firstChildElement(SCENES_NAME)
    val scenes = scenesElement?.childElements("scene")?.mapNotNull { element ->
        val sceneName = element.getAttribute("name").takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val themeId = element.getAttribute("themeId")
        Scene(
            name = sceneName,
            themeId = themeId,
            dark = element.getAttribute("dark").toBooleanStrictOrNull() ?: false,
        )
    }.orEmpty()
    return ProjectSceneConfig(
        name = name,
        path = path,
        uuid = uuid,
        defaultScene = scenesElement?.getAttribute(DEFAULT_ATTRIBUTE)?.takeIf { it.isNotBlank() },
        scenes = scenes,
    )
}

private const val ROOT_NAME = "workScenes"
private const val SCENES_NAME = "scenes"
private const val SCHEMA_ATTRIBUTE = "schema"
private const val DEFAULT_ATTRIBUTE = "default"
