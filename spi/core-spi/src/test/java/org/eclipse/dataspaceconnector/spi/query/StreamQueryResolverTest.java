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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class StreamQueryResolverTest {

    private StreamQueryResolver<FakeItem> queryResolver = new StreamQueryResolver<>(FakeItem.class);

    @Test
    void verifyQuery_noFilters() {
        var stream = IntStream.range(0, 10).mapToObj(FakeItem::new);

        QuerySpec spec = QuerySpec.Builder.newInstance().build();
        assertThat(queryResolver.applyQuery(spec, stream)).hasSize(10);
    }

    @Test
    @Disabled
    void verifyQuery_equal() {
        var stream = IntStream.range(0, 10).mapToObj(FakeItem::new);

        QuerySpec spec = QuerySpec.Builder.newInstance().equalsAsContains(false).filter("id=5").build();
        assertThat(queryResolver.applyQuery(spec, stream)).hasSize(1).extracting(FakeItem::getId).containsExactly(5);
    }

    @Test
    @Disabled
    void verifyQuery_criterionFilter() {
        var stream = IntStream.range(0, 10).mapToObj(FakeItem::new).collect(Collectors.toList());

        QuerySpec spec = QuerySpec.Builder.newInstance().filter(List.of(new Criterion("id", "=", "5"))).build();
        Collection<FakeItem> actual = queryResolver.applyQuery(spec, stream.stream()).collect(Collectors.toList());
        assertThat(actual).hasSize(1).extracting(FakeItem::getId).containsExactly(5);
    }

    @Test
    void verifyQuery_sortDesc() {
        var stream = IntStream.range(0, 10).mapToObj(FakeItem::new);

        QuerySpec spec = QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.DESC).build();
        assertThat(queryResolver.applyQuery(spec, stream)).hasSize(10).isSortedAccordingTo(Comparator.comparing(FakeItem::getId).reversed());
    }

    @Test
    void verifyQuery_sortAsc() {
        var stream = IntStream.range(0, 10).mapToObj(FakeItem::new);

        QuerySpec spec = QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.ASC).build();
        assertThat(queryResolver.applyQuery(spec, stream)).hasSize(10).isSortedAccordingTo(Comparator.comparing(FakeItem::getId));
    }

    @Test
    void verifyQuery_invalidSortField() {
        var stream = IntStream.range(0, 10).mapToObj(FakeItem::new);

        QuerySpec spec = QuerySpec.Builder.newInstance().sortField("xyz").sortOrder(SortOrder.ASC).build();
        assertThat(queryResolver.applyQuery(spec, stream)).isEmpty();
    }

    @Test
    void verifyQuery_offsetAndLimit() {
        var stream = IntStream.range(0, 10).mapToObj(FakeItem::new);

        QuerySpec spec = QuerySpec.Builder.newInstance().offset(1).limit(2).build();
        assertThat(queryResolver.applyQuery(spec, stream)).extracting(FakeItem::getId).containsExactly(1, 2);
    }

    @Test
    void verifyQuery_allFilters() {
        var stream = IntStream.range(0, 10).mapToObj(FakeItem::new);

        QuerySpec spec = QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.DESC).offset(1).limit(2).build();
        assertThat(queryResolver.applyQuery(spec, stream)).extracting(FakeItem::getId).containsExactly(8, 7);
    }

    private static class FakeItem {
        private int id;

        private FakeItem(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FakeItem fakeItem = (FakeItem) o;
            return id == fakeItem.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }

}