# Projects Merge API Documentation

**Version**: 6.0.0-SNAPSHOT
**Status**: BETA
**Base Path**: `/projects/{projectId}/merge`
**Last Updated**: 2025-12-18

---

## Table of Contents

1. [Overview](#overview)
2. [API Architecture](#api-architecture)
3. [API Reference](#api-reference)
4. [Data Models](#data-models)
5. [Workflows](#workflows)
6. [Error Handling](#error-handling)
7. [Examples](#examples)
8. [Best Practices](#best-practices)

---

## Overview

The Projects Merge API provides a comprehensive REST interface for managing Git branch merge operations in OpenL Tablets projects. This API enables developers to check merge feasibility, perform merges, handle conflicts, and resolve conflicts through automated strategies or custom file uploads.

### Key Features

- **Pre-merge Validation**: Check if branches can be merged without conflicts
- **Bidirectional Merging**: Support for both receiving changes from other branches and sending changes to other branches
- **Conflict Detection**: Automatic detection of merge conflicts with detailed file-level information
- **Conflict Resolution**: Multiple strategies for resolving conflicts (BASE, OURS, THEIRS, CUSTOM)
- **Session Management**: Maintains conflict state across multiple API calls
- **Excel File Prioritization**: Automatically prioritizes Excel files in conflict lists (business logic importance)
- **Project State Management**: Handles project lifecycle (open/close/refresh) during merge operations

### Use Cases

1. **Automated Merging**: Integrate branch merging into CI/CD pipelines
2. **Conflict Management**: Programmatically detect and resolve merge conflicts
3. **Multi-branch Development**: Coordinate changes across multiple development branches
4. **Release Management**: Merge feature branches into main/release branches
5. **Code Review Integration**: Check merge status before approving pull requests

### Architecture Overview

``` mermaid
flowchart TB
    %% REST Controller Layer
    subgraph REST["REST Controller Layer"]
        C[ProjectsMergeController<br/>- Input validation<br/>- Request/response mapping<br/>- Session management]
    end

    %% Service Layer
    subgraph SERVICE["Service Layer"]
        S1[ProjectsMergeService<br/>- Check merge status<br/>- Perform merge<br/>- Handle conflicts]
        S2[ProjectsMergeConflictsService<br/>- Analyze conflicts<br/>- Group conflicts<br/>- Resolve conflicts]
    end

    %% Repository / Git Layer
    subgraph GIT["Repository / Git Layer"]
        G[Git Operations<br/>- merge / checkout / commit<br/>- Conflict detection<br/>- File version management]
    end

    %% Connections
    C --> S1
    C --> S2
    S1 --> G
    S2 --> G
```

---

## API Architecture

### Component Architecture

#### 1. **ProjectsMergeController**
**Location**: `org.openl.studio.projects.rest.controller.ProjectsMergeController`

**Responsibilities**:
- Expose REST endpoints for merge operations
- Validate incoming requests
- Manage conflict session lifecycle
- Handle project state during merge operations
- Coordinate with service layer

**Dependencies**:
- `ProjectsMergeService`: Core merge logic
- `ProjectsMergeConflictsService`: Conflict analysis and resolution
- `WorkspaceProjectService`: Project lifecycle management
- `ProjectsMergeConflictsSessionHolder`: Session-based conflict storage

#### 2. **ProjectsMergeService**
**Responsibilities**:
- Check merge feasibility
- Execute merge operations
- Detect merge conflicts
- Coordinate Git operations

#### 3. **ProjectsMergeConflictsService**
**Responsibilities**:
- Analyze merge conflicts
- Group conflicts by project
- Retrieve conflict file versions
- Apply resolution strategies
- Generate conflict resolution results

#### 4. **ProjectsMergeConflictsSessionHolder**
**Responsibilities**:
- Store conflict information in user session
- Maintain state between check/merge and resolution
- Clean up resolved conflicts

### Session Management

The API uses session-based storage for conflict information:

```
1. User calls /check or /merge → Conflicts detected
2. Controller stores MergeConflictInfo in session → SessionHolder
3. User calls /conflicts → Retrieves conflicts from session
4. User calls /conflicts/resolve → Applies resolutions
5. On success → Clears session data
```

**Session Key**: Resolved project ID (combination of repository ID and project name)

### State Machine

``` mermaid
stateDiagram-v2
    [*] --> Initial

    Initial --> ConflictsStored: POST /check\n(conflicts detected)
    Initial --> ConflictsStored: POST /merge\n(conflicts detected)

    ConflictsStored --> ConflictsStored: GET /conflicts
    ConflictsStored --> ConflictsStored: GET /conflicts/files

    ConflictsStored --> Resolving: POST /conflicts/resolve

    Resolving --> ConflictsStored: Resolution fails
    Resolving --> Cleared: Resolution success
    Resolving --> Cleared: DELETE /conflicts (cancel)

    Cleared --> [*]
```

---

## API Reference

### 1. Check Merge Status

**Endpoint**: `POST /projects/{projectId}/merge/check`

**Description**: Checks if two branches can be merged without conflicts. Does not modify any data.

**HTTP Method**: POST

**Path Parameters**:
- `projectId` (string, required): Project identifier

**Request Body**:
```json
{
  "mode": "receive|send",
  "otherBranch": "branch-name"
}
```

**Response**: `200 OK`
```json
{
  "sourceBranch": "feature-branch",
  "targetBranch": "main",
  "status": "mergeable|up-to-date"
}
```

**Possible Statuses**:
- `mergeable`: Branches can be merged without conflicts
- `up-to-date`: Target branch is already up-to-date

**Errors**:
- `409 Conflict`: Project has unresolved merge conflicts from a previous operation
- `404 Not Found`: Project or branch not found

---

### 2. Perform Merge

**Endpoint**: `POST /projects/{projectId}/merge`

**Description**: Executes a merge operation between two branches. If conflicts are detected, they are stored in session for later resolution.

**HTTP Method**: POST

**Path Parameters**:
- `projectId` (string, required): Project identifier

**Request Body**:
```json
{
  "mode": "receive|send",
  "otherBranch": "branch-name"
}
```

**Merge Modes**:
- `receive`: Merge changes FROM `otherBranch` INTO current branch
- `send`: Merge changes FROM current branch INTO `otherBranch`

**Response**: `200 OK`

**Success (no conflicts)**:
```json
{
  "status": "success",
  "conflictGroups": []
}
```

**Conflicts detected**:
```json
{
  "status": "conflicts",
  "conflictGroups": [
    {
      "projectName": "MyProject",
      "projectPath": "projects/MyProject",
      "files": [
        "rules/BusinessRules.xlsx",
        "rules/ValidationRules.xlsx",
        "rules.xml"
      ]
    }
  ]
}
```

**Errors**:
- `409 Conflict`: Project has unresolved merge conflicts OR branches cannot be merged
- `404 Not Found`: Project or branch not found
- `500 Internal Server Error`: Git operation failed

**Side Effects**:
- On success: Project is merged, workspace refreshed, project reopened if previously open
- On conflicts: Conflict information stored in session, project state unchanged

---

### 3. Get Merge Conflicts

**Endpoint**: `GET /projects/{projectId}/merge/conflicts`

**Description**: Retrieves detailed information about merge conflicts for a project. Requires a previous merge operation that detected conflicts.

**HTTP Method**: GET

**Path Parameters**:
- `projectId` (string, required): Project identifier

**Response**: `200 OK`
```json
[
  {
    "projectName": "MyProject",
    "projectPath": "projects/MyProject",
    "files": [
      "rules/BusinessRules.xlsx",
      "rules/ValidationRules.xlsx",
      "rules.xml"
    ]
  }
]
```

**File Ordering**:
- Excel files (`.xls`, `.xlsx`) appear first (prioritized for business logic importance)
- Other files sorted alphabetically (case-insensitive)

**Errors**:
- `404 Not Found`: No conflict information found in session

---

### 4. Get Conflicted File

**Endpoint**: `GET /projects/{projectId}/merge/conflicts/files`

**Description**: Downloads a specific version of a conflicted file.

**HTTP Method**: GET

**Path Parameters**:
- `projectId` (string, required): Project identifier

**Query Parameters**:
- `file` (string, required): Relative path to the conflicted file
- `side` (enum, required): Version to retrieve: `BASE`, `OURS`, or `THEIRS`

**Side Definitions**:
- `BASE`: Common ancestor version (before branches diverged)
- `OURS`: Version from the current branch
- `THEIRS`: Version from the merging branch

**Response**: `200 OK`
- Content-Type: Determined by file extension (e.g., `application/vnd.ms-excel` for .xlsx)
- Content-Disposition: `attachment; filename="filename.ext"`
- Body: Binary file content

**Example**:
```bash
GET /projects/MyProject/merge/conflicts/files?file=rules/BusinessRules.xlsx&side=OURS
```

**Errors**:
- `404 Not Found`: No conflict information found OR file not in conflict list
- `400 Bad Request`: Invalid side parameter

---

### 5. Resolve Conflicts

**Endpoint**: `POST /projects/{projectId}/merge/conflicts/resolve`

**Description**: Resolves merge conflicts using specified strategies. Can upload custom files for custom resolution.

**HTTP Method**: POST

**Content-Type**: `multipart/form-data`

**Path Parameters**:
- `projectId` (string, required): Project identifier

**Request Parameters**:
- `resolutions` (array, required): JSON array of resolution strategies (minimum 1)
- `message` (string, optional): Commit message for the merge resolution
- `files` (multipart files, conditional): Custom files for CUSTOM strategy

**Resolution Format**:
```json
{
  "resolutions": [
    {
      "filePath": "rules/BusinessRules.xlsx",
      "strategy": "OURS",
      "file": null
    },
    {
      "filePath": "rules.xml",
      "strategy": "CUSTOM",
      "file": "<multipart-file>"
    }
  ],
  "message": "Resolved merge conflicts: kept business rules from current branch"
}
```

**Resolution Strategies**:
- `BASE`: Use the common ancestor version
- `OURS`: Use the current branch version
- `THEIRS`: Use the merging branch version
- `CUSTOM`: Use a custom uploaded file (file parameter required)

**Response**: `200 OK`
```json
{
  "status": "success",
  "resolvedFiles": [
    "rules/BusinessRules.xlsx",
    "rules.xml"
  ]
}
```

**Errors**:
- `404 Not Found`: No conflict information found in session
- `400 Bad Request`:
  - CUSTOM strategy without file upload
  - Empty resolutions array
  - Invalid file path
- `500 Internal Server Error`: Resolution operation failed

**Side Effects**:
- On success:
  - Conflict session data cleared
  - Merge completed and committed
  - Workspace refreshed
  - Project reopened if previously open
- On failure:
  - Session data preserved
  - Can retry resolution

---

### 6. Cancel Merge Conflicts

**Endpoint**: `DELETE /projects/{projectId}/merge/conflicts`

**Description**: Cancels an ongoing merge conflict resolution session without applying changes.

**HTTP Method**: DELETE

**Path Parameters**:
- `projectId` (string, required): Project identifier

**Response**: `204 No Content`

**Errors**:
- `404 Not Found`: No conflict information found in session

**Side Effects**:
- Conflict session data cleared
- No changes made to repository
- User can initiate a new merge operation

---

## Data Models

### MergeRequest

```typescript
interface MergeRequest {
  mode: 'receive' | 'send';      // Merge direction
  otherBranch: string;           // Target/source branch name
}
```

**Validation**:
- `mode`: Required, must be 'receive' or 'send'
- `otherBranch`: Required, non-blank string

---

### CheckMergeResult

```typescript
interface CheckMergeResult {
  sourceBranch: string;          // Source branch in the merge
  targetBranch: string;          // Target branch in the merge
  status: CheckMergeStatus;      // Merge feasibility status
}
```

**CheckMergeStatus Values**:
- `mergeable`: Branches can be merged without conflicts
- `up-to-date`: Target is already up-to-date with source

---

### MergeResultResponse

```typescript
interface MergeResultResponse {
  status: MergeResultStatus;     // Merge operation result
  conflictGroups: ConflictGroup[]; // Conflicts if any
}
```

**MergeResultStatus Values**:
- `success`: Merge completed without conflicts
- `conflicts`: Merge detected conflicts requiring resolution

---

### ConflictGroup

```typescript
interface ConflictGroup {
  projectName: string;           // Project name
  projectPath: string;           // Repository path to project
  files: string[];               // Array of conflicted file paths
}
```

**File Ordering**:
- Excel files appear first (business logic priority)
- Remaining files sorted alphabetically (case-insensitive)

---

### ConflictBase

```typescript
enum ConflictBase {
  BASE = 'BASE',                 // Common ancestor version
  OURS = 'OURS',                 // Current branch version
  THEIRS = 'THEIRS'              // Merging branch version
}
```

---

### ResolveConflictsRequest

```typescript
interface ResolveConflictsRequest {
  resolutions: FileConflictResolution[]; // At least 1 required
  message?: string;                      // Optional commit message
}

interface FileConflictResolution {
  filePath: string;                      // Path to conflicted file
  strategy: ConflictResolutionStrategy;  // Resolution approach
  file?: File;                           // Required for CUSTOM strategy
}
```

**ConflictResolutionStrategy Values**:
- `BASE`: Use common ancestor version
- `OURS`: Use current branch version
- `THEIRS`: Use merging branch version
- `CUSTOM`: Use uploaded custom file

**Validation**:
- At least one resolution required
- If strategy is `CUSTOM`, file must be provided
- File path must match a file in the conflict list

---

### ResolveConflictsResponse

```typescript
interface ResolveConflictsResponse {
  status: ConflictResolutionStatus; // Resolution result
  resolvedFiles: string[];          // Successfully resolved files
}
```

**ConflictResolutionStatus Values**:
- `success`: All conflicts resolved successfully

---

## Workflows

### Workflow 1: Simple Merge (No Conflicts)

```
1. Check merge status
   POST /projects/MyProject/merge/check
   {
     "mode": "receive",
     "otherBranch": "feature-123"
   }

   Response: { "status": "mergeable", ... }

2. Perform merge
   POST /projects/MyProject/merge
   {
     "mode": "receive",
     "otherBranch": "feature-123"
   }

   Response: { "status": "success", "conflictGroups": [] }

✓ Merge completed successfully
```

---

### Workflow 2: Merge with Conflicts

```
1. Attempt merge
   POST /projects/MyProject/merge
   {
     "mode": "receive",
     "otherBranch": "feature-123"
   }

   Response: {
     "status": "conflicts",
     "conflictGroups": [{
       "projectName": "MyProject",
       "files": ["rules/BusinessRules.xlsx", "rules.xml"]
     }]
   }

2. Get detailed conflicts
   GET /projects/MyProject/merge/conflicts

   Response: [{ "files": ["rules/BusinessRules.xlsx", "rules.xml"] }]

3. Download conflict versions for review
   GET /projects/MyProject/merge/conflicts/files?file=rules/BusinessRules.xlsx&side=OURS
   GET /projects/MyProject/merge/conflicts/files?file=rules/BusinessRules.xlsx&side=THEIRS

4. Resolve conflicts
   POST /projects/MyProject/merge/conflicts/resolve
   Content-Type: multipart/form-data

   resolutions=[
     {"filePath": "rules/BusinessRules.xlsx", "strategy": "OURS"},
     {"filePath": "rules.xml", "strategy": "THEIRS"}
   ]
   message="Merged feature-123: kept our business rules, accepted their config"

   Response: {
     "status": "success",
     "resolvedFiles": ["rules/BusinessRules.xlsx", "rules.xml"]
   }

✓ Conflicts resolved, merge completed
```

---

### Workflow 3: Custom Conflict Resolution

```
1. Merge with conflicts detected
   POST /projects/MyProject/merge
   { "mode": "receive", "otherBranch": "feature-123" }

   Response: { "status": "conflicts", ... }

2. Download all versions for manual merge
   GET /projects/MyProject/merge/conflicts/files?file=rules.xml&side=BASE
   GET /projects/MyProject/merge/conflicts/files?file=rules.xml&side=OURS
   GET /projects/MyProject/merge/conflicts/files?file=rules.xml&side=THEIRS

3. Manually merge files locally (external tool)
   - User creates merged-rules.xml combining changes

4. Upload custom resolution
   POST /projects/MyProject/merge/conflicts/resolve
   Content-Type: multipart/form-data

   resolutions=[{"filePath": "rules.xml", "strategy": "CUSTOM"}]
   files[0]=merged-rules.xml
   message="Custom merge of rules.xml"

   Response: { "status": "success", "resolvedFiles": ["rules.xml"] }

✓ Custom merge applied successfully
```

---

### Workflow 4: Cancel Conflicts

```
1. Merge with conflicts detected
   POST /projects/MyProject/merge
   { "mode": "receive", "otherBranch": "feature-123" }

   Response: { "status": "conflicts", ... }

2. User decides not to proceed
   DELETE /projects/MyProject/merge/conflicts

   Response: 204 No Content

✓ Conflict session cleared, can start new merge
```

---

## Error Handling

### Error Response Format

```json
{
  "status": 409,
  "error": "Conflict",
  "message": "project.unresolved.merge.conflicts.message",
  "path": "/projects/MyProject/merge"
}
```

---

### Common Error Scenarios

#### 1. Unresolved Conflicts Exist

**Scenario**: User tries to merge while previous conflicts are unresolved

**Request**:
```bash
POST /projects/MyProject/merge/check
```

**Response**: `409 Conflict`
```json
{
  "message": "project.unresolved.merge.conflicts.message"
}
```

**Resolution**:
- Resolve existing conflicts: `POST /conflicts/resolve`
- Cancel existing conflicts: `DELETE /conflicts`

---

#### 2. No Conflict Information Found

**Scenario**: User tries to access conflicts without a previous merge operation

**Request**:
```bash
GET /projects/MyProject/merge/conflicts
```

**Response**: `404 Not Found`
```json
{
  "message": "project.merge.result.not.found.message"
}
```

**Resolution**: Perform a merge operation first

---

#### 3. Branches Not Mergeable

**Scenario**: Merge check indicates branches cannot be merged

**Request**:
```bash
POST /projects/MyProject/merge
{"mode": "receive", "otherBranch": "incompatible-branch"}
```

**Response**: `409 Conflict`
```json
{
  "message": "project.branch.merge.not.mergeable.message"
}
```

**Resolution**: Check merge status first, resolve underlying issues

---

#### 4. Custom File Missing

**Scenario**: User selects CUSTOM strategy without uploading file

**Request**:
```bash
POST /projects/MyProject/merge/conflicts/resolve
resolutions=[{"filePath": "rules.xml", "strategy": "CUSTOM"}]
# No file uploaded
```

**Response**: `400 Bad Request`
```json
{
  "message": "project.merge.conflict.custom.file.missing.message",
  "args": ["rules.xml"]
}
```

**Resolution**: Upload the custom file in the request

---

## Examples

### Example 1: Receive Changes from Feature Branch

**Scenario**: Merge changes from `feature-user-auth` into current `main` branch

```bash
# Step 1: Check if merge is possible
curl -X POST http://localhost:8080/projects/MyProject/merge/check \
  -H "Content-Type: application/json" \
  -d '{
    "mode": "receive",
    "otherBranch": "feature-user-auth"
  }'

# Response
{
  "sourceBranch": "feature-user-auth",
  "targetBranch": "main",
  "status": "mergeable"
}

# Step 2: Perform the merge
curl -X POST http://localhost:8080/projects/MyProject/merge \
  -H "Content-Type: application/json" \
  -d '{
    "mode": "receive",
    "otherBranch": "feature-user-auth"
  }'

# Response (success)
{
  "status": "success",
  "conflictGroups": []
}
```

---

### Example 2: Send Changes to Release Branch

**Scenario**: Merge current `main` branch into `release-2.0`

```bash
curl -X POST http://localhost:8080/projects/MyProject/merge \
  -H "Content-Type: application/json" \
  -d '{
    "mode": "send",
    "otherBranch": "release-2.0"
  }'

# Response (success)
{
  "status": "success",
  "conflictGroups": []
}
```

---

### Example 3: Resolve Conflicts with Multiple Strategies

**Scenario**: Resolve conflicts using different strategies for different files

```bash
# Step 1: Merge detects conflicts
curl -X POST http://localhost:8080/projects/MyProject/merge \
  -H "Content-Type: application/json" \
  -d '{
    "mode": "receive",
    "otherBranch": "feature-pricing"
  }'

# Response (conflicts)
{
  "status": "conflicts",
  "conflictGroups": [{
    "projectName": "MyProject",
    "files": [
      "rules/PricingRules.xlsx",
      "rules/ValidationRules.xlsx",
      "rules.xml",
      "README.md"
    ]
  }]
}

# Step 2: Download versions for review
curl -o ours-pricing.xlsx \
  "http://localhost:8080/projects/MyProject/merge/conflicts/files?file=rules/PricingRules.xlsx&side=OURS"

curl -o theirs-pricing.xlsx \
  "http://localhost:8080/projects/MyProject/merge/conflicts/files?file=rules/PricingRules.xlsx&side=THEIRS"

# Step 3: Resolve with mixed strategies
curl -X POST http://localhost:8080/projects/MyProject/merge/conflicts/resolve \
  -F 'resolutions=[
    {"filePath":"rules/PricingRules.xlsx","strategy":"OURS"},
    {"filePath":"rules/ValidationRules.xlsx","strategy":"THEIRS"},
    {"filePath":"rules.xml","strategy":"CUSTOM"},
    {"filePath":"README.md","strategy":"THEIRS"}
  ]' \
  -F 'message=Merged feature-pricing: kept our pricing rules, accepted their validation rules and docs' \
  -F 'file=@merged-rules.xml'

# Response (success)
{
  "status": "success",
  "resolvedFiles": [
    "rules/PricingRules.xlsx",
    "rules/ValidationRules.xlsx",
    "rules.xml",
    "README.md"
  ]
}
```

---

### Example 4: Cancel Merge Operation

```bash
# Step 1: Merge detects conflicts
curl -X POST http://localhost:8080/projects/MyProject/merge \
  -H "Content-Type: application/json" \
  -d '{"mode":"receive","otherBranch":"feature-abc"}'

# Response (conflicts detected)
{
  "status": "conflicts",
  "conflictGroups": [...]
}

# Step 2: User decides to cancel
curl -X DELETE http://localhost:8080/projects/MyProject/merge/conflicts

# Response: 204 No Content
# Conflict session cleared
```

---

## Best Practices

### 1. Always Check Before Merging

```bash
# Good practice
POST /merge/check  # Check first
POST /merge        # Then merge if safe

# Risky practice
POST /merge        # Merge directly (may create conflicts)
```

### 2. Download All Versions Before Custom Resolution

When using CUSTOM strategy, review all three versions:

```bash
# Download BASE (common ancestor)
GET /conflicts/files?file=rules.xml&side=BASE

# Download OURS (current branch)
GET /conflicts/files?file=rules.xml&side=OURS

# Download THEIRS (merging branch)
GET /conflicts/files?file=rules.xml&side=THEIRS

# Create merged version with full context
```

### 3. Use Descriptive Commit Messages

```json
{
  "resolutions": [...],
  "message": "Merged feature-auth: kept our login rules (v2.0), accepted their LDAP config"
}
```

Better than:
```json
{
  "resolutions": [...],
  "message": "Resolved conflicts"
}
```

### 4. Prioritize Excel Files in Conflict Resolution

The API automatically sorts Excel files first because they contain business logic. Review and resolve these first:

```
Priority order:
1. BusinessRules.xlsx ← Most important (business logic)
2. ValidationRules.xlsx ← Important (business logic)
3. rules.xml ← Configuration
4. README.md ← Documentation
```

### 5. Handle Project State Correctly

The API manages project state automatically:
- Project is frozen during merge
- Dependencies are paused
- Project is reopened after successful merge

**Don't** manually modify project state during merge operations.

### 6. Session Cleanup

Always clean up conflict sessions:

```bash
# If resolution successful → automatic cleanup
POST /conflicts/resolve

# If canceling → manual cleanup
DELETE /conflicts

# Don't leave sessions hanging
```

### 7. Error Recovery

If resolution fails:

```
1. Review error message
2. Fix the issue (e.g., upload missing file)
3. Retry resolution (session preserved)
4. If unable to fix → cancel session
```

### 8. Atomic Operations

Each conflict resolution is atomic:
- All resolutions succeed → merge completes
- Any resolution fails → entire operation rolls back
- Session preserved for retry

### 9. Security Considerations

- Validate file paths to prevent directory traversal
- Limit uploaded file sizes
- Sanitize commit messages
- Check user permissions before merge operations

### 10. Performance Tips

- Use `/check` endpoint before `/merge` for large branches
- Batch conflict resolutions in a single request
- Download conflict files in parallel for faster review
- Consider file size limits for CUSTOM uploads

---

## Technical Implementation Notes

### Session Management

**Storage**: HTTP session with project-scoped keys

**Key Format**: `{repositoryId}:{projectName}`

**Lifecycle**:
1. Created: When merge/check detects conflicts
2. Accessed: During conflict retrieval and resolution
3. Cleared: On successful resolution or explicit cancel
4. Timeout: Follows HTTP session timeout (default: 30 minutes)

### Project Lifecycle During Merge

```java
// Before merge
studio.freezeProject(projectName);     // Prevent concurrent modifications
dependencyManager.pause();             // Pause workspace scanning

try {
  // Perform merge operation
  mergeService.merge(...);

  // On success
  project.close();                     // Close old reference
  project.open();                      // Reopen with new state
  workspace.refresh();                 // Update workspace
  studio.reset();                      // Clear cached data

} finally {
  studio.releaseProject(projectName);  // Always release lock
  dependencyManager.resume();          // Resume scanning
}
```

### Conflict File Retrieval

Files are retrieved from Git working tree with three-way merge markers:
- `BASE`: Common ancestor from merge base
- `OURS`: Current branch HEAD
- `THEIRS`: Other branch HEAD

### Excel File Priority Algorithm

```java
Comparator<String> excelFirstComparator = (f1, f2) -> {
  boolean isExcel1 = FileTypeHelper.isExcelFile(f1);
  boolean isExcel2 = FileTypeHelper.isExcelFile(f2);

  if (isExcel1 && isExcel2) return f1.compareToIgnoreCase(f2);
  if (isExcel1) return -1;  // Excel files first
  if (isExcel2) return 1;

  return f1.compareToIgnoreCase(f2);
};
```

---

## Related APIs

- **Projects API**: Project management operations
- **Branches API**: Branch creation and management
- **Repository API**: Repository configuration

---

## Changelog

### Version 6.0.0-SNAPSHOT (BETA)
- Initial implementation of Projects Merge API
- Support for bidirectional merging (receive/send)
- Multiple conflict resolution strategies
- Session-based conflict management
- Excel file prioritization
- Automatic project state management

---

## Support

For issues or questions:
- **GitHub Issues**: https://github.com/openl-tablets/openl-tablets/issues
- **Documentation**: https://openl-tablets.org
- **API Status**: BETA - Subject to changes in future releases

---

**Note**: This API is currently in BETA status. The interface may change in future releases. Feedback and bug reports are welcome.
