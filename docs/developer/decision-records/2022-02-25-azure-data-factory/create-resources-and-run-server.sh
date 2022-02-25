#!/bin/bash

set -euxo pipefail

test -s secrets || az ad sp create-for-rbac --skip-assignment > secrets

appId=$(jq -r .appId secrets)
tenant=$(jq -r .tenant secrets)
password=$(jq -r .password secrets)

subscriptionId=9d236f09-93d9-4f41-88a6-20201a6a1abc
rg=adfspike
kv=ageraspikeadfvault
adf=ageraspikeadfa
provstore=edcproviderstore
consstore=edcconsumerstore
location=EastUS


az account set -s $subscriptionId
az group create --name $rg -l "$location"

az storage account create --name $provstore --resource-group $rg
az storage account create --name $consstore --resource-group $rg

az datafactory create --location "$location" --name "$adf" --resource-group "$rg"

az datafactory linked-service create --factory-name "$adf" --properties "{\"type\":\"AzureKeyVault\",\"typeProperties\":{\"baseUrl\":\"https://$kv.vault.azure.net/\"}}" --name "AzureKeyVault1" --resource-group "$rg"


adfId=$(az datafactory show --name $adf --resource-group $rg --query identity.principalId -o tsv)


az keyvault show --name $kv --resource-group $rg || az keyvault create --name $kv --resource-group $rg --location "$location" --enable-rbac-authorization true


az role assignment create --assignee "$adfId" \
  --role "Key Vault Secrets User" \
  --scope "/subscriptions/$subscriptionId/resourcegroups/$rg/providers/Microsoft.KeyVault/vaults/$kv"

az role assignment create --assignee "$appId" \
  --role "Key Vault Secrets Officer" \
  --scope "/subscriptions/$subscriptionId/resourcegroups/$rg/providers/Microsoft.KeyVault/vaults/$kv"

export AZURE_CLIENT_ID=$appId
export AZURE_TENANT_ID=$tenant
export AZURE_CLIENT_SECRET=$password
export WEB_HTTP_PUBLIC_PORT=9190
export WEB_HTTP_CONTROL_PATH=/control
./gradlew :launchers:data-plane-server:run
