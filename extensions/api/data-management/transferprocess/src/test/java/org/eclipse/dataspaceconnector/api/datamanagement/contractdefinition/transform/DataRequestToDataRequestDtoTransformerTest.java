package org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.transform;

import org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.model.DataRequestDto;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DataRequestToDataRequestDtoTransformerTest {
    DataRequestTransformerTestData data = new DataRequestTransformerTestData();

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
        var result = transformer.transform(data.entity, data.context);
        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(data.dto);
    }
}