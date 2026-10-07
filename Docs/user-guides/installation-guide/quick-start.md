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

Download from [tomcat.apache.org](https://tomcat.apache.org/).

**Windows Installation:**
- Use ZIP distribution or Service Installer
- Extract to a directory without spaces (e.g., `C:\tomcat`)

**Configuration Steps:**

1. **Configure JVM Options** - Set heap memory settings:
   ```
   -Xms512m -Xmx2000m
   ```

2. **Set CATALINA_HOME** environment variable (optional but recommended)

**Starting Tomcat:**
- **Windows**: Run `bin\startup.bat` or start the service
- **Linux/macOS**: Run `bin/startup.sh`

---

### 3. Deploy OpenL Studio

#### Download OpenL Studio WAR

Download `openl-studio.war` from the latest [OpenL Tablets release on GitHub](https://github.com/openl-tablets/openl-tablets/releases).

#### Deploy to Tomcat

1. Copy `openl-studio.war` to `<TOMCAT_HOME>\webapps\webstudio.war`. The file name sets the context path.
2. Start or restart Tomcat

The WAR will auto-extract to a directory with the same name.

#### Access OpenL Studio

Navigate to: `http://localhost:8080/webstudio`

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

**JDBC Drivers:**
- MySQL and MariaDB: MySQL Connector/J or MariaDB Connector/J
- PostgreSQL: PostgreSQL JDBC Driver
- Oracle: Oracle JDBC Driver (`ojdbc`)
- MS SQL Server: Microsoft JDBC Driver for SQL Server

#### Database Connection Configuration

The database holds the users of OpenL Studio. In **Administration → System Settings → Database Configuration**, configure the connection:
- Database URL (e.g., `jdbc:mysql://localhost:3306/openl`)
- Login
- Password
- Maximum Pool Size

Or set `db.url`, `db.user`, and `db.password` in the `application.properties` file.

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

For multi-user mode, in **Administration → Security**, list the users with administrator privileges in **Administrators**. The default group of the newly created users is set there as well.

#### Step 2: Repository Configuration

Select repository type for storing rules:

- **JDBC**: Database storage (recommended for multi-user)
- **Git**: Version control integration
- **AWS S3**: Cloud storage
- **Azure Blob Storage**: Cloud storage
- **JNDI**: Enterprise datasource

---
