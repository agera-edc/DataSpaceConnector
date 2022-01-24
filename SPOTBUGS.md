# ADR on Spotbugs

Spotbugs is a program to find bugs in Java programs. It looks for instances of “bug patterns” — code instances that are likely to be errors by inspecting Java bytecode.

## Configuring SpotBugs

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

## Aggregating reports

### Using aggregator plugin

There is [a plugin](https://github.com/SimonScholz/report-aggregator) available from a developer to perform the aggregation

## Integration with Codacy

## Resource usage

Spotbugs can be quite resource intense. The [effort configuration](https://spotbugs.readthedocs.io/en/stable/effort.html) helps tune Spotbugs accordingly for each individual project.