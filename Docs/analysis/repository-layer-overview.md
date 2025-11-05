# Repository & Workspace Layer Overview

**Module Group**: STUDIO (Repository & Workspace modules)
**Location**: `/home/user/openl-tablets/STUDIO/`
**Last Updated**: 2025-11-05

---

## Executive Summary

The Repository & Workspace layer provides a comprehensive abstraction for managing OpenL rules projects across multiple storage backends with version control, branching, conflict resolution, and workspace management. This layer supports file systems, Git, AWS S3, Azure Blob Storage, and database backends with a unified API.

**Key Capabilities**:
- Multi-backend storage abstraction
- Full Git integration with branching and merging
- Cloud storage (AWS S3, Azure Blob)
- Excel file conflict detection and merge
- JSON serialization with type safety
- Workspace organization and management

---

## Modules Overview

| Module | Files | Purpose | Complexity |
|--------|-------|---------|------------|
| `org.openl.rules.repository` | 55 | Core repository API + implementations | 🟡 Medium |
| `org.openl.rules.repository.git` | 26 | Git backend (JGit) | 🔴 High |
| `org.openl.rules.repository.aws` | 4 | AWS S3 backend | 🟢 Low |
| `org.openl.rules.repository.azure` | 4 | Azure Blob backend | 🟢 Low |
| `org.openl.rules.workspace` | Multiple | Workspace management | 🟡 Medium |
| `org.openl.rules.diff` | Multiple | Table/rule comparison | 🟡 Medium |
| `org.openl.rules.jackson` | 25 | JSON serialization | 🟡 Medium |
| `org.openl.rules.jackson.configuration` | 2 | Jackson annotations | 🟢 Low |
| `org.openl.rules.xls.merge` | Multiple | Excel 3-way merge | 🔴 High |

---

## 1. Core Repository Module

**Location**: `/home/user/openl-tablets/STUDIO/org.openl.rules.repository/`
**Purpose**: Foundation abstraction for all storage backends

### Architecture

```
┌─────────────────────────────────────┐
│     Repository (API)                │
├─────────────────────────────────────┤
│  - list(), check(), read()          │
│  - save(), delete()                 │
│  - listHistory(), copyHistory()     │
│  - Features, Listener               │
└─────────────────────────────────────┘
         ↑                  ↑
         │                  │
┌────────┴────────┐  ┌──────┴──────────┐
│ BranchRepository│  │SearchableRepo   │
└─────────────────┘  └─────────────────┘
         ↑                  ↑
         │                  │
    ┌────┴──────────────────┴────┐
    │   GitRepository            │
    └────────────────────────────┘
```

### Core Interface: `Repository`

**Location**: `org.openl.rules.repository.api.Repository` (261 lines)

```java
public interface Repository {
    // File operations
    List<FileData> list(String path);
    FileData check(String name);
    FileItem read(String name);
    FileData save(FileData data, InputStream stream);
    boolean delete(FileData data);

    // History
    List<FileData> listHistory(String name);
    FileData copyHistory(String name, FileData data, String version);

    // Metadata
    Features supports();
    String getName();
    void setListener(Listener callback);

    // Lifecycle
    void close();
}
```

**Key Concepts**:
- **Path-based API**: All operations use string paths
- **Atomic operations**: Each save/delete is atomic
- **Version support**: History operations if backend supports versioning
- **Feature detection**: `supports()` returns capability flags
- **Change notifications**: Listener callback for file changes

### Extended Interfaces

**`BranchRepository`** - Adds branching support:
```java
public interface BranchRepository extends Repository, SearchableRepository {
    // Branch management
    String getBranch();
    List<String> getBranches(String path);
    void createBranch(String projectPath, String branch);
    void deleteBranch(String projectPath, String branch);
    BranchRepository forBranch(String branch);
    boolean isBranchProtected(String branch);

    // Merge operations
    void merge(String branchFrom, UserInfo author, ConflictResolveData data);
    void pull(UserInfo author);
    boolean isMergedInto(String from, String to);
}
```

**`SearchableRepository`** - Adds advanced search:
```java
public interface SearchableRepository {
    Page<FileData> listHistory(String name,
                                String globalFilter,
                                boolean techRevs,
                                Pageable pageable);
}
```

### Data Classes

**`FileData`** - File metadata container:
```java
public class FileData {
    private String name;              // File path
    private long size;                // Size in bytes
    private Date modifiedAt;          // Modification timestamp
    private String version;           // Version/commit ID
    private UserInfo author;          // Last modifier
    private String comment;           // Commit message
    private String branch;            // Branch name
    private String uniqueId;          // Backend-specific ID
    private boolean technicalRevision; // Technical commit flag
    private Map<String, Object> additionalData; // Extensible data
}
```

**`FileItem`** - File with content stream:
```java
public class FileItem implements Closeable {
    private final FileData data;
    private final InputStream stream;

    public FileItem(FileData data, InputStream stream) {
        this.data = data;
        this.stream = stream;
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }
}
```

**`Features`** - Repository capabilities:
```java
public class Features {
    boolean folders();          // Folder support
    boolean branches();         // Branch support
    boolean versions();         // Versioning support
    boolean mappedFolders();    // Folder mapping
    boolean uniqueFileId();     // Unique file IDs
    boolean isLocal();          // Local filesystem
    boolean searchable();       // Advanced search
}
```

