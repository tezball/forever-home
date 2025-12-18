# Secrets Management

# Generate JWT Secret
resource "random_password" "jwt_secret" {
  length  = 64
  special = false # Keep it simple for base64 encoding
}

# Store JWT Secret in Secrets Manager
resource "aws_secretsmanager_secret" "jwt_secret" {
  name                    = "${local.name_prefix}-jwt-secret-${random_id.suffix.hex}"
  description             = "JWT signing secret for Forever Home"
  recovery_window_in_days = 7

  tags = {
    Name = "${local.name_prefix}-jwt-secret"
  }
}

resource "aws_secretsmanager_secret_version" "jwt_secret" {
  secret_id     = aws_secretsmanager_secret.jwt_secret.id
  secret_string = random_password.jwt_secret.result
}
