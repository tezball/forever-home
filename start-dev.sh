#!/bin/bash

# Forever Home - Local Development Startup Script
# This script starts all services needed to run the app locally

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Project root directory
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo -e "${BLUE}================================================${NC}"
echo -e "${BLUE}   Forever Home - Local Development Setup${NC}"
echo -e "${BLUE}================================================${NC}"
echo

# Function to check if a command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to check if a port is in use
port_in_use() {
    lsof -i:"$1" >/dev/null 2>&1
}

# Function to wait for a service to be ready
wait_for_service() {
    local host=$1
    local port=$2
    local service_name=$3
    local max_attempts=30
    local attempt=1

    echo -e "${YELLOW}Waiting for $service_name to be ready...${NC}"
    while ! nc -z "$host" "$port" 2>/dev/null; do
        if [ $attempt -ge $max_attempts ]; then
            echo -e "${RED}Timeout waiting for $service_name${NC}"
            return 1
        fi
        sleep 1
        ((attempt++))
    done
    echo -e "${GREEN}$service_name is ready!${NC}"
}

# Check prerequisites
echo -e "${YELLOW}Checking prerequisites...${NC}"

if ! command_exists docker; then
    echo -e "${RED}Error: Docker is not installed${NC}"
    exit 1
fi

if ! command_exists java; then
    echo -e "${RED}Error: Java is not installed${NC}"
    exit 1
fi

if ! command_exists node; then
    echo -e "${RED}Error: Node.js is not installed${NC}"
    exit 1
fi

if ! command_exists npm; then
    echo -e "${RED}Error: npm is not installed${NC}"
    exit 1
fi

echo -e "${GREEN}All prerequisites found!${NC}"
echo

# Check if services are already running
if port_in_use 5432; then
    echo -e "${YELLOW}PostgreSQL port 5432 is already in use${NC}"
fi

if port_in_use 8080; then
    echo -e "${RED}Port 8080 is already in use. Please stop the existing service.${NC}"
    exit 1
fi

if port_in_use 5173; then
    echo -e "${RED}Port 5173 is already in use. Please stop the existing service.${NC}"
    exit 1
fi

# Start Docker Compose services (PostgreSQL + LocalStack)
echo
echo -e "${BLUE}Starting Docker services (PostgreSQL + LocalStack)...${NC}"
cd "$PROJECT_ROOT"

# Check if we need to reset volumes (database doesn't exist)
if docker compose ps -q postgres 2>/dev/null | grep -q .; then
    # Container exists, check if database exists
    if ! docker exec forever-home-postgres-1 psql -U postgres -lqt 2>/dev/null | grep -qw foreverhome; then
        echo -e "${YELLOW}Database 'foreverhome' not found. Recreating volumes...${NC}"
        docker compose down -v
    fi
fi

docker compose up -d

# Wait for PostgreSQL to be ready
wait_for_service localhost 5432 "PostgreSQL"

# Wait for LocalStack to be ready
wait_for_service localhost 4566 "LocalStack"

# Install frontend dependencies if needed
echo
echo -e "${BLUE}Checking frontend dependencies...${NC}"
cd "$PROJECT_ROOT/frontend"
if [ ! -d "node_modules" ]; then
    echo -e "${YELLOW}Installing frontend dependencies...${NC}"
    npm install
fi
echo -e "${GREEN}Frontend dependencies ready!${NC}"

# Create a cleanup function
cleanup() {
    echo
    echo -e "${YELLOW}Shutting down services...${NC}"

    # Kill background processes
    if [ -n "$BACKEND_PID" ]; then
        echo "Stopping backend (PID: $BACKEND_PID)..."
        kill $BACKEND_PID 2>/dev/null || true
    fi

    if [ -n "$FRONTEND_PID" ]; then
        echo "Stopping frontend (PID: $FRONTEND_PID)..."
        kill $FRONTEND_PID 2>/dev/null || true
    fi

    echo -e "${YELLOW}Docker services are still running. Run 'docker compose down' to stop them.${NC}"
    echo -e "${GREEN}Goodbye!${NC}"
    exit 0
}

# Set up trap to catch Ctrl+C
trap cleanup SIGINT SIGTERM

# Start the backend
echo
echo -e "${BLUE}Starting Spring Boot backend...${NC}"
cd "$PROJECT_ROOT"
./mvnw spring-boot:run -q &
BACKEND_PID=$!

# Wait for backend to be ready
sleep 5
wait_for_service localhost 8080 "Backend API"

# Start the frontend
echo
echo -e "${BLUE}Starting React frontend...${NC}"
cd "$PROJECT_ROOT/frontend"
npm run dev &
FRONTEND_PID=$!

# Wait a moment for frontend to start
sleep 3

echo
echo -e "${GREEN}================================================${NC}"
echo -e "${GREEN}   All services are running!${NC}"
echo -e "${GREEN}================================================${NC}"
echo
echo -e "  ${BLUE}Frontend:${NC}  http://localhost:5173"
echo -e "  ${BLUE}Backend:${NC}   http://localhost:8080"
echo -e "  ${BLUE}Database:${NC}  localhost:5432 (postgres/postgres)"
echo -e "  ${BLUE}LocalStack:${NC} localhost:4566"
echo
echo -e "${YELLOW}Press Ctrl+C to stop all services${NC}"
echo

# Wait for user to stop
wait
