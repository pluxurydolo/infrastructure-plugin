package com.pluxurydolo.util

class GitUtils {
    static String getLastCommitDate() {
        return 'git log -1 --format=%cd --date=short'.execute().text.trim()
    }
}
