package io.xymul.projtm.ui

import com.intellij.openapi.ui.ComboBox
import java.awt.Component
import java.awt.event.ItemEvent
import javax.swing.AbstractCellEditor
import javax.swing.JTable
import javax.swing.table.TableCellEditor

// Drop down cell editor that reads its items every time it is opened.
class ComboBoxCellEditor<T : Any>(
    private val itemsProvider: (Int) -> List<T>,
    private val onChosen: ((Int, T) -> Unit)? = null,
) : AbstractCellEditor(), TableCellEditor {

    private val comboBox = ComboBox<T>()
    private var updating = false
    private var row = -1

    init {
        comboBox.addItemListener { event ->
            if (!updating && event.stateChange == ItemEvent.SELECTED) {
                @Suppress("UNCHECKED_CAST")
                (comboBox.selectedItem as? T)?.let { chosen -> onChosen?.invoke(row, chosen) }
                stopCellEditing()
            }
        }
    }

    override fun getTableCellEditorComponent(
        table: JTable,
        value: Any?,
        isSelected: Boolean,
        row: Int,
        column: Int,
    ): Component {
        this.row = row
        updating = true
        comboBox.removeAllItems()
        itemsProvider(row).forEach { comboBox.addItem(it) }
        @Suppress("UNCHECKED_CAST")
        comboBox.selectedItem = value as? T
        updating = false
        return comboBox
    }

    override fun getCellEditorValue(): Any? = comboBox.selectedItem
}
