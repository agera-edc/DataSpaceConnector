package org.eclipse.dataspaceconnector.tests;

import io.gatling.app.Gatling;
import io.gatling.core.config.GatlingPropertiesBuilder;
import io.gatling.javaapi.core.Simulation;

import java.util.Iterator;
import java.util.function.Supplier;

public class GatlingUtils {

    public static void runGatling(Class<? extends Simulation> simulation) {
        var props = new GatlingPropertiesBuilder();
        props.simulationClass(simulation.getCanonicalName());
        props.resultsDirectory("build/gatling");
        Gatling.fromMap(props.build());
    }

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
