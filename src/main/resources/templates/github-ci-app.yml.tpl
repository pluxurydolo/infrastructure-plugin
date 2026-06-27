name: Deploy to VPS

on:
  push:
    branches:
      - '**'
  pull_request:
    branches:
      - main

env:
  JAVA_VERSION: '25'
  JAVA_DISTRIBUTION: 'temurin'

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v7

      - name: Set up JDK
        uses: actions/setup-java@v5
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: ${{ env.JAVA_DISTRIBUTION }}

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v6

      - name: Make gradlew executable
        run: chmod +x gradlew

      - name: Build
        run: ./gradlew assemble

  lint:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v7

      - name: Set up JDK
        uses: actions/setup-java@v5
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: ${{ env.JAVA_DISTRIBUTION }}

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v6

      - name: Make gradlew executable
        run: chmod +x gradlew

      - name: Run PMD checks
        run: ./gradlew pmdMain pmdTest pmdIntegrationTest

      - name: Validate logs
        run: ./gradlew validateLogs

  test:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v7

      - name: Set up JDK
        uses: actions/setup-java@v5
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: ${{ env.JAVA_DISTRIBUTION }}

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v6

      - name: Make gradlew executable
        run: chmod +x gradlew

      - name: Run tests
        run: ./gradlew test

      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v7
        with:
          name: test-results
          path: '**/build/test-results/**/TEST-*.xml'

  integration-test:
    needs: build
    runs-on: ubuntu-latest
    environment: CI/CD
    steps:
      - name: Checkout code
        uses: actions/checkout@v7

      - name: Set up JDK
        uses: actions/setup-java@v5
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: ${{ env.JAVA_DISTRIBUTION }}

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v6

      - name: Make gradlew executable
        run: chmod +x gradlew

      - name: Run integration tests
        env:
          SECRETS_JSON: ${{ toJSON(secrets) }}
        run: |
          eval "$(echo "$SECRETS_JSON" | jq -r 'to_entries[] | "export \(.key)='\''\(.value)'\''"')"
          envsubst < env/test.template > .env.test
          export $(cat .env.test | grep -v '^#' | xargs)
          ./gradlew integrationTest

      - name: Upload integration test results
        if: always()
        uses: actions/upload-artifact@v7
        with:
          name: integration-test-results
          path: '**/build/test-results/**/TEST-*.xml'

  bump-version:
    needs: [ lint, test, integration-test ]
    if: github.event_name == 'push' && github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    permissions:
      contents: write
    steps:
      - name: Checkout code
        uses: actions/checkout@v7
        with:
          token: ${{ secrets.GITHUB_TOKEN }}

      - name: Set up JDK
        uses: actions/setup-java@v5
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: ${{ env.JAVA_DISTRIBUTION }}

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v6

      - name: Make gradlew executable
        run: chmod +x gradlew

      - name: Configure Git
        run: |
          git config user.name "github-actions[bot]"
          git config user.email "github-actions[bot]@users.noreply.github.com"

      - name: Bump version
        uses: gradle/gradle-build-action@v3
        with:
          arguments: --no-daemon -i bumpVersion

      - name: Commit version changes
        run: |
          if git diff --quiet version.properties; then
            echo "No changes to version.properties"
          else
            git add version.properties
            git commit -m "chore: bump version [skip ci]"
            git push
          fi

  docker-publish:
    needs: bump-version
    if: github.event_name == 'push' && github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    environment: CI/CD
    steps:
      - name: Checkout code
        uses: actions/checkout@v7

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Login to Docker Hub
        uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKER_HUB_LOGIN }}
          password: ${{ secrets.DOCKER_HUB_PASSWORD }}

      - name: Get version
        id: version
        run: |
          VERSION=$(grep "^VERSION=" version.properties | cut -d'=' -f2)
          echo "VERSION=$VERSION" >> $GITHUB_OUTPUT
          echo "TAG=${VERSION}-${GITHUB_SHA::8}" >> $GITHUB_OUTPUT

      - name: Build and push Docker image
        uses: docker/build-push-action@v6
        with:
          context: .
          push: true
          tags: |
            ${{ secrets.DOCKER_HUB_LOGIN }}/{{SERVICE_NAME}}:${{ steps.version.outputs.TAG }}
            ${{ secrets.DOCKER_HUB_LOGIN }}/{{SERVICE_NAME}}:latest
          pull: true

  deploy:
    needs: [ docker-publish ]
    if: github.event_name == 'push' && github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    environment: CI/CD
    steps:
      - name: Checkout code
        uses: actions/checkout@v7
        with:
          fetch-depth: 0

      - name: Create .env.prod and get version
        env:
          SECRETS_JSON: ${{ toJSON(secrets) }}
        run: |
          eval "$(echo "$SECRETS_JSON" | jq -r 'to_entries[] | "export \(.key)='\''\(.value)'\''"')"
          envsubst < env/prod.template > .env.prod

          VERSION=$(grep "^VERSION=" version.properties | cut -d'=' -f2)
          echo "VERSION=$VERSION" >> $GITHUB_OUTPUT
          echo "TAG=${VERSION}-${GITHUB_SHA::8}" >> $GITHUB_OUTPUT
        id: version

      - name: Setup SSH and Deploy
        env:
          SSH_PRIVATE_KEY: ${{ secrets.SSH_PRIVATE_KEY }}
          SSH_USER: ${{ secrets.SSH_USER }}
          SSH_HOST: ${{ secrets.SSH_HOST }}
          IMAGE_TAG: ${{ steps.version.outputs.TAG }}
          IMAGE_NAME: ${{ secrets.DOCKER_HUB_LOGIN }}/{{SERVICE_NAME}}
        run: |
          mkdir -p ~/.ssh
          echo "$SSH_PRIVATE_KEY" | base64 -d > ~/.ssh/id_rsa
          chmod 600 ~/.ssh/id_rsa
          eval $(ssh-agent -s)
          ssh-add ~/.ssh/id_rsa
          echo "HOST *" > ~/.ssh/config
          echo "StrictHostKeyChecking no" >> ~/.ssh/config

          scp .env.prod ${SSH_USER}@${SSH_HOST}:~/.env

          ssh ${SSH_USER}@${SSH_HOST} "
            docker stop {{SERVICE_NAME}} || true
            docker rm {{SERVICE_NAME}} || true

            sudo fuser -k {{DEPLOY_PORT}}/tcp || true

            mkdir -p /home/${SSH_USER}/logs/{{SERVICE_NAME}}
            chmod 777 /home/${SSH_USER}/logs/{{SERVICE_NAME}}

            sleep 2

            docker pull ${IMAGE_NAME}:${IMAGE_TAG}

            docker run -d \
              --name {{SERVICE_NAME}} \
              --restart always \
              -p {{DEPLOY_PORT}}:8080 \
              -v ~/.env:/app/.env:ro \
              -v /home/${SSH_USER}/logs/{{SERVICE_NAME}}:/app/logs \
              ${IMAGE_NAME}:${IMAGE_TAG}
          "
