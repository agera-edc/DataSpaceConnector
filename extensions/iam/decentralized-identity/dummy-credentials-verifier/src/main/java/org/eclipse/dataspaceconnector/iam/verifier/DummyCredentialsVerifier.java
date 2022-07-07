/*
 *  Copyright (c) 2022 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.iam.verifier;

import org.eclipse.dataspaceconnector.spi.iam.Claim;
import org.eclipse.dataspaceconnector.iam.did.spi.credentials.CredentialsVerifier;
import org.eclipse.dataspaceconnector.iam.did.spi.document.DidDocument;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;

import java.util.Collection;
import java.util.List;

/**
 * Implements a sample credentials validator that checks for signed registration credentials.
 */
public class DummyCredentialsVerifier implements CredentialsVerifier {
    private final Monitor monitor;

    /**
     * Create a new credentials verifier that uses an Identity Hub
     *
     * @param monitor a {@link Monitor}
     */
    public DummyCredentialsVerifier(Monitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public Result<Collection<Claim>> getVerifiedClaims(DidDocument participantDid) {
        monitor.debug("Starting (dummy) claims verification from participant:" + participantDid);
        return Result.success(List.of(new Claim(participantDid.toString(), "region", "eu", "dummyIssuerDid")));
    }
}
