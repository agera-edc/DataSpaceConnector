package org.eclipse.dataspaceconnector.policy.store.memory;

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.contract.policy.store.PolicyStore;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.query.SortOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryPolicyStoreTest {
    private PolicyStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryPolicyStore();
    }

    @Test
    void findById_whenPresent() {
        Policy policy = Policy.Builder.newInstance().build();
        store.save(policy);

        assertThat(store.findById(policy.getUid())).isEqualTo(policy);
    }

    @Test
    void findById_whenMissing() {
        assertThat(store.findById(UUID.randomUUID().toString())).isNull();
    }

    @Test
    void findAll_whenNoFiltersWithLimitAndOffset() {
        Policy policy1 = Policy.Builder.newInstance().build();
        Policy policy2 = Policy.Builder.newInstance().build();
        Policy policy3 = Policy.Builder.newInstance().build();
        store.save(policy1);
        store.save(policy2);
        store.save(policy3);

        assertThat(store.findAll(QuerySpec.Builder.newInstance().limit(3).offset(1).build())).containsExactlyInAnyOrder(policy2, policy3);
    }

    @Test
    void findAll_whenEqualFilter() {
        Policy policy1 = Policy.Builder.newInstance().build();
        Policy policy2 = Policy.Builder.newInstance().build();
        Policy policy3 = Policy.Builder.newInstance().build();
        store.save(policy1);
        store.save(policy2);
        store.save(policy3);

        assertThat(store.findAll(QuerySpec.Builder.newInstance().filter("uid=" + policy1.getUid()).build())).containsExactly(policy1);
    }

    @Test
    void findAll_whenSort() {
        Policy policy1 = Policy.Builder.newInstance()
                .id("C")
                .build();
        Policy policy2 = Policy.Builder.newInstance()
                .id("A")
                .build();
        Policy policy3 = Policy.Builder.newInstance()
                .id("B")
                .build();
        store.save(policy1);
        store.save(policy2);
        store.save(policy3);

        assertThat(store.findAll(QuerySpec.Builder.newInstance().sortField("uid").sortOrder(SortOrder.ASC).build())).containsExactly(policy2, policy3, policy1);
    }

    @Test
    void findAll_allFilters() {
        Policy policy1 = Policy.Builder.newInstance()
                .target("target1")
                .id("1C")
                .build();
        Policy policy2 = Policy.Builder.newInstance()
                .target("target1")
                .id("1A")
                .build();
        Policy policy3 = Policy.Builder.newInstance()
                .target("target1")
                .id("1B")
                .build();
        Policy policyX = Policy.Builder.newInstance()
                .target("target2")
                .id("2X")
                .build();
        Policy policyY = Policy.Builder.newInstance()
                .target("target2")
                .id("2Y")
                .build();
        store.save(policy1);
        store.save(policy2);
        store.save(policy3);
        store.save(policyX);
        store.save(policyY);

        QuerySpec uid = QuerySpec.Builder.newInstance()
                .filter("target=target1")
                .sortField("uid")
                .sortOrder(SortOrder.DESC)
                .offset(1)
                .limit(1)
                .build();
        assertThat(store.findAll(uid)).containsExactly(policy3);
    }

    @Test
    void deleteById_whenPresent() {
        Policy policy = Policy.Builder.newInstance().build();
        store.save(policy);
        assertThat(store.findById(policy.getUid())).isEqualTo(policy);

        assertThat(store.delete(policy.getUid())).isEqualTo(policy);
        assertThat(store.findById(policy.getUid())).isNull();
    }

    @Test
    void deleteById_whenMissing() {
        assertThat(store.delete(UUID.randomUUID().toString())).isNull();
    }

}