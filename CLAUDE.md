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

Use `./dev.sh` for all development tasks. Run `./dev.sh --help` for full options.

### Local Development

```bash
./dev.sh start           # Start app + Docker services (LocalStack S3)
./dev.sh start --s3      # Start with real AWS S3
./dev.sh stop            # Stop all services
./dev.sh restart         # Restart with clean data
./dev.sh clean           # Stop and remove all data
./dev.sh status          # Show service status
```

### Testing

```bash
./dev.sh test                     # Run all unit tests
./dev.sh test unit PetServiceTest # Run specific test class
./dev.sh test e2e                 # Run all Playwright E2E tests
./dev.sh test e2e auth            # Run auth E2E tests only
./dev.sh test e2e ui              # Interactive Playwright UI
./dev.sh test gatling             # Run load tests (all user flows)
./dev.sh test gatling stress      # Run stress test
```

### Building

```bash
./dev.sh build              # Build JAR
./dev.sh build docker       # Build Docker image
./dev.sh build native       # Build GraalVM native image
```

### Deployment

```bash
./dev.sh deploy             # Deploy to AWS ECS
./dev.sh deploy --no-reset  # Deploy without DB reset
./dev.sh deploy --blank     # Production deploy (no demo data)
./dev.sh deploy infra       # Provision AWS infrastructure
./dev.sh deploy destroy     # Tear down all infrastructure
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
