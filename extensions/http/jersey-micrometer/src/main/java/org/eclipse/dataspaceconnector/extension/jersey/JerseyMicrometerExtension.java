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

package org.eclipse.dataspaceconnector.extension.jersey;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.jersey.server.DefaultJerseyTagsProvider;
import io.micrometer.core.instrument.binder.jersey.server.MetricsApplicationEventListener;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.system.Requires;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;

/**
 * An extension that register a Micrometer {@link MetricsApplicationEventListener} into Jersey to
 * provide metrics and request timings.
 */
@Requires({WebService.class})
public class JerseyMicrometerExtension implements ServiceExtension {

    @EdcSetting
    public static final String ENABLE_METRICS = "metrics.enabled";
    @EdcSetting
    public static final String ENABLE_JERSEY_METRICS = "metrics.jersey.enabled";

    @Override
    public String name() {
        return "Jersey Micrometer Metrics";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var enableMetrics = Boolean.valueOf(context.getSetting(ENABLE_METRICS, "true"));
        var enableJerseyMetrics = Boolean.valueOf(context.getSetting(ENABLE_JERSEY_METRICS, "true"));

        if (enableMetrics && enableJerseyMetrics) {
            enableJerseyControllerMetrics(context);
        }
    }

    private void enableJerseyControllerMetrics(ServiceExtensionContext context) {
        TypeManager typeManager = context.getTypeManager();

        var webService = context.getService(WebService.class);

        webService.registerResource(new MetricsApplicationEventListener(
                Metrics.globalRegistry,
                new DefaultJerseyTagsProvider(),
                /* metricName = */ "jersey",
                /* autoTimeRequests = */ true));
    }
}
