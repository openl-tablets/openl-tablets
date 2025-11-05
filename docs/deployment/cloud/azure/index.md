# Azure Deployment

Deploy OpenL Tablets on Microsoft Azure using AKS, Container Instances, or Virtual Machines.

## Deployment Options

| Option | Use Case | Complexity | Scalability |
|--------|----------|------------|-------------|
| **AKS (Kubernetes)** | Production, enterprise | Medium | High (auto-scaling) |
| **Container Instances** | Serverless containers | Low | Low-Medium |
| **App Service** | PaaS deployment | Low | Medium |
| **Virtual Machines** | Traditional deployment | Low-Medium | Medium (manual) |

---

## Option 1: Azure Kubernetes Service (Recommended)

Deploy OpenL Tablets on Azure Kubernetes Service for production workloads.

### Prerequisites

```bash
# Install Azure CLI
az --version

# Install kubectl
kubectl version --client

# Install Helm
helm version

# Login to Azure
az login

# Set subscription
az account set --subscription <subscription-id>
```

### Create AKS Cluster

```bash
# Create resource group
az group create \
  --name openl-rg \
  --location eastus

# Create AKS cluster
az aks create \
  --resource-group openl-rg \
  --name openl-cluster \
  --node-count 3 \
  --node-vm-size Standard_D4s_v3 \
  --enable-managed-identity \
  --enable-addons monitoring \
  --generate-ssh-keys

# Get credentials
az aks get-credentials \
  --resource-group openl-rg \
  --name openl-cluster

# Verify cluster
kubectl get nodes
```

### Deploy OpenL Tablets

```bash
# Add Helm repository
helm repo add openl-tablets https://openl-tablets.github.io/helm-charts
helm repo update

# Create namespace
kubectl create namespace openl

# Install with Helm
helm install openl-tablets openl-tablets/openl-tablets \
  --namespace openl \
  --set studio.replicaCount=3 \
  --set ruleservices.replicaCount=3 \
  --set postgresql.enabled=false \
  --set externalDatabase.host=<AZURE-POSTGRESQL-HOST> \
  --set redis.enabled=false \
  --set externalRedis.host=<AZURE-REDIS-HOST>
```

### Configure Azure Services

#### Azure Database for PostgreSQL

```bash
# Create PostgreSQL Flexible Server
az postgres flexible-server create \
  --resource-group openl-rg \
  --name openl-postgres \
  --location eastus \
  --admin-user openl \
  --admin-password <password> \
  --sku-name Standard_D4s_v3 \
  --tier GeneralPurpose \
  --storage-size 128 \
  --version 16 \
  --high-availability Enabled \
  --zone 1 \
  --standby-zone 2

# Create database
az postgres flexible-server db create \
  --resource-group openl-rg \
  --server-name openl-postgres \
  --database-name openl_studio

# Configure firewall (allow AKS)
az postgres flexible-server firewall-rule create \
  --resource-group openl-rg \
  --name openl-postgres \
  --rule-name AllowAKS \
  --start-ip-address <aks-outbound-ip> \
  --end-ip-address <aks-outbound-ip>
```

#### Azure Cache for Redis

```bash
# Create Redis cache
az redis create \
  --resource-group openl-rg \
  --name openl-redis \
  --location eastus \
  --sku Premium \
  --vm-size P1 \
  --enable-non-ssl-port false \
  --minimum-tls-version 1.2 \
  --redis-version 6

# Enable clustering for high availability
az redis patch-schedule create \
  --resource-group openl-rg \
  --name openl-redis \
  --schedule-entries dayOfWeek=Sunday startHourUtc=2 maintenanceWindow=PT5H
```

#### Azure Files (Shared Storage)

```bash
# Create storage account
az storage account create \
  --resource-group openl-rg \
  --name openlstorage \
  --location eastus \
  --sku Standard_LRS \
  --kind StorageV2

# Create file share
az storage share create \
  --account-name openlstorage \
  --name openl-workspace \
  --quota 100

# Get storage key
STORAGE_KEY=$(az storage account keys list \
  --resource-group openl-rg \
  --account-name openlstorage \
  --query "[0].value" -o tsv)
```

