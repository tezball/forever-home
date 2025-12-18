# Terraform variables for Forever Home
# See terraform.tfvars.example for all available options

# AWS Configuration
aws_region  = "eu-west-1"
environment = "prod"

# Domain Configuration
domain_name        = "forever-home.ie"
create_certificate = true

# Route53 DNS Configuration
# Using existing hosted zone for forever-home.ie
route53_zone_id     = "Z0686542PO9ME5MYU4TH"
create_route53_zone = false
create_www_redirect = true

# Application Configuration
app_base_url         = "https://forever-home.ie"
cors_allowed_origins = "https://forever-home.ie,https://www.forever-home.ie"

# Email Configuration
email_from = "noreply@forever-home.ie"
ses_domain = "forever-home.ie"

# Admin email for notifications
admin_email = "admin@forever-home.ie"

# Database - match existing RDS configuration (can't change subnet group of existing DB)
db_publicly_accessible = true
