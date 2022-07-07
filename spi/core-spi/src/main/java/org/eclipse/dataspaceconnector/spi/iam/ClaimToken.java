/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.spi.iam;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Models a token containing claims such as a JWT.
 * Currently only a String representation of claims values is supported.
 */
public class ClaimToken {
    private final Collection<Claim> claims = new ArrayList<>();

    private ClaimToken() {
    }

    /**
     * Returns the claims.
     */
    public Collection<Claim> getClaims() {
        return Collections.unmodifiableCollection(claims);
    }

    public static class Builder {
        private final ClaimToken token;

        private Builder() {
            token = new ClaimToken();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder claim(Claim claim) {
            token.claims.add(claim);
            return this;
        }

        public Builder claims(Collection<Claim> claims) {
            token.claims.addAll(claims);
            return this;
        }

        public ClaimToken build() {
            return token;
        }
    }
}
