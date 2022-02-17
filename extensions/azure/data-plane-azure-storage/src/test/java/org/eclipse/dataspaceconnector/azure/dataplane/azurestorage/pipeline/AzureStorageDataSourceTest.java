package org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.pipeline;

import com.github.javafaker.Faker;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.adapter.BlobAdapter;
import org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.adapter.BlobAdapterFactory;
import org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.schema.AzureBlobStoreSchema;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.InputStreamDataSource;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.pipeline.AzureStorageTestFixtures.*;
import static org.mockito.Mockito.*;

class AzureStorageDataSourceTest {

    Monitor monitor = mock(Monitor.class);
    BlobAdapterFactory blobAdapterFactory = mock(BlobAdapterFactory.class);

    static Faker faker = new Faker();

    DataFlowRequest.Builder request = createRequest(AzureBlobStoreSchema.TYPE);

    String accountName = createAccountName();
    String containerName = createContainerName();
    String sharedKey = createSharedKey();
    String blobName = createBlobName();
    String content = faker.lorem().sentence();

    Exception exception = new ACustomException(faker.lorem().sentence());

    AzureStorageDataSource dataSource = AzureStorageDataSource.Builder.newInstance()
            .accountName(accountName)
            .containerName(containerName)
            .blobName(blobName)
            .sharedKey(sharedKey)
            .requestId(request.build().getId())
            .retryPolicy(new RetryPolicy<>())
            .blobAdapterFactory(blobAdapterFactory)
            .monitor(monitor)
            .build();
    BlobAdapter destination = mock(BlobAdapter.class);
    ByteArrayInputStream input = new ByteArrayInputStream(content.getBytes(UTF_8));

    @BeforeEach
    void setUp() {
        when(destination.openInputStream()).thenReturn(input);
        when(blobAdapterFactory.getBlobAdapter(
                accountName,
                containerName,
                blobName,
                sharedKey))
                .thenReturn(destination);
    }

    @Test
    void openPartStream_succeeds() {
        var result = dataSource.openPartStream();
        assertThat(result).map(s -> s.openStream()).containsExactly(input);
    }

    @Test
    void openPartStream_whenBlobClientCreationFails_fails() {
        when(blobAdapterFactory.getBlobAdapter(
                accountName,
                containerName,
                blobName,
                sharedKey))
                .thenThrow(exception);

        assertThatExceptionOfType(EdcException.class)
                .isThrownBy(() -> dataSource.openPartStream())
                .withCause(exception);
        verify(monitor).severe(format("Error accessing blob %s on account %s", blobName, accountName), exception);
    }
}