# Forever Home - AWS Infrastructure
# Terraform configuration for deploying to AWS using Fargate with minimal sizing

terraform {
  required_version = ">= 1.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.0"
    }
  }

  # Uncomment and configure for remote state storage
  # backend "s3" {
  #   bucket         = "forever-home-terraform-state"
  #   key            = "terraform.tfstate"
  #   region         = "us-east-1"
  #   encrypt        = true
  #   dynamodb_table = "forever-home-terraform-locks"
  # }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "forever-home"
      Environment = var.environment
      ManagedBy   = "terraform"
    }
  }
}

# Random suffix for unique resource names
resource "random_id" "suffix" {
  byte_length = 4
}

locals {
  name_prefix = "forever-home-${var.environment}"
  common_tags = {
    Project     = "forever-home"
    Environment = var.environment
  }
}
