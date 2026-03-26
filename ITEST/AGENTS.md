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
- **JSON body**: structural comparison with wildcard support (whitespace ignored)
- **XML**: normalized comparison
- **Plain text**: regex-based matching
- **Binary**: byte array comparison

### Wildcard Patterns in Response Files

The `Comparators` class supports wildcard patterns in expected `.resp` files:

- `***` — matches any string (`[^\uFFFF]*` regex)
- `###` — matches digits (`[#\d]+` regex)
- `@@@` — matches word characters (`[@\w]+` regex)

These work in both plain text and JSON responses. In JSON, wildcards are matched per-value — the structural comparison walks expected and actual JSON trees node by node, applying pattern matching only on leaf text values.

### Naming Conventions

- File pairs: same name, different extension (`EPBDS-13489.req` + `EPBDS-13489.resp`)
- Numeric prefixes for ordering: `00-login.req`, `01-get-data.req`
- Group related tests in subdirectories
- Use `000` subdirectory for setup steps if needed
- Use `999` subdirectory to restore state at end of suite

### Debugging Failed Tests

When a test fails, the framework saves the actual response body to `target/responses/` mirroring the `test-resources` directory structure. Each failed request produces a `.req.body` file containing the actual response body.

Compare `target/responses/<path>/<name>.req.body` (actual body) with the corresponding `test-resources/<path>/<name>.resp` (expected response with headers).

### Updating Expected OpenAPI Responses

When REST controllers or OpenAPI annotations change, the large OpenAPI `.resp` files need updating. Key files:

- `itest.webstudio/test-resources-simple/openapi.json.resp`
- `itest.webstudio/test-resources-multi/000-openapi.json.resp`

**Procedure:**

1. Run the failing tests to capture actual responses in `target/responses/`
2. Compare expected vs actual JSON to identify structural differences (missing schemas, paths, tags)
3. Insert **only** the missing entries at correct positions — do not regenerate the entire file (keeps git history clean)
4. These files use wildcard `***` for `description`, `example`, `operationId`, and `summary` values
5. Keys are NOT alphabetically sorted — they follow the server's declaration order. Find insertion points by checking the actual response key order.

**Pitfalls:**

- Files use CRLF — read/write as binary with `\r\n` splits
- JSON keys like `/projects/{projectId}` contain literal `{}` — when counting braces to find block boundaries, skip characters inside quoted strings
- Verify the resulting JSON is valid before running tests
