package org.eclipse.dataspaceconnector.spi.telemetry;

import java.util.Map;

/**
 * Simple TraceCarrier to use in situations where no entity is persisted for asynchronous processing
 */
class InMemoryTraceCarrier implements TraceCarrier {

    private final Map<String, String> traceContext;

    public InMemoryTraceCarrier(Map<String, String> traceContext) {
        this.traceContext = traceContext;
    }

    @Override
    public Map<String, String> getTraceContext() {
        return traceContext;
    }
}
