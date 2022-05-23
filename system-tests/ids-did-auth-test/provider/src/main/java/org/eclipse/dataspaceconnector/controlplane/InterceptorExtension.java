package org.eclipse.dataspaceconnector.controlplane;

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
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

public class InterceptorExtension implements ServiceExtension {
    @Provider
    public Interceptor interceptor(ServiceExtensionContext context) {
        return new Interceptor() {
            @NotNull
            @Override
            public Response intercept(@NotNull Interceptor.Chain chain) throws IOException {
                if (chain.request().url().equals(HttpUrl.parse("https://consumer/.well-known/did.json"))) {

                    var p = Files.readString(Path.of("ec-keypair.pem"));
                    ECKey eckey;
                    try {
                        eckey = (ECKey) ECKey.parseFromPEMEncodedObjects(p);
                    } catch (JOSEException e) {
                        throw new EdcException(e);
                    }
                    var publicKey = new EllipticCurvePublicKey(eckey.getCurve().getName(), eckey.getKeyType().getValue(), eckey.getX().toString(), eckey.getY().toString());
                    var didDocument = DidDocument.Builder.newInstance()
                            .id("did:web:consumer")
                            .verificationMethod("#my-key1", DidConstants.ECDSA_SECP_256_K_1_VERIFICATION_KEY_2019, publicKey)
                            .service(Collections.singletonList(new Service("#my-service1", DidConstants.HUB_URL, "http://doesnotexi.st")))
                            .build();
                    var did = new TypeManager().writeValueAsString(didDocument);

                    return new Response.Builder()
                            .body(ResponseBody.create(did, MediaType.get("application/json")))
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .build();
                }
                return chain.proceed(chain.request());
            }
        };
    }
}
