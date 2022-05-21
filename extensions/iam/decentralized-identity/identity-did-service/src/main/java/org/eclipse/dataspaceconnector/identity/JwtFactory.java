package org.eclipse.dataspaceconnector.identity;

import com.nimbusds.jwt.SignedJWT;

import java.util.function.Function;

public interface JwtFactory extends Function<String, SignedJWT> {
}
