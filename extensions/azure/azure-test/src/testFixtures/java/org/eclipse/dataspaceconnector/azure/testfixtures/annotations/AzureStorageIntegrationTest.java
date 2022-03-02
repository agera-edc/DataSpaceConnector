package org.eclipse.dataspaceconnector.azure.testfixtures.annotations;

import org.eclipse.dataspaceconnector.common.annotations.IntegrationTest;
import org.junit.jupiter.api.Tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Composed annotation for Azure Storage integration testing.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@IntegrationTest
@Tag("azure-cosmos-db-integration-test")
public @interface AzureStorageIntegrationTest {
}
