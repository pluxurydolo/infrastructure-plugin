package com.pluxurydolo

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider

import static com.pluxurydolo.VersionManager.*
import static com.pluxurydolo.utils.FileUtils.*
import static com.pluxurydolo.utils.ProjectUtils.isPlugin
import static com.pluxurydolo.utils.ProjectUtils.isStarter

class InfrastructurePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
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
                generateFromTemplate(project, 'Dockerfile.tpl', 'Dockerfile')
                generateFromTemplate(project, 'dockerignore.tpl', '.dockerignore')
                generateFromTemplate(project, 'entrypoint.sh.tpl', 'scripts/entrypoint.sh', true)
            }
        }

        TaskProvider<Task> generateGitLabCi = project.tasks.register('generateGitLabCI') {
            it.group = 'ci'

            it.doLast {
                generateFromTemplate(project, 'gitlab-ci.yml.tpl', '.gitlab-ci.yml')
            }
        }

        TaskProvider<Task> generateGitHubCi = project.tasks.register('generateGitHubCI') {
            it.group = 'ci'

            it.doLast {
                if (isPlugin(project)) {
                    generateFromTemplate(project, 'github-ci-plugin.yml.tpl', '.github/workflows/release.yml')
                } else if (isStarter(project)) {
                    generateFromTemplate(project, 'github-ci-starter.yml.tpl', '.github/workflows/release.yml')
                }

            }
        }

        TaskProvider<Task> generateReadme = project.tasks.register('generateReadme') {
            it.group = 'readme'

            it.doLast {
                if (isPlugin(project) || isStarter(project)) {
                    generateJreleaserReadme(project)
                } else {
                    generateDockerReadme(project)
                }
            }
        }

        project.afterEvaluate {
            File versionFile = getVersionFile(project)

            if (!versionFile.exists()) {
                initVersionTask.get().actions.forEach { it.execute(initVersionTask.get()) }
            }

            setProjectVersion(project)

            if (isPlugin(project) || isStarter(project)) {
                generateGitHubCi.get().actions.each { it.execute(generateGitHubCi.get()) }
            } else {
                generateDockerFiles.get().actions.each { it.execute(generateDockerFiles.get()) }
                generateGitLabCi.get().actions.each { it.execute(generateGitLabCi.get()) }
                generateReadme.get().actions.each { it.execute(generateReadme.get()) }
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
}
