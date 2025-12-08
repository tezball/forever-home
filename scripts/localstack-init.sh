#!/bin/bash

# Create S3 bucket for pet images
awslocal s3 mb s3://forever-home-images

# Configure bucket for public read access (for image display)
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

# Configure CORS for bucket (allow uploads from frontend)
awslocal s3api put-bucket-cors --bucket forever-home-images --cors-configuration '{
  "CORSRules": [
    {
      "AllowedHeaders": ["*"],
      "AllowedMethods": ["GET", "PUT", "POST", "DELETE"],
      "AllowedOrigins": ["http://localhost:5173"],
      "ExposeHeaders": ["ETag"]
    }
  ]
}'

# Verify email identity for SES (LocalStack)
awslocal ses verify-email-identity --email-address noreply@foreverhome.local

echo "LocalStack initialized successfully!"
