# OpenL Tablets Repository Improvement Plan

## Executive Summary

This document outlines a comprehensive plan to improve the OpenL Tablets repository structure, configuration, and developer experience based on best practices from the langfuse/langfuse repository (18k+ stars).

**Analysis Date:** 2025-11-05
**Source Repository:** https://github.com/langfuse/langfuse
**Target Repository:** https://github.com/openl-tablets/openl-tablets

---

## Current State Assessment

### ✅ Already Implemented (from previous improvements)

The OpenL repository has already implemented many modern practices:

1. **GitHub Configuration**
   - ✅ Dependabot configuration with dependency grouping
   - ✅ Issue templates (bug_report.md, feature_request.md, documentation.md)
   - ✅ Pull request template
   - ✅ CODEOWNERS file
   - ✅ Workflows (build, release, deploy, docker, trivy)

2. **Documentation**
   - ✅ Comprehensive CONTRIBUTING.md
   - ✅ CODE_OF_CONDUCT.md
   - ✅ SECURITY.md
   - ✅ CHANGELOG.md
   - ✅ Improved README.md with badges
   - ✅ docs/ directory with detailed guides
   - ✅ CLAUDE.md files for AI assistance

3. **Project Structure**
   - ✅ Well-organized Maven multi-module structure
   - ✅ Docker support (Dockerfile, compose.yaml)
   - ✅ CITATION.cff for academic citations

### ❌ Missing Compared to Langfuse

1. **AI Assistant Configuration**
   - ❌ No .cursor/ directory with context-specific rules
   - ❌ No .claude/ directory with hooks and agents
   - ❌ Limited AI-specific documentation

2. **Developer Experience**
   - ❌ No .vscode/ configuration (settings, extensions)
   - ❌ No .editorconfig for consistent formatting
   - ❌ No package.json with developer scripts
   - ❌ No .husky/ for git hooks
   - ❌ No docker-compose.dev.yml for local development

3. **Code Quality Automation**
   - ❌ No Spotless/Checkstyle configuration in pom.xml
   - ❌ No automated code formatting enforcement
   - ❌ No pre-commit hooks for quality checks

4. **GitHub Workflows**
   - ❌ No PR title validation (conventional commits)
   - ❌ No stale issue management
   - ❌ No CodeQL security analysis
   - ❌ No spell-checking automation
   - ❌ Limited matrix testing (Java versions, databases)

5. **Documentation**
   - ❌ No multi-language README support
   - ❌ No .devcontainer for GitHub Codespaces
   - ❌ Limited quick-start documentation

---

## Improvement Plan

### Batch 1: AI Assistant Configuration (High Priority, Low Effort)
**Effort:** 2-3 hours
**Impact:** Immediate improvement for AI-assisted development

#### 1.1 Create .cursor/ Directory Structure
```
.cursor/
├── rules/
│   ├── global.mdc                      # Project overview and architecture
│   ├── dev-module.mdc                  # DEV module patterns
│   ├── studio-module.mdc               # STUDIO module patterns
│   ├── wsfrontend-module.mdc          # WSFrontend module patterns
│   ├── testing.mdc                     # Testing guidelines
│   ├── database.mdc                    # Database and Hibernate patterns
│   └── deployment.mdc                  # Deployment configurations
└── environment.json                    # Environment settings
```

**Content Examples:**

`.cursor/rules/global.mdc`:
```markdown
# OpenL Tablets Project Structure

OpenL Tablets is a Business Rules Management System (BRMS) built with Java.

## Multi-Module Maven Project

- **DEV/**: Core rule engine, compiler, and data binding
  - `org.openl.rules`: Core rules engine
  - `org.openl.core`: Language core
  - `org.openl.data.bind`: Data binding and type system

- **STUDIO/**: Web-based IDE for rule authoring
  - `org.openl.rules.webstudio`: Main WebStudio application
  - Spring Boot 3.5.6 + React UI

- **WSFrontend/**: REST/SOAP services layer
  - `org.openl.rules.ruleservice.ws`: Web services deployment

- **Util/**: Shared utilities
- **ITEST/**: Integration tests

## Technology Stack
- Java 21 (minimum JDK 21)
- Spring Framework 6.2.11
- Spring Boot 3.5.6
- Maven 3.9.9
- Jetty 12.1.3
- Hibernate / JPA
- PostgreSQL / Oracle / MySQL / H2

## Key Domain Concepts
- **Rule Tables**: Excel-based business rules
- **Decision Tables**: Tabular decision logic
- **Test Tables**: Automated rule testing
- **Projects**: Version-controlled rule sets
- **Repositories**: Git-based rule storage
```

`.cursor/rules/testing.mdc`:
```markdown
# Testing Guidelines

## Test Independence
Each test should be isolated and runnable independently.

## Naming Conventions
- Test classes: `<ClassName>Test.java`
- Integration tests: `<Feature>IntegrationTest.java`
- Test methods: `test<Scenario>_<ExpectedOutcome>()`

## Test Categories
- Unit tests: Fast, no external dependencies
- Integration tests: Require Docker (databases, etc.)
- Performance tests: `-DnoPerf` to skip

## Coverage Requirements
- All business logic must have unit tests
- Integration tests for repository operations
- Use TestNG for test organization
```

