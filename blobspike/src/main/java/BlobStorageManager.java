import com.azure.storage.blob.*;

import java.time.Duration;



public class BlobStorageManager {

    public static void main(String[] args) {
        String connectionString = ""; //connectionString
        BlobInfo sourceBlob = new BlobInfo("hello.txt", "container1", connectionString);
        BlobInfo destBlob = new BlobInfo("hello2.txt", "container2", connectionString);
        copyBlob(sourceBlob, destBlob);
    }

    public static void copyBlob(BlobInfo sourceBlob, BlobInfo destBlob) {
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
    }
}
