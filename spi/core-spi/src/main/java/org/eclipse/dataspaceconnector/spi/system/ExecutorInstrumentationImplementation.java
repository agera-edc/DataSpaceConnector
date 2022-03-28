/*
 *  Copyright (c) 2022 Microsoft Corporation
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
package org.eclipse.dataspaceconnector.spi.system;

/**
 * Interface for custom implementations of {@link ExecutorInstrumentation}.
 * <p>
 * A distinct interface is needed for dependency injection to resolve the optional
 * {@link ExecutorInstrumentationImplementation} vs. the mandatory {@link ExecutorInstrumentation}
 * that is provided by core services, providing a default implementation if none is provided.
 */
public interface ExecutorInstrumentationImplementation extends ExecutorInstrumentation {
}