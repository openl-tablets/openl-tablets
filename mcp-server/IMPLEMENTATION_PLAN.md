# OpenL Tablets MCP Server - Implementation Plan

## Overview

This plan implements the corrected understanding of OpenL Tablets with focus on:
- **Two independent versioning systems** (Git commits + Dimension properties)
- **Multiple table types** (Decision Tables, Spreadsheet, and others)
- **High-performance MCP server** through clear naming, descriptions, and prompts

---

## Implementation Phases

### Phase 1: Remove Incorrect Tools and Fix Critical Errors
**Goal**: Remove hallucinated functionality, fix version-related tools

**Duration**: 1-2 hours

#### 1.1 Remove `version_file` Tool

**Files to modify**:
- `src/tools.ts` - Remove tool definition
- `src/index.ts` - Remove handler
- `src/client.ts` - Remove `versionFile()` method
- `src/types.ts` - Remove `VersionFileRequest` and `VersionFileResult` interfaces
- `src/schemas.ts` - Remove `versionFileSchema`
- `prompts/version_file.md` - DELETE file

**Verification**:
```bash
npm run build
npm run lint
npm test
```

#### 1.2 Update `save_project` to Return Commit Hash

**File**: `src/client.ts`

**Current**:
```typescript
async saveProject(request: { projectId: string; comment?: string }): Promise<SaveProjectResult> {
  // ...
  return { success: true, message: "Project saved successfully" };
}
```

**Updated**:
```typescript
async saveProject(request: { projectId: string; comment?: string }): Promise<SaveProjectResult> {
  const [repository, projectName] = this.parseProjectId(request.projectId);

  const response = await this.axiosInstance.post(
    `/design-repositories/${repository}/projects/${projectName}/save`,
    { comment: request.comment }
  );

  const fileData: FileData = response.data;

  return {
    success: true,
    commitHash: fileData.version,           // Git commit hash
    version: fileData.version,              // Same as commitHash
    author: {
      name: fileData.author?.name || "unknown",
      email: fileData.author?.email || ""
    },
    timestamp: fileData.modifiedAt,
    message: `Project saved at commit ${fileData.version.substring(0, 8)}`
  };
}
```

**File**: `src/types.ts`

**Update interface**:
```typescript
export interface SaveProjectResult {
  success: boolean;
  commitHash: string;        // Git commit hash (e.g., "7a3f2b1c...")
  version: string;           // Same as commitHash
  author: {
    name: string;
    email: string;
  };
  timestamp: string;         // ISO timestamp
  message?: string;
  validationErrors?: ValidationError[];
}
```

#### 1.3 Update `upload_file` to Return Commit Hash

**File**: `src/client.ts`

**Update**:
```typescript
async uploadFile(request: UploadFileRequest): Promise<FileUploadResult> {
  const [repository, projectName] = this.parseProjectId(request.projectId);
  const buffer = Buffer.isBuffer(request.fileContent)
    ? request.fileContent
    : Buffer.from(request.fileContent, "base64");

  const response = await this.axiosInstance.post(
    `/design-repositories/${repository}/projects/${projectName}/files/${request.fileName}`,
    buffer,
    {
      params: { comment: request.comment },
      headers: {
        "Content-Type": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
      }
    }
  );

  const fileData: FileData = response.data;

  return {
    success: true,
    fileName: request.fileName,
    commitHash: fileData.version,
    version: fileData.version,
    author: {
      name: fileData.author?.name || "unknown",
      email: fileData.author?.email || ""
    },
    timestamp: fileData.modifiedAt,
    size: fileData.size,
    message: `File uploaded at commit ${fileData.version.substring(0, 8)}`
  };
}
```

**File**: `src/types.ts`

**Update interface**:
```typescript
export interface FileUploadResult {
  success: boolean;
  fileName: string;
  commitHash: string;        // Git commit hash
  version: string;           // Same as commitHash
  author: {
    name: string;
    email: string;
  };
  timestamp: string;
  size?: number;
  message?: string;
}
```

#### 1.4 Update `download_file` to Support Version Parameter

**File**: `src/client.ts`

**Update**:
```typescript
async downloadFile(request: DownloadFileRequest): Promise<DownloadFileResult> {
  const [repository, projectName] = this.parseProjectId(request.projectId);

  const params: any = {};
  if (request.version) {
    params.version = request.version;  // Git commit hash
  }

  const response = await this.axiosInstance.get(
    `/design-repositories/${repository}/projects/${projectName}/files/${request.fileName}`,
    {
      params,
      responseType: "arraybuffer"
    }
  );

  // Extract metadata from headers
  const metadata: FileData = {
    name: request.fileName,
    version: response.headers["x-file-version"] || request.version || "HEAD",
    author: {
      name: response.headers["x-file-author-name"] || "",
      email: response.headers["x-file-author-email"] || ""
    },
    modifiedAt: response.headers["x-file-modified-at"] || new Date().toISOString(),
    size: parseInt(response.headers["content-length"], 10)
  };

  return {
    content: Buffer.from(response.data).toString("base64"),
    metadata
  };
}
```

**File**: `src/types.ts`

**Update interfaces**:
```typescript
export interface DownloadFileRequest {
  projectId: string;
  fileName: string;
  version?: string;  // Optional Git commit hash to download specific version
}

export interface DownloadFileResult {
  content: string;   // Base64-encoded file content
  metadata: FileData;
}
```

**File**: `src/schemas.ts`

**Update schema**:
```typescript
export const downloadFileSchema = z.object({
  projectId: projectIdSchema,
  fileName: z.string().min(1).describe("File name (e.g., 'rules/Insurance.xlsx')"),
  version: z.string().optional().describe("Git commit hash to download specific version (e.g., '7a3f2b1c...'). Omit for latest version.")
});
```

**File**: `src/tools.ts`

**Update tool description**:
```typescript
{
  name: "download_file",
  description: "Download a file from OpenL project. Can download latest version (HEAD) or specific historical version using Git commit hash.",
  inputSchema: zodToJsonSchema(downloadFileSchema)
}
```

#### 1.5 Update `compare_versions` Schema

**File**: `src/schemas.ts`

**Current**:
```typescript
export const compareVersionsSchema = z.object({
  projectId: projectIdSchema,
  version1: z.string().describe("First version to compare"),
  version2: z.string().describe("Second version to compare"),
});
```

**Updated**:
```typescript
export const compareVersionsSchema = z.object({
  projectId: projectIdSchema,
  baseCommitHash: z.string().describe("Base Git commit hash to compare from (e.g., '7a3f2b1c...')"),
  targetCommitHash: z.string().describe("Target Git commit hash to compare to (e.g., '9e5d8a2f...')"),
});
```

**File**: `src/client.ts`

**Update method**:
```typescript
async compareVersions(request: CompareVersionsRequest): Promise<CompareVersionsResult> {
  const [repository, projectName] = this.parseProjectId(request.projectId);

  const response = await this.axiosInstance.get(
    `/design-repositories/${repository}/projects/${projectName}/compare`,
    {
      params: {
        base: request.baseCommitHash,
        target: request.targetCommitHash
      }
    }
  );

  return response.data;
}
```

**File**: `src/types.ts`

**Update interfaces**:
```typescript
export interface CompareVersionsRequest {
  projectId: string;
  baseCommitHash: string;    // Git commit hash
  targetCommitHash: string;  // Git commit hash
}

export interface CompareVersionsResult {
  baseCommit: string;
  targetCommit: string;

  tables: {
    added: Array<{ name: string; type: string; file: string }>;
    modified: Array<{ name: string; type: string; file: string; changes: string[] }>;
    removed: Array<{ name: string; type: string; file: string }>;
  };

  files: {
    added: string[];
    modified: string[];
    removed: string[];
  };

  summary: {
    totalChanges: number;
    tablesAdded: number;
    tablesModified: number;
    tablesRemoved: number;
    filesAdded: number;
    filesModified: number;
    filesRemoved: number;
  };

  baseAuthor: { name: string; email: string };
  targetAuthor: { name: string; email: string };
  baseTimestamp: string;
  targetTimestamp: string;
  baseComment: string;
  targetComment: string;
}
```

