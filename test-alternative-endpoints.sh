#!/bin/bash

# Test Alternative Endpoint Patterns
# Tests different URL patterns for endpoints that returned 404

set -e

# Configuration
BASE_URL="${1:-http://localhost:8080/webstudio/rest}"
USERNAME="${2:-admin}"
PASSWORD="${3:-admin}"
AUTH_HEADER="Authorization: Basic $(echo -n "$USERNAME:$PASSWORD" | base64)"

PROJECT_ID="ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n"
TABLE_ID="388cf75152fc76c44106546f1356e876"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Testing Alternative Endpoint Patterns${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

test_endpoint() {
    local method="$1"
    local endpoint="$2"
    local description="$3"
    local data="$4"

    echo -e "${YELLOW}Testing:${NC} $description"

    if [ -n "$data" ]; then
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
    body=$(echo "$response" | sed '/HTTP_STATUS:/d' | head -20)

    if [ "$http_status" -ge 200 ] && [ "$http_status" -lt 300 ]; then
        echo -e "  ${GREEN}✓ WORKS!${NC} (HTTP $http_status)"
        echo "  Response: $body" | head -5
        return 0
    elif [ "$http_status" -eq 404 ]; then
        echo -e "  ${RED}✗ 404${NC}"
        return 1
    else
        echo -e "  ${YELLOW}⚠ HTTP $http_status${NC}"
        return 1
    fi
    echo ""
}

# =============================================================================
# 1. Validation Endpoints
# =============================================================================
echo -e "${BLUE}=== VALIDATION ===${NC}"
test_endpoint "GET" "/projects/$PROJECT_ID/validate" "GET /validate"
test_endpoint "POST" "/projects/$PROJECT_ID/validate" "POST /validate"
test_endpoint "GET" "/projects/$PROJECT_ID/validation" "GET /validation (already tested)"
test_endpoint "POST" "/projects/$PROJECT_ID/validation" "POST /validation"
echo ""

# =============================================================================
# 2. Test Execution
# =============================================================================
echo -e "${BLUE}=== TEST EXECUTION ===${NC}"
test_endpoint "POST" "/projects/$PROJECT_ID/test" "POST /test"
test_endpoint "POST" "/projects/$PROJECT_ID/tests" "POST /tests (no /run)"
test_endpoint "POST" "/projects/$PROJECT_ID/tests/execute" "POST /tests/execute"
test_endpoint "GET" "/projects/$PROJECT_ID/tests" "GET /tests (list tests?)"
echo ""

# =============================================================================
# 3. Project History
# =============================================================================
echo -e "${BLUE}=== PROJECT HISTORY ===${NC}"
test_endpoint "GET" "/projects/$PROJECT_ID/commits" "GET /commits"
test_endpoint "GET" "/projects/$PROJECT_ID/versions" "GET /versions"
test_endpoint "GET" "/repos/design/history" "GET /repos/{repo}/history"
echo ""

# =============================================================================
# 4. Rules.xml
# =============================================================================
echo -e "${BLUE}=== RULES.XML ===${NC}"
test_endpoint "GET" "/projects/$PROJECT_ID/files/rules.xml" "GET /files/rules.xml"
test_endpoint "GET" "/projects/$PROJECT_ID/configuration" "GET /configuration"
test_endpoint "GET" "/projects/$PROJECT_ID/settings" "GET /settings"
echo ""

# =============================================================================
# 5. Table Properties (check if embedded in table details)
# =============================================================================
echo -e "${BLUE}=== TABLE PROPERTIES ===${NC}"
echo -e "${YELLOW}Checking if properties in table details...${NC}"
table_response=$(curl -s "$BASE_URL/projects/$PROJECT_ID/tables/$TABLE_ID" -H "$AUTH_HEADER")
has_properties=$(echo "$table_response" | grep -o '"properties"' | wc -l)
if [ "$has_properties" -gt 0 ]; then
    echo -e "  ${GREEN}✓ Properties ARE included in GET /tables/{id}${NC}"
    echo "  No separate endpoint needed!"
else
    echo -e "  ${YELLOW}⚠ Properties not found in table response${NC}"
fi
echo ""

# =============================================================================
# 6. Save/Update Endpoints (POST/PUT)
# =============================================================================
echo -e "${BLUE}=== SAVE/UPDATE OPERATIONS ===${NC}"
echo -e "${YELLOW}Note: These will fail without proper data, but we can check if endpoints exist${NC}"
test_endpoint "POST" "/projects/$PROJECT_ID/save" "POST /save" '{"comment":"test"}'
test_endpoint "PUT" "/projects/$PROJECT_ID" "PUT /projects/{id}" '{"comment":"test"}'
test_endpoint "POST" "/projects/$PROJECT_ID/commit" "POST /commit" '{"comment":"test"}'
echo ""

# =============================================================================
# 7. File Operations
# =============================================================================
echo -e "${BLUE}=== FILE OPERATIONS ===${NC}"
test_endpoint "GET" "/projects/$PROJECT_ID/files" "GET /files (list)"
test_endpoint "GET" "/repos/design/files" "GET /repos/{repo}/files"
echo ""

# =============================================================================
# 8. Deployment
# =============================================================================
echo -e "${BLUE}=== DEPLOYMENT ===${NC}"
echo "(Already confirmed GET /deployments works)"
test_endpoint "POST" "/deploy" "POST /deploy" "{\"projectId\":\"$PROJECT_ID\"}"
test_endpoint "POST" "/projects/$PROJECT_ID/deploy" "POST /projects/{id}/deploy"
echo ""

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Alternative Endpoint Testing Complete${NC}"
echo -e "${GREEN}========================================${NC}"
