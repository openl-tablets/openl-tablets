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
- Tools: `get_file_history`, `get_project_history`, `revert_version`, `compare_versions`

**2. Dimension Properties (Business Context)**
- Multiple rule versions in same commit
- Properties: `state`, `lob`, `effectiveDate`, `expirationDate`, `caProvince`, `country`, `currency`
- OpenL selects version by runtime context
- Tools: `get/set_file_name_pattern`, `get/set_table_properties`

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

## Tools (24 Total)

**Repository** (2): list_repositories, list_branches
**Project** (6): list_projects, get_project, open_project, close_project, save_project, validate_project
**Files** (3): upload_file, download_file, get_file_history
**Rules** (8): list_tables, get_table, update_table, create_rule, copy_table, execute_rule, run_test, run_all_tests
**Version Control** (3): get_project_history, compare_versions, revert_version
**Deployment** (2): list_deployments, deploy_project
**Dimension Properties** (4): get/set_file_name_pattern, get/set_table_properties
**Testing** (2): validate_project, get_project_errors

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
    "openl-tablets": {
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
  "name": "list_projects",
  "arguments": { "repository": "design" }
}
```

### Get Table
```typescript
{
  "name": "get_table",
  "arguments": {
    "projectId": "design-insurance-rules",
    "tableId": "Rules.xls_1234"
  }
}
```

### Create Rule
```typescript
{
  "name": "create_rule",
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
  "name": "deploy_project",
  "arguments": {
    "projectName": "insurance-rules",
    "repository": "design",
    "deploymentRepository": "production"
  }
}
```

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
**Table Access**: Must `open_project` first

See [AUTHENTICATION.md](./AUTHENTICATION.md) and [TESTING.md](./TESTING.md) for detailed troubleshooting.

## Resources

- [OpenL Tablets](https://github.com/openl-tablets/openl-tablets)
- [OpenL Documentation](https://openl-tablets.org/)
- [Model Context Protocol](https://modelcontextprotocol.io/)
- [MCP TypeScript SDK](https://github.com/modelcontextprotocol/typescript-sdk)

## License

Follows OpenL Tablets project license.
