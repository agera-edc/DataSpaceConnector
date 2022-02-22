package org.eclipse.dataspaceconnector.dataplane.spi.pipeline;

import com.github.javafaker.Faker;
import org.eclipse.dataspaceconnector.dataplane.spi.result.TransferResult;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.response.ResponseStatus;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ParallelSinkTest {

    Faker faker = new Faker();
    Monitor monitor = mock(Monitor.class);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    String requestId = UUID.randomUUID().toString();

    @Test
    void transfer() {
        var fakeSink = FakeParallelSink.Builder.newInstance()
                .monitor(monitor)
                .executorService(executor)
                .requestId(requestId)
                .build();
        var dataSource = new InputStreamDataSource(faker.lorem().word(), new ByteArrayInputStream(faker.lorem().characters().getBytes()));

        assertThat(fakeSink.transfer(dataSource)).succeedsWithin(500, TimeUnit.MILLISECONDS)
                .satisfies(transferResult -> assertThat(transferResult.succeeded()).isTrue());

        assertThat(fakeSink.parts).containsExactly(dataSource);
    }

    @Test
    void transfer_whenExceptionOpeningPartStream() {
        var fakeSink = FakeParallelSink.Builder.newInstance()
                .monitor(monitor)
                .executorService(executor)
                .requestId(requestId)
                .build();
        var dataSourceMock = mock(DataSource.class);

        when(dataSourceMock.openPartStream()).thenThrow(new RuntimeException(faker.lorem().sentence()));

        assertThat(fakeSink.transfer(dataSourceMock)).succeedsWithin(500, TimeUnit.MILLISECONDS)
            .satisfies(transferResult -> assertThat(transferResult.failed()).isTrue())
            .satisfies(transferResult -> assertThat(transferResult.getFailureMessages()).containsExactly("Error processing data transfer request"));

        assertThat(fakeSink.parts).isNull();
    }

    @Test
    void transfer_whenFailureDuringTransfer() {
        var errorMessage = faker.lorem().sentence();
        var fakeSink = FakeParallelSink.Builder.newInstance()
                .monitor(monitor)
                .executorService(executor)
                .requestId(requestId)
                .transferResultSupplier(() -> TransferResult.failure(ResponseStatus.FATAL_ERROR, errorMessage))
                .build();
        var dataSource = new InputStreamDataSource(faker.lorem().word(), new ByteArrayInputStream(faker.lorem().characters().getBytes()));

        assertThat(fakeSink.transfer(dataSource)).succeedsWithin(500, TimeUnit.MILLISECONDS)
            .satisfies(transferResult -> assertThat(transferResult.failed()).isTrue())
            .satisfies(transferResult -> assertThat(transferResult.getFailureMessages()).containsExactly(errorMessage));

        assertThat(fakeSink.parts).containsExactly(dataSource);
    }

    @Test
    void transfer_whenExceptionDuringTransfer() {
        var errorMessage = faker.lorem().sentence();
        var fakeSink = FakeParallelSink.Builder.newInstance()
                .monitor(monitor)
                .executorService(executor)
                .requestId(requestId)
                .transferResultSupplier(() -> { throw new RuntimeException(errorMessage); } )
                .build();
        var dataSource = new InputStreamDataSource(faker.lorem().word(), new ByteArrayInputStream(faker.lorem().characters().getBytes()));

        assertThat(fakeSink.transfer(dataSource)).succeedsWithin(500, TimeUnit.MILLISECONDS)
                .satisfies(transferResult -> assertThat(transferResult.failed()).isTrue())
                .satisfies(transferResult -> assertThat(transferResult.getFailureMessages())
                        .containsExactly("Unhandled exception raised when transferring data: java.lang.RuntimeException: " + errorMessage));

        assertThat(fakeSink.parts).containsExactly(dataSource);
    }

    private static class FakeParallelSink extends ParallelSink {

        List<DataSource.Part> parts;
        Supplier<TransferResult> transferResultSupplier = TransferResult::success;

        @Override
        protected TransferResult transferParts(List<DataSource.Part> parts) {
            this.parts = parts;
            return transferResultSupplier.get();
        }

        public static class Builder extends ParallelSink.Builder<Builder, FakeParallelSink> {

            public static Builder newInstance() {
                Builder builder = new Builder();
                return builder;
            }

            public Builder transferResultSupplier(Supplier<TransferResult> transferResultSupplier) {
                sink.transferResultSupplier = transferResultSupplier;
                return this;
            }

            protected void validate() {
            }

            private Builder() {
                super(new FakeParallelSink());
            }
        }
    }
}