**File**: `src/tools.ts`

**Update description**:
```typescript
{
  name: "compare_versions",
  description: "Compare two Git commit versions of a project to see what changed between them. Returns added, modified, and removed tables and files.",
  inputSchema: zodToJsonSchema(compareVersionsSchema)
}
```

#### 1.6 Update `revert_version` Schema

**File**: `src/schemas.ts`

**Current**:
```typescript
export const revertVersionSchema = z.object({
  projectId: projectIdSchema,
  targetVersion: z.string(),
  comment: commentSchema,
});
```

**Updated**:
```typescript
export const revertVersionSchema = z.object({
  projectId: projectIdSchema,
  commitHash: z.string().describe("Git commit hash to revert to (e.g., '7a3f2b1c...')"),
  comment: commentSchema.describe("Comment for the revert operation (will create a new commit)"),
});
```

**File**: `src/client.ts`

**Update method**:
```typescript
async revertVersion(request: RevertVersionRequest): Promise<RevertVersionResult> {
  const [repository, projectName] = this.parseProjectId(request.projectId);

  const response = await this.axiosInstance.post(
    `/design-repositories/${repository}/projects/${projectName}/revert`,
    {
      commitHash: request.commitHash,
      comment: request.comment
    }
  );

  const fileData: FileData = response.data;

  return {
    success: true,
    newCommitHash: fileData.version,
    message: `Reverted to ${request.commitHash.substring(0, 8)}, created new commit ${fileData.version.substring(0, 8)}`
  };
}
```

**File**: `src/tools.ts`

**Update description**:
```typescript
{
  name: "revert_version",
  description: "Revert project to a previous Git commit. Creates a new commit that restores the project state from the specified commit hash.",
  inputSchema: zodToJsonSchema(revertVersionSchema)
}
```

---

### Phase 2: Add New Tools for Git Version History
**Goal**: Add missing tools for Git-based versioning

**Duration**: 2-3 hours

#### 2.1 Add `get_file_history` Tool

**Purpose**: List all Git commits that modified a specific file

**File**: `src/types.ts`
```typescript
export interface GetFileHistoryRequest {
  projectId: string;
  filePath: string;
  limit?: number;        // Max commits to return (default: 50)
  offset?: number;       // Skip first N commits (for pagination)
}

export interface GetFileHistoryResult {
  filePath: string;
  commits: Array<{
    commitHash: string;
    author: { name: string; email: string };
    timestamp: string;
    comment: string;
    commitType: "SAVE" | "ARCHIVE" | "RESTORE" | "ERASE" | "MERGE";
    size?: number;
  }>;
  total: number;         // Total commits available
  hasMore: boolean;      // More commits available
}
```

**File**: `src/schemas.ts`
```typescript
export const getFileHistorySchema = z.object({
  projectId: projectIdSchema,
  filePath: z.string().min(1).describe("File path within project (e.g., 'rules/Insurance.xlsx')"),
  limit: z.number().int().positive().max(200).optional().describe("Maximum number of commits to return (default: 50, max: 200)"),
  offset: z.number().int().nonnegative().optional().describe("Number of commits to skip for pagination (default: 0)")
});
```

**File**: `src/client.ts`
```typescript
async getFileHistory(request: GetFileHistoryRequest): Promise<GetFileHistoryResult> {
  const [repository, projectName] = this.parseProjectId(request.projectId);

  const response = await this.axiosInstance.get(
    `/design-repositories/${repository}/projects/${projectName}/files/${request.filePath}/history`,
    {
      params: {
        limit: request.limit || 50,
        offset: request.offset || 0
      }
    }
  );

  const commits = response.data.commits.map((fileData: FileData) => ({
    commitHash: fileData.version,
    author: fileData.author,
    timestamp: fileData.modifiedAt,
    comment: fileData.comment || "",
    commitType: this.parseCommitType(fileData.comment),
    size: fileData.size
  }));

  return {
    filePath: request.filePath,
    commits,
    total: response.data.total || commits.length,
    hasMore: (request.offset || 0) + commits.length < (response.data.total || commits.length)
  };
}

private parseCommitType(comment?: string): "SAVE" | "ARCHIVE" | "RESTORE" | "ERASE" | "MERGE" {
  if (!comment) return "SAVE";
  if (comment.includes("Type: ARCHIVE")) return "ARCHIVE";
  if (comment.includes("Type: RESTORE")) return "RESTORE";
  if (comment.includes("Type: ERASE")) return "ERASE";
  if (comment.includes("Type: MERGE")) return "MERGE";
  return "SAVE";
}
```

**File**: `src/tools.ts`
```typescript
{
  name: "get_file_history",
  description: "Get Git commit history for a specific file. Returns list of commits with hashes, authors, timestamps, and commit types. Use this to see all versions of a file over time.",
  inputSchema: zodToJsonSchema(getFileHistorySchema)
}
```

**File**: `src/index.ts` (handler)
```typescript
case "get_file_history": {
  const request = getFileHistorySchema.parse(params);
  const result = await client.getFileHistory(request);
  return {
    content: [{
      type: "text",
      text: `File history for ${result.filePath}:\n\n` +
            `Total commits: ${result.total}\n` +
            `Showing: ${result.commits.length}\n\n` +
            result.commits.map((c, i) =>
              `${i + 1}. Commit ${c.commitHash.substring(0, 8)}\n` +
              `   Author: ${c.author.name} <${c.author.email}>\n` +
              `   Date: ${c.timestamp}\n` +
              `   Type: ${c.commitType}\n` +
              `   Comment: ${c.comment}\n`
            ).join("\n") +
            (result.hasMore ? `\n\nMore commits available. Use offset=${(request.offset || 0) + result.commits.length} to see next page.` : "")
    }]
  };
}
```

**Create prompt**: `prompts/file_history.md`
```markdown
# Working with File History in OpenL Tablets

OpenL Tablets uses **Git for version control**. Every save/upload creates a Git commit automatically.

## Version Identifier = Git Commit Hash

Versions are identified by Git commit hashes (e.g., "7a3f2b1c3e..."), NOT sequential numbers like v1, v2, v3.

## Viewing File History

The `get_file_history` tool shows all commits that modified a specific file:

```
File: rules/Insurance.xlsx

1. Commit 7a3f2b1c
   Author: John Doe <john@company.com>
   Date: 2025-01-15T10:30:00Z
   Type: SAVE
   Comment: Updated premium calculation rules

2. Commit 9e5d8a2f
   Author: Jane Smith <jane@company.com>
   Date: 2025-01-10T14:20:00Z
   Type: SAVE
   Comment: Added state-specific rules for CA
```

## Commit Types

- **SAVE**: Normal save operation
- **ARCHIVE**: File archived (soft delete)
- **RESTORE**: File restored from archive
- **ERASE**: File permanently deleted
- **MERGE**: Git branch merge

## Using Commit Hashes

Once you have the commit hash, you can:

1. **Download specific version**: `download_file` with `version` parameter
2. **Compare versions**: `compare_versions` between two commit hashes
3. **Revert to previous version**: `revert_version` to restore old state

## Pagination

For files with many commits, use `limit` and `offset`:
- `limit: 50` - Get 50 commits at a time
- `offset: 0` - Start from beginning
- `offset: 50` - Skip first 50, get next 50

## Important Notes

- File names **never change** between versions
- Same file path exists across all commits with different content
- Each commit is a snapshot of the entire project state
- Reverting creates a NEW commit (doesn't delete history)
```

#### 2.2 Add `get_project_history` Tool

**Purpose**: List all commits for entire project (not just one file)

