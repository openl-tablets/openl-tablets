## Integration: Studio + Rule Services

For a complete development and production workflow:

### 1. Shared Repository

Configure both OpenL Studio and Rule Services to use the same **deployment** repository:

- OpenL Studio writes deployed rules to deployment repository
- Rule Services reads from the same deployment repository

### 2. Database Repository (Recommended)

**OpenL Studio Configuration:**
```properties
repository.type=jdbc
repository.uri=jdbc:mysql://localhost:3306/openl_design
repository.login=openl_user
repository.password=***

deployment.repository.type=jdbc
deployment.repository.uri=jdbc:mysql://localhost:3306/openl_deploy
deployment.repository.login=openl_user
deployment.password=***
```

**Rule Services Configuration:**
```properties
production.repository.type=jdbc
production.repository.uri=jdbc:mysql://localhost:3306/openl_deploy
production.repository.login=openl_user
production.repository.password=***
```

### 3. Workflow

1. **Develop** rules in OpenL Studio
2. **Deploy** from Studio to deployment repository
3. **Rule Services** automatically picks up deployed rules
4. **Execute** rules via REST APIs

---

