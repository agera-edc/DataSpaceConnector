/*
 *  Copyright (c) 2022 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.identity;

import com.nimbusds.jose.jwk.ECKey;
import org.eclipse.dataspaceconnector.iam.did.crypto.key.EcPrivateKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.spi.credentials.CredentialsVerifier;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provider;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Objects;

import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.iam.did.spi.document.DidConstants.DID_URL_SETTING;

@Provides(IdentityService.class)
public class DecentralizedIdentityServiceExtension implements ServiceExtension {

    @Inject
    private DidResolverRegistry resolverRegistry;

    @Inject
    private CredentialsVerifier credentialsVerifier;

    @Inject
    private PrivateKeyResolver privateKeyResolver;

    @Inject
    private SignedJwtService signedJwtService;

    @Override
    public String name() {
        return "Distributed Identity Service";
    }

    @Provider
    public IdentityService identityService(ServiceExtensionContext context) {
        return new DecentralizedIdentityService(signedJwtService, resolverRegistry, credentialsVerifier, context.getMonitor());
    }

    @Provider(isDefault = true)
    SignedJwtService createSupplier(ServiceExtensionContext context) {
        var didUrl = context.getSetting(DID_URL_SETTING, null);
        if (didUrl == null) {
            throw new EdcException(format("The DID Url setting '(%s)' was null!", DID_URL_SETTING));
        }

        // we'll use the connector name to restore the Private Key
        var connectorName = context.getConnectorId();
        var privateKey = privateKeyResolver.resolvePrivateKey(connectorName, ECKey.class); //to get the private key
        Objects.requireNonNull(privateKey, "Couldn't resolve private key for " + connectorName);

        return new SignedJwtService(didUrl, connectorName, new EcPrivateKeyWrapper(privateKey));
    }

}
