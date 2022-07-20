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

/**
 * An injectable interface that provides access to security related information.
 */
public interface AuthenticationContext {

    /**
     * Returns a <code>java.security.Principal</code> object containing the name of the current authenticated user. If the
     * user has not been authenticated, the method returns null.
     *
     * @return a <code>java.security.Principal</code> containing the name of the user making this request; null if the user
     *         has not been authenticated
     * @throws java.lang.IllegalStateException if called outside the scope of a request
     */
    Principal getUserPrincipal();

    /**
     * Returns a boolean indicating whether the authenticated user is included in the specified logical "role". If the user
     * has not been authenticated, the method returns <code>false</code>.
     *
     * @param role a <code>String</code> specifying the name of the role
     * @return a <code>boolean</code> indicating whether the user making the request belongs to a given role;
     *         <code>false</code> if the user has not been authenticated
     * @throws java.lang.IllegalStateException if called outside the scope of a request
     */
    boolean isUserInRole(String role);

    /**
     * Returns the string value of the authentication scheme used to protect the resource. If the resource is not
     * authenticated, null is returned.
     * <p>
     * Values are the same as the CGI variable AUTH_TYPE
     *
     * @return one of the static members BASIC_AUTH, FORM_AUTH, CLIENT_CERT_AUTH, DIGEST_AUTH (suitable for == comparison)
     *         or the container-specific string indicating the authentication scheme, or null if the request was not authenticated.
     * @throws java.lang.IllegalStateException if called outside the scope of a request
     */
    String getAuthenticationScheme();
}
