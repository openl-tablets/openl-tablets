# STUDIO Module - Web Studio & Management Conventions

**Module**: STUDIO (Web IDE & Management)
**Location**: `/home/user/openl-tablets/STUDIO/`
**Version**: 6.0.0-SNAPSHOT
**Last Updated**: 2025-11-05

---

## Module Purpose

The STUDIO module provides the **Web-based IDE and management tools** for OpenL Tablets, including:
- Rule authoring and editing in a web browser
- Repository management (Git, AWS S3, Azure Blob Storage)
- User authentication and authorization
- Excel table editing
- OpenAPI generation from rules
- Modern React/TypeScript frontend
- Legacy JSF/RichFaces UI (being phased out)

**Critical**: STUDIO is in active modernization - migrating from JSF to React.

---

## Submodules Overview

| Submodule | Purpose | Technology | Status |
|-----------|---------|------------|--------|
| **`org.openl.rules.webstudio`** | Main Web Studio WAR | Spring Boot, JSF | ⚠️ Migrating to React |
| **`studio-ui`** | **Modern React frontend** | React 18, TypeScript 5.8 | ✅ Active development |
| **`org.openl.rules.webstudio.web`** | Web utilities | Spring MVC | ✅ Modern |
| **`org.openl.rules.webstudio.ai`** | AI features | Spring Boot, OpenAI | ✅ Modern |
| **`org.openl.rules.tableeditor`** | Excel table editor | JavaScript | ⚠️ Legacy |
| **`org.openl.rules.repository`** | Repository abstraction | Java | ✅ Modern |
| **`org.openl.rules.repository.git`** | Git backend | JGit | ✅ Modern |
| **`org.openl.rules.repository.aws`** | AWS S3 backend | AWS SDK | ✅ Modern |
| **`org.openl.rules.repository.azure`** | Azure backend | Azure SDK | ✅ Modern |
| **`org.openl.rules.workspace`** | Workspace management | Spring | ✅ Modern |
| **`org.openl.rules.diff`** | Excel diff utility | Java | ✅ Modern |
| **`org.openl.rules.xls.merge`** | Excel merge | Apache POI | ✅ Modern |
| **`org.openl.rules.jackson`** | JSON serialization | Jackson | ✅ Modern |
| **`org.openl.security`** | Security framework | Spring Security | ✅ Modern |
| **`org.openl.security.standalone`** | Standalone auth | Spring Security | ✅ Modern |
| **`org.openl.security.acl`** | ACL-based security | Spring ACL | ✅ Modern |
| **`org.openl.rules.project.openapi`** | OpenAPI generation | Swagger | ✅ Modern |

---

## Technology Stack

### Backend
- **Framework**: Spring Boot 3.5.6
- **Web Framework**: Spring MVC 6.2.11
- **Security**: Spring Security 6.5.5
- **Template Engine**: JSF/Facelets 4.0.12 (legacy)
- **REST Framework**: Spring REST
- **Build Tool**: Maven 3.9.9+

### Frontend
- **Modern**: React 18.3.1, TypeScript 5.8.3, Ant Design 5.26.4
- **Legacy**: JSF/RichFaces 10.0.0 (maintained fork)
- **Build**: Webpack 5.100.2, Node.js 24.9.0

### Data & Storage
- **Repositories**: Git (JGit), AWS S3, Azure Blob
- **Serialization**: Jackson 2.19.0
- **Excel**: Apache POI 5.3.0

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                    Web Browsers                         │
└────────────────┬───────────────────┬────────────────────┘
                 │                   │
     ┌───────────▼──────┐  ┌────────▼─────────┐
     │  studio-ui       │  │  JSF/RichFaces   │
     │  (React/TS)      │  │  (Legacy UI)     │
     │  [Modern] ✅     │  │  [Deprecated] ⚠️ │
     └───────────┬──────┘  └────────┬─────────┘
                 │                   │
                 └───────────┬───────┘
                             │
             ┌───────────────▼──────────────────┐
             │   org.openl.rules.webstudio      │
             │   (Spring Boot WAR)              │
             ├──────────────────────────────────┤
             │  REST Controllers                │
             │  Service Layer                   │
             │  Security Layer                  │
             └────────┬─────────────────┬───────┘
                      │                 │
         ┌────────────▼───────┐  ┌─────▼────────────┐
         │  Repository Layer  │  │  Workspace Mgmt  │
         │  (Git/S3/Azure)    │  │                  │
         └────────┬───────────┘  └─────┬────────────┘
                  │                    │
                  └──────────┬─────────┘
                             │
                   ┌─────────▼──────────┐
                   │  DEV Module         │
                   │  (Core Engine)      │
                   └─────────────────────┘
