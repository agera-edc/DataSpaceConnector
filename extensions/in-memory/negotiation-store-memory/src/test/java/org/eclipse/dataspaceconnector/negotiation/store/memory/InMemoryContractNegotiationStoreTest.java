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

package org.eclipse.dataspaceconnector.negotiation.store.memory;

import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.query.SortOrder;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.dataspaceconnector.negotiation.store.memory.TestFunctions.createNegotiation;
import static org.junit.jupiter.api.Assertions.*;

class InMemoryContractNegotiationStoreTest {
    private InMemoryContractNegotiationStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryContractNegotiationStore();
    }

    @Test
    void verifyCreateUpdateDelete() {
        String id = UUID.randomUUID().toString();
        ContractNegotiation negotiation = createNegotiation(id);
        negotiation.transitionInitial();

        store.save(negotiation);

        ContractNegotiation found = store.find(id);

        assertNotNull(found);
        assertNotSame(found, negotiation); // enforce by-value

        assertNotNull(store.findContractAgreement("agreementId"));

        assertEquals(ContractNegotiationStates.INITIAL.code(), found.getState());

        negotiation.transitionRequesting();

        store.save(negotiation);

        found = store.find(id);
        assertNotNull(found);
        assertEquals(ContractNegotiationStates.REQUESTING.code(), found.getState());

        store.delete(id);
        Assertions.assertNull(store.find(id));
        assertNull(store.findContractAgreement("agreementId"));

    }

    @Test
    void verifyNext() throws InterruptedException {
        String id1 = UUID.randomUUID().toString();
        ContractNegotiation negotiation1 = createNegotiation(id1);
        negotiation1.transitionInitial();
        negotiation1.transitionRequesting();
        String id2 = UUID.randomUUID().toString();
        ContractNegotiation negotiation2 = createNegotiation(id2);
        negotiation2.transitionInitial();
        negotiation2.transitionRequesting();

        store.save(negotiation1);
        store.save(negotiation2);

        negotiation2.transitionRequested();
        store.save(negotiation2);
        Thread.sleep(1);
        negotiation1.transitionRequested();
        store.save(negotiation1);

        assertTrue(store.nextForState(ContractNegotiationStates.REQUESTING.code(), 1).isEmpty());

        List<ContractNegotiation> found = store.nextForState(ContractNegotiationStates.REQUESTED.code(), 1);
        assertEquals(1, found.size());
        assertEquals(negotiation2, found.get(0));

        found = store.nextForState(ContractNegotiationStates.REQUESTED.code(), 3);
        assertEquals(2, found.size());
        assertEquals(negotiation2, found.get(0));
        assertEquals(negotiation1, found.get(1));
    }

    @Test
    void verifyMultipleRequest() {
        String id1 = UUID.randomUUID().toString();
        ContractNegotiation negotiation1 = createNegotiation(id1);
        negotiation1.transitionInitial();
        store.save(negotiation1);

        String id2 = UUID.randomUUID().toString();
        ContractNegotiation negotiation2 = createNegotiation(id2);
        negotiation2.transitionInitial();
        store.save(negotiation2);


        ContractNegotiation found1 = store.find(id1);
        assertNotNull(found1);

        ContractNegotiation found2 = store.find(id2);
        assertNotNull(found2);

        var found = store.nextForState(ContractNegotiationStates.INITIAL.code(), 3);
        assertEquals(2, found.size());

    }

    @Test
    void verifyOrderingByTimestamp() {
        for (int i = 0; i < 100; i++) {
            ContractNegotiation negotiation = createNegotiation("test-negotiation-" + i);
            negotiation.transitionInitial();
            store.save(negotiation);
        }

        List<ContractNegotiation> processes = store.nextForState(ContractNegotiationStates.INITIAL.code(), 50);

        assertThat(processes).hasSize(50);
        assertThat(processes).allMatch(p -> p.getStateTimestamp() > 0);
    }

    @Test
    void verifyNextForState_avoidsStarvation() throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            ContractNegotiation negotiation = createNegotiation("test-negotiation-" + i);
            negotiation.transitionInitial();
            store.save(negotiation);
        }

        var list1 = store.nextForState(ContractNegotiationStates.INITIAL.code(), 5);
        Thread.sleep(50); //simulate a short delay to generate different timestamps
        list1.forEach(tp -> store.save(tp));
        var list2 = store.nextForState(ContractNegotiationStates.INITIAL.code(), 5);
        assertThat(list1).isNotEqualTo(list2).doesNotContainAnyElementsOf(list2);
    }

    @Test
    void findAll_noQuerySpec() {
        IntStream.range(0, 10).forEach(i -> store.save(createNegotiation("test-neg-" + i)));

        var all = store.queryNegotiations(QuerySpec.Builder.newInstance().build());
        assertThat(all).hasSize(10);
    }

    @Test
    void findAll_verifyPaging() {

        IntStream.range(0, 10).forEach(i -> store.save(createNegotiation("test-neg-" + i)));

        // page size fits
        assertThat(store.queryNegotiations(QuerySpec.Builder.newInstance().offset(3).limit(4).build())).hasSize(4);

        // page size too large
        assertThat(store.queryNegotiations(QuerySpec.Builder.newInstance().offset(5).limit(100).build())).hasSize(5);
    }

    @Test
    void findAll_verifyFiltering() {
        IntStream.range(0, 10).forEach(i -> store.save(createNegotiation("test-neg-" + i)));
        assertThat(store.queryNegotiations(QuerySpec.Builder.newInstance().equalsAsContains(false).filter("id=test-neg-3").build())).extracting(ContractNegotiation::getId).containsOnly("test-neg-3");
    }

    @Test
    void findAll_verifyFiltering_invalidFilterExpression() {
        IntStream.range(0, 10).forEach(i -> store.save(createNegotiation("test-neg-" + i)));
        assertThatThrownBy(() -> store.queryNegotiations(QuerySpec.Builder.newInstance().filter("something foobar other").build())).isInstanceOfAny(IllegalArgumentException.class);
    }

    @Test
    void findAll_verifySorting() {
        IntStream.range(0, 10).forEach(i -> store.save(createNegotiation("test-neg-" + i)));

        assertThat(store.queryNegotiations(QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.ASC).build())).hasSize(10).isSortedAccordingTo(Comparator.comparing(ContractNegotiation::getId));
        assertThat(store.queryNegotiations(QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.DESC).build())).hasSize(10).isSortedAccordingTo((c1, c2) -> c2.getId().compareTo(c1.getId()));
    }

    @Test
    void findAll_verifySorting_invalidProperty() {
        IntStream.range(0, 10).forEach(i -> store.save(createNegotiation("test-neg-" + i)));

        var query = QuerySpec.Builder.newInstance().sortField("notexist").sortOrder(SortOrder.DESC).build();

        // must actually collect, otherwise the stream is not materialized
        assertThat(store.queryNegotiations(query).collect(Collectors.toList())).hasSize(10);
    }


}
