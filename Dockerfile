# Forever Home Application Dockerfile
# Multi-stage build for GraalVM native image

# Stage 1: Build frontend
FROM node:22-alpine AS frontend-builder
WORKDIR /frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./

# Set environment variables for the frontend build
# VITE_API_URL is relative so it works with any host
# VITE_TEST_MODE enables the quick login dropdown for testing
ENV VITE_API_URL=/api
ENV VITE_TEST_MODE=true

RUN npm run build -- --outDir dist

# Stage 2: Build native image with GraalVM
FROM ghcr.io/graalvm/native-image-community:25 AS builder

# Install git for git-commit-id-maven-plugin
RUN microdnf install -y git && microdnf clean all

WORKDIR /app

# Copy Maven wrapper and pom.xml first for dependency caching
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Copy .git directory for git-commit-id-maven-plugin to generate git info
COPY .git .git

# Download dependencies (cached if pom.xml doesn't change)
RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -B

# Copy source code
COPY src src

# Copy frontend build into static resources
COPY --from=frontend-builder /frontend/dist/ src/main/resources/static/

# Build the native image (skip tests and frontend plugin - frontend already built in stage 1)
# Use prod profile during AOT to avoid Loki connection attempts
ENV SPRING_PROFILES_ACTIVE=prod
RUN ./mvnw -Pnative package -Dmaven.test.skip=true -Dskip.frontend=true -B

# Stage 3: Runtime - minimal image with just the native executable
FROM debian:bookworm-slim

WORKDIR /app

# Add non-root user for security
RUN groupadd -g 1001 appgroup && \
    useradd -u 1001 -g appgroup -d /app appuser

# Install curl for health checks and ca-certificates for HTTPS
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl ca-certificates && \
    rm -rf /var/lib/apt/lists/*

# Copy the native executable from builder stage
COPY --from=builder /app/target/forever-home /app/forever-home

# Change ownership to non-root user
RUN chown -R appuser:appgroup /app && chmod +x /app/forever-home

# Switch to non-root user
USER appuser

# Expose the application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the native executable
ENTRYPOINT ["/app/forever-home"]
