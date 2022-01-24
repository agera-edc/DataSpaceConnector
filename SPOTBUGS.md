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

An own report is generated per each Gradle module.

![Spotbugs report](.attachments/spotbugs.png)

## Aggregating reports

TBD 

### Gradle aggregation

TBD 

### Using aggregator plugin

TBD
https://github.com/SimonScholz/report-aggregator

## Resource usage

Spotbugs can be quite resource intense. The [effort configuration](https://spotbugs.readthedocs.io/en/stable/effort.html) helps tune Spotbugs accordingly for each individual project.