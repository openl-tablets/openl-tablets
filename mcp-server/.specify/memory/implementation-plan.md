# OpenL Tablets MCP Server - Implementation Plan

## Technology Stack

### Core Technologies

**Runtime**: Node.js 18.0.0+
- Rationale: LTS version with ESM support, widespread adoption
- Features used: ES Modules, async/await, modern APIs

**Language**: TypeScript 5.7.2
- Rationale: Type safety, excellent IDE support, industry standard
- Configuration: Strict mode enabled, ES2022 target
- Module system: Node16 (native ESM)

**Protocol**: Model Context Protocol (MCP) SDK 1.21.1
- Rationale: Official SDK, latest stable version
- Features: Tools, resources, prompts, type-safe validation

### Dependencies

**Production Dependencies** (5):

1. **@modelcontextprotocol/sdk** (^1.21.1)
   - Purpose: MCP protocol implementation
   - Features: Server, tools, resources, prompts
   - Size: ~500KB

2. **axios** (^1.7.9)
   - Purpose: HTTP client for OpenL API
   - Features: Interceptors, timeout, connection pooling
   - Alternatives considered: fetch (lacks interceptors), node-fetch (no TypeScript)

3. **form-data** (^4.0.1)
   - Purpose: Multipart file uploads
   - Features: Stream support, proper content-type headers
   - Usage: Excel file uploads

4. **zod** (^3.23.8)
   - Purpose: Runtime schema validation
   - Features: Type inference, composable, excellent errors
   - Alternatives considered: Joi (no type inference), Yup (less TypeScript-friendly)

5. **zod-to-json-schema** (^3.23.5)
   - Purpose: Convert Zod schemas to JSON Schema for MCP
   - Features: Automatic conversion, maintains metadata
   - Integration: Seamless with Zod

**Development Dependencies** (11):
- TypeScript tooling: `typescript`, `ts-node`, `ts-jest`
- Testing: `jest`, `@types/jest`, `nock`
- Linting: `eslint`, `@typescript-eslint/*`
- Type definitions: `@types/node`, `@types/js-yaml`

**Rationale for Minimal Dependencies**:
- Reduced attack surface
- Faster npm install
- Easier maintenance
- Lower bundle size

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────┐
│         AI Coding Agent (Claude, etc.)          │
└───────────────────┬─────────────────────────────┘
                    │ MCP Protocol (stdio)
┌───────────────────▼─────────────────────────────┐
│            MCP Server (index.ts)                │
│  ┌──────────────────────────────────────────┐   │
│  │  Tool Handlers (24 tools)                │   │
│  ├──────────────────────────────────────────┤   │
│  │  Resource Providers (3 resources)        │   │
│  ├──────────────────────────────────────────┤   │
│  │  Prompt Registry (11 prompts)            │   │
│  └──────────────────────────────────────────┘   │
└───────────────────┬─────────────────────────────┘
                    │ Uses
┌───────────────────▼─────────────────────────────┐
│         OpenL Tablets API Client                │
│  ┌──────────────────────────────────────────┐   │
│  │  Authentication Manager                  │   │
│  │  - Basic Auth, API Key, OAuth 2.1        │   │
│  ├──────────────────────────────────────────┤   │
│  │  API Methods (30+ methods)               │   │
│  │  - Repository, Project, Rules, Deploy    │   │
│  ├──────────────────────────────────────────┤   │
│  │  Utilities (validation, sanitization)    │   │
│  └──────────────────────────────────────────┘   │
└───────────────────┬─────────────────────────────┘
                    │ HTTP/REST
