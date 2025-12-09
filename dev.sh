#!/bin/bash

# Forever Home - Development Environment Manager
# Usage: ./dev.sh [start|stop|restart|status|gatling]

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_PORT=8080
FRONTEND_PORT=5173
POSTGRES_PORT=5432
LOCALSTACK_PORT=4566
LOKI_PORT=3100
GRAFANA_PORT=3000

# Check if a port is in use (for local processes)
port_in_use() {
    lsof -ti:"$1" >/dev/null 2>&1
}

# Check if a port is accepting connections (for Docker services)
port_accepting() {
    nc -z localhost "$1" 2>/dev/null
}

# Kill process on a port
kill_port() {
    lsof -ti:"$1" | xargs -r kill -9 2>/dev/null || true
}

# Wait for a service to be ready (uses nc for better Docker support)
wait_for_port() {
    local port=$1
    local name=$2
    local max_wait=${3:-30}

    for i in $(seq 1 $max_wait); do
        if nc -z localhost "$port" 2>/dev/null; then
            echo -e "${GREEN}[$name] Running on port $port${NC}"
            return 0
        fi
        sleep 1
    done
    echo -e "${RED}[$name] Failed to start on port $port after ${max_wait}s${NC}"
    return 1
}

# Check prerequisites
check_prereqs() {
    local missing=()
    command -v docker >/dev/null 2>&1 || missing+=("docker")
    command -v java >/dev/null 2>&1 || missing+=("java")
    command -v node >/dev/null 2>&1 || missing+=("node")
    command -v npm >/dev/null 2>&1 || missing+=("npm")

    if [ ${#missing[@]} -gt 0 ]; then
        echo -e "${RED}Missing prerequisites: ${missing[*]}${NC}"
        exit 1
    fi
}

# Start Docker services
start_docker() {
    echo -e "${BLUE}[Docker] Starting PostgreSQL, LocalStack, Loki, Grafana...${NC}"
    cd "$PROJECT_ROOT"
    docker compose up -d

    # Wait for Docker services (don't fail, just warn)
    echo -e "${YELLOW}[Docker] Waiting for services...${NC}"
    wait_for_port $POSTGRES_PORT "PostgreSQL" 30 || echo -e "${YELLOW}[PostgreSQL] May still be starting...${NC}"
    wait_for_port $LOCALSTACK_PORT "LocalStack" 30 || echo -e "${YELLOW}[LocalStack] May still be starting...${NC}"
    wait_for_port $LOKI_PORT "Loki" 30 || echo -e "${YELLOW}[Loki] May still be starting...${NC}"
    wait_for_port $GRAFANA_PORT "Grafana" 30 || echo -e "${YELLOW}[Grafana] May still be starting...${NC}"
}

# Stop Docker services
stop_docker() {
    echo -e "${BLUE}[Docker] Stopping services...${NC}"
    cd "$PROJECT_ROOT"
    docker compose down
    echo -e "${GREEN}[Docker] Stopped${NC}"
}

# Start backend
start_backend() {
    if port_in_use $BACKEND_PORT; then
        echo -e "${YELLOW}[Backend] Already running on port $BACKEND_PORT${NC}"
    else
        echo -e "${BLUE}[Backend] Starting Spring Boot...${NC}"
        cd "$PROJECT_ROOT"
        ./mvnw spring-boot:run -q > /dev/null 2>&1 &
        wait_for_port $BACKEND_PORT "Backend" 30
    fi
}

# Start frontend
start_frontend() {
    if port_in_use $FRONTEND_PORT; then
        echo -e "${YELLOW}[Frontend] Already running on port $FRONTEND_PORT${NC}"
    else
        # Install deps if needed
        if [ ! -d "$PROJECT_ROOT/frontend/node_modules" ]; then
            echo -e "${YELLOW}[Frontend] Installing dependencies...${NC}"
            cd "$PROJECT_ROOT/frontend"
            npm install
        fi

        echo -e "${BLUE}[Frontend] Starting Vite...${NC}"
        cd "$PROJECT_ROOT/frontend"
        npm run dev > /dev/null 2>&1 &
        wait_for_port $FRONTEND_PORT "Frontend" 10
    fi
}

# Stop apps (backend + frontend)
stop_apps() {
    if port_in_use $BACKEND_PORT; then
        echo -e "${BLUE}[Backend] Stopping...${NC}"
        kill_port $BACKEND_PORT
        echo -e "${GREEN}[Backend] Stopped${NC}"
    else
        echo -e "${YELLOW}[Backend] Not running${NC}"
    fi

    if port_in_use $FRONTEND_PORT; then
        echo -e "${BLUE}[Frontend] Stopping...${NC}"
        kill_port $FRONTEND_PORT
        echo -e "${GREEN}[Frontend] Stopped${NC}"
    else
        echo -e "${YELLOW}[Frontend] Not running${NC}"
    fi
}

# Run Gatling load tests
run_gatling() {
    local simulation="${1:-}"
    shift 2>/dev/null || true
    local extra_args="$*"

    # Check if backend is running
    if ! nc -z localhost $BACKEND_PORT 2>/dev/null; then
        echo -e "${RED}[Gatling] Backend is not running on port $BACKEND_PORT${NC}"
        echo -e "${YELLOW}Start the backend first with: ./dev.sh start${NC}"
        exit 1
    fi

    echo -e "${BLUE}[Gatling] Running load tests against http://localhost:$BACKEND_PORT${NC}"

    cd "$PROJECT_ROOT"

    case "$simulation" in
        stress)
            echo -e "${YELLOW}[Gatling] Running stress test simulation...${NC}"
            ./mvnw gatling:test -Dgatling.simulationClass=com.example.foreverhome.simulation.RegistrationStressSimulation $extra_args
            ;;
        registration|"")
            echo -e "${YELLOW}[Gatling] Running user registration simulation...${NC}"
            ./mvnw gatling:test -Dgatling.simulationClass=com.example.foreverhome.simulation.UserRegistrationSimulation $extra_args
            ;;
        *)
            # Treat as a full class name
            echo -e "${YELLOW}[Gatling] Running simulation: $simulation${NC}"
            ./mvnw gatling:test -Dgatling.simulationClass="$simulation" $extra_args
            ;;
    esac

    echo -e "${GREEN}[Gatling] Load test complete. Reports at target/gatling/${NC}"
}

