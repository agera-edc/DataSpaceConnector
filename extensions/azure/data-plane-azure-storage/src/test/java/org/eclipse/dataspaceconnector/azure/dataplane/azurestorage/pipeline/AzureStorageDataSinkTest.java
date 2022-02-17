package org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.pipeline;

import com.github.javafaker.Faker;
import org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.adapter.BlobAdapter;
import org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.adapter.BlobAdapterFactory;
import org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.schema.AzureBlobStoreSchema;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSource;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSource.Part;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.InputStreamDataSource;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.pipeline.AzureStorageTestFixtures.*;
import static org.mockito.Mockito.*;

class AzureStorageDataSinkTest {

    Monitor monitor = mock(Monitor.class);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    BlobAdapterFactory blobAdapterFactory = mock(BlobAdapterFactory.class);

    static Faker faker = new Faker();

    DataFlowRequest.Builder request = createRequest(AzureBlobStoreSchema.TYPE);

    String accountName = createAccountName();
    String containerName = createContainerName();
    String sharedKey = createSharedKey();
    String blobName = createBlobName();
    String content = faker.lorem().sentence();

    String errorMessage = faker.lorem().sentence();
    Exception e = new MyException(errorMessage);
    String eName = e.getClass().getName();

    AzureStorageDataSink dataSink = AzureStorageDataSink.Builder.newInstance()
            .accountName(accountName)
            .containerName(containerName)
            .sharedKey(sharedKey)
            .requestId(request.build().getId())
            .blobAdapterFactory(blobAdapterFactory)
            .executorService(executor)
            .monitor(monitor)
            .build();
    BlobAdapter destination = mock(BlobAdapter.class);
    ByteArrayOutputStream os = new ByteArrayOutputStream();

    @BeforeEach
    void setUp() {
        when(destination.getOutputStream()).thenReturn(os);
        when(blobAdapterFactory.getBlobAdapter(
                accountName,
                containerName,
                blobName,
                sharedKey))
                .thenReturn(destination);
    }

    @Test
    void transferParts() {
        var result = dataSink.transferParts(List.of(new InputStreamDataSource(blobName, new ByteArrayInputStream(content.getBytes(UTF_8)))));
        assertThat(result.succeeded()).isTrue();
        assertThat(os.toString(UTF_8)).isEqualTo(content);
    }


    @Test
    void transferParts_whenBlobClientCreationFails_fails() {
        when(blobAdapterFactory.getBlobAdapter(
                accountName,
                containerName,
                blobName,
                sharedKey))
                .thenThrow(e);
        var result = dataSink.transferParts(List.of(new InputStreamDataSource(blobName, new ByteArrayInputStream(content.getBytes(UTF_8)))));
        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).containsExactly("Error writing Azure Storage blob");
        verify(monitor).severe(format("Error creating blob for %s on account %s", blobName, accountName), e);
    }

    @Test
    void transferParts_whenWriteFails_fails() {
        when(destination.getOutputStream()).thenThrow(e);
        var result = dataSink.transferParts(List.of(new InputStreamDataSource(blobName, new ByteArrayInputStream(content.getBytes(UTF_8)))));
        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).containsExactly("Error writing Azure Storage blob");
        verify(monitor).severe(format("Error creating blob for %s on account %s", blobName, accountName), e);
    }


    @Test
    void transferParts_whenReadFails_fails() {
        when(destination.getOutputStream()).thenThrow(new RuntimeException(errorMessage));
        Part part = mock(Part.class);
        when(part.openStream()).thenThrow(e);
        when(part.name()).thenReturn(blobName);
        var result = dataSink.transferParts(List.of(part));
        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).containsExactly("Error reading Azure Storage blob");
        verify(monitor).severe(format("Error reading blob %s", blobName), e);
    }

    private static class MyException extends RuntimeException {
        MyException(String message) {
            super(message);
        }
    }
}