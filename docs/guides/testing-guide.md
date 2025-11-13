# OpenL Tablets Testing Guide

**Last Updated**: 2025-11-05
**Version**: 6.0.0-SNAPSHOT
**Applies To**: All modules

---

## Table of Contents

- [Overview](#overview)
- [Testing Infrastructure](#testing-infrastructure)
- [Test Organization](#test-organization)
- [Unit Testing](#unit-testing)
- [Integration Testing](#integration-testing)
- [Test Frameworks and Tools](#test-frameworks-and-tools)
- [Writing Tests](#writing-tests)
- [Running Tests](#running-tests)
- [Test Best Practices](#test-best-practices)
- [Troubleshooting Tests](#troubleshooting-tests)

---

## Overview

OpenL Tablets uses a comprehensive testing strategy combining unit tests, integration tests, and end-to-end tests to ensure code quality and reliability.

### Testing Philosophy

1. **Unit Tests**: Test individual components in isolation
2. **Integration Tests**: Test component interactions and system integration
3. **End-to-End Tests**: Test complete workflows and user scenarios
4. **Test Isolation**: Tests must be independent and repeatable
5. **Fast Feedback**: Unit tests run quickly, integration tests run in CI

### Test Coverage Goals

- **Unit Tests**: 70%+ code coverage for core modules
- **Integration Tests**: All major workflows covered
- **Critical Paths**: 100% coverage for security and data persistence

---

## Testing Infrastructure

### Test Frameworks

| Framework | Version | Purpose |
|-----------|---------|---------|
| **JUnit 5** | 5.14.0 | Primary test framework |
| **JUnit Pioneer** | 2.3.0 | JUnit 5 extensions |
| **Mockito** | 5.20.0 | Mocking framework |
| **Spring Test** | 6.2.11 | Spring integration testing |
| **TestContainers** | 1.21.3 | Docker-based test containers |
| **Awaitility** | 4.3.0 | Async testing support |
| **XMLUnit** | 2.10.4 | XML comparison |
| **Greenmail** | 2.1.6 | Email testing |
| **DataSource Proxy** | 1.11.0 | Query monitoring |

### Custom Test Configuration

OpenL Tablets uses **non-standard** test directories:
```
test/                    # Unit tests (instead of src/test/java)
test-resources/          # Test resources (instead of src/test/resources)
resources/db/flyway/     # Database migration test fixtures
```

### Memory Settings

Tests run with strict memory limits to detect memory issues early:

```xml
<!-- Default test settings -->
<argLine>
  -Xms8m -Xmx128m -Xss256k
  -XX:MaxMetaspaceSize=160m
</argLine>

<!-- Performance-sensitive tests -->
<argLine>
  -Xms8m -Xmx61m -Xss256k
  -XX:MaxMetaspaceSize=160m
</argLine>
```

**Rationale**: Low memory limits ensure tests don't mask memory leaks or excessive allocations.

### Maven Surefire Configuration

```xml
<plugin>
  <artifactId>maven-surefire-plugin</artifactId>
  <configuration>
    <forkCount>1</forkCount>
    <reuseForks>false</reuseForks>
    <testSourceDirectory>${project.basedir}/test</testSourceDirectory>
  </configuration>
</plugin>
```

**Key Settings**:
- `forkCount=1`: One JVM per test class for isolation
- `reuseForks=false`: Fresh JVM for each test class
- Custom test source directory

---

## Test Organization

### Module Structure

Each module follows this test structure:

```
org.openl.rules.example/
├── src/                           # Production code
├── test/                          # Unit tests
│   └── org/openl/rules/example/  # Mirrors src structure
├── test-resources/                # Test fixtures
│   ├── test-data/                # Test input files
│   ├── expected-output/          # Expected results
│   └── application-test.yml      # Test configuration
└── pom.xml
```

### Integration Test Modules (ITEST)

OpenL Tablets has dedicated integration test modules in `/ITEST/`:

| Module | Purpose |
|--------|---------|
| `itest.smoke` | Basic functionality smoke tests |
| `itest.webstudio` | Web Studio integration tests |
| `itest.security` | Security and authentication tests |
| `itest.security.cas` | CAS authentication tests |
| `itest.security.saml` | SAML authentication tests |
| `itest.webservice.rest` | REST API tests |
| `itest.webservice.soap` | SOAP service tests |
| `itest.spring-boot` | Spring Boot integration tests |
| `itest.kafka.smoke` | Kafka integration tests |
| `itest.minio` | MinIO storage tests |
| `itest.healthchecks` | Health check endpoint tests |
| `itest.tracing` | OpenTelemetry tracing tests |
| `itest.storelogdata` | Log storage tests |
| `itest.deployment-filters` | Deployment filter tests |

### Test Naming Conventions

```java
// Unit test class names
public class RulesCompilerTest { }
public class ProjectDescriptorTest { }

// Integration test class names
public class RestApiIntegrationTest { }
public class SecurityIntegrationTest { }

// Test method names (use descriptive names)
@Test
public void shouldCompileSimpleRule() { }

@Test
public void shouldThrowExceptionWhenProjectNameIsNull() { }

@Test
public void shouldReturnEmptyListWhenNoProjectsExist() { }
```

---

## Unit Testing

### Basic Unit Test Example

```java
package org.openl.rules.project;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

class ProjectDescriptorTest {

    private ProjectDescriptor descriptor;

    @BeforeEach
    void setUp() {
        descriptor = new ProjectDescriptor();
    }

    @Test
    void shouldSetProjectName() {
        // Given
        String projectName = "my-project";

        // When
        descriptor.setName(projectName);

        // Then
        assertEquals(projectName, descriptor.getName());
    }

    @Test
    void shouldThrowExceptionWhenNameIsNull() {
        // When / Then
        assertThrows(IllegalArgumentException.class,
            () -> descriptor.setName(null));
    }
}
```

### Using Mockito

```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RulesServiceTest {

    @Mock
    private ProjectRepository repository;

    @Mock
    private RulesCompiler compiler;

    @InjectMocks
    private RulesService service;

    @Test
    void shouldCompileProject() {
        // Given
        String projectName = "test-project";
        Project project = new Project(projectName);
        when(repository.findByName(projectName)).thenReturn(project);
        when(compiler.compile(project)).thenReturn(new CompiledProject());

        // When
        CompiledProject result = service.compileProject(projectName);

        // Then
        assertNotNull(result);
        verify(repository).findByName(projectName);
        verify(compiler).compile(project);
    }
}
```

### Parameterized Tests

```java
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

class ValidationTest {

    @ParameterizedTest
    @ValueSource(strings = {"project1", "my-project", "test_project"})
    void shouldAcceptValidProjectNames(String name) {
        assertTrue(ProjectValidator.isValidName(name));
    }

    @ParameterizedTest
    @CsvSource({
        "123, true",
        "-1, false",
        "0, false"
    })
    void shouldValidatePositiveNumbers(int number, boolean expected) {
        assertEquals(expected, NumberValidator.isPositive(number));
    }
}
```

### Testing Exceptions

```java
@Test
void shouldThrowExceptionWithMessage() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> service.processInvalidInput(null)
    );

    assertEquals("Input cannot be null", exception.getMessage());
}
```

---

## Integration Testing

### Spring Integration Tests

```java
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringBootTest
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldCreateUser() {
        // Given
        String username = "testuser";

        // When
        User user = userService.createUser(username);

        // Then
        assertNotNull(user.getId());
        assertEquals(username, user.getUsername());

        // Verify database persistence
        User saved = userRepository.findById(user.getId()).orElse(null);
        assertNotNull(saved);
    }
}
```

### Database Testing with Flyway

OpenL Tablets uses Flyway for database migrations in tests:

```java
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

@SpringJUnitConfig
class DatabaseIntegrationTest {

    @Autowired
    private Flyway flyway;

    @BeforeEach
    void resetDatabase() {
        // Clean and migrate database before each test
        flyway.clean();
        flyway.migrate();
    }

    @Test
    void shouldQueryDatabase() {
        // Test database operations
    }
}
```

### TestContainers for External Dependencies

```java
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class PostgresIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("openl")
            .withUsername("test")
            .withPassword("test");

    @Test
    void shouldConnectToPostgres() {
        String jdbcUrl = postgres.getJdbcUrl();
        // Use jdbcUrl for testing
    }
}
```

### Keycloak Integration Testing

```java
import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class KeycloakAuthenticationTest {

    @Container
    private static final KeycloakContainer keycloak =
        new KeycloakContainer()
            .withRealmImportFile("/test-realm.json");

    @Test
    void shouldAuthenticateWithKeycloak() {
        String authUrl = keycloak.getAuthServerUrl();
        // Test OAuth2 authentication
    }
}
```

### REST API Testing

```java
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class RestApiIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldGetProjects() {
        // When
        ResponseEntity<String> response = restTemplate
            .getForEntity("/api/projects", String.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void shouldCreateProject() {
        // Given
        ProjectDTO project = new ProjectDTO("test-project");

        // When
        ResponseEntity<ProjectDTO> response = restTemplate
            .postForEntity("/api/projects", project, ProjectDTO.class);

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody().getId());
    }
}
```

### Async Testing with Awaitility

```java
import org.awaitility.Awaitility;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

@Test
void shouldProcessAsyncTask() {
    // Given
    String taskId = "task-123";
    service.submitTask(taskId);

    // When / Then
    await()
        .atMost(5, SECONDS)
        .pollInterval(100, TimeUnit.MILLISECONDS)
        .until(() -> service.isTaskComplete(taskId));

    TaskResult result = service.getTaskResult(taskId);
    assertNotNull(result);
}
```

---

## Test Frameworks and Tools

### JUnit 5 Assertions

```java
import static org.junit.jupiter.api.Assertions.*;

// Basic assertions
assertEquals(expected, actual);
assertNotEquals(unexpected, actual);
assertTrue(condition);
assertFalse(condition);
assertNull(object);
assertNotNull(object);

// Collection assertions
assertIterableEquals(expectedList, actualList);
assertArrayEquals(expectedArray, actualArray);

// Exception assertions
assertThrows(Exception.class, () -> method());
assertDoesNotThrow(() -> method());

// Timeout assertions
assertTimeout(Duration.ofSeconds(1), () -> method());

// Assertion messages
assertEquals(expected, actual, "Values should be equal");
assertEquals(expected, actual, () -> "Expensive message: " + compute());
```

### Mockito Patterns

```java
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

// Stubbing
when(mock.method()).thenReturn(value);
when(mock.method(anyString())).thenReturn(value);
when(mock.method(eq("specific"))).thenReturn(value);

// Stubbing with exceptions
when(mock.method()).thenThrow(new RuntimeException());

// Verification
verify(mock).method();
verify(mock, times(2)).method();
verify(mock, never()).method();
verify(mock, atLeastOnce()).method();

// Argument capture
ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
verify(mock).method(captor.capture());
String capturedValue = captor.getValue();

// Spy (partial mock)
List<String> list = new ArrayList<>();
List<String> spy = spy(list);
doReturn("foo").when(spy).get(0);
```

### XMLUnit for XML Testing

```java
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

@Test
void shouldGenerateCorrectXml() {
    String expectedXml = "<project><name>test</name></project>";
    String actualXml = generator.generateXml();

    Diff diff = DiffBuilder
        .compare(expectedXml)
        .withTest(actualXml)
        .ignoreWhitespace()
        .build();

    assertFalse(diff.hasDifferences(),
        "XML should match: " + diff.toString());
}
```

### Query Count Validation

```java
import net.ttddyy.dsproxy.QueryCountHolder;

@Test
void shouldNotCauseNPlusOneQuery() {
    // Given
    QueryCountHolder.clear();

    // When
    List<Project> projects = repository.findAllWithDependencies();

    // Then
    int selectCount = QueryCountHolder.getGrandTotal().getSelect();
    assertTrue(selectCount <= 2,
        "Should use at most 2 SELECT queries, but used: " + selectCount);
}
```

---

## Writing Tests

### Test Structure (Given-When-Then)

```java
@Test
void shouldCalculateTotalPrice() {
    // Given (Setup)
    ShoppingCart cart = new ShoppingCart();
    cart.addItem(new Item("Book", 10.00));
    cart.addItem(new Item("Pen", 2.50));

    // When (Execute)
    double total = cart.calculateTotal();

    // Then (Assert)
    assertEquals(12.50, total, 0.01);
}
```

### Test Data Builders

```java
// Builder pattern for test data
class ProjectBuilder {
    private String name = "default-project";
    private String version = "1.0";
    private List<Module> modules = new ArrayList<>();

    public ProjectBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public ProjectBuilder withVersion(String version) {
        this.version = version;
        return this;
    }

    public ProjectBuilder withModule(Module module) {
        this.modules.add(module);
        return this;
    }

    public Project build() {
        Project project = new Project();
        project.setName(name);
        project.setVersion(version);
        project.setModules(modules);
        return project;
    }
}

// Usage in tests
@Test
void shouldProcessProject() {
    Project project = new ProjectBuilder()
        .withName("test-project")
        .withVersion("2.0")
        .withModule(new Module("rules"))
        .build();

    Result result = processor.process(project);
    assertTrue(result.isSuccess());
}
```

### Test Fixtures

```java
// Shared test fixtures
class TestFixtures {

    public static Project createSimpleProject() {
        Project project = new Project();
        project.setName("simple-project");
        project.setVersion("1.0");
        return project;
    }

    public static User createTestUser() {
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        return user;
    }
}

// Usage
@Test
void shouldProcessSimpleProject() {
    Project project = TestFixtures.createSimpleProject();
    processor.process(project);
}
```

### Resource Loading

```java
@Test
void shouldLoadTestResource() throws IOException {
    // Load from test-resources
    InputStream inputStream = getClass()
        .getResourceAsStream("/test-data/sample-project.xlsx");
    assertNotNull(inputStream);

    // Or using ClassPathResource (Spring)
    Resource resource = new ClassPathResource("test-data/sample-project.xlsx");
    assertTrue(resource.exists());
}
```

---

## Running Tests

### Maven Commands

```bash
# Run all tests
mvn clean test

# Run specific test class
mvn test -Dtest=ProjectDescriptorTest

# Run specific test method
mvn test -Dtest=ProjectDescriptorTest#shouldSetProjectName

# Run tests matching pattern
mvn test -Dtest=*IntegrationTest

# Skip tests
mvn clean install -DskipTests

# Run tests with debugging
mvn test -Dmaven.surefire.debug

# Run tests with specific memory settings
mvn test -DargLine="-Xmx256m"

# Run integration tests only
mvn verify -Pintegration-tests
```

### Running Tests in IDE

#### IntelliJ IDEA

1. **Run single test**: Right-click test method → Run
2. **Run test class**: Right-click test class → Run
3. **Run all tests in module**: Right-click module → Run 'All Tests'
4. **Debug test**: Right-click → Debug
5. **Re-run failed tests**: Click "Re-run failed tests" in test results

**Custom test source directory configuration**:
- File → Project Structure → Modules
- Mark `test/` as Test Sources
- Mark `test-resources/` as Test Resources

#### Eclipse

1. **Run single test**: Right-click test → Run As → JUnit Test
2. **Run test class**: Right-click class → Run As → JUnit Test
3. **Debug test**: Right-click → Debug As → JUnit Test

### Continuous Integration

Tests run automatically in GitHub Actions:

```yaml
# .github/workflows/build.yml
- name: Run tests
  run: mvn clean verify

- name: Upload test results
  if: failure()
  uses: actions/upload-artifact@v3
  with:
    name: test-results
    path: '**/target/surefire-reports/'
```

---

## Test Best Practices

### 1. Test Independence

❌ **Bad** (Tests depend on each other):
```java
private static Project project;

@Test
void test1_createProject() {
    project = service.createProject("test");
}

@Test
void test2_updateProject() {
    project.setVersion("2.0");  // Depends on test1
    service.update(project);
}
```

✅ **Good** (Independent tests):
```java
@Test
void shouldCreateProject() {
    Project project = service.createProject("test");
    assertNotNull(project);
}

@Test
void shouldUpdateProject() {
    Project project = service.createProject("test");
    project.setVersion("2.0");
    service.update(project);
    assertEquals("2.0", project.getVersion());
}
```

### 2. Meaningful Test Names

❌ **Bad**:
```java
@Test void test1() { }
@Test void testCompile() { }
```

✅ **Good**:
```java
@Test void shouldCompileSimpleRule() { }
@Test void shouldThrowExceptionWhenRuleSyntaxIsInvalid() { }
@Test void shouldReturnEmptyListWhenNoRulesExist() { }
```

### 3. One Assertion Per Concept

❌ **Bad** (Multiple unrelated assertions):
```java
@Test
void testProject() {
    assertEquals("test", project.getName());
    assertEquals(3, project.getModules().size());
    assertTrue(project.isValid());
    assertNotNull(project.getCreatedDate());
}
```

✅ **Good** (Split into focused tests):
```java
@Test void shouldHaveCorrectName() {
    assertEquals("test", project.getName());
}

@Test void shouldHaveThreeModules() {
    assertEquals(3, project.getModules().size());
}

@Test void shouldBeValid() {
    assertTrue(project.isValid());
}
```

### 4. Use Test Doubles Appropriately

**Stub**: Returns predefined values
```java
when(repository.findById(1L)).thenReturn(project);
```

**Mock**: Verifies interactions
```java
service.deleteProject(1L);
verify(repository).delete(1L);
```

**Spy**: Partial mock of real object
```java
List<String> spy = spy(new ArrayList<>());
doReturn(10).when(spy).size();
```

### 5. Clean Test Data

```java
@BeforeEach
void setUp() {
    // Clean state before each test
    database.clean();
}

@AfterEach
void tearDown() {
    // Clean up resources after each test
    fileSystem.deleteTestFiles();
}
```

### 6. Test Edge Cases

```java
@Test void shouldHandleNullInput() { }
@Test void shouldHandleEmptyList() { }
@Test void shouldHandleVeryLargeInput() { }
@Test void shouldHandleSpecialCharacters() { }
@Test void shouldHandleConcurrentAccess() { }
```

### 7. Use Descriptive Assertion Messages

```java
assertEquals(expected, actual,
    () -> String.format("Project name should be '%s' but was '%s'",
        expected, actual));
```

---

## Troubleshooting Tests

### Common Issues

#### 1. OutOfMemoryError in Tests

**Problem**: Tests fail with `java.lang.OutOfMemoryError`

**Solution**:
```xml
<argLine>-Xmx256m</argLine>
```

Or temporarily increase for specific tests:
```java
// Add VM option in IDE: -Xmx512m
```

#### 2. Tests Pass Individually, Fail Together

**Problem**: Test order dependency or shared state

**Solution**:
- Ensure tests are independent
- Use `@BeforeEach` to reset state
- Avoid static fields
- Use `forkCount=1` and `reuseForks=false` in Maven

#### 3. Flaky Tests

**Problem**: Tests pass/fail randomly

**Common causes**:
- Timing issues (use Awaitility)
- External dependencies (use TestContainers or mocks)
- Shared resources (ensure cleanup)
- Non-deterministic code (seed random generators)

**Solution**:
```java
// Use Awaitility for async code
await().atMost(5, SECONDS).until(() -> condition);

// Seed random generators
Random random = new Random(12345);

// Mock current time
Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
```

#### 4. Database Tests Fail

**Problem**: Database state issues

**Solution**:
```java
@BeforeEach
void resetDatabase() {
    flyway.clean();
    flyway.migrate();
}
```

#### 5. TestContainers Timeout

**Problem**: Container startup timeout

**Solution**:
```java
@Container
private static final PostgreSQLContainer<?> postgres =
    new PostgreSQLContainer<>("postgres:15-alpine")
        .withStartupTimeout(Duration.ofMinutes(2));
```

### Debug Logging

Enable debug logging for tests:

```properties
# test-resources/logback-test.xml
<configuration>
    <logger name="org.openl" level="DEBUG"/>
    <logger name="org.springframework" level="INFO"/>
    <root level="WARN">
        <appender-ref ref="STDOUT"/>
    </root>
</configuration>
```

### Running Single Test with Maven Debug

```bash
mvn test -Dtest=MyTest -Dmaven.surefire.debug
# Connect debugger to port 5005
```

---

## Test Coverage

### Measuring Coverage with JaCoCo

```bash
# Run tests with coverage
mvn clean test jacoco:report

# View report
open target/site/jacoco/index.html
```

### Coverage Goals

- **Core Engine**: 80%+ coverage
- **API Layer**: 70%+ coverage
- **Utilities**: 60%+ coverage
- **Critical Security Code**: 90%+ coverage

---

## Summary

### Quick Reference

| Task | Command |
|------|---------|
| Run all tests | `mvn test` |
| Run specific test | `mvn test -Dtest=ClassName` |
| Run integration tests | `mvn verify` |
| Skip tests | `mvn install -DskipTests` |
| Debug test | `mvn test -Dmaven.surefire.debug` |
| Coverage report | `mvn test jacoco:report` |

### Key Takeaways

1. Use JUnit 5 for all new tests
2. Follow Given-When-Then structure
3. Keep tests independent and fast
4. Use TestContainers for external dependencies
5. Mock external dependencies, test real logic
6. Write descriptive test names
7. Clean up test data after execution
8. Test edge cases and error conditions

---

## Related Documentation

- [Development Setup](../onboarding/development-setup.md) - Setting up test environment
- [Common Tasks](../onboarding/common-tasks.md#running-tests) - Running tests
- [Troubleshooting](../onboarding/troubleshooting.md) - Common test issues
- [CI/CD Pipeline](../operations/ci-cd.md) - Automated testing

---

**Last Updated**: 2025-11-05
**Maintainer**: OpenL Tablets Team
