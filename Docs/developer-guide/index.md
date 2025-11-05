# OpenL Tablets Developer Guide

Welcome to the OpenL Tablets Developer Guide! This guide will help you set up your development environment, understand the codebase, and contribute effectively to the project.

## Table of Contents

- [Getting Started](#getting-started)
- [Development Environment Setup](#development-environment-setup)
- [Project Structure](#project-structure)
- [Building the Project](#building-the-project)
- [Running Tests](#running-tests)
- [Development Workflow](#development-workflow)
- [Coding Standards](#coding-standards)
- [Debugging](#debugging)
- [Common Tasks](#common-tasks)
- [Contributing](#contributing)

## Getting Started

### Prerequisites

Before you begin, ensure you have the following installed:

- **JDK 21 or later**
  - OpenJDK or Oracle JDK
  - Set `JAVA_HOME` environment variable
- **Maven 3.9.9 or later**
  - Download from [maven.apache.org](https://maven.apache.org)
  - Add Maven to your `PATH`
- **Docker 27.5.0 or later** (for integration tests)
  - Download from [docker.com](https://www.docker.com)
- **Docker Compose 2.32.4 or later**
- **Git** for version control
- **IDE** (recommended):
  - IntelliJ IDEA (Ultimate or Community)
  - Eclipse with Maven plugin
  - VS Code with Java extensions

### System Requirements

- **RAM**: 8 GB minimum, 16 GB recommended
- **Disk Space**: 5 GB free for build artifacts
- **OS**: Linux, macOS, or Windows

## Development Environment Setup

### 1. Clone the Repository

```bash
git clone https://github.com/openl-tablets/openl-tablets.git
cd openl-tablets
```

### 2. Verify Java and Maven

```bash
java -version  # Should show Java 21 or later
mvn -version   # Should show Maven 3.9.9 or later
```

### 3. Initial Build

Perform an initial build to download dependencies and verify setup:

```bash
mvn clean install -DskipTests
```

This will take several minutes on the first run as Maven downloads dependencies.

### 4. IDE Setup

#### IntelliJ IDEA

1. Open IntelliJ IDEA
2. Select "Open" and choose the `openl-tablets` directory
3. IntelliJ will automatically detect the Maven project
4. Wait for Maven to import dependencies
5. Configure code style:
   - Go to Settings → Editor → Code Style
   - Set tab size to 4 spaces
   - Enable "Use spaces instead of tabs"

#### Eclipse

1. Open Eclipse
2. File → Import → Maven → Existing Maven Projects
3. Select the `openl-tablets` directory
4. Wait for Maven import to complete
5. Configure code style in Preferences → Java → Code Style

## Project Structure

OpenL Tablets is organized into several main modules:

```
openl-tablets/
├── DEV/                    # Core OpenL engine and rules framework
│   ├── org.openl.core/     # Core language infrastructure
│   ├── org.openl.rules/    # Rules engine
│   └── ...
├── STUDIO/                 # OpenL Studio web application
│   ├── org.openl.rules.webstudio/  # Main Studio application
│   ├── org.openl.rules.workspace/  # Workspace management
│   └── ...
├── WSFrontend/            # Rule Services (REST web services)
│   ├── org.openl.rules.ruleservice/  # Core rule service
│   ├── org.openl.rules.ruleservice.ws/  # Web service endpoints
│   └── ...
├── DEMO/                  # Demo applications and examples
├── ITEST/                 # Integration tests
├── Util/                  # Utility modules
│   ├── openl-maven-plugin/  # Maven plugin for OpenL
│   ├── openl-openapi-parser/  # OpenAPI integration
│   └── ...
├── Docs/                  # Documentation
├── pom.xml               # Root Maven POM
└── docker-compose.yml    # Docker Compose configuration
```

### Key Components

#### DEV/ - Core Engine

The core OpenL Tablets engine:
- **org.openl.core** - Language infrastructure and parsing
- **org.openl.rules** - Rule processing and execution
- **org.openl.rules.project** - Project model and compilation

#### STUDIO/ - Development Environment

Web-based IDE for rule development:
- Rule editor and testing
- Version control integration
- Project management
- User interface components

#### WSFrontend/ - Rule Services

Runtime deployment of rules as services:
- REST endpoints
- Rule deployment and management
- Service configuration
- Request/response handling

## Building the Project

### Full Build

Build the entire project with all tests:

```bash
mvn clean verify
```

**Estimated time**: 10-30 minutes depending on your hardware.

### Quick Build

Build without heavy tests:

```bash
mvn clean verify -Dquick -DnoPerf
```

**Estimated time**: 5-10 minutes.

### Build Options

- `-DskipTests` - Skip all tests
- `-Dquick` - Skip heavy/non-critical tests
- `-DnoPerf` - Run tests without extreme memory limitations
- `-DnoDocker` - Skip Docker-based integration tests
- `-T1C` - Build using 1 thread per CPU core (parallel build)

### Building Specific Modules

Build only a specific module:

```bash
cd STUDIO/org.openl.rules.webstudio
mvn clean install
```

## Running Tests

### Unit Tests

Run all unit tests:

```bash
mvn test
```

Run tests for a specific module:

```bash
cd DEV/org.openl.core
mvn test
```

Run a specific test class:

```bash
mvn test -Dtest=YourTestClass
```

Run a specific test method:

```bash
mvn test -Dtest=YourTestClass#testMethod
```

### Integration Tests

Integration tests are located in the `ITEST/` directory:

```bash
cd ITEST
mvn verify
```

Some integration tests require Docker. Skip Docker tests with:

```bash
mvn verify -DnoDocker
```

### Test Reports

Test reports are generated in `target/surefire-reports/` for each module.

## Development Workflow

### 1. Create a Feature Branch

```bash
git checkout -b feature/your-feature-name
```

### 2. Make Changes

Edit code, add features, or fix bugs following our [coding standards](../../CONTRIBUTING.md#coding-standards).

### 3. Build and Test

```bash
mvn clean verify
```

### 4. Commit Changes

```bash
git add .
git commit -m "feat: add new feature"
```

Follow [commit message guidelines](../../CONTRIBUTING.md#commit-message-guidelines).

### 5. Push and Create PR

```bash
git push -u origin feature/your-feature-name
```

Then create a pull request on GitHub.

## Coding Standards

### Java Code Style

- **Indentation**: 4 spaces (no tabs)
- **Line Length**: 120 characters maximum
- **Naming Conventions**:
  - Classes: `PascalCase`
  - Methods: `camelCase`
  - Constants: `UPPER_SNAKE_CASE`
  - Packages: `lowercase.with.dots`

### Code Quality

- Write clean, self-documenting code
- Add JavaDoc comments for public APIs
- Use meaningful variable and method names
- Keep methods focused and concise (< 30 lines preferred)
- Follow SOLID principles

### Best Practices

- Always write tests for new functionality
- Handle exceptions appropriately
- Avoid premature optimization
- Use dependency injection where appropriate
- Prefer immutability when possible

## Debugging

### Running OpenL Studio Locally

1. Build the project:
   ```bash
   mvn clean install -DskipTests
   ```

2. Navigate to Studio directory:
   ```bash
   cd STUDIO/org.openl.rules.webstudio
   ```

3. Run with Maven:
   ```bash
   mvn jetty:run
   ```

4. Open browser to http://localhost:8080

### Debugging in IDE

#### IntelliJ IDEA

1. Create a new "Maven" run configuration
2. Set working directory to `STUDIO/org.openl.rules.webstudio`
3. Set command: `jetty:run`
4. Click "Debug" to start debugging

#### Remote Debugging

Start with debug port:

```bash
mvn jetty:run -Djetty.jvmArgs="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
```

Then attach your IDE's debugger to port 5005.

### Logging

OpenL Tablets uses Log4j2 for logging. Configure logging in:
- `src/main/resources/log4j2.xml` (for production)
- `test-resources/log4j2.xml` (for tests)

Enable debug logging:

```xml
<Logger name="org.openl" level="DEBUG"/>
```

## Common Tasks

### Adding a New Dependency

1. Add dependency to appropriate `pom.xml`:
   ```xml
   <dependency>
       <groupId>com.example</groupId>
       <artifactId>library</artifactId>
       <version>${library.version}</version>
   </dependency>
   ```

2. Add version property to root `pom.xml` if needed:
   ```xml
   <properties>
       <library.version>1.0.0</library.version>
   </properties>
   ```

3. Run dependency check:
   ```bash
   mvn dependency:analyze
   ```

### Running Security Scans

Check for vulnerable dependencies:

```bash
mvn org.owasp:dependency-check-maven:check
```

### Updating Dependencies

Update all dependencies to latest versions:

```bash
mvn versions:display-dependency-updates
```

### Formatting Code

The project uses Maven plugins for code formatting. Format code with:

```bash
mvn rewrite:run
```

## Contributing

### Before Contributing

1. Read the [Contributing Guide](../../CONTRIBUTING.md)
2. Review the [Code of Conduct](../../CODE_OF_CONDUCT.md)
3. Check existing issues and PRs to avoid duplicates

### Contribution Process

1. **Discuss**: For large changes, discuss in an issue first
2. **Fork**: Fork the repository
3. **Branch**: Create a feature branch
4. **Code**: Implement your changes
5. **Test**: Ensure all tests pass
6. **Commit**: Follow commit message guidelines
7. **Push**: Push to your fork
8. **PR**: Create a pull request

### Code Review

All contributions go through code review:
- Respond to feedback promptly
- Make requested changes
- Ensure CI checks pass
- Be patient and respectful

## Resources

### Documentation

- [Main README](../../README.md)
- [Configuration Guide](../Configuration.md)
- [API Documentation](../Invoking_OpenL.md)
- [All Documentation](../README.md)

### Community

- [GitHub Issues](https://github.com/openl-tablets/openl-tablets/issues)
- [GitHub Discussions](https://github.com/openl-tablets/openl-tablets/discussions)
- [Website](https://openl-tablets.org)

### Related Projects

- [Apache Maven](https://maven.apache.org)
- [Spring Framework](https://spring.io)
- [Apache CXF](https://cxf.apache.org)

## Getting Help

If you need help:

1. Check this developer guide
2. Search [existing issues](https://github.com/openl-tablets/openl-tablets/issues)
3. Ask in [GitHub Discussions](https://github.com/openl-tablets/openl-tablets/discussions)
4. Create a new issue with the question template

## Next Steps

- Explore the [codebase](../../)
- Run the [demo applications](../../DEMO/)
- Try [building from source](../../README.md#building-from-source)
- Make your [first contribution](../../CONTRIBUTING.md)

---

**Happy coding!** We look forward to your contributions to OpenL Tablets.
