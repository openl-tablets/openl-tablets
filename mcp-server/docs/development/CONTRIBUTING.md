# Contributing to OpenL Tablets MCP Server

Thank you for your interest in contributing to the OpenL Tablets MCP Server! This guide will help you understand the codebase structure and how to extend it.

## Table of Contents

- [Development Setup](#development-setup)
- [Code Structure](#code-structure)
- [Adding a New Tool](#adding-a-new-tool)
- [Adding Authentication Methods](#adding-authentication-methods)
- [Testing Guidelines](#testing-guidelines)
- [Code Style](#code-style)
- [Documentation](#documentation)
- [Submitting Changes](#submitting-changes)

## Development Setup

### Prerequisites

- Node.js 18.0.0 or higher
- npm or yarn
- Access to an OpenL Tablets WebStudio instance for testing

### Installation

```bash
# Clone the repository
cd mcp-server

# Install dependencies
npm install

# Build the project
npm run build

# Run tests
npm test

# Run linter
npm run lint
```

### Development Workflow

```bash
# Watch mode for automatic rebuilding
npm run watch

# Run tests in watch mode
npm run test:watch

# Run linter with auto-fix
npm run lint:fix
```

## Code Structure

The codebase is organized into modular components for maintainability:

```text
src/
├── index.ts             # Main MCP server entry point
├── client.ts            # OpenL Tablets API client
├── auth.ts              # Authentication manager (OAuth 2.1, API Key, Basic Auth)
├── tool-handlers.ts     # Tool registration and handlers
├── tools.ts             # Tool metadata definitions
├── schemas.ts           # Zod schemas for validation
├── formatters.ts        # Response formatting (JSON/Markdown)
├── validators.ts        # Input validation utilities
├── logger.ts            # Structured logging
├── utils.ts             # Utility functions
├── types.ts             # TypeScript type definitions
├── constants.ts         # Configuration constants
├── prompts.ts           # Prompt definitions
└── prompts-registry.ts  # Prompt management
```

### Module Responsibilities

- **index.ts**: MCP server setup and initialization
- **client.ts**: High-level API client for OpenL Tablets
- **auth.ts**: Authentication lifecycle, token management, interceptors
- **tool-handlers.ts**: Tool registration functions using `server.registerTool()`
- **tools.ts**: Tool definitions with metadata and categorization
- **schemas.ts**: Zod schemas for type-safe input validation with `.strict()`
- **formatters.ts**: Response formatting utilities (JSON/Markdown) with pagination
- **validators.ts**: Input validation helpers and utilities
- **logger.ts**: Structured logging with different log levels
- **utils.ts**: Shared utility functions (pagination, string manipulation, etc.)
- **types.ts**: TypeScript interfaces for OpenL Tablets API
- **constants.ts**: Configuration defaults and constants
- **prompts.ts**: Dynamic prompt definitions for MCP
- **prompts-registry.ts**: Prompt registration and management

## Adding a New Tool

Follow these steps to add a new MCP tool using the **new registerTool pattern**:

### 1. Define the Input Schema

Add a Zod schema in `src/schemas.ts` with `.strict()`:

```typescript
/**
 * Schema for the new tool
 */
export const myNewToolSchema = z.object({
  projectId: projectIdSchema,
  someParameter: z.string().describe("Description of this parameter"),
  optionalParam: z.number().optional().describe("Optional parameter"),
  response_format: ResponseFormat.optional(),
  limit: z.number().int().positive().max(200).default(50).optional(),
  offset: z.number().int().nonnegative().default(0).optional(),
}).strict();
```

**Important**: Always use `.strict()` to prevent unexpected properties and ensure type safety.

### 2. Add Tool Handler in tool-handlers.ts

Add a registration function in `src/tool-handlers.ts`:

```typescript
/**
 * Register the my_new_tool tool
 */
function registerMyNewTool(server: Server, client: OpenLClient) {
  server.registerTool({
    name: "openl_my_new_tool",
    title: "My New Tool",
    description: "Clear description of what this tool does",
    inputSchema: zodToJsonSchema(myNewToolSchema),
    annotations: {
      readOnlyHint: true,  // If tool doesn't modify state
      idempotentHint: true, // If tool can be safely retried
      openWorldHint: true,
    },
  }, async (args) => {
    // Extract parameters
    const { projectId, someParameter, response_format, limit, offset } =
      args as z.infer<typeof myNewToolSchema>;

    // Call client method
    const result = await client.someMethod(projectId, someParameter);

    // Apply pagination if needed
    if (Array.isArray(result) && limit !== undefined) {
      const paginated = paginateResults(result, limit, offset);
      return {
        content: [{
          type: "text",
          text: formatResponse(paginated, response_format || "markdown", {
            pagination: { limit, offset, total: paginated.total_count },
            dataType: "my_data_type"
          })
        }]
      };
    }

    // Format response
    return {
      content: [{
        type: "text",
        text: formatResponse(result, response_format || "markdown")
      }]
    };
  });
}
```

**Key Points**:
- Tool names MUST use the `openl_` prefix
- Use `zodToJsonSchema()` to convert Zod schemas to JSON Schema
- Set appropriate annotations (`readOnlyHint`, `idempotentHint`, `openWorldHint`)
- Use `formatResponse()` for consistent JSON/Markdown output
- Apply `paginateResults()` for array data with limit/offset

### 3. Register in registerAllTools()

Add your registration function to the main registration function in `src/tool-handlers.ts`:

```typescript
export function registerAllTools(server: Server, client: OpenLClient): void {
  // ... existing registrations
  registerMyNewTool(server, client);
}
```

### 4. Add to tools.ts for Metadata

Add metadata in `src/tools.ts`:

```typescript
{
  name: "openl_my_new_tool",
  description: "Clear description of what this tool does",
  inputSchema: zodToJsonSchema(myNewToolSchema),
  _meta: {
    version: "1.0.0",
    category: TOOL_CATEGORIES.PROJECT, // Choose appropriate category
    requiresAuth: true,
    modifiesState: false, // Set to true if the tool modifies data
  },
}
```

### 5. Add the API Method (if needed)

If the tool requires a new API endpoint, add it to `src/client.ts`:

```typescript
/**
 * Description of what this method does
 *
 * @param projectId - Project identifier
 * @param someParameter - Parameter description
 * @returns Description of return value
 */
async myNewMethod(
  projectId: string,
  someParameter: string
): Promise<ReturnType> {
  const [repository, projectName] = this.parseProjectId(projectId);
  const response = await this.axiosInstance.post<ReturnType>(
    `/repos/${repository}/projects/${projectName}/endpoint`,
    { someParameter }
  );
  return response.data;
}
```

### 6. Add Tests

Add tests in `tests/`:

```typescript
describe("openl_my_new_tool", () => {
  it("should handle valid input", async () => {
    // Test implementation
  });

  it("should validate input parameters", async () => {
    // Test validation with strict schema
  });

  it("should reject unexpected properties", async () => {
    // Test .strict() validation
  });

  it("should handle pagination correctly", async () => {
    // Test pagination logic
  });

  it("should format responses correctly", async () => {
    // Test both JSON and Markdown formats
  });

  it("should handle errors gracefully", async () => {
    // Test error handling
  });
});
```

### 7. Update Documentation

Add examples to [Usage Examples](../guides/EXAMPLES.md) and update [README](../../README.md) if needed.

## Response Formatting and Pagination

The MCP server provides built-in support for response formatting and pagination to ensure consistent output across all tools.

### Response Formatting

All tools support two output formats via the `response_format` parameter:

- **markdown** (default): Human-readable formatted output
- **json**: Structured JSON for programmatic processing

Use the `formatResponse()` utility from `src/formatters.ts`:

```typescript
import { formatResponse } from './formatters.js';

// Format a single object
const formatted = formatResponse(
  data,
  response_format || "markdown",
  { dataType: "project" }
);

// Format with pagination metadata
const formatted = formatResponse(
  paginatedData,
  response_format || "markdown",
  {
    pagination: { limit, offset, total: paginatedData.total_count },
    dataType: "projects"
  }
);
```

### Pagination Support

For tools that return arrays, implement pagination using `limit` and `offset` parameters:

```typescript
import { paginateResults } from './utils.js';

// In your schema
export const myToolSchema = z.object({
  // ... other fields
  limit: z.number().int().positive().max(200).default(50).optional(),
  offset: z.number().int().nonnegative().default(0).optional(),
}).strict();

// In your tool handler
if (Array.isArray(result) && limit !== undefined) {
  const paginated = paginateResults(result, limit, offset);
  return {
    content: [{
      type: "text",
      text: formatResponse(paginated, response_format || "markdown", {
        pagination: { limit, offset, total: paginated.total_count },
        dataType: "items"
      })
    }]
  };
}
```

**Pagination Guidelines**:
- Default `limit`: 50 items
- Maximum `limit`: 200 items
- Default `offset`: 0
- Always validate with Zod schemas
- Include total count in responses
- Document pagination in tool descriptions

### Format Options

The `formatResponse()` function accepts an optional `options` object:

```typescript
interface FormatOptions {
  pagination?: {
    limit: number;
    offset: number;
    total: number;
  };
  dataType?: string;  // Used for markdown headers and context
}
```

**Example Markdown Output**:
```markdown
# Projects (3 total)

## Project: insurance-rules
**Status**: active
**Modified**: 2024-01-15

---
Pagination: Showing 1-3 of 3 items
```

**Example JSON Output**:
```json
{
  "data": [...],
  "total_count": 3,
  "limit": 50,
  "offset": 0
}
```

## Adding Authentication Methods

To add a new authentication method:

### 1. Update Types

Add configuration interface in `src/types.ts`:

```typescript
export interface MyAuthConfig {
  // Configuration fields
}

export interface OpenLConfig {
  // ... existing fields
  myAuth?: MyAuthConfig;
}
```

### 2. Update Authentication Manager

Modify `src/auth.ts`:

```typescript
private async addAuthHeaders(
  config: InternalAxiosRequestConfig
): Promise<InternalAxiosRequestConfig> {
  // ... existing authentication

  // Add new authentication method
  else if (this.config.myAuth) {
    // Implement authentication logic
    config.headers[HEADERS.AUTHORIZATION] = `Bearer ${token}`;
  }

  return config;
}
```

### 3. Update Health Check

Update `getAuthMethod()` in `src/auth.ts`:

```typescript
public getAuthMethod(): string {
  if (this.config.myAuth) {
    return "My Auth";
  }
  // ... existing methods
}
```

### 4. Document the New Method

Update [Authentication Guide](../guides/AUTHENTICATION.md) with configuration examples and usage instructions.

## Testing Guidelines

### Test Structure

- **Unit Tests**: Test individual functions and classes
- **Integration Tests**: Test API interactions with mocking
- **Mock Data**: Use realistic mock responses in `tests/mocks/`

### Writing Tests

```typescript
describe("Feature", () => {
  let client: OpenLClient;

  beforeEach(() => {
    client = new OpenLClient({
      baseUrl: "http://localhost:8080/rest",
      username: "test",
      password: "test",
    });
  });

  it("should perform expected behavior", async () => {
    // Arrange: Set up test data and mocks
    const mockData = { /* ... */ };

    // Act: Execute the function
    const result = await client.someMethod();

    // Assert: Verify the results
    expect(result).toEqual(mockData);
  });
});
```

### Running Tests

```bash
# Run all tests
npm test

# Run specific test file
npm test -- openl-client.test.ts

# Run with coverage
npm run test:coverage

# Watch mode for TDD
npm run test:watch
```

## Code Style

### TypeScript

- Use **strict mode** (enabled in tsconfig.json)
- Prefer **interfaces** over types for object shapes
- Use **async/await** over promises
- Always add **return types** to functions
- Use **descriptive variable names**

### Naming Conventions

- **Classes**: PascalCase (e.g., `OpenLClient`)
- **Functions/Methods**: camelCase (e.g., `listProjects`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `DEFAULTS`)
- **Interfaces**: PascalCase (e.g., `OpenLConfig`)
- **Files**: kebab-case (e.g., `openl-client.ts`)

### Documentation

- Add **JSDoc comments** to all public functions and classes
- Include parameter descriptions with `@param`
- Include return value descriptions with `@returns`
- Add usage examples for complex functions

Example:

```typescript
/**
 * Get detailed project information
 *
 * @param projectId - Project ID in base64-encoded format (default). Supports backward compatibility with "repository-projectName" and "repository:projectName" formats.
 * @returns Project details including metadata and configuration
 * @throws Error if project ID format is invalid
 *
 * @example
 * ```typescript
 * const project = await client.getProject("design-insurance-rules");
 * console.log(project.name, project.status);
 * ```
 */
async getProject(projectId: string): Promise<Types.Project> {
  // Implementation
}
```

## Documentation

### Inline Documentation

- Add comments for **complex logic**
- Explain **why**, not just **what**
- Update comments when code changes

### External Documentation

- **[README.md](../../README.md)**: User-facing documentation
- **[Authentication Guide](../guides/AUTHENTICATION.md)**: Authentication setup guide
- **[Testing Guide](TESTING.md)**: Testing documentation
- **[Usage Examples](../guides/EXAMPLES.md)**: Usage examples
- **[Contributing Guide](CONTRIBUTING.md)**: This file

## Submitting Changes

### Before Submitting

1. **Run tests**: `npm test`
2. **Run linter**: `npm run lint`
3. **Build successfully**: `npm run build`
4. **Update documentation**: Add/update relevant docs
5. **Add tests**: For new features or bug fixes

### Commit Messages

Follow conventional commit format:

```text
type(scope): subject

body (optional)

footer (optional)
```

Types:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks

Examples:

```text
feat(tools): add support for table validation

Add new tool 'openl_validate_table' that checks table structure
and data integrity before deployment.

Closes #123
```

```text
fix(auth): handle OAuth token refresh race condition

Prevent concurrent token refresh requests by caching the
refresh promise and reusing it across multiple requests.
```

### Pull Request Guidelines

1. Create a descriptive PR title
2. Describe what changed and why
3. Reference related issues
4. Include test results
5. Update relevant documentation

## Best Practices

### Error Handling

- Always catch and handle errors appropriately
- Use `McpError` with proper error codes
- Include context in error messages
- Log errors with sufficient detail

```typescript
try {
  const result = await client.someMethod();
  return result;
} catch (error: any) {
  if (axios.isAxiosError(error)) {
    throw new McpError(
      ErrorCode.InternalError,
      `API error (${error.response?.status}): ${error.message}`,
      {
        endpoint: error.config?.url,
        method: error.config?.method,
        status: error.response?.status,
      }
    );
  }
  throw error;
}
```

### Performance

- Use connection pooling (Axios default)
- Cache OAuth tokens appropriately
- Avoid unnecessary API calls
- Use pagination for large datasets

### Security

- Never log sensitive data (passwords, tokens)
- Use environment variables for credentials
- Validate all user inputs with Zod schemas
- Follow principle of least privilege

## Getting Help

- Check existing documentation
- Search for similar issues
- Ask in discussions
- Review existing code for patterns

## License

This project follows the same license as OpenL Tablets.

---

Thank you for contributing to the OpenL Tablets MCP Server!
