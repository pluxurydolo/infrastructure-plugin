package com.pluxurydolo.util

enum InfrastructureFile {
    DOCKERFILE('Dockerfile.tpl', 'Dockerfile'),
    DOCKERIGNORE('dockerignore.tpl', '.dockerignore'),
    ENTRYPOINT('entrypoint.sh.tpl', 'scripts/entrypoint.sh'),
    GITLABCI('gitlab-ci.yml.tpl', '.gitlab-ci.yml'),
    GITHUBCI_PLUGIN('github-ci-plugin.yml.tpl', '.github/workflows/release.yml'),
    GITHUBCI_STARTER('github-ci-starter.yml.tpl', '.github/workflows/release.yml'),
    GITHUBCI_APP('github-ci-app.yml.tpl', '.github/workflows/deploy.yml'),
    DEPENDABOT_CONFIG('dependabot.yml.tpl', '.github/dependabot.yml'),
    README_DOCKER('readme-docker.tpl', 'README.md'),
    README_JRELEASER('readme-jreleaser.tpl', 'README.md');

    private final String template
    private final String output

    InfrastructureFile(String template, String output) {
        this.template = template
        this.output = output
    }

    String getTemplate() {
        return template
    }

    String getOutput() {
        return output
    }
}
