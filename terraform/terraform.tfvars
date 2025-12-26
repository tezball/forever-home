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

# Admin email for bootstrap (empty = no admin created on startup)
admin_email = ""

# Super admin email (created/upgraded on startup)
super_admin_email = "tezball86@gmail.com"

# Google OAuth2 Client ID (for Sign in with Google)
google_client_id = "426516691891-73le29i6edo132ve7duhdof7s1vf8vif.apps.googleusercontent.com"

# Database - match existing RDS configuration (can't change subnet group of existing DB)
db_publicly_accessible = true

# S3 Configuration - use existing bucket
s3_bucket_name = "forever-home-prod-images-9c83111f"
