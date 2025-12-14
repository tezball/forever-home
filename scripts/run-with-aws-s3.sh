#!/bin/bash
# DEPRECATED: This script is deprecated. Use dev.sh with --s3 flag instead.
#
# New usage:
#   ./dev.sh start --s3
#   ./dev.sh restart --s3
#
# This script now just calls dev.sh with the --s3 flag for backwards compatibility.

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "Note: This script is deprecated. Use './dev.sh start --s3' instead."
echo

cd "$PROJECT_DIR"
exec ./dev.sh start --s3