`.cursor/rules/database.mdc`:
```markdown
# Database Patterns

## Multi-Tenant Support
OpenL Tablets supports multiple databases:
- PostgreSQL (recommended for production)
- Oracle
- MySQL
- H2 (development/testing)

## Migration Patterns
- Use Liquibase for schema changes
- Migrations in: `STUDIO/org.openl.rules.webstudio/src/main/resources/db/`
- Include rollback scripts
- Test migrations on all supported databases

## Query Patterns
- Always use JPA/Hibernate entities
- Repository pattern for data access
- Transactions managed by Spring
```

#### 1.2 Create .claude/ Directory Structure
```
.claude/
├── settings.json                       # Claude configuration
├── hooks/
│   ├── user-prompt-submit.sh          # Pre-prompt hook
│   └── post-tool-use.sh               # Post-edit hook
└── agents/
    └── code-reviewer.md               # Custom review agent
```

`.claude/settings.json`:
```json
{
  "allowedBashCommands": ["mvn", "find", "grep", "ls", "cat", "head", "tail"],
  "deniedBashCommands": [],
  "mcpServers": {
    "allProjectMCPServers": true,
    "enabledServers": []
  },
  "hooks": [
    {
      "trigger": "UserPromptSubmit",
      "scriptPath": ".claude/hooks/user-prompt-submit.sh"
    }
  ]
}
```

#### 1.3 Update .gitignore
Remove lines 42-43 that ignore AI assistant files:
```diff
- # Ignore AI assistant files
- CLAUDE.md
- .claude
```

**Deliverables:**
- [ ] .cursor/rules/ with 7 context files
- [ ] .claude/ with settings and hooks
- [ ] Updated .gitignore
- [ ] Test with Claude/Cursor to verify effectiveness

---

### Batch 2: Developer Experience Enhancement (High Priority, Medium Effort)
**Effort:** 4-6 hours
**Impact:** Dramatically reduces onboarding time

#### 2.1 VS Code Configuration

`.vscode/settings.json`:
```json
{
  "java.configuration.updateBuildConfiguration": "automatic",
  "java.compile.nullAnalysis.mode": "automatic",
  "java.saveActions.organizeImports": true,
  "editor.formatOnSave": true,
  "editor.rulers": [120],
  "editor.tabSize": 4,

  "[java]": {
    "editor.defaultFormatter": "redhat.java"
  },

  "[xml]": {
    "editor.defaultFormatter": "redhat.vscode-xml",
    "editor.tabSize": 2
  },

  "[yaml]": {
    "editor.tabSize": 2
  },

  "[javascript]": {
    "editor.defaultFormatter": "esbenp.prettier-vscode",
    "editor.tabSize": 2
  },

  "[typescript]": {
    "editor.defaultFormatter": "esbenp.prettier-vscode",
    "editor.tabSize": 2
  },

  "[json]": {
    "editor.defaultFormatter": "esbenp.prettier-vscode",
    "editor.tabSize": 2
  },

  "files.exclude": {
    "**/target": true,
    "**/.idea": true,
    "**/*.iml": true
  },

  "search.exclude": {
    "**/target": true,
    "**/node_modules": true
  },

  "maven.terminal.useJavaHome": true,
  "maven.executable.path": "mvn"
}
```

`.vscode/extensions.json`:
```json
{
  "recommendations": [
    "vscjava.vscode-java-pack",
    "vscjava.vscode-spring-boot-dashboard",
    "vscjava.vscode-maven",
    "redhat.vscode-xml",
    "redhat.vscode-yaml",
    "EditorConfig.EditorConfig",
    "esbenp.prettier-vscode",
    "GitHub.copilot",
    "GitHub.copilot-chat"
  ]
}
```

`.vscode/launch.json`:
```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "OpenL Studio",
      "request": "launch",
      "mainClass": "org.openl.rules.webstudio.WebStudioLauncher",
      "projectName": "org.openl.rules.webstudio",
      "args": "",
      "envFile": "${workspaceFolder}/.env"
    },
    {
      "type": "java",
      "name": "Rule Services",
      "request": "launch",
      "mainClass": "org.openl.rules.ruleservice.ws.WSLauncher",
      "projectName": "org.openl.rules.ruleservice.ws",
      "args": "",
      "envFile": "${workspaceFolder}/.env"
    }
  ]
}
```

#### 2.2 EditorConfig

`.editorconfig`:
```ini
root = true

[*]
charset = utf-8
end_of_line = lf
insert_final_newline = true
trim_trailing_whitespace = true

[*.java]
indent_style = space
indent_size = 4
max_line_length = 120

[*.xml]
indent_style = space
indent_size = 2

[*.{yml,yaml}]
indent_style = space
indent_size = 2

[*.{js,jsx,ts,tsx,json}]
indent_style = space
indent_size = 2

[*.md]
trim_trailing_whitespace = false
max_line_length = off

[Makefile]
indent_style = tab
```

#### 2.3 Developer Scripts

