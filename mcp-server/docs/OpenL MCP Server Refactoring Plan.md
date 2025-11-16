Executive Summary
This plan refactors the existing MCP server to fully comply with the official MCP guidelines from https://github.com/anthropics/skills/tree/main/mcp-builder. The refactoring will modernize tool registration, add response formatting, implement pagination, strengthen validation, and improve maintainability.

Current State Analysis
Existing Implementation
Package name: openl-tablets-mcp-server
Server name: openl-tablets (in SERVER_INFO)
Tools: 19 tools using switch statement routing
Tool names: No service prefix (e.g., list_repositories, update_table)
Registration: TOOLS array with switch-case handlers
Schemas: Zod schemas WITHOUT .strict()
Output: JSON only (no markdown option)
Pagination: Partial (only in history tools)
Character limits: None enforced
Annotations: Custom _meta field (not MCP spec compliant)
Logging: console.error for errors
Compliance Gaps
❌ Tool names lack service prefix
❌ Using switch statement instead of registerTool
❌ Missing MCP annotations (readOnlyHint, destructiveHint, etc.)
❌ No response_format parameter
❌ No pagination for list operations
❌ No character limit enforcement (~25,000 chars)
❌ Schemas not using .strict()
Refactoring Objectives
1. Rename Server and Tools (Naming Conventions)
1.1 Server Naming
Current: openl-tablets (in code), openl-tablets-mcp-server (package.json) Proposed: openl-mcp-server (consistent everywhere)

Changes:

✏️ package.json: Update name to openl-mcp-server
✏️ package.json: Update bin to openl-mcp
✏️ constants.ts: Update SERVER_INFO.NAME to openl-mcp-server
✏️ README.md: Update all references
✏️ Claude Desktop config examples
1.2 Tool Naming
Current: list_repositories, update_table, etc. Proposed: openl_ prefix for all tools

Mapping (19 tools):

list_repositories      → openl_list_repositories
list_branches          → openl_list_branches
list_projects          → openl_list_projects
get_project            → openl_get_project
update_project_status  → openl_update_project_status
upload_file            → openl_upload_file
download_file          → openl_download_file
list_tables            → openl_list_tables
get_table              → openl_get_table
update_table           → openl_update_table
append_table           → openl_append_table
create_rule            → openl_create_rule
list_deployments       → openl_list_deployments
deploy_project         → openl_deploy_project
execute_rule           → openl_execute_rule
revert_version         → openl_revert_version
get_file_history       → openl_get_file_history
get_project_history    → openl_get_project_history
validate_project       → openl_validate_project (commented out)
get_project_errors     → openl_get_project_errors (commented out)
Changes:

✏️ tools.ts: Update all tool names
✏️ index.ts: Update switch statement cases
✏️ schemas.ts: No changes (schema variable names stay the same)
✏️ README.md & EXAMPLES.md: Update all examples
✏️ tests/: Update all test cases
✏️ prompts/: Update references in prompt files
2. Adopt Standard Tool Registration (registerTool Pattern)
2.1 Replace Switch Statement with registerTool
Current: 300+ line switch statement in index.ts:handleToolCall() Proposed: Individual server.registerTool() calls per tool

Implementation Pattern:

// For each tool in tools.ts, register with annotations
server.registerTool({
  name: "openl_list_repositories",
  title: "List OpenL Repositories",
  description: `
    List all design repositories in OpenL Tablets.
    
    Returns repository names, types (Git/Database), status, and metadata.
    Use this to discover available repositories before accessing projects.
    
    Example: List all repositories to find where your rules projects are stored.
  `,
  inputSchema: zodToJsonSchema(listRepositoriesInputSchema.strict()),
  annotations: {
    readOnlyHint: true,
    openWorldHint: true,
  },
}, async (args) => {
  // Handler implementation
  const result = await client.listRepositories(args);
  return formatResponse(result, args.response_format);
});
2.2 Create Tool Registration Module
New file: src/tool-handlers.ts

