# AWS Deployment

Deploy OpenL Tablets on Amazon Web Services using EKS, ECS, or EC2.

## Deployment Options

| Option | Use Case | Complexity | Scalability |
|--------|----------|------------|-------------|
| **EKS (Kubernetes)** | Production, enterprise | Medium | High (auto-scaling) |
| **ECS Fargate** | Serverless containers | Low-Medium | Medium-High |
| **EC2 + Docker** | Traditional deployment | Low | Medium (manual) |
| **Elastic Beanstalk** | PaaS deployment | Low | Medium |

---

## Option 1: Amazon EKS (Recommended)

Deploy OpenL Tablets on Amazon Elastic Kubernetes Service for production workloads.

### Prerequisites

```bash
# Install AWS CLI
aws --version

# Install eksctl
eksctl version

# Install kubectl
kubectl version --client

# Configure AWS credentials
aws configure
```

### Create EKS Cluster

```bash
# Create cluster with eksctl
eksctl create cluster \
  --name openl-cluster \
  --region us-east-1 \
  --nodegroup-name openl-nodes \
  --node-type t3.xlarge \
  --nodes 3 \
  --nodes-min 3 \
  --nodes-max 10 \
  --managed

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
  --set externalDatabase.host=<RDS-ENDPOINT> \
  --set redis.enabled=false \
  --set externalRedis.host=<ELASTICACHE-ENDPOINT>
```

### Configure AWS Services

#### Amazon RDS (PostgreSQL)

```bash
# Create RDS PostgreSQL instance
aws rds create-db-instance \
  --db-instance-identifier openl-postgres \
  --db-instance-class db.r6g.xlarge \
  --engine postgres \
  --engine-version 16.1 \
  --master-username openl \
  --master-user-password <password> \
  --allocated-storage 100 \
  --storage-type gp3 \
  --storage-encrypted \
  --multi-az \
  --vpc-security-group-ids <security-group-id> \
  --db-subnet-group-name <subnet-group>
```

#### Amazon ElastiCache (Redis)

```bash
# Create ElastiCache Redis cluster
aws elasticache create-replication-group \
  --replication-group-id openl-redis \
  --replication-group-description "OpenL Tablets Redis" \
  --engine redis \
  --cache-node-type cache.r6g.large \
  --num-cache-clusters 3 \
  --cache-subnet-group-name <subnet-group> \
  --security-group-ids <security-group-id> \
  --at-rest-encryption-enabled \
  --transit-encryption-enabled
```

#### Amazon EFS (Shared Storage)

```bash
# Create EFS file system
aws efs create-file-system \
  --creation-token openl-efs \
  --performance-mode generalPurpose \
  --throughput-mode bursting \
  --encrypted \
  --tags Key=Name,Value=openl-efs

# Create mount targets (one per AZ)
aws efs create-mount-target \
  --file-system-id <fs-id> \
  --subnet-id <subnet-id> \
  --security-groups <security-group-id>
```

### Storage Class for EFS

```yaml
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: efs-sc
provisioner: efs.csi.aws.com
parameters:
  provisioningMode: efs-ap
  fileSystemId: <efs-id>
  directoryPerms: "700"
```

### Ingress with ALB

```bash
# Install AWS Load Balancer Controller
helm repo add eks https://aws.github.io/eks-charts
helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  --namespace kube-system \
  --set clusterName=openl-cluster \
  --set serviceAccount.create=false \
  --set serviceAccount.name=aws-load-balancer-controller
```

Create Ingress:

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: openl-ingress
  namespace: openl
  annotations:
    alb.ingress.kubernetes.io/scheme: internet-facing
    alb.ingress.kubernetes.io/target-type: ip
    alb.ingress.kubernetes.io/listen-ports: '[{"HTTP": 80}, {"HTTPS":443}]'
    alb.ingress.kubernetes.io/certificate-arn: <acm-certificate-arn>
    alb.ingress.kubernetes.io/ssl-redirect: '443'
spec:
  ingressClassName: alb
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
```

---

## Option 2: Amazon ECS Fargate

Serverless container deployment with AWS Fargate.

### Create ECS Cluster

```bash
# Create cluster
aws ecs create-cluster \
  --cluster-name openl-cluster \
  --capacity-providers FARGATE FARGATE_SPOT
