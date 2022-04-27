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

package org.eclipse.dataspaceconnector.azure.blob.core;

import com.github.javafaker.Faker;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;

public class AzureStorageTestFixtures {

    static Faker faker = new Faker();

    public static DataFlowRequest.Builder createRequest(String type) {
        return DataFlowRequest.Builder.newInstance()
                .id("1").processId("1")
                .sourceDataAddress(createDataAddress(type).build())
                .destinationDataAddress(createDataAddress(type).build());
    }

    public static DataAddress.Builder createDataAddress(String type) {
        return DataAddress.Builder.newInstance().type(type);
    }

    public static String createAccountName() {
        return faker.lorem().characters(3, 24, false, false);
    }

    public static String createContainerName() {
        return faker.lorem().characters(3, 40, false, false);
    }

    public static String createBlobName() {
        return faker.lorem().characters(3, 40, false, false);
    }

    public static String createSharedKey() {
        return faker.lorem().characters();
    }

    public static String createSharedAccessSignature() {
        return faker.lorem().characters();
    }

    private AzureStorageTestFixtures() {
    }

}
