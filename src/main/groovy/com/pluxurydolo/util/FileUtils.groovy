package com.pluxurydolo.util

import com.pluxurydolo.InfrastructurePlugin
import com.pluxurydolo.extension.DeployExtension
import org.gradle.api.Project

import static com.pluxurydolo.util.DateUtils.getCurrentDate
import static com.pluxurydolo.util.InfrastructureFile.*

class FileUtils {
    static void createVersionFile(File versionFile, Project project) {
        String major = '1'
        String minor = '0'
        String patch = '0'
        String version = '1.0.0'
        String currentDate = getCurrentDate()

        Properties props = new Properties()
        props.setProperty('VERSION_MAJOR', major)
        props.setProperty('VERSION_MINOR', minor)
        props.setProperty('VERSION_PATCH', patch)
        props.setProperty('VERSION', version)
        props.setProperty('LAST_MODIFIED_DATE', currentDate)

        versionFile.withOutputStream { props.store(it, null) }
        project.logger.lifecycle('lzlw [infrastructure-plugin] Версионный файл создан с версией 1.0.0')
    }

    static File getVersionFile(Project project) {
        String fileName = 'version.properties'
        return new File(project.projectDir, fileName)
    }

    static void generateFromTemplate(Project project, InfrastructureFile infrastructureFile, boolean executable = false) {
        String serviceName = project.rootProject.name
        GString imageName = "pluxurydolo/${serviceName}"

        InputStream template = InfrastructurePlugin.classLoader.getResourceAsStream("templates/${infrastructureFile.template}")

        if (template == null) {
            project.logger.error("fpka [infrastructure-plugin] Шаблон не найден: ${infrastructureFile.template}")
            return
        }

        String deployPort = project.extensions.getByType(DeployExtension.class).port.toString()

        String content = template.text
                .replace('{{SERVICE_NAME}}', serviceName)
                .replace('{{IMAGE_NAME}}', imageName.toString())
                .replace('{{DEPLOY_PORT}}', deployPort)
                .replace('\r\n', '\n').replace('\r', '\n').replace('\n', '\r\n')

        File targetFile = project.rootProject.file(infrastructureFile.output)
        targetFile.parentFile.mkdirs()
        targetFile.text = content

        if (executable) {
            targetFile.setExecutable(true)
        }

        project.logger.lifecycle("sjrh [infrastructure-plugin] Файл ${infrastructureFile.output} сконфигурирован")
    }

    static boolean dockerFilesExist(Project project) {
        return List.of(DOCKERFILE, DOCKERIGNORE, ENTRYPOINT)
                .stream()
                .allMatch { fileExists(project, it) }
    }

    static boolean gitlabCIExists(Project project) {
        return fileExists(project, GITLABCI)
    }

    static boolean githubCIExists(Project project) {
        return fileExists(project, GITHUBCI_PLUGIN)
                || fileExists(project, GITHUBCI_STARTER)
                || fileExists(project, GITHUBCI_APP)
    }

    static boolean dependabotConfigExists(Project project) {
        return fileExists(project, DEPENDABOT_CONFIG)
    }

    static boolean readmeExists(Project project) {
        return fileExists(project, README_DOCKER) || fileExists(project, README_JRELEASER)
    }

    private static boolean fileExists(Project project, InfrastructureFile infrastructureFile) {
        return project.rootProject.file(infrastructureFile.output).exists()
    }
}
