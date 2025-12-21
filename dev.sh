#!/bin/bash

# Forever Home - Development CLI
# Usage: ./dev.sh <command> [options]

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
BOLD='\033[1m'
NC='\033[0m'

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TERRAFORM_DIR="$PROJECT_ROOT/terraform"
ENV_FILE="$PROJECT_ROOT/.env"

# Ports
APP_PORT=8080
POSTGRES_PORT=5432
LOCALSTACK_PORT=4566
LOKI_PORT=3100
GRAFANA_PORT=3000
MAILPIT_UI_PORT=8025
MAILPIT_SMTP_PORT=1025

# Default settings
S3_MODE="local"

#------------------------------------------------------------------------------
# Utility functions
#------------------------------------------------------------------------------
port_in_use() { lsof -ti:"$1" >/dev/null 2>&1; }
port_accepting() { nc -z localhost "$1" 2>/dev/null; }
kill_port() { lsof -ti:"$1" | xargs -r kill -9 2>/dev/null || true; }

wait_for_port() {
    local port=$1 name=$2 max_wait=${3:-30}
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

check_prereqs() {
    local missing=()
    command -v docker >/dev/null 2>&1 || missing+=("docker")
    command -v java >/dev/null 2>&1 || missing+=("java")
    if [ ${#missing[@]} -gt 0 ]; then
        echo -e "${RED}Missing prerequisites: ${missing[*]}${NC}"
        exit 1
    fi
}

load_aws_credentials() {
    if [ ! -f "$ENV_FILE" ]; then
        echo -e "${RED}Error: .env file not found. Copy .env.example and configure it.${NC}"
        return 1
    fi
    set -a; source "$ENV_FILE"; set +a
    if [ -z "$AWS_ACCESS_KEY_ID" ] || [ "$AWS_ACCESS_KEY_ID" = "your-access-key-here" ]; then
        echo -e "${RED}Error: AWS_ACCESS_KEY_ID not set in .env${NC}"
        return 1
    fi
    if [ -z "$AWS_SECRET_ACCESS_KEY" ] || [ "$AWS_SECRET_ACCESS_KEY" = "your-secret-key-here" ]; then
        echo -e "${RED}Error: AWS_SECRET_ACCESS_KEY not set in .env${NC}"
        return 1
    fi
    export AWS_REGION="${AWS_REGION:-eu-west-1}"
    export AWS_S3_BUCKET="${AWS_S3_BUCKET:-forever-home-images-dev}"
    echo -e "${GREEN}[S3] Using AWS S3: $AWS_S3_BUCKET (${AWS_REGION})${NC}"
}

#------------------------------------------------------------------------------
# Local development commands
#------------------------------------------------------------------------------
cmd_start() {
    local s3_mode="local"
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --s3|--aws) s3_mode="aws"; shift ;;
            --help|-h) echo "Usage: ./dev.sh start [--s3]"; echo "  --s3  Use AWS S3 instead of LocalStack"; exit 0 ;;
            *) shift ;;
        esac
    done

    echo -e "${BLUE}=== Starting Forever Home ===${NC}"
    check_prereqs
    [ "$s3_mode" = "aws" ] && { load_aws_credentials || exit 1; }

    echo -e "${YELLOW}Starting Docker services...${NC}"
    docker compose up -d
    wait_for_port $POSTGRES_PORT "PostgreSQL" 30 || true
    wait_for_port $LOCALSTACK_PORT "LocalStack" 30 || true
    wait_for_port $GRAFANA_PORT "Grafana" 30 || true
    wait_for_port $MAILPIT_UI_PORT "Mailpit" 30 || true

    if ! port_in_use $APP_PORT; then
        echo -e "${YELLOW}Starting application...${NC}"
        if [ "$s3_mode" = "aws" ]; then
            export AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY AWS_REGION AWS_S3_BUCKET
            ./mvnw spring-boot:run -Dspring-boot.run.profiles=default,s3-aws -Dmaven.test.skip=true -q > /dev/null 2>&1 &
        else
            ./mvnw spring-boot:run -q > /dev/null 2>&1 &
        fi
        wait_for_port $APP_PORT "App" 60
    fi
    echo -e "\n${GREEN}Ready at http://localhost:$APP_PORT${NC}"
    cmd_status
}

