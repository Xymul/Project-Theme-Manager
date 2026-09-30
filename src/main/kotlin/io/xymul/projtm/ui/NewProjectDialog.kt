package io.xymul.projtm.ui

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import io.xymul.projtm.core.ConfigRepository
import io.xymul.projtm.core.normalizePath
import io.xymul.projtm.model.Scene
import io.xymul.projtm.theme.ThemeItem
import java.nio.file.Path
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField

// Dialog that registers a new project together with its first work scene.
class NewProjectDialog(recentPaths: List<String>) : DialogWrapper(true) {

    private val projectBox = ComboBox<String>()
    private val nameField = JTextField(24)
    private val sceneNameField = JTextField("default", 24)
    private val themeBox = ComboBox<ThemeItem>()
    private val chooseButton = JButton(ProjectThemeManagerBundle.message("settings.dialog.newProject.chooseDirectory"))

    init {
        title = ProjectThemeManagerBundle.message("settings.dialog.newProject.title")
        recentPaths.forEach { projectBox.addItem(it) }
        projectBox.isEditable = true
        themeBox.renderer = ThemeItemRenderer()
        installedThemes().forEach { themeBox.addItem(it) }
        themeBox.selectedItem = installedThemes().firstOrNull { it.id == com.intellij.ide.ui.LafManager.getInstance().currentUIThemeLookAndFeel?.id }
        chooseButton.addActionListener { chooseDirectory() }
        projectBox.addActionListener { updateProjectName() }
        init()
    }

    override fun createCenterPanel(): JComponent = FormBuilder.createFormBuilder()
        .addLabeledComponent(
            JBLabel(ProjectThemeManagerBundle.message("settings.dialog.newProject.label.project")),
            JPanel().apply { add(projectBox); add(chooseButton) },
        )
        .addLabeledComponent(
            JBLabel(ProjectThemeManagerBundle.message("settings.dialog.newProject.label.sceneName")),
            sceneNameField,
        )
        .addLabeledComponent(
            JBLabel(ProjectThemeManagerBundle.message("settings.dialog.newProject.label.theme")),
            themeBox,
        )
        .panel

    override fun doValidate(): ValidationInfo? {
        val path = chosenPath()
        if (path == null) {
            return ValidationInfo(ProjectThemeManagerBundle.message("settings.dialog.newProject.emptyPath"), projectBox)
        }
        if (ConfigRepository.getInstance().findProject(path) != null) {
            return ValidationInfo(
                ProjectThemeManagerBundle.message("settings.dialog.newProject.alreadyExists", nameField.text),
                projectBox,
            )
        }
        return null
    }

    fun chosenPath(): String? = (projectBox.editor.item as? String)?.trim()?.takeIf { it.isNotEmpty() }

    fun projectName(): String = nameField.text.trim().ifEmpty { Path.of(chosenPath().orEmpty()).fileName?.toString().orEmpty() }

    fun sceneName(): String = sceneNameField.text.trim().ifEmpty { "default" }

    fun initialScene(): Scene {
        val theme = themeBox.selectedItem as? ThemeItem
        return Scene(sceneName(), theme?.id.orEmpty(), theme?.dark ?: false)
    }

    private fun chooseDirectory() {
        val chosen = com.intellij.openapi.fileChooser.FileChooser.chooseFile(
            com.intellij.openapi.fileChooser.FileChooserDescriptorFactory.createSingleFolderDescriptor(),
            null,
            null,
        ) ?: return
        projectBox.editor.item = chosen.path.replace('/', java.io.File.separatorChar)
        updateProjectName()
    }

    private fun updateProjectName() {
        val path = chosenPath() ?: return
        val normalized = normalizePath(path)
        if (nameField.text.isBlank()) {
            nameField.text = normalized.trimEnd('/').substringAfterLast('/')
        }
    }
}
