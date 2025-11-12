# MCP Tools Audit - Complete Implementation Mapping

This document maps all 29 MCP tools to their current implementations in `client.ts`.

---

## Priority Classification

- **P0 (Critical)**: Must work for basic functionality
- **P1 (Important)**: Needed for full workflow
- **P2 (Nice-to-have)**: Advanced features

---

## Tool-to-Implementation Mapping

| # | Tool Name | Priority | Client Method | API Endpoint(s) | Status | Notes |
|---|-----------|----------|---------------|----------------|--------|-------|
| 1 | list_repositories | P1 | `listRepositories()` | `GET /repos` | ❓ Unknown | Repository discovery |
| 2 | list_branches | P2 | `listBranches()` | `GET /repos/{repo}/branches` | ❓ Unknown | Branch management |
| 3 | **list_projects** | **P0** | `listProjects()` | `GET /projects` | ✅ **WORKS** | Critical - project discovery |
| 4 | **get_project** | **P0** | `getProject()` | Filters `GET /projects` | ❌ **BROKEN** | Uses workaround (filter list) |
| 5 | **open_project** | **P0** | `openProject()` | Calls `getProject()` | ❌ **BROKEN** | Just verifies exists |
| 6 | close_project | P1 | `closeProject()` | Calls `getProject()` | ❓ Unknown | No-op workaround |
| 7 | save_project | P0 | `saveProject()` | `POST /projects/{id}/save` | ❓ Unknown | Critical for persisting changes |
| 8 | upload_file | P1 | `uploadFile()` | `POST /projects/{id}/files/{name}` | ❓ Unknown | File upload |
| 9 | download_file | P1 | `downloadFile()` | `GET /projects/{id}/files/{name}` | ❓ Unknown | File download |
| 10 | **list_tables** | **P0** | `listTables()` | `GET /projects/{id}/tables` | ✅ **WORKS** | Critical - rules discovery |
| 11 | **get_table** | **P0** | `getTable()` | `GET /projects/{id}/tables/{tid}` | ❓ Unknown | View rule details |
| 12 | **update_table** | **P0** | `updateTable()` | `PUT /projects/{id}/tables/{tid}` | ❓ Unknown | Modify rules |
| 13 | create_rule | P1 | `createRule()` | `POST /projects/{id}/tables` | ❓ Unknown | Create new rules |
| 14 | list_deployments | P2 | `listDeployments()` | `GET /deployments` | ❓ Unknown | View deployments |
| 15 | deploy_project | P1 | `deployProject()` | `POST /deployments/{repo}` | ❓ Unknown | Production deployment |
| 16 | run_all_tests | P1 | `runAllTests()` | `POST /projects/{id}/tests/run` | ❓ Unknown | Full test suite |
| 17 | run_test | P1 | `runTest()` | `POST /projects/{id}/tests/run-selected` | ❓ Unknown | Selective testing |
| 18 | validate_project | P0 | `validateProject()` | `GET /projects/{id}/validation` | ❓ Unknown | Pre-save validation |
| 19 | get_project_errors | P1 | `getProjectErrors()` | Calls `validateProject()` + parsing | ❓ Unknown | Detailed error analysis |
| 20 | copy_table | P2 | `copyTable()` | `POST /projects/{id}/tables/{tid}/copy` | ❓ Unknown | Duplicate rules |
| 21 | execute_rule | P1 | `executeRule()` | `POST /projects/{id}/rules/{name}/execute` | ❓ Unknown | Test execution |
| 22 | compare_versions | P2 | `compareVersions()` | `GET /projects/{id}/versions/compare` | ❓ Unknown | Version diff |
| 23 | revert_version | P2 | `revertVersion()` | `POST /projects/{id}/revert` | ❓ Unknown | Rollback |
| 24 | get_file_history | P2 | `getFileHistory()` | `GET /projects/{id}/files/{path}/history` | ❓ Unknown | File version history |
| 25 | get_project_history | P1 | `getProjectHistory()` | `GET /projects/{id}/history` | ❓ Unknown | Project commit history |
| 26 | get_file_name_pattern | P2 | `getFileNamePattern()` | `GET /projects/{id}/rules.xml` | ❓ Unknown | Dimension properties config |
| 27 | set_file_name_pattern | P2 | `setFileNamePattern()` | `PUT /projects/{id}/rules.xml/pattern` | ❓ Unknown | Configure naming |
| 28 | get_table_properties | P2 | `getTableProperties()` | `GET /projects/{id}/tables/{tid}/properties` | ❓ Unknown | Read dimensions |
| 29 | set_table_properties | P2 | `setTableProperties()` | `PUT /projects/{id}/tables/{tid}/properties` | ❓ Unknown | Update dimensions |