```

### Create Task Definition

```json
{
  "family": "openl-studio",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "2048",
  "memory": "4096",
  "containerDefinitions": [
    {
      "name": "studio",
      "image": "openltablets/studio:latest",
      "portMappings": [
        {
          "containerPort": 8080,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {
          "name": "DATABASE_URL",
          "value": "postgresql://..."
        },
        {
          "name": "SECURITY_MODE",
          "value": "multi-user"
        }
      ],
      "secrets": [
        {
          "name": "ADMIN_PASSWORD",
          "valueFrom": "arn:aws:secretsmanager:..."
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/openl-studio",
          "awslogs-region": "us-east-1",
          "awslogs-stream-prefix": "ecs"
        }
      }
    }
  ]
}
```

### Create Service

```bash
# Create ECS service with ALB
aws ecs create-service \
  --cluster openl-cluster \
  --service-name openl-studio \
  --task-definition openl-studio:1 \
  --desired-count 3 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[<subnet-ids>],securityGroups=[<sg-ids>],assignPublicIp=DISABLED}" \
  --load-balancers "targetGroupArn=<tg-arn>,containerName=studio,containerPort=8080" \
  --health-check-grace-period-seconds 60
```

---

## Option 3: EC2 with Docker Compose

Traditional VM-based deployment.

### Launch EC2 Instance

```bash
# Launch EC2 instance
aws ec2 run-instances \
  --image-id ami-0c55b159cbfafe1f0 \
  --instance-type t3.xlarge \
  --key-name <your-key-pair> \
  --security-group-ids <security-group-id> \
  --subnet-id <subnet-id> \
  --block-device-mappings '[{"DeviceName":"/dev/sda1","Ebs":{"VolumeSize":100,"VolumeType":"gp3"}}]' \
  --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=openl-server}]'
```

### Install Docker

```bash
# SSH into instance
ssh -i <key>.pem ec2-user@<instance-ip>

# Install Docker
sudo yum update -y
sudo yum install -y docker
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker ec2-user

# Install Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" \
  -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
```

### Deploy with Docker Compose

```bash
# Download docker-compose file
curl -O https://raw.githubusercontent.com/openl-tablets/openl-tablets/master/docs/deployment/docker/docker-compose-multi.yaml

# Start services
docker-compose -f docker-compose-multi.yaml up -d
```

---

## Infrastructure as Code

### Terraform Configuration

Create `main.tf`:

```hcl
terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = "us-east-1"
}

# VPC
module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "5.1.0"

  name = "openl-vpc"
  cidr = "10.0.0.0/16"

  azs             = ["us-east-1a", "us-east-1b", "us-east-1c"]
  private_subnets = ["10.0.1.0/24", "10.0.2.0/24", "10.0.3.0/24"]
  public_subnets  = ["10.0.101.0/24", "10.0.102.0/24", "10.0.103.0/24"]

  enable_nat_gateway = true
  enable_dns_hostnames = true

  tags = {
    Environment = "production"
    Application = "openl-tablets"
  }
}

# EKS Cluster
module "eks" {
  source  = "terraform-aws-modules/eks/aws"
  version = "19.16.0"

  cluster_name    = "openl-cluster"
  cluster_version = "1.28"

  vpc_id     = module.vpc.vpc_id
  subnet_ids = module.vpc.private_subnets

  eks_managed_node_groups = {
    openl_nodes = {
      desired_size = 3
      min_size     = 3
      max_size     = 10

      instance_types = ["t3.xlarge"]
      capacity_type  = "ON_DEMAND"
    }
  }

  tags = {
    Environment = "production"
  }
}

# RDS PostgreSQL
module "db" {
  source  = "terraform-aws-modules/rds/aws"
  version = "6.1.0"

  identifier = "openl-postgres"

  engine               = "postgres"
  engine_version       = "16.1"
  family               = "postgres16"
  major_engine_version = "16"
  instance_class       = "db.r6g.xlarge"

  allocated_storage     = 100
  max_allocated_storage = 500
  storage_encrypted     = true

  db_name  = "openl"
  username = "openl"
  port     = 5432

  multi_az               = true
  db_subnet_group_name   = module.vpc.database_subnet_group_name
  vpc_security_group_ids = [module.security_group_db.security_group_id]

