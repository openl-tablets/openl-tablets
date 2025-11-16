# OpenL Tablets 6.0.0 Actual API Responses

This document catalogs the actual API responses from testing the OpenL instance.
Fill in the sections below as you test each endpoint.

---

## How to Use This Document

1. Run `./test-api-endpoints.sh` to test all endpoints
2. Check `api-test-results.txt` for results
3. For each successful endpoint, document the response structure below
4. Update API_ENDPOINT_MAPPING.md status accordingly

---

## 1. Repository Management

### GET /repos
**Status**: ❓ Not yet tested

**Example Response**:
```json
[Fill in actual response]
```

**Notes**:
- [Add any observations]

---

### GET /repos/{repository}/branches
**Status**: ❓ Not yet tested

**Example Request**: `GET /repos/design/branches`

**Example Response**:
```json
[Fill in actual response]
```

**Notes**:
- [Add any observations]

---

## 2. Project Management

### GET /projects
**Status**: ✅ Known to work

**Example Response**:
```json
[
  {
    "name": "Example 1 - Bank Rating",
    "modifiedBy": "DEFAULT",
    "modifiedAt": "2025-11-11T12:15:21-08:00",
    "branch": "master",
    "revision": "192cd6847b80c7b20a6995c4587bebd3b4a4d09c",
    "id": "ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n",
    "status": "OPENED",
    "comment": "Project Example 1 - Bank Rating is created.",
    "repository": "design",
    "selectedBranches": ["master"]
  }
]
```

**Notes**:
- Returns base64-encoded project IDs
- ID format: base64("repository:projectName")

---

### GET /projects/{id}
**Status**: ❓ Not yet tested

**Example Request**: `GET /projects/ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n`

**Example Response**:
```json
[Fill in actual response or error]
```

**Notes**:
- [Does this endpoint exist?]
- [What fields are returned?]

---

### GET /projects/{id}/info
**Status**: ❌ Known to fail (404)

**Example Request**: `GET /projects/ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n/info`

**Error Response**:
```
404 Not Found
```

**Notes**:
- This endpoint does not exist in OpenL 6.0.0
- Project info may be included in main project response

---

### POST /projects/{id}/open
**Status**: ❌ Known to fail (404)

**Example Request**: `POST /projects/ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n/open`

**Error Response**:
```
404 Not Found
```

**Notes**:
- This endpoint does not exist in OpenL 6.0.0
- Projects may be implicitly open/accessible

---

### POST /projects/{id}/close
**Status**: ❓ Not yet tested

**Example Request**: `POST /projects/ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n/close`

**Example Response**:
```json
[Fill in actual response or error]
```

**Notes**:
- [Does this endpoint exist?]

---

### POST /projects/{id}/save
**Status**: ❓ Not yet tested

**Example Request**:
```bash
POST /projects/ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n/save
Content-Type: application/json

{"comment": "Test save"}
```

**Example Response**:
```json
[Fill in actual response or error]
```

**Notes**:
- [What fields are in the response?]
- [Does it return commit hash?]

---

### GET /projects/{id}/validation
**Status**: ❓ Not yet tested

**Example Request**: `GET /projects/ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n/validation`

**Example Response**:
```json
[Fill in actual response]
```

**Notes**:
- [What is the structure of validation results?]
- [How are errors represented?]

---

## 3. Tables Management

### GET /projects/{id}/tables
**Status**: ✅ Known to work

**Example Request**: `GET /projects/ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n/tables`

**Example Response** (truncated):
```json
[
  {
    "id": "388cf75152fc76c44106546f1356e876",
    "tableType": "Spreadsheet",
    "kind": "Spreadsheet",
    "name": "BalanceDynamicIndexCalculation",
    "properties": {
      "description": "..."
    },
    "returnType": "Double",
    "signature": "BalanceDynamicIndexCalculation (FinancialData currentFinancialData, FinancialData previousFinancialData)",
    "file": "Example 1 - Bank Rating/Bank Rating.xlsx",
    "pos": "D54:G68"
  }
]
```

**Notes**:
- Table ID is a hash, not base64-encoded
- Includes signature, return type, position info

---

### GET /projects/{id}/tables/{tableId}
**Status**: ❓ Not yet tested

**Example Request**: `GET /projects/ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n/tables/388cf75152fc76c44106546f1356e876`

