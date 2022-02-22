package org.eclipse.dataspaceconnector.azure.testfixtures;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import org.eclipse.dataspaceconnector.spi.EdcException;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import static java.io.File.separator;
import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.common.configuration.ConfigurationFunctions.propOrEnv;

public interface CosmosTestClient {

    static CosmosClient createClient() {
        var cosmosKey = propOrEnv("COSMOS_KEY", null);
        if (cosmosKey != null) {
            return clientForCi(cosmosKey);
        } else {
            return localClient();
        }
    }

    private static CosmosClient clientForCi(String cosmosKey) {
        return new CosmosClientBuilder()
                .key(cosmosKey)
                .preferredRegions(List.of("westeurope"))
                .endpoint("https://cosmos-itest.documents.azure.com:443/")
                .buildClient();
    }

    private static CosmosClient localClient() {
        try {
            var endpoint = "https://127.0.0.1:8081";

            var keystorePath = System.getProperty("java.home") + separator + "lib" + separator + "security" + separator + "cacerts";
            var keystore = KeyStore.getInstance(KeyStore.getDefaultType());

            var keystorePassword = "changeit".toCharArray();
            keystore.load(new FileInputStream(keystorePath), keystorePassword);

            var certificate = getCertificateFrom(endpoint);
            keystore.setCertificateEntry("cosmosdb", certificate);

            try (var output = new FileOutputStream(keystorePath)) {
                keystore.store(output, keystorePassword);
            }

            var newKeystorePath = Files.createTempFile(null, null);
            try (var output = new FileOutputStream(newKeystorePath.toFile())) {
                keystore.store(output, keystorePassword);
            }
            System.setProperty("javax.net.ssl.trustStore", newKeystorePath.toString());

            // Seems to be a fixed value: https://github.com/Azure/azure-cosmos-db-emulator-docker
            String masterKey = "C2y6yDjf5/R+ob0N8A7Cgv30VRDJIWEHLM+4QDU5DE2nQ9nDuVTqobD4b8mGGyPMbIZnqyMsEcaGQy67XIw/Jw==";

            return new CosmosClientBuilder()
                    .key(masterKey)
                    .endpoint(endpoint)
                    .buildClient();

        } catch (Exception e) {
            throw new EdcException("Error in creating cosmos local client.", e);
        }
    }

    private static Certificate getCertificateFrom(String endpoint) {
        var url = endpoint + "/_explorer/emulator.pem";
        var client = trustAllHttpClient();
        var request = HttpRequest.newBuilder().uri(URI.create(url)).build();
        try (var stream = client.send(request, HttpResponse.BodyHandlers.ofInputStream()).body()) {
            return CertificateFactory.getInstance("X.509").generateCertificate(stream);
        } catch (Exception e) {
            throw new RuntimeException(format("Error getting certificate. Url: %s", url), e);
        }
    }

    private static HttpClient trustAllHttpClient() {
        var trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                }
            }
        };

        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            return HttpClient.newBuilder().sslContext(sslContext).build();
        } catch (Exception e) {
            throw new EdcException("Error initializing ssl context", e);
        }
    }

}
