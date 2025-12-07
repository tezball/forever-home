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
```

## Architecture

Standard Spring Boot layered architecture:
- `src/main/java/com/example/foreverhome/` - Main application code
- `src/test/java/` - Test classes
- `src/main/resources/application.properties` - Configuration
- `compose.yaml` - Docker Compose for PostgreSQL (auto-started by Spring Boot DevTools)
