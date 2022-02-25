import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobClientBuilder;
import com.azure.storage.blob.models.BlobRange;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.azure.storage.blob.specialized.BlockBlobClient;
import io.opentelemetry.extension.annotations.WithSpan;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;



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
            // new BlobStorageManager().copyBlobUsingSasToken(sourceBlob, destBlob);
            new BlobStorageManager().copyByBlock2(sourceBlob, destBlob);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @WithSpan("copy_blob")
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

        // start copying. It will trigger a copy by calling the blob service REST API.
        var syncPoller = destBlobClient.beginCopy(sourceBlobUrl, Duration.ofSeconds(1L));
        // wait for the copy to finish. It will check the blob properties to make sure the copy is finished.
        syncPoller.waitForCompletion();
    }

    @WithSpan("copy by blocks")
    public void copyByBlock(BlobInfo sourceBlob, BlobInfo destBlob) {
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
        BlockBlobClient destBlobClient = new BlobClientBuilder()
                .connectionString(destBlob.storageAccountConnectionString)
                .containerName(destBlob.containerName)
                .blobName(destBlob.blobName)
                .buildClient()
                .getBlockBlobClient();

        var sourceSize = sourceBlobClient.getProperties().getBlobSize();
        ArrayList<String> blockIds = new ArrayList<>();

        var blockSize = 8 * 1024 * 1024;
        buildRanges(sourceSize, blockSize).parallelStream()
                .forEach(range -> {
                    var blockId = Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
                    destBlobClient.stageBlockFromUrl(blockId, sourceBlobUrl, range);
                });

        destBlobClient.commitBlockList(blockIds);
    }

    private List<BlobRange> buildRanges(long blobSize, long blockSize) {
        var offset = 0;
        List<BlobRange> ranges = new ArrayList<>();
        while (offset < blobSize) {
            ranges.add(new BlobRange(offset, Math.min(blockSize, blobSize - offset)));
            offset += blockSize;
        }
        return ranges;
    }
}

