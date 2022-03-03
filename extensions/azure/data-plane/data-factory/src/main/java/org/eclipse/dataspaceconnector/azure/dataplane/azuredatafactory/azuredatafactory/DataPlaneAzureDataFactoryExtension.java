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
package org.eclipse.dataspaceconnector.azure.dataplane.azuredatafactory.azuredatafactory;

import org.eclipse.dataspaceconnector.azure.dataplane.azuredatafactory.pipeline.AzureDataFactoryTransferService;
import org.eclipse.dataspaceconnector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

/**
 * Provides support for reading data from an Azure Storage Blob endpoint and sending data to an Azure Storage Blob endpoint.
 */
public class DataPlaneAzureDataFactoryExtension implements ServiceExtension {

    @Inject
    private DataPlaneManager dataPlaneManager;

    @Override
    public String name() {
        return "Data Plane Azure Data Factory";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();

        var transferService = new AzureDataFactoryTransferService(monitor);
        dataPlaneManager.registerTransferService(transferService);
    }
}
