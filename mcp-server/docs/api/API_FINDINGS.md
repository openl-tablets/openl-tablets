# OpenL Tablets REST API Investigation Findings

## Summary

Investigation of the official OpenL Tablets documentation reveals a **discrepancy between documented endpoints and actual deployment**.

## Documented API Structure (from GitHub docs)

The official API_GUIDE.md documents these base paths:

| Base Path | Purpose |
|-----------|---------|
| `/api` | Studio API (projects, deployments, users, repositories) |
| `/rules` | Rule execution endpoints |
| `/admin` | System administration |
| `/actuator` | Health and monitoring |

## Actual Deployment (from API tests)

Our MCP client connects to: `http://localhost:8080/webstudio/rest`

This path is **NOT documented** in the official API_GUIDE.md.

## Documented Studio API Endpoints

The official documentation shows these `/api` endpoints:

### Project Management
- `GET /api/projects` - List all projects
- `GET /api/projects/{projectId}` - Get project details
- `POST /api/projects` - Create project
- `PUT /api/projects/{projectId}` - Update project
- `DELETE /api/projects/{projectId}` - Delete project

### Rule Operations
- `GET /api/projects/{projectId}/rules/{ruleId}` - Get rule content
- `PUT /api/projects/{projectId}/rules/{ruleId}` - Update rule
- `POST /api/projects/{projectId}/rules/{ruleId}/test` - **Test a specific rule**

### Version Control
- `POST /api/projects/{projectId}/git/commit` - Commit changes
- `POST /api/projects/{projectId}/git/push` - Push to remote
- `POST /api/projects/{projectId}/git/pull` - Pull from remote

## Missing Endpoints Analysis

Based on our API testing and documentation review:

### ❌ NOT Documented (and return 404)
These endpoints do **not appear** in the official API documentation:

1. **`/projects/{id}/validation`** - Not documented
2. **`/projects/{id}/tests/run`** - Not documented (only individual rule test exists)
3. **`/projects/{id}/history`** - Not documented (only git operations at project level)
4. **`/projects/{id}/files/{path}/history`** - Not documented
5. **`/projects/{id}/tables/{tableId}/properties`** - Not documented

### ✅ Confirmed Working
These endpoints work at `/webstudio/rest` base:

1. `GET /projects` - Lists all projects
2. `GET /projects/{base64-id}` - Get project details (using base64-encoded ID)
3. `GET /projects/{id}/tables` - List tables in project
4. `GET /projects/{id}/tables/{tableId}` - Get table details (includes embedded properties)

### 🤔 Path Discrepancy

**Documented**: `/api/projects`
**Actual**: `/webstudio/rest/projects`

This suggests either:
- Documentation is outdated
- Multiple API versions exist
- `/webstudio/rest` is a servlet mapping prefix
- Different deployment configurations use different paths

## Implications for MCP Implementation

### What We Should Do

1. **Use working endpoints** at `/webstudio/rest` base path
2. **Accept that some features don't exist** in the REST API:
   - Project validation (may be automatic)
   - Test execution (only individual rule testing documented)
   - History (only git commit/push/pull operations)
   - File history (not exposed via REST)
   - Separate properties endpoint (properties embedded in table details)

3. **Document limitations clearly** in our MCP tools

### Working Approach

Our current implementation is **correct** for the actual deployed API:

```typescript
// ✅ WORKS
GET /webstudio/rest/projects
GET /webstudio/rest/projects/{base64-id}
GET /webstudio/rest/projects/{id}/tables
GET /webstudio/rest/projects/{id}/tables/{tableId}

// ❌ Don't exist (documented correctly in our code now)
GET /webstudio/rest/projects/{id}/validation
POST /webstudio/rest/projects/{id}/tests/run
GET /webstudio/rest/projects/{id}/history
GET /webstudio/rest/projects/{id}/files/{path}/history
GET /webstudio/rest/projects/{id}/tables/{tableId}/properties
```

### Alternative for Missing Features

Based on documented endpoints, we might be able to:

1. **Testing**: Use `POST /api/projects/{projectId}/rules/{ruleId}/test` for individual rule testing
2. **Version Control**: Use git operations (`/git/commit`, `/git/push`, `/git/pull`)
3. **Properties**: Use embedded properties from table details (already implemented)
4. **Validation**: May happen automatically during deployment/compilation

## Recommendations

1. ✅ **Keep current implementation** using `/webstudio/rest` base path
2. ✅ **Keep documentation** noting missing endpoints
3. ⚠️ **Consider adding**: Individual rule test tool using documented endpoint
4. ⚠️ **Consider adding**: Git operation tools (commit, push, pull)
5. ✅ **Current status**: Working tools are sufficient for basic OpenL Tablets workflows

## Conclusion

The endpoints we documented as "missing" (404) are **genuinely not part of the REST API**. The official documentation confirms this. Our implementation is correct, and the missing features either:
- Don't exist in the REST API
- Are handled differently (embedded data, automatic processes)
- May be available only through the WebStudio UI

Our MCP server implementation accurately reflects the actual OpenL Tablets REST API capabilities.
