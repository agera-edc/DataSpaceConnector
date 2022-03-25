package org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.transform;

import org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.model.DataRequestDto;
import org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.model.TransferProcessDto;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformer;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class TransferProcessToTransferProcessDtoTransformer implements DtoTransformer<TransferProcess, TransferProcessDto> {

    private final DtoTransformer<DataRequest, DataRequestDto> dataRequestTransformer;

    public TransferProcessToTransferProcessDtoTransformer(DtoTransformer<DataRequest, DataRequestDto> dataRequestTransformer) {
        this.dataRequestTransformer = dataRequestTransformer;
    }

    @Override
    public Class<TransferProcess> getInputType() {
        return TransferProcess.class;
    }

    @Override
    public Class<TransferProcessDto> getOutputType() {
        return TransferProcessDto.class;
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
                .state(TransferProcessStates.from(object.getState()).name())
                .errorDetail(object.getErrorDetail())
                .dataRequest(dataRequestTransformer.transform(object.getDataRequest(), context))
                .build();
    }
}
