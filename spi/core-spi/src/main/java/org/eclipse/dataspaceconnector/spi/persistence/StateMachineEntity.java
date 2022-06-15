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

package org.eclipse.dataspaceconnector.spi.persistence;


import org.eclipse.dataspaceconnector.spi.telemetry.TraceCarrier;

import java.time.Clock;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public abstract class StateMachineEntity<T> implements TraceCarrier {

    protected String id;
    protected long createdTimestamp;
    protected int state;
    protected int stateCount;
    protected long stateTimestamp;
    protected Map<String, String> traceContext = new HashMap<>();
    protected String errorDetail;
    protected Clock clock = Clock.systemUTC();

    protected StateMachineEntity() {
    }

    public long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public int getState() {
        return state;
    }

    public int getStateCount() {
        return stateCount;
    }

    public long getStateTimestamp() {
        return stateTimestamp;
    }

    @Override
    public Map<String, String> getTraceContext() {
        return Collections.unmodifiableMap(traceContext);
    }

    public String getErrorDetail() {
        return errorDetail;
    }

    public void setErrorDetail(String errorDetail) {
        this.errorDetail = errorDetail;
    }

    /**
     * Sets the state timestamp to the clock time.
     *
     * @see Builder#clock(Clock)
     */
    public void updateStateTimestamp() {
        stateTimestamp = clock.millis();
    }

    protected void transitionTo(int targetState) {
        stateCount = state == targetState ? stateCount + 1 : 1;
        state = targetState;
        updateStateTimestamp();
    }

    public String getId() {
        return id;
    }

    public abstract T copy();

    protected abstract static class Builder<T extends StateMachineEntity<T>, B extends Builder<T, B>> {

        public abstract B self();

        protected final T target;

        protected Builder(T target) {
            this.target = target;
        }

        public B id(String id) {
            target.id = id;
            return self();
        }


        public B clock(Clock clock) {
            target.clock = clock;
            return self();
        }

        public B createdTimestamp(long value) {
            target.createdTimestamp = value;
            return self();
        }

        public B state(int value) {
            target.state = value;
            return self();
        }

        public B stateCount(int value) {
            target.stateCount = value;
            return self();
        }

        public B stateTimestamp(long value) {
            target.stateTimestamp = value;
            return self();
        }

        public B errorDetail(String errorDetail) {
            target.errorDetail = errorDetail;
            return self();
        }

        public B traceContext(Map<String, String> traceContext) {
            target.traceContext = traceContext;
            return self();
        }

        public T build() {
            Objects.requireNonNull(target.id, "id");
            Objects.requireNonNull(target.clock, "clock");
            if (target.stateTimestamp == 0) {
                target.stateTimestamp = target.clock.millis();
            }
            return target;
        }
    }
}
