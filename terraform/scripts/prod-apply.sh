#!/bin/bash
# Production Infrastructure Apply Script
# Creates or updates the full production environment

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TERRAFORM_DIR="$(dirname "$SCRIPT_DIR")"

cd "$TERRAFORM_DIR"

echo "============================================"
echo "Forever Home - Production Infrastructure"
echo "============================================"
echo ""

# Check for required tfvars
if [ ! -f "terraform.tfvars" ]; then
    echo "ERROR: terraform.tfvars not found!"
    echo "Please copy terraform.tfvars.example and configure it."
    exit 1
fi

# Check AWS credentials
if ! aws sts get-caller-identity &>/dev/null; then
    echo "ERROR: AWS credentials not configured!"
    echo "Run 'aws configure' or set AWS_ACCESS_KEY_ID/AWS_SECRET_ACCESS_KEY"
    exit 1
fi

echo "AWS Account: $(aws sts get-caller-identity --query 'Account' --output text)"
echo "AWS Region: $(grep aws_region terraform.tfvars | cut -d'"' -f2)"
echo ""

# Initialize if needed
if [ ! -d ".terraform" ]; then
    echo "Initializing Terraform..."
    terraform init
fi

# Plan
echo ""
echo "Planning infrastructure changes..."
terraform plan -out=tfplan

# Confirm
echo ""
read -p "Do you want to apply these changes? (yes/no): " confirm
if [ "$confirm" != "yes" ]; then
    echo "Aborted."
    rm -f tfplan
    exit 0
fi

# Apply
echo ""
echo "Applying infrastructure..."
terraform apply tfplan
rm -f tfplan

# Show outputs
echo ""
echo "============================================"
echo "Infrastructure Applied Successfully!"
echo "============================================"
terraform output application_url
terraform output docker_build_push_commands

echo ""
echo "Next steps:"
echo "1. Build and push your Docker image (see commands above)"
echo "2. Configure DNS (see 'terraform output dns_configuration_required')"
echo "3. Access your application at the URL above"