`package.json` (root):
```json
{
  "name": "openl-tablets",
  "version": "6.0.0",
  "private": true,
  "description": "OpenL Tablets Business Rules Management System",
  "scripts": {
    "dx": "npm run infra:up && npm run dev:studio",
    "dev:studio": "mvn -pl STUDIO/org.openl.rules.webstudio spring-boot:run",
    "dev:ws": "mvn -pl WSFrontend/org.openl.rules.ruleservice.ws spring-boot:run",
    "build": "mvn clean install",
    "build:quick": "mvn clean install -Dquick -DnoPerf -T1C",
    "build:no-tests": "mvn clean install -DskipTests",
    "test": "mvn test",
    "test:integration": "mvn verify -DskipTests=false",
    "test:unit": "mvn test -DskipTests=false",
    "lint": "mvn checkstyle:check",
    "format": "mvn spotless:apply",
    "format:check": "mvn spotless:check",
    "infra:up": "docker compose -f docker-compose.dev.yml up -d",
    "infra:down": "docker compose -f docker-compose.dev.yml down",
    "infra:logs": "docker compose -f docker-compose.dev.yml logs -f",
    "clean": "mvn clean && docker compose -f docker-compose.dev.yml down -v",
    "docker:build": "docker build -t openl-tablets:dev .",
    "docker:run": "docker run -p 8080:8080 openl-tablets:dev"
  },
  "repository": {
    "type": "git",
    "url": "https://github.com/openl-tablets/openl-tablets.git"
  },
  "license": "LGPL-2.1",
  "devDependencies": {
    "husky": "^9.0.11"
  }
}
```

#### 2.4 Development Docker Compose

`docker-compose.dev.yml`:
```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16
    container_name: openl-postgres-dev
    ports:
      - "127.0.0.1:5432:5432"
    environment:
      POSTGRES_DB: openl
      POSTGRES_USER: openl
      POSTGRES_PASSWORD: openl
    volumes:
      - postgres-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U openl"]
      interval: 5s
      timeout: 5s
      retries: 5
    networks:
      - openl-dev

  mysql:
    image: mysql:8.0
    container_name: openl-mysql-dev
    ports:
      - "127.0.0.1:3306:3306"
    environment:
      MYSQL_DATABASE: openl
      MYSQL_USER: openl
      MYSQL_PASSWORD: openl
      MYSQL_ROOT_PASSWORD: rootpassword
    volumes:
      - mysql-data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 5s
      timeout: 5s
      retries: 5
    networks:
      - openl-dev

  oracle:
    image: container-registry.oracle.com/database/express:21.3.0-xe
    container_name: openl-oracle-dev
    ports:
      - "127.0.0.1:1521:1521"
    environment:
      ORACLE_PWD: oracle
      ORACLE_CHARACTERSET: AL32UTF8
    volumes:
      - oracle-data:/opt/oracle/oradata
    healthcheck:
      test: ["CMD-SHELL", "echo 'SELECT 1 FROM DUAL;' | sqlplus -s sys/oracle@localhost:1521/XE as sysdba"]
      interval: 30s
      timeout: 10s
      retries: 5
    networks:
      - openl-dev

volumes:
  postgres-data:
  mysql-data:
  oracle-data:

networks:
  openl-dev:
    name: openl-dev
```

`.env.example`:
```bash
# Database Configuration (for local development)
DB_TYPE=postgresql
DB_HOST=localhost
DB_PORT=5432
DB_NAME=openl
DB_USER=openl
DB_PASSWORD=openl

# Application Settings
SERVER_PORT=8080
LOG_LEVEL=INFO

# OpenL Studio Settings
STUDIO_WORKSPACE=/opt/openl/workspace
STUDIO_REPOSITORY_TYPE=git

# Rule Services Settings
SERVICES_PORT=8081
```

#### 2.5 Update CONTRIBUTING.md

Add new "Quick Start" section at the top:

```markdown
## Quick Start (5 Minutes)

### One-Command Setup

```bash
# Clone repository
git clone https://github.com/openl-tablets/openl-tablets.git
cd openl-tablets

# Install npm dependencies (for scripts)
npm install

# Start infrastructure + OpenL Studio
npm run dx
```

Open http://localhost:8080 in your browser.

### Daily Development Commands

```bash
npm run dev:studio       # Start OpenL Studio
npm run dev:ws           # Start Rule Services
npm run infra:up         # Start databases
npm run infra:down       # Stop databases
npm run build            # Full build with tests
npm run build:quick      # Quick build, fewer tests
npm run test             # Run tests
npm run format           # Auto-format code
```

### Requirements
- JDK 21+
- Maven 3.9.9
- Docker 27.5.0
- Node.js 18+ (for npm scripts)
```

**Deliverables:**
- [ ] .vscode/ configuration
- [ ] .editorconfig
- [ ] package.json with dev scripts
- [ ] docker-compose.dev.yml
- [ ] .env.example
- [ ] Updated CONTRIBUTING.md

---

### Batch 3: Code Quality Automation (Medium Priority, Medium Effort)
**Effort:** 4-5 hours
**Impact:** Consistent code quality across contributors

#### 3.1 Add Spotless Plugin to Parent POM

Add to `pom.xml` (parent):

