# ADR on Code Quality

## Overview of tools

| Tool       | Analysis Type | Description                                                                                                                                                                                                                                                                                                              |
|------------|---------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Checkstyle | Source code   | Focuses on coding style making sure a team adheres to some standards including naming, braces, indentation, javadoc on public methods, etc.                                                                                                                                                                              |
| PMD        | Source code   | Focuses on common programming flaws like unused variables, empty catch blocks, unnecessary object creation. PMD also tells you about the [Cyclomatic complexity](https://en.wikipedia.org/wiki/Cyclomatic_complexity).                                                                                                   |
| Spotbugs   | Byte code     | Focuses on common programming flaws that can not be found with source code analysis like infinite loop, equals method always returns true, opened streams, a collection which contains itself, etc. The [FindSecBugs](https://find-sec-bugs.github.io/) plugin extends Spotbugs with the detection of 141 security bugs. |
| CodeQL     | Source code   | A semantic code analysis engine. CodeQL treats code as data, allowing you to find potential vulnerabilities in your code with greater confidence than traditional static analyzers.                                                                                                                                      |

## Overview of platforms

| Platform | Description                                                                                                                                            |
|----------|--------------------------------------------------------------------------------------------------------------------------------------------------------|
| Github   | Source code repository used by EDC. Different tools can be integrated into CI workflows using Github actions.                                          |
| Codacy   | Online platform for both static code analysis and test code coverage analysis. It is free for Open Source projects.                                    |
| LGTM     | Online platform for static code analysis using deep semantic code search with data science insights using CodeQL. It is free for Open Source projects. |

## Evaluation

- [Checkstyle](quality-tools/CHECKSTYLE.md)
- [PMD](quality-tools/PMD.md)
- [Spotbugs](quality-tools/SPOTBUGS.md)
- [CodeQL](quality-tools/CODEQL.md)

## Comparison of tools

The following table summarizes the strengths and weaknesses of the analyzed tools. Some of these tools are not exclusive to each other, but rather complementing. As a matter of fact it is very common to see setups with Checkstyle combined with PMD and/or Spotbugs. Checkstyle complements as well CodeQL but on the other hand CodeQL (with LGTM) overlaps with Spotbugs and PMD quite a bit, probably not bringing much value using these tools in combination.

| Tool                                 | Pertinence of results* | Breadth of results | Noise ratio (false positives) | Execution time | Tool maturity      | Comments                                                                                                                 |
|--------------------------------------|------------------------|--------------------|-------------------------------|----------------|--------------------|--------------------------------------------------------------------------------------------------------------------------|
| Checkstyle                           | ✅ high                 | ✅ large            | ✅ low                         | ✅ low          | ✅ well established | ✅ Already in use in EDC repo                                                                                             |
| PMD                                  | ⚠️ medium-low          | ✅ large            | ⚠️ high                       | ✅ medium       | ✅ well established | ⚠️ No result aggregation (IDE plugin recommended)                                                                        | 
| Spotbugs                             | ✅ high                 | ✅ large            | ⚠️ medium                     | ⚠️ high        | ✅ well established | ⚠️ No result aggregation (IDE plugin recommended)                                                                        |
| CodeQL with default java query pack  | ✅ high                 | ⚠️ medium          | ✅ low                         | ⚠️ high        | ⚠️ relatively new  | ⚠️ Only few security rules<br/> ⚠️ Does not support suppressions <br/> ✅ CodeQL Github Action already in use in EDC repo |
| CodeQL with extended java query pack | ✅ high                 | ✅ large            | ✅ low                         | ⚠️ high        | ⚠️ relatively new  | ⚠️ Dependency to external tool<br/>✅ CodeQL Github Action already in use in EDC repo                                     |

&ast; Pertinence of results refers to the importance/relevance of findings as estimated by the developer who wrote this documentation

## Comparison of platforms

| Platform                                                                    | Code coverage                                                                        | Code quality analysis                                                                                  | PR reports                                                                                          | Usage | 
|-------------------------------------------------------------------------|------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------|------------------------------|
| Github Actions*                                                             | ✅ available ([JaCoCo Report](https://github.com/marketplace/actions/jacoco-report)) | ✅ available ([Checkstyle](https://github.com/marketplace/actions/run-java-checkstyle), PMD, Spotbugs) | ✅ available                                                                                        | ⚠️ Every tool needs to be added to a workflow and configured manually<br/> ✅ The configuration is maintained in the repository (not external websites) |
| LGTM                                                                        | -                                                                                    | ✅ available (CodeQL)                                                                                  | ✅ available                                                                                        | ⚠️ Doesn't work on forks |
| Codecov                                                                     | ✅ available                                                                         | -                                                                                                      | ✅ available (PR comments added by a bot)                                                           | ✅ Measures available directly in Github and in the separate dashboard       |                                                             
| Codacy                                                                      | ✅ available                                                                         | ✅ available                                                                                           | ⚠️ Not supported for code coverage<br/>✅ available for code quality analysis                       | ✅ A lot of code quality scanning tools built in<br/> ⚠️ Code coverage view very basic<br/> ⚠️ Tools configuration maintained in Codacy dashboard<br/> ⚠️ Reports are sometimes delayed |

* Explicit usage of Github Actions from the [marketplace](https://github.com/marketplace?type=actions)
