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
package org.eclipse.dataspaceconnector.transfer.provision.http;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import static java.util.Objects.requireNonNull;

/**
 * A resource to be provisioned by an external HTTP endpoint.
 */
@JsonTypeName("dataspaceconnector:httpproviderresourcedefinition")
@JsonDeserialize(builder = HttpProviderResourceDefinition.Builder.class)
public class HttpProviderResourceDefinition extends AbstractHttpResourceDefinition {
    private String assetId;

    public String getAssetId() {
        return assetId;
    }

    private HttpProviderResourceDefinition() {
        super();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends AbstractHttpResourceDefinition.Builder<HttpProviderResourceDefinition, Builder> {

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder assetId(String assetId) {
            resourceDefinition.assetId = assetId;
            return this;
        }

        @Override
        protected void verify() {
            super.verify();
            requireNonNull(resourceDefinition.assetId, "assetId");
        }

        private Builder() {
            super(new HttpProviderResourceDefinition());
        }

    }

}
