# Projects Merge API - Architecture Design

**Version**: 6.0.0-SNAPSHOT
**Status**: BETA
**Last Updated**: 2026-01-07

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Component Design](#component-design)
3. [Data Flow](#data-flow)
4. [Session Management](#session-management)
5. [State Management](#state-management)
6. [Concurrency & Locking](#concurrency--locking)
7. [Error Handling Strategy](#error-handling-strategy)
8. [Design Decisions](#design-decisions)
9. [Performance Considerations](#performance-considerations)
10. [Security Architecture](#security-architecture)
11. [Testing Strategy](#testing-strategy)
12. [Future Enhancements](#future-enhancements)

---

## Architecture Overview

### System Context

The Projects Merge API is a critical component of the OpenL Tablets Studio that enables Git-based branch merging with conflict detection and resolution capabilities.

``` mermaid
flowchart TB
    %% External clients
    EXT["External Clients<br/>(Web UI, CLI Tools,<br/>CI/CD Pipelines, IDE Plugins)"]

    %% Spring MVC layer
    MVC["Spring MVC Layer<br/>- Request validation (Jakarta Validation)<br/>- Path parameter resolution (@ProjectId)<br/>- Exception handling (@RestControllerAdvice)"]

    %% Controller
    CTRL["ProjectsMergeController (REST)<br/>- Endpoint routing<br/>- Request/Response mapping<br/>- Session coordination<br/>- Project lifecycle management"]

    %% Services
    PMS["ProjectsMergeService<br/>- checkMerge()<br/>- merge()"]
    PMCS["ProjectsMergeConflictsService<br/>- getMergeConflicts()<br/>- resolveConflicts()"]
    WPS["WorkspaceProjectService<br/>- getProjectModel()<br/>- getUserWorkspace()"]

    %% Data Access Layer
    subgraph DAL["Data Access Layer"]
        GIT["Git Repository<br/>(JGit)"]
        WM["Workspace Model"]
        SS["Session Store"]
    end

    %% Connections
    EXT -->|HTTP / REST| MVC
    MVC --> CTRL

    CTRL --> PMS
    CTRL --> PMCS
    CTRL --> WPS

    PMS --> GIT
    PMCS --> GIT

    WPS --> WM
    CTRL --> SS

```

### Layered Architecture

The API follows a strict layered architecture:

``` mermaid
flowchart TB
    %% Presentation Layer
    subgraph PRESENTATION["Presentation Layer"]
        P["ProjectsMergeController<br/>- Request/Response DTOs<br/>- Input validation"]
    end

    %% Service Layer
    subgraph SERVICE["Service Layer"]
        S["Business logic<br/>- Transaction coordination<br/>- Session management"]
    end

    %% Repository Layer
    subgraph REPOSITORY["Repository Layer"]
        R["Git operations<br/>- File system access<br/>- Workspace management"]
    end

    %% Connections
    P --> S
    S --> R

```

**Design Principle**: Each layer depends only on layers below it. No upward dependencies.

---

## Component Design

### 1. ProjectsMergeController

**Location**: `org.openl.studio.projects.rest.controller.ProjectsMergeController`

#### Responsibilities

1. **Request Handling**
   - Expose REST endpoints
   - Validate incoming requests (Jakarta Validation)
   - Map DTOs to service layer models

2. **Session Coordination**
   - Check for existing conflict sessions
   - Store conflict information after detection
   - Clear session data after resolution

3. **Project Lifecycle Management**
   - Freeze/release projects during operations
   - Pause/resume dependency scanning
   - Handle project open/close states

4. **Error Translation**
   - Catch service layer exceptions
   - Map to appropriate HTTP status codes
   - Return user-friendly error messages

#### Key Design Patterns

**Pattern 1: Dependency Injection**
```java
public ProjectsMergeController(
    ProjectsMergeService mergeService,
    WorkspaceProjectService projectService,
    ProjectsMergeConflictsSessionHolder conflictsSessionHolder,
    ProjectsMergeConflictsService mergeConflictsService
) {
    // All dependencies injected via constructor
    // Enables testability and loose coupling
}
```

**Pattern 2: Path Parameter Resolution**
```java
@PostMapping("/check")
public CheckMergeResult check(
    @ProjectId @PathVariable("projectId") RulesProject project,
    // ...
) {
    // @ProjectId annotation resolves string ID to RulesProject object
    // Centralizes project resolution logic
    // Throws 404 automatically if project not found
}
```

**Pattern 3: Session Validation**
```java
private void validateUnresolvedConflict(RulesProject project) {
    var projectId = projectService.resolveProjectId(project);
    if (conflictsSessionHolder.hasConflictInfo(projectId)) {
        throw new ConflictException("project.unresolved.merge.conflicts.message");
    }
}
```

**Pattern 4: Resource Cleanup with Try-Finally**
```java
boolean shouldResumeDependencies = false;
try {
    studio.freezeProject(projectName);
    dependencyManager.pause();

    // Perform merge operation

    if (success) {
        // Success path
    } else {
        shouldResumeDependencies = true;
    }
} catch (Exception e) {
    shouldResumeDependencies = true;
    throw e;
} finally {
    if (shouldResumeDependencies && dependencyManager != null) {
        dependencyManager.resume();
    }
    studio.releaseProject(projectName);
}
```

**Why This Pattern?**
- Ensures resources always released
- Handles both success and failure paths
- Prevents dependency manager deadlocks
- Thread-safe project locking

---

### 2. ProjectsMergeService

**Responsibilities**:
1. Check merge feasibility between branches
2. Execute merge operations
3. Detect merge conflicts
4. Coordinate Git operations

**Key Methods**:

```java
public interface ProjectsMergeService {
    /**
     * Checks if two branches can be merged.
     *
     * @param project Project to check
     * @param otherBranch Target/source branch
     * @param mode Merge direction
     * @return Merge status result
     */
    CheckMergeResult checkMerge(
        RulesProject project,
        String otherBranch,
        MergeOpMode mode
    ) throws IOException;

    /**
     * Performs merge operation.
     *
     * @param project Project to merge
     * @param otherBranch Target/source branch
     * @param mode Merge direction
     * @return Merge result with conflicts if any
     */
    MergeResult merge(
        RulesProject project,
        String otherBranch,
        MergeOpMode mode
    ) throws IOException, ProjectException;
}
```

**Design Considerations**:
- Stateless service (no instance variables)
- Thread-safe operations
- Git operations wrapped in transactions
- Atomic merge operations (all or nothing)

---

### 3. ProjectsMergeConflictsService

**Responsibilities**:
1. Analyze merge conflicts
2. Group conflicts by project
3. Retrieve specific file versions (BASE, OURS, THEIRS)
4. Apply resolution strategies
5. Generate resolution results

**Key Methods**:

```java
public interface ProjectsMergeConflictsService {
    /**
     * Groups conflicted files by project.
     */
    List<ConflictGroup> getMergeConflicts(MergeConflictInfo conflictInfo);

    /**
     * Retrieves a specific version of a conflicted file.
     */
    FileItem getConflictFileItem(
        MergeConflictInfo conflictInfo,
        String filePath,
        ConflictBase side
    ) throws IOException;

    /**
     * Resolves conflicts using specified strategies.
     */
    ResolveConflictsResponse resolveConflicts(
        MergeConflictInfo conflictInfo,
        List<FileConflictResolution> resolutions,
        Map<String, InputStreamSource> customFiles,
        String message
    ) throws IOException, ProjectException;
}
```

**File Priority Algorithm**:

```java
// Excel files appear first (business logic priority)
TreeSet<String> files = new TreeSet<>((f1, f2) -> {
    boolean isExcel1 = FileTypeHelper.isExcelFile(f1);
    boolean isExcel2 = FileTypeHelper.isExcelFile(f2);

    if (isExcel1 && isExcel2) {
        return f1.compareToIgnoreCase(f2);  // Both Excel: alphabetical
    }
    if (isExcel1) return -1;  // Excel first
    if (isExcel2) return 1;   // Non-Excel second

    return f1.compareToIgnoreCase(f2);  // Both non-Excel: alphabetical
});
```

**Why Excel First?**
- Excel files contain business rules (core logic)
- Business logic conflicts are more critical than config/docs
- Users should review and resolve business logic first

---

### 4. ProjectsMergeConflictsSessionHolder

**Responsibilities**:
1. Store conflict information in HTTP session
2. Retrieve conflict information by project ID
3. Remove conflict information after resolution
4. Provide session lifecycle management

**Interface**:

```java
public interface ProjectsMergeConflictsSessionHolder {
    /**
     * Stores conflict info in session.
     */
    void store(String projectId, MergeConflictInfo conflictInfo);

    /**
     * Retrieves conflict info from session.
     */
    MergeConflictInfo getConflictInfo(String projectId);

    /**
     * Checks if conflict info exists.
     */
    boolean hasConflictInfo(String projectId);

    /**
     * Removes conflict info from session.
     */
    void remove(String projectId);
}
```

**Storage Strategy**:

```
Session Attributes:
  Key: "merge.conflicts:{repositoryId}:{projectName}"
  Value: MergeConflictInfo object
  Scope: HTTP Session
  Timeout: Session timeout (default: 30 minutes)
```

**Why Session Storage?**
- **Stateless API**: No server-side state between requests
- **User Isolation**: Each user has independent conflict sessions
- **Automatic Cleanup**: Session timeout handles abandoned operations
- **Scalability**: Works with session replication in clusters

**Alternative Considered**: Redis Cache
- **Pro**: Better for clustered deployments
- **Con**: Additional infrastructure dependency
- **Decision**: Use session for simplicity; can migrate to Redis later

---

### 5. WorkspaceProjectService

**Responsibilities**:
1. Project lookup and resolution
2. Workspace access
3. Project model management
4. WebStudio coordination

**Usage in Merge API**:

```java
// Resolve project ID for session keys
String projectId = projectService.resolveProjectId(project);

// Get project model for dependency management
ProjectModel model = projectService.getProjectModel(project);
WebStudioWorkspaceDependencyManager dm = model.getWebStudioWorkspaceDependencyManager();

// Access workspace for refresh operations
UserWorkspace workspace = projectService.getUserWorkspace();
workspace.refresh();

// Get WebStudio for project freezing
WebStudio studio = projectService.getWebStudio();
studio.freezeProject(projectName);
```

---

## Data Flow

### Flow 1: Check Merge Status

```
┌──────┐                           ┌──────────┐                    ┌─────────┐
│Client│                           │Controller│                    │ Service │
└──┬───┘                           └────┬─────┘                    └────┬────┘
   │                                    │                                │
   │ POST /merge/check                  │                                │
   ├───────────────────────────────────>│                                │
   │                                    │                                │
   │                                    │ validateUnresolvedConflict()   │
   │                                    ├───────────────┐                │
   │                                    │               │                │
   │                                    │<──────────────┘                │
   │                                    │                                │
   │                                    │ checkMerge(project, branch, mode)
   │                                    ├───────────────────────────────>│
   │                                    │                                │
   │                                    │                                │ Git: check merge
   │                                    │                                ├────────────┐
   │                                    │                                │            │
   │                                    │                                │<───────────┘
   │                                    │                                │
   │                                    │        CheckMergeResult        │
   │                                    │<───────────────────────────────┤
   │                                    │                                │
   │         200 OK                     │                                │
   │<───────────────────────────────────┤                                │
   │  {status: "mergeable"}             │                                │
```

---

### Flow 2: Merge with Conflicts

```
┌──────┐         ┌──────────┐         ┌─────────┐         ┌───────┐      ┌─────────┐
│Client│         │Controller│         │MergeServ│         │Session│      │Workspace│
└──┬───┘         └────┬─────┘         └────┬────┘         └───┬───┘      └────┬────┘
   │                  │                     │                  │               │
   │ POST /merge      │                     │                  │               │
   ├─────────────────>│                     │                  │               │
   │                  │                     │                  │               │
   │                  │ freezeProject()     │                  │               │
   │                  ├────────────────────────────────────────────────────────>│
   │                  │                     │                  │               │
   │                  │ merge()             │                  │               │
   │                  ├────────────────────>│                  │               │
   │                  │                     │                  │               │
   │                  │                     │ Git: merge       │               │
   │                  │                     ├──────────┐       │               │
   │                  │                     │          │       │               │
   │                  │                     │<─────────┘       │               │
   │                  │                     │ Conflicts!       │               │
   │                  │                     │                  │               │
   │                  │  MergeResult        │                  │               │
   │                  │  (with conflicts)   │                  │               │
   │                  │<────────────────────┤                  │               │
   │                  │                     │                  │               │
   │                  │ store(conflictInfo) │                  │               │
   │                  ├─────────────────────────────────────────>              │
   │                  │                     │                  │               │
   │                  │ releaseProject()    │                  │               │
   │                  ├────────────────────────────────────────────────────────>│
   │                  │                     │                  │               │
   │  200 OK          │                     │                  │               │
   │<─────────────────┤                     │                  │               │
   │  {status:"conflicts"}                  │                  │               │
```

---

### Flow 3: Resolve Conflicts

```
┌──────┐      ┌──────────┐    ┌──────────┐    ┌───────┐      ┌─────────┐
│Client│      │Controller│    │Conflicts │    │Session│      │Workspace│
│      │      │          │    │Service   │    │       │      │         │
└──┬───┘      └────┬─────┘    └────┬─────┘    └───┬───┘      └────┬────┘
   │               │               │              │               │
   │ POST /resolve │               │              │               │
   ├──────────────>│               │              │               │
   │               │               │              │               │
   │               │ getConflictInfo()            │               │
   │               ├─────────────────────────────>│               │
   │               │               │              │               │
   │               │ MergeConflictInfo            │               │
   │               │<─────────────────────────────┤               │
   │               │               │              │               │
   │               │ resolveConflicts(resolutions)│               │
   │               ├──────────────>│              │               │
   │               │               │              │               │
   │               │               │ Apply resolutions            │
   │               │               │ (BASE/OURS/THEIRS/CUSTOM)    │
   │               │               ├────────┐     │               │
   │               │               │        │     │               │
   │               │               │<───────┘     │               │
   │               │               │              │               │
   │               │               │ Git: commit  │               │
   │               │               ├────────┐     │               │
   │               │               │        │     │               │
   │               │               │<───────┘     │               │
   │               │               │              │               │
   │               │ ResolveConflictsResponse     │               │
   │               │<──────────────┤              │               │
   │               │               │              │               │
   │               │ remove(projectId)            │               │
   │               ├─────────────────────────────>│               │
   │               │               │              │               │
   │               │ workspace.refresh()          │               │
   │               ├─────────────────────────────────────────────>│
   │               │               │              │               │
   │  200 OK       │               │              │               │
   │<──────────────┤               │              │               │
   │  {status:"success"}           │              │               │
```

---

## Session Management

### Session Lifecycle

```
State: NO_SESSION
  │
  │ POST /check OR POST /merge (conflicts detected)
  ▼
State: CONFLICTS_STORED
  │
  ├─── GET /conflicts ──────────┐
  │                             │ (read-only, no state change)
  ├─── GET /conflicts/files ────┤
  │                             │
  ▼                             │
State: CONFLICTS_STORED <───────┘
  │
  ├─── POST /conflicts/resolve (success) ──> State: NO_SESSION
  │
  ├─── POST /conflicts/resolve (failure) ──> State: CONFLICTS_STORED
  │
  └─── DELETE /conflicts ──────────────────> State: NO_SESSION
```

### Session Storage Model

```java
// Session Attribute Structure
sessionAttributes = {
  "merge.conflicts:repo1:ProjectA": MergeConflictInfo {
    sourceBranch: "feature-123",
    targetBranch: "main",
    conflicts: [
      {projectPath: "projects/ProjectA", files: ["rules.xlsx", "config.xml"]}
    ],
    isMerging: true,
    timestamp: "2025-12-18T10:30:00Z",

    // Revision metadata for conflict resolution UI
    oursRevision: {
      branch: "main",
      commit: "abc1234567890def",
      author: "John Doe",
      modifiedAt: "2025-12-18T10:30:00Z",
      exists: true
    },
    theirsRevision: {
      branch: "feature-123",
      commit: "def0987654321abc",
      author: "Jane Smith",
      modifiedAt: "2025-12-17T14:22:00Z",
      exists: true
    },
    baseRevision: {
      branch: "main",
      commit: "base123456789abc",
      author: "John Doe",
      modifiedAt: "2025-12-10T09:00:00Z",
      exists: true
    },
    defaultMessage: "Merge branch 'feature-123' into main"
  },

  "merge.conflicts:repo1:ProjectB": MergeConflictInfo {
    sourceBranch: "main",
    targetBranch: "release-2.0",
    conflicts: [...],
    isMerging: false,
    timestamp: "2025-12-18T09:15:00Z",
    oursRevision: {...},
    theirsRevision: {...},
    baseRevision: {...},
    defaultMessage: "Merge branch 'main' into release-2.0"
  }
}
```

**Key Design Decisions**:

1. **Project-Scoped Keys**: Each project has independent conflict session
   - **Pro**: Multiple projects can have conflicts simultaneously
   - **Pro**: No cross-project interference

2. **Timestamp Tracking**: Record when conflicts were detected
   - **Use**: Debugging stale sessions
   - **Use**: Automatic cleanup of old sessions

3. **Merge Flag**: Track if conflict came from merge or other operation
   - **Use**: Different handling for merge vs. other conflicts
   - **Use**: Affects project locking behavior

### Session Cleanup Strategies

#### 1. Explicit Cleanup

```java
// Success path
POST /conflicts/resolve → Response: success
→ sessionHolder.remove(projectId)
→ Session cleaned

// Cancel path
DELETE /conflicts
→ sessionHolder.remove(projectId)
→ Session cleaned
```

#### 2. Implicit Cleanup

```java
// Session timeout (30 minutes default)
HttpSession timeout → All session attributes cleared
→ Conflict info automatically removed

// User logout
Session invalidated → All session data cleared
```

#### 3. Error Recovery Cleanup

```java
// If project deleted/renamed
→ Session becomes orphaned
→ Manual cleanup or timeout cleanup
→ Next operation on project will fail-fast (404)
```

### Concurrent Session Access

**Scenario**: User opens multiple browser tabs

```
Tab 1: POST /merge → Conflicts detected → Session stored
Tab 2: POST /merge → Throws ConflictException (unresolved conflicts exist)
```

**Protection Mechanism**:
```java
private void validateUnresolvedConflict(RulesProject project) {
    if (conflictsSessionHolder.hasConflictInfo(projectId)) {
        throw new ConflictException("project.unresolved.merge.conflicts.message");
    }
}
```

**Why?**
- Prevents concurrent merge operations on same project
- Ensures single source of truth for conflict state
- User must resolve or cancel before new operation

---

## State Management

### Project State Transitions

```
┌─────────────┐
│   OPENED    │ ← Project is open in workspace
└──────┬──────┘
       │
       │ merge() called
       ▼
┌─────────────┐
│   FROZEN    │ ← Project locked, no other operations allowed
└──────┬──────┘
       │
       │ merge operation
       ├─── Success ────┐
       │                │
       ▼                ▼
┌─────────────┐   ┌─────────────┐
│   CLOSED    │   │  CONFLICTS  │
└──────┬──────┘   └──────┬──────┘
       │                 │
       │                 │ resolveConflicts()
       │                 ├─── Success ───┐
       │                 │                │
       ▼                 ▼                ▼
┌─────────────┐   ┌─────────────┐  ┌─────────────┐
│   OPENED    │   │   CLOSED    │  │   CLOSED    │
│  (refreshed)│   └──────┬──────┘  └──────┬──────┘
└─────────────┘          │                │
                         ▼                ▼
                  ┌─────────────┐   ┌─────────────┐
                  │   OPENED    │   │   OPENED    │
                  │  (refreshed)│   │  (refreshed)│
                  └─────────────┘   └─────────────┘
```

### Dependency Manager States

```
┌──────────────┐
│   RUNNING    │ ← Scanning workspace for dependencies
└──────┬───────┘
       │
       │ merge() called
       ▼
┌──────────────┐
│    PAUSED    │ ← Suspended during merge
└──────┬───────┘
       │
       │ merge complete/failed
       ▼
┌──────────────┐
│   RUNNING    │ ← Resumed after operation
└──────────────┘
```

**Why Pause Dependencies?**
- Prevents workspace scanning during merge
- Avoids race conditions with file system changes
- Ensures consistent project state
- Reduces resource contention

### WebStudio State

```
┌──────────────┐
│    NORMAL    │
└──────┬───────┘
       │
       │ freezeProject()
       ▼
┌──────────────┐
│ PROJECT_FROZEN│ ← Project locked in WebStudio
└──────┬───────┘
       │
       │ ... operation ...
       │
       │ releaseProject() (in finally block)
       ▼
┌──────────────┐
│    NORMAL    │
└──────────────┘
```

**Guarantees**:
- Always released (finally block)
- Even on exceptions
- Thread-safe locking

---

## Concurrency & Locking

### Lock Hierarchy

```
Level 1: WebStudio Project Lock
  │
  ├─> Level 2: Workspace Lock
  │     │
  │     └─> Level 3: Git Repository Lock
  │           │
  │           └─> Level 4: File System Lock
  │
  └─> Prevents deadlocks by consistent ordering
```

### Lock Acquisition Order

**Correct Order** (implemented):
```java
// 1. WebStudio lock (highest level)
studio.freezeProject(projectName);

try {
  // 2. Pause workspace scanning
  dependencyManager.pause();

  // 3. Git operations (inside merge service)
  git.checkout(...);
  git.merge(...);

  // 4. File system operations
  Files.write(...);

} finally {
  // Release in reverse order
  dependencyManager.resume();
  studio.releaseProject(projectName);
}
```

**Why This Order?**
- Highest-level locks acquired first
- Prevents circular wait (deadlock condition)
- Ensures consistent lock ordering across all operations

### Concurrency Scenarios

#### Scenario 1: Concurrent Merges on Same Project

```
Thread 1: POST /merge (ProjectA)
  → studio.freezeProject("ProjectA") ✓
  → Performing merge...

Thread 2: POST /merge (ProjectA)
  → validateUnresolvedConflict() → throws ConflictException ✗
  → Blocked by validation check

Result: Thread 2 fails fast, no deadlock
```

#### Scenario 2: Concurrent Merges on Different Projects

```
Thread 1: POST /merge (ProjectA)
  → studio.freezeProject("ProjectA") ✓
  → Performing merge...

Thread 2: POST /merge (ProjectB)
  → studio.freezeProject("ProjectB") ✓
  → Performing merge...

Result: Both succeed independently
```

#### Scenario 3: Merge + Other Operation

```
Thread 1: POST /merge (ProjectA)
  → studio.freezeProject("ProjectA") ✓
  → Performing merge...

Thread 2: PUT /projects/ProjectA/tables/DataTable
  → Attempt to modify project
  → Blocked by WebStudio lock
  → Waits until merge completes

Result: Operations serialized correctly
```

### Resource Cleanup on Failure

```java
boolean shouldResumeDependencies = false;

try {
    studio.freezeProject(projectName);
    dependencyManager.pause();

    // ... merge operation ...

    if (mergeFailed) {
        shouldResumeDependencies = true;
    }

} catch (Exception e) {
    shouldResumeDependencies = true;
    throw e;

} finally {
    // Always resume if needed
    if (shouldResumeDependencies && dependencyManager != null) {
        dependencyManager.resume();
    }

    // Always release project lock
    studio.releaseProject(projectName);
}
```

**Why `shouldResumeDependencies` Flag?**
- Only resume if merge didn't complete successfully
- If merge succeeded, normal workspace refresh handles dependencies
- Prevents duplicate dependency scans
- Optimizes performance

---

## Error Handling Strategy

### Exception Hierarchy

```
Exception
  │
  ├─ RuntimeException
  │   │
  │   ├─ BadRequestException (400)
  │   │   └─ Invalid input, missing required fields
  │   │
  │   ├─ NotFoundException (404)
  │   │   └─ Project not found, conflict info not found
  │   │
  │   └─ ConflictException (409)
  │       └─ Unresolved conflicts, branches not mergeable
  │
  └─ ProjectException (500)
      └─ Git operation failed, file system error
```

### Error Response Mapping

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequest(BadRequestException ex) {
        return new ErrorResponse(
            400,
            "Bad Request",
            ex.getMessage(),
            request.getRequestURI()
        );
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(NotFoundException ex) {
        return new ErrorResponse(
            404,
            "Not Found",
            ex.getMessage(),
            request.getRequestURI()
        );
    }

    // ... other handlers
}
```

### Error Scenarios & Recovery

#### 1. Unresolved Conflicts Exist

**Trigger**:
```java
POST /merge/check
→ validateUnresolvedConflict()
→ sessionHolder.hasConflictInfo(projectId) == true
→ throw new ConflictException(...)
```

**Response**:
```json
{
  "status": 409,
  "error": "Conflict",
  "message": "project.unresolved.merge.conflicts.message"
}
```

**Recovery Options**:
1. `POST /conflicts/resolve` - Resolve conflicts
2. `DELETE /conflicts` - Cancel and start over

---

#### 2. No Conflict Information Found

**Trigger**:
```java
GET /conflicts
→ !sessionHolder.hasConflictInfo(projectId)
→ throw new NotFoundException(...)
```

**Response**:
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "project.merge.result.not.found.message"
}
```

**Recovery**:
- Perform a merge operation first
- Check if session expired (re-authenticate if needed)

---

#### 3. Custom File Missing

**Trigger**:
```java
POST /conflicts/resolve
resolutions=[{filePath:"rules.xml", strategy:"CUSTOM"}]
→ resolution.file() == null
→ throw new BadRequestException(...)
```

**Response**:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "project.merge.conflict.custom.file.missing.message",
  "args": ["rules.xml"]
}
```

**Recovery**:
- Upload the custom file in multipart request
- Change strategy to BASE/OURS/THEIRS

---

#### 4. Git Operation Failed

**Trigger**:
```java
POST /merge
→ mergeService.merge(...)
→ git.merge() throws IOException
→ throw new ProjectException(...)
```

**Response**:
```json
{
  "status": 500,
  "error": "Internal Server Error",
  "message": "Git operation failed: [details]"
}
```

**Recovery**:
- Check Git repository health
- Check network connectivity (if remote)
- Retry operation
- Contact administrator if persistent

---

### Transaction Rollback Strategy

```java
// Git operations are atomic by design
try {
    git.checkout(branch);
    git.merge(otherBranch);

    if (conflicts detected) {
        git.reset(--merge);  // Rollback to pre-merge state
        return MergeResult.withConflicts(...);
    }

    git.commit();  // Commit only if successful

} catch (Exception e) {
    git.reset(--hard, HEAD);  // Emergency rollback
    throw new ProjectException("Merge failed", e);
}
```

**Guarantees**:
- No partial merges committed
- Repository always in consistent state
- Can retry failed operations safely

---

## Design Decisions

### Decision 1: Session Storage vs. Database

**Problem**: Where to store conflict information between requests?

**Options Considered**:

| Option | Pros | Cons | Decision |
|--------|------|------|----------|
| **HTTP Session** | Simple, no extra infrastructure, automatic cleanup | Not suitable for clusters without session replication | ✅ **Selected** |
| **Database** | Persistent, cluster-friendly, queryable | Requires schema, manual cleanup, performance overhead | ❌ Rejected |
| **Redis Cache** | Fast, cluster-friendly, TTL support | Extra infrastructure, dependency | ⏸️ Future |

**Rationale**:
- Most OpenL deployments are single-server
- Session timeout provides automatic cleanup
- Simpler architecture for initial implementation
- Can migrate to Redis in future if clustering needed

---

### Decision 2: Bidirectional Merge Modes (RECEIVE/SEND)

**Problem**: How should users specify merge direction?

**Options Considered**:

| Option | Example | Pros | Cons | Decision |
|--------|---------|------|------|----------|
| **Explicit Source/Target** | `{source:"main", target:"feature"}` | Clear, unambiguous | Verbose, requires knowing both branches | ❌ Rejected |
| **Directional Mode** | `{mode:"receive", otherBranch:"feature"}` | Intuitive, context-aware | Requires understanding modes | ✅ **Selected** |
| **Git-style** | `{from:"feature", to:"main"}` | Familiar to Git users | Confusing for non-Git users | ❌ Rejected |

**Rationale**:
- Users typically think in terms of "pulling changes in" or "pushing changes out"
- Current branch is implicit context
- Fewer parameters required
- More intuitive for non-technical users

**Example**:
```json
// Receive: Merge feature-auth INTO current branch (main)
{"mode": "receive", "otherBranch": "feature-auth"}

// Send: Merge current branch (main) INTO release-2.0
{"mode": "send", "otherBranch": "release-2.0"}
```

---

### Decision 3: Excel File Prioritization

**Problem**: In what order should conflicted files be presented?

**Options Considered**:

| Option | Pros | Cons | Decision |
|--------|------|------|----------|
| **Alphabetical** | Simple, predictable | Doesn't prioritize important files | ❌ Rejected |
| **File Type Priority** | Business logic first | Adds complexity | ✅ **Selected** |
| **User-Configurable** | Flexible | Too complex for initial version | ⏸️ Future |

**Rationale**:
- Excel files contain business rules (core value of OpenL)
- Business logic conflicts should be reviewed first
- Configuration and documentation conflicts are less critical
- Helps users focus on high-priority conflicts

**Implementation**:
```java
TreeSet<String> files = new TreeSet<>((f1, f2) -> {
    boolean isExcel1 = FileTypeHelper.isExcelFile(f1);
    boolean isExcel2 = FileTypeHelper.isExcelFile(f2);

    if (isExcel1 && isExcel2) return f1.compareToIgnoreCase(f2);
    if (isExcel1) return -1;  // Excel files first
    if (isExcel2) return 1;

    return f1.compareToIgnoreCase(f2);
});
```

---

### Decision 4: Multipart Form Data for Resolution

**Problem**: How to support custom file uploads for conflict resolution?

**Options Considered**:

| Option | Pros | Cons | Decision |
|--------|------|------|----------|
| **Separate Endpoints** | RESTful, clear separation | Multiple requests, complex flow | ❌ Rejected |
| **Base64 in JSON** | Single request, simple | Size limit, encoding overhead | ❌ Rejected |
| **Multipart Form Data** | Standard, efficient, supports large files | Mixed JSON + binary | ✅ **Selected** |

**Rationale**:
- Standard HTTP multipart format
- Efficient for binary files (no base64 overhead)
- Supports large Excel files
- Single atomic request for all resolutions

**Example Request**:
```bash
POST /conflicts/resolve
Content-Type: multipart/form-data; boundary=----WebKitFormBoundary

------WebKitFormBoundary
Content-Disposition: form-data; name="resolutions"

[{"filePath":"rules.xml","strategy":"CUSTOM"}]
------WebKitFormBoundary
Content-Disposition: form-data; name="file"; filename="merged-rules.xml"
Content-Type: application/xml

<xml content here>
------WebKitFormBoundary--
```

---

### Decision 5: Atomic Conflict Resolution

**Problem**: What happens if some conflicts resolve successfully but others fail?

**Options Considered**:

| Option | Behavior | Pros | Cons | Decision |
|--------|----------|------|------|----------|
| **Partial Success** | Apply successful resolutions, report failures | Incremental progress | Inconsistent state, complex retry | ❌ Rejected |
| **All-or-Nothing** | All succeed or all fail | Consistent state, simple retry | Must fix all issues before retry | ✅ **Selected** |

**Rationale**:
- Ensures repository always in consistent state
- Simpler error handling and recovery
- Clear success/failure criteria
- Users can fix issues and retry entire operation

**Implementation**:
```java
public ResolveConflictsResponse resolveConflicts(...) {
    // Validate ALL resolutions first
    for (FileConflictResolution resolution : resolutions) {
        validateResolution(resolution);
    }

    // Apply ALL resolutions (or rollback)
    try {
        for (FileConflictResolution resolution : resolutions) {
            applyResolution(resolution);
        }

        git.commit();  // Commit only if all succeed

        return ResolveConflictsResponse.success(resolvedFiles);

    } catch (Exception e) {
        git.reset(--hard, HEAD);  // Rollback all changes
        throw new ProjectException("Resolution failed", e);
    }
}
```

---

## Performance Considerations

### 1. Git Operation Optimization

**Challenge**: Git operations can be slow for large repositories

**Optimizations**:

```java
// Use shallow clone for merge checks
git.clone()
   .setDepth(1)  // Only fetch recent history
   .setBranchesToClone(Arrays.asList(sourceBranch, targetBranch))
   .call();

// Fetch only required refs
git.fetch()
   .setRefSpecs(new RefSpec("+refs/heads/" + branch + ":refs/heads/" + branch))
   .call();

// Use diff algorithm for conflict detection
git.diff()
   .setOldTree(baseTree)
   .setNewTree(mergeTree)
   .setShowNameAndStatusOnly(true)  // Don't load full content
   .call();
```

---

### 2. Session Storage Size

**Challenge**: Large projects may have many conflicted files

**Mitigation**:

```java
// Store minimal conflict information
public class MergeConflictInfo {
    private String sourceBranch;    // Small
    private String targetBranch;    // Small
    private List<String> files;     // File paths only, not content
    private transient GitObject;    // Not serialized to session

    // Don't store:
    // - File contents (retrieve on demand)
    // - Full diff information
    // - Git commit objects
}
```

**Session Size Estimate**:
- Project with 100 conflicts: ~10 KB
- Acceptable for HTTP session storage
- Can support dozens of concurrent conflict sessions

---

### 3. Workspace Refresh Optimization

**Challenge**: Workspace refresh can be expensive

**Optimization**:

```java
// Only refresh after successful merge
if (mergeResult.status() == MergeResultStatus.SUCCESS) {
    workspace.refresh();  // Full refresh
} else {
    // No refresh needed (no changes committed)
}

// Pause dependency manager during refresh
dependencyManager.pause();
workspace.refresh();
dependencyManager.resume();  // Single scan after refresh
```

---

### 4. File Download Optimization

**Challenge**: Downloading large Excel files can be slow

**Optimizations**:

```java
// Stream file content (don't load into memory)
@GetMapping("/conflicts/files")
public ResponseEntity<byte[]> getConflictedFile(...) {
    FileItem fileItem = mergeConflictsService.getConflictFileItem(...);

    // Stream directly from Git to response
    try (InputStream stream = fileItem.getStream()) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        stream.transferTo(output);

        return ResponseEntity.ok()
            .contentType(determineMediaType(fileName))
            .body(output.toByteArray());
    }
}

// Use appropriate buffer sizes for large files
StreamUtils.copy(input, output, 8192);  // 8KB buffer
```

---

### 5. Concurrent Request Handling

**Challenge**: Multiple users performing merges simultaneously

**Optimizations**:

```java
// Project-level locking (not global)
studio.freezeProject(projectName);  // Only locks this project

// Other projects can merge concurrently
Thread 1: Merging ProjectA  ✓
Thread 2: Merging ProjectB  ✓
Thread 3: Merging ProjectC  ✓

// No global lock bottleneck
```

---

## Security Architecture

### 1. Authentication & Authorization

```java
@RestController
@RequestMapping("/projects/{projectId}/merge")
public class ProjectsMergeController {

    // Spring Security integration
    @PreAuthorize("hasPermission(#project, 'WRITE')")
    @PostMapping
    public MergeResultResponse merge(
        @ProjectId @PathVariable("projectId") RulesProject project,
        ...
    ) {
        // User must have WRITE permission on project
    }
}
```

**Permissions Required**:
- `READ`: Get conflicts, download conflict files
- `WRITE`: Check merge, perform merge, resolve conflicts

---

### 2. Input Validation

```java
// Jakarta Validation annotations
public record MergeRequest(
    @NotNull MergeOpMode mode,
    @NotBlank String otherBranch
) {}

// Custom validation in controller
private void validateBranchName(String branchName) {
    if (branchName.contains("..") || branchName.contains("/./")) {
        throw new BadRequestException("Invalid branch name");
    }
}

// Validate file paths
private void validateFilePath(String filePath) {
    if (filePath.startsWith("/") || filePath.contains("..")) {
        throw new BadRequestException("Invalid file path");
    }
}
```

---

### 3. Path Traversal Prevention

```java
// Ensure file path is within project directory
public FileItem getConflictFileItem(
    MergeConflictInfo conflictInfo,
    String filePath,
    ConflictBase side
) {
    // Normalize path
    Path normalizedPath = Paths.get(filePath).normalize();

    // Ensure within project
    Path projectPath = Paths.get(project.getRealPath());
    Path resolvedPath = projectPath.resolve(normalizedPath);

    if (!resolvedPath.startsWith(projectPath)) {
        throw new BadRequestException("Invalid file path");
    }

    // ...
}
```

---

### 4. File Upload Security

```java
// Limit file size
@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResolveConflictsResponse resolveConflicts(
    @RequestParam("file") MultipartFile file
) {
    // Check file size
    if (file.getSize() > MAX_UPLOAD_SIZE) {
        throw new BadRequestException("File too large");
    }

    // Validate file type
    String contentType = file.getContentType();
    if (!isAllowedContentType(contentType)) {
        throw new BadRequestException("Invalid file type");
    }

    // Scan for malware (if configured)
    if (antivirusEnabled) {
        scanFile(file);
    }
}
```

---

### 5. Session Security

```java
// Session fixation prevention
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) {
        http
            .sessionManagement()
            .sessionFixation().newSession()  // New session on auth
            .maximumSessions(1)              // One session per user
            .maxSessionsPreventsLogin(false);
    }
}

// CSRF protection
@PostMapping
@CsrfProtection  // Require CSRF token for state-changing operations
public MergeResultResponse merge(...) {
    // ...
}
```

---

### 6. Audit Logging

```java
@Aspect
@Component
public class MergeAuditAspect {

    @AfterReturning("@annotation(PostMapping)")
    public void logMergeOperation(JoinPoint joinPoint) {
        String username = SecurityContextHolder.getContext()
            .getAuthentication().getName();

        String operation = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        auditLog.info("User {} performed {} on project {}",
            username, operation, extractProjectId(args));
    }

    @AfterThrowing(pointcut = "@annotation(PostMapping)", throwing = "ex")
    public void logMergeFailure(JoinPoint joinPoint, Exception ex) {
        auditLog.error("Merge operation failed: {}", ex.getMessage());
    }
}
```

**Audit Events**:
- Merge check performed
- Merge initiated
- Conflicts detected
- Conflicts resolved
- Merge canceled
- Failures and errors

---

## Testing Strategy

### 1. Unit Tests

**ProjectsMergeControllerTest**:
```java
@WebMvcTest(ProjectsMergeController.class)
class ProjectsMergeControllerTest {

    @MockBean
    private ProjectsMergeService mergeService;

    @MockBean
    private ProjectsMergeConflictsService conflictsService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testCheckMerge_Success() throws Exception {
        // Given
        CheckMergeResult result = CheckMergeResult.builder()
            .sourceBranch("feature")
            .targetBranch("main")
            .status(CheckMergeStatus.MERGEABLE)
            .build();

        when(mergeService.checkMerge(any(), any(), any()))
            .thenReturn(result);

        // When/Then
        mockMvc.perform(post("/projects/MyProject/merge/check")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mode\":\"receive\",\"otherBranch\":\"feature\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("mergeable"));
    }

    @Test
    void testMerge_WithConflicts() {
        // Test merge operation that detects conflicts
        // Verify session storage
        // Verify response structure
    }

    @Test
    void testResolveConflicts_AllStrategies() {
        // Test BASE, OURS, THEIRS, CUSTOM strategies
        // Verify file uploads
        // Verify session cleanup
    }
}
```

---

### 2. Integration Tests

**ProjectsMergeIntegrationTest**:
```java
@SpringBootTest
@AutoConfigureMockMvc
class ProjectsMergeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProjectsMergeConflictsSessionHolder sessionHolder;

    @Test
    @WithMockUser(username = "testuser", roles = "PROJECT_EDITOR")
    void testCompleteWorkflow_MergeWithConflicts() throws Exception {
        // Step 1: Perform merge (expect conflicts)
        MvcResult mergeResult = mockMvc.perform(
                post("/projects/TestProject/merge")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"mode\":\"receive\",\"otherBranch\":\"feature\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("conflicts"))
            .andReturn();

        // Verify session storage
        assertTrue(sessionHolder.hasConflictInfo("repo:TestProject"));

        // Step 2: Get conflicts
        mockMvc.perform(get("/projects/TestProject/merge/conflicts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].files").isArray());

        // Step 3: Download conflict file
        mockMvc.perform(get("/projects/TestProject/merge/conflicts/files")
                .param("file", "rules.xlsx")
                .param("side", "OURS"))
            .andExpect(status().isOk())
            .andExpect(header().exists(HttpHeaders.CONTENT_DISPOSITION));

        // Step 4: Resolve conflicts
        mockMvc.perform(multipart("/projects/TestProject/merge/conflicts/resolve")
                .param("resolutions", "[{\"filePath\":\"rules.xlsx\",\"strategy\":\"OURS\"}]")
                .param("message", "Test resolution"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"));

        // Verify session cleanup
        assertFalse(sessionHolder.hasConflictInfo("repo:TestProject"));
    }
}
```

---

### 3. Git Integration Tests

**MergeServiceGitTest**:
```java
@ExtendWith(TempDirectoryExtension.class)
class MergeServiceGitTest {

    private Path tempDir;
    private Git git;

    @BeforeEach
    void setupGitRepo(@TempDir Path tempDir) {
        this.tempDir = tempDir;
        this.git = Git.init().setDirectory(tempDir.toFile()).call();

        // Create initial commit
        createFile("README.md", "Initial");
        commit("Initial commit");

        // Create branches
        git.branchCreate().setName("feature").call();
    }

    @Test
    void testMerge_NoConflicts() throws Exception {
        // Create change in feature branch
        git.checkout().setName("feature").call();
        createFile("feature.txt", "Feature content");
        commit("Add feature");

        // Merge into main
        git.checkout().setName("main").call();
        MergeResult result = mergeService.merge(project, "feature", RECEIVE);

        // Assert
        assertEquals(MergeResultStatus.SUCCESS, result.status());
        assertTrue(Files.exists(tempDir.resolve("feature.txt")));
    }

    @Test
    void testMerge_WithConflicts() throws Exception {
        // Create conflicting changes
        createFile("rules.txt", "Main version");
        commit("Main changes");

        git.checkout().setName("feature").call();
        git.reset().setMode(HARD).setRef("HEAD~1").call();
        createFile("rules.txt", "Feature version");
        commit("Feature changes");

        // Merge into main
        git.checkout().setName("main").call();
        MergeResult result = mergeService.merge(project, "feature", RECEIVE);

        // Assert
        assertEquals(MergeResultStatus.CONFLICTS, result.status());
        assertNotNull(result.conflictInfo());
        assertTrue(result.conflictInfo().getFiles().contains("rules.txt"));
    }
}
```

---

### 4. Performance Tests

```java
@SpringBootTest
class MergePerformanceTest {

    @Test
    void testMerge_LargeRepository() {
        // Given: Repository with 1000 files
        createLargeRepository(1000);

        // When: Perform merge
        long startTime = System.currentTimeMillis();
        MergeResult result = mergeService.merge(project, "feature", RECEIVE);
        long duration = System.currentTimeMillis() - startTime;

        // Then: Should complete in reasonable time
        assertTrue(duration < 5000, "Merge took " + duration + "ms");
    }

    @Test
    void testConcurrentMerges() throws Exception {
        // Given: 10 different projects
        List<RulesProject> projects = createProjects(10);

        // When: Merge all concurrently
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Future<MergeResult>> futures = projects.stream()
            .map(project -> executor.submit(() ->
                mergeService.merge(project, "feature", RECEIVE)))
            .collect(Collectors.toList());

        // Then: All should succeed
        for (Future<MergeResult> future : futures) {
            MergeResult result = future.get();
            assertNotNull(result);
        }

        executor.shutdown();
    }
}
```

---

## Future Enhancements

### 1. Three-Way Merge Visualization

**Current**: Users download BASE, OURS, THEIRS separately

**Future**: Provide visual diff interface

```json
GET /conflicts/diff?file=rules.xlsx&format=visual

Response:
{
  "file": "rules.xlsx",
  "sections": [
    {
      "line": 10,
      "base": "value1",
      "ours": "value2",
      "theirs": "value3",
      "conflict": true
    }
  ]
}
```

---

### 2. Automatic Conflict Resolution

**Current**: User must manually select strategy

**Future**: AI-powered auto-resolution

```java
public interface AutoMergeStrategy {
    /**
     * Attempts to automatically resolve conflicts.
     *
     * @return Recommended resolutions, or empty if cannot auto-resolve
     */
    List<FileConflictResolution> autoResolve(MergeConflictInfo conflicts);
}

// Implementation examples:
// - Trivial merges (whitespace, formatting)
// - Non-overlapping changes
// - Prefer newer/older consistently
// - ML-based resolution learning
```

---

### 3. Merge Preview

**Current**: Must perform actual merge to see conflicts

**Future**: Dry-run mode

```json
POST /merge/preview
{
  "mode": "receive",
  "otherBranch": "feature"
}

Response:
{
  "wouldSucceed": false,
  "conflicts": [...],
  "changedFiles": 15,
  "additions": 234,
  "deletions": 45
}
```

---

### 4. Redis-Based Session Storage

**Current**: HTTP session storage

**Future**: Redis for clustering

```java
@Configuration
@EnableRedisRepositories
public class RedisSessionConfig {

    @Bean
    public ProjectsMergeConflictsSessionHolder sessionHolder(
        RedisTemplate<String, MergeConflictInfo> redisTemplate
    ) {
        return new RedisConflictsSessionHolder(redisTemplate);
    }
}

// Benefits:
// - Supports clustered deployments
// - Session replication across servers
// - Better performance for large sessions
// - TTL-based automatic cleanup
```

---

### 5. Webhook Integration

**Future**: Notify external systems of merge events

```java
@Component
public class MergeEventPublisher {

    @EventListener
    public void onMergeComplete(MergeCompleteEvent event) {
        webhookService.send(
            WebhookEvent.builder()
                .type("merge.completed")
                .project(event.getProjectName())
                .branch(event.getBranch())
                .timestamp(Instant.now())
                .build()
        );
    }
}
```

**Use Cases**:
- Trigger CI/CD pipelines
- Send notifications (Slack, email)
- Update external tracking systems
- Audit logging to SIEM

---

### 6. Batch Merge Operations

**Future**: Merge multiple branches in one operation

```json
POST /merge/batch
{
  "operations": [
    {"mode": "receive", "otherBranch": "feature-1"},
    {"mode": "receive", "otherBranch": "feature-2"},
    {"mode": "send", "otherBranch": "release-2.0"}
  ]
}

Response:
{
  "results": [
    {"branch": "feature-1", "status": "success"},
    {"branch": "feature-2", "status": "conflicts", "conflicts": [...]},
    {"branch": "release-2.0", "status": "success"}
  ]
}
```

---

### 7. Merge History & Rollback

**Future**: Track merge history and support rollback

```json
GET /merge/history

Response:
{
  "merges": [
    {
      "timestamp": "2025-12-18T10:30:00Z",
      "sourceBranch": "feature-auth",
      "targetBranch": "main",
      "user": "john.doe",
      "commitSha": "abc123",
      "canRollback": true
    }
  ]
}

POST /merge/rollback/{commitSha}
→ Reverts merge commit
```

---

## Conclusion

The Projects Merge API provides a robust, secure, and user-friendly interface for managing Git branch merges in OpenL Tablets. Key architectural strengths:

1. **Layered Architecture**: Clear separation of concerns
2. **Session Management**: Stateful conflict tracking with automatic cleanup
3. **Concurrency Control**: Safe multi-user operations
4. **Error Handling**: Comprehensive error recovery
5. **Performance**: Optimized Git operations and workspace management
6. **Security**: Input validation, permission checks, audit logging
7. **Extensibility**: Designed for future enhancements

The API is currently in BETA status and will evolve based on user feedback and real-world usage patterns.

---

## References

- **API Documentation**: [projects-merge-api.md](projects-merge-api.md)
- **Source Code**: `/STUDIO/org.openl.rules.webstudio/src/org/openl/studio/projects/rest/controller/ProjectsMergeController.java`
- **Integration Tests**: `/ITEST/itest.studio/src/test/java/.../ProjectsMergeControllerTest.java`
- **Git Documentation**: https://git-scm.com/docs
- **JGit Documentation**: https://www.eclipse.org/jgit/documentation/

---

**Last Updated**: 2026-01-07
**Version**: 6.0.0-SNAPSHOT
**Status**: BETA
