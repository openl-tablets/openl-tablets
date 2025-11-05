#!/bin/bash
#
# Build OpenL Tablets Documentation
#
# This script builds the MkDocs documentation for OpenL Tablets.
# The generated site will be in docs/site/ directory.
#
# Usage:
#   ./scripts/build-docs.sh [OPTIONS]
#
# Options:
#   --clean     Clean the site directory before building
#   --strict    Enable strict mode (warnings as errors)
#   --verbose   Enable verbose output
#   --help      Show this help message
#

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Default options
CLEAN=false
STRICT=false
VERBOSE=false

# Parse command line arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --clean)
      CLEAN=true
      shift
      ;;
    --strict)
      STRICT=true
      shift
      ;;
    --verbose)
      VERBOSE=true
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

echo -e "${GREEN}Building OpenL Tablets Documentation${NC}"
echo "========================================"
echo ""

# Check if mkdocs is installed
if ! command -v mkdocs &> /dev/null; then
  echo -e "${YELLOW}MkDocs is not installed.${NC}"
  echo "Installing MkDocs and dependencies..."
  pip install -r "$DOCS_DIR/requirements.txt"
  echo ""
fi

# Clean site directory if requested
if [ "$CLEAN" = true ]; then
  echo -e "${YELLOW}Cleaning site directory...${NC}"
  rm -rf "$DOCS_DIR/site"
  echo ""
fi

# Build options
BUILD_OPTS=""
if [ "$STRICT" = true ]; then
  BUILD_OPTS="$BUILD_OPTS --strict"
fi
if [ "$VERBOSE" = true ]; then
  BUILD_OPTS="$BUILD_OPTS --verbose"
fi

# Change to docs directory
cd "$DOCS_DIR"

# Build documentation
echo "Building documentation..."
mkdocs build $BUILD_OPTS

# Build result
if [ $? -eq 0 ]; then
  echo ""
  echo -e "${GREEN}✓ Documentation built successfully!${NC}"
  echo ""
  echo "Output location: $DOCS_DIR/site/"
  echo ""
  echo "To view the documentation:"
  echo "  - Open: $DOCS_DIR/site/index.html"
  echo "  - Or run: ./scripts/preview-docs.sh"
  echo ""
else
  echo -e "${RED}✗ Documentation build failed!${NC}"
  exit 1
fi
