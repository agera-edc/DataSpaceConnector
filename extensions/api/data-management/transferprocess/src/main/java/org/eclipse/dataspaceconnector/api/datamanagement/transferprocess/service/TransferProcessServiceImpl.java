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
import org.eclipse.dataspaceconnector.transfer.core.command.commands.CancelTransferCommand;
import org.eclipse.dataspaceconnector.transfer.core.command.commands.DeprovisionRequest;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

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
    public TransferProcess findById(String transferProcessId) {
        var result = new AtomicReference<TransferProcess>();

        transactionContext.execute(() -> result.set(findbyIdImpl(transferProcessId)));

        return result.get();
    }

    private TransferProcess findbyIdImpl(String transferProcessId) {
        return transferProcessStore.find(transferProcessId);
    }

    @Override
    public Collection<TransferProcess> query(QuerySpec query) {
        var result = new AtomicReference<Collection<TransferProcess>>();
        transactionContext.execute(() -> result.set(queryImpl(query)));
        return result.get();
    }

    private Collection<TransferProcess> queryImpl(QuerySpec query) {
        return transferProcessStore.findAll(query).collect(toList());
    }

    @Override
    public String getState(String transferProcessId) {
        var result = new AtomicReference<String>();
        transactionContext.execute(() -> result.set(getStateImpl(transferProcessId)));
        return result.get();
    }

    private String getStateImpl(String transferProcessId) {
        var process = transferProcessStore.find(transferProcessId);
        if (process == null) {
            return null;
        }
        return getStateName(process);
    }

    @Override
    public Result<?> cancel(String transferProcessId) {
        var result = new AtomicReference<Result<?>>();

        transactionContext.execute(() -> result.set(cancelImpl(transferProcessId)));

        return result.get();
    }

    private Result<?> cancelImpl(String transferProcessId) {
        var transferProcess = transferProcessStore.find(transferProcessId);

        if (transferProcess == null) {
            return Result.failure("Not found " + transferProcessId);
        }

        try {
            transferProcess.transitionCancelled();
        } catch (IllegalStateException e) {
            return Result.failure("Cannot cancel a transfer process in state " + getStateName(transferProcess));
        }

        manager.enqueueCommand(new CancelTransferCommand(transferProcessId));

        return Result.success();
    }

    @Override
    public Result<?> deprovision(String transferProcessId) {
        var result = new AtomicReference<Result<?>>();

        transactionContext.execute(() -> result.set(deprovisionImpl(transferProcessId)));

        return result.get();
    }

    private Result<?> deprovisionImpl(String transferProcessId) {
        var transferProcess = transferProcessStore.find(transferProcessId);

        if (transferProcess == null) {
            return Result.failure("Not found " + transferProcessId);
        }

        try {
            transferProcess.transitionDeprovisioning();
        } catch (IllegalStateException e) {
            return Result.failure("Cannot deprovision a transfer process in state " + getStateName(transferProcess));
        }

        manager.enqueueCommand(new DeprovisionRequest(transferProcessId));

        return Result.success();
    }

    @NotNull
    private String getStateName(TransferProcess process) {
        return TransferProcessStates.from(process.getState()).name();
    }
}