```xml
<build>
  <pluginManagement>
    <plugins>
      <!-- Code Formatting -->
      <plugin>
        <groupId>com.diffplug.spotless</groupId>
        <artifactId>spotless-maven-plugin</artifactId>
        <version>2.43.0</version>
        <configuration>
          <java>
            <googleJavaFormat>
              <version>1.17.0</version>
              <style>GOOGLE</style>
            </googleJavaFormat>
            <removeUnusedImports/>
            <trimTrailingWhitespace/>
            <endWithNewline/>
            <importOrder>
              <order>java,javax,org,com</order>
            </importOrder>
          </java>
          <pom>
            <sortPom>
              <expandEmptyElements>false</expandEmptyElements>
            </sortPom>
          </pom>
        </configuration>
      </plugin>

      <!-- Checkstyle -->
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-checkstyle-plugin</artifactId>
        <version>3.3.1</version>
        <dependencies>
          <dependency>
            <groupId>com.puppycrawl.tools</groupId>
            <artifactId>checkstyle</artifactId>
            <version>10.12.5</version>
          </dependency>
        </dependencies>
        <configuration>
          <configLocation>checkstyle.xml</configLocation>
          <consoleOutput>true</consoleOutput>
          <failsOnError>true</failsOnError>
          <violationSeverity>warning</violationSeverity>
        </configuration>
      </plugin>

      <!-- Code Coverage -->
      <plugin>
        <groupId>org.jacoco</groupId>
        <artifactId>jacoco-maven-plugin</artifactId>
        <version>0.8.11</version>
        <executions>
          <execution>
            <goals>
              <goal>prepare-agent</goal>
            </goals>
          </execution>
          <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
              <goal>report</goal>
            </goals>
          </execution>
          <execution>
            <id>jacoco-check</id>
            <goals>
              <goal>check</goal>
            </goals>
            <configuration>
              <rules>
                <rule>
                  <element>PACKAGE</element>
                  <limits>
                    <limit>
                      <counter>LINE</counter>
                      <value>COVEREDRATIO</value>
                      <minimum>0.50</minimum>
                    </limit>
                  </limits>
                </rule>
              </rules>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </pluginManagement>

  <plugins>
    <!-- Apply Spotless to all modules -->
    <plugin>
      <groupId>com.diffplug.spotless</groupId>
      <artifactId>spotless-maven-plugin</artifactId>
    </plugin>
  </plugins>
</build>
```

#### 3.2 Create Checkstyle Configuration

`checkstyle.xml`:
```xml
<?xml version="1.0"?>
<!DOCTYPE module PUBLIC
  "-//Checkstyle//DTD Checkstyle Configuration 1.3//EN"
  "https://checkstyle.org/dtds/configuration_1_3.dtd">

<module name="Checker">
  <property name="severity" value="warning"/>
  <property name="fileExtensions" value="java, properties, xml"/>

  <!-- Checks for whitespace -->
  <module name="FileTabCharacter">
    <property name="eachLine" value="true"/>
  </module>

  <module name="TreeWalker">
    <!-- Naming Conventions -->
    <module name="ConstantName"/>
    <module name="LocalFinalVariableName"/>
    <module name="LocalVariableName"/>
    <module name="MemberName"/>
    <module name="MethodName"/>
    <module name="PackageName"/>
    <module name="ParameterName"/>
    <module name="StaticVariableName"/>
    <module name="TypeName"/>

    <!-- Imports -->
    <module name="AvoidStarImport"/>
    <module name="IllegalImport"/>
    <module name="RedundantImport"/>
    <module name="UnusedImports"/>

    <!-- Size Violations -->
    <module name="LineLength">
      <property name="max" value="120"/>
      <property name="ignorePattern" value="^package.*|^import.*"/>
    </module>
    <module name="MethodLength">
      <property name="max" value="150"/>
    </module>
    <module name="ParameterNumber">
      <property name="max" value="7"/>
    </module>

    <!-- Whitespace -->
    <module name="EmptyForIteratorPad"/>
    <module name="GenericWhitespace"/>
    <module name="MethodParamPad"/>
    <module name="NoWhitespaceAfter"/>
    <module name="NoWhitespaceBefore"/>
    <module name="OperatorWrap"/>
    <module name="ParenPad"/>
    <module name="TypecastParenPad"/>
    <module name="WhitespaceAfter"/>
    <module name="WhitespaceAround"/>

    <!-- Modifier Checks -->
    <module name="ModifierOrder"/>
    <module name="RedundantModifier"/>

    <!-- Blocks -->
    <module name="EmptyBlock"/>
    <module name="LeftCurly"/>
    <module name="NeedBraces"/>
    <module name="RightCurly"/>

    <!-- Coding -->
    <module name="EmptyStatement"/>
    <module name="EqualsHashCode"/>
    <module name="IllegalInstantiation"/>
    <module name="MissingSwitchDefault"/>
    <module name="SimplifyBooleanExpression"/>
    <module name="SimplifyBooleanReturn"/>

    <!-- Class Design -->
    <module name="FinalClass"/>
    <module name="HideUtilityClassConstructor"/>
    <module name="InterfaceIsType"/>
    <module name="VisibilityModifier">
      <property name="protectedAllowed" value="true"/>
    </module>
  </module>
</module>
```

#### 3.3 Git Hooks with Husky

`.husky/pre-commit`:
```bash
#!/bin/sh
. "$(dirname "$0")/_/husky.sh"

# Check if committing to main branch
current_branch=$(git rev-parse --abbrev-ref HEAD)
if [ "$current_branch" = "master" ] || [ "$current_branch" = "main" ]; then
    echo "⚠️  You are about to commit to the $current_branch branch."
    echo "Are you sure you want to continue? (y/n)"
    read -r response
    if [ "$response" != "y" ] && [ "$response" != "Y" ]; then
        echo "Commit cancelled."
        exit 1
    fi
fi

# Run format check
echo "Running format check..."
mvn spotless:check -q
if [ $? -ne 0 ]; then
    echo "❌ Code formatting issues detected!"
    echo "Run 'mvn spotless:apply' or 'npm run format' to fix formatting."
    exit 1
fi

echo "✅ Pre-commit checks passed!"
```

