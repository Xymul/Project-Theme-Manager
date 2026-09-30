package io.xymul.projtm.model

data class Scene(
    val name: String,
    val theme: String,
)

data class ProjectSceneConfig(
    val name: String,
    val path: String,
    val uuid: String,
    val defaultScene: String?,
    val scenes: List<Scene>,
)

data class ProjectEntry(
    val name: String,
    val path: String,
    val uuid: String,
    val scenesFile: String,
)

data class PluginSettings(
    val confirmOnDelete: Boolean = true,
)

data class ProjectsDocument(
    val schema: Int = 1,
    val pluginVersion: String,
    val ideName: String,
    val ideVersion: String,
    val settings: PluginSettings,
    val projects: List<ProjectEntry>,
)
