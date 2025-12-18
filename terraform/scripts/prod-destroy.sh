#!/bin/bash
# Production Infrastructure Destroy Script
# Completely removes all production infrastructure to save costs

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TERRAFORM_DIR="$(dirname "$SCRIPT_DIR")"

cd "$TERRAFORM_DIR"

echo "============================================"
echo "Forever Home - DESTROY Production Infrastructure"
echo "============================================"
echo ""

# Check for state file
if [ ! -f "terraform.tfstate" ]; then
    echo "No terraform.tfstate found. Nothing to destroy."
    exit 0
fi

# Show what will be destroyed
echo "WARNING: This will DESTROY all production infrastructure!"
echo ""
echo "Resources that will be DELETED:"
echo "  - ECS Cluster and all running tasks"
echo "  - RDS Database (ALL DATA WILL BE LOST)"
echo "  - S3 Bucket (ALL IMAGES WILL BE LOST)"
echo "  - ALB and networking"
echo "  - Route53 DNS records (if managed)"
echo "  - ECR repository and images"
echo "  - All secrets"
echo ""

# DNS Warning
echo "============================================"
echo "DNS IMPACT WARNING"
echo "============================================"
echo ""
echo "If using Route53 managed by Terraform:"
echo "  - DNS records will be deleted immediately"
echo "  - Service will be unavailable"
echo "  - DNS propagation after re-apply can take up to 48 hours"
echo ""
echo "If using external DNS:"
echo "  - DNS records will point to non-existent ALB"
echo "  - You must update DNS manually after re-apply"
echo ""

# Confirm destruction
echo "============================================"
read -p "Type 'DESTROY' to confirm deletion: " confirm
if [ "$confirm" != "DESTROY" ]; then
    echo "Aborted. Infrastructure preserved."
    exit 0
fi

echo ""
echo "Are you ABSOLUTELY sure? This cannot be undone!"
read -p "Type 'yes' to proceed: " final_confirm
if [ "$final_confirm" != "yes" ]; then
    echo "Aborted. Infrastructure preserved."
    exit 0
fi

# Disable deletion protection on ALB if enabled
echo ""
echo "Disabling deletion protection..."
ALB_ARN=$(terraform output -raw alb_arn 2>/dev/null || echo "")
if [ -n "$ALB_ARN" ]; then
    echo "  Disabling ALB deletion protection..."
    aws elbv2 modify-load-balancer-attributes \
        --load-balancer-arn "$ALB_ARN" \
        --attributes Key=deletion_protection.enabled,Value=false 2>/dev/null || true
fi

# Disable deletion protection on RDS if enabled
RDS_ID=$(terraform output -raw rds_endpoint 2>/dev/null | cut -d: -f1 || echo "")
if [ -n "$RDS_ID" ]; then
    # Extract instance identifier from endpoint (e.g., forever-home-prod-postgres.xxxxx.region.rds.amazonaws.com)
    RDS_INSTANCE=$(echo "$RDS_ID" | cut -d. -f1)
    if [ -n "$RDS_INSTANCE" ]; then
        echo "  Disabling RDS deletion protection on $RDS_INSTANCE..."
        aws rds modify-db-instance \
            --db-instance-identifier "$RDS_INSTANCE" \
            --no-deletion-protection \
            --apply-immediately 2>/dev/null || true
        # Wait for modification to take effect
        echo "  Waiting for RDS modification to apply..."
        sleep 10
    fi
fi

# Scale down ECS service to avoid capacity provider issues
echo ""
echo "Scaling down ECS service..."
ECS_CLUSTER=$(terraform output -raw ecs_cluster_name 2>/dev/null || echo "")
ECS_SERVICE=$(terraform output -raw ecs_service_name 2>/dev/null || echo "")
if [ -n "$ECS_CLUSTER" ] && [ -n "$ECS_SERVICE" ]; then
    echo "  Scaling service to 0 tasks..."
    aws ecs update-service \
        --cluster "$ECS_CLUSTER" \
        --service "$ECS_SERVICE" \
        --desired-count 0 2>/dev/null || true
    echo "  Waiting for tasks to drain..."
    sleep 15
fi

# Empty S3 bucket before destroy
echo ""
echo "Emptying S3 bucket..."
BUCKET_NAME=$(terraform output -raw s3_bucket_name 2>/dev/null || echo "")
if [ -n "$BUCKET_NAME" ]; then
    aws s3 rm "s3://$BUCKET_NAME" --recursive 2>/dev/null || true
    # Delete versions if versioning was enabled
    aws s3api list-object-versions --bucket "$BUCKET_NAME" --output json 2>/dev/null | \
        jq -r '.Versions[]? | "\(.Key) \(.VersionId)"' | \
        while read key version; do
            aws s3api delete-object --bucket "$BUCKET_NAME" --key "$key" --version-id "$version" 2>/dev/null || true
        done
    # Delete markers
    aws s3api list-object-versions --bucket "$BUCKET_NAME" --output json 2>/dev/null | \
        jq -r '.DeleteMarkers[]? | "\(.Key) \(.VersionId)"' | \
        while read key version; do
            aws s3api delete-object --bucket "$BUCKET_NAME" --key "$key" --version-id "$version" 2>/dev/null || true
        done
fi

# Destroy infrastructure
echo ""
echo "Destroying infrastructure..."
terraform destroy -auto-approve

echo ""
echo "============================================"
echo "Infrastructure Destroyed Successfully"
echo "============================================"
echo ""
echo "All resources have been deleted."
echo ""
echo "To recreate the infrastructure:"
echo "  ./scripts/prod-apply.sh"
echo ""
echo "Remember:"
echo "  - Database data is PERMANENTLY LOST"
echo "  - S3 images are PERMANENTLY LOST"
echo "  - DNS may take up to 48h to propagate after re-apply"
