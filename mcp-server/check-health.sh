#!/bin/bash

# Health check script for OpenL MCP Server
# Checks compilation, configuration, and basic connectivity

set -e

echo "🔍 OpenL MCP Server Health Check"
echo "=================================="
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check 1: Node.js version
echo "1️⃣  Checking Node.js version..."
NODE_VERSION=$(node --version)
REQUIRED_VERSION="v18.0.0"
if node -e "process.exit(require('semver').gte('$NODE_VERSION', '$REQUIRED_VERSION') ? 0 : 1)" 2>/dev/null || [ "$(printf '%s\n' "$REQUIRED_VERSION" "$NODE_VERSION" | sort -V | head -n1)" = "$REQUIRED_VERSION" ]; then
    echo -e "   ${GREEN}✓${NC} Node.js $NODE_VERSION (>= $REQUIRED_VERSION required)"
else
    echo -e "   ${RED}✗${NC} Node.js $NODE_VERSION (>= $REQUIRED_VERSION required)"
    exit 1
fi
echo ""

# Check 2: Dependencies installed
echo "2️⃣  Checking dependencies..."
if [ -d "node_modules" ]; then
    echo -e "   ${GREEN}✓${NC} Dependencies installed"
else
    echo -e "   ${RED}✗${NC} Dependencies not found. Run: npm install"
    exit 1
fi
echo ""

# Check 3: Build output
echo "3️⃣  Checking build output..."
if [ -f "dist/index.js" ]; then
    echo -e "   ${GREEN}✓${NC} Build output exists (dist/index.js)"
else
    echo -e "   ${YELLOW}⚠${NC}  Build output not found. Building..."
    npm run build
    if [ -f "dist/index.js" ]; then
        echo -e "   ${GREEN}✓${NC} Build successful"
    else
        echo -e "   ${RED}✗${NC} Build failed"
        exit 1
    fi
fi
echo ""

# Check 4: Environment variables
echo "4️⃣  Checking environment variables..."
MISSING_VARS=0

if [ -z "$OPENL_BASE_URL" ]; then
    echo -e "   ${RED}✗${NC} OPENL_BASE_URL not set"
    MISSING_VARS=1
else
    echo -e "   ${GREEN}✓${NC} OPENL_BASE_URL=$OPENL_BASE_URL"
fi

if [ -z "$OPENL_USERNAME" ] && [ -z "$OPENL_API_KEY" ] && [ -z "$OPENL_OAUTH2_CLIENT_ID" ]; then
    echo -e "   ${RED}✗${NC} No authentication method configured"
    echo -e "      Set one of: OPENL_USERNAME, OPENL_API_KEY, or OPENL_OAUTH2_CLIENT_ID"
    MISSING_VARS=1
else
    if [ -n "$OPENL_USERNAME" ]; then
        echo -e "   ${GREEN}✓${NC} OPENL_USERNAME set (Basic Auth)"
    fi
    if [ -n "$OPENL_API_KEY" ]; then
        echo -e "   ${GREEN}✓${NC} OPENL_API_KEY set (API Key Auth)"
    fi
    if [ -n "$OPENL_OAUTH2_CLIENT_ID" ]; then
        echo -e "   ${GREEN}✓${NC} OPENL_OAUTH2_CLIENT_ID set (OAuth 2.1)"
    fi
fi

if [ $MISSING_VARS -eq 1 ]; then
    echo ""
    echo -e "   ${YELLOW}ℹ${NC}  Set environment variables:"
    echo "      export OPENL_BASE_URL='http://localhost:8080/rest'"
    echo "      export OPENL_USERNAME='admin'"
    echo "      export OPENL_PASSWORD='admin'"
    echo ""
    echo "   Or check Claude Desktop config: ~/Library/Application Support/Claude/config.json"
fi
echo ""

# Check 5: TypeScript compilation
echo "5️⃣  Checking TypeScript compilation..."
if npm run build > /dev/null 2>&1; then
    echo -e "   ${GREEN}✓${NC} TypeScript compiles without errors"
