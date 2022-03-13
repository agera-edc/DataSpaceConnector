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
package org.eclipse.dataspaceconnector.azure.dataplane.azuredatafactory;

import com.azure.resourcemanager.datafactory.DataFactoryManager;
import com.azure.resourcemanager.datafactory.models.CreateRunResponse;
import com.azure.resourcemanager.datafactory.models.DatasetResource;
import com.azure.resourcemanager.datafactory.models.LinkedServiceResource;
import com.azure.resourcemanager.datafactory.models.PipelineResource;
import com.azure.resourcemanager.datafactory.models.PipelineRun;

public class DataFactoryClient {
    private final DataFactoryManager dataFactoryManager;
    private final String resourceGroupName;
    private final String factoryName;

    public DataFactoryClient(DataFactoryManager dataFactoryManager, String resourceGroupName, String factoryName) {
        this.dataFactoryManager = dataFactoryManager;
        this.resourceGroupName = resourceGroupName;
        this.factoryName = factoryName;
    }

    LinkedServiceResource.DefinitionStages.WithProperties defineLinkedService(String name) {
        return dataFactoryManager
                .linkedServices()
                .define(name)
                .withExistingFactory(resourceGroupName, factoryName);
    }

    PipelineResource.DefinitionStages.WithCreate definePipeline(String baseName) {
        return dataFactoryManager.pipelines()
                .define(baseName)
                .withExistingFactory(resourceGroupName, factoryName);
    }

    DatasetResource.DefinitionStages.WithProperties defineDataset(String name) {
        return dataFactoryManager
                .datasets()
                .define(name)
                .withExistingFactory(resourceGroupName, factoryName);
    }

    CreateRunResponse runPipeline(PipelineResource pipeline) {
        return dataFactoryManager.pipelines()
                .createRun(resourceGroupName, factoryName, pipeline.name());
    }

    PipelineRun getPipelineRun(String runId) {
        return dataFactoryManager
                .pipelineRuns()
                .get(resourceGroupName, factoryName, runId);
    }

    void cancelPipelineRun(String runId) {
        dataFactoryManager
                .pipelineRuns()
                .cancel(resourceGroupName, factoryName, runId);
    }
}
