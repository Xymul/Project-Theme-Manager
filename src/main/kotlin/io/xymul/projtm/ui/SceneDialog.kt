package io.xymul.projtm.ui

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import io.xymul.projtm.model.Scene
import io.xymul.projtm.theme.ThemeItem
import io.xymul.projtm.theme.ThemeService
import javax.swing.JComponent
import javax.swing.JTextField

// Dialog that creates one work scene or edits an existing one.
class SceneDialog(private val usedNames: Set<String>, scene: Scene? = null) : DialogWrapper(true) {

    private val nameField = JTextField(scene?.name.orEmpty(), 24)
    private val themeBox = ComboBox<ThemeItem>()
    private val originalName = scene?.name

    init {
        title = ProjectThemeManagerBundle.message("settings.dialog.scene.title")
        themeBox.renderer = ThemeItemRenderer()
        installedThemes().forEach { themeBox.addItem(it) }
        // A new scene starts with the theme that is active right now.
        val initialThemeId = scene?.themeId ?: ThemeService.getInstance().currentThemeId()
        themeBox.selectedItem = installedThemes().firstOrNull { it.id == initialThemeId }
        init()
    }

    override fun createCenterPanel(): JComponent = FormBuilder.createFormBuilder()
        .addLabeledComponent(JBLabel(ProjectThemeManagerBundle.message("settings.dialog.scene.label.name")), nameField)
        .addLabeledComponent(JBLabel(ProjectThemeManagerBundle.message("settings.dialog.scene.label.theme")), themeBox)
        .panel

    override fun getPreferredFocusedComponent(): JComponent = nameField

    override fun doValidate(): ValidationInfo? {
        val name = nameField.text.trim()
        if (name.isEmpty()) {
            return ValidationInfo(ProjectThemeManagerBundle.message("settings.dialog.scene.emptyName"), nameField)
        }
        if (name != originalName && name in usedNames) {
            return ValidationInfo(ProjectThemeManagerBundle.message("settings.dialog.scene.duplicateName", name), nameField)
        }
        return null
    }

    fun sceneName(): String = nameField.text.trim()

    fun themeId(): String? = (themeBox.selectedItem as? ThemeItem)?.id
}