### Storage Class for Azure Files

```yaml
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: azurefile
provisioner: file.csi.azure.com
parameters:
  skuName: Premium_LRS
  storageAccount: openlstorage
mountOptions:
  - dir_mode=0777
  - file_mode=0777
  - uid=0
  - gid=0
  - mfsymlinks
  - cache=strict
  - actimeo=30
```

### Ingress with Application Gateway

```bash
# Enable Application Gateway Ingress Controller
az aks enable-addons \
  --resource-group openl-rg \
  --name openl-cluster \
  --addon ingress-appgw \
  --appgw-name openl-appgw \
  --appgw-subnet-cidr "10.2.0.0/16"
```

Create Ingress:

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: openl-ingress
  namespace: openl
  annotations:
    kubernetes.io/ingress.class: azure/application-gateway
    appgw.ingress.kubernetes.io/ssl-redirect: "true"
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
spec:
  tls:
    - hosts:
        - openl.example.com
      secretName: openl-tls-secret
  rules:
    - host: openl.example.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: openl-studio
                port:
                  number: 8080
          - path: /ruleservices
            pathType: Prefix
            backend:
              service:
                name: openl-ruleservices
                port:
                  number: 8080
```

---

## Option 2: Azure Container Instances

Serverless container deployment with Azure Container Instances.

### Create Container Group

```yaml
# container-group.yaml
apiVersion: '2021-09-01'
location: eastus
name: openl-studio
properties:
  containers:
    - name: studio
      properties:
        image: openltablets/studio:latest
        resources:
          requests:
            cpu: 2
            memoryInGB: 4
        ports:
          - port: 8080
            protocol: TCP
        environmentVariables:
          - name: DATABASE_URL
            value: postgresql://...
          - name: SECURITY_MODE
            value: multi-user
          - name: ADMIN_PASSWORD
            secureValue: <password>
  osType: Linux
  restartPolicy: Always
  ipAddress:
    type: Public
    ports:
      - protocol: TCP
        port: 8080
    dnsNameLabel: openl-studio
tags:
  environment: production
type: Microsoft.ContainerInstance/containerGroups
```

Deploy:

```bash
az container create \
  --resource-group openl-rg \
  --file container-group.yaml
```

---

## Option 3: Azure App Service

PaaS deployment using Azure App Service for Containers.

### Create App Service Plan

```bash
# Create App Service Plan
az appservice plan create \
  --resource-group openl-rg \
  --name openl-plan \
  --location eastus \
  --is-linux \
  --sku P1V3

# Create Web App
az webapp create \
  --resource-group openl-rg \
  --plan openl-plan \
  --name openl-studio \
  --deployment-container-image-name openltablets/studio:latest

# Configure environment variables
az webapp config appsettings set \
  --resource-group openl-rg \
  --name openl-studio \
  --settings \
    DATABASE_URL="postgresql://..." \
    SECURITY_MODE="multi-user" \
    ADMIN_PASSWORD="<password>"

# Enable logging
az webapp log config \
  --resource-group openl-rg \
  --name openl-studio \
  --docker-container-logging filesystem
```

---

## Option 4: Virtual Machines with Docker

Traditional VM-based deployment.

### Create Virtual Machine

```bash
# Create VM
az vm create \
  --resource-group openl-rg \
  --name openl-vm \
  --image Ubuntu2204 \
  --size Standard_D4s_v3 \
  --admin-username azureuser \
  --generate-ssh-keys \
  --public-ip-address-allocation static \
  --public-ip-sku Standard

# Open port 80 and 443
az vm open-port \
  --resource-group openl-rg \
  --name openl-vm \
  --port 80 \
  --priority 1000

az vm open-port \
  --resource-group openl-rg \
  --name openl-vm \
  --port 443 \
  --priority 1001
```

### Install Docker

```bash
# SSH into VM
ssh azureuser@<vm-public-ip>

