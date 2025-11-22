# STUDIO, WSFrontend, and Util Modules Overview

**Module Groups**: STUDIO (Security, OpenL Studio, UI, OpenAPI), WSFrontend (Rule Services), Util (Tools)
**Batches Covered**: 3-9
**Last Updated**: 2025-11-05

---

## Executive Summary

This document provides comprehensive coverage of the upper layers of OpenL Tablets: Security framework, OpenL Studio application, Rule Services deployment, and utility tools. These modules build upon the core engine (DEV) and repository layer to provide production-ready web applications and services.

**Total Coverage**:
- **Security**: 3 modules (authentication, authorization, ACL)
- **OpenL Studio**: 4 core modules + React UI
- **OpenAPI**: 3 modules (generation, validation, integration)
- **Rule Services**: 12 modules (deployment, web services, Kafka)
- **Utilities**: 9 modules (Maven plugin, profiler, OpenTelemetry)
- **Integration Tests**: 18+ modules + Demo application

---

## BATCH 3: Security & Authentication (3 Modules)

### Overview

**Location**: `/home/user/openl-tablets/STUDIO/org.openl.security*/`
**Purpose**: Authentication, authorization, and access control for OpenL Studio and RuleService

### Module Structure

```
org.openl.security (Base)
  ├─ org.openl.security.standalone (Built-in user management)
  └─ org.openl.security.acl (ACL-based permissions)
```

### 1. org.openl.security - Base Security Framework

**Purpose**: Core security abstractions and Spring Security integration

**Key Interfaces**:
```java
// Authentication
interface OpenLAuthenticationProvider extends AuthenticationProvider {
    boolean supports(Class<?> authentication);
    Authentication authenticate(Authentication authentication);
}

// Authorization
interface PrivilegesEvaluator {
    boolean hasPrivilege(String privilege);
    boolean hasAuthority(String authority);
}

// User Management
interface UserManagementService {
    User getUserByUsername(String username);
    void createUser(User user);
    void updateUser(User user);
    void deleteUser(String username);
}
```

**Integration with Spring Security**:
- Custom `UserDetailsService` implementation
- Method security with `@PreAuthorize` annotations
- Security context management
- Session management

**Configuration**:
```properties
security.mode = standalone|saml|cas|acl
security.session.timeout = 1800
security.password.encoder = bcrypt
```

### 2. org.openl.security.standalone - Standalone Security

**Purpose**: Built-in user and group management with file-based storage

**Features**:
- File-based user database
- Password hashing (BCrypt)
- Group-based permissions
- Remember-me functionality

**User Storage**: `users.properties`, `groups.properties`

**Example users.properties**:
```properties
admin = {bcrypt}$2a$10$..., ADMIN
user1 = {bcrypt}$2a$10$..., Developers,Viewers
```

**Permissions**:
```
- CREATE_PROJECTS
- EDIT_PROJECTS
- DELETE_PROJECTS
- DEPLOY_PROJECTS
- VIEW_PROJECTS
- ADMIN
```

**Usage**:
```java
@PreAuthorize("hasAuthority('CREATE_PROJECTS')")
public void createProject(ProjectDescriptor project) {
    // Only users with CREATE_PROJECTS permission can access
}
```

### 3. org.openl.security.acl - ACL Security

**Purpose**: Fine-grained access control with Spring Security ACL

**Features**:
- Object-level permissions
- Inheritance support
- ACL caching
- Database-backed ACL storage

**ACL Structure**:
```
Object (Project/Rule)
  ├─ Owner (full control)
  ├─ ACL Entries
  │   ├─ User/Group → READ
  │   ├─ User/Group → WRITE
  │   └─ User/Group → DELETE
  └─ Parent ACL (inheritance)
```

**Database Schema**:
```sql
acl_class           -- Object types
acl_sid             -- Security identities (users/groups)
acl_object_identity -- Secured objects
acl_entry           -- Permissions
```

**Usage**:
```java
@PreAuthorize("hasPermission(#project, 'WRITE')")
public void updateProject(ProjectDescriptor project) {
    // Check ACL for WRITE permission on specific project
}
```