### Implementation: File System Repository

**Location**: `org.openl.rules.repository.file.FileSystemRepository`

**Features**:
```
folders: true
branches: false
versions: false
uniqueFileId: false
local: true
searchable: false
```

**Key Classes**:
- `FileSystemRepository` - Main implementation
- `LocalRepository` - Alias for FileSystemRepository
- `FileChangesMonitor` - Detects filesystem changes
- `LocalRepositoryFactory` - Factory (ID: `"repo-file"`)

**Configuration**:
```properties
repo-file.factory = repo-file
repo-file.uri = ${openl.home}/repositories/local
repo-file.listener-timer-period = 10
```

### Implementation: Database Repository

**Location**: `org.openl.rules.repository.db.DBRepository`

**Backends**:
- JDBC URL-based: `JdbcDBRepositoryFactory` (ID: `"repo-jdbc"`)
- JNDI Datasource: `DatasourceDBRepositoryFactory` (ID: `"repo-jndi"`)

**Supported Databases**:
- H2, MySQL, PostgreSQL, Oracle, SQL Server

**Configuration**:
```properties
# JDBC
repo-jdbc.factory = repo-jdbc
repo-jdbc.uri = jdbc:h2:${openl.home}/repositories/db/db
repo-jdbc.login = sa
repo-jdbc.password =

# JNDI
repo-jndi.factory = repo-jndi
repo-jndi.uri = java:comp/env/jdbc/DB
```

### Implementation: Archive Repositories

**ZIP Repository**: `org.openl.rules.repository.zip.ZippedLocalRepository`
- Read-only ZIP archive access
- Factory ID: `"repo-zip"`
- Multiple archives supported

**JAR Repository**: `org.openl.rules.repository.zip.JarLocalRepository`
- Classpath JAR access
- Factory ID: `"repo-jar"`

**Configuration**:
```properties
repo-zip.factory = repo-zip
repo-zip.uri = ${openl.home}/repositories/zipped
repo-zip.archives = archive1.zip,archive2.zip

repo-jar.factory = repo-jar
```

### Factory Pattern & SPI

**`RepositoryFactory`** interface:
```java
public interface RepositoryFactory {
    boolean accept(String factoryID);
    String getRefID();
    Repository create(Function<String, String> settings);
}
```

**ServiceLoader registration** (`/META-INF/services/org.openl.rules.repository.RepositoryFactory`):
```
org.openl.rules.repository.db.JdbcDBRepositoryFactory
org.openl.rules.repository.db.DatasourceDBRepositoryFactory
org.openl.rules.repository.file.LocalRepositoryFactory
org.openl.rules.repository.zip.ZipRepositoryFactory
org.openl.rules.repository.zip.JarRepositoryFactory
```

**Dynamic instantiation**:
```java
Repository repo = RepositoryInstatiator.newRepository(
    "production",
    propertyName -> propertyResolver.getProperty(propertyName)
);
```

### Security: Path Validation

**`PathCheckedRepository`** - Decorator that validates all paths:
- Prevents path traversal attacks (`../../../etc/passwd`)
- Normalizes paths
- Rejects absolute paths
- Validates characters

```java
Repository safeRepo = new PathCheckedRepository(unsafeRepo);
```

### Lock Management

**`LockManager`** - Filesystem-based distributed locking:
```java
LockManager lockManager = new LockManager(lockDir);
Lock lock = lockManager.lock("project-name");
try {
    // Protected operations
} finally {
    lock.unlock();
}
```

---

## 2. Git Repository Module

**Location**: `/home/user/openl-tablets/STUDIO/org.openl.rules.repository.git/`
**Purpose**: Production-grade Git backend with full version control
**Size**: 26 Java files, main class 3,526 lines

### Main Class: `GitRepository`

**Implements**: `BranchRepository`, `RepositorySettingsAware`, `Closeable`

**Key Features**:
- Full Git operations via JGit
- Branch management and merging
- Conflict detection and resolution
- Excel file conflict detection
- Protected branch enforcement
- Pull/push to remote
- Tag management

### Architecture

```
GitRepository
  ├─ ReadWriteLock repositoryLock      # Concurrent access control
  ├─ ReentrantLock remoteRepoLock      # Remote operation serialization
  ├─ org.eclipse.jgit.api.Git          # JGit API
  ├─ CommitMessageParser               # Parses commit messages
  ├─ WildcardBranchNameFilter          # Protected branch patterns
  └─ NotResettableCredentialsProvider  # Brute-force prevention
```

### File Operations

**List files at revision**:
```java
List<FileData> files = gitRepo.list("rules/");
// Returns files at current HEAD
```

**Read file at version**:
```java
FileItem item = gitRepo.read("rules/MyTable.xlsx");
try (InputStream stream = item.getStream()) {
    // Process file
}
```

**Save with commit**:
```java
FileData metadata = new FileData();
metadata.setName("rules/MyTable.xlsx");
metadata.setComment("Update premium calculation");
metadata.setAuthor(new UserInfo("john.doe", "john@example.com"));

FileData saved = gitRepo.save(metadata, inputStream);
// Commits to Git
```

