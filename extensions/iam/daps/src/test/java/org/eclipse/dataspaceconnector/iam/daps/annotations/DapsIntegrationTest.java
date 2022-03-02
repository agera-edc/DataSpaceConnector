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
 *
 */

package org.eclipse.dataspaceconnector.iam.daps.annotations;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for Daps integration testing. It applies specific Junit Tag.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Tag("daps-integration-test")
@EnabledIfEnvironmentVariable(named = "RUN_INTEGRATION_TEST", matches = "true")
public @interface DapsIntegrationTest {
}