**Configuration**:
```properties
security.acl.enabled = true
security.acl.cache.size = 1000
security.acl.inheritance.enabled = true
```

### Authentication Modes

**SAML** (via OpenSAML):
```properties
security.mode = saml
security.saml.entity-id = https://studio.example.com
security.saml.idp-metadata-url = https://idp.example.com/metadata
security.saml.keystore-file = saml-keystore.jks
security.saml.keystore-password = changeit
```

**CAS** (Central Authentication Service):
```properties
security.mode = cas
security.cas.server-url = https://cas.example.com
security.cas.service-url = https://studio.example.com
```

**JWT** (JSON Web Tokens):
```properties
security.jwt.enabled = true
security.jwt.secret = your-secret-key
security.jwt.expiration = 86400
```

---

## BATCH 4: OpenL Studio Core (4 Modules)

### Overview

**Location**: `/home/user/openl-tablets/STUDIO/`
**Purpose**: Web-based IDE for rules authoring and management

### 1. org.openl.rules.webstudio - Main Application

**Type**: WAR application
**Technology**: Spring Boot 3.5.6, JSF 4.0.12 (legacy), React 18.3.1 (modern)
**Build Output**: `webapp.war`

**Architecture**:
```
OpenL Studio (Spring Boot)
  ├─ REST Controllers (/api/*)
  ├─ JSF Managed Beans (legacy UI)
  ├─ React SPA (/studio-ui/*)
  ├─ Security Layer
  ├─ Workspace Manager
  └─ Repository Integration
```

**Key Components**:

**REST API** (`org.openl.rules.webstudio.web.rest`):
```java
@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    @GetMapping
    public List<ProjectDTO> listProjects();

    @PostMapping
    public ProjectDTO createProject(@RequestBody ProjectDTO project);

    @PutMapping("/{id}")
    public ProjectDTO updateProject(@PathVariable String id,
                                     @RequestBody ProjectDTO project);

    @DeleteMapping("/{id}")
    public void deleteProject(@PathVariable String id);
}
```

**Project Management**:
- Create/edit/delete projects
- Import/export projects
- Project validation
- Dependency management

**Rule Editing**:
- Table editor (JSF-based, legacy)
- Modern React-based editor
- Syntax validation
- Type checking

**Testing**:
- Run test tables
- View test results
- Export test reports

**Deployment**:
- Deploy to RuleService
- Manage deployment configurations
- Version management

**Configuration**:
```properties
webstudio.mode = multi-user|single-user
webstudio.workspace = ${openl.home}/workspace
webstudio.history.count = 100
webstudio.concurrent.builds = 4
```

### 2. org.openl.rules.webstudio.web - Web Utilities

**Purpose**: Shared web utilities and servlets

**Key Classes**:
- `OpenLFilter` - Request filtering
- `ServletUtils` - HTTP utilities
- `WebContext` - Request context holder
- `ExceptionHandler` - Global exception handling

### 3. org.openl.rules.webstudio.ai - AI Features

**Purpose**: AI-assisted rule authoring (experimental)

**Features**:
- Rule suggestion
- Auto-completion
- Pattern detection
- Code generation assistance

**Note**: Experimental module, limited documentation

### 4. org.openl.rules.tableeditor - Table Editor

**Purpose**: Excel-like table editor component

**Technology**: JavaScript, jQuery (legacy)

**Features**:
- Cell editing
- Formatting
- Formula support
- Undo/redo
- Copy/paste

**Being replaced**: Modern React table editor in development

---

## BATCH 5: Studio Frontend (React/TypeScript)

### Overview

**Location**: `/home/user/openl-tablets/STUDIO/studio-ui/`
**Technology**: React 18.3.1, TypeScript 5.8.3, Ant Design 5.26.4

### Architecture

