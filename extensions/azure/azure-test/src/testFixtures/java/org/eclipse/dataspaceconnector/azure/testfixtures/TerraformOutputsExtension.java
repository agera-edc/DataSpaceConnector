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

package org.eclipse.dataspaceconnector.azure.testfixtures;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.common.testfixtures.TestUtils;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.File;
import java.util.Map;
import java.util.Properties;

/**
 * A JUnit extension for setting system properties from a Terraform outputs JSON file.
 * Used by cloud integration tests.
 */
public class TerraformOutputsExtension implements BeforeAllCallback, AfterAllCallback {
    private Properties savedProperties;

    private static final String OUTPUTS_FILE = "resources/azure/testing/terraform_outputs.json";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {

        var root = TestUtils.findBuildRoot();
        var file = new File(root, OUTPUTS_FILE);
        if (!file.exists())
        {
            throw new EdcException("Could not locate " + OUTPUTS_FILE + ". Refer to developer docs to download this file.");
        }

        savedProperties = (Properties) System.getProperties().clone();
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> v = OBJECT_MAPPER.readValue(file, Map.class);
        for (var entry : v.entrySet()) {
            System.setProperty(entry.getKey(), entry.getValue().get("value").toString());
        }
    }

    @Override
    public void afterAll(ExtensionContext context) {
        System.setProperties(savedProperties);
    }
}
