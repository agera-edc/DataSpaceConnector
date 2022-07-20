/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.api.auth;

import org.eclipse.dataspaceconnector.spi.result.Result;

import java.util.List;
import java.util.Map;

/**
 * All pass implementation of AuthenticationService. Not to be used in a production environment
 */
public class AllPassAuthenticationService implements AuthenticationService {

    private static final String ALL_PASS = "ALL_PASS";

    @Override
    public Result<? extends AuthenticationContext> authenticate(Map<String, List<String>> headers) {
        return Result.success(new NamedPrincipalAuthenticationContext(ALL_PASS, ALL_PASS));
    }
}
