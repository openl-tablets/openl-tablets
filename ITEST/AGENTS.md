# ITEST Module - Claude Code Conventions

**Module**: OpenL Tablets ITEST (Integration Tests)
**Version**: 6.0.0-SNAPSHOT
**Last Updated**: 2026-01-08

This file provides specific guidance for integration testing in the OpenL Tablets project. For general project conventions, see the root `/AGENTS.md`.

---

## Module Overview

ITEST contains comprehensive integration tests for OpenL Tablets.
It tests end-to-end workflows for Web applications (STUDIO, WSFrontend) using real services, databases, and Docker containers.

**Key Characteristics**:
- **Multiple test suites**: Smoke tests, security, WebStudio E2E, Kafka, MinIO, etc.
- **Docker-based**: Uses TestContainers for PostgreSQL, Keycloak, MinIO, Kafka
- **Slow but thorough**: Full system testing, not unit tests
- **CI/CD integration**: Runs on schedule and in pull requests

---

## Module Structure

```
ITEST/
├── server-core/                         # Shared test utilities, a base framework
│   ├── src/org/openl/itest/core/        # Core test classes
│   │   ├── HttpClient.java              # HTTP request/response test framework
│   │   └──JettyServer.java              # Embedded Jetty server for tests
│   └── pom.xml
│
├── itest.WebService/                  # OpenL RuleServices tests (main)
│   ├── openl-repository/              # Location of test config and deployments
│   │   ├── application.properties     # A test config
│   │   ├── application-X.properties   # A test config for 'X' profile
│   │   └── deployments/               # Typical folder for deployed OpenL projects defined in the given test config
│   │       ├── deployment1/
│   │       │   ├── project1/          # OpenL project files
│   │       │   └── project2/          # OpenL project files
│   │       └── deployment2/
│   │           └── ...
│   ├── test/                          # Java JUnit test classes
│   │   ├── RuleExecutionTest.java
│   │   └── ...
│   ├── test-resources/                # HTTP request/response files for tests
│   ├── test-resources-X/              # Additional test resources for profile 'X'
│   ├── target/responses/              # Generated actual responses for failed tests
│   └── pom.xml
│
├── itest.smoke/                       # Base minimal set of OpenL RuleServices tests
├── itest.security/                    # Authentication/authorization tests
├── itest.webstudio/                   # WebStudio E2E tests
├── itest.kafka.smoke/                 # Kafka integration tests
├── itest.minio/                       # S3 storage tests
├── itest.tracing/                     # OpenTelemetry tests
├── itest.datasources/                 # Database integration tests
├── ... (more test modules)
│
└── pom.xml                            # Root ITEST POM
```

---

## Request/Response File Testing (*.req and *.resp)

OpenL Tablets ITEST uses a unique declarative testing approach: HTTP request/response files. This is the **primary testing mechanism** for WebService integration tests.

### Overview

Instead of writing Java test code, you create `.req` (request) and `.resp` (response) files that define HTTP exchanges.
The format of `.req` and `.resp` files is simple and human-readable, similar to raw HTTP messages and strictly follows to the RFC 7231 with some enhancements.
The test framework automatically:
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
- Prefix with ticket ID or meaningful tested functionality: `EPBDS-13489.req`
- Group related tests in subdirectories
- Use numeric prefixes for test ordering: `00.req`, `01.req`, `02.req`, etc.
- Use descriptive names for clarity: `00-login.req`, `01-get-data.req`, etc.

**⚠️ CRITICAL - Line Endings (CRLF Required) **:

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

Will replace `${USER_ID}` with variable value from the HttpClient.localEnv Map.

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
- ✅ Restore state in the end of the suite test (999 subdirectory for reverting changes)

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
# Compare target/responses/EPBDS-XXXXX/*.resp with test-resources/EPBDS-XXXXX/*.resp
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

---

## For More Information

- **Root Project Conventions**: `/AGENTS.md`
- **Testing Requirements**: Root `/AGENTS.md` Testing section
- **TestContainers**: https://www.testcontainers.org/
- **Awaitility**: https://github.com/awaitility/awaitility
- **JUnit 5**: https://junit.org/junit5/
- **Mockito**: https://site.mockito.org/
