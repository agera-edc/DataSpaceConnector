# Azure Blob Storage In Place Copy

## Copy with Java SDK

We evaluated the [Java azure storage client library](https://docs.microsoft.com/en-us/java/api/overview/azure/storage-blob-readme?view=azure-java-stable) to copy blobs in place.

### Evaluate if copy is done in place

The blob client calls the [Blob service REST API](https://docs.microsoft.com/en-us/rest/api/storageservices/blob-service-rest-api) to [copy a blob](https://docs.microsoft.com/en-us/rest/api/storageservices/copy-blob-from-url).
The copy is done in place as the client does not need to download the blob.
The client [get the blob properties](https://docs.microsoft.com/en-us/rest/api/storageservices/get-blob-properties) by calling the REST API in order to know if the copy operation is finished.

### Evaluate if an Azure Blob can be copied in place between different Azure tenants/subscriptions

### Evaluate if an Azure Blob can be copied in place within same container

Yes it is possible.

### Evaluate if an Azure Blob can be copied in place within same storage account but different containers

Yes it is possible.

### Evaluate if an Azure Blob can be copied in place between different storage accounts within same Azure tenant/subscription

Yes it is possible.

### Evaluate Integration testing approach (when actual storage instance is needed Azurite is preferred over cloud infra)

### Evaluate observability of copy-in-place operations and make sure it is traceable

## AZ copy (TODO if Java SDK not concluent)
