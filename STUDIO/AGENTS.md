# STUDIO Module - Claude Code Conventions

**Module**: OpenL Tablets STUDIO (Web IDE)
**Version**: 6.0.0-SNAPSHOT
**Last Updated**: 2025-12-02

This file provides specific guidance for the STUDIO module, which contains the OpenL web IDE. For general project conventions, see the root `/AGENTS.md`.

---

## Module Overview

The STUDIO module is the web-based IDE for OpenL Tablets. It contains:

- **Web application** (Spring Boot, JSF/React)
- **Modern React frontend** (TypeScript, Ant Design)
- **Workspace/project management**
- **Table editor**
- **Security and authentication**
- **User management**
- **Deployment and lifecycle management**

**Architecture Note**: STUDIO is transitioning from legacy JSF/RichFaces to modern React. New features go in React, bug fixes only in JSF.

---

## Module Structure

### Submodules

```
STUDIO/
├── org.openl.rules.webstudio/          # Main web application (Spring Boot)
│   ├── src/main/
│   │   ├── java/org/openl/rules/webstudio/
│   │   │   ├── service/               # Business logic services
│   │   │   ├── controller/            # Spring MVC controllers
│   │   │   ├── filter/                # HTTP filters
│   │   │   ├── security/              # Security configuration
│   │   │   └── util/                  # Utilities
│   │   ├── resources/
│   │   │   ├── application*.properties  # Configuration
│   │   │   └── messages/              # i18n messages
│   │   └── webapp/
│   │       ├── pages/                 # JSF pages (legacy)
│   │       ├── components/            # JSF components
│   │       ├── resources/             # Static assets
│   │       └── WEB-INF/              # Web configuration
│   └── test/
│
├── studio-ui/                          # React/TypeScript frontend (MODERN)
│   ├── src/
│   │   ├── components/                # React components
│   │   ├── pages/                     # Page components
│   │   ├── hooks/                     # Custom React hooks
│   │   ├── services/                  # API clients
│   │   ├── utils/                     # Utilities
│   │   ├── store/                     # Zustand state management
│   │   ├── i18n/                      # Internationalization
│   │   └── styles/                    # SCSS stylesheets
│   ├── public/                        # Static assets
│   ├── webpack.config.js              # Webpack configuration
│   ├── tsconfig.json                  # TypeScript configuration
│   ├── package.json                   # Dependencies
│   └── .eslintrc.json                 # ESLint configuration
│
├── org.openl.rules.tableeditor/       # Table editor (separate component)
├── org.openl.rules.workspace/         # Workspace management
├── org.openl.security.*/              # Security modules (LDAP, AD, etc.)
├── org.openl.rules.test.common/       # Test utilities
└── ...other supporting modules
```

---

## Build & Development

### Building the STUDIO Module

```bash
# Full build with tests
cd STUDIO && mvn clean install

# Quick build (recommended for development)
cd STUDIO && mvn clean install -Dquick -DnoPerf -T1C

# Skip tests
cd STUDIO && mvn clean install -DskipTests

# Build just the React UI
cd STUDIO/studio-ui && npm install && npm run build
```

### Frontend Development (React)

```bash
# Development server with hot reload
cd STUDIO/studio-ui
npm start
# Opens http://localhost:8080 (proxied to backend)

# Production build
npm run build

# Code quality
npm run lint
npm run lint:fix
```

### Spring Boot Development

```bash
# Run Studio web application locally
cd STUDIO/org.openl.rules.webstudio
mvn spring-boot:run

# Or use Docker Compose
docker compose up
# Studio: http://localhost:8080
# Services: http://localhost:8081
```

---

## Architecture

### Layered Architecture

```
React Frontend (studio-ui/)
    ↓ REST/JSON API
Spring Boot MVC Controller Layer
    ↓
Service/Business Logic Layer
    ↓
Repository/Data Access Layer
    ↓
DEV Engine (Rules Compilation)
    ↓
PostgreSQL / H2 Database
```

