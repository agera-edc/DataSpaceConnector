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
package org.eclipse.dataspaceconnector.api.datamanagement.transferprocess.service;

import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.command.CancelTransferCommand;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.command.DeprovisionRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

import static java.util.stream.Collectors.toList;

public class TransferProcessServiceImpl implements TransferProcessService {
    private final TransferProcessStore transferProcessStore;
    private final TransferProcessManager manager;
    private final TransactionContext transactionContext;

    public TransferProcessServiceImpl(TransferProcessStore transferProcessStore, TransferProcessManager manager, TransactionContext transactionContext) {
        this.transferProcessStore = transferProcessStore;
        this.manager = manager;
        this.transactionContext = transactionContext;
    }

    @Override
    public @Nullable TransferProcess findById(String transferProcessId) {
        return transactionContext.execute(() -> transferProcessStore.find(transferProcessId));
    }

    @Override
    public @NotNull Collection<TransferProcess> query(QuerySpec query) {
        return transactionContext.execute(() -> transferProcessStore.findAll(query).collect(toList()));
    }

    @Override
    public @Nullable String getState(String transferProcessId) {
        return transactionContext.execute(() -> {
            var process = transferProcessStore.find(transferProcessId);
            if (process == null) {
                return null;
            }
            return getStateName(process);
        });
    }

    @Override
    public @NotNull Result<?> cancel(String transferProcessId) {
        return transactionContext.execute(() -> {
            var transferProcess = transferProcessStore.find(transferProcessId);

            if (transferProcess == null) {
                return Result.failure("Not found " + transferProcessId);
            }

            // Attempt the transition only to verify that the transition is allowed.
            // The updated transfer process is not persisted at this point, and is discarded.
            try {
                transferProcess.transitionCancelled();
            } catch (IllegalStateException e) {
                return Result.failure("Cannot cancel a transfer process in state " + getStateName(transferProcess));
            }

            manager.enqueueCommand(new CancelTransferCommand(transferProcessId));

            return Result.success();
        });
    }

    @Override
    public @NotNull Result<?> deprovision(String transferProcessId) {
        return transactionContext.execute(() -> {
            var transferProcess = transferProcessStore.find(transferProcessId);

            if (transferProcess == null) {
                return Result.failure("Not found " + transferProcessId);
            }

            // Attempt the transition only to verify that the transition is allowed.
            // The updated transfer process is not persisted at this point, and is discarded.
            try {
                transferProcess.transitionDeprovisioning();
            } catch (IllegalStateException e) {
                return Result.failure("Cannot deprovision a transfer process in state " + getStateName(transferProcess));
            }

            manager.enqueueCommand(new DeprovisionRequest(transferProcessId));

            return Result.success();
        });
    }

    @NotNull
    private String getStateName(TransferProcess process) {
        return TransferProcessStates.from(process.getState()).name();
    }
}