```
studio-ui/
├── src/
│   ├── components/          # React components
│   │   ├── ProjectList/
│   │   ├── RuleEditor/
│   │   └── TestRunner/
│   ├── pages/              # Page components
│   │   ├── Dashboard.tsx
│   │   ├── Editor.tsx
│   │   └── Settings.tsx
│   ├── services/           # API services
│   │   ├── projectService.ts
│   │   └── ruleService.ts
│   ├── stores/             # Zustand state management
│   │   ├── projectStore.ts
│   │   └── userStore.ts
│   ├── utils/              # Utilities
│   └── App.tsx             # Root component
├── public/                 # Static assets
├── package.json
├── tsconfig.json
└── webpack.config.js
```

### Key Technologies

**State Management** - Zustand:
```typescript
import create from 'zustand';

interface ProjectStore {
  projects: Project[];
  currentProject: Project | null;
  loadProjects: () => Promise<void>;
  selectProject: (id: string) => void;
}

export const useProjectStore = create<ProjectStore>((set) => ({
  projects: [],
  currentProject: null,
  loadProjects: async () => {
    const projects = await projectService.list();
    set({ projects });
  },
  selectProject: (id) => {
    const project = projects.find(p => p.id === id);
    set({ currentProject: project });
  },
}));
```

**Routing** - React Router 7.6.3:
```typescript
import { BrowserRouter, Routes, Route } from 'react-router-dom';

<BrowserRouter>
  <Routes>
    <Route path="/" element={<Dashboard />} />
    <Route path="/projects" element={<ProjectList />} />
    <Route path="/editor/:projectId" element={<Editor />} />
    <Route path="/settings" element={<Settings />} />
  </Routes>
</BrowserRouter>
```

**UI Components** - Ant Design:
```typescript
import { Table, Button, Modal, Form } from 'antd';

const ProjectList: React.FC = () => {
  const [projects, setProjects] = useState<Project[]>([]);

  const columns = [
    { title: 'Name', dataIndex: 'name', key: 'name' },
    { title: 'Modified', dataIndex: 'modified', key: 'modified' },
    {
      title: 'Actions',
      key: 'actions',
      render: (_, record) => (
        <Button onClick={() => openProject(record.id)}>Open</Button>
      ),
    },
  ];

  return <Table dataSource={projects} columns={columns} />;
};
```

**API Integration**:
```typescript
// services/projectService.ts
import axios from 'axios';

const API_BASE = '/api/projects';

export const projectService = {
  list: () => axios.get(API_BASE).then(res => res.data),
  get: (id: string) => axios.get(`${API_BASE}/${id}`).then(res => res.data),
  create: (project: ProjectDTO) => axios.post(API_BASE, project).then(res => res.data),
  update: (id: string, project: ProjectDTO) => axios.put(`${API_BASE}/${id}`, project).then(res => res.data),
  delete: (id: string) => axios.delete(`${API_BASE}/${id}`),
};
```

**Internationalization** - i18next:
```typescript
import i18n from 'i18next';
import { useTranslation } from 'react-i18next';

const MyComponent: React.FC = () => {
  const { t } = useTranslation();

  return <h1>{t('welcome.title')}</h1>;
};
```

**Build**: Webpack 5.100.2, produces static assets served by OpenL Studio

---

## BATCH 6: OpenAPI & Code Generation (3 Modules)

### Overview

**Location**: `/home/user/openl-tablets/STUDIO/org.openl.rules.project.openapi*/`
**Purpose**: Auto-generate OpenAPI specs from rules and scaffold code

### 1. org.openl.rules.project.openapi - Generation

**Purpose**: Generate OpenAPI/Swagger specifications from OpenL rules

**Key Classes**:
```java
public class OpenAPIGenerator {
    public OpenAPI generateFrom(CompiledOpenClass openClass) {
        OpenAPI api = new OpenAPI();
        api.setInfo(createInfo());
        api.setPaths(generatePaths(openClass));
        api.setComponents(generateSchemas(openClass));
        return api;
    }
}
```

