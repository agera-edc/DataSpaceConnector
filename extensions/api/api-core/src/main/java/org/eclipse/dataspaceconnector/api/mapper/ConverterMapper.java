package org.eclipse.dataspaceconnector.api.mapper;

import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

class ConverterMapper<INPUT, OUTPUT> {

    private final Function<INPUT, OUTPUT> converter;
    private final String targetName;

    ConverterMapper(Function<INPUT, OUTPUT> converter, String targetName) {
        this.converter = converter;
        this.targetName = targetName;
    }

    public @Nullable OUTPUT transform(@Nullable INPUT value, @NotNull TransformerContext context) {
        if (value == null) {
            return null;
        }
        OUTPUT result = converter.apply(value);
        if (result == null) {
            context.reportProblem(String.format("Invalid value for %s", targetName));
        }
        return result;
    }
}