### Key Components

**Web Application** (`org.openl.rules.webstudio`):
- Spring Boot 3.5.6
- Spring Security 6.5.5 (authentication, authorization)
- Spring Data (ORM, database access)
- OpenSAML 5.1.6 (SAML support)
- Apache CXF (SOAP/REST)

**Frontend** (`studio-ui/`):
- React 19.x with Hooks
- TypeScript 5.9.x
- Ant Design 6.2.x (UI components)
- Zustand 5.0.x (state management)
- React Router 7.13.x (routing)
- i18next 25.x (internationalization)

### Request Flow

```
User Interaction (React Component)
    ↓
API Service Call (fetch/axios)
    ↓
Spring REST Controller
    ↓
Service Layer (Business Logic)
    ↓
Repository Layer (Data Access)
    ↓
Database / DEV Engine
    ↓
Response (JSON)
    ↓
React State Update (Zustand)
    ↓
Component Re-render
```

---

## Frontend Development (React/TypeScript)

### Project Structure

```
studio-ui/
├── src/
│   ├── components/          # Reusable components
│   │   ├── Editor/         # Editor components
│   │   ├── Common/         # Common UI components
│   │   └── ...
│   ├── pages/              # Page-level components
│   │   ├── Dashboard/
│   │   ├── Editor/
│   │   └── ...
│   ├── hooks/              # Custom React hooks
│   ├── services/           # API client functions
│   ├── utils/              # Utility functions
│   ├── store/              # Zustand stores
│   ├── i18n/               # i18n configuration
│   ├── styles/             # Global styles, SCSS
│   ├── types.ts            # TypeScript type definitions
│   └── App.tsx             # Root component
├── public/                 # Static assets
├── webpack.config.js       # Webpack build configuration
├── tsconfig.json           # TypeScript configuration
├── .eslintrc.json          # ESLint rules
└── package.json            # Dependencies and scripts
```

### Code Organization Conventions

**File Naming**:
- Components: PascalCase (`RuleEditor.tsx`)
- Utilities: camelCase (`formatDate.ts`)
- Hooks: camelCase, prefixed with `use` (`useEditor.ts`)
- Types: PascalCase (`types.ts` with exported interfaces)

**Component Structure**:

```typescript
// Functional component with hooks
export interface MyComponentProps {
  title: string;
  onSave: (value: string) => void;
}

export const MyComponent: React.FC<MyComponentProps> = ({ title, onSave }) => {
  const [value, setValue] = useState('');
  const store = useEditorStore();

  const handleSave = () => {
    onSave(value);
  };

  return (
    <div>
      <h1>{title}</h1>
      <input value={value} onChange={(e) => setValue(e.target.value)} />
      <button onClick={handleSave}>Save</button>
    </div>
  );
};
```

### State Management

**Global State** (Zustand stores):
- Location: `src/store/`
- Use for: Application-wide state (editor state, user info, settings)
- File: `useEditorStore.ts`, `useUserStore.ts`, etc.

**Local State** (React hooks):
- Use for: Component-specific state (form values, UI toggling)
- Keep close to where it's used

**Example Zustand Store**:

```typescript
import { create } from 'zustand';

interface EditorState {
  isOpen: boolean;
  currentFile: string | null;
  setOpen: (open: boolean) => void;
  setFile: (file: string) => void;
}

export const useEditorStore = create<EditorState>((set) => ({
  isOpen: false,
  currentFile: null,
  setOpen: (open) => set({ isOpen: open }),
  setFile: (file) => set({ currentFile: file }),
}));
```

### API Integration

**Location**: `src/services/`

**Pattern**:

```typescript
// services/api.ts
export const fetchRules = async (projectId: string): Promise<Rule[]> => {
  const response = await fetch(`/api/projects/${projectId}/rules`);
  if (!response.ok) throw new Error('Failed to fetch rules');
  return response.json();
};

// In component
const { data: rules } = useFetch(() => fetchRules(projectId));
```

