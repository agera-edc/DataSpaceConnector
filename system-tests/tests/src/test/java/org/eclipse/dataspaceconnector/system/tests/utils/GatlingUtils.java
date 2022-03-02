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

package org.eclipse.dataspaceconnector.system.tests.utils;

import io.gatling.app.Gatling;
import io.gatling.core.config.GatlingPropertiesBuilder;
import io.gatling.javaapi.core.Simulation;

import java.util.Iterator;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Utilities for Gatling tests.
 */
public class GatlingUtils {

    /**
     * Runs a Gatling simulation.
     *
     * @param simulation  Gatling simulation class. Must have a public no-args constructor.
     * @param description Description to be included in HTML report banner.
     * @throws AssertionError if Gatling assertions fails.
     */
    public static void runGatling(Class<? extends Simulation> simulation, String description) {
        var props = new GatlingPropertiesBuilder();
        props.simulationClass(simulation.getCanonicalName());
        props.resultsDirectory("build/reports/gatling");
        props.runDescription(description);

        var statusCode = Gatling.fromMap(props.build());

        assertThat(statusCode)
                .withFailMessage("Gatling Simulation failed")
                .isEqualTo(0);
    }

    /**
     * Returns an iterator that runs forever, getting each value by calling a {@see Supplier}.
     *
     * @param supplier source of iterator values.
     * @param <T>      iterator value type.
     * @return an unbounded iterator.
     */
    public static <T> Iterator<T> endlesslyWith(Supplier<T> supplier) {
        return new EndlessIterator<>(supplier);
    }

    private static class EndlessIterator<T> implements Iterator<T> {
        private final Supplier<T> supplier;

        private EndlessIterator(Supplier<T> supplier) {
            this.supplier = supplier;
        }

        @Override
        public boolean hasNext() {
            return true;
        }

        @Override
        public T next() {
            return supplier.get();
        }
    }
}
