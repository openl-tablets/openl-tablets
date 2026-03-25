# ITEST Module — Integration Tests

End-to-end tests for OpenL Rule Services and OpenL Studio using Docker, TestContainers, and a declarative HTTP req/resp framework.

## Test Modules

- **server-core/** — Shared framework: `HttpClient`, `JettyServer`, `HttpData`, `Comparators`
- **itest.smoke** — Base minimal Rule Services tests
- **itest.WebService** — Main Rule Services test suite (largest)
- **itest.security** — Authentication/authorization tests
- **itest.webstudio** — OpenL Studio E2E tests
- **itest.kafka.smoke** — Kafka integration
- **itest.s3** — S3 storage
- **itest.tracing** — OpenTelemetry
- **itest.storelogdata** — Audit logging
- **itest.healthchecks** — Health check endpoints
- **itest.spring-boot** — Spring Boot integration
- **itest.deployment-filter** — Deployment filtering
- **itest.BigServiceDeploy** — Large service deployment
- **itest.ruleServices-large-response** — Large response handling
- **itest.local-zip-repository** — ZIP repository tests
- **itest.unpackClasspathJar** / **itest.unpackClasspathZip** — Classpath unpacking
- **itest.ws-rest-rules-deploy** — REST rules deployment

## Declarative HTTP Testing (*.req / *.resp)

The **primary testing mechanism**. Instead of Java test code, define HTTP exchanges as file pairs.

### Structure

```
itest.module/                      # OpenL integration tests (main)
├── openl-repository/              # Location of test config and deployments
│   ├── application.properties     # A test config
│   ├── application-X.properties   # A test config for 'X' profile
│   └── deployments/               # Typical folder for deployed OpenL projects defined in the given test config
│       ├── deployment1/
│       │   ├── project1/          # OpenL project files
│       │   └── project2/          # OpenL project files
│       └── deployment2/
│           └── ...
├── test/                          # Java JUnit test classes
│   ├── RuleExecutionTest.java
│   └── ...
├── test-resources/                # HTTP request/response files for tests
│   ├── EPBDS-10027.req            # Standalone test
│   ├── EPBDS-10027.resp
│   └── EPBDS-13489/
│       ├── 00.req                 # Grouped tests (alphabetical order)
│       ├── 00.resp
│       ├── 01.req
│       └── 01.resp
├── test-resources-X/              # Additional test resources for profile 'X'
├── target/responses/              # Generated actual responses for failed tests
└── pom.xml
```

### CRITICAL: Line Endings

**Files MUST use CRLF (`\r\n`), not LF.** This is enforced in `.gitattributes`. Files with LF-only will **silently fail to parse**.

### Request File Format (*.req)

```http
METHOD /url/path HTTP/1.1
Header-Name: value

[optional body]
```

### Response File Format (*.resp)

```http
HTTP/1.1 STATUS_CODE
Header-Name: value

[optional body]
```

### Defining Request/Response Bodies in the separate file

```http
METHOD /url/path HTTP/1.1
Header-Name: value

&filename.ext
```

### Special Headers

- `X-OpenL-Test-Retry: yes` — retry for up to 2 minutes on mismatch (100ms delays)
- `X-OpenL-Test-Timeout: 30000` — custom timeout in ms

### Environment Variables

Use `${VAR_NAME}` in req/resp files — replaced from `HttpClient.localEnv` map.

### Cookie/Session Handling

Cookies persist **within each subdirectory** and reset when entering a new subdirectory.

### Response Comparison

- **Status code**: exact match
- **Headers**: case-insensitive (Content-Type, etc.)
- **JSON body**: flexible matching (whitespace ignored)
- **XML**: normalized comparison
- **Plain text**: exact match
- **Binary**: byte array comparison

### Naming Conventions

- File pairs: same name, different extension (`EPBDS-13489.req` + `EPBDS-13489.resp`)
- Numeric prefixes for ordering: `00-login.req`, `01-get-data.req`
- Group related tests in subdirectories
- Use `000` subdirectory for setup steps if needed
- Use `999` subdirectory to restore state at end of suite

### Debugging Failed Tests

Compare `target/responses/EPBDS-XXXXX/*.resp` (actual) with `test-resources/EPBDS-XXXXX/*.resp` (expected).
