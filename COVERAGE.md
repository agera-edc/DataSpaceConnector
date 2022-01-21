# ADR on Test Coverage

Test code coverage is a measure of the source code that executed when a test suite is run. A program with high test coverage has a lower chance of containing bugs.

## Spikes

### Option 1: JaCoCo

JaCoCo (Java Code Coverage) is a popular and mature open-source tool. It runs as a Java agent during test execution, to capture which lines are exercised during which test.

Capturing coverage for a particular project in JaCoCo is straightforward, using the [Gradle JaCoCo Plugin](https://docs.gradle.org/current/userguide/jacoco_plugin.html).

```kotlin
// build.gradle.kts
plugins {
    jacoco
}
```

This yields an HTML report.

![Code Coverage with JaCoCo](.attachments/code-coverage-jacoco-summary.png)

The report can be drilled to highlight covered lines (green), not covered lines (red), and lines where some execution branches are not covered (orange).

![Code Coverage with JaCoCo](.attachments/code-coverage-jacoco-code.png)

This configuration has limited value since each project produces its own report. Furthermore, there is no indication of whether a given commit is increasing or decreasing coverage, and in which areas of the code.

### Option 2: JaCoCo with aggregation

The Gradle documentation includes a sample for [Reporting code coverage across multiple sub-projects with JaCoCo](https://docs.gradle.org/current/samples/sample_jvm_multi_project_with_code_coverage.html). The sample explains how to generate a single aggregated report.

We were not able to get the sample working in the EDC repository.

In any case, extensive complex Kotlin code needs to be added to the build. This is concerning for maintainability.

As it would anyway not solve the problem that code coverage is best analyzed relatively to a previous commit, we did not attempt further to get the sample working.

### Option 3: JaCoCo with Codecov

Codecov is an online service for code coverage analysis that promises to "always be free for open source projects". We have been widely using it in various (open-source and proprietary) projects for years with good results.

We modified the root `build.gradle.kts` file to apply the JaCoCo plugin to all projects, and produce an XML format report that can be used by Codecov:

```kotlin
// build.gradle.kts

allprojects {
     //...
     apply(plugin = "jacoco")
     
     //...
     tasks.jacocoTestReport {
         reports {
             xml.required.set(true)
         }
     }
}

```

We modified the `.github/workflows/verify.yaml` workflow as follows:

```
      - name: Gradle Test Core
         run: ./gradlew clean check jacocoTestReport

       - name: CodeCov
         uses: codecov/codecov-action@v2
         with:
           token: ${{ secrets.CODECOV_TOKEN }}
```

The token is supposedly not required for open-source projects, but we got an error running the action without providing a token.

By logging in at https://about.codecov.io with our GitHub Account, we were able to browse straight away to our EDC (fork) repository and obtain a token for the repository. We added the token as a GitHub secret.

We merged a PR with the action configuration above into the `main` (default) branch of our fork repository, for Codecov to report code coverage differences in PRs.

Finally, we installed the Codecov GitHub app into the repository, to enable the Codecov bot to post comments directly into PRs.

The Codecov online site provides detailed coverage reports. These reports also measure cyclomatic complexity.

![Code Coverage with Codecov](.attachments/code-coverage-codecov-summary.png)

In PRs, the Codecov bot automatically posts a report indicating coverage changes.

![Code Coverage with Codecov](.attachments/code-coverage-codecov-pr-github.png)

These reports can also be accessed from the Codecov online service.

![Code Coverage with Codecov](.attachments/code-coverage-codecov-pr.png)

The report can be drilled to highlight the code subjected to coverage changes.

![Code Coverage with Codecov](.attachments/code-coverage-codecov-pr-detail.png)

The configuration of Codecov can be adjusted in a [`codecov.yaml` configuration file](https://docs.codecov.com/docs/codecov-yaml). That allows for example configuration to ensure each new PR [does not decrease coverage](https://docs.codecov.com/docs/common-recipe-list#increase-overall-coverage-on-each-pull-request).

