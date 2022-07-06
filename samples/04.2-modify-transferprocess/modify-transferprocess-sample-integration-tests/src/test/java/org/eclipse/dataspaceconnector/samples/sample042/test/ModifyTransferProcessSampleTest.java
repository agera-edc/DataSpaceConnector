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
 *       Microsoft Corporation - initial test implementation for sample
 *
 */

package org.eclipse.dataspaceconnector.samples.sample042.test;

import org.eclipse.dataspaceconnector.common.util.junit.annotations.EndToEndTest;
import org.eclipse.dataspaceconnector.junit.extensions.EdcRuntimeExtension;
import org.eclipse.dataspaceconnector.junit.testfixtures.TestUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.File;
import java.util.Map;

@EndToEndTest
public class ModifyTransferProcessSampleTest {

    static final String CONSUMER_CONFIG_PROPERTIES_FILE_PATH = "samples/04.2-modify-transferprocess/consumer/config.properties";
    @RegisterExtension
    static EdcRuntimeExtension consumer = new EdcRuntimeExtension(
            ":samples:04.2-modify-transferprocess:consumer",
            "provider",
            Map.of(
                    "edc.fs.config", getFileFromRelativePath(CONSUMER_CONFIG_PROPERTIES_FILE_PATH).getAbsolutePath()
            )
    );

    /**
     * Resolves a {@link File} instance from a relative path.
     */
    @NotNull
    static File getFileFromRelativePath(String relativePath) {
        return new File(TestUtils.findBuildRoot(), relativePath);
    }

    /**
     * Run all sample steps in one single test.
     * Note: Sample steps cannot be separated into single tests because {@link EdcRuntimeExtension}
     * runs before each single test.
     */
    @Test
    void runSampleSteps() {

    }
}
