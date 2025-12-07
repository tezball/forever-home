#!/bin/bash

# Create S3 bucket for pet images
awslocal s3 mb s3://forever-home-images

# Verify email identity for SES (LocalStack)
awslocal ses verify-email-identity --email-address noreply@foreverhome.local

echo "LocalStack initialized successfully!"
