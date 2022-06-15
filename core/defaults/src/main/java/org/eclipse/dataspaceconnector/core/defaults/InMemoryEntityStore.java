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

package org.eclipse.dataspaceconnector.core.defaults;

import org.eclipse.dataspaceconnector.spi.persistence.StateMachineEntity;
import org.eclipse.dataspaceconnector.spi.query.QueryResolver;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.query.ReflectionBasedQueryResolver;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * An in-memory, threadsafe entity store.
 * This implementation is intended for testing purposes only.
 */
public class InMemoryEntityStore<T extends StateMachineEntity<T>> {
    private final Map<String, T> entitiesById = new ConcurrentHashMap<>();
    private final QueryResolver<T> queryResolver;

    protected InMemoryEntityStore(Class<T> clazz) {
        queryResolver = new ReflectionBasedQueryResolver<>(clazz);
    }

    public T find(String id) {
        T t = entitiesById.get(id);
        if (t == null) {
            return null;
        }
        return t.copy();
    }

    public void upsert(T entity) {
        T internalCopy = entity.copy();
        entitiesById.put(entity.getId(), internalCopy);
    }

    public void delete(String id) {
        entitiesById.remove(id);
    }

    public Stream<T> findAll(QuerySpec querySpec) {
        return queryResolver.query(findAll(), querySpec);
    }

    public @NotNull List<T> nextForState(int state, int max) {
        return findAll()
                .filter(e -> e.getState() == state)
                .sorted(Comparator.comparingLong(T::getStateTimestamp)) //order by state timestamp, oldest first
                .limit(max)
                .map(T::copy)
                .collect(toList());
    }

    protected Stream<T> findAll() {
        return entitiesById.values().stream();
    }
}
