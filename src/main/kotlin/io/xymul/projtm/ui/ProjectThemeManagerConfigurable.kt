package io.xymul.projtm.ui

import com.intellij.CommonBundle
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Disposer
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBLabel
import com.intellij.ui.table.JBTable
import io.xymul.projtm.core.ConfigRepository
import io.xymul.projtm.core.projectPathOf
import io.xymul.projtm.model.ProjectEntry
import io.xymul.projtm.model.Scene
import io.xymul.projtm.theme.ActiveSceneService
import io.xymul.projtm.theme.SceneSwitcher
import io.xymul.projtm.theme.ThemeService
import java.awt.BorderLayout
import java.awt.Component
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.ListSelectionModel
import javax.swing.table.TableCellRenderer

private const val SCENES_COLUMN = 1

// Application level page that lists every project known to the plugin.
class ProjectThemeManagerConfigurable : Configurable {

    private val model = ProjectsTableModel()
    private val table = JBTable(model)
    private val disposable = Disposer.newDisposable("ProjectThemeManager")
    private val onRepositoryChanged: () -> Unit = { reset() }

    override fun getDisplayName(): String = ProjectThemeManagerBundle.message("settings.displayName")

    override fun createComponent(): JComponent {
        table.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        table.setShowGrid(false)
        table.columnModel.getColumn(0).cellRenderer = ProjectNameRenderer()
        table.columnModel.getColumn(2).cellEditor = ComboBoxCellEditor { row -> model.sceneNames(row) }
        table.columnModel.getColumn(3).cellEditor = ComboBoxCellEditor { _ -> installedThemes() }
        table.addMouseListener(scenePopupListener())
        val decorated = ToolbarDecorator.createDecorator(table)
            .setAddAction { addProject() }
            .setRemoveAction { removeProject() }
            .setAddActionName(ProjectThemeManagerBundle.message("settings.button.add"))
            .setRemoveActionName(ProjectThemeManagerBundle.message("settings.button.remove"))
            .setRemoveActionUpdater { table.selectedRow >= 0 }
            .createPanel()
        val panel = JPanel(BorderLayout())
        panel.add(JBLabel(ProjectThemeManagerBundle.message("settings.parentHint")), BorderLayout.NORTH)
        panel.add(decorated, BorderLayout.CENTER)
        ConfigRepository.getInstance().addChangeListener(onRepositoryChanged)
        ThemeService.getInstance().subscribe(disposable) { reset() }
        reset()
        return panel
    }

    override fun isModified(): Boolean {
        val repository = ConfigRepository.getInstance()
        return model.rows().any { row -> row.config != repository.scenesOf(row.entry) }
    }

    override fun apply() {
        val repository = ConfigRepository.getInstance()
        model.rows().forEach { row ->
            if (row.config != repository.scenesOf(row.entry)) repository.updateScenes(row.entry, row.config)
        }
        applyCurrentProjectTheme()
    }

    override fun reset() {
        val repository = ConfigRepository.getInstance()
        val current = currentEntry()
        val rows = repository.projects().map { ProjectRow(it, repository.scenesOf(it)) }
        model.setData(
            rows.sortedWith(compareBy({ it.entry.uuid != current?.uuid }, { it.entry.name.lowercase() })),
            current?.uuid,
        )
    }

    override fun disposeUIResources() {
        ConfigRepository.getInstance().removeChangeListener(onRepositoryChanged)
        Disposer.dispose(disposable)
    }

    private fun currentEntry(): ProjectEntry? {
        val project = currentProjectOf(table) ?: return null
        val path = projectPathOf(project) ?: return null
        return ConfigRepository.getInstance().findProject(path)
    }

    private fun addProject() {
        val repository = ConfigRepository.getInstance()
        val open = ProjectManager.getInstance().openProjects.mapNotNull { projectPathOf(it) }
        val dialog = NewProjectDialog((repository.projects().map { it.path } + open).distinct())
        if (!dialog.showAndGet()) return
        val path = dialog.chosenPath() ?: return
        repository.addProject(dialog.projectName(), path, dialog.initialScene())
        reset()
    }

