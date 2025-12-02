# ITEST Module - Claude Code Conventions

**Module**: OpenL Tablets ITEST (Integration Tests)
**Version**: 6.0.0-SNAPSHOT
**Last Updated**: 2025-12-02

This file provides specific guidance for integration testing in the OpenL Tablets project. For general project conventions, see the root `/AGENTS.md`.

---

## Module Overview

ITEST contains comprehensive integration tests for OpenL Tablets. It tests end-to-end workflows across multiple modules (DEV, STUDIO, WSFrontend) using real services, databases, and Docker containers.

**Key Characteristics**:
- **Multiple test suites**: Smoke tests, security, WebStudio E2E, Kafka, MinIO, etc.
- **Docker-based**: Uses TestContainers for PostgreSQL, Keycloak, MinIO, Kafka
- **Slow but thorough**: Full system testing, not unit tests
- **CI/CD integration**: Runs on schedule and in pull requests
- **Windows/macOS limitation**: Docker tests skipped on non-Linux

---

## Module Structure

```
ITEST/
├── server-core/                       # Shared test utilities
│   ├── src/test/java/org/openl/
│   │   └── itest/
│   │       ├── utils/                # Test utilities
│   │       ├── config/               # Test configuration
│   │       ├── matchers/             # Custom matchers
│   │       └── fixtures/             # Test data
│   └── pom.xml
│
├── itest.smoke/                       # Quick smoke tests (main)
│   ├── src/test/java/org/openl/itest/smoke/
│   │   ├── RuleExecutionTest.java
│   │   ├── RuleParsingTest.java
│   │   ├── DataTypeTest.java
│   │   └── ...
│   ├── src/test/resources/
│   │   ├── excel-files/               # Test Excel files
│   │   └── rules.xml                  # Test rule definitions
│   └── pom.xml
│
├── itest.security/                    # Authentication/authorization tests
├── itest.webstudio/                   # WebStudio E2E tests
├── itest.kafka.smoke/                 # Kafka integration tests
├── itest.minio/                       # S3 storage tests
├── itest.tracing/                     # OpenTelemetry tests
├── itest.datasources/                 # Database integration tests
├── ... (11 more test modules)
│
└── pom.xml                            # Root ITEST POM
```

---

## Request/Response File Testing (*.req and *.resp)

OpenL Tablets ITEST uses a unique declarative testing approach: HTTP request/response files. This is the **primary testing mechanism** for WebService integration tests.

### Overview

Instead of writing Java test code, you create `.req` (request) and `.resp` (response) files that define HTTP exchanges. The test framework automatically:
- Discovers all `*.req` files in `test-resources/` directories
- Executes them in lexicographic order
- Matches each request with corresponding `*.resp` file
- Compares actual responses with expected responses
- Reports pass/fail with execution time

**Key advantages**:
- Non-technical stakeholders can understand tests
- Easy to add new test cases without coding
- Tests remain stable across refactoring
- Great for regression testing
- Supports test grouping and ordering

### File Structure

```
test-resources/
├── EPBDS-10366.req                    # Simple request
├── EPBDS-10366.resp                   # Simple response
├── EPBDS-13489/
│   ├── 00.req                        # Grouped tests (alphabetical order)
│   ├── 00.resp
│   ├── 01.req
│   ├── 01.resp
│   └── ...
└── category-name/
    ├── test-case-1.req
    ├── test-case-1.resp
    └── ...
```

**Naming convention**:
- File pairs must have same name, different extensions: `name.req` and `name.resp`
- Prefix with test ID: `EPBDS-13489.req`
- Group related tests in subdirectories
- Use numeric prefixes for test ordering: `00.req`, `01.req`, `02.req`, etc.

**⚠️ CRITICAL - Line Endings (CRLF Required)**:

Files **MUST use CRLF (`\r\n`) line endings**, not LF (`\n`):
- HTTP protocol requires CRLF between headers and between header/body
- Parser explicitly expects: carriage return (ASCII 13) + line feed (ASCII 10)
- **Files with LF-only will silently fail to parse**

**Configure your editor to use CRLF**:
- **VS Code**: Set `"files.eol": "\r\n"` in settings or select "CRLF" in bottom right
- **IntelliJ IDEA**: Settings → Editor → Code Style → Line separator → `\r\n`
- **Git**: Add to `.gitattributes` to preserve CRLF:
  ```
  *.req eol=crlf
  *.resp eol=crlf
  ```

