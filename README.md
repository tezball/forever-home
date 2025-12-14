# Forever Home

A pet adoption platform that connects pet owners looking to rehome their pets with adopters through rescue organizations.

## Quick Start

```bash
# Start local development (PostgreSQL, LocalStack, Mailpit via Docker)
docker compose up -d

# Run the application
./mvnw spring-boot:run

# Access the app
open http://localhost:8080
```

## Development Setup

### Prerequisites

- Java 25+
- Docker & Docker Compose
- Node.js 18+ (for frontend development)
- AWS CLI v2 (optional, for AWS S3 mode)

### Local Development (Default)

By default, the application uses LocalStack for S3 storage. All data stays on your machine.

```bash
# Start all services
docker compose up -d

# Run the application
./mvnw spring-boot:run
```

Services started by Docker Compose:
- **PostgreSQL** - Database (port 5432)
- **LocalStack** - Local AWS S3/SES (port 4566)
- **Mailpit** - Email testing UI (port 8025)
- **Grafana** - Monitoring dashboard (port 3000)
- **Prometheus** - Metrics (port 9090)
- **Loki** - Log aggregation (port 3100)

---

## AWS S3 Setup (Step-by-Step)

Follow these steps to use real AWS S3 instead of LocalStack for image storage.

### Step 1: Create an IAM User

1. Go to **AWS Console** → **IAM** → **Users**
2. Click **Create user**
3. Enter a username (e.g., `forever-home-dev`)
4. Click **Next**
5. Select **Attach policies directly**
6. Search for and select **AmazonS3FullAccess**
7. Click **Next** → **Create user**

### Step 2: Generate Access Keys

1. Click on the user you just created
2. Go to **Security credentials** tab
3. Under **Access keys**, click **Create access key**
4. Select **Application running outside AWS**
5. Click **Next** → **Create access key**
6. **Copy the Access Key ID and Secret Access Key** (you won't see the secret again!)

### Step 3: Configure .env File

```bash
# Copy the example file
cp .env.example .env

# Edit with your credentials
nano .env  # or use your preferred editor
```

Update `.env` with your values:
```
AWS_ACCESS_KEY_ID=AKIA...your-key
AWS_SECRET_ACCESS_KEY=your-secret-key
AWS_REGION=eu-west-1
AWS_S3_BUCKET=forever-home-images-dev
```

### Step 4: Create an S3 Bucket

```bash
# Create the bucket in eu-west-1 (choose a globally unique name)
aws s3 mb s3://forever-home-images-dev --region eu-west-1

# Configure public read access (for serving images)
aws s3api put-public-access-block \
  --bucket forever-home-images-dev \
  --region eu-west-1 \
  --public-access-block-configuration \
  "BlockPublicAcls=false,IgnorePublicAcls=false,BlockPublicPolicy=false,RestrictPublicBuckets=false"

# Add bucket policy for public read
cat <<EOF | aws s3api put-bucket-policy --bucket forever-home-images-dev --region eu-west-1 --policy file:///dev/stdin
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "PublicReadGetObject",
      "Effect": "Allow",
      "Principal": "*",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::forever-home-images-dev/*"
    }
  ]
}
EOF
```

### Step 5: Run the Application with AWS S3

```bash
./scripts/run-with-aws-s3.sh
```

The script will:
1. Load credentials from `.env`
2. Validate the configuration
3. Start the application with the `s3-aws` Spring profile

### Step 6: Verify S3 Integration

After uploading an image through the app, verify it's in S3:

```bash
aws s3 ls s3://forever-home-images-dev/pets/ --region eu-west-1 --recursive
```

---

## Manual AWS S3 Configuration

If you prefer not to use the helper script:

```bash
# Set environment variables
export AWS_ACCESS_KEY_ID=your-key
export AWS_SECRET_ACCESS_KEY=your-secret
export AWS_REGION=eu-west-1
export AWS_S3_BUCKET=forever-home-images-dev

# Run with s3-aws profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=s3-aws
```

---

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `AWS_ACCESS_KEY_ID` | AWS access key | `test` (LocalStack) |
| `AWS_SECRET_ACCESS_KEY` | AWS secret key | `test` (LocalStack) |
| `AWS_REGION` | AWS region | `eu-west-1` |
| `AWS_S3_BUCKET` | S3 bucket name | `forever-home-images-dev` |
| `AWS_ENDPOINT` | Custom endpoint (LocalStack) | `http://localhost:4566` |

---

## Spring Profiles

| Profile | Description |
|---------|-------------|
| (default) | LocalStack for S3, local PostgreSQL |
| `s3-aws` | Real AWS S3, local PostgreSQL |
| `dev` | AWS-deployed development environment |
| `prod` | Production environment |

---

## Project Structure

```
forever-home/
├── src/main/java/          # Java backend
├── src/main/resources/     # Configuration files
├── frontend/               # React frontend
├── scripts/                # Helper scripts
│   ├── localstack-init.sh  # LocalStack bucket setup
│   └── run-with-aws-s3.sh  # AWS S3 helper
├── docs/                   # Documentation
└── compose.yaml            # Docker Compose services
```

---

## Testing

```bash
# Run all tests
./mvnw test

# Run a specific test class
./mvnw test -Dtest=PetImageServiceTest

# Run with coverage
./mvnw test jacoco:report
```

---

## Useful Commands

```bash
# Check LocalStack S3 contents
docker exec forever-home-localstack-1 awslocal s3 ls s3://forever-home-images/ --recursive

# View application logs
docker compose logs -f

# Reset LocalStack data
rm -rf localstack-data && docker compose restart localstack

# Build frontend
cd frontend && npm run build
```

---

## Documentation

- [CLAUDE.md](CLAUDE.md) - AI assistant instructions
- [docs/user-stories.md](docs/user-stories.md) - User stories and acceptance criteria
- [docs/domain-model.md](docs/domain-model.md) - Entity definitions and relationships
- [docs/pet-status.md](docs/pet-status.md) - Pet status lifecycle