`.husky/prepare-commit-msg`:
```bash
#!/bin/sh
. "$(dirname "$0")/_/husky.sh"

COMMIT_MSG_FILE=$1
COMMIT_SOURCE=$2

# Skip if amending, merging, or using commit template
if [ "$COMMIT_SOURCE" = "merge" ] || [ "$COMMIT_SOURCE" = "template" ]; then
  exit 0
fi

# Check conventional commit format
commit_regex='^(feat|fix|docs|style|refactor|perf|test|chore|ci|build|security)(\([A-Z]+\))?: .{1,100}'

if ! grep -qE "$commit_regex" "$COMMIT_MSG_FILE"; then
    echo "❌ Invalid commit message format!"
    echo ""
    echo "Expected: <type>(<scope>): <description>"
    echo "Example: feat(STUDIO): add dark mode toggle"
    echo ""
    echo "Types: feat, fix, docs, style, refactor, perf, test, chore, ci, build, security"
    echo "Scopes: DEV, STUDIO, WSFrontend, Util, ITEST, DEMO"
    exit 1
fi
```

Setup script:

```bash
# After npm install
npm install husky --save-dev
npx husky install
npx husky add .husky/pre-commit "npm run format:check"
```

**Deliverables:**
- [ ] Updated pom.xml with Spotless, Checkstyle, JaCoCo
- [ ] checkstyle.xml configuration
- [ ] .husky/ git hooks
- [ ] Test formatting on sample files

---

### Batch 4: Enhanced GitHub Workflows (Medium Priority, Medium Effort)
**Effort:** 5-6 hours
**Impact:** Better automation, security, and quality gates

#### 4.1 PR Title Validation

`.github/workflows/validate-pr-title.yml`:
```yaml
name: Validate PR Title

on:
  pull_request:
    types: [opened, edited, synchronize, reopened]

permissions:
  pull-requests: read

jobs:
  validate:
    runs-on: ubuntu-latest
    steps:
      - uses: amannn/action-semantic-pull-request@v5
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        with:
          types: |
            feat
            fix
            docs
            style
            refactor
            perf
            test
            chore
            ci
            build
            security
          scopes: |
            DEV
            STUDIO
            WSFrontend
            Util
            ITEST
            DEMO
          requireScope: false
          subjectPattern: ^.{1,100}$
          validateSingleCommit: false
          ignoreLabels: |
            bot
            dependencies
```

#### 4.2 CodeQL Security Analysis

`.github/workflows/codeql.yml`:
```yaml
name: CodeQL Security Analysis

on:
  push:
    branches: [master, develop]
  pull_request:
    branches: [master, develop]
  schedule:
    - cron: '30 2 * * 1'  # Monday at 2:30 AM

jobs:
  analyze:
    name: Analyze Code
    runs-on: ubuntu-latest
    permissions:
      actions: read
      contents: read
      security-events: write

    strategy:
      fail-fast: false
      matrix:
        language: [java, javascript]

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Initialize CodeQL
        uses: github/codeql-action/init@v3
        with:
          languages: ${{ matrix.language }}
          queries: security-extended

      - name: Setup Java
        if: matrix.language == 'java'
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'maven'

      - name: Build Java
        if: matrix.language == 'java'
        run: mvn clean install -DskipTests -Dquick

      - name: Perform CodeQL Analysis
        uses: github/codeql-action/analyze@v3
        with:
          category: "/language:${{ matrix.language }}"
```

#### 4.3 Stale Issue Management

`.github/workflows/stale-issues.yml`:
```yaml
name: Close Stale Issues

on:
  schedule:
    - cron: '0 0 * * *'  # Daily at midnight UTC

permissions:
  issues: write
  pull-requests: write

jobs:
  stale:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/stale@v9
        with:
          repo-token: ${{ secrets.GITHUB_TOKEN }}
          days-before-stale: 90
          days-before-close: 14
          stale-issue-message: |
            This issue has been automatically marked as stale because it has been open
            for 90 days with no activity. It will be closed in 14 days if no further
            activity occurs.

            If this issue is still relevant, please:
            - Comment to keep it open
            - Provide additional context or updates
            - Add the `pinned` label to prevent auto-closing

            Thank you for your contributions!
          close-issue-message: |
            This issue was automatically closed because it has been stale for 14 days
            with no activity.

            If this issue is still relevant, please reopen it with updated information
            or create a new issue referencing this one.
          stale-issue-label: 'stale'
          close-issue-label: 'auto-closed'
          exempt-issue-labels: 'pinned,security,roadmap,in-progress'
          exempt-pr-labels: 'pinned,in-review'
          operations-per-run: 100

          # Don't auto-close PRs
          days-before-pr-stale: -1
          days-before-pr-close: -1
```

#### 4.4 Spell Checking

`.github/workflows/codespell.yml`:
```yaml
name: Spell Check

on:
  push:
    branches: [master, develop]
  pull_request:
    branches: [master, develop]

jobs:
  codespell:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Run Codespell
        uses: codespell-project/actions-codespell@v2
        with:
          check_filenames: true
          check_hidden: true
          skip: target,node_modules,.git,*.jar,*.class,*.png,*.jpg,*.svg
          ignore_words_file: .codespellignore
```

`.codespellignore`:
```
colour
licence
parametre
```

