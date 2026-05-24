image: eclipse-temurin:25-jdk-alpine

stages:
  - build
  - test
  - integration_test
  - lint
  - docker
  - deploy

before_script:
  - chmod +x gradlew

build:
  stage: build
  script:
    - ./gradlew :assemble

test:
  stage: test
  script:
    - ./gradlew :test
  artifacts:
    reports:
      junit: '**/build/test-results/**/TEST-*.xml'

integration_test:
  stage: integration_test
  tags:
    - saas-linux-medium-amd64
  services:
    - docker:28.1.1-dind
  variables:
    DOCKER_HOST: tcp://docker:2375
    DOCKER_TLS_CERTDIR: ""
    DOCKER_DRIVER: overlay2
  before_script:
    - chmod +x gradlew
    - apk add --no-cache sed bash
    - cp env/test.template .env.test
    - |
      for var in \$(grep -o '{{[^}]*}}' .env.test | sed 's/{{//g;s/}}//g'); do
        value=\$(eval echo "\\\$\\\$var")
        if [ -n "\$value" ]; then
          sed -i "s|{{\$var}}|\$value|g" .env.test
        fi
      done
    - export \$(cat .env.test | grep -v '^#' | xargs)
  script:
    - ./gradlew :integrationTest
  artifacts:
    reports:
      junit: '**/build/test-results/**/TEST-*.xml'

checkstyle:
  stage: lint
  script:
    - ./gradlew :pmdMain :pmdTest :pmdIntegrationTest

validate_logs:
  stage: lint
  script:
    - ./gradlew :validateLogs

docker_publish:
  stage: docker
  image: docker:28.1.1
  services:
    - docker:28.1.1-dind
  before_script:
    - docker login -u \$DOCKER_HUB_LOGIN -p \$DOCKER_HUB_PASSWORD
  script:
    - IMAGE_NAME="{{IMAGE_NAME}}"
    - VERSION=\$(grep "^VERSION=" version.properties | cut -d'=' -f2)
    - TAG="\${VERSION}-\${CI_COMMIT_SHORT_SHA}"
    - echo "Building version \${VERSION}"
    - echo "Tag \${TAG}"
    - docker build --pull -t \${IMAGE_NAME}:\${TAG} -t \${IMAGE_NAME}:latest .
    - docker push \${IMAGE_NAME}:\${TAG}
    - docker push \${IMAGE_NAME}:latest
  only:
    - main

create_environment:
  stage: deploy
  image: alpine:latest
  before_script:
    - apk add --no-cache sed
  script:
    - cp env/prod.template .env.prod
    - |
      for var in \$(grep -o '{{[^}]*}}' .env.prod | sed 's/{{//g;s/}}//g'); do
        value=\$(eval echo "\\\$\\\$var")
        if [ -n "\$value" ]; then
          sed -i "s|{{\$var}}|\$value|g" .env.prod
        fi
      done
  artifacts:
    paths:
      - .env.prod
    expire_in: 1 hour
  only:
    - main

deploy:
  stage: deploy
  image: dwdraju/ssh-client-alpine
  needs:
    - docker_publish
    - create_environment
  script:
    - mkdir -p ~/.ssh
    - cat "\$SSH_PRIVATE_KEY" | base64 -d > ~/.ssh/id_rsa
    - chmod 600 ~/.ssh/id_rsa
    - eval \$(ssh-agent -s)
    - ssh-add ~/.ssh/id_rsa
    - echo "HOST *" > ~/.ssh/config
    - echo "StrictHostKeyChecking no" >> ~/.ssh/config
    - VERSION=\$(grep "^VERSION=" version.properties | cut -d'=' -f2)
    - TAG="\${VERSION}-\${CI_COMMIT_SHORT_SHA}"
    - IMAGE_NAME="{{IMAGE_NAME}}"
    - scp .env.prod \$SSH_USER@\$SSH_HOST:~/.env
    - |
      ssh \$SSH_USER@\$SSH_HOST "
        docker stop {{SERVICE_NAME}} || true
        docker rm {{SERVICE_NAME}} || true
        sudo fuser -k 8888/tcp || true
        sleep 2
        docker pull \${IMAGE_NAME}:\${TAG}
        docker run -d \\
          --name {{SERVICE_NAME}} \\
          --restart always \\
          -p 8888:8080 \\
          -v ~/.env:/app/.env:ro \\
          \${IMAGE_NAME}:\${TAG}
      "
  only:
    - main
