name: Publish to Maven Central

on:
  push:
    branches:
      - main

env:
  JAVA_VERSION: '25'
  JAVA_DISTRIBUTION: 'temurin'

jobs:
  lint:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout repository
        uses: actions/checkout@v7

      - name: Set up JDK
        uses: actions/setup-java@v5
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: ${{ env.JAVA_DISTRIBUTION }}

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v6

      - name: Checkstyle (PMD)
        uses: gradle/gradle-build-action@v3
        with:
          arguments: --no-daemon -i pmdMain pmdTest pmdIntegrationTest

      - name: Validate Logs
        uses: gradle/gradle-build-action@v3
        with:
          arguments: --no-daemon -i validateLogs

  test:
    needs: lint
    runs-on: ubuntu-latest
    steps:
      - name: Checkout repository
        uses: actions/checkout@v7

      - name: Set up JDK
        uses: actions/setup-java@v5
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: ${{ env.JAVA_DISTRIBUTION }}

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v6

      - name: Unit Tests
        uses: gradle/gradle-build-action@v3
        with:
          arguments: --no-daemon -i test

      - name: Upload Test Results
        if: always()
        uses: actions/upload-artifact@v7
        with:
          name: test-results
          path: '**/build/test-results/**/TEST-*.xml'

  integration-test:
    needs: lint
    runs-on: ubuntu-latest
    steps:
      - name: Checkout repository
        uses: actions/checkout@v7

      - name: Set up JDK
        uses: actions/setup-java@v5
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: ${{ env.JAVA_DISTRIBUTION }}

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v6

      - name: Integration Tests
        uses: gradle/gradle-build-action@v3
        with:
          arguments: --no-daemon -i integrationTest

      - name: Upload Integration Test Results
        if: always()
        uses: actions/upload-artifact@v7
        with:
          name: integration-test-results
          path: '**/build/test-results/**/TEST-*.xml'

  bump-and-tag:
    needs: [test, integration-test]
    runs-on: ubuntu-latest
    permissions:
      contents: write
    outputs:
      new_version: ${{ steps.commit_and_tag.outputs.version }}
    steps:
      - name: Checkout repository
        uses: actions/checkout@v7
        with:
          fetch-depth: 0
          token: ${{ secrets.GITHUB_TOKEN }}

      - name: Set up JDK
        uses: actions/setup-java@v5
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: ${{ env.JAVA_DISTRIBUTION }}

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v6

      - name: Configure Git
        run: |
          git config user.name "github-actions[bot]"
          git config user.email "github-actions[bot]@users.noreply.github.com"

      - name: Bump version
        uses: gradle/gradle-build-action@v3
        with:
          arguments: --no-daemon -i bumpVersion

      - name: Get new version
        id: get_version
        run: |
          VERSION=$(grep "^VERSION=" version.properties | cut -d'=' -f2)
          echo "VERSION=$VERSION" >> $GITHUB_OUTPUT

      - name: Commit version update and create tag
        id: commit_and_tag
        run: |
          git config user.name "github-actions[bot]"
          git config user.email "github-actions[bot]@users.noreply.github.com"
          git add version.properties
          if ! git diff --cached --quiet; then
            git commit -m "chore: bump version [skip ci]"
            git push origin HEAD:main
            git tag -a "${{ steps.get_version.outputs.VERSION }}" -m "Release ${{ steps.get_version.outputs.VERSION }}"
            git push origin "${{ steps.get_version.outputs.VERSION }}"
            echo "version=${{ steps.get_version.outputs.VERSION }}" >> $GITHUB_OUTPUT
          else
            echo "No changes to version.properties"
            echo "version=" >> $GITHUB_OUTPUT
          fi

  publish:
    needs: bump-and-tag
    if: needs.bump-and-tag.outputs.new_version != ''
    runs-on: ubuntu-latest
    environment: CI/CD
    permissions:
      contents: write
    steps:
      - name: Checkout repository
        uses: actions/checkout@v7
        with:
          ref: "${{ needs.bump-and-tag.outputs.new_version }}"
          fetch-depth: 0
          token: ${{ secrets.GITHUB_TOKEN }}

      - name: Set up JDK
        uses: actions/setup-java@v5
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: ${{ env.JAVA_DISTRIBUTION }}

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v6

      - name: Build and publish to Maven Central
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
