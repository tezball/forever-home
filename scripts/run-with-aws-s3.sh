#!/bin/bash
set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
ENV_FILE="$PROJECT_DIR/.env"

echo -e "${GREEN}=== Forever Home - AWS S3 Mode ===${NC}"

# Load .env file if it exists
if [ -f "$ENV_FILE" ]; then
    echo -e "Loading credentials from ${YELLOW}.env${NC}"
    set -a
    source "$ENV_FILE"
    set +a
else
    echo -e "${RED}Error: .env file not found${NC}"
    echo -e "Create one by copying .env.example:"
    echo -e "  ${YELLOW}cp .env.example .env${NC}"
    echo -e "Then edit .env with your AWS credentials"
    exit 1
fi

# Validate required variables
if [ -z "$AWS_ACCESS_KEY_ID" ] || [ "$AWS_ACCESS_KEY_ID" = "your-access-key-here" ]; then
    echo -e "${RED}Error: AWS_ACCESS_KEY_ID not set in .env${NC}"
    exit 1
fi

if [ -z "$AWS_SECRET_ACCESS_KEY" ] || [ "$AWS_SECRET_ACCESS_KEY" = "your-secret-key-here" ]; then
    echo -e "${RED}Error: AWS_SECRET_ACCESS_KEY not set in .env${NC}"
    exit 1
fi

# Set defaults if not provided
export AWS_REGION="${AWS_REGION:-eu-west-1}"
export AWS_S3_BUCKET="${AWS_S3_BUCKET:-forever-home-images-dev}"

echo -e "\n${GREEN}Configuration:${NC}"
echo -e "  AWS_REGION: $AWS_REGION"
echo -e "  AWS_S3_BUCKET: $AWS_S3_BUCKET"
echo -e "  AWS_ACCESS_KEY_ID: ${AWS_ACCESS_KEY_ID:0:4}****"

# Start the application with s3-aws profile
echo -e "\n${GREEN}Starting application with AWS S3...${NC}"
cd "$PROJECT_DIR"
./mvnw spring-boot:run -Dspring-boot.run.profiles=s3-aws
