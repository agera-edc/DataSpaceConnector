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

import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.KeyParser;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.StringWriter;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Resolves an RSA or EC private key from a JKS keystore.
 */
public class FsPrivateKeyResolver implements PrivateKeyResolver {
    private final Map<String, PrivateKey> privateKeyCache = new HashMap<>();
    private final List<KeyParser<?>> parsers = new ArrayList<>();

    private final Monitor monitor;

    /**
     * Constructor.
     * Caches the private keys for performance.
     *
     * @param password the keystore password. Individual key passwords are not supported.
     * @param keyStore the keystore
     */
    public FsPrivateKeyResolver(String password, KeyStore keyStore, Monitor monitor) {
        this.monitor = monitor;
        char[] encodedPassword = password.toCharArray();
        try {
            Enumeration<String> iter = keyStore.aliases();
            while (iter.hasMoreElements()) {
                String alias = iter.nextElement();
                if (!keyStore.isKeyEntry(alias)) {
                    continue;
                }
                Key key = keyStore.getKey(alias, encodedPassword);
                if ((key instanceof RSAPrivateKey || key instanceof ECPrivateKey)) {
                    privateKeyCache.put(alias, (PrivateKey) key);
                }
            }

        } catch (GeneralSecurityException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public <T> @Nullable T resolvePrivateKey(String id, Class<T> keyType) {
        var key = privateKeyCache.get(id);

        if (key == null) {
            return null;
        }

        var keyParser = getParser(keyType);

        if (keyParser == null) {
            monitor.debug("No KeyParser available for type " + keyType);
            return keyType.cast(privateKeyCache.get(id));
        }

        return keyType.cast(keyParser.parse(toPemEncoded(key)));
    }

    @Override
    public <T> void addParser(KeyParser<T> parser) {
        parsers.add(parser);
    }

    @Override
    public <T> void addParser(Class<T> forType, Function<String, T> parseFunction) {
        var p = new KeyParser<T>() {

            @Override
            public boolean canParse(Class<?> keyType) {
                return Objects.equals(keyType, forType);
            }

            @Override
            public T parse(String encoded) {
                return parseFunction.apply(encoded);
            }
        };
        addParser(p);
    }

    @SuppressWarnings("unchecked")
    private <T> KeyParser<T> getParser(Class<T> keytype) {
        return (KeyParser<T>) parsers.stream().filter(p -> p.canParse(keytype))
                .findFirst().orElse(null);
    }

    private String toPemEncoded(PrivateKey key) {
        var writer = new StringWriter();
        try (var jcaPEMWriter = new JcaPEMWriter(writer)) {
            jcaPEMWriter.writeObject(key);
        } catch (IOException e) {
            throw new EdcException("Unable to convert private in PEM format ", e);
        }

        return writer.toString();
    }
}
