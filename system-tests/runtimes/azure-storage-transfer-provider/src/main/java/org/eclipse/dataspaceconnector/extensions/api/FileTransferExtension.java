/*
 *  Copyright (c) 2021 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.extensions.api;

import org.eclipse.dataspaceconnector.azure.blob.AzureBlobStoreSchema;
import org.eclipse.dataspaceconnector.dataloading.AssetLoader;
import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyStore;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;

import java.util.Objects;

public class FileTransferExtension implements ServiceExtension {

    public static final String USE_POLICY = "use-eu";
    private static final String CONTAINER_NAME = "edc.test.asset.container.name";

    @Inject
    private ContractDefinitionStore contractStore;
    @Inject
    private AssetLoader loader;
    @Inject
    private PolicyStore policyStore;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();

        var policy = createPolicy();
        policyStore.save(policy);

        registerDataEntries(context);
        registerContractDefinition(policy.getUid());

        context.getMonitor().info("File Transfer Extension initialized!");
    }

    private Policy createPolicy() {

        var usePermission = Permission.Builder.newInstance()
                .action(Action.Builder.newInstance().type("idsc:USE").build())
                .build();

        return Policy.Builder.newInstance()
                .id(USE_POLICY)
                .permission(usePermission)
                .target("test-document")
                .build();
    }

    private void registerDataEntries(ServiceExtensionContext context) {
        var containerName = Objects.requireNonNull(context.getSetting(CONTAINER_NAME, null));

        var dataAddress = DataAddress.Builder.newInstance()
                .type(AzureBlobStoreSchema.TYPE)
                .keyName("account1-key1") // FIXME: required?
                .property(AzureBlobStoreSchema.ACCOUNT_NAME, "account1")
                .property(AzureBlobStoreSchema.CONTAINER_NAME, containerName)
                .property(AzureBlobStoreSchema.BLOB_NAME, "test-document")
                .build();

        var assetId = "test-document";
        var asset = Asset.Builder.newInstance().id(assetId).build();

        loader.accept(asset, dataAddress);
    }

    private void registerContractDefinition(String policyId) {

        var contractDefinition = ContractDefinition.Builder.newInstance()
                .id("1")
                .accessPolicyId(policyId)
                .contractPolicyId(policyId)
                .selectorExpression(AssetSelectorExpression.Builder.newInstance().whenEquals(Asset.PROPERTY_ID, "test-document").build())
                .build();

        contractStore.save(contractDefinition);
    }
}
