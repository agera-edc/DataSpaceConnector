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

import java.security.Principal;

class NamedPrincipalAuthenticationContext implements AuthenticationContext {
    private final Principal userPrincipal;
    private final String authenticationScheme;

    NamedPrincipalAuthenticationContext(String userPrincipalName, String authenticationScheme) {
        this.userPrincipal = new NamedPrincipal(userPrincipalName);
        this.authenticationScheme = authenticationScheme;
    }

    @Override
    public boolean isUserInRole(String role) {
        return false;
    }

    @Override
    public Principal getUserPrincipal() {
        return userPrincipal;
    }

    @Override
    public String getAuthenticationScheme() {
        return authenticationScheme;
    }
}
