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
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.eclipse.dataspaceconnector.spi.transformer.TypeTransformer;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Function;

public class TransferProcessToTransferProcessDtoTransformer implements DtoTransformer<TransferProcess, TransferProcessDto> {

    private final TypeTransformer<Integer, String> typeEnumTransformer =
            new ToEnumNameTransformer<>(
                    new ToEnumTransformer<>(Integer.class, TransferProcessStates.class, TransferProcessStates::from, "TransferProcess.state"));

    @Override
    public Class<TransferProcess> getInputType() {
        return TransferProcess.class;
    }

    @Override
    public Class<TransferProcessDto> getOutputType() {
        return TransferProcessDto.class;
    }

    @Override
    public boolean canHandle(@NotNull Object object, @NotNull Class<?> outputType) {
        return getInputType().isInstance(object) && getOutputType().equals(outputType);
    }

    @Override
    public @Nullable TransferProcessDto transform(@Nullable TransferProcess object, @NotNull TransformerContext context) {
        Objects.requireNonNull(context);
        if (object == null) {
            return null;
        }
        return TransferProcessDto.Builder.newInstance()
                .id(object.getId())
                .type(object.getType().name())
                .state(typeEnumTransformer.transform(object.getState(), context))
                .errorDetail(object.getErrorDetail())
                .dataRequest(context.transform(object.getDataRequest(), DataRequestDto.class))
                .build();
    }

    static class ToEnumTransformer<T, E extends Enum<E>> implements TypeTransformer<T, E> {

        private final Class<T> inputType;
        private final Class<E> outputType;
        private final Function<T, E> converter;
        private final String fieldName;

        ToEnumTransformer(Class<T> inputType, Class<E> outputType, Function<T, E> converter, String fieldName) {
            this.inputType = inputType;
            this.outputType = outputType;
            this.converter = converter;
            this.fieldName = fieldName;
        }

        @Override
        public Class<T> getInputType() {
            return inputType;
        }

        @Override
        public Class<E> getOutputType() {
            return outputType;
        }

        @Override
        public @Nullable E transform(@Nullable T value, @NotNull TransformerContext context) {
            if (value == null) {
                return null;
            }
            E result = converter.apply(value);
            if (result == null) {
                context.reportProblem(String.format("Invalid value for %s", fieldName));
            }
            return result;
        }
    }

    static class ToEnumNameTransformer<T> implements TypeTransformer<T, String> {

        private final ToEnumTransformer<T, ?> delegate;

        ToEnumNameTransformer(ToEnumTransformer<T, ?> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Class<T> getInputType() {
            return delegate.getInputType();
        }

        @Override
        public Class<String> getOutputType() {
            return String.class;
        }

        @Override
        public @Nullable String transform(@Nullable T value, @NotNull TransformerContext context) {
            var result = delegate.transform(value, context);
            if (result == null) {
                return null;
            }
            return result.name();
        }
    }
}
