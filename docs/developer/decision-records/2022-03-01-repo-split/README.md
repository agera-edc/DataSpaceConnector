# EDC Repository Split

Currently, the EDC codebase is stored in a monorepo in Github where core modules coexist with vendor-specific extensions. This strategy allows for flexibility especially during the initial phase of development where bigger application-wide refactorings are common. Integration tests spanning several extensions are easy to write and help to reduce the likelihood of a regression within a certain module affecting other modules. 

As more contributors join the project and more vendor-specific implementations are added to the codebase, the drawbacks of a monorepo become apparent:

- A new version of an API forces an immediate adaptation of all implementing modules. This requires coordination among several teams, ultimately slowing down the development cycle.
- A new version of a module or extension requires a full EDC release, even for small fixes.
- The full test suite is always run for any change. Having separate smaller modules will improve CI performance.

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

## EDC multirepo strategy

### By vendor

EDC defines a series of core APIs in the [spi](../../../../spi) module. Implementations for these APIs are provided by using extensions. An example of this is `TransferProcessStore` in the `transfer-spi` core module with available implementations `InMemoryTransferProcessStore` in the `transfer-store-memory` extension, and a `CosmosTransferProcessStore` in the `transfer-process-store-cosmos` extension.

```
<EDC Core>
    |_ <Azure Extensions>
    |_ <AWS Extensions>
    |_ <Google Extensions>
```

Vendor splits are meaningful whenever there is at least a default implementation that can be used in EDC core to perform testing (for instance an in-memory version of a store like `InMemoryTransferProcessStore`).



