## Deploying OpenL Rule Services

### Download

Download the Rule Services WAR from [openl-tablets.org/downloads](https://openl-tablets.org/downloads).

### Deployment

1. Copy to `<TOMCAT_HOME>/webapps/`
2. Rename to the desired context (e.g., `ruleservice.war`)
3. Restart Tomcat

### Configuration

Configure the data source for deployed rules. It is a repository, which Rule Services reads the deployed projects from.

**Supported Sources:**
- **Classpath JAR**: Projects packed in the JAR files of the application. This is the default source.
- **File system**: A local folder
- **JDBC**: Database storage (shared with OpenL Studio)
- **JNDI**: Database through a data source of the application server
- **AWS S3**, **Azure Blob Storage**, and **Git**: Available in the *all* web application of Rule Services

**Key Configuration File:**
- `application.properties`, placed in the working directory, in a `config` folder of it, or in the user's home directory. See [Externalized Configuration](https://openl-tablets.github.io/openl-tablets/developer-guides/externalized-config) for all locations.
- The default values of all properties are published at `<context path>/admin/config/application.properties` of a running instance.

**Example Configuration:**
```properties
# Production data source
production-repository.factory = repo-jdbc
production-repository.uri = jdbc:mysql://localhost:3306/openl
production-repository.login = openl_user
production-repository.password = ***
```

For all data sources and their settings, see [OpenL Rule Services Configuration](../rule-services/configuration.md#configuring-a-data-source).

---
