#!/bin/bash

# OpenL Tablets 6.0.0 API Endpoint Testing Script
#
# Usage: ./test-api-endpoints.sh [BASE_URL] [USERNAME] [PASSWORD]
# Example: ./test-api-endpoints.sh "http://localhost:8080/webstudio/rest" "admin" "admin"

set -e

# Configuration
BASE_URL="${1:-http://localhost:8080/webstudio/rest}"
USERNAME="${2:-admin}"
PASSWORD="${3:-admin}"
AUTH_HEADER="Authorization: Basic $(echo -n "$USERNAME:$PASSWORD" | base64)"

# Example project ID (base64 encoded "design:Example 1 - Bank Rating")
PROJECT_ID="ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n"
REPOSITORY="design"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Output file
OUTPUT_FILE="api-test-results.txt"
echo "OpenL Tablets 6.0.0 API Test Results" > "$OUTPUT_FILE"
echo "====================================" >> "$OUTPUT_FILE"
echo "Base URL: $BASE_URL" >> "$OUTPUT_FILE"
echo "Test Date: $(date)" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

# Function to test an endpoint
test_endpoint() {
    local method="$1"
    local endpoint="$2"
    local description="$3"
    local data="$4"

    echo -e "${BLUE}Testing:${NC} $description"
    echo "" >> "$OUTPUT_FILE"
    echo "---" >> "$OUTPUT_FILE"
    echo "TEST: $description" >> "$OUTPUT_FILE"
    echo "METHOD: $method" >> "$OUTPUT_FILE"
    echo "ENDPOINT: $endpoint" >> "$OUTPUT_FILE"

    if [ -n "$data" ]; then
        echo "REQUEST BODY: $data" >> "$OUTPUT_FILE"
        response=$(curl -s -w "\nHTTP_STATUS:%{http_code}" \
            -X "$method" \
            "$BASE_URL$endpoint" \
            -H "$AUTH_HEADER" \
            -H "Content-Type: application/json" \
            -d "$data" 2>&1)
    else
        response=$(curl -s -w "\nHTTP_STATUS:%{http_code}" \
            -X "$method" \
            "$BASE_URL$endpoint" \
            -H "$AUTH_HEADER" 2>&1)
    fi

    http_status=$(echo "$response" | grep "HTTP_STATUS:" | cut -d: -f2)
    body=$(echo "$response" | sed '/HTTP_STATUS:/d')

    echo "HTTP STATUS: $http_status" >> "$OUTPUT_FILE"
    echo "RESPONSE:" >> "$OUTPUT_FILE"
    echo "$body" | head -100 >> "$OUTPUT_FILE"

    if [ "$http_status" -ge 200 ] && [ "$http_status" -lt 300 ]; then
        echo -e "  ${GREEN}✓ Success${NC} (HTTP $http_status)"
        echo "RESULT: ✓ SUCCESS" >> "$OUTPUT_FILE"
    elif [ "$http_status" -eq 404 ]; then
        echo -e "  ${RED}✗ Not Found${NC} (HTTP 404)"
        echo "RESULT: ✗ NOT FOUND (404)" >> "$OUTPUT_FILE"
    else
        echo -e "  ${YELLOW}⚠ Failed${NC} (HTTP $http_status)"
        echo "RESULT: ⚠ FAILED ($http_status)" >> "$OUTPUT_FILE"
    fi
    echo ""
}

echo ""
echo -e "${YELLOW}========================================${NC}"
echo -e "${YELLOW}OpenL Tablets 6.0.0 API Endpoint Tests${NC}"
echo -e "${YELLOW}========================================${NC}"
echo ""

# Repository Management
echo -e "${BLUE}=== REPOSITORY MANAGEMENT ===${NC}"
test_endpoint "GET" "/repos" "List Repositories"
test_endpoint "GET" "/repos/$REPOSITORY/branches" "List Branches for $REPOSITORY"

# Project Management
echo -e "${BLUE}=== PROJECT MANAGEMENT ===${NC}"
test_endpoint "GET" "/projects" "List All Projects"
test_endpoint "GET" "/projects?repository=$REPOSITORY" "List Projects in $REPOSITORY"
test_endpoint "GET" "/projects/$PROJECT_ID" "Get Project by ID (direct)"
test_endpoint "GET" "/projects/$PROJECT_ID/info" "Get Project Info (separate endpoint)"
test_endpoint "POST" "/projects/$PROJECT_ID/open" "Open Project"
test_endpoint "POST" "/projects/$PROJECT_ID/close" "Close Project"
test_endpoint "GET" "/projects/$PROJECT_ID/validation" "Validate Project"

# Tables Management
echo -e "${BLUE}=== TABLES MANAGEMENT ===${NC}"
test_endpoint "GET" "/projects/$PROJECT_ID/tables" "List Tables"

# Get first table ID from list_tables response for further tests
echo -e "${BLUE}Getting first table ID for testing...${NC}"
tables_response=$(curl -s "$BASE_URL/projects/$PROJECT_ID/tables" -H "$AUTH_HEADER")
TABLE_ID=$(echo "$tables_response" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)

if [ -n "$TABLE_ID" ]; then
    echo -e "  ${GREEN}Found table ID:${NC} $TABLE_ID"
    test_endpoint "GET" "/projects/$PROJECT_ID/tables/$TABLE_ID" "Get Table Details"
    test_endpoint "GET" "/projects/$PROJECT_ID/tables/$TABLE_ID/properties" "Get Table Properties"
else
    echo -e "  ${YELLOW}⚠ No tables found, skipping table-specific tests${NC}"
fi

# Testing & Validation
echo -e "${BLUE}=== TESTING & VALIDATION ===${NC}"
test_endpoint "POST" "/projects/$PROJECT_ID/tests/run" "Run All Tests"

# File Management
echo -e "${BLUE}=== FILE MANAGEMENT ===${NC}"
test_endpoint "GET" "/projects/$PROJECT_ID/rules.xml" "Get rules.xml"

# Version Control
echo -e "${BLUE}=== VERSION CONTROL ===${NC}"
test_endpoint "GET" "/projects/$PROJECT_ID/history" "Get Project History"

# Deployment
echo -e "${BLUE}=== DEPLOYMENT ===${NC}"
test_endpoint "GET" "/deployments" "List Deployments"

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Testing Complete!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "Results saved to: ${BLUE}$OUTPUT_FILE${NC}"
echo ""
echo -e "${YELLOW}Next Steps:${NC}"
echo "1. Review $OUTPUT_FILE for detailed results"
echo "2. Share successful endpoints with development team"
echo "3. Update API_ENDPOINT_MAPPING.md with verified status"
echo ""
