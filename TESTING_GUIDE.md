# OpenL Tablets MCP Testing & Verification Guide

This guide provides step-by-step instructions for verifying and fixing the MCP server implementation.

---

## Overview

The MCP server has been implemented based on assumptions about the OpenL Tablets 6.0.0 API. We need to verify these assumptions against your actual OpenL instance and fix any issues.

## Current Status

- ✅ **Working**: `list_projects`, `list_tables`
- ❌ **Broken**: `get_project`, `open_project` (using workarounds)
- ❓ **Unknown**: 25 other tools (need testing)

---

## Phase 1: API Endpoint Discovery ✅ COMPLETE

### Deliverables Created

1. **API_ENDPOINT_MAPPING.md** - Complete endpoint inventory with curl examples
2. **test-api-endpoints.sh** - Automated testing script
3. **API_RESPONSES.md** - Template for documenting actual responses

### Next Steps for User

**Run the endpoint testing script:**

```bash
cd /home/user/openl-tablets

# Make script executable (if not already)
chmod +x test-api-endpoints.sh

# Run tests
./test-api-endpoints.sh "http://localhost:8080/webstudio/rest" "admin" "admin"

# Review results
cat api-test-results.txt
```

**What to look for:**
- Which endpoints return 200 (success)?
- Which endpoints return 404 (not found)?
- What are the actual response structures?

---

## Phase 2: MCP Tools Audit ✅ COMPLETE

### Deliverable Created

**MCP_TOOLS_AUDIT.md** - Complete mapping of all 29 tools to their implementations

### Key Findings

1. **Priority Classification**:
   - P0 (Critical): 8 tools - must work for basic functionality
   - P1 (Important): 9 tools - needed for full workflow
   - P2 (Nice-to-have): 12 tools - advanced features

2. **Implementation Issues**:
   - `get_project` uses inefficient filtering workaround
   - `open_project` is a no-op (just verifies existence)
   - `close_project` is a no-op (just verifies existence)

3. **Code Quality**:
   - Inconsistent URL encoding
   - Misleading version-change comments
   - Some `as any` type casts

---

## Phase 3: Integration Test Suite ✅ COMPLETE

### Deliverables Created

1. **tests/integration/openl-live.test.ts** - Comprehensive integration tests
2. **tests/integration/README.md** - Test documentation
3. **package.json** - Updated with `test:integration` script

### How to Run Integration Tests

**Prerequisites:**
1. OpenL Tablets 6.0.0 instance running
2. Access credentials (username/password)

**Setup:**

```bash
cd /home/user/openl-tablets/mcp-server

# Create test environment file
cat > .env.test << EOF
OPENL_BASE_URL=http://localhost:8080/webstudio/rest
OPENL_USERNAME=admin
OPENL_PASSWORD=admin
SKIP_LIVE_TESTS=false
EOF

# Build the project
npm run build

# Run integration tests
npm run test:integration
```

**What to expect:**
- Tests connect to your OpenL instance
- Tests run through critical workflows
- Results show which tools work vs fail
- Console output shows actual API responses

---

## Phase 4: Code Cleanup & Fixes ⏳ PENDING

### Deliverable Created

**CODE_CLEANUP_PLAN.md** - Detailed cleanup plan

### What Needs to Happen

**IMPORTANT**: Do NOT proceed with Phase 4 until after running tests!

1. **Wait for test results** from Phase 1 and Phase 3
2. **Document actual API behavior** in API_RESPONSES.md
3. **Update implementations** based on real API, not guesses
4. **Remove misleading comments** about version changes
5. **Standardize code** (error handling, URL encoding, types)

---

## Phase 5: Final Validation ⏳ PENDING

Will include:
- API reference documentation
- Examples for each MCP tool
- Troubleshooting guide
- Known limitations

---

## Recommended Testing Workflow

### Step 1: Manual Endpoint Testing (15 minutes)

```bash
# Run the automated endpoint tester
./test-api-endpoints.sh

# Review results
less api-test-results.txt

# Look for patterns:
# - Which endpoint patterns work?
# - Which return 404?
# - Are there alternative endpoints?
```

### Step 2: Integration Test Run (5 minutes)

```bash
cd mcp-server

# Run integration tests
npm run test:integration 2>&1 | tee integration-test-output.txt

# Review results
less integration-test-output.txt
```

### Step 3: Document Findings (10 minutes)

Fill in **API_RESPONSES.md** with:
- Actual endpoint URLs that work
- Response structures
- Error messages for failed endpoints
- Any alternative approaches discovered

### Step 4: Share Results

Provide these files:
1. `api-test-results.txt` - Manual endpoint test results
2. `integration-test-output.txt` - Integration test output
3. `API_RESPONSES.md` - Filled in with actual responses

### Step 5: Implement Fixes

Based on results, update:
1. **client.ts** - Fix broken implementations
2. **types.ts** - Adjust types if needed
3. Remove misleading comments
4. Standardize approach

---

## Quick Start Checklist

- [ ] OpenL Tablets 6.0.0 instance is running
- [ ] Have admin credentials
- [ ] Run `./test-api-endpoints.sh`
- [ ] Review `api-test-results.txt`
- [ ] Run `npm run test:integration`
- [ ] Review integration test output
- [ ] Fill in `API_RESPONSES.md`
- [ ] Share results for Phase 4 implementation

---

## Common Issues & Solutions

### Issue: Endpoints return 404

**Possible causes:**
1. Endpoint doesn't exist in API
2. Wrong URL structure
3. Project ID format incorrect
4. Authentication issue

**Investigation:**
- Check OpenL API documentation
- Try alternative URL patterns
- Verify base64 encoding is correct
- Test with curl manually

### Issue: Endpoints return 401/403

**Solution:**
- Verify credentials in .env.test
- Check authentication method (Basic Auth)
- Ensure user has required permissions

### Issue: Tests timeout

**Possible causes:**
- OpenL instance is slow
- Network latency
- Large project/table operations

**Solution:**
- Increase timeout in tests
- Check OpenL instance performance
- Use smaller test projects

---

## Files Reference

### Documentation
- `API_ENDPOINT_MAPPING.md` - Endpoint inventory
- `API_RESPONSES.md` - Actual response documentation
- `MCP_TOOLS_AUDIT.md` - Tool implementation mapping
- `CODE_CLEANUP_PLAN.md` - Cleanup task list
- `TESTING_GUIDE.md` - This file

### Scripts
- `test-api-endpoints.sh` - Automated endpoint tester
- `mcp-server/package.json` - NPM scripts

### Tests
- `mcp-server/tests/integration/openl-live.test.ts` - Integration tests
- `mcp-server/tests/integration/README.md` - Test documentation

### Source Code
- `mcp-server/src/client.ts` - Main API client
- `mcp-server/src/utils.ts` - Utility functions
- `mcp-server/src/types.ts` - Type definitions
- `mcp-server/src/tools.ts` - MCP tool definitions

---

## Success Criteria

At the end of this process, we should have:

1. ✅ **Complete API documentation** - All endpoints mapped and documented
2. ✅ **Working implementations** - All P0 tools functioning correctly
3. ✅ **Clean codebase** - No misleading comments or workarounds
4. ✅ **Passing tests** - Both unit and integration tests pass
5. ✅ **User-friendly MCP** - Claude Desktop can use all tools effectively

---

## Contact & Support

If you encounter issues:
1. Check this guide first
2. Review test output for errors
3. Check OpenL Tablets documentation
4. Document unexpected behavior in API_RESPONSES.md

---

**Ready to proceed?** Start with Step 1: Manual Endpoint Testing
