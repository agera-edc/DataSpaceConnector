# ADR on code quality tooling

## Checkstyle 

TBD

## PMD

TBD

## Spotbugs

[Spotbugs](https://spotbugs.github.io/) is a program which uses static analysis to look for bugs in Java code. It looks for instances of “bug patterns” — code instances that are likely to be errors by inspecting Java bytecode.

### Running SpotBugs with Gradle

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

#### Aggregating reports

There is [a plugin](https://github.com/SimonScholz/report-aggregator) available to perform the aggregation. The plugin seems not to be in active development and gives an error when trying to run it:

```
 In plugin 'com.simonscholz.reports' type 'com.simonscholz.report.GenerateAggregatedReportTask' property 'level' is missing an input or output annotation.
```

For the moment it looks like a more custom solution with a XSL aggregation/transformation of Spotbugs XML output files is the most promising approach.

### Findsecbugs plugin

The [Find Security Bugs](https://find-sec-bugs.github.io/) plugin extends Spotbugs with additional security checks.

#### Resource usage

Spotbugs can be quite resource intense. The [effort configuration](https://spotbugs.readthedocs.io/en/stable/effort.html) helps tune Spotbugs accordingly for each individual project.

### Running Spotbugs with Codacy

According to [the docs](https://docs.codacy.com/related-tools/local-analysis/running-spotbugs/), codacy supports Spotbugs integration. 

We configured a [pipeline](.github/workflows/codacy-analysis.yaml) to run Spotbugs on pushes, but we were not able to get the analysis running, as it seems that classes and sources are not detected properly despite trying to define class/source directories as described in the [documentation](https://docs.codacy.com/related-tools/local-analysis/running-spotbugs/#detecting-sources-and-compiled-classes). See [failing CI run](https://github.com/Agera-CatenaX/EclipseDataSpaceConnector/runs/4925578885?check_suite_focus=true).

```
Error executing the tool
java.io.IOException: IOException while scanning codebases
	at edu.umd.cs.findbugs.FindBugs2.execute(FindBugs2.java:311)
	at com.codacy.tools.spotbugs.SpotBugs$.$anonfun$runTool$1(SpotBugs.scala:113)
	at scala.util.Try$.apply(Try.scala:213)
	at com.codacy.tools.spotbugs.SpotBugs$.runTool(SpotBugs.scala:82)
	at com.codacy.tools.spotbugs.SpotBugs$.$anonfun$apply$9(SpotBugs.scala:52)
	at scala.collection.TraversableLike.$anonfun$map$1(TraversableLike.scala:285)
	at scala.collection.immutable.Set$Set2.foreach(Set.scala:181)
	at scala.collection.TraversableLike.map(TraversableLike.scala:285)
	at scala.collection.TraversableLike.map$(TraversableLike.scala:278)
	at scala.collection.AbstractSet.scala$collection$SetLike$$super$map(Set.scala:53)
	at scala.collection.SetLike.map(SetLike.scala:105)
	at scala.collection.SetLike.map$(SetLike.scala:105)
	at scala.collection.AbstractSet.map(Set.scala:53)
	at com.codacy.tools.spotbugs.SpotBugs$.apply(SpotBugs.scala:53)
	at com.codacy.tools.scala.seed.DockerEngine.executeTool(DockerEngine.scala:53)
	at com.codacy.tools.scala.seed.DockerEngine.$anonfun$main$2(DockerEngine.scala:35)
	at scala.util.Success.$anonfun$map$1(Try.scala:255)
	at scala.util.Success.map(Try.scala:213)
	at com.codacy.tools.scala.seed.DockerEngine.$anonfun$main$1(DockerEngine.scala:27)
	at scala.util.Success.flatMap(Try.scala:251)
	at com.codacy.tools.scala.seed.DockerEngine.main(DockerEngine.scala:26)
	at com.codacy.tools.spotbugs.Engine.main(Engine.scala)
Caused by: edu.umd.cs.findbugs.classfile.ResourceNotFoundException: Resource not found: java/lang/Object.class
	at edu.umd.cs.findbugs.classfile.impl.ClassPathImpl.lookupResource(ClassPathImpl.java:162)
	at edu.umd.cs.findbugs.classfile.impl.ClassPathBuilder.build(ClassPathBuilder.java:282)
	at edu.umd.cs.findbugs.FindBugs2.buildClassPath(FindBugs2.java:708)
	at edu.umd.cs.findbugs.FindBugs2.execute(FindBugs2.java:245)
	... 21 more
```

### Reported EDC bugs

We did a quick evaluation of high priority bugs (P1) as reported by Spotbugs:

| Bug                                                | Description | Fix effort |
|----------------------------------------------------| ----------- |------------|
| Reliance on default encoding (DM_DEFAULT_ENCODING) | Found a call to a method which will perform a byte to String (or String to byte) conversion, and will assume that the default platform encoding is suitable. This will cause the application behaviour to vary between platforms. Use an alternative API and specify a charset name or Charset object explicitly     | low        |
| Field isn't final but should be (MS_SHOULD_BE_FINAL) | This static field public but not final, and could be changed by malicious code or by accident from another package. The field could be made final to avoid this vulnerability.      | low        |
