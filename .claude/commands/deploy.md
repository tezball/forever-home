# Deploy to AWS

Deploy the application to AWS ECS Fargate.

## Instructions

Run the deploy script to build and deploy the application:

```bash
./deploy.sh
```

This will:
1. Validate AWS credentials and prerequisites
2. Login to ECR (Elastic Container Registry)
3. Build and push the Docker image
4. **Reset database to seed data** (drops all tables, Flyway recreates schema, seeds demo data)
5. Force ECS to restart with the new image

## Options

```bash
./deploy.sh [--tag TAG] [--no-reset] [--blank]
```

- `--tag, -t TAG` - Docker image tag (default: latest)
- `--no-reset` - Skip database reset (preserves existing data)
- `--blank` - Deploy with empty database (no demo data, no demo login dropdown)

## Examples

Deploy with demo data (default):
```bash
./deploy.sh
```

Deploy without resetting the database:
```bash
./deploy.sh --no-reset
```

Deploy with blank database (production-ready, no demo content):
```bash
./deploy.sh --blank
```

Deploy with a custom image tag:
```bash
./deploy.sh --tag v1.0.0
```

## Deployment Modes

| Mode | Database | Demo Data | Demo Login |
|------|----------|-----------|------------|
| Default (`./deploy.sh`) | Reset | Seeded | Visible |
| `--no-reset` | Preserved | Unchanged | Unchanged |
| `--blank` | Reset (empty) | None | Hidden |

## After deployment

Monitor the deployment progress with:
```bash
aws ecs describe-services --cluster <cluster-name> --services <service-name> --query 'services[0].deployments'
```

View application logs with:
```bash
aws logs tail /ecs/<cluster-name> --follow
```
