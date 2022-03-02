package org.eclipse.dataspaceconnector.iam.daps.annotations;

import org.eclipse.dataspaceconnector.common.annotations.IntegrationTest;
import org.junit.jupiter.api.Tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Composed annotation for Daps integration testing.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@IntegrationTest
@Tag("daps-integration-test")
public @interface DapsIntegrationTest {
}