# Install Docker
sudo apt-get update
sudo apt-get install -y docker.io docker-compose
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker $USER

# Deploy with Docker Compose
curl -O https://raw.githubusercontent.com/openl-tablets/openl-tablets/master/docs/deployment/docker/docker-compose-multi.yaml
docker-compose -f docker-compose-multi.yaml up -d
```

---

## Infrastructure as Code

### Terraform Configuration

Create `main.tf`:

```hcl
terraform {
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 3.0"
    }
  }
}

provider "azurerm" {
  features {}
}

# Resource Group
resource "azurerm_resource_group" "openl" {
  name     = "openl-rg"
  location = "East US"
}

# Virtual Network
resource "azurerm_virtual_network" "openl" {
  name                = "openl-vnet"
  address_space       = ["10.0.0.0/16"]
  location            = azurerm_resource_group.openl.location
  resource_group_name = azurerm_resource_group.openl.name
}

resource "azurerm_subnet" "aks" {
  name                 = "aks-subnet"
  resource_group_name  = azurerm_resource_group.openl.name
  virtual_network_name = azurerm_virtual_network.openl.name
  address_prefixes     = ["10.0.1.0/24"]
}

# AKS Cluster
resource "azurerm_kubernetes_cluster" "openl" {
  name                = "openl-cluster"
  location            = azurerm_resource_group.openl.location
  resource_group_name = azurerm_resource_group.openl.name
  dns_prefix          = "openl"

  default_node_pool {
    name           = "default"
    node_count     = 3
    vm_size        = "Standard_D4s_v3"
    vnet_subnet_id = azurerm_subnet.aks.id
  }

  identity {
    type = "SystemAssigned"
  }

  network_profile {
    network_plugin = "azure"
    network_policy = "calico"
  }

  tags = {
    Environment = "Production"
  }
}

# PostgreSQL Flexible Server
resource "azurerm_postgresql_flexible_server" "openl" {
  name                   = "openl-postgres"
  resource_group_name    = azurerm_resource_group.openl.name
  location               = azurerm_resource_group.openl.location
  version                = "16"
  administrator_login    = "openl"
  administrator_password = var.db_password
  storage_mb             = 131072
  sku_name               = "GP_Standard_D4s_v3"

  high_availability {
    mode                      = "ZoneRedundant"
    standby_availability_zone = "2"
  }

  backup_retention_days = 30
}

resource "azurerm_postgresql_flexible_server_database" "openl" {
  name      = "openl_studio"
  server_id = azurerm_postgresql_flexible_server.openl.id
  collation = "en_US.utf8"
  charset   = "utf8"
}

# Azure Cache for Redis
resource "azurerm_redis_cache" "openl" {
  name                = "openl-redis"
  location            = azurerm_resource_group.openl.location
  resource_group_name = azurerm_resource_group.openl.name
  capacity            = 1
  family              = "P"
  sku_name            = "Premium"
  enable_non_ssl_port = false
  minimum_tls_version = "1.2"

  redis_configuration {
    maxmemory_policy = "allkeys-lru"
  }
}

# Storage Account
resource "azurerm_storage_account" "openl" {
  name                     = "openlstorage"
  resource_group_name      = azurerm_resource_group.openl.name
  location                 = azurerm_resource_group.openl.location
  account_tier             = "Standard"
  account_replication_type = "LRS"
  account_kind             = "StorageV2"
}

resource "azurerm_storage_share" "openl" {
  name                 = "openl-workspace"
  storage_account_name = azurerm_storage_account.openl.name
  quota                = 100
}

# Application Gateway
resource "azurerm_application_gateway" "openl" {
  name                = "openl-appgw"
  resource_group_name = azurerm_resource_group.openl.name
  location            = azurerm_resource_group.openl.location

  sku {
    name     = "Standard_v2"
    tier     = "Standard_v2"
    capacity = 2
  }

  gateway_ip_configuration {
    name      = "gateway-ip-config"
    subnet_id = azurerm_subnet.appgw.id
  }

  # Additional configuration...
}
```

### Deploy with Terraform

```bash
# Initialize Terraform
terraform init