Structure:

export function registerAllTools(server: Server, client: OpenLClient): void {
  // Repository tools
  registerListRepositories(server, client);
  registerListBranches(server, client);
  
  // Project tools
  registerListProjects(server, client);
  registerGetProject(server, client);
  // ... etc
}

function registerListRepositories(server: Server, client: OpenLClient) {
  server.registerTool({...}, async (args) => {...});
}
2.3 Add MCP Annotations
Map custom _meta to MCP annotations:

| Tool | readOnlyHint | destructiveHint | idempotentHint | openWorldHint | |------|--------------|-----------------|----------------|---------------| | openl_list_repositories | ✓ | ✗ | ✓ | ✓ | | openl_list_branches | ✓ | ✗ | ✓ | ✓ | | openl_list_projects | ✓ | ✗ | ✓ | ✓ | | openl_get_project | ✓ | ✗ | ✓ | ✓ | | openl_update_project_status | ✗ | ✓ (if discard) | ✗ | ✓ | | openl_upload_file | ✗ | ✗ | ✓ | ✓ | | openl_download_file | ✓ | ✗ | ✓ | ✓ | | openl_list_tables | ✓ | ✗ | ✓ | ✓ | | openl_get_table | ✓ | ✗ | ✓ | ✓ | | openl_update_table | ✗ | ✗ | ✓ | ✓ | | openl_append_table | ✗ | ✗ | ✓ | ✓ | | openl_create_rule | ✗ | ✗ | ✗ | ✓ | | openl_list_deployments | ✓ | ✗ | ✓ | ✓ | | openl_deploy_project | ✗ | ✗ | ✓ | ✓ | | openl_execute_rule | ✓ | ✗ | ✓ | ✓ | | openl_revert_version | ✗ | ✓ | ✗ | ✓ | | openl_get_file_history | ✓ | ✗ | ✓ | ✓ | | openl_get_project_history | ✓ | ✗ | ✓ | ✓ |

Changes:

✏️ Delete index.ts:handleToolCall() switch statement
✏️ Create tool-handlers.ts with all registrations
✏️ Update tools.ts to include MCP annotations
✏️ Add title field to each tool (human-friendly short name)
3. Implement Response Formatting and Pagination
3.1 Add response_format Parameter
New enum:

export const ResponseFormat = z.enum(["json", "markdown"]).default("markdown");
Add to ALL tool schemas:

export const listRepositoriesSchema = z.object({
  response_format: ResponseFormat.optional(),
}).strict();

export const listProjectsSchema = z.object({
  repository: z.string().optional(),
  status: z.string().optional(),
  tag: z.string().optional(),
  response_format: ResponseFormat.optional(),
  limit: z.number().int().positive().max(200).default(50).optional(),
  offset: z.number().int().nonnegative().default(0).optional(),
}).strict();
3.2 Create Response Formatter
New file: src/formatters.ts

export interface PaginatedResponse<T> {
  data: T;
  pagination?: {
    limit: number;
    offset: number;
    has_more: boolean;
    next_offset?: number;
    total_count?: number;
  };
  truncated?: boolean;
  truncation_message?: string;
}

export function formatResponse<T>(
  data: T,
  format: "json" | "markdown" = "markdown",
  options?: {
    pagination?: { limit: number; offset: number; total: number };
    characterLimit?: number;
  }
): string {
  // 1. Apply pagination metadata
  // 2. Convert to string (JSON or Markdown)
  // 3. Check character limit (~25,000)
  // 4. Truncate if needed with message
  // 5. Return formatted string
}

export function toMarkdown(data: unknown, type: string): string {
  // Convert JSON to readable markdown
  // - Repositories: table with name, type, status
  // - Projects: list with bullet points
  // - Tables: formatted table structure
  // etc.
}
3.3 Add Pagination to List Operations
Tools requiring pagination (8):

