# OpenL Tablets MCP Server - Constitution

## Project Identity

**Name**: OpenL Tablets MCP Server
**Purpose**: Model Context Protocol server for OpenL Tablets Business Rules Management System
**Version**: 2.0.0
**License**: MIT
**Repository**: https://github.com/openl-tablets/openl-tablets/tree/main/mcp-server

## Mission Statement

Provide a production-ready, enterprise-grade MCP server that enables AI coding agents to interact seamlessly with OpenL Tablets Business Rules Management System through a type-safe, well-documented, and secure interface.

## Core Principles

### 1. Type Safety Above All

- **Strict TypeScript** with zero tolerance for `any` types
- **Runtime validation** using Zod schemas for all inputs
- **Explicit return types** on all functions
- **Type inference** from schemas to avoid duplication
- **Type guards** for proper error handling

**Rationale**: Type safety prevents runtime errors and provides excellent IDE support for developers.

### 2. Security First

- **Never log credentials** - All error messages must be sanitized
- **Input validation** - Validate all user inputs before processing
- **URL validation** - Check all URLs for format and safety
- **Timeout limits** - All network requests must have timeouts
- **Least privilege** - Request only necessary permissions

**Implementation**:
```typescript
// All errors pass through sanitizeError()
catch (error: unknown) {
  const sanitizedMessage = sanitizeError(error);
  throw new McpError(ErrorCode.InternalError, sanitizedMessage);
}
```

### 3. Modular Architecture

- **Single responsibility** - Each module has one clear purpose
- **Clear separation** - No circular dependencies
- **Well-defined interfaces** - Clean contracts between modules
- **Easy to extend** - Adding features should be straightforward

**Module Structure**:
- `index.ts` - Server orchestration and initialization
- `client.ts` - OpenL API client implementation
- `auth.ts` - Authentication lifecycle management
- `tool-handlers.ts` - Tool registration functions (registerTool pattern)
- `tools.ts` - Tool metadata and constants
- `schemas.ts` - Zod input validation schemas (.strict())
- `types.ts` - TypeScript type definitions
- `constants.ts` - Configuration constants
- `utils.ts` - Security and formatting utilities

### 4. Documentation as Code

- **JSDoc on all public APIs** - No exceptions
- **Parameter descriptions** - Explain what each parameter does
- **Usage examples** - Show how to use complex functions
- **External documentation** - Comprehensive guides for users

**Documentation Set**:
- README.md - User-facing overview
- AUTHENTICATION.md - Auth setup guide
- CONTRIBUTING.md - Developer guide
- TESTING.md - Testing documentation
- EXAMPLES.md - Usage examples
- BEST_PRACTICES.md - Implementation practices

### 5. Test-Driven Quality

- **Comprehensive tests** - All critical paths covered
- **Mock data** - Realistic test scenarios
- **Fast feedback** - Tests run quickly in watch mode
- **Coverage goals** - Maintain >80% coverage on critical modules

**Testing Stack**:
- Jest with ESM support
- Nock for HTTP mocking
- TypeScript support via ts-jest

### 6. Tool Naming Consistency

**Principle**: All MCP tools MUST use the `openl_` prefix for namespace clarity and MCP compliance.

**Rationale**:
- Prevents naming conflicts in multi-server MCP environments
- Clearly identifies the service provider
- Follows MCP best practices from official guidelines
- Makes tool discovery more intuitive

**Application**:
- All tool names: `openl_list_projects`, `openl_get_table`, `openl_search_rules`, etc.
- No exceptions - even simple tools use prefix
- Applies to new tools added in future
- Consistent with MCP namespace conventions

**Examples**:
```typescript
// ✅ Correct - with openl_ prefix
server.registerTool("openl_list_projects", ...);
server.registerTool("openl_get_table", ...);

// ❌ Incorrect - missing prefix
server.registerTool("list_projects", ...);
server.registerTool("getTable", ...);
```

### 7. Error Handling Consistency

- **Unknown over any** - Error types should be unknown
- **Type guards** - Check error types before accessing properties
- **Enhanced context** - Include HTTP status, endpoint, method
- **Sanitized messages** - Redact all credentials

**Error Pattern**:
```typescript
catch (error: unknown) {
  if (axios.isAxiosError(error)) {
    throw new McpError(
      ErrorCode.InternalError,
      `API error (${error.response?.status}): ${sanitizeError(error)}`
    );
  }
  throw error;
}
```

### 8. Performance Consciousness

- **Token caching** - Cache OAuth tokens with expiration
- **Connection pooling** - Use Axios default pooling
- **Lazy evaluation** - Fetch resources only when needed
- **Concurrent safety** - Prevent duplicate requests

### 9. Maintainability Focus

- **Clear naming** - Functions named for what they do
- **Consistent patterns** - Same approach throughout codebase
- **No magic numbers** - All constants defined and named
- **Comments when needed** - Explain complex logic

### 10. MCP Protocol Compliance

- **Latest SDK** - Use stable MCP SDK v1.21.1+
- **Standard patterns** - Follow MCP best practices
- **Tool metadata** - Include version, category, auth requirements
- **Resource URIs** - Follow MCP URI conventions

