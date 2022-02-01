# ADRs on Code Quality and Test Coverage

In this spike we performed an analysis of tools to measure and guarantee code quality and test coverage. Please refer to the separate ADRs for the evaluation results:

* [ADR on Code Quality](CODEQUALITY.md)
* [ADR on Test Coverage](COVERAGE.md)

## Recommendations for EDC

## Increase CodeQL analysis scope 

At the moment the `codeql-analysis.xml` runs a CodeQL scan with the default java pack (0.0.7 at the time of writing). This only executes ~44 security queries not including code quality.

We recommend extending the scope of the queries to include code quality analysis. Additional suites can be found in the [CodeQL Github repository](https://github.com/github/codeql/tree/main/java/ql/src/codeql-suites).

## Use LGTM

TBD

## Code coverage with jacoco and Codecov

TBD