**File**: `src/types.ts`
```typescript
export interface GetProjectHistoryRequest {
  projectId: string;
  limit?: number;
  offset?: number;
  branch?: string;   // Optional: specific branch (default: current branch)
}

export interface GetProjectHistoryResult {
  projectId: string;
  branch: string;
  commits: Array<{
    commitHash: string;
    author: { name: string; email: string };
    timestamp: string;
    comment: string;
    commitType: "SAVE" | "ARCHIVE" | "RESTORE" | "ERASE" | "MERGE";
    filesChanged: number;
    tablesChanged?: number;
  }>;
  total: number;
  hasMore: boolean;
}
```

**File**: `src/schemas.ts`
```typescript
export const getProjectHistorySchema = z.object({
  projectId: projectIdSchema,
  limit: z.number().int().positive().max(200).optional().describe("Maximum commits to return (default: 50, max: 200)"),
  offset: z.number().int().nonnegative().optional().describe("Commits to skip for pagination (default: 0)"),
  branch: z.string().optional().describe("Git branch name (default: current branch)")
});
```

**File**: `src/client.ts`
```typescript
async getProjectHistory(request: GetProjectHistoryRequest): Promise<GetProjectHistoryResult> {
  const [repository, projectName] = this.parseProjectId(request.projectId);

  const response = await this.axiosInstance.get(
    `/design-repositories/${repository}/projects/${projectName}/history`,
    {
      params: {
        limit: request.limit || 50,
        offset: request.offset || 0,
        branch: request.branch
      }
    }
  );

  return {
    projectId: request.projectId,
    branch: response.data.branch || "main",
    commits: response.data.commits.map((commit: any) => ({
      commitHash: commit.version,
      author: commit.author,
      timestamp: commit.modifiedAt,
      comment: commit.comment || "",
      commitType: this.parseCommitType(commit.comment),
      filesChanged: commit.filesChanged || 0,
      tablesChanged: commit.tablesChanged
    })),
    total: response.data.total || response.data.commits.length,
    hasMore: (request.offset || 0) + response.data.commits.length < (response.data.total || response.data.commits.length)
  };
}
```

**File**: `src/tools.ts`
```typescript
{
  name: "get_project_history",
  description: "Get Git commit history for entire project. Shows all commits across all files with summary of changes. Useful for understanding project evolution over time.",
  inputSchema: zodToJsonSchema(getProjectHistorySchema)
}
```

---

### Phase 3: Add Dimension Properties Support
**Goal**: Support OpenL Core versioning through dimension properties

**Duration**: 3-4 hours

#### 3.1 Add `get_file_name_pattern` Tool

**Purpose**: Get dimension properties file naming pattern from rules.xml

**File**: `src/types.ts`
```typescript
export interface GetFileNamePatternRequest {
  projectId: string;
}

export interface GetFileNamePatternResult {
  pattern: string | null;  // e.g., ".*-%state%-%effectiveDate:MMddyyyy%-%lob%"
  properties: string[];     // Extracted property names: ["state", "effectiveDate", "lob"]
}
```

**File**: `src/schemas.ts`
```typescript
export const getFileNamePatternSchema = z.object({
  projectId: projectIdSchema
});
```

**File**: `src/client.ts`
```typescript
async getFileNamePattern(request: GetFileNamePatternRequest): Promise<GetFileNamePatternResult> {
  const [repository, projectName] = this.parseProjectId(request.projectId);

  const response = await this.axiosInstance.get(
    `/design-repositories/${repository}/projects/${projectName}/rules.xml`
  );

  // Parse XML to extract properties-file-name-pattern
  const pattern = this.extractFileNamePattern(response.data);
  const properties = this.extractPropertiesFromPattern(pattern);

  return {
    pattern,
    properties
  };
}

private extractFileNamePattern(xmlContent: string): string | null {
  const match = xmlContent.match(/<properties-file-name-pattern>(.*?)<\/properties-file-name-pattern>/);
  return match ? match[1] : null;
}

private extractPropertiesFromPattern(pattern: string | null): string[] {
  if (!pattern) return [];
  const matches = pattern.match(/%([^:%]+)(?::[^%]+)?%/g);
  if (!matches) return [];
  return matches.map(m => m.replace(/%([^:%]+)(?::[^%]+)?%/, "$1"));
}
```

**File**: `src/tools.ts`
```typescript
{
  name: "get_file_name_pattern",
  description: "Get dimension properties file naming pattern from rules.xml. Returns pattern and list of properties used in file names (state, lob, effectiveDate, etc.).",
  inputSchema: zodToJsonSchema(getFileNamePatternSchema)
}
```

**Create prompt**: `prompts/dimension_properties.md`
```markdown
# Dimension Properties in OpenL Tablets

OpenL Tablets has a **second versioning system** (independent from Git) called **Dimension Properties**.

## Purpose

Allow multiple versions of the SAME rule (same signature) to coexist, differentiated by business context:
- **Geographic**: state, country, region
- **Business**: lob (Line of Business), currency
- **Temporal**: effectiveDate, expirationDate, startRequestDate, endRequestDate
- **Scenario**: version, active flag

## File Name Patterns

Properties can be encoded in file names using patterns in `rules.xml`:

```xml
<properties-file-name-pattern>.*-%state%-%effectiveDate:MMddyyyy%-%lob%</properties-file-name-pattern>
```

**Examples**:
- `Insurance-CA-01012025-Auto.xlsx` (California, effective Jan 1 2025, Auto insurance)
- `Insurance-TX-06152025-Home.xlsx` (Texas, effective Jun 15 2025, Home insurance)
- `Insurance-CW-01012025-Life.xlsx` (Country-wide, effective Jan 1 2025, Life insurance)

**Special Values**:
- `CW` = Country-Wide (applies to all US states)
- `Any` = Matches any value

## Table-Level Properties

In Excel, properties appear after table header:

```
Rules int calculatePremium(String state, int age)
properties
effectiveDate    01/01/2025
expirationDate   12/31/2025
state            CA
lob              Auto
```

## Runtime Selection

When a rule is called, OpenL:
1. Filters rules matching input context (date, state, lob, etc.)
2. Selects most specific match (table > category > module properties)
3. Returns result from selected rule version

## Two Versioning Systems Working Together

**Git Commits** track file changes over time:
- Commit a1b2c3d (Jan 1) → Commit e4f5g6h (Feb 1) → Commit i7j8k9l (Mar 1)

**Within each commit**, dimension properties create multiple rule versions:
- `calculatePremium` for CA with effective date 01/01/2025
- `calculatePremium` for CA with effective date 06/01/2025
- `calculatePremium` for TX with effective date 01/01/2025

## Available Properties

| Property | Description | Example Values |
|----------|-------------|----------------|
| effectiveDate | When rule becomes legally active | 2025-01-01 |
| expirationDate | When rule expires | 2025-12-31 |
| startRequestDate | When rule is operationally introduced | 2025-01-15 |
| endRequestDate | When rule is operationally retired | 2025-12-15 |
| state | US State | CA, NY, TX, CW |
| lob | Line of Business | Auto, Home, Life |
| country | Country code | US, CA, UK |
| currency | Currency code | USD, EUR, GBP |
| region | US Region | West, East |
| language | Language code | en, es, fr |
| caProvince | Canadian Province | ON, BC, QC |
| origin | Base vs Deviation | Base, Deviation |
| nature | User-defined | Custom values |
| version | Scenario version | v1, v2, draft |
| active | Active flag | true, false |

## When to Use

Use dimension properties when:
- Rules vary by state, region, or country
- Rules change over time (effective/expiration dates)
- Need to maintain multiple business scenarios
- Same rule has different implementations for different contexts

Use Git commits when:
- Tracking file changes over time
- Need audit trail of who changed what
- Comparing historical versions
- Reverting to previous state
```

