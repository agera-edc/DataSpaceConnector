# Dependency analysis

## Decision

The CI workflow is extended with two Gradle tasks for analyzing EDC module dependencies:

- The [Dependency Analysis Gradle Plugin](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin) is used for detecting unused dependencies and dependencies declared on the wrong configuration (`api` vs `implementation` vs `compileOnly`, etc.).
- A custom build task defines rules on which module dependencies are allowed within EDC (e.g. no module may depend directly on any of the core modules).

The tasks are initially set to only emit warnings, but will be configured to fail builds once all warnings have been resolved.

The tasks are run in the CI workflow.
