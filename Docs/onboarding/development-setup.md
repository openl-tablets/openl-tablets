# OpenL Tablets Development Setup

**Last Updated**: 2025-11-05
**Estimated Setup Time**: 30-60 minutes

---

## Prerequisites

### Required Software

| Software | Minimum Version | Recommended | Download |
|----------|----------------|-------------|----------|
| **JDK** | 21 | 21+ | https://adoptium.net/ |
| **Maven** | 3.9.9 | 3.9.11 | https://maven.apache.org/download.cgi |
| **Git** | 2.x | Latest | https://git-scm.com/downloads |
| **Docker** | 27.5.0 | Latest | https://www.docker.com/get-started |
| **Docker Compose** | 2.32.4 | Latest | Included with Docker Desktop |

### System Requirements

- **RAM**: 4 GB minimum (8 GB recommended for development)
- **Disk Space**: 5 GB free (for source, build artifacts, Docker images)
- **OS**: Linux, macOS, or Windows (with WSL2 recommended)

### Optional but Recommended

| Tool | Purpose | Download |
|------|---------|----------|
| **IntelliJ IDEA** | Java IDE | https://www.jetbrains.com/idea/ |
| **VS Code** | Frontend development | https://code.visualstudio.com/ |
| **Node.js** | Frontend builds (24.9.0) | Handled by frontend-maven-plugin |
| **Postman** | API testing | https://www.postman.com/downloads/ |

---

## Step 1: Clone the Repository

```bash
# Clone from GitHub
git clone https://github.com/openl-tablets/openl-tablets.git
cd openl-tablets

# Or if you have SSH configured
git clone git@github.com:openl-tablets/openl-tablets.git
cd openl-tablets
```

**Repository Size**: ~100 MB (excluding build artifacts)

---

## Step 2: Verify Prerequisites

### Check Java

```bash
java -version
# Should show: openjdk version "21" or higher
```

**Important**: Ensure `JAVA_HOME` is set correctly:

```bash
# Linux/macOS
export JAVA_HOME=/path/to/jdk-21
export PATH=$JAVA_HOME/bin:$PATH

# Windows (PowerShell)
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
```

### Check Maven

```bash
mvn -version
# Should show: Apache Maven 3.9.9 or higher
# Should also show the correct Java version
```

### Check Docker

```bash
docker --version
# Should show: Docker version 27.5.0 or higher

docker compose version
# Should show: Docker Compose version 2.32.4 or higher
```

---

## Step 3: Build the Project

### Option A: Full Build (30+ minutes)

Build everything with all tests:

```bash
mvn clean install
```

**Expected Output**:
- All tests pass
- WAR files in `target/` directories
- Total time: 10-30 minutes (depending on hardware)

### Option B: Quick Build (5-10 minutes) ⭐ Recommended for First Build

Build with minimal tests:

```bash
mvn clean install -Dquick -DnoPerf -T1C
```

**Flags Explained**:
- `-Dquick` - Skip heavy/non-essential tests
- `-DnoPerf` - Run tests without extreme memory limitations
- `-T1C` - Multi-threaded build (1 thread per CPU core)

### Option C: Skip Tests (2-5 minutes)

Build without running tests:

```bash
mvn clean install -DskipTests
```

**Use Case**: When you just need artifacts quickly

### Build Options Reference

| Flag | Effect |
|------|--------|
| `-DskipTests` | Skip all tests |
| `-Dquick` | Skip heavy tests |
| `-DnoPerf` | No memory limits on tests |
| `-DnoDocker` | Skip Docker-based tests |
| `-T1C` | Multi-threaded build |
| `-Dmaven.javadoc.skip=true` | Skip JavaDoc generation |

### Common Build Issues

**Issue**: `OutOfMemoryError` during build

**Solution**: Increase Maven memory:
```bash
export MAVEN_OPTS="-Xmx4g -XX:MaxMetaspaceSize=1g"
```

**Issue**: Tests fail due to port conflicts