**Delete (soft delete)**:
```java
boolean deleted = gitRepo.delete(fileData);
// Renames to .archived extension
```

### Branch Management

**List branches**:
```java
List<String> branches = gitRepo.getBranches("rules/");
// Returns: [main, feature/new-rules, hotfix/bug-123]
```

**Create branch**:
```java
gitRepo.createBranch("rules/", "feature/new-feature", "main");
// Creates new branch from main
```

**Switch branch**:
```java
BranchRepository featureBranch = gitRepo.forBranch("feature/new-feature");
// Returns scoped repository for branch
```

**Protected branches**:
```java
boolean isProtected = gitRepo.isBranchProtected("main");
// Checks against protected branch patterns
```

**Configuration**:
```properties
repo-git.protected-branches = main,master,release/*,hotfix/*
```

### Merge & Conflict Resolution

**Simple merge** (no conflicts):
```java
gitRepo.merge("feature/new-rules", author, null);
// Merges feature branch into current branch
```

**Merge with conflicts**:
```java
try {
    gitRepo.merge("feature/conflicting", author, null);
} catch (MergeConflictException e) {
    // Examine conflicts
    String baseCommit = e.getBaseCommit();
    String yoursCommit = e.getYoursCommit();
    String theirsCommit = e.getTheirsCommit();

    // Auto-resolvable Excel files
    Map<String, WorkbookDiffResult> autoResolve = e.getToAutoResolve();

    // Manual resolution
    Map<String, ResolutionType> resolutions = new HashMap<>();
    resolutions.put("rules/ConflictedFile.xlsx", ResolutionType.OURS);

    ConflictResolveData resolveData = new ConflictResolveData(
        theirsCommit,
        resolutions,
        "Resolved conflicts"
    );

    gitRepo.merge("feature/conflicting", author, resolveData);
}
```

**Pull from remote**:
```java
gitRepo.pull(author);
// Fetches and merges from remote
```

### Excel Conflict Detection

**Automatic conflict resolution** for Excel files:
- Non-overlapping sheet changes → auto-merge
- Different cells in same sheet → auto-merge
- Same cell changed on both sides → conflict

**Integration**: Uses `XlsWorkbookMerger` (see section 9)

### Commit Message Parsing

**`CommitMessageParser`** parses structured commit messages:

**Template**:
```properties
repo-git.comment-template = {user-message} Type: {commit-type}.
```

**Example commit message**:
```
Update premium calculation rules Type: SAVE.
```

**Parsed data**:
```java
CommitMessageParser parser = new CommitMessageParser(template);
CommitMessageParser.CommitMessage message = parser.parse(commitMsg);

String userMessage = message.getUserMessage(); // "Update premium calculation rules"
CommitType type = message.getCommitType();     // CommitType.SAVE
String username = message.getUsername();       // (if included)
```

**Commit types**:
```java
enum CommitType {
    SAVE,       // Normal save
    ARCHIVE,    // Archive file
    RESTORE,    // Restore from archive
    ERASE,      // Permanent delete
    MERGE       // Merge commit
}
```

### Configuration

```properties
# Connection
repo-git.factory = repo-git
repo-git.uri = https://github.com/org/rules.git
repo-git.local-repository-path = ${openl.home}/repositories/rules-git
repo-git.branch = main

# Authentication
repo-git.login = username
repo-git.password = token

# Timeouts
repo-git.connection-timeout = 60
repo-git.failed-authentication-seconds = 300
repo-git.max-authentication-attempts = 5

# Protected branches
repo-git.protected-branches = main,master,release/*

# Tags
repo-git.tag-prefix = v

# Commit message
repo-git.comment-template = {user-message} Type: {commit-type}.

# GC
repo-git.gc-auto-detach = true

# Change detection
repo-git.listener-timer-period = 10
```

### Key Classes

1. **`GitRepository`** (3,526 lines) - Main implementation
2. **`GitRepositoryFactory`** - Factory (ID: `"repo-git"`)
3. **`CommitMessageParser`** - Message parsing
4. **`MergeConflictException`** - Conflict information carrier
5. **`BranchDescription`** & `BranchesData`** - Branch metadata
6. **`WildcardBranchNameFilter`** - Protected branch matching
7. **`NotResettableCredentialsProvider`** - Brute-force prevention
8. **`LazyFileData`** - Deferred Git object loading

### JGit Integration

**Core JGit classes used**:
```
org.eclipse.jgit.api.*
  ├─ CloneCommand
  ├─ CommitCommand
  ├─ PushCommand / FetchCommand
  ├─ MergeCommand
  ├─ ResetCommand
  └─ Status

org.eclipse.jgit.revwalk.*
  ├─ RevWalk
  └─ RevCommit

org.eclipse.jgit.treewalk.*
  ├─ TreeWalk
  └─ ObjectReader

org.eclipse.jgit.diff.*
  ├─ DiffFormatter
  └─ DiffEntry
```

### Known Limitations

- **No shallow clone** - Always clones full history
- **LFS requires setup** - Git LFS filters must be configured
- **Protected branch client-side** - Server hooks recommended for enforcement

