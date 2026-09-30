package io.xymul.projtm.ui

import io.xymul.projtm.model.ProjectEntry
import io.xymul.projtm.model.ProjectSceneConfig
import io.xymul.projtm.theme.ThemeItem
import javax.swing.table.AbstractTableModel

// One row of the project table: the registry entry plus its pending scene configuration.
class ProjectRow(val entry: ProjectEntry, var config: ProjectSceneConfig)

class ProjectsTableModel : AbstractTableModel() {

    private var rows = emptyList<ProjectRow>()
    private var currentUuid: String? = null

    fun setData(newRows: List<ProjectRow>, current: String?) {
        rows = newRows
        currentUuid = current
        fireTableDataChanged()
    }

    fun rows(): List<ProjectRow> = rows

    fun rowAt(index: Int): ProjectRow = rows[index]

    fun indexOf(row: ProjectRow): Int = rows.indexOf(row)

    fun refreshRow(index: Int) = fireTableRowsUpdated(index, index)

    fun sceneNames(row: Int): List<String> = rows[row].config.scenes.map { it.name }.sorted()

    fun isCurrent(row: Int): Boolean = rows[row].entry.uuid == currentUuid

    override fun getRowCount(): Int = rows.size

    override fun getColumnCount(): Int = 4

    override fun getColumnName(column: Int): String = when (column) {
        0 -> ProjectThemeManagerBundle.message("settings.column.project")
        1 -> ProjectThemeManagerBundle.message("settings.column.scenes")
        2 -> ProjectThemeManagerBundle.message("settings.column.defaultScene")
        else -> ProjectThemeManagerBundle.message("settings.column.defaultTheme")
    }

    override fun getColumnClass(columnIndex: Int): Class<*> = Any::class.java

    override fun isCellEditable(row: Int, column: Int): Boolean = column >= 2

    override fun getValueAt(row: Int, column: Int): Any? {
        val projectRow = rows[row]
        return when (column) {
            0 -> projectRow.entry
            1 -> sceneSummary(projectRow)
            2 -> projectRow.config.defaultScene
            3 -> defaultSceneOf(projectRow)?.let { themeItem(it.theme) }
            else -> null
        }
    }

    override fun setValueAt(value: Any?, row: Int, column: Int) {
        val projectRow = rows[row]
        when (column) {
            2 -> projectRow.config = projectRow.config.copy(defaultScene = value as? String)
            3 -> {
                val theme = value as? ThemeItem ?: return
                val name = projectRow.config.defaultScene ?: return
                projectRow.config = projectRow.config.copy(
                    scenes = projectRow.config.scenes.map { scene ->
                        if (scene.name == name) scene.copy(theme = theme.name) else scene
                    },
                )
            }
            else -> return
        }
        fireTableRowsUpdated(row, row)
    }

    private fun defaultSceneOf(projectRow: ProjectRow) =
        projectRow.config.scenes.firstOrNull { it.name == projectRow.config.defaultScene }

    private fun sceneSummary(projectRow: ProjectRow): String {
        val summary = projectRow.config.scenes.joinToString("; ") { it.name }
        return if (summary.length > 48) summary.take(45) + "..." else summary
    }
}
