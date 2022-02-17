# Azure Blob Storage Copy

We evaluated the possibility to copy a blob.

## Copy with Java SDK

We evaluated the [Java azure storage client library](https://docs.microsoft.com/en-us/java/api/overview/azure/storage-blob-readme?view=azure-java-stable) to copy blobs.

### Evaluate if copy can be done with client not handling data flow

The blob client calls the [Blob service REST API](https://docs.microsoft.com/en-us/rest/api/storageservices/blob-service-rest-api) to [copy a blob](https://docs.microsoft.com/en-us/rest/api/storageservices/copy-blob-from-url).
The client does not need to download the blob. It only instructs the cloud to copy the blob from source to destination.
The client [get the blob properties](https://docs.microsoft.com/en-us/rest/api/storageservices/get-blob-properties) by calling the REST API in order to know if the copy operation is finished.

### Azure Blob Copy within same container

To copy Azure Blob within the same container using Azure Java SDK we can use BlobContainerClient.

```java

BlobContainerClient container = new BlobContainerClientBuilder()
                .connectionString(storageAccountConnectionString)
                .containerName(containerName)
                .buildClient();

BlobClient sourceBlobClient = container.getBlobClient(sourceBlobName);
BlobClient destBlobClient = container.getBlobClient(destinationBlobName);

```

Now using the destination and source blob clients we can trigger the copying:

```java

String source = sourceBlobClient.getBlobUrl();
// It will trigger a copy by calling the blob service REST API.
destBlobClient.beginCopy(source, Duration.ofSeconds(1L));
        
```

Method _beginCopy_ triggers an asynchronous operation of copying the data at the source URL to a blob.
To fetch the status of the operation we can use poller and poll for the status manually checking if it's complete.

```java
var syncPoller = destBlobClient.beginCopy(source, Duration.ofSeconds(1L));
PollResponse<BlobCopyInfo> response = syncPoller.poll();

while(!response.getStatus().isComplete()) {
    ...
    response = syncPoller.poll();
}
        
```
Alternatively, we can also block the thread waiting for the completion using _waitForCompletion_ method.

```java 

var syncPoller = destBlobClient.beginCopy(source, Duration.ofSeconds(1L));
// Wait for polling to complete.
syncPoller.waitForCompletion(Duration.ofSeconds(5));
        
```

### Azure Blob Copy within different containers/different storage accounts within the same tenant/subscription

To copy Azure Blob between containers within the same storage account as well as different storage accounts we can use BlobClients specifying their properties: 
```java

// source blob client
BlobClient sourceBlobClient = new BlobClientBuilder()
    .connectionString(sourceBlob.storageAccountConnectionString)
    .containerName(sourceBlob.containerName)
    .blobName(sourceBlob.blobName)
    .buildClient();

// destination blob client
BlobClient destBlobClient = new BlobClientBuilder()
    .connectionString(destBlob.storageAccountConnectionString)
    .containerName(destBlob.containerName)
    .blobName(destBlob.blobName)
    .buildClient();
```

### Azure Blob Copy between different Azure tenants/subscriptions

##### Copy between tenants using Sas token

Azure Blob can be copied between tenants using Java SDK. It requires creating a sas token to provide appropriate permissions to the blob.

```java

// set permissions for the blob
BlobSasPermission permission = new BlobSasPermission()
    .setReadPermission(true);
// define rule how long the permission is valid
BlobServiceSasSignatureValues sas = new BlobServiceSasSignatureValues(OffsetDateTime.now().plusDays(1), permission)
    .setStartTime(OffsetDateTime.now());
// generate sas token
String sasToken = sourceBlobClient.generateSas(sas);
        
String sourceBlobUrl = sourceBlobClient.getBlobUrl() + "?" + sasToken;

```

### Evaluate Integration testing approach (when actual storage instance is needed Azurite is preferred over cloud infra)

### Evaluate observability of copy-in-place operations and make sure it is traceable

The blob storage library uses reactor-netty for network I/O. It is [supported by open telemetry](https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/022914139e0d7156e98efca382397663ed247bde/instrumentation/reactor/reactor-netty).
That's why we can see the HTTP calls corresponding to the blob copy.

![Jaeger screenshot blob copy](./jaeger-blob-copy.png)

The HTTP PUT corresponds to the [blob copy](https://docs.microsoft.com/en-us/rest/api/storageservices/copy-blob-from-url).  
The HTTP HEAD corresponds to the [get blob property](https://docs.microsoft.com/en-us/rest/api/storageservices/get-blob-properties). When the SyncPoller evaluates if the blob copy is finished by checking the blob properties.

## AZ copy (TODO if Java SDK not concluent)
