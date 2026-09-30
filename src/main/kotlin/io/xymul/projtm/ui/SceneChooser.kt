package io.xymul.projtm.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.util.ui.JBUI
import java.awt.Component
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.ListCellRenderer

// The platform default list entry is too tight, entries get comfortable padding here.
class ChooserRenderer : ListCellRenderer<Any> {

    override fun getListCellRendererComponent(
        list: JList<out Any>,
        value: Any?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean,
    ): Component {
        val label = JLabel(value?.toString().orEmpty())
        label.isOpaque = true
        label.border = JBUI.Borders.empty(4, 12, 4, 12)
        label.background = if (isSelected) list.selectionBackground else list.background
        label.foreground = if (isSelected) list.selectionForeground else list.foreground
        return label
    }
}

// Shows the scene chooser centered: in the project window, or in the settings dialog as a fallback.
fun <T : Any> showSceneChooser(
    choices: List<T>,
    title: String,
    project: Project?,
    anchor: Component?,
    onChosen: (T) -> Unit,
) {
    val builder = JBPopupFactory.getInstance().createPopupChooserBuilder(choices)
    builder.setTitle(title)
    builder.setMinSize(JBUI.size(280, 0))
    builder.setRequestFocus(true)
    builder.setItemChosenCallback { chosen -> onChosen(chosen) }
    builder.setRenderer(ChooserRenderer())
    val popup = builder.createPopup()
    if (project != null && !project.isDisposed) {
        popup.showCenteredInCurrentWindow(project)
    } else if (anchor != null) {
        popup.showInCenterOf(anchor)
    } else {
        popup.showInFocusCenter()
    }
}
