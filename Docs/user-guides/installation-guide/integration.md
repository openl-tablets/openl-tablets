## Integration: Studio + Rule Services

For a complete development and production workflow:

### 1. Shared Deployment Repository

Configure both OpenL Studio and Rule Services to use the same **deployment** repository:

- OpenL Studio writes deployed rules to the deployment repository
- Rule Services reads from the same deployment repository

### 2. Database Repository (Recommended)

Both applications need the JDBC driver of the database in their classpath.

**OpenL Studio Configuration:**
```properties
# The deployment repository
production-repository-configs = production
repository.production.name = Deployment
repository.production.$ref = repo-jdbc
repository.production.uri = jdbc:mysql://localhost:3306/openl_deploy
repository.production.login = openl_user
repository.production.password = ***
```

**Rule Services Configuration:**
```properties
production-repository.factory = repo-jdbc
production-repository.uri = jdbc:mysql://localhost:3306/openl_deploy
production-repository.login = openl_user
production-repository.password = ***
```

The design repository of OpenL Studio is a different repository; see [Configuration](configuration.md).

### 3. Workflow

1. **Develop** rules in OpenL Studio
2. **Deploy** from Studio to the deployment repository
3. **Rule Services** detects the new deployment in the repository and publishes the services
4. **Execute** rules via REST APIs

---
