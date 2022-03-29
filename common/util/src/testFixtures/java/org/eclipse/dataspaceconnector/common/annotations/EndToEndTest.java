package org.eclipse.dataspaceconnector.common.annotations;

import org.junit.jupiter.api.Tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for End to End integration testing. When the entire system is involved in a test.
 * It applies a specific Junit Tag.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@IntegrationTest
@Tag("EndToEndTest")
public @interface EndToEndTest {
}