```

---

## Modernization Status

### ✅ Complete / Modern
- Repository backends (Git, AWS, Azure)
- Security framework (Spring Security)
- OpenAPI generation
- JSON serialization (Jackson)
- REST APIs

### 🔄 In Progress
- Frontend migration (JSF → React)
- Table editor modernization
- AI features integration

### ⚠️ Legacy / Deprecated
- JSF/RichFaces UI (use React instead)
- Server-side rendering (use REST + React)
- Legacy JavaScript table editor (plan migration)

---

## Coding Conventions

### Java/Spring Boot

#### Package Structure
```
org.openl.rules.webstudio/
├── controller/           # REST controllers
├── service/              # Business logic
├── repository/           # Data access (not DB, but rule repos)
├── security/             # Security configuration
├── model/                # DTOs and domain models
├── config/               # Spring configuration
└── exception/            # Exception handling
```

#### REST Controller Example
```java
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public ResponseEntity<List<ProjectDTO>> listProjects() {
        return ResponseEntity.ok(projectService.findAll());
    }

    @PostMapping
    public ResponseEntity<ProjectDTO> createProject(
            @Valid @RequestBody CreateProjectRequest request) {
        ProjectDTO created = projectService.create(request);
        return ResponseEntity
            .created(URI.create("/api/projects/" + created.getId()))
            .body(created);
    }

    @ExceptionHandler(ProjectNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ProjectNotFoundException ex) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(ex.getMessage()));
    }
}
```

#### Service Layer Example
```java
@Service
@Transactional
public class ProjectServiceImpl implements ProjectService {

    private final UserWorkspaceService workspaceService;
    private final SecurityChecker securityChecker;

    @Override
    public List<ProjectDTO> findAll() {
        // Check permissions
        securityChecker.checkPermission("projects.read");

        // Get projects from workspace
        Collection<RulesProject> projects =
            workspaceService.getUserWorkspace().getProjects();

        // Convert to DTOs
        return projects.stream()
            .map(ProjectMapper::toDTO)
            .collect(Collectors.toList());
    }
}
```

### React/TypeScript Frontend

**See**: [studio-ui/CLAUDE.md](studio-ui/CLAUDE.md) for detailed frontend conventions.

**Quick Overview**:
- Functional components with hooks
- TypeScript for type safety
- Zustand for state management
- Ant Design for UI components
- React Router for navigation
- i18next for internationalization

---

## Module-Specific Guidelines

### 1. Repository Management (`org.openl.rules.repository*`)

#### Key Interfaces
```java
// Base repository interface
public interface Repository {
    List<FileData> list(String path);
    FileData read(String name);
    FileData save(FileData data);
    List<FileData> delete(List<FileData> data);
    void close();
}

// Repository factory
public interface RepositoryFactory {
    Repository create(RepositoryConfiguration config);
}
```

#### Adding a New Repository Backend

**Steps**:
1. Implement `Repository` interface
2. Implement `RepositoryFactory` interface
3. Register factory in Spring configuration
4. Add configuration properties
5. Add tests (unit + integration)
6. Document configuration in user guide

**Example**: See `org.openl.rules.repository.git.GitRepository`

#### Best Practices
- Always close repositories when done
- Handle concurrent access carefully (locks)
- Validate paths to prevent directory traversal
- Implement proper error handling
- Log all repository operations
- Support both local and remote repositories

### 2. Security (`org.openl.security*`)

#### Security Architecture

```
┌──────────────────────────────────────────┐
│      Spring Security Filter Chain       │
├──────────────────────────────────────────┤
│  Authentication Filters                  │
│  (Form, SAML, CAS, JWT)                 │
└────────────────┬─────────────────────────┘
                 │
┌────────────────▼─────────────────────────┐
│      SecurityChecker                     │
│      (Permission validation)             │
└────────────────┬─────────────────────────┘
                 │
