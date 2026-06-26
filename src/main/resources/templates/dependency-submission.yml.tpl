name: Automatic Dependency Submission

on:
  push:
    branches:
      - main

jobs:
  submit-gradle:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v7
      - uses: actions/setup-java@v5
        with:
          java-version: '25'
          distribution: 'temurin'
      - uses: gradle/actions/dependency-submission@v6
