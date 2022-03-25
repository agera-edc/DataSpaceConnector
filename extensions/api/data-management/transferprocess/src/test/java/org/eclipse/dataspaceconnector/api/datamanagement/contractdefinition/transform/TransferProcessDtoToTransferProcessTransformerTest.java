/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */
package org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.transform;

import com.github.javafaker.Faker;
import org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.model.TransferProcessDto;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class TransferProcessDtoToTransferProcessTransformerTest {
    static Faker faker = new Faker();

    TransferProcessTransformerTestData data = new TransferProcessTransformerTestData();
    String invalidValue = faker.lorem().word();

    TransferProcessDtoToTransferProcessTransformer transformer = new TransferProcessDtoToTransferProcessTransformer();
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
        data.dto.type(invalidValue);
        data.entity.type(null);
        problems.add("Invalid value for TransferProcess.type");

        assertThatDtoTransformsToEntity();
    }

    @ParameterizedTest
    @MethodSource("blankStrings")
    void transform_whenBlankType(String blankValue) {
        data.dto.type(blankValue);
        data.entity.type(null);

        assertThatDtoTransformsToEntity();
    }

    @Test
    void transform_whenInvalidState() {
        String invalidState = faker.lorem().word();
        data.dto.state(invalidState);
        data.entity.state(0);
        problems.add("Invalid value for TransferProcess.state");

        assertThatDtoTransformsToEntity();
    }

    @ParameterizedTest
    @MethodSource("blankStrings")
    void transform_whenBlankState(String blankValue) {
        data.dto.state(blankValue);
        data.entity.state(0);

        assertThatDtoTransformsToEntity();
    }

    void assertThatDtoTransformsToEntity() {
        when(data.registry.transform(data.dataRequestDto, DataRequest.class)).thenReturn(Result.success(data.dataRequest));

        var result = transformer.transform(data.dto.build(), data.context);

        assertThat(result)
                .usingRecursiveComparison()
                .ignoringFields("stateTimestamp")
                .isEqualTo(data.entity.build());

        assertThat(data.context.getProblems()).containsExactlyElementsOf(problems);
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