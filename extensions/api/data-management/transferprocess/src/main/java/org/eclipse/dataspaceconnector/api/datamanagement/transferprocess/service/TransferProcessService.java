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
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;

import java.util.Collection;

public interface TransferProcessService {

    /**
     * Returns an transferProcess by its id
     *
     * @param transferProcessId id of the transferProcess
     * @return the transferProcess, null if it's not found
     */
    TransferProcess findById(String transferProcessId);


    /**
     * Query transferProcesss
     *
     * @param query request
     * @return the collection of transferProcesss that match the query
     */
    Collection<TransferProcess> query(QuerySpec query);

    String getState(String transferProcessId);

    Result cancel(String transferProcessId);

    Result deprovision(String transferProcessId);
}
