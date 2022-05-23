package org.eclipse.dataspaceconnector.controlplane;

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.security.VaultPrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.system.Provider;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FilePrivateKeyResolverExtension implements ServiceExtension {
    @Provider
    public PrivateKeyResolver privateKeyResolver(ServiceExtensionContext context) {
        Vault vault = new Vault() {

            @Override
            public @Nullable String resolveSecret(String key) {
                if (!"test-connector-name".equals(key)) {
                    return null;
                }

                try {
                    return Files.readString(Path.of("ec-keypair.pem"));
                } catch (IOException e) {
                    throw new EdcException(e);
                }
            }

            @Override
            public Result<Void> storeSecret(String key, String value) {
                return Result.failure("Not implemented");
            }

            @Override
            public Result<Void> deleteSecret(String key) {
                return Result.failure("Not implemented");
            }
        };
        return new VaultPrivateKeyResolver(vault);
    }
}