openl_list_repositories
openl_list_branches
openl_list_projects
openl_list_tables
openl_list_deployments
openl_get_file_history (already has it)
openl_get_project_history (already has it)
Implementation:

// If OpenL API doesn't support pagination, implement client-side
function paginateResults<T>(
  results: T[],
  limit: number,
  offset: number
): { data: T[]; has_more: boolean; next_offset?: number; total_count: number } {
  const total_count = results.length;
  const data = results.slice(offset, offset + limit);
  const has_more = offset + limit < total_count;
  const next_offset = has_more ? offset + limit : undefined;
  
  return { data, has_more, next_offset, total_count };
}
3.4 Character Limit Enforcement
New constant: src/constants.ts

export const RESPONSE_LIMITS = {
  /** Maximum response character count (~25,000) */
  MAX_CHARACTERS: 25000,
  
  /** Truncation warning message */
  TRUNCATION_MESSAGE: "Response truncated due to size. Use limit/offset parameters or narrower filters to retrieve full data.",
} as const;
Apply in formatters:

if (formattedString.length > RESPONSE_LIMITS.MAX_CHARACTERS) {
  const truncated = formattedString.slice(0, RESPONSE_LIMITS.MAX_CHARACTERS);
  return {
    ...result,
    truncated: true,
    truncation_message: RESPONSE_LIMITS.TRUNCATION_MESSAGE,
    content: truncated,
  };
}
Changes:

✏️ Create formatters.ts with response formatting logic
✏️ Update all schemas to include response_format, limit, offset
✏️ Add RESPONSE_LIMITS to constants.ts
✏️ Update all tool handlers to use formatters
✏️ Add markdown conversion for each data type
4. Strengthen Input Validation and Security
4.1 Add .strict() to All Schemas
Current: 19 schemas without .strict() Proposed: All schemas use .strict() to reject unknown fields

Example:

// Before
export const listProjectsSchema = z.object({
  repository: z.string().optional(),
});

// After
export const listProjectsSchema = z.object({
  repository: z.string().optional(),
  response_format: ResponseFormat.optional(),
  limit: z.number().int().positive().max(200).default(50).optional(),
  offset: z.number().int().nonnegative().default(0).optional(),
}).strict();
Changes:

✏️ Add .strict() to all 19+ schemas in schemas.ts
4.2 Enhanced Base64 Validation
New validators: src/validators.ts

export function validateBase64(content: string): boolean {
  const base64Regex = /^[A-Za-z0-9+/]*={0,2}$/;
  if (!base64Regex.test(content)) return false;
  
  try {
    Buffer.from(content, "base64");
    return true;
  } catch {
    return false;
  }
}

export function validateProjectId(projectId: string): { repository: string; projectName: string } {
  const match = PROJECT_ID_PATTERN.exec(projectId);
  if (!match) {
    throw new McpError(
      ErrorCode.InvalidParams,
      `Invalid projectId format. Expected 'repository-projectName', got '${projectId}'`
    );
  }
  return { repository: match[1], projectName: match[2] };
}
Apply in handlers:

// In upload_file handler
if (!validateBase64(args.fileContent)) {
  throw new McpError(
    ErrorCode.InvalidParams,
    "fileContent must be valid base64-encoded string"
  );
}
4.3 Improve Error Sanitization
Current: sanitizeError() in utils.ts Enhancement: More comprehensive redaction

export function sanitizeError(error: unknown): string {
  let message = error instanceof Error ? error.message : String(error);
  
  // Redact patterns (existing + new)
  const redactPatterns = [
    /Bearer\s+[^\s]+/gi,
    /api[_-]?key[:\s]+[^\s]+/gi,
    /client[_-]?secret[:\s]+[^\s]+/gi,
    /(password|passwd|pwd)[:\s]+[^\s]+/gi,
    /\/\/[^:]+:[^@]+@/gi,  // URLs with credentials
    /token[:\s]+[^\s]+/gi,
  ];
  
  for (const pattern of redactPatterns) {
    message = message.replace(pattern, (match) => {
      const prefix = match.split(/[:\s]+/)[0];
      return `${prefix}: [REDACTED]`;
    });
  }
  
  return message;
}
4.4 Structured Logging (stderr only)
New file: src/logger.ts

