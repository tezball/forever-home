# Forever Home - AWS Terraform Infrastructure

This Terraform configuration deploys the Forever Home application to AWS using Fargate with minimal sizing for cost optimization.

## Architecture

```
                                    ┌─────────────────────────────────────────────────────────────┐
                                    │                         AWS Cloud                           │
                                    │  ┌───────────────────────────────────────────────────────┐  │
                                    │  │                        VPC                            │  │
                                    │  │                                                       │  │
┌──────────┐    ┌─────────────┐     │  │  ┌─────────────────┐    ┌─────────────────────────┐  │  │
│          │    │             │     │  │  │  Public Subnet  │    │     Private Subnet      │  │  │
│  Users   │───▶│    ALB      │─────│──│──│                 │───▶│                         │  │  │
│          │    │             │     │  │  │                 │    │  ┌─────────────────┐    │  │  │
└──────────┘    └─────────────┘     │  │  │  NAT Gateway    │    │  │   ECS Fargate   │    │  │  │
                                    │  │  │                 │    │  │   (256 CPU/     │    │  │  │
                                    │  │  └─────────────────┘    │  │    512 MB)      │    │  │  │
                                    │  │                         │  └────────┬────────┘    │  │  │
                                    │  │                         │           │             │  │  │
                                    │  │                         │  ┌────────▼────────┐    │  │  │
                                    │  │                         │  │   RDS Postgres  │    │  │  │
                                    │  │                         │  │  (db.t4g.micro) │    │  │  │
                                    │  │                         │  └─────────────────┘    │  │  │
                                    │  │                         └─────────────────────────┘  │  │
                                    │  └───────────────────────────────────────────────────────┘  │
                                    │                                                             │
                                    │  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐   │
                                    │  │     ECR      │  │      S3      │  │ Secrets Manager  │   │
                                    │  │  (Images)    │  │   (Files)    │  │   (DB/JWT)       │   │
                                    │  └──────────────┘  └──────────────┘  └──────────────────┘   │
                                    └─────────────────────────────────────────────────────────────┘
```

## Minimal Sizing (Cost Optimized)

| Component | Size | Estimated Cost (USD/month) |
|-----------|------|---------------------------|
| ECS Fargate | 0.25 vCPU / 512 MB | ~$9 |
| RDS PostgreSQL | db.t4g.micro (2 vCPU, 1GB) | ~$12 |
| ALB | 1 load balancer | ~$16 |
| NAT Gateway | 1 gateway | ~$32 |
| S3 | Pay per use | ~$1-5 |
| ECR | Pay per storage | ~$1 |
| CloudWatch | Basic logs | ~$1-5 |
| **Total** | | **~$72-80/month** |

> **Note**: FARGATE_SPOT can reduce ECS costs by ~70% but may have interruptions.

## Prerequisites

1. AWS CLI configured with appropriate credentials
2. Terraform >= 1.0 installed
3. Docker installed (for building images)

## Quick Start

### 1. Initialize Terraform

```bash
cd terraform
terraform init
```

### 2. Configure Variables

```bash
cp terraform.tfvars.example terraform.tfvars
# Edit terraform.tfvars with your values
```

### 3. Plan and Apply

```bash
# Preview changes
terraform plan

# Apply infrastructure
terraform apply
```

### 4. Build and Deploy Application

```bash
# Get ECR login command from outputs
$(terraform output -raw docker_login_command)

# Build and push Docker image
cd ..
docker build -t $(terraform output -raw ecr_repository_url):latest .
docker push $(terraform output -raw ecr_repository_url):latest

# Force ECS deployment
aws ecs update-service \
  --cluster $(terraform output -raw ecs_cluster_name) \
  --service $(terraform output -raw ecs_service_name) \
  --force-new-deployment
```

### 5. Access Application

```bash
# Get the application URL
terraform output application_url
```

## Configuration

### Production Settings (Default)
- Multi-AZ RDS for high availability
- Deletion protection enabled
- HTTPS with ACM certificate
- Private database (not publicly accessible)

```hcl
environment         = "prod"
ecs_task_cpu        = 512
ecs_task_memory     = 1024
ecs_desired_count   = 2
db_instance_class   = "db.t4g.small"
db_multi_az         = true
create_certificate  = true
domain_name         = "yourdomain.com"
```

## Key Features

### Cost Optimization
- **VPC Endpoints**: Reduce NAT Gateway data transfer costs for AWS services
- **FARGATE_SPOT**: Use spot instances for non-production (up to 70% savings)
- **S3 Lifecycle**: Automatic transition to cheaper storage classes
- **ECR Lifecycle**: Keep only last 10 images

### Security
- **Private Subnets**: Application and database in private subnets
- **Secrets Manager**: Secure storage for database credentials and JWT secret
- **S3 Encryption**: Server-side encryption enabled
- **Security Groups**: Minimal required access

### High Availability (Production)
- **Multi-AZ RDS**: Database failover support
- **Multiple AZs**: ECS tasks distributed across availability zones
- **Auto Scaling**: CPU-based scaling for ECS service

## Useful Commands

```bash
# View all outputs
terraform output

# Get specific output
terraform output ecr_repository_url

# View ECS service status
aws ecs describe-services \
  --cluster $(terraform output -raw ecs_cluster_name) \
  --services $(terraform output -raw ecs_service_name)

# View application logs
aws logs tail $(terraform output -raw cloudwatch_log_group) --follow

# Force new deployment
aws ecs update-service \
  --cluster $(terraform output -raw ecs_cluster_name) \
  --service $(terraform output -raw ecs_service_name) \
  --force-new-deployment

# Destroy infrastructure
terraform destroy
```

## Remote State (Recommended for Teams)

Uncomment the backend configuration in `main.tf` and create the required resources:

```bash
# Create S3 bucket for state
aws s3 mb s3://forever-home-terraform-state

# Create DynamoDB table for locking
aws dynamodb create-table \
  --table-name forever-home-terraform-locks \
  --attribute-definitions AttributeName=LockID,AttributeType=S \
  --key-schema AttributeName=LockID,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST
```

## Troubleshooting

### ECS Task Fails to Start
1. Check CloudWatch logs: `aws logs tail /ecs/forever-home-prod --follow`
2. Verify ECR image exists: `aws ecr list-images --repository-name forever-home-prod-app`
3. Check task definition: `aws ecs describe-task-definition --task-definition forever-home-prod-app`

### Database Connection Issues
1. Verify security groups allow traffic from ECS tasks
2. Check RDS instance status: `aws rds describe-db-instances`
3. Verify secrets are accessible

### Health Check Failures
1. Ensure `/actuator/health` endpoint is accessible
2. Check container logs for startup errors
3. Verify security group allows ALB to reach ECS tasks