# Show status
show_status() {
    echo -e "${BLUE}=== Forever Home Service Status ===${NC}"
    echo

    if nc -z localhost $POSTGRES_PORT 2>/dev/null; then
        echo -e "${GREEN}[PostgreSQL]  Running on port $POSTGRES_PORT${NC}"
    else
        echo -e "${RED}[PostgreSQL]  Not running${NC}"
    fi

    if nc -z localhost $LOCALSTACK_PORT 2>/dev/null; then
        echo -e "${GREEN}[LocalStack]  Running on port $LOCALSTACK_PORT${NC}"
    else
        echo -e "${RED}[LocalStack]  Not running${NC}"
    fi

    if nc -z localhost $LOKI_PORT 2>/dev/null; then
        echo -e "${GREEN}[Loki]        Running on http://localhost:$LOKI_PORT${NC}"
    else
        echo -e "${RED}[Loki]        Not running${NC}"
    fi

    if nc -z localhost $GRAFANA_PORT 2>/dev/null; then
        echo -e "${GREEN}[Grafana]     Running on http://localhost:$GRAFANA_PORT (admin/admin)${NC}"
    else
        echo -e "${RED}[Grafana]     Not running${NC}"
    fi

    if nc -z localhost $BACKEND_PORT 2>/dev/null; then
        echo -e "${GREEN}[Backend]     Running on http://localhost:$BACKEND_PORT${NC}"
    else
        echo -e "${RED}[Backend]     Not running${NC}"
    fi

    if nc -z localhost $FRONTEND_PORT 2>/dev/null; then
        echo -e "${GREEN}[Frontend]    Running on http://localhost:$FRONTEND_PORT${NC}"
    else
        echo -e "${RED}[Frontend]    Not running${NC}"
    fi
    echo
}

# Main command handler
case "${1:-}" in
    start)
        echo -e "${BLUE}=== Starting Forever Home ===${NC}"
        check_prereqs
        start_docker
        start_backend
        start_frontend
        echo
        show_status
        ;;
    stop)
        echo -e "${BLUE}=== Stopping Forever Home ===${NC}"
        stop_apps
        stop_docker
        echo -e "${GREEN}All services stopped${NC}"
        ;;
    restart)
        echo -e "${BLUE}=== Restarting Forever Home Apps ===${NC}"
        stop_apps
        sleep 1
        start_backend
        start_frontend
        echo
        show_status
        ;;
    status)
        show_status
        ;;
    gatling)
        shift
        run_gatling "$@"
        ;;
    *)
        echo "Usage: $0 {start|stop|restart|status|gatling}"
        echo
        echo "Commands:"
        echo "  start                - Start all services (Docker + Backend + Frontend)"
        echo "  stop                 - Stop all services"
        echo "  restart              - Restart backend and frontend (keeps Docker running)"
        echo "  status               - Show status of all services"
        echo "  gatling [SIM] [OPTS] - Run Gatling load tests"
        echo
        echo "Gatling simulations:"
        echo "  registration  - User registration simulation (default)"
        echo "  stress        - Stress test simulation"
        echo "  <class>       - Custom simulation class name"
        echo
        echo "Gatling options (pass after simulation name):"
        echo "  -DUSERS=N          - Number of users (default: 10)"
        echo "  -DRAMP_DURATION=N  - Ramp-up duration in seconds (default: 10)"
        echo
        echo "Examples:"
        echo "  $0 gatling                           # Run default registration simulation"
        echo "  $0 gatling stress                    # Run stress test"
        echo "  $0 gatling registration -DUSERS=50  # Registration with 50 users"
        exit 1
        ;;
esac
