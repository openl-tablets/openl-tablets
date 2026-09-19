# OpenL Tablets Migration Guide

**Last Updated**: 2025-11-05
**Version**: 6.0.0-SNAPSHOT

---

## Table of Contents

- [Overview](#overview)
- [Version Migration](#version-migration)
- [Database Migration](#database-migration)
- [Repository Migration](#repository-migration)
- [Configuration Migration](#configuration-migration)
- [API Migration](#api-migration)
- [Migration Troubleshooting](#migration-troubleshooting)

---

## Overview

This guide covers various migration scenarios in OpenL Tablets:

1. **Version Migration**: Upgrading from one version to another
2. **Database Migration**: Schema changes and data migration
3. **Repository Migration**: Changing repository backends
4. **Configuration Migration**: Updating application configuration

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
| 5.27.15 | 6.5.0 | Direct | ✅ Yes |
| 6.0.x - 6.4.x | 6.5.0 | Direct | ✅ Yes |
| 5.27.9 and older | 6.5.0 | Via 5.27.15 | ⚠️ Multi-step |

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

### Migration to 6.5.0

**Key Changes**:

1. **Database Migrations Moved to Liquibase**
   - Old: Flyway, tracked in the `openl_security_flyway` table
   - New: **Liquibase**, tracked in `DATABASECHANGELOG`
   - The first startup adopts the existing schema, applies only the changes made after 6.0.0, and drops the
     history table of the previous tool

2. **Upgrades Start at 5.27.15**
   - The change log creates the current schema on an empty database, and converts the permission model of a
     database of 5.27.15 to the one 6.0.0 introduced
   - A database left behind by a release before 5.27.10, which still keeps its group hierarchy in
     `OpenL_Group2Group`, is **refused**: the application stops instead of starting on a schema it would
     misread, and the database is left untouched
   - Action: Such an installation has to be upgraded to 5.27.15 or 6.0.0 first

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

### Liquibase Change Logs

OpenL Tablets keeps the security schema under **Liquibase**, and applies the change logs on startup.

**Change log location**:
```
STUDIO/org.openl.security.standalone/resources/db/changelog/
├── db.changelog-master.xml            # Types per database, and the list of change logs below
├── install/                           # The tables of the current release, one file per area
│   ├── db.changelog-users.xml         # Users, groups, authorities, memberships, settings, external groups
│   ├── db.changelog-tags.xml          # Tag types, tags and tag templates
│   ├── db.changelog-lock.xml          # Cluster-wide lock registry
│   ├── db.changelog-pat-tokens.xml    # Personal access tokens
│   └── db.changelog-acl.xml           # Access control lists and the default permissions
└── upgrade/                           # What a database of an earlier release still needs
    ├── db.changelog-6.0.0.xml         # Conversion of the permission model of 5.27.15
    ├── db.changelog-6.4.0.xml         # The column 6.4.0 added
    └── db.changelog-6.5.0.xml         # Changes released in 6.5.0
```

`install/` describes the schema itself, split by area so that each file covers one part of the product;
`upgrade/` holds one file per release that changed something an earlier release had already created. The
master change log lists them in order. Each change is written once and rendered for the database in use, so
the same change log serves H2, MySQL, MariaDB, SQL Server, Azure SQL Database, Oracle and PostgreSQL.

Liquibase records what it has applied in its own `DATABASECHANGELOG` table and guards concurrent startups with
`DATABASECHANGELOGLOCK`.

An empty database gets the current schema created in one step, instead of replaying the release-by-release
history that produced it. An existing database keeps every table it already has, and receives only the ones it
never had and the changes of the releases after it, so an upgrade can start from any of these:

| Database handed over by | What the first startup does |
|-------------------------|-----------------------------|
| nothing (empty) | Creates the schema, the default groups and the default permissions |
| 5.27.15 | Adds the personal access tokens and the index over the external group names, and converts the permissions into the roles of the current model |
| 6.0.0 - 6.3.x | Adds the moment of the last sign-in |
| 6.4.x | Adopts the schema as it is |

Every change set of `upgrade/` names the release that introduced it, so the two directories together still
read as the history of the schema.

> [!Note]
> A database of a release before **5.27.10** is refused on startup: it still keeps the group hierarchy in
> `OpenL_Group2Group`, and the change set that flattens it is no longer part of the change log. Upgrade such an
> installation to 5.27.15 or 6.0.0 first, and let it start once, before upgrading to this version. Releases
> between 5.27.10 and 5.27.14 left the same schema as 5.27.15 and are converted the same way, though 5.27.15
> is the oldest release the conversion is tested against.

### Adopting a Database of an Earlier Release

Releases up to 6.4.0 migrated the schema with Flyway and tracked it in the `openl_security_flyway` table. The
first startup of 6.5.0 recognizes such a database, records as already applied every change set whose change is
already there, applies the rest, and drops the history table of the previous tool. Nothing has to be prepared
by hand, and the users, groups and projects of the installation are untouched.

The change log creates only the tables the application reads. `OpenL_Projects` and `OpenL_Project_Tags` are
not among them: they belong to the releases before 6.0.0, an upgraded database brings them along, and the
project tag migration described above empties and drops them.

An adopted schema also keeps the column types the release that created it chose. A new installation declares
the national character type (`NVARCHAR` on SQL Server, `NVARCHAR2` on Oracle) only for the columns that hold
a name, a description or a mail address, and a plain `VARCHAR` or a fixed-length `CHAR` for the ones the
product fills itself, such as a password hash, a lock key or a token identifier. An upgraded database keeps
the wider types it already had. Both serve the application, and no column is converted.

### Key Database Changes

| Change log | Change set | Description |
|------------|------------|-------------|
| `install/db.changelog-users.xml` | `users` | Users, groups, group authorities and per-user settings |
| `install/db.changelog-users.xml` | `users-external-groups-index` | Index over the external group names |
| `install/db.changelog-tags.xml` | `tags` | The tags a project can carry, and the tag templates |
| `install/db.changelog-lock.xml` | `lock` | Cluster-wide lock registry |
| `install/db.changelog-pat-tokens.xml` | `pat-tokens` | Personal access tokens |
| `install/db.changelog-acl.xml` | `acl-*` | Access control lists, and the seed data of an empty installation |
| `upgrade/db.changelog-6.0.0.xml` | `6.0.0-drop-unlock-authorities` | Removal of the unlock authorities of the previous model |
| `upgrade/db.changelog-6.0.0.xml` | `6.0.0-role-based-permissions` | Conversion of the permissions of 5.27.15 into the current roles |
| `upgrade/db.changelog-6.4.0.xml` | `6.4.0-user-last-login` | Moment of the last successful sign-in |
| `upgrade/db.changelog-6.5.0.xml` | `6.5.0-drop-flyway-history` | Removal of the history table of the previous migration tool |

The refusal of a database older than 5.27.10 is a precondition of `db.changelog-master.xml` itself, not a
change set, so nothing is applied and nothing is recorded when it stops the migration.

### Running Migrations Manually

The application migrates on startup, so this is only needed to inspect a database or to prepare a change for a
review:

```bash
# Show which change sets are still missing
liquibase --url=jdbc:postgresql://localhost/openl --username=openl --password=secret \
          --changelog-file=db/changelog/db.changelog-master.xml status

# Write the SQL of the pending change sets instead of running it
liquibase --url=jdbc:postgresql://localhost/openl --username=openl --password=secret \
          --changelog-file=db/changelog/db.changelog-master.xml update-sql
```

### Rollback Strategy

A change set is written forward only, so a downgrade is a restore:

1. **Restore the backup taken before the upgrade**:
   ```bash
   psql openl < openl_backup_20250101.sql
   ```

2. **Or add a change set that reverses the change** and release it, which keeps the history of every
   installation consistent.

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

```sql
-- Which change sets have been applied, and in which order
SELECT id, author, exectype, dateexecuted FROM databasechangelog ORDER BY orderexecuted;
```

Liquibase stops at the failed change set and applies nothing after it. Fix the cause and start the application
again: the change sets already recorded are skipped, and the failed one is retried.

#### Startup Blocked by the Change Log Lock

A node that was killed during a migration leaves the lock behind, and every later startup waits for it:

```sql
-- Release the lock of a node that is no longer running
UPDATE databasechangeloglock SET locked = FALSE, lockgranted = NULL, lockedby = NULL WHERE id = 1;
```

#### Checksum Mismatch

An already applied change set was edited afterwards. Restore the change set to the form that was released, and
ship the correction as a new change set instead.

#### Database Older Than the Oldest Supported Release

The startup stops with a message naming 5.27.10, and the database is left as it was. The installation is on a
release before 5.27.10, which still keeps its group hierarchy in `OpenL_Group2Group`. Upgrade it to 5.27.15 or
6.0.0 first, let it start once so that it flattens the hierarchy, and then upgrade to this version.

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

Project tags moved from the OpenL Studio database into a `tags.properties` file of each project in 6.0.0. An
upgrade from an earlier version moves them automatically:

- The tags of every project are written into its `tags.properties` file, both in a folder-based and in an
  archive-based repository. A project whose file is already there is left untouched.
- The legacy `OpenL_Projects` and `OpenL_Project_Tags` tables are dropped once every project has its tags in its
  own file. A project the repository does not hold — its repository is no longer configured, or the project is
  gone from it — keeps the tables, and with them its tags, instead of losing them.

The file holds one line per tag type:

```properties
Environment=production
Owner=team-a
```

**Migration Trigger**: The first startup after an upgrade from a version below 6.0.0. An installation upgraded
from 6.0.0 or newer, an installation without a database, and one whose legacy tables are already dropped are left
untouched.

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
```sql
-- Find the change set the migration stopped at
SELECT id, author, exectype, dateexecuted FROM databasechangelog ORDER BY orderexecuted;
```

Fix the cause the log names and start the application again, or restore the backup taken before the upgrade:

```bash
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