#### 3.2 Add `set_file_name_pattern` Tool

**File**: `src/types.ts`
```typescript
export interface SetFileNamePatternRequest {
  projectId: string;
  pattern: string;  // e.g., ".*-%state%-%lob%"
}

export interface SetFileNamePatternResult {
  success: boolean;
  pattern: string;
  message: string;
}
```

**File**: `src/schemas.ts`
```typescript
export const setFileNamePatternSchema = z.object({
  projectId: projectIdSchema,
  pattern: z.string().describe("File name pattern with property placeholders (e.g., '.*-%state%-%effectiveDate:MMddyyyy%-%lob%')")
});
```

**File**: `src/client.ts`
```typescript
async setFileNamePattern(request: SetFileNamePatternRequest): Promise<SetFileNamePatternResult> {
  const [repository, projectName] = this.parseProjectId(request.projectId);

  await this.axiosInstance.put(
    `/design-repositories/${repository}/projects/${projectName}/rules.xml/pattern`,
    { pattern: request.pattern }
  );

  return {
    success: true,
    pattern: request.pattern,
    message: "File name pattern updated successfully"
  };
}
```

**File**: `src/tools.ts`
```typescript
{
  name: "set_file_name_pattern",
  description: "Set dimension properties file naming pattern in rules.xml. Pattern determines how properties are encoded in file names (e.g., '%state%-%lob%' creates 'CA-Auto.xlsx').",
  inputSchema: zodToJsonSchema(setFileNamePatternSchema)
}
```

#### 3.3 Add `get_table_properties` Tool

**File**: `src/types.ts`
```typescript
export interface GetTablePropertiesRequest {
  projectId: string;
  tableId: string;
}

export interface GetTablePropertiesResult {
  tableId: string;
  tableName: string;
  properties: Record<string, any>;  // e.g., { state: "CA", lob: "Auto", effectiveDate: "01/01/2025" }
}
```

**File**: `src/schemas.ts`
```typescript
export const getTablePropertiesSchema = z.object({
  projectId: projectIdSchema,
  tableId: z.string().describe("Table identifier")
});
```

**File**: `src/client.ts`
```typescript
async getTableProperties(request: GetTablePropertiesRequest): Promise<GetTablePropertiesResult> {
  const [repository, projectName] = this.parseProjectId(request.projectId);

  const response = await this.axiosInstance.get(
    `/design-repositories/${repository}/projects/${projectName}/tables/${request.tableId}/properties`
  );

  return {
    tableId: request.tableId,
    tableName: response.data.name,
    properties: response.data.properties || {}
  };
}
```

**File**: `src/tools.ts`
```typescript
{
  name: "get_table_properties",
  description: "Get dimension properties for a specific table. Returns properties like state, lob, effectiveDate that determine when and where this rule version applies.",
  inputSchema: zodToJsonSchema(getTablePropertiesSchema)
}
```

#### 3.4 Add `set_table_properties` Tool

**File**: `src/types.ts`
```typescript
export interface SetTablePropertiesRequest {
  projectId: string;
  tableId: string;
  properties: Record<string, any>;  // e.g., { state: "CA", lob: "Auto", effectiveDate: "01/01/2025" }
  comment?: string;
}

export interface SetTablePropertiesResult {
  success: boolean;
  tableId: string;
  properties: Record<string, any>;
  message: string;
}
```

**File**: `src/schemas.ts`
```typescript
export const setTablePropertiesSchema = z.object({
  projectId: projectIdSchema,
  tableId: z.string().describe("Table identifier"),
  properties: z.record(z.any()).describe("Dimension properties object (e.g., { state: 'CA', lob: 'Auto', effectiveDate: '01/01/2025' })"),
  comment: commentSchema.optional()
});
```

**File**: `src/client.ts`
```typescript
async setTableProperties(request: SetTablePropertiesRequest): Promise<SetTablePropertiesResult> {
  const [repository, projectName] = this.parseProjectId(request.projectId);

  await this.axiosInstance.put(
    `/design-repositories/${repository}/projects/${projectName}/tables/${request.tableId}/properties`,
    {
      properties: request.properties,
      comment: request.comment
    }
  );

  return {
    success: true,
    tableId: request.tableId,
    properties: request.properties,
    message: "Table properties updated successfully"
  };
}
```

**File**: `src/tools.ts`
```typescript
{
  name: "set_table_properties",
  description: "Set dimension properties for a table. Properties determine when/where rule applies (state, lob, effectiveDate, etc.). Allows multiple versions of same rule to coexist.",
  inputSchema: zodToJsonSchema(setTablePropertiesSchema)
}
```

---

### Phase 4: Update `create_rule` for Multiple Table Types
**Goal**: Support creating Decision Tables (5 types) and Spreadsheet tables

**Duration**: 4-5 hours

#### 4.1 Update Type Definitions

**File**: `src/types.ts`

**Add table type enum**:
```typescript
export type TableType =
  // Decision Tables (most common)
  | "Rules"
  | "SimpleRules"
  | "SmartRules"
  | "SimpleLookup"
  | "SmartLookup"
  // Spreadsheet (most common)
  | "Spreadsheet"
  // Other types (rarely used)
  | "Method"
  | "TBasic"
  | "Data"
  | "Datatype"
  | "Test"
  | "Run"
  | "Properties"
  | "Configuration";

export interface CreateRuleRequest {
  projectId: string;
  tableName: string;
  tableType: TableType;
  returnType: string;
  parameters: Array<{ type: string; name: string }>;

  // For Decision Tables (Rules, SimpleRules, SmartRules)
  conditions?: Array<{
    expression: string;      // e.g., "driverType == ?"
    paramType?: string;      // e.g., "String" (for Rules table)
    paramName?: string;      // e.g., "driverType" (for Rules table)
    title?: string;          // e.g., "Driver Type"
  }>;

  // For Lookup Tables (SimpleLookup, SmartLookup)
  verticalConditions?: Array<{
    expression: string;
    paramType?: string;
    paramName?: string;
    title?: string;
  }>;
  horizontalConditions?: Array<{
    expression: string;
    paramType?: string;
    paramName?: string;
    title?: string;
  }>;

  // For Rules table only (Actions)
  actions?: Array<{
    expression: string;
    paramType: string;
    paramName: string;
    title: string;
  }>;

  // Return column (all decision tables)
  returnColumn?: {
    expression: string;
    paramType?: string;
    paramName?: string;
    title?: string;
  };

  // For Spreadsheet tables
  spreadsheetStructure?: {
    rows: Array<{ name: string }>;          // Step names
    columns: Array<{ name: string }>;       // Column names
    cells?: Record<string, string>;         // cell formulas: { "Step1_A": "baseAmount", "Step2_Premium": "$A * 1.5" }
  };

  // For Data tables
  dataStructure?: {
    dataType: string;                       // e.g., "Person"
    rows: Array<Record<string, any>>;       // Data rows
  };

  // For Datatype tables
  datatypeStructure?: {
    extends?: string;                       // Parent type
    attributes: Array<{
      type: string;
      name: string;
      defaultValue?: any;
    }>;
  };

  // Dimension properties (optional, all table types)
  properties?: {
    effectiveDate?: string;
    expirationDate?: string;
    startRequestDate?: string;
    endRequestDate?: string;
    state?: string;
    lob?: string;
    country?: string;
    currency?: string;
    region?: string;
    language?: string;
    caProvince?: string;
    origin?: "Base" | "Deviation";
    nature?: string;
    version?: string;
    active?: boolean;
    [key: string]: any;
  };

  file?: string;           // Target file (optional, uses default if not specified)
  comment?: string;        // Commit comment
}
```

#### 4.2 Update Schema

