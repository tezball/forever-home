# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Forever Home is a pet adoption platform that connects pet owners looking to rehome their pets with adopters through rescue organizations. Key domain concepts:
- **Foster**: Registers pets for adoption
- **Rescue Organization**: Manages pets and facilitates adoptions
- **Vet**: Signs off on pets (must be neutered, vaccinated, healthy)
- **Adopter**: Inherits a pet from a foster through a rescue after vet sign-off

## Tech Stack

- Spring Boot 4.0.0 with Java 25
- Spring Data JDBC for database access
- PostgreSQL database
- JWT authentication (access token 15min + refresh token 7-30 days)
- Spring Boot Actuator for monitoring
- GraalVM Native Image support
- Docker Compose for local development

## Development CLI

There are two CLI options:
- **`./dev.sh`** - Bash script for quick commands
- **`fh`** - Spring Shell CLI with interactive features and more commands

### Bash Script (`./dev.sh`)

Use `./dev.sh` for quick development tasks. Run `./dev.sh --help` for full options.

```bash
# Local Development
./dev.sh start           # Start app + Docker services (LocalStack S3)
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
./dev.sh test gatling             # Run load tests (all user flows)
./dev.sh test gatling stress      # Run stress test

# Building
./dev.sh build              # Build JAR
./dev.sh build docker       # Build Docker image
./dev.sh build native       # Build GraalVM native image

# Deployment
./dev.sh deploy             # Deploy to AWS ECS
./dev.sh deploy --no-reset  # Deploy without DB reset
./dev.sh deploy --blank     # Production deploy (no demo data)
./dev.sh deploy infra       # Provision AWS infrastructure
./dev.sh deploy destroy     # Tear down all infrastructure
```

### Spring Shell CLI (`fh`)

The `fh` CLI provides an interactive shell with additional features. Build and run with:

```bash
cd cli && ../mvnw package -DskipTests && ./fh
```

#### Dev Commands
```bash
fh dev start              # Start all development services
fh dev start --s3         # Use AWS S3 instead of LocalStack
fh dev stop               # Stop all services (preserves data)
fh dev clean              # Stop services and remove all data
fh dev restart            # Clean restart (stop + clean + start)
fh dev status             # Show service status
fh dev logs               # View logs for a service
fh dev logs -s postgres   # View logs for specific service (postgres, localstack, grafana, loki, mailpit)
fh dev logs -f            # Follow log output
fh dev db                 # Show database utilities help
fh dev db-reset           # Reset database to seed data
fh dev db-shell           # Open psql shell
fh dev moderation-reset   # Reset all pet moderation statuses to PENDING for AI re-check
```

#### Test Commands
```bash
fh test unit              # Run all unit tests
fh test unit -c MyTest    # Run specific test class
fh test unit -c MyTest -m testMethod  # Run specific test method
fh test build             # Build the project
fh test build -T          # Build skipping tests
fh test e2e               # Run all Playwright E2E tests
fh test e2e --suite auth  # Run specific test suite
fh test e2e --headed      # Run in headed browser mode
fh test e2e --ui          # Open Playwright UI mode
fh test e2e --debug       # Run in debug mode
fh test e2e-report        # Open Playwright HTML report
fh test e2e-suites        # List available E2E test suites
fh test gatling           # Run Gatling load tests
fh test gatling -s stress # Run stress simulation
fh test gatling -u 50     # Set number of users
fh test gatling-sims      # List available Gatling simulations
```

#### Deploy Commands
```bash
fh deploy aws             # Full AWS deployment (Terraform + Docker + ECS)
fh deploy aws -y          # Auto-approve Terraform changes
fh deploy aws --skip-infra    # Skip Terraform infrastructure deployment
fh deploy aws --skip-build    # Skip Docker image build
fh deploy build           # Build Docker image only
fh deploy build -t v1.0   # Build with specific tag
fh deploy push            # Push image to ECR and trigger ECS deployment
fh deploy push --no-deploy    # Push without triggering ECS deployment
fh deploy status          # Check deployment status
fh deploy logs            # View ECS service logs
fh deploy logs -f         # Follow ECS log output
fh deploy destroy         # Destroy AWS infrastructure (requires --confirm)
```

#### Config Commands
```bash
fh config show            # Display current configuration
fh config set -k aws.region -v us-west-2  # Set a configuration value
fh config get -k aws.region   # Get a configuration value
fh config unset -k aws.region # Remove a configuration value
fh config reset           # Reset to default configuration
fh config edit            # Open configuration file in editor
fh config env             # Show environment status (Java, AWS, Docker, Terraform)
fh config path            # Show project paths
```

#### Docs Commands
```bash
fh docs                   # Browse documentation interactively
fh docs list              # List all available documentation
fh docs show -t pet-status    # Display a documentation topic
fh docs search -q "pet status"    # Search documentation
fh docs open -t pet-status    # Open documentation in external viewer
fh docs quickstart        # Show quick start guide
fh docs architecture      # Show architecture overview
fh docs pet-status        # Show pet status lifecycle
```

### Moderation Service CLI

The moderation-service has its own CLI for AI-powered content moderation. Requires Ollama running locally.

```bash
# Build and run
cd moderation-service && ../mvnw package -DskipTests && ./moderate
```

#### Moderate Commands
```bash
moderate check-api                    # Check if Forever Home API is available
moderate pet --pet-id <UUID>          # Moderate a single pet
moderate batch --limit 100            # Run batch moderation on available pets
moderate batch --limit 50 --text-only     # Text-only moderation
moderate batch --limit 50 --images-only   # Images-only moderation
moderate flagged --limit 50           # View flagged content
moderate flagged --category NOT_PET   # View flagged by category
moderate review --result-id <UUID> --action approve   # Approve flagged content
moderate review --result-id <UUID> --action reject --notes "Reason"  # Reject with notes
moderate stats                        # View moderation statistics
moderate jobs --limit 10              # View recent moderation jobs
```

### S3 Storage Modes

```bash
# LocalStack S3 (default) - fully local, no AWS required
./dev.sh start

# AWS S3 mode - uses real AWS S3 (requires .env file)
cp .env.example .env  # Edit with your AWS credentials
./dev.sh start --s3
```

## Architecture

Standard Spring Boot layered architecture:
- `src/main/java/com/example/foreverhome/` - Main application code
- `src/test/java/` - Test classes
- `src/main/resources/application.properties` - Configuration
- `compose.yaml` - Docker Compose for PostgreSQL (auto-started by Spring Boot DevTools)

## Key Design Decisions

- **JWT Auth**: Stateless authentication with short-lived access tokens (15min) and refresh tokens in httpOnly cookies
- **Microchip-based vet lookup**: Vets find pets by microchip number rather than being assigned by rescues
- **Pet status state machine**: Pets flow through Draft → PendingRescue → PendingVet → Available → InProgress → Adopted
- **Microchip required**: All pets must have a microchip ID (used for vet lookup and ownership tracking)
- **Profile completion**: Users complete role-specific profile after initial registration

## Infrastructure

- **Image Storage**: AWS S3 for pet images and organization logos
- **Email Service**: AWS SES for transactional emails (notifications, password reset)
- **Admin Bootstrap**: First admin created via environment variable `ADMIN_EMAIL` on startup

## Documentation

See `docs/` for detailed specifications:
- `user-stories.md` - All user stories with acceptance criteria and UI specs
- `domain-model.md` - Entity definitions, relationships, and JWT auth spec
- `pet-status.md` - Pet status lifecycle and transitions
- `ui-style-guide.md` - Design system (colors, typography, components)
- `src/main/resources/static/style-guide.html` - Living component library
