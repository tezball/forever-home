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
E2E_REPORT_DIR="$PROJECT_ROOT/frontend/playwright-report"

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
        all|allflows)
            echo -e "${YELLOW}[Gatling] Running ALL user flows simulation...${NC}"
            echo -e "${BLUE}This tests: Public browsing, Foster, Adopter, Rescue Org, Vet, and Admin flows${NC}"
            ./mvnw gatling:test -Dgatling.simulationClass=com.example.foreverhome.simulation.AllUserFlowsSimulation $extra_args
            ;;
        endurance)
            echo -e "${YELLOW}[Gatling] Running endurance simulation (runs forever until Ctrl+C)...${NC}"
            ./mvnw gatling:test -Dgatling.simulationClass=com.example.foreverhome.simulation.FullFeatureEnduranceSimulation $extra_args
            ;;
        stress)
            echo -e "${YELLOW}[Gatling] Running stress test simulation...${NC}"
            ./mvnw gatling:test -Dgatling.simulationClass=com.example.foreverhome.simulation.RegistrationStressSimulation $extra_args
            ;;
        registration)
            echo -e "${YELLOW}[Gatling] Running user registration simulation...${NC}"
            ./mvnw gatling:test -Dgatling.simulationClass=com.example.foreverhome.simulation.UserRegistrationSimulation $extra_args
            ;;
        "")
            # Default to allflows simulation
            echo -e "${YELLOW}[Gatling] Running ALL user flows simulation (default)...${NC}"
            echo -e "${BLUE}This tests: Public browsing, Foster, Adopter, Rescue Org, Vet, and Admin flows${NC}"
            ./mvnw gatling:test -Dgatling.simulationClass=com.example.foreverhome.simulation.AllUserFlowsSimulation $extra_args
            ;;
        *)
            # Treat as a full class name
            echo -e "${YELLOW}[Gatling] Running simulation: $simulation${NC}"
            ./mvnw gatling:test -Dgatling.simulationClass="$simulation" $extra_args
            ;;
    esac

    echo -e "${GREEN}[Gatling] Load test complete. Reports at target/gatling/${NC}"
}