**Error Handling**:
- Use try/catch for async operations
- Display user-friendly error messages
- Log technical details for debugging
- Implement retry logic for transient failures

### Internationalization (i18n)

**Configuration**: `src/i18n/`

**Usage**:

```typescript
import { useTranslation } from 'i18next';

export const MyComponent = () => {
  const { t } = useTranslation();

  return <h1>{t('common.welcome')}</h1>;
};
```

**Translation Files**: `locales/*.json`
- English: `en.json`
- Other languages: `[lang].json`

### Styling

**Approach**:
- CSS-in-JS via Ant Design theme
- SCSS for custom styles
- BEM naming convention for class names

**Ant Design Configuration**:
- Theme customization in webpack config
- Color variables defined in theme

**Custom Styles**:

```scss
// styles/editor.scss
.editor {
  &__container {
    // ...
  }

  &__toolbar {
    // ...
  }
}
```

---

## Backend Development (Spring Boot)

### Controller Development

**Location**: `org.openl.rules.webstudio/src/main/java/org/openl/rules/webstudio/controller/`

**Pattern**:

```java
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

  private final ProjectService projectService;

  @GetMapping("/{id}")
  public ProjectDto getProject(@PathVariable String id) {
    return projectService.getProject(id);
  }

  @PostMapping
  public ProjectDto createProject(@RequestBody CreateProjectRequest request) {
    return projectService.createProject(request);
  }
}
```

**OpenAPI Documentation**:

```java
@GetMapping("/{id}")
@Operation(summary = "Get project by ID")
@ApiResponse(responseCode = "200", description = "Project found")
@ApiResponse(responseCode = "404", description = "Project not found")
public ProjectDto getProject(@PathVariable String id) {
  return projectService.getProject(id);
}
```

### Service Development

**Location**: `org.openl.rules.webstudio/src/main/java/org/openl/rules/webstudio/service/`

**Responsibilities**:
- Business logic implementation
- Transaction management (`@Transactional`)
- Service orchestration
- DTOs and entity conversion

**Pattern**:

```java
@Service
public class ProjectService {

  private final ProjectRepository repository;
  private final ProjectFactory factory;

  @Transactional
  public ProjectDto createProject(CreateProjectRequest request) {
    Project project = factory.createProject(request);
    Project saved = repository.save(project);
    return toDto(saved);
  }

  private ProjectDto toDto(Project project) {
    // Convert entity to DTO
  }
}
```

### Security Configuration

**Location**: `org.openl.rules.webstudio/src/main/java/org/openl/rules/webstudio/security/`

**Key Classes**:
- `SecurityConfig` - Security configuration
- `AuthenticationProvider` - Custom authentication
- `AccessControl` - Authorization checks

**Authentication Methods Supported**:
- Form-based
- SAML
- OAuth2 (via Keycloak)
- LDAP/Active Directory

**Usage**:

```java
@GetMapping("/admin-only")
@PreAuthorize("hasRole('ADMIN')")
public String adminOnly() {
  return "Admin section";
}
```

### Database Access

**ORM**: Spring Data JPA / Hibernate

**Location**: `org.openl.rules.webstudio/src/main/resources/db/`

**Database Migrations**:
- Liquibase or Flyway for schema management
- Migration scripts in `db/changelog/`

**Repository Pattern**:

```java
@Repository
public interface ProjectRepository extends JpaRepository<Project, String> {
  List<Project> findByOwner(String owner);
  Optional<Project> findByNameAndWorkspace(String name, String workspace);
}
```

---

## Critical Areas - Handle with Care

### 1. Security & Authentication

**Location**: `security/`, `org.openl.security.*/`

**Why Critical**:
- Affects all user access control
- Protects sensitive rule data
- Compliance-sensitive (GDPR, SOX, etc.)

