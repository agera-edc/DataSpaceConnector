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

package org.eclipse.dataspaceconnector.common.jsonweb.crypto.spi;

import org.eclipse.dataspaceconnector.common.jsonweb.crypto.spi.PublicKeyWrapper;
import org.eclipse.dataspaceconnector.spi.result.Result;

public interface PublicKeyWrapperResolver {

    /**
     * Resolves the public key.
     */
    Result<PublicKeyWrapper> resolvePublicKey(String did);
}
