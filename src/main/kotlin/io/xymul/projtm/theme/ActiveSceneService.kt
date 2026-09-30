package io.xymul.projtm.theme

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

// Remembers which work scene is currently in effect for one project.
@Service(Service.Level.PROJECT)
class ActiveSceneService(@Suppress("unused") private val project: Project) {

    companion object {
        fun getInstance(project: Project): ActiveSceneService = project.getService(ActiveSceneService::class.java)
    }

    var activeScene: String? = null
        private set

    var applied: Boolean = false
        private set

    fun markApplied(sceneName: String) {
        activeScene = sceneName
        applied = true
    }

    // The user changed the theme by hand, so no scene describes the current theme any more.
    fun markDetached() {
        activeScene = null
        applied = true
    }
}
