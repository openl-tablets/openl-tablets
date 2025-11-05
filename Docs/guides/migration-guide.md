# OpenL Tablets Migration Guide

**Last Updated**: 2025-11-05
**Version**: 6.0.0-SNAPSHOT

---

## Table of Contents

- [Overview](#overview)
- [Version Migration](#version-migration)
- [Database Migration](#database-migration)
- [Frontend Migration (JSF to React)](#frontend-migration-jsf-to-react)
- [Repository Migration](#repository-migration)
- [Configuration Migration](#configuration-migration)
- [API Migration](#api-migration)
- [Migration Troubleshooting](#migration-troubleshooting)

---

## Overview

This guide covers various migration scenarios in OpenL Tablets:

1. **Version Migration**: Upgrading from one version to another
2. **Database Migration**: Schema changes and data migration
3. **Frontend Migration**: Moving from JSF to React
4. **Repository Migration**: Changing repository backends
5. **Configuration Migration**: Updating application configuration

---

## Version Migration

### Migration System Architecture

OpenL Tablets has a built-in migration orchestrator that runs automatically on application startup:

**Location**: `STUDIO/org.openl.rules.webstudio/src/org/openl/rules/webstudio/Migrator.java`

The migrator handles:
- Configuration file updates
- Repository structure changes
- Database schema migrations
- Security configuration updates

### Supported Version Paths

| From Version | To Version | Migration Path | Auto? |
|-------------|-----------|----------------|-------|
| 5.23.x | 5.24.x | Direct | ✅ Yes |
| 5.24.x | 5.26.0 | Direct | ✅ Yes |
| 5.26.0 | 5.26.1 | Direct | ✅ Yes |
| 5.26.x | 6.0.0 | Direct | ✅ Yes |
| 5.22.x | 6.0.0 | Via 5.24 → 5.26 | ⚠️ Multi-step |

### Migration to 5.24.x

**Key Changes**:

1. **Project History Migration**
   - Changed from unlimited history to count-based retention
   - Old: All versions kept indefinitely
   - New: Configurable version count (default: 50)

2. **Test Parallel Execution**
   - New setting: `test.parallel.execution=true`
   - Enables parallel test execution for better performance

3. **Repository Path Migration**
   - Design repository path changes
   - Production repository path changes
   - Automatic path resolution

4. **Project Lock Migration**
   - Enhanced project locking mechanism
   - Prevents concurrent modifications

5. **Git Branch Properties Migration**
   - Git repository branch configuration updates

**Action Required**: None (automatic on startup)

### Migration to 5.26.0

**Key Changes**:

1. **Repository Factory Refactoring**
   - Old: `repository-factory` property
   - New: `repository-ref` property
   - Migration: Automatic conversion on startup

2. **Production Repository Defaults**
   - Restoration of default production repository settings
   - Verification of repository configuration

3. **SAML Property Cleanup**
   - Removal of deprecated SAML properties
   - Migration to new SAML configuration format

4. **H2 Database v2 Migration**
   - ⚠️ **Important**: H2 database upgraded to version 2.x
   - Old H2 v1.x databases require manual migration
   - See [H2 Database Migration](#h2-database-migration)

**Action Required**:
- Review H2 database version if using embedded H2
- Manual migration required for H2 v1.x databases

### Migration to 5.26.1

**Key Changes**:

1. **Repository Factory Improvements**
   - Further repository factory refactoring
   - Production repository defaults restoration

**Action Required**: None (automatic on startup)

### Migration to 6.0.0

**Key Changes**:

1. **Java Version Requirement**
   - Old: Java 8 / Java 11
   - New: **Java 21+ required**
   - Action: Upgrade JDK before migration

2. **Spring Framework Upgrade**
   - Spring Boot upgraded to 3.5.6
   - Spring Framework upgraded to 6.2.11
   - Jakarta EE namespace changes (javax → jakarta)

3. **Jakarta EE Migration**
   - All `javax.*` imports → `jakarta.*`
   - Affects: Servlet API, Persistence API, Bean Validation

4. **Hibernate Upgrade**
   - Hibernate ORM upgraded to 6.6.31
   - Behavioral changes in lazy loading
   - New query syntax requirements

5. **Jetty Upgrade**
   - Jetty upgraded to 12.1.3
   - Configuration changes required

**Action Required**:
1. Upgrade to Java 21+
2. Update custom code to use Jakarta EE namespaces
3. Review Hibernate query compatibility
4. Update Jetty configuration

### Pre-Migration Checklist

Before upgrading:

- [ ] **Backup Everything**
  - Database backup
  - Repository backup (rules, projects)
  - Configuration files backup

- [ ] **Review Release Notes**
  - Check breaking changes
  - Review new features
  - Identify deprecated APIs

- [ ] **Test in Staging**
  - Deploy to staging environment first
  - Run full test suite
  - Verify all critical functionality

- [ ] **Update Dependencies**
  - Update custom plugins
  - Update client libraries
  - Test integrations

- [ ] **Plan Rollback**
  - Document rollback procedure
  - Prepare rollback scripts
  - Test rollback in staging

### Migration Procedure

```bash
# 1. Stop OpenL Tablets
systemctl stop openl-tablets

# 2. Backup database
pg_dump openl > openl_backup_$(date +%Y%m%d).sql

# 3. Backup repository
tar -czf openl_repository_backup_$(date +%Y%m%d).tar.gz /path/to/repository

# 4. Backup configuration
cp -r /path/to/config /path/to/config_backup_$(date +%Y%m%d)

# 5. Deploy new version
cp openl-tablets-6.0.0.war /path/to/deployment

# 6. Start OpenL Tablets
systemctl start openl-tablets

# 7. Monitor logs for migration
tail -f /var/log/openl-tablets/application.log

# 8. Verify migration
# - Check application logs
# - Test critical functionality
# - Verify data integrity
```

---

## Database Migration

### Flyway Migration System

OpenL Tablets uses **Flyway** for database version control:

**Migration Scripts Location**:
```
STUDIO/org.openl.security.standalone/resources/db/flyway/
├── common/           # Database-agnostic migrations
├── postgresql/       # PostgreSQL-specific migrations
├── mysql/            # MySQL-specific migrations
├── oracle/           # Oracle-specific migrations
├── h2/               # H2-specific migrations
└── mssql/            # MS SQL Server-specific migrations
```

### Migration Naming Convention

```
V{version}__{description}.sql

Examples:
V5.1__Create_identity.sql
V8__Extend_Group_Name_Length.sql
V9__ACL.sql
V10.2__Replace_privileges_with_permissions.sql
```

### Flyway Configuration

```properties
# application.properties

# Migration settings
spring.flyway.enabled=true
spring.flyway.baseline-version=0
spring.flyway.table=openl_security_flyway
spring.flyway.locations=classpath:db/flyway/common,classpath:db/flyway/{vendor}

# Database settings
spring.datasource.url=jdbc:postgresql://localhost:5432/openl
spring.datasource.username=openl
spring.datasource.password=secret
```

### Key Database Migrations

| Version | Migration | Description |
|---------|-----------|-------------|
| V5.1 | Create_identity | Initial identity tables (Oracle) |
| V8 | Extend_Group_Name_Length | Increase group name column size |
| V9 | ACL | Access Control List tables |
| V9.1-V9.4 | ACL | Oracle-specific ACL migrations |
| V10.2 | Replace_privileges_with_permissions | Permission system refactoring |
| V11.1 | Lock_Modify_Date | Add lock modification tracking |
| V12.1 | Expand_nested_groups | Support nested group hierarchy |
| V13.1 | Update_ACL_permissions | Update ACL permission structure |

### Running Migrations Manually

```bash
# Using Maven
mvn flyway:migrate -Dflyway.url=jdbc:postgresql://localhost/openl

# Using Flyway CLI
flyway -url=jdbc:postgresql://localhost/openl \
       -user=openl \
       -password=secret \
       migrate

# Check migration status
flyway info
```

### Rollback Strategy

Flyway doesn't support automatic rollback. For rollback:

1. **Create undo scripts manually**:
   ```sql
   -- U9__ACL.sql (undo for V9__ACL.sql)
   DROP TABLE IF EXISTS acl_entry;
   DROP TABLE IF EXISTS acl_object_identity;
   DROP TABLE IF EXISTS acl_class;
   ```

2. **Use database backups**:
   ```bash
   # Restore from backup
   psql openl < openl_backup_20250101.sql
   ```

3. **Use Flyway undo** (commercial version):
   ```bash
   flyway undo
   ```

### H2 Database Migration

**Migrating from H2 v1.x to v2.x**:

```bash
# 1. Export data from H2 v1.x
java -cp h2-1.4.200.jar org.h2.tools.Script \
  -url jdbc:h2:/path/to/database \
  -script backup.sql

# 2. Create new H2 v2.x database
java -cp h2-2.3.232.jar org.h2.tools.RunScript \
  -url jdbc:h2:/path/to/new_database \
  -script backup.sql

# 3. Update application.properties
spring.datasource.url=jdbc:h2:/path/to/new_database
```

### PostgreSQL Migration

**Upgrading PostgreSQL version**:

```bash
# Using pg_upgrade
pg_upgrade \
  -b /usr/lib/postgresql/13/bin \
  -B /usr/lib/postgresql/15/bin \
  -d /var/lib/postgresql/13/data \
  -D /var/lib/postgresql/15/data

# Or using logical replication
# 1. Create subscription on new server
# 2. Replicate data
# 3. Switch application to new server
```

### Database Migration Troubleshooting

#### Migration Failed Halfway

```bash
# 1. Check Flyway history
SELECT * FROM openl_security_flyway ORDER BY installed_rank;

# 2. Mark failed migration as repaired
flyway repair

# 3. Re-run migration
flyway migrate
```

#### Checksum Mismatch

```bash
# If migration script was modified after execution
flyway repair

# Repair will update checksums in history table
```

#### Migration Validation Error

```bash
# Validate migrations
flyway validate

# Clean database (⚠️ DANGEROUS - deletes all data)
flyway clean

# Baseline existing database
flyway baseline -baselineVersion=5.0
```

---

## Frontend Migration (JSF to React)

### Overview

OpenL Tablets is migrating from JavaServer Faces (JSF) to React:

- **Old**: JSF with PrimeFaces
- **New**: React 18.3.1 + TypeScript + Ant Design

**Location**: `STUDIO/studio-ui/`

### Migration Status

| Component | Status | Priority |
|-----------|--------|----------|
| Project List | ✅ Migrated | High |
| Project Editor | ⏳ In Progress | High |
| Rule Editor | ❌ Pending | High |
| User Management | ✅ Migrated | Medium |
| Repository Settings | ⏳ In Progress | Medium |
| Deployment Manager | ❌ Pending | High |
| ACL Editor | ❌ Pending | Low |

### Migration Approach

#### 1. Hybrid Approach

Both JSF and React coexist during migration:

```
┌─────────────────────────────────────┐
│  OpenL Tablets Web Application      │
├─────────────────────────────────────┤
│  JSF Pages (Legacy)                 │
│  - Complex rule editor              │
│  - Some admin pages                 │
├─────────────────────────────────────┤
│  React SPA (Modern)                 │
│  - Project list                     │
│  - User management                  │
│  - New features                     │
└─────────────────────────────────────┘
```

#### 2. REST API Backend

All new React components use REST APIs:

```typescript
// Frontend: React + TypeScript
const projects = await api.get<Project[]>('/api/projects');

// Backend: Spring REST Controller
@RestController
@RequestMapping("/api/projects")
public class ProjectsController {
    @GetMapping
    public List<Project> getProjects() {
        return projectService.findAll();
    }
}
```

### Migrating a JSF Page to React

#### Step 1: Identify JSF Page

```xhtml
<!-- Old JSF page: projects.xhtml -->
<ui:composition template="/pages/layout/mainLayout.xhtml">
    <h:form id="projectsForm">
        <p:dataTable value="#{projectsBean.projects}" var="project">
            <p:column headerText="Name">
                #{project.name}
            </p:column>
            <p:column headerText="Version">
                #{project.version}
            </p:column>
        </p:dataTable>
    </h:form>
</ui:composition>
```

#### Step 2: Create REST API

```java
@RestController
@RequestMapping("/api/projects")
public class ProjectsController {

    @Autowired
    private ProjectService projectService;

    @GetMapping
    public List<ProjectDTO> getProjects() {
        return projectService.findAll()
            .stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    private ProjectDTO toDTO(Project project) {
        ProjectDTO dto = new ProjectDTO();
        dto.setId(project.getId());
        dto.setName(project.getName());
        dto.setVersion(project.getVersion());
        return dto;
    }
}
```

#### Step 3: Create React Component

```typescript
// src/pages/Projects/ProjectList.tsx
import React, { useEffect, useState } from 'react';
import { Table } from 'antd';
import { api } from '../../services/api';

interface Project {
  id: string;
  name: string;
  version: string;
}

export const ProjectList: React.FC = () => {
  const [projects, setProjects] = useState<Project[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadProjects();
  }, []);

  const loadProjects = async () => {
    try {
      setLoading(true);
      const data = await api.get<Project[]>('/api/projects');
      setProjects(data);
    } catch (error) {
      console.error('Failed to load projects', error);
    } finally {
      setLoading(false);
    }
  };

  const columns = [
    {
      title: 'Name',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: 'Version',
      dataIndex: 'version',
      key: 'version',
    },
  ];

  return (
    <Table
      dataSource={projects}
      columns={columns}
      loading={loading}
      rowKey="id"
    />
  );
};
```

#### Step 4: Add Routing

```typescript
// src/App.tsx
import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { ProjectList } from './pages/Projects/ProjectList';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/projects" element={<ProjectList />} />
      </Routes>
    </BrowserRouter>
  );
}
```

#### Step 5: Update Navigation

```java
// Redirect from old JSF URL to new React URL
@Controller
public class NavigationController {

    @GetMapping("/faces/pages/projects.xhtml")
    public String redirectToReact() {
        return "redirect:/ui/projects";
    }
}
```

### State Management Migration

**Old (JSF Managed Bean)**:
```java
@Named
@ViewScoped
public class ProjectsBean implements Serializable {
    private List<Project> projects;

    @PostConstruct
    public void init() {
        projects = projectService.findAll();
    }

    public List<Project> getProjects() {
        return projects;
    }
}
```

**New (React with Zustand)**:
```typescript
// src/stores/projectStore.ts
import { create } from 'zustand';
import { api } from '../services/api';

interface ProjectStore {
  projects: Project[];
  loading: boolean;
  loadProjects: () => Promise<void>;
}

export const useProjectStore = create<ProjectStore>((set) => ({
  projects: [],
  loading: false,

  loadProjects: async () => {
    set({ loading: true });
    try {
      const projects = await api.get<Project[]>('/api/projects');
      set({ projects });
    } finally {
      set({ loading: false });
    }
  },
}));
```

### Testing React Components

```typescript
// src/pages/Projects/ProjectList.test.tsx
import { render, screen, waitFor } from '@testing-library/react';
import { ProjectList } from './ProjectList';
import { api } from '../../services/api';

jest.mock('../../services/api');

describe('ProjectList', () => {
  it('should render projects', async () => {
    const mockProjects = [
      { id: '1', name: 'Project 1', version: '1.0' },
      { id: '2', name: 'Project 2', version: '2.0' },
    ];

    (api.get as jest.Mock).mockResolvedValue(mockProjects);

    render(<ProjectList />);

    await waitFor(() => {
      expect(screen.getByText('Project 1')).toBeInTheDocument();
      expect(screen.getByText('Project 2')).toBeInTheDocument();
    });
  });
});
```

### Migration Checklist

- [ ] Identify JSF page to migrate
- [ ] Create REST API backend
- [ ] Create React component
- [ ] Add state management (if needed)
- [ ] Add routing
- [ ] Add internationalization (i18n)
- [ ] Write unit tests
- [ ] Write integration tests
- [ ] Update navigation/links
- [ ] Remove old JSF page
- [ ] Update documentation

---

## Repository Migration

### Supported Repository Types

| Type | Use Case | Pros | Cons |
|------|----------|------|------|
| **File System** | Development | Simple, fast | No versioning |
| **Git** | Version control | Full history, branching | More complex |
| **JDBC** | Database storage | Transactional | Limited to DB size |
| **AWS S3** | Cloud storage | Scalable, durable | Network latency |
| **Azure Blob** | Azure cloud | Azure integration | Network latency |

### Migrating Between Repository Types

#### From File System to Git

```bash
# 1. Initialize Git repository
cd /path/to/repository
git init
git add .
git commit -m "Initial commit"

# 2. Update application.properties
repository.type=git
repository.uri=file:///path/to/repository
```

#### From File System to AWS S3

```bash
# 1. Create S3 bucket
aws s3 mb s3://openl-repository

# 2. Upload existing files
aws s3 sync /path/to/repository s3://openl-repository

# 3. Update application.properties
repository.type=aws
repository.aws.bucket=openl-repository
repository.aws.region=us-east-1
repository.aws.access-key=YOUR_ACCESS_KEY
repository.aws.secret-key=YOUR_SECRET_KEY
```

#### From JDBC to Git

```bash
# 1. Export projects from database
# (Custom script or API call)

# 2. Initialize Git repository
git init
# Copy exported projects to repository
git add .
git commit -m "Migrated from JDBC"

# 3. Update configuration
repository.type=git
repository.uri=file:///path/to/repository
```

### Project Tags Migration

**Issue**: EPBDS-15267 - Migrate project tags from database to repository files

OpenL Tablets 5.26+ automatically migrates project tags:

```java
// ProjectTagsMigrator
// Migrates tags from database to tags.properties file
// Supports both folder-based and archive-based repositories

// tags.properties format:
project.tag.environment=production
project.tag.version=2.1.0
project.tag.owner=team-a
```

**Migration Trigger**: Runs automatically on first startup after upgrade

**Location**: Tags stored in `tags.properties` within each project

---

## Configuration Migration

### From application.properties to YAML

**Old** (`application.properties`):
```properties
server.port=8080
spring.datasource.url=jdbc:postgresql://localhost/openl
repository.design.path=/path/to/design
```

**New** (`application.yml`):
```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:postgresql://localhost/openl

repository:
  design:
    path: /path/to/design
```

### Security Configuration Migration

#### From CAS to OAuth2

**Old CAS Configuration**:
```properties
security.cas.enabled=true
security.cas.server-url=https://cas.example.com
```

**New OAuth2 Configuration**:
```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          keycloak:
            client-id: openl-tablets
            client-secret: secret
            authorization-grant-type: authorization_code
            scope: openid, profile, email
        provider:
          keycloak:
            issuer-uri: https://keycloak.example.com/realms/openl
```

#### From SAML to OAuth2

**Migration Steps**:

1. **Export user data**:
   ```sql
   SELECT username, email, first_name, last_name
   FROM users;
   ```

2. **Configure OAuth2 provider** (Keycloak, Azure AD, etc.)

3. **Import users** into OAuth2 provider

4. **Update configuration**:
   ```yaml
   spring:
     security:
       oauth2:
         client:
           registration:
             azure:
               client-id: ${AZURE_CLIENT_ID}
               client-secret: ${AZURE_CLIENT_SECRET}
   ```

5. **Test authentication**

6. **Migrate group/role mappings**

---

## API Migration

### From Internal API to Public API

**Internal API** (Subject to change):
```java
// Don't use in external code
IProject project = repository.getProject("my-project");
```

**Public API** (Stable):
```java
@RestController
@RequestMapping("/api/projects")
public class ProjectsController {

    @GetMapping("/{name}")
    public ProjectDTO getProject(@PathVariable String name) {
        return projectService.findByName(name);
    }
}
```

---

## Migration Troubleshooting

### Common Issues

#### 1. Database Migration Failed

**Symptom**: Application won't start after upgrade

**Solution**:
```bash
# Check Flyway history
SELECT * FROM openl_security_flyway;

# Repair if needed
flyway repair

# Or restore from backup
psql openl < backup.sql
```

#### 2. Repository Access Denied After Migration

**Symptom**: Cannot access projects after repository migration

**Solution**:
```bash
# Check file permissions
ls -la /path/to/repository

# Fix permissions
chown -R openl:openl /path/to/repository
chmod -R 755 /path/to/repository
```

#### 3. Session Lost After Upgrade

**Symptom**: Users logged out after deployment

**Solution**:
- Expected behavior (session format may change)
- Users need to log in again
- Consider session migration if critical

#### 4. Configuration Not Applied

**Symptom**: Old configuration still in effect

**Solution**:
```bash
# Clear configuration cache
rm -rf /path/to/work/directory

# Restart application
systemctl restart openl-tablets
```

### Rollback Procedures

#### Version Rollback

```bash
# 1. Stop application
systemctl stop openl-tablets

# 2. Restore previous version
cp openl-tablets-5.26.1.war /path/to/deployment

# 3. Restore database (if schema changed)
psql openl < backup_pre_migration.sql

# 4. Restore configuration
cp -r /backup/config/* /path/to/config/

# 5. Start application
systemctl start openl-tablets
```

#### Database Rollback

```bash
# Restore from backup
psql openl < backup.sql

# Or use point-in-time recovery (if available)
```

---

## Best Practices

### Before Migration

1. **Backup Everything**: Database, repository, configuration
2. **Test in Staging**: Never migrate production directly
3. **Read Release Notes**: Understand all changes
4. **Plan Downtime**: Schedule maintenance window
5. **Prepare Rollback**: Have rollback plan ready

### During Migration

1. **Monitor Logs**: Watch for errors during migration
2. **Verify Each Step**: Don't skip verification
3. **Document Issues**: Record any problems encountered
4. **Communicate**: Keep stakeholders informed

### After Migration

1. **Verify Functionality**: Test all critical features
2. **Check Performance**: Monitor performance metrics
3. **Review Logs**: Check for warnings or errors
4. **Update Documentation**: Document any changes
5. **Gather Feedback**: Get user feedback

---

## Related Documentation

- [Testing Guide](testing-guide.md) - Testing migrations
- [Docker Guide](../operations/docker-guide.md) - Docker migration
- [CI/CD Pipeline](../operations/ci-cd.md) - Automated migrations
- [Troubleshooting](../onboarding/troubleshooting.md) - Migration issues

---

**Last Updated**: 2025-11-05
**Maintainer**: OpenL Tablets Team