### Request File Format (*.req)

**Basic HTTP request structure**:

```
METHOD /url/path HTTP/1.1
Header-Name: value
Another-Header: value

[optional request body]
```

**Example - Simple GET**:

```
GET /MyService/methods/calculate HTTP/1.1

```

**Example - POST with JSON body**:

```
POST /MyService/methods/processData HTTP/1.1
Content-Type: application/json

{
  "name": "John",
  "age": 30,
  "items": [
    {"id": 1, "value": 100}
  ]
}
```

**Example - GET with query parameters**:

```
GET /MyService/methods/getUser?id=123&format=json HTTP/1.1

```

**HTTP Methods**:
- `GET` - Retrieve data
- `POST` - Send data
- `PUT` - Update resource
- `DELETE` - Remove resource
- `PATCH` - Partial update

### Response File Format (*.resp)

**Basic HTTP response structure**:

```
HTTP/1.1 STATUS_CODE [optional status text]
Header-Name: value
Another-Header: value

[optional response body]
```

**Example - JSON Response**:

```
HTTP/1.1 200
Content-Type: application/json

{
  "id": 1,
  "name": "Test Result",
  "status": "SUCCESS"
}
```

**Example - Empty 204 Response**:

```
HTTP/1.1 204

```

**Example - Error Response**:

```
HTTP/1.1 400
Content-Type: application/json

{
  "error": "Invalid input",
  "message": "The 'id' parameter is required"
}
```

**Common Status Codes**:
- `200` - OK (success)
- `201` - Created
- `204` - No Content
- `400` - Bad Request
- `401` - Unauthorized
- `403` - Forbidden
- `404` - Not Found
- `500` - Internal Server Error

### Test Settings (X-OpenL-Test-* Headers)

Special headers control test behavior:

**Retry on failure**:

```
GET /api/slow-endpoint HTTP/1.1
X-OpenL-Test-Retry: yes

```

If the response doesn't match expected, retry for up to 2 minutes with 100ms delays between attempts.

**Custom timeout**:

```
POST /api/heavy-computation HTTP/1.1
X-OpenL-Test-Timeout: 30000

```

Wait up to 30 seconds for this specific request.

### Environment Variable Substitution

Use `${VAR_NAME}` to inject environment variables:

```
GET /api/users/${USER_ID} HTTP/1.1

```

Will replace `${USER_ID}` with environment variable value. Variables are set in:
- Java system properties: `-DuserName=john`
- Environment variables: `export MY_VAR=value`
- Test configuration files

### Cookie Handling (Sessions)

Cookies are automatically managed **within each test subdirectory**:

```
test-resources/
├── EPBDS-13489/
│   ├── 00-login.req          # First request sets cookie
│   ├── 00-login.resp
│   ├── 01-get-data.req       # Cookie preserved
│   ├── 01-get-data.resp
│   └── 02-logout.req         # Still have cookie
│
└── EPBDS-13490/
    ├── 00-new-session.req    # Cookie RESET (new subdirectory)
    ├── 00-new-session.resp
```

**Cookie reset**:
- Automatic when moving to new subdirectory
- Preserves session within single directory
- Useful for testing multi-step flows with authentication

### Writing Tests - Step by Step

#### Example 1: Simple API Test

Create `test-resources/EPBDS-13584.req`:

```
GET /MyRules/methods/calculate?x=10&y=20 HTTP/1.1

```

Create `test-resources/EPBDS-13584.resp`:

```
HTTP/1.1 200
Content-Type: application/json

30
```

#### Example 2: POST with Complex JSON

Create `test-resources/EPBDS-13528/10-getAnimal.req`:

```
POST /AnimalService/methods/getAnimal HTTP/1.1
Content-Type: application/json

{
  "type": "dog",
  "breed": "labrador"
}
```

Create `test-resources/EPBDS-13528/10-getAnimal.resp`:

```
HTTP/1.1 200
Content-Type: application/json

{
  "name": "Buddy",
  "age": 5,
  "weight": 30.5
}
```

#### Example 3: Multi-Step Test with Session

Create `test-resources/EPBDS-13167/00-openapi.req`:

```
GET /openapi.json HTTP/1.1

```

Create `test-resources/EPBDS-13167/00-openapi.resp`:

