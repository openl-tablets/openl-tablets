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

```
src/
├── index.ts          # Main MCP server entry point
├── client.ts         # OpenL Tablets API client
├── auth.ts           # Authentication manager (OAuth 2.1, API Key, Basic Auth)
├── tools.ts          # MCP tool definitions and metadata
├── schemas.ts        # Zod schemas for input validation
├── types.ts          # TypeScript type definitions
└── constants.ts      # Configuration constants and defaults
```

### Module Responsibilities

- **index.ts**: MCP server setup, tool handlers, resource providers
- **client.ts**: High-level API client for OpenL Tablets
- **auth.ts**: Authentication lifecycle, token management, interceptors
- **tools.ts**: Tool definitions with metadata and categorization
- **schemas.ts**: Zod schemas for type-safe input validation
- **types.ts**: TypeScript interfaces for OpenL Tablets API
- **constants.ts**: Configuration defaults and constants

## Adding a New Tool

Follow these steps to add a new MCP tool:

### 1. Define the Input Schema

Add a Zod schema in `src/schemas.ts`:

```typescript
/**
 * Schema for the new tool
 */
export const myNewToolSchema = z.object({
  projectId: projectIdSchema,
  someParameter: z.string().describe("Description of this parameter"),
  optionalParam: z.number().optional().describe("Optional parameter"),
});
```

### 2. Add the Tool Definition

Add the tool to `src/tools.ts`:

```typescript
{
  name: "my_new_tool",
  description: "Clear description of what this tool does",
  inputSchema: zodToJsonSchema(schemas.myNewToolSchema) as any,
  _meta: {
    version: "1.0.0",
    category: TOOL_CATEGORIES.PROJECT, // Choose appropriate category
    requiresAuth: true,
    modifiesState: true, // Set to true if the tool modifies data
  },
}
```

### 3. Add the API Method (if needed)

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
    `/design-repositories/${repository}/projects/${projectName}/endpoint`,
    { someParameter }
  );
  return response.data;
}
```

### 4. Add the Tool Handler

Add the tool handler in `src/index.ts`:

```typescript
case "my_new_tool": {
  if (!args) throw new McpError(ErrorCode.InvalidParams, "Missing arguments");

  const { projectId, someParameter, optionalParam } = args as {
    projectId: string;
    someParameter: string;
    optionalParam?: number;
  };

  const result = await this.client.myNewMethod(projectId, someParameter);

  return {
    content: [
      {
        type: "text",
        text: JSON.stringify(result, null, 2),
      },
    ],
  };
}
```

### 5. Add Tests

Add tests in `tests/`:

```typescript
describe("myNewTool", () => {
  it("should handle valid input", async () => {
    // Test implementation
  });

  it("should validate input parameters", async () => {
    // Test validation
  });

  it("should handle errors gracefully", async () => {
    // Test error handling
  });
});
```

### 6. Update Documentation

Add examples to `EXAMPLES.md` and update `README.md` if needed.

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

Update `AUTHENTICATION.md` with configuration examples and usage instructions.

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
      baseUrl: "http://localhost:8080/webstudio/rest",
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
 * @param projectId - Project ID in format "repository-projectName"
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

- **README.md**: User-facing documentation
- **AUTHENTICATION.md**: Authentication setup guide
- **TESTING.md**: Testing documentation
- **EXAMPLES.md**: Usage examples
- **CONTRIBUTING.md**: This file

## Submitting Changes

### Before Submitting

1. **Run tests**: `npm test`
2. **Run linter**: `npm run lint`
3. **Build successfully**: `npm run build`
4. **Update documentation**: Add/update relevant docs
5. **Add tests**: For new features or bug fixes

### Commit Messages

Follow conventional commit format:

```
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

```
feat(tools): add support for table validation

Add new tool 'validate_table' that checks table structure
and data integrity before deployment.

Closes #123
```

```
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
