package org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.transform;

import com.github.javafaker.Faker;
import org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.model.DataRequestDto;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DataRequestToDataRequestDtoTransformerTest {
    static Faker faker = new Faker();

    TransformerContext context = mock(TransformerContext.class);
    String assetId = faker.lorem().word();
    String contractId = faker.lorem().word();
    String connectorId = faker.lorem().word();

    DataRequest entity = DataRequest.Builder.newInstance()
            .assetId(assetId)
            .contractId(contractId)
            .connectorId(connectorId)
            .dataDestination(DataAddress.Builder.newInstance().type(faker.lorem().word()).build())
            .build();

    DataRequestDto dto = DataRequestDto.Builder.newInstance()
            .assetId(assetId)
            .contractId(contractId)
            .connectorId(connectorId)
            .build();

    DataRequestToDataRequestDtoTransformer transformer = new DataRequestToDataRequestDtoTransformer();

    @Test
    void getInputType() {
        assertThat(transformer.getInputType()).isEqualTo(DataRequest.class);
    }

    @Test
    void getOutputType() {
        assertThat(transformer.getOutputType()).isEqualTo(DataRequestDto.class);
    }

    @Test
    void transform() {
        var result = transformer.transform(entity, context);
        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(dto);
    }
}