**File**: `src/schemas.ts`
```typescript
export const createRuleSchema = z.object({
  projectId: projectIdSchema,
  tableName: z.string().min(1).describe("Table name (must be valid Java identifier)"),
  tableType: z.enum([
    "Rules", "SimpleRules", "SmartRules", "SimpleLookup", "SmartLookup",
    "Spreadsheet", "Method", "TBasic", "Data", "Datatype", "Test", "Run", "Properties", "Configuration"
  ]).describe("Type of table to create. Most common: Rules/SimpleRules/SmartRules/SimpleLookup/SmartLookup (decision tables) or Spreadsheet (calculations)"),

  returnType: z.string().describe("Return type (e.g., 'int', 'String', 'SpreadsheetResult', or custom type)"),

  parameters: z.array(z.object({
    type: z.string().describe("Parameter type (e.g., 'String', 'int', 'Policy')"),
    name: z.string().describe("Parameter name (e.g., 'driverType', 'age')")
  })).describe("Method parameters"),

  conditions: z.array(z.object({
    expression: z.string(),
    paramType: z.string().optional(),
    paramName: z.string().optional(),
    title: z.string().optional()
  })).optional().describe("Condition columns (for decision tables)"),

  verticalConditions: z.array(z.object({
    expression: z.string(),
    paramType: z.string().optional(),
    paramName: z.string().optional(),
    title: z.string().optional()
  })).optional().describe("Vertical conditions (for lookup tables)"),

  horizontalConditions: z.array(z.object({
    expression: z.string(),
    paramType: z.string().optional(),
    paramName: z.string().optional(),
    title: z.string().optional()
  })).optional().describe("Horizontal conditions (for lookup tables)"),

  actions: z.array(z.object({
    expression: z.string(),
    paramType: z.string(),
    paramName: z.string(),
    title: z.string()
  })).optional().describe("Action columns (for Rules table only)"),

  returnColumn: z.object({
    expression: z.string(),
    paramType: z.string().optional(),
    paramName: z.string().optional(),
    title: z.string().optional()
  }).optional().describe("Return column definition"),

  spreadsheetStructure: z.object({
    rows: z.array(z.object({ name: z.string() })),
    columns: z.array(z.object({ name: z.string() })),
    cells: z.record(z.string()).optional()
  }).optional().describe("Spreadsheet structure (for Spreadsheet table)"),

  dataStructure: z.object({
    dataType: z.string(),
    rows: z.array(z.record(z.any()))
  }).optional().describe("Data structure (for Data table)"),

  datatypeStructure: z.object({
    extends: z.string().optional(),
    attributes: z.array(z.object({
      type: z.string(),
      name: z.string(),
      defaultValue: z.any().optional()
    }))
  }).optional().describe("Datatype structure (for Datatype table)"),

  properties: z.record(z.any()).optional().describe("Dimension properties (state, lob, effectiveDate, etc.)"),

  file: z.string().optional().describe("Target Excel file (e.g., 'rules/Insurance.xlsx'). If not specified, uses default."),
  comment: commentSchema.optional()
});
```

#### 4.3 Update Client Method

**File**: `src/client.ts`
```typescript
async createRule(request: CreateRuleRequest): Promise<CreateRuleResult> {
  const [repository, projectName] = this.parseProjectId(request.projectId);

  // Build table definition based on type
  const tableDefinition = this.buildTableDefinition(request);

  const response = await this.axiosInstance.post(
    `/design-repositories/${repository}/projects/${projectName}/tables`,
    {
      name: request.tableName,
      type: request.tableType,
      definition: tableDefinition,
      properties: request.properties,
      file: request.file,
      comment: request.comment
    }
  );

  return {
    success: true,
    tableId: response.data.id,
    tableName: request.tableName,
    tableType: request.tableType,
    file: response.data.file,
    message: `Created ${request.tableType} table: ${request.tableName}`
  };
}

private buildTableDefinition(request: CreateRuleRequest): any {
  switch (request.tableType) {
    case "Rules":
      return this.buildRulesTableDefinition(request);
    case "SimpleRules":
      return this.buildSimpleRulesTableDefinition(request);
    case "SmartRules":
      return this.buildSmartRulesTableDefinition(request);
    case "SimpleLookup":
      return this.buildSimpleLookupTableDefinition(request);
    case "SmartLookup":
      return this.buildSmartLookupTableDefinition(request);
    case "Spreadsheet":
      return this.buildSpreadsheetTableDefinition(request);
    case "Data":
      return this.buildDataTableDefinition(request);
    case "Datatype":
      return this.buildDatatypeTableDefinition(request);
    default:
      throw new Error(`Unsupported table type: ${request.tableType}`);
  }
}

private buildRulesTableDefinition(request: CreateRuleRequest): any {
  // Rules table with explicit C1, A1, RET1 markers
  const signature = this.buildSignature(request.returnType, request.tableName, request.parameters);

  return {
    signature,
    columns: [
      // Condition columns
      ...(request.conditions || []).map((c, i) => ({
        type: "C",
        index: i + 1,
        expression: c.expression,
        parameterType: c.paramType,
        parameterName: c.paramName,
        title: c.title
      })),
      // Action columns
      ...(request.actions || []).map((a, i) => ({
        type: "A",
        index: i + 1,
        expression: a.expression,
        parameterType: a.paramType,
        parameterName: a.paramName,
        title: a.title
      })),
      // Return column
      {
        type: "RET",
        index: 1,
        expression: request.returnColumn?.expression || request.tableName,
        parameterType: request.returnColumn?.paramType || request.returnType,
        parameterName: request.returnColumn?.paramName || "result",
        title: request.returnColumn?.title || "Result"
      }
    ],
    rows: []  // Initially empty
  };
}

private buildSimpleRulesTableDefinition(request: CreateRuleRequest): any {
  // SimpleRules - positional matching, no column markers
  const signature = this.buildSignature(request.returnType, request.tableName, request.parameters);

  return {
    signature,
    conditions: request.conditions || [],
    returnColumn: request.returnColumn,
    rows: []
  };
}

private buildSmartRulesTableDefinition(request: CreateRuleRequest): any {
  // SmartRules - name-based matching
  const signature = this.buildSignature(request.returnType, request.tableName, request.parameters);

  return {
    signature,
    conditions: request.conditions || [],
    returnColumn: request.returnColumn,
    rows: []
  };
}

private buildSimpleLookupTableDefinition(request: CreateRuleRequest): any {
  // SimpleLookup - 2D table with horizontal/vertical conditions
  const signature = this.buildSignature(request.returnType, request.tableName, request.parameters);

  return {
    signature,
    verticalConditions: request.verticalConditions || [],
    horizontalConditions: request.horizontalConditions || [],
    returnColumn: request.returnColumn,
    rows: []
  };
}

private buildSmartLookupTableDefinition(request: CreateRuleRequest): any {
  // SmartLookup - 2D with smart matching
  const signature = this.buildSignature(request.returnType, request.tableName, request.parameters);

  return {
    signature,
    verticalConditions: request.verticalConditions || [],
    horizontalConditions: request.horizontalConditions || [],
    returnColumn: request.returnColumn,
    rows: []
  };
}

private buildSpreadsheetTableDefinition(request: CreateRuleRequest): any {
  // Spreadsheet table
  const signature = this.buildSignature(request.returnType, request.tableName, request.parameters);

  return {
    signature,
    rows: request.spreadsheetStructure?.rows || [],
    columns: request.spreadsheetStructure?.columns || [],
    cells: request.spreadsheetStructure?.cells || {}
  };
}

private buildDataTableDefinition(request: CreateRuleRequest): any {
  // Data table
  return {
    dataType: request.dataStructure?.dataType || "Object",
    tableName: request.tableName,
    rows: request.dataStructure?.rows || []
  };
}

private buildDatatypeTableDefinition(request: CreateRuleRequest): any {
  // Datatype table
  return {
    typeName: request.tableName,
    extends: request.datatypeStructure?.extends,
    attributes: request.datatypeStructure?.attributes || []
  };
}

private buildSignature(returnType: string, name: string, parameters: Array<{ type: string; name: string }>): string {
  const params = parameters.map(p => `${p.type} ${p.name}`).join(", ");
  return `${returnType} ${name}(${params})`;
}
```

