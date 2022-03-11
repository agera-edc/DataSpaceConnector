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
     *
     * @param policyId id of the policy.
     */
    @Nullable
    Policy findById(String policyId);

    /**
     * Returns stream of policies in the store based on query spec.
     *
     * @param spec query specification.
     */
    Stream<Policy> findAll(QuerySpec spec);

    /**
     * Persists the policy.
     *
     * @param policy to be saved.
     */
    void save(Policy policy);

    /**
     * Removes a policy for the given id.
     * Returns deleted policy or null if policy not found.
     *
     * @param policyId id of the policy to be removed.
     */
    Policy delete(String policyId);
}