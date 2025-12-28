# AWS SES Setup Guide

This guide walks through configuring AWS Simple Email Service (SES) for email verification and notifications in Forever Home.

## Prerequisites

- AWS account with appropriate permissions
- Domain you own with access to DNS settings
- Terraform installed and configured with AWS credentials

## Step 1: Configure Terraform Variables

Update your `terraform/terraform.tfvars` file with your domain:

```hcl
# SES Domain Configuration
ses_domain = "yourdomain.com"
email_from = "noreply@yourdomain.com"
```

## Step 2: Apply Terraform

Run Terraform to create the SES resources:

```bash
cd terraform
terraform plan    # Review changes
terraform apply   # Apply changes
```

After apply completes, Terraform will output the required DNS records:

```
ses_dns_records_required = <<EOT
============================================
DNS RECORDS REQUIRED FOR SES VERIFICATION
============================================

1. TXT Record (Domain Verification):
   Name:  _amazonses.yourdomain.com
   Value: <verification-token>

2. CNAME Records (DKIM - add all 3):
   <token1>._domainkey.yourdomain.com -> <token1>.dkim.amazonses.com
   <token2>._domainkey.yourdomain.com -> <token2>.dkim.amazonses.com
   <token3>._domainkey.yourdomain.com -> <token3>.dkim.amazonses.com
============================================
EOT
```

## Step 3: Add DNS Records

Add the DNS records output by Terraform to your domain registrar:

### TXT Record (Domain Verification)
| Type | Name | Value |
|------|------|-------|
| TXT | `_amazonses.yourdomain.com` | `<verification-token from terraform output>` |

### CNAME Records (DKIM - all 3 required)
| Type | Name | Value |
|------|------|-------|
| CNAME | `<token1>._domainkey.yourdomain.com` | `<token1>.dkim.amazonses.com` |
| CNAME | `<token2>._domainkey.yourdomain.com` | `<token2>.dkim.amazonses.com` |
| CNAME | `<token3>._domainkey.yourdomain.com` | `<token3>.dkim.amazonses.com` |

## Step 4: Verify Domain Status

Wait for DNS propagation (5-30 minutes), then check verification status:

```bash
# Check domain verification status
aws ses get-identity-verification-attributes \
  --identities yourdomain.com \
  --region us-east-1

# Expected output when verified:
{
    "VerificationAttributes": {
        "yourdomain.com": {
            "VerificationStatus": "Success"
        }
    }
}
```

Check DKIM status:

```bash
aws ses get-identity-dkim-attributes \
  --identities yourdomain.com \
  --region us-east-1

# Expected output when verified:
{
    "DkimAttributes": {
        "yourdomain.com": {
            "DkimEnabled": true,
            "DkimVerificationStatus": "Success",
            "DkimTokens": ["token1", "token2", "token3"]
        }
    }
}
```

## Step 5: Exit SES Sandbox

By default, new AWS accounts are in **SES Sandbox mode**, which means:
- You can only send emails to verified email addresses
- Daily sending limit is 200 emails
- Maximum 1 email per second

### Request Production Access

1. Open the AWS Console → SES → Account Dashboard
2. Click **Request production access**
3. Fill out the form:
   - **Mail type**: Transactional
   - **Website URL**: Your application URL
   - **Use case description**: Example:
     ```
     Forever Home is a pet adoption platform. We send transactional emails for:
     - Email verification when users register
     - Password reset links
     - Welcome emails to new users
     - Adoption status notifications

     We implement double opt-in for all users and follow email best practices.
     ```
4. Submit and wait for approval (typically 24-48 hours)

### While in Sandbox (for testing)

You can verify individual email addresses for testing:

```bash
# Verify a test email address
aws ses verify-email-identity \
  --email-address test@example.com \
  --region us-east-1
```

## Step 6: Deploy Application

After domain verification is complete, deploy the application:

```bash
# Build and push Docker image
docker build -t $(terraform output -raw ecr_repository_url):latest .
docker push $(terraform output -raw ecr_repository_url):latest

# Force new deployment
aws ecs update-service \
  --cluster $(terraform output -raw ecs_cluster_name) \
  --service $(terraform output -raw ecs_service_name) \
  --force-new-deployment
```

## Step 7: Test Email Delivery

Test the verification email flow:

```bash
# Register a new user
curl -X POST https://your-api-url/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "testuser@example.com",
    "password": "SecurePass123!",
    "name": "Test User",
    "role": "ADOPTER"
  }'
```

The user should receive a verification email.

## Monitoring

### CloudWatch Metrics

SES metrics are automatically sent to CloudWatch with the configuration set:

- **Sends**: Total emails sent
- **Deliveries**: Successfully delivered emails
- **Bounces**: Emails that bounced
- **Complaints**: Spam complaints
- **Rejects**: Emails rejected by SES

View metrics in CloudWatch → Metrics → SES → Configuration Set Metrics

### Email Sending Statistics

```bash
# Get current sending statistics
aws ses get-send-statistics --region us-east-1

# Get sending quota
aws ses get-send-quota --region us-east-1
```

## Troubleshooting

### "Email address is not verified"
- You're in SES Sandbox mode and trying to send to an unverified email
- Either verify the recipient email or request production access

### "Domain is not verified"
- DNS records haven't propagated yet (wait 5-30 minutes)
- DNS records are incorrectly configured (double-check values)
- Wrong region (SES is region-specific)

### Emails going to spam
- Ensure DKIM is enabled and verified
- Check your domain's SPF record
- Review email content for spam triggers
- Monitor and address bounce/complaint rates

### No emails being sent
- Check application logs for errors
- Verify `EMAIL_PROVIDER=ses` environment variable is set
- Verify IAM permissions allow `ses:SendEmail`

## Environment Variables Reference

| Variable | Description | Example |
|----------|-------------|---------|
| `EMAIL_PROVIDER` | Email service provider | `ses` |
| `EMAIL_FROM` | Sender email address | `noreply@yourdomain.com` |
| `AWS_SES_CONFIGURATION_SET` | SES configuration set name | `forever-home-dev-emails` |
| `AWS_REGION` | AWS region for SES | `us-east-1` |

## Security Considerations

- Never commit AWS credentials to version control
- Use IAM roles for ECS tasks (configured automatically by Terraform)
- Monitor bounce and complaint rates to maintain sender reputation
- Implement rate limiting on email endpoints (already done in the application)
- Use TLS for all SES communications (enforced by configuration set)