#### 4.5 Enhanced Build Workflow with Matrix Testing

`.github/workflows/build-matrix.yml`:
```yaml
name: Build Matrix

on:
  push:
    branches: [master, develop]
  pull_request:
    branches: [master, develop]

jobs:
  build:
    runs-on: ${{ matrix.os }}
    strategy:
      fail-fast: false
      matrix:
        os: [ubuntu-latest, windows-latest, macos-latest]
        java: [21, 23]
        exclude:
          # Exclude some combinations to save CI time
          - os: windows-latest
            java: 23
          - os: macos-latest
            java: 23

    steps:
      - uses: actions/checkout@v4

      - name: Setup Java ${{ matrix.java }}
        uses: actions/setup-java@v4
        with:
          java-version: ${{ matrix.java }}
          distribution: 'temurin'
          cache: 'maven'

      - name: Build with Maven
        run: mvn clean install -Dquick -DnoPerf

      - name: Upload test results
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: test-results-${{ matrix.os }}-java${{ matrix.java }}
          path: '**/target/surefire-reports/'

  test-databases:
    runs-on: ubuntu-latest
    strategy:
      fail-fast: false
      matrix:
        database: [postgresql, mysql, oracle]

    services:
      postgres:
        image: postgres:16
        env:
          POSTGRES_DB: openl_test
          POSTGRES_USER: openl
          POSTGRES_PASSWORD: openl
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 5432:5432

      mysql:
        image: mysql:8.0
        env:
          MYSQL_DATABASE: openl_test
          MYSQL_USER: openl
          MYSQL_PASSWORD: openl
          MYSQL_ROOT_PASSWORD: rootpassword
        options: >-
          --health-cmd="mysqladmin ping"
          --health-interval=10s
          --health-timeout=5s
          --health-retries=5
        ports:
          - 3306:3306

    steps:
      - uses: actions/checkout@v4

      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'maven'

      - name: Run Integration Tests
        run: mvn verify -P integration-tests -Ddb.type=${{ matrix.database }}
        env:
          DB_HOST: localhost
          DB_PORT: ${{ matrix.database == 'postgresql' && '5432' || '3306' }}
          DB_NAME: openl_test
          DB_USER: openl
          DB_PASSWORD: openl
```

**Deliverables:**
- [ ] validate-pr-title.yml
- [ ] codeql.yml
- [ ] stale-issues.yml
- [ ] codespell.yml
- [ ] build-matrix.yml
- [ ] Test all workflows

---

### Batch 5: Documentation Enhancement (Low Priority, Medium Effort)
**Effort:** 6-8 hours
**Impact:** Better discoverability and onboarding

#### 5.1 Dev Container Configuration

`.devcontainer/devcontainer.json`:
```json
{
  "name": "OpenL Tablets Development",
  "dockerComposeFile": "../docker-compose.dev.yml",
  "service": "devcontainer",
  "workspaceFolder": "/workspace",

  "customizations": {
    "vscode": {
      "extensions": [
        "vscjava.vscode-java-pack",
        "vscjava.vscode-spring-boot-dashboard",
        "vscjava.vscode-maven",
        "redhat.vscode-xml",
        "redhat.vscode-yaml",
        "EditorConfig.EditorConfig",
        "GitHub.copilot",
        "GitHub.copilot-chat"
      ],
      "settings": {
        "java.configuration.updateBuildConfiguration": "automatic",
        "maven.executable.preferMavenWrapper": false
      }
    }
  },

  "forwardPorts": [8080, 8081, 5432, 3306],
  "postCreateCommand": "mvn clean install -DskipTests",

  "remoteUser": "vscode"
}
```

Add devcontainer service to `docker-compose.dev.yml`:
```yaml
services:
  devcontainer:
    build:
      context: .
      dockerfile: .devcontainer/Dockerfile
    volumes:
      - ..:/workspace:cached
    command: sleep infinity
    depends_on:
      - postgres
      - mysql
```

`.devcontainer/Dockerfile`:
```dockerfile
FROM mcr.microsoft.com/devcontainers/java:21

# Install Node.js for npm scripts
RUN curl -fsSL https://deb.nodesource.com/setup_18.x | bash - \
    && apt-get install -y nodejs

# Install additional tools
RUN apt-get update && apt-get install -y \
    git \
    maven \
    && rm -rf /var/lib/apt/lists/*

USER vscode
```

#### 5.2 Quick Start Guide

