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
package org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.transform;

import org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.model.DataRequestDto;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformer;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class DataRequestDtoToDataRequestTransformer implements DtoTransformer<DataRequestDto, DataRequest> {

    @Override
    public Class<DataRequestDto> getInputType() {
        return DataRequestDto.class;
    }

    @Override
    public Class<DataRequest> getOutputType() {
        return DataRequest.class;
    }

    @Override
    public boolean canHandle(@NotNull Object object, @NotNull Class<?> outputType) {
        return getInputType().isInstance(object) && getOutputType().equals(outputType);
    }

    @Override
    public @Nullable DataRequest transform(@Nullable DataRequestDto object, @NotNull TransformerContext context) {
        Objects.requireNonNull(context);
        if (object == null) {
            return null;
        }
        return DataRequest.Builder.newInstance()
                .assetId(object.getAssetId())
                .contractId(object.getContractId())
                .connectorId(object.getConnectorId())
                .build();
    }
}
