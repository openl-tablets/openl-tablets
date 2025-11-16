# OpenL Tablets MCP Server - Implementation Plan

> **Status**: Refactored & Production-Ready (v2.0.0)
> **Last Updated**: 2025-11-16
> **Major Milestone**: Completed architectural refactoring from monolithic to modular design

## Executive Summary

The OpenL Tablets MCP Server underwent a **major architectural refactoring** in November 2025, transforming from a monolithic 766-line `index.ts` file to a clean, modular architecture with dedicated modules for tool handling, formatting, validation, and logging.

**Key Improvements**:
- ✅ **RegisterTool Pattern**: Replaced 400+ line switch statement with registry-based tool management
- ✅ **Modular Architecture**: Split into 14 focused modules (~5,400 lines total)
- ✅ **Enhanced Validation**: All schemas now use `.strict()` mode for runtime safety
- ✅ **Response Formatting**: Unified JSON/Markdown formatting with pagination support
- ✅ **Structured Logging**: stderr-only logging with credential sanitization
- ✅ **MCP Annotations**: First-class support for readOnlyHint, idempotentHint, destructiveHint, openWorldHint
- ✅ **Tool Prefix**: All 24 tools now use `openl_` prefix for namespacing
- ✅ **Character Limits**: Automatic truncation at 100,000 characters
- ✅ **Pagination**: Built-in pagination (limit: 50, max: 200) for all list operations

**New Modules**:
- `tool-handlers.ts` (998 lines) - Tool registry and handlers
- `formatters.ts` (360 lines) - Response formatting and pagination
- `validators.ts` (117 lines) - Input validation utilities
- `logger.ts` (107 lines) - Structured logging

**Code Reduction**: index.ts reduced from 766 lines to 352 lines (54% reduction)

---

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

The MCP server has been **refactored from a 766-line monolithic index.ts to a modular architecture** with dedicated modules for tool handling, formatting, validation, and logging.

**Layer 1: Entry Point**
- `src/index.ts` (352 lines, **REDUCED from 766**)
  - MCP server initialization
  - Request routing (delegated to tool-handlers)
  - Resource providers
  - Prompt integration
  - High-level error handling

**Layer 2: Tool Management** ⭐ **NEW ARCHITECTURE**
- `src/tool-handlers.ts` (998 lines, **NEW**)
  - RegisterTool pattern (replaces switch statements)
  - Tool registry and execution
  - 24 tool registration functions
  - MCP annotations (readOnlyHint, idempotentHint, destructiveHint, openWorldHint)
  - Centralized tool error handling
  - Handler type definitions

- `src/tools.ts` (370 lines)
  - Tool metadata definitions
  - Tool categorization
  - Helper functions for tool discovery

**Layer 3: Business Logic**
- `src/client.ts` (1,150 lines)
  - OpenL API client
  - 30+ API methods
  - Project ID parsing and conversion
  - URL building
  - Response parsing

**Layer 4: Formatting & Validation** ⭐ **NEW**
- `src/formatters.ts` (360 lines, **NEW**)
  - Response formatting (JSON/Markdown)
  - Pagination helpers (paginateResults)
  - Character limit enforcement
  - Truncation handling
  - Markdown table generation

- `src/validators.ts` (117 lines, **NEW**)
  - Input validation utilities
  - Base64 validation
  - Response format validation
  - Pagination parameter validation
  - Project ID validation

- `src/logger.ts` (107 lines, **NEW**)
  - Structured logging to stderr
  - Log levels (ERROR, WARN, INFO, DEBUG)
  - Context sanitization
  - Credential redaction

**Layer 5: Cross-Cutting Concerns**
- `src/auth.ts` (232 lines)
  - Authentication lifecycle
  - Token management
  - Request interceptors
  - Multi-method support (Basic, API Key, OAuth 2.1)

- `src/prompts-registry.ts` (365 lines)
  - Prompt loading and caching
  - YAML frontmatter parsing
  - Argument substitution
  - Template rendering

- `src/prompts.ts` (171 lines)
  - Prompt definitions
  - Prompt metadata

- `src/utils.ts` (209 lines)
  - Error sanitization
  - Timeout validation
  - Project ID parsing
  - Safe JSON serialization

**Layer 6: Definitions**
- `src/schemas.ts` (270 lines, **ENHANCED**)
  - Zod schemas with .strict() mode
  - Input validation (24 tool schemas)
  - Type inference
  - Runtime safety

- `src/types.ts` (653 lines)
  - TypeScript interfaces (40+ types)
  - OpenL API types
  - Configuration types
  - Result types

- `src/constants.ts` (82 lines, **ENHANCED**)
  - Default values
  - Categories
  - HTTP headers
  - Regex patterns
  - **RESPONSE_LIMITS** (MAX_CHARACTERS, MAX_ARRAY_ITEMS)

