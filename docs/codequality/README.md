# ADRs on Code Quality and Test Coverage

In this spike we performed an analysis of tools to measure and guarantee code quality and test coverage. Please refer to the separate ADRs for the evaluation results:

* [ADR on Code Quality](CODEQUALITY.md)
* [ADR on Test Coverage](COVERAGE.md)

## Recommendations for EDC

## Increase CodeQL analysis scope 

At the moment the `codeql-analysis.xml` runs a CodeQL scan with the default java pack (0.0.7 at the time of writing). This only executes ~44 security queries not including code quality.

We recommend extending the scope of the queries to include code quality analysis. Additional suites can be found in the [CodeQL Github repository](https://github.com/github/codeql/tree/main/java/ql/src/codeql-suites).

TBD

## Code coverage with jacoco

TBD

## Visualize code coverage in PRs with Codecov

We recommend adding a tool for visualizing code coverage statistics on PR. This will enable developers to react to a decrease of coverage introduced by certain PRs.

TBD
- Codecov more detail report than Github action
- Dashboard

## Incrementally increase the scope of static code analysis

TBD 
Additional custom CodeQL queries

As many PMD and Spotbugs findings overlap with CodeQL query results, we do not recommend adding these tools in an initial step. Once static code analysis with CodeQL has established within the EDC community, it is advisable to review if adding PMD and/or Spotbugs with a very targeted set of rules complementing  
