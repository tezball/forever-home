# S3 Bucket for Pet Images

resource "aws_s3_bucket" "images" {
  bucket = var.s3_bucket_name != "" ? var.s3_bucket_name : "${local.name_prefix}-images-${random_id.suffix.hex}"

  tags = {
    Name = "${local.name_prefix}-images"
  }
}

# Block public access
resource "aws_s3_bucket_public_access_block" "images" {
  bucket = aws_s3_bucket.images.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# Bucket versioning (for recovery)
resource "aws_s3_bucket_versioning" "images" {
  bucket = aws_s3_bucket.images.id

  versioning_configuration {
    status = var.environment == "prod" ? "Enabled" : "Suspended"
  }
}

# Server-side encryption
resource "aws_s3_bucket_server_side_encryption_configuration" "images" {
  bucket = aws_s3_bucket.images.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

# Lifecycle rules for cost optimization
resource "aws_s3_bucket_lifecycle_configuration" "images" {
  bucket = aws_s3_bucket.images.id

  rule {
    id     = "cleanup-old-versions"
    status = "Enabled"

    filter {
      prefix = ""
    }

    # Move to cheaper storage class after 90 days
    transition {
      days          = 90
      storage_class = "STANDARD_IA"
    }

    # Delete old versions after 30 days (if versioning enabled)
    noncurrent_version_expiration {
      noncurrent_days = 30
    }

    # Abort incomplete multipart uploads
    abort_incomplete_multipart_upload {
      days_after_initiation = 1
    }
  }
}

# CORS configuration for frontend access
resource "aws_s3_bucket_cors_configuration" "images" {
  bucket = aws_s3_bucket.images.id

  cors_rule {
    allowed_headers = ["*"]
    allowed_methods = ["GET", "PUT", "POST", "DELETE"]
    allowed_origins = var.cors_allowed_origins != "" ? split(",", var.cors_allowed_origins) : ["*"]
    expose_headers  = ["ETag"]
    max_age_seconds = 3600
  }
}

# Bucket policy (optional - for CloudFront access if needed later)
resource "aws_s3_bucket_policy" "images" {
  bucket = aws_s3_bucket.images.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "AllowECSTaskAccess"
        Effect = "Allow"
        Principal = {
          AWS = aws_iam_role.ecs_task.arn
        }
        Action = [
          "s3:GetObject",
          "s3:PutObject",
          "s3:DeleteObject"
        ]
        Resource = "${aws_s3_bucket.images.arn}/*"
      }
    ]
  })
}