### Data Flow

**Tool Execution Flow** (Refactored):
```
1. AI Agent → MCP Request (tool name + args)
2. index.ts → CallToolRequest handler receives request
3. index.ts → Delegates to executeTool() in tool-handlers.ts
4. tool-handlers.ts → Looks up tool in registry
5. tool-handlers.ts → Validates args with Zod schema (.strict())
6. tool-handlers.ts → Calls tool handler function
7. Tool Handler → Validates input (validators.ts)
8. Tool Handler → Calls client method (client.ts)
9. Client → Add authentication (auth.ts interceptor)
10. Client → Make HTTP request to OpenL API
11. OpenL API → Process request
12. OpenL API → Return response
13. Client → Parse response
14. Tool Handler → Apply pagination (paginateResults)
15. Tool Handler → Format response (formatResponse)
16. Tool Handler → Return formatted result
17. index.ts → Return to AI agent
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

**Design**: Zod schemas with .strict() mode for runtime validation + TypeScript types

**Schema Pattern** (Enhanced):
```typescript
export const listProjectsSchema = z.object({
  repository: z.string().optional().describe("Filter by repository"),
  status: z.enum(["OPENED", "CLOSED"]).optional(),
  tag: z.string().optional(),
  response_format: ResponseFormat.optional(),
  limit: z.number().int().positive().max(200).default(50).optional(),
  offset: z.number().int().nonnegative().default(0).optional(),
}).strict();  // ⭐ NEW: Reject unknown properties

// Automatic type inference
type ListProjectsInput = z.infer<typeof listProjectsSchema>;
```

**JSON Schema Conversion**:
```typescript
const inputSchema = zodToJsonSchema(listProjectsSchema);
// → Used in MCP tool definition
```

**Benefits**:
- **.strict() mode** prevents extra/unknown properties
- Runtime validation catches bad inputs
- TypeScript types ensure compile-time safety
- Single source of truth (Zod schema)
- Excellent error messages
- Centralized validation utilities (validators.ts)

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

### RegisterTool Pattern ⭐ **NEW ARCHITECTURE**

**Design**: Registry-based tool management (replaces switch statements in index.ts)

**Previous Approach** (Deprecated):
```typescript
// OLD: 400+ line switch statement in index.ts
switch (name) {
  case "list_repositories":
    // handler code here (50+ lines)
  case "list_projects":
    // handler code here (50+ lines)
  // ... 22 more cases
}
```

**New Approach** (Current):
```typescript
// tool-handlers.ts
registerTool({
  name: "openl_list_repositories",
  title: "List Repositories",
  description: "List all design repositories...",
  inputSchema: zodToJsonSchema(schemas.listRepositoriesSchema) as Record<string, unknown>,
  annotations: {
    readOnlyHint: true,
    openWorldHint: true,
    idempotentHint: true,
  },
  handler: async (args, client): Promise<ToolResponse> => {
    // Validation
    const format = validateResponseFormat(args?.response_format);
    const { limit, offset } = validatePagination(args?.limit, args?.offset);

    // API call
    const repositories = await client.listRepositories();

    // Pagination
    const paginated = paginateResults(repositories, limit, offset);

    // Formatting
    const formatted = formatResponse(paginated.data, format, {
      pagination: { limit, offset, total: paginated.total_count },
      dataType: "repositories",
    });

    return { content: [{ type: "text", text: formatted }] };
  },
});
```

**Tool Registry**:
```typescript
// Map of tool name → tool definition
const toolHandlers = new Map<string, ToolDefinition>();

// Register all tools at startup
export function registerAllTools(server: Server, client: OpenLClient): void {
  registerTool({ /* tool 1 */ });
  registerTool({ /* tool 2 */ });
  // ... 24 tools total
}

