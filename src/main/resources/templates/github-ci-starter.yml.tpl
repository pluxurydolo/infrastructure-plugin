name: Publish to central.sonatype.com

on:
  push:
    tags:
      - '*'

jobs:
  build:
    runs-on: ubuntu-latest
    environment: central.sonatype.com
    permissions:
      contents: write

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4
        with:
          fetch-depth: 0
          token: ${{ secrets.GITHUB_TOKEN }}

      - name: Set up JDK 25
        uses: actions/setup-java@v4
        with:
          java-version: '25'
          distribution: 'temurin'

      - name: Checkstyle
        uses: gradle/gradle-build-action@v3
        with:
          arguments: --no-daemon -i pmdMain pmdTest pmdIntegrationTest

      - name: Validate Logs
        uses: gradle/gradle-build-action@v3
        with:
          arguments: --no-daemon -i validateLogs

      - name: Test
        uses: gradle/gradle-build-action@v3
        with:
          arguments: --no-daemon -i test

      - name: Integration Test
        uses: gradle/gradle-build-action@v3
        with:
          arguments: --no-daemon -i integrationTest

      - name: Bump version
        uses: gradle/gradle-build-action@v3
        with:
          arguments: --no-daemon -i bumpVersion

      - name: Commit version update
        run: |
          git config user.name "github-actions[bot]"
          git config user.email "github-actions[bot]@users.noreply.github.com"
          git add version.properties
          if ! git diff --cached --quiet; then
            git commit -m "chore: bump version [skip ci]"
            git push origin HEAD:main
          else
            echo "No changes to version.properties"
          fi

      - name: Build and publish
        uses: gradle/gradle-build-action@v3
        with:
          arguments: --no-daemon -i jreleaserConfig build publish
        env:
          JRELEASER_GPG_SECRET_KEY: ${{ secrets.JRELEASER_GPG_SECRET_KEY }}
          JRELEASER_GPG_PASSPHRASE: ${{ secrets.JRELEASER_GPG_PASSPHRASE }}
          JRELEASER_GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
          JRELEASER_MAVENCENTRAL_SONATYPE_USERNAME: ${{ secrets.JRELEASER_MAVENCENTRAL_SONATYPE_USERNAME }}
          JRELEASER_MAVENCENTRAL_SONATYPE_TOKEN: ${{ secrets.JRELEASER_MAVENCENTRAL_SONATYPE_TOKEN }}
          JRELEASER_GPG_PUBLIC_KEY: ${{ secrets.JRELEASER_GPG_PUBLIC_KEY }}

      - name: Release
        uses: gradle/gradle-build-action@v3
        with:
          arguments: --no-daemon -i jreleaserFullRelease
        env:
          JRELEASER_GPG_SECRET_KEY: ${{ secrets.JRELEASER_GPG_SECRET_KEY }}
          JRELEASER_GPG_PASSPHRASE: ${{ secrets.JRELEASER_GPG_PASSPHRASE }}
          JRELEASER_GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
          JRELEASER_MAVENCENTRAL_USERNAME: ${{ secrets.JRELEASER_MAVENCENTRAL_SONATYPE_USERNAME }}
          JRELEASER_MAVENCENTRAL_TOKEN: ${{ secrets.JRELEASER_MAVENCENTRAL_SONATYPE_TOKEN }}
          JRELEASER_GPG_PUBLIC_KEY: ${{ secrets.JRELEASER_GPG_PUBLIC_KEY }}
        continue-on-error: true
