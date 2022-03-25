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
import org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.model.TransferProcessDto;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformer;
import org.eclipse.dataspaceconnector.common.string.StringUtils;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.eclipse.dataspaceconnector.spi.transformer.TypeTransformer;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class TransferProcessDtoToTransferProcessTransformer implements DtoTransformer<TransferProcessDto, TransferProcess> {

    private final TypeTransformer<DataRequestDto, DataRequest> dataRequestTransformer;
    private final TypeTransformer<String, TransferProcess.Type> typeEnumTransformer =
            new StringToEnumTransformer<>(TransferProcess.Type.class, "TransferProcess.type");
    private final TypeTransformer<String, TransferProcessStates> stateEnumTransformer =
            new StringToEnumTransformer<>(TransferProcessStates.class, "TransferProcess.state");

    public TransferProcessDtoToTransferProcessTransformer(TypeTransformer<DataRequestDto, DataRequest> dataRequestTransformer) {
        this.dataRequestTransformer = dataRequestTransformer;
    }

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
                .type(typeEnumTransformer.transform(object.getType(), context))
                .state(Objects.requireNonNullElse(stateEnumTransformer.transform(object.getState(), context), TransferProcessStates.UNSAVED).code())
                .dataRequest(dataRequestTransformer.transform(object.getDataRequest(), context))
                .errorDetail(object.getErrorDetail())
                .build();
    }

    static class StringToEnumTransformer<E extends Enum<E>> implements TypeTransformer<String, E> {

        private final Class<E> outputType;
        private final String fieldName;

        StringToEnumTransformer(Class<E> outputType, String fieldName) {
            this.outputType = outputType;
            this.fieldName = fieldName;
        }

        @Override
        public Class<String> getInputType() {
            return String.class;
        }

        @Override
        public Class<E> getOutputType() {
            return outputType;
        }

        @Override
        public @Nullable E transform(@Nullable String name, @NotNull TransformerContext context) {
            if (StringUtils.isNullOrBlank(name)) {
                return null;
            }
            try {
                return Enum.valueOf(outputType, name);
            } catch (IllegalArgumentException e) {
                context.reportProblem(String.format("Invalid value for %s", fieldName));
                return null;
            }
        }
    }
}
