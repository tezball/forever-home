#!/bin/bash

# Forever Home - AWS Deployment Script
# Builds and deploys the application to AWS ECS Fargate
# Usage: ./deploy.sh [--tag TAG]

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TERRAFORM_DIR="$PROJECT_ROOT/terraform"

# Default tag is 'latest'
IMAGE_TAG="latest"

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --tag|-t)
            IMAGE_TAG="$2"
            shift 2
            ;;
        --help|-h)
            echo "Usage: ./deploy.sh [--tag TAG]"
            echo ""
            echo "Options:"
            echo "  --tag, -t TAG    Docker image tag (default: latest)"
            echo "  --help, -h       Show this help message"
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            exit 1
            ;;
    esac
done

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}Forever Home - AWS Deployment${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""

# Check prerequisites
echo -e "${YELLOW}Checking prerequisites...${NC}"

if ! command -v aws &> /dev/null; then
    echo -e "${RED}ERROR: AWS CLI is not installed${NC}"
    exit 1
fi

if ! command -v docker &> /dev/null; then
    echo -e "${RED}ERROR: Docker is not installed${NC}"
    exit 1
fi

if ! command -v terraform &> /dev/null; then
    echo -e "${RED}ERROR: Terraform is not installed${NC}"
    exit 1
fi

# Check AWS credentials
if ! aws sts get-caller-identity &>/dev/null; then
    echo -e "${RED}ERROR: AWS credentials not configured${NC}"
    echo "Run 'aws configure' or set AWS_ACCESS_KEY_ID/AWS_SECRET_ACCESS_KEY"
    exit 1
fi

echo -e "${GREEN}Prerequisites OK${NC}"
echo ""

# Get terraform outputs
echo -e "${YELLOW}Reading Terraform outputs...${NC}"
cd "$TERRAFORM_DIR"

if [ ! -f "terraform.tfstate" ]; then
    echo -e "${RED}ERROR: Terraform state not found${NC}"
    echo "Run './terraform/scripts/prod-apply.sh' first to provision infrastructure"
    exit 1
fi

AWS_REGION=$(terraform output -raw aws_region 2>/dev/null || grep 'aws_region' terraform.tfvars | cut -d'"' -f2)
ECR_REPO_URL=$(terraform output -raw ecr_repository_url)
ECS_CLUSTER=$(terraform output -raw ecs_cluster_name)
ECS_SERVICE=$(terraform output -raw ecs_service_name)

if [ -z "$ECR_REPO_URL" ] || [ -z "$ECS_CLUSTER" ] || [ -z "$ECS_SERVICE" ]; then
    echo -e "${RED}ERROR: Could not read Terraform outputs${NC}"
    echo "Ensure infrastructure is deployed with 'terraform apply'"
    exit 1
fi

echo -e "  ECR Repository: ${GREEN}$ECR_REPO_URL${NC}"
echo -e "  ECS Cluster:    ${GREEN}$ECS_CLUSTER${NC}"
echo -e "  ECS Service:    ${GREEN}$ECS_SERVICE${NC}"
echo -e "  Image Tag:      ${GREEN}$IMAGE_TAG${NC}"
echo ""

# Login to ECR
echo -e "${YELLOW}Logging in to ECR...${NC}"
aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$ECR_REPO_URL"
echo -e "${GREEN}ECR login successful${NC}"
echo ""

# Build Docker image
echo -e "${YELLOW}Building Docker image...${NC}"
cd "$PROJECT_ROOT"
docker build -t "$ECR_REPO_URL:$IMAGE_TAG" .
echo -e "${GREEN}Docker image built successfully${NC}"
echo ""

# Push to ECR
echo -e "${YELLOW}Pushing image to ECR...${NC}"
docker push "$ECR_REPO_URL:$IMAGE_TAG"
echo -e "${GREEN}Image pushed successfully${NC}"
echo ""

# Force new ECS deployment
echo -e "${YELLOW}Forcing ECS deployment...${NC}"
aws ecs update-service \
    --cluster "$ECS_CLUSTER" \
    --service "$ECS_SERVICE" \
    --force-new-deployment \
    --region "$AWS_REGION" \
    --output text > /dev/null

echo -e "${GREEN}Deployment initiated${NC}"
echo ""

# Show deployment status
echo -e "${BLUE}============================================${NC}"
echo -e "${GREEN}Deployment Complete!${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""
echo -e "Image: ${GREEN}$ECR_REPO_URL:$IMAGE_TAG${NC}"
echo ""
echo -e "Monitor deployment progress:"
echo -e "  ${YELLOW}aws ecs describe-services --cluster $ECS_CLUSTER --services $ECS_SERVICE --query 'services[0].deployments'${NC}"
echo ""
echo -e "View application logs:"
echo -e "  ${YELLOW}aws logs tail /ecs/$ECS_CLUSTER --follow${NC}"
echo ""

# Get application URL
cd "$TERRAFORM_DIR"
APP_URL=$(terraform output -raw application_url 2>/dev/null || echo "")
if [ -n "$APP_URL" ]; then
    echo -e "Application URL: ${GREEN}$APP_URL${NC}"
fi
