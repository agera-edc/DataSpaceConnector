### Application runtime settings ###

output "EDC_AZURE_TENANTID" {
  value       = data.azurerm_client_config.current.tenant_id
  description = "Azure Active Directory Tenant ID for the GitHub workflow identity."
}

output "EDC_AZURE_SUBSCRIPTIONID" {
  value       = data.azurerm_client_config.current.subscription_id
  description = "Azure Subscription ID in which cloud resources are deployed."
}

output "EDC_DATAFACTORY_RESOURCEID" {
  value       = azurerm_data_factory.main.id
  description = "Resource ID of the Azure Data Factory deployed for tests."
}

output "EDC_DATAFACTORY_KEYVAULT_RESOURCEID" {
  value       = azurerm_key_vault.main.id
  description = "Resource ID of the Azure Key Vault connected to the Data Factory."
}

output "EDC_DATAFACTORY_KEYVAULT_LINKEDSERVICENAME" {
  value       = azurerm_data_factory_linked_service_key_vault.main.name
  description = "Name of the Data Factory linked service representing the connected Key Vault."
}

### Values for CI scripts ###

output "ci_client_id" {
  value        = var.ci_client_id
  description = "Application ID (Client ID) of the GitHub workflow that runs the CI job and needs access to cloud resources."
}

### Integration test configuration values ###

output "test_provider_storage_resourceid" {
  value       = azurerm_storage_account.provider.id
  description = "Resource ID of the Azure Storage account deployed for holding provider data in tests."
}

output "test_consumer_storage_resourceid" {
  value       = azurerm_storage_account.consumer.id
  description = "Resource ID of the Azure Storage account deployed for holding consumer data in tests."
}
