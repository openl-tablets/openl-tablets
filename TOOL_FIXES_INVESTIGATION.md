# Tool Fixes Investigation - download_file and create_rule

## Investigation Date
2025-11-13

## Issues Discovered

### 1. download_file - 400 Bad Request

#### Error Log
```
Message from client: {
  "method":"tools/call",
  "params":{
    "name":"download_file",
    "arguments":{
      "projectId":"design-Example 1 - Bank Rating",
      "fileName":"Example 1 - Bank Rating/Bank Rating.xlsx"
    }
  }
}

Message from server: {
  "error":{
    "code":-32603,
    "message":"OpenL Tablets API error (400): Request failed with status code 400 [GET /projects/ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n/files/Example%201%20-%20Bank%20Rating%2FBank%20Rating.xlsx]"
  }
}
```

#### Analysis

**Current Implementation** (`src/client.ts:368-386`):
```typescript
async downloadFile(projectId: string, fileName: string, version?: string): Promise<Buffer> {
  const projectPath = this.buildProjectPath(projectId);
  const response = await this.axiosInstance.get<ArrayBuffer>(
    `${projectPath}/files/${encodeURIComponent(fileName)}`,
    {
      responseType: "arraybuffer",
      params: version ? { version } : {},
    }
  );
  return Buffer.from(response.data);
}
```

**Problem Identified**:
- The `fileName` parameter includes the project directory: `"Example 1 - Bank Rating/Bank Rating.xlsx"`
- This creates a redundant path structure since `projectPath` already identifies the project
- The actual API endpoint becomes: `/projects/ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n/files/Example%201%20-%20Bank%20Rating%2FBank%20Rating.xlsx`
- OpenL likely expects just the file name relative to the project: `"Bank Rating.xlsx"`

**Root Cause**:
The file path returned by `list_tables` includes the project directory prefix, which users then pass directly to `download_file`. The tool should handle this by stripping the project prefix.

**Expected Behavior**:
- Input: `fileName: "Example 1 - Bank Rating/Bank Rating.xlsx"`
- Should extract: `"Bank Rating.xlsx"`
- Or handle nested directories properly

### 2. create_rule - 405 Method Not Allowed

#### Error Log
```
Message from client: {
  "method":"tools/call",
  "params":{
    "name":"create_rule",
    "arguments":{
      "projectId":"design-Example 1 - Bank Rating",
      "name":"Tier1CapitalRatioScore",
      "tableType":"SimpleRules",
      "returnType":"Double",
      "parameters":[{"name":"tier1CapitalRatio","type":"Double"}],
      "file":"Example 1 - Bank Rating/Bank Rating.xlsx",
      "comment":"Add Tier 1 Capital Ratio scoring rule based on Basel III standards"
    }
  }
}

Message from server: {
  "result":{
    "content":[{
      "type":"text",
      "text":"{\n  \"success\": false,\n  \"message\": \"Failed to create SimpleRules table 'Tier1CapitalRatioScore': Request failed with status code 405\"\n}"
    }]
  }
}
```

#### Analysis

**Current Implementation** (`src/client.ts:439-481`):
```typescript
async createRule(projectId: string, request: Types.CreateRuleRequest): Promise<Types.CreateRuleResult> {
  const projectPath = this.buildProjectPath(projectId);

  try {
    const response = await this.axiosInstance.post(
      `${projectPath}/tables`,
      {
        name: request.name,
        type: request.tableType,
        signature,
        returnType: request.returnType,
        parameters: request.parameters,
        properties: request.properties,
        file: request.file,
        comment: request.comment,
      }
    );

    return {
      success: true,
      tableId: response.data.id || `${request.name}-${request.tableType}`,
      ...
    };
  } catch (error: unknown) {
    return {
      success: false,
      message: `Failed to create ${request.tableType} table '${request.name}': ${sanitizeError(error)}`,
    };
  }
}
```

**Problem Identified**:
- HTTP 405 (Method Not Allowed) indicates `POST /projects/{id}/tables` doesn't exist or doesn't support POST
- According to API documentation review (API_FINDINGS.md), table creation endpoints are not documented in the official REST API
- OpenL Tablets uses Excel files to define tables - tables are not created via REST API directly

**Root Cause**:
The REST API may not support direct table creation. Tables in OpenL are defined within Excel files, which means:
- Tables exist as Excel worksheet structures
- Creating a table requires modifying an Excel file
- The REST API likely only supports file upload/download, not table creation