cmd_stop() {
    echo -e "${BLUE}=== Stopping Forever Home ===${NC}"
    port_in_use $APP_PORT && { kill_port $APP_PORT; echo -e "${GREEN}[App] Stopped${NC}"; }
    docker compose down
    echo -e "${GREEN}All services stopped${NC}"
}

cmd_restart() {
    local s3_flag=""
    [[ "$*" == *"--s3"* || "$*" == *"--aws"* ]] && s3_flag="--s3"
    cmd_stop
    docker compose down -v
    sleep 1
    cmd_start $s3_flag
}

cmd_clean() {
    echo -e "${BLUE}=== Cleaning Forever Home ===${NC}"
    port_in_use $APP_PORT && kill_port $APP_PORT
    docker compose down -v
    echo -e "${GREEN}All services stopped and data removed${NC}"
}

cmd_status() {
    echo -e "${BLUE}=== Service Status ===${NC}"
    local services=(
        "$POSTGRES_PORT:PostgreSQL"
        "$LOCALSTACK_PORT:LocalStack"
        "$LOKI_PORT:Loki"
        "$GRAFANA_PORT:Grafana (http://localhost:$GRAFANA_PORT)"
        "$MAILPIT_UI_PORT:Mailpit (http://localhost:$MAILPIT_UI_PORT)"
        "$APP_PORT:App (http://localhost:$APP_PORT)"
    )
    for svc in "${services[@]}"; do
        local port="${svc%%:*}" name="${svc#*:}"
        if nc -z localhost "$port" 2>/dev/null; then
            echo -e "  ${GREEN}✓${NC} $name"
        else
            echo -e "  ${RED}✗${NC} $name"
        fi
    done
}

#------------------------------------------------------------------------------
# Test commands
#------------------------------------------------------------------------------
cmd_test() {
    local subcmd="${1:-unit}"; shift 2>/dev/null || true
    case "$subcmd" in
        unit|"")     cmd_test_unit "$@" ;;
        e2e)         cmd_test_e2e "$@" ;;
        gatling|load) cmd_test_gatling "$@" ;;
        --help|-h)   help_test ;;
        *)           echo -e "${RED}Unknown test command: $subcmd${NC}"; help_test; exit 1 ;;
    esac
}

cmd_test_unit() {
    local test_class="$1"
    echo -e "${BLUE}=== Running Unit Tests ===${NC}"
    if [ -n "$test_class" ]; then
        ./mvnw test -Dtest="$test_class"
    else
        ./mvnw test
    fi
}

cmd_test_e2e() {
    local suite="${1:-}"; shift 2>/dev/null || true
    if ! nc -z localhost $APP_PORT 2>/dev/null; then
        echo -e "${RED}App not running. Start with: ./dev.sh start${NC}"; exit 1
    fi

    cd "$PROJECT_ROOT/frontend"
    [ ! -d "node_modules" ] && npm install
    [ ! -d "$HOME/.cache/ms-playwright" ] && npx playwright install

    case "$suite" in
        ui)      npx playwright test --ui "$@" ;;
        headed)  npx playwright test --headed "$@" ;;
        debug)   npx playwright test --debug "$@" ;;
        report)  npx playwright show-report ;;
        auth)    npx playwright test auth-flows.spec.ts auth.spec.ts "$@" ;;
        foster)  npx playwright test foster-journey.spec.ts "$@" ;;
        adopter) npx playwright test adopter-journey.spec.ts "$@" ;;
        rescue)  npx playwright test rescue-journey.spec.ts "$@" ;;
        vet)     npx playwright test vet-journey.spec.ts "$@" ;;
        admin)   npx playwright test admin-journey.spec.ts "$@" ;;
        lifecycle) npx playwright test adoption-lifecycle.spec.ts "$@" ;;
        public)  npx playwright test public-pages.spec.ts pet-browsing.spec.ts "$@" ;;
        all|"")  npx playwright test "$@" ;;
        *)       npx playwright test -g "$suite" "$@" ;;
    esac
}

cmd_test_gatling() {
    local sim="${1:-all}"; shift 2>/dev/null || true
    if ! nc -z localhost $APP_PORT 2>/dev/null; then
        echo -e "${RED}App not running. Start with: ./dev.sh start${NC}"; exit 1
    fi

    cd "$PROJECT_ROOT"
    local class=""
    case "$sim" in
        all|allflows) class="com.example.foreverhome.simulation.AllUserFlowsSimulation" ;;
        registration) class="com.example.foreverhome.simulation.UserRegistrationSimulation" ;;
        stress)       class="com.example.foreverhome.simulation.RegistrationStressSimulation" ;;
        endurance)    class="com.example.foreverhome.simulation.FullFeatureEnduranceSimulation" ;;
        *)            class="$sim" ;;
    esac
    echo -e "${BLUE}Running Gatling: $class${NC}"
    ./mvnw gatling:test -Dgatling.simulationClass="$class" "$@"
}

