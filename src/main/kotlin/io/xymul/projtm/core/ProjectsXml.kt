package io.xymul.projtm.core

import io.xymul.projtm.model.PluginSettings
import io.xymul.projtm.model.ProjectEntry
import io.xymul.projtm.model.ProjectsDocument
import org.w3c.dom.Document

const val PROJECTS_SCHEMA_VERSION = 1

// Builds the projects.xml document described in the requirements.
fun serializeProjects(document: ProjectsDocument): Document {
    val xml = newDocument(ROOT_NAME, mapOf(SCHEMA_ATTRIBUTE to document.schema.toString()))
    val root = xml.rootElement()
    xml.childElement(root, "plugin_version", document.pluginVersion)
    val editor = xml.childElement(root, "editor")
    xml.childElement(editor, "name", document.ideName)
    xml.childElement(editor, "version", document.ideVersion)
    val settings = xml.childElement(root, "settings")
    xml.childElement(settings, "confirmOnDelete", document.settings.confirmOnDelete.toString())
    document.projects.forEach { project ->
        xml.childElement(
            root, "project",
            attributes = mapOf("scenes" to project.scenesFile),
        ).also { element ->
            xml.childElement(element, "name", project.name)
            xml.childElement(element, "path", project.path)
            xml.childElement(element, "uuid", project.uuid)
        }
    }
    return xml
}

// Reads projects.xml, returns null when the root element is not ours.
fun parseProjects(xml: Document): ProjectsDocument? {
    val root = xml.rootElement()
    if (root.nodeName != ROOT_NAME) return null
    val editor = root.firstChildElement("editor")
    val settings = root.firstChildElement("settings")
    val projects = root.childElements("project").mapNotNull { element ->
        val name = element.childText("name") ?: return@mapNotNull null
        val path = element.childText("path") ?: return@mapNotNull null
        val uuid = element.childText("uuid") ?: return@mapNotNull null
        ProjectEntry(
            name = name,
            path = path,
            uuid = uuid,
            scenesFile = element.getAttribute("scenes").orEmpty(),
        )
    }
    return ProjectsDocument(
        schema = xml.rootElement().getAttribute(SCHEMA_ATTRIBUTE).toIntOrNull() ?: PROJECTS_SCHEMA_VERSION,
        pluginVersion = root.childText("plugin_version").orEmpty(),
        ideName = editor?.childText("name").orEmpty(),
        ideVersion = editor?.childText("version").orEmpty(),
        settings = PluginSettings(
            confirmOnDelete = settings?.childText("confirmOnDelete")?.toBooleanStrictOrNull() ?: true,
        ),
        projects = projects,
    )
}

private const val ROOT_NAME = "projectThemeManager"
private const val SCHEMA_ATTRIBUTE = "schema"
