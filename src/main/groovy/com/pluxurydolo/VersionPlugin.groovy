package com.pluxurydolo

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider

import static com.pluxurydolo.VersionManager.bumpVersion
import static com.pluxurydolo.VersionManager.initVersionFile
import static com.pluxurydolo.VersionManager.showVersion
import static com.pluxurydolo.utils.FileUtils.getVersionFile

class VersionPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        TaskProvider<Task> initVersionTask = project.tasks.register('initVersion') {
            group = 'version'

            doLast {
                initVersionFile(project)
            }
        }

        TaskProvider<Task> bumpVersionTask = project.tasks.register('bumpVersion') {
            group = 'version'

            doLast {
                bumpVersion(project)
            }
        }

        project.tasks.register('showVersion') {
            group = 'version'

            doLast {
                showVersion(project)
            }
        }

        project.afterEvaluate {
            File versionFile = getVersionFile(project)
            if (!versionFile.exists()) {
                initVersionTask.get().actions.forEach { it.execute(initVersionTask.get()) }
            }

            setProjectVersion(project)
        }
    }

    private static void setProjectVersion(Project project) {
        File versionFile = getVersionFile(project)
        Properties props = new Properties()
        versionFile.withInputStream { props.load(it) }

        String version = props.getProperty('VERSION')
        project.version = version
        project.logger.lifecycle("fxpe [version-plugin] Установлена версия проекта: $version")
    }
}
