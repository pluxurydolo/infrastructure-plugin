package com.pluxurydolo

import com.pluxurydolo.extension.DeployExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider

import static com.pluxurydolo.VersionManager.*
import static com.pluxurydolo.util.FileUtils.*
import static com.pluxurydolo.util.InfrastructureFile.*
import static com.pluxurydolo.util.ProjectUtils.isPlugin
import static com.pluxurydolo.util.ProjectUtils.isStarter

class InfrastructurePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        DeployExtension deployExtension = project.extensions.create('deploy', DeployExtension.class)

        registerInitVersion(project)
        registerBumpVersion(project)
        registerShowVersion(project)

        registerGenerateDockerFiles(project)
        registerGenerateGitLabCI(project)
        registerGenerateGitHubCI(project)
        registerGenerateDependabotConfig(project)
        registerGenerateDependencySubmission(project)
        registerGenerateReadme(project)

        project.afterEvaluate {
            validateDeployPort(project, deployExtension)
            initVersionIfNeeded(project)
            setProjectVersion(project)
            generateMissingFiles(project)
        }
    }

    private static void registerInitVersion(Project project) {
        String taskName = 'initVersion'
        String taskGroup = 'version'
        Runnable action = () -> initVersionFile(project)
        registerTask(project, taskName, taskGroup, action)
    }

    private static void registerBumpVersion(Project project) {
        String taskName = 'bumpVersion'
        String taskGroup = 'version'
        Runnable action = () -> bumpVersion(project)
        registerTask(project, taskName, taskGroup, action)
    }

    private static void registerShowVersion(Project project) {
        String taskName = 'showVersion'
        String taskGroup = 'version'
        Runnable action = () -> showVersion(project)
        registerTask(project, taskName, taskGroup, action)
    }

    private static void registerGenerateDockerFiles(Project project) {
        String taskName = 'generateDockerFiles'
        String taskGroup = 'docker'

        Runnable action = () -> {
            generateFromTemplate(project, DOCKERFILE)
            generateFromTemplate(project, DOCKERIGNORE)
            generateFromTemplate(project, ENTRYPOINT, true)
        }

        registerTask(project, taskName, taskGroup, action)
    }

    private static void registerGenerateGitLabCI(Project project) {
        String taskName = 'generateGitLabCI'
        String taskGroup = 'ci'
        Runnable action = () -> generateFromTemplate(project, GITLABCI)
        registerTask(project, taskName, taskGroup, action)
    }

    private static void registerGenerateGitHubCI(Project project) {
        String taskName = 'generateGitHubCI'
        String taskGroup = 'ci'

        Runnable action = () -> {
            if (isPlugin(project)) {
                generateFromTemplate(project, GITHUBCI_PLUGIN)
            } else if (isStarter(project)) {
                generateFromTemplate(project, GITHUBCI_STARTER)
            } else {
                generateFromTemplate(project, GITHUBCI_APP)
            }
        }

        registerTask(project, taskName, taskGroup, action)
    }

    private static void registerGenerateDependabotConfig(Project project) {
        String taskName = 'generateDependabotConfig'
        String taskGroup = 'github'
        Runnable action = () -> generateFromTemplate(project, DEPENDABOT_CONFIG)
        registerTask(project, taskName, taskGroup, action)
    }

    private static void registerGenerateDependencySubmission(Project project) {
        String taskName = 'generateDependencySubmission'
        String taskGroup = 'github'
        Runnable action = () -> generateFromTemplate(project, DEPENDENCY_SUBMISSION)
        registerTask(project, taskName, taskGroup, action)
    }

    private static void registerGenerateReadme(Project project) {
        String taskName = 'generateReadme'
        String taskGroup = 'readme'

        Runnable action = () -> {
            if (isPlugin(project) || isStarter(project)) {
                generateFromTemplate(project, README_JRELEASER)
            } else {
                generateFromTemplate(project, README_DOCKER)
            }
        }

        registerTask(project, taskName, taskGroup, action)
    }

    private static TaskProvider<Task> registerTask(Project project, String name, String group, Runnable action) {
        return project.tasks.register(name) {
            it.group = group
            it.doLast { action.run() }
        }
    }

    private static void validateDeployPort(Project project, DeployExtension deployExtension) {
        if (deployExtension.port == null && !isPlugin(project) && !isStarter(project)) {
            project.logger.error('hxdm [infrastructure-plugin] Требуется указать порт деплоя: deploy { port = 1234 }')
            throw new GradleException('Не указан порт деплоя')
        }
    }

    private static void initVersionIfNeeded(Project project) {
        if (!getVersionFile(project).exists()) {
            executeTask(project, 'initVersion')
        }
    }

    private static void setProjectVersion(Project project) {
        Properties props = new Properties()
        File versionFile = getVersionFile(project)
        versionFile.withInputStream { props.load(it) }

        String version = props.getProperty('VERSION')
        project.version = version
        project.logger.lifecycle("fxpe [infrastructure-plugin] Установлена версия проекта: $version")
    }

    private static void generateMissingFiles(Project project) {
        generateIfMissing(project, 'generateDependabotConfig', () -> dependabotConfigExists(project))
        generateIfMissing(project, 'generateGitHubCI', () -> githubCIExists(project))
        generateIfMissing(project, 'generateReadme', () -> readmeExists(project))

        if (!isPlugin(project) && !isStarter(project)) {
            generateIfMissing(project, 'generateDockerFiles', () -> dockerFilesExist(project))
            generateIfMissing(project, 'generateGitLabCI', () -> gitlabCIExists(project))
        }
    }

    private static void generateIfMissing(Project project, String taskName, Closure<Boolean> existsCheck) {
        if (!existsCheck.call()) {
            executeTask(project, taskName)
        }
    }

    private static void executeTask(Project project, String taskName) {
        TaskProvider<Task> taskProvider = project.tasks.named(taskName)
        taskProvider.get().actions.each { it.execute(taskProvider.get()) }
    }
}