  backup_retention_period = 30
  backup_window           = "03:00-04:00"
  maintenance_window      = "mon:04:00-mon:05:00"

  tags = {
    Environment = "production"
  }
}

# ElastiCache Redis
resource "aws_elasticache_replication_group" "openl_redis" {
  replication_group_id       = "openl-redis"
  replication_group_description = "OpenL Tablets Redis cluster"

  engine               = "redis"
  engine_version       = "7.0"
  node_type            = "cache.r6g.large"
  num_cache_clusters   = 3
  parameter_group_name = "default.redis7"
  port                 = 6379

  subnet_group_name          = aws_elasticache_subnet_group.openl.name
  security_group_ids         = [module.security_group_redis.security_group_id]
  at_rest_encryption_enabled = true
  transit_encryption_enabled = true

  automatic_failover_enabled = true
  multi_az_enabled           = true

  tags = {
    Environment = "production"
  }
}

# EFS File System
resource "aws_efs_file_system" "openl" {
  creation_token = "openl-efs"
  encrypted      = true

  performance_mode = "generalPurpose"
  throughput_mode  = "bursting"

  tags = {
    Name        = "openl-efs"
    Environment = "production"
  }
}

# EFS Mount Targets
resource "aws_efs_mount_target" "openl" {
  count = length(module.vpc.private_subnets)

  file_system_id  = aws_efs_file_system.openl.id
  subnet_id       = module.vpc.private_subnets[count.index]
  security_groups = [module.security_group_efs.security_group_id]
}

# Security Groups
module "security_group_db" {
  source  = "terraform-aws-modules/security-group/aws"
  version = "5.1.0"

  name        = "openl-db-sg"
  description = "Security group for RDS PostgreSQL"
  vpc_id      = module.vpc.vpc_id

  ingress_with_cidr_blocks = [
    {
      from_port   = 5432
      to_port     = 5432
      protocol    = "tcp"
      cidr_blocks = module.vpc.vpc_cidr_block
    }
  ]
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

# Get outputs
terraform output
```

---

## Monitoring and Observability

### Amazon CloudWatch

```bash
# Create log group
aws logs create-log-group --log-group-name /aws/openl/studio

# Enable Container Insights for EKS
aws eks update-cluster-config \
  --name openl-cluster \
  --logging '{"clusterLogging":[{"types":["api","audit","authenticator","controllerManager","scheduler"],"enabled":true}]}'
```

### AWS X-Ray

Add X-Ray daemon sidecar:

```yaml
# In Kubernetes deployment
- name: xray-daemon
  image: amazon/aws-xray-daemon
  ports:
    - containerPort: 2000
      protocol: UDP
```

### Prometheus on EKS

```bash
# Install Prometheus
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm install prometheus prometheus-community/kube-prometheus-stack \
  --namespace monitoring \
  --create-namespace
```

---

## Security Best Practices

### IAM Roles and Policies

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::openl-artifacts/*",
        "arn:aws:s3:::openl-artifacts"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "secretsmanager:GetSecretValue"
      ],
      "Resource": "arn:aws:secretsmanager:*:*:secret:openl/*"
    }
  ]
}
```

### Secrets Manager

```bash
# Store secrets
aws secretsmanager create-secret \
  --name openl/admin-password \
  --secret-string "your-secure-password"

# Reference in ECS
"secrets": [
  {
    "name": "ADMIN_PASSWORD",
    "valueFrom": "arn:aws:secretsmanager:us-east-1:123456789012:secret:openl/admin-password"
  }
]
```

---

## Cost Optimization

### EKS Cost Optimization

- Use Spot instances for non-critical workloads
- Enable Cluster Autoscaler
- Use Fargate for variable workloads
- Right-size node instance types

### RDS Cost Optimization

- Use Reserved Instances for production
- Enable automated backups with retention
- Use gp3 storage (cheaper than gp2)
- Consider Aurora Serverless for variable workloads

---

## Related Documentation

- [Kubernetes Deployment](../../kubernetes/) - Kubernetes guide
- [Docker Deployment](../../docker/) - Docker containers
- [Configuration Reference](../../../configuration/) - Configuration options
- [Security Guide](../../../configuration/security.md) - Security best practices

---

**Last Updated**: 2025-11-05
**Version**: 6.0.0-SNAPSHOT
