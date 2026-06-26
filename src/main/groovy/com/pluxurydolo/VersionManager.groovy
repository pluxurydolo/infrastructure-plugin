package com.pluxurydolo

import org.gradle.api.Project

import static com.pluxurydolo.util.DateUtils.isDateAfter
import static com.pluxurydolo.util.FileUtils.createVersionFile
import static com.pluxurydolo.util.FileUtils.getVersionFile
import static com.pluxurydolo.util.GitUtils.lastCommitDate

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

        String lastCommitDate = getLastCommitDate()
        String lastModifiedDate = props.getProperty('LAST_MODIFIED_DATE', lastCommitDate)

        int majorVersion = props.getProperty('VERSION_MAJOR').toInteger()
        int minorVersion = props.getProperty('VERSION_MINOR').toInteger()
        int patchVersion = props.getProperty('VERSION_PATCH').toInteger()

        if (isDateAfter(lastCommitDate, lastModifiedDate)) {
            minorVersion++
            patchVersion = 0
        } else if (patchVersion == 0 && lastModifiedDate == lastCommitDate) {
            project.logger.lifecycle("jamc [infrastructure-plugin] Версия $majorVersion.$minorVersion.$patchVersion свежая, бамп патч-версии не произойдет")
            return
        } else {
            patchVersion++
        }

        String version = "$majorVersion.$minorVersion.$patchVersion"
        project.logger.lifecycle("snua [infrastructure-plugin] Новая версия проекта: $version")

        props.setProperty('VERSION_MAJOR', majorVersion.toString())
        props.setProperty('VERSION_MINOR', minorVersion.toString())
        props.setProperty('VERSION_PATCH', patchVersion.toString())

        props.setProperty('VERSION', version)
        props.setProperty('LAST_MODIFIED_DATE', lastCommitDate)

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