**Rules**:
- Never remove authentication checks
- Always validate user permissions
- Sanitize user input
- Log security events
- Test with various authentication methods

### 2. Legacy JSF/RichFaces UI

**Location**: `org.openl.rules.webstudio/src/main/webapp/`

**Status**: ⚠️ **Legacy - Being replaced by React**

**Rules**:
- Bug fixes only
- Do not add new features
- New features go in React `studio-ui/` app

**If you must fix JSF**:
1. Identify the `.xhtml` file
2. Understand RichFaces component syntax
3. Fix the issue with minimal changes
4. Test in browser
5. Document why RichFaces needs this (migration blocker)

### 3. Database Schema

**Location**: `org.openl.rules.webstudio/src/main/resources/db/`

**Why Critical**:
- Affects all existing deployments
- Schema changes require migrations
- Data loss risk with incorrect migrations

**Rules**:
- Always create migrations (Liquibase/Flyway)
- Never modify schema directly
- Test migrations with data preservation
- Provide rollback capability
- Document schema changes

### 4. API Contracts

**Location**: `controller/`, OpenAPI definitions

**Why Critical**:
- WebServices module depends on these
- Frontend depends on these
- External integrations may depend on these

**Rules**:
- Document API with OpenAPI/Swagger
- Never remove endpoints without deprecation
- Breaking changes require major version bump
- Provide migration path for clients

---

## Testing Strategy

### Unit Tests

**Location**: `org.openl.rules.webstudio/test/`

**Framework**: JUnit 5, Mockito, Spring Test

**Test Naming**:
```java
@Test
void testCreateProject_validRequest_projectCreated() {
  // Arrange
  CreateProjectRequest request = new CreateProjectRequest("MyProject", "Desc");

  // Act
  ProjectDto result = controller.createProject(request);

  // Assert
  assertNotNull(result.getId());
  assertEquals("MyProject", result.getName());
}
```

### Integration Tests

**Location**: `ITEST/itest.webstudio/`

**Framework**: TestContainers, Spring Test, Selenium/Playwright

**Scope**:
- Spring context initialization
- Database integration
- REST API endpoints
- UI workflows (E2E)

**Running Tests**:

```bash
# WebStudio integration tests
cd ITEST/itest.webstudio && mvn verify

# Specific test class
mvn test -Dtest=ProjectControllerIT
```

### Frontend Tests

**Framework**: Jest, React Testing Library (when implemented)

**Status**: Placeholder (implementation pending)

```bash
cd STUDIO/studio-ui
npm test
```

---

## Common Development Tasks

### Task: Add a New REST Endpoint

**Steps**:
1. Create controller method in `WebStudioController` or new `*Controller`
2. Add OpenAPI annotations (@Operation, @ApiResponse, etc.)
3. Implement service method with business logic
4. Add unit tests for controller
5. Add integration tests in `ITEST/itest.webstudio`
6. Update API documentation
7. Update frontend to call new endpoint

### Task: Add a React Component

**Steps**:
1. Create component in `studio-ui/src/components/[Feature]/`
2. Define TypeScript interface for props
3. Use React hooks (useState, useContext, etc.)
4. Connect to Zustand store if needed
5. Implement i18n for user-facing text
6. Style with SCSS (BEM naming)
7. Add unit tests (when framework ready)
8. Test in browser

### Task: Modify Database Schema

**Steps**:
1. Create migration file: `db/changelog/v[version]/schema-change.sql`
2. Update JPA entity classes
3. Update repository interfaces if needed
4. Update service layer
5. Test migration on clean database
6. Test migration with existing data
7. Provide rollback migration
8. Document breaking changes

### Task: Add Authentication Method

**Steps**:
1. Create `AuthenticationProvider` implementation
2. Register in `SecurityConfig`
3. Add configuration properties
4. Implement user lookup/validation
5. Add mapping for user details
6. Test with real provider (or mock)
7. Document configuration
8. Test with WebStudio and Services

