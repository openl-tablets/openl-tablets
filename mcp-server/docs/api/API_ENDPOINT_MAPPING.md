# OpenL Tablets 6.0.0 API Endpoint Mapping

## Status Legend
- ✅ **Verified Working** - Tested and confirmed working in user's instance
- ❌ **Verified Broken** - Tested and confirmed returning 404/error
- ❓ **Unknown** - Not yet tested
- 🔄 **Needs Verification** - Implementation exists but needs testing

---

## 1. Repository Management

### 1.1 list_repositories
- **Status**: 🔄 Needs Verification
- **Current Implementation**: `GET /repos`
- **Expected Response**: Array of repository objects
- **Test Command**:
  ```bash
  curl -X GET "http://localhost:8080/webstudio/rest/repos" \
    -H "Authorization: Basic $(echo -n 'admin:admin' | base64)"
  ```

### 1.2 list_branches
- **Status**: 🔄 Needs Verification
- **Current Implementation**: `GET /repos/{repository}/branches`
- **Parameters**: `repository` (e.g., "design")
- **Expected Response**: Array of branch names
- **Test Command**:
  ```bash
  curl -X GET "http://localhost:8080/webstudio/rest/repos/design/branches" \
    -H "Authorization: Basic $(echo -n 'admin:admin' | base64)"
  ```

---

## 2. Project Management

### 2.1 list_projects
- **Status**: ✅ Verified Working (from user logs)
- **Current Implementation**: `GET /projects`
- **Query Parameters**:
  - `repository` (optional filter)
  - `status` (optional filter)
  - `tag` (optional filter)
- **Expected Response**: Array of projects with base64-encoded IDs
- **Example Response**:
  ```json
  [
    {
      "name": "Example 1 - Bank Rating",
      "id": "ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n",
      "repository": "design",
      "status": "OPENED",
      ...
    }
  ]
  ```
- **Test Command**:
  ```bash
  curl -X GET "http://localhost:8080/webstudio/rest/projects" \
    -H "Authorization: Basic $(echo -n 'admin:admin' | base64)"
  ```

### 2.2 get_project
- **Status**: ❌ Verified Broken (404 error from user logs)
- **Current Implementation**:
  - Attempts: `GET /projects/{base64-id}/info` (404)
  - Fallback: Filters `GET /projects` results
- **User Log Error**: `GET /projects/ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n/info` → 404
- **Needs Investigation**:
  - Does `GET /projects/{base64-id}` work (without /info)?
  - Is filtering list_projects the correct approach?
- **Test Commands**:
  ```bash
  # Try direct project endpoint
  curl -X GET "http://localhost:8080/webstudio/rest/projects/ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n" \
    -H "Authorization: Basic $(echo -n 'admin:admin' | base64)"

  # Try with URL encoding
  curl -X GET "http://localhost:8080/webstudio/rest/projects/$(echo -n 'ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n' | jq -sRr @uri)" \
    -H "Authorization: Basic $(echo -n 'admin:admin' | base64)"
  ```

### 2.3 open_project
- **Status**: ❌ Verified Broken (404 error from user logs)
- **Current Implementation**: `POST /projects/{base64-id}/open`
- **User Log Error**: `POST /projects/ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n/open` → 404
- **Questions**:
  - Is there an open endpoint at all?
  - Are projects always open/accessible?
  - Is there a different lifecycle management approach?
- **Test Commands**:
  ```bash
  # Try POST to /open
  curl -X POST "http://localhost:8080/webstudio/rest/projects/ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n/open" \
    -H "Authorization: Basic $(echo -n 'admin:admin' | base64)"

  # Try PUT instead of POST
  curl -X PUT "http://localhost:8080/webstudio/rest/projects/ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n/open" \
    -H "Authorization: Basic $(echo -n 'admin:admin' | base64)"
  ```

### 2.4 close_project
- **Status**: ❓ Unknown (likely broken, same as open_project)
- **Current Implementation**: `POST /projects/{base64-id}/close`
- **Test Command**:
  ```bash
  curl -X POST "http://localhost:8080/webstudio/rest/projects/ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n/close" \
    -H "Authorization: Basic $(echo -n 'admin:admin' | base64)"
  ```