---

## Critical Path Tools (P0) - Must Fix First

These 7 tools are essential for basic MCP functionality:

### 1. list_projects ✅
- **Status**: Working
- **Implementation**: Correct (`GET /projects`)
- **Action**: None needed

### 2. get_project ❌
- **Status**: Broken (uses filter workaround)
- **Current**: Filters `listProjects()` results
- **Problem**: Inefficient, may not return full details
- **Action**: Need to verify if `GET /projects/{id}` exists

### 3. open_project ❌
- **Status**: Broken (no-op workaround)
- **Current**: Just calls `getProject()`
- **Problem**: Doesn't actually open project
- **Action**: Determine if endpoint exists or if projects are always open

### 4. list_tables ✅
- **Status**: Working
- **Implementation**: Correct (`GET /projects/{id}/tables`)
- **Action**: None needed

### 5. get_table ❓
- **Status**: Unknown
- **Current**: `GET /projects/{id}/tables/{tableId}`
- **Action**: Test endpoint

### 6. update_table ❓
- **Status**: Unknown
- **Current**: `PUT /projects/{id}/tables/{tableId}`
- **Action**: Test endpoint

### 7. save_project ❓
- **Status**: Unknown
- **Current**: `POST /projects/{id}/save`
- **Action**: Test endpoint (critical for persisting changes!)

### 8. validate_project ❓
- **Status**: Unknown
- **Current**: `GET /projects/{id}/validation`
- **Action**: Test endpoint (needed before save)

---

## Detailed Implementation Analysis

### client.ts Method Breakdown

| Method | Lines | API Calls | Uses buildProjectPath? | Notes |
|--------|-------|-----------|----------------------|-------|
| `listRepositories()` | 81-86 | `GET /repos` | No | Direct endpoint |
| `listBranches()` | 94-99 | `GET /repos/{repo}/branches` | No | Uses encodeURIComponent |
| `parseProjectId()` | 117-147 | - | No | Parses 3 formats (dash/colon/base64) |
| `toBase64ProjectId()` | 158-172 | - | No | Converts to base64 |
| `buildProjectPath()` | 184-187 | - | No | Returns `/projects/{base64-id}` |
| `listProjects()` | 195-203 | `GET /projects` | No | Works correctly |
| `getProject()` | 213-230 | `GET /projects` (filtered) | No | **WORKAROUND - inefficient** |
| `openProject()` | 241-250 | Calls `getProject()` | No | **WORKAROUND - no-op** |
| `closeProject()` | 261-270 | Calls `getProject()` | No | **WORKAROUND - no-op** |
| `saveProject()` | 279-304 | `POST /projects/{id}/save` | Yes | Needs testing |
| `uploadFile()` | 319-370 | `POST /projects/{id}/files/{name}` | Yes | Needs testing |
| `downloadFile()` | 379-397 | `GET /projects/{id}/files/{name}` | Yes | Needs testing |
| `createBranch()` | 407-417 | `POST /projects/{id}/branches` | Yes | Needs testing |
| `listTables()` | 431-440 | `GET /projects/{id}/tables` | Yes | **WORKS** |
| `createRule()` | 450-493 | `POST /projects/{id}/tables` | Yes | Needs testing |
| `getTable()` | 501-509 | `GET /projects/{id}/tables/{tid}` | Yes | Needs testing |
| `updateTable()` | 519-530 | `PUT /projects/{id}/tables/{tid}` | Yes | Needs testing |
| `listDeployments()` | 541-547 | `GET /deployments` | No | Needs testing |
| `deployProject()` | 557-570 | `POST /deployments/{repo}` | No | Needs testing |
| `healthCheck()` | 583-614 | Calls `listRepositories()` | No | Tests connectivity |
| `runAllTests()` | 626-632 | `POST /projects/{id}/tests/run` | Yes | Needs testing |
| `validateProject()` | 640-647 | `GET /projects/{id}/validation` | Yes | Needs testing |
| `runTest()` | 655-682 | `POST /projects/{id}/tests/run[-selected]` | Yes | Needs testing |
| `getProjectErrors()` | 691-735 | Calls `validateProject()` | No | Depends on validate |
| `copyTable()` | 747-771 | `POST /projects/{id}/tables/{tid}/copy` | Yes | Needs testing |
| `executeRule()` | 779-802 | `POST /projects/{id}/rules/{name}/execute` | Yes | Needs testing |
| `compareVersions()` | 810-823 | `GET /projects/{id}/versions/compare` | Yes | Needs testing |
| `revertVersion()` | 836-877 | `POST /projects/{id}/revert` | Yes | Needs testing |
| `getFileHistory()` | 903-932 | `GET /projects/{id}/files/{path}/history` | Yes | Needs testing |
| `getProjectHistory()` | 940-970 | `GET /projects/{id}/history` | Yes | Needs testing |
| `getFileNamePattern()` | 1007-1021 | `GET /projects/{id}/rules.xml` | Yes | Needs testing |
| `setFileNamePattern()` | 1029-1041 | `PUT /projects/{id}/rules.xml/pattern` | Yes | Needs testing |
| `getTableProperties()` | 1049-1062 | `GET /projects/{id}/tables/{tid}/properties` | Yes | Needs testing |
| `setTableProperties()` | 1070-1087 | `PUT /projects/{id}/tables/{tid}/properties` | Yes | Needs testing |