`docs/getting-started/QUICKSTART.md`:
```markdown
# OpenL Tablets Quick Start Guide

Get up and running with OpenL Tablets in 5 minutes!

## Prerequisites

- JDK 21+
- Maven 3.9.9
- Docker (for databases)
- Node.js 18+ (for npm scripts)

## Installation

### Option 1: Docker (Fastest)

```bash
docker compose up
```

Access:
- OpenL Studio: http://localhost:8080
- Rule Services: http://localhost:8081

### Option 2: From Source

1. **Clone repository**
   ```bash
   git clone https://github.com/openl-tablets/openl-tablets.git
   cd openl-tablets
   ```

2. **Install dependencies**
   ```bash
   npm install
   ```

3. **Start infrastructure**
   ```bash
   npm run infra:up
   ```

4. **Start OpenL Studio**
   ```bash
   npm run dev:studio
   ```

5. **Open browser**
   http://localhost:8080

## First Steps

### 1. Create Your First Project

1. Click "Create New Repository"
2. Choose "Local" repository type
3. Click "Create"
4. Click "Create New Project"
5. Select "Simple Project" template
6. Name it "MyFirstRules"

### 2. Create a Decision Table

1. Open your project
2. Click "Create" → "Decision Table"
3. Name it "DiscountCalculation"
4. Add rules:
   - If order amount > $1000, discount = 10%
   - If order amount > $500, discount = 5%
   - Otherwise, discount = 0%

### 3. Test Your Rules

1. Click "Run" tab
2. Enter test data
3. See results instantly

### 4. Deploy as REST Service

1. Click "Deploy" → "Rule Services"
2. Your rules are now available as REST API
3. Test at: http://localhost:8081/swagger-ui.html

## Next Steps

- [Tutorial: Building Insurance Rating Rules](tutorials/insurance-rating.md)
- [User Guide](../reference/user-guide.md)
- [Developer Guide](../developer-guide/index.md)
- [API Reference](../api/public-api-reference.md)

## Troubleshooting

### Port Already in Use

```bash
# Change port in .env file
SERVER_PORT=8082
```

### Maven Build Fails

```bash
# Clean and rebuild
npm run clean
npm run build
```

### Database Connection Issues

```bash
# Restart infrastructure
npm run infra:down
npm run infra:up
```

## Getting Help

- [GitHub Discussions](https://github.com/openl-tablets/openl-tablets/discussions)
- [Issue Tracker](https://github.com/openl-tablets/openl-tablets/issues)
- [Documentation](https://openl-tablets.org)
```

#### 5.3 Enhanced README Structure

Update `README.md` with better organization:

```markdown
# OpenL Tablets - Easy Business Rules

![Build](https://github.com/openl-tablets/openl-tablets/workflows/Build/badge.svg)
![Maven Central](https://img.shields.io/maven-central/v/org.openl/org.openl.core)
![License](https://img.shields.io/badge/license-LGPL-blue.svg)
![Stars](https://img.shields.io/github/stars/openl-tablets/openl-tablets)

**OpenL Tablets** bridges the gap between business rules and software implementation, making business rules management accessible and efficient.

[Website](https://openl-tablets.org) | [Documentation](docs/) | [Quick Start](docs/getting-started/QUICKSTART.md) | [Contributing](CONTRIBUTING.md) | [Discord](#)

---

## 📋 Table of Contents

- [About](#about)
- [Key Features](#key-features)
- [Quick Start](#quick-start)
- [Use Cases](#use-cases)
- [Documentation](#documentation)
- [Community](#community)
- [Contributing](#contributing)
- [License](#license)

## 🎯 About

OpenL Tablets is an open-source Business Rules Management System (BRMS) that enables:

- **Business Users**: Create rules in Excel or via AI tools
- **Developers**: Integrate rule engines with minimal code
- **Organizations**: Bridge business policies and technical implementation

## ✨ Key Features

### For Business Users
- 📊 **Excel-Based Authoring**: Familiar spreadsheet interface
- 🤖 **AI Tools Support**: MCP integration for Claude, Cursor, etc.
- ✅ **Real-Time Validation**: Instant error checking
- 🔄 **Version Control**: Git integration for rule history

### For Developers
- 🚀 **One-Click Deploy**: REST/SOAP services instantly
- 🔌 **Java API**: Reflection-like rule access
- 🧪 **Testing Framework**: Comprehensive test tools
- 📦 **Multiple Deployment**: Standalone, embedded, or services

### For Organizations
- 🏢 **Production-Ready**: Battle-tested in enterprises
- ⚡ **Fast & Scalable**: High-performance execution
- 🔒 **Secure**: Enterprise-grade security
- 📈 **Traceable**: Full audit trail

## 🚀 Quick Start

### Docker (30 seconds)
```bash
docker compose up
```
Open http://localhost:8080

### From Source (5 minutes)
```bash
git clone https://github.com/openl-tablets/openl-tablets.git
cd openl-tablets
npm install
npm run dx
```
Open http://localhost:8080

[Full Quick Start Guide →](docs/getting-started/QUICKSTART.md)

## 💼 Use Cases

- ✅ Insurance Rating & Underwriting
- ✅ Loan Approval & Credit Scoring
- ✅ Pricing & Discounting
- ✅ Compliance & Regulatory Rules
- ✅ Fraud Detection
- ✅ Tax Calculation

[See Examples →](docs/examples/)

## 📚 Documentation

- [Quick Start Guide](docs/getting-started/QUICKSTART.md)
- [User Guide](docs/reference/user-guide.md)
- [Developer Guide](docs/developer-guide/index.md)
- [API Reference](docs/api/public-api-reference.md)
- [Deployment Guide](Docs/Production_Deployment.md)

## 🤝 Community & Support

- **Discussions**: [GitHub Discussions](https://github.com/openl-tablets/openl-tablets/discussions)
- **Issues**: [Issue Tracker](https://github.com/openl-tablets/openl-tablets/issues)
- **Website**: [openl-tablets.org](https://openl-tablets.org)

## 🌟 Contributing

We welcome contributions! See [CONTRIBUTING.md](CONTRIBUTING.md)

Quick contribution steps:
1. Fork the repository
2. Create your feature branch
3. Make your changes with tests
4. Submit a pull request

## 📄 License

OpenL Tablets is licensed under the [LGPL 2.1](LICENSE).

## 🏆 Built With OpenL Tablets

Organizations worldwide use OpenL Tablets for mission-critical business rules.

[Add your organization →](https://github.com/openl-tablets/openl-tablets/discussions)

---

**Star** ⭐ this repository if you find it helpful!
```