export const logger = {
  error: (message: string, context?: Record<string, unknown>) => {
    // Log to stderr only (never stdout - would break MCP protocol)
    console.error(`[ERROR] ${message}`, context ? JSON.stringify(context) : '');
  },
  warn: (message: string, context?: Record<string, unknown>) => {
    console.error(`[WARN] ${message}`, context ? JSON.stringify(context) : '');
  },
  // NO info/debug during normal operation
};
Replace all console.error calls:

// Before
console.error("Failed to start server:", error);

// After
logger.error("Failed to start server", { error: sanitizeError(error) });
Changes:

✏️ Create validators.ts with validation utilities
✏️ Enhance sanitizeError() with more patterns
✏️ Create logger.ts with structured logging
✏️ Update all error handling to use logger
✏️ Add base64 validation to upload_file handler
5. Enhance Tool Functionality and Usability
5.1 Add Filters to List Operations
openl_list_projects - Add optional filters:

export const listProjectsSchema = z.object({
  repository: z.string().optional(),
  status: z.string().optional(),  // Existing
  tag: z.string().optional(),      // Existing
  branch: z.string().optional(),   // NEW
  response_format: ResponseFormat.optional(),
  limit: z.number().int().positive().max(200).default(50).optional(),
  offset: z.number().int().nonnegative().default(0).optional(),
}).strict();
openl_list_tables - Add table type filter (already exists):

export const listTablesSchema = z.object({
  projectId: projectIdSchema,
  tableType: z.string().optional(),  // Existing
  name: z.string().optional(),        // Existing
  file: z.string().optional(),        // Existing
  response_format: ResponseFormat.optional(),
  limit: z.number().int().positive().max(200).default(50).optional(),
  offset: z.number().int().nonnegative().default(0).optional(),
}).strict();
5.2 Improve Error Messages
Pattern: Suggest alternative tools when endpoints unavailable

// In create_rule handler
} catch (error) {
  if (isAxiosError(error) && error.response?.status === 405) {
    throw new McpError(
      ErrorCode.MethodNotAllowed,
      "create_rule is not supported in this OpenL Tablets version. " +
      "Alternative: Use openl_upload_file to upload Excel files with table definitions, " +
      "or use the OpenL WebStudio UI to create tables."
    );
  }
  // ... standard error handling
}
Changes:

✏️ Add branch filter to listProjectsSchema
✏️ Enhance error messages with alternative suggestions
✏️ Document all filter parameters in tool descriptions
6. Refactor Project Structure (Optional but Recommended)
Current Structure
src/
├── index.ts      (600 lines - server + switch statement)
├── client.ts     (370 lines - API client)
├── auth.ts       (232 lines)
├── tools.ts      (363 lines)
├── schemas.ts    (231 lines)
├── types.ts      (270 lines)
├── utils.ts      (121 lines)
├── constants.ts  (72 lines)
├── prompts.ts
└── prompts-registry.ts
Proposed Structure
src/
├── index.ts                    # Server entry point (minimal)
├── server.ts                   # Server class
├── services/
│   ├── openl-client.ts        # Renamed from client.ts
│   └── auth-service.ts        # Renamed from auth.ts
├── tools/
│   ├── index.ts               # Export all tools
│   ├── repository-tools.ts    # list_repositories, list_branches
│   ├── project-tools.ts       # list_projects, get_project, etc.
│   ├── file-tools.ts          # upload_file, download_file
│   ├── table-tools.ts         # list_tables, get_table, update_table, etc.
│   ├── deployment-tools.ts    # list_deployments, deploy_project
│   ├── version-tools.ts       # get_*_history, revert_version
│   └── execution-tools.ts     # execute_rule
├── schemas/
│   ├── index.ts               # Export all schemas
│   ├── common.ts              # Common schemas (projectId, etc.)
│   ├── repository-schemas.ts
│   ├── project-schemas.ts
│   ├── file-schemas.ts
│   ├── table-schemas.ts
│   ├── deployment-schemas.ts
│   ├── version-schemas.ts
│   └── execution-schemas.ts
├── utils/
│   ├── formatters.ts          # Response formatting
│   ├── validators.ts          # Input validation
│   ├── logger.ts              # Logging utilities
│   ├── sanitizer.ts           # Error sanitization
│   └── pagination.ts          # Pagination helpers
├── constants.ts               # Constants
└── types.ts                   # TypeScript types
Benefits:

