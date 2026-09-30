# Project Theme Manager

A IntelliJ IDEs plugin, user can register to remembers the UI theme of each project and applies it when the project opens.
And it provides a feature named `Work Scene` to quickly switch UI theme from registered(project level or <s>global level</s>) scenes.

The motivation behind this plugin is the need to switch themes in different scenarios.

For example, Most people probably prefer a dark theme when coding, but a light theme that's easy on the eyes when reading documentation.

Even though IntelliJ IDEs lets you quickly switch themes with shortcuts, but having to pinpoint which one you want among several themes each time is tedious

So this plugin binds the UI theme to the scene, you just need to register a scene for your project (which can be global in the future) and set the UI Theme for that scene.

When you need to switch themes, just press the shortcut key of this plugin, and you can quickly switch themes to suit your current situation.

**You can think of this plugin as a bookmark that’s an alias for a UI Theme.**

## Build

Requirements:
- IntelliJ SDK >= 2026.2.3

First to **MODIFY** the `gradle.propreties`, and ensure your environment have IntelliJ IDEs:
```properties
# root directory of IDE which provides the sdk
localIdePath=$1

org.gradle.java.installations.paths=$2

org.gradle.java.home=$3
```

**OR** *modify* `build.gradle`:

Remove three items above in `gradle.properties` and this:
```groovy
def localIdePath = providers.gradleProperty('localIdePath').get()
```

then add this:
```groovy
dependencies {
    intellijPlatform {
        create(IntelliJPlatformType.IntellijIdeaCommunity, "2026.2.3")
    }
}
```

After you have modified build files, run this in terminal to build project:
```shell
./gradlew build
./gradlew buildPlugin
./gradlew --offline build
```

To test the effect of this plugin, run:
```shell
./gradlew runIde
```