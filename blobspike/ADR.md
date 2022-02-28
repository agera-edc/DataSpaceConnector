# Server side copy extension

## Copy with Java SDK

The optimal strategy to copy blobs on server side is to copy the blob by blocks in parallel.
The size of the block is configurable. The parallelization is not handled by the client but should be handled the logic on top of it.
In order to achieve that, call [stageBlockFromUrl](https://docs.microsoft.com/en-us/java/api/com.azure.storage.blob.specialized.blockblobclient.stageblockfromurl?view=azure-java-stable) for each block to copy.
Once blocks are successfully written, call [commitBlockList](https://docs.microsoft.com/en-us/java/api/com.azure.storage.blob.specialized.blockblobclient.commitblocklist?view=azure-java-stable).
This will create the blob composed ot the blocks.
Note that a Java data movement library is in design process now. This solution is proposed because the library is not available now.

For example:
```java
var sourceSize = sourceBlobClient.getProperties().getBlobSize();
ArrayList<String> blockIds = new ArrayList<>();

var blockSize = 8 * 1024 * 1024;

var blobRanges = buildRanges(sourceSize, blockSize);
blobRanges.parallelStream()
        .forEach(range -> {
            // BlockId of the destination block
            var blockId = Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
            destBlobClient.stageBlockFromUrl(blockId, sourceBlobUrl, range);
        });

destBlobClient.commitBlockList(blockIds);
```