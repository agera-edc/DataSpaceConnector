package org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.transform;

import org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.model.DataRequestDto;
import org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.model.TransferProcessDto;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformer;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TransferProcessToTransferProcessDtoTransformerTest {
    TransferProcessTestData data = new TransferProcessTestData();

    DtoTransformer<DataRequest, DataRequestDto> dataRequestTransformer = mock(DataRequestToDataRequestDtoTransformer.class);

    TransferProcessToTransferProcessDtoTransformer transformer = new TransferProcessToTransferProcessDtoTransformer(dataRequestTransformer);
    List<String> problems = new ArrayList<>();

    @Test
    void getInputType() {
        assertThat(transformer.getInputType()).isEqualTo(TransferProcess.class);
    }

    @Test
    void getOutputType() {
        assertThat(transformer.getOutputType()).isEqualTo(TransferProcessDto.class);
    }

    @Test
    void transform() {
        assertThatEntityTransformsToDto();
    }

    void assertThatEntityTransformsToDto() {
        when(dataRequestTransformer.transform(data.dataRequest, data.context)).thenReturn(data.dataRequestDto);

        var result = transformer.transform(data.entity.build(), data.context);

        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(data.dto.build());

        assertThat(data.context.getProblems()).containsExactlyElementsOf(problems);
    }
}