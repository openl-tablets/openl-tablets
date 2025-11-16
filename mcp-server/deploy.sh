#!/bin/bash

# OpenL Tablets MCP Server Deployment Script

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if .env file exists
check_env() {
    if [ ! -f .env ]; then
        log_warn ".env file not found. Creating from .env.example..."
        if [ -f .env.example ]; then
            cp .env.example .env
            log_info "Created .env file. Please edit it with your configuration."
            exit 0
        else
            log_error ".env.example not found. Please create .env manually."
            exit 1
        fi
    fi
}

# Install dependencies
install_deps() {
    log_info "Installing dependencies..."
    npm install
}

# Build the project
build() {
    log_info "Building TypeScript..."
    npm run build
}

# Run tests
test() {
    log_info "Running tests..."
    npm test
}

# Start the server
start() {
    log_info "Starting MCP server..."
    npm start
}

# Deploy with Docker
docker_deploy() {
    log_info "Building Docker image..."
    docker build -t openl-mcp-server:latest .

    log_info "Starting Docker container..."
    docker-compose up -d

    log_info "Checking container status..."
    docker-compose ps
}

# Stop Docker deployment
docker_stop() {
    log_info "Stopping Docker containers..."
    docker-compose down
}

# Show logs
docker_logs() {
    docker-compose logs -f
}

# Display help
show_help() {
    cat << EOF
OpenL Tablets MCP Server Deployment Script

Usage: ./deploy.sh [command]

Commands:
    install     Install dependencies
    build       Build TypeScript code
    test        Run tests
    start       Start the MCP server
    deploy      Full deployment (install, build, test)
    docker      Build and deploy with Docker
    docker-stop Stop Docker deployment
    docker-logs Show Docker logs
    clean       Clean build artifacts
    help        Show this help message

Examples:
    ./deploy.sh deploy      # Full local deployment
    ./deploy.sh docker      # Deploy with Docker
    ./deploy.sh test        # Run tests only

EOF
}

# Clean build artifacts
clean() {
    log_info "Cleaning build artifacts..."
    rm -rf dist/
    rm -rf node_modules/
    rm -rf coverage/
    log_info "Clean complete"
}

# Main deployment function
deploy() {
    check_env
    install_deps
    build
    test
    log_info "Deployment complete! Run './deploy.sh start' to start the server."
}

# Main script logic
case "${1:-help}" in
    install)
        install_deps
        ;;
    build)
        build
        ;;
    test)
        install_deps
        test
        ;;
    start)
        check_env
        start
        ;;
    deploy)
        deploy
        ;;
    docker)
        check_env
        docker_deploy
        ;;
    docker-stop)
        docker_stop
        ;;
    docker-logs)
        docker_logs
        ;;
    clean)
        clean
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        log_error "Unknown command: $1"
        show_help
        exit 1
        ;;
esac