### 11. Dependency Hygiene

- **Minimal dependencies** - Only essential packages
- **Active maintenance** - All dependencies actively maintained
- **Security audit** - Regular npm audit checks
- **Version pinning** - Use exact versions in production

**Production Dependencies** (5 only):
- `@modelcontextprotocol/sdk`
- `axios`
- `form-data`
- `zod`
- `zod-to-json-schema`

## Development Guidelines

### Code Style

**Naming Conventions**:
- Classes: `PascalCase`
- Functions/Methods: `camelCase`
- Constants: `UPPER_SNAKE_CASE`
- Interfaces: `PascalCase`
- Files: `kebab-case`

**TypeScript Rules**:
- Strict mode enabled
- No implicit any
- Explicit return types
- Prefer interfaces over types
- Use async/await over promises

**ESLint Compliance**:
- 0 errors
- 0 warnings
- Fix issues, don't suppress (except documented exceptions)

### RegisterTool Pattern (Not Switch Statements)

**Guideline**: Use the `server.registerTool()` pattern, never switch statements.

**Rationale**:
- **Modular**: Each tool in its own function
- **Testable**: Tools can be tested independently
- **Maintainable**: Easy to add/modify tools
- **MCP-compliant**: Supports annotations natively

**Process**:
1. Define schema with `.strict()` in `schemas.ts`
2. Create registration function in `tool-handlers.ts`
3. Add MCP annotations (`readOnlyHint`, etc.)
4. Register in `registerAllTools()`
5. Add metadata to `tools.ts`

**Example**:
```typescript
// ✅ Correct - registerTool pattern
export function registerListProjectsTool(
  server: Server,
  client: OpenLClient
): void {
  server.registerTool(
    "openl_list_projects",
    {
      description: "List all OpenL Tablets projects",
      inputSchema: zodToJsonSchema(ListProjectsArgsSchema),
      annotations: {
        readOnlyHint: true,
        requiresAuth: true,
      },
    },
    async (args) => {
      const { limit, offset, response_format } =
        ListProjectsArgsSchema.parse(args);
      const projects = await client.listProjects(limit, offset);
      return formatResponse(projects, response_format);
    }
  );
}

// ❌ Incorrect - switch statement pattern
server.setRequestHandler(CallToolRequestSchema, async (request) => {
  switch (request.params.name) {
    case "openl_list_projects":
      // handler code...
      break;
  }
});
```

### Response Format Support

**Guideline**: All tools MUST support `response_format` parameter (json/markdown).

**Default**: `markdown` (better for AI consumption)

**Rationale**:
- Flexibility for different use cases
- Markdown provides better readability for AI agents
- JSON enables programmatic processing
- Consistent user experience across tools

**Implementation**:
```typescript
// Schema definition
export const BaseToolArgsSchema = z.object({
  response_format: z.enum(['json', 'markdown']).default('markdown'),
}).strict();

// Usage in tool
const result = response_format === 'json'
  ? { content: [{ type: "text", text: JSON.stringify(data, null, 2) }] }
  : { content: [{ type: "text", text: formatAsMarkdown(data) }] };
```

### Pagination Standard

**Guideline**: All list operations MUST support pagination (limit/offset).

**Defaults**:
- `limit`: 50 (max 200)
- `offset`: 0

**Rationale**:
- Handle large datasets efficiently
- Prevent memory issues
- Reduce network overhead
- Improve response times

**Implementation**:
```typescript
export const PaginationArgsSchema = z.object({
  limit: z.number().int().min(1).max(200).default(50),
  offset: z.number().int().min(0).default(0),
}).strict();

// Extend in tool schemas
export const ListProjectsArgsSchema = BaseToolArgsSchema
  .merge(PaginationArgsSchema)
  .strict();
```

### Commit Guidelines

**Conventional Commits Format**:
```
type(scope): subject

body (optional)

footer (optional)
```

**Types**:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `refactor`: Code refactoring
- `test`: Testing changes
- `chore`: Maintenance tasks

**Example**:
```
feat(tools): add table validation tool

Add new tool 'validate_table' that checks table structure
and data integrity before deployment.

Closes #123
```

### Testing Requirements

**Before Submitting**:
- [ ] All tests pass (`npm test`)
- [ ] Linter clean (`npm run lint`)
- [ ] Build successful (`npm run build`)
- [ ] Documentation updated
- [ ] Tests added for new features

**Test Coverage Targets**:
- Critical modules (auth, client, index): >85%
- Utilities: >90%
- Overall project: >80%

### Extension Pattern

**Adding a New Tool** (6 Steps):
1. Define Zod schema with `.strict()` in `schemas.ts`
2. Create registration function in `tool-handlers.ts` using `registerTool()`
3. Add tool metadata to `tools.ts`
4. Add API method to `client.ts` (if needed)
5. Register in `registerAllTools()` in `tool-handlers.ts`
6. Add tests and examples

