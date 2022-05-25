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
 *       Microsoft Corporation - initial implementation
 *
 */

package org.eclipse.dataspaceconnector.test.didauth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.ECKey;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.dataspaceconnector.iam.did.spi.document.DidConstants;
import org.eclipse.dataspaceconnector.iam.did.spi.document.DidDocument;
import org.eclipse.dataspaceconnector.iam.did.spi.document.EllipticCurvePublicKey;
import org.eclipse.dataspaceconnector.iam.did.spi.document.Service;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.system.Provider;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

public class DidDocumentInterceptorExtension implements ServiceExtension {

    private String did;

    @Override
    public void initialize(ServiceExtensionContext context) {
        try {
            var keypair = Files.readString(Path.of("ec-keypair.pem"));
            var eckey = (ECKey) ECKey.parseFromPEMEncodedObjects(keypair);

            var publicKey = new EllipticCurvePublicKey(eckey.getCurve().getName(), eckey.getKeyType().getValue(), eckey.getX().toString(), eckey.getY().toString());
            var didDocument = DidDocument.Builder.newInstance()
                    .id("did:web:consumer")
                    .verificationMethod("#my-key1", DidConstants.ECDSA_SECP_256_K_1_VERIFICATION_KEY_2019, publicKey)
                    .service(Collections.singletonList(new Service("#my-service1", DidConstants.HUB_URL, "http://doesnotexi.st")))
                    .build();
            did = new TypeManager().writeValueAsString(didDocument);
        } catch (JOSEException | IOException e) {
            throw new EdcException(e);
        }
    }

    @Provider
    public Interceptor interceptor(ServiceExtensionContext context) {
        return chain -> {
            if (!chain.request().url().equals(HttpUrl.parse("https://consumer/.well-known/did.json"))) {
                return chain.proceed(chain.request());
            }

            return new Response.Builder()
                    .body(ResponseBody.create(did, MediaType.get("application/json")))
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .build();
        };
    }
}
