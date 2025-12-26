# Variables for Forever Home AWS Infrastructure

variable "aws_region" {
  description = "AWS region to deploy to"
  type        = string
  default     = "eu-west-1"
}

variable "environment" {
  description = "Environment name"
  type        = string
  default     = "prod"
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
  description = "Desired number of ECS tasks (2+ recommended for production HA)"
  type        = number
  default     = 2
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
  description = "Enable Multi-AZ for RDS for high availability"
  type        = bool
  default     = true
}

variable "db_publicly_accessible" {
  description = "Make RDS publicly accessible for external connections. WARNING: Should be false in production!"
  type        = bool
  default     = false
}

# Application Configuration
variable "admin_email" {
  description = "Admin email for bootstrap"
  type        = string
  default     = ""
}

variable "super_admin_email" {
  description = "Email for the super admin user (will be created on startup)"
  type        = string
  default     = ""
}

variable "google_client_id" {
  description = "Google OAuth2 Client ID for Sign in with Google"
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

# Email Configuration
variable "email_from" {
  description = "Email address to send emails from (domain must be verified in SES)"
  type        = string
  default     = ""
}

variable "ses_domain" {
  description = "Domain to verify in SES for sending emails"
  type        = string
  default     = ""
}

# Database Reset Configuration
variable "reset_database_on_deploy" {
  description = "WARNING: If true, drops ALL tables and re-runs migrations on each deploy. DANGEROUS!"
  type        = bool
  default     = false
}

# Domain Configuration (optional)
variable "domain_name" {
  description = "Domain name for the application (optional)"
  type        = string
  default     = ""
}

variable "create_certificate" {
  description = "Create ACM certificate for HTTPS (recommended for production)"
  type        = bool
  default     = true
}

# Route53 Configuration
variable "route53_zone_id" {
  description = "Existing Route53 hosted zone ID (leave empty to create new or skip)"
  type        = string
  default     = ""
}

variable "create_route53_zone" {
  description = "Create a new Route53 hosted zone (only if route53_zone_id is empty)"
  type        = bool
  default     = false
}

variable "create_www_redirect" {
  description = "Create www subdomain alias record"
  type        = bool
  default     = true
}