#### 4.4 Update Tool Description

**File**: `src/tools.ts`
```typescript
{
  name: "create_rule",
  description: `Create a new table in OpenL project. Supports multiple table types:

MOST COMMON:
- Decision Tables: Rules, SimpleRules, SmartRules (conditional logic)
- Lookup Tables: SimpleLookup, SmartLookup (2D lookups)
- Spreadsheet: Multi-step calculations with formulas

RARELY USED:
- Method, TBasic, Data, Datatype, Test, Run, Properties, Configuration

Decision tables use conditions to select outcomes. Spreadsheets use formulas for calculations.

Can optionally add dimension properties (state, lob, effectiveDate) to create multiple versions of same rule.`,
  inputSchema: zodToJsonSchema(createRuleSchema)
}
```

#### 4.5 Update `create_rule.md` Prompt

**File**: `prompts/create_rule.md`
```markdown
# Creating Tables in OpenL Tablets

You are helping the user create a table in OpenL Tablets.

## Most Common Table Types

### A. DECISION TABLES (For Conditional Logic)

Choose outcome based on conditions. OpenL supports 5 decision table types:

#### 1. Rules Table (Standard)
- **Use when**: Complex Boolean logic, multiple actions, full control
- **Format**: `Rules <ReturnType> ruleName(<params>)`
- **Structure**: Explicit column markers (C1, C2, A1, RET1)
- **Matching**: Explicit column assignment
- **Example**: Insurance premium with complex risk calculations
- **Best for**: Maximum flexibility, complex conditions

#### 2. Simple Rules Table
- **Use when**: Simple logic, parameters map left-to-right
- **Format**: `SimpleRules <ReturnType> ruleName(<params>)`
- **Structure**: No column markers
- **Matching**: Positional (first condition = first parameter)
- **Example**: Discount based on tier and amount
- **Best for**: Quick setup, straightforward logic

#### 3. Smart Rules Table
- **Use when**: Need flexible column ordering
- **Format**: `SmartRules <ReturnType> ruleName(<params>)`
- **Structure**: No column markers
- **Matching**: By name (column title matches parameter name)
- **Example**: Policy validation with many parameters
- **Best for**: Readable tables, parameters have descriptive names

#### 4. Simple Lookup Table
- **Use when**: Two-dimensional lookup (rate table)
- **Format**: `SimpleLookup <ReturnType> ruleName(<params>)`
- **Structure**: Horizontal conditions (HC1, HC2...) across top, vertical on left
- **Matching**: Positional
- **Example**: Premium rates by risk level × age bracket
- **Best for**: Rate tables, cross-reference tables

#### 5. Smart Lookup Table
- **Use when**: 2D lookup with flexible parameter matching
- **Format**: `SmartLookup <ReturnType> ruleName(<params>)`
- **Structure**: Horizontal conditions with smart vertical matching
- **Matching**: Vertical by name, horizontal positional
- **Example**: Tax rates by state × income bracket
- **Best for**: Flexible 2D lookups

### B. SPREADSHEET TABLES (For Calculations)

Multi-step calculations with intermediate values:

- **Use when**: Complex calculations, need breakdown, audit trail
- **Format**: `Spreadsheet <ReturnType> spreadsheetName(<params>)`
- **Structure**: Grid with row/column names, formulas reference cells using `$columnName`
- **Return types**:
  - `SpreadsheetResult` - Returns entire calculation matrix
  - Specific type (int, double) - Returns final value
- **Example**: Insurance premium with base, adjustments, discounts shown
- **Best for**: Financial calculations, multi-stage pricing

## Choosing Between Decision Table and Spreadsheet

**Decision Table**: If-then rules, conditional logic
- "If driver is SAFE and age > 25, then premium is 800"
- Multiple conditions select one outcome
- Focus: Which rule applies?

**Spreadsheet**: Step-by-step calculations
- "Step1: base = 1000, Step2: adjusted = base * 1.2, Step3: final = adjusted - discount"
- Formulas build on previous results
- Focus: How is value calculated?

**Key difference**: Spreadsheets can call decision tables, but not vice versa.

## Dimension Properties (Optional)

After creating the table, consider adding dimension properties for versioning:

**Geographic**:
- `state`: US state (CA, NY, TX, CW=country-wide)
- `country`: Country code (US, CA, UK)
- `region`: US region (West, East, South)

**Business**:
- `lob`: Line of Business (Auto, Home, Life)
- `currency`: Currency code (USD, EUR, GBP)

**Temporal**:
- `effectiveDate`: When rule becomes legally active (MM/DD/YYYY)
- `expirationDate`: When rule expires
- `startRequestDate`: When rule is operationally introduced
- `endRequestDate`: When rule is operationally retired

**Scenario**:
- `version`: Version name (v1, draft, Q1_2025)
- `active`: Active flag (true/false)

**Why use dimension properties?**
- Multiple versions of SAME rule coexist (different states, dates, LOBs)
- OpenL selects appropriate version at runtime based on request context
- Example: `calculatePremium` for CA effective 01/01/2025 vs TX effective 06/15/2025

## Example Interactions

**User**: "Create a decision table for premium calculation"
**You**: "I'll help you create a decision table. A few questions:

1. **Table type**: How complex is your logic?
   - Simple conditions (tier, amount) → SimpleRules
   - Many parameters, flexible ordering → SmartRules
   - Complex conditions, actions → Rules
   - 2D lookup (risk × age) → SimpleLookup or SmartLookup

2. **Parameters**: What inputs does the rule need?
   - Example: driverType (String), age (int), vehicleValue (double)

3. **Conditions**: What determines the outcome?
   - Example: driverType == "SAFE", age > 25

4. **Return**: What does it calculate?
   - Example: int premium

5. **Versioning**: Does this vary by state, date, or LOB?
   - If yes, we'll add dimension properties"

**User**: "Create a spreadsheet for premium calculation"
**You**: "I'll create a Spreadsheet table. To set it up:

1. **Steps**: What calculation steps are needed?
   - Example: Step1=base, Step2=adjustments, Step3=discounts, Step4=final

2. **Columns**: What values are calculated?
   - Example: A=inputs, B=factors, Premium=calculated value

3. **Formulas**: How are values calculated?
   - Example: Premium at Step3 = `$A * $B * 1.2`

4. **Return type**:
   - SpreadsheetResult (show full breakdown)
   - int/double (just final value)

Spreadsheets are great when you need to show how a value is derived step-by-step."

## Rare Table Types

The following types are rarely used but available:

- **Method**: Java-like code (for complex algorithms)
- **TBasic**: Flow control (loops, conditionals)
- **Data**: Test/reference data storage
- **Datatype**: Custom data structure definitions
- **Test**: Unit testing rules
- **Run**: Test suite execution
- **Properties**: Category/module properties
- **Configuration**: Environment settings

Most users only need Decision Tables and Spreadsheets.
```

---

### Phase 5: Tool Name and Description Optimization
**Goal**: Ensure excellent MCP server performance through clear naming and descriptions

**Duration**: 1-2 hours

#### 5.1 Review All Tool Names

**Principles**:
1. **Verb-first**: `get_file_history`, `create_rule`, `list_tables`
2. **Clear intent**: Name should reveal purpose without reading description
3. **Consistent naming**: Similar operations use similar patterns
4. **No abbreviations**: `get_project` not `get_proj`