**Generated Spec Example**:
```yaml
openapi: 3.0.0
info:
  title: Insurance Rules API
  version: 1.0.0
paths:
  /calculatePremium:
    post:
      operationId: calculatePremium
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/PremiumRequest'
      responses:
        '200':
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/PremiumResponse'
components:
  schemas:
    PremiumRequest:
      type: object
      properties:
        age: { type: integer }
        coverage: { type: number }
    PremiumResponse:
      type: object
      properties:
        premium: { type: number }
```

### 2. org.openl.rules.project.validation.openapi - Validation

**Purpose**: Validate generated OpenAPI specs

**Validations**:
- Schema validity
- Path consistency
- Type compatibility
- Security definitions

### 3. org.openl.rules.spring.openapi - Spring Integration

**Purpose**: Integrate OpenAPI generation with Spring Boot

**Auto-configuration**:
```java
@Configuration
@ConditionalOnEnable("openapi.enabled")
public class OpenAPIAutoConfiguration {
    @Bean
    public OpenAPI customOpenAPI(CompiledOpenClass openClass) {
        return new OpenAPIGenerator().generateFrom(openClass);
    }
}
```

**Swagger UI Integration**: Available at `/swagger-ui/`

---

## BATCH 7: Rule Services Core (12 Modules)

### Overview

**Location**: `/home/user/openl-tablets/WSFrontend/`
**Purpose**: Deploy rules as REST web services

### Architecture

```
RuleService (WAR)
  ├─ Service Loader (monitors repositories)
  ├─ Service Deployer (hot deployment)
  ├─ Service Manager (lifecycle)
  ├─ CXF REST (endpoints)
  ├─ Kafka Consumer (message-driven)
  └─ Log Storage (request/response logging)
```

### 1. org.openl.rules.ruleservice - Core Engine

**Purpose**: Core service orchestration

**Key Classes**:

**`RuleServiceLoader`**:
```java
public class RuleServiceLoader {
    public void load(Repository repository) {
        List<ProjectDescriptor> projects = repository.listProjects();
        for (ProjectDescriptor project : projects) {
            deployService(project);
        }
    }
}
```

**`RuleServiceDeployer`**:
```java
public class RuleServiceDeployer {
    public void deploy(DeploymentDescriptor deployment) {
        // Compile rules
        CompiledOpenClass openClass = compile(deployment);

        // Generate service interface
        Class<?> serviceInterface = generateInterface(openClass);

        // Create service instance
        Object serviceInstance = instantiate(openClass, serviceInterface);

        // Register endpoint
        registerEndpoint(serviceInstance, deployment);
    }
}
```

**`RuleServiceManager`**:
- Service lifecycle management
- Version management
- Hot reload support
- Dependency resolution

### 2. org.openl.rules.ruleservice.ws - Web Services

**Type**: WAR application
**Build Output**: `webapp.war`

**Protocols**:
- REST (Apache CXF JAX-RS)

**REST Endpoint Example**:
```
POST /REST/InsuranceRules/calculatePremium
Content-Type: application/json

{
  "age": 25,
  "coverage": 100000
}

Response:
{
  "premium": 500.00
}
```

**Swagger UI**: Available at `/swagger-ui/`

### 3. org.openl.rules.ruleservice.kafka - Kafka Integration

**Purpose**: Message-driven rule execution

**Configuration**:
```properties
kafka.enabled = true
kafka.bootstrap.servers = localhost:9092
kafka.consumer.group.id = openl-ruleservice
kafka.consumer.topics = rule-requests
kafka.producer.topic = rule-responses
```

**Message Format**:
```json
{
  "correlationId": "req-123",
  "service": "InsuranceRules",
  "method": "calculatePremium",
  "parameters": {
    "age": 25,
    "coverage": 100000
  }
}
```

### 4. org.openl.rules.ruleservice.deployer - Deployment

**Purpose**: Deployment management and filtering

**Deployment Descriptor** (`rules-deploy.xml`):
```xml
<deployments>
  <deployment>
    <name>InsuranceRules</name>
    <version>1.0.0</version>
    <modules>
      <module>premium-calc</module>
      <module>risk-assessment</module>
    </modules>
    <url>InsuranceRules</url>
    <lazy-modules-for-compilation>true</lazy-modules-for-compilation>
    <provide-runtime-context>true</provide-runtime-context>
  </deployment>
</deployments>
```

