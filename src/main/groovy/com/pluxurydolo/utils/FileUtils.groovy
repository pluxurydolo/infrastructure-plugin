package com.pluxurydolo.utils

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
        project.logger.lifecycle('lzlw [version-plugin] Версионный файл создан с версией 1.0.0')
    }

    static File getVersionFile(Project project) {
        String fileName = 'version.properties'
        return new File(project.projectDir, fileName)
    }
}