---

## 3. AWS S3 Repository Module

**Location**: `/home/user/openl-tablets/STUDIO/org.openl.rules.repository.aws/`
**Purpose**: Cloud storage backend using Amazon S3

### Main Class: `S3Repository`

**Implements**: `Repository`, `Closeable`

**Features**:
```
folders: false (flat key structure)
branches: false
versions: true (if bucket versioning enabled)
uniqueFileId: true (ETag)
local: false
searchable: false
```

### Operations

**List objects**:
```java
List<FileData> files = s3Repo.list("rules/");
// Lists S3 objects with "rules/" prefix
```

**Read object**:
```java
FileItem item = s3Repo.read("rules/MyTable.xlsx");
// Streams S3 object content
```

**Save object**:
```java
FileData saved = s3Repo.save(metadata, inputStream);
// PutObject to S3
```

**Versioning**:
```java
List<FileData> versions = s3Repo.listHistory("rules/MyTable.xlsx");
// Returns all versions if bucket versioning enabled
```

### Configuration

```properties
repo-aws-s3.factory = repo-aws-s3
repo-aws-s3.bucket-name = my-openl-rules
repo-aws-s3.region-name = us-east-1

# Custom endpoint (for S3-compatible services)
repo-aws-s3.service-endpoint = https://s3.example.com

# Credentials
repo-aws-s3.access-key = AKIAIOSFODNN7EXAMPLE
repo-aws-s3.secret-key = wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY

# Encryption
repo-aws-s3.sse-algorithm = AES256

# Change detection
repo-aws-s3.listener-timer-period = 10
```

### Key Classes

1. **`S3Repository`** - Main S3 backend
2. **`S3RepositoryFactory`** - Factory (ID: `"repo-aws-s3"`)
3. **`DrainableInputStream`** - Stream handling
4. **`LazyFileData`** - Deferred S3 object loading

### AWS SDK Integration

**Dependencies**:
```xml
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3</artifactId>
</dependency>
```

**Key SDK classes**:
```
S3Client
ListObjectsV2Request
ListObjectVersionsRequest
PutObjectRequest
GetObjectRequest
CopyObjectRequest
DeleteObjectRequest
BucketVersioningStatus
```

---

## 4. Azure Blob Storage Module

**Location**: `/home/user/openl-tablets/STUDIO/org.openl.rules.repository.azure/`
**Purpose**: Microsoft Azure cloud storage backend

### Main Class: `AzureBlobRepository`

**Implements**: `Repository`

**Features**:
```
folders: true (via prefix emulation)
branches: false
versions: true (emulated via YAML metadata)
uniqueFileId: true
local: false
searchable: false
```

### Version System

**Emulated Git-like versioning**:
- Each version = "Azure Commit" (YAML metadata)
- Stored in `.versions/versions.yaml`
- Contains author, message, file versions
- Uses `PassiveExpiringMap` cache with TTL

**Structure**:
```
[container]
├── [content]/
│   ├── rules/
│   │   └── MyTable.xlsx
│   └── ...
├── .versions/
│   └── versions.yaml       # All commits
└── .modification           # Modification tracker
```

**versions.yaml format**:
```yaml
commits:
  - id: commit-hash-1
    author:
      name: John Doe
      email: john@example.com
    message: "Initial commit"
    timestamp: 2024-01-01T10:00:00Z
    files:
      - path: rules/MyTable.xlsx
        version: version-id-1
  - id: commit-hash-2
    ...
```

### Operations

**List blobs**:
```java
List<FileData> files = azureRepo.list("rules/");
// Lists blobs with "rules/" prefix
```

**Read with version**:
```java
FileItem item = azureRepo.read("rules/MyTable.xlsx");
// Downloads blob content
```

**Save with commit**:
```java
FileData saved = azureRepo.save(metadata, inputStream);
// Uploads blob + updates versions.yaml
```

**History**:
```java
List<FileData> history = azureRepo.listHistory("rules/MyTable.xlsx");
// Returns versions from metadata
```

### Configuration

```properties
repo-azure-blob.factory = repo-azure-blob

# SAS URI
repo-azure-blob.uri = https://account.blob.core.windows.net/container?sas-token

# OR Shared Key
repo-azure-blob.account-name = mystorageaccount
repo-azure-blob.account-key = base64-key

# Change detection
repo-azure-blob.listener-timer-period = 10
```

### Key Classes

1. **`AzureBlobRepository`** - Main Azure backend
2. **`AzureBlobRepositoryFactory`** - Factory (ID: `"repo-azure-blob"`)
3. **`AzureCommit`** - Version metadata
4. **`FileInfo`** - File metadata for commits

### Azure SDK Integration

**Dependency**:
```xml
<dependency>
    <groupId>com.azure</groupId>
    <artifactId>azure-storage-blob</artifactId>
</dependency>
```

**Key SDK classes**:
```
BlobContainerClient
BlobClient
BlobContainerClientBuilder
BlobItem
BlobProperties
ListBlobsOptions
StorageSharedKeyCredential
```

### Known Issues

**TODO in pom.xml**: "Rewrite to use REST API without external library"

---