# Run Playwright E2E tests
run_e2e() {
    local suite="${1:-}"
    shift 2>/dev/null || true
    local extra_args="$*"

    # Check if frontend is running
    if ! nc -z localhost $FRONTEND_PORT 2>/dev/null; then
        echo -e "${RED}[E2E] Frontend is not running on port $FRONTEND_PORT${NC}"
        echo -e "${YELLOW}Start the frontend first with: ./dev.sh start${NC}"
        exit 1
    fi

    # Check if backend is running
    if ! nc -z localhost $BACKEND_PORT 2>/dev/null; then
        echo -e "${RED}[E2E] Backend is not running on port $BACKEND_PORT${NC}"
        echo -e "${YELLOW}Start the backend first with: ./dev.sh start${NC}"
        exit 1
    fi

    echo -e "${BLUE}[E2E] Running Playwright tests against http://localhost:$FRONTEND_PORT${NC}"

    cd "$PROJECT_ROOT/frontend"

    # Install Playwright browsers if needed
    if [ ! -d "$HOME/.cache/ms-playwright" ] && [ ! -d "$PROJECT_ROOT/frontend/node_modules/.cache/ms-playwright" ]; then
        echo -e "${YELLOW}[E2E] Installing Playwright browsers...${NC}"
        npx playwright install
    fi

    case "$suite" in
        all)
            echo -e "${YELLOW}[E2E] Running ALL e2e tests...${NC}"
            npx playwright test $extra_args
            ;;
        auth)
            echo -e "${YELLOW}[E2E] Running authentication tests...${NC}"
            npx playwright test auth-flows.spec.ts auth.spec.ts $extra_args
            ;;
        foster)
            echo -e "${YELLOW}[E2E] Running foster journey tests...${NC}"
            npx playwright test foster-journey.spec.ts $extra_args
            ;;
        adopter)
            echo -e "${YELLOW}[E2E] Running adopter journey tests...${NC}"
            npx playwright test adopter-journey.spec.ts $extra_args
            ;;
        rescue)
            echo -e "${YELLOW}[E2E] Running rescue org journey tests...${NC}"
            npx playwright test rescue-journey.spec.ts $extra_args
            ;;
        vet)
            echo -e "${YELLOW}[E2E] Running vet journey tests...${NC}"
            npx playwright test vet-journey.spec.ts $extra_args
            ;;
        admin)
            echo -e "${YELLOW}[E2E] Running admin journey tests...${NC}"
            npx playwright test admin-journey.spec.ts $extra_args
            ;;
        lifecycle)
            echo -e "${YELLOW}[E2E] Running full adoption lifecycle tests...${NC}"
            npx playwright test adoption-lifecycle.spec.ts $extra_args
            ;;
        public)
            echo -e "${YELLOW}[E2E] Running public pages tests...${NC}"
            npx playwright test public-pages.spec.ts pet-browsing.spec.ts $extra_args
            ;;
        ui)
            echo -e "${YELLOW}[E2E] Opening Playwright UI mode...${NC}"
            npx playwright test --ui $extra_args
            ;;
        headed)
            echo -e "${YELLOW}[E2E] Running tests in headed mode...${NC}"
            npx playwright test --headed $extra_args
            ;;
        debug)
            echo -e "${YELLOW}[E2E] Running tests in debug mode...${NC}"
            npx playwright test --debug $extra_args
            ;;
        report)
            echo -e "${YELLOW}[E2E] Opening test report...${NC}"
            npx playwright show-report
            ;;
        "")
            # Default to all tests
            echo -e "${YELLOW}[E2E] Running ALL e2e tests (default)...${NC}"
            npx playwright test $extra_args
            ;;
        *)
            # Treat as a specific test file or grep pattern
            echo -e "${YELLOW}[E2E] Running tests matching: $suite${NC}"
            npx playwright test -g "$suite" $extra_args
            ;;
    esac

    local exit_code=$?

    if [ $exit_code -eq 0 ]; then
        echo -e "${GREEN}[E2E] All tests passed!${NC}"
    else
        echo -e "${RED}[E2E] Some tests failed. Check the report for details.${NC}"
        echo -e "${YELLOW}[E2E] Run './dev.sh e2e report' to view the HTML report${NC}"
    fi

    echo -e "${GREEN}[E2E] Test complete. Reports at frontend/playwright-report/${NC}"
    return $exit_code
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
    e2e)
        shift
        run_e2e "$@"
        ;;
    *)
        echo "Usage: $0 {start|stop|restart|status|gatling|e2e}"
        echo
        echo "Commands:"
        echo "  start                - Start all services (Docker + Backend + Frontend)"
        echo "  stop                 - Stop all services"
        echo "  restart              - Restart backend and frontend (keeps Docker running)"
        echo "  status               - Show status of all services"
        echo "  gatling [SIM] [OPTS] - Run Gatling load tests"
        echo "  e2e [SUITE] [OPTS]   - Run Playwright E2E tests"
        echo
        echo "E2E test suites:"
        echo "  all        - Run ALL e2e tests (default)"
        echo "  auth       - Authentication flows (login, register, logout)"
        echo "  foster     - Foster user journey (create pet, submit to rescue)"
        echo "  adopter    - Adopter user journey (browse, favorite, apply)"
        echo "  rescue     - Rescue org journey (review pets, manage vets, applications)"
        echo "  vet        - Vet journey (lookup pets, sign-off)"
        echo "  admin      - Admin journey (analytics, moderation, user management)"
        echo "  lifecycle  - Full adoption lifecycle (cross-role integration)"
        echo "  public     - Public pages (unauthenticated browsing)"
        echo "  ui         - Open Playwright UI mode for interactive testing"
        echo "  headed     - Run tests in headed browser mode"
        echo "  debug      - Run tests in debug mode (step through)"
        echo "  report     - Open the HTML test report"
        echo "  <pattern>  - Run tests matching a grep pattern"
        echo
        echo "E2E examples:"
        echo "  $0 e2e                         # Run all e2e tests"
        echo "  $0 e2e auth                    # Run only authentication tests"
        echo "  $0 e2e foster                  # Run only foster journey tests"
        echo "  $0 e2e lifecycle               # Run full adoption lifecycle tests"
        echo "  $0 e2e ui                      # Interactive Playwright UI"
        echo "  $0 e2e headed                  # Run tests with visible browser"
        echo "  $0 e2e debug                   # Debug mode with step-through"
        echo "  $0 e2e report                  # View HTML test report"
        echo "  $0 e2e \"should login\"          # Run tests matching pattern"
        echo "  $0 e2e all --project=chromium  # Run on specific browser"
        echo
        echo "Gatling simulations:"
        echo "  all, allflows  - ALL user flows simulation (default) - tests all roles and features"
        echo "  registration   - User registration simulation only"
        echo "  stress         - Stress test simulation (high load)"
        echo "  endurance      - Endurance test (runs forever until Ctrl+C)"
        echo "  <class>        - Custom simulation class name"
        echo
        echo "Gatling options (pass after simulation name):"
        echo "  -DUSERS=N           - Number of users per scenario (default: 5 for all, 10 for others)"
        echo "  -DRAMP_DURATION=N   - Ramp-up duration in seconds (default: 30)"
        echo "  -DTEST_DURATION=N   - Total test duration in seconds (default: 120)"
        echo "  -DADMIN_EMAIL=X     - Admin email for admin tests (default: admin@foreverhome.com)"
        echo "  -DADMIN_PASSWORD=X  - Admin password (default: AdminPass123!)"
        echo
        echo "Examples:"
        echo "  $0 gatling                           # Run all user flows simulation (default)"
        echo "  $0 gatling all                       # Same as above"
        echo "  $0 gatling all -DUSERS=10            # All flows with 10 users per scenario"
        echo "  $0 gatling registration              # Run registration simulation"
        echo "  $0 gatling stress                    # Run stress test"
        echo "  $0 gatling endurance                 # Run forever until Ctrl+C"
        echo "  $0 gatling registration -DUSERS=50  # Registration with 50 users"
        echo
        echo "All flows simulation covers:"
        echo "  - Public browsing (pets, rescue profiles, vet profiles)"
        echo "  - Foster flow (register, create pet, submit for review)"
        echo "  - Adopter flow (browse, favorite, apply, track applications)"
        echo "  - Rescue Org flow (approve pets, manage vets, process applications)"
        echo "  - Vet flow (lookup pets, sign-off)"
        echo "  - Admin flow (analytics, moderation, user management)"
        exit 1
        ;;
esac
