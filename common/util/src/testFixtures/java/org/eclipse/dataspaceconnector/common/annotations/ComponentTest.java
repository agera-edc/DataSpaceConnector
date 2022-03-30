package org.eclipse.dataspaceconnector.common.annotations;

import org.junit.jupiter.api.Tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for ComponentTest.
 * The ComponentTest do not use an external system but uses collaborator objects instead of mocks. For example in-memory implementations.
 * It applies a specific Junit Tag.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@IntegrationTest
@Tag("ComponentTest")
public @interface ComponentTest {
}