**Review checklist**:
```
✅ list_repositories
✅ list_branches
✅ list_projects
✅ get_project (v2.0 - comprehensive)
✅ open_project
✅ close_project
✅ list_tables (v2.0 - with filters)
✅ get_table
✅ update_table
✅ create_rule
✅ copy_table
✅ save_project (UPDATED - returns commit hash)
✅ upload_file (UPDATED - returns commit hash)
✅ download_file (UPDATED - accepts version)
✅ list_deployments
✅ deploy_project
✅ run_all_tests
✅ run_test
✅ validate_project
✅ get_project_errors
✅ execute_rule
✅ compare_versions (UPDATED - uses commit hashes)
✅ revert_version (UPDATED - uses commit hash)

🆕 get_file_history (NEW)
🆕 get_project_history (NEW)
🆕 get_file_name_pattern (NEW)
🆕 set_file_name_pattern (NEW)
🆕 get_table_properties (NEW)
🆕 set_table_properties (NEW)

❌ version_file (REMOVED - hallucination)
❌ health_check (REMOVED - not needed)
❌ create_branch (REMOVED - Git operation, not business rules)
❌ get_project_history (REMOVED - redundant with get_project)
```

#### 5.2 Optimize Tool Descriptions

**Format**:
```typescript
{
  name: "tool_name",
  description: `[One sentence purpose]. [Key features]. [When to use].`,
  inputSchema: zodToJsonSchema(schema)
}
```

**Examples**:

**Good**:
```typescript
{
  name: "get_file_history",
  description: "Get Git commit history for a specific file. Returns list of commits with hashes, authors, timestamps, and commit types. Use this to see all versions of a file over time.",
  inputSchema: zodToJsonSchema(getFileHistorySchema)
}
```

**Bad** (too vague):
```typescript
{
  name: "get_file_history",
  description: "Get history for a file",  // Missing: What kind of history? What's returned? When to use?
  inputSchema: zodToJsonSchema(getFileHistorySchema)
}
```

**Review all tool descriptions**:
- Each description should be 2-3 sentences
- First sentence: What it does
- Second sentence: What it returns or key features
- Third sentence (optional): When to use or important notes

#### 5.3 Optimize Parameter Descriptions

**Principles**:
1. **Include examples**: `"File path (e.g., 'rules/Insurance.xlsx')"`
2. **Specify format**: `"Git commit hash (e.g., '7a3f2b1c...')"`
3. **Mention defaults**: `"Maximum commits (default: 50, max: 200)"`
4. **Clarify optional**: `"Optional: Git branch name (default: current branch)"`

**Example schema with optimized descriptions**:
```typescript
export const getFileHistorySchema = z.object({
  projectId: projectIdSchema.describe("Project identifier in format 'repository-projectName'"),
  filePath: z.string().min(1).describe("File path within project (e.g., 'rules/Insurance.xlsx')"),
  limit: z.number().int().positive().max(200).optional()
    .describe("Maximum number of commits to return (default: 50, max: 200)"),
  offset: z.number().int().nonnegative().optional()
    .describe("Number of commits to skip for pagination (default: 0)")
});
```

#### 5.4 Add Tool Usage Examples to Prompts

Each prompt file should include:
1. **Purpose**: What the tool does
2. **When to use**: Scenarios where tool is helpful
3. **Example request**: Show actual usage
4. **Example response**: Show what user gets
5. **Common patterns**: Typical workflows

**Example** (`prompts/file_history.md`):
```markdown
## Example Usage

### Scenario 1: View Recent Changes

**User asks**: "Who changed the Insurance rules last week?"

**Your action**:
1. Use `get_file_history` with `filePath: "rules/Insurance.xlsx"`, `limit: 10`
2. Review commits from last 7 days
3. Report who changed what

**Example**:
```json
{
  "projectId": "production-InsuranceRules",
  "filePath": "rules/Insurance.xlsx",
  "limit": 10
}
```

**Response shows**:
- Commit 7a3f2b1c by John Doe on 2025-01-15: "Updated CA premium rates"
- Commit 9e5d8a2f by Jane Smith on 2025-01-12: "Added deductible rules"

### Scenario 2: Find Version from Last Month

**User asks**: "I need the version of rules from December 15th"

**Your action**:
1. Use `get_file_history` with larger limit
2. Find commit closest to December 15th
3. Use that commit hash with `download_file`

**Workflow**:
1. `get_file_history` → Find commit hash from Dec 15
2. `download_file` with `version: "{commit_hash}"` → Get that version
```

---

### Phase 6: Testing and Validation
**Goal**: Ensure all tools work correctly

**Duration**: 2-3 hours

#### 6.1 Unit Tests

**File**: `tests/client.test.ts`

Add tests for:
- `getFileHistory()` - pagination, filtering
- `getProjectHistory()` - branch filtering
- `getFileNamePattern()` - pattern extraction
- `setFileNamePattern()` - pattern update
- `getTableProperties()` - property retrieval
- `setTableProperties()` - property update
- Updated `saveProject()` - returns commit hash
- Updated `uploadFile()` - returns commit hash
- Updated `downloadFile()` - accepts version parameter
- Updated `compareVersions()` - uses commit hashes
- Updated `revertVersion()` - uses commit hash
- Updated `createRule()` - all table types

**Example test**:
```typescript
describe("getFileHistory", () => {
  it("should return file history with commits", async () => {
    const mockResponse = {
      data: {
        commits: [
          {
            version: "7a3f2b1c",
            author: { name: "John Doe", email: "john@example.com" },
            modifiedAt: "2025-01-15T10:00:00Z",
            comment: "Updated rules Type: SAVE.",
            size: 12345
          }
        ],
        total: 1
      }
    };

    mockAxios.onGet().reply(200, mockResponse.data);

    const result = await client.getFileHistory({
      projectId: "repo-project",
      filePath: "rules/Insurance.xlsx"
    });

    expect(result.commits).toHaveLength(1);
    expect(result.commits[0].commitHash).toBe("7a3f2b1c");
    expect(result.commits[0].commitType).toBe("SAVE");
  });
});
```

#### 6.2 Integration Tests

**File**: `tests/integration.test.ts`

Test complete workflows:
1. **Version History Workflow**:
   - Get file history
   - Download specific version
   - Compare two versions
   - Revert to previous version

2. **Dimension Properties Workflow**:
   - Get file name pattern
   - Set file name pattern
   - Create table with properties
   - Get table properties
   - Update table properties

3. **Create Rule Workflow**:
   - Create Rules table
   - Create SimpleRules table
   - Create SmartRules table
   - Create SimpleLookup table
   - Create SmartLookup table
   - Create Spreadsheet table

#### 6.3 Manual Testing

**Test scenarios**:
1. Save project → verify commit hash returned
2. Upload file → verify commit hash returned
3. Get file history → verify commits listed
4. Download old version → verify correct version retrieved
5. Compare versions → verify changes detected
6. Revert version → verify new commit created
7. Create decision table → verify structure correct
8. Create spreadsheet → verify formulas work
9. Set dimension properties → verify properties applied
10. File name pattern → verify properties in file name

---

### Phase 7: Documentation Updates
**Goal**: Update README and documentation

**Duration**: 1 hour

#### 7.1 Update README.md

**Sections to add/update**:

1. **Versioning Section**:
```markdown
## Versioning in OpenL Tablets

OpenL Tablets has **two independent versioning systems**:

### 1. Git-Based Repository Versioning

Every save/upload creates a Git commit:
- Version identifier = commit hash (e.g., "7a3f2b1c...")
- File names never change between versions
- Tools: `get_file_history`, `get_project_history`, `download_file` (with version), `compare_versions`, `revert_version`

### 2. Dimension Properties Versioning

Multiple versions of same rule differentiated by business properties:
- Geographic: state, country, region
- Business: lob, currency
- Temporal: effectiveDate, expirationDate
- Tools: `get_file_name_pattern`, `set_file_name_pattern`, `get_table_properties`, `set_table_properties`
```

