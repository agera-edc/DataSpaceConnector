package org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.transform;

import com.github.javafaker.Faker;
import org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.model.DataRequestDto;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DataRequestDtoToDataRequestTransformerTest {
    DataRequestTransformerTestData data = new DataRequestTransformerTestData();

    DataRequestDtoToDataRequestTransformer transformer = new DataRequestDtoToDataRequestTransformer();

    @Test
    void getInputType() {
        assertThat(transformer.getInputType()).isEqualTo(DataRequestDto.class);
    }

    @Test
    void getOutputType() {
        assertThat(transformer.getOutputType()).isEqualTo(DataRequest.class);
    }

    @Test
    void transform() {
        var result = transformer.transform(data.dto, data.context);
        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(data.entity);
    }
}