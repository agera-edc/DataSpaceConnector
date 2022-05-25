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

package org.eclipse.dataspaceconnector.spi.iam;

/**
 * Data for token generation.
 */
public class TokenGenerationContext {
    private String scope;

    private TokenGenerationContext() {
    }

    /**
     * Returns the scope if existent otherwise null.
     */
    public String getScope() {
        return scope;
    }

    public static class Builder {
        private final TokenGenerationContext result;

        private Builder() {
            result = new TokenGenerationContext();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder scope(String scope) {
            result.scope = scope;
            return this;
        }

        public TokenGenerationContext build() {
            return result;
        }
    }
}
