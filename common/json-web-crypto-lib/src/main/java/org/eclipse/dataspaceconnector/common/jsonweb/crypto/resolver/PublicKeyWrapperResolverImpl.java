/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.common.jsonweb.crypto.resolver;

import org.eclipse.dataspaceconnector.common.jsonweb.crypto.key.EcPublicKeyWrapper;
import org.eclipse.dataspaceconnector.common.jsonweb.crypto.spi.PublicKeyWrapper;
import org.eclipse.dataspaceconnector.common.jsonweb.crypto.spi.PublicKeyWrapperResolver;
import org.eclipse.dataspaceconnector.spi.iam.PublicKeyResolver;
import org.eclipse.dataspaceconnector.spi.result.Result;

import java.security.interfaces.ECPublicKey;

public class PublicKeyWrapperResolverImpl implements PublicKeyWrapperResolver {

    private final PublicKeyResolver resolver;

    public PublicKeyWrapperResolverImpl(PublicKeyResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public Result<PublicKeyWrapper> resolvePublicKey(String didUrl) {
        var key = resolver.resolvePublicKey(didUrl);
        if (key.failed()) {
            return Result.failure("Could not retrieve public key");
        }
        try {
            return Result.success(new EcPublicKeyWrapper((ECPublicKey) key.getContent()));
        } catch (IllegalArgumentException e) {
            return Result.failure("Public key was not a valid EC key. Details: " + e.getMessage());
        }
    }

}