┌───────────────────▼─────────────────────────────┐
│      OpenL Tablets WebStudio REST API           │
│      (http://host:port/webstudio/rest)          │
└─────────────────────────────────────────────────┘
```

### Module Structure

**Layer 1: Entry Point**
- `src/index.ts` (766 lines)
  - MCP server initialization
  - Tool request routing
  - Resource providers
  - Prompt integration
  - Error handling

**Layer 2: Business Logic**
- `src/client.ts` (1,123 lines)
  - OpenL API client
  - 30+ API methods
  - Project ID parsing
  - URL building
  - Response parsing

**Layer 3: Cross-Cutting Concerns**
- `src/auth.ts` (232 lines)
  - Authentication lifecycle
  - Token management
  - Request interceptors
  - Multi-method support

- `src/prompts-registry.ts` (348 lines)
  - Prompt loading and caching
  - YAML frontmatter parsing
  - Argument substitution
  - Template rendering

- `src/utils.ts` (209 lines)
  - Error sanitization
  - Timeout validation
  - Project ID parsing
  - Safe JSON serialization

**Layer 4: Definitions**
- `src/tools.ts` (430 lines)
  - Tool definitions (24 tools)
  - Metadata and categorization
  - Helper functions

- `src/schemas.ts` (239 lines)
  - Zod schemas (15 schemas)
  - Input validation
  - Type inference

- `src/types.ts` (643 lines)
  - TypeScript interfaces (40+ types)
  - OpenL API types
  - Configuration types
  - Result types

- `src/constants.ts` (71 lines)
  - Default values
  - Categories
  - HTTP headers
  - Regex patterns

### Data Flow

**Tool Execution Flow**:
```
1. AI Agent → MCP Request (tool name + args)
2. MCP Server → Validate args with Zod schema
3. MCP Server → Route to tool handler
4. Tool Handler → Call client method
5. Client → Add authentication (interceptor)
6. Client → Make HTTP request to OpenL API
7. OpenL API → Process request
8. OpenL API → Return response
9. Client → Parse response
10. Tool Handler → Format MCP response
11. MCP Server → Return to AI agent
```

**Error Flow**:
```
1. Error occurs (any layer)
2. Catch with try/catch
3. Sanitize error message (redact credentials)
4. Wrap in McpError with context
5. Return to AI agent with error details
```

**Authentication Flow (OAuth)**:
```
1. Request needs authentication
2. Check if token cached and valid
3. If not, acquire new token:
   a. POST to token URL with client credentials
   b. Parse response for access_token
   c. Cache token with expiration
4. Add token to Authorization header
5. Proceed with request
```

## Implementation Details

### Authentication System

**Design**: Pluggable authentication via interceptors

**Supported Methods**:

1. **Basic Authentication**:
   ```typescript
   config: { username: "admin", password: "admin" }
   → Header: "Authorization: Basic YWRtaW46YWRtaW4="
   ```

2. **API Key**:
   ```typescript
   config: { apiKey: "sk-..." }
   → Header: "Authorization: Bearer sk-..."
   ```

3. **OAuth 2.1**:
   ```typescript
   config: {
     oauth: {
       tokenUrl: "https://auth.example.com/token",
       clientId: "client-id",
       clientSecret: "secret",
       grantType: "client_credentials",
       scope: "openl:read openl:write"
     }
   }
   → Fetch token → Cache → Header: "Authorization: Bearer eyJ..."
   ```

**Token Caching**:
- Tokens cached in memory with expiration
- Automatic refresh before expiration
- Concurrent request deduplication
- No persistent storage (security)

**Interceptors**:
- Request interceptor adds auth headers
- Response interceptor handles 401/403
- Error interceptor sanitizes messages

### Input Validation System

**Design**: Zod schemas for runtime validation + TypeScript types

**Schema Pattern**:
```typescript
export const listProjectsSchema = z.object({
  repository: z.string().optional().describe("Filter by repository"),
  status: z.enum(["OPENED", "CLOSED"]).optional(),
  tag: z.string().optional(),
});

// Automatic type inference
type ListProjectsInput = z.infer<typeof listProjectsSchema>;
```

**JSON Schema Conversion**:
```typescript
const inputSchema = zodToJsonSchema(listProjectsSchema);
// → Used in MCP tool definition
```

**Benefits**:
- Runtime validation catches bad inputs
- TypeScript types ensure compile-time safety
- Single source of truth (Zod schema)
- Excellent error messages

### Error Handling System

**Design**: Centralized sanitization + contextual wrapping

**Sanitization** (`sanitizeError()`):
```typescript
function sanitizeError(error: unknown): string {
  const message = extractMessage(error);
  return message
    .replace(/Bearer\s+[\w-]+\.[\w-]+\.[\w-]+/g, "Bearer [REDACTED]")
    .replace(/apiKey[=:]\s*[\w-]+/gi, "apiKey=[REDACTED]")
    .replace(/client_secret[=:]\s*[\w-]+/gi, "client_secret=[REDACTED]");
}
```

**Error Wrapping**:
```typescript
try {
  const result = await this.client.someMethod();
} catch (error: unknown) {
  if (axios.isAxiosError(error)) {
    throw new McpError(
      ErrorCode.InternalError,
      `API error (${error.response?.status}): ${sanitizeError(error)}`,
      {
        endpoint: error.config?.url,
        method: error.config?.method,
        status: error.response?.status,
      }
    );
  }
  throw error; // Re-throw non-Axios errors
}
```

**Benefits**:
- No credentials leak in logs/errors
- Rich context for debugging
- Type-safe error handling
- Consistent error format

### Prompt System

**Design**: YAML frontmatter + template rendering

**File Format**:
```markdown
---
name: create_rule
description: Guide for creating OpenL tables
arguments:
  - name: tableName
    description: Name of the table being created
    required: false
---

# Creating Rules in OpenL Tablets

{if tableName}
## Creating Table: **{tableName}**
{end if}

Content with {variable} placeholders...
```

**Rendering**:
1. Load prompt file
2. Parse YAML frontmatter (metadata)
3. Extract content (Markdown)
4. Substitute arguments: `{tableName}` → actual value
5. Evaluate conditionals: `{if tableName}...{end if}`
6. Return rendered text

**Caching**:
- Prompts loaded once at startup
- Cached in memory
- No file I/O on each request

### Resource System

**Design**: MCP resources expose OpenL data

**Resources**:
1. `openl://repositories` - List of repositories
2. `openl://projects` - List of projects
3. `openl://deployments` - List of deployments

**Dynamic Content**:
- Resources fetch fresh data on each access
- No caching (always up-to-date)
- Error handling for unavailable data

**Use Case**: Allow AI agents to browse available resources without executing tools.

### Project ID Handling

**Challenge**: OpenL 6.0.0+ uses base64-encoded project IDs in URLs

**Solution**: Accept all three formats, convert as needed

**Formats Supported**:
1. **Dash format**: `"design-Example 1 - Bank Rating"` (user-friendly)
2. **Colon format**: `"design:Example 1 - Bank Rating"` (decoded)
3. **Base64 format**: `"ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n"` (OpenL API)

**Conversion**:
```typescript
parseProjectId(projectId: string): [string, string] {
  // Try colon format
  if (projectId.includes(':')) {
    const [repo, name] = projectId.split(':', 2);
    return [repo, name];
  }
  // Try dash format
  const match = projectId.match(/^([^-]+)-(.+)$/);
  if (match) return [match[1], match[2]];
  // Try base64
  const decoded = Buffer.from(projectId, 'base64').toString('utf-8');
  const [repo, name] = decoded.split(':', 2);
  return [repo, name];
}

toBase64ProjectId(projectId: string): string {
  const [repo, name] = this.parseProjectId(projectId);
  const colonFormat = `${repo}:${name}`;
  return Buffer.from(colonFormat, 'utf-8').toString('base64');
}
```

**Benefits**:
- Backward compatibility
- User-friendly IDs in list_projects
- Automatic conversion for API calls

## Build and Deployment

### Build Process

**TypeScript Compilation**:
```bash
npm run build
# → tsc
# → Outputs to dist/ directory
# → Generates .d.ts type definitions
# → Creates source maps
```

**Build Configuration** (`tsconfig.json`):
- Target: ES2022
- Module: Node16 (native ESM)
- Module resolution: Node16
- Strict: true (all strict checks enabled)
- Output: dist/
- Source maps: true

**Build Output**:
```
dist/
├── index.js          # Main entry point
├── index.d.ts        # Type definitions
├── client.js         # API client
├── client.d.ts
├── auth.js
├── auth.d.ts
└── ... (all modules)
```

### Package Configuration

**package.json**:
```json
{
  "name": "openl-tablets-mcp-server",
  "version": "1.0.0",
  "type": "module",  // ES Modules
  "main": "dist/index.js",
  "bin": {
    "openl-tablets-mcp": "dist/index.js"
  },
  "engines": {
    "node": ">=18.0.0"
  }
}
```

**Entry Point** (`dist/index.js`):
- Shebang: `#!/usr/bin/env node`
- Executable permission
- Reads config from environment or args
- Starts MCP server on stdio

### Deployment Options

**1. npm Package**:
```bash
npm install -g openl-tablets-mcp-server
openl-tablets-mcp
```

**2. Docker Container**:
```dockerfile
FROM node:18-alpine
WORKDIR /app
COPY package*.json ./
RUN npm ci --production
COPY dist/ ./dist/
CMD ["node", "dist/index.js"]
```

**3. Claude Desktop Integration**:
```json
{
  "mcpServers": {
    "openl-tablets": {
      "command": "node",
      "args": ["/path/to/dist/index.js"],
      "env": {
        "OPENL_BASE_URL": "http://localhost:8080/webstudio/rest",
        "OPENL_USERNAME": "admin",
        "OPENL_PASSWORD": "admin"
      }
    }
  }
}
```

**4. Standalone Script**:
```bash
# Build
npm run build

# Run directly
node dist/index.js
```

### Configuration

**Environment Variables**:
```bash
# Required
OPENL_BASE_URL=http://localhost:8080/webstudio/rest

# Authentication (choose one)
OPENL_USERNAME=admin
OPENL_PASSWORD=admin
# OR
OPENL_API_KEY=sk-...
# OR
OPENL_OAUTH_TOKEN_URL=https://auth.example.com/token
OPENL_OAUTH_CLIENT_ID=client-id
OPENL_OAUTH_CLIENT_SECRET=secret
OPENL_OAUTH_SCOPE="openl:read openl:write"

# Optional
OPENL_TIMEOUT=30000  # milliseconds
```

**Configuration Validation**:
- Base URL format checked
- At least one auth method required
- Complete OAuth config enforced
- Timeout range validated

### Testing

**Test Structure**:
```
tests/
├── mocks/
│   └── openl-api-mocks.ts      # Mock API responses
├── openl-client.test.ts         # Unit tests for client
├── mcp-server.test.ts           # Integration tests for server
├── prompts.test.ts              # Prompt rendering tests
└── integration/
    └── openl-live.test.ts       # Live integration tests
```

**Test Execution**:
```bash
# All tests
npm test

# With coverage
npm run test:coverage

# Watch mode
npm run test:watch

# Live tests (requires OpenL server)
SKIP_LIVE_TESTS=false npm test
```

**Jest Configuration**:
- Preset: ts-jest
- Test environment: node
- Module: ESM (experimental VM modules)
- Coverage: Istanbul
- Transform: TypeScript to JavaScript

### Code Quality

**ESLint**:
```bash
npm run lint       # Check
npm run lint:fix   # Fix automatically
```

**Configuration**:
- Extends: @typescript-eslint/recommended
- Parser: @typescript-eslint/parser
- Rules: Strict TypeScript rules
- Target: 0 errors, 0 warnings

**TypeScript**:
- Strict mode enabled
- No implicit any
- Unused variables/imports checked
- Proper null handling

## Performance Optimizations

### 1. Token Caching
- OAuth tokens cached in memory
- Expiration-based invalidation
- Prevents redundant token requests
- Thread-safe concurrent access

### 2. Connection Pooling
- Axios default connection pooling
- Reuses TCP connections
- Reduces handshake overhead
- Configurable pool size

### 3. Lazy Initialization
- Client created only when first tool called
- Tokens acquired only when needed
- Prompts loaded at startup (cached)
- No unnecessary work on startup

### 4. Request Deduplication
- Concurrent token requests deduplicated
- Single token fetch for multiple requests
- Promise caching pattern

### 5. Efficient JSON Serialization
- Circular reference protection
- Safe stringify with fallback
- No unnecessary stringification

## Security Measures

### 1. Credential Sanitization
- All errors sanitized
- Regex patterns for tokens, secrets
- Applied at error boundary
- No credentials in logs

### 2. Input Validation
- All inputs validated with Zod
- Type checking at runtime
- Early rejection of bad inputs
- Detailed validation errors

### 3. URL Validation
- Base URL format checked
- Token URL format checked
- No arbitrary URLs accepted
- Protocol whitelisting

### 4. Timeout Protection
- All requests have timeouts
- Prevents hung connections
- Configurable limits
- Default: 30 seconds

### 5. Dependency Security
- Minimal dependencies (5 prod, 11 dev)
- Regular npm audit
- Dependabot updates
- Version pinning

## Monitoring and Debugging

### Logging Strategy

**Current**: No built-in logging (MCP is stdio-based)

**Debug Approach**:
- Error messages include context
- HTTP status codes included
- Endpoint and method logged in errors
- Tool name included in errors

**Future**: Optional debug mode with file logging

### Error Tracking

**Error Categories**:
- Network errors (axios errors)
- Authentication errors (401/403)
- Validation errors (400)
- Not found (404)
- Server errors (500/503)

**Error Context**:
- HTTP status code
- API endpoint
- HTTP method
- Tool name
- Sanitized message

### Health Monitoring

**Health Check Tool**:
- Connectivity test
- Authentication verification
- Response time measurement
- Status reporting

**Usage**:
```
Tool: health_check
→ Attempts to list repositories
→ Returns: { status, baseUrl, authMethod, timestamp, serverReachable }
```

## Future Architecture Considerations

### Potential Enhancements

1. **Streaming Support**:
   - Large file downloads
   - Real-time log streaming
   - Progress reporting

2. **Caching Layer**:
   - Redis for distributed caching
   - Project metadata caching
   - Table structure caching
   - Configurable TTL

3. **Batch Operations**:
   - Multiple tool execution
   - Transaction support
   - Rollback on failure

4. **WebSocket Support**:
   - Real-time updates
   - Event subscriptions
   - Push notifications

5. **Metrics and Telemetry**:
   - Performance metrics
   - Usage analytics
   - Error tracking
   - OpenTelemetry integration

---

*Last Updated: 2025-11-13*
*Version: 1.0.0*
*Status: Implemented*
