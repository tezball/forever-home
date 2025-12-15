# AWS SES (Simple Email Service) Configuration

# Domain identity for sending emails
# This creates the domain verification and DKIM records
resource "aws_ses_domain_identity" "main" {
  count  = var.ses_domain != "" ? 1 : 0
  domain = var.ses_domain
}

# DKIM verification for the domain (improves deliverability)
resource "aws_ses_domain_dkim" "main" {
  count  = var.ses_domain != "" ? 1 : 0
  domain = aws_ses_domain_identity.main[0].domain
}

# SES Configuration Set for tracking email metrics
resource "aws_ses_configuration_set" "main" {
  count = var.ses_domain != "" ? 1 : 0
  name  = "${local.name_prefix}-emails"

  reputation_metrics_enabled = true
  sending_enabled            = true

  delivery_options {
    tls_policy = "Require"
  }
}

# CloudWatch event destination for email tracking
resource "aws_ses_event_destination" "cloudwatch" {
  count                  = var.ses_domain != "" ? 1 : 0
  name                   = "${local.name_prefix}-cloudwatch"
  configuration_set_name = aws_ses_configuration_set.main[0].name
  enabled                = true
  matching_types         = ["send", "reject", "bounce", "complaint", "delivery"]

  cloudwatch_destination {
    default_value  = "default"
    dimension_name = "ses:configuration-set"
    value_source   = "emailHeader"
  }
}

# Output the verification token for DNS configuration
output "ses_domain_verification_token" {
  description = "Add this TXT record to your DNS: _amazonses.{domain}"
  value       = var.ses_domain != "" ? aws_ses_domain_identity.main[0].verification_token : null
}

output "ses_dkim_tokens" {
  description = "Add these CNAME records to your DNS for DKIM"
  value       = var.ses_domain != "" ? aws_ses_domain_dkim.main[0].dkim_tokens : null
}

output "ses_dns_records_required" {
  description = "DNS records that need to be added"
  value = var.ses_domain != "" ? join("\n", [
    "",
    "============================================",
    "DNS RECORDS REQUIRED FOR SES VERIFICATION",
    "============================================",
    "",
    "1. TXT Record (Domain Verification):",
    "   Name:  _amazonses.${var.ses_domain}",
    "   Value: ${aws_ses_domain_identity.main[0].verification_token}",
    "",
    "2. CNAME Records (DKIM - add all 3):",
    "   ${aws_ses_domain_dkim.main[0].dkim_tokens[0]}._domainkey.${var.ses_domain} -> ${aws_ses_domain_dkim.main[0].dkim_tokens[0]}.dkim.amazonses.com",
    "   ${aws_ses_domain_dkim.main[0].dkim_tokens[1]}._domainkey.${var.ses_domain} -> ${aws_ses_domain_dkim.main[0].dkim_tokens[1]}.dkim.amazonses.com",
    "   ${aws_ses_domain_dkim.main[0].dkim_tokens[2]}._domainkey.${var.ses_domain} -> ${aws_ses_domain_dkim.main[0].dkim_tokens[2]}.dkim.amazonses.com",
    "",
    "============================================"
  ]) : null
}
