# Forever Home - Production Deployment Guide

This guide covers deploying Forever Home to a production environment.

## Prerequisites

- Java 25+ (GraalVM recommended for native images)
- PostgreSQL 15+
- AWS Account (for S3 and SES)
- Domain with SSL certificate

## Environment Variables

The following environment variables must be set for production:

### Required Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `prod` |
| `DATABASE_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://host:5432/foreverhome` |
| `DATABASE_USERNAME` | Database username | `foreverhome_user` |
| `DATABASE_PASSWORD` | Database password | `<secure-password>` |
| `JWT_SECRET` | JWT signing secret (min 256 bits) | `<64-char-random-string>` |
| `AWS_ACCESS_KEY_ID` | AWS access key | `AKIA...` |
| `AWS_SECRET_ACCESS_KEY` | AWS secret key | `<secret>` |
| `AWS_REGION` | AWS region | `us-east-1` |
| `AWS_S3_BUCKET` | S3 bucket for images | `foreverhome-images-prod` |
| `APP_BASE_URL` | Frontend URL | `https://foreverhome.com` |
| `CORS_ALLOWED_ORIGINS` | Allowed CORS origins | `https://foreverhome.com` |
| `EMAIL_FROM` | Sender email for notifications | `noreply@foreverhome.com` |
| `ADMIN_EMAIL` | Initial admin email (created on startup) | `admin@foreverhome.com` |

### Optional Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `AWS_ENDPOINT` | Custom AWS endpoint (for LocalStack) | AWS default |
| `SERVER_PORT` | Application port | `8080` |

## Database Setup

### 1. Create PostgreSQL Database

```sql
CREATE DATABASE foreverhome;
CREATE USER foreverhome_user WITH ENCRYPTED PASSWORD 'your-secure-password';
GRANT ALL PRIVILEGES ON DATABASE foreverhome TO foreverhome_user;
```

### 2. Run Migrations

Flyway migrations run automatically on startup. For manual execution:

```bash
./mvnw flyway:migrate -Dflyway.url=jdbc:postgresql://host:5432/foreverhome \
  -Dflyway.user=foreverhome_user \
  -Dflyway.password=your-password
```

## AWS Setup

### S3 Bucket Configuration

1. Create an S3 bucket for pet images:

```bash
aws s3 mb s3://foreverhome-images-prod --region us-east-1
```

2. Configure CORS for the bucket:

```json
{
  "CORSRules": [
    {
      "AllowedOrigins": ["https://foreverhome.com"],
      "AllowedMethods": ["GET", "PUT"],
      "AllowedHeaders": ["*"],
      "MaxAgeSeconds": 3600
    }
  ]
}
```

