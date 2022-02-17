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

package org.eclipse.dataspaceconnector.core;

import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.okhttp3.OkHttpMetricsEventListener;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import okhttp3.EventListener;
import org.eclipse.dataspaceconnector.core.executor.ExecutorInstrumentationImplementation;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import static io.micrometer.core.instrument.Metrics.globalRegistry;

@BaseExtension
@Provides({EventListener.class, ExecutorInstrumentationImplementation.class})
public class MicrometerExtension implements ServiceExtension {

    @EdcSetting
    public static final String ENABLE_METRICS = "metrics.enabled";
    @EdcSetting
    public static final String ENABLE_SYSTEM_METRICS = "metrics.system.enabled";
    @EdcSetting
    public static final String ENABLE_OKHTTP_METRICS = "metrics.okhttp.enabled";
    @EdcSetting
    public static final String ENABLE_EXECUTOR_METRICS = "metrics.executor.enabled";

    @Override
    public String name() {
        return "Micrometer Metrics";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var enableMetrics = Boolean.valueOf(context.getSetting(ENABLE_METRICS, "true"));
        var enableSystemMetrics = Boolean.valueOf(context.getSetting(ENABLE_SYSTEM_METRICS, "true"));
        var enableOkHttpMetrics = Boolean.valueOf(context.getSetting(ENABLE_OKHTTP_METRICS, "true"));
        var enableExecutorMetrics = Boolean.valueOf(context.getSetting(ENABLE_EXECUTOR_METRICS, "true"));

        if (enableMetrics && enableSystemMetrics) {
            enableSystemMetrics();
        }

        if (enableMetrics && enableOkHttpMetrics) {
            enableOkHttpMetrics(context);
        }

        if (enableMetrics && enableExecutorMetrics) {
            enableExecutorMetrics(context);
        }
    }

    private void enableSystemMetrics() {
        new ClassLoaderMetrics().bindTo(globalRegistry);
        new JvmMemoryMetrics().bindTo(globalRegistry);
        new JvmGcMetrics().bindTo(globalRegistry);
        new ProcessorMetrics().bindTo(globalRegistry);
        new JvmThreadMetrics().bindTo(globalRegistry);
    }

    private void enableOkHttpMetrics(ServiceExtensionContext context) {
        var listener = OkHttpMetricsEventListener.builder(globalRegistry, "okhttp.requests").build();
        context.registerService(EventListener.class, listener);
    }

    private void enableExecutorMetrics(ServiceExtensionContext context) {
        context.registerService(ExecutorInstrumentationImplementation.class, new MicrometerExecutorInstrumentation());
    }
}
