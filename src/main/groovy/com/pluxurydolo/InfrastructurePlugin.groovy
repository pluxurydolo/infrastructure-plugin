package com.pluxurydolo

import com.pluxurydolo.extension.DeployExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider

import static com.pluxurydolo.VersionManager.*
import static com.pluxurydolo.utils.FileUtils.*
import static com.pluxurydolo.utils.InfrastructureFile.*
import static com.pluxurydolo.utils.ProjectUtils.isPlugin
import static com.pluxurydolo.utils.ProjectUtils.isStarter

class InfrastructurePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        DeployExtension deployExtension = project.extensions.create('deploy', DeployExtension.class)

        TaskProvider<Task> initVersionTask = project.tasks.register('initVersion') {
            it.group = 'version'

            it.doLast {
                initVersionFile(project)
            }
        }

        project.tasks.register('bumpVersion') {
            it.group = 'version'

            it.doLast {
                bumpVersion(project)
            }
        }

        project.tasks.register('showVersion') {
            it.group = 'version'

            it.doLast {
                showVersion(project)
            }
        }

        TaskProvider<Task> generateDockerFiles = project.tasks.register('generateDockerFiles') {
            it.group = 'docker'

            it.doLast {
                generateFromTemplate(project, DOCKERFILE)
                generateFromTemplate(project, DOCKERIGNORE)
                generateFromTemplate(project, ENTRYPOINT, true)
            }
        }

        TaskProvider<Task> generateGitLabCi = project.tasks.register('generateGitLabCI') {
            it.group = 'ci'

            it.doLast {
                generateFromTemplate(project, GITLABCI)
            }
        }

        TaskProvider<Task> generateGitHubCi = project.tasks.register('generateGitHubCI') {
            it.group = 'ci'

            it.doLast {
                if (isPlugin(project)) {
                    generateFromTemplate(project, GITHUBCI_PLUGIN)
                } else if (isStarter(project)) {
                    generateFromTemplate(project, GITHUBCI_STARTER)
                }

            }
        }

        TaskProvider<Task> generateReadme = project.tasks.register('generateReadme') {
            it.group = 'readme'

            it.doLast {
                if (isPlugin(project) || isStarter(project)) {
                    generateFromTemplate(project, README_JRELEASER)
                } else {
                    generateFromTemplate(project, README_DOCKER)
                }
            }
        }

        project.afterEvaluate {
            if (deployExtension.port == null && !isPlugin(project) && !isStarter(project)) {
                throw new GradleException('hxdm Требуется указать порт деплоя: deploy { port = 1234 }')
            }

            File versionFile = getVersionFile(project)

            if (!versionFile.exists()) {
                initVersionTask.get().actions.forEach { it.execute(initVersionTask.get()) }
            }

            setProjectVersion(project)

            if (isPlugin(project) || isStarter(project)) {
                generatePluginOrStarterFiles(project, generateGitHubCi, generateReadme)
            } else {
                generateApplicationFiles(project, generateDockerFiles, generateGitLabCi, generateReadme)
            }
        }
    }

    private static void setProjectVersion(Project project) {
        File versionFile = getVersionFile(project)
        Properties props = new Properties()
        versionFile.withInputStream { props.load(it) }

        String version = props.getProperty('VERSION')
        project.version = version
        project.logger.lifecycle("fxpe [infrastructure-plugin] Установлена версия проекта: $version")
    }

    private static void generatePluginOrStarterFiles(
            Project project,
            TaskProvider<Task> generateGitHubCi,
            TaskProvider<Task> generateReadme
    ) {
        if (!githubCiExists(project)) {
            generateGitHubCi.get().actions.each { it.execute(generateGitHubCi.get()) }
        }

        if (!readmeExists(project)) {
            generateReadme.get().actions.each { it.execute(generateReadme.get()) }
        }
    }

    private static void generateApplicationFiles(
            Project project,
            TaskProvider<Task> generateDockerFiles,
            TaskProvider<Task> generateGitLabCi,
            TaskProvider<Task> generateReadme
    ) {
        if (!dockerFilesExist(project)) {
            generateDockerFiles.get().actions.each { it.execute(generateDockerFiles.get()) }
        }

        if (!gitlabCiExists(project)) {
            generateGitLabCi.get().actions.each { it.execute(generateGitLabCi.get()) }
        }

        if (!readmeExists(project)) {
            generateReadme.get().actions.each { it.execute(generateReadme.get()) }
        }
    }
}
