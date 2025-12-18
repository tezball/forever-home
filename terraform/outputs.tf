# Outputs for Forever Home Infrastructure

# VPC Outputs
output "vpc_id" {
  description = "VPC ID"
  value       = aws_vpc.main.id
}

output "public_subnet_ids" {
  description = "Public subnet IDs"
  value       = aws_subnet.public[*].id
}

output "private_subnet_ids" {
  description = "Private subnet IDs"
  value       = aws_subnet.private[*].id
}

# ECR Outputs
output "ecr_repository_url" {
  description = "ECR repository URL for the application"
  value       = aws_ecr_repository.app.repository_url
}

output "ecr_repository_name" {
  description = "ECR repository name"
  value       = aws_ecr_repository.app.name
}

# RDS Outputs
output "rds_endpoint" {
  description = "RDS endpoint"
  value       = aws_db_instance.main.endpoint
}

output "rds_host" {
  description = "RDS hostname (without port)"
  value       = aws_db_instance.main.address
}

output "rds_port" {
  description = "RDS port"
  value       = aws_db_instance.main.port
}

output "rds_database_name" {
  description = "RDS database name"
  value       = aws_db_instance.main.db_name
}

output "rds_username" {
  description = "RDS master username"
  value       = var.db_username
}

output "rds_secret_arn" {
  description = "ARN of the RDS credentials secret"
  value       = aws_secretsmanager_secret.db_password.arn
}

output "rds_publicly_accessible" {
  description = "Whether RDS is publicly accessible"
  value       = var.db_publicly_accessible
}

# IDE Database Connection Info (only shown when public access is enabled)
output "ide_database_connection" {
  description = "Database connection info for IDE (only when publicly accessible)"
  value = (var.db_publicly_accessible
    ? <<-EOT

    ============================================
    DATABASE CONNECTION INFO FOR IDE
    ============================================

    Host:     ${aws_db_instance.main.address}
    Port:     ${aws_db_instance.main.port}
    Database: ${aws_db_instance.main.db_name}
    Username: ${var.db_username}
    Password: (retrieve from AWS Secrets Manager)

    To get the password, run:
    aws secretsmanager get-secret-value --secret-id ${aws_secretsmanager_secret.db_password.arn} --query 'SecretString' --output text | jq -r '.password'

    JDBC URL:
    jdbc:postgresql://${aws_db_instance.main.address}:${aws_db_instance.main.port}/${aws_db_instance.main.db_name}

    ============================================
    EOT
    : "Database is not publicly accessible. Set db_publicly_accessible=true to enable IDE connections."
  )
  sensitive = false
}

# ECS Outputs
output "ecs_cluster_name" {
  description = "ECS cluster name"
  value       = aws_ecs_cluster.main.name
}

output "ecs_cluster_arn" {
  description = "ECS cluster ARN"
  value       = aws_ecs_cluster.main.arn
}

output "ecs_service_name" {
  description = "ECS service name"
  value       = aws_ecs_service.app.name
}

output "ecs_task_definition_arn" {
  description = "ECS task definition ARN"
  value       = aws_ecs_task_definition.app.arn
}

# ALB Outputs
output "alb_arn" {
  description = "ALB ARN"
  value       = aws_lb.main.arn
}

output "alb_dns_name" {
  description = "ALB DNS name"
  value       = aws_lb.main.dns_name
}

output "alb_zone_id" {
  description = "ALB zone ID (for Route53 alias)"
  value       = aws_lb.main.zone_id
}

output "application_url" {
  description = "Application URL"
  value       = var.domain_name != "" ? "https://${var.domain_name}" : "http://${aws_lb.main.dns_name}"
}

# S3 Outputs
output "s3_bucket_name" {
  description = "S3 bucket name for images"
  value       = aws_s3_bucket.images.id
}

output "s3_bucket_arn" {
  description = "S3 bucket ARN"
  value       = aws_s3_bucket.images.arn
}

# CloudWatch Outputs
output "cloudwatch_log_group" {
  description = "CloudWatch log group name"
  value       = aws_cloudwatch_log_group.app.name
}

# Secrets Outputs
output "jwt_secret_arn" {
  description = "ARN of the JWT secret"
  value       = aws_secretsmanager_secret.jwt_secret.arn
}

# Route53 Outputs
output "route53_zone_id" {
  description = "Route53 hosted zone ID"
  value       = var.domain_name != "" && var.route53_zone_id != "" ? var.route53_zone_id : (var.create_route53_zone ? aws_route53_zone.main[0].zone_id : null)
}

output "route53_nameservers" {
  description = "Route53 nameservers (only if zone was created)"
  value       = var.create_route53_zone && var.domain_name != "" ? aws_route53_zone.main[0].name_servers : null
}

output "dns_configuration_required" {
  description = "DNS configuration instructions"
  value = var.domain_name != "" ? (
    var.route53_zone_id != "" || var.create_route53_zone ? <<-EOT

    ============================================
    DNS MANAGED BY TERRAFORM
    ============================================

    Your domain ${var.domain_name} is configured with Route53.
    ${var.create_route53_zone ? "Zone ID: ${aws_route53_zone.main[0].zone_id}" : ""}
    ${var.create_route53_zone ? "Nameservers:\n  ${join("\n  ", aws_route53_zone.main[0].name_servers)}" : ""}

    DNS records are automatically managed by Terraform.

    WARNING: Running 'terraform destroy' will:
    - Delete all DNS records
    - Cause immediate service unavailability
    - Require DNS propagation (up to 48h) after re-apply

    ============================================
    EOT
    : <<-EOT

    ============================================
    MANUAL DNS CONFIGURATION REQUIRED
    ============================================

    Point your domain ${var.domain_name} to the ALB:

    Option A (Recommended - CNAME record):
      Name:  ${var.domain_name}
      Type:  CNAME
      Value: ${aws_lb.main.dns_name}

    Option B (A record via Route53 Alias):
      Name:     ${var.domain_name}
      Type:     A (Alias)
      Target:   ${aws_lb.main.dns_name}
      Zone ID:  ${aws_lb.main.zone_id}

    WARNING: The ALB DNS name changes on destroy/apply.
    You will need to update your DNS records manually.

    ============================================
    EOT
  ) : null
}

# Deployment Commands
output "docker_login_command" {
  description = "Command to login to ECR"
  value       = "aws ecr get-login-password --region ${var.aws_region} | docker login --username AWS --password-stdin ${aws_ecr_repository.app.repository_url}"
}

output "docker_build_push_commands" {
  description = "Commands to build and push Docker image"
  value       = <<-EOT
    # Build the Docker image
    docker build -t ${aws_ecr_repository.app.repository_url}:latest .

    # Push to ECR
    docker push ${aws_ecr_repository.app.repository_url}:latest

    # Force new deployment
    aws ecs update-service --cluster ${aws_ecs_cluster.main.name} --service ${aws_ecs_service.app.name} --force-new-deployment
  EOT
}