// Execute tool by name
export async function executeTool(name: string, args: unknown, client: OpenLClient) {
  const tool = toolHandlers.get(name);
  if (!tool) throw new McpError(ErrorCode.MethodNotFound, `Unknown tool: ${name}`);
  return await tool.handler(args, client);
}
```

**Benefits**:
- **Modularity**: Each tool is self-contained
- **Maintainability**: Add/remove tools without touching index.ts
- **Testability**: Test individual tools in isolation
- **Type Safety**: Dedicated handler types for each tool
- **MCP Annotations**: First-class support for tool metadata
- **Cleaner Code**: index.ts reduced from 766 to 352 lines

**Adding a New Tool** (4-Step Process):

**Step 1**: Define schema in `schemas.ts`
```typescript
export const myNewToolSchema = z.object({
  param1: z.string().describe("Description"),
  param2: z.number().optional(),
  response_format: ResponseFormat.optional(),
  limit: z.number().int().positive().max(200).default(50).optional(),
  offset: z.number().int().nonnegative().default(0).optional(),
}).strict();  // Always use .strict()
```

**Step 2**: Create registration function in `tool-handlers.ts`
```typescript
registerTool({
  name: "openl_my_new_tool",  // Always use openl_ prefix
  title: "My New Tool",
  description: "Detailed description for AI agents",
  inputSchema: zodToJsonSchema(schemas.myNewToolSchema) as Record<string, unknown>,
  annotations: {
    readOnlyHint: true,  // Does not modify data
    idempotentHint: true,  // Safe to retry
    // destructiveHint: true,  // Uncomment if destructive
    // openWorldHint: true,  // Uncomment if returns dynamic data
  },
  handler: async (args, client): Promise<ToolResponse> => {
    // 1. Type cast and validate
    const typedArgs = args as z.infer<typeof schemas.myNewToolSchema>;
    const format = validateResponseFormat(typedArgs.response_format);

    // 2. Call client method
    const data = await client.myNewMethod(typedArgs.param1);

    // 3. Apply pagination if needed
    const paginated = paginateResults(data, typedArgs.limit, typedArgs.offset);

    // 4. Format response
    const formatted = formatResponse(paginated.data, format);

    return { content: [{ type: "text", text: formatted }] };
  },
});
```

**Step 3**: Register in `registerAllTools()`
```typescript
// Already done if you used registerTool() in Step 2
// The function is called automatically at server startup
```

**Step 4**: Add metadata to `tools.ts` (optional, for categorization)
```typescript
export const MY_NEW_TOOL: ToolMetadata = {
  name: "openl_my_new_tool",
  title: "My New Tool",
  category: ToolCategory.RULES,
  description: "Short description",
};
```

### Response Formatting System ⭐ **NEW**

**Design**: Unified formatting with JSON/Markdown support (`formatters.ts`)

**Format Response**:
```typescript
export function formatResponse(
  data: unknown,
  format: "json" | "markdown" = "json",
  options?: FormatOptions
): string {
  // Apply character limit (defaults to 100,000 characters)
  const formatted = format === "json"
    ? formatAsJson(data, options)
    : formatAsMarkdown(data, options);

  return enforceCharacterLimit(formatted, options?.characterLimit);
}
```

**JSON Formatting**:
```typescript
function formatAsJson(data: unknown, options?: FormatOptions): string {
  const result: PaginatedResponse<unknown> = { data };

  // Add pagination metadata if present
  if (options?.pagination) {
    result.pagination = {
      limit: options.pagination.limit,
      offset: options.pagination.offset,
      total_count: options.pagination.total,
      has_more: options.pagination.offset + options.pagination.limit < options.pagination.total,
      next_offset: /* calculate */,
    };
  }

  return safeStringify(result, 2);  // Pretty-print with 2-space indent
}
```

**Markdown Formatting**:
```typescript
function formatAsMarkdown(data: unknown, options?: FormatOptions): string {
  // Auto-detect data type
  if (Array.isArray(data)) {
    return formatArrayAsMarkdown(data);
  }

  // Format as table if applicable
  if (isTableData(data)) {
    return formatAsMarkdownTable(data);
  }

  // Fallback to JSON
  return "```json\n" + formatAsJson(data, options) + "\n```";
}
```

**Character Limit Enforcement**:
```typescript
function enforceCharacterLimit(text: string, limit?: number): string {
  const maxChars = limit ?? RESPONSE_LIMITS.MAX_CHARACTERS;  // Default: 100,000

  if (text.length <= maxChars) return text;

  const truncated = text.slice(0, maxChars - 200);
  return truncated + "\n\n⚠️ Response truncated (exceeded " + maxChars + " characters)";
}
```

**Benefits**:
- Consistent formatting across all tools
- Automatic truncation for large responses
- Pagination metadata included in JSON
- AI-friendly Markdown tables
- Configurable character limits

### Pagination Implementation ⭐ **NEW**

**Design**: Client-side pagination with metadata (`formatters.ts`)

**Pagination Helper**:
```typescript
export function paginateResults<T>(
  items: T[],
  limit: number = 50,
  offset: number = 0
): {
  data: T[];
  total_count: number;
  has_more: boolean;
  next_offset?: number;
} {
  const totalCount = items.length;
  const paginatedItems = items.slice(offset, offset + limit);
  const hasMore = offset + limit < totalCount;

  return {
    data: paginatedItems,
    total_count: totalCount,
    has_more: hasMore,
    next_offset: hasMore ? offset + limit : undefined,
  };
}
```

**Usage in Tools**:
```typescript
// 1. Fetch all data from API
const allProjects = await client.listProjects();