┌────────────────▼─────────────────────────┐
│      ACL (Access Control List)           │
│      (Fine-grained permissions)          │
└──────────────────────────────────────────┘
```

#### Authentication Methods Supported
- **Form-based**: Username/password
- **SAML 2.0**: Enterprise SSO
- **CAS**: Central Authentication Service
- **JWT**: API token authentication
- **OAuth 2.0**: Third-party authentication

#### Adding Permission Checks
```java
@PreAuthorize("hasPermission(#projectName, 'project', 'read')")
public ProjectDTO getProject(String projectName) {
    // Method implementation
}
```

#### Custom Permission Evaluators
```java
@Component
public class ProjectPermissionEvaluator implements PermissionEvaluator {

    @Override
    public boolean hasPermission(Authentication auth,
                                 Object targetDomainObject,
                                 Object permission) {
        // Custom permission logic
        return checkProjectAccess(auth, targetDomainObject, permission);
    }
}
```

### 3. OpenAPI Generation (`org.openl.rules.project.openapi`)

#### Purpose
Automatically generates OpenAPI 3.0 specifications from OpenL rule projects.

#### How It Works
```
Rule Methods → Type Analysis → OpenAPI Schema → Swagger UI
```

#### Customizing OpenAPI Output
```java
// Annotate rule methods in Excel with metadata
@OpenAPIOperation(
    summary = "Calculate insurance premium",
    description = "Calculates premium based on driver profile"
)
public Double calculatePremium(Driver driver, Vehicle vehicle) {
    // Rule implementation
}
```

#### Configuration
```yaml
openapi:
  generation:
    enabled: true
    title: "Insurance Rules API"
    version: "1.0.0"
    server-url: "https://api.example.com"
```

### 4. Workspace Management (`org.openl.rules.workspace`)

#### Workspace Architecture
```
User → UserWorkspace → DesignTimeRepository → LocalWorkspace
                     → ProductionRepository
```

**UserWorkspace**: Per-user view of projects
**DesignTimeRepository**: Projects being edited
**ProductionRepository**: Deployed/published projects
**LocalWorkspace**: User's local working copy

#### Working with Workspaces
```java
@Autowired
private UserWorkspaceService workspaceService;

