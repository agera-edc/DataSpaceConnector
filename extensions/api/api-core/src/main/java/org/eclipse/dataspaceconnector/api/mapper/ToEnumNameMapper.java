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
package org.eclipse.dataspaceconnector.api.mapper;

import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class ToEnumNameMapper<T> {
    private final ConverterMapper<T, Enum<?>> delegate;

    public ToEnumNameMapper(Function<T, Enum<?>> converter, String fieldName) {
        this.delegate = new ConverterMapper<>(converter, fieldName);
    }

    public @Nullable String transform(@Nullable T value, @NotNull TransformerContext context) {
        var result = delegate.transform(value, context);
        if (result == null) {
            return null;
        }
        return result.name();
    }
}
