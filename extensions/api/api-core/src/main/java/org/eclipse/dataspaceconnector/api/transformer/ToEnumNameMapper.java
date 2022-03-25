package org.eclipse.dataspaceconnector.api.transformer;

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
