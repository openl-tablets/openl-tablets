# MCP Tools Review - OpenL Tablets API Comparison

**Date**: 2025-01-27  
**Version**: 1.0.0  
**Purpose**: Review MCP tools against OpenL Tablets REST API to identify missing inputs, extra parameters, and recommendations

---

## Repository Tools

### 1. `openl_list_repositories`

**Status**: ✅ Complete  
**OpenL API**: `GET /repos`

**Extra/Missed Inputs**:
- ✅ No missing inputs - API has no query parameters

**Recommendations**:
- None - tool matches API perfectly

---

### 2. `openl_list_branches`

**Status**: ✅ Complete  
**OpenL API**: `GET /repos/{repository}/branches`

**Extra/Missed Inputs**:
- ✅ No missing inputs - API has no query parameters

**Recommendations**:
- None - tool matches API perfectly

---

### 3. `openl_list_repository_features`

**Status**: ✅ Complete  
**OpenL API**: `GET /repos/{repository}/features`

**Extra/Missed Inputs**:
- ✅ No missing inputs

**Recommendations**:
- None

---

### 4. `openl_list_deploy_repositories`

**Status**: ✅ Complete  
**OpenL API**: `GET /production-repos`

**Extra/Missed Inputs**:
- ✅ No missing inputs - API has no query parameters

**Recommendations**:
- None

---

## Project Tools

### 5. `openl_list_projects`

**Status**: ✅ Complete  
**OpenL API**: `GET /projects?repository={repo}&status={status}&tags.{key}={value}`

**Extra/Missed Inputs**:
- ✅ All API parameters covered: `repository`, `status`, `tags`
- ✅ Pagination handled correctly (`limit`, `offset`)

**Recommendations**:
- None - tool matches API perfectly

---

### 6. `openl_get_project`

**Status**: ✅ Complete  
**OpenL API**: `GET /projects/{projectId}`

**Extra/Missed Inputs**:
- ✅ No missing inputs

**Recommendations**:
- None

---

### 7. `openl_update_project_status`

**Status**: ⚠️ Partial  
**OpenL API**: `PATCH /projects/{projectId}` with `ProjectStatusUpdateModel`

**Extra/Missed Inputs**:
- ✅ Covered: `status`, `comment`, `branch`, `revision`, `selectedBranches`
- ⚠️ **MISSING**: API supports `status` values: `LOCAL`, `ARCHIVED`, `OPENED`, `VIEWING_VERSION`, `EDITING`, `CLOSED`
  - Tool only allows `OPENED` and `CLOSED` (documentation says others are auto-set, but API accepts them)
- ✅ Client-side safety: `discardChanges` parameter (not in API, handled client-side)

