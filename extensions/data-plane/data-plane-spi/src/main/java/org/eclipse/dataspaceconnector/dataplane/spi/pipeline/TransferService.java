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

import org.eclipse.dataspaceconnector.dataplane.spi.result.TransferResult;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public interface TransferService {

    /**
     * A successful validation result.
     */
    Result<Boolean> VALID = Result.success(true);

    /**
     * Returns true if this factory can transfer the request.
     */
    boolean canHandle(DataFlowRequest request);

    /**
     * Returns true if the request is valid.
     */
    @NotNull Result<Boolean> validate(DataFlowRequest request);

    @NotNull CompletableFuture<TransferResult> transfer(DataFlowRequest request);
}
