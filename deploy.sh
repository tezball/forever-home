#!/bin/bash

# Forever Home - AWS Deployment Script
# Builds and deploys the application to AWS ECS Fargate
# Usage: ./deploy.sh [--tag TAG] [--no-reset] [--blank]

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
RESET_DB="true"
BLANK_MODE="false"

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --tag|-t)
            IMAGE_TAG="$2"
            shift 2
            ;;
        --no-reset)
            RESET_DB="false"
            shift
            ;;
        --blank)
            BLANK_MODE="true"
            shift
            ;;
        --help|-h)
            echo "Usage: ./deploy.sh [--tag TAG] [--no-reset] [--blank]"
            echo ""
            echo "Options:"
            echo "  --tag, -t TAG    Docker image tag (default: latest)"
            echo "  --no-reset       Skip database reset (preserves existing data)"
            echo "  --blank          Deploy with empty database (no demo data, no demo login)"
            echo "  --help, -h       Show this help message"
            echo ""
            echo "By default, the database is reset to seed data on each deploy."
            echo "Use --blank for a production-ready deployment with no demo content."
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

if [ "$BLANK_MODE" = "true" ] && ! command -v jq &> /dev/null; then
    echo -e "${RED}ERROR: jq is not installed (required for --blank mode)${NC}"
    echo "Install with: sudo apt-get install jq"
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

# Check for clean git working directory
echo -e "${YELLOW}Checking git status...${NC}"
if [ -n "$(git status --porcelain)" ]; then
    echo -e "${RED}ERROR: Working directory has uncommitted changes${NC}"
    echo ""
    echo "Production builds must be from a clean git state to ensure"
    echo "the /actuator/info endpoint accurately reflects the deployed commit."
    echo ""
    echo "Please commit or stash your changes before deploying:"
    echo "  git status        # See what's changed"
    echo "  git stash         # Temporarily stash changes"
    echo "  git add . && git commit -m 'message'  # Or commit them"
    exit 1
fi
echo -e "${GREEN}Git working directory is clean${NC}"
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
echo -e "  Reset Database: ${GREEN}$RESET_DB${NC}"
echo -e "  Blank Mode:     ${GREEN}$BLANK_MODE${NC}"
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

# Get the latest active task definition info (needed for both reset and service update)
echo -e "${YELLOW}Getting task definition info...${NC}"
TASK_DEF_ARN=$(aws ecs describe-services \
    --cluster "$ECS_CLUSTER" \
    --services "$ECS_SERVICE" \
    --region "$AWS_REGION" \
    --query 'services[0].taskDefinition' \
    --output text)
TASK_DEF_FAMILY=$(echo "$TASK_DEF_ARN" | rev | cut -d: -f2 | cut -d/ -f1 | rev)

LATEST_TASK_DEF=$(aws ecs list-task-definitions \
    --family-prefix "$TASK_DEF_FAMILY" \
    --status ACTIVE \
    --sort DESC \
    --region "$AWS_REGION" \
    --query 'taskDefinitionArns[0]' \
    --output text)

# Get container name from task definition
CONTAINER_NAME=$(aws ecs describe-task-definition \
    --task-definition "$LATEST_TASK_DEF" \
    --region "$AWS_REGION" \
    --query 'taskDefinition.containerDefinitions[0].name' \
    --output text)

# Get network configuration from the service
NETWORK_CONFIG=$(aws ecs describe-services \
    --cluster "$ECS_CLUSTER" \
    --services "$ECS_SERVICE" \
    --region "$AWS_REGION" \
    --query 'services[0].networkConfiguration' \
    --output json)

