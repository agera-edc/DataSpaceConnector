# JWT hardening

## Decision

A JWT token sent to another participant other than the one it was initially intended for must be rejected. Use the DID of the message receiver as the JWT "audience" claim to achieve this. Use a central registry to resolve DIDs for dataspace participants. 

## Rationale

[Json Web Tokens (JWT)](https://datatracker.ietf.org/doc/html/rfc7519) are used in EDC as means to authenticate IDS requests. The current implementation allows for a malicious entity that gets ahold of a JWT to impersonate the original sender, and thus send requests to any other participant. Two categories or impersonation attacks are possible depending on how the JWT used for the impersonation attack is obtained:

1. "JWT reuse": A malicious participant might reuse JWTs sent to him as provider, to send requests to other participants as a consumer impersonating the signer of the JWT.
2. "JWT leak": A malicious attacker that gets hold of a leaked JWT token might use it to send request to any participant impersonating the signer of the JWT.
 
Next we discuss solutions for these two cases.

### JWT reuse

The JWT spec defines the [audience](https://datatracker.ietf.org/doc/html/rfc7519#section-4.1.3) claim to identify the JWT target of a token to protect from this security issue. A JWT token sent to a participant different than the one it was initially intended for will be rejected.

The DID of the target participant needs to be used as JWT audience as this is the global identifier for each participant. Currently, the consumer participant has no way to know the DID of the provider. There are 3 possible ways to solve this:

1. Have the provider deliver this information during the IDS information exchange. This would require to adapt the IDS communication flow to always include a step where the provider shares this information in a secure way.
2. Have a central register keep track of the DIDs for each participant. The registry is a component already existing in the current dataspace instances to hold URLs of participants, it would only need to be extended to contain the DID for each participant as well.
3. Locate the DID in a well-known location in same domain as the URL of the corresponding participant (DID domain matching) so that it can be retrieved by the consumer.

As a first step we will implement a solution based on the second item, as this involves the least effort and bases on an already existing component in the current dataspace implementations (the registry).

### JWT leak

This is a less critical case as the first one, as it involves a previous JWT leak through a system security hole.

DID domain matching (solution 3. in the previous section) might also solve this security issue in some scenarios. The DID of the consumer available as issuer of the JWT can be matched against the URL of the request sender. Nonetheless, resolving the origin URL can have complications as this can be manipulated by network components like gateways, leaving with no reliable way to get to this information.

Given the lower probability of this security issue, we decide to go with no solution other than preventing JWT leaks themselves through common security best practices. 