**Possible Solutions**:
1. **Remove the tool** - If the REST API doesn't support it
2. **Excel manipulation** - Download file, modify using Excel library, upload
3. **Different endpoint** - Find if there's an undocumented endpoint that works

## Next Steps

### Phase 1: Test API Endpoints Directly

1. **Test download_file with corrected path**:
```bash
# Test with just the file name
curl -X GET "http://localhost:8080/webstudio/rest/projects/ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n/files/Bank%20Rating.xlsx" \
  -H "Authorization: Basic $(echo -n 'admin:admin' | base64)" \
  -o test-download.xlsx

# Test with full path (current behavior)
curl -X GET "http://localhost:8080/webstudio/rest/projects/ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n/files/Example%201%20-%20Bank%20Rating%2FBank%20Rating.xlsx" \
  -H "Authorization: Basic $(echo -n 'admin:admin' | base64)" \
  -o test-download-full.xlsx
```

2. **Test create_rule with different methods**:
```bash
# Try POST to /tables (current approach)
curl -v -X POST "http://localhost:8080/webstudio/rest/projects/ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n/tables" \
  -H "Authorization: Basic $(echo -n 'admin:admin' | base64)" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "TestRule",
    "type": "SimpleRules",
    "signature": "Double TestRule(Double x)",
    "file": "Bank Rating.xlsx"
  }'

# Try PUT to /tables (alternative)
curl -v -X PUT "http://localhost:8080/webstudio/rest/projects/ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n/tables" \
  -H "Authorization: Basic $(echo -n 'admin:admin' | base64)" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "TestRule",
    "type": "SimpleRules",
    "signature": "Double TestRule(Double x)",
    "file": "Bank Rating.xlsx"
  }'

# Try POST to /files/{fileName}/tables (alternative structure)
curl -v -X POST "http://localhost:8080/webstudio/rest/projects/ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n/files/Bank%20Rating.xlsx/tables" \
  -H "Authorization: Basic $(echo -n 'admin:admin' | base64)" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "TestRule",
    "type": "SimpleRules",
    "signature": "Double TestRule(Double x)"
  }'
```

### Phase 2: Implement Fixes

#### Fix 1: download_file - Handle File Path Properly

**Option A: Strip project directory prefix**
```typescript
async downloadFile(projectId: string, fileName: string, version?: string): Promise<Buffer> {
  const projectPath = this.buildProjectPath(projectId);

  // Extract project name from projectId (format: "repository-projectName")
  const projectName = projectId.split('-').slice(1).join('-');

  // Strip project directory prefix if present
  let normalizedFileName = fileName;
  if (fileName.startsWith(projectName + '/')) {
    normalizedFileName = fileName.substring(projectName.length + 1);
  }

  const response = await this.axiosInstance.get<ArrayBuffer>(
    `${projectPath}/files/${encodeURIComponent(normalizedFileName)}`,
    {
      responseType: "arraybuffer",
      params: version ? { version } : {},
    }
  );
  return Buffer.from(response.data);
}
```

**Option B: Keep full path but encode differently**
- Test if double-encoding helps: `encodeURIComponent(encodeURIComponent(fileName))`
- Test if path segments should be encoded separately

#### Fix 2: create_rule - Remove or Reimplement

**Option A: Remove the tool entirely**
- Update tools.ts to remove create_rule
- Document limitation in README
- Guide users to use OpenL WebStudio UI for table creation

**Option B: Implement Excel-based creation**
- Use exceljs library to manipulate Excel files
- Download existing file (or create new)
- Add table structure to worksheet
- Upload modified file
- **Complexity**: High, requires understanding OpenL Excel format

**Option C: Find working endpoint (if exists)**
- Test alternative endpoints from Phase 1
- Update implementation if one works

### Phase 3: Testing & Verification

1. **Unit tests** for download_file with various file path formats
2. **Integration tests** with real OpenL instance
3. **Update tool documentation** with correct usage
4. **Update error messages** to guide users

### Phase 4: Documentation

1. Document the root causes
2. Update README with limitations
3. Add examples of correct usage
4. Note any breaking changes

## Questions to Answer

1. ✅ What is the correct file path format for download_file?
   - Need to test: just filename vs. full path

2. ✅ Does the REST API support table creation at all?
   - Need to test: alternative endpoints

3. ❓ If not, should we implement Excel manipulation?
   - Decision: Depends on user requirements

4. ❓ Are there other tools with similar file path issues?
   - Need to audit: upload_file, get_file_history, etc.
