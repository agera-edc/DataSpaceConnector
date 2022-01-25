# ADR on Spotbugs

Spotbugs is a program to find bugs in Java programs. It looks for instances of “bug patterns” — code instances that are likely to be errors by inspecting Java bytecode.

## Running SpotBugs with Gradle

Use the [Spotbugs Gradle Plugin](https://github.com/spotbugs/spotbugs-gradle-plugin) to run Spotbugs. 

The plugin is configured to generate html reports and with a file for accepted exclusions: 

```kotlin
spotbugs {
    ignoreFailures.set(true) // if false, build fails on bugs
    excludeFilter.set(file("$rootDir/resources/spotbugs-excludes.xml"))
}

tasks.spotbugsMain {
    reports.create("html") {
        required.set(true)
        outputLocation.set(file("$buildDir/reports/spotbugs.html"))
        setStylesheet("fancy-hist.xsl")
    }
}
```

An own report is generated per each Gradle module. This is not practical as one has to navigate to the different modules to get to the findings, a central aggregated overview would come handy.

![Spotbugs report](.attachments/spotbugs.png)

### Aggregating reports

There is [a plugin](https://github.com/SimonScholz/report-aggregator) available to perform the aggregation. The plugin seems not to be in active development and gives an error when trying to run it:

```
 In plugin 'com.simonscholz.reports' type 'com.simonscholz.report.GenerateAggregatedReportTask' property 'level' is missing an input or output annotation.
```

### Resource usage

Spotbugs can be quite resource intense. The [effort configuration](https://spotbugs.readthedocs.io/en/stable/effort.html) helps tune Spotbugs accordingly for each individual project.

## Running Spotbugs with Codacy

According to [the docs](https://docs.codacy.com/related-tools/local-analysis/running-spotbugs/), codacy supports Spotbugs integration. 

We configured a [pipeline](.github/workflows/codacy-analysis.yaml) to run Spotbugs on pushes, but we were not able to get the analysis running, as it seems that classes and sources are not detected properly despite trying to define class/source directories as described in the [documentation](https://docs.codacy.com/related-tools/local-analysis/running-spotbugs/#detecting-sources-and-compiled-classes). See [failing CI run](https://github.com/Agera-CatenaX/EclipseDataSpaceConnector/runs/4925578885?check_suite_focus=true).