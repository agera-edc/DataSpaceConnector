# ADR on Test Coverage

Test code coverage is a measure of the source code that executed when a test suite is run. A program with high test coverage has a lower chance of containing bugs.

## Spikes

We evaluated the following options:

- [JaCoCo without or with aggregation](coverage-tools/JACOCO.md)
- [JaCoCo with Codecov](coverage-tools/CODECOV.md)
- [JaCoCo with Codacy](coverage-tools/CODACY.md)
- [JaCoCo with SonarQube](coverage-tools/SONARQUBE.md)
- [JaCoCo with Github Action](coverage-tools/JACOCO_GITHUB_ACTION.md)

## Comparison of selected options

| Tool | Project coverage report | Coverage on PR in Github | Additional comments |
|-------|------------------|-------------------------|--------------------|
| JaCoCo with Codecov | ✅ Detailed report on Codecov dashboard | ✅ Github bot messages on every PR (coverage after the PR is merged, total project coverage, code complexity) | ✅ Reports on code complexity<br/> ✅ Easy configuration |
| JaCoCo with Github Actions | ✅ Basic report (percentage) | ✅ Github bot messages on every PR (coverage on changed files and total project coverage) | ⚠️ [Minor issue] Manual configuration (JaCoCo Report Github Action requires a property to path to JaCoCo reports) |
| JaCoCo with Codacy | ✅ Report available on Codacy dashboard | ⚠️ Not supported | ⚠️ Delays in reports showing up in the dashboard |