### Task: Fix a JSF/Legacy Issue

**Steps**:
1. Locate `.xhtml` file causing issue
2. Understand RichFaces component
3. Apply minimal fix
4. Test in browser
5. Document why RichFaces needed fix
6. Consider React migration timeline

---

## Configuration Management

### Application Properties

**Location**: `org.openl.rules.webstudio/src/main/resources/`

**Files**:
- `application.properties` - Default configuration
- `application-dev.properties` - Development overrides
- `application-prod.properties` - Production overrides

**Key Properties**:
```properties
server.port=8080
spring.datasource.url=jdbc:postgresql://localhost/openl
spring.security.oauth2.client.registration.keycloak.client-id=...

# Custom
openl.workspace.location=/path/to/workspace
openl.deployment.repository=/path/to/deployments
```

### Environment Variables

For Docker deployment:
```bash
DATABASE_URL=jdbc:postgresql://db:5432/openl
SECURITY_PROVIDER=saml
OAUTH2_CLIENT_ID=...
```

---

## Performance Considerations

### Frontend Performance

**Optimization Tips**:
- Code splitting: Load components on demand
- Image optimization: Use appropriate formats/sizes
- Caching: Cache API responses when possible
- Lazy loading: Load lists/tables incrementally

**Tools**:
- Webpack bundle analyzer: Identify large chunks
- React DevTools Profiler: Identify slow renders
- Chrome DevTools: Network, performance profiling

### Backend Performance

**Caching Strategies**:
- Cache compiled rules (expensive to compile)
- Cache user lookups (LDAP/AD)
- Cache project metadata
- Use database query indexes

**Optimization**:
- Batch API calls when possible
- Pagination for large result sets
- Avoid N+1 queries (use JPA eager loading)
- Async processing for long operations

---

## Build Guidelines

### Maven Build

```bash
# Full build with tests
cd STUDIO && mvn clean install

# Quick build
cd STUDIO && mvn clean install -Dquick -DnoPerf -T1C

# Build without tests
cd STUDIO && mvn clean install -DskipTests

# Just webstudio application
cd STUDIO/org.openl.rules.webstudio && mvn package
```

### Frontend Build

```bash
cd STUDIO/studio-ui

# Development
npm start          # Hot reload server
npm run build      # Production build
npm run lint       # Code quality check
npm run lint:fix   # Auto-fix issues
```

### Docker Image

```bash
# Build Studio Docker image
docker build -t openl-studio:latest .

# Run with Docker Compose
docker compose up

# Or run individual image
docker run -p 8080:8080 openl-studio:latest
```

---

## Code Review Checklist for STUDIO

Before committing STUDIO changes:

**Backend**:
- [ ] Follows Spring Boot conventions
- [ ] Security checks (authentication, authorization)
- [ ] OpenAPI documentation added
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] No breaking API changes
- [ ] Database migrations provided (if schema change)

**Frontend**:
- [ ] TypeScript types defined properly
- [ ] ESLint and Stylelint pass
- [ ] Components tested in browser
- [ ] i18n keys added for user-facing text
- [ ] Zustand state management used correctly
- [ ] No hardcoded strings in components
- [ ] Responsive design verified

**Both**:
- [ ] No sensitive data in logs
- [ ] Error handling comprehensive
- [ ] Documentation updated
- [ ] Backward compatibility maintained

---

## For More Information

- **Root Project Conventions**: `/AGENTS.md`
- **Frontend Specific**: `STUDIO/studio-ui/AGENTS.md` (to be created)
- **Architecture**: `/Docs/ARCHITECTURE.md`
- **API Documentation**: `/Docs/API_GUIDE.md`
- **Integration Guides**: `/Docs/integration-guides/`

---

**Last Updated**: 2025-12-02
**Version**: 6.0.0-SNAPSHOT
