package com.pluxurydolo.utils

import com.pluxurydolo.InfrastructurePlugin
import org.gradle.api.Project

import static com.pluxurydolo.utils.DateUtils.getCurrentDate

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

    static void generateFromTemplate(Project project, String templateName, String outputPath, boolean executable = false) {
        String serviceName = project.rootProject.name
        GString imageName = "pluxurydolo/${serviceName}"

        InputStream template = InfrastructurePlugin.classLoader.getResourceAsStream("templates/${templateName}")

        if (template == null) {
            project.logger.error("fpka [infrastructure-plugin] Шаблон не найден: ${templateName}")
            return
        }

        String content = template.text
                .replace('{{SERVICE_NAME}}', serviceName)
                .replace('{{IMAGE_NAME}}', imageName.toString())
                .replace('\r\n', '\n').replace('\r', '\n').replace('\n', '\r\n')

        File targetFile = project.rootProject.file(outputPath)
        targetFile.parentFile.mkdirs()
        targetFile.text = content

        if (executable) {
            targetFile.setExecutable(true)
        }

        project.logger.lifecycle("sjrh [infrastructure-plugin] Файл ${outputPath} сконфигурирован")
    }

    static void generateDockerReadme(Project project) {
        String serviceName = project.rootProject.name

        InputStream template = InfrastructurePlugin.classLoader.getResourceAsStream('templates/readme-docker.tpl')

        if (template == null) {
            project.logger.error('nqpz [infrastructure-plugin] Шаблон не найден: readme-docker.tpl')
            return
        }

        String content = template.text
                .replace('{{SERVICE_NAME}}', serviceName)
                .replace('\r\n', '\n').replace('\r', '\n').replace('\n', '\r\n')

        File targetFile = project.rootProject.file('README.md')
        targetFile.parentFile.mkdirs()
        targetFile.text = content

        project.logger.lifecycle('tarq [infrastructure-plugin] Файл README.md сконфигурирован')
    }

    static void generateJreleaserReadme(Project project) {
        InputStream template = InfrastructurePlugin.classLoader.getResourceAsStream('templates/readme-jreleaser.tpl')

        if (template == null) {
            project.logger.error('pndk [infrastructure-plugin] Шаблон не найден: readme-jreleaser.tpl')
            return
        }

        String content = template.text
                .replace('\r\n', '\n').replace('\r', '\n').replace('\n', '\r\n')

        File targetFile = project.rootProject.file('README.md')
        targetFile.parentFile.mkdirs()
        targetFile.text = content

        project.logger.lifecycle('qnls [infrastructure-plugin] Файл README.md сконфигурирован')
    }
}
