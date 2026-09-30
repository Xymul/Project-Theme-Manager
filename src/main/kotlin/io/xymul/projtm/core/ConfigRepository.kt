package io.xymul.projtm.core

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import io.xymul.projtm.model.PluginSettings
import io.xymul.projtm.model.ProjectEntry
import io.xymul.projtm.model.ProjectSceneConfig
import io.xymul.projtm.model.ProjectsDocument
import io.xymul.projtm.model.Scene
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

const val NOTIFICATION_GROUP_ID = "ProjectThemeManager"

// Single writer for projects.xml and for the files inside the WorkScenes directory.
@Service(Service.Level.APP)
class ConfigRepository {

    private val writeLock = ReentrantLock()
    private val listeners = CopyOnWriteArrayList<() -> Unit>()
    private val sceneCache = HashMap<String, ProjectSceneConfig>()
    private var document: ProjectsDocument? = null

    companion object {
        fun getInstance(): ConfigRepository =
            ApplicationManager.getApplication().getService(ConfigRepository::class.java)
    }

    fun projects(): List<ProjectEntry> = document().projects

    fun settings(): PluginSettings = document().settings

    fun findProject(path: String): ProjectEntry? {
        val normalized = normalizePath(path)
        return projects().firstOrNull { normalizePath(it.path) == normalized }
    }

    fun addChangeListener(listener: () -> Unit) = listeners.add(listener)

    fun removeChangeListener(listener: () -> Unit) = listeners.remove(listener)

    // Registers a project and writes its first work scene file.
    fun addProject(name: String, path: String, initialScene: Scene): ProjectEntry {
        val used = projects().map { it.uuid }.toSet()
        val uuid = deduplicateId(readableId(name, path), used)
        val entry = ProjectEntry(name, path, uuid, scenesFileName(name, ideName(), ideVersion(), uuid))
        val config = ProjectSceneConfig(name, path, uuid, initialScene.name, listOf(initialScene))
        sceneCache[uuid] = config
        updateDocument { it.copy(projects = it.projects + entry) }
        persist {
            writeWorkScenes(entry, config)
            writeProjects()
        }
        return entry
    }

    // Drops the registry entry first and deletes the scene file afterwards.
    fun removeProject(entry: ProjectEntry) {
        sceneCache.remove(entry.uuid)
        updateDocument { it.copy(projects = it.projects.filterNot { project -> project.uuid == entry.uuid }) }
        persist {
            writeProjects()
            deleteWorkScenes(entry)
        }
    }

    fun updateScenes(entry: ProjectEntry, config: ProjectSceneConfig) {
        sceneCache[entry.uuid] = config
        updateDocument { document ->
            document.copy(projects = document.projects.map { project ->
                if (project.uuid == entry.uuid) project.copy(name = config.name, path = config.path) else project
            })
        }
        persist {
            writeWorkScenes(entry, config)
            writeProjects()
        }
    }

    fun scenesOf(entry: ProjectEntry): ProjectSceneConfig {
        sceneCache[entry.uuid]?.let { return it }
        val parsed = parseDocument(scenesFile(entry))?.let { parseWorkScenes(it) }
            ?: ProjectSceneConfig(entry.name, entry.path, entry.uuid, null, emptyList())
        sceneCache[entry.uuid] = parsed
        return parsed
    }

    fun updateSettings(settings: PluginSettings) {
        updateDocument { it.copy(settings = settings) }
        persist { writeProjects() }
    }

    // Drops the in memory state so the next read hits the disk again.
    fun reload() {
        synchronized(this) {
            document = null
            sceneCache.clear()
        }
        notifyListeners()
    }

    private fun document(): ProjectsDocument = synchronized(this) {
        document ?: loadFromDisk().also { document = it }
    }

    private fun loadFromDisk(): ProjectsDocument {
        val file = projectsFile()
        parseDocument(file)?.let { parsed -> parseProjects(parsed)?.let { return it } }
        if (Files.isRegularFile(file)) backupBrokenFile(file)
        return ProjectsDocument(
            pluginVersion = pluginVersion(),
            ideName = ideName(),
            ideVersion = ideVersion(),
            settings = PluginSettings(),
            projects = emptyList(),
        )
    }

    private fun backupBrokenFile(file: java.nio.file.Path) {
        runCatching {
            Files.copy(file, file.resolveSibling(file.fileName.toString() + ".bak"), StandardCopyOption.REPLACE_EXISTING)
        }
        notify("Could not read " + file.fileName + ", a copy was kept next to it.", NotificationType.WARNING)
    }

    private fun updateDocument(block: (ProjectsDocument) -> ProjectsDocument) {
        synchronized(this) { document = block(document()) }
        notifyListeners()
    }

    private fun notifyListeners() {
        val application = ApplicationManager.getApplication()
        if (application == null) {
            listeners.forEach { listener -> listener() }
            return
        }
        application.invokeLater { listeners.forEach { listener -> listener() } }
    }

    private fun persist(block: () -> Unit) {
        val application = ApplicationManager.getApplication()
        if (application == null) {
            runPersist(block)
            return
        }
        application.executeOnPooledThread { runPersist(block) }
    }

    private fun runPersist(block: () -> Unit) = writeLock.withLock {
        try {
            block()
        } catch (throwable: Throwable) {
            notify("Could not save the project theme configuration: " + throwable.message, NotificationType.ERROR)
        }
    }

    private fun writeProjects() = writeDocumentAtomically(serializeProjects(document()), projectsFile())

    private fun writeWorkScenes(entry: ProjectEntry, config: ProjectSceneConfig) =
        writeDocumentAtomically(serializeWorkScenes(config, pluginVersion()), scenesFile(entry))

    private fun deleteWorkScenes(entry: ProjectEntry) {
        val file = scenesFile(entry)
        if (file.parent != workScenesDir()) return
        Files.deleteIfExists(file)
    }

    private fun scenesFile(entry: ProjectEntry) = workScenesDir().resolve(entry.scenesFile)

    private fun notify(content: String, type: NotificationType) {
        if (ApplicationManager.getApplication() == null) return
        runCatching {
            NotificationGroupManager.getInstance().getNotificationGroup(NOTIFICATION_GROUP_ID)
                .createNotification("Project Theme Manager", content, type)
                .notify(null)
        }
    }
}
