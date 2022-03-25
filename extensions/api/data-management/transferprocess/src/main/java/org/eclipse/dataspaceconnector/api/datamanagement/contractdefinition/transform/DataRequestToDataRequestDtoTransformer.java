package org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.transform;

import org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.model.DataRequestDto;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformer;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class DataRequestToDataRequestDtoTransformer implements DtoTransformer<DataRequest, DataRequestDto> {

    @Override
    public Class<DataRequest> getInputType() {
        return DataRequest.class;
    }

    @Override
    public Class<DataRequestDto> getOutputType() {
        return DataRequestDto.class;
    }

    @Override
    public @Nullable DataRequestDto transform(@Nullable DataRequest object, @NotNull TransformerContext context) {
        Objects.requireNonNull(context);
        if (object == null) {
            return null;
        }
        return DataRequestDto.Builder.newInstance()
                .assetId(object.getAssetId())
                .contractId(object.getContractId())
                .connectorId(object.getConnectorId())
                .build();
    }
}
