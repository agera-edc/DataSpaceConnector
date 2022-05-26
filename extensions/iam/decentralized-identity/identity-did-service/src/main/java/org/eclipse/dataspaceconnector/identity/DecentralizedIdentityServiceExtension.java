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

import org.eclipse.dataspaceconnector.common.jsonweb.crypto.spi.PrivateKeyWrapper;
import org.eclipse.dataspaceconnector.common.token.JwtDecoratorRegistry;
import org.eclipse.dataspaceconnector.common.token.TokenGenerationService;
import org.eclipse.dataspaceconnector.common.token.TokenGenerationServiceImpl;
import org.eclipse.dataspaceconnector.common.token.TokenValidationRulesRegistry;
import org.eclipse.dataspaceconnector.common.token.TokenValidationService;
import org.eclipse.dataspaceconnector.common.token.TokenValidationServiceImpl;
import org.eclipse.dataspaceconnector.iam.did.spi.credentials.CredentialsVerifier;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.iam.PublicKeyResolver;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provider;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.time.Duration;
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
    private JwtDecoratorRegistry decoratorRegistry;

    @Inject
    private TokenValidationRulesRegistry validationRulesRegistry;

    @Inject
    private PrivateKeyResolver privateKeyResolver;

    @Inject
    private PublicKeyResolver publicKeyResolver;

    @Inject
    private TokenValidationRulesRegistry tokenValidationRulesRegistry;

    @Override
    public String name() {
        return "Distributed Identity Service";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var didUrl = context.getSetting(DID_URL_SETTING, null);
        if (didUrl == null) {
            throw new EdcException(format("The DID Url setting '(%s)' was null!", DID_URL_SETTING));
        }

        // we'll use the connector name to restore the Private Key
        var connectorName = context.getConnectorId();

        decoratorRegistry.register(new DidJwtDecorator(didUrl, connectorName, Duration.ofMinutes(10)));
        validationRulesRegistry.addRule(new DidJwtValidationRule());
    }

    @Provider
    public IdentityService identityService(ServiceExtensionContext context) {
        var tokenGenerationService = createTokenGenerationService(context);
        var tokenValidationService = (TokenValidationService) new TokenValidationServiceImpl(publicKeyResolver, tokenValidationRulesRegistry);
        return new DecentralizedIdentityService(tokenGenerationService, tokenValidationService, resolverRegistry, credentialsVerifier, context.getMonitor(), decoratorRegistry);
    }

    public TokenGenerationService createTokenGenerationService(ServiceExtensionContext context) {
        String connectorId = context.getConnectorId();
        var privateKey = privateKeyResolver.resolvePrivateKey(connectorId, PrivateKeyWrapper.class);
        Objects.requireNonNull(privateKey, format("Private key for connectorId %s not found", connectorId));
        return new TokenGenerationServiceImpl(privateKey);
    }
}
