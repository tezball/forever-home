# Variables for Forever Home AWS Infrastructure

variable "aws_region" {
  description = "AWS region to deploy to"
  type        = string
  default     = "eu-west-1"
}

variable "environment" {
  description = "Environment name (dev, staging, prod)"
  type        = string
  default     = "dev"
}

# VPC Configuration
variable "vpc_cidr" {
  description = "CIDR block for VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "availability_zones" {
  description = "List of availability zones"
  type        = list(string)
  default     = ["eu-west-1a", "eu-west-1b"]
}

# ECS Configuration - Minimal sizing for cost optimization
variable "ecs_task_cpu" {
  description = "CPU units for the ECS task (256 = 0.25 vCPU, 512 = 0.5 vCPU)"
  type        = number
  default     = 512
}

variable "ecs_task_memory" {
  description = "Memory for the ECS task in MB (1024 MB for Spring Boot with static assets)"
  type        = number
  default     = 1024
}

variable "ecs_desired_count" {
  description = "Desired number of ECS tasks"
  type        = number
  default     = 1
}

variable "container_port" {
  description = "Port the container listens on"
  type        = number
  default     = 8080
}

# RDS Configuration - Minimal sizing
variable "db_instance_class" {
  description = "RDS instance class (db.t4g.micro is the smallest)"
  type        = string
  default     = "db.t4g.micro"
}

variable "db_allocated_storage" {
  description = "Allocated storage for RDS in GB"
  type        = number
  default     = 20
}

variable "db_name" {
  description = "Database name"
  type        = string
  default     = "foreverhome"
}

variable "db_username" {
  description = "Database master username"
  type        = string
  default     = "foreverhome_admin"
}

variable "db_multi_az" {
  description = "Enable Multi-AZ for RDS (set to false for cost savings in dev)"
  type        = bool
  default     = false
}

# Application Configuration
variable "admin_email" {
  description = "Admin email for bootstrap"
  type        = string
  default     = ""
}

variable "app_base_url" {
  description = "Base URL for the application (frontend URL)"
  type        = string
  default     = ""
}

variable "cors_allowed_origins" {
  description = "CORS allowed origins"
  type        = string
  default     = ""
}

# S3 Configuration
variable "s3_bucket_name" {
  description = "S3 bucket name for pet images (leave empty for auto-generated)"
  type        = string
  default     = ""
}

# Domain Configuration (optional)
variable "domain_name" {
  description = "Domain name for the application (optional)"
  type        = string
  default     = ""
}

variable "create_certificate" {
  description = "Create ACM certificate for HTTPS"
  type        = bool
  default     = false
}
