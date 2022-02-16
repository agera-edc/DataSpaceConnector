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

The blob storage library uses reactor-netty for network I/O. It is [supported by open telemetry](https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/022914139e0d7156e98efca382397663ed247bde/instrumentation/reactor/reactor-netty).
That's why we can see the HTTP calls corresponding to the blob copy.

![Jaeger screenshot blob copy](./jaeger-blob-copy.png)

The HTTP PUT corresponds to the [blob copy](https://docs.microsoft.com/en-us/rest/api/storageservices/copy-blob-from-url).  
The HTTP HEAD corresponds to the [get blob property](https://docs.microsoft.com/en-us/rest/api/storageservices/get-blob-properties). When the SyncPoller evaluates if the blob copy is finished by checking the blob properties.

## AZ copy (TODO if Java SDK not concluent)
