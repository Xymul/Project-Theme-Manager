package io.xymul.projtm.ui

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBLabel
import com.intellij.ui.table.JBTable
import io.xymul.projtm.core.ConfigRepository
import io.xymul.projtm.model.ProjectEntry
import io.xymul.projtm.model.ProjectSceneConfig
import io.xymul.projtm.model.Scene
import io.xymul.projtm.theme.ActiveSceneService
import io.xymul.projtm.theme.ThemeService
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.ListSelectionModel

// Child page that shows and edits the work scenes of the current project.
class CurrentProjectConfigurable : Configurable {

    private val model = ScenesTableModel()
    private val table = JBTable(model)
    private val projectLabel = JBLabel()
    private val hintLabel = JBLabel()
    private val disposable = Disposer.newDisposable("ProjectThemeManager")
    private val onRepositoryChanged: () -> Unit = { reset() }

    override fun getDisplayName(): String =
        currentProject()?.name ?: ProjectThemeManagerBundle.message("settings.displayName")

    override fun createComponent(): JComponent {
        table.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        table.setShowGrid(false)
        table.columnModel.getColumn(1).cellEditor = ComboBoxCellEditor { _ -> installedThemes() }
        table.addMouseListener(sceneClickListener())
        val header = JPanel(BorderLayout())
        header.add(projectLabel, BorderLayout.WEST)
        header.add(hintLabel, BorderLayout.EAST)
        val decorated = ToolbarDecorator.createDecorator(table)
            .setAddAction { addScene() }
            .setRemoveAction { removeScene() }
            .setAddActionName(ProjectThemeManagerBundle.message("settings.button.addScene"))
            .setRemoveActionName(ProjectThemeManagerBundle.message("settings.button.removeScene"))
            .setRemoveActionUpdater { table.selectedRow >= 0 }
            .createPanel()
        val panel = JPanel(BorderLayout())
        panel.add(header, BorderLayout.NORTH)
        panel.add(decorated, BorderLayout.CENTER)
        ConfigRepository.getInstance().addChangeListener(onRepositoryChanged)
        ThemeService.getInstance().subscribe(disposable) { reset() }
        reset()
        return panel
    }

    override fun isModified(): Boolean {
        val project = currentProject() ?: return model.scenes().isNotEmpty()
        val entry = entryFor(project) ?: return model.scenes().isNotEmpty()
        val stored = ConfigRepository.getInstance().scenesOf(entry)
        return stored.scenes != model.scenes() || stored.defaultScene != model.defaultName()
    }

    override fun apply() {
        val project = currentProject() ?: return
        val path = projectPathOf(project) ?: return
        val repository = ConfigRepository.getInstance()
        val entry = entryFor(project) ?: run {
            val first = model.scenes().firstOrNull() ?: return
            repository.addProject(project.name, path, first)
        }
        val config = ProjectSceneConfig(entry.name, path, entry.uuid, model.defaultName(), model.scenes())
        repository.updateScenes(entry, config)
        applyDefaultTheme(project, config)
    }

    override fun reset() {
        val repository = ConfigRepository.getInstance()
        val project = currentProject()
        if (project == null) {
            projectLabel.text = ProjectThemeManagerBundle.message("settings.page.noProject")
            hintLabel.text = ""
            model.setData(emptyList(), null)
            table.isEnabled = false
            return
        }
        val path = projectPathOf(project).orEmpty()
        val entry = repository.findProject(path)
        projectLabel.text = project.name + " (" + path + ")"
        hintLabel.text = if (entry == null) ProjectThemeManagerBundle.message("settings.page.notRegistered") else ""
        val config = entry?.let { repository.scenesOf(it) }
        model.setData(config?.scenes.orEmpty(), config?.defaultScene)
        table.isEnabled = true
    }

    override fun disposeUIResources() {
        ConfigRepository.getInstance().removeChangeListener(onRepositoryChanged)
        Disposer.dispose(disposable)
    }

    private fun addScene() {
        val dialog = SceneDialog(model.names())
        if (!dialog.showAndGet()) return
        val service = ThemeService.getInstance()
        val themeId = dialog.themeId() ?: service.currentThemeId() ?: return
        model.add(Scene(dialog.sceneName(), themeId, service.isDark(themeId)))
    }

    private fun removeScene() {
        val row = table.selectedRow
        if (row >= 0) model.remove(row)
    }

    private fun sceneClickListener(): MouseAdapter = object : MouseAdapter() {
        override fun mouseClicked(event: MouseEvent) {
            val row = table.rowAtPoint(event.point)
            if (row >= 0 && event.clickCount == 2) editScene(row)
        }
    }

    private fun editScene(row: Int) {
        val scene = model.sceneAt(row)
        val dialog = SceneDialog(model.names() - scene.name, scene)
        if (!dialog.showAndGet()) return
        val themeId = dialog.themeId() ?: scene.themeId
        model.update(row, Scene(dialog.sceneName(), themeId, ThemeService.getInstance().isDark(themeId)))
    }

    // Switches the theme right away when the current project is on the edited scene.
    private fun applyDefaultTheme(project: Project, config: ProjectSceneConfig) {
        val state = ActiveSceneService.getInstance(project)
        if (state.applied && state.activeScene != config.defaultScene) return
        val scene = config.scenes.firstOrNull { it.name == config.defaultScene } ?: return
        if (ThemeService.getInstance().applyTheme(scene.themeId)) state.markApplied(scene.name)
    }

    private fun currentProject(): Project? = currentProjectOf(table)

    private fun entryFor(project: Project): ProjectEntry? {
        val path = projectPathOf(project) ?: return null
        return ConfigRepository.getInstance().findProject(path)
    }
}
