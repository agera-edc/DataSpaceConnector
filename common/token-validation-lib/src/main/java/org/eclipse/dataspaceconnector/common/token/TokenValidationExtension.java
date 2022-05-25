/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *
 */


package org.eclipse.dataspaceconnector.common.token;

import org.eclipse.dataspaceconnector.spi.system.Provider;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;

public class TokenValidationExtension implements ServiceExtension {
    @Provider(isDefault = true)
    public TokenValidationRulesRegistry tokenValidationRulesRegistry() {
        return new TokenValidationRulesRegistryImpl();
    }
}