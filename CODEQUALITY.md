# ADR on Code Quality

## Overview of tools

| Tool       | Analysis Type | Description                                                                                                                                                                                                                                                                                                              |
|------------|---------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Checkstyle | Source code   | Focuses on coding style making sure a team adheres to some standards including naming, braces, indentation, javadoc on public methods, etc.                                                                                                                                                                              |
| PMD        | Source code   | Focuses on common programming flaws like unused variables, empty catch blocks, unnecessary object creation. PMD also tells you about the [Cyclomatic complexity](https://en.wikipedia.org/wiki/Cyclomatic_complexity).                                                                                                   |
| Spotbugs   | Byte code     | Focuses on common programming flaws that can not be found with source code analysis like infinite loop, equals method always returns true, opened streams, a collection which contains itself, etc. The [FindSecBugs](https://find-sec-bugs.github.io/) plugin extends Spotbugs with the detection of 141 security bugs. |
| CodeQL     | Source code   | A semantic code analysis engine. CodeQL treats code as data, allowing you to find potential vulnerabilities in your code with greater confidence than traditional static analyzers.                                                                                                                                      |

## Evaluation

- [Checkstyle](CHECKSTYLE.md)
- [PMD](PMD.md)
- [Spotbugs](SPOTBUGS.md)
- [CodeQL](CODEQL.md)

## Comparison of tools

The following table summarizes the strengths and weaknesses of the analyzed tools. Some of these tools are not exclusive to each other, but rather complementing. As a matter of fact it is very common to see setups with Checkstyle combined with PMD and/or Spotbugs. Checkstyle complements as well CodeQL but on the other hand CodeQL overlaps with Spotbugs and PMD quite a bit, probably not bringing much value using these tools in combination. 

| Tool                                  | Pertinence of results | Breadth of results | Noise ratio (false positives) | Execution time | Tool maturity      | Comments                                                                                            |
|---------------------------------------|-----------------------|--------------------|-------------------------------|----------------|--------------------|-----------------------------------------------------------------------------------------------------|
| Checkstyle                            | ✅ high                | ✅ large            | ✅ low                         | ✅ low          | ✅ well established | ✅ Already in use in EDC repo                                                                        |
| PMD                                   | ⚠️ medium-low         | ✅ large            | ⚠️ high                       | ✅ medium       | ✅ well established | ⚠️ No result aggregation -> IDE plugin recommended                                                  | 
| Spotbugs                              | ✅ high                | ✅ large            | ⚠️ medium                     | ⚠️ high        | ✅ well established | ⚠️ No result aggregation -> IDE plugin recommended                                                  |
| CodeQL with default java pack         | ✅ high                | ⚠️ medium          | ✅ low                         | ⚠️ high        | ⚠️ relatively new  | ⚠️ Only few security rules<br/> ⚠️ Does not support suppressions <br/> ✅ Already in use in EDC repo |
| CodeQL with [LGTM](https://lgtm.com/) | ✅ high                | ✅ large            | ✅ low                         | ⚠️ high        | ⚠️ relatively new  | ⚠️ Dependency to external tool<br/>✅ CodeQL already in use in EDC repo                              |
