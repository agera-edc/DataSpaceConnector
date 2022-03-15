/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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
package org.eclipse.dataspaceconnector.spi.query;

import java.util.stream.Stream;

/**
 * Class responsible for applying queries on an input elements of type T. Currently supports querying a stream of elements.
 *
 * @param <T> type of the queried elements.
 */
public abstract class QueryResolver<T> {

    /**
     * Method to query a stream by provided specification.
     *
     * @param stream stream to be queried.
     * @param spec query specification.
     * @return stream result from queries.
     */
    public abstract Stream<T> query(Stream<T> stream, QuerySpec spec);
}
