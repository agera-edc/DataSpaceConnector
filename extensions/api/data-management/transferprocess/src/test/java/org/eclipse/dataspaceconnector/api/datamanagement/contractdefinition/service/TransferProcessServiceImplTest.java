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
package org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.service;

import com.github.javafaker.Faker;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.transaction.NoopTransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;
import org.eclipse.dataspaceconnector.transfer.core.command.commands.CancelTransferCommand;
import org.eclipse.dataspaceconnector.transfer.core.command.commands.DeprovisionRequest;
import org.eclipse.dataspaceconnector.transfer.core.command.commands.SingleTransferProcessCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.CANCELLED;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.COMPLETED;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.DEPROVISIONED;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.DEPROVISIONING;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.ENDED;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.ERROR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TransferProcessServiceImplTest {
    static Faker faker = new Faker();

    String id = faker.lorem().word();
    TransferProcess process1 = transferProcess();

    TransferProcess process2 = transferProcess();
    QuerySpec query = QuerySpec.Builder.newInstance().limit(5).offset(2).build();
    ArgumentCaptor<SingleTransferProcessCommand> commandCaptor = ArgumentCaptor.forClass(SingleTransferProcessCommand.class);

    TransferProcessStore store = mock(TransferProcessStore.class);
    TransferProcessManager manager = mock(TransferProcessManager.class);
    TransactionContext transactionContext = spy(new NoopTransactionContext());

    TransferProcessService service = new TransferProcessServiceImpl(store, manager, transactionContext);

    @AfterEach
    void after() {
        verify(transactionContext).execute(any());
    }

    @Test
    void findById_whenFound() {
        when(store.find(id)).thenReturn(process1);
        assertThat(service.findById(id)).isSameAs(process1);
    }

    @Test
    void findById_whenNotFound() {
        assertThat(service.findById(id)).isNull();
    }

    @Test
    void query() {
        var stream = Stream.of(process1, process2);
        when(store.findAll(query)).thenReturn(stream);
        assertThat(service.query(query)).containsExactly(process1, process2);
    }

    @Test
    void getState_whenFound() {
        when(store.find(id)).thenReturn(process1);
        assertThat(service.getState(id)).isEqualTo(TransferProcessStates.from(process1.getState()).name());
    }

    @Test
    void getState_whenNotFound() {
        assertThat(service.getState(id)).isNull();
    }

    @ParameterizedTest
    @MethodSource("cancellableStates")
    void cancel(TransferProcessStates state) {
        // Arrange
        var process = transferProcess(state);
        when(store.find(id)).thenReturn(process);

        // Act
        var result = service.cancel(id);

        // Assert
        assertThat(result.succeeded()).isTrue();
        verify(manager).enqueueCommand(commandCaptor.capture());
        assertThat(commandCaptor.getValue()).isInstanceOf(CancelTransferCommand.class);
        assertThat(commandCaptor.getValue().getTransferProcessId())
                .isEqualTo(id);
    }

    @ParameterizedTest
    @MethodSource("nonCancellableStates")
    void cancel_whenNonCancellable(TransferProcessStates state) {
        // Arrange
        var process = transferProcess(state);
        when(store.find(id)).thenReturn(process);

        // Act
        Result<?> result = service.cancel(id);

        // Assert
        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).containsExactly("Cannot cancel a transfer process in state " + state);
        verifyNoInteractions(manager);
    }

    @Test
    void cancel_whenNotFound() {
        // Act
        Result<?> result = service.cancel(id);

        // Assert
        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).containsExactly("Not found " + id);
    }

    public static List<TransferProcessStates> cancellableStates() {
        var states = new ArrayList<>(Arrays.asList(TransferProcessStates.values()));
        states.removeAll(nonCancellableStates());
        return states;
    }

    public static List<TransferProcessStates> nonCancellableStates() {
        return List.of(
                COMPLETED,
                ENDED,
                ERROR
        );
    }

    @ParameterizedTest
    @MethodSource("deprovisionableStates")
    void deprovision(TransferProcessStates state) {
        // Arrange
        var process = transferProcess(state);
        when(store.find(id)).thenReturn(process);

        // Act
        var result = service.deprovision(id);

        // Assert
        assertThat(result.succeeded()).isTrue();
        verify(manager).enqueueCommand(commandCaptor.capture());
        assertThat(commandCaptor.getValue()).isInstanceOf(DeprovisionRequest.class);
        assertThat(commandCaptor.getValue().getTransferProcessId())
                .isEqualTo(id);
    }

    @ParameterizedTest
    @MethodSource("nonDeprovisionableStates")
    void deprovision_whenNonDeprovisionable(TransferProcessStates state) {
        // Arrange
        var process = transferProcess(state);
        when(store.find(id)).thenReturn(process);

        // Act
        Result<?> result = service.deprovision(id);

        // Assert
        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).containsExactly("Cannot deprovision a transfer process in state " + state);
        verifyNoInteractions(manager);
    }

    @Test
    void deprovision_whenNotFound() {
        // Act
        Result<?> result = service.deprovision(id);

        // Assert
        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).containsExactly("Not found " + id);
    }

    public static List<TransferProcessStates> nonDeprovisionableStates() {
        var states = new ArrayList<>(Arrays.asList(TransferProcessStates.values()));
        states.removeAll(deprovisionableStates());
        return states;
    }

    public static List<TransferProcessStates> deprovisionableStates() {
        return List.of(
                COMPLETED,
                DEPROVISIONING,
                DEPROVISIONED,
                ENDED,
                CANCELLED
        );
    }

    private TransferProcess transferProcess() {
        return transferProcess(faker.options().option(TransferProcessStates.class));
    }

    private TransferProcess transferProcess(TransferProcessStates state) {
        return TransferProcess.Builder.newInstance()
                .state(state.code())
                .id(UUID.randomUUID().toString())
                .build();
    }
}