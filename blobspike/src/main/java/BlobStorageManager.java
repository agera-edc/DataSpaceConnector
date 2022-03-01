import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobClientBuilder;
import com.azure.storage.blob.models.BlobRange;
import com.azure.storage.blob.models.Block;
import com.azure.storage.blob.models.BlockListType;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.azure.storage.blob.specialized.BlockBlobClient;
import io.opentelemetry.extension.annotations.WithSpan;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

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
            new BlobStorageManager().copyByBlock(sourceBlob, destBlob);

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
        var start = Instant.now();
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

        // destination blob client (consider using BlockBlobAsyncClient)
        BlockBlobClient destBlobClient = new BlobClientBuilder()
                .connectionString(destBlob.storageAccountConnectionString)
                .containerName(destBlob.containerName)
                .blobName(destBlob.blobName)
                .buildClient()
                .getBlockBlobClient();

        var sourceSize = sourceBlobClient.getProperties().getBlobSize();
        ArrayList<String> blockIds = new ArrayList<>();

        // this size is chosen because block sizes of 8 * 1024 * 1024=8388608 bytes are used by AzCopy
        var blockSize = 8 * 1024 * 1024;
        var blobRanges = buildRanges(sourceSize, blockSize);
        blobRanges.parallelStream()
                .forEach(range -> {
                    // BlockId of the destination block
                    var blockId = Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
                    destBlobClient.stageBlockFromUrl(blockId, sourceBlobUrl, range);
                    blockIds.add(blockId);
                });

        destBlobClient.commitBlockList(blockIds);
        var end = Instant.now().minusMillis(start.toEpochMilli());
        System.out.println("End: " + end.toEpochMilli() + "ms");
    }

    /*
    * Given the size of a blob, return the list of blob ranges to cut the blob into equal parts.
    * The last block can have a different size.
    * */
    private List<BlobRange> buildRanges(long blobSize, long blockSize) {
        var offset = 0;
        List<BlobRange> ranges = new ArrayList<>();
        while (offset < blobSize) {
            // Block size will be of size `blobSize - offset` if it is the last block of the blob.
            var currentBlockSize = Math.min(blockSize, blobSize - offset);
            ranges.add(new BlobRange(offset, currentBlockSize));
            offset += blockSize;
        }
        return ranges;
    }
}