**Deployment Filters**:
- Filter by project name pattern
- Filter by version
- Filter by properties
- Custom filters

### 5-12. Supporting Modules

**org.openl.rules.ruleservice.annotation**: Service annotations
**org.openl.rules.ruleservice.common**: Common utilities
**org.openl.rules.ruleservice.ws.all**: WS with all plugins
**org.openl.rules.ruleservice.ws.common**: WS utilities
**org.openl.rules.ruleservice.ws.annotation**: WS annotations
**org.openl.rules.ruleservice.ws.storelogdata**: Log storage abstraction
**org.openl.rules.ruleservice.ws.storelogdata.db**: DB log storage
**org.openl.rules.ruleservice.ws.storelogdata.db.annotation**: Log annotations

**Request/Response Logging**:
```properties
ruleservice.store.logs.enabled = true
ruleservice.store.logs.db.url = jdbc:h2:./logs/request-logs
ruleservice.store.logs.include-request = true
ruleservice.store.logs.include-response = true
```

---

## BATCH 8: Utilities & Tools (9 Modules)

### Overview

**Location**: `/home/user/openl-tablets/Util/`
**Purpose**: Developer tools, Maven plugins, utilities

### 1. openl-maven-plugin - Maven Plugin

**Purpose**: Compile OpenL rules during Maven build

**Usage**:
```xml
<plugin>
  <groupId>org.openl</groupId>
  <artifactId>openl-maven-plugin</artifactId>
  <version>6.0.0-SNAPSHOT</version>
  <executions>
    <execution>
      <goals>
        <goal>compile</goal>
        <goal>generate-openapi</goal>
      </goals>
    </execution>
  </executions>
  <configuration>
    <sourceDirectory>src/main/openl</sourceDirectory>
    <outputDirectory>target/generated-sources</outputDirectory>
    <interfaceClass>com.example.MyRules</interfaceClass>
  </configuration>
</plugin>
```

**Goals**:
- `compile` - Compile rules to Java classes
- `generate-openapi` - Generate OpenAPI spec
- `test` - Run rule tests
- `package` - Package rules as JAR

### 2. openl-project-archetype - Maven Archetype

**Purpose**: Quick start project template

**Usage**:
```bash
mvn archetype:generate \
  -DarchetypeGroupId=org.openl \
  -DarchetypeArtifactId=openl-project-archetype \
  -DarchetypeVersion=6.0.0-SNAPSHOT \
  -DgroupId=com.example \
  -DartifactId=my-rules-project
```

**Generated Structure**:
```
my-rules-project/
├── pom.xml
├── rules.xml
├── rules/
│   └── MyRules.xlsx
└── src/
    ├── main/java/
    └── test/java/
```

### 3. openl-simple-project-archetype

**Purpose**: Minimal project template

### 4. openl-openapi-model-scaffolding - Code Generator

**Purpose**: Generate OpenL types from OpenAPI models

**Usage**:
```bash
java -jar openl-openapi-model-scaffolding.jar \
  --input api.yaml \
  --output rules/Datatypes.xlsx
```

**Generates**: Excel datatype tables from OpenAPI schemas

### 5. openl-openapi-parser - Parser

**Purpose**: Parse and validate OpenAPI specifications

### 6. openl-excel-builder - Excel Builder

**Purpose**: Programmatically create Excel rule files

**Example**:
```java
ExcelBuilder builder = new ExcelBuilder();
builder.createSheet("Rules");
builder.addDecisionTable("Calculate Premium")
       .addCondition("age", "IntRange")
       .addCondition("coverage", "DoubleRange")
       .addAction("premium", "Double")
       .addRow("18-25", "50000-100000", "500")
       .addRow("26-65", "50000-100000", "300");
builder.save("rules/Premium.xlsx");
```

### 7. openl-yaml - YAML Support

**Purpose**: YAML parsing and generation utilities

