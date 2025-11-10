# OpenL Tablets MCP Server

Model Context Protocol (MCP) server for [OpenL Tablets](https://github.com/openl-tablets/openl-tablets) Business Rules Management System. This server provides AI assistants with the ability to interact with OpenL Tablets rules projects through a standardized interface.

## Features

### Resources
- **Repositories** - Browse design repositories
- **Projects** - View all projects across repositories
- **Deployments** - Monitor deployed projects

### Tools

#### Repository Management
- `list_repositories` - List all design repositories
- `list_branches` - List branches for a repository

#### Project Management
- `list_projects` - List projects with filters (repository, status, tag)
- `get_project` - Get detailed project information
- `get_project_info` - Get project info including modules and dependencies
- `open_project` - Open a project for viewing/editing
- `close_project` - Close an open project
- `get_project_history` - Get version history
- `create_branch` - Create a new branch

#### Rules (Tables) Management
- `list_tables` - List all tables/rules in a project
- `get_table` - Get detailed table data and structure
- `update_table` - Update table content

#### Deployment
- `list_deployments` - List all deployments
- `deploy_project` - Deploy a project to production

## Installation

### Prerequisites

- Node.js 18.0.0 or higher
- Access to an OpenL Tablets WebStudio instance
- Valid credentials (username/password or API key)

### Setup

1. Navigate to the MCP server directory:
```bash
cd mcp-server
```

2. Install dependencies:
```bash
npm install
```

3. Build the TypeScript code:
```bash
npm run build
```

## Configuration

Configure the server using environment variables:

```bash
# Required: OpenL Tablets WebStudio URL
export OPENL_BASE_URL="http://localhost:8080/webstudio/rest"

# Authentication: Use either username/password OR API key
export OPENL_USERNAME="your-username"
export OPENL_PASSWORD="your-password"

# OR use API key authentication
export OPENL_API_KEY="your-api-key"
```

### Configuration Files

Create a `.env` file in the MCP server directory:

```env
OPENL_BASE_URL=http://localhost:8080/webstudio/rest
OPENL_USERNAME=admin
OPENL_PASSWORD=admin
```

Or for API key authentication:

```env
OPENL_BASE_URL=http://localhost:8080/webstudio/rest
OPENL_API_KEY=your-api-key-here
```

## Usage

### Running the Server

Direct execution:
```bash
npm start
```

Development mode with auto-rebuild:
```bash
npm run watch
```

### Configuring MCP Clients

#### Claude Desktop

Add to your Claude Desktop configuration file:

**MacOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`
**Windows**: `%APPDATA%/Claude/claude_desktop_config.json`

```json
{
  "mcpServers": {
    "openl-tablets": {
      "command": "node",
      "args": ["/path/to/openl-tablets/mcp-server/dist/index.js"],
      "env": {
        "OPENL_BASE_URL": "http://localhost:8080/webstudio/rest",
        "OPENL_USERNAME": "admin",
        "OPENL_PASSWORD": "admin"
      }
    }
  }
}
```

#### Other MCP Clients

The server uses stdio transport and can be integrated with any MCP-compatible client. Provide the path to `dist/index.js` and set the required environment variables.

## Tool Usage Examples

### List All Projects

```typescript
// Request
{
  "name": "list_projects",
  "arguments": {}
}

// Response
[
  {
    "name": "insurance-rules",
    "repository": "design",
    "status": "OPENED",
    "modifiedAt": "2025-11-10T10:30:00Z",
    "modifiedBy": "admin"
  }
]
```

### Get Project Details

```typescript
// Request
{
  "name": "get_project",
  "arguments": {
    "projectId": "design-insurance-rules"
  }
}
```

### List Tables in a Project

```typescript
// Request
{
  "name": "list_tables",
  "arguments": {
    "projectId": "design-insurance-rules"
  }
}

// Response
[
  {
    "id": "Rules.xls_1234",
    "tableType": "simplerules",
    "kind": "XLS_DT",
    "name": "CalculatePremium",
    "returnType": "Double",
    "file": "Rules.xlsx"
  }
]
```

### Get Table Data

```typescript
// Request
{
  "name": "get_table",
  "arguments": {
    "projectId": "design-insurance-rules",
    "tableId": "Rules.xls_1234"
  }
}

// Returns full table structure including:
// - Properties
// - Columns/rows (for decision tables)
// - Fields (for datatypes)
// - Spreadsheet data
```

### Update Table

```typescript
// Request
{
  "name": "update_table",
  "arguments": {
    "projectId": "design-insurance-rules",
    "tableId": "Rules.xls_1234",
    "view": {
      // Modified table view object
    },
    "comment": "Updated premium calculation logic"
  }
}
```

### Deploy to Production

```typescript
// Request
{
  "name": "deploy_project",
  "arguments": {
    "projectName": "insurance-rules",
    "repository": "design",
    "deploymentRepository": "production"
  }
}
```

## API Reference

### OpenL Tablets REST API

This MCP server wraps the OpenL Tablets WebStudio REST API. For detailed API documentation, refer to:

- **API Base Path**: `/webstudio/rest`
- **Swagger UI**: `http://your-server:8080/webstudio/swagger-ui.html`
- **OpenAPI Spec**: `http://your-server:8080/webstudio/v3/api-docs`

### Project ID Format

Project IDs follow the format: `{repository-name}-{project-name}`

Examples:
- `design-insurance-rules`
- `production-loan-calculator`

### Table Types

OpenL Tablets supports various table types:
- `datatype` - Custom data type definitions
- `vocabulary` - Enumeration/vocabulary tables
- `spreadsheet` - Spreadsheet calculation tables
- `simplerules` - Simple decision tables
- `smartrules` - Smart decision tables
- `method` - Method tables
- `test` - Test tables
- `data` - Data tables

## Development

### Project Structure

```
mcp-server/
├── src/
│   ├── index.ts      # Main server implementation
│   └── types.ts      # TypeScript type definitions
├── dist/             # Compiled JavaScript (generated)
├── package.json
├── tsconfig.json
└── README.md
```

### Building

```bash
npm run build
```

### Type Checking

The project uses TypeScript with strict type checking. All OpenL Tablets API types are defined in `src/types.ts`.

## Troubleshooting

### Connection Issues

If you encounter connection errors:

1. Verify OpenL Tablets WebStudio is running
2. Check the `OPENL_BASE_URL` is correct
3. Ensure the REST API path is `/webstudio/rest`
4. Verify network connectivity

### Authentication Errors

If you receive 401/403 errors:

1. Verify username/password or API key
2. Check user has appropriate permissions in OpenL Tablets
3. Ensure the user can access the WebStudio interface

### Project Not Found

If projects cannot be found:

1. Ensure the project is in a design repository
2. Check the project ID format: `{repo-name}-{project-name}`
3. Verify the project exists in OpenL Tablets WebStudio

### Table Access Issues

To access tables:

1. Project must be OPENED first using `open_project`
2. Use the correct `projectId` and `tableId`
3. Check user has edit permissions for modifications

## Architecture

### OpenL Tablets Structure

```
OpenL Tablets
├── Design Repositories (Git, File System, etc.)
│   └── Projects
│       ├── rules.xml (Project descriptor)
│       └── Modules
│           └── Excel Files (.xlsx, .xls)
│               └── Tables (Decision Tables, Datatypes, etc.)
│
└── Production Repositories
    └── Deployed Projects
```

### MCP Server Flow

1. Client connects via stdio transport
2. Server exposes resources and tools
3. Tools make REST API calls to OpenL Tablets
4. Responses formatted and returned to client

## Security Considerations

- Store credentials securely (use environment variables, not hardcoded)
- Use HTTPS for production OpenL Tablets instances
- Implement proper access control in OpenL Tablets
- API keys are preferred over username/password
- Never commit credentials to version control

## Contributing

This MCP server is part of the OpenL Tablets project. For issues or contributions, please refer to the main OpenL Tablets repository.

## License

This MCP server follows the same license as the OpenL Tablets project.

## Resources

- [OpenL Tablets GitHub](https://github.com/openl-tablets/openl-tablets)
- [OpenL Tablets Documentation](https://openl-tablets.org/)
- [Model Context Protocol](https://modelcontextprotocol.io/)
- [MCP TypeScript SDK](https://github.com/modelcontextprotocol/typescript-sdk)

## Support

For issues specific to:
- **MCP Server**: Open an issue in the OpenL Tablets repository
- **OpenL Tablets**: Refer to OpenL Tablets documentation and community
- **MCP Protocol**: Refer to Model Context Protocol documentation
