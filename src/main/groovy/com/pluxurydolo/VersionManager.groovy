package com.pluxurydolo

import org.gradle.api.Project

import static com.pluxurydolo.utils.DateUtils.currentDate
import static com.pluxurydolo.utils.DateUtils.isDateAfter
import static com.pluxurydolo.utils.FileUtils.createVersionFile
import static com.pluxurydolo.utils.FileUtils.getVersionFile

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

        String currentDate = getCurrentDate()
        String lastModifiedDate = props.getProperty('LAST_MODIFIED_DATE', currentDate)

        int majorVersion = props.getProperty('VERSION_MAJOR').toInteger()
        int minorVersion = props.getProperty('VERSION_MINOR').toInteger()

        String versionPatch = props.getProperty('VERSION_PATCH')
        String[] parts = versionPatch.split('-')
        int patchVersion = parts[0].toInteger()

        if (isDateAfter(currentDate, lastModifiedDate)) {
            minorVersion++
            patchVersion = 0
        } else {
            patchVersion++
        }

        String patchWithQualificator = extractQualificator(patchVersion, parts)

        String version = "$majorVersion.$minorVersion.$patchWithQualificator"
        project.logger.lifecycle("snua [infrastructure-plugin] Новая версия проекта: $version")

        props.setProperty('VERSION_MAJOR', majorVersion.toString())
        props.setProperty('VERSION_MINOR', minorVersion.toString())
        props.setProperty('VERSION_PATCH', patchWithQualificator)

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

    private static String extractQualificator(int patch, String[] parts) {
        if (parts.length == 1) {
            return patch
        }

        String qualificator = parts[1]

        return "$patch-$qualificator"
    }
}