## 5. Workspace Management Module

**Location**: `/home/user/openl-tablets/STUDIO/org.openl.rules.workspace/`
**Purpose**: Organizes projects across repositories and manages user workspaces

### Architecture

```
UserWorkspace
  ├─ DesignTimeRepository (DTR)
  │   └─ List<Repository>
  │       ├─ FileSystemRepository
  │       ├─ GitRepository
  │       └─ S3Repository / AzureRepository
  └─ LocalWorkspace (LW)
      └─ LocalRepository (filesystem)
```

### Core Interfaces

**`UserWorkspace`**:
```java
public interface UserWorkspace extends ProjectsContainer {
    void activate();
    void passivate();
    void refresh();
    void syncProjects();

    DesignTimeRepository getDesignTimeRepository();
    LocalWorkspace getLocalWorkspace();

    AProject getProject(String repositoryId, String name);
    Collection<? extends AProject> getProjects();
    Collection<? extends AProject> getProjects(String repositoryId);

    void uploadLocalProject(String repositoryId, String name);
    AProject getProjectByPath(String repositoryId, String branch, String path);

    WorkspaceUser getUser();
    ProjectsLockEngine getProjectsLockEngine();
}
```

**`DesignTimeRepository`**:
```java
public interface DesignTimeRepository extends ProjectsContainer {
    AProject getProject(String repositoryId, String name, String version);
    AProject getProjectByPath(String repositoryId,
                               String branch,
                               String path,
                               String version);

    Repository getRepository(String id);
    Collection<Repository> getRepositories();
    String getRulesLocation();

    void refresh();
    void addListener(DesignTimeRepositoryListener listener);
}
```

**`LocalWorkspace`**:
```java
public interface LocalWorkspace extends ProjectsContainer {
    File getLocation();
    LocalRepository getRepository(String id);
    void refresh();
    void addListener(LocalWorkspaceListener listener);
}
```

### Implementation Classes

**`UserWorkspaceImpl`**:
- Combines DTR + LocalWorkspace
- Project filtering and organization
- Lock engine for concurrency
- User context (WorkspaceUser)

**`DesignTimeRepositoryImpl`**:
- Project cache (HashMap<ProjectKey, AProject>)
- Multiple repositories (List<Repository>)
- Version cache
- Listener notifications

**`LocalWorkspaceImpl`**:
- Filesystem-based project storage
- LocalRepository integration
- Change detection and refresh

### Configuration

**Multi-repository setup**:
```properties
# Repository list
design-repository-configs = local, production, staging

# Local workspace
local.$ref = repo-file
local.uri = ${openl.home}/rules

# Production Git
production.$ref = repo-git
production.uri = https://github.com/org/production-rules.git
production.branch = main

# Staging Git
staging.$ref = repo-git
staging.uri = https://github.com/org/staging-rules.git
staging.branch = develop
```

### Usage Example

**Access projects**:
```java
UserWorkspace workspace = workspaceFactory.createUserWorkspace(user);
workspace.activate();

// List all projects
Collection<AProject> allProjects = workspace.getProjects();

// Get specific project
AProject project = workspace.getProject("production", "InsuranceRules");

// Get project by path
AProject projectByPath = workspace.getProjectByPath(
    "production",
    "main",
    "rules/insurance",
    null
);

// Upload local project to DTR
workspace.uploadLocalProject("production", "LocalProject");

workspace.passivate();
```

### Concurrency: Lock Engine

**`ProjectsLockEngine`**:
```java
boolean tryLock(ProjectDescriptor project);
void unlock(ProjectDescriptor project);
boolean isLocked(ProjectDescriptor project);
LockInfo getLockInfo(ProjectDescriptor project);
```

**Usage**:
```java
ProjectsLockEngine lockEngine = workspace.getProjectsLockEngine();
if (lockEngine.tryLock(projectDescriptor)) {
    try {
        // Modify project
    } finally {
        lockEngine.unlock(projectDescriptor);
    }
}
```

---

## 6. Diff Module

**Location**: `/home/user/openl-tablets/STUDIO/org.openl.rules.diff/`
**Purpose**: Advanced table/rule comparison and change detection

### Architecture

```
DiffTreeBuilder
  ├─ builds → DiffTreeNode
  │            ├─ contains → DiffElement
  │            │              └─ has → DiffStatus
  │            └─ children → DiffTreeNode[]
  │
  └─ uses → Projection
              ├─ XlsProjection (Excel tables)
              └─ PropertySet
```

### Core Classes

**`DiffStatus`** - Change classification:
```java
enum DiffStatus {
    ADDED,            // New element
    REMOVED,          // Deleted element
    EQUALS,           // Identical
    DIFFERS,          // Changed
    ORIGINAL,         // Original version marker
    ORIGINAL_ABSENT   // No original available
}
```

**`DiffTreeNode`** - Hierarchical diff representation:
```java
interface DiffTreeNode {
    DiffElement getElement();
    DiffTreeNode[] getChildren();
    DiffStatus getStatus();
}
```

**`DiffElement`** - Single comparison item:
```java
interface DiffElement {
    String getName();
    Object getValue();
    DiffStatus getStatus();
}
```

### Excel-Specific Diff