    private fun removeProject() {
        val index = table.selectedRow
        if (index < 0) return
        val row = model.rowAt(index)
        if (!confirmRemoval(row)) return
        ConfigRepository.getInstance().removeProject(row.entry)
        reset()
    }

    // Uses the platform dialog that offers a do not ask again checkbox.
    private fun confirmRemoval(row: ProjectRow): Boolean {
        val repository = ConfigRepository.getInstance()
        if (!repository.settings().confirmOnDelete) return true
        var askAgain = true
        val handler = java.util.function.BiFunction<Int, JCheckBox, Int> { choice, checkbox ->
            askAgain = !checkbox.isSelected
            choice
        }
        val answer = Messages.showCheckboxMessageDialog(
            ProjectThemeManagerBundle.message("settings.remove.confirm.message", row.entry.name),
            ProjectThemeManagerBundle.message("settings.remove.confirm.title"),
            arrayOf(ProjectThemeManagerBundle.message("settings.button.remove"), CommonBundle.getCancelButtonText()),
            ProjectThemeManagerBundle.message("settings.remove.confirm.checkbox"),
            true, 0, 0, Messages.getWarningIcon(),
            handler,
        )
        if (!askAgain) repository.updateSettings(repository.settings().copy(confirmOnDelete = false))
        return answer == 0
    }

    private fun scenePopupListener(): MouseAdapter = object : MouseAdapter() {
        override fun mouseClicked(event: MouseEvent) {
            val row = table.rowAtPoint(event.point)
            if (row < 0 || table.columnAtPoint(event.point) != SCENES_COLUMN) return
            showScenesPopup(row)
        }
    }

    // Centered chooser, the entries use the same padding as the quick switch popup.
    private fun showScenesPopup(row: Int) {
        val names = model.sceneNames(row)
        if (names.isEmpty()) return
        showSceneChooser(names, model.rowAt(row).entry.name, null, table) { chosen -> editScene(row, chosen) }
    }

    private fun editScene(row: Int, sceneName: String) {
        val projectRow = model.rowAt(row)
        val scene = projectRow.config.scenes.firstOrNull { it.name == sceneName } ?: return
        val dialog = SceneDialog(projectRow.config.scenes.map { it.name }.toSet() - sceneName, scene)
        if (!dialog.showAndGet()) return
        val themeName = dialog.themeName() ?: scene.theme
        val newName = dialog.sceneName()
        projectRow.config = projectRow.config.copy(
            defaultScene = if (projectRow.config.defaultScene == sceneName) newName else projectRow.config.defaultScene,
            scenes = projectRow.config.scenes.map { current ->
                if (current.name == sceneName) Scene(newName, themeName) else current
            },
        )
        model.refreshRow(row)
    }

    // Applies the new default theme when the current project is on that scene.
    private fun applyCurrentProjectTheme() {
        val project = currentProjectOf(table) ?: return
        val path = projectPathOf(project) ?: return
        val repository = ConfigRepository.getInstance()
        val entry = repository.findProject(path) ?: return
        val config = repository.scenesOf(entry)
        val state = ActiveSceneService.getInstance(project)
        if (state.applied && state.activeScene != config.defaultScene) return
        config.defaultScene?.let { SceneSwitcher.switchTo(project, it) }
    }
}

// Shows the project name in bold followed by the grayed out absolute path.
private class ProjectNameRenderer : TableCellRenderer {

    private val component = SimpleColoredComponent()

    override fun getTableCellRendererComponent(
        table: JTable,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int,
    ): Component {
        component.clear()
        val entry = value as? ProjectEntry
        if (entry != null) {
            component.append(entry.name, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
            component.append("  " + entry.path, SimpleTextAttributes.GRAYED_ATTRIBUTES)
            component.toolTipText = entry.path
        }
        component.isOpaque = true
        component.background = if (isSelected) table.selectionBackground else table.background
        component.foreground = if (isSelected) table.selectionForeground else table.foreground
        return component
    }
}