```
HTTP/1.1 200
Content-Type: application/json

{
  "openapi": "3.0.0",
  "info": {"title": "API", "version": "1.0"}
}
```

Create `test-resources/EPBDS-13167/01-error.req`:

```
POST /MyService/methods/process HTTP/1.1
Content-Type: application/json

{"invalid": "data"}
```

Create `test-resources/EPBDS-13167/01-error.resp`:

```
HTTP/1.1 400
Content-Type: application/json

{
  "error": "Missing required field: name"
}
```

### Test Execution Details

**Java implementation**: `/ITEST/server-core/src/org/openl/itest/core/HttpClient.java`

```java
public void test(String path) {
    // 1. Walk directory recursively
    // 2. Find all *.req files
    // 3. Sort lexicographically
    // 4. For each .req file:
    //    a. Find matching .resp file
    //    b. Parse request
    //    c. Send HTTP request
    //    d. Compare actual vs expected response
    //    e. Report pass/fail with timing
}
```

**Execution output**:

```
=============== RESET COOKIE ===============
test-resources/EPBDS-13489/00.req - OK (125ms)
test-resources/EPBDS-13489/01.req - OK (89ms)
test-resources/EPBDS-13489/02.req - OK (156ms)
test-resources/EPBDS-13167/01-error.req - OK (45ms)
test-resources/EPBDS-13167/02-error.req - OK (78ms)
```

### Response Comparison

The framework compares:
- **Status code**: Must match exactly
- **Headers**: Content-Type, etc. (case-insensitive)
- **Body**:
  - JSON: Compared with flexible matching (whitespace ignored)
  - XML: Normalized comparison
  - Plain text: Exact match
  - Binary: Byte array comparison

### Best Practices

