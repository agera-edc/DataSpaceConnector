/*
 *  Copyright (c) 2021 Microsoft Corporation
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
package org.eclipse.dataspaceconnector.dataplane.spi.pipeline;

import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;

/**
 * Creates {@link DataFlowRequest}s
 */
public interface DataSinkFactory {

    /**
     * Returns true if this factory can create a {@link DataSink} for the request.
     */
    boolean canHandle(DataFlowRequest request);

    /**
     * Creates a sink to send data to.
     */
    DataSink createSink(DataFlowRequest request);

}