public void openProject(String projectName) {
    UserWorkspace workspace = workspaceService.getUserWorkspace();
    RulesProject project = workspace.getProject(projectName);

    // Work with project
    project.open();

    // Save changes
    project.save();

    // Release lock
    project.close();
}
```

---

## Testing Requirements

### Unit Tests

**Framework**: JUnit 5, Mockito, Spring Test

**Example**:
```java
@SpringBootTest
@AutoConfigureMockMvc
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProjectService projectService;

    @Test
    void testListProjects() throws Exception {
        // Arrange
        when(projectService.findAll())
            .thenReturn(List.of(new ProjectDTO("test")));

        // Act & Assert
        mockMvc.perform(get("/api/projects"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("test"));
    }
}
```

### Integration Tests

**Location**: `/ITEST/itest.webstudio/`

**Requirements**:
- Test with real repositories (in-memory Git)
- Test security configurations
- Test REST endpoints end-to-end
- Use TestContainers for external dependencies

### Frontend Tests

**See**: [studio-ui/CLAUDE.md](studio-ui/CLAUDE.md)

**Tools**: Jest, React Testing Library, Cypress (E2E)

---

## Common Tasks

### Task: Add a New REST Endpoint

**Steps**:
1. Create DTO classes in `model/`
2. Create controller method
3. Add service layer implementation
4. Add permission checks
5. Write unit tests
6. Write integration tests
7. Update OpenAPI documentation
8. Update frontend to consume endpoint

**Example**: See `ProjectController` above

### Task: Add a New Repository Backend

**Steps**:
1. Create new module `org.openl.rules.repository.<backend>`
2. Implement `Repository` interface
3. Implement `RepositoryFactory` interface
4. Add Spring configuration
5. Add configuration properties
6. Write tests (unit + integration)
7. Document in user guide
8. Add to WebStudio UI

### Task: Migrate JSF Page to React

**Process**:
1. Identify JSF page functionality
2. Design React component structure
3. Create REST API for data
4. Implement React components
5. Add routing
6. Add translations (i18n)
7. Test thoroughly
8. Mark JSF page as deprecated
9. Document migration

**See**: [Migration Guide](../Docs/guides/migration-guide.md)

---

## Legacy Code - Do Not Enhance

### ⚠️ JSF/RichFaces UI

**Location**: `org.openl.rules.webstudio/src/main/webapp/`

**Status**: **DEPRECATED** - Being replaced by React

**Rules**:
- ✅ Bug fixes only
- ❌ No new features
- ❌ No UI enhancements
- ✅ Add REST API instead
- ✅ Implement in React

**Migration Priority**:
- High: Project management pages
- High: Rule editing pages
- Medium: Repository management
- Low: Admin pages

### Legacy Table Editor

**Location**: `org.openl.rules.tableeditor/`

**Status**: Legacy JavaScript code

**Rules**:
- Maintenance mode only
- Plan migration to modern framework
- Do not add complex features

---

## Performance Considerations

### Repository Operations
- **Lazy loading**: Load projects on demand
- **Caching**: Cache repository metadata
- **Batching**: Batch multiple operations
- **Connection pooling**: For remote repositories

### Frontend Performance
- **Code splitting**: Split React bundles
- **Lazy loading**: Load components on demand
- **Memoization**: Use React.memo() appropriately
- **Virtual scrolling**: For large lists

### Security
- **Session management**: Limit concurrent sessions
- **Token expiration**: Set appropriate timeouts
- **Rate limiting**: Prevent brute force attacks

---

## Security Best Practices

### Input Validation
```java
@PostMapping("/projects")
public ResponseEntity<ProjectDTO> createProject(
        @Valid @RequestBody CreateProjectRequest request) {

    // Validate project name
    if (!ProjectValidator.isValidName(request.getName())) {
        throw new ValidationException("Invalid project name");
    }

    // Sanitize path
    String safePath = PathUtils.sanitize(request.getPath());

    // Create project
    return ResponseEntity.ok(projectService.create(safePath));
}
```

### Authentication
- Use Spring Security
- Never store passwords in plain text
- Use `PassCoder` for password encoding
- Implement password policies
- Support MFA where possible

### Authorization
- Use role-based access control (RBAC)
- Use ACL for fine-grained permissions
- Check permissions at service layer (not just controller)
- Log all permission denials

### Data Protection
- Never log sensitive data (passwords, tokens)
- Encrypt data at rest (repository files)
- Use HTTPS in production
- Implement CSRF protection
- Sanitize all user inputs

---

## Build & Deployment

### Development Build
```bash
cd STUDIO
mvn clean install -DskipTests
```

### Production Build
```bash
cd STUDIO
mvn clean package -Pprod
```

### Docker Deployment
```bash
# Build Docker image
docker build -t openl/webstudio:latest .

# Run container
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -v /data/repos:/openl/repos \
  openl/webstudio:latest
```

---

## Common Issues & Solutions

### Issue: Repository Lock Conflicts
**Symptom**: "Project is locked by another user"
**Solution**: Implement proper lock release in finally blocks

### Issue: Session Timeout
**Symptom**: Users logged out unexpectedly
**Solution**: Increase session timeout or implement "keep alive"

### Issue: Large Excel Files
**Symptom**: OutOfMemoryError when loading projects
**Solution**: Increase JVM heap size, optimize POI usage

---

## Documentation References

- **Module Analysis**: [STUDIO Overview](../Docs/analysis/studio-wsfrontend-util-overview.md)
- **Root Conventions**: [/CLAUDE.md](../CLAUDE.md)
- **Frontend Conventions**: [studio-ui/CLAUDE.md](studio-ui/CLAUDE.md)
- **Architecture**: [Docs/architecture/](../Docs/architecture/)
- **Testing Guide**: [Docs/guides/testing-guide.md](../Docs/guides/testing-guide.md)

---

## When in Doubt

1. **Check if it's legacy code** - Don't enhance JSF/RichFaces
2. **Use React for new UI** - Add to studio-ui, not JSF
3. **Follow Spring Boot conventions** - Standard Spring practices
4. **Add tests** - Unit + integration tests required
5. **Check security** - Always validate permissions
6. **Ask for review** - STUDIO changes affect many users

---

**Last Updated**: 2025-11-05
**Version**: 6.0.0-SNAPSHOT
**Modernization Status**: Active (JSF → React migration in progress)
