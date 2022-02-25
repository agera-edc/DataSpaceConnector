# Azure Data Factory

## Decision

Azure Data Factory is used as an additional mechanism to transfer data in DPF.

The Data Plane Framework uses a routing mechanism to either manage data transfer by itself (using pluggable sources and sinks), or delegate transfer to a pluggable service such as an extension for performing transfers with Azure Data Factory.

## Rationale

...

## Spike

Run the following script to deploy Azure resources. Resource names can be adapted.

```bash
docs/developer/decision-records/2022-02-25-azure-data-factory/create-resources-and-run-server.sh
```

Once the DPF server is running, all resources have been deployed.

You can then run integration test `org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.AzureDataFactoryCopyIntegrationTest`.

In Azure portal, navigate to the data factory instance and open Data Factory Studio. In the Monitor tab, you can view details for the run.

![](/Users/algattik/IdeaProjects/DataSpaceConnector/docs/developer/decision-records/2022-02-25-azure-data-factory/adf-run-details.png)



You can also use DPF server that is run by the script, to start a transfer between any storage accounts with curl or Postman.

```
curl http://localhost:8181/control/transfer -H "Content-Type: application/json" --data '{
    "id": "9033DE3C-711A-4803-8AB8-489AC795D82D",
    "edctype": "dataspaceconnector:dataflowrequest",
    "processId": "1647468C-2A0F-4DB1-B45C-08DB3EAF7AD9",
    "sourceDataAddress": {
        "properties": {
            "account": "<ACCOUNTNAME>",
            "type": "AzureStorageBlobData",
            "container": "<CONTAINERNAME>",
            "blob": "<BLOBNAME>",
            "sharedKey": "<SHAREDKEY>"
        }
    },
    "destinationDataAddress": {
        "properties": {
            "account": "<ACCOUNTNAME>",
            "type": "AzureStorageBlobData",
            "container": "<CONTAINERNAME>",
            "sharedKey": "<SHAREDKEY>"
        }
    }
}'

```



