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

package org.eclipse.dataspaceconnector.micrometer;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import org.eclipse.dataspaceconnector.core.executor.ExecutorInstrumentationImplementation;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * {@link ExecutorInstrumentationImplementation} that decorates executors using wrappers
 * provided by Micrometer {@link ExecutorServiceMetrics} to report metrics such as thread pool
 * size and execution timings.
 */
public class MicrometerExecutorInstrumentation implements ExecutorInstrumentationImplementation {
    @Override
    public ScheduledExecutorService instrument(ScheduledExecutorService target, String name) {
        return ExecutorServiceMetrics.monitor(Metrics.globalRegistry, target, name);
    }

    @Override
    public ExecutorService instrument(ExecutorService target, String name) {
        return ExecutorServiceMetrics.monitor(Metrics.globalRegistry, target, name);
    }
}
