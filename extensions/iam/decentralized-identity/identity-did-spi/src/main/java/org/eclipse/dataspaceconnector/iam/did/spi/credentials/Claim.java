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

package org.eclipse.dataspaceconnector.iam.did.spi.credentials;

/**
 * A <a href="https://www.w3.org/TR/vc-data-model/#claims">claim</a> is a statement about a subject.
 * For example, ParticipantA is in region eu, is a claim about participantA.
 */
public class Claim {

    String subjectDid;

    // A Property name, for example "region".
    String property;
    // Property value, for example "eu".
    String value;
    // DID of the entity that issued the claim
    String issuerDid;

    public Claim(String subjectDid, String property, String value, String issuerDid) {
        this.subjectDid = subjectDid;
        this.property = property;
        this.value = value;
        this.issuerDid = issuerDid;
    }
}
