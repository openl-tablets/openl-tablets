#!/bin/bash
#
# Preview OpenL Tablets Documentation
#
# This script starts a local web server to preview the MkDocs documentation.
# The documentation will be available at http://localhost:8000
#
# Usage:
#   ./scripts/preview-docs.sh [OPTIONS]
#
# Options:
#   --port PORT    Port to use (default: 8000)
#   --host HOST    Host to bind to (default: 127.0.0.1)
#   --strict       Enable strict mode (warnings as errors)
#   --help         Show this help message
#
# The server will auto-reload when documentation files are changed.
#

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Default options
PORT=8000
HOST="127.0.0.1"
STRICT=false

# Parse command line arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --port)
      PORT="$2"
      shift 2
      ;;
    --host)
      HOST="$2"
      shift 2
      ;;
    --strict)
      STRICT=true
      shift
      ;;
    --help)
      grep '^#' "$0" | sed 's/^# //' | sed 's/^#//'
      exit 0
      ;;
    *)
      echo -e "${RED}Unknown option: $1${NC}"
      echo "Use --help for usage information"
      exit 1
      ;;
  esac
done

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
DOCS_DIR="$PROJECT_ROOT/docs"

echo -e "${GREEN}OpenL Tablets Documentation Preview${NC}"
echo "===================================="
echo ""

# Check if mkdocs is installed
if ! command -v mkdocs &> /dev/null; then
  echo -e "${YELLOW}MkDocs is not installed.${NC}"
  echo "Installing MkDocs and dependencies..."
  pip install -r "$DOCS_DIR/requirements.txt"
  echo ""
fi

# Build options
SERVE_OPTS="--dev-addr ${HOST}:${PORT}"
if [ "$STRICT" = true ]; then
  SERVE_OPTS="$SERVE_OPTS --strict"
fi

# Change to docs directory
cd "$DOCS_DIR"

# Start server
echo "Starting documentation server..."
echo ""
echo -e "${GREEN}Documentation available at: http://${HOST}:${PORT}${NC}"
echo ""
echo "Press Ctrl+C to stop the server"
echo ""

mkdocs serve $SERVE_OPTS