# Plan deployment
terraform plan -out=tfplan

# Apply configuration
terraform apply tfplan
```

### ARM Templates

Create `azuredeploy.json`:

```json
{
  "$schema": "https://schema.management.azure.com/schemas/2019-04-01/deploymentTemplate.json#",
  "contentVersion": "1.0.0.0",
  "parameters": {
    "clusterName": {
      "type": "string",
      "defaultValue": "openl-cluster"
    },
    "nodeCount": {
      "type": "int",
      "defaultValue": 3
    }
  },
  "resources": [
    {
      "type": "Microsoft.ContainerService/managedClusters",
      "apiVersion": "2023-05-01",
      "name": "[parameters('clusterName')]",
      "location": "[resourceGroup().location]",
      "identity": {
        "type": "SystemAssigned"
      },
      "properties": {
        "dnsPrefix": "[parameters('clusterName')]",
        "agentPoolProfiles": [
          {
            "name": "agentpool",
            "count": "[parameters('nodeCount')]",
            "vmSize": "Standard_D4s_v3",
            "mode": "System"
          }
        ],
        "networkProfile": {
          "networkPlugin": "azure",
          "networkPolicy": "calico"
        }
      }
    }
  ]
}
```

Deploy:

```bash
az deployment group create \
  --resource-group openl-rg \
  --template-file azuredeploy.json \
  --parameters clusterName=openl-cluster nodeCount=3
```

---

## Monitoring and Observability

### Azure Monitor

```bash
# Enable Container Insights
az aks enable-addons \
  --resource-group openl-rg \
  --name openl-cluster \
  --addons monitoring

# Create Log Analytics workspace
az monitor log-analytics workspace create \
  --resource-group openl-rg \
  --workspace-name openl-workspace
```

### Application Insights

```bash
# Create Application Insights
az monitor app-insights component create \
  --resource-group openl-rg \
  --app openl-insights \
  --location eastus \
  --kind web

# Get instrumentation key
INSTRUMENTATION_KEY=$(az monitor app-insights component show \
  --resource-group openl-rg \
  --app openl-insights \
  --query instrumentationKey -o tsv)
```

Add to application:

```yaml
env:
  - name: APPLICATIONINSIGHTS_CONNECTION_STRING
    value: "InstrumentationKey=<key>;..."
```

---

## Security Best Practices

### Azure Key Vault

```bash
# Create Key Vault
az keyvault create \
  --resource-group openl-rg \
  --name openl-vault \
  --location eastus

# Store secrets
az keyvault secret set \
  --vault-name openl-vault \
  --name admin-password \
  --value "your-secure-password"

# Enable AKS to access Key Vault
az aks enable-addons \
  --resource-group openl-rg \
  --name openl-cluster \
  --addons azure-keyvault-secrets-provider
```

### Managed Identity

```bash
# Assign managed identity to AKS
az aks update \
  --resource-group openl-rg \
  --name openl-cluster \
  --enable-managed-identity

# Grant access to Key Vault
az keyvault set-policy \
  --name openl-vault \
  --object-id <managed-identity-object-id> \
  --secret-permissions get list
```

---

## Cost Optimization

### AKS Cost Optimization

- Use Azure Spot VMs for non-critical workloads
- Enable Cluster Autoscaler
- Use Azure Reserved VM Instances
- Right-size node VM types

### Database Cost Optimization

- Use Burstable SKU for development
- Enable automated backups with retention
- Use zone-redundant HA only when needed
- Consider serverless PostgreSQL for variable workloads

---

## Related Documentation

- [Kubernetes Deployment](../../kubernetes/) - Kubernetes guide
- [Docker Deployment](../../docker/) - Docker containers
- [Configuration Reference](../../../configuration/) - Configuration options
- [Security Guide](../../../configuration/security.md) - Security best practices

---

**Last Updated**: 2025-11-05
**Version**: 6.0.0-SNAPSHOT
