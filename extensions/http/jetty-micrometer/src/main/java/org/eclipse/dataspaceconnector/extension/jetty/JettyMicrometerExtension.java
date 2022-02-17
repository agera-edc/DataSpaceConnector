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

package org.eclipse.dataspaceconnector.extension.jetty;

import io.micrometer.core.instrument.binder.jersey.server.MetricsApplicationEventListener;
import io.micrometer.core.instrument.binder.jetty.JettyConnectionMetrics;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.system.Requires;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

/**
 * An extension that registers Micrometer {@link JettyConnectionMetrics} into Jetty to
 * provide server metrics.
 */
@Requires({ JettyService.class })
public class JettyMicrometerExtension implements ServiceExtension {

    @EdcSetting
    public static final String ENABLE_METRICS = "metrics.enabled";
    @EdcSetting
    public static final String ENABLE_JETTY_METRICS = "metrics.jetty.enabled";

    @Override
    public String name() {
        return "Jetty Micrometer Metrics";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var enableMetrics = Boolean.valueOf(context.getSetting(ENABLE_METRICS, "true"));
        var enableJettyMetrics = Boolean.valueOf(context.getSetting(ENABLE_JETTY_METRICS, "true"));

        if (enableMetrics && enableJettyMetrics) {
            enableJettyConnectorMetrics(context);
        }
    }

    private void enableJettyConnectorMetrics(ServiceExtensionContext context) {
        var jettyService = context.getService(JettyService.class);
        jettyService.addConnectorConfigurationCallback(new JettyMicrometerConfiguration());
    }
}