### 8. org.openl.rules.profiler - Performance Profiler

**Purpose**: Profile rule execution performance

**Usage**:
```java
@EnableProfiling
public interface MyRules {
    double calculatePremium(int age, double coverage);
}

// Profiling data automatically collected
ProfileReport report = profiler.getReport();
System.out.println("Method: " + report.getMethodName());
System.out.println("Calls: " + report.getCallCount());
System.out.println("Avg Time: " + report.getAverageTime() + "ms");
```

### 9. openl-rules-opentelemetry - Observability

**Purpose**: OpenTelemetry integration for tracing and metrics

**Configuration**:
```properties
opentelemetry.enabled = true
opentelemetry.service.name = openl-ruleservice
opentelemetry.exporter.otlp.endpoint = http://localhost:4317
opentelemetry.traces.enabled = true
opentelemetry.metrics.enabled = true
```

**Features**:
- Distributed tracing
- Rule execution metrics
- Custom spans
- Context propagation

---

## BATCH 9: Integration Tests & Demo

### Overview

**Location**: `/home/user/openl-tablets/ITEST/`
**Purpose**: End-to-end testing of all components

### Test Modules (18+)

**Smoke Tests**:
- `itest.smoke` - Basic functionality
- `itest.webservice` - Rule Services

**Security Tests**:
- `itest.security` - Authentication/authorization

**Integration Tests**:
- `itest.kafka.smoke` - Kafka integration
- `itest.spring-boot` - Spring Boot apps
- `itest.minio` - MinIO storage
- `itest.health` - Health checks
- `itest.deployment-filters` - Deployment filtering
- `itest.store-log-data` - Request logging
- `itest.tracing` - OpenTelemetry tracing

**Infrastructure**:
- `server-core` - Shared test server
- TestContainers for Docker-based tests

### Test Infrastructure

**TestContainers Usage**:
```java
@Testcontainers
class IntegrationTest {
    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16")
        .withDatabaseName("openl")
        .withUsername("test")
        .withPassword("test");

    @Container
    static GenericContainer keycloak = new GenericContainer("keycloak:latest")
        .withExposedPorts(8080);

    @Test
    void testWithDatabase() {
        // Test using real database
    }
}
```

### Demo Application

**Location**: `/home/user/openl-tablets/DEMO/`
**Output**: `openl-tablets-demo.zip`

**Contents**:
- Sample rules project
- OpenL Studio instance
- RuleService instance
- Configuration examples
- Getting started guide

**Quick Start**:
```bash
unzip openl-tablets-demo.zip
cd openl-tablets-demo
./start.sh
# Open http://localhost:8080
```

---

## Configuration Management

### Centralized Configuration

**application.properties hierarchy**:
```
1. Default properties (in JARs)
2. openl-default.properties
3. application.properties
4. Environment variables
5. System properties
6. Command-line arguments
```

### Configuration Properties Reference

**OpenL Studio**:
```properties
webstudio.mode = multi-user
webstudio.workspace = ${openl.home}/workspace
webstudio.history.count = 100
webstudio.concurrent.builds = 4
webstudio.export.excel.max-hidden-columns = 20
```

**RuleService**:
```properties
ruleservice.datasource = production
ruleservice.deploy.local.path = ${openl.home}/deploy
ruleservice.isProvideRuntimeContext = true
ruleservice.isProvideVariations = true
```

**Repository**:
```properties
design-repository-configs = local,production
production.$ref = repo-git
production.uri = https://github.com/org/rules.git
```

**Security**:
```properties
security.mode = standalone
security.session.timeout = 1800
security.password.encoder = bcrypt
```

---

## Deployment Patterns

### Pattern 1: Single Server

```
Server
├─ OpenL Studio (port 8080)
└─ RuleService (port 8081)
```

### Pattern 2: Separated Studio/Services

```
Studio Server (Internal)
└─ OpenL Studio (rule authoring)

Service Servers (Public)
├─ RuleService Instance 1
├─ RuleService Instance 2
└─ Load Balancer
```

### Pattern 3: Microservices