Easier to find and modify specific tools
Reduced file sizes (better readability)
Clear domain boundaries
Easier testing (mock specific tool categories)
Migration Strategy:

Create new directory structure
Move code incrementally (one category at a time)
Update imports
Run tests after each migration
Update CONTRIBUTING.md with new structure
Changes:

✏️ Create new directory structure
✏️ Split tools.ts into 7 files by category
✏️ Split schemas.ts into 8 files by category
✏️ Move utilities from utils.ts into specialized files
✏️ Update all imports throughout codebase
✏️ Update CONTRIBUTING.md guide
7. Update Documentation and Tests
7.1 Documentation Updates
Files to update:

✏️ README.md: New tool names, pagination examples, response_format
✏️ BEST_PRACTICES.md: Add MCP compliance section
✏️ EXAMPLES.md: Update all 19 tool examples
✏️ CONTRIBUTING.md: New tool registration pattern
✏️ AUTHENTICATION.md: No changes needed
✏️ TESTING.md: Update test examples
New sections needed:

### Response Formatting
All tools support `response_format` parameter:
- `json` (default): Structured JSON response
- `markdown`: Human-readable markdown format

Example:
{
  "name": "openl_list_projects",
  "arguments": {
    "repository": "design",
    "response_format": "markdown"
  }
}

### Pagination
List operations support pagination:
- `limit`: Max items per page (default: 50, max: 200)
- `offset`: Starting position (default: 0)

Response includes:
- `has_more`: More results available
- `next_offset`: Next page offset
- `total_count`: Total items
7.2 Test Updates
Test files to update:

✏️ tests/mcp-server.test.ts: Update tool names
✏️ tests/openl-client.test.ts: No changes needed
✏️ tests/integration/openl-live.test.ts: Update tool names
New test cases needed:

describe('Response Formatting', () => {
  test('should return JSON when format=json', async () => {
    const result = await callTool('openl_list_repositories', { 
      response_format: 'json' 
    });
    expect(result).toMatchObject({ data: expect.any(Array) });
  });
  
  test('should return markdown when format=markdown', async () => {
    const result = await callTool('openl_list_repositories', { 
      response_format: 'markdown' 
    });
    expect(typeof result).toBe('string');
    expect(result).toContain('# Repositories');
  });
});

describe('Pagination', () => {
  test('should paginate results with limit/offset', async () => {
    const result = await callTool('openl_list_projects', {
      limit: 10,
      offset: 0
    });
    expect(result.pagination).toMatchObject({
      limit: 10,
      offset: 0,
      has_more: expect.any(Boolean),
    });
  });
  
  test('should truncate responses exceeding character limit', async () => {
    // Mock large response
    const result = await callTool('openl_list_tables', {
      projectId: 'large-project'
    });
    if (result.truncated) {
      expect(result.truncation_message).toBeTruthy();
    }
  });
});

