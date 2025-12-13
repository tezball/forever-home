#!/bin/bash
set -e

echo "=== LocalStack Initialization Script ==="

# Check if bucket already exists (idempotent)
if awslocal s3api head-bucket --bucket forever-home-images 2>/dev/null; then
    echo "Bucket 'forever-home-images' already exists"
    OBJECT_COUNT=$(awslocal s3 ls s3://forever-home-images --recursive 2>/dev/null | wc -l || echo "0")
    echo "  - Current object count: $OBJECT_COUNT"
else
    echo "Creating S3 bucket 'forever-home-images'..."
    awslocal s3 mb s3://forever-home-images
    echo "  - Bucket created successfully"
fi

# Configure bucket for public read access (for image display)
# This is idempotent - applying the same policy is safe
echo "Configuring bucket policy for public read access..."
awslocal s3api put-bucket-policy --bucket forever-home-images --policy '{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "PublicReadGetObject",
      "Effect": "Allow",
      "Principal": "*",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::forever-home-images/*"
    }
  ]
}'
echo "  - Bucket policy applied"

# Configure CORS for bucket (allow uploads from frontend)
# This is idempotent - applying the same CORS config is safe
echo "Configuring CORS for frontend uploads..."
awslocal s3api put-bucket-cors --bucket forever-home-images --cors-configuration '{
  "CORSRules": [
    {
      "AllowedHeaders": ["*"],
      "AllowedMethods": ["GET", "PUT", "POST", "DELETE", "HEAD"],
      "AllowedOrigins": ["http://localhost:5173", "http://localhost:3000"],
      "ExposeHeaders": ["ETag"],
      "MaxAgeSeconds": 3600
    }
  ]
}'
echo "  - CORS configuration applied"

# Verify email identity for SES (LocalStack) - idempotent
echo "Verifying SES email identity..."
awslocal ses verify-email-identity --email-address noreply@foreverhome.local 2>/dev/null || true
echo "  - Email identity verified"

# Final status
echo ""
echo "=== LocalStack Initialization Complete ==="
echo "S3 Endpoint: http://localhost:4566"
echo "Bucket: forever-home-images"
FINAL_COUNT=$(awslocal s3 ls s3://forever-home-images --recursive 2>/dev/null | wc -l || echo "0")
echo "Objects in bucket: $FINAL_COUNT"
echo ""