**`XlsDiff2`** - Excel file comparison:
```java
XlsDiff2 differ = new XlsDiff2();
DiffTreeNode result = differ.diffFiles(file1, file2);

// Traverse diff tree
for (DiffTreeNode child : result.getChildren()) {
    DiffStatus status = child.getStatus();
    if (status == DiffStatus.DIFFERS) {
        // Handle difference
    }
}
```

**Table matching algorithm**:
1. **Exact match**: Same sheet, location, header
2. **Strong match**: Same sheet, location
3. **Moderate match**: Same sheet, header
4. **Weak match**: Same sheet
5. **Fuzzy match**: Similar names/structure

**`XlsProjection`** - Excel table as projection:
```java
XlsProjection projection = new XlsProjection(table);
ProjectionProperty[] properties = projection.getProperties();

for (ProjectionProperty prop : properties) {
    String name = prop.getName();
    Object value = prop.getValue();
}
```

### Projection-Based Comparison

**`Projection`** - Abstract view of object:
```java
interface Projection {
    PropertySet getProperties();
    String getName();
}
```

**`ProjectionDiffer`** - Compares projections:
```java
ProjectionDiffer differ = new ProjectionDifferImpl();
MergeResult result = differ.diff(projection1, projection2);
```

### Known Issues

**Comment in XlsDiff2.java**:
> "Incomplete. Need AxB vs CxD implementation"

**Performance**: O(n²) for large workbooks with many tables

---

## 7. Jackson Serialization Module

**Location**: `/home/user/openl-tablets/STUDIO/org.openl.rules.jackson/`
**Purpose**: Type-safe JSON serialization for OpenL types
**Size**: 25 Java files

### Main Factory: `JacksonObjectMapperFactoryBean`

**Purpose**: Creates configured `ObjectMapper` for OpenL types

**Configuration options**:
```properties
# Type handling
jackson.defaultTypingMode = DISABLED
jackson.jsonTypeInfoId = CLASS
jackson.typingPropertyName = @type
jackson.simpleClassNameAsTypingPropertyValue = false

# Serialization
jackson.serializationInclusion = NON_NULL
jackson.failOnEmptyBeans = false

# Deserialization
jackson.failOnUnknownProperties = false
jackson.caseInsensitiveProperties = false

# Formatting
jackson.defaultDateFormat = yyyy-MM-dd'T'HH:mm:ss.SSSZ
jackson.propertyNamingStrategy = DefaultStrategy

# Root classes
databinding.rootClassNames = com.example.MyClass1,com.example.MyClass2
```

### Type Handling Modes

**`DefaultTypingMode`** enum:
```java
enum DefaultTypingMode {
    DISABLED,                    // No type info
    JAVA_LANG_OBJECT,           // Only Object fields
    NON_CONCRETE_AND_ARRAYS,    // Interfaces and arrays
    EVERYTHING                   // All types
}
```

**Example**:
```json
{
  "@type": "com.example.PremiumRequest",
  "age": 25,
  "coverage": {
    "@type": "com.example.Coverage",
    "amount": 100000
  }
}
```

### Custom Serializers

**Float/Double precision**:
```java
// FloatSerializer
jackson.floatPrecision = 2

// Output: 123.45 instead of 123.44999694824219
```

**Date formatting**:
```java
// ExtendedStdDateFormat
// Supports ISO8601 + custom formats
jackson.defaultDateFormat = yyyy-MM-dd
```

### Property Naming Strategies

**Available strategies**:
```
DefaultStrategy          - camelCase (default)
SnakeCaseStrategy        - snake_case
LowerCaseStrategy        - lowercase
UpperCamelCaseStrategy   - PascalCase
LowerCamelCaseStrategy   - camelCase (explicit)
```

**Example**:
```java
// Java field: myPropertyName

// SnakeCaseStrategy
{"my_property_name": "value"}

// LowerCaseStrategy
{"mypropertyname": "value"}

// UpperCamelCaseStrategy
{"MyPropertyName": "value"}
```

### MixIn Annotations

**Purpose**: Add JSON annotations to existing classes without modifying them

**Example**:
```java
// Original class (can't modify)
public class MyDataType {
    private String field1;
    private int field2;
}

// MixIn class (add annotations)
@MixInClass
public abstract class MyDataTypeMixIn {
    @JsonProperty("custom_field_1")
    abstract String getField1();

    @JsonIgnore
    abstract int getField2();
}
```

**Registration**:
```java
ObjectMapper mapper = factory.createJacksonObjectMapper();
mapper.addMixIn(MyDataType.class, MyDataTypeMixIn.class);
```

### Project-Specific Factory

**`ProjectJacksonObjectMapperFactoryBean`**:
- Extends `JacksonObjectMapperFactoryBean`
- OpenL rule types integration
- Spreadsheet result customization
- Datatype class customization
- Root class name binding

**Usage**:
```java
ProjectJacksonObjectMapperFactoryBean factory =
    new ProjectJacksonObjectMapperFactoryBean();
factory.setClassLoader(projectClassLoader);
factory.setXlsModuleOpenClass(compiledOpenClass);

ObjectMapper mapper = factory.createJacksonObjectMapper();
String json = mapper.writeValueAsString(ruleResult);
```

