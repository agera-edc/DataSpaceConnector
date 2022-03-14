# Dependency analysis

## Decision

The following

- The [Dependency Analysis Gradle Plugin](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin) is used for detecting unused dependencies and dependencies declared on the wrong configuration (`api` vs `implementation` vs `compileOnly`, etc.).
- Rules are enforced on which module dependencies are allowed within EDC (e.g. no module may depend directly on any of the core modules).
- 

*modules may only depend on -spi modules, with the following exceptions:*
*launchers and tests*
*common*
*extensions:http:jetty (this exception should be removed once there is an SPI for jetty)*
*there should not be "cross-module" dependencies at the same level, for example core:boot should not depend on core:contract. Exceptions*
*core:policy since there is no SPI for it.*
*technology libs, i.e. common modules like azure-commons*
*core:spi cannot depend on any other module*
*no module may depend directly on any of the core modules*
*no module may depend on a launcher, samples or system-tests module*
*no cyclic dependencies, direct or indirect*
*Further validations:*

launchers should not reference two modules providing impls for the same interface
there can not be two modules with identical names (that would confuse Gradle)
there should not be unused dependencies

## Rationale

In order to avoid "invalid" modules dependencies we'll introduce a possibility to check dependencies during build time.

There should be a tool with some sort of rule engine that evaluates every module based on a set of global rules and fails the build if one of them is violated. This tool should be executable as gradle task.

Selecting an appropriate tool is part of this issue.
