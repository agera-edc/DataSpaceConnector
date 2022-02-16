import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class BlobStorageManager {

    public static void main(String[] args) {
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
            new BlobStorageManager().copyBlob(sourceBlob, destBlob);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
        destBlobClient.beginCopy(source, Duration.ofSeconds(1L));

        var sourceBlobContent = new ByteArrayOutputStream();
        var destBlobContent = new ByteArrayOutputStream();
        sourceBlobClient.download(sourceBlobContent);
        destBlobClient.download(destBlobContent);
        assertEquals(sourceBlobContent.toString(), destBlobContent.toString());
    }
}
