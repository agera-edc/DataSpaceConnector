package org.eclipse.dataspaceconnector.spi.contract.policy.store;

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.persistence.EdcPersistenceException;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

/**
 * Persists {@link Policy}.
 */
public interface PolicyStore {

    /**
     * Findss the policy by id.
     *
     * @param policyId id of the policy.
     * @return found Policy or null if not found.
     * @throws EdcPersistenceException if something goes wrong.
     */
    @Nullable
    Policy findById(String policyId);

    /**
     * Find stream of policies in the store based on query spec.
     *
     * @param spec query specification.
     * @return stream of found policies.
     * @throws EdcPersistenceException if something goes wrong.
     */
    Stream<Policy> findAll(QuerySpec spec);

    /**
     * Persists the policy.
     *
     * @param policy to be saved.
     * @throws EdcPersistenceException if something goes wrong.
     */
    void save(Policy policy);

    /**
     * Deletes a policy for the given id.
     *
     * @param policyId id of the policy to be removed.
     * @return deleted policy or null if policy not found.
     * @throws EdcPersistenceException if something goes wrong.
     */
    Policy delete(String policyId);
}