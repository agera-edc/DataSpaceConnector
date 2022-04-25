/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.system.tests.local;

<<<<<<<< HEAD:extensions/data-plane-transfer/build.gradle.kts

dependencies {
    api(project(":extensions:data-plane-transfer:data-plane-transfer-sync"))
    api(project(":extensions:data-plane-transfer:data-plane-transfer-client"))
}

publishing {
    publications {
        create<MavenPublication>("data-plane-transfer") {
            artifactId = "data-plane-transfer"
            from(components["java"])
        }
========

public class BlobTransferLocalSimulation extends TransferLocalSimulation {
    static final String ACCOUNT_NAME_PROPERTY = "BlobTransferLocalSimulation-account-name";

    public BlobTransferLocalSimulation() {
        super(new BlobTransferRequestFactory(System.getProperty(ACCOUNT_NAME_PROPERTY)));
>>>>>>>> ee1826861 (merging changes from feature/1183/231-azure-e2e-test):system-tests/azure-tests/src/test/java/org/eclipse/dataspaceconnector/system/tests/local/BlobTransferLocalSimulation.java
    }
}
