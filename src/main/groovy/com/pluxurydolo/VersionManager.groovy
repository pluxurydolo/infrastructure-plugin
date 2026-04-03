package com.pluxurydolo

import org.gradle.api.Project

import static com.pluxurydolo.utils.DateUtils.currentDate
import static com.pluxurydolo.utils.DateUtils.isAfter
import static com.pluxurydolo.utils.FileUtils.createVersionFile
import static com.pluxurydolo.utils.FileUtils.getVersionFile

class VersionManager {
    static void initVersionFile(Project project) {
        File versionFile = getVersionFile(project)

        if (!versionFile.exists()) {
            createVersionFile(versionFile, project)
        } else {
            project.logger.lifecycle('ludl [version-plugin] Версионный файл уже существует в проекте')
        }
    }

    static void bumpVersion(Project project) {
        Properties props = new Properties()

        File versionFile = getVersionFile(project)
        versionFile.withInputStream { props.load(it) }

        String currentDate = getCurrentDate()
        String lastModifiedDate = props.getProperty('LAST_MODIFIED_DATE', currentDate)

        int major = props.getProperty('VERSION_MAJOR').toInteger()
        int minor = props.getProperty('VERSION_MINOR').toInteger()
        int patch = props.getProperty('VERSION_PATCH').toInteger()

        if (isAfter(currentDate, lastModifiedDate)) {
            minor++
            patch = 0
        } else {
            patch++
        }

        String version = "$major.$minor.$patch"
        project.logger.lifecycle("snua [version-plugin] Новая версия проекта: $version")

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

        project.logger.lifecycle("bxqi [version-plugin] Версия проекта: ${props.getProperty('VERSION')}")
        project.logger.lifecycle("rspg [version-plugin] Время последнего изменения версии проекта: ${props.getProperty('LAST_MODIFIED_DATE')}")
    }
}
