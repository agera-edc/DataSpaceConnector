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
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.api.auth;

import jakarta.ws.rs.core.SecurityContext;

import java.security.Principal;

public class AuthenticationContextSecurityContext implements SecurityContext {
    private final AuthenticationContext authenticationContext;

    public AuthenticationContextSecurityContext(AuthenticationContext authenticationContext) {
        this.authenticationContext = authenticationContext;
    }

    @Override
    public Principal getUserPrincipal() {
        return authenticationContext.getUserPrincipal();
    }

    @Override
    public boolean isUserInRole(String role) {
        return authenticationContext.isUserInRole(role);
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public String getAuthenticationScheme() {
        return authenticationContext.getAuthenticationScheme();
    }
}
