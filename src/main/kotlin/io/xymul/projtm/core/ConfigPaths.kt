package io.xymul.projtm.core

import com.intellij.openapi.application.PathManager
import java.nio.file.Path

// Root of the plugin data directory, kept inside the IDE system directory and never inside the IDE installation.
fun configRoot(): Path = runCatching { Path.of(PathManager.getSystemPath(), "project-theme-manager") }
    .getOrElse { Path.of(System.getProperty("java.io.tmpdir"), "project-theme-manager") }

fun projectsFile(): Path = configRoot().resolve("projects.xml")

fun workScenesDir(): Path = configRoot().resolve("WorkScenes")
