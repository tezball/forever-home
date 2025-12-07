#!/bin/bash

# Forever Home - Stop Development Services

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo -e "${YELLOW}Stopping Forever Home development services...${NC}"

# Stop Docker Compose services
cd "$PROJECT_ROOT"
docker compose down

echo -e "${GREEN}All services stopped!${NC}"