**Deliverables:**
- [ ] .devcontainer/ configuration
- [ ] QUICKSTART.md guide
- [ ] Enhanced README.md
- [ ] Tutorial documentation

---

### Batch 6: Advanced Features (Optional, Low Priority)
**Effort:** 8-10 hours
**Impact:** Nice-to-have improvements

#### 6.1 Multi-Language README Support

Create translated READMEs:
- `README.md` (English - primary)
- `README.zh-CN.md` (Simplified Chinese)
- `README.ja.md` (Japanese)
- `README.ko.md` (Korean)

#### 6.2 Release Automation

`.github/workflows/release-automation.yml`:
```yaml
name: Release Automation

on:
  push:
    tags:
      - 'v*.*.*'

jobs:
  release:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Generate Changelog
        id: changelog
        uses: metcalfc/changelog-generator@v4
        with:
          myToken: ${{ secrets.GITHUB_TOKEN }}

      - name: Create GitHub Release
        uses: softprops/action-gh-release@v1
        with:
          body: ${{ steps.changelog.outputs.changelog }}
          files: |
            DEMO/target/*.zip
            STUDIO/org.openl.rules.webstudio/target/*.war
            WSFrontend/org.openl.rules.ruleservice.ws/target/*.war
```

#### 6.3 Performance Benchmarking

Create performance benchmark suite and track over time.

#### 6.4 API Documentation Generation

Automate OpenAPI/Swagger documentation generation and publishing.

**Deliverables:**
- [ ] Multi-language READMEs
- [ ] Release automation
- [ ] Performance benchmarks
- [ ] API doc generation

---

## Implementation Timeline

### Phase 1: Quick Wins (Week 1)
- ✅ Batch 1: AI Assistant Configuration (2-3 hours)
- ✅ Batch 2: Developer Experience (4-6 hours)

**Expected Impact:** Immediate improvement in developer productivity

### Phase 2: Quality & Automation (Week 2)
- ✅ Batch 3: Code Quality Automation (4-5 hours)
- ✅ Batch 4: Enhanced GitHub Workflows (5-6 hours)

**Expected Impact:** Consistent code quality, better security

### Phase 3: Documentation (Week 3)
- ✅ Batch 5: Documentation Enhancement (6-8 hours)

**Expected Impact:** Better onboarding, reduced support burden

### Phase 4: Polish (Optional)
- ⚠️  Batch 6: Advanced Features (8-10 hours)

**Expected Impact:** Nice-to-have improvements

## Success Metrics

### Developer Experience
- ⏱️ **Onboarding Time**: Target < 10 minutes (from 1+ hours)
- 📝 **PR Quality**: 50% fewer review cycles
- 🤖 **AI Assistance**: 80% relevant suggestions

### Code Quality
- ✅ **Test Coverage**: Maintain > 50% (track with JaCoCo)
- 🐛 **Bug Detection**: 30% fewer bugs in production
- 🔒 **Security**: Zero critical vulnerabilities

### Community
- 💬 **Response Time**: < 24 hours to issues
- 👥 **Contributors**: 20% increase
- ⭐ **Stars**: 10% monthly growth

## Rollout Strategy

### 1. Create Feature Branch
```bash
git checkout -b feature/repository-improvements
```

### 2. Implement in Batches
- Implement one batch at a time
- Test thoroughly
- Get feedback from team
- Iterate based on feedback

### 3. Gradual Rollout
- Week 1: Deploy Batch 1-2 to dev branch
- Week 2: Deploy Batch 3-4 after team review
- Week 3: Deploy Batch 5 with documentation review
- Week 4: Optional Batch 6 based on team feedback

### 4. Communication Plan
- Announce changes in GitHub Discussions
- Update CONTRIBUTING.md with new processes
- Create video walkthrough (optional)
- Host Q&A session for contributors

## Risks & Mitigation

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| Breaking existing workflows | High | Low | Thorough testing, gradual rollout |
| Developer resistance to new tools | Medium | Medium | Clear documentation, benefits communication |
| CI/CD overhead | Low | Medium | Optimize workflows, use caching |
| Maintenance burden | Medium | Low | Automate where possible, clear docs |

## Conclusion

This improvement plan draws from langfuse's proven practices and adapts them to OpenL Tablets' Java/Maven ecosystem. The phased approach ensures minimal disruption while maximizing benefits.

### Key Recommendations

1. **Start with Batch 1-2** (AI config + dev experience) - highest ROI
2. **Automate code quality** (Batch 3) to reduce review burden
3. **Enhance workflows** (Batch 4) for better security and automation
4. **Improve documentation** (Batch 5) to grow community
5. **Consider advanced features** (Batch 6) based on team capacity

### Expected Outcomes

After full implementation:
- ✅ 80% reduction in onboarding time
- ✅ 50% fewer PR review cycles
- ✅ Consistent code quality across all modules
- ✅ Better AI assistance with context-aware suggestions
- ✅ Improved security with automated scanning
- ✅ Growing contributor community

---

**Questions or feedback?** Please comment on [GitHub Discussion #XXX]

**Ready to start?** Let's begin with Batch 1!