3. Configure bucket policy for public read access to images:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "PublicReadGetObject",
      "Effect": "Allow",
      "Principal": "*",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::foreverhome-images-prod/*"
    }
  ]
}
```

### SES Configuration

1. Verify your sending domain in SES
2. Request production access (to send to unverified emails)
3. Configure the sender email address

## Building the Application

### Standard JAR Build

```bash
./mvnw clean package -DskipTests
```

Output: `target/forever-home-0.0.1-SNAPSHOT.jar`

### Native Image Build (GraalVM)

```bash
./mvnw native:compile -Pnative -DskipTests
```

Output: `target/forever-home`

### Docker Image Build

```bash
./mvnw spring-boot:build-image -Pnative
```

## Running the Application

### JAR Execution

```bash
java -jar target/forever-home-0.0.1-SNAPSHOT.jar
```

### Native Executable

```bash
./target/forever-home
```

### Docker

```bash
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DATABASE_URL=jdbc:postgresql://host:5432/foreverhome \
  -e DATABASE_USERNAME=user \
  -e DATABASE_PASSWORD=pass \
  -e JWT_SECRET=your-secret \
  ... \
  forever-home:latest
```

## Health Checks

The application exposes health endpoints:

- **Health**: `GET /actuator/health`
- **Info**: `GET /actuator/info`

Example health check response:

```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" }
  }
}
```

## Monitoring

### Prometheus Metrics

The application exposes Prometheus metrics at `/actuator/prometheus`. Configure your Prometheus server to scrape this endpoint.

#### Custom Business Metrics

The application tracks the following custom business metrics:

| Metric | Type | Description | Labels |
|--------|------|-------------|--------|
| `foreverhome_adoptions_completed_total` | Counter | Total completed adoptions | - |
| `foreverhome_applications_submitted_total` | Counter | Total applications submitted | - |
| `foreverhome_applications_approved_total` | Counter | Total applications approved | - |
| `foreverhome_applications_rejected_total` | Counter | Total applications rejected | - |
| `foreverhome_users_registrations_total` | Counter | Total user registrations | `role` |
| `foreverhome_pets_registrations_total` | Counter | Total pets registered | `species` |
| `foreverhome_vet_signoffs_total` | Counter | Total vet sign-offs | `result` (approved/declined) |
| `foreverhome_emails_sent_total` | Counter | Total emails sent | `type` |
| `foreverhome_emails_failed_total` | Counter | Total failed emails | `type` |
| `foreverhome_ratelimit_hits_total` | Counter | Rate limit violations | `endpoint` |
| `foreverhome_auth_login_attempts_total` | Counter | Login attempts | `result` (success/failure) |
| `foreverhome_pets_status_transitions_total` | Counter | Pet status changes | `from`, `to` |
| `foreverhome_application_processing_time` | Timer | Time to process applications | - |

#### Gauge Metrics (Live Database Queries)

| Metric | Description |
|--------|-------------|
| `foreverhome_users_active` | Number of active users |
| `foreverhome_pets_available` | Number of pets available for adoption |
| `foreverhome_applications_pending` | Number of pending applications |
| `foreverhome_rescueorgs_pending` | Number of rescue orgs pending approval |

#### Standard Spring Boot Metrics

In addition, all standard Spring Boot Actuator metrics are available:
- `http_server_requests` - HTTP request metrics with percentiles
- `jvm_memory_used` - JVM memory usage
- `jvm_gc_pause` - Garbage collection pause times
- `system_cpu_usage` - System CPU utilization

### Grafana Dashboard

A pre-built Grafana dashboard is available at `infra/grafana/dashboards/forever-home.json`. Import this dashboard to visualize:

- Adoption funnel (applications → approvals → completions)
- User registrations by role
- Pet registrations by species
- Authentication success/failure rates
- Rate limiting events
- Email delivery success rates

### Logging

Logs are output in JSON format in production. Configure your log aggregator to collect from stdout.

For Grafana Loki integration, set the `LOKI_URL` environment variable:

```bash
export LOKI_URL=http://loki:3100/loki/api/v1/push
```

Log labels include:
- `app` - Application name (forever-home)
- `env` - Environment (dev/prod)
- `level` - Log level
- `host` - Hostname (production only)

Structured log fields:
- `traceId` - Request trace ID for correlation
- `userId` - Authenticated user ID
- `userRole` - User's role
- `clientIp` - Client IP address
- `requestMethod` - HTTP method
- `requestPath` - Request URI
- `responseStatus` - HTTP response status
- `durationMs` - Request duration in milliseconds

### Alerting

Example Prometheus alerting rules:

```yaml
groups:
  - name: foreverhome
    rules:
      - alert: HighRateLimitViolations
        expr: increase(foreverhome_ratelimit_hits_total[5m]) > 50
        for: 1m
        labels:
          severity: warning
        annotations:
          summary: "High rate limit violations detected"

      - alert: HighEmailFailureRate
        expr: rate(foreverhome_emails_failed_total[5m]) / rate(foreverhome_emails_sent_total[5m]) > 0.1
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Email failure rate exceeds 10%"

      - alert: HighLoginFailureRate
        expr: rate(foreverhome_auth_login_attempts_total{result="failure"}[5m]) / rate(foreverhome_auth_login_attempts_total[5m]) > 0.3
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Login failure rate exceeds 30%"
```

## Security Checklist

Before going live, verify:

- [ ] JWT_SECRET is a strong, unique secret (min 64 random characters)
- [ ] Database credentials are secure and unique
- [ ] CORS_ALLOWED_ORIGINS only includes your production domain
- [ ] HTTPS is enforced at the load balancer/proxy level
- [ ] Database is not publicly accessible
- [ ] S3 bucket does not have overly permissive policies
- [ ] SES is in production mode (not sandbox)
- [ ] Email verification is enabled (`app.email.verification.auto-activate=false`)
- [ ] Test mode is disabled (`app.test-mode.enabled=false`)

## Generating a Secure JWT Secret

```bash
openssl rand -base64 64 | tr -d '\n'
```

## Troubleshooting

### Common Issues

**Database connection refused**
- Verify DATABASE_URL is correct
- Check database server is running and accessible
- Verify security groups/firewall rules

**JWT signature invalid**
- Ensure JWT_SECRET is the same across all instances
- Secret must be at least 256 bits (32 characters)

**S3 upload fails**
- Verify AWS credentials have s3:PutObject permission
- Check bucket CORS configuration
- Verify bucket name is correct

**Emails not sending**
- Verify SES is out of sandbox mode
- Check sender email is verified
- Verify AWS credentials have ses:SendEmail permission

### Getting Help

For issues not covered here:
- Check application logs
- Review Spring Boot Actuator health endpoint
- Contact support at support@foreverhome.local