---

## Issues Found

### 1. get_project Implementation (Lines 213-230)
**Problem**: Uses workaround instead of direct endpoint
```typescript
// Current implementation
const allProjects = await this.listProjects({ repository });
const project = allProjects.find(p => {
  const parsed = parseProjectIdUtil(p.id);
  return parsed.repository === repository && parsed.projectName === projectName;
});
```

**Why it's bad**:
- Fetches ALL projects in repository
- Inefficient for large repositories
- May not return complete project details
- Adds unnecessary latency

**Need to verify**:
- Does `GET /projects/{base64-id}` work?
- What response does it return?

### 2. open_project Implementation (Lines 241-250)
**Problem**: No-op workaround
```typescript
// Current implementation
async openProject(projectId: string): Promise<boolean> {
  try {
    await this.getProject(projectId);
    return true;
  } catch (error) {
    throw new Error(`Cannot open project: ${sanitizeError(error)}`);
  }
}
```

**Why it's bad**:
- Doesn't actually open the project
- Just verifies it exists
- May cause issues if project needs to be opened before editing

**Need to verify**:
- Is there an open endpoint?
- Are projects always accessible?
- What's the actual lifecycle?

### 3. URL Encoding Inconsistency
**Problem**: Some methods use `encodeURIComponent`, others don't

Examples:
- `listBranches()`: Uses `encodeURIComponent(repository)` ✓
- `buildProjectPath()`: Uses `encodeURIComponent(base64Id)` ✓
- But base64 IDs shouldn't need encoding since they're URL-safe

**Need to verify**:
- Are all URL encodings necessary?
- Should we encode base64 IDs?

---

## Testing Priority Order

Based on user's workflow and tool dependencies:

### Phase 1: Critical Path (P0)
1. ✅ list_projects - **Already working**
2. ❌ get_project - **Test `GET /projects/{id}` directly**
3. ❌ open_project - **Determine actual lifecycle**
4. ✅ list_tables - **Already working**
5. ❓ get_table - **Test endpoint**
6. ❓ update_table - **Test endpoint**
7. ❓ save_project - **Critical for persistence**
8. ❓ validate_project - **Needed before save**

### Phase 2: Important Workflow (P1)
9. ❓ close_project
10. ❓ run_all_tests
11. ❓ run_test
12. ❓ create_rule
13. ❓ execute_rule
14. ❓ get_project_history
15. ❓ upload_file
16. ❓ download_file

### Phase 3: Advanced Features (P2)
17-29. All other tools

---

## Next Steps for Phase 2

1. ✅ Create this comprehensive mapping
2. ⏭️ Run test script against actual OpenL instance
3. ⏭️ Document actual API responses
4. ⏭️ Identify which endpoints work vs need fixes
5. ⏭️ Update implementation plan based on findings
