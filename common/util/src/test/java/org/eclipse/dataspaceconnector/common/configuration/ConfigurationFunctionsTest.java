/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.common.configuration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigurationFunctionsTest {

    private static final int EPHEMERAL_PORT_MIN = 1024;
    private static final int EPHEMERAL_PORT_MAX = 65535;

    @Test
    void findsRandomPort() {
        var port1 = ConfigurationFunctions.findUnallocatedServerPort();
        var port2 = ConfigurationFunctions.findUnallocatedServerPort();

        assertThat(port1).isBetween(EPHEMERAL_PORT_MIN, EPHEMERAL_PORT_MAX);
        assertThat(port2).isBetween(EPHEMERAL_PORT_MIN, EPHEMERAL_PORT_MAX);
        assertThat(port2).isNotEqualTo(port1);
    }
}
