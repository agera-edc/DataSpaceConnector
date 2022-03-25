package org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.transform;

import com.github.javafaker.Faker;
import org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.model.DataRequestDto;
import org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.model.TransferProcessDto;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformer;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformerRegistry;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformerRegistryImpl;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TransferProcessToTransferProcessDtoTransformerTest {
    static Faker faker = new Faker();

    DtoTransformerRegistry registry = mock(DtoTransformerRegistry.class);
    TransformerContext context = new DtoTransformerRegistryImpl.DtoTransformerContext(registry);
    String id = faker.lorem().word();
    TransferProcess.Type type = faker.options().option(TransferProcess.Type.class);
    TransferProcessStates state = faker.options().option(TransferProcessStates.class);
    String errorDetail = faker.lorem().word();

    DataRequest dataRequest = DataRequest.Builder.newInstance()
            .dataDestination(DataAddress.Builder.newInstance().type(faker.lorem().word()).build())
            .build();
    DataRequestDto dataRequestDto = DataRequestDto.Builder.newInstance().build();

    TransferProcess.Builder entity = TransferProcess.Builder.newInstance()
            .id(id)
            .type(type)
            .state(state.code())
            .errorDetail(errorDetail)
            .dataRequest(dataRequest);

    TransferProcessDto.Builder dto = TransferProcessDto.Builder.newInstance()
            .id(id)
            .type(type.name())
            .state(state.name())
            .errorDetail(errorDetail)
            .dataRequest(dataRequestDto);

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
        when(dataRequestTransformer.transform(dataRequest, context)).thenReturn(dataRequestDto);

        var result = transformer.transform(entity.build(), context);

        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(dto.build());

        assertThat(context.getProblems()).containsExactlyElementsOf(problems);
    }
}