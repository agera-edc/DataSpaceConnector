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

import org.eclipse.dataspaceconnector.common.testfixtures.TestUtils;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.File;
import java.util.Properties;

/**
 * A JUnit extension for setting configuration properties from a properties file.
 * Used by cloud integration tests.
 */
public class TerraformOutputsExtension implements BeforeAllCallback, AfterAllCallback {
    private Properties savedProperties;

    @Override
    public void beforeAll(ExtensionContext context) {
        savedProperties = (Properties) System.getProperties().clone();
        System.setProperty("edc.fs.config", new File(TestUtils.findBuildRoot(), "resources/azure/testing/runtime_settings.properties").getAbsolutePath());
    }

    @Override
    public void afterAll(ExtensionContext context) {
        System.setProperties(savedProperties);
    }
}