**Solution**: Kill processes on ports 8080, 8081:
```bash
# Linux/macOS
lsof -ti:8080 | xargs kill -9
lsof -ti:8081 | xargs kill -9

# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

**Issue**: Docker tests fail

**Solution**: Ensure Docker is running:
```bash
docker ps
```

If Docker isn't available, skip Docker tests:
```bash
mvn clean install -DnoDocker
```

---

## Step 4: Run OpenL Locally

### Option A: Docker Compose (Easiest) ⭐ Recommended

Run pre-built Docker images:

```bash
docker compose up
```

**Access**:
- OpenL Studio: http://localhost
- Default credentials: admin/admin

**Stop**:
```bash
docker compose down
```

### Option B: Run from Source

#### Running OpenL Studio

```bash
cd STUDIO/org.openl.rules.webstudio
mvn jetty:run
```

**Access**: http://localhost:8080
**Default credentials**: admin/admin

**Stop**: Press `Ctrl+C`

#### Running RuleService

```bash
cd WSFrontend/org.openl.rules.ruleservice.ws
mvn jetty:run -Djetty.port=8081
```

**Access**: http://localhost:8081
**Swagger UI**: http://localhost:8081/swagger-ui/

### Option C: Run as Spring Boot Application

```bash
# Studio
cd STUDIO/org.openl.rules.webstudio
mvn spring-boot:run

# RuleService
cd WSFrontend/org.openl.rules.ruleservice.ws
mvn spring-boot:run -Dserver.port=8081
```

---

## Step 5: IDE Setup

### IntelliJ IDEA Setup

#### 1. Import Project

1. **File** → **Open**
2. Select `/path/to/openl-tablets/pom.xml`
3. Choose **Open as Project**
4. Wait for Maven import to complete

#### 2. Configure JDK

1. **File** → **Project Structure** → **Project**
2. Set **Project SDK** to JDK 21
3. Set **Project language level** to 21

#### 3. Configure Maven

1. **File** → **Settings** → **Build, Execution, Deployment** → **Build Tools** → **Maven**
2. Set **Maven home path** to your Maven installation
3. Set **VM options for importer**: `-Xmx2g`

#### 4. Configure Code Style

1. **File** → **Settings** → **Editor** → **Code Style** → **Java**
2. Use default Java conventions
3. **Imports**:
   - Class count to use import with '*': 99
   - Names count to use static import with '*': 99

#### 5. Enable Annotation Processing

1. **File** → **Settings** → **Build, Execution, Deployment** → **Compiler** → **Annotation Processors**
2. Check **Enable annotation processing**

#### 6. Recommended Plugins

- **Maven Helper** - Dependency analysis
- **SonarLint** - Code quality
- **CheckStyle-IDEA** - Code style checking

### VS Code Setup (for Frontend)

#### 1. Open Frontend Project

```bash
cd STUDIO/studio-ui
code .
```

#### 2. Install Extensions

- **ESLint** - Linting
- **Prettier** - Code formatting
- **TypeScript and JavaScript Language Features** (built-in)
- **React DevTools** - React debugging

#### 3. Configure Settings

Create `.vscode/settings.json`:
```json
{
  "editor.formatOnSave": true,
  "editor.codeActionsOnSave": {
    "source.fixAll.eslint": true
  },
  "typescript.tsdk": "node_modules/typescript/lib"
}
```

---

## Step 6: Verify Installation

### Test Core Engine

Create a simple test:

```java
import org.openl.rules.runtime.RulesEngineFactory;

public class TestOpenL {
    public static void main(String[] args) {
        // This will compile and run rules from an Excel file
        RulesEngineFactory<MyRules> factory =
            new RulesEngineFactory<>("rules/MyRules.xlsx", MyRules.class);

        MyRules rules = factory.newInstance();
        System.out.println("OpenL is working!");
    }
}
```

### Test OpenL Studio

1. Navigate to http://localhost:8080
2. Login with admin/admin
3. Create a new project
4. Add a simple decision table
5. Run tests

### Test RuleService

1. Navigate to http://localhost:8081/swagger-ui/
2. Explore available REST endpoints
3. Try executing a rule via REST API

---

## Step 7: Database Setup (Optional)

By default, OpenL uses H2 embedded database. For production or testing with other databases:

### PostgreSQL Setup

```bash
# Using Docker
docker run --name openl-postgres \
  -e POSTGRES_PASSWORD=openl \
  -e POSTGRES_USER=openl \
  -e POSTGRES_DB=openl \
  -p 5432:5432 \
  -d postgres:16
```

**Configuration** in `application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/openl
spring.datasource.username=openl
spring.datasource.password=openl
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
```

### MySQL Setup

```bash
# Using Docker
docker run --name openl-mysql \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=openl \
  -e MYSQL_USER=openl \
  -e MYSQL_PASSWORD=openl \
  -p 3306:3306 \
  -d mysql:8
```

**Configuration**:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/openl
spring.datasource.username=openl
spring.datasource.password=openl
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
```

