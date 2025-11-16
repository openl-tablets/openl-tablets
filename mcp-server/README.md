# OpenL Tablets MCP Server

Model Context Protocol server for [OpenL Tablets](https://github.com/openl-tablets/openl-tablets) Business Rules Management System.

Built with MCP SDK v1.21.1 featuring type-safe validation (Zod), OAuth 2.1 support, and comprehensive OpenL Tablets integration.

## Quick Start

```bash
cd mcp-server
npm install
npm run build

# Configure
export OPENL_BASE_URL="http://localhost:8080/webstudio/rest"
export OPENL_USERNAME="admin"
export OPENL_PASSWORD="admin"

# Run
npm start
```

## OpenL Tablets Concepts

### Versioning (Dual System)

OpenL has TWO independent versioning systems:

**1. Git-Based (Temporal)**
- Every save creates Git commit automatically
- Version = commit hash (e.g., "7a3f2b1c")
- Tools: `openl_get_file_history`, `openl_get_project_history`, `openl_revert_version`, `openl_compare_versions`

**2. Dimension Properties (Business Context)**
- Multiple rule versions in same commit
- Properties: `state`, `lob`, `effectiveDate`, `expirationDate`, `caProvince`, `country`, `currency`
- OpenL selects version by runtime context
- Managed via OpenL WebStudio UI

### Table Types

**Decision Tables** (5 variants):
- `Rules` - Standard with C1/C2/A1/RET markers
- `SimpleRules` - Positional matching (left-to-right)
- `SmartRules` - Smart matching by column name
- `SimpleLookup` - 2D matrix lookup
- `SmartLookup` - 2D smart lookup

**Spreadsheet Tables**:
- Multi-step calculations with `$columnName$rowName` references
- Return `SpreadsheetResult` (full matrix) or specific type (final value)

**Other**: `Method`, `TBasic`, `Data`, `Datatype`, `Test`, `Run`, `Properties`, `Configuration`

See [prompts/create_rule.md](./prompts/create_rule.md) for detailed table type guidance.

## Tools (19 Total)

**Repository** (2): openl_list_repositories, openl_list_branches
**Project** (6): openl_list_projects, openl_get_project, openl_open_project, openl_close_project, openl_save_project, openl_validate_project
**Files** (3): openl_upload_file, openl_download_file, openl_get_file_history
**Rules** (7): openl_list_tables, openl_get_table, openl_update_table, openl_create_rule, openl_execute_rule, openl_run_test, openl_run_all_tests
**Version Control** (3): openl_get_project_history, openl_compare_versions, openl_revert_version
**Deployment** (2): openl_list_deployments, openl_deploy_project
**Testing** (2): openl_validate_project, openl_get_project_errors

## Prompts (11 Total)

Expert guidance templates for complex OpenL Tablets workflows. Prompts provide contextual assistance, best practices, and step-by-step instructions directly in Claude Desktop or MCP Inspector.

### Available Prompts

| Prompt | Description | Arguments |
|--------|-------------|-----------|
| **create_rule** | Comprehensive guide for creating OpenL tables (decision tables, spreadsheets, datatypes) with examples for all 5 decision table variants | None |
| **datatype_vocabulary** | Guide for defining custom datatypes (domain objects) and vocabularies (enumerations) with inheritance and validation | None |
| **create_test** | Step-by-step guide for creating OpenL test tables with proper 3-row structure and validation | `tableName`, `tableType` |
| **update_test** | Guide for modifying existing tests, adding test cases, and updating expected values | `testId`, `tableName` |
| **run_test** | Test selection logic and workflow for efficient test execution (single, multiple, or all tests) | `scope`, `tableIds` |
| **dimension_properties** | Explanation of OpenL dimension properties (state, lob, dates) vs Git versioning with runtime selection logic | None |
| **execute_rule** | Guide for constructing test data and executing OpenL rules with proper JSON formatting | `ruleName`, `projectId` |
| **deploy_project** | Deployment workflow with mandatory validation checks and environment selection | `projectId`, `environment` |
| **get_project_errors** | Error analysis workflow with pattern matching and fix recommendations | `projectId` |
| **file_history** | Guide for viewing Git-based file version history and commit navigation | `filePath`, `projectId` |
| **project_history** | Guide for viewing project-wide Git commit history | `projectId` |

### Using Prompts

**In Claude Desktop:**
```
"Use the create_rule prompt"
"Show me the dimension_properties prompt"
```

**With Arguments:**
```
"Use create_test prompt with tableName=calculatePremium"
"Show deploy_project prompt for projectId=design-Insurance"
```

**In MCP Inspector:**
1. Navigate to "Prompts" tab
2. Select prompt from list
3. Provide arguments if needed
4. View rendered guidance

### Prompt Content

Prompts are stored as markdown files in `prompts/` directory:
- **Comprehensive**: Cover all aspects of the workflow
- **Examples**: Real-world code samples and patterns
- **Best Practices**: OpenL-specific recommendations
- **Decision Guides**: Help choose the right approach

See individual prompt files for detailed content.

### Prompt Structure (YAML Frontmatter)

Each prompt file uses YAML frontmatter for metadata and argument definitions:

```markdown
---
name: create_test
description: Guide for creating OpenL test tables with proper structure and validation
arguments:
  - name: tableName
    description: Name of the table being tested
    required: false
  - name: tableType
    description: Type of table (Rules, SimpleRules, Spreadsheet, etc.)
    required: false
---

# Creating Test Tables in OpenL Tablets

{if tableName}
## Creating Test for: **{tableName}**
{end if}

Content with {variable} placeholders and {if condition}...{end if} blocks...
```

**Frontmatter fields:**
- `name`: Prompt identifier (must match filename)
- `description`: Brief summary shown in MCP Inspector
- `arguments`: Optional array of argument definitions

**Template syntax:**
- `{variable}`: Simple substitution
- `{if variable}...{end if}`: Conditional blocks (shown only when argument provided)

**Benefits:**
- **Dynamic content**: Prompts adapt to context
- **Type safety**: Arguments validated by MCP protocol
- **Backward compatible**: All arguments are optional
- **Self-documenting**: Frontmatter serves as API documentation

## Configuration

### Environment Variables

```bash
# Required
OPENL_BASE_URL=http://localhost:8080/webstudio/rest

# Auth Method 1: Basic Auth
OPENL_USERNAME=admin
OPENL_PASSWORD=admin

# Auth Method 2: API Key
OPENL_API_KEY=your-api-key

# Auth Method 3: OAuth 2.1
OPENL_OAUTH2_CLIENT_ID=client-id
OPENL_OAUTH2_CLIENT_SECRET=client-secret
OPENL_OAUTH2_TOKEN_URL=https://auth.example.com/oauth/token
OPENL_OAUTH2_SCOPE=openl:read openl:write

# Optional
OPENL_CLIENT_DOCUMENT_ID=mcp-server-1
OPENL_TIMEOUT=60000
```

See [AUTHENTICATION.md](./AUTHENTICATION.md) for detailed auth setup.

### Claude Desktop

MacOS: `~/Library/Application Support/Claude/claude_desktop_config.json`
Windows: `%APPDATA%/Claude/claude_desktop_config.json`

```json
{
  "mcpServers": {
    "openl-mcp-server": {
      "command": "node",
      "args": ["/absolute/path/to/mcp-server/dist/index.js"],
      "env": {
        "OPENL_BASE_URL": "http://localhost:8080/webstudio/rest",
        "OPENL_USERNAME": "admin",
        "OPENL_PASSWORD": "admin"
      }
    }
  }
}
```

## Usage Examples

### List Projects
```typescript
{
  "name": "openl_list_projects",
  "arguments": { "repository": "design" }
}
```

### Get Table
```typescript
{
  "name": "openl_get_table",
  "arguments": {
    "projectId": "design-insurance-rules",
    "tableId": "Rules.xls_1234"
  }
}
```

### Create Rule
```typescript
{
  "name": "openl_create_rule",
  "arguments": {
    "projectId": "design-insurance-rules",
    "name": "calculatePremium",
    "tableType": "SimpleRules",
    "returnType": "double",
    "parameters": [
      { "type": "String", "name": "driverType" },
      { "type": "int", "name": "age" }
    ]
  }
}
```

### Deploy
```typescript
{
  "name": "openl_deploy_project",
  "arguments": {
    "projectName": "insurance-rules",
    "repository": "design",
    "deploymentRepository": "production"
  }
}
```

### Response Formatting

All tools support the `response_format` parameter:
- `json`: Structured JSON response (for programmatic use)
- `markdown`: Human-readable markdown format (default, better for AI consumption)

Example:
```json
{
  "name": "openl_list_projects",
  "arguments": {
    "repository": "design",
    "response_format": "markdown"
  }
}
```

### Pagination

List operations support pagination parameters:
- `limit`: Maximum items per page (default: 50, max: 200)
- `offset`: Starting position (default: 0)

Response includes pagination metadata:
- `has_more`: Whether more results are available
- `next_offset`: Offset for the next page
- `total_count`: Total number of items

Example:
```json
{
  "name": "openl_list_projects",
  "arguments": {
    "limit": 10,
    "offset": 0
  }
}
```

### Response Size and Truncation

To ensure optimal performance and compatibility with MCP clients, responses are subject to a character limit:

- **Maximum response size**: 25,000 characters
- **Automatic truncation**: Responses exceeding this limit are automatically truncated
- **Truncation message**: When truncation occurs, a message is appended indicating the response was truncated

**Best practices to avoid truncation:**
- Use `limit` parameter to reduce page size (e.g., `limit: 20` instead of default 50)
- Use `offset` parameter to paginate through large result sets
- Apply filters to narrow results (e.g., filter by repository, status, tableType)
- Use specific queries instead of broad searches

**Example - Handling large result sets:**
```json
{
  "name": "openl_list_tables",
  "arguments": {
    "projectId": "design-large-project",
    "tableType": "Rules",
    "limit": 20,
    "offset": 0,
    "response_format": "markdown"
  }
}
```

If you receive a truncation message, reduce `limit` or add filters to retrieve complete data.

## Project Structure

```
mcp-server/
├── src/
│   ├── index.ts      # MCP server entry point
│   ├── client.ts     # OpenL Tablets API client
│   ├── auth.ts       # Authentication (OAuth 2.1, API Key, Basic)
│   ├── tools.ts      # Tool definitions with metadata
│   ├── schemas.ts    # Zod validation schemas
│   ├── types.ts      # TypeScript types
│   └── utils.ts      # Security utilities
├── tests/            # Jest test suites
├── prompts/          # AI assistant guidance (OpenL-specific)
├── dist/             # Compiled output
├── README.md         # This file
├── AUTHENTICATION.md # Auth setup guide
├── TESTING.md        # Testing guide
├── EXAMPLES.md       # Detailed examples
└── CONTRIBUTING.md   # Development guide
```

## Development

```bash
npm run build          # Build TypeScript
npm test               # Run tests (47 total)
npm run lint           # Check code quality
npm run watch          # Dev mode with auto-rebuild
```

See [CONTRIBUTING.md](./CONTRIBUTING.md) for development guidelines.

## Key Features

- **Type-Safe**: Zod schemas with TypeScript inference
- **OpenL-Specific**: Proper table types (SimpleRules not simplerules)
- **Dual Versioning**: Git commits + dimension properties
- **OAuth 2.1**: Automatic token management and refresh
- **Enhanced Errors**: Detailed context (endpoint, method, tool)
- **Tool Metadata**: Version, category, operation flags
- **AI Prompts**: OpenL-specific guidance in prompts/ directory

## Troubleshooting

**Connection**: Check `OPENL_BASE_URL` ends with `/webstudio/rest`
**Auth 401/403**: Verify credentials, check user permissions
**Project Not Found**: Format is `{repo}-{project}` (e.g., `design-insurance-rules`)
**Table Access**: Must `openl_open_project` first

See [AUTHENTICATION.md](./AUTHENTICATION.md) and [TESTING.md](./TESTING.md) for detailed troubleshooting.

## Resources

- [OpenL Tablets](https://github.com/openl-tablets/openl-tablets)
- [OpenL Documentation](https://openl-tablets.org/)
- [Model Context Protocol](https://modelcontextprotocol.io/)
- [MCP TypeScript SDK](https://github.com/modelcontextprotocol/typescript-sdk)

## License

Follows OpenL Tablets project license.
