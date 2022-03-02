# EDC Repository Split

Currently, the EDC codebase is stored in a monorepo in Github where core modules coexist with vendor-specific extensions. This strategy allows for flexibility especially during the initial phase of development where bigger application-wide refactorings are common. Integration tests spanning several extensions are easy to write and help to reduce the likelihood of a regression within a certain module affecting other modules. 

As more contributors join the project and more vendor-specific implementations are added to the codebase, the drawbacks of a monorepo become apparent:

- A new version of an SPI forces an immediate adaptation of all implementing modules. This requires coordination among several teams, ultimately slowing down the development cycle.
- A new version of a module or extension requires a full EDC release, even for small fixes.
- The full test suite run for any change is suboptimal. Having separate smaller modules improves CI performance, but this can be achieved as well on a monorepo by having multiple parallel CI pipelines.

## Monorepo/Multirepo comparison

| Monorepo                                                                              | Multirepo                                                                                               | 
|---------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------|
| Easier to do Application wide refactoring                                             | Easier to do small refactorings of modules/libraries                                                    |
| More difficult to break functionality </br>(they are visible and can be solved early) | Easy to break the functionality, but breaking changes can be released (new version)                     | 
| Central code management (easier to navigate the code)                                 | Not easy to locate the code/debug (need for searching in multiple repos)                                |
| Easier to share and maintain one Development culture (style guides etc.)              | Working autonomously on the library code (different development cultures)                               | 
| Slow CI builds                                                                        | Faster CI builds                                                                                        | 
| Introducing breaking changes slows the development cycle                              | Breaking changes are released as a new version, do not slow down development cycles of other libraries  |
| Heavyweight codebase                                                                  | Multiple lightweight codebases                                                                          |
| Lower barriers of entry (everything in one place) to understand the project           | Contributing is easier (forking, no need to understand the whole repo)                                  |
| Good white box testing because all projects are testable together                     | Good black box testing because each project is testable separately, and verifiable independently        |

## EDC multirepo strategy

### By vendor

EDC defines a series of core APIs in the [spi](../../../../spi) module. Implementations for these APIs are provided by using extensions. 
An example of this is `TransferProcessStore` in the `transfer-spi` core module with available implementations `InMemoryTransferProcessStore` in the `transfer-store-memory` extension, and a `CosmosTransferProcessStore` in the `transfer-process-store-cosmos` extension. 
This option involves splitting a repo for each vendor providing an implementation for core SPI interfaces: 

```
<EDC> (in EDC repo)
    |_ <InMemory Extensions> (in EDC repo)
    |
    |_ <Azure Extensions> (in Azure repo)
    |_ <AWS Extensions> (in AWS repo)
    |_ <Google Extensions> (in Google repo)
```

Important considerations:
- A default implementation that can be used in EDC core to perform testing is required (for instance an in-memory version of a store like `InMemoryTransferProcessStore`).
- A set of integration tests must be provided by the core EDC repository to verify that components work as expected using the default implementation. These test can be reused by other implementations to verify conformance.
- Separate release cycles for vendor extensions is possible, but an adequate versioning strategy is required to simplify understanding of compatibility with EDC core (for instance Azure Extensions 1.1.x are always compatible with EDC core 1.1)
- EDC repository lacks of real world implementations as they are moved to vendor repositories. This might lead to misunderstandings within the EDC developer community. 

### By domains (microservices)

This options opts for a split by microservices. At the time of writing 2 architectural components come into question for such a split: EDC Core and DPF. 

```
<EDC Core> (in EDC repo)
    |_ <In Memory Extensions> (in EDC repo)
    |_ <Azure Extensions> (in EDC repo)
    |_ <AWS Extensions> (in EDC repo)
    
<DPF> (in DPF repo)
    |_ <In Memory Extensions> (in DPF repo)
    |_ <Azure Extensions> (in DPF repo)
```

Important considerations:
- Well-defined and stable interfaces between microservices at a higher level than programmatically 
- Split by microservice allows for more flexibility on appropriate technology choices for each service
- Separate release and deployment cycles
- Requires overarching set of E2E tests

## TBD

-> lifecycle of a change with multirepo
-> define possible next steps: improve in memory impls to match what Azure impls do and add tests


### Dependency analysis

This section shows if there are any blockers or tightly coupled dependencies that can have impact on the repo split process (for the current state of the 
repository).

It uses Intellij Dependency Matrix tool (Code -> Analyse Code -> Dependency Matrix).

#### By domains (microservices)

Currently only microservice in the repository is DPF. There aro no dependencies in the core modules that depend on data-plane or its extensions.

Below dependency matrix shows azure -> data-plane-azure-storage uses data-plane. Both will be a part of new repo in this scenario.

![Dependency matrix](dependency_matrix_1.png)

#### By vendors

Dependency that would need to be solved to extract Azure as a separate repo:

Azure events-config is used in iam->decentralized-identity->registration-service.

![Dependency matrix](dependency_matrix_2.png)
