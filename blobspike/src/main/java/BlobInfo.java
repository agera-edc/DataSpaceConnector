public class BlobInfo {
    String blobName;
    String containerName;
    String storageAccountConnectionString;

    public BlobInfo(String blobName, String containerName, String storageAccountConnectionString) {
        this.blobName = blobName;
        this.containerName = containerName;
        this.storageAccountConnectionString = storageAccountConnectionString;
    }
}