---

## Step 8: Configure Git

### Git Hooks (Optional)

Set up pre-commit hooks for code quality:

```bash
# In project root
cat > .git/hooks/pre-commit << 'EOF'
#!/bin/bash
# Run quick build before commit
mvn clean test -Dquick -DnoPerf
EOF

chmod +x .git/hooks/pre-commit
```

### Git Configuration

```bash
# Set your identity
git config user.name "Your Name"
git config user.email "your.email@example.com"

# Enable auto-correct
git config help.autocorrect 1

# Set default editor
git config core.editor "vim"  # or "code --wait" for VS Code
```

---

## Common Development Tasks

### Running Specific Module Tests

```bash
# Test core engine
cd DEV/org.openl.rules
mvn test

# Test OpenL Studio
cd STUDIO/org.openl.rules.webstudio
mvn test

# Test RuleService
cd WSFrontend/org.openl.rules.ruleservice
mvn test
```

### Building Specific Modules

```bash
# Build only DEV module
cd DEV
mvn clean install -DskipTests

# Build only STUDIO
cd STUDIO
mvn clean install -DskipTests
```

### Running Integration Tests

```bash
cd ITEST/itest.smoke
mvn verify
```

### Generating JavaDoc

```bash
mvn javadoc:aggregate
# Output: target/site/apidocs/
```

### Checking Dependencies

```bash
# Show dependency tree
mvn dependency:tree

# Check for updates
mvn versions:display-dependency-updates

# Check for vulnerabilities (OWASP)
mvn dependency-check:check
```

### Frontend Development

```bash
cd STUDIO/studio-ui

# Install dependencies (done by Maven, but can be manual)
npm install

# Run dev server with hot reload
npm start

# Build for production
npm run build

# Run linter
npm run lint

# Run tests
npm test
```

---

## Troubleshooting

### Build Failures

**Symptom**: Build fails with compilation errors

**Solution**:
1. Ensure JDK 21 is being used: `mvn -version`
2. Clean and rebuild: `mvn clean install -U`
3. Check for IDE-specific issues (reimport Maven project)

### Test Failures

**Symptom**: Tests fail intermittently

**Solution**:
1. Run tests in isolation: `mvn test -Dtest=SpecificTest`
2. Check for port conflicts
3. Increase test timeout values
4. Run with `-DnoDocker` if Docker tests fail

### Memory Issues

**Symptom**: `OutOfMemoryError` during build or tests

**Solution**:
```bash
export MAVEN_OPTS="-Xmx4g -XX:MaxMetaspaceSize=1g -XX:+UseG1GC"
```

### Docker Issues

**Symptom**: Docker tests fail or timeout

**Solution**:
1. Verify Docker is running: `docker ps`
2. Pull required images: `docker compose pull`
3. Clean Docker cache: `docker system prune -a`
4. Increase Docker resources (Docker Desktop settings)

---

## Environment Variables

### Required

```bash
export JAVA_HOME=/path/to/jdk-21
export PATH=$JAVA_HOME/bin:$PATH
```

### Optional but Recommended

```bash
# Maven memory settings
export MAVEN_OPTS="-Xmx4g -XX:MaxMetaspaceSize=1g"

# Maven local repository (to avoid conflicts)
export MAVEN_LOCAL_REPO=~/.m2/repository

# Skip Docker tests by default
export NO_DOCKER=true
```

### For Windows (PowerShell)

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
$env:MAVEN_OPTS = "-Xmx4g -XX:MaxMetaspaceSize=1g"
```

---

## Next Steps

1. ✅ **Explore**: Browse code with [Codebase Tour](/docs/onboarding/codebase-tour.md)
2. ✅ **Understand**: Read [DEV Module Overview](/docs/analysis/dev-module-overview.md)
3. ✅ **Contribute**: Check `/CONTRIBUTING.md` (if available)
4. ✅ **Ask**: Join community forums or Slack (check website)

---

## Quick Reference Commands

```bash
# Full build
mvn clean install

# Quick build
mvn clean install -Dquick -DnoPerf -T1C

# Skip tests
mvn clean install -DskipTests

# Run Studio
cd STUDIO/org.openl.rules.webstudio && mvn jetty:run

# Run RuleService
cd WSFrontend/org.openl.rules.ruleservice.ws && mvn jetty:run -Djetty.port=8081

# Docker
docker compose up

# Clean everything
mvn clean && docker compose down -v
```

---

**Ready to develop! Happy coding!**
