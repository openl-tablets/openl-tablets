# OpenL Tablets BRMS Installation Guide

**Status**: ✅ Migrated from OpenLdocs
**Source**: [OpenLdocs Installation Guide](https://github.com/EISTW/OpenLdocs/blob/master/docs/documentation/guides/installation_guide.md)
**Last Updated**: 2025-11-05

---

## Overview

OpenL Tablets is a Business Rules Management System built on Excel-based tables, enabling business documents with logic specifications to function as executable code. The platform provides OpenL Studio for rule creation and management, plus OpenL Rule Services for application integration.

---

## System Requirements

### Operating Systems
- **Windows**: 11+
- **Linux**: Ubuntu 22.4+
- **macOS**: 15+

### Supported Browsers
- **Microsoft Edge**: 131+
- **Firefox**: 128 ESR+
- **Chrome**: 131+

### Databases
- **MySQL**: 8+
- **MariaDB**: 10.5+
- **MS SQL Server**: 2014+
- **Oracle**: 12c+
- **PostgreSQL**: 11.2+

### Other Software
- **Java**: OpenJDK 11 or 21
- **Application Servers**: Apache Tomcat 9, Jetty 10

### Hardware Requirements
- **RAM**: Minimum 4GB (6GB recommended)
- **Processor**: 1GHz+ (32-bit or 64-bit)
- **Disk Space**: 2GB+ for installation and workspace

---

## Installation Steps

### 1. Install JDK

Download OpenJDK 21 from [adoptium.net](https://adoptium.net/).

**Important Configuration:**
- Set the `JAVA_HOME` environment variable to your installation directory
- **Windows Users**: Avoid installing to Program Files due to space character issues
- Add `%JAVA_HOME%\bin` (Windows) or `$JAVA_HOME/bin` (Linux/macOS) to PATH

**Verification:**
```bash
java -version
```

---

### 2. Install Apache Tomcat

Download from [tomcat.apache.org](http://tomcat.apache.org/).

**Windows Installation:**
- Use ZIP distribution or Service Installer
- Extract to a directory without spaces (e.g., `C:\tomcat`)

**Configuration Steps:**

1. **Configure JVM Options** - Set heap memory settings:
   ```
   -Xms512m -Xmx2000m
   ```

2. **Update server.xml** - Add UTF-8 encoding to Connector elements:
   ```xml
   <Connector port="8080" protocol="HTTP/1.1"
              connectionTimeout="20000"
              redirectPort="8443"
              URIEncoding="UTF-8" />
   ```

3. **Set CATALINA_HOME** environment variable (optional but recommended)

**Starting Tomcat:**
- **Windows**: Run `bin\startup.bat` or start the service
- **Linux/macOS**: Run `bin/startup.sh`

---

### 3. Deploy OpenL Studio

#### Download OpenL Studio WAR

Download the latest WAR file from [openl-tablets.org/downloads](https://openl-tablets.org/downloads).

#### Deploy to Tomcat

1. Copy the WAR file to `<TOMCAT_HOME>\webapps\`
2. Rename to `webstudio.war` if desired (this sets the context path)
3. Start or restart Tomcat

The WAR will auto-extract to a directory with the same name.

#### Access Installation Wizard

Navigate to:
```
http://localhost:8080/webstudio
```

The installation wizard will guide you through initial setup.

---

### 4. Configure Database (Optional)

For multi-user mode, configure an external database.

#### Supported Databases

- MySQL / MariaDB
- PostgreSQL
- Oracle
- MS SQL Server
- H2 (embedded - suitable for development only)

#### JDBC Driver Installation

1. Download the appropriate JDBC driver for your database
2. Copy to `<TOMCAT_HOME>\lib\` directory
3. Restart Tomcat

**Common JDBC Drivers:**
- MySQL: `mysql-connector-java-8.x.x.jar`
- PostgreSQL: `postgresql-42.x.x.jar`
- Oracle: `ojdbc8.jar`
- MS SQL Server: `mssql-jdbc-x.x.x.jre11.jar`

#### Database Connection Configuration

During the installation wizard, provide:
- Database URL (e.g., `jdbc:mysql://localhost:3306/openl`)
- Username
- Password
- Driver class name

---

### 5. Complete Installation Wizard

The installation wizard guides you through:

#### Step 1: User Mode Selection

Choose one of the following modes:

- **Demo Mode**: Quick setup with examples, single user
- **Single-user Mode**: Local development, no authentication
- **Multi-user Mode**: Database-backed, with user management
- **SSO Options**: Active Directory, SAML, OAuth2, CAS

#### Step 2: Repository Configuration

Select repository type for storing rules:

- **JDBC**: Database storage (recommended for multi-user)
- **Git**: Version control integration
- **AWS S3**: Cloud storage
- **File System**: Local or network drive (single-user)
- **JNDI**: Enterprise datasource

#### Step 3: User Credentials

For multi-user mode, set up the administrator account:
- Admin username
- Admin password
- Email (optional)

#### Step 4: Finish

Review settings and complete installation.

---

## Key Configuration Options

### Repository Types

#### JDBC Repository
- Database-backed storage
- Full versioning and history
- Multi-user collaboration
- Requires database setup

#### Git Repository
- Git-based version control
- Integration with GitHub, GitLab, Bitbucket
- Branching and merging support
- Requires Git credentials

#### AWS S3 Repository
- Cloud storage
- Scalable and distributed
- Requires AWS credentials and bucket

### User Authentication Modes

#### Multi-user (Database)
- Built-in user management
- Role-based access control
- Stored in database

#### Active Directory / LDAP
- Enterprise integration
- Centralized authentication
- Group-based permissions

#### SAML
- Single Sign-On (SSO)
- Works with Okta, Auth0, Azure AD
- Federation support

#### OAuth2
- Modern authentication
- Support for Google, GitHub, etc.
- Token-based

#### CAS
- Central Authentication Service
- University and enterprise SSO

---

## Cluster Mode Configuration

For multiple OpenL Studio instances sharing workload:

1. **Before First Launch**: Configure `openl.home.shared` in `application.properties`
2. **Shared Resources**: All instances must access the same:
   - Repository storage
   - Deployment configuration
   - User database
3. **Load Balancer**: Use sticky sessions for proper user experience

**Configuration Example:**
```properties
openl.home.shared=/path/to/shared/openl/home
```

---

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

## Docker Support

Official Docker images are available for quick deployment.

### Available Images

- **OpenL Rule Services**: Production-ready rules execution
- **OpenL Studio**: Rule development environment
- **OpenL Demo**: Complete demo environment

### Docker Hub

Access images at: [hub.docker.com/r/openltablets/](https://hub.docker.com/r/openltablets/)

### Quick Start

```bash
# Run OpenL Studio
docker run -p 8080:8080 openltablets/studio

# Run Rule Services
docker run -p 8080:8080 openltablets/ruleservice

# Run Demo Environment
docker run -p 8080:8080 openltablets/demo
```

### Docker Compose

For complete setup, see [Docker Guide](../../operations/docker-guide.md).

---

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
4. **Execute** rules via REST/SOAP APIs

---

## Troubleshooting

### Common Issues

#### Port 8080 Already in Use
- Change Tomcat port in `server.xml`
- Or stop conflicting application

#### JAVA_HOME Not Set
```bash
# Windows
setx JAVA_HOME "C:\Program Files\Eclipse Adoptium\jdk-21.0.1.12-hotspot"

# Linux/macOS
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
```

#### Out of Memory Errors
- Increase heap size: `-Xmx4096m` or higher
- Monitor with JConsole or VisualVM

#### Database Connection Fails
- Verify JDBC driver is in `<TOMCAT_HOME>\lib\`
- Check database URL, username, password
- Ensure database server is running
- Verify firewall rules allow connection

#### Encoding Issues
- Ensure `URIEncoding="UTF-8"` in Tomcat connector
- Set file encoding: `-Dfile.encoding=UTF-8`

---

## Next Steps

After installation:

1. **Explore Examples**: Check pre-loaded tutorial projects
2. **Read Documentation**:
   - [Demo Package Guide](../demo-package/index.md)
   - [WebStudio User Guide](../webstudio/index.md)
   - [Rule Services Guide](../rule-services/index.md)
3. **Create Your First Project**: Start developing business rules
4. **Configure Security**: Set up proper authentication for production
5. **Set Up Backups**: Regular backups of repository and database

---

## Additional Resources

- **Official Website**: [openl-tablets.org](https://openl-tablets.org)
- **Downloads**: [openl-tablets.org/downloads](https://openl-tablets.org/downloads)
- **GitHub**: [github.com/openl-tablets/openl-tablets](https://github.com/openl-tablets/openl-tablets)
- **Documentation**: [OpenL Tablets Documentation](../../README.md)
- **Configuration Guide**: [Configuration Overview](../../configuration/overview.md)
- **Production Deployment**: [Deployment Guide](../../configuration/deployment.md)

---

**Need Help?**
- [Troubleshooting Guide](../../onboarding/troubleshooting.md)
- [GitHub Issues](https://github.com/openl-tablets/openl-tablets/issues)
- [Community Support](https://github.com/openl-tablets/openl-tablets/discussions)

---

*Migrated from OpenLdocs repository as part of documentation consolidation (Batch 3)*