# If blank mode, update task definition FIRST to disable test mode
# This ensures the reset task uses the correct configuration
if [ "$BLANK_MODE" = "true" ]; then
    echo -e "${YELLOW}Updating task definition for blank mode (disabling test mode)...${NC}"

    # Get current task definition JSON and update TEST_MODE_ENABLED
    TASK_DEF_JSON=$(aws ecs describe-task-definition \
        --task-definition "$LATEST_TASK_DEF" \
        --region "$AWS_REGION" \
        --query 'taskDefinition' \
        --output json)

    # Update TEST_MODE_ENABLED to false and remove non-writable fields
    TEMP_TASK_DEF=$(mktemp)
    echo "$TASK_DEF_JSON" | jq '
        .containerDefinitions[0].environment = (
            [.containerDefinitions[0].environment[] | select(.name != "TEST_MODE_ENABLED")] +
            [{"name": "TEST_MODE_ENABLED", "value": "false"}]
        ) |
        del(.taskDefinitionArn, .revision, .status, .requiresAttributes, .compatibilities, .registeredAt, .registeredBy, .deregisteredAt)
    ' > "$TEMP_TASK_DEF"

    # Register new task definition
    LATEST_TASK_DEF=$(aws ecs register-task-definition \
        --cli-input-json "file://$TEMP_TASK_DEF" \
        --region "$AWS_REGION" \
        --query 'taskDefinition.taskDefinitionArn' \
        --output text)
    rm -f "$TEMP_TASK_DEF"

    echo -e "${GREEN}Task definition updated: $LATEST_TASK_DEF${NC}"
fi

# Reset database if requested (default: true)
if [ "$RESET_DB" = "true" ]; then
    echo -e "${YELLOW}Resetting database via one-off ECS task...${NC}"

    # Build environment overrides - always include RESET_DATABASE=true
    # For blank mode, TEST_MODE_ENABLED=false is already in the task definition
    ENV_OVERRIDES='[{"name": "RESET_DATABASE", "value": "true"}]'

    # Run a one-off task with RESET_DATABASE=true
    echo -e "  Starting database reset task..."
    TASK_ARN=$(aws ecs run-task \
        --cluster "$ECS_CLUSTER" \
        --task-definition "$LATEST_TASK_DEF" \
        --launch-type FARGATE \
        --network-configuration "$NETWORK_CONFIG" \
        --overrides "{
            \"containerOverrides\": [{
                \"name\": \"$CONTAINER_NAME\",
                \"environment\": $ENV_OVERRIDES
            }]
        }" \
        --region "$AWS_REGION" \
        --query 'tasks[0].taskArn' \
        --output text)

    if [ -z "$TASK_ARN" ] || [ "$TASK_ARN" = "None" ]; then
        echo -e "${RED}ERROR: Failed to start database reset task${NC}"
        exit 1
    fi

    echo -e "  Task started: ${YELLOW}$TASK_ARN${NC}"
    echo -e "  Waiting for database reset to complete (90 seconds)..."

    # Wait for the task to reach RUNNING state
    aws ecs wait tasks-running \
        --cluster "$ECS_CLUSTER" \
        --tasks "$TASK_ARN" \
        --region "$AWS_REGION" 2>/dev/null || true

    # Give the app time to run Flyway clean/migrate and seed data
    sleep 60

    # Stop the reset task (we don't need the web server running)
    echo -e "  Stopping reset task..."
    aws ecs stop-task \
        --cluster "$ECS_CLUSTER" \
        --task "$TASK_ARN" \
        --reason "Database reset complete" \
        --region "$AWS_REGION" \
        --output text > /dev/null 2>&1 || true

    echo -e "${GREEN}Database reset complete${NC}"
    echo ""
fi

# Update ECS service with the task definition
if [ "$BLANK_MODE" = "true" ]; then
    # In blank mode, the task definition was already updated earlier
    echo -e "${YELLOW}Updating ECS service with blank mode task definition...${NC}"
    aws ecs update-service \
        --cluster "$ECS_CLUSTER" \
        --service "$ECS_SERVICE" \
        --task-definition "$LATEST_TASK_DEF" \
        --force-new-deployment \
        --region "$AWS_REGION" \
        --output text > /dev/null
else
    # Force new ECS deployment with existing task definition
    echo -e "${YELLOW}Forcing ECS deployment...${NC}"
    aws ecs update-service \
        --cluster "$ECS_CLUSTER" \
        --service "$ECS_SERVICE" \
        --force-new-deployment \
        --region "$AWS_REGION" \
        --output text > /dev/null
fi

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
