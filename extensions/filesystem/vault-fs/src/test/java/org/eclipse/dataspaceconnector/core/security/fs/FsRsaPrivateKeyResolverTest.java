/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering - Improvements
 *
 */

package org.eclipse.dataspaceconnector.core.security.fs;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.ECKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.eclipse.dataspaceconnector.iam.did.crypto.key.EcPrivateKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.spi.key.PrivateKeyWrapper;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.security.KeyParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.security.KeyStore;
import java.security.Security;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.RSAPrivateKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FsRsaPrivateKeyResolverTest {
    private static final String PASSWORD = "test123";
    private static final String TEST_KEYSTORE = "edc-test-keystore.jks";

    private FsPrivateKeyResolver keyResolver;

    @Test
    public void verifyResolution_Rsa() {
        // Arrange - Add Key Parser
        keyResolver.addParser(new FsPrivateKeyTestParser<RSAPrivateKey>());
        // Act & Assert
        assertThat(keyResolver.resolvePrivateKey("testkey", RSAPrivateKey.class))
                .isNotNull();
    }

    @Test
    public void verifyResolution_ec() {
        // Arrange - Add Key Parser
        keyResolver.addParser(new FsPrivateKeyTestParser<ECPrivateKey>());
        // Act & Assert
        assertThat(keyResolver.resolvePrivateKey("testkey-ec", ECPrivateKey.class))
                .isNotNull();
    }

    @Test
    public void verifyParser_PrivateKeyWrapper() {
        // Without PrivateKeyWrapper Parser throws exception
        assertThatThrownBy(() -> keyResolver.resolvePrivateKey("testkey-ec", PrivateKeyWrapper.class))
                .isInstanceOf(EdcException.class);
        // Add Key Parser
        keyResolver.addParser(PrivateKeyWrapper.class, (encoded) -> {
            try {
                var ecKey = (ECKey) ECKey.parseFromPEMEncodedObjects(encoded);
                return new EcPrivateKeyWrapper(ecKey);
            } catch (JOSEException e) {
                throw new EdcException(e);
            }
        });
        // Now able to parse key
        assertThat(keyResolver.resolvePrivateKey("testkey-ec", PrivateKeyWrapper.class)).isNotNull();
    }

    // Test Key Parser for RSAPrivateKey and ECPrivateKey keys.
    static class FsPrivateKeyTestParser<T> implements KeyParser<T> {

        @Override
        public boolean canParse(Class<?> keyType) {
            return keyType.equals(RSAPrivateKey.class) || keyType.equals(ECPrivateKey.class);
        }

        @Override
        public T parse(String encoded) {
            try {
                var pemParser = new PEMParser(new StringReader(encoded));
                var keyObj = pemParser.readObject();
                if (!(keyObj instanceof PEMKeyPair)) {
                    throw new IOException("Not an OpenSSL key");
                }
                var keyPair = new JcaPEMKeyConverter()
                        .setProvider("BC").getKeyPair((PEMKeyPair) keyObj);

                return (T) keyPair.getPrivate();
            } catch (IOException e) {
                throw new EdcException(e);
            }
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        // Add BouncyCastleProvider to java security.
        Security.addProvider(new BouncyCastleProvider());

        var url = getClass().getClassLoader().getResource(FsRsaPrivateKeyResolverTest.TEST_KEYSTORE);
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        assert url != null;
        try (InputStream stream = url.openStream()) {
            keyStore.load(stream, FsRsaPrivateKeyResolverTest.PASSWORD.toCharArray());
        }
        keyResolver = new FsPrivateKeyResolver(FsRsaPrivateKeyResolverTest.PASSWORD, keyStore);
    }
}
