package org.eclipse.dataspaceconnector.api.mapper;

import org.eclipse.dataspaceconnector.common.string.StringUtils;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StringToEnumMapper<E extends Enum<E>> {

    private final Class<E> outputType;
    private final String targetName;

    public StringToEnumMapper(Class<E> outputType, String targetName) {
        this.outputType = outputType;
        this.targetName = targetName;
    }

    public @Nullable E transform(@Nullable String name, @NotNull TransformerContext context) {
        if (StringUtils.isNullOrBlank(name)) {
            return null;
        }
        try {
            return Enum.valueOf(outputType, name);
        } catch (IllegalArgumentException e) {
            context.reportProblem(String.format("Invalid value for %s", targetName));
            return null;
        }
    }
}
