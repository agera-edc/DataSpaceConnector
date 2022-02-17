import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobClientBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import io.opentelemetry.extension.annotations.WithSpan;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class BlobStorageManager {

    public static void main(String[] args) {
        // See config.example.properties file.
        String propertiesFilePath = args[0];
        try (InputStream input = new FileInputStream(propertiesFilePath)) {
            Properties prop = new Properties();
            prop.load(input);

            String sourceConnectionString = prop.getProperty("source.connection.string");
            String destConnectionString = prop.getProperty("dest.connection.string");
            String sourceContainer = prop.getProperty("source.container.name");
            String destContainer = prop.getProperty("dest.container.name");
            String sourceBlobName = prop.getProperty("source.blob.name");
            String destBlobName = prop.getProperty("dest.blob.name");

            BlobInfo sourceBlob = new BlobInfo(sourceBlobName, sourceContainer, sourceConnectionString);
            BlobInfo destBlob = new BlobInfo(destBlobName, destContainer, destConnectionString);
            // new BlobStorageManager().copyBlobWithinContainer(sourceBlob, destBlobName);
            new BlobStorageManager().copyBlob(sourceBlob, destBlob);
            // new BlobStorageManager().copyBlobUsingSasToken(sourceBlob, destBlob);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @WithSpan("copy_blob")
    public void copyBlob(BlobInfo sourceBlob, BlobInfo destBlob) {
        BlobContainerClient sourceContainerClient = new BlobContainerClientBuilder()
                .connectionString(sourceBlob.storageAccountConnectionString)
                .containerName(sourceBlob.containerName)
                .buildClient();
        BlobContainerClient destContainerClient = new BlobContainerClientBuilder()
                .connectionString(destBlob.storageAccountConnectionString)
                .containerName(destBlob.containerName)
                .buildClient();

        BlobClient sourceBlobClient = sourceContainerClient.getBlobClient(sourceBlob.blobName);
        BlobClient destBlobClient = destContainerClient.getBlobClient(destBlob.blobName);
        String source = sourceBlobClient.getBlobUrl();
        // It will trigger a copy by calling the blob service REST API.
        var syncPoller = destBlobClient.beginCopy(source, Duration.ofSeconds(1L));
        // It will check the blob properties to make sure the copy is finished.
        syncPoller.waitForCompletion();

        var sourceBlobContent = new ByteArrayOutputStream();
        var destBlobContent = new ByteArrayOutputStream();
        sourceBlobClient.download(sourceBlobContent);
        destBlobClient.download(destBlobContent);
        assertEquals(sourceBlobContent.toString(), destBlobContent.toString());
    }

    public void copyBlobWithinContainer(BlobInfo sourceBlob, String destinationBlobName) {
        BlobContainerClient container = new BlobContainerClientBuilder()
                .connectionString(sourceBlob.storageAccountConnectionString)
                .containerName(sourceBlob.containerName)
                .buildClient();

        BlobClient sourceBlobClient = container.getBlobClient(sourceBlob.blobName);
        BlobClient destBlobClient = container.getBlobClient(destinationBlobName);
        String source = sourceBlobClient.getBlobUrl();

        // It will trigger a copy by calling the blob service REST API.
        var syncPoller = destBlobClient.beginCopy(source, Duration.ofSeconds(1L));

        // It will check the blob properties to make sure the copy is finished.
        syncPoller.waitForCompletion(Duration.ofSeconds(5));

        var sourceBlobContent = new ByteArrayOutputStream();
        var destBlobContent = new ByteArrayOutputStream();
        sourceBlobClient.download(sourceBlobContent);
        destBlobClient.download(destBlobContent);
        assertEquals(sourceBlobContent.toString(), destBlobContent.toString());
    }

    public void copyBlobUsingSasToken(BlobInfo sourceBlob, BlobInfo destBlob) {

        // source blob client
        BlobClient sourceBlobClient = new BlobClientBuilder()
                .connectionString(sourceBlob.storageAccountConnectionString)
                .containerName(sourceBlob.containerName)
                .blobName(sourceBlob.blobName)
                .buildClient();

        // set permissions for the blob
        BlobSasPermission permission = new BlobSasPermission()
                .setReadPermission(true);
        // define rule how long the permission is valid
        BlobServiceSasSignatureValues sas = new BlobServiceSasSignatureValues(OffsetDateTime.now().plusDays(1), permission)
                .setStartTime(OffsetDateTime.now());
        // generate sas token
        String sasToken = sourceBlobClient.generateSas(sas);

        String sourceBlobUrl = sourceBlobClient.getBlobUrl() + "?" + sasToken;

        // destination blob client
        BlobClient destBlobClient = new BlobClientBuilder()
                .connectionString(destBlob.storageAccountConnectionString)
                .containerName(destBlob.containerName)
                .blobName(destBlob.blobName)
                .buildClient();

        // start copying
        destBlobClient.beginCopy(sourceBlobUrl, Duration.ofSeconds(1L));

        // assert the blobs are the same
        var sourceBlobContent = new ByteArrayOutputStream();
        var destBlobContent = new ByteArrayOutputStream();
        sourceBlobClient.download(sourceBlobContent);
        destBlobClient.download(destBlobContent);
        assertEquals(sourceBlobContent.toString(), destBlobContent.toString());
    }

}

