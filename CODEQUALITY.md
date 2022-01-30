# ADR on Code Quality

## Overview of tools

| Tool       | Analysis Type | Description                                                                                                                                                                                                            |
|------------|---------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Checkstyle | Source code   | Focuses on coding style making sure a team adheres to some standards including naming, braces, indentation, javadoc on public methods, etc.                                                                            |
| PMD        | Source code   | Focuses on common programming flaws like unused variables, empty catch blocks, unnecessary object creation. PMD also tells you about the [Cyclomatic complexity](https://en.wikipedia.org/wiki/Cyclomatic_complexity). |
| Spotbugs   | Byte code     | Focuses on common programming flaws that can not be found with source code analysis like infinite loop, equals method always returns true, opened streams, a collection which contains itself, etc.                    |
| CodeQL     | Source code   | A semantic code analysis engine. CodeQL treats code as data, allowing you to find potential vulnerabilities in your code with greater confidence than traditional static analyzers.                                    |

## Evaluation

- [Checkstyle](CHECKSTYLE.md)
- [PMD](PMD.md)
- [Spotbugs](SPOTBUGS.md)
- [CodeQL](CODEQL.md)

## Comparison of tools

| Tool                                     | Criticality of results | Breadth of results | Noise ratio (false positives) | Impact on build time      | Tool maturity      | Comments                                                                                            |
|------------------------------------------|------------------------|--------------------|-------------------------------|---------------------------|--------------------|-----------------------------------------------------------------------------------------------------|
| PMD                                      | ⚠️ medium-low          | ✅ large            | ⚠️ high                       | ✅ medium                  | ✅ well established | ⚠️ Requires usage of IDE plugin                                                                     | 
| Spotbugs                                 | ✅ high                 | ✅ large            | ⚠️ medium                     | ⚠️ high                   | ✅ well established | ⚠️ Requires usage of IDE plugin                                                                     |
| CodeQL without [LGTM](https://lgtm.com/) | ✅ high                 | ⚠️ medium          | ✅ low                         | ✅ low (parallel pipeline) | ⚠️ relatively new  | ⚠️ Only few security rules<br/> ⚠️ Does not support suppressions <br/> ✅ Already in use in EDC repo |
| CodeQL with [LGTM](https://lgtm.com/)    | ✅ high                 | ✅ large            | ✅ low                         | ✅ low (parallel pipeline) | ⚠️ relatively new  | ⚠️ Dependency to external tool                                                                      |
