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

import org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.model.TransferProcessDto;
import org.eclipse.dataspaceconnector.api.mapper.StringToEnumMapper;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformer;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class TransferProcessDtoToTransferProcessTransformer implements DtoTransformer<TransferProcessDto, TransferProcess> {

    private final StringToEnumMapper<TransferProcess.Type> typeMapper =
            new StringToEnumMapper<>(TransferProcess.Type.class, "TransferProcess.type");
    private final StringToEnumMapper<TransferProcessStates> stateMapper =
            new StringToEnumMapper<>(TransferProcessStates.class, "TransferProcess.state");

    @Override
    public Class<TransferProcessDto> getInputType() {
        return TransferProcessDto.class;
    }

    @Override
    public Class<TransferProcess> getOutputType() {
        return TransferProcess.class;
    }

    @Override
    public boolean canHandle(@NotNull Object object, @NotNull Class<?> outputType) {
        return getInputType().isInstance(object) && getOutputType().equals(outputType);
    }

    @Override
    public @Nullable TransferProcess transform(@Nullable TransferProcessDto object, @NotNull TransformerContext context) {
        Objects.requireNonNull(context);
        if (object == null) {
            return null;
        }

        return TransferProcess.Builder.newInstance()
                .id(object.getId())
                .type(typeMapper.transform(object.getType(), context))
                .state(Objects.requireNonNullElse(stateMapper.transform(object.getState(), context), TransferProcessStates.UNSAVED).code())
                .dataRequest(context.transform(object.getDataRequest(), DataRequest.class))
                .errorDetail(object.getErrorDetail())
                .build();
    }
}