help_test() {
    cat << 'EOF'
Usage: ./dev.sh test <command> [options]

Commands:
  unit [Class]     Run unit tests (default). Optionally specify test class
  e2e [suite]      Run Playwright E2E tests
  gatling [sim]    Run Gatling load tests

E2E Suites: all, auth, foster, adopter, rescue, vet, admin, lifecycle, public
E2E Modes:  ui, headed, debug, report

Gatling Sims: all, registration, stress, endurance

Examples:
  ./dev.sh test                          # Run all unit tests
  ./dev.sh test unit UserServiceTest     # Run specific test class
  ./dev.sh test e2e                      # Run all E2E tests
  ./dev.sh test e2e auth                 # Run auth E2E tests
  ./dev.sh test e2e ui                   # Interactive Playwright UI
  ./dev.sh test gatling                  # Run all user flows load test
  ./dev.sh test gatling stress           # Run stress test
EOF
}

#------------------------------------------------------------------------------
# Build commands
#------------------------------------------------------------------------------
cmd_build() {
    local subcmd="${1:-jar}"; shift 2>/dev/null || true
    case "$subcmd" in
        jar|"")  cmd_build_jar "$@" ;;
        docker)  cmd_build_docker "$@" ;;
        native)  cmd_build_native "$@" ;;
        --help|-h) help_build ;;
        *)       echo -e "${RED}Unknown build command: $subcmd${NC}"; help_build; exit 1 ;;
    esac
}

cmd_build_jar() {
    echo -e "${BLUE}=== Building JAR ===${NC}"
    ./mvnw package -DskipTests
    echo -e "${GREEN}Build complete: target/*.jar${NC}"
}

cmd_build_docker() {
    local tag="${1:-latest}"
    echo -e "${BLUE}=== Building Docker Image ===${NC}"
    docker build -t forever-home:"$tag" .
    echo -e "${GREEN}Built: forever-home:$tag${NC}"
}

cmd_build_native() {
    echo -e "${BLUE}=== Building Native Image ===${NC}"
    ./mvnw native:compile -Pnative -DskipTests
    echo -e "${GREEN}Native build complete${NC}"
}

help_build() {
    cat << 'EOF'
Usage: ./dev.sh build <command> [options]

Commands:
  jar              Build JAR package (default)
  docker [tag]     Build Docker image (default tag: latest)
  native           Build GraalVM native image

Examples:
  ./dev.sh build                   # Build JAR
  ./dev.sh build docker            # Build Docker image
  ./dev.sh build docker v1.0.0     # Build with specific tag
  ./dev.sh build native            # Build native executable
EOF
}

#------------------------------------------------------------------------------
# Deploy commands
#------------------------------------------------------------------------------
cmd_deploy() {
    local subcmd="${1:-}"; shift 2>/dev/null || true
    case "$subcmd" in
        ""|aws)  cmd_deploy_aws "$@" ;;
        infra)   cmd_deploy_infra "$@" ;;
        destroy) cmd_deploy_destroy "$@" ;;
        --help|-h) help_deploy ;;
        *)       echo -e "${RED}Unknown deploy command: $subcmd${NC}"; help_deploy; exit 1 ;;
    esac
}

cmd_deploy_aws() {
    local tag="latest" reset="true" blank="false" auto_approve=""
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --tag|-t)    tag="$2"; shift 2 ;;
            --no-reset)  reset="false"; shift ;;
            --blank)     blank="true"; shift ;;
            -y|--auto-approve) auto_approve="true"; shift ;;
            *) shift ;;
        esac
    done

    echo -e "${BLUE}=== Deploying to AWS ===${NC}"

    # Check prerequisites
    for cmd in aws docker terraform; do
        command -v $cmd >/dev/null 2>&1 || { echo -e "${RED}Missing: $cmd${NC}"; exit 1; }
    done
    [ "$blank" = "true" ] && { command -v jq >/dev/null 2>&1 || { echo -e "${RED}Missing: jq (required for --blank)${NC}"; exit 1; }; }

    aws sts get-caller-identity >/dev/null 2>&1 || { echo -e "${RED}AWS credentials not configured${NC}"; exit 1; }

    # Delegate to deploy.sh for the actual deployment
    cd "$PROJECT_ROOT"
    local args=""
    [ "$tag" != "latest" ] && args="$args --tag $tag"
    [ "$reset" = "false" ] && args="$args --no-reset"
    [ "$blank" = "true" ] && args="$args --blank"
    ./deploy.sh $args
}

