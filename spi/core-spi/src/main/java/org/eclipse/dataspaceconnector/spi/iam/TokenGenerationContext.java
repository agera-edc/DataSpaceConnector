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
 *
 */

package org.eclipse.dataspaceconnector.spi.iam;

public class TokenGenerationContext {
    private final String scope;
    private final String audience;

    public TokenGenerationContext(String scope, String audience) {
        this.scope = scope;
        this.audience = audience;
    }

    public String getScope() {
        return scope;
    }

    public String getAudience() {
        return audience;
    }
}
