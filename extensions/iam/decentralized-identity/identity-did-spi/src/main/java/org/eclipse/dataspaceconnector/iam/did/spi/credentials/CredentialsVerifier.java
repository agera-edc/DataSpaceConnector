/*
 *  Copyright (c) 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.iam.did.spi.credentials;

import org.eclipse.dataspaceconnector.iam.did.spi.document.DidDocument;
import org.eclipse.dataspaceconnector.spi.iam.Claim;
import org.eclipse.dataspaceconnector.spi.result.Result;

import java.util.Collection;

/**
 * Obtains and verifies claims associated with a DID according to an implementation-specific trust model.
 */
@FunctionalInterface
public interface CredentialsVerifier {

    /**
     * Get and verifies claims of a participant.
     *
     * @param participantDid DID Document of the participant
     */
    Result<Collection<Claim>> getVerifiedClaims(DidDocument participantDid);

}
