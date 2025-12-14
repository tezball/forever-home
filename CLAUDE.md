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

## Common Commands

```bash
# Run application (starts PostgreSQL via Docker Compose automatically)
./mvnw spring-boot:run

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=ForeverHomeApplicationTests

# Run a single test method
./mvnw test -Dtest=ForeverHomeApplicationTests#contextLoads

# Build the project
./mvnw package

# Build native image
./mvnw spring-boot:build-image -Pnative

# Compile native executable (requires GraalVM 25+)
./mvnw native:compile -Pnative

# Run tests in native image
./mvnw test -PnativeTest

# Run Gatling load tests (default: user registration simulation)
./mvnw gatling:test

# Run specific Gatling simulation
./mvnw gatling:test -Dgatling.simulationClass=com.example.foreverhome.simulation.UserRegistrationSimulation

# Run stress test simulation
./mvnw gatling:test -Dgatling.simulationClass=com.example.foreverhome.simulation.RegistrationStressSimulation

# Gatling with custom parameters
./mvnw gatling:test -DBASE_URL=http://localhost:8080 -DUSERS=20 -DRAMP_DURATION=30
```

## Development Script

Use `./dev.sh` to manage local development environment:

```bash
# Start all services with LocalStack S3 (default)
./dev.sh start

# Start all services with real AWS S3
./dev.sh start --s3

# Stop all services (preserves data)
./dev.sh stop

# Stop and remove all data (fresh start)
./dev.sh clean

# Restart with clean data
./dev.sh restart
./dev.sh restart --s3    # With AWS S3

# Check service status
./dev.sh status

# Run Gatling load tests
./dev.sh gatling

# Run Playwright E2E tests
./dev.sh e2e
```

## S3 Storage Profiles

```bash
# LocalStack S3 (default) - fully local, no AWS required
./dev.sh start

# AWS S3 mode - uses real AWS S3 (requires .env file)
cp .env.example .env  # Edit with your AWS credentials
./dev.sh start --s3

# Or manually with environment variables
export AWS_ACCESS_KEY_ID=your-key
export AWS_SECRET_ACCESS_KEY=your-secret
export AWS_REGION=eu-west-1
export AWS_S3_BUCKET=your-bucket
./mvnw spring-boot:run -Dspring-boot.run.profiles=s3-aws
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
