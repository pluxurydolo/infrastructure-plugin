package com.pluxurydolo

import org.gradle.api.Project

import java.time.LocalDate

import static com.pluxurydolo.util.FileUtils.createVersionFile
import static com.pluxurydolo.util.FileUtils.getVersionFile

class VersionManager {
    static void initVersionFile(Project project) {
        File versionFile = getVersionFile(project)

        if (versionFile.exists()) {
            project.logger.lifecycle('ludl [infrastructure-plugin] Версионный файл уже существует в проекте')
        } else {
            createVersionFile(versionFile, project)
        }
    }

    static void bumpVersion(Project project) {
        Properties props = new Properties()
        File versionFile = getVersionFile(project)
        versionFile.withInputStream { props.load(it) }

        String currentDate = LocalDate.now().toString()
        String lastModifiedDate = props.getProperty('LAST_MODIFIED_DATE', currentDate)

        int major = props.getProperty('VERSION_MAJOR').toInteger()
        int minor = props.getProperty('VERSION_MINOR').toInteger()
        int patch = props.getProperty('VERSION_PATCH').toInteger()

        if (patch == -1) {
            patch = 0
        } else if (currentDate > lastModifiedDate) {
            minor++
            patch = 0
        } else {
            patch++
        }

        GString version = "$major.$minor.$patch"
        project.logger.lifecycle("ukqf [infrastructure-plugin] Новая версия проекта: $version")

        props.setProperty('VERSION_MAJOR', major.toString())
        props.setProperty('VERSION_MINOR', minor.toString())
        props.setProperty('VERSION_PATCH', patch.toString())
        props.setProperty('VERSION', version)
        props.setProperty('LAST_MODIFIED_DATE', currentDate)

        versionFile.withOutputStream { props.store(it, null) }
    }

    static void showVersion(Project project) {
        Properties props = new Properties()
        File versionFile = getVersionFile(project)
        versionFile.withInputStream { props.load(it) }

        String version = props.getProperty('VERSION')
        project.logger.lifecycle("bxqi [infrastructure-plugin] Версия проекта: ${version}")

        String lastModifiedDate = props.getProperty('LAST_MODIFIED_DATE')
        project.logger.lifecycle("rspg [infrastructure-plugin] Время последнего изменения версии проекта: ${lastModifiedDate}")
    }
}
