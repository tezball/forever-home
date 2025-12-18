# Route53 DNS Configuration
# Manages DNS records for the application domain

# Option 1: Use existing hosted zone (recommended for production)
data "aws_route53_zone" "main" {
  count        = var.domain_name != "" && var.route53_zone_id != "" ? 1 : 0
  zone_id      = var.route53_zone_id
  private_zone = false
}

# Option 2: Create new hosted zone (useful for dedicated environments)
resource "aws_route53_zone" "main" {
  count = var.domain_name != "" && var.route53_zone_id == "" && var.create_route53_zone ? 1 : 0
  name  = var.domain_name

  tags = {
    Name = "${local.name_prefix}-zone"
  }
}

locals {
  # Use existing zone ID if provided, otherwise use created zone, or null
  zone_id = var.domain_name != "" ? (
    var.route53_zone_id != "" ? var.route53_zone_id : (
      var.create_route53_zone ? aws_route53_zone.main[0].zone_id : null
    )
  ) : null

  # Determine if we should create DNS records
  create_dns_records = local.zone_id != null && var.domain_name != ""
}

# A Record (Alias) pointing to ALB
resource "aws_route53_record" "app" {
  count   = local.create_dns_records ? 1 : 0
  zone_id = local.zone_id
  name    = var.domain_name
  type    = "A"

  alias {
    name                   = aws_lb.main.dns_name
    zone_id                = aws_lb.main.zone_id
    evaluate_target_health = true
  }
}

# WWW subdomain redirect (optional)
resource "aws_route53_record" "www" {
  count   = local.create_dns_records && var.create_www_redirect ? 1 : 0
  zone_id = local.zone_id
  name    = "www.${var.domain_name}"
  type    = "A"

  alias {
    name                   = aws_lb.main.dns_name
    zone_id                = aws_lb.main.zone_id
    evaluate_target_health = true
  }
}

# ACM Certificate DNS Validation Records
resource "aws_route53_record" "cert_validation" {
  for_each = var.create_certificate && local.create_dns_records ? {
    for dvo in aws_acm_certificate.main[0].domain_validation_options : dvo.domain_name => {
      name   = dvo.resource_record_name
      record = dvo.resource_record_value
      type   = dvo.resource_record_type
    }
  } : {}

  allow_overwrite = true
  name            = each.value.name
  records         = [each.value.record]
  ttl             = 60
  type            = each.value.type
  zone_id         = local.zone_id
}

# Certificate validation
resource "aws_acm_certificate_validation" "main" {
  count                   = var.create_certificate && local.create_dns_records ? 1 : 0
  certificate_arn         = aws_acm_certificate.main[0].arn
  validation_record_fqdns = [for record in aws_route53_record.cert_validation : record.fqdn]
}

# SES Domain Verification TXT Record
resource "aws_route53_record" "ses_verification" {
  count   = local.create_dns_records && var.ses_domain != "" ? 1 : 0
  zone_id = local.zone_id
  name    = "_amazonses.${var.ses_domain}"
  type    = "TXT"
  ttl     = 600
  records = [aws_ses_domain_identity.main[0].verification_token]
}

# SES DKIM CNAME Records
resource "aws_route53_record" "ses_dkim" {
  count   = local.create_dns_records && var.ses_domain != "" ? 3 : 0
  zone_id = local.zone_id
  name    = "${aws_ses_domain_dkim.main[0].dkim_tokens[count.index]}._domainkey.${var.ses_domain}"
  type    = "CNAME"
  ttl     = 600
  records = ["${aws_ses_domain_dkim.main[0].dkim_tokens[count.index]}.dkim.amazonses.com"]
}
