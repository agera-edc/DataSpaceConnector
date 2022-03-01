import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobClientBuilder;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Fail.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class BlobStorageManagerIntegrationTest {

    protected List<Runnable> containerCleanup = new ArrayList<>();

    @AfterEach
    public void teardown() {
        for (var cleanup : containerCleanup) {
            try {
                cleanup.run();
            } catch (Exception ex) {
                fail("teardown failed, subsequent tests might fail as well!");
            }
        }
    }

    protected String getConnectionString(String accountName, String key) {
        return String.format("DefaultEndpointsProtocol=http;AccountName=%s;AccountKey=%s;BlobEndpoint=http://127.0.0.1:10000/%s;QueueEndpoint=http://127.0.0.1:10001/%s;", accountName, key, accountName, accountName);
    }

    @Test
    public void blobStorageManagerTest() {
        var connectionString1 = getConnectionString("account1", "key1");
        var connectionString2 = getConnectionString("account2", "key2");
        BlobInfo sourceBlob = new BlobInfo("blob1", "container1", connectionString1);
        BlobInfo destBlob = new BlobInfo("blob2", "container2", connectionString2);

        createContainer(sourceBlob.containerName, connectionString1);
        createContainer(destBlob.containerName, connectionString2);

        BlobClient sourceBlobClient = new BlobClientBuilder()
                .connectionString(connectionString1)
                .containerName(sourceBlob.containerName)
                .blobName(sourceBlob.blobName)
                .buildClient();

        BlobClient destBlobClient = new BlobClientBuilder()
                .connectionString(connectionString2)
                .containerName(destBlob.containerName)
                .blobName(destBlob.blobName)
                .buildClient();

        File f = new File("source_file_for_integration_tests");
        sourceBlobClient.uploadFromFile(f.getAbsolutePath());
        new BlobStorageManager().copyBlobUsingSasToken(sourceBlob, destBlob);

        // assert the blobs are the same
        var sourceBlobContent = new ByteArrayOutputStream();
        var destBlobContent = new ByteArrayOutputStream();
        sourceBlobClient.download(sourceBlobContent);
        destBlobClient.download(destBlobContent);
        assertEquals(sourceBlobContent.toString(), destBlobContent.toString());
    }

    public void createContainer(String containerName, String connectionString) {
        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder().connectionString(connectionString).buildClient();
        blobServiceClient.createBlobContainer(containerName);
        containerCleanup.add(() -> blobServiceClient.deleteBlobContainer(containerName));
    }
}