**Example - Adding a New Tool**:
```typescript
// 1. schemas.ts - Define schema with .strict()
export const GetProjectArgsSchema = BaseToolArgsSchema
  .merge(z.object({
    project_name: z.string().min(1).describe("Project name"),
  }))
  .strict();

// 2. tool-handlers.ts - Create registration function
export function registerGetProjectTool(
  server: Server,
  client: OpenLClient
): void {
  server.registerTool(
    "openl_get_project",  // Note: openl_ prefix required
    {
      description: "Get detailed information about a project",
      inputSchema: zodToJsonSchema(GetProjectArgsSchema),
      annotations: {
        readOnlyHint: true,
        requiresAuth: true,
      },
    },
    async (args) => {
      const { project_name, response_format } =
        GetProjectArgsSchema.parse(args);
      const project = await client.getProject(project_name);
      return formatResponse(project, response_format);
    }
  );
}

// 3. tools.ts - Add metadata
export const TOOL_METADATA = {
  openl_get_project: {
    category: "project_management",
    requiresAuth: true,
    version: "2.0.0",
  },
};

// 4. client.ts - Add API method (if needed)
async getProject(projectName: string): Promise<Project> {
  const response = await this.makeRequest<Project>(
    `/projects/${projectName}`
  );
  return response.data;
}

// 5. tool-handlers.ts - Register in registerAllTools()
export function registerAllTools(
  server: Server,
  client: OpenLClient
): void {
  registerListProjectsTool(server, client);
  registerGetProjectTool(server, client);  // Add this line
  // ... other tools
}

// 6. Add tests to __tests__/tool-handlers.test.ts
```

**Adding Authentication Method**:
1. Update types in `types.ts`
2. Implement in `auth.ts`
3. Update `getAuthMethod()`
4. Document in `AUTHENTICATION.md`

## Quality Gates

### Pre-Commit
- Code compiles without errors
- ESLint passes with 0 errors/warnings
- All tests pass

### Pre-PR
- Test coverage maintained
- Documentation updated
- Changelog updated (if applicable)
- Examples added (if new feature)

### Pre-Release
- Full test suite passes
- npm audit clean (0 vulnerabilities)
- Documentation complete and reviewed
- Examples tested and verified

## Non-Negotiables

**Code Quality**:
- ❌ **No credentials in code** - Ever. Use environment variables.
- ❌ **No any types** - Use unknown and type guards instead.
- ❌ **No suppressing ESLint** - Fix issues, don't hide them.
- ❌ **No missing tests** - New features require tests.
- ❌ **No incomplete docs** - Public APIs must have JSDoc.
- ❌ **No circular dependencies** - Keep module graph acyclic.
- ❌ **No magic numbers** - Define constants with meaningful names.

**Architecture**:
- ❌ **Tools without `openl_` prefix** - All tools must use namespace prefix.
- ❌ **Schemas without `.strict()` mode** - All Zod schemas must be strict.
- ❌ **List operations without pagination support** - All list tools must support limit/offset.
- ❌ **Tools without `response_format` parameter** - All tools must support json/markdown output.
- ❌ **Switch statements for tool handling** - Use `registerTool()` pattern only.

**Security**:
- ❌ **No security vulnerabilities** - npm audit must be clean.
- ❌ **No unvalidated inputs** - All inputs must pass through Zod schemas.
- ❌ **No timeout-less requests** - All network requests must have timeouts.

## Success Metrics

**Code Quality**:
- TypeScript strict: ✓
- ESLint clean: ✓
- Test coverage: >80%
- Documentation: Complete

**Security**:
- npm audit: 0 vulnerabilities
- Credential sanitization: 100%
- Input validation: All inputs validated

**Performance**:
- Token caching: Implemented
- Connection pooling: Enabled
- Memory leaks: None

**Maintainability**:
- Modular architecture: ✓
- Clear patterns: ✓
- Extension guides: ✓
- No technical debt

## Decision-Making Framework

When faced with design decisions, prioritize in this order:

1. **Security** - Is it safe?
2. **Type Safety** - Is it type-safe?
3. **Simplicity** - Is it simple?
4. **Performance** - Is it fast enough?
5. **Extensibility** - Can it grow?

## Philosophy

> "Make it work, make it right, make it fast - in that order."
>
> "Type safety isn't optional - it's the foundation."
>
> "Security is not a feature - it's a requirement."
>
> "Documentation is for humans - code is for machines."
>
> "Namespacing prevents chaos - prefix everything."
>
> "Modularity beats monoliths - one tool, one function."
>
> "Strict schemas catch bugs - always use .strict()."

## Enforcement

This constitution is enforced through:

- **Automated checks** - ESLint, TypeScript, Jest
- **Code review** - All PRs reviewed for compliance
- **CI/CD pipeline** - Gates prevent non-compliant code
- **Documentation** - Guidelines clearly documented

## Evolution

This constitution may evolve as the project grows, but core principles (type safety, security, modularity) are immutable.

**Amendment Process**:
1. Propose change in issue or discussion
2. Document rationale and impact
3. Gather feedback from contributors
4. Update constitution with consensus
5. Update tooling to enforce new rules

---

*Last Updated: 2025-11-16*
*Version: 2.0.0 (Post-Refactoring)*
*Status: Active*
