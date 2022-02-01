# ADRs on Code Quality and Test Coverage

In this spike we performed an analysis of tools to measure and guarantee code quality and test coverage. Please refer to the separate ADRs for the evaluation results:

* [ADR on Code Quality](CODEQUALITY.md)
* [ADR on Test Coverage](COVERAGE.md)

## Recommendations for EDC

### Code coverage with jacoco and Codecov

We recommend introducing jacoco for measuring code coverage to get metrics on the current state of EDC testing as well as its evolution over time. 

Additionally, we recommend adding a tool for visualizing code coverage statistics on PRs. This will raise developer awareness on an increase/decrease of coverage introduced by PRs. We suggest using the Codecov platform for this purpose, as it provides a detailed report including a dashboard with additional metrics like code complexity. See the [evaluation results](COVERAGE.md) for more details.

### Increase CodeQL analysis scope

At the moment the `codeql-analysis.xml` runs a CodeQL scan with the default java pack (0.0.7 at the time of writing). This only executes ~44 security queries not including code quality.

We recommend extending the scope of the queries to include code quality analysis. Additional suites can be found in the [CodeQL Github repository](https://github.com/github/codeql/tree/main/java/ql/src/codeql-suites). This would require fixing the critical alerts in the same PR introducing the new rules. Less critical ones can be suppressed using the [Github UI](https://docs.github.com/en/code-security/code-scanning/automatically-scanning-your-code-for-vulnerabilities-and-errors/managing-code-scanning-alerts-for-your-repository#dismissing-or-deleting-alerts).

### Incrementally increase the scope of static code analysis

We recommend starting with a smaller set of CodeQL queries for measuring code quality and incrementally increase the scope of the analysis. There is two possible approaches for this:

- Use additional CodeQL query packs / write additional custom CodeQL queries
- Use additional tools like PMD/Spotbugs with a very reduced set of rules complementing the existing CodeQL queries