2. **Table Types Section**:
```markdown
## Table Types

### Most Common

**Decision Tables** (5 variants):
- `Rules` - Explicit column markers (C1, A1, RET1), complex logic
- `SimpleRules` - Positional matching, simple syntax
- `SmartRules` - Name-based matching, flexible layout
- `SimpleLookup` - 2D lookup, positional
- `SmartLookup` - 2D lookup, smart matching

**Spreadsheet Tables**:
- Multi-step calculations with formulas
- Cell references: `$columnName`, `$rowName$columnName`
- Returns: `SpreadsheetResult` or specific value

### Rarely Used

Method, TBasic, Data, Datatype, Test, Run, Properties, Configuration
```

3. **Tools List Section**:

Update with all 27 tools (after Phase 1-2):
- 21 existing tools (3 removed, 6 updated)
- 6 new tools

#### 7.2 Update Tool Count

**Final tool count**: 24 tools

**Breakdown**:
- Core project operations: 6 (list_repositories, list_branches, list_projects, get_project, open_project, close_project)
- Table operations: 5 (list_tables, get_table, update_table, create_rule, copy_table)
- File operations: 4 (upload_file, download_file, save_project, get_file_history)
- Version control: 4 (get_project_history, compare_versions, revert_version)
- Testing & validation: 4 (run_all_tests, run_test, validate_project, get_project_errors)
- Deployment: 2 (list_deployments, deploy_project)
- Execution: 1 (execute_rule)
- Dimension properties: 4 (get_file_name_pattern, set_file_name_pattern, get_table_properties, set_table_properties)

**Removed**: 3 tools (version_file, health_check, create_branch)
**Added**: 6 tools (get_file_history, get_project_history, get_file_name_pattern, set_file_name_pattern, get_table_properties, set_table_properties)
**Updated**: 6 tools (save_project, upload_file, download_file, compare_versions, revert_version, create_rule)

---

## Implementation Order

### Week 1: Critical Fixes
- **Day 1-2**: Phase 1 (Remove incorrect tools, fix version-related tools)
- **Day 3-4**: Phase 2 (Add Git version history tools)
- **Day 5**: Testing Phase 1-2

### Week 2: Dimension Properties and Table Types
- **Day 1-2**: Phase 3 (Dimension properties tools)
- **Day 3-5**: Phase 4 (Update create_rule for multiple table types)

### Week 3: Polish and Documentation
- **Day 1**: Phase 5 (Optimize naming and descriptions)
- **Day 2-3**: Phase 6 (Testing and validation)
- **Day 4-5**: Phase 7 (Documentation updates)

---

## Success Criteria

### Phase 1 ✅
- [ ] `version_file` tool removed
- [ ] `save_project` returns commit hash
- [ ] `upload_file` returns commit hash
- [ ] `download_file` accepts version parameter
- [ ] `compare_versions` uses commit hashes
- [ ] `revert_version` uses commit hash
- [ ] All tests pass
- [ ] Build succeeds
- [ ] Linter clean

### Phase 2 ✅
- [ ] `get_file_history` tool added and working
- [ ] `get_project_history` tool added and working
- [ ] Prompts created (file_history.md)
- [ ] Tests pass
- [ ] Integration tests pass

### Phase 3 ✅
- [ ] `get_file_name_pattern` tool added
- [ ] `set_file_name_pattern` tool added
- [ ] `get_table_properties` tool added
- [ ] `set_table_properties` tool added
- [ ] Prompts created (dimension_properties.md)
- [ ] Tests pass

### Phase 4 ✅
- [ ] `create_rule` supports all 5 decision table types
- [ ] `create_rule` supports Spreadsheet tables
- [ ] `create_rule` supports dimension properties
- [ ] Prompts updated (create_rule.md)
- [ ] Tests cover all table types

### Phase 5 ✅
- [ ] All tool names reviewed and optimized
- [ ] All tool descriptions updated
- [ ] All parameter descriptions include examples
- [ ] Prompts include usage examples

### Phase 6 ✅
- [ ] Unit tests: 90%+ coverage
- [ ] Integration tests: Key workflows covered
- [ ] Manual testing: All scenarios verified
- [ ] No regressions

### Phase 7 ✅
- [ ] README.md updated
- [ ] All prompt files created/updated
- [ ] Tool count documented
- [ ] Examples added

---

## Performance Optimization Tips

### 1. Clear Tool Names
- Use verb-first naming: `get_`, `list_`, `create_`, `update_`, `set_`
- Be specific: `get_file_history` not `get_history`
- Avoid abbreviations: `get_project` not `get_proj`

### 2. Detailed Descriptions
- First sentence: What it does
- Second sentence: What it returns
- Third sentence: When to use
- Include keywords AI will search for

### 3. Rich Parameter Descriptions
- Always include examples in parentheses
- Specify format (Git commit hash, ISO date, etc.)
- Mention defaults and constraints
- Use "Optional:" prefix for optional params

### 4. Comprehensive Prompts
- Explain the "why" not just "what"
- Show example interactions
- Include common workflows
- Provide decision trees (when to use X vs Y)

### 5. Consistent Patterns
- Similar operations use similar naming
- Similar parameters use similar descriptions
- Similar return types use similar structures
- Patterns help AI learn faster

---

## Risk Mitigation

### Risk 1: OpenL API Differences
**Problem**: Actual OpenL REST API might differ from assumptions

**Mitigation**:
- Phase implementation (can adjust based on API responses)
- Mock API responses in tests
- Document API endpoint assumptions
- Add fallback error handling

### Risk 2: Complex Table Structures
**Problem**: Creating tables via API might be more complex than anticipated

**Mitigation**:
- Start with simplest table type (SimpleRules)
- Incremental complexity (SimpleRules → SmartRules → Rules)
- Validate with actual OpenL instance if possible
- Allow raw JSON input as escape hatch

### Risk 3: Dimension Properties Complexity
**Problem**: Dimension properties might have validation rules we don't know

**Mitigation**:
- Start with simple properties (state, lob)
- Add validation based on errors encountered
- Document known property values
- Provide clear error messages

### Risk 4: Breaking Changes
**Problem**: Updating existing tools might break current users

**Mitigation**:
- Version tools (e.g., get_project v2.0)
- Keep backward compatibility where possible
- Document changes in CHANGELOG
- Provide migration guide

---

## Next Steps After Implementation

1. **Gather Real-World Feedback**
   - Deploy to test users
   - Monitor tool usage patterns
   - Identify pain points

2. **Optimize Based on Usage**
   - Most-used tools get priority for optimization
   - Rarely-used tools can be simplified
   - Add convenience tools based on common patterns

3. **Expand Table Type Support**
   - Add remaining table types if requested
   - Optimize for most common use cases
   - Consider template library

4. **Enhanced Prompts**
   - Add interactive decision trees
   - Include more examples
   - Add troubleshooting guides

5. **Performance Monitoring**
   - Track tool success/failure rates
   - Monitor response times
   - Identify bottlenecks

---

## Summary

This implementation plan:

✅ **Removes hallucinated functionality** (version_file)
✅ **Fixes critical version-related tools** (save_project, upload_file, download_file, compare_versions, revert_version)
✅ **Adds Git version history support** (get_file_history, get_project_history)
✅ **Adds dimension properties support** (4 new tools)
✅ **Expands table type support** (Decision Tables + Spreadsheet)
✅ **Optimizes for MCP performance** (clear names, detailed descriptions, rich prompts)
✅ **Phased and testable** (7 phases over 3 weeks)
✅ **Low risk** (incremental, can adjust based on feedback)

**Total**: 24 tools (from 21 current)
- **Removed**: 3 (version_file, health_check, create_branch)
- **Added**: 6 (file history, project history, dimension properties)
- **Updated**: 6 (version-related tools, create_rule)
- **Unchanged**: 15

This creates a **high-performance, well-documented MCP server** that correctly implements OpenL Tablets functionality with clear naming, detailed descriptions, and comprehensive prompts for optimal AI assistant interaction.