// 2. Apply pagination
const paginated = paginateResults(allProjects, limit, offset);

// 3. Format with pagination metadata
const formatted = formatResponse(paginated.data, format, {
  pagination: {
    limit,
    offset,
    total: paginated.total_count,
  },
});
```

**Default Limits**:
- Default limit: 50 items
- Max limit: 200 items
- Default offset: 0

**Benefits**:
- Prevents overwhelming AI agents with large datasets
- Supports iteration through results
- Clear "has_more" signal for continuation
- next_offset provided for convenience

### MCP Annotations ⭐ **NEW**

**Design**: First-class support for MCP tool metadata

**Supported Annotations**:

1. **readOnlyHint**: Tool does not modify server state
   ```typescript
   annotations: { readOnlyHint: true }
   // Examples: openl_list_repositories, openl_get_project, openl_search_tables
   ```

2. **idempotentHint**: Tool is safe to retry (same result on repeat)
   ```typescript
   annotations: { idempotentHint: true }
   // Examples: openl_list_projects, openl_get_deployment_info
   ```

3. **destructiveHint**: Tool modifies or deletes data (use with caution)
   ```typescript
   annotations: { destructiveHint: true }
   // Examples: openl_delete_project, openl_erase_project, openl_update_rules
   ```

4. **openWorldHint**: Tool returns dynamic data (may change between calls)
   ```typescript
   annotations: { openWorldHint: true }
   // Examples: openl_list_branches, openl_get_project (data may change)
   ```

**Tool Categorization by Annotations**:

| Annotation | Count | Examples |
|------------|-------|----------|
| readOnlyHint | 20/24 | openl_list_*, openl_get_*, openl_search_* |
| idempotentHint | 18/24 | Most read operations |
| destructiveHint | 3/24 | openl_delete_project, openl_erase_project, openl_update_rules |
| openWorldHint | 22/24 | Almost all tools (dynamic data) |

**Benefits**:
- AI agents can understand tool semantics
- Safety checks for destructive operations
- Better retry strategies (idempotent tools)
- Clear expectations for data stability

### Structured Logging ⭐ **NEW**

**Design**: Stderr-only logging with context sanitization (`logger.ts`)

**Logger API**:
```typescript
import { logger } from "./logger.js";

// Log levels
logger.error("Failed to fetch projects", { repository: "design", error });
logger.warn("Token expiring soon", { expiresIn: 300 });
logger.info("Tool execution started", { toolName: "openl_list_projects" });
logger.debug("API request details", { url, method, headers });
```

**Output Format**:
```
[ERROR] Failed to fetch projects {"repository":"design","error":"[SANITIZED]"}
[WARN] Token expiring soon {"expiresIn":300}
[INFO] Tool execution started {"toolName":"openl_list_projects"}
[DEBUG] API request details {"url":"...","method":"GET"}
```

**Context Sanitization**:
```typescript
function sanitizeContext(context: LogContext): LogContext {
  const sanitized: LogContext = {};
  for (const [key, value] of Object.entries(context)) {
    if (value instanceof Error) {
      sanitized[key] = sanitizeError(value);  // Remove credentials
    } else if (typeof value === "string") {
      sanitized[key] = sanitizeError(value);
    } else {
      sanitized[key] = value;
    }
  }
  return sanitized;
}
```

**Why stderr?**
- MCP protocol uses stdout for communication
- Logs must not interfere with protocol messages
- stderr is the standard channel for logging

**Benefits**:
- No credential leakage in logs
- Structured context for debugging
- Configurable log levels
- MCP-compatible (stderr only)

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
- User-friendly IDs in openl_list_projects
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
Tool: openl_health_check
→ Attempts to list repositories
→ Returns: { status, baseUrl, authMethod, timestamp, serverReachable }
```

**Annotations**:
```typescript
annotations: {
  readOnlyHint: true,
  idempotentHint: true,
  openWorldHint: true,
}
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

## Refactoring History

**Phase 1-3: Major Refactoring (2025-11-16)**
- Split index.ts (766 lines → 352 lines)
- Created tool-handlers.ts (998 lines) with registerTool pattern
- Added formatters.ts (360 lines) for response formatting
- Added validators.ts (117 lines) for input validation
- Added logger.ts (107 lines) for structured logging
- Enhanced schemas.ts with .strict() mode
- Enhanced constants.ts with RESPONSE_LIMITS
- Replaced switch statement pattern with registry-based architecture
- Added MCP annotations (readOnlyHint, idempotentHint, etc.)
- Implemented pagination and character limit enforcement
- Updated all tool names to openl_ prefix
- Total refactoring: ~2,000 lines of new modular code

---

*Last Updated: 2025-11-16*
*Version: 2.0.0* (Post-Refactoring)
*Status: Refactored & Production-Ready*
