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

#### Access OpenL Studio

Navigate to: http://localhost:8080/webstudio

OpenL Studio opens in single-user mode, ready to use. Configure repositories, security, and other settings from the [Administration](../openl-studio/administration/) UI (see step 5).

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

In **Administration → Repositories Settings**, configure the database connection:
- Database URL (e.g., `jdbc:mysql://localhost:3306/openl`)
- Username
- Password
- Driver class name

---

### 5. Configure Initial Settings

On first launch, OpenL Studio starts in **single-user mode** and is ready to use. Configure your
instance from the [Administration](../openl-studio/administration/) UI (**Administration → System Settings** and **Security**):

#### Step 1: User Mode Selection

Choose one of the following modes:

- **Single-User**: Local development, no authentication
- **Multi-User**: Database-backed, with user management
- **Active Directory**: Enterprise directory-based authentication (AD/LDAP)
- **SSO: SAML**: Single Sign-On via a SAML 2.0 identity provider
- **SSO: OIDC (OAuth2)**: Single Sign-On via an OAuth2/OIDC identity provider

For multi-user mode, set up the administrator account:

- Admin username
- Admin password
- Email (optional)

#### Step 2: Repository Configuration

Select repository type for storing rules:

- **JDBC**: Database storage (recommended for multi-user)
- **Git**: Version control integration
- **AWS S3**: Cloud storage
- **Azure Blob Storage**: Cloud storage
- **File System**: Local or network drive (single-user)
- **JNDI**: Enterprise datasource

---
