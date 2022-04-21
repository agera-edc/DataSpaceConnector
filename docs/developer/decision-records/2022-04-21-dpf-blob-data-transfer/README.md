# Blob storage transfer

ADR describing the blob storage transfer end to end flow between 2 participants.

## Sequence diagram
The following sequence diagram describes the blob transfer flow between 2 participants, with managedResources=true.
It starts from the client triggering the transfer on the consumer side and finishes when the consumer deletes the blob
after the client triggered the data deletion.

![blob-transfer](../../../diagrams/blob-transfer.png)

1- The client calls the data management API to trigger a transfer process. 
managedResources is set to true, it means that the consumer should provision the blob container.
2- Consumer get the destination storage account access key in its Vault.
3- Consumer create a container where the Provider DPF may write blobs. The container is created only if the client specifies managedResources=true.
4- Consumer sends an IDS message to the provider, to provide the information needed to write the blob: for example the blob name and the SAS token needed to write it.
5- Provider store the SAS token in its Vault.
6- Provider request the blob transfer on the Provider DPF. The provider DPF can be embedded or run in a separated runtime. If it runs on a separated runtime, provider request the transfer via an HTTP request.
7- The Provider DPF get the source storage account key.
8- The Provider DPF get the SAS token needed to write the blob to the consumer blob container.
9- The Provider DPF reads the data that needs to be transfered.
10- The Provider DPF writes the data to the destination blob so that the consumer can access the data.
11- In the meantime, the client polls regularly to check if the data transfer is finished.
12- When the datatransfer is finished, the client can read the blob.
13- Then, the client can call the Data Management API to destroy the data.
14- Consumer deletes the container containing the blob.