else
    echo -e "   ${RED}✗${NC} TypeScript compilation errors found"
    npm run build
    exit 1
fi
echo ""

# Check 6: Linting
echo "6️⃣  Checking code quality (linting)..."
if npm run lint > /dev/null 2>&1; then
    echo -e "   ${GREEN}✓${NC} No linting errors"
else
    echo -e "   ${YELLOW}⚠${NC}  Linting warnings found (non-critical)"
fi
echo ""

# Check 7: Unit tests
echo "7️⃣  Running unit tests..."
if npm run test:unit > /dev/null 2>&1; then
    echo -e "   ${GREEN}✓${NC} Unit tests passed"
else
    echo -e "   ${YELLOW}⚠${NC}  Some unit tests failed (check output above)"
    echo "   Run 'npm test' for details"
fi
echo ""

# Check 8: OpenL API connectivity (if URL is set)
if [ -n "$OPENL_BASE_URL" ]; then
    echo "8️⃣  Checking OpenL API connectivity..."
    if command -v curl > /dev/null 2>&1; then
        # Try to connect to OpenL API
        HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 "$OPENL_BASE_URL/repository" 2>/dev/null || echo "000")
        if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "401" ] || [ "$HTTP_CODE" = "403" ]; then
            echo -e "   ${GREEN}✓${NC} OpenL API is reachable (HTTP $HTTP_CODE)"
            echo -e "      ${YELLOW}ℹ${NC}  HTTP $HTTP_CODE: API is accessible"
            if [ "$HTTP_CODE" = "401" ] || [ "$HTTP_CODE" = "403" ]; then
                echo -e "      ${YELLOW}ℹ${NC}  Authentication required (this is expected)"
            fi
        elif [ "$HTTP_CODE" = "000" ]; then
            echo -e "   ${RED}✗${NC} Cannot connect to OpenL API"
            echo -e "      ${YELLOW}ℹ${NC}  Make sure OpenL Tablets is running at: $OPENL_BASE_URL"
        else
            echo -e "   ${YELLOW}⚠${NC}  Unexpected HTTP response: $HTTP_CODE"
        fi
    else
        echo -e "   ${YELLOW}⚠${NC}  curl not available, skipping connectivity check"
    fi
    echo ""
fi

# Check 9: Claude Desktop config
echo "9️⃣  Checking Claude Desktop configuration..."
CLAUDE_CONFIG="$HOME/Library/Application Support/Claude/config.json"
if [ -f "$CLAUDE_CONFIG" ]; then
    if grep -q "openl-mcp-server" "$CLAUDE_CONFIG" 2>/dev/null; then
        echo -e "   ${GREEN}✓${NC} MCP server configured in Claude Desktop"
        # Check if path is correct
        ABS_PATH="/Users/asamuseu/IdeaProjects/openl-tablets/mcp-server/dist/index.js"
        if grep -q "$ABS_PATH" "$CLAUDE_CONFIG" 2>/dev/null; then
            echo -e "   ${GREEN}✓${NC} Path is correct"
        else
            echo -e "   ${YELLOW}⚠${NC}  Path might be incorrect, check config file"
        fi
    else
        echo -e "   ${YELLOW}⚠${NC}  MCP server not found in Claude Desktop config"
    fi
else
    echo -e "   ${YELLOW}⚠${NC}  Claude Desktop config not found at: $CLAUDE_CONFIG"
fi
echo ""

# Summary
echo "=================================="
echo -e "${GREEN}✅ Health check complete!${NC}"
echo ""
echo "Next steps:"
echo "  1. If OpenL API is not reachable, start OpenL Tablets"
echo "  2. Restart Claude Desktop to load MCP server"
echo "  3. In Claude Desktop, check MCP server status"
echo "  4. Try using an OpenL tool: 'List repositories' or 'List projects'"
echo ""

