version: 2
updates:
  - package-ecosystem: "gradle"
    directory: "/"
    schedule:
      interval: "weekly"
      day: "sunday"
      time: "00:00"
      timezone: "Europe/Moscow"
    open-pull-requests-limit: 15
    labels:
      - "dependencies"
    reviewers:
      - "pluxurydolo"
    assignees:
      - "pluxurydolo"
    commit-message:
      prefix: "chore"
      include: "scope"

  - package-ecosystem: "github-actions"
    directory: "/"
    schedule:
      interval: "weekly"
      day: "sunday"
      time: "00:00"
      timezone: "Europe/Moscow"
    labels:
      - "dependencies"
      - "ci"
    commit-message:
      prefix: "ci"
