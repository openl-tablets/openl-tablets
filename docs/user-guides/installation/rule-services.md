## Deploying OpenL Rule Services

### Download

Download the Rule Services WAR from [openl-tablets.org/downloads](https://openl-tablets.org/downloads).

### Deployment

1. Copy to `<TOMCAT_HOME>\webapps\`
2. Rename to desired context (e.g., `ruleservice.war`)
3. Restart Tomcat

### Configuration

Configure the data source for deployed rules:

**Supported Sources:**
- **File System**: Local folders or ZIP archives
- **Classpath JAR**: Embedded rules
- **JDBC**: Database storage (shared with OpenL Studio)
- **JNDI**: Enterprise datasource
- **AWS S3**: Cloud storage
- **Git**: Version control repository

**Key Configuration File:**
- `application.properties` (Spring Boot configuration)
- Located in `WEB-INF/classes/` or externalized

**Example Configuration:**
```properties
# Production data source
production.repository.type=jdbc
production.repository.uri=jdbc:mysql://localhost:3306/openl
production.repository.login=openl_user
production.repository.password=***
```

---

