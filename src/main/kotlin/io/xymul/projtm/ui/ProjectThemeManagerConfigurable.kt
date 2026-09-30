package io.xymul.projtm.ui

import com.intellij.CommonBundle
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Disposer
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBLabel
import com.intellij.ui.table.JBTable
import io.xymul.projtm.core.ConfigRepository
import io.xymul.projtm.core.projectPathOf
import io.xymul.projtm.model.ProjectEntry
import io.xymul.projtm.model.ProjectSceneConfig
import io.xymul.projtm.model.Scene
import io.xymul.projtm.theme.SceneSwitcher
import io.xymul.projtm.theme.ThemeItem
import io.xymul.projtm.theme.ThemeService
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.ListSelectionModel
import javax.swing.table.TableCellRenderer

private const val SCENES_COLUMN = 1

// Soft highlight of the current project row, tuned for light and dark themes.
private val CURRENT_PROJECT_BACKGROUND = JBColor(0xE8F1FD, 0x2F3B4E)

// Application level page that lists every project known to the plugin.
class ProjectThemeManagerConfigurable : Configurable {

    private val model = ProjectsTableModel()

    // The current project is pinned to the first row and keeps its highlight in every column.
    private val table = highlightedTable()

    private fun highlightedTable(): JBTable {
        val projects = model
        return object : JBTable(projects) {
            override fun prepareRenderer(renderer: TableCellRenderer, row: Int, column: Int): Component {
                val component = super.prepareRenderer(renderer, row, column)
                if (!isRowSelected(row) && projects.isCurrent(row)) component.background = CURRENT_PROJECT_BACKGROUND
                return component
            }
        }
    }
    private val disposable = Disposer.newDisposable("ProjectThemeManager")
    private val onRepositoryChanged: () -> Unit = { reset() }

    // The project of the current window, used to pin and highlight its row.
    private val statusLabel = JBLabel()

    override fun getDisplayName(): String = ProjectThemeManagerBundle.message("settings.displayName")

    override fun createComponent(): JComponent {
        table.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        table.setShowGrid(false)
        table.columnModel.getColumn(0).cellRenderer = ProjectNameRenderer(model)
        // All editable columns use the same drop down, choosing a scene opens its editor.
        table.columnModel.getColumn(SCENES_COLUMN).cellEditor =
            ComboBoxCellEditor<String>({ row -> model.sceneNames(row) }) { row, name -> editScene(row, name) }
        table.columnModel.getColumn(2).cellEditor = ComboBoxCellEditor<String>({ row -> model.sceneNames(row) })
        table.columnModel.getColumn(3).cellEditor = ComboBoxCellEditor<ThemeItem>({ _ -> installedThemes() })
        val decorated = ToolbarDecorator.createDecorator(table)
            .setAddAction { addProject() }
            .setRemoveAction { removeProject() }
            .setAddActionName(ProjectThemeManagerBundle.message("settings.button.add"))
            .setRemoveActionName(ProjectThemeManagerBundle.message("settings.button.remove"))
            .setRemoveActionUpdater { table.selectedRow >= 0 }
            .createPanel()
        val header = JPanel(BorderLayout())
        header.add(JBLabel(ProjectThemeManagerBundle.message("settings.parentHint")), BorderLayout.WEST)
        header.add(statusLabel, BorderLayout.EAST)
        val panel = JPanel(BorderLayout())
        panel.add(header, BorderLayout.NORTH)
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
        val stored = currentEntry()?.let { repository.scenesOf(it) }
        model.rows().forEach { row ->
            if (row.config != repository.scenesOf(row.entry)) repository.updateScenes(row.entry, row.config)
        }
        applyCurrentProjectTheme(stored)
    }

    override fun reset() {
        val repository = ConfigRepository.getInstance()
        val project = currentProjectOf(table)
        val current = currentEntry()
        statusLabel.text = statusText(project, current)
        val rows = repository.projects().map { ProjectRow(it, repository.scenesOf(it)) }
        model.setData(
            rows.sortedWith(compareBy({ it.entry.uuid != current?.uuid }, { it.entry.name.lowercase() })),
            current?.uuid,
        )
    }

    // Tells the user which project this page treats as the current one.
    private fun statusText(project: Project?, entry: ProjectEntry?): String = when {
        project == null -> ProjectThemeManagerBundle.message("settings.page.noProject")
        entry == null -> ProjectThemeManagerBundle.message("settings.page.currentProjectNotConfigured", project.name)
        else -> ProjectThemeManagerBundle.message("settings.page.currentProject", project.name)
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

    // Applies the new default theme when the default scene or its theme has changed.
    private fun applyCurrentProjectTheme(stored: ProjectSceneConfig?) {
        val project = currentProjectOf(table) ?: return
        val path = projectPathOf(project) ?: return
        val repository = ConfigRepository.getInstance()
        val entry = repository.findProject(path) ?: return
        val config = repository.scenesOf(entry)
        if (!SceneSwitcher.shouldApply(project, stored, config)) return
        config.defaultScene?.let { SceneSwitcher.switchTo(project, it) }
    }
}

// Shows the project name followed by the grayed out absolute path, the current project stays bold.
private class ProjectNameRenderer(private val model: ProjectsTableModel) : TableCellRenderer {

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
            val nameAttributes = if (model.isCurrent(row)) {
                SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES
            } else {
                SimpleTextAttributes.REGULAR_ATTRIBUTES
            }
            component.append(entry.name, nameAttributes)
            component.append("  " + entry.path, SimpleTextAttributes.GRAYED_ATTRIBUTES)
            component.toolTipText = entry.path
        }
        component.isOpaque = true
        component.background = if (isSelected) table.selectionBackground else table.background
        component.foreground = if (isSelected) table.selectionForeground else table.foreground
        return component
    }
}
