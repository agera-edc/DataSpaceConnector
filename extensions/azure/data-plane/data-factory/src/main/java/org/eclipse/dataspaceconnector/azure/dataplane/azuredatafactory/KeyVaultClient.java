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

import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;

public class KeyVaultClient {
    private final SecretClient secretClient;

    public KeyVaultClient(SecretClient secretClient) {
        this.secretClient = secretClient;
    }

    KeyVaultSecret setSecret(String name, String accountKey) {
        return secretClient.setSecret(name, accountKey);
    }
}