cmd_deploy_infra() {
    local auto_approve=""
    [[ "$*" == *"-y"* || "$*" == *"--auto-approve"* ]] && auto_approve="-auto-approve"

    echo -e "${BLUE}=== Provisioning Infrastructure ===${NC}"
    cd "$TERRAFORM_DIR"

    [ ! -f "terraform.tfvars" ] && { echo -e "${RED}Missing terraform.tfvars${NC}"; exit 1; }
    aws sts get-caller-identity >/dev/null 2>&1 || { echo -e "${RED}AWS credentials not configured${NC}"; exit 1; }

    terraform init -input=false
    terraform plan -out=tfplan -input=false

    if [ -n "$auto_approve" ]; then
        terraform apply -input=false tfplan
    else
        terraform apply tfplan
    fi
    rm -f tfplan

    echo -e "\n${GREEN}Infrastructure ready!${NC}"
    terraform output application_url
}

cmd_deploy_destroy() {
    echo -e "${RED}=== DESTROY Infrastructure ===${NC}"
    echo -e "${YELLOW}This will permanently delete all AWS resources including the database!${NC}"
    read -p "Type 'DESTROY' to confirm: " confirm
    [ "$confirm" != "DESTROY" ] && { echo "Aborted."; exit 0; }

    cd "$TERRAFORM_DIR"
    "$TERRAFORM_DIR/scripts/prod-destroy.sh"
}

help_deploy() {
    cat << 'EOF'
Usage: ./dev.sh deploy <command> [options]

Commands:
  aws [opts]       Deploy application to AWS ECS (default)
  infra [-y]       Provision AWS infrastructure with Terraform
  destroy          Destroy all AWS infrastructure

AWS Deploy Options:
  --tag TAG        Docker image tag (default: latest)
  --no-reset       Preserve database (don't reset)
  --blank          Deploy without demo data (production mode)
  -y               Auto-approve Terraform changes

Examples:
  ./dev.sh deploy                      # Deploy to AWS
  ./dev.sh deploy aws --no-reset       # Deploy without DB reset
  ./dev.sh deploy aws --blank          # Production deploy (no demo data)
  ./dev.sh deploy infra                # Provision infrastructure
  ./dev.sh deploy infra -y             # Auto-approve infrastructure
  ./dev.sh deploy destroy              # Tear down everything
EOF
}

#------------------------------------------------------------------------------
# Main help
#------------------------------------------------------------------------------
help_main() {
    cat << EOF
${BOLD}Forever Home Development CLI${NC}

${BOLD}Usage:${NC} ./dev.sh <command> [options]

${BOLD}Local Development:${NC}
  start [--s3]     Start all services (app + Docker)
  stop             Stop all services
  restart [--s3]   Restart with clean data
  clean            Stop and remove all data
  status           Show service status

${BOLD}Testing:${NC}
  test             Run unit tests (default)
  test e2e         Run Playwright E2E tests
  test gatling     Run Gatling load tests

${BOLD}Building:${NC}
  build            Build JAR (default)
  build docker     Build Docker image
  build native     Build GraalVM native image

${BOLD}Deployment:${NC}
  deploy           Deploy to AWS ECS
  deploy infra     Provision infrastructure
  deploy destroy   Tear down infrastructure

Run './dev.sh <command> --help' for detailed options.
EOF
}

#------------------------------------------------------------------------------
# Main entry point
#------------------------------------------------------------------------------
case "${1:-}" in
    start)   shift; cmd_start "$@" ;;
    stop)    cmd_stop ;;
    restart) shift; cmd_restart "$@" ;;
    clean)   cmd_clean ;;
    status)  cmd_status ;;
    test)    shift; cmd_test "$@" ;;
    build)   shift; cmd_build "$@" ;;
    deploy)  shift; cmd_deploy "$@" ;;
    --help|-h|"") help_main ;;
    *)       echo -e "${RED}Unknown command: $1${NC}"; help_main; exit 1 ;;
esac
