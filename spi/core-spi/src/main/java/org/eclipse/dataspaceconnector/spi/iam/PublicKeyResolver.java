/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.spi.iam;

import org.eclipse.dataspaceconnector.spi.result.Result;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;

/**
 * Resolves an RSA public key.
 */
@FunctionalInterface
public interface PublicKeyResolver {

    /**
     * Resolves the key.
     */
    @NotNull Result<? extends PublicKey> resolveKey(String id);
}
