package com.pluxurydolo.util

import org.gradle.api.Project

class ProjectUtils {
    static boolean isPlugin(Project project) {
        return project.name.endsWith('-plugin')
    }

    static boolean isStarter(Project project) {
        return project.name.endsWith('-spring-boot-starter')
    }
}