### 2.5 save_project
- **Status**: ❓ Unknown
- **Current Implementation**: `POST /projects/{base64-id}/save`
- **Request Body**: `{ "comment": "optional commit message" }`
- **Test Command**:
  ```bash
  curl -X POST "http://localhost:8080/webstudio/rest/projects/ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n/save" \
    -H "Authorization: Basic $(echo -n 'admin:admin' | base64)" \
    -H "Content-Type: application/json" \
    -d '{"comment": "Test save"}'
  ```

### 2.6 validate_project
- **Status**: ❓ Unknown
- **Current Implementation**: `GET /projects/{base64-id}/validation`
- **Test Command**:
  ```bash
  curl -X GET "http://localhost:8080/webstudio/rest/projects/ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n/validation" \
    -H "Authorization: Basic $(echo -n 'admin:admin' | base64)"
  ```

### 2.7 get_project_errors
- **Status**: ❓ Unknown
- **Current Implementation**: Calls `validate_project` and categorizes errors
- **Dependencies**: Needs validate_project to work

---

## 3. File Management

### 3.1 upload_file
- **Status**: ❓ Unknown
- **Current Implementation**: `POST /projects/{base64-id}/files/{filename}`
- **Headers**: `Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- **Test Command**:
  ```bash
  curl -X POST "http://localhost:8080/webstudio/rest/projects/ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n/files/test.xlsx" \
    -H "Authorization: Basic $(echo -n 'admin:admin' | base64)" \
    -H "Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" \
    --data-binary "@test.xlsx"
  ```

### 3.2 download_file
- **Status**: ❓ Unknown
- **Current Implementation**: `GET /projects/{base64-id}/files/{filename}`
- **Query Parameters**: `version` (optional Git commit hash)
- **Test Command**:
  ```bash
  curl -X GET "http://localhost:8080/webstudio/rest/projects/ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n/files/Example%201%20-%20Bank%20Rating%2FBank%20Rating.xlsx" \
    -H "Authorization: Basic $(echo -n 'admin:admin' | base64)" \
    -o downloaded.xlsx
  ```

---

## 4. Tables/Rules Management

### 4.1 list_tables
- **Status**: ✅ Verified Working (from user logs)
- **Current Implementation**: `GET /projects/{base64-id}/tables`
- **Query Parameters**:
  - `tableType` (optional filter)
  - `name` (optional filter with wildcards)
  - `file` (optional filter)
- **User Log Success**: Returns table list successfully
- **Test Command**:
  ```bash
  curl -X GET "http://localhost:8080/webstudio/rest/projects/ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n/tables" \
    -H "Authorization: Basic $(echo -n 'admin:admin' | base64)"
  ```

### 4.2 get_table
- **Status**: ❓ Unknown
- **Current Implementation**: `GET /projects/{base64-id}/tables/{tableId}`
- **Test Command**:
  ```bash
  curl -X GET "http://localhost:8080/webstudio/rest/projects/ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n/tables/388cf75152fc76c44106546f1356e876" \
    -H "Authorization: Basic $(echo -n 'admin:admin' | base64)"
  ```

### 4.3 update_table
- **Status**: ❓ Unknown
- **Current Implementation**: `PUT /projects/{base64-id}/tables/{tableId}`
- **Request Body**: `{ "view": {...}, "comment": "..." }`
- **Test Command**:
  ```bash
  curl -X PUT "http://localhost:8080/webstudio/rest/projects/ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n/tables/388cf75152fc76c44106546f1356e876" \
    -H "Authorization: Basic $(echo -n 'admin:admin' | base64)" \
    -H "Content-Type: application/json" \
    -d '{"view": {...}, "comment": "test update"}'
  ```

### 4.4 create_rule
- **Status**: ❓ Unknown
- **Current Implementation**: `POST /projects/{base64-id}/tables`
- **Request Body**: `{ "name": "...", "type": "...", "signature": "...", ... }`
- **Test Command**:
  ```bash
  curl -X POST "http://localhost:8080/webstudio/rest/projects/ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n/tables" \
    -H "Authorization: Basic $(echo -n 'admin:admin' | base64)" \
    -H "Content-Type: application/json" \
    -d '{"name": "TestRule", "type": "SimpleRules", "signature": "void TestRule()"}'
  ```

### 4.5 copy_table
- **Status**: ❓ Unknown
- **Current Implementation**: `POST /projects/{base64-id}/tables/{tableId}/copy`
- **Request Body**: `{ "newName": "...", "targetFile": "...", "comment": "..." }`
- **Test Command**:
  ```bash
  curl -X POST "http://localhost:8080/webstudio/rest/projects/ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n/tables/388cf75152fc76c44106546f1356e876/copy" \
    -H "Authorization: Basic $(echo -n 'admin:admin' | base64)" \
    -H "Content-Type: application/json" \
    -d '{"newName": "TestCopy"}'
  ```

### 4.6 get_table_properties
- **Status**: ❓ Unknown
- **Current Implementation**: `GET /projects/{base64-id}/tables/{tableId}/properties`
- **Test Command**:
  ```bash
  curl -X GET "http://localhost:8080/webstudio/rest/projects/ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n/tables/388cf75152fc76c44106546f1356e876/properties" \
    -H "Authorization: Basic $(echo -n 'admin:admin' | base64)"
  ```

### 4.7 set_table_properties
- **Status**: ❓ Unknown
- **Current Implementation**: `PUT /projects/{base64-id}/tables/{tableId}/properties`
- **Request Body**: `{ "properties": {...}, "comment": "..." }`
- **Test Command**:
  ```bash
  curl -X PUT "http://localhost:8080/webstudio/rest/projects/ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n/tables/388cf75152fc76c44106546f1356e876/properties" \
    -H "Authorization: Basic $(echo -n 'admin:admin' | base64)" \
    -H "Content-Type: application/json" \
    -d '{"properties": {"state": "CA"}, "comment": "test"}'
  ```

---

## 5. Testing & Validation

### 5.1 run_all_tests
- **Status**: ❓ Unknown
- **Current Implementation**: `POST /projects/{base64-id}/tests/run`
- **Test Command**:
  ```bash
  curl -X POST "http://localhost:8080/webstudio/rest/projects/ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n/tests/run" \
    -H "Authorization: Basic $(echo -n 'admin:admin' | base64)"
  ```

### 5.2 run_test
- **Status**: ❓ Unknown
- **Current Implementation**:
  - All tests: `POST /projects/{base64-id}/tests/run`
  - Selective: `POST /projects/{base64-id}/tests/run-selected`
- **Request Body**: `{ "testIds": [...], "tableIds": [...] }`
- **Test Command**:
  ```bash
  curl -X POST "http://localhost:8080/webstudio/rest/projects/ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n/tests/run-selected" \
    -H "Authorization: Basic $(echo -n 'admin:admin' | base64)" \
    -H "Content-Type: application/json" \
    -d '{"tableIds": ["388cf75152fc76c44106546f1356e876"]}'
  ```

### 5.3 execute_rule
- **Status**: ❓ Unknown
- **Current Implementation**: `POST /projects/{base64-id}/rules/{ruleName}/execute`
- **Request Body**: Input parameters as JSON object
- **Test Command**:
  ```bash
  curl -X POST "http://localhost:8080/webstudio/rest/projects/ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n/rules/BalanceDynamicIndexCalculation/execute" \
    -H "Authorization: Basic $(echo -n 'admin:admin' | base64)" \
    -H "Content-Type: application/json" \
    -d '{"currentFinancialData": {...}, "previousFinancialData": {...}}'
  ```

---

## 6. Deployment

### 6.1 list_deployments
- **Status**: ❓ Unknown
- **Current Implementation**: `GET /deployments`
- **Test Command**:
  ```bash
  curl -X GET "http://localhost:8080/webstudio/rest/deployments" \
    -H "Authorization: Basic $(echo -n 'admin:admin' | base64)"
  ```

### 6.2 deploy_project
- **Status**: ❓ Unknown
- **Current Implementation**: `POST /deployments/{deploymentRepository}`
- **Request Body**: `{ "projectName": "...", "repository": "...", "version": "..." }`
- **Test Command**:
  ```bash
  curl -X POST "http://localhost:8080/webstudio/rest/deployments/production-deploy" \
    -H "Authorization: Basic $(echo -n 'admin:admin' | base64)" \
    -H "Content-Type: application/json" \
    -d '{"projectName": "Example 1 - Bank Rating", "repository": "design"}'
  ```

---

## 7. Version Control

### 7.1 get_file_history
- **Status**: ❓ Unknown
- **Current Implementation**: `GET /projects/{base64-id}/files/{filePath}/history`
- **Query Parameters**: `limit`, `offset`
- **Test Command**:
  ```bash
  curl -X GET "http://localhost:8080/webstudio/rest/projects/ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n/files/Example%201%20-%20Bank%20Rating%2FBank%20Rating.xlsx/history" \
    -H "Authorization: Basic $(echo -n 'admin:admin' | base64)"
  ```

### 7.2 get_project_history
- **Status**: ❓ Unknown
- **Current Implementation**: `GET /projects/{base64-id}/history`
- **Query Parameters**: `limit`, `offset`, `branch`
- **Test Command**:
  ```bash
  curl -X GET "http://localhost:8080/webstudio/rest/projects/ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n/history" \
    -H "Authorization: Basic $(echo -n 'admin:admin' | base64)"
  ```

### 7.3 compare_versions
- **Status**: ❓ Unknown
- **Current Implementation**: `GET /projects/{base64-id}/versions/compare`
- **Query Parameters**: `base`, `target` (commit hashes)
- **Test Command**:
  ```bash
  curl -X GET "http://localhost:8080/webstudio/rest/projects/ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n/versions/compare?base=abc123&target=def456" \
    -H "Authorization: Basic $(echo -n 'admin:admin' | base64)"
  ```

### 7.4 revert_version
- **Status**: ❓ Unknown
- **Current Implementation**:
  - `GET /projects/{base64-id}/versions/{commitHash}` (retrieve)
  - `POST /projects/{base64-id}/revert` (revert)
- **Request Body**: `{ "targetVersion": "...", "comment": "..." }`
- **Test Command**:
  ```bash
  curl -X POST "http://localhost:8080/webstudio/rest/projects/ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n/revert" \
    -H "Authorization: Basic $(echo -n 'admin:admin' | base64)" \
    -H "Content-Type: application/json" \
    -d '{"targetVersion": "abc123", "comment": "Reverting to previous version"}'
  ```

---

## 8. Dimension Properties

### 8.1 get_file_name_pattern
- **Status**: ❓ Unknown
- **Current Implementation**: `GET /projects/{base64-id}/rules.xml`
- **Note**: Parses XML to extract pattern
- **Test Command**:
  ```bash
  curl -X GET "http://localhost:8080/webstudio/rest/projects/ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n/rules.xml" \
    -H "Authorization: Basic $(echo -n 'admin:admin' | base64)"
  ```

### 8.2 set_file_name_pattern
- **Status**: ❓ Unknown
- **Current Implementation**: `PUT /projects/{base64-id}/rules.xml/pattern`
- **Request Body**: `{ "pattern": "..." }`
- **Test Command**:
  ```bash
  curl -X PUT "http://localhost:8080/webstudio/rest/projects/ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n/rules.xml/pattern" \
    -H "Authorization: Basic $(echo -n 'admin:admin' | base64)" \
    -H "Content-Type: application/json" \
    -d '{"pattern": ".*-%state%-%lob%"}'
  ```

---

## Summary

### Status Breakdown
- ✅ **Verified Working**: 2 tools (list_projects, list_tables)
- ❌ **Verified Broken**: 2 tools (get_project, open_project)
- ❓ **Unknown**: 25 tools (need testing)

### Next Steps
1. **User to provide actual API responses** for the curl commands above
2. **Verify base URL structure** - is it `/projects/{base64-id}` or something else?
3. **Test critical paths first**:
   - Project lifecycle (get, open, close, save)
   - Table operations (get, update, create)
   - Testing (run tests)
4. **Document actual request/response formats** for each working endpoint
5. **Update client.ts** based on verified endpoints

### Questions for User
1. What is your OpenL Tablets base URL? (e.g., `http://localhost:8080/webstudio/rest`)
2. Can you run the curl commands for the ❌ and ❓ endpoints and share results?
3. Are there any OpenL Tablets 6.0.0 API documentation links?
4. Do you have access to OpenL Tablets admin/logs to see what endpoints actually exist?