**Recommendations**:
- Consider allowing all status values for advanced use cases, even if documentation warns they're typically auto-set
- Document that `EDITING` status cannot be set manually (it's set when project is modified)

---

## File Management Tools

### 8. `openl_upload_file`

**Status**: 🔴 DISABLED (Temporarily)  
**OpenL API**: `POST /projects/{projectId}/files/{fileName}?comment={comment}`

**Extra/Missed Inputs**:
- ✅ Covered: `projectId`, `fileName`, `fileContent` (base64), `comment`
- ✅ Content-Type header handled correctly (`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`)

**Recommendations**:
- **CRITICAL**: Re-enable this tool - it's essential for project management
- Consider adding validation for file size limits
- Add support for file path validation (ensure it matches project structure)

---

### 9. `openl_download_file`

**Status**: 🔴 DISABLED (Temporarily)  
**OpenL API**: `GET /projects/{projectId}/files/{fileName}?version={commitHash}`

**Extra/Missed Inputs**:
- ✅ Covered: `projectId`, `fileName`, `version` (optional commit hash)

**Recommendations**:
- **CRITICAL**: Re-enable this tool - it's essential for project management
- Consider adding file type detection/validation in response
- Add metadata in response: file size, last modified date, commit hash

---

## Table/Rule Tools

### 10. `openl_list_tables`

**Status**: ✅ Complete  
**OpenL API**: `GET /projects/{projectId}/tables?kind={kind[]}&name={name}&properties.{key}={value}`

**Extra/Missed Inputs**:
- ✅ All API parameters covered: `kind` (array), `name`, `properties`
- ✅ Pagination handled correctly

**Recommendations**:
- None - tool matches API perfectly

---

### 11. `openl_get_table`

**Status**: ✅ Complete  
**OpenL API**: `GET /projects/{projectId}/tables/{tableId}`

**Extra/Missed Inputs**:
- ✅ No missing inputs

**Recommendations**:
- None

---

### 12. `openl_update_table`

**Status**: ✅ Complete  
**OpenL API**: `PUT /projects/{projectId}/tables/{tableId}` with `EditableTableView`

**Extra/Missed Inputs**:
- ✅ Covered: `projectId`, `tableId`, `view` (full table structure)
- ⚠️ **NOTE**: `comment` parameter is accepted by tool but **NOT supported by OpenL API** (documented in client.ts line 814)
  - Tool accepts it but doesn't send it to API
  - Consider removing from tool schema or documenting that it's ignored

**Recommendations**:
- Remove `comment` parameter from schema (or document it's ignored) since API doesn't support it
- Consider adding validation to ensure `view` contains all required fields before sending

---

### 13. `openl_append_table`

**Status**: ✅ Complete  
**OpenL API**: `POST /projects/{projectId}/tables/{tableId}/lines` with `AppendTableView`

**Extra/Missed Inputs**:
- ✅ Covered: `projectId`, `tableId`, `appendData` (discriminated union by tableType)
- ⚠️ **NOTE**: `comment` parameter is accepted but **NOT supported by OpenL API**
  - Similar to `update_table` - consider removing or documenting

**Recommendations**:
- Remove `comment` parameter (or document it's ignored)
- Consider adding validation for tableType-specific append data structure

---

### 14. `openl_create_rule`

**Status**: 🔴 DISABLED (Temporarily)  
**OpenL API**: `POST /projects/{projectId}/tables` (returns 405 Method Not Allowed in OpenL 6.0.0)

**Extra/Missed Inputs**:
- ✅ Covered: `projectId`, `name`, `tableType`, `returnType`, `parameters`, `file`, `properties`, `comment`

**Recommendations**:
- **CRITICAL**: Tool is disabled because API returns 405 - table creation via REST is not supported
- Document that tables must be created via Excel file upload (`openl_upload_file`)
- Consider keeping tool disabled permanently with clear documentation, OR
- Implement a workaround: create empty Excel file with table structure and upload it

---

## Deployment Tools

### 15. `openl_list_deployments`

**Status**: ⚠️ Partial  
**OpenL API**: `GET /deployments?repository={repository}`

**Extra/Missed Inputs**:
- ❌ **MISSING**: `repository` query parameter (API supports filtering by repository)
  - Client method `listDeployments(repository?: string)` supports it
  - Tool schema doesn't include it

**Recommendations**:
- **ADD**: `repository` optional parameter to filter deployments by production repository
- Example: `openl_list_deployments(repository: "production-deploy")`

---

### 16. `openl_deploy_project`

**Status**: ✅ Complete  
**OpenL API**: `POST /deployments` with `DeployProjectRequest`

**Extra/Missed Inputs**:
- ✅ All API parameters covered: `projectId`, `deploymentName`, `productionRepositoryId`, `comment`

**Recommendations**:
- None

---

### 17. `openl_redeploy_project`

**Status**: ✅ Complete  
**OpenL API**: `POST /deployments/{deploymentId}` with `RedeployProjectRequest`

**Extra/Missed Inputs**:
- ✅ All API parameters covered: `deploymentId`, `projectId`, `comment`

**Recommendations**:
- None

---

## Version Control Tools

### 18. `openl_create_project_branch`

**Status**: ✅ Complete  
**OpenL API**: `POST /projects/{projectId}/branches` with `CreateBranchModel`

**Extra/Missed Inputs**:
- ✅ All API parameters covered: `projectId`, `branchName`, `revision` (optional)

**Recommendations**:
- None

---

### 19. `openl_repository_project_revisions`

**Status**: ✅ Complete  
**OpenL API**: `GET /repos/{repository}/projects/{projectName}/history?branch={branch}&search={search}&techRevs={techRevs}&page={page}&size={size}`

**Extra/Missed Inputs**:
- ✅ All API parameters covered: `repository`, `projectName`, `branch`, `search`, `techRevs`, `page`, `size`

**Recommendations**:
- None - tool matches API perfectly

---

### 20. `openl_list_project_local_changes`

**Status**: ✅ Complete  
**OpenL API**: `GET /history/project` (session-based, requires project to be open)

**Extra/Missed Inputs**:
- ✅ Covered: `projectId` (used for context, but endpoint is session-based)

**Recommendations**:
- Document that project must be opened in WebStudio session first
- Consider adding validation to check if project is open before calling

---

### 21. `openl_restore_project_local_change`

**Status**: ✅ Complete  
**OpenL API**: `POST /history/restore` with `historyId` (text/plain body)

**Extra/Missed Inputs**:
- ✅ Covered: `projectId`, `historyId`

**Recommendations**:
- Document that project must be opened in WebStudio session first

---

### 22. `openl_revert_version`

**Status**: 🔴 DISABLED (Temporarily)  
**OpenL API**: Not found in current API documentation

**Extra/Missed Inputs**:
- ⚠️ **ISSUE**: API endpoint may not exist or may be deprecated
- Client method exists but endpoint may be missing

**Recommendations**:
- Verify if endpoint exists: `POST /projects/{projectId}/revert` or similar
- If endpoint doesn't exist, remove client method and tool permanently
- If endpoint exists but uses different path, update client method

---

### 23. `openl_get_file_history`

**Status**: 🔴 DISABLED (Temporarily)  
**OpenL API**: Not found in current API documentation

**Extra/Missed Inputs**:
- ⚠️ **ISSUE**: API endpoint may not exist
- Client method exists but endpoint may be missing

**Recommendations**:
- Verify if endpoint exists: `GET /projects/{projectId}/files/{filePath}/history` or similar
- Consider using `openl_repository_project_revisions` as alternative (shows project-level history)
- If endpoint doesn't exist, document alternative approach

---

### 24. `openl_get_project_history`

**Status**: 🔴 DISABLED (Temporarily)  
**OpenL API**: Not found in current API documentation

**Extra/Missed Inputs**:
- ⚠️ **ISSUE**: API endpoint may not exist
- Client method exists but endpoint may be missing

**Recommendations**:
- **USE ALTERNATIVE**: `openl_repository_project_revisions` provides similar functionality
- Verify if endpoint exists: `GET /projects/{projectId}/history` or similar
- If endpoint doesn't exist, recommend using `openl_repository_project_revisions` instead
- Consider removing client method if endpoint doesn't exist

---

## Testing & Validation Tools

### 25. `openl_start_project_tests`

**Status**: ✅ Complete  
**OpenL API**: `POST /projects/{projectId}/tests/run?fromModule={tableId}&testRanges={ranges}`

**Extra/Missed Inputs**:
- ✅ All API parameters covered: `projectId`, `tableId` (as `fromModule`), `testRanges`
- ✅ Returns confirmation that tests have been started
- ✅ Separated from result retrieval for better control

**Recommendations**:
- ✅ Tool correctly handles async test execution start
- ✅ Use `openl_get_project_test_results` to retrieve results

---

### 26. `openl_get_project_test_results`

**Status**: ✅ Complete  
**OpenL API**: `GET /projects/{projectId}/tests/summary?failuresOnly={bool}&page={page}&size={size}`

**Extra/Missed Inputs**:
- ✅ All API parameters covered: `projectId`, `failuresOnly`, pagination
- ✅ `waitForCompletion` parameter allows immediate status check or polling until completion
- ✅ Tool correctly handles async test execution with polling mechanism

**Recommendations**:
- ✅ Polling mechanism implemented with exponential backoff
- ✅ Timeout handling for long-running tests
- ✅ Can return current status immediately if `waitForCompletion: false`

---

### 27. `openl_run_project_tests` (Deprecated)

**Status**: ⚠️ Deprecated  
**OpenL API**: `POST /projects/{projectId}/tests/run` then `GET /projects/{projectId}/tests/summary`

**Extra/Missed Inputs**:
- ✅ All API parameters covered: `projectId`, `tableId` (as `fromModule`), `testRanges`, `failuresOnly`, pagination
- ✅ Tool correctly handles async test execution (POST then GET summary with polling)

**Recommendations**:
- ⚠️ **DEPRECATED**: Use `openl_start_project_tests` followed by `openl_get_project_test_results` instead
- ✅ Maintained for backward compatibility
- ✅ Uses new methods internally

---

### 28. `openl_validate_project` (Missing Tool)

**Status**: ❌ MISSING TOOL  
**OpenL API**: `GET /projects/{projectId}/validation` (may return 404 - endpoint may not exist)

**Extra/Missed Inputs**:
- Client method exists: `validateProject(projectId)`
- Schema exists: `validateProjectSchema`
- **Tool is not registered** - missing from tool handlers

**Recommendations**:
- **ADD TOOL**: Create `openl_validate_project` tool
- Verify if endpoint exists (client.ts line 1082 notes it may return 404)
- If endpoint doesn't exist, use `openl_get_project_errors` as alternative
- If endpoint exists, register the tool

---

### 29. `openl_get_project_errors` (Missing Tool)

**Status**: ❌ MISSING TOOL  
**OpenL API**: Uses `GET /projects/{projectId}/validation` internally

**Extra/Missed Inputs**:
- Client method exists: `getProjectErrors(projectId, includeWarnings)`
- Schema exists: `getProjectErrorsSchema`
- **Tool is not registered** - missing from tool handlers

**Recommendations**:
- **ADD TOOL**: Create `openl_get_project_errors` tool
- This is a higher-level wrapper around validation with error categorization
- Very useful for debugging - should be exposed as a tool

---

## Execution Tools

### 30. `openl_execute_rule`

**Status**: 🔴 DISABLED (Temporarily)  
**OpenL API**: `POST /projects/{projectId}/rules/{ruleName}/execute` with input data

**Extra/Missed Inputs**:
- ✅ Covered: `projectId`, `ruleName`, `inputData`

**Recommendations**:
- **CRITICAL**: Re-enable this tool - it's essential for testing rules
- Consider adding timeout parameter
- Consider adding error handling for execution failures
- Document that project must be compiled/valid before execution

---

## Comparison Tools

### 31. `openl_compare_versions` (Missing Tool)

**Status**: ❌ MISSING TOOL  
**OpenL API**: `GET /projects/{projectId}/versions/compare?base={commitHash}&target={commitHash}`

**Extra/Missed Inputs**:
- Client method exists: `compareVersions(request)`
- Schema exists: `compareVersionsSchema`
- **Tool is not registered** - missing from tool handlers

**Recommendations**:
- **ADD TOOL**: Create `openl_compare_versions` tool
- Very useful for reviewing changes between versions
- Should be exposed as a tool

---

## Additional Client Methods Not Exposed as Tools

### 32. `deleteProject` (Missing Tool)

**Status**: ❌ MISSING TOOL  
**OpenL API**: `DELETE /projects/{projectId}`

**Extra/Missed Inputs**:
- Client method exists: `deleteProject(projectId)`
- **Tool is not registered**

**Recommendations**:
- **ADD TOOL**: Create `openl_delete_project` tool
- Mark as `destructiveHint: true`
- Require confirmation parameter
- Very useful for cleanup operations

---

### 33. `saveProject` (Missing Tool)

**Status**: ❌ MISSING TOOL  
**OpenL API**: `POST /projects/{projectId}/save?comment={comment}`

**Extra/Missed Inputs**:
- Client method exists: `saveProject(projectId, comment)`
- Schema exists: `saveProjectSchema`
- **Tool is not registered**

**Recommendations**:
- **ADD TOOL**: Create `openl_save_project` tool
- Currently `update_project_status` with `comment` can save, but dedicated tool is clearer
- Should validate project before saving (client already does this)

---

### 34. `openProject` / `closeProject` (Missing Tools)

**Status**: ❌ MISSING TOOLS  
**OpenL API**: `PATCH /projects/{projectId}` with `status: "OPENED"` or `status: "CLOSED"`

**Extra/Missed Inputs**:
- Client methods exist: `openProject(projectId, options)`, `closeProject(projectId, comment)`
- **Tools are not registered** (functionality exists in `update_project_status`)

**Recommendations**:
- Consider adding dedicated `openl_open_project` and `openl_close_project` tools for clarity
- OR document that `update_project_status` handles these cases
- Current approach (unified tool) is fine, but dedicated tools may be more intuitive

---

### 35. `healthCheck` (Missing Tool)

**Status**: ❌ MISSING TOOL  
**OpenL API**: Uses `GET /repos` as connectivity check

**Extra/Missed Inputs**:
- Client method exists: `healthCheck()`
- **Tool is not registered**

**Recommendations**:
- **ADD TOOL**: Create `openl_health_check` tool
- Very useful for debugging connection issues
- Should be exposed as a tool

---

## Summary

### Tools Status

| Status | Count | Tools |
|--------|-------|-------|
| ✅ Complete | 17 | All repository, project, table, deployment, branch, and test tools |
| ⚠️ Partial | 2 | `openl_update_project_status`, `openl_list_deployments` |
| 🔴 Disabled | 7 | `upload_file`, `download_file`, `create_rule`, `execute_rule`, `revert_version`, `get_file_history`, `get_project_history` |
| ❌ Missing | 8 | `validate_project`, `get_project_errors`, `compare_versions`, `delete_project`, `save_project`, `openl_open_project`, `openl_close_project`, `health_check` |

### Critical Issues

1. **Missing Inputs**:
   - `openl_list_deployments`: Missing `repository` filter parameter

2. **Extra Parameters** (not in API):
   - `openl_update_table`: `comment` parameter (ignored by API)
   - `openl_append_table`: `comment` parameter (ignored by API)

3. **Disabled Tools** (need re-enabling):
   - `openl_upload_file` - Essential for project management
   - `openl_download_file` - Essential for project management
   - `openl_execute_rule` - Essential for testing

4. **Missing Tools** (should be added):
   - `openl_validate_project` - Client method exists, tool missing
   - `openl_get_project_errors` - Client method exists, tool missing
   - `openl_compare_versions` - Client method exists, tool missing
   - `openl_delete_project` - Client method exists, tool missing
   - `openl_save_project` - Client method exists, tool missing
   - `openl_open_project` - Client method exists, tool missing (functionality available via `update_project_status`)
   - `openl_close_project` - Client method exists, tool missing (functionality available via `update_project_status`)
   - `openl_health_check` - Client method exists, tool missing

5. **API Endpoint Verification Needed**:
   - `revert_version` - Endpoint may not exist
   - `get_file_history` - Endpoint may not exist
   - `get_project_history` - Endpoint may not exist (use `repository_project_revisions` instead)
   - `validate_project` - Endpoint may return 404

### Recommendations Priority

**HIGH PRIORITY**:
1. Add `repository` parameter to `openl_list_deployments`
2. Re-enable `openl_upload_file`, `openl_download_file`, `openl_execute_rule`
3. Add missing tools: `openl_validate_project`, `openl_get_project_errors`, `openl_compare_versions`
4. Remove or document ignored `comment` parameters in `update_table` and `append_table`

**MEDIUM PRIORITY**:
5. Add `openl_delete_project` tool
6. Add `openl_save_project` tool
7. Add `openl_health_check` tool
8. Verify and fix/remove `revert_version`, `get_file_history`, `get_project_history` tools

**LOW PRIORITY**:
9. Add `openl_open_project` tool
10. Add `openl_close_project` tool
11. Add timeout parameters to long-running operations
12. Add polling mechanism for async test execution

---

## End of Review