describe('Input Validation', () => {
  test('should reject unknown parameters with .strict()', async () => {
    await expect(
      callTool('openl_list_projects', { unknown_param: 'value' })
    ).rejects.toThrow(/unrecognized/i);
  });
  
  test('should validate base64 in upload_file', async () => {
    await expect(
      callTool('openl_upload_file', {
        projectId: 'test-project',
        fileName: 'test.xlsx',
        fileContent: 'not-valid-base64!!!'
      })
    ).rejects.toThrow(/base64/i);
  });
});

describe('MCP Annotations', () => {
  test('should include annotations in tool metadata', () => {
    const tool = getToolByName('openl_list_repositories');
    expect(tool.annotations).toMatchObject({
      readOnlyHint: true,
      openWorldHint: true,
    });
  });
});
Changes:

✏️ Add 30+ new test cases for:
Response formatting (JSON vs Markdown)
Pagination boundaries
Character limit truncation
.strict() rejection of unknown fields
Base64 validation
MCP annotations presence
✏️ Update existing tests with new tool names
✏️ Add test helpers for pagination testing
Implementation Phases
Phase 1: Foundation (Days 1-2)

Rename server and tools (naming conventions)

Add [object Object] to all schemas

Create [object Object], [object Object], [object Object]

Add [object Object] to all schemas
Phase 2: Tool Registration (Days 3-4)

Create [object Object]

Implement [object Object] for each tool (19 tools)

Add MCP annotations to all tools

Remove switch statement from [object Object]

Update tests
Phase 3: Pagination & Formatting (Days 5-6)

Add [object Object]/[object Object] to list operation schemas

Implement pagination logic (client-side)

Implement markdown formatters for each data type

Add character limit enforcement

Update tests
Phase 4: Enhanced Validation (Day 7)

Add base64 validation

Enhance error sanitization

Implement structured logging

Add filter parameters

Update tests
Phase 5: Project Structure (Optional, Days 8-9)

Create new directory structure

Migrate tools to separate files

Migrate schemas to separate files

Update imports

Run full test suite
Phase 6: Documentation (Day 10)

Update README.md

Update EXAMPLES.md

Update CONTRIBUTING.md

Update BEST_PRACTICES.md

Update TESTING.md

Add 3+ examples per major tool
Risk Assessment
Low Risk
✅ Adding .strict() to schemas (breaking change but improves validation) ✅ Renaming tools (users update config once) ✅ Adding optional parameters (backward compatible)

Medium Risk
⚠️ Switching from switch statement to registerTool (requires careful testing) ⚠️ Response format change (need to handle both JSON and markdown)

High Risk
🔴 Project structure refactoring (optional - can skip if timeline is tight)

Success Metrics
Compliance

All 19 tools use [object Object] prefix

All tools registered via [object Object]

All tools have MCP annotations

All schemas use [object Object]

All list operations support pagination

All tools support [object Object]

Character limit enforced (<25,000 chars)
Testing

All existing tests pass with new names

30+ new tests for new features

Coverage >80%
Documentation

README updated with new conventions

3+ examples per major tool

Migration guide for users
Migration Guide for Users
Breaking Changes
Tool names changed: All tools now prefixed with openl_

Before: list_projects
After: openl_list_projects
Unknown parameters rejected: Schemas now use .strict()

Before: Extra params ignored
After: Extra params throw validation error
Default response format: Now markdown (was JSON)

Add response_format: "json" to get old behavior
Update Steps
// Claude Desktop config
{
  "mcpServers": {
    "openl": {  // Can rename from "openl-tablets"
      "command": "node",
      "args": ["/path/to/openl-mcp-server/dist/index.js"],
      "env": { ... }
    }
  }
}
Gradual Migration
Phase 1: Both old and new names work (deprecation warnings) Phase 2: Only new names work (major version bump to v2.0.0)

Approved Decisions
Server naming: Proceed with openl-mcp-server
Project structure refactoring: Include in scope 
Breaking changes: Accept tool name changes requiring user config updates
Default response format: Markdown (more readable)
Timeline: Implement all phases 
Version bump: Not needed, Everything can stay v1.0.0 as it's new impl.
