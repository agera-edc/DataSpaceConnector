package org.eclipse.dataspaceconnector.spi.contract.policy.store;

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.jetbrains.annotations.Nullable;
import java.util.stream.Stream;

/**
 * Persists {@link Policy}.
 */
public interface PolicyStore {

    /**
     * Returns the policy by id or returns null if not found.
     */
    @Nullable
    Policy findById(String policyId);

    /**
     * Returns stream of all policies in the store.
     */
    Stream<Policy> findAll(QuerySpec spec);

    /**
     * Persists the policy.
     */
    void save(Policy policy);

    /**
     * Removes a policy for the given id.
     * Returns deleted policy or null if policy not found.
     */
    Policy delete(String policyId);
}