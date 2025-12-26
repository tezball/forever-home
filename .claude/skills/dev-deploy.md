---
description: Run development and deployment tasks using the project CLI tools (./dev.sh and fh)
---

Execute all development and deployment tasks using the project's CLI tools instead of raw commands.

## CLI Tools

### ./dev.sh (Quick Commands)
Use for single, quick operations:

```bash
# Local Development
./dev.sh start           # Start app + Docker services
./dev.sh start --s3      # Start with real AWS S3
./dev.sh stop            # Stop all services
./dev.sh restart         # Restart with clean data
./dev.sh clean           # Stop and remove all data
./dev.sh status          # Show service status

# Testing
./dev.sh test                     # Run all unit tests
./dev.sh test unit PetServiceTest # Run specific test class
./dev.sh test e2e                 # Run all Playwright E2E tests
./dev.sh test e2e auth            # Run auth E2E tests only
./dev.sh test e2e ui              # Interactive Playwright UI
./dev.sh test gatling             # Run load tests

# Building
./dev.sh build              # Build JAR
./dev.sh build docker       # Build Docker image
./dev.sh build native       # Build GraalVM native image

# Deployment
./dev.sh deploy             # Deploy to AWS ECS
./dev.sh deploy --no-reset  # Deploy without DB reset
./dev.sh deploy infra       # Provision AWS infrastructure
./dev.sh deploy destroy     # Tear down infrastructure
```

### fh (Spring Shell CLI)
Use for interactive features, complex workflows, and additional commands:

```bash
# Build CLI first
cd cli && ../mvnw package -DskipTests && java -jar target/forever-home-cli-*.jar

# Dev Commands
fh dev start              # Start all development services
fh dev stop               # Stop all services
fh dev clean              # Stop and remove all data
fh dev restart            # Clean restart
fh dev status             # Show service status
fh dev logs               # View logs
fh dev logs -s postgres   # View specific service logs
fh dev db-reset           # Reset database to seed data
fh dev db-shell           # Open psql shell

# Test Commands
fh test unit              # Run all unit tests
fh test unit -c MyTest    # Run specific test class
fh test e2e               # Run E2E tests
fh test e2e --suite auth  # Run specific suite
fh test e2e --ui          # Open Playwright UI
fh test gatling           # Run load tests

# Deploy Commands
fh deploy aws             # Full AWS deployment
fh deploy aws -y          # Auto-approve Terraform
fh deploy aws --skip-infra    # Skip Terraform
fh deploy build           # Build Docker image
fh deploy push            # Push to ECR + deploy
fh deploy status          # Check deployment status
fh deploy logs            # View ECS logs
fh deploy destroy         # Destroy infrastructure
```

## Rules

1. **ALWAYS** use `./dev.sh` or `fh` for these operations
2. **NEVER** run these raw commands directly:
   - `./mvnw` - Use `./dev.sh build` or `fh test build`
   - `docker compose` - Use `./dev.sh start/stop`
   - `terraform` - Use `./dev.sh deploy infra` or `fh deploy aws`
   - `npm test` or `npx playwright` - Use `./dev.sh test e2e`

## When to Use Which

| Task | Use |
|------|-----|
| Quick start/stop | `./dev.sh start/stop` |
| Run tests | `./dev.sh test` |
| Build JAR/Docker | `./dev.sh build` |
| Simple deploy | `./dev.sh deploy` |
| View logs | `fh dev logs` or `fh deploy logs` |
| Database operations | `fh dev db-reset` or `fh dev db-shell` |
| Complex deploy workflow | `fh deploy aws` |
| Interactive debugging | `fh` (shell mode) |

## Examples

Starting local development:
```bash
./dev.sh start
```

Running a specific test:
```bash
./dev.sh test unit PetServiceTest
```

Deploying to AWS with auto-approve:
```bash
fh deploy aws -y
```

Checking ECS logs after deployment:
```bash
fh deploy logs -f
```
