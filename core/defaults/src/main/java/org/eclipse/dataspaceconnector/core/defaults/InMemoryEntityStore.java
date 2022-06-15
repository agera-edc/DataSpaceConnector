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

import org.eclipse.dataspaceconnector.common.concurrency.LockManager;
import org.eclipse.dataspaceconnector.spi.persistence.StateMachineEntity;
import org.eclipse.dataspaceconnector.spi.query.QueryResolver;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.query.ReflectionBasedQueryResolver;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * An in-memory, threadsafe entity store.
 * This implementation is intended for testing purposes only.
 */
public class InMemoryEntityStore<T extends StateMachineEntity<T>> {
    private final Map<String, Item> entitiesById = new ConcurrentHashMap<>();
    private final QueryResolver<T> queryResolver;
    private final LockManager lockManager = new LockManager(new ReentrantReadWriteLock());

    private class Item{
        private T item;
        private boolean leased;

        public Item(T item) {
            this.item = item;
        }

        public Item(T item, boolean leased) {
            this.item = item;
            this.leased = leased;
        }
    }

    protected InMemoryEntityStore(Class<T> clazz) {
        queryResolver = new ReflectionBasedQueryResolver<>(clazz);
    }

    public T find(String id) {
        Item t = entitiesById.get(id);
        if (t == null) {
            return null;
        }
        return t.item.copy();
    }

    public void upsert(T entity) {
        T internalCopy = entity.copy();
        // entitiesById.merge(entity.getId(), new Item(internalCopy), (v1, v2) -> new Item(v2.item, v1.leased));
        entitiesById.put(entity.getId(), new Item(internalCopy));
    }

    public void delete(String id) {
        entitiesById.remove(id);
    }

    public Stream<T> findAll(QuerySpec querySpec) {
        return queryResolver.query(findAll(), querySpec);
    }

    public @NotNull List<T> nextForState(int state, int max) {
        return lockManager.writeLock(() -> {
            var items = entitiesById.values().stream()
                    .filter(e -> e.item.getState() == state)
                    .filter(e -> !e.leased)
                    .sorted(Comparator.comparingLong(e -> e.item.getStateTimestamp())) //order by state timestamp, oldest first
                    .limit(max)
                    .collect(toList());
            items.forEach(e -> e.leased = true);
            return items.stream().map(e -> e.item.copy()).collect(toList());
        });
    }

    protected Stream<T> findAll() {
        return entitiesById.values().stream().map(e -> e.item);
    }
}
