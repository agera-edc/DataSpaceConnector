# EDC Repository Split

## Monorepo/ Multirepo comparison

| Monorepo         | Multirepo     | Choice for EDC |
|--------------|-----------|------------|
| Easier to do Application wide refactoring | Easier to do small refactorings of modules/libraries     | Multirepo <br/>EDC bases on extensions, no need to do application wide refactorings  |
| More difficult to break functionality </br>(they are visible and can be solved early) | Easy to break the functionality, but breaking changes can be released (new version) | Multirepo <br> Favors faster development cycles. |
| Central code management (easier to locate the code) | Not easy to locate the code/debug (need for searching in multiple repos) | ? |
| Easier to share and maintain one Development culture (style guides etc.) | Working autonomously on the library code (different development cultures) | Monorepo <br>Keeping consistent development culture can be beneficial. |
| Slow CI builds | Faster CI builds | Multirepo |
| Introducing breaking changes slows the development cycle | Breaking changes are released as a new version, do not slow down development cycles of other libraries | Multirepo <br> Libraries can have different speeds of development cycles. |
| Heavy codebase | Multiple lightweight codebases  | Multirepo |
| Lower barriers of entry (everything in one place) to understand the project | Contributing is easier (forking, no need to understand the whole repo) | Multirepo </br>No need to understand the whole repository to contribute. |