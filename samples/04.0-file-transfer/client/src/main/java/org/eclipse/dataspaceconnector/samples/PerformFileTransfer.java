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

package org.eclipse.dataspaceconnector.samples;

/**
 * Client application for performing a file transfer
 */
public class PerformFileTransfer {

    public static void main(String[] args) {
        if (args.length != 4) {
            System.err.println("Requires 4 arguments: consumerUrl providerUrl destinationPath apiKey");
            System.exit(1);
        }
        var consumerUrl = args[0];
        var providerUrl = args[1];
        var destinationPath = args[2];
        var apiKey = args[3];

        var client = new FileTransferClient();
        client.setConsumerUrl(consumerUrl);
        client.setProviderUrl(providerUrl);
        client.setDestinationPath(destinationPath);
        client.setApiKey(apiKey);

        var contractAgreementId = client.negotiateContractAgreement();
        client.performFileTransfer(contractAgreementId);
    }
}