### Dependencies

```xml
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.20.0</version>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jsr310</artifactId>
</dependency>
```

---

## 8. Jackson Configuration Module

**Location**: `/home/user/openl-tablets/STUDIO/org.openl.rules.jackson.configuration/`
**Purpose**: Minimal annotation-based configuration
**Size**: 2 Java files

### Key Classes

**`@MixInClass`** - Annotation for MixIn identification:
```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MixInClass {
}
```

**`JacksonBindingConfigurationUtils`** - Utilities:
```java
public class JacksonBindingConfigurationUtils {
    public static boolean isConfiguration(Class<?> clazz) {
        return clazz.isAnnotationPresent(MixInClass.class);
    }
}
```

**Usage**:
```java
@MixInClass
public abstract class MyTypeMixIn {
    // MixIn annotations
}
```

---

## 9. Excel Merge Module

**Location**: `/home/user/openl-tablets/STUDIO/org.openl.rules.xls.merge/`
**Purpose**: Three-way Excel file merge with conflict detection

### Main Class: `XlsWorkbookMerger`

**Implements**: `Closeable`

**Three-way merge**:
```
Base (common ancestor)
  ├─ Ours (first branch)
  └─ Theirs (second branch)
       ↓
  Result (merged)
```

### Usage

**Simple merge**:
```java
try (InputStream base = new FileInputStream("base.xlsx");
     InputStream ours = new FileInputStream("ours.xlsx");
     InputStream theirs = new FileInputStream("theirs.xlsx");
     XlsWorkbookMerger merger = XlsWorkbookMerger.of(base, ours, theirs)) {

    // Perform merge
    Workbook result = merger.merge();

    // Check for conflicts
    WorkbookDiffResult diffResult = merger.getDiffResult();
    Set<String> conflictingSheets = diffResult.getConflictingSheets();

    if (conflictingSheets.isEmpty()) {
        // No conflicts - save result
        try (FileOutputStream out = new FileOutputStream("result.xlsx")) {
            result.write(out);
        }
    } else {
        // Handle conflicts
        for (String sheet : conflictingSheets) {
            SheetDiffResult sheetDiff = diffResult.getSheetDiffResult(sheet);
            // Examine cell-level conflicts
        }
    }
}
```

### Conflict Detection

**Auto-resolvable scenarios**:
1. **Non-overlapping changes**: Different cells modified → auto-merge
2. **Different sheets**: Union of sheets
3. **One-side modification**: Use modified version

**Conflicting scenarios**:
1. **Same cell changed**: Both sides modified same cell → conflict
2. **Same sheet changed**: Both sides modified same sheet → conflict

**`DiffStatus`** for merge:
```java
enum DiffStatus {
    ADDED,         // New sheet/cell
    REMOVED,       // Deleted sheet/cell
    MODIFIED,      // Changed content
    NO_CHANGE,     // Identical
    CONFLICT       // Both sides modified
}
```

### Diff Results

**`WorkbookDiffResult`**:
```java
WorkbookDiffResult result = merger.getDiffResult();

// Get all sheets grouped by status
Map<DiffStatus, Set<String>> grouped = result.getSheetsGroupedByStatus();
Set<String> conflicting = grouped.get(DiffStatus.CONFLICT);

// Check specific sheet
DiffStatus status = result.getDiffStatus("Sheet1");
```

**`SheetDiffResult`**:
```java
SheetDiffResult sheetResult = result.getSheetDiffResult("Sheet1");

// Get conflicting cells
Set<CellAddress> conflicts = sheetResult.getConflictingCells();

// Get all changes
Set<CellAddress> changes = sheetResult.getChangedCells();

// Check specific cell
DiffStatus cellStatus = sheetResult.getCellStatus(new CellAddress("A1"));
```

### Sheet Matching

**`XlsWorkbooksMatcher`** - Matches sheets across workbooks:
1. **By name**: Exact sheet name match
2. **By position**: Sheet at same index
3. **By content**: Similar structure/content

### Cell Comparison

**`XlsSheetsMatcher`** - Cell-level comparison:
- Compares cell values
- Compares cell styles
- Compares cell formulas
- Detects conflicts

### HSSF Optimization

**For XLS files**:
- **`HSSFOptimiser`**: Optimizes workbook
- **`HSSFPaletteMatcher`**: Aligns color palettes
- **`HSSFPaletteDiffResult`**: Color palette differences

### Formula Handling

**`FormulaEvaluator`** integration:
- Preserves formulas during merge
- Re-evaluates after merge
- Updates cell references if needed

### Dependencies

```xml
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.4.1</version>
</dependency>
<dependency>
    <groupId>org.openl</groupId>
    <artifactId>org.openl.commons</artifactId>
</dependency>
```

---

## Integration Examples

### Multi-Backend Configuration

**Setup with File, Git, and S3**:
```properties
design-repository-configs = local, production, backup

# Local filesystem
local.$ref = repo-file
local.uri = ${openl.home}/rules

# Production Git
production.$ref = repo-git
production.uri = https://github.com/org/prod-rules.git
production.branch = main
production.login = ${git.username}
production.password = ${git.token}

# Backup S3
backup.$ref = repo-aws-s3
backup.bucket-name = rules-backup
backup.region-name = us-east-1
backup.access-key = ${aws.accessKey}
backup.secret-key = ${aws.secretKey}
```

