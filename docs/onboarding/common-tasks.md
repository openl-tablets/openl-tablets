# Common Development Tasks

**Last Updated**: 2025-11-05
**Target Audience**: Developers working on OpenL Tablets

---

## Table of Contents

- [Build & Development](#build--development)
- [Working with Core Engine](#working-with-core-engine)
- [Working with Web Studio](#working-with-web-studio)
- [Working with Rule Services](#working-with-rule-services)
- [Testing](#testing)
- [Git Workflow](#git-workflow)
- [Troubleshooting](#troubleshooting)

---

## Build & Development

### Full Build from Scratch

```bash
# Clone repository
git clone https://github.com/openl-tablets/openl-tablets.git
cd openl-tablets

# Full clean build (takes ~10-15 minutes)
mvn clean install

# Quick build (recommended for development)
mvn clean install -Dquick -DnoPerf -T1C

# Skip tests entirely
mvn clean install -DskipTests
```

### Build Specific Module

```bash
# Build only DEV module
cd DEV
mvn clean install

# Build only STUDIO
cd STUDIO
mvn clean install -DskipTests

# Build only WSFrontend
cd WSFrontend
mvn clean install -DskipTests
```

### Run Web Studio Locally

```bash
# Build studio-ui frontend
cd STUDIO/studio-ui
npm install
npm run build

# Start WebStudio
cd ../org.openl.rules.webstudio
mvn spring-boot:run

# Access at http://localhost:8080
```

### Run Rule Service Locally

```bash
# Start RuleService
cd WSFrontend/org.openl.rules.ruleservice.ws
mvn spring-boot:run

# Access at http://localhost:8081
# Swagger UI: http://localhost:8081/swagger-ui.html
```

---

## Working with Core Engine

### Task: Add a New Table Type

**Example**: Adding a "validation table" type

**Steps**:

1. **Define Excel syntax** (in Excel file):
```
Validation ValidationName
Input Field | Expected Type | Validation Rule
age         | Integer       | age >= 18
email       | String        | email.contains("@")
```

2. **Create table syntax node** (`/DEV/org.openl.rules/src/org/openl/rules/table/`):
```java
package org.openl.rules.table;

public class ValidationTableSyntaxNode extends TableSyntaxNode {
    public ValidationTableSyntaxNode(/* params */) {
        // Implementation
    }
}
```

3. **Create node binder** (`/DEV/org.openl.rules/src/org/openl/rules/lang/xls/binding/`):
```java
package org.openl.rules.lang.xls.binding;

public class ValidationTableBinder extends ATableBinder {
    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) {
        // Parse table structure
        // Create bound node
        // Register in binding context
        return new ValidationTableBoundNode(/* ... */);
    }
}
```

4. **Register in XlsDefinitions** (`XlsSheetSourceCodeModule.java`):
```java
// Add to table type recognition
if (tableName.startsWith("Validation")) {
    return new ValidationTableSyntaxNode(/* ... */);
}
```

5. **Add tests**:
```java
@Test
void testValidationTable() {
    // Create test Excel with validation table
    // Compile
    // Execute
    // Assert results
}
```

6. **Document** in user guide and update `/DEV/CLAUDE.md`

### Task: Add Built-in Function

**Example**: Adding a string manipulation function

**Steps**:

1. **Add method to utility class** (`/DEV/org.openl.rules.util/src/`):
```java
package org.openl.rules.util;

public class StringUtils {
    /**
     * Capitalizes the first letter of each word
     */
    public static String capitalizeWords(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        // Implementation
    }
}
```

2. **Register in configuration** if needed

3. **Add tests**:
```java
@Test
void testCapitalizeWords() {
    assertEquals("Hello World", StringUtils.capitalizeWords("hello world"));
}
```

4. **Document** in user guide

### Task: Modify Parser Grammar

**Warning**: ⚠️ High risk - affects all rules

**Steps**:

1. **Backup current grammar**:
```bash
cp DEV/org.openl.rules/grammar/bexgrammar.jj DEV/org.openl.rules/grammar/bexgrammar.jj.backup
```

2. **Modify grammar file** (`/DEV/org.openl.rules/grammar/bexgrammar.jj`):
```javacc
// Example: Add new operator
void NewOperator() :
{}
{
    <NEW_KEYWORD> <IDENTIFIER>
    {
        // Semantic actions
    }
}
```

3. **Regenerate parser**:
```bash
cd DEV/org.openl.rules
mvn generate-sources
```

4. **Test extensively**:
```bash
# Run all parser tests
mvn test -Dtest=*Parser*

# Run integration tests
cd ../../ITEST
mvn verify
```

5. **Document breaking changes** if any

---

## Working with Web Studio

### Task: Add New REST Endpoint

**Example**: Add endpoint to export project statistics

**Steps**:

1. **Create DTO** (`/STUDIO/org.openl.rules.webstudio/src/.../model/`):
```java
package org.openl.rules.webstudio.model;

public class ProjectStatistics {
    private String projectName;
    private int tableCount;
    private int methodCount;

    // Getters, setters, constructors
}
```

2. **Create service method** (`/STUDIO/org.openl.rules.webstudio/src/.../service/`):
```java
@Service
public class ProjectStatisticsService {

    public ProjectStatistics getStatistics(String projectName) {
        // Analyze project
        // Return statistics
    }
}
```

3. **Create REST controller** (`/STUDIO/org.openl.rules.webstudio/src/.../controller/`):
```java
@RestController
@RequestMapping("/api/projects")
public class ProjectStatisticsController {

    private final ProjectStatisticsService statsService;

    @GetMapping("/{name}/statistics")
    public ResponseEntity<ProjectStatistics> getStats(@PathVariable String name) {
        return ResponseEntity.ok(statsService.getStatistics(name));
    }
}
```

4. **Add security check**:
```java
@PreAuthorize("hasPermission(#name, 'project', 'read')")
@GetMapping("/{name}/statistics")
public ResponseEntity<ProjectStatistics> getStats(@PathVariable String name) {
    // ...
}
```

5. **Write tests**:
```java
@SpringBootTest
@AutoConfigureMockMvc
class ProjectStatisticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetStatistics() throws Exception {
        mockMvc.perform(get("/api/projects/my-project/statistics"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.projectName").value("my-project"));
    }
}
```

6. **Update OpenAPI documentation** (auto-generated from annotations)

### Task: Add React Component

**Example**: Project statistics widget

**Steps**:

1. **Create component** (`/STUDIO/studio-ui/src/components/projects/`):
```typescript
// ProjectStats.tsx
import React, { useEffect, useState } from 'react';
import { Card, Statistic, Row, Col } from 'antd';
import { ProjectApi } from '@/api/projectApi';

interface ProjectStatsProps {
  projectName: string;
}

export const ProjectStats: React.FC<ProjectStatsProps> = ({ projectName }) => {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadStats();
  }, [projectName]);

  const loadStats = async () => {
    setLoading(true);
    try {
      const data = await ProjectApi.getStatistics(projectName);
      setStats(data);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Card title="Project Statistics" loading={loading}>
      <Row gutter={16}>
        <Col span={12}>
          <Statistic title="Tables" value={stats?.tableCount} />
        </Col>
        <Col span={12}>
          <Statistic title="Methods" value={stats?.methodCount} />
        </Col>
      </Row>
    </Card>
  );
};
```

2. **Add API method** (`/STUDIO/studio-ui/src/api/projectApi.ts`):
```typescript
export const ProjectApi = {
  getStatistics: async (name: string): Promise<ProjectStatistics> => {
    const response = await apiClient.get(`/projects/${name}/statistics`);
    return response.data;
  },
};
```

3. **Add translations** (`/STUDIO/studio-ui/src/i18n/locales/en.json`):
```json
{
  "projectStats": {
    "title": "Project Statistics",
    "tables": "Tables",
    "methods": "Methods"
  }
}
```

4. **Write tests**:
```typescript
import { render, screen, waitFor } from '@testing-library/react';
import { ProjectStats } from './ProjectStats';

test('renders project statistics', async () => {
  render(<ProjectStats projectName="test-project" />);

  await waitFor(() => {
    expect(screen.getByText('Tables')).toBeInTheDocument();
  });
});
```

5. **Build and test**:
```bash
cd STUDIO/studio-ui
npm run build
npm test
```

---

## Working with Rule Services

### Task: Deploy New Rule Service

**Steps**:

1. **Create `rules-deploy.xml`** in production repository:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<rules>
  <serviceName>AutoInsuranceService</serviceName>
  <url>AutoInsuranceService</url>
  <serviceClass>com.example.IAutoInsurance</serviceClass>

  <ruleServiceConfiguration>
    <project>rules/AutoInsurance</project>
    <lazy-modules-for-compilation>false</lazy-modules-for-compilation>
    <support-variations>true</support-variations>
  </ruleServiceConfiguration>

  <publishers>
    <publisher>RESTFUL</publisher>
    <publisher>WEBSERVICE</publisher>
  </publishers>
</rules>
```

2. **Publish rule project** from WebStudio to production repository

3. **Restart rule service** or wait for auto-deployment:
```bash
# If manual restart needed
cd WSFrontend/org.openl.rules.ruleservice.ws
mvn spring-boot:run
```

4. **Verify service is deployed**:
```bash
# Check service list
curl http://localhost:8081/admin/services

# Get WSDL (SOAP)
curl http://localhost:8081/AutoInsuranceService?wsdl

# Get OpenAPI (REST)
curl http://localhost:8081/AutoInsuranceService/openapi.json
```

5. **Test service invocation**:
```bash
# REST
curl -X POST http://localhost:8081/AutoInsuranceService/calculatePremium \
  -H "Content-Type: application/json" \
  -d '{"driver": {"age": 25}, "vehicle": {"year": 2020}}'

# SOAP
curl -X POST http://localhost:8081/AutoInsuranceService \
  -H "Content-Type: text/xml" \
  -d '<soap:Envelope>...</soap:Envelope>'
```

### Task: Add Kafka Support to Service

**Steps**:

1. **Update `rules-deploy.xml`**:
```xml
<publishers>
  <publisher>RESTFUL</publisher>
  <publisher>KAFKA</publisher>
</publishers>
```

2. **Configure Kafka** (`application.yml`):
```yaml
ruleservice:
  kafka:
    enabled: true
    consumer:
      bootstrap-servers: localhost:9092
      topics:
        - rule-requests
    producer:
      bootstrap-servers: localhost:9092
      topic: rule-responses
```

3. **Restart service**

4. **Test with Kafka**:
```bash
# Produce request message
kafka-console-producer --broker-list localhost:9092 --topic rule-requests
{"serviceName":"AutoInsuranceService","methodName":"calculatePremium","parameters":[...]}

# Consume response
kafka-console-consumer --bootstrap-server localhost:9092 --topic rule-responses --from-beginning
```

---

## Testing

### Run All Tests

```bash
# Unit tests only
mvn test

# Integration tests
cd ITEST
mvn verify

# Specific module tests
cd DEV/org.openl.rules
mvn test
```

### Run Specific Test

```bash
# By class name
mvn test -Dtest=MyTestClass

# By method name
mvn test -Dtest=MyTestClass#testMethod

# Pattern matching
mvn test -Dtest=*Parser*
```

### Debug Tests

```bash
# Run with debug enabled
mvn test -Dmaven.surefire.debug

# Connect debugger to port 5005
```

### Frontend Tests

```bash
cd STUDIO/studio-ui

# Run all tests
npm test

# Run with coverage
npm run test:coverage

# Run specific test
npm test ProjectCard.test.tsx

# Watch mode
npm test -- --watch
```

---

## Git Workflow

### Create Feature Branch

```bash
# Update main
git checkout main
git pull origin main

# Create feature branch
git checkout -b feature/my-feature

# Make changes
# ...

# Commit
git add .
git commit -m "feat: Add new feature"

# Push
git push -u origin feature/my-feature
```

### Create Pull Request

```bash
# Ensure tests pass
mvn clean install -Dquick

# Push final changes
git push origin feature/my-feature

# Create PR via GitHub web interface or CLI
gh pr create --title "Add new feature" --body "Description"
```

### Review Changes Before Commit

```bash
# See what changed
git status

# See detailed diff
git diff

# See staged changes
git diff --staged

# Review specific file
git diff path/to/file
```

---

## Troubleshooting

### Build Failures

**Problem**: Maven build fails with compilation errors

**Solutions**:
```bash
# Clean Maven cache
mvn clean install -U

# Clean .m2 repository
rm -rf ~/.m2/repository/org/openl

# Rebuild from scratch
mvn clean install -DskipTests
```

**Problem**: Frontend build fails

**Solutions**:
```bash
cd STUDIO/studio-ui

# Clean node_modules
rm -rf node_modules package-lock.json
npm install

# Clear npm cache
npm cache clean --force
npm install
```

### Runtime Issues

**Problem**: WebStudio won't start

**Check**:
```bash
# Check port 8080 is free
lsof -i :8080

# Check logs
tail -f STUDIO/org.openl.rules.webstudio/target/*.log

# Try different port
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8090
```

**Problem**: Rules not compiling

**Debug**:
```bash
# Enable debug logging
mvn spring-boot:run -Dlogging.level.org.openl=DEBUG
```

### Test Failures

**Problem**: Tests fail inconsistently

**Try**:
```bash
# Run tests in isolation
mvn test -Dtest=MyTest

# Increase memory
export MAVEN_OPTS="-Xmx2g -XX:MaxPermSize=512m"
mvn test

# Skip flaky tests
mvn test -Dtest=!FlakyTest
```

---

## Quick Reference

### Build Commands

| Command | Purpose |
|---------|---------|
| `mvn clean install` | Full build with tests |
| `mvn clean install -Dquick` | Quick build (skip heavy tests) |
| `mvn clean install -DskipTests` | Build without tests |
| `mvn test` | Run unit tests only |
| `mvn spring-boot:run` | Start Spring Boot application |

### Module Locations

| Module | Path |
|--------|------|
| Core Engine | `/DEV/org.openl.rules/` |
| Web Studio Backend | `/STUDIO/org.openl.rules.webstudio/` |
| Web Studio Frontend | `/STUDIO/studio-ui/` |
| Rule Services | `/WSFrontend/org.openl.rules.ruleservice/` |
| Maven Plugin | `/Util/openl-maven-plugin/` |

### Ports

| Service | Default Port |
|---------|-------------|
| Web Studio | 8080 |
| Rule Service | 8081 |
| Frontend Dev Server | 3000 |

---

**See Also**:
- [Troubleshooting Guide](troubleshooting.md)
- [Testing Guide](../guides/testing-guide.md)
- [Development Setup](development-setup.md)

**Last Updated**: 2025-11-05
