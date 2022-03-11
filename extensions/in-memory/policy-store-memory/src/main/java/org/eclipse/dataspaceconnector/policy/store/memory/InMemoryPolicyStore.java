/*
 *  Copyright (c) 2020-2022 Microsoft Corporation
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
package org.eclipse.dataspaceconnector.policy.store.memory;

import org.eclipse.dataspaceconnector.common.concurrency.LockManager;
import org.eclipse.dataspaceconnector.common.reflection.ReflectionUtil;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.contract.policy.store.PolicyStore;
import org.eclipse.dataspaceconnector.spi.query.BaseCriterionToPredicateConverter;
import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.query.SortOrder;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.eclipse.dataspaceconnector.common.reflection.ReflectionUtil.propertyComparator;

/**
 * An in-memory, threadsafe policy store.
 * This implementation is intended for testing purposes only.
 */
public class InMemoryPolicyStore implements PolicyStore {

    private final LockManager lockManager = new LockManager(new ReentrantReadWriteLock());
    private final Map<String, Policy> policiesById =  new HashMap<>();

    @Override
    public @Nullable Policy findById(String policyId) {
        return lockManager.readLock(() -> policiesById.get(policyId));
    }

    @Override
    public Stream<Policy> findAll(QuerySpec spec) {
        return lockManager.readLock(() -> {
            var stream = policiesById.values().stream();
            return applyQuery(spec, stream, Policy.class);
        });
    }

    @Override
    public void save(Policy policy) {
        lockManager.writeLock(() -> policiesById.put(policy.getUid(), policy));
    }

    @Override
    public Policy delete(String policyId) {
        return lockManager.writeLock(() -> Optional.ofNullable(policiesById.remove(policyId)).orElse(null));
    }

    private <T> Stream<T> applyQuery(QuerySpec spec, Stream<T> streamOfAll, Class<?> clazz) {

        //filter
        var andPredicate = spec.getFilterExpression().stream().map(this::toPredicate).reduce(x -> true, Predicate::and);
        Stream<T> stream  = streamOfAll.filter(andPredicate);

        //sort
        var sortField = spec.getSortField();

        if (sortField != null) {
            // if the sortfield doesn't exist on the object -> return empty
            if (ReflectionUtil.getFieldRecursive(clazz, sortField) == null) {
                return Stream.empty();
            }
            var comparator = propertyComparator(spec.getSortOrder() == SortOrder.ASC, sortField);
            stream = stream.sorted(comparator);
        }

        // limit
        return stream.skip(spec.getOffset()).limit(spec.getLimit());
    }

    private <T> Predicate<T> toPredicate(Criterion criterion) {
        BaseCriterionToPredicateConverter<T> predicateConverter = new BaseCriterionToPredicateConverter<>() {
            @Override
            protected <R> R property(String key, Object object) {
                return ReflectionUtil.getFieldValueSilent(key, object);
            }
        };
        return predicateConverter.convert(criterion);
    }

}