### Git Workflow Example

**Feature branch workflow**:
```java
// 1. Create feature branch
gitRepo.createBranch("rules/", "feature/new-pricing", "main");

// 2. Switch to feature branch
BranchRepository featureBranch = gitRepo.forBranch("feature/new-pricing");

// 3. Make changes
FileData metadata = new FileData();
metadata.setName("rules/pricing/NewPricing.xlsx");
metadata.setComment("Add new pricing table");
metadata.setAuthor(author);
featureBranch.save(metadata, inputStream);

// 4. Merge back to main
gitRepo.merge("feature/new-pricing", author, null);

// 5. Push to remote
gitRepo.push(author);
```

### Excel Conflict Resolution

**Resolve merge conflicts**:
```java
try {
    gitRepo.merge("feature/conflicting", author, null);
} catch (MergeConflictException e) {
    // Get auto-resolvable files
    Map<String, WorkbookDiffResult> autoResolve = e.getToAutoResolve();

    for (Map.Entry<String, WorkbookDiffResult> entry : autoResolve.entrySet()) {
        String file = entry.getKey();
        WorkbookDiffResult diff = entry.getValue();

        // Check if truly auto-resolvable
        if (diff.getConflictingSheets().isEmpty()) {
            // No conflicts - can auto-resolve
        } else {
            // Manual resolution needed
            Map<String, ResolutionType> resolutions = new HashMap<>();
            resolutions.put(file, ResolutionType.OURS);
        }
    }

    // Retry merge with resolution
    ConflictResolveData resolveData = new ConflictResolveData(
        e.getTheirsCommit(),
        resolutions,
        "Resolved conflicts"
    );
    gitRepo.merge("feature/conflicting", author, resolveData);
}
```

---

## Design Patterns Summary

| Pattern | Usage | Example |
|---------|-------|---------|
| **Strategy** | Multiple Repository implementations | FileSystem, Git, S3, Azure |
| **Factory** | Repository creation | RepositoryFactory + SPI |
| **Decorator** | Path validation | PathCheckedRepository |
| **Observer** | Change notifications | Listener callbacks |
| **Proxy** | Lazy loading | LazyFileData |
| **Composite** | Hierarchical operations | Workspace + Repositories |
| **Adapter** | Backend adaptation | Various repository adapters |
| **Builder** | ObjectMapper configuration | JacksonObjectMapperFactoryBean |
| **Template Method** | Abstract repository structure | AbstractArchiveRepository |

---

## Performance Considerations

### Caching Strategies

**Git Repository**:
- Branch metadata cached in memory
- Commit cache with TTL
- ReadWriteLock for concurrent access

**Azure Repository**:
- PassiveExpiringMap for version metadata
- Configurable TTL for cached commits
- Reduces API calls

**Workspace**:
- Project cache in DesignTimeRepository
- Version cache for quick lookups
- Listener-based invalidation

### Optimization Tips

1. **Use local workspace** for frequent access
2. **Enable S3 versioning** at bucket level (faster than object metadata)
3. **Configure listener timer** appropriately (default: 10 seconds)
4. **Use branch-scoped repositories** for isolated changes
5. **Batch operations** where possible (Git push after multiple commits)

---

## Security Considerations

### Path Validation

Always use `PathCheckedRepository` wrapper:
```java
Repository safeRepo = new PathCheckedRepository(unsafeRepo);
```

**Prevents**:
- Path traversal (`../../../`)
- Absolute paths
- Invalid characters

### Authentication

**Git**:
- Token-based authentication recommended
- `NotResettableCredentialsProvider` prevents brute-force
- Failed authentication cooldown

**AWS S3**:
- IAM roles preferred over access keys
- Temporary credentials via STS
- Bucket policies for access control

**Azure**:
- SAS tokens with expiration
- Shared Key authentication
- Azure AD integration supported

### Encryption

**S3**:
- Server-side encryption (AES256, aws:kms)
- In-transit encryption (HTTPS)

**Azure**:
- Storage Service Encryption enabled by default
- HTTPS required

---

## Known Issues & Limitations

### General
- **No transaction support** across files
- **Synchronous listeners** block operations
- **Single user per UserWorkspace** instance

### Git-Specific
- No shallow clone support
- LFS requires manual setup
- Protected branches client-side only

### Azure-Specific
- **TODO**: Rewrite to use REST API directly
- Version emulation overhead (YAML metadata)

### Diff
- **Incomplete** AxB vs CxD implementation
- O(n²) performance for large workbooks

### Jackson
- Configuration complexity
- Type information increases JSON size

---

## See Also

- [DEV Module Overview](/docs/analysis/dev-module-overview.md) - Core engine details
- [Technology Stack](/docs/architecture/technology-stack.md) - Technology overview
- [Development Setup](/docs/onboarding/development-setup.md) - Getting started

---

**Module Group Documentation Complete**
**Batch**: 2 of 10
**Last Updated**: 2025-11-05