**DO**:
- ✅ **Use CRLF line endings** (not LF) - this is critical and non-negotiable
- ✅ Group related tests in subdirectories
- ✅ Use numeric prefixes for test order (`00-`, `01-`, etc.)
- ✅ Use descriptive file names with issue numbers (`EPBDS-13489.req`)
- ✅ Test happy path and error cases
- ✅ Include comments in complex requests (// allowed)
- ✅ Use retry for flaky services (`X-OpenL-Test-Retry: yes`)
- ✅ Keep request/response files small and focused

**DON'T**:
- ❌ Use LF-only line endings (will silently fail to parse)
- ❌ Mix unrelated tests in same directory
- ❌ Hardcode timestamps or IDs (use environment variables)
- ❌ Create massive JSON bodies (keep files readable)
- ❌ Test implementation details, test behavior
- ❌ Depend on test execution order outside subdirectory
- ❌ Ignore response body formatting issues

### Adding New Tests

1. Create `test-resources/EPBDS-XXXXX/` directory
2. Create `00.req` with HTTP request
3. Create `00.resp` with expected HTTP response
4. Run test: `mvn verify -Dtest=RunWebservicesITest`
5. Check console output for pass/fail
6. Iterate if response doesn't match

### Debugging Failed Tests

When tests fail:

```bash
# Run specific test
mvn test -Dtest=RunWebservicesITest

# View detailed output
mvn test -Dtest=RunWebservicesITest -X

# Check actual vs expected
# Look in console output for assertion details
```

**Common issues**:

| Issue | Cause | Solution |
|-------|-------|----------|
| **File silently fails to parse** | LF-only line endings (not CRLF) | **Check editor settings!** Convert to CRLF. Run `dos2unix` to verify: `file *.req` should show "CRLF line terminators" |
| Header mismatch | Extra/missing headers | Check `Content-Type`, remove non-essential headers from expected response |
| Status code mismatch | Wrong HTTP method | Verify GET/POST/PUT in request file |
| JSON body mismatch | Different formatting | JSON comparison ignores whitespace; check field values |
| Cookie issues | Cross-directory reference | Move tests to same subdirectory for session sharing |
| Timeout | Service too slow | Add `X-OpenL-Test-Retry: yes` or increase timeout |

---

## Running Integration Tests

### Full Test Suite

```bash
# All ITEST tests (includes Docker)
cd ITEST && mvn verify

# Specific test module
cd ITEST/itest.smoke && mvn verify

# Specific test class
mvn test -Dtest=RuleExecutionTest

# Specific test method
mvn test -Dtest=RuleExecutionTest#testSimpleRule_validInput_executesSuccessfully
```

### Quick Testing (Development)

```bash
# Smoke tests only (fastest)
cd ITEST/itest.smoke && mvn verify -Dquick

# Without Docker (on Windows/macOS)
mvn verify -DnoDocker

# Skip heavy tests
mvn verify -Dquick -DnoPerf
```

### CI/CD Testing

```bash
# Full matrix (Java 21/25, Ubuntu/Windows/macOS)
# Triggered on schedule or manual dispatch
# See .github/workflows/build.yml
```

---

## Writing Integration Tests

### Test Structure

**Location**: `itest.smoke/src/test/java/org/openl/itest/smoke/`

```java
package org.openl.itest.smoke;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.openl.itest.utils.*;

@DisplayName("Rule Execution Tests")
class RuleExecutionTest {

  @Test
  @DisplayName("Simple rule with valid input executes successfully")
  void testSimpleRule_validInput_executesSuccessfully() {
    // Arrange
    RulesEngineFactory<MyRules> factory = new RulesEngineFactory<>(
      "rules/simple.xlsx",
      MyRules.class);

    // Act
    MyRules rules = factory.newInstance();
    int result = rules.calculateScore(10, 20);

    // Assert
    assertEquals(30, result);
  }

  @Test
  void testComplexRule_multipleScenarios_allPass() {
    // Test multiple scenarios for complex rules
  }

  @Test
  void testRuleWithTypeConversion_validTypes_convertsAndExecutes() {
    // Test type conversion during rule execution
  }
}
```

### Naming Convention

```java
// Format: test<RuleOrFeature>_<Scenario>_<ExpectedResult>
@Test
void testDecisionTable_multipleConditions_returnsCorrectBranch() { }

@Test
void testSpreadsheet_complexCalculations_producesAccurateResults() { }

@Test
void testDataType_nestedStructure_parsesAndBindsCorrectly() { }
```

### Test Lifecycle

```java
@BeforeEach
void setUp() {
  // Initialize common test resources
  // Load shared Excel files
  // Configure test-specific settings
}

@AfterEach
void tearDown() {
  // Clean up resources
  // Close factories
  // Clear caches (if needed)
}

@BeforeAll
static void setupClass() {
  // One-time setup (expensive operations)
  // Docker containers start here
}

@AfterAll
static void tearDownClass() {
  // One-time cleanup
  // Docker containers stop here
}
```

---

## TestContainers Setup

### Database Testing

**PostgreSQL Container**:

```java
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class RuleServiceDatabaseTest {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
    .withDatabaseName("testdb")
    .withUsername("test")
    .withPassword("test");

  @Test
  void testRuleServiceWithDatabase() {
    String jdbcUrl = postgres.getJdbcUrl();
    // Use jdbcUrl in tests
  }
}
```

### Keycloak (OAuth2/OpenID Connect)

```java
import org.testcontainers.containers.GenericContainer;

@Testcontainers
class AuthenticationTest {

  @Container
  static GenericContainer<?> keycloak = new GenericContainer<>("keycloak/keycloak:latest")
    .withExposedPorts(8080)
    .withEnv("KEYCLOAK_ADMIN", "admin")
    .withEnv("KEYCLOAK_ADMIN_PASSWORD", "admin");

  @Test
  void testOAuth2Flow() {
    String keycloakUrl = "http://localhost:" + keycloak.getFirstMappedPort();
    // Test OAuth2 authentication
  }
}
```

### MinIO (S3-compatible Storage)

```java
import org.testcontainers.containers.MinIOContainer;

@Testcontainers
class StorageTest {

  @Container
  static MinIOContainer minio = new MinIOContainer("minio/minio:latest")
    .withUserName("minioadmin")
    .withPassword("minioadmin");

  @Test
  void testFileUpload() {
    String s3Url = minio.getS3URL();
    // Test S3/MinIO operations
  }
}
```

### Kafka

```java
import org.testcontainers.kafka.KafkaContainer;

@Testcontainers
class KafkaIntegrationTest {

  @Container
  static KafkaContainer kafka = new KafkaContainer("confluentinc/cp-kafka:7.5.0")
    .withExposedPorts(9092);

  @Test
  void testKafkaPublishing() {
    String bootstrapServers = kafka.getBootstrapServers();
    // Test Kafka producer/consumer
  }
}
```

---

## Testing Patterns

### End-to-End Rule Execution

```java
@Test
void testCompleteRuleExecution() {
  // 1. Compile rules
  RulesEngineFactory<MyRules> factory = new RulesEngineFactory<>("rules.xlsx", MyRules.class);

  // 2. Create instance
  MyRules rules = factory.newInstance();

  // 3. Execute method
  Result result = rules.executeComplexLogic(input1, input2);

  // 4. Verify result
  assertNotNull(result);
  assertEquals(expectedValue, result.getValue());
  assertTrue(result.isValid());
}
```

### Testing Decision Tables

```java
@Test
void testDecisionTable_allRows_correctResults() {
  // Decision tables often have many rows
  // Test representative scenarios

  testRow(input1, expected1);  // First row
  testRow(input2, expected2);  // Middle row
  testRow(input3, expected3);  // Last row
  testEdgeCases();              // Boundary conditions
}

private void testRow(Input input, Output expected) {
  // Isolated test for single row
}
```

### Testing Type System

```java
@Test
void testDataTypeResolution() {
  IOpenClass ruleType = engine.compile(sourceFile).getCompiledClass();

  // Test type metadata
  assertNotNull(ruleType);
  assertEquals("MyRule", ruleType.getName());

  // Test methods
  IOpenMethod[] methods = ruleType.getMethods();
  assertTrue(methods.length > 0);

  // Test method signature
  IOpenMethod method = findMethod(methods, "myMethod");
  assertNotNull(method);
  assertEquals(Integer.class, method.getReturnType());
}
```

### Testing Error Handling

```java
@Test
void testInvalidRuleHandling() {
  // Invalid Excel structure
  assertThrows(RulesCompilationException.class, () -> {
    new RulesEngineFactory<>(INVALID_EXCEL, MyRules.class);
  });
}

@Test
void testRuntimeError() {
  MyRules rules = factory.newInstance();

  // Method call with invalid input should handle gracefully
  assertThrows(RulesExecutionException.class, () -> {
    rules.divide(10, 0);  // Division by zero
  });
}
```

---

## Performance & Load Testing

### Basic Performance Test

```java
@Test
void testRuleExecutionPerformance() {
  MyRules rules = factory.newInstance();

  // Warm-up
  for (int i = 0; i < 100; i++) {
    rules.calculate(i);
  }

  // Measure
  long start = System.nanoTime();
  for (int i = 0; i < 10_000; i++) {
    rules.calculate(i);
  }
  long duration = System.nanoTime() - start;

  // Assert: Should complete in reasonable time
  long avgNanos = duration / 10_000;
  assertTrue(avgNanos < 100_000, "Avg execution > 100µs");
}
```

### Load Testing with RestAssured

```java
@Test
void testRuleServiceUnderLoad() {
  // Requires running service
  int requests = 100;
  int concurrency = 10;

  // Test with concurrent requests
  ExecutorService executor = Executors.newFixedThreadPool(concurrency);

  for (int i = 0; i < requests; i++) {
    executor.submit(() -> {
      given()
        .body(createRequest())
        .when()
        .post("/api/services/MyService/methods/calculate")
        .then()
        .statusCode(200);
    });
  }

  executor.shutdown();
  assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));
}
```

---

## Async Testing

### Using Awaitility

```java
import org.awaitility.Awaitility;
import static java.util.concurrent.TimeUnit.SECONDS;

@Test
void testAsyncRuleExecution() {
  asyncRuleService.executeAsync(input);

  // Wait for result with timeout
  Awaitility
    .await()
    .atMost(5, SECONDS)
    .pollInterval(100, TimeUnit.MILLISECONDS)
    .until(() -> resultIsReady());

  verifyResult();
}
```

---

## XML & JSON Testing

### XML Comparison

```java
import org.xmlunit.XMLUnit;
import org.xmlunit.matchers.CompareMatcher;

@Test
void testXMLOutput() {
  String actual = rules.getXMLOutput();
  String expected = loadXMLFile("expected.xml");

  XMLUnit.setNormalizeWhitespace(true);
  assertThat(actual, CompareMatcher.isSimilarTo(expected));
}
```

### JSON Assertion

```java
import io.rest-assured.RestAssured;

@Test
void testJSONResponse() {
  given()
    .when()
    .get("/api/rules/info")
    .then()
    .statusCode(200)
    .body("name", equalTo("MyRule"))
    .body("methods.size()", greaterThan(0))
    .body("methods[0].name", notNullValue());
}
```

---

## Test Data Management

### Fixture Files

**Location**: `itest.smoke/src/test/resources/`

```
resources/
├── excel-files/
│   ├── simple-rules.xlsx
│   ├── complex-rules.xlsx
│   ├── invalid-rules.xlsx
│   └── ...
├── data/
│   ├── test-data.json
│   └── test-data.xml
└── config/
    └── test-app.properties
```

### Test Data Builder

```java
public class RuleTestDataBuilder {

  public static RulesEngineFactory<MyRules> createSimpleFactory() {
    return new RulesEngineFactory<>("excel-files/simple-rules.xlsx", MyRules.class);
  }

  public static MyRules createSimpleRules() {
    return createSimpleFactory().newInstance();
  }

  public static ExecutionRequest createRequest(String methodName, Object... args) {
    ExecutionRequest request = new ExecutionRequest();
    request.setMethodName(methodName);
    request.setArguments(args);
    return request;
  }
}

// Usage
@Test
void test() {
  MyRules rules = RuleTestDataBuilder.createSimpleRules();
  ExecutionRequest request = RuleTestDataBuilder.createRequest("calculate", 10, 20);
}
```

---

## Common Test Issues & Solutions

### Issue: Test Hangs

**Causes**: Infinite loop in binding, deadlock, missing timeout

**Solution**:
```java
@Test(timeout = 5000)  // 5 second timeout
void testWithTimeout() {
  // Test code
}

// Or with Junit 5
@Timeout(value = 5, unit = TimeUnit.SECONDS)
void testWithJunit5Timeout() {
  // Test code
}
```

### Issue: Test Passes Locally, Fails in CI

**Causes**: Environment differences, timing issues, missing Docker

**Solution**:
```bash
# Run full test suite locally
mvn verify

# Skip Docker tests (if on Windows/macOS)
mvn verify -DnoDocker

# Check CI logs for environment details
```

### Issue: Flaky Tests

**Causes**: Timing assumptions, resource exhaustion, race conditions

**Solution**:
```java
// Use Awaitility for async operations
Awaitility.await().atMost(5, SECONDS).until(() -> condition());

// Use Thread.sleep sparingly, prefer polling
waitFor(() -> resource.isReady(), 5000);

// Isolate tests (no shared state)
@BeforeEach
void setUp() {
  // Fresh setup for each test
}
```

---

## Performance Considerations

### Memory Management

Tests run with strict memory constraints to detect leaks:

```bash
# Default (strict)
mvn verify

# Relaxed (for development)
mvn verify -DnoPerf
```

### Test Isolation

**Good**:
```java
@Test
void testIndependently() {
  // Complete setup in @BeforeEach
  // No shared state
  // No dependencies on other tests
}
```

**Bad**:
```java
static class SharedResource { }  // Don't share across tests

@Test
void test1() { SharedResource.init(); }
@Test
void test2() { SharedResource.use(); }  // Depends on test1
```

---

## Code Review Checklist for ITEST

Before committing integration tests:

- [ ] Test class has clear `@DisplayName`
- [ ] Test methods follow naming: `test<Feature>_<Scenario>_<Expected>`
- [ ] Comprehensive: Happy path + error cases
- [ ] Setup/teardown proper (@BeforeEach, @AfterEach)
- [ ] No test interdependencies
- [ ] Uses TestContainers for external services
- [ ] Timeout configured (prevents hangs)
- [ ] Clear assertions with meaningful messages
- [ ] Test data properly organized
- [ ] Async tests use Awaitility
- [ ] Performance tests documented
- [ ] Passes on CI (Linux, Windows, macOS)

---

## For More Information

- **Root Project Conventions**: `/AGENTS.md`
- **Testing Requirements**: Root `/AGENTS.md` Testing section
- **TestContainers**: https://www.testcontainers.org/
- **Awaitility**: https://github.com/awaitility/awaitility
- **JUnit 5**: https://junit.org/junit5/
- **Mockito**: https://site.mockito.org/
- **RestAssured**: https://rest-assured.io/

---

**Last Updated**: 2025-12-02
**Version**: 6.0.0-SNAPSHOT
