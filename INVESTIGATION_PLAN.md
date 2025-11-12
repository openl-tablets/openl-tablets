# Investigation Plan for Missing Endpoints

Based on test results, 8 endpoints returned 404. Some may have alternative URLs.

## Confirmed Working ✅
1. `GET /projects/{base64-id}` - **Works!** Need to fix client.ts

## Missing Endpoints - Investigation Needed

### 1. Validation Endpoint
- **Failed**: `GET /projects/{id}/validation`
- **Alternative possibilities**:
  - Maybe validation happens automatically when getting project?
  - Might be at WebStudio UI level, not REST API
  - Could check project response for validation status fields
- **Action**: Check if project response includes validation info

### 2. Test Execution
- **Failed**: `POST /projects/{id}/tests/run`
- **Alternative possibilities**:
  - Might be at different path: `/projects/{id}/test`
  - Could be WebStudio UI feature only
  - Test tables might need to be executed differently
- **Action**: Try alternative paths, check WebStudio API docs

### 3. Project History
- **Failed**: `GET /projects/{id}/history`
- **Alternative possibilities**:
  - Might be at repository level: `/repos/{repo}/projects/{name}/history`
  - Could use Git revision info from project response
  - Might need to query Git directly
- **Action**: Check if revision/branch info in project response is sufficient

### 4. Table Properties
- **Failed**: `GET /projects/{id}/tables/{tableId}/properties`
- **Looking at successful response**: `GET /projects/{id}/tables/{tableId}` returns:
  ```json
  {
    "properties": {"description": "..."},
    ...
  }
  ```
- **Conclusion**: Properties ARE included in table details!
- **Action**: Use existing get_table response, no separate endpoint needed

### 5. Rules.xml
- **Failed**: `GET /projects/{id}/rules.xml`
- **Alternative possibilities**:
  - Might be under files: `/projects/{id}/files/rules.xml`
  - Could be repository-level config
  - Might not be exposed via API
- **Action**: Try file download endpoint

### 6. File History
- **Failed**: `GET /projects/{id}/files/{path}/history`
- **Alternative possibilities**:
  - Use project history and filter by file
  - Might not be available in API
- **Action**: Check if project history covers files

### 7. Open/Close Project
- **Failed**: `POST /projects/{id}/open` and `/close`
- **Conclusion**: Projects are always accessible in OpenL 6.0.0
- **Action**: Document as no-ops, remove from critical tools

## Test Script for Alternative URLs

```bash
#!/bin/bash
PROJECT_ID="ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n"
BASE_URL="http://localhost:8080/webstudio/rest"
AUTH="Authorization: Basic $(echo -n 'admin:admin' | base64)"

echo "Testing alternative endpoint patterns..."

# Try validation at different paths
curl -s -w "\nHTTP: %{http_code}\n" -X GET "$BASE_URL/projects/$PROJECT_ID/validate" -H "$AUTH"
curl -s -w "\nHTTP: %{http_code}\n" -X POST "$BASE_URL/projects/$PROJECT_ID/validate" -H "$AUTH"

# Try test execution alternatives
curl -s -w "\nHTTP: %{http_code}\n" -X POST "$BASE_URL/projects/$PROJECT_ID/test" -H "$AUTH"
curl -s -w "\nHTTP: %{http_code}\n" -X POST "$BASE_URL/projects/$PROJECT_ID/tests" -H "$AUTH"

# Try rules.xml as file
curl -s -w "\nHTTP: %{http_code}\n" -X GET "$BASE_URL/projects/$PROJECT_ID/files/rules.xml" -H "$AUTH"

# Try history at repository level
curl -s -w "\nHTTP: %{http_code}\n" -X GET "$BASE_URL/repos/design/projects/Example%201%20-%20Bank%20Rating/history" -H "$AUTH"
```

## Immediate Actions

### 1. Fix get_project (Confirmed Working)
Replace workaround with direct endpoint call.

### 2. Use Embedded Properties
get_table_properties should parse the existing properties field from get_table response.

### 3. Document No-Ops
open_project and close_project are no-ops - document clearly.

### 4. Run Alternative Tests
Test the alternative URLs above to find working patterns.

### 5. Update Tools
Based on findings, update or remove tools without API support.