```
Studio (Kubernetes)
RuleService Pods (Kubernetes)
  ├─ Service A (scaling: 3)
  ├─ Service B (scaling: 5)
  └─ Service C (scaling: 2)
Kafka Cluster
PostgreSQL (HA)
Git Repository
```

### Docker Compose Example

```yaml
version: '3.8'
services:
  webstudio:
    image: openl/webstudio:latest
    ports:
      - "8080:8080"
    environment:
      - DESIGN_REPOSITORY_CONFIGS=production
      - PRODUCTION_URI=https://github.com/org/rules.git
    volumes:
      - studio-workspace:/openl/workspace

  ruleservice:
    image: openl/ruleservice:latest
    ports:
      - "8081:8081"
    environment:
      - RULESERVICE_DATASOURCE=production
      - PRODUCTION_URI=https://github.com/org/rules.git
    depends_on:
      - postgres

  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: openl
      POSTGRES_USER: openl
      POSTGRES_PASSWORD: openl

volumes:
  studio-workspace:
```

---

## Performance Tuning

### OpenL Studio Performance

**Concurrent Builds**:
```properties
webstudio.concurrent.builds = 4
# Increase for better responsiveness on large projects
```

**Cache Settings**:
```properties
webstudio.cache.enabled = true
webstudio.cache.size = 1000
webstudio.cache.ttl = 3600
```

### RuleService Performance

**Lazy Compilation**:
```xml
<lazy-modules-for-compilation>true</lazy-modules-for-compilation>
```

**Thread Pool**:
```properties
ruleservice.threadpool.core-size = 10
ruleservice.threadpool.max-size = 50
ruleservice.threadpool.queue-capacity = 100
```

**Caching**:
```properties
ruleservice.cache.enabled = true
ruleservice.cache.provider = caffeine
ruleservice.cache.size = 10000
```

---

## Monitoring & Observability

### Health Checks

**OpenL Studio**:
```
GET /actuator/health

{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" },
    "ping": { "status": "UP" }
  }
}
```

**RuleService**:
```
GET /admin/health

{
  "status": "UP",
  "services": {
    "InsuranceRules-v1.0": "UP",
    "MortgageRules-v2.0": "UP"
  },
  "repositories": {
    "production": "UP"
  }
}
```

### Metrics (Micrometer)

**Available metrics**:
- `ruleservice.invocations` - Rule invocation count
- `ruleservice.execution.time` - Execution duration
- `webstudio.projects.count` - Number of projects
- `webstudio.users.active` - Active users

**Export to**:
- Prometheus
- Graphite
- InfluxDB
- Datadog

### OpenTelemetry Tracing

**Distributed traces**:
```
HTTP Request → OpenL Studio
  ├─ Repository.read()
  ├─ Compiler.compile()
  │   ├─ Parser.parse()
  │   └─ Binder.bind()
  └─ RuleService.deploy()
```

---

## Troubleshooting Guide

### Common Issues

**1. Compilation Errors**
```
Problem: "Cannot resolve method"
Solution: Check dependencies in rules.xml
```

**2. Deployment Failures**
```
Problem: "Service already exists"
Solution: Check for duplicate deployment names
```

**3. Authentication Issues**
```
Problem: "Access denied"
Solution: Verify user permissions and ACLs
```

**4. Performance Degradation**
```
Problem: Slow rule execution
Solution: Enable profiling, check table size, optimize rules
```

### Debug Logging

**Enable debug logging**:
```properties
logging.level.org.openl.rules = DEBUG
logging.level.org.openl.rules.ruleservice = TRACE
```

---

## See Also

- [DEV Module Overview](/docs/analysis/dev-module-overview.md) - Core engine
- [Repository Layer Overview](/docs/analysis/repository-layer-overview.md) - Storage layer
- [Technology Stack](/docs/architecture/technology-stack.md) - Technologies
- [Development Setup](/docs/onboarding/development-setup.md) - Getting started

---

**Modules Documentation Complete**
**Batches**: 3-9 of 10
**Last Updated**: 2025-11-05
