/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.core.defaults.transferprocessstore;

import org.eclipse.dataspaceconnector.core.defaults.InMemoryEntityStore;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.jetbrains.annotations.Nullable;

/**
 * An in-memory, threadsafe process store.
 * This implementation is intended for testing purposes only.
 */
public class InMemoryTransferProcessStore extends InMemoryEntityStore<TransferProcess> implements TransferProcessStore {

    public InMemoryTransferProcessStore() {
        super(TransferProcess.class);
    }

    @Override
    @Nullable
    public String processIdForTransferId(String id) {
        return findAll()
                .filter(p -> id.equals(p.getDataRequest().getId()))
                .findFirst()
                .map(p -> p.getId())
                .orElse(null);
    }

    @Override
    public void create(TransferProcess process) {
        upsert(process);
    }

    @Override
    public void update(TransferProcess process) {
        upsert(process);
    }
}
