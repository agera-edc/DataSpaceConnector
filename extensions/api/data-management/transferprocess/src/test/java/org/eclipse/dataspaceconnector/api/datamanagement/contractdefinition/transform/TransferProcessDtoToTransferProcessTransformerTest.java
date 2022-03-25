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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TransferProcessDtoToTransferProcessTransformerTest {
    static Faker faker = new Faker();

    DtoTransformerRegistry registry = mock(DtoTransformerRegistry.class);
    TransformerContext context = new DtoTransformerRegistryImpl.DtoTransformerContext(registry);
    String id = faker.lorem().word();
    TransferProcess.Type type = faker.options().option(TransferProcess.Type.class);
    TransferProcessStates state = faker.options().option(TransferProcessStates.class);
    String errorDetail = faker.lorem().word();
    String invalidValue = faker.lorem().word();

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

    DtoTransformer<DataRequestDto, DataRequest> dataRequestTransformer = mock(DataRequestDtoToDataRequestTransformer.class);

    TransferProcessDtoToTransferProcessTransformer transformer = new TransferProcessDtoToTransferProcessTransformer(dataRequestTransformer);
    List<String> problems = new ArrayList<>();

    @Test
    void getInputType() {
        assertThat(transformer.getInputType()).isEqualTo(TransferProcessDto.class);
    }

    @Test
    void getOutputType() {
        assertThat(transformer.getOutputType()).isEqualTo(TransferProcess.class);
    }

    @Test
    void transform() {
        assertThatDtoTransformsToEntity();
    }

    @Test
    void transform_whenInvalidType() {
        dto.type(invalidValue);
        entity.type(null);
        problems.add("Invalid value for TransferProcess.type");

        assertThatDtoTransformsToEntity();
    }

    @ParameterizedTest
    @MethodSource("blankStrings")
    void transform_whenBlankType(String blankValue) {
        dto.type(blankValue);
        entity.type(null);

        assertThatDtoTransformsToEntity();
    }

    @Test
    void transform_whenInvalidState() {
        String invalidState = faker.lorem().word();
        dto.state(invalidState);
        entity.state(0);
        problems.add("Invalid value for TransferProcess.state");

        assertThatDtoTransformsToEntity();
    }

    @ParameterizedTest
    @MethodSource("blankStrings")
    void transform_whenBlankState(String blankValue) {
        dto.state(blankValue);
        entity.state(0);

        assertThatDtoTransformsToEntity();
    }

    void assertThatDtoTransformsToEntity() {
        when(dataRequestTransformer.transform(dataRequestDto, context)).thenReturn(dataRequest);

        var result = transformer.transform(dto.build(), context);

        assertThat(result)
                .usingRecursiveComparison()
                .ignoringFields("stateTimestamp")
                .isEqualTo(entity.build());

        assertThat(context.getProblems()).containsExactlyElementsOf(problems);
    }

    static Stream<String> blankStrings() {
        return Stream.of(
                faker.regexify("[ \t\n\r]+"),
                "",
                " ",
                null
        );
    }
}