**Example Response**:
```json
[Fill in actual response]
```

**Notes**:
- [What additional details are returned?]
- [Does it include table data/rows?]

---

### PUT /projects/{id}/tables/{tableId}
**Status**: ❓ Not yet tested

**Example Request**:
```bash
PUT /projects/ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n/tables/388cf75152fc76c44106546f1356e876
Content-Type: application/json

{"view": {...}, "comment": "test update"}
```

**Example Response**:
```json
[Fill in actual response or error]
```

**Notes**:
- [What is the structure of the "view" object?]
- [Does it require project to be opened?]

---

### POST /projects/{id}/tables
**Status**: ❓ Not yet tested

**Example Request**:
```bash
POST /projects/ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n/tables
Content-Type: application/json

{
  "name": "TestRule",
  "type": "SimpleRules",
  "signature": "void TestRule()"
}
```

**Example Response**:
```json
[Fill in actual response or error]
```

**Notes**:
- [What fields are required?]
- [What does response include?]

---

### POST /projects/{id}/tables/{tableId}/copy
**Status**: ❓ Not yet tested

**Example Request**:
```bash
POST /projects/ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n/tables/388cf75152fc76c44106546f1356e876/copy
Content-Type: application/json

{"newName": "TestCopy"}
```

**Example Response**:
```json
[Fill in actual response or error]
```

**Notes**:
- [Does it return new table ID?]

---

### GET /projects/{id}/tables/{tableId}/properties
**Status**: ❓ Not yet tested

**Example Response**:
```json
[Fill in actual response]
```

---

### PUT /projects/{id}/tables/{tableId}/properties
**Status**: ❓ Not yet tested

**Example Response**:
```json
[Fill in actual response or error]
```

---

## 4. Testing & Validation

### POST /projects/{id}/tests/run
**Status**: ❓ Not yet tested

**Example Response**:
```json
[Fill in actual response]
```

**Notes**:
- [What is the test result structure?]
- [How long does it take?]

---

### POST /projects/{id}/tests/run-selected
**Status**: ❓ Not yet tested

**Example Response**:
```json
[Fill in actual response or error]
```

---

### POST /projects/{id}/rules/{ruleName}/execute
**Status**: ❓ Not yet tested

**Example Response**:
```json
[Fill in actual response or error]
```

**Notes**:
- [What formats are accepted for input?]
- [How are errors handled?]

---

## 5. File Management

### GET /projects/{id}/rules.xml
**Status**: ❓ Not yet tested

**Example Response**:
```xml
[Fill in actual XML response]
```

---

### PUT /projects/{id}/rules.xml/pattern
**Status**: ❓ Not yet tested

**Example Response**:
```json
[Fill in actual response or error]
```

---

### POST /projects/{id}/files/{filename}
**Status**: ❓ Not yet tested

**Example Response**:
```json
[Fill in actual response or error]
```

---

### GET /projects/{id}/files/{filename}
**Status**: ❓ Not yet tested

**Example Response**:
```
[Binary file or error]
```

---

## 6. Version Control

### GET /projects/{id}/history
**Status**: ❓ Not yet tested

**Example Response**:
```json
[Fill in actual response]
```

**Notes**:
- [What commit information is included?]
- [Is pagination supported?]

---

### GET /projects/{id}/files/{filename}/history
**Status**: ❓ Not yet tested

**Example Response**:
```json
[Fill in actual response]
```

---

### GET /projects/{id}/versions/compare
**Status**: ❓ Not yet tested

**Example Response**:
```json
[Fill in actual response or error]
```

---

### POST /projects/{id}/revert
**Status**: ❓ Not yet tested

**Example Response**:
```json
[Fill in actual response or error]
```

---

## 7. Deployment

### GET /deployments
**Status**: ❓ Not yet tested

**Example Response**:
```json
[Fill in actual response]
```

---

### POST /deployments/{deploymentRepository}
**Status**: ❓ Not yet tested

**Example Response**:
```json
[Fill in actual response or error]
```

---

## Summary Template

After testing, fill in this summary:

### Working Endpoints (2xx responses)
- [ ] List endpoints that work

### Not Found (404 responses)
- [x] GET /projects/{id}/info
- [x] POST /projects/{id}/open
- [ ] Other 404s

### Other Errors
- [ ] List endpoints with other error codes

### Untested
- [ ] List untested endpoints
